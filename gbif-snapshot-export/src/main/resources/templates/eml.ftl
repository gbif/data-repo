<eml:eml
 packageId="${doi}"
 system="http://dataone.ornith.cornell.edu/"
 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
 xmlns:eml="eml://ecoinformatics.org/eml-2.1.1"
 xsi:schemaLocation="eml://ecoinformatics.org/eml-2.1.1 eml.xsd">
   	<access authSystem="dataone" order="allowFirst">
        <allow>
            <principal>public</principal>
         	<permission>read</permission>
        </allow>
   	</access>
	<dataset>
  		<alternateIdentifier>gbif_occurrence_data_${exportDate}</alternateIdentifier>
  		<title>GBIF Snapshot - ${exportDate}</title>
	  	<creator>
        <organizationName>GBIF - Global Biodiversity Information Facility</organizationName>
        <address>
          <deliveryPoint>Universitetsparken 15</deliveryPoint>
          <city>Copenhagen</city>
          <administrativeArea>Østerbro</administrativeArea>
          <postalCode>2100</postalCode>
          <country>Denmark</country>
        </address>
        <onlineUrl>http://www.gbif.org</onlineUrl>
		  </creator>
		<metadataProvider>
			<organizationName>GBIF - Global Biodiversity Information Facility</organizationName>
			<address>
				<deliveryPoint>Universitetsparken 15</deliveryPoint>
				<city>Copenhagen</city>
				<administrativeArea>Østerbro</administrativeArea>
				<postalCode>2100</postalCode>
				<country>Denmark</country>
			</address>
			<onlineUrl>http://www.gbif.org</onlineUrl>
		</metadataProvider>
   		<pubDate>${exportDate}</pubDate>
		<abstract>
			<para>GBIF—the Global Biodiversity Information Facility—is an international network and research infrastructure
			funded by the world’s governments and aimed at providing anyone, anywhere, open access to data about all types of life on Earth.
			The dataset contains occurrence records published through GBIF.org by different members of the GBIF network.</para>
		</abstract>
		<keywordSet>
			<keyword>gbif</keyword>
			<keyword>biodiversity</keyword>
			<keyword>observation</keyword>
			<keyword>checklist</keyword>
			<keyword>count</keyword>
			<keyword>species</keyword>
			<keyword>taxon</keyword>
			<keyword>taxonomic</keyword>
			<keyword>international</keyword>
			<keyword>location</keyword>
			<keyword>latitude</keyword>
			<keyword>longitude</keyword>
			<keyword>date</keyword>
		</keywordSet>
		<intellectualRights>
			<section>
				<title>Data License - CC BY-NC</title>
				<para>Data are made available for any use provided that attribution is appropriately given and provided the use is not for commercial purposes</para>
			</section>
		</intellectualRights>
		<coverage>
			<geographicCoverage>
				<geographicDescription>Worldwide</geographicDescription>
				<boundingCoordinates>
					<westBoundingCoordinate>-180.0</westBoundingCoordinate>
					<eastBoundingCoordinate>+180.0</eastBoundingCoordinate>
					<northBoundingCoordinate>+90.0</northBoundingCoordinate>
					<southBoundingCoordinate>-90.0</southBoundingCoordinate>
				</boundingCoordinates>
			</geographicCoverage>
		</coverage>
		<maintenance>
			<description>
				<para>Released quarterly</para>
			</description>
			<maintenanceUpdateFrequency>otherMaintenancePeriod</maintenanceUpdateFrequency>
		</maintenance>
   		<contact>
      		<individualName>
         		<givenName>Tim</givenName>
         		<surName>Robertson</surName>
      		</individualName>
			<address>
                <deliveryPoint>Universitetsparken 15</deliveryPoint>
                <city>Copenhagen</city>
                <administrativeArea>Østerbro</administrativeArea>
                <postalCode>2100</postalCode>
                <country>Denmark</country>
            </address>
      		<electronicMailAddress>informatics@gbif.org</electronicMailAddress>
   		</contact>


<dataTable>
   <entityName>${exportFileName}</entityName>
   <entityDescription>GBIF snapshot of occurrence data</entityDescription>
   <physical>
      <objectName>${exportFileName}</objectName>
      <size>${exportFileSize?string.computer}</size>
      <compressionMethod>gzip</compressionMethod>
      <dataFormat>
         <textFormat>
           <numHeaderLines>1</numHeaderLines>
           <recordDelimiter>\n</recordDelimiter>
           <attributeOrientation>column</attributeOrientation>
           <simpleDelimited>
              <fieldDelimiter>TAB</fieldDelimiter>
           </simpleDelimited>
        </textFormat>
      </dataFormat>

   </physical>

   <attributeList>
      <attribute>
        <attributeName>gbifID</attributeName>
        <attributeDefinition>http://rs.gbif.org/terms/1.0/gbifID</attributeDefinition>
        <measurementScale>
            <nominal>
               <nonNumericDomain>
                  <textDomain>
                     <definition>Non-specified text</definition>
                  </textDomain>
               </nonNumericDomain>
            </nominal>
         </measurementScale>
      </attribute>
     <#list terms as term>
      <attribute>
               <attributeName>${term.simpleName()}</attributeName>
               <attributeDefinition>${term.qualifiedName()}</attributeDefinition>
               <measurementScale>
               <#if term.simpleName() == "decimalLatitude">
                    <ratio>
                        <unit>
                            <standardUnit>degree</standardUnit>
                        </unit>
                        <numericDomain>
                            <numberType>real</numberType>
                            <bounds>
                                <minimum exclusive="false">-90</minimum>
                                <maximum exclusive="false">90</maximum>
                            </bounds>
                        </numericDomain>
                    </ratio>
               <#elseif term.simpleName() == "decimalLongitude">
                        <ratio>
                            <unit>
                                <standardUnit>degree</standardUnit>
                            </unit>
                            <numericDomain>
                                <numberType>real</numberType>
                                <bounds>
                                    <minimum exclusive="false">-180</minimum>
                                    <maximum exclusive="false">180</maximum>
                                </bounds>
                            </numericDomain>
                        </ratio>
               <#else>
                       <nominal>
                          <nonNumericDomain>
                             <textDomain>
                                <definition>Non-specified text</definition>
                             </textDomain>
                          </nonNumericDomain>
                       </nominal>
                </#if>
                </measurementScale>
       </attribute>
     </#list>
   </attributeList>
   <numberOfRecords>${numberOfRecords?string.computer}</numberOfRecords>
   </dataTable>
 </dataset>
</eml:eml>
