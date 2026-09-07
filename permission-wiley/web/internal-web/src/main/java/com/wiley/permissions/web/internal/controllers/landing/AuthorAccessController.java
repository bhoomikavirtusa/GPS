package com.wiley.permissions.web.internal.controllers.landing;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.ModelAndView;

import com.wiley.permissions.common.transformer.ObjectToJson;
import com.wiley.permissions.domain.persistence.permissions.AuthorCommonWorkPK;
import com.wiley.permissions.domain.persistence.permissions.AuthorToCommonWork;
import com.wiley.permissions.domain.persistence.permissions.CommonWork;
import com.wiley.permissions.domain.persistence.permissions.Component;
import com.wiley.permissions.domain.persistence.permissions.Privilege;
import com.wiley.permissions.domain.persistence.permissions.Product;
import com.wiley.permissions.domain.persistence.permissions.PublicationStatus;
import com.wiley.permissions.domain.persistence.permissions.Role;
import com.wiley.permissions.domain.persistence.permissions.User;
import com.wiley.permissions.repositories.CommonWorkRepository;
import com.wiley.permissions.repositories.UserRepository;
import com.wiley.permissions.security.sso.SSOUser;
import com.wiley.permissions.services.CommonWorkService;
import com.wiley.permissions.services.ExternalUserLdapClient;
import com.wiley.permissions.services.SSOUserLookupUtility;
import com.wiley.permissions.services.SecurityService;
import com.wiley.permissions.services.UserService;
import com.wiley.permissions.web.internal.controllers.admin.ManageUserForm;
import com.wiley.permissions.web.shared.PermUserContext;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;

/*@RequestMapping("/landing/authorAccess")*/
@RequestMapping
@SessionAttributes(value = {AuthorAccessController.USER_FORM_MODEL_NAME, AuthorAccessController.AUTHOR_PRIVILEGE_FORM_NAME})
public class AuthorAccessController extends BaseAnnotatedController {

	// --------------------- static data ---------------------------------
	private final static Log log = LogFactory.getLog(AuthorAccessController.class);

	protected final static String USER_FORM_MODEL_NAME = "manageUserForm";
	protected final static String AUTHOR_PRIVILEGE_FORM_NAME = "authorPrivilegeForm";

	// --------------------- instance data -------------------------------

	private String privilegeView;

	private CommonWorkService commonWorkService;
	private CommonWorkRepository commonWorkRepository;
	private UserRepository userRepository;
	private UserService userService;
	private SecurityService securityService;
	private ExternalUserLdapClient externalUserLdapClient;
	private SSOUserLookupUtility externalSSOUserLookupUtility;


	@Override
	protected List<String> getReferenceDataNames(HttpServletRequest request)
	throws Exception
	{
		List<String> output = new ArrayList<String>();

		output.add("countries");
		return output;
	}

