package com.wiley.permissions.web.internal.controllers.admin;

import java.util.ArrayList;
import java.util.List;

import com.wiley.permissions.domain.persistence.permissions.SourceGroup;
import com.wiley.permissions.web.shared.controllers.BaseFormBean;

/**
 *
 * @author ttidwell
 */
public class ManageSourceGroupForm
extends BaseFormBean
{
	private static final long serialVersionUID = 1L;

	private FormMode originalMode = null;
	private SourceGroup group = null;
	private Integer selectedSource = null;
	private Integer selectedChild = null;
	private Integer selectedDeal = null;
	private Integer selectedDealChild = null;
	private List<Integer> removedSources = new ArrayList<Integer>();
	private String sourceName;
	private String dealName;
	private Integer seats;
	private Integer totalPrintRun;
	private boolean nofly;
	private String sourceGroupComment;

	//sandhya

	private boolean readytosublicense;

	public boolean isReadytosublicense() {
		return readytosublicense;
	}

	public void setReadytosublicense(boolean readytosublicense) {
		this.readytosublicense = readytosublicense;
	}

	//sandhya
	public ManageSourceGroupForm() {
	}

	public FormMode getOriginalMode() {
		return originalMode;
	}

	public void setNofly(boolean nofly) {
		this.nofly = nofly;
	}

	public boolean getNofly() {
		return nofly;
	}

	public void setSourceGroupComment(String sourceGroupComment) {
		this.sourceGroupComment = sourceGroupComment;
	}

	public String getSourceGroupComment() {
		return sourceGroupComment;
	}


	public void setOriginalMode(FormMode originalMode) {
		this.originalMode = originalMode;
	}

	public String getSourceName() {
		return sourceName;
	}

	public void setSourceName(String sourceName) {
		this.sourceName = sourceName;
	}

	public String getDealName() {
		return dealName;
	}

	public void setDealName(String dealName) {
		this.dealName = dealName;
	}

	public SourceGroup getGroup() {
		return group;
	}

	public void setGroup(SourceGroup group) {
		this.group = group;
	}

	public Integer getSelectedSource() {
		return selectedSource;
	}

	public void setSelectedSource(Integer selectedSource) {
		this.selectedSource = selectedSource;
	}

	public Integer getSelectedChild() {
		return selectedChild;
	}

	public void setSelectedChild(Integer selectedChild) {
		this.selectedChild = selectedChild;
	}

	public Integer getSelectedDeal() {
		return selectedDeal;
	}

	public void setSelectedDeal(Integer selectedDeal) {
		this.selectedDeal = selectedDeal;
	}

	public Integer getSelectedDealChild() {
		return selectedDealChild;
	}

	public void setSelectedDealChild(Integer selectedDealChild) {
		this.selectedDealChild = selectedDealChild;
	}

	public List<Integer> getRemovedSources() {
		return removedSources;
	}

	public void setRemovedSources(List<Integer> removedSources) {
		this.removedSources = removedSources;
	}

	public Integer getSeats() {
		return seats;
	}

	public void setSeats(Integer seats) {
		this.seats = seats;
	}

	public Integer getTotalPrintRun() {
		return totalPrintRun;
	}

	public void setTotalPrintRun(Integer totalPrintRun) {
		this.totalPrintRun = totalPrintRun;
	}

	public void clear() {
		setDealName(null);
		setSeats(null);
		setTotalPrintRun(null);
	}
}
