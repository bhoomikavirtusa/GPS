package com.wiley.permissions.camel.web.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.sf.common.xml.XSLTransform;

/**
 * Camel port of Mule {@code XsltMessageTransformer#transform} — no org.mule imports.
 * Uses the same XSL classpath resources as peclient / pesearch.
 */
public class XsltProcessor implements Processor {

	private static final Log log = LogFactory.getLog(XsltProcessor.class);

	private final String xsltFile;
	private final boolean saveBefore;

	public XsltProcessor(String xsltFile) {
		this(xsltFile, false);
	}

	public XsltProcessor(String xsltFile, boolean saveBefore) {
		this.xsltFile = xsltFile;
		this.saveBefore = saveBefore;
	}

	@Override
	public void process(Exchange exchange) throws Exception {
		String payload = exchange.getIn().getBody(String.class);
		if (saveBefore) {
			exchange.getIn().setHeader("external_xml", payload);
		}
		if (log.isDebugEnabled()) {
			log.debug("------ Message Before transform ----------");
			log.debug(payload);
		}
		XSLTransform tf = new XSLTransform();
		tf.setXml(payload);
		tf.setXsl(XsltProcessor.class, xsltFile);
		String transformedXml = tf.transformToString();
		if (log.isDebugEnabled()) {
			log.debug("------ Message After transform ----------");
			log.debug(transformedXml);
		}
		exchange.getIn().setBody(transformedXml);
	}
}
