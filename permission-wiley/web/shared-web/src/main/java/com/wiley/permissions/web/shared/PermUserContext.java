package com.wiley.permissions.web.shared;

import java.text.SimpleDateFormat;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.permissions.domain.persistence.permissions.CommonWork;
import com.wiley.permissions.security.web.UserPrincipal;

/**
 * A utility class to access user information and details like the current
 * product the user working on and other details.
 */
public class PermUserContext
{
	public static final String USER_SESSION_NAME = "userSession";
	public static final String CURRENT_CW = "current_common_work";
	//public static final String CANCEL_MAP_KEY = "cancelMap";
	public static final String DATE_FORMAT = "dateFormat";

    @SuppressWarnings("unused")
	private final static Log log = LogFactory.getLog(PermUserContext.class);

	/**
	 * It returns the Current User Session
	 *
	 * @param request
	 * @return UserSession
	 * @see <code>com.wiley.permissions.web.UserSession</code>
	 */
	public static UserSession getUserSession(HttpServletRequest request)
	{
		HttpSession session = request.getSession(false);

		if (session == null)  return null;
		else {
			return (UserSession) session.getAttribute(USER_SESSION_NAME);
		}
	}

	public static void setUserSession(UserSession userSession, HttpServletRequest request) {
		HttpSession session = request.getSession(true);
		session.setAttribute(USER_SESSION_NAME, userSession);
	}

	public static void setCurrentCommonWork(HttpSession httpSession, CommonWork commonWork) {
		if (commonWork != null) {
			httpSession.setAttribute(CURRENT_CW, commonWork);
		}
	}

	public static void setCurrentCommonWork(HttpServletRequest request, CommonWork commonWork) {
		HttpSession session = request.getSession(false);
		setCurrentCommonWork(session, commonWork);
	}

	public static CommonWork getCurrentCommonWork(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		CommonWork cw = (CommonWork) session.getAttribute(CURRENT_CW);

		return cw;
	}

	public static void setDateFormat(HttpSession httpSession, String dateFormat) {
		if (dateFormat != null) {
			httpSession.setAttribute(DATE_FORMAT, dateFormat);
		}
	}

	public static void setDateFormat(HttpServletRequest request, String dateFormat) {
		HttpSession session = request.getSession(false);
		dateFormat = dateFormat.toLowerCase();
		dateFormat = dateFormat.replace("yyyy", "yy");
		setDateFormat(session, dateFormat);
	}

	public static String getDateFormat(HttpServletRequest request) {
		HttpSession session = request.getSession(false);

		return (String) session.getAttribute(DATE_FORMAT);
	}

	/**
	 * Returns standard date format used internally (regardless of user format).
	 */
	public static SimpleDateFormat getInternalDateFormat() {
		return new SimpleDateFormat("MM/dd/yyyy");
	}

	public static SimpleDateFormat getSimpleDateFormat(HttpServletRequest request) {
		String formatString = "MM/dd/yyyy";  // default
		if (StringUtils.isNotBlank(getDateFormat(request))) {
			formatString = getDateFormat(request);
			formatString = formatString.replace("mm", "MM");
		}
		return new SimpleDateFormat(formatString);
	}

	public static String getPickerDateFormat(HttpServletRequest request) {
		String formatString = "mm/dd/yy";
		if (null != PermUserContext.getDateFormat(request)) {
			formatString = PermUserContext.getDateFormat(request).toLowerCase();
			formatString = formatString.replace("yyyy", "yy");
		}
		return formatString;
	}
/*
	@SuppressWarnings("unchecked")
	public static Map<Integer, Boolean> getCancelMap (HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		return (Map<Integer, Boolean>) session.getAttribute(CANCEL_MAP_KEY);
	}

	public static void setCancelMap (HttpServletRequest request,  Map<Integer, Boolean> cancelMap) {
		HttpSession session = request.getSession(false);
		session.setAttribute(CANCEL_MAP_KEY, cancelMap);
	}
*/
	public static Integer getCurrentUserId(HttpServletRequest request)
	throws Exception
	{
		UserSession session = getUserSession(request);

		if (session == null) {
			throw new Exception("No User Session");
		}
		else  {
			return session.getCurrentUser().getId();
		}
	}

	public static UserPrincipal getCurrentUser(HttpServletRequest request) {
		UserSession userSession = getUserSession(request);
		return userSession.getCurrentUser();
	}
}
