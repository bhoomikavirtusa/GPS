package com.wiley.sf.common.lang;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

/**
 * JUnit test class for StringUtil.
 *
 * @since   JDK 1.6, JUnit 4.10
 * @version $Id: StringUtilTest.java,v 1.22 2014-02-26 00:25:46 smarkoff Exp $
 * @author  Steve Markoff
 */
public class StringUtilTest {

    @Test
    public void truncate() {
        truncate(null, 5, null);
        truncate(null, 0, null);
        truncate(null, -2, null);
        truncate("", 2, "");
        truncate("", 1, "");
        truncate("a", 1, "a");
        truncate("a", 2, "a");
        truncate("abc", -1, "abc");
        truncate("abc", 0, "abc");
        truncate("abc", 1, "a");
        truncate("abc", 2, "ab");
        truncate("abc", 6, "abc");
        truncate("0123456789", 10, "0123456789");
        truncate("0123456789123", 10, "0123456...");
        truncate("01234567890123456789", 20, "01234567890123456789");  // because minKeepLength = 5
        truncate("012345678901234567890123456789", 25, "0123456...[23 more chars]");
    }

    private void truncate(String input, int limit, String expected) {
        String output = StringUtil.truncate(input, limit, true);
        String msg = "Input was ('" + input + "', " + limit + ")"
            + "  Expected output was '" + expected + "' but got '" + output + "'";
        assertTrue(msg, StringUtils.equals(output, expected));
    }

    @Test
    public void containsWhitespace() {
        assertFalse(StringUtil.containsWhitespace(null));
        assertFalse(StringUtil.containsWhitespace(""));
        assertTrue(StringUtil.containsWhitespace("quick brown"));
        assertTrue(StringUtil.containsWhitespace("\nquickbrown"));
        assertTrue(StringUtil.containsWhitespace("quick\tbrown"));
        assertFalse(StringUtil.containsWhitespace("quick_brown"));
    }

    @Test
    public void indexOfIgnoreCase() {
        indexOfIgnoreCase(null, null, 0, -1);
        indexOfIgnoreCase("big HamBurgeR", "hAMbURGEr", 0, 4);
        indexOfIgnoreCase("big HamBurgeR", "Ham1", 0, -1);
        indexOfIgnoreCase("big HamBurgeR", "", 0, 0);
    }

    private void indexOfIgnoreCase(String s, String find, int start,
        int expected)
    {
        int index = StringUtil.indexOfIgnoreCase(s, find, start);
        String msg = "Looking for '" + find + "' in string '" + s
            + "' found at index " + index + " but expected at " + expected;
        assertTrue(msg, index == expected);
    }

    @Test
    public void indexOfAny() {
        assertTrue(StringUtil.indexOfAny(null, null, 0) == -1);
        String s = "quick brown fox";
        int index = StringUtil.indexOfAny(s, "fbi", 0);
        assertTrue(index == 2);

        index = StringUtil.indexOfAny(s, "fbi", 3);
        assertTrue(index == 6);

        index = StringUtil.indexOfAny(s, "ck", 6);
        assertTrue(index == -1);
    }

    @Test
    public void equalsAny() {
        String [] array = null;
        assertFalse(StringUtil.equalsAny(null, array));
        assertFalse(StringUtil.equalsAny("", array));
        array = new String [] { };
        assertFalse(StringUtil.equalsAny(null, array));
        assertFalse(StringUtil.equalsAny("", array));
        array = new String [] { "foo", "two","blue" };
        assertFalse(StringUtil.equalsAny("", array));
        assertFalse(StringUtil.equalsAny("one", array));
        assertTrue(StringUtil.equalsAny("foo", array));
        assertTrue(StringUtil.equalsAny("blue", array));
    }

    @Test
    public void equalsAnyIgnoreCase() {
        String [] array = null;
        assertFalse(StringUtil.equalsAnyIgnoreCase(null, array));
        array = new String [] { "foo", "two", "blue" };
        assertTrue(StringUtil.equalsAnyIgnoreCase("TWO", array));
    }

