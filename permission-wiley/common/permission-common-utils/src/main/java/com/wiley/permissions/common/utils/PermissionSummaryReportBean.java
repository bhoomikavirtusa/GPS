package com.wiley.permissions.common.utils;

import java.util.Comparator;

public class PermissionSummaryReportBean {

	private String description ;
	private String creditLine;
	private String sourceNames;
	private String sourceRef;
	private String componentName;
	private String position;
	private String mediaType;
	private String usage;
	private String finalPage;
	private String permissionStatus;
	private String editions;
	private String medium;
	private String territory;
	private String languages;
	private String totalPrintRun;
	private String seats;
	private String derivatives;
	private String licenseFlag;
	private String invNumber;
	private String invStartDate;
	private String invExpireDate;
	private String invoicedDate;
	private String estimatedCost;
	private double finalCost;
	private String multiUsageAsset;
	private String gbpmCategory;

	public String getDescription() {
		return description;
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
	public String getComponentName() {
		return componentName;
	}
	public String getPosition() {
		return position;
	}
	public String getMediaType() {
		return mediaType;
	}
	public String getUsage() {
		return usage;
	}
	public String getFinalPage() {
		return finalPage;
	}
	public String getPermissionStatus() {
		return permissionStatus;
	}
	public String getEditions() {
		return editions;
	}
	public String getMedium() {
		return medium;
	}
	public String getTerritory() {
		return territory;
	}
	public String getLanguages() {
		return languages;
	}
	public String getTotalPrintRun() {
		return totalPrintRun;
	}
	public String getSeats() {
		return seats;
	}
	public String getDerivatives() {
		return derivatives;
	}
	public String getLicenseFlag() {
		return licenseFlag;
	}
	public String getInvNumber() {
		return invNumber;
	}
	public String getInvStartDate() {
		return invStartDate;
	}
	public String getInvExpireDate() {
		return invExpireDate;
	}
	public String getInvoicedDate() {
		return invoicedDate;
	}
	public String getEstimatedCost() {
		return estimatedCost;
	}
	public double getFinalCost() {
		return finalCost;
	}
	public String getMultiUsageAsset() {
		return multiUsageAsset;
	}
	public String getGbpmCategory() {
		return gbpmCategory;
	}
	public void setDescription(String description) {
		this.description = description;
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
	public void setComponentName(String componentName) {
		this.componentName = componentName;
	}
	public void setPosition(String position) {
		this.position = position;
	}
	public void setMediaType(String mediaType) {
		this.mediaType = mediaType;
	}
	public void setUsage(String usage) {
		this.usage = usage;
	}
	public void setFinalPage(String finalPage) {
		this.finalPage = finalPage;
	}
	public void setPermissionStatus(String permissionStatus) {
		this.permissionStatus = permissionStatus;
	}
	public void setEditions(String editions) {
		this.editions = editions;
	}
	public void setMedium(String medium) {
		this.medium = medium;
	}
	public void setTerritory(String territory) {
		this.territory = territory;
	}
	public void setLanguages(String languages) {
		this.languages = languages;
	}
	public void setTotalPrintRun(String totalPrintRun) {
		this.totalPrintRun = totalPrintRun;
	}
	public void setSeats(String seats) {
		this.seats = seats;
	}
	public void setDerivatives(String derivatives) {
		this.derivatives = derivatives;
	}
	public void setLicenseFlag(String licenseFlag) {
		this.licenseFlag = licenseFlag;
	}
	public void setInvNumber(String invNumber) {
		this.invNumber = invNumber;
	}
	public void setInvStartDate(String invStartDate) {
		this.invStartDate = invStartDate;
	}
	public void setInvExpireDate(String invExpireDate) {
		this.invExpireDate = invExpireDate;
	}
	public void setInvoicedDate(String invoicedDate) {
		this.invoicedDate = invoicedDate;
	}
	public void setEstimatedCost(String estimatedCost) {
		this.estimatedCost = estimatedCost;
	}
	public void setFinalCost(double finalCost) {
		this.finalCost = finalCost;
	}
	public void setMultiUsageAsset(String multiUsageAsset) {
		this.multiUsageAsset = multiUsageAsset;
	}
	public void setGbpmCategory(String gbpmCategory) {
		this.gbpmCategory = gbpmCategory;
	}

	public static Comparator<PermissionSummaryReportBean> finalCostComparator = new Comparator<PermissionSummaryReportBean>() {
		@Override
		public int compare(PermissionSummaryReportBean o1,PermissionSummaryReportBean o2) {
			return (int) (o1.getFinalCost()-o2.getFinalCost());
		}
	};

}
