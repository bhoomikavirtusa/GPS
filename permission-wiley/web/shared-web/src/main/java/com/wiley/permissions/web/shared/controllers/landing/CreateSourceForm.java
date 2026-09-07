package com.wiley.permissions.web.shared.controllers.landing;

import org.springframework.web.multipart.MultipartFile;

import com.wiley.permissions.domain.persistence.permissions.Address;
import com.wiley.permissions.domain.persistence.permissions.Contact;
import com.wiley.permissions.domain.persistence.permissions.Source;

/**
 * @author smarkoff
 */
public class CreateSourceForm {

	private Source source = new Source();
	private Address address = new Address();
	private Contact contact = new Contact();
	private Address contactAddress = new Address();
	private MultipartFile multipartFile;

	public CreateSourceForm() {

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

	public MultipartFile getMultipartFile() {
		return multipartFile;
	}

	public void setMultipartFile(MultipartFile multipartFile) {
		this.multipartFile = multipartFile;
	}

	public Address getContactAddress() {
    	return contactAddress;
    }

	public void setContactAddress(Address contactAddress) {
    	this.contactAddress = contactAddress;
    }
}
