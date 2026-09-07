package com.wiley.permissions.web.shared.controllers.asset;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.persistence.NoResultException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.wiley.permissions.common.transformer.ObjectToJson;
import com.wiley.permissions.domain.persistence.permissions.AssetUse;
import com.wiley.permissions.domain.persistence.permissions.Role;
import com.wiley.permissions.domain.persistence.permissions.User;
import com.wiley.permissions.domain.persistence.permissions.UserToRole;
import com.wiley.permissions.persistence.PersistenceException;
import com.wiley.permissions.repositories.AssetRepository;
import com.wiley.permissions.repositories.AssetUseRepository;
import com.wiley.permissions.repositories.UserRepository;
import com.wiley.permissions.services.AssetUseService;
import com.wiley.permissions.web.shared.PermUserContext;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;

@Controller
/*@RequestMapping(value={"/asset/manageAsset"})*/
@RequestMapping
public class ManageAssetController  extends BaseAnnotatedController {

	protected static final String FORM_MODEL_NAME = "manageAssetForm";

	private final static Log log = LogFactory.getLog(ManageAssetController.class);

	private AssetUseService assetUseService;
	private AssetUseRepository assetUseRepository;
	private AssetRepository assetRepository;
	private UserRepository userRepository;

	@Override
	protected List<String> getReferenceDataNames(HttpServletRequest request) throws Exception
	{
		List<String> output = new ArrayList<String>();

		output.add("cancelReasons");
		return output;
	}

	/**
	 * This method loads also the replace form in case there is only one asset.
	 * @param auIds
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/asset/manageAsset/cancel", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView cancelForm(HttpServletRequest request, @RequestParam(value="auIds", required=true) String auIds)
			throws Exception
	{
		log.debug("cancelForm(): auIds = " + auIds);
		String[] aIds = StringUtils.split(auIds, ",");

		Integer userId = PermUserContext.getCurrentUserId(request);
		User user = userRepository.lazyLoad(User.class, userId, new String[] { "group" });
		Properties properties = new Properties();
		properties.setProperty("user_id", userId + "");
		user.setRoles(getUserRepository().loadAll(UserToRole.class, properties));

		Integer userGroupId = user.getGroup().getId();

		if (1 != aIds.length) {
			log.error("One assetUse id is required");
			return null;
		}

		AssetUse au = getRepository ().lazyLoad(AssetUse.class, new Integer (aIds[0]), new String[] {
			"asset", "asset.sources", "component", "usage", "commonWork", "userGroup", "importSource"});

		// according to JHopkin Anyone can cancel a migrated asset. Nobody can delete a migrated asset. Rightslink can be deleted.
//		if(null != au.getImportSource() && !au.getImportSource().equals(ImportSource.FROM_RIGHTS_LINK)) {
//			log.error("Imported Asset Uses May not be canceled");
//			ModelAndView mv = new ModelAndView("dialog.success");
//			mv.addObject("message", "You have no authority to cancel this Asset");
//			mv.addObject("redirectUrl", "/sapp/cwlanding/main?cwid=" + au.getCommonWork().getId());
//			return mv;
//		}
		/*if (!user.hasAuthority(Role.ADMIN) ) {
			if (null == au.getUserGroup() || au.getUserGroup().getId() != userGroupId) {
				log.error("You have no authority to cancel this Asset");
				ModelAndView mv = new ModelAndView("dialog.success");
				mv.addObject("message", "You have no authority to cancel this Asset");
				mv.addObject("redirectUrl", "/sapp/cwlanding/main?cwid=" + au.getCommonWork().getId());
				return mv;
			}
		}*/

		ModelAndView mv = new ModelAndView("pages.cancel.one.asset");

		mv.addObject("assetUse", au);
		Number count = (Number)getRepository().executeSingleResultNamedQuery("Asset.countAssetUsesInCW",
							new Object[] {au.getAsset().getId(), au.getCommonWork().getId()});
		//Added for INC_134961
		if (count.equals(0)) {
			log.error("There is no asset to cancel ....");
			}
		//End for INC_134961
		mv.addObject("multi",  (count.intValue() > 1));

