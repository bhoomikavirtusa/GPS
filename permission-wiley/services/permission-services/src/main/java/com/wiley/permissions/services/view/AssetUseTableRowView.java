package com.wiley.permissions.services.view;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.wiley.permissions.domain.persistence.permissions.AssetUse;
import com.wiley.permissions.domain.persistence.permissions.Component;
import com.wiley.permissions.domain.persistence.permissions.GbpmCategory;
import com.wiley.permissions.domain.persistence.permissions.MediaType;
import com.wiley.permissions.domain.persistence.permissions.Source;
import com.wiley.permissions.domain.persistence.permissions.Usage;
import com.wiley.permissions.services.AssetUseSearchResult;

@XmlRootElement
public class AssetUseTableRowView {

	private String description;
	private int assetUseId;
	private String position;
	private String mediaType;
	private String manuscriptPage;
	private String componentName;
	private String usage;
	private String permissionStatus;
	private String sourcesAsHTMLLinks;
	private int cwId;
	private String mediaTypeCode;
	private String usageCode;
	private int componentId;
	private int componentSortOrder;
	private boolean isCanceled;
	private List<Component> components;
	private List<MediaType> mediaTypes;
	private List<Usage> usages;
	private List<GbpmCategory> gbpmCategories;
	private List<Source> sources;
	private String finalPage;
	private String sourceRefNumber;
	private String creditLine;
	private boolean pickup;
	private boolean reusedFromPrevEdtn;
	private boolean mediaManager;
	private boolean needsPaymentRequest;
	private boolean paid;
	private boolean sentToProduction;
	private String ownerTypeCode;
	private boolean royaltyFree;
	private boolean cameraCopyToCome;
	private boolean custom;
	private String permissionComment;
	private String sortOrder;
	private boolean hasFiles;
	private int replacementId;
	private int creatorGroupId;
	private boolean hasComments = false;
	private boolean hasThumbnail = false;
	private boolean hasContracts = false;
	private Integer gbpmCategory;
	private boolean reviewedOrAuthorUnknown = false;
	private String artist;
	private boolean noFlyMatchApproved = false;

	public AssetUseTableRowView() {
	}

	public AssetUseTableRowView(AssetUse au) {
		this.description = au.getAsset().getDescription();
		this.assetUseId = au.getId();
		this.position = au.getPosition();
		this.isCanceled = au.isCanceled();
		if (null != au.getAsset().getMediaType())
			this.mediaType = au.getAsset().getMediaType().getDescription();
		this.manuscriptPage = au.getManuscriptPage();
		this.componentName = au.getComponentName();
		this.usage = au.getUsage().getDescription();
		this.setSources(au.getAsset().getSources());
		this.sourcesAsHTMLLinks = au.getAsset().getSourcesAsHTML();
		if (null != au.getStatus())
			this.permissionStatus = au.getStatus().getDescription();
		this.cwId = au.getCommonWork().getId();
		if (null != au.getComponent()) {
			this.componentId = au.getComponent().getId();
			this.componentSortOrder = au.getComponent().getSortOrder();
		}
		if (null != au.getAsset().getMediaType())
			this.mediaTypeCode = au.getAsset().getMediaType().getCode();
		if (null != au.getUsage())
			this.usageCode = au.getUsage().getCode();
		this.creditLine = au.getCreditLine();
		this.finalPage = au.getFinalPage();
		this.sourceRefNumber = au.getAsset().getVendorId();
		this.pickup = au.isPickup();
		this.mediaManager = au.isMediaManager();
		this.reusedFromPrevEdtn = au.isReusedFromPreviousEdition();
		this.needsPaymentRequest = au.isNeedPaymentRequest();
		this.paid = au.isPaid();
		this.sentToProduction = au.isSentToProduction();
		if (null != au.getAsset().getOwnerType())
			this.ownerTypeCode = au.getAsset().getOwnerType().getCode();
		this.royaltyFree = au.getAsset().isRoyaltyFree();
		this.cameraCopyToCome = au.isCameraCopyToCome();
		this.custom = au.isCustom();
		this.permissionComment = au.getPermissionComment();
		this.sortOrder = au.getSortOrder();
		if (null != au.getCancelReplacement())
			this.replacementId = au.getCancelReplacement().getId();
		if (null != au.getAsset().getCreatedUser().getGroup())
			this.creatorGroupId = au.getCreatedUser().getGroup().getId();
		if (null != au.getPermissionComment() || null != au.getProductionComment()) {
			this.setHasComments(true);
		} else {
			this.setHasComments(false);
		}
		if (au.getAsset().getFiles().size() > 0) {
			this.setHasThumbnail(true);
		}
		this.gbpmCategory = au.getGbpmCategory();
		this.reviewedOrAuthorUnknown = au.getAsset().isReviewedUnknown() || au.getAsset().isAuthorProvidedUnknown();
		this.artist = au.getAsset().getArtist();
		this.noFlyMatchApproved = au.isNoFlyMatchApproved();
	}

