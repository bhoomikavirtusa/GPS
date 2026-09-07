package com.wiley.permissions.domain.persistence.permissions;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.OneToOne;
import javax.persistence.PersistenceUnit;
import javax.persistence.Table;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Amendment DAO
 * 
 * @version $Id: Amendment.java,v 1.7 2013-10-08 20:22:30 lnagy Exp $
 */
@Entity
@PersistenceUnit(unitName = "permissions")
@Table(name = "AMENDMENT")
public class Amendment extends ExtendedAuditBase {

	private static final long serialVersionUID = 1L;

	@SuppressWarnings("unused")
	private final static Log log = LogFactory.getLog(Amendment.class);

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID", nullable = false, unique = true)
	private Integer id = null;

	@OneToOne
	@JoinColumn(name = "CONTRACT_ID")
	private Contract contract;

	@Column(name = "NOTE", nullable = true, length = 200)
	private String note;

	@Column(name = "FILE_NAME", nullable = true, length = 200)
	private String fileName = null;

	// Have this mainly because Spring MultipartFile provides contentType
	@Column(name = "MIME_TYPE", nullable = true, length = 100)
	private String mimeType = null;

	@Lob
	@Column(name = "DATA", nullable = true, length = 10111000)
	private byte[] data = null;

	@Column(name = "LANGUAGE_CODE")
	private String languageCode = EnumLanguage.EN;

	public Amendment() {
		super();
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

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	public String getLanguageCode() {
		return languageCode;
	}

	public void setLanguageCode(String languageCode) {
		this.languageCode = languageCode;
	}
}
