package com.wiley.permissions.domain.persistence.permissions;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.PersistenceUnit;
import javax.persistence.Table;

@PersistenceUnit(unitName = "permissions")
@Table(name = "CONTRACT_2_ASSET")
@Embeddable
public class ContractAssetPK
implements Serializable
{
	private static final long serialVersionUID = 1L;

	@Column(name="CONTRACT_ID")
	private Integer contractId = null;

	@Column(name="ASSET_BASE_ID")
	private Integer assetBaseId = null;

	public ContractAssetPK() {
		super();
	}

	public ContractAssetPK(int assetBaseId, int contractId) {
	    super();
	    setAssetBaseId(assetBaseId);
	    setContractId(contractId);
	}

	public ContractAssetPK(AssetBase assetBase, Contract contract) {
	    super();
	    setAssetBaseId(assetBase.getId());
	    setContractId(contract.getId());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		// Important to use "instance of" due to JPA proxies
		//if (!getClass().equals(obj.getClass())) return false;
		if (!(obj instanceof ContractAssetPK)) return false;
		ContractAssetPK other = (ContractAssetPK) obj;
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

	@Override
	public String toString() {
		return "contractId = " + contractId
		    + ", assetBaseId = " + assetBaseId;
	}
}
