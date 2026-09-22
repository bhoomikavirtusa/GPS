package com.wiley.sf.common.servlet;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.wiley.sf.common.lang.ArgUtil;

/**
 *
 * @since   JDK 1.6, Servlet API 2.5
 * @version $Id: ServletUtil.java,v 1.4 2013-01-08 20:44:31 smarkoff Exp $
 * @author  Steve Markoff
 */
public class ServletUtil {

    /**
     * This method is for debugging purposes. Returns a String with all the
     * HTTP header name/value pairs (so you can print out).
     * Returns the empty string if there are no headers.
     *
     * @param request  Must be non-null
     */
    public static String getAllHeaders(HttpServletRequest request) {
        return getAllHeaders(request, 0);
    }

    /**
     * This method is for debugging purposes. Returns a String with all the
     * HTTP header name/value pairs (so you can print out).
     * Returns the empty string if there are no headers.
     *
     * @param request  Must be non-null
     * @param limit    Specify an integer greater than 0 to limit the header
     *                 values to a certain number of characters - when a value
     *                 exceeds the limit it will be shown as
     *                 "start... [truncated from length of <original length>]".
     */
    public static String getAllHeaders(HttpServletRequest request, int limit) {
        ArgUtil.notNull(request, "request");
        StringBuilder sb = new StringBuilder();
        Enumeration<String> e = request.getHeaderNames();

        while (e.hasMoreElements()) {
            String name = e.nextElement();
            Enumeration<String> e2 = request.getHeaders(name);

            while (e2.hasMoreElements()) {
                String value = e2.nextElement();
                sb.append(name);
                sb.append(": ");
                sb.append(limitValue(value, limit));
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * This method is for debugging purposes. Returns a String with all the
     * Servlet parameter name/value pairs (so you can print out).
     * Will print out multiple values for parameters that have them.
     * Returns the empty string if there are no parameters.
     *
     * @param request  Must be non-null
     */
    public static String getAllParameters(ServletRequest request) {
        return getAllParameters(request, 0);
    }

    /**
     * This method is for debugging purposes. Returns a String with all the
     * Servlet parameter name/value pairs (so you can print out).
     * Will print out multiple values for parameters that have them.
     * Returns the empty string if there are no parameters.
     *
     * @param request  Must be non-null
     * @param limit    Specify an integer greater than 0 to limit the parameter
     *                 values to a certain number of characters - when a value
     *                 exceeds the limit it will be shown as
     *                 "start... [truncated from length of <original length>]".
     */
    public static String getAllParameters(ServletRequest request, int limit) {
        ArgUtil.notNull(request, "request");
        StringBuilder sb = new StringBuilder();
        Enumeration<String> e = request.getParameterNames();

        while (e.hasMoreElements()) {
            String name = e.nextElement();
            sb.append(name);
            sb.append(" = ");
            String [] values = request.getParameterValues(name);

            for (int i = 0; i < values.length; i++) {
                sb.append(limitValue(values[i], limit));
                if (i + 1 < values.length)  sb.append(", ");
            }

            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * javax.servlet.ServletRequest has a method called getParameterMap,
     * where the map values are String arrays. This method is similar, but returns
     * a map where the values are just Strings. You should only use this method
     * if you are sure that all the parameters in the request only have a single
     * value.
     *
     * @param request  Must be non-null
     */
    public static HashMap<String, String> getSingleValueParameterMap(ServletRequest request) {
        ArgUtil.notNull(request, "request");
        HashMap<String, String> map = new HashMap<String, String>();
        Enumeration<String> e = request.getParameterNames();

        while (e.hasMoreElements()) {
            String name = e.nextElement();
            String value = request.getParameter(name);
            map.put(name, value);
        }

        return map;
    }


    // Note: It would be nice if ServletRequest, ServletContext, and HttpSession
    // all implemented some common getAttribute interface, so we didn't have to
    // have practically the same methods repeated three times.

    /**
     * Returns a Map where the keys are request attribute names and the values are
     * the attribute values.
     *
     * @param request  Must be non-null
     */
    public static Map<String, Object> getAttributeMap(ServletRequest request) {
        ArgUtil.notNull(request, "request");
        Map<String, Object> map = new HashMap<String, Object>();
        Enumeration<String> e = request.getAttributeNames();

        while (e.hasMoreElements()) {
            String name = e.nextElement();
            String value = request.getAttribute(name).toString();
            map.put(name, value);
        }

        return map;
    }

    /**
     * Returns a Map where the keys are context attribute names and the values are
     * the attribute values.
     *
     * @param context  Must be non-null
     */
    public static Map<String, Object> getAttributeMap(ServletContext context) {
        ArgUtil.notNull(context, "context");
        Map<String, Object> map = new HashMap<String, Object>();
        Enumeration<String> e = context.getAttributeNames();

        while (e.hasMoreElements()) {
            String name = e.nextElement();
            String value = context.getAttribute(name).toString();
            map.put(name, value);
        }

        return map;
    }

    /**
     * Returns a Map where the keys are session attribute names and the values are
     * the attribute values.
     *
     * @param session  Must be non-null
     */
    public static Map<String, Object> getAttributeMap(HttpSession session) {
        ArgUtil.notNull(session, "session");
        Map<String, Object> map = new HashMap<String, Object>();
        Enumeration<String> e = session.getAttributeNames();

        while (e.hasMoreElements()) {
            String name = e.nextElement();
            String value = session.getAttribute(name).toString();
            map.put(name, value);
        }

        return map;
    }


    /**
     * @param value  Must be non-null
     * @param limit  Can be anything - less than 1 indicates no limit
     */
    private static String limitValue(String value, int limit) {
        if (limit > 0 && value.length() > limit) {
            value = value.substring(0, limit) + " [truncated from length of "
                + value.length() + "]";
        }
        return value;
    }

    /**
     * There is no reason to create an instance of this class since all methods
     * are static.
     */
    private ServletUtil() { }
}
