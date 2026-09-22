package com.wiley.sf.common.codec;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * JUnit test class for Hex.
 *
 * @since   JDK 1.6, JUnit 4.7
 * @version $Id: HexTest.java,v 1.3 2010-12-04 02:47:14 smarkoff Exp $
 * @author  Steve Markoff
 */
public class HexTest {

    @Test
    public void stringToHex() {
        String output = Hex.stringToHex("ab\r\n\u00b2\u25a0", true);
        String expected = "0061 0062 000d 000a\n00b2 25a0";
        String msg = "Expected: [" + expected + "] but got [" + output + "]";
        assertTrue(msg, output.equals(expected));
    }

    @Test
    public void bytesToHex() {
        // Need to cast bytes > 127 to compile
        byte [] input = { 0x01, 0x10, (byte) 0x0a, (byte) 0xa1, (byte) 0xff };
        String output = Hex.bytesToHex(input);
        assertTrue(output.equals("01100aa1ff"));
    }

    @Test
    public void hexToBytes() {
        String input = "01100aa1ff";
        byte [] output = Hex.hexToBytes(input);
        // Need to cast bytes > 127 to compile
        byte [] expected = { 0x01, 0x10, (byte) 0x0a, (byte) 0xa1, (byte) 0xff };
        assertTrue(output.length == expected.length);
        for (int i = 0; i < output.length; i++) {
            assertTrue(output[i] == expected[i]);
        }
    }

}
