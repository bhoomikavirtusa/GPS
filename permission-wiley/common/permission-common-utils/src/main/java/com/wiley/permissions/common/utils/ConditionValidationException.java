package com.wiley.permissions.common.utils;

public class ConditionValidationException extends Exception {

	private static final long serialVersionUID = 1L;

	private String messageKey;
	private Object [] args = null;
	
	public ConditionValidationException(String messageKey)
	{
		super();
		this.messageKey = messageKey;
	}
	
	public ConditionValidationException(String messageKey, Object [] args)
	{
		super();
		this.messageKey = messageKey;
		this.args = args;
	}
	
	public String getMessageKey() {
		return messageKey;
	}
	
	public Object [] getArgs() {
		return args;
	}
}