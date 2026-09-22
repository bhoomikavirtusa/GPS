package com.wiley.sf.common.io;

import java.io.File;
import java.io.IOException;

import org.junit.*;
import static org.junit.Assert.*;

/**
 * JUnit test class for CleanOldFiles.
 *
 * @since   JDK 1.5, JUnit 4.4
 * @version $Id: CleanOldFilesTest.java,v 1.3 2008-09-02 12:58:47 smarkoff Exp $
 * @author  Steve Markoff
 */
public class CleanOldFilesTest {

    private long currentTime = System.currentTimeMillis();
    
    @Test
    public void test() throws IOException {
        File rootDir = FileUtil.createTempDirectory("CleanOldFilesTest", true);
            // throws IOException
        File subDir = new File(rootDir, "sub");
        subDir.mkdir();
        
        // create 4 new files in root dir
        for (int i = 0; i < 4; i++) {
            File file = new File(rootDir, "new" + i + ".txt");
            FileUtil.stringToFile("foo", file);
        }
        
        // create 4 old files in root dir
        for (int i = 0; i < 4; i++) {
            File file = new File(rootDir, "old" + i + ".txt");
            FileUtil.stringToFile("foo", file);
            setDaysOld(file, 5);
        }
        
        // create 4 new files in sub dir
        for (int i = 0; i < 4; i++) {
            File file = new File(subDir, "new" + i + ".txt");
            FileUtil.stringToFile("foo", file);
        }
        
        // create 4 old files in sub dir
        for (int i = 0; i < 4; i++) {
            File file = new File(subDir, "old" + i + ".txt");
            FileUtil.stringToFile("foo", file);
            setDaysOld(file, 5);
        }
        
        CleanOldFiles.clean(rootDir, 2, 6, true, false);
        
        int numFiles = rootDir.listFiles().length - 1;  // -1 for sub dir
        assertTrue("numFiles expected to be 6", numFiles == 6);
        
        numFiles = subDir.listFiles().length;
        assertTrue("numFiles expected to be 6", numFiles == 6);
        
        CleanOldFiles.clean(rootDir, 2, 1, true, false);
        
        numFiles = rootDir.listFiles().length - 1;  // -1 for sub dir
        assertTrue("numFiles expected to be 4", numFiles == 4);
        
        numFiles = subDir.listFiles().length;
        assertTrue("numFiles expected to be 4", numFiles == 4);
        
        CleanOldFiles.clean(rootDir, 0, 0, true, false);
        CleanOldFiles.clean(rootDir, 2, 0, true, true);
        
        numFiles = rootDir.listFiles().length;
        assertTrue("numFiles expected to be 1", numFiles == 1);
        
        setDaysOld(subDir, 5);
        CleanOldFiles.clean(rootDir, 2, 2, true, true);
        
        numFiles = rootDir.listFiles().length;
        assertTrue("numFiles expected to be 0", numFiles == 0);
    }
    
    private void setDaysOld(File file, int days) {
        file.setLastModified(currentTime - (days * 24 * 60 * 60 * 1000));
    }
}
