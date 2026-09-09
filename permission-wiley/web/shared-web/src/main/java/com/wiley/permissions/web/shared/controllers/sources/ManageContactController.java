package com.wiley.permissions.web.shared.controllers.sources;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.SessionAttributes;

import com.wiley.permissions.domain.persistence.permissions.Contact;
import com.wiley.permissions.domain.persistence.permissions.Country;
import com.wiley.permissions.domain.persistence.permissions.Source;
import com.wiley.permissions.repositories.SourceRepository;
import com.wiley.permissions.services.SourceService;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;
import com.wiley.permissions.web.shared.controllers.BaseFormBean.FormMode;
import com.wiley.permissions.web.shared.util.CountryPropertyEditorSupport;

/**
 *
 * @author ttidwell
 */
@Controller
/*@RequestMapping(value = {"/sources/manageContact", "/sources/po/manageContact"})*/
@RequestMapping
@SessionAttributes(ManageContactController.MODEL_FORM_NAME)
public class ManageContactController extends BaseAnnotatedController {

	private final static Log log = LogFactory.getLog(ManageContactController.class);

	// when compile on command line requires this not to be private or won't compile
	// (since used above class definition)
	protected static final String MODEL_FORM_NAME = "manageContactForm";

	private static final String FORM_VIEW_SOURCES = "pages.sources.manageContact";
	private static final String FORM_VIEW_PO = "pages.po.addContact";
	private static final String SUCCESS_VIEW_SOURCES = "redirect:/sapp/sources/viewSource/form";
	private static final String SUCCESS_VIEW_PO = "redirect:/sapp/permissions/po/details";

	private SourceService sourceService = null;
	private SourceRepository sourceRepository = null;

	private String resolveFormView(HttpServletRequest request) {
		return isPoPath(request) ? FORM_VIEW_PO : FORM_VIEW_SOURCES;
	}

	private String resolveSuccessView(HttpServletRequest request) {
		return isPoPath(request) ? SUCCESS_VIEW_PO : SUCCESS_VIEW_SOURCES;
	}

	private boolean isPoPath(HttpServletRequest request) {
		String uri = request.getRequestURI();
		return uri != null && uri.contains("/sources/po/manageContact");
	}

	@Override
	@InitBinder
	public void initBinder(WebDataBinder binder) throws Exception
	{
		log.debug("initBinder(): called for objectName = " + binder.getObjectName());
		super.initBinder(binder);
		binder.registerCustomEditor(Country.class, new CountryPropertyEditorSupport());
	}

	@Override
	protected List<String> getReferenceDataNames(HttpServletRequest request) throws Exception
	{
		List<String> output = new ArrayList<String>();

		output.add("addressTypes");
		output.add("countries");

		return output;
	}

	// lnagy - both methods were POST
	// difficult to differentiate between SUBMIT and the link from ViewSource
	// that is why I changed the URL
	/*@GetMapping("/form")*/
	@RequestMapping(value = {"/sources/manageContact/form", "/sources/po/manageContact/form"}, method = {RequestMethod.GET, RequestMethod.POST})
	@SuppressWarnings("incomplete-switch")
	public String form(Model model, HttpServletRequest request) throws Exception
	{
		log.debug("form(): entered...");

		ManageContactForm form = new ManageContactForm();

		Integer sourceId = null;

		String test = request.getParameter(ViewSourceController.SOURCE_ID);

		if (test == null) {
			sourceId = (Integer) request.getAttribute(ViewSourceController.SOURCE_ID);
		}
		else {
			sourceId = new Integer(test);
		}

		if (sourceId != null) {
			form.setSourceId(sourceId);

			FormMode modeTest = (FormMode) request.getAttribute("mode");

			if (modeTest != null) {
				form.setMode(modeTest);
			}
			else {
				form.setMode(FormMode.ADD);
			}

			Source source = sourceRepository.loadSourceById(sourceId);

			Contact contact = null;

			log.info("form(): Got source: " + source.getId() + " -- " + form.getMode());

			switch (form.getMode()) {
				case ADD:
					contact = new Contact();
					contact.setSource(source);
					break;

				case VIEW:
				case MODIFY:
				case REMOVE:
					Integer contactId = (Integer) request.getAttribute("contactId");
					if (contactId != null) {
						contact = sourceRepository.find(Contact.class, contactId);
					}
			}

			form.setContact(contact);
		}

		form.setCameFrom(request.getParameter("cameFrom"));

		model.addAttribute(MODEL_FORM_NAME, form);

		return resolveFormView(request);
	}

	/*@GetMapping("/submit")*/
	@RequestMapping(value = {"/sources/manageContact/submit", "/sources/po/manageContact/submit"}, method = {RequestMethod.GET, RequestMethod.POST})
	@SuppressWarnings("incomplete-switch")
	public String onSubmit(HttpServletRequest request, Model model,
			@ModelAttribute(MODEL_FORM_NAME) ManageContactForm form, BindingResult bindingResult)
			throws Exception
	{
		log.debug("onSubmit(): entered...");

		if (form.getMode() != FormMode.CANCEL) {
			getValidator().validate(form, bindingResult);

			if (bindingResult.hasErrors()) {
				model.addAttribute(MODEL_FORM_NAME, form);
				return resolveFormView(request);
			}
		}

		Contact contact = form.getContact();

		switch (form.getMode()) {
			case ADD:
			case MODIFY:
				if (contact.getAddress().isBlank()) {
					contact.setAddress(null);
					// now when saves contact will Cascade remove on Address
				}
				sourceRepository.saveRequiresNew(contact);
				break;

			case REMOVE:
				sourceRepository.removeRequiresNew(contact);
				break;

			case CANCEL:
				break;
		}

		String viewName = resolveSuccessView(request) + "?" + ViewSourceController.SOURCE_ID + "=" + form.getSourceId();

		if (StringUtils.isNotBlank(form.getCameFrom())) {
			viewName += "&cameFrom=" + form.getCameFrom();
		}

		log.debug("onSubmit(): Going to viewName: " + viewName);

		return viewName;
	}

	public SourceService getSourceService() {
    	return sourceService;
    }

	public void setSourceService(SourceService sourceService) {
    	this.sourceService = sourceService;
    }

	public SourceRepository getSourceRepository() {
    	return sourceRepository;
    }

	public void setSourceRepository(SourceRepository sourceRepository) {
    	this.sourceRepository = sourceRepository;
    }
}
