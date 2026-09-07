package com.wiley.permissions.web.shared.controllers;

import org.springframework.web.multipart.MultipartFile;

public class UploadForm {
	
	public final static String FORM_MODEL_NAME = "uploadForm";
	
	private MultipartFile file;

	public MultipartFile getFile()
	{
		return file;
	}

	public void setFile(MultipartFile file)
	{
		this.file = file;
	}	
}
