package com.wiley.permissions.security.web;


/**
 *
 * @author ttidwell
 */
public class PermissionsSecurityException
// TODO: extend PermBaseException
//extends PermBaseException - causes build problem - resolve later - dependency issue
extends Exception
{
	private static final long serialVersionUID = 1L;
	
	public PermissionsSecurityException(String message)
	{
		super(message);
	}

	public PermissionsSecurityException(Throwable cause)
	{
		super(cause);
	}

	public PermissionsSecurityException(String message, Throwable cause)
	{
		super(message, cause);
	}
}
