package com.wiley.permissions.cmsclient;

import javax.ws.rs.Path;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.permissions.services.SourceService;
import com.wiley.permissions.services.message.RestMessageService;

//TODO SOURCE
/**
 * This class is totally out of date (was written for old Wintouch schema).
 * Needs to be updated if ever to be used again.
 */
@Path("/")
public class SourceResource {

	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(SourceResource.class);

	private SourceService sourceService;
	private RestMessageService restMessageService;

/*	@POST
	@Produces("application/xml")
	@Consumes("application/xml")
	@Path("/{wid}")
	public RestMessage saveSource(@PathParam("wid") String externalId, RestMessage eSource) {
		log.debug("saveSource(): entered...");
		String rc = "Could not find source with the given wid";
		Source dOriginalSource = null;
		Enterprise dOriginalEnterprise = null;

		List<Object> dList = eSource.getItemList();
		ExtendedSourceEnterpriseMessage dSource = (ExtendedSourceEnterpriseMessage) dList.get(0);

		try {
			if (dSource.getExternalId().equals("0")) {
				// new source

				dOriginalSource = new Source();
				dOriginalEnterprise = new Enterprise();

				if (StringUtils.isBlank(dSource.getName())) {
					return restMessageService.createErrorMessage(null, MessageErrorOp.REQUIRED_FIELD_MISSING,
							null, "Name is missing from REST request");
				}
				else {
				    dOriginalSource.setName(dSource.getName());
				    dOriginalEnterprise.setName(dOriginalSource.getExternalName());
				}

			    dOriginalSource.setCreditLine(dSource.getCreditLine());
			    dOriginalSource.setDeliveryMethod(dSource.getDeliveryMethod());

				dOriginalEnterprise.setPhoneNumber(dSource.getPhone());
				dOriginalEnterprise.setFaxNumber(dSource.getFax());
				dOriginalEnterprise.setWebAddress(dSource.getWebsite());
				dOriginalEnterprise.setCoreIdOne(dSource.getCoreIdOne());
				dOriginalEnterprise.setCoreIdTwo(dSource.getCoreIdTwo());

				if (dSource.getPermissionType() == null) {
					return restMessageService.createErrorMessage(null, MessageErrorOp.REQUIRED_FIELD_MISSING,
							null, "PermissionType is missing from REST request");
				} else {
					dOriginalEnterprise.setPermissionType(dSource.getPermissionType());
				}

				if (dSource.getCountry() == null) {
					return restMessageService.createErrorMessage(null, MessageErrorOp.REQUIRED_FIELD_MISSING,
							null, "Country is missing from REST request");
				} else {
					dOriginalEnterprise.setCountry(dSource.getCountry());
				}

				// add the new records to database
				rc = "Unable to Create new Source";  // this is in case an exception is trapped
				dOriginalEnterprise.setPermissionsId(null);
				dOriginalSource.setId(null);

				// test for duplicates
				String existingSourceKey =  enterpriseRepository.checkForDuplicateName(dOriginalSource);
				if (null != existingSourceKey) {
					return restMessageService.createErrorMessage(null, MessageErrorOp.DUPLICATE_SOURCE_NAME_ERROR,
							existingSourceKey,
							rc + " reason: Source '" + dSource.getName() +  "' already exists with an id of '" +
							existingSourceKey + "'" );
				}

				Enterprise di = getEnterpriseService().saveEnterprise(dOriginalEnterprise, dOriginalSource, true);


			//	return restMessageService.createSuccessMessage(MessageSuccessOp.OBJECT_CREATED,
			//			dSource.getExternalId() , "Source was created");
				return restMessageService.createSuccessMessage(MessageSuccessOp.OBJECT_CREATED,
						di.getPermissionsId() , "Source was created");
			//	return returnSuccess(MessageSuccessOp.OBJECT_CREATED,
			//			dSource.getExternalId() , "Source was created");
			} else {
				// existing source
				dOriginalSource =  enterpriseRepository.loadSourceByExternalId(dSource.getExternalId());
				if (null == dOriginalSource) {
					return restMessageService.createErrorMessage(null, MessageErrorOp.NOT_FOUND, dSource.getExternalId(),
							"Could not find source with the given wid");
				}

				// existing enterprise
				dOriginalEnterprise = enterpriseRepository.loadEnterpriseByPermissionsId(dSource.getExternalId());
				if (null == dOriginalEnterprise) {
					return restMessageService.createErrorMessage(null, MessageErrorOp.NOT_FOUND, dSource.getExternalId(),
							"Could not find Enterprise with the given wid");
				}

				if (StringUtils.isBlank(dSource.getName())) {
					return restMessageService.createErrorMessage(null, MessageErrorOp.REQUIRED_FIELD_MISSING,
							null, "Name is missing from REST request");
				}
				else {
				    dOriginalSource.setName(dSource.getName());
				    dOriginalEnterprise.setName(dSource.getName());
				}

			    dOriginalSource.setCreditLine(dSource.getCreditLine());
			    dOriginalSource.setDeliveryMethod(dSource.getDeliveryMethod());

				dOriginalEnterprise.setPhoneNumber(dSource.getPhone());
				dOriginalEnterprise.setFaxNumber(dSource.getFax());
				dOriginalEnterprise.setWebAddress(dSource.getWebsite());
				dOriginalEnterprise.setCoreIdOne(dSource.getCoreIdOne());
				dOriginalEnterprise.setCoreIdTwo(dSource.getCoreIdTwo());

				if (dSource.getPermissionType() == null) {
					return restMessageService.createErrorMessage(null, MessageErrorOp.REQUIRED_FIELD_MISSING,
							null, "PermissionType is missing from REST request");
				} else {
					dOriginalEnterprise.setPermissionType(dSource.getPermissionType());
				}

				if (dSource.getCountry() == null) {
					return restMessageService.createErrorMessage(null, MessageErrorOp.REQUIRED_FIELD_MISSING,
							null, "Country is missing from REST request");
				} else {
					dOriginalEnterprise.setCountry(dSource.getCountry());
				}

				 msg does not contain address or contact stuff
				if (null != dSource.getAddresses()) {
					dOriginalEnterprise = mergeAddresses(dOriginalEnterprise, dSource.getAddresses());
				}
				if (null != dSource.getContactList()) {
					dOriginalEnterprise.setContacts(dSource.getECContactList());
				}


				// update database
				rc = "Unable to Update Source with the given wid";  // this is in case an exception is trapped
				getEnterpriseService().saveEnterprise(dOriginalEnterprise, dOriginalSource, false);

				return restMessageService.createSuccessMessage(MessageSuccessOp.OBJECT_UPDATED,
						dSource.getExternalId(), "Source was updated");
			}

		//	String values = "Items in the list :" + dList.size();
		//	String values = dSource.toString();
		} catch (Exception e) {
			if (null == dSource.getExternalId()) dSource.setExternalId("0");
			return restMessageService.createErrorMessage(null, MessageErrorOp.DUPLICATE_SOURCE_NAME_ERROR,
					dSource.getExternalId(),
					rc + " reason: Source '" + dSource.getName() +  "' already exists" );
		}
	}

	// Method not used
	private Enterprise mergeAddresses(Enterprise into, List<EnterpriseAddress> newValues) {
		List<EnterpriseAddress> dUpdated = into.getAddresses();

		for (int x = 0; x < newValues.size(); x++) {
			EnterpriseAddress newValue = newValues.get(x);
			boolean found = false;

			for (int y = 0; y < dUpdated.size(); y ++) {
				EnterpriseAddress current =  dUpdated.get(y);

				if (current.getAddress().getId().equals(newValue.getAddress().getId())) {
					found = true;
					Address from = newValue.getAddress();
					Address to = current.getAddress();
					to.setLineOne(from.getLineOne());
					to.setLineTwo(from.getLineTwo());
					to.setLineThree(from.getLineThree());
					to.setCity(from.getCity());
					to.setPostalCode(from.getPostalCode());
					current.setAddress(to);
					dUpdated.set(y,current);
				}

				if (!found) {
					Address from = newValue.getAddress();
					from.setId(new Long(0));
					dUpdated.add(newValue);
				} else {
					y = dUpdated.size() + 1;
				}
			}

		}

		return into;
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
