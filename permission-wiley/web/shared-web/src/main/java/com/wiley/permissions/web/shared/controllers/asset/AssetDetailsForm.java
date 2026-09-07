package com.wiley.permissions.web.shared.controllers.asset;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.wiley.permissions.domain.persistence.permissions.Asset;
import com.wiley.permissions.domain.persistence.permissions.AssetUse;
import com.wiley.permissions.domain.persistence.permissions.CommonWorkStatus;

public class AssetDetailsForm {

	private AssetUse assetUse = new AssetUse();
	private MultipartFile assetFile = null;
	private boolean takenFromAnotherPublication = false;
	private boolean searching = false;
	private boolean hasBiblio;

	public boolean isSearching() {
		return searching;
	}

	public boolean getSearching() {
		return searching;
	}

	public void setSearching(boolean value) {
		searching = value;
	}

	public AssetUse getAssetUse() {
		return assetUse;
	}

	public void setAssetUse(AssetUse assetUse) {
		this.assetUse = assetUse;
	}

	public MultipartFile getAssetFile() {
		return assetFile;
	}

	public void setAssetFile(MultipartFile assetFile) {
		this.assetFile = assetFile;
	}

	public boolean getHasBiblio() {
		Asset dAsset = assetUse.getAsset();
		if (null != dAsset) {
			if (StringUtils.isNotBlank(dAsset.getCitation()) ||
				StringUtils.isNotBlank(dAsset.getBiblio())) {
				hasBiblio = true;
			}
		}
		return hasBiblio;
	}

	public void setHasBiblio(boolean hasBiblio) {
		this.hasBiblio = hasBiblio;
		if (!hasBiblio) {
			Asset dAsset = assetUse.getAsset();
			dAsset.setCitation(null);
			dAsset.setBiblio(null);
		}
	}

	public void setTakenFromAnotherPublication(boolean value) {
		takenFromAnotherPublication = value;
	}

	public boolean getTakenFromAnotherPublication() {
		Asset dAsset = assetUse.getAsset();
		if (null != dAsset) {
			if (StringUtils.isNotBlank(dAsset.getOriginalPublicationIsbn()) ||
				StringUtils.isNotBlank(dAsset.getOriginalPublicationTitle()) ||
				StringUtils.isNotBlank(dAsset.getOriginalArticleTitle())) {
				takenFromAnotherPublication = true;
			}
		}

		return takenFromAnotherPublication;
	}

	// not sure this method is being used
	public boolean isNoPermissionsRequired5() {
		return assetUse.getCommonWork().getInteriorCWStatus().equals(CommonWorkStatus.NO_PERMISSIONS_REQUIRED);
	}
}
