package org.gbif.datarepo.test.mocks;

import org.gbif.api.model.common.DOI;
import org.gbif.datarepo.api.model.Tag;
import org.gbif.datarepo.persistence.mappers.TagMapper;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.ibatis.annotations.Param;

/**
 * Mock MyBatis mapper.
 */
public class TagMapperMock implements TagMapper {

  @Override
  public void create(Tag tag) {
    //do nothing
  }

  @Override
  public void delete(@Param("tagKey") Integer tagKey) {
    //do nothing
  }

  @Override
  public List<Tag> listByDoi(@Param("dataPackageDoi") DOI dataPackageDoi) {
    Tag tag = new Tag();
    tag.setCreated(new Date());
    tag.setCreatedBy("testUser");
    tag.setDataPackageDoi(dataPackageDoi);
    tag.setValue("TagValue");
    return Collections.singletonList(tag);
  }
}
