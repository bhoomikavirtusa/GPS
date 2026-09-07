package com.wiley.permissions.domain.message;

import java.io.Serializable;

import com.wiley.permissions.common.utils.PermBaseException;

public class RequiresDelayedProcessingException
extends PermBaseException
implements Serializable
{
	private static final long serialVersionUID = 1L;

	public RequiresDelayedProcessingException(String message)
	{
		super(message);
	}

	public RequiresDelayedProcessingException(String message, Exception cause)
	{
		super(message, cause);
	}

	public RequiresDelayedProcessingException(Throwable cause)
	{
		super(cause);
	}
}
