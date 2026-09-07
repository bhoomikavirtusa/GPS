package com.wiley.permissions.web.shared.controllers.login;

import java.net.URLEncoder;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.security.core.context.SecurityContextHolder;

import com.wiley.permissions.domain.persistence.permissions.User;
import com.wiley.permissions.security.web.AuthenticationException;
import com.wiley.permissions.security.web.AuthenticationException.Type;
import com.wiley.permissions.security.web.CredentialEncryptionUtility;
import com.wiley.permissions.services.SSOUserLookupUtility;
import com.wiley.permissions.services.SecurityService;
import com.wiley.permissions.web.shared.PermUserContext;
import com.wiley.permissions.web.shared.UserSession;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;
import com.wiley.sf.common.lang.StringUtil;
import com.wiley.sf.common.servlet.ServletUtil;

@Controller
@RequestMapping("")
public abstract class BaseLoginController extends BaseAnnotatedController {
	private static final Log log = LogFactory.getLog(BaseLoginController.class);

	private static final String SECURITY_LOGIN_URL = "/j_spring_security_check"; // "j_security_check";
	protected static final String FORM_MODEL_NAME = "loginForm";

	private SecurityService securityService;


	@RequestMapping(value = "/security/login/load", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView load(HttpServletRequest request) throws Exception {
		log.debug("load(): entered...");

		ModelAndView mv = checkForSSOHeaders(request);
		log.debug("load(): ModelAndView returned from checkForSSOHeaders(): " + mv);

		if (null != mv) return mv;

		LoginForm form = new LoginForm();

		mv = new ModelAndView(getFormView());
		mv.addObject(FORM_MODEL_NAME, form);

		return mv;
	}

	@RequestMapping(value = "/security/login/submit", method = RequestMethod.POST)
	public ModelAndView submit(HttpServletRequest request, @ModelAttribute(FORM_MODEL_NAME) LoginForm form,
			BindingResult bindingResult) throws Exception
	{
		log.debug("submit(): entered...");
		 Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		 log.debug("principal: entered..."+principal.toString());
		final String email = form.getEmail();
		final String password = form.getPassword();
		ModelAndView mv = null;
		try {
			User user = securityService.loginUser(email, password, getSsoUserLookupUtility());

			// assume I need to associate to session just like below in checkForSSOHeaders()
			UserSession userSession = PermUserContext.getUserSession(request);
			userSession.setSSOSession(true);
			PermUserContext.setUserSession(userSession, request);
			//mv= new ModelAndView("pages.landing.main");
			mv = forwardToRealm(email);
		}
		catch (AuthenticationException ae) {
			log.error("submit(): Could Not Authenticate/Create User For: " + email, ae);
			if (ae.getType() == Type.DISABLED) {
				bindingResult.reject("login.errors.disabled");
			}
			else {
				bindingResult.reject("login.errors.other");
			}
			mv = new ModelAndView(getFormView());
		}
		catch (Exception e) {
			log.error("submit(): Could Not Authenticate/Create User For: " + email, e);
			bindingResult.reject("login.errors.other");
			mv = new ModelAndView(getFormView());
		}
		log.debug("Returning model view "+mv.getViewName());
		return mv;
	}

	private ModelAndView checkForSSOHeaders(HttpServletRequest request) throws Exception {
		log.debug("checkForSSOHeaders(): entered...");

		log.info("checkForSSOHeaders(): allHeaders: " + ServletUtil.getAllHeaders(request));

		ModelAndView mv = null;
		String ssoDnHeader = request.getHeader("user_dn");  // this API is case-INsensitive
		String ssoMailHeader = request.getHeader("user_mail");

		if (ssoDnHeader != null) {
			log.info("checkForSSOHeaders(): SSO user_dn header found.  Going to look up user, etc.");

			if (StringUtils.isBlank(ssoMailHeader)) {  // not expected but check anyway
				log.error("checkForSSOHeaders(): user_dn header [" + ssoDnHeader
					+ "] was found but user_mail header missing or blank");
				return mv;
			}

			ssoDnHeader = StringUtil.normalizeLdapDN(ssoDnHeader);

			try {
				User user = securityService.checkCreateSSOUser(ssoMailHeader, ssoDnHeader, getSsoUserLookupUtility());

				if (user != null) {
					UserSession us = PermUserContext.getUserSession(request);
					us.setSSOSession(true);
					String userIP = request.getHeader("user_ip");  // this API is case-INsensitive
					// note if user did not use SSO then they must have logged in internally
					if (StringUtils.isBlank(userIP)) {
						log.error("checkForSSOHeaders(): user_ip header not found -- needed to determine whether user is external");
					}
					else {
						us.setExternalLogin(!userIP.startsWith("10."));
					}
					PermUserContext.setUserSession(us, request);

					mv = forwardToRealm(user.getEmail());
				}
			}
			catch (Exception e) {
				log.error("checkForSSOHeaders(): Caught unexpected exception, user_dn = " + ssoDnHeader, e);
			}
		}

		return mv;
	}

	private ModelAndView forwardToRealm(String credential) throws Exception {
		String redirectURL = "redirect:" + SECURITY_LOGIN_URL;
		String key = CredentialEncryptionUtility.getUniqueKey();
		credential = CredentialEncryptionUtility.encrypt(credential, key);
		redirectURL += "?j_username=" + URLEncoder.encode(credential, "US-ASCII");
		redirectURL += "&j_password=" + URLEncoder.encode(key, "US-ASCII");
		log.debug("forwardToRealm(): redirectURL = " + redirectURL);
		ModelAndView output = new ModelAndView();
		output.setViewName(redirectURL);
		log.debug("Model & View Object"+output.getViewName()+" ");
		return output;
	}

	public SecurityService getSecurityService() {
		return securityService;
	}

	public void setSecurityService(SecurityService securityService) {
		this.securityService = securityService;
	}

	// super class will implement to return either Internal or External (Author) implementation
	public abstract SSOUserLookupUtility getSsoUserLookupUtility();
}
