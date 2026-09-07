package com.wiley.permissions.common.transformer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageAwareTransformer;

import com.wiley.sf.common.xml.XSLTransform;

/**
 * This class can be skipped and use instead mule-xml:xslt-transformer
 * I created the class so I can ignore the ReplyTo messages, but I created a filter so
 * the messages are not getting to be transformed anymore
 * Keep it for now for debugging purposes
 *
 * smarkoff: Also keep because now it saves the external_xml so we can
 * store in the database (happens later in the chain).
 *
 * @author lnagy
 */
public class XsltMessageTransformer
extends AbstractMessageAwareTransformer
{
	private static final Log log = LogFactory.getLog(XsltMessageTransformer.class);

	private String xsltFile;

	private boolean saveBefore = false;


	@Override
	public Object transform(MuleMessage muleMessage, String encoding)
			throws TransformerException {
		String payload = (String) muleMessage.getPayload();
		if (saveBefore) {
			muleMessage.setProperty("external_xml", payload);
		}

		log.debug("------ Message Before transform ----------");
		log.debug(payload);

		try {
			XSLTransform tf = new XSLTransform();
			tf.setXml(payload);
			tf.setXsl(XsltMessageTransformer.class, getXsltFile());

			String transformedXml = tf.transformToString();

			log.debug("------ Message After transform ----------");
			log.debug(transformedXml);

			return transformedXml;
		}
		catch (Exception e) {
			// log.debug ();
			throw new TransformerException(this, e);
		}
	}

	public String getXsltFile() {
		return xsltFile;
	}

	public void setXsltFile(String xsltFile) {
		this.xsltFile = xsltFile;
	}

	public boolean getSaveBefore() {
		return saveBefore;
	}

	public void setSaveBefore(boolean b) {
		this.saveBefore = b;
	}

}