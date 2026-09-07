package com.wiley.permissions.message.routing;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.wiley.permissions.domain.message.Message;

import com.wiley.permissions.domain.message.MessageException;


/**
 * This router is just for debugging the messages received from MQ (PE)
 *
 * @author lnagy
 */
public class DebugMessageRouter
{
	private static final Log log = LogFactory.getLog(DebugMessageRouter.class);

	public DebugMessageRouter()
	{

	}

	public Message debugMessage(Message message)
	throws MessageException
	{
		log.debug("-----------------------------------------");
		log.debug("Debug Message: " + message);
		log.debug("-----------------------------------------");
		return message;
	}
}
