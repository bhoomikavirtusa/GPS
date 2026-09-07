package com.wiley.permissions.services.imports;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import net.sf.jxls.reader.XLSDataReadException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.wiley.permissions.common.bean.BeanUtility;
import com.wiley.permissions.domain.persistence.permissions.AssetToSource;
import com.wiley.permissions.domain.persistence.permissions.AssetUse;
import com.wiley.permissions.domain.persistence.permissions.CommonWork;
import com.wiley.permissions.domain.persistence.permissions.Component;
import com.wiley.permissions.domain.persistence.permissions.ComponentCategory;
import com.wiley.permissions.domain.persistence.permissions.ConditionNode;
import com.wiley.permissions.domain.persistence.permissions.ConditionType;
import com.wiley.permissions.domain.persistence.permissions.ConditionType.ConditionCode;
import com.wiley.permissions.domain.persistence.permissions.ExtendedAssetUse;
import com.wiley.permissions.domain.persistence.permissions.ImportSource;
import com.wiley.permissions.domain.persistence.permissions.PermissionType;
import com.wiley.permissions.domain.persistence.permissions.Product;
import com.wiley.permissions.domain.persistence.permissions.Source;
import com.wiley.permissions.domain.persistence.permissions.Usage;
import com.wiley.permissions.domain.persistence.permissions.UserGroup;
import com.wiley.permissions.persistence.PersistenceException;
import com.wiley.permissions.repositories.AssetUseRepository;
import com.wiley.permissions.repositories.ConditionRepository;
import com.wiley.permissions.repositories.ContractRepository;
import com.wiley.permissions.repositories.SourceRepository;

public class CSFilemakerDataImportUtility {

	private static final Log log = LogFactory.getLog(CSFilemakerDataImportUtility.class);

	private SourceRepository sourceRepository;
	private ConditionRepository conditionRepository;
	private AssetUseRepository assetUseRepository;
	private ImportAssetsUtility importAssetsUtility;
	private ContractRepository contractRepository;

	private static final String CS_FILEMAKER_ASSETS_MAPPING_FILE = "/conf/mapping/filemakerCSMapping.xml";

