package com.wiley.permissions.services.view;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.permissions.domain.persistence.permissions.User;

/**
 * POJO class to store the results of UserRepository.loadAllUserSubsetList().
 *
 * @author smarkoff
 */
public class UserSubsetView {

	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(UserSubsetView.class);

	private final Integer id;
	private final User.Type type;
	private final boolean enabled;
	private final String email;
	private final String firstName;
	private final String lastName;


	public UserSubsetView(int id, User.Type type, boolean enabled, String email, String firstName, String lastName) {
		this.id = id;
		this.type = type;
		this.enabled = enabled;
		this.email = email;
		this.firstName = firstName;
		this.lastName = lastName;
	}

	public Integer getId() {
		return id;
	}

	public User.Type getType() {
		return type;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public String getEmail() {
		return email;
	}

	public String getFirstName() {
		return firstName;
	}

	public String getLastName() {
		return lastName;
	}


	@Override
	public String toString() {
		return "id = " + id + ", type = " + type + ",enabled = " + isEnabled()
			+ ", firstName = " + getFirstName() + ", lastName = " + getLastName()
			+ ",email = " + getEmail();
	}
}
