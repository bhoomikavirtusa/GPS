package com.wiley.sf.common.codec;

import com.wiley.sf.common.lang.ArgUtil;

/**
 * Provides methods to convert byte arrays into hexadecimal representation
 * (as a String) and vice versa.
 *
 * Also see java.lang.Integer methods:
 * toHexString(int i)
 * toString(int i, int radix)
 * valueOf(String s, int radix)
 *
 * @since   JDK 1.6
 * @version $Id: Hex.java,v 1.3 2010-12-04 02:46:33 smarkoff Exp $
 * @author  Steve Markoff
 */
public class Hex {

    /**
     * For example converts ("ab\r\n", true) to "0061 0062 000d 000a\n".
     *
     * @param s  If null then null returned
     */
    public static String stringToHex(String s, boolean addSpacesAndNewlines) {
        if (s == null)  return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            String hex = Integer.toHexString(c);
            for (int j = hex.length(); j < 4; j++) {
                sb.append('0');
            }
            sb.append(hex);
            if (addSpacesAndNewlines && i < s.length() - 1) {
                sb.append(c == '\n' ? '\n' : ' ');
            }
        }

        return sb.toString();
    }

    /**
     * Convert a byte array into hexadecimal representation.
     *
     * @param b  Must be non-null
     */
    public static String bytesToHex(byte [] b) {
        ArgUtil.notNull(b, "b");
        StringBuilder sb = new StringBuilder(b.length * 2);

        for (int i = 0; i < b.length; i++) {
            // look up high nibble char
            sb.append(hexChar[(b[i] & 0xf0) >>> 4]);

            // look up low nibble char
            sb.append(hexChar[b[i] & 0x0f]);
        }

        return sb.toString();
    }

    /**
     * Convert a hex string to a byte array.
     * Permits upper or lower case hex.
     *
     * @param s String must have even number of characters and be formed only
     *          of digits 0-9 A-F or a-f. No spaces, minus or plus signs.
     * @return  corresponding byte array.
     */
    public static byte [] hexToBytes(String s) {
        ArgUtil.notNull(s, "s");
        int stringLength = s.length();
        if ((stringLength & 0x1) != 0) {
            throw new IllegalArgumentException(
                "fromHexString requires an even number of hex characters");
        }
        byte [] b = new byte[stringLength / 2];

        for (int i = 0, j = 0; i < stringLength; i += 2, j++) {
            int high = charToNibble(s.charAt(i));
            int low = charToNibble(s.charAt(i + 1));
            b[j] = (byte) ((high << 4) | low);
        }

        return b;
    }

    // table to convert a nibble to a hex char.
    private final static char [] hexChar = {
        '0' , '1' , '2' , '3' ,
        '4' , '5' , '6' , '7' ,
        '8' , '9' , 'a' , 'b' ,
        'c' , 'd' , 'e' , 'f'
    };

    /**
     * convert a single char to corresponding nibble.
     *
     * @param c char to convert. must be 0-9 a-f A-F, no spaces,
     *          plus or minus signs.
     *
     * @return  corresponding integer
     */
    private static int charToNibble(char c) {
        if ('0' <= c && c <= '9') {
            return c - '0';
        }
        else if ('a' <= c && c <= 'f') {
            return c - 'a' + 0xa;
        }
        else if ('A' <= c && c <= 'F') {
            return c - 'A' + 0xa;
        }
        else {
            throw new IllegalArgumentException("Invalid hex character: " + c);
        }
    }


    /**
     * There is no reason to create an instance of this class since all
     * methods are static.
     */
    private Hex() { }
}
