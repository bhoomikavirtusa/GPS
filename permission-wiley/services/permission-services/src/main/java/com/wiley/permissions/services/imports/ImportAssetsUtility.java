package com.wiley.permissions.services.imports;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import net.sf.jxls.reader.XLSDataReadException;
import net.sf.jxls.reader.XLSReadMessage;
import net.sf.jxls.reader.XLSReadStatus;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.xml.sax.SAXException;

import com.wiley.permissions.common.excel.ExcelTransformerUtility;
import com.wiley.permissions.common.utils.UniqueConstraintViolationException;
import com.wiley.permissions.common.utils.ValidateException;
import com.wiley.permissions.domain.persistence.permissions.Asset;
import com.wiley.permissions.domain.persistence.permissions.AssetUse;
import com.wiley.permissions.domain.persistence.permissions.CommonWorkStatus;
import com.wiley.permissions.domain.persistence.permissions.Component;
import com.wiley.permissions.domain.persistence.permissions.ComponentCategory;
import com.wiley.permissions.domain.persistence.permissions.ConditionNode;
import com.wiley.permissions.domain.persistence.permissions.Contract;
import com.wiley.permissions.domain.persistence.permissions.Country;
import com.wiley.permissions.domain.persistence.permissions.ExtendedAssetUse;
import com.wiley.permissions.domain.persistence.permissions.ImportSource;
import com.wiley.permissions.domain.persistence.permissions.MediaType;
import com.wiley.permissions.domain.persistence.permissions.OwnerType;
import com.wiley.permissions.domain.persistence.permissions.PermissionType;
import com.wiley.permissions.domain.persistence.permissions.Product;
import com.wiley.permissions.domain.persistence.permissions.Source;
import com.wiley.permissions.domain.persistence.permissions.Usage;
import com.wiley.permissions.persistence.PersistenceException;
import com.wiley.permissions.persistence.repository.JPARepository;
import com.wiley.permissions.repositories.AssetRepository;
import com.wiley.permissions.repositories.CommonWorkRepository;
import com.wiley.permissions.repositories.ConditionRepository;
import com.wiley.permissions.repositories.ContractRepository;
import com.wiley.permissions.repositories.ProductRepository;
import com.wiley.permissions.repositories.SourceRepository;
import com.wiley.permissions.repositories.UserRepository;
import com.wiley.permissions.services.AssetUseService;
import com.wiley.permissions.services.CommonWorkService;
import com.wiley.permissions.services.ContractService;
import com.wiley.permissions.services.util.ServiceException;
import com.wiley.sf.common.lang.ArgUtil;

public class ImportAssetsUtility {

	private static final Log log = LogFactory.getLog(ImportAssetsUtility.class);

	public static final String IGNORE = "ignore";

	// NOT USED ANYMORE - the mapping is done in FilemakerDataImportUtility.class
	// private static final String FILEMAKER_ASSET_MAPPING_FILE = "/conf/mapping/filemakerAssetMapping.xml";

	private ExcelTransformerUtility excelUtility;

	private AssetUseService assetUseService;
	private ContractService contractService;
	private CommonWorkService commonWorkService;

	private ImportProductsUtility importProductsUtility;

	private SourceRepository sourceRepository;
	private ProductRepository productRepository;
	private CommonWorkRepository cwRepository;
	private AssetRepository assetRepository;
	private UserRepository userRepository;
	private ConditionRepository conditionRepository;
	private ContractRepository contractRepository;

	/**
	 * Imports one asset from the FileMaker database.
	 */
	protected ImportAssetsStatus populateAssetUse(ExtendedAssetUse assetUse) {
		log.debug("populateAssetUse()...begin");

		ImportAssetsStatus status = new ImportAssetsStatus();
		// populate Product before passing to importAssetUses
		// if product not found we log the error and extract it from the list to
		// be persisted
		assetUse.mapData();

		try {
			// if asset is null, just log and continue
			if (null != assetUse.getAsset()) {
				log.debug("populateAssetUse(): Processing : " + assetUse.getAsset().getDescription());
			} else {
				throw new ValidateException("no asset so something is wrong and we cannot import");
			}

			// if product is not empty, continue (when the method is called from the UI, we will have a primary product)
			if (null != assetUse.getCommonWork().getPrimaryProduct()) {
				// do nothing
			} else {
				// just log and continue
			    if (StringUtils.isBlank(assetUse.getCommonWork().getPrimaryProduct().getIsbn10())) {
			    	throw new ValidateException ("product identifier is empty.");
			    }
			}

			String identifier = getImportProductsUtility().setProductIdentifiers(
			        assetUse.getCommonWork().getPrimaryProduct());

			Product product = productRepository.getProductByISBN(identifier.toUpperCase());

			// if product not found by ISBN, just log and continue
			if (product == null) {
				throw new ValidateException ("product not found by ISBN[" + identifier + "]");
			} else {
				// sets the commonWork of the product it finds
				// lazy load the common work
				product = productRepository.lazyLoad (Product.class, product.getId(), new String[] {"commonWork"});
				assetUse.setCommonWork(product.getCommonWork());
			}
		} catch (Exception e) {
			status.setStatusOK(false);
			status.setErrorMessage("populateAssetUse(): " + e.getMessage());
			return status;
		}

		log.debug("populateAssetUse(): before populate and validate ");

		List<ExtendedAssetUse> auList = new ArrayList<ExtendedAssetUse>();
		auList.add(assetUse);

		populateAndValidateAssetUseList(auList, status, false);

		log.debug("populateAssetUse(): after populate and validate");
		return status;
	}

