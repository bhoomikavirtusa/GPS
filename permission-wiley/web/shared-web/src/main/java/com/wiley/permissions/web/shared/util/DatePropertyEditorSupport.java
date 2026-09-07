package com.wiley.permissions.web.shared.util;

import java.beans.PropertyEditorSupport;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.sf.common.lang.DateUtil;

/**
 * Unlike the Spring class CustomDateEditor, this class handles multiple Date formats.
 *
 * @author smarkoff
 */
public class DatePropertyEditorSupport
extends PropertyEditorSupport
{
	@SuppressWarnings("unused")
	private final static Log log = LogFactory.getLog(DatePropertyEditorSupport.class);

	// MM/dd/yyyy - is the format used by Date picker widget so we at least need to handle
	// this format.
	// Note this class will be called for any java.util.Date that is part of a Form, even
	// if the date is never shown on the UI.
	// Timezone is probably unnecessary since it will not change
	// Don't use static SimpleDateFormat instances because it's not threadsafe.
	private final static String [] DATE_FORMATS = {
		"MM/dd/yyyy", "yyyy-dd-MM", "dd.MM.yyyy"
	};
	private final static String TIME_FORMAT = "HH:mm:ss";

	@Override
	public String getAsText() {
		Date date = (Date) getValue();
		if (date == null)  return "";
		else {
			//Calendar cal = Calendar.getInstance();
			//cal.setTime(date);
			//SimpleDateFormat dateFormat;

			//if (cal.get(Calendar.SECOND) == 0 && cal.get(Calendar.MINUTE) == 0 && cal.get(Calendar.HOUR_OF_DAY) == 0) {
			//	dateFormat = new SimpleDateFormat(DATE_FORMATS[0]);
			//}
			//else {
			//	dateFormat = new SimpleDateFormat(DATE_FORMATS[0] + " " + TIME_FORMAT);
			//}
			SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMATS[0]);

			String output = dateFormat.format(date);
			//log.debug("getAsText(): returning [" + output + "]");
			return output;
		}
	}

	@Override
	public void setAsText(String text)
	    throws IllegalArgumentException
	{
		//log.debug("setAsText() called with [" + text + "]");
		if (StringUtils.isBlank(text)) {
			setValue(null);
			return;
		}

		text = text.trim();
		boolean hasTime = text.length() > 10;
		Date date = null;

		for (String format : DATE_FORMATS) {
			SimpleDateFormat dateFormat;
			if (hasTime) {
				dateFormat = new SimpleDateFormat(format + " " + TIME_FORMAT);
			}
			else {
				dateFormat = new SimpleDateFormat(format);
			}

			try {
				date = dateFormat.parse(text);  // throws ParseException
				date = DateUtil.expand2DigitYearTo4(date, 40, 60);  // important to fix year before addTimeIfToday()
				// Time is now added on the server side in ExtendedAuditBase so this call is not necessary.
				//if (!hasTime) {
				//	date = addTime(date);
				//}

				break;
			}
			catch (ParseException ex) {
				continue;
			}
		}

		if (date == null) {
			throw new IllegalArgumentException("Invalid date: " + text);
		}

		setValue(date);
	}

	/*
	private Date addTime(Date date) {
		// If time was not included but the date is today, add the current time
		Calendar today = Calendar.getInstance();
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		//log.debug("cal year month day = " + year + " " + month + " " + day);
		cal.set(Calendar.HOUR_OF_DAY, today.get(Calendar.HOUR_OF_DAY));
		cal.set(Calendar.MINUTE, today.get(Calendar.MINUTE));
		cal.set(Calendar.SECOND, today.get(Calendar.SECOND));

		return cal.getTime();
	}
	*/
}
