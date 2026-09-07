package com.wiley.permissions.repositories;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.sql.DataSource;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.wiley.permissions.domain.persistence.permissions.Asset;
import com.wiley.permissions.domain.persistence.permissions.AssetUse;
import com.wiley.permissions.domain.persistence.permissions.CommonWork;
import com.wiley.permissions.domain.persistence.permissions.ComponentCategory;
import com.wiley.permissions.domain.persistence.permissions.PermissionStatus;
import com.wiley.permissions.domain.persistence.permissions.PurchaseOrder;
import com.wiley.permissions.domain.persistence.permissions.PurchaseOrderList;
import com.wiley.permissions.persistence.PersistenceException;
import com.wiley.permissions.persistence.repository.JPARepository;
import com.wiley.permissions.services.view.POAssetView;
import com.wiley.sf.common.lang.StringUtil;
import com.wiley.sf.common.monitor.PerformanceMonitor.PerfTimer;

/**
 * @version $Id: PurchaseOrderRepository.java,v 1.95 2014-07-16 06:44:33 rbongoni Exp $
 */
public class PurchaseOrderRepository extends JPARepository
{
	private static final Log log = LogFactory.getLog(PurchaseOrderRepository.class);

	private AssetUseRepository assetUseRepository;

	private DataSource dataSource;

	@PersistenceContext(unitName = "permissions")
	protected EntityManager entityManager;

	@Override
	protected EntityManager getEntityManager() {
		return entityManager;
	}

