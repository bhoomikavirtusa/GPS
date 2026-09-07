package com.wiley.permissions.web.shared.controllers.sources;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import com.wiley.permissions.domain.persistence.permissions.Country;
import com.wiley.permissions.domain.persistence.permissions.PermissionType;
import com.wiley.permissions.domain.persistence.permissions.Source;
import com.wiley.permissions.domain.persistence.permissions.SourceFile;
import com.wiley.permissions.repositories.SourceRepository;
import com.wiley.permissions.services.AssetService;
import com.wiley.permissions.services.SourceService;
import com.wiley.permissions.web.shared.PermUserContext;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;
import com.wiley.permissions.web.shared.controllers.BaseFormBean.FormMode;
import com.wiley.permissions.web.shared.controllers.sources.ManageSourceForm.ManagementMode;
import com.wiley.sf.common.lang.ArgUtil;

/**
 *
 * @author ttidwell
 */
@Controller
/*@RequestMapping("/sources/manageSource")*/
@RequestMapping
// TODO: Try taking form off session and see what happens.
// Would be better not to use session and probably don't need to.
@SessionAttributes(ManageSourceController.MODEL_FORM_NAME)
public class ManageSourceController
extends BaseAnnotatedController
{
	private final static Log log = LogFactory.getLog(ManageSourceController.class);

	// when compile on command line requires this not to be private or won't compile
	// (since used above class definition)
	protected static final String MODEL_FORM_NAME = "manageSourceForm";

	private SourceService sourceService;
	private AssetService assetService;

	private SourceRepository sourceRepository;

	// default can be overridden in Spring config
	private long maxFileSize = 10 * 1024 * 1024;


	@Override
	protected List<String> getReferenceDataNames(HttpServletRequest request)
	throws Exception
	{
		List<String> output = new ArrayList<String>();

		output.add("countries");
		output.add("deliveryMethods");

		return output;
	}

	@Override
	@ModelAttribute
	public void referenceData(Model model, HttpServletRequest request)
	throws Exception
	{
		log.debug("referenceData(): entered...");
		super.referenceData(model, request);

		model.addAttribute("permissionTypes", PermissionType.VALID_VALUES);
	}

	@RequestMapping(value="/sources/manageSource/form", method = {RequestMethod.GET, RequestMethod.POST})
	@SuppressWarnings("incomplete-switch")
	public ModelAndView form(HttpServletRequest request)
	throws Exception
	{
		log.debug("form(): entering...");

		//log.debug("formBackingObject(): request params: " + StringUtil.mapToString(request.getParameterMap(), "\n@"));

		ManageSourceForm form = new ManageSourceForm();

		FormMode mode = (FormMode) request.getAttribute("mode");

		if (mode == null) {
			mode = FormMode.valueOf(request.getParameter("mode"));
		}

		if (mode == null) {
			mode = FormMode.ADD;
		}

		form.setMode(mode);

		ManagementMode mMode = (ManagementMode) request.getAttribute("managementMode");

		if (mMode != null) {
			form.setManagementMode(mMode);
		}

		Source source = null;

		log.debug("form(): mode = " + mode);

		switch (mode) {
			case ADD: {
				source = new Source();

				// default profile
				String countryCode = PermUserContext.getUserSession(request).getCurrentUser().getCountryCode();
				source.setCountry((Country) getReferenceDataCache().getObjectByType(
					Country.class, countryCode));

				String test = request.getParameter("assetId");

				if (StringUtils.isNotBlank(test)) {
					form.setAssetId(new Integer(test));
				}

				if (form.getAssetId() == null) {
					// This is in case we were forwarded through the gateway mechanism
					// and the Asset Id is in the attributes.
					form.setAssetId((Integer) request.getAttribute("assetId"));
				}

				break;
			}

			case VIEW:
			case MODIFY:
			case REMOVE: {
				Integer sourceId = (Integer) request.getAttribute(ViewSourceController.SOURCE_ID);
				if (sourceId == null) {
					sourceId = Integer.valueOf(request.getParameter(ViewSourceController.SOURCE_ID));
				}
				source = sourceRepository.loadSourceByIdForView(sourceId, false);
			}
		}

		form.setSource(source);
		form.setOriginalMode(mode);
		form.setCameFrom(request.getParameter("cameFrom"));

		log.info("form(): form.getAssetId() = " + form.getAssetId());

		ModelAndView mv = new ModelAndView(getFormView());
		mv.addObject(MODEL_FORM_NAME, form);
		return mv;
	}

	/*@GetMapping("/submit")*/
	@RequestMapping(value="/sources/manageSource/submit", method = RequestMethod.POST)
	@PreAuthorize("hasAuthority('EMPLOYEE_DEFAULT')")
	@SuppressWarnings("incomplete-switch")
	public String onSubmit(HttpServletRequest request,
			@ModelAttribute(MODEL_FORM_NAME) ManageSourceForm form, BindingResult bindingResult)
	throws Exception
	{
		log.debug("onSubmit(): FormMode: " + form.getMode());

		getValidator().validate(form, bindingResult);

		if (bindingResult.hasErrors() && form.getMode() != FormMode.CANCEL) {
			return getFormView();
		}

		Source source = form.getSource();

		switch (form.getMode()) {
			case ADD:
			case MODIFY: {
				if (form.getMode() == FormMode.MODIFY) {
					// This is protection against doing a create when we want to modify,
					// since saveSource() below will do a create if any of these
					// id's are null.
					ArgUtil.notBlank(source.getExternalId(), "source.externalId");
					ArgUtil.notNull(source.getId(), "source.id");
				}
				source.setLastUpdatedDate(new java.util.Date());//Added By santhosh for REQ0376879
				source = sourceService.saveSource(source, true);

				if (form.getMode() == FormMode.ADD && form.getAssetId() != null) {
					assetService.addSourceToAsset(form.getAssetId(), source.getId());
				}

				break;
			}

			case CANCEL: {
				break;
			}
		}

		form.setManagementMode(ManagementMode.GENERAL);
		return createSuccessView(form, source);
	}

	private String createSuccessView(ManageSourceForm form, Source source) {
		String viewName = getSuccessView() + "&" + ViewSourceController.SOURCE_ID + "=" + source.getId();

		if (StringUtils.isNotBlank(form.getCameFrom())) {
			viewName += "&cameFrom=" + form.getCameFrom();
		}

		log.debug("createSuccessView(): Going to viewName: " + viewName);
		return viewName;
	}

	@PreAuthorize("hasAuthority('EMPLOYEE_DEFAULT')")
	/*@GetMapping("/submitFile")*/
	@RequestMapping(value="/sources/manageSource/submitFile", method = RequestMethod.POST)
	@SuppressWarnings("incomplete-switch")
	public ModelAndView onSubmitFile(HttpServletRequest request,
			@ModelAttribute(MODEL_FORM_NAME) ManageSourceForm form, BindingResult bindingResult)
		throws Exception
	{
		log.debug("onSubmitFile(): FormMode: " + form.getMode());
		Source source = form.getSource();

		switch (form.getMode()) {
			case ADD_CHILD: {
				MultipartFile mpf = form.getNewMultipartFile();

				if (mpf != null) {
					if (mpf.getSize() > 0 && mpf.getSize() <= maxFileSize) {
						ArgUtil.notNull(source.getId(), "source.id");
						source = sourceRepository.loadSourceByIdForView(source.getId(), false);
						// reset the source on the form because we've changed the object ref
						form.setSource(source);

						SourceFile newSourceFile = form.getNewFile();

						newSourceFile.setFileData(mpf.getBytes());
						newSourceFile.setFileName(mpf.getOriginalFilename());
						newSourceFile.setMimeType(mpf.getContentType());
						newSourceFile.setSource(source);

						log.debug("onSubmitFile(): File has " + mpf.getSize() + " bytes");

						sourceService.createSourceFile(newSourceFile);

						// note this next line doesn't do anything with the DB since the source
						// object is not attached to a persistent context (here) - just so the UI has the new file
						source.getFiles().add(newSourceFile);

						form.setNewMultipartFile(null);
						form.setNewFile(new SourceFile());
					} else if (mpf.getSize() > maxFileSize) {
						log.info("onSubmitFile(): File TOO LARGE - has " + mpf.getSize() + " bytes");

						bindingResult.rejectValue("newMultipartFile",
								"sources.error.newFile.tooLarge");
					} else {
						bindingResult.rejectValue("newMultipartFile",
								"sources.error.newFile.empty",
								new Object[] { maxFileSize }, "GET");
					}
				}

				break;
			}

			case REMOVE_CHILD: {
				int fileId = form.getFileToRemove();
				log.debug("removing source file with id = " + fileId);
				sourceService.deleteSourceFile(fileId);
				// equals() and hashCode() based only on id
				SourceFile dummy = new SourceFile(fileId);
				source.getFiles().remove(dummy);

				ArgUtil.notNull(source.getId(), "source.id");
				source = getSourceRepository().loadSourceByIdForView(source.getId(), false);
				// reset the source on the form because we've changed the object ref
				form.setSource(source);

				break;
			}

			case VIEW: {
				form.setManagementMode(ManagementMode.GENERAL);
				return new ModelAndView(createSuccessView(form, source));
			}
		}

		form.setManagementMode(ManagementMode.FILES);
		ModelAndView mv = new ModelAndView(getFormView());
		mv.addObject(MODEL_FORM_NAME, form);
		return mv;
	}

	@PreAuthorize("hasAuthority('EMPLOYEE_DEFAULT')")
	/*@GetMapping("/noflyList")*/
	@RequestMapping(value="/sources/manageSource/noflyList", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView noflyList(HttpServletRequest request,
			@RequestParam(value="cameFrom", required=false) String cameFrom,
			@RequestParam(value="cwId", required=false) String cwId)
		throws Exception
	{
		log.debug("noflyList(): cameFrom = " + cameFrom + ", cwId = " + cwId);

		ModelAndView mv = new ModelAndView("pages.sources.sources.nofly.List");
		List <Source> sources = getSourceRepository().loadDisabledSources();
		mv.addObject("sources",sources);
		if (null != cameFrom && cameFrom.equals("landing")) {
			mv.addObject("nextUrl", "/sapp/cwlanding/scroll?cwId=" + cwId);
		} else {
			mv.addObject("nextUrl","/sapp/product/userLanding/main");
		}
		return mv;
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

	public long getMaxFileSize() {
		return maxFileSize;
	}

	public void setMaxFileSize(long maxFileSize) {
		this.maxFileSize = maxFileSize;
	}

	public SourceRepository getSourceRepository() {
		return sourceRepository;
	}

	public void setSourceRepository(SourceRepository sourceRepository) {
		this.sourceRepository = sourceRepository;
	}
}
