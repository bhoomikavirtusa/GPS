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
@Table(name="ADDRESS_TYPE")
public class AddressType extends EnumData {
	
	public static final AddressType MAIN = new AddressType("A", "Main Address");
	public static final AddressType BILLING = new AddressType("B", "Billing Address");
	// code M used to be for Mailing Address
	public static final AddressType OTHER = new AddressType("M", "Other Address");

	public AddressType() { }

	public AddressType(String code, String description) {
		setCode(code);
		setDescription(description);
	}
}
