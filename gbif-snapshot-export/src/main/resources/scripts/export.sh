HIVE_DB=$1
SNAPSHOT_TABLE=$2

hadoop dfs -getmerge /user/hive/warehouse/$($HIVE_DB).db/$SNAPSHOT_TABLE