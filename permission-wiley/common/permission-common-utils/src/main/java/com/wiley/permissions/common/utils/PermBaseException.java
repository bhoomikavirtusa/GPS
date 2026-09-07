package com.wiley.permissions.common.utils;

public class PermBaseException
extends Exception
{
	private static final long serialVersionUID = 1L;
	
	private boolean justShowExceptionMessage = false;

	public PermBaseException(String string)
	{
		super(string);
	}

	public PermBaseException(String string, Throwable cause)
	{
		super(string, cause);
	}
	
	public PermBaseException(String string, boolean justShowExceptionMessage)
	{
		super(string);
		this.justShowExceptionMessage = justShowExceptionMessage;
	}

	public PermBaseException(Throwable cause)
	{
		super(cause);
	}
	
	public boolean getJustShowExceptionMessage() {
		return justShowExceptionMessage;
	}
	
	public void setJustShowExceptionMessage(boolean b) {
		justShowExceptionMessage = b;
	}
}
