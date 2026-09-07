package com.wiley.permissions.web.shared.controllers.permissions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.permissions.domain.persistence.permissions.Address;
import com.wiley.permissions.domain.persistence.permissions.AssetBase;
import com.wiley.permissions.domain.persistence.permissions.CommonWork;
import com.wiley.permissions.domain.persistence.permissions.ConditionNode;
import com.wiley.permissions.domain.persistence.permissions.Contract;
import com.wiley.permissions.domain.persistence.permissions.ContractAsset;
import com.wiley.permissions.domain.persistence.permissions.ImportSource;
import com.wiley.permissions.domain.persistence.permissions.PurchaseOrder;
import com.wiley.permissions.domain.persistence.permissions.RoyaltyFreeDeal;
import com.wiley.permissions.domain.persistence.permissions.Source;
import com.wiley.permissions.domain.persistence.permissions.SourceGroup;
import com.wiley.permissions.domain.persistence.permissions.UsageConditionSize;
import com.wiley.permissions.repositories.AssetRepository;

public class ContractWizardForm {

	private final static Log log = LogFactory.getLog(ContractWizardForm.class);

	private Source source;
	private Contract contract = new Contract();
	private int poId;
	private int cwId;
	private int assetId;
	private String data;
	private String prevUrl;
	private boolean permissionRequest;
	private boolean appliedOnline;	// Added for DM-1606
	private boolean noRestrictions;
	private int numberOfCopies;
	private int numberOfCopies2;
	private int mailingAddressId;
	private List<ConditionNode> conditions;
	private boolean isRerequest;
	private Address address;
	private String compRecipient;
	private boolean readOnly;
	private Integer maDealId;
	private Map<Integer, List<UsageConditionSize>> usageSizeMap;
	private boolean useLimitationOfLiabilityLogic = false;
	private double totalAssetPrice;
	//Start: Added to implement DM-122 & DM-123
	private boolean assetLevelRF;
	private boolean sourceGroupHasMasterAgreements;
	private boolean hasRFDealsForSouceGroup;
	private boolean loadedAssetLvlRF;
	private boolean hasNamedRFDealForSouceGroup;//to check predefined asset level RF deal format i.e. "{source name} Royalty Free"

	//Start: Added to implement DM-1185
	private String invoiceDateStr = null;
	private boolean maExistForInvoiceDate;//to know whether Master Agreement present for the selected invoice date


	public boolean isMaExistForInvoiceDate() {
		return maExistForInvoiceDate;
	}

	public void setMaExistForInvoiceDate(boolean maExistForInvoiceDate) {
		this.maExistForInvoiceDate = maExistForInvoiceDate;
	}

	public String getInvoiceDateStr() {
		return invoiceDateStr;
	}

	public void setInvoiceDateStr(String invoiceDateStr) {
		this.invoiceDateStr = invoiceDateStr;
	}
	//End: Added to implement DM-1185

	
	public boolean isAssetLevelRF() {
		return assetLevelRF;
	}

	public void setAssetLevelRF(boolean assetLevelRF) {
		this.assetLevelRF = assetLevelRF;
	}

	public boolean isSourceGroupHasMasterAgreements() {
		return sourceGroupHasMasterAgreements;
	}

	public void setSourceGroupHasMasterAgreements(
			boolean sourceGroupHasMasterAgreements) {
		this.sourceGroupHasMasterAgreements = sourceGroupHasMasterAgreements;
	}

	public boolean isHasRFDealsForSouceGroup() {
		return hasRFDealsForSouceGroup;
	}

	public void setHasRFDealsForSouceGroup(boolean hasRFDealsForSouceGroup) {
		this.hasRFDealsForSouceGroup = hasRFDealsForSouceGroup;
	}

	public boolean isLoadedAssetLvlRF() {
		return loadedAssetLvlRF;
	}

	public void setLoadedAssetLvlRF(boolean loadedAssetLvlRF) {
		this.loadedAssetLvlRF = loadedAssetLvlRF;
	}

