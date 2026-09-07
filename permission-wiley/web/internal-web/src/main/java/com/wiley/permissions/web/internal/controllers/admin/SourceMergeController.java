package com.wiley.permissions.web.internal.controllers.admin;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.ModelAndView;

import com.wiley.permissions.domain.persistence.permissions.Asset;
import com.wiley.permissions.domain.persistence.permissions.AssetUse;
import com.wiley.permissions.domain.persistence.permissions.Source;
import com.wiley.permissions.repositories.SourceRepository;
import com.wiley.permissions.services.AssetUseIndexService;
import com.wiley.permissions.services.util.ServiceException;
import com.wiley.permissions.web.shared.PermUserContext;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;

@Controller
/*@RequestMapping("/admin/mergeSource/")*/
@RequestMapping
@SessionAttributes(SourceMergeController.FORM_MODEL_NAME)
public class SourceMergeController extends BaseAnnotatedController {

	// --------------------- static data ---------------------------------

	private final static Log log = LogFactory.getLog(SourceMergeController.class);
	protected final static String FORM_MODEL_NAME = "SourceMergeForm";

	// --------------------- instance data -------------------------------

	private SourceRepository sourceRepository;
	private AssetUseIndexService assetUseIndexService;

	@Override
	protected List<String> getReferenceDataNames(HttpServletRequest request) throws Exception
	{
		List<String> output = new ArrayList<String>();

		output.add("sizes");
		output.add("pagePositions");
		output.add("mediaTypes");

		return output;
	}

