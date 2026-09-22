/*
 * &copy; John Wiley &amp; Sons, Inc
 */
package com.wiley.sf.common.lang;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.sf.common.io.ThreadedStreamReader;
import com.wiley.sf.common.io.ThreadedStreamWriter;

/**
 * Utility methods having to do with creating / running separate processes.
 *
 * @since JDK 1.5
 * @version $Id: ProcessUtil.java,v 1.13 2013-02-23 00:11:51 smarkoff Exp $
 */
public class ProcessUtil {

	private static final Log log = LogFactory.getLog(ProcessUtil.class);

	/**
	 * executes a command line program
	 * @param cmdline  Must be non-blank
	 * @param workingDir  May be null. Null means that the process created
     *                    will have the same working directory as the current
     *                    process.
     * @param input  May be null. If not null/empty, sent to standard input of the process.
     * @param timeoutSecs  If > 0, the process will be destroyed if it runs longer
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public static ProcessResult cmdExec(String cmdline, File workingDir,
	        String input, int timeoutSecs)
	    throws InterruptedException, IOException
	{
        log.debug ("cmdExec(): cmdLine = " + cmdline);
        ArgUtil.notBlank(cmdline, "cmdLine");
        long startTime = System.currentTimeMillis();
		Process p = Runtime.getRuntime().exec(cmdline, null, workingDir);
		    // throws IOException

		// following 3 lines each throw UnsupportedEncodingException
		ThreadedStreamWriter in = new ThreadedStreamWriter(p.getOutputStream(), input);
		    // if input is null/empty then thread will not be started
		ThreadedStreamReader out = new ThreadedStreamReader(p.getInputStream());
        ThreadedStreamReader err = new ThreadedStreamReader(p.getErrorStream());

        ProcessMonitor pm = new ProcessMonitor(p, timeoutSecs);
		p.waitFor();  // throws InterruptedException
		pm.processDone();

		// From testing, sometimes after the process is done, the stream
		// threads are still not quite done. Generally it only takes a
		// tiny bit more time for the stream threads to finish.

		final long waitTime = 50;  // milliseconds

		while (!in.isDone()) {
		    log.debug("cmdExec(): waiting for in");
		    try { Thread.sleep(waitTime); } catch (InterruptedException ex) { }
		}

	    while (!out.isDone()) {
	        log.debug("cmdExec(): waiting for out");
	        try { Thread.sleep(waitTime); } catch (InterruptedException ex) { }
	    }

	    while (!err.isDone()) {
	        log.debug("cmdExec(): waiting for err");
	        try { Thread.sleep(waitTime); } catch (InterruptedException ex) { }
	    }

	    long time = System.currentTimeMillis() - startTime;

		if (in.getException() != null) {
		    throw in.getException();  // IOException
		}

		if (out.getException() != null) {
		    throw out.getException();  // IOException
		}

		if (err.getException() != null) {
		    throw err.getException();  // IOException
		}

        return new ProcessResult(p.exitValue(), out.getString(), err.getString(),
            time, pm.destroyCalled(), getPid(p));
	}

    /**
     * Execute a java command.
     * For example given ("com.foo.MyClass arg1", 256, null, 0) will expand this to
     * "C:\jdk1.5.0_16\jre\bin\java.exe -cp <current classpath> -Xmx256m com.foo.MyClass arg1"
     * and execute the command in a separate process.
     *
     * @param command      Must be non-blank
     * @param workingDir   May be null. Null means that the process created
     *                     will have the same working directory as the current
     *                     process.
     * @param input        May be null. If not null/empty, sent to standard input of the process.
     * @param maxMemoryMB  0 means use default
     * @param propNames    May be null
     * @param debugPort    0 means don't enable remote debugging
     */
    public static ProcessResult javaCmdExec(String command, File workingDir,
            String input, int maxMemoryMB, String [] propNames, int debugPort)
        throws Exception
    {
        String javaHome = System.getProperty("java.home");
        // Won't use java.class.path - in Tomcat the context classpath is different.
        //String classpath = System.getProperty("java.class.path");

        String binPath = javaHome + File.separator + "bin" + File.separator;
        File java = new File(binPath + "java");
        File winJava = new File(binPath + "java.exe");
        if (!java.exists() && !winJava.exists()) {
            throw new RuntimeException("Unable to find java " +
                    "command, looked here [" + java + "] and " +
                            "here [" + winJava + "]");
        }

        StringBuilder sb = new StringBuilder();
        File javaToUse = java.exists() ? java : winJava;
        sb.append(javaToUse.getAbsolutePath());
        sb.append(" -cp ");
        sb.append(getContextClassPath());  // throws Exception
        sb.append(" ");
        if (maxMemoryMB > 0) {
            sb.append("-Xmx");
            sb.append(maxMemoryMB);
            sb.append("m ");
        }
        if (propNames != null) {
            for (int i = 0; i < propNames.length; i++) {
                String name = propNames[i];
                String value = System.getProperty(name);
                if (!StringUtils.isBlank(value)) {
                    sb.append("-D");
                    sb.append(name);
                    // Doing -Dname="value" on Unix at least causes
                    // the value to actually have quotes so don't use quotes
                    // (but what if the property value has spaces?).
                    sb.append("=");
                    sb.append(value);
                    sb.append(" ");
                }
            }
        }
        if (debugPort > 0) {
            sb.append("-Xdebug -Xrunjdwp:transport=dt_socket,address=");
            sb.append(debugPort);
            sb.append(",server=y,suspend=n ");
        }
        sb.append(command);

        // cmdExec() logs command and return code so I'm not going to do it again here
        return cmdExec(sb.toString(), workingDir, input, 0);
            // throws InterruptedException, IOException
    }

