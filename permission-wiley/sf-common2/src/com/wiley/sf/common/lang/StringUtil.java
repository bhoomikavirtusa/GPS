/*
 * &copy; John Wiley &amp; Sons, Inc
 */
package com.wiley.sf.common.lang;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;

/**
 * General String manipulation utilities.
 *
 * Some methods that this class does NOT contain because they can be found
 * in org.apache.commons.lang.StringUtils:
 * capitalize, contains, containsIgnoreCase, containsAny, containsOnly,
 * isBlank, isEmpty, equals, equalsIgnoreCase, remove,
 * startsWithIgnoreCase, endsWithIgnoreCase,
 * left, right, leftPad, rightPad, center, indexOfAny,
 * deleteWhitespace, stripToNull.
 *
 * @since   JDK 1.6
 * @version $Id: StringUtil.java,v 1.44 2014-02-26 00:24:19 smarkoff Exp $
 * @author  Steve Markoff
 */
public class StringUtil {

    /**
     * Similar to org.apache.commons.lang.StringUtils.left()
     * but adds "..." and optionally "[xx more chars]" to the end of the result string.
     * And unlike left(), a limit of 0 or less means no limit.
     *
     * @param s  May be null
     * @param limit  Anything less than 1 will be treated as no limit
     * @param showNumChars  show "...[xx more chars]" instead of just "..."
     */
    public static String truncate(String s, int limit, boolean showNumChars) {
        if (s == null)  return null;
        if (limit <= 0)  limit = Integer.MAX_VALUE;
        if (s.length() <= limit)  return s;

        final int minKeepLength = 5;
        final String shortEnd = "...";
        if (limit - shortEnd.length() < minKeepLength)  return StringUtils.left(s, limit);

        // In some cases we may not use the full allowed limit because we calculate
        // the longEnd using the total number of chars - would be tricky to change this I think.
        String longEnd = "...[" + s.length() + " more chars]";
        if (showNumChars && (limit - longEnd.length()) >= minKeepLength) {
            return s.substring(0, limit - longEnd.length())
                + "...[" + (s.length() - (limit - longEnd.length())) + " more chars]";
        }
        else {
            return s.substring(0, limit - shortEnd.length()) + "...";
        }
    }

    /**
     * Returns true if the String contains any whitespace character.
     *
     * @param s  If null then false is returned
     */
    public static boolean containsWhitespace(String s) {
        if (s == null)  return false;

        for (int i = 0; i < s.length(); i++) {
            if (Character.isWhitespace(s.charAt(i)))  return true;
        }

        return false;
    }

    public static boolean allDigits(String s) {
        return StringUtils.containsOnly(s, "0123456789");
    }

    public static boolean allDigitsOrWhitespace(String s) {
        // Technically should use Character.isWhitespace() to also check
        // for multi-byte Unicode whitespace (but not bothering).
        return StringUtils.containsOnly(s, "0123456789 \r\n\t");
    }

    /**
     * Returns the index of the first occurrence of one String
     * (case INsensitive) within another, after a start index.
     *
     * @param s     The String to be searched (null will return -1)
     * @param find  The String to find the index of (null will return -1)
     *              If zero-length, then 0 will be returned
     *
     * @return      The index where the String was found, or -1
     *              if the String was not found.
     */
    public static int indexOfIgnoreCase(String s, String find) {
        return indexOfIgnoreCase(s, find, 0);
    }

    /**
     * Returns the index of the first occurrence of one String
     * (case INsensitive) within another, after a start index.
     *
     * @param s     The String to be searched (null will return -1)
     * @param find  The String to find the index of (null will return -1)
     *              If zero-length, then start value will be returned
     * @param start The index to start searching at (must be >= 0)
     * @return      The index where the String was found, or -1
     *              if the String was not found.
     */
    public static int indexOfIgnoreCase(String s, String find, int start) {
        if (s == null || find == null)  return -1;
        int numberMatched = 0;

        // check for zero-length search string
        if (find.length() == 0)  return start;

        // We convert the find string all to lowercase since this string
        // is generally not long, and we will probably look at each char
        // more than once.
        // We only convert the characters we look at in the search string
        // since this string might be very long, and we will only look at
        // each char once.

        String findLow = find.toLowerCase();

        for (int i = start; i < s.length(); i++) {
            char c = findLow.charAt(numberMatched);
            char c2 = Character.toLowerCase(s.charAt(i));

            if (c == c2) {
                numberMatched++;
                if (numberMatched == find.length())  return start;
            }
            else  {
                numberMatched = 0;
                start = i + 1;
            }
        }
        return -1;  // reached end of string before found
    }

