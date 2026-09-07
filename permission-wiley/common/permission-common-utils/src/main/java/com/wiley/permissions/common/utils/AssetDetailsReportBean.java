package com.wiley.permissions.common.utils;

import java.util.Comparator;


public class AssetDetailsReportBean {


	private String sortOrder ;
	private String componentName;
	private String assetUsage;
	private String assetPosition;
	private String manuScriptPage;
	private String mediaType;
	private String decscription;
	private String creditLine;
	private String sourceNames;
	private String sourceRef;
	private String finalPage;
	private String permStatusCode;
	private String contractNumber;
	private String contractDate;
	private String reuse;
	private Double estimatedCost;
	private Double finalCost;
	private String mutiUsageAsset;
	private String cRoyalyFree;
	private String pickup;
	private String cameraCopyToCome;
	private String sentToProduction;
	private String permissionComment;

	public String getSortOrder() {
		return sortOrder;
	}
	public String getComponentName() {
		return componentName;
	}
	public String getAssetUsage() {
		return assetUsage;
	}
	public String getAssetPosition() {
		return assetPosition;
	}
	public String getManuScriptPage() {
		return manuScriptPage;
	}
	public String getMediaType() {
		return mediaType;
	}
	public String getDecscription() {
		return decscription;
	}
	public String getCreditLine() {
		return creditLine;
	}
	public String getSourceNames() {
		return sourceNames;
	}
	public String getSourceRef() {
		return sourceRef;
	}
	public String getFinalPage() {
		return finalPage;
	}
	public String getPermStatusCode() {
		return permStatusCode;
	}
	public String getContractNumber() {
		return contractNumber;
	}
	public String getContractDate() {
		return contractDate;
	}
	public String getReuse() {
		return reuse;
	}
	public Double getEstimatedCost() {
		return estimatedCost;
	}
	public Double getFinalCost() {
		return finalCost;
	}
	public String getMutiUsageAsset() {
		return mutiUsageAsset;
	}
	public String getcRoyalyFree() {
		return cRoyalyFree;
	}
	public String getPickup() {
		return pickup;
	}
	public String getCameraCopyToCome() {
		return cameraCopyToCome;
	}
	public String getSentToProduction() {
		return sentToProduction;
	}
	public String getPermissionComment() {
		return permissionComment;
	}
	public void setSortOrder(String sortOrder) {
		this.sortOrder = sortOrder;
	}
	public void setComponentName(String componentName) {
		this.componentName = componentName;
	}
	public void setAssetUsage(String assetUsage) {
		this.assetUsage = assetUsage;
	}
	public void setAssetPosition(String assetPosition) {
		this.assetPosition = assetPosition;
	}
	public void setManuScriptPage(String manuScriptPage) {
		this.manuScriptPage = manuScriptPage;
	}
	public void setMediaType(String mediaType) {
		this.mediaType = mediaType;
	}
	public void setDecscription(String decscription) {
		this.decscription = decscription;
	}
	public void setCreditLine(String creditLine) {
		this.creditLine = creditLine;
	}
	public void setSourceNames(String sourceNames) {
		this.sourceNames = sourceNames;
	}
	public void setSourceRef(String sourceRef) {
		this.sourceRef = sourceRef;
	}
	public void setFinalPage(String finalPage) {
		this.finalPage = finalPage;
	}
	public void setPermStatusCode(String permStatusCode) {
		this.permStatusCode = permStatusCode;
	}
	public void setContractNumber(String contractNumber) {
		this.contractNumber = contractNumber;
	}
	public void setContractDate(String contractDate) {
		this.contractDate = contractDate;
	}
	public void setReuse(String reuse) {
		this.reuse = reuse;
	}
	public void setEstimatedCost(Double d) {
		this.estimatedCost = d;
	}
	public void setFinalCost(Double finalCost2) {
		this.finalCost = finalCost2;
	}
	public void setMutiUsageAsset(String mutiUsageAsset) {
		this.mutiUsageAsset = mutiUsageAsset;
	}
	public void setcRoyalyFree(String cRoyalyFree) {
		this.cRoyalyFree = cRoyalyFree;
	}
	public void setPickup(String pickup) {
		this.pickup = pickup;
	}
	public void setCameraCopyToCome(String cameraCopyToCome) {
		this.cameraCopyToCome = cameraCopyToCome;
	}
	public void setSentToProduction(String sentToProduction) {
		this.sentToProduction = sentToProduction;
	}
	public void setPermissionComment(String permissionComment) {
		this.permissionComment = permissionComment;
	}


	public static Comparator<AssetDetailsReportBean> finalCostComparator = new Comparator<AssetDetailsReportBean>() {

        @Override
        public int compare(AssetDetailsReportBean e1, AssetDetailsReportBean e2) {
            return (int)(e1.getFinalCost() - e2.getFinalCost());
        }
    };

}
