package com.wiley.permissions.web.shared.controllers.asset;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import com.wiley.permissions.common.transformer.ObjectToJson;
import com.wiley.permissions.domain.persistence.permissions.Address;
import com.wiley.permissions.domain.persistence.permissions.Asset;
import com.wiley.permissions.domain.persistence.permissions.AssetFile;
import com.wiley.permissions.domain.persistence.permissions.AssetUse;
import com.wiley.permissions.domain.persistence.permissions.AssetUseFile;
import com.wiley.permissions.domain.persistence.permissions.CommonWork;
import com.wiley.permissions.domain.persistence.permissions.Component;
import com.wiley.permissions.domain.persistence.permissions.Contact;
import com.wiley.permissions.domain.persistence.permissions.ContractList;
import com.wiley.permissions.domain.persistence.permissions.Currency;
import com.wiley.permissions.domain.persistence.permissions.ExtendedAssetUse;
import com.wiley.permissions.domain.persistence.permissions.MediaType;
import com.wiley.permissions.domain.persistence.permissions.ModelRelease;
import com.wiley.permissions.domain.persistence.permissions.Product;
import com.wiley.permissions.domain.persistence.permissions.PurchaseOrder;
import com.wiley.permissions.domain.persistence.permissions.PurchaseOrderList;
import com.wiley.permissions.domain.persistence.permissions.RenditionType;
import com.wiley.permissions.domain.persistence.permissions.Source;
import com.wiley.permissions.domain.persistence.permissions.User;
import com.wiley.permissions.domain.persistence.permissions.UserDefaults;
import com.wiley.permissions.persistence.PersistenceException;
import com.wiley.permissions.repositories.AssetRepository;
import com.wiley.permissions.repositories.AssetUseRepository;
import com.wiley.permissions.repositories.CommonWorkRepository;
import com.wiley.permissions.repositories.ConditionRepository;
import com.wiley.permissions.repositories.ContractRepository;
import com.wiley.permissions.repositories.PurchaseOrderRepository;
import com.wiley.permissions.repositories.SourceRepository;
import com.wiley.permissions.repositories.UserRepository;
import com.wiley.permissions.services.AssetService;
import com.wiley.permissions.services.AssetUseIndexService;
import com.wiley.permissions.services.AssetUseService;
import com.wiley.permissions.services.PurchaseOrderService;
import com.wiley.permissions.services.imports.CSFilemakerDataImportUtility;
import com.wiley.permissions.web.shared.PermUserContext;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;
import com.wiley.permissions.web.shared.controllers.UploadForm;
import com.wiley.permissions.web.shared.controllers.asset.CustomAssetForm.UsageForm;
import com.wiley.permissions.web.shared.util.ComponentPropertyEditor;
import com.wiley.permissions.web.shared.util.GenericFileView;
import com.wiley.sf.common.lang.StringUtil;

@Controller
/*@RequestMapping("/asset/custom/")*/
@RequestMapping
@SessionAttributes(value={CustomAssetController.FORM_MODEL_NAME})
public class CustomAssetController extends BaseAnnotatedController {

	private static final Log log = LogFactory.getLog(CustomAssetController.class);
	private static final long maximumAssetUseFileSize = 10 * 1024 * 1024;
	private static final long maximumAssetFileSize = 500111000;

	protected static final String FORM_MODEL_NAME = "customAssetForm";
	protected static final String FORM_MODEL_LIST = "customAssetFormList";

	private ContractRepository contractRepository;
	private PurchaseOrderRepository poRepository;
	private ConditionRepository conditionRepository;
	private CSFilemakerDataImportUtility importAssetsUtility;

	private SourceRepository sourceRepository;
	private AssetRepository assetRepository;
	private CommonWorkRepository commonWorkRepository;
	private AssetUseRepository assetUseRepository;
	private UserRepository userRepository;

	private AssetUseService assetUseService;
	private PurchaseOrderService poService;
	private AssetService assetService;
	private AssetUseIndexService assetUseIndexService;

	@Override
	@InitBinder
	public void initBinder(WebDataBinder binder) throws Exception
	{
		log.debug("initBinder(): called for objectName = " + binder.getObjectName());
		super.initBinder(binder);
		binder.registerCustomEditor(Component.class, new ComponentPropertyEditor());
	}

	@Override
	protected List<String> getReferenceDataNames(HttpServletRequest request) throws Exception
	{
		List<String> output = new ArrayList<String>();

		output.add("mediaTypes");
		output.add("ownerTypes");
		output.add("renditionTypes");
		output.add("usages");
		output.add("sizes");
		output.add("statuses");
		output.add("pagePositions");
		output.add("modelReleases");
		output.add("currencies");
		output.add("componentCategoryList");
		output.add("pagePositions");
		output.add("languages");
		output.add("gbpmCategories");
		return output;
	}

	@ModelAttribute("mysources")
	List<Source> getMySources(HttpServletRequest request) throws Exception {
		Integer userId = PermUserContext.getCurrentUserId(request);
		List<Source> profileSources = userRepository.loadFavoriteSources (userId);
		return profileSources;
	}

	@ModelAttribute("istockphoto")
	String getiStockphoto(HttpServletRequest request) throws Exception {
		List<Integer> iStockSourceIds = getSourceRepository().loadiStockphotoSourceIds ();
		String iStockIds = StringUtil.collectionToString(iStockSourceIds, ",");
		return iStockIds;
	}

	@ModelAttribute(FORM_MODEL_LIST)
	List<CustomAssetForm> getCustomAssetFormList(HttpServletRequest request) throws Exception {
		@SuppressWarnings("unchecked")
		List<CustomAssetForm> forms = (List<CustomAssetForm>) request.getSession().getAttribute(FORM_MODEL_LIST);
		if (null == forms) {
			forms = new ArrayList<CustomAssetForm>();
			request.getSession().setAttribute(FORM_MODEL_LIST, forms);
		}
		return forms;
	}

