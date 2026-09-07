package com.wiley.permissions.web.shared.controllers.landing;

import org.springframework.web.multipart.MultipartFile;

import com.wiley.permissions.web.shared.controllers.BaseFormBean;

public class ImportAssetsForm extends BaseFormBean {

	private static final long serialVersionUID = 1L;
	
	private String[] values;
	private MultipartFile file;
	private String filename;
	private boolean forcePersist; 
	
	public String[] getValues()
	{
		return values;
	}
	public void setValues(String[] values)
	{
		this.values = values;
	}
	public String getFilename()
	{
		return filename;
	}
	public void setFilename(String filename)
	{
		this.filename = filename;
	}
	public MultipartFile getFile()
	{
		return file;
	}
	public void setFile(MultipartFile file)
	{
		this.file = file;
	}
	public boolean isForcePersist()
	{
		return forcePersist;
	}
	public void setForcePersist(boolean force)
	{
		this.forcePersist = force;
	}	
}