    @Test
    public void replaceIgnoreCase() {
        replaceIgnoreCase(null, "a", "bb", null);
        replaceIgnoreCase("", "a", "bb", "");
        replaceIgnoreCase("quick", "a", "bb", "quick");
        replaceIgnoreCase("quack", "A", "bb", "qubbck");
        replaceIgnoreCase("quAck a A", "a", "BB", "quBBck BB BB");
        replaceIgnoreCase("qUick brown Quick", "quick", "sLow", "sLow brown sLow");
    }

    private void replaceIgnoreCase(String s, String find, String replacement, String expected) {
        String result = StringUtil.replaceIgnoreCase(s, find, replacement);
        String msg = "Result: " + result + ", Expected: " + expected;
        assertTrue(msg, StringUtils.equals(result, expected));
    }

    @Test
    public void removeAny() {
        removeAny(null, " gbf", null);
        removeAny("", " gbf", "");
        removeAny("abc def ghi", " gbf", "acdehi");
        removeAny("abc def ghi", "", "abc def ghi");
        removeAny("abc def ghi", "ai ", "bcdefgh");
    }

    private void removeAny(String s, String chars, String expected) {
        String actual = StringUtil.removeAny(s, chars);
        String msg = "Original: '" + s + "' remove chars: '" + chars
            + "' expected: '" + expected + "' actual: '" + actual + "'";
        assertTrue(msg, StringUtils.equals(expected, actual));
    }

    @Test
    public void removeSpecialControlChars() {
        removeSpecialControlChars(null, null);
        removeSpecialControlChars("", "");
        removeSpecialControlChars("a", "a");
        removeSpecialControlChars("\u0002", "");
        removeSpecialControlChars("a\u0002", "a");
        removeSpecialControlChars("\u0002a", "a");
        removeSpecialControlChars("\u0000\u0001\u0002_\u0003\u0004\u0005\u0006\u0007\u0008\u0009", "_\u0009");
        // for some reason eclipse doesn't like u000a or u000d - must use n and r instead
        removeSpecialControlChars("\n\u000b\u000c\r\u000e\u000f", "\n\r");
        removeSpecialControlChars("_\u0010\u0011\u0012\u0013\u0014\u0015\u0016\u0017\u0018\u0019_", "__");
        removeSpecialControlChars("\u001a\u001b\u001c\u001d_\u001e\u001f ABCabc123?|\u007f", "_ ABCabc123?|");
    }

    private void removeSpecialControlChars(String s, String expected) {
        String result = StringUtil.removeSpecialControlChars(s);
        String msg = "result was [" + result + "] but expected [" + expected + "]";
        assertTrue(msg, StringUtils.equals(result, expected));
    }

    @Test
    public void replaceAny() {
        replaceAny(null, " gbf", "repl", null);
        replaceAny("", " gbf", "repl", "");
        replaceAny("abc def ghi", " gbf", "_", "a_c_de___hi");
        replaceAny("abc def ghi", "", "_", "abc def ghi");
        replaceAny("abc def ghi", "ai ", "_", "_bc_def_gh_");
    }

    private void replaceAny(String s, String chars, String replacement, String expected) {
        String actual = StringUtil.replaceAny(s, chars, replacement);
        String msg = "Original: '" + s + "' replace chars: '" + chars
            + "' expected: '" + expected + "' actual: '" + actual + "'";
        assertTrue(msg, StringUtils.equals(expected, actual));
    }

    @Test
    public void equalsIgnoreWhitespace() {
        equalsIgnoreWhitespace(null, null, true);
        equalsIgnoreWhitespace(null, "", false);
        equalsIgnoreWhitespace("", null, false);
        equalsIgnoreWhitespace("a", "a_", false);
        equalsIgnoreWhitespace("a", " a ", true);
        equalsIgnoreWhitespace("\r\n a \t ", "a", true);
    }