	@RequestMapping(value="/landing/authorAccess/viewEdit", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView viewEdit(HttpServletRequest request,
			@RequestParam("cwId") Integer cwId)
			throws Exception
	{
		log.debug("viewEdit(): entered...cwId = " + cwId);

		List<User> peAuthorList = commonWorkRepository.getPEAuthorsForCommonWork(cwId);
		List<User> accessAuthorList = userRepository.getAuthorAccessForCommonWork(cwId);
		CommonWork cw = commonWorkRepository.lazyLoad(CommonWork.class, cwId, new String[] {"products"});
		Product primary = commonWorkRepository.lazyLoad(Product.class, cw.getPrimaryProduct().getId(), new String[] {"publicationStatus"});
		// don't show anything in the PE list that is already in our custom list
		peAuthorList.removeAll(accessAuthorList);

		ModelAndView mv = new ModelAndView(getFormView());
		mv.addObject("peAuthorList", peAuthorList);
		mv.addObject("accessAuthorList", accessAuthorList);
		mv.addObject("isPreProduction", PublicationStatus.isPreProduction(primary.getPublicationStatus().getCode(), false)); // pubStatus not nullable
		mv.addObject("publicationStatus", primary.getPublicationStatus());
		return mv;
	}

	@RequestMapping(value = "/landing/authorAccess/userDetails", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView userDetailsLoad(HttpServletRequest request,
			@RequestParam(value="userId", required=false) Integer userId,
			@RequestParam(value="lastName", required=false) String lastName,
			@RequestParam("cwId") int cwId)
			throws Exception
	{
		// cwId is not used in this method but it is read by the JSP
		log.debug("userDetailsLoad(): entered...userId = " + userId + ", cwId = " + cwId);

		ModelAndView mv = new ModelAndView("pages.landing.authorAccess.userDetails");
		ManageUserForm form = new ManageUserForm();
		User user = new User();
		if (userId == null || userId == 0) {
			if (StringUtils.isNotBlank(lastName)) {
				List<User> userList = userRepository.loadAuthorsByLastName(lastName);
				if (userList.size() == 0) {
					user.setLastName(lastName);
				}
				else user = userList.get(0);
			}
		} else {
			user = userRepository.loadUserById(userId);
		}
		form.setUser(user);

		// default country to user profile
		if (StringUtils.isBlank(user.getCountryCode())) {
			String countryCode = PermUserContext.getUserSession(request).getCurrentUser().getCountryCode();
			user.setCountryCode(countryCode);
		}

		mv.addObject(USER_FORM_MODEL_NAME, form);
		return mv;
	}

	@RequestMapping(value = "/landing/authorAccess/userDetails", method = RequestMethod.POST)
	public Object userDetailsSubmit(@ModelAttribute(USER_FORM_MODEL_NAME) ManageUserForm form,
			@RequestParam("cwId") int cwId,
			HttpServletResponse response)
			throws Exception
	{
		log.debug("userDetailsSubmit(): entered...cwId = " + cwId);
		User user = form.getUser();

		// smarkoff: If this was a user imported from PE then will exist in our DB only
		// so don't assume that user exists in externalSSO just because we have in our database.
		// - Also don't assume that if user does not exist in our database, that it does not exist
		// in externalSSO -- could have been created by a different app. server (QA vs. PROD) or
		// been created and then not put in our DB due to an error.

		SSOUser ssoUser = externalSSOUserLookupUtility.findByEmail(user.getEmail());
		log.debug("userDetailsSubmit(): user already in our db: " + (user.getId() != null));
		log.debug("userDetailsSubmit(): user already in externalSSO: " + (ssoUser != null));

		if (ssoUser == null) {
			try {
				String password = externalUserLdapClient.createAccount(user.getEmail(), user.getFirstName(), user.getLastName(), null);
				commonWorkService.sendEmailAuthorTempPassword(user.getEmail(), password);
				log.debug("userDetailsSubmit(): generated password for new user lastName ["
					+ user.getLastName() + "] email [" + user.getEmail() + "]");
			} catch (Exception e) {
				log.debug("userDetailsSubmit(): failed to create the account", e);
				PrintWriter writer = response.getWriter(); // throws IOException
				writer.write("Error: " + e.getMessage());
				return null;
			}
		}
		//Added for Author/Freelancer to make as internal User
		user.setViewType("addUser");
		user = userRepository.saveUser(user);
		user = commonWorkService.addAuthorIfNotIncluded(cwId, user);
		userRepository.addRoleToUserIfNotThere(user.getId(), Role.AUTHOR_DEFAULT);
		ModelAndView mv = new ModelAndView(getPrivilegeView() + "?cwId=" + cwId + "&authorId=" + user.getId());
		return mv;
	}

	@RequestMapping(value = "/landing/authorAccess/regenPassword", method = RequestMethod.POST)
	public void regenPassword(@ModelAttribute(USER_FORM_MODEL_NAME) ManageUserForm form, HttpServletResponse response)
			throws Exception
	{
		log.debug("regenPassword(): entered...");
		User user = form.getUser();
		// if new user
		if (user.getId() == null || user.getId() == 0 || StringUtils.isBlank(user.getEmail()))
			return;

		String password = externalUserLdapClient.setPassword(user.getEmail(), null);
		commonWorkService.sendEmailAuthorTempPassword(user.getEmail(), password);
		log.debug("regenPassword(): generated password for user lastName ["
			+ user.getLastName() + "] email [" + user.getEmail() + "]");

		PrintWriter writer = response.getWriter(); // throws IOException
		writer.write("");
		return;
	}

	@RequestMapping(value = "/landing/authorAccess/privileges", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView privilegesLoad(HttpServletRequest request,
			@RequestParam("cwId") int cwId, @RequestParam("authorId") int authorId)
			throws Exception
	{
		log.debug("privilegesLoad(): entered...");

		AuthorPrivilegeForm form = new AuthorPrivilegeForm();
		form.setCwId(cwId);
		form.setAuthorId(authorId);

		AuthorToCommonWork a2cw = userRepository.find(AuthorToCommonWork.class, new AuthorCommonWorkPK (authorId, cwId));
		Date dueDate = (null == a2cw) ? null : a2cw.getDueDate();
		form.setDueDate(dueDate);
		boolean authorReadOnly = (null == a2cw) ? false : a2cw.isReadOnly();
		form.setAuthorReadOnly(authorReadOnly);

		List<String> selectedCwList;
		List<String> selectedGlobalList;
		if (dueDate == null) {  // means that no privileges ever saved for this author/cwId combo
			Integer userId = PermUserContext.getCurrentUserId(request);
			// We can just use same list of all selected privileges for both cw and global
			selectedCwList = userRepository.getAuthorDefaultPrivileges(userId);
			selectedGlobalList = selectedCwList;
		}
		else {
			selectedCwList = userRepository.getSelectedAuthorPrivileges(authorId, cwId);
			selectedGlobalList = userRepository.getSelectedAuthorPrivileges(authorId, null);
		}
		List<Privilege> completeCwList = userRepository.getCompleteAuthorPrivileges(false);
		List<Privilege> completeGlobalList = userRepository.getCompleteAuthorPrivileges(true);
		String [] selectedCwArray = selectedCwList.toArray(new String[selectedCwList.size()]);
		String [] selectedGlobalArray = selectedGlobalList.toArray(new String[selectedGlobalList.size()]);
		form.setSelectedCwPrivileges(selectedCwArray);
		form.setSelectedGlobalPrivileges(selectedGlobalArray);
		ModelAndView mv = new ModelAndView("pages.landing.authorAccess.privileges");
		mv.addObject("completeCwPrivilegeList", completeCwList);
		mv.addObject("completeGlobalPrivilegeList", completeGlobalList);
		mv.addObject(AUTHOR_PRIVILEGE_FORM_NAME, form);
		
		String dateFormat = PermUserContext.getPickerDateFormat(request);
		mv.addObject("dateFormat",dateFormat);
	
		return mv;
	}

	@RequestMapping(value = "/landing/authorAccess/privileges", method = RequestMethod.POST)
	public ModelAndView privilegesSubmit(HttpServletRequest request,
			@ModelAttribute(AUTHOR_PRIVILEGE_FORM_NAME) AuthorPrivilegeForm form)
			throws Exception
	{
		log.debug("privilegesSubmit(): entered...");

		AuthorToCommonWork a2cw = userRepository.find(AuthorToCommonWork.class, new AuthorCommonWorkPK (form.getAuthorId(), form.getCwId()));
		if (null == a2cw) {
			a2cw = new AuthorToCommonWork();
			a2cw.setUserId(form.getAuthorId());
			a2cw.setCwId(form.getCwId());
		}
		a2cw.setDueDate(form.getDueDate());
		a2cw.setReadOnly(form.getAuthorReadOnly());
		userRepository.save(a2cw);

		// userRepository.saveAuthorDueDate(form.getAuthorId(), form.getCwId(), form.getDueDate());
		userRepository.saveSelectedAuthorPrivileges(form.getAuthorId(), form.getCwId(), form.getSelectedCwPrivileges());
		userRepository.saveSelectedAuthorPrivileges(form.getAuthorId(), null, form.getSelectedGlobalPrivileges());
		// userRepository.saveAuthorReadOnly(form.getAuthorId(), form.getCwId(), form.getAuthorReadOnly());

		if (form.getSaveAsDefaults()) {
			Integer userId = PermUserContext.getCurrentUserId(request);
			List<String> selectedPrivileges = new ArrayList<String>();
			for (String code : form.getSelectedCwPrivileges()) {
				selectedPrivileges.add(code);
			}
			for (String code : form.getSelectedGlobalPrivileges()) {
				selectedPrivileges.add(code);
			}
			userRepository.saveAuthorDefaultPrivileges(userId, selectedPrivileges);
		}

		// The JSP just closes the dialog after receiving the result of this controller
		// so the contents don't matter.
		ModelAndView mv = new ModelAndView("pages.landing.authorAccess.privileges");
		// if edit all chapters is not selected, we redirect to edit_chapters page
		if (!ArrayUtils.contains(form.getSelectedCwPrivileges(), Privilege.EDIT_CHAPTERS.getCode())) {
			mv = new ModelAndView("redirect://sapp/landing/authorAccess/chapters");
			mv.addObject(AUTHOR_PRIVILEGE_FORM_NAME, form);
		}
		return mv;
	}

	@RequestMapping(value = "/landing/authorAccess/disable", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView disableAuthorAccessAssociation(HttpServletRequest request,
			@RequestParam("cwId") int cwId, @RequestParam("authorId") int authorId)
			throws Exception
	{
		ModelAndView mv = new ModelAndView("redirect:/sapp/landing/authorAccess/viewEdit?cwId=" + cwId);

		try {
			userRepository.removeAuthorToCommonWorkAssociation(authorId, cwId);
		} catch (Exception e) {
			// do nothing
		}

		return mv;
	}

	@RequestMapping(value = "/landing/authorAccess/readOnly", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView makeProductReadOnly(HttpServletRequest request,
			@RequestParam("cwId") int cwId, @RequestParam("authorId") int authorId)
			throws Exception
	{
		ModelAndView mv = new ModelAndView("redirect:/sapp/landing/authorAccess/viewEdit?cwId=" + cwId);

		try {
			AuthorToCommonWork a2cw = userRepository.find(AuthorToCommonWork.class, new AuthorCommonWorkPK (authorId, cwId));
			if (null == a2cw) {
				a2cw = new AuthorToCommonWork();
				a2cw.setUserId(authorId);
				a2cw.setCwId(cwId);
			}
			// toggle
			boolean newValue = true;
			if (a2cw.isReadOnly()) newValue = false;
			a2cw.setReadOnly(newValue);
			userRepository.save(a2cw);

		} catch (Exception e) {
			// do nothing
		}

		return mv;
	}

	@RequestMapping(value = "/landing/authorAccess/chapters", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView chaptersLoad(@ModelAttribute(AUTHOR_PRIVILEGE_FORM_NAME) AuthorPrivilegeForm form)
			throws Exception
	{
		log.debug("chaptersLoad(): entered...");

		List<Component> chapters = commonWorkRepository.loadChapterList (form.getCwId());
		List<Integer> selectedChapters = commonWorkRepository.loadSelectedChaptersId (form.getAuthorId(), form.getCwId(), false);
		ModelAndView mv = new ModelAndView("pages.landing.authorAccess.chapters");
		mv.addObject("chapterList", chapters);
		mv.addObject("selectedChaptersId", selectedChapters);
		return mv;
	}

	@RequestMapping(value = "/landing/authorAccess/chapters", method = RequestMethod.POST)
	public ModelAndView chaptersSubmit(@RequestParam(value="values", required=false) String[] values,
			@ModelAttribute(AUTHOR_PRIVILEGE_FORM_NAME) AuthorPrivilegeForm form)
			throws Exception
	{
		log.debug("chaptersSubmit(): entered..." + values);
		// It's possible there are no chapters to select from, in which case "values" is null
		if (values != null) {
			commonWorkRepository.saveSelectedAuthorChapters (form.getAuthorId(), form.getCwId(), values);
		}
		ModelAndView mv = new ModelAndView("dialog.success");
		mv.addObject("message", "Information entered");
		return mv;
	}

	@GetMapping("/checkEmail")
	public void checkEmail(HttpServletRequest request, HttpServletResponse response,
			@ModelAttribute(USER_FORM_MODEL_NAME) ManageUserForm form,
			@RequestParam("email") String email) throws Exception
	{
		String msg = "OK";
		if (StringUtils.isNotBlank(email)) {
			User user = userRepository.loadByEmail(email);
			if (null != user) {
				msg = "A user with this e-mail address already exists \n\n" +
					" name: " + user.getFullName() + "\n\n" +
					"Is this the one you want to use?";
			}
		}

		String json = ObjectToJson.doTransform(msg, null);
		log.debug("json: " + json);

        response.setContentType("application/json");
        PrintWriter writer = response.getWriter();  // throws IOException
        writer.println(json);

	}

	@RequestMapping(value = "/landing/authorAccess/userDetailsByEmail", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView userDetailsLoadByEmail(HttpServletRequest request,
			@RequestParam("email") String email,
			@RequestParam("cwId") int cwId) throws Exception
	{
		ModelAndView mv = new ModelAndView("pages.landing.authorAccess.userDetails");

		User user = null;
		if (null != email && email.length() > 0) {
			user = userRepository.loadByEmail(email);
			if (null != user) {
				mv = new ModelAndView ("redirect:/sapp/landing/authorAccess/userDetails?cwId=" + cwId + "&userId=" + user.getId() );
			}
		}

		return mv;
	}


	public String getPrivilegeView() {
		return privilegeView;
	}

	public void setPrivilegeView(String privilegeView) {
		this.privilegeView = privilegeView;
	}

	public void setCommonWorkService(CommonWorkService commonWorkService) {
		this.commonWorkService = commonWorkService;
	}

	public CommonWorkService getCommonWorkService() {
		return commonWorkService;
	}

	public void setCommonWorkRepository(CommonWorkRepository commonWorkRepository) {
		this.commonWorkRepository = commonWorkRepository;
	}

	public CommonWorkRepository getCommonWorkRepository() {
		return commonWorkRepository;
	}

	public UserRepository getUserRepository() {
		return userRepository;
	}

	public void setUserRepository(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public UserService getUserService() {
		return userService;
	}

	public void setUserService(UserService userService) {
		this.userService = userService;
	}

	public SecurityService getSecurityService() {
		return securityService;
	}

	public void setSecurityService(SecurityService securityService) {
		this.securityService = securityService;
	}

	public void setExternalUserLdapClient(ExternalUserLdapClient client) {
		this.externalUserLdapClient = client;
	}

	public void setExternalSSOUserLookupUtility(SSOUserLookupUtility o) {
		this.externalSSOUserLookupUtility = o;
	}
}
