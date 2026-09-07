package com.wiley.permissions.common.transformer;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.text.NumberFormat;
import java.util.zip.GZIPInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageAwareTransformer;

import com.ibm.jms.JMSBytesMessage;
import com.ibm.jms.JMSTextMessage;

/**
 * Does what org.mule.transport.jms.transformers.JMSMessageToObject does
 * with JMSTextMessage payloads.
 * For a JMSBytesMessage payload,
 * assumes this is GZipped and unzips to a String (assuming UTF-8 encoding).
 *
 * @author smarkoff
 */
public class JMSAndGZipMessageTransformer
extends AbstractMessageAwareTransformer
{
	private static final Log log = LogFactory.getLog(JMSAndGZipMessageTransformer.class);

	@Override
	public Object transform(MuleMessage muleMessage, String encoding)
			throws TransformerException {
		try {
			return transformInner(muleMessage, encoding);
		}
		catch (Exception ex) {
			throw new TransformerException(this, ex);
		}
	}

	private Object transformInner(MuleMessage muleMessage, String encoding) throws Exception {
		Object payload = muleMessage.getPayload();
		String payloadClassName = payload.getClass().getName();
		log.debug("Payload class = " + payloadClassName);

		if (payload instanceof JMSTextMessage) {
			JMSTextMessage textMessage = (JMSTextMessage) payload;
			String s = textMessage.getText();  // throws JMSException
			//log.debug("text payload:\r\n" + s + "\r\n----------(end text payload)---------\r\n");
			return s;
		}
		else if (payload instanceof JMSBytesMessage) {
			JMSBytesMessage bytesMessage = (JMSBytesMessage) payload;
			return ungzipBytesMessage(bytesMessage);  // throws Exception
		}
		else {
			throw new RuntimeException("Unexpected payload type: " + payloadClassName);
		}
	}

	private String ungzipBytesMessage(JMSBytesMessage bytesMessage) throws Exception {
		long startTime = System.currentTimeMillis();
		if (bytesMessage.getBodyLength() > Integer.MAX_VALUE) {
			throw new RuntimeException("Can't handle payload over 2GB.");
		}
		int size = (int) bytesMessage.getBodyLength();
		byte [] array = new byte[size];
		bytesMessage.readBytes(array);
		ByteArrayInputStream bain = new ByteArrayInputStream(array);
		GZIPInputStream gin = new GZIPInputStream(bain);
		InputStreamReader isr = new InputStreamReader(gin, "UTF-8");
		// note the String could end up smaller than the byte array
		// if there are double-byte chars but "size" gives us a good estimate
		// for the char buffer size needed
		StringBuilder sb = new StringBuilder(size);
		int c = isr.read();
		while (c != -1) {
			sb.append((char) c);
			c = isr.read();
		}
		isr.close();
		gin.close();
		bain.close();
		String ret = sb.toString();
		NumberFormat intFormat = NumberFormat.getIntegerInstance();
		long time = System.currentTimeMillis() - startTime;
		log.debug("unzipped payload of " + intFormat.format(array.length)
			+ " bytes -> " + intFormat.format(ret.length()) + " chars, in " + time + " ms.");
		return ret;
	}
}
