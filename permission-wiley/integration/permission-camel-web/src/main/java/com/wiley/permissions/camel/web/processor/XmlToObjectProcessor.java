package com.wiley.permissions.camel.web.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import com.wiley.permissions.common.transformer.XmlToObject;

/**
 * Camel port of Mule {@code XmlToObjectTransformer#doTransform} — no org.mule imports.
 */
public class XmlToObjectProcessor implements Processor {

	private final XmlToObject transformer;

	public XmlToObjectProcessor(Class<?> returnClass) {
		this.transformer = new XmlToObject(returnClass);
	}

	@Override
	public void process(Exchange exchange) throws Exception {
		Object body = exchange.getIn().getBody();
		if (body == null) {
			return;
		}
		String xml = body instanceof String ? (String) body : exchange.getIn().getBody(String.class);
		exchange.getIn().setBody(transformer.transform(xml));
	}
}
