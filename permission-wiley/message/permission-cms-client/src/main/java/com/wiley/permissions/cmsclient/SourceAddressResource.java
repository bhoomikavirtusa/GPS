package com.wiley.permissions.cmsclient;

import javax.ws.rs.Path;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.permissions.services.SourceService;
import com.wiley.permissions.services.message.RestMessageService;

//TODO SOURCE
@Path("/")
public class SourceAddressResource {

	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(SourceAddressResource.class);

	private SourceService sourceService;
	private RestMessageService restMessageService;

/*	@POST
	@Produces("application/xml")
    @Consumes("application/xml")
	@Path("/{sourceWid}")
	public RestMessage saveSourceAddress(@PathParam("sourceWid") String sourceExtId, RestMessage eSource) {
		log.debug("saveSourceAddress() - entered");
		Enterprise dEnterprise = null;
		// override address type
		AddressType dAddressType = new AddressType();

		if (null == eSource) {
			return restMessageService.createErrorMessage(null,
				MessageErrorOp.REQUIRED_FIELD_MISSING, "N/A", "Missing Address Information");
		}

		try {
			dEnterprise = enterpriseRepository.loadEnterpriseByPermissionsId(sourceExtId);
		} catch (PersistenceException e1) {

		}

		if (null == dEnterprise) {
			return restMessageService.createErrorMessage(null, MessageErrorOp.NOT_FOUND,
					sourceExtId, "The Enterprise Record could not be found");
		}


		List<Object> dList = eSource.getItemList();
		ExtendedAddressMessage dAddress = (ExtendedAddressMessage) dList.get(0);

		if (null == dAddress) {
			return restMessageService.createErrorMessage(null, MessageErrorOp.REQUIRED_FIELD_MISSING,
					null, "The Address info is missing from REST request");
		}

		if (null == dAddress.getAddress()) {
			return restMessageService.createErrorMessage(null, MessageErrorOp.REQUIRED_FIELD_MISSING,
					null, "The Address part of the rest message is missing");
		}

		dAddressType = dAddress.getType();

		if (dAddressType == null || StringUtils.isBlank(dAddressType.getCode())) {
			return restMessageService.createErrorMessage(null, MessageErrorOp.REQUIRED_FIELD_MISSING,
					null, "The AddressType info is missing from REST request");
		}

		if (StringUtils.isBlank(dAddress.getLineOne())) {
			return restMessageService.createErrorMessage(null, MessageErrorOp.REQUIRED_FIELD_MISSING,
					null, "The Address Line1 is missing from REST request");
		}

		if (StringUtils.isBlank(dAddress.getCity())) {
			return restMessageService.createErrorMessage(null, MessageErrorOp.REQUIRED_FIELD_MISSING,
					null, "The Address City is missing from REST request");
		}

		if (StringUtils.isBlank(dAddress.getPostalCode())) {
			return restMessageService.createErrorMessage(null, MessageErrorOp.REQUIRED_FIELD_MISSING,
					null, "The Address Postal Code is missing from REST request");
		}

		if (dAddress.getCountry() == null || StringUtils.isBlank(dAddress.getCountry().getCode())) {
			return restMessageService.createErrorMessage(null, MessageErrorOp.REQUIRED_FIELD_MISSING,
					null, "The Address Country is missing from REST request");
		}

		try {
			Long addressId = dAddress.getId();
			if (null == addressId || addressId.longValue() == 0) {
				// new Address
				Address tempAddress = dAddress.getAddress();
				tempAddress.setId(null);

			    try {
				    EnterpriseAddress dEA = getEnterpriseService().saveEnterpriseAddress(
				    	dEnterprise.getId(), dAddressType, tempAddress);

				    return restMessageService.createSuccessMessage(MessageSuccessOp.OBJECT_CREATED,
						String.valueOf(dEA.getAddress().getId()), "Address was created for given source");
			    }
			    catch (DuplicateAddressTypeException ex) {
					return restMessageService.createErrorMessage(null, MessageErrorOp.DUPLICATE_ADDRESS_TYPE_ERROR,
							String.valueOf(ex.getExistingId()), ex.getMessage());
			    }
			} else {
				try {
				    EnterpriseAddress dEA = getEnterpriseService().saveEnterpriseAddress(
						dEnterprise.getId(), dAddressType, dAddress.getAddress());

				    return restMessageService.createSuccessMessage(MessageSuccessOp.OBJECT_UPDATED,
					    String.valueOf(dEA.getAddress().getId()), "Address was updated for given source");
				}
				catch (DuplicateAddressTypeException ex) {
					return restMessageService.createErrorMessage(null, MessageErrorOp.DUPLICATE_ADDRESS_TYPE_ERROR,
							String.valueOf(ex.getExistingId()), ex.getMessage());
				}
			}
		} catch (Exception e) {
			return restMessageService.createErrorMessage(null, MessageErrorOp.NOT_FOUND, sourceExtId,
				"failed to process address for given source - Reason:" + e.getMessage());
		}
	}

	@DELETE
    @Produces("application/xml")
	@Path("/{sourceWid}/{addressid}")
	public RestMessage deleteSourceAddress(
		@PathParam("sourceWid") String sourceExtId,
		@PathParam("addressid") String addressId) {

		log.debug("deleteSourceAddress(): About to delete Source Address with id " + addressId);

		Enterprise enterprise = null;
		try {
			enterprise = enterpriseRepository.loadEnterpriseByPermissionsId(sourceExtId);
		} catch (PersistenceException ex) {

		}

		if (enterprise == null) {
			return restMessageService.createErrorMessage(null, MessageErrorOp.NOT_FOUND,
					sourceExtId, "The Enterprise Record could not be found");
		}

		try {
			Long addressIdLong = new Long(addressId);
			getEnterpriseService().deleteEnterpriseAddressByIds(enterprise.getId(), addressIdLong);
		} catch (Exception ex) {
			log.warn("deleteSourceAddress(): ", ex);
			return restMessageService.createErrorMessage(null, MessageErrorOp.NOT_FOUND, addressId,
				"Could not find or delete address with the given id");
		}

		return restMessageService.createSuccessMessage(MessageSuccessOp.OBJECT_DELETED, addressId, "Address was deleted");
	}
*/


	public SourceService getSourceService() {
		return sourceService;
	}

	public void setSourceService(SourceService sourceService) {
		this.sourceService = sourceService;
	}

	public RestMessageService getRestMessageService() {
		return restMessageService;
	}

	public void setRestMessageService(RestMessageService restMessageService) {
		this.restMessageService = restMessageService;
	}
}
