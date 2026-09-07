package com.wiley.permissions.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.persistence.Query;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.queryparser.classic.ParseException;
import org.mule.api.MuleException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.wiley.permissions.common.bean.BeanUtility;
import com.wiley.permissions.common.dispatcher.DispatcherException;
import com.wiley.permissions.common.dispatcher.OperationType;
import com.wiley.permissions.domain.persistence.permissions.Account;
import com.wiley.permissions.domain.persistence.permissions.Address;
import com.wiley.permissions.domain.persistence.permissions.Amendment;
import com.wiley.permissions.domain.persistence.permissions.Asset;
import com.wiley.permissions.domain.persistence.permissions.AssetBase;
import com.wiley.permissions.domain.persistence.permissions.AssetUse;
import com.wiley.permissions.domain.persistence.permissions.CommonWork;
import com.wiley.permissions.domain.persistence.permissions.CompCopy;
import com.wiley.permissions.domain.persistence.permissions.ConditionNode;
import com.wiley.permissions.domain.persistence.permissions.Contract;
import com.wiley.permissions.domain.persistence.permissions.Contract.PaymentType;
import com.wiley.permissions.domain.persistence.permissions.ContractAsset;
import com.wiley.permissions.domain.persistence.permissions.ContractList;
import com.wiley.permissions.domain.persistence.permissions.CopyrightType;
import com.wiley.permissions.domain.persistence.permissions.GeographicalLocation;
import com.wiley.permissions.domain.persistence.permissions.ImportSource;
import com.wiley.permissions.domain.persistence.permissions.OwnerType;
import com.wiley.permissions.domain.persistence.permissions.PaymentRequest;
import com.wiley.permissions.domain.persistence.permissions.PermissionStatus;
import com.wiley.permissions.domain.persistence.permissions.Product;
import com.wiley.permissions.domain.persistence.permissions.PurchaseOrder;
import com.wiley.permissions.domain.persistence.permissions.Source;
import com.wiley.permissions.domain.persistence.permissions.Usage;
import com.wiley.permissions.domain.persistence.permissions.UsageConditionSize;
import com.wiley.permissions.domain.rightslink.License;
import com.wiley.permissions.persistence.PersistenceException;
import com.wiley.permissions.repositories.AssetRepository;
import com.wiley.permissions.repositories.AssetUseRepository;
import com.wiley.permissions.repositories.ConditionRepository;
import com.wiley.permissions.repositories.ContractRepository;
import com.wiley.permissions.repositories.ProductRepository;
import com.wiley.permissions.repositories.SourceRepository;
import com.wiley.permissions.security.web.PermissionsSecurityException;
import com.wiley.sf.common.lang.ArgUtil;
import com.wiley.sf.common.monitor.PerformanceMonitor.PerfTimer;

/**
 *
 */
public class ContractServiceImpl extends BaseService implements ContractService {
	private static final Log log = LogFactory.getLog(ContractServiceImpl.class);

	private String smtpServer;

	private int daysBeforeExpire;

	private ContractRepository contractRepository;
	private AssetUseRepository assetUseRepository;
	private AssetRepository assetRepository;
	private ConditionRepository conditionRepository;
	private ProductRepository productRepository;
	private CommonWorkService commonWorkService;
	private AssetUseService assetUseService;
	private AssetUseIndexService assetUseIndexService;
	private SourceService sourceService;
	private SourceRepository sourceRepository;


	/**
	 * Saves an amendment and recalculates the statuses of assets attached to the contract
	 * attached to the amendment
	 */
	@Override
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
	public Amendment save(Amendment amendment) throws Exception
	{
		Amendment am = contractRepository.saveRequiresNew(amendment); // REQUIRES_NEW

		contractRepository.saveRequiresNew(amendment.getContract()); // REQUIRES_NEW

		updateStatusForContract(amendment.getContract().getId(), null);

		return am;
	}

	// Convenience method
	@Override
	public Contract save(Contract contract, List<ConditionNode> conditions,
			Map<Integer, List<UsageConditionSize>> usageSizeMap, List<Integer> returnRemovedAssetIds)
	throws Exception
	{
		return save(contract, conditions, usageSizeMap, returnRemovedAssetIds, true);
	}

