package com.wiley.permissions.common.utils;

public class ConditionBlankException extends ConditionValidationException {

	private static final long serialVersionUID = 1L;

	public ConditionBlankException() {
		super("condition.error.blank");
	}
}