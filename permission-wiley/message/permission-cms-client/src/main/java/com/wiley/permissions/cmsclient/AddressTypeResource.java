package com.wiley.permissions.cmsclient;

import javax.ws.rs.Path;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.permissions.services.message.RestMessageService;

//TODO SOURCE
@Path("/")
public class AddressTypeResource {

	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(AddressTypeResource.class);


	private RestMessageService restMessageService;

/*	@GET
    @Produces("application/xml")
    @Path("/list")
    // Don't use @Transactional - appears to cause a problem
    //@Transactional(propagation = Propagation.REQUIRED)
    public RestMessage getAddressTypeList() throws Exception {

		log.debug("getAddressType(): entered...");

		List<AddressType> addressTypeList = enterpriseRepository.loadAddressTypeList();

		return restMessageService.createMessage (addressTypeList);
    }*/

//	 @PUT
//	 @Path("/addlist")
//	  public void setAddressType(@PathParam("loc") String locationId) {
//		 // should not be able to add locations to wintouch so do nothing
//	 }
//

	public RestMessageService getRestMessageService() {
		return restMessageService;
	}

	public void setRestMessageService(RestMessageService restMessageService) {
		this.restMessageService = restMessageService;
	}
}