	public boolean isHasNamedRFDealForSouceGroup() {
		if(null != this.getSource() && this.getSource().getSourceGroup() != null)
		{
			List<RoyaltyFreeDeal> rfDeals = this.getSource().getSourceGroup().getRoyaltyFreeDeals();
			String assetLevelRFDealName = this.getSource().getName() + "Royalty Free"; //This is the predefined format of asset level RF.
			if(rfDeals.size() > 0) {
				for(RoyaltyFreeDeal rfdeal : rfDeals) {
					if(assetLevelRFDealName.equals(rfdeal.getDescription())) {
						return true;
					}
				}
			}
		}
		return hasNamedRFDealForSouceGroup;
	}

	public void setHasNamedRFDealForSouceGroup(boolean hasNamedRFDealForSouceGroup) {
		this.hasNamedRFDealForSouceGroup = hasNamedRFDealForSouceGroup;
	}
	//End: Added to implement DM-122 & DM-123
	//Ram added below code for invoice extension
		private ImportSource importSource;

	//ends here

	//Added for PaperWork -- Task C Start
	private String wizardOwnerType =  null;
	//Added for PaperWork -- Task C End

	private AssetRepository assetRepository;

	//Added to implement Build DM-374 -- Start
	private boolean managerApproved = false;

	public boolean isManagerApproved() {
		return managerApproved;
	}

	public void setManagerApproved(boolean managerApproved) {
		this.managerApproved = managerApproved;
	}
	private boolean inEditAsset = false;

	public boolean isInEditAsset() {
		return inEditAsset;
	}

	public void setInEditAsset(boolean inEditAsset) {
		this.inEditAsset = inEditAsset;
	}
	//Added to implement Build DM-374 -- End

	public double getTotalAssetPrice() {
		return totalAssetPrice;
	}

	public void setTotalAssetPrice(double totalAssetPrice) {
		this.totalAssetPrice= totalAssetPrice;
	}

	public String getData() {
		return data;
	}

	/**
	 * Receives a list of values separated by comma, containing assetId, followed by the price
	 * @param data
	 */
	public void setData(String data) {
		log.debug("setData(): data = " + data);

		// remove assets that were not selected and keep the selected ones
		List<ContractAsset> newAssets = new ArrayList<ContractAsset>();
		this.data = data;
		//double totalCost = 0;
		if (StringUtils.isNotBlank(data)) {
			String[] dataArr = StringUtils.split(data, "|");

			for (int i = 0; i < dataArr.length; i++) {
				String aid = dataArr[i];
				i++;
				double price = new Double(dataArr[i]);
				i++;
				int rfDealId = new Integer(dataArr[i]);
				i++;
				String creditLine = dataArr[i];
				// client side has logic to set credit line to "n/a" if blank just as a place holder
				// so convert this black to null (also if user enters "n/a" we don't want that either)
				if ("n/a".equalsIgnoreCase(creditLine)) {
					creditLine = null;
				}
				i++;
				boolean noCrop = Boolean.parseBoolean(dataArr[i]);
				i++;
				boolean noBleed = Boolean.parseBoolean(dataArr[i]);
				ContractAsset contractAsset = contract.getContractAssetById(new Integer(aid));
				// if is new
				if (null == contractAsset || null == contractAsset.getContractId()) {
					contractAsset = new ContractAsset();
					AssetBase assetBase = new AssetBase();
					assetBase.setId(new Integer (aid));
					contractAsset = new ContractAsset(assetBase, contract, price);
					//Start: Added to implement DM-122
					if (rfDealId == -1) {
						contractAsset.setAssetLevelRFDeal(true);
						setAssetLevelRF(true);
						rfDealId = 0;
						contractAsset.setRForRM(null);//Added to implement DM-123
					} else if (rfDealId == 0) {
						contractAsset.setAssetLevelRFDeal(false);
						setAssetLevelRF(false);
						contractAsset.setRForRM(null);//Added to implement DM-123
					} else {//We are here means, we have selected source level RF deal
						contractAsset.setAssetLevelRFDeal(false);
						setAssetLevelRF(false);
						contractAsset.setRForRM(null);//Added to implement DM-123
						contractAsset.setRoyaltyFreeDealId(rfDealId);
					}
					//End: Added to implement DM-122
				// if old
				} else {
					contractAsset.setPrice(price);
					//Start: Added to implement DM-122
					if (rfDealId == -1) {
						contractAsset.setAssetLevelRFDeal(true);
						setAssetLevelRF(true);
						rfDealId = 0;
						contractAsset.setRForRM("ARF");//Added to implement DM-123
					} else if (rfDealId == 0) {
						contractAsset.setAssetLevelRFDeal(false);
						setAssetLevelRF(false);
						contractAsset.setRForRM("RM");//Added to implement DM-123
					} else {//We are here means, we have selected source level RF deal
						contractAsset.setAssetLevelRFDeal(false);
						setAssetLevelRF(false);
						contractAsset.setRForRM("SRF");//Added to implement DM-123
					}
					//End: Added to implement DM-122
				}
				contractAsset.setRoyaltyFreeDealId(rfDealId);
				contractAsset.setCreditLine(creditLine);
				contractAsset.setPrice(price);
				contractAsset.setNoCrop(noCrop);
				contractAsset.setNoBleed(noBleed);
				//totalCost += price;
				newAssets.add(contractAsset);
			}
		}

		contract.setAssets(newAssets);
		// lnagy - do not default the contract price. Show a warning and let the use change it
		// contract.setPrice(totalCost);
	}

