package com.wiley.permissions.domain.message;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlElement;

/**
 *
 * @author smarkoff
 */
public class MessageError
implements Serializable
{
	private static final long serialVersionUID = 1L;

	@XmlElement
    private String code = null;  // later maybe this will be an enum or integer instead of String

	@XmlElement
    private String text = null;


	public MessageError()
	{
	}

	public MessageError(String code, String text)
	{
		this.code = code;
		this.text = text;
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
}
