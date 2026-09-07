package com.wiley.permissions.security.web;

/**
 *
 * @author ttidwell
 */
public interface LoginModule
{
	public abstract UserPrincipal findUserForLogin(String identifier) throws Exception;
}
