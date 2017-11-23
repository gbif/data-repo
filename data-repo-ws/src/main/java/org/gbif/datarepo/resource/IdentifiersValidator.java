package org.gbif.datarepo.resource;

import org.gbif.datarepo.api.DataRepository;
import org.gbif.datarepo.api.model.Identifier;
import org.gbif.datarepo.resource.validation.ResourceValidations;
import org.gbif.datarepo.impl.download.FileDownload;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.ws.rs.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.gbif.datarepo.resource.PathsParams.IDENTIFIERS_FILE_URL_PARAM;
import static org.gbif.datarepo.resource.PathsParams.IDENTIFIERS_FILE_PARAM;

import org.glassfish.jersey.media.multipart.FormDataMultiPart;

/**
 * Utility class to process and validates related identifiers.
 */
public class IdentifiersValidator {

  private static final Logger LOG = LoggerFactory.getLogger(IdentifiersValidator.class);

  /**
   * Class that encapsulates the response of identifiers processing.
   */
  private static class IdentifiersUsage {

    private final Set<Identifier> relatedIdentifiers;

    private final Set<Identifier> alternativeIdentifiersInUse;

    IdentifiersUsage(Set<Identifier> relatedIdentifiers, Set<Identifier> alternativeIdentifiersInUse) {
      this.relatedIdentifiers = relatedIdentifiers;
      this.alternativeIdentifiersInUse = alternativeIdentifiersInUse;
    }

    public Set<Identifier> getRelatedIdentifiers() {
      return relatedIdentifiers;
    }

    public Set<Identifier> getAlternativeIdentifiersInUse() {
      return alternativeIdentifiersInUse;
    }

    public void combine(IdentifiersUsage identifiersUsage) {
      relatedIdentifiers.addAll(identifiersUsage.relatedIdentifiers);
      alternativeIdentifiersInUse.addAll(identifiersUsage.alternativeIdentifiersInUse);
    }
  }

  private final DataRepository dataRepository;
  private final FileDownload fileDownload;

  public IdentifiersValidator(DataRepository dataRepository, FileDownload fileDownload) {
    this.dataRepository = dataRepository;
    this.fileDownload = fileDownload;
  }

  /**
   * Collects all the related identifiers submitted.
   * Validates that identifiersFile and identifiers parameters do not contain alternative identifiers in use.
   */
  public Set<Identifier> validateIdentifiers(FormDataMultiPart multiPart, Collection<Identifier> identifiers) {
    IdentifiersUsage identifiersUsage = processRelatedIdentifiers(multiPart, identifiers);
    if (!identifiersUsage.getAlternativeIdentifiersInUse().isEmpty()) {
      ResourceValidations.throwBadRequest("Identifiers are used as alternative identifiers in another data package: "
                                          + identifiers.stream().map(Identifier::getIdentifier)
                                            .collect(Collectors.joining(" , ")));
    }
    return identifiersUsage.getRelatedIdentifiers();
  }
  /**
   * Is the identifier an alternative identifier and it is being used by any other DataPackage.
   */
  private boolean isInUse(Identifier identifier) {
   return Identifier.RelationType.IsAlternativeOf == identifier.getRelationType()
          && dataRepository.isAlternativeIdentifierInUse(identifier);
  }

  /**
   * Collects all the related identifiers submitted.
   * Validates that identifiersFile and identifiers parameters do not contain alternative identifiers in use.
   */
  private IdentifiersUsage processRelatedIdentifiers(FormDataMultiPart multiPart, Collection<Identifier> identifiers) {
    //Collects identifiers submitted as JSON/List
    IdentifiersUsage identifiersUsage = processRelatedIdentifiers(identifiers);

    //Add the identifiers submitted as file
    Optional.ofNullable(multiPart.getField(IDENTIFIERS_FILE_PARAM))
      .map(bodyPart -> processRelatedIdentifiers(bodyPart.getValueAs(InputStream.class)))
      .ifPresent(identifiersUsage::combine);

    //Add the identifiers submitted as file url
    Optional.ofNullable(multiPart.getField(IDENTIFIERS_FILE_URL_PARAM))
      .map(bodyPart -> {
        try {
          URI uri = new URI(bodyPart.getValue());
          return  processRelatedIdentifiers(fileDownload.openStream(uri));
        } catch (URISyntaxException | IOException ex) {
          String message = String.format("Wrong URI %s", bodyPart.getValue());
          LOG.error(message, ex);
          throw new BadRequestException(message) ;
        }
      }).ifPresent(identifiersUsage::combine);

    return identifiersUsage;
  }

  /**
   * Collects and validates the identifiers submitted as a list.
   */
  private IdentifiersUsage processRelatedIdentifiers(Collection<Identifier> identifiers) {
    Set<Identifier> relatedIdentifiers = new HashSet<>();
    Optional.ofNullable(identifiers).ifPresent(relatedIdentifiers::addAll);
    Set<Identifier> identifiersInUse = new HashSet<>(relatedIdentifiers.stream().filter(this::isInUse)
                                                       .collect(Collectors.toSet()));
    return new IdentifiersUsage(relatedIdentifiers, identifiersInUse);
  }

  /**
   * Collects and validates an identifiersFile.
   */
  private IdentifiersUsage processRelatedIdentifiers(InputStream identifiersFile) {
    Set<Identifier> relatedIdentifiers = new HashSet<>();
    Set<Identifier> identifiersInUse = new HashSet<>();
    try(BufferedReader reader = new BufferedReader(new InputStreamReader(identifiersFile))) {
      String line;
      while((line = reader.readLine()) != null){
        asIdentifier(line).ifPresent(relatedIdentifier -> {
                                       if (isInUse(relatedIdentifier)) {
                                         identifiersInUse.add(relatedIdentifier);
                                       } else {
                                         relatedIdentifiers.add(relatedIdentifier);
                                       }
                                     });

      }
    } catch (IOException ex) {
      throw new IllegalStateException(ex);
    }
    return new IdentifiersUsage(relatedIdentifiers, identifiersInUse);
  }

  /**
   * Translates a line in the format 'identifier,identifierType,relationType' into an Identifier instance.
   */
  private static Optional<Identifier> asIdentifier(String line) {
    String[] identifierLine = line.split(",");
    if (identifierLine.length == 3) {
      Identifier identifier = new Identifier();
      identifier.setIdentifier(identifierLine[0]);
      identifier.setType(Identifier.Type.valueOf(identifierLine[1]));
      identifier.setRelationType(Identifier.RelationType.valueOf(identifierLine[2]));
      return Optional.of(identifier);
    }
    if (identifierLine.length == 2) {
      Identifier identifier = new Identifier();
      identifier.setIdentifier(identifierLine[0]);
      identifier.setType(Identifier.Type.valueOf(identifierLine[1].toUpperCase()));
      identifier.setRelationType(Identifier.RelationType.References);
      return Optional.of(identifier);
    }
    if (identifierLine.length == 1) {
      Identifier identifier = new Identifier();
      identifier.setIdentifier(identifierLine[0]);
      identifier.setType(Identifier.Type.DOI);
      identifier.setRelationType(Identifier.RelationType.References);
      return Optional.of(identifier);
    }
    return Optional.empty();
  }

}
