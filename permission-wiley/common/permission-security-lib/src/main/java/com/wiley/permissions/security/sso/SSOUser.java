package com.wiley.permissions.security.sso;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlElement;

/**
 *
 * @author ttidwell
 */
public class SSOUser implements Serializable
{
	private static final long serialVersionUID = 1L;

	// smarkoff - I don't think we ever use JAXB on this class, probably don't need XML annotations
	@XmlElement
	private String email;

	@XmlElement(name="firstname")
	private String firstName;

	@XmlElement(name="lastname")
	private String lastName;

	@XmlElement(name="identifier")
	private String ldapDN;

	@XmlElement(name="uid")
	private String userId;

	private String ldapGroupName;

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getLdapDN() {
		return ldapDN;
	}

	public void setLdapDN(String ldapDN) {
		this.ldapDN = ldapDN;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getLdapGroupName() {
		return ldapGroupName;
	}

	public void setLdapGroupName(String groupName) {
		this.ldapGroupName = groupName;
	}

	@Override
	public String toString() {
		return "email = " + email
			+ ", firstName = " + firstName
			+ ", lastName = " + lastName
			+ ", ldapDN = " + ldapDN
			+ ", userId = " + userId
			+ ", groupName = " + ldapGroupName;
	}
}
