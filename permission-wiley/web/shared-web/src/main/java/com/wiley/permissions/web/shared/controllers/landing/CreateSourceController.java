package com.wiley.permissions.web.shared.controllers.landing;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections.CollectionUtils;
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
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import com.wiley.permissions.domain.persistence.permissions.AddressType;
import com.wiley.permissions.domain.persistence.permissions.Contact;
import com.wiley.permissions.domain.persistence.permissions.Country;
import com.wiley.permissions.domain.persistence.permissions.PermissionType;
import com.wiley.permissions.domain.persistence.permissions.Source;
import com.wiley.permissions.domain.persistence.permissions.SourceAddress;
import com.wiley.permissions.domain.persistence.permissions.SourceFile;
import com.wiley.permissions.repositories.SourceRepository;
import com.wiley.permissions.services.SourceService;
import com.wiley.permissions.web.shared.PermUserContext;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;
import com.wiley.sf.common.lang.ArgUtil;


@Controller
@SessionAttributes(CreateSourceController.FORM_MODEL_NAME)
public class CreateSourceController extends BaseAnnotatedController {
	private static final Log log = LogFactory.getLog(CreateSourceController.class);

	public static final String FORM_MODEL_NAME = "createSourceForm";

	private static final String FORM_VIEW = "pages.sources.include.createSource";
	private static final String SUCCESS_CUSTOM = "redirect:/sapp/asset/custom/submit";
	private static final String SUCCESS_LANDING_CREATE = "redirect:/sapp/landing/source/submit_3rdparty_search";
	private static final String SUCCESS_LANDING_EDIT = "redirect:/sapp/permissions/po/assets";

	private SourceService sourceService = null;
	private SourceRepository sourceRepository = null;

	// default can be overridden in Spring config
	private final long maxFileSize = 10 * 1024 * 1024;

	/**
	 * Spring 5 registers @RequestMapping on every bean instance, so only one
	 * CreateSourceController bean may exist. Resolve successView from the request
	 * path instead of separate beans with different successView values.
	 */
	private String resolveSuccessView() {
		String path = currentServletPath();
		if (path != null) {
			if (path.contains("/custom/createSource")) {
				return SUCCESS_CUSTOM;
			}
			if (path.contains("/landing/editSource")) {
				return SUCCESS_LANDING_EDIT;
			}
		}
		return SUCCESS_LANDING_CREATE;
	}

	private String currentServletPath() {
		try {
			ServletRequestAttributes attrs =
					(ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
			if (attrs != null) {
				HttpServletRequest request = attrs.getRequest();
				String uri = request.getRequestURI();
				String context = request.getContextPath();
				if (uri != null && context != null && uri.startsWith(context)) {
					return uri.substring(context.length());
				}
				return uri;
			}
		} catch (Exception e) {
			log.debug("currentServletPath(): unable to resolve request path", e);
		}
		return null;
	}

	@Override
	protected List<String> getReferenceDataNames(HttpServletRequest request) throws Exception
	{
		List<String> output = new ArrayList<String>();

		output.add("countries");
		output.add("deliveryMethods");
		output.add("addressTypes");
		return output;
	}

	@Override
	@ModelAttribute
	public void referenceData(Model model, HttpServletRequest request)
	throws Exception
	{
		super.referenceData(model, request);
		model.addAttribute("permissionTypes", PermissionType.VALID_VALUES);
	}

	@RequestMapping(value = {"/custom/createSource", "/landing/createSource", "/landing/editSource"},
			method = RequestMethod.GET)
	public String formBackingObject(HttpServletRequest request,
			@RequestParam(value="sourceId", required=false) Integer sourceId,
			Model model)
	throws Exception
	{
		log.debug("formBackingObject(): entered...");

		CreateSourceForm form = new CreateSourceForm();
		if (null != sourceId) {
			Source source = sourceRepository.lazyLoad(Source.class, sourceId, new String[] {"contacts"});
			form.setSource(source);
			SourceAddress ea = sourceRepository.loadSourceAddressOfType(source.getId(), AddressType.MAIN);
			if (null != ea)
				form.setAddress(ea.getAddress());
			List<Contact> entc = source.getContacts();
			// load first contact
			if (CollectionUtils.isNotEmpty(entc)) {
				form.setContact(entc.get(0));
			}
		} else {
			// default to user profile
			String countryCode = PermUserContext.getUserSession(request).getCurrentUser().getCountryCode();
			form.getSource().setCountry((Country) getReferenceDataCache().getObjectByType(
					Country.class, countryCode));
		}
		model.addAttribute(FORM_MODEL_NAME, form);

		return FORM_VIEW;
	}

	// request is expected to always be POST
	@RequestMapping(value = {"/custom/createSource", "/landing/createSource", "/landing/editSource"},
			method = RequestMethod.POST)
	public ModelAndView onSubmit(HttpServletRequest request,
			@ModelAttribute(FORM_MODEL_NAME) CreateSourceForm form,
			BindingResult bindingResult)
	throws Exception
	{
		log.debug("onSubmit(): entered...");

		Source source = form.getSource();
		log.debug("onSubmit(): sourceId: " + source.getId());
		getValidator().validate(form, bindingResult);

		if (bindingResult.hasErrors()) {
			// lnagy - to be improved here
			ModelAndView mv = new ModelAndView("dialog.success");
			mv.addObject("message", bindingResult.getAllErrors());
			return mv;
		}

		source = sourceService.saveSource(source, true);

		MultipartFile mpf = form.getMultipartFile();
		if (mpf != null) {
			if (mpf.getSize() > 0 && mpf.getSize() <= maxFileSize) {
				ArgUtil.notNull(source.getId(), "source.id");

				SourceFile file = new SourceFile();
				file.setFileData(mpf.getBytes());
				file.setFileName(mpf.getOriginalFilename());
				file.setMimeType(mpf.getContentType());
				file.setSource(source);

				log.debug("onSubmit(): File has " + mpf.getSize() + " bytes");

				sourceService.createSourceFile(file);
			} else if (mpf.getSize() > maxFileSize) {
				log.info("onSubmit(): File TOO LARGE - has " + mpf.getSize() + " bytes");
				
				bindingResult.rejectValue("agreementFile",
						"sources.error.newFile.tooLarge");
			}
		}

		if (form.getAddress().isNotBlank()) {
			sourceRepository.saveSourceAddress(source.getId(), form.getAddress());
		}

		if (form.getContact().isNotBlank()) {
			form.getContact().setSource(source);

			if (form.getContactAddress().isNotBlank()) {
				form.setContactAddress(sourceRepository.saveRequiresNew(form.getContactAddress()));
				form.getContact().setAddress(form.getContactAddress());
			}
			sourceRepository.saveRequiresNew(form.getContact());
		}
		// return to addSource
		return new ModelAndView(resolveSuccessView() + "?sourceId=" + source.getId());
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
