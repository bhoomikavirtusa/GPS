package com.wiley.permissions.cmsclient;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.permissions.domain.message.RestMessage;
import com.wiley.permissions.domain.persistence.permissions.DeliveryMethod;
import com.wiley.permissions.repositories.SourceRepository;
import com.wiley.permissions.services.message.RestMessageService;

@Path("/")
public class DeliveryMethodResource {

	private static final Log log = LogFactory.getLog(DeliveryMethodResource.class);

	private SourceRepository sourceRepository;

	private RestMessageService restMessageService;

	@GET
    @Produces("application/xml")
    @Path("/list")
    // Don't use @Transactional - appears to cause a problem
    //@Transactional(propagation = Propagation.REQUIRED)
    public RestMessage getDeliveryMethodList() throws Exception {
		log.debug("getAddressType(): entered...");

		List<DeliveryMethod> deliveryMethodList = sourceRepository.loadDeliveryMethodList();

		return restMessageService.createMessage (deliveryMethodList);
    }

	
	public SourceRepository getSourceRepository()
	{
		return sourceRepository;
	}

	public void setSourceRepository(SourceRepository sourceRepository)
	{
		this.sourceRepository = sourceRepository;
	}

	public RestMessageService getRestMessageService()
	{
		return restMessageService;
	}

	public void setRestMessageService(RestMessageService restMessageService)
	{
		this.restMessageService = restMessageService;
	}
}
