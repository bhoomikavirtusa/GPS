package com.wiley.permissions.common.utils;

public class ConditionExistsException extends ConditionValidationException {

	private static final long serialVersionUID = 1L;

	public ConditionExistsException() {
		super("condition.error.duplicate");
	}
}