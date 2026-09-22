package com.wiley.sf.common.config;

import java.util.Date;
import java.util.HashMap;
import java.util.ArrayList;

import java.sql.Timestamp;

import java.text.SimpleDateFormat;
import java.text.ParseException;

import java.net.URL;
import java.net.MalformedURLException;

import org.junit.*;
import static org.junit.Assert.*;

/**
 * JUnit test for class ValueHelper.
 *
 * @since   JDK 1.5, JUnit 4.4
 * @version $Id: ValueHelperTest.java,v 1.1 2008-07-21 13:15:28 smarkoff Exp $
 * @author  Steve Markoff
 */
public class ValueHelperTest {

    private ValueHelperTestImpl vh;

    @Before
    public void setUp() {
        vh = new ValueHelperTestImpl();
    }

    @After
    public void tearDown() {
        vh = null;
    }

    @Test
    public void getInt() {
        vh.setValue("  5  ");
        assertTrue(vh.getInt("name") == 5);

        vh.setValue("non-number");
        try {
            vh.getInt("name");
            fail();
        }
        catch (Exception ex) { }
    }

    @Test
    public void getInt2() {
        vh.setValue("  5  ");
        assertTrue(vh.getInt("name", 4) == 5);

        vh.setValue("non-number");
        assertTrue(vh.getInt("name", 4) == 4);
    }

    @Test
    public void getLong() {
        vh.setValue("  5  ");
        assertTrue(vh.getLong("name") == 5);

        vh.setValue("non-number");
        try {
            vh.getLong("name");
            fail();
        }
        catch (Exception ex) { }
    }

    @Test
    public void getLong2() {
        vh.setValue("  5  ");
        assertTrue(vh.getLong("name", 4) == 5);

        vh.setValue("non-number");
        assertTrue(vh.getLong("name", 4) == 4);
    }

    @Test
    public void getDouble() {
        vh.setValue("  5.0  ");
        assertTrue(vh.getDouble("name") == 5.0);

        vh.setValue("non-number");
        try {
            vh.getDouble("name");
            fail();
        }
        catch (Exception ex) { }
    }

    @Test
    public void getDouble2() {
        vh.setValue("  5.0  ");
        assertTrue(vh.getDouble("name", 4.0) == 5.0);

        vh.setValue("non-number");
        assertTrue(vh.getDouble("name", 4.0) == 4.0);
    }

    @Test
    public void getBoolean() {
        vh.setValue("  true  ");
        assertTrue(vh.getBoolean("name"));

        vh.setValue("non-boolean");
        try {
            vh.getBoolean("name");
            fail();
        }
        catch (Exception ex) { }
    }

    @Test
    public void getBoolean2() {
        vh.setValue("  true  ");
        assertTrue(vh.getBoolean("name", false));

        vh.setValue("non-boolean");
        assertFalse(vh.getBoolean("name", false));
    }

    @Test
    public void getURL()
        throws MalformedURLException
    {
        URL url = new URL("http://www.ibm.com");
            // throws MalformedURLException

        vh.setValue("  " + url.toString() + "  ");
        assertTrue(vh.getURL("name").equals(url));

        vh.setValue("foo");
        try {
            vh.getURL("name");
            fail();
        }
        catch (Exception ex) { }
    }

    @Test
    public void getURL2()
        throws MalformedURLException
    {
        URL url = new URL("http://www.ibm.com");
        URL url2 = new URL("http://www.yahoo.com");
            // throws MalformedURLException

        vh.setValue("  " + url.toString() + "  ");
        assertTrue(vh.getURL("name", url2).equals(url));

        vh.setValue("blah");
        assertTrue(vh.getURL("name", url2).equals(url2));
    }

    @Test
    public void getString() {
        vh.setValue("  foo  ");
        assertTrue(vh.getString("name").equals("  foo  "));

        vh.setValue(null);
        try {
            vh.getString("name");
            fail();
        }
        catch (Exception ex) { }
    }

    @Test
    public void getString2() {
        vh.setValue("  foo  ");
        assertTrue(vh.getString("name", "blah").equals("  foo  "));

        vh.setValue(null);
        assertTrue(vh.getString("name", "blah").equals("blah"));
    }

    @Test
    public void getStringTrim() {
        vh.setValue("  foo  ");
        assertTrue(vh.getStringTrim("name").equals("foo"));

        vh.setValue(null);
        try {
            vh.getStringTrim("name");
            fail();
        }
        catch (Exception ex) { }
    }

    @Test
    public void getStringTrim2() {
        vh.setValue("  foo  ");
        assertTrue(vh.getStringTrim("name", "blah").equals("foo"));

        vh.setValue(null);
        assertTrue(vh.getStringTrim("name", "blah").equals("blah"));
    }

    @Test
    public void getStringNotBlank() {
        vh.setValue("  foo  ");
        assertTrue(vh.getStringNotBlank("name").equals("foo"));

        vh.setValue("  ");
        try {
            vh.getStringNotBlank("name");
            fail();
        }
        catch (Exception ex) { }
    }

    @Test
    public void getStringNotBlank2() {
        vh.setValue("  foo  ");
        assertTrue(vh.getStringNotBlank("name", "blah").equals("foo"));

        vh.setValue("  ");
        assertTrue(vh.getStringNotBlank("name", "blah").equals("blah"));
    }

