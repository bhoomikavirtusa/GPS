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
import javax.persistence.OneToMany;
import javax.persistence.PersistenceUnit;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.wiley.permissions.common.bean.Merge;
import com.wiley.permissions.common.bean.Merge.PropertyProtection;
import com.wiley.permissions.domain.DomainObject;
import com.wiley.permissions.domain.MaterializationKey;
import com.wiley.permissions.domain.MaterializationKey.Mode;

@Entity
@PersistenceUnit(unitName="permissions")
@Table(name="ROLE", uniqueConstraints = @UniqueConstraint(columnNames={"CODE", "ROLE_TYPE"}))
@Cache (usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class Role
extends DomainObject
{
	private static final long serialVersionUID = 1L;

	// role constants - these should match the database exactly
	public static final Role AUTHOR = new Role(RoleType.AUTHOR, "A", "Author", false);

	// global roles
	public static final Role AUTHOR_DEFAULT = new Role(RoleType.AUTHOR, "AUTHOR_DEFAULT", "Default Author", true);
	public static final Role EMPLOYEE_DEFAULT = new Role(RoleType.EMPLOYEE, "EMPLOYEE_DEFAULT", "Default Employee", true);
	public static final Role SUPER = new Role(RoleType.EMPLOYEE, "SUPER", "Super User", true);
	public static final Role ADMIN = new Role(RoleType.EMPLOYEE, "ADMIN", "Administrator", true);
	public static final Role DEV = new Role(RoleType.EMPLOYEE, "DEV", "Developer", true);
	public static final Role PRINT_RUN_ADMIN = new Role(RoleType.EMPLOYEE, "PRINT_RUN_ADMIN", "Print Run Admin", true);
	public static final Role ADMIN_EMAIL_RECEIVER = new Role(RoleType.EMPLOYEE, "ADMIN_EMAIL_RECEIVER", "Admin role for receiving emails", true);
	public static final Role RFDEAL_MAIL_RECEIVER = new Role(RoleType.EMPLOYEE, "RFDEAL_MAIL_RECEIVER", "Admin role for receiving Royalty Free Deal emails", true);
	public static final Role SERVICE_DESK = new Role(RoleType.EMPLOYEE, "SERVICE_DESK", "L1 role for providing GPS access to the users", true);

	// product roles
	public static final Role EDITOR_AUTHOR = new Role(RoleType.AUTHOR, "E", "Editor", false);
	public static final Role EDITOR_EMPLOYEE = new Role(RoleType.EMPLOYEE, "ED", "Editor", false);
	public static final Role PRODUCTION_EDITOR = new Role(RoleType.EMPLOYEE, "PE", "Production Ed.", false);
	public static final Role PHOTO_EDITOR = new Role(RoleType.EMPLOYEE, "PHE", "Photo Editor", false);
	public static final Role PHOTO_RESEARCHER = new Role(RoleType.EMPLOYEE, "PHO", "Photo Researcher", false);
	public static final Role DEVELOPMENT_EDITOR = new Role(RoleType.EMPLOYEE, "DE", "Development Ed.", false);
	public static final Role DESIGNER_HIGHER_ED = new Role(RoleType.EMPLOYEE, "DSN", "Designer Higher Ed", false);
	public static final Role DESIGNER_WCS = new Role(RoleType.EMPLOYEE, "CDS", "WCS Designer", false);


	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID", nullable = false, unique = true)
	private Integer id = null;

	@XmlEnum
	public enum RoleType
	{
		@XmlEnumValue("AUTHOR")
		AUTHOR,
		@XmlEnumValue("EMPLOYEE")
		EMPLOYEE
	}

	@Enumerated(EnumType.STRING)
	@Column(name = "ROLE_TYPE", nullable = false, length = 20)
	@MaterializationKey(mode=Mode.ADDITIVE)
	private RoleType roleType = null;

	@Column(name = "CODE", nullable = false, length = 20)
	@MaterializationKey(mode=Mode.ADDITIVE)
	@Merge(propertyProtection=PropertyProtection.NEVER_MERGE)
	private String code = null;

	@Column(name="IS_GLOBAL", nullable=false)
	@Merge(propertyProtection=PropertyProtection.NEVER_MERGE)
	private boolean global = false;

	@Column(name = "DESCRIPTION", nullable = true, length = 100)
	@Merge(propertyProtection=PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private String description = null;

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "role", cascade =
    {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.REMOVE})
	private List<UserToRole> users;

	public Role()
	{
		super();
	}

	public Role(RoleType roleType, String code) {
		this.roleType = roleType;
		this.code = code;
	}

	public Role(RoleType roleType, String code, String description, boolean global) {
		this.roleType = roleType;
		this.code = code;
		this.description = description;
		this.global = global;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@XmlElement
	public RoleType getRoleType() {
		return roleType;
	}

	public void setRoleType(RoleType roleType) {
		this.roleType = roleType;
	}

	@XmlElement
	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public boolean isGlobal() {
		return global;
	}

	public void setGlobal(boolean global) {
		this.global = global;
	}

	@XmlElement
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Some imported roles have a blank description so this returns the code
	 * in cases where the description is blank.
	 */
	@Transient
	public String getDescriptionNotBlank() {
		String desc = getDescription();
		return StringUtils.isNotBlank(desc) ? desc : getCode();
	}

	@Transient
	public String getDescriptionAndType() {
		return getDescriptionNotBlank() + " (" + getRoleType() + ")";
	}

	public List<UserToRole> getUsers()
	{
		return users;
	}

	@Transient
	public List<UserToRole> getUsersNotNull() {
		if (users == null) {
			users = new ArrayList<UserToRole>();
		}
		return users;
	}

	public void setUsers(List<UserToRole> users) {
		this.users = users;
	}

	// Add an user to a role.
	// Create an association object for the relationship and set its' data.
	// for now we add users just for global roles this way
	public void addUser(User user) {
		UserToRole u2r = new UserToRole();
		u2r.setRole(this);
		u2r.setProduct(null);
		u2r.setUser(user);
		users.add(u2r);
	}

	@Override
	public String toString() {
		// call getters because of the way JPA works
		return "id = " + getId()
			+ ", code = " + getCode()
			+ ", roleType = " + getRoleType()
		    + ", global = " + isGlobal()
		    + ", description = " + getDescription()
		    + ", (relationships not printed out)";
	}

	/**
	 * Base on code and roleType.
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((code == null) ? 0 : code.hashCode());
		result = prime * result + ((roleType == null) ? 0 : roleType.hashCode());
		return result;
	}

	/**
	 * Base on code and roleType.
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		// Important to use "instance of" due to JPA proxies
		//if (getClass() != obj.getClass()) return false;
		if (!(obj instanceof Role)) return false;
		Role other = (Role) obj;
		if (code == null) {
			if (other.code != null) return false;
		}
		else if (!code.equals(other.code)) return false;
		if (roleType != other.roleType) return false;
		return true;
	}
}
