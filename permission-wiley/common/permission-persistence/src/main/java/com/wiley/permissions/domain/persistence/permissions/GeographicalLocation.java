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

import com.wiley.permissions.common.bean.Merge;
import com.wiley.permissions.common.bean.Merge.PropertyProtection;
import com.wiley.permissions.domain.DomainObject;
import com.wiley.permissions.domain.MaterializationKey;

@Entity
@PersistenceUnit(unitName="permissions")
@Table(name = "GEO_LOCATION")
@Cache (usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class GeographicalLocation
extends DomainObject
{
	private static final long serialVersionUID = 1L;
	
	// These constants should match the database values (code, description) exactly.
	public static final GeographicalLocation
	    NY = new GeographicalLocation("NY", "U.S. Product"),
	    AUS = new GeographicalLocation("AUS", "Australia Product");

	@Id
	@Column(name = "CODE", nullable = false, length = 20)
	@MaterializationKey
	@Merge(propertyProtection=PropertyProtection.NEVER_MERGE)
	private String code = null;

	@Column(name = "NAME", nullable = true, length = 100)
	@Merge(propertyProtection=PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private String name = null;

	public GeographicalLocation() {
		super();
	}
	
	public GeographicalLocation(String code, String name) {
		this.code = code;
		this.name = name;
	}

	@XmlElement(name="code")
	@XmlID
	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	@XmlElement(name="name")
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	// just look at code for equals / hashCode

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		// Important to use "instance of" due to JPA proxies
		//if (getClass() != obj.getClass()) return false;
		if (!(obj instanceof GeographicalLocation)) return false;
		GeographicalLocation other = (GeographicalLocation) obj;
		if (code == null) {
			if (other.code != null) return false;
		} else if (!code.equals(other.code)) return false;

		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((code == null) ? 0 : code.hashCode());
		return result;
	}
}
