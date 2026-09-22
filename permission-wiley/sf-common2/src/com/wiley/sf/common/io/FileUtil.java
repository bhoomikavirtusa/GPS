/*
 * &copy; John Wiley &amp; Sons, Inc
 */
package com.wiley.sf.common.io;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.sf.common.codec.MessageDigestUtil;
import com.wiley.sf.common.lang.ArgUtil;

/**
 * General utility methods that deal with files.
 *
 * Some methods that this class does NOT contain because they can be found
 * in org.apache.commons.io.FileUtils:
 * byteCountToDisplaySize, contentEquals,
 * copyFile, copyDirectory, cleanDirectory, deleteDirectory,
 * listFiles, touch
 *
 * or org.apache.commons.io.IOUtils:
 * closeQuietly
 * copy (InputStream to OutputStream, Reader to Writer)
 * toString(InputStream or Reader)
 *
 * or org.apache.commons.io.FilenameUtils:
 * getExtension, removeExtension, normalize, equalsNormalized
 *
 * or org.apache.commons.lang.SystemUtils:
 * getJavaIoTmpDir
 *
 * Other useful classes in Apache Commons IO:
 * DirectoryWalker, SizeFileFilter, SuffixFileFilter, WildcardFileFilter
 *
 * Note this class does contain fileToString and stringToFile methods
 * even though org.apache.commons.io.FileUtils contains two very
 * similar methods called readFileToString and writeStringToFile.
 * The main difference is that the methods in this class always
 * assume that files are UTF-8 encoded.
 *
 * @since   JDK 1.5
 * @version $Id: FileUtil.java,v 1.20 2013-02-23 00:14:06 smarkoff Exp $
 * @author  Steve Markoff
 */
public class FileUtil {

    private static final Log log = LogFactory.getLog(FileUtil.class);

    /**
     * Checks that the given input is an existing file and is not
     * a directory. Checks that the file is readable.
     * Returns a File object if these conditions are true,
     * otherwise throws an IllegalArgumentException.
     *
     * @param fileName  Must be non-null and non-blank
     */
    public static File checkFile(String fileName) {
        return checkFile(fileName, false);
    }

    /**
     * Checks that the given input is an existing directory and is not
     * a file. Checks that the directory is readable.
     * Returns a File object if these conditions are true,
     * otherwise throws an IllegalArgumentException.
     *
     * @param dirName  Must be non-null and non-blank
     */
    public static File checkDirectory(String dirName) {
        return checkDirectory(dirName, false);
    }

    /**
     * Checks that the given input is an existing file and is not
     * a directory. Checks that the file is readable.
     * If writable is true, also checks that the file is writable.
     * Returns a File object if these conditions are true,
     * otherwise throws an IllegalArgumentException.
     *
     * @param fileName  Must be non-null and non-blank
     * @param writable
     */
    public static File checkFile(String fileName, boolean writable) {
        ArgUtil.notBlank(fileName, "fileName");
        File file = new File(fileName);
        checkFile(file, writable);
        return file;
    }

    /**
     * Checks that the given input is an existing file and is not
     * a directory. Checks that the file is readable.
     * If writable is true, also checks that the file is writable.
     * If any of these conditions are not true,
     * throws an IllegalArgumentException.
     *
     * @param file      Must be non-null
     * @param writable
     */
    public static void checkFile(File file, boolean writable) {
        ArgUtil.notNull(file, "file");

        if (!file.exists() || file.isDirectory()) {
            throw new IllegalArgumentException(file.getPath()
                + " is not an existing file!");
        }
        if (!file.canRead()) {
            throw new IllegalArgumentException(file.getPath()
                + " is not readable!");
        }
        if (writable && !file.canWrite()) {
            throw new IllegalArgumentException(file.getPath()
                + " is not writable!");
        }
    }

    /**
     * Checks that the given input is an existing directory and is not
     * a file. Checks that the directory is readable.
     * If writable is true, also checks that the directory is writable.
     * Returns a File object if these conditions are true,
     * otherwise throws an IllegalArgumentException.
     *
     * @param dirName  Must be non-null and non-blank
     * @param writable
     */
    public static File checkDirectory(String dirName, boolean writable) {
        ArgUtil.notBlank(dirName, "dirName");
        File dir = new File(dirName);
        checkDirectory(dir, writable);
        return dir;
    }

