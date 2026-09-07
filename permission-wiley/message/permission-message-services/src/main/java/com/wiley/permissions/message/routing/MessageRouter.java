package com.wiley.permissions.message.routing;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.ExceptionPayload;
import org.mule.api.MuleException;

import com.wiley.permissions.common.dispatcher.DispatcherException;
import com.wiley.permissions.common.dispatcher.ServiceDispatcher;
import com.wiley.permissions.domain.message.Message;
import com.wiley.permissions.domain.message.Message.MessageEntity;
import com.wiley.permissions.domain.message.Message.MessageProcessingDirection;
import com.wiley.permissions.domain.message.Message.MessageStatus;
import com.wiley.permissions.domain.message.MessageException;
import com.wiley.permissions.domain.message.MessageOperation;
import com.wiley.permissions.domain.message.RequiresDelayedProcessingException;


/**
 * This router is in charge of routing messages and caching them to the
 * database before doing so.
 *
 * @author ttidwell
 */
public class MessageRouter
{
	private static final Log log = LogFactory.getLog(MessageRouter.class);

	private Map<MessageEntity, ServiceDispatcher> entityDispatchers = new HashMap<MessageEntity, ServiceDispatcher>();

	private Map<MessageEntity, String> entityOutboundEndpoints = new HashMap<MessageEntity, String>();

	private Map<MessageEntity, String> entityInboundEndpoints = new HashMap<MessageEntity, String>();

	public MessageRouter()
	{
		log.debug("constructor called");
	}

	public Message handleMessage(Message message)
	throws MessageException
	{
		log.debug("-----------------------------------------");
		log.debug("handleMessage(): message id: " + message.getId() + " type: "
			+ message.getType().getCode() + " direction: " + message.getDirection());
		log.debug("-----------------------------------------");
		try
		{
			if (message.getDirection() == MessageProcessingDirection.INCOMING)
			{
				log.debug("-----------------------------------------");
				log.debug("\t\t\t INCOMING MESSAGE");
				routeIncomingMessage(message);
				log.debug("\t\t\t END _ INCOMING MESSAGE");
				log.debug("-----------------------------------------");
			}
			else
			{
				log.debug("-----------------------------------------");
				log.debug("\t\t\t OUTGOING MESSAGE");
				routeOutgoingMessage(message);
				log.debug("\t\t\t END _ OUTGOING MESSAGE");
				log.debug("-----------------------------------------");
			}
		}
		catch (Exception e)
		{
			log.debug("************************************");
			log.debug("\t\t ERROR: " + e.getMessage());
			log.debug("************************************");
			message.setStatus(MessageStatus.FAILED);
			message.setLastFailureMessage(e);
		}

		log.debug("handleMessage(): returning message id " + message.getId() + ", status = " + message.getStatus());
		return message;
	}

	private void routeIncomingMessage(Message message)
	throws MessageException
	{
		log.debug("routeIncomingMessage(): message id = " + message.getId());

		for (MessageOperation operation : message.getOperations())
		{
			MessageStatus finalMessageStatus = MessageStatus.PROCESSED;

			Throwable exception = null;

			String endpoint = entityInboundEndpoints.get(message.getSource());

			log.debug(message.getSource());

			if (endpoint != null)
			{
				log.debug("routeIncomingMessage(): Routing To Endpoint: " + endpoint);

				ServiceDispatcher dispatcher = entityDispatchers.get(MessageEntity.PERMISSIONS);

				try
				{
					log.debug("-----------------------------------------");
					log.debug("\t\t\t dispatcher.send");
					dispatcher.send(endpoint, message, null);
					log.debug("\t\t\t END _ dispatcher.send");
					log.debug("-----------------------------------------");
				}
				catch (MuleException e) {
					exception = e;
					finalMessageStatus = MessageStatus.FAILED;
				}
				catch (DispatcherException e)
				{
					exception = e;
					finalMessageStatus = MessageStatus.FAILED;

					ExceptionPayload umoError = e.getExceptionPayload();

					if (umoError != null)
					{
						exception = umoError.getRootException();

						if (exception instanceof RequiresDelayedProcessingException)
						{
							// This means that we're waiting on more information
							finalMessageStatus = MessageStatus.PENDING_MORE_INFORMATION;
						}
						else
						{
							// We need to just mark this message as a failure and try and
							// redeliver it later.
							finalMessageStatus = MessageStatus.FAILED;
						}
					}
				}
			}
			else
			{
				exception = new MessageException("No Endpoint Found For Operation " + operation.getOperationType());
			}

			message.setStatus(finalMessageStatus);

			message.setLastFailureMessage(exception);

			if (exception != null)
			{
				break;
			}
		}
	}

