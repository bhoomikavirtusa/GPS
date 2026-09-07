package com.wiley.permissions.web.internal.controllers.landing;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author smarkoff
 */
public class AuthorPrivilegeForm {

	@SuppressWarnings("unused")
	private final static Log log = LogFactory.getLog(AuthorPrivilegeForm.class);

	private Integer cwId;
	private Integer authorId;
	private Date dueDate;
	private String [] selectedCwPrivileges;
	private String [] selectedGlobalPrivileges;
	private boolean saveAsDefaults;
	private boolean authorReadOnly;

	public Integer getCwId() {
		return cwId;
	}

	public void setCwId(Integer cwId) {
		this.cwId = cwId;
	}

	public Integer getAuthorId() {
		return authorId;
	}

	public void setAuthorId(Integer authorId) {
		this.authorId = authorId;
	}

	public Date getDueDate() {
		return dueDate;
	}

	public void setDueDate(Date dueDate) {
		this.dueDate = dueDate;
	}

	public boolean getAuthorReadOnly() {
		return authorReadOnly;
	}

	public void setAuthorReadOnly(boolean authorReadOnly) {
		this.authorReadOnly = authorReadOnly;
	}

	public String[] getSelectedCwPrivileges() {
		return selectedCwPrivileges;
	}

	public void setSelectedCwPrivileges(String[] selectedCwPrivileges) {
		this.selectedCwPrivileges = selectedCwPrivileges;
	}

	public String[] getSelectedGlobalPrivileges() {
		return selectedGlobalPrivileges;
	}

	public void setSelectedGlobalPrivileges(String[] selectedGlobalPrivileges) {
		this.selectedGlobalPrivileges = selectedGlobalPrivileges;
	}

	public boolean getSaveAsDefaults() {
		return saveAsDefaults;
	}

	public void setSaveAsDefaults(boolean b) {
		this.saveAsDefaults = b;
	}
}
