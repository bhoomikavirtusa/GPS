package com.wiley.permissions.services.message;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.wiley.permissions.common.bean.BeanUtility;
import com.wiley.permissions.common.dispatcher.OperationType;
import com.wiley.permissions.domain.message.Message;
import com.wiley.permissions.domain.message.Message.MessageEntity;
import com.wiley.permissions.domain.message.Message.MessageType;
import com.wiley.permissions.domain.message.MessageError;
import com.wiley.permissions.domain.message.MessageException;
import com.wiley.permissions.domain.message.MessageOperation;
import com.wiley.permissions.domain.message.RequiresDelayedProcessingException;
import com.wiley.permissions.domain.persistence.permissions.Asset;
import com.wiley.permissions.domain.persistence.permissions.AssetUse;
import com.wiley.permissions.domain.persistence.permissions.Component;
import com.wiley.permissions.domain.persistence.permissions.ComponentList;
import com.wiley.permissions.domain.persistence.permissions.Reference;
import com.wiley.permissions.domain.persistence.permissions.Source;
import com.wiley.permissions.domain.persistence.permissions.User;
import com.wiley.sf.common.monitor.PerformanceMonitor.PerfTimer;

/**
 *
 * @author ttidwell
 */
public class CMSMessageService extends GenericMessageService {

	private final static Log log = LogFactory.getLog(CMSMessageService.class);

