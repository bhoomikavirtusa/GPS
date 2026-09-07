package com.wiley.permissions.common.dispatcher;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.context.MuleContextAware;
import org.mule.module.client.MuleClient;

import org.mule.api.client.LocalMuleClient;
import com.wiley.sf.common.lang.ArgUtil;

/**
 * <p>
 * This is a dispatcher that only dispatches to the local mule instance.
 * </p>
 *
 * @author ttidwell
 */
public class LocalMuleDispatcher implements ServiceDispatcher, MuleContextAware {

	private static final Log log = LogFactory.getLog(LocalMuleDispatcher.class);

	// for Mule 3 this will be the interface (api package) instead of the class
	//private final MuleClient client;
	private LocalMuleClient client;

	public LocalMuleDispatcher() throws MuleException
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
	public Object send(String url, Object payload, Map<String, Object> properties) throws MuleException, DispatcherException
	{
		return send(url, payload, properties, DEFAULT_TIMEOUT);
	}

	/**
	 * @param url  Must be non-null
	 * @param payload  Should probably be non-null
	 * @param properties  May be null (converted to empty Map)
	 * @throws
	 */
	public Object send(String url, Object payload, Map<String, Object> properties, int timeout)
			throws MuleException, DispatcherException
	{
		ArgUtil.notNull(url, "url");

		if (properties == null) {
			properties = new HashMap<String, Object>();
		}

		Object replyPayload = null;

		//for (String key : properties.keySet()) {
			//client.setProperty(key, properties.get(key));
		//}

		MuleMessage replyMsg = client.send(url, payload, properties, timeout);
		if (replyMsg != null) {
			if (replyMsg.getExceptionPayload() != null) {
				throw new DispatcherException("Error Calling Service", replyMsg.getExceptionPayload());
			}
			else {
				replyPayload = replyMsg.getPayload();
			}
		}

		return replyPayload;
	}

	/**
	 * @param service  Must be non-null
	 * @param payload  Should probably be non-null
	 * @param properties  May be null (converted to empty Map)
	 */
	public Object send(OperationType service, Object payload, Map<String, Object> properties) throws MuleException, DispatcherException
	{
		ArgUtil.notNull(service, "service");

		Object replyPayload = send(service.getEndpoint(), payload, properties);
		return replyPayload;
	}

	/**
	 * @param service  Must be non-null
	 * @param payload  Should probably be non-null
	 * @param properties  May be null (converted to empty Map)
	 * @param timeout  In milliseconds, see Mule documentation for valid values
	 */
	public Object send(OperationType service, Object payload, Map<String, Object> properties, int timeout) throws MuleException, DispatcherException
	{
		ArgUtil.notNull(service, "service");

		Object replyPayload = send(service.getEndpoint(), payload, properties, timeout);
		return replyPayload;
	}

	/**
	 * @param url  Must be non-null
	 * @param payload  Should probably be non-null
	 * @param properties  May be null (converted to empty Map)
	 * @throws MuleException
	 */
	public void dispatch(String url, Object payload, Map<String, Object> properties) throws MuleException
	{
		ArgUtil.notNull(url, "url");

		if (properties == null) {
			properties = new HashMap<String, Object>();
		}

		//for (String key : properties.keySet())
		////{
			//client.setProperty(key, properties.get(key));
		//}

		client.dispatch(url, payload, properties);
	}

	/**
	 * @param service  Must be non-null
	 * @param payload  Should probably be non-null
	 * @param properties  May be null (converted to empty Map)
	 * @throws MuleException
	 */
	public void dispatch(OperationType service, Object payload, Map<String, Object> properties)
			throws MuleException
	{
		ArgUtil.notNull(service, "service");

		dispatch(service.getEndpoint(), payload, properties);
	}

	// smarkoff - for Mule 3
	/** Implements MuleContextAware interface. */
	public void setMuleContext(MuleContext muleContext) {
		log.debug("setMuleContext() called");
		// uncomment next line for Mule 3
		client = muleContext.getClient();
	}
}
