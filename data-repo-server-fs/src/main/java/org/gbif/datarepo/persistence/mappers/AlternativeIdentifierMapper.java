package org.gbif.datarepo.persistence.mappers;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.datarepo.api.model.AlternativeIdentifier;

import java.util.Date;
import java.util.List;
import javax.annotation.Nullable;

import org.apache.ibatis.annotations.Param;

/**
 * MyBatis mapper to store and manage AlternativeIdentifier instances.
 */
public interface AlternativeIdentifierMapper {

  /**
   * Retrieves a DataPackageFile by its doi name and fileName, i.e.: prefix/suffix.
   */
  AlternativeIdentifier get(@Param("identifier") String identifier);

  /**
   * List AlternativeIdentifier by a data package DOI.
   */
  List<AlternativeIdentifier> listByDataPackageDoi(@Nullable @Param("doi") DOI doi);

  /**
   * Page through AlternativeIdentifiers, optionally filtered by user and dates.
   */
  List<AlternativeIdentifier> list(@Nullable @Param("user") String user, @Nullable @Param("page") Pageable page,
                                   @Nullable @Param("type") AlternativeIdentifier.Type type,
                                   @Nullable @Param("created") Date created, @Nullable @Param("doi") DOI doi);

  /**
   * Count AlternativeIdentifiers, optionally filtered by user.
   */
  Long count(@Nullable @Param("user") String user, @Nullable @Param("page") Pageable page,
             @Nullable @Param("type") AlternativeIdentifier.Type type,
             @Nullable @Param("created") Date created, @Nullable @Param("doi") DOI doi);


  /**
   * Persists a new data package file.
   */
  void create(AlternativeIdentifier alternativeIdentifier);

  /**
   * Deletes a data package file by its doi and name.
   */
  void delete(@Param("identifier") String identifier);
}
