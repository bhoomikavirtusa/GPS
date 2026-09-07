package com.wiley.permissions.services.message;

import java.util.List;

import org.mule.api.MuleException;

import com.wiley.permissions.common.dispatcher.DispatcherException;
import com.wiley.permissions.domain.message.MessageException;
import com.wiley.permissions.domain.message.RequiresDelayedProcessingException;

public interface MessageService <T, V> {

	public V sendMessage(T message) throws MuleException, DispatcherException, MessageException;

	public void receiveMessage(V message)
    throws MessageException, RequiresDelayedProcessingException, MuleException, DispatcherException;

	public T createMessage();

	public T createMessage(List<?> items);

	public T createErrorMessage(String replyId, String code, String referenceExternalId, String text);
}
