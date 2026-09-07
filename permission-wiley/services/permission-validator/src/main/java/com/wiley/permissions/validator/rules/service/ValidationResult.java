package com.wiley.permissions.validator.rules.service;

import com.wiley.permissions.domain.persistence.permissions.PermissionStatus;

/**
 * Encapsulates the results from the rules engine.
 */
public class ValidationResult {

	private PermissionStatus status;
	private boolean needPaymentRequest = false;
	private boolean paid = false;
	private String explanation;
	private Integer poId;
	private Integer contractId;


	public ValidationResult() {

	}

	public ValidationResult(PermissionStatus status) {
		this.status = status;
	}

	public PermissionStatus getStatus() {
		return status;
	}

	public void setStatus(PermissionStatus status) {
		this.status = status;
	}

	public boolean isNeedPaymentRequest() {
		return needPaymentRequest;
	}

	public void setNeedPaymentRequest(boolean needPaymentRequest) {
		this.needPaymentRequest = needPaymentRequest;
	}

	public boolean isPaid() {
		return paid;
	}

	public void setPaid(boolean paid) {
		this.paid = paid;
	}

	public String getExplanation() {
		return explanation;
	}

	public void setExplanation(String explanation) {
		this.explanation = explanation;
	}

	public Integer getPoId() {
		return poId;
	}

	public void setPoId(Integer poId) {
		this.poId = poId;
	}

	public Integer getContractId() {
		return contractId;
	}

	public void setContractId(Integer contractId) {
		this.contractId = contractId;
	}

	@Override
	public String toString() {
		return "Status = " + status + ", needPaymentRequest = " + needPaymentRequest
		    + ",\nexplanation = " + explanation;
	}
}
