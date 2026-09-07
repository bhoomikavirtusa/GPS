package com.wiley.permissions.repositories;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.wiley.permissions.common.dispatcher.OperationType;
import com.wiley.permissions.domain.message.Message;
import com.wiley.permissions.domain.message.Message.MessageStatus;
import com.wiley.permissions.domain.message.MessageException;
import com.wiley.permissions.domain.message.MessageOperation;
import com.wiley.permissions.domain.persistence.message.CachedMessage;
import com.wiley.permissions.persistence.PersistenceException;
import com.wiley.permissions.persistence.repository.JPARepository;
import com.wiley.permissions.services.CommonWorkService;
import com.wiley.sf.common.lang.ArgUtil;
import com.wiley.sf.common.monitor.PerformanceMonitor.PerfTimer;

@Transactional(propagation=Propagation.REQUIRED)
public class CachedMessageRepository extends JPARepository
{
	private static final Log log = LogFactory.getLog(CachedMessageRepository.class);

	@PersistenceContext(unitName = "permissions")
	protected EntityManager entityManager;

	@Override
	protected EntityManager getEntityManager() {
		return entityManager;
	}

	private CommonWorkService commonWorkService;


	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<CachedMessage> loadByMessageId(String messageId)
	throws PersistenceException
	{
		TypedQuery<CachedMessage> q = entityManager.createNamedQuery("CachedMessage.findByMessageId", CachedMessage.class);
		q.setParameter("messageId", messageId);

		try {
			// lnagy - until we figure out why we have multiple messages, just load all of them
			// return q.getSingleResult();
			return q.getResultList();
		}
		catch (NoResultException e) {
			return null;
		}
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public int countByStatusAfterDate(MessageStatus status, Date date) {
		final String sql = "select count(*) as count from cached_msg where status = ? and last_delivery_attempt > ?";
		Query query = entityManager.createNativeQuery(sql, "scalarCount");
		query.setParameter(1, status.getCode());
		query.setParameter(2, date);
		// For count, DB2 and MS SQL Server return Integer but MySQL returns BigInteger
		// Integer and BigInteger inherit from Number
		Number result = (Number) query.getSingleResult();
		return result.intValue();
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<CachedMessage> findByStatusBeforeDate(MessageStatus status, Date date) throws PersistenceException {
		return findByStatusBeforeAfterDate(status, date, true);
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<CachedMessage> findByStatusAfterDate(MessageStatus status, Date date) throws PersistenceException {
		return findByStatusBeforeAfterDate(status, date, false);
	}

	private List<CachedMessage> findByStatusBeforeAfterDate(MessageStatus status, Date date, boolean before)
	throws PersistenceException
	{
		String queryName = (before ? "CachedMessage.findByStatusBeforeDate" : "CachedMessage.findByStatusAfterDate");
		TypedQuery<CachedMessage> q = entityManager.createNamedQuery(queryName, CachedMessage.class);
		q.setParameter("status", status);
		q.setParameter("date", date);
		// this method is called by MessageReprocessor and don't want to load too many messages into memory at once
		// - now we want strict FIFO processing so just return a single record
		q.setMaxResults(1);

		return q.getResultList();
	}

	/**
	 * smarkoff: No longer using this method
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public Date getSentForLastMasterListReply(String masterListName) {
		String sql = "from CachedMessage cm where cm.operationType = 'MASTER_LIST_UPDATE'"
			+ " and cm.operationSubType = ? and cm.type = 'REPLY'"
			+ " and cm.status = 'PROCESSED' and cm.lastFailureMessage is null"
			+ " order by cm.lastUpdatedDate desc";
		try {
			TypedQuery<CachedMessage> query = entityManager.createQuery(sql, CachedMessage.class);
			query.setParameter(1, masterListName);
			List<CachedMessage> results = query.getResultList();
			if (results.size() > 0) {
				return results.get(0).getSent();
			}
			else  return null;
		}
		catch (Exception ex) {
			log.warn("getSentForLastMasterListReply(): Caught Exception: ", ex);
			return null;
		}
	}

	/**
	 * Load the contents of the CachedMessage table but omitting the large columns
	 * (which are the message XML itself, the external XML, and the failure message).
	 * Loading all the data can be a very large performance hit (speed and memory)
	 * when there are 500+ rows, especially if some of the messages are large.
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<CachedMessage> loadAllPartial() throws PersistenceException {
		PerfTimer timer = monitor.startTimer("CachedMessageRepository::loadAllPartial");

		// Not good - loads everything from db
		//TypedQuery<CachedMessage> query = entityManager.createQuery(
		//	"select cm from CachedMessage cm order by id", CachedMessage.class);

		// list all columns here except failure_message, message_xml, external_xml
		// for which we hardcode to null to avoid loading into memory
		String sql = "select id, message_id, status, direction, type, operation_type, operation_sub_type,"
			+ " source, item_count, metadata_count, delivery_attempts, last_delivery_attempt,"
			+ " sent, created_date, last_updated_date, protocol_version,"
			+ " null as failure_message, null as message_xml, null as external_xml"
			+ " from cached_msg order by id";
		Query query = entityManager.createNativeQuery(sql, CachedMessage.class);

		@SuppressWarnings("unchecked")
		List<CachedMessage> list = query.getResultList();
		timer.stopTimer();
		return list;
	}

	/**
	 * REQUIRES_NEW ensures that commit is done at the end of this method
	 * which is important for the controller which reloads the message list
	 * after calling delete.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void delete(int deleteId) throws Exception {
		String sql = "delete from CachedMessage where id = ?";
		Query q = entityManager.createQuery(sql);
		q.setParameter(1, deleteId);
		q.executeUpdate();
	}

	/**
	 * Delete all messages that are over 3 days old and PROCESSED.
	 *
	 * This method is called by cron (quartz) - see cron-mule-config.xml.
	 * Could get rid of this version of the method and just call clean(days)
	 * directly from cron-mule-config.xml if could specify method param.
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public void clean() throws PersistenceException {
		clean(3);
	}

	/**
	 * Delete all messages that are over X days old and PROCESSED.
	 *
	 * @param days  Must be 0 or greater
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public void clean(int days) throws PersistenceException {
		ArgUtil.notLess0(days, "days");
		// ANSI, MySQL, PostgreSQL, and Oracle: (current_timestamp - interval '5' day)
		// DB2: (current_timestamp - 5 days)
		// SQL Server: (current_timestamp - 5)
		// - Avoid using any SQL syntax for X days - just use a parameter
		final String sql = "delete from cached_msg where status = 'PROCESSED' and LAST_UPDATED_DATE < ?";
		Query q = entityManager.createNativeQuery(sql, CachedMessage.class);
		long ts = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000);
		Timestamp xDaysAgo = new Timestamp(ts);
		q.setParameter(1, xDaysAgo);
		int numRows = q.executeUpdate();
		log.debug("clean(): numRows deleted = " + numRows);
	}

	/**
	 * Check and see how big our backlog is for not processed
	 * (NEW, FAILED, etc) messages and send an email if
	 * we have more than 500 (later make this number configurable).
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public void checkNotProcessedCount() {
		Query query = entityManager.createNativeQuery(
			"select count(*) as count from cached_msg where status != 'PROCESSED'",
			"scalarCount");
		// For count, DB2 and MS SQL Server return Integer but MySQL returns BigInteger
		// Integer and BigInteger inherit from Number
		Number resultNum = (Number) query.getSingleResult();
		int result = resultNum.intValue();

		log.debug("checkNotProcessedCount(): count was " + result);

		if (result > 500) {
			String subject = "Warning: !PROCESSED message count is " + result;
			String msg = "Not Processed Count is currently " + result;
			commonWorkService.sendEmailToAdminReceiver(subject, msg);
		}
	}

	@Transactional(propagation=Propagation.REQUIRED)
	public Message cacheMessage(Message message)
	throws MessageException
	{
		log.debug("cacheMessage(): msg id = " + message.getId());

		try {
			CachedMessage cachedMessage = new CachedMessage(message);  // throws TransformationException

			entityManager.persist(cachedMessage);
		}
		catch (Exception e) {
			String exMsg = "Could Not Cache Message";

			try {
				MessageOperation op = message.getOperations().get(0);
				OperationType opType = op.getOperationType();
				exMsg = exMsg + " of type " + opType.getCode();
			}
			catch (Exception ex) { }

			throw new MessageException(exMsg, e);
		}

		return message;
	}

	@Transactional(propagation=Propagation.REQUIRED)
	public Message preProcessMessage(Message message)
	throws MessageException
	{
		log.debug("preProcessMessage(): message.status = " + message.getStatus());

		List<CachedMessage> cachedMessages = null;

		try {
			cachedMessages = loadByMessageId(message.getId());

			if (cachedMessages == null || cachedMessages.isEmpty()) {
				throw new PersistenceException ("Could not load the message from DB");
			}

			for (CachedMessage cachedMessage : cachedMessages) {
				cachedMessage.setStatus(MessageStatus.PROCESSING);
				cachedMessage.setLastDeliveryAttempt(new Date());
				cachedMessage.setDeliveryAttempts(cachedMessage.getDeliveryAttempts() + 1);

				entityManager.persist(cachedMessage);
			}
		}
		catch (PersistenceException e) {
			throw new MessageException("Could Not Cache Message", e);
		}

		return message;
	}

	@Transactional(propagation=Propagation.REQUIRED)
	public Message postProcessMessage(Message message) {
		log.debug("postProcessMessage(): message.id = " + message.getId());

		List<CachedMessage> cachedMessages = null;

		try {
			log.debug("postProcessMessage(): ---- before loadByMessageId");
			cachedMessages = loadByMessageId(message.getId());
			log.debug("postProcessMessage(): ---- after loadByMessageId");
			if (CollectionUtils.isEmpty(cachedMessages))
			{
				log.debug("postProcessMessage(): ---- cachedMessages is EMPTY");
				cachedMessages = new ArrayList<CachedMessage>(1);
				cachedMessages.add(new CachedMessage(message));
			}

			log.debug("postProcessMessage(): ---- cachedMessages size " + cachedMessages.size());
			for (CachedMessage cachedMessage : cachedMessages) {
				log.debug("postProcessMessage(): ---- cachedMessage messageId, direction = "
					+ cachedMessage.getMessageId() + ", " + message.getDirection());

				cachedMessage.setStatus(message.getStatus());
				cachedMessage.setLastFailureMessage(message.getLastFailureMessage());

				log.debug("postProcessMessage(): ---- Persist " + cachedMessage.getMessageId());
				entityManager.persist(cachedMessage);
			}
		}
		catch (Exception e)	{
			log.error("Could Not Process Message Post Handling", e);
		}

		return message;
	}

	@Transactional(propagation=Propagation.REQUIRED)
	public void markTerminallyFailed(CachedMessage message)
	throws MessageException
	{
		log.debug("markFailed(): message.status = " + message.getStatus());

		List<CachedMessage> cachedMessages = null;

		try {
			cachedMessages = loadByMessageId(message.getMessageId());

			if (cachedMessages == null || cachedMessages.isEmpty()) {
				throw new PersistenceException ("Could not load the message from DB");
			}

			for (CachedMessage cachedMessage : cachedMessages) {
				cachedMessage.setStatus(MessageStatus.TERMINALLY_FAILED);

				entityManager.merge(cachedMessage);
			}
		}
		catch (PersistenceException e) {
			throw new MessageException("Could Not Cache Message", e);
		}
	}

	public CommonWorkService getCommonWorkService() {
		return commonWorkService;
	}

	public void setCommonWorkService(CommonWorkService commonWorkService) {
		this.commonWorkService = commonWorkService;
	}
}
