package com.wiley.permissions.web.shared.controllers.servicedesk;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.permissions.domain.persistence.permissions.Role;
import com.wiley.permissions.domain.persistence.permissions.User;
import com.wiley.permissions.domain.persistence.permissions.UserGroup;
import com.wiley.permissions.web.shared.controllers.BaseFormBean;


public class AddUsersForm 
extends BaseFormBean
{
	private static final long serialVersionUID = 1L;
	
	private final static Log log = LogFactory.getLog(AddUsersForm.class);
	private FormMode originalMode;
	
	private Integer selectedChild;
	private List<Integer> deletedChildren = new ArrayList<Integer>();


	private String firstName;
	private String lastName;
	private String email;
	private String code;
	private List<UserGroup> userGroup1;
	private List<User.Type> userType;
	private boolean enabled;
	private Date updatedDate;
	private Date createdDate;
	private String type;
	private List<Role> globalRoles;
	private Integer favoriteGroupId;
	
	
	//private Map<Integer, List<UsageConditionSize>> usageSizeMap;

	public String getType() {
		return type;
	}



	public void setType(String type) {
		this.type = type;
	}
	private UserGroup group;
	private User user;

	public AddUsersForm() {
		super();
	}
	
	
	
	public void setGroup(UserGroup userGroup) {
		this.group = userGroup;
	}
	
	public UserGroup getGroup() {
		return group;
	}

	public FormMode getOriginalMode() {
		return originalMode;
	}

	public void setOriginalMode(FormMode originalMode) {
		this.originalMode = originalMode;
	}
	public void setUser(User user) {
		this.user = user;
	}
	
	public User getUser() {
		return user;
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
	public String getFirstName() {
		return firstName;
	}
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}
	public String getLastName() {
		return lastName;
	}
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	public List<UserGroup> getUserGroup1() {
		return userGroup1;
	}
	public void setUserGroup1(List<UserGroup> userGroup1) {
		this.userGroup1 = userGroup1;
	}
	public List<User.Type> getUserType() {
		return userType;
	}
	public void setUserType(List<User.Type> userType) {
		this.userType = userType;
	}
	public boolean isEnabled() {
		return enabled;
	}
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	public Date getUpdatedDate() {
		return updatedDate;
	}
	public void setUpdatedDate(Date updatedDate) {
		this.updatedDate = updatedDate;
	}
	public Date getCreatedDate() {
		return createdDate;
	}
	public void setCreatedDate(Date createdDate) {
		this.createdDate = createdDate;
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
