package com.wiley.permissions.domain.persistence.permissions;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.PersistenceUnit;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.xml.bind.annotation.XmlElement;

import com.wiley.permissions.domain.DomainObject;

@Entity
@PersistenceUnit(unitName = "permissions")
@Table(name = "UPLOAD_DOCS_HISTORY")
public class UploadDocumentsHistory extends DomainObject {

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
	@Column(name = "UPLOADED_DATE", nullable=false, updatable=false)
	private Date uploadedDate = null;

	@Column(name = "UPLOADED_USER_ID", nullable = false)
	private Integer uploadedUserId = null;

	@Column(name = "UPLOAD_IN_PROGRESS")
	private boolean uploadInprogress;

	@OneToMany(cascade = CascadeType.ALL, mappedBy = "uploadHistoryId")
	private List<UploadedDocumentsDetails> uploadedDocuments = new ArrayList<UploadedDocumentsDetails>();

	public List<UploadedDocumentsDetails> getUploadedDocuments() {
		return uploadedDocuments;
	}

	public void setUploadedDocuments(
			List<UploadedDocumentsDetails> uploadedDocuments) {
		this.uploadedDocuments = uploadedDocuments;
	}

	public UploadDocumentsHistory() {
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

	public Date getUploadedDate() {
		return uploadedDate;
	}

	public void setUploadedDate(Date uploadedDate) {
		this.uploadedDate = uploadedDate;
	}

	public Integer getUploadedUserId() {
		return uploadedUserId;
	}

	public void setUploadedUserId(Integer uploadedUserId) {
		this.uploadedUserId = uploadedUserId;
	}

	public boolean isUploadInprogress() {
		return uploadInprogress;
	}

	public void setUploadInprogress(boolean uploadInprogress) {
		this.uploadInprogress = uploadInprogress;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((uploadedDate == null) ? 0 : uploadedDate.hashCode());
		result = prime * result + ((cwId == null) ? 0 : cwId.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((isbn13 == null) ? 0 : isbn13.hashCode());
		result = prime * result
				+ ((uploadedUserId == null) ? 0 : uploadedUserId.hashCode());
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
		if (!(obj instanceof UploadDocumentsHistory)) {
			return false;
		}
		UploadDocumentsHistory other = (UploadDocumentsHistory) obj;
		if (uploadedDate == null) {
			if (other.uploadedDate != null) {
				return false;
			}
		} else if (!uploadedDate.equals(other.uploadedDate)) {
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
		if (uploadedUserId == null) {
			if (other.uploadedUserId != null) {
				return false;
			}
		} else if (!uploadedUserId.equals(other.uploadedUserId)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return new StringBuilder("UploadDocumentsHistory [id=").append(id).append(", cwId=").append(cwId)
				.append(", isbn13=").append(isbn13).append(", createdDate=").append(uploadedDate)
				.append(", uploadedUserId=").append(uploadedUserId).append(", uploadInprogress=")
				.append(uploadInprogress).append("]").toString();
	}

}
