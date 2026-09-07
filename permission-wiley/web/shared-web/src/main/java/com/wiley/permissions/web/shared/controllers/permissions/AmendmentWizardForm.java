package com.wiley.permissions.web.shared.controllers.permissions;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.permissions.domain.persistence.permissions.Amendment;
import com.wiley.permissions.repositories.AssetRepository;

public class AmendmentWizardForm {

	@SuppressWarnings("unused")
	private final static Log log = LogFactory.getLog(AmendmentWizardForm.class);

	private List<Integer> assetIds = new ArrayList<Integer>();
	private Amendment amendment = new Amendment();
	private int cwId;
	private int auId;
	private String prevUrl;
	private String nextUrl;

	private AssetRepository assetRepository;

	public int getCwId() {
		return cwId;
	}

	public void setCwId(int cwId) {
		this.cwId = cwId;
	}

	public void setPrevUrl(String url) {
		prevUrl = url;
	}

	public String getPrevUrl() {
		return prevUrl;
	}

	public Amendment getAmendment() {
		return amendment;
	}

	public void setAmendment(Amendment amendment) {
		this.amendment = amendment;
	}

	public List<Integer> getAssetIds() {
		return assetIds;
	}

	public void setAssetIds(List<Integer> assetIds) {
		this.assetIds = assetIds;
	}

	public int getAuId() {
		return auId;
	}

	public void setAuId(int auId) {
		this.auId = auId;
	}

	public AssetRepository getAssetRepository() {
		return assetRepository;
	}

	public void setAssetRepository(AssetRepository assetRepository) {
		this.assetRepository = assetRepository;
	}

	public String getNextUrl() {
		return nextUrl;
	}

	public void setNextUrl(String nextUrl) {
		this.nextUrl = nextUrl;
	}
}
