package com.wiley.sf.common.lang;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/**
 * JUnit test class for CollectionUtil.
 *
 * @since   JDK 1.6, JUnit 4.10
 * @version $Id: CollectionUtilTest.java,v 1.1 2012-07-19 20:03:52 smarkoff Exp $
 * @author  Steve Markoff
 */
public class CollectionUtilTest {

    @Test
    public void split() {
        ArrayList<String> list = new ArrayList<String>();
        split(list, 10, 0);

        list.add("foo");
        split(list, 10, 1);

        list.add("foo2");
        split(list, 10, 1);
        split(list, 1, 2);

        list.add("foo3");
        list.add("foo4");
        split(list, 10, 1);
        split(list, 2, 2);
        split(list, 1, 4);
    }

    private void split(List<String> list, int targetSize, int expectedListSize) {
        List<List<String>> list2 = CollectionUtil.split(list, targetSize);
        String msg = "Expected list size to be " + expectedListSize + " but was "  + list.size();
        assertTrue(msg, list2.size() == expectedListSize);
    }
}
