package com.wiley.permissions.services.message;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.wiley.permissions.common.dispatcher.DispatcherException;
import com.wiley.permissions.common.dispatcher.OperationType;
import com.wiley.permissions.common.transformer.ObjectToXml;
import com.wiley.permissions.domain.message.Message;
import com.wiley.permissions.domain.message.Message.MessageType;
import com.wiley.permissions.domain.message.MessageException;
import com.wiley.permissions.domain.message.MessageOperation;
import com.wiley.permissions.domain.message.RequiresDelayedProcessingException;
import com.wiley.permissions.domain.persistence.permissions.MessageErrorOp;
import com.wiley.permissions.domain.persistence.permissions.Reference;
import com.wiley.permissions.domain.util.UniqueIdentifierGenerator;
import com.wiley.permissions.repositories.CachedMessageRepository;
import com.wiley.permissions.services.BaseService;
import com.wiley.sf.common.monitor.PerformanceMonitor.PerfTimer;

/**
 * Handles the Generic Perm Message (the default PERM Message).
 * For now, this types of messages come only from MQ,
 * they are cached and the cache processor sends the requests to these Services
 */
public abstract class GenericMessageService extends BaseService
implements MessageService<Message, Message>
{
	private final static Log log = LogFactory.getLog(GenericMessageService.class);

	private boolean enabled = true;  // If false, will not actually send messages

	private CachedMessageRepository cachedMessageRepository;

	/**
	 * Caching the message, does not send the message.
	 * The processor will read the messages from cache, and send them
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public Message sendMessage(Message message) throws MessageException
	{
		PerfTimer timer = getMonitor().startTimer("MessageService::sendMessage");
		log.debug("sendMessage(): message.getId() = " + message.getId());

		if (!enabled)  return null;

		// lnagy - DO NOT COMMENT THIS OUT - the send message will not work properly
		// if we comment out
	    try {
		    String xml = ObjectToXml.objectToXml(message);
		    //log.debug("sendMessage(): xml:\n" + xml);
	    }
	    catch (Exception ex) {
	    	log.error("Object to Xml failed: ", ex);
	    }

		try {
			// Small little hack here to make sure this is set correctly.
			message.setDirection(Message.MessageProcessingDirection.OUTGOING);

			// thru Mule is faster
			try {
				getServiceDispatcher().dispatch(OperationType.CACHE_MESSAGE, message, null);
			} catch (Exception e) {
				log.error("Failed to send message", e);
			}
			// cache directly instead of though Mule call
			// cachedMessageRepository.cacheMessage(message);
		}
		finally {
			timer.stopTimer();
		}
		return message;
	}

	/**
	 * Returns a new message with basic fields pre-filled.
	 * (id, source, direction, sent, protocolVersion, and target)
	 */
	public Message createMessage() {
		Message msg = new Message();
		msg.setId(UniqueIdentifierGenerator.getNextIdentifier());
		msg.setSource(Message.MessageEntity.PERMISSIONS);
		msg.setDirection(Message.MessageProcessingDirection.OUTGOING);
		msg.setSent(System.currentTimeMillis());
		msg.setProtocolVersion("1.0");
		return msg;
	}

	public Message createMessage(List<?> items) {
		return createMessage();
	}

	public abstract void receiveMessage(Message message)
			throws MessageException, RequiresDelayedProcessingException, MuleException, DispatcherException;

	public Message createErrorMessage(String replyId, String code, String referenceExternalId, String text) {
		Message msg = createMessage();
		msg.setType(MessageType.ERROR);
		msg.setReplyId(replyId);

		MessageErrorOp msgErrorOp = new MessageErrorOp();
		msgErrorOp.setCode(code);
		msgErrorOp.setText(text);

		if (StringUtils.isNotBlank(referenceExternalId)) {
			Reference ref = new Reference(referenceExternalId);
			msgErrorOp.setReference(ref);
		}

		List<MessageOperation> opList = new ArrayList<MessageOperation>();
		MessageOperation msgOp = new MessageOperation();
		msgOp.setOperationType(OperationType.ERROR);

		List<Object> itemList = new ArrayList<Object>();
		itemList.add(msgErrorOp);

		msgOp.setItems(itemList);
		opList.add(msgOp);
		msg.setOperations(opList);

		return msg;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public CachedMessageRepository getCachedMessageRepository() {
		return cachedMessageRepository;
	}

	public void setCachedMessageRepository(CachedMessageRepository cachedMessageRepository) {
		this.cachedMessageRepository = cachedMessageRepository;
	}
}
