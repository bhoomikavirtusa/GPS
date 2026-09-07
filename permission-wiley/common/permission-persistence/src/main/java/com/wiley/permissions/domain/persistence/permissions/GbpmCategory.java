package com.wiley.permissions.domain.persistence.permissions;


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PersistenceUnit;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.wiley.permissions.domain.DomainObject;
import com.wiley.permissions.domain.MaterializationKey;

@Entity
@PersistenceUnit(unitName="permissions")
@Table(name = "GBPM_CATEGORY")
@Cache (usage = CacheConcurrencyStrategy.READ_ONLY)
public class GbpmCategory
extends DomainObject
{
	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "CODE", nullable = false, length = 20)
	@XmlElement(name="code")
	@XmlID
	@MaterializationKey
	private Integer code = null;

	@Column(name = "DESCRIPTION", nullable = true, length = 100)
	@XmlElement(name="description")
	private String description = null;

	public GbpmCategory() {
		super();
	}

	public GbpmCategory(Integer code, String description) {
		this.code = code;
		this.description = description;
	}

	public Integer getCode() {
		return code;
	}

	public void setCode(Integer code) {
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
		// use getters due to JPA
		return "code = " + getCode()
			+ ", description = " + getDescription();
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
		if (this == obj) return true;
		if (obj == null) return false;
		// Important to use "instance of" due to JPA proxies
		//if (getClass() != obj.getClass()) return false;
		if (!(obj instanceof GbpmCategory)) return false;
		GbpmCategory other = (GbpmCategory) obj;
		if (code == null) {
			if (other.code != null) return false;
		} else if (!code.equals(other.code)) return false;
		return true;
	}
}
