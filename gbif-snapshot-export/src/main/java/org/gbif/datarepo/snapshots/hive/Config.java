package org.gbif.datarepo.snapshots.hive;

public class Config {
    private final String metaStoreUris;
    private final String hive2JdbcUrl;
    private final String hiveDB;
    private final String snapshotTable;
    private final String exportPath;

    public Config(String metaStoreUris, String hive2JdbcUrl, String hiveDB, String snapshotTable, String exportPath) {
        this.metaStoreUris = metaStoreUris;
        this.hive2JdbcUrl = hive2JdbcUrl;
        this.hiveDB = hiveDB;
        this.snapshotTable = snapshotTable;
        this.exportPath = exportPath;
    }

    public String getMetaStoreUris() {
        return metaStoreUris;
    }

    public String getHiveDB() {
        return hiveDB;
    }

    public String getSnapshotTable() {
        return snapshotTable;
    }

    public String getHive2JdbcUrl() {
        return hive2JdbcUrl;
    }

    public String getExportPath() {
        return exportPath;
    }

    public String getFullSnapshotTableName() {
        return hiveDB  + "." + snapshotTable;
    }
}
