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

@Entity
@PersistenceUnit(unitName = "permissions")
@Table(name = "CONTRACT_FILE_NAME")
public class ContractFileName extends AuditBase {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID", nullable = false, unique = true)
	@XmlElement(name = "id")
	private Integer id = null;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "CONTRACT_ID", nullable = false)
	@XmlElement(name = "contract")
	@XmlIDREF
	private Contract contract = null;

	@Column(name = "FILE_NAME", nullable = false, length = 255)
	@XmlElement(name = "fileName")
	private String fileName = null;

	@Column(name = "FILE_UPLOADED")
	@XmlElement(name = "fileUploaded")
	private boolean fileUploaded = false;

	public ContractFileName() {
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Contract getContract() {
		return contract;
	}

	public void setContract(Contract contract) {
		this.contract = contract;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public boolean isFileUploaded() {
		return fileUploaded;
	}

	public void setFileUploaded(boolean fileUploaded) {
		this.fileUploaded = fileUploaded;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((fileName == null) ? 0 : fileName.hashCode());
		result = prime * result + (fileUploaded ? 1231 : 1237);
		result = prime * result + ((id == null) ? 0 : id.hashCode());
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
		if (!(obj instanceof ContractFileName)) {
			return false;
		}
		ContractFileName other = (ContractFileName) obj;
		if (fileName == null) {
			if (other.fileName != null) {
				return false;
			}
		} else if (!fileName.equals(other.fileName)) {
			return false;
		}
		if (fileUploaded != other.fileUploaded) {
			return false;
		}
		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return new StringBuilder().append("ID :: ").append(getId()).append("; File Names :: ").append(getFileName()).append("; FilesUploaded :: ")
				.append(isFileUploaded()).toString();

	}

}