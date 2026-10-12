package com.wiley.permissions.camel.web;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.camel.CamelExecutionException;
import org.apache.camel.ProducerTemplate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.permissions.common.dispatcher.DispatcherException;
import com.wiley.permissions.common.dispatcher.OperationType;
import com.wiley.permissions.common.dispatcher.ServiceDispatcher;
import com.wiley.sf.common.lang.ArgUtil;

/**
 * In-process Camel implementation of {@link ServiceDispatcher}.
 * Maps Mule {@code vm://} endpoints to Camel {@code direct:} routes.
 */
public class CamelServiceDispatcher implements ServiceDispatcher {

	private static final Log log = LogFactory.getLog(CamelServiceDispatcher.class);

	private ProducerTemplate producerTemplate;

	public void setProducerTemplate(ProducerTemplate producerTemplate) {
		this.producerTemplate = producerTemplate;
	}

	@Override
	public Object send(String url, Object payload, Map<String, Object> properties) throws DispatcherException {
		return send(url, payload, properties, DEFAULT_TIMEOUT);
	}

	@Override
	public Object send(String url, Object payload, Map<String, Object> properties, int timeout)
			throws DispatcherException {
		ArgUtil.notNull(url, "url");
		String endpoint = toDirect(url);
		Map<String, Object> headers = properties != null ? properties : new HashMap<String, Object>();
		try {
			Future<Object> future = producerTemplate.asyncRequestBodyAndHeaders(endpoint, payload, headers);
			return future.get(timeout, TimeUnit.MILLISECONDS);
		}
		catch (TimeoutException e) {
			throw new DispatcherException("Timeout Calling Service: " + endpoint + " (" + timeout + "ms)", e);
		}
		catch (CamelExecutionException e) {
			throw unwrap("Error Calling Service: " + endpoint, e);
		}
		catch (Exception e) {
			throw new DispatcherException("Error Calling Service: " + endpoint, e);
		}
	}

	@Override
	public Object send(OperationType service, Object payload, Map<String, Object> properties)
			throws DispatcherException {
		ArgUtil.notNull(service, "service");
		return send(service.getEndpoint(), payload, properties);
	}

	@Override
	public Object send(OperationType service, Object payload, Map<String, Object> properties, int timeout)
			throws DispatcherException {
		ArgUtil.notNull(service, "service");
		return send(service.getEndpoint(), payload, properties, timeout);
	}

	@Override
	public void dispatch(String url, Object payload, Map<String, Object> properties) throws DispatcherException {
		ArgUtil.notNull(url, "url");
		String endpoint = toDirect(url);
		Map<String, Object> headers = properties != null ? properties : new HashMap<String, Object>();
		try {
			producerTemplate.sendBodyAndHeaders(endpoint, payload, headers);
		}
		catch (CamelExecutionException e) {
			throw unwrap("Error Dispatching Service: " + endpoint, e);
		}
		catch (Exception e) {
			throw new DispatcherException("Error Dispatching Service: " + endpoint, e);
		}
	}

	@Override
	public void dispatch(OperationType service, Object payload, Map<String, Object> properties)
			throws DispatcherException {
		ArgUtil.notNull(service, "service");
		dispatch(service.getEndpoint(), payload, properties);
	}

	/**
	 * {@code vm://path} → {@code direct:path}; already-{@code direct:} left as-is.
	 */
	static String toDirect(String url) {
		if (url.startsWith("vm://")) {
			return "direct:" + url.substring("vm://".length());
		}
		if (url.startsWith("direct:")) {
			return url;
		}
		return "direct:" + url;
	}

	private static DispatcherException unwrap(String message, CamelExecutionException e) {
		Throwable cause = e.getCause() != null ? e.getCause() : e;
		log.error(message, cause);
		return new DispatcherException(message, cause);
	}
}
