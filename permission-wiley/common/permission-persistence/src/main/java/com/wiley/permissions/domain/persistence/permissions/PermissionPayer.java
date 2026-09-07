package com.wiley.permissions.domain.persistence.permissions;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PersistenceUnit;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;

import com.wiley.permissions.domain.DomainObject;
import com.wiley.permissions.domain.MaterializationKey;

@Entity
@PersistenceUnit(unitName = "permissions")
@Table(name = "PERMISSION_PAYER")
public class PermissionPayer
extends DomainObject
{
	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "CODE", nullable = false, length = 20)
	@XmlElement(name = "code")
	@XmlID
	@MaterializationKey
	private String code = null;

	@Column(name = "DESCRIPTION", nullable = true, length = 100)
	@XmlElement(name = "description")
	private String description = null;

	public PermissionPayer() {
		super();
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

	// base equals() and hashCode just on code

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
		if (!(obj instanceof PermissionPayer)) return false;
		PermissionPayer other = (PermissionPayer) obj;
		if (code == null) {
			if (other.code != null) return false;
		}
		else if (!code.equals(other.code)) return false;
		return true;
	}
}
