package com.wiley.sf.common.monitor;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.text.NumberFormat;

/**
 * Convenience class to get memory statistics.
 * Note I think the most useful thing to know is the amount of memory
 * currently used (total - free).
 *
 * @since   JDK 1.6
 * @version $Id: MemoryStats.java,v 1.1 2010-06-11 01:37:47 smarkoff Exp $
 * @author  Steve Markoff
 */
public class MemoryStats
    implements Externalizable
{
    private static final long serialVersionUID = 1L;

    /**
     * Returns a string of the form 40 / 80 / 256 MB
     * where the numbers represent currently used / total / free
     * memory. (1 MB = 100,000 bytes)
     */
    public static String getCurrentAsString() {
        NumberFormat intFormat = NumberFormat.getIntegerInstance();

        Runtime runtime = Runtime.getRuntime();
        long total = runtime.totalMemory() / 1000000;
        long free = runtime.freeMemory() / 1000000;
        long max = runtime.maxMemory() / 1000000;
        long used = total - free;

        return intFormat.format(used) + " / "
            + intFormat.format(total) + " / "
            + intFormat.format(max) + " MB";
    }


    private long totalMemory;
    private long freeMemory;
    private long maxMemory;

    private NumberFormat intFormat = NumberFormat.getIntegerInstance();

    public MemoryStats() {
        setToCurrent();
    }

    public MemoryStats(long totalMemory, long freeMemory, long maxMemory) {
        this.totalMemory = totalMemory;
        this.freeMemory = freeMemory;
        this.maxMemory = maxMemory;
    }

    public long getTotalMemory() { return totalMemory; }
    public long getFreeMemory() { return freeMemory; }
    public long getMaxMemory() { return maxMemory; }

    public long getUsedMemory() {
        return totalMemory - freeMemory;
    }

    public void setToCurrent() {
        Runtime runtime = Runtime.getRuntime();
        totalMemory = runtime.totalMemory();
        freeMemory = runtime.freeMemory();
        maxMemory = runtime.maxMemory();
    }

    /**
     * Returns a string of the form 40 / 80 / 256 MB
     * where the numbers represent currently used / total / free
     * memory. (1 MB = 100,000 bytes)
     */
    public String getAsString() {
        return
            intFormat.format(getUsedMemory() / 1000000)
            + " / "
            + intFormat.format(totalMemory / 1000000)
            + " / "
            + intFormat.format(maxMemory / 1000000)
            + " MB";
    }

    /** Implements Externalizable interface. */
    public void writeExternal(ObjectOutput out)
        throws IOException
    {
        out.writeLong(totalMemory);
        out.writeLong(freeMemory);
        out.writeLong(maxMemory);
    }

    /** Implements Externalizable interface. */
    public void readExternal(ObjectInput in)
        throws IOException
    {
        totalMemory = in.readLong();
        freeMemory = in.readLong();
        maxMemory = in.readLong();
    }
}