	/**
	 * Processes a list of AssetUse.
	 */
	protected void populateAndValidateAssetUseList(List<ExtendedAssetUse> eauList,
	        ImportAssetsStatus status, boolean isTemplate) {
		log.debug("importAssetUses(): assetUse list size = " + eauList.size());
		StringBuilder errorMessage = new StringBuilder();

		status.setTotalCount(eauList.size());
		int failures = status.getFailureCount();
		boolean usage = false;
		int currentRow = isTemplate ? 4 : 2; // skip the header
		if(null != status.getErrorMessage()) {
			errorMessage.append(status.getErrorMessage());
		}

		for (ExtendedAssetUse au : eauList) {
			try {

				// lnagy - not pleased to move this here, but because it is not
				// actually an exception,
				// we just need to add a message, I'll do it here
				if (null == au.getUsage()) {
					usage = true;
				}
				populateAndValidateAssetUse(au);
				status.getGoodRecords().add(au);
				status.setProcessedCount(status.getProcessedCount() + 1);
			} catch (Exception e) {
				log.debug(
				        "importAssetUses(): Row (" + (currentRow) + ") failed processing assetUse  ["
				                + e.getMessage() + "]",
				        e);
				failures++;
				errorMessage.append(failures+". " + e.getMessage() + " for asset \"" + au.getPosition() + "\" <br/>");
				/*errorMessage.append(failures+". Row (" + (currentRow) + ") : " + e.getMessage() + "<br/>");*/
			}
			currentRow++;
		}
		if (usage) {
			errorMessage
			        .append("Some rows have no usage value, so it has been set to Figure and may be incorrect"
			                + "<br/>");
		}
		status.setFailureCount(failures);

		// if we have failures, we set status on FALSE and add the ErrorMessage
		if (failures > 0) {
			status.setStatusOK(false);
			status.setErrorMessage(errorMessage.toString());
			log.debug(errorMessage.toString());
		}
	}

	/**
	 * Processes a list of AssetUse.
	 */
	protected void populateAndValidateAssetUseListForExcel(List<ExtendedAssetUse> eauList,
	        ImportAssetsStatus status, boolean isTemplate) {
		log.debug("importAssetUses(): assetUse list size = " + eauList.size());
		StringBuilder errorMessage = new StringBuilder();

		//status.setTotalCount(eauList.size());//this is set in calling method, so commented here
		int failures = status.getFailureCount();
		boolean usage = false;
		int currentRow = isTemplate ? 4 : 2; // skip the header
		if(null != status.getErrorMessage()) {
			errorMessage.append(status.getErrorMessage());
		}

		for (ExtendedAssetUse au : eauList) {
			try {

				// lnagy - not pleased to move this here, but because it is not
				// actually an exception,
				// we just need to add a message, I'll do it here
				/*if (null == au.getUsage()) {
					usage = true;
				}*/
				populateAndValidateAssetUseForExcel(au);
				status.getGoodRecords().add(au);
				status.setProcessedCount(status.getProcessedCount() + 1);
			} catch (Exception e) {
				log.debug(
				        "importAssetUses(): Row (" + (currentRow) + ") failed processing assetUse  ["
				                + e.getMessage() + "]",
				        e);
				failures++;
				errorMessage.append(failures+". " + e.getMessage() + " for asset \"" + au.getPosition() + "\" <br/>");
				/*errorMessage.append(failures+". Row (" + (currentRow) + ") : " + e.getMessage() + "<br/>");*/
			}
			currentRow++;
		}
		/*if (usage) {
			errorMessage
			        .append("Some rows have no usage value, so it has been set to Figure and may be incorrect"
			                + "<br/>");
		}*/
		status.setFailureCount(failures);

		// if we have failures, we set status on FALSE and add the ErrorMessage
		if (failures > 0) {
			status.setStatusOK(false);
			status.setErrorMessage(errorMessage.toString());
			log.debug(errorMessage.toString());
		}
	}

	//	Start : Added for DM-533
	protected void populateAndValidateMultiCostAssetUseListForExcel(List<ExtendedAssetUse> eauList,
	        ImportAssetsStatus status, boolean isTemplate) {
		log.debug("importAssetUses(): assetUse list size = " + eauList.size());
		StringBuilder errorMessage = new StringBuilder();

		//status.setTotalCount(eauList.size());//this is set in calling method, so commented here
		int failures = status.getFailureCount();
		boolean usage = false;
		int currentRow = isTemplate ? 4 : 2; // skip the header
		if(null != status.getErrorMessage()) {
			errorMessage.append(status.getErrorMessage());
		}
		commonWorkService.validateCostDuplicationAssetWarning(eauList,errorMessage,status);	// Added for DM-533
		for (ExtendedAssetUse au : eauList) {
			try {

				// lnagy - not pleased to move this here, but because it is not
				// actually an exception,
				// we just need to add a message, I'll do it here
				/*if (null == au.getUsage()) {
					usage = true;
				}*/
				populateAndValidateAssetUseForExcel(au);
				status.getGoodRecords().add(au);
				status.setProcessedCount(status.getProcessedCount() + 1);
			} catch (Exception e) {
				log.debug(
				        "importAssetUses(): Row (" + (currentRow) + ") failed processing assetUse  ["
				                + e.getMessage() + "]",
				        e);
				failures++;
				errorMessage.append(failures+". " + e.getMessage() + " for asset \"" + au.getPosition() + "\" <br/>");
				/*errorMessage.append(failures+". Row (" + (currentRow) + ") : " + e.getMessage() + "<br/>");*/
			}
			currentRow++;
		}
		/*if (usage) {
			errorMessage
			        .append("Some rows have no usage value, so it has been set to Figure and may be incorrect"
			                + "<br/>");
		}*/
		status.setFailureCount(failures);

		// if we have failures, we set status on FALSE and add the ErrorMessage
		if (failures > 0) {
			status.setStatusOK(false);
			status.setErrorMessage(errorMessage.toString());
			log.debug(errorMessage.toString());
		}
	}
	//	End : Added for DM-533
	/**
	 * Reads the spreadsheet and converts it into a List of ExtendedAssetUse.
	 *
	 * @param isTemplate
	 */
	protected List<ExtendedAssetUse> convertXLSToAssetUseList(String[] columns, byte[] data,
	        String mappingFile, boolean isTemplate) throws XLSDataReadException {
		StringBuilder errorMessage = new StringBuilder();
		List<ExtendedAssetUse> eauList = new ArrayList<ExtendedAssetUse>();

		InputStream in = getClass().getResourceAsStream(mappingFile);
		String startRow = (isTemplate) ? "4" : "2";
		try {
			String mapping = IOUtils.toString(in);
			int index = 0;
			if (null != columns) {
				String columnDefinitions = StringUtils.EMPTY;
				for (String column : columns) {
					//log.debug("convertXLSToAssetUseList(): Column [ " + column + "]");
					/*if (column.equals(IGNORE)) {
						index++;
					} else {
						columnDefinitions += "<mapping row=\"" + startRow + "\" col=\"" + (index++) + "\">" + column + "</mapping>";
					}*/
					columnDefinitions += "<mapping row=\"" + startRow + "\" col=\"" + (index++) + "\">" + column + "</mapping>";
				}
				mapping = mapping.replaceAll("\\<definitions\\/\\>", columnDefinitions);
				mapping = mapping.replaceAll("\\$\\{row\\}", startRow);
			}
			log.debug("convertXLSToAssetUseList(): mapping file [" + mapping + "]");

			eauList = convertXLSToAssetUseList(data, mapping.getBytes());
		} catch (Exception e) {
			throw new XLSDataReadException("Failed to read the spreadsheet file [" + e.getMessage() + "]");
		}

		// decided to throw the errors from reading the spreadsheet here
		// not wait until the assets are processed
		String errorMsgString = errorMessage.toString();
		if (StringUtils.isNotBlank(errorMsgString)) {
			log.debug("convertXLSToAssetUseList(): failed to read the spreadsheet file" + errorMsgString);
			throw new XLSDataReadException(errorMsgString);
		}
		return eauList;
	}

