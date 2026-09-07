package com.wiley.permissions.rules.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Use this class to log messages from inside the rules file.
 * All method return true so that they can be called inside the "when"
 * part of a rule.
 *
 * @author smarkoff
 */
public class RuleLog {

	private static final Log log = LogFactory.getLog(RuleLog.class);

	public static boolean debug(String message) {
		log.debug(message);
		return true;
	}

	public static boolean info(String message) {
		log.info(message);
		return true;
	}

	public static boolean warn(String message) {
		log.warn(message);
		return true;
	}

	public static boolean error(String message) {
		log.error(message);
		return true;
	}
}
