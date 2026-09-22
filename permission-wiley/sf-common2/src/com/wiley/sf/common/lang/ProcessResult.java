/*
 * &copy; John Wiley &amp; Sons, Inc
 */
package com.wiley.sf.common.lang;

import java.text.NumberFormat;

/**
 * Represents data collected from a process that has finished.
 * Includes the exitValue, the content from standard out, and the content
 * from standard error.
 * 
 * @since JDK 1.5
 * @author smarkoff, created 8/14/2008
 * @version $Id: ProcessResult.java,v 1.12 2008-08-28 19:20:05 smarkoff Exp $
 */
public class ProcessResult {
    
    private final int exitValue;
    private final String out;
    private final String error;
    private final long timeMS;
    private final boolean destroyCalled;
    private final String pid;
    
    /**
     * @param exitValue
     * @param out        Content from standard out
     * @param error      Content from standard error
     * @param timeMS     Time the process took to run in ms
     * @param destroyCalled  Whether or not the process had to be destroyed
     *                       because it was taking too long
     * @param pid        pid of the Process that ran, if available (may be null to indicate unknown)
     */
    public ProcessResult(int exitValue, String out, String error, long timeMS,
            boolean destroyCalled, String pid)
    {
        this.exitValue = exitValue;
        this.out = out;
        this.error = error;
        this.timeMS = timeMS;
        this.destroyCalled = destroyCalled;
        this.pid = pid;
    }
    
    public int getExitValue() { return exitValue; }
    
    public String getOut() { return out; }
    
    public String getError() { return error; }
    
    public long getTimeMS() { return timeMS; }
    
    public boolean destroyCalled() { return destroyCalled; }
    
    /** A return value of null means the pid is unknown. */
    public String getPid() { return pid; }
    
    @Override
    public String toString() {
        final String ls = StringUtil.getLineSeparator();
        
        return getSummaryString() + ls
            + "out: " + out + ls
            + "error: " + error;
    }
    
    public String getSummaryString() {
        // declare NumberFormat locally since not threadsafe
        final NumberFormat intFormat = NumberFormat.getIntegerInstance();
        
        final String ls = StringUtil.getLineSeparator();
        
        return "exitValue: " + exitValue + ls
        + "timeMS: " + intFormat.format(timeMS) + ls
        + "destroyCalled: " + destroyCalled + ls
        + "pid: " + pid + ls
        + "out length: " + intFormat.format(out.length()) + ls
        + "error length: " + intFormat.format(error.length());
    }
}