    /**
     * Checks that the given input is an existing directory and is not
     * a file. Checks that the directory is readable.
     * If writable is true, also checks that the directory is writable.
     * If any of these conditions are not true,
     * throws an IllegalArgumentException.
     *
     * @param dir       Must be non-null
     * @param writable
     */
    public static void checkDirectory(File dir, boolean writable) {
        ArgUtil.notNull(dir, "dir");

        if (!dir.exists() || dir.isFile()) {
            throw new IllegalArgumentException(dir.getPath()
                + " is not an existing directory!");
        }
        if (!dir.canRead()) {
            throw new IllegalArgumentException(dir.getPath() + " is not readable!");
        }
        if (writable && !dir.canWrite()) {
            throw new IllegalArgumentException(dir.getPath() + " is not writable!");
        }
    }

    /**
     * Check that the specified directory either already exists
     * or can be created (goes ahead and creates),
     * is not a file, and is both readable and writable.
     * Returns a File object if these conditions are true,
     * otherwise throws an IllegalArgumentException.
     *
     * @param dirName  Must be non-blank
     */
    public static File checkCreateDirectory(String dirName) {
        ArgUtil.notBlank(dirName, "dirName");
        File dir = new File(dirName);
        checkCreateDirectory(dir);
        return dir;
    }

    /**
     * Check that the specified directory either already exists
     * or can be created (goes ahead and creates),
     * is not a file, and is both readable and writable.
     * If any of these conditions are not true,
     * throws an IllegalArgumentException.
     *
     * @param dir  Must be non-null
     */
    public static void checkCreateDirectory(File dir) {
        ArgUtil.notNull(dir, "dir");

        if (!dir.exists()) {
            boolean ok = dir.mkdirs();
            if (!ok) {
                throw new IllegalArgumentException(dir.getPath()
                    + " is not an existing directory and could not be created!");
            }
        }
        if (dir.isFile()) {
            throw new IllegalArgumentException(dir.getPath()
                + " is a file, not a directory!");
        }
        if (!dir.canRead()) {
            throw new IllegalArgumentException(dir.getPath() + " is not readable!");
        }
        if (!dir.canWrite()) {
            throw new IllegalArgumentException(dir.getPath() + " is not writable!");
        }
    }

    /**
     * Use for text files only.
     * Will handle UTF-8 files properly. Note that when reading a file with
     * double-bytes characters the resulting String will be shorter in length
     * than the size of the file in bytes.
     *
     * Has same behavior as
     * org.apache.commons.io.FileUtils.readFileToString() except always
     * uses UTF-8 encoding instead of the default encoding for the VM.
     *
     * @param fileName  Must be non-null and non-blank
     */
    public static String fileToString(String fileName)
        throws IOException
    {
        ArgUtil.notBlank(fileName, "fileName");
        return fileToString(new File(fileName));
    }

