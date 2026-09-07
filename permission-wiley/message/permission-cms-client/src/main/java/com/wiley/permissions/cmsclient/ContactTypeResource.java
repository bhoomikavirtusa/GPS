package com.wiley.permissions.cmsclient;

import javax.ws.rs.Path;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.permissions.services.message.RestMessageService;

// TODO SOURCE
@Path("/")
public class ContactTypeResource {

	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(ContactTypeResource.class);

	private RestMessageService restMessageService;

/*	@GET
    @Produces("application/xml")
    @Path("/list")
    // Don't use @Transactional - appears to cause a problem
    //@Transactional(propagation = Propagation.REQUIRED)
    public RestMessage getContactTypeList() throws Exception {
		log.debug("getContactTypeList(): entered...");

		List<ContactRelationshipType> contactTypeList = enterpriseRepository.loadContactTypeList();

		return restMessageService.createMessage (contactTypeList);
    }*/

	public RestMessageService getRestMessageService() {
		return restMessageService;
	}

	public void setRestMessageService(RestMessageService restMessageService) {
		this.restMessageService = restMessageService;
	}
}
