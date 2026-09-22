package com.wiley.sf.common.lang;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.*;
import static org.junit.Assert.*;

/**
 * JUnit test class for ResourceUtil.
 *
 * @since   JDK 1.5, JUnit 4.4
 * @version $Id: ResourceUtilTest.java,v 1.1 2008-01-03 23:37:19 smarkoff Exp $
 * @author  Steve Markoff
 */
public class ResourceUtilTest {

    @Test
    public void doesResourceExist() {
        assertTrue(ResourceUtil.doesResourceExist(ResourceUtilTest.class,
            "ResourceUtilTest.txt"));
        assertTrue(ResourceUtil.doesResourceExist(ResourceUtilTest.class,
            "/com/wiley/sf/common/lang/ResourceUtilTest.txt"));
        assertFalse(ResourceUtil.doesResourceExist(ResourceUtilTest.class,
            "ResourceUtilTest.blah"));
    }

    @Test
    public void getStringResource() throws IOException {
        String contents = ResourceUtil.getStringResource(ResourceUtilTest.class,
            "ResourceUtilTest.txt");
        assertTrue("test string 123".equals(contents.trim()));
        try {
            ResourceUtil.getStringResource(ResourceUtilTest.class,
                "ResourceUtilTest.blah");
            fail("Should have thrown FileNotFoundException.");
        }
        catch (FileNotFoundException ex) { }
    }
}
