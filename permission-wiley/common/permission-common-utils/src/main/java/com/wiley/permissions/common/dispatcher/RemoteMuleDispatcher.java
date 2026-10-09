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
 * Mule remote client implementation of {@link ServiceDispatcher}.
 * org.mule types stay inside this class; callers only see {@link DispatcherException}.
 */
public class RemoteMuleDispatcher implements ServiceDispatcher {

	private static final Log log = LogFactory.getLog(RemoteMuleDispatcher.class);

	/**
	 * This is the default value. If you want to change it, you can set
	 * different values in spring context config files
	 */
	private String serverURL;

	private final MuleClient client;

	public RemoteMuleDispatcher() throws DispatcherException
	{
		log.debug("constructor called");

		try {
			//smarkoff: false with Mule 3 causes exception upon calling send()
			// - "connector.http.mule.default" is stopped (when http endpoint is called)
			// true with Mule 2 causes "already started" exception
			client = new MuleClient(true);
		}
		catch (MuleException e) {
			throw new DispatcherException("Failed to create MuleClient", e);
		}
	}

	public RemoteMuleDispatcher(String serverURL) throws DispatcherException
	{
		this();
		setServerURL(serverURL);
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
	public Object send(String url, Object payload, Map<String, Object> properties, int timeout) throws DispatcherException
	{
		ArgUtil.notNull(url, "url");

		Object replyPayload = null;
		RemoteDispatcher dispatcher = null;
		try {
			dispatcher = client.getRemoteDispatcher(serverURL);

			MuleMessage replyMsg = dispatcher.sendRemote(url, payload, properties, timeout);
			if (replyMsg != null) {
				if (replyMsg.getExceptionPayload() != null) {
					throw toDispatcherException("Error Calling Service", replyMsg);
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
		finally {
			if (dispatcher != null) {
				dispatcher.dispose();
			}
		}

		return replyPayload;
	}

	/**
	 * @param service  Must be non-null
	 * @param parameters  Should probably be non-null
	 * @param props  May be null (converted to empty Map)
	 */
	public Object send(OperationType service, Object parameters, Map<String, Object> props)
	throws DispatcherException
	{
		ArgUtil.notNull(service, "service");

		Object replyPayload = send(service.getEndpoint(), parameters, props);
		return replyPayload;
	}

	/**
	 * @param service  Must be non-null
	 * @param parameters  Should probably be non-null
	 * @param props  May be null (converted to empty Map)
	 * @param timeout  Milliseconds
	 */
	public Object send(OperationType service, Object parameters, Map<String, Object> props, int timeout)
	throws DispatcherException
	{
		ArgUtil.notNull(service, "service");

		Object replyPayload = send(service.getEndpoint(), parameters, props, timeout);
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

		RemoteDispatcher dispatcher = null;
		try {
			dispatcher = client.getRemoteDispatcher(serverURL);

			// With Mule 3.1.2 get exception if use dispatchRemote():
			// - timeout is negative value -- probably a bug so check again with next version of Mule
			// - sendAsyncRemote doesn't report any error but also doesn't work!
			dispatcher.dispatchRemote(url, payload, properties);
		}
		catch (MuleException e) {
			throw new DispatcherException("Error Dispatching Service: " + url, e);
		}
		finally {
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
			throws DispatcherException
	{
		ArgUtil.notNull(service, "service");

		dispatch(service.getEndpoint(), payload, properties);
	}

	public String getServerURL() {
		return serverURL;
	}

	public void setServerURL(String serverURL) {
		this.serverURL = serverURL;
	}

	/**
	 * Convert Mule ExceptionPayload into a plain Throwable-backed DispatcherException.
	 */
	static DispatcherException toDispatcherException(String message, MuleMessage replyMsg)
	{
		Throwable cause = null;
		if (replyMsg != null && replyMsg.getExceptionPayload() != null) {
			cause = replyMsg.getExceptionPayload().getRootException();
			if (cause == null) {
				cause = replyMsg.getExceptionPayload().getException();
			}
		}
		if (cause != null) {
			return new DispatcherException(message, cause);
		}
		return new DispatcherException(message);
	}
}
