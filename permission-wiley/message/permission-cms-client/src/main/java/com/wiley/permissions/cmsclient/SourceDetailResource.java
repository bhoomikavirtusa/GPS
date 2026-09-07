package com.wiley.permissions.cmsclient;

import javax.ws.rs.Path;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.permissions.services.message.RestMessageService;

//TODO SOURCE
@Path("/")
public class SourceDetailResource {

	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(SourceDetailResource.class);

	private RestMessageService restMessageService;

/*	@GET
    @Produces("application/xml")
    @Path("/{wid}")
    public RestMessage getSourceDetail(@PathParam("wid") String externalId) {
		log.debug("getSourceDetail(): entered...");

		// Note that data from the Source and Enterprise tables has to be combined
		// into a single Source XML object.
		// We are using option 1).
		// Either 1) Create a separate Source object with the right JAXB annotations
		// and copy the data into this
		// or 2) use XSL transforms to deal with the situation

		Source source = new Source();
		ExtendedSourceEnterpriseMessage eSource = new ExtendedSourceEnterpriseMessage();
		Enterprise enterprise = null;

		try {
			source =  enterpriseRepository.loadSourceByExternalId(externalId);
		} catch (PersistenceException e) {
			source = null;
		}

		if (null == source) {
			return restMessageService.createErrorMessage(null, MessageErrorOp.NOT_FOUND,
				externalId, "Could not find source with the given wid");
		} else {
			eSource.setId(source.getId());
			eSource.setExternalId(source.getExternalId());
			eSource.setCreditLine(source.getCreditLine());
			eSource.setName(source.getName());
		    eSource.setDeliveryMethod(source.getDeliveryMethod());
		}

		try {
		    enterprise = enterpriseRepository.loadEnterpriseByPermissionsId(externalId);
		}  catch (PersistenceException e) {
			enterprise = null;
		}

		if (enterprise == null) {
			// Normally if the source is found, the enterprise should exist also.
			log.error("Source found for externalId [" + externalId + "] but enterprise missing!");

			return restMessageService.createErrorMessage(null, MessageErrorOp.NOT_FOUND,
				externalId, "Could not find enterprise with the given wid");
		}
		else {
			eSource.setPhone(enterprise.getPhoneNumber());
			eSource.setFax(enterprise.getFaxNumber());
			eSource.setWebsite(enterprise.getWebAddress());
			eSource.setCoreIdOne(enterprise.getCoreIdOne());
			eSource.setCoreIdTwo(enterprise.getCoreIdTwo());
			eSource.setAddresses(enterprise.getAddresses());
			eSource.setECContactList(enterprise.getContacts());
			eSource.setPermissionType(enterprise.getPermissionType());
			eSource.setCountry(enterprise.getCountry());
			eSource.setLastUpdatedDate(enterprise.getLastUpdated());
		}

		ArrayList<ExtendedSourceEnterpriseMessage> sourceList =
			new ArrayList<ExtendedSourceEnterpriseMessage>();
		sourceList.add(eSource);

		return restMessageService.createMessage (sourceList);
    }*/


	public RestMessageService getRestMessageService() {
		return restMessageService;
	}

	public void setRestMessageService(RestMessageService restMessageService) {
		this.restMessageService = restMessageService;
	}
}
