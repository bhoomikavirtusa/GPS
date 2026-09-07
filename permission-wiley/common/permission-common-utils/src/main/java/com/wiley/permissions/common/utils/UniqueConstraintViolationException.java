package com.wiley.permissions.common.utils;

/**
 * 
 * @author smarkoff
 */
public class UniqueConstraintViolationException extends PermBaseException
{
	private static final long serialVersionUID = 1L;
	
	private String existingId = null;

	public UniqueConstraintViolationException(String string)
	{
		super(string);
	}
	
	public UniqueConstraintViolationException(String string, boolean justShowExceptionMessage)
	{
		super(string, justShowExceptionMessage);
	}
	
	public UniqueConstraintViolationException(String string, String existingId, boolean justShowExceptionMessage)
	{
		super(string, justShowExceptionMessage);
		this.existingId = existingId;
	}
	
	public String getExistingId() {
		return existingId;
	}
	
	public void setExsistingId(String existingId) {
		this.existingId = existingId;
	}
}
