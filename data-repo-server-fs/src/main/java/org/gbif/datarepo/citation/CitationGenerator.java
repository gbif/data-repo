package org.gbif.datarepo.citation;

import org.gbif.datarepo.api.model.Creator;
import org.gbif.datarepo.api.model.DataPackage;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

public class CitationGenerator {

  private static final ZoneId UTC = ZoneId.of("UTC");

  /**
   * Utility class
   */
  private CitationGenerator(){}


  /**
   * Generate a citation for a {@link DataPackage}.
   * TODO add support for i18n
   * @param dataPackage
   * @return generated citation as {@link String}
   */
  public static String generateCitation(DataPackage dataPackage) {

    Objects.requireNonNull(dataPackage, "DataPackage shall be provided");

    StringJoiner joiner = new StringJoiner(" ");
    List<String> authorsName = generateAuthorsName(dataPackage.getCreators());
    String authors = authorsName.stream().collect(Collectors.joining(", "));

    //only add a dot if we are not gonna add it with the year
    authors += dataPackage.getCreated() == null ? "." : "";
    joiner.add(authors);

    if (dataPackage.getCreated() != null) {
      joiner.add("(" + dataPackage.getCreated().toInstant().atZone(UTC).getYear() + ").");
    }

    // add title
    joiner.add(StringUtils.trim(dataPackage.getTitle()) + ".");

    joiner.add("DataPackage");

    // add DOI as the identifier.
    if (dataPackage.getDoi() != null) {
      joiner.add(dataPackage.getDoi().getUrl().toString());
    }

    joiner.add("accessed via GBIF.org on " + LocalDate.now(UTC) + ".");

    return joiner.toString();
  }

  /**
   * Given a list of authors, generates a {@link List} of {@link String} representing the creators name.
   *
   * @param creators ordered list of authors
   * @return list of creator names (if it can be generated) or empty list, never null
   */
  public static List<String> generateAuthorsName(Collection<Creator> creators) {
    if (creators == null || creators.isEmpty()) {
      return Collections.emptyList();
    }

    return creators.stream()
      .map(Creator::getName)
      .collect(Collectors.toList());
  }

}
