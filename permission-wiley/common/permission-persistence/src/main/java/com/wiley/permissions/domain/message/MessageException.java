package com.wiley.permissions.domain.message;

import java.io.Serializable;

import com.wiley.permissions.common.utils.PermBaseException;

public class MessageException
extends PermBaseException
implements Serializable
{
	private static final long serialVersionUID = 1L;

	private String messageId;

	public MessageException(String messageId, String string) {
		super(string);
		this.messageId = messageId;
	}

	public MessageException(String string) {
		super(string);
	}

	public MessageException(String string, Throwable cause) {
		super(string, cause);
	}

	public MessageException(Throwable cause) {
		super(cause);
	}

	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}
}
