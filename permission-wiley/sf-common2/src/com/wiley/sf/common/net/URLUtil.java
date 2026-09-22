/*
 * &copy; John Wiley &amp; Sons, Inc
 */
package com.wiley.sf.common.net;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.io.IOUtils;

import com.wiley.sf.common.lang.ArgUtil;

/**
 *
 * @since   JDK 1.6
 * @version $Id: URLUtil.java,v 1.9 2010-06-07 03:25:35 smarkoff Exp $
 * @author  Steve Markoff
 */
public class URLUtil {

    /**
     * Assumes the content of the URL is a UTF-8 encoded String.
     *
     * @param url  Must be non-null
     */
    public static String urlToString(URL url)
        throws IOException
    {
        ArgUtil.notNull(url, "url");

        URLConnection con = url.openConnection();
        InputStream in = con.getInputStream();  // throws IOException
        int contentLength = con.getContentLength();
        String ret;

        if (contentLength == -1) {  // means contentLength is not known
            ret = IOUtils.toString(in, "UTF-8");  // throws IOException
        }
        else {
            // I think it's important to read as bytes and then convert to a String
            // because the contentLength should refer to the number of bytes,
            // which might not be the same as String length.

            byte [] buffer = streamToByteArray(in, contentLength);  // throws IOException
            ret = new String(buffer, "UTF-8");
        }

        IOUtils.closeQuietly(in);
        return ret;
    }

    /**
     * @param url  Must be non-null
     */
    public static byte [] urlToByteArray(URL url)
        throws IOException
    {
        ArgUtil.notNull(url, "url");

        URLConnection con = url.openConnection();
        InputStream in = con.getInputStream();  // throws IOException
        int contentLength = con.getContentLength();
        byte [] ret;

        if (contentLength == -1) {  // means contentLength is not known
            ret = IOUtils.toByteArray(in);  // throws IOException
        }
        else {
            ret = streamToByteArray(in, contentLength);  // throws IOException
        }

        IOUtils.closeQuietly(in);
        return ret;
    }

    private static byte [] streamToByteArray(InputStream in, int contentLength) throws IOException {
        byte [] buffer = new byte[contentLength];
        int amountRead = in.read(buffer, 0, contentLength);
        int length = amountRead;

        while (amountRead != -1 && length < contentLength) {
            amountRead = in.read(buffer, length, contentLength - length);
            if (amountRead != -1)  length += amountRead;
        }

        if (length == -1) {
            // We hit the end of the stream right away - not expected
            return new byte[0];
        }
        if (length == contentLength) {
            // this is what we normally expect
            return buffer;
        }
        else {
            // we hit the end of the stream before contentLength was reached (not expected)
            byte [] newBuffer = new byte[length];
            System.arraycopy(buffer, 0, newBuffer, 0, length);
            return newBuffer;
        }
    }


    /**
     * There is no reason to ever create an instance of this class
     * since all methods are static.
     */
    private URLUtil() { }
}