	public AssetUseTableRowView(AssetUseSearchResult au) {
		this.description = au.getDescription();
		this.assetUseId = new Integer (au.getAssetUseId());
		this.position = au.getPosition();
		this.mediaType = au.getMediaType();
		this.manuscriptPage = au.getManuscriptPage();
		this.componentName = au.getComponentName();
		this.componentId = au.getComponentId() == null ? 0 : au.getComponentId();
		this.usage = au.getUsage();
		this.isCanceled = au.isCanceled();
		this.permissionStatus = au.getPermissionStatusDescription();
		this.setSources(au.getSources());
		this.setSourcesAsHTMLLinks(au.getSourcesAsHTML());
		this.cwId = new Integer(au.getCommonWorkId());
		this.creditLine = au.getCreditLine();
		this.finalPage = au.getFinalPage();
		this.sourceRefNumber = au.getSourceRef();
		this.pickup = au.isPickup();
		this.mediaManager = au.isMediaManager();
		this.reusedFromPrevEdtn = au.isReuse();
		this.needsPaymentRequest = au.isNeedPaymentRequest();
		this.paid = au.isPaid();
		this.sentToProduction = au.isSentToProduction();
		this.ownerTypeCode = au.getOwnerTypeCode();
		this.royaltyFree = au.isRoyaltyFree();
		this.cameraCopyToCome = au.isCameraCopyToCome();
		this.custom = au.isCustom();
		this.permissionComment = au.getPermissionComment();
		this.sortOrder = au.getSortOrder();
		this.hasFiles = au.isHasFiles();
		this.replacementId = au.getReplacementId() == null ? 0 : au.getReplacementId();
		this.creatorGroupId = au.getCreatedGroupId() == null ? 0 : au.getCreatedGroupId();
		this.setHasComments( au.getHasComments());
		this.setHasThumbnail(au.isHasAssetFiles());
		this.setHasContracts(au.isHasContracts());
		this.gbpmCategory = (au.getGbpmCategory());
		this.reviewedOrAuthorUnknown = au.isReviewedOrAuthorUnknown();
		this.artist = au.getArtist();
		this.noFlyMatchApproved = au.isNoFlyMatchApproved();
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public int getAssetUseId() {
		return assetUseId;
	}

	public void setAssetUseId(int assetUseId) {
		this.assetUseId = assetUseId;
	}

	public String getPosition() {
		return position;
	}

	public void setPosition(String position) {
		this.position = position;
	}

	public String getMediaType() {
		return mediaType;
	}

	public void setMediaType(String mediaType) {
		this.mediaType = mediaType;
	}

	public String getSourcesAsHTMLLinks() {
		return this.sourcesAsHTMLLinks;
	}

	public void setSourcesAsHTMLLinks(String sourcesAsHTMLLinks) {
		this.sourcesAsHTMLLinks = sourcesAsHTMLLinks;
	}

	public String getManuscriptPage() {
		return manuscriptPage;
	}

	public void setManuscriptPage(String manuscriptPage) {
		this.manuscriptPage = manuscriptPage;
	}

	public String getComponentName() {
		return "<span title=\"" + getComponentSortOrder() + "\"></span>" + (StringUtils.isNotBlank(componentName) ? componentName : "");
	}

	public void setComponentName(String componentName) {
		this.componentName = componentName;
	}

	public String getUsage() {
		return usage;
	}

	public void setUsage(String usage) {
		this.usage = usage;
	}

	public String getPermissionStatus() {
		return permissionStatus;
	}

	public void setPermissionStatus(String permissionStatus) {
		this.permissionStatus = permissionStatus;
	}

	public List<Source> getSources() {
		return sources;
	}

	public void setSources(List<Source> sources) {
		this.sources = sources;
	}

	public List<Component> getComponents() {
		return components;
	}

	public void setComponents(List<Component> components) {
		this.components = components;
	}

	public List<MediaType> getMediaTypes() {
		return mediaTypes;
	}

	/**
	 * Needs to accept List<?> so can be called after get
	 * generic List out of ReferenceDataCache.
	 */
	@SuppressWarnings("unchecked")
	public void setMediaTypes(List<?> mediaTypes) {
		this.mediaTypes = (List<MediaType>) mediaTypes;
	}

	public int getCwId() {
		return cwId;
	}

	public void setCwId(int cwId) {
		this.cwId = cwId;
	}

	public List<Usage> getUsages() {
		return usages;
	}

	/**
	 * Needs to accept List<?> so can be called after get
	 * generic List out of ReferenceDataCache.
	 */
	@SuppressWarnings("unchecked")
	public void setUsages(List<?> usages) {
		this.usages = (List<Usage>) usages;
	}

	public List<GbpmCategory> getGbpmCategories() {
		return gbpmCategories;
	}

	/**
	 * Needs to accept List<?> so can be called after get
	 * generic List out of ReferenceDataCache.
	 */
	@SuppressWarnings("unchecked")
	public void setGbpmCategories(List<?> gbpmCategories) {
		this.gbpmCategories = (List<GbpmCategory>) gbpmCategories;
	}

	public String getMediaTypeCode() {
		return mediaTypeCode;
	}

	public void setMediaTypeCode(String mediaTypeCode) {
		this.mediaTypeCode = mediaTypeCode;
	}

	public String getUsageCode() {
		return usageCode;
	}

	public void setUsageCode(String usageCode) {
		this.usageCode = usageCode;
	}

	public int getComponentId() {
		return componentId;
	}

	public void setComponentId(int componentId) {
		this.componentId = componentId;
	}

	public boolean isCanceled() {
		return isCanceled;
	}

	public void setCanceled(boolean isCanceled) {
		this.isCanceled = isCanceled;
	}

	public boolean isRoyaltyFree() {
		return royaltyFree;
	}

	public void setRoyaltyFree(boolean isRoyaltyFree) {
		this.royaltyFree = isRoyaltyFree;
	}

	public boolean isCameraCopyToCome() {
		return cameraCopyToCome;
	}

	public void setCameraCopyToCome(boolean isCameraCopyToCome) {
		this.cameraCopyToCome = isCameraCopyToCome;
	}

	public String getFinalPage() {
		return finalPage;
	}

	public void setFinalPage(String finalPage) {
		this.finalPage = finalPage;
	}

	public String getSourceRefNumber() {
		return sourceRefNumber;
	}

	public void setSourceRefNumber(String sourceRefNumber) {
		this.sourceRefNumber = sourceRefNumber;
	}

	public String getCreditLine() {
		return creditLine;
	}

	public void setCreditLine(String creditLine) {
		this.creditLine = creditLine;
	}

	public boolean isPickup() {
		return pickup;
	}

	public void setPickup(boolean pickup) {
		this.pickup = pickup;
	}


	public boolean isMediaManager() {
		return mediaManager;
	}

	public void setMediaManager(boolean mediaManager) {
		this.mediaManager = mediaManager;
	}

	public boolean isReusedFromPrevEdtn() {
		return reusedFromPrevEdtn;
	}

	public void setReusedFromPrevEdtn(boolean reusedFromPrevEdtn) {
		this.reusedFromPrevEdtn = reusedFromPrevEdtn;
	}

	public boolean isNeedsPaymentRequest() {
		return needsPaymentRequest;
	}

	public void setNeedsPaymentRequest(boolean needsPaymentRequest) {
		this.needsPaymentRequest = needsPaymentRequest;
	}

	public boolean isPaid() {
		return paid;
	}

	public void setPaid(boolean paid) {
		this.paid = paid;
	}

	public boolean isSentToProduction() {
		return sentToProduction;
	}

	public void setSentToProduction(boolean sentToProduction) {
		this.sentToProduction = sentToProduction;
	}

	public String getOwnerTypeCode() {
		return ownerTypeCode;
	}

	public void setOwnerTypeCode(String ownerTypeCode) {
		this.ownerTypeCode = ownerTypeCode;
	}

	public int getComponentSortOrder() {
		return componentSortOrder;
	}

	public void setComponentSortOrder(int componentSortOrder) {
		this.componentSortOrder = componentSortOrder;
	}

	public boolean isCustom() {
		return custom;
	}

	public void setCustom(boolean custom) {
		this.custom = custom;
	}

	public String getPermissionComment() {
		return permissionComment;
	}

	public void setPermissionComment(String permissionComment) {
		this.permissionComment = permissionComment;
	}

	public Boolean isHasComments() {
		return hasComments;
	}

	public void setHasComments(Boolean hasComments) {
		this.hasComments = hasComments;
	}

	public Boolean isHasThumbnail() {
		return hasThumbnail;
	}

	public void setHasThumbnail(Boolean hasThumbnail) {
		this.hasThumbnail = hasThumbnail;
	}


	public String getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(String sortOrder) {
		this.sortOrder = sortOrder;
	}

	public boolean isHasFiles() {
		return hasFiles;
	}

	public void setHasFiles(boolean hasFiles) {
		this.hasFiles = hasFiles;
	}

	public int getReplacementId() {
    	return replacementId;
    }

	public void setReplacementId(int replacementId) {
    	this.replacementId = replacementId;
    }

	public int getCreatorGroupId() {
		return creatorGroupId;
	}

	public void setCreatorGroupId(int creatorGroupId) {
		this.creatorGroupId = creatorGroupId;
	}

	public boolean isHasContracts() {
		return hasContracts;
	}

	public void setHasContracts(boolean hasContracts) {
		this.hasContracts = hasContracts;
	}

	// lnagy - have this method return a boolean.
	// We have the replacementId also in case we might need it for something else
	public boolean isReplaced() {
    	return replacementId != 0;
    }

	public Integer getGbpmCategory() {
		return gbpmCategory;
	}

	public void setGbpmCategory(Integer gbpmCategory) {
		this.gbpmCategory = gbpmCategory;
	}

	public boolean isReviewedOrAuthorUnknown() {
		return reviewedOrAuthorUnknown;
	}

	public void setReviewedOrAuthorUnknown(boolean b) {
		reviewedOrAuthorUnknown = b;
	}

	public boolean isDisabled() {
		if (CollectionUtils.isEmpty(sources))
			return false;
		for (Source source : sources) {
			if (source.isNofly())
				return true;
		}
		return false;
	}

	public String getArtist() {
		return artist;
	}

	public void setArtist(String artist) {
		this.artist = artist;
	}

	public boolean isNoFlyMatchApproved() {
		return noFlyMatchApproved;
	}

	public void setNoFlyMatchApproved(boolean noFlyMatchApproved) {
		this.noFlyMatchApproved = noFlyMatchApproved;
	}
}
