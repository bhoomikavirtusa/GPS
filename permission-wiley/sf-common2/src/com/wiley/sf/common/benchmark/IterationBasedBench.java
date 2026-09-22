package com.wiley.sf.common.benchmark;

/**
 * Base class for most classes that implement the Bench interface.
 *
 * @since   JDK 1.5
 * @version $Id: IterationBasedBench.java,v 1.2 2010-08-31 02:59:44 smarkoff Exp $
 * @author  Steve Markoff
 */
public abstract class IterationBasedBench implements Bench {

    /** Implements Bench interface. */
    public int bench() {
        int iterations = 0;
        long elapsedTime;

        long startTime = System.currentTimeMillis();
        while (true) {
            oneIteration();
            iterations++;
            elapsedTime = System.currentTimeMillis() - startTime;
            if (elapsedTime >= 10000) break;
        }
        double seconds = elapsedTime / 1000.0;
        int score = (int) Math.round(10 * iterations / seconds);

        System.out.println(String.valueOf(iterations) + " iterations in " + seconds + " seconds");

        return score;
    }

    public abstract void oneIteration();
}
