package org.gbif.datarepo.snapshots;

import org.gbif.api.model.common.DOI;
import org.gbif.api.vocabulary.License;
import org.gbif.datarepo.api.DataRepository;
import org.gbif.datarepo.api.model.DataPackage;
import org.gbif.datarepo.api.model.FileInputContent;
import org.gbif.datarepo.api.model.Identifier;
import org.gbif.datarepo.logging.EventLogger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.UUID;

import com.google.common.base.Throwables;
import com.google.common.collect.Sets;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to persist DataPackages for GBIF Snapshots.
 */
public class DataPackageManager {

    private static final String CREATOR = "gbif-snapshot";

    private static final Logger LOG = LoggerFactory.getLogger(DataPackageManager.class);

    private final DataRepository dataRepository;

    /**
     * Builds an instance using the {@link DataRepository}.
     */
    public DataPackageManager(DataRepository dataRepository) {
        this.dataRepository = dataRepository;
    }


    /**
     * Builds a simple DataPackage using basic information and the 'relatedDoi' as a related identifier.
     */
    private DataPackage buildDataPackageWithDoi(String title, String description, String relatedDoi) {
        DataPackage dataPackage = buildDataPackage(title, description);
        Identifier identifier = new Identifier();
        identifier.setIdentifier(relatedDoi);
        identifier.setType(Identifier.Type.DOI);
        identifier.setRelationType(Identifier.RelationType.References);
        identifier.setCreatedBy(CREATOR);
        dataPackage.addRelatedIdentifier(identifier);
        return dataPackage;
    }

    /**
     * Builds a DataPackage that will be shared in DataOne.
     */
    private DataPackage buildDataPackage(String title, String description) {
        DataPackage dataPackage = new DataPackage();
        dataPackage.setCreatedBy(CREATOR);
        dataPackage.setTitle(title);
        dataPackage.setDescription(description);
        dataPackage.setLicense(License.CC_BY_4_0);
        dataPackage.setShareIn(Sets.newHashSet("DataOne"));
        return dataPackage;
    }

    /**
     * Creates and persists a DataPackage from Hadoop HDFS path.
     */
    public DataPackage createSnapshotDataPackage(URI file) {
        Path path = new Path(file);
        LOG.info("Creating DataPackage for file {}", file);
        DataPackage newDataPackage = dataRepository.create(buildDataPackage(
                "GBIF occurrence data snapshot " + path.getName(),
                "GBIF snapshot data in compress format"),
                Collections.singletonList(FileInputContent.from(path.getName(), file)), true);
        EventLogger.logCreate(LOG, null, newDataPackage.getKey().toString());
        return newDataPackage;
    }

    /**
     * Creates and persists a DataPackage from a download
     */
    public DataPackage createSnapshotDataPackageFromDownload(URI file, String doi) {
        Path path = new Path(file);
        LOG.info("Creating DataPackage for file {}", file);
        DataPackage dataPackage = buildDataPackage(
          "GBIF occurrence data snapshot " + path.getName(),
          "GBIF snapshot data in compress format");
        dataPackage.setDoi(new DOI(doi));

        DataPackage newDataPackage = dataRepository.create(dataPackage,
                                                           Collections.singletonList(FileInputContent.from(path.getName(), file)),
                                                           false);
        EventLogger.logCreate(LOG, null, newDataPackage.getKey().toString());
        return newDataPackage;
    }

    public DataPackage getDataPackage(UUID id) {
        return dataRepository.get(id).orElseThrow(IllegalArgumentException::new);
    }

    public InputStream getDataPackageInputStream(UUID dataPackageKey, String fileName) {
        return dataRepository.getFileInputStream(dataPackageKey, fileName).orElseThrow(IllegalArgumentException::new);
    }

    /**
     * Creates and persists a DataPackage from local file.
     */
    private DataPackage createDataPackage(File file, DataPackage dataPackage) {
        LOG.info("Creating DataPackage for file {}", file);
        try (InputStream inputStream = new FileInputStream(file)) {
            DataPackage newDataPackage = dataRepository.create(
                    dataPackage,
                    Collections.singletonList(FileInputContent.from(file.getName(), inputStream)), true);
            EventLogger.logCreate(LOG, null, newDataPackage.getKey().toString());
            return newDataPackage;
        } catch (IOException ex) {
            LOG.error("Error creating DataPackage", ex);
            throw Throwables.propagate(ex);
        }
    }

    /**
     * Creates RDF DataPackage related to the snapshotDOI param.
     */
    public DataPackage createSnapshotRdfDataPackage(File file, String snapshotDOI, UUID generatedId) {
        DataPackage dataPackage = buildDataPackageWithDoi("GBIF Snapshot RDF Metadata " + file.getName(),
                "RDF Metadata for GBIF Snapshot " + file.getName(), snapshotDOI);
        dataPackage.setKey(generatedId);
        return createDataPackage(file, dataPackage);
    }

    /**
     * Creates EML DataPackage related to the snapshotDOI param.
     */

    public DataPackage createSnapshotEmlDataPackage(File file, String snapshotDOI) {
        return createDataPackage(file, buildDataPackageWithDoi("GBIF Snapshot Metadata " + file.getName(),
                "EML Metadata for GBIF Snapshot " + file.getName(), snapshotDOI));
    }

}
