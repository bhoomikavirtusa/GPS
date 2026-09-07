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

import com.wiley.permissions.common.utils.ValidateException;
import com.wiley.permissions.domain.DomainObject;
import com.wiley.permissions.domain.MaterializationKey;

@Entity
@PersistenceUnit(unitName = "permissions")
@Table(name = "PAGE_POS_ENUM")
@Cache (usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class PagePosition
extends DomainObject
{
	private static final long serialVersionUID = 1L;

	// The following constants should match the database exactly.
	public static final PagePosition
		NA = new PagePosition("N/A", "N/A"),
		TOP = new PagePosition("top", "Top"),
		BOTTOM = new PagePosition("bottom", "Bottom"),
		TOP_RIGHT = new PagePosition("top right", "Top Right"),
		TOP_LEFT = new PagePosition("top left", "Top Left"),
		BOTTOM_RIGHT = new PagePosition("bottom right", "Bottom Right"),
		BOTTOM_LEFT = new PagePosition("bottom left", "Bottom Left"),
		LEFT = new PagePosition("left", "Left"),
		RIGHT = new PagePosition("right", "Right"),
		CENTER = new PagePosition("center", "Center"),
		CENTER_LEFT = new PagePosition("center left", "Center Left"),
		CENTER_RIGHT = new PagePosition("center right", "Center Right"),
		TOP_CENTER = new PagePosition("top center", "Top Center"),
		BOTTOM_CENTER = new PagePosition("bottom center", "Bottom Center");

	public static final PagePosition [] ALL_PAGE_POSITION_TYPES = {
		TOP, BOTTOM, TOP_RIGHT, TOP_LEFT, BOTTOM_RIGHT, BOTTOM_LEFT, LEFT, RIGHT, CENTER,
		CENTER_LEFT, CENTER_RIGHT, TOP_CENTER, BOTTOM_CENTER, NA
	};

	@Id
	@Column(name = "CODE", nullable = false, length = 20)
	@MaterializationKey
	private String code = null;

	@Column(name = "DESCRIPTION", nullable = true, length = 100)
	private String description = null;

	@Column(name = "SORT_ORDER", nullable = false)
	private int sortOrder;


	public PagePosition() {

	}

	public PagePosition(String code, String description) {
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
	 * We automatically correct the code if it contains "(,)"
	 * or the case is wrong.
	 */
	public void validate() throws ValidateException {

		for (PagePosition u: ALL_PAGE_POSITION_TYPES) {

			if (u.getCode().equals(getCode()))  return;

			if (u.getCode().equalsIgnoreCase(getCode())) {
				return ;
			}
		}

		throw new ValidateException("\"" + code + "\" is not a valid PagePosition code");
	}

	// smarkoff: Do not call this method getInstance() - causes issue
	// with JPARepository.replaceEntities() since there is no corresponding set method.
	public PagePosition instanceForCode() {
		for (PagePosition u: ALL_PAGE_POSITION_TYPES) {
			if (u.getCode().equals(getCode()))  return u;

			if (u.getCode().equalsIgnoreCase(getCode())) {
				return u;
			}
		}
		return null;
	}
	
	// base equals() and hashCode() on code

	@Override
	public boolean equals(Object obj) {
		if (obj == null) return false;
		// Important to use "instance of" due to JPA proxies
		//if (getClass() != obj.getClass()) return false;
		if (!(obj instanceof PagePosition)) return false;
		final PagePosition other = (PagePosition) obj;
		if (this.code != other.code && (this.code == null || !this.code.equals(other.code)))
		{
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 47 * hash + (this.code != null ? this.code.hashCode() : 0);
		return hash;
	}
}
