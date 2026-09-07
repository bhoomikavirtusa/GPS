package com.wiley.permissions.services.view;

import com.wiley.permissions.domain.persistence.permissions.Address;

/**
 * Class that holds the results of sourceService.loadSourceSummaryByName().
 *
 * @author smarkoff
 */
public class SourceSummaryView {
	private String name;
	private String permissionsId;
	private String country;
	private Address mainAddress = new Address();
	private boolean disabled = false;
	private String vendor;
	private String sourceGroup;
	private String masterAgreement;
	private String nofly;
	private Integer numberOfContacts;
	
	public Integer getNumberOfContacts() {
		return numberOfContacts;
	}
	public void setNumberOfContacts(Integer numberOfContacts) {
		this.numberOfContacts = numberOfContacts;
	}
	
	public String getNofly() {
		return nofly;
	}
	public void setNofly(String nofly) {
		this.nofly = nofly;
	}

	public String getMasterAgreement() {
		return masterAgreement;
	}
	public void setMasterAgreement(String masterAgreement) {
		this.masterAgreement = masterAgreement;
	}
	
	public String getSourceGroup() {
		return sourceGroup;
	}
	public void setSourceGroup(String sourceGroup) {
		this.sourceGroup = sourceGroup;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	public String getPermissionsId() {
		return permissionsId;
	}
	public void setPermissionsId(String id) {
		this.permissionsId = id;
	}

	public String getCountry() {
		return country;
	}
	public void setCountry(String country) {
		this.country = country;
	}

	public Address getMainAddress() {
		return mainAddress;
	}
	public void setMainAddress(Address mainAddress) {
		this.mainAddress = mainAddress;
	}

	public boolean isDisabled() {
		return disabled;
	}
	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}
	
	public String getVendor() {
		return vendor;
	}
	public void setVendor(String vendor) {
		this.vendor = vendor;
	}
}
