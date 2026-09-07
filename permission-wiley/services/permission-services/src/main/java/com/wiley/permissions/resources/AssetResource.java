package com.wiley.permissions.resources;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.NoResultException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.queryparser.classic.ParseException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.util.DateUtils;

import com.wiley.permissions.common.transformer.ObjectToJson;
import com.wiley.permissions.common.transformer.TransformationException;
import com.wiley.permissions.domain.persistence.permissions.Asset;
import com.wiley.permissions.domain.persistence.permissions.AssetUse;
import com.wiley.permissions.domain.persistence.permissions.CommonWork;
import com.wiley.permissions.domain.persistence.permissions.Component;
import com.wiley.permissions.domain.persistence.permissions.CopyrightType;
import com.wiley.permissions.domain.persistence.permissions.MediaType;
import com.wiley.permissions.domain.persistence.permissions.ModelRelease;
import com.wiley.permissions.domain.persistence.permissions.PermissionStatus;
import com.wiley.permissions.domain.persistence.permissions.Usage;
import com.wiley.permissions.domain.persistence.permissions.User;
import com.wiley.permissions.domain.persistence.permissions.UserDefaults;
import com.wiley.permissions.persistence.PersistenceException;
import com.wiley.permissions.repositories.AssetRepository;
import com.wiley.permissions.repositories.AssetUseRepository;
import com.wiley.permissions.repositories.CommonWorkRepository;
import com.wiley.permissions.repositories.ReferenceDataCache;
import com.wiley.permissions.repositories.UserRepository;
import com.wiley.permissions.security.web.ThreadLocalUser;
import com.wiley.permissions.security.web.UserPrincipal;
import com.wiley.permissions.services.AssetUseIndexService;
import com.wiley.permissions.services.AssetUseService;
import com.wiley.permissions.services.CommonWorkService;
import com.wiley.permissions.services.view.AssetUseTableRowView;
import com.wiley.permissions.services.view.JSONDataTableView;
import com.wiley.permissions.services.view.SourceTableRowView;

/**
 * This class is the REST service implementation
 * Uses the ProductService to make the calls, but it also transforms the results
 * to XML or JSON
 * We need this because the version of MULE we have I tested it and it does not do the
 * JSON transformation
 * Maybe MULE 3.0 will handle the JSON transformation and then we can annotate the ProductServiceImpl
 * directly as a REST resource
 * Because of cross domain requests, we need JSONP (a callback method name is passed as QueryParam)
 * @author lnagy
 */
@Path ("/")
public class AssetResource {

	private static final Log log = LogFactory.getLog(AssetResource.class);

	private AssetUseService assetUseService;
	private AssetUseIndexService assetUseIndexService;
	private CommonWorkService commonWorkService;

	private AssetUseRepository assetUseRepository;
	private AssetRepository assetRepository;
	private CommonWorkRepository commonWorkRepository;
	private UserRepository userRepository;
	private ReferenceDataCache cachedData;

	/** For testing. */
	@GET
	@Produces("text/plain; charset=UTF-8")
	@Path("/test")
	public String test() {
		log.debug("test(): called");
		SimpleDateFormat format = new SimpleDateFormat("yyyy-dd-MM HH:mm:ss");
		return "Current date/time is: " + format.format(new Date());
	}

	@GET
	@Produces("application/json")
	@Path("/test2")
	public String test2(@QueryParam("callback") String callback) throws InitialisationException, TransformationException {
		log.debug("test2(): called");
		SimpleDateFormat format = new SimpleDateFormat("yyyy-dd-MM HH:mm:ss");
		String s = "Current date/time is: " + format.format(new Date());
		return ObjectToJson.doTransform(s, callback);
	}

