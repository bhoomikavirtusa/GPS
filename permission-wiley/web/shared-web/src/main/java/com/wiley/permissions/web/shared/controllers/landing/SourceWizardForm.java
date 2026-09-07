package com.wiley.permissions.web.shared.controllers.landing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.wiley.permissions.domain.persistence.permissions.Asset;
import com.wiley.permissions.domain.persistence.permissions.AssetUse;
import com.wiley.permissions.domain.persistence.permissions.MasterAgreementDeal;
import com.wiley.permissions.domain.persistence.permissions.OwnerType;
import com.wiley.permissions.domain.persistence.permissions.Source;
import com.wiley.permissions.services.view.AssetUseTableRowView;

public class SourceWizardForm {

	public enum FormOwnerType {
		WILEY_OWNED_BOOK("Wiley_Owned_Book", OwnerType.WILEY),
		WILEY_OWNED_JOURNAL("Wiley_Owned_Journal", OwnerType.WILEY),
		WILEY_OWNED_AUTHOR("Wiley_Owned_Author", OwnerType.AUTHOR_OWNED),
		WORK_FOR_HIRE("Work_For_Hire", OwnerType.WORK_FOR_HIRE),
		WILEY_CREATED("Wiley_Created", OwnerType.WILEY_CREATED),
		PHOTO_REQUEST("Photo_Request", OwnerType.PHOTO_REQUEST),
		ILLUSTRATION_REQUEST("Illustration_Request", OwnerType.ILLUSTRATION_REQUEST),
		THIRD_PARTY_INSTITUTION("3rd_Party_Institution", OwnerType.THIRD_PARTY),
		THIRD_PARTY_PUBLICATION("3rd_Party_Publication", OwnerType.THIRD_PARTY),
		PREVIOUS_EDITION("Previous_Edition", OwnerType.WILEY),
		RIGHTSLINK_WARNING("Rights_Link", OwnerType.THIRD_PARTY);

		private final String code;
		private final OwnerType ownerType;


		private FormOwnerType(String code, OwnerType ownerType) {
			this.code = code;
			this.ownerType = ownerType;
		}

		public String getCode() {
			return code;
		}

		public OwnerType getOwnerType() {
			return ownerType;
		}

		public static FormOwnerType get(String code) {
			if (StringUtils.isNotBlank(code)) {
				for (FormOwnerType b : FormOwnerType.values()) {
					if (code.equalsIgnoreCase(b.code)) {
						return b;
					}
				}
				return null;
			}
			return null;
		}

		public static FormOwnerType get(OwnerType ownerType) {
			if (null != ownerType) {
				for (FormOwnerType b : FormOwnerType.values()) {
					if (ownerType.equals(b.ownerType)) {
						return b;
					}
				}
				return null;
			}
			return null;
		}
	} // end enum


	private String[] assetList = null;
	private int currentAssetIdx = -1;
	private boolean multiAsset = false;
	private boolean lastAssetUse = false;
	private boolean includeAssetDescription = false;
	private String assetDescription;

	private int auId;
	private String ownerType;
	private AssetUse assetUse;
	private boolean replace = false;
	private boolean clear = false;

	// author provided fields
	private MultipartFile assetFile = null;
	private MultipartFile files = null;

	// 3rd party
	private String sourceName;
	private int sourceId;
	private boolean publicDomain;
	private boolean mediaManager;
	private boolean fairUse;
	private Map<Integer, Boolean> sourcesMap = new HashMap<Integer, Boolean>();
	private boolean multiSource;
	private boolean hasBiblio;
	private List<MasterAgreementDeal> masterAgreementDeals = new ArrayList<MasterAgreementDeal>();
	private String selectedDealPricing;

	// previous edition
	private ArrayList <LabelValueBean> editions;
	private String edition;
	private AssetSearchView previousEditionRequestCriteria = new AssetSearchView();

	private ArrayList<AssetSearchView> assetsFound;
	private String prevUrl;
	private String assetSelected;
	private boolean editionUse;
	private List<Source> profileSources;

	// previous wiley pub related fields
	private ArrayList<AssetUseTableRowView> previousPubAssets;
	private List<Asset> currentPubAssets;
	private Integer selectedPreviousAssetUseId;
	private AssetUse selectedPreviousAssetUse;
	private String publicationAuthor;
	private OwnerType newOwnerType;
	private String returnAddress;
	private String userSignature;
	private boolean makeItDefault;
	private String medium;
	private String editionNumber;
	private boolean hasRoyaltyFreeDeals;

