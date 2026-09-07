package com.wiley.permissions.domain.persistence.permissions;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.PersistenceUnit;
import javax.persistence.Table;

/**
 *
 * @author smarkoff
 */
@PersistenceUnit(unitName = "permissions")
@Table(name = "AU_SOURCE_PERM_STATUS")
@Embeddable
public class AuSourcePermStatusPK
implements Serializable
{
	private static final long serialVersionUID = 1L;

	@Column(name="ASSET_USE_ID")
	private int assetUseId;

	@Column(name="SOURCE_ID")
	private int sourceId;

	public AuSourcePermStatusPK() {

	}

	public AuSourcePermStatusPK(int assetUseId, int sourceId) {
	    super();
	    setAssetUseId(assetUseId);
	    setSourceId(sourceId);
	}

	public int getAssetUseId() {
		return assetUseId;
	}

	public void setAssetUseId(int assetUseId) {
		this.assetUseId = assetUseId;
	}

	public int getSourceId() {
		return sourceId;
	}

	public void setSourceId(int sourceId) {
		this.sourceId = sourceId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + assetUseId;
		result = prime * result + sourceId;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		// Important to use "instance of" due to JPA proxies
		//if (getClass() != obj.getClass()) return false;
		if (!(obj instanceof AuSourcePermStatusPK)) return false;
		AuSourcePermStatusPK other = (AuSourcePermStatusPK) obj;
		// ok to use != since we are comparing int's (not Integer)
		if (assetUseId != other.assetUseId) return false;
		if (sourceId != other.sourceId) return false;
		return true;
	}
}
