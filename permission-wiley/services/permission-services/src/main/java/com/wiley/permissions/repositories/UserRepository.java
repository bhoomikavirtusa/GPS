package com.wiley.permissions.repositories;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.sql.DataSource;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.wiley.permissions.domain.persistence.permissions.AuthorCommonWorkPK;
import com.wiley.permissions.domain.persistence.permissions.AuthorToCommonWork;
import com.wiley.permissions.domain.persistence.permissions.CommonWork;
import com.wiley.permissions.domain.persistence.permissions.CommonWorkStatus;
import com.wiley.permissions.domain.persistence.permissions.FavoriteGroup;
import com.wiley.permissions.domain.persistence.permissions.Privilege;
import com.wiley.permissions.domain.persistence.permissions.Product;
import com.wiley.permissions.domain.persistence.permissions.PublicationStatus;
import com.wiley.permissions.domain.persistence.permissions.Role;
import com.wiley.permissions.domain.persistence.permissions.Role.RoleType;
import com.wiley.permissions.domain.persistence.permissions.Source;
import com.wiley.permissions.domain.persistence.permissions.User;
import com.wiley.permissions.domain.persistence.permissions.UserDefaults;
import com.wiley.permissions.domain.persistence.permissions.UserGroup;
import com.wiley.permissions.domain.persistence.permissions.UserProfile;
import com.wiley.permissions.domain.persistence.permissions.UserToRole;
import com.wiley.permissions.persistence.PersistenceException;
import com.wiley.permissions.persistence.repository.JPARepository;
import com.wiley.permissions.security.sso.SSOUser;
import com.wiley.permissions.security.web.PermissionsSecurityException;
import com.wiley.permissions.security.web.PrivilegePrincipal;
import com.wiley.permissions.security.web.UserPrincipal;
import com.wiley.permissions.services.SSOUserLookupUtility;
import com.wiley.permissions.services.view.UserProfileView;
import com.wiley.permissions.services.view.UserSubsetView;
import com.wiley.sf.common.monitor.PerformanceMonitor.PerfTimer;
import com.wiley.permissions.services.util.ServiceException;

/**
 * @author ttidwell
 */
public class UserRepository extends JPARepository {
	private final static Log log = LogFactory.getLog(UserRepository.class);

	// this refers to Permissions user id
	public static final int MASTER_USER_ID = 1;

	private SSOUserLookupUtility internalSSOUserLookupUtility;
	private DataSource dataSource;


	@PersistenceContext(unitName = "permissions")
	protected EntityManager entityManager;

	@Override
	public EntityManager getEntityManager() {
		return entityManager;
	}

	public UserRepository() {
	}

