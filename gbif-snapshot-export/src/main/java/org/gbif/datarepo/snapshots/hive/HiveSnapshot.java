package org.gbif.datarepo.snapshots.hive;

import com.google.common.base.Throwables;
import com.google.common.io.ByteStreams;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.HiveMetaStoreClient;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.thrift.TException;
import org.gbif.dwc.terms.GbifInternalTerm;
import org.gbif.dwc.terms.GbifTerm;
import org.gbif.dwc.terms.Term;
import org.gbif.dwc.terms.TermFactory;
import org.gbif.hadoop.compress.d2.D2CombineInputStream;
import org.gbif.hadoop.compress.d2.D2Utils;
import org.gbif.hadoop.compress.d2.zip.ModalZipOutputStream;
import org.gbif.hadoop.compress.d2.zip.ZipEntry;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

class HiveSnapshot {

    private final Config config;


    HiveSnapshot(Config config) {
        this.config = config;
    }

    private static int runHiveExport(String pathToQueryFile) {
        try {
            return new ProcessBuilder("hive" , "-f", pathToQueryFile)
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                    .start()
                    .waitFor();
        } catch (IOException | InterruptedException ex) {
            throw  new RuntimeException(ex);
        }
    }

    private static FileSystem getFileSystem() {
        try {
            return FileSystem.newInstance(new org.apache.hadoop.conf.Configuration());
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }

    }

    /**
     * Merges the pre-deflated content using the hadoop-compress library.
     */
    private void zipPreDeflated(String header, Path sourcePath, Path outputPath) throws IOException {
        FileSystem fs = getFileSystem();
        appendHeaderFile(header, fs, sourcePath);
        try (FSDataOutputStream zipped = fs.create(outputPath, true);
             ModalZipOutputStream zos = new ModalZipOutputStream(new BufferedOutputStream(zipped));
             D2CombineInputStream in =
                     new D2CombineInputStream(
                             Arrays.stream(fs.listStatus(sourcePath))
                                     .map(
                                             input -> {
                                                 try {
                                                     return fs.open(input.getPath());
                                                 } catch (IOException ex) {
                                                     throw Throwables.propagate(ex);
                                                 }
                                             })
                                     .collect(Collectors.toList()))
        )
        {

            ZipEntry ze = new ZipEntry(sourcePath.getName() + ".csv");
            zos.putNextEntry(ze, ModalZipOutputStream.MODE.PRE_DEFLATED);
            ByteStreams.copy(in, zos);
            in.close(); // required to get the sizes
            ze.setSize(in.getUncompressedLength()); // important to set the sizes and CRC
            ze.setCompressedSize(in.getCompressedLength());
            ze.setCrc(in.getCrc32());
            zos.closeEntry();
        } catch (Exception ex) {
            throw Throwables.propagate(ex);
        }
    }


    /**
     * Creates a compressed file named '0' that contains the content of the file HEADER.
     */
    private void appendHeaderFile(String header, FileSystem fileSystem, Path dir)
            throws IOException {
        try (FSDataOutputStream fsDataOutputStream = fileSystem.create(new Path(dir, "0"))) {
            D2Utils.compress(new ByteArrayInputStream(header.getBytes()), fsDataOutputStream);
        }
    }

    private static String getRunningContext() {
        return new java.io.File(HiveSnapshot.class.getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .getPath())
                .getName();
    }

    private void generateHiveExport(Map<Term, FieldSchema> colTerms) {
        Map<String,String> hiveColMapping = colTerms.entrySet()
                .stream()
                .filter(entry -> entry.getKey() != GbifTerm.gbifID)
                .collect(Collectors.toMap(e -> e.getKey().simpleName(), e -> e.getValue().getName(), (e1,e2) -> e1, TreeMap::new));
        Map<String, Object> params = new HashMap<>();
        params.put("colMap", hiveColMapping);
        params.put("hiveDB", config.getHiveDB());
        params.put("snapshotTable", config.getSnapshotTable());
        String runningContext = getRunningContext();
        if(runningContext.endsWith(".jar")) {
            params.put("thisJar", runningContext);
        }
        TemplateUtils.runTemplate(params, "export_snapshot.ftl", "export_snapshot.ql");
    }



    private static Term getColumnTerm(String columnName) {
        if (columnName.equalsIgnoreCase("id")) {
            return GbifTerm.gbifID;
        }
        if (columnName.equalsIgnoreCase("publisher_country")) {
            return GbifTerm.publishingCountry;
        }
        if (columnName.equalsIgnoreCase("publisher_id")) {
            return GbifInternalTerm.publishingOrgKey;
        }
        return  TermFactory.instance()
                .findTerm(columnName.replaceFirst("v_", "")
                        .replaceAll("_id", "key").replaceAll("_",""));
    }

    public void export() {
        try {
            HiveConf hiveConf = new HiveConf();
            hiveConf.setVar(HiveConf.ConfVars.METASTOREURIS, config.getMetaStoreUris());
            HiveMetaStoreClient hiveMetaStoreClient = new HiveMetaStoreClient(hiveConf);
            Map<Term,FieldSchema> colTerms =  hiveMetaStoreClient.getFields(config.getHiveDB(), config.getSnapshotTable())
                    .stream()
                    .map(fieldSchema -> new AbstractMap.SimpleEntry<>(getColumnTerm(fieldSchema.getName()), fieldSchema))
                    .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
            String header = GbifTerm.gbifID.simpleName() + '\t' +colTerms.keySet().stream().filter(term -> term != GbifTerm.gbifID)
                    .map(Term::simpleName).sorted().collect(Collectors.joining("\t"))  + '\n';
            generateHiveExport(colTerms);
            runHiveExport("export_snapshot.ql");
            zipPreDeflated(header, new Path("/user/hive/warehouse/" + config.getHiveDB() + ".db/export_" + config.getSnapshotTable() + "/"), new Path(config.getExportPath() + config.getSnapshotTable()  + ".zip"));
        } catch (TException | IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private Long count() {
        try {
            Class.forName("org.apache.hive.jdbc.HiveDriver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        try (Connection con = DriverManager.getConnection(config.getHive2JdbcUrl(), "", "");
             Statement stmt = con.createStatement();
             ResultSet result = stmt.executeQuery("SELECT COUNT(*) FROM " + config.getHiveDB() + "." + config.getSnapshotTable())) {
            return result.next() ? result.getLong(1) : 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
