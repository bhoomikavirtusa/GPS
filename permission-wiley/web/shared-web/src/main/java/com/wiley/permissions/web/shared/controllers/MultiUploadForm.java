package com.wiley.permissions.web.shared.controllers;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

public class MultiUploadForm {

	public final static String FORM_MODEL_NAME = "multiUploadForm";

	private List<MultipartFile> files;

	public List<MultipartFile> getFiles()
	{
		return files;
	}

	public void setFiles(List<MultipartFile> files)
	{
		this.files = files;
	}
}
