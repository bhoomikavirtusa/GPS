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
 * @author lnagy
 */
@Entity
@PersistenceUnit(unitName = "permissions")
@Table(name = "ASSET_USE_FILE")
public class AssetUseFile
extends AuditBase
{
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID", nullable = false, unique = true)
	private Integer id = null;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "AU_ID", nullable = false)
	private AssetUse assetUse = null;

	@Column(name = "FILE_NAME", nullable = false, length = 255)
	private String fileName = null;

	@Column(name = "DESCRIPTION", nullable = true, length = 1000)
	private String description = null;

	@Column(name = "MIME_TYPE", nullable = false, length = 100)
	private String mimeType = null;

	@Lob
	@Column(name = "FILE_DATA", nullable = false, length = 10111000)
	private byte[] fileData = null;

	public AssetUseFile() {
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

	public AssetUse getAssetUse() {
		return assetUse;
	}

	public void setAssetUse(AssetUse assetUse) {
		this.assetUse = assetUse;
	}
}
