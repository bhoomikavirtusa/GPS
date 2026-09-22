/*
 * &copy; John Wiley &amp; Sons, Inc
 */
package com.wiley.sf.common.io;

import java.text.NumberFormat;

import com.wiley.sf.common.lang.StringUtil;

/**
 * Encapsulates results of calling CleanOldFiles.clean().
 * 
 * @since JDK 1.5
 * @author smarkoff, created 8/28/2008
 * @version $Id: CleanOldFilesResult.java,v 1.1 2008-08-29 17:28:58 smarkoff Exp $
 */
public class CleanOldFilesResult {

    private final int numFilesDeleted;
    private final long totalBytesDeleted;
    private final int numDirsDeleted;
    private final int failedFileDeletes;
    private final int failedDirDeletes;
    
    public CleanOldFilesResult(int numFilesDeleted, long totalBytesDeleted,
            int numDirsDeleted, int failedFileDeletes, int failedDirDeletes)
    {
        this.numFilesDeleted = numFilesDeleted;
        this.totalBytesDeleted = totalBytesDeleted;
        this.numDirsDeleted = numDirsDeleted;
        this.failedFileDeletes = failedFileDeletes;
        this.failedDirDeletes = failedDirDeletes;
    }
    
    public int getNumFilesDeleted() { return numFilesDeleted; }
    
    public long getTotalBytesDeleted() { return totalBytesDeleted; }
    
    public int getNumDirsDeleted() { return numDirsDeleted; }
    
    public int getFailedFileDeletes() { return failedFileDeletes; }
    
    public int getFailedDirDeletes() { return failedDirDeletes; }
    
    @Override
    public String toString() {
        // declare NumberFormat locally since not threadsafe
        final NumberFormat intFormat = NumberFormat.getIntegerInstance();
        
        final String ls = StringUtil.getLineSeparator();
        
        return "numFilesDeleted: " + intFormat.format(numFilesDeleted) + ls
            + "totalBytesDeleted: " + intFormat.format(totalBytesDeleted) + ls
            + "numDirsDeleted: " + intFormat.format(numDirsDeleted) + ls
            + "failedFileDeletes: " + intFormat.format(failedFileDeletes) + ls
            + "failedDirDeletes: " + intFormat.format(failedDirDeletes) + ls;
    }
}
