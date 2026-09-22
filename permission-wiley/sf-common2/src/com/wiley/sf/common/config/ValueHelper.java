package com.wiley.sf.common.config;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;

/**
 * This is a base class for cases where you want to obtain values from
 * a source, given a name for each value.
 * A number of sources that contain name/values pairs exist.
 * These include: Applet parameters, Properties, Servlet init parameters,
 *                and Servlet request parameters.
 * This base class provides all the methods for converting a String
 * into various data types, and either throwing an IllegalArgumentException,
 * or using a default value if the value returned from the source is null or
 * not suitable for converting to the desired data type.
 * A subclass only needs to implement two simple abstract methods,
 * and then inherits the rest. A subclass may provide additional methods
 * that are specific to the source.
 *
 * All methods such as getInt, getDate, etc, except getString, will always
 * trim the string from getValue before trying to do the data type conversion.
 * getString does not trim the string, but getStringTrim and getStringNotEmpty
 * do.
 *
 * @since   JDK 1.5
 * @version $Id: ValueHelper.java,v 1.3 2009-11-12 18:36:32 smarkoff Exp $
 * @author  Steve Markoff
 */

public abstract class ValueHelper {

    /**
     * This method must be implemented to return a value (possibly null)
     * based upon a name.
     */
    protected abstract String getValue(String name);

    /**
     * This method should be implemented to return the type of value
     * that the subclass is dealing with, ie property, applet parameter,
     * servlet init parameter, servlet request parameter.
     */
    protected abstract String getType();