	private boolean noFlyMatchApproved;

	public boolean isNoFlyMatchApproved() {
		return noFlyMatchApproved;
	}

	public void setNoFlyMatchApproved(boolean noFlyMatchApproved) {
		this.noFlyMatchApproved = noFlyMatchApproved;
	}

	public List<MasterAgreementDeal> getMasterAgreementDeals() {
		return masterAgreementDeals;
	}

	public void setMasterAgreementDeals(List<MasterAgreementDeal> masterAgreementDeals) {
		this.masterAgreementDeals = masterAgreementDeals;
	}

	public boolean getMultiDeal() {
		return CollectionUtils.isNotEmpty(getMasterAgreementDeals()) && getMasterAgreementDeals().size() > 1;
	}

	public boolean getHasDeals() {
		return CollectionUtils.isNotEmpty(getMasterAgreementDeals());
	}

	public void setHasRoyaltyFreeDeals(boolean hasRoyaltyFreeDeals) {
		this.hasRoyaltyFreeDeals = hasRoyaltyFreeDeals;
	}

	public boolean getHasRoyaltyFreeDeals() {
		return hasRoyaltyFreeDeals;
	}

	public String getSelectedDealPricing() {
		return selectedDealPricing;
	}

	public void setSelectedDealPricing(String selectedDealPricing) {
		this.selectedDealPricing = selectedDealPricing;
	}

	public void setEditionNumber(String editionNumber) {
		this.editionNumber = editionNumber;
	}

	public String getEditionNumber() {
		return editionNumber;
	}

	public void setMedium(String medium) {
		this.medium = medium;
	}

	public String getMedium() {
		return medium;
	}

	public void setReturnAddress(String returnAddress) {
		this.returnAddress = returnAddress;
	}

	public String getReturnAddress() {
		return returnAddress;
	}

	public void setUserSignature(String userSignature) {
		this.userSignature = userSignature;
	}

	public String getUserSignature() {
		return userSignature;
	}


	public void setMakeItDefault(boolean makeItDefault) {
		this.makeItDefault = makeItDefault;
	}

	public boolean getMakeItDefault() {
		return makeItDefault;
	}

	public boolean isEditionUse() {
		return editionUse;
	}

	public void setEditionUse(boolean editionUse) {
		this.editionUse = editionUse;
	}

	public boolean getIncludeAssetDescription() {
		return includeAssetDescription;
	}

	public void setIncludeAssetDescription(boolean includeAssetDescription) {
		this.includeAssetDescription = includeAssetDescription;
	}

	public boolean getLastAssetUse() {
		return lastAssetUse;
	}

	public void setLastAssetUse(boolean lastAssetUse) {
		this.lastAssetUse = lastAssetUse;
	}

	public boolean getMultiAsset() {
		return multiAsset;
	}

	public void setMultiAsset(boolean multiAsset) {
		this.multiAsset = multiAsset;
	}

	public Integer getCurrentAssetIdx() {
		return currentAssetIdx;
	}

	public void setCurrentAssetIdx(Integer currentAssetIdx) {
		this.currentAssetIdx = currentAssetIdx;
	}

	public String[] getAssetList() {
		return assetList;
	}

	public void setAssetList(String[] assetList) {
		this.assetList = assetList;
	}

	public boolean getHasBiblio() {
		return hasBiblio;
	}

	public void setHasBiblio(boolean hasBiblio) {
		this.hasBiblio = hasBiblio;
	}

	public AssetSearchView getPreviousEditionRequestCriteria() {
		return  previousEditionRequestCriteria;
	}

	public void setPreviousEditionRequestCriteria(AssetSearchView value) {
		this.previousEditionRequestCriteria = value;
	}

	public String getAssetSelected() {
		return assetSelected;
	}

	public void setAssetSelected(String assetSelected) {
		this.assetSelected = assetSelected;
	}

	public ArrayList <AssetSearchView> getAssetsFound() {
		return assetsFound;
	}

	public void setAssetsFound(ArrayList <AssetSearchView> assetsFound) {
		this.assetsFound = assetsFound;
	}

	public void setPreviousPubAssets(ArrayList <AssetUseTableRowView> previousPubAssets) {
		this.previousPubAssets = previousPubAssets;
	}

