package com.wiley.sf.common.config;

import java.applet.Applet;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import com.wiley.sf.common.lang.ArgUtil;

/**
 * This class provides useful convenience methods to obtain information
 * about Properties (some of these methods are inherited from the base class).
 * There is also a method to convert applet parameters to Properties.
 *
 * @since   JDK 1.6
 * @version 2/3/2013
 * @author  Steve Markoff
 */

public class PropertiesUtil extends ValueHelper {

    // ------------------------- static methods -------------------------------

    /**
     * Get the Applet parameters with the given names and copy them into
     * the given Properties object.
     *
     * @param applet  Must be non-null
     * @param props   Must be non-null
     * @param names   Must be non-null
     */
    public static void addAppletParametersToProperties(Applet applet,
            Properties props, String [] names)
    {
        ArgUtil.notNull(applet, "applet");
        ArgUtil.notNull(props, "props");
        ArgUtil.notNull(names, "names");

        for (int i = 0; i < names.length; i++) {
            String value = applet.getParameter(names[i]);
            if (value != null)  props.put(names[i], value);
        }
    }

    /**
     * Throws an Exception of any of the property names specified don't exist.
     *
     * @param props  Must be non-null
     * @param names  Must be non-null
     */
    public static void checkForExistanceOfProperties(Properties props,
            String [] names)
        throws Exception
    {
        ArgUtil.notNull(props, "props");
        ArgUtil.notNull(names, "names");

        for (int i = 0; i < names.length; i++) {
            if (props.getProperty(names[i]) == null) {
                throw new Exception("Didn't find '" + names[i] + "' in Properties.");
            }
        }
    }

    /**
     * Load Properties from file.
     *
     * @param file  Must be non-null
     */
    public static Properties loadProperties(File file)
        throws IOException
    {
        ArgUtil.notNull(file, "file");

        Properties props = new Properties();
        FileInputStream in = new FileInputStream(file);  // throws IOException
        props.load(in);
        in.close();  // throws IOException
        return props;
    }

    /**
     * Load Properties from file.
     * If you want a PropertiesUtil instance instead of Properties,
     * use one of the PropertiesUtil constructors.
     *
     * @param fileName  Must be non-null and non-blank
     */
    public static Properties loadProperties(String fileName)
        throws IOException
    {
        ArgUtil.notBlank(fileName, "fileName");

        Properties props = new Properties();
        FileInputStream in = new FileInputStream(fileName);  // throws IOException
        props.load(in);
        in.close();  // throws IOException
        return props;
    }

    // --------------------------- instance data ------------------------------

    private final Properties props;

    // -------------------------- instance methods ----------------------------

    /**
     * @param props  Must be non-null
     */
    public PropertiesUtil(Properties props) {
        this.props = props;
        ArgUtil.notNull(props, "props");
    }

    /**
     * Load Properties from file.
     *
     * @param file  Must be non-null
     */
    public PropertiesUtil(File file)
        throws IOException
    {
        ArgUtil.notNull(file, "file");

        props = new Properties();
        FileInputStream in = new FileInputStream(file);  // throws IOException
        props.load(in);
        in.close();  // throws IOException
    }

    /**
     * Load Properties from file.
     *
     * @param fileName  Must be non-null and non-blank
     */
    public PropertiesUtil(String fileName)
        throws IOException
    {
        ArgUtil.notBlank(fileName, "fileName");

        props = new Properties();
        FileInputStream in = new FileInputStream(fileName);  // throws IOException
        props.load(in);
        in.close();  // throws IOException
    }

    /**
     * The same as calling getProperty on the Properties instance passed
     * into the constructor. For convenience.
     */
    public String getProperty(String key) {
        return props.getProperty(key);
    }

    /**
     * Get the underlying properties.
     */
    public Properties getProperties() {
        return props;
    }

    /** Implement abstract method from base class. */
    @Override
    protected String getValue(String name) {
        return props.getProperty(name);
    }

    /** Implement abstract method from base class. */
    @Override
    protected String getType() {
        return "property";
    }

}
