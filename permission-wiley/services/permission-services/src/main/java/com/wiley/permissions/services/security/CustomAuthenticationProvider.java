package com.wiley.permissions.services.security;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.wiley.permissions.repositories.UserRepository;
import com.wiley.permissions.security.web.CredentialEncryptionUtility;
import com.wiley.permissions.security.web.UserPrincipal;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.WebAttributes;

import java.io.IOException;

/*public class CustomAuthenticationProvider implements AuthenticationSuccessHandler  {
	private RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();
	private UserRepository userRepository;
	private final static Log log = LogFactory.getLog(CustomAuthenticationProvider.class);*/
public class CustomAuthenticationProvider implements AuthenticationProvider {

	private final static Log log = LogFactory.getLog(CustomAuthenticationProvider.class);

	private UserRepository userRepository;

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		log.debug("Inside custom Authentication provider");
	    UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) authentication;
	    String username = String.valueOf(auth.getPrincipal());
	    String password = String.valueOf(auth.getCredentials());

	    log.debug("username:" + username);
	    log.debug("password:" + password);

	    try {
			String plainCred = CredentialEncryptionUtility.decrypt(username, password);
			UserPrincipal up = userRepository.loadUserForLogin(plainCred);
			return up;
	    } catch (Exception e) {
	    	e.printStackTrace();
	    	throw new BadCredentialsException("Username/Password does not match for " + username);
	    }
	}
/*
	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)throws IOException {
	handle(request, response, authentication);
	clearAuthenticationAttributes(request);
}
	
	protected void handle(HttpServletRequest request, HttpServletResponse response, Authentication authentication)throws IOException {
			String targetUrl = determineTargetUrl(authentication);
			if (response.isCommitted()) {
			log.debug("Response has already been committed. Unable to redirect to "+ targetUrl);
			return;
			}
			
			redirectStrategy.sendRedirect(request, response, targetUrl);
}
	protected String determineTargetUrl(Authentication authentication) {
		UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) authentication;
	    String username = String.valueOf(auth.getPrincipal());
	    String password = String.valueOf(auth.getCredentials());

	    log.debug("username:" + username);
	    log.debug("password:" + password);

	    try {
			String plainCred = CredentialEncryptionUtility.decrypt(username, password);
			UserPrincipal up = userRepository.loadUserForLogin(plainCred);
			int userid=up.getId();
			return "/sapp/product/userLanding//main?userId="+userid;
			//return up;
	    } catch (Exception e) {
	    	e.printStackTrace();
	    	throw new BadCredentialsException("Username/Password does not match for " + username);
	    }
		
	}
	protected void clearAuthenticationAttributes(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session == null) {
				return;
		}
		session.removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
}
		
		public void setRedirectStrategy(RedirectStrategy redirectStrategy) {
		this.redirectStrategy = redirectStrategy;}
		protected RedirectStrategy getRedirectStrategy() {
		return redirectStrategy;}*/
	@Override
	/**
	 * To indicate that this authentication provider can handle the auth request.
	 * since there's currently only one way of logging in, always return true
	 */
	public boolean supports(Class<?> aClass) {
	    return true;
	}

	public UserRepository getUserRepository() {
		return userRepository;
	}

	public void setUserRepository(UserRepository userRepository) {
		this.userRepository = userRepository;
	}
}