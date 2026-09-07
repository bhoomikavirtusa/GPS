package com.wiley.permissions.common.dispatcher;

import java.util.Map;

import org.mule.api.MuleException;

public interface ServiceDispatcher {
	public final static int DEFAULT_TIMEOUT = 60000;  // 60 sec, in ms

	public abstract Object send(String url, Object payload, Map<String, Object> properties)
		throws MuleException, DispatcherException;

	public abstract Object send(String url, Object payload, Map<String, Object> properties, int timeout)
		throws MuleException, DispatcherException;

	public Object send(OperationType service, Object payload, Map<String, Object> properties)
		throws MuleException, DispatcherException;

	/**
	 * Turns out in Mule 2.1.2 the timeout parameter has no effect. You just get whatever
	 * timeout is specified on the connector (we specify in pesearch-mule-config.xml).
	 * So don't use this version of the method.
	 */
	public Object send(OperationType service, Object payload, Map<String, Object> properties, int timeout)
		throws MuleException, DispatcherException;

	public abstract void dispatch(String url, Object payload, Map<String, Object> properties)
		throws MuleException;

	public abstract void dispatch(OperationType service, Object payload, Map<String, Object> properties)
		throws MuleException;
}
