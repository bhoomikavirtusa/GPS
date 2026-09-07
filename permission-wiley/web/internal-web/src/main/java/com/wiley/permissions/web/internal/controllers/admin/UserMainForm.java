package com.wiley.permissions.web.internal.controllers.admin;

import com.wiley.permissions.web.shared.controllers.BaseFormBean;

/**
 *
 * @author ttidwell
 */
public class UserMainForm
extends BaseFormBean
{
	private static final long serialVersionUID = 1L;
	
	private Integer userId = null;
	private String search = null;
	
	public UserMainForm() {
	}

	public Integer getUserId() {
		return userId;
	}

	public void setUserId(Integer userId) {
		this.userId = userId;
	}

	public String getSearch() {
		return search;
	}

	public void setSearch(String search) {
		this.search = search;
	}
}
