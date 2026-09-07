package com.wiley.permissions.domain.persistence.permissions;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.PersistenceUnit;
import javax.persistence.Table;

/**
 *
 * @author lnagy
 */
@Entity
@PersistenceUnit(unitName = "permissions")
@Table(name = "GUIDELINE_FILE")
public class GuidelineFile
extends AuditBase
{
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID", nullable = false, unique = true)
	private Integer id = null;

	@Column(name = "FILE_NAME", nullable = false, length = 200)
	private String fileName = null;

	@Column(name = "DISPLAY_NAME", nullable = false, length = 200)
	private String displayName = null;

	@Column(name = "MIME_TYPE", nullable = false, length = 100)
	private String mimeType = null;
	
	@Lob
	@Column(name = "FILE_DATA", nullable = false, length = 10111000)
	private byte[] fileData = null;

	@Column(name = "SORT_ORDER", nullable = false)
	private int sortOrder;

	public GuidelineFile()
	{
	}

	public Integer getId()
	{
		return id;
	}

	public void setId(Integer id)
	{
		this.id = id;
	}

	public String getFileName()
	{
		return fileName;
	}

	public void setFileName(String fileName)
	{
		this.fileName = fileName;
	}

	public byte[] getFileData()
	{
		return fileData;
	}

	public void setFileData(byte[] fileData)
	{
		this.fileData = fileData;
	}

	public String getDisplayName() {
    	return displayName;
    }

	public void setDisplayName(String displayName) {
    	this.displayName = displayName;
    }

	public String getMimeType() {
    	return mimeType;
    }

	public void setMimeType(String mimeType) {
    	this.mimeType = mimeType;
    }

	public int getSortOrder() {
    	return sortOrder;
    }

	public void setSortOrder(int sortOrder) {
    	this.sortOrder = sortOrder;
    }

	@Override
    public boolean equals(Object obj) {
	    if (this == obj)
		    return true;
	    if (obj == null)
		    return false;
	    if (getClass() != obj.getClass())
		    return false;
	    GuidelineFile other = (GuidelineFile) obj;
	    if (id == null) {
		    if (other.id != null)
			    return false;
	    } else if (!id.equals(other.id))
		    return false;
	    return true;
    }

	
}