	/**
	 * This is required by the LoginModule interface. It just needs to use an e-mail to find the user and
	 * convert the user to his/her appropriate principal.
	 *
	 * @param email
	 * @return
	 * @throws com.wiley.permissions.security.web.PermissionsSecurityException
	 * @throws PersistenceException
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public UserPrincipal loadUserForLogin(String email) throws PermissionsSecurityException, PersistenceException
	{
		log.info("loadUserForLogin(): We got asked to find a user for: " + email);

		User user = loadByEmail(email);

		if (user == null) {
			throw new PermissionsSecurityException("No user found for email: " + email);
		}

		Properties properties = new Properties();
		properties.setProperty("user_id", String.valueOf(user.getId()));
		user.setRoles(loadAll(UserToRole.class, properties));

		log.info("loadUserForLogin(): Found User: " + user.getId());

		UserPrincipal output = new UserPrincipal();

		output.setId(user.getId());
		output.setEmail(user.getEmail());
		output.setFirstName(user.getFirstName());
		output.setLastName(user.getLastName());
		output.setUniqueName(email);
		if (null != user.getGroup())
			output.setGroupId(user.getGroup().getId());
		output.setAuthenticated(true);

		Set<PrivilegePrincipal> privileges = new HashSet<PrivilegePrincipal>();

		// add global roles as privileges - getGlobalUserToRoles() works because we called setRoles() above
		log.debug("loadUserForLogin(): user.getGlobalUserToRoles().size() = " + user.getGlobalUserToRoles().size());
		for (UserToRole role : user.getGlobalUserToRoles()) {
			log.debug("loadUserForLogin(): role code " + role.getRole().getCode());
			PrivilegePrincipal rp = new PrivilegePrincipal();
			rp.setCode(role.getRole().getCode());
			privileges.add(rp);
		}

		// add global privileges too
		Set<PrivilegePrincipal> privSet = getGlobalPrivileges(user.getId());
		privileges.addAll(privSet);

		output.setGlobalPrivileges(privileges);

		if (null != user.getUserDefaults()) {
			output.setShowRequest(user.getUserDefaults().isShowRequest());
			output.setCustomMode(user.getUserDefaults().isCustomMode());
			output.setCustomFilter(user.getUserDefaults().getCustomFilter());
			output.setCurrencyCode(user.getUserDefaults().getCurrency().getCode());
			output.setCountryCode(user.getUserDefaults().getCountryCode());
			output.setDateFormat(user.getUserDefaults().getDatePickerDateFormat());
		}

		return output;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public User loadDataForUserLanding(Integer userId, List<UserToRole> landingList) throws PersistenceException {
		User user = find(User.class, userId);  // throws PersistenceException
		if (user == null) {  // we don't expect this to happen
			throw new IllegalArgumentException("userId " + userId + " not found.");
		}

		// TODO maybe optimize this later
		// pre-load data for jsp (otherwise get lazy-load no session exception)
		List<CommonWork> commonWorkList = user.getWatchedCommonWorks();
		for (CommonWork cw : commonWorkList) {
			Product product = cw.getPrimaryProduct();
			// normally this should not happen (product == null) but we had a situation where it did
			// - Somehow Steve Robinson had a CommonWork bookmarked with no products.
			// (10/2013 - DEV only so don't worry about it)
			// don't try to fix it, just have a better error message than NullPointerException
			// The only way this could happen is if we have no products at all stored in our db for this product
			// since getPrimaryProduct() already throws an exception if there are products but none or too many
			// are marked as primary.
			if (product == null) {
				throw new RuntimeException("No Primary product for commonWork: " + cw);
			}
			product.getAuthorsAsString();
			product.getPublicationStatus().getDescription();
		}

		// User.roles is now transient so have to manually load roles
		// before call getProductUserToRoles()
		Properties properties = new Properties();
		properties.setProperty("user_id", String.valueOf(userId));
		user.setRoles(loadAll(UserToRole.class, properties));

		List<UserToRole> upList = user.getProductUserToRoles();

		for (UserToRole u2p : upList) {
			if (u2p.getProduct().getPublicationStatus().getCode()
					.equals(PublicationStatus.IN_PRODUCTION.getCode())
					|| u2p.getProduct().getPublicationStatus().getCode()
							.equals(PublicationStatus.EDITORIAL.getCode()))
			{
				// preload Authors
				u2p.getProduct().getAuthorsAsString();
				landingList.add(u2p);
			}
		}

		return user;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public User loadUserById(Integer id) throws PersistenceException {
		return find(User.class, id);
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public List<User> searchUser(String firstName, String lastName)
	{
		// if both empty, just return null (or we can decide to return all users)
		if (StringUtils.isBlank(firstName) && StringUtils.isBlank(lastName)) {
			return null;
		}

		TypedQuery<User> query = null;
		if (StringUtils.isNotBlank(firstName) && StringUtils.isBlank(lastName)) {
			query = createNamedQuery("User.findByFirstName", User.class);
			query.setParameter("firstName", firstName);
		}

		if (StringUtils.isNotBlank(lastName) && StringUtils.isBlank(firstName)) {
			query = createNamedQuery("User.findByLastName", User.class);
			query.setParameter("lastName", lastName);
		}

		if (StringUtils.isNotBlank(lastName) && StringUtils.isNotBlank(firstName)) {
			query = createNamedQuery("User.findByFirstNameLastName", User.class);
			query.setParameter("lastName", lastName);
			query.setParameter("firstName", firstName);
		}

		return query.getResultList();
	}

	/**
	 * Has a bunch of logic (not just JPA merge).
	 *
	 * @param user  Must be non-null
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public User saveUser(User user) throws ServiceException,PersistenceException
	{
		log.debug("saveUser() called with User: " + user);
		int gid = 0;

		// Before we store things to the DB, we need to supplement some
		// information in different ways for the various user types.
		switch (user.getType()) {
			case SYSTEM: {
				if (StringUtils.isBlank(user.getCode()) && StringUtils.isNotBlank(user.getEmail())) {
					user.setCode(user.getEmail());
				}
				if (null == user.getGroup() || !user.getGroup().equals(UserGroup.CORPORATE_GROUP)) {
					user.setGroup((UserGroup)this.executeSingleResultNamedQuery("UserGroup.findByName", new Object[] {UserGroup.CORPORATE_GROUP.getName()}));
				}
				break;
			}
			case AUTHOR: {				
				if(user.getViewType().equals("editUser")){
					if(null != user.getGroup()){
						gid = user.getGroup().getId(); 						
					}
				}else{
					//do nothing
				}
					
				if (StringUtils.isBlank(user.getCode()) && StringUtils.isNotBlank(user.getEmail())) {
					user.setCode(user.getEmail());
				}				
				if (null == user.getGroup() || !user.getGroup().equals(UserGroup.AUTHOR_GROUP)) {
					user.setGroup((UserGroup)this.executeSingleResultNamedQuery("UserGroup.findByName", new Object[] {UserGroup.AUTHOR_GROUP.getName()}));					
				}		
				break;
			}
			case FREELANCER: {
				if(user.getViewType().equals("editUser")){
					if(null != user.getGroup()){
						gid = user.getGroup().getId(); 						
					}
				}else{
					//do nothing
				}
				if (StringUtils.isBlank(user.getCode()) && StringUtils.isNotBlank(user.getEmail())) {
					user.setCode(user.getEmail());
				}
				if (null == user.getGroup() || !user.getGroup().equals(UserGroup.FREELANCER_GROUP)) {
					user.setGroup((UserGroup)this.executeSingleResultNamedQuery("UserGroup.findByName", new Object[] {UserGroup.FREELANCER_GROUP.getName()}));
				}

				break;
			}

			case EMPLOYEE: {
				// This one is a little trickier. We need to lookup their
				// username from the LDAP services. We'll use that as their
				// lookup code.
				SSOUser ssoUser = null;
				String lookupCredential = null;

				if (StringUtils.isNotBlank(user.getEmail())) {
					ssoUser = internalSSOUserLookupUtility.findByEmail(user.getEmail());
					lookupCredential = "email = " + user.getEmail();
				}
				// no longer need to keep ldapDN in our database
				/*
				else if (StringUtils.isNotBlank(user.getLdapDN())) {
					ssoUser = internalSSOUserLookupUtility.findByDN(user.getLdapDN());
					lookupCredential = "ldapDN = " + user.getLdapDN();
				}*/

