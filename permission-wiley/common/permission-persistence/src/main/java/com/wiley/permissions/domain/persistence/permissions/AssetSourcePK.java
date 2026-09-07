package com.wiley.permissions.domain.persistence.permissions;

import java.io.Serializable;

import javax.persistence.Column;

public class AssetSourcePK
implements Serializable
{
	private static final long serialVersionUID = 1L;

	@Column(name="SOURCE_ID")	
	private Integer sourceId = null;

	@Column(name="ASSET_ID")	
	private Integer assetId = null;

	public AssetSourcePK() {
		super();
	}

	public AssetSourcePK(int sourceId, int assetId) {
	    super();
	    setAssetId(assetId);
	    setSourceId(sourceId);
	}

	public AssetSourcePK(Asset asset, Source source) {
	    super();
	    setAssetId(asset.getId());
	    setSourceId(source.getId());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		// Important to use "instance of" due to JPA proxies
		//if (!getClass().equals(obj.getClass())) return false;
		if (!(obj instanceof AssetSourcePK)) return false;
		AssetSourcePK other = (AssetSourcePK) obj;
		// use getters due to JPA proxies (not completely sure if necessary in this case)
		if (getAssetId() == null) {
			if (other.getAssetId() != null)
				return false;
		} else if (!getAssetId().equals(other.getAssetId()))
			return false;
		if (getSourceId() == null) {
			if (other.getSourceId() != null)
				return false;
		} else if (!getSourceId().equals(other.getSourceId()))
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		// use getters due to JPA proxies (not completely sure if necessary)
		result = prime * result + ((getAssetId() == null) ? 0 : getAssetId().hashCode());
		result = prime * result
				+ ((getSourceId() == null) ? 0 : getSourceId().hashCode());
		return result;
	}

	public Integer getAssetId() {
		return assetId;
	}

	public void setAssetId(Integer value) {
		this.assetId = value;
	}

	public Integer getSourceId() {
		return sourceId;
	}

	public void setSourceId(Integer did) {
		this.sourceId = did;
	}

	@Override
	public String toString() {
		return "sourceId = " + sourceId
		    + ", assetId = " + assetId;
	}
}
