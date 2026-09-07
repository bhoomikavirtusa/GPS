package com.wiley.permissions.services;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryparser.classic.ParseException;
import org.mule.api.MuleException;

import com.wiley.permissions.common.bean.BeanMergeException;
import com.wiley.permissions.common.dispatcher.DispatcherException;
import com.wiley.permissions.domain.message.MessageException;
import com.wiley.permissions.domain.message.RequiresDelayedProcessingException;
import com.wiley.permissions.domain.persistence.permissions.Asset;
import com.wiley.permissions.domain.persistence.permissions.AssetUse;
import com.wiley.permissions.domain.persistence.permissions.AuSourcePermStatus;
import com.wiley.permissions.domain.persistence.permissions.Reference;
import com.wiley.permissions.domain.persistence.permissions.Source;
import com.wiley.permissions.persistence.PersistenceException;
import com.wiley.permissions.security.web.PermissionsSecurityException;
import com.wiley.permissions.services.util.ServiceException;
import com.wiley.permissions.services.view.AssetUseTableRowView;
import com.wiley.permissions.services.view.SourceTableRowView;
import com.wiley.sf.common.monitor.PerfStatSnapshot;

public interface AssetUseService {

	public void handleGetAssetUseMessage(List<Reference> refs) throws PersistenceException, MessageException;

	/**
	 * Called by SourceUpdateThread.
	 */
	public void updateIndexForSource(String sourceExtId) throws Exception;

	public void addAssetUseByIdToIndex(int auId, IndexWriter writer)
			throws PersistenceException, IOException;

	/**
	 * Attaches another AssetUse from another CommonWork to current CW.
	 *
	 * @param cw
	 * @param oldCw
	 * @param auId
	 * @param edition
	 * @throws Exception
	 */
	public AssetUse copyAsset(int cwId, int origCwId,
			Integer auId, boolean previousEdition, boolean includeUsage, int userGroupId, boolean copyFlag) throws Exception;

	public void handleUpdateAssetUseMessage(AssetUse au) throws Exception;

	public void handleUpdateAssetUseMessage(List<AssetUse> list) throws Exception;

	/**
	 * merges the DB version with the incoming version. This need to be done for the imports, messages, etc
	 * Not necessary for UI updates
	 * This method does NOT update the database.
	 *
	 * @param au
	 * @return AssetUse
	 * @throws BeanMergeException
	 * @throws Exception
	 */
	public AssetUse mergeAssetUseProperties(AssetUse au) throws PersistenceException, BeanMergeException;

	/**
	 * @param int auId
	 * @param sendMessage
	 *
	 * @return The number of statuses that changed (0 or 1 in this case)
	 */
	public int updateStatusForAssetUse(Integer auId, boolean sendMessage) throws Exception;

	/**
	 * @param au Must be non-null
	 *
	 * @return The number of statuses that changed (0 or 1 in this case)
	 */
	public int updateStatusForAssetUse(AssetUse au, boolean sendMessage) throws Exception;

	/**
	 * @param asset  Must be non-null
	 *
	 * @return The number of statuses that changed
	 */
	public int updateStatusForAsset(Asset asset) throws Exception;

	/**
	 * @param asset  Must be non-null
	 * @param sendMessage  True means sent CMS an update message if the status changes for an AssetUse
	 *
	 * @return The number of statuses that changed
	 */
	public int updateStatusForAsset(Asset asset, boolean sendMessage) throws Exception;

	/**
	 * Recalculates the status for a list of assets just for just the usages
	 * for the given common work.
	 *
	 * @param cwId
	 * @param assetList
	 * @throws Exception
	 */
	public void updateStatusForAssetCollection(int cwId, List<Asset> assetList) throws Exception;

	/**
	 * @param collection  Must be non-null but may be empty
	 * @param sendMessage  True means sent CMS an update message if the status changes for an AssetUse
	 *
	 * @return The number of statuses that changed
	 */
	public int updateStatusForAssetCollection(Collection<Asset> collection) throws Exception;

	/**
	 * @param collection  Must be non-null but may be empty
	 * @param sendMessage  True means sent CMS an update message if the status changes for an AssetUse
	 *
	 * @return The number of statuses that changed
	 */
	public int updateStatusForAssetCollection(Collection<Asset> collection, boolean sendMessage) throws Exception;

	/**
	 * @param collection  Must be non-null but may be empty
	 * @param sendMessage  True means sent CMS an update message if the status changes for an AssetUse
	 *
	 * @return The number of statuses that changed
	 */
	public int updateStatusForAssetIdCollection(Collection<Integer> collection, boolean sendMessage) throws Exception;

	/**
	 * @param collection  Must be non-null
	 *
	 * @return The number of statuses that changed
	 */
	public int updateStatusForAssetUseCollection(Collection<AssetUse> collection) throws Exception;