    /**
     * Use for text files only.
     * Will handle UTF-8 files properly. Note that when reading a file with
     * double-byte characters the resulting String will be shorter in length
     * than the size of the file in bytes.
     *
     * Has same behavior as
     * org.apache.commons.io.FileUtils.readFileToString() except always
     * uses UTF-8 encoding instead of the default encoding for the VM.
     *
     * @param file  Must be non-null
     */
    public static String fileToString(File file)
        throws FileNotFoundException, IOException
    {
        ArgUtil.notNull(file, "file");

        if (file.length() > 4000000) {  // 4 MB
            String msg = "loading big file into memory - size = "
                + file.length() + " path = " + file.getAbsolutePath();
            log.warn("fileToString(): " + msg);
        }

        // Get the length of the file first so we can allocate all the memory
        // we need up front.
        // Need to convert file length to int because arrays take int
        // as index (also String does not support lengths bigger than int).

        // Can't use FileReader because it doesn't provide a way to set the
        // encoding.

        long fileLength = file.length();
        if (fileLength > Integer.MAX_VALUE) {
            throw new RuntimeException(
                "The file is too big to be loaded into a String.");
        }

        int fileLengthInt = (int) fileLength;
        FileInputStream fin = new FileInputStream(file);
            // throws FileNotFoundException
        InputStreamReader in = null;

        try {
            in = new InputStreamReader(fin, "UTF-8");
            // Note that the number of Unicode characters in the file may
            // be fewer than the file size in bytes. The buffer we create
            // is at least as big as we need (maybe bigger).
            char [] buffer = new char[fileLengthInt];
            int amountRead = in.read(buffer, 0, fileLengthInt);
            int length = amountRead;

            while (amountRead != -1 && length < fileLengthInt) {
                amountRead = in.read(buffer, length, fileLengthInt - length);
                if (amountRead != -1)  length += amountRead;
            }

            if (length == -1)  return "";
            else  return new String(buffer, 0, length);
        }
        finally {
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(fin);
        }
    }

    /**
     * Will handle UTF-8 Strings properly. Note that for example a String
     * consisting of a single double-byte character (length of 1) will
     * end up being a file of 2 bytes.
     *
     * Has same behavior as
     * org.apache.commons.io.FileUtils.writeStringToFile() except always
     * uses UTF-8 encoding instead of the default encoding for the VM.
     *
     * @param theString  Must be non-null
     * @param file       Must be non-null
     */
    public static void stringToFile(String theString, File file)
        throws IOException
    {
        ArgUtil.notNull(theString, "theString");
        ArgUtil.notNull(file, "file");

        // Can't use FileWriter because it doesn't provide a way to set the
        // encoding.

        FileOutputStream fout = new FileOutputStream(file);
        OutputStreamWriter out = null;

        try {
            out = new OutputStreamWriter(fout, "UTF-8");
            out.write(theString);
        }
        finally {
            IOUtils.closeQuietly(out);
            IOUtils.closeQuietly(fout);
        }
    }

    /**
     * Will handle UTF-8 Strings properly. Note that for example a String
     * consisting of a single double-byte character (length of 1) will
     * end up being a file of 2 bytes.
     *
     * Has same behavior as
     * org.apache.commons.io.FileUtils.writeStringToFile() except always
     * uses UTF-8 encoding instead of the default encoding for the VM.
     *
     * @param theString  Must be non-null
     * @param fileName   Must be non-null and non-blank
     */
    public static void stringToFile(String theString, String fileName)
        throws IOException
    {
        ArgUtil.notNull(theString, "theString");
        ArgUtil.notBlank(fileName, "fileName");

    	stringToFile(theString, new File(fileName));
    }

    /**
     * The same as org.apache.commons.io.IOUtils.toString(InputStream)
     * except always uses UTF-8 encoding instead of the platform default
     * (which may be the same or different).
     *
     * @param in  Must be non-null
     */
    public static String toString(InputStream in) throws IOException {
    	return IOUtils.toString(in, "UTF-8");
    }

    /**
     * Convert the extension of the given fileName to lowercase
     * (if not already) and return the converted fileName (which
     * may be the same). File names without an extension or null
     * fileNames are returned as is.
     *
     * @param fileName  May be null
     */
    public static String lowerExtension(String fileName) {
        if (fileName == null)  return null;
        int index = fileName.lastIndexOf('.');
        if (index == -1)  return fileName;
        String ext = fileName.substring(index + 1);
        String rest = fileName.substring(0, index);
        return rest + "." + ext.toLowerCase();
    }

    /**
     * @param fill null causes null return value
     */
    public static String getNameWithoutExtension(File file) {
        if (file == null)  return null;
        return getNameWithoutExtension(file.getName());
    }

    /**
     * The returned name will include the path if it was part
     * of the input string.
     *
     * @param fileName  null causes null return value
     */
    public static String getNameWithoutExtension(String fileName) {
        if (fileName == null)  return null;

        int sepIndex = fileName.lastIndexOf('/');
        int sepIndex2 = fileName.lastIndexOf('\\');
        if (sepIndex2 > sepIndex)  sepIndex = sepIndex2;

        int index = fileName.lastIndexOf('.');
        if (index != -1 && index > sepIndex + 1) {
            return fileName.substring(0, index);
        }
        return fileName;
    }

