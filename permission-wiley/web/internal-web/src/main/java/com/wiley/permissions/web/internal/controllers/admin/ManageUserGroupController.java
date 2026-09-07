package com.wiley.permissions.web.internal.controllers.admin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.ModelAndView;

import com.wiley.permissions.domain.persistence.permissions.User;
import com.wiley.permissions.domain.persistence.permissions.UserGroup;
import com.wiley.permissions.persistence.PersistenceException;
import com.wiley.permissions.repositories.UserRepository;
import com.wiley.permissions.services.SecurityService;
import com.wiley.permissions.services.view.UserSubsetView;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;
import com.wiley.permissions.web.shared.controllers.BaseFormBean.FormMode;

/**
 *
 * @author ttidwell
 */
@Controller
/*@RequestMapping("/admin/manageUserGroup")*/
@RequestMapping
@SessionAttributes(ManageUserGroupController.FORM_NAME)
public class ManageUserGroupController
extends BaseAnnotatedController
{
	private final static Log log = LogFactory.getLog(ManageUserGroupController.class);

	// when compile on command line requires this not to be private or won't compile
	// (since used above class definition)
	protected final static String FORM_NAME = "manageUserGroupForm";

	private SecurityService securityService;
	private UserRepository userRepository;

	private String manageView;
	private String findUserView;
	private String finishView;


	@Override
	protected List<String> getReferenceDataNames(HttpServletRequest request)
	throws Exception
	{
		List<String> output = new ArrayList<String>();
		output.add("privileges");
		return output;
	}

	@RequestMapping(value="/admin/manageUserGroup/form", method = {RequestMethod.GET, RequestMethod.POST})
	@SuppressWarnings("incomplete-switch")
	public ModelAndView formBackingObject(HttpServletRequest request)
	throws PersistenceException
	{
		log.debug("formBackingObject(): entered...");

		ManageUserGroupForm form = new ManageUserGroupForm();

		Object test = request.getAttribute("groupId");

		UserGroup group = null;
		Integer groupId = null;

		if (test != null) {
			groupId = (Integer) test;

			test = request.getAttribute("mode");

			if (test != null) {
				form.setMode((FormMode) test);
			}
			else {
				form.setMode(FormMode.ADD);
			}
		}
		else {
			form.setMode(FormMode.ADD);
		}

		switch (form.getMode()) {
			case ADD:
				group = new UserGroup();
				break;

			case VIEW:
			case MODIFY:
			case REMOVE:
				group = securityService.loadUserGroupByIdForManageUserGroup(groupId);
		}

		form.setGroup(group);
		form.setOriginalMode(form.getMode());

		ModelAndView mv = new ModelAndView(getFormView(), FORM_NAME, form);
		return mv;
	}

	@RequestMapping(value="/admin/manageUserGroup/manage", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView manage(HttpServletRequest request,
			@ModelAttribute(FORM_NAME) ManageUserGroupForm form,
			BindingResult bindingResult) {
		log.debug("manage(): selectedChild = " + form.getSelectedChild());

		UserGroup group = form.getGroup();

		if (form.getSelectedChild() != null) {
			int selectedChild = form.getSelectedChild();

			if (form.getMode() == FormMode.REMOVE_CHILD) {
				User user = getUserWithIdFromCollection(selectedChild, group.getUsers());
				if (user != null) {
					user.setGroup(null);
					group.getUsers().remove(user);
					form.getRemovedUsers().add(user.getId());
				}
				form.setSelectedChild(null);
				form.setSelectedUser(null);
			}
		}
		else { // smarkoff: I'm not sure this is ever executed now that switched to annotated - findUser below is used
			if (form.getMode() == FormMode.ADD_CHILD) {
				doSearch(request);
			}
		}

		form.setSelectedUser(null);

		ModelAndView mv = new ModelAndView(getManageView());
		return mv;
	}

	@RequestMapping(value="/admin/manageUserGroup/findUser", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView findUser(HttpServletRequest request,
			@ModelAttribute(FORM_NAME) ManageUserGroupForm form,
			BindingResult bindingResult) throws PersistenceException {

		log.debug("findUser(): selectedUser = " + form.getSelectedUser());
		ModelAndView mv = new ModelAndView(getFormView());
		UserGroup group = form.getGroup();

		if (form.getSelectedUser() == null) {
			// submit param may be null so put "back" on left
			if ("back".equalsIgnoreCase(request.getParameter("submit"))) {
				reset(form);
			}
			else {
				doSearch(request);
				mv.setViewName(getFindUserView());
			}
		}
		else {
			User user = getUserRepository().loadUserById(form.getSelectedUser());

			if (user != null) {
				group.getUsersNotNull().add(user);
			}
		}

		form.setSelectedUser(null);

		return mv;
	}

	private User getUserWithIdFromCollection(Integer userId, Collection<User> users) {
		for (User u : users) {
			if (u.getId().equals(userId)) {
				return u;
			}
		}

		return null;
	}

	@RequestMapping(value="/admin/manageUserGroup/finish", method = {RequestMethod.GET, RequestMethod.POST})
	@SuppressWarnings("incomplete-switch")
	protected ModelAndView finish(HttpServletRequest request,
			@ModelAttribute(FORM_NAME) ManageUserGroupForm form, BindingResult bindingResult)
	throws Exception
	{
		ModelAndView mv = new ModelAndView(getFinishView());
		UserGroup group = form.getGroup();

		log.debug("finish(): mode = " + form.getMode());
		
		switch (form.getMode()) {
			case ADD:
			case MODIFY:
				try {
					group = securityService.saveUserGroup(group, form.getRemovedUsers());
					form.setGroup(group);				
				} catch (Exception ca) {
					log.debug("finish(): Persist failed", ca);
					Throwable cause = ca.getCause();
					if (cause instanceof ConstraintViolationException) {
						// most likely the exception is because same name exists, so we handle this
						bindingResult.rejectValue("group.name", null, "Duplicate group name.");
					}
				}
				break;

			case REMOVE:
				securityService.deleteUserGroupById(group.getId());
				break;
		}

		getReferenceDataCache().invalidate(UserGroup.class);

		if (bindingResult.hasErrors()) {
			mv.setViewName(getManageView());
		}

		log.debug("finish(): Going to view: " + mv.getViewName());

		return mv;
	}

	private void reset(ManageUserGroupForm form) {
		form.setMode(form.getOriginalMode());
		form.setSelectedChild(null);
	}

	private void doSearch(HttpServletRequest request) {
		List<UserSubsetView> users = userRepository.loadAllUserSubsetList();
		request.setAttribute("users", users);
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

	public String getManageView() {
		return manageView;
	}

	public void setManageView(String manageView) {
		this.manageView = manageView;
	}

	public String getFindUserView() {
		return findUserView;
	}

	public void setFindUserView(String findUserView) {
		this.findUserView = findUserView;
	}

	public String getFinishView() {
		return finishView;
	}

	public void setFinishView(String finishView) {
		this.finishView = finishView;
	}
}
