package com.wiley.permissions.common.bean;

/**
 *
 * @author ttidwell
 */
public class BeanCopyException
extends Exception
{
	private static final long serialVersionUID = 1L;

	public BeanCopyException()
	{

	}

	public BeanCopyException(String message)
	{
		super(message);
	}

	public BeanCopyException(Throwable e)
	{
		super(e);
	}

	public BeanCopyException(String message, Throwable e)
	{
		super(message, e);
	}
}
