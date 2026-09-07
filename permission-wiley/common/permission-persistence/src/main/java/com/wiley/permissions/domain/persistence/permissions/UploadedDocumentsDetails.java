package com.wiley.permissions.domain.persistence.permissions;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceUnit;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlIDREF;

import com.wiley.permissions.domain.DomainObject;

@Entity
@PersistenceUnit(unitName = "permissions")
@Table(name = "UPLOADED_DOCS_DETAILS")
public class UploadedDocumentsDetails extends DomainObject {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID", nullable = false, unique = true)
	@XmlElement(name = "id")
	private Integer id = null;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "UPLOAD_HISTORY_ID", nullable = false)
	@XmlElement(name = "uploadHistoryId")
	@XmlIDREF
	private UploadDocumentsHistory uploadHistoryId = null;

	@Column(name = "COMPONENT")
	private String component = null;

	@Column(name = "MEDIA_TYPE")
	private String mediaType = null;

	@Column(name = "DESCRIPTION")
	private String description = null;

	@Column(name = "SOURCE")
	private String source = null;

	@Column(name = "SOURCE_REF_NUMBER")
	private String sourceRefNumber = null;

	@Column(name = "DOCUMENT_NAME")
	private String documentName = null;

	@Column(name = "UPLOAD_STATUS", nullable = false)
	private String uploadStatus = null;

	public UploadedDocumentsDetails() {
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public UploadDocumentsHistory getUploadHistoryId() {
		return uploadHistoryId;
	}

	public void setUploadHistoryId(UploadDocumentsHistory uploadHistoryId) {
		this.uploadHistoryId = uploadHistoryId;
	}

	public String getComponent() {
		return component;
	}

	public void setComponent(String component) {
		this.component = component;
	}

	public String getMediaType() {
		return mediaType;
	}

	public void setMediaType(String mediaType) {
		this.mediaType = mediaType;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getSourceRefNumber() {
		return sourceRefNumber;
	}

	public void setSourceRefNumber(String sourceRefNumber) {
		this.sourceRefNumber = sourceRefNumber;
	}

	public String getDocumentName() {
		return documentName;
	}

	public void setDocumentName(String documentName) {
		this.documentName = documentName;
	}

	public String getUploadStatus() {
		return uploadStatus;
	}

	public void setUploadStatus(String uploadStatus) {
		this.uploadStatus = uploadStatus;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((component == null) ? 0 : component.hashCode());
		result = prime * result
				+ ((description == null) ? 0 : description.hashCode());
		result = prime * result
				+ ((documentName == null) ? 0 : documentName.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result
				+ ((mediaType == null) ? 0 : mediaType.hashCode());
		result = prime * result + ((source == null) ? 0 : source.hashCode());
		result = prime * result
				+ ((sourceRefNumber == null) ? 0 : sourceRefNumber.hashCode());
		result = prime * result
				+ ((uploadHistoryId == null) ? 0 : uploadHistoryId.hashCode());
		result = prime * result
				+ ((uploadStatus == null) ? 0 : uploadStatus.hashCode());
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
		if (!(obj instanceof UploadedDocumentsDetails)) {
			return false;
		}
		UploadedDocumentsDetails other = (UploadedDocumentsDetails) obj;
		if (component == null) {
			if (other.component != null) {
				return false;
			}
		} else if (!component.equals(other.component)) {
			return false;
		}
		if (description == null) {
			if (other.description != null) {
				return false;
			}
		} else if (!description.equals(other.description)) {
			return false;
		}
		if (documentName == null) {
			if (other.documentName != null) {
				return false;
			}
		} else if (!documentName.equals(other.documentName)) {
			return false;
		}
		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}
		if (mediaType == null) {
			if (other.mediaType != null) {
				return false;
			}
		} else if (!mediaType.equals(other.mediaType)) {
			return false;
		}
		if (source == null) {
			if (other.source != null) {
				return false;
			}
		} else if (!source.equals(other.source)) {
			return false;
		}
		if (sourceRefNumber == null) {
			if (other.sourceRefNumber != null) {
				return false;
			}
		} else if (!sourceRefNumber.equals(other.sourceRefNumber)) {
			return false;
		}
		if (uploadHistoryId == null) {
			if (other.uploadHistoryId != null) {
				return false;
			}
		} else if (!uploadHistoryId.equals(other.uploadHistoryId)) {
			return false;
		}
		if (uploadStatus == null) {
			if (other.uploadStatus != null) {
				return false;
			}
		} else if (!uploadStatus.equals(other.uploadStatus)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("UploadedDocumentsDetails [id=");
		builder.append(id);
		builder.append(", uploadHistoryId=");
		builder.append(uploadHistoryId);
		builder.append(", component=");
		builder.append(component);
		builder.append(", mediaType=");
		builder.append(mediaType);
		builder.append(", description=");
		builder.append(description);
		builder.append(", source=");
		builder.append(source);
		builder.append(", sourceRefNumber=");
		builder.append(sourceRefNumber);
		builder.append(", documentName=");
		builder.append(documentName);
		builder.append(", uploadStatus=");
		builder.append(uploadStatus);
		builder.append("]");
		return builder.toString();
	}

}
