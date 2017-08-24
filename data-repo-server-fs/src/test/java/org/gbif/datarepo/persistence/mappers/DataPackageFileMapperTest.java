package org.gbif.datarepo.persistence.mappers;

import org.gbif.api.model.common.DOI;
import org.gbif.datarepo.api.model.DataPackage;
import org.gbif.datarepo.api.model.DataPackageFile;

import java.util.Date;
import java.util.List;

import com.google.common.hash.Hashing;
import com.google.inject.Injector;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests of DataPackageMapper.
 */
public class DataPackageFileMapperTest  extends BaseMapperTest {

  //Guice injector used to instantiate Mappers.
  private static Injector injector;

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
    String testChecksum = Hashing.md5().newHasher(32).hash().toString();
    dataPackage.setDoi(new DOI(DOI.TEST_PREFIX, Long.toString(new Date().getTime())));
    dataPackage.setChecksum(testChecksum);
    dataPackage.setCreated(new Date());
    dataPackage.setCreatedBy("testUser");
    dataPackage.setDescription("test");
    dataPackage.setMetadata("metadata.xml");
    dataPackage.setSize(1);
    dataPackage.setTitle("test");
    dataPackage.addFile(new DataPackageFile("test.xml", testChecksum, 1));
    return dataPackage;
  }

  /**
   * Tests methods create and get.
   */
  @Test
  public void testCreate() {
    DataPackageMapper mapper = injector.getInstance(DataPackageMapper.class);
    DataPackage dataPackage = testDataPackage();
    mapper.create(dataPackage);
    DataPackage justCreated = mapper.get(dataPackage.getDoi().getDoiName());
    Assert.assertEquals(justCreated.getDoi(), dataPackage.getDoi());
  }

  /**
   * Tests methods create and delete.
   */
  @Test
  public void testDelete() {
    DataPackageMapper mapper = injector.getInstance(DataPackageMapper.class);
    DataPackage dataPackage = testDataPackage();
    mapper.create(dataPackage);
    mapper.delete(dataPackage.getDoi());
  }

  /**
   * Tests methods create and list.
   */
  @Test
  public void testList() {
    DataPackageMapper mapper = injector.getInstance(DataPackageMapper.class);
    DataPackage dataPackage = testDataPackage();
    mapper.create(dataPackage);
    List<DataPackage> dataPackages = mapper.list("testUser", null, null, null, false);
    Assert.assertTrue(dataPackages.size() >=  1);
  }

  /**
   * Tests methods create and count.
   */
  @Test
  public void testCount() {
    DataPackageMapper mapper = injector.getInstance(DataPackageMapper.class);
    DataPackage dataPackage = testDataPackage();
    mapper.create(dataPackage);
    Assert.assertTrue(mapper.count("testUser", null, null, null, false) >= 1);
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
    Assert.assertEquals("newTitle", mapper.get(dataPackage.getDoi().getDoiName()).getTitle());
  }
}
