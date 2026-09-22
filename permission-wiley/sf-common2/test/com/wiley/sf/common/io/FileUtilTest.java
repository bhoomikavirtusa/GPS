package com.wiley.sf.common.io;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

/**
 * JUnit test class for FileUtil.
 *
 * @since JDK 1.6, JUnit 4.10
 * @version $Id: FileUtilTest.java,v 1.9 2013-02-23 00:02:54 smarkoff Exp $
 * @author Steve Markoff
 */
public class FileUtilTest {

    @Test
    public void stringToFileAndBack()
        throws IOException
    {
        File file = File.createTempFile("stf", null);  // throws IOException
        String s = "abc\u2105\u211E\u2122";
        FileUtil.stringToFile(s, file);  // throws IOException
        System.out.println("string length = " + s.length());
        System.out.println("file length = " + file.length());
        String msg = "String length = " + s.length() + ", file length = "
            + file.length() + " - expected file length to be longer because "
            + "of double-byte characters.";
        assertTrue(msg, file.length() > s.length());

        String s2 = FileUtil.fileToString(file);
        msg = "Original string didn't match resulting string.";
        assertTrue(msg, s2.equals(s));
    }

    @Test
    public void lowerExtension() {
        assertTrue(FileUtil.lowerExtension((String) null) == null);
        assertTrue(FileUtil.lowerExtension("").equals(""));
        assertTrue(FileUtil.lowerExtension("foo").equals("foo"));
        assertTrue(FileUtil.lowerExtension("foo.").equals("foo."));
        assertTrue(FileUtil.lowerExtension("foo.TXT").equals("foo.txt"));
        assertTrue(FileUtil.lowerExtension("DIR/FOO.2.txt").equals("DIR/FOO.2.txt"));
        assertTrue(FileUtil.lowerExtension("dir/foo.2.TXT").equals("dir/foo.2.txt"));
        assertTrue(FileUtil.lowerExtension("dir.TXT/foo.TXT").equals("dir.TXT/foo.txt"));
    }

    @Test
    public void getNameWithoutExtension() {
        getNameWithoutExtension(null, null);
        getNameWithoutExtension("", "");
        getNameWithoutExtension("name.zip", "name");
        getNameWithoutExtension("name", "name");
        getNameWithoutExtension("path/name.txt", "path/name");
        getNameWithoutExtension("path/name.mid.txt", "path/name.mid");
        getNameWithoutExtension("path.2/name.zip", "path.2/name");
        getNameWithoutExtension("path.2/name", "path.2/name");
    }

    private void getNameWithoutExtension(String fileName, String expected) {
        String result = FileUtil.getNameWithoutExtension(fileName);
        String msg = "fileName = " + fileName + ", expected = " + expected
            + ", actual = " + result;
        assertTrue(msg, StringUtils.equals(expected, result));
    }

    @Test
    public void replaceExtension() {
        replaceExtension(null, "zip", null);
        replaceExtension("name.zip", null, "name");
        replaceExtension("name", "zip", "name.zip");
        replaceExtension("path/name.mid.txt", "zip", "path/name.mid.zip");
        replaceExtension("path/name.mid.txt", null, "path/name.mid");
        replaceExtension("path.2/name.zip", "txt", "path.2/name.txt");
        replaceExtension("path.2/name", "txt", "path.2/name.txt");
    }

    private void replaceExtension(String fileName, String newExt, String expected) {
        String result = FileUtil.replaceExtension(fileName, newExt);
        String msg = "fileName = " + fileName + ", newExt = " + newExt
            + ", expected = " + expected + ", actual = " + result;
        assertTrue(msg, StringUtils.equals(expected, result));
    }

    @Test
    public void moveFile()
        throws IOException
    {
        // Test moving file to file (same file system) where
        // 2nd file does NOT yet exist
        File sourceFile = File.createTempFile("abc", null);
            // throws IOException
        File targetFile = new File(sourceFile.getParentFile(),
            "tg_" + sourceFile.getName());
        moveFile(sourceFile, targetFile);

        // Test moving file to file (same file system) where
        // 2nd file ALREADY exists
        sourceFile = File.createTempFile("abc", null);
        targetFile = File.createTempFile("def", null);
        moveFile(sourceFile, targetFile);

        // Test moving file to directory (same file system)
        sourceFile = File.createTempFile("abc", null);
        File targetDir = FileUtil.createTempDirectory(null, false);
        targetFile = new File(targetDir, sourceFile.getName());
        moveFile(sourceFile, targetFile);
        targetDir.delete();

        // Following is difficult to test with a unit test:
        // Test moving file to file on a different file system
        // Test moving file to directory on different file system
    }

    private void moveFile(File sourceFile, File targetFile) throws IOException {
        Random random = new Random();
        final int SIZE = random.nextInt(9000) + 1000;
        FileUtil.createFileOfSize(sourceFile.getPath(), SIZE);
            // throws IOException
        assertTrue(sourceFile.length() == SIZE);
        FileUtil.moveFile(sourceFile, targetFile);  // throws IOException
        assertFalse("Source file still exists.", sourceFile.exists());
        assertTrue("File size was " + targetFile.length() + " instead of " + SIZE,
            targetFile.length() == SIZE);
        targetFile.delete();
    }

    @Test
    public void createFileOfSize()
        throws IOException
    {
        File file = File.createTempFile("abc", null);
        final int SIZE = 4561;
        FileUtil.createFileOfSize(file.getPath(), SIZE);
        assertTrue(file.length() == SIZE);
        file.delete();
    }

    @Test
    public void checksumCompare()
        throws IOException
    {
        String s1 = "The quick brown fox ate 55 sheep.";
        String s2 = "The quick brown fox ate 55 sheep.";
        checksumCompare(s1, s2, true);

        s2 = "The quick brown fox ate 55 lambs.";
        checksumCompare(s1, s2, false);
        s2 = "The quick brown fox ate 56 sheep.";
        checksumCompare(s1, s2, false);
    }

    private void checksumCompare(String s1, String s2, boolean expected)
        throws IOException
    {
        File file1 = File.createTempFile("test", null);  // throws IOException
        File file2 = File.createTempFile("test", null);  //
        FileUtil.stringToFile(s1, file1);
        FileUtil.stringToFile(s2, file2);
        boolean result = FileUtil.checksumCompare(file1, file2);
        file1.delete();
        file2.delete();
        String msg = "result: " + result + ", expected: " + expected
            + ", string1: " + s1 + ", string2: " + s2;
        assertTrue(msg, result == expected);
    }
}