	/*@GetMapping(value = "/upload_asset_file")*/
	@RequestMapping(value="/asset/custom/upload_asset_file", method = {RequestMethod.GET, RequestMethod.POST})
	public void uploadAssetFile(HttpServletRequest request, HttpServletResponse response,
			@ModelAttribute(UploadForm.FORM_MODEL_NAME) UploadForm form)
			throws Exception {
		log.debug("uploadAssetFile(): entered...");
		MultipartFile mpf = form.getFile();
		CustomAssetForm cForm = (CustomAssetForm) request.getSession().getAttribute(FORM_MODEL_NAME);

		if (mpf != null && mpf.getSize() > 0) {
			PrintWriter writer = response.getWriter(); // throws IOException
			response.setContentType("text/plain");
			
			if (mpf.getSize() <= maximumAssetFileSize) {
				AssetFile assetFile = new AssetFile();
				// set a value so we can identify them during delete or download
				//	assetFile.setId(0 - (cForm.getFiles().size()));
				// this will be overwritten during save because we can have multiple usages, each one with
				// different auIds
				assetFile.setId(null);
				assetFile.setAsset(cForm.getExtendedAssetUse().getAsset());
				assetFile.setData(mpf.getBytes());
				assetFile.setObjectName(mpf.getOriginalFilename());
				assetFile.setFileFormat(mpf.getContentType());
				assetFile.setRenditionType(RenditionType.ORIGINAL);
				assetFile.setCreatedDate(new Date());
				assetFile.setLastUpdatedDate(new Date());
				// getAssetService().saveAssetFile(assetFile);

				cForm.setThumbnail(assetFile);
				// Asset asset = assetUseRepository.lazyLoad(Asset.class, cForm.getExtendedAssetUse().getAsset().getId(), new String[] {"sources", "files"});
				//cForm.getExtendedAssetUse().getAsset().setFiles(asset.getFiles());

				log.debug("uploadAssetFile(): File has " + mpf.getSize() + " bytes");
				writer.write("File " + mpf.getOriginalFilename() + " (" + mpf.getSize() + " bytes) uploaded ");
				//	writer.write(resultVar);
			}
			else {
				log.info("uploadAssetFile(): File TOO LARGE - has " + mpf.getSize() + " bytes");
				writer.write("File too large. Maximum size is " + maximumAssetFileSize + " bytes.");
			}
		}
	}

	/*@GetMapping(value = "/upload_file")*/
	@RequestMapping(value="/asset/custom/upload_file", method = {RequestMethod.GET, RequestMethod.POST})
	public void uploadAssetUseFile(HttpServletRequest request, HttpServletResponse response,
			@ModelAttribute(UploadForm.FORM_MODEL_NAME) UploadForm form)
			throws Exception {
		log.debug("uploadAssetUseFile(): entered...");
		MultipartFile mpf = form.getFile();
		CustomAssetForm cForm = (CustomAssetForm) request.getSession().getAttribute(FORM_MODEL_NAME);

		if (mpf != null && mpf.getSize() > 0) {
			PrintWriter writer = response.getWriter(); // throws IOException
			response.setContentType("text/plain");
			
			if (mpf.getSize() <= maximumAssetUseFileSize) {
				AssetUseFile auFile = new AssetUseFile();
				// set a value so we can identify them during delete or download
				auFile.setId(0 - cForm.getFiles().size());
				// this will be overwritten during save because we can have multiple usages, each one with
				// different auIds
				auFile.setAssetUse(cForm.getExtendedAssetUse());
				
				auFile.setFileData(mpf.getBytes());
				auFile.setFileName(mpf.getOriginalFilename());
				auFile.setMimeType(mpf.getContentType());
				cForm.getFiles().add(auFile);
				
				log.debug("uploadAssetUseFile(): File has " + mpf.getSize() + " bytes");
				writer.write("File " + mpf.getOriginalFilename() + " (" + mpf.getSize() + " bytes) uploaded");
			}
			else {
				log.info("uploadAssetUseFile(): File TOO LARGE - has " + mpf.getSize() + " bytes");
				writer.write("File too large. Maximum size is " + maximumAssetUseFileSize + " bytes.");
			}
		}
	}

