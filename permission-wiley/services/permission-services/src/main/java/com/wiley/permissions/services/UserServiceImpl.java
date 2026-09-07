package com.wiley.permissions.services;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.permissions.common.utils.UniqueConstraintViolationException;
import com.wiley.permissions.domain.persistence.permissions.User;
import com.wiley.permissions.persistence.PersistenceException;
import com.wiley.permissions.repositories.UserRepository;
import com.wiley.permissions.services.message.CMSMessageService;

public class UserServiceImpl extends BaseService implements UserService {

	@SuppressWarnings("unused")
	private final static Log log = LogFactory.getLog(UserServiceImpl.class);

	private UserRepository userRepository;
	private CMSMessageService outgoingMessageService;

	// add methods as needed



	public UserRepository getUserRepository() {
		return userRepository;
	}

	public void setUserRepository(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	/**
	 * Note while this method has no transaction annotation, it calls another method
	 * which is REQUIRES_NEW, so this method behaves similar to being REQUIRES_NEW.
	 */
	@Override
	public User saveUser(User user, boolean sendMessage)
			throws PersistenceException, UniqueConstraintViolationException, Exception
	{
		//boolean isNew = StringUtils.isBlank(user.getExternalId());

		user = userRepository.saveUser(user);  // REQUIRES_NEW

		//if (isNew) {
			// reload the source to get the externalId created from the trigger
			user = userRepository.loadUserById (user.getId());
	//	}

	/*	if (sendMessage) {
			try {
				getOutgoingMessageService().sendUpdateUserMessage(user, null);
			}
			catch (Exception ex) {
				log.error("Exception caught trying to send UpdateSource message: ", ex);
			}
		}
	*/
		return user;
	}

		
	public CMSMessageService getOutgoingMessageService() {
		return outgoingMessageService;
	}

	public void setOutgoingMessageService(CMSMessageService outgoingMessageService) {
		this.outgoingMessageService = outgoingMessageService;
	}
	
	
}
