package com.wiley.permissions.validator.rules.exception;

import com.wiley.permissions.common.utils.PermBaseException;

/**
 * The Class ValidationException.
 * 
 * @author Sreenath Putta (sputta@wiley.com)
 * @version 1.1 Created on May 30, 2008 at 3:28:51 PM
 */
public class ValidationException extends PermBaseException {

	private static final long serialVersionUID = 1L;

	/**
	 * Instantiates a new validation exception.
	 * 
	 * @param cause the cause
	 */
	public ValidationException(Throwable cause) {
		super(cause);
	}

	/**
	 * The Constructor.
	 * 
	 * @param message the message
	 * @param cause the cause
	 */
	public ValidationException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Instantiates a new validation exception.
	 * 
	 * @param message the message
	 */
	public ValidationException(String message) {
		super(message);
	}
}