				if (ssoUser == null) {
					// Don't allow creation of a new employee or modifying an existing employee
					// such that the email does not match SSO - except for our special Permissions user.
					if (!user.getFirstName().equals("Permissions") && !(user.getLastName().equals("System"))) {
						//user.setEnabled(false);
						String msg = "saveUser(): Could Not Lookup An Employee From SSO using " + lookupCredential
						+ "\r\nuser: " + user;
					//	throw new RuntimeException(msg);
						throw new ServiceException("E-mail [" + user.getEmail() + "] is not found in ldap, email id doesn't exists or valid......",true);
					}
				}
				else {
					user.setEmail(ssoUser.getEmail());
					user.setFirstName(ssoUser.getFirstName());
					user.setLastName(ssoUser.getLastName());
					//user.setLdapDN(ssoUser.getLdapDN());
					user.setCode(user.getCode());
				}
				if (null == user.getGroup()) {
					user.setGroup((UserGroup)this.executeSingleResultNamedQuery("UserGroup.findByName", new Object[] {UserGroup.getInstance(ssoUser.getLdapGroupName())}));
				}
				break;
			} // end case EMPLOYEE
		} // end switch

		log.debug("saveUser(): about to merge User: " + user);
		List<UserToRole> oldGlobalRoles = user.getGlobalUserToRoles();
		List<UserToRole> oldProductRoles = user.getProductUserToRoles();
		user = merge(user);	
	
		log.debug("saveUser(): about to merge UserToRoles: ");

		if (CollectionUtils.isNotEmpty(oldGlobalRoles)) {
			for (UserToRole old : oldGlobalRoles) {
				old.setUser(user); // to get id's if a new user
				merge(old);
			}
		}

		if (CollectionUtils.isNotEmpty(oldProductRoles)) {
			for (UserToRole old : oldProductRoles) {
				old.setUser(user); // to get id's if a new user
				merge(old);
			}
		}		
		//Added for SS Task 4
		switch (user.getType()) {
		case AUTHOR: {
				if(gid>0){
					 updateUserGroupId(gid,user.getId());
				}
			}
		case FREELANCER: {
				if(gid>0){
					 updateUserGroupId(gid,user.getId());
				}
			}
		}
		//Ends

		return user;
	}
	
	@Transactional(propagation = Propagation.REQUIRED)
	public void flipEnabled(int userId) {
		String sql = "update user_table set enabled = !enabled where id = ?";
		Query query = entityManager.createNativeQuery(sql);
		query.setParameter(1, userId);
		int numRows = query.executeUpdate();
		if (numRows != 1) {
			log.error("flipEnabled(" + userId + "): numRows = " + numRows + " (1 row expected)");
		}
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public User createUserFromSSO(SSOUser ssoUser) throws PermissionsSecurityException, PersistenceException
	{
		log.debug("createUserFromSSO(): entered...");
		User user = new User();
		user.setEnabled(true);
		user.setFirstName(ssoUser.getFirstName());
		user.setLastName(ssoUser.getLastName());
		user.setEmail(ssoUser.getEmail());
		//user.setLdapDN(ssoUser.getLdapDN());  // we no longer store this
		user.setCode(ssoUser.getUserId());

		UserDefaults ud = new UserDefaults();
		ud.setUser(user);
		user.setUserDefaults(ud);

		// example Employee LdapDN: CN=Steve Markoff,OU=San Francisco,OU=United States,OU=North America,OU=Wiley Users,DC=wiley,DC=com
		// example Author LdapDN: CN=john.doe,OU=gmail,OU=com,OU=Wiley Customers,DC=wiley,DC=com
		boolean authorDN = ssoUser.getLdapDN().contains("Wiley Customers");
		if (authorDN) user.setType(User.Type.AUTHOR);

		persist(user);

		if (authorDN) {
			addRoleToUser(user.getId(), Role.AUTHOR_DEFAULT);
			setUserGroup(user.getId(), UserGroup.AUTHOR_GROUP);
		}
		else {
			addRoleToUser(user.getId(), Role.EMPLOYEE_DEFAULT);
			UserGroup userGroup = UserGroup.getInstance(ssoUser.getLdapGroupName());
			log.debug("createUserFromSSO(): userGroup determined from ldapGroupName = " + userGroup);
			setUserGroup(user.getId(), userGroup);
		}

		return user;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void addRoleToUserIfNotThere(int userId, Role role) {
		if (userHasRole(userId, role)) {
			log.debug("addRoleToUserIfNotThere(): userId " + userId
				+ " already assigned role: " + role);
			return;
		}
		log.debug("addRoleToUserIfNotThere(): userId " + userId
				+ " - will assign role (not there): " + role);
		addRoleToUser(userId, role);
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public boolean userHasRole(int userId, Role role) {
		String sql = "select count(*) as count from user_2_role where user_id = ?"
			+ " and role_id = (select id from role where code = ? and role_type = ?)";
		Query query = entityManager.createNativeQuery(sql, "scalarCount");
		query.setParameter(1, userId);
		query.setParameter(2, role.getCode());
		query.setParameter(3, role.getRoleType().name());
		// For count, DB2 and MS SQL Server return Integer but MySQL returns BigInteger
		// Integer and BigInteger inherit from Number
		Number resultNum = (Number) query.getSingleResult();
		return resultNum.intValue() > 0;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void addRoleToUser(int userId, Role role) {
		String sql = "insert into user_2_role (user_id, role_id) values "
			+ "(?, (select id from role where code = ? and role_type = ?) )";
		Query query = entityManager.createNativeQuery(sql);
		query.setParameter(1, userId);
		query.setParameter(2, role.getCode());
		query.setParameter(3, role.getRoleType().name());
		int numRows = query.executeUpdate();
		if (numRows != 1) {
			log.error("addRoleToUser(" + userId + "): numRows = " + numRows + " (1 row expected)");
		}
	}

	/**
	 *
	 * @param userId  Should be valid userId
	 * @param group  Should contain valid group name (id is looked up inside this method based on name)
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public void setUserGroup(int userId, UserGroup group) {
		String sql = "update user_table set user_group_id = (select id from user_group where name = ?) where id = ?";
		Query q = entityManager.createNativeQuery(sql);
		q.setParameter(1, group.getName());
		q.setParameter(2, userId);
		int rows = q.executeUpdate();
		if (rows != 1) {
			log.error("setUserGroup(): updated " + rows + " rows, userId = " + userId + ", userGroup name = " + group.getName());
		}
	}

	/**
	 *
	 * @param userId
	 * @param groupId  Must be valid or < 1 (means null)
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public void updateUserFavoriteGroup(int userId, int groupId) throws PermissionsSecurityException, PersistenceException {
		if (groupId > 0) {
			final String sql = "update user_table set favorite_group_id = ? where id = ?";
			Query q = entityManager.createNativeQuery(sql);
			q.setParameter(1, groupId);
			q.setParameter(2, userId);
			q.executeUpdate();
		}
		else {
			final String sql = "update user_table set favorite_group_id = null where id = ?";
			Query q = entityManager.createNativeQuery(sql);
			q.setParameter(1, userId);
			q.executeUpdate();
		}
	}
	
	/*
	 * Added for Updating the User_group_id for External users
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public void updateUserGroupId(int gId, int id) {
		final String sql = "update user_table set user_group_id = ? where id = ?";		
		Query q = entityManager.createNativeQuery(sql);
		q.setParameter(1, gId);
		q.setParameter(2, id);
		q.executeUpdate();
	}
	
	/**
	 * Returns null if a UserDefaults with the given userId was not found.
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public UserDefaults loadUserDefaults(int userId) {
		TypedQuery<UserDefaults> q = createQuery("from UserDefaults ud where ud.user.id = ?1", UserDefaults.class);
		q.setParameter(1, userId);

		try {
			return q.getSingleResult();
		}
		catch (NoResultException e) {
			return null;
		}
	}

	/**
	 * Returns null if a User with the given code was not found.
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public User loadByCode(String code) {
		TypedQuery<User> q = createQuery("from User u where lower(u.code) = :code", User.class);
		q.setParameter("code", User.normalizeValue(code));

		try {
			return q.getSingleResult();
		}
		catch (NoResultException e) {
			return null;
		}
	}

	/**
	 * This method is just like loadByCode() but designed to be faster in the case where we
	 * only need the user's first + last name combined. Also this method uses a readOnly transaction.
	 *
	 * Returns null if a User with the given code was not found.
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public String loadCombinedNameByCode(String code) {
		final String sql = "select first_name || ' ' || last_name as string from user_table where code = ?";
		Query query = entityManager.createNativeQuery(sql, "scalarString");
		query.setParameter(1, User.normalizeValue(code));

		try {
			return (String) query.getSingleResult();
		}
		catch (NoResultException e) {
			return null;
		}
	}

	/**
	 * Returns null if a User with the given email was not found.
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public User loadByEmail(String email) {
		TypedQuery<User> q = createQuery("from User u where lower(u.email) = :email", User.class);
		q.setParameter("email", email.toLowerCase());

		User user = null;
		try {
			return q.getSingleResult();
		}
		catch (NoResultException e) { }

		log.info("loadByEmail(): found user by email [" + email + "]: " + (user != null));
		return user;
	}

	@SuppressWarnings("unchecked")
	@Transactional(propagation = Propagation.REQUIRED)
	public List<User> getAllUsersByGlobalRole(Role role)
	{
		String sql = "select * from user_table u, user_2_role map, role r "
			+ "where u.id = map.user_id and map.role_id = r.id and r.code = ? and r.role_type = ?";
		Query q = createNativeQuery(sql, User.class);
		q.setParameter(1, role.getCode());
		// if just have role.getRoleType() doesn't give error but don't get any results
		q.setParameter(2, role.getRoleType().name());
		return q.getResultList();
	}

	/**
	 * Returns null if not found.
	 *
	 * @param role  Must be non-null
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public Role loadRoleByTypeAndCode(Role role) throws PersistenceException {
		return loadRoleByTypeAndCode(role.getRoleType(), role.getCode());
	}

	/**
	 * Returns null if not found.
	 *
	 * @param type  Must be non-null
	 * @param code  Should be non-blank
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public Role loadRoleByTypeAndCode(RoleType type, String code)
	throws PersistenceException
	{
		try {
			TypedQuery<Role> query = createQuery(
			    "from Role r where r.roleType = ?1 and r.code = ?2", Role.class);
			query.setParameter(1, type);  // note type.toString() give error, JPA wants object
		    query.setParameter(2, code);
		    return query.getSingleResult();
		}
		catch (NoResultException e) {
			log.debug("loadRoleByTypeAndCode(): failed to load by type ["
					+ type + " ] and code [" + code + "]");
			return null;
		}
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public List<UserSubsetView> loadAllUserSubsetList() {
		PerfTimer timer = monitor.startTimer("UserRepository::loadAllUserSubsetList");

		// load result sets in view objects
		String queryStr =
				"select new com.wiley.permissions.services.view.UserSubsetView(u.id, u.type, u.enabled, " +
						"u.email, u.firstName, u.lastName) FROM User as u";

		// http://opensource.atlassian.com/projects/hibernate/browse/HHH-5348
		// TypedQuery does not work with view objects
		// TypedQuery<UserSubset> query = createQuery(queryStr, UserSubset.class);

		Query query = createQuery(queryStr);
		@SuppressWarnings("unchecked")
		List<UserSubsetView> list = query.getResultList();

		timer.stopTimer();
		return list;
	}

	public List<UserProfile> loadProfile(int userId) {
		Query query = entityManager.createNativeQuery(
				"select * from user_profile where user_id = :id " +
				"	union " +
				"select * from user_profile where user_id = 1 and " +
				"	name not in (select name from user_profile where user_id = :id) " +
				"order by sort_order", UserProfile.class);
		query.setParameter("id", userId);
		@SuppressWarnings("unchecked")
		List<UserProfile> list = query.getResultList();
		return list;
	}

	public UserProfile loadProfileField(int userId, String field) {
		TypedQuery<UserProfile> query = entityManager.createNamedQuery(
				"UserProfile.findByUserIdAndName", UserProfile.class);
		query.setParameter("id", userId);
		query.setParameter("name", field);

		return query.getSingleResult();
	}

	/**
	 * will duplicate the master field and set the visible flag to true
	 * the user profile will contain just those fields that are different them MASTER profile
	 * @param userId
	 * @param field
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws PersistenceException
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public void saveProfileField(int userId, UserProfileView field)
	throws IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException, PersistenceException
	{
		UserProfile masterField = loadProfileField (MASTER_USER_ID, field.getName());
		UserProfile pField = (UserProfile) BeanUtils.cloneBean(masterField);
		// new record
		pField.setId(null);
		User owner = new User();
		owner.setId(userId);
		pField.setUser(owner);
		// for now we just set the visible flag
		pField.setVisible(field.isVisible());
		pField.setSortOrder(field.getSortOrder());
		merge(pField);
	}

	/**
	 * Called by SecurityServiceImpl.saveProfile (needs to be in a separate class
	 * so can say REQUIRES_NEW.
	 *
	 * @param userId
	 * @throws PersistenceException
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void deleteProfile(int userId) throws PersistenceException
	{
		String sql = "delete from UserProfile where user.id = ?1";

		Query q = entityManager.createQuery(sql);
		q.setParameter(1, userId);
		q.executeUpdate();
	}

	/**
	 * This method does an exact match (not starts with) (but is case insensitive).
	 * If you need "starts with" use searchAuthorsByLastName().
	 *
	 * Returns an empty list if nothing is found.
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public List<User> loadAuthorsByLastName(String lastName) throws PersistenceException
	{
		String sql = "from User u where UPPER(u.lastName) = ?1 "
			+ "and (u.type = 'AUTHOR' or u.type = 'FREELANCER')";
		TypedQuery<User> query = entityManager.createQuery(sql, User.class);
		query.setParameter(1, lastName.toUpperCase());
		return query.getResultList();
	}

	/**
	 * This method uses a "like" clause ("starts with") (and is case insensitive).
	 * If you need an exact match use loadAuthorsByLastName().
	 *
	 * Returns an empty list if nothing is found.
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<User> searchAuthorsByLastName(String lastName) throws PersistenceException
	{
		long startTime = System.currentTimeMillis();
		Query query = entityManager.createNativeQuery(
				"select * from user_table where UPPER(last_name) like ? " +
				"and (user_type = 'AUTHOR' or user_type = 'FREELANCER') "  +
				"order by last_name", User.class);

		if (null != lastName && lastName.indexOf(",") > -1) {
			String lname = lastName.substring(0,lastName.indexOf(",")).trim();
			String fname = "";

			if (lastName.length() > lastName.indexOf(",") + 1) {
				fname = lastName.substring(lastName.indexOf(",") + 1).trim();

				query = entityManager.createNativeQuery(
						"select * from user_table where (UPPER(last_name) like UPPER(:lname) and UPPER(first_name) like UPPER(:fname)) " +
						"and (user_type = 'AUTHOR' or user_type = 'FREELANCER') "  +
						"order by last_name", User.class);
				query.setParameter("lname", "%" + lname + "%");
				query.setParameter("fname", "%" + fname + "%");
			} else {
				query.setParameter(1, lastName.toUpperCase() + "%");
			}
		} else {
			query.setParameter(1, lastName.toUpperCase() + "%");
		}

		@SuppressWarnings("unchecked")
		List<User> list = query.getResultList();
		long time = System.currentTimeMillis() - startTime;
		log.debug("searchAuthorsByName(): time was " + time + " ms.");
		return list;
	}

	/**
	 * Returns a Set of global privilege codes.
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public Set<String> getGlobalPrivileges() {
		final String sql = "select code from privilege where is_global is true";
		Query q = createNativeQuery(sql, "scalarCode");
		@SuppressWarnings("unchecked")
		List<String> list = q.getResultList();
		Set<String> set = new HashSet<String>();
		set.addAll(list);
		return set;
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public Set<PrivilegePrincipal> getGlobalPrivileges(int userId) {
		final String sql = "select * from privilege p, user_2_privilege map"
			+ " where map.user_id = ? and map.cw_id is null"
			+ " and p.code = map.privilege_code";
		Query query = entityManager.createNativeQuery(sql, Privilege.class);
		query.setParameter(1, userId);
		@SuppressWarnings("unchecked")
		List<Privilege> list = query.getResultList();
		Set<PrivilegePrincipal> privileges = new HashSet<PrivilegePrincipal>();
		for (Privilege priv : list) {
			PrivilegePrincipal rp = new PrivilegePrincipal();
			log.debug("getGlobalPrivileges(): privilege code " + priv.getCode());
			rp.setCode(priv.getCode());
			privileges.add(rp);
		}
		return privileges;
	}

	/**
	 * Also checks for author_ok == true
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public Set<PrivilegePrincipal> getCwPrivileges(int userId, int cwId) {
		// and p.author_ok isn't strictly necessary because nothing that isn't
		// author_ok should have been assigned but check anyway
		final String sql = "select * from privilege p, user_2_privilege map"
			+ " where map.user_id = ? and map.cw_id = ?"
			+ " and p.code = map.privilege_code and p.author_ok = true";
		Query query = entityManager.createNativeQuery(sql, Privilege.class);
		query.setParameter(1, userId);
		query.setParameter(2, cwId);
		@SuppressWarnings("unchecked")
		List<Privilege> list = query.getResultList();
		Set<PrivilegePrincipal> privileges = new HashSet<PrivilegePrincipal>();
		for (Privilege priv : list) {
			PrivilegePrincipal rp = new PrivilegePrincipal();
			log.debug("getCwPrivileges(): privilege code " + priv.getCode());
			rp.setCode(priv.getCode());
			privileges.add(rp);
		}
		return privileges;
	}

	/**
	 * This returns data from our author_2_cw table, NOT the PE Authors.
	 *
	 * @param cwId  Should be a valid cwId
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<User> getAuthorAccessForCommonWork(int cwId) {
		final String sql = "select u.* from user_table u, author_2_cw map"
			+ " where map.cw_id = ? and u.id = map.user_id";
		Query query = entityManager.createNativeQuery(sql, User.class);
		query.setParameter(1, cwId);
		@SuppressWarnings("unchecked")
		List<User> list = query.getResultList();
		// set transient values needed by authorAccess.jspx
		for (User u2p : list) {
			try {
				AuthorToCommonWork a2cw = find(AuthorToCommonWork.class, new AuthorCommonWorkPK (u2p.getId(), cwId));
				u2p.setCwReadOnly(a2cw.isReadOnly());
				u2p.setPermissionFlag(a2cw.isPermissionComplete());
			} catch (Exception e) {
				// do nothing
			}
		}

		return list;
	}

	/**
	 * Returns all the privileges that are possible for authors.
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<Privilege> getCompleteAuthorPrivileges(boolean global) {
		final String jpql = "from Privilege p where author_ok is true and is_global is " + global;
		TypedQuery<Privilege> query = entityManager.createQuery(jpql, Privilege.class);
		return query.getResultList();
	}

	/**
	 * Returns the privileges (cw-specific and global) that the given userId
	 * (author) currently has.
	 * Will return an empty list if privileges have never been set for the author
	 * or the author has not been given any privileges.
	 *
	 * @param userId  Should be valid
	 * @param cwId  May be null to indicate global privileges
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<String> getSelectedAuthorPrivileges(int userId, Integer cwId) {
		// "and cw_id = ?" does not work if ? is null -- "cw_id is ?" and "cw_id is null" both work
		final String sql = "select privilege_code as code from user_2_privilege"
			+ " where user_id = ? and " + ((cwId == null) ? "cw_id is ?" : "cw_id = ?");
		Query q = createNativeQuery(sql, "scalarCode");
		q.setParameter(1, userId);
		q.setParameter(2, cwId);
		@SuppressWarnings("unchecked")
		List<String> list = q.getResultList();
		return list;
	}

	/**
	 *
	 * @param authorId
	 * @param cwId  Should be null to indicate global privileges
	 * @param selectedPrivileges
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public void saveSelectedAuthorPrivileges(int authorId, Integer cwId, String [] selectedPrivileges) {
		// "and cw_id = ?" does not work if ? is null - "cw_id is ?" and "cw_id is null" both work
		final String deleteSql = "delete from user_2_privilege where user_id = ? and "
			+ ((cwId == null) ? "cw_id is ?" : "cw_id = ?");
		Query q = createNativeQuery(deleteSql);
		q.setParameter(1, authorId);
		q.setParameter(2, cwId);
		int numRows = q.executeUpdate();
		log.debug("saveSelectedAuthorPrivileges(): deleted " + numRows + " rows for cwId = " + cwId);

		final String insertSql = "insert into user_2_privilege (user_id, cw_id, privilege_code) values (?, ?, ?)";
		q = createNativeQuery(insertSql);
		int insertCount = 0;
		for (String code : selectedPrivileges) {
			q.setParameter(1, authorId);
			q.setParameter(2, cwId);
			q.setParameter(3, code);
			insertCount += q.executeUpdate();
		}
		log.debug("saveSelectedAuthorPrivileges(): inserted " + insertCount + " rows for cwId = " + cwId);
	}

	/**
	 * Returns the default profile of author privileges for the given user (not the author).
	 * Both CW (Common Work) and Global privileges are returned.
	 *
	 * @param user_id  Should be for the current user (not the author)
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<String> getAuthorDefaultPrivileges(int userId) {
		final String sql = "select privilege_code as code from author_default_privilege where user_id = ?";
		Query q = createNativeQuery(sql, "scalarCode");
		q.setParameter(1, userId);
		@SuppressWarnings("unchecked")
		List<String> list = q.getResultList();
		return list;
	}

	/**
	 * This method should be called with a combined list of cw and global privileges.
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public void saveAuthorDefaultPrivileges(int userId, List<String> selectedPrivileges) {
		final String deleteSql = "delete from author_default_privilege where user_id = ?";
		Query q = createNativeQuery(deleteSql);
		q.setParameter(1, userId);
		int numRows = q.executeUpdate();
		log.debug("saveAuthorDefaultPrivileges(): deleted " + numRows + " rows.");

		final String insertSql = "insert into author_default_privilege (user_id, privilege_code) values (?, ?)";
		q = createNativeQuery(insertSql);
		int insertCount = 0;
		for (String code : selectedPrivileges) {
			q.setParameter(1, userId);
			q.setParameter(2, code);
			insertCount += q.executeUpdate();
		}
		log.debug("saveAuthorDefaultPrivileges(): inserted " + insertCount + " rows.");
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void removeAuthorToCommonWorkAssociation(int userId, int cwId) {
		final String updateSql = "delete from author_2_cw where user_id = ? and cw_id = ?";
		Query q = createNativeQuery(updateSql);
		q.setParameter(1, userId);
		q.setParameter(2, cwId);
		int numRows = q.executeUpdate();
		if (numRows != 1) {
			throw new RuntimeException("Number of updated rows expected to be 1 but was "
				+ numRows + " - userId [" + userId + "] cwId [" + cwId + "]");
		}
	}


	/**
	 * This returns a list of primary products based on author_2_cw table.
	 * Only returns products not in (PRE_CONTRACT, EDITORIAL, IN_PRODUCTION)
	 * or where the cw is permissionComplete.
	 *
	 * @param userId  Should be a valid userId (the author userId)
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<Product> getArchivedProductsForAuthor(int userId) {
		// cannot do join fetch because of the mapping table, so we just lazy load data for each product
		final String sql = "select p.* from product p, author_2_cw map, common_work cw"
			+ " where map.user_id = ? and p.cw_id = map.cw_id and p.is_cw_primary = true"
			+ " and cw.id = map.cw_id"
			+ " and (cw.interior_cw_status != '" + CommonWorkStatus.IN_PROGRESS.getCode() + "'"
			+ "   or p.pub_status not in " + PublicationStatus.getPreProductionSQL(true)
			+ "   or (p.pub_status = '" + PublicationStatus.IN_PRODUCTION.getCode() + "'"
			+ "     and cw.id not in (select cw_id from user_2_privilege where user_id = ?"
			+ "       and privilege_code = '" + Privilege.IN_PRODUCTION_ACTIVE.getCode() + "')))";
		Query query = entityManager.createNativeQuery(sql, Product.class);
		query.setParameter(1, userId);
		query.setParameter(2, userId);
		@SuppressWarnings("unchecked")
		List<Product> list = query.getResultList();
		// lazy load data
		for (Product product : list) {
			product.getAuthorsAsString();
			if (null != product.getPublicationStatus())
				product.getPublicationStatus().getDescription();
			if (null != product.getMedium())
				product.getMedium().getName();
		}
		return list;
	}

	/**
	 * This returns a list of primary products based on author_2_cw table.
	 * Only returns products IN (PRE_CONTRACT, EDITORIAL, IN_PRODUCTION)
	 * and where the cw is !permissionComplete.
	 *
	 * @param userId  Should be a valid userId (the author userId)
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<Product> getCurrentProductsForAuthor(int userId) {
		// cannot do join fetch because of the mapping table, so we just lazy load data for each product
		String sql = "select p.* from product p, author_2_cw map, common_work cw"
			+ " where map.user_id = ? and p.cw_id = map.cw_id and p.is_cw_primary = true"
			+ " and cw.id = map.cw_id"
			+ " and cw.interior_cw_status = '" + CommonWorkStatus.IN_PROGRESS.getCode() + "'"
			+ " and (p.pub_status in " + PublicationStatus.getPreProductionSQL(false)
			+ "   or (p.pub_status = '" + PublicationStatus.IN_PRODUCTION.getCode() + "'"
			+ "       and cw.id in (select cw_id from user_2_privilege where user_id = ?"
			+ "         and privilege_code = '" + Privilege.IN_PRODUCTION_ACTIVE.getCode() + "')))";
		log.debug("getCurrentProductsForAuthor(): sql: " + sql);
		Query query = entityManager.createNativeQuery(sql, Product.class);
		query.setParameter(1, userId);
		query.setParameter(2, userId);
		@SuppressWarnings("unchecked")
		List<Product> list = query.getResultList();
		// lazy load data
		for (Product product : list) {
			product.getCommonWork().getId();
		}
		return list;
	}

	/**
	 * This method currently does NOT WORK, see comment below in code.
	 *
	 * This returns a list of common works based on author_2_cw table.
	 *
	 * @param userId  Should be a valid userId (the author userId)
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<CommonWork> getCurrentCWsForAuthor(int userId) {
		//TODO: enhance to only load cw's whose primary products are IN_PRODUCTION
		// and where the cw is !permissionComplete

		// Unfortunately neither of these queries works -- cause NullPointerException in
		// Hibernate - I think it's a bug in Hibernate, maybe related to fact that CommonWork
		// object has Formula fields

		//String sql = "select cw.* from common_work cw, author_2_cw map"
		//	+ " where map.user_id = ? and cw.id = map.cw_id";
		String sql = "select cw from CommonWork cw where id in (select cw_id from author_2_cw where user_id = ?1)";
		TypedQuery<CommonWork> query = entityManager.createQuery(sql, CommonWork.class);
		query.setParameter(1, userId);
		List<CommonWork> list = query.getResultList();
		return list;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public List<Source> loadFavoriteSources(Integer userId) throws Exception
	{
		User user = lazyLoad (User.class, userId, new String[] {"sources", "favoriteGroup"});
		List<Source> profileSources = user.getSources();
		// https://www.pivotaltracker.com/story/show/43232173 bug fix - to not save the favorite sources as profile sources
		List<Source> returnedSources = new ArrayList<Source> ();
		CollectionUtils.addAll(returnedSources, profileSources.iterator());

		// if user has a favorite profile, do a union of the sources in it and the my sources from user profile
		if (null != user.getFavoriteGroup()) {
			FavoriteGroup fab = find(FavoriteGroup.class, user.getFavoriteGroup().getId());
			for (Integer x = 0; x < fab.getSources().size(); x++) {
				if (!returnedSources.contains(fab.getSources().get(x))) {
					returnedSources.add(fab.getSources().get(x));
				}
			}
		}

		// sort combined source list by source name
		Collections.sort(returnedSources, new Comparator<Source>() {

			@Override
			public int compare(Source p1, Source p2) {
				String t1 = "";
				String t2 = "";
				if (null != p1.getExternalName())
					t1 = p1.getExternalName();
				if (null != p2.getExternalName())
					t2 = p2.getExternalName();
				return t1.compareToIgnoreCase(t2);
			}
		});
		return returnedSources;
	}

	/**
	 * Returns a list of components for a commonWork
	 * @param cwId
	 * @return List<Component>
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<UserGroup> loadUserGroupList () {
		String sql = "from UserGroup";

		TypedQuery<UserGroup> query = entityManager.createQuery(sql, UserGroup.class);
		return query.getResultList();
	}

	public void setInternalSSOUserLookupUtility(SSOUserLookupUtility internalSSOUserLookupUtility) {
		this.internalSSOUserLookupUtility = internalSSOUserLookupUtility;
	}

	public DataSource getDataSource() {
		return dataSource;
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public boolean doesExportInProgress(Integer cw_id) {
		final String sql = "select count(*) as count from export_asset where cw_id=? and export_status = ?";
		Query query = entityManager.createNativeQuery(sql, "scalarCount");
		query.setParameter(1, cw_id);
		query.setParameter(2, "in_progress");
		// For count, DB2 and MS SQL Server return Integer but MySQL returns BigInteger
		// Integer and BigInteger inherit from Number
		Number result = (Number) query.getSingleResult();
		return result.intValue() > 0;  // (for externalId we expect 0 or 1 rows)
	}
}
