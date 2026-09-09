package com.wiley.permissions.web.internal.controllers.myaccount;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.wiley.permissions.services.SSOUserLookupUtility;
import com.wiley.permissions.web.shared.controllers.login.BaseLoginController;

@Controller
@RequestMapping("")
public class LoginController extends BaseLoginController {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(LoginController.class);

	private SSOUserLookupUtility ssoUserLookupUtility;

	public void setSsoUserLookupUtility(SSOUserLookupUtility lookup) {
		ssoUserLookupUtility = lookup;
	}

	/** Implements abstract method from superclass. */
	@Override
	public SSOUserLookupUtility getSsoUserLookupUtility() {
		return ssoUserLookupUtility;
	}
}