	/**
	 * Reads the spreadsheet and converts it into ProductList
	 *
	 * @param data
	 * @return List<Product>
	 * @throws IOException
	 * @throws SAXException
	 * @throws XLSDataReadException
	 */
	public List<ExtendedAssetUse> convertXLSToAssetUseList(byte[] data, byte[] mapping)
	        throws XLSDataReadException {
		log.debug("convertXLSToAssetUseList()...begin");

		List<ExtendedAssetUse> eauList = new ArrayList<ExtendedAssetUse>();
		Map<String, Object> input = new HashMap<String, Object>();
		StringBuilder errorMessage = new StringBuilder();

		try {
			input.put("inputStream", data);
			input.put("inputMapping", mapping);
			// empty list that will be populated
			input.put("assets", eauList);
			log.debug("about to transformExcelToBeans");
			XLSReadStatus status = getExcelUtility().transformExcelToBeans(input);

			for (XLSReadMessage message : (List<XLSReadMessage>) status.getReadMessages()) {
				errorMessage.append(message.getMessage());
				errorMessage.append("<br/>");
			}
		} catch (Exception e) {
			throw new XLSDataReadException("Failed to read the spreadsheet file [" + e.getMessage() + "]");
		}

		// decided to throw the errors from reading the spreadsheet here
		// not wait until the assets are processed
		String errorMsg = errorMessage.toString();
		if (StringUtils.isNotBlank(errorMsg)) {
			log.debug("convertXLSToAssetUseList(): failed to read the spreadsheet file"
			        + errorMsg);
			// throw new XLSDataReadException(errorMsg);
		}
		return eauList;
	}

	/**
	 * Persists the list of asset uses.
	 *
	 * @param eauList
	 * @param status
	 * @throws PersistenceException
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public void persistAssetUseList(List<ExtendedAssetUse> eauList, ImportAssetsStatus status, boolean calculateStatus)
	        throws PersistenceException {
		if (null == eauList)
			return;

		// reset processed
		status.setProcessedCount(0);

		StringBuilder errorMessage = new StringBuilder();

		for (ExtendedAssetUse assetUse : eauList) {
			try {
				AssetUse au = persistAssetUse(assetUse, calculateStatus);

				Source source = au.getAsset().getSource(0);

				Contract contract = assetUse.getContract();

				log.debug("contract:" + contract);

			//	contract = contractRepository.lazyLoad(Contract.class, contract.getId(),  new String[] {"assets"});
			//	log.debug(contract.getAssets().size());

				if (null != source && null != contract) {
					contract.setCommonWork(au.getCommonWork());
					contract = createContract(contract, source, au.getAsset(), calculateStatus);
					// set the new saved contract in ExtendedAssetUse
					assetUse.setContract(contract);
					// if price or deal, we update the contract information
					if (null != assetUse.getPrice() || null != assetUse.getDealId()) {
						contractRepository.updateContract2Asset(contract.getId(), au.getAsset().getId(), assetUse.getPrice(), assetUse.getDealId());
					}
					//Start: Added for DM-532
					if (StringUtils.isNotBlank(assetUse.getContractFileNamesStr())) {
						contractRepository.updateContractFileName(contract, assetUse.getContractFileList());
					}
					//End: Added for DM-532
				}
				// save the asset use id in the list, so we can get it from controller
				assetUse.setId(au.getId());
				status.setProcessedCount(status.getProcessedCount() + 1);
			} catch (Exception e) {
				log.debug("persistAssetUseList(): failed persisting assetUse: ", e);
				errorMessage.append(e.getMessage() + "<br/>");
			}
		}

		String errorMsgString = errorMessage.toString();
		if (StringUtils.isNotBlank(errorMsgString)) {
			throw new PersistenceException(errorMsgString);
		}
	}

	/**
	 * Persist an assetUse entry from the spreadsheet.
	 *
	 * @param assetUse
	 * @param product
	 * @param user
	 * @param force
	 * @throws Exception
	 */
	public AssetUse persistAssetUse(ExtendedAssetUse assetUse, boolean calculateStatus) throws Exception {
		log.debug("persistAssetUse(): assetUse = " + assetUse.toString());

		// After this method call the source will be committed since
		// createSource() calls saveSource() which is REQUIRES_NEW
		Source source = createSource(assetUse.getAsset().getSource(0));

		// After this method call the component will be committed since
		// createComponent() calls addComponentToCommonWork() which is REQUIRES_NEW
		// assetUse.getComponent() may be null which is ok
		Component component = createComponent(assetUse.getComponent());

		assetUse.setComponent(component);

		AssetUse au = assetUseService.saveDetachedAssetUse(assetUse, source, calculateStatus); // REQUIRES_NEW


		log.debug("persistAssetUse(): assetUse.externalId: " + au.getExternalId());

		return au;
	}

	/**
	 * Creates (persists) the contract. Even if looks like a new contract, we still try to load it again
	 * because
	 *
	 * @param contract
	 * @param source
	 * @return
	 * @throws Exception
	 */
	private Contract createContract(Contract contract, Source source, Asset asset, boolean updateStatus) throws Exception {
		Contract oldContract = null;
		if (!StringUtils.isEmpty(contract.getNumber())) {
			oldContract = contractRepository.loadContractByNumber(contract.getNumber(), source.getId(), contract.getCommonWork().getId());
		}

		// added by Napoleon
		if(null == oldContract && null != contract.getId()) {
			oldContract  = contractRepository.lazyLoad(Contract.class, contract.getId(),  new String[] {"assets"});
		}

		if (null == oldContract) {
			contract.setSource(source);

			List<ConditionNode> conditions = contract.getConditionNodeTree();
			contract.setConditions(null);
			contract.addAssetId(asset.getId());
			return contractService.save(contract, conditions, null, null, updateStatus);
		} else {
			List<ConditionNode> conditions = contract.getConditionNodeTree();
			/*
			 * List<ContractAsset> clist = oldContract.getAssets();
			 * List<Integer> aids = new ArrayList<Integer>();
			 * for(ContractAsset ca : clist) {
			 * 		aids.add(ca.getAssetBaseId());
			 * }
			 * aids.add(asset.getId());
			 */
			oldContract.addAssetId(asset.getId());
			return contractService.save(oldContract, conditions, null, null, updateStatus);
		}
	}

