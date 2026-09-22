/*
 * &copy; John Wiley &amp; Sons, Inc
 */
package com.wiley.sf.common.io;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/**
 * JUnit test class for CSVTabParser.
 *
 * @since JDK 1.6, JUnit 4.7
 * @author smarkoff
 * @version $Id: CSVTabParserTest.java,v 1.3 2011-06-23 18:27:10 smarkoff Exp $
 */
public class CSVTabParserTest {

    @Test
    public void parseLine() {
        String line = "1,2";
        String [] expected = { "1", "2" };
        parseLine(line, expected);

        line = "\"Smith, John\", hello ";
        expected = new String [] { "Smith, John", " hello " };
        parseLine(line, expected);
    }

    private void parseLine(String line, String [] expected) {
        CSVTabParser parser = CSVTabParser.getCSVInstance();
        List<String> result = parser.parseLine(line);
        String msg = "result size was " + result.size() + " but expected size was " + expected.length;
        assertTrue(msg, result.size() == expected.length);
        if (result.size() == expected.length) {
            for (int i = 0; i < result.size(); i++) {
                String r = result.get(i);
                String e = expected[i];
                msg = "result element " + i + " was [" + r + "] but was expected to be [" + e + "]";
                assertTrue(msg, r.equals(e));
            }
        }
    }

    @Test
    public void generateLine() {
        List<String> list = new ArrayList<String>();
        list.add("Smith, John");
        list.add(" hello ");
        list.add("foo");
        generateLine(list, "\"Smith, John\",\" hello \",\"foo\"\n");
    }

    private void generateLine(List<?> list, String expected) {
        CSVTabParser parser = CSVTabParser.getCSVInstance();
        String result = parser.generateLine(list);
        String msg = "result was [" + result + "] but expected [" + expected + "]";
        assertTrue(msg, result.equals(expected));
    }
}
