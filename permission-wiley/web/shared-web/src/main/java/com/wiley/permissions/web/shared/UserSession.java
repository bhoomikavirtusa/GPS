package com.wiley.permissions.web.shared;

import java.util.Date;

import com.wiley.permissions.security.web.UserPrincipal;

/**
 *
 * @author ttidwell
 */
public class UserSession
{
	private UserPrincipal currentUser = null;
	private boolean containedMode = false;
	private boolean ssoSession = false;
	private boolean externalLogin = false;

	private String productSearchString = "";
	private String productSearchField = "ISBN";

	private boolean helpOn = false;


	public Date getCurrentDate() {
		return new Date();
	}

	public UserPrincipal getCurrentUser() {
		return currentUser;
	}

	public void setCurrentUser(UserPrincipal currentUser) {
		this.currentUser = currentUser;
	}

	public boolean isContainedMode() {
		return containedMode;
	}

	public void setContainedMode(boolean containedMode) {
		this.containedMode = containedMode;
	}

	public boolean isSSOSession() {
		return ssoSession;
	}

	public void setSSOSession(boolean ssoSession) {
		this.ssoSession = ssoSession;
	}

	public boolean isExternalLogin() {
		return externalLogin;
	}

	public void setExternalLogin(boolean b) {
		this.externalLogin = b;
	}

	public void setProductSearchString(String productSearchString) {
		this.productSearchString = productSearchString;
	}

	public String getProductSearchString() {
		return productSearchString;
	}

	public void setProductSearchField(String productSearchField) {
		this.productSearchField = productSearchField;
	}

	public String getProductSearchField() {
		return productSearchField;
	}

	public boolean isHelpOn() {
		return helpOn;
	}

	public void setHelpOn(boolean helpOn) {
		this.helpOn = helpOn;
	}
}