	/**
	 * This method is no longer used - LandingTableController does the same thing
	 * except gets the LandingFilterForm from the session to include in the index
	 * search if appropriate.
	 */
	@GET
	@Produces("application/json")
	@Path("/list/index/{cwId}")
	public String loadAssetListTableFromIndex (@PathParam("cwId") int cwId, @QueryParam("callback") String callback)
			throws PersistenceException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, InitialisationException, TransformationException, CorruptIndexException, IOException, ParseException
	{
		log.debug("loadAssetListTableFromIndex(): called with cwId = " + cwId);
		// just going to pass true for includeCovers since comment above says "this method is no longer used"
		List<AssetUseTableRowView> assetUseList = assetUseService.loadAssetListTableFromIndex(cwId, null, true);
		JSONDataTableView aaData = new JSONDataTableView();
		aaData.setTableRows(assetUseList);
		return ObjectToJson.doTransform(aaData, callback);
	}

	@GET
	@Produces("application/json")
	@Path("source/list/{cwId}/{includeCovers}")
	public String loadSourceList (@PathParam("cwId") int cwId,
			@PathParam("includeCovers") boolean includeCovers,
			@QueryParam("callback") String callback)
			throws Exception
	{
		log.debug("loadSourceList(): called with cwId = " + cwId + ", includeCovers = " + includeCovers);
		List<SourceTableRowView> sourcesList = assetUseService.loadSourceListView(cwId, includeCovers);
		JSONDataTableView aaData = new JSONDataTableView();
		aaData.setTableRows(sourcesList);
		return ObjectToJson.doTransform(aaData, callback);
	}

	// load a list of assets (to refresh rows)
	@GET
	@Produces("application/json")
	@Path("/load/{cwId}")
	public String loadAssetUses (@PathParam("cwId") int cwId,
			@QueryParam("ids") String ids,
			@QueryParam("callback") String callback)
			throws Exception
	{
		log.debug("loadAssetUses(): called with cwId = " + cwId + ", ids = " + ids);
		List <Map<String, Object>> statuses = new ArrayList<Map<String, Object>> ();
		if (StringUtils.isBlank(ids)) {
			log.debug("loadAssetUses(): no ids received");
			return ObjectToJson.doTransform(statuses, callback);
		}

		String[] aIds = StringUtils.split(ids, ",");
		for (String id : aIds) {
			Map<String, Object> status = new HashMap <String, Object> ();

			int auId = new Integer (id.trim());
			status.put("auId", auId);
			status.put("aData", assetUseService.loadAssetUseTableRow(auId, cwId));
			statuses.add(status);
		}
		return ObjectToJson.doTransform(statuses, callback);
	}

	@GET
	@Produces("application/json")
	@Path("/edit/{cwId}/{authorId}/{includeCovers}/{auId}")
	public String loadAssetUseForm (@PathParam("cwId") int cwId, @PathParam("authorId") int authorId,
			@PathParam("includeCovers") boolean includeCovers,
			@PathParam("auId") int auId, @QueryParam("callback") String callback)
			throws Exception
	{
		log.debug("loadAssetUseForm(): called with cwId = " + cwId + ", includeCovers = " + includeCovers);
		AssetUseTableRowView assetUseRow = assetUseService.loadAssetUseForEdit(auId, cwId);
		// if authorId is not specified, we load all components, otherwise only those assigned to user for selected common work
		if (0 == authorId)
			assetUseRow.setComponents(commonWorkRepository.loadComponentList(cwId, includeCovers));
		else {
			// loadSelectedChapters will only return components in author_2_component so don't need to worry about covers
			assetUseRow.setComponents(commonWorkRepository.loadSelectedChapters(authorId, cwId, includeCovers));
		}
		return ObjectToJson.doTransform(assetUseRow, callback);
	}

	@GET
	@Produces("application/json")
	@Path("/status/{auId}")
	public String loadAssetStatusDescription (@PathParam("auId") int auId, @QueryParam("callback") String callback)
			throws Exception
	{
		log.debug("loadAssetStatusDescription(): called with auId = " + auId);
		AssetUse au = assetUseRepository.lazyLoad(AssetUse.class, auId, new String[] {
			"status", "cancelUser.fullName", "cancelReplacement.componentName", "cancelReplacement.position",
			"cancelReplacement.asset"
		});
		Map<String, String> status = new HashMap <String, String> ();
		if (PermissionStatus.isCanceled(au.getStatus())) {
			status.put("explanation", getCancelledString(au));
		} else {
			status.put("explanation", au.getStatusExplanation());
		}
		return ObjectToJson.doTransform(status, callback);
	}

