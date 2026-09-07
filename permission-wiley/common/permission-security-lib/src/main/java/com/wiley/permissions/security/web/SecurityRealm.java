package com.wiley.permissions.security.web;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import javax.naming.InitialContext;

import org.apache.catalina.realm.GenericPrincipal;
import org.apache.catalina.realm.RealmBase;

/**
 *
 * @author ttidwell
 */
public class SecurityRealm
extends RealmBase
{
	private String loginModuleJNDIName = null;
	private LoginModule loginModule = null;
	
	public SecurityRealm() {
		super();
	}

	public void initialize()
	throws SecurityException
	{
		try {
			InitialContext ic = new InitialContext();

			loginModule = (LoginModule) ic.lookup(loginModuleJNDIName);
		}
		catch (Exception e) {
			throw new SecurityException("Error Finding Login Module: " + loginModuleJNDIName, e);
		}
	}

	@Override
	protected String getName() {
		return this.getClass().getSimpleName();
	}

	@Override
	protected String getPassword(String arg0)
	throws SecurityException
	{
		if (loginModule == null) initialize();

		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	protected Principal getPrincipal(String arg0)
	throws SecurityException
	{
		if (loginModule == null) initialize();

		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Principal authenticate(String credential, String key)
	throws SecurityException
	{
		if (loginModule == null) initialize();

		try {
			String plainCred = CredentialEncryptionUtility.decrypt(credential, key);

			UserPrincipal userPrincipal = loginModule.findUserForLogin(plainCred);
			
			List<String> privileges = new ArrayList<String>();
			
			for (PrivilegePrincipal privilege : userPrincipal.getGlobalPrivileges()) {
				privileges.add(privilege.getCode());
			}
			
			return new GenericPrincipal(credential, key, privileges, userPrincipal);
		}
		catch (Exception e) {
			return null;
		}
	}

	public String getLoginModuleJNDIName() {
		return loginModuleJNDIName;
	}

	public void setLoginModuleJNDIName(String loginModuleJNDIName) {
		this.loginModuleJNDIName = loginModuleJNDIName;
	}

	public LoginModule getLoginModule()	{
		return loginModule;
	}

	public void setLoginModule(LoginModule loginModule) {
		this.loginModule = loginModule;
	}
}