	/**
	 * Creates (persists) the new source. Even if looks like a new source, we still try to load it again
	 * because it is possible the source was created during the import of a previous row
	 *
	 * @param source
	 * @return Source
	 * @throws ValidateException
	 * @throws PersistenceException
	 */
	private Source createSource(Source source) throws ValidateException, PersistenceException {
		if (null == source) {
			return null;
		}

		// source already loaded in validate
		if (null != source.getId()) {
			return source;
		}

		// it already passed the validation, but we have to try to load it again
		// here, because some of the sources can be created on persisting other
		// assets from the same spreadsheet
		Source existingSource = sourceRepository.loadSourceByTemplate(source);

		// if source already exists
		if (null != existingSource) {
			return existingSource;
		} else {
			// persists the new source
			// if source is null, probably source did already exist and we
			// do not have to create the new one
			if (null != source) {
				try {
					source = sourceRepository.saveSource(source);
				} catch (UniqueConstraintViolationException e) {
					throw new ValidateException("Duplicate source " + source.getName());
				} catch (Exception e) {
					log.debug("Failed to create source " + source.getName(), e);
					throw new ValidateException("Failed to create source " + source.getName());
				}

				log.debug("Source " + source);
				log.debug("source.getExternalId() " + source.getExternalId());

				if (null == source) {
					throw new ValidateException("Failed to create source - not found source in DB ");
				}

				return source;
			} else {
				log.debug("source is null, but it shouldn't because it is a new source");
				throw new ValidateException("Failed to create source source is null " + source.getName());
			}
		}
	}

	/**
	 * Creates (persists) the new component. Even if looks like a new component, we still try to load it again
	 * because it is possible the component was created during the import of a previous row
	 *
	 * @param Component
	 *            c
	 * @throws ValidateException
	 * @throws PersistenceException
	 */
	private Component createComponent(Component c) throws ValidateException {
		if (null == c) {
			return null;
		}

		// component already loaded in validate
		if (null != c.getId()) {
			return c;
		}

		// if it is a new component and it is chapter or cover
		/*if (ComponentCategory.CHAPTER.equals(c.getCategory()) || ComponentCategory.COVER.equals(c.getCategory())) {*/
			try {
				// maybe the component was created during the import...that is why
				// we try to load it again
				Component existingComponent = cwRepository.loadComponentByName(
				        c.getCommonWork().getId(),
				        c.getName());

				if (null != existingComponent)
					return existingComponent;

				c = commonWorkService.addComponentToCommonWork(c, false);

				return c;
			} catch (Exception e) {
				log.debug("Failed to create component [" + c.getName() + "]", e);
				throw new ValidateException("Failed to create component [" + c.getName() + "]");
			}
		/*} else {
			log.debug("new component but it is not a chapter ... don't know what to do with it, "
			        + "we just ignore for now");
			return null;
		}*/
	}

	/**
	 * Populates the AssetUse with the missing info.
	 */
	private AssetUse populateAndValidateAssetUseForExcel(ExtendedAssetUse assetUse) throws PersistenceException,
	        ServiceException, ValidateException {
		log.debug("entering populateAndValidateAssetUseForExcel");
		ArgUtil.notNull(assetUse, "assetUse");
		ArgUtil.notNull(assetUse.getAsset(), "asset");

		// try to find the asset by description or externalId, or vendorId if available
		log.debug("asset use:" + assetUse);
		log.debug("asset " + assetUse.getAsset());
		log.debug("component " + assetUse.getComponent());

		populateAssetForExcel(assetUse);

		// populates the source information
		populateSourceForExcel(assetUse.getAsset());

		// some validations and default values will go here
		// if position is not empty, and usage is empty, set usage to FIGURE
		/*if (null == assetUse.getUsage() || StringUtils.isBlank(assetUse.getUsage().getCode())) {
			assetUse.setUsage(Usage.FIGURE);
		}*/

		// Convert obsolete value "Numbered Figure" to "Figure" - we are back to using "Unnumbered Figure" now
		/*if (assetUse.getUsage() != null && "Numbered Figure".equalsIgnoreCase(assetUse.getUsage().getCode())) {
			assetUse.setUsage(Usage.FIGURE);
		}*/
		//if (assetUse.getUsage() != null && "Unnumbered Figure".equalsIgnoreCase(assetUse.getUsage().getCode())) {
		//	assetUse.setUsage(Usage.FIGURE);
		//}
		// Convert obsolete value "Cover" to "Front Cover"
		/*if (assetUse.getUsage() != null && "Cover".equalsIgnoreCase(assetUse.getUsage().getCode())) {
			assetUse.setUsage(Usage.FRONT_COVER);
		}*/

		// if no OwnerType, we default to 3rd party
		if (null == assetUse.getAsset().getOwnerType()
		        || StringUtils.isBlank(assetUse.getAsset().getOwnerType().getCode())) {
			// add logic for product flag isAllWileyOwned.
			// If the flag is TRUE, all OwnerTypes are WILEY
			if (assetUse.getCommonWork().getInteriorCWStatus().equals(CommonWorkStatus.NO_PERMISSIONS_REQUIRED)) {
				assetUse.getAsset().setOwnerType(OwnerType.WILEY);
			} else {
				assetUse.getAsset().setOwnerType(OwnerType.THIRD_PARTY);
			}
		}

		// default 3rd party to Managed
		if (assetUse.getAsset().getOwnerType().equals(OwnerType.THIRD_PARTY)) {
			assetUse.getAsset().setManaged(true);
		}

		// If !managed then set royaltyFree to true
		// TODO: In the future, when/if we allow the user to have a royaltyFree
		// column in the spreadsheet, give an error if they have asked for
		// royaltyFree == false (and managed is true).
		if (!assetUse.getAsset().isManaged()) {
			assetUse.getAsset().setRoyaltyFree(true);
		}

		// if no MediaType, we default to Photo
		if (null == assetUse.getAsset().getMediaType()
		        || StringUtils.isBlank(assetUse.getAsset().getMediaType().getCode())) {
			assetUse.getAsset().setMediaType(MediaType.PHOTO);
		}

		// if it is an old asset, we check the ignore table and update just the asset and exit
		if (null != assetUse.getAsset().getId() && assetRepository.isIgnoredAsset(assetUse.getAsset().getId())) {
			try {
				assetRepository.saveRequiresNew(assetUse.getAsset());
			} catch (PersistenceException e) {
				throw new ServiceException("Failed to persist the asset metadata [" + assetUse.getAsset().getId() + "]", e);
			}
			throw new ServiceException("Found an asset that needs to be ignored - still persist metadata [" + assetUse.getAsset().getId() + "]");
		}

		// handles component - tries to load by name, or sets up for a new one
		log.debug("about to call pupulateComponent");
		/*populateComponentForExcel(assetUse);*/

		// run the rest of the validations
		log.debug("about to call validate()");
		validate(assetUse);

		return assetUse;
	}

