package com.wiley.permissions.domain.persistence.permissions;

import javax.xml.bind.annotation.XmlElement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.permissions.domain.DomainObject;

/**
 * Encapsulates PE productError XML element
 * (but with everything as sub-elements instead of attributes).
 *
 * @author smarkoff
 */
public class ProductError
extends DomainObject
{
	private static final long serialVersionUID = 1L;

	@SuppressWarnings("unused")
	private final static Log log = LogFactory.getLog(ProductError.class);

	@XmlElement
	private String code;

	@XmlElement
	private String text;

	@XmlElement
	private String queryType;

	@XmlElement
	private String queryValue;


	public ProductError() {

	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getQueryType() {
		return queryType;
	}

	public void setQueryType(String queryType) {
		this.queryType = queryType;
	}

	public String getQueryValue() {
		return queryValue;
	}

	public void setQueryValue(String queryValue) {
		this.queryValue = queryValue;
	}

	@Override
	public String toString() {
		return "code = " + code
			+ ", text = " + text
			+ ", queryType = " + queryType
			+ ", queryValue= " + queryValue;
	}
}
