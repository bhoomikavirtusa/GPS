package com.wiley.permissions.domain.message;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAttribute;

/**
 *
 * @author ttidwell
 */
public class MessageOperationParameter
implements Serializable
{
	private static final long serialVersionUID = 1L;

	@XmlAttribute
	private String name = null;

	@XmlAttribute
	private String value = null;

	public MessageOperationParameter()
	{

	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getValue()
	{
		return value;
	}

	public void setValue(String value)
	{
		this.value = value;
	}
}
