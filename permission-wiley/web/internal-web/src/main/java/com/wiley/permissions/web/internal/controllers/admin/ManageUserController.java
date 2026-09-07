package com.wiley.permissions.web.internal.controllers.admin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.ModelAndView;

import com.wiley.permissions.domain.persistence.permissions.Product;
import com.wiley.permissions.domain.persistence.permissions.Role;
import com.wiley.permissions.domain.persistence.permissions.User;
import com.wiley.permissions.domain.persistence.permissions.UserDefaults;
import com.wiley.permissions.domain.persistence.permissions.UserGroup;
import com.wiley.permissions.domain.persistence.permissions.UserToRole;
import com.wiley.permissions.repositories.ProductRepository;
import com.wiley.permissions.repositories.UserRepository;
import com.wiley.permissions.services.CommonWorkService;
import com.wiley.permissions.services.ExternalUserLdapClient;
import com.wiley.permissions.services.SSOUserLookupUtility;
import com.wiley.permissions.services.SecurityService;
import com.wiley.permissions.services.UserService;
import com.wiley.permissions.web.internal.controllers.product.SearchProductForm;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;
import com.wiley.permissions.web.shared.controllers.BaseFormBean.FormMode;

/**
 *
 * @author ttidwell
 */
@Controller
/*@RequestMapping("/admin/manageUser")*/
@RequestMapping
@SessionAttributes(ManageUserController.FORM_MODEL_NAME)
public class ManageUserController
extends BaseAnnotatedController
{
	private final static Log log = LogFactory.getLog(ManageUserController.class);

	// Can't be private or get compile error for SessionAttributes above
	protected final static String FORM_MODEL_NAME = "manageUserForm";

	private SecurityService securityService;

	private String searchView;
	private String userProductView;
	private ProductRepository productRepository;
	private UserRepository userRepository;
	private UserService userService;
	private CommonWorkService commonWorkService;
	private SSOUserLookupUtility externalSSOUserLookupUtility;
	private ExternalUserLdapClient externalUserLdapClient;


	@Override
	protected List<String> getReferenceDataNames(HttpServletRequest request)
	throws Exception
	{
		List<String> output = new ArrayList<String>();

		output.add("globalRoles");
		output.add("productRoles");
		output.add("accounts");
		output.add("componentCategoryList");
		output.add("userGroups");
		output.add("favoriteGroups");

		return output;
	}

	@ModelAttribute
	public void customReferenceData(Model model) {
		List<User.Type> userTypes = Arrays.asList(User.Type.values());

		model.addAttribute("userTypes", userTypes);
	}

	@RequestMapping(value="/admin/manageUser/formNew", method = {RequestMethod.GET, RequestMethod.POST})
	public String formNew(HttpServletRequest request, Model model)
	throws Exception
	{
		log.debug("formNew(): entered...");
		return formBackingObject(request, new ManageUserForm(), model);
	}

	@RequestMapping(value="/admin/manageUser/form", method = {RequestMethod.GET, RequestMethod.POST})
	@SuppressWarnings("incomplete-switch")
	public String formBackingObject(HttpServletRequest request,
			@ModelAttribute(FORM_MODEL_NAME) ManageUserForm form,
			Model model)
	throws Exception
	{
		log.debug("formBackingObject(): entered...");

		Object test = request.getAttribute("userId");

		User user = null;
		Integer userId = null;

		if (test != null) {
			userId = (Integer) test;

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
			case ADD: {
				user = new User();
				break;
			}

			case VIEW:
			case MODIFY:
			case REMOVE: {
				user = securityService.loadUserByIdForManageUser(userId);
				Properties properties = new Properties();
				properties.setProperty("user_id", userId + "");
				user.setRoles(getUserRepository().loadAll(UserToRole.class, properties));

				form.setGlobalRoles(user.getGlobalRoles());
			}
		}

		if (null != user && null != userId) {
			Properties properties = new Properties();
			properties.setProperty("user_id", userId + "");
			user.setRoles(getUserRepository().loadAll(UserToRole.class, properties));
		}

		form.setUser(user);
		if (null != user.getFavoriteGroup()) {
			form.setFavoriteGroupId(user.getFavoriteGroup().getId());
		}

		if (user.getUserDefaults() == null) {
			UserDefaults defaults = new UserDefaults();
			defaults.setUser(user);
			user.setUserDefaults(defaults);
		}

		form.setOriginalMode(form.getMode());

		model.addAttribute(FORM_MODEL_NAME, form);

		return getFormView();
	}

	@RequestMapping(value="/admin/manageUser/submit", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView onSubmit(HttpServletRequest request,
			@ModelAttribute(FORM_MODEL_NAME) ManageUserForm form,
			BindingResult bindingResult, @RequestParam("page") int page)
	throws Exception
	{
		log.debug("onSubmit(): called: page " + page + ", mode " + form.getMode());

		User user = form.getUser();
		String view = getFormView();

		switch (page) {
			case 0: {
				// logic for when User does Product Search from manageUser page in order
				// to add Product Role to User - stick search parameters in the session
				// form because we can't add onto the redirect that will happen next
				// (since this is a wizard controller)
				String searchString = request.getParameter("searchString");
				String searchField = request.getParameter("searchField");

				if (StringUtils.isNotBlank(searchString) && StringUtils.isNotBlank(searchField)) {
					HttpSession session = request.getSession();
					SearchProductForm spForm = (SearchProductForm) session.getAttribute("searchProductForm");
					if (spForm == null)  spForm = new SearchProductForm();
					spForm.setSearchString(searchString);
					spForm.setSearchField(searchField);
					session.setAttribute("searchProductForm", spForm);
					view = getSearchView();
				}

				if (form.getSelectedChild() != null) {
					int selectedChild = form.getSelectedChild();

					if (form.getMode() == FormMode.MODIFY_CHILD) {
						UserToRole u2r = getUserToRoleByIdFromCollection(user.getProductUserToRoles(), selectedChild);
						form.setUserToRole(u2r);
						view = getUserProductView();
					}
					else if (form.getMode() == FormMode.REMOVE_CHILD) {
						UserToRole u2r = getUserToRoleByIdFromCollection(user.getProductUserToRoles(), selectedChild);

						user.getRoles().remove(u2r);

						if (u2r.getId() != null) {
							form.getDeletedChildren().add(u2r.getId());
						}

						view = getFormView();
					}
				}

				break;
			}

			case 1: {
				if (form.getProductId() != null) {
					Product product = getProductRepository().loadById(form.getProductId());

					UserToRole userToRole = null;

					if (product != null) {
						for (UserToRole test : user.getProductUserToRoles()) {
							if (test.getProduct().getId().equals(form.getProductId())) {
								userToRole = test;
								break;
							}
						}

						if (userToRole == null) {
							// lnagy - I'll default to EMPLOYEE_DEFAULT for now
							form.getUser().addProductRole(product, Role.EMPLOYEE_DEFAULT);
						}
						else {
							form.setMode(FormMode.MODIFY_CHILD);
						}
					}
				}
				else {
					reset(form);
				}

				break;
			}

			case 2: {
				log.debug("processPage(): Page 2 Completed: " + form.getMode());
				reset(form);
				break;
			}

			default: {
				throw new IllegalArgumentException("Illegal page: " + page);
			}
		}

		form.setProductId(null);


		log.debug("onSubmit(): going to view: " + view);
		ModelAndView mv = new ModelAndView(view);
		mv.addObject(FORM_MODEL_NAME, form);
		return mv;
	}

	private UserToRole getUserToRoleByIdFromCollection(Collection<UserToRole> userToRoles, Integer id) {
		for (UserToRole u2r : userToRoles) {
			if (u2r.getId().equals(id)) {
				return u2r;
			}
		}
		return null;
	}

	@RequestMapping(value="/admin/manageUser/finish", method = {RequestMethod.GET, RequestMethod.POST})
	@SuppressWarnings("incomplete-switch")
	public String processFinish(HttpServletRequest request,
			@ModelAttribute(FORM_MODEL_NAME) ManageUserForm form,
			BindingResult bindingResult, SessionStatus sessionStatus,
			@RequestParam(value="oldEmail", required=false) String oldEmail)
	throws Exception
	{
		log.debug("processFinish(): entered...mode = " + form.getMode());

		// oldEmail is required for MODIFY
//		if (form.getMode().equals(FormMode.MODIFY) && StringUtils.isBlank(oldEmail)) {
//			throw new IllegalArgumentException("oldEmail request param is required");
//		}

		if (form.getMode() != FormMode.CANCEL) {
			getValidator().validate(form, bindingResult);

			if (bindingResult.hasErrors()) {
				return getFormView();
			}
		}

		switch (form.getMode()) {
			case ADD:
			case MODIFY: {
				User user = form.getUser();
				UserGroup ug = user.getGroup();
				
				// smarkoff 2/2014: Took this out because it was causing problems
				// - First of all it's assigning a UserGroup id in place of a FavoriteGroup - these are two different
				// tables and the ids are NOT interchangable.
				//if (form.getFavoriteGroupId() == -1) {
				//	form.setFavoriteGroupId(ug.getId());
				//}
				
				log.debug("processFinish(): group:" + ug.getDescription() + ", id:" + ug.getId());

				log.debug("processFinish(): User about to be modified has: "
				    + user.getRoles().size() + " products.");

				// mark all to be deleted, but reconsider those that are still used
				List<UserToRole> oldGlobalRoles = user.getGlobalUserToRoles();
				List<Role> globalRoles = form.getGlobalRolesNotNull();
				for (UserToRole old : oldGlobalRoles) {
					form.getDeletedChildren().add(old.getId());
				}

				// put back the ones that do not need to be removed
				for (Role global : globalRoles) {
					if (user.hasRole(global)) {
						form.getDeletedChildren().remove(user.getUserToRole(global).getId());
					}
					else {
						user.addGlobalRole(global);
					}
				}
				user.setGroup(ug);
				user.setViewType("editUser");
				user = userRepository.saveUser(user);
				if (null != oldEmail) {
					updateAuthorLdapIfNeeded(user, oldEmail);
				}
				if (null != form.getFavoriteGroupId() && form.getFavoriteGroupId() > -1) {
					// updating favorite source group
					log.debug("processFinish(): about to update group to: " + form.getFavoriteGroupId() + " for user: " + user.getId());
					userRepository.updateUserFavoriteGroup(user.getId(), form.getFavoriteGroupId());
				}

				log.debug("processFinish(): User after create/update has: "
				    + user.getRoles().size() + " products.");

				for (Integer u2p : form.getDeletedChildren()) {
					securityService.deleteUserToRoleById(u2p, null);
				}

				form.setUser(user);

				break;
			}

			case REMOVE: {
				// This isn't really a remove.  We simply want to enable/disable the user.
				userRepository.flipEnabled(form.getUser().getId());
				break;
			}
		}

		sessionStatus.setComplete();

		return getSuccessView();
	}

	private void updateAuthorLdapIfNeeded(User user, String oldEmail) {
		log.debug("updateAuthorLdapIfNeeded(): oldEmail [" + oldEmail + "] new email [" + user.getEmail() + "]");
		if (user.getType() == User.Type.EMPLOYEE || user.getType() == User.Type.SYSTEM)  return;
		if (StringUtils.isBlank(oldEmail)) return;
		if (oldEmail.equalsIgnoreCase(user.getEmail())) return;
		if (externalSSOUserLookupUtility.findByEmail(oldEmail) == null) return;

		try {
			log.info("updateAuthorLdapIfNeeded(): deleting from ldap where email [" + oldEmail + "]...");
			externalUserLdapClient.deleteAccount(oldEmail);

			// not going to worry about modifying password (or firstName, lastName) for existing record
			// - firstName, lastName doesn't matter for our purposes and password can also be
			// reset separately
			if (externalSSOUserLookupUtility.findByEmail(user.getEmail()) != null) return;

			log.info("updateAuthorLdapIfNeeded(): creating ldap account for email [" + user.getEmail() + "]");
			String password = externalUserLdapClient.createAccount(user.getEmail(), user.getFirstName(), user.getLastName(), null);
			commonWorkService.sendEmailAuthorTempPassword(user.getEmail(), password);
		}
		catch (Exception ex) {
			log.error("updateAuthorLdapIfNeeded(): caught exception: ", ex);
		}
	}

	protected void reset(ManageUserForm form) {
		log.debug("reset(): entered...");

		form.setMode(form.getOriginalMode());
		form.setUserToRole(null);
		form.setProductId(null);
		form.setSelectedChild(null);
	}

	public SecurityService getSecurityService() {
		return securityService;
	}

	public void setSecurityService(SecurityService securityService) {
		this.securityService = securityService;
	}

	public String getSearchView() {
		return searchView;
	}

	public void setSearchView(String searchView) {
		this.searchView = searchView;
	}

	public String getUserProductView() {
		return userProductView;
	}

	public void setUserProductView(String userProductView) {
		this.userProductView = userProductView;
	}

	public ProductRepository getProductRepository() {
		return productRepository;
	}

	public void setProductRepository(ProductRepository productRepository) {
		this.productRepository = productRepository;
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

	public void setUserRepository(UserService userService) {
		this.userService = userService;
	}

	public void setCommonWorkService(CommonWorkService service) {
		this.commonWorkService = service;
	}

	public void setExternalSSOUserLookupUtility(SSOUserLookupUtility lookup) {
		this.externalSSOUserLookupUtility = lookup;
	}

	public void setExternalUserLdapClient(ExternalUserLdapClient client) {
		this.externalUserLdapClient = client;
	}
}
