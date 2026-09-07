package com.wiley.permissions.web.internal.controllers.admin;

import com.wiley.permissions.web.shared.controllers.BaseFormBean;

/**
 *
 * @author ttidwell
 */
public class UserGroupMainForm
extends BaseFormBean
{
	private static final long serialVersionUID = 1L;
	
	private Integer groupId = null;
	private String search = null;
	
	public UserGroupMainForm() {

	}

	public Integer getGroupId() {
		return groupId;
	}

	public void setGroupId(Integer groupId) {
		this.groupId = groupId;
	}

	public String getSearch() {
		return search;
	}

	public void setSearch(String search) {
		this.search = search;
	}
}
