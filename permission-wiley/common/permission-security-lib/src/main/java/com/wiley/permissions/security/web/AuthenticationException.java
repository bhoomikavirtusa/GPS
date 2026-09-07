package com.wiley.permissions.security.web;

/**
 *
 * @author ttidwell
 */
public class AuthenticationException
//TODO: extend PermBaseException - causes dependency issue on clean build - should be able to fix
//extends PermBaseException
extends Exception
{
	private static final long serialVersionUID = 1L;

	public enum Type {
		USER_NOT_FOUND,
		INVALID_PASSWORD,
		INCOMPLETE_USER,
		DISABLED,
		SYSTEM_ERROR
	}

	private Type type = Type.SYSTEM_ERROR;


	public AuthenticationException(String message) {
		super(message);
	}

	public AuthenticationException(Throwable cause) {
		super(cause);
	}

	public AuthenticationException(String message, Throwable cause) {
		super(message, cause);
	}

	public AuthenticationException(Type type, String message) {
		this(message);
		setType(type);
	}

	public AuthenticationException(Type type, Throwable cause) {
		this(cause);
		setType(type);
	}

	public AuthenticationException(Type type, String message, Throwable cause) {
		this(message, cause);
		setType(type);
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}
}
