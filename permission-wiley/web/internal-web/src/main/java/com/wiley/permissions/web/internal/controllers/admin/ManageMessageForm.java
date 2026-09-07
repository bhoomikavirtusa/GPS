package com.wiley.permissions.web.internal.controllers.admin;

import org.apache.commons.lang3.StringUtils;

import com.wiley.permissions.domain.persistence.permissions.DataSource;
import com.wiley.permissions.web.shared.controllers.BaseFormBean;


/**
 *
 * @author smarkoff
 */
public class ManageMessageForm
extends BaseFormBean {

	private static final long serialVersionUID = 1L;

	private String messageName;
	private String externalId;
	private String dataSourceCode;
	private int bunchLimit;
	private String masterListName;
	private int updatedSinceMins;
	private int updatedSinceHours;
	private int updatedSinceDays;


	public ManageMessageForm() {
		super();
	}

	public void setMessageName(String messageName) {
		this.messageName = messageName;
	}

	public String getMessageName() {
		return messageName;
	}

	public void setExternalId(String externalId) {
		this.externalId = StringUtils.stripToNull(externalId);
	}

	public String getExternalId() {
		return externalId;
	}

	public void setDataSourceCode(String dataSourceCode) {
		this.dataSourceCode = StringUtils.trimToNull(dataSourceCode);
	}

	public String getDataSourceCode() {
		return dataSourceCode;
	}

	public int getBunchLimit() {
		return bunchLimit;
	}

	public void setBunchLimit(int limit) {
		bunchLimit = limit;
	}

	public void setMasterListName(String name) {
		this.masterListName = StringUtils.stripToNull(name);
	}

	public String getMasterListName() {
		return masterListName;
	}

	public void setUpdatedSinceMins(int updatedSinceMins) {
		this.updatedSinceMins = updatedSinceMins;
	}

	public int getUpdatedSinceMins() {
		return updatedSinceMins;
	}

	public void setUpdatedSinceHours(int updatedSinceHours) {
		this.updatedSinceHours = updatedSinceHours;
	}

	public int getUpdatedSinceHours() {
		return updatedSinceHours;
	}

	public void setUpdatedSinceDays(int updatedSinceDays) {
		this.updatedSinceDays = updatedSinceDays;
	}

	public int getUpdatedSinceDays() {
		return updatedSinceDays;
	}

	public DataSource [] getDataSourceArray() {
		return DataSource.ALL;
	}
}
