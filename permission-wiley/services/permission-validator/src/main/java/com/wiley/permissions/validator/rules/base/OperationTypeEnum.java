package com.wiley.permissions.validator.rules.base;

/**
 * smarkoff 11/2010: I'm pretty sure this class is not being used.
 *
 * @author Sreenath
 * @version 1.1 Created on May 30, 2008 at 3:45:46 PM
 */
public enum OperationTypeEnum {

	PERMISSION_STATUS_VALIDATION("permission-status"),
	ASSET_USE("AssetUse");

	private final String name;

	private OperationTypeEnum(String name) {
		this.name = name;
	}

	/**
	 * Overrides java.lang.Enum.toString.
	 */
	@Override
	public String toString() {
		return this.name;
	}
}
