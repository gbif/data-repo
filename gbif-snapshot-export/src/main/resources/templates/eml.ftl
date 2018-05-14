
<eml:eml
 packageId="clo-ebird_observation_data_12-12-2017"
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
  		<alternateIdentifier>${doi}</alternateIdentifier>
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
			<para>The eBird Citizen Science Project is a real-time, on line checklist program launched in 2002 by the
                          Cornell Lab of Ornithology and the National Audubon Society. The observations of each volunteer participant
                          join those of others creating a database of international avian observation events. The dataset contains
                          count data for bird species observed by novice and experienced bird observers alike. Aggregated observation
                          data is available for non-commercial research purposes.</para>
			<para>eBird data can be used to document the presence or absence of avian species, as well as abundance
                          and distribution at a variety of spatial and temporal scales. The dataset includes spatial, and
                          temporal parameters describing the observation event.</para>
			<para>The data collection is static, updated annually</para>
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
				<title>Data License</title>
				<para>Data and documentation are copyrighted by Cornell University ("Cornell") and ownership remains
				  with Cornell.</para>
				<para>Cornell hereby grants You (hereafter, Licensee) a fee-free license to use the data and documentation
				  for academic, and research purposes only.  Any commercial use of the data or documentation are expressly
				  prohibited.</para>
				<para>These data are made available through the Cornell Lab of Ornithology at Cornell University and the
				  National Audubon Society. No warranty either expressed or implied is made regarding the accuracy of
				  these data.</para>
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
			<maintenanceUpdateFrequency>quarterly</maintenanceUpdateFrequency>
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
   <entityDescription>Worldwide bird observation data submitted to eBird</entityDescription>
   <physical>
      <objectName>${exportFileName}</objectName>
      <size>${exportFileSize}</size>
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
     <#list terms as term>
      <attribute>
               <attributeName>${term.simpleName()}</attributeName>
               <attributeDefinition>${term.qualifiedName()}</attributeDefinition>
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
     </#list>
   </attributeList>
   <numberOfRecords>${numberOfRecords}</numberOfRecords>
   </dataTable>
 </dataset>
</eml:eml>
