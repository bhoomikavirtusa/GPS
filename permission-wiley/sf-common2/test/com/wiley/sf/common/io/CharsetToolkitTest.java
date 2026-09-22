/*
 * &copy; John Wiley &amp; Sons, Inc
 */
package com.wiley.sf.common.io;

import java.io.*;

import org.junit.*;
import static org.junit.Assert.*;

import com.wiley.sf.common.io.CharsetToolkit;
import com.wiley.sf.common.io.CharsetType;

/**
 * JUnit test class for CharsetToolkit.
 * 
 * @since JDK 1.5, JUnit 4.4
 * @author smarkoff, created 5/21/2007
 * @version $Id: CharsetToolkitTest.java,v 1.2 2008-02-09 01:09:32 smarkoff Exp $
 */
public class CharsetToolkitTest {

    @Test
    public void testUTF8() throws IOException {
        // This is the UTF-8 encoded string "ab" + registered trademark symbol
        //   + "=" + curly right single quote.
        // registered trademark symbol = C2 AE
        // curly right single quote = E2 80 99
        byte [] byteArray = { 0x61, 0x62, (byte)0xC2, (byte)0xAE, 0x3D,
            (byte)0xE2, (byte)0x80, (byte)0x99 };
        // decimal values: 97, 98, 194, 174, 61, 226, 128, 153
        test(byteArray, CharsetType.UTF8);
    }
    
    @Test
    public void testLatin1() throws IOException {
        // This is the Latin encoded string "ab" + registered trademark symbol
        //   + "=" + paragraph symbol.
        // registered trademark symbol = AE
        // paragraph symbol = B6
        byte [] byteArray = { 0x61, 0x62, (byte)0xAE, 0x3D, (byte)0xB6 };
        // decimal values: 97, 98, 174, 61, 182
        test(byteArray, CharsetType.LATIN1);
        
        // case where first char could be start of UTF-8 but not
        byteArray = new byte [] { (byte)0xC2, 0x61 };
        test(byteArray, CharsetType.LATIN1);
    }
    
    @Test
    public void testWindows1252() throws IOException {
        // This is the Windows1252 encoded string "ab" + registered trademark symbol
        //   + "=" + paragraph symbol + trademark symbol.
        // registered trademark symbol = AE
        // paragraph symbol = B6
        // trademark symbol = 99
        byte [] byteArray = { 0x61, 0x62, (byte)0xAE, 0x3D, (byte)0xB6, (byte)0x99 };
        // decimal values: 97, 98, 174, 61, 182, 153
        test(byteArray, CharsetType.WINDOWS1252);
    }
    
    @Test
    public void testUnknown() throws IOException {
        // Has a 2-byte UTF-8 char and then a Latin-1 char
        byte [] byteArray = { (byte)0xC2, (byte)0xAE, (byte)0xC2 };
        test(byteArray, CharsetType.UNKNOWN);
        
        // Note really this byte array should probably be considered Latin-1
        // but CharsetDet doesn't go back and change it's decision on
        // characters already checked.
        // The Unix "file" utility considers this byte array UTF-8 probably
        // because it starts out that way - the correct assumption if you
        // assume the file is consistently one character set.
    }

    private static void test(byte [] byteArray, CharsetType expected) throws IOException {
        ByteArrayInputStream inStream = new ByteArrayInputStream(byteArray);
        CharsetType type = CharsetToolkit.determineCharset(inStream).getCharsetType();
            // throws IOException
        String msg = "Expected charset of " + expected.getName() + " but "
            + type.getName() + " reported instead.";
        assertTrue(msg, type == expected);
    }

}