	public ArrayList <AssetUseTableRowView> getPreviousPubAssets() {
		return previousPubAssets;
	}

	public void setCurrentPubAssets(List <Asset> currentPubAssets) {
		this.currentPubAssets = currentPubAssets;
	}

	public List <Asset> getCurrentPubAssets() {
		return currentPubAssets;
	}

	public void setSelectedPreviousAssetUseId(Integer selectedPreviousAssetUseId) {
		this.selectedPreviousAssetUseId = selectedPreviousAssetUseId;
	}

	public Integer getSelectedPreviousAssetUseId() {
		return selectedPreviousAssetUseId;
	}

	public void setSelectedPreviousAssetUse(AssetUse selectedPreviousAssetUse) {
		this.selectedPreviousAssetUse = selectedPreviousAssetUse;
	}

	public AssetUse getSelectedPreviousAssetUse() {
		return selectedPreviousAssetUse;
	}

	public void setPublicationAuthor(String publicationAuthor) {
		this.publicationAuthor = publicationAuthor;
	}

	public String getPublicationAuthor() {
		return publicationAuthor;
	}

	public void setNewOwnerType(OwnerType newOwnerType) {
		this.newOwnerType = newOwnerType;
	}

	public OwnerType getNewOwnerType() {
		return newOwnerType;
	}

	public boolean isReplace() {
		return replace;
	}

	public void setReplace(boolean replace) {
		this.replace = replace;
	}

	public String getEdition() {
		return edition;
	}

	public void setEdition(String edition) {
		this.edition = edition;
	}

	public void setAssetDescription(String assetDescription) {
		this.assetDescription = assetDescription;
	}

	public String getAssetDescription() {
		return this.assetDescription;
	}

	public ArrayList<LabelValueBean> getEditions() {
		return editions;
	}

	public void setEditions(ArrayList <LabelValueBean> editions) {
		this.editions = editions;
	}

	public Map<Integer, Boolean> getSourcesMap() {
		return sourcesMap;
	}

	public void setSourcesMap(Map<Integer, Boolean> sourceIds) {
		this.sourcesMap = sourceIds;
	}

	public void removeSourceId() {
		if (sourcesMap.containsKey(sourceId)) {
			sourcesMap.put(sourceId, false);
		}
	}

	public void addSourceId() {
		// adds the last selected sourceId if not already selected
		sourcesMap.put(sourceId, true);
	}

	public boolean isMultiSource() {
		return multiSource;
	}

	public void setMultiSource(boolean multiSource) {
		// once set to true, will remain true
		if (!this.multiSource)
			this.multiSource = multiSource;
	}

	public boolean isPublicDomain() {
		return publicDomain;
	}

	public void setPublicDomain(boolean publicDomain) {
		this.publicDomain = publicDomain;
	}

	public boolean isMediaManager() {
		return mediaManager;
	}

	public void setMediaManager(boolean mediaManager) {
		this.mediaManager = mediaManager;
	}
	
	public boolean isFairUse() {
		return fairUse;
	}

	public void setFairUse(boolean fairUse) {
		this.fairUse = fairUse;
	}

	public String getSourceName() {
		return sourceName;
	}

	public void setSourceName(String sourceName) {
		this.sourceName = sourceName;
	}

	public int getSourceId() {
		return sourceId;
	}

	public void setSourceId(int sourceId) {
		this.sourceId = sourceId;
	}

	public MultipartFile getFiles() {
		return files;
	}

	public void setFiles(MultipartFile files) {
		this.files = files;
	}

	public MultipartFile getAssetFile() {
		return assetFile;
	}

	public void setAssetFile(MultipartFile assetFile) {
		this.assetFile = assetFile;
	}

	public String getOwnerType() {
		return ownerType;
	}

	public void setOwnerType(String ownerType) {
		this.ownerType = ownerType;
	}

	public void setAUId(Integer auId) {
		this.auId = auId;
	}

	public int getAUId() {
		return auId;
	}

	public AssetUse getAssetUse() {
		return assetUse;
	}

	public void setAssetUse(AssetUse assetUse) {
		this.assetUse = assetUse;
	}

	public String getPrevUrl() {
		return prevUrl;
	}

	public void setPrevUrl(String prevUrl) {
		this.prevUrl = prevUrl;
	}