	/**
	 * Return the newest PO that applies to the given asset, source, and cw.
	 * Wraps the SINGLE PO in a list to make easier to use by the rule engine.
	 * An empty list is returned if there is no PO (so return list size is 0 or 1).
	 * Return PurchaseOrderList instead of List<PurchaseOrder> because Rules engine doesn't
	 * yet support matching based on generics.
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public PurchaseOrderList loadLatestForAssetSourceCW(int assetId, int sourceId, int cwId) throws PersistenceException
	{
		PerfTimer timer = monitor.startTimer("PurchaseOrderService::loadListForAssetSourceCW");

		String queryString = "select distinct po from PurchaseOrder po, in (po.assets) a where a.id = ?"
			+ " and po.commonWork.id = ? and po.source.id = ?"
			+ " order by po.date desc, po.id desc";
			// a lot of the PO dates are truncated to just day (not hours, etc)
			// so sort by "id desc" also
		TypedQuery<PurchaseOrder> query = entityManager.createQuery(queryString, PurchaseOrder.class);
		query.setParameter(1, assetId);
		query.setParameter(2, cwId);
		query.setParameter(3, sourceId);
		query.setMaxResults(1);
		List<PurchaseOrder> list = query.getResultList();

		PurchaseOrderList poList = new PurchaseOrderList();
		poList.addAll(list);

		timer.stopTimer();

		return poList;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public PurchaseOrder loadPurchaseOrderById(Integer purchaseOrderId)
    throws Exception
    {
		return find(PurchaseOrder.class, purchaseOrderId);
    }

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void removeAssetFromPO(Integer purchaseOrderId, Integer assetId)
	    throws Exception
	{
		PurchaseOrder purchaseOrder = loadPurchaseOrderById (purchaseOrderId);

		List<Asset> assets = purchaseOrder.getAssets();

		int index = -1;

		for (Asset asset: assets) {
			if (assetId.equals(asset.getId())) {
				index = assets.indexOf(asset);
			}
		}

		if (index == -1) {
			throw new PersistenceException("Asset Not Found In the Given Purchase Order");
		}

		assets.remove(index);

		purchaseOrder = entityManager.merge(purchaseOrder);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public ArrayList<Asset> addAssetsToPO(Integer purchaseOrderId, Integer [] assetIds)
	    throws Exception
	{
		log.debug("addAssetsToPurchaseOrder(): assetIds: " + StringUtil.arrayToString(assetIds, ", "));

		ArrayList<Asset> addedList = new ArrayList<Asset>();
		PurchaseOrder purchaseOrder = find(PurchaseOrder.class, purchaseOrderId);

		for (Integer assetId : assetIds) {
			Asset asset = find(Asset.class, assetId);
			purchaseOrder.getAssets().add(asset);
			addedList.add(asset);
		}

		// add the assets to purchase order
		entityManager.merge(purchaseOrder);

		return addedList;
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<POAssetView> loadPOAssetsView(int poId)
	{
		String sql = " select distinct a.id, a.description, a.media_type, GROUP_CONCAT(au.usage_type), GROUP_CONCAT(au.position), " +
				"po.id, po.date, GROUP_CONCAT(c.name), c.id " +
				" from  asset a join asset_use au on a.id = au.asset_id" +
				" left join component c on au.component_id = c.id" +
				" left join purchase_order_2_asset po_2_a on po_2_a.asset_base_id = a.id " +
				" left join purchase_order po on po_2_a.purchase_order_id = po.id" +
				" where po.id = ? group by a.id";

		Object[] args = { poId };

		JdbcTemplate template = new JdbcTemplate(dataSource);
		List<POAssetView> poViews = template.query(sql, args, new RowMapper<POAssetView>() {
			@Override
			public POAssetView mapRow(ResultSet rs, int rowNum) throws SQLException
			{
				POAssetView poView = new POAssetView ();
				poView.setAssetId (rs.getInt(1));
				poView.setAssetDescription (rs.getString(2));
				poView.setMediaTypeCode (rs.getString(3));
				poView.setUsageTypeCode (rs.getString(4));
				poView.setPosition (rs.getString(5));
				poView.setPoId (rs.getInt(6));
				poView.setPoDate (rs.getDate(7));
				poView.setComponentName (rs.getString (8));
				poView.setComponentId (rs.getInt (9));
				return poView;
			}
		});

		return poViews;
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<POAssetView> loadPOAssetsViewForSource(int sourceId, int cwId,
			PermissionStatus [] statuses, boolean includeCovers, boolean allChapters, int userId)
	{
		// lnagy - change to use the new asset_perm_ref table - easier to get data
		String sql = "select distinct a.id, a.description, a.media_type, GROUP_CONCAT(au.usage_type), GROUP_CONCAT(au.position), " +
				"po.id, po.date, GROUP_CONCAT(c.name), c.id, a.vendor_id, a.is_royalty_free, " +
				"  a.will_be_work_for_hire, a.will_be_royalty_free,a.credit_line  " +
				"from  asset_perm_ref apr join asset a on a.id = apr.asset_id join asset_use au on au.id = apr.asset_use_id " +
				" left join component c on au.component_id = c.id" +
				" left join purchase_order po on po.id = apr.po_id" +
				(allChapters ? "" : " left join author_2_component a2c on a2c.component_id = c.id") +
				" where apr.source_id = ? and apr.cw_id = ? and " +
				" apr.permission_status in (" + PermissionStatus.arrayToString(statuses) + ")" +
				(includeCovers ? "" : "and c.category != '" + ComponentCategory.COVER.getCode() + "'") +
				(allChapters ? "" : " and a2c.cw_id = ? and a2c.user_id = ?") +
				" group by a.id";
		Object[] args = { sourceId, cwId };
		if (!allChapters) {
			args = new Object [] { sourceId, cwId, cwId, userId };
		}
		String debugSql = sql.replaceFirst("\\?", "<" + sourceId + ">");
		debugSql = debugSql.replaceFirst("\\?", "<" + cwId + ">");
		log.debug("loadPOAssetsViewForSource(): debugSql: " + debugSql);

		/*String sql = "select distinct a.id, a.description, a.media_type, au.usage_type, au.position, po.id, po.date, c.name, c.id , a.vendor_id " +
		"from  asset_use au join asset a on a.id = au.asset_id" +
		" left join component c on au.component_id = c.id" +
		" join asset_2_source on asset_2_source.asset_id = a.id and asset_2_source.source_id = ?" +
		" join au_source_perm_status aups on aups.asset_use_id = au.id and aups.source_id = ?" +
		"	and (aups.permission_status = '" + Status.UNREQUESTED.getCode() + "' or aups.permission_status = '" + Status.PO_SENT.getCode() + "')" +
		" left join purchase_order_2_asset po_2_a on po_2_a.asset_base_id = a.id " +
		" left join purchase_order po on po_2_a.purchase_order_id = po.id" +
		" where au.cw_id = ? order by c.name";
		Object[] args = { sourceId, sourceId, cwId };
		 */

