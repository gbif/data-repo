package org.gbif.datarepo.persistence.mappers;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.datarepo.api.model.Identifier;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;

import org.apache.ibatis.annotations.Param;

/**
 * MyBatis mapper to store and manage IdentifierMapper instances.
 */
public interface IdentifierMapper {

  /**
   * Retrieves a DataPackageFile by its doi name and fileName, i.e.: prefix/suffix.
   */
  Identifier get(@Param("key") Integer key);

  /**
   * List AlternativeIdentifier by a data package DOI.
   */
  List<Identifier> listByDataPackageKey(@Nullable @Param("dataPackageKey") UUID dataPackageKey);

  /**
   * Page through AlternativeIdentifiers, optionally filtered by user and dates.
   */
  List<Identifier> list(@Nullable @Param("user") String user, @Nullable @Param("page") Pageable page,
                        @Nullable @Param("type") Identifier.Type type,
                        @Nullable @Param("relationType") Identifier.RelationType relationType,
                        @Nullable @Param("created") Date created, @Nullable @Param("doi") DOI doi);

  /**
   * Count AlternativeIdentifiers, optionally filtered by user.
   */
  Long count(@Nullable @Param("user") String user, @Nullable @Param("page") Pageable page,
             @Nullable @Param("type") Identifier.Type type,
             @Nullable @Param("relationType") Identifier.RelationType relationType,
             @Nullable @Param("created") Date created, @Nullable @Param("doi") DOI doi);


  /**
   * Persists a new data package file.
   */
  void create(Identifier identifier);

  /**
   * Deletes a data package file by its doi and name.
   */
  void delete(@Param("key") Integer key);
}
