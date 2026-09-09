package com.wiley.permissions.services;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Query;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.wiley.permissions.common.bean.BeanCopyException;
import com.wiley.permissions.domain.persistence.permissions.Privilege;
import com.wiley.permissions.domain.persistence.permissions.Role;
import com.wiley.permissions.domain.persistence.permissions.User;
import com.wiley.permissions.domain.persistence.permissions.UserGroup;
import com.wiley.permissions.domain.persistence.permissions.UserProfile;
import com.wiley.permissions.domain.persistence.permissions.UserToRole;
import com.wiley.permissions.persistence.PersistenceException;
import com.wiley.permissions.repositories.UserRepository;
import com.wiley.permissions.security.sso.SSOUser;
import com.wiley.permissions.security.web.AuthenticationException;
import com.wiley.permissions.security.web.AuthenticationException.Type;
import com.wiley.permissions.security.web.LoginModule;
import com.wiley.permissions.security.web.PermissionsSecurityException;
import com.wiley.permissions.security.web.UserPrincipal;
import com.wiley.permissions.services.view.UserProfileView;
import com.wiley.sf.common.lang.ArgUtil;

/**
 * lnagy - for now all methods are Transactional (SUPPORT) because of the Filter
 * (when the filter tries to persist the session we get detached entity tried to persist)
 * smarkoff - We don't use the filter anymore
 *
 * @author ttidwell
 */
// @Path ("/security")
@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = Exception.class)
public class SecurityServiceImpl extends BaseService implements LoginModule, SecurityService {

	private final static Log log = LogFactory.getLog(SecurityServiceImpl.class);

	// These properties are all set by Spring
	private ExternalUserLdapClient externalUserLdapClient;
	private SSOUserLookupUtility internalSSOUserLookupUtility;
	private UserRepository userRepository;


	/**
	 * This method should only be called after receiving an SSO header pair
	 * (user_dn + user_mail) or otherwise authenticating the user, because
	 * this method assumes that the user is authenticated.
	 */
	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public User checkCreateSSOUser(String email, String dn, SSOUserLookupUtility lookup)
	throws PermissionsSecurityException, PersistenceException, AuthenticationException
	{
		User user = userRepository.loadByEmail(email);

		if (user == null) {
			log.info("checkCreateSSOUser(): Creating User: " + email);

			user = checkIfSSOUserAlreadyExistsWithoutEmail(email, dn);
			if (user != null) return user;

			// hit Ldap to get firstName, lastName
			SSOUser ssoUser = lookup.findByEmail(email);
			return userRepository.createUserFromSSO(ssoUser);
		}
		else {
			if (!user.isEnabled()) {
				throw new AuthenticationException(Type.DISABLED, "User Account Is Disabled");
			}
			log.info("checkCreateSSOUser(): Found User: " + email);
		}

		return user;
	}

	/**
	 * This method should be called when the login form is submitted (instead
	 * of receiving SSO headers) - assumes the user is NOT yet authenticated.
	 *
	 * @param email  Must be non-blank or AuthenticationException thrown
	 * @param password  Must be non-blank or AuthenticationException thrown
	 * @param lookup  Must be non-null
	 */
	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public User loginUser(String email, String password, SSOUserLookupUtility lookup)
	throws AuthenticationException, PersistenceException, PermissionsSecurityException {
		if (StringUtils.isBlank(email)) {
			throw new AuthenticationException(Type.INCOMPLETE_USER, "email was blank");
		}
		if (StringUtils.isBlank(password)) {
			throw new AuthenticationException(Type.INCOMPLETE_USER, "password was blank");
		}

		SSOUser ssoUser = lookup.findByEmail(email);
		log.debug("loginUser(): email [" + email + "] found: " + (ssoUser != null));
		if (ssoUser == null) {
			throw new AuthenticationException(Type.USER_NOT_FOUND, "email [" + email + "] not found in ldap");
		}

		// have time delay to prevent brute force password attack
		try { Thread.sleep(1000); } // 1 second
		catch (InterruptedException ex) { }

		boolean ok = lookup.authenticateByEmail(email, password);
		if (!ok) {
			throw new AuthenticationException(Type.INCOMPLETE_USER, "password for email [" + email + "] not valid");
		}

		return checkCreateSSOUser(email, ssoUser.getLdapDN(), lookup);
	}

