package com.wiley.sf.common.benchmark;


/**
 * Bench interface.
 *
 * @since   JDK 1.5
 * @version $Id: Bench.java,v 1.3 2010-08-31 02:04:12 smarkoff Exp $
 * @author  Steve Markoff
 */
public interface Bench {

    /**
     * Returns benchmark score.
     */
    public int bench() throws Exception;

    /**
     * It is ok or reasonable to have multiple threads running the benchmark?
     * @return
     */
    public boolean multipleThreadsOk();
}