	private void populateComponentForExcel(AssetUse assetUse) throws PersistenceException {
		ArgUtil.notNull(assetUse, "assetUse");

		Component comp = assetUse.getComponent();

		if(null == comp){
			log.debug("comp is null");
			assetUse.setComponent(null);
			return;
		}

		if (null == comp || StringUtils.isBlank(comp.getName())) {
			assetUse.setComponent(null);
			return;
		}

		Component c = new Component();
		StringBuilder sb = new StringBuilder();
		char ch;
		String componentNumberOnly = "";
		String componentName = comp.getName();

		log.debug("populateComponent(): findComponent[1]1 " + componentName);
		if (NumberUtils.isDigits(componentName)) {
			c.setSortOrder(100 + new Integer(componentName));
			// if one digit component, we add extra 0 in front
			if (componentName.length() == 1) {
				componentName = "0" + componentName;
			}
			componentName = "Chapter " + componentName;
			c.setCategory(ComponentCategory.CHAPTER);
		} else {
			if (null == comp.getCategory())
				c.setCategory(ComponentCategory.getComponentCategory(componentName));
			else
				c.setCategory(comp.getCategory());

			for (int i=0; i < componentName.length(); i++) {
				ch = componentName.charAt(i);

				if (Character.isDigit(ch)) {
					sb.append(ch);
				}
			}
			componentNumberOnly = sb.toString();
			if (componentNumberOnly.length() == 1) {
				componentName = "Chapter 0" + componentNumberOnly;
			}
			else {
				if(componentName.contains("Chapter")) {
				componentName = "Chapter " + componentNumberOnly;
				}
			}
		}
		log.debug("populateComponent(): findComponent[2]2 " + componentName);
		// now we try to load the component
		c.setCommonWork(assetUse.getCommonWork());
		c.setName(componentName);

		Component existingComponent = cwRepository.loadComponentByName(c.getCommonWork().getId(), c.getName());
		if (existingComponent == null) {
			// lnagy - if we do not find the component, we create it
			// throw new ValidateException("Invalid component name [ " + component + "]. Please first create the component.");
			log.debug("populateComponent(): Invalid component name [" + componentName + "]. We try to create it in persist.");
		} else {
			c = existingComponent;
		}
		assetUse.setComponent(c);
	}

	private void populateSourceForExcel(Asset asset) throws ValidateException, PersistenceException {
		ArgUtil.notNull(asset, "asset");

		if (CollectionUtils.isEmpty(asset.getSources())) {
			return;
		}

		Source source = asset.getSource(0);

		// clear all sources in case none will be found or created
		asset.getSources().clear();

		// source name is blank
		if (StringUtils.isBlank(source.getName())) {
			return;
		}

		// check to see if it is OwnerType - if it is, we set the owner type,
		// otherwise we consider it a source name
		/*OwnerType ownerType = OwnerType.getOwnerType(source.getName());
		if (null != ownerType) {
			asset.setOwnerType(ownerType);
			return;
		}*/

		// if length is greater then DB size, the findSourceByTemplate
		// method fails with an ugly error
		if (source.getName().length() > Source.MAX_NAME_LENGTH) {
			throw new ValidateException("Invalid source name : length > " + Source.MAX_NAME_LENGTH
			        + " chars [" + source.getName() + "]");
		} else {
			// new a new method that load by first looking at transfer history by name or by external_id
			// this will replace loadSourceByTemplate here
			Source oldSource = sourceRepository.loadSourceByTemplate(source);

			log.debug("populate(): findSourceByTemplate " + oldSource);
			// source not found in DB - we create one
			if (null == oldSource) {
				// lnagy - not throwing the exception anymore, if source not found
				// throw new ValidateException("Invalid source " + source.getName());

				// if the required fields to create a source are not present, we throw exception
				// lnagy - change so we not throw exception if required fields are missing, but default them
				/*
				 * if (null == source.getCountry() || StringUtils.isBlank(source.getCountry().getCode()) ||
				 * StringUtils.isBlank(source.getPermissionType())) { throw new ValidateException("Invalid
				 * source - missing required fields to create a new one - " + source.getName()); }
				 */
				// if the source is null, it means no extra required fields
				// are coming from spreadsheet

				// change so we not throw exception if required fields are
				// missing, but default them
				if (null == source.getCountry()
				        || StringUtils.isBlank(source.getCountry().getCode())) {
					source.setCountry(new Country("US", "USA"));
				}

				if (StringUtils.isBlank(source.getPermissionType())) {
					source.setPermissionType(PermissionType.ONE_TIME_USE.getCode());
				}
				// display name cannot be null
				source.setDisplayName(source.getName());

				oldSource = source;
				// persist the source together with the asset
			}
			// add the existing source
			asset.getSources().add(oldSource);
		}
	}

