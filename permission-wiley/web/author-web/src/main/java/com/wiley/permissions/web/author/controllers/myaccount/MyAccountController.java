package com.wiley.permissions.web.author.controllers.myaccount;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import com.wiley.permissions.security.web.UserPrincipal;
import com.wiley.permissions.services.ExternalUserLdapClient;
import com.wiley.permissions.services.SSOUserLookupUtility;
import com.wiley.permissions.web.shared.PermUserContext;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;

@Controller
@RequestMapping("/account")
public class MyAccountController extends BaseAnnotatedController {
	private static final Log log = LogFactory.getLog(MyAccountController.class);

	protected final static String CHANGE_PASSWORD_FORM_NAME = "changePasswordForm";

	private SSOUserLookupUtility ssoUserLookupUtility;
	private ExternalUserLdapClient externalUserLdapClient;


	@RequestMapping(value="/changePassword", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView changePasswordLoad(HttpServletRequest request) throws Exception {
		log.debug("changePasswordLoad(): entered...");

		ModelAndView mv = new ModelAndView("user.account.changePassword");  // changePassword.jspx
		ChangePasswordForm form = new ChangePasswordForm();
		mv.addObject(CHANGE_PASSWORD_FORM_NAME, form);

		return mv;
	}

	@RequestMapping(value="/changePassword", method = RequestMethod.POST)
	public ModelAndView changePasswordSubmit(HttpServletRequest request,
		@ModelAttribute(CHANGE_PASSWORD_FORM_NAME) ChangePasswordForm form, BindingResult bindingResult)
	throws Exception {
		log.debug("changePasswordSubmit(): entered...");

		// required fields validated by client-side JavaScript
		//getValidator().validate(form, bindingResult);

		// Extra custom validation:
		if (!form.getPassword().equals(form.getConfirmPassword())) {
			String msg = "Confirmation password does not match";
			bindingResult.reject(null, msg);
		}

		UserPrincipal userPrin = PermUserContext.getCurrentUser(request);

		boolean oldPasswordAuth = ssoUserLookupUtility.authenticateByEmail(userPrin.getEmail(), form.getOldPassword());
		log.debug("changePasswordSubmit(): oldPassword for [" + userPrin.getEmail() + "] authenticated: " + oldPasswordAuth);
		if (!oldPasswordAuth) {
			bindingResult.rejectValue("oldPassword", null, "Old Password is not correct.");
		}

		// maybe later
		//if (PasswordUtil.isPasswordStrong(form.getPassword())) {
		//	bindingResult.rejectValue("password", null, "Password is not strong");
		//}

		if (!bindingResult.hasErrors()) {
			try {
				log.debug("changePasswordSubmit(): calling externalUserLdapClient.setPassword()...");
				externalUserLdapClient.setPassword(userPrin.getEmail(), form.getPassword());
			}
			catch (Exception ex) {
				log.error("changePasswordSubmit(): Caught Exception calling setPassword: ", ex);
				bindingResult.reject(null, "Caught Exception: " + ex);
			}
		}

		// If the password has been successfully changed, the JSP will just close the dialog
		ModelAndView mv = new ModelAndView("user.account.changePassword");  // changePassword.jspx
		mv.addObject("onSubmit", true);
		mv.addObject("hasErrors", bindingResult.hasErrors());

		return mv;
	}

	public void setSsoUserLookupUtility(SSOUserLookupUtility ssoUserLookupUtility) {
		this.ssoUserLookupUtility = ssoUserLookupUtility;
	}

	public void setExternalUserLdapClient(ExternalUserLdapClient ldapClient) {
		this.externalUserLdapClient = ldapClient;
	}
}