	/*@GetMapping("/download_file")*/
	@RequestMapping(value="/asset/custom/download_file", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView downloadFile(HttpServletRequest request, @RequestParam("fileId") Integer fileId)
			throws Exception {
		log.debug("downloadFile(): entered...");

		ModelAndView mv = new ModelAndView();
		GenericFileView view = new GenericFileView();
		AssetUseFile auf = assetUseRepository.lazyLoad (AssetUseFile.class, fileId, new String[] {});

		view.setFileName(auf.getFileName());
		view.setContentType(auf.getMimeType());

		log.debug("downloadFile(): fileData.length: " + auf.getFileData().length);
		view.setData(auf.getFileData());
		mv.setView(view);
		return mv;
	}

	/*@GetMapping("/cdownload_file")*/
	@RequestMapping(value="/asset/custom/cdownload_file", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView downloadFileFromCache(HttpServletRequest request, @RequestParam("fileId") Integer fileId)
			throws Exception {
		log.debug("downloadFileFromCache(): entered...");

		ModelAndView mv = new ModelAndView();
		GenericFileView view = new GenericFileView();
		CustomAssetForm cForm = (CustomAssetForm) request.getSession().getAttribute(FORM_MODEL_NAME);

		for (AssetUseFile auf : cForm.getFiles()) {
			if (auf.getId() == fileId) {
				view.setFileName(auf.getFileName());
				view.setContentType(auf.getMimeType());

				log.debug("downloadFileFromCache(): fileData.length: " + auf.getFileData().length);
				view.setData(auf.getFileData());
				mv.setView(view);
			}
		}
		return mv;
	}

	/**
	 * Delete AssetUseFile.
	 */
	@RequestMapping(value = "/asset/custom/delete_file", method = {RequestMethod.GET, RequestMethod.POST})
	public void deleteFile(HttpServletRequest request, HttpServletResponse response,
			@RequestParam("fileId") Integer auFileId)
			throws Exception {
		log.debug("deleteFile(): entered..., auFileId = " + auFileId);
		CustomAssetForm cForm = (CustomAssetForm) request.getSession().getAttribute(FORM_MODEL_NAME);

		Iterator<AssetUseFile> i = cForm.getFiles().iterator();
		while (i.hasNext()) {
			AssetUseFile auf = i.next();
			if (auf.getId().equals(auFileId)) {
		//		Integer savedAuId = auf.getAssetUse().getId();
				log.debug("deleteFile(): deleting file (name description): " + auf.getFileName() + " " + auf.getDescription());
				i.remove();
				assetUseRepository.deleteAssetUseFile(auFileId);
				try {
					assetUseIndexService.updateIndex(cForm.getExtendedAssetUse().getId());
				} catch (Exception ex) {  // this is not expected
					// log exception and continue
					log.error("deleteFile(): caught exception calling updateIndex(): ", ex);
				}
			}
		}
		
		String output = "File removed";
		// If don't set contentType causes JavaScript error in Firefox
		// (either "text/plain" or "text/html" works)
		response.setContentType("text/plain");
		// Only ok to set contentLength because we know that there are no double-byte chars in output
        response.setContentLength(output.length());
		PrintWriter writer = response.getWriter(); // throws IOException
		writer.write(output);
	}

	/**
	 * Delete AssetFile.
	 * 
	 * Note this method is exactly the same as AssetDetailsController.deleteOriginalFile().
	 * Perhaps some refactoring can be done.
	 */
	@RequestMapping(value = "/asset/custom/delete_original_file", method = {RequestMethod.GET, RequestMethod.POST})
	public void deleteOriginalFile(HttpServletRequest request, HttpServletResponse response,
			@RequestParam("assetId") Integer assetId)
			throws Exception {
		log.debug("deleteOriginalFile(): entered..., assetId = " + assetId);

		// delete all files because we expect them all to be based on the original file
		assetUseRepository.deleteAllAssetFiles(assetId);

		CustomAssetForm cForm = (CustomAssetForm) request.getSession().getAttribute(FORM_MODEL_NAME);
		cForm.getExtendedAssetUse().getAsset().setFiles(null);
		
		log.debug("about to rebuild index for asset use:" + cForm.getExtendedAssetUse().getId() );
		
		try{
			getAssetUseIndexService().updateIndexNow(cForm.getExtendedAssetUse().getId());
		} catch (Exception err) {
			// log error and continue
			log.debug("ERROR:" + err.getMessage());
		}
		
		String output = "File removed";
		// If don't set contentType causes JavaScript error in Firefox
		// (either "text/plain" or "text/html" works)
		response.setContentType("text/plain");
		// Only ok to set contentLength because we know that there are no double-byte chars in output
        response.setContentLength(output.length());
		PrintWriter writer = response.getWriter(); // throws IOException
		writer.write(output);
	}

	/**
	 * the load action will load from db if not already loaded, or from cache if already loaded
	 * @param request
	 * @param forms
	 * @param id
	 * @param cwId
	 * @return
	 * @throws Exception
	 */
	/*@GetMapping("/load")*/
	@RequestMapping(value = "/asset/custom/load", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView load(HttpServletRequest request,
			@ModelAttribute(value = FORM_MODEL_LIST) List<CustomAssetForm> forms,
			@RequestParam(value = "auId", required=true) int auId,
			@RequestParam(value = "cwId", required=true) int cwId) throws Exception
	{
		log.debug("load(): entered..., auId = " + auId + ", cwId = " + cwId);
		ModelAndView mv = new ModelAndView("pages.asset.custom.details");
		// first check if the assetUse is in the cached form
		CustomAssetForm cform = getCustomAssetForm(forms, auId);
		log.debug("load(): cform : " + cform);
		if (null == cform) {
			// asset not loaded yet, we redirect to dbload
			mv = new ModelAndView("redirect:/sapp/asset/custom/dbload?auId=" + auId + "&cwId=" + cwId);
		} else {
			mv.addObject(FORM_MODEL_NAME, cform);
			mv.addObject(FORM_MODEL_LIST, forms);
		}
		mv.addObject("estimatedCost", calculateEstimatedCost (cform, forms));

		List<Component> components = getCommonWorkRepository().loadComponentList (cwId, true);
		mv.addObject("components", components);

		return mv;
	}

	/**
	 * will cache the form
	 * @param request
	 * @param response
	 * @param forms
	 * @param sourceId
	 * @param form
	 * @throws Exception
	 */
	/*@GetMapping("/cache")*/
	@RequestMapping(value = "/asset/custom/cache", method = {RequestMethod.GET, RequestMethod.POST})
	public void cache(HttpServletRequest request,
			HttpServletResponse response,
			@ModelAttribute(value = FORM_MODEL_LIST) List<CustomAssetForm> forms,
			@RequestParam(value = "sourceId", required=false) Integer sourceId,
			@ModelAttribute(FORM_MODEL_NAME) CustomAssetForm form) throws Exception
	{
		log.debug("cache(): entered..., sourceId = " + sourceId);
		cache (forms, form, sourceId, request.getParameter("sourceName"));
		response.setContentType("application/json");
		PrintWriter writer = response.getWriter(); // throws IOException
		writer.println(form.getExtendedAssetUse().getId());
	}

	/**
	 * Most of the time assetUseId will be the parameter passed,
	 * but when the user clicks on "Save and add another use" then
	 * the assetId parameter will be passed instead.
	 * (And for createAsset nothing is passed.)
	 */
	/*@GetMapping("/dbload")*/
	@RequestMapping(value = "/asset/custom/dbload", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView dbload(HttpServletRequest request,
			@RequestParam(value = "auId", required=true) int auId,
			@ModelAttribute(FORM_MODEL_LIST) List<CustomAssetForm> forms,
			@RequestParam(value = "cwId", required=true) int cwId,
			@RequestParam(value = "updateMode", required=false) String updateMode) throws Exception
	{
		log.debug("dbload(): entered..., auId = " + auId + ", cwId = " + cwId + ", updateMode = " + updateMode);

		CustomAssetForm cform = new CustomAssetForm();

		if (null != updateMode && updateMode.equals("1")) {
			cform.setInEditMode(true);
		}

		ModelAndView mv = null;
		AssetUse assetUse = null;
		ExtendedAssetUse extended = null;
		PurchaseOrder po = new PurchaseOrder();
		// if it is a new assetUse
		if (0 == auId) {
			assetUse = new AssetUse();
			assetUse.setNew(true);
			assetUse.setId(0);
			// we set negative values (forms should not be null here)
			assetUse.setAsset(new Asset());
			assetUse.getAsset().setModelRelease(ModelRelease.NOT_NEEDED); // default
			CommonWork cw = commonWorkRepository.lazyLoad(CommonWork.class, cwId, new String[] {"products"});
			assetUse.setCommonWork(cw);
			assetUse.setCustom(true);
			assetUse.getAsset().setMediaType(MediaType.PHOTO);
			po.setDate(null);
			extended = new ExtendedAssetUse(assetUse);
			int userId = PermUserContext.getCurrentUserId(request);
			po = poService.loadPurchaseOrderForm (null, cwId, null, userId);

			UserDefaults ud = userRepository.loadUserDefaults(userId);
			if (ud != null) {  // should always be non-null
				assetUse.setSize(ud.getSize());
			}

			// if we already have assets, we copy source, permissionType and po from last one
			if (forms.size() > 0) {
				CustomAssetForm lastform = forms.get(forms.size()-1);
				extended.setPermissionType(lastform.getExtendedAssetUse().getPermissionType());
				assetUse.getAsset().getSources().clear();
				assetUse.getAsset().setSources(lastform.getExtendedAssetUse().getAsset().getSources());
				assetUse.setEstimatedCurrency(lastform.getUsages().get(0).getEstimatedCurrency());
				po.setNote(lastform.getPo().getNote());
				po.setReturnAddress(lastform.getPo().getReturnAddress());
				po.setEstimatedPrice(lastform.getPo().getEstimatedPrice());
				po.setEstimatedCurrency(lastform.getPo().getEstimatedCurrency());
			} else {
				// default to user profile setting
				String currencyCode = PermUserContext.getUserSession(request).getCurrentUser().getCurrencyCode();
				Currency profileCurr = (Currency) getReferenceDataCache().getObjectByType(Currency.class, currencyCode);
				assetUse.setEstimatedCurrency(profileCurr);
				po.setEstimatedCurrency(profileCurr);
			}
			cform.addUsageForm(new UsageForm (	assetUse.getUsage(), 
												assetUse.getSize(), 
												assetUse.getComponent(),
												assetUse.getPosition(),
												assetUse.getFinalPage(),
												assetUse.getPagePosition(), 
												assetUse.getEstimatedCost(), 
												assetUse.getEstimatedCurrency()));

		// load an existing assetUse
		} else {
			assetUse = assetUseRepository.lazyLoad(AssetUse.class, auId,
				new String[] {"asset", "commonWork", "usage", "size", "pagePosition", "component", "status"});
			if (assetUse == null) {
				throw new Exception("assetUseId of " + auId + " not found.");
			}
			cform.addUsageForm(new UsageForm (assetUse.getUsage(), 
					assetUse.getSize(), 
					assetUse.getComponent(),
					assetUse.getPosition(),
					assetUse.getFinalPage(),
					assetUse.getPagePosition(), 
					assetUse.getEstimatedCost(), 
					assetUse.getEstimatedCurrency()));
			// load files
			List<AssetUseFile> auFiles = assetUseRepository.loadAssetUseFiles (assetUse.getId());
			cform.getFiles().addAll(auFiles);
			// lazyLoad sources
			Asset asset = assetUseRepository.lazyLoad(Asset.class, assetUse.getAsset().getId(), new String[] {"sources", "files"});
			assetUse.setAsset(asset);
			CommonWork cw = commonWorkRepository.lazyLoad(CommonWork.class, cwId, new String[] {"products"});
			assetUse.setCommonWork(cw);
			List<Source> sources = assetUse.getAsset().getSources();
			extended = new ExtendedAssetUse(assetUse);
			if (!CollectionUtils.isEmpty(sources)) {
				// load first source - usually if the asset was created using custom page, will have just one,
				// but multiple can be added from the product landing page
				Source source = sources.get(0);

				PurchaseOrderList pos = poRepository.loadLatestForAssetSourceCW(assetUse.getAsset().getId(), source.getId(), cwId);
				// get first po
				if (!CollectionUtils.isEmpty(pos))
					po = pos.get(0);
				po = poService.loadPurchaseOrderForm (po.getId(), cwId, source.getId(), PermUserContext.getCurrentUserId(request));
				// load the contract so the permission type will be correct (check mapCustomForm method)
				ContractList contracts = contractRepository.loadLatestForAssetSourceCW (assetUse.getAsset().getId(), source.getId(), cwId);
				if (!CollectionUtils.isEmpty(contracts))
					extended.setContract(contracts.get(0));
			}
			if (cform.getInEditMode()) {
				// AssetUse dau = assetUseRepository.loadAssetUseById(id);
				// only load other uses if not in edit mode
				log.debug("*** in edit mode ***");
			} else {
				List<AssetUse> aus = assetUseRepository.loadAssetUseListByCWIdAssetId(cwId, assetUse.getAsset().getId());
				for (AssetUse au : aus) {
					if (au.getId() != auId)
						cform.addUsageForm(new UsageForm (au.getUsage(), 
								au.getSize(), 
								au.getComponent(),
								au.getPosition(),
								au.getFinalPage(),
								au.getPagePosition(), 
								au.getEstimatedCost(), 
								au.getEstimatedCurrency()));
				}
			}
		}

		cform.setExtendedAssetUse(extended);
		cform.setPo(po);
		extended.mapCustomForm();

		if (assetUse.isCustom()) {
			mv = new ModelAndView("pages.asset.custom.details");
		} else {
			mv = new ModelAndView("pages.asset.custom.view");
			mv.addObject("generalMessage", "You may not edit assets that were not created using custom mode.");
		}
		forms.add(cform);
		mv.addObject(FORM_MODEL_NAME, cform);
		mv.addObject("estimatedCost", calculateEstimatedCost (cform, forms));
		List<Component> components = getCommonWorkRepository().loadComponentList (cwId, true);
		mv.addObject("components", components);
		return mv;
	}

	/**
	 * if the source exists, will just redirect to persist, otherwise will ask to enter source info
	 * @param request
	 * @param forms
	 * @param sourceId
	 * @param form
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value="/asset/custom/submit", method = {RequestMethod.POST, RequestMethod.GET})
	public Object onSubmit(HttpServletRequest request,
			@ModelAttribute(value = FORM_MODEL_LIST) List<CustomAssetForm> forms,
			@RequestParam(value = "sourceId", required=false) Integer sourceId,
			@ModelAttribute(FORM_MODEL_NAME) CustomAssetForm form,
			HttpServletResponse response)
		throws Exception
	{
		log.debug("onSubmit(): entered...");
		cache (forms, form, sourceId, request.getParameter("sourceName"));
		ExtendedAssetUse au = form.getExtendedAssetUse();
		// load source from last cached asset
		Source source = au.getAsset().getSource(0);
		response.setContentType("text/plain");
		PrintWriter writer = response.getWriter(); // throws IOException

		Map<String, Object> map = new HashMap<String, Object>();
		// source does not exists
		ModelAndView mv = new ModelAndView("redirect:/sapp/asset/custom/persist");
		if (null != source && null == source.getId()) {
			map.put("url", request.getContextPath() + "/sapp/asset/custom/3rdparty_newsource?sourceName=" + source.getName() + "&auId=" + au.getId());
			String output = ObjectToJson.doTransform(map, null); // throws InitialisationException,
			response.setContentType("application/json");
			writer.write(output);
			return null;
		} else {
			return mv;
		}
	}

	@RequestMapping(value = "/asset/custom/3rdparty_newsource", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView newSourceLoad(@ModelAttribute(FORM_MODEL_NAME) CustomAssetForm form)
	throws Exception
	{
		log.debug("newSourceLoad(): entered...");
		ModelAndView mv = new ModelAndView("pages.asset.custom.source.3rdparty.new");
		return mv;
	}

	@RequestMapping(value="/asset/custom/persist", method = {RequestMethod.GET, RequestMethod.POST})
	public void onPersist(HttpServletRequest request,
			@ModelAttribute(value = FORM_MODEL_LIST) List<CustomAssetForm> forms,
			HttpServletResponse response)
		throws Exception
	{
		log.debug("onPersist(): request params: " + StringUtil.mapToString(request.getParameterMap(), "\n"));
		Integer userId = PermUserContext.getCurrentUserId(request);

		User user = getUserRepository().lazyLoad (User.class, userId, new String[] {"UserDefaults"});
		// User user = getUserRepository().loadUserById(userId);

		response.setContentType("text/plain");
		PrintWriter writer = response.getWriter(); // throws IOException

		List<Integer> auids = new ArrayList<Integer>();
		Map<String, Object> map = new HashMap<String, Object>();

		String error = "";
		for (CustomAssetForm form : forms) {
			try {
				auids.addAll(persistAssetForm (form, user));
			} catch (Exception e) {
				log.error("onPersist(): Failed to persist: ", e);
				error += e.getMessage();
			}
		}
		log.debug("onPersist(): auIds returned " + auids);
		if (!StringUtils.isEmpty(error)) {
			map.put("error", error);
		}
		map.put("auids", auids);

		// if create PO we create the PO and redirect to PO
		// if the last submit set the createPO that means the CreatePO button was pressed
		CustomAssetForm lastForm = forms.get(forms.size() - 1);
		if (lastForm.isCreatePO()) {
			if (lastForm.isMakeItDefault()) {
				log.debug("return address:" + lastForm.getPo().getReturnAddress());
				try {
					if (user.getUserDefaults() == null) {
						UserDefaults ud = new UserDefaults();
						ud.setUser(user);
						user.setUserDefaults(ud);
					}
					user.getUserDefaults().setReturnAddress(lastForm.getPo().getReturnAddress());

					user = userRepository.save(user);

				} catch (Exception e) {
					log.error("onPersist(): Failed to persist User Defaults", e);
				}
			}
		}
		// if create PO was pressed
		if (lastForm.isCreatePO()) {
			try {
				// test xxxxx
		//		lastForm.getPo().setSourceContactInfo(lastForm.getSourceContactInfo());
		//		lastForm.getPo().setSourceAddressInfo(lastForm.getSourceAddressInfo());


				boolean isAddressSet = false;

				if (lastForm.getPo().getSourceContactInfo().equals("0")) {
					lastForm.getPo().setSourceContactInfo("To whom it may concern");
					Source source = getRepository().lazyLoad(Source.class, lastForm.getExtendedAssetUse().getAsset().getSource(0).getId(), new String[] {"addresses"});
					if(source.getAddresses().size() > 0) {
						lastForm.getPo().setSourceAddressInfo(source.getAddresses().get(0).getAddress().toString());
						isAddressSet = true;
					}
				} else {
					Contact contact = getRepository().find(Contact.class, new Integer(lastForm.getPo().getSourceContactInfo()));
					String addr = "";

				//	if(null != lastForm.getPo().getSource().getDisplayName()) {
				//		addr = lastForm.getPo().getSource().getDisplayName();
				//	} else {
			//			addr = lastForm.getPo().getSource().getExternalName();
				//	}

					String sourceContactInfo = addr  + "\nAttn: " + contact.getName();
				//	po.setSourceContactInfo(contact.getName());
					lastForm.getPo().setSourceContactInfo(sourceContactInfo);
					// if source address value is C:email then is the contact email
					if (null != lastForm.getPo().getSourceAddressInfo() && lastForm.getPo().getSourceAddressInfo().equals("C:email")) {
						lastForm.getPo().setSourceAddressInfo(contact.getEmail());
						isAddressSet = true;
					// if the value is C:<integer> is the contact address
					} else if (null != lastForm.getPo().getSourceAddressInfo() && lastForm.getPo().getSourceAddressInfo().contains("C:")) {
						lastForm.getPo().setSourceAddressInfo(contact.getAddress().getAddressDisplay());
						isAddressSet = true;
					}  else {
						map.put("error", "PO requires either address or contact with email address");
						throw(new Exception("PO requires either address or contact with email address"));
					}
				}

				// If no address was set already,load address and set the address info
				if (!isAddressSet && null != lastForm.getPo().getSourceAddressInfo()) {
					Address address = getRepository().find(Address.class, new Integer(lastForm.getPo().getSourceAddressInfo()));
					lastForm.getPo().setSourceAddressInfo(address.getAddressDisplay());
				}

				PurchaseOrder po = persistPO (lastForm, auids);

				CommonWork cw = commonWorkRepository.lazyLoad(CommonWork.class, po.getCommonWork().getId(), new String[] {"products"});
				Product primary = cw.getPrimaryProduct();
				map.put("url", request.getContextPath() + "/sapp/permissions/preview?cwId=" + cw.getId() + "&productId=" + primary.getId() + "&poId=" + po.getId() + "&type=po");
			} catch (Exception e) {
				log.error("onPersist(): Failed to persist PO in custom asset page", e);
				map.put("error", error);
			}
		}
		// if createContract was pressed
		if (lastForm.isCreateContract()) {
			ExtendedAssetUse eau = lastForm.getExtendedAssetUse();
			map.put("url", request.getContextPath() + "/sapp/permissions/contract/custom_start?cwId=" +
					eau.getCommonWork().getId() +
					"&currency=" + eau.getEstimatedCurrency().getCode() +
					"&sourceId=" + eau.getAsset().getSource(0).getId() +
					"&data=" + StringUtil.collectionToString(getAssetData(auids), "|"));
		}
		//skip po finish
		// lnagy - https://www.pivotaltracker.com/story/show/62204754 - fix the menus after saving quick asset
		/*
		if (lastForm.isFinish()) {
			ExtendedAssetUse eau = lastForm.getExtendedAssetUse();
			map.put("url", null);
		}
		*/

		//skip po add another
		if (lastForm.isAddAnother()) {
			ExtendedAssetUse eau = lastForm.getExtendedAssetUse();
			map.put("url", request.getContextPath() + "/sapp/asset/main/custom/first?auIds= " );
		}

		String output = ObjectToJson.doTransform(map, null); // throws InitialisationException,
		response.setContentType("application/json");
		writer.write(output);
	}


	/**
	 * Loads the assetIds for the assetUses that were saved, together with the asset price
	 * @param auids
	 * @return List<Integer>
	 * @throws Exception
	 */
	private List<Object> getAssetData (List<Integer> auids) throws Exception {
		// just for uniqueness
		List<Object> assetData = new ArrayList<Object>();
		for (Integer id : auids) {
			// TODO after Napoleon is done with changes to the import I'd like to change the persistCustomAssetUseList
			// to return the assetUse instead of id, that way I do not have to load it here again
			AssetUse assetUse = assetUseRepository.lazyLoad(AssetUse.class, id,	new String[] {"asset"});
			if (!assetData.contains(assetUse.getAsset().getId())) {
				// this data must match the same data string format used in contract/assets.jspx
				// (and is processed by ContractWizardForm.setData())
				assetData.add(assetUse.getAsset().getId());
				assetData.add(0); // price is empty
				assetData.add(0); // royalty free deal
				assetData.add("n/a");  // n/a will be converted to a blank credit line
				assetData.add("false");  // no crop
				assetData.add("false");  // no bleed
			}
		}
		return assetData;
	}

	/**
	 * saves the Purchase Order from the last page
	 * @param form
	 * @return PurchaseOrder
	 * @throws Exception
	 */
	private PurchaseOrder persistPO (CustomAssetForm form, List<Integer> auids) throws Exception {
		CommonWork cw = form.getExtendedAssetUse().getCommonWork();
		PurchaseOrder po = form.getPo();
		// set the common work
		po.setCommonWork(cw);
		// set todays date
		po.setDate(new Date());
		// set the first source
		po.setSource(form.getExtendedAssetUse().getAsset().getSource(0));
		// add the assets
		List<Asset> assets = new ArrayList<Asset> ();
		// just for uniqueness
		List<Integer> assetIds = new ArrayList<Integer>();
		double estimatedPrice = 0;
		for (Integer id : auids) {
			// TODO after Napoleon is done with changes to the import I'd like to change the persistCustomAssetUseList
			// to return the assetUse instead of id, that way I do not have to load it here again
			AssetUse assetUse = assetUseRepository.lazyLoad(AssetUse.class, id,	new String[] {"asset"});
			estimatedPrice += assetUse.getEstimatedCost();
			if (!assetIds.contains(assetUse.getAsset().getId())) {
				assets.add (assetUse.getAsset());
				assetIds.add(assetUse.getAsset().getId());
			}
		}
		po.setAssets(assets);
		// if price is not overwritten, we calculate the total
		if (po.getEstimatedPrice() == 0) {
			po.setEstimatedPrice(estimatedPrice);
		}

		po = poService.save(po);
		return po;
	}

	/**
	 * Persists the asset and asset usages that where cached in the CustomAssetForm
	 * @param form
	 * @param user
	 * @return
	 * @throws Exception
	 */
	private List<Integer> persistAssetForm (CustomAssetForm form, User user) throws Exception {
		List<UsageForm> usages = form.getUsages();

//		User wkUser = userRepository.lazyLoad(User.class, user.getId(), new String[] { "group" });

		ExtendedAssetUse au = form.getExtendedAssetUse();
		
		log.debug ("********************************");
		log.debug ("********************************");
		log.debug ("********************************");
		log.debug (au.getContract());
//		au.setUserGroup(wkUser.getGroup());
/*
		String dPage = au.getFinalPage();
		if (null != dPage) dPage = dPage.replaceAll(",,", "");
		if (dPage.indexOf(",") > -1) {
			String[] r = dPage.split(",");
			dPage = r[0];
		}
		au.setFinalPage(dPage);
		String dPosition = au.getPosition();
		if (null != dPosition) dPosition = dPosition.replaceAll(",,", "");
		if (dPosition.indexOf(",") > -1) {
			String[] r = dPosition.split(",");
			dPosition = r[0];
		}
		au.setPosition(dPosition);
*/
		int usageCount = form.getUsageCount();
		// should never be the case
		if (usageCount < 1) {
			throw new Exception ("We need at least one usage to save the asset");
		}
		// save first usage (plus asset plus contract)
		au.setUsage(usages.get(0).getUsage());
		au.setSize(usages.get(0).getSize());
		// fix null pointer exception if no components created
		au.setComponent(null == usages.get(0).getComponent() ? null : commonWorkRepository.loadComponentById(usages.get(0).getComponent().getId()));
		au.setPosition(usages.get(0).getPosition());
		au.setFinalPage(usages.get(0).getFinalPage());
		au.setPagePosition(usages.get(0).getPagePosition());
		au.setEstimatedCost(usages.get(0).getEstimatedCost());
		au.setEstimatedCurrency(usages.get(0).getEstimatedCurrency());

		Asset asset = au.getAsset();
		CommonWork cw = commonWorkRepository.lazyLoad(CommonWork.class, au.getCommonWork().getId(), new String[] {"products"});
		au.setCommonWork(cw);

		log.debug("persistAssetForm(): Asset: " + asset.toString());
		asset.setLastUpdatedUser(user);

		// do not call this before the other settings are being done because it might remove some of those settings
		au.mapCustomBean();
		List<Integer> ids = new ArrayList<Integer>();
		try {
			AssetUse newAssetUse = getImportAssetsUtility().persistCustomAssetUse(au, true);

			int id = newAssetUse.getId();
			assetUseRepository.saveAssetUseFiles (id, form.getFiles());
			ids.add(id);

			AssetUse assetUse = assetUseRepository.lazyLoad(AssetUse.class, id,
					new String[] {"asset", "commonWork", "usage", "size", "pagePosition", "component", "status"});
			if (assetUse == null) {
				throw new Exception("assetUseId of " + id + " not found.");
			}

			// lazyLoad sources
			assetUse.setAsset(assetRepository.lazyLoad(Asset.class, assetUse.getAsset().getId(), new String[] {"sources", "files", "allContracts"}));
			assetUse.setCommonWork(cw);

			if (null != form.getThumbnail()) {
				log.debug("about to save thumbnail");
				form.getThumbnail().setAsset(assetUse.getAsset());
				form.getThumbnail().setRenditionType(RenditionType.ORIGINAL);

				try {
					getAssetService().saveAssetFile(form.getThumbnail());
				} catch (Exception ex) {
					log.error("unable to persist thumbnail " + ex.getMessage());
				}
			} else {
				log.debug("No thumbnail");
			}

			// save the other usages
			log.debug("persistAssetForm():... now save usages " + usageCount);
			for (int i = 1; i < usageCount; i++) {
				ExtendedAssetUse ex = new ExtendedAssetUse(assetUse, null);
				ex.setId(null);
				ex.setExternalId(null);
				ex.setUsage(usages.get(i).getUsage());
				ex.setSize(usages.get(i).getSize());
				ex.setComponent(commonWorkRepository.loadComponentById(usages.get(i).getComponent().getId()));
				ex.setPosition(usages.get(i).getPosition());
				ex.setFinalPage(usages.get(i).getFinalPage());
				ex.setPagePosition(usages.get(i).getPagePosition());
				ex.setEstimatedCost(usages.get(i).getEstimatedCost());
				ex.setEstimatedCurrency(usages.get(i).getEstimatedCurrency());
				AssetUse nAu = getImportAssetsUtility().persistCustomAssetUse(ex, true);
				assetUseRepository.saveAssetUseFiles (nAu.getId(), form.getFiles());
				ids.add(nAu.getId());
				}
			log.debug("persistAssetForm(): auIds returned " + ids);
			return ids;
		} catch (Exception e) {
			throw new Exception("Failed to save asset [" + e.getMessage() + "]", e);
		}
	}

	/**
	 * will add all estimates for all forms except the current one
	 * @param currentForm
	 * @param forms
	 * @return
	 * @throws Exception
	 */
	private double calculateEstimatedCost(CustomAssetForm currentForm, List<CustomAssetForm> forms) throws Exception {
		log.debug("calculateEstimatedCost(): start");
		double estimatedCost = 0;
		if (null != forms) {
			for (CustomAssetForm form : forms) {
				// we skip the current page so it is easier to do the calculation
				if (currentForm == form) {
					continue;
				}
				List<UsageForm> usages = form.getUsages();
				for (UsageForm usage : usages) {
					log.debug("calculateEstimatedCost(): usage estimated cost " + usage.getEstimatedCost());
					estimatedCost += usage.getEstimatedCost();
				}
			}
		}
		log.debug("calculateEstimatedCost(): " + estimatedCost);
		return estimatedCost;
	}

	/**
	 * Check if asset form is in the cache
	 * @param forms
	 * @param auid
	 * @return CustomAssetForm
	 */
	private CustomAssetForm getCustomAssetForm (List<CustomAssetForm> forms, int auid) {
		log.debug("getCustomAssetForm(): forms.size(): " + forms.size() + " auid: " + auid);
		CustomAssetForm found = null;
		for (CustomAssetForm form : forms) {
			log.debug("getCustomAssetForm(): auid(): " + form.getExtendedAssetUse().getId());
			if (form.getExtendedAssetUse().getId() == auid)
				found = form;
		}
		if (null != found) {
			log.debug("getCustomAssetForm(): found form for id " + auid);
		}
		return found;
	}

	/**
	 * caches the last form into forms array. Adds the source also to asset if the source was not added yet
	 * @param forms
	 * @param form
	 * @param sourceId
	 * @param sourceName
	 * @throws PersistenceException
	 */
	private void cache (List<CustomAssetForm> forms, CustomAssetForm form, Integer sourceId, String sourceName) throws PersistenceException {
		ExtendedAssetUse au = form.getExtendedAssetUse();
		int oldId = au.getId();
		// if it is a new asset use we set the id = 0 - forms.size()
		if (0 == oldId) {
			au.setId(0 - forms.size());
		}
		Source source = null;
		// save the source
		if (au.isSourceRequired()) {
			if (null == sourceId || sourceId == 0) {
				if (StringUtils.isNotBlank(sourceName)) {
					source = sourceRepository.loadSourceByName(sourceName);
					if (null != source) {
						sourceId = source.getId();
					} else {
						source = new Source();
						source.setName(sourceName);
					}
				}
			} else {
				source = sourceRepository.loadSourceById(sourceId);
			}
			if (null != source) {
				au.getAsset().getSources().clear();
				au.getAsset().setSource(0, source);
			}
		}
	}



	public ContractRepository getContractRepository() {
		return contractRepository;
	}

	public void setContractRepository(ContractRepository contractRepository) {
		this.contractRepository = contractRepository;
	}

	public CSFilemakerDataImportUtility getImportAssetsUtility() {
		return importAssetsUtility;
	}

	public void setImportAssetsUtility(CSFilemakerDataImportUtility importAssetsUtility) {
		this.importAssetsUtility = importAssetsUtility;
	}

	public AssetRepository getAssetRepository() {
		return assetRepository;
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

	public AssetUseRepository getAssetUseRepository() {
		return assetUseRepository;
	}

	public void setAssetUseRepository(AssetUseRepository assetUseRepository) {
		this.assetUseRepository = assetUseRepository;
	}

	public UserRepository getUserRepository() {
		return userRepository;
	}

	public void setUserRepository(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public SourceRepository getSourceRepository() {
		return sourceRepository;
	}

	public void setSourceRepository(SourceRepository sourceRepository) {
		this.sourceRepository = sourceRepository;
	}

	public AssetUseService getAssetUseService() {
		return assetUseService;
	}

	public void setAssetUseService(AssetUseService assetUseService) {
		this.assetUseService = assetUseService;
	}

	public AssetService getAssetService() {
		return assetService;
	}

	public void setAssetService(AssetService assetService) {
		this.assetService = assetService;
	}

	public PurchaseOrderRepository getPoRepository() {
		return poRepository;
	}

	public void setPoRepository(PurchaseOrderRepository poRepository) {
		this.poRepository = poRepository;
	}

	public PurchaseOrderService getPoService() {
		return poService;
	}

	public void setPoService(PurchaseOrderService poService) {
		this.poService = poService;
	}

	public ConditionRepository getConditionRepository() {
		return conditionRepository;
	}

	public void setConditionRepository(ConditionRepository conditionRepository) {
		this.conditionRepository = conditionRepository;
	}
	
	public AssetUseIndexService getAssetUseIndexService() {
		return assetUseIndexService;
	}

	public void setAssetUseIndexService(AssetUseIndexService assetUseIndexService) {
		this.assetUseIndexService = assetUseIndexService;
	}
	
}
