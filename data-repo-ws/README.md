#Datata Repository Web Service
This project contains a RESTful application that exposes the services provided by the GBIF Data Repository.
 
 
##Endpoints

### Data Package creation

* Description: creates a data package using JSON definition, a list of files and an optional file containing related identifiers
* URL: */data_packages/* 
* Method: POST 
* Authentication: yes 
* Parameters format: *multipart/form-data* 
* Parameters:

| Parameter | Format | Required | Multiplicity | Validation |
| ------- | ------- | --------- | ------------- | --------- |
| `file`  | content-disposition, file field | No | Multiple | at least one input file must bu supplied in combination with fileUrl param |
| `fileUrl` | form input field,  supported schemes are HDFS, HTTP(S) and FTP(s) | No | Multiple | at least one input file must be supplied in combination with fileUrl param |
| `dataPackage` | form input field containing JSON data of the data package to be created | Yes | Single | required fields are: title, description and license |
| `identifiersFile` | content-disposition, file field | No | Single | See "Identifiers file validation" below |
| `identifiersFileUrl` | form input field,  supported schemes are HDFS, HTTP(S) and FTP(s) | No | Single | See "Identifiers file validation" below |

  * Identifiers file validation: The expected format for this file is identifier, identifierType and relationType. A line can contain only the identifier making the default values *identifierType=DOI* and *relationType=references*. 
       
* Response type: *application/json*
* Success Response: 
  * Code: 200 OK
  * Content: a JSON representation of the created data package containing the key generated identifiers and DOI 
* Error Response:
  * Code: 401 UNAUTHORIZED
  * Content: authentication error message
  
  OR
  
  * Code: 400 BAD REQUEST
  * Content: error message
  * Conditions:
    * An alternative identifier is already being used by another data packag.
    * At least one file should be specified using the parameters *url* and *fileUrl*
    * A required field is missing in the data package JSON parameter
  

### GET Data Package 

* Description: retrieves the information about a data package
* URL: */data_packages/{identifier}* 
* Method: GET
* Parameter: the parameter `identifier` is part of the URL and can contain an UUID (data package key), a DOI or alternative identifier 
* Authentication: no 
* Response type: *application/json*
* Success Response: 
  * Code: 200 OK
  * Content: a JSON representation of the requested data package. 
* Error Response:
  * Code: 404 NOT FOUND
  * Content: identifier not found  

### GET Data Package Metadata

* Description: retrives the DataCite metadata associated to a data package
* URL: */data_packages/{identifier}/metadata* 
* Method: GET
* Parameter: `identifier` is part of the URL, it must contain an UUID (data package key), a DOI or alternative identifier 
* Authentication: no 
* Response type: *application/xml*
* Success Response: 
  * Code: 200 OK
  * Content: a xml file containing the DataCite metadata generated for the requested identifier 
* Error Response:
  * Code: 404 NOT FOUND
  * Content: identifier or metadata not found  

### GET Data Package File

* Description: downloads a file contained in data package 
* URL: */data_packages/{identifier}/{fileName}* 
* Method: GET
* Parameters: 
  * `identifier`: part of the URL, it must contain an UUID (data package key), a DOI or alternative identifier
  * `fileName`: is part of the URL and must be a valid file contained in the data package
* Authentication: no 
* Response type: *application/octet-stream*
* Success Response: 
  * Code: 200 OK
  * Content: a file content stream. 
* Error Response:
  * Code: 404 NOT FOUND
  * Content: identifier or file name not found

### GET Data Package File

* Description: retrieves metadata about a file contained in a data package
* URL: */data_packages/{identifier}/{fileName}/data* 
* Method: GET
* Parameters: 
  * `identifier`: is part of the URL and can contain an UUID (data package key), a DOI or alternative identifier
  * `fileName`: is part of the URL and must be a valid file contained in the data package
* Authentication: no 
* Response type: *application/json*
* Success Response: 
  * Code: 200 OK
  * Content: a JSON response containing basic metadata (name, size and checksum) about the requested file name 
* Error Response:
  * Code: 404 NOT FOUND
  * Content: identifier or file name not found


### DELETE Data Package 

* Description: deletes a data package
* URL: */data_packages/{identifier}* 
* Method: DELETE
* Parameters: 
  * `identifier`: part of the URL, it must contain an UUID (data package key), a DOI or alternative identifier
* Authentication: yes 
* Response type: *application/json*
* Success Response: 
  * Code: 200 OK
  * Content: a JSON response containing basic metadata (name, size and checksum) about the requested file name 
* Error Response:
  * Code: 401 UNAUTHORIZED
  * Content: authentication error message
  
  OR
    
  * Code: 404 NOT FOUND
  * Content: identifier or file name not found

### List/Search Data Packages

* Description: search and list data packages
* URL: */data_packages/* 
* Method: GET
* Parameters: 

| Parameter | Format | Required | Multiplicity | Validation |
| ------- | ------- | --------- | ------------- | --------- |
| `user`  | query string parameter | No | Single | any string, it will be match against the user who created a data package |
| `offset` | query string parameter | No | Single | positive integer, how many items to skip before beginning to return rows  |
| `limit` | query string parameter | No  | Single | positive integer, maximum number of results to return |
| `fromDate` | query string parameter | No | Single | a date as a string value in UTC format, list items form this date |
| `toDate` | query string parameter | No | Single | a date as a string value in UTC format, list items up to this date |
| `tag` | query string parameter | No | Multiple | a string containing a data package tag |
| `q` | query string parameter | No | Single | a string containing a full text search parameter |
   
* Authentication: no 
* Response type: *application/json*
* Success Response: 
  * Code: 200 OK
  * Content: a JSON response containing basic metadata (name, size and checksum) about the requested file name 

### List/Search Data Packages Identifiers

* Description: search and list data packages identifiers
* URL: */data_packages/{dataPackageIdentifier}* 
* Method: GET
* Parameters: 

| Parameter | Format | Required | Multiplicity | Validation |
| ------- | ------- | --------- | ------------- | --------- |
| `dataPackageIdentifier`  | part of the URL path | yes | Single | part of the URL, it must contain an UUID (data package key), a DOI or alternative identifier |
| `user`  | query string parameter | No | Single | any string, it will be match against the user who created a data package |
| `offset` | query string parameter | No | Single | positive integer, how many items to skip before beginning to return rows  |
| `limit` | query string parameter | No  | Single | positive integer, maximum number of results to return |
| `identifier` | query string parameter | Single | Multiple | a string containing a data package tag |
| `relationType` | query string parameter | No | Single | a string containing a valid identifier relation type |
| `type` | query string parameter | No | Single | a string containing a valid identifier type |
| `created` | query string parameter | No | Single | a date as a string value in UTC format, list items created at this date |
   
* Authentication: no 
* Response: *application/json*
* Success Response: 
  * Code: 200 OK
  * Content: a JSON response containing basic metadata (name, size and checksum) about the requested file name 
