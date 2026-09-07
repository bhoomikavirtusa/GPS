package com.wiley.permissions.domain.persistence.permissions;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PersistenceUnit;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.wiley.permissions.domain.DomainObject;

@Entity
@PersistenceUnit(unitName = "permissions")
@Table(name = "SIZE_ENUM")
@Cache (usage = CacheConcurrencyStrategy.READ_ONLY)
public class Size
extends DomainObject
{
	private static final long serialVersionUID = 1L;

	// These constants should match the database values (code, description) exactly.
	public static final Size
		ONE_EIGHTH = new Size("1/8 Page", "1/8 Page", 1),
    	QUARTER_PAGE = new Size("1/4 Page", "1/4 Page", 2),
    	THIRD_PAGE = new Size("1/3 Page", "1/3 Page", 3),
    	HALF_PAGE = new Size("1/2 Page", "1/2 Page", 4),
    	THREE_QUARTER_PAGE = new Size("3/4 Page", "3/4 Page", 5),
    	FULL_PAGE = new Size("Full Page", "Full Page", 6),
    	ONE_AND_14_PAGE = new Size("1 1/4 Page", "1 1/4 Page", 7),
    	ONE_AND_12_PAGE = new Size("1 1/2 Page", "1 1/2 Page", 8),
    	ONE_AND_34_PAGE = new Size("1 3/4 Page", "1 3/4 Page", 9),
    	DOUBLE_PAGE = new Size("Double Page", "Double Page", 10),
    	SPOT = new Size("Spot", "Spot", 1),  // same compValue as ONE_EIGHTH
    	NA = new Size("N/A", "N/A", 20);  // trumps everything so biggest value

	public static final Size [] ALL = {
		ONE_EIGHTH, QUARTER_PAGE, THIRD_PAGE, HALF_PAGE, THREE_QUARTER_PAGE, FULL_PAGE,
		ONE_AND_14_PAGE, ONE_AND_12_PAGE, ONE_AND_34_PAGE, DOUBLE_PAGE, SPOT, NA
	};

	/**
	 * Returns null if the given code does not match any Size.
	 */
	public static Size getByCode(String code) {
		for (Size size : ALL) {
			if (StringUtils.equalsIgnoreCase(code, size.getCode()))  return size;
		}
		return null;
	}

	@Id
	@Column(name = "CODE", nullable = false, length = 20)
	private String code = null;

	@Column(name = "DESCRIPTION", nullable = true, length = 100)
	private String description = null;

	@Column(name = "SORT_ORDER", nullable = false)
	private int sortOrder;

	@Transient
	private int compValue;  // this is used by ConditionRepository to compare Sizes (see if one Size is smaller than another)


	public Size() {

	}

	public Size(String code, String description, int compValue) {
		this.code = code;
		this.description = description;
		this.compValue = compValue;
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

	public void setSortOrder(int sortOrder) {
		this.sortOrder = sortOrder;
	}

	public int getSortOrder() {
		return sortOrder;
	}

	@Transient
	public int getCompValue() {
		// because don't store compValue in DB, any Size object loaded from DB won't have correct compValue
		// so in that case load from memory array
		return compValue == 0 ? getByCode(getCode()).compValue : compValue;
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
		if (!(obj instanceof Size)) return false;
		final Size other = (Size) obj;
		if (this.code != other.code && (this.code == null || !this.code.equals(other.code)))
		{
			return false;
		}
		return true;
	}

	/**
	 * Base on code.
	 */
	@Override
	public int hashCode() {
		int hash = 7;
		hash = 47 * hash + (this.code != null ? this.code.hashCode() : 0);
		return hash;
	}

	@Override
	public String toString() {
		return "code = " + getCode() + ", description = " + getDescription();
	}
}
