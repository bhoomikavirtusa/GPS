package com.wiley.permissions.repositories;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.wiley.permissions.common.bean.BeanUtility;
import com.wiley.permissions.domain.message.MessageException;
import com.wiley.permissions.domain.persistence.permissions.Asset;
import com.wiley.permissions.domain.persistence.permissions.AssetFile;
import com.wiley.permissions.domain.persistence.permissions.AssetSourcePK;
import com.wiley.permissions.domain.persistence.permissions.AssetToSource;
import com.wiley.permissions.domain.persistence.permissions.ContractAsset;
import com.wiley.permissions.domain.persistence.permissions.ImportSource;
import com.wiley.permissions.domain.persistence.permissions.RoyaltyFreeDeal;
import com.wiley.permissions.domain.persistence.permissions.Source;
import com.wiley.permissions.domain.persistence.permissions.SourceGroup;
import com.wiley.permissions.persistence.PersistenceException;
import com.wiley.permissions.persistence.repository.JPARepository;
import com.wiley.permissions.services.util.ServiceException;
import com.wiley.sf.common.lang.ArgUtil;
import com.wiley.sf.common.monitor.PerformanceMonitor.PerfTimer;

public class AssetRepository extends JPARepository
{
	private static final Log log = LogFactory.getLog(AssetRepository.class);

	@PersistenceContext(unitName = "permissions")
	protected EntityManager entityManager;

	@Override
	public EntityManager getEntityManager() {
		return entityManager;
	}

	private SourceRepository sourceRepository;


