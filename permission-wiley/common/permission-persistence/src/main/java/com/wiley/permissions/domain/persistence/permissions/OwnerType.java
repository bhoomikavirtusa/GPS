package com.wiley.permissions.domain.persistence.permissions;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PersistenceUnit;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.wiley.permissions.common.utils.ValidateException;
import com.wiley.permissions.domain.DomainObject;
import com.wiley.permissions.domain.MaterializationKey;

@Entity
@PersistenceUnit(unitName = "permissions")
@Table(name = "OWNER_TYPE")
@Cache (usage = CacheConcurrencyStrategy.READ_ONLY)
public class OwnerType
extends DomainObject
{
	private static final long serialVersionUID = 1L;

	// These constants should match the database values (code, description) exactly.
	public static final OwnerType
	    WILEY = new OwnerType("Wiley Owned", "Wiley/Author Provided"),
	    WORK_FOR_HIRE = new OwnerType("Work For Hire", "Work For Hire"),
	    THIRD_PARTY = new OwnerType("3rd Party", "3rd Party"),
	    PUBLIC_DOMAIN = new OwnerType("Public Domain", "Public Domain"),
	    FAIR_USE = new OwnerType("Fair Use", "Fair Use"),
	    PHOTO_REQUEST = new OwnerType("Photo Request", "Photo Request"),
	    ILLUSTRATION_REQUEST = new OwnerType("Illustration Request", "Illustration Request"),
		WILEY_CREATED = new OwnerType("Wiley Created", "Wiley Created"),
	    AUTHOR_OWNED = new OwnerType("Author Owned", "Author Owned");

	public static final OwnerType [] ALL_OWNER_TYPES = {
		WILEY, WORK_FOR_HIRE, THIRD_PARTY, PUBLIC_DOMAIN, FAIR_USE, PHOTO_REQUEST, ILLUSTRATION_REQUEST,
		WILEY_CREATED, AUTHOR_OWNED
	};

	/**
	 * Special method only used for spreadsheet import.
	 * Recognizes the names in the spreadsheet for OwnerType,
	 * NOT the names we use in the database.
	 */
	public static OwnerType getOwnerType(String name) {
		if (StringUtils.isBlank(name))
			return null;

		name = name.trim();

		if (name.equalsIgnoreCase("public domain")) {
			return OwnerType.PUBLIC_DOMAIN;
		} else if (name.equalsIgnoreCase("author")) {
			return OwnerType.WILEY;
		} else if (name.equalsIgnoreCase("wiley")) {
			return OwnerType.WILEY;
		} else {
			return null;
		}
	}

	@Id
	@Column(name = "CODE", nullable = false, length = 20)
	@MaterializationKey
	private String code = null;

	@Column(name = "DESCRIPTION", nullable = true, length = 100)
	private String description = null;

	@Column(name = "SORT_ORDER", nullable = false)
	private int sortOrder;


	public OwnerType() {
		super();
	}

	public OwnerType(String code, String description) {
		this.code = code;
		this.description = description;
	}

	@XmlElement
	@XmlID
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

	public void setSortOrder(int sortOrder) {
		this.sortOrder = sortOrder;
	}

	public int getSortOrder() {
		return sortOrder;
	}

	/**
	 * We automatically correct the code if only the case is wrong.
	 *
	 * @throws ValidateException
	 */
	public void validate() throws ValidateException {
		for (OwnerType ot: ALL_OWNER_TYPES) {
			if (ot.getCode().equals(code))  return;
			if (ot.getCode().equalsIgnoreCase(code)) {
				code = ot.getCode();
				return;
			}
		}

		throw new ValidateException("\"" + code + "\" is not a valid OwnerType code");
	}

	@Override
	public String toString() {
		return "code = " + code
		    + ", description = " + description
		    + ", sortOrder = " + sortOrder;
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
		if (!(obj instanceof OwnerType)) return false;
		OwnerType other = (OwnerType) obj;
		if (code == null) {
			if (other.code != null) return false;
		} else if (!code.equals(other.code)) return false;

		return true;
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
}
