package com.wiley.sf.common.io;

import org.apache.commons.logging.Log;

import com.wiley.sf.common.lang.ArgUtil;

/**
 * Fix bad characters.
 *
 * @since   JDK 1.6
 * @version 12/19/2012
 * @author  Steve Markoff
 *
 * @see com.wiley.sf.common.lang.StringUtil#removeSpecialControlChars(String)
 */
public class FixWindows1252Chars {

    private static char [] convertArray = {
        //           dec:hex
        '\u20ac', // 128:80 - euro sign
        0,        // 129:81 - illegal even in Windows-1252
        '\u201a', // 130:82 - single low-9 quotation mark
        '\u0192', // 131:83 - latin small letter f with hook
        '\u201e', // 132:84 - double low-9 quotation mark
        '\u2026', // 133:85 - horizontal ellipsis
        '\u2020', // 134:86 - dagger
        '\u2021', // 135:87 - double dagger
        '\u02c6', // 136:88 - modifier letter circumflex accent
        '\u2030', // 137:89 - per mille sign
        '\u0160', // 138:8a - latin capital letter s with caron
        '\u2039', // 139:8b - single left-pointing angle quotation mark
        '\u0152', // 140:8c - latin capital ligature OE
        0,        // 141:8d - illegal even in Windows-1252
        '\u017d', // 142:8e - latin capital letter z with caron
        0,        // 143:8f - illegal even in Windows-1252
        0,        // 144:90 - illegal even in Windows-1252
        '\u2018', // 145:91 - single left quote
        '\u2019', // 146:92 - single right quote
        '\u201c', // 147:93 - double left quote
        '\u201d', // 148:94 - double right quote
        '\u2022', // 149:95 - bullet
        '\u2013', // 150:96 - en dash
        '\u2014', // 151:97 - em dash
        '\u02dc', // 152:98 - small tilde
        '\u2122', // 153:99 - TM symbol
        '\u0161', // 154:9a - latin small letter s with caron
        '\u203a', // 155:9b - single right-pointing angle quotation mark
        '\u0153', // 156:9c - latin small ligature OE
        0,        // 157:9d - illegal even in Windows-1252
        '\u017e', // 158:9e - latin small letter z with caron
        '\u0178', // 159:9f - latin capital letter y with diaeresis
    };

    /**
     * Check for characters that are only legal in the Windows-1252
     * character set, log them, and fix them where possible.
     *
     * @param s  May be null
     * @param fieldName  Should be non-null
     * @param methodName  Should be non-null
     * @param log  Must be non-null
     */
    public static String fix(String s, String fieldName, String methodName, Log log) {
        if (s == null)  return null;
        ArgUtil.notNull(log, "log");

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (c >= 128 && c <= 159) {
                char replaceChar = convertArray[c - 128];

                if (replaceChar == 0) {
                    log.warn(methodName + "(): " + fieldName + " contains illegal char: index = " + i
                            + ", decimal value = " + c + ", " + fieldName + ": " + s);
                }
                else {
                    log.debug(methodName + "(): " + fieldName + " contains char "
                            + ((int)c) + " - replacing with " + ((int)replaceChar));
                    s = s.replace(c, replaceChar);
                }
            }
        }

        return s;
    }

    /**
     * Check for characters that are only legal in the Windows-1252
     * character set and fix them where possible.
     *
     * @param s  May be null
     */
    public static String fix(String s) {
        if (s == null)  return null;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (c >= 128 && c <= 159) {
                char replaceChar = convertArray[c - 128];

                if (replaceChar == 0) {
                    // do nothing
                }
                else {
                    s = s.replace(c, replaceChar);
                }
            }
        }

        return s;
    }
}