    private void equalsIgnoreWhitespace(String s1, String s2, boolean expected) {
        boolean result = StringUtil.equalsIgnoreWhitespace(s1, s2);
        String msg = "Expected: " + expected + "\r\ns1: " + s1 + "\r\ns2: " + s2;
        assertTrue(msg, result == expected);
    }

    @Test
    public void createStringOfLength() {
        String repeat = "1234567890";
        createStringOfLength(0, repeat, "");
        createStringOfLength(1, repeat, "1");
        createStringOfLength(2, repeat, "12");
        createStringOfLength(10, repeat, repeat);
        createStringOfLength(11, repeat, repeat + "1");
        createStringOfLength(24, repeat, repeat + repeat + "1234");

        repeat = "a";
        createStringOfLength(1, repeat, "a");
        createStringOfLength(2, repeat, "aa");
        createStringOfLength(3, repeat, "aaa");
    }

    public void createStringOfLength(int length, String repeat, String expected) {
        String result = StringUtil.createStringOfLength(length, repeat);
        String msg = "Expected [" + expected + "] but got [" + result + "]";
        assertTrue(msg, result.equals(expected));
    }

    @Test
    public void arrayToString() {
        Object [] array = null;
        String expected = null;
        String separator = ", ";
        arrayToString(array, separator, expected);

        array = new Object [] { };
        expected = "";
        arrayToString(array, separator, expected);

        array = new Object [] { null };
        expected = "null";
        arrayToString(array, separator, expected);

        array = new String [] { "element1", "element2" };
        expected = "element1, element2";
        arrayToString(array, separator, expected);
    }

    private void arrayToString(Object [] array, String separator, String expected) {
        String output = StringUtil.arrayToString(array, separator);
        String msg = "Output was: " + output + "\nbut expected was: " + expected;
        assertTrue(msg, StringUtils.equals(output, expected));
    }

    @Test
    public void collectionToString() {
        Collection<String> col = null;
        String expected = null;
        String separator = ", ";
        collectionToString(col, separator, expected);

        col = new ArrayList<String>();
        expected = "";
        collectionToString(col, separator, expected);

        col.add(null);
        expected = "null";
        collectionToString(col, separator, expected);

        col.clear();
        col.add("element1");
        col.add("element2");
        expected = "element1, element2";
        collectionToString(col, separator, expected);
    }

    private void collectionToString(Collection<?> col, String separator, String expected) {
        String output = StringUtil.collectionToString(col, separator);
        String msg = "Output was: " + output + "\nbut expected was: " + expected;
        assertTrue(msg, StringUtils.equals(output, expected));
    }

    @Test
    public void collectionToString2() {
        Collection<String> col = new ArrayList<String>();
        col.add("element1");
        col.add("element2");
        String expected = "<element1>, <element2>";
        collectionToString2(col, ", ", "<", ">", expected);
    }

    private void collectionToString2(Collection<?> col, String separator,
        String start, String end, String expected)
    {
        String output = StringUtil.collectionToString(col, separator, start, end);
        String msg = "Output was: " + output + "\nbut expected was: " + expected;
        assertTrue(msg, StringUtils.equals(output, expected));
    }

    @Test
    public void collectionToString3() {
    	Collection<String> col = new ArrayList<String>();
    	String [] results = StringUtil.collectionToString(col, ", ", 0);
    	assertTrue("Expected to get array length 0", results.length == 0);

    	col.add("e1");
    	col.add("e2");
    	col.add("e3");
    	col.add("e4");
    	col.add("e5");
    	results = StringUtil.collectionToString(col, ", ", 2);
    	assertTrue("Expected to get array length 3", results.length == 3);
    	assertTrue(results[0].equals("e1, e2"));
    	assertTrue(results[1].equals("e3, e4"));
    	assertTrue(results[2].equals("e5"));
    }

