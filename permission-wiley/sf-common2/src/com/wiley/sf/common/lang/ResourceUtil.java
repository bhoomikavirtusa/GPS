/*
 * &copy; John Wiley &amp; Sons, Inc
 */
package com.wiley.sf.common.lang;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;

import com.wiley.sf.common.net.URLUtil;

/**
 * Contains methods to load different types of resources.
 * Each method takes a Class and a name. Here are some examples of usage:
 *   Get a resource called "foo.txt" that is in the same directory
 *   as your class "MyClass":
 *   - getStringResource(MyClass.class, "foo.txt");
 *   Get something relative to a directory a class is in:
 *   - getStringResource(MyClass.class, "subdir/foo.txt");
 *   Get something relative to the root package:
 *   - getStringResource(MyClass.class, "/com/mycompany/foo.txt");
 *   (in this case the directory of the class you pass in does not matter)
 *
 * @since   JDK 1.6
 * @version $Id: ResourceUtil.java,v 1.14 2010-06-07 01:07:09 smarkoff Exp $
 * @author  Steve Markoff
 */
public class ResourceUtil {

    /**
     * Determines whether a resource exists.
     *
     * @param cl    Must be non-null
     * @param name  Must be non-null and non-blank
     */
    public static boolean doesResourceExist(Class<?> cl, String name) {
        ArgUtil.notNull(cl, "cl");
        ArgUtil.notBlank(name, "name");

        URL url = cl.getResource(name);
        return url != null;
    }

    /**
     * Copies the specified resource to the given file.
     *
     * @param cl    Must be non-null
     * @param name  Must be non-blank
     * @param file  Must be non-null
     *
     * @throws  FileNotFoundException  If the resource is not found
     * @throws  IOException            If there is a problem reading the resource or writing to the file
     */
    public static boolean copyResourceToFile(Class<?> cl, String name, File file)
        throws FileNotFoundException, IOException
    {
        ArgUtil.notNull(file, "file");
        InputStream in = getResourceInputStream(cl, name);
            // throws FileNotFoundException
        FileOutputStream out = new FileOutputStream(file);
            // throws IOException
        try {
            IOUtils.copy(in, out);  // throws IOException
        }
        finally {
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(out);
        }
        return true;
    }

    /**
     * Returns the resource as a byte array.
     *
     * @param cl    Must be non-null
     * @param name  Must be non-blank
     *
     * @throws  FileNotFoundException  If the resource is not found
     * @throws  IOException            If there is a problem reading the resource
     */
    public static byte [] getByteArrayResource(Class<?> cl, String name)
        throws FileNotFoundException, IOException
    {
        URL url = getResourceURL(cl, name);  // throws FileNotFoundException
        return URLUtil.urlToByteArray(url);  // throws IOException
    }

    /**
     * Returns the resource as an image.
     *
     * @param cl    Must be non-null
     * @param name  Must be non-blank
     *
     * @throws  FileNotFoundException  If the resource is not found
     * @throws  IOException            If there is a problem reading the resource
     */
    public static BufferedImage getImageResource(Class<?> cl, String name)
        throws FileNotFoundException, IOException
    {
        URL url = getResourceURL(cl, name);  // throws FileNotFoundException
        return ImageIO.read(url);  // throws IOException
    }

    /**
     * Returns the String resource.
     *
     * @param cl    Must be non-null
     * @param name  Must be non-blank
     *
     * @throws  FileNotFoundException  If the resource is not found
     * @throws  IOException            If there is a problem reading the resource
     */
    public static String getStringResource(Class<?> cl, String name)
        throws FileNotFoundException, IOException
    {
        URL url = getResourceURL(cl, name);  // throws FileNotFoundException
        return URLUtil.urlToString(url);  // throws IOException
    }

    /**
     * Returns a Properties object representing the specified resource.
     *
     * @param cl    Must be non-null
     * @param name  Must be non-blank
     *
     * @throws  FileNotFoundException  If the resource is not found
     * @throws  IOException            If there is a problem reading the resource
     */
    public static Properties getPropertiesResource(Class<?> cl, String name)
        throws FileNotFoundException, IOException
    {
        InputStream in = getResourceInputStream(cl, name);
            // throws FileNotFoundException
        Properties props = new Properties();
        props.load(in);  // throws IOException
        return props;
    }

    /**
     * Returns a URL for the resource. If the resource is not found
     * throws FileNotFoundException (will never return null).
     *
     * @param cl    Must be non-null
     * @param name  Must be non-blank
     *
     * @throws  FileNotFoundException  If the resource is not found
     */
    public static URL getResourceURL(Class<?> cl, String name)
        throws FileNotFoundException
    {
        ArgUtil.notNull(cl, "cl");
        ArgUtil.notBlank(name, "name");

        URL url = cl.getResource(name);
        if (url == null) {
            throw new FileNotFoundException("Resource not found: Class = "
                + cl.getName() + " name = " + name);
        }
        else return url;
    }

    /**
     * Returns an InputStream for the resource. If the resource is not found
     * throws FileNotFoundException (will never return null).
     *
     * @param cl    Must be non-null
     * @param name  Must be non-blank
     *
     * @throws  FileNotFoundException  If the resource is not found
     */
    public static InputStream getResourceInputStream(Class<?> cl, String name)
        throws FileNotFoundException, IOException
    {
        ArgUtil.notNull(cl, "cl");
        ArgUtil.notBlank(name, "name");

        InputStream in = cl.getResourceAsStream(name);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: Class = "
                + cl.getName() + " name = " + name);
        }
        else  return in;
    }

    /**
     * There is no reason to ever create an instance of this class
     * since all methods are static.
     */
    private ResourceUtil() { }
}
