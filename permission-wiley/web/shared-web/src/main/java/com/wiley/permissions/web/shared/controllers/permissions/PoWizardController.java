package com.wiley.permissions.web.shared.controllers.permissions;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.ModelAndView;

import com.wiley.permissions.common.bean.BeanUtility;
import com.wiley.permissions.common.transformer.ObjectToJson;
import com.wiley.permissions.domain.persistence.permissions.Address;
import com.wiley.permissions.domain.persistence.permissions.Asset;
import com.wiley.permissions.domain.persistence.permissions.AssetUse;
import com.wiley.permissions.domain.persistence.permissions.CommonWork;
import com.wiley.permissions.domain.persistence.permissions.Contact;
import com.wiley.permissions.domain.persistence.permissions.MasterAgreementDeal;
import com.wiley.permissions.domain.persistence.permissions.PermissionStatus;
import com.wiley.permissions.domain.persistence.permissions.Privilege;
import com.wiley.permissions.domain.persistence.permissions.Product;
import com.wiley.permissions.domain.persistence.permissions.PurchaseOrder;
import com.wiley.permissions.domain.persistence.permissions.Role;
import com.wiley.permissions.domain.persistence.permissions.Source;
import com.wiley.permissions.domain.persistence.permissions.User;
import com.wiley.permissions.domain.persistence.permissions.UserDefaults;
import com.wiley.permissions.repositories.AssetRepository;
import com.wiley.permissions.repositories.AssetUseRepository;
import com.wiley.permissions.repositories.CommonWorkRepository;
import com.wiley.permissions.repositories.ContractRepository;
import com.wiley.permissions.repositories.PurchaseOrderRepository;
import com.wiley.permissions.repositories.SourceRepository;
import com.wiley.permissions.services.PurchaseOrderService;
import com.wiley.permissions.services.view.POAssetView;
import com.wiley.permissions.web.shared.PermUserContext;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;

@Controller
/*@RequestMapping("/permissions/po")*/
@SessionAttributes(PoWizardController.FORM_NAME)
public class PoWizardController extends BaseAnnotatedController {

	// --------------------- static data ---------------------------------

	private final static Log log = LogFactory.getLog(PoWizardController.class);

	protected final static String FORM_NAME = "poWizardForm";

	// --------------------- instance data -------------------------------

	private AssetUseRepository assetUseRepository;
	private AssetRepository assetRepository;
	private PurchaseOrderRepository purchaseOrderRepository;
	private CommonWorkRepository commonWorkRepository;
	private PurchaseOrderService purchaseOrderService;
	private SourceRepository sourceRepository;
	private ContractRepository contractRepository;


	@Override
	protected List<String> getReferenceDataNames(HttpServletRequest request) throws Exception
	{
		List<String> output = new ArrayList<String>();

		output.add("currencies");
		output.add("languages");
		return output;
	}

	@ModelAttribute(FORM_NAME)
	PoWizardForm getPoWizardForm(HttpServletRequest request) throws Exception {
		PoWizardForm form = (PoWizardForm)request.getSession().getAttribute(FORM_NAME);
		if (null == form) {
			form = new PoWizardForm();
			form.setAssetRepository(assetRepository);
			request.getSession().setAttribute(FORM_NAME, form);
		}
		return form;
	}

