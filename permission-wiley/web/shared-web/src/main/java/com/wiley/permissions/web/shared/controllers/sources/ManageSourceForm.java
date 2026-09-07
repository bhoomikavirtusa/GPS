package com.wiley.permissions.web.shared.controllers.sources;

import org.springframework.web.multipart.MultipartFile;

import com.wiley.permissions.domain.persistence.permissions.Source;
import com.wiley.permissions.domain.persistence.permissions.SourceFile;
import com.wiley.permissions.web.shared.controllers.BaseFormBean;

/**
 *
 * @author ttidwell
 */
public class ManageSourceForm
extends BaseFormBean
{
	private static final long serialVersionUID = 1L;

	public enum ManagementMode {
		GENERAL,
		FILES,
		NOFLY
	}

	private Integer assetId;
	private Source source;
	private FormMode originalMode;
	private ManagementMode managementMode = ManagementMode.GENERAL;
	private MultipartFile newMultipartFile;
	private SourceFile newFile = new SourceFile();
	private int fileToRemove = -1;

	public ManageSourceForm() {
	}

	public Integer getAssetId() {
		return assetId;
	}

	public void setAssetId(Integer assetId) {
		this.assetId = assetId;
	}

	public Source getSource() {
		return source;
	}

	public void setSource(Source source) {
		this.source = source;
	}

	public FormMode getOriginalMode() {
		return originalMode;
	}

	public void setOriginalMode(FormMode originalMode) {
		this.originalMode = originalMode;
	}

	public ManagementMode getManagementMode() {
		return managementMode;
	}

	public void setManagementMode(ManagementMode managementMode) {
		this.managementMode = managementMode;
	}

	public MultipartFile getNewMultipartFile() {
		return newMultipartFile;
	}

	public void setNewMultipartFile(MultipartFile newMultipartFile) {
		this.newMultipartFile = newMultipartFile;
	}

	public int getFileToRemove() {
		return fileToRemove;
	}

	public void setFileToRemove(int fileToRemove) {
		this.fileToRemove = fileToRemove;
	}

	public SourceFile getNewFile() {
		return newFile;
	}

	public void setNewFile(SourceFile newFile) {
		this.newFile = newFile;
	}

	@Override
	public String toString() {
		return super.toString()
		    + ", assetId = " + assetId
		    + ", source = " + source
		    + ", (other fields not displayed)";
	}
}
