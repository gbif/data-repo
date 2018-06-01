package org.gbif.datarepo.snapshots.hive;

import org.gbif.datarepo.impl.conf.DataRepoConfiguration;

/**
 * Configuration settings to run a Snapshot export into the GBIF DataRepo/DataOne.
 */
public class Config {
    private String metaStoreUris;
    private String hive2JdbcUrl;
    private String hiveDB;
    private String snapshotTable;
    private String exportPath;
    private DataRepoConfiguration dataRepoConfiguration;

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

    public DataRepoConfiguration getDataRepoConfiguration() {
        return dataRepoConfiguration;
    }

    public void setDataRepoConfiguration(DataRepoConfiguration dataRepoConfiguration) {
        this.dataRepoConfiguration = dataRepoConfiguration;
    }

    public String getFullSnapshotTableName() {
        return hiveDB  + "." + snapshotTable;
    }
}