	/**
	 * Imports the assets from CS fileMakerPro
	 *
	 * @param data
	 * @throws XLSDataReadException
	 * @throws PersistenceException
	 * @throws IOException
	 */
	public void importFileMakerAssets(byte[] data) throws XLSDataReadException, PersistenceException, IOException {
		log.debug("submit(): Before populateAndValidate");
		InputStream in = getClass().getResourceAsStream(CS_FILEMAKER_ASSETS_MAPPING_FILE);
		String mapping = IOUtils.toString(in);

		// this has to be done this way because userGroup defaults is not set up with id in it.  Do not change this without changing userGroup class;
		UserGroup defaultUg = assetUseRepository.find(UserGroup.class, new Integer("1"));

		log.debug("mapping:" + mapping);
		List<ExtendedAssetUse> eauList = importAssetsUtility.convertXLSToAssetUseList(data, mapping.getBytes());
		log.debug("total rows found in spreadsheet=" + eauList.size());

		for (ExtendedAssetUse assetUse : eauList) {
			log.debug("CS IMPORT : " + assetUse.getAsset().getDescription());

			try {
				List<ExtendedAssetUse> updateAssetUseList = new ArrayList<ExtendedAssetUse>();

				// Need to redo this method (getPrintRun) with the new conditions
				//log.debug("assetUse.contract.printRun:" + assetUse.getContract().getPrintRun());
				log.debug("assetUse.asset.mediaType.code:" + assetUse.getAsset().getMediaType().getCode());
				log.debug("assetUse.canceled:" + assetUse.isCanceled());
				log.debug("assetUse.contract.date:" + assetUse.getContract().getDate());
				log.debug("assetUse.contract.endDate:" + assetUse.getContract().getEndDate());
				log.debug("assetUse.permissionComment:" + assetUse.getPermissionComment());
				log.debug("assetUse.asset.description:" + assetUse.getAsset().getDescription());
				log.debug("assetUse.permissionType:" + assetUse.getPermissionType());
				log.debug("assetUse.permissionStatus.description:" + assetUse.getStatus().getDescription());
				log.debug("assetUse.statusExplanation:" + assetUse.getStatusExplanation());
				log.debug("assetUse.creditLine:" + assetUse.getAsset().getCreditLine());
				log.debug("assetUse.price:" + assetUse.getPrice());
				log.debug("assetUse.createdDate:" + assetUse.getCreatedDate());
				log.debug("assetUse.photoSize:" + assetUse.getPhotoSize());
				log.debug("assetUse.contract.startDate:" + assetUse.getContract().getStartDate());
				log.debug("assetUse.usage.code:" + assetUse.getUsage().getCode());
				log.debug("assetUse.asset.vendorId:" + assetUse.getAsset().getVendorId());
				log.debug("assetUse.printRuncondition:" + assetUse.getPrintRunCondition());
				log.debug("assetUse.ebookPrintRuncondition:" + assetUse.getEbookPrintRunCondition());
				log.debug("assetUse.ebookPrintRunIncludeEbooks:" + assetUse.getPrintRunIncludeEbooks());
				log.debug("assetUse.languageCondition:" + assetUse.getLanguageCondition());
				log.debug("assetUse.salesTerritoryCondition:" + assetUse.getSalesTerritoryCondition());
				log.debug("assetUse.unlimitedPrintRunCondition:" + assetUse.getUnlimitedPrintRunCondition());
				log.debug("assetUse.contract.limitationOfLiability:" + assetUse.getContract().getLimitationOfLiability());
				log.debug("assetUse.territoryComments:" + assetUse.getTerritoryComments());
				log.debug("assetUse.asset.source[0].name:" + assetUse.getAsset().getSourcesAsString());

                //log.debug("assetUse"+assetUse.getCommonWork().getPrimaryProduct().getIsbn10() + " " +assetUse.getCommonWork().getPrimaryProduct().getIsbn13());
				log.debug("assetUse.isb10:" + assetUse.getIsb10());
				log.debug("assetUse.isb13:" + assetUse.getIsb13());

				//adding source
				Source source = getSourceRepository().loadSourceByName(assetUse.getAsset().getSourcesAsString());
				if(source == null && (assetUse.getAsset().getSourcesAsString() != null || assetUse.getAsset().getSourcesAsString().equals(""))){
					Source wSrc = new Source();
					wSrc.setName(assetUse.getAsset().getSourcesAsString());
					wSrc.setDisplayName(assetUse.getAsset().getSourcesAsString());
					wSrc.setNofly(false);
					wSrc.setPermissionType(PermissionType.REUSE_PO.getCode());
					//wSrc.setExternalId(externalId);
					try{
					source = getSourceRepository().saveSource(wSrc);
					}catch(Exception e){
						System.out.println("saveSource() had error : "+e.getMessage());
					}
				}
				assetUse.getAsset().setSources(new ArrayList<Source>());
				assetUse.getAsset().getSources().add(source);
				if(null != assetUse.getContract()) {
					assetUse.getContract().setSource(source);

				}

				// load the primary product
				String wIsbn = assetUse.getIsb13();
				if(null == wIsbn) wIsbn =  assetUse.getIsb10();
				Product wProduct = getAssetUseRepository().getCommonWorkRepository().getProductRepository().getProductByISBN(wIsbn);
				wProduct = getAssetUseRepository().getCommonWorkRepository().lazyLoad(Product.class, wProduct.getId(), new String[] {"commonWork"});
				log.debug("found cwid:" + wProduct.getCommonWork().getId());

				CommonWork wCW = getAssetUseRepository().getCommonWorkRepository().lazyLoad(CommonWork.class, wProduct.getCommonWork().getId(), new String[] {"products"});

				//assetUse.getContract().setCommonWork(wCW);

				log.debug("cwFound" + wCW.getId());

				assetUse.setCommonWork(wCW);

				if(null == assetUse.getCommonWork()) {
					log.debug("ERROR no common weork");
				} else {
					log.debug("common work found ");
					log.debug("id:" + assetUse.getCommonWork().getId());
				}
				if (null == assetUse.getCommonWork().getPrimaryProduct()) {
					log.debug("no promary product");
				} else {
					log.debug("product found ");
					log.debug("id:" + assetUse.getCommonWork().getPrimaryProduct().getId());

				}

				// this has to be done this way because userGroup defaults is not set up with id in it.  Do not change this without changing userGroup class;
				assetUse.setUserGroup(defaultUg);

				log.debug("assetUse.isb10:" + assetUse.getIsb10());
				log.debug("assetUse.isb13:" + assetUse.getIsb13());



		//		assetUse.setUserGroup(defaultUg);

				assetUse.setImportSource(ImportSource.FROM_CS_SPREADSHEET);
				assetUse.getAsset().setImportSource(ImportSource.FROM_CS_SPREADSHEET);

				Product product = assetUse.getCommonWork().getPrimaryProduct();



				if (null != assetUse.getCommonWork() && null != product) {
					if (StringUtils.isEmpty(product.getIsbn10())) {
						product.setIsbn10(product.getIsbn13());
					}

					log.debug("primaryProduct.isbn10:" + product.getIsbn10());
					log.debug("primaryProduct.isbn13:" + product.getIsbn13());
				}
				/////////////////////////////////////////////////////////////////
				// read the source
				//Source src = getSource(assetUse);
				Source src = source;
				if (null == src) {
					log.debug("importFileMakerAssets(): source not found");
					continue;
				}

				/////////////////////////////////////////////////////////////////
				// read the deal
				if (null != assetUse.getPermissionType() && assetUse.getPermissionType().equals("Royalty Free")) {
					assetUse.setDealId(getDealId (src));
				}

				/////////////////////////////////////////////////////////////////
				// process the conditions
				assetUse.getContract().setConditionNodeTree(processConditions(assetUse, assetUse.getContract().isPermissionForm()));
				assetUse.setStatus(null);
				assetUse.getContract().setPurchaseOrder(null);

				//log.debug(assetUse.getContract());
				/////////////////////////////////////////////////////////////////
				// deal with usages
				String usageData = assetUse.getUsage().getCode();
				String[] tokens = usageData.split("/");
				String dUsage = tokens[0];
				assetUse.setUsage(Usage.getUsageByDescription(dUsage));

				// process other asset uses (handle multiple asset use separated by a slash
				for (int x = 0; x < tokens.length; x++) {
					dUsage = tokens[x];
					try {
						if (null == dUsage || dUsage.length() < 1)
							continue;
						ExtendedAssetUse ex = BeanUtility.clone(assetUse, ExtendedAssetUse.class);
						if (x > 0) {
							ex.setContract(null);
						}
						log.debug("***** adding assetuse to update list for:" + dUsage);
						ex.setId(null);
						ex.setUsage(Usage.getUsageByDescription(dUsage));
						ex.setStatus(null);
						if (null != ex.getUsage()) {
							updateAssetUseList.add(ex);
							log.debug("***** assetuse  added to update list for:" + dUsage);
						} else {
							log.debug("bad asse use not added to list");
						}
					} catch (Exception e) {
						log.error(e);
						// ignore
					}
				} // end of asset use tokens processing loop

				/////////////////////////////////////////////////////////////////
				// now go save the asset use bean(s)
				for (ExtendedAssetUse updateAssetUse : updateAssetUseList) {
					try {
						log.debug("about to persist " + updateAssetUse.getUsage().getCode()
						            + " for asset " + updateAssetUse.getAsset().getVendorId());
						// FIGURE OUT IF ASSET USE ALREADY EXISTS - create contract but use existing asset use
						String isbnSearch = "";

						String isbn10 = product.getIsbn10();
						String isbn13 = product.getIsbn13();
						if (StringUtils.isNotBlank(isbn13)) {
							isbnSearch = isbn13;
						} else {
							isbnSearch = isbn10;
						}
						String usage = assetUse.getUsage().getCode();
						String vendorId = assetUse.getAsset().getVendorId();

						AssetUse oldAu = getAssetUseRepository().loadAssetUseByUsage(isbnSearch, usage, vendorId);

						if (null != oldAu) {
							log.debug("Processing existing asset use");
							ExtendedAssetUse wau = new ExtendedAssetUse(oldAu, updateAssetUse.getContract());
							updateAssetUse = wau;
							if(assetUse.getAsset().getSources().toString() != null){
								List<Source> src_old =  new ArrayList<Source>();
								Source source_again = getSourceRepository().loadSourceByName(assetUse.getAsset().getSourcesAsString());
								src_old.add(source_again);
								//updateAssetUse.getAsset().getSources().add(source);
								updateAssetUse.getAsset().setSources(src_old);
							}
						} else {
							log.debug("processing new asset use");
						}

						//changed
						//updateAssetUse.getAsset().setSources(new ArrayList<Source>());
						if(source != null){
							List<AssetToSource> assetToSources = new ArrayList<AssetToSource>();
							AssetToSource a2s = new AssetToSource();
							a2s.setAsset(assetUse.getAsset());
							a2s.setAssetId(assetUse.getAsset().getId());
							a2s.setSource(source);
							a2s.setSourceId(source.getId());
							assetToSources.add(a2s);
							updateAssetUse.getAsset().setAssetToSources(assetToSources);
						}
						//Added for Validating the Duplicates for Asset based on ISBN , Contract Date, VendorID, Source Name for CS Spreadsheet
						if(StringUtils.isNotBlank(source.getId().toString()) &&
						   StringUtils.isNotBlank(isbnSearch) &&
						   StringUtils.isNotBlank(vendorId) &&
						   StringUtils.isNotBlank(assetUse.getContract().getNumber())){

							String contractNum = assetUse.getContract().getNumber();
							int countOfDupAssets = getAssetUseRepository().loadAssetBySourceVendorIdForCS(source.getId(), vendorId, isbnSearch, assetUse.getContract().getNumber());
							if(countOfDupAssets>0){
								throw new Exception("failed to persist Duplicates found in the DB "+vendorId+" "+isbnSearch+" "+source.getId().toString()+" "+contractNum);
							}
						}
						updateAssetUse.getAsset().addSource(source);
						//updateAssetUse.getAsset().getSources().add(getSourceRepository().loadSourceByName(assetUse.getAsset().getSourcesAsString()));
						//ends
						AssetUse wAu = persistCustomAssetUse(updateAssetUse, false);
						log.debug("******* successfully persisted ********" + wAu.getAsset().getDescription());

					} catch (Exception e) {
						log.debug("******* failed to persist ********" + assetUse.getAsset().getDescription(), e);
						log.error("CS importFileMakerAssets() could not process assetUse: " + updateAssetUse.getUsage().getCode()
						            + " for asset " + updateAssetUse.getAsset().getVendorId());
					}
				}

			} catch (Exception ex) {
				log.error("***** errors found in spreadsheet row ******", ex);
			}
			log.debug("------------------------------------------------------------------------------------");
		} // end of process loop for spreadsheet row
	}

