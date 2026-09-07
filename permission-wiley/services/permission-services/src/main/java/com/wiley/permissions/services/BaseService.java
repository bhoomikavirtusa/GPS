package com.wiley.permissions.services;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.support.ResourceBundleMessageSource;

import com.wiley.permissions.common.dispatcher.ServiceDispatcher;
import com.wiley.sf.common.monitor.PerformanceMonitor;

/**
 * This object represents the base class for all services inside the
 * Permissions system.  It provides an enormous amount of functionality
 * to the extending services, including some basic persistence methods,
 * utility methods to send messages and data requests, and other high-level
 * functionality common to service business logic.
 *
 * @author ttidwell
 */
public abstract class BaseService
{
	@SuppressWarnings("unused")
	private final static Log log = LogFactory.getLog(BaseService.class);

	// dispatcher used to communicate between WebServer and MULE_WEB mule instance.
	// This will be instantiated with the remoteWebDispatcher (MULE_WEB) remote instance
	// will cache messages (will NOT send messages), will do statusUpdate,
	// PE search, PE product update, compCopy requests
	private ServiceDispatcher remoteDispatcher;

	private ServiceDispatcher localDispatcher;

	private ResourceBundleMessageSource messageSource;

	private PerformanceMonitor monitor;

	/**
	 * Consider calling getServiceDispatcher() instead of this method unless
	 * you are sure you want a LocalDispatcher instead of remote in all cases.
	 */
	public ServiceDispatcher getLocalDispatcher() {
		return localDispatcher;
	}

	public void setLocalDispatcher(ServiceDispatcher localDispatcher) {
		this.localDispatcher = localDispatcher;
	}

	/**
	 * Consider calling getServiceDispatcher() instead of this method unless
	 * you are sure you want a RemoteDispatcher instead of local in all cases.
	 */
	public ServiceDispatcher getRemoteDispatcher() {
		return remoteDispatcher;
	}

	public void setRemoteDispatcher(ServiceDispatcher remoteDispatcher) {
		this.remoteDispatcher = remoteDispatcher;
	}

	/**
	 * Generally this is the method you want to call instead of calling getLocalDispatcher()
	 * or getRemoteDispatcher().
	 */
	public ServiceDispatcher getServiceDispatcher() {
		boolean insideMule = (System.getProperty("mule.home") != null);
		log.debug("Printing Dispatcher ------------------------->"+insideMule);
		//return insideMule ? localDispatcher : remoteDispatcher;
		return remoteDispatcher;
	}

	public ResourceBundleMessageSource getMessageSource() {
		return messageSource;
	}

	public void setMessageSource(ResourceBundleMessageSource messageSource) {
		this.messageSource = messageSource;
	}

	public PerformanceMonitor getMonitor() {
		return monitor;
	}

	public void setMonitor(PerformanceMonitor monitor) {
		this.monitor = monitor;
	}
}
