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
@Table(name = "IMPORT_SOURCE")
@Cache (usage = CacheConcurrencyStrategy.READ_ONLY)
public class ImportSource
extends DomainObject
{
	private static final long serialVersionUID = 1L;

	// The following constants should always match the database exactly.
	public static final ImportSource
	    FROM_FILEMAKER = new ImportSource(1, "GE Filemaker"),
	    FROM_CS_SPREADSHEET = new ImportSource(2, "CS SpreadSheet"),
	    FROM_AUSTRALIA = new ImportSource(3, "Australia"),
    	FROM_RIGHTS_LINK = new ImportSource(4, "Rights Link"),
    	COPY_FROM_PREVIOUS_EDITION = new ImportSource(6, "Copy Asset from Previous Edition");


	public static final ImportSource [] ALL = {
		FROM_FILEMAKER, FROM_CS_SPREADSHEET, FROM_AUSTRALIA, FROM_RIGHTS_LINK,COPY_FROM_PREVIOUS_EDITION
	};

	/**
	 * Throws an exception if the code is not valid.
	 */
	public static ImportSource getImportSourceForCode(Integer code) {
		for (ImportSource is : ALL) {
			if (is.getCode().equals(code)) return is;
		}

		throw new IllegalArgumentException("Invalid ImportSource code: " + code);
	}

	@Id
	@Column(name = "CODE", nullable = false, length = 20)
	@XmlElement(name="code")
	@XmlID
	@MaterializationKey
	private Integer code = null;

	@Column(name = "DESCRIPTION", nullable = true, length = 100)
	@XmlElement(name="description")
	private String description = null;

	public ImportSource() {
		super();
	}

	public ImportSource(Integer code, String description) {
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
	/**
	 * Checks for either another ImportSource object or an Integer.
	 */
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;

		// special logic for Integer comparison
		if (obj instanceof Integer) {
			Integer i = (Integer) obj;
			return i.equals(code);
		}

		// Important to use "instance of" due to JPA proxies
		//if (getClass() != obj.getClass()) return false;
		if (!(obj instanceof ImportSource)) return false;
		ImportSource other = (ImportSource) obj;
		if (code == null) {
			if (other.code != null) return false;
		} else if (!code.equals(other.code)) return false;
		return true;
	}
}