	public int updateStatusForAssetUseIdCollection(Collection<Integer> collection) throws Exception;

	/**
	 * This method designed to be called from Tomcat.
	 */
	public List<PerfStatSnapshot> getPerformanceStatsFromMuleProc() throws DispatcherException, MuleException;

	/**
	 * This method receives the call from Tomcat (will be called inside Mule).
	 */
	public List<PerfStatSnapshot> handleGetPerformanceStats(String dummyInput);

	public void handleDeleteAssetUseMessage(List<Reference> refs) throws Exception;

	public int updateStatusByExpression(String expression) throws Exception;

	/**
	 * The AssetUse object may be a brand new object or one that is being updated.
	 * It might come from an import and we need to merge the properties first for a detached object
	 * @param au
	 * @param source
	 * @return
	 * @throws Exception
	 */
	public AssetUse saveDetachedAssetUse(AssetUse au, Source source, boolean calculateStatus) throws Exception;

	/**
	 * The AssetUse object may be a brand new object or one that is being updated.
	 * @param au
	 * @return
	 * @throws Exception
	 */
	public AssetUse saveAssetUse(AssetUse au) throws Exception;

	/**
	 * IMPORTANT: This method should only be called when AssetUse.Asset is also
	 * being created/updated since this method calls another method
	 * to recalculate the status for all AssetUses of the AssetUse.Asset.
	 *
	 * The AssetUse object may be a brand new object or one that is being
	 * updated. The method is called with sendMessage = true with the exception
	 * of ImportAssets when we do not want to send the messages
	 * @param au
	 * @param boolean sendMessage
	 * @return
	 * @throws Exception
	 */
	public AssetUse saveAssetUse(AssetUse au, boolean calculateStatus, boolean sendMessage) throws Exception;

	/**
	 * loads the asset, product from the AssetUse when only the external IDs are specified
	 * (usually this is the case from a CMS message)
	 * @param au
	 * @return AssetUse
	 * @throws PersistenceException
	 * @throws ServiceException
	 * @throws RequiresDelayedProcessingException
	 * @throws PermissionsSecurityException
	 */
	public AssetUse loadAssetUseFromMessage(AssetUse au) throws Exception;

	/**
	 * Returns the new value of canceled.
	 */
	public boolean cancelOrDeleteAssetUse(int auId, int userId, String comment, Integer replacementId, boolean bDeleteIfEmpty) throws Exception;

	public void unCancelAssetUse(Integer auId) throws Exception;

	public AssetUse saveAssetUseRow(AssetUse au) throws Exception;

	public AuSourcePermStatus mergeAssetUsePermStatus(AuSourcePermStatus au) throws PersistenceException;

	/**
	 * deletes all asset associations to a product, only if there are no POs or Contracts for that asset
	 * also, if that is the only asset to product association, will delete the asset too
	 * @param productId
	 * @throws Exception
	 */
	public void deleteAssetsForCommonWork(Integer cwId)
			throws Exception;

	/**
	 * Called by PermissionStatusValidationServiceImpl (needs to be in a separate class
	 * so can say REQUIRES_NEW.
	 *
	 * @param au
	 * @param sources
	 * @throws PersistenceException
	 */
	public void deleteNonCurrentSourceStatuses(AssetUse au, List<Source> sources) throws PersistenceException;

	public void sendUpdateAssetUseMessages(List<AssetUse> auList, String object) throws MessageException;

	public AssetUse loadAssetView(Integer assetUseId) throws Exception;

	/****************************************************
	 * VIEWS LOAD METHODS							    *
	 * those methods that we want exposed thru a REST 	*
	 * service have to be defined in Service class 		*
	 ****************************************************/
	public List<SourceTableRowView> loadSourceListView(Integer cwId, boolean includeCovers)
		throws ParseException, IOException, PersistenceException;

	/**
	 * @param cwId  Should be valid
	 * @param form  May be null
	 */
	public List<AssetUseTableRowView> loadAssetListTableFromIndex (int cwId, LandingFilterForm form, boolean includeCovers)
		throws PersistenceException, ParseException, IOException;

	public AssetUseTableRowView loadAssetUseForEdit (int auId, int cwId) throws Exception;

	public AssetUseTableRowView loadAssetUseTableRow (int auId, int cwId) throws PersistenceException;

	/**
	 * Deletes the assetUse for the given id and any associated files.
	 * Also deletes from the assetUse index.
	 * Does not delete anything else (asset, contract, etc).
	 * Returns a list of other AssetUse ids whose cancel_replacement_id was nulled out (these require status recalc).
	 */
	public List<Integer> clearAssetUseById(int auId) throws Exception;

	public String deleteAllAssetsForCommonWork(Integer cw_id) throws Exception;

}
