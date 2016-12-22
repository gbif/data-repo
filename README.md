# GBIF Open Data Repository

## Vision

### Drivers
  1. Support long-term persistence of biodiversity data shared through the network.
  2. Simplify data publication pathways.
  3. Identify and verify datasets within GBIF network without current owners. 
  4. Develop processes and mechanisms for adoption of orphaned datasets by suitable agencies or experts.
  5. Improve presentation and reporting of data validation results.

Data publishing efforts in the GBIF Community haven been focused on providing tools and standards to facilitate the process of mapping, validation and publishing existing data; the existing publishing data is not always suitable for data created coming from non-traditional GBIF publishers, for example:

  * Snapshots of GBIF data.
  * Custom exports of GBIF data.
  * Data published by time-bounded projects.
  * Datasets no longer available at the original source.
  * Derived/curated versions of unmaintained datasets.
  * Data published in non-GBIF standards.
  * Meaningful publications, scientific papers or documents.

The GBIF Open Data Repository intends to promote an infrastructure to store and share: 
  * Snapshots and custom exports of GBIF data.
  * Valuable biodiversity data which publishing process does not follow current standards or is not supported by the Integrated Publishing Toolkit ([IPT](http://www.gbif.org/ipt)).
  * Datasets coming from sources that are not capable of maintaining a long-term technological platform, examples of those types of sources are short-term projects and contributions from individuals.

### Objectives
  * Support long-term persistence through the integration of GBIF as a DataOne member node.
  * Provide a data repository capable of:
    * Provide long-term storage of dataset of various sizes.
    * Integrate uploaded content with [DOI](https://www.doi.org/) citation and [open licensing](http://opendefinition.org/licenses/) mechanisms implemented in the GBIF network.
    * Validate published data using GBIF available services (see [gbif-data-validator](https://github.com/gbif/gbif-data-validator/)).
    * Data indexing in the GBIF platform.
    * Sharing GBIF derived data (i.e.: snapshots and custom exports) with member of the GBIF network.
    
# Implementation plan

## Milestone 1:  Research available platforms
Implementation of open data repositories is problem that has been tackle by developments efforts made from different communities. Therefore, the initial scope of this project is to research and evaluate available services and open source platforms. To evaluate different options the following variables will be considered:
  1.	Total cost of ownership (TCO): in terms of human and technical resources.
  2.	Usability: quality of a user's experience when interacting with it.
  3.	Extensibility: how easy is to customize it and integrate it with external platforms?
  4.	Compliance with the existing technology stack and standards: DOI, data licensing, data validation and indexing.

### List of platforms 

| Platform | Type | TCO | Usability | Extensibility | Compliance |
| -------- | ---- | --- | --------- | ------------- | ---------- |
| [Dryad](http://datadryad.org/) |  |  |  |  |  |
| [Dspace](http://www.dspace.org/) |  |  |  |  |  |
| [Fedora](http://www.fedora-commons.org/) |  |  |  |  |  |
| [Duraspace](http://www.duraspace.org/) |  |  |  |  |  |
| [Figshare](https://figshare.com/) |  |  |  |  |  |
| [Zeonodo](https://zenodo.org/) |  |  |  |  |  |
| [Invenio](http://invenio-software.org/) |  |  |  |  |  |
| [Eprints](http://www.eprints.org/) |  |  |  |  |  |


## Milestone 2: Evaluation of selected platforms
Platforms that passed the evaluation criterias will be evaluated in a short technical proof of concept.

## Milestone 3: Initial implementation
As the initial implementation, one major requirement has been selected as a functionality that encompasses expected functionalities and features: *support ad hoc exports of GBIF data which can be downloaded, cited and referenced using DOI*.
