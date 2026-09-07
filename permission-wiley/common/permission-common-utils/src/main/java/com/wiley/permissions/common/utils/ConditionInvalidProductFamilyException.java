package com.wiley.permissions.common.utils;

/**
 * 
 * @author smarkoff
 */
public class ConditionInvalidProductFamilyException extends ConditionValidationException {

	private static final long serialVersionUID = 1L;

	public ConditionInvalidProductFamilyException(String invalidValue) {
		super("condition.error.invalid.product.family", new Object [] { invalidValue} );
	}
}