package com.wiley.permissions.web.shared.controllers.permissions;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.permissions.domain.persistence.permissions.Asset;
import com.wiley.permissions.domain.persistence.permissions.CommonWork;
import com.wiley.permissions.domain.persistence.permissions.MasterAgreementDeal;
import com.wiley.permissions.domain.persistence.permissions.PermissionType;
import com.wiley.permissions.domain.persistence.permissions.PurchaseOrder;
import com.wiley.permissions.domain.persistence.permissions.Source;
import com.wiley.permissions.persistence.PersistenceException;
import com.wiley.permissions.repositories.AssetRepository;
import com.wiley.permissions.services.view.POAssetView;

public class PoWizardForm {

	private final static Log log = LogFactory.getLog(PoWizardForm.class);

	private List<Integer> assetIds = new ArrayList<Integer>();
	private Source source;
	private PurchaseOrder po = new PurchaseOrder();
	private int cwId;
	// permissionRequest represents the option chosen: request, PO, or "Request was created outside the system"
	private int permissionRequest;
	private String aids;
	private String prevUrl = null;
	private String nextUrl = null;
	private String[] sourceIds = null;
	private boolean multiForm = false;
	private boolean makeItDefault = false;
	private boolean isRerequest = false;
	private int selectedAuId;
	private List<POAssetView> assetViewList;

	//Added for PaperWork -- Task C Start
	private String wizardOwnerType =  null;
    //Added for PaperWork -- Task C End

	//Added to implement Build DM-374 -- Start
	private boolean managerApproved = false;

	public boolean isManagerApproved() {
		return managerApproved;
	}

	public void setManagerApproved(boolean managerApproved) {
		this.managerApproved = managerApproved;
	}
	//Added to implement Build DM-374 -- End

	private List<MasterAgreementDeal> masterAgreementDeals = new ArrayList<MasterAgreementDeal>();
	private String selectedDealPricing;


	// place holder for assets selected to be attached on PO
	private List<Integer> attachedAssetIds = new ArrayList<Integer>();

	private AssetRepository assetRepository;

	public String getAids() {
		return aids;
	}

	public void setAids(String aids) {
		this.aids = aids;
		assetIds.clear();
		if (StringUtils.isNotBlank(aids)) {
			String[] aIds = StringUtils.split(aids, ",");
			for (String aid : aIds) {
				addAssetId (new Integer (aid));
			}
		}
	}

	public int getPermissionRequest() {
		return permissionRequest;
	}

	public void setPermissionRequest(int permissionRequest) {
		this.permissionRequest = permissionRequest;
	}

	public PurchaseOrder getPo() {
		return po;
	}

	public void setPo(PurchaseOrder po) {
		this.po = po;
	}

	public List<Integer> getAssetIds() {
		return assetIds;
	}

	public void setAssetIds(List<Integer> assetIds) {
		this.assetIds = assetIds;
	}

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

	public void addAssetId(Integer aid) {
		if (!assetIds.contains(aid))
			assetIds.add(aid);
	}

	public void populateEditForm() {
		if (po.isOutsideRecord())
			permissionRequest = 2;
		else
			permissionRequest = po.isPermissionRequest() ? 1 : 0;

		setCwId(po.getCommonWork().getId());

		for (Asset asset : po.getAssets()) {
			addAssetId (asset.getId());
		}
	}

	public void populateNewForm () {
		permissionRequest = 1;
		if (null != source && null != source.getPermissionType()) {
			if (PermissionType.REUSE_PO.getCode().equals(source.getPermissionType())) {
				permissionRequest = 0;
			}
			if (PermissionType.PREFERRED_VENDOR.getCode().equals(source.getPermissionType())) {
				permissionRequest = 0;
			}
		}
	}

	public void populateBean() {
		// if permissionRequest = 2, the value comes from the page, we do not overwrite
		if (permissionRequest != 2)
			po.setPermissionRequest(permissionRequest == 1);
		po.setOutsideRecord(permissionRequest == 2);
		po.getAssets().clear();
		for (Integer aid : assetIds) {
			log.debug("ASSET ID " + aid);
			try {
				Asset dAsset = assetRepository.find(Asset.class, aid);
				// there is a situation where the same asset is added more than once to the list
				// this causes a duplicate key exception when the PO is persisted.
				// this would normally happen when a source is assiged to an asset for the first time
				// on a given common work.
				if (!po.getAssets().contains(dAsset)) {
					po.getAssets().add(assetRepository.find(Asset.class, aid));
				}
			}
			catch (PersistenceException e) {
				log.debug ("failed to load asset by id " + aid);
			}
		}
		CommonWork cw = new CommonWork();
		cw.setId(cwId);
		po.setCommonWork(cw);
		po.setSource(source);
	}


	public List<MasterAgreementDeal> getMasterAgreementDeals() {
		return masterAgreementDeals;
	}

	public void setMasterAgreementDeals(List<MasterAgreementDeal> masterAgreementDeals) {
		this.masterAgreementDeals = masterAgreementDeals;
	}

	public Boolean getMultiDeal() {
		return CollectionUtils.isNotEmpty(getMasterAgreementDeals()) && getMasterAgreementDeals().size() > 1;
	}

	public String getSelectedDealPricing() {
		return selectedDealPricing;
	}

	public void setSelectedDealPricing(String selectedDealPricing) {
		this.selectedDealPricing = selectedDealPricing;
	}

	public AssetRepository getAssetRepository() {
		return assetRepository;
	}

	public void setAssetRepository(AssetRepository assetRepository) {
		this.assetRepository = assetRepository;
	}

	public void setPrevUrl(String url) {
		prevUrl = url;
	}

	public String getPrevUrl() {
		return prevUrl;
	}

	public String getNextUrl() {
		return nextUrl;
	}

	public void setNextUrl(String nextUrl) {
		this.nextUrl = nextUrl;
	}

	public String[] getSourceIds() {
		return sourceIds;
	}

	public void setSourceIds(String[] sids) {
		this.sourceIds = sids;
	}

	public boolean isMultiForm() {
		return multiForm;
	}

	public void setMultiForm(boolean multiForm) {
		this.multiForm = multiForm;
	}

	public boolean isMakeItDefault() {
		return makeItDefault;
	}

	public void setMakeItDefault(boolean makeItDefault) {
		this.makeItDefault = makeItDefault;
	}

	public boolean isRerequest() {
		return isRerequest;
	}

	public void setRerequest(boolean isRerequest) {
		this.isRerequest = isRerequest;
	}

	public int getSelectedAuId() {
		return selectedAuId;
	}

	public void setSelectedAuId(int value) {
		this.selectedAuId = value;
	}

	public List<POAssetView> getAssetViewList() {
		return assetViewList;
	}

	public void setAssetViewList(List<POAssetView> assetList) {
		this.assetViewList = assetList;
	}

	public List<Integer> getAttachedAssetIds() {
		return attachedAssetIds;
	}

	public void setAttachedAssetIds(List<Integer> attachedAssetIds) {
		this.attachedAssetIds = attachedAssetIds;
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
