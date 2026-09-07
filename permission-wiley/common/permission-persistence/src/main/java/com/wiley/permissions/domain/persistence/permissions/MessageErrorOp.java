package com.wiley.permissions.domain.persistence.permissions;

import javax.xml.bind.annotation.XmlElement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.permissions.domain.DomainObject;

/**
 * Note this is different from the MessageError class.
 * MessageError is used for PE messages.
 * This class is used for CMS messages.
 *
 * @author smarkoff
 */
public class MessageErrorOp
extends DomainObject
{
	private static final long serialVersionUID = 1L;
	
	// error codes
	// maybe convert this to an enum later
	public static final String NOT_FOUND = "101";
	public static final String CANNOT_DELETE = "102";
	public static final String REQUIRED_FIELD_MISSING = "103";
	public static final String DUPLICATE_SOURCE_NAME_ERROR = "104";
	public static final String DUPLICATE_ADDRESS_TYPE_ERROR = "105";

	@SuppressWarnings("unused")
	private final static Log log = LogFactory.getLog(MessageErrorOp.class);

	@XmlElement
	private String code;
	
	@XmlElement
	private String text;
	
	@XmlElement
	private Reference reference;


	public MessageErrorOp() {

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
