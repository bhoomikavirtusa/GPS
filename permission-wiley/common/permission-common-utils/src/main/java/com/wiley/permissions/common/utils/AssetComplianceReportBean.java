package com.wiley.permissions.common.utils;

import java.util.Comparator;

public class AssetComplianceReportBean {

	private String component ;
	private String usage;
	private String position;
	private String description;
	private String status;
	private String creditLine;
	private String sourceName;
	private String sourceRef;
	private String invoiceOrPermLetter;
	private String invoiceOrPermDate;
	private double finalCost;
	private String mutiUsageAsset;
	private String checkNumber;
	private String checkDate;
	private String royaltyFree;
	private String seatsvalue;
	private String data1;
	private String comments;

	public String getComponent() {
		return component;
	}
	public String getUsage() {
		return usage;
	}
	public String getPosition() {
		return position;
	}
	public String getDescription() {
		return description;
	}
	public String getStatus() {
		return status;
	}
	public String getCreditLine() {
		return creditLine;
	}
	public String getSourceName() {
		return sourceName;
	}
	public String getSourceRef() {
		return sourceRef;
	}
	public String getInvoiceOrPermLetter() {
		return invoiceOrPermLetter;
	}
	public String getInvoiceOrPermDate() {
		return invoiceOrPermDate;
	}
	public double getFinalCost() {
		return finalCost;
	}
	public String getMutiUsageAsset() {
		return mutiUsageAsset;
	}
	public String getCheckNumber() {
		return checkNumber;
	}
	public String getCheckDate() {
		return checkDate;
	}
	public String getRoyaltyFree() {
		return royaltyFree;
	}
	public String getSeatsvalue() {
		return seatsvalue;
	}
	public String getData1() {
		return data1;
	}
	public String getComments() {
		return comments;
	}
	public void setComponent(String component) {
		this.component = component;
	}
	public void setUsage(String usage) {
		this.usage = usage;
	}
	public void setPosition(String position) {
		this.position = position;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public void setCreditLine(String creditLine) {
		this.creditLine = creditLine;
	}
	public void setSourceName(String sourceName) {
		this.sourceName = sourceName;
	}
	public void setSourceRef(String sourceRef) {
		this.sourceRef = sourceRef;
	}
	public void setInvoiceOrPermLetter(String invoiceOrPermLetter) {
		this.invoiceOrPermLetter = invoiceOrPermLetter;
	}
	public void setInvoiceOrPermDate(String invoiceOrPermDate) {
		this.invoiceOrPermDate = invoiceOrPermDate;
	}
	public void setFinalCost(double finalCost) {
		this.finalCost = finalCost;
	}
	public void setMutiUsageAsset(String mutiUsageAsset) {
		this.mutiUsageAsset = mutiUsageAsset;
	}
	public void setCheckNumber(String checkNumber) {
		this.checkNumber = checkNumber;
	}
	public void setCheckDate(String checkDate) {
		this.checkDate = checkDate;
	}
	public void setRoyaltyFree(String royaltyFree) {
		this.royaltyFree = royaltyFree;
	}
	public void setSeatsvalue(String seatsvalue) {
		this.seatsvalue = seatsvalue;
	}
	public void setData1(String data1) {
		this.data1 = data1;
	}
	public void setComments(String comments) {
		this.comments = comments;
	}

	public static Comparator<AssetComplianceReportBean> finalCostComparator =  new Comparator<AssetComplianceReportBean>() {

		@Override
		public int compare(AssetComplianceReportBean o1,
				AssetComplianceReportBean o2) {
			// TODO Auto-generated method stub
			return (int)(o1.getFinalCost()-o2.getFinalCost());
		}
	};
}
