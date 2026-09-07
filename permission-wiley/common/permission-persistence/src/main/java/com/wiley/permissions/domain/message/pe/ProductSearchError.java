package com.wiley.permissions.domain.message.pe;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

/**
 *
 * @author ttidwell
 */
public class ProductSearchError
implements Serializable
{
	private static final long serialVersionUID = 1L;

	@XmlAttribute
	private String code;

	@XmlValue
	private String message;

	public ProductSearchError()
	{

	}

	public String getCode()
	{
		return code;
	}

	public void setCode(String code)
	{
		this.code = code;
	}

	public String getMessage()
	{
		return message;
	}

	public void setMessage(String message)
	{
		this.message = message;
	}
}
