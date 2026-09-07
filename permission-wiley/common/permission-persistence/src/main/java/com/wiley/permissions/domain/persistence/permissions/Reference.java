package com.wiley.permissions.domain.persistence.permissions;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.permissions.domain.DomainObject;

/**
 * Object that only has an externalId attribute.
 * Can be used as a reference for any object (Asset, Source, etc).
 * Used for the CMS to/from Permissions Get and Delete messages.
 *
 * @author smarkoff
 */
public class Reference
extends DomainObject
{
	private static final long serialVersionUID = 1L;

	@SuppressWarnings("unused")
	private final static Log log = LogFactory.getLog(Reference.class);

	@XmlID
	@XmlElement
	private String externalId = null;


	public Reference() {

	}

	public Reference(String externalId) {
		setExternalId(externalId);
	}

	public String getExternalId() {
		return externalId;
	}

	public void setExternalId(String externalId) {
		this.externalId = externalId;
	}

	@Override
	public String toString() {
		return "externalId = " + externalId;
	}
}
