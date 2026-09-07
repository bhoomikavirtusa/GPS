package com.wiley.permissions.domain.persistence.permissions;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PersistenceUnit;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.xml.bind.annotation.XmlElement;

import com.wiley.permissions.domain.DomainObject;

@Entity
@PersistenceUnit(unitName = "permissions")
@Table(name = "EXPORT_ASSET")
public class ExportAsset extends DomainObject {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID", nullable = false, unique = true)
	@XmlElement(name = "id")
	private Integer id = null;

	@Column(name = "CW_ID", nullable = false)
	private Integer cwId = null;

	@Column(name = "ISBN13")
	private String isbn13 = null;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "EXPORT_DATE", nullable=false, updatable=false)
	private Date exportDate = null;

	@Column(name = "USER_ID", nullable = false)
	private Integer userId = null;

	@Column(name = "EXPORT_STATUS")
	private String exportStatus;

	@Column(name = "TOTAL_COUNT", nullable = false)
	private Integer totalCount = null;
	
	@Column(name = "COMPLETED", nullable = false)
	private Integer completed = null;

	public ExportAsset() {
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Integer getCwId() {
		return cwId;
	}

	public void setCwId(Integer cwId) {
		this.cwId = cwId;
	}

	public String getIsbn13() {
		return isbn13;
	}

	public void setIsbn13(String isbn13) {
		this.isbn13 = isbn13;
	}

	public Date getExportDate() {
		return exportDate;
	}

	public void setExportDate(Date exportDate) {
		this.exportDate = exportDate;
	}

	public Integer getUserId() {
		return userId;
	}

	public void setUserId(Integer userId) {
		this.userId = userId;
	}

	public String getExportStatus() {
		return exportStatus;
	}

	public void setExportStatus(String exportStatus) {
		this.exportStatus = exportStatus;
	}

	public Integer getTotalCount() {
		return totalCount;
	}

	public void setTotalCount(Integer totalCount) {
		this.totalCount = totalCount;
	}

	public Integer getCompleted() {
		return completed;
	}

	public void setCompleted(Integer completed) {
		this.completed = completed;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((exportDate == null) ? 0 : exportDate.hashCode());
		result = prime * result + ((cwId == null) ? 0 : cwId.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((isbn13 == null) ? 0 : isbn13.hashCode());
		result = prime * result
				+ ((userId == null) ? 0 : userId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof ExportAsset)) {
			return false;
		}
		ExportAsset other = (ExportAsset) obj;
		if (exportDate == null) {
			if (other.exportDate != null) {
				return false;
			}
		} else if (!exportDate.equals(other.exportDate)) {
			return false;
		}
		if (cwId == null) {
			if (other.cwId != null) {
				return false;
			}
		} else if (!cwId.equals(other.cwId)) {
			return false;
		}
		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}
		if (isbn13 == null) {
			if (other.isbn13 != null) {
				return false;
			}
		} else if (!isbn13.equals(other.isbn13)) {
			return false;
		}
		if (userId == null) {
			if (other.userId != null) {
				return false;
			}
		} else if (!userId.equals(other.userId)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return new StringBuilder("UploadDocumentsHistory [id=").append(id).append(", cwId=").append(cwId)
				.append(", isbn13=").append(isbn13).append(", createdDate=").append(exportDate)
				.append(", uploadedUserId=").append(userId).append(", uploadInprogress=")
				.append(exportStatus).append("]").toString();
	}

}
