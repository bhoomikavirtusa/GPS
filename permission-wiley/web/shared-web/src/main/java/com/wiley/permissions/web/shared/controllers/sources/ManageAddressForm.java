package com.wiley.permissions.web.shared.controllers.sources;

import com.wiley.permissions.domain.persistence.permissions.Address;
import com.wiley.permissions.web.shared.controllers.BaseFormBean;

/**
 *
 * @author ttidwell
 */
public class ManageAddressForm
extends BaseFormBean
{
	private static final long serialVersionUID = 1L;

	private Integer sourceId = null;
	private Integer contactId = null;
	private Address address = null;

	public ManageAddressForm() {

	}

	public Integer getSourceId() {
		return sourceId;
	}

	public void setSourceId(Integer sourceId) {
		this.sourceId = sourceId;
	}

	public Integer getContactId() {
    	return contactId;
    }

	public void setContactId(Integer contactId) {
    	this.contactId = contactId;
    }

	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}
}