	private void routeOutgoingMessage(Message message)
	throws MessageException
	{
		List<MessageEntity> targets = message.getTargets();

		log.debug("routeOutgoingMessage(): message id = " + message.getId() + ", targets.size() = " + targets.size());

		if (targets.size() > 0)
		{
			for (MessageEntity target : targets)
			{
				String endpoint = entityOutboundEndpoints.get(target);

				log.debug("routeOutgoingMessage(): sending to target: " + target + ", endpoint = " + endpoint);

				ServiceDispatcher dispatcher = entityDispatchers.get(target);

				MessageStatus finalMessageStatus = MessageStatus.PROCESSED;

				Throwable error = null;

				try
				{
					dispatcher.send(endpoint, message, null);
				}
				catch (MuleException e) {
					error = e;
					log.error("routeOutgoingMessage(): message id " + message.getId() + " failed", e);
					finalMessageStatus = MessageStatus.FAILED;
				}
				catch (DispatcherException e)
				{
					error = e;
					log.error("routeOutgoingMessage(): message id " + message.getId() + " failed", e);
					finalMessageStatus = MessageStatus.FAILED;

					ExceptionPayload umoError = e.getExceptionPayload();

					if (umoError != null)
					{
						error = umoError.getException();

						if (error instanceof RequiresDelayedProcessingException)
						{
							// This means that we're waiting on more information
							finalMessageStatus = MessageStatus.PENDING_MORE_INFORMATION;
						}
						else
						{
							// We need to just mark this message as a failure and try and
							// redeliver it later.
							finalMessageStatus = MessageStatus.FAILED;
						}
					}
				}

				message.setLastFailureMessage(error);
				message.setStatus(finalMessageStatus);

				if (error != null)
				{
					break;
				}
			}

			log.debug("routeOutgoingMessage(): message id " + message.getId() + " final status: " + message.getStatus());
		}
		else
		{
			throw new MessageException("No Targets Defined By This Message");
		}
	}

	/**
	 * This is totally a convenience method to let spring easily set these dispatchers.
	 * @param entityDispatchers
	 */
	public void setEntityDispatcherMap(Map<String, ServiceDispatcher> dispatchers)
	throws IllegalArgumentException
	{
		for (String key : dispatchers.keySet())
		{
			MessageEntity entity = MessageEntity.valueOf(key);

			entityDispatchers.put(entity, dispatchers.get(key));
		}
	}

	/**
	 * This is totally a convenience method to let spring easily set these dispatchers.
	 * @param entityDispatchers
	 */
	public void setEntityOutboundEndpointMap(Map<String, String> endpoints)
	throws IllegalArgumentException
	{
		for (String key : endpoints.keySet())
		{
			MessageEntity entity = MessageEntity.valueOf(key);

			log.debug("Adding Entity Outbound Endpoint: " + entity + " -- " + endpoints.get(key));

			entityOutboundEndpoints.put(entity, endpoints.get(key));
		}
	}

	/**
	 * This is totally a convenience method to let spring easily set these dispatchers.
	 * @param entityDispatchers
	 */
	public void setEntityInboundEndpointMap(Map<String, String> endpoints)
	throws IllegalArgumentException
	{
		for (String key : endpoints.keySet())
		{
			MessageEntity entity = MessageEntity.valueOf(key);

			log.debug("Adding Entity Inbound Endpoint: " + entity + " -- " + endpoints.get(key));

			entityInboundEndpoints.put(entity, endpoints.get(key));
		}
	}

	public Map<MessageEntity, ServiceDispatcher> getEntityDispatchers()
	{
		return entityDispatchers;
	}

	public void setEntityDispatchers(Map<MessageEntity, ServiceDispatcher> entityDispatchers)
	{
		this.entityDispatchers = entityDispatchers;
	}

	public Map<MessageEntity, String> getEntityInboundEndpoints()
	{
		return entityInboundEndpoints;
	}

	public void setEntityInboundEndpoints(Map<MessageEntity, String> entityInboundEndpoints)
	{
		this.entityInboundEndpoints = entityInboundEndpoints;
	}

	public Map<MessageEntity, String> getEntityOutboundEndpoints()
	{
		return entityOutboundEndpoints;
	}

	public void setEntityOutboundEndpoints(Map<MessageEntity, String> entityOutboundEndpoints)
	{
		this.entityOutboundEndpoints = entityOutboundEndpoints;
	}
}
