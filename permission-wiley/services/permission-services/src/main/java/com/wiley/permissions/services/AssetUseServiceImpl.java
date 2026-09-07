package com.wiley.permissions.services;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryparser.classic.ParseException;
import org.mule.api.MuleException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.wiley.permissions.common.bean.BeanMergeException;
import com.wiley.permissions.common.bean.BeanUtility;
import com.wiley.permissions.common.dispatcher.DispatcherException;
import com.wiley.permissions.common.dispatcher.OperationType;
import com.wiley.permissions.domain.message.MessageException;
import com.wiley.permissions.domain.message.RequiresDelayedProcessingException;
import com.wiley.permissions.domain.persistence.permissions.Asset;
import com.wiley.permissions.domain.persistence.permissions.AssetUse;
import com.wiley.permissions.domain.persistence.permissions.AuSourcePermStatus;
import com.wiley.permissions.domain.persistence.permissions.CommonWork;
import com.wiley.permissions.domain.persistence.permissions.CommonWorkStatus;
import com.wiley.permissions.domain.persistence.permissions.Component;
import com.wiley.permissions.domain.persistence.permissions.Condition;
import com.wiley.permissions.domain.persistence.permissions.Contact;
import com.wiley.permissions.domain.persistence.permissions.DataSource;
import com.wiley.permissions.domain.persistence.permissions.ImportSource;
import com.wiley.permissions.domain.persistence.permissions.MessageErrorOp;
import com.wiley.permissions.domain.persistence.permissions.PagePosition;
import com.wiley.permissions.domain.persistence.permissions.PermissionStatus;
import com.wiley.permissions.domain.persistence.permissions.Product;
import com.wiley.permissions.domain.persistence.permissions.PublicationStatus;
import com.wiley.permissions.domain.persistence.permissions.Reference;
import com.wiley.permissions.domain.persistence.permissions.RenditionType;
import com.wiley.permissions.domain.persistence.permissions.Source;
import com.wiley.permissions.domain.persistence.permissions.User;
import com.wiley.permissions.persistence.PersistenceException;
import com.wiley.permissions.repositories.AssetRepository;
import com.wiley.permissions.repositories.AssetUseRepository;
import com.wiley.permissions.repositories.CommonWorkRepository;
import com.wiley.permissions.repositories.ProductRepository;
import com.wiley.permissions.repositories.ReferenceDataCache;
import com.wiley.permissions.repositories.SourceRepository;
import com.wiley.permissions.services.message.CMSMessageService;
import com.wiley.permissions.services.message.UpdatePermissionStatusInput;
import com.wiley.permissions.services.util.ServiceException;
import com.wiley.permissions.services.view.AssetUseTableRowView;
import com.wiley.permissions.services.view.SourceTableRowView;
import com.wiley.sf.common.monitor.PerfStatSnapshot;
import com.wiley.sf.common.monitor.PerformanceMonitor.PerfTimer;

@Transactional(propagation = Propagation.SUPPORTS)
public class AssetUseServiceImpl extends BaseService implements AssetUseService {
	private static final Log log = LogFactory.getLog(AssetUseService.class);

	private AssetUseRepository assetUseRepository;
	private AssetRepository assetRepository;
	private CommonWorkRepository commonWorkRepository;
	private ProductRepository productRepository;
	private SourceRepository sourceRepository;

	private ReferenceDataCache cachedData;

	private ProductService productService;
	private CMSMessageService outgoingMessageService;
	private AssetUseIndexService assetUseIndexService;
	private AssetService assetService;

