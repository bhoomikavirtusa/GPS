package com.wiley.permissions.domain.persistence.permissions;

import javax.persistence.Entity;
import javax.persistence.PersistenceUnit;
import javax.persistence.Table;

/**
 *
 * @author ttidwell
 */
@Entity
@PersistenceUnit(unitName = "permissions")
@Table(name="COUNTRY")
public class Country
extends EnumData
{
	public static final String USA = "US";

	public Country() {
		super();
	}

	public Country(String code, String description) {
		setCode(code);
		setDescription(description);
	}
}
