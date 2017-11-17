package org.gbif.datarepo.persistence;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.datarepo.api.DataRepository;
import org.gbif.datarepo.api.model.DataPackage;
import org.gbif.datarepo.api.model.DataPackageFile;
import org.gbif.datarepo.api.model.Identifier;
import org.gbif.datarepo.api.model.RepositoryStats;
import org.gbif.datarepo.persistence.mappers.CreatorMapper;
import org.gbif.datarepo.persistence.mappers.DataPackageFileMapper;
import org.gbif.datarepo.persistence.mappers.DataPackageMapper;
import org.gbif.datarepo.persistence.mappers.IdentifierMapper;
import org.gbif.datarepo.persistence.mappers.RepositoryStatsMapper;
import org.gbif.datarepo.persistence.mappers.TagMapper;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import javax.inject.Inject;

public class DataRepoPersistenceService {

  private static final PagingRequest EMPTY_PAGE = new PagingRequest(0,0);

  private final DataPackageMapper dataPackageMapper;

  private final DataPackageFileMapper dataPackageFileMapper;

  private final IdentifierMapper identifierMapper;

  private final RepositoryStatsMapper repositoryStatsMapper;

  private final TagMapper tagMapper;

  private final CreatorMapper creatorMapper;

  /**
   * Executes the listing operation if the page is not null and the pageable.limit > 0.
   */
  private static <L> PagingResponse<L> doPageableResponse(Pageable pageable,
                                                          Supplier<Long> countSupplier,
                                                          Supplier<List<L>> listSupplier) {
    Pageable pagingRequest = Optional.ofNullable(pageable).orElse(EMPTY_PAGE);
    List<L> result =  pagingRequest.getLimit() > 0 ? listSupplier.get() : Collections.emptyList();
    return new PagingResponse<>(pagingRequest, countSupplier.get(), result);
  }

  @Inject
  public DataRepoPersistenceService(DataPackageMapper dataPackageMapper, DataPackageFileMapper dataPackageFileMapper,
                                    IdentifierMapper identifierMapper, RepositoryStatsMapper repositoryStatsMapper,
                                    TagMapper tagMapper, CreatorMapper creatorMapper) {
    this.dataPackageMapper = dataPackageMapper;
    this.dataPackageFileMapper = dataPackageFileMapper;
    this.identifierMapper = identifierMapper;
    this.repositoryStatsMapper = repositoryStatsMapper;
    this.tagMapper = tagMapper;
    this.creatorMapper = creatorMapper;
  }

  public DataPackage create(DataPackage dataPackage) {
    //Persist data package info
    dataPackageMapper.create(dataPackage);
    dataPackage.getFiles()
      .forEach(dataPackageFile -> dataPackageFileMapper.create(dataPackage.getKey(), dataPackageFile));
    dataPackage.getRelatedIdentifiers().forEach(identifierMapper::create);
    dataPackage.getTags().forEach(tagMapper::create);
    dataPackage.getCreators().forEach(creatorMapper::create);
    return getDataPackage(dataPackage.getKey());
  }

  public void createDataPackageFile(UUID dataPackageKey, DataPackageFile dataPackageFile) {
    dataPackageFileMapper.create(dataPackageKey, dataPackageFile);
  }

