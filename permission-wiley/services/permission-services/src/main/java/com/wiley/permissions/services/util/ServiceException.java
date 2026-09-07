package com.wiley.permissions.services.util;

import com.wiley.permissions.common.utils.PermBaseException;

public class ServiceException extends PermBaseException
{
	private static final long serialVersionUID = 1L;

	public ServiceException(String message) {
		super(message);
	}

	public ServiceException(String message, Throwable cause) {
		super(message, cause);
	}

	public ServiceException(String message, boolean justShowExceptionMessage) {
		super(message, justShowExceptionMessage);
	}

	public ServiceException(Throwable cause) {
		super(cause);
	}
}
