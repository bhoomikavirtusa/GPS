package com.wiley.permissions.web.shared.controllers.myaccount;

import java.util.List;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.wiley.permissions.domain.persistence.permissions.User;
import com.wiley.permissions.persistence.PersistenceException;
import com.wiley.permissions.repositories.UserRepository;
import com.wiley.permissions.services.SecurityService;
import com.wiley.permissions.services.util.ServiceException;
import com.wiley.permissions.services.view.UserProfileView;
import com.wiley.permissions.web.shared.PermUserContext;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;

/**
 * Manages the user profile
 *
 * @author lnagy
 */
@RequestMapping("/user/profile")
@Controller
public class UserProfileController extends BaseAnnotatedController {

	// --------------------- static data ---------------------------------

	private final static Log log = LogFactory.getLog(UserProfileController.class);

	// --------------------- instance data -------------------------------

	private SecurityService securityService;

	private UserRepository userRepository;

	/**
	 * Load
	 * @param request
	 * @param userId
	 * @return ModelAndView
	 * @throws Exception
	 */
	@RequestMapping(method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView load(HttpServletRequest request,
			HttpServletResponse response,
			@RequestParam(value = "userId", required = false) Integer userId
			) throws Exception
	{
		log.debug("load(): entered..., userId = " + userId);
		if (null == userId)
			userId = PermUserContext.getCurrentUserId(request);

		validate(userId);

		ModelAndView mv = new ModelAndView(getFormView());
		List<UserProfileView> profile = securityService.loadProfile(userId);
		mv.addObject("profile", profile);
		mv.addObject("userId", userId);
		return mv;
	}

	/**
	 * Save
	 * @param userId
	 * @param fieldList
	 * @return ModelAndView
	 * @throws Exception
	 */
	@RequestMapping(method = RequestMethod.POST)
	public ModelAndView submit(HttpServletRequest request, HttpServletResponse response, @RequestParam(value = "userId") int userId,
			@RequestParam(value="field", required=false) List<String> fieldList,
			@RequestParam(value="sortup", required=false) Integer upSeq,
			@RequestParam(value="sortdown", required=false) Integer downSeq
			) throws Exception
	{
		log.debug("submit(): entered..., userId = " + userId);

		if (null != upSeq) {
			log.debug("submit(): upsort for " + upSeq);
		}
		if (null != downSeq) {
			log.debug("submit(): down sort for " + downSeq);
		}
		validate(userId);
		// first load the profile from DB
		List<UserProfileView> profile = securityService.loadProfile(userId);
		// merge the profiles
		for (UserProfileView field : profile) {
			if (!field.isRequired()) {
				if (CollectionUtils.isNotEmpty(fieldList) && fieldList.contains(field.getName())) {
					field.setVisible(true);
				} else {
					field.setVisible(false);
				}
			}
			log.debug("submit(): sort order: " + field.getSortOrder() + " for field: " + field.getTitle());
		}
		if (null != upSeq) {
			Integer idx = upSeq -1;
			profile.get(idx).setSortOrder(upSeq - 1);
			profile.get(idx -1).setSortOrder(upSeq);
		}
		if (null != downSeq) {
			Integer idx = downSeq -1;
			profile.get(idx).setSortOrder(downSeq + 1);
			profile.get(idx + 1).setSortOrder(downSeq);
		}

		securityService.saveProfile(userId, profile);
		// clear the cookies so the table can include the new columns
		deleteCookie(request, response, "SpryMedia_DataTables_assetTable_main");
		deleteCookie(request, response, "SpryMedia_DataTables_assetTable_scroll");
		deleteCookie(request, response, "wileySearch_assetTable");

		String msg = getMessageSource().getMessage("profile.save.success", null, null);
		// VERY IMPORTANT: do not redirect - loses the response and the cookie is not deleted
		// return load (request, response, userId);
		ModelAndView mv = new ModelAndView("pages.success");
		mv.addObject("message", msg);
		mv.addObject("redirectUrl", getSuccessView() + "?userId=" + userId);
		if (null != upSeq || null != downSeq) {
			mv = new ModelAndView("redirect:/sapp/cwlanding/user/profile?userId="+ userId);
		}
		return mv;
	}

	/**
	 * If userId = 1 (PERMISSIONS_USER) we do not allow editing
	 * Also we check if the user exists
	 *
	 * @param userId
	 * @throws ServiceException
	 * @throws PersistenceException
	 */
	private void validate(int userId) throws ServiceException, PersistenceException
	{
		if (userId == UserRepository.MASTER_USER_ID) {
			throw new ServiceException("Permissions user profile cannot be edited.", true);
		}

		User u = userRepository.loadUserById(userId);
		if (null == u)
			throw new ServiceException("User not found.", true);
	}

	/**
	 * Deletes a cookie from request
	 * @param request
	 * @param response
	 * @param cookieName
	 */
	public static void deleteCookie (HttpServletRequest request, HttpServletResponse response, String cookieName) {
		Cookie[] cookies = request.getCookies();
		for (int i = 0; i < cookies.length; i++) {
			Cookie cookie = cookies[i];
			log.debug("----COOKIE NAME:  " + cookie.getName() + ": " + cookie.getValue());
			if (cookieName.equals(cookie.getName())) {
				log.debug("cookie " + cookieName + " found");
				cookie.setMaxAge(0);
				cookie.setValue (null);
				cookie.setPath(request.getContextPath() + "/sapp/cwlanding/");
				response.addCookie(cookie);
			}
		}
	}


	// --------------------- getters and setters -------------------------

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
