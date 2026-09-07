package com.wiley.permissions.domain.persistence.permissions;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PersistenceUnit;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;

import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.wiley.permissions.domain.DomainObject;
import com.wiley.permissions.domain.MaterializationKey;

@Entity
@PersistenceUnit(unitName = "permissions")
@Table(name = "MODEL_RELEASE")
@Cache (usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class ModelRelease
extends DomainObject
{
	private static final long serialVersionUID = 1L;

	public static final ModelRelease
    ACQUIRED = new ModelRelease("acquired", "Acquired"),
    NOT_NEEDED = new ModelRelease("not_needed", "Not needed"),
    NEEDED = new ModelRelease("needed", "Needed");

	public static final ModelRelease [] ALL_MODEL_RELEASE = {
		ACQUIRED, NOT_NEEDED, NEEDED
	};

	@Id
	@Column(name = "CODE", nullable = false, length = 20)
	@MaterializationKey
	private String code = null;

	@Column(name = "NAME", nullable = true, length = 100)
	private String name = null;


	public ModelRelease() {

	}

	public ModelRelease(String code, String name) {
		this.code = code;
		this.name = name;
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

	/**
	 * Returns null if the given description does not match any Usage.
	 */
	public static ModelRelease getByDescription(String description) {
		for (ModelRelease modelRelease : ALL_MODEL_RELEASE) {
			if (StringUtils.equalsIgnoreCase(description, modelRelease.getName()))  return modelRelease;
		}
		return null;
	}

	// base equals and hashCode on the @XmlID which is code

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((code == null) ? 0 : code.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)  return true;
		if (obj == null)  return false;
		// Important to use "instance of" due to JPA proxies
		if (!(obj instanceof ModelRelease)) return false;
		final ModelRelease other = (ModelRelease) obj;
		if (code == null) {
			if (other.code != null) return false;
		}
		else if (!code.equals(other.code)) {
			return false;
		}
		return true;
	}
}