	public void populateForm() {
		if (null != assetUse && null != assetUse.getWizardOwnerType())
		//not sure why this is not working
		//	this.setOwnerType (FormOwnerType.get (assetUse.getWizardOwnerType()).getCode());
			this.setOwnerType(assetUse.getWizardOwnerType());
		if (assetUse.getAsset().getDescription().indexOf("*ENTER DESCRIPTION") > -1  || assetUse.getAsset().getDescription().length() < 1) {
			assetUse.getAsset().setDescription("");
			setIncludeAssetDescription(true);
		} else {
			setIncludeAssetDescription(false);
		}
	}

	public void populateBean(AssetUse assetUse) {
		assetUse.getAsset().setOwnerType(FormOwnerType.get(this.getOwnerType()).getOwnerType());

		assetUse.setEstimatedCost(this.assetUse.getEstimatedCost());
		assetUse.setEstimatedCurrency(this.assetUse.getEstimatedCurrency());

		assetUse.setWizardOwnerType(FormOwnerType.get(this.getOwnerType()).getCode());
		// set Managed = T for all but Author provided. Set Fee req = F for all and Royalty free = F for all
		// https://www.pivotaltracker.com/story/show/14663271
		assetUse.getAsset().setManaged(true);
		assetUse.getAsset().setRoyaltyFree(false);
		assetUse.getAsset().setFeeRequired(false);

		if (isPublicDomain()) {
			assetUse.getAsset().setOwnerType(OwnerType.PUBLIC_DOMAIN);
			assetUse.getAsset().setManaged(false);
			assetUse.getAsset().setRoyaltyFree(true);
		}
		if (isFairUse()) {
			assetUse.getAsset().setOwnerType(OwnerType.FAIR_USE);
			assetUse.getAsset().setManaged(false);
			assetUse.getAsset().setFeeRequired(false);
			assetUse.getAsset().setRoyaltyFree(true);
		}

		// If reselect then restart Source wizard. If add then go to Source select.
		// https://www.pivotaltracker.com/story/show/14664045
		if (isReplace() && null != assetUse.getAsset().getSources()) {
			assetUse.getAsset().getSources().clear();
		}
	}

	/**
	 * This method is temporary until some future phase when users will
	 * be able to edit all assets in Permissions including ones created in CMS.
	 */
	public boolean isAssetFromCMS() {
		// not sure whether assetUse or asset would ever be null
		if (assetUse == null)  return false;
		Asset asset = assetUse.getAsset();
		if (asset == null)  return false;
		String extId = asset.getExternalId();
		return extId != null && extId.startsWith("CMS");
	}

	public void populate3rdPartyInstitutionForm() {
		OwnerType otype = assetUse.getAsset().getOwnerType();
		if (null != otype && otype.equals(OwnerType.PUBLIC_DOMAIN)) {
			setPublicDomain(true);
			assetUse.getAsset().setManaged(false);
			assetUse.getAsset().setRoyaltyFree(true);

		}

		if (null != otype && otype.equals(OwnerType.FAIR_USE)) {
			setFairUse(true);
			assetUse.getAsset().setManaged(false);
			assetUse.getAsset().setFeeRequired(false);
			assetUse.getAsset().setRoyaltyFree(true);
		}
	}

	public void populate3rdPartyAssetDetailsForm() {
		AssetUse au = getAssetUse();
		if (au.getAsset().getDescription().indexOf("*ENTER DESCRIPTION") > -1  || au.getAsset().getDescription().length() < 1) {
			au.getAsset().setDescription("");
			setIncludeAssetDescription(true);
		} else {
			setIncludeAssetDescription(false);
		}
		setHasBiblio (StringUtils.isNotBlank(au.getAsset().getCitation()) || StringUtils.isNotBlank(au.getAsset().getBiblio()));
	}

