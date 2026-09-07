package com.wiley.permissions.web.internal.controllers.admin;

import com.wiley.permissions.domain.persistence.permissions.UserGroup;
import com.wiley.permissions.web.shared.controllers.BaseFormBean;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author ttidwell
 */
public class ManageUserGroupForm
extends BaseFormBean
{
	private static final long serialVersionUID = 1L;

	private FormMode originalMode = null;
	private UserGroup group = null;
	private Integer selectedUser = null;
	private Integer selectedChild = null;
	private List<Integer> removedUsers = new ArrayList<Integer>();

	public ManageUserGroupForm() {
	}

	public FormMode getOriginalMode() {
		return originalMode;
	}

	public void setOriginalMode(FormMode originalMode) {
		this.originalMode = originalMode;
	}

	public UserGroup getGroup() {
		return group;
	}

	public void setGroup(UserGroup group) {
		this.group = group;
	}

	public Integer getSelectedUser() {
		return selectedUser;
	}

	public void setSelectedUser(Integer selectedUser) {
		this.selectedUser = selectedUser;
	}

	public Integer getSelectedChild() {
		return selectedChild;
	}

	public void setSelectedChild(Integer selectedChild) {
		this.selectedChild = selectedChild;
	}

	public List<Integer> getRemovedUsers() {
		return removedUsers;
	}

	public void setRemovedUsers(List<Integer> removedUsers) {
		this.removedUsers = removedUsers;
	}
}
