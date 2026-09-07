package com.wiley.permissions.domain.persistence.permissions;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PersistenceUnit;
import javax.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.wiley.permissions.common.bean.Merge;
import com.wiley.permissions.common.bean.Merge.PropertyProtection;
import com.wiley.permissions.domain.DomainObject;
import com.wiley.permissions.domain.MaterializationKey;

// maybe change to
/*
@XmlEnum
public enum MessageDataSource
{
	@XmlEnumValue("US")
	US("US"),

	@XmlEnumValue("CA")
	CA("CA"),

	@XmlEnumValue("AU")
	AU("AU"),

	@XmlEnumValue("UK")
	UK("UK"),

	@XmlEnumValue("DE")
	DE("DE"),

	@XmlEnumValue("SG")
	SG("SG");

	private String code;

	private MessageDataSource(String code) {
		this.code = code;
	}

	public String getCode() {
		return code;
	}
}
 */

@Entity
@PersistenceUnit(unitName="permissions")
@Table(name = "DATA_SOURCE")
@Cache (usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class DataSource
extends DomainObject
{
	private static final long serialVersionUID = 1L;

	public static final DataSource
		US = new DataSource("US", "United States"),
		CA = new DataSource("CA", "Canada"),
		AU = new DataSource("AU", "Austraila"),
		UK = new DataSource("UK", "United Kingdom"),
		DE = new DataSource("DE", "Germany"),
		SG = new DataSource("SG", "Singapore");

	public static DataSource [] ALL = {
		US, CA, AU, UK, DE, SG
	};

	/**
	 * Throws an exception if the given code is not one of the expected values.
	 */
	public static DataSource forCode(String code) {
		for (DataSource ds : ALL) {
			if (ds.getCode().equals(code))  return ds;
		}

		throw new IllegalArgumentException("DataSource code [" + code + "] is not valid.");
	}

	@Id
	@Column(name = "CODE", nullable = false, length = 20)
	@MaterializationKey
	@Merge(propertyProtection=PropertyProtection.NEVER_MERGE)
	private String code = null;

	@Column(name = "DESCRIPTION", nullable = true, length = 100)
	@Merge(propertyProtection=PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private String description = null;

	public DataSource() {
		super();
	}

	private DataSource(String code, String description) {
		this.code = code;
		this.description = description;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public String toString() {
		return code;
	}

	/**
	 * Base on code.
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		// Important to use "instance of" due to JPA proxies
		//if (getClass() != obj.getClass()) return false;
		if (!(obj instanceof DataSource)) return false;
		DataSource other = (DataSource) obj;
		if (code == null) {
			if (other.code != null) return false;
		} else if (!code.equals(other.code)) return false;

		return true;
	}

	/**
	 * Base on code.
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((code == null) ? 0 : code.hashCode());
		return result;
	}
}
