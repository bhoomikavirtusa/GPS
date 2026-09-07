package com.wiley.permissions.web.internal.controllers.myaccount;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.ModelAndView;

import com.wiley.permissions.domain.persistence.permissions.FavoriteGroup;
import com.wiley.permissions.domain.persistence.permissions.PurchaseOrder;
import com.wiley.permissions.domain.persistence.permissions.Source;
import com.wiley.permissions.domain.persistence.permissions.Country;
import com.wiley.permissions.domain.persistence.permissions.User;
import com.wiley.permissions.domain.persistence.permissions.UserDefaults;
import com.wiley.permissions.domain.persistence.permissions.UserGroup;
import com.wiley.permissions.repositories.ConditionRepository;
import com.wiley.permissions.repositories.UserRepository;
import com.wiley.permissions.services.SecurityService;
import com.wiley.permissions.services.UserService;
import com.wiley.permissions.web.shared.PermUserContext;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;
import com.wiley.permissions.web.shared.controllers.BaseFormBean.FormMode;

/**
 *
 * @author ttidwell
 */
@Controller
/*@RequestMapping("/myaccount")*/
@RequestMapping
@SessionAttributes(MyAccountController.MODEL_FORM_NAME)
public class MyAccountController
extends BaseAnnotatedController
{
	private final static Log log = LogFactory.getLog(MyAccountController.class);

	// when compile on command line requires this not to be private or won't compile
	// (since used above class definition)
	protected final static String MODEL_FORM_NAME = "myAccountForm";

	private String [] pages;

	private SecurityService securityService;
	private UserService userService;
	private UserRepository userRepository;
	private ConditionRepository conditionRepository;

	@Override
	protected List<String> getReferenceDataNames(HttpServletRequest request)
	throws Exception
	{
		List<String> output = new ArrayList<String>();

		output.add("accounts");
		output.add("componentCategoryList");
		output.add("wileyEntities");
		output.add("currencies");
		output.add("countries");
		output.add("favoriteGroups");
		output.add("userLocations");
		output.add("businessUnits");
		output.add("sizes");
		return output;
	}

	private MyAccountForm loadEssentials(Model model, HttpServletRequest request, String [] lazyLoadProps) throws Exception {
		MyAccountForm form = new MyAccountForm();
		model.addAttribute(MODEL_FORM_NAME, form);

		Integer userId = PermUserContext.getCurrentUserId(request);
		User user = getUserRepository().lazyLoad (User.class, userId, lazyLoadProps);

		if (user.getUserDefaults() == null) {
			UserDefaults ud = new UserDefaults();
			ud.setUser(user);
			user.setUserDefaults(ud);
		}

		form.setUser(user);

		return form;
	}

	private ModelAndView createSuccessView(MyAccountForm form) {
		ModelAndView mv = new ModelAndView(getSuccessView());
		mv.addObject(MODEL_FORM_NAME, form);
		return mv;
	}

	// smarkoff: Over time, it might be good to break out more of these "pages" into
	// separate methods (as above).

	@RequestMapping(value = "/myaccount/form", method = {RequestMethod.GET, RequestMethod.POST})
	public String form(HttpServletRequest request,
			@RequestParam(value="page", required=false) Integer page,
			Model model)
	throws Exception
	{
		log.debug("form(): entered...page = " + page);

		MyAccountForm form = loadEssentials(model, request, new String[] {"sources", "favoriteGroup", "group"});
		User user = form.getUser();

		Country country = getRepository().find(Country.class, user.getUserDefaults().getCountryCode());
		user.getUserDefaults().setCountry(country);

		if (null != user.getFavoriteGroup()) {
			form.setFavoriteGroupId(user.getFavoriteGroup().getId());
		}

		List<UserGroup> userGroupList = userRepository.loadAll(UserGroup.class);
		model.addAttribute("userGroupList", userGroupList);

		if (page == null)  return getFormView();
		else  return pages[page];
	}

	@RequestMapping(value = "/myaccount/submit", method = RequestMethod.POST)
	public ModelAndView submit(HttpServletRequest request,
			@ModelAttribute(MODEL_FORM_NAME) MyAccountForm form,
			BindingResult bindingResult, @RequestParam("page") int page,
			@RequestParam(value = "values", required=false) String[] sourceIds)
	throws Exception
	{
		log.debug("onSubmit(): entered...page = " + page + ", form.getMode() = " + form.getMode());
		FormMode mode = form.getMode();

		if (mode != FormMode.CANCEL) {
			// actually right now this code does nothing because the
			// validation for this form is empty (in validation.xml)
			getValidator().validate(form, bindingResult);

			if (page == 2) {
				// checking for blank should really be done by validation.xml (see note above)
				// but still need extra check for number of newlines (below)

				String returnAddress = form.getUser().getUserDefaults().getReturnAddress();
				String errorMsg = PurchaseOrder.validateReturnAddress(returnAddress);
				if (errorMsg != null) {
					bindingResult.rejectValue("user.userDefaults.returnAddress", null, errorMsg);
				}

				if (StringUtils.isBlank(form.getUser().getUserDefaults().getUserSignature())) {
					bindingResult.rejectValue("user.userDefaults.userSignature", null, "User Signature cannot be blank.");
				}
			}

			if (bindingResult.hasErrors()) {
				ModelAndView mv = new ModelAndView(pages[page]);
				mv.addObject(MODEL_FORM_NAME, form);
				return mv;
			}
		}

		if (mode != FormMode.CANCEL && page > 0) {
			User user = form.getUser();

			if (page == 5) {
				PermUserContext.getUserSession(request).getCurrentUser().setShowRequest(user.getUserDefaults().isShowRequest());
				PermUserContext.getUserSession(request).getCurrentUser().setCustomMode(user.getUserDefaults().isCustomMode());
				PermUserContext.getUserSession(request).getCurrentUser().setCustomFilter(user.getUserDefaults().getCustomFilter());
				PermUserContext.getUserSession(request).getCurrentUser().setCurrencyCode(user.getUserDefaults().getCurrency().getCode());
				PermUserContext.getUserSession(request).getCurrentUser().setCountryCode(user.getUserDefaults().getCountryCode());
			}

			// My Sources page
			if (page == 6) {
				List<Source> sources = new ArrayList<Source>();
				// null means no sources
				if (null != sourceIds) {
					for (String sourceId : sourceIds) {
						sources.add (userRepository.find(Source.class, new Integer(sourceId)));
					}
					user.setSources(sources);
				} else {
					user.setSources(null);
				}
				
				if (null != form.getFavoriteGroupId()) {
					if (form.getFavoriteGroupId() < 1) {
						user.setFavoriteGroup(null);
					}
					else {
						user.setFavoriteGroup(new FavoriteGroup(form.getFavoriteGroupId(), "dummy"));
					}
				}
			}

			user = userRepository.save(user);
			
			log.debug("onSubmit(): updated user.");
			form.setUser(user);
		}

		return createSuccessView(form);
	}


	public String [] getPages() {
		return pages;
	}

	public void setPages(String [] pages) {
		this.pages = pages;
	}

	public SecurityService getSecurityService() {
		return securityService;
	}

	public void setSecurityService(SecurityService securityService) {
		this.securityService = securityService;
	}

	public UserService getUserService() {
		return userService;
	}

	public void setUserService(UserService userService) {
		this.userService = userService;
	}

	public UserRepository getUserRepository() {
		return userRepository;
	}

	public void setUserRepository(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public void setConditionRepository(ConditionRepository conditionRepository) {
		this.conditionRepository = conditionRepository;
	}

	public ConditionRepository getConditionRepository() {
		return conditionRepository;
	}
}