	@GET
	@Produces("application/json")
	@Path("/comments/{auId}")
	public String loadAssetUseComments (@PathParam("auId") int auId, @QueryParam("callback") String callback)
			throws Exception
		{
		log.debug("loadAssetUseComments(): called with auId = " + auId);
		AssetUse au = assetUseRepository.loadAssetUseById(auId);
		Map<String, String> comments = new HashMap <String, String> ();
		String comment = "";
		if (null != au.getPermissionComment()) comment = comment + "Permission Comments: " + au.getPermissionComment() + "<br/>";
		if (null != au.getProductionComment()) comment = comment + "Production Comments: " + au.getProductionComment();
		if(comment.length() == 0) comment = "No Comments Available";

		comments.put("comments", comment);

		return ObjectToJson.doTransform(comments, callback);
	}

	/**
	 * returns a status message for cancelled and replaced asset uses
	 * @param au
	 * @return String - the status message that will be displayed
	 */
	public String getCancelledString (AssetUse au) {
		log.debug("getCancelledString(): called with AssetUse.id: " + au.getId());
		StringBuilder sb = new StringBuilder();
		try {
			if (null != au.getCancelUser()) {
				sb.append(au.getCancelUser().getFullName());
			} else {
				sb.append ("System");
			}
			sb.append (" cancelled use on ");
			if (null == au.getCancelTimestamp()) {
				sb.append ("N/A");
			} else {
				sb.append (DateUtils.formatTimeStamp(au.getCancelTimestamp(), "MM/dd/yyyy"));
			}
			sb.append (" with notes ");
			if (null == au.getCancelComment()) {
				sb.append ("N/A");
			} else {
				sb.append (au.getCancelComment());
			}
			if (null != au.getCancelReplacement()) {
				AssetUse replace = au.getCancelReplacement();
				sb.append (" and was replaced by " + replace.getComponentName() + " " + replace.getPosition() + " " + replace.getAsset().getDescription() );
			}
		} catch (Exception e) {
			log.debug ("getCancelledString(): caught Exception: ", e);
			sb.append ("Failed to load status explanation");
		}
		return sb.toString();
	}

