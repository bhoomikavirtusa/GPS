package com.wiley.sf.common.sql;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

import com.wiley.sf.common.config.PropertiesUtil;
import com.wiley.sf.common.lang.ArgUtil;

/**
 * This class provides a simple API to create connections to a database.
 * Don't use this class when a connection pool would be more appropriate.
 *
 * @since   JDK 1.6
 * @version 4/23/2012
 * @author  Steve Markoff
 */
public class SimpleDBConnect {

    /**
     * Uses the following properties: driverClass (required), connectString (required),
     * user (optional), password (optional), driverJarFile (optional), transactionIsolation (optional).
     * And you may have extra properties with keys extra1.name, extra1.value, etc.
     *
     * @param propertiesFileName  Must be non-blank
     */
    public static SimpleDBConnect create(String propertiesFileName)
    throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException
    {
        ArgUtil.notBlank(propertiesFileName, "propertiesFileName");
        Properties props = PropertiesUtil.loadProperties(propertiesFileName);  // throws IOException
        return create(props);
    }

    /**
     * Uses the following properties: driverClass (required), connectString (required),
     * user (optional), password (optional), driverJarFile (optional), transactionIsolation (optional).
     * And you may have extra properties with keys extra1.name, extra1.value, etc.
     *
     * @param props  Must be non-null
     */
    public static SimpleDBConnect create(Properties props)
    throws FileNotFoundException, MalformedURLException, InstantiationException,
        IllegalAccessException, ClassNotFoundException
    {
        ArgUtil.notNull(props, "props");
        PropertiesUtil pu = new PropertiesUtil(props);
        String driverClass = pu.getString("driverClass");
        String connectString = pu.getString("connectString");
        String user = pu.getString("user", null);
        String password = pu.getString("password", null);
        String driverJarFile = pu.getString("driverJarFile", null);
        String transIsoString = pu.getString("transactionIsolation", null);

        int transactionIsolation = -1;
        if (StringUtils.isNotBlank(transIsoString) && !transIsoString.equals("default")) {
            transactionIsolation = NameForConstants.lookupTransactionIsolationByName(transIsoString);
        }

        Properties extraProperties = new Properties();
        int extraCount = 1;
        String extraName = pu.getString("extra1.name", null);

        while (StringUtils.isNotBlank(extraName)) {
            String extraValue = pu.getString("extra" + extraCount + ".value");
            extraProperties.put(extraName, extraValue);

            extraCount++;
            extraName = pu.getString("extra" + extraCount + ".name", null);
        }

        return new SimpleDBConnect(driverClass, connectString, user, password,
            extraProperties, driverJarFile, transactionIsolation);
            // throws various exceptions
    }


    private final Driver driver;
    private final String connectString;
    private final Properties properties = new Properties();
    private final int transactionIsolation;

    /**
     * @param driverClass    Must be non-blank
     * @param connectString  Must be non-blank
     */
    public SimpleDBConnect(String driverClass, String connectString)
        throws InstantiationException, IllegalAccessException, ClassNotFoundException,
            FileNotFoundException, MalformedURLException
    {
        this(driverClass, connectString, null, null);
    }

    /**
     * @param driverClass    Must be non-blank
     * @param connectString  Must be non-blank
     * @param user           May be null (blank treated as null)
     * @param password       May be null (blank treated as null)
     */
    public SimpleDBConnect(String driverClass, String connectString,
            String user, String password)
        throws InstantiationException, IllegalAccessException, ClassNotFoundException,
            FileNotFoundException, MalformedURLException
    {
        this(driverClass, connectString, user, password, null, null, -1);
    }

    /**
     * If driverJarFile is null then the driverClass should already be in the
     * classpath. Otherwise the driverClass should be in the specified jar file.
     *
     * @param driverClass    Must be non-blank
     * @param connectString  Must be non-blank
     * @param user           May be null (blank treated as null)
     * @param password       May be null (blank treated as null)
     * @param extraProperties  May be null
     * @param driverJarFile  May be null (blank treated as null)
     * @param transactionIsolation  -1 means keep the database default
     */
    public SimpleDBConnect(String driverClass, String connectString,
            String user, String password, Properties extraProperties,
            String driverJarFile, int transactionIsolation)
        throws ClassNotFoundException, InstantiationException, IllegalAccessException,
            FileNotFoundException, MalformedURLException
    {
        ArgUtil.notBlank(driverClass, "driverClass");
        ArgUtil.notBlank(connectString, "connectString");

        Class<?> clazz;
        if (StringUtils.isBlank(driverJarFile)) {
            clazz = Class.forName(driverClass);
                // throws ClassNotFoundException
        }
        else {
            File file = new File(driverJarFile);
            if (!file.exists()) {
                throw new FileNotFoundException("File not found: "
                        + file.getAbsolutePath());
            }
            String path = file.getAbsolutePath();
            // (See notes on file URLs.)
            if (path.startsWith("/")) { // Unix
                path = "file://" + path;
            }
            else { // Windows
                path = "file:/" + path;
            }
            URL url = new URL(path);
                // throws MalformedURLException
            URLClassLoader loader = new URLClassLoader(new URL[] { url });

            clazz = Class.forName(driverClass, true, loader);
                // throws ClassNotFoundException
        }

        driver = (Driver) clazz.newInstance();
        // throws InstantiationException, IllegalAccessException

        this.connectString = connectString;

        if (StringUtils.isNotBlank(user)) properties.put("user", user);
        if (StringUtils.isNotBlank(password)) properties.put("password", password);

        if (extraProperties != null) {
            properties.putAll(extraProperties);
        }

        this.transactionIsolation = transactionIsolation;
    }

    public Connection getConnection() throws SQLException {
        // Note some drivers don't like 'null' for properties so always
        // at least pass empty Properties object.
        Connection con = driver.connect(connectString, properties);

        if (con == null) {
            // JDK says driver will return null if based on the connectString
            // it thinks it's the wrong driver (connect string is for some other
            // driver).
            throw new SQLException("Driver didn't like connectString: "
                    + connectString);
        }
        else {
            if (transactionIsolation != -1)  con.setTransactionIsolation(transactionIsolation);
            return con;
        }
    }

    public DriverPropertyInfo [] getDriverPropertyInfo() throws SQLException {
        return driver.getPropertyInfo(connectString, properties);
    }
}

