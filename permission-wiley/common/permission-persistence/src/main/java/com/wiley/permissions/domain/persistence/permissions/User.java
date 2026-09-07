package com.wiley.permissions.domain.persistence.permissions;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.PersistenceUnit;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlID;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.permissions.common.bean.Merge;
import com.wiley.permissions.common.bean.Merge.PropertyProtection;
import com.wiley.permissions.domain.MaterializationKey;

@Entity
@PersistenceUnit(unitName = "permissions")
@Table(name = "USER_TABLE")
@NamedQueries ({
	@NamedQuery (
	    name="User.findByFirstName",
	    query="from User user where firstName like :firstName"
	)
	,
	@NamedQuery (
	    name="User.findByLastName",
	    query="from User user where lastName like :lastName"
	),
	@NamedQuery (
		name="User.findByFirstNameLastName",
		query="from User user where lastName like :lastName and firstName like :firstName "
	),
	@NamedQuery (
		name="User.findUsersWithNoGroup",
		query="from User user where group is null"
	)
})
public class User extends AuditBase {
	private static final long serialVersionUID = 1L;

	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(User.class);

	public static final User SYSTEM = new User() {{
		setId(1);
		setType(Type.SYSTEM);
	}};

	@XmlEnum
	public enum Type {
		@XmlEnumValue("EMPLOYEE") EMPLOYEE,
		@XmlEnumValue("AUTHOR")	AUTHOR,
		@XmlEnumValue("FREELANCER") FREELANCER,
		@XmlEnumValue("SYSTEM") SYSTEM;

		// for easy access in jspx
		public String getName() {
		    return name();
		}
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID", nullable = false, unique = true)
	@Merge(propertyProtection = PropertyProtection.NEVER_MERGE)
	private Integer id = null;

	// maybe - @Valid
	@Enumerated(EnumType.STRING)
	@Column(name = "USER_TYPE", length = 20, nullable = false)
	private User.Type type = User.Type.EMPLOYEE;

	@Column(name = "ENABLED", nullable = false)
	@Merge(propertyProtection = PropertyProtection.NEVER_MERGE)
	private boolean enabled = true;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "USER_GROUP_ID", nullable = true)
	@Merge(propertyProtection = PropertyProtection.NEVER_MERGE)
	private UserGroup group;

	@Size (max=256)
	@Column(name = "EMAIL", nullable = true, length = 256)
	@MaterializationKey(ignoreCase = true)
	@Merge(propertyProtection = PropertyProtection.NEVER_MERGE)
	private String email;

