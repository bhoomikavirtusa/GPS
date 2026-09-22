package com.wiley.sf.common.lang;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

/**
 * Provides methods to validate method arguments (parameters).
 *
 * This class is similar to org.apache.commons.lang.Validate but the main
 * difference is that all methods take an argument name to use inside
 * the message, instead of having the choice between a message that
 * doesn't include the argument name or having to specify the full message
 * yourself. Another difference is that this class has some additional
 * methods - notBlank, notLess0, notLess1, inRange, noBlankElements.
 * Also this class has additional notEmpty methods for arrays of primitive
 * types. This class does not have an allElementsOfType method because now
 * type-safety for Collections can be enforced using generics.
 *
 * Suppose you have a method that takes a String argument that is
 * expected to be non-blank, and takes an int argument
 * that is expected to be no less than 1. In this case you would
 * call ArgUtil.notBlank() and ArgUtil.notLess1() with the
 * these parameters in the beginning of your method to validate that
 * everything is as expected before continuing.
 *
 * @since    JDK 1.5
 * @version  $Id: ArgUtil.java,v 1.3 2013-02-23 00:11:26 smarkoff Exp $
 * @author   Steve Markoff
 */

public class ArgUtil {

    /**
     * Checks to see if input object is null and throws an
     * IllegalArgumentException if it is null.
     *
     * @param o     Object to check
     * @param name  What does this object represent (used in exception message)
     *
     * @throws IllegalArgumentException  If the object is null
     */
    public static void notNull(Object o, String name) {
        if (o == null) {
            throw new IllegalArgumentException(name + " cannot be null");
        }
    }

    /**
     * Checks to see if input string is blank (null or whitespace) and throws
     * an IllegalArgumentException if it is blank.
     *
     * @param s     String to check
     * @param name  What does this string represent (used in exception message)
     *
     * @throws IllegalArgumentException  If the string is blank
     */
    public static void notBlank(String s, String name) {
        if (StringUtils.isBlank(s)) {
            String problem = (s == null) ? "null" : "blank";
            throw new IllegalArgumentException(name + " cannot be " + problem);
        }
    }

    /**
     * Checks to see if input string is empty (null or zero-length) and throws
     * an IllegalArgumentException if it is empty.
     *
     * @param s     String to check
     * @param name  What does this string represent (used in exception message)
     *
     * @throws IllegalArgumentException  If the string is empty
     */
    public static void notEmpty(String s, String name) {
        if (StringUtils.isEmpty(s)) {
            String problem = (s == null) ? "null" : "zero-length";
            throw new IllegalArgumentException(name + " cannot be " + problem);
        }
    }

    /**
     * Checks to see if the input array is null or zero-length and throws
     * an IllegalArgumentException if it is null or zero-length.
     *
     * @param array  Array to check
     * @param name   What does this array represent (used in exception message)
     *
     * @throws IllegalArgumentException  If the array is null or zero-length
     */
    public static void notEmpty(Object [] array, String name) {
        if (array == null) {
            throw new IllegalArgumentException(name + " cannot be null");
        }
        else if (array.length == 0) {
            throw new IllegalArgumentException(name + " cannot be zero-length");
        }
    }

    /**
     * Checks to see if the input array is null or zero-length and throws
     * an IllegalArgumentException if it is null or zero-length.
     *
     * @param array  Array to check
     * @param name   What does this array represent (used in exception message)
     *
     * @throws IllegalArgumentException  If the array is null or zero-length
     */
    public static void notEmpty(byte [] array, String name) {
        if (array == null) {
            throw new IllegalArgumentException(name + " cannot be null");
        }
        else if (array.length == 0) {
            throw new IllegalArgumentException(name + " cannot be zero-length");
        }
    }

    /**
     * Checks to see if the input array is null or zero-length and throws
     * an IllegalArgumentException if it is null or zero-length.
     *
     * @param array  Array to check
     * @param name   What does this array represent (used in exception message)
     *
     * @throws IllegalArgumentException  If the array is null or zero-length
     */
    public static void notEmpty(int [] array, String name) {
        if (array == null) {
            throw new IllegalArgumentException(name + " cannot be null");
        }
        else if (array.length == 0) {
            throw new IllegalArgumentException(name + " cannot be zero-length");
        }
    }

    /**
     * Checks to see if the input array is null or zero-length and throws
     * an IllegalArgumentException if it is null or zero-length.
     *
     * @param array  Array to check
     * @param name   What does this array represent (used in exception message)
     *
     * @throws IllegalArgumentException  If the array is null or zero-length
     */
    public static void notEmpty(char [] array, String name) {
        if (array == null) {
            throw new IllegalArgumentException(name + " cannot be null");
        }
        else if (array.length == 0) {
            throw new IllegalArgumentException(name + " cannot be zero-length");
        }
    }

    /**
     * Checks to see if the input Collection is null or zero-size and throws
     * an IllegalArgumentException if it is null or zero-size.
     *
     * @param c      Collection to check
     * @param name   What does this Collection represent (used in exception message)
     *
     * @throws IllegalArgumentException  If the Collection is null or zero-size
     */
    public static void notEmpty(Collection<?> c, String name) {
        if (c == null) {
            throw new IllegalArgumentException(name + " cannot be null");
        }
        else if (c.size() == 0) {
            throw new IllegalArgumentException(name + " cannot be zero-size");
        }
    }

    /**
     * Checks to see if the input Map is null or zero-size and throws
     * an IllegalArgumentException if it is null or zero-size.
     *
     * @param map    Map to check
     * @param name   What does this Map represent (used in exception message)
     *
     * @throws IllegalArgumentException  If the Map is null or zero-size
     */
    public static void notEmpty(Map<?,?> map, String name) {
        if (map == null) {
            throw new IllegalArgumentException(name + " cannot be null");
        }
        else if (map.size() == 0) {
            throw new IllegalArgumentException(name + " cannot be zero-size");
        }
    }

