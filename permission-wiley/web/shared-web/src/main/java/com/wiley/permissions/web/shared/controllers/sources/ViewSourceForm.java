package com.wiley.permissions.web.shared.controllers.sources;

import com.wiley.permissions.domain.persistence.permissions.Address;
import com.wiley.permissions.domain.persistence.permissions.Contact;
import com.wiley.permissions.domain.persistence.permissions.Source;
import com.wiley.permissions.domain.persistence.permissions.SourceAddress;
import com.wiley.permissions.web.shared.controllers.BaseFormBean;
import com.wiley.permissions.web.shared.controllers.sources.ManageSourceForm.ManagementMode;


/**
 *
 * @author ttidwell
 */
public class ViewSourceForm
extends BaseFormBean
{
	private static final long serialVersionUID = 1L;

	public enum Command
	{
		VIEW,
		MODIFY_SOURCE,
		NOFLY,
		ADD_SOURCE_ADDRESS,
		MODIFY_SOURCE_ADDRESS,
		REMOVE_SOURCE_ADDRESS,
		ADD_CONTACT,
		MODIFY_CONTACT,
		REMOVE_CONTACT,
		ADD_CONTACT_ADDRESS,
		MODIFY_CONTACT_ADDRESS,
		REMOVE_CONTACT_ADDRESS
	}

	private Command sourceCommand = Command.VIEW;
	private ManagementMode managementMode = ManagementMode.GENERAL;
	private Integer currentSourceId;
	private Integer currentSourceAddressId;
	private Integer currentContactId;
	private Integer currentContactAddressId;
	private Source source;
	private SourceAddress sourceAddress;
	private Address contactAddress;
	private Contact contact;


	public ViewSourceForm() {
	}

	public Command getSourceCommand() {
		return sourceCommand;
	}

	public void setSourceCommand(Command sourceCommand) {
		this.sourceCommand = sourceCommand;
	}

	public ManagementMode getManagementMode() {
		return managementMode;
	}

	public void setManagementMode(ManagementMode managementMode) {
		this.managementMode = managementMode;
	}

	public Integer getCurrentSourceId() {
		return currentSourceId;
	}

	public void setCurrentSourceId(Integer currentSourceId) {
		this.currentSourceId = currentSourceId;
	}

	public Source getSource() {
		return source;
	}

	public void setSource(Source source) {
		this.source = source;
	}

	public SourceAddress getSourceAddress() {
    	return sourceAddress;
    }

	public void setSourceAddress(SourceAddress address) {
    	this.sourceAddress = address;
    }

	public Contact getContact() {
    	return contact;
    }

	public void setContact(Contact contact) {
    	this.contact = contact;
    }

	public Address getContactAddress() {
    	return contactAddress;
    }

	public void setContactAddress(Address contactAddress) {
    	this.contactAddress = contactAddress;
    }

	public void setCurrentSourceAddressId(Integer currentSourceAddressId) {
    	this.currentSourceAddressId = currentSourceAddressId;
    }

	public void setCurrentContactId(Integer currentContactId) {
    	this.currentContactId = currentContactId;
    }

	public void setCurrentContactAddressId(Integer currentContactAddressId) {
    	this.currentContactAddressId = currentContactAddressId;
    }

	public Integer getCurrentSourceAddressId() {
    	return currentSourceAddressId;
    }

	public Integer getCurrentContactId() {
    	return currentContactId;
    }

	public Integer getCurrentContactAddressId() {
    	return currentContactAddressId;
    }
}
