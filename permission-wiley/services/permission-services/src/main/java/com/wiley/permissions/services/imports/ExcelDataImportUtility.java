package com.wiley.permissions.services.imports;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.jxls.reader.XLSDataReadException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.wiley.permissions.common.utils.SrcSrcRefInvCombination;	// Added for DM-533
import com.wiley.permissions.common.utils.ValidateException;
import com.wiley.permissions.domain.persistence.permissions.CommonWork;
import com.wiley.permissions.domain.persistence.permissions.CommonWorkStatus;
import com.wiley.permissions.domain.persistence.permissions.ExtendedAssetUse;
import com.wiley.permissions.domain.persistence.permissions.Source;
import com.wiley.permissions.domain.persistence.permissions.User;
import com.wiley.permissions.persistence.PersistenceException;
import com.wiley.permissions.repositories.CommonWorkRepository;
import com.wiley.permissions.repositories.UserRepository;
import com.wiley.permissions.repositories.SourceRepository;
import com.wiley.permissions.services.CommonWorkService;

public class ExcelDataImportUtility extends CSFilemakerDataImportUtility {

	private static final Log log = LogFactory.getLog(ExcelDataImportUtility.class);
	private static final String ASSET_MAPPING_FILE = "/conf/mapping/assetMapping.xml";

	private ImportAssetsUtility importAssetsUtility;

	private CommonWorkService commonWorkService;

	private CommonWorkRepository cwRepository;
	private UserRepository userRepository;
	private SourceRepository sourceRepository;//Added by santhosh for DM-532

	public static final Map<String, String> headerMapping = new HashMap<String, String>() {
		{
			put("1", ImportAssetsUtility.IGNORE);
			put("description", "assetUse.asset.description");
			put("2", "assetUse.position");
			put("3", "assetUse.component.name");
			put("4", "assetUse.asset.source[0].name");
			put("5", "assetUse.asset.mediaType.code");
			put("6", "assetUse.usage.code");
			put("7", "assetUse.asset.ownerType.code");
			put("8", "assetUse.asset.vendorId");
			put("9", "assetUse.asset.creditLine");
			put("10", "assetUse.caption");
			put("11", "assetUse.permissionComment");
			put("12", "assetUse.reuse");
			put("13", "assetUse.isExtPickup");
			put("14", "assetUse.manuscriptPage");
			put("15", "assetUse.asset.originalPublicationTitle");
			put("16", "assetUse.asset.originalArticleTitle");
			put("17", "assetUse.asset.originalPublicationAuthor");
			put("18", "assetUse.asset.originalPublicationIsbn");
			put("19", "assetUse.asset.originalPageNumber");
			put("20", "assetUse.asset.originalFigureNumber");
			put("21", "assetUse.asset.artist");
			put("22", "assetUse.permissionForm");
			put("23", "assetUse.photoSize");
			put("24", "assetUse.cameraCopy");
			put("25", "assetUse.pickupISBN");
			put("26", "assetUse.pickupPosition");
			put("27", "assetUse.pickupPage");
			put("28", "assetUse.pickupComment");
			put("29", "assetUse.asset.filename");
			put("30", "assetUse.productionComment");
			put("31", "assetUse.permissionType");
			put("32", "assetUse.contract.number");
			put("33", "assetUse.contract.date");
			put("34", "assetUse.price");
			put("35", "assetUse.contract.startDate");
			put("36", "assetUse.contract.endDate");
			put("37", "assetUse.unlimitedPrintRunCondition");
			put("38", "assetUse.printRunCondition");
			put("39", "assetUse.languageCondition");
			put("40", "assetUse.salesTerritoryCondition");
		}
	};

