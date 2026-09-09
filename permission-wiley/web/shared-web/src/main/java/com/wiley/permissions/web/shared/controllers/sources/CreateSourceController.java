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
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.wiley.permissions.domain.persistence.permissions.Contact;
import com.wiley.permissions.domain.persistence.permissions.Country;
import com.wiley.permissions.domain.persistence.permissions.PermissionType;
import com.wiley.permissions.domain.persistence.permissions.Source;
import com.wiley.permissions.repositories.SourceRepository;
import com.wiley.permissions.services.AssetService;
import com.wiley.permissions.services.SourceService;
import com.wiley.permissions.web.shared.PermUserContext;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;

/**
 * TODO: Possibly merge with ManageSourceController.
 *
 * @author smarkoff
 */
@Controller
public class CreateSourceController extends BaseAnnotatedController {
	private static final Log log = LogFactory.getLog(CreateSourceController.class);

	public static final String FORM_MODEL_NAME = "createSourceForm";

	private String landingView = null;

	private SourceRepository sourceRepository = null;

	private SourceService sourceService = null;
	private AssetService assetService = null;

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

	@RequestMapping(value = "/sources/createSource", method = RequestMethod.GET)
	public String form(HttpServletRequest request,	Model model)
	throws Exception
	{
		log.debug("form(): entered...");

		CreateSourceForm form = new CreateSourceForm();

		// default to user profile country
		String countryCode = PermUserContext.getUserSession(request).getCurrentUser().getCountryCode();
		Country defaultCountry = (Country) getReferenceDataCache().getObjectByType(
				Country.class, countryCode);
		form.getSource().setCountry(defaultCountry);
		form.getAddress().setCountry(defaultCountry);
		form.getContact().getAddress().setCountry(defaultCountry);

		form.setCameFrom(request.getParameter("cameFrom"));

		model.addAttribute(FORM_MODEL_NAME, form);

		return getFormView();
	}

	@RequestMapping(value = "/sources/createSource", method = RequestMethod.POST)
	public String submit(HttpServletRequest request,
			@ModelAttribute(FORM_MODEL_NAME) CreateSourceForm form,
			BindingResult bindingResult)
	throws Exception
	{
		log.debug("submit(): entered...");

		// following code similar to ManageSourceController.onSubmit - ADD section

		Source source = form.getSource();

		// Note this only validates the main source part of the form
		// because we don't want to validate the address or contact if
		// they are completely blank.
		getValidator().validate(form, bindingResult);

		if (bindingResult.hasErrors()) {
			return getFormView();
		}

		source = sourceService.saveSource(source, true);

		// The only time that assetId is null should be when the user is creating a new asset
		if (form.getAssetId() != null) {
			assetService.addSourceToAsset(form.getAssetId(), source.getId());
		}

		if (form.getAddress().isNotBlank()) {
			sourceRepository.saveSourceAddress(source.getId(), form.getAddress());
		}

		Contact contact = form.getContact();
		if (contact.isNotBlank()) {
			contact.setSource(source);

			if (contact.getAddress().isBlank()) {
				contact.setAddress(null);  // don't save blank address
			}
			sourceRepository.saveRequiresNew(form.getContact());
		}

		String viewName = getSuccessView() + "?" + ViewSourceController.SOURCE_ID + "=" + source.getId();
		if (StringUtils.isNotBlank (form.getCameFrom())) {
			// just hardcode redirect to landing for now - no other cameFrom
			viewName = getLandingView();
		}
		return viewName;
	}

	public SourceService getSourceService() {
		return sourceService;
	}

	public void setSourceService(SourceService sourceService) {
		this.sourceService = sourceService;
	}

	public AssetService getAssetService() {
		return assetService;
	}

	public void setAssetService(AssetService assetService) {
		this.assetService = assetService;
	}

	public SourceRepository getSourceRepository() {
    	return sourceRepository;
    }

	public void setSourceRepository(SourceRepository sourceRepository) {
    	this.sourceRepository = sourceRepository;
    }

	public String getLandingView() {
		return landingView;
	}

	public void setLandingView(String landingView) {
		this.landingView = landingView;
	}
}
