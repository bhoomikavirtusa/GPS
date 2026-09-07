package com.wiley.permissions.domain.message;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageAwareTransformer;

/**
 * Just copies the external_xml saved earlier by XsltMessageTransformer
 * from the MuleMessage properties into our own Message object.
 *
 * @author smarkoff
 */
public class ExternalMessageTransformer extends AbstractMessageAwareTransformer {

	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(ExternalMessageTransformer.class);

	@Override
	public Object transform(MuleMessage muleMessage, String encoding)
			throws TransformerException {
		Message message = (Message) muleMessage.getPayload();
		String externalXml = (String) muleMessage.getProperty("external_xml");
		message.setExternalXml(externalXml);
		return message;
	}

}
