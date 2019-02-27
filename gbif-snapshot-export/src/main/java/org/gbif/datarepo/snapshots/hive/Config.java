package org.gbif.datarepo.snapshots.hive;

import org.gbif.datarepo.impl.conf.DataRepoConfiguration;

import java.util.List;
import java.util.UUID;

/**
 * Configuration settings to run a Snapshot export into the GBIF DataRepo/DataOne.
 */
public class Config {
    private String metaStoreUris;
    private String hive2JdbcUrl;
    private String hiveDB;
    private String snapshotTable;
    private String exportPath;
    private UUID updateMetadataPackage;
    private DataRepoConfiguration dataRepoConfiguration;

    // TODO: get from ws??
    // downloads
    private List<DownloadInfo> downloads;

    public String getMetaStoreUris() {
        return metaStoreUris;
    }

    public void setMetaStoreUris(String metaStoreUris) {
        this.metaStoreUris = metaStoreUris;
    }

    public String getHive2JdbcUrl() {
        return hive2JdbcUrl;
    }

    public void setHive2JdbcUrl(String hive2JdbcUrl) {
        this.hive2JdbcUrl = hive2JdbcUrl;
    }

    public String getHiveDB() {
        return hiveDB;
    }

    public void setHiveDB(String hiveDB) {
        this.hiveDB = hiveDB;
    }

    public String getSnapshotTable() {
        return snapshotTable;
    }

    public void setSnapshotTable(String snapshotTable) {
        this.snapshotTable = snapshotTable;
    }

    public String getExportPath() {
        return exportPath;
    }

    public void setExportPath(String exportPath) {
        this.exportPath = exportPath;
    }

    public UUID getUpdateMetadataPackage() {
        return updateMetadataPackage;
    }

    public void setUpdateMetadataPackage(UUID updateMetadataPackage) {
        this.updateMetadataPackage = updateMetadataPackage;
    }

    public DataRepoConfiguration getDataRepoConfiguration() {
        return dataRepoConfiguration;
    }

    public void setDataRepoConfiguration(DataRepoConfiguration dataRepoConfiguration) {
        this.dataRepoConfiguration = dataRepoConfiguration;
    }

    public String getFullSnapshotTableName() {
        return hiveDB  + "." + snapshotTable;
    }

    public List<DownloadInfo> getDownloads() {
        return downloads;
    }

    public void setDownloads(List<DownloadInfo> downloads) {
        this.downloads = downloads;
    }

    enum Format {
        DWCA, CSV, AVRO
    }

    static class DownloadInfo{
        Format format;
        String path;
        String doi;

        public Format getFormat() {
            return format;
        }

        public void setFormat(Format format) {
            this.format = format;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getDoi() {
            return doi;
        }

        public void setDoi(String doi) {
            this.doi = doi;
        }
    }
}