	/**
	 * Returns true if the user already existed and was able to update the user record
	 * with the email and ldapDN.
	 *
	 * This method handles the situation where first a user (employee) is
	 * entered into the system due to a Product import (The user is an editor or has
	 * some other role). In this case the email and ldapDN for the user are not saved.
	 * So then when the user tries to login for the first time (after the import) they
	 * can't -- this method fixes that.
	 *
	 * @throws PersistenceException
	 * @throws PermissionsSecurityException
	 */
	private User checkIfSSOUserAlreadyExistsWithoutEmail(String email, String dn) throws PersistenceException, PermissionsSecurityException {
		log.debug("checkIfSSOUserAlreadyExistsWithoutEmail(): entered...");

		int index = email.indexOf('@');
		if (index == -1)  return null;
		String beg = email.substring(0, index);
		if (StringUtils.isBlank(beg))  return null;

		User user = userRepository.loadByCode(beg);
		if (user == null || StringUtils.isNotBlank(user.getEmail()))  return null;

		user.setEmail(email);
		//user.setLdapDN(dn);  // we no longer store this
		user = userRepository.merge(user);

		userRepository.addRoleToUser(user.getId(), Role.EMPLOYEE_DEFAULT);

		return user;
	}

	@Override
	public UserPrincipal findUserForLogin(String email) throws PermissionsSecurityException, PersistenceException
	{
		return userRepository.loadUserForLogin(email);
	}

	/**
	 * Remove associations between the group and any users, then delete the group.
	 *
	 * @param id
	 * @throws PersistenceException
	 */
	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public void deleteUserGroupById(Integer id) throws PersistenceException
	{
		Query q = userRepository.createQuery("update User u set u.group = null where u.group.id = ?1");
		q.setParameter(1, id);
		q.executeUpdate();

		Query q2 = userRepository.createQuery("delete from UserGroup ug where ug.id = ?1");
		q2.setParameter(1, id);
		q2.executeUpdate();
	}

	/**
	 * If you pass a non-null role, loads the Role when done so you have
	 * the refreshed Role without the deleted User.
	 * If you pass null for Role, the Role is not loaded and null is returned.
	 */
	@Override
	@Transactional(propagation=Propagation.REQUIRED)
	public Role deleteUserToRoleById(Integer userToRoleId, Role role) throws PersistenceException
	{
		// ---- Part 1
		// UserToRole u2r = userRepository.find(UserToRole.class, id);
		// userRepository.remove(u2r);

		// Think this should be slightly more efficient - don't need to load the object
		Query q = userRepository.createQuery("delete from UserToRole ur where ur.id = ?1");
		q.setParameter(1, userToRoleId);
		q.executeUpdate();

		if (role == null)  return null;

		// ---- Part 2

		role = userRepository.loadRoleByTypeAndCode(role);
		for (UserToRole u2r : role.getUsersNotNull()) {
			u2r.getUser().getLastName();
		}
		return role;
	}

