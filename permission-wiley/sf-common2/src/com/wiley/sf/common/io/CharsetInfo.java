package com.wiley.sf.common.io;

import java.text.NumberFormat;

/**
 * 
 * @author smarkoff, created 6/8/2007
 * @version $Id: CharsetInfo.java,v 1.2 2008-02-21 19:02:17 smarkoff Exp $
 */
public class CharsetInfo {
    
    private static final NumberFormat intFormat = NumberFormat.getIntegerInstance();
    
    private final CharsetType type;
    private final int num7BitChars;
    private final int numUTF8Chars;
    private final int numLatin1Chars;
    private final int numWindows1252Chars;
    
    public CharsetInfo(CharsetType type, int num7BitChars, int numUTF8Chars,
        int numLatin1Chars, int numWindows1252Chars)
    {
        this.type = type;
        this.num7BitChars = num7BitChars;
        this.numUTF8Chars = numUTF8Chars;
        this.numLatin1Chars = numLatin1Chars;
        this.numWindows1252Chars = numWindows1252Chars;
    }
    
    public CharsetType getCharsetType() {
        return type;
    }
    
    public int getNum7BitChars() {
        return num7BitChars;
    }

    public int getNumUTF8Chars() {
        return numUTF8Chars;
    }
    
    public int getNumLatin1Chars() {
        return numLatin1Chars;
    }
    
    public int getNumWindows1252Chars() {
        return numWindows1252Chars;
    }
    
    public String toString() {
        final String newLine = "\n";
        StringBuilder sb = new StringBuilder();
        sb.append("num7BitChars = " + intFormat.format(num7BitChars));
        sb.append(newLine);
        sb.append("numUTF8Chars = " + intFormat.format(numUTF8Chars));
        sb.append(newLine);
        sb.append("numLatin1Chars = " + intFormat.format(numLatin1Chars));
        sb.append(newLine);
        sb.append("numWindows1252Chars = " + intFormat.format(numWindows1252Chars));
        sb.append(newLine);
        sb.append("type = " + type.getDetail());

        return sb.toString();
    }
}
