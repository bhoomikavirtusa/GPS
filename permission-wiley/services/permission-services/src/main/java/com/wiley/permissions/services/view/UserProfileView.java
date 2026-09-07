package com.wiley.permissions.services.view;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import com.wiley.permissions.domain.persistence.permissions.UserProfile;

public class UserProfileView {

	private String name;
	private String title;
	private boolean visible;
	private boolean required;
	private boolean sortable;
	private boolean searchable;
	private String style;
	private String sortType;
	private String widthPercentage;
	private Integer sortOrder;
	
	public UserProfileView() {
	}

	public UserProfileView(UserProfile up) {
		name = up.getName();
		title = up.getTitle();
		visible = up.isVisible();
		required = up.isRequired();
		sortable = up.isSortable();
		searchable = up.isSearchable();
		sortType = up.getSortType();
		sortOrder = up.getSortOrder();
		style = (up.isEditable() ? "editable" : "non_editable") + " " + name;
		widthPercentage = up.getWidth() + "%";
	}

	@XmlElement (name="sName")
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@XmlElement (name="sTitle")
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	@XmlElement (name="bVisible")	
	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	@XmlTransient
	public boolean isRequired() {
		return required;
	}

	public void setRequired(boolean required) {
		this.required = required;
	}

	@XmlElement (name="bSortable")	
	public boolean isSortable() {
		return sortable;
	}

	public void setSortable(boolean sortable) {
		this.sortable = sortable;
	}

	@XmlElement (name="bSearchable")	
	public boolean isSearchable() {
		return searchable;
	}

	public void setSearchable(boolean searchable) {
		this.searchable = searchable;
	}

	@XmlElement (name="sClass")
	public String getStyle() {
		return style;
	}

	public void setStyle(String style) {
		this.style = style;
	}

	@XmlElement (name="sWidth")
	public String getWidthPercentage() {
		return widthPercentage;
	}

	public void setWidthPercentage(String widthPercentage) {
		this.widthPercentage = widthPercentage;
	}

	@XmlElement (name="sType")
	public String getSortType() {
		return sortType;
	}

	public void setSortType(String sType) {
		this.sortType = sType;
	}
	
	public Integer getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(Integer sortOrder) {
		this.sortOrder = sortOrder;
	}
}
