package com.wiley.permissions.cmsclient;

import javax.ws.rs.Path;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.permissions.services.message.RestMessageService;

//TODO SOURCE
@Path("/")
public class CountryResource {

	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(CountryResource.class);

	private RestMessageService restMessageService;

/*	@GET
    @Produces("application/xml")
    @Path("/list")
    // Don't use @Transactional - appears to cause a problem
    //@Transactional(propagation = Propagation.REQUIRED)
    public RestMessage getCountryList() throws Exception {
		log.debug("getCountryList(): entered...");

		List<Country> countryList = enterpriseRepository.loadCountryList();

		return restMessageService.createMessage (countryList);
    }*/

	public RestMessageService getRestMessageService() {
		return restMessageService;
	}

	public void setRestMessageService(RestMessageService restMessageService) {
		this.restMessageService = restMessageService;
	}
}
