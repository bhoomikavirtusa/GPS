package com.wiley.sf.common.lang;

import static org.junit.Assert.assertTrue;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

/**
 * JUnit test class for JsonUtil.
 *
 * @since   JDK 1.6, JUnit 4.10
 * @version $Id: JsonUtilTest.java,v 1.3 2013-02-23 00:02:05 smarkoff Exp $
 * @author  Steve Markoff
 */
public class JsonUtilTest {

    @Test
    public void escape() {
        escape("The \"quick\" fox.", "The \\\"quick\\\" fox.");
        escape("The \tquick fox.", "The \\tquick fox.");
        escape("\\", "\\\\");
        escape("\"a\"\r\n\\/", "\\\"a\\\"\\r\\n\\\\\\/");
    }

    private void escape(String input, String expected) {
        String output = JsonUtil.escape(input);
        String msg = "Input was (" + input + ")"
            + "  Expected output was (" + expected + ") but got (" + output + ")";
        assertTrue(msg, StringUtils.equals(output, expected));
    }

    @Test
    public void pair() {
        pair("name", "value", "\"name\" : \"value\"");
        pair("name", 5, "\"name\" : 5");
    }

    private void pair(String name, String value, String expected) {
        String output = JsonUtil.pair(name, value);
        String msg = "Input was (name [" + name + "], value [" + value + "])"
            + "  Expected output was (" + expected + ") but got (" + output + ")";
        assertTrue(msg, StringUtils.equals(output, expected));
    }

    private void pair(String name, int value, String expected) {
        String output = JsonUtil.pair(name, value);
        String msg = "Input was (name [" + name + "], value [" + value + "])"
            + "  Expected output was (" + expected + ") but got (" + output + ")";
        assertTrue(msg, StringUtils.equals(output, expected));
    }
}
