package com.wiley.permissions.services;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.permissions.common.dispatcher.ServiceDispatcher;

/**
 * This object represents the base class for all remote services inside the
 * Permissions system.
 *
 * @author lnagy
 */
public abstract class BaseRemoteService
{
	private final static Log log = LogFactory.getLog(BaseRemoteService.class);

	protected abstract String getURI();

	// dispatcher used to communicate between WebServer and MULE_WEB mule instance.
	// This will be instantiated with the remoteWebDispatcher (MULE_WEB) remote instance
	// will cache messages (will NOT send messages), will do statusUpdate,
	// PE search, PE product update, compCopy requests
	protected ServiceDispatcher serviceDispatcher = null;

	protected Object send(String method, Object obj, Map<String, Object> properties)
	{
		try
		{
			// throws MuleException, DispatcherException
			return serviceDispatcher.send(getURI() + method, obj, properties);
		}
		catch (Exception e)
		{
			log.debug ("Failed to call handleErrorMessageOp remotly", e);
			return null;
		}
	}

	/**
	 * Simple wrapper method for the ServiceUtility callService method.
	 *
	 * @param service
	 * @param parameters
	 * @param props
	 * @return
	 * @throws com.wiley.permissions.common.services.ServiceException
	 * @throws com.wiley.permissions.common.dispatcher.DispatcherException
	 */
	public ServiceDispatcher getServiceDispatcher()
	{
		return serviceDispatcher;
	}

	public void setServiceDispatcher(ServiceDispatcher serviceDispatcher)
	{
		this.serviceDispatcher = serviceDispatcher;
	}
}
