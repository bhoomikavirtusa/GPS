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


@SessionAttributes(CreateSourceController.FORM_MODEL_NAME)
public class CreateSourceController extends BaseAnnotatedController {
	private static final Log log = LogFactory.getLog(CreateSourceController.class);

	public static final String FORM_MODEL_NAME = "createSourceForm";

	private SourceService sourceService = null;
	private SourceRepository sourceRepository = null;

	// default can be overridden in Spring config
	private final long maxFileSize = 10 * 1024 * 1024;

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

	@RequestMapping(method = {RequestMethod.GET, RequestMethod.POST})
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

		return getFormView();
	}

	// request is expected to always be POST
	@RequestMapping(method = RequestMethod.POST)
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
		return new ModelAndView(getSuccessView() + "?sourceId=" + source.getId());
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