	private void populateAssetForExcel(AssetUse assetUse) throws ServiceException {
		ArgUtil.notNull(assetUse, "assetUse");
		ArgUtil.notNull(assetUse.getAsset(), "assetUse.asset");
		ArgUtil.notNull(assetUse.getCommonWork(), "assetUse.commonWork");

		Asset asset = assetUse.getAsset();
		log.debug("asset before:" + assetUse);

		// description is a non blank field, so we validate it here
		/*if (StringUtils.isBlank(asset.getDescription())) {//We are validating it before populating excel data
			if (StringUtils.isBlank(assetUse.getPosition())) {
				throw new ServiceException("Asset description and position are empty.");
			} else {
				// set the description to be position
				asset.setDescription(assetUse.getPosition());
			}
		}*/

		Asset oldAsset = null;
		log.debug("populateAsset(): before findAssetBySource: asset = " + asset);

		// we try to find the asset by source and vendor id if that is not empty
	/*	if (CollectionUtils.isNotEmpty(asset.getSources()) &&
			StringUtils.isNotBlank(assetUse.getAsset().getSource(0).getName()) &&
			StringUtils.isNotBlank(assetUse.getAsset().getVendorId())) {
			oldAsset = assetRepository.loadAssetBySourceVendorId (
					assetUse.getAsset().getSource(0).getName(),
					assetUse.getAsset().getVendorId());
			if (null != oldAsset) {
				log.debug("populateAsset(): found asset = " + oldAsset);
			}
		}*/

		if (null == oldAsset) {
			log.debug("populateAsset(): before findAssetByTemplate: asset = " + asset);
			try {
				oldAsset = assetRepository.loadAssetByTemplate(assetUse.getCommonWork().getId(), asset);
				if(null != oldAsset) {
					oldAsset = assetRepository.lazyLoad(Asset.class, oldAsset.getId(), new String[] {"sources"});
				}
			} catch (Exception e) {
				log.debug("populateAsset(): findAssetByTemplate failed [" + e.getMessage() + "]");
				throw new ServiceException("Failed to load asset "
				        + ((asset.getDescription() == null) ? "" : "[" + asset.getDescription() + "]"), e);
			}

			log.debug("populateAsset(): after findAssetByTemplate: asset = " + oldAsset);
		}

		if (null != oldAsset) {
			// lnagy - do not merge - just use the oldAsset
			/*
			try {
				BeanUtility.merge(asset, oldAsset);

				log.debug("populateAsset(): copyProperties " + oldAsset);
			} catch (Exception e) {
				throw new ServiceException("Failed to merge existing asset with incoming asset", e);
			}
			 */
			if(null != assetUse.getAsset().getImportSource()){
			if(assetUse.getAsset().getImportSource().getDescription().equals("CS SpreadSheet")){
				if(assetUse.getAsset().getSources().toString() != null){
					//List<Source> src_old =  new ArrayList<Source>();
					 List<Source> src_old = assetUse.getAsset().getSources();
					//src_old.add(source_again);
					//updateAssetUse.getAsset().getSources().add(source);
					//assetUse.getAsset().setSources(src_old);
					oldAsset.setSources(src_old);
				}
			}
			}
			assetUse.setAsset(oldAsset);

		} else {
			assetUse.setAsset(asset);
		}



		log.debug("asset after:" + assetUse.getAsset());

	}

	/**
	 * Populates the AssetUse with the missing info.
	 */
	private AssetUse populateAndValidateAssetUse(ExtendedAssetUse assetUse) throws PersistenceException,
	        ServiceException, ValidateException {
		log.debug("entering pupulateAndValidateAssetUse");
		ArgUtil.notNull(assetUse, "assetUse");
		ArgUtil.notNull(assetUse.getAsset(), "asset");

		// try to find the asset by description or externalId, or vendorId if available
		log.debug("asset use:" + assetUse);
		log.debug("asset " + assetUse.getAsset());
		log.debug("component " + assetUse.getComponent());

		populateAsset(assetUse);

		// populates the source information
		populateSource(assetUse.getAsset());

		// some validations and default values will go here
		// if position is not empty, and usage is empty, set usage to FIGURE
		if (null == assetUse.getUsage() || StringUtils.isBlank(assetUse.getUsage().getCode())) {
			assetUse.setUsage(Usage.FIGURE);
		}

		// Convert obsolete value "Numbered Figure" to "Figure" - we are back to using "Unnumbered Figure" now
		if (assetUse.getUsage() != null && "Numbered Figure".equalsIgnoreCase(assetUse.getUsage().getCode())) {
			assetUse.setUsage(Usage.FIGURE);
		}
		//if (assetUse.getUsage() != null && "Unnumbered Figure".equalsIgnoreCase(assetUse.getUsage().getCode())) {
		//	assetUse.setUsage(Usage.FIGURE);
		//}
		// Convert obsolete value "Cover" to "Front Cover"
		if (assetUse.getUsage() != null && "Cover".equalsIgnoreCase(assetUse.getUsage().getCode())) {
			assetUse.setUsage(Usage.FRONT_COVER);
		}

		// if no OwnerType, we default to 3rd party
		if (null == assetUse.getAsset().getOwnerType()
		        || StringUtils.isBlank(assetUse.getAsset().getOwnerType().getCode())) {
			// add logic for product flag isAllWileyOwned.
			// If the flag is TRUE, all OwnerTypes are WILEY
			if (assetUse.getCommonWork().getInteriorCWStatus().equals(CommonWorkStatus.NO_PERMISSIONS_REQUIRED)) {
				assetUse.getAsset().setOwnerType(OwnerType.WILEY);
			} else {
				assetUse.getAsset().setOwnerType(OwnerType.THIRD_PARTY);
			}
		}

		// default 3rd party to Managed
		if (assetUse.getAsset().getOwnerType().equals(OwnerType.THIRD_PARTY)) {
			assetUse.getAsset().setManaged(true);
		}

		// If !managed then set royaltyFree to true
		// TODO: In the future, when/if we allow the user to have a royaltyFree
		// column in the spreadsheet, give an error if they have asked for
		// royaltyFree == false (and managed is true).
		if (!assetUse.getAsset().isManaged()) {
			assetUse.getAsset().setRoyaltyFree(true);
		}

		// if no MediaType, we default to Photo
		if (null == assetUse.getAsset().getMediaType()
		        || StringUtils.isBlank(assetUse.getAsset().getMediaType().getCode())) {
			assetUse.getAsset().setMediaType(MediaType.PHOTO);
		}

		// if it is an old asset, we check the ignore table and update just the asset and exit
		if (null != assetUse.getAsset().getId() && assetRepository.isIgnoredAsset(assetUse.getAsset().getId())) {
			try {
				assetRepository.saveRequiresNew(assetUse.getAsset());
			} catch (PersistenceException e) {
				throw new ServiceException("Failed to persist the asset metadata [" + assetUse.getAsset().getId() + "]", e);
			}
			throw new ServiceException("Found an asset that needs to be ignored - still persist metadata [" + assetUse.getAsset().getId() + "]");
		}

		// handles component - tries to load by name, or sets up for a new one
		log.debug("about to call pupulateComponent");
		populateComponent(assetUse);

		// run the rest of the validations
		log.debug("about to call validate()");
		validate(assetUse);

		return assetUse;
	}

