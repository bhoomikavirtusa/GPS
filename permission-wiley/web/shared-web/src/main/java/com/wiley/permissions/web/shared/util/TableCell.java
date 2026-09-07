package com.wiley.permissions.web.shared.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.sf.common.lang.JsonUtil;

/**
 * To be used with DataTablesUtil.
 *
 * We don't bother to jsonEscape the html until getHtml() is called
 * because much of the time only a subset of TableCell objects ever
 * make it to the point where we want to transmit the data. (So this
 * is a performance enhancement).
 *
 * TODO: Eventually move to sf-common2 library.
 *
 * @author smarkoff
 */
public class TableCell {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(TableCell.class);

	private final String data;
	private final String html;

	/**
	 * @param data  May be null
	 */
	public TableCell(String data) {
		this.data = data;
		this.html = (data == null) ? "" : data;
	}

	/**
	 * Automatically formats the boolean as yes/no and with color.
	 */
	public TableCell(boolean b) {
		data = b ? "yes" : "no";
		final String styleClass = b ? "booleanYes" : "booleanNo";
		html = "<span class=\"" + styleClass + "\">" + data + "</span>";
	}

	/**
	 * @param data  May be null
	 * @param html  May be null
	 */
	public TableCell(String data, String html) {
		this.data = data;
		this.html = html;
	}

	/**
	 * @param b Converted to "true" or "false"
	 * @param html  May be null
	 */
	public TableCell(boolean b, String html) {
		this.data = String.valueOf(b);
		this.html = html;
	}

	public String getData() { return data; }
	public String getHtml() { return JsonUtil.escape(html); }
}
