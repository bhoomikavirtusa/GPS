package com.wiley.permissions.web.shared;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;

import com.wiley.permissions.security.web.UserPrincipal;

/**
 *
 * @author ttidwell
 */
public class SecurityFilter
implements Filter
{
	private FilterConfig config;

	private String userSessionName = "userSession";

	public void init(FilterConfig config)
	throws ServletException
	{
		setConfig(config);

		String sessionName = config.getInitParameter("sessionName");

		if (StringUtils.isNotBlank(sessionName)) {
			setUserSessionName(sessionName);
		}
	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
	throws IOException, ServletException
	{
		if (request instanceof HttpServletRequest) {
			HttpServletRequest tmpRequest = (HttpServletRequest) request;

			HttpSession session = tmpRequest.getSession(true);

			UserSession userSession = (UserSession) session.getAttribute(userSessionName);

			if (userSession == null) {
				userSession = new UserSession();
				session.setAttribute(userSessionName, new UserSession());
			}

			if (userSession.getCurrentUser() == null) {
				userSession.setCurrentUser((UserPrincipal) tmpRequest.getUserPrincipal());
			}
		}

		chain.doFilter(request, response);
	}

	public void destroy() {
		// Nothing really to do here
	}

	public FilterConfig getConfig() {
		return config;
	}

	public void setConfig(FilterConfig config) {
		this.config = config;
	}

	public String getUserSessionName() {
		return userSessionName;
	}

	public void setUserSessionName(String userSessionName) {
		this.userSessionName = userSessionName;
	}
}
