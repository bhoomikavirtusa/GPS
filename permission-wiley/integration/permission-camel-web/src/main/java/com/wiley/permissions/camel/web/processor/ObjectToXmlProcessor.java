package com.wiley.permissions.camel.web.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import com.wiley.permissions.common.transformer.ObjectToXml;

/**
 * Camel port of Mule {@code ObjectToXmlTransformer#doTransform} — no org.mule imports.
 */
public class ObjectToXmlProcessor implements Processor {

	private final ObjectToXml transformer = new ObjectToXml();

	@Override
	public void process(Exchange exchange) throws Exception {
		Object body = exchange.getIn().getBody();
		if (body == null) {
			return;
		}
		exchange.getIn().setBody(transformer.transform(body));
	}
}
