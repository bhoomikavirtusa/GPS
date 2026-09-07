package com.wiley.permissions.domain.persistence.permissions;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.PersistenceUnit;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.Size;

import org.apache.commons.lang3.StringUtils;

import com.wiley.permissions.domain.DomainObject;

/**
 *
 * @author ttidwell
 */
@Entity
@PersistenceUnit(unitName = "permissions")
@Table(name = "CONTRACT_2_ASSET")
@IdClass(ContractAssetPK.class)
public class ContractAsset
extends DomainObject
{
	private static final long serialVersionUID = 1L;

	@Id
	private Integer contractId = null;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "CONTRACT_ID", nullable = false, insertable=false, updatable = false)
	private Contract contract = null;

	@Id
	private Integer assetBaseId = null;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "ASSET_BASE_ID", nullable = false, insertable=false, updatable = false)
	private AssetBase assetBase = null;

	@Column(name = "PRICE", nullable = true)
	private Double price = null;

	@OneToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "RFDEAL_ID", nullable = true)
	private RoyaltyFreeDeal royaltyFreeDeal = null;

	@Size(max=1000)
	@Column(name = "CREDIT_LINE", length = 1000)
	private String creditLine;

	@Column(name="NO_CROP", nullable=false)
	private boolean noCrop = false;

	@Column(name="NO_BLEED", nullable=false)
	private boolean noBleed = false;

	// Added for SS Task 12 - Start
	@Column(name = "approver_name")
	private String approverName;

	@Column(name = "approver_remarks")
	private String approverRemarks;
	
	@Column(name = "user_name")
	private String userName;
	// Added for SS Task 12 - End

	//Start: Added to implement DM-122
	@Column(name="IS_ASSET_LEVEL_RFDEAL", nullable=true)
	private boolean isAssetLevelRFDeal = false;

	@Column(name = "SEATS", nullable = true)
	private Integer seats;

	@Column(name = "TOTAL_PRINT_RUN", nullable = true)
	private Integer totalPrintRun;

	@Column(name = "GRANT_TYPE", nullable = true)
	private String grantType;

	@Column(name="SUBLICENCE", nullable=true)
	private boolean sublicence = false;

	public boolean isAssetLevelRFDeal() {
		return isAssetLevelRFDeal;
	}

	public void setAssetLevelRFDeal(boolean isAssetLevelRFDeal) {
		this.isAssetLevelRFDeal = isAssetLevelRFDeal;
	}

	public Integer getSeats() {
		return seats;
	}

	public void setSeats(Integer seats) {
		this.seats = seats;
	}

	public Integer getTotalPrintRun() {
		return totalPrintRun;
	}

	public void setTotalPrintRun(Integer totalPrintRun) {
		this.totalPrintRun = totalPrintRun;
	}

	public String getGrantType() {
		return grantType;
	}

	public void setGrantType(String grantType) {
		this.grantType = grantType;
	}

	public boolean isSublicence() {
		return sublicence;
	}

	public void setSublicence(boolean sublicence) {
		this.sublicence = sublicence;
	}
	//End: Added to implement DM-122

	public ContractAsset() {
	}

	public ContractAsset(Integer assetBaseId, Integer contractId, Double price) {
	    setAssetBaseId(assetBaseId);
	    setContractId(contractId);
	    setPrice(price);
	}

	public ContractAsset(AssetBase asset, Contract contract, Double price) {
		this(asset.getId(), contract.getId(), price);
		setAssetBase(asset);
		setContract(contract);
	}

	public Contract getContract() {
		return contract;
	}

	public void setContract(Contract contract) {
		this.contract = contract;
		setContractId(contract.getId());
	}

	public AssetBase getAssetBase() {
		return assetBase;
	}

	public void setAssetBase(AssetBase assetBase) {
		this.assetBase = assetBase;
		setAssetBaseId(assetBase.getId());
	}

	public Double getPrice() {
		return price;
	}

	public void setPrice(Double price) {
		this.price = price;
	}

	public Integer getAssetBaseId() {
		return assetBaseId;
	}

	public void setAssetBaseId(Integer assetBaseId) {
		this.assetBaseId = assetBaseId;
	}

	public Integer getContractId() {
		return contractId;
	}

	public void setContractId(Integer contractId) {
		this.contractId = contractId;
	}

	public RoyaltyFreeDeal getRoyaltyFreeDeal() {
		return royaltyFreeDeal;
	}

	public void setRoyaltyFreeDeal(RoyaltyFreeDeal royaltyFreeDeal) {
		this.royaltyFreeDeal = royaltyFreeDeal;
	}

	@Transient
	public int getRoyaltyFreeDealId() {
		// 0 indicates Rights managed but we cannot save 0 because of the constraint to RoyaltyFreeDeal table
		return (null == royaltyFreeDeal ? 0 : royaltyFreeDeal.getId());
	}

	@Transient
	public void setRoyaltyFreeDealId(int rfDealId) {
		// 0 indicates Rights managed but we cannot save 0 because of the constraint to RoyaltyFreeDeal table
		royaltyFreeDeal = (rfDealId == 0 ? null : new RoyaltyFreeDeal());
		if (null != royaltyFreeDeal) {
			royaltyFreeDeal.setId(rfDealId);
		}
	}

	//Start: Added for DM-123
	@Transient
	String RForRM;

	@Transient
	public String getRForRM() {
		return this.RForRM;
	}

	@Transient
	public void setRForRM(String str) {
		this.RForRM = str;
	}
	//End: Added for DM-123

	public String getCreditLine() {
		return creditLine;
	}

	public void setCreditLine(String creditLine) {
		this.creditLine = StringUtils.stripToNull(creditLine);
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

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		// Important to use "instance of" due to JPA proxies
		//if (!getClass().equals(obj.getClass())) return false;
		if (!(obj instanceof ContractAsset)) return false;
		ContractAsset other = (ContractAsset) obj;
		// use getters due to JPA proxies (not completely sure if necessary in this case)
		if (getAssetBaseId() == null) {
			if (other.getAssetBaseId() != null)
				return false;
		} else if (!getAssetBaseId().equals(other.getAssetBaseId()))
			return false;
		if (getContractId() == null) {
			if (other.getContractId() != null)
				return false;
		} else if (!getContractId().equals(other.getContractId()))
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		// use getters due to JPA proxies (not completely sure if necessary in this case)
		result = prime * result
				+ ((getAssetBaseId() == null) ? 0 : getAssetBaseId().hashCode());
		result = prime * result
				+ ((getContractId() == null) ? 0 : getContractId().hashCode());
		return result;
	}

	@Override
	public String toString() {
		// use getters due to JPA proxies
		return "contractId = " + getContractId()
		    + ", assetBaseId = " + getAssetBaseId()
		    + ", price = " + getPrice()
		    + ", royaltyFreeDealId = " + getRoyaltyFreeDealId()
			+ ", creditLine = " + getCreditLine()
			+ ", noCrop = " + isNoCrop()
			+ ", noBleed = " + isNoBleed()
			+ ", isAssetLevelRFDeal = " + isAssetLevelRFDeal()
			+ ", seats = " + getSeats()
			+ ", totalPrintRun = " + getTotalPrintRun()
			+ ", grantType = " + getGrantType()
			+ ", sublicence = " + isSublicence();
	}

	// Added for SS Task 12 - Start
	public String getApproverName() {
		return approverName;
	}

	public void setApproverName(String approverName) {
		this.approverName = approverName;
	}

	public String getApproverRemarks() {
		return approverRemarks;
	}

	public void setApproverRemarks(String approverRemarks) {
		this.approverRemarks = approverRemarks;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}
	// Added for SS Task 12 - End
}
