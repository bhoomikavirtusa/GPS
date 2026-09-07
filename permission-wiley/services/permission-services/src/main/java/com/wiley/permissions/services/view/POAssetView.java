package com.wiley.permissions.services.view;

import java.util.Date;

import com.wiley.permissions.domain.DomainObject;

/**
 * Used by poRepository().loadPOAssetsViewForSource()
 * @author lnagy
 *
 */
public class POAssetView
extends DomainObject
{
	private static final long serialVersionUID = 1L;

	private int assetId = 0;
	private String assetDescription;
	private String mediaTypeCode;
	private String usageTypeCode;
	private String position;
	private Integer poId;
	private Date poDate;
	private String componentName;
	private Integer componentId;
	private boolean selected;
	private Double price;
	private String creditLine;
	private Integer rfDealId;
	private String vendorId;
	private String conditionCreditLine;
	private boolean noCrop;
	private boolean noBleed;
	private boolean isRoyaltyFree;
	private boolean willBeWorkForHire;
	private boolean willBeRoyaltyFree;
	private boolean hasAssetFiles; //Added for implementing DM-117
	private boolean hasAssetLevelRFDeal; //Added for implementing DM-122


	public POAssetView() {
	}

	public boolean getWillBeWorkForHire() {
		return willBeWorkForHire;
	}

	public void setWillBeWorkForHire(Boolean willBeWorkForHire) {
		this.willBeWorkForHire = willBeWorkForHire;
	}

	public boolean getWillBeRoyaltyFree() {
		return willBeRoyaltyFree;
	}

	public void setWillBeRoyaltyFree(Boolean willBeRoyaltyFree) {
		this.willBeRoyaltyFree = willBeRoyaltyFree;
	}


	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	public int getAssetId() {
		return assetId;
	}

	public void setAssetId(int assetId) {
		this.assetId = assetId;
	}

	public String getAssetDescription() {
		return assetDescription;
	}

	public void setAssetDescription(String assetDescription) {
		this.assetDescription = assetDescription;
	}

	public String getMediaTypeCode() {
		return mediaTypeCode;
	}

	public void setMediaTypeCode(String mediaTypeCode) {
		this.mediaTypeCode = mediaTypeCode;
	}

	public String getUsageTypeCode() {
		return usageTypeCode;
	}

	public void setUsageTypeCode(String usageTypeCode) {
		this.usageTypeCode = usageTypeCode;
	}

	public String getPosition() {
		return position;
	}

	public void setPosition(String position) {
		this.position = position;
	}

	public Integer getPoId() {
		return poId;
	}

	public void setPoId(Integer poId) {
		this.poId = poId;
	}

	public Date getPoDate() {
		return poDate;
	}

	public void setPoDate(Date poDate) {
		this.poDate = poDate;
	}

	public String getComponentName() {
		return componentName;
	}

	public void setComponentName(String componentName) {
		this.componentName = componentName;
	}

	public Integer getComponentId() {
		return componentId;
	}

	public void setComponentId(Integer componentId) {
		this.componentId = componentId;
	}

	public Double getPrice() {
		return price;
	}

	public void setPrice(Double price) {
		this.price = price;
	}

	public String getCreditLine() {
		return creditLine;
	}

	public void setCreditLine(String creditLine) {
		this.creditLine = creditLine;
	}

	public Integer getRfDealId() {
		return rfDealId;
	}

	public void setRfDealId(Integer rfdealId) {
		this.rfDealId = rfdealId;
	}

	public String getVendorId() {
		return vendorId;
	}

	public void setVendorId(String vendorId) {
		this.vendorId = vendorId;
	}

	public String getConditionCreditLine() {
		return conditionCreditLine;
	}

	public void setConditionCreditLine(String conditionCreditLine) {
		this.conditionCreditLine = conditionCreditLine;
	}

	public boolean isNoCrop() {
		return noCrop;
	}

	public void setNoCrop(boolean b) {
		noCrop = b;
	}

	public boolean isNoBleed() {
		return noBleed;
	}

	public void setNoBleed(boolean b) {
		noBleed = b;
	}

	public boolean isRoyaltyFree() {
		return isRoyaltyFree;
	}

	public void setRoyaltyFree(boolean isRoyaltyFree) {
		this.isRoyaltyFree = isRoyaltyFree;
	}

	public boolean isHasAssetFiles() {
		return hasAssetFiles;
	}

	public void setHasAssetFiles(boolean hasAssetFiles) {
		this.hasAssetFiles = hasAssetFiles;
	}

	public boolean isHasAssetLevelRFDeal() {
		return hasAssetLevelRFDeal;
	}

	public void setHasAssetLevelRFDeal(boolean hasAssetLevelRFDeal) {
		this.hasAssetLevelRFDeal = hasAssetLevelRFDeal;
	}
}
