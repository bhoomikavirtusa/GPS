package com.wiley.sf.common.io;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.wiley.sf.common.lang.ArgUtil;

/**
 * Intended usage:
 * 1) An instance of this class is created.
 * 2) The calling code is aware of when the output has been fully written
 * either by calling isDone() or some external mechanism.
 * 3) The calling code calls getException() to check for an error.
 *
 * @since   JDK 1.5
 * @version $Id: ThreadedStreamWriter.java,v 1.3 2013-02-23 00:14:59 smarkoff Exp $
 * @author  Steve Markoff
 */

public class ThreadedStreamWriter extends Thread {

    private final String output;
    private OutputStreamWriter osw;
    private IOException exception = null;
    private volatile boolean done = false;

    /**
     * Assumes the OutputStream is UTF-8 (or just plain ASCII).
     *
     * @param outputStream  Must be non-null
     * @param output  If null/empty this class does nothing
     */
    public ThreadedStreamWriter(OutputStream outputStream, String output)
        throws UnsupportedEncodingException
    {
        this.output = output;
        if (StringUtils.isEmpty(output)) {
            done = true;
            return;
        }
        ArgUtil.notNull(outputStream, "outputStream");
        osw = new OutputStreamWriter(outputStream, "UTF-8");
            // throws UnsupportedEncodingException
        start();
    }

    /**
     * Returns null unless there was an exception writing to the stream.
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
            osw.write(output);  // throws IOException
        }
        catch (IOException e) {
            exception = e;
        }
        finally {
            done = true;
            IOUtils.closeQuietly(osw);
        }
    }

}