	// @POST will be better but I could not get POST to work with cross domain requests
	// if it is @POST we can use Object as method parameter
	// for now we need to list every single possible field send by the page
	// save will return back the new values, so we do not do another call to the server to load them
	@GET
	@Produces("application/json")
	@Path("/save/{cwId}/{auId}")
	public String saveAssetUseForm (@PathParam("cwId") int cwId,
			@PathParam("auId") int auId,
			@QueryParam("userId") int userId,
			@QueryParam("componentId") int componentId,
			@QueryParam("manuscriptPage") String manuscriptPage,
			@QueryParam("description") String description,
			//Added to make Photographer column editable
			@QueryParam("artist") String artist,
			@QueryParam("usageCode") String usageCode,
			@QueryParam("mediaTypeCode") String mediaTypeCode,
			@QueryParam("position") String position,
			@QueryParam("finalPage") String finalPage,
			@QueryParam("sourceRefNumber") String sourceRefNumber,
			@QueryParam("creditLine") String creditLine,
			@QueryParam("pickup") Integer pickup,
			@QueryParam("royaltyFree") Integer royaltyFree,
			@QueryParam("cameraCopyToCome") Integer cameraCopyToCome,
			@QueryParam("sentToProduction") Integer sentToProduction,
			@QueryParam("permissionComment") String permissionComment,
			@QueryParam("sortOrder") String sortOrder,
			@QueryParam("gbpmCategory") Integer gbpmCategory,
			@QueryParam("mediaManager") Integer mediaManager,
			@QueryParam("callback") String callback)
			throws Exception
	{
		log.debug("saveAssetUseForm(): cwId = " + cwId + ", auId = " + auId + ", userId = " + userId+ ", finalPage = " + finalPage+", position = " + position);

		UserPrincipal userPrincipal = new UserPrincipal();
		userPrincipal.setId(userId);
		User user = userRepository.lazyLoad(User.class, userId, new String[] { "group" });
		userPrincipal.setGroupId(user.getGroup().getId());
		ThreadLocalUser.set(userPrincipal);  // so correct userId saved for lastUpdatedUserId
		if (null == gbpmCategory) {
			gbpmCategory = 0;
		}

		AssetUse assetUse = null;
		if (auId != 0) {
			assetUse = getAssetUseRepository().loadAssetUseById(auId);
			// throws PersistenceException, NoResultException
		} else {
			assetUse = new AssetUse();
			Asset asset = new Asset();
			asset.setModelRelease(ModelRelease.NOT_NEEDED); // default
			asset.setCopyrightType(CopyrightType.NOT_WILEY_OWNED);
			assetUse.setAsset(asset);
			CommonWork cw = getCommonWorkRepository().loadCWById(cwId);
			assetUse.setCommonWork(cw);
		}

		assetUse.setCustom(false);
		assetUse.getAsset().setDescription(description);
		assetUse.setSortOrder(sortOrder);  // will convert blank to null
		//Added to make Photographer column editable
		assetUse.getAsset().setArtist(artist);


		if (gbpmCategory != 0) {
			assetUse.setGbpmCategory(gbpmCategory);
		} else {
			assetUse.setGbpmCategory(null);
		}

		if (StringUtils.isNotBlank(position)) {
			assetUse.setPosition(position);
		}
		if (StringUtils.isBlank(position)) {
			assetUse.setPosition(position);
		}
		if (StringUtils.isNotBlank(manuscriptPage)) {
			assetUse.setManuscriptPage(manuscriptPage);
		}
		if (StringUtils.isBlank(manuscriptPage)) {
			assetUse.setManuscriptPage(manuscriptPage);
		}
		if (StringUtils.isNotBlank(finalPage)) {
			assetUse.setFinalPage(finalPage);
		}
		if (StringUtils.isBlank(finalPage)) {
			assetUse.setFinalPage(finalPage);
		}

		if (StringUtils.isNotBlank(sourceRefNumber)) {
			assetUse.getAsset().setVendorId(sourceRefNumber);
		}
		if (StringUtils.isBlank(sourceRefNumber)) {
			assetUse.getAsset().setVendorId(sourceRefNumber);
		}
		if (StringUtils.isNotBlank(creditLine)) {
			assetUse.getAsset().setCreditLine(creditLine);
		}
		if (StringUtils.isBlank(creditLine)) {
			assetUse.getAsset().setCreditLine(creditLine);
		}
		if (StringUtils.isNotBlank(permissionComment)) {
			assetUse.setPermissionComment(permissionComment);
		}
		if (StringUtils.isBlank(permissionComment)) {
			assetUse.setPermissionComment(permissionComment);
		}
		// fix bug https://www.pivotaltracker.com/story/show/52499591
		if (null != pickup) {
			assetUse.setPickup((pickup == 1));
		}
		assetUse.setCameraCopyToCome((cameraCopyToCome != null && cameraCopyToCome == 1) ? true : false);
		assetUse.setSentToProduction((sentToProduction != null && sentToProduction == 1) ? true : false);
		/* Royalty free is coming as null always when asset is edited from product landing page. So, commented
		 * below line. If it is required for any flow, then please take care of all flows before uncomment it.
		 * Slightly updated code is written below. Before modify it please make sure it works for the above case also.
		 */
		//assetUse.getAsset().setRoyaltyFree((royaltyFree != null && royaltyFree == 1) ? true : false);
		if(royaltyFree != null) {
			if(royaltyFree == 1)
				assetUse.getAsset().setRoyaltyFree(true);
			else
				assetUse.getAsset().setRoyaltyFree(false);
		}
		assetUse.setMediaManager((mediaManager != null && mediaManager == 1) ? true : false); //SR_301213

		if (StringUtils.isNotBlank(usageCode)) {
			assetUse.setUsage((Usage) getCachedData().getObjectByType(Usage.class, usageCode));
		} else {
			assetUse.setUsage(Usage.FIGURE);
		}

		if (StringUtils.isNotBlank(mediaTypeCode)) {
			assetUse.getAsset().setMediaType((MediaType) getCachedData().getObjectByType(MediaType.class, mediaTypeCode));
		} else {
			assetUse.getAsset().setMediaType(MediaType.PHOTO);
		}

		if (componentId != 0) {
			Component comp = getAssetUseRepository().find(Component.class, componentId);
			assetUse.setComponent(comp);
		}

		UserDefaults ud = userRepository.loadUserDefaults(userId);
		if (ud != null) {  // should always be non-null
			assetUse.setSize(ud.getSize());
		}

		//User user = userRepository.lazyLoad(User.class, userId, new String[] { "group" });
		//assetUse.setUserGroup(user.getGroup());

		try {
			assetUse = getAssetUseService().saveAssetUseRow(assetUse);
			if (null != assetUse) {
				auId = assetUse.getId();
			}
		}
		catch (Exception e) {
			log.debug("saveAssetUseForm(): failed to save assetUse: ", e);
		}

		// we use assetUse.getId() because it might be a new one
		AssetUseTableRowView assetUseRow = assetUseService.loadAssetUseTableRow(auId, cwId);
		ThreadLocalUser.cleanup();  // cleanup id set above since this thread could be reused for another user
		return ObjectToJson.doTransform(assetUseRow, callback);
	}

