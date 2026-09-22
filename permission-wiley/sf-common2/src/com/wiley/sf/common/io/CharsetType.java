package com.wiley.sf.common.io;

/**
 * 
 * @author smarkoff, created May 18, 2007
 * @version $Id: CharsetType.java,v 1.1 2008-02-09 00:53:35 smarkoff Exp $
 */
public class CharsetType {

    public static final CharsetType UNKNOWN
        = new CharsetType(0, "Unknown",
        "Could not determine since some UTF-8 and some other.");
    public static final CharsetType ASCII
        = new CharsetType(1, "ASCII",
        "plain ASCII (7-bit) (also UTF-8 compatible)");
    public static final CharsetType UTF8
        = new CharsetType(2, "UTF-8",
        "UTF-8");
    public static final CharsetType LATIN1
        = new CharsetType(3, "Latin-1 aka ISO 8859-1",
        "Latin-1 (ISO 8859-1) - also compatible with Windows-1252");
    public static final CharsetType WINDOWS1252
        = new CharsetType(4, "Windows-1252",
        "Windows-1252");
    
    
    private final int code;
    private final String name;
    private final String detail;
    
    private CharsetType(int code, String name, String detail) {
        this.code = code;
        this.name = name;
        this.detail = detail;
    }
    
    public int getCode() { return code; }
    public String getName() { return name; }
    public String getDetail() { return detail; }
    
}
