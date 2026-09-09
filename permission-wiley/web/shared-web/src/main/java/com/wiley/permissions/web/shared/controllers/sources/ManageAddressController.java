package com.wiley.permissions.web.shared.controllers.sources;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.ModelAndView;

import com.wiley.permissions.common.utils.DuplicateAddressTypeException;
import com.wiley.permissions.domain.persistence.permissions.Address;
import com.wiley.permissions.domain.persistence.permissions.Contact;
import com.wiley.permissions.domain.persistence.permissions.Country;
import com.wiley.permissions.domain.persistence.permissions.SourceAddress;
import com.wiley.permissions.domain.persistence.permissions.SourceAddressPK;
import com.wiley.permissions.repositories.SourceRepository;
import com.wiley.permissions.services.SourceService;
import com.wiley.permissions.web.shared.PermUserContext;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;
import com.wiley.permissions.web.shared.controllers.BaseFormBean.FormMode;

/**
 * @author ttidwell
 */
@Controller
/*@RequestMapping(value={"/sources/manageAddress", "/sources/po/manageAddress"})*/
@RequestMapping
@SessionAttributes(ManageAddressController.MODEL_FORM_NAME)
public class ManageAddressController extends BaseAnnotatedController {
	private final static Log log = LogFactory.getLog(ManageAddressController.class);

	// when compile on command line requires this not to be private or won't compile
	// (since used above class definition)
	protected static final String MODEL_FORM_NAME = "manageAddressForm";

	private static final String FORM_VIEW_SOURCES = "pages.sources.manageAddress";
	private static final String FORM_VIEW_PO = "pages.po.addAddress";
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
		return uri != null && uri.contains("/sources/po/manageAddress");
	}

	@Override
	protected List<String> getReferenceDataNames(HttpServletRequest request)
	        throws Exception {
		List<String> output = new ArrayList<String>();

		output.add("addressTypes");
		output.add("countries");

		return output;
	}

	/*@GetMapping("/form")*/
	@RequestMapping(value={"/sources/manageAddress/form", "/sources/po/manageAddress/form"}, method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView form(HttpServletRequest request)
	        throws Exception {
		log.debug("form(): entered...");

		ManageAddressForm form = new ManageAddressForm();

		Address address = null;
		Integer sourceId = null;

		String test = request.getParameter(ViewSourceController.SOURCE_ID);

		if (test == null) {
			sourceId = (Integer) request.getAttribute(ViewSourceController.SOURCE_ID);
		} else {
			sourceId = new Integer(test);
		}

		if (sourceId != null) {
			form.setSourceId(sourceId);

			FormMode testMode = (FormMode) request.getAttribute("mode");

			if (testMode != null) {
				form.setMode(testMode);
			} else {
				form.setMode(FormMode.ADD);
			}

			Integer addressId = (Integer) request.getAttribute("addressId");

			if (addressId != null) {
				address = sourceRepository.loadAddressById(addressId);
			}

			Integer contactId = (Integer) request.getAttribute("contactId");

			if (contactId != null) {
				Contact ic = sourceRepository.find(Contact.class, contactId);
				form.setContactId(contactId);
			}
		}

		if (address == null)
			address = new Address();

		if (address.getCountry() == null) {
			// default profile
			String countryCode = PermUserContext.getUserSession(request).getCurrentUser().getCountryCode();
			address.setCountry((Country) getReferenceDataCache().getObjectByType(
			        Country.class, countryCode));
		}

		form.setAddress(address);

		form.setCameFrom(request.getParameter("cameFrom"));

		ModelAndView mv = new ModelAndView(resolveFormView(request));
		mv.addObject(MODEL_FORM_NAME, form);
		return mv;
	}

	/*@GetMapping("/submit")*/
	@RequestMapping(value={"/sources/manageAddress/submit", "/sources/po/manageAddress/submit"}, method = {RequestMethod.GET, RequestMethod.POST})
	@SuppressWarnings("incomplete-switch")
	public String onSubmit(HttpServletRequest request,
	        @ModelAttribute(MODEL_FORM_NAME) ManageAddressForm form, BindingResult bindingResult)
	        throws Exception {
		log.debug("onSubmit(): entered...");

		getValidator().validate(form, bindingResult);

		if (bindingResult.hasErrors() && form.getMode() != FormMode.CANCEL) {
			return resolveFormView(request);
		}

		switch (form.getMode()) {
			case ADD:
			case MODIFY: {
				if (form.getSourceId() != null) {
					try {
						sourceRepository.saveSourceAddress(form.getSourceId(), form.getAddress());
					} catch (DuplicateAddressTypeException de) {
						bindingResult.reject (null, de.getMessage());
						return resolveFormView(request);
					}
				} else if (form.getContactId() != null) {
					//TODO SOURCE - maybe create method in repository
					Address address = sourceRepository.saveRequiresNew(form.getAddress());
					Contact ic = sourceRepository.find(Contact.class, form.getContactId());
					ic.setAddress(address);
					sourceRepository.saveRequiresNew (ic);
				}

				break;
			}

			case REMOVE: {
				if (form.getSourceId() != null) {
					//TODO SOURCE - maybe create method in repository
					SourceAddress sa = sourceRepository.find(SourceAddress.class, new SourceAddressPK(form.getSourceId(), form.getAddress().getId()));
					sourceRepository.removeRequiresNew(sa);
					sourceRepository.removeRequiresNew(form.getAddress());
				} else if (form.getContactId() != null) {
					//TODO SOURCE - maybe create method in repository
					Contact contact = sourceRepository.find (Contact.class, form.getContactId());
					Address sa = contact.getAddress();
					contact.setAddress(null);
					sourceRepository.saveRequiresNew(contact);
					sourceRepository.removeRequiresNew(sa);
				}

				break;
			}
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
