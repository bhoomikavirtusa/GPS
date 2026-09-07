package com.wiley.permissions.services;

import com.wiley.permissions.common.utils.UniqueConstraintViolationException;
import com.wiley.permissions.domain.persistence.permissions.User;
import com.wiley.permissions.persistence.PersistenceException;

public interface UserService {

	// add methods here as needed
	public User saveUser(User user, boolean sendMessage)
			throws PersistenceException, UniqueConstraintViolationException, Exception;
	
}