	@Transactional(propagation = Propagation.REQUIRED)
	public Asset loadAssetById(int assetId) throws PersistenceException {
		return find(Asset.class, assetId);
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public Asset loadAssetByExternalId(String externalId)
	throws PersistenceException
	{
		try {
			TypedQuery<Asset> query = entityManager.createQuery(
			    "from Asset a where a.externalId = ?1", Asset.class);
		    query.setParameter(1, externalId);
		    return query.getSingleResult();
		}
		catch (NoResultException e) {
			log.debug("loadAssetByExternalId(): failed to load by externalId [" + externalId + "]");
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	@Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
	public Asset loadAssetByTemplate(int cwId, Asset asset) throws PersistenceException
	{
		String sql = "select asset.*, asset_base.* from asset join asset_use on asset.id = asset_use.asset_id "
			+ "join asset_base on asset.id = asset_base.id "
			+ "where asset_use.cw_id = ? ";
		if (null != asset.getId())
			sql += " and asset.id = ?";
		else if (StringUtils.isNotBlank(asset.getDescription()))
			sql += " and asset.description = ?";
		else if (StringUtils.isNotBlank(asset.getVendorId()))
			sql += " and asset.vendor_id = ?";
		log.debug (sql);
		Query query = createNativeQuery(sql, Asset.class);
		query.setParameter(1, cwId);
		if (null != asset.getId())
			query.setParameter(2, asset.getId());
		else if (StringUtils.isNotBlank(asset.getDescription()))
			query.setParameter(2, asset.getDescription());
		else if (StringUtils.isNotBlank(asset.getVendorId()))
			query.setParameter(2, asset.getVendorId());

		List<Asset> assets = query.getResultList();
		// we return the first one, because it is possible to have the
		// same asset name multiple times in the same product
		if (CollectionUtils.isNotEmpty(assets)) {
			return assets.get(0);
		}
		else return null;
	}

	/**
	 * will return true or false if the asset id is in the import_ignore table. Example of ignored assets
	 * are those that have contracts or po, or assets that were reused in another product
	 * @param assetId
	 * @return boolean
	 * @throws ServiceException
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
	public boolean isIgnoredAsset(int assetId) throws ServiceException
	{
		try {
			Query query = createNativeQuery("select count(asset_id) as count from import_ignore where asset_id=?", "scalarCount");
			query.setParameter(1, assetId);
			Number result = (Number) query.getSingleResult();
			log.debug("isIgnoredAsset(): found " + (result.intValue() > 0));
			return (result.intValue() > 0);
		} catch (Exception e) {
			throw new ServiceException("Failed to search for ignored assets", e);
		}
	}

	/**
	 * Important: This method is only designed to be called
	 * from handling an updateAsset message - could be new or existing asset.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public Asset saveAssetFromMessage(Asset asset) throws Exception
	{
		PerfTimer timer = monitor.startTimer("AssetRepository::saveAssetFromMessage");

		log.debug("saveAssetFromMessage(): asset: " + asset.toString());

		Asset old = loadAssetByExternalId(asset.getExternalId());
			// throws ServiceException

		if (old == null) {
			old = new Asset();
		}

		if (asset.getSources() != null) {
			// load each Source so that the id is filled in for each one
			// create temporary second set rather than removing/adding anything
			// during the loop to avoid java.util.ConcurrentModificationException
			Set<Source> newSourceSet = new HashSet<Source>();
			List<Source> sources = asset.getSources();

			for (Source s: sources) {
				Source s2 = sourceRepository.loadSourceByExternalId(s.getExternalId());
				newSourceSet.add(s2);
			}

			sources.clear();
			sources.addAll(newSourceSet);
			// this will update through asset to sources
			// asset.setSources(sources);
		}

		// If there are files to save, get rid of them for the first merge
		// and then add it afterwards (since Asset does not cascade to files).
		List<AssetFile> fileList = asset.getFiles();
		asset.setFiles(null);

		// Important: Asset does NOT have Replace on bean merge for files, so whatever
		// files exist in old will NOT be wiped out (asset has none due to above code)
		BeanUtility.merge(asset, old);  // throws BeanMergeException

		// For some reason the bean merge doesn't properly handle the case
		// where asset.description is non-null and old.description is non-null.
		// - Should overwrite the old value but does not.
		// Perhaps related to the fact that Asset has a base class with a
		// getDescription() method?
		// In any case, fix manually.
		if (StringUtils.isNotBlank(asset.getDescription())) {
			old.setDescription(asset.getDescription());
		}

	    // CMS has description as nullable but it's non-nullable
		// for us, so check for null.
		// - really only need to do this when Asset is new b/c
		// otherwise the Bean merge above won't override non-null with null
	    if (old.getDescription() == null) {
    		old.setDescription("(blank)");
	   	}

		// persist the bean whether it is old or new
		old = entityManager.merge(old); // throws PersistenceException
		//log.debug("updateAsset(): files size = " + old.getFiles().size());

		timer.stopTimer();
		return old;
	}

	/**
	 * Replaces any existing AssetFile of the same RenditionType as the given
	 * AssetFile with the AssetFile given. If there is no existing AssetFile
	 * with the same RenditionType, then adds the given AssetFile to the
	 * Asset's collection.
	 *
	 * @param asset  Must be non-null and be an existing asset
	 * @param file   May be null - if so this method does nothing
	 * @throws PersistenceException
	 * @throws IOException
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void saveAssetFile(AssetFile file) throws PersistenceException {
		PerfTimer timer = monitor.startTimer("AssetRepository::saveAssetFile");

		if (file == null)  return;

		Asset asset = file.getAsset();
		asset = getEntityManager().getReference(Asset.class, asset.getId());

        ArgUtil.notNull(asset, "asset");
        ArgUtil.notNull(asset.getId(), "asset.getId()");

		file.setAsset(asset);
		AssetFile oldFile = asset.getFileOfRenditionType(file.getRenditionType());
		if (oldFile == null) {
			if (asset.getFiles() == null)  asset.setFiles(new ArrayList<AssetFile>());
			asset.getFiles().add(file);
			entityManager.persist(file);
		}
		else {
			oldFile.setAsset(asset);
			oldFile.setObjectName(file.getObjectName());
			oldFile.setFileFormat(file.getFileFormat());
			oldFile.setData(file.getData());
		    entityManager.merge(oldFile);
		}

		timer.stopTimer();
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void addSourceToAsset(Integer assetId, Integer sourceId)
	throws PersistenceException
	{
		Source source = find(Source.class, sourceId);
        	// throws PeristenceException
		Asset asset = find(Asset.class, assetId);
			// throws PeristenceException

		// Not expected but check
		if (source == null || asset == null) {
			throw new PersistenceException("Source or Asset not found");
		}

		asset.getSources().add(source);
		// asset.addSource(source);

		// With the line below and the other one below commented out, this method
		// works - otherwise get a unique constraint violation when the transaction
           // tries to commit (which is when updatePermissionStatus() is called below).

		//source.getAssets().add(asset);

		asset = entityManager.merge(asset);  // throws PersistenceException
	}

	/**
	 * Delete the specified asset along with any files it may have.
	 * (Not contract or asset use files but only asset files.)
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public void deleteAssetAndFiles(int assetId) {
		// There are two ways to handle this
		// 1) Do JPA find and remove - and add cascade on remove to asset.files - chosen NOT to do this for now
		// 2) Use native queries as below
		List<Object> params = new ArrayList<Object>(1);
		params.add(assetId);
		executeNativeQuery("delete from asset_file where asset_id = ?", params);

		executeNativeQuery("delete from asset_2_source where asset_id = ?", params);
		executeNativeQuery("delete from asset where id = ?", params);
		executeNativeQuery("delete from asset_base where id = ?", params);
	}

	/**
	 * TODO: This should probably also delete any files associated to the asset.
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public void deleteAssetByExternalId(String externalId)
	    throws PersistenceException, MessageException
	{
		Asset asset = loadAssetByExternalId(externalId);

		if (null != asset)
			entityManager.remove(asset);
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void deleteAssetToSource(AssetToSource a2s) throws Exception
	{
		a2s = find(AssetToSource.class, new AssetSourcePK(a2s.getSourceId(),a2s.getAssetId()));

		if (a2s == null) { // Not expected but check
			log.warn("deleteAssetToSource(): a2s not found");
			return;
		}

		getEntityManager().remove(a2s);
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<AssetToSource> loadAssetToSourcesByAssetId(Integer assetId) throws PersistenceException
	{
		if (null == assetId) return new ArrayList<AssetToSource>();

		TypedQuery<AssetToSource> query = entityManager.createNamedQuery("AssetToSource.AssetToSource.loadByAssetId", AssetToSource.class);
		query.setParameter("assetId", assetId);

		List<AssetToSource> list = query.getResultList();

		return list;
	}

	/**
	 * loads an asset by source name or source external id and vendorId.
	 * This method is used by the asset imports to dedup assets
	 * @return Asset
	 */
	public Asset loadAssetBySourceVendorId(String source, String vendorId) {
		ArgUtil.notNull(source, "source");
		ArgUtil.notNull(vendorId, "vendorId");
		List<Source> sources = executeMultiResultNamedQuery("Source.findSourceByNameOrExternalId", new Object[] {source, source});
		// if no source found by name or externalId
		if (CollectionUtils.isEmpty(sources)) {
			return null;
		}
		List<Integer> ids = new ArrayList<Integer> ();
		for (Source s : sources) {
			ids.add(s.getId());
		}
		List<Asset> assets = loadAssetBySourceVendorId (ids, vendorId);
		// we return the first one, but it should return only one
		if (CollectionUtils.isNotEmpty(assets)) {
			return assets.get(0);
		}
		else return null;
	}

	public Asset findDuplicateBySourceAndVendorId(int sourceId, String vendorId, int assetId) {
		ArgUtil.notNull(vendorId, "vendorId");
		List<Source> sources = executeMultiResultNamedQuery("Source.findSourcesInSameSourceGroup", new Object[] {sourceId, sourceId});
		// if no source found
		if (CollectionUtils.isEmpty(sources)) {
			return null;
		}
		List<Integer> ids = new ArrayList<Integer> ();
		for (Source s : sources) {
			ids.add(s.getId());
		}
		List<Asset> assets = loadAssetBySourceVendorId (ids, vendorId);
		for (Asset asset : assets) {
			if (asset.getId() == assetId) {
				continue;
			} else {
				return asset;
			}
		}
		return null;
	}

	public List<Asset> loadAssetBySourceVendorId(List<Integer> sourceIds, String vendorId) {

		String sql = "select asset.*, asset_base.* from asset join asset_base on asset.id = asset_base.id " +
				"join asset_2_source a2s on a2s.asset_id = asset.id join source on source.id = a2s.source_id " +
				" where source.id in (" + StringUtils.join(sourceIds,  ",") + ") and asset.vendor_id=?";
		log.debug (sql);
		Query query = createNativeQuery(sql, Asset.class);
		query.setParameter(1, vendorId);
		@SuppressWarnings("unchecked")
		List<Asset> assets = query.getResultList();
		return assets;
    }

	/**
	 *
	 * @param assetId  Must be non-null
	 * @param sourceId  Must be non-null
	 * @param notes  May be null
	 * @return true if a row was updated
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public boolean updateAssetToSourceNotes(Integer assetId, Integer sourceId, String notes) throws PersistenceException
	{
		ArgUtil.notNull(assetId, "assetId");
		ArgUtil.notNull(sourceId, "sourceId");

		Query q = entityManager.createNativeQuery(
			"update asset_2_source set notes = ? where asset_id = ? and source_id = ?",
			AssetToSource.class);
		q.setParameter(1, notes);
		q.setParameter(2, assetId);
		q.setParameter(3, sourceId);
		int numRows = q.executeUpdate();
		return numRows > 0;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public Asset loadAssetByIdWithFiles(int assetId) throws PersistenceException
	{
		Asset asset = loadAssetById(assetId);

		for (AssetFile file : asset.getFilesNotNull()) {
			file.getObjectName();
		}

		return asset;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void updateTotalSeatsAndPrintRunByAssetId(int assetId) throws Exception {
		log.debug("updateTotalSeatsAndPrintRunByAssetId(): assetId: " + assetId);
		Asset asset = find(Asset.class, assetId);
		if (asset == null) {
			throw new RuntimeException("Asset with id " + assetId + " not found in db.");
		}
		// note: James says don't worry about including RFDeals for canceled contracts (i.e. include them)
		// and also don't worry if user has created two contracts for same asset where the newer contract
		// is essentially a revision of the old contract -- solution will be to train users not to do
		// this (modify existing contract instead).
		List<Integer> rfDealIds = loadRFDealIdsForAsset(assetId);

		if (rfDealIds.size() > 0) {
			asset.setTotalUsedSeats(calculateTotalUsedSeatsByAssetId(assetId));
		}
		else asset.setTotalUsedSeats(0);

		int totalSeats = 0;
		boolean totalSeatsUnlimited = false;
		int totalPrintRun = 0;
		boolean totalPrintRunUnlimited = false;
		boolean isSublicense = false;

		for (Integer rfDealId : rfDealIds) {
			RoyaltyFreeDeal rfDeal = find(RoyaltyFreeDeal.class, rfDealId);

			if (rfDeal.getSeats() == 0) {
				totalSeats = 0;
				totalSeatsUnlimited = true;
			}
			else if (!totalSeatsUnlimited) {
				totalSeats += rfDeal.getSeats();
			}

			if (rfDeal.getTotalPrintRun() == 0) {
				totalPrintRun = 0;
				totalPrintRunUnlimited = true;
			}
			else if (!totalPrintRunUnlimited) {
				totalPrintRun += rfDeal.getTotalPrintRun();
			}

			if(rfDeal.isLicenseFlag())
			{
				isSublicense = true;
			} else
				isSublicense = false;
		}

		asset.setTotalSeats(totalSeats);
		asset.setTotalPrintRun(totalPrintRun);
		asset.setSublicense(isSublicense);
			//sandhya label change
		// logic for assets from fileMaker which may be royaltyFree but don't have a contract
		if (ImportSource.FROM_FILEMAKER.equals(asset.getImportSource()) && asset.getTotalSeats() == 0 && asset.getTotalUsedSeats() == 0
				&& asset.getAllContractsCount() == 0 && CollectionUtils.isNotEmpty(asset.getSources())) {
			for (Source source : asset.getSources()) {
				SourceGroup group = source.getSourceGroup();
				if (group != null) {
					List<RoyaltyFreeDeal> dealList = group.getRoyaltyFreeDeals();  // orders by primary key (id)
					if (CollectionUtils.isNotEmpty(dealList)) {
						RoyaltyFreeDeal deal = dealList.get(0);
						asset.setTotalSeats(deal.getSeats());
						asset.setTotalPrintRun(deal.getTotalPrintRun());
						asset.setSublicense(deal.isLicenseFlag());
						break;
					}
				}
			}
		}

		log.debug("updateTotalSeatsAndPrintRunByAssetId(): totalSeats: " + asset.getTotalSeats()
				+ ", totalUsedSeats: " + asset.getTotalUsedSeats()
				+ ", totalPrintRun: " + asset.getTotalPrintRun());

		asset.setRoyaltyFree(rfDealIds.size() > 0);
		asset.setManaged(rfDealIds.size() == 0);
		getEntityManager().merge(asset);
	}

	//Start: Added for DM-123
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void updateTotalSeatsAndPrintRunByAssetId(int assetId, ContractAsset ca) throws Exception {
		log.debug("updateTotalSeatsAndPrintRunByAssetId(): assetId: " + assetId);
		Asset asset = find(Asset.class, assetId);
		if (asset == null) {
			throw new RuntimeException("Asset with id " + assetId + " not found in db.");
		}

		asset.setTotalSeats(0);
		asset.setTotalPrintRun(ca.getTotalPrintRun());
		asset.setSublicense(ca.isSublicence());
		asset.setRoyaltyFree(false);
		asset.setManaged(true);
		getEntityManager().merge(asset);
	}
	//End: Added for DM-123

	//Start: Added to implement DM-122
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void updateAssetLevelTotalSeatsAndPrintRunByAssetId(ContractAsset ca) throws Exception {
		int assetId = ca.getAssetBaseId();
		log.debug("updateAssetLevelTotalSeatsAndPrintRunByAssetId(): assetId: " + assetId);
		Asset asset = find(Asset.class, assetId);
		if (asset == null) {
			throw new RuntimeException("Asset with id " + assetId + " not found in db.");
		}

		if (ca.getSeats() >= 0) {
			asset.setTotalUsedSeats(calculateTotalUsedSeatsByAssetId(assetId));
		}
		else asset.setTotalUsedSeats(0);

		int totalSeats = 0;
		boolean totalSeatsUnlimited = false;
		int totalPrintRun = 0;
		boolean totalPrintRunUnlimited = false;
		boolean isSublicense = false;

		if (null != ca.getSeats() && ca.getSeats() == 0) {
			totalSeats = 0;
			totalSeatsUnlimited = true;
		}
		else if (!totalSeatsUnlimited) {
			totalSeats += ca.getSeats();
		}

		if (null != ca.getTotalPrintRun() && ca.getTotalPrintRun() == 0) {
			totalPrintRun = 0;
			totalPrintRunUnlimited = true;
		}
		else if (!totalPrintRunUnlimited) {
			totalPrintRun += ca.getTotalPrintRun();
		}

		if(ca.isSublicence())
		{
			isSublicense = true;
		} else
			isSublicense = false;

		asset.setTotalSeats(totalSeats);
		asset.setTotalPrintRun(totalPrintRun);
		asset.setSublicense(isSublicense);
		log.debug("updateAssetLevelTotalSeatsAndPrintRunByAssetId(): totalSeats: " + asset.getTotalSeats()
				+ ", totalUsedSeats: " + asset.getTotalUsedSeats()
				+ ", totalPrintRun: " + asset.getTotalPrintRun());

		asset.setRoyaltyFree(ca.isAssetLevelRFDeal());
		asset.setManaged(!ca.isAssetLevelRFDeal());
		//Start: Added for DM-123
		if(null != ca.getRForRM() && ca.getRForRM().equals("ARF")) { //When RM has changed to Asset level RF
			asset.setRoyaltyFree(true);
			asset.setManaged(false);
		}
		if(null != ca.getRForRM() && ca.getRForRM().equals("RM")) { //Asset level RF has changed to RM
			asset.setRoyaltyFree(false);
			asset.setManaged(true);
		}
		if(null != ca.getRForRM() && ca.getRForRM().equals("SRF")) { //Asset level RF or RM has changed to source level RF
			RoyaltyFreeDeal deal = sourceRepository.loadRoyaltyFreeDealById(ca.getRoyaltyFreeDealId());
			if(null != deal) {
				asset.setTotalSeats(deal.getSeats());
				asset.setTotalPrintRun(deal.getTotalPrintRun());
				asset.setSublicense(deal.isLicenseFlag());
			}
			asset.setRoyaltyFree(deal != null);
			asset.setManaged(deal == null);
		}
		//End: Added for DM-123
		getEntityManager().merge(asset);
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public int loadAssetLevelRoyaltyFreeDealsInfo(Integer id) {
		final String sql = "select count(*) as count from asset A join asset_2_source A2S on A.id = A2S.asset_id "
			+ " left outer join source S on A2S.source_id = S.id where S.id = ? and A.is_royalty_free = 1";
		Query query = entityManager.createNativeQuery(sql);
		query.setParameter(1, id);
		// For count, DB2 and MS SQL Server return Integer but MySQL returns BigInteger
		// Integer and BigInteger inherit from Number
		Number result = (Number) query.getSingleResult();
		return result.intValue();
	}
	//End: Added to implement DM-122

	private List<Integer> loadRFDealIdsForAsset(int assetId) {
		String sql = "select rfdeal_id as id from contract_2_asset where asset_base_id = ? and rfdeal_id is not null";
		Query query = createNativeQuery(sql, "scalarId");
		query.setParameter(1, assetId);
		@SuppressWarnings("unchecked")
		List<Integer> list = query.getResultList();
		return list;
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<Integer> loadAllAssetIds() {
		Query query = createNativeQuery("select id from asset", "scalarId");
		@SuppressWarnings("unchecked")
		List<Integer> list = query.getResultList();
		return list;
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public int calculateTotalUsedSeatsByAssetId(int assetId) throws PersistenceException {
		Query query = entityManager.createNativeQuery(
			"select count(distinct(created_user_id)) as count from asset_use where asset_id = ?",
			"scalarCount");
		query.setParameter(1, assetId);
		// For count, DB2 and MS SQL Server return Integer but MySQL returns BigInteger
		// Integer and BigInteger inherit from Number
		Number result = (Number) query.getSingleResult();

		return result.intValue();
	}

	// Added for implementing DM-117
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public int getAssetFilesCount(int assetId) throws PersistenceException {
		Query query = entityManager.createNativeQuery(
			"select count(*) as count from asset_file where asset_id = ?",
			"scalarCount");
		query.setParameter(1, assetId);
		// For count, DB2 and MS SQL Server return Integer but MySQL returns BigInteger
		// Integer and BigInteger inherit from Number
		Number result = (Number) query.getSingleResult();

		return result.intValue();
	}

	/**
	 * This method processes all the assets that have duplicates based on source + vendorId.
	 * This could take a long time to run. If you already know the vendorId, use mergeDuplicateAssetsByVendor
	 * which is much faster.
	 */
/*	lnagy - NOT USED ANYMORE
 * @Transactional(propagation = Propagation.REQUIRED)
	public void mergeDuplicateAssets() throws SQLException {
		String sql1 = "select a.*, c.* from asset a, asset_2_source b, asset_base c " +
					  " where c.id = a.id and a.id =  b.asset_id " +
					  " group by a.vendor_id, b.source_id having count(vendor_id) > 1  " +
					  " and a.vendor_id is not null and a.vendor_id <> ''  and a.vendor_id > ? " +
					  " order by count(vendor_id) ";
		Query query = entityManager.createNativeQuery(sql1, Asset.class);

		boolean processed = true;
		String previousVendor = "";

		// need to read a single one at a time because it can use up all the available memory if I don't
		while (processed) {
			query.setParameter(1, previousVendor);
			@SuppressWarnings("unchecked")
			List<Asset> list = query.getResultList();
			processed = false;  // if no results, this fill force exit from the loop
			if (CollectionUtils.isNotEmpty(list)) {
				processed = true;  // something was processed so we need to try again
				// we only need one vendorId for they should all be the same.  The merge method called here will take care of the others
					mergeDuplicateAssetsByVendor(list.get(0).getVendorId());
					previousVendor = list.get(0).getVendorId();
			}
		}
	}*/

/*	lnagy - NOT USED ANYMORE
 * @Transactional(propagation = Propagation.REQUIRED)
	public void mergeDuplicateAssetsByVendor(String vendorId) throws SQLException {
		String sql1 = "select * from ASSET_2_SOURCE a where a.asset_id in (select id from asset where vendor_id = ? )  " +
				" and a.source_id in (select  b.source_id from asset c, asset_2_Source b " +
			    " where c.id =  b.asset_id " +
 			    " group by  c.vendor_id, b.source_id having count(c.vendor_id) > 1 ) ";

		Query query = entityManager.createNativeQuery(sql1, AssetToSource.class);
		query.setParameter(1, vendorId);

		@SuppressWarnings("unchecked")
		List<AssetToSource> a2s = query.getResultList();

		AssetToSource previousA2s = null;

		if (CollectionUtils.isNotEmpty(a2s)) {
			// process here
			for (int x=0; x < a2s.size(); x++) {
				if (null == previousA2s || previousA2s.getSourceId() != a2s.get(x).getSourceId() ) {
					previousA2s = a2s.get(x);
					continue;
				}
				transferAsset(a2s.get(x).getAssetId(), previousA2s.getAssetId(), previousA2s.getSourceId());
			}
		}
	}*/

	// this method transfers the data to all tables associated with an asset to a different asset
	// for as long as all the assets belong to the same source.
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void transferAsset(Integer fromId, Integer toId) {
		try {

			// Changed for Bec Issue - Start
			boolean sameVendor = false;
			String sql = "select vendor_id as id from asset where id = ?";
			Query query = createNativeQuery(sql, "scalarId");
			query.setParameter(1, fromId);
			String fromVendorId = (String) query.getSingleResult();
			log.debug("fromVendorId: " + fromVendorId);

			sql = "select vendor_id as id from asset where id = ?";
			query = createNativeQuery(sql, "scalarId");
			query.setParameter(1, toId);
			String toVendorId = (String) query.getSingleResult();
			log.debug("toVendorId: " + toVendorId);

			sql = "select source_id as id from asset_2_source where asset_id = ?";
			query = createNativeQuery(sql, "scalarId");
			query.setParameter(1, fromId);
			int fromSourceId = (Integer) query.getSingleResult();
			log.debug("fromSourceId: " + fromSourceId);

			sql = "select source_id as id from asset_2_source where asset_id = ?";
			query = createNativeQuery(sql, "scalarId");
			query.setParameter(1, toId);
			int toSourceId = (Integer) query.getSingleResult();
			log.debug("toSourceId: " + toSourceId);

			if (fromSourceId != 0 && toSourceId != 0 && fromVendorId != null
					&& toVendorId != null) {
				if (fromSourceId == toSourceId) {
					if (fromVendorId.equals(toVendorId)) {
						log.debug("Same Vendor Id");
						sameVendor = true;
					}
				}
			}
			log.debug("sameVendor: " + sameVendor);
			if (sameVendor) {
			// Changed for Bec Issue - Ends

			log.debug(new Date() +  ": Transfering AssetId from:" + fromId + " to " + toId);

			// transfer po's
			//executeNativeQuery("update purchase_order_2_asset set asset_base_id = " + toId + " where asset_base_id = " + fromId, null);

			// transfer contract 2 asset
			//executeNativeQuery("update contract_2_asset set asset_base_id = " + toId + " where asset_base_id = " + fromId , null) ;

			// usage 2 size
			//executeNativeQuery("update usage_2_size set asset_base_id = " + toId + " where asset_base_id = " + fromId, null) ;

			// transfer asset use
			executeNativeQuery("update asset_use set asset_id = " + toId + " where asset_id = " + fromId, null);

			// transfer asset files
			executeNativeQuery("update asset_file set asset_id = " + toId + " where asset_id = " + fromId, null) ;

			// delete old asset to source
			executeNativeQuery("delete from asset_2_source where asset_id = " + fromId, null);


			// transfer asset perm ref
			executeNativeQuery("update asset_perm_ref set asset_id = " + toId + " where asset_id = " + fromId, null);

			// delete old asset if no other sources are using it
			executeNativeQuery("delete from asset where id = " + fromId, null) ;

			// delete old asset base if no other sources are using it
			//executeNativeQuery("delete from asset_base where id = " + fromId, null) ;
			// Changed for Bec Issue - Start
		}
		// Changed for Bec Issue - Ends
		} catch (Exception e) {
			log.error("transferAsset(): caught exception: ", e);
		}
	}

	/**
	 * Will update all references to po and contract to null and remove rows from mapping tables
	 * @param assetId
	 * @throws Exception
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public void clearAssetReferences(final int assetId) throws Exception {
		@SuppressWarnings("serial")
		ArrayList<Integer> params = new ArrayList<Integer>() {{add(assetId);}};

		// update the ref table
		executeNativeQuery("update asset_perm_ref set po_id = null, contract_id = null where asset_id = ?",
				params);

		// update the latest contract
		// latest_contract_id moved to au_source_perm_ref (and also have other contract/po columns there)
		//executeNativeQuery("update asset_use set latest_contract_id = null where asset_id = ?", params);
		String sql = "update au_source_perm_status set latest_contract_id = null, latest_po_id = null,"
			+ " active_contract_id = null, active_po_id = null, contract_credit_line = null"
			+ " where asset_use_id in (select id from asset_use where asset_id = ?)";
		executeNativeQuery(sql, params);

		// Save PO and Contract ids for possible deletion below
		// In most cases there will only be a single id but there could be more than one
		sql = "select purchase_order_id as id from purchase_order_2_asset where asset_base_id = ?";
		Query query = createNativeQuery(sql, "scalarId");
		query.setParameter(1, assetId);
		@SuppressWarnings("unchecked")
		List<Integer> poList = query.getResultList();

		sql = "select contract_id as id from contract_2_asset where asset_base_id = ?";
		query = createNativeQuery(sql, "scalarId");
		query.setParameter(1, assetId);
		@SuppressWarnings("unchecked")
		List<Integer> contractList = query.getResultList();

		// delete the reference
		executeNativeQuery("delete from purchase_order_2_asset where asset_base_id = ?",
				params);
		// delete the reference
		executeNativeQuery("delete from contract_2_asset where asset_base_id = ?",
				params);

		// Now delete the PO / Contract if there are no asset associations left
		// - delete contract first because it may reference the PO
		for (Integer contractId : contractList) {
			log.debug("clearAssetReferences: looking at contractId: " + contractId);
			deleteContractIfNoAssetsLeft(contractId);
		}

		for (Integer poId : poList) {
			log.debug("clearAssetReferences: looking at poId: " + poId);
			deletePOIfNoAssetsLeft(poId);
		}
	}

	// called by clearAssetReferences()
	private void deleteContractIfNoAssetsLeft(int contractId) {
		String sql = "select count(*) as count from contract_2_asset where contract_id = ?";
		Query query = createNativeQuery(sql, "scalarCount");
		query.setParameter(1, contractId);
		// For count, DB2 and MS SQL Server return Integer but MySQL returns BigInteger
		// Integer and BigInteger inherit from Number
		Number resultNum = (Number) query.getSingleResult();
		int count = resultNum.intValue();
		if (count > 0)  return;

		log.debug("clearAssetReferences(): contractId [" + contractId + "] has no assets left so will delete");

		// Need to get condition ids so can delete after delete association to contract
		sql = "select condition_id as id from contract_2_condition where contract_id = ?";
		query = createNativeQuery(sql, "scalarId");
		query.setParameter(1, contractId);
		@SuppressWarnings("unchecked")
		List<Integer> conditionList = query.getResultList();

		ArrayList<Integer> params = new ArrayList<Integer>();
		params.add(contractId);
		executeNativeQuery("delete from contract_2_condition where contract_id = ?", params);
		executeNativeQuery("delete from contract_file where contract_id = ?", params);
		executeNativeQuery("delete from payment_request where contract_id = ?", params);
		executeNativeQuery("delete from amendment where contract_id = ?", params);
		executeNativeQuery("delete from contract where id = ?", params);

		// finally delete orphaned conditions
		deleteConditions(conditionList);
	}

	// called by clearAssetReferences()
	private void deletePOIfNoAssetsLeft(int poId) {
		String sql = "select count(*) as count from purchase_order_2_asset where purchase_order_id = ?";
		Query query = createNativeQuery(sql, "scalarCount");
		query.setParameter(1, poId);
		// For count, DB2 and MS SQL Server return Integer but MySQL returns BigInteger
		// Integer and BigInteger inherit from Number
		Number resultNum = (Number) query.getSingleResult();
		int count = resultNum.intValue();
		if (count > 0)  return;

		log.debug("clearAssetReferences(): poId [" + poId + "] has no assets left so will delete");

		// POs no longer have conditions so don't need to worry about that

		ArrayList<Integer> params = new ArrayList<Integer>();
		params.add(poId);
		executeNativeQuery("delete from purchase_order where id = ?", params);
	}

	/* this method is used by the clear asset function */
	@Transactional(propagation = Propagation.REQUIRED)
	public void deleteContract(int contractId) {
		log.debug("deleteContract():  contractId: " + contractId);

		// Need to get condition ids so can delete after delete association to contract
		String sql = "select condition_id as id from contract_2_condition where contract_id = ?";
		Query query = createNativeQuery(sql, "scalarId");
		query.setParameter(1, contractId);
		@SuppressWarnings("unchecked")
		List<Integer> conditionList = query.getResultList();

		ArrayList<Integer> params = new ArrayList<Integer>();
		params.add(contractId);
		executeNativeQuery("delete from contract_2_asset where contract_id = ?", params);
		executeNativeQuery("delete from contract_2_condition where contract_id = ?", params);
		executeNativeQuery("delete from contract_file where contract_id = ?", params);
		executeNativeQuery("delete from contract_file_name where contract_id = ?", params); //Added for DM-532 - it has to delete all files in contract_file_name also
		executeNativeQuery("delete from payment_request where contract_id = ?", params);
		executeNativeQuery("delete from amendment where contract_id = ?", params);
		// asset_perm_ref has ON DELETE CASCADE on contract_id so don't have to delete that here
		// au_source_perm_status has ON DELETE CASCADE on latest_contract_id and active_contract_id so ditto
		executeNativeQuery("delete from contract where id = ?", params);

		// finally delete orphaned conditions
		deleteConditions(conditionList);
		log.debug("contract " + contractId + " deleted");
	}

	/* this method is used by the clear asset function */
	@Transactional(propagation = Propagation.REQUIRED)
	public void deletePurchaseOrder(int poId) {
		log.debug("deletePurchaseOrder(): poId: " + poId);

		// POs no longer have conditions so don't need to worry about that

		ArrayList<Integer> params = new ArrayList<Integer>();
		params.add(poId);
		executeNativeQuery("delete from purchase_order_2_asset where purchase_order_id = ?", params);
		executeNativeQuery("delete from purchase_order where id = ?", params);
		// asset_perm_ref has ON DELETE CASCADE on po_id so don't have to delete that here
		// au_source_perm_status has ON DELETE CASCADE on latest_po_id and active_po_id so ditto

		log.debug("Purchase Order " + poId + " deleted");
	}

	private void deleteConditions(List<Integer> conditionIds) {
		if (conditionIds.size() == 0) return;
		String sql = "delete from condition_value where id in ("
			+ StringUtils.join(conditionIds, ", ") + ")";
		executeNativeQuery(sql, null);
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<Integer> getContractIdsForAssetAndCW(int assetId, int cwId) {
		String sql = "select c.id from contract c, contract_2_asset map where map.contract_id = c.id and map.asset_base_id = ? and c.cw_id = ?";
		Query query = entityManager.createNativeQuery(sql, "scalarId");
		query.setParameter(1, assetId);
		query.setParameter(2, cwId);
		@SuppressWarnings("unchecked")
		List<Integer> list = query.getResultList();
		return list;
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<Integer> getPOIdsForAssetAndCW(int assetId, int cwId) {
		String sql = "select po.id from purchase_order po, purchase_order_2_asset map"
			+ " where map.purchase_order_id = po.id and map.asset_base_id = ? and po.cw_id = ?";
		Query query = entityManager.createNativeQuery(sql, "scalarId");
		query.setParameter(1, assetId);
		query.setParameter(2, cwId);
		@SuppressWarnings("unchecked")
		List<Integer> list = query.getResultList();
		return list;
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<Integer> getAssetIdsForContract(int contractId) {
		String sql = "select asset_base_id as id from contract_2_asset where contract_id = ?";
		Query query = entityManager.createNativeQuery(sql, "scalarId");
		query.setParameter(1, contractId);
		@SuppressWarnings("unchecked")
		List<Integer> list = query.getResultList();
		return list;
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public boolean doesAssetHaveContractsOutsideCW(int assetId, int cwId) {
		final String sql = "select count(*) as count from contract c, contract_2_asset map "
			+ "where map.contract_id = c.id and map.asset_base_id = ? and c.cw_id != ?";
		Query query = entityManager.createNativeQuery(sql, "scalarCount");
		query.setParameter(1, assetId);
		query.setParameter(2, cwId);
		// For count, DB2 and MS SQL Server return Integer but MySQL returns BigInteger
		// Integer and BigInteger inherit from Number
		Number result = (Number) query.getSingleResult();

		return result.intValue() > 0;
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public boolean doesAssetHavePOsOutsideCW(int assetId, int cwId) {
		final String sql = "select count(*) as count from purchase_order po, purchase_order_2_asset map "
			+ "where map.purchase_order_id = po.id and map.asset_base_id = ? and po.cw_id != ?";
		Query query = entityManager.createNativeQuery(sql, "scalarCount");
		query.setParameter(1, assetId);
		query.setParameter(2, cwId);
		// For count, DB2 and MS SQL Server return Integer but MySQL returns BigInteger
		// Integer and BigInteger inherit from Number
		Number result = (Number) query.getSingleResult();

		return result.intValue() > 0;
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public boolean doesAssetHaveAssetUsesOutsideCW(int assetId, int cwId) {
		final String sql = "select count(*) as count from asset_use "
			+ "where asset_id = ? and cw_id != ?";
		Query query = entityManager.createNativeQuery(sql, "scalarCount");
		query.setParameter(1, assetId);
		query.setParameter(2, cwId);
		// For count, DB2 and MS SQL Server return Integer but MySQL returns BigInteger
		// Integer and BigInteger inherit from Number
		Number result = (Number) query.getSingleResult();

		return result.intValue() > 0;
	}

	public SourceRepository getSourceRepository() {
    	return sourceRepository;
    }

	public void setSourceRepository(SourceRepository sourceRepository) {
    	this.sourceRepository = sourceRepository;
    }

	// Start : Added for DM-1606
	// Check whether PO created for given asset id in PURCHASE_ORDER table
	// is_permission_request = false : PO is created else permission Request is created
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public boolean checkForPoCreation(int assetId) {
		final String sql = "select count(id) as count from PURCHASE_ORDER where is_permission_request=? and id in  "
				+ "(select purchase_order_id from purchase_order_2_asset where asset_base_id=?)";
			Query query = entityManager.createNativeQuery(sql, "scalarCount");
			query.setParameter(1, false);
			query.setParameter(2, assetId);
			// For count, DB2 and MS SQL Server return Integer but MySQL returns BigInteger
			// Integer and BigInteger inherit from Number
			Number result = (Number) query.getSingleResult();
			log.debug("checkForPoCreation() sql "+sql);

			return result.intValue() > 0;
		}
	// End : Added for DM-1606
}