	public boolean getUseLimitationOfLiabilityLogic() {
		return useLimitationOfLiabilityLogic;
	}

	public void setUseLimitationOfLiabilityLogic(boolean useLimitationOfLiabilityLogic) {
		this.useLimitationOfLiabilityLogic = useLimitationOfLiabilityLogic;
	}

	public String getCompRecipient() {
		return compRecipient;
	}

	public void setCompRecipient(String recipient) {
		this.compRecipient = recipient;
	}

	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}

/*	public List<Integer> getAssetIds() {
		return assetIds;
	}

	public void setAssetIds(List<Integer> aids) {
		this.assetIds = aids;
	}*/

	public Source getSource() {
		return source;
	}

	public void setSource(Source source) {
		this.source = source;
	}

	public int getCwId() {
		return cwId;
	}

	public void setCwId(int cwId) {
		this.cwId = cwId;
	}

	public Contract getContract() {
		return contract;
	}

	public void setContract(Contract contract) {
		this.contract = contract;
	}

	public int getPoId() {
		return poId;
	}

	public void setPoId(int poId) {
		this.poId = poId;
	}

	public int getAssetId() {
		return assetId;
	}

	public void setAssetId(int aid) {
		this.assetId = aid;
	}

	public void populateForm() {
	}

	public void populateEditForm() {
		permissionRequest = contract.isPermissionForm();
		setCwId(contract.getCommonWork().getId());
		//for (ContractAsset asset : contract.getAssets())
		//	addAssetId (asset.getAssetBaseId());
		if (null != contract.getPurchaseOrder())
			setPoId (contract.getPurchaseOrder().getId());
		//contract.getConditionsNotNull().clear();
	}

	public void populateBean() {
		if (isNoRestrictions()) {
			contract.setPrice(0.0);
		}
		contract.setPermissionForm(isPermissionRequest());
		if (poId != 0) {
			PurchaseOrder po = new PurchaseOrder ();
			po.setId(poId);
			contract.setPurchaseOrder(po);
		}
		CommonWork cw = new CommonWork ();
		cw.setId(cwId);
		contract.setCommonWork(cw);
		contract.setSource(source);
		// lazy load the masterAgreements
		if (null != source && null != source.getSourceGroup()) {
			try {
				source.setSourceGroup(assetRepository.lazyLoad (
						SourceGroup.class, source.getSourceGroup().getId(), new String[] {"masterAgreements"}));
			} catch (Exception e) {
				log.debug("populateBean(): failed to load master agreement");
			}
		}
	}

	public void setPrevUrl(String url) {
		prevUrl = url;
	}

	public String getPrevUrl() {
		return prevUrl;
	}

	public void setPermissionRequest(boolean pr) {
		permissionRequest = pr;
	}

	public boolean isPermissionRequest() {
		return permissionRequest;
	}

	// Start : Added for DM-1606
	public boolean isAppliedOnline() {
		return appliedOnline;
	}

	public void setAppliedOnline(boolean appliedOnline) {
		this.appliedOnline = appliedOnline;
	}
	// End : Added for DM-1606
	public boolean isNoRestrictions() {
		return noRestrictions;
	}

	public void setNoRestrictions(boolean noRestrictions) {
		this.noRestrictions = noRestrictions;
	}

	public int getNumberOfCopies() {
		return numberOfCopies;
	}

	public void setNumberOfCopies(int numberOfCopies) {
		this.numberOfCopies = numberOfCopies;
	}

	public int getNumberOfCopies2() {
		return numberOfCopies2;
	}

	public void setNumberOfCopies2(int numberOfCopies) {
		this.numberOfCopies2 = numberOfCopies;
	}

	public int getMailingAddressId() {
		return mailingAddressId;
	}

	public void setMailingAddressId(int mailingAddressId) {
		this.mailingAddressId = mailingAddressId;
	}

	public AssetRepository getAssetRepository() {
		return assetRepository;
	}

	public void setAssetRepository(AssetRepository assetRepository) {
		this.assetRepository = assetRepository;
	}

	public List<ConditionNode> getConditions() {
		return conditions;
	}

	public void setConditions(List<ConditionNode> conditions) {
		this.conditions = conditions;
	}

	public boolean isRerequest() {
		return isRerequest;
	}

	public void setRerequest(boolean isRerequest) {
		this.isRerequest = isRerequest;
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	public Integer getMaDealId() {
		return maDealId;
	}

	public void setMaDealId(Integer i) {
		maDealId = i;
	}

	public boolean getSingleAsset() {
		if (CollectionUtils.isEmpty(contract.getAssets())) return false;
		return (contract.getAssets().size() > 1 ? false : true);
	}

	public void setPrice(Integer aid, Double price) {
		ContractAsset asset = contract.getContractAssetById (aid);
		log.debug ("setPrice: contract asset " + asset);
		if (null != asset) {
			log.debug ("setPrice: price " + price);
			asset.setPrice(price);
		}
	}

	public Double getPrice(String aids) {
		String[] saids = aids.split(",");
		List<Double> prices = new ArrayList<Double>();
		for (String aid : saids) {
			log.debug ("getPrice: aid " + aid);
			ContractAsset asset = contract.getContractAssetById(new Integer(aid));
			log.debug ("getPrice: asset " + asset);
			if (null != asset) {
				if (null != asset.getPrice()) {
					if (!prices.contains(asset.getPrice())) {
						log.debug ("getPrice: add price " + asset.getPrice());
						prices.add(asset.getPrice());
					}
				}
			}
		}
		// if all prices are equal
		if (prices.size() == 1)
			return prices.get(0);

		return null;
	}

	public Map<Integer, List<UsageConditionSize>> getUsageSizeMap() {
		return usageSizeMap;
	}

	public Map<Integer, List<UsageConditionSize>> getUsageSizeMapSelectedOnly() {
		Map<Integer, List<UsageConditionSize>> map2 = new HashMap<Integer, List<UsageConditionSize>>();
		List<Integer> contractAssetIds = contract.getAssetIds();
		for (Integer assetId : usageSizeMap.keySet()) {
			for (Integer contractAssetId : contractAssetIds) {
				if (assetId.equals(contractAssetId)) {
					map2.put(assetId, usageSizeMap.get(assetId));
				}
			}
		}
		return map2;
	}

	public void setUsageSizeMap(Map<Integer, List<UsageConditionSize>> map) {
		this.usageSizeMap = map;
	}

	public ImportSource getImportSource() {
		return importSource;
	}

	public void setImportSource(ImportSource importSource) {
		this.importSource = importSource;
	}

	//Added for PaperWork -- Task C Start
	public String getWizardOwnerType() {
		return wizardOwnerType;
	}

	public void setWizardOwnerType(String wizardOwnerType) {
		this.wizardOwnerType = wizardOwnerType;
	}
	//Added for PaperWork -- Task C End


}
