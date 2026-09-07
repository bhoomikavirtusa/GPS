package com.wiley.permissions.domain.persistence.permissions;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.PersistenceUnit;
import javax.persistence.Table;

@Entity
@PersistenceUnit(unitName = "permissions")
@Table(name = "ASSET_PERM_REF")
public class AssetPermissionRef {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID", nullable = false, unique = true)
	private Integer id = null;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ASSET_ID", nullable = false)
	private Asset asset = null;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ASSET_USE_ID", nullable = false)
	private AssetUse assetUse = null;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "CW_ID", nullable = false)
	private CommonWork commonWork = null;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "SOURCE_ID", nullable = true)
	private Source source = null;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "PO_ID", nullable = true)
	private PurchaseOrder purchaseOrder = null;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "CONTRACT_ID", nullable = true)
	private Contract contract = null;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "PAYMENT_REQUEST_ID", nullable = true)
	private PaymentRequest paymentRequest = null;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "PERMISSION_STATUS", nullable = true)
	private PermissionStatus permissionStatus = null;

	@Column(name = "STATUS_EXPLANATION", nullable = true)
	private String statusExplanation = null;


	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Asset getAsset() {
		return asset;
	}

	public void setAsset(Asset asset) {
		this.asset = asset;
	}

	public AssetUse getAssetUse() {
		return assetUse;
	}

	public void setAssetUse(AssetUse assetUse) {
		this.assetUse = assetUse;
	}

	public CommonWork getCommonWork() {
		return commonWork;
	}

	public void setCommonWork(CommonWork commonWork) {
		this.commonWork = commonWork;
	}

	public Source getSource() {
		return source;
	}

	public void setSource(Source source) {
		this.source = source;
	}

	public PurchaseOrder getPurchaseOrder() {
		return purchaseOrder;
	}

	public void setPurchaseOrder(PurchaseOrder purchaseOrder) {
		this.purchaseOrder = purchaseOrder;
	}

	public Contract getContract() {
		return contract;
	}

	public void setContract(Contract contract) {
		this.contract = contract;
	}

	public PaymentRequest getPaymentRequest() {
		return paymentRequest;
	}

	public void setPaymentRequest(PaymentRequest paymentRequest) {
		this.paymentRequest = paymentRequest;
	}

	public PermissionStatus getPermissionStatus() {
		return permissionStatus;
	}

	public void setPermissionStatus(PermissionStatus permissionStatus) {
		this.permissionStatus = permissionStatus;
	}

	public String getStatusExplanation() {
		return statusExplanation;
	}

	public void setStatusExplanation(String statusExplanation) {
		this.statusExplanation = statusExplanation;
	}
}
