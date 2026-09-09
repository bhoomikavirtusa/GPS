package com.wiley.permissions.repositories;

import java.util.ArrayList;
import java.util.Date;	// Added for DM-534
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.sql.DataSource;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.atomikos.jdbc.AtomikosDataSourceBean;
import com.wiley.permissions.common.bean.BeanMergeException;
import com.wiley.permissions.common.bean.BeanUtility;
import com.wiley.permissions.common.utils.FixCommonWorks;
import com.wiley.permissions.common.utils.UniqueConstraintViolationException;
import com.wiley.permissions.common.utils.ValidateException;
import com.wiley.permissions.domain.message.MessageException;
import com.wiley.permissions.domain.persistence.permissions.CommonWork;
import com.wiley.permissions.domain.persistence.permissions.CommonWorkStatus;
import com.wiley.permissions.domain.persistence.permissions.Component;
import com.wiley.permissions.domain.persistence.permissions.ComponentCategory;
import com.wiley.permissions.domain.persistence.permissions.Condition;
import com.wiley.permissions.domain.persistence.permissions.CwFile;
import com.wiley.permissions.domain.persistence.permissions.CwHistory;
import com.wiley.permissions.domain.persistence.permissions.CwPhotoEstimate;
import com.wiley.permissions.domain.persistence.permissions.ExportAsset;	// Added for DM-534
import com.wiley.permissions.domain.persistence.permissions.MasterAgreementDeal;
import com.wiley.permissions.domain.persistence.permissions.MediaType;
import com.wiley.permissions.domain.persistence.permissions.PermissionStatus;
import com.wiley.permissions.domain.persistence.permissions.Product;
import com.wiley.permissions.domain.persistence.permissions.ProductEdition;
import com.wiley.permissions.domain.persistence.permissions.Role;
import com.wiley.permissions.domain.persistence.permissions.Source;
import com.wiley.permissions.domain.persistence.permissions.Usage;
import com.wiley.permissions.domain.persistence.permissions.User;
import com.wiley.permissions.domain.persistence.permissions.UserGroup;
import com.wiley.permissions.persistence.PersistenceException;
import com.wiley.permissions.persistence.repository.JPARepository;
import com.wiley.permissions.services.view.AssetSummaryView;
import com.wiley.sf.common.lang.ArgUtil;
import com.wiley.sf.common.monitor.PerformanceMonitor.PerfTimer;
public class CommonWorkRepository extends JPARepository {
	private static final Log log = LogFactory.getLog(CommonWorkRepository.class);

	private AssetUseRepository assetUseRepository;
	private ProductRepository productRepository;

	private DataSource dataSource;

	@PersistenceContext(unitName = "permissions")
	protected EntityManager entityManager;

