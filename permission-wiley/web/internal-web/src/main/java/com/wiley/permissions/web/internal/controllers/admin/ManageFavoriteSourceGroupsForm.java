package com.wiley.permissions.web.internal.controllers.admin;

import java.util.ArrayList;
import java.util.List;

import com.wiley.permissions.domain.persistence.permissions.FavoriteGroup;



public class ManageFavoriteSourceGroupsForm {

	private String favoriteGroupDescription = null;
	private Integer favoriteGroupId = -1;
	private FavoriteGroup favoriteGroup = null;

	private List<FavoriteGroup> favoriteGroups = new ArrayList<FavoriteGroup>();

	public Integer getFavoriteGroupId()
	{
		return favoriteGroupId;
	}

	public void setFavoriteGroupId(Integer favoriteGroupId)
	{
		this.favoriteGroupId = favoriteGroupId;
	}

	public String getFavoriteGroupDescription()
	{
		return favoriteGroupDescription;
	}

	public void setFavoriteGroupDescription(String favoriteGroupDescription)
	{
		this.favoriteGroupDescription = favoriteGroupDescription;
	}

	public List<FavoriteGroup> getFavoriteGroups()
	{
		return this.favoriteGroups;
	}
	public void setFavoriteGroups(List<FavoriteGroup> favoriteGroups) 
	{
		this.favoriteGroups = favoriteGroups;
		
	}
	
	public void setFavoriteGroup(FavoriteGroup group) {
		this.favoriteGroup = group;
	}
	
	public FavoriteGroup getFavoriteGroup() {
		return this.favoriteGroup;
	}

}