    @Test
    public void getStringListTrim() {
        ValueHelperTestImpl2 vh = new ValueHelperTestImpl2();
        vh.setValue("1foo", "");
        vh.setValue("2foo", " 2foo ");
        vh.setValue("foo1", " foo1 ");
        vh.setValue("foo2", "");

        ArrayList<String> list1 = vh.getStringListTrim("foo", true);
        ArrayList<String> list2 = vh.getStringListTrim("foo", false);

        assertTrue(list1.size() == 1);
        String v1 = list1.get(0);
        assertTrue(v1.equals("2foo"));

        assertTrue(list2.size() == 1);
        v1 = list2.get(0);
        assertTrue(v1.equals("foo1"));
    }

    @Test
    public void getDate() {
        long time1 = 1234567890123L;  // A date in Feb 2009
        Date date1 = new Date(time1);

        vh.setValue("  " + String.valueOf(time1) + "  ");
        assertTrue(vh.getDate("name").equals(date1));

        vh.setValue("non-number");
        try {
            vh.getDate("name");
            fail();
        }
        catch (Exception ex) { }
    }

    @Test
    public void getDate2() {
        long time1 = 1234567890123L;  // A date in Feb 2009
        long time2 = 1234567890111L;
        Date date1 = new Date(time1);
        Date date2 = new Date(time2);

        vh.setValue("  " + String.valueOf(time1) + "  ");
        assertTrue(vh.getDate("name", date2).equals(date1));

        vh.setValue("non-number");
        assertTrue(vh.getDate("name", date2).equals(date2));
    }

    @Test
    public void getDate3()
        throws ParseException
    {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date1 = dateFormat.parse("2009-02-15");  // throws ParseException

        vh.setValue("  " + dateFormat.format(date1) + "  ");
        assertTrue(vh.getDate("name", dateFormat).equals(date1));
            // throws ParseException

        vh.setValue("non-number");
        try {
            vh.getDate("name", dateFormat);
            fail();
        }
        catch (Exception ex) { }
    }

    @Test
    public void getDate4()
        throws ParseException
    {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date1 = dateFormat.parse("2009-02-15");  // throws ParseException
        Date date2 = dateFormat.parse("2009-02-21");  // throws ParseException

        vh.setValue("  " + dateFormat.format(date1) + "  ");
        assertTrue(vh.getDate("name", dateFormat, date2).equals(date1));

        vh.setValue("non-number");
        assertTrue(vh.getDate("name", dateFormat, date2).equals(date2));
    }

    @Test
    public void getTimestamp() {
        long time1 = 1234567890123L;  // A date in Feb 2009
        Timestamp ts1 = new Timestamp(time1);

        vh.setValue("  " + String.valueOf(time1) + "  ");
        assertTrue(vh.getTimestamp("name").equals(ts1));

        vh.setValue("non-number");
        try {
            vh.getTimestamp("name");
            fail();
        }
        catch (Exception ex) { }
    }

    @Test
    public void getTimestamp2() {
        long time1 = 1234567890123L;  // A date in Feb 2009
        long time2 = 1234567890111L;
        Timestamp ts1 = new Timestamp(time1);
        Timestamp ts2 = new Timestamp(time2);

        vh.setValue("  " + String.valueOf(time1) + "  ");
        assertTrue(vh.getTimestamp("name", ts2).equals(ts1));

        vh.setValue("non-number");
        assertTrue(vh.getTimestamp("name", ts2).equals(ts2));
    }

    @Test
    public void getTimestamp3()
        throws ParseException
    {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date1 = dateFormat.parse("2009-02-15");  // throws ParseException
        Timestamp ts1 = new Timestamp(date1.getTime());

        vh.setValue("  " + dateFormat.format(ts1) + "  ");
        assertTrue(vh.getTimestamp("name", dateFormat).equals(ts1));

        vh.setValue("non-number");
        try {
            vh.getTimestamp("name", dateFormat);
            fail();
        }
        catch (Exception ex) { }
    }

    @Test
    public void getTimestamp4()
        throws ParseException
    {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date1 = dateFormat.parse("2009-02-15");  // throws ParseException
        Date date2 = dateFormat.parse("2009-02-21");  // throws ParseException
        Timestamp ts1 = new Timestamp(date1.getTime());
        Timestamp ts2 = new Timestamp(date2.getTime());

        vh.setValue("  " + dateFormat.format(ts1) + "  ");
        assertTrue(vh.getTimestamp("name", dateFormat, ts2).equals(ts1));

        vh.setValue("non-number");
        assertTrue(vh.getTimestamp("name", dateFormat, ts2).equals(ts2));
    }

}

class ValueHelperTestImpl extends ValueHelper {

    private String value;

    public void setValue(String value) {
        this.value = value;
    }

    /** Implements abstract method from super class. */
    public String getValue(String name) {
        // we ignore the name parameter for this class
        return value;
    }

    /** Implements abstract method from super class. */
    public String getType() {
        return "implementation for testing";
    }
}

// used to test getStringListTrim()
class ValueHelperTestImpl2 extends ValueHelper {

    private final HashMap<String, String> map = new HashMap<String, String>();

    public void setValue(String name, String value) {
        map.put(name, value);
    }

    /** Implements abstract method from super class. */
    public String getValue(String name) {
        return map.get(name);
    }

    /** Implements abstract method from super class. */
    public String getType() {
        return "implementation for testing";
    }
}