	/**
	 * smarkoff: The comments right below here are rather old and I think not very accurate anymore.
	 *
	 * This method is different then createUpdateContract because the asset list is passed as parameter
	 * (might be passed in Contract object later on), as well as the conditions.
	 * Will not initialize with the default unused assets or default conditions
	 *
	 * This method might need more work for update action: delete the assets and conditions, then add them again
	 * Maybe rename it then to save
	 *
	 * @param contract  Must be non-null
	 * @param conditions  May be null
	 * @param usageSizeMap  May be null
	 * @param returnRemovedAssetIds  May be null
	 */
	// @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
	// DO NOT ADD TRANSACTION ON THIS METHOD - will create a deadlock in the database
	@Override
	public Contract save(Contract contract, List<ConditionNode> conditions,
			Map<Integer, List<UsageConditionSize>> usageSizeMap, List<Integer> returnRemovedAssetIds, boolean updateStatus)
	throws Exception
	{
		List<CompCopy> compCopies = contract.getCompCopies();
		List<ContractAsset> newAssetList = contract.getAssets();
		//Start: Added to implement DM-122
		populateConditionsValuesToContractAssets(newAssetList, conditions);
		//End: Added to implement DM-122

		ArgUtil.notEmpty(newAssetList, "newAssetList");
		// reset the contract assets, so they do not get saved thru merge (it fails on merge)
		contract.setAssets(null);

		contract = contractRepository.saveRequiresNew(contract); // REQUIRES_NEW

		adjustContractAndOrPODate(contract);

		contract = contractRepository.lazyLoad(Contract.class, contract.getId(), new String[] {"assets", "compCopies", "source"});
		log.debug("save(): #conditions after reload contract = " + contract.getConditionsNotNull().size());

		compCopies = contract.getCompCopies();

		// if one asset only, save contract price in the asset price (https://www.pivotaltracker.com/story/show/37695281)
		if (newAssetList.size() == 1) {
			ContractAsset firstAsset = newAssetList.get(0);
			firstAsset.setPrice(contract.getPrice());
			newAssetList.get(0).setPrice(contract.getPrice());
		}

		// reconciles the incoming ContractAsset list with the existing list
		List<Integer> removedAssetIds = mergeContractAssets (contract, newAssetList);
		// removedAssetIds will be non-null but may be empty
		for (Integer assetId: removedAssetIds) {
			// need to recalculate the print run on removed assets in order to reset seats and print run info
			try {
				assetRepository.updateTotalSeatsAndPrintRunByAssetId(assetId);  // REQUIRES_NEW, throws Exception
			}
			catch (Exception ex) {
				log.error("save(): caught exception calling updateTotalSeatsAndPrintRunByAssetId() with assetId " + assetId + ": ", ex);
			}
		}

		if (returnRemovedAssetIds != null) {
			returnRemovedAssetIds.addAll(removedAssetIds);
		}

		if (null != conditions) {
			// save the conditions
			conditionRepository.saveContractConditions(contract.getId(), conditions); // REQUIRES_NEW
		}

		for (ContractAsset ca : newAssetList) {

			if(ca.isAssetLevelRFDeal() || (null != ca.getRForRM() && ca.getRForRM().equals("ARF"))) {//Added to implement DM-122
				assetRepository.updateAssetLevelTotalSeatsAndPrintRunByAssetId(ca);  // REQUIRES_NEW
				//Start: Added to implement DM-122
				//Initiate sending mail to the concerned as Asset level RF deal is saved in GPS system.
				if(null == ca.getRForRM() || (ca.getRForRM().equals("ARF"))) {//New asset or old asset now changed to ARF, so send email with details
					if(null != contract.getSource() && null != contract.getSource().getId()) {
						int assetCount = assetRepository.loadAssetLevelRoyaltyFreeDealsInfo(contract.getSource().getId());
						if(assetCount > 1) {//We have to send eMail only if the assetCount is 2 or more in the system.
							assetCount = assetCount - 1;//eMail body should contain the asset count as "assetCount - 1".
							Asset asset = assetRepository.loadAssetById(ca.getAssetBaseId());
							List<Product> products = productRepository.loadProductsByCWId(contract.getCommonWork().getId());
							String isbn13 = null;
							for(Product product : products) {
								if(product.isCwPrimary()) {
									isbn13 = product.getIsbn13();
									break;
								}

							}
							commonWorkService.sendEmailToAdminRFDealReceiver(asset.getDescription(), isbn13, contract.getSource().getName(), assetCount);
						}
					}
				}
				//End: Added to implement DM-122
			} else {
				if(null != ca.getRForRM() && ca.getRForRM().equals("RM")) {
					assetRepository.updateTotalSeatsAndPrintRunByAssetId(ca.getAssetBaseId(), ca);
				} if(null != ca.getRForRM() && ca.getRForRM().equals("SRF")) {
					assetRepository.updateAssetLevelTotalSeatsAndPrintRunByAssetId(ca);
				}
				else {
					assetRepository.updateTotalSeatsAndPrintRunByAssetId(ca.getAssetBaseId());  // REQUIRES_NEW
				}

			}

			Asset asset = assetRepository.find(Asset.class, ca.getAssetBaseId());
			log.debug("FINALLY  "+asset.isManaged());
			if (contract.getCopyrightType() != null) {
				asset.setCopyrightType(contract.getCopyrightType());

				if (CopyrightType.WILEY_OWNED_WORK_FOR_HIRE.equals(contract.getCopyrightType())
					|| CopyrightType.WILEY_OWNED_CTR.equals(contract.getCopyrightType())) {
					asset.setOwnerType(OwnerType.WILEY);
				}

				//Added for Paperwork task C Public Domain Starts
				log.debug("Before FInally in "+asset.getOwnerType().getCode());
			    if(StringUtils.isNotBlank(asset.getOwnerType().toString())){
					if (asset.getOwnerType().getCode().equals("Public Domain")) {
							asset.setManaged(false);
					}
			    }
			    log.debug("After FInally in "+asset.getOwnerType().getCode());
			   //PaperWork task C public Domain Ends

				// double saves the asset but there is no way to over pass it now
				assetRepository.saveRequiresNew(asset); // REQUIRES_NEW
			}
		}

		// the only time the usageSizeMap should be null is when this method is called from ImportAssets
		if (usageSizeMap != null) {
			conditionRepository.saveUsageSizeMap(contract.getId(), usageSizeMap);
		}

		// send the compCopy request
		for (CompCopy compCopy : compCopies) {
			if (StringUtils.isBlank(compCopy.getOrderNum())) {
				compCopy.setContract(contract);
				CompCopy requestedCompCopy = requestCompCopyOrder(compCopy);

		//		if (null == compCopy.getId()) {
		//			compCopy = getContractRepository().saveRequiresNew(compCopy);
		//		}

				BeanUtility.merge(requestedCompCopy, compCopy);

				contractRepository.save(compCopy);  // REQUIRED
	//			compCopy = getContractRepository().saveRequiresNew(compCopy);
			}
		}

		if (updateStatus) {
			updateStatusForContract(contract.getId(), removedAssetIds);
		}

		return contract;
	}