  public DataPackage update(DataPackage dataPackage, DataRepository.UpdateMode updateMode) {
    //deletes existing associated elements
    UUID dataPackageKey = dataPackage.getKey();
    DataPackage existingDataPackage = getDataPackage(dataPackageKey);
    existingDataPackage.getRelatedIdentifiers()
      .forEach(alternativeIdentifier -> identifierMapper.delete(alternativeIdentifier.getKey()));
    existingDataPackage.getTags().forEach(tag -> tagMapper.delete(tag.getKey()));
    existingDataPackage.getCreators().forEach(creator -> creatorMapper.delete(creator.getKey()));

    if (DataRepository.UpdateMode.APPEND == updateMode) {
      //Delete existing files
      existingDataPackage.getFiles()
        .stream()
        .filter(existingFile -> dataPackage.getFiles().stream()
          .anyMatch(file -> file.getFileName().equalsIgnoreCase(existingFile.getFileName())))
        .forEach(fileToDelete -> dataPackageFileMapper.delete(dataPackageKey, fileToDelete.getFileName()));
    } else { //Is  DataRepository.UpdateMode.OVERWRITE
      existingDataPackage.getFiles().forEach(existingFile -> dataPackageFileMapper.delete(dataPackageKey,
                                                                                          existingFile.getFileName()));
    }
    //update data package info
    dataPackageMapper.update(dataPackage);

    //re-create associated elements
    dataPackage.getRelatedIdentifiers().forEach(identifierMapper::create);
    dataPackage.getTags().forEach(tagMapper::create);
    dataPackage.getCreators().forEach(creatorMapper::create);
    dataPackage.getFiles().forEach(dataPackageFile -> dataPackageFileMapper.create(dataPackageKey, dataPackageFile));
    return getDataPackage(dataPackageKey);
  }

  public DataPackage getDataPackage(UUID dataPackageKey) {
    return dataPackageMapper.getByKey(dataPackageKey);
  }

  public DataPackage getDataPackage(DOI dataPackageDoi) {
    return dataPackageMapper.getByDOI(dataPackageDoi.getDoiName());
  }

  public DataPackage getDataPackageByAlternativeIdentifier(String alternativeIdentifier) {
    return dataPackageMapper.getByAlternativeIdentifier(alternativeIdentifier);
  }

  public void deleteDataPackage(UUID dataPackageKey) {
    dataPackageMapper.delete(dataPackageKey);
  }

  public void archiveDataPackage(UUID dataPackageKey) {
    dataPackageMapper.archive(dataPackageKey);
  }

  public void deleteDataPackageFile(UUID dataPackageKey, String fileName) {
    dataPackageFileMapper.delete(dataPackageKey, fileName);
  }

  public void deleteTag(Integer tagKey) {
    tagMapper.delete(tagKey);
  }

  public void deleteCreator(Integer creatorKey) {
    creatorMapper.delete(creatorKey);
  }

  public void deleteIdentifier(Integer identifierKey) {
    identifierMapper.delete(identifierKey);
  }

  public DataPackageFile getDataPackageFile(UUID dataPackageKey, String fileName) {
    return dataPackageFileMapper.get(dataPackageKey, fileName);
  }

  public void deleteDataPackageFiles(DataPackage dataPackage) {
    Optional.ofNullable(dataPackage.getFiles()).ifPresent(files ->
    files.forEach(dataPackageFile -> dataPackageFileMapper.delete(dataPackage.getKey(),
                                                                  dataPackageFile.getFileName())));
  }

  public PagingResponse<DataPackage> listDataPackages(String user, @Nullable Pageable page,
                                          @Nullable Date fromDate, @Nullable Date toDate,
                                          @Nullable Boolean deleted, @Nullable List<String> tags,
                                          @Nullable String q) {
    return doPageableResponse(page,
                              () -> dataPackageMapper.count(user, fromDate, toDate, deleted, tags, q),
                              () -> dataPackageMapper.list(user, page, fromDate, toDate, deleted, tags, q));
  }

  /**
   * Page through AlternativeIdentifiers, optionally filtered by user and dates.
   */
  public PagingResponse<Identifier> listIdentifiers(@Nullable String user, @Nullable Pageable page,
                                                    @Nullable String identifier,
                                                    @Nullable UUID dataPackageKey,
                                                    @Nullable Identifier.Type type,
                                                    @Nullable Identifier.RelationType relationType,
                                                    @Nullable Date created) {
    return doPageableResponse(page,
                              () -> identifierMapper.count(user, identifier, dataPackageKey, type, relationType,
                                                           created),
                              () -> identifierMapper.list(user, page, identifier, dataPackageKey, type, relationType,
                                                          created));
  }

  public RepositoryStats getRepositoryStats() {
    return repositoryStatsMapper.get();
  }
}