	@Override
	// I see we want to use it outside the scope of this class (like in Services)
	// I have to make it public for that
	public EntityManager getEntityManager() {
		return entityManager;
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public boolean needsPaymentRequest(int cwId)
	{
		final String sql = "select count(id) as count from asset_use au where au.cw_id = ? and au.need_payment_request is true";
		Query query = entityManager.createNativeQuery(sql, "scalarCount");
		query.setParameter(1, cwId);
		// For count, DB2 and MS SQL Server return Integer but MySQL returns BigInteger
		// Integer and BigInteger inherit from Number
		Number resultNum = (Number) query.getSingleResult();
		int result = resultNum.intValue();

		return result > 0;
	}

    /**
     * To be NoPermRequired capable, all assets must be non-managed
     * (and not third party - this condition has been removed by James).
     *//*
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public boolean isNoPermRequiredCapable(int cwId) {
		final String sql = "select count(au.id) as count from asset_use au join asset a on au.asset_id = a.id " +
			" where au.cw_id = ? and a.is_managed is true";
		Query query = entityManager.createNativeQuery(sql, "scalarCount");
		query.setParameter(1, cwId);
		// For count, DB2 and MS SQL Server return Integer but MySQL returns BigInteger
		// Integer and BigInteger inherit from Number
		Number resultNum = (Number) query.getSingleResult();
		int result = resultNum.intValue();

		return result == 0;
	}*/

    /**
     * To be NoPermRequired capable, all assets must be non-managed
     * (and not third party - this condition has been removed by James).
     */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public boolean isNoPermRequiredCapable(int cwId, boolean interior) {
		// James says to use Component Category to determine if Cover, not usage_type
		// No/null Component counts as Interior
		String sql = "select count(au.id) as count from asset_use au join asset a on au.asset_id = a.id " +
			" join component c on au.component_id = c.id where au.cw_id = ? and a.is_managed is true" +
			" and au.component_id = c.id and c.category = 'CVW'";
		if (interior) {
			sql = "select count(au.id) as count from asset_use au join asset a on au.asset_id = a.id " +
			" left join component c on au.component_id = c.id where au.cw_id = ? and a.is_managed is true" +
			" and (c.category is null or c.category != 'CVW')";
		}
		Query query = entityManager.createNativeQuery(sql, "scalarCount");
		query.setParameter(1, cwId);
		// For count, DB2 and MS SQL Server return Integer but MySQL returns BigInteger
		// Integer and BigInteger inherit from Number
		Number resultNum = (Number) query.getSingleResult();
		int result = resultNum.intValue();

		return result == 0;
	}

	/**
     * To be Complete capable, all assets must be in GRANTED or CANCELED
     */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public boolean isCompleteCapable(int cwId, boolean interior) {
		// James says to use Component Category to determine if Cover, not usage_type
		// No/null Component counts as Interior
		String sql = "select count(au.id) as count from asset_use au, component c " +
			" where au.cw_id = ? and (au.permission_status is null or (au.permission_status not in ("
		+ PermissionStatus.getSQLClause(PermissionStatus.COMPLETE_GROUP) + ")) " +
		" or (au.permission_status in ("
				+ PermissionStatus.getSQLClause(PermissionStatus.MIGRATED_GROUP) + ")"
		+ " ))" +
		" and (c.category is null or c.category != 'CVW')";
		if (interior) {
			sql = "select count(au.id) as count from asset_use au left join component c on c.id = au.component_id " +
			" where au.cw_id = ? and (au.permission_status is null " +
			" or (au.permission_status not in ("
					+ PermissionStatus.getSQLClause(PermissionStatus.COMPLETE_GROUP) + ")) " +
			" or (au.permission_status in ("
					+ PermissionStatus.getSQLClause(PermissionStatus.MIGRATED_GROUP) + ")"
			+ " ))" +
			" and (c.category is null or c.category != 'CVW')";
		}
		log.debug("isCompleteCapable(): sql = " + sql);
		Query query = entityManager.createNativeQuery(sql, "scalarCount");
		query.setParameter(1, cwId);
		// For count, DB2 and MS SQL Server return Integer but MySQL returns BigInteger
		// Integer and BigInteger inherit from Number
		Number resultNum = (Number) query.getSingleResult();
		int result = resultNum.intValue();

		return result == 0;
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public int getTotalPhotoCount(int cwId) {
		final String sql = "select count(*) as count from asset_use au, asset a where au.asset_id = a.id"
			+ " and cw_id = ? and a.media_type = '" + MediaType.PHOTO.getCode() + "'";
		Query q = entityManager.createNativeQuery(sql, "scalarCount");
		q.setParameter(1, cwId);

		// For count, DB2 and MS SQL Server return Integer but MySQL returns BigInteger
		// Integer and BigInteger inherit from Number
		Number resultNum = (Number) q.getSingleResult();
		return resultNum.intValue();
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public Number getCoverAssetsCount(int cwId, String filter) {
		// this method should really be called PhotoExternalAssetsCount (include Spine)
		final String sql = "select count(*) as count from (select distinct a.id from asset_use au join asset a on au.asset_id = a.id " +
			" left join contract_2_asset c2a on c2a.asset_base_id = a.id left join contract c on c.id = c2a.contract_id and c.cw_id = ? " +
			" left join purchase_order_2_asset p2a on p2a.asset_base_id = a.id left join purchase_order p on p.id = p2a.purchase_order_id and p.cw_id = ? " +
			" where au.cw_id = ? and au.usage_type in ('" + Usage.FRONT_COVER.getCode() + "', '" +
			Usage.BACK_COVER.getCode() + "', '" + Usage.SPINE.getCode() + "')" +
			// " and a.media_type = '" + MediaType.PHOTO.getCode() + "' "
			filter + ") alias";
		//log.debug("getPhotoCoverAssetsCount(): cwId = " + cwId + ", sql = " + sql);
		Query query = entityManager.createNativeQuery(sql, "scalarCount");
		query.setParameter(1, cwId);
		query.setParameter(2, cwId);
		query.setParameter(3, cwId);
		// For count, DB2 and MS SQL Server return Integer but MySQL returns BigInteger
		// Integer and BigInteger inherit from Number
		Number resultNum = (Number) query.getSingleResult();
		return resultNum;
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public Number getInternalAssetsCount(int cwId, String filter)
	{
		final String sql = "select count(*) as count from (select distinct a.id  from asset_use au join asset a on au.asset_id = a.id " +
			" left join contract_2_asset c2a on c2a.asset_base_id = a.id left join contract c on c.id = c2a.contract_id and c.cw_id = ? " +
			" left join purchase_order_2_asset p2a on p2a.asset_base_id = a.id left join purchase_order p on p.id = p2a.purchase_order_id and p.cw_id = ? " +
			" where au.cw_id = ? and au.usage_type not in ('" + Usage.FRONT_COVER.getCode() + "', '" +
			Usage.BACK_COVER.getCode() + "', '" + Usage.SPINE.getCode() + "')" +
			// " and a.media_type='" + MediaType.PHOTO.getCode() + "' "
			filter + ") alias";
		Query query = entityManager.createNativeQuery(sql, "scalarCount");
		query.setParameter(1, cwId);
		query.setParameter(2, cwId);
		query.setParameter(3, cwId);
		// For count, DB2 and MS SQL Server return Integer but MySQL returns BigInteger
		// Integer and BigInteger inherit from Number
		Number resultNum = (Number) query.getSingleResult();
		return resultNum;
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public Number getAssetsCount(int cwId, String filter)
	{
		final String sql = "select count(*) as count from (select distinct a.id  from asset_use au join asset a on au.asset_id = a.id " +
			" where au.cw_id = ?" +
			// " and a.media_type='" + MediaType.PHOTO.getCode() + "' "
			filter + ") alias";
		Query query = entityManager.createNativeQuery(sql, "scalarCount");
		query.setParameter(1, cwId);
		// For count, DB2 and MS SQL Server return Integer but MySQL returns BigInteger
		// Integer and BigInteger inherit from Number
		Number resultNum = (Number) query.getSingleResult();
		return resultNum;
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public Number getCostAssetsCount(int cwId, String filter)
	{   //Made change to query for INC_53852
		final String sql = "select sum(c2a.price) as scalarDouble from contract_2_asset c2a,contract c where c.id=c2a.contract_id and c2a.asset_base_id in "
				+ "(select distinct au.asset_id from asset_use au join asset a on a.id=au.asset_id where au.cw_id=? " + filter + ") and c.cw_id=?";
			//"and au.usage_type not in ('" + Usage.FRONT_COVER.getCode() + "', '" +
			//Usage.BACK_COVER.getCode() + "', '" + Usage.SPINE.getCode() + "')" +
			// " and a.media_type='" + MediaType.PHOTO.getCode() + "' "
		Query query = entityManager.createNativeQuery(sql);
		
		query.setParameter(1, cwId);
		//Made change  for INC_53852
		query.setParameter(2, cwId);
		// For count, DB2 and MS SQL Server return Integer but MySQL returns BigInteger
		// Integer and BigInteger inherit from Number
		Number resultNum = (Number) query.getSingleResult();
		return (resultNum == null) ? 0 : resultNum;
	}

	public void loadAssetSummaryByFilter (AssetSummaryView summary, String key, int cwId, String filter, boolean bWithCost) {
		summary.getCovers().put(key, getCoverAssetsCount(cwId, filter));
		summary.getInternals().put(key, getInternalAssetsCount(cwId, filter));
		if (bWithCost)
			summary.getCosts().put(key, getCostAssetsCount(cwId, filter));
	}

	public AssetSummaryView loadAssetSummary(int cwId, boolean includeCovers, String mediaTypes, String userGroups) throws PersistenceException {
		AssetSummaryView summary = new AssetSummaryView();
		mediaTypes = StringUtils.isBlank(mediaTypes) ? "" : " and a.media_type in(" + mediaTypes + ")";
		userGroups = StringUtils.isBlank(userGroups) ? "" : " and au.user_group_id in(" + userGroups + ")";
		loadAssetSummaryByFilter (summary, "new_rm", cwId, " and au.is_canceled = 0 and au.reused_from_prev_ed = 0 and a.is_managed = 1 " + mediaTypes + userGroups, true);
		loadAssetSummaryByFilter (summary, "new_royaltyfree", cwId, " and au.is_canceled = 0 and au.reused_from_prev_ed = 0 and a.is_royalty_free=1 and (c2a.price is null or c2a.price = 0)"  + mediaTypes + userGroups, true);
		loadAssetSummaryByFilter (summary, "new_royaltyfree$$", cwId, " and au.is_canceled = 0 and au.reused_from_prev_ed = 0 and a.is_royalty_free=1 and c2a.price != 0 " + mediaTypes + userGroups, true);
		loadAssetSummaryByFilter (summary, "new_free", cwId, " and au.is_canceled = 0 and au.reused_from_prev_ed = 0 and a.is_royalty_free=0 and (c2a.price is null or c2a.price = 0)" + mediaTypes + userGroups, true);

		loadAssetSummaryByFilter (summary, "reuse_rm", cwId, " and au.is_canceled = 0 and au.reused_from_prev_ed = 1 and a.is_managed = 1" + mediaTypes + userGroups, true);
		loadAssetSummaryByFilter (summary, "reuse_royaltyfree", cwId, " and au.is_canceled = 0 and au.reused_from_prev_ed = 1 and a.is_royalty_free=1 and (c2a.price is null or c2a.price = 0)" + mediaTypes + userGroups, true);
		loadAssetSummaryByFilter (summary, "reuse_royaltyfree$$", cwId, " and au.is_canceled = 0 and au.reused_from_prev_ed = 1 and a.is_royalty_free=1 and c2a.price != 0" + mediaTypes + userGroups, true);
		loadAssetSummaryByFilter (summary, "reuse_free", cwId, " and au.is_canceled = 0 and au.reused_from_prev_ed = 1 and a.is_royalty_free=0 and (c2a.price is null or c2a.price = 0)" + mediaTypes + userGroups, true);

		List<PermissionStatus> statuses = loadAll(PermissionStatus.class);
		for (PermissionStatus status : statuses) {
			// Start : Updated for DM-1606
			String filter = mediaTypes + userGroups ;
			if(status.getCode().equalsIgnoreCase(PermissionStatus.FORM_SENT.getCode())) {
				filter = filter + " and au.permission_status='" + status.getCode() + "' and p.is_permission_request=true";
			}
			else if(status.getCode().equalsIgnoreCase(PermissionStatus.FORM_WAITING_ON_INVOICE.getCode())) {
				filter = filter + " and au.permission_status='" + PermissionStatus.FORM_SENT.getCode() + "' and p.is_permission_request=false";
			}
			else {
				filter = filter + " and au.permission_status='" + status.getCode()+"'";
			}
			loadAssetSummaryByFilter (summary, status.getCode(), cwId, filter , false);
			// End : Updated for DM-1606
		}
		/*
		loadAssetSummaryByFilter (summary, "canceled", cwId, " and au.is_canceled = 1" + mediaTypes + userGroups, true);
		loadAssetSummaryByFilter (summary, "replaced", cwId, "  and au.is_canceled = 1 and au.cancel_replacement_id is not null" + mediaTypes + userGroups, true);
		loadAssetSummaryByFilter (summary, "noSource", cwId, " and au.permission_status='" + PermissionStatus.NO_SOURCE.getCode() + "'" + mediaTypes + userGroups, false);
		loadAssetSummaryByFilter (summary, "requestSent", cwId, " and au.permission_status IN (" + PermissionStatus.getSQLClause(PermissionStatus.FORM_SENT_GROUP) + ") and p.is_permission_request=1" + mediaTypes + userGroups, false);
		loadAssetSummaryByFilter (summary, "poSent", cwId, " and au.permission_status IN (" + PermissionStatus.getSQLClause(PermissionStatus.FORM_SENT_GROUP) + ") and p.is_permission_request=0" + mediaTypes + userGroups, false);
		loadAssetSummaryByFilter (summary, "grantedPublicDomain", cwId, " and au.permission_status='" + PermissionStatus.GRANTED_PUBLIC_DOMAIN + "'" + mediaTypes + userGroups, false);
		loadAssetSummaryByFilter (summary, "grantedWileyOwned", cwId, " and au.permission_status='" + PermissionStatus.GRANTED_WILEY_OWNED + "'" + mediaTypes + userGroups, false);
		loadAssetSummaryByFilter (summary, "grantedAuthorCreated", cwId, " and au.permission_status='" + PermissionStatus.GRANTED_AUTHOR_CREATED + "'" + mediaTypes + userGroups, false);
		loadAssetSummaryByFilter (summary, "grantedFairUse", cwId, " and au.permission_status='" + PermissionStatus.GRANTED_FAIR_USE + "'" + mediaTypes + userGroups, false);
		loadAssetSummaryByFilter (summary, "grantedRFUnlimited", cwId, " and au.permission_status='" + PermissionStatus.GRANTED_RF_UNLIMITED + "'"  + mediaTypes + userGroups, false);
		loadAssetSummaryByFilter (summary, "grantedRFLimited", cwId, " and au.permission_status IN (" + PermissionStatus.getSQLClause(PermissionStatus.GRANTED_ROYALTY_FREE_LIMITED_GROUP) + ")" + mediaTypes + userGroups, false);
		loadAssetSummaryByFilter (summary, "grantedLimited", cwId, " and au.permission_status IN ('" + PermissionStatus.GRANTED_LIMITED + "', '" + PermissionStatus.GRANTED_LIMITED_PRINT + "')" + mediaTypes + userGroups, false);
		loadAssetSummaryByFilter (summary, "granted", cwId, " and au.permission_status='" + PermissionStatus.GRANTED + "'" + mediaTypes + userGroups, false);
		loadAssetSummaryByFilter (summary, "contractInsufficient", cwId, " and au.permission_status='" + PermissionStatus.CONTRACT_INSUFFICIENT + "'" + mediaTypes + userGroups, false);
		loadAssetSummaryByFilter (summary, "outOfCompliance", cwId, " and au.permission_status IN (" + PermissionStatus.getSQLClause(PermissionStatus.OUT_OF_COMPLIANCE_GROUP) + ")" + mediaTypes + userGroups, false);
		loadAssetSummaryByFilter (summary, "amendmentSent", cwId, " and au.permission_status='" + PermissionStatus.AMENDMENT_SENT + "'" + mediaTypes + userGroups, false);
		loadAssetSummaryByFilter (summary, "illegal", cwId, " and au.permission_status='" + PermissionStatus.ILLEGAL + "'" + mediaTypes + userGroups, false);
		 */
		// summary.setStatusCounts(getAssetUseStatusCountByStatus(cwId, includeCovers));

		return summary;
	}

	public void loadAssetCountsByFilter (AssetSummaryView summary, String key, int cwId, String filter) {
		summary.getInternals().put(key, getAssetsCount(cwId, filter));
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public AssetSummaryView loadAssetsCountByUserGroup(int cwId, boolean includeCovers) throws PersistenceException {
		AssetSummaryView summary = new AssetSummaryView();
		loadAssetCountsByFilter (summary, "total", cwId, " ");
		List<UserGroup> userGroups = loadAll(UserGroup.class);
		for (UserGroup userGroup : userGroups) {
			String group = " and au.user_group_id =" + userGroup.getId();
			loadAssetCountsByFilter (summary, userGroup.getName() + "_count", cwId, " " + group);
			loadAssetCountsByFilter (summary, userGroup.getName() + "_workflow", cwId, " and au.permission_status IN (" + PermissionStatus.getSQLClause(PermissionStatus.WORKFLOW_GROUP) + ")" + group);
			loadAssetCountsByFilter (summary, userGroup.getName() + "_complete", cwId, " and au.permission_status IN (" + PermissionStatus.getSQLClause(PermissionStatus.COMPLETE_GROUP) + ")" + group);
			loadAssetCountsByFilter (summary, userGroup.getName() + "_problem", cwId, " and au.permission_status IN (" + PermissionStatus.getSQLClause(PermissionStatus.PROBLEM_GROUP) + ")" + group);
		}

		return summary;
	}

	public int getPhotoInternalFreeAssetCount(Integer cwId) {
		return getInternalAssetsCount(cwId, " and a.is_royalty_free=0 and (c2a.price is null or c2a.price = 0)").intValue();
    }

	public int getPhotoCoverPickupAssetCount(Integer cwId) {
	    return getCoverAssetsCount(cwId, "and au.is_canceled = 0 and au.is_pickup = 1").intValue();
    }

	public int getPhotoInternalRoyaltyFreeAssetCount(Integer cwId) {
		return getInternalAssetsCount(cwId, " and au.is_canceled=0 and a.is_royalty_free=1").intValue();
    }

	/**
	 * Returns a CommonWork loaded by code - returns null if not found.
	 *
	 * @param code  Should be non-null
	 * @return
	 * @throws PersistenceException
	 */
	// lnagy - if propagation = REQUIRED does not get the products after updateProductFromPE
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public CommonWork loadByCode(String code) throws PersistenceException
	{
		try {
			TypedQuery<CommonWork> query = entityManager.createQuery("from CommonWork cw where cw.code = ?1", CommonWork.class);
			query.setParameter(1, code);
			return query.getSingleResult();
		}
		catch (NoResultException e) {
			return null;
		}
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public CommonWork loadByCodeWithProductCount(String code) throws PersistenceException
	{
		CommonWork cw = loadByCode(code);
		if (cw != null) cw.getProductCount();
		return cw;
	}

	@SuppressWarnings("unchecked")
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<Integer> loadCWIds() throws PersistenceException
	{
		Query query = entityManager.createNativeQuery(
			"select id from common_work cw", "scalarId");
		List<Integer> list = query.getResultList();
		return list;
	}

	// Not making read-only because this is used by CommonWorkService.togglePermissionComplete()
	// which updates the database
	@Transactional(propagation = Propagation.REQUIRED)
	public CommonWork loadByIdForProductIndex(int cwId) throws Exception {
		CommonWork cw = find(CommonWork.class, cwId);  // throws PersistenceException
		if (cw == null)  return null;

		for (Product p : cw.getProducts()) {
			productRepository.lazyLoadForIndex(p);  // throws Exception
		}
		return cw;
	}

	@SuppressWarnings("unchecked")
	@Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
	public List<Integer> loadCWIdsForStatusRecalculate() throws PersistenceException
	{
		// "as id" needed to match "scalarId"
		// count(*) is the number of asset uses for the CW (recalculate "smaller" CWs first having same date)
		Query query = entityManager.createNativeQuery(
			"select cw_id as id from asset_use group by cw_id order by min(last_updated_status), count(*)", "scalarId");
		List<Integer> list = query.getResultList();

		return list;
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<Integer> loadAssetIdsForCWId(int cwId) throws PersistenceException
	{
		PerfTimer timer = monitor.startTimer("CommonWorkService::loadAssetIdsByCWId");
		try {
			// Use "as id" to match scalarId
			Query query = entityManager.createNativeQuery(
					"select distinct asset_id as id from asset_use where cw_id = ?",
					"scalarId"
					);
			query.setParameter(1, cwId);
			@SuppressWarnings("unchecked")
			List<Integer> assetIdList = query.getResultList();
			return assetIdList;
		}
		finally {
			timer.stopTimer();
		}
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public Set<Integer> loadAssetIdsForCWIdAsSet(int cwId) throws PersistenceException {
		HashSet<Integer> set = new HashSet<Integer>();
		set.addAll(loadAssetIdsForCWId(cwId));
		return set;
	}

	/*
	 * Alternate methods -- would probably work but not tested
	 * -- Would need to change to be CommonWork-based
	 *
	 * @Transactional(propagation = Propagation.REQUIRED) public void
	 * addProductToUserWatch(int userId, int productId) throws
	 * PersistenceException { WatchedProduct wp = new WatchedProduct(userId,
	 * productId); persist(wp); }
	 *
	 * @Transactional(propagation = Propagation.REQUIRED) public WatchedProduct
	 * loadWatchedProduct(int userId, int productId) throws PersistenceException {
	 * try { WatchedProduct wp = loadByPK(WatchedProduct.class, new
	 * WatchedProductPK(userId, productId)); return wp; } catch
	 * (NoResultException ex) { return null; } }
	 *
	 * @Transactional(propagation = Propagation.REQUIRED) public void
	 * removeProductFromUserWatch(int userId, int productId) throws
	 * PersistenceException { WatchedProduct wp = loadWatchedProduct(userId,
	 * productId); if (wp != null) { remove(wp); } }
	 */

	/**
	 * Returns true or false if the selected common work is watched or not
	 *
	 * @param userId
	 * @param cwId
	 * @return boolean
	 * @throws PersistenceException
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public boolean isCwWatched(int userId, int cwId) throws PersistenceException
	{
		Query query = entityManager.createNativeQuery(
				"select count(cw_id) as count from watched_cw where user_id = ? and cw_id = ?",
				"scalarCount");

		query.setParameter(1, userId);
		query.setParameter(2, cwId);
		Number resultNum = (Number) query.getSingleResult();
		int result = resultNum.intValue();

		return result > 0;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void saveCwNotes(int cwId, String notes) throws PersistenceException
	{
		CommonWork cw = find(CommonWork.class, cwId);

		if (cw == null) { // We don't expect this to happen but check
			throw new PersistenceException("commonWorkId " + cwId + " not found!");
		}

		cw.setNotes(notes);  // JPA will automatically merge this without even calling merge
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public Component loadComponentByExternalId(String externalId) throws PersistenceException
	{
		try {
			TypedQuery<Component> query = entityManager.createQuery("from Component c where c.externalId = ?1", Component.class);
			query.setParameter(1, externalId);
			return query.getSingleResult();
		}
		catch (NoResultException nre) {
			return null;
		}
	}

	/**
	 * The following fields in component must be set prior to calling this
	 * method: product, name, category. Furthermore product.id must match an
	 * existing product and category code must match an existing category. (This
	 * method will set id and externalId.) The calling method should keep the
	 * Component returned instead of the one passed in in order to have the
	 * values for id, externalId, and dates.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public Component addComponentToCommonWork(Component component)
			throws UniqueConstraintViolationException, PersistenceException, MessageException
	{
		ArgUtil.notNull(component.getCommonWork(), "commonWork");
		ArgUtil.notNull(component.getName(), "name");
		ArgUtil.notNull(component.getCategory(), "category");

		Integer cwId = component.getCommonWork().getId();

		if (component.getSortOrder() == 0) {
			component.setSortOrder(((Number)executeSingleResultNamedQuery("Component.getNextSortOrderInCW",
					new Object[] {component.getCommonWork().getId()})).intValue());
		}

		Number count = (Number)executeSingleResultNamedQuery("Component.checkForDuplicatesInCW",
				new Object[] {cwId, component.getName(), component.getSortOrder()});

		if (count.intValue() > 0) {
			throw new UniqueConstraintViolationException("Cannot insert component - name ["
					+ component.getName() + "] or sort [" + component.getSortOrder() + "] is a duplicate for commonWork id ["
					+ component.getCommonWork().getId() + "].");
		}

		CommonWork cw = entityManager.getReference(CommonWork.class, cwId);
		component.setCommonWork(cw);

		entityManager.persist(component);

		return component;
	}

	/**
	 * Component has a unique constraint on (cw_id, name). Return true if
	 * an insert of the given component will violate the constraint. The given
	 * component is expected to have a non-null cw_id and name.
	 */
	public boolean checkComponentForDuplicate(Component component) throws PersistenceException
	{
		ArgUtil.notNull(component.getCommonWork(), "commonWork");
		ArgUtil.notNull(component.getName(), "name");
		ArgUtil.notNull(component.getSortOrder(), "sortOrder");

		Component c = loadComponentByName(component.getCommonWork().getId(), component.getName());

		return c != null;
	}

	/**
	 * Returns null if the Component was not found.
	 *
	 * @param name  Must be non-null
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public Component loadComponentByName(int cwId, String name) throws PersistenceException
	{
		ArgUtil.notNull(name, "name");

		TypedQuery<Component> query = entityManager.createQuery(
				"from Component c where c.commonWork.id = ?1 and c.name = ?2", Component.class);
		query.setParameter(1, cwId);
		query.setParameter(2, name);

		try {
			return query.getSingleResult();
		}
		catch (NoResultException ex) {
			return null;
		}
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public Component loadComponentById(Integer componentId) throws PersistenceException
	{
		return find(Component.class, componentId);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public boolean deleteComponentById(int componentId) throws PersistenceException, ValidateException
	{
		Component component = find (Component.class, componentId);

		if (component == null)
			return false;

		boolean didDelete = false;

		// First check if component is referenced by any AssetUse objects.
		// (We don't just try the delete and catch ConstraintViolationException
		// because this is nested deeply in other exceptions and hard to get to)
		int count = getComponentReferencesCount(componentId);
		if (count > 0) {
			String msg = "Cannot delete component - used by " + count + " assets.";
			throw new ValidateException (msg);
		}
		else {
			entityManager.remove(component);
			didDelete = true;
		}

		return didDelete;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
	public int getComponentReferencesCount(int componentId) throws PersistenceException
	{
		Query q = entityManager.createNativeQuery(
				"select count(id) as count from asset_use where component_id = ?",
				"scalarCount");
		q.setParameter(1, componentId);
		// For count, DB2 and MS SQL Server return Integer but MySQL returns BigInteger
		// Integer and BigInteger inherit from Number
		Number result = (Number) q.getSingleResult();

		return result.intValue();
	}

	/**
	 * Returns a list of photo estimates for a commonWork
	 * @param cwId
	 * @return List<CwPhotoEstimate>
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<CwPhotoEstimate> loadPhotoEstimates (int cwId) {
		TypedQuery<CwPhotoEstimate> query = entityManager.createQuery("from CwPhotoEstimate up where up.commonWork.id = ?1",
				CwPhotoEstimate.class);
		query.setParameter(1, cwId);
		return query.getResultList();
	}

	/**
	 * Returns a list of files for a commonWork
	 * @param cwId
	 * @return List<CwFile>
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<CwFile> loadFiles (int cwId) {
		TypedQuery<CwFile> query = entityManager.createQuery("from CwFile up where up.commonWork.id = ?1",
				CwFile.class);
		query.setParameter(1, cwId);
		return query.getResultList();
	}

	/**
	 * Calculates the total of photo estimates for a CommonWork
	 * @param cwId
	 * @return double
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public double getTotalEstimatedCost(int cwId)
	{
		Query query = entityManager.createNativeQuery(
				"select sum(estimated_cost) as double_value from asset_use au where au.cw_id = ?",
				"scalarDouble");
		query.setParameter(1, cwId);
		Number resultNum = (Number) query.getSingleResult();
		return (resultNum == null) ? 0 : resultNum.doubleValue();
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void saveSelectedAuthorChapters(int authorId, int cwId, String [] selectedChapters) {
		final String deleteSql = "delete from author_2_component where user_id = ? and cw_id = ?";
		Query q = createNativeQuery(deleteSql);
		q.setParameter(1, authorId);
		q.setParameter(2, cwId);
		int numRows = q.executeUpdate();
		log.debug("saveSelectedAuthorChapters(): deleted " + numRows + " rows.");

		final String insertSql = "insert into author_2_component (user_id, cw_id, component_id) values (?, ?, ?)";
		q = createNativeQuery(insertSql);
		int insertCount = 0;
		for (String id : selectedChapters) {
			q.setParameter(1, authorId);
			q.setParameter(2, cwId);
			q.setParameter(3, id);
			insertCount += q.executeUpdate();
		}
		log.debug("saveSelectedAuthorChapters(): inserted " + insertCount + " rows.");
	}

	/**
	 * Returns a list of chapter ids for a commonWork assigned to an author
	 * @param cwId
	 * @return List<Component>
	 */
	@SuppressWarnings("unchecked")
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<Integer> loadSelectedChaptersId (int authorId, int cwId, boolean includeCovers) {
		Query query = entityManager.createNativeQuery("select * from component " +
				"where id in (select component_id from author_2_component where user_id=? and cw_id=?)" +
				(includeCovers ? " union select * from component where cw_id=? and category='" + ComponentCategory.COVER.getCode() + "'": ""),
				"scalarId");
		query.setParameter(1, authorId);
		query.setParameter(2, cwId);
		if (includeCovers)
			query.setParameter(3, cwId);
		return query.getResultList();
	}

	/**
	 * Returns a list of chapter for a commonWork assigned to an author
	 * @param cwId
	 * @return List<Component>
	 */
	@SuppressWarnings("unchecked")
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<Component> loadSelectedChapters (int authorId, int cwId, boolean includeCovers) {
		String sql = "select * from component " +
			"where id in (select component_id from author_2_component where user_id = ? and cw_id = ?)" +
			(includeCovers ? " union select * from component where cw_id=? and category='" + ComponentCategory.COVER.getCode() + "'": "");
		Query query = entityManager.createNativeQuery(sql, Component.class);
		query.setParameter(1, authorId);
		query.setParameter(2, cwId);
		if (includeCovers)
			query.setParameter(3, cwId);
		return query.getResultList();
	}

	/**
	 * Returns a list of chapters for a commonWork
	 * @param cwId
	 * @return List<Component>
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<Component> loadChapterList (int cwId) {
		return loadComponentListByComponentType (cwId, ComponentCategory.CHAPTER, true);
	}

	/**
	 * Returns a list of chapters for a commonWork
	 * @param cwId
	 * @return List<Component>
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<Component> loadComponentListByComponentType (int cwId, ComponentCategory type, boolean include) {
		TypedQuery<Component> query = entityManager.createQuery("from Component c where c.commonWork.id = ?1 and " +
				"c.category.code" + (include ? "=" : "!=") + "'" + type.getCode() + "'" + "order by sortOrder",
				Component.class);
		query.setParameter(1, cwId);
		return query.getResultList();
	}

	/**
	 * Returns a string containing all component names separated by colla, or the string "All" if all were selected
	 * @param cwId
	 * @param String componentIds - list of ids separated by comma
	 * @return String
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public String loadComponentListNames (int cwId, String componentIds, Integer userGroupId) {
		String sql = "select id from component where id not in (" + componentIds + ") and cw_id=?";

		Query query = entityManager.createNativeQuery(sql, "scalarId");
		query.setParameter(1, cwId);
		@SuppressWarnings("unchecked")
		List<Integer> ids = query.getResultList();
		// if no id, it means all were selected
		if (CollectionUtils.isEmpty(ids)) {
			return "All";
		} else {
			// get the names
			sql = "select group_concat(name) as string from component where id in (" + componentIds + ") and cw_id = ?";
			log.debug("loadComponentListNames(): sql: " + sql);
			query = entityManager.createNativeQuery(sql, "scalarString");
			query.setParameter(1, cwId);
			String names = (String) query.getSingleResult();
			return names;
		}
	}

	/**
	 * Returns a list of components for a commonWork
	 * @param cwId
	 * @return List<Component>
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<Component> loadComponentList (int cwId, boolean includeCovers) {
		String sql = "from Component c where c.commonWork.id = ?1 order by sortOrder";
		if (!includeCovers) {
			sql = "from Component c where c.commonWork.id = ?1 and c.category.code != '"
				+ ComponentCategory.COVER.getCode() + "' order by sortOrder";
		}
		TypedQuery<Component> query = entityManager.createQuery(sql, Component.class);
		query.setParameter(1, cwId);
		return query.getResultList();
	}


	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public boolean hasComponents(int cwId) {
		Query query = entityManager.createNativeQuery(
			"select count(*) as count from component where cw_id = ?", "scalarCount"
		);
		query.setParameter(1, cwId);

		// For count, DB2 and MS SQL Server return Integer but MySQL returns BigInteger
		// Integer and BigInteger inherit from Number
		Number resultNum = (Number) query.getSingleResult();
		return resultNum.intValue() > 0;
	}

	/**
	 * tries to copy the list of components to the product
	 * @param product
	 * @param componentList
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void copyComponents(int cwId, List<Component> componentList)
	{
		PerfTimer timer = monitor.startTimer("AssetUseService::copyComponents");

		if (CollectionUtils.isEmpty(componentList)) {
			log.debug("copyComponent(): no components to copy");
			return;
		}

		for (Component component : componentList) {
			try {
				copyComponent (cwId, component);
			}
			catch (Exception e) {
				log.debug("copyComponents(): failed to copy component " + component.getName());
			}
		}

		timer.stopTimer();
	}

	public Component copyComponent(int cwId, Component component)
		throws BeanMergeException, PersistenceException
	{
		log.debug("copyComponent(): cwId = " + cwId + ", component.name = " + component.getName());
		PerfTimer timer = monitor.startTimer("AssetUseService::copyComponent");

		// first try to load the component by name if exists
		Component newComponent = loadComponentByName(cwId, component.getName());

		// if no component found for current product, create one
		if (null == newComponent) {
			log.debug("copyComponent(): no component found: " + component.getName());

			newComponent = new Component();
			BeanUtility.merge(component, newComponent);
			// new one
			newComponent.setId(null);
			newComponent.setExternalId(null);
			// attach to new product
			CommonWork cw = new CommonWork ();
			cw.setId(cwId);
			newComponent.setCommonWork(cw);

			entityManager.persist(newComponent);
		}
		else {
			log.debug("copyComponent(): component with name ["
				+ component.getName() + "] already exists, doing nothing.");
		}

		timer.stopTimer();
		return newComponent;
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<Source> loadSourcesForCw(Integer cwId) {
		// TODO: Try and rewrite this as a JPQL query
		String sql = "select distinct s.id, s.name from asset_use au, asset a, asset_2_source a2s, source s "
			+ "where au.cw_id = ? and au.asset_id = a.id and a2s.asset_id = a.id and a2s.source_id = s.id "
			+ "order by s.name";
		Query q = entityManager.createNativeQuery(sql);
		q.setParameter(1, cwId);
		@SuppressWarnings("unchecked")
		List<Object []> results = q.getResultList();
		List<Source> sourceList = new ArrayList<Source>(results.size());
		for (Object [] objectArray : results) {
			Source s = new Source();
			s.setId((Integer) objectArray[0]);
			s.setName((String) objectArray[1]);
			sourceList.add(s);
		}
		return sourceList;
	}

	//Code Change for SS Task 3 - Added new parameter userGroupId
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<Object[]> loadSourceSummary(Integer cwId, Integer userGroupId) throws Exception
	{
		String sql = "select distinct asset_id from asset_use where cw_id=?";
		if(userGroupId > 0){
			 sql += " and user_group_id=?";
		}
		log.debug("sql: "+sql);
		Query q = entityManager.createNativeQuery(
				"select name, count, id from source join (" +
				"select source_id, count(*) as count from asset_2_source where asset_id " +
				"in (" +sql+ ") group by source_id) a " +
				"on source.id = a.source_id order by name");
		q.setParameter(1, cwId);
		if(userGroupId > 0){
			q.setParameter(2, userGroupId);
		}

		log.debug("query: "+q);
		@SuppressWarnings("unchecked")
		List<Object[]> results = q.getResultList();

		log.debug("results: "+results);

		return results;
	}

	//Code Change for SS Task 3 - Added new parameter userGroupId
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<Object[]> loadCwDetail(Integer cwId, Integer userGroupId) throws Exception
	{
		String sql = "select distinct asset_id from asset_use where cw_id=?";
		if(userGroupId > 0){
			 sql += " and user_group_id=?";
		}
		Query q = entityManager.createNativeQuery(
				"select name, count, id from source join (" +
				"select source_id, count(*) as count from asset_2_source where asset_id " +
				"in ("+ sql +") group by source_id) a " +
				"on source.id = a.source_id order by name");
		q.setParameter(1, cwId);
		if(userGroupId > 0){
			q.setParameter(2, userGroupId);
		}
		@SuppressWarnings("unchecked")
		List<Object[]> results = q.getResultList();

		return results;
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public double loadCostByCwSourceName(Integer cwId, Integer sourceId) throws Exception
	{
		String sql = "select sum(price) from contract where cw_id = ? and source_id = ? ";
		Query q = entityManager.createNativeQuery(sql);
		q.setParameter(1, cwId);
		q.setParameter(2, sourceId);

		Number resultNum = (Number) q.getSingleResult();
		// The sum of no rows is null - return 0 in this case
		return (resultNum == null) ? 0 : resultNum.doubleValue();
	}

	/**
	 * For now sum all contracts. Later when integrated with payment system
	 * may only sum those that are paid. Canceled contracts may still count
	 * if they were paid. Superseded contracts may also have been paid.
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public double getFinalCost(Integer cwId) {
		String sql = "select sum(price) from contract where cw_id = ?";
		Query q = entityManager.createNativeQuery(sql);
		q.setParameter(1, cwId);

		Number resultNum = (Number) q.getSingleResult();
		// The sum of no rows is null - return 0 in this case
		return (resultNum == null) ? 0 : resultNum.doubleValue();
	}

	/**
	 * Returns null if not found.
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public Double getMSToComp(int cwId) {
		String sql = "select reproduction_fees as double_value from cw_photo_estimate where photo_estimate_type = 'ms-to-comp' and cw_id = ?";
		Query q = entityManager.createNativeQuery(sql, "scalarDouble");
		q.setParameter(1, cwId);

		try {
			Number resultNum = (Number) q.getSingleResult();
			return resultNum.doubleValue();
		}
		catch (NoResultException ex) {
			return null;
		}
	}

	//rbongoni: Added for Asset Compliance Report
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<Object[]> loadComplianceReportData(Integer userGroupId,int cwId) throws Exception
	{
		log.debug("loadComplianceReportData(): entered...");
          //Manasa: Modified the field placing
		//This query is modified by Chandra as per RN ticket 151026-000296 and 151007-000929
		String userGroupSql = "";
		if (!userGroupId.equals(new Integer(0))) {
			userGroupSql = " and AU.user_group_id = " + userGroupId + " ";
		}
		/*Modified Query for INC_101082 when an asset has 2 sources status displayed with comma separator*/
		String sql = new StringBuilder().append("select co.name,AU.usage_type,AU.position,A.description, ")
				// Start : Query updated for DM-1606
				.append("case when PS.permission_status like 'formSent' and po.is_permission_Request=false then 'Waiting on Invoice' else PS.permission_status END as  permission_status,")
				.append("A.credit_line,S.display_name,")
				// End : Query updated for DM-1606
                .append("A.vendor_id,C.number,DATE_FORMAT(C.date,'%m-%d-%Y'),coalesce(C2A.price,0),C.check_number,DATE_FORMAT(C.payment_date,'%m-%d-%Y'), ")
                .append("CASE when A.is_royalty_Free=1 then 'true' else 'false' END,C2A.contract_id,")
                .append(" CASE ")
                .append(" WHEN PS.permission_status like'granted' THEN 'None' ")
                .append(" ELSE PS.status_explanation ")
                .append(" END,S.source_group,C.date, ")
                .append("C2A.seats, C2A.grant_type , ")//Added to implement DM-122
                .append("A.id ") // Added for DM-284
                .append("from asset_use AU join asset A on AU.asset_id = A.id ")
                .append("left outer join asset_2_source A2S on AU.asset_id = A2S.asset_id ")
                .append("left outer join source S on A2S.source_id = S.id ")
                .append("left outer join contract_2_asset C2A on AU.asset_id = C2A.asset_base_id ")
                .append("left outer join contract C on C2A.contract_id = C.id ")
                .append("left outer join component co on co.id = AU.component_id ")
                .append("left outer join au_source_perm_status PS on PS.asset_use_id = AU.id ")
                // Start : Query updated for DM-1606
                .append("left outer join purchase_order_2_asset po2a on po2a.asset_base_id = au.asset_id ")
                .append("left outer join PURCHASE_ORDER po on po.id = po2a.purchase_order_id ")
                // End : Query updated for DM-1606
                .append("where AU.cw_id = ?  and (C.cw_id = ")
                .append(cwId)
                .append(" or C.cw_id is null) ")
                .append(userGroupSql)
                .append(" and PS.permission_status is NOT NULL ")
                /* Modified Query for INC_96567 */
                .append(" UNION ALL (")
                .append("select co.name,AU.usage_type,AU.position,A.description, ")
                // Start : Query updated for DM-1606
				.append("case when PS.permission_status like 'formSent' and po.is_permission_Request=false then 'Waiting on Invoice' else PS.permission_status END as  permission_status,")
				.append("A.credit_line,S.display_name,")
				// End : Query updated for DM-1606
                .append("A.vendor_id,C.number,DATE_FORMAT(C.date,'%m-%d-%Y'),coalesce(C2A.price,0),C.check_number,DATE_FORMAT(C.payment_date,'%m-%d-%Y'),")
                .append("CASE when A.is_royalty_Free=1 then 'true' else 'false' END,C2A.contract_id,")
                .append(" CASE ")
                .append(" WHEN PS.permission_status like'granted' THEN 'None' ")
                .append(" ELSE PS.status_explanation ")
                .append(" END,S.source_group,C.date, ")
                .append("C2A.seats, C2A.grant_type, ")//Added to implement DM-122
                .append("A.id ") // Added for DM-284
                .append("from asset_use AU join asset A on AU.asset_id = A.id ")
                .append("left outer join asset_2_source A2S on AU.asset_id = A2S.asset_id ")
                .append("left outer join source S on A2S.source_id = S.id ")
                .append("left outer join contract_2_asset C2A on AU.asset_id = C2A.asset_base_id ")
                .append("left outer join contract C on C2A.contract_id = C.id ")
                .append("left outer join component co on co.id = AU.component_id ")
                .append("left outer join au_source_perm_status PS on PS.asset_use_id = AU.id ")
                // Start : Query updated for DM-1606
                .append("left outer join purchase_order_2_asset po2a on po2a.asset_base_id = au.asset_id ")
                .append("left outer join PURCHASE_ORDER po on po.id = po2a.purchase_order_id ")
                // Start : Query updated for DM-1606
                .append("where AU.cw_id = ? ")
                .append(" and PS.latest_contract_id=C.id and C.cw_id != AU.cw_id")
                .append(" and PS.permission_status is NOT NULL)")
                /*end Modification*/
                .append(" order by name")
                .toString();
		log.debug("cwid value= "+cwId);
		
		/*if (!userGroupId.equals(new Integer(0))) {
		sql = sql +  " and AU.user_group_id = " + userGroupId + " ;";
		}*/

		Query q = entityManager.createNativeQuery(sql);
		q.setParameter(1, cwId);
		q.setParameter(2, cwId);
		// q.setParameter(2, userGroupId);

		@SuppressWarnings("unchecked")
		List<Object[]> results = q.getResultList();

		return results;
	}
	//Ends

				//
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public String loadComplianceReportConditionData(Object contractId,Object sourceGroupId,String invoiceDate) throws Exception
	{
		 log.debug("loadComplianceReportData(): entered...");
		 log.debug("contractId -- "+contractId);
		 log.debug("sourceGroupId -- "+sourceGroupId);
		 log.debug("invoiceDate -- "+invoiceDate);
		 log.debug(invoiceDate);

		 String ma_sql="select distinct mad.* from master_agreement_deal mad " +
		               "where mad.source_group_id = ? and mad.start_date <= ? and (mad.end_date is null or mad.end_date >= ?)";
		 			Query query = entityManager.createNativeQuery(ma_sql, MasterAgreementDeal.class);

					 query.setParameter(1, sourceGroupId);
					 query.setParameter(2, invoiceDate);
					 query.setParameter(3, invoiceDate+ " 60:60:99");

   		 @SuppressWarnings("unchecked")
		 List<MasterAgreementDeal> list = query.getResultList();
		 log.debug("loadMasterAgreements(): resultList.size() = " + list.size());

		 //By above code we can get the Master Agreement Deal then we need to see if we have Master Agreement Deal or not

		 if(list.size()>0){
		 		log.info("enetered"+invoiceDate);
		 		log.debug("loadMasterAgreements(): resultList.size() = " + list.get(0).getFileName());
		 		log.debug("loadMasterAgreements(): resultList.size() = " + list.get(0).getId());

		 String sql = "select GROUP_CONCAT(vc.rollup_value SEPARATOR ', ') as exec_value " +
		              "  from view_ma_deal_condition vc,ma_deal_2_condition mc   where vc.id=mc.condition_id  "+
		              "and condition_type in ('sales','language','medium','print_run','dwork','print_run_ebook') "+
		              "and mc.ma_deal_id = ? ";

						Query q = entityManager.createNativeQuery(sql);
						q.setParameter(1, list.get(0).getId());
						@SuppressWarnings("unchecked")
						List<String> conditionList = q.getResultList();
						log.debug("SOURCE LEVEL CONDITIONS LIST= "+conditionList.toString());
						return conditionList.toString();
		 }else{
				log.debug("contractId ="+contractId);
		  String sql = "select GROUP_CONCAT(cv.rollup_value SEPARATOR ', ') as exec_value " +
				       "  from condition_value cv,contract_2_condition cc  where cv.id = cc.condition_id  "+
				       "and condition_type in ('sales','language','medium','print_run','dwork') "+
				       "and cc.contract_id = ? ";

						 Query q = entityManager.createNativeQuery(sql);
						 String columns_int = (contractId.toString());
								q.setParameter(1, contractId);
						 @SuppressWarnings("unchecked")
						 List<String> results = q.getResultList();
						 log.debug("ASSET LEVEL CONDITIONS LIST= "+results.toString());
						 log.debug("loadComplianceReportData(): exit..."+results.toString());
						 return results.toString();
			}
	}
				//Ends

	// smarkoff: Note the product index also has this data - could use it instead
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<Object[]> loadEditorUsageDetail(String copyrightYearFrom, String copyrightYearTo, String editorCode, String businessUnit,
												String productLine, String crdFrom, String crdTo, String workedFrom, String workedTo
												) throws Exception
	{
		log.debug("loadEditorUsageDetail(): entered...");

		String editorSelect = " and a.editor = '" + editorCode + "'";
		if (StringUtils.isBlank(editorCode))  {
			editorSelect = "";
		}

		String copyrightYearFromSelect = " and a.copyright_year >= '" + copyrightYearFrom + "'";
		if (StringUtils.isBlank(copyrightYearFrom))  {
			copyrightYearFromSelect = "";
		}

		String copyrightYearToSelect = " and a.copyright_year <= '" + copyrightYearTo + "'";
		if (StringUtils.isBlank(copyrightYearTo))  {
			copyrightYearToSelect = "";
		}

		String productLineSelect = " and a.product_line_id = '" + productLine + "'";
		if (StringUtils.isBlank(productLine))  {
			productLineSelect = "";
		}

		String businessUnitSelect = " and a.business_unit = '" + businessUnit + "'";
		if (StringUtils.isBlank(businessUnit))  {
			businessUnitSelect = "";
		}

		String crdFromSelect = " and a.consolidated_release_date >= '" + crdFrom + "'";
		if (StringUtils.isBlank(crdFrom)) {
			crdFromSelect = "";
		}

		String crdToSelect = " and a.consolidated_release_date <= '" + crdTo + " 23:59:59.0'";
		if (StringUtils.isBlank(crdTo)) {
			crdToSelect = "";
		}

		String workedFromSelect = " and (p.last_updated_date  >= '" + workedFrom + "' ) ";
		// String workedFromSelect = " and a.cw_id in (select cw_id from cw_history where last_updated_date >= '" + workedFrom + "' or created_date  >= '" + workedFrom + "')";
		if (StringUtils.isBlank(workedFrom)) {
			workedFromSelect = "";
		}

		String workedToSelect = " and (p.last_updated_date  <= '" + workedTo + " 23:59:59.0' ) ";
		if (StringUtils.isBlank(workedTo)) {
			workedToSelect = "";
		}

		// James says to use ComponentCategory to determine 'Cover' instead of usage_type
		String sql = "select a.business_unit, b.name, f.code, f.name productLineName, " +
			"a.editor, a.cw_id, a.id, a.isbn10, a.isbn13, a.title, a.copyright_year, consolidated_release_date CRD, " +
			"a.photo_illus_total_count totalFromGBPM " +
			",(select count(*) from asset_use b where b.cw_id = a.cw_id) totalAssets " +
			",(select count(*) from asset_use b, component comp where b.cw_id = a.cw_id and b.component_id = comp.id and comp.category = 'CVW') coverAssets " +
			",(select count(*) from asset_use b left join component comp on comp.id = b.component_id where b.cw_id = a.cw_id and (comp.category is null or comp.category != 'CVW')) notCoverAssets " +
			",(select count(*) from asset_use b where b.cw_id = a.cw_id and b.permission_status in (" + PermissionStatus.getSQLClause(PermissionStatus.CANCELED_GROUP) + "," + PermissionStatus.getSQLClause(PermissionStatus.GRANTED_GROUP) + ")  statusOK " +
			",(select count(*) from asset_use b where b.cw_id = a.cw_id and ( b.permission_status in (" + PermissionStatus.getSQLClause(PermissionStatus.PROBLEM_GROUP) + ") or b.permission_status is null)) statusNotOK  " +
			",(select count(*) from asset_use c where c.cw_id = a.cw_id and c.permission_status in ('" + PermissionStatus.NO_SOURCE.getCode() + "'," + PermissionStatus.getSQLClause(PermissionStatus.UNREQUESTED_GROUP) + ")) statusNotRequested " +
			",(select count(*) from asset_use c where c.cw_id = a.cw_id and c.permission_status in (" + PermissionStatus.getSQLClause(PermissionStatus.IN_PROGRESS_GROUP) + ")) statusInProgress " +
			" , p.last_updated_date " +
			" from product a " +
			"     left outer join business_unit b on a.business_unit = b.code " +
			" left outer join product_line f on f.id = a.product_line_id " +
			"  left outer join cw_history p on p.cw_id = a.cw_id   and p.id in (select max(id) from cw_history where cw_id = a.cw_id) " +
			" where a.cw_id is not null and a.is_cw_primary = 1 " +
			editorSelect + copyrightYearFromSelect + copyrightYearToSelect + productLineSelect + businessUnitSelect +
			crdFromSelect + crdToSelect + workedFromSelect + workedToSelect +
			" order by b.name, f.name, a.editor, a.copyright_year, a.title";

		Query q = entityManager.createNativeQuery(sql);

		@SuppressWarnings("unchecked")
		List<Object[]> results = q.getResultList();

		return results;
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<Object[]> loadPhotoEditorDetail(Integer id) throws Exception
	{
		Query q = entityManager.createNativeQuery(
				" select a.id, a.CW_ID, title, isbn13, copyright_year, h.code, e.research_progress_notes,  " +
				" e.permissions_due, e.page_proof_schedule_date  " +
				" from product a left outer join user_2_role c on a.id = c.product_id " +
				" left outer join user_table d on d.id = c.user_id  " +
				" left outer join cw_summary e on e.cw_id = a.CW_ID  " +
				" left outer join common_work f on f.id = a.cw_id  " +
                " left outer join role g on c.role_id = g.id and g.code = 'PHE' and g.role_type = 'EMPLOYEE' " +
                " left outer join product_line h on a.product_line_id = h.id and a.data_source = h.data_source " +
 				" where a.is_cw_primary = 1 and d.id is not null and f.interior_cw_status != 'complete' " +
 				" and a.cw_id in (select distinct cw_id from asset_use where (import_source is null or import_source <> 1)) " +
				" and d.id = ? order by title");

		q.setParameter(1, id);

		@SuppressWarnings("unchecked")
		List<Object[]> results = q.getResultList();

		return results;
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<Object[]> loadPhotoEditors() throws Exception
	{
		Query q = entityManager.createNativeQuery(
				"select distinct a.user_id, b.first_name || ' ' || b.last_name from user_2_role a, user_table b, role c " +
				" where a.user_id = b.id and a.role_id = c.id and c.code = 'PHE' and c.role_type = 'EMPLOYEE' " );
		@SuppressWarnings("unchecked")
		List<Object[]> results = q.getResultList();

		return results;
	}


	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<Object[]> loadEditorsByRole(String role) throws Exception
	{
		Query q = entityManager.createNativeQuery(
				"select distinct a.user_id, b.first_name || ' ' || b.last_name from user_2_role a, user_table b, role c " +
				" where a.user_id = b.id and a.role_id = c.id and c.code in (" + role + ") and c.role_type = 'EMPLOYEE' order by b.last_name ");
		@SuppressWarnings("unchecked")
		List<Object[]> results = q.getResultList();

		return results;
	}

	/**
	 * Compute the current total cost of all associated contracts (contracts that have this product as it's
	 * primary product).
	 * @param cwId
	 * @return double
	 */
	public double getCurrentActualCost(int cwId) {
		//Made change to query for INC_53852
		Query query = entityManager.createNativeQuery(
				"select sum(c2a.price) as double_value from contract_2_asset c2a,contract c where c.id = c2a.contract_id and c.cw_id = ?",
				"scalarDouble");
		query.setParameter(1, cwId);
		Number resultNum = (Number) query.getSingleResult();
		return (resultNum == null) ? 0 : resultNum.doubleValue();
	}

	// standard getter for common work bean.
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public CommonWork loadCWById(Integer cwId) throws PersistenceException {
		return find(CommonWork.class, cwId);
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public CommonWork loadWithPrimaryProductById(Integer cwId) throws PersistenceException
	{
		CommonWork cw = find(CommonWork.class, cwId);
		// check for null only to throw more descriptive exception instead of just NullPointerException
		// The cw can be null if for example the user goes to the landing page and then edits the URL
		// to have a non-existent cw id on the end.
		if (cw == null) {
			throw new RuntimeException("cwId " + cwId + " does not exist in DB");
		}
		cw.getPrimaryProduct();

		return cw;
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public CommonWork loadWithExtendedPrimaryProductById(Integer cwId) throws PersistenceException
	{
		CommonWork cw = loadWithPrimaryProductById(cwId);
		Product p = cw.getPrimaryProduct();
		productRepository.loadExtendedProduct(p);
		return cw;
	}

	/**
	 * This method is necessary for classes that need to update anything in the common work.
	 * I found that if I use readOnly on the CommonWorkConditionsController.  The controller would
	 * update the common work in memory but would not persist it.  It would not throw any exceptions either.
	 * also the propagation did not get persisted either.  Adding CW conditions to a CW that was read only
	 * would not persist the CWConditions either.
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public CommonWork loadForUpdateById(Integer cwId) throws PersistenceException {
		return find(CommonWork.class, cwId);
	}

	/**
	 *
	 * @param cwId  Must be non-null
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public CommonWork loadByIdForManageAsset(Integer cwId) throws PersistenceException {
		ArgUtil.notNull(cwId, "cwId");
		CommonWork cw = find(CommonWork.class, cwId);

		// preload stuff needed by Controller and jsp
		for (Condition cond : cw.getConditions()) {
			cond.getValue();
		}

		for (Product p : cw.getProducts()) {
			p.getTitle();
		}

		Product primary = cw.getPrimaryProduct();
		if (primary != null) {
			primary.getIsbn10();
			if (primary.getPublicationStatus() != null) {
				primary.getPublicationStatus().getCode();
			}
			if (primary.getLocation() != null) {
				primary.getLocation().getCode();
			}
		}

		return cw;
	}

	public void setInteriorCWStatus(CommonWork cw, CommonWorkStatus cwStatus) {
		cw.setInteriorCWStatus(cwStatus);
		entityManager.merge(cw);
	}

	/**
     * Returns a count of assets given a specific status for a given common work
     * if status is null then it selects a count of all asset-uses
     */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public Integer getAssetUseCountByStatus(Integer cwId, PermissionStatus status) {
		Query query = null;

		if (null == status) {
			String sql = "select count(au.id) as count from asset_use au where au.cw_id = ? ";
			query = entityManager.createNativeQuery(sql, "scalarCount");
		} else {
			String sql = "select count(au.id) as count from asset_use au " +
			" where au.cw_id = ? and au.permission_status = '" + status.getCode() + "' ";
			query = entityManager.createNativeQuery(sql, "scalarCount");
		}
		query.setParameter(1, cwId);
		// For count, DB2 and MS SQL Server return Integer but MySQL returns BigInteger
		// Integer and BigInteger inherit from Number
		Number resultNum = (Number) query.getSingleResult();
		int result = resultNum.intValue();

		return result;
	}

	/**
     * Returns a list of total asset count and counts for each status.
     */
	@Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
	public List <Object[]> getAssetUseStatusCountByStatus(Integer cwId, boolean includeCovers) {
		PerfTimer timer = monitor.startTimer("CommonWorkRepository::getAssetUseStatusCountByStatus");

		final String originalSql = "select 'Number of Assets', count(distinct (asset_id)) from asset_use where cw_id = ? " +
			"union " +
			"select ps.description, count(distinct (asset_id)) from asset_use au, permission_status ps where au.cw_id = ? and " +
			"au.permission_status = ps.code group by ps.description ";

		final String excludeCoversSql = "select 'Number of Assets', " +
			"count(distinct (asset_id)) from asset_use au left join component c on au.component_id = c.id and c.category != '" + ComponentCategory.COVER.getCode() + "' " +
			"where au.cw_id = ? "  +
			"union " +
			"select ps.description, count(distinct (asset_id)) from asset_use au " +
			"join permission_status ps on au.permission_status = ps.code " +
			"left join component c on au.component_id = c.id and c.category != '" + ComponentCategory.COVER.getCode() + "' " +
			"where au.cw_id = ? " +
			"group by ps.description";

		final String sql = includeCovers ? originalSql : excludeCoversSql;
		//log.debug("getAssetUseStatusCountByStatus(): includeCovers = " + includeCovers + ",\nsql: " + sql);
		Query query = entityManager.createNativeQuery(sql);
		query.setParameter(1, cwId);
		query.setParameter(2, cwId);
		@SuppressWarnings("unchecked")
		List<Object[]> results = query.getResultList();

		timer.stopTimer();

		if (results.size() < 1) return null;
		return results;
	}

	/* returns an array of objects containing cw_id and Edition_name
	 *  given a supplied common work id
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List <Object[]> getAllEditionsByCwId(Integer cwId) throws Exception
	{
		CommonWork firstCw = this.loadCWById(cwId);

		// no first edition cannot calculate the rest
		if (null == firstCw.getPrimaryProduct().getEdition()) return null;

		ProductEdition firstEd = firstCw.getPrimaryProduct().getEdition();
		String dTitle = firstEd.getName().substring(0, firstEd.getName().indexOf(" EDITION "));

		Query query = null;
		// TODO - ask Napoleon if edition should be tested against product_edition.id
		query = entityManager.createNativeQuery(
				"select a.cw_id, b.name || ' - ' || a.short_title from product a, product_edition b  " +
				"where  a.edition in ( " +
				"select id from product_edition where name like '" +
				dTitle + "%')  and a.edition = b.id order by a.edition, b.name " );

		@SuppressWarnings("unchecked")
		List<Object[]> results = query.getResultList();
		if (results.size() < 1) return null;

		return results;
	}

	/**
	 * Returns an array of commonWorks given a common work id.
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<CommonWork> getAllCwsInEdition(Integer cwId) throws Exception
	{
		List<CommonWork> cws = new ArrayList<CommonWork>();

		CommonWork firstCw = this.loadCWById(cwId);

		// no first edition cannot calculate the rest
		if (null == firstCw.getPrimaryProduct().getEdition()) return null;

		ProductEdition firstEd = firstCw.getPrimaryProduct().getEdition();
		String dTitle = firstEd.getName().substring(0, firstEd.getName().indexOf(" EDITION "));
		String sql = "select id from comon work where id in ( " +
			"(select cw_id from product where edition in ( " +
			"select id from product_edition where name like '" + dTitle + "%'))";
		Query query = entityManager.createNativeQuery(sql);

		@SuppressWarnings("unchecked")
		List<Object[]> results = query.getResultList();

		if (results.size() < 1 ) return null;

		for (int x = 0; x < results.size();x ++) {
			Integer ncwId = (Integer) results.get(x)[0];
			cws.add(this.loadCWById(ncwId));
		}

		return cws;
	}

	/**
	 * @param cwId
	 * @param limit 0 means no limit
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<CwHistory> getHistory(int cwId, int limit) throws PersistenceException
	{
		PerfTimer timer = monitor.startTimer("CommonWorkRepository::getHistory");
		TypedQuery<CwHistory> query = entityManager.createQuery(
				"from CwHistory h where h.cwId = ?1 order by lastUpdatedDate desc",
			CwHistory.class);
		query.setParameter(1, cwId);
		List<CwHistory> list = query.getResultList();

		if (limit > 0 && list.size() > limit) {
			list = list.subList(0, limit);
		}

		// preload User full names and email
		for (CwHistory history : list) {
			history.getLastUpdatedUser().getFullName();
			history.getLastUpdatedUser().getEmail();
		}

		timer.stopTimer();

		return list;
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<User> getPEAuthorsForCommonWork(int cwId) throws Exception
	{
		// Instead of only returning Users that have RoleType of Author AND Role of Author
		// we return all Users associated by any RoleType of Author (often some
		// users are associated by role type AUTHOR and role of Editor or Contributor).
		Role role = Role.AUTHOR;
		final String sql = "select distinct(u.id), last_name, first_name from user_2_role map, user_table u, role r, product p"
			+ " where map.user_id = u.id and map.role_id = r.id and map.product_id = p.id and p.cw_id = ?"
			+ " and r.role_type = ?";
			//+ " and r.code = ?";
		Query q = entityManager.createNativeQuery(sql);
		q.setParameter(1, cwId);
		q.setParameter(2, role.getRoleType().name());  // doesn't work without .name()
		//q.setParameter(3, role.getCode());
		@SuppressWarnings("unchecked")
		List<Object[]> results = q.getResultList();
		List<User> userList = new ArrayList<User>();

		for (Object[] columns : results) {
			User u = new User();
			u.setId((Integer) columns[0]);
			u.setLastName((String) columns[1]);
			u.setFirstName((String) columns[2]);
			userList.add(u);
		}

		return userList;
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<User> getCreatedUsersForCWAssetUses(int cwId) {
		final String sql = "select distinct(u.id), u.first_name, u.last_name"
			+ " from user_table u, asset_use au where au.cw_id = ? and au.created_user_id = u.id";
		Query q = entityManager.createNativeQuery(sql);
		q.setParameter(1, cwId);
		@SuppressWarnings("unchecked")
		List<Object[]> results = q.getResultList();
		List<User> userList = new ArrayList<User>();

		for (Object[] columns : results) {
			User u = new User();
			u.setId((Integer) columns[0]);
			u.setLastName((String) columns[1]);
			u.setFirstName((String) columns[2]);
			userList.add(u);
		}

		return userList;
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<UserGroup> getCreatedUserGroupsForCWAssetUses(int cwId, Boolean adminUser) {
		String sql = "select distinct(g.id), g.name, g.description"
			+ " from user_table u, asset_use au, user_group g where au.cw_id = ?";


		// admin users see all user groups
		if(adminUser) {
			sql = sql + " and au.created_user_id = u.id";
		} else {
			sql = sql + " and au.created_user_id = u.id and g.id = u.user_group_id";
		}
		Query q = entityManager.createNativeQuery(sql);
		q.setParameter(1, cwId);
		@SuppressWarnings("unchecked")
		List<Object[]> results = q.getResultList();
		List<UserGroup> groupList = new ArrayList<UserGroup>();

		for (Object[] columns : results) {
			UserGroup g = new UserGroup();
			g.setId((Integer) columns[0]);
			g.setName((String) columns[1]);
			g.setDescription((String) columns[2]);
			groupList.add(g);
		}

		return groupList;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void saveGrantYears(int cwId, int grantYears) {
		final String sql = "update common_work set min_grant_years = ? where id = ?";
		Query q = createNativeQuery(sql);
		q.setParameter(1, grantYears);
		q.setParameter(2, cwId);
		int numRows = q.executeUpdate();  // we expect numRows to always be 1
		log.debug("saveGrantYears(): updated " + numRows + " rows.");
	}

	public void fixCommonWorks() {
		try {
			FixCommonWorks fix = new FixCommonWorks(dataSource.getConnection(), false);
			fix.go();
		}
		catch (Exception ex) {
			log.error("caught exception trying to run FixCommonWorks: ", ex);
		}
	}

	// smarkoff: Originally had this as @Formula on CommonWork but seemed to be eagerly loaded
	// at times (even though specified as LAZY) and so moved here since a somewhat complicated query
	public int totalPrintings(int cwId) {
		PerfTimer timer = monitor.startTimer("CommonWorkRepository::totalPrintings");

		// This is slow so use query below instead that avoid the IN clause
		//final String sql = "select sum(pp.order_quantity) as count from product_printing pp, product p where p.id = pp.product_id and " +
		//	"(p.cw_id = ? OR p.external_id in" +
		//	"(select r.related_wid from product p, relation r where r.product_id = p.id and r.code = 'WC' and p.cw_id = ?))";
		final String sql = "select sum(a.order_quantity) as count from (" +
				"select pp.order_quantity from product_printing pp join product p on p.id = pp.product_id and p.cw_id = ?" +
				" union all" +
				" select pp.order_quantity from product_printing pp" +
				" join product p on p.id = pp.product_id" +
				" join product p2 on p2.cw_id = ?" +
				" join relation r on p2.id = r.product_id and r.code = 'WC'" +
				" where p.external_id = r.related_wid" +
				") a";
		Query query = entityManager.createNativeQuery(sql, "scalarCount");
		query.setParameter(1, cwId);
		query.setParameter(2, cwId);
		// For sum, DB2 and MS SQL Server return Integer but MySQL returns BigDecimal
		// Integer and BigDecimal inherit from Number
		Number resultNum = (Number) query.getSingleResult();
		// the result is null for some products
		int result = resultNum == null ? 0 : resultNum.intValue();

		// slow version not using
		// select sum(ebook_sales) as count from product where cw_id = ? OR external_id in
		// (select r.related_wid from product p, relation r where r.product_id = p.id and r.code = 'WC' and p.cw_id = ?)
		final String sql2 = "select sum(a.ebook_sales) as count from (" +
			"select ebook_sales from product where cw_id = ?" +
			" union all" +
			" select p.ebook_sales from product p" +
			" join product p2 on p2.cw_id = ?" +
			" join relation r on p2.id = r.product_id and r.code = 'WC'" +
			" where p.external_id = r.related_wid" +
			") a";
		query = entityManager.createNativeQuery(sql2, "scalarCount");
		query.setParameter(1, cwId);
		query.setParameter(2, cwId);
		// For sum, DB2 and MS SQL Server return Integer but MySQL returns BigDecimal
		// Integer and BigDecimal inherit from Number
		resultNum = (Number) query.getSingleResult();
		// the result is null for some common works
		int result2 = resultNum == null ? 0 : resultNum.intValue();

		timer.stopTimer();

		log.debug("totalPrintings(): counts for cwId [" + cwId + "] are " + result + " + " + result2);

		return result + result2;
	}


	public AssetUseRepository getAssetUseRepository() {
		return assetUseRepository;
	}

	public void setAssetUseRepository(AssetUseRepository service) {
		this.assetUseRepository = service;
	}

	public DataSource getDataSource() {
		return dataSource;
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;

		// smarkoff:
		// log debug info (can be removed but might as well leave it for now)
		// I was trying to figure out if we can get to information about how
		// many connections are currently borrowed from the pool - I don't
		// think this is the way to do it because we can't seem to get to
		// the DBCP BasicDataSource class from here, which has getNumActive().
		String className = dataSource.getClass().getCanonicalName();
		log.debug("setDataSource(): dataSource className = " + className);
		if (!className.equals("com.atomikos.jdbc.AtomikosDataSourceBean")) return;

		AtomikosDataSourceBean bean = (AtomikosDataSourceBean) dataSource;
		log.debug("setDataSource(): Atomikos maxPoolSize = " + bean.getMaxPoolSize());
		log.debug("setDataSource(): Atomikos minPoolSize = " + bean.getMinPoolSize());
		log.debug("setDataSource(): Atomikos XaDataSourceClassName = " + bean.getXaDataSourceClassName());
		// XaDataSourceClassName ends up being com.mysql.jdbc.jdbc2.optional.MysqlXADataSource,
		// not org.apache.commons.dbcp.BasicDataSource like I was hoping
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public String loadCommonWorkPermissionInfo (CommonWork cw) {
		// "scalarGroup" is defined in an annotation at the top of the AssetUse class.
		Query query = entityManager.createNativeQuery(
				"select group_concat(condt.description || ':' || cond.rollup_value) as cgroup " +
				"from condition_value cond join condition_type condt on condt.code = cond.condition_type " +
				"where rollup_value is not null and cond.id in " +
				"(select condition_id from cw_2_condition where cw_id = ?)",
			"scalarGroup");
		query.setParameter(1, cw.getId());
		String groupValue = (String) query.getSingleResult();
		groupValue += ",Length of Grant:" +
			(0 == cw.getMinGrantYears()? "life of the edition" : cw.getMinGrantYears() + " years");
		groupValue += ",no sublicense";
		return groupValue;
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public String loadCommonWorkPermissionInfoNoHeaders (CommonWork cw) {
		// "scalarGroup" is defined in an annotation at the top of the AssetUse class.
		Query query = entityManager.createNativeQuery(
				"select group_concat(cond.rollup_value) as cgroup " +
				"from condition_value cond join condition_type condt on condt.code = cond.condition_type " +
				"where rollup_value is not null and cond.id in " +
				"(select condition_id from cw_2_condition where cw_id = ?)",
			"scalarGroup");
		query.setParameter(1, cw.getId());
		String groupValue = (String) query.getSingleResult();
		groupValue += "," +
			(0 == cw.getMinGrantYears()? "life of the edition" : cw.getMinGrantYears() + " years");
		groupValue += ",no sublicense";
		return groupValue;
	}

	//Start: Added for DM-534
		@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
		public ExportAsset loadexportStatus(int cwId, int userId) throws PersistenceException
		{
			PerfTimer timer = monitor.startTimer("CommonWorkRepository::loadexportStatus");
			ExportAsset ea =null;
			try {
			TypedQuery<ExportAsset> query = entityManager.createQuery("select es from ExportAsset es where es.cwId=?1 and es.userId=?2", ExportAsset.class);

			query.setParameter(1, cwId);
			query.setParameter(2, userId);
			ea = query.getSingleResult();
			}
			catch(NoResultException nre) {

			}
			timer.stopTimer();
			return ea;

		}

		@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
		public ExportAsset loadUserexportStatus(int cwId, String status) throws PersistenceException
		{
			PerfTimer timer = monitor.startTimer("CommonWorkRepository::loadexportStatus");
			ExportAsset ea =null;
			try {
			TypedQuery<ExportAsset> query = entityManager.createQuery("select es from ExportAsset es where es.cwId=?1 and es.exportStatus=?2", ExportAsset.class);

			query.setParameter(1, cwId);
			query.setParameter(2, status);
			ea = query.getSingleResult();
			}
			catch(NoResultException nre) {

			}
			timer.stopTimer();
			return ea;

		}

		@Transactional(propagation = Propagation.REQUIRED)
		public void updateExportAsset(String status,int cwId, int userid,int total) {
			final String sql = "update export_asset set export_status = ?,total_count=? where cw_id = ? and user_id=?";
			Query q = createNativeQuery(sql);
			q.setParameter(1, status);
			q.setParameter(2, total);
			q.setParameter(3, cwId);
			q.setParameter(4, userid);
			int numRows = q.executeUpdate();  // we expect numRows to always be 1
			log.debug("updateExportAsset(): updated " + numRows + " rows.");
		}

		@Transactional(propagation = Propagation.REQUIRED)
		public void saveExportAsset(String status,int cwId, int userid,int total) {
			final String insertSql = "insert into export_asset (cw_id,export_date,user_id,export_status,total_count) values (?, ?, ?,?,?)";
			Query q = createNativeQuery(insertSql);
			int insertCount = 0;
				q.setParameter(1, cwId);
				q.setParameter(2, new Date());
				q.setParameter(3, userid);
				q.setParameter(4, status);
				q.setParameter(5, total);
				insertCount += q.executeUpdate();

			log.debug("saveExportAsset(): inserted " + insertCount + " rows.");
		}

		@Transactional(propagation = Propagation.REQUIRED)
		public void updateExportAssetCount(int count,int cwId,int userid) {
			final String sql = "update export_asset set completed = ? where cw_id = ? and user_id=?";
			Query q = createNativeQuery(sql);
			q.setParameter(1, count);
			q.setParameter(2, cwId);
			q.setParameter(3, userid);
			int numRows = q.executeUpdate();  // we expect numRows to always be 1
			log.debug("updateExportAssetCount(): updated " + numRows + " rows.");
		}

		@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
		public ExportAsset loadexportCount(Integer cwId) throws PersistenceException
		{
			PerfTimer timer = monitor.startTimer("CommonWorkRepository::loadexportStatus");
			ExportAsset ea =null;
			try {
			TypedQuery<ExportAsset> query = entityManager.createQuery("from ExportAsset es where es.cwId=?1 order by exportDate desc", ExportAsset.class);

			query.setParameter(1, cwId);
			List<ExportAsset> eassets = query.getResultList();
			if(eassets.size()==0)
				{
				return ea;
				}
			else
				{
				ea=eassets.get(0);
				}
			}
			catch(NoResultException nre) {
				return ea;
			}
			timer.stopTimer();
			return ea;

		}
		//End: Added for DM-534


	public ProductRepository getProductRepository() {
		return productRepository;
	}

	public void setProductRepository(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}

}
