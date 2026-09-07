package com.wiley.permissions.web.internal.controllers.admin;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import com.wiley.permissions.domain.persistence.permissions.UserGroup;
import com.wiley.permissions.repositories.UserRepository;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;
import com.wiley.permissions.web.shared.controllers.BaseFormBean.FormMode;

/**
 *
 * @author ttidwell
 */
@Controller
public class UserGroupMainController
extends BaseAnnotatedController
{
	private final static Log log = LogFactory.getLog(UserGroupMainController.class);

	private final static String FORM_MODEL_NAME = "groupMainForm";

	private String manageUserGroupURL = null;
	private UserRepository userRepository = null;


	@RequestMapping(value="/admin/userGroups" , method = {RequestMethod.GET,RequestMethod.POST})
	@SuppressWarnings("incomplete-switch")
	public ModelAndView handle(HttpServletRequest request,
			@ModelAttribute(FORM_MODEL_NAME) UserGroupMainForm form,
			BindingResult bindingResult)
	throws Exception
	{
		log.debug("handle(): entered...");

		ModelAndView mv = new ModelAndView(getSuccessView());
		// When does this make a difference (test case)?
		// think can take next line out (smarkoff)
		mv.addAllObjects(bindingResult.getModel());

		String redirectURL = null;

		FormMode newMode = FormMode.ADD;
		FormMode mode = form.getMode();

		if (mode == null) {
			mode = FormMode.SEARCH;
		}

		log.info("handle(): mode: " + mode);

		switch (mode) {
			case VIEW:
			case SEARCH: {
				List<UserGroup> groups = new ArrayList<UserGroup>();

				try {
					groups = getUserRepository().loadAll(UserGroup.class);
				}
				catch (Exception e) {
					log.error("Error Searching", e);
				}

				mv.addObject("groups", groups);

				break;
			}

			case ADD_CHILD: {
				redirectURL = manageUserGroupURL;
				newMode = FormMode.ADD;
				mv.addObject("fresh", "true");
				break;
			}

			case MODIFY_CHILD: {
				redirectURL = manageUserGroupURL;
				newMode = FormMode.MODIFY;
				break;
			}

			case REMOVE_CHILD: {
				redirectURL = manageUserGroupURL;
				newMode = FormMode.REMOVE;
				break;
			}
		}

		if (redirectURL != null) {
			if (form.getGroupId() != null && form.getGroupId() > 0) {
				mv.addObject("groupId", form.getGroupId());
			}

			mv.addObject("mode", newMode);
			mv.setViewName("forward:" + redirectURL);
			form.setMode(FormMode.SEARCH);
		}

		log.debug("handle(): Going to view: " + mv.getViewName());

		return mv;
	}

	public String getManageUserGroupURL() {
		return manageUserGroupURL;
	}

	public void setManageUserGroupURL(String manageUserGroupURL) {
		this.manageUserGroupURL = manageUserGroupURL;
	}

	public UserRepository getUserRepository() {
		return userRepository;
	}

	public void setUserRepository(UserRepository userRepository) {
		this.userRepository = userRepository;
	}
}
