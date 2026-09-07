package com.wiley.permissions.web.shared.util;

import java.beans.PropertyEditorSupport;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.permissions.domain.persistence.permissions.Country;

/**
 *
 * @author smarkoff
 */
public class CountryPropertyEditorSupport
extends PropertyEditorSupport
{
	@SuppressWarnings("unused")
	private final static Log log = LogFactory.getLog(CountryPropertyEditorSupport.class);

	@Override
	public String getAsText() {
		Country country = (Country) getValue();
		if (country == null)  return "";
		else  return country.getCode();
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

		Country country = new Country();
		country.setCode(text);
		setValue(country);
	}
}
