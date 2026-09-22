package com.wiley.sf.common.io;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.commons.io.IOUtils;

import com.wiley.sf.common.lang.ArgUtil;

/**
 * Intended usage:
 * 1) An instance of this class is created.
 * 2) The calling code is aware of when the stream has been fully read
 * either by calling isDone() or some external mechanism.
 * 3) The calling code calls getException() to check for an error.
 * 4) Call getString() to get the contents of the stream.
 *
 * @since   JDK 1.5
 * @version $Id: ThreadedStreamReader.java,v 1.3 2008-08-15 15:09:36 smarkoff Exp $
 * @author  Steve Markoff
 */

public class ThreadedStreamReader extends Thread {

    private final char [] buffer = new char[4000];
    private final InputStreamReader isr;
    private final StringBuilder sb = new StringBuilder();
    private IOException exception = null;
    private volatile boolean done = false;

    /**
     * Assumes the InputStream is UTF-8 (or just plain ASCII).
     * 
     * @param inputStream  Must be non-null
     */
    public ThreadedStreamReader(InputStream inputStream)
        throws UnsupportedEncodingException
    {
        ArgUtil.notNull(inputStream, "inputStream");
        isr = new InputStreamReader(inputStream, "UTF-8");
            // throws UnsupportedEncodingException
        start();
    }

    /**
     * Returns null if !isDone(), otherwise always returns a non-null String
     * (but may be empty).
     */
    public String getString() {
        if (done)  return sb.toString();
        else  return null;
    }

    /**
     * Returns null unless there was an exception reading the stream.
     */
    public IOException getException() {
        return exception;
    }

    /**
     * Note generally calling !isAlive() (inherited from Thread) will have
     * the same result as calling this method, but this method is
     * preferred because there will be a split-second period of time
     * after calling the constructor where isAlive() will be false
     * but the Thread has not done it's work yet.
     */
    public boolean isDone() {
        return done;
    }

    /** Override method from super class. */
    @Override
    public void run() {
        try {
            runInner();
        }
        catch (IOException e) {
            exception = e;
        }
        finally {
            done = true;
            IOUtils.closeQuietly(isr);
        }
    }

    private void runInner() throws IOException {
        int amount = isr.read(buffer, 0, buffer.length);
            // throws IOException
        while (amount != -1) {
            sb.append(buffer, 0, amount);
            amount = isr.read(buffer, 0, buffer.length);
                // throws IOException
        }
    }

}