	//Start: Added to implement DM-122
	private void populateConditionsValuesToContractAssets(List<ContractAsset> newAssetList, List<ConditionNode> conditions) {
		String conditionRollupValue = null;
		//ConditionNode conditionToRemove = null;
		for (ContractAsset ca : newAssetList) {
			if(ca.isAssetLevelRFDeal() || (null != ca.getRForRM() && ca.getRForRM().equals("ARF"))) {//Updated for DM-123
				for (ConditionNode condition : conditions) {
					conditionRollupValue = condition.getRollupValue();
					if (!StringUtils.isEmpty(conditionRollupValue)) {
						conditionRollupValue = conditionRollupValue.trim();
						if ("Does the grant/letter include any reference to print run?".equals(condition.getDescription().trim())) {
							if(conditionRollupValue.startsWith("=")) {
								//Assign the value to print run - value comes as =10 for 10
								try {
									int totalPrintRun = Integer.parseInt(conditionRollupValue.substring(1));
									ca.setTotalPrintRun(totalPrintRun);
								} catch (NumberFormatException ex) {
									//can be ignored
								}

							} else {
								ca.setTotalPrintRun(0);
							}
						} else if ("Are sublicensing rights granted?".equals(condition.getDescription().trim())) {
							if("Wiley can include the asset(s) when sub-licensing product".equals(conditionRollupValue)) {
								ca.setSublicence(true);
							} else {
								ca.setSublicence(false);
							}
						}
						else if ("Does the grant/letter include any reference to Seats?".equals(condition.getDescription().trim())) {
							ca.setAssetLevelRFDeal(true);
							conditionRollupValue = conditionRollupValue.trim();
							if("Unlimited Seats are granted".equals(conditionRollupValue)) {
								//Assign contract asset seat value to unlimited. i.e. 0 (zero)
								ca.setSeats(0);
								ca.setGrantType("Unlimited");
							} else if("Seats limitation is not mentioned".equals(conditionRollupValue)) {
								ca.setSeats(0);
								ca.setGrantType("Unlimited - Not mentioned");
							} else {
								if(conditionRollupValue.startsWith("=")) {
									//Assign the value to seats - value comes as =10 for 10 seat limitation
									try {
										int seats = Integer.parseInt(conditionRollupValue.substring(1));
										ca.setSeats(seats);
										ca.setGrantType(null);
									} catch (NumberFormatException ex) {
										//can be ignored
									}
								}
							}
						}
					}
				}
			} else if(null != ca.getRForRM() && ca.getRForRM().equals("RM")) {
				ca.setAssetLevelRFDeal(false);
				ca.setSeats(0);
				ca.setRoyaltyFreeDealId(0);
				ca.setRoyaltyFreeDeal(null);
				ca.setGrantType(null);
				for (ConditionNode condition : conditions) {
					conditionRollupValue = condition.getRollupValue();
					if (!StringUtils.isEmpty(conditionRollupValue)) {
						conditionRollupValue = conditionRollupValue.trim();
						if ("Does the grant/letter include any reference to print run?".equals(condition.getDescription().trim())) {
							//ca.setTotalPrintRun(null);//set it to default null as it is now RM, so this value is not required to capture in this table
							if(conditionRollupValue.startsWith("=")) {
								//Assign the value to print run - value comes as =10 for 10
								try {
									int totalPrintRun = Integer.parseInt(conditionRollupValue.substring(1));
									ca.setTotalPrintRun(totalPrintRun);
								} catch (NumberFormatException ex) {
									//can be ignored
								}

							} else {
								ca.setTotalPrintRun(0);
							}
						} else if ("Are sublicensing rights granted?".equals(condition.getDescription().trim())) {
							//ca.setSublicence(false);//set it to default false as it is now RM, so this value is not required to capture here
							if("Wiley can include the asset(s) when sub-licensing product".equals(conditionRollupValue)) {
								ca.setSublicence(true);
							} else {
								ca.setSublicence(false);
							}
						}
						else if ("Does the grant/letter include any reference to Seats?".equals(condition.getDescription().trim())) {
							ca.setSeats(0);
							condition.setRollupValue(null);
							for(ConditionNode node : condition.getChildren()) {
								node.setValue(null);
								node.setRollupValue(null);
								if(StringUtils.isNotBlank(node.getDescription()) && "Maximum Seats allowed".equals(node.getDescription())) {
									node.getChildren().get(0).setValue(null);
								}
				        	}
							condition.setValue(null);
						}
					}
					if ("Does the grant/letter include any reference to Seats?".equals(condition.getDescription().trim())) {//Added for DM-123
						//set seats to default value and remove the condition node
						ca.setSeats(0);
						condition.setRollupValue(null);
						for(ConditionNode node : condition.getChildren()) {
							node.setValue(null);
							node.setRollupValue(null);
							if(StringUtils.isNotBlank(node.getDescription()) && "Maximum Seats allowed".equals(node.getDescription())) {
								node.getChildren().get(0).setValue(null);
							}
			        	}
						condition.setValue(null);
					}
				}
			} else {
				//We need to set all default values because we opted out of Asset level RF
				ca.setAssetLevelRFDeal(false);
				ca.setTotalPrintRun(0);
				ca.setSublicence(false);
				ca.setSeats(0);
				ca.setGrantType(null);
				for (ConditionNode condition : conditions) {//don't need to do anything. in later stage we set the RF deal id and corresponding terms picked from RF deal
					if ("Does the grant/letter include any reference to Seats?".equals(condition.getDescription().trim())) {//Added for DM-123
						condition.setRollupValue(null);
						for(ConditionNode node : condition.getChildren()) {
							node.setValue(null);
							node.setRollupValue(null);
							if(StringUtils.isNotBlank(node.getDescription()) && "Maximum Seats allowed".equals(node.getDescription())) {
								node.getChildren().get(0).setValue(null);
							}
			        	}
						condition.setValue(null);
					}
				}
			}
		}
	}
	//End: Added to implement DM-122

	private void adjustContractAndOrPODate(Contract contract) throws PersistenceException {
		// First, if the contract date does not contain time, then use the current time.
		// Adding the current time (regardless of the date) ensures that for two Contracts
		// created with the same date, the one created later will have a later date.
		Date cDate = contract.getDate();
		Calendar cCal = Calendar.getInstance();
		cCal.setTime(cDate);
		if (cCal.get(Calendar.HOUR_OF_DAY) == 0 && cCal.get(Calendar.MINUTE) == 0 && cCal.get(Calendar.SECOND) == 0) {
			Calendar today = Calendar.getInstance();
 			cCal.set(Calendar.HOUR_OF_DAY, today.get(Calendar.HOUR_OF_DAY));
 			cCal.set(Calendar.MINUTE, today.get(Calendar.MINUTE));
 			cCal.set(Calendar.SECOND, today.get(Calendar.SECOND));
     		contract.setDate(cCal.getTime());
		}

    	// If there is a PO that this contract is based on, check if the contract date is older
    	// than the PO date - if so, change the PO date to be the same as the Contract date.
    	if (contract.getPurchaseOrder() != null) {
    		PurchaseOrder po = contract.getPurchaseOrder();
    		if (po.getDate().after(contract.getDate())) {
    			po.setDate(contract.getDate());
    			contractRepository.saveRequiresNew(po);  // throws PersistenceException
    		}
    	}
	}

