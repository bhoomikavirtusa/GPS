package com.wiley.permissions.web.internal.controllers.admin;

import java.util.ArrayList;
import java.util.List;

import com.wiley.permissions.domain.persistence.permissions.Source;
import com.wiley.permissions.persistence.LabelValueBean;

public class MultiSourceForm {

	private String sourceSearch;

	private Integer sourceGroupId;
	private Integer favoriteSourceGroupId;

	private List<Source> sources = new ArrayList<Source>();

	private List<LabelValueBean> selectedSources = new ArrayList<LabelValueBean>();


	public Integer getSourceGroupId()
	{
		return sourceGroupId;
	}
	public Integer getFavoriteSourceGroupId()
	{
		return favoriteSourceGroupId;
	}

	public void setSelectedSources(List<LabelValueBean> selectedSources)
	{
		this.selectedSources = selectedSources;
	}

	public List<LabelValueBean> getSelectedSources()
	{
		return this.selectedSources;
	}

	public void setSources(List<Source> sources)
	{
		this.sources = sources;
	}

	public List<Source> getSources()
	{
		return this.sources;
	}

	public void setSourceGroupId(Integer id)
	{
		this.sourceGroupId = id;
	}
	
	public void setFavoriteSourceGroupId(Integer id)
	{
		this.favoriteSourceGroupId = id;
	}

	public String getSourceSearch()
	{
		return sourceSearch;
	}

	public void setSourceSearch(String sourceSearch)
	{
		this.sourceSearch = sourceSearch;
	}

	public boolean isOnlyOneSourceInList() {
		if (this.getSelectedSources().size() == 1) return true;
		return false;
	}

}
