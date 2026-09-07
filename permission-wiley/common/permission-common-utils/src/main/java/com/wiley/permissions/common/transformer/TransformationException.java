package com.wiley.permissions.common.transformer;

import com.wiley.permissions.common.utils.PermBaseException;

/**
 *
 * @author ttidwell
 */
public class TransformationException
extends PermBaseException
{
	private static final long serialVersionUID = 1L;

	
	public TransformationException(String message)
	{
		super(message);
	}

	public TransformationException(Throwable cause)
	{
		super(cause);
	}

	public TransformationException(String message, Throwable cause)
	{
		super(message, cause);
	}
}
