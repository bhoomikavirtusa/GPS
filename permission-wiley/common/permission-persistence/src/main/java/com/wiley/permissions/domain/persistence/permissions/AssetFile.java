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
import javax.xml.bind.annotation.XmlInlineBinaryData;

/**
 *
 * @author smarkoff
 */
@Entity
@PersistenceUnit(unitName = "permissions")
@Table(name = "ASSET_FILE")
public class AssetFile
extends AuditBase
{
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID", nullable = false, unique = true)
	private Integer id = null;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ASSET_ID", nullable = false)
	private Asset asset = null;

	@Column(name = "OBJECT_NAME", nullable = false, length = 200)
	private String objectName = null;

	@Column(name = "FILE_FORMAT", nullable = false, length = 20)
	private String fileFormat = null;

	@Lob
	@Column(name = "DATA", nullable = false, length = 500111000)
	private byte[] data = null;

	@ManyToOne
	@JoinColumn(name="RENDITION_TYPE", referencedColumnName="CODE")
	private RenditionType renditionType;


	public AssetFile() {
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Asset getAsset() {
		return asset;
	}

	public void setAsset(Asset asset) {
		this.asset = asset;
	}

	@XmlElement
	public String getObjectName() {
		return objectName;
	}

	public void setObjectName(String objectName) {
		this.objectName = objectName;
	}

	@XmlElement
	public String getFileFormat() {
		return fileFormat;
	}

	public void setFileFormat(String fileFormat) {
		this.fileFormat = fileFormat;
	}

	@XmlInlineBinaryData
	@XmlElement(name="fileBlobData")
	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	@XmlElement
	public RenditionType getRenditionType() {
		return renditionType;
	}

	public void setRenditionType(RenditionType renditionType) {
		this.renditionType = renditionType;
	}
}
