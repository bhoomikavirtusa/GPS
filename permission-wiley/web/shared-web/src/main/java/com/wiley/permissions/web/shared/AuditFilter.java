package com.wiley.permissions.web.shared;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.permissions.security.web.ThreadLocalUser;
import com.wiley.permissions.security.web.UserPrincipal;

/**
 * We want to store the current userId as a ThreadLocal variable so
 * it can be used by transactions that require a userId for auditing purposes.
 *
 * @author smarkoff
 */
public class AuditFilter
implements Filter
{
	@SuppressWarnings("unused")
	private final static Log log = LogFactory.getLog(AuditFilter.class);


	public void init(FilterConfig config)
	throws ServletException
	{
		// Do nothing here, we have to wait for the transaction manager
		// to be loaded by the spring listener and all that.
	}

	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
	throws IOException, ServletException
	{
		HttpServletRequest request = (HttpServletRequest) servletRequest;
		UserPrincipal user = null;

		try {
			user = PermUserContext.getCurrentUser(request);
			//log.debug("doFilter: setting userId to " + userId);
			//log.debug("doFilter(): currentThread.id = " + Thread.currentThread().getId());
			ThreadLocalUser.set(user);
			// set userId on request so can be used by jsp's that use ParameterizableViewController
			// or controller doesn't bother to set userId
			request.setAttribute("currentUserId", user.getId());
		}
		catch (Exception e) {
			// Nothing to do here, it means there is no user session yet
			//log.debug("doFilter(): caught exception from getCurrentUserId(): ", ex);
		}

		chain.doFilter(request, servletResponse);  // throws IOException, ServletException

		// If the doFilter() method above throws an exception, I believe the ThreadLocalUserId
		// will still be cleaned up fine by garbage collection.
		ThreadLocalUser.cleanup();
	}

	public void destroy() {
		// do nothing
	}
}
