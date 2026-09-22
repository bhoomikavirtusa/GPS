package com.wiley.sf.common.benchmark;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;

/**
 * Benchmarks file IO (writing and reading).
 *
 * @since   JDK 1.5
 * @version $Id: FileIOBench.java,v 1.5 2010-08-31 02:05:25 smarkoff Exp $
 * @author  Steve Markoff
 */
public class FileIOBench implements Bench {

    // ----------------------------- static -----------------------------------

    private final static int MAX_BLOCK_SIZE = 8 * 1024;

    private final static int DEFAULT_NUM_FILES = 1000;
    private final static int DEFAULT_FILE_KB = 500;

    private final static NumberFormat intFormat = NumberFormat.getIntegerInstance();


    public static void main(String [] args)
        throws IOException
    {
        int numFiles = DEFAULT_NUM_FILES;
        int fileSizeKB = DEFAULT_FILE_KB;
        String dir = null;

        if (args.length > 0) {
            numFiles = Integer.parseInt(args[0]);
        }

        if (args.length > 1) {
            fileSizeKB = Integer.parseInt(args[1]);
        }

        if (args.length > 2) {
            dir = args[2];
        }

        if (args.length < 3) {
            System.err.println("usage: [numFiles] [fileSizeKB] [directory]");
            System.err.println("Using values: " + numFiles
                + " " + fileSizeKB + " {current directory}");
        }

        FileIOBench bench = new FileIOBench(numFiles, fileSizeKB, dir);
        bench.bench();  // throws IOException
    }


    // ----------------------------- instance ---------------------------------

    private final int numFiles;
    private final long fileSizeBytes;
    private final int blockSize;
    private final String block;
    private final char [] readBuf;
    private final File dir;
    private final File [] fileArray;

    /*
     * @param dirString   Null indicates to use the current directory,
     *                    otherwise must represent an existing directory that is
     *                    readable and writable
     */
    public FileIOBench(String dirString) {
        this(DEFAULT_NUM_FILES, DEFAULT_FILE_KB, dirString);
    }

    /**
     * @param numFiles    Must be between 1 and 1 million
     * @param fileSizeKB  Must be between 1 and 100 million
     *                    (100 million = 100 GB)
     * @param dirString   Null indicates to use the current directory,
     *                    otherwise must represent an existing directory that is
     *                    readable and writable
     */
    public FileIOBench(int numFiles, int fileSizeKB, String dirString) {
        if (numFiles < 1 || numFiles > 1000000) {
            throw new IllegalArgumentException(
                "numFiles must be between 1 and 1 million");
        }

        if (fileSizeKB < 1 || fileSizeKB > 100000000) {
            throw new IllegalArgumentException(
                "fileSizeKB must be between 1 and 100 million");
        }

        this.numFiles = numFiles;
        this.fileSizeBytes = ((long)fileSizeKB) * 1000;
        this.blockSize = (int) Math.min(fileSizeBytes, MAX_BLOCK_SIZE);
        this.block = createBlock();
        this.readBuf = new char[blockSize];
        this.dir = (dirString == null) ? new File(".") : new File(dirString);

        if (!dir.isDirectory()) {
            throw new IllegalArgumentException(dir.getPath() + " is not a directory!");
        }

        if (!dir.canRead()) {
            throw new IllegalArgumentException(dir.getPath() + " is not readable!");
        }

        if (!dir.canWrite()) {
            throw new IllegalArgumentException(dir.getPath() + " is not writable!");
        }

        fileArray = createFileArray();
    }

    /** Implements Bench interface. */
    public int bench()
        throws IOException
    {
        long writeBytesPerSec = writeFiles();  // throws IOException
        dirListing();
        long readBytesPerSec = readFiles();  // throws IOException
        deleteFiles();

        long combined = writeBytesPerSec + readBytesPerSec;
        int score = (int) (combined / 1000000);
        return score;
    }

    /** Implements Bench interface. */
    public boolean multipleThreadsOk() {
        return false;
    }

    private long writeFiles()
        throws IOException
    {
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < fileArray.length; i++) {
            writeFile(fileArray[i]);  // throws IOException
        }

        long time = System.currentTimeMillis() - startTime;
        System.out.println("Write time = " + intFormat.format(time) + " ms.");

        if (time == 0)  time = 1;  // to avoid divide by zero below
        long totalBytesWritten = fileSizeBytes * numFiles;
        double bytesPerMS = ((double) totalBytesWritten) / time;
        long bytesPerSec = (long) (bytesPerMS * 1000);

        System.out.println("Total bytes written = " + intFormat.format(totalBytesWritten));
        System.out.println("Write bytes per second = " + intFormat.format(bytesPerSec));

        return bytesPerSec;
    }

    private void writeFile(File file)
        throws IOException
    {
        FileWriter writer = new FileWriter(file);
        int numBlocks = (int) (fileSizeBytes / blockSize);

        for (int i = 0; i < numBlocks; i++) {
            writer.write(block);
        }

        writer.close();
    }

    private long readFiles()
        throws IOException
    {
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < fileArray.length; i++) {
            readFile(fileArray[i]);  // throws IOException
        }

        long time = System.currentTimeMillis() - startTime;
        System.out.println("Read time = " + intFormat.format(time) + " ms.");

        if (time == 0)  time = 1;  // to avoid divide by zero below
        long totalBytesRead = fileSizeBytes * numFiles;
        double bytesPerMS = ((double) totalBytesRead) / time;
        long bytesPerSec = (long) (bytesPerMS * 1000);

        System.out.println("Total bytes read = " + intFormat.format(totalBytesRead));
        System.out.println("Read bytes per second = " + intFormat.format(bytesPerSec));

        return bytesPerSec;
    }

    private void readFile(File file)
        throws IOException
    {
        FileReader reader = new FileReader(file);
        int amountRead = reader.read(readBuf);
        while (amountRead != -1) {
            amountRead = reader.read(readBuf);
        }
        reader.close();
    }

    private void dirListing() {
        long startTime = System.currentTimeMillis();
        File [] fa = dir.listFiles();
        long time = System.currentTimeMillis() - startTime;

        System.out.println("File listing took " + intFormat.format(time) + " ms.");
        System.out.println("There were " + intFormat.format(fa.length)
            + " files in the directory.");
    }

    private void deleteFiles() {
        for (int i = 0; i < fileArray.length; i++) {
            boolean ok = fileArray[i].delete();
            if (!ok) {
                System.out.println(fileArray[i].getPath() + " could not be deleted.");
            }
        }
    }

    private File [] createFileArray() {
        File [] fileArray = new File[numFiles];
        for (int i = 0; i < numFiles; i++) {
            fileArray[i] = new File(dir, String.valueOf(i) + ".txt");
        }
        return fileArray;
    }

    private String createBlock() {
        StringBuilder sb = new StringBuilder(blockSize);

        for (int i = 0; i < blockSize; i++) {
            sb.append(String.valueOf(i % 10));
        }

        return sb.toString();
    }
}
