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

@Entity
@PersistenceUnit(unitName="permissions")
@Table(name = "WILEY_ENTITY")
@Cache (usage = CacheConcurrencyStrategy.READ_ONLY)
public class WileyEntity
extends DomainObject
{
	private static final long serialVersionUID = 1L;

	public static final WileyEntity
    WILEY_US = new WileyEntity("WILEY_US", "John Wiley & Sons, Inc.");
	
	@Id
	@Column(name = "CODE", nullable = false, length = 20)
	@XmlElement(name="code")
	@XmlID
	private String code = null;

	@Column(name = "DESCRIPTION", nullable = true, length = 100)
	@XmlElement(name="description")
	private String description = null;

	public WileyEntity() {
		super();
	}

	public WileyEntity(String code, String description) {
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
		// use getters due to JPA
		return "code = " + getCode()
			+ ", description = " + getDescription();
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

	/**
	 * Base on code.
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		// Important to use "instance of" due to JPA proxies
		//if (getClass() != obj.getClass()) return false;
		if (!(obj instanceof WileyEntity)) return false;
		final WileyEntity other = (WileyEntity) obj;
		if (code == null) {
			if (other.code != null) return false;
		} else if (!code.equals(other.code)) return false;
		return true;
	}
}