	// creates a duplicate asset use and returns the new asset use row;
	@GET
	@Produces("application/json")
	@Path("/duplicate/{cwId}")
	public String duplicateAssetUse (
			@PathParam("cwId") int cwId,
			@QueryParam("userId") int userId,
			@QueryParam("ids") String ids,
			@QueryParam("callback") String callback)
			throws Exception
	{
		log.debug("duplicateAssetUse(): called with cwId = " + cwId + ", ids = " + ids);

		UserPrincipal userPrincipal = new UserPrincipal();
		userPrincipal.setId(userId);
		User user = userRepository.lazyLoad(User.class, userId, new String[] { "group" });
		userPrincipal.setGroupId(user.getGroup().getId());
		ThreadLocalUser.set(userPrincipal);  // so correct userId saved for lastUpdatedUserId
		int userGroupId = userPrincipal.getGroupId();
		String[] aIds = StringUtils.split(ids, ",");

		List <Object> newAus = new ArrayList<Object>();
		for (String id : aIds) {
			AssetUse newAu = assetUseService.copyAsset (cwId, cwId, new Integer (id), false, true, userGroupId, false);
			AssetUseTableRowView assetUseRow = assetUseService.loadAssetUseTableRow(newAu.getId(), cwId);
			newAus.add(assetUseRow);
		}

		return ObjectToJson.doTransform(newAus, callback);
	}


	// checks to see if there is already an asset with same vendorId
	@GET
	@Produces("application/json")
	@Path("/validateSourceRefId/{sourceRefId}/{sourceId}/{assetId}")
	public String validateSourceRefId (
			@PathParam("sourceRefId") String sourceRefId,
			@PathParam("sourceId") int sourceId,
			@PathParam("assetId") int assetId,
			@QueryParam("callback") String callback)
			throws Exception
	{
		log.debug("validateSourceRefId(): called with sourceRefId = " + sourceRefId + ", sourceId = " + sourceId);
		Asset asset = assetRepository.findDuplicateBySourceAndVendorId(sourceId, sourceRefId, assetId);
		if (null != asset){
			// lazy load because transform is trying to do that and we have no transaction
			asset = assetRepository.lazyLoad(Asset.class, asset.getId(), new String[] {"sources", "files"});
		}
		return ObjectToJson.doTransform(asset, callback);
	}

