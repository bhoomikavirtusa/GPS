package com.wiley.permissions.services.message;

import java.util.List;


import com.wiley.permissions.common.dispatcher.DispatcherException;
import com.wiley.permissions.domain.message.MessageException;
import com.wiley.permissions.domain.message.RequiresDelayedProcessingException;

public interface MessageService <T, V> {

	public V sendMessage(T message) throws DispatcherException, MessageException;

	public void receiveMessage(V message)
    throws MessageException, RequiresDelayedProcessingException, DispatcherException;

	public T createMessage();

	public T createMessage(List<?> items);

	public T createErrorMessage(String replyId, String code, String referenceExternalId, String text);
}
