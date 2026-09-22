package com.wiley.sf.common.lang;

import java.util.Calendar;
import java.util.Date;

/**
 * This class contains various methods for dealing with dates that
 * are not provided by the standard JDK.
 *
 * @version $Id: DateUtil.java,v 1.1 2012-03-06 21:13:20 smarkoff Exp $
 * @since   JDK 1.6
 * @author  Steve Markoff
 */
public class DateUtil {

    /**
     * Uses a 50/50 year window to determine the 4 digit year.
     * See unit test for example input/output.
     * If the given date has a year greater than 99, the date is returned unmodified.
     *
     * Note java.text.SimpleDateFormat has similar functionality but only with
     * a strict 2-digit year format and a fixed 80/20 window.
     *
     * @param date  If null, then null is returned
     */
    public static Date expand2DigitYearTo4(Date date) {
        return expand2DigitYearTo4(date, 50, 50);
    }

    /**
     * Uses a window to determine the 4 digit year.
     * See unit test for example input/output.
     * If the given date has a year greater than 99, the date is returned unmodified.
     *
     * Note java.text.SimpleDateFormat has similar functionality but only with
     * a strict 2-digit year format and a fixed 80/20 window.
     *
     * windowBefore + windowAfter must add up to 100
     *
     * @param date  If null, then null is returned
     * @param windowBefore  Must be between 0 and 100
     * @param windowAfter  Must be between 0 and 100
     */
    public static Date expand2DigitYearTo4(Date date, int windowBefore, int windowAfter) {
        if (date == null)  return null;
        Calendar cal = Calendar.getInstance();
        int currentYear = cal.get(Calendar.YEAR);
        cal.setTime(date);
        int year = cal.get(Calendar.YEAR);
        if (year > 99) return date;
        int newYear = expand2DigitYearTo4(year, currentYear, windowBefore, windowAfter);
        cal.set(Calendar.YEAR, newYear);
        return cal.getTime();
    }

    /**
     * Uses a 50/50 year window to determine the 4 digit year.
     * See unit test for example input/output.
     *
     * windowBefore + windowAfter must add up to 100
     *
     * @param year  Must be between 0 and 99
     * @param currentYear  Must be at least 200
     * @param windowBefore  Must be between 0 and 100
     * @param windowAfter  Must be between 0 and 100
     */
    public static int expand2DigitYearTo4(int year, int currentYear, int windowBefore, int windowAfter) {
        ArgUtil.inRange(year, "year", 0, 99);
        ArgUtil.inRange(currentYear, "currentYear", 200, Integer.MAX_VALUE);
        ArgUtil.inRange(windowBefore, "windowBefore", 0, 100);
        ArgUtil.inRange(windowAfter, "windowAfter", 0, 100);
        int windowSize = windowBefore + windowAfter;
        if (windowSize != 100) {
            throw new IllegalArgumentException("windowBefore + windowAfter must be 100! windowBefore = "
                + windowBefore + " windowAfter = " + windowAfter);
        }
        int minYear = currentYear - windowBefore;
        int maxYear = currentYear + windowAfter;
        int century1 = truncateToCentury(minYear);
        int century2 = truncateToCentury(maxYear);
        int choice1 = year + century1;
        int choice2 = year + century2;
        return choice1 <= maxYear && choice1 >= minYear ? choice1 : choice2;
    }

    private static final int truncateToCentury(int year) {
        return year - (year % 100);
    }

    /**
     * There is no reason to create an instance of this class since all methods
     * are static.
     */
    private DateUtil() { }
}
