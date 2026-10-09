package com.wiley.permissions.common.dispatcher;

import java.util.Map;

/**
 * Transport-agnostic service dispatcher API. Implementations may use Mule today
 * and Camel later; callers only see {@link DispatcherException}.
 */
public interface ServiceDispatcher {
	/** Default timeout: 60 sec, in ms. */
	public final static int DEFAULT_TIMEOUT = 60000;

	public abstract Object send(String url, Object payload, Map<String, Object> properties)
		throws DispatcherException;

	public abstract Object send(String url, Object payload, Map<String, Object> properties, int timeout)
		throws DispatcherException;

	public Object send(OperationType service, Object payload, Map<String, Object> properties)
		throws DispatcherException;

	public Object send(OperationType service, Object payload, Map<String, Object> properties, int timeout)
		throws DispatcherException;

	/**
	 * Sends a one-way message asynchronously (fire-and-forget).
	 */
	public abstract void dispatch(String url, Object payload, Map<String, Object> properties)
		throws DispatcherException;

	public abstract void dispatch(OperationType service, Object payload, Map<String, Object> properties)
		throws DispatcherException;
}
