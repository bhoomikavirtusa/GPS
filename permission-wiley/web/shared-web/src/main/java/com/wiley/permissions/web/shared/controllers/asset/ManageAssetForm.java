package com.wiley.permissions.web.shared.controllers.asset;

import com.wiley.permissions.domain.persistence.permissions.AssetUse;
 
public class ManageAssetForm {

	private AssetUse assetUse;
	// flag as replacement 
	private Integer assetBeingReplaced;
	private boolean replaceAll;
	

	public void setAssetBeingReplaced(Integer assetBeingReplaced) {
		this.assetBeingReplaced = assetBeingReplaced;
	}

	public Integer  getAssetBeingReplaced() {
		return assetBeingReplaced;
	}
	
	public void setReplaceAll(boolean replaceAll) {
		this.replaceAll = replaceAll;
	}

	public boolean  getReplaceAll() {
		return replaceAll;
	}
	
	public AssetUse getAssetUse() {
		return assetUse;
	}

	public void setAssetUse(AssetUse assetUse) {
		this.assetUse = assetUse;
	}
	
}