    /**
     * Returns the input string with the file extension replaced by
     * the string given. Example: input ("name.txt", "zip") -> "name.zip"
     * If no period is found in the input file name then the output
     * will be the same as the input.
     * If the new extension given is null, then any existing extension
     * will be removed. (However if the extension given is the empty
     * string then the trailing period will remain.)
     *
     * @param fileName   If null then null is returned
     * @param newExtension  If null then extension is removed
     */
    public static String replaceExtension(String fileName,
        String newExtension)
    {
        if (fileName == null)  return null;

        String nameWithoutExtension = getNameWithoutExtension(fileName);
        if (newExtension == null) return nameWithoutExtension;
        else return nameWithoutExtension + "." + newExtension;
    }

    public static File replaceExtension(File file, String newExtension) {
        String fileName = file.getName();
        String newName = replaceExtension(fileName, newExtension);
        File newFile = new File(file.getParent(), newName);
        return newFile;
    }

    /**
     * Move file from one location to another, overwriting the target.
     * If the given target already exists and is a directory, the actual
     * target file is assumed to be a file of the same name in the target
     * directory.
     * If the target does not exist and has parent directories that do not
     * exist, the parent directories will be created.
     * The move is first attempted by using File.renameTo(). But this method
     * generally doesn't work when moving a file from one filesystem (drive)
     * to another. It also won't overwrite an existing file. So if renameTo()
     * doesn't work then the file will be moved by copying and then deleting
     * the source.
     * Returns true if everything went ok. Throws an IOException if there was
     * a problem copying the file. Returns false if the file was copied fine
     * but could not be deleted (generally due to file permissions).
     *
     * @param source  Must be non-null and a file (not directory)
     * @param target  Must be non-null
     */
    public static boolean moveFile(File source, File target)
        throws IOException
    {
        ArgUtil.notNull(source, "source");
        checkFile(source, true);
        ArgUtil.notNull(target, "target");

        if (target.isDirectory()) {
            target = new File(target, source.getName());
        }
        else {
            // It's a little redundant to create parent dirs here when
            // copyFile() will do the same but by doing it here it's
            // more likely that renameTo() will work and copyFile()
            // won't be needed.
            File parentFile = target.getParentFile();
            if (!parentFile.exists()) {
                boolean ok = parentFile.mkdirs();
                if (!ok) {
                    throw new IOException(
                        "Couldn't create parent directories for "
                            + target.getPath());
                }
            }
        }

        boolean ok = source.renameTo(target);

        if (ok)  return true;
        else {
            FileUtils.copyFile(source, target); // throws IOException
            return source.delete();
        }
    }

    /**
     * This method tends to be useful for incorporating into test programs.
     * Any existing file with the given name will be overwritten.
     *
     * @param fileName  Must be non-null and non-blank
     * @param bytes     Must be a non-negative number (0+)
     */
    public static void createFileOfSize(String fileName, long bytes)
        throws IOException
    {
        ArgUtil.notBlank(fileName, "fileName");
        ArgUtil.notLess0(bytes, "bytes");

        // We're going to use our own manual buffer because it's much faster
        // than using java.io.BufferedWriter.

        int bufSize = (int) Math.min(bytes, 8 * 1024);
        char [] buffer = new char[bufSize];
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = 'a';
        }

        FileWriter out = new FileWriter(fileName);  // throws IOException
        long bytesNeeded = bytes;

        while (bytesNeeded > 0) {
            int amount = (int) Math.min(bytesNeeded, buffer.length);
            out.write(buffer, 0, amount);  // throws IOException
            bytesNeeded -= amount;
        }