	/**
	 * Delete all associations between the role and users / privileges
	 * and then delete the role itself.
	 *
	 * @param role  Must be non-null with a valid type and code
	 * @throws PersistenceException
	 */
	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public void deleteRoleByTypeAndCode(Role role) throws PersistenceException
	{
		Role role2 = userRepository.loadRoleByTypeAndCode(role);
		if (role2 == null) {
			log.warn("deleteRoleByTypeAndCode(): Asked to delete non-existant role: " + role);
			return;
		}
		role = role2;

		// smarkoff: Get Hibernate "deleted entity passed to persist" from this
		// if the Role has an associated user or privilege.
		//role.getPrivileges().clear();
		//role.getUsers().clear();
		//userRepository.remove(role);

		// Alternate way
		int roleId = role.getId();
		Query q = userRepository.createQuery("delete from UserToRole ur where ur.role.id = ?1");
		q.setParameter(1, roleId);
		q.executeUpdate();

		q = userRepository.createNativeQuery("delete from role_2_priv where role_id = ?");
		q.setParameter(1, roleId);
		q.executeUpdate();

		q = userRepository.createQuery("delete from Role where id = ?1");
		q.setParameter(1, roleId);
		q.executeUpdate();
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public void voidUserGroup(Integer userId) throws PersistenceException
	{	/*
		User u = userRepository.find(User.class, userId);
		u.getGroup().getUsers().remove(u);
		u.setGroup(null);
		userRepository.merge(u);
		*/

		Query q = userRepository.createQuery("update User u set u.group = null where u.id = ?1");
		q.setParameter(1, userId);
		q.executeUpdate();
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public Role mergeRole(Role role) throws PersistenceException
	{
		return userRepository.merge(role);
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public UserGroup saveUserGroup(UserGroup group, List<Integer> removedUserIds) throws PersistenceException
	{
		group = userRepository.merge(group);

		// Create separate list to use in for loop or get
		// java.util.ConcurrentModificationException if there are 2+ users in the group
		List<User> userList2 = new ArrayList<User>();
		userList2.addAll(group.getUsersNotNull());

		for (User user : userList2) {
			user.setGroup(group);
			user = userRepository.merge(user);
		}

		for (Integer userId : removedUserIds) {
			voidUserGroup(userId);  // throws PersistenceException
		}

		return group;
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public User loadUserByIdForManageUser(Integer id) throws PersistenceException
	{
		User user = null;
		try {
		    user = getUserRepository().lazyLoad (User.class, id, new String[] {"favoriteGroup"});
		} catch (Exception e) {
			user = null;
		}
	//	User user = userRepository.find(User.class, id);
		if (user != null) {
			UserGroup group = user.getGroup();
			if (group != null) group.getDescription();
		}

		return user;
	}

	/**
	 * @param role  Must be non-null and have a valid roleType and code
	 */
	@Override
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public Role loadRoleByTypeAndCodeForManageRole(Role role) throws PersistenceException {
		ArgUtil.notNull(role, "role");
		Role lookup = userRepository.loadRoleByTypeAndCode(role);
		if (lookup == null) {
			throw new RuntimeException("failed to load role for type ["
				+ role.getRoleType() + "] code [" + role.getCode() + "].");
		}
		else {
			role = lookup;
		}

		for (UserToRole u2r : role.getUsersNotNull()) {
			u2r.getUser().getLastName();
		}

		return role;
	}

	@Override
	public Privilege loadPrivilegeByCodeForManagePrivilege(String code) throws PersistenceException {
		return userRepository.find(Privilege.class, code);
	}

	@Override
	public UserGroup loadUserGroupByIdForManageUserGroup(Integer id) throws PersistenceException {
		UserGroup group = userRepository.find(UserGroup.class, id);

		for (User user : group.getUsersNotNull()) {
			user.getLastName();
		}

		return group;
	}

	@Override
	public List<UserProfileView> loadProfile(int userId) {
		List<UserProfile> upList = userRepository.loadProfile(userId);
		List<UserProfileView> upvList = new ArrayList<UserProfileView>();
		for (UserProfile up : upList)
			upvList.add (new UserProfileView (up));
		return upvList;
	}

	@Override
	public void saveProfile(int userId, List<UserProfileView> profile)
	throws PersistenceException, BeanCopyException, IllegalAccessException, InstantiationException,
	InvocationTargetException, NoSuchMethodException
	{
		// first we delete the existing fields
		userRepository.deleteProfile(userId);

		// fieldList might be null if no values selected
		if (CollectionUtils.isNotEmpty(profile)) {
			// add the new list of fields
			for (UserProfileView field : profile) {
				userRepository.saveProfileField(userId, field);
			}
		}
	}

	/**
	 * Assign users that are unassigned to groups.
	 * This method is called from the DevMisc page.
	 * This method (and the UI link) are temporary (at some point can be removed).
	 */
	@Override
	public void assignUsersToGroups() throws Exception {
		List<User> users = userRepository.executeNamedQuery("User.findUsersWithNoGroup", null, false);
		log.info("assignUsersToGroups(): found " + users.size() + " users with no group.");
		int successCount = 0;
		int noEmailErrorCount = 0;
		int ldapLookupErrorCount = 0;
		int groupErrorCount = 0;
		int updateErrorCount = 0;
		int totalCount = -1;

		for (User user : users) {
			String email = user.getEmail();
			String code = user.getCode();
			log.debug("assignUsersToGroups(): processing email: " + email + ", code: " + code);
			boolean updateByCode = false;

			try {
				// need to have this at the top because of all the continues below
				totalCount++;
				if (totalCount % 100 == 0) {
					log.info("assignUsersToGroups(): totalCount so far = " + totalCount + " out of " + users.size());
				}

				if (StringUtils.isBlank(email)) {
					// In most cases if code begins with a letter it is the first part of user's email so try that
					// code is non-nullable but check for blank just in case
					if (StringUtils.isNotBlank(code) && Character.isLetter(code.charAt(0))) {
						email = code + "@wiley.com";
						updateByCode = true;
					}
					else {
						log.error("assignUsersToGroups(): email is null and code does not begin with letter...continue");
						noEmailErrorCount++;
						continue;
					}
				}

				// lnagy - I had the option to call UserRepository.saveUser but it was very slow.
				// I decided to replicate the code a little bit here and do the updates using queries
				SSOUser ssoUser = internalSSOUserLookupUtility.findByEmail(email);
				if (ssoUser == null) {
					log.error("assignUsersToGroups(): Failed to load user from ldap for email [" + email + "]");
					ldapLookupErrorCount++;
					continue;
				}

				UserGroup group = UserGroup.getInstance(ssoUser.getLdapGroupName());
				if (group != null) {
					group = (UserGroup)userRepository.executeSingleResultNamedQuery("UserGroup.findByName", new Object[] {group.getName()});
				}
				if (null == group) {
					log.error("assignUsersToGroups(): Failed to load group for group [" + ssoUser.getLdapGroupName() + "]");
					groupErrorCount++;
					continue;
				}

				if (updateByCode)  {
					ArrayList<Object> params = new ArrayList<Object>();
					params.add(group.getId());
					params.add(code);
					userRepository.executeNativeQuery("update user_table set user_group_id=? where code=?", params);
				}
				else {  // update by email
					ArrayList<Object> params = new ArrayList<Object>();
					params.add(group.getId());
					params.add(email);
					userRepository.executeNativeQuery("update user_table set user_group_id=? where email=?", params);
				}

				successCount++;
			} catch (Exception e) {
				log.error("assignUsersToGroups(): Failed to update user [" + email + "]", e);
				updateErrorCount++;
			}
		} // end for

		log.info("assignUsersToGroups(): successCount = " + successCount
			+ ",\r\nnoEmailErrorCount = " + noEmailErrorCount + ", ldapLookupErrorCount = " + ldapLookupErrorCount
			+ ", groupErrorCount = " + groupErrorCount + ", updateErrorCount = " + updateErrorCount);
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public Privilege mergePrivilege(Privilege privilege) throws PersistenceException {
		return userRepository.merge(privilege);
	}

	public ExternalUserLdapClient getExternalUserLdapClient() {
		return externalUserLdapClient;
	}

	public void setExternalUserLdapClient(ExternalUserLdapClient externalUserLdapClient) {
		this.externalUserLdapClient = externalUserLdapClient;
	}

	public UserRepository getUserRepository() {
		return userRepository;
	}

	public void setUserRepository(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public SSOUserLookupUtility getInternalSSOUserLookupUtility() {
		return internalSSOUserLookupUtility;
	}

	public void setInternalSSOUserLookupUtility(
			SSOUserLookupUtility internalSSOUserLookupUtility) {
		this.internalSSOUserLookupUtility = internalSSOUserLookupUtility;
	}
}
