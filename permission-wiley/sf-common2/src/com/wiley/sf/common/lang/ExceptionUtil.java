/*
 * &copy; John Wiley &amp; Sons, Inc
 */
package com.wiley.sf.common.lang;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Provides utility methods to deal with Exceptions.
 *
 * @since   JDK 1.5
 * @version $Id: ExceptionUtil.java,v 1.1 2008-07-08 17:14:22 smarkoff Exp $
 * @author  Steve Markoff
 */
public class ExceptionUtil {

    private final static String lineSep = StringUtil.getLineSeparator();

    /**
     * Returns the stack trace of the Throwable input, as a String.
     *
     * @param throwable  Must be non-null
     *
     * @see java.lang.Throwable#printStackTrace(PrintWriter)
     */
    public static String getStackTrace(Throwable throwable) {
        final StringWriter stringWriter = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(stringWriter);
        throwable.printStackTrace(printWriter);
        printWriter.close();
        return stringWriter.toString();
    }

    /**
     * Returns the combined messages of the given Throwable input and
     * any/all root causes. Does not print stack traces - if you want
     * a stack trace then use the getStrackTrace method (and the stack
     * trace will contain root causes).
     *
     * @param throwable  If null then the empty string is returned
     */
    public static String getCombinedTree(Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        int level = 0;

        while (throwable != null) {
            if (level > 0)  sb.append(lineSep);
            sb.append("---- cause (level " + level + "): ");
            sb.append(throwable.getClass().getName());
            sb.append(": ");
            sb.append(throwable.getMessage());
            throwable = throwable.getCause();
            level++;
        }

        return sb.toString();
    }

    /**
     * Searches for an Exception of the Class specified in the "cause"
     * tree of the specified Exception. Returns the appropriate Exception
     * if found, otherwise returns null.
     *
     * @param e  If null then null returned
     * @param c  Should be non-null
     */
    public static Throwable findException(Throwable ex, Class<?> clazz) {
        if (ex == null)  return null;
        else if (clazz.isInstance(ex))  return ex;
        else return findException(ex.getCause(), clazz);
    }

    /**
     * There is no reason to create an instance of this class since all methods
     * are static.
     */
    private ExceptionUtil() { }
}
