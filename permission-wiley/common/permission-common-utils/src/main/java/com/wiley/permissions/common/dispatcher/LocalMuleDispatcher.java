package com.wiley.permissions.common.dispatcher;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.api.context.MuleContextAware;

import com.wiley.sf.common.lang.ArgUtil;

/**
 * Local Mule client implementation of {@link ServiceDispatcher}.
 * org.mule types stay inside this class; callers only see {@link DispatcherException}.
 */
public class LocalMuleDispatcher implements ServiceDispatcher, MuleContextAware {

	private static final Log log = LogFactory.getLog(LocalMuleDispatcher.class);

	// for Mule 3 this will be the interface (api package) instead of the class
	private LocalMuleClient client;

	public LocalMuleDispatcher()
	{
		log.debug("constructor called");

		// smarkoff - don't do this for Mule 3 - uncomment line below in setMuleContext instead
		//client = new MuleClient(false);
	}

	/**
	 * @param url  Must be non-null
	 * @param payload  Should probably be non-null
	 * @param properties  May be null (converted to empty Map)
	 */
	public Object send(String url, Object payload, Map<String, Object> properties) throws DispatcherException
	{
		return send(url, payload, properties, DEFAULT_TIMEOUT);
	}

	/**
	 * @param url  Must be non-null
	 * @param payload  Should probably be non-null
	 * @param properties  May be null (converted to empty Map)
	 * @param timeout  Milliseconds
	 */
	public Object send(String url, Object payload, Map<String, Object> properties, int timeout)
			throws DispatcherException
	{
		ArgUtil.notNull(url, "url");

		if (properties == null) {
			properties = new HashMap<String, Object>();
		}

		Object replyPayload = null;

		try {
			MuleMessage replyMsg = client.send(url, payload, properties, timeout);
			if (replyMsg != null) {
				if (replyMsg.getExceptionPayload() != null) {
					throw RemoteMuleDispatcher.toDispatcherException("Error Calling Service", replyMsg);
				}
				else {
					replyPayload = replyMsg.getPayload();
				}
			}
		}
		catch (DispatcherException e) {
			throw e;
		}
		catch (MuleException e) {
			throw new DispatcherException("Error Calling Service: " + url, e);
		}

		return replyPayload;
	}

	/**
	 * @param service  Must be non-null
	 * @param payload  Should probably be non-null
	 * @param properties  May be null (converted to empty Map)
	 */
	public Object send(OperationType service, Object payload, Map<String, Object> properties) throws DispatcherException
	{
		ArgUtil.notNull(service, "service");

		Object replyPayload = send(service.getEndpoint(), payload, properties);
		return replyPayload;
	}

	/**
	 * @param service  Must be non-null
	 * @param payload  Should probably be non-null
	 * @param properties  May be null (converted to empty Map)
	 * @param timeout  Milliseconds
	 */
	public Object send(OperationType service, Object payload, Map<String, Object> properties, int timeout) throws DispatcherException
	{
		ArgUtil.notNull(service, "service");

		Object replyPayload = send(service.getEndpoint(), payload, properties, timeout);
		return replyPayload;
	}

	/**
	 * @param url  Must be non-null
	 * @param payload  Should probably be non-null
	 * @param properties  May be null (converted to empty Map)
	 */
	public void dispatch(String url, Object payload, Map<String, Object> properties) throws DispatcherException
	{
		ArgUtil.notNull(url, "url");

		if (properties == null) {
			properties = new HashMap<String, Object>();
		}

		try {
			client.dispatch(url, payload, properties);
		}
		catch (MuleException e) {
			throw new DispatcherException("Error Dispatching Service: " + url, e);
		}
	}

	/**
	 * @param service  Must be non-null
	 * @param payload  Should probably be non-null
	 * @param properties  May be null (converted to empty Map)
	 */
	public void dispatch(OperationType service, Object payload, Map<String, Object> properties)
			throws DispatcherException
	{
		ArgUtil.notNull(service, "service");

		dispatch(service.getEndpoint(), payload, properties);
	}

	/** Implements MuleContextAware interface. */
	public void setMuleContext(MuleContext muleContext) {
		log.debug("setMuleContext() called");
		client = muleContext.getClient();
	}
}
