package com.wiley.permissions.services;

//import com.wiley.perm.plugin.annotation.GenerateRemote;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import com.wiley.permissions.common.bean.BeanCopyException;
import com.wiley.permissions.domain.persistence.permissions.Privilege;
import com.wiley.permissions.domain.persistence.permissions.Role;
import com.wiley.permissions.domain.persistence.permissions.User;
import com.wiley.permissions.domain.persistence.permissions.UserGroup;
import com.wiley.permissions.persistence.PersistenceException;
import com.wiley.permissions.security.web.AuthenticationException;
import com.wiley.permissions.security.web.PermissionsSecurityException;
import com.wiley.permissions.security.web.UserPrincipal;
import com.wiley.permissions.services.view.UserProfileView;

//@GenerateRemote
public interface SecurityService {

	public User checkCreateSSOUser(String email, String dn, SSOUserLookupUtility lookup)
	    throws PermissionsSecurityException, PersistenceException, AuthenticationException;

	public User loginUser(String email, String password, SSOUserLookupUtility lookup)
	    throws AuthenticationException, PersistenceException, PermissionsSecurityException;

	/**
	 * needs to be here because it is required by LoginModule
	 */
	public UserPrincipal findUserForLogin(String email) throws PermissionsSecurityException, PersistenceException;

	public void deleteUserGroupById(Integer id) throws PersistenceException;

	public Role deleteUserToRoleById(Integer userToRoleId, Role role) throws PersistenceException;

	public void deleteRoleByTypeAndCode(Role role) throws PersistenceException;

	public void voidUserGroup(Integer userId) throws PersistenceException;

	public Privilege mergePrivilege(Privilege privilege) throws PersistenceException;

	public Role mergeRole(Role role) throws PersistenceException;

	public User loadUserByIdForManageUser(Integer id) throws PersistenceException;

	public Role loadRoleByTypeAndCodeForManageRole(Role role) throws PersistenceException;

	public Privilege loadPrivilegeByCodeForManagePrivilege(String code) throws PersistenceException;

	public UserGroup loadUserGroupByIdForManageUserGroup(Integer id) throws PersistenceException;

	public List<UserProfileView> loadProfile(int userId);

	public void saveProfile(int userId, List<UserProfileView> profile)
	throws PersistenceException, BeanCopyException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException;

	public UserGroup saveUserGroup(UserGroup group, List<Integer> removedUserIds) throws PersistenceException;

	public void assignUsersToGroups() throws Exception;
}
