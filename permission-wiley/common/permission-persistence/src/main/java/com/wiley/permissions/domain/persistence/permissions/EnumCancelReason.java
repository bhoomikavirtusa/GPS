package com.wiley.permissions.domain.persistence.permissions;

import javax.persistence.Entity;
import javax.persistence.PersistenceUnit;
import javax.persistence.Table;

/**
 *
 * @author lnagy
 */
@Entity
@PersistenceUnit(unitName = "permissions")
@Table(name="ENUM_CANCEL_REASON")
public class EnumCancelReason extends EnumData
{
	public EnumCancelReason() { }

	public EnumCancelReason(String code, String description) {
		setCode(code);
		setDescription(description);
	}
}
