package com.wiley.permissions.common.dispatcher;

import com.wiley.permissions.common.utils.PermBaseException;

/**
 * Dispatcher failures. Intentionally free of org.mule types so WEB/PROC can move
 * to Camel (or another transport) without changing caller signatures.
 */
public class DispatcherException
extends PermBaseException
{
	private static final long serialVersionUID = 1L;

	public DispatcherException(String message)
	{
		super(message);
	}

	public DispatcherException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public DispatcherException(Throwable cause)
	{
		super(cause);
	}
}