	/**
	 * Imports one asset from the CS Filemaker or custom view database.
	 * @param assetUse
	 * @return
	 */
	private ImportAssetsStatus populateCustomAssetUse(ExtendedAssetUse assetUse) {
		log.debug("populateCustomAssetUseList()...begin");
		// TODO set defaults
		// lnagy - do not default to usage - we needed that for the import but tha is done
		if(assetUse.getImportSource() != null &&  assetUse.getImportSource().equals(ImportSource.FROM_CS_SPREADSHEET))
		{
		Component c = new Component();
		c.setCategory(ComponentCategory.COVER);
		c.setName(assetUse.getUsage().getCode());
		assetUse.setComponent(c);
		}
	//	if(null != assetUse.getUserGroup()) {
	//		assetUse.setUserGroup(assetUse.getUserGroup());
	//	}
		assetUse.setCustom(true);
		// etc.
		return importAssetsUtility.populateAssetUse(assetUse);
	}

	/**
	 * Persists the list of extended asset use beans
	 * @param au
	 * @param calculateStatus
	 * @return number of good records
	 * @throws Exception
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public AssetUse persistCustomAssetUse(ExtendedAssetUse au, boolean calculateStatus) throws Exception {
		ImportAssetsStatus status = populateCustomAssetUse(au);
		if (! status.isStatusOK()) {
			throw new Exception (status.getErrorMessage());
		}

		// the following call includes updating the permission status and sending
		// update Asset and AssetUse messages
		List<ExtendedAssetUse> goodList = status.getGoodRecords();
		try {
			importAssetsUtility.persistAssetUseList(goodList, status, calculateStatus);
			if (goodList.size() != 1) {
				throw new Exception("failed to persist");
			}
		} catch (Exception e) {
			log.debug("persistCustomAssetUseList(): failed to persist", e);
			throw new Exception("failed to persist[" + e.getMessage() + "]");
		}
		// the list should contain one assetUse
		au = goodList.get(0);
		log.debug("persistCustomAssetUseList(): auId returned " + au.getId());
		// we return the id
		return au;
	}

	/**
	 * Loads the source
	 * @param assetUse
	 * @return Source
	 * @throws PersistenceException
	 */
	protected Source getSource (ExtendedAssetUse assetUse) throws PersistenceException {
		if (CollectionUtils.isEmpty(assetUse.getAsset().getSources())) {
			return null;
		}
		Source src = assetUse.getAsset().getSources().get(0);

		src = getSourceRepository().loadSourceUsingTransferHistory(src);
		if (null != src) {
			assetUse.getAsset().getSources().clear();
			assetUse.getAsset().getSources().add(src);
		}
		return src;
	}

	/**
	 * returns the dealId if any applicable
	 * @param src
	 * @return integer
	 * @throws PersistenceException
	 */
	protected Integer getDealId (Source src) throws PersistenceException {
		// read the deal
		Integer dealId = 0;
		if (null != src.getSourceGroup() && CollectionUtils.isNotEmpty(src.getSourceGroup().getRoyaltyFreeDeals())) {
			dealId = src.getSourceGroup().getRoyaltyFreeDeals().get(0).getId();
		}
		return dealId;
	}


	/**
	 * returns a list of condition nodes
	 * @param assetUse
	 * @return List<ConditionNode>
	 */
	protected List<ConditionNode> processConditions (ExtendedAssetUse assetUse, boolean isPermissionForm) {
		// load default condition nodes
		List<ConditionNode> conditionNodes = new ArrayList<ConditionNode>();

		conditionNodes = conditionRepository.loadContractConditions(null, isPermissionForm);

		// TODO: 6/2014 Important! Conditions have changed a lot so this section should be reviewed/revised if it's going to be used again
		/*if (!isPermissionForm)
			throw new RuntimeException("Must revise this condition section before using");*/

		for(ConditionNode node : conditionNodes){
			String cConditionType = node.getCode();;
			String cConditionRollupValue = node.getValue();
			String cConditionValue = node.getRollupValue();

			log.debug("1.cConditionType : "+cConditionType);
			log.debug("2.cConditionRollupValue : "+cConditionRollupValue);
			log.debug("3.cConditionValue : "+cConditionValue);

			//First Condition check for What Media Types are granted?
			if (node.getCode().equals(ConditionType.PRINT_RUN_EBOOK.getCode())) {

				if(assetUse.getPrintRunIncludeEbooks().toLowerCase().equals("yes")){
					cConditionType = ConditionCode.MEDIUM.getCode();
					cConditionRollupValue = ConditionType.MEDIUM_ALL.getCode();
					conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
					cConditionType = ConditionCode.MEDIUM_ALL.getCode();
					cConditionValue = "true";
					conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
				}
				else if(assetUse.getPrintRunIncludeEbooks().toLowerCase().equals("no")){
					cConditionType = ConditionCode.MEDIUM.getCode();
					cConditionRollupValue = ConditionType.MEDIUM_PRINT_ONLY.getCode();
					conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
					cConditionType = ConditionCode.MEDIUM_PRINT_ONLY.getCode();
					cConditionValue = "true";
					conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
				}
				else if(assetUse.getPrintRunIncludeEbooks().toLowerCase().equals("silent")){
					cConditionType = ConditionCode.MEDIUM.getCode();
					cConditionRollupValue = ConditionType.MEDIUM_NO_MENTION.getCode();
					conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
					cConditionType = ConditionCode.MEDIUM_NO_MENTION.getCode();
					cConditionValue = "true";
					conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);

				}else{
					//do nothing for now
				}
				   //conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
			}

			//Second Condition check for What distribution or sales territory rights are granted?
			if(node.getCode().equals(ConditionType.SALES_TERRITORY.getCode())){

				if(assetUse.getSalesTerritoryCondition().toLowerCase().equals("world")){
					cConditionType = ConditionCode.SALES_TERRITORY.getCode();
					cConditionRollupValue = ConditionType.SALES_WORLD_ALIAS.getCode();
					conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
					cConditionType = ConditionCode.SALES_WORLD_ALIAS.getCode();
					cConditionValue = "true";
					conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
				}
				else if(assetUse.getSalesTerritoryCondition().equals("North America")){
					cConditionType = ConditionCode.SALES_TERRITORY.getCode();
					cConditionRollupValue = ConditionType.SALES_NORTH_AMERICA_ALIAS.getCode();
					conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
					cConditionType = ConditionCode.SALES_NORTH_AMERICA_ALIAS.getCode();
					cConditionValue = "true";
					conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);

				}
				else if(assetUse.getSalesTerritoryCondition().equals("") || assetUse.getSalesTerritoryCondition() == null){
					cConditionType = ConditionCode.SALES_TERRITORY.getCode();
					cConditionRollupValue = ConditionType.SALES_NO_MENTION.getCode();
					conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
					cConditionType = ConditionCode.SALES_NO_MENTION.getCode();
					cConditionValue = "true";
					conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
				}
				else{
					cConditionType = ConditionCode.SALES_TERRITORY.getCode();
					cConditionRollupValue = ConditionType.SALES_WORLD_ALIAS.getCode();
					conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
					cConditionType = ConditionCode.SALES_WORLD_ALIAS.getCode();
					cConditionValue = "true";
					conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
				}
			}