	/**
	 * Will reconcile the incoming ContractAsset list with the existing ContractAsset list.
	 * Returns a list of asset ids removed from the contract (which is guaranteed to be non-null but may be empty).
	 *
	 * @param contract
	 * @param newAssetList
	 * @throws PersistenceException
	 */
	private List<Integer> mergeContractAssets(Contract contract, List<ContractAsset> newAssetList) throws PersistenceException {
		log.debug("mergeContractAssets(): newAssetList:\r\n" + StringUtils.join(newAssetList, "\r\n"));

		Map<ContractAsset, Integer> mergeAssets = new HashMap<ContractAsset, Integer>();
		// code = 1 it is new, 2 it is old, 3 is removed
		for (ContractAsset asset : newAssetList) {
			mergeAssets.put(asset, 1);
		}

		if (null != contract.getAssets()) {
			for (ContractAsset asset : contract.getAssets()) {
				if (mergeAssets.containsKey(asset)) {
					mergeAssets.put(asset, 2);
				} else {
					mergeAssets.put(asset, 3);
				}
			}
		}

		List<Integer> removedAssetIds = new ArrayList<Integer>();
		for (ContractAsset asset : mergeAssets.keySet()) {
			// it is new
			if (mergeAssets.get(asset) == 1) {
				// set the new loaded entity - do not move this above mergeAssets.get because it changes the asset object and get will fail
				asset.setContract(contract);
				log.debug("mergeContractAssets(): add new asset " + asset);
				asset = contractRepository.saveRequiresNew (asset); // REQUIRES_NEW
				contract.addAsset(asset);
			// it is old
			} else if (mergeAssets.get(asset) == 2) {
				// set the new loaded entity - do not move this above mergeAssets.get because it changes the asset object and get will fail
				asset.setContract(contract);
				log.debug("mergeContractAssets(): existing asset " + asset);
				contractRepository.saveRequiresNew (asset); // REQUIRES_NEW
				// do nothing - it already exists
			// it does not exists anymore
			} else if (mergeAssets.get(asset) == 3) {
				// set the new loaded entity - do not move this above mergeAssets.get because it changes the asset object and get will fail
				asset.setContract(contract);
				log.debug("mergeContractAssets(): remove asset " + asset);
				contractRepository.removeRequiresNew (asset); // REQUIRES_NEW
				contract.removeAsset(asset);
				removedAssetIds.add(asset.getAssetBaseId());
			}
		}

		return removedAssetIds;
	}

	/**
	 * refreshes the asset index for all assets in the contract
	 * @return list of affected asset use id
	 */
	@Override
	public List<Integer> refreshAssetIndex(Integer contractId) throws Exception {
		Contract contract = contractRepository.lazyLoad(Contract.class, contractId,
				new String[] { "assets" });
		List<Integer> auIds = new ArrayList<Integer> ();
		for (Integer assetId : contract.getAssetIds()) {
			List<AssetUse> auList = assetUseRepository.loadAssetUseListByCWIdAssetId (contract.getCommonWork().getId(), assetId);
			for (AssetUse au : auList) {
				assetUseIndexService.updateIndex(au.getId());
				auIds.add(au.getId());
			}
		}
		return auIds;
	}