	// cancels/deletes an asset use
	// if the asset use is already canceled, will uncancel it
	@GET
	@Produces("application/json")
	@Path("/uncancel/{cwId}")
	public String uncancelAssetUse (@PathParam("cwId") int cwId,
			@QueryParam("ids") String ids,
			@QueryParam("callback") String callback)
			throws Exception
	{
		log.debug("uncancelAssetUse(): cwId = " + cwId + ", ids = " + ids);

		List <Map<String, Object>> statuses = new ArrayList<Map<String, Object>> ();
		if (StringUtils.isBlank(ids)) {
			log.debug("cancelAssetUse(): no ids received");
			return ObjectToJson.doTransform(statuses, callback);
		}

		String[] aIds = StringUtils.split(ids, ",");
		for (String id : aIds) {
			Map<String, Object> status = new HashMap <String, Object> ();

			int auId = new Integer (id.trim());
			try {
				getAssetUseService().unCancelAssetUse(auId);
				status.put("auId", auId);
				status.put("deleted", false);
				status.put("aData", assetUseService.loadAssetUseTableRow(auId, cwId));
			} catch (NoResultException e) {
				// if not found something might be wrong but we remove it from the table
				status.put("auId", id);
				status.put("deleted", true);
			}
			statuses.add(status);
		}
		return ObjectToJson.doTransform(statuses, callback);
	}

	@GET
	@Path("/recalculate/{type}/{value}")
	public String recalculateStatus (@PathParam("type") String objectType, @PathParam("value") String value,
			@QueryParam("callback") String callback)
			throws Exception
	{
		log.debug("recalculateStatus(): type = " + objectType + ", value = " + value);

		if (objectType.equalsIgnoreCase("cw")) {
			if (value.equals("0")) {
				commonWorkService.updateAllStatuses();
			} else {
				String [] array = value.split(",");
				for (String cwId : array) {
					log.debug("recalculateStatus(): calling update for cwId: " + cwId);
					assetUseService.updateStatusByExpression("cw_id = " + cwId);
				}
			}
		} else if (objectType.equalsIgnoreCase("au")) {
			if (value.equals("0")) {
				commonWorkService.updateAllStatuses();
			} else {
				String [] array = value.split(",");
				for (String auId : array) {
					log.debug("recalculateStatus(): calling update for auId: " + auId);
					assetUseService.updateStatusByExpression("id = " + auId);
				}
			}
		} else if (objectType.equalsIgnoreCase("status")) {
			if (value.equalsIgnoreCase("null")) {
				assetUseService.updateStatusByExpression("permission_status is null");
			} else {
				assetUseService.updateStatusByExpression("permission_status = '" + value + "'");
			}
		}
		else if (objectType.equalsIgnoreCase("date")) {
			// value should be of the format '2014-01-01 00:00:00' (time can be omitted)
			assetUseService.updateStatusByExpression("last_updated_status < '" + value + "'");
		}
		else {
			log.error("recalculateStatus(): objectType not valid [" + objectType + "] - not doing anything.");
		}

		return StringUtils.EMPTY;
	}

	public AssetUseIndexService getAssetUseIndexService() {
		return assetUseIndexService;
	}

	public void setAssetUseIndexService(AssetUseIndexService assetUseIndexService) {
		this.assetUseIndexService = assetUseIndexService;
	}

	public AssetUseService getAssetUseService() {
		return assetUseService;
	}

	public void setAssetUseService(AssetUseService assetUseService) {
		this.assetUseService = assetUseService;
	}

	public CommonWorkService getCommonWorkService() {
		return commonWorkService;
	}

	public void setCommonWorkService(CommonWorkService commonWorkService) {
		this.commonWorkService = commonWorkService;
	}


	public AssetUseRepository getAssetUseRepository() {
		return assetUseRepository;
	}

	public void setAssetUseRepository(AssetUseRepository assetUseRepository) {
		this.assetUseRepository = assetUseRepository;
	}

	public ReferenceDataCache getCachedData() {
		return cachedData;
	}

	public void setCachedData(ReferenceDataCache cachedData) {
		this.cachedData = cachedData;
	}

	public CommonWorkRepository getCommonWorkRepository() {
		return commonWorkRepository;
	}

	public void setCommonWorkRepository(CommonWorkRepository commonWorkRepository) {
		this.commonWorkRepository = commonWorkRepository;
	}

	public UserRepository getUserRepository() {
		return userRepository;
	}

	public void setUserRepository(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public AssetRepository getAssetRepository() {
		return assetRepository;
	}

	public void setAssetRepository(AssetRepository assetRepository) {
		this.assetRepository = assetRepository;
	}
}
