package org.gbif.datarepo.persistence.mappers;

import org.gbif.api.model.common.DOI;
import org.gbif.api.vocabulary.License;
import org.gbif.datarepo.api.model.Identifier;
import org.gbif.datarepo.api.model.DataPackage;
import org.gbif.datarepo.api.model.DataPackageFile;
import org.gbif.datarepo.api.model.Tag;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;
import com.google.common.hash.Hashing;
import com.google.inject.Injector;
import org.gbif.datarepo.impl.util.MimeTypesUtil;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests of DataPackageMapper.
 */
public class DataPackageFileMapperTest extends BaseMapperTest {

  private static final String TEST_REPO_NAME = "TestRepo";
  private static final String TEST_OTHER_REPO_NAME = "OtherRepo";

  //Guice injector used to instantiate Mappers.
  private static Injector injector;

  private static final String ALTERNATIVE_ID_TEST = UUID.randomUUID().toString();

  public static final UUID DATA_PACKAGE_KEY_TEST = UUID.fromString("85fc0ce8-f762-11e1-a439-00145eb45e9a");

  /**
   * Initializes the MyBatis module.
   */
  @BeforeClass
  public static void init() {
    injector = buildInjector();
  }

  /**
   * Creates a new instance of a DataPackage.
   */
  private static DataPackage testDataPackage() {
    DataPackage dataPackage = new DataPackage();
    Identifier alternativeIdentifier = new Identifier();
    alternativeIdentifier.setCreated(new Date());
    alternativeIdentifier.setCreatedBy("testUser");
    alternativeIdentifier.setType(Identifier.Type.GBIF_DATASET_KEY);
    alternativeIdentifier.setIdentifier(ALTERNATIVE_ID_TEST);
    Tag tag = new Tag();
    tag.setCreated(new Date());
    tag.setCreatedBy("testUser");
    tag.setValue("DataOne");
    String testChecksum = Hashing.md5().newHasher(32).hash().toString();
    dataPackage.setKey(DATA_PACKAGE_KEY_TEST);
    dataPackage.setDoi(new DOI(DOI.TEST_PREFIX, Long.toString(new Date().getTime())));
    dataPackage.addRelatedIdentifier(alternativeIdentifier);
    dataPackage.addTag(tag);
    dataPackage.setChecksum(testChecksum);
    dataPackage.setCreated(new Date());
    dataPackage.setCreatedBy("testUser");
    dataPackage.setDescription("test data package description");
    dataPackage.setSize(1);
    dataPackage.setTitle("test");
    dataPackage.setPublishedIn(TEST_REPO_NAME);
    dataPackage.setShareIn(Sets.newHashSet(TEST_OTHER_REPO_NAME));
    dataPackage.setLicense(License.CC_BY_NC_4_0);
    String fileName = "test.xml";
    dataPackage.addFile(new DataPackageFile(fileName, MimeTypesUtil.detectDataOneFormat(fileName),testChecksum, 1));
    return dataPackage;
  }

  /**
   * Utility method that creates a test data package in the DB.
   */
  private static void insertDataPackage(DataPackage dataPackage) {
    DataPackageMapper dataPackageMapper = injector.getInstance(DataPackageMapper.class);
    dataPackageMapper.create(dataPackage);
    dataPackage.getRelatedIdentifiers().forEach(identifier -> {
      identifier.setCreatedBy(dataPackage.getCreatedBy());
      identifier.setDataPackageKey(dataPackage.getKey());
    });
    dataPackage.getTags().forEach(tag -> {
      tag.setCreatedBy(dataPackage.getCreatedBy());
      tag.setDataPackageKey(dataPackage.getKey());
    });
    IdentifierMapper identifierMapper = injector.getInstance(IdentifierMapper.class);
    dataPackage.getRelatedIdentifiers().forEach(identifierMapper::create);

    TagMapper tagMapper = injector.getInstance(TagMapper.class);
    dataPackage.getTags().forEach(tagMapper::create);
  }

  /**
   * Base method for full text search test cases.
   */
  private static void baseFullTextSearchTest(String query, BiFunction<List<DataPackage>, Long, Boolean> assertion) {
    DataPackageMapper mapper = injector.getInstance(DataPackageMapper.class);
    DataPackage dataPackage = testDataPackage();
    mapper.create(dataPackage);
    List<DataPackage> dataPackages = mapper.list(null, null, null, null, null, null, null, null, query, null);
    Long count = mapper.count(null, null, null, null, null, null, null, query, null);
    Assert.assertTrue(assertion.apply(dataPackages, count));
  }

  /**
   * Tests methods create and get.
   */
  @Test
  public void testCreate() {
    DataPackage dataPackage = testDataPackage();
    insertDataPackage(dataPackage);
    DataPackageMapper mapper = injector.getInstance(DataPackageMapper.class);

    DataPackage justCreated = mapper.getByKey(DATA_PACKAGE_KEY_TEST);
    Assert.assertEquals(justCreated.getDoi(), dataPackage.getDoi());
  }

