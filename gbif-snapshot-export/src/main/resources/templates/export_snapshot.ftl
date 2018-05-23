USE ${hiveDB};
CREATE TEMPORARY FUNCTION cleanDelimiters AS 'org.gbif.occurrence.hive.udf.CleanDelimiterCharsUDF';

CREATE TABLE export_${snapshotTable} ROW FORMAT DELIMITED FIELDS TERMINATED BY '\t' TBLPROPERTIES ("serialization.null.format"="")
AS
SELECT
<#list colMap as key, value>
  ${key} AS ${value}<#sep>,
</#list>

FROM ${snapshotTable};
