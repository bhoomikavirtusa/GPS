package com.wiley.permissions.web.shared.controllers.landing;

import java.util.List;

import com.wiley.permissions.domain.persistence.permissions.Component;

public class ChapterMergeForm {

	private int cwId;
	private int selectedChapter;
	private List<AssetRenumberView> renumList;
	private List<Component> components;
	private boolean chaptersCheckBox = false;
	private boolean appendicesCheckBox = false;
	private String currentLargestChapter;
	private String currentLargestAppendix;
	private String chapterFrom;
	private String chapterTo;
	private String appendicesFrom;
	private String appendicesTo;

	public List<Component> getComponents() {
		return components;
	}

	public void setComponents(List<Component> components) {
		this.components = components;
	}

	public Component getNextComponent(Component after) {
		Component dcomp = null;

		for (int x=0; x < this.components.size(); x++) {
			if (components.get(x).getName().compareTo(after.getName()) > 0 ) return this.components.get(x);
		}

		return dcomp;
	}

	public List<AssetRenumberView> getRenumList() {
		return renumList;
	}

	public void setRenumList(List<AssetRenumberView> renumList) {
		this.renumList = renumList;
	}

	public int getCwId()
	{
		return cwId;
	}

	public void setCwId(int cwId)
	{
		this.cwId = cwId;
	}

	public int getSelectedChapter()
	{
		return selectedChapter;
	}

	public void setSelectedChapter(int Chapter)
	{
		this.selectedChapter = Chapter;
	}
	
	public boolean getChaptersCheckBox()
	{
		return chaptersCheckBox;
	}

	public void setChaptersCheckBox(boolean chapterCheckBox)
	{
		this.chaptersCheckBox = chapterCheckBox;
	}
	
	public boolean getAppendicesCheckBox()
	{
		return appendicesCheckBox;
	}

	public void setAppendicesCheckBox(boolean appendicesCheckBox)
	{
		this.appendicesCheckBox = appendicesCheckBox;
	}
	
	public String getChapterFrom()
	{
		return chapterFrom;
	}

	public void setChapterFrom(String chapterFrom)
	{
		this.chapterFrom = chapterFrom;
	}
	
	public String getChapterTo()
	{
		return chapterTo;
	}

	public void setChapterTo(String chapterTo)
	{
		this.chapterTo = chapterTo;
	}
	
	public String getAppendicesFrom()
	{
		return appendicesFrom;
	}

	public void setAppendicesFrom(String appendicesFrom)
	{
		this.appendicesFrom = appendicesFrom;
	}
	
	public String getAppendicesTo()
	{
		return appendicesTo;
	}

	public void setAppendicesTo(String appendicesTo)
	{
		this.appendicesTo = appendicesTo;
	}
	
	
	public String getCurrentLargestChapter()
	{
		return currentLargestChapter;
	}

	public void setCurrentLargestChapter(String currentLargestChapter)
	{
		this.currentLargestChapter = currentLargestChapter;
	}
	
	public String getCurrentLargestAppendix()
	{
		return currentLargestAppendix;
	}

	public void setCurrentLargestAppendix(String currentLargestAppendix)
	{
		this.currentLargestAppendix = currentLargestAppendix;
	}
	
	
}
