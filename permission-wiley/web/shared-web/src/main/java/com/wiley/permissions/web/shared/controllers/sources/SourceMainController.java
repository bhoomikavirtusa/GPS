package com.wiley.permissions.web.shared.controllers.sources;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.ModelAndView;

import com.wiley.permissions.domain.persistence.permissions.Source;
import com.wiley.permissions.repositories.SourceRepository;
import com.wiley.permissions.services.AssetUseIndexService;
import com.wiley.permissions.services.SourceService;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;
import com.wiley.permissions.web.shared.controllers.BaseFormBean.FormMode;

/**
 * I think the only thing we really need in the session is "cameFrom"
 * (smarkoff).
 *
 * @author ttidwell
 */
@Controller
@SessionAttributes(SourceMainController.FORM_MODEL_NAME)
/*@RequestMapping("/sources/sources")*/
@RequestMapping
public class SourceMainController extends BaseAnnotatedController {
	private final static Log log = LogFactory.getLog(SourceMainController.class);

	protected final static String FORM_MODEL_NAME = "sourceMainForm";

	private SourceRepository sourceRepository;

	private SourceService sourceService;
	private AssetUseIndexService assetUseIndexService;

	private String viewSourceURL = null;
	private String addSourceURL = null;
	private String redirectURL = null;

	/**
	 * If don't have this method than complains that "sourceMainForm"
	 * (FORM_MODEL_NAME) is not in the session when the handle method is called
	 * (smarkoff).
	 */
	@ModelAttribute(FORM_MODEL_NAME)
	public SourceMainForm initForm()
	{
		log.debug("initForm(): called");
		return new SourceMainForm();
	}

	@RequestMapping(value="/sources/sources/remove", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView remove(
			@RequestParam(value = "selectedSourceExtId", required = true) String sourceExtId)
			throws Exception
	{
		log.debug("remove(): entered...for source external id " + sourceExtId);

		ModelAndView mv = new ModelAndView(getRedirectURL());

		try {
			sourceService.deleteSourceByExternalId(sourceExtId, true);
		}
		catch (Exception e) {
			mv.addObject("generalMessage", e.getMessage());
		}

		return mv;
	}

	/*@GetMapping("/simpleList")*/
	@RequestMapping(value="/sources/sources/simpleList", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView simpleList(@RequestParam(value = "searchTerm", required = false) String filter)
			throws Exception
	{
		log.debug("simpleList(): entered, filter = " + filter);

		ModelAndView mv = new ModelAndView("pages.sources.simple.sources.main");

		// Don't need to pass sourceList to jsp unless switch back to client side table
		//mv.addObject("sourceList", getSourceRepository().loadSourceSummaryListWithDisabledFlag(filter));

		return mv;
	}

	/*@GetMapping("/list")*/
	@RequestMapping(value="/sources/sources/list", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView list() throws Exception
	{
		log.debug("list(): entered...");

		ModelAndView mv = new ModelAndView(getFormView());

		// Don't need to pass sourceList to jsp unless switch back to client side table
		//mv.addObject("sourceList", getSourceService().loadSourceSummaryListWithDisabledFlag(null));

		return mv;
	}

	// lnagy - this method I don't think it is used anymore
	/*@GetMapping("/view")*/
	@RequestMapping(value="/sources/sources/view", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView view(@RequestParam(value = "selectedSourceExtId", required = true) String sourceExtId)
			throws Exception
	{
		log.debug("view(): entered..., selectedSourceExtId = " + sourceExtId);

		ModelAndView mv = new ModelAndView("forward:" + viewSourceURL);

		FormMode newMode = FormMode.VIEW;

		Source source = sourceRepository.loadSourceByExternalId(sourceExtId);
		// The only reasonable time that an invalid externalId might be passed
		// is an automated load test.
		if (source == null) {
			throw new RuntimeException("Source not found for externalId: " + sourceExtId);
		}

		log.debug("view(): Found Source Ext. Id " + sourceExtId);
		log.debug("view(): Source Id Came Back: " + source.getId());

		mv.addObject(ViewSourceController.SOURCE_ID, source.getId());

		mv.addObject("mode", newMode);

		return mv;
	}

	// lnagy - this method I don't think it is used anymore
	/*@GetMapping("/create")*/
	@RequestMapping(value="/sources/sources/create", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView create(@RequestParam(value = "cameFrom", required = false) String cameFrom,
			@RequestParam(value = "create", required = false) String create) throws Exception
	{
		log.debug("create(): entered...cameFrom [" + cameFrom + "] create [" + create + "]");

		FormMode newMode = FormMode.ADD;

		String redirectURL = addSourceURL;

		if (StringUtils.isNotBlank(cameFrom)) {
			redirectURL += "?cameFrom=" + cameFrom;
		}

		ModelAndView mv = new ModelAndView("forward:" + redirectURL);

		mv.addObject("fresh", "true");
		mv.addObject("mode", newMode);

		return mv;
	}

	/*@GetMapping("/resetDisabled")*/
	@RequestMapping(value="/sources/sources/resetDisabled", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView resetDisabled(@RequestParam(value = "selectedSourceExtId", required = true)
		String selectedSourceExtId) throws Exception
	{
		log.debug("resetDisabled(): entered..., selectedSourceExtId = " + selectedSourceExtId);

		sourceRepository.resetDisabledSourceFlag(selectedSourceExtId);

		assetUseIndexService.updateIndexForSourceInSeparateThread(selectedSourceExtId);

		ModelAndView mv = new ModelAndView(getRedirectURL() + "?activateMode=1");

		return mv;
	}

	public AssetUseIndexService getAssetUseIndexService() {
		return assetUseIndexService;
	}

	public void setAssetUseIndexService(AssetUseIndexService assetUseIndexService) {
		this.assetUseIndexService = assetUseIndexService;
	}

	public String getViewSourceURL() {
		return viewSourceURL;
	}

	public void setViewSourceURL(String viewSourceURL) {
		this.viewSourceURL = viewSourceURL;
	}

	public String getAddSourceURL() {
		return addSourceURL;
	}

	public void setAddSourceURL(String addSourceURL) {
		this.addSourceURL = addSourceURL;
	}

	public String getRedirectURL() {
		return redirectURL;
	}

	public void setRedirectURL(String redirectURL) {
		this.redirectURL = redirectURL;
	}

	public SourceRepository getSourceRepository() {
    	return sourceRepository;
    }

	public void setSourceRepository(SourceRepository sourceRepository) {
    	this.sourceRepository = sourceRepository;
    }

	public SourceService getSourceService() {
    	return sourceService;
    }

	public void setSourceService(SourceService sourceService) {
    	this.sourceService = sourceService;
    }
}