		// load the replacements
		List<AssetUse> replacements = getRepository().executeMultiResultNamedQuery("AssetUse.loadUsesWithSamePositionInCW",
				new Object[] {au.getCommonWork().getId(), au.getId(), au.getPosition()});
		mv.addObject("replacements",  replacements);
		return mv;
	}

	@RequestMapping(value = "/asset/manageAsset/cancelMulti", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView cancelMulti(HttpServletRequest request, @RequestParam("auIds") String auIds) throws Exception {
		log.debug("cancelMulti(): auIds = " + auIds);
		String[] aIds = StringUtils.split(auIds, ",");
		if (aIds.length < 2) {
			log.error("This method should not be called for less than 2 assetUse ids - cancelForm() should be called.");
			return null;
		}

		Integer userId = PermUserContext.getCurrentUserId(request);
		User user = userRepository.lazyLoad(User.class, userId, new String[] { "group" });
		Properties properties = new Properties();
		properties.setProperty("user_id", userId + "");
		user.setRoles(getUserRepository().loadAll(UserToRole.class, properties));

		Integer userGroupId = user.getGroup().getId();
		// load assets
		List<AssetUse> auList = new ArrayList<AssetUse>();
		for (String id : aIds) {
			AssetUse auwk = getRepository ().lazyLoad(AssetUse.class, new Integer (id), new String[]
			      { "component", "asset.mediaType", "asset.sources","userGroup","commonWork" });
			/*if ((null != auwk.getUserGroup() && auwk.getUserGroup().getId() == userGroupId) ||
					(user.hasAuthority(Role.ADMIN) )) {*/
				auList.add(auwk);
			//}
		}
		
		//Added for INC_134961
	/*	if (auList.size() < 2) {
			log.error("You have no authority to cancel all selected assets - cancelForm() should be called.");
			ModelAndView mv = new ModelAndView("dialog.success");
			mv.addObject("message", "You have no authority to cancell all selected asses");
			Integer cwId = auList.get(0).getCommonWork().getId();
			mv.addObject("redirectUrl", "/sapp/cwlanding/main?cwid=" + cwId);
			return mv;
		} */
		//End for INC_134961

		ModelAndView mv = new ModelAndView("pages.cancel.multi.assets");
		mv.addObject("assetUses", auList);
		return mv;
	}

	@RequestMapping(value = "/asset/manageAsset/cancelMulti", method = RequestMethod.POST)
	public void cancelMultiSubmit(HttpServletRequest request, HttpServletResponse response,
			@RequestParam("auIds") Integer [] auIds,
			@RequestParam("cancelComment") String comment) // lnagy - name of the param cannot be changed
	throws Exception
	{
		log.debug("cancelMultiSubmit(): auIds = " + StringUtils.join(auIds, ", "));

		int userId = PermUserContext.getCurrentUserId(request);  // throws Exception

		List <Map<String, Object>> statuses = new ArrayList<Map<String, Object>> ();

		for (Integer auId : auIds) {
		    boolean deleted = assetUseService.cancelOrDeleteAssetUse(auId, userId, comment, null, true);
		    Map<String, Object> status = new HashMap <String, Object> ();
		    status.put("auId", auId);
		    status.put("deleted", deleted);
		    statuses.add(status);
		}

		String output = ObjectToJson.doTransform(statuses, null); // throws InitialisationException,
		log.debug("cancelMultiSubmit(): sending:\r\n" + output);

		response.setContentType("application/json");
		// Safe to set contentLength since no double-byte chars are in output
		response.setContentLength(output.length());
		response.getWriter().write(output); // throws IOException
	}

	/**
	 * this method will cancel one use if that option is selected. If replacement is selected, will also cancel the use,
	 * instead of canceling/deleting all uses
	 * If cancelAll is selected, will cancel/delete all uses (replacement should not be available in that case)
	 * @param auId
	 * @param cwId
	 * @param replacementId
	 * @param cancelAllUses
	 * @param comments
	 * @param request
	 * @param response
	 * @throws Exception
	 */
	@RequestMapping(value = "/asset/manageAsset/cancelOne", method = RequestMethod.POST)
	public void cancelOneSubmit(@RequestParam(value="auId", required=true) int auId,
							  @RequestParam(value="cwId", required=true) int cwId,
							  @RequestParam(value="replacementId", required=false) Integer replacementId,
							  @RequestParam(value="cancelR", required=false) Integer cancelAllUses,
							  @RequestParam(value="cancelComment", required=false) String comment,
							  HttpServletRequest request, HttpServletResponse response)
			throws Exception
	{
		List <Map<String, Object>> statuses = new ArrayList<Map<String, Object>> ();
		int userId = PermUserContext.getCurrentUserId(request);

		// if replacement selected or if multi uses and user selected to cancel just the selected use
		if ((null != replacementId && replacementId != 0) || (null != cancelAllUses && cancelAllUses.intValue() == 1)) {
			log.debug("cancelOneSubmit(): replacement id is available or cancel asset use selected. In this case we just mark the asset use as canceled");
			assetUseService.cancelOrDeleteAssetUse (auId, userId, comment, replacementId, false); // do not delete if empty

			Map<String, Object> status = new HashMap <String, Object> ();
			status.put("auId", auId);
			status.put("deleted", false); // not deleted, just replaced
			statuses.add(status);

		// if replacement is not selected and user chooses to cancel all uses
		} else {
			log.debug("cancelOneSubmit(): cancel/delete all usages");
			List<Integer> auIds = getRepository().executeMultiResultNamedQuery("AssetUse.loadUsesWithSameAssetInCW",  new Object[] {auId});
			for (int assetUseId : auIds) {
				Map<String, Object> status = new HashMap <String, Object> ();

				try {
					boolean deleted = assetUseService.cancelOrDeleteAssetUse(assetUseId, userId, comment, replacementId, true); // delete if empty
					status.put("auId", assetUseId);
					status.put("deleted", deleted);
				} catch (NoResultException e) {
					// if not found something might be wrong but we remove it from the table
					status.put("auId", assetUseId);
					status.put("deleted", true);
				}
				statuses.add(status);
			}
		}

		String output = ObjectToJson.doTransform(statuses, null); // throws InitialisationException,

		log.debug("cancelOneSubmit(): sending:\r\n" + output);

		response.setContentType("application/json");
		// Safe to set contentLength since no double-byte chars are in output
		response.setContentLength(output.length());
		response.getWriter().write(output); // throws IOException
	}

	@RequestMapping(value = "/asset/manageAsset/flagAsReplace", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView flagAsReplaceStart(HttpServletRequest request,
											@RequestParam(value="auId") Integer auId)
	throws Exception
	{
		log.debug("flagAsReplaceStart(): entered...");

		AssetUse au = getAssetUseRepository().lazyLoad(AssetUse.class, auId,  new String[] {
			"asset", "commonWork",  "asset.sources", "asset.mediaType", "component", "usage"});

		log.debug("processing cw:" + au.getCommonWork().getId());
		List<AssetUse> aus = getAssetUseRepository().loadAssetUseListByCWIdForReplacements(au.getCommonWork().getId(), "au.canceled = 1");
		List<AssetUse> canceledUses = new ArrayList<AssetUse>();

		for (AssetUse auwk : aus) {
			if (StringUtils.isBlank(auwk.getPosition())) continue;
			if (StringUtils.equals(au.getPosition(), auwk.getPosition())) {
				canceledUses.add(auwk);
			}
		}

		ManageAssetForm form = new ManageAssetForm();

		ModelAndView mv = new ModelAndView("pages.flag.as.replacement");
		mv.addObject("canceledUses",canceledUses);

		form.setAssetUse(au);
		form.setReplaceAll(false);
		mv.addObject(FORM_MODEL_NAME, form);
		return mv;
	}

	@RequestMapping(value = "/asset/manageAsset/flagAsReplace", method = RequestMethod.POST)
	public void flagAsReplace(HttpServletRequest request, HttpServletResponse response,
			@ModelAttribute(FORM_MODEL_NAME) ManageAssetForm form,
									  @RequestParam(value="auId") Integer auId)
	throws Exception
	{
		log.debug("flagAsReplace(): entered...");

		int userId = PermUserContext.getCurrentUserId(request);

		AssetUse au = getAssetUseRepository().loadAssetUseById(form.getAssetUse().getId());
		AssetUse  replaceAu = getAssetUseRepository().loadAssetUseById(form.getAssetBeingReplaced());
		List<AssetUse> toReplaceAus = new ArrayList<AssetUse>();
        log.debug("replace all:" + form.getReplaceAll());
		if (form.getReplaceAll() == true) {
			log.debug("replace all is true");
			List<AssetUse> ausList = getAssetUseRepository().loadAssetUseListByCWIdForReplacements(
					au.getCommonWork().getId(), "a.id = " + replaceAu.getAsset().getId() );

			if (null != ausList && ausList.size() > 1) {
				for (AssetUse auwk : ausList) {
					AssetUse wkUse = getAssetUseRepository().loadAssetUseById(auwk.getId());
					toReplaceAus.add(wkUse);
				}
			} else {
				toReplaceAus.add(replaceAu);
			}
		} else  {
			log.debug("replaceall is false");
			toReplaceAus.add(replaceAu);
		}

		ArrayList<Integer> assetIds =  tagAssetUses(toReplaceAus, au, userId);

		java.util.Collections.sort(assetIds);

		String output = ObjectToJson.doTransform(assetIds, null);
		response.setContentType("application/json");
		// Safe to set contentLength since no double-byte chars are in output
		response.setContentLength(output.length());
		response.getWriter().write(output); // throws IOException
	}


	@RequestMapping(value = "/asset/manageAsset/getReplacements", method = RequestMethod.POST)
	public ModelAndView getReplacements(HttpServletRequest request,
										@ModelAttribute(FORM_MODEL_NAME) ManageAssetForm form,
										@RequestParam(value="auId") Integer auId)
	throws Exception
	{
		log.debug("getReplacements(): entered...");

		// reload selected asset use
		 AssetUse au = getAssetUseRepository().loadAssetUseById(form.getAssetUse().getId());

		AssetUse  replaceAu = getAssetUseRepository().loadAssetUseById(form.getAssetBeingReplaced());
		List<AssetUse> toReplaceAus = new ArrayList<AssetUse>();

		if (null != form.getAssetBeingReplaced() && form.getAssetBeingReplaced() != 0) {
			List<AssetUse> ausList = getAssetUseRepository().loadAssetUseListByCWIdForReplacements(au.getCommonWork().getId(), "a.id = " + replaceAu.getAsset().getId() );
			if (null!= ausList && ausList.size() > 1) {
				for (AssetUse auwk : ausList) {
					if (StringUtils.isBlank(auwk.getPosition())) continue;
					toReplaceAus.add(auwk);
				}
			}
		}

		ModelAndView mv = new ModelAndView("pages.get.replacements");
		form.setAssetUse(au);
		form.setReplaceAll(false);
		mv.addObject("toReplaceAus",toReplaceAus);
		mv.addObject(FORM_MODEL_NAME, form);

		return mv;
	}

	public ArrayList<Integer> tagAssetUses(List<AssetUse> toReplaceAus, AssetUse referenceAu, Integer userId) throws Exception {
		log.debug("tagAssetUses() : entered...");

		User user = assetUseRepository.lazyLoad(User.class, userId, new String[] {"type"});

		ArrayList<Integer> assetUsesIds = new ArrayList<Integer>();
		// load all assets to check for existing asset uses
		List<AssetUse> existingAus = getAssetUseRepository().loadAssetUseListByCWIdForReplacements(
			referenceAu.getCommonWork().getId(), "a.id = " + referenceAu.getAsset().getId() );

		for(AssetUse au : toReplaceAus) {
			// if no position ignore it
			if (StringUtils.isBlank(au.getPosition())) continue;
			try {
				AssetUse wkUse = getAssetUseRepository().loadAssetUseById(au.getId());
				if (wkUse.isCanceled()) {
					// only need to add reference and persist
					wkUse.setCancelReplacement(referenceAu);
					wkUse.setCancelTimestamp(new Date());
					wkUse.setCancelUser(user);
					wkUse.setCancelReplacement(getCorrespondingAu(existingAus, wkUse, referenceAu));
					if (wkUse.getCancelReplacement().isJustAdded()) {
						assetUsesIds.add(new Integer("-1"));
					}
					wkUse = getAssetUseService().saveAssetUse(wkUse,true,false);
					assetUsesIds.add(wkUse.getId());
				} else {
					// need to cancel this one and possibly create a new asset use for new asset
					wkUse.setCanceled(true);
					wkUse.setCancelReplacement(referenceAu);
					wkUse.setCancelTimestamp(new Date());
					wkUse.setCancelUser(user);
					// do we need to create a new asset use?
					wkUse.setCancelReplacement(getCorrespondingAu(existingAus, wkUse, referenceAu));
					if (wkUse.getCancelReplacement().isJustAdded()) {
						assetUsesIds.add(new Integer("-1"));
					}
					wkUse = getAssetUseService().saveAssetUse(wkUse,true,false);
					assetUsesIds.add(wkUse.getId());
				}
			} catch (PersistenceException e) {
				// just ignore for now
			}
		}

		return assetUsesIds;
	}

	public AssetUse getExistingAu(List<AssetUse> currentUses, AssetUse oldAu, AssetUse refAu) {
		log.debug("doesUseExist(): entered...");
		for (AssetUse use : currentUses) {
			if (StringUtils.isBlank(use.getPosition())) continue;
			if (use.isCanceled()) continue;
			if (!use.getAsset().getId().equals(refAu.getAsset().getId())) continue;
			if (StringUtils.equals(use.getUsage().getCode(),oldAu.getUsage().getCode())
				&& StringUtils.equals(use.getPosition(),oldAu.getPosition())) {
				return use;
			}
		}
		return null;
	}

    public AssetUse getCorrespondingAu(List<AssetUse> currentUses, AssetUse thisAu ,AssetUse refAu) throws PersistenceException {
    	log.debug("getCorrespondingAu(): entered...");
    	AssetUse existing = getExistingAu(currentUses, thisAu, refAu);

    	if (null == existing) {
    		// need to create a new asset use using the canceled au data +
    		// the new (ref) asset use asset;
    		AssetUse newAu = new AssetUse(thisAu);
    		newAu.setCanceled(false);
    		newAu.setCancelComment("");
    		newAu.setCancelReplacement(null);
    		newAu.setCancelTimestamp(null);
    		newAu.setCancelUser(null);
    		newAu.setAsset(refAu.getAsset());
			try {
				newAu = getAssetUseService().saveAssetUse(newAu, true, false);
			} catch (Exception e) {
			}
			newAu.setIsJustAdded(true);
			return newAu;
    	} else {
    		return existing;
    	}
    }

	/**
	 * This method loads also the replace form in case there is only one asset.
	 * @param auIds
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/asset/manageAsset/assetsClear", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView clearAssetsForm(HttpServletRequest request, @RequestParam("auIds") String auIds)
			throws Exception
	{
		log.debug("clearAssetsForm(): auIds = " + auIds);

		Integer userId = PermUserContext.getCurrentUserId(request);
		User user = userRepository.lazyLoad(User.class, userId, new String[] { "group" });

		Properties properties = new Properties();
		properties.setProperty("user_id", userId + "");
		user.setRoles(getUserRepository().loadAll(UserToRole.class, properties));

		List<AssetUse> toClear = new ArrayList<AssetUse>();

		toClear = assetUseRepository.loadAssetUseListByIdList(auIds);

		log.debug("returned " + toClear.size() );

		if (!user.hasRole(Role.ADMIN)) {
			log.error("You have no authority to cancel this Asset");
			ModelAndView mv = new ModelAndView("dialog.success");
			mv.addObject("message", "You have no authority to cancel this Asset");
			mv.addObject("redirectUrl", "/sapp/cwlanding/main?cwid=" + toClear.get(0).getCommonWork().getId());
			return mv;
		}

		ModelAndView mv = new ModelAndView("pages.clear.assets");

		mv.addObject("toClear", toClear);
		return mv;
	}

	@RequestMapping(value = "/asset/manageAsset/assetsClear", method = RequestMethod.POST)
	public void processClearAssetsForm(HttpServletRequest request, HttpServletResponse response,
			@RequestParam("auIds") int [] auIds)
			throws Exception
	{
		log.debug("processClearAssetsForm(): auIds = " + StringUtils.join(auIds, ", "));

		// the following lists will hold ids that have actually been deleted
		// (NOT the same thing as other lists here that will hold things that MIGHT be deleted)
		// These lists are for debugging and reporting info back to the user
		ArrayList<Integer> auIdClearedList = new ArrayList<Integer>();
		ArrayList<Integer> assetIdClearedList = new ArrayList<Integer>();
		ArrayList<Integer> contractIdClearedList = new ArrayList<Integer>();
		ArrayList<Integer> poIdClearedList = new ArrayList<Integer>();

		boolean someAssetsWarning = false;

		Set<Integer> auIdsToBeClearedSet = new HashSet<Integer>();
		for (int auId : auIds) {
			auIdsToBeClearedSet.add(auId);
		}
		ArrayList<Integer> assetIdDeleteList = new ArrayList<Integer>();

		// The cwId should be the same for all auIds
		int cwId = assetUseRepository.loadCwIdForAssetUseId(auIds[0]);

		for (int auId : auIds) {
			Integer assetId = assetUseRepository.loadAssetIdForAssetUseId(auId);
			List<Integer> auIdsForAssetAndCW = assetUseRepository.loadAssetUseIdsForAssetIdAndCwId(assetId, cwId);

			if (auIdsToBeClearedSet.containsAll(auIdsForAssetAndCW)) {
				assetIdDeleteList.add(assetId);
			}
			else {
				// ignoring List<Integer> replacedIds returned from clearAssetUseById()
				// - the UI just reloads the whole table so don't need to send this info to UI
				assetUseService.clearAssetUseById(auId);
				auIdClearedList.add(auId);
			}
		}

		log.debug("processClearAssetsForm(): assetIdDeleteList = " + StringUtils.join(assetIdDeleteList, ", "));
		Set<Integer> assetIdDeleteSet = new HashSet<Integer>(assetIdDeleteList);
		ArrayList<Integer> contractIdDeleteList = new ArrayList<Integer>();

		for (Integer assetId : assetIdDeleteList) {
			boolean abort = false;
			List<Integer> contractIds = assetRepository.getContractIdsForAssetAndCW(assetId, cwId);

			if (contractIds.size() == 0) {
				deleteAssetUsesAndMaybeAsset(assetId, cwId, auIdClearedList, assetIdClearedList, contractIdClearedList, poIdClearedList);
			}
			else {
				for (int i = 0; i < contractIds.size() && !abort; i++) {
					Integer contractId = contractIds.get(i);
					List<Integer> assetIdsForContract = assetRepository.getAssetIdsForContract(contractId);
					log.debug("processClearAssetsForm(): assetIdsForContract " + contractId + " = " + StringUtils.join(assetIdsForContract, ", "));

					if (assetIdDeleteSet.containsAll(assetIdsForContract)) {
						contractIdDeleteList.add(contractId);
					}
					else {
						contractIdDeleteList.clear();
						abort = true;
					}
				}

				if (!abort) {
					for (Integer contractId : contractIdDeleteList) {
						// includes deletion of conditions, contract files, contract_2_asset, etc.
						assetRepository.deleteContract(contractId);
						contractIdClearedList.add(contractId);
					}

					contractIdDeleteList.clear();
					deleteAssetUsesAndMaybeAsset(assetId, cwId, auIdClearedList, assetIdClearedList, contractIdClearedList, poIdClearedList);
				}
				else {
					someAssetsWarning = true;
				}
			} // end else
		}

		// could send auIdClearedList to UI as JSON structured data but actually
		// the UI just reloads the whole table so don't bother
		// - Instead we will send free form userMsg string as plain text
		//String output = ObjectToJson.doTransform(auIdClearedList, null);
		//response.setContentType("application/json");
		StringBuilder userMsg = new StringBuilder();
		if (someAssetsWarning) {
			userMsg.append("Some assets were not deleted because they are in contracts "
				+ "with other assets that are not being cleared.\r\n");
		}
		userMsg.append("Deleted " + auIdClearedList.size() + " assetUses.\r\n");
		userMsg.append("Deleted " + assetIdClearedList.size() + " assets.\r\n");
		userMsg.append("Deleted " + contractIdClearedList.size() + " contracts.\r\n");
		userMsg.append("Deleted " + poIdClearedList.size() + " purchase orders.");
		// We're not saying anything about how many other objects deleted (contract_2_asset, amendments, files, etc)
		response.setContentType("text/plain");
		String output = userMsg.toString();
		log.debug("processClearAssetForm(): output: " + output);
		// Safe to set contentLength since no double-byte chars are in output
		response.setContentLength(output.length());
		response.getWriter().write(output); // throws IOException
	}

	private void deleteAssetUsesAndMaybeAsset(int assetId, int cwId, List<Integer> auIdClearedList,
			List<Integer> assetIdClearedList, List<Integer> contractIdClearedList,
			List<Integer> poIdClearedList) throws Exception
	{
		log.debug("deleteAssetUsesAndMaybeAsset(): assetId = " + assetId + ", cwId = " + cwId);
		List<Integer> auIdsForAssetAndCW = assetUseRepository.loadAssetUseIdsForAssetIdAndCwId(assetId, cwId);
		for (Integer auId : auIdsForAssetAndCW) {
			// ignoring List<Integer> replacedIds returned from clearAssetUseById()
			// - the UI just reloads the whole table so don't need to send this info to UI
			assetUseService.clearAssetUseById(auId);
			auIdClearedList.add(auId);
		}

		assetUseRepository.clearAssetDocumentationForCW(assetId, cwId, contractIdClearedList, poIdClearedList);

		if (!assetRepository.doesAssetHaveContractsOutsideCW(assetId, cwId)
			&& !assetRepository.doesAssetHavePOsOutsideCW(assetId, cwId)
			&& !assetRepository.doesAssetHaveAssetUsesOutsideCW(assetId, cwId)) {
			assetRepository.deleteAssetAndFiles(assetId);
			assetIdClearedList.add(assetId);
		}
	}

	public AssetUseService getAssetUseService() {
    	return assetUseService;
    }

	public void setAssetUseService(AssetUseService assetUseService) {
    	this.assetUseService = assetUseService;
    }

	public AssetUseRepository getAssetUseRepository() {
    	return assetUseRepository;
    }

	public void setAssetUseRepository(AssetUseRepository assetUseRepository) {
    	this.assetUseRepository = assetUseRepository;
    }

	public AssetRepository getAssetRepository() {
		return assetRepository;
	}

	public void setAssetRepository(AssetRepository assetRepository) {
		this.assetRepository = assetRepository;
	}

	public UserRepository getUserRepository() {
		return userRepository;
	}

	public void setUserRepository(UserRepository userRepository) {
		this.userRepository = userRepository;
	}
}
