/*
 * &copy; John Wiley &amp; Sons, Inc
 */
package com.wiley.sf.common.lang;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Monitors a Process and destroys it if the Process runs too long.
 * Intended usage: Some other class creates a Process and creates
 * an instance of this class to monitor it. The other class must
 * call processDone() when it knows the process is done, otherwise
 * this class will destroy the process when the timeout is up.
 * 
 * Generally the other class logic will look like this:
 *   ProcessMonitor pm = new ProcessMonitor(p, timeoutSecs);
 *   p.waitFor();
 *   pm.processDone();
 *   System.out.println("destroyCalled: " + pm.destroyCalled());
 * 
 * @since JDK 1.5
 * @author smarkoff, created 8/25/2008
 * @version $Id: ProcessMonitor.java,v 1.2 2008-08-26 17:16:02 smarkoff Exp $
 */
public class ProcessMonitor extends Thread {

	private static final Log log = LogFactory.getLog(ProcessMonitor.class);
    
	private final Process process;
	private final int timeoutSecs;
	private int sleptSecs = 0;
	private volatile boolean done = false;
	private boolean destroyed = false;
	
	/**
	 * @param p  Must be non-null
	 * @param timeoutSecs  If < 1, the created instance will do nothing
	 */
	public ProcessMonitor(Process p, int timeoutSecs) {
	    this.process = p;
	    this.timeoutSecs = timeoutSecs;
	    if (timeoutSecs > 0)  start();
	}
	
	public void processDone() {
	    done = true;
	}
	
	public void run() {
	    while (sleptSecs < timeoutSecs) {
	        try { Thread.sleep(1000); }
	        catch (InterruptedException ex) { }
	        
	        sleptSecs++;
	        if (done)  break;
	    }

	    if (!done) {
	        log.warn("process not done after " + timeoutSecs + " seconds. Destroying...");
	        process.destroy();
	        destroyed = true;
	    }
	}

	public boolean destroyCalled() {
	    return destroyed;
	}
}
