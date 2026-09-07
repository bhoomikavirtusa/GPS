package com.wiley.permissions.web.internal.controllers.admin;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.permissions.domain.persistence.permissions.Role;
import com.wiley.permissions.domain.persistence.permissions.User;
import com.wiley.permissions.domain.persistence.permissions.UserToRole;
import com.wiley.permissions.web.shared.controllers.BaseFormBean;

/**
 *
 * @author ttidwell
 */
public class ManageUserForm
extends BaseFormBean
{
	private static final long serialVersionUID = 1L;

	@SuppressWarnings("unused")
	private final static Log log = LogFactory.getLog(ManageUserForm.class);

	private FormMode originalMode;

	private User user;
	private Integer productId;
	private UserToRole userToRole;
	private List<Role> globalRoles;
	private Integer favoriteGroupId;

	private Integer selectedChild;
	private List<Integer> deletedChildren = new ArrayList<Integer>();

	public ManageUserForm() {
		super();
	}

	public FormMode getOriginalMode() {
		return originalMode;
	}

	public void setOriginalMode(FormMode originalMode) {
		this.originalMode = originalMode;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public Integer getProductId() {
		return productId;
	}

	public void setProductId(Integer selectedProduct) {
		this.productId = selectedProduct;
	}

	public UserToRole getUserToRole() {
		return userToRole;
	}

	public void setUserToRole(UserToRole userToRole) {
		this.userToRole = userToRole;
	}

	public Integer getSelectedChild() {
		return selectedChild;
	}

	public void setSelectedChild(Integer selectedChild) {
		this.selectedChild = selectedChild;
	}

	public List<Integer> getDeletedChildren() {
		return deletedChildren;
	}

	public void setDeletedChildren(List<Integer> deletedChildren) {
		this.deletedChildren = deletedChildren;
	}

	public List<Role> getGlobalRolesNotNull() {
		if (globalRoles == null) {
			globalRoles = new ArrayList<Role>(0);
		}
		return globalRoles;
	}

	public List<Role> getGlobalRoles() {
		return globalRoles;
	}

	public void setGlobalRoles(List<Role> globalRoles) {
		this.globalRoles = globalRoles;
	}

	public void setFavoriteGroupId(Integer favoriteGroupId) {
		this.favoriteGroupId = favoriteGroupId;
	}

	public Integer getFavoriteGroupId() {
		return favoriteGroupId;
	}
}
