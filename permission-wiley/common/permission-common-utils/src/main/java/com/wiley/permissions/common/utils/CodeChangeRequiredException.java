package com.wiley.permissions.common.utils;

/**
 * Use the exception for places where code needs to be changed.
 * (For temporary use only.)
 * 
 * @author smarkoff
 */
public class CodeChangeRequiredException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	
	public CodeChangeRequiredException()
	{
		super("code change required");
	}
}
