package com.wiley.permissions.common.dispatcher;

import org.mule.api.ExceptionPayload;

import com.wiley.permissions.common.utils.PermBaseException;

public class DispatcherException
extends PermBaseException
{
	private static final long serialVersionUID = 1L;
	
	private ExceptionPayload exceptionPayload;
	
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

	public DispatcherException(String message, ExceptionPayload exceptionPayload)
	{
		super(message);
		this.exceptionPayload = exceptionPayload;		
	}
	
	public ExceptionPayload getExceptionPayload()
	{
		return exceptionPayload;
	}

	public void setExceptionPayload(ExceptionPayload exceptionPayload)
	{
		this.exceptionPayload = exceptionPayload;
	}
}
