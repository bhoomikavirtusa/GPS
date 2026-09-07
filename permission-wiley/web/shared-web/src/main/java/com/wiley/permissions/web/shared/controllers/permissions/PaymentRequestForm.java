package com.wiley.permissions.web.shared.controllers.permissions;

import java.util.Date;

import org.apache.commons.lang3.StringUtils;

import com.wiley.permissions.domain.persistence.permissions.Contract.PaymentType;
import com.wiley.permissions.domain.persistence.permissions.Source;

/**
 *
 * @author Nmedrano
 */
public class PaymentRequestForm {

	private String costCenter = null;
	private String accountNumberAndSubCode = null;
	private int contractId;
	private int sourceId;
	private int cwId;
	private int assetId;
	private String prevUrl;
	private Source source;
	private PaymentType paymentType;
	private Date paymentDate;
	private int checkNumber;	

	public PaymentRequestForm() {
		super();
	}

	public String getCostCenter() {
		return costCenter;
	}

	public void setCostCenter(String costCenter) {
		this.costCenter = costCenter;
	}

	public String getAccountNumberAndSubCode() {
		return accountNumberAndSubCode;
	}

	public void setAccountNumberAndSubCode(String accountNumberAndSubCode) {
		this.accountNumberAndSubCode = accountNumberAndSubCode;
	}

	public String getAccountNumber() {
		if (StringUtils.isBlank(accountNumberAndSubCode))  return null;

		int index = accountNumberAndSubCode.indexOf("-");
		String accountNumber = accountNumberAndSubCode.substring(0, index);
		return accountNumber;
	}

	public String getSubCode() {
		if (StringUtils.isBlank(accountNumberAndSubCode))  return null;

		int index = accountNumberAndSubCode.indexOf("-");
		String subCode = accountNumberAndSubCode.substring(index + 1, accountNumberAndSubCode.length());
		return subCode;
	}

	public int getCwId() {
		return cwId;
	}

	public void setCwId(int cwId) {
		this.cwId = cwId;
	}

	public int getAssetId() {
		return assetId;
	}

	public void setAssetId(int assetId) {
		this.assetId = assetId;
	}

	public String getPrevUrl() {
		return prevUrl;
	}

	public void setPrevUrl(String prevUrl) {
		this.prevUrl = prevUrl;
	}

	public Source getSource() {
		return source;
	}

	public void setSource(Source source) {
		this.source = source;
	}

	public int getContractId() {
		return contractId;
	}

	public void setContractId(int contractId) {
		this.contractId = contractId;
	}

	public int getSourceId() {
		return sourceId;
	}

	public void setSourceId(int sourceId) {
		this.sourceId = sourceId;
	}

	public PaymentType getPaymentType() {
		return paymentType;
	}

	public void setPaymentType(PaymentType paymentType) {
		this.paymentType = paymentType;
	}

	public Date getPaymentDate() {
		if (null == paymentDate)
			return new Date();
		else
			return paymentDate;
	}

	public void setPaymentDate(Date paymentDate) {
		this.paymentDate = paymentDate;
	}
	
	public int getCheckNumber() {
		return checkNumber;
	}

	public void setCheckNumber(int checkNumber) {
		this.checkNumber = checkNumber;
	}
}
