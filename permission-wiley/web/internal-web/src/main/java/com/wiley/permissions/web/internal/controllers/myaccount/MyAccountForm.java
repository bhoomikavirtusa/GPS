package com.wiley.permissions.web.internal.controllers.myaccount;

import com.wiley.permissions.domain.persistence.permissions.User;
import com.wiley.permissions.web.shared.controllers.BaseFormBean;

/**
 *
 * @author ttidwell
 */
public class MyAccountForm
extends BaseFormBean
{
	private static final long serialVersionUID = 1L;

	private User user = null;
	private Integer favoriteGroupId = null;
	private Integer userGroupId;

	public MyAccountForm() {

	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public void setFavoriteGroupId(Integer favoriteGroupId) {
		this.favoriteGroupId = favoriteGroupId;
	}

	public Integer getFavoriteGroupId() {
		return favoriteGroupId;
	}

	public void setUserGroupId(Integer userGroupId) {
		this.userGroupId = userGroupId;
	}

	public Integer getUserGroupId() {
		return userGroupId;
	}
}
