HIVE_DB=$1
SNAPSHOT_TABLE=$2

hive -hiveconf CUSTOM_JARS="*.jar" -f export_snapshot.ql