			//third Condition check for What language rights are granted?
			if(node.getCode().equals(ConditionType.LANGUAGE.getCode())){

				if(assetUse.getLanguageCondition().equals("All Languages")){
					cConditionType = ConditionCode.LANGUAGE.getCode();
					cConditionRollupValue = ConditionType.LANGUAGE_ALL_ALIAS.getCode();
					conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
					cConditionType = ConditionCode.LANGUAGE_ALL_ALIAS.getCode();
					cConditionValue = "true";
					conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
				}
				else if(assetUse.getLanguageCondition().toLowerCase().equals("english")){
					cConditionType = ConditionCode.LANGUAGE.getCode();
					cConditionRollupValue = ConditionType.LANGUAGE_ENGLISH_ALIAS.getCode();
					conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
					cConditionType = ConditionCode.LANGUAGE_ENGLISH_ALIAS.getCode();
					cConditionValue = "true";
					conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
				}
				else if(assetUse.getLanguageCondition().equals("") || assetUse.getLanguageCondition() == null){
					cConditionType = ConditionCode.LANGUAGE.getCode();
					cConditionRollupValue = ConditionType.LANGUAGE_NO_MENTION.getCode();
					conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
					cConditionType = ConditionCode.LANGUAGE_NO_MENTION.getCode();
					cConditionValue = "true";
					conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
				}
				else{
					cConditionType = ConditionCode.LANGUAGE.getCode();
					if(assetUse.getLanguageCondition().toLowerCase().equals("german")){
						cConditionRollupValue = ConditionType.LANGUAGE_GERMAN.getCode();
					}else if(assetUse.getLanguageCondition().toLowerCase().equals("french")) {
						cConditionRollupValue = ConditionType.LANGUAGE_FRENCH.getCode();
					}else{
						//cConditionRollupValue = ConditionType.LANGUAGE_NO_MENTION.getCode();
					}
					conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
					if(assetUse.getLanguageCondition().toLowerCase().equals("german")){
						cConditionType = ConditionCode.LANGUAGE_GERMAN.getCode();
					}else if(assetUse.getLanguageCondition().toLowerCase().equals("french")) {
						cConditionType = ConditionCode.LANGUAGE_FRENCH.getCode();
					}else{
						//cConditionType = ConditionCode.LANGUAGE_NO_MENTION.getCode();
					}
					cConditionValue = "true";
					conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
				}

			}

			//fourth Condition check for Does the grant/letter include any reference to print run?
			if(node.getCode().equals(ConditionType.PRINT_RUN.getCode())){

				if(null != assetUse.getUnlimitedPrintRunCondition()){
				if(assetUse.getUnlimitedPrintRunCondition().equals("Yes")){
					cConditionType = ConditionCode.PRINT_RUN.getCode();
					cConditionRollupValue = ConditionType.PRINT_RUN_UNLIMITED.getCode();
					conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
					cConditionType = ConditionCode.PRINT_RUN_UNLIMITED.getCode();
					cConditionValue = "true";
					conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
				}
				else{
					int printRun = new Integer(assetUse.getPrintRunCondition());
					if(printRun > 0){
						cConditionType = ConditionCode.PRINT_RUN.getCode();
						cConditionRollupValue = "="+assetUse.getPrintRunCondition();
						conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
						cConditionType = ConditionCode.PRINT_RUN_LIMIT_BOX.getCode();
						cConditionValue = assetUse.getPrintRunCondition();
						conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
					}else{
						cConditionType = ConditionCode.PRINT_RUN.getCode();
						cConditionRollupValue = ConditionType.PRINT_RUN_NO_MENTION.getCode();
						conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
						cConditionType = ConditionCode.PRINT_RUN_NO_MENTION.getCode();
						cConditionValue = "true";
						conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
					}
				}



				}
				else {
					cConditionType = ConditionCode.PRINT_RUN.getCode();
					//if(!assetUse.getPrintRunCondition().equals("") || assetUse.getPrintRunCondition() != null  ){
						cConditionRollupValue = ConditionType.PRINT_RUN_NO_MENTION.getCode();
					//}else{
					//	cConditionRollupValue = "="+assetUse.getPrintRunCondition();
					//}

					conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
					//if(!assetUse.getPrintRunCondition().equals("") || assetUse.getPrintRunCondition() != null  ){
						cConditionType = ConditionCode.PRINT_RUN_NO_MENTION.getCode();
					//}else{
					//	cConditionValue = assetUse.getPrintRunCondition();
					//}

					cConditionValue = "true";
					conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
				}
				/*else if(assetUse.getUnlimitedPrintRunCondition().equals("") || assetUse.getUnlimitedPrintRunCondition() == null){
					cConditionType = ConditionCode.PRINT_RUN.getCode();
					cConditionRollupValue = "="+assetUse.getPrintRunCondition();
					conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
					cConditionType = ConditionCode.PRINT_RUN_LIMIT_BOX.getCode();
					cConditionValue = assetUse.getPrintRunCondition();
					conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
				}
				else{

				}*/
			}

			//fifth Condition check for Does the grant/letter specify use in particular editions?
			if(node.getCode().equals(ConditionType.EDITION.getCode())){
					cConditionType = ConditionCode.EDITION.getCode();
					cConditionRollupValue = ConditionType.EDITION_THIS_FUTURE_AUTHOR.getCode();
					conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
					cConditionType = ConditionCode.EDITION_THIS_FUTURE_AUTHOR.getCode();
					cConditionValue = "true";
					conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
			}

			//sixth Condition check for Does the grant cover use of the asset(s) in derivative works?
			if(node.getCode().equals(ConditionType.DERIVATIVE_WORKS.getCode())){
					cConditionType = ConditionCode.DERIVATIVE_WORKS.getCode();
					cConditionRollupValue = ConditionType.DERIVATIVE_WORKS_ALL.getCode();
					conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
					cConditionType = ConditionCode.DERIVATIVE_WORKS_ALL.getCode();
					cConditionValue = "true";
					conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
			}

			//seventh Condition check for Are sublicensing rights granted?
			if(node.getCode().equals(ConditionType.SUBLICENSE.getCode())){
					cConditionType = ConditionCode.SUBLICENSE.getCode();
					cConditionRollupValue = ConditionType.SUBLICENSE_RIGHT.getCode();
					conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
					cConditionType = ConditionCode.SUBLICENSE_RIGHT.getCode();
					cConditionValue = "true";
					conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
			}

