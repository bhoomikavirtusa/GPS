package com.wiley.permissions.services;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;

/**
 * This form is in the services package so it can be use by ProductIndexService.
 *
 * @author smarkoff
 */
public class ComplianceForm {

	private static Date stringToDate(String s) throws ParseException {
		SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
		return dateFormat.parse(s);
	}

	private static int stringToYear(String s) {
		// input string is expected to either be whole date (MM/dd/yyyy)
		// or just the year (yyyy)
		// input string is expected to already be trimmed (by setFrom/ToDate)
		try {
			if (s.length() == 4) {
				return Integer.parseInt(s);
			}
			Date date = stringToDate(s);
			Calendar cal = Calendar.getInstance();
			cal.setTime(date);
			return cal.get(Calendar.YEAR);
		}
		catch (ParseException ex) {
			return 0;
		}
		catch (NumberFormatException ex) {
			return 0;
		}
	}

	private String dateCriterion;
	private String fromDate;
	private String toDate;
	private String businessUnitCode;
	private String productLineCode;
	private String editorCode;
	private boolean inProduction;
	private boolean useFilter = true;


	public String getDateCriterion() {
		return dateCriterion;
	}
	public void setDateCriterion(String dateCriterion) {
		this.dateCriterion = dateCriterion;
	}

	public String getFromDate() {
		return fromDate;
	}
	public long getFromDateMS() {
		try {
			Date date = stringToDate(fromDate);
			return date.getTime();
		}
		catch (ParseException ex) {
			return 0;  // 1970
		}
	}
	public int getFromDateYear() {
		return stringToYear(fromDate);
	}

	public void setFromDate(String fromDate) {
		this.fromDate = StringUtils.trimToEmpty(fromDate);
	}

	public String getToDate() {
		return toDate;
	}
	public long getToDateMS() {
		try {
			Date date = stringToDate(toDate);
			return date.getTime();
		}
		catch (ParseException ex) {
			return 0;  // 1970
		}
	}
	public int getToDateYear() {
		return stringToYear(toDate);
	}

	public void setToDate(String toDate) {
		this.toDate = StringUtils.trimToEmpty(toDate);
	}

	public String getBusinessUnitCode() {
		return businessUnitCode;
	}
	public void setBusinessUnitCode(String businessUnitCode) {
		this.businessUnitCode = businessUnitCode;
	}

	public String getProductLineCode() {
		return productLineCode;
	}
	public void setProductLineCode(String productLineCode) {
		this.productLineCode = productLineCode;
	}

	public String getEditorCode() {
		return editorCode;
	}
	public void setEditorCode(String editorCode) {
		this.editorCode = editorCode;
	}

	public void setInProduction(boolean inProduction) {
		this.inProduction = inProduction;
	}
	public boolean isInProduction() {
		return inProduction;
	}

	public void setUseFilter(boolean useFilter) {
		this.useFilter = useFilter;
	}
	public boolean isUseFilter() {
		return useFilter;
	}

	public String getQueryString() {
		StringBuilder sb = new StringBuilder();
		sb.append("&businessUnitCode=").append(businessUnitCode);
		sb.append("&inProduction=").append(inProduction);
		if (StringUtils.isNotBlank(dateCriterion)) {
			sb.append("&dateCriterion=").append(dateCriterion);
		}
		sb.append("&fromDate=").append(fromDate);
		sb.append("&toDate=").append(toDate);
		if (StringUtils.isNotBlank(productLineCode)) {
			sb.append("&productLineCode=").append(productLineCode);
		}
		if (StringUtils.isNotBlank(editorCode)) {
			sb.append("&editorCode=").append(editorCode);
		}
		sb.append("&useFilter=").append(useFilter);
		return sb.toString();
	}
}