	public AssetUse populate3rdPartyAssetDetailsBean(AssetUse assetUse) throws Exception {
		populateBean(assetUse);
		assetUse.setCaption(this.assetUse.getCaption());
		assetUse.getAsset().setArtist(this.assetUse.getAsset().getArtist());
		assetUse.setColor(this.assetUse.isColor());
		assetUse.setSize(this.assetUse.getSize());
		assetUse.setMediaManager(this.assetUse.isMediaManager());  //SR_301213
		assetUse.setPagePosition(this.assetUse.getPagePosition());
		assetUse.setMediaReturnRequested(this.assetUse.isMediaReturnRequested());
		assetUse.getAsset().setModelRelease(this.assetUse.getAsset().getModelRelease());
		assetUse.getAsset().setPropertyRelease(this.assetUse.getAsset().isPropertyRelease());
		assetUse.setPermissionComment(this.assetUse.getPermissionComment());
		assetUse.getAsset().setFrom1(this.assetUse.getAsset().getFrom1());
		assetUse.getAsset().setFrom2(this.assetUse.getAsset().getFrom2());
		assetUse.getAsset().setFrom3(this.assetUse.getAsset().getFrom3());
		assetUse.getAsset().setTo1(this.assetUse.getAsset().getTo1());
		assetUse.getAsset().setTo2(this.assetUse.getAsset().getTo2());
		assetUse.getAsset().setTo3(this.assetUse.getAsset().getTo3());
		assetUse.getAsset().setDescription(this.assetUse.getAsset().getDescription());
		assetUse.getAsset().setWillBeRoyaltyFree(this.assetUse.getAsset().isWillBeRoyaltyFree());
		assetUse.getAsset().setWillBeWorkForHire(this.assetUse.getAsset().isWillBeWorkForHire());
		assetUse.getAsset().setStmGuidelines(this.assetUse.getAsset().isStmGuidelines());
		assetUse.getAsset().setManagerApproved(this.assetUse.getAsset().isManagerApproved());//Added to implement Build DM-374
		if (hasBiblio) {
			assetUse.getAsset().setCitation(this.assetUse.getAsset().getCitation());
			assetUse.getAsset().setBiblio(this.assetUse.getAsset().getBiblio());
		} else {
			assetUse.getAsset().setCitation(null);
			assetUse.getAsset().setBiblio(null);
		}
		return assetUse;
	}

	public AssetUse populate3rdPartyInstitutionBean(AssetUse assetUse) throws Exception {
		populateBean(assetUse);
		if (isPublicDomain ()) {
			assetUse.getAsset().setOwnerType(OwnerType.PUBLIC_DOMAIN);
			assetUse.getAsset().setManaged(false);
			assetUse.getAsset().setRoyaltyFree(true);
		}
		if (isFairUse ()) {
			assetUse.getAsset().setOwnerType(OwnerType.FAIR_USE);
			assetUse.getAsset().setManaged(false);
			assetUse.getAsset().setFeeRequired(false);
			assetUse.getAsset().setRoyaltyFree(true);
		}
		assetUse.setPermissionComment(this.assetUse.getPermissionComment());
		assetUse.getAsset().setDescription(this.assetUse.getAsset().getDescription());
		assetUse.getAsset().setCreditLine (this.assetUse.getAsset().getCreditLine());
		assetUse.getAsset().setVendorId (this.assetUse.getAsset().getVendorId());
		populate3rdPartyAssetDetailsBean (assetUse);
		return assetUse;
	}

	public AssetUse populate3rdPartyPublicationBean(AssetUse assetUse) throws Exception {
		populateBean(assetUse);
		Asset asset = assetUse.getAsset();
		if (isPublicDomain()) {
			asset.setOwnerType(OwnerType.PUBLIC_DOMAIN);
			asset.setManaged(false);
			asset.setRoyaltyFree(true);
		}
		if (isFairUse()) {
			asset.setOwnerType(OwnerType.FAIR_USE);
			asset.setManaged(false);
			asset.setFeeRequired(false);
			asset.setRoyaltyFree(true);
		}
		assetUse.setPermissionComment(this.assetUse.getPermissionComment());
		asset.setCreditLine (this.assetUse.getAsset().getCreditLine());
		asset.setOriginalArticleAuthor(this.assetUse.getAsset().getOriginalArticleAuthor());
		asset.setOriginalArticleTitle(this.assetUse.getAsset().getOriginalArticleTitle());
		asset.setOriginalPublicationAuthor(this.assetUse.getAsset().getOriginalPublicationAuthor());
		asset.setOriginalPublicationDate(this.assetUse.getAsset().getOriginalPublicationDate());
		asset.setOriginalPublicationTitle(this.assetUse.getAsset().getOriginalPublicationTitle());
		asset.setOriginalPublicationIsbn(this.assetUse.getAsset().getOriginalPublicationIsbn());
		asset.setOriginalFigureNumber(this.assetUse.getAsset().getOriginalFigureNumber());
		asset.setOriginalPageNumber(this.assetUse.getAsset().getOriginalPageNumber());
		asset.setDescription(this.assetUse.getAsset().getDescription());
		asset.setVendorId (this.assetUse.getAsset().getVendorId());
		populate3rdPartyAssetDetailsBean(assetUse);
		return assetUse;
	}