			//Eighth Condition check for Other restrictions and permission notes
			/*if(node.getCode().equals(ConditionType.OTHER.getCode())){

				if(assetUse.getPrintRunCondition().equals("Yes")){

				}
			}*/
		}

	//	if (!isPermissionForm)
	//		throw new RuntimeException("Must revise this condition section before using");


		/*
		for (ConditionNode node : conditionNodes) {
			String cConditionType = node.getCode();
			String cConditionRollupValue = node.getValue();
			String cConditionValue = node.getRollupValue();

			if (node.getCode().equals(ConditionType.PRINT_RUN_EBOOK.getCode())) {
				if (null != assetUse.getEbookPrintRunCondition() && assetUse.getEbookPrintRunCondition().trim().compareTo("null") != 0) {
					String printRunValue = assetUse.getEbookPrintRunCondition().trim().replace(",", "");
					cConditionRollupValue = printRunValue;
					cConditionValue = printRunValue;
					conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
				}
			}

			if (node.getCode().equals(ConditionType.PRINT_RUN.getCode())) {
				if (null != assetUse.getPrintRunCondition()) {
					String printRunValue = " ";
					if (null != assetUse.getPrintRunCondition()) {
						printRunValue = assetUse.getPrintRunCondition().trim().replace(",", "");
						cConditionValue = printRunValue;
					}
					boolean unlimited = false;
					if (ExtendedAssetUse.getBoolean (assetUse.getUnlimitedPrintRunCondition())) {
						printRunValue = "Unlimited Print Run";
						unlimited = true;
					}
					cConditionRollupValue = printRunValue;
					if (!unlimited && printRunValue.equals(" ")) {
						cConditionValue = node.getValue();
					}
					conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
				}
			}

			if (node.getCode().equals(ConditionType.LANGUAGE.getCode())) {
				if (null != assetUse.getLanguageCondition()) {
					cConditionRollupValue = node.getRollupValue();
					cConditionValue = assetUse.getLanguageCondition();

					if (assetUse.getLanguageCondition().toLowerCase().equals("all languages")) {
						// do nothing when all selected
					}
					if (assetUse.getLanguageCondition().toLowerCase().equals("english")) {
						conditionNodes = conditionRepository.updateConditionNodeData(ConditionType.LANGUAGE_ENGLISH.getCode(), "true", null, conditionNodes);

						// first set the language parent condition
						cConditionValue = "English";
						cConditionRollupValue = "English";
						conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
						// now set the English child condition
						cConditionType = ConditionType.LANGUAGE_ENGLISH.getCode();
						cConditionValue = "true";
						cConditionRollupValue = null;
					}
				}
			}

			if (node.getCode().equals(ConditionType.SALES_TERRITORY.getCode())) {
				if (null != assetUse.getSalesTerritoryCondition()) {
					if (assetUse.getSalesTerritoryCondition().toLowerCase().equals("world")) {
						// do nothing when all selected
					}

					if (assetUse.getSalesTerritoryCondition().toLowerCase().equals("north america")) {
						cConditionType = "sales_north_america";
						cConditionValue = "true";
					}

					// when territory is other, we need to look at the territory comments to see if we find what countries
					// to include
					if (assetUse.getSalesTerritoryCondition().toLowerCase().equals("other")) {
						if (null != assetUse.getTerritoryComments() &&
						        assetUse.getTerritoryComments().toLowerCase().indexOf("english language countries") > -1) {
							node.setChildren(conditionRepository.checkUncheckNode("sales_au", node.getChildren(), true));
							node.setChildren(conditionRepository.checkUncheckNode("sales_australia", node.getChildren(), true));
							node.setChildren(conditionRepository.checkUncheckNode("sales_ca", node.getChildren(), true));
							node.setChildren(conditionRepository.checkUncheckNode("sales_uk", node.getChildren(), true));
							node.setChildren(conditionRepository.checkUncheckNode("sales_uk-e", node.getChildren(), true));
							node.setChildren(conditionRepository.checkUncheckNode("sales_uk-ni", node.getChildren(), true));
							node.setChildren(conditionRepository.checkUncheckNode("sales_uk-s", node.getChildren(), true));
							node.setChildren(conditionRepository.checkUncheckNode("sales_uk-w", node.getChildren(), true));
							node.setChildren(conditionRepository.checkUncheckNode("sales_us", node.getChildren(), true));
						}

						if (null != assetUse.getTerritoryComments() && (
						        assetUse.getTerritoryComments().indexOf("U.S. only") > -1 ||
						                assetUse.getTerritoryComments().indexOf("United States") > -1 ||
						        assetUse.getTerritoryComments().indexOf("USA") > -1)) {
							node.setChildren(conditionRepository.checkUncheckNode("sales_us", node.getChildren(), true));
						}

						if (null != assetUse.getTerritoryComments() && (assetUse.getTerritoryComments().indexOf("Philippines") > -1)) {
							node.setChildren(conditionRepository.checkUncheckNode("sales_ph", node.getChildren(), true));
						}

						if (null != assetUse.getTerritoryComments() && assetUse.getTerritoryComments().toLowerCase().indexOf("excluding german") > -1) {
							conditionNodes = conditionRepository.updateConditionNodeData("sales_europe", "true", null, conditionNodes);
							node.setChildren(conditionRepository.checkUncheckNode("sales_de", node.getChildren(), false));
							node.setChildren(conditionRepository.checkUncheckNode("sales_ch", node.getChildren(), false));
							node.setChildren(conditionRepository.checkUncheckNode("sales_at", node.getChildren(), false));
						}
					}
				}
				conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
			}

			if (node.getCode().equals(ConditionType.MEDIUM.getCode())) {
				if (ExtendedAssetUse.getBoolean (assetUse.getPrintRunIncludeEbooks())) {
					// do not select anything because by default this means all selections
				} else {
					conditionNodes = conditionRepository.checkUncheckNode("medium_all_physical", conditionNodes, true);
				}
			}
		}
	*/
		return conditionNodes;
	}

	/**
	 * returns a list of condition nodes
	 * @param assetUse
	 * @return List<ConditionNode>
	 */
	protected List<ConditionNode> processConditionsForExcel (ExtendedAssetUse assetUse, boolean isPermissionForm) {
		// load default condition nodes
		List<ConditionNode> conditionNodes = new ArrayList<ConditionNode>();

		conditionNodes = conditionRepository.loadContractConditions(null, isPermissionForm);

		List<ConditionType> conditionTypes = conditionRepository.loadAllConditionTypeFromDB();
		ConditionType ct = null;
		String [] tokenStrings = null;
		String tempStr = null;

		for(ConditionNode node : conditionNodes){
			String cConditionType = node.getCode();
			String cConditionRollupValue = node.getValue();
			String cConditionValue = node.getRollupValue();

			log.debug("1.cConditionType : "+cConditionType);
			log.debug("2.cConditionRollupValue : "+cConditionRollupValue);
			log.debug("3.cConditionValue : "+cConditionValue);

			//First Condition check for What Media Types are granted? -- good
			if (node.getCode().equals(ConditionType.MEDIUM.getCode())) {

				if(assetUse.getDoesAgreedPrintRunIncludeEBooks().equalsIgnoreCase("yes")){
					if(StringUtils.isBlank(assetUse.getClearedForAllMediaTypes()) || assetUse.getClearedForAllMediaTypes().toLowerCase().equals("no")) {
						cConditionType = ConditionCode.MEDIUM.getCode();
						cConditionRollupValue = ConditionType.MEDIUM_PHYSICAL_EBOOK_WEB.getCode();
						conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
						cConditionType = ConditionCode.MEDIUM_PHYSICAL_EBOOK_WEB.getCode();
						cConditionValue = "true";
						conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
					}
					else {
						cConditionType = ConditionCode.MEDIUM.getCode();
						cConditionRollupValue = ConditionType.MEDIUM_ALL.getCode();
						conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
						cConditionType = ConditionCode.MEDIUM_ALL.getCode();
						cConditionValue = "true";
						conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
					}

				} else if(StringUtils.isBlank(assetUse.getDoesAgreedPrintRunIncludeEBooks()) || assetUse.getDoesAgreedPrintRunIncludeEBooks().toLowerCase().equals("no")){
					if(StringUtils.isNotBlank(assetUse.getMediaLimitations())) {
						if(assetUse.getMediaLimitations().equalsIgnoreCase("All Physical and Electronic Media (but not Future Types)")) {
							cConditionType = ConditionCode.MEDIUM.getCode();
							cConditionRollupValue = ConditionType.MEDIUM_PHYSICAL_ELECTRONIC.getCode();
							conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
							cConditionType = ConditionCode.MEDIUM_PHYSICAL_ELECTRONIC.getCode();
							cConditionValue = "true";
							conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
						}
						else if(assetUse.getMediaLimitations().equalsIgnoreCase("All Physical Media including Print and CD")) {
							cConditionType = ConditionCode.MEDIUM.getCode();
							cConditionRollupValue = ConditionType.MEDIUM_ALL_PHYSICAL.getCode();
							conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
							cConditionType = ConditionCode.MEDIUM_ALL_PHYSICAL.getCode();
							cConditionValue = "true";
							conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
						}
						else if(assetUse.getMediaLimitations().equalsIgnoreCase("Print Only")) {
							cConditionType = ConditionCode.MEDIUM.getCode();
							cConditionRollupValue = ConditionType.MEDIUM_PRINT_ONLY.getCode();
							conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
							cConditionType = ConditionCode.MEDIUM_PRINT_ONLY.getCode();
							cConditionValue = "true";
							conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
						}
					}
					else {
						cConditionType = ConditionCode.MEDIUM.getCode();
						cConditionRollupValue = ConditionType.MEDIUM_NO_MENTION.getCode();
						conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
						cConditionType = ConditionCode.MEDIUM_NO_MENTION.getCode();
						cConditionValue = "true";
						conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
					}
				}
			}

			//Second Condition check for What distribution or sales territory rights are granted? -- good
			if(node.getCode().equals(ConditionType.SALES_TERRITORY.getCode())){

				if(StringUtils.isBlank(assetUse.getGrantedWorldSalesTerritory())) {
					cConditionType = ConditionCode.SALES_TERRITORY.getCode();
					cConditionRollupValue = ConditionType.SALES_NO_MENTION.getCode();
					conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
					cConditionType = ConditionCode.SALES_NO_MENTION.getCode();
					cConditionValue = "true";
					conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
				}
				else if(assetUse.getGrantedWorldSalesTerritory().equalsIgnoreCase("yes")){
					cConditionType = ConditionCode.SALES_TERRITORY.getCode();
					cConditionRollupValue = ConditionType.SALES_WORLD_ALIAS.getCode();
					conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
					cConditionType = ConditionCode.SALES_WORLD_ALIAS.getCode();
					cConditionValue = "true";
					conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
				}
				else if(assetUse.getGrantedWorldSalesTerritory().equalsIgnoreCase("no")){
					if(StringUtils.isNotBlank(assetUse.getSalesTerritoryLimitation())) {
						tokenStrings = assetUse.getSalesTerritoryLimitation().split(",");
						for(String tokenString : tokenStrings) {
							tempStr = tokenString.replace('\u00A0',' ').trim();//this is required since spread sheet cell can provide non breaking white space
							if(tempStr.equalsIgnoreCase("North America (ALL)")) {
								cConditionType = ConditionCode.SALES_WORLD.getCode();
								cConditionRollupValue = ConditionCode.SALES_NORTH_AMERICA.getCode();
								cConditionValue = "false";
								conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
								cConditionType = ConditionCode.SALES_NORTH_AMERICA.getCode();
								cConditionValue = "true";
								conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
							}
							else if(tempStr.endsWith("(ALL)")) {
								tempStr = tempStr.substring(0, tempStr.indexOf("(ALL)")).trim();
								ct = getConditionTypeByDescription ( conditionTypes, tempStr);
								cConditionType = ct.getParentCode();
								cConditionRollupValue = ct.getCode();
								cConditionValue = "false";
								conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
								cConditionType = ct.getCode();
								cConditionValue = "true";
								conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
							}
							else {
								ct = getConditionTypeByDescription ( conditionTypes, tempStr);
								cConditionType = ct.getParentCode();
								cConditionRollupValue = ct.getCode();
								cConditionValue = "false";
								conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
								cConditionType = ct.getCode();
								cConditionValue = "true";
								conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
							}
						}
					}
				}
			}

			//third Condition check for What language rights are granted? -- good
			if(node.getCode().equals(ConditionType.LANGUAGE.getCode())){

				if (StringUtils.isBlank(assetUse.getAllLanguagesGranted())) {
					cConditionType = ConditionCode.LANGUAGE.getCode();
					cConditionRollupValue = ConditionType.LANGUAGE_NO_MENTION.getCode();
					conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
					cConditionType = ConditionCode.LANGUAGE_NO_MENTION.getCode();
					cConditionValue = "true";
					conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
				}
				else if (assetUse.getAllLanguagesGranted().equalsIgnoreCase("yes")) {
					cConditionType = ConditionCode.LANGUAGE.getCode();
					cConditionRollupValue = ConditionType.LANGUAGE_ALL_ALIAS.getCode();
					conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
					cConditionType = ConditionCode.LANGUAGE_ALL_ALIAS.getCode();
					cConditionValue = "true";
					conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
				}
				else if (assetUse.getAllLanguagesGranted().equalsIgnoreCase("no")) {
					if(StringUtils.isNotBlank(assetUse.getLanguageLimitation())) {
						tokenStrings = assetUse.getLanguageLimitation().split(",");
						for(String tokenString : tokenStrings) {
							if(tokenString.trim().equalsIgnoreCase("All Languages") || tokenString.trim().equalsIgnoreCase("English only")) {
								ct = getConditionTypeByDescription ( conditionTypes, tokenString.trim());
								cConditionType = ct.getParentCode();
								cConditionRollupValue = ct.getCode();
								conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
								cConditionType = ct.getCode();
								cConditionValue = "true";
								conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
							}
							else {
								ct = getConditionTypeByDescription ( conditionTypes, tokenString.trim());
								cConditionType = ct.getParentCode();
								cConditionRollupValue = ct.getCode();
								cConditionValue = "false";
								conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
								cConditionType = ct.getCode();
								cConditionValue = "true";
								conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
							}
						}
					}
					else {
						cConditionType = ConditionCode.LANGUAGE.getCode();
						cConditionRollupValue = ConditionType.LANGUAGE_NO_MENTION.getCode();
						conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
						cConditionType = ConditionCode.LANGUAGE_NO_MENTION.getCode();
						cConditionValue = "true";
						conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
					}
				}
			}

			//fourth Condition check for Does the grant/letter include any reference to print run? -- good
			if(node.getCode().equals(ConditionType.PRINT_RUN.getCode())){
				if(null != assetUse.getUnlimitedPrintRunCondition()){
					if(assetUse.getUnlimitedPrintRunCondition().equals("Yes")){
						cConditionType = ConditionCode.PRINT_RUN.getCode();
						cConditionRollupValue = ConditionType.PRINT_RUN_UNLIMITED.getCode();
						conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
						cConditionType = ConditionCode.PRINT_RUN_UNLIMITED.getCode();
						cConditionValue = "true";
						conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
					}
					else{
						int printRun = 0; //new Integer(assetUse.getAgreedPrintRun());
						if(StringUtils.isNotBlank(assetUse.getAgreedPrintRun())) {
							printRun = new Integer(assetUse.getAgreedPrintRun());
						}
						if(printRun > 0){
							cConditionType = ConditionCode.PRINT_RUN.getCode();
							cConditionRollupValue = "="+assetUse.getAgreedPrintRun();
							conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
							cConditionType = ConditionCode.PRINT_RUN_LIMIT_BOX.getCode();
							cConditionValue = assetUse.getAgreedPrintRun();
							conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
						}else{
							cConditionType = ConditionCode.PRINT_RUN.getCode();
							cConditionRollupValue = ConditionType.PRINT_RUN_NO_MENTION.getCode();
							conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
							cConditionType = ConditionCode.PRINT_RUN_NO_MENTION.getCode();
							cConditionValue = "true";
							conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
						}
					}
				}
				else {
					cConditionType = ConditionCode.PRINT_RUN.getCode();
					cConditionRollupValue = ConditionType.PRINT_RUN_NO_MENTION.getCode();
					conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
					cConditionType = ConditionCode.PRINT_RUN_NO_MENTION.getCode();
					cConditionValue = "true";
					conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
				}
			}

			//fifth Condition check for Does the grant/letter specify use in particular editions? -- done
			if(node.getCode().equals(ConditionType.EDITION.getCode())){
				if (StringUtils.isBlank(assetUse.getClearedForAllFutureEditions())) {
					cConditionType = ConditionCode.EDITION.getCode();
					cConditionRollupValue = ConditionType.EDITION_NO_MENTION.getCode();
					conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
					cConditionType = ConditionCode.EDITION_NO_MENTION.getCode();
					cConditionValue = "true";
					conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
				}
				else if(assetUse.getClearedForAllFutureEditions().equalsIgnoreCase("yes")) {
					cConditionType = ConditionCode.EDITION.getCode();
					cConditionRollupValue = ConditionType.EDITION_FUTURE.getCode();
					conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
					cConditionType = ConditionCode.EDITION_FUTURE.getCode();
					cConditionValue = "true";
					conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
				}
				else if(assetUse.getClearedForAllFutureEditions().equalsIgnoreCase("no")) {
					if(StringUtils.isNotBlank(assetUse.getEditionLimitation())) {
						cConditionType = ConditionCode.EDITION.getCode();
						cConditionRollupValue = ConditionType.EDITION_THIS.getCode();
						conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
						cConditionType = ConditionCode.EDITION_THIS.getCode();
						cConditionValue = "true";
						conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
					}
					else {
						cConditionType = ConditionCode.EDITION.getCode();
						cConditionRollupValue = ConditionType.EDITION_NO_MENTION.getCode();
						conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
						cConditionType = ConditionCode.EDITION_NO_MENTION.getCode();
						cConditionValue = "true";
						conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
					}
				}
			}

			//sixth Condition check for Does the grant cover use of the asset(s) in derivative works? -- done
			if(node.getCode().equals(ConditionType.DERIVATIVE_WORKS.getCode())){
				if (StringUtils.isBlank(assetUse.getDoesDerivativeCusRightsGranted())) {
					cConditionType = ConditionCode.DERIVATIVE_WORKS.getCode();
					cConditionRollupValue = ConditionType.DERIVATIVE_WORKS_NO_MENTION.getCode();
					conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
					cConditionType = ConditionCode.DERIVATIVE_WORKS_NO_MENTION.getCode();
					cConditionValue = "true";
					conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
				}
				else if(assetUse.getDoesDerivativeCusRightsGranted().equalsIgnoreCase("yes")) {
					cConditionType = ConditionCode.DERIVATIVE_WORKS.getCode();
					cConditionRollupValue = ConditionType.DERIVATIVE_WORKS_ALL.getCode();
					conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
					cConditionType = ConditionCode.DERIVATIVE_WORKS_ALL.getCode();
					cConditionValue = "true";
					conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
				}
				else if(assetUse.getDoesDerivativeCusRightsGranted().equalsIgnoreCase("no")) {
					if(StringUtils.isNotBlank(assetUse.getCustomOrDerivative())) {
						if(assetUse.getCustomOrDerivative().equalsIgnoreCase("No Derivative or Custom")) {
							cConditionType = ConditionCode.DERIVATIVE_WORKS.getCode();
							cConditionRollupValue = ConditionType.DERIVATIVE_WORKS_MAIN_ONLY.getCode();
							conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
							cConditionType = ConditionCode.DERIVATIVE_WORKS_MAIN_ONLY.getCode();
							cConditionValue = "true";
							conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
						}
						else if(assetUse.getCustomOrDerivative().equalsIgnoreCase("No Custom")) {
							cConditionType = ConditionCode.DERIVATIVE_WORKS.getCode();
							cConditionRollupValue = ConditionType.DERIVATIVE_WORKS_ANC_AND_DERIV.getCode();
							conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
							cConditionType = ConditionCode.DERIVATIVE_WORKS_ANC_AND_DERIV.getCode();
							cConditionValue = "true";
							conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
						}
						else {
							cConditionType = ConditionCode.DERIVATIVE_WORKS.getCode();
							cConditionRollupValue = ConditionType.DERIVATIVE_WORKS_NO_MENTION.getCode();
							conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
							cConditionType = ConditionCode.DERIVATIVE_WORKS_NO_MENTION.getCode();
							cConditionValue = "true";
							conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
						}

					}
				}
			}

			//seventh Condition check for Are sub-licensing rights granted? -- good
			if(node.getCode().equals(ConditionType.SUBLICENSE.getCode())){
				if (StringUtils.isBlank(assetUse.getDoesSublicensingGranted())) {
					cConditionType = ConditionCode.SUBLICENSE.getCode();
					cConditionRollupValue = ConditionType.SUBLICENSE_NO_MENTION.getCode();
					conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
					cConditionType = ConditionCode.SUBLICENSE_NO_MENTION.getCode();
					cConditionValue = "true";
					conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
				}
				else if(assetUse.getDoesSublicensingGranted().equalsIgnoreCase("yes")) {
					cConditionType = ConditionCode.SUBLICENSE.getCode();
					cConditionRollupValue = ConditionType.SUBLICENSE_RIGHT.getCode();
					conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
					cConditionType = ConditionCode.SUBLICENSE_RIGHT.getCode();
					cConditionValue = "true";
					conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
				}
				else if(assetUse.getDoesSublicensingGranted().equalsIgnoreCase("no")) {
					cConditionType = ConditionCode.SUBLICENSE.getCode();
					cConditionRollupValue = ConditionType.SUBLICENSE_NO_RIGHT.getCode();
					conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
					cConditionType = ConditionCode.SUBLICENSE_NO_RIGHT.getCode();
					cConditionValue = "true";
					conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
				}
			}

			//Eighth Condition check for Other restrictions and permission notes
			/*if(node.getCode().equals(ConditionType.OTHER.getCode())){

				if(assetUse.getPrintRunCondition().equals("Yes")){

				}
			}*/
		}

	//	if (!isPermissionForm)
	//		throw new RuntimeException("Must revise this condition section before using");


		/*
		for (ConditionNode node : conditionNodes) {
			String cConditionType = node.getCode();
			String cConditionRollupValue = node.getValue();
			String cConditionValue = node.getRollupValue();

			if (node.getCode().equals(ConditionType.PRINT_RUN_EBOOK.getCode())) {
				if (null != assetUse.getEbookPrintRunCondition() && assetUse.getEbookPrintRunCondition().trim().compareTo("null") != 0) {
					String printRunValue = assetUse.getEbookPrintRunCondition().trim().replace(",", "");
					cConditionRollupValue = printRunValue;
					cConditionValue = printRunValue;
					conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
				}
			}

			if (node.getCode().equals(ConditionType.PRINT_RUN.getCode())) {
				if (null != assetUse.getPrintRunCondition()) {
					String printRunValue = " ";
					if (null != assetUse.getPrintRunCondition()) {
						printRunValue = assetUse.getPrintRunCondition().trim().replace(",", "");
						cConditionValue = printRunValue;
					}
					boolean unlimited = false;
					if (ExtendedAssetUse.getBoolean (assetUse.getUnlimitedPrintRunCondition())) {
						printRunValue = "Unlimited Print Run";
						unlimited = true;
					}
					cConditionRollupValue = printRunValue;
					if (!unlimited && printRunValue.equals(" ")) {
						cConditionValue = node.getValue();
					}
					conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
				}
			}

			if (node.getCode().equals(ConditionType.LANGUAGE.getCode())) {
				if (null != assetUse.getLanguageCondition()) {
					cConditionRollupValue = node.getRollupValue();
					cConditionValue = assetUse.getLanguageCondition();

					if (assetUse.getLanguageCondition().toLowerCase().equals("all languages")) {
						// do nothing when all selected
					}
					if (assetUse.getLanguageCondition().toLowerCase().equals("english")) {
						conditionNodes = conditionRepository.updateConditionNodeData(ConditionType.LANGUAGE_ENGLISH.getCode(), "true", null, conditionNodes);

						// first set the language parent condition
						cConditionValue = "English";
						cConditionRollupValue = "English";
						conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
						// now set the English child condition
						cConditionType = ConditionType.LANGUAGE_ENGLISH.getCode();
						cConditionValue = "true";
						cConditionRollupValue = null;
					}
				}
			}

			if (node.getCode().equals(ConditionType.SALES_TERRITORY.getCode())) {
				if (null != assetUse.getSalesTerritoryCondition()) {
					if (assetUse.getSalesTerritoryCondition().toLowerCase().equals("world")) {
						// do nothing when all selected
					}

					if (assetUse.getSalesTerritoryCondition().toLowerCase().equals("north america")) {
						cConditionType = "sales_north_america";
						cConditionValue = "true";
					}

					// when territory is other, we need to look at the territory comments to see if we find what countries
					// to include
					if (assetUse.getSalesTerritoryCondition().toLowerCase().equals("other")) {
						if (null != assetUse.getTerritoryComments() &&
						        assetUse.getTerritoryComments().toLowerCase().indexOf("english language countries") > -1) {
							node.setChildren(conditionRepository.checkUncheckNode("sales_au", node.getChildren(), true));
							node.setChildren(conditionRepository.checkUncheckNode("sales_australia", node.getChildren(), true));
							node.setChildren(conditionRepository.checkUncheckNode("sales_ca", node.getChildren(), true));
							node.setChildren(conditionRepository.checkUncheckNode("sales_uk", node.getChildren(), true));
							node.setChildren(conditionRepository.checkUncheckNode("sales_uk-e", node.getChildren(), true));
							node.setChildren(conditionRepository.checkUncheckNode("sales_uk-ni", node.getChildren(), true));
							node.setChildren(conditionRepository.checkUncheckNode("sales_uk-s", node.getChildren(), true));
							node.setChildren(conditionRepository.checkUncheckNode("sales_uk-w", node.getChildren(), true));
							node.setChildren(conditionRepository.checkUncheckNode("sales_us", node.getChildren(), true));
						}

						if (null != assetUse.getTerritoryComments() && (
						        assetUse.getTerritoryComments().indexOf("U.S. only") > -1 ||
						                assetUse.getTerritoryComments().indexOf("United States") > -1 ||
						        assetUse.getTerritoryComments().indexOf("USA") > -1)) {
							node.setChildren(conditionRepository.checkUncheckNode("sales_us", node.getChildren(), true));
						}

						if (null != assetUse.getTerritoryComments() && (assetUse.getTerritoryComments().indexOf("Philippines") > -1)) {
							node.setChildren(conditionRepository.checkUncheckNode("sales_ph", node.getChildren(), true));
						}

						if (null != assetUse.getTerritoryComments() && assetUse.getTerritoryComments().toLowerCase().indexOf("excluding german") > -1) {
							conditionNodes = conditionRepository.updateConditionNodeData("sales_europe", "true", null, conditionNodes);
							node.setChildren(conditionRepository.checkUncheckNode("sales_de", node.getChildren(), false));
							node.setChildren(conditionRepository.checkUncheckNode("sales_ch", node.getChildren(), false));
							node.setChildren(conditionRepository.checkUncheckNode("sales_at", node.getChildren(), false));
						}
					}
				}
				conditionNodes = conditionRepository.updateConditionNodeData(cConditionType, cConditionValue, cConditionRollupValue, conditionNodes);
			}

			if (node.getCode().equals(ConditionType.MEDIUM.getCode())) {
				if (ExtendedAssetUse.getBoolean (assetUse.getPrintRunIncludeEbooks())) {
					// do not select anything because by default this means all selections
				} else {
					conditionNodes = conditionRepository.checkUncheckNode("medium_all_physical", conditionNodes, true);
				}
			}
		}
	*/
		return conditionNodes;
	}

	private ConditionType getConditionTypeByDescription (List<ConditionType> conditionTypes, String description) {
		for(ConditionType ct : conditionTypes) {
			if(ct.getDescription().equalsIgnoreCase(description)) {
				return ct;
			}
		}
		return null;
	}

	public SourceRepository getSourceRepository() {
    	return sourceRepository;
    }

	public void setSourceRepository(SourceRepository sourceRepository) {
    	this.sourceRepository = sourceRepository;
    }

	public ConditionRepository getConditionRepository() {
    	return conditionRepository;
    }

	public void setConditionRepository(ConditionRepository conditionRepository) {
    	this.conditionRepository = conditionRepository;
    }

	public AssetUseRepository getAssetUseRepository() {
    	return assetUseRepository;
    }

	public void setAssetUseRepository(AssetUseRepository assetUseRepository) {
    	this.assetUseRepository = assetUseRepository;
    }

	public ImportAssetsUtility getImportAssetsUtility() {
    	return importAssetsUtility;
    }

	public void setImportAssetsUtility(ImportAssetsUtility importAssetsUtility) {
    	this.importAssetsUtility = importAssetsUtility;
    }

	public ContractRepository getContractRepository() {
    	return contractRepository;
    }

	public void setContractRepository(ContractRepository contractRepository) {
    	this.contractRepository = contractRepository;
    }
}