        out.close();  // throws IOException
    }

    /**
     * <p>
     * Somewhat similar to java.io.File.createTempFile() except creates a
     * directory. We already have the standard temp directory via the
     * system property "java.io.tmpdir" but the purpose of this method is to
     * create a directory inside that one.
     * Either you specify a complete name (instead of a prefix)
     * or specifying null will cause a completely random name to be generated.
     * The requested directory is always created as a subdirectory of the
     * system temp directory.
     * </p>
     * <p>An exception is thrown under one of three conditions:</p>
     * <ol>
     * <li>The requested directory cannot be created (and does not already
     * exist) (IOException will be thrown).
     * </li>
     * <li>The requested directory already exists, clear is true, but
     * the directory cannot be cleaned. (IOException will be thrown)
     * </li>
     * <li>The requested directory already exists, is not clean, and clean
     * is false. (RuntimeException will be thrown)</li>
     * </ol>
     *
     * @param name   Can be null but must not be blank
     * @param clean  True indicates that if the directory already exists it
     *               should be cleaned out (an exception will be thrown if
     *               it cannot be cleaned out). False indicates to throw an
     *               exception if it already exists and is not empty.
     * @return       The temporary directory
     */
    public static File createTempDirectory(String name, boolean clean)
        throws IOException
    {
        if (name == null) {
            // Don't use System.currentTimeMillis() because if this method
            // is called twice in a row it could still be the same time.
            Random random = new Random();
            // make sure the name is at least 5 digits long
            int i = random.nextInt(Integer.MAX_VALUE - 10000) + 10000;
            name = String.valueOf(i);
        }
        ArgUtil.notBlank(name, "name");

        File systemTempDir = SystemUtils.getJavaIoTmpDir();
        File tempDir = new File(systemTempDir, name);

        if (tempDir.exists()) {
            if (clean) {
                FileUtils.cleanDirectory(tempDir);  // throws IOException
            }
            else if (tempDir.listFiles().length > 0) {
                throw new RuntimeException("The directory " + tempDir.getPath()
                    + " already exits and is not empty.");
            }
        }
        else {
            boolean ok = tempDir.mkdir();
            if (!ok) {
                throw new IOException("Could not create the directory "
                    + tempDir.getPath());
            }
        }

        return tempDir;
    }

    /**
     * Returns true if the two given files have the same length and
     * the same MD5 checksum.
     *
     * @param file1  Must be non-null and an existing file
     * @param file2  Must be non-null and an existing file
     */
    public static boolean checksumCompare(File file1, File file2)
        throws IOException
    {
        ArgUtil.notNull(file1, "file1");
        ArgUtil.notNull(file2, "file2");

        if (file1.length() != file2.length())  return false;

        String sum1 = MessageDigestUtil.getDigest(file1, MessageDigestUtil.Algorithm.MD5);
            // throws IOException
        String sum2 = MessageDigestUtil.getDigest(file2, MessageDigestUtil.Algorithm.MD5);

        return sum1.equals(sum2);
    }

    /**
     * Returns true if the two given files have exactly the same contents
     * (compares every byte, but exits early if a difference is found).
     *
     * @param file1  Must be non-null and an existing file
     * @param file2  Must be non-null and an existing file
     */
    public static boolean binaryCompare(File file1, File file2)
        throws IOException
    {
        ArgUtil.notNull(file1, "file1");
        ArgUtil.notNull(file2, "file2");

        if (file1.length() != file2.length())  return false;

        FileInputStream f1 = null;
        FileInputStream f2 = null;
        BufferedInputStream in1 = null;
        BufferedInputStream in2 = null;

        try {
            // following 6 lines throw IOException
            f1 = new FileInputStream(file1);
            f2 = new FileInputStream(file2);
            in1 = new BufferedInputStream(f1);
            in2 = new BufferedInputStream(f2);

            int c1 = in1.read();
            int c2 = in2.read();

            while (true) {
                if (c1 != c2) {
                    return false;
                }
                if (c1 == -1)  return true;

                c1 = in1.read();
                c2 = in2.read();
            }
        }
        finally {
            IOUtils.closeQuietly(in1);
            IOUtils.closeQuietly(in2);
            IOUtils.closeQuietly(f1);
            IOUtils.closeQuietly(f2);
        }
    }
}
