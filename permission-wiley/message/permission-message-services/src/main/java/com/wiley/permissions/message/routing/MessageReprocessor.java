package com.wiley.permissions.message.routing;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleException;
import org.xml.sax.InputSource;

import com.sun.xml.bind.IDResolver;
import com.wiley.permissions.common.dispatcher.ServiceDispatcher;
import com.wiley.permissions.common.transformer.JAXBContextProvider;
import com.wiley.permissions.domain.message.Message;
import com.wiley.permissions.domain.message.Message.MessageStatus;
import com.wiley.permissions.domain.message.MessageException;
import com.wiley.permissions.domain.persistence.message.CachedMessage;
import com.wiley.permissions.persistence.PersistenceException;
import com.wiley.permissions.repositories.CachedMessageRepository;
import com.wiley.sf.common.xml.bind.CustomIDResolver;

/**
 *
 * @author ttidwell
 */
public class MessageReprocessor
implements Runnable
{
	private static final Log log = LogFactory.getLog(MessageReprocessor.class);

	private final ReentrantLock lock;
	private final Condition waiter;
	private Thread currentThread = null;

	private CachedMessageRepository cmRepo;

	// localMuleServer (localDispatcher) - messages will be reprocess by same Mule instance as the one that
	// does instantiate the reprocessor
	private ServiceDispatcher serviceDispatcher = null;

	private boolean run = false;

	// lnagy - be careful - these are defined also in reprocessor-spring-context.xml
	private int startupDelaySecs = 30;
	private int runIntervalSecs = 1;
	private int retryIntervalSecs = 90;
	private int delayedProcessingRetryIntervalSecs = 90;
	private int processingTimeoutSecs = 18000; // 5h x 60min x 60s
	private int maximumDeliveryAttempts = 3;


	public MessageReprocessor() {
		log.debug("constructor called");
		lock = new ReentrantLock(true);

		waiter = lock.newCondition();
	}

	/**
	 * This method is here simply so that we can wrap it in a spring-
	 * managed transaction and make sure that everything is kosher.
	 * lnagy - do not use transactional - let it commit every operation in this method,
	 * because it is critical
	 */
	// @Transactional(propagation=Propagation.REQUIRES_NEW)
	public void reprocessMessages(Date timestamp)
	throws PersistenceException, JAXBException, MessageException, ClassNotFoundException
	{
		log.trace("reprocessMessages(): timestamp: " + timestamp);

		List<CachedMessage> retryList = new ArrayList<CachedMessage>();

		// OK, we have an EntityManager, now we need to move on to looking for failed messages
		// that failed and need to be retried.

		// important: findByStatusBeforeDate() loads a max of 1 record (used to be 10 but we want strict FIFO),
		// so we don't load all messages into memory if there are a lot

		Date activeBeforeDate = new Date(timestamp.getTime() - (1000 * processingTimeoutSecs));
		int processingCount = cmRepo.countByStatusAfterDate(MessageStatus.PROCESSING, activeBeforeDate);
		if (processingCount > 0) {
			log.trace("reprocessMessages(): " + processingCount + " still PROCESSING so won't start more messages");
			return;
		}

		List<CachedMessage> tmpList = cmRepo.findByStatusBeforeDate(MessageStatus.NEW, timestamp);

		retryList.addAll(tmpList);

		logBasedOnSize(tmpList, "reprocessMessages(): Found " + tmpList.size() + " new messages to retry before " + timestamp.toString());

		Date failedBeforeDate = new Date(timestamp.getTime() - (1000 * retryIntervalSecs));

		tmpList = cmRepo.findByStatusBeforeDate(MessageStatus.FAILED, failedBeforeDate);

		retryList.addAll(tmpList);

		logBasedOnSize(tmpList, "reprocessMessages(): Found " + tmpList.size() + " failed messages to retry before " + failedBeforeDate.toString());

		// Now we need messages that were delayed
		Date delayedBeforeDate = new Date(timestamp.getTime() - (1000 * delayedProcessingRetryIntervalSecs));

		tmpList = cmRepo.findByStatusBeforeDate(MessageStatus.PENDING_MORE_INFORMATION, delayedBeforeDate);

		retryList.addAll(tmpList);

		logBasedOnSize(tmpList, "reprocessMessages(): Found " + tmpList.size() + " delayed messages to retry before" + delayedBeforeDate.toString());

		// Finally, we need messages that are processing and probably timed out.
		tmpList = cmRepo.findByStatusBeforeDate(MessageStatus.PROCESSING, activeBeforeDate);

		retryList.addAll(tmpList);

		logBasedOnSize(tmpList, "reprocessMessages(): Found " + tmpList.size() + " processing messages to retry before " + activeBeforeDate.toString());

		logBasedOnSize(retryList, "reprocessMessages(): Found " + retryList.size() + " total messages to retry.");
		
		if (retryList.size() < 1) {
			log.trace("reprocessMessages(): nothing to process, existing");
			return;
		}


		JAXBContext jaxbContext = JAXBContextProvider.getContext(Message.class); //JAXBContext.newInstance(Message.class);

		Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

		// we need the customIDResolver because of same IDREF values and IDREFs missing full objects
		unmarshaller.setProperty(IDResolver.class.getName(), new CustomIDResolver("com.wiley.permissions.domain.persistence.permissions.Bundle"));
			// throws JAXBException

		for (CachedMessage msg : retryList) {
			if (msg.getDeliveryAttempts() < maximumDeliveryAttempts) {
				try {
					processMessage(msg, unmarshaller);
				}
				catch (Exception ex) {
					log.error("reprocessMessages(): Error Reprocessing Message id " + msg.getId() + ": ", ex);
				}
			}
			else {
				cmRepo.markTerminallyFailed(msg);
			}
		}
	}

	private void logBasedOnSize(List<?> list, String msg) {
		if (list.size() == 0) {
			log.trace(msg);
		}
		else {
			log.debug(msg);
		}
	}

	private void processMessage(CachedMessage msg, Unmarshaller unmarshaller) throws JAXBException, MessageException, MuleException {
		String data = msg.getMessage();

		StringReader reader = new StringReader(data);

		Message tmpMessage = (Message) unmarshaller.unmarshal(new InputSource(reader));

		reader.close();

		log.debug ("processMessage(): preProcessMessage - update message status to PROCESSING");

		cmRepo.preProcessMessage(tmpMessage);

		serviceDispatcher.dispatch("vm://messageHandler", tmpMessage, null);
		// throws MuleException

		log.debug("processMessage(): messageHandler was called ... " +
			"now up to mule to actually send the message");
	}

	public void run() {
		// Wait here for xx seconds just because the MULE has the
		// components not configured yet.
		try {
			Thread.sleep(startupDelaySecs * 1000);
		}
		catch (InterruptedException e1) {
			e1.printStackTrace();
		}

		while (run) {
			log.trace("run(): thread waiting...");
			try {
				lock.lockInterruptibly();
				waiter.await(runIntervalSecs, TimeUnit.SECONDS);
			}
			catch (InterruptedException e) {
				log.error("run(): MessageReprocessor Waiter Interrupted In Run Loop", e);
			}
			finally {
				lock.unlock();
			}

			log.trace("run(): thread running...");
			if (run) {
				try {
					reprocessMessages (new Date());
				}
				catch (Exception e) {
					log.error("run(): Error Reprocessing Messages", e);
				}
			}
		}
	}

	public void start() {
		log.debug("start(): called...");

		run = true;
		currentThread = new Thread(this);
		currentThread.start();
	}

	public void stop() {
		log.debug("stop(): called...");
		run = false;

		if (currentThread != null) {
			try {
				lock.lock();
				waiter.signal();
			}
			finally {
				lock.unlock();
			}

			try {
				currentThread.join();
			}
			catch (InterruptedException e) {
				log.error("stop(): Error Joining MessageReprocessor Thread", e);
			}
		}
	}

	public Thread getCurrentThread() {
		return currentThread;
	}

	public void setCurrentThread(Thread currentThread) {
		this.currentThread = currentThread;
	}

	public CachedMessageRepository getCachedMessageRepository() {
		return cmRepo;
	}

	public void setCachedMessageRepository(CachedMessageRepository cmRepo) {
		this.cmRepo = cmRepo;
	}

	public ServiceDispatcher getServiceDispatcher() {
		return serviceDispatcher;
	}

	public void setServiceDispatcher(ServiceDispatcher messageDispatcher) {
		this.serviceDispatcher = messageDispatcher;
	}

	public boolean isRun() {
		return run;
	}

	public void setRun(boolean run) {
		this.run = run;
	}

	public int getStartupDelaySecs() {
		return startupDelaySecs;
	}

	public void setStartupDelaySecs(int startupDelaySecs) {
		this.startupDelaySecs = startupDelaySecs;
	}

	public int getRunIntervalSecs() {
		return runIntervalSecs;
	}

	public void setRunIntervalSecs(int runIntervalSecs) {
		this.runIntervalSecs = runIntervalSecs;
	}

	public int getRetryIntervalSecs() {
		return retryIntervalSecs;
	}

	public void setRetryIntervalSecs(int retryIntervalSecs) {
		this.retryIntervalSecs = retryIntervalSecs;
	}

	public int getDelayedProcessingRetryIntervalSecs() {
		return delayedProcessingRetryIntervalSecs;
	}

	public void setDelayedProcessingRetryIntervalSecs(int delayedProcessingRetryIntervalSecs) {
		this.delayedProcessingRetryIntervalSecs = delayedProcessingRetryIntervalSecs;
	}

	public int getProcessingTimeoutSecs() {
		return processingTimeoutSecs;
	}

	public void setProcessingTimeoutSecs(int processingTimeoutSecs) {
		this.processingTimeoutSecs = processingTimeoutSecs;
	}

	public int getMaximumDeliveryAttempts() {
		return maximumDeliveryAttempts;
	}

	public void setMaximumDeliveryAttempts(int maximumDeliveryAttempts) {
		this.maximumDeliveryAttempts = maximumDeliveryAttempts;
	}
}