	//Start: Changes made for the new spreadsheet upload requirement DM-289
	public static final List<String> columnList = new ArrayList<String>() {
		{
			add("assetUse.batchNumber"); //1. Batch Number
			add("assetUse.strSortOrder"); //2. Asset Order
			add("assetUse.componentNameString"); //3. Component
			add("assetUse.usageDesc"); //4. Usage
			add("assetUse.mediaTypeDesc"); //5. Media Type (Content Type)
			add("assetUse.strPosition"); //6. Position Number
			add("assetUse.assetDescription"); //7. Description of Image
			add("assetUse.assetArtist"); //8. Original Artist/ Photographer
			add("assetUse.getSourceName"); //9. Source (Publication/ Vendor /Rightsholder)
			add("assetUse.assetSourceVendorId"); //10. Source Reference Number
			add("assetUse.assetOriginalPubIsbn"); //11. Source Publication ISSN or ISBN
			add("assetUse.assetOriginalPubTitle"); //12. Source Publication Title
			add("assetUse.assetOriginalArticleTitle"); //13. Source Article title
			add("assetUse.assetOriginalPubAuthor"); //14. Original Publication Author
			add("assetUse.assetOriginalPubPageNo"); //15. Source Publication Page number (may contain hyphen)
			add("assetUse.assetOriginalPubDate"); //16. Original Publication Date (dd.mm.yyyy)---------------------Do something else to avoid excel read error
			add("assetUse.assetCreditLine"); //17. Credit Line
			add("assetUse.assetOwnerTypeCode"); //18. Owner Type (Permission Status)
			add("assetUse.assetModelRelease"); //19. Model Release/ Patient Consent//assetUse.asset.modelRelease
			add("assetUse.willBeRoyaltyFree"); //20. Royalty Free?   (Yes/No)
			add("assetUse.publicDomain"); //21. Public Domain? (Yes/No) -- if Yes, set assetUse.getAsset().setManaged(false); assetUse.getAsset().setRoyaltyFree(true);
			add("assetUse.willBeWorkForHire"); //22. Is this a Work for Hire Contract? (Yes/No)
			add("assetUse.fairUse"); /*23. Is this Fair Use? (Yes/No) - if Yes, set assetUse.getAsset().setManaged(false);
											assetUse.getAsset().setFeeRequired(false); assetUse.getAsset().setRoyaltyFree(true);*/
			add("assetUse.contractFileNamesStr"); //24. Contract Filename(s) //Added for DM-532
			add("assetUse.contractNoStr"); //25. Invoice #
			add("assetUse.date"); //26. Invoice/ Permission Letter Date (dd.mm.yyyy)
			add("assetUse.contractCurrencyDesc"); //27. Currency
			add("assetUse.contractPrice"); //28. Cost of Asset (no symbols)
			add("assetUse.contractStartDate"); //29. Start Date (date of invoice or date permission letter is signed) (dd.mm.yyyy)
			add("assetUse.contractEndDate"); //30. Expiration Date (end date of license if applicable) (dd.mm.yyyy)
			add("assetUse.rightsLinkLicenseNumber"); //31. CCC/ Rightslink License Number -- Note: Not to be populated in GPS
			add("assetUse.unlimitedPrintRunCondition"); //32. Unlimited Print Run? (Yes/No)
			add("assetUse.agreedPrintRun"); //33. Agreed Print Run
			add("assetUse.doesAgreedPrintRunIncludeEBooks"); //34. Does Agreed Print Run Include eBooks? (Yes/No)
			add("assetUse.eBookPrintRun"); //35. eBook Print Run/ Ebook unit limit /sales (number here) -- Note: Not to be populated in GPS
			add("assetUse.isSizeLimited"); //36. Size limitation? (Yes/No)
			add("assetUse.sizeVal"); //37. Size Granted (size of image on page)
			add("assetUse.allLanguagesGranted"); //38. All Languages Granted ? (Yes/No)
			add("assetUse.languageLimitation"); //39. Language Limitation
			add("assetUse.grantedWorldSalesTerritory"); //40. World Sales Territory Granted? (Yes/No)
			add("assetUse.salesTerritoryLimitation"); //41. Sales Territory Limitation
			add("assetUse.doesDerivativeCusRightsGranted"); //42. Ancillary, Derivative, Custom Rights Granted? (Yes/No)
			add("assetUse.customOrDerivative"); //43. Ancilary/ Derivative / Custom Limitations
			add("assetUse.clearedForAllMediaTypes"); //44. Cleared For all Future Media Types? (Yes/No)
			add("assetUse.mediaLimitations"); //45. Media Limitations
			add("assetUse.clearedForAllFutureEditions"); //46. Cleared for all future editions? (Yes/No)
			add("assetUse.editionLimitation"); //47. Edition Limitation
			add("assetUse.doesSublicensingGranted"); //48. Sublicensing Granted? (Yes/No)
			add("assetUse.permissionCommentStr"); //49. Permission Comments field (NOTES)
			add("assetUse.productionCommentStr"); //50. Production Comments field (NOTES)
			add("assetUse.numberOfCompCopies"); //51. Comp Copies? (no. of comp copies)
			add("assetUse.compRecipient"); //52. Comp Copy name
			add("assetUse.address.lineOne"); //53. Comp Copy mailing address
			add("assetUse.address.city"); //54. Comp Copy City
			add("assetUse.address.province"); //55. Comp Copy State/Province
			add("assetUse.address.postalCode"); //56. Comp Copy Postal Code
			add("assetUse.address.country.description"); //57. Comp Copy Country
			add("assetUse.continuedUse"); //58. Continued Use (Reuse /Pickup) -- could be one of assetUse.reusedFromPreviousEdition or assetUse.isPickup
			add("assetUse.continuedISBN"); //59. ISBN (is a Continued Pickup/Reused ISBN, based on assetUse.reusedFromPreviousEdition or assetUse.isPickup)
			add("assetUse.continuedPosition"); //60. Continued Use Position (is a Pickup/Reuse Position, based on assetUse.reusedFromPreviousEdition or assetUse.isPickup)
			add("assetUse.continuedPage"); //61. Continued Use Page number (is a Pickup/Reuse Page, based on assetUse.reusedFromPreviousEdition or assetUse.isPickup)
			add("assetUse.continuedComment"); //62. Continued Use Comment (is a Pickup/Reuse Comment, based on assetUse.reusedFromPreviousEdition or assetUse.isPickup)
			add("assetUse.pplQAEcolumns"); //63. PPL Q-AE columns -- Note: Not to be populated in GPS
		}
	};
	//End: Changes made for the new spreadsheet upload requirement DM-289


