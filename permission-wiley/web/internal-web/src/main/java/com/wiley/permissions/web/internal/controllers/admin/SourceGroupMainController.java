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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.wiley.permissions.domain.persistence.permissions.SourceGroup;
import com.wiley.permissions.repositories.SourceRepository;
import com.wiley.permissions.repositories.UserRepository;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;
import com.wiley.permissions.web.shared.controllers.BaseFormBean.FormMode;

/**
 *
 * @author ttidwell
 */
@Controller
public class SourceGroupMainController
extends BaseAnnotatedController
{
	private final static Log log = LogFactory.getLog(SourceGroupMainController.class);

	private final static String FORM_MODEL_NAME = "sourceGroupMainForm";

	private String manageSourceGroupURL = null;
	private UserRepository userRepository = null;
	private SourceRepository sourceRepository = null;


	@RequestMapping("/admin/sourceGroups")
	@SuppressWarnings("incomplete-switch")
	public ModelAndView handle(HttpServletRequest request,
			@RequestParam(value = "cwId", required = false) Integer cwId,
			@ModelAttribute(FORM_MODEL_NAME) SourceGroupMainForm form,
			BindingResult bindingResult)
	throws Exception
	{
		log.debug("handle(): entered...");

		ModelAndView mv = new ModelAndView(getSuccessView());
		// When does this make a difference (test case)?
		// think can take next line out (smarkoff)
		mv.addAllObjects(bindingResult.getModel());
		
		if (null != cwId) {
			mv.addObject("cwId", cwId);
		}

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
				List<SourceGroup> groups = new ArrayList<SourceGroup>();
				List<SourceGroup> activeGroups = new ArrayList<SourceGroup>();	
				List<SourceGroup> noFlyGroups = new ArrayList<SourceGroup>();
				
				try {
					groups = getSourceRepository().loadSourceGroupList();
					for (SourceGroup sg : groups) {
						if (sourceRepository.hasInUseRoyaltyFreeDeals(sg) || sourceRepository.hasInUseMasterAgreements(sg)) {
							sg.setIsBeingUsed(true);
						}
						if (sg.isNofly()) {
							noFlyGroups.add(sg);
						} else {
							activeGroups.add(sg);
						}
					}
				}
				catch (Exception e) {
					log.error("Error Searching", e);
				}

				mv.addObject("groups", activeGroups);
				mv.addObject("noflygroups", noFlyGroups);

				break;
			}

			case ADD_CHILD:
				redirectURL = manageSourceGroupURL;
				newMode = FormMode.ADD;
				mv.addObject("fresh", "true");
				break;

			case MODIFY_CHILD:
				redirectURL = manageSourceGroupURL;
				newMode = FormMode.MODIFY;
				break;

			case REMOVE_CHILD:
				redirectURL = manageSourceGroupURL;
				newMode = FormMode.REMOVE;
				break;
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

	public String getManageSourceGroupURL() {
		return manageSourceGroupURL;
	}

	public void setManageSourceGroupURL(String manageSourceGroupURL) {
		this.manageSourceGroupURL = manageSourceGroupURL;
	}

	public UserRepository getUserRepository() {
		return userRepository;
	}

	public void setUserRepository(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public SourceRepository getSourceRepository() {
		return sourceRepository;
	}

	public void setSourceRepository(SourceRepository sourceRepository) {
		this.sourceRepository = sourceRepository;
	}
}
