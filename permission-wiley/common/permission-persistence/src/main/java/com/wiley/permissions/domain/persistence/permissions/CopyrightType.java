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
@Table(name = "COPYRIGHT_TYPE")
@Cache (usage = CacheConcurrencyStrategy.READ_ONLY)
public class CopyrightType
extends DomainObject
{
	private static final long serialVersionUID = 1L;

	// These constants should match the database values (code, description) exactly.
	public static final CopyrightType
	    NOT_WILEY_OWNED = new CopyrightType("0", "NOT Wiley owned"),
	    WILEY_OWNED_WORK_FOR_HIRE = new CopyrightType("1", "Wiley owned - Work for hire"),
	    WILEY_OWNED_CTR = new CopyrightType("2", "Wiley owned - Copyright Transfer Agreement");
	    

	public static final CopyrightType [] ALL_COPYRIGHT_TYPES = {
		NOT_WILEY_OWNED, WILEY_OWNED_WORK_FOR_HIRE, WILEY_OWNED_CTR
	};

	/**
	 * Special method only used for spreadsheet import.
	 * Recognizes the names in the spreadsheet for CopyrightType,
	 * NOT the names we use in the database.
	 */
	public static CopyrightType getCopyrightType(String name)
	{
		if (StringUtils.isBlank(name)) return null;

		name = name.trim();

		if (name.equalsIgnoreCase("NOT Wiley owned")) {
			return CopyrightType.NOT_WILEY_OWNED;
		} else if (name.equalsIgnoreCase("Wiley owned - Work for hire")) {
			return CopyrightType.WILEY_OWNED_WORK_FOR_HIRE;
		} else if (name.equalsIgnoreCase("Wiley owned - Copyright Transfer Agreement")) {
			return CopyrightType.WILEY_OWNED_CTR;
		} else {
			return null;
		}
	}

	@Id
	@Column(name = "CODE", nullable = false, length = 20)
	@MaterializationKey
	private String code;

	@Column(name = "DESCRIPTION", nullable = true, length = 100)
	private String description = null;


	public CopyrightType() {
	}

	public CopyrightType(String code, String description) {
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


	/**
	 * We automatically correct the code if only the case is wrong.
	 *
	 * @throws ValidateException
	 */
	public void validate() throws ValidateException {
		for (CopyrightType ot: ALL_COPYRIGHT_TYPES) {
			if (ot.getCode().equals(code))  return;
			if (ot.getCode().equals(code)) {
				code = ot.getCode();
				return;
			}
		}

		throw new ValidateException("\"" + code + "\" is not a valid CopyrightType code");
	}

	@Override
	public String toString() {
		return "code = " + code
		    + ", description = " + description;
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
		if (!(obj instanceof CopyrightType)) return false;
		CopyrightType other = (CopyrightType) obj;
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
