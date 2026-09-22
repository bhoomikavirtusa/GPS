package com.wiley.sf.common.lang;

import org.junit.*;
//import static org.junit.Assert.*;

/**
 * JUnit test class for ProcessUtil.
 *
 * @since   JDK 1.5, JUnit 4.4
 * @version $Id: ProcessUtilTest.java,v 1.2 2008-12-15 14:14:22 smarkoff Exp $
 * @author  Steve Markoff
 */
public class ProcessUtilTest {

    @Test
    public void getPid() {
        // We have no good way to verify that the pid we get from getPid()
        // is correct (system dependent).

        System.out.println("unverified pid: " + ProcessUtil.getPid());

        // delay so have time to check actual PID on Windows using Task Manager
        // or Linux using ps command
        //try { Thread.sleep(10000); } catch (InterruptedException e) { }
    }
}
