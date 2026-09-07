package com.wiley.permissions.web.shared.controllers.asset;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
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

import com.wiley.permissions.common.utils.PermBaseException;
import com.wiley.permissions.domain.persistence.permissions.Asset;
import com.wiley.permissions.domain.persistence.permissions.AssetFile;
import com.wiley.permissions.domain.persistence.permissions.AssetUse;
import com.wiley.permissions.domain.persistence.permissions.Component;
import com.wiley.permissions.domain.persistence.permissions.CopyrightType;
import com.wiley.permissions.domain.persistence.permissions.OwnerType;
import com.wiley.permissions.domain.persistence.permissions.PagePosition;
import com.wiley.permissions.domain.persistence.permissions.Privilege;
import com.wiley.permissions.domain.persistence.permissions.RenditionType;
import com.wiley.permissions.domain.persistence.permissions.Role;
import com.wiley.permissions.domain.persistence.permissions.RoyaltyFreeDeal;
import com.wiley.permissions.domain.persistence.permissions.User;
import com.wiley.permissions.domain.persistence.permissions.UserGroup;
import com.wiley.permissions.persistence.PersistenceException;
import com.wiley.permissions.repositories.AssetRepository;
import com.wiley.permissions.repositories.AssetUseRepository;
import com.wiley.permissions.repositories.CommonWorkRepository;
import com.wiley.permissions.repositories.UserRepository;
import com.wiley.permissions.services.AssetService;
import com.wiley.permissions.services.AssetUseIndexService;
import com.wiley.permissions.services.AssetUseService;
import com.wiley.permissions.services.util.ServiceException;
import com.wiley.permissions.web.shared.PermUserContext;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;
import com.wiley.permissions.web.shared.util.ComponentPropertyEditor;
import com.wiley.permissions.web.shared.util.GenericFileView;
import com.wiley.sf.common.lang.StringUtil;

@Controller
/*@RequestMapping("/asset/details")*/
@RequestMapping
@SessionAttributes(value={AssetDetailsController.FORM_MODEL_NAME, AssetDetailsController.FORM_USAGE_MODEL_NAME})
public class AssetDetailsController extends BaseAnnotatedController {

	private static final Log log = LogFactory.getLog(AssetDetailsController.class);

	// when compile on command line requires this not to be private or won't compile
	// (since used above class definition)
	protected static final String FORM_MODEL_NAME = "assetDetailsForm";
	protected static final String FORM_USAGE_MODEL_NAME = "usageDetailsForm";

	private AssetUseService assetUseService;
	private AssetService assetService;

