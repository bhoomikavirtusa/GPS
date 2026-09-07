package com.wiley.permissions.domain.persistence.permissions;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PersistenceUnit;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.lang3.StringUtils;

import com.wiley.permissions.common.utils.ValidateException;
import com.wiley.permissions.domain.DomainObject;

@Entity
@PersistenceUnit(unitName="permissions")
@Table(name="ASSET_2_SOURCE")
@IdClass(AssetSourcePK.class)
@NamedQueries ({
@NamedQuery(
    name="AssetToSource.AssetToSource.loadByAssetId",
    query="from AssetToSource a2s where " +
    		"a2s.assetId = :assetId"
)
})
public class AssetToSource extends DomainObject implements Comparable<AssetToSource> {
	private static final long serialVersionUID = 1L;

	@Id
	private Integer sourceId = null;

	@Id
	private Integer assetId = null;

	@Column(name = "NOTES", nullable = true)
	private String notes = null;

	@ManyToOne
	@JoinColumn(name = "ASSET_ID" , insertable=false, updatable=false)
	private Asset asset = null;

	@ManyToOne
	@JoinColumn(name = "SOURCE_ID", insertable=false, updatable=false)
	private Source source = null;

	public Asset getAsset() {
		return asset;
	}

	public void setAsset(Asset value) {
		asset = value;
	}

	public Source getSource() {
		return source;
	}

	public void setSource(Source value) {
		source = value;
	}

	public Integer getSourceId() {
		return sourceId;
	}

	public void setSourceId(Integer sourceId) {
		this.sourceId = sourceId;
	}

	public Integer getAssetId() {
		return assetId;
	}

	public void setAssetId(Integer assetId) {
		this.assetId = assetId;
	}


	public String getNotes() {
		return notes;
	}

	public void setNotes(String value) {
		this.notes = value;
	}


	@Override
	public String toString() {
		return super.toString()
		    + ", assetId = " + getAssetId()
		    + ", sourceId = " + getSourceId()
		    + ", notes = [" + notes + "]\r\n";
	}

	@Transient
	public void validate() throws ValidateException {
		if (null == getAssetId()) {
			throw new ValidateException ("AssetId is required");
		}

		if (null == getSourceId()) {
			throw new ValidateException ("Source Id is required");
		}
	}

	/** Implements Comparable interface. */
	public int compareTo(AssetToSource s) {
		if (s.getAssetId() == getAssetId()
				&& s.getSourceId() == getSourceId()
				&& StringUtils.equals(s.getNotes(), getNotes())) return 0;

		return 1;
	}

	// Base hashCode() and equals only on assetId and SourceId

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((assetId == null) ? 0 : assetId.hashCode());
		result = prime * result
				+ ((sourceId == null) ? 0 : sourceId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		// Important to use "instance of" due to JPA proxies
		//if (getClass() != obj.getClass()) return false;
		if (!(obj instanceof AssetToSource)) return false;
		AssetToSource other = (AssetToSource) obj;
		if (assetId == null) {
			if (other.assetId != null) return false;
		}
		else if (!assetId.equals(other.assetId)) return false;
		if (sourceId == null) {
			if (other.sourceId != null) return false;
		}
		else if (!sourceId.equals(other.sourceId)) return false;
		return true;
	}
}
