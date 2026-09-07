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

/**
 *
 * @author smarkoff
 */
@Entity
@PersistenceUnit(unitName = "permissions")
@Table(name = "CW_FILE")
public class CwFile
extends AuditBase
{
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID", nullable = false, unique = true)
	private Integer id = null;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "CW_ID", nullable = false)
	private CommonWork commonWork = null;

	@Column(name = "FILE_NAME", nullable = false, length = 200)
	private String fileName = null;

	// Have this mainly because Spring MultipartFile provides contentType
	@Column(name = "MIME_TYPE", nullable = false, length = 100)
	private String mimeType = null;

	@Column(name = "DESCRIPTION", nullable = true, length = 500)
	private String description = null;

	@Lob
	@Column(name = "DATA", nullable = false, length = 10111000)
	private byte[] data = null;


	public CwFile() {
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public CommonWork getCommonWork() {
		return commonWork;
	}

	public void setCommonWork(CommonWork commonWork) {
		this.commonWork = commonWork;
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

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}
}
