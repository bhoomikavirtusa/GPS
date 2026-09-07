package com.wiley.permissions.web.shared.controllers.login;

import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author ttidwell
 */
public class LoginForm {

	private String email;
	private String password;

	public LoginForm() {
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = StringUtils.stripToEmpty(email);
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = StringUtils.stripToEmpty(password);
	}
}
