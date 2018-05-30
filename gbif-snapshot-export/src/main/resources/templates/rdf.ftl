<?xml version="1.0" encoding="UTF-8"?>
<rdf:RDF
   xmlns:cito="http://purl.org/spar/cito/"
   xmlns:dc="http://purl.org/dc/elements/1.1/"
   xmlns:dcterms="http://purl.org/dc/terms/"
   xmlns:foaf="http://xmlns.com/foaf/0.1/"
   xmlns:ore="http://www.openarchives.org/ore/terms/"
   xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
   xmlns:rdfs1="http://www.w3.org/2001/01/rdf-schema#">
  <rdf:Description rdf:about="http://www.openarchives.org/ore/terms/ResourceMap">
    <rdfs1:label>ResourceMap</rdfs1:label>
    <rdfs1:isDefinedBy>http://www.openarchives.org/ore/terms/</rdfs1:isDefinedBy>
  </rdf:Description>
  <rdf:Description rdf:about="https://cn.dataone.org/cn/v1/resolve/${snapshotTable}.">
    <ore:describes rdf:resource="https://cn.dataone.org/cn/v1/resolve/${snapshotTable}.#aggregation"/>
    <dcterms:creator rdf:resource="https://github.com/gbif/data-repo"/>
    <rdf:type rdf:resource="http://www.openarchives.org/ore/terms/ResourceMap"/>
    <dcterms:identifier>${snapshotTable}.</dcterms:identifier>
    <dcterms:created>${exportDate}</dcterms:created>
    <dc:format>application/rdf+xml</dc:format>
    <dcterms:modified>${exportDate}</dcterms:modified>
  </rdf:Description>
  <rdf:Description rdf:about="http://www.openarchives.org/ore/terms/Aggregation">
    <rdfs1:isDefinedBy>http://www.openarchives.org/ore/terms/</rdfs1:isDefinedBy>
    <rdfs1:label>Aggregation</rdfs1:label>
  </rdf:Description>
  <rdf:Description rdf:about="https://cn.dataone.org/cn/v1/resolve/${snapshotTable}.csv.gz">
    <cito:documents rdf:resource="https://cn.dataone.org/cn/v1/resolve/${snapshotTable}.eml"/>
    <dcterms:identifier>${snapshotTable}.csv.gz</dcterms:identifier>
  </rdf:Description>
  <rdf:Description rdf:about="https://cn.dataone.org/cn/v1/resolve/${snapshotTable}.#aggregation">
    <ore:aggregates rdf:resource="https://cn.dataone.org/cn/v1/resolve/${snapshotTable}.csv.gz"/>
    <ore:aggregates rdf:resource="https://cn.dataone.org/cn/v1/resolve/${snapshotTable}.eml"/>
    <rdf:type rdf:resource="http://www.openarchives.org/ore/terms/Aggregation"/>
  </rdf:Description>
  <rdf:Description rdf:about="https://cn.dataone.org/cn/v1/resolve/${snapshotTable}.eml">
    <dcterms:identifier>${snapshotTable}.eml</dcterms:identifier>
    <cito:isDocumentedBy rdf:resource="https://cn.dataone.org/cn/v1/resolve/${snapshotTable}.csv.zip"/>
  </rdf:Description>
</rdf:RDF>