package com.wiley.permissions.common.bean;

/**
 *
 * @author ttidwell
 */
public class BeanMergeException
extends Exception
{
	private static final long serialVersionUID = 1L;

	public BeanMergeException()
	{

	}

	public BeanMergeException(String message)
	{
		super(message);
	}

	public BeanMergeException(Throwable e)
	{
		super(e);
	}

	public BeanMergeException(String message, Throwable e)
	{
		super(message, e);
	}
}
