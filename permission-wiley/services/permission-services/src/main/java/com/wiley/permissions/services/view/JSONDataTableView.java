package com.wiley.permissions.services.view;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class JSONDataTableView {

	private int totalRecords;
	private int totalDisplayRecords;
	private List<?> tableRows;

	public JSONDataTableView() {
	}

	@XmlElement(name="iTotalRecords")
	public int getTotalRecords() {
		return totalRecords;
	}

	public void setTotalRecords(int totalRecords) {
		this.totalRecords = totalRecords;
	}

	@XmlElement(name="iTotalDisplayRecords")
	public int getTotalDisplayRecords() {
		return totalDisplayRecords;
	}

	public void setTotalDisplayRecords(int totalDisplayRecords) {
		this.totalDisplayRecords = totalDisplayRecords;
	}

	@XmlElement (name="aaData")
	public List<?> getTableRows() {
		return tableRows;
	}

	public void setTableRows(List<?> tableRows) {
		this.tableRows = tableRows;
	}
}
