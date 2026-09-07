package com.wiley.permissions.domain.util;


import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class now just wraps around the built in Java class java.util.UUID.
 * Just includes "PERM." at the beginning of all identifiers.
 *
 * @author ttidwell
 */
public abstract class UniqueIdentifierGenerator
{
	@SuppressWarnings("unused")
	private final static Log log = LogFactory.getLog(UniqueIdentifierGenerator.class);

	/**
	 * Uses the prefix of "perm."
	 */
	public static String getNextIdentifier() {
		return getNextIdentifier("perm.");
	}
	
	/**
	 * By convention, prefix should be "perm.tableName." and end with a period.
	 * Example "perm.asset_base."
	 */
	public static String getNextIdentifier(String prefix) {
		final String uuid = UUID.randomUUID().toString();
		//log.debug("getNextIdentifier(): uuid = " + uuid);
		return prefix + uuid;
	}
}
