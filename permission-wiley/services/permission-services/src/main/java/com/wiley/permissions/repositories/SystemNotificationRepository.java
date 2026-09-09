package com.wiley.permissions.repositories;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.wiley.permissions.domain.message.MessageException;
import com.wiley.permissions.domain.persistence.permissions.SystemNotification;
import com.wiley.permissions.persistence.PersistenceException;
import com.wiley.permissions.persistence.repository.JPARepository;
import com.wiley.sf.common.monitor.PerformanceMonitor.PerfTimer;

/**
 * All System Notification related database operations
 * The load/find methods can be used directly if wanted
 * The update/save should be used only thru the ProductService interface
 *
 * @author nmedrano
 */
public class SystemNotificationRepository extends JPARepository {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(SystemNotificationRepository.class);

	@PersistenceContext(unitName = "permissions")
	protected EntityManager entityManager;

	@Override
	protected EntityManager getEntityManager() {
		return entityManager;
	}


	/**
	 * Returns a list of notifications
	 *
	 * @param userId
	 * @return list of products
	 * @throws PersistenceException
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<SystemNotification> loadActiveNotifications() throws PersistenceException
	{
		PerfTimer timer = monitor.startTimer("SystemNotification::loadActiveNotifications");
		TypedQuery<SystemNotification> query = entityManager.createQuery(
				"from SystemNotification sn where sn.fromDate <= date(NOW()) and sn.toDate >= date(NOW())" +
				" order by sn.fromDate, sn.toDate ", SystemNotification.class);
		List<SystemNotification> result = query.getResultList();
		timer.stopTimer();
		return result;
	}

	/**
	 * Returns a list of notifications
	 *
	 * @param userId
	 * @return list of products
	 * @throws PersistenceException
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<SystemNotification> loadPendingNotifications() throws PersistenceException
	{
		PerfTimer timer = monitor.startTimer("SystemNotification::loadActiveNotifications");
		TypedQuery<SystemNotification> query = entityManager.createQuery(
				"from SystemNotification sn where sn.fromDate >= date(NOW()) or sn.toDate >= date(NOW()) " +
				" order by sn.fromDate, sn.toDate ", SystemNotification.class);
		List<SystemNotification> result = query.getResultList();
		timer.stopTimer();
		return result;
	}

	/**
	 * Returns a System Notification loaded by Id - returns null if not found.
	 * Will load the commonWork object too, so we have access to commonWork.id
	 * @param Id
	 * @return
	 * @throws PersistenceException
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public SystemNotification loadById(Integer id) throws PersistenceException
	{
			TypedQuery<SystemNotification> query = entityManager.createQuery("from SystemNotification n " +
				"where n.id = ?1",
				SystemNotification.class);
		query.setParameter(1, id);
		return query.getSingleResult();
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void deleteNotification(SystemNotification note)
	    throws PersistenceException, MessageException
	{
       	entityManager.remove(note);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void deleteNotificationById(Integer id)
	    throws PersistenceException, MessageException
	{
		SystemNotification note = loadById(id);
       	entityManager.remove(note);
	}

	@Transactional(propagation=Propagation.REQUIRES_NEW)
	public SystemNotification persistSystemNotification(SystemNotification note) throws Exception
	{
		entityManager.persist(note);
		return note;
	}

}
