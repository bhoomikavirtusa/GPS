package com.wiley.sf.common.monitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.sf.common.lang.ArgUtil;

/**
 * This class is threadsafe.
 *
 * @since JDK 1.6
 * @version $Id: PerformanceMonitor.java,v 1.10 2013-08-14 18:36:14 smarkoff Exp $
 * @author smarkoff
 */
public class PerformanceMonitor
{
	@SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(PerformanceMonitor.class);

	private static PerformanceMonitor instance = new PerformanceMonitor();

	public static PerformanceMonitor getInstance() {
	    return instance;
	}


	private final Map<String, PerfStat> statMap = new HashMap<String, PerfStat>();


	private PerformanceMonitor() {

	}

	/**
	 * The returned list is NOT sorted in any particular order.
	 * (You may sort it yourself.)
	 * The returned list will not be null but may have zero elements.
	 */
	public synchronized List<PerfStatSnapshot> getStatSnapshot() {
	    ArrayList<PerfStatSnapshot> list = new ArrayList<PerfStatSnapshot>();
        for (String key : statMap.keySet()) {
            PerfStat stat = statMap.get(key);
            list.add(new PerfStatSnapshot(key, stat.getCount(), stat.getAvg(), stat.getMin(), stat.getMax()));
        }
	    return list;
	}

	/**
	 * @param key  Must be non-blank
	 */
	public PerfTimer startTimer(String key) {
	    ArgUtil.notBlank(key, "key");
		return new PerfTimer(this, key);
	}

	/**
	 * Normally you don't want to call this method.
	 * Use startTimer() / PerfTimer.stopTimer() instead.
	 *
	 * @param key  Must be non-blank
	 */
	public synchronized void addTime(String key, long time) {
	    ArgUtil.notBlank(key, "key");
	    PerfStat stat = statMap.get(key);
	    if (stat == null) {
	        stat = new PerfStat();
	        statMap.put(key, stat);
	    }
	    stat.addValue(time);
	}

	/**
	 * Simple plain text format of stats. Sorts by key.
	 * You might consider calling getStatSnapshot() and writing your own display
	 * method to format the stats in something other than plain text.
	 */
	public String dumpStats() {
	    StringBuilder sb = new StringBuilder();
	    List<PerfStatSnapshot> list = getStatSnapshot();
	    Collections.sort(list);
	    for (PerfStatSnapshot snapshot : list) {
	        sb.append(snapshot).append("\r\n");
	    }
	    return sb.toString();
	}

	//
	// inner class
	//

	public class PerfTimer {
		private static final long serialVersionUID = 1L;

		private final PerformanceMonitor monitor;
		private final String key;
		private final long startTime = System.currentTimeMillis();


		public PerfTimer(PerformanceMonitor monitor, String key) {
		    this.monitor = monitor;
			this.key = key;
		}

		/**
		 * The time is added to the stats.
		 * If you also want to do something else with the time
		 * you can look at the return value.
		 *
		 * @return elapsed time in milliseconds
		 */
		public long stopTimer() {
		    long time = System.currentTimeMillis() - startTime;
		    monitor.addTime(key, time);
		    return time;
		}
	}

	//
	// inner class
	//

	public class PerfStat {
		private static final long serialVersionUID = 1L;

		private long count = 0;
		private long total = 0;
		private long min = -1;
		private long max = 0;

		public PerfStat() {
		}

		public long getCount() {
			return count;
		}

		public long getTotal() {
		    return total;
		}

		public long getMin() {
			return min;
		}

		public long getMax() {
			return max;
		}

		public long getAvg() {
			return total / count;
		}

		public void addValue(long newVal) {
			if (newVal < 0) {
				return;
			}

			if (newVal < min || min == -1) {
				min = newVal;
			}

			if (newVal > max) {
				max = newVal;
			}

			total += newVal;
			count++;
		}
	}
}
