package com.wiley.permissions.domain.persistence.permissions;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PersistenceUnit;
import javax.persistence.Table;

import com.wiley.permissions.domain.DomainObject;

@Entity
@PersistenceUnit(unitName="permissions")
@Table(name = "isbn_data")
public class ISBNData
extends DomainObject
{
	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "ISBN13", nullable = false, unique = true, length = 13)
	private String isbn13 = null;

	@Column(name = "GROSS_UNITS", nullable = true)
	private Integer grossUnits = 0;

	public ISBNData() {
		super();
	}

	public ISBNData(String isbn13, Integer grossUnits) {
		this.isbn13 = isbn13;
		this.grossUnits = grossUnits;
	}

	public String getIsbn13() {
		return isbn13;
	}

	public void setIsbn13(String isbn13) {
		this.isbn13 = isbn13;
	}

	public Integer getGrossUnits() {
		return grossUnits;
	}

	public void setGrossUnits(Integer grossUnits) {
		this.grossUnits = grossUnits;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((isbn13 == null) ? 0 : isbn13.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		// Important to use "instance of" due to JPA proxies
		//if (getClass() != obj.getClass()) return false;
		if (!(obj instanceof ISBNData)) return false;
		ISBNData other = (ISBNData) obj;
		if (isbn13 == null) {
			if (other.isbn13 != null) return false;
		}
		else if (!isbn13.equals(other.isbn13)) return false;
		return true;
	}
}