	private SessionFactory sessionFactory = null;
	private EntityManagerFactory entityManagerFactory;

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public void receiveMessage(Message message) throws MessageException, RequiresDelayedProcessingException
	{
		PerfTimer timer = getMonitor().startTimer("CMSMessageService::receiveMessage");
		log.debug("Receive CMS Message: " + message.getId());
		if (!isEnabled()) return;
		List<MessageError> errorList = message.getErrorList();

		try {
			if (CollectionUtils.isNotEmpty(errorList)) {
				getServiceDispatcher().send(OperationType.ERROR, errorList, null);
				// throws MuleException, DispatcherException
			}

			for (MessageOperation operation : message.getOperations()) {
				OperationType operationType = operation.getOperationType();

				log.debug("Operation type : " + operationType);

				getServiceDispatcher().send(operationType, operation.getItems(), null);
				// throws MuleException, DispatcherException
			}
		}
		catch (Exception e) {
			throw new MessageException("Error Dispatching Message Through Mule", e);
		}
		timer.stopTimer();
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void sendMessage(Object obj, String replyId, OperationType opType) throws MessageException
	{
		if (!isEnabled()) return;
		MessageType msgType = null;
		if (StringUtils.isNotBlank(replyId)) {
			msgType = Message.MessageType.REPLY;
		}
		else {
			msgType = Message.MessageType.NOTIFICATION;
		}
		sendMessage(obj, replyId, opType, msgType);
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void sendMessage(Object obj, String replyId, OperationType opType, MessageType msgType)
			throws MessageException
	{
		PerfTimer timer = getMonitor().startTimer("CMSMessageService::sendMessage::" + obj.getClass());
		if (!isEnabled()) return;
		if (null == msgType) {
			if (StringUtils.isNotBlank(replyId)) {
				msgType = Message.MessageType.REPLY;
			}
			else {
				msgType = Message.MessageType.NOTIFICATION;
			}
		}

		Message msg = createMessage();

		msg.setType(msgType);

		if (StringUtils.isNotBlank(replyId)) {
			msg.setReplyId(replyId);
		}

		MessageOperation msgOp = msg.createMessageOperation(opType);

		msgOp.setOperationType(opType);

		List<Object> itemList = new ArrayList<Object>();

		// So far, this check for List is not actually needed because we
		// never send a list except one that is wrapped by another object
		if (obj instanceof List) {
			@SuppressWarnings("unchecked")
			List<Object> objList = (List<Object>) obj;
			itemList.addAll(objList);
		}
		else {
			itemList.add(obj);
		}

		msgOp.setItems(itemList);

		msg.getOperations().add(msgOp);

		sendMessage(msg);
		// throws MessageException

		timer.stopTimer();
		log.info("sendMessage(): sent message w/o exception");
	}

	/**
	 * Returns a new message with basic fields pre-filled. (id, source,
	 * direction, sent, protocolVersion, and target)
	 */
	@Override
	public Message createMessage() {
		Message msg = super.createMessage();
		List<MessageEntity> targetList = new ArrayList<MessageEntity>();
		targetList.add(MessageEntity.CMS_US);
		msg.setTargets(targetList);

		return msg;
	}

	/**
	 * sends a message where itemsList is just a reference object
	 *
	 * @param externalId
	 *            Should be non-blank
	 */
	private void sendReferenceMessage(MessageType msgType, OperationType opType, String externalId)
			throws MessageException
	{
		sendMessage(new Reference(externalId), null, opType, msgType); // throws
																		// MessageException
		log.info("sendReferenceMessage(): sent message w/o exception");
	}

	public void sendErrorMessageLogException(String replyId, String code, String text,
			String referenceExternalId)
	{
		try {
			sendErrorMessage(replyId, code, text, referenceExternalId);
		}
		catch (Exception ex) {
			log.error("Caught Exception trying to send error msg: ", ex);
		}
	}

	public void sendErrorMessage(String replyId, String code, String text, String referenceExternalId)
			throws MessageException
	{
		Message msg = createErrorMessage(replyId, code, referenceExternalId, text);
		sendMessage(msg); // throws MessageException

		log.info("sendErrorMessage(): sent message w/o exception");
	}

	/**
	 *
	 * @param asset    Must be non-null
	 * @param replyId  May be null
	 */
	public void sendUpdateAssetMessage(Asset asset, String replyId) throws MessageException
	{
		sendMessage(asset, replyId, OperationType.UPDATE_ASSET);
	}

	/**
	 *
	 * @param assetUse  Must be non-null
	 * @param replyId   May be null
	 */
	public void sendUpdateAssetUseMessage(AssetUse au, String replyId)
			throws MessageException
	{
		log.debug("sendUpdateAssetUseMessage(): entered...");
		List<AssetUse> auList = new ArrayList<AssetUse>();
		auList.add(au);
		sendUpdateAssetUseMessages(auList, replyId);
	}

	/**
	 *
	 * @param auList   Must be non-null
	 * @param replyId  May be null
	 */
	public void sendUpdateAssetUseMessages(List<AssetUse> auList, String replyId)
			throws MessageException
	{
		log.debug("sendUpdateAssetUseMessages(): entered...");

		// we clone the assetUse collection (we need just the IDs,
		// because the objects will be loaded inside the thread run method)
		// we need to clone it because otherwise the objects will be attached to 2 sessions and throws
		// an exception :
		// org.hibernate.HibernateException: illegally attempted to associate a proxy with two open Sessions
		List<AssetUse> cloneList = new ArrayList<AssetUse>();
		for (AssetUse au : auList) {
			AssetUse clone = new AssetUse ();
			clone.setId(au.getId());
			cloneList.add (clone);
		}

		new SendCollectionMessageThread<AssetUse>(
				cloneList,
				replyId,
				OperationType.UPDATE_ASSET_USE,
				MessageType.NOTIFICATION);
	}

	/**
	 *
	 * @param source
	 *            Must be non-null
	 * @param replyId
	 *            May be null
	 */
	public void sendUpdateSourceMessage(Source source, String replyId) throws MessageException
	{
		sendMessage(source, replyId, OperationType.UPDATE_SOURCE);
	}
	/**
	 *
	 * @param source
	 *            Must be non-null
	 * @param replyId
	 *            May be null
	 */
	public void sendUpdateUserMessage(User user, String replyId) throws MessageException
	{
		sendMessage(user, replyId, OperationType.UPDATE_USER);
	}
	/**
	 *
	 * @param component
	 *            Must be non-null
	 * @param replyId
	 *            May be null
	 */
	public void sendUpdateComponentMessage(Component component, String replyId) throws MessageException
	{
		sendMessage(component, replyId, OperationType.UPDATE_COMPONENT);
	}

	/**
	 *
	 * @param assetExtId
	 *            Should be non-blank
	 */
	public void sendGetAssetMessage(String assetExtId) throws MessageException
	{
		sendReferenceMessage(Message.MessageType.REQUEST, OperationType.GET_ASSET, assetExtId);
	}

	/**
	 *
	 * @param assetExtId
	 *            Should be non-blank
	 */
	public void sendDeleteAssetMessage(String assetExtId) throws MessageException
	{
		sendReferenceMessage(Message.MessageType.NOTIFICATION, OperationType.DELETE_ASSET, assetExtId);
	}

	public void sendDeleteAssetMessages(List<Reference> externalIds)
	{
		new SendCollectionMessageThread<Reference>(
				externalIds,
				null,
				OperationType.DELETE_ASSET,
				MessageType.NOTIFICATION);
	}

	/**
	 *
	 * @param assetUseExtId
	 *            Should be non-blank
	 */
	public void sendGetAssetUseMessage(String assetUseExtId) throws MessageException
	{
		sendReferenceMessage(Message.MessageType.REQUEST, OperationType.GET_ASSET_USE, assetUseExtId);
	}

	/**
	 *
	 * @param assetUseExtId
	 *            Should be non-blank
	 */
	public void sendDeleteAssetUseMessage(String assetUseExtId) throws MessageException
	{
		sendReferenceMessage(Message.MessageType.NOTIFICATION, OperationType.DELETE_ASSET_USE, assetUseExtId);
	}

	public void sendDeleteAssetUseMessages(List<Reference> externalIds)
	{
		new SendCollectionMessageThread<Reference>(
				externalIds,
				null,
				OperationType.DELETE_ASSET_USE,
				MessageType.NOTIFICATION);
	}

	/**
	 *
	 * @param sourceExtId
	 *            Should be non-blank
	 */
	public void sendGetSourceMessage(String sourceExtId) throws MessageException
	{
		sendReferenceMessage(Message.MessageType.REQUEST, OperationType.GET_SOURCE, sourceExtId);
	}

	/**
	 *
	 * @param sourceExtId
	 *            Should be non-blank
	 */
	public void sendDeleteSourceMessage(String sourceExtId) throws MessageException
	{
		sendReferenceMessage(Message.MessageType.NOTIFICATION, OperationType.DELETE_SOURCE, sourceExtId);
	}

	/**
	 *
	 * @param componentExtId
	 *            Should be non-blank
	 */
	public void sendGetComponentMessage(String componentExtId) throws MessageException
	{
		sendReferenceMessage(Message.MessageType.REQUEST, OperationType.GET_COMPONENT, componentExtId);
	}

	/**
	 *
	 * @param sourceExtId
	 *            Should be non-blank
	 */
	public void sendDeleteComponentMessage(String componentExtId) throws MessageException
	{
		sendReferenceMessage(Message.MessageType.NOTIFICATION, OperationType.DELETE_COMPONENT, componentExtId);
	}

	/**
	 *
	 * @param productExtId
	 *            Should be non-blank
	 */
	public void sendGetComponentListMessage(String cwExtId) throws MessageException
	{
		sendReferenceMessage(
				Message.MessageType.REQUEST,
				OperationType.GET_COMPONENT_LIST,
				cwExtId);
	}

	/**
	 *
	 * @param list     Must be non-null
	 * @param replyId  May be null
	 */
	public void sendReportComponentListMessage(ComponentList list, String replyId)
			throws MessageException
	{
		sendMessage(list, replyId, OperationType.REPORT_COMPONENT_LIST);
	}

	@Transactional
	public class SendCollectionMessageThread<T> extends Thread {
		List<T> collection = null;
		String replyId = null;
		OperationType operation = null;
		MessageType msgType = null;

		SendCollectionMessageThread(List<T> collection, String replyId, OperationType operation,
				MessageType msgType)
		{
			this.collection = collection;
			this.replyId = replyId;
			this.operation = operation;
			this.msgType = msgType;
			start();
		}

		@Override
		public void run() {
			EntityManager em = null;
			try {
				em = getEntityManagerFactory().createEntityManager();
				//Session ses = (Session)em.getDelegate();
				Session session = getSessionFactory().openSession();
				for (T item : collection) {
					// if entity type we attach it to the session
					Entity entity = BeanUtility.getAnnotation(Entity.class, item.getClass());
					Object loadedItem = null;
					if (null != entity) {
						// this LOCK does nothing, just attaches the object to
						// the session
						session.lock(item, LockMode.NONE);
						Object identifier = session.getIdentifier(item);
						log.debug("run(): item IDENTIFIER " + identifier);

						loadedItem = em.find(item.getClass(), identifier);
					}

					sendMessage(loadedItem, replyId, operation, msgType);
				}
			}
			catch (Exception ex) {
				log.debug("SendCollectionMessageThread(): caught exception: ", ex);
			}
			finally {
				em.close();
			}
		}
	}

	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public EntityManagerFactory getEntityManagerFactory() {
		return entityManagerFactory;
	}

	public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory) {
		this.entityManagerFactory = entityManagerFactory;
	}
}
