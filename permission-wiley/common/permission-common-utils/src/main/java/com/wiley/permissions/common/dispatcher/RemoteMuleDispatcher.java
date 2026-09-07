package com.wiley.permissions.common.dispatcher;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.module.client.RemoteDispatcher;

import com.wiley.sf.common.lang.ArgUtil;

/**
 * <p>
 * This dispatcher is a mule dispatcher used for remote mule instances.
 * </p>
 *
 * @see SmartDispatcher
 */
public class RemoteMuleDispatcher implements ServiceDispatcher {

	private static final Log log = LogFactory.getLog(RemoteMuleDispatcher.class);

	/**
	 * This is the default value. If you want to change it, you can set
	 * different values in spring context config files
	 */
	private String serverURL;

	private final MuleClient client;

	public RemoteMuleDispatcher() throws MuleException
	{
		log.debug("constructor called");

		//smarkoff: false with Mule 3 causes exception upon calling send()
		// - "connector.http.mule.default" is stopped (when http endpoint is called)
		// true with Mule 2 causes "already started" exception
		client = new MuleClient(true);
	}

	public RemoteMuleDispatcher(String serverURL) throws MuleException
	{
		this();
		setServerURL(serverURL);
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
	 */
	public Object send(String url, Object payload, Map<String, Object> properties, int timeout) throws MuleException, DispatcherException
	{
		ArgUtil.notNull(url, "url");

		Object replyPayload = null;
		RemoteDispatcher dispatcher = null;
		try {
			//log.debug("send(): serverURL [" + serverURL + "] url [" + url + "] payload.class.name [" + payload.getClass().getName() + "]");
			dispatcher = client.getRemoteDispatcher(serverURL);  // throws MuleException

			MuleMessage replyMsg = dispatcher.sendRemote(url, payload, properties, timeout);  // throws MuleException
			if (replyMsg != null) {
				if (replyMsg.getExceptionPayload() != null) {
					throw new DispatcherException("Error Calling Service", replyMsg.getExceptionPayload());
				}
				else {
					replyPayload = replyMsg.getPayload();
				}
			}
		}
		finally {
			// smarkoff: Should we call dispose() ?
			// - documentation doesn't say anything about when/if to call this.
			if (dispatcher != null) {
				dispatcher.dispose();
			}
		}

		return replyPayload;
	}

	/**
	 * @param service  Must be non-null
	 * @param payload  Should probably be non-null
	 * @param properties  May be null (converted to empty Map)
	 */
	public Object send(OperationType service, Object parameters, Map<String, Object> props)
	throws MuleException, DispatcherException
	{
		ArgUtil.notNull(service, "service");

		Object replyPayload = send(service.getEndpoint(), parameters, props);  // throws MuleException, DispatcherException
		return replyPayload;
	}

	/**
	 * @param service  Must be non-null
	 * @param payload  Should probably be non-null
	 * @param properties  May be null (converted to empty Map)
	 * @param timeout  In milliseconds, see Mule documentation for valid values
	 */
	public Object send(OperationType service, Object parameters, Map<String, Object> props, int timeout)
	throws MuleException, DispatcherException
	{
		ArgUtil.notNull(service, "service");

		Object replyPayload = send(service.getEndpoint(), parameters, props, timeout);  // throws MuleException, DispatcherException
		return replyPayload;
	}

	/**
	 * @param url  Must be non-null
	 * @param payload  Should probably be non-null
	 * @param properties  May be null (converted to empty Map)
	 */
	public void dispatch(String url, Object payload, Map<String, Object> properties) throws MuleException
	{
		ArgUtil.notNull(url, "url");

		RemoteDispatcher dispatcher = null;
		try {
			//log.debug("send(): serverURL [" + serverURL + "] url [" + url + "] payload.class.name [" + payload.getClass().getName() + "]");
			dispatcher = client.getRemoteDispatcher(serverURL);  // throws MuleException

			// With Mule 3.1.2 get exception if use dispatchRemote():
			// - timeout is negative value -- probably a bug so check again with next version of Mule
			// - sendAsyncRemote doesn't report any error but also doesn't work!
			dispatcher.dispatchRemote(url, payload, properties);
			//dispatcher.sendAsyncRemote(url, payload, properties);  // throws MuleException
		}
		finally {
			// smarkoff: Should we call dispose() ?
			// - documentation doesn't say anything about when/if to call this.
			if (dispatcher != null) {
				dispatcher.dispose();
			}
		}
	}

	/**
	 * @param service  Must be non-null
	 * @param payload  Should probably be non-null
	 * @param properties  May be null (converted to empty Map)
	 */
	public void dispatch(OperationType service, Object payload, Map<String, Object> properties)
			throws MuleException
	{
		ArgUtil.notNull(service, "service");

		dispatch(service.getEndpoint(), payload, properties);  // throws MuleException
	}

	public String getServerURL() {
		return serverURL;
	}

	public void setServerURL(String serverURL) {
		this.serverURL = serverURL;
	}
}