	void validate(ExtendedAssetUse assetUse) throws ValidateException {
		StringBuilder validateError = new StringBuilder();
		ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
		Validator validator = validatorFactory.getValidator();
		Set<? extends ConstraintViolation<?>> violations = validator.validate(assetUse);

		validateError.append(JPARepository.buildValidationErrorMessage(violations));
		// if no source but contract specified something is wrong

		if( assetUse.getImportSource() != null && assetUse.getImportSource().equals(ImportSource.FROM_CS_SPREADSHEET)) {
			if(null != assetUse.getAsset().getId() || assetUse.getContract().getId() != null ) {
				if (null == assetUse.getContract().getSource() &&
					(null != assetUse.getContract() && StringUtils.isNotBlank(assetUse.getContract().getNumber()))) {
				throw new ValidateException("No source but contract specified");
				}
			}

		} else {
			if(null != assetUse.getAsset().getId() || ( null != assetUse.getContract() &&  assetUse.getContract().getId() != null )) {
				if (CollectionUtils.isEmpty(assetUse.getAsset().getSources()) &&
					(null != assetUse.getContract() && StringUtils.isNotBlank(assetUse.getContract().getNumber()))) {
				throw new ValidateException("No source but contract specified");
				}
			}
		}

		// if contract but no start date
		if (null != assetUse.getContract() && StringUtils.isNotBlank(assetUse.getContract().getNumber()) && null == assetUse.getContract().getDate()){
			throw new ValidateException("Contract but no date");
		}
		//TODO add all impossible rules here
		try {
			assetUse.validate();
		} catch (ValidateException ve) {
			validateError.append(ve.getMessage());
		}

		String veString = validateError.toString();
		if (StringUtils.isNotBlank(veString)) {
			throw new ValidateException(veString);
		}
	}

	/**
	 * Loads an existing source, validates the source or sets an source object to be created in persist.
	 *
	 * @param asset
	 * @return
	 * @throws ValidateException
	 * @throws PersistenceException
	 */
	private void populateSource(Asset asset) throws ValidateException, PersistenceException {
		ArgUtil.notNull(asset, "asset");

		if (CollectionUtils.isEmpty(asset.getSources())) {
			return;
		}

		Source source = asset.getSource(0);

		// clear all sources in case none will be found or created
		asset.getSources().clear();

		// source name is blank
		if (StringUtils.isBlank(source.getName())) {
			return;
		}

		// check to see if it is OwnerType - if it is, we set the owner type,
		// otherwise we consider it a source name
		OwnerType ownerType = OwnerType.getOwnerType(source.getName());
		if (null != ownerType) {
			asset.setOwnerType(ownerType);
			return;
		}

		// if length is greater then DB size, the findSourceByTemplate
		// method fails with an ugly error
		if (source.getName().length() > Source.MAX_NAME_LENGTH) {
			throw new ValidateException("Invalid source name : length > " + Source.MAX_NAME_LENGTH
			        + " chars [" + source.getName() + "]");
		} else {
			// new a new method that load by first looking at transfer history by name or by external_id
			// this will replace loadSourceByTemplate here
			Source oldSource = sourceRepository.loadSourceByTemplate(source);

			log.debug("populate(): findSourceByTemplate " + oldSource);
			// source not found in DB - we create one
			if (null == oldSource) {
				// lnagy - not throwing the exception anymore, if source not found
				// throw new ValidateException("Invalid source " + source.getName());

				// if the required fields to create a source are not present, we throw exception
				// lnagy - change so we not throw exception if required fields are missing, but default them
				/*
				 * if (null == source.getCountry() || StringUtils.isBlank(source.getCountry().getCode()) ||
				 * StringUtils.isBlank(source.getPermissionType())) { throw new ValidateException("Invalid
				 * source - missing required fields to create a new one - " + source.getName()); }
				 */
				// if the source is null, it means no extra required fields
				// are coming from spreadsheet

				// change so we not throw exception if required fields are
				// missing, but default them
				if (null == source.getCountry()
				        || StringUtils.isBlank(source.getCountry().getCode())) {
					source.setCountry(new Country("US", "USA"));
				}

				if (StringUtils.isBlank(source.getPermissionType())) {
					source.setPermissionType(PermissionType.ONE_TIME_USE.getCode());
				}
				// display name cannot be null
				source.setDisplayName(source.getName());

				oldSource = source;
				// persist the source together with the asset
			}
			// add the existing source
			asset.getSources().add(oldSource);
		}
	}

	/**
	 * if component specified, if the value is numeric, it is a chapter and we try match against the name
	 * "Chapter XX", else if it is alpha numeric, we try to match against the component name
	 *
	 * @param assetUse
	 * @throws PersistenceException
	 */
	private void populateComponent(AssetUse assetUse) throws PersistenceException {
		ArgUtil.notNull(assetUse, "assetUse");

		Component comp = assetUse.getComponent();

		if(null == comp){
			log.debug("comp is null");
			assetUse.setComponent(null);
			return;
		}

		if (null == comp || StringUtils.isBlank(comp.getName())) {
			assetUse.setComponent(null);
			return;
		}

		Component c = new Component();
		StringBuilder sb = new StringBuilder();
		char ch;
		String componentNumberOnly = "";
		String componentName = comp.getName();

		log.debug("populateComponent(): findComponent[1]1 " + componentName);
		if (NumberUtils.isDigits(componentName)) {
			c.setSortOrder(100 + new Integer(componentName));
			// if one digit component, we add extra 0 in front
			if (componentName.length() == 1) {
				componentName = "0" + componentName;
			}
			componentName = "Chapter " + componentName;
			c.setCategory(ComponentCategory.CHAPTER);
		} else {
			if (null == comp.getCategory())
				c.setCategory(ComponentCategory.getComponentCategory(componentName));
			else
				c.setCategory(comp.getCategory());

			for (int i=0; i < componentName.length(); i++) {
				ch = componentName.charAt(i);

				if (Character.isDigit(ch)) {
					sb.append(ch);
				}
			}
			componentNumberOnly = sb.toString();
			if (componentNumberOnly.length() == 1) {
				componentName = "Chapter 0" + componentNumberOnly;
			}
			else {
				if(componentName.contains("Chapter")) {
				componentName = "Chapter " + componentNumberOnly;
				}
			}
		}
		log.debug("populateComponent(): findComponent[2]2 " + componentName);
		// now we try to load the component
		c.setCommonWork(assetUse.getCommonWork());
		c.setName(componentName);

		Component existingComponent = cwRepository.loadComponentByName(c.getCommonWork().getId(), c.getName());
		if (existingComponent == null) {
			// lnagy - if we do not find the component, we create it
			// throw new ValidateException("Invalid component name [ " + component + "]. Please first create the component.");
			log.debug("populateComponent(): Invalid component name [" + componentName + "]. We try to create it in persist.");
		} else {
			c = existingComponent;
		}
		assetUse.setComponent(c);
	}