	@NotNull
	@Size (max=50)
	@Column(name = "FIRST_NAME", nullable = false, length = 50)
	@Merge(propertyProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private String firstName;

	@NotNull
	@Size (max=50)
	@Column(name = "LAST_NAME", nullable = false, length = 50)
	@Merge(propertyProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private String lastName;

	@NotNull
	@Size (max=200)
	@Column(name = "CODE", nullable = false, length = 20)
	@MaterializationKey(ignoreCase = true, alwaysTrim = true)
	@Merge(propertyProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private String code;

	@Size (max=15)
	@Column(name = "PHONE", nullable = true, length = 15)
	@Merge(propertyProtection = PropertyProtection.NEVER_MERGE)
	private String phone;

	@Size (max=100)
	@Column(name = "STREET", nullable = true, length = 100)
	@Merge(propertyProtection = PropertyProtection.NEVER_MERGE)
	private String street;

	@Size (max=100)
	@Column(name = "CITY", nullable = true, length = 100)
	@Merge(propertyProtection = PropertyProtection.NEVER_MERGE)
	private String city;

	@Size (max=10)
	@Column(name = "ZIP", nullable = true, length = 10)
	@Merge(propertyProtection = PropertyProtection.NEVER_MERGE)
	private String zip;

	@Size (max=50)
	@Column(name = "STATE", nullable = true, length = 50)
	@Merge(propertyProtection = PropertyProtection.NEVER_MERGE)
	private String state;

	// cannot link to Country entity as they are from different persistence units
	@Size (max=5)
	@Column(name = "COUNTRY_CODE", nullable = true)
	private String countryCode;

	@Transient
	private Country country;

	// this transient is only valid within the context of a commonWork
	// and is loaded by userRepository.getAuthorAccessForCommonWork{}
	@Transient
	private Boolean cwReadOnly = false;
	
	//Added for SS task 16 
	//Boolean used to display the Checked box indicating the author has completed
	//Permission work on that product
	@Transient
	private Boolean permissionFlag = false;	
	
	//Added for Updating the authors/Freelancers to internal Users
	@Transient
	private String viewType;	

	// // @Merge(propertyProtection=PropertyProtection.NEVER_MERGE)
	// // need cascade on MERGE so admin UI works where add a Product Role to a User
	// @OneToMany(fetch = FetchType.LAZY, mappedBy = "user", cascade =
    // {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})
	@Transient
	private List<UserToRole> roles = new ArrayList<UserToRole>();

	@ManyToMany
	@JoinTable(name = "WATCHED_CW", joinColumns = @JoinColumn(name = "USER_ID"), inverseJoinColumns = @JoinColumn(name = "CW_ID"))
	@Merge(propertyProtection = PropertyProtection.NEVER_MERGE)
	private List<CommonWork> watchedCommonWorks = new ArrayList<CommonWork>();

	@ManyToMany
	@JoinTable(name = "AUTHOR_2_CW", joinColumns = @JoinColumn(name = "USER_ID"), inverseJoinColumns = @JoinColumn(name = "CW_ID"))
	@Merge(propertyProtection = PropertyProtection.NEVER_MERGE)
	private List<CommonWork> authorCommonWorks = new ArrayList<CommonWork>();

	@OneToOne(cascade = CascadeType.ALL, mappedBy = "user")
	@Merge(propertyProtection = PropertyProtection.NEVER_MERGE)
	private UserDefaults userDefaults = null;

	@OneToMany (fetch=FetchType.LAZY)
	@OrderBy( "name ASC")
	@JoinTable(name = "USER_2_SOURCE", joinColumns = @JoinColumn(name = "USER_ID"), inverseJoinColumns = @JoinColumn(name = "SOURCE_ID"))
	@Merge(propertyProtection = PropertyProtection.NEVER_MERGE)
	private List<Source> sources;
	
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name = "FAVORITE_GROUP_ID")
	@Merge(propertyProtection = PropertyProtection.NEVER_MERGE)
	private FavoriteGroup favoriteGroup;


	public User() {
		super();
	}

	@Transient
	public String getFullName() {
		String name = lastName;
		if (StringUtils.isNotBlank(firstName))
			name += ", " + firstName;
		return name;
	}

	public static String normalizeValue(String in) {
		if (in == null)  return null;
		else return in.trim().toLowerCase();
	}

	public boolean hasRole(Role role) {
		return hasRole(null, role);
	}

	public boolean hasRole(Integer productId, Role role) {
		UserToRole u2r = getUserToRole (productId, role);
		return u2r != null;
	}

	public UserToRole getUserToRole(Role role) {
		return getUserToRole (null, role);
	}

	public UserToRole getUserToRole(Integer productId, Role role) {
		for (UserToRole u2p : roles) {
			if ((null == productId && u2p.getProduct() == null)
					|| (null != productId && u2p.getProduct() != null && u2p
							.getProduct()
							.getId()
							.equals(productId)))
			{
				if (u2p.getRole().equals(role)) {
					return u2p;
				}
			}
		}

		return null;
	}

	@Transient
	public List<Role> getGlobalRoles() {
		List<UserToRole> globalU2R = getGlobalUserToRoles ();
		List<Role> global = new ArrayList<Role> ();

		for (UserToRole u2p : globalU2R) {
			global.add(u2p.getRole());
		}

		return global;
	}

	@Transient
	public List<UserToRole> getGlobalUserToRoles() {
		List<UserToRole> global = new ArrayList<UserToRole>();

		for (UserToRole u2r : getRoles()) {
			if (null == u2r.getProduct())
				global.add(u2r);
		}

		return global;
	}

	@Transient
	public List<UserToRole> getProductUserToRoles() {
		List<UserToRole> productRoles = new ArrayList<UserToRole>();

		for (UserToRole u2r : getRoles()) {
			if (null != u2r.getProduct())
				productRoles.add(u2r);
		}

		return productRoles;
	}

	/**
     * Add a role to the user.
	 * Create an association object for the relationship and set its data.
     * The calling method should persist the returned UserToRole object.
     */
	@Transient
	public UserToRole addGlobalRole(Role role) {
		return addProductRole(null, role);
	}

	/**
     * Add a role to the user.
	 * Create an association object for the relationship and set its data.
     * The calling method should persist the returned UserToRole object.
     */
	@Transient
	public UserToRole addProductRole(Product product, Role role) {
		UserToRole u2r = new UserToRole();
		u2r.setRole(role);
		u2r.setProduct(product);
		u2r.setUser(this);
		roles.add(u2r);
		return u2r;
	}

	@XmlElement
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@XmlElement
	public User.Type getType() {
		return type;
	}

	public void setType(User.Type type) {
		this.type = type;
	}

	@Transient
	public void setType(Object type) {
		this.type = (User.Type) type;
	}

	@XmlElement(name = "enabled")
	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public UserGroup getGroup() {
		return group;
	}

	public void setGroup(UserGroup group) {
		this.group = group;
	}

	@XmlElement(name = "email")
	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = StringUtils.trimToNull(email);
	}

	@XmlElement
	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = StringUtils.trimToNull(firstName);
	}

