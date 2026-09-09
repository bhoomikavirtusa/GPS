package com.wiley.permissions.web.author.controllers.myaccount;

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

import com.wiley.permissions.domain.persistence.permissions.User;
import com.wiley.permissions.repositories.UserRepository;
import com.wiley.permissions.services.CommonWorkService;
import com.wiley.permissions.services.ExternalUserLdapClient;
import com.wiley.permissions.services.SSOUserLookupUtility;
import com.wiley.permissions.web.shared.controllers.login.BaseLoginController;
import com.wiley.permissions.web.shared.controllers.login.LoginForm;

@Controller
@RequestMapping("")
public class LoginController extends BaseLoginController {
	private static final Log log = LogFactory.getLog(LoginController.class);

	private UserRepository userRepository;
	private SSOUserLookupUtility ssoUserLookupUtility;
	private ExternalUserLdapClient externalUserLdapClient;
	private CommonWorkService commonWorkService;


	@RequestMapping(value = "/security/login/forgotPassword", method = RequestMethod.POST)
	public ModelAndView forgotPassword(HttpServletRequest request, @ModelAttribute(FORM_MODEL_NAME) LoginForm form,
			BindingResult bindingResult) throws Exception
	{
		log.debug("forgotPassword(): entered...");

		if (StringUtils.isBlank(form.getEmail())) {
			String msg = "You must fill in your email";
			bindingResult.reject(null, msg);
		}
		else {
			User user = userRepository.loadByEmail(form.getEmail());
			if (user == null) {
				String msg = "Sorry, that email address is not registered in our system";
				bindingResult.reject(null, msg);
				// delay so a brute force attempt to discover email addresses in the system will be slowed down
				try { Thread.sleep(1000); } catch (InterruptedException ex) { }
			}
			else {
				String newPassword = externalUserLdapClient.setPassword(user.getEmail(), null);
				commonWorkService.sendEmailAuthorTempPassword(user.getEmail(), newPassword);
				log.debug("generateNewPassword(): generated new password [" + newPassword + "] for user lastName ["
					+ user.getLastName() + "] email [" + user.getEmail() + "]");

				String msg = "You have been sent a new password - check your email and then enter the password here";
				bindingResult.reject(null, msg);

				form.setPassword("");
			}
		}

		ModelAndView mv = new ModelAndView(getFormView());
		mv.addObject(FORM_MODEL_NAME, form);
		return mv;
	}

	public void setUserRepository(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public void setSsoUserLookupUtility(SSOUserLookupUtility lookup) {
		ssoUserLookupUtility = lookup;
	}

	public void setExternalUserLdapClient(ExternalUserLdapClient client) {
		this.externalUserLdapClient = client;
	}

	public void setCommonWorkService(CommonWorkService service) {
		this.commonWorkService = service;
	}

	/** Implements abstract method from superclass. */
	@Override
	public SSOUserLookupUtility getSsoUserLookupUtility() {
		return ssoUserLookupUtility;
	}
}
