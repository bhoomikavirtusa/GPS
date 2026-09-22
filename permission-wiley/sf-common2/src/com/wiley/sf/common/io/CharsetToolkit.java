package com.wiley.sf.common.io;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.wiley.sf.common.lang.ArgUtil;

/**
 * The primary method that will be used is the static method
 * determineCharset(File or InputStream).
 *
 * This class makes the assumption that files are in one of four
 * encodings: 7-bit ASCII, UTF-8, Latin-1 (aka ISO 8859-1), or
 * Windows-1252. Many more encodings exist but are less common.
 *
 * Given the assumption about what charsets we have to choose from, this class
 * can determine the actual encoding. But it will not determine the correct
 * encoding if in fact a file uses a more obscure charset. (Might also try
 * the Unix "file" utility).
 *
 * Some notes about these charsets:
 * Latin-1 (ISO 8859-1) is a superset of 7-bit ASCII.
 * Windows-1252 is basically a superset of Latin-1 - it redefines some
 * characters that were non-printable in Latin-1 as different printable
 * characters.
 * UTF-8 is a (much larger) superset of 7-bit ASCII.
 * Windows-1252 and Latin-1 are the same as ASCII and UTF-8
 * in the 7-bit range but different in the 8-bit range.
 *
 * @author smarkoff, created April 25, 2007
 * @version $Id: CharsetToolkit.java,v 1.3 2008-12-31 16:43:27 smarkoff Exp $
 */
public class CharsetToolkit {

    // -------------------------- static methods ----------------------------

    public static void main(String [] args) throws IOException {
        if (args.length < 1) {
            System.err.println("usage: <file to test>");
            System.exit(1);
        }

        File file = new File(args[0]);
        CharsetToolkit tk = new CharsetToolkit(file);
        tk.det();  // throws IOException
        System.out.println(tk.getCharsetInfo().toString());
    }

    /**
     * @param file  Must be non-null
     */
    public static CharsetInfo determineCharset(File file) throws IOException {
        CharsetToolkit tk = new CharsetToolkit(file);
        return determineCharset(tk);
    }

    /**
     * @param in  Must be non-null
     */
    public static CharsetInfo determineCharset(InputStream in) throws IOException {
        CharsetToolkit tk = new CharsetToolkit(in);
        return determineCharset(tk);
    }

    private static CharsetInfo determineCharset(CharsetToolkit tk) throws IOException {
        tk.det();  // throws IOException
        return tk.getCharsetInfo();
    }

    /**
     * Get the value of a particular bit from an int.
     *
     * @param bitStore  The int you want to read the bit from
     * @param bitIndex  Must be 0-31
     * @return          The bit value (true corresponds to 1,
     *                  false corresponds to 0)
     */
    private static boolean getBit(int bitStore, int bitIndex) {
        final int mask = 1 << bitIndex;
        return (mask & bitStore) == mask;
    }

    // --------------------------- instance data ----------------------------

    private File file = null;
    private InputStream inStream = null;

    private int num7BitChars = 0;
    private int numUTF8Chars = 0;
    private int numLatin1Chars = 0;
    private int numWindows1252Chars = 0;
    private CharsetType charsetType = CharsetType.UNKNOWN;
    private CharsetInfo charsetInfo = null;

    // --------------------------- instance methods -------------------------

    /**
     * @param file  Must be non-null
     */
    public CharsetToolkit(File file) {
        ArgUtil.notNull(file, "file");
        this.file = file;
    }

    /**
     * @param inStream  Must be non-null
     */
    public CharsetToolkit(InputStream inStream) {
        ArgUtil.notNull(inStream, "inStream");
        this.inStream = inStream;
    }

    public void det() throws IOException {
        FileInputStream fin = null;
        if (file != null) {
            fin = new FileInputStream(file);
            inStream = new BufferedInputStream(fin);
        }

        int next = inStream.read();
        while (next != -1) {
            if (next < 128)  num7BitChars++;
            else {
                checkHigherByte(next);
            }
            next = inStream.read();
        }

        if (file != null) {
            inStream.close();
            fin.close();
        }

        pickCode();

        charsetInfo = new CharsetInfo(charsetType, num7BitChars, numUTF8Chars,
            numLatin1Chars, numWindows1252Chars);
    }

    public CharsetType getCharsetType() {
        return charsetType;
    }

    public CharsetInfo getCharsetInfo() {
        return charsetInfo;
    }

    private void pickCode() {
        if (numLatin1Chars == 0 && numWindows1252Chars == 0) {
            if (numUTF8Chars == 0) {
                charsetType = CharsetType.ASCII;
            }
            else {
                charsetType = CharsetType.UTF8;
            }
        }
        else {
            if (numUTF8Chars > 0) {
                charsetType = CharsetType.UNKNOWN;
                // -- in this case may want to go back and assume everything is Latin-1 or Windows-1252
            }
            else {
                if (numWindows1252Chars > 0) {
                    charsetType = CharsetType.WINDOWS1252;
                }
                else {
                    charsetType = CharsetType.LATIN1;
                }
            }
        }
    }

    private void checkHigherByte(int next) throws IOException {
        // UTF-8 reference notes
        // bytes | bits | representation
        // 1     |    7 | 0vvvvvvv
        // 2     |   11 | 110vvvvv 10vvvvvv
        // 3     |   16 | 1110vvvv 10vvvvvv 10vvvvvv
        // 4     |   21 | 11110vvv 10vvvvvv 10vvvvvv 10vvvvvv

        // see if byte is of the form 11xxxxxx
        if (getBit(next, 7) && getBit(next, 6)) {
            if (!getBit(next, 5))  finishUTF8(next, 1);
            else if (!getBit(next, 4))  finishUTF8(next, 2);
            else if (!getBit(next, 3))  finishUTF8(next, 3);
            else  {
                checkNonUTF8Byte(next);
            }
        }
        else {
            checkNonUTF8Byte(next);
        }
    }

    /**
     * @param next
     * @param numExtraBytes  Should be 1, 2, or 3
     */
    private void finishUTF8(int next, int numExtraBytes) throws IOException {
        // next <numExtraBytes] should be of the form 10vvvvvv if char is
        // UTF-8 but might not be
        int numRead = 0;
        int [] extra = new int[3];
        for (int i = 0; i < numExtraBytes; i++) {
            extra[i] = inStream.read();
            if (extra[i] == -1) {
                checkNonUTF8Byte(next);
                for (int c = 0; c < numRead; c++) {
                    checkNonUTF8Byte(extra[c]);
                }
                break;
            }
            else {
                numRead++;
                if (getBit(extra[i], 7) && !getBit(extra[i], 6)) {
                    if (numRead == numExtraBytes)  numUTF8Chars++;
                }
                else {
                    checkNonUTF8Byte(next);
                    for (int c = 0; c < numRead; c++) {
                        checkNonUTF8Byte(extra[c]);
                    }
                    break;
                }
            }
        } // end for
    }

    private void checkNonUTF8Byte(int next) {
        if (next >= 128 && next <= 159) {
            numWindows1252Chars++;
        }
        else {
            numLatin1Chars++;
        }
    }
}
