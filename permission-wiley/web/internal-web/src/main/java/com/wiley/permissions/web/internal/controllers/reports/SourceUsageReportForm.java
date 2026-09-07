package com.wiley.permissions.web.internal.controllers.reports;

import java.util.ArrayList;
import java.util.Date;

import com.wiley.permissions.domain.persistence.permissions.Source;
import com.wiley.permissions.domain.persistence.permissions.User;
import com.wiley.permissions.web.shared.controllers.BaseFormBean;

/**
 *
 * @author nmedrano
 */
public class SourceUsageReportForm
extends BaseFormBean
{
	private static final long serialVersionUID = 1L;

	private User user = null;
	private Integer favoriteGroupId = null;
	private Integer userGroupId; 
	private String searchSourceName;
	private Integer selectedSourceId;
	private ArrayList<Source> sources;
	private Integer dateType;
	private Date fromDate;
	private Date toDate;
	private String division;
	private String productLines;  // comma separated list
	private String productStatus;
	private Integer reportType;
	private Integer assetsToShow;
	private String processingType;
	private Integer selectedSourceGroup;


	public SourceUsageReportForm() {}


	public Integer getSelectedSourceGroup() {
		return selectedSourceGroup;
	}
	public void setSelectedSourceGroup(Integer selectedSourceGroup) {
		this.selectedSourceGroup = selectedSourceGroup;
	}
	
	public String getProcessingType() {
		return processingType;
	}
	public void setProcessingType(String processingType) {
		this.processingType = processingType;
	}

	public Integer getSelectedSourceId() {
		return selectedSourceId;
	}
	public void setSelectedSourceId(Integer selectedSourceId) {
		this.selectedSourceId = selectedSourceId;
	}


	public Integer getAssetsToShow() {
		return assetsToShow;
	}
	public void setAssetsToShow(Integer assetsToShow) {
		this.assetsToShow = assetsToShow;
	}

	public Integer getReportType() {
		return reportType;
	}
	public void setReportType(Integer reportType) {
		this.reportType = reportType;
	}

	public String getProductStatus() {
		return productStatus;
	}
	public void setProductStatus(String productStatus) {
		this.productStatus = productStatus;
	}

	public String getProductLines() {
		return productLines;
	}
	public void setProductLines(String productLines) {
		this.productLines = productLines;
	}


	public String getDivision() {
		return division;
	}
	public void setDivision(String division) {
		this.division = division;
	}

	public Date getToDate() {
		return toDate;
	}
	public void setToDate(Date toDate) {
		this.toDate = toDate;
	}

	public Date  getFromDate() {
		return fromDate;
	}
	public void setFromDate(Date fromDate) {
		this.fromDate = fromDate;
	}

	public Integer  getDateType() {
		return dateType;
	}
	public void setDateType(Integer dateType) {
		this.dateType = dateType;
	}

	public ArrayList<Source>  getSources() {
		return sources;
	}
	public void setSources(ArrayList<Source> sources) {
		this.sources = sources;
	}

	public String getSearchSourceName() {
		return searchSourceName;
	}

	public void setSearchSourceName(String searchSourceName) {
		this.searchSourceName = searchSourceName;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public void setFavoriteGroupId(Integer favoriteGroupId) {
		this.favoriteGroupId = favoriteGroupId;
	}

	public Integer getFavoriteGroupId() {
		return favoriteGroupId;
	}

	public void setUserGroupId(Integer userGroupId) {
		this.userGroupId = userGroupId;
	}

	public Integer getUserGroupId() {
		return userGroupId;
	}
}
