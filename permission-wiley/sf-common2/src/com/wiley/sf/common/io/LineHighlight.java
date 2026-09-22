/*
 * &copy; John Wiley &amp; Sons, Inc
 */
package com.wiley.sf.common.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

import org.apache.commons.io.IOUtils;

import com.wiley.sf.common.lang.ArgUtil;

/**
 * Provides a utility to highlight a line in some text given the text (as
 * a String, File, InputSource, or Reader) and a line number.
 * Currently the "highlighting" just consists of repeating the lines of
 * text around the line in question. The number of lines shown above and
 * below the given line is controlled by lineSurround.
 * Currently the column given is ignored.
 * 
 * @since  JDK 1.5
 * @author smarkoff, created 8/20/2007
 * @version $Id: LineHighlight.java,v 1.2 2008-07-16 12:26:16 smarkoff Exp $
 */
public class LineHighlight {
    
    private final boolean zeroStart;
    private final int lineSurround;
    
    /**
     * Specify true for zeroStart to mean that lines numbers will start with
     * zero (0) for the first line and similarly with columns.
     * Specify false for zeroStart to mean that line and column numbers
     * will start with one (1).
     * 
     * @param zeroStart
     */
    public LineHighlight(boolean zeroStart) {
        this.zeroStart = zeroStart;
        this.lineSurround = 5;  // default
    }
    
    /**
     * @param zeroStart
     * @param lineSurround  Must be 0 or greater
     */
    public LineHighlight(boolean zeroStart, int lineSurround) {
        ArgUtil.notLess0(lineSurround, "lineSurround");
        this.zeroStart = zeroStart;
        this.lineSurround = lineSurround;
    }
    
    /**
     * 
     * @param text  Must be non-null
     * @param line  Must be 0 or greater
     * @param column  Must be 0 or greater
     */
    public String highlight(String text, int line, int column) {
        ArgUtil.notNull(text, "text");
        validateLineCol(line, column);
        
        try {
            return read(text, line, column);
        }
        catch (Exception ex) {
            return "[Exception trying to read text: " + ex.toString() + "]";
        }
    }
    
    /**
     * Note this method is designed to only load lines of the file
     * around the given line number, so it won't use up a lot of memory
     * with a large file.
     * 
     * @param textFile  Must be an existing file
     * @param line      Must be 0 or greater
     * @param column    Must be 0 or greater
     */
    public String highlight(File textFile, int line, int column) {
        ArgUtil.notNull(textFile, "textFile");
        validateLineCol(line, column);
        
        try {
            return read(textFile, line, column);
        }
        catch (Exception ex) {
            return "[Exception trying to load file: " + ex.toString() + "]";
        }
    }
    
    /**
     * Note this method is designed to only load lines of the Reader
     * around the given line number, so it won't use up a lot of memory
     * with a large stream.
     * 
     * @param reader  Must be non-null
     * @param line      Must be 0 or greater
     * @param column    Must be 0 or greater
     */
    public String highlight(Reader reader, int line, int column) {
        ArgUtil.notNull(reader, "reader");
        validateLineCol(line, column);
        
        try {
            return read(reader, line, column);
        }
        catch (Exception ex) {
            return "[Exception trying to load Reader: " + ex.toString() + "]";
        }
    }
    
    private void validateLineCol(int line, int column) {
        int start = zeroStart ? 0 : 1;
        if (line < start) {
            throw new IllegalArgumentException("line cannot be < " + start);
        }
        if (column < start) {
            throw new IllegalArgumentException("column cannot be < " + start);
        }
    }
 
    private String read(String text, int line, int column) throws IOException {
        Reader reader = new StringReader(text);
        return read(reader, line, column);  // throws IOException
    }
    
    private String read(File file, int line, int column) throws IOException {
        FileInputStream fis = new FileInputStream(file);  // throws IOException
        InputStreamReader reader = null;
        
        try {
            return read(reader, line, column);  // throws IOException
        }
        finally {
            IOUtils.closeQuietly(reader);
            IOUtils.closeQuietly(fis);
        }
    }
    
    /**
     * The calling code is responsible for closing the reader passed in.
     */
    private String read(Reader reader, int lineNumber, int column) throws IOException {
        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();
        
        try {
            br = new BufferedReader(reader);
            String line;
            int currentLine = zeroStart ? -1 : 0;
            while ((line = br.readLine()) != null) {
                currentLine++;
                if (Math.abs(currentLine - lineNumber) <= lineSurround) {
                    sb.append(currentLine);
                    sb.append(": ");
                    sb.append(line);
                    sb.append("\n");
                }
            }
            
            return sb.toString();
        }
        finally {
            IOUtils.closeQuietly(br);
        }
    }
}
