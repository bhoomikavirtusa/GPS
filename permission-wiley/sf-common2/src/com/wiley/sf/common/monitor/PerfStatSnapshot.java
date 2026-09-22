package com.wiley.sf.common.monitor;

import java.beans.ConstructorProperties;
import java.io.Serializable;

import com.wiley.sf.common.lang.ArgUtil;

/**
 * This class is returned by PerformanceMonitor.getSnapshot().
 * This class is immutable and threadsafe.
 * (Don't make this class an inner class of PerformanceMonitor because would be
 * more expensive to serialize since inner classes contain an implicit reference
 * to their outer class.)
 *
 * @since JDK 1.6
 * @version $Id: PerfStatSnapshot.java,v 1.2 2013-05-30 22:38:45 smarkoff Exp $
 * @author smarkoff
 */
public class PerfStatSnapshot implements Serializable, Comparable<PerfStatSnapshot> {

    private static final long serialVersionUID = 1L;

    private final String key;
    private final long count;
    private final long avg;
    private final long min;
    private final long max;

    /**
     * @param key  Must be non-blank
     * @param count
     * @param avg
     * @param min
     * @param max
     */
    @ConstructorProperties({"key", "count", "avg", "min", "max"})
    public PerfStatSnapshot(String key, long count, long avg, long min, long max) {
        ArgUtil.notBlank(key, "key");
        this.key = key;
        this.count = count;
        this.avg = avg;
        this.min = min;
        this.max = max;
    }

    public String getKey() {
        return key;
    }

    public long getCount() {
        return count;
    }

    public long getAvg() {
        return avg;
    }

    public long getMin() {
        return min;
    }

    public long getMax() {
        return max;
    }

    @Override
    public String toString() {
        return key + ": count = " + count + ", avg = " + avg + ", min = "
            + min + ", max = " + max;
    }

    /**
     * Compare by key.
     */
    public int compareTo(PerfStatSnapshot o) {
        return key.compareTo(o.key);
    }
}