package com.wiley.permissions.web.shared.controllers.sources;

import com.wiley.permissions.domain.persistence.permissions.Address;
import com.wiley.permissions.domain.persistence.permissions.Contact;
import com.wiley.permissions.domain.persistence.permissions.Source;
import com.wiley.permissions.web.shared.controllers.BaseFormBean;


/**
 *
 * @author smarkoff
 */
public class CreateSourceForm
extends BaseFormBean
{
	private static final long serialVersionUID = 1L;

	private Integer assetId;
	private Integer assetUseId;

	private Source source = new Source();
	private Address address = new Address();
	private Contact contact = new Contact();


	public CreateSourceForm() {
		contact.setAddress(new Address());
	}

	public Integer getAssetId() {
		return assetId;
	}

	public void setAssetId(Integer assetId) {
		this.assetId = assetId;
	}

	public Integer getAssetUseId() {
		return assetUseId;
	}

	public void setAssetUseId(Integer assetUseId) {
		this.assetUseId = assetUseId;
	}

	public Source getSource() {
		return source;
	}

	public void setSource(Source source) {
		this.source = source;
	}

	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}

	public Contact getContact() {
    	return contact;
    }

	public void setContact(Contact contact) {
    	this.contact = contact;
    }

	@Override
	public String toString() {
		return super.toString()
		    + ", assetId = " + assetId
		    + ", assetUseId = " + assetUseId
		    + ", source = " + source
		    + ", (other fields not displayed)";
	}
}
