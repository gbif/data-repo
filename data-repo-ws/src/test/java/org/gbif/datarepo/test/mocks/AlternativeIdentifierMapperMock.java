package org.gbif.datarepo.test.mocks;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.datarepo.api.model.AlternativeIdentifier;
import org.gbif.datarepo.persistence.mappers.AlternativeIdentifierMapper;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;

import org.apache.ibatis.annotations.Param;

public class AlternativeIdentifierMapperMock implements AlternativeIdentifierMapper {

  @Override
  public AlternativeIdentifier get(@Param("identifier") String identifier) {
    AlternativeIdentifier alternativeIdentifier = new AlternativeIdentifier();
    alternativeIdentifier.setType(AlternativeIdentifier.Type.UUID);
    alternativeIdentifier.setCreated(new Date());
    alternativeIdentifier.setCreatedBy("testUser");
    alternativeIdentifier.setIdentifier(UUID.randomUUID().toString());
    return alternativeIdentifier;
  }

  @Override
  public List<AlternativeIdentifier> listByDataPackageDoi(
    @Nullable @Param("doi") DOI doi
  ) {
    return new ArrayList<>();
  }

  @Override
  public List<AlternativeIdentifier> list(
    @Nullable @Param("user") String user,
    @Nullable @Param("page") Pageable page,
    @Nullable @Param("type") AlternativeIdentifier.Type type,
    @Nullable @Param("created") Date created,
    @Nullable @Param("doi") DOI doi
  ) {
    return new ArrayList<>();
  }

  @Override
  public Long count(
    @Nullable @Param("user") String user,
    @Nullable @Param("page") Pageable page,
    @Nullable @Param("type") AlternativeIdentifier.Type type,
    @Nullable @Param("created") Date created,
    @Nullable @Param("doi") DOI doi
  ) {
    return 1L;
  }

  @Override
  public void create(AlternativeIdentifier alternativeIdentifier) {
    // do nothing
  }

  @Override
  public void delete(@Param("identifier") String identifier) {
    // do nothing
  }
}
