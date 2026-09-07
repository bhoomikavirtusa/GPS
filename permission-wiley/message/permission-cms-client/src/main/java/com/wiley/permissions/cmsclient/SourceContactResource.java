package com.wiley.permissions.cmsclient;

import javax.ws.rs.Path;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.permissions.services.SourceService;
import com.wiley.permissions.services.message.RestMessageService;

//TODO SOURCE
@Path("/")
public class SourceContactResource {

	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(SourceContactResource.class);

	private SourceService sourceService;
	private RestMessageService restMessageService;


/*	@POST
	@Produces("application/xml")
    @Consumes("application/xml")
	@Path("/{sourceWid}")
	public RestMessage sourceContact(@PathParam("sourceWid") String sourceExtId, RestMessage eContact) {
		log.debug("sourceContact(): entered");
		Enterprise dEnterprise = null;

		if (null == eContact) {
			return restMessageService.createErrorMessage(null,
				MessageErrorOp.REQUIRED_FIELD_MISSING, "N/A", "Missing Contact Information");
		}

		try {
			 dEnterprise = enterpriseRepository.loadEnterpriseByPermissionsId(sourceExtId);
		} catch (PersistenceException e1) {

		}

		if (null == dEnterprise) {
			return restMessageService.createErrorMessage(null, MessageErrorOp.NOT_FOUND,
					sourceExtId, "The Enterprise Record could not be found");
		}

		List<Object> dList = eContact.getItemList();
		ExtendedEnterpriseContactMessage extContact = (ExtendedEnterpriseContactMessage) dList.get(0);

		if (extContact.getContact() == null) {
			return restMessageService.createErrorMessage(null, MessageErrorOp.REQUIRED_FIELD_MISSING,
					sourceExtId, "The Contact is missing from REST request");
		}

		if (extContact.getType() == null || StringUtils.isBlank(extContact.getType().getCode())) {
			return restMessageService.createErrorMessage(null, MessageErrorOp.REQUIRED_FIELD_MISSING,
					sourceExtId, "The ContactType is missing from REST request");
		}

		if (StringUtils.isBlank(extContact.getFirstName())) {
			return restMessageService.createErrorMessage(null, MessageErrorOp.REQUIRED_FIELD_MISSING,
					sourceExtId, "FirstName is missing from REST request");
		}

		if (StringUtils.isBlank(extContact.getLastName())) {
			return restMessageService.createErrorMessage(null, MessageErrorOp.REQUIRED_FIELD_MISSING,
					sourceExtId, "LastName is missing from REST request");
		}

		EnterpriseContact newEC = extContact.getEnterpriseContact();
		newEC.setEnterprise(dEnterprise);
		Long contactId = newEC.getContact().getId();

		try {
			if (null == contactId || contactId.longValue() == 0) {
				// new Contact
				newEC.setId(null);

			    // There is no cascade on Contact.addresses.
				// Remove any address set or will get transient object error from JPA.
				// Then save address separately.
			    ContactAddress mainAddress = newEC.getContact().getMainAddress();
			    newEC.getContact().setAddresses(null);
			    getEnterpriseService().saveEnterpriseContact(newEC);

			    contactId = newEC.getContact().getId();
			    if (mainAddress != null) {
			    	newEC.getContact().setMainAddress(mainAddress);
		      	    updateContactAddress(newEC.getId(), mainAddress);
			    }
				return restMessageService.createSuccessMessage(MessageSuccessOp.OBJECT_CREATED,
						String.valueOf(contactId), "Contact was created for given source");
			} else {
				// existing Contact

				// set the right id for newEC.getId() for persist down below
				EnterpriseContact ec = enterpriseRepository.loadEnterpriseContact(dEnterprise.getId(), contactId);
				newEC.setId(ec.getId());

				Contact oldContact = enterpriseRepository.loadContactById(contactId);

				// deal with address first, then contact
				ContactAddress mainAddress = newEC.getContact().getMainAddress();
				ContactAddress oldAddress = oldContact.getMainAddress();
				if (mainAddress == null) {
					if (oldAddress != null) {
						oldContact.removeMainAddress();
						getEnterpriseService().deleteContactAddress(oldAddress);
					}
				}
				else {
					if (oldAddress == null) {
						oldContact.setMainAddress(mainAddress);
						updateContactAddress(newEC.getId(), mainAddress);
					}
					else {
						Address target = oldAddress.getAddress();
						Address source = mainAddress.getAddress();

						target.setLineOne(source.getLineOne());
						target.setLineTwo(source.getLineTwo());
						target.setLineThree(source.getLineThree());
						target.setCity(source.getCity());
						target.setProvince(source.getProvince());
						target.setPostalCode(source.getPostalCode());
						target.setCountry(source.getCountry());

						updateContactAddress(newEC.getId(), oldAddress);
					}
				}

				Contact newContact = newEC.getContact();

				oldContact.setCountry(newContact.getCountry());
				oldContact.setEmail(newContact.getEmail());
				oldContact.setFirstName(newContact.getFirstName());
				oldContact.setMiddleName(newContact.getMiddleName());
				oldContact.setLastName(newContact.getLastName());
				oldContact.setGender(newContact.getGender());
				oldContact.setHomePhoneNumber(newContact.getHomePhoneNumber());
				oldContact.setMobilePhoneNumber(newContact.getMobilePhoneNumber());

				newEC.setContact(oldContact);
				newEC = getEnterpriseService().saveEnterpriseContact(newEC);

				contactId = newEC.getContact().getId();
				return restMessageService.createSuccessMessage(MessageSuccessOp.OBJECT_UPDATED,
					String.valueOf(contactId), "Contact " + contactId + " was updated for given source");
			}
		} catch (Exception e) {
			log.error("sourceContact(): ", e);
			return restMessageService.createErrorMessage(null, MessageErrorOp.NOT_FOUND, sourceExtId ,
				"failed to process contact for given source - Reason:" + e.getMessage());
		}
	}

	public void updateContactAddress(Long ecId, ContactAddress contactAddress) {
		log.debug("updateContactAddress(): entered...");

		if (contactAddress == null || contactAddress.getAddress() == null) {
			return;
		}

		// override address type
		AddressType dType = AddressType.MAIN_ADDRESS;

		try {
//			if (null == dAddress.getAddress().getId() || dAddress.getAddress().getId().longValue() == 0) {
//				// new Address
//				// how do we figure out which contact or enterprise this is associated with
//				Address temp = dAddress.getAddress();
//				temp.setId(null);  // make sure to set for adding
//				EnterpriseAddress dEA = new EnterpriseAddress();
//				dEA.setAddress(temp);
//				dEA.setType(dType);
//			    dEA.setEnterprise(dEnterprise);
//				getEnterpriseService().persistContactAddress(ecId, dType, dIA.getAddress());
//				return;
//			} else {
//				// existing address
				getEnterpriseService().saveContactAddress(ecId, dType, contactAddress.getAddress());
				return;
//			}
		} catch (Exception e) {
			log.error("updateContactAddress(): ", e);
		}
	}

	// I believe this service is NOT BEING USED
	@DELETE
	@Produces("application/xml")
	@Path("/address/{contactid}/{addressid}")
	public RestMessage deleteContactAddress(
			@PathParam("contactid") String contactId,
			@PathParam("addressid") String addressId) {
		log.debug("deleteContactAddress(): About to delete contact Address" + contactId);
		try {
			 Long dId = new Long(contactId);
			 Long daId = new Long(addressId);
			 getEnterpriseService().deleteContactAddressByIds(dId, daId);
		}  catch (PersistenceException e) {
			return restMessageService.createErrorMessage(null, MessageErrorOp.NOT_FOUND, contactId,
					"Could not find or delete contact with the given id");
		}

		return restMessageService.createSuccessMessage(
			MessageSuccessOp.OBJECT_DELETED, contactId , "Contact was deleted");
	}

	@DELETE
    @Produces("application/xml")
	@Path("/{sourceWid}/{contactid}")
	public RestMessage deleteSourceContact(@PathParam("sourceWid") String sourceExtId,
			@PathParam("contactid") String contactId) {
		log.debug("deleteSourceContact(): About to delete contact " + contactId);

		Enterprise enterprise = null;

		try {
			 enterprise = enterpriseRepository.loadEnterpriseByPermissionsId(sourceExtId);
		} catch (PersistenceException e1) {

		}

		if (enterprise == null) {
			return restMessageService.createErrorMessage(null, MessageErrorOp.NOT_FOUND,
					sourceExtId, "The Enterprise Record could not be found");
		}

		Long contactIdLong = new Long(contactId);
		EnterpriseContact ec = null;
		try {
		    ec = enterpriseRepository.loadEnterpriseContact(enterprise.getId(), contactIdLong);
		}
		catch (Exception ex) { }

		if (ec == null) {
			String idPair = "(" + sourceExtId + ", " + contactId + ")";
			return restMessageService.createErrorMessage(null, MessageErrorOp.NOT_FOUND,
					idPair, "The EnterpriseContact Record could not be found");
		}

		try {
			getEnterpriseService().deleteEnterpriseContact(ec);
		} catch (Exception f) {
			return restMessageService.createErrorMessage(null, MessageErrorOp.NOT_FOUND,
				String.valueOf(ec.getId()),
				"Could not find or delete EnterpriseContact with the given id");
		}

		return restMessageService.createSuccessMessage(
			MessageSuccessOp.OBJECT_DELETED, contactId, "Contact was deleted");
	}
*/


	public RestMessageService getRestMessageService() {
		return restMessageService;
	}

	public SourceService getSourceService() {
    	return sourceService;
    }

	public void setSourceService(SourceService sourceService) {
    	this.sourceService = sourceService;
    }

	public void setRestMessageService(RestMessageService restMessageService) {
		this.restMessageService = restMessageService;
	}
}
