package com.wiley.permissions.web.shared;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author smarkoff
 */
public class NoCacheFilter implements Filter
{
	private static final Log log = LogFactory.getLog(NoCacheFilter.class);

	private String special;

	/** Implements Filter interface. */
	@Override
	public void init(FilterConfig config) throws ServletException {
		special = config.getInitParameter("special");
		log.debug("init(): special = [" + special + "]");
	}

	/** Implements Filter interface. */
	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException
    {
		HttpServletRequest req = (HttpServletRequest) request;
		//log.debug("doFilter(): called for path: " + req.getRequestURI());
		HttpServletResponse resp = (HttpServletResponse) response;
		// uri will look like [/contextPath]/sapp/landing/rowTemplate2
		if (StringUtils.isNotBlank(special) && req.getRequestURI().endsWith(special)) {
			resp.setHeader("Cache-Control", "max-age=20, must-revalidate");
		}
		else {
			resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");

			// the above is sufficient for Firefox and Chrome but not for IE 8 (and not tested IE 9 yet)
			resp.setHeader("Expires", "Tue, 01 Oct 2013 06:00:00 GMT");  // any date in the past will do
			//resp.setHeader("Last-Modified", new Date().toString());
		}

		chain.doFilter(request, response);
    }

	/** Implements Filter interface. */
	@Override
	public void destroy() {

	}
}