	public String[] getMappingColumns(String[] columns) throws ValidateException {
		List<String> values = new ArrayList<String>();
		for (String column : columns) {
			String mapValue = headerMapping.get(column.toLowerCase());
			if (null == mapValue) {
				throw new ValidateException("column with header " + column + " has no mapping");
			}
			values.add(mapValue);
		}
		String[] strarray = new String[values.size()];
		return values.toArray(strarray);
	}
	//Start: Changes made for the new spreadsheet upload requirement DM-289
	public String[] getColumns(String[] columns) throws ValidateException {
		String[] strarray = new String[columnList.size()];
		return columnList.toArray(strarray);
	}
	//End: Changes made for the new spreadsheet upload requirement DM-289

	public void validateColumns(String[] columns) throws ValidateException {
		boolean bDescription = false;
		for (String column : columns) {
			if (column.equalsIgnoreCase("assetUse.asset.description"))
				bDescription = true;
		}
		if (!bDescription)
			throw new ValidateException("Description column is required.");
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void populateAssetUseListFromExcel(int userId, int cwId, String[] columns, byte[] data,
	        ImportAssetsStatus status, boolean isTemplate) throws XLSDataReadException, PersistenceException, Exception {
		List<ExtendedAssetUse> eauList = importAssetsUtility.convertXLSToAssetUseList(columns, data, ASSET_MAPPING_FILE, isTemplate);
		log.debug("populateAssetUseListFromExcel(): total rows found in spreadsheet=" + eauList.size());
		/*List<String> componentList = null;
		List<String> usageTypeList = null;
		List<String> mediaTypeList = null;
		List<String> currencyList = null;
		try {
			componentList = cwRepository.getAllComponentCategoryDescriptions();
			log.debug("Component list : "+componentList);
			usageTypeList = cwRepository.getAllUsageTypeDescriptions();
			log.debug("Usage Type list : "+usageTypeList);
			mediaTypeList = cwRepository.getAllMediaTypeDescriptions();
			log.debug("Media Type list : "+mediaTypeList);
			currencyList = cwRepository.getAllCurrencyDescriptions();
			log.debug("Currency list : "+currencyList);
		} catch (Exception e) {
			log.error("Exception occured in populateAssetUseListFromExcel(): "+e.getMessage());
		}*/

		// for some reason the ${row} in the mapping file does not work (reads the second row also)
		// so we remove it here
		if (isTemplate)
			eauList.remove(0);

		CommonWork cw = cwRepository.loadCWById(cwId);
	//	User user = userRepository.loadUserById(userId);
		User user;
		try {
			// This should always work unless db is down.
			user = userRepository.lazyLoad(User.class, userId, new String[] { "group" });
		} catch (Exception e) {
			user = userRepository.loadUserById(userId);
		}

		// lnagy - always set the allWiley flag to false during an asset import
		commonWorkService.setInteriorCWStatus(cw, CommonWorkStatus.IN_PROGRESS);

		// return only those that pass the validation to be persisted
		List<ExtendedAssetUse> validList = new ArrayList<ExtendedAssetUse>();
		status.setTotalCount(eauList.size());

		StringBuilder errorMessage = new StringBuilder();
		int failures = 0;
		boolean skipFlag = false;

		for (ExtendedAssetUse assetUse : eauList) {
			try{
				skipFlag = validateAssetUseToGenerateErrors(assetUse, status, errorMessage);
				failures = status.getFailureCount();
				assetUse.setCommonWork(cw);
				if(skipFlag) {
					skipFlag = false;
					log.debug("populateAssetUseListFromExcel(): skipped for : "+getAssetIdentificationString(assetUse));
					continue;
				}
				assetUse.mapExcelData();
				if (isTemplate && StringUtils.isEmpty(assetUse.getAsset().getDescription())) {
					continue;
				}

				assetUse.setLastUpdatedUser(user);
				assetUse.getAsset().setLastUpdatedUser(user);
				//if(null != user.getGroup()) {
				//	assetUse.setUserGroup(user.getGroup());
				//}


				/////////////////////////////////////////////////////////////////
				// read the source
				Source src = getSource(assetUse);
				if (null == src) {
					log.debug("populateAssetUseListFromExcel(): source not found");
				}

				/////////////////////////////////////////////////////////////////
				// read the deal
				if (null != src && null != assetUse.getPermissionType() && assetUse.getPermissionType().equals("Royalty Free")) {
					assetUse.setDealId(getDealId (src));
				}

				/////////////////////////////////////////////////////////////////
				// process the conditions
				if (null != assetUse.getContract()) {
					assetUse.getContract().setConditionNodeTree(processConditionsForExcel(assetUse, assetUse.getContract().isPermissionForm()));
					assetUse.getContract().setPurchaseOrder(null);
				}
				assetUse.setStatus(null);
				validList.add(assetUse);
			} catch (Exception e) {
				log.error("populateAssetUseListFromExcel(): Unexpected error occured: ", e.fillInStackTrace());
				failures += 1;;
				errorMessage.append(failures +". Unexpected error for asset \"" + getAssetIdentificationString(assetUse) + "\" <br/>");
				status.setFailureCount(failures);
			}
		}
		status.setErrorMessage(errorMessage.toString());
		//status.setFailureCount(failures);
		log.debug("populateAndValidateAssetUseListForExcel ValidList : "+validList+"\n status :"+status);
		/*importAssetsUtility.populateAndValidateAssetUseList(validList, status, isTemplate);*/
		importAssetsUtility.populateAndValidateAssetUseListForExcel(validList, status, isTemplate);
	}

	//	Start : Added for DM-533
	@Transactional(propagation = Propagation.REQUIRED)
	public void populateMultiCostAssetUseListFromExcel(int userId, int cwId, String[] columns, byte[] data,
	        ImportAssetsStatus status, boolean isTemplate) throws XLSDataReadException, PersistenceException, Exception {
		List<ExtendedAssetUse> eauList = importAssetsUtility.convertXLSToAssetUseList(columns, data, ASSET_MAPPING_FILE, isTemplate);
		log.debug("populateAssetUseListFromExcel(): total rows found in spreadsheet=" + eauList.size());

		// for some reason the ${row} in the mapping file does not work (reads the second row also)
		// so we remove it here
		if (isTemplate)
			eauList.remove(0);

		CommonWork cw = cwRepository.loadCWById(cwId);
		//	User user = userRepository.loadUserById(userId);
		User user;
		try {
			// This should always work unless db is down.
			user = userRepository.lazyLoad(User.class, userId, new String[] { "group" });
		} catch (Exception e) {
			user = userRepository.loadUserById(userId);
		}

		// lnagy - always set the allWiley flag to false during an asset import
		commonWorkService.setInteriorCWStatus(cw, CommonWorkStatus.IN_PROGRESS);

		// return only those that pass the validation to be persisted
		List<ExtendedAssetUse> validList = new ArrayList<ExtendedAssetUse>();
		status.setTotalCount(eauList.size());

		StringBuilder errorMessage = new StringBuilder();
		int failures = 0;
		boolean skipFlag = false;
		commonWorkService.validateCostDuplicationAssetError(eauList,errorMessage,status);

		for (ExtendedAssetUse assetUse : eauList) {
			try{
				skipFlag = validateAssetUseToGenerateErrors(assetUse, status, errorMessage);
				failures = status.getFailureCount();
				assetUse.setCommonWork(cw);
				if(skipFlag) {
					skipFlag = false;
					log.debug("populateAssetUseListFromExcel(): skipped for : "+getAssetIdentificationString(assetUse));
					continue;
				}
				if(status.getSrcSrcRefInvStatusMap().containsKey(new SrcSrcRefInvCombination(assetUse.getGetSourceName(),
						assetUse.getAssetSourceVendorId(),assetUse.getContractNoStr()))){
					skipFlag = true;
				}
				if(skipFlag) {
					skipFlag = false;
					log.debug("populateAssetUseListFromExcel(): skipped for : "+getAssetIdentificationString(assetUse));
					continue;
				}
				assetUse.mapExcelData();
				if (isTemplate && StringUtils.isEmpty(assetUse.getAsset().getDescription())) {
					continue;
				}

				assetUse.setLastUpdatedUser(user);
				assetUse.getAsset().setLastUpdatedUser(user);
				//if(null != user.getGroup()) {
				//	assetUse.setUserGroup(user.getGroup());
				//}

				// read the source
				Source src = getSource(assetUse);
				if (null == src) {
					log.debug("populateAssetUseListFromExcel(): source not found");
				}
				// read the deal
				if (null != src && null != assetUse.getPermissionType() && assetUse.getPermissionType().equals("Royalty Free")) {
					assetUse.setDealId(getDealId (src));
				}
				// process the conditions
				if (null != assetUse.getContract()) {
					assetUse.getContract().setConditionNodeTree(processConditionsForExcel(assetUse, assetUse.getContract().isPermissionForm()));
					assetUse.getContract().setPurchaseOrder(null);
				}
				assetUse.setStatus(null);
				validList.add(assetUse);
			} catch (Exception e) {
				log.error("populateAssetUseListFromExcel(): Unexpected error occured: ", e.fillInStackTrace());
				failures += 1;;
				errorMessage.append(failures +". Unexpected error for asset \"" + getAssetIdentificationString(assetUse) + "\" <br/>");
				status.setFailureCount(failures);
			}
		}
		status.setErrorMessage(errorMessage.toString());
		//status.setFailureCount(failures);
		log.debug("populateAndValidateAssetUseListForExcel ValidList : "+validList+"\n status :"+status);
		/*importAssetsUtility.populateAndValidateAssetUseList(validList, status, isTemplate);*/
		importAssetsUtility.populateAndValidateMultiCostAssetUseListForExcel(validList, status, isTemplate);
	}

	//	End : Added for DM-533

	private boolean validateAssetUseToGenerateErrors (ExtendedAssetUse assetUse, ImportAssetsStatus status, StringBuilder errorMessage) {
		int failures = status.getFailureCount();
		if(!StringUtils.isNotBlank(assetUse.getComponentNameString())) {
			failures += 1;
			errorMessage.append(failures +". \"Component\" cannot have null value for asset \"" + getAssetIdentificationString(assetUse) + "\" <br/>");
			status.setFailureCount(failures);
			return true;
		}
		if(!StringUtils.isNotBlank(assetUse.getStrPosition())) {
			failures += 1;
			errorMessage.append(failures +". \"Positon\" cannot have null value for asset \"" + getAssetIdentificationString(assetUse) + "\" <br/>");
			status.setFailureCount(failures);
			return true;
		}
		if(!StringUtils.isNotBlank(assetUse.getAssetDescription())) {
			failures += 1;
			errorMessage.append(failures +". \"Description\" cannot have null value for asset \"" + getAssetIdentificationString(assetUse) + "\" <br/>");
			status.setFailureCount(failures);
			return true;
		}
		//Start: Added for DM-532
		if(StringUtils.isNotBlank(assetUse.getContractFileNamesStr())) {
			if(StringUtils.isBlank(assetUse.getDate()) || StringUtils.isBlank(assetUse.getContractStartDate())) {
				failures += 1;
				errorMessage.append(failures +". Invoice details must be populated for asset \"" + getAssetIdentificationString(assetUse) + "\" as it "
						+ "has value in the \"Contract Filename\" column.<br/>");
				status.setFailureCount(failures);
				return true;
			} else {
				boolean flag = validateContractFileNamesStr (assetUse.getContractFileNamesStr());
				log.debug("flag value from validateContractFileNamesStr : "+flag);
				if(flag) {
					failures += 1;
					errorMessage.append(failures +". Values in the \"Contract Filename\" column must be valid filenames with file extensions and "
							+ "separated by semicolon for asset \"" + getAssetIdentificationString(assetUse) + "\" <br/>");
					status.setFailureCount(failures);
					return true;
			}
			}
		}
		//new code Added by Santhosh for Nofly Validation
		if(StringUtils.isNotBlank(assetUse.getGetSourceName())) {
			try {
				boolean retvalue = sourceRepository.isNoFly(assetUse.getGetSourceName());
				if(retvalue) {
					failures += 1;
					errorMessage.append(failures +". \"Source\" added is No Fly Source for asset \"" + getAssetIdentificationString(assetUse) + "\" <br/>");
					status.setFailureCount(failures);
					return true;
				}

			} catch (PersistenceException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		if(StringUtils.isNotBlank(assetUse.getAssetArtist())) {
			try {
				boolean retvalue = sourceRepository.isNoFlyArtist(assetUse.getAssetArtist());
				if(retvalue) {
					failures += 1;
					errorMessage.append(failures +". \"Photographer\" added is in No Fly List for asset \"" + getAssetIdentificationString(assetUse) + "\" <br/>");
					status.setFailureCount(failures);
					return true;
				}
			} catch (PersistenceException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		//end new code Added by Santhosh for Nofly Validation
		//End: Added for DM-532
		/*if(!StringUtils.isNotBlank(assetUse.getAssetCreditLine())) {//This is commented as Credit line in not mandatory for spreadsheet upload
			failures += 1;
			errorMessage.append(failures +". \"Credit Line\" cannot have null value for asset \"" + getAssetIdentificationString(assetUse) + "\" <br/>");
			status.setFailureCount(failures);
			return true;
		}*/
		return false;
	}

	private String getAssetIdentificationString(ExtendedAssetUse assetUse) {
		String assetIdStr = "";
		if(StringUtils.isNotBlank(assetUse.getStrPosition())) {
			assetIdStr = assetUse.getStrPosition();
		} else if(StringUtils.isNotBlank(assetUse.getStrSortOrder())) {
			assetIdStr = assetUse.getStrSortOrder();
		} else if(StringUtils.isNotBlank(assetUse.getUsageDesc())) {
			assetIdStr = assetUse.getUsageDesc();
		}
		return assetIdStr;
	}

	private boolean validateContractFileNamesStr (String str) {

		String contractFileName = str.trim().replaceAll("\n", "").replaceAll(" ", ""); // replace both \n and space with
																						// empty character
		String[] fileNames = contractFileName.trim().split(";");

		// New logic Added by Chethan for DM-532
		for (String fileName : fileNames) {
			fileName = fileName.trim();

			//String pattern = "^([a-zA-Z0-9_]+(\\.[pdf,doc,docx,png,gif,ppt,xls,xlsx,txt,rtf,tif,msg,jpg,jpeg]{3,4}))$";

			String pattern = "^([a-zA-Z0-9_]+(\\.[a-zA-Z]{3,4}))$";
			Pattern p = Pattern.compile(pattern);
			Matcher m = p.matcher(fileName.trim()); // get a matcher object
			if (!m.find()) {
				return true;
			}

			//System.out.println("fileNameParts.length " + fileNameParts.length + "fileNameParts .." + fileNameParts[0]);
				if ( !fileName.endsWith(".txt") && !fileName.endsWith(".pdf") && !fileName.endsWith(".doc")  && !fileName.endsWith(".docx")  && !fileName.endsWith(".png")  && !fileName.endsWith(".gif")
						 && !fileName.endsWith(".ppt")  && !fileName.endsWith(".xls")  && !fileName.endsWith(".xlsx")  && !fileName.endsWith(".rtf") && !fileName.endsWith(".tif")
						 && !fileName.endsWith(".msg") && !fileName.endsWith(".jpg") && !fileName.endsWith(".jpeg")) {
					return true;
				}
		}
		return false;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void persistAssetUseListFromExcel(List<ExtendedAssetUse> auList, ImportAssetsStatus status, boolean b) throws PersistenceException {
		importAssetsUtility.persistAssetUseList (auList, status, true);
    }

	@Override
	public ImportAssetsUtility getImportAssetsUtility() {
    	return importAssetsUtility;
    }

	@Override
	public void setImportAssetsUtility(ImportAssetsUtility importAssetsUtility) {
    	this.importAssetsUtility = importAssetsUtility;
    }

	public CommonWorkService getCommonWorkService() {
    	return commonWorkService;
    }

	public void setCommonWorkService(CommonWorkService commonWorkService) {
    	this.commonWorkService = commonWorkService;
    }

	public CommonWorkRepository getCwRepository() {
    	return cwRepository;
    }

	public void setCwRepository(CommonWorkRepository cwRepository) {
    	this.cwRepository = cwRepository;
    }

	public UserRepository getUserRepository() {
    	return userRepository;
    }

	public void setUserRepository(UserRepository userRepository) {
    	this.userRepository = userRepository;
    }

	//Added by santhosh for DM-532
	public SourceRepository getSourceRepository() {
		return sourceRepository;
	}
	public void setSourceRepository(SourceRepository sourceRepository) {
		this.sourceRepository = sourceRepository;
	}
	////End:Added by santhosh for DM-532
}
