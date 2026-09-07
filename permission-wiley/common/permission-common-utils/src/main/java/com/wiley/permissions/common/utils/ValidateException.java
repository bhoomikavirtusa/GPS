package com.wiley.permissions.common.utils;

/**
 * Used to throw a Validation exception like in case of 
 * AssetUse validate method
 * @author lnagy
 */
public class ValidateException extends PermBaseException {

	private static final long serialVersionUID = 1L;

	public ValidateException(String string)
	{
		super(string);
	}
}
