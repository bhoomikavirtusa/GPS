package com.wiley.permissions.domain.persistence.permissions;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PersistenceUnit;
import javax.persistence.Table;

import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@PersistenceUnit(unitName = "permissions")
@Table(name = "ENUM_USAGE_CONDITION")
@Cache (usage = CacheConcurrencyStrategy.READ_ONLY)
public class EnumUsageCondition extends EnumData
{
	// These constants should match the database values (code, description) exactly.
	public static final EnumUsageCondition
		FRONT_COVER = new EnumUsageCondition("Front Cover", "Front Cover"),
		BACK_COVER = new EnumUsageCondition("Back Cover", "Back Cover"),
		SPINE = new EnumUsageCondition("Spine", "Spine"),
		WRAP_COVER = new EnumUsageCondition("Wrap Cover", "Wrap Cover"),
		FLAPS = new EnumUsageCondition("Flaps", "Flaps"),
		INTERIOR_SINGLE_USE = new EnumUsageCondition("Interior(single use)", "Interior (single use)"),
		INTERIOR_MULTI_USE = new EnumUsageCondition("Interior(multi use)", "Interior (multi use)"),
		OPENER_EPIGRAPH = new EnumUsageCondition("Opener/Epigraph", "Opener/Epigraph"),
		MARKETING_PROMO = new EnumUsageCondition("Marketing/Promo", "Marketing/Promo"),
		CDROM = new EnumUsageCondition("CDROM", "CDROM");

	public static final EnumUsageCondition [] ALL = {
		FRONT_COVER, BACK_COVER, SPINE, WRAP_COVER, FLAPS,
		INTERIOR_SINGLE_USE, INTERIOR_MULTI_USE, OPENER_EPIGRAPH, MARKETING_PROMO, CDROM
	};

	/**
	 * Returns null if the given code does not match any EnumUsageCondition.
	 */
	public static EnumUsageCondition getByCode(String code) {
		for (EnumUsageCondition usage : ALL) {
			if (StringUtils.equalsIgnoreCase(code, usage.getCode()))  return usage;
		}
		return null;
	}

	@Column(name = "SORT_ORDER", nullable = false)
	private int sortOrder;


	public EnumUsageCondition() {

	}

	public EnumUsageCondition(String code, String description) {
		super(code, description);
	}

	public void setSortOrder(int sortOrder) {
		this.sortOrder = sortOrder;
	}

	public int getSortOrder() {
		return sortOrder;
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
		if (!(obj instanceof EnumUsageCondition)) return false;
		final EnumUsageCondition other = (EnumUsageCondition) obj;
		if (getCode() != other.getCode() && (getCode() == null || !getCode().equals(other.getCode())))
		{
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "code = " + getCode() + ", description = " + getDescription();
	}
}
