/*
 * &copy; John Wiley &amp; Sons, Inc
 */
package com.wiley.sf.common.io;

import org.junit.*;
import static org.junit.Assert.*;

import com.wiley.sf.common.io.LineHighlight;

/**
 * Test class for LineHighlight.
 * 
 * @since  JDK 1.5, JUnit 4.4
 * @author smarkoff, created 8/20/2007
 * @version $Id: LineHighlightTest.java,v 1.2 2008-02-09 01:56:17 smarkoff Exp $
 */
public class LineHighlightTest {

    @Test
    public void test() {
        String s = "line 1\n"
            + "line 2\n"
            + "line 3\n"
            + "line 4\n"
            + "line 5\n"
            + "line 6\n"
            + "line 7\n"
            + "line 8\n"
            + "line 9\n"
            + "line 10\n"
            + "line 11\n"
            + "line 12\n"
            + "line 13\n"
            + "line 14\n"
            + "line 15\n"
            + "line 16\n"
            + "line 17\n"
            + "line 18\n"
            + "line 19\n"
            + "line 20\n";
        
        LineHighlight lh = new LineHighlight(false, 1);
        String h = lh.highlight(s, 16, 5);
        
        String expected = "15: line 15\n"
            + "16: line 16\n"
            + "17: line 17\n";
        assertTrue(h.equals(expected));
        
        lh = new LineHighlight(true, 0);
        h = lh.highlight(s, 16, 5);
        expected = "16: line 17\n";
        assertTrue(h.equals(expected));
    }

}
