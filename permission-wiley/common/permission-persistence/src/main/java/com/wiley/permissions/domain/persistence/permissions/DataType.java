package com.wiley.permissions.domain.persistence.permissions;


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PersistenceUnit;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.wiley.permissions.domain.DomainObject;
import com.wiley.permissions.domain.MaterializationKey;

@Entity
@PersistenceUnit(unitName="permissions")
@Table(name = "DATA_TYPE")
@Cache (usage = CacheConcurrencyStrategy.READ_ONLY)
public class DataType
extends DomainObject
{
	private static final long serialVersionUID = 1L;

	// The following constants should always match the database exactly.
	public static final DataType
	    MONEY = new DataType("money", "Money"),
	    TEXT = new DataType("text", "Text"),
	    TEXTAREA = new DataType("textarea", "TextArea"),
	    CHECKBOX = new DataType("checkbox", "Checkbox"),
	    RADIO = new DataType("radio", "Radio");

	@Id
	@Column(name = "CODE", nullable = false, length = 20)
	@XmlElement(name="code")
	@XmlID
	@MaterializationKey
	private String code = null;

	@Column(name = "DESCRIPTION", nullable = true, length = 100)
	@XmlElement(name="description")
	private String description = null;

	public DataType() {
		super();
	}

	public DataType(String code, String description) {
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

	@Transient
	public boolean isTextOrTextArea() {
		return DataType.TEXT.equals(this) || DataType.TEXTAREA.equals(this);
	}

	@Transient
	public boolean isCheckboxOrRadio() {
		return DataType.CHECKBOX.equals(this) || DataType.RADIO.equals(this);
	}
	
	@Transient
	public boolean isCheckbox() {
		return DataType.CHECKBOX.equals(this);
	}
	
	@Transient
	public boolean isRadio() {
		return DataType.RADIO.equals(this);
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
		if (!(obj instanceof DataType)) return false;
		DataType other = (DataType) obj;
		if (code == null) {
			if (other.code != null) return false;
		} else if (!code.equals(other.code)) return false;
		return true;
	}
}
