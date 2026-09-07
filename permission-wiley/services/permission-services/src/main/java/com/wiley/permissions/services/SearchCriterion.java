package com.wiley.permissions.services;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.sf.common.lang.ArgUtil;

/**
 * Encapsulates a simple name/value pair.
 *
 * @author smarkoff
 */
public class SearchCriterion
{
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(SearchCriterion.class);


	private final String name;
	private final String value;

	/**
	 *
	 * @param name  Must be non-blank
	 * @param value  May be blank
	 */
	public SearchCriterion(String name, String value) {
		ArgUtil.notBlank(name, "name");
		this.name = name;
		this.value = value;
	}

    public String getName() {
    	return name;
    }

    public String getValue() {
    	return value;
    }
}
