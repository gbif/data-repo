package org.gbif.datarepo.impl.metadata;

import org.gbif.datarepo.api.model.Creator;
import org.gbif.datarepo.api.model.DataPackage;
import org.gbif.datarepo.api.model.Identifier;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.doi.metadata.datacite.DateType;
import org.gbif.doi.metadata.datacite.DescriptionType;
import org.gbif.doi.metadata.datacite.RelatedIdentifierType;
import org.gbif.doi.metadata.datacite.RelationType;
import org.gbif.doi.service.InvalidMetadataException;
import org.gbif.doi.service.datacite.DataCiteValidator;

import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;

import com.fasterxml.jackson.databind.util.ISO8601Utils;

/**
 * Utility class that generates a DataCiteMetadata instance or xml from a DataPackage object.
 */
public class DataCiteMetadataGenerator {

  /**
   * Private constructor.
   */
  private DataCiteMetadataGenerator() {
    //do nothing
  }

  /**
   * Generates a String containing a XML document with the DataCite metadata content of DataPackage.
   */
  public static String toXmlDataCiteMetadata(DataPackage dataPackage) throws InvalidMetadataException {
      return DataCiteValidator.toXml(toDataCiteMetadata(dataPackage), false);
  }

  /**
   * Generates a DataCiteMetadata from a DataPackage.
   */
  public static DataCiteMetadata toDataCiteMetadata(DataPackage dataPackage) {
    Date metadataCreationDate = Optional.ofNullable(dataPackage.getCreated()).orElse(new Date());
    DataCiteMetadata.Builder<Void> builder = DataCiteMetadata.builder();
    return
      //Add alternative and related identifiers
      withIdentifiers(builder, dataPackage)
        //Add creators
        .withCreators(getCreators(dataPackage))
        //Title
        .withTitles(DataCiteMetadata.Titles.builder()
                      .withTitle(DataCiteMetadata.Titles.Title.builder()
                                   .withValue(dataPackage.getTitle())
                                   .build())
                      .build())
        //Dates
        .withDates(DataCiteMetadata.Dates.builder()
                     .withDate(DataCiteMetadata.Dates.Date.builder()
                                 .withDateType(DateType.CREATED)
                                 .withValue(ISO8601Utils.format(metadataCreationDate))
                                 .build())
                     .build())
        .withPublisher(dataPackage.getCreatedBy())
        .withPublicationYear(String.valueOf(metadataCreationDate.toInstant()
                                              .atZone(ZoneId.systemDefault()).getYear()))
        //Description
        .withDescriptions(DataCiteMetadata.Descriptions.builder()
                            .withDescription(DataCiteMetadata.Descriptions.Description
                                               .builder()
                                               .withContent(dataPackage.getDescription())
                                               .withDescriptionType(DescriptionType.ABSTRACT)
                                               .build())
                            .build())
        .build();
  }

  /**
   * Adds related and alternative identifiers to the DataCiteMetadata.Builder.
   */
  private static DataCiteMetadata.Builder<Void> withIdentifiers(DataCiteMetadata.Builder<Void> builder,
                                                                DataPackage dataPackage) {
    DataCiteMetadata.RelatedIdentifiers.Builder<Void> relatedIdentifiers = DataCiteMetadata.RelatedIdentifiers.builder();
    DataCiteMetadata.AlternateIdentifiers.Builder<Void> alternateIdentifiers = DataCiteMetadata.AlternateIdentifiers.builder();
    Optional.ofNullable(dataPackage.getRelatedIdentifiers()).ifPresent(
      dpRelatedIdentifiers -> dpRelatedIdentifiers.forEach(identifier -> {
        if (Identifier.RelationType.IsAlternativeOf == identifier.getRelationType()) {
          alternateIdentifiers.addAlternateIdentifier(DataCiteMetadata.AlternateIdentifiers.AlternateIdentifier
                                                        .builder()
                                                        .withValue(identifier.getIdentifier())
                                                        .withAlternateIdentifierType(identifier.getRelationType().name())
                                                        .build());
        } else {
          asDataCiteRelatedIdentifier(identifier).ifPresent(relatedIdentifiers::addRelatedIdentifier);
        }
      }));
    return builder.withAlternateIdentifiers(alternateIdentifiers.build()).withRelatedIdentifiers(relatedIdentifiers.build());
  }

  /**
   * If possible, translates a Identifier into a DataCiteMetadata.RelatedIdentifiers.RelatedIdentifier.
   */
  private static Optional<DataCiteMetadata.RelatedIdentifiers.RelatedIdentifier> asDataCiteRelatedIdentifier(Identifier identifier) {
    Optional<RelatedIdentifierType> identifierType = asDataCiteRelatedIdentifierType(identifier.getType());
    Optional<RelationType> relationType = asDataCiteRelationType(identifier.getRelationType());
    if (identifierType.isPresent() && relationType.isPresent()) {
      DataCiteMetadata.RelatedIdentifiers.RelatedIdentifier.builder()
        .withRelatedIdentifierType(identifierType.get())
        .withValue(identifier.getIdentifier())
        .withRelationType(relationType.get())
        .build();
    }
    return Optional.empty();
  }

  /**
   * Converts a Identifier.Type to a RelatedIdentifierType, if possible.
   */
  private static Optional<RelatedIdentifierType> asDataCiteRelatedIdentifierType(Identifier.Type identifierType) {
    if (Identifier.Type.DOI == identifierType ) {
      return Optional.of(RelatedIdentifierType.DOI);
    } else if (Identifier.Type.URL == identifierType ) {
      return Optional.of(RelatedIdentifierType.URL);
    } else if (Identifier.Type.GBIF_DATASET_KEY == identifierType ) {
      return Optional.of(RelatedIdentifierType.URL);
    }
    return Optional.empty();
  }

  /**
   * Converts a Identifier.Type to a RelatedIdentifierType, if possible.
   */
  private static Optional<RelationType> asDataCiteRelationType(Identifier.RelationType relationType) {
    try {
      return Optional.of(RelationType.fromValue(relationType.name()));
    } catch (IllegalArgumentException ex) {
      return Optional.empty();
    }
  }

  /**
   * Adds related and alternative identifiers to the DataCiteMetadata.Builder.
   */
  private static DataCiteMetadata.Creators getCreators(DataPackage dataPackage) {
    DataCiteMetadata.Creators.Builder<Void> creators = DataCiteMetadata.Creators.builder();
    creators.addCreator(DataCiteMetadata.Creators.Creator.builder().withCreatorName(dataPackage.getCreatedBy())
                          .build());
    Optional.ofNullable(dataPackage.getCreators())
      .ifPresent(dpCreators -> dpCreators.forEach(dpCreator ->
                                                    creators.addCreator(DataCiteMetadata.Creators.Creator.builder()
                                                                          .withCreatorName(dpCreator.getName())
                                                                          .withNameIdentifier(getNameIdentifier(dpCreator)
                                                                                                .orElse(null))
                                                                          .build())
      ));
    return creators.build();
  }

  /**
   * Extracts the name identifier from a data package creator.
   */
  private static Optional<DataCiteMetadata.Creators.Creator.NameIdentifier> getNameIdentifier(Creator creator) {
    return Optional.ofNullable(creator.getIdentifier()).map( id ->
              DataCiteMetadata.Creators.Creator.NameIdentifier.builder()
                .withNameIdentifierScheme(
                  Optional.ofNullable(creator.getIdentifierScheme())
                    .map(Creator.IdentifierScheme::name).orElse(null))
                .withValue(creator.getIdentifier())
                .withSchemeURI(creator.getSchemeURI())
                .build()
            );
  }
}
