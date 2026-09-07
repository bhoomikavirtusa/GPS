package com.wiley.permissions.security.sso;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author ttidwell
 */
@XmlRootElement(name="users")
public class SSOUserMessage
implements Serializable
{
	private static final long serialVersionUID = 1L;

	@XmlElement(name="user")
	private List<SSOUser> users = new ArrayList<SSOUser>();

	public List<SSOUser> getUsers()
	{
		return users;
	}

	public void setUsers(List<SSOUser> users)
	{
		this.users = users;
	}

}
