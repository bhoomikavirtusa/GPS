package com.wiley.permissions.common.transaction;

import java.util.Properties;

import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.config.MuleConfiguration;
import org.mule.api.transaction.TransactionManagerFactory;
import org.springframework.context.ApplicationContext;

import com.atomikos.icatch.jta.UserTransactionManager;
import com.wiley.permissions.common.spring.AppContext;

/**
 * This lets Hibernate find our transaction manager instance.
 *
 * This class is referenced by processor-mule-config.xml and web-mule-config.xml
 * (so used for Mule). Don't think it's used for Tomcat.
 *
 * @author ttidwell
 */
public class TransactionManagerLookupUtility implements TransactionManagerFactory
{
	private final static Log log = LogFactory.getLog(TransactionManagerLookupUtility.class);

	// DON'T USE (this method) ANYMORE
	/**
	 * (smarkoff) You should always call this method instead of
	 * getCurrentTransaction().commit() or getNewTransaction() separately
	 * (now the others are private).
	 * This way other services will still have an open transaction to use if needed,
	 * and all the resources from the old transaction are copied to the new one.
	 */
	/**
	public static void commitCurrentTransactionAndStartNew() throws Exception {
		Transaction txOld = getCurrentTransaction();
		Map<String, Object> resourceMap = null;
		if (txOld != null) {
		    resourceMap = TransactionResourceManager.getAllResources(txOld);
		    txOld.commit();  // throws various exceptions
		}

		Transaction txNew = getNewTransaction();  // throws SystemException, NotSupportedException
		if (txOld != null) {
			TransactionResourceManager.addResources(txNew, resourceMap);
		}
	}
	*/

	// DON'T USE (these methods) ANYMORE
	/*
	public static void commitCurrentTransaction() throws Exception {
		Transaction tx = getCurrentTransaction();  // throws SystemException
		if (tx != null)  tx.commit();  // throws various exceptions
	}

	public static void rollbackCurrentTransactionAndStartNew() throws Exception {
		Transaction txOld = getCurrentTransaction();
		Map<String, Object> resourceMap = TransactionResourceManager.getAllResources(txOld);
		txOld.rollback();  // throws various exceptions

		Transaction txNew = getNewTransaction();  // throws SystemException, NotSupportedException
		TransactionResourceManager.addResources(txNew, resourceMap);
	}

	private static Transaction getCurrentTransaction() throws SystemException {
		TransactionManagerLookupUtility lookup = new TransactionManagerLookupUtility();
		TransactionManager tm = lookup.getTransactionManager();
		log.debug ("----------------getCurrentTransaction(): " + tm.getTransaction());
		return tm.getTransaction();  // throws SystemException
	}

	private static Transaction getNewTransaction() throws SystemException, NotSupportedException {
		TransactionManagerLookupUtility lookup = new TransactionManagerLookupUtility();
		TransactionManager tm = lookup.getTransactionManager();
		tm.begin();  // throws NotSupportedException, SystemException
		Transaction transaction = tm.getTransaction();  // throws SystemException
		log.debug ("----------------getNewTransaction(): " + transaction);

		return transaction;
	}
	*/

	// ---- Atomikos
	public TransactionManager getTransactionManager() {
		ApplicationContext ctx = AppContext.getApplicationContext();
		TransactionManager jtaTM  = (TransactionManager) ctx.getBean("transactionManager");
		log.debug ("----------------getTransactionManager(): " + jtaTM);
		return jtaTM;
	}

	// ---- Bitronix
	//public TransactionManager getTransactionManager() {
	//	return TransactionManagerServices.getTransactionManager();
	//}

	/**
	 * Implements TransactionManagerFactory interface (required for Mule).
	 */
	/*@Override
	public TransactionManager create()
	throws Exception
	{
		log.info("About to get a transaction manager...");

		TransactionManager output = getTransactionManager();

		log.info("Returning " + output + " as transaction manager");

		return output;
	}*/

	/**
	 * NEW for Mule 3.
	 * Implements TransactionManagerFactory interface (required for Mule).
	 */
	@Override
	public TransactionManager create(MuleConfiguration config) throws Exception {
		log.info("About to get a transaction manager...");

		TransactionManager output = getTransactionManager();

		log.info("Returning " + output + " as transaction manager");

		return output;
		//return create();
	}

	public void dumpCurrentState (String message) throws Exception {
		// Atomikos
		UserTransactionManager tm = (UserTransactionManager) getTransactionManager();

		// Bitronix
		//BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
		//try {
		//	BitronixTransaction t = tm.getCurrentTransaction();
		//	log.debug(message  + " > CURRENT GRID " + t.getStatusDescription() + " " + t.getGtrid());
		//} catch (Exception e) {}
	}
}