	@XmlElement
	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = StringUtils.trimToNull(lastName);
	}

	@XmlElement
	@XmlID
	public String getCode() {
		return normalizeValue(code);
	}

	public void setCode(String code) {
		this.code = normalizeValue(code);
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = StringUtils.trimToNull(phone);
	}

	public String getStreet() {
		return street;
	}

	public void setStreet(String street) {
		this.street = StringUtils.trimToNull(street);
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = StringUtils.trimToNull(city);
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = StringUtils.trimToNull(state);
	}

	public String getZip() {
		return zip;
	}

	public void setZip(String zip) {
		this.zip = StringUtils.trimToNull(zip);
	}

	public String getCountryCode() {
		return countryCode;
	}

	public void setCountryCode(String countryCode) {
		this.countryCode = countryCode;
	}

	@Transient
	public Country getCountry() {
		return country;
	}
	@Transient
	// this transient is only valid within the context of a commonWork
	// and is loaded by userRepository.getAuthorAccessForCommonWork{}
	public void setCwReadOnly(Boolean cwReadOnly) {
		this.cwReadOnly = cwReadOnly;
	}

	// this transient is only valid within the context of a commonWork
	// and is loaded by userRepository.getAuthorAccessForCommonWork{}
	@Transient
	public Boolean getCwReadOnly() {
		return cwReadOnly;
	}
	
	// this transient is only valid within the context of a commonWork
	// and is loaded by userRepository.getAuthorAccessForCommonWork{}
	@Transient
	public Boolean getPermissionFlag() {
		return permissionFlag;
	}
    
	// this transient is only valid within the context of a commonWork
	// and is loaded by userRepository.getAuthorAccessForCommonWork{}
	@Transient
	public void setPermissionFlag(Boolean permissionFlag) {
		this.permissionFlag = permissionFlag;
	}
	
	//Added for Updating the authors/Freelancers to internal Users
	@Transient
	public String getViewType() {
			return viewType;
	}
	//Added for Updating the authors/Freelancers to internal Users
	@Transient
	public void setViewType(String viewType) {
			this.viewType = viewType;
	}

	public void setCountry(Country country) {
		this.country = country;
		setCountryCode(country == null ? null : country.getCode());
	}

	@XmlElementWrapper(name = "roles")
	@XmlElement(name = "role")
	//@XmlIDREF
	public List<UserToRole> getRoles() {
		return roles;
	}

	public void setRoles(List<UserToRole> roles) {
		this.roles = roles;
	}

	public List<CommonWork> getWatchedCommonWorks() {
		return watchedCommonWorks;
	}

	public void setWatchedCommonWorks(List<CommonWork> watchedCommonWorks) {
		this.watchedCommonWorks = watchedCommonWorks;
	}

	public List<CommonWork> getAuthorCommonWorks() {
		return authorCommonWorks;
	}

	public void setAuthorCommonWorks(List<CommonWork> authorCommonWorks) {
		this.authorCommonWorks = authorCommonWorks;
	}

	public UserDefaults getUserDefaults() {
		return userDefaults;
	}

	public void setUserDefaults(UserDefaults userDefaults) {
		this.userDefaults = userDefaults;
	}

	public List<Source> getSources() {
		return sources;
	}

	public void setSources(List<Source> sources) {
		this.sources = sources;
	}

	public FavoriteGroup getFavoriteGroup() {
		return favoriteGroup;
	}

	public void setFavoriteGroup(FavoriteGroup favoriteGroup) {
		this.favoriteGroup = favoriteGroup;
	}

	@Transient
	public void addSource(Source source) {
		if (!sources.contains(source)) {
			sources.add(source);
		}
	}

	@Transient
	public void removeSource(Source source) {
		if (sources.contains(source)) {
			sources.remove(source);
		}
	}

	/**
	 * Base on id (could switch to code since also unique).
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		// call getId() since using JPA - not sure if necessary
		result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
		return result;
	}

	/**
	 * Base on id (could switch to code since also unique).
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		// Important to use "instance of" due to JPA proxies
		//if (getClass() != obj.getClass()) return false;
		if (!(obj instanceof User)) return false;
		User other = (User) obj;
		if (getId() == null) {
			if (other.getId() != null) return false;
		}
		else if (!getId().equals(other.getId())) return false;
		return true;
	}

	@Override
	public String toString() {
		// use getters due to way JPA works
		return super.toString() + ",\r\nid = " + getId() + ", code = " + getCode() + ", type = " + getType()
			+ ",\r\nenabled = " + isEnabled() + ", firstName = " + getFirstName() + ", lastName = " + getLastName()
			+ ",\r\nemail = " + getEmail()
			+ ", (other attributes not shown)";
	}
}
