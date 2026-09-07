package com.wiley.permissions.resources;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.lifecycle.InitialisationException;

import com.wiley.permissions.common.transformer.ObjectToJson;
import com.wiley.permissions.common.transformer.TransformationException;
import com.wiley.permissions.domain.persistence.permissions.User;
import com.wiley.permissions.persistence.PersistenceException;
import com.wiley.permissions.repositories.UserRepository;
import com.wiley.permissions.services.SecurityService;
import com.wiley.permissions.services.view.UserProfileView;

/**
 * This class is the REST service implementation
 * Uses the SecurityService to make the calls, but it also transforms the results
 * to XML or JSON
 * We need this because the version of MULE we have I tested it and it does not do the
 * JSON transformation
 * Maybe MULE 3.0 will handle the JSON transformation and then we can annotate the ProductServiceImpl
 * directly as a REST resource
 * Because of cross domain requests, we need JSONP (a callback method name is passed as QueryParam)
 * @author lnagy
 */
@Path ("/")
public class UserResource {

	private static final Log log = LogFactory.getLog(UserResource.class);

	private SecurityService securityService;
	private UserRepository userRepository;

	@GET
	@Produces("application/json")
	@Path("/profile/{id}")
	public String loadUserProfile (@PathParam("id") Integer id, @QueryParam("callback") String callback)
	throws InitialisationException, TransformationException
	{
		log.debug("loadUserProfile(): called with id = " + id);
		List<UserProfileView> product = securityService.loadProfile(id);
		return ObjectToJson.doTransform(product, callback);
	}


	@GET
	@Produces("application/json")
	@Path("/searchAuthor/{term}")
	public String searchAuthorView (@PathParam("term") String term, @QueryParam("callback") String callback)
			throws PersistenceException, IllegalAccessException, InvocationTargetException,
			NoSuchMethodException, InitialisationException, TransformationException
	{
		log.debug("searchAuthorView(): called with term = " + term);
		List<User> users = userRepository.searchAuthorsByLastName(term);
		return ObjectToJson.doTransform(users, callback);
	}


	public SecurityService getSecurityService() {
		return securityService;
	}

	public void setSecurityService(SecurityService securityService) {
		this.securityService = securityService;
	}

	public UserRepository getUserRepository() {
		return userRepository;
	}

	public void setUserRepository(UserRepository userRepository) {
		this.userRepository = userRepository;
	}
}
