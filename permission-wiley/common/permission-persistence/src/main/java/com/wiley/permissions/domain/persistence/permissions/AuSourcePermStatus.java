package com.wiley.permissions.domain.persistence.permissions;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceUnit;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.wiley.permissions.domain.DomainObject;
import com.wiley.sf.common.lang.ArgUtil;

/**
 *
 * @author smarkoff
 */
@Entity
@PersistenceUnit(unitName = "permissions")
@Table(name = "AU_SOURCE_PERM_STATUS")
@IdClass(AuSourcePermStatusPK.class)
public class AuSourcePermStatus
extends DomainObject
{
	private static final long serialVersionUID = 1L;

	@Id
	private Integer assetUseId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ASSET_USE_ID", nullable = false, insertable = false, updatable = false)
	private AssetUse assetUse;

	@Id
	private Integer sourceId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "SOURCE_ID", nullable = false, insertable = false, updatable = false)
	private Source source;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "PERMISSION_STATUS")
	private PermissionStatus status;

	@Column(name = "STATUS_EXPLANATION")
	private String statusExplanation;

	@Column(name = "LATEST_CONTRACT_ID")
	private Integer latestContractId;

	@Column(name = "LATEST_PO_ID")
	private Integer latestPoId;

	@Column(name = "ACTIVE_CONTRACT_ID")
	private Integer activeContractId;

	@Column(name = "ACTIVE_PO_ID")
	private Integer activePoId;

	@Column(name = "CONTRACT_CREDIT_LINE")
	private String contractCreditLine;  // credit line from latestContract

	@Column(name = "NEED_PAYMENT_REQUEST")
	private boolean needPaymentRequest;

	@Column(name = "PAID")
	private boolean paid;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "LAST_UPDATED_DATE", nullable=false)
	private Date lastUpdatedDate = new Date();


	public AuSourcePermStatus() {
	}

	/**
	 * @param assetUseId  Must be non-null
	 * @param sourceId    Must be non-null
	 */
	public AuSourcePermStatus(Integer assetUseId, Integer sourceId, PermissionStatus status, String explanation) {
		ArgUtil.notNull(assetUseId, "assetUseId");
		ArgUtil.notNull(sourceId, "sourceId");
	    setAssetUseId(assetUseId);
	    setSourceId(sourceId);
	    setStatus(status);
	    setStatusExplanation(explanation);
	}

	public AuSourcePermStatus(AssetUse assetUse, Source source, PermissionStatus status, String explanation) {
		this(assetUse.getId(), source.getId(), status, explanation);
		setAssetUse(assetUse);
		setSource(source);
	}

	public Integer getAssetUseId() {
		return assetUseId;
	}

	public void setAssetUseId(Integer assetUseId) {
		this.assetUseId = assetUseId;
	}

	public Integer getSourceId() {
		return sourceId;
	}

	public void setSourceId(Integer sourceId) {
		this.sourceId = sourceId;
	}

	public AssetUse getAssetUse() {
		return assetUse;
	}

	public void setAssetUse(AssetUse assetUse) {
		this.assetUse = assetUse;
	}

	public Source getSource() {
		return source;
	}

	public void setSource(Source source) {
		this.source = source;
	}

	public PermissionStatus getStatus() {
		return status;
	}

	public void setStatus(PermissionStatus status) {
		this.status = status;
	}

	public String getStatusExplanation() {
		return statusExplanation;
	}

	public void setStatusExplanation(String statusExplanation) {
		this.statusExplanation = statusExplanation;
	}

	public Integer getLatestContractId() {
		return latestContractId;
	}

	public void setLatestContractId(Integer i) {
		latestContractId = i;
	}

	public Integer getLatestPoId() {
		return latestPoId;
	}

	public void setLatestPoId(Integer i) {
		latestPoId = i;
	}

	public Integer getActiveContractId() {
		return activeContractId;
	}

	public void setActiveContractId(Integer i) {
		activeContractId = i;
	}

	public Integer getActivePoId() {
		return activePoId;
	}

	public void setActivePoId(Integer i) {
		activePoId = i;
	}

	public String getContractCreditLine() {
		return contractCreditLine;
	}

	public void setContractCreditLine(String s) {
		contractCreditLine = s;
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

	public Date getLastUpdatedDate() {
		return lastUpdatedDate;
	}

	public void setLastUpdatedDate(Date lastUpdatedDate) {
		this.lastUpdatedDate = lastUpdatedDate;
	}
}