    // org.apache.commons.lang.StringUtils has a indexOfAny() method
    // but it's missing a version that takes a start index.

    /**
     * Returns the index of the first occurrence of any of several
     * characters (case sensitive) within a String.
     *
     * @param s      The String to be searched (null will return -1)
     * @param chars  The characters to look for (null will return -1)
     *
     * @return       The index where one of the characters was found,
     *               or -1 if none were found.
     */
    public static int indexOfAny(String s, String chars) {
        return indexOfAny(s, chars, 0);
    }

    /**
     * Returns the index of the first occurrence of any of several
     * characters (case sensitive) within a String, after a start index.
     *
     * @param s      The String to be searched (null will return -1)
     * @param chars  The characters to look for (null will return -1)
     * @param start  The index to start searching at.
     *               Any index < 0 will be treated as 0.
     *               Any index >= s.length() will always produce a return
     *               value of -1.
     * @return       The index where one of the characters was found,
     *               or -1 if none were found.
     */
    public static int indexOfAny(String s, String chars, int start) {
        if (s == null || chars == null)  return -1;
        if (start < 0)  start = 0;

        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            for (int j = 0; j < chars.length(); j++) {
                if (c == chars.charAt(j))  return i;
            }
        }

        return -1;
    }

    /**
     * Returns true if the given String is equal (case-sensitive) to any of
     * the Strings in the given array. If the given String is null or the
     * given array is null or zero-length then false will be returned.
     *
     * @param s      May be null
     * @param array  May be null or zero-length
     */
    public static boolean equalsAny(String s, String [] array) {
        if (s == null || array == null)  return false;

        for (int i = 0; i < array.length; i++) {
            if (s.equals(array[i]))  return true;
        }

        return false;
    }

    /**
     * Returns true if the given String is equal (case-sensitive) to any of
     * the Strings in the given List. If the given String is null or the
     * given List is null or zero-length then false will be returned.
     *
     * @param s     May be null
     * @param list  May be null or zero-length
     */
    public static boolean equalsAny(String s, List<String> list) {
        if (s == null || list == null)  return false;

        Iterator<String> it = list.iterator();
        while (it.hasNext()) {
            if (s.equals(it.next()))  return true;
        }

        return false;
    }

    /**
     * Returns true if the given String is equal (case-INsensitive) to any of
     * the Strings in the given array. If the given String is null or the
     * given array is null or zero-length then false will be returned.
     *
     * @param s      May be null
     * @param array  May be null or zero-length
     */
    public static boolean equalsAnyIgnoreCase(String s, String [] array) {
        if (s == null || array == null)  return false;

        for (int i = 0; i < array.length; i++) {
            if (s.equalsIgnoreCase(array[i]))  return true;
        }

        return false;
    }

    /**
     * Returns true if the given String is equal (case-INsensitive) to any of
     * the Strings in the given List. If the given String is null or the
     * given List is null or zero-length then false will be returned.
     *
     * @param s     May be null
     * @param list  May be null or zero-length
     */
    public static boolean equalsAnyIgnoreCase(String s, List<String> list) {
        if (s == null || list == null)  return false;

        Iterator<String> it = list.iterator();
        while (it.hasNext()) {
            if (s.equalsIgnoreCase(it.next()))  return true;
        }

        return false;
    }

    /**
     * Returns true if both input strings are null or both are equal
     * ignoring any whitespace in either string.
     *
     * @param s1  May be null
     * @param s2  May be null
     */
    public static boolean equalsIgnoreWhitespace(String s1, String s2) {
        if (s1 == null || s2 == null)  return (s1 == s2);
        s1 = StringUtils.deleteWhitespace(s1);
        s2 = StringUtils.deleteWhitespace(s2);
        return s1.equals(s2);
    }

    /**
     * Replaces all instances of 'find' with 'replacement' in the string
     * where 'find' is treat case insensitively.
     *
     * @param s  the String to search to perform replacements on
     * @param find  the String that should be replaced
     * @param replacement  the String that will replace all instances of find
     *
     * @return a String will all instances of find replaced by replacement
     */
    public static final String replaceIgnoreCase(String s, String find,
            String replacement)
    {
        if (s == null)  return null;

        String lower = s.toLowerCase();
        String findLower = find.toLowerCase();
        int i = 0;
        if ((i = lower.indexOf(findLower, i)) != -1) {
            char [] s2 = s.toCharArray();
            char [] repArray = replacement.toCharArray();
            int findLength = find.length();
            StringBuilder sb = new StringBuilder(s2.length);
            sb.append(s2, 0, i).append(repArray);
            i += findLength;
            int j = i;
            while ((i=lower.indexOf(findLower, i)) > 0) {
                sb.append(s2, j, i - j).append(repArray);
                i += findLength;
                j = i;
            }
            sb.append(s2, j, s2.length - j);
            return sb.toString();
        }

        return s;
    }

    public static String removeIgnoreCase(String s, String remove) {
        return replaceIgnoreCase(s, remove, "");
    }

    /**
     * Removes any and all (and all occurrences) of the characters in the
     * second String from the first and returns the result.
     * Everything is case sensitive.
     *
     * @param s  If null then null will be returned
     * @param chars  Must be non-null
     */
    public static String removeAny(String s, String chars) {
        return replaceAny(s, chars, "");
    }

    /**
     * Removes all control characters in the 7bit range except
     * tab, newline, and carriage return.
     *
     * @param s  If null then null will be returned
     *
     * @see com.wiley.sf.common.io.FixWindows1252Chars
     */
    public static String removeSpecialControlChars(String s) {
        if (s == null || s.length() == 0)  return s;

        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c >= 32 && c <= 126) || c == 9 || c == 10 || c == 13 || c > 127) {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    /**
     * Removes all control characters in the 7bit range except
     * tab, newline, and carriage return.
     *
     * @param s  If null then null will be returned
     * @param fieldName  Should be non-null
     * @param methodName  Should be non-null
     * @param log  Must be non-null
     *
     * @see com.wiley.sf.common.io.FixWindows1252Chars
     */
    public static String removeSpecialControlChars(String s, String fieldName, String methodName, Log log) {
        if (s == null || s.length() == 0)  return s;
        ArgUtil.notNull(log, "log");

        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c >= 32 && c <= 126) || c == 9 || c == 10 || c == 13 || c > 127) {
                sb.append(c);
            }
            else {
                log.debug(methodName + "(): " + fieldName + " contained char " + ((int)c) + " - removed.");
            }
        }

        return sb.toString();
    }

    /**
     * Replaces any and all (and all occurrences) of the characters in the
     * second String from the first and returns the result.
     * Everything is case sensitive.
     *
     * @param s  If null then null will be returned
     * @param chars  Must be non-null
     */
    public static String replaceAny(String s, String chars, String replacement) {
        if (s == null)  return null;

        StringBuilder sb = new StringBuilder(s.length());

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (chars.indexOf(c) == -1) {
                sb.append(c);
            }
            else {
                sb.append(replacement);
            }
        }

        return sb.toString();
    }

    public static String createStringOfLength(int length) {
        return createStringOfLength(length, "abcde67890");
    }

    /**
     * Create a string of the given length using the given repeat string.
     *
     * @param length  Any number less than 0 is treated as if it were 0
     * @param repeat  Must be non-empty. The output string will consist of this string repeated as
     *                  needed (but truncated to the length requested)
     */
    public static String createStringOfLength(int length, String repeat) {
        ArgUtil.notEmpty(repeat, "repeat");
        if (length <= 0)  return "";
        if (length == 1)  return repeat.substring(0, 1);

        int count = ((length - 1) / repeat.length()) + 1;
        StringBuilder sb = new StringBuilder(count * repeat.length());
        for (int i = 0; i < count; i++) {
            sb.append(repeat);
        }
        return sb.substring(0, length);
    }

    /**
     * Converts the given array to a string of the form
     * "element1[separator]element2..etc".
     * The separator is NOT put after the last element.
     * If the array is zero-length then the empty string will be returned.
     * Elements are displayed using their toString methods.
     * If an element is null, it will be displayed as "null".
     *
     * @param array       If null, then null will be returned
     * @param separator   If null, then "\n" will be used
     */
    public static String arrayToString(Object [] array, String separator) {
        if (array == null)  return null;
        if (separator == null)  separator = "\n";

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < array.length; i++) {
            if (i > 0)  sb.append(separator);
            sb.append(array[i]);
        }

        return sb.toString();
    }

    /**
     * Converts the given Collection to a string of the form
     * "element1[separator]element2..etc".
     * The separator is NOT put after the last element.
     * If the Collection is empty then the empty string will be returned.
     * Elements are displayed using their toString methods.
     * If an element is null, it will be displayed as "null".
     *
     * @param col         If null, then null will be returned
     * @param separator   If null, then "\n" will be used
     */
    public static String collectionToString(Collection<?> col, String separator) {
        if (col == null)  return null;
        if (separator == null)  separator = "\n";

        StringBuilder sb = new StringBuilder();

        for (Iterator<?> it = col.iterator(); it.hasNext();) {
        	sb.append(it.next());
            if (it.hasNext()) sb.append(separator);
        }

        return sb.toString();
    }

	/**
	 * Converts the given Collection to a string of the form
	 * "[start]element1[end][separator][start]element2..etc".
	 * The separator is NOT put after the last element.
	 * If the Collection is empty then the empty string will be returned.
	 * Elements are displayed using their toString methods.
	 * If an element is null, it will be displayed as "null".
	 *
	 * @param col        If null, then null will be returned
	 * @param separator  If null, then "\n" will be used
	 * @param start      If null, then "" will be used
	 * @param end        If null, then "" will be used
	 */
	public static String collectionToString(Collection<?> col,
	    String separator, String start, String end)
	{
		if (col == null) return null;
		if (separator == null)  separator = "\n";
		if (start == null)  start = "";
		if (end == null)  end = "";

		StringBuilder sb = new StringBuilder();

		for (Iterator<?> it = col.iterator(); it.hasNext();) {
			sb.append(start);
			sb.append(it.next());
			sb.append(end);
			if (it.hasNext()) sb.append(separator);
		}

		return sb.toString();
	}

    /**
     * Converts the given Collection to multiple strings of the form
     * "element1[separator]element2..etc".
     * The separator is NOT put after the last element.
     * If the Collection is empty then an array of length 0 will be returned.
     * Elements are displayed using their toString methods.
     * If an element is null, it will be displayed as "null".
     *
     * @param col        If null, then null will be returned
     * @param separator  If null, then "\n" will be used
     * @param size       Anything less than 1 means an array of length 1 will be returned
     */
    public static String[] collectionToString(Collection<?> col,
            String separator, int size) {
        if (col == null)  return null;
        if (col.size() == 0)  return new String[0];
        if (separator == null)  separator = "\n";
        if (size < 1) return new String[] { collectionToString(col, separator) };

        ArrayList<String> results = new ArrayList<String>();
        Object[] objects = col.toArray();
        StringBuilder sb = new StringBuilder();

        for (int x = 1 ; x <= objects.length ; x++) {
            sb.append(objects[x - 1]);
            if (x % size == 0) {
                results.add(sb.toString());
                sb = new StringBuilder();
                continue;
            }
            if (x != objects.length) sb.append(separator);
        }
        if (sb.length() > 0) results.add(sb.toString());
        return results.toArray(new String[0]);
    }

    /**
     * Converts the given Map to a string of the form
     * "key1=value1[separator]key2=value2..etc".
     * The separator is NOT put after the last key/value pair.
     * If the Map is empty then the empty string will be returned.
     * Keys and values are displayed using their toString methods.
     * If a key or value is null, it will be displayed as "null".
     *
     * @param map        If null, then null will be returned
     * @param separator  If null, then "\n" will be used
     */
    public static String mapToString(Map<?,?> map, String separator) {
    	return mapToString(map, separator, "=");
    }

    /**
     * Converts the given Map to a string of the form
     * "key1[keyValueSeparator]value1[separator]key2[keyValueSeparator]value2..etc".
     * The separator is NOT put after the last key/value pair.
     * If the Map is empty then the empty string will be returned.
     * Keys and values are displayed using their toString methods,
     * except if a value is a String array it will be displayed in the
     * format {string1, string2}.
     * If a key or value is null, it will be displayed as "null".
     *
     * @param map        If null, then null will be returned
     * @param separator  If null, then "\n" will be used
     * @param keyValueSeparator  If null, then "=" will be used
     */
    public static String mapToString(Map<?,?> map, String separator,
    	String keyValueSeparator)
    {
        if (map == null)  return null;
        if (separator == null)  separator = "\n";
        if (keyValueSeparator == null)  separator = "=";

        StringBuilder sb = new StringBuilder();

        Set<?> keySet = map.keySet();
        Iterator<?> it = keySet.iterator();

        while (it.hasNext()) {
            Object key = it.next();
            Object value = map.get(key);
            sb.append(key);
            sb.append(keyValueSeparator);
            if (value == null || !(value instanceof String[])) {
                sb.append(value);
            }
            else {
                String [] array = (String []) value;
                sb.append("{");
                sb.append(arrayToString(array, ", "));
                sb.append("}");
            }
            if (it.hasNext()) sb.append(separator);
        }

        return sb.toString();
    }

    /**
     * Convert a List<String> to a String array (String []).
     *
     * @param list  If null then null is returned.
     */
    public static String [] stringListToStringArray(List<String> list) {
        if (list == null)  return null;
        String [] sa = new String[list.size()];
        Iterator<String> it = list.iterator();
        for (int i = 0; it.hasNext(); i++) {
            sa[i] = it.next();
        }
        return sa;
    }

    /**
     * Creates a String array given a String of the form
     * "element1[delim]element2..etc".
     * If the given String is zero-length then a zero-length array is returned.
     *
     * Has the same behavior as org.apache.commons.lang.StringUtils.split().
     *
     * @param s      If null then null is returned
     * @param delim  null/empty splits on whitespace, multiple chars treated as StringTokenizer delim
     */
    public static String[] stringToArray(String s, String delim) {
        ArrayList<String> list = stringToArrayList(s, delim);
        return stringListToStringArray(list);
    }

    /**
     * Creates an ArrayList<String> given a String of the form
     * "element1[delim]element2".
     * If the given String is zero-length then a zero-size List is returned.
     *
     * @param s      If null then null is returned
     * @param delim  null/empty splits on whitespace, multiple chars treated as StringTokenizer delim
     */
    public static ArrayList<String> stringToArrayList(String s, String delim) {
        if (s == null) return null;
        if (StringUtils.isEmpty(delim))  delim = " \t\n\r\f";
        ArrayList<String> list = new ArrayList<String>();
        StringTokenizer tok = new StringTokenizer(s, delim);
        while (tok.hasMoreTokens()) {
            list.add(tok.nextToken());
        }
        return list;
    }

    /**
     * Creates a HashSet<String> given a String of the form
     * "element1[delim]element2..etc".
     * If the given String is zero-length then an empty Set is returned.
     *
     * @param s      If null then null is returned
     * @param delim  null/empty splits on whitespace, multiple chars treated as StringTokenizer delim
     */
    public static HashSet<String> stringToHashSet(String s, String delim) {
        if (s == null) return null;
        if (StringUtils.isEmpty(delim))  delim = " \t\n\r\f";
        HashSet<String> set = new HashSet<String>();
        StringTokenizer tok = new StringTokenizer(s, delim);
        while (tok.hasMoreTokens()) {
            set.add(tok.nextToken());
        }
        return set;
    }

    /**
     * Creates a HashMap<String, String> given a String of the form
     * "key1=value1[delim]key2=value2..etc".
     * If the String is zero-length then a Map with zero elements is returned.
     * Keys or values of "null" will be converted to actual null.
     *
     * @param s      If null then null will be returned
     * @param delim  null/empty splits on whitespace, multiple chars treated as StringTokenizer delim
     */
    public static HashMap<String, String> stringToHashMap(String s, String delim) {
    	return stringToHashMap(s, delim, "=");
    }

    /**
     * Creates a HashMap<String, String> given a String of the form
     * "key1[keyValueSeparator]value1[delim]key2[keyValueSeparator]value2..etc".
     * If the String is zero-length then a Map with zero elements is returned.
     * Keys or values of "null" will be converted to actual null.
     *
     * @param s      If null then null will be returned
     * @param delim  null/empty splits on whitespace, multiple chars treated as StringTokenizer delim
     * @param keyValueSeparator  If null/empty then will default to "="
     */
    public static HashMap<String, String> stringToHashMap(String s,
        String delim, String keyValueSeparator)
    {
        if (s == null) return null;
        if (StringUtils.isEmpty(delim))  delim = " \t\n\r\f";
        if (StringUtils.isEmpty(keyValueSeparator))  keyValueSeparator = "=";
        HashMap<String, String> map = new HashMap<String, String>();
        StringTokenizer tok = new StringTokenizer(s, delim);
        while (tok.hasMoreTokens()) {
            String kv = tok.nextToken();
            int index = kv.indexOf(keyValueSeparator);
            if (index == -1) {
                throw new RuntimeException(
                    "Missing keyValueSeparator[" + keyValueSeparator
                        + "] in String to be read as Map.");
            }
            String key = kv.substring(0, index);
            String value = kv.substring(index + 1);
            if (key.equals("null"))  key = null;
            if (value.equals("null"))  value = null;
            map.put(key, value);
        }
        return map;
    }

    /**
     * Returns true if and only if the given String arrays are the same
     * in every way. Nulls are checked for.
     *
     * @param a1  May be null or zero-length
     * @param a2  May be null or zero-length
     */
    public static boolean equals(String [] a1, String [] a2) {
        if (a1 == null && a2 == null)  return true;
        if (a1 == null || a2 == null)  return false;
        if (a1.length != a2.length)  return false;

        for (int i = 0; i < a1.length; i++) {
            if (!StringUtils.equals(a1[i], a2[i]))  return false;
        }

        return true;
    }

    /**
     * Returns true if and only if the given String 2-dimensional arrays
     * are the same in every way. Nulls are checked for.
     *
     * @param m1  May be null or zero-length
     * @param m2  May be null or zero-length
     */
    public static boolean equals(String [][] m1, String [][] m2) {
        if (m1 == null && m2 == null)  return true;
        if (m1 == null || m2 == null)  return false;
        if (m1.length != m2.length)  return false;

        for (int i = 0; i < m1.length; i++) {
            if (!equals(m1[i], m2[i]))  return false;
        }

        return true;
    }

    /**
     * Returns true if and only if the given lists of String arrays
     * are the same in every way. Nulls are checked for.
     *
     * @param m1  May be null or zero-length
     * @param m2  May be null or zero-length
     */
    public static boolean equals(List<String []> list1, List<String []> list2) {
        if (list1 == null && list2 == null)  return true;
        if (list1 == null || list2 == null)  return false;
        if (list1.size() != list2.size())  return false;

        for (int i = 0; i < list1.size(); i++) {
            if (!equals(list1.get(i), list2.get(i)))  return false;
        }
        return true;
    }

    /**
     * Breaks up long lines over a specified length with the chosen breakup
     * (would generally be optional carriage return + newline).
     * The algorithm used is to go along the text and when any line exceeds the
     * given amount to convert the next whitespace character (if it's not CR/LF)
     * into the given breakup string. But if the hardLineLength is reached then
     * the line will be broken immediately at that point.
     * (ToDo: Would be nice to try and break up line before soft limit rather
     * than after.)
     *
     * @param text            If null, null will be returned
     * @param softLineLength  Should be > 1 but if < 1 then method will have no effect
     * @param breakup         Should be non-null but if null then method will have no effect
     * @param hardLineLength  Should be > softLineLength, anything less will be converted to Integer.MAX_VALUE
     */
    public static String breakUpLongLines(String text, int softLineLength,
        String breakup, int hardLineLength)
    {
        if (text == null)  return null;

        if (text.length() < softLineLength || breakup == null) {
            return text;
        }

        if (hardLineLength < softLineLength)  hardLineLength = Integer.MAX_VALUE;

        int bufSize = text.length()
            + (breakup.length() * (text.length() / softLineLength));
        StringBuilder sb = new StringBuilder(bufSize);
        int runSize = 0;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n' || c == '\r') {
                sb.append(c);
                runSize = 0;
            }
            else if (runSize >= softLineLength && Character.isWhitespace(c)) {
                sb.append(breakup);
                runSize = 0;
            }
            else if (runSize >= hardLineLength) {
                sb.append(breakup);
                sb.append(c);
                runSize = 1;
            }
            else {
                sb.append(c);
                runSize++;
            }
        }

        return sb.toString();
    }

    /**
     * Returns true if any character in the input string is a double-byte
     * character (one whose value is greater than 255).
     *
     * @param s  Must be non-null
     */
    public static boolean containsDoubleByteChars(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) > 255)  return true;
        }
        return false;
    }

    /**
     * Returns true if any character in the input string is an extended
     * ASCII character (in the 8-bit range instead of the 7-bit range)
     * or a double-byte character.
     *
     * @param s  Must be non-null
     */
    public static boolean containsNon7BitChars(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) > 127)  return true;
        }
        return false;
    }

    /**
     * Returns true if any character in the input string is an extended
     * ASCII character (in the 8-bit range instead of the 7-bit range)
     * or a double-byte character or a control character (in the 7-bit range).
     *
     * @param s  Must be non-null
     */
    public static boolean containsNon7BitOrControlChars(String s) {
        // 7bit control chars are: 0--31, 127
        // 7bit non-control are: 32-126
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!(c >= 32 && c <= 126))  return true;
        }
        return false;
    }

    /**
     * Returns true if any character in the input string is an extended
     * ASCII character (in the 8-bit range instead of the 7-bit range)
     * or a double-byte character or a special control character (in the 7-bit range).
     * By special, we mean not a carriage return, new line, or tab.
     *
     * @param s  Must be non-null
     */
    public static boolean containsNon7BitOrSpecialControlChars(String s) {
        // 7bit special control chars are: 0-8, 11, 12, 14-31, 127
        // 7bit non-control are: 9, 10, 13, 32-126
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!(c == 9 || c == 10 || c == 13 || (c >= 32 && c <= 126)))  return true;
        }
        return false;
    }

    /**
     * A shortcut for System.getProperty("line.separator").
     * Better to use this method to avoid typos in the name of the property.
     */
    public static String getLineSeparator() {
        return System.getProperty("line.separator");
    }

    /**
     * Removes spaces after comma's (if there are any)
     * and lowercases all field names (if not already lowercased).
     *
     * @param dn  If null then returns null
     */
    public static String normalizeLdapDN(String dn) {
        if (dn == null)  return null;
        StringBuilder sb = new StringBuilder(dn.length());
        char lastChar = 'a';
        boolean inName = true;
        for (int i = 0; i < dn.length(); i++) {
            char c = dn.charAt(i);
            if (lastChar != ',' || c != ' ') {
                sb.append(inName ? Character.toLowerCase(c) : c);
            }
            lastChar = c;
            if (c == '=')  inName = false;
            if (c == ',')  inName = true;
        }
        return sb.toString();
    }

    /**
     * There is no reason to ever create an instance of this class
     * since all methods are static.
     */
    private StringUtil() { }
}