	@RequestMapping(value = "/permissions/po/download_po_start", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView downloadPOStart(@RequestParam(value="poId") Integer poId)
	throws Exception
	{
		log.debug("downloadPOStart(): entered, poId = " + poId);
		// load minimum information here. If you need extra information,
		// load it on the specific branch (see authorProvided example)
		PoWizardForm form = new PoWizardForm();
		form.setAssetRepository(assetRepository);

		PurchaseOrder po = purchaseOrderRepository.lazyLoad (PurchaseOrder.class, poId, new String[] {"commonWork"});
		CommonWork cw = commonWorkRepository.loadWithPrimaryProductById (po.getCommonWork().getId());
		Product primary = cw.getPrimaryProduct();

		ModelAndView mv = new ModelAndView();
		//SR_330683 - New French permission letter
		if (po.isPermissionRequest()) {
			if (po.isWileyOrAuthorOwned()) {
				mv = new ModelAndView("redirect:/sapp/permissions/preview?cwId=" + cw.getId()
						+ "&productId=" + primary.getId() + "&poId=" + po.getId() + "&type=internalpr3");
			} 
			
			else {
					if(po.getLanguageCode().equalsIgnoreCase("fr")){
					
					mv = new ModelAndView("redirect:/sapp/permissions/preview?cwId=" + cw.getId()
						+ "&productId=" + primary.getId() + "&poId=" + po.getId() + "&type=frenchpr");
				}
				else {
				mv = new ModelAndView("redirect:/sapp/permissions/preview?cwId=" + cw.getId()
						+ "&productId=" + primary.getId() + "&poId=" + po.getId() + "&type=pr");
				}
			}
		} else {
			mv = new ModelAndView("redirect:/sapp/permissions/preview?cwId=" + cw.getId()
					+ "&productId=" + primary.getId() + "&poId=" + po.getId() + "&type=po");
		}
		return mv;
	}

	/*@GetMapping(value = "/download_start")*/
	@RequestMapping(value = "/permissions/po/download_start", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView downloadStart(@RequestParam(value="auId") Integer auId)
	throws Exception
	{
		log.debug("downloadStart(): entered, auId = " + auId);
		// load minimum information here. If you need extra information,
		// load it on the specific branch (see authorProvided example)
		PoWizardForm form = new PoWizardForm();
		form.setAssetRepository(assetRepository);
		form.setSelectedAuId(auId);

		ModelAndView mv = new ModelAndView("pages.po.sources");
		AssetUse au = getAssetUseRepository().loadAssetUseById(auId);
		List<Source> sources = getAssetUseRepository().loadSourceForStatus(auId, PermissionStatus.FORM_SENT_GROUP);
		if (CollectionUtils.isEmpty(sources)) {
			mv = new ModelAndView("dialog.success");
			mv.addObject("message", "No source for status unrequested");
		} else if (sources.size() == 1) {
			mv = new ModelAndView("redirect:/sapp/permissions/po/download_select_source?sourceId=" + sources.get(0).getId());
		} else {
			mv.addObject("sources", sources);
			form.setNextUrl ("/sapp/permissions/po/download_select_source");
		}
		form.setCwId (au.getCommonWork().getId());
		form.addAssetId(au.getAsset().getId());
		mv.addObject(FORM_NAME, form);

		return mv;
	}

	/*@GetMapping(value = "/rerequest_start")*/
	@RequestMapping(value = "/permissions/po/rerequest_start", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView rerequestStart(@RequestParam(value="auId") Integer auId)
	throws Exception
	{
		log.debug("rerequestStart(): entered, auId = " + auId);
		// load minimum information here. If you need extra information,
		// load it on the specific branch (see authorProvided example)
		PoWizardForm form = new PoWizardForm();
		form.setRerequest(true);
		form.setAssetRepository(assetRepository);
		form.setSelectedAuId(auId);

		ModelAndView mv = new ModelAndView("pages.po.sources");
		AssetUse au = getAssetUseRepository().loadAssetUseById(auId);
		List<Source> sources = au.getAsset().getSources();
		if (CollectionUtils.isEmpty(sources)) {
			mv = new ModelAndView("dialog.success");
			mv.addObject("message", "No source for status unrequested");
		} else if (sources.size() == 1) {
			mv = new ModelAndView("redirect:/sapp/permissions/po/select_source?sourceId=" + sources.get(0).getId());
			form.setPrevUrl (null);
		} else {
			mv.addObject("sources", sources);
			form.setNextUrl ("/sapp/permissions/po/select_source");
		}
		form.setCwId (au.getCommonWork().getId());
		form.addAssetId(au.getAsset().getId());
		mv.addObject("auId",auId);
		mv.addObject(FORM_NAME, form);

		return mv;
	}

	/*@GetMapping(value = "/asset_start")*/
	@RequestMapping(value = "/permissions/po/asset_start", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView assetStart(@RequestParam(value="auId") Integer auId)
	throws Exception
	{
		log.debug("start(): entered...");
		// load minimum information here. If you need extra information,
		// load it on the specific branch (see authorProvided example)
		PoWizardForm form = new PoWizardForm();
		form.setAssetRepository(assetRepository);
		form.setSelectedAuId(auId);

		ModelAndView mv = new ModelAndView("pages.po.sources");
		AssetUse au = getAssetUseRepository().loadAssetUseById(auId);
		List<Source> sources = getAssetUseRepository().loadSourceForStatus(auId, PermissionStatus.UNREQUESTED_GROUP);
		if (CollectionUtils.isEmpty(sources)) {
			mv = new ModelAndView("dialog.success");
			mv.addObject("message", "No source for status unrequested");
		} else if (sources.size() == 1) {
			mv = new ModelAndView("redirect:/sapp/permissions/po/select_source?sourceId=" + sources.get(0).getId());
			form.setPrevUrl (null);
		} else {
			mv.addObject("sources", sources);
			form.setNextUrl ("/sapp/permissions/po/select_source");
		}
		//Added for PaperWork -- Task C Start
		form.setWizardOwnerType(au.getWizardOwnerType());
		//Added for PaperWork -- Task C End
		form.setCwId (au.getCommonWork().getId());
		form.addAssetId(au.getAsset().getId());
		//Added to implement Build DM-374 -- Start
		if(null != au && null != au.getAsset()) {
			form.setManagerApproved(au.getAsset().isManagerApproved());
		}
		//Added to implement Build DM-374 -- End
		mv.addObject(FORM_NAME, form);
		mv.addObject("auId",auId);

		return mv;
	}

	/*@GetMapping(value = "/source_start")*/
	@RequestMapping(value = "/permissions/po/source_start", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView sourceStart(@RequestParam(value="sourceId") Integer sourceId,
			@RequestParam(value="cwId") Integer cwId)
	throws Exception
	{
		log.debug("sourceStart(): entered...");
		// load minimum information here. If you need extra information,
		// load it on the specific branch (see authorProvided example)
		PoWizardForm form = new PoWizardForm();
		form.setAssetRepository(assetRepository);
		form.setCwId(cwId);

		ModelAndView mv = new ModelAndView("redirect:/sapp/permissions/po/select_source?sourceId=" + sourceId);
		mv.addObject(FORM_NAME, form);

		return mv;
	}

	/*@GetMapping(value = "/source_start_multi")*/
	@RequestMapping(value = "/permissions/po/source_start_multi", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView multiSourceStart(HttpServletRequest request, @RequestParam(value="sIds") String sourceIds,
			@RequestParam(value="cwId") Integer cwId)
	throws Exception
	{
		log.debug("multiSourceStart(): entered...");
		// load minimum information here. If you need extra information,
		// load it on the specific branch (see authorProvided example)
		ModelAndView mv = new ModelAndView("pages.po.main");

		PoWizardForm form = new PoWizardForm();
		form.setAssetRepository(assetRepository);
		form.setCwId(cwId);

		log.debug("ids:" + sourceIds);

		String[] sIds = StringUtils.split(sourceIds, ",");

		form.setSourceIds(sIds);
		form.setMultiForm(true);
		form.setPo (purchaseOrderService.loadPurchaseOrderForm (null, form.getCwId(), null, PermUserContext.getCurrentUserId(request)));
		Source temp = new Source();
		temp.setName("multiple sources");
		// this prevents a null pointer exception in podetails
		if (!sIds[0].isEmpty()) {
			temp.setId(new Integer(sIds[0]));
		}
		form.setSource(temp);

		//xxxxxx  not sure if multi works as originally intended po/details does not get
		// sufficient information to process request.  Temporary fix is to get the first source
		// id and go to select_source for now

		form.setNextUrl("/sapp/permissions/po/details");

		form.populateNewForm();

		mv = new ModelAndView("redirect:/sapp/permissions/po/select_source?sourceId=" + temp.getId());

		mv.addObject(FORM_NAME, form);
		return mv;
	}

	/*@GetMapping(value = "/download_select_source")*/
	@RequestMapping(value = "/permissions/po/download_select_source", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView downloadSelectSource(HttpServletRequest request,
			@RequestParam(value="sourceId") Integer sourceId,
			@ModelAttribute(FORM_NAME) PoWizardForm form)
	throws Exception
	{
		log.debug("downloadSelectSource(): entered...");
		ModelAndView mv = new ModelAndView();
		if (CollectionUtils.isEmpty(form.getAssetIds())) {
			mv = new ModelAndView("dialog.success");
			mv.addObject("message", "No asset ids found");
			return mv;
		}
		List<PurchaseOrder> pos = purchaseOrderRepository.loadMostRecentPurchaseOrderList (form.getCwId(), sourceId, form.getAssetIds().get(0));
		// we should have just one po per asset per source. Anyway, we select the first one
		if (CollectionUtils.isEmpty(pos)) {
			mv = new ModelAndView("dialog.success");
			mv.addObject("message", "No permission requests found");
			return mv;
		}
		PurchaseOrder po = pos.get(0);
		// go to preview
		CommonWork cw = commonWorkRepository.loadWithPrimaryProductById (po.getCommonWork().getId());
		Product primary = cw.getPrimaryProduct();

		
		
		if (po.isPermissionRequest()) {
			if (po.isWileyOrAuthorOwned()) {
				mv = new ModelAndView("redirect:/sapp/permissions/preview?cwId=" + form.getCwId()
						+ "&productId=" + primary.getId() + "&poId=" + po.getId() + "&type=internalpr3");
			} 
			
			else {
					if(po.getLanguageCode().equalsIgnoreCase("fr")){
					
					mv = new ModelAndView("redirect:/sapp/permissions/preview?cwId=" + form.getCwId()
						+ "&productId=" + primary.getId() + "&poId=" + po.getId() + "&type=frenchpr");
				}
				else {
				mv = new ModelAndView("redirect:/sapp/permissions/preview?cwId=" + form.getCwId()
						+ "&productId=" + primary.getId() + "&poId=" + po.getId() + "&type=pr");
				}
			}
		} else {
			mv = new ModelAndView("redirect:/sapp/permissions/preview?cwId=" + form.getCwId()
					+ "&productId=" + primary.getId() + "&poId=" + po.getId() + "&type=po");
		}
		
		

		return mv;
	}

	/*@GetMapping(value = "/select_source")*/
	@RequestMapping(value = "/permissions/po/select_source", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView selectSource(HttpServletRequest request,
			@RequestParam(value="sourceId") Integer sourceId,
			@ModelAttribute(FORM_NAME) PoWizardForm form)
	throws Exception
	{
		log.debug("selectSource(): entered...");
		ModelAndView mv = new ModelAndView("pages.po.main");
		Source source = sourceRepository.lazyLoad(Source.class, sourceId, new String[] {"contacts", "addresses"});
		if (null == source) {
			mv = new ModelAndView("dialog.success");
			mv.addObject("message", "No source found");
		}
		form.setPo (purchaseOrderService.loadPurchaseOrderForm (null, form.getCwId(), sourceId, PermUserContext.getCurrentUserId(request)));
		form.setSource(source);
		form.populateNewForm();
		form.setNextUrl ("/sapp/permissions/po/submit_type");
		mv.addObject(FORM_NAME, form);
		return mv;
	}

	/*@GetMapping(value = "/edit_start")*/
	@RequestMapping(value = "/permissions/po/edit_start", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView editStart(@RequestParam(value="sourceId") Integer sourceId,
			@RequestParam(value="cwId") Integer cwId)
	throws Exception
	{
		log.debug("editStart(): entered...");
		ModelAndView mv = null;
		// load minimum information here. If you need extra information,
		// load it on the specific branch (see authorProvided example)
		PoWizardForm form = new PoWizardForm();
		form.setAssetRepository(assetRepository);
		List<PurchaseOrder> pos = purchaseOrderRepository.loadMostRecentPurchaseOrderList(cwId, sourceId, 0);
		log.debug("editStart(): " + pos);
		if (CollectionUtils.isEmpty(pos)) {
			mv = new ModelAndView("dialog.success");
			mv.addObject("message", "No permission requests found");
		} else if (pos.size() == 1) {
			mv = new ModelAndView("redirect:/sapp/permissions/po/select_po?poId=" + pos.get(0).getId());
			form.setPrevUrl("");
		} else {
			mv = new ModelAndView("pages.po.polist");
			form.setPrevUrl ("/sapp/permissions/po/edit_start?sourceId=" + sourceId + "&cwId=" + cwId);
			mv.addObject("pos", pos);
		}
		mv.addObject(FORM_NAME, form);
		return mv;
	}

	/*@GetMapping(value = "/select_po")*/
	@RequestMapping(value = "/permissions/po/select_po", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView selectPo(@RequestParam(value="poId") Integer poId,
			@ModelAttribute(FORM_NAME) PoWizardForm form)
	throws Exception
	{
		log.debug("selectPo(): entered...");
		ModelAndView mv = new ModelAndView("pages.po.main");
		PurchaseOrder po = purchaseOrderRepository.lazyLoad (PurchaseOrder.class, poId, new String[] {"source", "assets", "commonWork"});
		Source source = sourceRepository.lazyLoad(Source.class, po.getSource().getId(), new String[]{"contacts", "addresses"});
		if (null == source) {
			mv = new ModelAndView("dialog.success");
			mv.addObject("message", "No source found");
		}
		form.setSource(source);
		form.setPo(po);
		form.populateEditForm();
		form.setNextUrl ("/sapp/permissions/po/submit_type");

		mv.addObject(FORM_NAME, form);
		return mv;
	}

	/*@GetMapping(value = "/main")*/
	@RequestMapping(value = "/permissions/po/main", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView page1Load(HttpServletRequest request, @ModelAttribute(FORM_NAME) PoWizardForm form)
	throws Exception
	{
		log.debug("page1Load(): entered...");
		ModelAndView mv = new ModelAndView("pages.po.main");
		form.setNextUrl ("/sapp/permissions/po/submit_type");
		mv.addObject(FORM_NAME, form);


		String dateFormat = PermUserContext.getPickerDateFormat(request);
		mv.addObject("dateFormat",dateFormat);


		return mv;
	}

	/*@PostMapping(value = "/submit_type")*/
	@RequestMapping(value = "/permissions/po/submit_type", method = RequestMethod.POST)
	public ModelAndView page1Submit(@ModelAttribute(FORM_NAME) PoWizardForm form)
	throws Exception
	{
		log.debug("submit_type(): entered...");
		ModelAndView mv = null;
		// if outside system, do not ask for source details (https://www.pivotaltracker.com/story/show/37189593)
		if (form.getPermissionRequest() == 2) {
		// Start : Added for DM-1606
			if(form.getPo().getRequestAppliedOnline()==1)
			{
				form.getPo().setPermissionRequest(true);
			}
			else
			{
				if(form.getPo().getRequestAppliedOnline()==3)
					form.getPo().setAppliedOnline(true);
				form.getPo().setPermissionRequest(false);
			}
			// End : Added for DM-1606
			log.debug("assetsLoad(): is permission request " + form.getPo().isPermissionRequest());
			mv = new ModelAndView("redirect:/sapp/permissions/contract/source_start?sourceId=" + form.getSource().getId()
					+ "&cwId=" + form.getCwId()
					+ "&rerequest=" + form.isRerequest()
					+ "&permissionRequest=" + form.getPo().isPermissionRequest()
					+ "&auId=" + form.getSelectedAuId()
					+ "&wizardOwnerType=" + form.getWizardOwnerType()
					+ "&appliedOnline=" + form.getPo().isAppliedOnline() // Added for DM-1606
					);
			mv.addObject(FORM_NAME, form);
			return mv;

		}
		mv = new ModelAndView("redirect:/sapp/permissions/po/sourceDetails");
		return mv;
	}

	/*@GetMapping(value = "/assets")*/
	@RequestMapping(value = "/permissions/po/assets", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView assetsLoad(@ModelAttribute(FORM_NAME) PoWizardForm form, HttpServletRequest request)
	throws Exception
	{
		log.debug("assetsLoad(): entered..., form.isRerequest() = " + form.isRerequest());
		ModelAndView mv = null;

		mv = new ModelAndView("pages.po.assets");
		Source source = form.getSource();
		PermissionStatus [] statuses;
		if (form.isRerequest()) {
			statuses = PermissionStatus.REREQUEST_GROUP;
		} else {
			statuses = ArrayUtils.addAll(PermissionStatus.UNREQUESTED_GROUP, PermissionStatus.FORM_SENT_GROUP);
		}
		boolean includeCovers = request.isUserInRole(Role.EMPLOYEE_DEFAULT.getCode())
			|| request.isUserInRole(Privilege.COVER_ASSETS.getCode());
		boolean allChapters = request.isUserInRole(Role.EMPLOYEE_DEFAULT.getCode())
			|| request.isUserInRole(Privilege.EDIT_CHAPTERS.getCode());
		Integer userId = PermUserContext.getCurrentUserId(request);
		List<POAssetView> poAssetViews = purchaseOrderRepository.loadPOAssetsViewForSource(
			source.getId(), form.getCwId(), statuses, includeCovers, allChapters, userId);

		for (POAssetView poAssetView : poAssetViews) {
			int count = assetRepository.getAssetFilesCount(poAssetView.getAssetId()); //Added for DM-117
			if(count > 0) {// Added for implementing DM-117
				poAssetView.setHasAssetFiles(true);
			}
			if (form.getAssetIds().contains(poAssetView.getAssetId())) {
				poAssetView.setSelected(true);
			}
		}
		mv.addObject("assets", poAssetViews);
		// we save it in the form so we can display it in the details
		form.setAssetViewList(poAssetViews);
		List<Contact> contacts = form.getSource().getContacts();
		// select the first contact to display
		if (CollectionUtils.isNotEmpty(contacts)) {
			mv.addObject("contact", contacts.get(0));
		}

		if (poAssetViews.size() == 1) {
			// skip asset selection if only one
			Asset dAsset = getAssetRepository().loadAssetById(poAssetViews.get(0).getAssetId());
			form.getPo().getAssets().add(dAsset);
			form.addAssetId(dAsset.getId());
			mv = new ModelAndView("redirect:/sapp/permissions/po/details");
		}

		return mv;
	}

	@RequestMapping(value = "/permissions/po/submit_assets", method = RequestMethod.POST)
	public ModelAndView assetsSubmit(@ModelAttribute(FORM_NAME) PoWizardForm form)
	throws Exception
	{
		log.debug("submit_assets(): assetIds = " + StringUtils.join(form.getAssetIds(), ", "));

		if (CollectionUtils.isEmpty(form.getAssetIds())) {
			return new ModelAndView("redirect:/sapp/permissions/po/assets?generalMessage=You must select at least one Asset");
		}
		else {
			return new ModelAndView("redirect:/sapp/permissions/po/details");
		}
	}

	@RequestMapping(value = "/permissions/po/details", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView detailsLoad(@ModelAttribute(FORM_NAME) PoWizardForm form, HttpServletRequest request)
	throws Exception
	{
		log.debug("detailsLoad(): entered...");
		ModelAndView mv = new ModelAndView("pages.po.details");
		Source source = sourceRepository.lazyLoad(Source.class, form.getSource().getId(), new String[] {"contacts", "addresses", "sourceGroup" });
		form.setSource(source);

		if (null != source && null != source.getSourceGroup()) {
			Integer userId = PermUserContext.getCurrentUserId(request);
			List<MasterAgreementDeal> dDeals = contractRepository.loadMasterAgreements(source.getSourceGroup().getId(), userId, new Date());
			form.setMasterAgreementDeals(dDeals);
			if (dDeals.size() == 1) {  // otherwise leave blank until user picks a master agreement
				form.setSelectedDealPricing(dDeals.get(0).getPricingInfo());
			}
		}

		// show the asset list
		for (POAssetView poAssetView : form.getAssetViewList()) {
			int count = assetRepository.getAssetFilesCount(poAssetView.getAssetId()); //Added for DM-117
			if(count > 0) {// Added for implementing DM-117
				poAssetView.setHasAssetFiles(true);
			}
			if (form.getAssetIds().contains(poAssetView.getAssetId())) {
				poAssetView.setSelected(true);
			}
		}
		// do not set in po.estimatedPrice, because the user can do Previous and
		// select another set of assets, in that case the estimated price will not be recalculated
		mv.addObject("estimatedCost", calculateEstimatedCost (form));
		return mv;
	}

	@RequestMapping(value = "/permissions/po/submit_details", method = RequestMethod.POST)
	public ModelAndView detailsSubmit(HttpServletRequest request,
			@ModelAttribute(FORM_NAME) PoWizardForm form)
	throws Exception
	{
		log.debug("submit_details(): entered...");

		// save values as user defaults
		if (form.isMakeItDefault()) {
			Integer userId = PermUserContext.getCurrentUserId(request);
			User user = getRepository().find (User.class, userId);
			if (user.getUserDefaults() == null) {
				UserDefaults ud = new UserDefaults();
				ud.setUser(user);
				user.setUserDefaults(ud);
			}
			UserDefaults ud = user.getUserDefaults();
			ud.setReturnAddress(form.getPo().getReturnAddress());
		//	if (form.getPermissionRequest() == 1) {
				ud.setUserSignature(form.getPo().getUserSignature());
	//		}
			ud.setShowEstimatedCost(form.getPo().isShowEstimatedCost());
			getRepository().save(user);
		}

		PurchaseOrder po = form.getPo();
		boolean isAddressSet = false;
		// load contact and set the contact name in contactSourceInfo (the value that comes from jsp is the contactid or 0)
		// sourceAddressInfo possible values are : C:email, C:address, A:<integer> (addressId)
		if (po.getSourceContactInfo().equals("0")) {
			po.setSourceContactInfo("To whom it may concern");
		} else {
			Contact contact = getRepository().find(Contact.class, new Integer(po.getSourceContactInfo()));
			String sourceContactInfo = po.getSource().getDisplayName() + "\nAttn: " + contact.getName();
		//	po.setSourceContactInfo(contact.getName());
			po.setSourceContactInfo(sourceContactInfo);
			// if source address value is C:email then is the contact email
			if (po.getSourceAddressInfo().equals("C:email")) {
				po.setSourceAddressInfo(contact.getEmail());
				isAddressSet = true;
			// if the value is C:<integer> is the contact address
			} else if (po.getSourceAddressInfo().contains("C:")) {
				po.setSourceAddressInfo(contact.getAddress().getAddressDisplay());
				isAddressSet = true;
			}
		}

		// If no address was set already,load address and set the address info
		if (!isAddressSet) {
			Address address = getRepository().find(Address.class, new Integer(po.getSourceAddressInfo()));
			po.setSourceAddressInfo(address.getAddressDisplay());
		}

		return new ModelAndView("redirect:/sapp/permissions/po/preview");
	}

	@RequestMapping(value = "/permissions/po/sourceDetails", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView loadSourceDetails (HttpServletRequest request, @ModelAttribute(FORM_NAME) PoWizardForm form)
	throws Exception
	{
		ModelAndView mv;

		log.debug("loadSourceDetails(): entered...");
		if (!form.getSource().isValid()) {
			mv = new ModelAndView("pages.po.sourceDetails");
		} else {
			mv = new ModelAndView("redirect:/sapp/permissions/po/assets");
		}
		return mv;
	}

	@RequestMapping(value = "/permissions/po/preview", method = {RequestMethod.GET, RequestMethod.POST})
	public void preview(HttpServletRequest request, HttpServletResponse response,
			@ModelAttribute(FORM_NAME) PoWizardForm form)
	throws Exception
	{
		log.debug("preview(): entered...");
		form.populateBean ();

		// if multi form, deal with it separately
		if (form.isMultiForm()) {
			saveMultiForm (request, response, form);
			return;
		}
		PurchaseOrder po = form.getPo();
		po = purchaseOrderService.save(po);
		form.setPo(po);

		// update the "is attached" flag
		List<Integer> attachedAssetList = form.getAttachedAssetIds();
		if (CollectionUtils.isNotEmpty(attachedAssetList)) {
			purchaseOrderRepository.updateAttachedAssetIds (po.getId(), StringUtils.join(attachedAssetList, ','));
		}

		// create the JSON object to be sent to the dialog
		Map<String, Object> map = new HashMap <String, Object> ();

		// load all asset use ids for the assets attached to purchase order so we can refresh them in data table
		List<Integer> aids = form.getAssetIds();
		List<Integer> auids = new ArrayList<Integer> ();
		for (Integer aid : aids) {
			List<Integer> ids = assetUseRepository.loadAssetUseIdsForAssetId(aid);
			auids.addAll(ids);
		}
		map.put("auids", auids);

		// go to preview
		CommonWork cw = commonWorkRepository.loadWithPrimaryProductById (form.getCwId());
		Product primary = cw.getPrimaryProduct();

		if (form.getPermissionRequest() == 1) {
			if (po.isWileyOrAuthorOwned()) {
				map.put("url", request.getContextPath() + "/sapp/permissions/preview?cwId=" + form.getCwId()
						+ "&productId=" + primary.getId() + "&poId=" + po.getId() + "&type=internalpr3");
			} 
			else {
				 if(po.getLanguageCode().equalsIgnoreCase("fr")){
					
					map.put("url", request.getContextPath() + "/sapp/permissions/preview?cwId=" + form.getCwId()
							+ "&productId=" + primary.getId() + "&poId=" + po.getId() + "&type=frenchpr");
				}
				 else{
				
				map.put("url", request.getContextPath() + "/sapp/permissions/preview?cwId=" + form.getCwId()
						+ "&productId=" + primary.getId() + "&poId=" + po.getId() + "&type=pr");
				}
			}
		} else {
			map.put("url", request.getContextPath() + "/sapp/permissions/preview?cwId=" + form.getCwId()
					+ "&productId=" + primary.getId() + "&poId=" + po.getId() + "&type=po");
		}
		String output = ObjectToJson.doTransform(map, null);  // throws InitialisationException, TransformationException

		log.debug("preview(): sending:\r\n" + output);

		response.setContentType("application/json");
		// Don't set contentLength because String.length() is not the same as #bytes if there are double-byte chars
		//response.setContentLength(output.length());
		PrintWriter writer = response.getWriter();  // throws IOException
		writer.write(output);
	}

	/**
	 * Creates purchase orders for all sources selected. If the source has no Unused assets, then it
	 * does not create the po.
	 * @param request
	 * @param response
	 * @param form
	 * @throws Exception
	 */
	public void saveMultiForm (HttpServletRequest request, HttpServletResponse response,
			@ModelAttribute(FORM_NAME) PoWizardForm form)
	throws Exception
	{
		log.debug("saveMultiForm(): entered...");

		String[] sourceIds = form.getSourceIds();
		List<PurchaseOrder> pos = new ArrayList<PurchaseOrder> ();

		PurchaseOrder formPo = form.getPo();
		for (String sId : sourceIds) {
			// copy the bean because if we use the formPo every time, second time it tries to save is already populated with
			// merged data, so we need a fresh bean every time
			// tried to use BeanUtility.copy but did not work. Maybe fix it or improve the clone method to do deep cloning
			PurchaseOrder newPo = BeanUtility.clone (formPo, PurchaseOrder.class);
			newPo.setId(null);
			Source source = purchaseOrderRepository.find(Source.class, new Integer (sId));
			newPo.setSource(source);

			List<Asset> assets = purchaseOrderRepository.getPOUnusedAssets(0, new Integer (sId), form.getCwId());
			// if no unused assets, go to the next source
			if (CollectionUtils.isEmpty(assets))
				continue;
			else
				newPo.setAssets(assets);
			pos.add (purchaseOrderService.save(newPo));
		}
		// create the JSON object to be sent to the dialog
		Map<String, Object> map = new HashMap <String, Object> ();

		CommonWork cw = commonWorkRepository.loadWithPrimaryProductById (form.getCwId());
		Product primary = cw.getPrimaryProduct();

		if (CollectionUtils.isEmpty(pos)) {
			map.put("url", StringUtils.EMPTY);
		} else {
			String poids = "";
			for (PurchaseOrder po : pos)
				poids += po.getId() + ",";
			poids = poids.substring(0, poids.length() - 1);
			log.debug("POIDS " + poids);
			if (form.getPermissionRequest() == 1) {
				map.put("url", request.getContextPath() + "/sapp/permissions/preview?cwId=" + form.getCwId() + "&productId=" + primary.getId() + "&poId=" + poids + "&type=pr");
			} else {
				map.put("url", request.getContextPath() + "/sapp/permissions/preview?cwId=" + form.getCwId() + "&productId=" + primary.getId() + "&poId=" + poids + "&type=po");
			}
		}
		String output = ObjectToJson.doTransform(map, null);  // throws InitialisationException, TransformationException

		log.debug("saveMultiForm(): sending:\r\n" + output);

		response.setContentType("application/json");
		// Don't set contentLength because String.length() is not the same as #bytes if there are double-byte chars
		//response.setContentLength(output.length());
		PrintWriter writer = response.getWriter();  // throws IOException
		writer.write(output);
	}

	/**
	 * returns the po estimated cost if exists, if not the sum of all estimated costs of all asset uses
	 * (for assets attached to po)
	 * @param form
	 * @return double estimated cost
	 */
	private double calculateEstimatedCost (PoWizardForm form) {
		log.debug ("calculateEstimatedCost(): start");
		double estimatedCost = 0;
		// we now calculate estimated cost every time
	//	if (0 == form.getPo().getEstimatedPrice()) {
			log.debug ("calculateEstimatedCost(): po cost is 0");
			for (Integer aid : form.getAssetIds()) {
				log.debug("ASSET ID " + aid);
				List<AssetUse> assetUses = assetUseRepository.loadAssetUseListForAssetId(aid);
				for (AssetUse au : assetUses) {
					// bug https://www.pivotaltracker.com/story/show/57609504
					if (!au.isCanceled()) {
						log.debug ("calculateEstimatedCost(): asset use cost " + au.getEstimatedCost());
						estimatedCost += au.getEstimatedCost();
					}
				}
			}
	//	} else {
	//		log.debug ("calculateEstimatedCost(): po cost is NOT 0");
	//		estimatedCost = form.getPo().getEstimatedPrice();
	//	}
		return estimatedCost;
	}

	// --------------------- getters and setters --------------------------

	public AssetUseRepository getAssetUseRepository() {
		return assetUseRepository;
	}

	public void setAssetUseRepository(AssetUseRepository assetUseRepository) {
		this.assetUseRepository = assetUseRepository;
	}

	public PurchaseOrderRepository getPurchaseOrderRepository() {
		return purchaseOrderRepository;
	}

	public void setPurchaseOrderRepository(PurchaseOrderRepository purchaseOrderRepository) {
		this.purchaseOrderRepository = purchaseOrderRepository;
	}

	public PurchaseOrderService getPurchaseOrderService() {
		return purchaseOrderService;
	}

	public void setPurchaseOrderService(PurchaseOrderService purchaseOrderService) {
		this.purchaseOrderService = purchaseOrderService;
	}

	public CommonWorkRepository getCommonWorkRepository() {
		return commonWorkRepository;
	}

	public void setCommonWorkRepository(CommonWorkRepository commonWorkRepository) {
		this.commonWorkRepository = commonWorkRepository;
	}

	public AssetRepository getAssetRepository() {
		return assetRepository;
	}

	public void setAssetRepository(AssetRepository assetRepository) {
		this.assetRepository = assetRepository;
	}

	public SourceRepository getSourceRepository() {
    	return sourceRepository;
    }

	public void setSourceRepository(SourceRepository sourceRepository) {
    	this.sourceRepository = sourceRepository;
    }

	public ContractRepository getContractRepository() {
    	return contractRepository;
    }

	public void setContractRepository(ContractRepository contractRepository) {
    	this.contractRepository = contractRepository;
    }
}
