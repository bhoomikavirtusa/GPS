package com.wiley.permissions.peclient;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.permissions.domain.message.Message;
import com.wiley.permissions.services.message.PEMessageService;

@Path("/")
public class ProductResource {

	private static final Log log = LogFactory.getLog(ProductResource.class);

	private PEMessageService peMessageService;

	@POST
	@Path("/REQUEST")
	@Produces("application/xml")
	@Consumes("application/xml")
	public String sendProductUpdateMessage(String eSource)
	{

		log.debug("sendProductUpdateMessage(): entered...EXTERNAL MESSAGE " + eSource);

		try
		{
			Message source = getPeMessageService().convertPEXmlToMessage(eSource, "/xsl/pe-request-to-permissions(restlet).xsl");

			return getPeMessageService().createProductUpdateReplyMessage(source);
		}
		catch (Exception e)
		{
			Message msg = getPeMessageService().createErrorMessage(null, "1001", null, e.getMessage());

			return getPeMessageService().convertMessageToPEXml(msg, "/xsl/permissions-reply-to-pe(restlet).xsl");
		}
	}

	public PEMessageService getPeMessageService()
	{
		return peMessageService;
	}

	public void setPeMessageService(PEMessageService peMessageService)
	{
		this.peMessageService = peMessageService;
	}
}
