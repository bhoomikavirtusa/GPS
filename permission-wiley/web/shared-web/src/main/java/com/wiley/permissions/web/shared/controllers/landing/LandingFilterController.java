package com.wiley.permissions.web.shared.controllers.landing;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.wiley.permissions.domain.persistence.permissions.Component;
import com.wiley.permissions.domain.persistence.permissions.MediaType;
import com.wiley.permissions.domain.persistence.permissions.OwnerType;
import com.wiley.permissions.domain.persistence.permissions.PermissionStatus;
import com.wiley.permissions.domain.persistence.permissions.Privilege;
import com.wiley.permissions.domain.persistence.permissions.Role;
import com.wiley.permissions.domain.persistence.permissions.Source;
import com.wiley.permissions.domain.persistence.permissions.Usage;
import com.wiley.permissions.domain.persistence.permissions.User;
import com.wiley.permissions.domain.persistence.permissions.UserGroup;
import com.wiley.permissions.repositories.CommonWorkRepository;
import com.wiley.permissions.services.LandingFilterForm;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;

/**
 *
 * @author smarkoff
 */
/*@RequestMapping("/landing/filter")*/
@RequestMapping
public class LandingFilterController extends BaseAnnotatedController {

	// --------------------- static data ---------------------------------
	private final static Log log = LogFactory.getLog(LandingFilterController.class);

	protected final static String MODEL_FORM_NAME = "landingFilterForm";

	// --------------------- instance data -------------------------------

	private CommonWorkRepository cwRepository;


	@RequestMapping(value="/landing/filter/form", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView formBackingObject(HttpServletRequest request,
			@RequestParam(value = "cwId") int cwId)
	{
		log.debug("formBackingObject(): entered... cwId = " + cwId);

		ModelAndView mv = new ModelAndView(getFormView());

		boolean includeCovers = request.isUserInRole(Role.EMPLOYEE_DEFAULT.getCode())
			|| request.isUserInRole(Privilege.COVER_ASSETS.getCode());
		boolean adminUser = request.isUserInRole(Role.ADMIN.getCode());
		List<Component> componentList = cwRepository.loadComponentList(cwId, includeCovers);
		mv.addObject("componentList", componentList);
		mv.addObject("mediaTypeList", MediaType.ALL_MEDIA_TYPES);
		mv.addObject("usageList", Usage.ALL_USAGES);
		mv.addObject("ownerTypeList", OwnerType.ALL_OWNER_TYPES);
		List<Source> sourceList = cwRepository.loadSourcesForCw(cwId);
		mv.addObject("sourceList", sourceList);
		List<User> userList = cwRepository.getCreatedUsersForCWAssetUses(cwId);
		mv.addObject("userList", userList);
		List<UserGroup> groupList = cwRepository.getCreatedUserGroupsForCWAssetUses(cwId, adminUser);
		mv.addObject("groupList", groupList);
		mv.addObject("statusList", PermissionStatus.ALL_PERMISSION_STATUS_ARRAY);

		LandingFilterForm form = (LandingFilterForm) request.getSession().getAttribute(MODEL_FORM_NAME + cwId);
		if (form == null)  form = new LandingFilterForm();
		mv.addObject(MODEL_FORM_NAME, form);

		return mv;
	}

	/*@GetMapping("/submit")*/
	@RequestMapping(value="/landing/filter/submit", method = RequestMethod.POST)
	public String onSubmit(HttpServletRequest request,
		@RequestParam(value = "cwId") int cwId,
		@ModelAttribute(MODEL_FORM_NAME) LandingFilterForm form, BindingResult bindingResult)
	{
		log.debug("onSubmit(): entered... cwId = " + cwId);

		form.setUseFilter(true);
		request.getSession().setAttribute(MODEL_FORM_NAME + cwId, form);
		return getSuccessView() + "?cwId=" + cwId;
	}

	/*@GetMapping("/toggle")*/
	@RequestMapping(value="/landing/filter/toggle", method = {RequestMethod.GET, RequestMethod.POST})
	public String toggle(HttpServletRequest request,
		@RequestParam(value = "cwId") int cwId,
		@RequestParam(value = "useFilter") boolean useFilter)
	{
		log.debug("toggle(): entered... cwId = " + cwId + ", useFilter = " + useFilter);

		LandingFilterForm form = (LandingFilterForm) request.getSession().getAttribute(MODEL_FORM_NAME + cwId);
		if (form != null) {
			form.setUseFilter(useFilter);
		}
		return getSuccessView() + "?cwId=" + cwId;
	}


	public void setCommonWorkRepository(CommonWorkRepository cwRepository) {
		this.cwRepository = cwRepository;
	}

	public CommonWorkRepository getCommonWorkRepository() {
		return cwRepository;
	}
}
