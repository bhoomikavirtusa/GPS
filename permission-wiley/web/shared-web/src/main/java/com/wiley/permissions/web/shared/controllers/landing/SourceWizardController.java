package com.wiley.permissions.web.shared.controllers.landing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import com.wiley.permissions.domain.persistence.permissions.Address;
import com.wiley.permissions.domain.persistence.permissions.AddressType;
import com.wiley.permissions.domain.persistence.permissions.Asset;
import com.wiley.permissions.domain.persistence.permissions.AssetFile;
import com.wiley.permissions.domain.persistence.permissions.AssetUse;
import com.wiley.permissions.domain.persistence.permissions.CommonWork;
import com.wiley.permissions.domain.persistence.permissions.CopyrightType;
import com.wiley.permissions.domain.persistence.permissions.FavoriteGroup;
import com.wiley.permissions.domain.persistence.permissions.MasterAgreementDeal;
import com.wiley.permissions.domain.persistence.permissions.MediaType;
import com.wiley.permissions.domain.persistence.permissions.OwnerType;
import com.wiley.permissions.domain.persistence.permissions.Privilege;
import com.wiley.permissions.domain.persistence.permissions.Product;
import com.wiley.permissions.domain.persistence.permissions.PurchaseOrder;
import com.wiley.permissions.domain.persistence.permissions.RenditionType;
import com.wiley.permissions.domain.persistence.permissions.Role;
import com.wiley.permissions.domain.persistence.permissions.Source;
import com.wiley.permissions.domain.persistence.permissions.Usage;
import com.wiley.permissions.domain.persistence.permissions.User;
import com.wiley.permissions.persistence.PersistenceException;
import com.wiley.permissions.repositories.AssetRepository;
import com.wiley.permissions.repositories.AssetUseRepository;
import com.wiley.permissions.repositories.CommonWorkRepository;
import com.wiley.permissions.repositories.ContractRepository;
import com.wiley.permissions.repositories.PurchaseOrderRepository;
import com.wiley.permissions.repositories.SourceRepository;
import com.wiley.permissions.repositories.UserRepository;
import com.wiley.permissions.services.AssetService;
import com.wiley.permissions.services.AssetUseIndexService;
import com.wiley.permissions.services.AssetUseSearchResult;
import com.wiley.permissions.services.AssetUseSearchResults;
import com.wiley.permissions.services.AssetUseService;
import com.wiley.permissions.services.CommonWorkService;
import com.wiley.permissions.services.LandingFilterForm;
import com.wiley.permissions.services.util.ServiceException;
import com.wiley.permissions.services.view.AssetUseTableRowView;
import com.wiley.permissions.web.shared.PermUserContext;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;
import com.wiley.permissions.web.shared.util.GenericFileView;

@Controller
/*@RequestMapping("/landing/source/")*/
@RequestMapping
@SessionAttributes(SourceWizardController.FORM_MODEL_NAME)
public class SourceWizardController extends BaseAnnotatedController {

	// --------------------- static data ---------------------------------

	private final static Log log = LogFactory.getLog(SourceWizardController.class);
	protected final static String FORM_MODEL_NAME = "sourceWizardForm";

	// --------------------- instance data -------------------------------

	private AssetUseRepository assetUseRepository;
	private AssetRepository assetRepository;
	private SourceRepository sourceRepository;
	private UserRepository userRepository;
	private CommonWorkRepository commonWorkRepository;
	private AssetUseService assetUseService;
	private AssetService assetService;
	private AssetUseIndexService assetUseIndexService;
	private PurchaseOrderRepository purchaseOrderRepository;
	private CommonWorkService commonWorkService;
	private ContractRepository contractRepository;


	@Override
	protected List<String> getReferenceDataNames(HttpServletRequest request) throws Exception
	{
		List<String> output = new ArrayList<String>();

		output.add("sizes");
		output.add("pagePositions");
		output.add("mediaTypes");
		output.add("modelReleases");
		output.add("currencies");
		output.add("ownerTypes");
		output.add("mediums");
		output.add("gbpmCategories");
		output.add("addressTypes");
		return output;
	}

