package com.wiley.sf.common.lucene;

import java.text.NumberFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Holder for basic information about an index.
 * See IndexReadManager.readIndexInfo().
 *
 * @since  JDK 1.6, Lucene 4.7
 * @author smarkoff
 */
public class IndexInfo
{
    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(IndexInfo.class);

    private final boolean indexExists;
    private final int numDocs;
    private final int numDeletedDocs;
    private final long version;
    private final int numFiles;
    private final long totalFileBytes;
    private final int numSegments;

    public IndexInfo(boolean indexExists, int numDocs, int numDeletedDocs,
        long version, int numFiles, long totalFileBytes, int numSegments)
    {
        this.indexExists = indexExists;
        this.numDocs = numDocs;
        this.numDeletedDocs = numDeletedDocs;
        this.version = version;
        this.numFiles = numFiles;
        this.totalFileBytes = totalFileBytes;
        this.numSegments = numSegments;
    }

    public boolean getIndexExists() {
        return indexExists;
    }

    public int getNumDocs() {
        return numDocs;
    }

    public int getNumDeletedDocs() {
        return numDeletedDocs;
    }

    public long getVersion() {
        return version;
    }

    public int getNumFiles() {
        return numFiles;
    }

    public long getTotalFileBytes() {
        return totalFileBytes;
    }

    public int getNumSegments() {
        return numSegments;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        final NumberFormat intFormat = NumberFormat.getIntegerInstance();
        final String CRLF = "\r\n";
        sb.append("numDocs = " + intFormat.format(numDocs)).append(CRLF);
        sb.append("numDeletedDocs = " + intFormat.format(numDeletedDocs)).append(CRLF);
        sb.append("version = " + version).append(CRLF);
        sb.append("numFiles = " + intFormat.format(numFiles)).append(CRLF);
        sb.append("totalFileBytes = " + intFormat.format(totalFileBytes)).append(CRLF);
        sb.append("numSegments = " + intFormat.format(numSegments)).append(CRLF);
        return sb.toString();
    }
}
