package com.wiley.permissions.domain.persistence.permissions;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PersistenceUnit;
import javax.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.wiley.permissions.domain.DomainObject;
import com.wiley.permissions.domain.MaterializationKey;
import com.wiley.sf.common.lang.ArgUtil;

@Entity
@PersistenceUnit(unitName="permissions")
@Table(name = "USER_LOCATION")
@Cache (usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class UserLocation
extends DomainObject
{
	private static final long serialVersionUID = 1L;

	public static UserLocation
		US = new UserLocation("US", "US"),
		CA = new UserLocation("CA", "Canada"),
		UK = new UserLocation("UK", "UK"),
		DE = new UserLocation("DE", "Germany"),
		AU = new UserLocation("AU", "Australia"),
		SG = new UserLocation("SG", "Singapore"),
		IN = new UserLocation("IN", "India"),
		CN = new UserLocation("CN", "China");

	public static UserLocation [] ALL = { US, CA, UK, DE, AU, SG, IN, CN };

	/**
	 * Throws an exception if the given code is not one of the expected values.
	 */
	public static UserLocation forCode(String code) {
		for (UserLocation ul : ALL) {
			if (ul.getCode().equals(code))  return ul;
		}

		throw new IllegalArgumentException("UserLocation code [" + code + "] is not valid.");
	}

	@Id
	@Column(name = "CODE", nullable = false, length = 20)
	@MaterializationKey
	private String code = null;

	@Column(name = "NAME", nullable = true, length = 100)
	private String name = null;

	public UserLocation() {

	}

	/**
	 * @param code  Must be non-blank
	 * @param name  Must be non-blank
	 */
	private UserLocation(String code, String name) {
		ArgUtil.notBlank(code, "code");
		ArgUtil.notBlank(name, "name");
		this.code = code;
		this.name = name;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Based only on code.
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		// Important to use "instance of" due to JPA proxies
		//if (getClass() != obj.getClass()) return false;
		if (!(obj instanceof UserLocation)) return false;
		UserLocation other = (UserLocation) obj;
		if (code == null) {
			if (other.code != null) return false;
		} else if (!code.equals(other.code)) return false;

		return true;
	}

	/**
	 * Based only on code.
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((code == null) ? 0 : code.hashCode());
		return result;
	}
}