	@Override
	public void handleGetAssetUseMessage(List<Reference> refs) throws PersistenceException, MessageException
	{
		PerfTimer timer = getMonitor().startTimer("AssetUseService::handleGetAssetUseMessage");
		log.info("handleGetAssetUseMessage() called");

		// TODO: need to pass original message ID to this method somehow so can
		// use for replyId
		String replyId = null;

		for (Reference ref : refs) {
			AssetUse au = assetUseRepository.loadAssetUseByExternalId(ref.getExternalId());
			// throws PersistenceException
			if (au == null) {
				getOutgoingMessageService().sendErrorMessageLogException(
						replyId,
						MessageErrorOp.NOT_FOUND,
						"Could not find the AssetUse with the given wid",
						ref.getExternalId());
			}
			else {
				getOutgoingMessageService().sendUpdateAssetUseMessage(au, replyId);
				// throws MessageException
			}
		}
		timer.stopTimer();
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
	public void updateIndexForSource(String sourceExtId) throws Exception {
		List<Integer> auIdList = assetUseRepository.loadAssetUseIdsForSourceExtId(sourceExtId);
		log.debug("updateIndexForSource(): " + auIdList.size()
			+ " assetUses for Source extId " + sourceExtId + " to update...");

	    for (Integer auId: auIdList) {
	    	assetUseIndexService.updateIndex(auId);  // REQUIRES_NEW
	    }
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
	public void addAssetUseByIdToIndex(int auId, IndexWriter writer)
	throws PersistenceException, IOException
	{
		AssetUse au = assetUseRepository.loadAssetUseById(auId);  // throws PersistenceException
		assetUseIndexService.addToIndex(au, writer);  // throws IOException
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public AssetUse copyAsset(int cwId, int origCwId,
			Integer auId, boolean previousEdition, boolean includeUsage, int userGroupId, boolean copyFlag) throws Exception
	{
		log.debug("copyAsset(): cwId = " + cwId + ", origCwId = " + origCwId
			+ ", auId = " + auId + ", previousEdition = " + previousEdition + ", includeUsage = " + includeUsage);

		log.debug("in AssetServiceImpl...userGroupId "+userGroupId);
		// copyAsset is REQUIRES_NEW
		AssetUse au = assetUseRepository.copyAsset(cwId, origCwId, auId, previousEdition, includeUsage, userGroupId);

		log.debug("IN AssetUseRepositoryImpl -->"+au.getStatus());
		log.debug("IN AssetUseRepositoryImpl -->"+au.getStatusExplanation());

		//Added for paperwork task Granted Starts
		List<Condition> valueList;
		//log.debug("valuelist1 --> "+valueList.size());
				if(copyFlag){
					log.debug("here you go "+copyFlag+"CWID "+origCwId);
					valueList = assetUseRepository.loadEditionConditionValueForAssetId(au.getAsset().getId(), origCwId);
					//log.debug("LIST ---> "+valueList.toString());
					//log.debug(!au.getStatus().equals(PermissionStatus.MIGRATED_FROM_FILEMAKER));
					boolean conditionFlag = false;

                    au.setImportSource(ImportSource.COPY_FROM_PREVIOUS_EDITION);


					if(valueList.size() > 0 && (valueList.get(0).getRollupValue().equals("Granted for this edition only") || valueList.get(0).getRollupValue().equals("this edition")) ){
                      log.debug("valueList.get(0).getRollupValue() "+valueList.get(0).getRollupValue());
                      conditionFlag = true;
                      au.setImportSource(null);
                      log.debug("Condition Flag VALUELIST "+conditionFlag);
					}
					log.debug("valuelist1 --> "+valueList.size());
					log.debug("Import Source 1--> "+au.getImportSource());

					if(au.getStatus().equals(PermissionStatus.MIGRATED_FROM_FILEMAKER) ){
						au.setImportSource(null);
					}
					log.debug("Import Source 2--> "+au.getImportSource());
					if(au.getStatus().getCode().contains("grantedRF")){
						log.debug("IAM IN grantedRF");
						au.setImportSource(ImportSource.COPY_FROM_PREVIOUS_EDITION);
					}
					log.debug("Import Source 3--> "+au.getImportSource());
					if(au.getStatus().getCode().contains("grantedRFlimitedprint") || au.getStatus().getCode().contains("grantedRFlimitedSeats")){
						log.debug("IAM IN grantedRFlimitedprint");
					    au.setStatusExplanation(null);
					    au.setStatus(null);
					    au.setImportSource(null);
					    au.getAsset().setRoyaltyFree(false);
					    au.getAsset().setManaged(true);
					    au.getAsset().setWillBeRoyaltyFree(false);
					}
					log.debug("Import Source 4--> "+au.getImportSource());
					log.debug("VALUELIST "+au.getImportSource());
				}

				log.debug("StatusExplanation --> "+au.getStatusExplanation());
				log.debug("Status --> "+au.getStatus());
				log.debug("Import Source 5--> "+au.getImportSource());
		//Paperwork task Granted ends

		// call the save asset use because does much more
		au = saveAssetUse(au, true, true);


		log.debug("copyAsset(): new auId = " + au.getId());
        //log.debug("test for Copy FLag is true or not --> "+copyFlag +"au.getImportSource() --> "+au.getImportSource().getDescription());
		return au;
	}

	@Override
	public void handleUpdateAssetUseMessage(AssetUse au) throws Exception
	{
		PerfTimer timer = getMonitor().startTimer("AssetUseService::handleUpdateAssetUseMessage");
		au = loadAssetUseFromMessage(au);

		// merge the incoming object with the DB
		au = mergeAssetUseProperties(au);
		au = saveAssetUse(au);

		timer.stopTimer();
	}

	@Override
	public void handleUpdateAssetUseMessage(List<AssetUse> list) throws Exception
	{
		for (AssetUse au : list) {
			handleUpdateAssetUseMessage(au);
		}
	}

	// @Transactional(propagation = Propagation.REQUIRES_NEW)
	@Override
	public AssetUse mergeAssetUseProperties(AssetUse au) throws PersistenceException, BeanMergeException
	{
		PerfTimer timer = getMonitor().startTimer("AssetUseService::mergeAssetUseProperties");
		// TODO load by externalAssetId, externalProductId and extra params ...
		AssetUse old = assetUseRepository.loadAssetUseByExternalId(au.getExternalId()); // throws PersistenceException

		if (old == null) {
			old = new AssetUse();
		}

		// if we have one bean in the db, we copy new data
		BeanUtility.merge(au, old);

		timer.stopTimer();
		return old;
	}

	/**
	 * Makes a synchronous Mule call for the status update. However the sendMessage part is async.
	 */
	@Override
	public int updateStatusForAssetUse(Integer auId, boolean sendMessage) throws Exception
	{
		PerfTimer timer = getMonitor().startTimer("AssetUseService::updateStatusForAssetUse by ID");
		try {
			AssetUse au = assetUseRepository.loadAssetUseById(auId);
			if (au == null) return 0;
			else return updateStatusForAssetUse(au, sendMessage);
		} finally {
			timer.stopTimer();
		}
	}

	/**
	 * Makes a synchronous Mule call for the status update. However sendMessage is async.
	 */
	@Override
	public int updateStatusForAssetUse(AssetUse au, boolean sendMessage) throws Exception
	{
		PerfTimer timer = getMonitor().startTimer("AssetUseService::updateStatusForAssetUse");
		try {
			log.info("updateStatusForAssetUse(AssetUse, sendMessage) called");

			ArrayList<AssetUse> list = new ArrayList<AssetUse>(1);
			list.add(au);
			//log.debug("TEST A "+list.toString());
			log.debug("TEST B "+list.size());

			return updateStatusForAssetUseCollection(list, sendMessage);

		} finally {
			timer.stopTimer();
		}
	}

	/**
	 * Makes a synchronous Mule call for the status update.
	 */
	@Override
	public int updateStatusForAsset(Asset asset) throws Exception
	{
		return updateStatusForAsset(asset, true);
	}

	/**
	 * Makes a synchronous Mule call for the status update. However the sendMessage is async.
	 */
	@Override
	public int updateStatusForAsset(Asset asset, boolean sendMessage) throws Exception
	{
		ArrayList<Asset> list = new ArrayList<Asset>(1);
		list.add(asset);
		return updateStatusForAssetCollection(list, sendMessage);
	}

	/**
	 * Recalculates the status for a list of assets just for just the usages
	 * for the given common work.
	 */
	@Override
	public void updateStatusForAssetCollection(int cwId, List<Asset> assetList) throws Exception
	{
		List<AssetUse> auList = assetUseRepository.loadAssetUseListByCWIdAssetList(cwId, assetList); // throws PersistenceException
		updateStatusForAssetUseCollection(auList);  // throws Exception
	}

	@Override
	public int updateStatusForAssetCollection(Collection<Asset> collection) throws Exception
	{
		return updateStatusForAssetCollection(collection, true);
	}

	/**
	 * Makes a synchronous Mule call for the status update. However the sendMessage part is asynchronous.
	 *
	 * @param collection  Must be non-null but may be empty
	 * @param sendMessage  True means sent CMS an update message if the status changes for an AssetUse
	 */
	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public int updateStatusForAssetIdCollection(Collection<Integer> collection, boolean sendMessage) throws Exception
	{
		PerfTimer timer = getMonitor().startTimer("AssetUseService::updateStatusForAssetIdCollection");

		try {
			log.info("updateStatusForAssetIdCollection(Collection<Integer>) called");

			Collection<AssetUse> auCollection  = new HashSet<AssetUse>();

			for (Integer assetId : collection) {
				Asset asset = assetRepository.loadAssetById(assetId);
				auCollection.addAll(asset.getAssetUses());
			}

			return updateStatusForAssetUseCollection (auCollection, sendMessage);
		}
		finally {
			timer.stopTimer();
		}
	}

	/**
	 * Makes a synchronous Mule call for the status update. However the sendMessage part is asynchronous.
	 *
	 * @param collection  Must be non-null but may be empty
	 * @param sendMessage  True means sent CMS an update message if the status changes for an AssetUse
	 */
	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public int updateStatusForAssetCollection(Collection<Asset> collection, boolean sendMessage) throws Exception
	{
		PerfTimer timer = getMonitor().startTimer("AssetUseService::updateStatusForAssetCollection");

		try {
			log.info("updateStatusForAssetCollection(Collection<Asset>) called");

			Collection<AssetUse> auCollection  = new HashSet<AssetUse>();

			for (Asset asset : collection) {
				asset = assetRepository.lazyLoad(Asset.class, asset.getId(), new String [] { "assetUses" });
				auCollection.addAll(asset.getAssetUses());
			}

			return updateStatusForAssetUseCollection (auCollection, sendMessage);
		}
		finally {
			timer.stopTimer();
		}
	}

	/**
	 * Makes a synchronous Mule call for the status update.
	 *
	 * @param whereExpression  Applied to the Asset Use table
	 */
	@Override
	public int updateStatusByExpression(String whereExpression) throws Exception {
		List<Integer> auIds = assetUseRepository.loadAssetUseIds(whereExpression);
		return updateStatusForAssetUseIdCollection(auIds, false);
	}

	/**
	 * Makes a synchronous Mule call for the status update.
	 */
	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public int updateStatusForAssetUseCollection(Collection<AssetUse> collection) throws Exception {
		return updateStatusForAssetUseCollection(collection, true);
	}

	/**
	 * Makes a synchronous Mule call for the status update. However the sendMessage part is asynchronous.
	 */
	private int updateStatusForAssetUseCollection(Collection<AssetUse> collection, boolean sendMessage) throws Exception
	{
		ArrayList<Integer> idList = new ArrayList<Integer>(collection.size());
		ArrayList<Integer> cwIdList = new ArrayList<Integer>(collection.size());
		for (AssetUse au : collection) {
			idList.add(au.getId());
			cwIdList.add(au.getCopyAssetsCwId());
		}
		return updateStatusForAssetUseIdCollection(idList, true);
	}

	/**
	 * Makes a synchronous Mule call for the status update.
	 */
	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public int updateStatusForAssetUseIdCollection(Collection<Integer> collection) throws Exception {
		return updateStatusForAssetUseIdCollection(collection, true);
	}

	/**
	 * Makes a synchronous Mule call for the status update. However the sendMessage part is asynchronous.
	 */
	private int updateStatusForAssetUseIdCollection(Collection<Integer> auIdCollection, boolean sendMessage) throws Exception
	{
		PerfTimer timer = getMonitor().startTimer("AssetUseService::updateStatusForAssetUseCollection");
		NumberFormat intFormat = NumberFormat.getIntegerInstance();
		log.debug("updateStatusForAssetUseCollection(Collection<Integer>) called, collection size = " + intFormat.format(auIdCollection.size()));

		UpdatePermissionStatusInput input = new UpdatePermissionStatusInput(auIdCollection, sendMessage);

		// If we are inside Mule, then use LocalDispatcher so we have no timeout,
		// otherwise use regular dispatcher, which is the remote one.
		// - this logic is now inside getServiceDispatcher().
		Integer statusChangeCount = (Integer) getServiceDispatcher().send(
				OperationType.ASSET_USE_COLLECTION_STATUS_VALIDATION, input, null);

		// update the index regardless of whether the status changed because
		// other fields may have changed
		assetUseIndexService.updateIndexNow(auIdCollection);

		timer.stopTimer();
		return statusChangeCount;
	}

	/**
	 * This method designed to be called from Tomcat.
	 */
	@Override
	public List<PerfStatSnapshot> getPerformanceStatsFromMuleProc() throws DispatcherException, MuleException {
		PerfTimer timer = getMonitor().startTimer("AssetUseService::getPerformanceStatsFromMuleProc");
		String input = "dummyInput";
		@SuppressWarnings("unchecked")
		List<PerfStatSnapshot> result = (List<PerfStatSnapshot>) getServiceDispatcher().send(OperationType.GET_PERFORMANCE_STATS, input, null);
		timer.stopTimer();
		return result;
	}

	/**
	 * This method receives the call from Tomcat (will be called inside Mule).
	 */
	@Override
	public List<PerfStatSnapshot> handleGetPerformanceStats(String dummyInput) {
		log.debug("handleGetPerformanceStats() called");
		List<PerfStatSnapshot> result = getMonitor().getStatSnapshot();
		return result;
	}

	@Override
	public void handleDeleteAssetUseMessage(List<Reference> refs) throws Exception
	{
		log.info("handleDeleteAssetUseMessage() called");

		for (Reference ref : refs) {
			try {
				deleteAssetUseByExternalId(ref.getExternalId(), false); // throws Exception
			}
			catch (IOException e) {
				log.error("Failed to delete", e);
			}
			// throws PersitenceException, MessageException
		}
	}

	/**
	 * this method is used ONLY by AssetResource save method, when we know the fields that are changing
	 * will not affect the status if the status is already != granted or insufficient or amendment needed
	 * https://www.pivotaltracker.com/story/show/14715921
	 * @throws Exception
	 */
	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public AssetUse saveAssetUseRow(AssetUse au) throws Exception {
		// if new asset, status is "No Source"
		if (null == au.getId() || null == au.getStatus()) {
			au.setStatus(PermissionStatus.NO_SOURCE);
		}

		//if status <> granted limited or insufficient or amendment needed then no need to recalculation status after row save
		if (!PermissionStatus.isGranted(au.getStatus()) &&
			!PermissionStatus.isInsufficient (au.getStatus()) &&
			!au.getStatus().equals(PermissionStatus.AMENDMENT_NEEDED)) {
			// call setNeedsConfirmationFlag() before calling mergeAssetUse
			// otherwise lazyLoad instead the method will fail since the AssetUse
			// will be detached
			return saveAssetUse (au, false, true);
		} else {
			return saveAssetUse (au, true, true);
		}
	}

	/**
	 * Persist an assetUse entry from the spreadsheet.
	 *
	 * @param assetUse
	 * @param product
	 * @param user
	 * @param force
	 * @throws Exception
	 */
	// @Transactional(propagation = Propagation.REQUIRES_NEW)
	@Override
	public AssetUse saveDetachedAssetUse(AssetUse au, Source source, boolean calculateStatus) throws Exception
	{
		log.debug("saveDetachedAssetUse(): assetUse = " + au.toString());

		// this method does not update the database
		// - mergeAssetUse() below does
		au = mergeAssetUseProperties(au);
		if (null != source) {
			au.getAsset().getSources().clear();
			au.getAsset().setSource(0, source);
		}

		au = saveAssetUse(au, calculateStatus, false);

		log.debug("saveDetachedAssetUse(): assetUse.externalId: " + au.getExternalId());

		return au;
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public AssetUse saveAssetUse(AssetUse au) throws Exception
	{
		return saveAssetUse (au, true, true);
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public AssetUse saveAssetUse(AssetUse au, boolean calculateStatus, boolean sendMessage) throws Exception
	{
		log.debug("saveAssetUse(): entered..., au.getId() = " + au.getId() + ", calculateStatus = "
				+ calculateStatus + ", sendMessage = " + sendMessage);
		boolean wasNewAu = (au.getId() == null);

		Integer groupId = null;
		int cwId = au.getCommonWork().getId();
		if (null != au.getUserGroup()) {
			groupId = au.getUserGroup().getId();
		}

		if (null != au.getGbpmCategory() && au.getGbpmCategory() == 0) {
			au.setGbpmCategory(null);
		}

		PerfTimer timer = getMonitor().startTimer("AssetUseService::saveAssetUse");


		if (null != au.getPickupISBN() || null != au.getReusedComment()) {
			log.debug("Modifying this block for ImportSource because of the Import Activity");
			if(au.getImportSource() != null){

			}else{
				au.setImportSource(null);
			}
		}
		//SR_301213 starts
				if (au.isMediaManager()) {
					au.setMediaManager(true);
				}
				else
				{
					au.setMediaManager(false);
				}
				//SR_301213 ends
		//log.debug("IN 2 "+au.getStatus());
		//log.debug("IN 2 "+au.getStatusExplanation());

		au = assetUseRepository.saveRequiresNew(au);  // REQUIRES_NEW

		if (null != groupId) {
			assetUseRepository.assignUserGroupToAu(au.getId(), groupId);
		}

		// if the asset has a vendorId (sourceRef#), we check for dups.
		if (StringUtils.isNotBlank(au.getAsset().getVendorId())) {
			// can throw and exception if more than one asset
			Asset asset = dedupAssetBySourceGroupAndVerndorId (au.getAsset(), cwId);
			// the new asset
			if (null != asset) {
				au.setAsset(asset);
			}
		}

		// calculate seats for royaltyFree
		if (wasNewAu && au.getAsset().isRoyaltyFree()) {
			Asset asset = au.getAsset();
			asset.setTotalUsedSeats(assetRepository.calculateTotalUsedSeatsByAssetId(asset.getId()));
			log.debug("saveAssetUse(): totalUsedSeats for asset id " + asset.getId() + " now " + asset.getTotalUsedSeats());
			asset = assetUseRepository.saveRequiresNew(asset);  // REQUIRES_NEW
			// set the new asset in asset use so the status calculation will have the new asset with the increased number of seats used
			au.setAsset(asset);
		}

		// IMPORTANT: clear the entityManager here because otherwise will try to update again when this transaction ends
		assetUseRepository.clear();

		// update the reference table
		assetUseRepository.saveAssetPermissionRef (au.getId()); // REQUIRES_NEW

		int numStatusChange = 0;
		// sometimes we do not need to calculate status, but we still need to update index
		if (calculateStatus) {
			numStatusChange = updateStatusForAssetUse(au, sendMessage);
		} else {
			assetUseIndexService.updateIndexNow(au.getId());
		}

		// reload AssetUse into persistent context because when sending messages
		// lazyLoad is required (get exception otherwise)
		au = assetUseRepository.loadAssetUseById(au.getId());

		if (numStatusChange != 0 && sendMessage) {
			outgoingMessageService.sendUpdateAssetUseMessage(au, null);
		}

		try {
			Asset asset = au.getAsset();
			if (sendMessage) {
				outgoingMessageService.sendUpdateAssetMessage(asset, null);
			}


			// because we already have the status calculated for au, maybe we can
			// improve this and calculate only for the rest of au associated with this asset
			// the message will not be sent twice, but the status will be calculated twice for au
			// lnagy - these can be improved but for now at least a test to see if asset has
			// just one asset use, so we know it is the one we just saved
			if(null != au.getPickupISBN() || null != au.getReusedComment()){

			}else{
				int auCount = ((Number)(assetUseRepository.executeSingleResultNamedQuery("Asset.countAssetUses",
						new Object[] {asset.getId()}))).intValue();
				if (calculateStatus && auCount != 1) {
					updateStatusForAsset(asset, sendMessage);
				}
			}
		}
		catch (Exception e) {
			log.error("Exception caught trying to send UpdateAsset or AssetUse message", e);
		}

		checkCWStatuses(au);

		timer.stopTimer();
		return au;
	}

	/**
	 * checked for duplicates and transfers the assets to the new ones if found
	 * @param asset
	 * @return the asset with replaced with
	 * @throws ServiceException
	 */
	public Asset dedupAssetBySourceGroupAndVerndorId(Asset asset, Integer cwId) throws Exception {
		asset = assetRepository.lazyLoad (Asset.class, asset.getId(), new String[] {"sources"});
		List<Source> sources = asset.getSources();
         log.debug("prior to empty check"+sources.toString());
		 // if no sources or no vendorId, nothing to dedup
		if (CollectionUtils.isEmpty(sources) || StringUtils.isEmpty(asset.getVendorId())) {
			return null;
		}

		Map<Integer, Asset> dups = new HashMap<Integer, Asset> ();
		for (Source source : sources) {
			//Asset dupAsset = assetRepository.findDuplicateBySourceAndVendorId(source.getId(), asset.getVendorId(), asset.getId());

			//Modified Code for Creative Services Spreadsheet Task

			Asset dupAsset=null;
			if(null != asset.getImportSource()){
				log.debug("test for "+asset.getImportSource().toString());
				if(asset.getImportSource().getCode().toString()== "2" ){
					}else{
						dupAsset=assetRepository.findDuplicateBySourceAndVendorId(source.getId(), asset.getVendorId(), asset.getId());
					}
			}else{

				dupAsset=assetRepository.findDuplicateBySourceAndVendorId(source.getId(), asset.getVendorId(), asset.getId());

			}

		/*
			Asset dupAsset=null;
			if(asset.getImportSource().getCode().toString().equals("2")){

			}else
			{
				dupAsset=assetRepository.findDuplicateBySourceAndVendorId(source.getId(), asset.getVendorId(), asset.getId());
			}*/


			/** Changes for CS Spreadsheet ends */
			// if no dup found, try next source
			if (null == dupAsset) {
				continue;
			} else {
				dups.put(source.getId(), dupAsset);
			}
		}

		// if no duplicate found, we return
		if (CollectionUtils.isEmpty(dups.keySet())) {
			return null;
		}

		int poCount = ((Number)(assetUseRepository.executeSingleResultNamedQuery(
				"Asset.countPurchaseOrdersInCW", new Object[] {asset.getId(), cwId}))).intValue();
		int contractCount = ((Number)(assetUseRepository.executeSingleResultNamedQuery(
				"Asset.countContractsInCW", new Object[] {asset.getId(), cwId}))).intValue();

		// if dups found but requests or contracts exists
		if (CollectionUtils.isNotEmpty(dups.keySet()) && (poCount != 0 || contractCount != 0)) {
			throw new ServiceException ("Duplicate detected but asset already has requests or contracts");
		}

		// if more than one
		if (dups.size() > 1) {
			throw new ServiceException ("You cannot have more than one duplicate asset");
		} else if (dups.size() == 1) {
			Set<Integer> sourceIds = dups.keySet();
			for (Integer sourceId : sourceIds) {
				Asset dupAsset = dups.get(sourceId);
				// if the dup asset has more then one source
				if (dupAsset.getSources().size() > 1) {
					throw new ServiceException ("You cannot deduplicate an asset with one that has more than one source");
				}
				assetRepository.transferAsset (asset.getId(), dupAsset.getId());
				return dups.get(sourceId);
			}
		}

		return null;
	}

	private void checkCWStatuses(AssetUse au) {
		CommonWork cw = au.getCommonWork();
		if (CommonWorkStatus.COMPLETE.equals(cw.getCoverCWStatus())
			&& !commonWorkRepository.isCompleteCapable(cw.getId(), false)) {
			log.debug("checkCWStatuses(): setting CoverCWStatus to IN_PROGRESS");
			cw.setCoverCWStatus(CommonWorkStatus.IN_PROGRESS);
		}

		if (CommonWorkStatus.COMPLETE.equals(cw.getInteriorCWStatus())
			&& !commonWorkRepository.isCompleteCapable(cw.getId(), true)) {
			log.debug("checkCWStatuses(): setting InteriorCWStatus to IN_PROGRESS");
			cw.setInteriorCWStatus(CommonWorkStatus.IN_PROGRESS);
		}

		if (CommonWorkStatus.NO_PERMISSIONS_REQUIRED.equals(cw.getCoverCWStatus())
				&& !commonWorkRepository.isNoPermRequiredCapable(cw.getId(), false)) {
				log.debug("checkCWStatuses(): setting CoverCWStatus to IN_PROGRESS");
				cw.setCoverCWStatus(CommonWorkStatus.IN_PROGRESS);
		}

		if (CommonWorkStatus.NO_PERMISSIONS_REQUIRED.equals(cw.getInteriorCWStatus())
			&& !commonWorkRepository.isNoPermRequiredCapable(cw.getId(), true)) {
			log.debug("checkCWStatuses(): setting InteriorCWStatus to IN_PROGRESS");
			cw.setInteriorCWStatus(CommonWorkStatus.IN_PROGRESS);
		}
	}

	@Override
	public AssetUse loadAssetUseFromMessage(AssetUse au) throws Exception
	{
		PerfTimer timer = getMonitor().startTimer("AssetUseService::loadAssetUseFromMessage");
		// this means that only the external ids of the Asset and Product are
		// set so we need to load the full data so when we do a merge below
		// it won't null-out this data
		String assetExtId = au.getAsset().getExternalId();
		Asset asset = assetRepository.loadAssetByExternalId(assetExtId);
		// throws ServiceException
		if (asset == null) {
			// This case normally should not happen
			log.warn("loadAssetUseFromMessage(): We don't have the Asset referenced in the UpdateAssetUse message (wid = "
							+ assetExtId + ")");
			try {
				getOutgoingMessageService().sendGetAssetMessage(assetExtId);
				// We'd prefer to just requeue the UpdateAssetUse msg we've
				// got, but can't do this with
				// the current code structure (no handle to msg_cache) so
				// just request it again.
				// getOutgoingMessageService().sendGetAssetUseMessage(au.getExternalId());
			}
			catch (MessageException ex) {
				log.error("loadAssetUseFromMessage(): Caught exception trying to deal "
						+ "with the situation just mentioned by sending out two Get messages.", ex);
			}

			throw new RequiresDelayedProcessingException("Requeue message for " + au.getExternalId());
			// return null;
		}
		else {
			au.setAsset(asset);
		}

		Component component = au.getComponent();
		// lnagy - TODO what to do if component is null ? For now just test
		// it so it does not fail
		if (null != component) {
			String compExtId = component.getExternalId();
			component = commonWorkRepository.loadComponentByExternalId(compExtId);
			// throws PersistenceException

			if (component == null) {
				// This case normally should not happen
				log.warn("loadAssetUseFromMessage(): We don't have the Component referenced in the UpdateAssetUse message (wid = "
						+ compExtId + ")");
				try {
					getOutgoingMessageService().sendGetComponentMessage(compExtId);
					// We'd prefer to just requeue the UpdateAssetUse msg
					// we've got, but can't do this with
					// the current code structure (no handle to msg_cache)
					// so just request it again.
					// getOutgoingMessageService().sendGetAssetUseMessage(au.getExternalId());
				}
				catch (MessageException ex) {
					log.error("loadAssetUseFromMessage(): Caught exception trying to deal "
							+ "with the situation just mentioned by sending out two Get messages.", ex);
				}

				throw new RequiresDelayedProcessingException("Requeue message for " + au.getExternalId());
				// return null;
			}
			else {
				au.setComponent(component);
			}
		}


		String productExternalId = au.getCommonWork().getPrimaryProduct().getExternalId();
		Product product = productRepository.loadByExternalId(productExternalId);
		if (product == null) {
			// get product from PE
			// TODO: don't hard code US DataSource
			product = getProductService()
					.refreshProduct(productExternalId, DataSource.US.getCode(), true);
			// throws Exception
		}
		au.setCommonWork(product.getCommonWork());
		// throws PersistenceException
		timer.stopTimer();
		return au;
	}

	private void deleteAssetUseByExternalId(String externalId, boolean sendMessage)
			throws Exception
	{
		AssetUse au = assetUseRepository.loadAssetUseByExternalId(externalId); // throws PersistenceException
		deleteAssetUseById(au.getId());  // throws Exception

		if (au != null) {
			if (sendMessage) {
				getOutgoingMessageService().sendDeleteAssetUseMessage(externalId);
				// throws MessageException
			}
		}
	}

	private void deleteAssetUseById(int auId)
		throws Exception
	{
		List<Integer> replacedIds = assetUseRepository.deleteAssetUseById(auId);  // throws PersistenceException
		updateStatusForAssetUseIdCollection(replacedIds);  // throws Exception
		assetUseIndexService.deleteFromIndexNow(auId);
	}

	@Override
	public void unCancelAssetUse(Integer auId) throws Exception
	{
		AssetUse au = assetUseRepository.loadAssetUseById(auId);

		au.setCanceled(false);
		au.setRemoved(false);
		au.setCancelComment(null);
		au.setCancelReplacement(null);
		au.setCancelTimestamp(null);
		au.setCancelUser(null);

		au = assetUseRepository.saveRequiresNew(au);
		updateStatusForAssetUse(auId, true);
	}

	/**
	 * marks an usage as being canceled (and optionally) replaced
	 * lnagy - do not worry about seat allocation. James said the logic will be to complicated
	 * @param auId
	 * @param userId
	 * @param comment
	 * @param replacementId can be null or 0
	 * @throws Exception
	 */
	private void cancelAssetUse(int auId, int userId, String comment, Integer replacementId) throws Exception
	{
		//AssetUse au = assetUseRepository.loadAssetUseById(auId);
		AssetUse au = assetUseRepository.lazyLoad(AssetUse.class, auId, new String[] {"commonWork.primaryProduct.publicationStatus"});

		au.setCanceled(true);
		PublicationStatus ps = au.getCommonWork().getPrimaryProduct().getPublicationStatus();
		// pub_status is a non-nullable column in the product table
		au.setRemoved(ps.equals(PublicationStatus.PUBLISHED) || ps.equals(PublicationStatus.MAY_OR_MAY_NOT_REPRINT)
				|| ps.equals(PublicationStatus.NEW_EDITION_PENDING) || ps.equals(PublicationStatus.NO_REPRINT));
		au.setCancelComment(comment);

		// assuming that replacementId is always a valid id here
		if (null != replacementId && replacementId != 0) {
			AssetUse replacement = new AssetUse();
			replacement.setId(replacementId);
			au.setCancelReplacement(replacement);
		}

		au.setCancelTimestamp(new Date());

		// assuming that userId is always a valid id here
		User cancelUser = new User();
		cancelUser.setId(userId);
		au.setCancelUser(cancelUser);

		au = assetUseRepository.saveRequiresNew(au);
		// update status
		updateStatusForAssetUse(auId, true);
	}

	/**
	 * Deletes the assetUse for the given id and any associated files.
	 * Also deletes from the assetUse index.
	 * Does not delete anything else (asset, contract, etc).
	 * Returns a list of other AssetUse ids whose cancel_replacement_id was nulled out (these require status recalc).
	 */
	@Override
	public List<Integer> clearAssetUseById(int auId) throws Exception
	{
		assetUseRepository.deleteAssetUseFiles(auId);
		List<Integer> replacedIds = assetUseRepository.deleteAssetUseById(auId);  // throws PersistenceException
		assetUseIndexService.deleteFromIndexNow(auId);
		return replacedIds;
	}

	/**
	 * tries to cancel an asset use. If the asset is not used, will delete it
	 * @return boolean true if deleted
	 */
	@Override
	public boolean cancelOrDeleteAssetUse(int auId, int userId, String comment, Integer replacementId, boolean bDeleteIfEmpty) throws Exception
	{
		log.debug("cancelOrDeleteAssetUse(): auId = " + auId + ", userId = " + userId
				+ ", replacmentId = " + replacementId + ", bDeleteIfEmpty = " + bDeleteIfEmpty);
		boolean deleted = false;
		if (bDeleteIfEmpty) {
			AssetUse dau = assetUseRepository.find(AssetUse.class, auId);
			// we need to figure out if the asset-use needs to be canceled, or deleted.
			// we also need to know if the associated asset needs to also be deleted.

			Asset dasset = assetUseRepository.find(Asset.class, dau.getAsset().getId());

			int poCount = ((Number)(assetUseRepository.executeSingleResultNamedQuery(
					"Asset.countPurchaseOrdersInCW", new Object[] {dasset.getId(), dau.getCommonWork().getId()}))).intValue();
			int contractCount = ((Number)(assetUseRepository.executeSingleResultNamedQuery(
					"Asset.countContractsInCW", new Object[] {dasset.getId(), dau.getCommonWork().getId()}))).intValue();
			int assetUseFileCount = ((Number)(assetUseRepository.executeSingleResultNamedQuery(
					"AssetUse.countFiles", new Object [] { auId }))).intValue();

			if (poCount == 0 && contractCount == 0 && assetUseFileCount == 0 && !PermissionStatus.isLegacy(dau.getStatus())
				&& null == dau.getImportSource()) {
				// delete the usage
				deleteAssetUseById(auId); // ends up calling REQUIRES_NEW method so the delete is done right away
				deleted = true;

				// auForAssetCount will be 0 is there was only one (delete transaction already completed)
				int auForAssetCount = ((Number)(assetUseRepository.executeSingleResultNamedQuery("Asset.countAssetUses",
						new Object[] {dasset.getId()}))).intValue();
				log.debug("cancelOrDeleteAssetUse(): auForAssetCount = " + auForAssetCount);

				// we may also delete the asset if
				// 1) The asset was only referenced by this one AssetUse (which now is already deleted - see above)
				// and 2) it was created by permissions UI and not CMS
				// and 3) it has no files - if it has files, just orphan
				int assetFileCount = ((Number)(assetRepository.executeSingleResultNamedQuery(
						"Asset.countFiles", new Object [] { dasset.getId() }))).intValue();
				if (auForAssetCount == 0 && dau.getExternalId().contains("perm.asset_use.")) {
					if (assetFileCount == 0) {
						getAssetService().deleteAssetByExternalId(dasset.getExternalId());
					} else {
						log.debug ("cancelOrDeleteAssetUse(): asset id " + dasset.getId() + " HAS BEEN ORPHANED because if has files");
					}
				}
			// if there is paper work, we just cancel
			} else {
				bDeleteIfEmpty = false;
			}
		}

		// if just cancel the use
		if (!bDeleteIfEmpty) {
			cancelAssetUse (auId, userId, comment, replacementId);
		}

		return deleted;
	}

	@Override
	public AuSourcePermStatus mergeAssetUsePermStatus(AuSourcePermStatus au) throws PersistenceException
	{
		return assetUseRepository.saveRequiresNew(au);
	}

	/**
	 * deletes all asset associations to a product, only if there are no POs or Contracts for that asset
	 * also, if that is the only asset to product association, will delete the asset too
	 * @param assetExternalIds
	 * @param externalIds
	 * @param productId
	 * @throws Exception
	 */
	@Override
	public void deleteAssetsForCommonWork(Integer cwId)
	throws Exception
	{
		// get a list of externalIds so we can send all the messages at once
		List<Reference> externalIds = new ArrayList<Reference> ();
		List<Reference> assetExternalIds = new ArrayList<Reference> ();

		try {
			CommonWork cw = assetUseRepository.find (CommonWork.class, cwId);
			if (null == cw) {
				throw new PersistenceException ("No CommonWork found for ID [" + cwId + "]", true);
			}
			List<Integer> auIds = assetUseRepository.loadAssetUseIds("cw_id = " + cwId);
			StringBuilder message = new StringBuilder();

			for (Integer auId : auIds) {
				AssetUse au = assetUseRepository.loadAssetUseById(auId);
				Asset asset = assetUseRepository.lazyLoad(Asset.class, au.getAsset().getId(), new String[] {"purchaseOrders", "contracts", "assetUses"});

				if (asset.getAllPurchaseOrdersCount() > 0 || asset.getAllContractsCount() > 0) {
					message.append("[" + StringEscapeUtils.escapeHtml4(asset.getDescription())
							+ "] has PO or Contracts<br/>");
					log.debug("AssetUse [" + au.getId() + "] description ["
							+ asset.getDescription() + "] could not be deleted because it has PO or Contracts");
					continue;
				}

				try {
					// deletes the association - true will send the message
					deleteAssetUseById(au.getId());
				} catch (Exception e) {
					log.debug ("Failed to delete the assetUsage [" + au.getExternalId() + "]", e);
					continue;
				}

				externalIds.add(new Reference(au.getExternalId()));

				// checks to see if we can delete the asset too
				Collection<AssetUse> allAssetUses = asset.getAssetUses();
				boolean onlyOneUsage = (allAssetUses.size() == 1);
				log.debug("Usages [" + allAssetUses.size() + "] for " + asset.getDescription());

				for (Iterator<AssetUse> iter2 = allAssetUses.iterator(); iter2.hasNext();) {
					AssetUse au2 = iter2.next();
					if (au2.getId().intValue() == au.getId().intValue()) {
						// remove from the asset collection
						iter2.remove();
					}
				}

				// if is only one usage (the selected one)
				if (onlyOneUsage) {
					try {
						log.debug("Delete asset [" + asset.getDescription() + "]");
						assetExternalIds.add(new Reference(asset.getExternalId()));
						// delete the asset and send message
						getAssetRepository().deleteAssetByExternalId(asset.getExternalId());
					} catch (Exception e) {
						log.debug("Failed to delete the asset [" + asset.getDescription() + "]", e);
					}
				}
			}
			message.append("Deleted " + externalIds.size() + " usage(s) and " + assetExternalIds.size() + " asset(s) <br/>");
			String msg = message.toString();
			if (StringUtils.isNotEmpty(msg)) {
				throw new PersistenceException(msg, true);
			}
		} finally {
			// now send the messages - first the AssetUse then Asset
			getOutgoingMessageService().sendDeleteAssetUseMessages(externalIds);

			getOutgoingMessageService().sendDeleteAssetMessages(assetExternalIds);
		}
	}

	@Override
	public String deleteAllAssetsForCommonWork(Integer cwId) throws Exception
	{
		CommonWork cw = assetUseRepository.find (CommonWork.class, cwId);
		List<Integer> auIds = assetUseRepository.loadAssetUseIds("cw_id = " + cwId);


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

		// not required int cwId = assetUseRepository.loadCwIdForAssetUseId(auIds[0]);

		for (int auId : auIds) {
			Integer assetId = assetUseRepository.loadAssetIdForAssetUseId(auId);
			List<Integer> auIdsForAssetAndCW = assetUseRepository.loadAssetUseIdsForAssetIdAndCwId(assetId, cwId);
			if (auIdsToBeClearedSet.containsAll(auIdsForAssetAndCW)) {
				assetIdDeleteList.add(assetId);
			}
			else {
				// ignoring List<Integer> replacedIds returned from clearAssetUseById()
				// - the UI just reloads the whole table so don't need to send this info to UI
				clearAssetUseById(auId);
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
		String output = userMsg.toString();
		log.debug("processClearAssetForm(): output: " + output);
		return output;

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
			clearAssetUseById(auId);
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

	@Override
	public void deleteNonCurrentSourceStatuses(AssetUse au, List<Source> sources) throws PersistenceException
	{
		assetUseRepository.deleteNonCurrentSourceStatuses(au, sources);
	}

	@Override
	public void sendUpdateAssetUseMessages(List<AssetUse> auList, String object) throws MessageException
	{
		getOutgoingMessageService().sendUpdateAssetUseMessages(auList, object);
	}


	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public AssetUse loadAssetView(Integer assetUseId) throws Exception
	{
		AssetUse assetUse = getAssetUseRepository().loadAssetUseById(assetUseId);
		assetUse.getCommonWork().getProducts();
		assetUse.getCommonWork().getPrimaryProduct();
		assetUse.getAsset().getFiles();
		assetUse.getCreditLine();
		PagePosition pagePosition = assetUse.getPagePosition();
		if (pagePosition != null) pagePosition.getDescription();
		assetUse.getAsset().getFileOfRenditionType(RenditionType.SMALL_THUMBNAIL);
		assetUse.getAsset().getFileOfRenditionType(RenditionType.LARGE_THUMBNAIL);
		assetUse.getAsset().getAssetUses();

		return assetUse;
	}

	/****************************************************
	 * VIEWS LOAD METHODS							    *
	 * those methods that we want exposed thru a REST 	*
	 * service have to be defined in Service class 		*
	 * @throws PersistenceException 					*
	 ****************************************************/
	/**
	 *
	 * @param cwId  Should be valid
	 * @param form  May be null
	 */
	@Override
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<AssetUseTableRowView> loadAssetListTableFromIndex(int cwId, LandingFilterForm form,
			boolean includeCovers) throws PersistenceException, ParseException, IOException
	{
		PerfTimer timer = getMonitor().startTimer("AssetUseService::loadAssetListTableFromIndex()");

		List<Component> components = getCommonWorkRepository().loadComponentList(cwId, includeCovers);
		//Added for Build RN Ticket 151007-001006 --- Start
		List<Source> disabledSources = sourceRepository.loadDisabledSources();
		String artistName = null;
		String noFlyPhotographerLastName = null;
		String noFlySourceName = null;
		//Added for Build RN Ticket 151007-001006 --- End
		AssetUseSearchResults assetUseList = assetUseIndexService.searchIndex(cwId, form, includeCovers, true);
		List<AssetUseSearchResult> resultList = assetUseList.getDocuments();
		List<AssetUseTableRowView> results = new ArrayList<AssetUseTableRowView>(resultList.size());
		for (AssetUseSearchResult au : resultList) {
			AssetUseTableRowView auView = new AssetUseTableRowView (au);
			//Added for Build RN Ticket 151007-001006 --- Start
			artistName = auView.getArtist();
			if(null != artistName) {
				artistName = artistName.trim();
	    		if(!StringUtils.isEmpty(artistName)) {artistName= artistName.replaceAll("[^\\x20-\\x7E]", ""); // to ignore copy right symbol and other special characters
				artistName = artistName.trim(); // to ignore trailing spaces
    			//strike through the photographer name if a matching no-fly source (or) last name of no-fly source found
    			for (Source source: disabledSources) {
    				noFlyPhotographerLastName = source.getNoFlyPhotographerLastName();
    				if(!StringUtils.isEmpty(noFlyPhotographerLastName)) {
    				noFlyPhotographerLastName=noFlyPhotographerLastName.replaceAll("[^\\x20-\\x7E]", "");
    				noFlyPhotographerLastName=noFlyPhotographerLastName.trim();
    				}
    						
    				noFlySourceName = source.getExternalName();
    				if(auView.isPickup() || auView.isReusedFromPrevEdtn()){
    					auView.setNoFlyMatchApproved(false);
    				}
    				if(!(auView.isNoFlyMatchApproved())) {
        				if((null != noFlySourceName && !StringUtils.isEmpty(noFlySourceName.trim()) && StringUtils.equalsIgnoreCase(noFlySourceName, artistName))
        						|| (null != noFlyPhotographerLastName && !StringUtils.isEmpty(noFlyPhotographerLastName.trim())
        						&& StringUtils.equalsIgnoreCase(artistName, noFlyPhotographerLastName))) {
        			
        						auView.setArtist("<del>"+artistName+"</del>");
	        					auView.setDescription("<del>"+auView.getDescription()+"</del>");
	        					if((auView.getManuscriptPage()!=null) || !StringUtils.isEmpty(auView.getManuscriptPage())){
	        						auView.setManuscriptPage("<del>"+auView.getManuscriptPage()+"</del>");
	        					}
	        					if((auView.getPosition()!=null) || !StringUtils.isEmpty(auView.getPosition())){
	        					auView.setPosition("<del>"+auView.getPosition()+"</del>");
	        					}
	        					if((auView.getMediaType()!=null) || !StringUtils.isEmpty(auView.getMediaType())){
	        					auView.setMediaType("<del>"+auView.getMediaType()+"</del>");
	        					}
	        					if((auView.getComponentName()!=null) || !StringUtils.isEmpty(auView.getComponentName())){
	        					auView.setComponentName("<del>"+auView.getComponentName()+"</del>");
	        					}
	        					if((auView.getUsage()!=null) || !StringUtils.isEmpty(auView.getUsage())){
	        					auView.setUsage("<del>"+auView.getUsage()+"</del>");
	        					}
	        					if((auView.getPermissionStatus()!=null) || !StringUtils.isEmpty(auView.getPermissionStatus())){
	        					auView.setPermissionStatus("<del>"+auView.getPermissionStatus()+"</del>");
	        					}

	        					//auView.setSources(auView.getSources());

	        					if((auView.getFinalPage()!=null) || !StringUtils.isEmpty(auView.getFinalPage())){
	        					auView.setFinalPage("<del>"+auView.getFinalPage()+"</del>");
	        					}
	        					if((auView.getCreditLine()!=null) || !StringUtils.isEmpty(auView.getCreditLine())){
	        					auView.setCreditLine("<del>"+auView.getCreditLine()+"</del>");
	        					}
	        					if((auView.getSortOrder()!=null) || !StringUtils.isEmpty(auView.getSortOrder())){
	        					auView.setSortOrder("<del>"+auView.getSortOrder()+"</del>");
	        					}
	        					if((auView.getSourceRefNumber()!=null) || !StringUtils.isEmpty(auView.getSourceRefNumber())){
	        					auView.setSourceRefNumber("<del>"+auView.getSourceRefNumber()+"</del>");
	        					}
	        					if((auView.getPermissionComment()!=null) || !StringUtils.isEmpty(auView.getPermissionComment())){
	            					auView.setPermissionComment("<del>"+auView.getPermissionComment()+"</del>");
	            				}
	        					break;
	        				}
        				}
        			}
	    		}
			}
			//Added for Build RN Ticket 151007-001006 --- End
			// set the component sort order (we need it for sorting by component)
			if (0 != auView.getComponentId()) {
				for (Component component : components) {
					if (component.getId() == auView.getComponentId()) {
						auView.setComponentSortOrder(component.getSortOrder());
						break;
					}
				}
			}

			auView.setHasThumbnail(au.isHasAssetFiles());

			// Start : Added for DM-1606
			String permStatusDescription = auView.getPermissionStatus();
			if(permStatusDescription.equalsIgnoreCase(PermissionStatus.FORM_SENT.getDescription()))
			{
				boolean createdPO = assetRepository.checkForPoCreation(au.getAssetId());
				log.debug("AssetUseTableRowView() PO created "+createdPO+" for asset ID "+au.getAssetId());
				if(createdPO)
					permStatusDescription = PermissionStatus.FORM_WAITING_ON_INVOICE.getDescription();
			}
			auView.setPermissionStatus(permStatusDescription);
			// End : Added for DM-1606

			results.add(auView);
		}

		timer.stopTimer();
		return results;
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<SourceTableRowView> loadSourceListView(Integer cwId, boolean includeCovers) throws ParseException, IOException, PersistenceException
	{
		Map<Integer, SourceTableRowView> rowMap = new HashMap<Integer, SourceTableRowView>();
		// read data from index and compute the summary - false param means do NOT include canceled asset uses
		AssetUseSearchResults results = assetUseIndexService.getCWAssetUsesFromIndex(cwId, includeCovers, false);
		List<AssetUseSearchResult> resultList = results.getDocuments();
		for (AssetUseSearchResult auResult : resultList) {
			List<Source> sources = auResult.getSources();

			for (Source source : sources) {
				SourceTableRowView row = rowMap.get(source.getId());
				if (null == row) {
					row = new SourceTableRowView();
					try {
	                    source = sourceRepository.lazyLoad(Source.class, source.getId(),new String[] {"contacts"});
                    } catch (Exception e) {
                    	log.error("loadSourceListView(): failed to load source", e);
                    	continue;
                    }
					row.setName(source.getName());
					row.setSourceId(source.getId());
					row.setDisabled(source.isNofly());
					List<Contact> contacts = source.getContacts();
					// select the first contact to display
					if (CollectionUtils.isNotEmpty(contacts)) {
						row.setContactName(contacts.get(0).getName());
						row.setContactEmail(contacts.get(0).getEmail());
					}

					rowMap.put(source.getId(), row);
				}
				row.addAssetUse(auResult);
			}
		}

		// calculate the totals after we enter all data
		Set<Integer> sourceIds = rowMap.keySet();
		for (Integer sourceId : sourceIds) {
			SourceTableRowView row = rowMap.get(sourceId);
			row.calculateTotals();
		}
		// will be sorted in the client
		return new ArrayList<SourceTableRowView>(rowMap.values());
	}

	/**
	 * load the asset inline edit row
	 * @param int auId
	 * @return AssetUseTableRowView
	 * @throws Exception
	 */
	@Override
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public AssetUseTableRowView loadAssetUseForEdit (int auId, int cwId) throws Exception
	{
		AssetUseTableRowView auView = loadAssetUseTableRow (auId, cwId);

		// TODO - load all possible values that might need to be displayed
		auView.setMediaTypes(cachedData.get("mediaTypes"));
		auView.setUsages(cachedData.get("usages"));
		auView.setGbpmCategories(cachedData.get("gbpmCategories"));

		return auView;
	}

	/**
	 * load the asset inline view row. If id = 0 it is a new record
	 * @param int auId
	 * @return AssetUseTableRowView
	 * @throws PersistenceException
	 */
	@Override
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public AssetUseTableRowView loadAssetUseTableRow (int auId, int cwId) throws PersistenceException
	{
		// instantiate a new record
		AssetUseTableRowView auView = new AssetUseTableRowView();

		if (auId != 0) {
			try {
				AssetUse au = getAssetUseRepository().loadAssetUseById(auId);

				auView = new AssetUseTableRowView(au);

				if (assetUseRepository.doesAssetUseHaveFiles(auId) || assetUseRepository.doesLatestContractHaveFile(auId)) {
					auView.setHasFiles(true);
				}
				if (null != au.getPermissionComment() || null != au.getProductionComment()) {
					auView.setHasComments(true);
				} else {
					auView.setHasComments(false);
				}
				if (au.getAsset().getFiles().size() > 0) {
					auView.setHasThumbnail(true);
				}
				if (au.getAsset().getAllContracts().size() > 0)
					auView.setHasContracts(true);

				// Start : Added for DM-1606
				String permStatusDescription = auView.getPermissionStatus();
				if(permStatusDescription.equalsIgnoreCase(PermissionStatus.FORM_SENT.getDescription()))
				{
					boolean createdPO = assetRepository.checkForPoCreation(au.getAsset().getId());
					log.debug("AssetUseTableRowView() PO created "+createdPO+" for asset ID "+au.getAsset().getId());
					if(createdPO)
						permStatusDescription = PermissionStatus.FORM_WAITING_ON_INVOICE.getDescription();
				}
				auView.setPermissionStatus(permStatusDescription);
				// End : Added for DM-1606
			}
			catch (Exception e) {
				throw new PersistenceException(e);
			}
		} else {
			getCommonWorkRepository().loadCWById(cwId).getId();
		}

		auView.setCwId(cwId);
		return auView;
	}

	public CMSMessageService getOutgoingMessageService() {
		return outgoingMessageService;
	}

	public void setOutgoingMessageService(CMSMessageService outgoingMessageService) {
		this.outgoingMessageService = outgoingMessageService;
	}

	public void setAssetUseIndexService(AssetUseIndexService assetUseIndexService) {
		this.assetUseIndexService = assetUseIndexService;
	}

	public AssetUseIndexService getAssetUseIndexService() {
		return assetUseIndexService;
	}

	public ProductService getProductService() {
		return productService;
	}

	public void setProductService(ProductService productService) {
		this.productService = productService;
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

	public CommonWorkRepository getCommonWorkRepository() {
		return commonWorkRepository;
	}

	public void setCommonWorkRepository(CommonWorkRepository commonWorkRepository) {
		this.commonWorkRepository = commonWorkRepository;
	}

	public ProductRepository getProductRepository() {
		return productRepository;
	}

	public void setProductRepository(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}

	public AssetService getAssetService() {
		return assetService;
	}

	public void setAssetService(AssetService assetService) {
		this.assetService = assetService;
	}

	public ReferenceDataCache getCachedData() {
		return cachedData;
	}

	public void setCachedData(ReferenceDataCache cachedData) {
		this.cachedData = cachedData;
	}

	public SourceRepository getSourceRepository() {
    	return sourceRepository;
    }

	public void setSourceRepository(SourceRepository sourceRepository) {
    	this.sourceRepository = sourceRepository;
    }

}
