package com.wiley.sf.common.lang;

import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.Date;

import org.junit.Test;

/**
 * JUnit test class for DateUtil.
 *
 * @since   JDK 1.6, JUnit 4.10
 * @version $Id: DateUtilTest.java,v 1.2 2012-03-06 22:29:00 smarkoff Exp $
 * @author  Steve Markoff
 */
public class DateUtilTest {

    @Test
    public void expand2DigitYearTo4_Date() {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        cal.set(Calendar.YEAR, year % 100);
        Date date = DateUtil.expand2DigitYearTo4(cal.getTime(), 50, 50);
        cal.setTime(date);
        int result = cal.get(Calendar.YEAR);
        String msg = "expected year " + year + " but got " + result;
        assertTrue(msg, year == result);
    }

    @Test
    public void expand2DigitYearTo4() {
        // -- 50/50 window
        expand2DigitYearTo4(10, 2010, 2010, 50, 50);
        expand2DigitYearTo4(50, 2010, 2050, 50, 50);
        expand2DigitYearTo4(90, 2010, 1990, 50, 50);

        expand2DigitYearTo4(10, 2050, 2010, 50, 50);
        expand2DigitYearTo4(50, 2050, 2050, 50, 50);
        expand2DigitYearTo4(90, 2050, 2090, 50, 50);

        expand2DigitYearTo4(10, 2090, 2110, 50, 50);
        expand2DigitYearTo4(50, 2090, 2050, 50, 50);
        expand2DigitYearTo4(90, 2090, 2090, 50, 50);

        // -- 90/10 window
        expand2DigitYearTo4(10, 2010, 2010, 90, 10);
        expand2DigitYearTo4(50, 2010, 1950, 90, 10);
        expand2DigitYearTo4(90, 2010, 1990, 90, 10);

        expand2DigitYearTo4(10, 2050, 2010, 90, 10);
        expand2DigitYearTo4(50, 2050, 2050, 90, 10);
        expand2DigitYearTo4(90, 2050, 1990, 90, 10);

        expand2DigitYearTo4(10, 2090, 2010, 90, 10);
        expand2DigitYearTo4(50, 2090, 2050, 90, 10);
        expand2DigitYearTo4(90, 2090, 2090, 90, 10);

        // -- 10/90 window
        expand2DigitYearTo4(10, 2010, 2010, 10, 90);
        expand2DigitYearTo4(50, 2010, 2050, 10, 90);
        expand2DigitYearTo4(90, 2010, 2090, 10, 90);

        expand2DigitYearTo4(10, 2050, 2110, 10, 90);
        expand2DigitYearTo4(50, 2050, 2050, 10, 90);
        expand2DigitYearTo4(90, 2050, 2090, 10, 90);

        expand2DigitYearTo4(10, 2090, 2110, 10, 90);
        expand2DigitYearTo4(50, 2090, 2150, 10, 90);
        expand2DigitYearTo4(90, 2090, 2090, 10, 90);
    }

    private void expand2DigitYearTo4(int twoYear, int currentYear, int expectedYear, int windowBefore, int windowAfter) {
        int result = DateUtil.expand2DigitYearTo4(twoYear, currentYear, windowBefore, windowAfter);
        String msg = "expected " + expectedYear + " but result was " + result;
        assertTrue(msg, result == expectedYear);
    }
}