    @Test
    public void mapToString() {
        HashMap<String, String> map = null;
        String expected = null;
        String separator = ", ";
        mapToString(map, separator, expected);

        map = new HashMap<String, String>();
        expected = "";
        mapToString(map, separator, expected);

        map.put(null, null);
        expected = "null=null";
        mapToString(map, separator, expected);

        map.clear();
        map.put("key1", "value1");
        map.put("key2", "value2");
        expected = "key1=value1, key2=value2";
        // JDK 1.5 produces the above but JDK 1.6 produces the reverse
        // instead (which is ok since maps do not have an ordering).
        mapToString(map, separator, expected, "key2=value2, key1=value1");

        HashMap<String, String []> map2 = new HashMap<String, String []>();
        map2.put("key1", new String [] { "value1", "value2" });
        expected = "key1={value1, value2}";
        mapToString(map2, separator, expected);
    }

    private void mapToString(HashMap<?,?> map, String separator, String expected) {
        mapToString(map, separator, expected, expected);
    }

    private void mapToString(HashMap<?,?> map, String separator, String expected, String alt) {
        String output = StringUtil.mapToString(map, separator);
        String msg = "Output was: " + output + "\nbut expected was: " + expected;
        assertTrue(msg, StringUtils.equals(output, expected) || StringUtils.equals(output, alt));
    }

    @Test
    public void stringToArray() {
        stringToArray(null, ",", null);
        stringToArray("", ",", new String [] { });
        stringToArray("0 1 2 3", null, new String [] { "0", "1", "2", "3" });
        stringToArray("0 1 2\r\n3", "", new String [] { "0", "1", "2", "3" });
        stringToArray("0,1,2,3", ",", new String [] { "0", "1", "2", "3" });
        stringToArray("0, 1, 2, 3", ", ", new String [] { "0", "1", "2", "3" });
    }

    private void stringToArray(String input, String delim, String [] expected) {
        String [] output = StringUtil.stringToArray(input, delim);
        if (output == null) {
            assertTrue("output was null but expected non-null", expected == null);
        }
        else {
            assertTrue("output was non-null but expected null", expected != null);
        }
        if (output == null)  return;
        String msg = "output length was " + output.length + " but expected length was " + expected.length;
        assertTrue(msg, output.length == expected.length);
        for (int i = 0; i < output.length; i++) {
            String o = output[i];
            String e = expected[i];
            msg = "position " + i + " value was [" + o + "] but expected [" + e;
            assertTrue(msg, StringUtils.equals(o, e));
        }
    }

    @Test
    public void stringToArrayList() {
        ArrayList<String> list = StringUtil.stringToArrayList(null, ",");
        assertTrue(list == null);
        list = StringUtil.stringToArrayList("", ",");
        assertTrue(list != null);
        assertTrue(list.size() == 0);

        list = StringUtil.stringToArrayList("0,1,2,3", ",");
        assertTrue(list.size() == 4);
        assertTrue(list.get(0).equals("0"));
        assertTrue(list.get(3).equals("3"));
    }

    @Test
    public void stringToHashSet() {
        HashSet<String> set = StringUtil.stringToHashSet(null, ",");
        assertTrue(set == null);
        set = StringUtil.stringToHashSet("", ",");
        assertTrue(set != null);
        assertTrue(set.size() == 0);

        set = StringUtil.stringToHashSet("0,1,2,3", ",");
        assertTrue(set.size() == 4);
        assertTrue(set.contains("0"));
        assertTrue(set.contains("3"));
    }

    @Test
    public void stringToHashMap() {
        HashMap<String, String> map = StringUtil.stringToHashMap(null, ",");
        assertTrue(map == null);
        map = StringUtil.stringToHashMap("", ",");
        assertTrue(map != null);
        assertTrue(map.size() == 0);

        map = StringUtil.stringToHashMap("0=a,1=b,2=c,3=d", ",");
        assertTrue(map.size() == 4);
        assertTrue(map.get("0").equals("a"));
        assertTrue(map.get("3").equals("d"));
    }

