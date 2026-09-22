package com.wiley.sf.common.lang;

import java.util.ArrayList;
import java.util.HashMap;

import org.junit.*;
import static org.junit.Assert.*;

/**
 * JUnit test class for ArgUtil.
 *
 * @since   JDK 1.5, JUnit 4.4
 * @version $Id: ArgUtilTest.java,v 1.1 2008-07-16 12:23:15 smarkoff Exp $
 * @author  Steve Markoff
 */
public class ArgUtilTest {

    @Test
    public void notNull() {
        ArgUtil.notNull("not null", "argName");

        try {
            ArgUtil.notNull(null, "argName");
            fail("Exception should have been thrown");
        }
        catch (IllegalArgumentException ex) { }
    }

    @Test
    public void notBlank() {
        ArgUtil.notBlank("not blank", "argName");

        try {
            ArgUtil.notBlank(null, "argName");
            fail("Exception should have been thrown");
        }
        catch (IllegalArgumentException ex) { }

        try {
            ArgUtil.notBlank(" ", "argName");
            fail("Exception should have been thrown");
        }
        catch (IllegalArgumentException ex) { }
    }

    @Test
    public void notEmpty_String() {
        ArgUtil.notEmpty("not empty", "argName");
        ArgUtil.notEmpty("  ", "argName");
        ArgUtil.notEmpty("\n", "argName");
        ArgUtil.notEmpty("\r", "argName");
        ArgUtil.notEmpty("\t", "argName");

        try {
			ArgUtil.notEmpty((String) null, "argName");
			fail("Exception should have been thrown");
		}
		catch (IllegalArgumentException ex) { }

		try {
			ArgUtil.notEmpty("", "argName");
			fail("Exception should have been thrown");
		}
		catch (IllegalArgumentException ex) { }
    }

    @Test
    public void notEmpty_Array() {
        String [] sa = null;
        try {
            ArgUtil.notEmpty(sa, "argName");
            fail("Exception should have been thrown");
        }
        catch (IllegalArgumentException ex) { }

        sa = new String[0];
        try {
            ArgUtil.notEmpty(sa, "argName");
            fail("Exception should have been thrown");
        }
        catch (IllegalArgumentException ex) { }

        sa = new String[] { "foo" };
        ArgUtil.notEmpty(sa, "argName");
    }

    @Test
    public void notEmpty_Collection() {
        ArrayList<String> list = null;
        try {
            ArgUtil.notEmpty(list, "argName");
            fail("Exception should have been thrown");
        }
        catch (IllegalArgumentException ex) { }

        list = new ArrayList<String>();
        try {
            ArgUtil.notEmpty(list, "argName");
            fail("Exception should have been thrown");
        }
        catch (IllegalArgumentException ex) { }

        list.add("foo");
        ArgUtil.notEmpty(list, "argName");
    }

    @Test
    public void notEmpty_Map() {
        HashMap<String, String> map = null;
        try {
            ArgUtil.notEmpty(map, "argName");
            fail("Exception should have been thrown");
        }
        catch (IllegalArgumentException ex) { }

        map = new HashMap<String, String>();
        try {
            ArgUtil.notEmpty(map, "argName");
            fail("Exception should have been thrown");
        }
        catch (IllegalArgumentException ex) { }
        
        map.put("foo", "foo");
        ArgUtil.notEmpty(map, "argName");
    }

    @Test
    public void noNullElements_Array() {
		String [] array = null;
		try {
			ArgUtil.noNullElements(array, "argName");
			fail("Exception should have been thrown");
		}
		catch (IllegalArgumentException ex) { }

        array = new String[2];
        array[0] = "foo";
        array[1] = null;

        try {
			ArgUtil.noNullElements(array, "argName");
			fail("Exception should have been thrown");
		}
		catch (IllegalArgumentException ex) { }

		array[1] = "foo";
		ArgUtil.noNullElements(array, "argName");
		array = new String[0];
		ArgUtil.noNullElements(array, "argName");
    }

    @Test
    public void noBlankElements_Array() {
        String [] array = new String[2];
        array[0] = "foo";
        array[1] = " ";

        try {
			ArgUtil.noBlankElements(array, "argName");
			fail("Exception should have been thrown");
		}
		catch (IllegalArgumentException ex) { }

		array[0] = "  foo  ";
		array[1] = "  _";
		ArgUtil.noBlankElements(array, "argName");
    }
    
    @Test
    public void noNullElements_Collection() {
        ArrayList<String> list = new ArrayList<String>();
        ArgUtil.noNullElements(list, "argName");
        list.add("foo");
        ArgUtil.noNullElements(list, "argName");

        list.add(null);
        try {
			ArgUtil.noNullElements(list, "argName");
			fail("Exception should have been thrown");
		}
		catch (IllegalArgumentException ex) { }

		list = null;
        try {
			ArgUtil.noNullElements(list, "argName");
			fail("Exception should have been thrown");
		}
		catch (IllegalArgumentException ex) { }
    }

    @Test
    public void noBlankElements_Collection() {
        ArrayList<String> list = new ArrayList<String>();
        list.add("foo");
        ArgUtil.noBlankElements(list, "argName");

        list.add(" ");
        try {
			ArgUtil.noBlankElements(list, "argName");
			fail("Exception should have been thrown");
		}
		catch (IllegalArgumentException ex) { }
    }
    
    @Test
    public void notLess0() {
        ArgUtil.notLess0(0, "argName");

        try {
            ArgUtil.notLess0(-1, "argName");
            fail("Exception should have been thrown");
        }
        catch (IllegalArgumentException ex) { }
    }

    @Test
    public void notLess1() {
        ArgUtil.notLess1(1, "argName");

        try {
            ArgUtil.notLess1(0, "argName");
            fail("Exception should have been thrown");
        }
        catch (IllegalArgumentException ex) { }
    }

    @Test
    public void inRange() {
        ArgUtil.inRange(-1, "argName", -5, 5);

        try {
            ArgUtil.inRange(6, "argName", -5, 5);
            fail("Exception should have been thrown");
        }
        catch (IllegalArgumentException ex) { }
    }

}