    /**
     * Gets a value which is expected to be an int.
     * If the value is not found or is not in the proper format an
     * IllegalArgumentException is thrown.
     *
     * @param name  Must be non-blank
     */
    public int getInt(String name)
        throws IllegalArgumentException
    {
        String value = getValue(name);
        if (value == null) {
            throw new IllegalArgumentException(getType() + " " + name + " not found.");
        }
        try {
            return Integer.parseInt(value.trim());  // throws NumberFormatException
        }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException(getType() + " " + name + " not an integer.");
        }
    }

    /**
     * Gets a value which is expected to be an int.
     * The default is returned if the value is not available or
     * is not in the proper format.
     *
     * @param name          Must be non-blank
     * @param defaultValue
     */
    public int getInt(String name, int defaultValue) {
        String value = getValue(name);
        if (value == null)  return defaultValue;
        else {
            try {
                return Integer.parseInt(value.trim());  // throws NumberFormatException
            }
            catch (NumberFormatException e) {
                return defaultValue;
            }
        }
    }

    /**
     * Gets a value which is expected to be a long.
     * If the value is not found or is not in the proper format an
     * IllegalArgumentException is thrown.
     *
     * @param name  Must be non-blank
     */
    public long getLong(String name)
        throws IllegalArgumentException
    {
        String value = getValue(name);
        if (value == null) {
            throw new IllegalArgumentException(getType() + " " + name + " not found.");
        }
        try {
            return Long.parseLong(value.trim());  // throws NumberFormatException
        }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException(getType() + " " + name + " not a long.");
        }
    }

    /**
     * Gets a value which is expected to be a long.
     * The default is returned if the value is not available or
     * is not in the proper format.
     *
     * @param name          Must be non-blank
     * @param defaultValue
     */
    public long getLong(String name, long defaultValue) {
        String value = getValue(name);
        if (value == null)  return defaultValue;
        else {
            try {
                return Long.parseLong(value.trim());  // throws NumberFormatException
            }
            catch (NumberFormatException e) {
                return defaultValue;
            }
        }
    }

    /**
     * Gets a value which is expected to be a double.
     * If the value is not found or is not in the proper format an
     * IllegalArgumentException is thrown.
     *
     * @param name  Must be non-blank
     */
    public double getDouble(String name)
        throws IllegalArgumentException
    {
        String value = getValue(name);
        if (value == null) {
            throw new IllegalArgumentException(getType() + " " + name + " not found.");
        }
        try {
            return Double.parseDouble(value.trim());  // throws NumberFormatException
        }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException(getType() + " " + name + " not a double.");
        }
    }

    /**
     * Gets a value which is expected to be a double.
     * The default is returned if the value is not available or
     * is not in the proper format.
     *
     * @param name          Must be non-blank
     * @param defaultValue
     */
    public double getDouble(String name, double defaultValue) {
        String value = getValue(name);
        if (value == null)  return defaultValue;
        else {
            try {
                return Double.parseDouble(value.trim());  // throws NumberFormatException
            }
            catch (NumberFormatException e) {
                return defaultValue;
            }
        }
    }

    /**
     * Gets a value which is expected to be a boolean.
     * Valid values for true are "true" or "yes" (case insensitive).
     * Valid values for false are "false" or "no" (case insensitive).
     * If the value is not found or is not in the proper format
     * an IllegalArgumentException is thrown.
     *
     * @param name  Must be non-blank
     */
    public boolean getBoolean(String name)
        throws IllegalArgumentException
    {
        String value = getValue(name);
        if (value == null) {
            throw new IllegalArgumentException(getType() + " " + name + " not found.");
        }
        else {
            value = value.trim();
            if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes"))  return true;
            else if (value.equalsIgnoreCase("false") || value.equalsIgnoreCase("no"))  return false;
            else {
                throw new IllegalArgumentException(getType() + " " + name + " not a boolean.");
            }
        }
    }

    /**
     * Gets a value which is expected to be a boolean.
     * Valid values for true are "true" or "yes" (case insensitive).
     * Valid values for false are "false" or "no" (case insensitive).
     * A default value is specified in case the value is not found
     * or is not in the expected format.
     *
     * @param name          Must be non-blank
     * @param defaultValue
     */
    public boolean getBoolean(String name, boolean defaultValue) {
        String value = getValue(name);
        if (value == null)  return defaultValue;
        else {
            value = value.trim();
            if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes"))  return true;
            else if (value.equalsIgnoreCase("false") || value.equalsIgnoreCase("no"))  return false;
            else  return defaultValue;
        }
    }

    /**
     * Gets a value which is expected to be a URL.
     * If the value is not found or is not in the proper format
     * an IllegalArgumentException is thrown.
     *
     * @param name  Must be non-blank
     */
    public URL getURL(String name)
        throws IllegalArgumentException
    {
        String value = getValue(name);
        if (value == null) {
            throw new IllegalArgumentException(getType() + " " + name + " not found.");
        }
        try {
            return new URL(value.trim());
        }
        catch (MalformedURLException e) {
            throw new IllegalArgumentException(getType() + " " + name + " not a proper URL.");
        }
    }

    /**
     * Gets a value which is expected to be a URL.
     * The default is returned if the value is not available or
     * is not in the proper format.
     *
     * @param name          Must be non-blank
     * @param defaultValue  May be null
     */
    public URL getURL(String name, URL defaultValue) {
        String value = getValue(name);
        if (value == null)  return defaultValue;
        else {
            try {
                return new URL(value.trim());
            }
            catch (MalformedURLException e) {
                return defaultValue;
            }
        }
    }

    /**
     * Gets a value which is expected to be a String.
     * If the value is not found an IllegalArgumentException is thrown.
     *
     * @param name  Must be non-blank
     */
    public String getString(String name)
        throws IllegalArgumentException
    {
        String value = getValue(name);
        if (value == null) {
            throw new IllegalArgumentException(getType() + " " + name + " not found.");
        }
        else  return value;
    }

    /**
     * Gets a value which is expected to be a String.
     * A default value is specified in case the value is not found.
     *
     * @param name          Must be non-blank
     * @param defaultValue  May be null
     */
    public String getString(String name, String defaultValue) {
        String value = getValue(name);
        return (value == null) ? defaultValue : value;
    }

    /**
     * Gets a value which is expected to be a String.
     * The value is trimmed before it is returned.
     * If the value is not found an IllegalArgumentException is thrown.
     *
     * @param name  Must be non-blank
     */
    public String getStringTrim(String name)
        throws IllegalArgumentException
    {
        return getString(name).trim();
    }

    /**
     * Gets a value which is expected to be a String.
     * The value is trimmed before it is returned.
     * A default value is specified in case the value is not found.
     * (The defaultValue will be trimmed if it needs it.)
     *
     * @param name          Must be non-blank
     * @param defaultValue  May be null
     */
    public String getStringTrim(String name, String defaultValue) {
        String value = getString(name, defaultValue);
        return (value == null) ? value : value.trim();
    }

    /**
     * Gets a value which is expected to be a String.
     * The value is trimmed before it is returned.
     * An blank value is treated as not found.
     * If the value is not found an IllegalArgumentException is thrown.
     *
     * @param name  Must be non-blank
     */
    public String getStringNotBlank(String name)
        throws IllegalArgumentException
    {
        String value = getValue(name);
        if (value == null) {
            throw new IllegalArgumentException(getType() + " " + name + " not found.");
        }
        else {
            value = value.trim();
            if (value.length() == 0) {
                throw new IllegalArgumentException(getType() + " " + name + " was blank.");
            }
            else return value;
        }
    }

    /**
     * Gets a value which is expected to be a String.
     * The value is trimmed before it is returned.
     * (The defaultValue will be trimmed if it needs it.)
     * An blank value is treated as not found.
     * A default value is specified in case the value is not found.
     *
     * @param name          Must be non-blank
     * @param defaultValue  May be null
     */
    public String getStringNotBlank(String name, String defaultValue) {
        String value = getValue(name);
        if (value == null)  return defaultValue;
        else {
            value = value.trim();
            if (value.length() == 0) {
                return defaultValue == null ? defaultValue : defaultValue.trim();
            }
            else  return value;
        }
    }

    /**
     * Gets a List of values based on a series of names (e.g. foo1, foo2, foo3).
     * The number in the name must start with 1.
     * Values are trimmed. Values are skipped if they are the empty string.
     * A null value is taken to signify the end of the list.
     *
     * @param name  The part of the name to match after the prefix or before
     *              the suffix.
     * @param numberPrefix  Use numbers as a prefix if true, otherwise use as
     *                      a suffix.
     * @return  A non-null ArrayList (but may be of size zero)
     */
    public ArrayList<String> getStringListTrim(String name, boolean numberPrefix) {
        ArrayList<String> list = new ArrayList<String>();
        int count = 1;
        String fullName = numberPrefix ? count + name : name + count;
        String value = getStringTrim(fullName, null);

        while (value != null) {
            if (value.length() > 0)  list.add(value);

            count++;
            fullName = numberPrefix ? count + name : name + count;
            value = getStringTrim(fullName, null);
        }

        return list;
    }

    /**
     * Same as getStringListTrim() except creates a File for each String.
     * Does not validate that any file exists or is a file or directory.
     */
    public ArrayList<File> getFileListTrim(String name, boolean numberPrefix) {
        ArrayList<String> stringList = getStringListTrim(name, numberPrefix);
        ArrayList<File> fileList = new ArrayList<File>(stringList.size());

        for (String s: stringList) {
            fileList.add(new File(s));
        }

        return fileList;
    }

    /**
     * Gets a value which is expected to be a Date.
     * If the value is not found or is not in the proper format an
     * IllegalArgumentException is thrown.
     *
     * The proper format in this case is expected to be the value
     * of Date.getTime() (a long).
     *
     * @param name  Must be non-blank
     */
    public Date getDate(String name)
        throws IllegalArgumentException
    {
        String value = getValue(name);
        if (value == null) {
            throw new IllegalArgumentException(getType() + " " + name + " not found.");
        }
        try {
            long time = Long.parseLong(value.trim());  // throws NumberFormatException
            return new Date(time);
        }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException(getType() + " " + name + " not a Date.");
        }
    }

    /**
     * Gets a value which is expected to be a Date.
     * A default value is specified in case the value is not found
     * or is not in the expected format.
     *
     * The proper format in this case is expected to be the value
     * of Date.getTime() (a long).
     *
     * @param name          Must be non-blank
     * @param defaultValue  May be null
     */
    public Date getDate(String name, Date defaultValue) {
        String value = getValue(name);
        if (value == null)  return defaultValue;
        else {
            try {
                long time = Long.parseLong(value.trim());  // throws NumberFormatException
                return new Date(time);
            }
            catch (NumberFormatException e) {
                return defaultValue;
            }
        }
    }

    /**
     * Gets a value which is expected to be a Date.
     * If the value is not found or is not in the proper format an
     * IllegalArgumentException or ParseException is thrown.
     *
     * The proper format in this case is expected to match the
     * given DateFormat.
     *
     * @param name        Must be non-blank
     * @param dateFormat  Must be non-null
     */
    public Date getDate(String name, DateFormat dateFormat)
        throws IllegalArgumentException, ParseException
    {
        String value = getStringTrim(name);  // throws IllegalArgumentException
        return dateFormat.parse(value);  // throws ParseException
    }

    /**
     * Gets a value which is expected to be a Date.
     * A default value is specified in case the value is not found
     * or is not in the expected format.
     *
     * The proper format in this case is expected to match the
     * give DateFormat.
     *
     * @param name        Must be non-blank
     * @param dateFormat  Must be non-null
     * @param defaultValue  May be null
     */
    public Date getDate(String name, DateFormat dateFormat,
        Date defaultValue)
    {
        String value = getValue(name);
        if (value == null)  return defaultValue;
        else {
            try {
                return dateFormat.parse(value.trim());  // throws ParseException
            }
            catch (ParseException e) {
                return defaultValue;
            }
        }
    }

    /**
     * Gets a value which is expected to be a java.sql.Timestamp.
     * If the value is not found or is not in the proper format an
     * IllegalArgumentException is thrown.
     *
     * The proper format in this case is expected to be the value
     * of Timestamp.getTime() (a long).
     *
     * @param name  Must be non-blank
     */
    public Timestamp getTimestamp(String name)
        throws IllegalArgumentException
    {
        String value = getValue(name);
        if (value == null) {
            throw new IllegalArgumentException(getType() + " " + name + " not found.");
        }
        try {
            long time = Long.parseLong(value.trim());  // throws NumberFormatException
            return new Timestamp(time);
        }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException(getType() + " " + name + " not a Timestamp.");
        }
    }

    /**
     * Gets a value which is expected to be a java.sql.Timestamp.
     * A default value is specified in case the value is not found
     * or is not in the expected format.
     *
     * The proper format in this case is expected to be the value
     * of Timestamp.getTime() (a long).
     *
     * @param name          Must be non-blank
     * @param defaultValue  May be null
     */
    public Timestamp getTimestamp(String name, Timestamp defaultValue) {
        String value = getValue(name);
        if (value == null)  return defaultValue;
        else {
            try {
                long time = Long.parseLong(value.trim());  // throws NumberFormatException
                return new Timestamp(time);
            }
            catch (NumberFormatException e) {
                return defaultValue;
            }
        }
    }

    /**
     * Gets a value which is expected to be a java.sql.Timestamp.
     * If the value is not found or is not in the proper format an
     * IllegalArgumentException or ParseException is thrown.
     *
     * The proper format in this case is expected to match the
     * given DateFormat.
     *
     * @param name        Must be non-blank
     * @param dateFormat  Must be non-null
     */
    public Timestamp getTimestamp(String name, DateFormat dateFormat)
        throws IllegalArgumentException, ParseException
    {
        String value = getStringTrim(name);  // throws IllegalArgumentException
        Date date = dateFormat.parse(value);  // throws ParseException
        return new Timestamp(date.getTime());
    }

    /**
     * Gets a value which is expected to be a java.sql.Timestamp.
     * A default value is specified in case the value is not found
     * or is not in the expected format.
     *
     * The proper format in this case is expected to match the
     * give DateFormat.
     *
     * @param name        Must be non-blank
     * @param dateFormat  Must be non-null
     * @param defaultValue  May be null
     */
    public Timestamp getTimestamp(String name, DateFormat dateFormat,
        Timestamp defaultValue)
    {
        String value = getValue(name);
        if (value == null)  return defaultValue;
        else {
            try {
                Date date = dateFormat.parse(value.trim());  // throws ParseException
                return new Timestamp(date.getTime());
            }
            catch (ParseException e) {
                return defaultValue;
            }
        }
    }
}