	/**
	 * tries to load the asset and merges the values with the incoming values
	 *
	 * @param assetUse
	 * @return
	 * @throws ServiceException
	 */
	private void populateAsset(AssetUse assetUse) throws ServiceException {
		ArgUtil.notNull(assetUse, "assetUse");
		ArgUtil.notNull(assetUse.getAsset(), "assetUse.asset");
		ArgUtil.notNull(assetUse.getCommonWork(), "assetUse.commonWork");

		Asset asset = assetUse.getAsset();
		log.debug("asset before:" + assetUse);

		// description is a non blank field, so we validate it here
		if (StringUtils.isBlank(asset.getDescription())) {
			if (StringUtils.isBlank(assetUse.getPosition())) {
				throw new ServiceException("Asset description and position are empty.");
			} else {
				// set the description to be position
				asset.setDescription(assetUse.getPosition());
			}
		}

		Asset oldAsset = null;
		log.debug("populateAsset(): before findAssetBySource: asset = " + asset);

		// we try to find the asset by source and vendor id if that is not empty
	/*	if (CollectionUtils.isNotEmpty(asset.getSources()) &&
			StringUtils.isNotBlank(assetUse.getAsset().getSource(0).getName()) &&
			StringUtils.isNotBlank(assetUse.getAsset().getVendorId())) {
			oldAsset = assetRepository.loadAssetBySourceVendorId (
					assetUse.getAsset().getSource(0).getName(),
					assetUse.getAsset().getVendorId());
			if (null != oldAsset) {
				log.debug("populateAsset(): found asset = " + oldAsset);
			}
		}*/

		if (null == oldAsset) {
			log.debug("populateAsset(): before findAssetByTemplate: asset = " + asset);
			try {
				oldAsset = assetRepository.loadAssetByTemplate(assetUse.getCommonWork().getId(), asset);
				if(null != oldAsset) {
					oldAsset = assetRepository.lazyLoad(Asset.class, oldAsset.getId(), new String[] {"sources"});
				}
			} catch (Exception e) {
				log.debug("populateAsset(): findAssetByTemplate failed [" + e.getMessage() + "]");
				throw new ServiceException("Failed to load asset "
				        + ((asset.getDescription() == null) ? "" : "[" + asset.getDescription() + "]"), e);
			}

			log.debug("populateAsset(): after findAssetByTemplate: asset = " + oldAsset);
		}

		if (null != oldAsset) {
			// lnagy - do not merge - just use the oldAsset
			/*
			try {
				BeanUtility.merge(asset, oldAsset);

				log.debug("populateAsset(): copyProperties " + oldAsset);
			} catch (Exception e) {
				throw new ServiceException("Failed to merge existing asset with incoming asset", e);
			}
			 */
			if(null != assetUse.getAsset().getImportSource()){
			if(assetUse.getAsset().getImportSource().getDescription().equals("CS SpreadSheet")){
				if(assetUse.getAsset().getSources().toString() != null){
					//List<Source> src_old =  new ArrayList<Source>();
					 List<Source> src_old = assetUse.getAsset().getSources();
					//src_old.add(source_again);
					//updateAssetUse.getAsset().getSources().add(source);
					//assetUse.getAsset().setSources(src_old);
					oldAsset.setSources(src_old);
				}
			}
			}
			assetUse.setAsset(oldAsset);

		} else {
			assetUse.setAsset(asset);
		}



		log.debug("asset after:" + assetUse.getAsset());

	}

	public ExcelTransformerUtility getExcelUtility() {
		return excelUtility;
	}

	public void setExcelUtility(ExcelTransformerUtility excelUtility) {
		this.excelUtility = excelUtility;
	}

	public AssetUseService getAssetUseService() {
		return assetUseService;
	}

	public void setAssetUseService(AssetUseService assetUseService) {
		this.assetUseService = assetUseService;
	}

	public SourceRepository getSourceRepository() {
    	return sourceRepository;
    }

	public void setSourceRepository(SourceRepository sourceRepository) {
    	this.sourceRepository = sourceRepository;
    }

	public ImportProductsUtility getImportProductsUtility() {
		return importProductsUtility;
	}

	public void setImportProductsUtility(ImportProductsUtility importProductsUtility) {
		this.importProductsUtility = importProductsUtility;
	}

	public CommonWorkService getCommonWorkService() {
		return commonWorkService;
	}

	public void setCommonWorkService(CommonWorkService commonWorkService) {
		this.commonWorkService = commonWorkService;
	}

	public AssetRepository getAssetRepository() {
		return assetRepository;
	}

	public void setAssetRepository(AssetRepository assetRepository) {
		this.assetRepository = assetRepository;
	}

	public ContractService getContractService() {
		return contractService;
	}

	public void setContractService(ContractService contractService) {
		this.contractService = contractService;
	}

	public ProductRepository getProductRepository() {
		return productRepository;
	}

	public void setProductRepository(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}

	public CommonWorkRepository getCommonWorkRepository() {
		return cwRepository;
	}

	public void setCommonWorkRepository(CommonWorkRepository cwRepository) {
		this.cwRepository = cwRepository;
	}

	public UserRepository getUserRepository() {
		return userRepository;
	}

	public void setUserRepository(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public ConditionRepository getConditionRepository() {
		return conditionRepository;
	}

	public void setConditionRepository(ConditionRepository conditionRepository) {
		this.conditionRepository = conditionRepository;
	}

	public ContractRepository getContractRepository() {
		return contractRepository;
	}

	public void setContractRepository(ContractRepository contractRepository) {
		this.contractRepository = contractRepository;
	}
}
