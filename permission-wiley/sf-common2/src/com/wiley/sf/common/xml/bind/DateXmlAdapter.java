package com.wiley.sf.common.xml.bind;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.apache.commons.lang3.StringUtils;

/**
 * XmlAdapter for use with JAXB. Gives you the option of specifying
 * a dateFormat in SimpleDateFormat style, or else defaults to
 * converting a Date back and forth from Date.getTime().
 *
 * @since  JDK 1.6
 * @author smarkoff
 */
public class DateXmlAdapter extends XmlAdapter<String, Date> {

    private static String dateFormatString = null;

    /**
     * To be called by a Spring config, InitServlet,
     * or some other initialization mechanism.
     */
    public static void setDateFormatString(String dateFormatString) {
        DateXmlAdapter.dateFormatString = dateFormatString;
    }

    private SimpleDateFormat df = null;

    /**
     * Create an instance that uses the static dateFormatString.
     */
    public DateXmlAdapter() {
        if (StringUtils.isNotBlank(dateFormatString)) {
            df = new SimpleDateFormat(dateFormatString);
        }
    }

    /**
     * Create an instance that may be different from the default
     * as specified by the static dateFormatString.
     *
     * @param dateFormatString  May be null or blank
     */
    public DateXmlAdapter(String dateFormatString) {
        if (StringUtils.isNotBlank(dateFormatString)) {
            df = new SimpleDateFormat(dateFormatString);
        }
    }

    @Override
    public Date unmarshal(String date) throws Exception {
        if (df == null) {
            return new Date(Long.parseLong(date));
        }
        else return df.parse(date);
    }

    @Override
    public String marshal(Date date) throws Exception {
        if (df == null) {
            return String.valueOf(date.getTime());
        }
        else return df.format(date);
    }
}