		JdbcTemplate template = new JdbcTemplate(dataSource);
		List<POAssetView> poViews = template.query(sql, args, new RowMapper<POAssetView>() {
			@Override
			public POAssetView mapRow(ResultSet rs, int rowNum) throws SQLException
			{
				POAssetView poView = new POAssetView();
				poView.setAssetId (rs.getInt(1));
				poView.setAssetDescription (rs.getString(2));
				poView.setMediaTypeCode (rs.getString(3));
				poView.setUsageTypeCode (rs.getString(4));
				poView.setPosition (rs.getString(5));
				poView.setPoId (rs.getInt(6));
				poView.setPoDate (rs.getDate(7));
				poView.setComponentName (rs.getString (8));
				poView.setComponentId (rs.getInt (9));
				poView.setVendorId(rs.getString(10));
				poView.setRoyaltyFree(rs.getBoolean(11));
				poView.setWillBeWorkForHire(rs.getBoolean(12));
				poView.setWillBeRoyaltyFree(rs.getBoolean(13));
				poView.setCreditLine(rs.getString(14));
				return poView;
			}
		});

		log.debug("loadPOAssetsViewForSource(): #views (assets) returning: " + poViews.size());
		return poViews;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public List<Asset> getPOUnusedAssets(int poId, int sourceId, int cwId)
	{
		// Return a list of Assets which are not included in the given
		// purchase order and has a source which is the same as the purchase
		// order.
		// Also only add an Asset to the list if it has unrequested status
		// (i.e. it is not part of any other purchase order
		// and it needs one [not auto-granted]).

		List<Asset> removedAssets = new ArrayList<Asset>();

		// TODO: Can this be converted to a JQL so that we get back a TypedQuery?
		// we need the AssetBase because of the way the Asset bean is defined (needs CREATED_DATE, etc)
		final String sql = "select a.*, ab.* from asset a join asset_base ab on a.id = ab.id join asset_use au on a.id = au.asset_id " +
			" join asset_2_source on asset_2_source.asset_id = a.id " +
			" and asset_2_source.source_id = ? join au_source_perm_status aups " +
			"    	on aups.asset_use_id = au.id and aups.source_id = ? and "
			+ "     aups.permission_status IN (" + PermissionStatus.getSQLClause (PermissionStatus.UNREQUESTED_GROUP) + ")" +
			"	where au.cw_id = ?" +
			" and a.id not in (select asset_base_id from purchase_order_2_asset where purchase_order_id = ?)";
		Query query = entityManager.createNativeQuery(sql, Asset.class);
		query.setParameter(1, sourceId);
		query.setParameter(2, sourceId);
		query.setParameter(3, cwId);
		query.setParameter(4, poId);
		@SuppressWarnings("unchecked")
		List<Asset> aList = query.getResultList();

		if (CollectionUtils.isEmpty(aList))
			return removedAssets;

		for (Asset asset : aList) {
			// removedAssets might already contain that asset
			// from a previous loop if the asset has two asset uses.
			if (!removedAssets.contains(asset)) {
			    removedAssets.add(asset);
			}
		}

		return removedAssets;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public List<Asset> getPOUsedAssetsWithPosition(PurchaseOrder po) throws PersistenceException
	{
		PerfTimer timer = monitor.startTimer("PurchaseOrderService::getPOUsedAssetsWithPosition");

		List<Asset> assets = po.getAssets();
		CommonWork cw = po.getCommonWork();

		// if assets collection is empty, return null
		if (assets.size() <= 0)
			return null;

		// only if PO attached to a commonWork, we calculate the positions
		// performance probably not the best
		if (null != cw) {
			for (Asset asset : assets) {
				List<AssetUse> auList = assetUseRepository.loadAssetUseListByCWIdAssetId(cw.getId(), asset.getId());
				StringBuilder sb = new StringBuilder();
				boolean gotOne = false;

				for (AssetUse usage : auList) {
					if (gotOne)
						sb.append(", ");
					String position = usage.getPosition();
					if (StringUtils.isNotBlank(position)) {
						sb.append(position);
						gotOne = true;
					}
				}

				asset.setPositionsAsString(sb.toString());
			}
		}
		timer.stopTimer();
		return assets;
	}

	/**
	 * Returns a list of purchaseOrders for a commonWork
	 * @param cwId
	 * @return List<CwFile>
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<PurchaseOrder> loadPurchaseOrdersByCWId(int cwId) {
		String sql = "from PurchaseOrder po join fetch po.source join fetch po.commonWork where po.commonWork.id = ?";
		TypedQuery<PurchaseOrder> query = entityManager.createQuery(sql, PurchaseOrder.class);
		query.setParameter(1, cwId);
		return query.getResultList();
	}

	/**
	 * Will return a list of purchase orders without a contract ordered desc by date, if no asset id specified,
	 * else returns the most recent one for the selected asset
	 * Maybe it an be done with 2 different queries and the decision can be done outside the function
	 * @param cwId
	 * @param sourceId
	 * @param assetId
	 * @return List<PurchaseOrder>
	 */
	@SuppressWarnings("unchecked")
	public List<PurchaseOrder> loadMostRecentPurchaseOrderList(Integer cwId, Integer sourceId, int assetId)
	{
		log.debug("loadMostRecentPurchaseOrderList(): cwId [" + cwId + "], sourceId [" + sourceId + "], assetId [" + assetId + "]");
		String sql = "select po.* from purchase_order po " +
			" left join contract on po.id = contract.purchase_order_id " +
			(0 != assetId ? " right join purchase_order_2_asset on po.id = purchase_order_2_asset.purchase_order_id and asset_base_id = :assetId" : "") +
			" where po.cw_id = :cwId and po.source_id = :sourceId and contract.id is null " +
			" order by date desc " +
			(0 != assetId ? " limit 1 " : "");
		log.debug("loadMostRecentPurchaseOrderList(): sql: " + sql);
		Query query = entityManager.createNativeQuery(sql, PurchaseOrder.class);

		query.setParameter("cwId", cwId);
		query.setParameter("sourceId", sourceId);
		if (0 != assetId)
			query.setParameter("assetId", assetId);
		return query.getResultList();
	}

	// this only includes assets for the special case Author Created from previous edition
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	@SuppressWarnings("unchecked")
	public List<Asset> getPrevEdAssets(Integer cwId, Integer sourceId) throws PersistenceException
	{
		String sql =  "select asset.*,asset_base.* from Asset, asset_base  where asset.id = asset_base.id " +
			" and asset.id in (select au.asset_id from asset_use au where au.cw_id = ? and previous_wileypub_au_id is not null)" +
			" and asset.id in (select asset_id from asset_2_source where source_id = ?);";
		log.debug("about to execute: " + sql);
		Query query = entityManager.createNativeQuery(sql, Asset.class);

		query.setParameter(1, cwId);
		query.setParameter(2, sourceId);

		List<Asset> assets = query.getResultList();
		log.debug("getPrevEdAssets() found " + assets.size() + " assets");

		return assets;
	}

	// this only includes the PO for the special case Author Created from previous edition
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	@SuppressWarnings("unchecked")
	public List<PurchaseOrder> getPrevEdAssetsPurchaseOrders(Integer cwId, Integer sourceId) throws PersistenceException
	{
		String sql = "select * from purchase_order where source_id = " + sourceId + " and cw_id = " + cwId + " and id " +
			"in(select purchase_order_id from purchase_order_2_asset where asset_base_id in(select id from asset" +
			" where id in(select asset_id from asset_use where cw_id = " + cwId + " and previous_wileypub_au_id is not null)))";

		log.debug("about to execute: " + sql);

		Query query = entityManager.createNativeQuery(sql, PurchaseOrder.class);

		return query.getResultList();
	}

	/**
	 * updates the "is attached" flag for assets in the purchase order, flag is going to be used in the PDF generation
	 * @param poId
	 * @param ids
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public void updateAttachedAssetIds(Integer poId, String ids) {
		Query query = entityManager.createNativeQuery("update purchase_order_2_asset set copy_attached=0 where purchase_order_id=?");
		query.setParameter(1, poId);
		query.executeUpdate();

		// Hibernate doesn't support binding collection to IN (...) in SQL queries.
		query = entityManager.createNativeQuery("update purchase_order_2_asset set copy_attached=1 where purchase_order_id=? and asset_base_id in (" + ids + ")");
		query.setParameter(1, poId);
		query.executeUpdate();
	}

	public AssetUseRepository getAssetUseRepository() {
		return assetUseRepository;
	}

	public void setAssetUseRepository(AssetUseRepository assetUseRepository) {
		this.assetUseRepository = assetUseRepository;
	}

	public DataSource getDataSource() {
		return dataSource;
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}
}