    @Test
    public void equals_Array() {
        String [] a1 = null;
        String [] a2 = null;
        assertTrue(StringUtil.equals(a1, a2));
        a1 = new String [] { "", null };
        assertFalse(StringUtil.equals(a1, a2));
        a2 = new String [] { "", null };
        assertTrue(StringUtil.equals(a1, a2));
        a1 = new String [] { "foo", " " };
        assertFalse(StringUtil.equals(a1, a2));
        a2 = new String [] { "foo", " " };
        assertTrue(StringUtil.equals(a1, a2));
    }

    @Test
    public void equals_Matrix() {
        String [][] m1 = {
            { "foo" },
            { "1", "2" },
            { null }
        };

        String [][] m2 = {
            { "foo" },
            { "1", "2" },
            { null, "" }
        };

        assertFalse(StringUtil.equals(m1, m2));
        m2[2] = new String [] { null };
        assertTrue(StringUtil.equals(m1, m2));
    }

    @Test
    public void breakUpLongLines() {
        String s = "The quick brown fox jumped over the lazy dogs. "
            + "The quick brown fox jumped over the lazy dogs.";
        String result = StringUtil.breakUpLongLines(s, 20, "\n", 0);
        String expected = "The quick brown fox jumped\nover the lazy dogs. "
            + "The\nquick brown fox jumped\nover the lazy dogs.";
        assertTrue(result.equals(expected));

        s = "The quick brown fox jumped123456789 over the lazy dogs. "
            + "The quick brown fox jumped over the lazy dogs.";
        result = StringUtil.breakUpLongLines(s, 20, "\n", 30);
        expected = "The quick brown fox jumped1234\n56789 over the lazy dogs.\n"
            + "The quick brown fox jumped\nover the lazy dogs.";
        assertTrue(result.equals(expected));
    }

    @Test
    public void containsDoubleByteChars() {
        assertTrue(StringUtil.containsDoubleByteChars("Hi\u20ac"));
            // 20ac is the Euro char
        assertFalse(StringUtil.containsDoubleByteChars("quick brown+-*\u0020"));
            // 20 is a space
    }

    @Test
    public void containsNon7BitChars() {
        assertTrue(StringUtil.containsNon7BitChars("Hi\u00ab"));
            // ab is the pound sterling currency symbol
        assertTrue(StringUtil.containsNon7BitChars("Hi\u20ac"));
            // 20ac is the Euro char
        assertFalse(StringUtil.containsNon7BitChars("quick brown+-*\u0020"));
            // 20 is a space
    }

    @Test
    public void normalizeLdapDN() {
        // these are really not strings we normally expect but test anyway
        normalizeLdapDN(null, null);
        normalizeLdapDN("", "");
        normalizeLdapDN(" ", " ");
        normalizeLdapDN("foo", "foo");

        // expected cases
        normalizeLdapDN("CN=Steve Markoff, OU=San Francisco, OU=United States, OU=North America, OU=Wiley Users, DC=wiley, DC=com",
            "cn=Steve Markoff,ou=San Francisco,ou=United States,ou=North America,ou=Wiley Users,dc=wiley,dc=com");
        normalizeLdapDN("CN=Steve Markoff,OU=San Francisco,OU=United States,OU=North America,OU=Wiley Users, DC=wiley, DC=com",
            "cn=Steve Markoff,ou=San Francisco,ou=United States,ou=North America,ou=Wiley Users,dc=wiley,dc=com");
    }

    private void normalizeLdapDN(String input, String expected) {
        String output = StringUtil.normalizeLdapDN(input);
        String msg = "Input was\r\n" + input + "\r\n"
            + "Expected output was\r\n" + expected + " but got\r\n" + output;
        assertTrue(msg, StringUtils.equals(output, expected));
    }
}