	public void populatePreviousEditionForm() {
		// previous Edition fields into form

		Integer currentEdition = 0;
		if (null != assetUse.getCommonWork().getPrimaryProduct().getEdition()) {
			currentEdition = assetUse.getCommonWork().getPrimaryProduct().getEdition().getEditionNumber();
		}

		setEdition(currentEdition + "");

		if (currentEdition == 0) return;

		ArrayList<LabelValueBean> workEditions = new ArrayList<LabelValueBean>();

		int max = 20;
		for (int x = currentEdition -1; x > 0; x--) {
			workEditions.add(new LabelValueBean("edition " + x, x + ""));
			max--;
			if (max == 0) break;
		}

		setEditions(workEditions);
	}


	public AssetUse populatePreviousEditionBean(AssetUse assetUse) {
		populateBean(assetUse);
		assetUse.getAsset().setOriginalPublicationIsbn(this.assetUse.getAsset().getOriginalPublicationIsbn());
		assetUse.getAsset().setOriginalPageNumber(this.assetUse.getAsset().getOriginalPageNumber());
		assetUse.getAsset().setOriginalFigureNumber(this.assetUse.getAsset().getOriginalFigureNumber());
		assetUse.setPermissionComment(this.assetUse.getPermissionComment());
		assetUse.getAsset().setMediaType(this.assetUse.getAsset().getMediaType());

		// according to pivotal 36280059
		assetUse.setPickupISBN(this.assetUse.getAsset().getOriginalPublicationIsbn());
		assetUse.setPickupPosition(this.assetUse.getPosition());
		assetUse.setPickupPage(this.assetUse.getAsset().getOriginalPageNumber());
		assetUse.setPickup(true);

		return assetUse;
	}

	public AssetUse populatePhotoRequestBean(AssetUse assetUse) {
		populateBean(assetUse);
		assetUse.setRequestComment(this.assetUse.getRequestComment());
		assetUse.getAsset().setDescription(this.assetUse.getAsset().getDescription());
		return assetUse;
	}

	public AssetUse populateWileyBean(AssetUse assetUse) {
		populateBean(assetUse);
		assetUse.setPickup(true);
		assetUse.setPickupComment(this.assetUse.getPickupComment());
		assetUse.setPickupISBN(this.assetUse.getPickupISBN());
		assetUse.setPickupPage(this.assetUse.getPickupPage());
		assetUse.setPickupPosition(this.assetUse.getPickupPosition());
		assetUse.setPickupIssueNumber(this.assetUse.getPickupIssueNumber());
		assetUse.setPickupIssueDate(this.assetUse.getPickupIssueDate());
		assetUse.setPickupTitle(this.assetUse.getPickupTitle());
		assetUse.setPickupAuthor(this.assetUse.getPickupAuthor());
		assetUse.getAsset().setDescription(this.assetUse.getAsset().getDescription());
		return assetUse;
	}

	public AssetUse populateAuthorProvidedBean(AssetUse assetUse) {
		populateBean(assetUse);
		// author provided fields
		assetUse.setCaption(this.assetUse.getCaption());
		assetUse.setColor(this.assetUse.isColor());
		assetUse.setMediaReturnRequested(this.assetUse.isMediaReturnRequested());
		assetUse.getAsset().setModelRelease(this.assetUse.getAsset().getModelRelease());
		assetUse.getAsset().setPropertyRelease(this.assetUse.getAsset().isPropertyRelease());
		assetUse.setPermissionComment(this.assetUse.getPermissionComment());
		assetUse.setSize(this.assetUse.getSize());
		assetUse.getAsset().setDescription(this.assetUse.getAsset().getDescription());

		// set Managed = T for all but Author provided
		assetUse.getAsset().setManaged(false);
		assetUse.getAsset().setRoyaltyFree(true);

		return assetUse;
	}

	public List<Source> getProfileSources() {
		return profileSources;
	}

	public void setProfileSources(List<Source> profileSources) {
		this.profileSources = profileSources;
	}

	public boolean isClear() {
		return clear;
	}

	public void setClear(boolean clear) {
		this.clear = clear;
	}
}
