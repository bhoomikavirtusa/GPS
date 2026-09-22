package com.wiley.sf.common.benchmark;

import java.text.NumberFormat;


/**
 * Benchmarks integer performance.
 *
 * @since   JDK 1.5
 * @version $Id: MemoryBench.java,v 1.2 2010-08-31 03:05:50 smarkoff Exp $
 * @author  Steve Markoff
 */
public class MemoryBench extends IterationBasedBench {

    private final int outerCount;
    private final int innerCount;

    private int outerSquare;
    private int innerSquare;

    private final NumberFormat intFormat = NumberFormat.getIntegerInstance();


    public MemoryBench() {
        this(5, 5, 5, 5);
    }

    public MemoryBench(int outerCount, int innerCount,
        int outerMB, int innerMB)
    {
        this.outerCount = outerCount;
        this.innerCount = innerCount;

        // represent as number of 32-bit ints, so divide bytes by 4
        outerSquare = (int) Math.round(Math.sqrt(outerMB * 1000 * 1000 / 4));
        innerSquare = (int) Math.round(Math.sqrt(innerMB * 1000 * 1000 / 4));

        // adjust values of outerMB and innerMB to print out
        outerMB = outerSquare * outerSquare * 4 / 1000 / 1000;
        innerMB = innerSquare * innerSquare * 4 / 1000 / 1000;

        int totalMBAtOnce = outerMB + innerMB;
        int totalMB = outerCount * (outerMB + (innerMB * innerCount));

        int totalCalls = outerCount * (1 + outerSquare)
            + outerCount * (innerCount * (1 + innerSquare));

        System.out.println("The most memory ever allocated at once (that can");
        System.out.println("not be collected) = "
          + intFormat.format(totalMBAtOnce) + " MB");
        System.out.println("total memory allocated (not at once) = "
          + intFormat.format(totalMB) + " MB");
        System.out.println("total calls to new = " + intFormat.format(totalCalls));
    }

    /*
    public Benchable duplicate() {
        return new MemoryBench(outerCount, innerCount, outerMB, innerMB);
    }
    */

    /** Implements Bench interface. */
    public boolean multipleThreadsOk() {
        return true;
    }

    /** Implements IterationBasedBench abstract method. */
    @Override
    public void oneIteration() {
        for (int o = 0; o < outerCount; o++) {
            int [][] outerArray = new int[outerSquare][];
            for (int m = 0; m < outerArray.length; m++) {
                outerArray[m] = new int[outerSquare];
            }

            for (int i = 0; i < innerCount; i++) {
                int [][] innerArray = new int[innerSquare][];
                for (int m = 0; m < innerArray.length; m++) {
                    innerArray[m] = new int[innerSquare];
                }
            }
        }
    }


    public static void main(String [] args) {
        MemoryBench b = new MemoryBench();
        int score = b.bench();

        System.out.println("score = " + score);
    }
}
