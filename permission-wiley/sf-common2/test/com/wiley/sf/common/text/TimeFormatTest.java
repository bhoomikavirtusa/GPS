package com.wiley.sf.common.text;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * JUnit test class for TimeFormat.
 *
 * @since   JDK 1.6, JUnit 4.7
 * @version 9/11/2010
 * @author  Steve Markoff
 */
public class TimeFormatTest {
    private TimeFormat shortFormat = new TimeFormat();
    private TimeFormat longFormat = new TimeFormat(true);

    @Test
    public void formatSeconds() {
        formatSeconds(1, "0:01", false);
        formatSeconds(1, "1 sec", true);
        formatSeconds(20, "0:20", false);
        formatSeconds(20, "20 sec", true);
        formatSeconds(20 * 60, "20:00", false);
        formatSeconds(20 * 60, "20 min, 0 sec", true);
        formatSeconds(90 * 60, "1:30:00", false);
        formatSeconds(90 * 60, "1 hours, 30 min, 0 sec", true);
    }

    private void formatSeconds(int secs, String expected, boolean useLongFormat) {
        String result = useLongFormat ? longFormat.formatSeconds(secs) : shortFormat.formatSeconds(secs);
        String msg = "Result was " + result + " - expected " + expected;
        assertTrue(msg, expected.equals(result));
    }

    @Test
    public void formatMS() {
        formatMS(1000, false, "0:01", false);
        formatMS(1000, false, "1 sec", true);
        formatMS(1000, true, "0:01.000", false);
        formatMS(1000, true, "1 sec, 0 ms", true);
        formatMS(1500, true, "0:01.500", false);
        formatMS(1500, true, "1 sec, 500 ms", true);
        formatMS(1050, true, "0:01.050", false);
        formatMS(1050, true, "1 sec, 50 ms", true);
    }

    private void formatMS(long ms, boolean includeMS, String expected, boolean useLongFormat) {
        String result = useLongFormat ? longFormat.formatMS(ms, includeMS) : shortFormat.formatMS(ms, includeMS);
        String msg = "Result was " + result + " - expected " + expected;
        assertTrue(msg, expected.equals(result));
    }
}
