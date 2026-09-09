package com.wiley.permissions.web.internal.controllers.admin;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.ModelAndView;

import com.wiley.permissions.domain.persistence.permissions.SystemNotification;
import com.wiley.permissions.repositories.SystemNotificationRepository;
import com.wiley.permissions.repositories.UserRepository;
import com.wiley.permissions.services.SecurityService;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;

/**
 *
 * @author nmedrano
 */
@Controller
@RequestMapping("/admin/manageSystemNotification")
@SessionAttributes(ManageNotificationController.FORM_MODEL_NAME)
public class ManageNotificationController

extends BaseAnnotatedController
{
	private final static Log log = LogFactory.getLog(ManageNotificationController.class);

	// Can't be private or get compile error for SessionAttributes above
	protected final static String FORM_MODEL_NAME = "manageNotificationForm";

	private SecurityService securityService;
	private SystemNotificationRepository systemNotificationRepository;

	private UserRepository userRepository;

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

		return output;
	}

	@RequestMapping(value = "/edit", method = RequestMethod.GET)
	public ModelAndView formBackingObject(HttpServletRequest request,	Model model)
	throws Exception
	{
		log.debug("formBackingObject() entered...");

		ModelAndView mv = null;
		mv = new ModelAndView("pages.admin.system.notification.manage");
		SystemNotification sn = null;
		HttpSession session = request.getSession();
		ManageNotificationForm form = (ManageNotificationForm) session.getAttribute(FORM_MODEL_NAME);

		form = new ManageNotificationForm();
		sn = new SystemNotification();

			List<SystemNotification> notes = getSystemNotificationRepository().loadPendingNotifications();
			if (null == notes || notes.size() < 1) {
				sn =  new SystemNotification();
				sn.setFromDate(new Date());
				sn.setToDate(new Date());
				sn.setMessageToPost("");
			} else {
				sn = notes.get(0);
			}

			form.setSystemNotification(sn);

			form.setNotelist(notes);

		mv.addObject(FORM_MODEL_NAME, form);

		return mv;
	}


	@RequestMapping(value = "/edit", method =  RequestMethod.POST)
	public ModelAndView onSubmit(HttpServletRequest request,
			@ModelAttribute(FORM_MODEL_NAME) ManageNotificationForm form,
			BindingResult bindingResult)
	throws Exception
	{
		log.debug("onSubmit() called: ");
		ModelAndView mv = new ModelAndView(getSuccessView());

		if ("Delete".equals(form.getSelectedAction())) {
			try {
			    getSystemNotificationRepository().deleteNotificationById(form.getSelectedId());
			} catch (Exception e) {
				// no worries
			}
			form.setSelectedAction(null);
			form.setNotelist(null);
			mv = new ModelAndView(getSuccessView());
	    	mv.addObject(FORM_MODEL_NAME, form);

			return mv;
		}

		if ("Update".equals(form.getSelectedAction())) {
			form.setSystemNotification(getSystemNotificationRepository().loadById(form.getSelectedId()));

			form.setSelectedAction(null);
			form.setNotelist(null);
			mv = new ModelAndView("pages.admin.system.notification.manage");
			mv.addObject(FORM_MODEL_NAME, form);

			return mv;
		}

		if ("Add".equals(form.getSelectedAction())) {
			SystemNotification note = new SystemNotification();
			note.setMessageToPost("Add you message here");
			note.setFromDate(new Date());
			note.setToDate(new Date());
			form.setSystemNotification(note);
			form.setSelectedAction(null);
			form.setSelectedId(null);
			mv = new ModelAndView("pages.admin.system.notification.manage");
			form.setNotelist(null);
			mv.addObject(FORM_MODEL_NAME, form);

			return mv;
		}

		if ("Save".equals(form.getSelectedAction())) {
			getSystemNotificationRepository().saveRequiresNew(form.getSystemNotification());

			mv = new ModelAndView(getSuccessView());
	    	mv.addObject(FORM_MODEL_NAME, form);

			return mv;
		}

		mv = new ModelAndView("redirect:/sapp/admin/manageSystemNotification/edit");
		mv.addObject(FORM_MODEL_NAME, form);
        return mv;
	}

	public SecurityService getSecurityService() {
		return securityService;
	}

	public void setSecurityService(SecurityService securityService) {
		this.securityService = securityService;
	}

	public SystemNotificationRepository getSystemNotificationRepository() {
		return systemNotificationRepository;
	}

	public void setSystemNotificationRepository(SystemNotificationRepository systemNotificationRepository) {
		this.systemNotificationRepository = systemNotificationRepository;
	}

	public UserRepository getUserRepository() {
		return userRepository;
	}

	public void setUserRepository(UserRepository userRepository) {
		this.userRepository = userRepository;
	}
}
