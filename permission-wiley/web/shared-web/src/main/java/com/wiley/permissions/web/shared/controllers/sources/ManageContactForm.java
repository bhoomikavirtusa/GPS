package com.wiley.permissions.web.shared.controllers.sources;

import com.wiley.permissions.domain.persistence.permissions.Address;
import com.wiley.permissions.domain.persistence.permissions.Contact;
import com.wiley.permissions.web.shared.controllers.BaseFormBean;


/**
 *
 * @author ttidwell
 */
public class ManageContactForm
extends BaseFormBean
{
	private static final long serialVersionUID = 1L;

	private Integer sourceId = null;
	private Contact contact = null;

	public ManageContactForm() {

	}

	public Integer getSourceId() {
		return sourceId;
	}

	public void setSourceId(Integer sourceId) {
		this.sourceId = sourceId;
	}

	public Contact getContact() {
    	return contact;
    }

	public void setContact(Contact contact) {
    	this.contact = contact;
    }

	/**
	 * Shortcut so when ManageContactForm given to manageAddressInclude.jspx
	 * 'address' can be accessed directly off the form.
	 */
	public Address getAddress() {
		return contact.getAddress();
	}

	/**
	 * Shortcut so when ManageContactForm given to manageAddressInclude.jspx
	 * 'address' can be accessed directly off the form.
	 */
	public void setAddress(Address a) {
		contact.setAddress(a);
	}
}
