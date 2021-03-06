SET hive.exec.compress.output=true;
SET io.seqfile.compression.type=BLOCK;
SET mapred.output.compression.codec=org.gbif.hadoop.compress.d2.D2Codec;
SET io.compression.codecs=org.gbif.hadoop.compress.d2.D2Codec;
<#if thisJar??>
ADD JAR ${thisJar};
</#if>
USE ${hiveDB};

DROP TABLE IF EXISTS export_${snapshotTable};

CREATE TABLE export_${snapshotTable} ROW FORMAT DELIMITED FIELDS TERMINATED BY '\t' TBLPROPERTIES ("serialization.null.format"="")
AS
SELECT
  id AS gbifID,
<#list colMap as key, value>
  ${value} AS ${key}<#sep>,
</#list>

FROM ${snapshotTable};
