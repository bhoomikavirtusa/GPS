package com.wiley.sf.common.text;

/**
 * Provides formatting of relative amounts of time. While the Java JDK
 * provides java.text.DateFormat to format dates, it does not provide a class
 * to format a relative time such as a certain number of seconds.
 *
 * This class is threadsafe.
 *
 * @since    JDK 1.6
 * @version  9/11/2010
 * @author   Steve Markoff
 */
public class TimeFormat {

    private final boolean longFormat;

    /**
     * Same as TimeFormat(false).
     */
    public TimeFormat() {
        this.longFormat = false;
    }

    /**
     * Long format looks like "5 hours, 35 min, 21 sec[, 789 ms]".
     * Short format looks like "5:35:21[.789]".
     */
    public TimeFormat(boolean longFormat) {
        this.longFormat = longFormat;
    }

    /**
     * Given an amount of seconds, returns a string of the form
     * where the hours part is omitted if the amount of time is less than
     * one hour.
     * <pre>
     * Example input/output:
     * 1 -> 0:01 or "1 sec"
     * 20 -> 0:20 or "20 sec"
     * 20 * 60 -> 20:00 or "20 min, 0 sec"
     * 90 * 60 -> 1:30:00 or "1 hours, 30 min, 0 sec"
     * 25 * 60 * 60 -> 25:00:00 (this method is not really intended for a  time over 24 hours)
     * </pre>
     */
    public String formatSeconds(int seconds) {
        int min = seconds / 60;
        seconds = seconds % 60;
        String secString = String.valueOf(seconds);
        if (!longFormat && secString.length() == 1) secString = "0" + secString;
        if (min < 60) {
            if (longFormat) {
                if (min == 0) return secString + " sec";
                else return String.valueOf(min) + " min, " + secString + " sec";
            }
            else {
                return String.valueOf(min) + ":" + secString;
            }
        }
        else {
            int hours = min / 60;
            min = min % 60;
            String minString = String.valueOf(min);
            if (longFormat) {
                return String.valueOf(hours) + " hours, " + minString + " min, "
                    + secString + " sec";
            }
            else {
                if (minString.length() == 1) minString = "0" + minString;
                return String.valueOf(hours) + ":" + minString + ":" + secString;
            }
        }
    }

    /**
     * The output of this method is the same as calling
     * formatSeconds((int)(ms/1000)).
     */
    public String formatMS(long ms) {
        return formatMS(ms, false);
    }

    /**
     * When includeMS is true, the result has .000 tacked on the
     * end (or whatever the proper millisecond value is) for the short format
     * and 0 ms tacked on the end for the long format.
     */
    public String formatMS(long ms, boolean includeMS) {
        String result = formatSeconds((int) (ms / 1000));
        if (includeMS) {
            String fraction = String.valueOf(ms % 1000);
            if (longFormat) {
                result += ", " + fraction + " ms";
            }
            else {
                if (fraction.length() < 3) fraction = "0" + fraction;
                if (fraction.length() < 3) fraction = "0" + fraction;
                result += "." + fraction;
            }
        }
        return result;
    }
}
