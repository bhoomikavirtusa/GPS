package com.wiley.permissions.web.shared;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;


@Component
public class CustomUsernamePasswordAuthenticationFilter extends UsernamePasswordAuthenticationFilter {
	private final static Log log = LogFactory.getLog(CustomUsernamePasswordAuthenticationFilter.class);
	public CustomUsernamePasswordAuthenticationFilter() {
		log.debug("CustomUsernamePasswordAuthenticationFilter");
		setRequiresAuthenticationRequestMatcher(new AntPathRequestMatcher("/j_spring_security_check", "GET"));
	}
}