	/**
	 * @param contractId        Must be valid contract id
	 * @param removedAssetsIds  May be null or empty
	 */
	@Override
	public void updateStatusForContract(int contractId, List<Integer> removedAssetIds) throws Exception {
		log.debug("updateStatusForContract(): called with contractId = " + contractId
				+ ", removedAssetIds = " + StringUtils.join(removedAssetIds, ", "));
		Contract contract = contractRepository.lazyLoad(Contract.class, contractId, new String[] {"assets"});

		// for now ignore AssetGroup since we don't use at this point

		List<ContractAsset> col = contract.getAssets();

		List<Asset> assetCol = new ArrayList<Asset>(col.size());

		for (ContractAsset ca: col) {
			AssetBase ab = ca.getAssetBase();
			// AssetBase is always AssetBase - never an instance of Asset or AssetGroup as I would think
			// - Just put all id's into Asset objects - updatePermissionStatus will reload from
			// id and not use if can't reload (if an AssetGroup instead of an Asset)
			Asset asset = new Asset();
			asset.setId(ab.getId());
			assetCol.add(asset);
		}

		if (removedAssetIds != null) {
			for (Integer assetId : removedAssetIds) {
				Asset asset = new Asset();
				asset.setId(assetId);
				assetCol.add(asset);
			}
		}

		getAssetUseService().updateStatusForAssetCollection (contract.getCommonWork().getId(), assetCol);
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public void removeAssetFromContract(Integer contractId, Integer assetId)
			throws Exception
	{
		Contract contract = contractRepository.loadContractById(contractId);

		contractRepository.removeAssetFromContract (contract, assetId);

		List<AssetUse> auList = assetUseRepository.loadAssetUseListByCWIdAssetId(
				contract.getCommonWork().getId(), assetId);

		getAssetUseService().updateStatusForAssetUseCollection(auList);
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public void addAssetsToContract(Integer contractId, Integer[] assetIds) throws Exception
	{
		Contract contract = contractRepository.loadContractById(contractId);

		ArrayList<Asset>addedList  = contractRepository.addAssetsToContract (contract, assetIds);

		getAssetUseService().updateStatusForAssetCollection (contract.getCommonWork().getId(), addedList);
	}

	@Override
	public CompCopy requestCompCopyOrder(CompCopy origCompCopy)
			throws PermissionsSecurityException, PersistenceException, MuleException, DispatcherException
	{
		CompCopy cc = new CompCopy();

	//	CommonWork cw = origCompCopy.getContract().getCommonWork();
		Integer cwId = origCompCopy.getContract().getCommonWork().getId();
		CommonWork cw = getProductRepository().getCommonWorkRepository().loadByIdForManageAsset(cwId);

		if (null == cw) {
			// throw new ServiceException("product is null");
			log.debug("requestCompCopyOrder() : product is null");
			cc.setSuccessFlag("10");
			cc.setErrorMsg("PrimaryProduct is empty");
			return cc;
		}

		HashMap<String, Object> params = new HashMap<String, Object> ();
		params.put("isbn10", cw.getPrimaryProduct().getIsbn10());

		// address is not loaded using JPA FETCH, because the objects are
		// defined in 2 different persistent units
		// so we load it here from the AddressId
		Integer addressId = origCompCopy.getAddressId();
		Address address = null;
		if (null != addressId) {
			address = getSourceRepository().loadAddressById(addressId);
		}

		if (null == address) {
			log.debug("requestCompCopyOrder() : address is null");
			cc.setSuccessFlag("11");
			cc.setErrorMsg("Address is empty");
			return cc;
		}

		Source source = origCompCopy.getContract().getSource();
		log.debug("Source " + source);

		if (null == source) {
			// throw new ServiceException("source is null");
			log.debug("requestCompCopyOrder() : source is null");
			cc.setSuccessFlag("12");
			cc.setErrorMsg("Source is empty");
			return cc;
		}

		if (null != cw && null != cw.getPrimaryProduct() && null != cw.getPrimaryProduct().getLocation()) {
			if (cw.getPrimaryProduct().getLocation().getCode().compareTo(GeographicalLocation.NY.getCode()) != 0 &&
					cw.getPrimaryProduct().getLocation().getCode().compareTo(GeographicalLocation.AUS.getCode()) != 0) {
				log.debug("requestCompCopyOrder() : product is NOT US or Australia");
				cc.setSuccessFlag("03");
				cc.setErrorMsg("Product is NOT a US or AU product - You need to manually fulfill it");
				return cc;
			}
		}

		params.put("fname", "");
		// just send source name as lname
		params.put("lname", source.getName());
		params.put("street1", StringUtils.defaultIfEmpty (address.getLineOne(), ""));
		params.put("street2", StringUtils.defaultIfEmpty (address.getLineTwo(), ""));
		params.put("city", StringUtils.defaultIfEmpty (address.getCity(), ""));
		params.put("state", StringUtils.defaultIfEmpty (address.getProvince(), ""));

		if (null == address.getCountry()) {
			params.put("countrycode", "");
		}
		else {
			params.put("countrycode", StringUtils.defaultIfEmpty (address.getCountry().getCode(), ""));
		}
		params.put("postcode", StringUtils.defaultIfEmpty (address.getPostalCode(), ""));

		// Changed the if condition as equals to Australia code from not equals as the GET_COMP_AU_COPY calls the Australia website.


		//AUS comcopy starts
		/***
		if (cw.getPrimaryProduct().getLocation().getCode().compareTo(GeographicalLocation.AUS.getCode()) == 0	) {
			String account13 = origCompCopy.getContract().getSource().getJdeVendorNumber() + "0000";
			params.put("acctno", account13);

	 **/
			if (cw.getPrimaryProduct().getLocation().getCode().compareTo(GeographicalLocation.AUS.getCode()) == 0	) {
				String account=origCompCopy.getContract().getSource().getJdeVendorNumber();
				if(account != null)
				{
					log.debug("Account Number is" + account);
				}
				if(account != null && account.length() > 6)
				{
					log.debug("affter checking account length");
					String account13="01"+ origCompCopy.getContract().getSource().getJdeVendorNumber()+"0000";
					log.debug("13 Digit Account Number is"+account13);
					params.put("acctno", account13);
					return (CompCopy) getServiceDispatcher().send (OperationType.GET_COMP_AU_COPY, params, null);
				}
				else
				{
					log.debug("Account number is not valid"+ origCompCopy.getContract().getSource().getJdeVendorNumber());
					return null;
				}
				// AUS compcopy ends here

			/* Commenting out the addition of purchase order id as a comp copy is not related to any purchase order
			 *  Also removed the poId from the "CompCopyAuService" in the services-mule-config.xml
			 *  To add it back, we can uncomment the below code and also uncomment the CompCopyAuService
			 *  in the services-mule-config.xml
			 */

			/*String poId = "N/A";
			if (null != origCompCopy.getContract() || null != origCompCopy.getContract().getPurchaseOrder()) {
				log.debug("in ContractServiceImpl if part");
				poId = "po_" + origCompCopy.getContract().getPurchaseOrder().getId();
			} else {
				log.debug("in ContractServiceImpl else part");
				poId = "N/A";
			}
			log.debug("in ContractServiceImpl......poId "+poId);
			if (poId.length() > 12) {
				poId = poId.substring(0, 11);
			}
			params.put("po", poId);*/
		//AUS compcopy starts
		//	return (CompCopy) getServiceDispatcher().send (OperationType.GET_COMP_AU_COPY, params, null);

		// AUS compcopy ends here
			}

		log.debug("in returnCompcopy.....end");
		return (CompCopy) getServiceDispatcher().send (OperationType.GET_COMP_COPY, params, null);
	}

	/**
	 * @param contractId  0 means create new contract
	 * @param refreshAuIds  Should be passed in empty to be filled (like 2nd return variable)
	 *
	 * Returns list of newly created auIds (also any existing).
	 */
	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public List<Integer> saveLicense(License license, int cwId, List<ConditionNode> conditions,
			int numAssets, int contractId, List<Integer> refreshAuIds) throws Exception
	{
		if (StringUtils.isBlank(license.getSourceName())) {
			throw new RuntimeException("License does not have valid source name (blank)");
		}

		CommonWork commonWork;
		Source source;
		Contract contract;
		List<ContractAsset> contractAssetList;
		List<Integer> newAuIds = new ArrayList<Integer>();

		if (contractId > 0) {  // existing contract
			contract = contractRepository.lazyLoad(Contract.class, contractId,
					new String[] { "assets", "commonWork", "source" });
			commonWork = contract.getCommonWork();
			source = contract.getSource();
			contractAssetList = contract.getAssets();

			for (ContractAsset contractAsset : contractAssetList) {
				List<Integer> list = assetUseRepository.loadAssetUseIdsForAssetId(contractAsset.getAssetBaseId());
				refreshAuIds.addAll(list);
			}
		}
		else {  // create new contract
			commonWork = new CommonWork();
			commonWork.setId(cwId);
			source = sourceRepository.loadSourceByName(license.getSourceName());
			if (source == null) {
				source = new Source();
				source.setName(license.getSourceName());
				source = sourceService.saveSource(source, true);
			}

			contract = new Contract();
			contract.setCommonWork(commonWork);
			contract.setSource(source);
			contractAssetList = new ArrayList<ContractAsset>(numAssets);
			contract.setAssets(contractAssetList);
		}

		license.merge(contract);

		for (int i = contractAssetList.size(); i < numAssets; i++) {
			Asset asset = new Asset();
			asset.setImportSource(ImportSource.FROM_RIGHTS_LINK);
			asset.setOwnerType(OwnerType.THIRD_PARTY);
			List<Source> sourceList = new ArrayList<Source>(1);
			sourceList.add(source);
			asset.setSources(sourceList);
			asset.setDescription(license.getAssetDescription());
			//asset.setCreditLine(license.getCreditLine());  // - put on Contract instead (below)
			asset.setOriginalPublicationTitle(license.getOriginalPublicationTitleExtended());
			asset.setOriginalArticleTitle(license.getOriginalArticleTitle());
			asset.setOriginalArticleAuthor(license.getOriginalArticleAuthor());
			asset.setMediaType(license.getMediaType());

			AssetUse au = new AssetUse();
			au.setImportSource(ImportSource.FROM_RIGHTS_LINK);
			au.setAsset(asset);
			au.setCommonWork(commonWork);
			au.setUsage(Usage.FRONT_COVER);  // for now (cannot be null)
			au = assetUseService.saveAssetUse(au, true, true);
			newAuIds.add(au.getId());

			ContractAsset contractAsset = new ContractAsset();
			contractAsset.setContract(contract);
			contractAsset.setAssetBase(au.getAsset());
			contractAsset.setCreditLine(license.getCreditLine());
			contractAssetList.add(contractAsset);
		}

		contract = save(contract, conditions, null, null);

		return newAuIds;
	}

	/* Not needed
	private static Date licenseDateToObject(String dateString)  {
		if (StringUtils.isBlank(dateString)) return null;

		// Sample data has dates in this format: Sep 02, 2013
		SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy");
		try {
			return dateFormat.parse(dateString);
		}
		catch (java.text.ParseException ex) {
			log.debug("licenseDateToObject(): could not parse [" + dateString + "]");
			return null;
		}
	}*/

	@Override
	public Account getAccountByNumberAndSubCode(String accountNumber, String subCode) throws PersistenceException
	{
		return contractRepository.getAccountByNumberAndSubCode(accountNumber, subCode);
	}

	/** Returns a list of 0 or 1 contracts.
	 * @throws IOException
	 * @throws ParseException */
	@Override
	public ContractList loadLatestForAssetSourceCW(int assetId, int sourceId, int cwId) throws PersistenceException, ParseException, IOException
	{
		return contractRepository.loadLatestForAssetSourceCW(assetId, sourceId, cwId);
	}

	/**
	 * If you all you need is the latest contract, call the above method instead for efficiency.
	 */
	@Override
	public ContractList loadListForAssetSourceCW(int assetId, int sourceId, int cwId) throws PersistenceException, ParseException, IOException
	{
		return contractRepository.loadListForAssetSourceCW(assetId, sourceId, cwId);
	}

	/****************************************************
	 * LOAD VIEW METHODS							    *
	 * (// preload stuff needed by Controller and jsp) 	*
	 ****************************************************/

	/**
	 * ContractList page
	 * @param cwId
	 * @return List<Contract>
	 */
	@Override
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<Contract> loadContractListView(int cwId)
	{
		List<Contract> cList = contractRepository.loadContractsByCWId (cwId);
		for (Contract c : cList) {
			c.getCurrency().getCode();
			c.getCommonWork().getPrimaryProduct();

			for (ContractAsset contractAsset: c.getAssets()) {
				contractAsset.getAssetBase();
			}
		}
		return cList;
	}

	/**
	 * PaymentRequestList page
	 * @param cwId
	 * @return List<PaymentRequest>
	 */
	@Override
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<PaymentRequest> loadPaymentRequestListView(int cwId)
	{
		List<PaymentRequest> pList = contractRepository.getPaymentRequestsByCWId (cwId);
		for (PaymentRequest p : pList) {
			p.getContract().getSource().getId();
		}
		return pList;
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public PaymentRequest loadPaymentRequestView(Integer paymentRequestId) throws PersistenceException
	{
		PaymentRequest paymentRequest = contractRepository.find(PaymentRequest.class, paymentRequestId);
		// prevent lazy initialization exceptions
		paymentRequest.getAccount();
		paymentRequest.getContract().getSource().getName();

		return paymentRequest;
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public Map<Source, List<Contract>> loadAssetPermissionsView (int auId) throws Exception
	{
		AssetUse au = getAssetUseRepository().loadAssetUseById(auId);
		List<Source> sources = au.getAsset().getSources();
		Map<Source, List<Contract>> permissions = new HashMap<Source, List<Contract>>();
		for (Source source : sources) {
			ContractList contractList = getContractRepository().
					loadListForAssetSourceCW(au.getAsset().getId(), source.getId(), au.getCommonWork().getId());
			log.debug("Contract list size"+contractList.size());
			if (CollectionUtils.isEmpty(contractList)) {
				  List<Integer> list = getAssetUseRepository().getLatestContractIds(auId);
				  if (!CollectionUtils.isEmpty(list)){
				  Contract c=contractRepository.loadContractById(list.get(0));
				  log.debug("Commonwork id from original"+c.getCommonWork().getId());
				  contractList = getContractRepository().
							loadListForAssetSourceCW(au.getAsset().getId(), source.getId(), c.getCommonWork().getId());
				  }
				}
			List<Contract> cList = new ArrayList<Contract>();
			if (null != contractList) {
				for (Contract contract : contractList) {
					contract = contractRepository.lazyLoad (Contract.class, contract.getId(),
							new String[] {"source", "assets", "compCopies", "files", "fileNames", "currency"}); //added "fileNames" for DM-532
					// we need to include expired contracts
			//		if (!contract.isDateExpired()) {
						List<CompCopy> compCopies = contract.getCompCopies();
						if (CollectionUtils.isNotEmpty(compCopies)) {
							for (CompCopy cc : compCopies)
								cc.getAddress();
						} else {
							contract.setCompCopies(null);
						}
						cList.add(contract);
				//	}
				}
			}
			if (CollectionUtils.isEmpty(cList))
				cList = null;
			permissions.put(source, cList);
		}
		return permissions;
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public PaymentRequest payContract(int contractId, PaymentType pType, Date pDate, Account account, String costCenter, int checkNumber) throws Exception
	{
		Contract contract = getContractRepository().loadContractById(contractId);
		contract.setPaymentType(pType);
		contract.setPaymentDate(pDate);
		contract.setCheckNumber(checkNumber);

		getContractRepository().saveRequiresNew(contract);
		PaymentRequest paymentRequest = null;

		// if generate payment request
		if (contract.getPaymentType().equals(PaymentType.PAYMENT)) {
			paymentRequest = new PaymentRequest();
			paymentRequest.setAccount(account);
			paymentRequest.setCostCenter(costCenter);
			paymentRequest.setContract(contract);
			paymentRequest.setPaid(false);
			paymentRequest.setDateSubmitted(new Date());

			paymentRequest = contractRepository.saveRequiresNew(paymentRequest);
		}

		updateStatusForContract(contract.getId(), null);
		return paymentRequest;
	}

	/**
	 * Will send warnings if a contract is close to expire, or if the contract has
	 * expired will send an email and also will recalculate the asset statuses.
	 * We also recalculate for print run.
	 * This method is normally called from a cron job (also from the admin interface for testing).
	 */
	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public void calculateOutOfCompliance() throws Exception {
		PerfTimer timer = getMonitor().startTimer("ContractService::calculateOutOfCompliance");

		{ // scope - so don't accidently reuse any vars from this section for print run section
			// send close to expire emails
			List<Contract> closeContracts = contractRepository.getContractsCloseToExpireOrWindow(daysBeforeExpire);
			log.debug("calculateOutOfCompliance(): #contracts close to expire: " + closeContracts.size());
			int skipCount = 0;
			for (Contract contract : closeContracts) {
				log.debug("calculateOutOfCompliance(): Close to expire contractId: " + contract.getId());
				if (skipContract(contract.getId(), contract.getSource().getId())) {
					log.debug("calculateOutOfCompliance(): skipping b/c the contract source doesn't match"
						+ " any of the sources for the assets in the contract.");
					skipCount++;
				}
				else {
					boolean substitute = contractRepository.moreRecentPOOrContractWithAllAssets(contract);
					if (!substitute) {
						//Temporarily disabling contract expiration emails
					//	commonWorkService.sendEmailContractCloseToExpire(contract);
					}
					// don't need to do anything with the substitute - if it is also close to expire or expired contract
					// then it will be processed as another item in this list or the expired list
				}
			}
			log.debug("calculateOutOfCompliance(): close skipCount = " + skipCount + " (out of " + closeContracts.size() + ")");

			// send expired emails
			List<Integer> assetIdsNeedRecalc = new ArrayList<Integer>();
			List<Contract> expiredContracts = contractRepository.getContractsExpired();
			log.debug("calculateOutOfCompliance(): #contracts expired: " + expiredContracts.size());
			skipCount = 0;
			for (Contract contract : expiredContracts) {
				log.debug("calculateOutOfCompliance(): Expired contract id: " + contract.getId());
				if (skipContract(contract.getId(), contract.getSource().getId())) {
					log.debug("calculateOutOfCompliance(): skipping b/c the contract source doesn't match"
						+ " any of the sources for the assets in the contract.");
					skipCount++;
				}
				else {
					boolean substitute = contractRepository.moreRecentPOOrContractWithAllAssets(contract);
					if (!substitute) {
						//Temporarily disabling contract expiration emails
						//commonWorkService.sendEmailContractExpired(contract);
						List<ContractAsset> cas = contract.getAssets();
						for (ContractAsset ca : cas) {
							assetIdsNeedRecalc.add(ca.getAssetBaseId());
						}
					}
				}
			}
			log.debug("calculateOutOfCompliance(): expired skipCount = " + skipCount + " (out of " + expiredContracts.size() + ")");

			if (CollectionUtils.isNotEmpty(assetIdsNeedRecalc)) {
				getAssetUseService().updateStatusForAssetIdCollection(assetIdsNeedRecalc, true);
			}
		} // end scope

		// print run logic
		//List<Product> products = productRepository.getProductsUpdatedLately(daysFromLastUpdate);  // throws Exception

		// smarkoff: Change in logic: Rather than look at AssetUses for recently updated products (updated from PE)
		// we look at all AssetUses that are GRANTED_LIMITED_PRINT_RUN or OUT_OF_COMPLIANCE and check if they
		// are near or exceeding print run.
		// The problem with only looking at recently updated products is that we may miss situations
		// where the print run conditions have been lowered but the product printing has not changed.
		// Because there may be 10K+ GRANTED_LIMITED_PRINT_RUN assetUses in the future, we only load the list of
		// AU ids into memory - not the list of AU objects.

		List<Integer> auIdList = new ArrayList<Integer>();

		List<Integer> auIdGrantedLimited = assetUseRepository.loadAssetUseIdListByStatusForOutOfCompliance(
				PermissionStatus.GRANTED_LIMITED_PRINT_RUN_GROUP);
		log.debug("calculateOutOfCompliance(): #grantedLimited = " + auIdGrantedLimited.size());

		// Some recalculation may be done for assetUses that don't need it, but I think this
		// is better than checking every assetUse below for printing - or is it? Later
		// when we have more grantedLimited statuses, could test both ways.
		if (CollectionUtils.isNotEmpty(auIdGrantedLimited)) {
			getAssetUseService().updateStatusForAssetUseIdCollection(auIdGrantedLimited);
		}

		auIdList.addAll(auIdGrantedLimited);

		List<Integer> auIdOutOfCompliance = assetUseRepository.loadAssetUseIdListByStatusForOutOfCompliance(
				PermissionStatus.OUT_OF_COMPLIANCE_GROUP);
		log.debug("calculateOutOfCompliance(): #outOfCompliance = " + auIdOutOfCompliance.size());

		auIdList.addAll(auIdOutOfCompliance);

		// Keep track of contract emails already sent - don't send a 2nd email for the same
		// contract, so if there are 2+ AUs for a contract, only send one email.
		HashSet<Integer> exceededIdSet = new HashSet<Integer>();
		HashSet<Integer> closeIdSet = new HashSet<Integer>();

		// send print run notification emails
		for (Integer auId : auIdList) {
			AssetUse au = assetUseRepository.loadAssetUseById(auId);

			// We don't need to check PO's (just Contracts) because if the newest PO or Contract
			// was a PO then the status would be FORM_SENT and not GRANTED_LIMITED or OUT_OF_COMPLIANCE

			// In order for the status to be GRANTED_(UN)LIMITED or OUT_OF_COMPLIANCE there should always be
			// a latestContract already set by the rules engine.
			// There may be more than one latestContractId if the Asset has more than one source.
			List<Integer> latestContractIds = assetUseRepository.getLatestContractIds(auId);
			for (Integer contractId : latestContractIds) {
				Contract contract = contractRepository.loadContractById(contractId);
				if (!exceededIdSet.contains(contractId)) {
					if (contractRepository.exceededPrinting(contract)) {
						log.debug("calculateOutOfCompliance(): We have a Contract that exceeded printing.");
						commonWorkService.sendEmailExceedsPrinting(au.getCommonWork(), contractRepository.getPrintRunFromContract(contract), contract);
						exceededIdSet.add(contractId);
					}
					else if (!closeIdSet.contains(contractId) && contractRepository.closeToExceedingPrinting(contract)) {
						log.debug("calculateOutOfCompliance(): We have a Contract that is close to exceeding printing.");
						commonWorkService.sendEmailWarningPrinting(au.getCommonWork(), contractRepository.getPrintRunFromContract(contract), contract);
						closeIdSet.add(contractId);
					}
				}
			}
		}

		timer.stopTimer();
	}

	/**
	 * Called by the above calculateOutOfCompliance method.
	 * Returns true if the source of the contract does not match ANY of the sources
	 * for the assets associated with the contract.
	 * This situation can arise when the source(s) of all the assets
	 * for the contract have been changed after a contract was already created for the old source(s).
	 */
	private boolean skipContract(int contractId, int contractSourceId) {
		final String sql = "select count(*) as count from contract_2_asset c2a, asset_2_source a2s"
			+ " where c2a.contract_id = ? and c2a.asset_base_id = a2s.asset_id and a2s.source_id = ?";
		Query query = contractRepository.createNativeQuery(sql, "scalarCount");
		query.setParameter(1, contractId);
		query.setParameter(2, contractSourceId);
		// For count, DB2 and MS SQL Server return Integer but MySQL returns BigInteger
		// Integer and BigInteger inherit from Number
		Number result = (Number) query.getSingleResult();
		return result.intValue() == 0;
	}


	public AssetUseService getAssetUseService() {
		return assetUseService;
	}

	public void setAssetUseService(AssetUseService assetUseService) {
		this.assetUseService = assetUseService;
	}

	public ContractRepository getContractRepository() {
		return contractRepository;
	}

	public void setContractRepository(ContractRepository contractRepository) {
		this.contractRepository = contractRepository;
	}

	public AssetUseRepository getAssetUseRepository() {
		return assetUseRepository;
	}

	public void setAssetUseRepository(AssetUseRepository assetUseRepository) {
		this.assetUseRepository = assetUseRepository;
	}

	public ConditionRepository getConditionRepository() {
		return conditionRepository;
	}

	public void setConditionRepository(ConditionRepository conditionRepository) {
		this.conditionRepository = conditionRepository;
	}

	public AssetRepository getAssetRepository() {
		return assetRepository;
	}

	public void setAssetRepository(AssetRepository assetRepository) {
		this.assetRepository = assetRepository;
	}

	public String getSmtpServer() {
		return smtpServer;
	}

	public void setSmtpServer(String smtpServer) {
		this.smtpServer = smtpServer;
	}

	public int getDaysBeforeExpire() {
		return daysBeforeExpire;
	}

	public void setDaysBeforeExpire(int daysBeforeExpire) {
		this.daysBeforeExpire = daysBeforeExpire;
	}

	public ProductRepository getProductRepository() {
		return productRepository;
	}

	public void setProductRepository(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}

	public CommonWorkService getCommonWorkService() {
		return commonWorkService;
	}

	public void setCommonWorkService(CommonWorkService commonWorkService) {
		this.commonWorkService = commonWorkService;
	}

	public AssetUseIndexService getAssetUseIndexService() {
		return assetUseIndexService;
	}

	public void setAssetUseIndexService(AssetUseIndexService assetUseIndexService) {
		this.assetUseIndexService = assetUseIndexService;
	}

	public SourceService getSourceService() {
    	return sourceService;
    }

	public void setSourceService(SourceService sourceService) {
    	this.sourceService = sourceService;
    }

	public SourceRepository getSourceRepository() {
    	return sourceRepository;
    }

	public void setSourceRepository(SourceRepository sourceRepository) {
    	this.sourceRepository = sourceRepository;
    }
}
