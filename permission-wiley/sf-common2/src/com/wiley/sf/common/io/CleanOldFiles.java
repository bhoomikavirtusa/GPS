/*
 * &copy; John Wiley &amp; Sons, Inc
 */
package com.wiley.sf.common.io;

import java.io.File;
import java.io.FileFilter;
import java.text.SimpleDateFormat;
import java.util.Arrays;

import org.apache.commons.io.comparator.LastModifiedFileComparator;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.sf.common.lang.ArgUtil;
import com.wiley.sf.common.io.FileUtil;


/**
 * Provides a simple API to look through a directory and delete
 * files which are older than a certain number of days.
 * 
 * @since JDK 1.5
 * @author smarkoff, created 8/28/2008
 * @version $Id: CleanOldFiles.java,v 1.7 2008-10-17 17:14:43 smarkoff Exp $
 */
public class CleanOldFiles {
    
    private static final Log log = LogFactory.getLog(CleanOldFiles.class);
    
    /**
     * Deletes files in the specified directory that are at least
     * the specified number of days old, but optionally keeping at least
     * a certain number of files.
     *
     * If deleteSubDirs is true, directories will only be deleted that are
     * XX days old and empty (before or after deleting old files in them).
     *
     * @param dir           Must be non-null and be an existing directory
     * @param days          Should be 0 or greater (0 means delete everything)
     * @param keepAmount    Means regardless of days, keep this number of files for each directory (may be 0)
     * @param cleanSubDirs  Whether to clean sub-directories
     * @param deleteSubDirs  Whether to delete sub-directories
     */
    public static CleanOldFilesResult clean(File dir, int days, int keepAmount,
            boolean cleanSubDirs, boolean deleteSubDirs)
    {
        CleanOldFiles clean = new CleanOldFiles(dir, days, keepAmount,
            cleanSubDirs, deleteSubDirs);
        return clean.clean();
    }
    
    
    private final File dir;
    private final long ageCutoff;
    private final int keepAmount;
    private final boolean cleanSubDirs;
    private final boolean deleteSubDirs;
    private final long currentTime;
    private final SimpleDateFormat dateFormat =
        new SimpleDateFormat("yyyy-MM-dd kk:mm  zzz");
    
    private int numFilesDeleted = 0;
    private int numDirsDeleted = 0;
    private long totalBytesDeleted = 0;
    private int failedFileDeletes = 0;
    private int failedDirDeletes = 0;
    

    private CleanOldFiles(File dir, int days, int keepAmount,
            boolean cleanSubDirs, boolean deleteSubDirs)
    {
        ArgUtil.notNull(dir, "dir");
        FileUtil.checkDirectory(dir.getAbsolutePath());
        this.dir = dir;
        currentTime = System.currentTimeMillis();
        // Very important to use a long for days in ageCutoff calculation
        long daysLong = days;
        ageCutoff = currentTime - (daysLong * 24 * 60 * 60 * 1000);
        log.debug("ageCutoff = " + dateFormat.format(ageCutoff));
        this.keepAmount = keepAmount;
        this.cleanSubDirs = cleanSubDirs;
        this.deleteSubDirs = deleteSubDirs;
    }
    
    private CleanOldFilesResult clean() {
        clean(dir, true);
        return new CleanOldFilesResult(numFilesDeleted, totalBytesDeleted,
            numDirsDeleted, failedFileDeletes, failedDirDeletes);
    }
    
    private void clean(File dir, boolean root) {
        // Note on Windows and Linux (and probably all platforms),
        // when you delete a file from a directory (or add one)
        // this changes the lastModified date of the directory.
        // So get the dir lastModified first, and then check later,
        // so a directory just emptied of old files will be
        // deleted right away.
        
        long dirLastModified = dir.lastModified();
        
        // deal with sub-directories first
        if (cleanSubDirs) {
            File [] fileArray = dir.listFiles((FileFilter)DirectoryFileFilter.DIRECTORY);
        
            for (File file: fileArray) {
                clean(file, false);
            }
        }
        
        // deal with files
        File [] fileArray = dir.listFiles((FileFilter)FileFileFilter.FILE);
        // sort so newest files are first
        // Get compiler warning since LastModifiedFileComparator is not "Generified"
        // - Probably later Apache will update for JDK 1.5
        Arrays.sort(fileArray, LastModifiedFileComparator.LASTMODIFIED_REVERSE);
        
        int startIndex = 0;
        if (keepAmount > 0)  startIndex = keepAmount;
        
        for (int f = startIndex; f < fileArray.length; f++) {
            File file = fileArray[f];
            
            if (file.lastModified() < ageCutoff) {
                long size = file.length();
                int daysOld = daysOld(file);
                boolean ok = file.delete();
                if (ok) {
                    log.debug("deleted file (" + daysOld + " days old): "
                        + file.getAbsolutePath());
                    numFilesDeleted++;
                    totalBytesDeleted += size;
                }
                else {
                    log.warn("could not delete file: " + file.getAbsolutePath());
                    failedFileDeletes++;
                }
            }
        }
        
        if (!root && deleteSubDirs && dirLastModified < ageCutoff) {
            fileArray = dir.listFiles();

            if (fileArray.length == 0) {
                int daysOld = daysOld(dir);
                boolean ok = dir.delete();
                if (ok) {
                    log.debug("deleted dir (" + daysOld + " days old): "
                        + dir.getAbsolutePath());
                    numDirsDeleted++;
                }
                else {
                    log.warn("could not delete dir: " + dir.getAbsolutePath());
                    failedDirDeletes++;
                }
            }
        }
    }
    
    private int daysOld(File file) {
        long ms = currentTime - file.lastModified();
        long days = ms / 1000 / 60 / 60 / 24;
        return (int) days;
    }

}
