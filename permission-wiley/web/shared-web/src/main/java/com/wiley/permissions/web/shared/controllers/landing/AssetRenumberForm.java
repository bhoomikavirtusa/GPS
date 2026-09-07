package com.wiley.permissions.web.shared.controllers.landing;

import java.util.List;

public class AssetRenumberForm {

	private int cwId;
	private int selectedComponent;
	private String [] usageCodes;
	private List<AssetRenumberView> renumList;


	public List<AssetRenumberView> getRenumList() {
		return renumList;
	}

	public void setRenumList(List<AssetRenumberView> renumList) {
		this.renumList = renumList;
	}

	public String [] getUsageCodes() {
		return usageCodes;
	}

	public void setUsageCodes(String [] usageCodes) {
		this.usageCodes = usageCodes;
	}

	public int getCwId()
	{
		return cwId;
	}

	public void setCwId(int cwId)
	{
		this.cwId = cwId;
	}

	public int getSelectedComponent()
	{
		return selectedComponent;
	}

	public void setSelectedComponent(int component)
	{
		this.selectedComponent = component;
	}
}
