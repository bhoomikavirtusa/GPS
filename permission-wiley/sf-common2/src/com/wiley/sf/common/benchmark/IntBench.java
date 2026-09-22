package com.wiley.sf.common.benchmark;

import java.text.NumberFormat;

/**
 * Benchmarks integer performance.
 *
 * @since   JDK 1.5
 * @version $Id: IntBench.java,v 1.5 2010-08-31 03:04:22 smarkoff Exp $
 * @author  Steve Markoff
 */
public class IntBench extends IterationBasedBench {

    public IntBench() { }

    /** Implements Bench interface. */
    public boolean multipleThreadsOk() {
        return true;
    }

    /** Implements IterationBasedBench abstract method. */
    @Override
    public void oneIteration() {
        computeNumPrimesLessEqual(9000000);
    }

    // taken from http://www.cs.princeton.edu/introcs/14array/PrimeSieve.java.html
    // and modified a little
    public static int computeNumPrimesLessEqual(int n) {
        // initially assume all integers are prime
        boolean[] isPrime = new boolean[n + 1];
        for (int i = 2; i <= n; i++) {
            isPrime[i] = true;
        }

        // mark non-primes <= N using Sieve of Eratosthenes
        for (int i = 2; i*i <= n; i++) {

            // if i is prime, then mark multiples of i as nonprime
            // suffices to consider multiples i, i+1, ..., N/i
            if (isPrime[i]) {
                for (int j = i; i*j <= n; j++) {
                    isPrime[i*j] = false;
                }
            }
        }

        // count primes
        int count = 0;
        for (int i = 2; i <= n; i++) {
            if (isPrime[i]) {
                //if (i < 1000) System.out.println(i);
                count++;
            }
        }

        return count;
    }

    public static void main(String [] args) {
        int n = 10000000;
        if (args.length > 0) {
            n = Integer.parseInt(args[0]);
        }
        int result = computeNumPrimesLessEqual(n);
        NumberFormat intFormat = NumberFormat.getIntegerInstance();
        System.out.println("The number of primes <= " + intFormat.format(n)
            + " is " + intFormat.format(result));

        // --------------
        IntBench b = new IntBench();
        int score = b.bench();

        System.out.println("score = " + score);
    }
}