	@RequestMapping(value = "/landing/source/multi", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView multi(@RequestParam(value="ids") String ids)
	throws Exception
	{
		String[] assetList = ids.split(",");
		// start with the first au in the list;
		Integer auid = new Integer(assetList[0]);

		ModelAndView mv = start (auid);
		SourceWizardForm form = (SourceWizardForm) mv.getModel().get(FORM_MODEL_NAME);

		if (assetList.length > 1) {
			form.setAssetList(assetList);
			// form.setCurrentAssetIdx(0);
			form.setMultiAsset(true);
		}
		mv.addObject(FORM_MODEL_NAME, form);
		return mv;
	}

	@RequestMapping(value = "/landing/source/start", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView start(@RequestParam(value="auId") Integer auId)
	throws Exception
	{
		log.debug("start(): entered...");
		// load minimum information here. If you need extra information,
		// load it on the specific branch (see authorProvided example)
		AssetUse au = getAssetUseRepository().loadAssetUseById(auId);

		SourceWizardForm form = new SourceWizardForm();

		ModelAndView mv = null;
		// if owner type is 3rd party, we ask if they want to replace or add another one
		if (null != au.getAsset().getOwnerType() &&
				au.getAsset().getOwnerType().equals(OwnerType.THIRD_PARTY) &&
				!CollectionUtils.isEmpty(au.getAsset().getSources())) {
			mv = new ModelAndView("pages.landing.source.3rdparty.replace");
		} else {
			if (null != au.getAsset().getOwnerType() &&
					(au.getAsset().getOwnerType().equals(OwnerType.PUBLIC_DOMAIN) ||
					 au.getAsset().getOwnerType().equals(OwnerType.FAIR_USE))) {
				form.setReplace(true);
			}
			mv = new ModelAndView("pages.landing.source.main");
		}

		form.setPrevUrl("/sapp/landing/source/start?auId=" + auId);
		form.setAUId(auId);
		form.setAssetUse(au);
		form.populateForm();
		mv.addObject(FORM_MODEL_NAME, form);
		return mv;
	}

	@RequestMapping(value = "/landing/source/replace", method = RequestMethod.POST)
	public ModelAndView replace(@ModelAttribute(FORM_MODEL_NAME) SourceWizardForm form)
	throws Exception
	{
		log.debug("replace(): entered...");
		form.setReplace (true);
		AssetUse au = form.getAssetUse();
		ModelAndView mv = null;
		// we need to figure out if the asset-use needs to be canceled, or deleted.
		// we also need to know if the associated asset needs to also be deleted.
		Asset dasset = assetUseRepository.lazyLoad(Asset.class, au.getAsset().getId(), new String[] {"purchaseOrders", "contracts"});
		if (dasset.getAllPurchaseOrdersCount() < 1 && dasset.getAllContractsCount() < 1) {
			mv = new ModelAndView("redirect:/sapp/landing/source/main");
			mv.addObject(FORM_MODEL_NAME, form);
		} else {
			mv = new ModelAndView("pages.landing.source.3rdparty.clear");
			mv.addObject(FORM_MODEL_NAME, form);
		}
		return mv;
	}

	@RequestMapping(value = "/landing/source/clean", method = RequestMethod.POST)
	public ModelAndView clearReferences(@ModelAttribute(FORM_MODEL_NAME) SourceWizardForm form)
	throws Exception
	{
		log.debug("clearReferences(): entered...");
		// we do not delete here, because the user might cancel the action. Delete on submit
		form.setClear(true);
		ModelAndView mv = new ModelAndView("redirect:/sapp/landing/source/main");
		mv.addObject(FORM_MODEL_NAME, form);
		return mv;
	}

	@RequestMapping(value = "/landing/source/main", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView page1Load(@ModelAttribute(FORM_MODEL_NAME) SourceWizardForm form)
	throws Exception
	{
		log.debug("page1(): entered...");
		ModelAndView mv = new ModelAndView("pages.landing.source.main");
		mv.addObject(FORM_MODEL_NAME, form);
		return mv;
	}

	@RequestMapping(value = "/landing/source/main", method = RequestMethod.POST)
	public ModelAndView page1Submit(@ModelAttribute(FORM_MODEL_NAME) SourceWizardForm form)
	throws Exception
	{
		log.debug("page1(): submit entered...");

		// REQUEST BRANCH
		if (form.getOwnerType().equals(SourceWizardForm.FormOwnerType.PHOTO_REQUEST.getCode()) ||
			form.getOwnerType().equals(SourceWizardForm.FormOwnerType.ILLUSTRATION_REQUEST.getCode())) {
			ModelAndView mv = new ModelAndView("redirect:/sapp/landing/source/submit_photorequest");
			return mv;
		// WILEY PUBLICATION BRANCH
		} else if (form.getOwnerType().equals(SourceWizardForm.FormOwnerType.WILEY_OWNED_BOOK.getCode()) ||
				form.getOwnerType().equals(SourceWizardForm.FormOwnerType.WILEY_OWNED_JOURNAL.getCode())) {
			ModelAndView mv = new ModelAndView("redirect:/sapp/landing/source/submit_wiley");
			return mv;
		// AUTHOR PROVIDED BRANCH  and WILEY_CREATED
		} else if (form.getOwnerType().equals(SourceWizardForm.FormOwnerType.WILEY_OWNED_AUTHOR.getCode()) ||
				form.getOwnerType().equals(SourceWizardForm.FormOwnerType.WILEY_CREATED.getCode())) {
			//***Added for Paperwork Task Section C--Start***
            //ModelAndView mv = new ModelAndView("redirect:/sapp/landing/source/submit_author");
			  log.debug("OwnerType value in SourceWizardForm "+form.getOwnerType());
              ModelAndView mv = new ModelAndView("redirect:/sapp/landing/source/3rdparty_search");
            //***Added for Paperwork Task Section C--End***
			return mv;
			// WORK_FOR_HIRE
		} else if (form.getOwnerType().equals(SourceWizardForm.FormOwnerType.WORK_FOR_HIRE.getCode())) {
			ModelAndView mv = new ModelAndView("redirect:/sapp/landing/source/3rdparty_search");
			return mv;
		// 3rd PARTY BRANCH
		} else if (form.getOwnerType().equals(SourceWizardForm.FormOwnerType.THIRD_PARTY_INSTITUTION.getCode()) ||
				form.getOwnerType().equals(SourceWizardForm.FormOwnerType.THIRD_PARTY_PUBLICATION.getCode())) {
			ModelAndView mv = new ModelAndView("redirect:/sapp/landing/source/3rdparty_search");
			return mv;
		// PREVIOUS EDITION BRANCH
		} else if (form.getOwnerType().equals(SourceWizardForm.FormOwnerType.PREVIOUS_EDITION.getCode()) ||
				form.getOwnerType().equals(SourceWizardForm.FormOwnerType.PREVIOUS_EDITION.getCode())) {
			log.debug("previous edition");
			// load commonwork here - otherwise it loads too much data that is not used in other branches
			CommonWork cw = commonWorkRepository.loadWithExtendedPrimaryProductById (form.getAssetUse().getCommonWork().getId());
			form.getAssetUse().setCommonWork(cw);
			form.populatePreviousEditionForm();

			form.setEditions(getEditionArray(form.getAssetUse().getCommonWork().getId()));

			ModelAndView mv = new ModelAndView("pages.landing.source.previous.edition.select.edition");
			if (null == form.getEditions() || form.getEdition().equals("0")) {
				// no previous editions
				 mv = new ModelAndView("pages.landing.source.previous.edition.asset.save");
			}

			mv.addObject(FORM_MODEL_NAME, form);
			return mv;

			// REIGHTSLINK BRANCH
	} else if (form.getOwnerType().equals(SourceWizardForm.FormOwnerType.RIGHTSLINK_WARNING.getCode()) ) {
		log.debug("rights link warning");
		ModelAndView mv = new ModelAndView("pages.landing.source.rights.link.warning");
		return mv;
	} else
			throw new ServiceException ("Not implemented", true);
	}

	@RequestMapping(value = "/landing/source/3rdparty_search", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView search3rdPartyLoad (HttpServletRequest request,
			@ModelAttribute(FORM_MODEL_NAME) SourceWizardForm form,
			@RequestParam(value = "multi", required=false) boolean multiSource)
	throws Exception
	{
		log.debug("search3rdPartyLoad(): entered...");
		Collection<Source> sources = getSourceRepository().getAllEnabledSourcesForCW(form.getAssetUse().getCommonWork().getId());
		Integer userId = PermUserContext.getCurrentUserId(request);
		User user = userRepository.lazyLoad(User.class, userId, new String[] { "sources", "favoriteGroup" });

		List<Source> profileSources = user.getSources();

		// if user has a favorite profile, do a union of the sources in it and the my sources from user profile
		if (user.getFavoriteGroup() != null) {
			FavoriteGroup fab = getUserRepository().find(FavoriteGroup.class, user.getFavoriteGroup().getId());
			for (int x = 0; x < fab.getSources().size(); x++) {
				if (!profileSources.contains(fab.getSources().get(x))) {
					profileSources.add(fab.getSources().get(x));
				}
			}
		}

		form.setProfileSources (profileSources);
		ModelAndView mv = new ModelAndView("pages.landing.source.3rdparty.search");
		if (!multiSource) {
			// remove the source id
			form.removeSourceId ();
		}
		form.setSourceId(0);
		mv.addObject(FORM_MODEL_NAME, form);
		mv.addObject("sources", sources);
		mv.addObject("mysources", profileSources);
		return mv;
	}

	// we need to be a GET so we can redirect from create new source
	@RequestMapping(value = "/landing/source/submit_3rdparty_search", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView search3rdPartySubmit (HttpServletRequest request,
			@ModelAttribute(FORM_MODEL_NAME) SourceWizardForm form)
	throws Exception
	{
		log.debug("search3rdPartySubmit(): entered...");
		ModelAndView mv = null;
		// we make the decision here if the source was found or we create one
		if (form.getSourceId() == 0) {
			String sourceName = form.getSourceName();
			if (StringUtils.isNotBlank(sourceName)) {
				Source source = sourceRepository.loadSourceByName(sourceName);
				if (null != source) {
					form.setSourceId(source.getId());
				}
			}
		}
		if (form.getSourceId() != 0) {
			form.setHasRoyaltyFreeDeals(false);
			Source source = sourceRepository.lazyLoad(Source.class, form.getSourceId(), new String[] {"sourceGroup","sourceGroup.royaltyFreeDeals"});
			if (null != source && null != source.getSourceGroup()) {
				if (null != source.getSourceGroup().getRoyaltyFreeDeals()
					&&  source.getSourceGroup().getRoyaltyFreeDeals().size() > 0)
				{
					form.setHasRoyaltyFreeDeals(true);
				}
			  }
		}

		if (form.getSourceId() != 0) {
			// if 3rd party institution or previous edition selected
			if (form.getOwnerType().equals(SourceWizardForm.FormOwnerType.THIRD_PARTY_INSTITUTION.getCode()) ||
					form.getOwnerType().equals(SourceWizardForm.FormOwnerType.PREVIOUS_EDITION.getCode())) {
				mv = new ModelAndView("redirect:/sapp/landing/source/3rdpartyi_sourcedetails");
			}
			if (form.getOwnerType().equals(SourceWizardForm.FormOwnerType.THIRD_PARTY_PUBLICATION.getCode())) {
				mv = new ModelAndView("redirect:/sapp/landing/source/3rdpartyp_sourcedetails");
			}
			if (form.getOwnerType().equals(SourceWizardForm.FormOwnerType.WORK_FOR_HIRE.getCode())) {
				mv = new ModelAndView("redirect:/sapp/landing/source/3rdpartyw_sourcedetails");
			}
			//***Added for Paperwork Task Section C--Start***
            if (form.getOwnerType().equals(SourceWizardForm.FormOwnerType.WILEY_OWNED_AUTHOR.getCode()) ||
            	 form.getOwnerType().equals(SourceWizardForm.FormOwnerType.WILEY_CREATED.getCode())) {
            	log.debug("search3rdPartySubmit()3: entered...3" +form.getSourceId());
                mv = new ModelAndView("redirect:/sapp/landing/source/3rdpartyi_sourcedetails");
            }
            //***Added for Paperwork Task Section C--End***
		// source not found - create a new one
		} else {
			mv = new ModelAndView("redirect:/sapp/landing/source/3rdparty_newsource");
		}

		return mv;
	}

	@RequestMapping(value = "/landing/source/3rdparty_newsource", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView newSourceLoad(@ModelAttribute(FORM_MODEL_NAME) SourceWizardForm form)
	throws Exception
	{
		ModelAndView mv = new ModelAndView("pages.landing.source.3rdparty.new");
		return mv;
	}

	@RequestMapping(value = "/landing/source/3rdpartyi_sourcedetails", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView iSourceDetailsLoad(HttpServletRequest request, @ModelAttribute(FORM_MODEL_NAME) SourceWizardForm form)
	throws Exception
	{
		ModelAndView mv = new ModelAndView("pages.landing.source.3rdparty.institution");
		// for second and so on, we do not load the form again
		if (!form.isMultiSource())
			form.populate3rdPartyInstitutionForm ();
		Source source = sourceRepository.loadSourceById(form.getSourceId());

		Integer userId = PermUserContext.getCurrentUserId(request);
		User user = userRepository.lazyLoad(User.class, userId, new String[] { "sources" });
		List<Source> profileSources = user.getSources();

		mv.addObject("isfavorite", profileSources.contains(source));
		mv.addObject("source", source);
		if (null != source.getSourceGroup()) {
			List<MasterAgreementDeal> dDeals = contractRepository.loadMasterAgreements(source.getSourceGroup().getId(), userId, new Date());

			form.setMasterAgreementDeals(dDeals);
			if (dDeals.size() == 1) {  // otherwise leave blank until user picks a master agreement
				form.setSelectedDealPricing(dDeals.get(0).getPricingInfo());
			}
		}
		Address ea = sourceRepository.loadAddressOfType(source.getId(), AddressType.MAIN);
		if (null != ea) {
			mv.addObject("address", ea);
		}
		mv.addObject(FORM_MODEL_NAME, form);
		return mv;
	}

	@RequestMapping(value = "/landing/source/3rdpartyp_sourcedetails", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView pSourceDetailsLoad(HttpServletRequest request, @ModelAttribute(FORM_MODEL_NAME) SourceWizardForm form)
	throws Exception
	{
		ModelAndView mv = new ModelAndView("pages.landing.source.3rdparty.publication");
		// for second and so on, we do not load the form again
		if (!form.isMultiSource())
			form.populate3rdPartyInstitutionForm ();
		Source source = sourceRepository.loadSourceById(form.getSourceId());
		Integer userId = PermUserContext.getCurrentUserId(request);
		User user = userRepository.lazyLoad(User.class, userId, new String[] { "sources"});

		List<Source> profileSources = user.getSources();

		mv.addObject("isfavorite", profileSources.contains(source));
		mv.addObject("source", source);
		Address ea = sourceRepository.loadAddressOfType(source.getId(), AddressType.MAIN);
		if (null != ea) {
			mv.addObject("address", ea);
		}
		mv.addObject(FORM_MODEL_NAME, form);
		return mv;
	}

	@RequestMapping(value = "/landing/source/3rdpartyw_sourcedetails", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView wSourceDetailsLoad(@ModelAttribute(FORM_MODEL_NAME) SourceWizardForm form)
	throws Exception
	{
		ModelAndView mv = new ModelAndView("pages.landing.source.3rdparty.publication");
		// for second and so on, we do not load the form again
		if (!form.isMultiSource())
			form.populate3rdPartyInstitutionForm ();
		Source source = sourceRepository.loadSourceById(form.getSourceId());
		List<Source> profileSources = form.getProfileSources();
		mv.addObject("isfavorite", profileSources.contains(source));
		mv.addObject("source", source);
		Address ea = sourceRepository.loadAddressOfType(source.getId(), AddressType.MAIN);
		if (null != ea) {
			mv.addObject("address", ea);
		}
		mv.addObject(FORM_MODEL_NAME, form);
		return mv;
	}

	@RequestMapping(value = "/landing/source/3rdparty_addsource", method = RequestMethod.POST)
	public ModelAndView addSourceSubmit(
			HttpServletRequest request,
			@RequestParam(value = "isfavorite", required=false) boolean isFavorite,
			@RequestParam(value = "multi", required=false) boolean multiSource,
			@ModelAttribute(FORM_MODEL_NAME) SourceWizardForm form)
	throws Exception
	{
		form.addSourceId ();
		form.setMultiSource (multiSource);
		// add/remove from favorites
		Integer userId = PermUserContext.getCurrentUserId(request);
		User user = userRepository.lazyLoad (User.class, userId, new String[] {"sources"});
		Source source = sourceRepository.find(Source.class, new Integer(form.getSourceId()));
		if (isFavorite) {
			user.addSource (source);
		} else {
			user.removeSource (source);
		}
		userRepository.save(user);
		/* do not save after source is selected https://www.pivotaltracker.com/story/show/34027645
		// save the selected source
		if (form.getOwnerType().equals(SourceWizardForm.FormOwnerType.THIRD_PARTY_INSTITUTION.getCode()) ||
				form.getOwnerType().equals(SourceWizardForm.FormOwnerType.PREVIOUS_EDITION.getCode())) {
			submit3rdPartyInstitution(form);
		}
		if (form.getOwnerType().equals(SourceWizardForm.FormOwnerType.THIRD_PARTY_PUBLICATION.getCode())) {
			submit3rdPartyPublication(form);
		}

		if (form.getOwnerType().equals(SourceWizardForm.FormOwnerType.WORK_FOR_HIRE.getCode())) {
			submitWorkForHire(form);
		}
		*/
		// continue

		ModelAndView mv = null;
		if (!multiSource) {
			 mv = new ModelAndView("redirect:/sapp/landing/source/3rdparty_assetdetails");
		} else {
			 mv = new ModelAndView("redirect:/sapp/landing/source/3rdparty_search?multi=" + multiSource);
		}

		return mv;
	}

	@RequestMapping(value="/landing/source/download_file",method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView downloadAgreement(@RequestParam("agreementId") Integer agreementId)
			throws Exception
	{
		log.debug("downloadAgreement(): entered...");

		ModelAndView mv = new ModelAndView();
		GenericFileView view = new GenericFileView();

		MasterAgreementDeal ma = getRepository().find(MasterAgreementDeal.class, agreementId);

		view.setFileName(ma.getFileName());
		view.setContentType(ma.getMimeType());

		log.debug("File Data size: " + ma.getFileData().length);
		view.setData(ma.getFileData());
		mv.setView(view);
		return mv;
	}

	@RequestMapping(value = "/landing/source/3rdparty_assetdetails", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView sourceAssetDetailsLoad (HttpServletRequest request, @ModelAttribute(FORM_MODEL_NAME) SourceWizardForm form)
	throws Exception
	{
		int cwId =  form.getAssetUse().getCommonWork().getId();
		log.debug("sourceAssetDetailsLoad(): submit entered..."+cwId);
		ModelAndView mv = new ModelAndView("pages.landing.source.3rdparty.asset.details");
		form = multiAssetNextAsset(form);

		if (null != form) {
			form.populate3rdPartyAssetDetailsForm ();
			// lazy load files
			AssetUse au = form.getAssetUse();
			Asset asset = getAssetRepository().lazyLoad(Asset.class, form.getAssetUse().getAsset().getId(), new String[]{"files"});
			// preserve the asset because information has been entered in previous screens
			au.getAsset().setFiles(asset.getFiles());

			Source source = sourceRepository.lazyLoad(Source.class, form.getSourceId(), new String[] {"contacts", "sourceGroup"});
			if (null != source && null != source.getSourceGroup()) {
				Integer userId = PermUserContext.getCurrentUserId(request);
				List<MasterAgreementDeal> dDeals = contractRepository.loadMasterAgreements(source.getSourceGroup().getId(), userId, new Date());
				form.setMasterAgreementDeals(dDeals);
				if (dDeals.size() == 1) {  // otherwise leave blank until user picks a master agreement
					form.setSelectedDealPricing(dDeals.get(0).getPricingInfo());
				}
			}
			mv.addObject("cwId", cwId);
			mv.addObject(FORM_MODEL_NAME, form);
		} else {
			mv = new ModelAndView("dialog.success");
			mv.addObject("message", "Information entered");
		}
		return mv;
	}

	@RequestMapping(value = "/landing/source/submit_3rdparty", method = RequestMethod.POST)
	public ModelAndView submit3rdParty(@ModelAttribute(FORM_MODEL_NAME) SourceWizardForm form)
	throws Exception
	{
		log.debug("submit3rdParty(): submit entered..." +form.isPublicDomain());
		log.debug("submit3rdParty(): submit entered..." +form.isMultiSource());

		AssetUse au = getAssetUseRepository().loadAssetUseById(form.getAUId());

		if(form.getAssetUse().getAsset().isWillBeWorkForHire() && form.getAssetUse().getAsset().getCopyrightType().equals(CopyrightType.NOT_WILEY_OWNED)){
			form.getAssetUse().getAsset().setCopyrightType(CopyrightType.WILEY_OWNED_WORK_FOR_HIRE);
		}

		// if we need to clear first
		if (form.isClear())
			assetRepository.clearAssetReferences (au.getAsset().getId());

		try {
			if (form.getOwnerType().equals(SourceWizardForm.FormOwnerType.THIRD_PARTY_INSTITUTION.getCode()) ||
					form.getOwnerType().equals(SourceWizardForm.FormOwnerType.PREVIOUS_EDITION.getCode())) {
				submit3rdPartyInstitution(form);
			}
			if (form.getOwnerType().equals(SourceWizardForm.FormOwnerType.THIRD_PARTY_PUBLICATION.getCode())) {
				submit3rdPartyPublication(form);
			}
			if (form.getOwnerType().equals(SourceWizardForm.FormOwnerType.WORK_FOR_HIRE.getCode())) {
				submitWorkForHire(form);
			}
			//***Added for Paperwork Task Section C--Start***
            if (form.getOwnerType().equals(SourceWizardForm.FormOwnerType.WILEY_OWNED_AUTHOR.getCode()) ||
                        form.getOwnerType().equals(SourceWizardForm.FormOwnerType.WILEY_CREATED.getCode())) {
                submit3rdPartyInstitution(form);
            }
            //***Added for Paperwork Task Section C--End***
		} catch (ServiceException e) {
			ModelAndView mv = new ModelAndView("dialog.success");
			mv.addObject("message", "Error: " + e.getMessage());
			return mv;
		}
		ModelAndView mv = new ModelAndView("redirect:/sapp/landing/source/3rdparty_assetdetails");
		return mv;
	}

	public void submit3rdPartyInstitution (SourceWizardForm form)
	throws Exception
	{
		log.debug("submit3rdPartyInstitution(): submit entered...");
		log.debug("AssetUse ID " + form.getAUId());
		log.debug("AssetUse ID " + form.isPublicDomain());
		// decided to reload the AssetUse in submit, because from the beginning to end might
		// take a long time and the data might have been changed
		AssetUse au = getAssetUseRepository().loadAssetUseById(form.getAUId());
		au = form.populate3rdPartyInstitutionBean (au);
		au.setNoFlyMatchApproved(form.isNoFlyMatchApproved());
		
		//SR_301213 starts
		if(form.isMediaManager()){
			au.setMediaManager(true);
		}
		//SR_301213 ends
		
		

		Map<Integer, Boolean> sourcesMap = form.getSourcesMap();
		for (Integer sid : sourcesMap.keySet()) {
			Source source = sourceRepository.loadSourceById(sid);
			// add
			if (sourcesMap.get(sid)) {
				if (!au.getAsset().getSources().contains(source))
					au.getAsset().getSources().add(source);
			// remove
 			} else {
 				if (au.getAsset().getSources().contains(source))
					au.getAsset().getSources().remove(source);
			}
		}

		//Start of Modification for INC_60612 to display the correct Permission Status as Granted- Author Owned/Created
		/*if(form.getOwnerType().equals(SourceWizardForm.FormOwnerType.WILEY_OWNED_AUTHOR.getCode())){
			log.debug("In Author Owned block setting isManaged to false");
		    au.getAsset().setManaged(false);
		}*///This has commented out since the spec has changed now as per DM-186
		//End of Modification for INC_60612

		if(form.isPublicDomain()){
			au.getAsset().setManaged(true);
		}

		/*Added for Incident INC_60293*/
		log.debug("Asset id from ------>"+au.getAsset().getVendorId());
		log.debug("Source id--------->"+form.getSourceId());
		for (Integer sid : sourcesMap.keySet()) {
			log.debug("Sources map------->"+sourcesMap.get(sid));
		}
		List<Integer> sourceIds = new ArrayList<Integer>();
		sourceIds.add(form.getSourceId());
		String vendorId = au.getAsset().getVendorId();
		List<Asset> assets = assetRepository.loadAssetBySourceVendorId(sourceIds, vendorId);
		String assetId="";
		if(!assets.isEmpty()){
			for(Asset asset : assets){
				assetId = ""+asset.getId();
			}
		}
		log.debug("Asset Id wanted----->"+assetId);
		if (StringUtils.isNotBlank(assetId)){
		AssetUse previousau = assetUseRepository.loadPreviousAssetUse(assetId);
		if(null != previousau){
		AssetUse prevau = getAssetUseRepository().loadAssetUseById(previousau.getId());
		log.debug("permission comment------->"+previousau.getPermissionComment());
		log.debug("permission comment------->"+previousau.getProductionComment());
		au.setPermissionComment(previousau.getPermissionComment());
		au.setProductionComment(previousau.getProductionComment());
		//au.setFinalPage(previousau.getFinalPage());
		//au.setPosition(previousau.getPosition());
		log.debug("Common work id------->"+au.getCommonWork().getId());
		List<Product> productList = new ArrayList<Product>();
		CommonWork cw = commonWorkRepository.loadWithPrimaryProductById(au.getCommonWork().getId());
		Product primaryProduct = cw.getPrimaryProduct();

		log.debug("Common work for previous--->"+prevau.getCommonWork().getId());
		CommonWork cw1 = commonWorkRepository.loadWithPrimaryProductById(prevau.getCommonWork().getId());
		Product primaryProduct1 = cw1.getPrimaryProduct();

		Product previousEdition = null;
		String previousEditionWID = primaryProduct.getPreviousEditionWID();
		log.debug("previousEditionWID------->"+previousEditionWID);
		if (StringUtils.isNotBlank(previousEditionWID)) {
			try{
			previousEdition = commonWorkRepository.getProductRepository().loadByExternalId(previousEditionWID);
			}catch(Exception e){
				e.printStackTrace();
			}
			if (previousEdition == null) {
				log.error("previousEditionWID [" + previousEditionWID
					+ "] for WID [" + primaryProduct.getExternalId() + "] not found in DB");
			}

		}
		while (previousEdition != null) {
			productList.add(previousEdition);
			previousEditionWID = previousEdition.getPreviousEditionWID();
			if (StringUtils.isNotBlank(previousEditionWID)) {
				previousEdition = commonWorkRepository.getProductRepository().loadByExternalId(previousEditionWID);
			}
			else previousEdition = null;
		}


		for(Product prod : productList){
			if(prod.getExternalId().equals(primaryProduct1.getExternalId())){
				au.setReusedFromPreviousEdition(true);
				au.setReusedISBN(prod.getIsbn13());
				au.setReusedPage(previousau.getFinalPage());
				au.setReusedPosition(previousau.getPosition());
				au.setReusedComment(previousau.getPermissionComment());
			}

			}
		if(!au.isReusedFromPreviousEdition()){
			au.setPickup(true);
			au.setPickupISBN(primaryProduct1.getIsbn13());
			au.setPickupPage(previousau.getFinalPage());
			au.setPickupPosition(previousau.getPosition());
			au.setPickupComment(previousau.getPermissionComment());
		}
		}
		}
///*End Added for Incident INC_60293*/
		saveAssetFile(au.getAsset(), form);
		// might throw an exception
		au = getAssetUseService().saveAssetUse(au,true,false);
	}

	public void submit3rdPartyPublication (SourceWizardForm form)
	throws Exception
	{
		log.debug("submit3rdPartyPublication(): submit entered...");
		log.debug("AssetUse ID " + form.getAUId());
		// decided to reload the AssetUse in submit, because from the beginning to end might
		// take a long time and the data might have been changed
		AssetUse au = getAssetUseRepository().loadAssetUseById(form.getAUId());
		au = form.populate3rdPartyPublicationBean (au);
		au.setNoFlyMatchApproved(form.isNoFlyMatchApproved());
		au.setMediaManager(form.isMediaManager()); //SR_301213
		Map<Integer, Boolean> sourcesMap = form.getSourcesMap();
		for (Integer sid : sourcesMap.keySet()) {
			Source source = sourceRepository.loadSourceById(sid);
			// add
			if (sourcesMap.get(sid)) {
				if (!au.getAsset().getSources().contains(source))
					au.getAsset().getSources().add(source);
			// remove
 			} else {
 				if (au.getAsset().getSources().contains(source))
					au.getAsset().getSources().remove(source);
			}
		}
		//Added for implementing Build RN 151026-001937 -- Start
		if(form.isPublicDomain()){
			au.getAsset().setManaged(true);
		}
		//Added for implementing Build RN 151026-001937 -- End
		/*Added for Incident INC_60293*/
		log.debug("Asset id from ------>"+au.getAsset().getVendorId());
		log.debug("Source id--------->"+form.getSourceId());
		for (Integer sid : sourcesMap.keySet()) {
			log.debug("Sources map------->"+sourcesMap.get(sid));
		}
		List<Integer> sourceIds = new ArrayList<Integer>();
		sourceIds.add(form.getSourceId());
		String vendorId = au.getAsset().getVendorId();
		List<Asset> assets = assetRepository.loadAssetBySourceVendorId(sourceIds, vendorId);
		String assetId="";
		if(!assets.isEmpty()){
			for(Asset asset : assets){
				assetId = ""+asset.getId();
			}
		}
		log.debug("Asset Id wanted----->"+assetId);
		if (StringUtils.isNotBlank(assetId)){
		AssetUse previousau = assetUseRepository.loadPreviousAssetUse(assetId);
		if(null != previousau){
		AssetUse prevau = getAssetUseRepository().loadAssetUseById(previousau.getId());
		log.debug("permission comment------->"+previousau.getPermissionComment());
		log.debug("permission comment------->"+previousau.getProductionComment());
		au.setPermissionComment(previousau.getPermissionComment());
		au.setProductionComment(previousau.getProductionComment());
		//au.setFinalPage(previousau.getFinalPage());
		//au.setPosition(previousau.getPosition());
		log.debug("Common work id------->"+au.getCommonWork().getId());
		List<Product> productList = new ArrayList<Product>();
		CommonWork cw = commonWorkRepository.loadWithPrimaryProductById(au.getCommonWork().getId());
		Product primaryProduct = cw.getPrimaryProduct();

		log.debug("Common work for previous--->"+prevau.getCommonWork().getId());
		CommonWork cw1 = commonWorkRepository.loadWithPrimaryProductById(prevau.getCommonWork().getId());
		Product primaryProduct1 = cw1.getPrimaryProduct();

		Product previousEdition = null;
		String previousEditionWID = primaryProduct.getPreviousEditionWID();
		log.debug("previousEditionWID------->"+previousEditionWID);
		if (StringUtils.isNotBlank(previousEditionWID)) {
			try{
			previousEdition = commonWorkRepository.getProductRepository().loadByExternalId(previousEditionWID);
			}catch(Exception e){
				e.printStackTrace();
			}
			if (previousEdition == null) {
				log.error("previousEditionWID [" + previousEditionWID
					+ "] for WID [" + primaryProduct.getExternalId() + "] not found in DB");
			}

		}
		while (previousEdition != null) {
			productList.add(previousEdition);
			previousEditionWID = previousEdition.getPreviousEditionWID();
			if (StringUtils.isNotBlank(previousEditionWID)) {
				previousEdition = commonWorkRepository.getProductRepository().loadByExternalId(previousEditionWID);
			}
			else previousEdition = null;
		}


		for(Product prod : productList){
			if(prod.getExternalId().equals(primaryProduct1.getExternalId())){
				au.setReusedFromPreviousEdition(true);
				au.setReusedISBN(prod.getIsbn13());
				au.setReusedPage(previousau.getFinalPage());
				au.setReusedPosition(previousau.getPosition());
				au.setReusedComment(previousau.getPermissionComment());
			}

			}
		if(!au.isReusedFromPreviousEdition()){
			au.setPickup(true);
			au.setPickupISBN(primaryProduct1.getIsbn13());
			au.setPickupPage(previousau.getFinalPage());
			au.setPickupPosition(previousau.getPosition());
			au.setPickupComment(previousau.getPermissionComment());
		}
		}
		}
///*End Added for Incident INC_60293*/
		au = getAssetUseService().saveAssetUse(au,true,false);
		// now save the uploaded files
		saveAssetFile(au.getAsset(), form);

		// temporary fix
		getAssetUseIndexService().updateIndex(au.getId());
	}

	public void submitWorkForHire (SourceWizardForm form)
	throws Exception
	{
		log.debug("submitWorkForHire(): submit entered...");
		// decided to reload the AssetUse in submit, because from the beginning to end might
		// take a long time and the data might have been changed
		AssetUse au = getAssetUseRepository().loadAssetUseById(form.getAUId());
		au = form.populate3rdPartyPublicationBean (au);
		au.getAsset().setManaged(false);
		au.getAsset().setRoyaltyFree(true);
		au.getAsset().setFeeRequired(false);
		au.setNoFlyMatchApproved(form.isNoFlyMatchApproved());
		au.setMediaManager(form.isMediaManager()); // SR_301213
		Map<Integer, Boolean> sourcesMap = form.getSourcesMap();
		for (Integer sid : sourcesMap.keySet()) {
			Source source = sourceRepository.loadSourceById(sid);
			// add
			if (sourcesMap.get(sid)) {
				if (!au.getAsset().getSources().contains(source))
					au.getAsset().getSources().add(source);
			// remove
 			} else {
 				if (au.getAsset().getSources().contains(source))
					au.getAsset().getSources().remove(source);
			}
		}
		au = getAssetUseService().saveAssetUse(au,true,false);
	}

	@RequestMapping(value = "/landing/source/submit_photorequest", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView presubmitPhotoRequest(@ModelAttribute(FORM_MODEL_NAME) SourceWizardForm form)
	throws Exception
	{
		ModelAndView mv = new ModelAndView("pages.landing.source.photo.request");
		form = multiAssetNextAsset(form);
		if (null != form) {
			mv.addObject(FORM_MODEL_NAME, form);
		} else {
		    mv = new ModelAndView("dialog.success");
			mv.addObject("message", "Information entered");
		}
		return mv;
	}

	@RequestMapping(value = "/landing/source/submit_photorequest", method = RequestMethod.POST)
	public ModelAndView submitPhotoRequest(@ModelAttribute(FORM_MODEL_NAME) SourceWizardForm form)
	throws Exception
	{
		log.debug("submitPhotoRequest(): submit entered...");
		// decided to reload the AssetUse in submit, because from the beginning to end might
		// take a long time and the data might have been changed
		AssetUse au = getAssetUseRepository().loadAssetUseById(form.getAUId());
		au = form.populatePhotoRequestBean (au);
		au = getAssetUseService().saveAssetUse(au);

		ModelAndView mv = null;
		mv = new ModelAndView("redirect:/sapp/landing/source/submit_photorequest");
		return mv;
	}

	@RequestMapping(value = "/landing/source/submit_wiley", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView presubmitWileyBook(HttpServletRequest request, @ModelAttribute(FORM_MODEL_NAME) SourceWizardForm form)
	throws Exception
	{
		ModelAndView mv = new ModelAndView("pages.landing.source.wiley.book");
		if (form.getOwnerType().equals(SourceWizardForm.FormOwnerType.WILEY_OWNED_JOURNAL.getCode())) {
			mv = new ModelAndView("pages.landing.source.wiley.journal");
		}
		form = multiAssetNextAsset(form);
		if (null != form) {
			mv.addObject(FORM_MODEL_NAME, form);
		} else {
		    mv = new ModelAndView("dialog.success");
			mv.addObject("message", "Information entered");
		}

		String dateFormat = PermUserContext.getPickerDateFormat(request);
		mv.addObject("dateFormat",dateFormat);

		return mv;
	}

	@RequestMapping(value = "/landing/source/submit_wiley", method = RequestMethod.POST)
	public ModelAndView submitWileyBook(@ModelAttribute(FORM_MODEL_NAME) SourceWizardForm form)
	throws Exception
	{
		log.debug("submitWileyBook(): submit entered...");
		// decided to reload the AssetUse in submit, because from the beginning to end might
		// take a long time and the data might have been changed
		AssetUse au = getAssetUseRepository().loadAssetUseById(form.getAUId());
		au = form.populateWileyBean (au);
		// assume new asset will be third party so if anything happens and this branch
		// does not finish, it show as third party
		au.setWizardOwnerType(OwnerType.THIRD_PARTY.getCode());
		au.getAsset().setOwnerType(OwnerType.THIRD_PARTY);

		if (null != au.getPickupISBN() || null != au.getReusedComment()) {
			au.setImportSource(null);
		}

		au = getAssetUseService().saveAssetUse(au, true, false);

		if (null != au.getPickupISBN() && !form.getOwnerType().equals(SourceWizardForm.FormOwnerType.WILEY_OWNED_JOURNAL.getCode()) ) {
			au.setImportSource(null);
			Product product = commonWorkRepository.getProductRepository().getProductByISBN(au.getPickupISBN());
			if (product != null) {
				product = commonWorkRepository.lazyLoad(Product.class, product.getId(), new String[] {"commonWork"});

				int cwId = product.getCommonWork().getId();
				LandingFilterForm lff = new LandingFilterForm();

				List<AssetUseTableRowView> auList = assetUseService.loadAssetListTableFromIndex(cwId, lff, true);
				ArrayList<AssetUseTableRowView> sulst2 = new ArrayList<AssetUseTableRowView>();
				if (auList == null || auList.size() == 0) {
					ModelAndView mv = new ModelAndView("pages.landing.source.determine.owner.type");
				   	form.setPrevUrl("/sapp/landing/source/pub_asset_details");
					mv.addObject(FORM_MODEL_NAME, form);
					return mv;
				}

				log.debug("submitWileyBook(): assets found on previous wiley publication *****");
				for (int x = 0; x < auList.size(); x++) {
					// reviewed unknown and author provided unknown are excluded
					if (auList.get(x).isReviewedOrAuthorUnknown()) continue;
					sulst2.add(auList.get(x));
					log.debug("submitWileyBook(): auid: " + auList.get(x).getAssetUseId()
						+ " Asset Description: " + auList.get(x).getDescription() +  " usage: " + auList.get(x).getUsage());
				}
				form.setPreviousPubAssets(sulst2);

				ModelAndView mv = new ModelAndView("redirect:/sapp/landing/source/pub_assets_details");
				form.setPrevUrl("/sapp/landing/source/start?auId=" + form.getAUId());
				mv.addObject(FORM_MODEL_NAME, form);
				return mv;
			}
		}

		ModelAndView mv = null;
		mv = new ModelAndView("redirect:/sapp/landing/source/submit_wiley");
		return mv;
	}

	@RequestMapping(value = "/landing/source/pub_assets_details", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView previousPubAssets(@ModelAttribute(FORM_MODEL_NAME) SourceWizardForm form)
	throws Exception
	{
		log.debug("previousPubAssets(): entered...asset count = "
			+ (form.getPreviousPubAssets() == null ? "null" : form.getPreviousPubAssets().size()));

		form.setPrevUrl("/sapp/landing/source/submit_wiley");

		ModelAndView mv = new ModelAndView("pages.landing.source.wiley.pub.assets.details");
		mv.addObject(FORM_MODEL_NAME, form);

		return mv;
	}

	@RequestMapping(value = "/landing/source/pub_assets_details", method = RequestMethod.POST)
	public ModelAndView submitPreviousPubAssets(@ModelAttribute(FORM_MODEL_NAME) SourceWizardForm form)
	throws Exception
	{
		log.debug("starting submitPreviousPubAssets() ");
		ModelAndView mv = new ModelAndView("");

		if (null == form.getSelectedPreviousAssetUseId() || form.getSelectedPreviousAssetUseId() == 0) {
			mv = new ModelAndView("pages.landing.source.determine.owner.type");

			if (form.getPreviousPubAssets().size() > 0) {
				mv = new ModelAndView("pages.landing.source.asset.not.on.the.list");
			}
			form.setPrevUrl("/sapp/landing/source/pub_asset_details");
			mv.addObject(FORM_MODEL_NAME, form);
			return mv;
		}

		AssetUse currentAu = getAssetUseRepository().loadAssetUseById(form.getAUId());

		List<AssetUseTableRowView> auList = form.getPreviousPubAssets();
		for (int x = 0; x < auList.size(); x++) {
			String selected = "No";
			if (auList.get(x).getAssetUseId() == form.getSelectedPreviousAssetUseId()) {
				selected = "Yes" ;
				AssetUse peAu = assetUseRepository.loadAssetUseById(auList.get(x).getAssetUseId());
				Asset peAsset = assetUseRepository.lazyLoad(Asset.class, peAu.getAsset().getId(), new String[] {"ownerType","sources","files"});
				form.setSelectedPreviousAssetUse(peAu);

				if (! peAsset.getOwnerType().equals(OwnerType.AUTHOR_OWNED)) {
					currentAu.setAsset(peAsset);
				} else {
					currentAu.getAsset().setDescription(peAsset.getDescription());
					currentAu.getAsset().setBiblio(peAsset.getBiblio());
					currentAu.getAsset().setCitation(peAsset.getCitation());
					currentAu.getAsset().setCreditLine(peAsset.getCreditLine());
					currentAu.getAsset().setOwnerType(peAsset.getOwnerType());
					currentAu.getAsset().setArtist(peAsset.getArtist());
			 		if (null == peAsset.getOwnerType()) {
			 			currentAu.getAsset().setOwnerType(OwnerType.THIRD_PARTY);
			 		}
			 		if (peAsset.getSources().size() > 0) {
						currentAu.getAsset().getSources().clear();
						for(int s = 0;s<peAsset.getSources().size(); s++) {
							currentAu.getAsset().getSources().add(peAsset.getSources().get(s));
						}
					}
				}

		//		if (StringUtils.isBlank(currentAu.getPickupTitle())  && !StringUtils.isBlank(currentAu.getPickupISBN())) {

			 	CommonWork pecw = getCommonWorkRepository().lazyLoad(CommonWork.class, peAu.getCommonWork().getId(), new String[] {"products"});
			 	Product primaryProduct = getCommonWorkRepository().lazyLoad(Product.class, pecw.getPrimaryProduct().getId(), new String[] {"users","commonWork"});


				    currentAu.setPickupTitle(primaryProduct.getTitle());
				    currentAu.getAsset().setOriginalPublicationAuthor(primaryProduct.getAuthorsAsString());
				    currentAu.getAsset().setOriginalPublicationIsbn(primaryProduct.getIsbn13());
				    currentAu.getAsset().setOriginalPublicationTitle(primaryProduct.getTitle());
				    currentAu.setImportSource(null);
		//		}

			    if (null != peAsset.getOwnerType() && peAsset.getOwnerType().equals(OwnerType.AUTHOR_OWNED)) {
			    	mv = new ModelAndView("redirect:/sapp/landing/source/author_created?auId=" + currentAu.getId());
					currentAu = assetUseService.saveAssetUse(currentAu,true,false);
					mv.addObject(FORM_MODEL_NAME, form);
			    	return mv;
			    }

			    if (null != peAsset.getOwnerType() && peAsset.getOwnerType().equals(OwnerType.WILEY)) {
					 currentAu.getAsset().setOwnerType(OwnerType.WILEY);
					 currentAu.getAsset().setManaged(false);
					 currentAu.getAsset().setRoyaltyFree(true);
					 currentAu.getAsset().setFeeRequired(false);
					 currentAu.setPreviousWileypubAssetUseId(null);
					 currentAu = assetUseService.saveAssetUse(currentAu,true,false);
				}

				currentAu = assetUseService.saveAssetUse(currentAu,true,false);
			}

			log.debug("Asset Description:" + auList.get(x).getDescription() +  " usage:" +auList.get(x).getUsage() + " selected:" + selected);
		}

		mv = new ModelAndView("dialog.success");
		mv.addObject("message", "Done");
		mv.addObject("redirectUrl", "/sapp/cwlanding/main?cwid=" + form.getAssetUse().getCommonWork().getId());
		return mv;
	}

	@RequestMapping(value = "/landing/source/author_created", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView authorCreatedVerifyAuthor(HttpServletRequest request,
			@ModelAttribute(FORM_MODEL_NAME) SourceWizardForm form,
			@RequestParam(value="auId") Integer auId)
	throws Exception
	{
		log.debug("authorCreatedVerifyAuthor(): entered, auId = " + auId);
		AssetUse peAu = form.getSelectedPreviousAssetUse();
	 	Source dSource = null;

	 	CommonWork pecw = getCommonWorkRepository().lazyLoad(CommonWork.class, peAu.getCommonWork().getId(), new String[] {"products"});
	 	Product primaryProduct = getCommonWorkRepository().lazyLoad(Product.class, pecw.getPrimaryProduct().getId(), new String[] {"users","commonWork"});

	 	try {
	   		dSource = getSourceRepository().loadSourceByName(primaryProduct.getAuthorFullName());
	   		log.debug("authorCreatedVerifyAuthor(): source found, externalName = " + dSource.getExternalName());
	   		form.setPublicationAuthor(dSource.getExternalName());
	   	} catch (Exception ex) {
	   		form.setPublicationAuthor(primaryProduct.getAuthorFullName());
	   		log.debug("authorCreatedVerifyAuthor(): source Not found: exception msg: " + ex.getMessage());
	   	}

	   	if (null != dSource) {
	   		AssetUse currentAu = assetUseRepository.loadAssetUseById(form.getAUId());
	   		PurchaseOrder po = new PurchaseOrder();
			try {
				List<PurchaseOrder> pos = purchaseOrderRepository.getPrevEdAssetsPurchaseOrders(currentAu.getCommonWork().getId(),dSource.getId());
				if (null != pos && pos.size() > 0) {
					po = pos.get(0);
					form.setUserSignature(po.getUserSignature());
					form.setReturnAddress(po.getReturnAddress());
					form.setCurrentPubAssets(po.getAssets());
				}
			} catch (Exception e) {
				log.warn("authorCreatedVerifyAuthor(): No PO found: exception msg: " + e.getMessage());
				// ignore error
			}
	   	}

		if (StringUtils.isBlank(form.getUserSignature()) || StringUtils.isBlank(form.getReturnAddress()) ) {
			Integer userId = PermUserContext.getCurrentUserId(request);
			User user = userRepository.lazyLoad(User.class, userId, new String[] { "sources", "favoriteGroups" });
			form.setUserSignature(user.getUserDefaults().getUserSignature());
			form.setReturnAddress(user.getUserDefaults().getReturnAddress());
		}

	   	form.setNewOwnerType(OwnerType.THIRD_PARTY);

		ModelAndView mv = new ModelAndView("pages.landing.source.author.created");
	   	form.setPrevUrl("/sapp/landing/source/pub_asset_details");
		mv.addObject(FORM_MODEL_NAME, form);
		return mv;
	}

	@RequestMapping(value = "/landing/source/author_created", method = RequestMethod.POST)
	public ModelAndView authorCreatedSaveAuthor(HttpServletRequest request,
			@ModelAttribute(FORM_MODEL_NAME) SourceWizardForm form,
			@RequestParam(value = "noform", required=false) boolean noForm)
	throws Exception
	{
		log.debug("authorCreatedSaveAuthor(): entered...");

		if (form.getMakeItDefault()) {
			try {
				Integer userId = PermUserContext.getCurrentUserId(request);
				User user = userRepository.loadUserById(userId);
				user.getUserDefaults().setUserSignature(form.getUserSignature());
				user.getUserDefaults().setReturnAddress(form.getReturnAddress());
				userRepository.save(user);
			} catch (Exception f) {
				// do nothing
			}
		}

		boolean needRequestForm = false;
		AssetUse currentAu = assetUseRepository.loadAssetUseById(form.getAUId());

		AssetUse peAu = form.getSelectedPreviousAssetUse();

	   	ModelAndView mv = new ModelAndView("redirect:/sapp/landing/source/author_created?auId=" + currentAu.getId());
	   	log.debug("authorCreatedSaveAuthor(): author owned processing entered");
	   	needRequestForm = true;
	   	currentAu.getAsset().setOwnerType(OwnerType.THIRD_PARTY);
	   	Source dSource = null;

	 //  	Product primaryProduct = getCommonWorkRepository().lazyLoad(Product.class, peAu.getCommonWork().getId(), new String[] {"users"});

	   	CommonWork pecw = getCommonWorkRepository().lazyLoad(CommonWork.class, peAu.getCommonWork().getId(), new String[] {"products"});
	 	Product primaryProduct = getCommonWorkRepository().lazyLoad(Product.class, pecw.getPrimaryProduct().getId(), new String[] {"users","commonWork"});


	   	// add source to asset use or make the previous edition primary product's author the source for the asset use
	   	try {
	   		dSource = getSourceRepository().loadSourceByName(form.getPublicationAuthor());
	   		log.debug("authorCreatedSaveAuthor(): source found, external name = " + dSource.getExternalName());
	   	} catch (Exception error) {
	   		dSource = null;
	   	}

	   	if (null != dSource) {
	   		currentAu.getAsset().getSources().clear();
	   		currentAu.getAsset().getSources().add(dSource);
	   	} else {
	   		log.debug("authorCreatedSaveAuthor(): source NOT found.");
	   		dSource = new Source();
	   		dSource.setName(primaryProduct.getAuthorFullName());
	   		dSource.setDisplayName(dSource.getName());
	   		dSource = sourceRepository.saveSource(dSource);
	   		currentAu.getAsset().getSources().clear();
	   		currentAu.getAsset().getSources().add(dSource);
	   	}

	   	currentAu.setPreviousWileypubAssetUseId(form.getSelectedPreviousAssetUseId());
		currentAu = assetUseService.saveAssetUse(currentAu, true, false);
		PurchaseOrder po = updatePo(currentAu, form);

		log.debug("authorCreatedSaveAuthor(): need request form flag: " + needRequestForm);
		if (noForm) {
		//	 mv = new ModelAndView("pages.success");
		//		mv.addObject("message", "Done");
		//		mv.addObject("redirectUrl", "/sapp/cwlanding/main?cwid=" + form.getAssetUse().getCommonWork().getId());
			log.debug("authorCreatedSaveAuthor(): found no form");

			mv = new ModelAndView("pages.landing.source.add.another.asset");
			AssetUse wkAu =  assetUseRepository.lazyLoad(AssetUse.class, currentAu.getId(), new String[] {"commonWork"});
			log.debug("commonworkId:" + wkAu.getCommonWork().getId());
			AssetUse newAu = new AssetUse();
			newAu.setCommonWork(wkAu.getCommonWork());
			log.debug("commonWorkId:" + newAu.getCommonWork().getId());
			newAu.setAsset(new Asset());
			newAu.getAsset().setDescription("*ENTER DESCRIPTION");
			newAu.getAsset().setSources(currentAu.getAsset().getSources());
			newAu.getAsset().setMediaType(MediaType.PHOTO);
			newAu.getAsset().setOwnerType(OwnerType.AUTHOR_OWNED);
			newAu.setPickupISBN(currentAu.getPickupISBN());
			newAu.setPickupAuthor(currentAu.getPickupAuthor());
			newAu.setPickupTitle(currentAu.getPickupTitle());
			newAu.setUsage(Usage.FIGURE);

			newAu = getAssetUseService().saveAssetUse(newAu,true,false);
			po = updatePo(newAu, form);

			log.debug("authorCreatedSaveAuthor(): assetUse succesfully saved: " + newAu);
			newAu = assetUseRepository.lazyLoad(AssetUse.class, newAu.getId(), new String[] {"commonWork","asset"});

			form.setAUId(newAu.getId());
			form.setAssetUse(newAu);
			form.setSelectedPreviousAssetUseId(null);
			form.setSelectedPreviousAssetUse(null);

			mv.addObject(FORM_MODEL_NAME, form);
			return mv;
		}

		po = updatePo(currentAu, form);

		if (needRequestForm) {
			Integer cwId = currentAu.getCommonWork().getId();
			CommonWork commonWork = getCommonWorkRepository().lazyLoad(CommonWork.class, cwId, new String[] {"products"});
			primaryProduct = getCommonWorkRepository().lazyLoad(Product.class, commonWork.getPrimaryProduct().getId(), new String[] {"users"});
			Integer productId = primaryProduct.getId();
			mv = new ModelAndView("redirect:/sapp/permissions/preview?cwId=" + cwId + "&productId=" + productId + "&poId=" + po.getId() + "&type=po");
			return mv;
		}

		return mv;
	}

	// this method creates a new po or updates existing po with an asset use.  This is used by the other Wiley publication branch
	// because it maintains a single po for all asset uses created
	public PurchaseOrder updatePo(AssetUse currentAu, SourceWizardForm form) {

		PurchaseOrder po = new PurchaseOrder();
		try {
			List<PurchaseOrder> pos = purchaseOrderRepository.getPrevEdAssetsPurchaseOrders(currentAu.getCommonWork().getId(),
					currentAu.getAsset().getSources().get(0).getId());
			if (null != pos && pos.size() > 0) {
				log.debug("updatePo(): found po: " + pos.get(0).getId());
				po = pos.get(0);
			}
		} catch (Exception e) {
			log.debug("updatePo(): no po's found error: " + e.getMessage());
			// ignore error
		}

		try {
		po.setPermissionRequest(true);
		po.setCommonWork(currentAu.getCommonWork());
		po.setSource(currentAu.getAsset().getSources().get(0));
		po.setAssets(purchaseOrderRepository.getPrevEdAssets(currentAu.getCommonWork().getId(), currentAu.getAsset().getSources().get(0).getId()));
		po.setCreatedDate(new Date());
		po.setDate(new Date());
		po.setUserSignature(form.getUserSignature());
		po.setReturnAddress(form.getReturnAddress());
		po = purchaseOrderRepository.save(po);

		// need to calculate status again to account for the po
		assetUseService.saveAssetUse(currentAu,true,false);
		} catch (Exception e) {
			log.debug(e.getMessage());
		}

		return po;
	}

	@RequestMapping(value = "/landing/source/determine_owner_type", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView determineOwnerType(@ModelAttribute(FORM_MODEL_NAME) SourceWizardForm form)
	throws Exception
	{
		log.debug("starting determineOwnerType() ");
	   	form.setNewOwnerType(OwnerType.THIRD_PARTY);

		ModelAndView mv = new ModelAndView("pages.landing.source.determine.owner.type");
		mv.addObject(FORM_MODEL_NAME, form);
		return mv;
	}

	@RequestMapping(value = "/landing/source/determine_owner_type", method = RequestMethod.POST)
	public ModelAndView determineOwnerTypeProcess(@ModelAttribute(FORM_MODEL_NAME) SourceWizardForm form)
	throws Exception
	{
		log.debug("starting determineOwnerTypeProcess() ");

		ModelAndView mv = new ModelAndView("dialog.success");
		mv.addObject("message", "Done");
		mv.addObject("redirectUrl", "/sapp/cwlanding/main?cwid=" + form.getAssetUse().getCommonWork().getId());

		if (form.getNewOwnerType().equals(OwnerType.AUTHOR_OWNED)) {
			// fake previous edition asset use cause none were found
			AssetUse peAu = assetUseRepository.loadAssetUseById(form.getAUId());
			Product product = commonWorkRepository.getProductRepository().getProductByISBN(peAu.getPickupISBN());
			product = commonWorkRepository.lazyLoad(Product.class, product.getId(), new String[] {"commonWork"});
			peAu.setCommonWork(product.getCommonWork());
			form.setSelectedPreviousAssetUse(peAu);
			form.setSelectedPreviousAssetUseId(peAu.getId());
		   	mv = new ModelAndView("redirect:/sapp/landing/source/author_created?auId=" + form.getAUId());
			mv.addObject(FORM_MODEL_NAME, form);
			return mv;
		}

		AssetUse currentAu = assetUseRepository.loadAssetUseById(form.getAUId());

		if (form.getNewOwnerType().equals(OwnerType.WILEY)) {
			currentAu.getAsset().setOwnerType(OwnerType.WILEY);
			currentAu.getAsset().setManaged(false);
			currentAu.getAsset().setRoyaltyFree(true);
			currentAu.getAsset().setFeeRequired(false);
			currentAu.setPreviousWileypubAssetUseId(null);
			currentAu = assetUseService.saveAssetUse(currentAu,true,false);
			updatePo(currentAu, form);  // ignore returned PO
		}

	    if (form.getNewOwnerType().equals(OwnerType.THIRD_PARTY)) {
	    	currentAu.getAsset().setOwnerType(OwnerType.THIRD_PARTY);
	    	currentAu.setPreviousWileypubAssetUseId(null);
			currentAu = assetUseService.saveAssetUse(currentAu,true,false);
	    	form.setOwnerType(SourceWizardForm.FormOwnerType.THIRD_PARTY_PUBLICATION.getCode());
	    	if (StringUtils.isNotBlank(form.getPublicationAuthor())) {
	    		currentAu = LoadGetAuthor(form.getPublicationAuthor(), currentAu);
	    		form.setSelectedPreviousAssetUse(currentAu);
				currentAu = assetUseService.saveAssetUse(currentAu,true,false);
				updatePo(currentAu, form);  // ignore returned PO
	    		if (null != currentAu.getAsset().getSources() && currentAu.getAsset().getSources().size() > 0) {
	    			mv = new ModelAndView("dialog.success");
	    			mv.addObject("message", "Done");
	    			mv.addObject("redirectUrl", "/sapp/cwlanding/main?cwid=" + form.getAssetUse().getCommonWork().getId());
	    			return mv;
	    		}
	    	}
	    	mv = new ModelAndView("redirect:/sapp/landing/source/3rdparty_search");
	    }

	   	mv.addObject(FORM_MODEL_NAME, form);
		return mv;
	}

	// add search for source and attach it to given assetUse
	public AssetUse LoadGetAuthor(String sourceName, AssetUse currentAu) {
	 	Source dSource = null;
	   	try {
	   		dSource = getSourceRepository().loadSourceByName(sourceName);
	   	} catch (Exception error) {
	   		return currentAu;
	   	}

	   	if (null != dSource) {
	   		currentAu.getAsset().getSources().clear();
	   		currentAu.getAsset().getSources().add(dSource);
	   	}

	   	return currentAu;
	}

	@RequestMapping(value = "/landing/source/submit_author", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView presubmitAuthorProvided(@ModelAttribute(FORM_MODEL_NAME) SourceWizardForm form)
	throws Exception
	{
		ModelAndView mv = new ModelAndView("pages.landing.source.author.provided");
		form = multiAssetNextAsset(form);
		if (null != form) {
			AssetUse au = form.getAssetUse();
			Asset asset = assetRepository.lazyLoad(Asset.class, form.getAssetUse().getAsset().getId(), new String[]{"files"});
			au.setAsset(asset);
			mv.addObject(FORM_MODEL_NAME, form);
		} else {
		    mv = new ModelAndView("dialog.success");
			mv.addObject("message", "Information entered");
		}
		return mv;
	}

	@RequestMapping(value = "/landing/source/submit_author", method = RequestMethod.POST)
	public ModelAndView submitAuthorProvided(@ModelAttribute(FORM_MODEL_NAME) SourceWizardForm form)
	throws Exception
	{
		log.debug("submit(): submit entered for author provided...");
		// decided to reload the AssetUse in submit, because from the beginning to end might
		// take a long time and the data might have been changed

		AssetUse au = assetUseRepository.loadAssetUseById(form.getAUId());
		au = form.populateAuthorProvidedBean (au);
		au = assetUseService.saveAssetUse(au, true, false);
		updatePo(au, form);  // ignore returned PO

		// now save the uploaded files
		saveAssetFile(au.getAsset(), form);

		// temporary fix
		assetUseIndexService.updateIndex(au.getId());

		ModelAndView mv = null;
		mv = new ModelAndView("redirect:/sapp/landing/source/submit_author");
		return mv;
	}

	@RequestMapping(value = "/landing/source/submit_previous_edition", method = RequestMethod.POST)
	public ModelAndView submitPreviousEdition(@ModelAttribute(FORM_MODEL_NAME) SourceWizardForm form,
			HttpServletRequest request)
	throws Exception
	{
		log.debug("submit(): submit entered...");
		// decided to reload the AssetUse in submit, because from the beginning to end might
		// take a long time and the data might have been changed

		ModelAndView mv = new ModelAndView("pages.success");
		mv.addObject("message", "Information entered");
		ArrayList<AssetSearchView> assetSearchResults = new ArrayList<AssetSearchView>();

		if (null != form.getEdition()) {
			// does it have assets ?
			ArrayList<AssetUseSearchResult> docs = null;

			try {
				// TODO - ask Napoleon if edition is same with cwId in this case
				boolean includeCovers = request.isUserInRole(Role.EMPLOYEE_DEFAULT.getCode())
					|| request.isUserInRole(Privilege.COVER_ASSETS.getCode());
				// pass false for includeCanceled
				AssetUseSearchResults results = assetUseIndexService.getCWAssetUsesFromIndex(new Integer(form.getEdition()), includeCovers, false);
				docs = results.getDocuments();
				boolean include = true;
				for (int x = 0; x < docs.size(); x++) {
					AssetUseSearchResult doc = docs.get(x);
					if (doc.isReviewedOrAuthorUnknown()) continue;
					if (StringUtils.isNotBlank(form.getPreviousEditionRequestCriteria().getMediaType())) {
						if (doc.getMediaType().compareToIgnoreCase(form.getPreviousEditionRequestCriteria().getMediaType().trim()) != 0) {
							include = false;
						}
					}

					if (StringUtils.isNotBlank(form.getPreviousEditionRequestCriteria().getComponent())) {
						if (doc.getComponentName().compareToIgnoreCase(form.getPreviousEditionRequestCriteria().getComponent().trim()) != 0) {
							include = false;
						}
					}

					if (StringUtils.isNotBlank(form.getPreviousEditionRequestCriteria().getAssetDescription())) {
						if (doc.getDescription().indexOf(form.getPreviousEditionRequestCriteria().getAssetDescription().trim()) == -1) {
							include = false;
						}
					}

					if (StringUtils.isNotBlank(form.getPreviousEditionRequestCriteria().getUsage())) {
						if (doc.getUsage().compareToIgnoreCase(form.getPreviousEditionRequestCriteria().getUsage().trim()) != 0) {
							include = false;
						}
					}

					if (StringUtils.isNotBlank(form.getPreviousEditionRequestCriteria().getPagePosition())) {
						if (doc.getPosition().compareToIgnoreCase(form.getPreviousEditionRequestCriteria().getPagePosition().trim()) != 0) {
							include = false;
						}
					}

					if (include) {
						assetSearchResults.add(new AssetSearchView(doc));
					}

				}  // end for loop
				if (assetSearchResults.size() < 1) {
					assetSearchResults = null;
				}

				form.setAssetsFound(assetSearchResults);
				form.setAssetSelected(null);

			} catch (Exception e) {
				docs = null;
			}
			if (null == docs || docs.size() < 1) {
				// if there are no assets at all on this edition go to the save option
				mv = new ModelAndView("pages.landing.source.previous.edition.asset.save");
			} else {
				// there are assets but none match selection criteria
				form.setEditions(getEditionArray(form.getAssetUse().getCommonWork().getId()));
				mv = new ModelAndView("pages.landing.source.previous.edition.select.asset");
			}
		}

		return mv;
	}

	@RequestMapping(value = "/landing/source/submit_previous_edition_select_edition", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView submitPreviousEditionSelectEdition(@ModelAttribute(FORM_MODEL_NAME) SourceWizardForm form)
	throws Exception
	{
		ModelAndView mv = new ModelAndView("pages.landing.source.previous.edition.select.edition");
		form.populateForm();
		form.setAssetSelected(null);

		mv.addObject(FORM_MODEL_NAME, form);
		return mv;
	}

	@RequestMapping(value = "/landing/source/load_previous_edition_asset_save", method = RequestMethod.POST)
	public ModelAndView previousEditionAssetSaveLoad(@ModelAttribute(FORM_MODEL_NAME) SourceWizardForm form)
	throws NumberFormatException, PersistenceException
	{
		if (StringUtils.isNotBlank(form.getAssetSelected())) {
			Asset sAsset = getAssetRepository().loadAssetById(new Integer(form.getAssetSelected()));

			CommonWork prevEdCw = getCommonWorkRepository().loadWithPrimaryProductById(new Integer(form.getEdition()));

			form.getAssetUse().getAsset().setOriginalPublicationIsbn(prevEdCw.getPrimaryProduct().getIsbn13());
			form.getAssetUse().getAsset().setOriginalFigureNumber(sAsset.getOriginalFigureNumber());
			form.getAssetUse().getAsset().setOriginalPageNumber(sAsset.getOriginalPageNumber());
			form.getAssetUse().getAsset().setMediaType(sAsset.getMediaType());
			form.getAssetUse().getAsset().setImportSource(null);
			// pivotal request 36280059
			form.getAssetUse().setPickupISBN(prevEdCw.getPrimaryProduct().getIsbn13());
			form.getAssetUse().setPickupPage(sAsset.getOriginalPageNumber());
			form.getAssetUse().setPickupPosition(sAsset.getPositionsAsString());
			form.getAssetUse().setImportSource(null);
		}
		ModelAndView mv = new ModelAndView("pages.landing.source.previous.edition.asset.save");
		return mv;
	}

	@RequestMapping(value = "/landing/source/submit_previous_edition_asset_save", method = RequestMethod.POST)
	public ModelAndView submitPreviousEditionAssetSave(@ModelAttribute(FORM_MODEL_NAME) SourceWizardForm form)
	throws Exception
	{
		log.debug("submit(): submit entered...");

		ModelAndView mv = new ModelAndView("pages.success");

		// decided to reload the AssetUse in submit, because from the beginning to end might
		// take a long time and the data might have been changed
		AssetUse au = getAssetUseRepository().loadAssetUseById(form.getAUId());
		au = form.populatePreviousEditionBean (au);

		// pivotal request 63503420
		au.setPickup(false);
		au.setNew(false);
		au.setReusedFromPreviousEdition(true);
		au.setImportSource(null);


		au = getAssetUseService().saveAssetUse(au,true,false);

		if (StringUtils.isNotBlank(form.getAssetSelected())) {
			mv.addObject("message", "Information entered");
		} else {
			// mv = new ModelAndView("redirect:/sapp/landing/source/3rdparty_search");
			mv = new ModelAndView("redirect:/sapp/landing/source/main");
			form.setEditionUse (true);
			mv.addObject(FORM_MODEL_NAME, form);
		}

		return mv;
	}

	@RequestMapping(value = "/landing/source/submit_previous_edition_asset_search", method = RequestMethod.POST)
	public ModelAndView submitPreviousEditionAssetSearch(@ModelAttribute(FORM_MODEL_NAME) SourceWizardForm form,
			HttpServletRequest request)
	throws Exception
	{
		ModelAndView mv = new ModelAndView("pages.success");

		if (null != form.getEdition()) {
			// does it have assets ?
			ArrayList<AssetUseSearchResult> docs = null;
			ArrayList<AssetSearchView> assetSearchResults = new ArrayList<AssetSearchView>();
			try {
				boolean includeCovers = request.isUserInRole(Role.EMPLOYEE_DEFAULT.getCode())
					|| request.isUserInRole(Privilege.COVER_ASSETS.getCode());
				// pass false for includeCanceled
				AssetUseSearchResults results = getAssetUseIndexService().getCWAssetUsesFromIndex(new Integer(form.getEdition()), includeCovers, false);
				docs = results.getDocuments();
				boolean include = true;
				for (int x = 0; x < docs.size(); x++) {
					AssetUseSearchResult doc = docs.get(x);

					if (StringUtils.isNotBlank(form.getPreviousEditionRequestCriteria().getMediaType())) {
						if (doc.getMediaType().compareToIgnoreCase(form.getPreviousEditionRequestCriteria().getMediaType().trim()) != 0) {
							include = false;
						}
					}

					if (StringUtils.isNotBlank(form.getPreviousEditionRequestCriteria().getComponent())) {
						if (doc.getComponentName().compareToIgnoreCase(form.getPreviousEditionRequestCriteria().getComponent().trim()) != 0) {
							include = false;
						}
					}

					if (StringUtils.isNotBlank(form.getPreviousEditionRequestCriteria().getAssetDescription())) {
						if (doc.getDescription().indexOf(form.getPreviousEditionRequestCriteria().getAssetDescription().trim()) == -1) {
							include = false;
						}
					}

					if (StringUtils.isNotBlank(form.getPreviousEditionRequestCriteria().getUsage())) {
						if (doc.getUsage().compareToIgnoreCase(form.getPreviousEditionRequestCriteria().getUsage().trim()) != 0) {
							include = false;
						}
					}

					if (StringUtils.isNotBlank(form.getPreviousEditionRequestCriteria().getPagePosition())) {
						if (doc.getPosition().compareToIgnoreCase(form.getPreviousEditionRequestCriteria().getPagePosition().trim()) != 0) {
							include = false;
						}
					}

					if (include) {
						assetSearchResults.add(new AssetSearchView(doc));
					}

				}  // end for loop
				if (assetSearchResults.size() < 1) {
					assetSearchResults = null;
				}

				form.setAssetsFound(assetSearchResults);
				form.setAssetSelected(null);

			} catch (Exception e) {
				docs = null;
			}

			mv = new ModelAndView("pages.landing.source.previous.edition.select.asset");

	//		if (null == docs || docs.size() < 1) {
	//			mv = new ModelAndView("pages.landing.source.previous.edition.asset.save");
	//		} else {
	//			form.setEditions(getEditionArray(form.getAssetUse().getCommonWork().getId()));
	//			mv = new ModelAndView("pages.landing.source.previous.edition.select.asset");
	//		}
		}

		return mv;
	}

	private ArrayList <LabelValueBean> getEditionArray(Integer cwId) {
		ArrayList<LabelValueBean> workEditions = new ArrayList<LabelValueBean>();

		try {
			List<Object[]> list = getCommonWorkRepository().getAllEditionsByCwId(cwId);
			int max = 20;
			for (int x = 0; x < list.size(); x++ ) {
				Object[] listEntry = list.get(x);
				if (listEntry[0] != cwId ) {
					workEditions.add(new LabelValueBean(listEntry[1] + "", listEntry[0] + ""));
				}
				max--;
				if (max==0) break;
			}
			if (workEditions.size() < 1) return null;
		} catch (Exception ex) {
			log.warn("getEditionArray(): caught exception: ", ex);
			return null;
		}

		return workEditions;
	}

	private void saveAssetFile(Asset asset, SourceWizardForm form) throws Exception
	{
		// MultipartFile mpf = form.getAssetFile();
		MultipartFile mpf = form.getFiles();

		if (mpf != null && mpf.getSize() > 0) {
			AssetFile original = new AssetFile();
			original.setAsset(asset);
			original.setObjectName(mpf.getOriginalFilename());
			original.setFileFormat(mpf.getContentType());
			original.setData(mpf.getBytes());
			original.setRenditionType(RenditionType.ORIGINAL);

			getAssetService().saveAssetFile(original);
		}
	}

	private SourceWizardForm multiAssetNextAsset(SourceWizardForm form) throws Exception
	{
		// if it was the last one. This will be true if we have selected just one assetUse but we have processed that one already
		if (form.getLastAssetUse()) {
			return null;
		}

		if (!form.getMultiAsset()) {
			form.setLastAssetUse(true);
			return form;
		}

		log.debug("multiAssetNextAsset(): Current IDX " + form.getCurrentAssetIdx());
		Integer nextAssetIdx = form.getCurrentAssetIdx();
		nextAssetIdx ++;
		form.setCurrentAssetIdx(nextAssetIdx);
		log.debug("multiAssetNextAsset(): NEXT IDX " + form.getCurrentAssetIdx());

		String[] assetUseList = form.getAssetList();
		Integer auid = (new Integer(assetUseList[nextAssetIdx]));
		log.debug("multiAssetNextAsset(): ASSET ID " + auid);
		AssetUse au = getAssetUseRepository().loadAssetUseById(auid);

		form.setAUId(auid);
		form.setAssetUse(au);
		// save the original OwnerType and store it in form after populate
		String ownerType = form.getOwnerType();
		form.populateForm ();
		form.setOwnerType(ownerType);

		if (nextAssetIdx == form.getAssetList().length - 1) {
			log.debug("multiAssetNextAsset(): SET LAST ON ");
			form.setLastAssetUse(true);
		}
		return form;
	}

	// --------------------- getters and setters --------------------------

	public AssetUseRepository getAssetUseRepository() {
		return assetUseRepository;
	}

	public void setAssetUseRepository(AssetUseRepository assetUseRepository) {
		this.assetUseRepository = assetUseRepository;
	}

	public AssetUseService getAssetUseService() {
		return assetUseService;
	}

	public void setAssetUseService(AssetUseService assetUseService) {
		this.assetUseService = assetUseService;
	}

	public AssetUseIndexService getAssetUseIndexService() {
		return assetUseIndexService;
	}

	public void setAssetUseIndexService(AssetUseIndexService assetUseIndexService) {
		this.assetUseIndexService = assetUseIndexService;
	}

	public AssetRepository getAssetRepository() {
		return assetRepository;
	}

	public AssetService getAssetService() {
		return assetService;
	}

	public void setAssetService(AssetService assetService) {
		this.assetService = assetService;
	}

	public void setAssetRepository(AssetRepository assetRepository) {
		this.assetRepository = assetRepository;
	}

	public CommonWorkRepository getCommonWorkRepository() {
		return commonWorkRepository;
	}

	public void setCommonWorkRepository(CommonWorkRepository commonWorkRepository) {
		this.commonWorkRepository = commonWorkRepository;
	}

	public SourceRepository getSourceRepository() {
		return sourceRepository;
	}

	public void setSourceRepository(SourceRepository sourceRepository) {
		this.sourceRepository = sourceRepository;
	}

	public UserRepository getUserRepository() {
		return userRepository;
	}

	public void setUserRepository(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public PurchaseOrderRepository getPurchaseOrderRepository() {
		return purchaseOrderRepository;
	}

	public void setPurchaseOrderRepository(PurchaseOrderRepository poRepository) {
		this.purchaseOrderRepository = poRepository;
	}

	public CommonWorkService getCommonWorkService() {
		return commonWorkService;
	}

	public void setCommonWorkService(CommonWorkService commonWorkService) {
		this.commonWorkService = commonWorkService;
	}

	public ContractRepository getContractRepository() {
		return contractRepository;
	}

	public void setContractRepository(ContractRepository contractRepository) {
		this.contractRepository = contractRepository;
	}
}
