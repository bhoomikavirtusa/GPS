package com.wiley.permissions.persistence;

import com.wiley.permissions.common.utils.PermBaseException;

/**
 *
 * @author ttidwell
 */
public class PersistenceException
extends PermBaseException
{
	private static final long serialVersionUID = 1L;

	public PersistenceException(String message)
	{
		super(message);
	}

	public PersistenceException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public PersistenceException(String message, boolean justShowExceptionMessage)
	{
		super(message, justShowExceptionMessage);
	}

	public PersistenceException(Throwable cause)
	{
		super(cause);
	}
}