    /**
     * Checks to see if the given array has any null elements or is null and
     * throws an IllegalArgumentException if so.
     *
     * @param array  Array to check
     * @param name   What does this array represent (used in exception message)
     *
     * @throws IllegalArgumentException  If the array has any null elements or is null
     */
    public static void noNullElements(Object [] array, String name) {
        if (array == null) {
	        throw new IllegalArgumentException(name + " cannot be null");
	    }
	    else {
            for (int i = 0; i < array.length; i++) {
                if (array[i] == null) {
                    throw new IllegalArgumentException(name
		                + " contains null element at index " + i);
                }
	        }
	    }
    }

    /**
     * Checks to see if the given array has any blank elements or is null and
     * throws an IllegalArgumentException if so.
     *
     * @param array  Array to check
     * @param name   What does this array represent (used in exception message)
     *
     * @throws IllegalArgumentException  If the array has any blank elements or is null
     */
    public static void noBlankElements(String [] array, String name) {
		noNullElements(array, name);

        for (int i = 0; i < array.length; i++) {
            if (StringUtils.isBlank(array[i])) {
			    throw new IllegalArgumentException(name
				    + " contains blank element at index " + i);
			}
	    }
    }

    /**
     * Checks to see if the given Collection has any null elements or is null and
     * throws an IllegalArgumentException if so.
     *
     * @param c     Collection to check
     * @param name  What does this Collection represent (used in exception message)
     *
     * @throws IllegalArgumentException  If the Collection has any null elements or is null
     */
    public static void noNullElements(Collection<?> c, String name) {
        if (c == null) {
	        throw new IllegalArgumentException(name + " cannot be null");
	    }
	    else if (c.contains(null)) {
            throw new IllegalArgumentException(name + " contains null element");
	    }
    }

    /**
     * Checks to see if the given Collection has any blank elements or is null and
     * throws an IllegalArgumentException if so.
     *
     * @param c     Collection to check
     * @param name  What does this Collection represent (used in exception message)
     *
     * @throws IllegalArgumentException  If the Collection has any blank elements or is null
     */
    public static void noBlankElements(Collection<String> c, String name) {
		noNullElements(c, name);

	    Iterator<String> it = c.iterator();
	    while (it.hasNext()) {
			if (StringUtils.isBlank(it.next())) {
				throw new IllegalArgumentException(name + " contains blank element");
			}
		}
    }

    /**
     * Checks to see if input int is < 0 and throws an
     * IllegalArgumentException if it is < 0.
     *
     * @param i     int to check
     * @param name  What does this int represent (used in exception message)
     *
     * @throws IllegalArgumentException  If the int is < 0
     */
    public static void notLess0(int i, String name) {
        if (i < 0) {
            throw new IllegalArgumentException(name
                + " cannot be < 0 (is " + i + ")");
        }
    }

    /**
     * Checks to see if input long is < 0 and throws an
     * IllegalArgumentException if it is < 0.
     *
     * @param l     long to check
     * @param name  What does this long represent (used in exception message)
     *
     * @throws IllegalArgumentException  If the long is < 0
     */
    public static void notLess0(long l, String name) {
        if (l < 0) {
            throw new IllegalArgumentException(name
                + " cannot be < 0 (is " + l + ")");
        }
    }

    /**
     * Checks to see if input int is < 1 and throws an
     * IllegalArgumentException if it is < 1.
     *
     * @param i     int to check
     * @param name  What does this int represent (used in exception message)
     *
     * @throws IllegalArgumentException  If the int is < 1
     */
    public static void notLess1(int i, String name) {
        if (i < 1) {
            throw new IllegalArgumentException(name
                + " cannot be < 1 (is " + i + ")");
        }
    }

    /**
     * Checks to see if input long is < 1 and throws an
     * IllegalArgumentException if it is < 1.
     *
     * @param l     long to check
     * @param name  What does this long represent (used in exception message)
     *
     * @throws IllegalArgumentException  If the int is < 1
     */
    public static void notLess1(long l, String name) {
        if (l < 1) {
            throw new IllegalArgumentException(name
                + " cannot be < 1 (is " + l + ")");
        }
    }

    /**
     * Checks to see if input int is within the given range and throws an
     * IllegalArgumentException if it is not.
     *
     * @param i     int to check
     * @param name  What does this int represent (used in exception message)
     * @param min   Start of range (inclusive)
     * @param max   End of range (inclusive)
     *
     * @throws IllegalArgumentException  If the int is not within the given range
     */
    public static void inRange(int i, String name, int min, int max) {
        if (i < min) {
            throw new IllegalArgumentException(name + " cannot be < " + min
                + " (is " + i + ")");
        }
        if (i > max) {
            throw new IllegalArgumentException(name + " cannot be > " + max
                + " (is " + i + ")");
        }
    }

    /**
     * Checks to see if input long is within the given range and throws an
     * IllegalArgumentException if it is not.
     *
     * @param l     long to check
     * @param name  What does this long represent (used in exception message)
     * @param min   Start of range (inclusive)
     * @param max   End of range (inclusive)
     *
     * @throws IllegalArgumentException  If the long is not within the given range
     */
    public static void inRange(long l, String name, long min, long max) {
        if (l < min) {
            throw new IllegalArgumentException(name + " cannot be < " + min
                + " (is " + l + ")");
        }
        if (l > max) {
            throw new IllegalArgumentException(name + " cannot be > " + max
                + " (is " + l + ")");
        }
    }


    /**
     * There is no reason to ever create an instance of this class
     * since all methods are static.
     */
    private ArgUtil() { }
}
