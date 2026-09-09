package com.wiley.permissions.web.shared.controllers.servicedesk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import com.wiley.permissions.domain.persistence.permissions.PermissionType;
import com.wiley.permissions.domain.persistence.permissions.Role;
import com.wiley.permissions.domain.persistence.permissions.User;
import com.wiley.permissions.repositories.UserRepository;
import com.wiley.permissions.security.sso.SSOUser;
import com.wiley.permissions.security.web.AuthenticationException;
import com.wiley.permissions.services.AssetService;
import com.wiley.permissions.services.SSOUserLookupUtility;
import com.wiley.permissions.services.SecurityService;
import com.wiley.permissions.services.UserService;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;

/**
 * TODO: 
 *
 * @author Abdul
 */
@Controller
public class AddUsersController extends BaseAnnotatedController {
	private static final Log log = LogFactory.getLog(AddUsersController.class);

	public static final String FORM_MODEL_NAME = "addUsersForm";
	private final static String GENERAL_MESSAGE = "generalMessage";
	private SecurityService securityService;
	private String landingView = null;
	boolean saved = false;
	private UserRepository userRepository = null;
	private UserService userService = null;
	
	

	@Override
	protected List<String> getReferenceDataNames(HttpServletRequest request) throws Exception
	{
		List<String> output = new ArrayList<String>();

		output.add("favoriteGroups");
	//	output.add("userTypes");
		output.add("globalRoles");
		output.add("userGroups");

		return output;
	}
	
	@ModelAttribute
	public void customReferenceData(Model model) {
		List<User.Type> userTypes = Arrays.asList(User.Type.values());

		model.addAttribute("userTypes", userTypes);
	}

	@Override
	@ModelAttribute
	public void referenceData(Model model, HttpServletRequest request)
	throws Exception
	{
		super.referenceData(model, request);
		model.addAttribute("permissionTypes", PermissionType.VALID_VALUES);
	}

	@RequestMapping(value = "/servicedesk/addUsers", method = RequestMethod.GET)
	public String form(HttpServletRequest request,	Model model)
	throws Exception
	{
		log.debug("form(): entered...");
		
		AddUsersForm form= new AddUsersForm();

		
		form.setCameFrom(request.getParameter("cameFrom"));
		
		model.addAttribute(FORM_MODEL_NAME, form);

		return getFormView();
	}

	
	@RequestMapping(value = "/servicedesk/addUsers", method = RequestMethod.POST)
	public ModelAndView submit(HttpServletRequest request,
			@ModelAttribute(FORM_MODEL_NAME) AddUsersForm form,
			BindingResult bindingResult)
	throws AuthenticationException,Exception
	{
		
		
		//SSOUser ssoUser = null;
		log.debug("submit(): Abdul entered...");
		getValidator().validate(form, bindingResult);

		if (form.getCode()==null || form.getFirstName()==null || form.getLastName()==null || form.getEmail()==null || form.getGroup()==null ) {
		    bindingResult.reject(null, "You must specify all the values.");
		}
		
		
		
	/*	
	
		if(form.getEmail()!=null)
		{
		ssoUser = internalSSOUserLookupUtility.findByEmail(form.getEmail());
		if(null == ssoUser){
				request.setAttribute(GENERAL_MESSAGE, "Could Not Find user From LDAP");
			}
		}
		else {
			String msg = "Could Not Find User From LDAP";
				throw new ServiceException (msg,true);
		}
				
		if (ssoUser == null) {
			bindingResult.reject(null, "Enter the valid Wiley email id");
		}
	
		*/
		User user1 = null;
		if (null != form.getEmail() && form.getEmail().length() > 0) {
			user1 = userRepository.loadByEmail(form.getEmail());
			if (null != user1) {
				bindingResult.reject(null, "This email id already exists in the system.");
			}
		}

		if (null != form.getCode() && form.getCode().length() > 0) {
			user1 = userRepository.loadByCode(form.getCode());
			if (null != user1) {
				bindingResult.reject(null, "This username already exists in the system.");
			}
		}
		
		if (bindingResult.hasErrors()) {
			return new ModelAndView(getFormView());
		}
	//	String view = getFormView();
		ModelAndView mv= null;
		User user=new User();

		
		// add try catch and in catch block write the error to be shown to the user.
		user.setCode(form.getCode());
		user.setFirstName(form.getFirstName());
		user.setLastName(form.getLastName());
		user.setEmail(form.getEmail());
		user.setEnabled(form.isEnabled());
		user.setGroup(form.getGroup());
	//	user.setType(form.getUserType());
		
		List<Role> globalRoles = form.getGlobalRolesNotNull();
		
		
		for (Role global : globalRoles) {
				user.addGlobalRole(global);
			}
		
			user = userService.saveUser(user,true);  
	
	
		mv = new ModelAndView(getFormView());
	//	mv.addObject("hasErrors", bindingResult.hasErrors());
		mv.addObject("generalMessage", "Your data for  [" + form.getEmail() + "]  has been successfully saved.");
		mv.addObject(FORM_MODEL_NAME, form);
		
		saved=true;
		
		if(saved){
			reset(form);
			}
		return mv;
		
	}
	
	protected void reset(AddUsersForm form) {
		log.debug("reset(): entered...");

		form.setMode(form.getOriginalMode());
		form.setCode(null);
		form.setEmail(null);
		form.setEnabled(false);
		form.setFirstName(null);
		form.setLastName(null);
		form.getGlobalRoles().clear();
		form.setGroup(null);
		
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
	
	public UserRepository getUserRepository() {
    	return userRepository;
    }

	public void setUserRepository(UserRepository userRepository) {
    	this.userRepository = userRepository;
    }

	public String getLandingView() {
		return landingView;
	}

	public void setLandingView(String landingView) {
		this.landingView = landingView;
	}
	
	
}
