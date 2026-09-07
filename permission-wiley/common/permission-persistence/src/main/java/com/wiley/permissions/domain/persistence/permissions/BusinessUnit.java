package com.wiley.permissions.domain.persistence.permissions;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PersistenceUnit;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.wiley.permissions.common.bean.Merge;
import com.wiley.permissions.common.bean.Merge.PropertyProtection;
import com.wiley.permissions.domain.DomainObject;
import com.wiley.permissions.domain.MaterializationKey;
import com.wiley.sf.common.lang.ArgUtil;

@Entity
@PersistenceUnit(unitName="permissions")
@Table(name = "BUSINESS_UNIT")
@Cache (usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class BusinessUnit
extends DomainObject
{
	private static final long serialVersionUID = 1L;

	public static final BusinessUnit
		U0 = new BusinessUnit("0", "Unassigned"),
		GE = new BusinessUnit("1", "Global Education", "GE"),  // note we still get "Higher Education" from PE (the old name)
		PD = new BusinessUnit("2", "Professional Development", "PD"),
		GR = new BusinessUnit("3", "Global Research", "GR"),
		M = new BusinessUnit("4", "Miscellaneous"),
		U5 = new BusinessUnit("5", "Unassigned"),
		U6 = new BusinessUnit("6", "Unassigned"),
		U7 = new BusinessUnit("7", "Unassigned"),
		U8 = new BusinessUnit("8", "Unassigned"),
		U9 = new BusinessUnit("9", "Unassigned");  // for Australia 9 = School

	public static BusinessUnit [] ALL = {
		U0, GE, PD, GR, M, U5, U6, U7, U8, U9
	};

	public static BusinessUnit [] ALL_FOR_MA_DEAL = { GE, PD, GR };

	/**
	 * Throws an exception if the given code is not one of the expected values.
	 */
	public static BusinessUnit forCode(String code) {
		for (BusinessUnit bu : ALL) {
			if (bu.getCode().equals(code))  return bu;
		}

		throw new IllegalArgumentException("BusinessUnit code [" + code + "] is not valid.");
	}


	@Id
	@Column(name = "CODE", nullable = false, length = 20)
	@MaterializationKey(alwaysTrim=true)
	@Merge(propertyProtection=PropertyProtection.NEVER_MERGE)
	private String code;

	@Column(name = "NAME", nullable = true, length = 100)
	@Merge(propertyProtection=PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private String name;

	// could add this field to database, but don't need to, have only in our constants
	@Transient
	private String shortName;

	@Column(name = "EMAIL", nullable = true, length = 100)
	@Merge(propertyProtection=PropertyProtection.NEVER_MERGE)
	private String email;

	@Column(name = "WARN_ROLES", nullable = true, length = 200)
	@Merge(propertyProtection=PropertyProtection.NEVER_MERGE)
	private String warnRoles;

	@Column(name = "WARN_LAST_UPDATE_USER", nullable = false)
	@Merge(propertyProtection=PropertyProtection.NEVER_MERGE)
	private boolean warnLastUpdateUser;

	@Column(name = "EMAIL_COVERS", nullable = true, length = 100)
	@Merge(propertyProtection=PropertyProtection.NEVER_MERGE)
	private String emailCovers;

	@Column(name = "WARN_ROLES_COVERS", nullable = true, length = 200)
	@Merge(propertyProtection=PropertyProtection.NEVER_MERGE)
	private String warnRolesCovers;

	@Column(name = "WARN_LAST_UPDATE_USER_COVERS", nullable = false)
	@Merge(propertyProtection=PropertyProtection.NEVER_MERGE)
	private boolean warnLastUpdateUserCovers;


	public BusinessUnit() {
		super();
	}

	/**
	 * @param code  Must be non-blank
	 * @param name  Must be non-blank
	 */
	private BusinessUnit(String code, String name) {
		ArgUtil.notBlank(code, "code");
		ArgUtil.notBlank(name, "name");
		this.code = code;
		this.name = name;
	}

	/**
	 * @param code  Must be non-blank
	 * @param name  Must be non-blank
	 * @param shortName  May be null
	 */
	private BusinessUnit(String code, String name, String shortName) {
		ArgUtil.notBlank(code, "code");
		ArgUtil.notBlank(name, "name");
		this.code = code;
		this.name = name;
		this.shortName = shortName;
	}

	@XmlElement
	@XmlID
	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	@XmlElement
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Transient
	public String getShortName() {
		return shortName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getWarnRoles() {
		return warnRoles;
	}

	public void setWarnRoles(String warnRoles) {
		this.warnRoles = warnRoles;
	}

	public boolean getWarnLastUpdateUser() {
		return warnLastUpdateUser;
	}

	public void setWarnLastUpdateUser(boolean warnLastUpdateUser) {
		this.warnLastUpdateUser = warnLastUpdateUser;
	}

	public String getEmailCovers() {
		return emailCovers;
	}

	public void setEmailCovers(String emailCovers) {
		this.emailCovers = emailCovers;
	}

	public String getWarnRolesCovers() {
		return warnRolesCovers;
	}

	public void setWarnRolesCovers(String warnRolesCovers) {
		this.warnRolesCovers = warnRolesCovers;
	}

	public boolean getWarnLastUpdateUserCovers() {
		return warnLastUpdateUserCovers;
	}

	public void setWarnLastUpdateUserCovers(boolean warnLastUpdateUserCovers) {
		this.warnLastUpdateUserCovers = warnLastUpdateUserCovers;
	}

	@Override
	public String toString() {
		return "code = " + getCode() + ", name = " + getName();
	}

	/**
	 * Base on code (XmlID).
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((code == null) ? 0 : code.hashCode());
		return result;
	}

	/**
	 * Base on code (XmlID).
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		// Important to use "instance of" due to JPA proxies
		//if (getClass() != obj.getClass()) return false;
		if (!(obj instanceof BusinessUnit)) return false;
		BusinessUnit other = (BusinessUnit) obj;
		if (code == null) {
			if (other.code != null) return false;
		}
		else if (!code.equals(other.code)) return false;
		return true;
	}
}
