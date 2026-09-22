/*
 * &copy; John Wiley &amp; Sons, Inc
 */
package com.wiley.sf.common.io;

import static org.junit.Assert.assertTrue;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

/**
 * JUnit test class for FixWindows1252Chars.
 *
 * @since JDK 1.6, JUnit 4.10
 * @author smarkoff, created 5/21/2007
 * @version $Id: FixWindows1252CharsTest.java,v 1.2 2013-02-23 00:03:46 smarkoff Exp $
 */
public class FixWindows1252CharsTest {

    @Test
    public void fix() {
        fix(null, null);
        fix("", "");
        fix("a", "a");
        fix("abc", "abc");
        fix("\u20ac", "\u20ac");
        fix("\u0080", "\u20ac");
        fix("\u0081", "\u0081");  // won't fix because there is no fix
        fix("\u2019", "\u2019");
        fix("\u0092", "\u2019");
        fix("a\u0080\u2019_", "a\u20ac\u2019_");
    }

    private void fix(String input, String expected) {
        String result = FixWindows1252Chars.fix(input);
        String msg = "expected [" + expected + "] but result was [" + result + "].";
        assertTrue(msg, StringUtils.equals(result, expected));
    }
}
