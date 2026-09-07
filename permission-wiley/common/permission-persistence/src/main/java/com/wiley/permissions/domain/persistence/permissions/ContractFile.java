package com.wiley.permissions.domain.persistence.permissions;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceUnit;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlInlineBinaryData;

/**
 *
 * @author ttidwell
 */
@Entity
@PersistenceUnit(unitName = "permissions")
@Table(name = "CONTRACT_FILE")
public class ContractFile
extends AuditBase
{
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

	@Column(name = "DESCRIPTION", nullable = true, length = 1000)
	@XmlElement(name = "description")
	private String description = null;

	@Column(name = "MIME_TYPE", nullable = false, length = 100)
	@XmlElement(name = "mimeType")
	private String mimeType = null;

	@Lob
	@Column(name = "FILE_DATA", nullable = false, length = 10111000)
	@XmlInlineBinaryData
	@XmlElement(name = "fileBlobData")
	private byte[] fileData = null;

	public ContractFile() {
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	public byte[] getFileData() {
		return fileData;
	}

	public void setFileData(byte[] fileData) {
		this.fileData = fileData;
	}

	public Contract getContract() {
		return contract;
	}

	public void setContract(Contract contract) {
		this.contract = contract;
	}
}