	private AssetRepository assetRepository;
	private CommonWorkRepository commonWorkRepository;
	private AssetUseRepository assetUseRepository;
	private UserRepository userRepository;
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
		output.add("currencies");
		output.add("modelReleases");
		output.add("gbpmCategories");
		//output.add("userGroups");
		return output;
	}

	/**
	 * Most of the time assetUseId will be the parameter passed,
	 * but when the user clicks on "Save and add another use" then
	 * the assetId parameter will be passed instead.
	 * (And for createAsset nothing is passed.)
	 */
	@RequestMapping(value="/asset/details/form",method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView formBackingObject(HttpServletRequest request,
			@RequestParam(value = "auId") Integer auId,
			@RequestParam(value = "usage", required=false) boolean usage,
			@RequestParam(value = "readOnly", required=false) boolean readOnly,
			@RequestParam(value = "addTo", required=false) String addToCwId,
			@RequestParam(value = "search", required=false) String search) throws Exception
	{
		log.debug("formBackingObject(): entered, auId = " + auId + ", usage = " + usage
			+ ", readOnly = " + readOnly + ", addToCwId = " + addToCwId + ", search = " + search);

		AssetUse assetUse = assetUseRepository.lazyLoad(AssetUse.class, auId, new String[] {
			"asset", "commonWork", "usage", "size", "pagePosition", "component", "cancelUser.fullName",
			"cancelReplacement.asset.sourcesAsString"
		});
		if (assetUse == null) {
			throw new PermBaseException("assetUseId of " + auId + " not found.", true);
		}

		int userGroupId = PermUserContext.getCurrentUser(request).getGroupId();
		int cwId = assetUse.getCommonWork().getId();
		if (null != addToCwId) {
			AssetUse newAssetUse = assetUseService.copyAsset (new Integer(addToCwId), cwId,
					assetUse.getId(), false, true, userGroupId, false);
			newAssetUse.setPagePosition(PagePosition.NA);
			assetUse = newAssetUse;
		}
		// lazyLoad sources
		assetUse.setAsset(assetUseRepository.lazyLoad(Asset.class, assetUse.getAsset().getId(), new String[] {"sources", "files", "copyrightType", "contracts"}));
		List<RoyaltyFreeDeal> rfDeals = assetRepository.executeMultiResultNamedQuery("Asset.activeRoyaltyFreeDeals", new Object[] {auId});

		AssetDetailsForm form = new AssetDetailsForm();
		form.setSearching("y".equals(search));
		if(assetUse.getAsset().isWillBeWorkForHire() && null == assetUse.getAsset().getCopyrightType()){
			assetUse.getAsset().setCopyrightType(CopyrightType.WILEY_OWNED_WORK_FOR_HIRE);
		}
		form.setAssetUse(assetUse);
		ModelAndView mv = null;
		if (usage) {
			if (readOnly) {
				mv = new ModelAndView("pages.asset.view.usage.details");
			} else {
				mv = new ModelAndView("pages.asset.usage.details");
			}
			boolean includeCovers = request.isUserInRole(Role.EMPLOYEE_DEFAULT.getCode())
				|| request.isUserInRole(Privilege.COVER_ASSETS.getCode());
			boolean includeAllChapters = request.isUserInRole(Role.EMPLOYEE_DEFAULT.getCode())
				|| request.isUserInRole(Privilege.EDIT_CHAPTERS.getCode());
			List<Component> components;

			// Code changes for SS task 6 starts
			List<UserGroup> userGroups = userRepository.loadUserGroupList();
			mv.addObject("userGroupList", userGroups);
			// Code changes for SS task 6 ends
			if (includeAllChapters) {
				components = commonWorkRepository.loadComponentList(cwId, includeCovers);
			}
			else {
				int authorId = PermUserContext.getCurrentUserId(request);
				components = commonWorkRepository.loadSelectedChapters(authorId, cwId, includeCovers);
			}
			mv.addObject("componentList", components);
			mv.addObject("assetFinalCost", getRepository().executeSingleResultNamedQuery(
					"Asset.finalCost", new Object[] {assetUse.getAsset().getId(), cwId}));
			mv.addObject(FORM_USAGE_MODEL_NAME, form);

			String replacementForDescriptions = assetUseRepository.loadReplacementForDescriptions(auId);
			mv.addObject("replacementForDescriptions", replacementForDescriptions);
		} else {
			if (readOnly) {
				mv = new ModelAndView("pages.asset.view.details");
			} else {
				mv = new ModelAndView("pages.asset.details");
			}
			mv.addObject("commonWorkId", cwId);
			mv.addObject(FORM_MODEL_NAME, form);
		}
		if (form.isSearching()) {
			mv.addObject("searchflag", "y");
		}
		mv.addObject("hasFile", assetUseRepository.doesLatestContractHaveFile(auId));
		mv.addObject("rfDeals", rfDeals);

		return mv;
	}

	@RequestMapping(value="/asset/details/asset_submit", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView onAssetSubmit(HttpServletRequest request,
			@RequestParam(value = "search", required=false) String search,
			@ModelAttribute(FORM_MODEL_NAME) AssetDetailsForm form, BindingResult bindingResult)
		throws Exception
	{
		log.debug("onAssetSubmit(): request params: " + StringUtil.mapToString(request.getParameterMap(), "\n"));

		AssetUse assetUse = form.getAssetUse();
		ModelAndView mv = new ModelAndView ("redirect:/sapp/asset/details/form?auId=" + assetUse.getId());

		// setter method trims to null
		assetUse.getAsset().setOriginalPublicationIsbn(assetUse.getAsset().getOriginalPublicationIsbn());

		// copyright type overrides owner type
		if (!assetUse.getAsset().getCopyrightType().equals(CopyrightType.NOT_WILEY_OWNED)) {
			assetUse.getAsset().setOwnerType(OwnerType.WILEY);
			assetUse.getAsset().setManaged(false);
			assetUse.getAsset().setRoyaltyFree(true);
		}

		if (StringUtils.isNotBlank(assetUse.getAsset().getOriginalPublicationIsbn()) &&
				assetUse.getAsset().getOriginalPublicationIsbn().length() > 13)
			bindingResult.reject(null, "size must be between 0 and 13 for Original Publication ISBN");

		if (bindingResult.hasErrors()) {
			mv.addObject("generalMessage", bindingResult.getAllErrors());
			mv.addObject(FORM_MODEL_NAME, form);
			return mv;
		}

		Integer userId = PermUserContext.getCurrentUserId(request);
		User user = getUserRepository().loadUserById(userId);

		Asset asset = assetUse.getAsset();
		log.debug("onAssetSubmit(): Asset: " + asset.toString());

		asset.setLastUpdatedUser(user);

		if (!asset.isRestrictedUse()) {
			assetUse.setRestrictedUseApproved(false);
		}

		if (assetUse.isRestrictedUseApproved()
				&& assetUse.getRestrictedUseApprovedUser() == null)
		{
			assetUse.setRestrictedUseApprovedUser(user);
			assetUse.setRestrictedUseApprovedDate(new Date());
		}

		if (!assetUse.isRestrictedUseApproved()) {
			assetUse.setRestrictedUseApprovedUser(null);
			assetUse.setRestrictedUseApprovedDate(null);
		}

		// If the user uploaded a new file, replace any/all old files
		MultipartFile mpFile = form.getAssetFile();
		if (mpFile != null && mpFile.getSize() > 0) {
			// delete all existing files - will replace below in saveAssetFile()
			assetUseRepository.deleteAllAssetFiles(assetUse.getAsset().getId());
			assetUse.getAsset().setFiles(null);
		}

		// the following call includes updating the permission status and sending
		// update Asset and AssetUse messages
		try {
			assetUse = getAssetUseService().saveAssetUse(assetUse);
			// refresh asset variable reference so when call asset.getId() below it is set
			asset = assetUse.getAsset();

			form.setAssetUse(assetUse);

			// reload or else get lazy init exception
			asset = getAssetRepository().loadAssetById(asset.getId());
			saveAssetFile(asset, form);

			// TODO change text and move it to messages
			mv.addObject("generalMessage", "Your data has been successfully saved.");

		} catch (Exception e) {
			mv.addObject("generalMessage", e.getMessage());
		}

		mv.addObject(FORM_MODEL_NAME, form);

		return mv;
	}

	@RequestMapping(value="/asset/details/usage_submit", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView onUsageSubmit(HttpServletRequest request,
			@RequestParam(value = "search", required=false) String search,
			@ModelAttribute(FORM_USAGE_MODEL_NAME) AssetDetailsForm form, BindingResult bindingResult)
		throws Exception
	{
		// log request params because even with debug mode on they won't show up
		// in the UI since a redirect is involved
		log.debug("onUsageSubmit(): request params: " + StringUtil.mapToString(request.getParameterMap(), "\n"));

		AssetUse assetUse = form.getAssetUse();
		ModelAndView mv = new ModelAndView ("redirect:/sapp/asset/details/form?usage=true&auId=" + assetUse.getId());

		assetUse.setPickupISBN(trim (assetUse.getPickupISBN()));
		assetUse.setReusedISBN(trim (assetUse.getReusedISBN()));

		if (StringUtils.isNotBlank(assetUse.getPickupISBN()) && assetUse.getPickupISBN().length() > 13)
			bindingResult.reject(null, "size must be between 0 and 13 for Pickup ISBN");
		if (StringUtils.isNotBlank(assetUse.getReusedISBN()) && assetUse.getReusedISBN().length() > 13)
			bindingResult.reject(null, "size must be between 0 and 13 for Reused ISBN");

		if (bindingResult.hasErrors()) {
			mv.addObject("generalMessage", bindingResult.getAllErrors());
			return mv;
		}

		Asset asset = assetUse.getAsset();
		log.debug("onSubmit(): Asset: " + asset.toString());

		Integer userId = PermUserContext.getCurrentUserId(request);
		User user = getUserRepository().loadUserById(userId);

		assetUse.setLastUpdatedUser(user);

		// If a Component has been chosen, id will be set, but not any
		// other fields. We would just use a Reference object but load
		// the full object because we need the externalId of the component
		// for the UpdateAssetUse message.
		Component component = assetUse.getComponent();
		if (component != null) {
			component = getAssetRepository().find(Component.class, component.getId());
			assetUse.setComponent(component);
		}
		log.debug("------------onUsageSubmit():  " + assetUse.getManuscriptPage());
		// the following call includes updating the permission status and sending
		// update Asset and AssetUse messages
		assetUse = getAssetUseService().saveAssetUse(assetUse);

		mv.addObject("generalMessage", "Your data has been successfully saved.");

		return mv;
	}

	private String trim(String isbn) {
		if (StringUtils.isBlank(isbn))
			return null;
		isbn = isbn.trim();
		isbn = isbn.replaceAll("-", "");
		isbn = isbn.replaceAll(" ", "");
		return isbn;
	}

	private void saveAssetFile(Asset asset, AssetDetailsForm form) throws Exception
	{
		MultipartFile mpf = form.getAssetFile();
		if (mpf != null && mpf.getSize() > 0) {
			AssetFile original = new AssetFile();
			original.setAsset(asset);
			original.setObjectName(mpf.getOriginalFilename());
			original.setFileFormat(mpf.getContentType());
			original.setData(mpf.getBytes());
			original.setRenditionType(RenditionType.ORIGINAL);

			getAssetService().saveAssetFile(original);

			// need to make sure to update index with asset file
			try {
				assetUseService.updateStatusForAsset(asset);
			}
			catch (Exception ex) {
				log.error("Caught exception trying to update Permission status for Asset: ", ex);
			}
		}
	}

/*	lnagy - NOT USED ANYMORE
 * @RequestMapping("/mergeAssets")
	public ModelAndView mergeAssets(HttpServletRequest request) throws Exception
	{
		log.debug("mergeAssets{} : entered...");

		getAssetRepository().mergeDuplicateAssets();

		return new ModelAndView("redirect:/sapp/product/userLanding");
	}*/

	@RequestMapping(value="/asset/details/download_file", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView downloadFile(HttpServletRequest request, @RequestParam("fileId") Integer fileId)
			throws Exception {
		log.debug("downloadFile(): entered, fileId = " + fileId);

		ModelAndView mv = new ModelAndView();
		GenericFileView view = new GenericFileView();
		AssetFile af = assetUseRepository.lazyLoad (AssetFile.class, fileId, new String[] {});

		view.setFileName(af.getObjectName());
		view.setContentType(af.getFileFormat());

		log.debug("File Data: " + af.getData().length);
		view.setData(af.getData());
		mv.setView(view);
		return mv;
	}

	@RequestMapping(value="/asset/details/deleteAssets", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView deleteAssets(@RequestParam("cwId") int cwId) throws PersistenceException, ServiceException
	{
		log.debug("deleteAssets(): entered...for cwId " + cwId);
		ModelAndView mv = new ModelAndView("redirect:/sapp/admin/main");

		try {
			getAssetUseService().deleteAssetsForCommonWork(cwId);
			// always will throw an exception (with a message how many were deleted)
		} catch (Exception e) {
			mv.addObject("generalMessage", e.getMessage());
		}
		return mv;
	}

	/**
	 * Delete AssetFile.
	 *
	 * Note this method is exactly the same as CustomAssetController.deleteOriginalFile().
	 * Perhaps some refactoring can be done.
	 */
	@RequestMapping(value = "/asset/details/delete_original_file", method = {RequestMethod.GET, RequestMethod.POST})
	public void deleteOriginalFile(HttpServletRequest request, HttpServletResponse response,
			@RequestParam("assetId") Integer assetId)
			throws Exception {
		log.debug("deleteOriginalFile(): entered..., assetId = " + assetId);

		// delete all files because we expect them all to be based on the original file
		assetUseRepository.deleteAllAssetFiles(assetId);

		AssetDetailsForm cForm = (AssetDetailsForm) request.getSession().getAttribute(FORM_MODEL_NAME);
		cForm.getAssetUse().getAsset().setFiles(null);

		log.debug("about to rebuild index for asset use:" + cForm.getAssetUse().getId() );

		try {
			assetUseIndexService.updateIndexNow(cForm.getAssetUse().getId());
		} catch (Exception ex) {  // this is not expected
			// log exception and continue
			log.error("deleteOriginalFile(): caught exception trying to update index: ", ex);
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

	public AssetRepository getAssetRepository() {
		return assetRepository;
	}

	public void setAssetRepository(AssetRepository assetRepository) {
		this.assetRepository = assetRepository;
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

	public CommonWorkRepository getCommonWorkRepository() {
		return commonWorkRepository;
	}

	public void setCommonWorkRepository(CommonWorkRepository commonWorkRepository) {
		this.commonWorkRepository = commonWorkRepository;
	}

	public AssetUseIndexService getAssetUseIndexService() {
		return assetUseIndexService;
	}

	public void setAssetUseIndexService(AssetUseIndexService assetUseIndexService) {
		this.assetUseIndexService = assetUseIndexService;
	}
}