  /**
   * Tests methods create and get.
   */
  @Test
  public void testPublishInRepos() {
    DataPackage dataPackage = testDataPackage();
    insertDataPackage(dataPackage);
    DataPackageMapper mapper = injector.getInstance(DataPackageMapper.class);
    List<DataPackage> packages = mapper.list(null, null, null, null, null, null, TEST_REPO_NAME, null, null, null);
    Long packagesCount = mapper.count(null, null, null, null, null, TEST_REPO_NAME, null, null, null);
    Assert.assertTrue("DataPackage not found in repository",
                      packages.stream().anyMatch(dp -> dp.getKey().equals(dataPackage.getKey())));
    Assert.assertTrue("DataPackage not found in repository", packagesCount > 0);

    List<DataPackage> packagesNotInRepo = mapper.list(null, null, null, null, null, null, TEST_OTHER_REPO_NAME, null, null, null);
    Long packagesNotInRepoCount = mapper.count(null, null, null, null, null, TEST_OTHER_REPO_NAME, null, null, null);
    Assert.assertTrue("No DataPackages must be found in this repository", packagesNotInRepo.stream().noneMatch(dp -> dp.getKey().equals(dataPackage.getKey())));
    Assert.assertTrue("No DataPackages must be found in this repository", packagesNotInRepoCount == 0);
  }

  /**
   * Tests methods create and get.
   */
  @Test
  public void testGetByAlternativeIdentifier() {
    DataPackageMapper mapper = injector.getInstance(DataPackageMapper.class);
    DataPackage dataPackage = testDataPackage();
    insertDataPackage(dataPackage);
    DataPackage justCreated = mapper.getByAlternativeIdentifier(ALTERNATIVE_ID_TEST);
    Assert.assertEquals(dataPackage.getKey(), justCreated.getKey());
  }


  /**
   * Tests methods create and delete.
   */
  @Test
  public void testDelete() {
    DataPackageMapper mapper = injector.getInstance(DataPackageMapper.class);
    DataPackage dataPackage = testDataPackage();
    insertDataPackage(dataPackage);
    mapper.delete(dataPackage.getKey());
    Assert.assertNotNull(mapper.getByKey(dataPackage.getKey()).getDeleted());
  }

  /**
   * Tests methods create and list.
   */
  @Test
  public void testList() {
    DataPackageMapper mapper = injector.getInstance(DataPackageMapper.class);
    DataPackage dataPackage = testDataPackage();
    mapper.create(dataPackage);
    List<DataPackage> dataPackages = mapper.list("testUser", null, null, null, false, null, null, null, null, null);
    Assert.assertTrue(dataPackages.size() >=  1);
  }

  /**
   * Tests methods create and list.
   */
  @Test
  public void testListByTags() {
    DataPackageMapper mapper = injector.getInstance(DataPackageMapper.class);
    DataPackage dataPackage = testDataPackage();
    insertDataPackage(dataPackage);
    List<DataPackage> dataPackages = mapper.list(null, null, null, null, null,
                                                 dataPackage.getTags().stream()
                                                   .map(Tag::getValue).collect(Collectors.toList()),
                                                 null, null, null, null);
    Assert.assertTrue(dataPackages.size() >=  1);
  }

  /**
   * Tests methods create and list.
   */
  @Test
  public void testListByNonExistingTags() {
    DataPackageMapper mapper = injector.getInstance(DataPackageMapper.class);
    DataPackage dataPackage = testDataPackage();
    insertDataPackage(dataPackage);
    List<DataPackage> dataPackages = mapper.list(null, null, null, null, null, Collections.singletonList("NoATag"), null, null, null, null);
    Long count = mapper.count(null, null, null, null, Collections.singletonList("NoATag"), null, null, null, null);
    Assert.assertTrue(dataPackages.isEmpty());
    Assert.assertTrue(count ==  0);
  }

  /**
   * Tests methods create and count.
   */
  @Test
  public void testCount() {
    DataPackageMapper mapper = injector.getInstance(DataPackageMapper.class);
    DataPackage dataPackage = testDataPackage();
    mapper.create(dataPackage);
    Assert.assertTrue(mapper.count("testUser", null, null, false, null, null, null, null, null) >= 1);
  }

  /**
   * Tests methods create and update.
   */
  @Test
  public void testUpdate() {
    DataPackageMapper mapper = injector.getInstance(DataPackageMapper.class);
    DataPackage dataPackage = testDataPackage();
    mapper.create(dataPackage);
    dataPackage.setTitle("newTitle");
    mapper.update(dataPackage);
    Assert.assertEquals("newTitle", mapper.getByKey(dataPackage.getKey()).getTitle());
  }

  /**
   * Tests full text search.
   */
  @Test
  public void testFullTextSearch() {
    baseFullTextSearchTest("description", (dataPackages, count) ->
                                            dataPackages.size() == count.intValue() && count >= 1L);
  }

  /**
   * Tests full text search with no matches.
   */
  @Test
  public void testFullTextSearchNoMatches() {
    baseFullTextSearchTest("ThisMustNoReturnAnything", (dataPackages, count) ->
                                                          dataPackages.size() == count.intValue() && count >= 0L);
  }
}
