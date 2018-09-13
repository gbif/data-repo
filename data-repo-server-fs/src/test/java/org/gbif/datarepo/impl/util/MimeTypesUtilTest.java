package org.gbif.datarepo.impl.util;

import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

/**
 * Tests for class {@link MimeTypesUtil}.
 */
public class MimeTypesUtilTest {


    //Map of file names and expected Mime types
    private static final Map<String,String> TEST_DATA = new ImmutableMap.Builder<String,String>()
            .put("test.rdf","application/rdf+xml")
            .put("test.pdf","application/pdf")
            .put("test.xml","application/xml")
            .put("test.png","image/png")
            .put("test.zip","application/zip")
            .put("test.tar","application/x-tar")
            .put("test.gz","application/gzip")
            .put("test.doc","application/msword")
            .put("test.eml","message/rfc822")
            .put("test.bin","application/octet-stream") //default MimeType
            .build();


    /**
     * Test cases for MimeType detections.
     */
    @Test
    public void testMimeTypeDetect() {
       TEST_DATA.forEach( (key, value) -> Assert.assertEquals(MimeTypesUtil.detectMimeType(key),value));
    }
}
