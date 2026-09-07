package com.wiley.permissions.services.datatypes;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class NumericType implements IDataType {

	private static final Log log = LogFactory.getLog(NumericType.class);

	private static NumericType dataType = null;

	public static IDataType getInstance() {
		if (null == dataType) {
			try {
				dataType = new NumericType();
			} catch (Exception e) {  // this should not happen
				log.error("getInstance(): ", e);
			}
		}
		return dataType;
	}

	@Override
	public boolean valid(String value) {
		return true;
	}

	@Override
	public String format(String value) {
		// remove any non-digit characters
		value = value.replaceAll("[^\\d]", "");

		// Also if more than 9 chars, chop down to 9 since anything longer than 9 may not work (max 32-bit int is 4 billion)
		if (value.length() > 9) {
			String newValue = StringUtils.left(value, 9);  // chops off from right side, leaving left intact
			log.debug("format(): User entered [" + value + "] for numeric field - too long for 32-bit int - shortened to 9 digits [" + newValue + "].");
			value = newValue;
		}

		return value;
	}
}
