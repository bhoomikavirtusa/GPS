package com.wiley.permissions.services;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author smarkoff
 */
public class LandingFilterForm {

	private boolean useFilter = false;

	private List<Integer> componentIds = new ArrayList<Integer>();
	private String [] mediaTypeCodes;
	private String [] usageCodes;
	private String [] ownerTypeCodes;
	private List<Integer> sourceIds = new ArrayList<Integer>();
	private boolean userSelected;
	private Integer userId;
	private boolean groupSelected;
	private Integer groupId;
	private String [] statusCodes;
	private String customFilter;

	public LandingFilterForm() {

	}

	public boolean getUseFilter() {
		return useFilter;
	}

	public void setUseFilter(boolean b) {
		useFilter = b;
	}

	public List<Integer> getComponentIds() {
		return componentIds;
	}

	public void setComponentIds(List<Integer> componentIds) {
		if (null == componentIds)
			componentIds = new ArrayList<Integer>();
		this.componentIds = componentIds;
	}

	public void addComponentId (int cid) {
		if (!componentIds.contains(cid))
			componentIds.add(cid);
	}

	public String [] getMediaTypeCodes() {
		return mediaTypeCodes;
	}

	public void setMediaTypeCodes(String [] mediaTypeCodes) {
		this.mediaTypeCodes = mediaTypeCodes;
	}

	public String [] getUsageCodes() {
		return usageCodes;
	}

	public void setUsageCodes(String [] usageCodes) {
		this.usageCodes = usageCodes;
	}

	public String [] getOwnerTypeCodes() {
		return ownerTypeCodes;
	}

	public void setOwnerTypeCodes(String [] ownerTypeCodes) {
		this.ownerTypeCodes = ownerTypeCodes;
	}

	public List<Integer> getSourceIds() {
		return sourceIds;
	}

	public void setSourceIds(List<Integer> sourceIds) {
		if (null == sourceIds)
			sourceIds = new ArrayList<Integer>();
		this.sourceIds = sourceIds;
	}

	public boolean isUserSelected() {
		return userSelected;
	}

	public void setUserSelected(boolean b) {
		this.userSelected = b;
	}

	public Integer getUserId() {
		return userId;
	}

	public void setUserId(Integer userId) {
		this.userId = userId;
	}

	public boolean isGroupSelected() {
		return groupSelected;
	}

	public void setGroupSelected(boolean b) {
		this.groupSelected = b;
	}

	public Integer getGroupId() {
		return groupId;
	}

	public void setGroupId(Integer groupId) {
		this.groupId = groupId;
	}

	public String [] getStatusCodes() {
		return statusCodes;
	}

	public void setStatusCodes(String [] statusCodes) {
		this.statusCodes = statusCodes;
	}

	public String getCustomFilter() {
		return customFilter;
	}

	public void setCustomFilter(String customFilter) {
		this.customFilter = customFilter;
	}
}
