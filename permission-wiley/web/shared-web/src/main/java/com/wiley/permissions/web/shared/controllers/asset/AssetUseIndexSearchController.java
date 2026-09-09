package com.wiley.permissions.web.shared.controllers.asset;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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

import com.wiley.permissions.domain.persistence.permissions.CommonWork;
import com.wiley.permissions.repositories.AssetUseRepository;
import com.wiley.permissions.services.AssetSearchForm;
import com.wiley.permissions.services.AssetUseIndexService;
import com.wiley.permissions.services.AssetUseSearchResults;
import com.wiley.permissions.web.shared.PermUserContext;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;

/**
 * Note need the SessionAttributes annotation in addition to our
 * manual session code for when the user clicks on a JMesa column
 * in the results to sort.
 *
 * @version $Id: AssetUseIndexSearchController.java,v 1.3.2.1 2017-10-06 13:56:34 sdevadasan Exp $
 * @author smarkoff
 */
@Controller
@SessionAttributes("assetSearchForm")
public class AssetUseIndexSearchController extends BaseAnnotatedController {
	private static final Log log = LogFactory.getLog(AssetUseIndexSearchController.class);

	// constant also used by ReportsController
	public static final String MODEL_FORM_NAME = "assetSearchForm";

	private AssetUseIndexService assetUseIndexService;
	private AssetUseRepository assetUseRepository;

	@Override
	protected List<String> getReferenceDataNames(HttpServletRequest request) throws Exception
	{
		List<String> output = new ArrayList<String>();

		output.add("publicationStatusList");

		output.add("mediaTypes");
		output.add("ownerTypes");

		output.add("statuses");
		output.add("usages");
		output.add("sizes");
		output.add("modelReleases");

		//Start: Added for DM-280
		output.add("businessUnits");
		output.add("mediums");
		//End: Added for DM-280

		return output;
	}

	@RequestMapping(value = "/asset/assetSearch", method = RequestMethod.GET)
	public String formBackingObject(HttpServletRequest request,	Model model)
	throws Exception
	{
		log.debug("formBackingObject(): entered...");

		HttpSession session = request.getSession();
		AssetSearchForm form = (AssetSearchForm) session.getAttribute(MODEL_FORM_NAME);
		if (form == null)  form = new AssetSearchForm();

		CommonWork commonWork = PermUserContext.getCurrentCommonWork(request);
		if (commonWork == null) {
			form.setIncludeCommonWorkId(false);
		} else {
			form.setCommonWorkId(commonWork.getId());
		}

		model.addAttribute(MODEL_FORM_NAME, form);

		return getFormView();
	}

	@RequestMapping(value = "/asset/assetSearch", method = RequestMethod.POST)
	public ModelAndView onSubmit(HttpServletRequest request,
			@ModelAttribute(MODEL_FORM_NAME) AssetSearchForm form,
			BindingResult bindingResult)
	throws Exception
	{
		log.debug("onSubmit(): entered...");

		// currently we don't have any validation configured for this form
		// but there is no harm in calling validate() - it just won't do anything.
		getValidator().validate(form, bindingResult);

		if (!form.isSomethingSpecified()) {
		    bindingResult.reject(null, "You must specify at least one search criteria.");
		}

        if (bindingResult.hasErrors()) {
        	return new ModelAndView(getFormView());
        }
        else {
        	AssetUseSearchResults results = null;
        	// will get parse exception by lucene parser when searched with only "-"
        	try {
        		results = assetUseIndexService.searchIndex(form);
			} catch (Exception e) {
				log.error("cron(): caught exception: ", e);
				/* bindingResult.reject(null, "You must specify a valid search criteria");
				 return new ModelAndView(getFormView());*/
			}

        	Set<Integer> assetIdSet = null;
        	if (form.getCommonWorkId() != null) {
        		assetIdSet = assetUseRepository.getCommonWorkRepository().loadAssetIdsForCWIdAsSet(form.getCommonWorkId());
        	}
        	results.setAssetIdSetForCurrentCW(assetIdSet);

        	ModelAndView mv = new ModelAndView(getSuccessView());
        	mv.addObject("results", results);
        	mv.addObject(MODEL_FORM_NAME, form);

        	HttpSession session = request.getSession();
        	session.setAttribute(MODEL_FORM_NAME, form);

            return mv;
        }
	}

	public AssetUseIndexService getAssetUseIndexService() {
		return assetUseIndexService;
	}

	public void setAssetUseIndexService(AssetUseIndexService assetUseIndexService) {
		this.assetUseIndexService = assetUseIndexService;
	}

	public AssetUseRepository getAssetUseRepository() {
		return assetUseRepository;
	}

	public void setAssetUseRepository(AssetUseRepository assetUseRepository) {
		this.assetUseRepository = assetUseRepository;
	}
}
