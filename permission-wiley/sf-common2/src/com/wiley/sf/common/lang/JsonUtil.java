package com.wiley.sf.common.lang;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 *
 * @since    JDK 1.6
 * @version  $Id: JsonUtil.java,v 1.5 2014-06-10 22:25:40 smarkoff Exp $
 * @author   Steve Markoff
 */
public class JsonUtil {
    private static final Log log = LogFactory.getLog(JsonUtil.class);

    public static void debug(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c < 32 && c != 10 && c != 13) || c > 127) {
                log.debug("found char, index = " + i + ", dec value = " + ((int) c));
            }
        }
    }

    /**
     * This method has similar behavior to StringEscapeUtils.escapeJson()
     * from Apache Commons-Lang but at least with my test data is 10x faster.
     */
    public static String escape(String s) {
        if (s == null)  return null;

        // JSON spec: http://json.org/
        // I don't understand why it would be necessary to escape anything
        // except double quotes and backward slashes, but I will follow the spec.

        // characters to escape:
        // backslash (dec 92)
        // double-quote (dec 34)
        // forward slash (dec 47)
        // backspace (dec 8)
        // form feed (dec 12)
        // newline (dec 10)
        // carriage return (dec 13)
        // horizontal tab (dec 9)
        // *vertical tab (dec 11)

        // * vertical tab not mentioned in JSON spec but at least with
        // DataTables 1.7.5 this needed to be escaped.
        // - Perhaps the spec really intends that all control characters are
        // escaped but just mentions the most common ones (really anything
        // else besides these should not appear in a string).

        // If this was JavaScript (which uses the syntax backslash + uXXXX for unicode chars)
        // then we'd also replace backslash-u with double-backslash-u but since this is Java
        // we don't need to do this.

        char [] check = { '\\', '"', '/', '\b', '\f', '\n', '\r', '\t', '\u000b' };
        String [] replacement = { "\\\\", "\\\"", "\\/", "\\b", "\\f", "\\n", "\\r", "\\t", "\\u000b"};

        StringBuilder sb = new StringBuilder(s.length() + 10);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean replaced = false;
            for (int j = 0; j < check.length; j++) {
                if (c == check[j]) {
                    sb.append(replacement[j]);
                    replaced = true;
                    break;
                }
            }
            if (!replaced) {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    public static String pair(String name, String value) {
        return "\"" + name + "\" : \"" + escape(value) + "\"";
    }

    public static String pair(String name, int value) {
        return "\"" + name + "\" : " + value;
    }

    public static String pair(String name, long value) {
        return "\"" + name + "\" : " + value;
    }

    public static String pair(String name, float value) {
        return "\"" + name + "\" : " + value;
    }

    public static String pair(String name, double value) {
        return "\"" + name + "\" : " + value;
    }

    public static String pair(String name, boolean value) {
        return "\"" + name + "\" : " + value;
    }

    /**
     * There is no reason to ever create an instance of this class
     * since all methods are static.
     */
    private JsonUtil() { }
}
