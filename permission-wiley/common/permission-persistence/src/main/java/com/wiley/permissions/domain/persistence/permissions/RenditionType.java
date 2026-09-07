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
@Table(name = "RENDITION_TYPE")
@Cache (usage = CacheConcurrencyStrategy.READ_ONLY)
public class RenditionType
extends DomainObject
{
	private static final long serialVersionUID = 1L;

	// The following constants should match the database exactly.
	// Sometimes they are used instead of loading the information from the db.
	// - Except the db does not have maxWidth/Height fields.
	public static final RenditionType
	    ORIGINAL = new RenditionType("original", "Original", 0, 0),
	    SMALL_THUMBNAIL = new RenditionType("small_th", "Small Thumbnail", 40, 40),
	    MEDIUM_THUMBNAIL = new RenditionType("medium_th", "Medium Thumbnail", 90, 90),
	    LARGE_THUMBNAIL = new RenditionType("large_th", "Large Thumbnail", 200, 200);
	
	/**
	 * Returns the corresponding RenditionType, or else throws an IllegalArgumentException
	 * for an illegal size string.
	 * 
	 * @param size  Expected to be "large", "medium", or "small" (case INsensitive)
	 */
	public static RenditionType getRenditionTypeForString(String size) {
		if ("large".equalsIgnoreCase(size)) {
			return RenditionType.LARGE_THUMBNAIL;
		}
		else if ("medium".equalsIgnoreCase(size)) {
			return RenditionType.MEDIUM_THUMBNAIL;
		}
		else if ("small".equalsIgnoreCase(size)) {
			return RenditionType.SMALL_THUMBNAIL;
		}
		else {
			throw new IllegalArgumentException("size of [" + size + "] is not valid.");
		}
	}
	
	@Id
	@Column(name = "CODE", unique = true, nullable = false, length = 50)
	@MaterializationKey
	private String code;

	@Column(name = "DESCRIPTION", nullable = true, length = 100)
	private String description;

	@Column(name = "SORT_ORDER", nullable = false)
	private int sortOrder;
	
	@Transient
	private int maxWidth;
	
	@Transient
	private int maxHeight;


	public RenditionType()
	{
		super();
	}

	public RenditionType(String code, String description, int maxWidth, int maxHeight) {
		this.code = code;
		this.description = description;
		this.maxWidth = maxWidth;
		this.maxHeight = maxHeight;
	}

	@XmlElement
	@XmlID
	public String getCode()
	{
		return code;
	}

	public void setCode(String code)
	{
		this.code = code;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	public void setSortOrder(int sortOrder) {
		this.sortOrder = sortOrder;
	}

	public int getSortOrder() {
		return sortOrder;
	}
	
	@Transient
	public int getMaxWidth() {
		return maxWidth;
	}
	
	@Transient
	public int getMaxHeight() {
		return maxHeight;
	}
	
	@Override
	public String toString() {
		// use getters due to the way JPA works
	    return "code = " + getCode()
	        + ", description = " + getDescription()
	        + ", sortOrder = " + getSortOrder();
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
		if (!(obj instanceof RenditionType)) return false;
		RenditionType other = (RenditionType) obj;
		if (code == null) {
			if (other.code != null) return false;
		} else if (!code.equals(other.code)) return false;

		return true;
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
}
