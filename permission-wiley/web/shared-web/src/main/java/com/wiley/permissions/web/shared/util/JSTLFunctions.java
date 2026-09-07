package com.wiley.permissions.web.shared.util;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.permissions.domain.persistence.permissions.Asset;
import com.wiley.permissions.domain.persistence.permissions.Source;
import com.wiley.permissions.services.AssetUseSearchResult;
import com.wiley.permissions.web.shared.PermUserContext;
import com.wiley.permissions.web.shared.UserSession;
import com.wiley.sf.common.config.PropertiesUtil;
import com.wiley.sf.common.lang.StringUtil;

public class JSTLFunctions
implements Serializable
{
	private static final long serialVersionUID = 1L;

	private final static Log log = LogFactory.getLog(JSTLFunctions.class);

	// ideally we'd set these directly from Spring but not sure how to
	// set a static field so will load properties file separately from
	// Spring in static init block and set that way
	private static String restUrlInternal;
	private static String restUrlExternal;
	private static String environment;//Added for Build RN Ticket 160209-001330
	private static String prodCountURL;//Added for Build Ticket DM-292

	static {
		String fileName = System.getenv("PERMISSIONS_HOME") + "/permissions.properties";
		PropertiesUtil pu = null;
		try {
			pu = new PropertiesUtil(fileName);
			log.info("static init(): loaded properties from " + fileName);
		}
		catch (IOException e) {
			log.error("static init(): Could not load Properties from: " + fileName);
		}

		if (pu != null) {
			try {
				restUrlInternal = pu.getStringNotBlank("rest.url.internal");
				restUrlExternal = pu.getStringNotBlank("rest.url.external");
				environment = pu.getStringNotBlank("gps.environment");//Added for Build RN Ticket 160209-001330
				prodCountURL = pu.getStringNotBlank("prod.count.request.url");//Added for Build Ticket DM-292
			}
			catch (Exception ex) {
				log.error("static init(): Error reading properties: ", ex);
			}
		}
	}


	public static boolean contains(Collection<?> collection, Object obj) {
		boolean output = false;

		//log.debug("contains(): collection: " + collection + " --obj: " + obj);

		if (collection != null && obj != null) {
			output = collection.contains(obj);
		}

		return output;
	}

	/**
	 * Returns the size of a file
	 */
	public static String filesize(byte[] obj) {
		long output = 0;

		if (null != obj) {
			output = obj.length;
		}
		return FileUtils.byteCountToDisplaySize(output);
	}

	private static boolean getBoolean(Object obj) {
		if (null == obj || !(obj instanceof Boolean)) {
			return false;
		}
		else {
			Boolean b = (Boolean) obj;
			return b.booleanValue();
		}
	}

	public static String formatBoolean(Object obj) {
		return getBoolean(obj) ? "yes" : "no";
	}

	public static String booleanClass(Object obj) {
		return getBoolean(obj) ? "booleanYes" : "booleanNo";
	}

	public static String booleanTD(Object obj) {
		return getBoolean(obj) ? "<td class=\"booleanYes\">yes</td>" : "<td class=\"booleanNo\">no</td>";
	}

	public static String booleanSpan(Object obj) {
		return getBoolean(obj) ? "<span class=\"booleanYes\">yes</span>" : "<span class=\"booleanNo\">no</span>";
	}

	/**
	 * Returns String.valueOf(obj) except truncates the string
	 * to the given limit.
	 * Example:
	 * Input: ("The quick brown fox jumped over the lazy dogs.", 30)
	 * Output: "The quick br...[27 more chars]"
	 */
	public static String truncate(Object obj, int limit, boolean showNumChars) {
		String s = String.valueOf(obj);
		return StringUtil.truncate(s, limit, showNumChars);
	}

	/**
	 * Performs the same function as the standard c:out tag except
	 * also converts new lines to a visible br tag.
	 */
	public static String escapeHtmlAndNewline(Object o) {
		String s = String.valueOf(o);
		s = StringEscapeUtils.escapeHtml4(s);

		StringBuilder sb = new StringBuilder(s.length());
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			switch (c) {
				case '\r':  break;
				case '\n': sb.append("<br />");
				default: sb.append(c);
			}
		}

		return sb.toString();
	}

	public static String sourceAsHTML(Object obj) {
		Source source = (Source) obj;
		List<Source> sources = new ArrayList<Source>();
		sources.add(source);

		return sourcesAsHTML(sources);
	}

	public static String sourcesAsHTML(List<Source> sources) {
		if (sources == null)  return "";

		StringBuilder sb = new StringBuilder();
	    boolean gotOne = false;

		for (Source source : sources) {
			if (gotOne) sb.append(", ");
			if (source.isNofly()) {
				sb.append("<span class='strikethrough'>" + StringEscapeUtils.escapeHtml4(source.getName()) + "</span>");
			}
			else {
				sb.append(StringEscapeUtils.escapeHtml4(source.getName()));
			}
			gotOne = true;
		}

		return sb.toString();
	}

	public static String isActive(Object obj) {
		Asset asset = (Asset) obj;
		StringBuilder sb = new StringBuilder();
		if (asset.isActive())
			sb.append(StringEscapeUtils.escapeHtml4(asset.getDescription()));
		else
			sb.append("<span class='strikethrough'>" + StringEscapeUtils.escapeHtml4(asset.getDescription()) + "</span>");
		return sb.toString();
	}

	/**
	 * If we change all AssetUse lists to use the index, we might not need the isActive
	 * Until then, we keep both
	 * @param obj AssetUseSearchResult
	 * @return String
	 */
	public static String isResultActive(Object obj) {
		AssetUseSearchResult au = (AssetUseSearchResult) obj;
		StringBuilder sb = new StringBuilder();
		if (au.isActive())
			sb.append(StringEscapeUtils.escapeHtml4(au.getDescription()));
		else
			sb.append("<span class='strikethrough'>" + StringEscapeUtils.escapeHtml4(au.getDescription()) + "</span>");
		return sb.toString();
	}

	public static boolean isInstance(Object obj, String className)
	throws ClassNotFoundException
	{
		Class<?> clazz = Class.forName(className);

		return clazz.isInstance(obj);
	}

	/* lnagy - NOT USED ANYMORE - use security:authorize instead
	public static boolean hasPrivilege(HttpServletRequest request, String privilegeCode)
	{
		UserPrincipal up = (UserPrincipal) request.getUserPrincipal();

		if (up != null) {
			return up.hasPrivilege(privilegeCode);
		}

		return false;
	}
	*/

	public static String getRestUrl(HttpServletRequest request) {
		UserSession userSession = PermUserContext.getUserSession(request);
		return userSession.isExternalLogin() ? restUrlExternal : restUrlInternal;
	}

	//Start : Added for Build RN Ticket 160209-001330
	public static String getEnvironmentString() {
		//Usually URL look like - http://permdev.wiley.com:8080
		if(!StringUtils.isEmpty(environment)) {
			environment = environment.trim();
			if(environment.equalsIgnoreCase("QA") || environment.equalsIgnoreCase("DEV")) {
				return environment;
			}
		}
		return "";
	}
	//End : Added for Build RN Ticket 160209-001330
	//Start : Added for Build Ticket DM-292
	public static String getProdCountURL() {
		if(!StringUtils.isEmpty(prodCountURL)) {
			return prodCountURL.trim();
		}
		return "";
	}
	//End : Added for Build Ticket DM-292
}