    /**
     * Similar to javaCmdExec but spawns a thread to issue the command.
     * The return code is logged but not returned for obvious reasons.
     * Any exception that happens in the thread will be logged.
     *
     * @param command
     */
    public static void javaCmdExecSpawn(String command, File workingDir,
            String input, int maxMemoryMB, String [] propNames, int debugPort) {
        new ProcessUtil().new ThreadedJavaCmdExec(command, workingDir, input,
            maxMemoryMB, propNames, debugPort).start();
    }

    /**
     * Execute a java command in a thread
     */
    class ThreadedJavaCmdExec extends Thread {
        private final String command;
        private final File workingDir;
        private final String input;
        private final int maxMemoryMB;
        private final String [] propNames;
        private final int debugPort;

        public ThreadedJavaCmdExec(String command, File workingDir, String input,
                int maxMemoryMB, String [] propNames, int debugPort) {
            this.command = command;
            this.workingDir = workingDir;
            this.input = input;
            this.maxMemoryMB = maxMemoryMB;
            this.propNames = propNames;
            this.debugPort = debugPort;
        }

        @Override
        public void run() {
            try {
                javaCmdExec(command, workingDir, input, maxMemoryMB, propNames, debugPort);
                    // ignore returned ProcessResult
            }
            catch (Exception e) {
                log.error("Exception calling Java command: ", e);
            }
        }
    }

    /**
     * In Tomcat, the context classpath is not the same as the system
     * property java.class.path, so this method provides a way to get
     * the context classpath.
     */
    public static String getContextClassPath() throws Exception {
        URLClassLoader loader =
            (URLClassLoader) Thread.currentThread().getContextClassLoader();
        StringBuilder classPath = new StringBuilder();

        // cast for null on next line avoids compile warning for JDK 1.5
        Method method = loader.getClass().getDeclaredMethod("getURLs", (Class[]) null);
            // throws NoSuchMethodException
        method.setAccessible(true);
        // cast for null on next line avoids compile warning for JDK 1.5
        URL[] urls = (URL[]) method.invoke(loader, (Object[]) null);
            // throws IllegalAccessException, InvocationTargetException
        for (int x = 0 ; x < urls.length ; x++) {
            if (! StringUtils.isBlank(urls[x].getFile()) &&
                    new File(urls[x].getFile()).exists()) {
                classPath.append(urls[x].getFile() + File.pathSeparator);
            }
        }

        return classPath.toString();
    }

    /**
     * Attempts to determine the pid (process id) of the current process.
     * From testing, this method works on Windows and Linux (other platforms not tested).
     * Null is returned if the pid cannot be determined.
     */
    public static String getPid() {
        String s = ManagementFactory.getRuntimeMXBean().getName();
        if (s == null)  return s;
        int index = s.indexOf('@');
        if (index == -1) return null;
        s = s.substring(0, index);
        return s;
    }

    /**
     * Since there is no Process.getPid() API (as of JDK 1.6),
     * attempts to find out the pid (process id) of the given Process.
     * From testing, this method does NOT work on Windows but does work
     * on Linux. Null is returned if the pid cannot be determined.
     *
     * @param process  Must be non-null
     */
    public static String getPid(Process process) {
        // On Windows XP with JDK 1.6, we get a NoSuchFieldException.
        // On Linux (RHEL 4.x) with JDK 1.6, this works.
        // On Solaris - ? (not tested).
        try {
            Field field = process.getClass().getDeclaredField("pid");
                // throws NoSuchFieldException
            field.setAccessible(true);  // throws SecurityException
            Object o = field.get(process);  // throws IllegalArgumentException, IllegalAccessException
            return String.valueOf(o);
        }
        catch (Exception ex) {
            //log.debug("Exception trying to get pid", ex);
            return null;
        }
    }

    /**
     * There is no reason to create an instance of this class so prevent
     * with a private constructor.
     */
    private ProcessUtil() {
        // do nothing
    }
}
