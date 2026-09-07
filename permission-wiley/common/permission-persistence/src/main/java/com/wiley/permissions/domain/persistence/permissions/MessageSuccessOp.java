package com.wiley.permissions.domain.persistence.permissions;

import javax.xml.bind.annotation.XmlElement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.permissions.domain.DomainObject;

/**
 * 
 *
 * @author smarkoff
 */
public class MessageSuccessOp
extends DomainObject
{
	private static final long serialVersionUID = 1L;
	
	// success codes
	// maybe convert this to an enum later
	public static final String OBJECT_CREATED = "201";
	public static final String OBJECT_UPDATED = "202";
	public static final String OBJECT_DELETED = "203";

	@SuppressWarnings("unused")
	private final static Log log = LogFactory.getLog(MessageSuccessOp.class);

	@XmlElement
	private String code;
	
	@XmlElement
	private String text;
	
	@XmlElement
	private Reference reference;


	public MessageSuccessOp() {

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
	
	public Reference getReference() {
		return reference;
	}
	
	public void setReference(Reference ref) {
	    reference = ref;
    }

	@Override
	public String toString() {
		return "code = " + code
		    + ", text = " + text
		    + ", reference = " + reference;
	}
}