	@RequestMapping(value = "/admin/mergeSource/merge_search", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView sourceMergeSearch()
	throws Exception
	{
		log.debug("sourceMergeSearch(): entered...");

		ModelAndView mv = new ModelAndView("pages.admin.source.merge");
		SourceMergeForm form = new SourceMergeForm();
		mv.addObject("formMessage", "Select the source you want to merge from and the source you want to merge into");
		form.setFromSourceId(-1);
		form.setToSourceId(-1);
		form.setSourceFrom("");
		form.setSourceTo("");
		mv.addObject(FORM_MODEL_NAME, form);
		return mv;
	}

	@RequestMapping(value = "/admin/mergeSource/merge_search", method = RequestMethod.POST)
	public ModelAndView processForm(HttpServletRequest request,
			@ModelAttribute(FORM_MODEL_NAME) SourceMergeForm form,
			BindingResult bindingResult)
	throws Exception
	{
		log.debug("processForm(): entered...");

		Integer userId = PermUserContext.getCurrentUserId(request);

		Source fromSource = getSourceRepository().loadSourceById(form.getFromSourceId());
		String externalId = fromSource.getExternalId();
		fromSource = null;

		ModelAndView mv = new ModelAndView("pages.admin.source.merge");

		String dmsg = "";

		if (form.getFromSourceId() < 1 || StringUtils.isBlank(form.getSourceFrom())) {
			dmsg = "You must select a valid From Source";
			mv = new ModelAndView("pages.admin.source.merge");
			form.setFromSourceId(-1);
		} else if (form.getToSourceId() < 1 || StringUtils.isBlank(form.getSourceTo())) {
			dmsg = "You must select a valid To Source";
			mv = new ModelAndView("pages.admin.source.merge");
			form.setToSourceId(-1);
		}

		List<Integer> previousAssetIds = new ArrayList<Integer>();
		if (dmsg.equals("")) {
			// first move data to the new source
			try {
				// first save the ids of the existing assets in the destination source so we can update only the index of the transfered assets
				Source wkSource = getSourceRepository().loadSourceByIdForMerge(form.getToSourceId());
				Iterator <Asset> iterOld = wkSource.getAssets().iterator();
				while (iterOld.hasNext()) {
					Asset wkAsset = iterOld.next();
					previousAssetIds.add(wkAsset.getId());
				}

				getSourceRepository().mergeSources(form.getFromSourceId(), form.getToSourceId(), userId);
				// now delete old source
				// now update search index for all assets in the destination source
				Source destinationSource = getSourceRepository().loadSourceByIdForMerge(form.getToSourceId());

				Iterator <Asset> iter = destinationSource.getAssets().iterator();
				while(iter.hasNext()) {
					Asset dAsset = iter.next();
					// if the asset was not transfered then we do not need to build the index
					if(previousAssetIds.contains(dAsset.getId())) continue;
					Iterator <AssetUse> auIter = dAsset.getAssetUses().iterator();
					while (auIter.hasNext()) {
						AssetUse au = auIter.next();
						assetUseIndexService.updateIndex(au.getId());
					}
				}
				dmsg = "Sources succesfully merged";
			} catch (ServiceException e) {
				dmsg = e.getMessage();
			}

			form.setFromSourceId(-1);
			form.setToSourceId(-1);
			form.setSourceFrom("");
			form.setSourceTo("");
		}

		mv.addObject("formMessage", dmsg);
		mv.addObject(FORM_MODEL_NAME, form);
		return mv;
	}

	@RequestMapping(value = "/admin/mergeSource/merge_details", method = RequestMethod.POST)
	public ModelAndView mergeDetails(HttpServletRequest request,
			@ModelAttribute(FORM_MODEL_NAME) SourceMergeForm form,
			BindingResult bindingResult)
	throws Exception
	{
		log.debug("mergeDetails(): entered...");

		ModelAndView mv = new ModelAndView("pages.admin.source.merge.details");

		Source fromSource = null;
		Source toSource =  null;
		Integer mergeCount = 0;
		if (form.getFromSourceId() != -1) {
		//	fromSource = getSourceRepository().lazyLoad(Source.class, form.getFromSourceId(), new String[] { "assets","sourceGroup","contracts", "contacts"  });
		//	fromSource = getSourceRepository().lazyLoad(Source.class, form.getFromSourceId(), new String[] { "assets","sourceGroup"  });
			fromSource = getSourceRepository().getSourceMergeView(form.getFromSourceId());
			mv.addObject("fromSource", fromSource);
			Integer count1 = getSourceRepository().assetCountForSource(fromSource.getId());
			mergeCount = mergeCount + count1;
			if (null != fromSource.getAssets()) mv.addObject("fromSourceAssets", count1);
			if (null != fromSource.getContracts()) mv.addObject("fromSourceContracts", fromSource.getContracts().size());
			if (null != fromSource.getContacts()) mv.addObject("fromSourceContacts", fromSource.getContacts().size());
		}
		if (form.getToSourceId() != -1) {
	//		toSource = getSourceRepository().lazyLoad(Source.class, form.getToSourceId(), new String[] { "assets","sourceGroup","contracts","contacts" });
			// toSource = getSourceRepository().lazyLoad(Source.class, form.getToSourceId(), new String[] { "assets","sourceGroup"});
			toSource = getSourceRepository().getSourceMergeView(form.getToSourceId());
			mv.addObject("toSource", toSource);
			Integer count2 = getSourceRepository().assetCountForSource(toSource.getId());
			mergeCount = mergeCount + count2;
			if (null != toSource.getAssets()) mv.addObject("toSourceAssets", getSourceRepository().assetCountForSource(toSource.getId()));
			if (null != toSource.getContracts()) mv.addObject("toSourceContracts", toSource.getContracts().size());
			if (null != toSource.getContacts()) mv.addObject("toSourceContacts", toSource.getContacts().size());
		}

		if (null != fromSource && null != toSource && fromSource.getId() != toSource.getId()) {
			if ((null == fromSource.getSourceGroup() && null == toSource.getSourceGroup())
				|| (null != fromSource.getSourceGroup() && null != toSource.getSourceGroup() &&
						fromSource.getSourceGroup().getId() == toSource.getSourceGroup().getId()))
			{
				Source finalSource = (Source) BeanUtils.cloneBean(toSource);
				if (!finalSource.isNofly() && fromSource.isNofly()) {
					finalSource.setNofly(true);
				}
				if (finalSource.getCountry() == null) {
					finalSource.setCountry(fromSource.getCountry());
				}
				if (StringUtils.isBlank(finalSource.getCreditLine())) {
					finalSource.setCreditLine(fromSource.getCreditLine());
				}
				if (finalSource.getFaxNumber() == null) {
					finalSource.setFaxNumber(fromSource.getFaxNumber());
				}
				if (finalSource.getJdeVendorNumber() == null) {
					finalSource.setJdeVendorNumber(fromSource.getJdeVendorNumber());
				}
				if (finalSource.getNoFlyPhotographerLastName() == null) {
					finalSource.setNoFlyPhotographerLastName(fromSource.getNoFlyPhotographerLastName());
				}
				if (finalSource.getPermissionRequestUrl() == null) {
					finalSource.setPermissionRequestUrl(fromSource.getPermissionRequestUrl());
				}
				if (finalSource.getPhoneNumber() == null) {
					finalSource.setPhoneNumber(fromSource.getPhoneNumber());
				}
				if (finalSource.getSourceGroup() == null) {
					finalSource.setSourceGroup(fromSource.getSourceGroup());
				}
				if (finalSource.getWebsite() == null) {
					finalSource.setWebsite(fromSource.getWebsite());
				}
				if (StringUtils.isBlank(finalSource.getComment())) {
					finalSource.setComment(fromSource.getComment());
				}
				finalSource.getAssets().addAll(fromSource.getAssets());
				finalSource.getContracts().addAll(fromSource.getContracts());
				finalSource.getContacts().addAll(fromSource.getContacts());
				mv.addObject("finalSource", finalSource);
				if (null != finalSource.getAssets()) mv.addObject("finalSourceAssets", mergeCount);
				if (null != finalSource.getContracts()) mv.addObject("finalSourceContracts", finalSource.getContracts().size());
				if (null != finalSource.getContacts()) mv.addObject("finalSourceContacts", finalSource.getContacts().size());
			} else {
				mv.addObject("detailsError", "1");
				mv.addObject("errorMessage", "Sources Cannot be merged because they are in different source groups");
			}
		}

		if (null != fromSource && null != toSource && fromSource.getId() == toSource.getId()) {
			mv.addObject("errorMessage", "You cannot merge a source onto itself");
			mv.addObject("detailsError", "1");
		}

		return mv;
	}

	// --------------------- getters and setters --------------------------

	public SourceRepository getSourceRepository() {
		return sourceRepository;
	}

	public void setSourceRepository(SourceRepository sourceRepository) {
		this.sourceRepository = sourceRepository;
	}

	public AssetUseIndexService getAssetUseIndexService() {
		return assetUseIndexService;
	}

	public void setAssetUseIndexService(AssetUseIndexService assetUseIndexService) {
		this.assetUseIndexService = assetUseIndexService;
	}
}
