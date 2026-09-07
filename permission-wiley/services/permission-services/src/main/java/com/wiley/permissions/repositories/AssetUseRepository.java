package com.wiley.permissions.repositories;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.wiley.permissions.common.bean.BeanUtility;
import com.wiley.permissions.domain.persistence.permissions.Asset;
import com.wiley.permissions.domain.persistence.permissions.AssetFile;
import com.wiley.permissions.domain.persistence.permissions.AssetPermissionRef;
import com.wiley.permissions.domain.persistence.permissions.AssetUse;
import com.wiley.permissions.domain.persistence.permissions.AssetUseFile;
import com.wiley.permissions.domain.persistence.permissions.AuSourcePermStatus;
import com.wiley.permissions.domain.persistence.permissions.CommonWork;
import com.wiley.permissions.domain.persistence.permissions.Component;
import com.wiley.permissions.domain.persistence.permissions.ComponentCategory;
import com.wiley.permissions.domain.persistence.permissions.Condition;
import com.wiley.permissions.domain.persistence.permissions.ConditionType.ConditionCode;
import com.wiley.permissions.domain.persistence.permissions.Contract;
import com.wiley.permissions.domain.persistence.permissions.ContractList;
import com.wiley.permissions.domain.persistence.permissions.ImportSource;
import com.wiley.permissions.domain.persistence.permissions.OwnerType;
import com.wiley.permissions.domain.persistence.permissions.PagePosition;
import com.wiley.permissions.domain.persistence.permissions.PermissionStatus;
import com.wiley.permissions.domain.persistence.permissions.Product;
import com.wiley.permissions.domain.persistence.permissions.PublicationStatus;
import com.wiley.permissions.domain.persistence.permissions.PurchaseOrder;
import com.wiley.permissions.domain.persistence.permissions.Source;
import com.wiley.permissions.domain.persistence.permissions.UserDefaults;
import com.wiley.permissions.domain.persistence.permissions.UserGroup;
import com.wiley.permissions.persistence.PersistenceException;
import com.wiley.permissions.persistence.repository.JPARepository;
import com.wiley.permissions.services.CommonWorkService;
import com.wiley.permissions.services.view.InvoiceTabView;
import com.wiley.sf.common.lang.StringUtil;
import com.wiley.sf.common.monitor.PerformanceMonitor.PerfTimer;


public class AssetUseRepository extends JPARepository {

	private static final Log log = LogFactory.getLog(AssetUseRepository.class);

	@PersistenceContext(unitName = "permissions")
	protected EntityManager entityManager;

	@Override
	protected EntityManager getEntityManager() {
		return entityManager;
	}

	private DataSource dataSource;

	private CommonWorkRepository cwRepository;
	private CommonWorkService cwService;
	private AssetRepository assetRepository;


	@Transactional(propagation = Propagation.REQUIRED)
	public AssetUse loadAssetUseByExternalId(String externalId) throws PersistenceException
	{
		log.debug("loadAssetUseByExternalId(): entered - extId = [" + externalId + "]");
		try {
			TypedQuery<AssetUse> query = entityManager.createQuery(
					"from AssetUse au where au.externalId = ?", AssetUse.class);
			query.setParameter(1, externalId);
			return query.getSingleResult();
		}
		catch (NoResultException e) {
			log.debug("loadAssetUseByExternalId(): failed to load by externalId [" + externalId + "]");
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	@Transactional(propagation = Propagation.REQUIRED, readOnly=true)
	public AssetUse loadAssetUseByUsage(String isbn, String usage, String vendorId) throws PersistenceException
	{
		log.debug("loadAssetUseByUsage(): entered ");
		try {
			String sql = "select * from asset_use au join asset a on au.asset_id = a.id and a.vendor_id= ? "  +
			" join product p on au.cw_id = p.cw_id and ";

			if (isbn.trim().length() == 13) {
				sql = sql + "isbn13 = ? where usage_type = ? ";
			} else {
				sql = sql + "isbn10 = ? where usage_type = ? ";
			}
			log.debug("loadAssetUseByUsage(): sql: " + sql + "with isbn: " + isbn + " usage: " + usage + " vendor_id: " + vendorId);
			Query  query = entityManager.createNativeQuery(sql, AssetUse.class);

			query.setParameter(2, isbn);
			query.setParameter(3, usage);
			query.setParameter(1,vendorId);
			List<AssetUse> result = query.getResultList();

			if (result.size() > 0) {
				// return only the first one
				AssetUse test = result.get(0);
				if (null != test.getUsage()) {
					test.getUsage().getCode();
				}
				test.getAsset().getSources();
				if (null != test.getStatus()) {
					test.getStatus().getCode();
				}
				if (null != test.getAsset().getMediaType()) {
					test.getAsset().getMediaType().getCode();
				}

				if (null != test.getPagePosition()){
					test.getPagePosition().getCode();
				}

				if (null != test.getAsset().getOwnerType()) {
					test.getAsset().getOwnerType().getCode();
				}

				test.getCommonWork().getProducts();
				test.getCommonWork().getPrimaryProduct();
				for (AssetFile file : test.getAsset().getFiles()) {
					file.getAsset();
				}

				return test;
			} else {
				log.debug("loadAssetUseByUsage(): failed to load ");
				return null;
			}
		}
		catch (NoResultException e) {
			log.debug("loadAssetUseByUsage(): failed to load ");
			return null;
		}
	}


	@Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
	public int countAssetUseInCommonWork(int cwId) throws PersistenceException
	{
		final String sql = "select count(id) as count from asset_use au where au.cw_id = ?";
		Query query = entityManager.createNativeQuery(sql, "scalarCount");
		query.setParameter(1, cwId);
		// For count, DB2 and MS SQL Server return Integer but MySQL returns BigInteger
		// Integer and BigInteger inherit from Number
		Number result = (Number) query.getSingleResult();

		return result.intValue();
	}

	/**
	 * Returns the asset descriptions of any asset uses that the given auId is a replacement for.
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public String loadReplacementForDescriptions(int auId) throws PersistenceException
	{
		// Generally there is only going to be a single replacement
		// But write query so that if there are multiple we get all that are unique (distinct)
		final String sql = "select group_concat(distinct(a.description)) as string"
				+ " from asset_use au, asset a where au.cancel_replacement_id = ? and au.asset_id = a.id";
		Query query = entityManager.createNativeQuery(sql, "scalarString");
		query.setParameter(1, auId);
		String result = (String) query.getSingleResult();
		return result;
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<AssetUse> loadAssetUsesByComponentId(int componentId) throws PersistenceException
	{
		PerfTimer timer = monitor.startTimer("AssetUseRepository::loadAssetUsesByComponentId");
		TypedQuery<AssetUse> query = entityManager.createQuery(
				"from AssetUse au where au.component.id = ?", AssetUse.class);
		query.setParameter(1, componentId);
		List<AssetUse> result = query.getResultList();
		timer.stopTimer();
		return result;
	}

	public PermissionStatus getAssetUseWorstStatus(List<AssetUse> assetUses){
		PermissionStatus status = null;
		PermissionStatus work = null;

		for (AssetUse assetUse : assetUses) {
			work = assetUse.getStatus();
			if (null == status ) status = work;
			if (null != work) {
				if (work.getStatusRating() < status.getStatusRating()) {
					status = work;
				}
			}
		}
		return status;
	}

	/**
	 * @param whereExpression  May be null to mean load all ids
	 */
	@SuppressWarnings("unchecked")
	@Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
	public List<Integer> loadAssetUseIds(String whereExpression) throws PersistenceException
	{
		final String sql = "select id from asset_use au" + (StringUtils.isBlank(whereExpression) ? "" : " where " + whereExpression);
		Query query = entityManager.createNativeQuery(sql, "scalarId");
		List<Integer> list = query.getResultList();
		return list;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
	public boolean isAssetInMultipleCws(Integer assetId) throws PersistenceException
	{
		final String sql = "select count(cw_id) as count from asset_use au where au.asset_id = ?";
		Query query = entityManager.createNativeQuery(sql, "scalarCount");
		query.setParameter(1, assetId);
		// For count, DB2 and MS SQL Server return Integer but MySQL returns BigInteger
		// Integer and BigInteger inherit from Number
		Number result = (Number) query.getSingleResult();
		return result.intValue() > 1;
	}

	/**
	 * If the auId does not exist a NoResultException will be thrown.
	 * (In other words this method assumes that the given auId is valid.)
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public Integer loadAssetIdForAssetUseId(int auId) {
		// Use "as id" to match scalarId
		final String sql = "select asset_id as id from asset_use where id = ?";
		Query query = entityManager.createNativeQuery(sql, "scalarId");
		query.setParameter(1, auId);
		return (Integer) query.getSingleResult();
	}

	/**
	 * Added for paperwork task to look for the Edition Cosmo Quiz
	 */
	/*@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<Condition> loadEditionConditionValueForAssetId(int assetId,int cwId) {
		// Use "as id" to match scalarId
		log.debug("Asset ID recieved"+assetId);
		log.debug("CW ID recieved"+cwId);
		final String sql_masterDeal = "select ma_deal_id as id from contract c,contract_2_asset c2a where c2a.contract_id = c.id and c2a.asset_base_id = ? and c.cw_id = ? ";
		Query query_masterDeal = entityManager.createNativeQuery(sql_masterDeal, "scalarId");
		query_masterDeal.setParameter(1, assetId);
		query_masterDeal.setParameter(2, cwId);
		try {
			int dealID = 0;
			try{
			dealID = (Integer)query_masterDeal.getSingleResult();
			}
			catch(NoResultException ex){
		//		log.warn("Deal id : ", ex);
			}
			log.debug(" deal ID");
			log.debug(dealID);
			if(dealID > 0){
				log.debug(" IN IF BLOCK");
				final String masterCondition = "select cvl.* from condition_value cvl where cvl.condition_type = '" + ConditionCode.EDITION.getCode() + "' and "
						+ "cvl.id in (select condition_id from ma_deal_2_condition where ma_deal_id = '"+dealID+"')";
				Query query_masterCondition = entityManager.createNativeQuery(masterCondition, Condition.class);
				try{
					@SuppressWarnings("unchecked")
					List<Condition> valueList = query_masterCondition.getResultList();
					log.debug(" IN IF BLOCK"+valueList.size());
					return valueList;
				}
				catch(NoResultException ex){
					log.warn("If Block caught exception: ", ex);
				}
			}
			else{
				final String sql = "select cv.* from contract_2_asset c2a,contract c,contract_2_condition c2c,condition_value cv "
				           +"where c.id = c2a.contract_id and c2c.contract_id = c.id and c2c.condition_id = cv.id and c.cw_id = ? and c2a.asset_base_id = ? and cv.condition_type = '" + ConditionCode.EDITION.getCode() + "'";
				Query query = entityManager.createNativeQuery(sql, Condition.class);
				query.setParameter(1, cwId);
				query.setParameter(2, assetId);
				try{
					@SuppressWarnings("unchecked")
					List<Condition> valueList = query.getResultList();
					log.debug(" IN ELSE BLOCK"+valueList.size());
					//log.debug("trdfddf --> "+valueList.get(0));
					return valueList;
				}
				catch(NoResultException ex){
					log.warn("Else Block caught exception: ", ex);
				}
			}
		}
		catch (NoResultException ex) {
			log.warn("Main Try Block caught exception: ", ex);
		}
		return null;
	}*/


	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<Condition> loadEditionConditionValueForAssetId(int assetId,int cwId) {
		// Use "as id" to match scalarId
		//final String sql = "select rollup_value from view_contract_condition where contract_id in (select contract_id from contract_2_asset where asset_base_id = ?) "
	//						+ " and condition_type = 'edition'";
	//	final String sql = "select cv.* from contract_2_condition c2c, condition_value cv "
	//					   + "where c2c.contract_id in (select contract_id from contract_2_asset where asset_base_id = ?) and c2c.condition_id = cv.id and cv.condition_type = '" + ConditionCode.EDITION.getCode() + "'";

        final String sql = "select cv.* from contract_2_asset c2a,contract c,contract_2_condition c2c,condition_value cv "
                           + "where c.id = c2a.contract_id and c2c.contract_id = c.id and c2c.condition_id = cv.id and c.cw_id = ? and c2a.asset_base_id = ? and cv.condition_type = '" + ConditionCode.EDITION.getCode() + "'";
		Query query = entityManager.createNativeQuery(sql, Condition.class);

		query.setParameter(1, cwId);
		query.setParameter(2, assetId);
		try{
			@SuppressWarnings("unchecked")
			List<Condition> valueList = query.getResultList();
			//log.debug("trdfddf --> "+valueList.get(0));
			return valueList;
		}
		catch(NoResultException ex){
			return null;
		}
	}

	/**
	 * If the auId does not exist then null will be returned.
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public Integer loadCwIdForAssetUseId(int auId) {
		// Use "as id" to match scalarId
		final String sql = "select cw_id as id from asset_use where id = ?";
		Query query = entityManager.createNativeQuery(sql, "scalarId");
		query.setParameter(1, auId);
		try {
			return (Integer) query.getSingleResult();
		}
		catch (NoResultException ex) {
			return null;
		}
	}

	//Added for Paperwork Task Granted Starts
		/**
		 * If the auId does not exist then null will be returned.
		 */
		@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
		public Integer loadCwIdForAssetId(int assetId, int sourceId) {
			// Use "as id" to match scalarId
			List<Contract> contract = new ArrayList<Contract>();
			int poId = 0;
			final String sql1 = "select distinct po from PurchaseOrder po, in (po.assets) a where a.id = ?"
					+ " and  po.source.id = ?"
					+ " order by po.date desc, po.id desc";
				// a lot of the PO dates are truncated to just day (not hours, etc)
				// so sort by "id desc" also
			TypedQuery<PurchaseOrder> query1 = entityManager.createQuery(sql1, PurchaseOrder.class);
			query1.setParameter(1, assetId);
			query1.setParameter(2, sourceId);
			query1.setMaxResults(1);
			@SuppressWarnings("unchecked")
			List<PurchaseOrder> list = query1.getResultList();
			if(list.size()>0){
				return list.get(0).getCommonWork().getId();
			}else{
				return 0;
			}
			//PurchaseOrderList poList = new PurchaseOrderList();
			//poList.addAll(list);
		}

		/**
		 * If the auId does not exist then null will be returned.
		 */
		@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
		public Integer loadCwIdForAssetIdForContract(int assetId, int sourceId,boolean latestOnly) {
			PerfTimer timer = monitor.startTimer("ContractRepository::loadForAssetSourceCW::latestOnly-"+latestOnly);
			try {
				TypedQuery<Contract> query = entityManager.createQuery(
					"select c from Contract c, ContractAsset ca where " +
					" c.source.id = ? and ca.contract.id = c.id and ca.assetBase.id = ?"
					+ " order by c.date desc, c.id desc",
					// a lot of the contract dates are truncated to just day (not hours, etc)
					// so sort by "id desc" also
					Contract.class
				);

				query.setParameter(1, sourceId);
				query.setParameter(2, assetId);
				if (latestOnly) {
					query.setMaxResults(1);
				}
				List<Contract> list = query.getResultList();
				ContractList contractList = new ContractList();
				contractList.addAll(list);
				if(contractList.size()>0){
					return contractList.get(0).getCommonWork().getId();
				}else{
					return 0;
				}
			}
			finally {
				timer.stopTimer();
			}
		}
		//Paperwork Task Granted Ends

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<Integer> loadAssetUseIdsForAssetIdAndCwId(int assetId, int cwId) {
		final String sql = "select id from asset_use where asset_id = ? and cw_id = ?";
		Query query = entityManager.createNativeQuery(sql, "scalarId");
		query.setParameter(1, assetId);
		query.setParameter(2, cwId);
		@SuppressWarnings("unchecked")
		List<Integer> auIdList = query.getResultList();
		return auIdList;
	}

	/**
	 * Consider whether you need ALL AU ids for the assetId or just those for a particular CW.
	 * If just a particular CW, use the method above.
	 *
	 * If you just need the count, use the method below instead.
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<Integer> loadAssetUseIdsForAssetId(int assetId) {
		// Use "as id" to match scalarId
		final String sql = "select id from asset_use where asset_id = ?";
		Query query = entityManager.createNativeQuery(sql, "scalarId");
		query.setParameter(1, assetId);
		@SuppressWarnings("unchecked")
		List<Integer> auIdList = query.getResultList();
		return auIdList;
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public int countAssetUsesForAssetId(int assetId) {
		final String sql = "select count(*) as count from asset_use where asset_id = ?";
		Query query = entityManager.createNativeQuery(sql, "scalarCount");
		query.setParameter(1, assetId);
		// For count, DB2 and MS SQL Server return Integer but MySQL returns BigInteger
		// Integer and BigInteger inherit from Number
		Number result = (Number) query.getSingleResult();
		return result.intValue();
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public int countContractsForAssetId(int assetId) {
		final String sql = "select count(*) as count from contract_2_asset where asset_base_id = ?";
		Query query = entityManager.createNativeQuery(sql, "scalarCount");
		query.setParameter(1, assetId);
		// For count, DB2 and MS SQL Server return Integer but MySQL returns BigInteger
		// Integer and BigInteger inherit from Number
		Number result = (Number) query.getSingleResult();
		return result.intValue();
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public int countAssetUsesForAssetIdAndCwId(int assetId, int cwId) {
		final String sql = "select count(*) as count from asset_use where asset_id = ? and cw_id = ?";
		Query query = entityManager.createNativeQuery(sql, "scalarCount");
		query.setParameter(1, assetId);
		query.setParameter(2, cwId);
		// For count, DB2 and MS SQL Server return Integer but MySQL returns BigInteger
		// Integer and BigInteger inherit from Number
		Number result = (Number) query.getSingleResult();
		return result.intValue();
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<AssetUse> loadAssetUseListForAssetId(int assetId) {
		final String sql = "select au from AssetUse au " +
			"join fetch au.commonWork " +
			// left join on status because it can be null if there is a bug in the rules
			// left join on ownerType and mediaType because it's possible we can receive from CMS as null
			"join fetch au.asset a " +
			// Think for this method it is safe to join on sources since we are only getting a single AssetUse
			"left join fetch a.sources " +
			"where au.asset.id = ? ";
		TypedQuery<AssetUse> query = entityManager.createQuery(sql, AssetUse.class);
		query.setParameter(1, assetId);
		return query.getResultList();
	}

	// lnagy - maybe improve performance even more by doing a query with asset_id IN (..)
	// smarkoff - time it both ways (won't be able to re-use the same prepared statement with IN)
	public List<AssetUse> loadAssetUseListByCWIdAssetList(int cwId, List<Asset> aList) throws PersistenceException
	{
		List<AssetUse> auList = new ArrayList<AssetUse>();
		// if assets collection is empty, return null
		if (aList.size() <= 0) {
			return auList;
		}

		for (Asset asset : aList) {
			List<AssetUse> au = loadAssetUseListByCWIdAssetId(cwId, asset.getId());
			auList.addAll(au);
		}

		return auList;
	}


	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<AssetUse> loadAssetUseListByCWIdAssetId(Integer cwId, Integer assetId)
	throws PersistenceException
	{
		PerfTimer timer = monitor.startTimer("AssetUseService::loadAssetUseListByCWIdAssetId");
		final String sql = "select au from AssetUse au " +
			// left join on status because it can be null if there is a bug in the rules
			// left join on ownerType and mediaType because it's possible we can receive from CMS as null
			"join fetch au.asset a join fetch au.usage left join fetch au.component left join fetch au.status " +
			"left join fetch au.size " +
			"left join fetch a.ownerType left join fetch a.mediaType " +
			// Don't join on sources because if a product has more than one asset usage for the same asset
			// then we get each source the asset has multiple times
			// "left join fetch a.sources " +
			"where au.commonWork.id = ? and au.asset.id = ?";
		TypedQuery<AssetUse> query = entityManager.createQuery(sql, AssetUse.class);
		query.setParameter(1, cwId);
		query.setParameter(2, assetId);
		List<AssetUse> auList = query.getResultList();
		timer.stopTimer();
		return auList;
	}

	/* This method is primarily used to extract a list of assets to be cleared
	 * but it could be use anyplace where a list of asset uses are needed based on a
	 * comma separated list of asset use id's
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<AssetUse> loadAssetUseListByIdList(String assetUseIds) throws PersistenceException
	{
		final String sql = "select au from AssetUse au " +
		"join fetch au.asset a join fetch au.usage left join fetch au.component left join fetch au.status " +
		"left join fetch a.ownerType left join fetch a.mediaType " +
		"left join fetch au.commonWork left join fetch au.userGroup " +
		 "left join fetch a.sources " +
		"where au.id in (" + assetUseIds + ")";

	TypedQuery<AssetUse> query = entityManager.createQuery(sql, AssetUse.class);

	List<AssetUse> auList = query.getResultList();

	return auList;

	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<AssetUse> loadAssetUseListByCWId(int cwId) throws PersistenceException
	{
		PerfTimer timer = monitor.startTimer("AssetUseService::loadAssetUseListByCWId");

		TypedQuery<AssetUse> query = entityManager.createQuery(
			"select distinct au from AssetUse au " +
			// left join on status because it can be null if there is a bug in the rules
			// left join on ownerType and mediaType because it's possible we can receive from CMS as null
			"join fetch au.asset a join fetch au.usage left join fetch au.component left join fetch au.status " +
			"left join fetch au.size " +
			"left join fetch a.ownerType left join fetch a.mediaType " +
			// Don't join on sources because if a CommonWork has more than one asset usage for the same asset
			// then we get each source the asset has multiple times
			// "left join fetch a.sources " +
			"where au.commonWork.id = ? order by au.position ", AssetUse.class
		);

		query.setParameter(1, cwId);
		List<AssetUse> auList = query.getResultList();
		timer.stopTimer();
		return auList;
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<AssetUse> loadAssetUseListByCWIdForReplacements(int cwId, String filter) throws PersistenceException
	{
		PerfTimer timer = monitor.startTimer("AssetUseService::loadAssetUseListByCWIdForReplacements");
		String wfilter = "";
		if(null != filter) {
			wfilter = " and " + filter;
		}
		TypedQuery<AssetUse> query = entityManager.createQuery(
			"select distinct au from AssetUse au " +
			"join fetch au.asset a join fetch au.usage left join fetch au.component left join fetch au.status " +
			"left join fetch a.ownerType left join fetch a.mediaType " +
			 "left join fetch a.sources " +
			 "left join fetch au.component " +
			 "left join fetch au.usage " +
			"where au.commonWork.id = ? " + wfilter + " order by au.position ", AssetUse.class
		);

		query.setParameter(1, cwId);
		List<AssetUse> auList = query.getResultList();
		timer.stopTimer();
		return auList;
	}


	/**
	 * @param status  Must be non-null
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<Integer> loadAssetUseIdListByStatus(PermissionStatus status) throws PersistenceException
	{
		PerfTimer timer = monitor.startTimer("AssetUseService::loadAssetUseIdListByStatus");

		// Use "as id" to match scalarId
		final String sql = "select id from asset_use where permission_status = ?";
		Query query = entityManager.createNativeQuery(sql, "scalarId");
		query.setParameter(1, status.getCode());
		@SuppressWarnings("unchecked")
		List<Integer> list = query.getResultList();

		timer.stopTimer();
		return list;
	}

	/**
	 * Duplicate method because we want to filter products with pub status not in 'O', 'W'
	 * for out of compliance emails
	 * https://www.pivotaltracker.com/story/show/33963139
	 * @param status  Must be non-null
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<Integer> loadAssetUseIdListByStatusForOutOfCompliance(PermissionStatus[] status) throws PersistenceException
	{
		PerfTimer timer = monitor.startTimer("AssetUseService::loadAssetUseIdListByStatusForOutOfCompliance");

		// Use "as id" to match scalarId
		String sql = "select au.id from asset_use au join product p on au.cw_id = p.cw_id  where au.permission_status IN ("
				+ PermissionStatus.getSQLClause(status) + ") " +
				" and p.is_cw_primary is true and p.pub_status not in ('"
				+ PublicationStatus.OUT_OF_PRINT.getCode() + "', '" + PublicationStatus.NEW_EDITION_PENDING.getCode() + "')";
		Query query = entityManager.createNativeQuery(sql, "scalarId");
		@SuppressWarnings("unchecked")
		List<Integer> list = query.getResultList();

		timer.stopTimer();
		return list;
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<AssetUse> loadAssetUseListByCWIdComponent(int cwId, int componentId) throws PersistenceException
	{
		PerfTimer timer = monitor.startTimer("AssetUseService::loadAssetUseListByCWIdComponent");

		TypedQuery<AssetUse> query = entityManager.createQuery(
			"select distinct au from AssetUse au " +
			// left join on status because it can be null if there is a bug in the rules
			// left join on ownerType and mediaType because it's possible we can receive from CMS as null
			"join fetch au.asset a join fetch au.usage left join fetch au.component left join fetch au.status " +
			"left join fetch au.size " +
			"left join fetch a.ownerType left join fetch a.mediaType " +
			// Don't join on sources because if a CommonWork has more than one asset usage for the same asset
			// then we get each source the asset has multiple times
			// "left join fetch a.sources " +
			"where au.commonWork.id = ?  and au.component.id = ? ", AssetUse.class
		);

		query.setParameter(1, cwId);
		query.setParameter(2, componentId);
		List<AssetUse> auList = query.getResultList();
		timer.stopTimer();
		return auList;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public AssetUse loadAssetUseById(int auId) throws PersistenceException
	{
		PerfTimer timer = monitor.startTimer("AssetUseRepository::loadAssetUseById");

		TypedQuery<AssetUse> query = entityManager.createQuery(
			"select au from AssetUse au " +
			"join fetch au.commonWork " +
			// left join on status because it can be null if there is a bug in the rules
			// left join on ownerType and mediaType because it's possible we can receive from CMS as null
			"join fetch au.asset a join fetch au.usage left join fetch au.component left join fetch au.status " +
			"left join fetch au.size " +
			"left join fetch au.pagePosition " +
			"left join fetch a.ownerType left join fetch a.mediaType " +
			// Think for this method it is safe to join on sources since we are only getting a single AssetUse
			"left join fetch a.sources " +
			"where au.id = ? ", AssetUse.class
		);

		query.setParameter(1, auId);
		AssetUse au = query.getSingleResult();

		timer.stopTimer();
		return au;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public List<Source> loadSourceForStatus(int auId, PermissionStatus[] stat) throws PersistenceException
	{
		AssetUse au = loadAssetUseById (auId);
		List<Source> sources = new ArrayList<Source> ();

		List<AuSourcePermStatus> statuses = au.getSourceStatuses();
		for (AuSourcePermStatus status : statuses) {
			// peek the first source with status
			if (Arrays.asList(stat).contains(status.getStatus())) {
				sources.add(status.getSource());
			}
		}

		return sources;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public List<Source> loadSourcesNeedingPayment(int auId) throws Exception {
		AssetUse au = lazyLoad(AssetUse.class, auId, new String [] {"sourceStatuses"});  // throws Exception
		List<Source> sources = new ArrayList<Source>(au.getSourceStatuses().size());
		for (AuSourcePermStatus asps : au.getSourceStatuses()) {
			if (asps.isNeedPaymentRequest()) {
				sources.add(asps.getSource());
				asps.getSource().getId();  // lazy load so can call outside this method
			}
		}
		return sources;
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public AssetUse loadAssetUseByIdForManageAsset(int auId) throws PersistenceException {
		AssetUse au = loadAssetUseById(auId);

		PagePosition pp = au.getPagePosition();
		if (pp != null)  pp.getDescription();

		for (Product p : au.getCommonWork().getProducts()) {
			p.getTitle();
		}

		for (AssetFile file : au.getAsset().getFiles()) {
			file.getObjectName();
		}

		return au;
	}

	public void setNeedsConfirmationFlag(AssetUse au) {
		au.setNeedToConfirmCancels(false);

		if (null != au.getAsset()) {
			// asset use may come from search index and it may be missing assets
			// so need to load it here
			// so we can test if the asset also belongs to another product
			try {
				if (au.getAsset().getAllPurchaseOrdersCount() > 1
						|| au.getAsset().getAllContractsCount() > 1
						|| au.isCanceled()) {
					au.setNeedToConfirmCancels(false);
					return;
				}
				if (isAssetInMultipleCws(au.getAsset().getId())) {
					au.setNeedToConfirmCancels(true);
				}
			}
			catch (Exception ex) {
				log.warn("setNeedsConfirmationFlag(): caught exception: ", ex);
			}
		}
	}

	/**
	 * Attaches another AssetUse from another CommonWork to current CW.
	 *
	 * @param cw
	 * @param oldCw
	 * @param auId
	 * @param edition
	 * @throws Exception
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public AssetUse copyAsset(int cwId, int origCwId,
			Integer auId, boolean previousEdition, boolean includeUsage, int userGroupId) throws Exception
	{
		log.debug("copyAsset(): entered...");
		PerfTimer timer = monitor.startTimer("AssetUseService::copyAsset");
		try {
			AssetUse newAu = new AssetUse();
			AssetUse oldAu = find(AssetUse.class, auId);
			log.debug("StatusExplanation--> "+oldAu.getStatusExplanation());
			log.debug("Status--> "+oldAu.getStatus());
			Asset asset = oldAu.getAsset();
			//lnagy - copy just the active ones, do not copy disabled ones (prevented from UI also)
			if (!asset.isActive())
				return null;

			// Code change for SS task 11 starts
			newAu.setUserGroup((UserGroup)this.executeSingleResultNamedQuery("UserGroup.findById", new Object[] {userGroupId}));
			// Code change for SS task 11 ends
			// Users need option to copy assets from another product with or without the usage data.
			// the usage boolean flag indicates if we want to copy the usage or not
			log.debug("old user group id..."+oldAu.getUserGroup().getId());
			log.debug("old user group name..."+oldAu.getUserGroup().getName());
			if (includeUsage) {
				PerfTimer mergeTimer = monitor.startTimer("AssetUseService::copyAsset::merge");
				BeanUtility.merge(oldAu, newAu);
				newAu.setFinalPage(null);
				newAu.setManuscriptPage(null);
				//Changes for SS Task 9
				newAu.setPosition(oldAu.getPosition());
				mergeTimer.stopTimer();
				// "copy" the component also
				Component oldComponent = oldAu.getComponent();
				if (null != oldComponent) {
					PerfTimer copyComponentTimer = monitor.startTimer("AssetUseService::copyAsset::copyComponent");
					Component newComponent = getCommonWorkRepository().copyComponent (cwId, oldComponent);
					newAu.setComponent(newComponent);
					copyComponentTimer.stopTimer();
				}
			}
			else {
				newAu.setUsage(oldAu.getUsage());
				//Changes for SS Task 9
				newAu.setPosition(null);
			}

			log.debug("copyAsset(): after include usage..."+newAu.getUserGroup().getName());
			// new one
			newAu.setCreatedDate(null);
			newAu.setExternalId(null);
			newAu.setImportSource(null);
			newAu.setId(null);
			// set them to null so the preInsert/preUpdate will set the right user ids
			newAu.setCreatedUser(null);
			newAu.setLastUpdatedUser(null);
			// do not import position, sortOrder (https://www.pivotaltracker.com/story/show/38121321)
			//Changes for SS Task 9
			//newAu.setPosition(null);
			newAu.setSortOrder(null);
			// not sure if merge takes care of it
			newAu.setAsset(asset);
			// attach to current product
			CommonWork cw = new CommonWork();
			cw.setId(cwId);
			newAu.setCommonWork(cw);

			CommonWork oldCw = cwRepository.loadCWById(origCwId);

			if (previousEdition) {
				// sets Reuse from Previous Edition to true
				newAu.setReusedFromPreviousEdition(true);
				newAu.setReusedISBN(oldCw.getPrimaryProduct().getIsbn13());
				//Made change for Paperwork
				//newAu.setPickup(false);
				//newAu.setPickupISBN(null);
				//Changes made for SS Task 9
				newAu.setReusedPosition(oldAu.getPosition());
				newAu.setReusedPage(oldAu.getReusedPage());//modified for Import issues
			}
			// copy for multiuse should not setup pickup
			// https://www.pivotaltracker.com/story/show/63909094
			else if (cwId != origCwId) {
				// sets Pickup
				newAu.setPickup(true);
				newAu.setPickupISBN(oldCw.getPrimaryProduct().getIsbn13());
				newAu.setPickupTitle(oldCw.getPrimaryProduct().getTitle());
				newAu.setPickupAuthor(oldCw.getPrimaryProduct().getAuthorsAsString());
				newAu.setPickupEditionNumber(oldCw.getPrimaryProduct().getEditionNumber());
				newAu.setPickupPosition(oldAu.getPosition());
				newAu.setPickupPage(oldAu.getPickupPage());//modified for Import issues
				//Made change for Paperwork
			//	newAu.setReusedFromPreviousEdition(false);
			//	newAu.setReusedISBN(null);

			}

			//Added for Granted Import Task Starts
			newAu.setStatusExplanation(oldAu.getStatusExplanation());
			newAu.setStatus(oldAu.getStatus());
			//Granted Import Task ends
			log.debug("Status --> "+newAu.getStatusExplanation());
			log.debug("Status --> "+newAu.getStatus().toString());

			// creates and updates the permission status
			// lnagy - for performance purposes we do not send the message here
			// we get a list of all assetsToProduct that need a message
			// and send a separate command to send the messages
			// newAu = entityManager.merge(newAu);
			return newAu;
		} finally {
			timer.stopTimer();
		}
	}

	/**
	 * This method is called by AssetUseService.deleteAssetUseById().
	 * - Always call the Service method from a Controller (not this method).
	 * (This method should ONLY be called by the Service method.)
	 * @throws PersistenceException
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public List<Integer> deleteAssetUseById(int auId) throws PersistenceException
	{
		// delete replacement refs
		// -- don't need to because put "ON DELETE SET NULL" on au.cancel_replacement_id
		// -- but DO need to get list of AssetUse Ids that are affected so can recalculate status
		// for these (status will change from replaced to cancel and the index will be updated also)
		List<Integer> replacedIds = getReplacedByAUIds(auId);
		log.debug("deleteAssetUseById(): replacedIds: "
			+ (replacedIds.size() == 0 ? "(none)" : StringUtils.join(replacedIds, ", ")));

		// au.previous_wileypub_au_id has "ON DELETE SET NULL" so don't need code for this

		// delete from asset_perm_ref table
		// -- taken care of by "ON DELETE CASCADE" on asset_perm_ref.asset_use_id

		// delete from au_source_perm_status
		// -- taken care of by "ON DELETE CASCADE" on au_source_perm_status.asset_use_id

		Integer cwId = loadCwIdForAssetUseId(auId);
		if (cwId != null) {
			final String sql = "delete from asset_use where id = ?";
			Query q = entityManager.createNativeQuery(sql);
			q.setParameter(1, auId);
			int rows = q.executeUpdate();
			if (rows != 1) {
				log.warn("deleteAssetUseById(): rows affected = " + rows + " instead of 1. auId = " + auId);
			}

			cwService.addToCwHistory_assetUseDelete(cwId);
		}

		return replacedIds;
	}

	/**
	 * This method is called by the clear asset function.
	 * It checks to see if there are po or contract objects that need to be cleared out.
	 * Adds the ids of any deleted contracts to the given contractIdClearedList.
	 * Similar for poIdClearedList.
	 * @throws PersistenceException
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void clearAssetDocumentationForCW(int assetId, int cwId,
			List<Integer> contractIdClearedList, List<Integer> poIdClearedList) throws PersistenceException
	{
		log.debug("clearAssetDocumentation(): assetId: " + assetId + " cwId: " + cwId );

		// delete contracts first since POs may reference contracts

		List<Integer> contractIds = assetRepository.getContractIdsForAssetAndCW(assetId, cwId);
		log.debug("clearAssetDocumentation(): contractIds.size() = " + contractIds.size());
		for (Integer contractId : contractIds) {
			log.debug("clearAssetDocumentation(): deleting contract: " + contractId);
			assetRepository.deleteContract(contractId);
			contractIdClearedList.add(contractId);
		}

		List<Integer> poIds = assetRepository.getPOIdsForAssetAndCW(assetId, cwId);
		log.debug("clearAssetDocumentation(): poIds.size() = " + poIds.size());
		for (Integer poId : poIds) {
			log.debug("clearAssetDocumentation(): deleting PO: " + poId);
			assetRepository.deletePurchaseOrder(poId);
			poIdClearedList.add(poId);
		}
	}

	private List<Integer> getReplacedByAUIds(int auId) {
		Query query = entityManager.createNativeQuery(
				"select id from asset_use where cancel_replacement_id = ?",
				"scalarId");
		query.setParameter(1, auId);
		@SuppressWarnings("unchecked")
		List<Integer> ids = query.getResultList();
		return ids;
	}

	/**
	 * Normally this will be used instead of deleteAssetFile() below since an asset
	 * normally contains an original file plus thumb nails in different sizes.
	 * So normally if you are deleting the original file you want to delete all files.
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public void deleteAllAssetFiles(int assetId) throws PersistenceException
	{
		final String sql = "delete from asset_file where asset_id = ?";
		Query q = entityManager.createNativeQuery(sql);
		q.setParameter(1, assetId);
		int rows = q.executeUpdate();
		log.debug("deleteAllAssetFiles(): deleted " + rows + " files.");
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void deleteAssetFile(int assetFileId) throws PersistenceException
	{
		final String sql = "delete from asset_file where id = ?";
		Query q = entityManager.createNativeQuery(sql);
		q.setParameter(1, assetFileId);
		int rows = q.executeUpdate();
		if (rows != 1) {
			log.warn("deleteAssetFile(): rows affected = " + rows + " instead of 1. assetFileId = " + assetFileId);
		}
	}

	/**
	 * Use deleteAssetUseFiles (plural) below if you want to delete all the files for an AssetUse.
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public void deleteAssetUseFile(int auFileId) throws PersistenceException
	{
		final String sql = "delete from asset_use_file where id = ?";
		Query q = entityManager.createNativeQuery(sql);
		q.setParameter(1, auFileId);
		int rows = q.executeUpdate();
		if (rows != 1) {
			log.warn("deleteAssetUseFile(): rows affected = " + rows + " instead of 1. auFileId = " + auFileId);
		}
	}

	/**
	 * Deletes all the files for an AssetUse.
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public void deleteAssetUseFiles(int auId) throws PersistenceException
	{
		final String sql = "delete from asset_use_file where au_id = ?";
		Query q = entityManager.createNativeQuery(sql);
		q.setParameter(1, auId);
		int rows = q.executeUpdate();
		log.debug("deleteAssetUseFiles(): " + rows + " rows deleted");
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public int getNextAssetUseId(AssetUse assetUse) {
		Query query = entityManager.createNativeQuery(
				"select au.id from asset_use au join asset a on a.id = au.asset_id " +
				" left join component c on c.id = au.component_id " +
				"where au.cw_id = ? order by c.name, position, a.description",
				"scalarId");
		query.setParameter(1, assetUse.getCommonWork().getId());
		@SuppressWarnings("unchecked")
		List<Integer> ids = query.getResultList();
		int index = ids.indexOf(assetUse.getId());
		if (index != -1 && (index + 1) < ids.size()) {
			return ids.get(index + 1);
		}
		else return ids.get(0);
	}

	/**
	 * Called by PermissionStatusValidationServiceImpl (needs to be in a separate class
	 * so can say REQUIRES_NEW.
	 *
	 * @param au
	 * @param sources
	 * @throws PersistenceException
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void deleteNonCurrentSourceStatuses(AssetUse au, List<Source> sources) throws PersistenceException
	{
		String sql = "delete from AuSourcePermStatus where assetUseId = ?";

		if (CollectionUtils.isNotEmpty(sources)) {
			List<Integer> sourceIdList = new ArrayList<Integer>(sources.size());
			for (Source s : sources) {
				sourceIdList.add(s.getId());
			}
			sql += " and sourceId NOT in (" + StringUtils.join(sourceIdList, ", ") + ")";
		}

		Query q = entityManager.createQuery(sql);
		q.setParameter(1, au.getId());
		q.executeUpdate();
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
	public List<Integer> loadAssetUseIdsForSourceExtId(String sourceExtId)
	{
		final String sql = "select au.id from asset a, source s, asset_2_source a2s, asset_use au "
			+ "where s.external_id = ? and a2s.source_id = s.id and a2s.asset_id = a.id and au.asset_id = a.id";
		// "scalarId" is defined in an annotation at the top of the AssetUse class.
		Query query = entityManager.createNativeQuery(sql, "scalarId");
		query.setParameter(1, sourceExtId);
		@SuppressWarnings("unchecked")
		List<Integer> auIdList = query.getResultList();

        return auIdList;
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<Integer> loadAssetUseIdsBySourceIdCwIdPermissionStatus(Integer sourceId, Integer cwId, PermissionStatus permissionStatus)
	{
		final String sql = "select au.id from asset a, source s, asset_2_source a2s, asset_use au "
			+ "where s.id = ? and a2s.source_id = s.id and a2s.asset_id = a.id and au.asset_id = a.id"
			+ " and au.cw_Id = ? and au.permission_status = ?";
		// "scalarId" is defined in an annotation at the top of the AssetUse class.
		Query query = entityManager.createNativeQuery(sql, "scalarId");
		query.setParameter(1, sourceId);
		query.setParameter(2, cwId);
		query.setParameter(3, permissionStatus.getCode());
		@SuppressWarnings("unchecked")
		List<Integer> auIdList = query.getResultList();

        return auIdList;
	}

/*	@Transactional(propagation = Propagation.NEVER)
	public void logTransactionIsolation() throws SQLException {
		DataSource dataSource = getDataSource();
		Connection con = dataSource.getConnection();
		log.debug("logTransactionIsolation(): transactionIsolation = "
				+ NameForConstants.nameForTransactionIsolation(con.getTransactionIsolation()));
		con.close();
		IsolationLevelDet det = new IsolationLevelDet(getDataSource(), 1);
		int detLevel = det.determineIsoLevel();
		log.debug("logTransactionIsolation(): determined trans. isolation = " + NameForConstants.nameForTransactionIsolation(detLevel));
	}
*/

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<InvoiceTabView> loadInvoiceTabView(int cwId, String customFilter) throws SQLException
	{
		PerfTimer timer = monitor.startTimer("AssetUseRepository::loadInvoiceTabView");

		if (StringUtils.isBlank(customFilter)) {
			customFilter = StringUtils.EMPTY;
		}
		final String sql =
			"select a.id, au.position, u.description, a.description, mt.description, ps.description,\r\n" +
			" s.name, c.number, DATE_FORMAT(c.date, '%b %d,%Y') as cdateformatted, c.payment_type is not null,\r\n" +
			" coalesce((c.id = aps.latest_contract_id), false) as latest, c.id, c.purchase_order_id\r\n" +
			" from contract c, contract_2_asset c2a, asset a, asset_use au\r\n" +
			" left join component comp on au.component_id = comp.id\r\n" +
			" left join au_source_perm_status aps on aps.asset_use_id = au.id,\r\n" +
			" media_type mt, usage_type u, source s, permission_status ps\r\n" +
			// smarkoff: bug fix 9/20/2013 - before added "and au_cw_id = c.cw_id" would show extra rows for asset uses outside this cwId
			" where c.id = c2a.contract_id and c.cw_id = ? and au.cw_id = c.cw_id\r\n" +
			" and a.id = c2a.asset_base_id and au.asset_id = a.id\r\n" +
			" and au.usage_type = u.code and a.media_type = mt.code\r\n" +
			" and c.source_id = s.id and aps.source_id = s.id and ps.code = aps.permission_status\r\n" +
			(customFilter.equals(UserDefaults.CUSTOM_FILTER) ? " and comp.category = '" + ComponentCategory.COVER.getCode() + "' " : "") +
			(customFilter.equals(UserDefaults.INTERNAL_FILTER) ? " and comp.category != '" + ComponentCategory.COVER.getCode() + "' " : "") +
			
			"union\r\n" +
			
			"select a.id,au.position,u.description,a.description,mt.description, ps.description,\r\n"+
			"s.name,c.number,DATE_FORMAT(c.date, '%b %d,%Y') as cdateformatted, c.payment_type is not null,\r\n" +
			"coalesce((c.id = aps.latest_contract_id), false) as latest, c.id, c.purchase_order_id\r\n" +
			"from contract c,asset_use au,contract_2_asset c2a,au_source_perm_status aps,asset a,usage_type u,media_type mt,permission_status ps, source s,asset_2_source a2s\r\n"+
			"where c2a.contract_id=c.id and c2a.asset_base_id=au.asset_id and aps.asset_use_id=au.id and aps.latest_contract_id=c.id and mt.code=a.media_type and ps.code=au.permission_status\r\n"+
			"and a2s.asset_id=a.id and a2s.source_id=s.id and au.cw_id=? and au.cw_id!=c.cw_id and au.asset_id=a.id and au.usage_type=u.code\r\n" +
			"order by name";
			//"order by s.id, c.id, c2a.asset_base_id";

		log.debug("loadInvoiceTabView(): sql:\r\n" + sql);
		Connection con = getDataSource().getConnection();
		PreparedStatement ps = null;

		try {
			ps = con.prepareStatement(sql);
			ps.setInt(1, cwId);
			ps.setInt(2, cwId);   // Fix for INC_107966 ,added the Union part in sql. 
			ResultSet rs = ps.executeQuery();
			List<InvoiceTabView> rows = new ArrayList<InvoiceTabView>();
			while (rs.next()) {
				InvoiceTabView row = new InvoiceTabView();
				row.setAssetId(rs.getInt(1));
				row.setPosition(rs.getString(2));
				row.setUsage(rs.getString(3));
				row.setDescription(rs.getString(4));
				row.setMediaType(rs.getString(5));
				row.setStatus(rs.getString(6));
				row.setSource(rs.getString(7));
				row.setContractNumber(rs.getString(8));
				row.setContractDate(rs.getString(9));
				row.setPaid(rs.getBoolean(10));
				row.setLatest(rs.getBoolean(11));
				row.setInvoiceId(rs.getInt(12));
				row.setPoId(rs.getInt(13));
				rows.add(row);
			}
			timer.stopTimer();
			return rows;
		}
		finally {
			if (ps != null) ps.close();
			con.close();
		}
	}

	//Code Change for SS Task 3 - Added new parameter userGroupId
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public Map<String, String> loadSummaryReportInfo(int cwId, int userGroupId, int auId, String photoFlag) throws SQLException, PersistenceException
	{
		log.debug("SummaryReport ------> "+userGroupId);
		PerfTimer timer = monitor.startTimer("AssetUseRepository::loadSummaryReportInfo");
		Connection con = getDataSource().getConnection();
		PreparedStatement ps = null;

		//Modified for RightNow Ticket starts
		Map<String, String> list = loadPreviousContractDtls(cwId, auId);
		//Modified for RightNow Ticket ends
		String sql =
			"select au.request_comment, " +
			" DATE_FORMAT(po.created_date, '%c/%d/%Y') as pgroup, " +
			" DATE_FORMAT(IF(c.is_permission_form, c.created_date, c.start_date), '%c/%d/%Y') as cgroup, " +
			" c.number as invoiceNumber, c.id as contractId, DATE_FORMAT(c.start_date, '%c/%d/%Y') as startDate, " +
			" DATE_FORMAT(c.end_date, '%c/%d/%Y') as expirationDate," +
			" DATE_FORMAT(c.date, '%c/%d/%Y') as invoiceDate, au.estimated_cost as estimatedCost, " +
			//Added for Asset Final Cost by replacing Contract cost
			" CASE when c.date is null then 0.00 ELSE c2a.price END as finalCost, " +
			" CASE when c.id is null then '' ELSE (select  rollup_value from contract_2_condition, condition_value " +
		    " where contract_2_condition.contract_id = c.id and contract_2_condition.condition_id = condition_value.id " +
            " and condition_type = '" + ConditionCode.LANGUAGE.getCode() + "') END as language, " +
            " CASE when c.id is null then '' ELSE (select  rollup_value from contract_2_condition, condition_value " +
            " where contract_2_condition.contract_id = c.id and contract_2_condition.condition_id = condition_value.id " +
            " and condition_type = '" + ConditionCode.SALES_TERRITORY.getCode() + "') END as territory,  " +
            " CASE when c.id is null then '' ELSE (select  rollup_value from contract_2_condition, condition_value  " +
            " where contract_2_condition.contract_id = c.id and contract_2_condition.condition_id = condition_value.id " +
            " and condition_type = '" + ConditionCode.PRINT_RUN.getCode() + "') END as printRun, " +
            //Start: Added to implement DM-122
            " CASE when c.id is null then '' ELSE (select  rollup_value from contract_2_condition, condition_value  " +
            " where contract_2_condition.contract_id = c.id and contract_2_condition.condition_id = condition_value.id " +
            " and condition_type = '" + ConditionCode.SEATS.getCode() + "') END as seats, " +
            //End: Added to implement DM-122
            " CASE when c.id is null then '' ELSE (select  rollup_value from contract_2_condition, condition_value " +
            " where contract_2_condition.contract_id = c.id and contract_2_condition.condition_id = condition_value.id " +
            " and condition_type = '" + ConditionCode.DERIVATIVE_WORKS.getCode() + "') END as derivativeWork, " +
            " CASE when c.id is null then '' ELSE (select  rollup_value from contract_2_condition, condition_value " +
            " where contract_2_condition.contract_id = c.id and contract_2_condition.condition_id = condition_value.id " +
            " and condition_type = '" + ConditionCode.SUBLICENSE.getCode() + "') END as sublicense, " +
            " CASE when c.id is null then '' ELSE (select  rollup_value from contract_2_condition, condition_value " +
            " where contract_2_condition.contract_id = c.id and contract_2_condition.condition_id = condition_value.id " +
            " and condition_type = '" + ConditionCode.EDITION.getCode() + "') END as editions, " +
            " CASE when c.id is null then '' ELSE (select  rollup_value from contract_2_condition, condition_value " +
            " where contract_2_condition.contract_id = c.id and contract_2_condition.condition_id = condition_value.id " +
            //Added Gbpm category
            //" and condition_type = '" + ConditionCode.MEDIUM.getCode() + "') END as medium " + //Added rfdealId for DM-1185
            " and condition_type = '" + ConditionCode.MEDIUM.getCode() + "') END as medium, au.gbpm_category, c2a.rfdeal_id as rfdealId ,"+
            " a.id as assetId " + // Added for DM-284
            " from asset_use au " +
            // 6/2014: James said we do want ALL po's/contract's covering the asset for the CW here
            // i.e. not just the latest contract

            // Code Fix for SS Task 3
            //" left join purchase_order_2_asset po2a on po2a.asset_base_id = au.asset_id left join purchase_order po on po.id = po2a.purchase_order_id and po.cw_id = ? " +
            "left join asset a on a.id=au.asset_id " +
			" left join purchase_order_2_asset po2a on po2a.asset_base_id = a.id left join purchase_order po on po.id = po2a.purchase_order_id and po.cw_id = ? " +

			" left join contract_2_asset c2a on c2a.asset_base_id = au.asset_id left join contract c on c.id = c2a.contract_id and c.cw_id = ? " +
			" where au.id = ? and au.cw_id = ? ";

		if ("Y".equals(photoFlag)) {
			sql += "and ( (au.camera_copy_to_come is not null and au.camera_copy_to_come = 1) "
				+ "or (a.owner_type is not null and a.owner_type = '" + OwnerType.WORK_FOR_HIRE.getCode() + "')) ";
		}

		if(userGroupId>0){
			sql += "and au.user_group_id = ?";
		}

		//Modified for RightNow Ticket starts
				if(list != null){
			        if(list.get("flagCurrent") != null || list.get("flagPrevious") != null){
			        	log.debug("flagCurrent --> "+list.get("flagCurrent"));
			        	log.debug("flagPrevious --> "+list.get("flagPrevious"));
			        	if(list.get("flagPrevious") != null){
			        		log.debug("commonWorkId --> "+list.get("commonWorkId"));
			        		log.debug("assetUseId --> "+list.get("assetUseId"));
			        	}
			        }
				}

				if(list != null){
					if(list.get("flagCurrent") != null || list.get("flagPrevious") != null){
						sql += "and au.cw_id = c.cw_id group by au.id";
					}
				}
		//Modified for RightNow Ticket ends
		//log.debug("loadSummaryReportInfo(): sql: " + sql);

		try {
			ps = con.prepareStatement(sql);
			if(list != null && list.get("assetUseId") != null){
				if(list.get("assetUseId") != null && list.get("commonWorkId") != null){
					ps.setInt(1, Integer.parseInt(list.get("commonWorkId")));
					ps.setInt(2, Integer.parseInt(list.get("commonWorkId")));
					ps.setInt(3, Integer.parseInt(list.get("assetUseId")));
					ps.setInt(4, Integer.parseInt(list.get("commonWorkId")));
				}
			}else{
				ps.setInt(1, cwId);
				ps.setInt(2, cwId);
				ps.setInt(3, auId);
				ps.setInt(4, cwId);
			}
			if(userGroupId>0){
				ps.setInt(5, userGroupId);
			}
			ResultSet rs = ps.executeQuery();
			List<Map<String, String>> rows = new ArrayList<Map<String, String>>();

			while (rs.next()) {
				Map<String, String> info = new HashMap<String, String> ();
				info.put("requestComment", rs.getString(1));
				info.put("dateRequested", rs.getString(2));
				info.put("dateGranted", rs.getString(3));
				info.put("invoiceNumber", rs.getString(4));
				info.put("contractId", rs.getString(5));
				info.put("startDate", rs.getString(6));
				info.put("expirationDate", rs.getString(7));
				info.put("invoiceDate", rs.getString(8));
				info.put("estimatedCost", rs.getString(9));
				info.put("finalCost", rs.getString(10));
				info.put("language", rs.getString(11));
				info.put("territory", rs.getString(12));
				info.put("printRun", rs.getString(13));
				info.put("seats", rs.getString(14)); //Added to implement DM-122 and adjusted index accordingly
				info.put("derivatives", rs.getString(15));
				info.put("sublicense", rs.getString(16));
				info.put("editions", rs.getString(17));
				info.put("medium", rs.getString(18));
				//Added Gbpm category
				info.put("gbpmcategory", rs.getString(19));
				info.put("rfdealId", rs.getString(20));//Added for DM-1185
				info.put("assetId", rs.getString(21));// Added for DM-284
				rows.add(info);
			}
			timer.stopTimer();
			// Code Fix for SS Task 3
			if (rows.size() <= 0){
				Map<String, String> info = new HashMap<String, String> ();
				rows.add(info);
			}
			return rows.get(0);
		}
		finally {
			if (ps != null) ps.close();
			con.close();
		}
	}

		//Added for 'Report For Legal' - Start
		@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
		public Map<String, String> loadReportForLegalInfo(int cwId, int auId) throws SQLException
		{
			PerfTimer timer = monitor.startTimer("AssetUseRepository::loadSummaryReportInfo");
			Connection con = getDataSource().getConnection();
			PreparedStatement ps = null;

			String sql =
				"select c2a.approver_name, c2a.approver_remarks, au.request_comment, " +
				" DATE_FORMAT(po.created_date, '%c/%d/%Y') as pgroup, " +
				" DATE_FORMAT(IF(c.is_permission_form, c.created_date, c.start_date), '%c/%d/%Y') as cgroup, " +
				" c.number as invoiceNumber, c.id as contractId, DATE_FORMAT(c.start_date, '%c/%d/%Y') as startDate, " +
				" DATE_FORMAT(c.end_date, '%c/%d/%Y') as expirationDate," +
				" DATE_FORMAT(c.date, '%c/%d/%Y') as invoiceDate, au.estimated_cost as estimatedCost, " +
				" c.price as finalCost, " +
				" CASE when c.id is null then '' ELSE (select  rollup_value from contract_2_condition, condition_value " +
			    " where contract_2_condition.contract_id = c.id and contract_2_condition.condition_id = condition_value.id " +
	            " and condition_type = '" + ConditionCode.LANGUAGE.getCode() + "') END as language, " +
	            " CASE when c.id is null then '' ELSE (select  rollup_value from contract_2_condition, condition_value " +
	            " where contract_2_condition.contract_id = c.id and contract_2_condition.condition_id = condition_value.id " +
	            " and condition_type = '" + ConditionCode.SALES_TERRITORY.getCode() + "') END as territory,  " +
	            " CASE when c.id is null then '' ELSE (select  rollup_value from contract_2_condition, condition_value  " +
	            " where contract_2_condition.contract_id = c.id and contract_2_condition.condition_id = condition_value.id " +
	            " and condition_type = '" + ConditionCode.PRINT_RUN.getCode() + "') END as printRun, " +
	            " CASE when c.id is null then '' ELSE (select  rollup_value from contract_2_condition, condition_value " +
	            " where contract_2_condition.contract_id = c.id and contract_2_condition.condition_id = condition_value.id " +
	            " and condition_type = '" + ConditionCode.DERIVATIVE_WORKS.getCode() + "') END as derivativeWork, " +
	            " CASE when c.id is null then '' ELSE (select  rollup_value from contract_2_condition, condition_value " +
	            " where contract_2_condition.contract_id = c.id and contract_2_condition.condition_id = condition_value.id " +
	            " and condition_type = '" + ConditionCode.SUBLICENSE.getCode() + "') END as sublicense, " +
	            " CASE when c.id is null then '' ELSE (select  rollup_value from contract_2_condition, condition_value " +
	            " where contract_2_condition.contract_id = c.id and contract_2_condition.condition_id = condition_value.id " +
	            " and condition_type = '" + ConditionCode.EDITION.getCode() + "') END as editions, " +
	            " CASE when c.id is null then '' ELSE (select  rollup_value from contract_2_condition, condition_value " +
	            " where contract_2_condition.contract_id = c.id and contract_2_condition.condition_id = condition_value.id " +
	            " and condition_type = '" + ConditionCode.MEDIUM.getCode() + "') END as medium " +
	            " from asset_use au " +
	            "left join asset a on a.id=au.asset_id " +
				" left join purchase_order_2_asset po2a on po2a.asset_base_id = a.id left join purchase_order po on po.id = po2a.purchase_order_id and po.cw_id = ? " +
				" left join contract_2_asset c2a on c2a.asset_base_id = au.asset_id left join contract c on c.id = c2a.contract_id and c.cw_id = ? " +
				" where au.id = ? and au.cw_id = ? ";

			try {
				ps = con.prepareStatement(sql);
				ps.setInt(1, cwId);
				ps.setInt(2, cwId);
				ps.setInt(3, auId);
				ps.setInt(4, cwId);

				ResultSet rs = ps.executeQuery();
				List<Map<String, String>> rows = new ArrayList<Map<String, String>>();

				while (rs.next()) {
					Map<String, String> info = new HashMap<String, String> ();
					info.put("approverName", rs.getString(1));
					info.put("approverRemarks", rs.getString(2));
					info.put("requestComment", rs.getString(3));
					info.put("dateRequested", rs.getString(4));
					info.put("dateGranted", rs.getString(5));
					info.put("invoiceNumber", rs.getString(6));
					info.put("contractId", rs.getString(7));
					info.put("startDate", rs.getString(8));
					info.put("expirationDate", rs.getString(9));
					info.put("invoiceDate", rs.getString(10));
					info.put("estimatedCost", rs.getString(11));
					info.put("finalCost", rs.getString(12));
					info.put("language", rs.getString(13));
					info.put("territory", rs.getString(14));
					info.put("printRun", rs.getString(15));
					info.put("derivatives", rs.getString(16));
					info.put("sublicense", rs.getString(17));
					info.put("editions", rs.getString(18));
					info.put("medium", rs.getString(19));
					rows.add(info);
				}
				timer.stopTimer();
				if (rows.size() <= 0){
					Map<String, String> info = new HashMap<String, String> ();
					rows.add(info);
				}
				return rows.get(0);
			}
			finally {
				if (ps != null) ps.close();
				con.close();
			}
		}

		//Modified for RightNow Ticket starts
    	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
		public Map<String, String> loadPreviousContractDtls(int cwId, int auId) throws SQLException, PersistenceException {
    		log.debug("In loadPreviousContractDtls method ");
    		String isbn13 = "";
    		int assetId = 0;
    		Map<String, String> results = new HashMap<String, String>();
    		//final String sql = "select * from asset_use au join asset a on au.asset_id = a.id au.id = '"+auId+"'";
  			//TypedQuery<AssetUse> auQuery = entityManager.createQuery(sql, AssetUse.class);
			AssetUse auList = loadAssetUseById(auId);
				isbn13 = auList.getPickupISBN() != null  ? auList.getPickupISBN() : auList.getReusedISBN();
				assetId = auList.getAsset().getId();
				//assetUse.getImportSource() != null && assetUse.getImportSource().equals(ImportSource.FROM_CS_SPREADSHEET)
				log.debug("isbn13 --> "+isbn13);
				log.debug("assetId --> "+assetId);


			String contractToAssetSql = "select c.* from contract c right join contract_2_asset c2a on c.id = c2a.contract_id and c2a.asset_base_id = '"+assetId+"' where c.cw_id = '"+cwId+"'";
			log.debug("contractToAssetSql: " + contractToAssetSql);
			Query query = entityManager.createNativeQuery(contractToAssetSql, Contract.class);
			@SuppressWarnings("unchecked")
			List<Contract> contractToAssetList = query.getResultList();
			if(!contractToAssetList.isEmpty()){
				results.put("flagCurrent", "SUCCESS");
				log.debug("contractToAssetList ");
				return results;
			}

			if(StringUtils.isNotBlank(isbn13) && auList.getImportSource() != null && auList.getImportSource().equals(ImportSource.FROM_CS_SPREADSHEET)){
				log.debug("NOT BLANK");
				int imported_cw_id = getCwIdForIsbn(isbn13);
				String contractSql = "select c.* from contract c right join contract_2_asset c2a on c.id = c2a.contract_id and c2a.asset_base_id = '"+assetId+"' where  c.cw_id = '"+imported_cw_id+"'";
				Query preContractQuery = entityManager.createNativeQuery(contractSql, Contract.class);
				//TypedQuery<Contract> contractDtlsQuery = entityManager.createQuery(contractSql, Contract.class);
				@SuppressWarnings("unchecked")
				List<Contract> contractList = preContractQuery.getResultList();
				if(!contractList.isEmpty()){
					log.debug("contractList");
					List<Integer> assetUseIds = loadAssetUseIdsForAssetIdAndCwId(assetId,imported_cw_id);
					//String assetUseSql = "select id from asset_use where asset_id = '"+assetId+"' and cw_id '"+imported_cw_id+"'";
					//TypedQuery<AssetUse> assetUseDtlsQuery = entityManager.createQuery(assetUseSql, AssetUse.class);
					//List<AssetUse> assetUseDtlsList = assetUseDtlsQuery.getResultList();
					for (Integer aid : assetUseIds) {
						results.put("flagPrevious", "SUCCESS");
						results.put("assetUseId", aid.toString());
					}
					results.put("commonWorkId", imported_cw_id + "");
					log.debug("imorted_cw_id--> "+imported_cw_id + "");
					return results;
				}
				//Here we have to send Asset Use Id and Common Work Id of the imported Asset
			}
			return null;
		}

    	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
    	public Integer getCwIdForIsbn(String isbn) throws SQLException {
    		String sql = "select cw_id as id from product where isbn13 = ?";
    		Query query = entityManager.createNativeQuery(sql, "scalarId");
    		query.setParameter(1, isbn);
    		try {
    			return (Integer) query.getSingleResult();
    		}
    		catch (NoResultException ex) {
    			return null;
    		}
       	}

		//Modified for RightNow Ticket Ends


		@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
		public List<Object[]> reportLegalAssetUseDetail(int cwId) throws Exception
		{
			Query q = entityManager.createNativeQuery(
					"select AU.id, AU.cw_id from contract C join contract_2_asset C2A on C.id = C2A.contract_id left outer join asset A on A.id = C2A.asset_base_id "+
					"left outer join asset_use AU on AU.asset_id = A.id where C2A.approver_name is not null and C2A.approver_remarks is not null and AU.cw_id =?");

			q.setParameter(1, cwId);
			@SuppressWarnings("unchecked")
			List<Object[]> results = q.getResultList();

			return results;
		}
		//Added for 'Report For Legal' - End

	// lnagy - NOT USED ANYMORE
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public Map<String, String> loadAssetPermissionInfo (String assetId, String assetUseId, String permCode)
	{
		Map<String, String> info = new HashMap<String, String> ();
		Query query = null;

		if (null == permCode) {
			info.put("restrictions", null);
		} else if (null != permCode && PermissionStatus.isOutOfCompliance(permCode)) {
			try {
				AssetUse au = find(AssetUse.class, assetUseId);
				info.put("restrictions", (null != au ? au.getStatusExplanation() : null));
			} catch (Exception e) {}
		} else {
			// "scalarGroup" is defined in an annotation at the top of the AssetUse class.
			query = entityManager.createNativeQuery(
					/*
					"select group_concat(condt.description || ':' || cond.rollup_value) as cgroup " +
					"from condition_value cond join condition_type condt on condt.code=cond.condition_type " +
					"where rollup_value is not null and cond.id in (select condition_id from contract_2_condition where contract_id " +
					"in (select contract_id from contract_2_asset where asset_base_id=?))",
					*/
					"select group_concat(cgroup) as cgroup from (" +
					"		select group_concat(condt.description || ':' || cond.rollup_value) as cgroup " +
					"		from condition_value cond join condition_type condt on condt.code=cond.condition_type " +
					"		join contract_2_condition c2c on cond.id = c2c.condition_id " +
					"		join contract_2_asset c2a on c2c.contract_id = c2a.contract_id and c2a.asset_base_id = ?" +
					"		where rollup_value is not null" +
					"		union " +
					"		select c2a.credit_line as cgroup from contract_2_asset c2a where c2a.asset_base_id =?" +
					"		union " +
					"		select u2s.usage_condition_code || ':' || u2s.size_code as cgroup from usage_2_size u2s where u2s.asset_base_id =?" +
					"		) a",
				"scalarGroup");
			query.setParameter(1, assetId);
			query.setParameter(2, assetId);
			query.setParameter(3, assetId);
			String groupValue = (String) query.getSingleResult();

			info.put("restrictions", groupValue);
		}

		// "scalarPrice" is defined in an annotation at the top of the AssetUse class.
		query = entityManager.createNativeQuery(
				"select  FORMAT((c.price / count(c2a.asset_base_id)),2) as dprice from contract c join contract_2_asset c2a on c.id = c2a.contract_id " +
				"where c.id in (select contract_id from contract_2_asset where asset_base_id=?)",
			"scalarPrice");
		query.setParameter(1, assetId);
		Object groupPrice = query.getSingleResult();
		info.put("price", (null != groupPrice ? groupPrice.toString() : ""));

		// "scalarPrice" is defined in an annotation at the top of the AssetUse class.
		query = entityManager.createNativeQuery(
				"select SUM(num_copies) as cgroup from comp_copy cc where cc.contract_id in " +
				"(select contract_id from contract_2_asset where asset_base_id=?)",
			"scalarGroup");
		query.setParameter(1, assetId);
		Object compCopies = query.getSingleResult();
		info.put("compCopies", (null != compCopies ? compCopies.toString() : ""));

		return info;
	}

	/**
	 * Saves the status in the ref table, also saves the Po_ID and Contract_ID that were used to generate the status
	 * @param auId
	 * @param sourceId
	 * @param poId
	 * @param contractId
	 * @param permissionStatus
	 * @param explanation
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void saveAssetPermissionRefStatus(AssetUse au, Integer sourceId, Integer poId, Integer contractId,
			PermissionStatus permissionStatus, String explanation) {
		int auId = au.getId();
		log.debug ("saveAssetPermissionRefStatus(): auId " + auId + ", sourceId " + sourceId + ", poId " + poId
				+ ", contractId " + contractId + ", permissionStatus " + permissionStatus);
		// TODO might need to add status per PurchaseOrder/Contract when implementing re-request
		String sql = "select * from ASSET_PERM_REF au where au.asset_use_id = ? and " +
					((sourceId != null) ? "au.source_id = ?" : "au.source_id is null");
		log.debug("saveAssetPermissionRefStatus(): sql: " + sql);
		Query query = entityManager.createNativeQuery(sql, AssetPermissionRef.class);
		query.setParameter(1, auId);
		if (null != sourceId)
			query.setParameter(2, sourceId);
		@SuppressWarnings("unchecked")
		// smarkoff: We expect either 1 or 0 results (auId + sourceId is a unique combination)
		List<AssetPermissionRef> aprs = query.getResultList();

		PurchaseOrder po = null;
		Contract contract = null;
		if (poId != null && poId != -1) {
			po = new PurchaseOrder();
			po.setId(poId);
		}
		if (contractId != null && contractId != -1) {
			contract = new Contract();
			contract.setId(contractId);
		}

		// smarkoff: again, there there is only going to be 0 or 1 element in the list
		for (AssetPermissionRef apr : aprs) {
			apr.setPurchaseOrder(po);
			apr.setContract(contract);
			apr.setPermissionStatus(permissionStatus);
			apr.setStatusExplanation(explanation);
			entityManager.merge(apr);
		}

		// smarkoff: before this method did nothing if aprs.isEmpty() -- it does seem this method
		// needs to cover this case otherwise if you recalculate statuses for a cw and the ref table
		// doesn't already have entries, it won't get any new records added
		// (although it's generally the case that it does already have entries - but not sure of the
		// scenarios where it may or may not already have entries)
		if (aprs.isEmpty()) {
			AssetPermissionRef apr = new AssetPermissionRef();
			apr.setAssetUse(au);
			apr.setAsset(au.getAsset());
			apr.setCommonWork(au.getCommonWork());
			if (sourceId != null) {
				Source source = new Source();
				source.setId(sourceId);
				apr.setSource(source);
			}
			apr.setPurchaseOrder(po);
			apr.setContract(contract);
			apr.setPermissionStatus(permissionStatus);
			apr.setStatusExplanation(explanation);
			entityManager.persist(apr);
		}
	}

	/**
	 * method called when an asset use is saved: will compare the sources to know if needs to update all asset_uses
	 * or just one
	 * @param auId
	 * @throws Exception
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void saveAssetPermissionRef (int auId) throws Exception {
		AssetUse au = lazyLoad (AssetUse.class, auId, new String[] {"commonWork", "asset"});
		log.debug("au.getStatusExplanation()1 --> "+au.getStatusExplanation());
		log.debug("au.getStatus()1 --> "+au.getStatus());

		int assetId = au.getAsset().getId();
		log.debug("saveAssetPermissionRef(): auId: " + au.getId() + " assetId: " + assetId + " cwId: " + au.getCommonWork().getId());

		// first thing we compare sources
		Query query = entityManager.createNativeQuery("select distinct source_id as id from ASSET_2_SOURCE where asset_id = ?", "scalarId");
		query.setParameter(1, assetId);
		@SuppressWarnings("unchecked")
		List<Integer> newSourceIds = query.getResultList();
		if (CollectionUtils.isEmpty(newSourceIds))
			newSourceIds.add(null);

		query = entityManager.createNativeQuery("select distinct source_id as id from ASSET_PERM_REF au where asset_id = ?", "scalarId");
		query.setParameter(1, assetId);
		@SuppressWarnings("unchecked")
		List<Integer> oldSourceIds = query.getResultList();

		if (CollectionUtils.isEqualCollection(newSourceIds, oldSourceIds)) {
			log.debug ("saveAssetPermissionRef(): sources have NOT changed ");
			saveAssetPermissionRef (au, newSourceIds);
			log.debug("au.getStatusExplanation()2 --> "+au.getStatusExplanation());
			log.debug("au.getStatus()2 --> "+au.getStatus());
		} else {
			log.debug ("saveAssetPermissionRef(): sources have changed ");
			saveAssetPermissionRef (au.getAsset(), newSourceIds);
			log.debug("au.getStatusExplanation()3 --> "+au.getStatusExplanation());
			log.debug("au.getStatus()3 --> "+au.getStatus());
		}
	}

	/**
	 * Updates entries for asset use
	 * @param au
	 * @param newSourceIds
	 * @throws Exception
	 */
	private void saveAssetPermissionRef (AssetUse au, List<Integer> newSourceIds) throws Exception {
		log.debug ("saveAssetPermissionRef(): auId: " + au.getId());
		// we don't actually need to lazyLoad everything here - saveAssetPermissionRefInner will lazyLoad more
		au = lazyLoad (AssetUse.class, au.getId(), new String[] {"commonWork"});

		int assetId = au.getAsset().getId();
		log.debug ("saveAssetPermissionRef(): auId: " + au.getId() + " assetId: " + assetId + " cwId: " + au.getCommonWork().getId());

		// delete the relations
		Query query = entityManager.createNativeQuery("delete from ASSET_PERM_REF where asset_use_id = ?");
		query.setParameter(1, au.getId());
		query.executeUpdate();

		saveAssetPermissionRefInner(au.getAsset(), newSourceIds, au);
	}

	/**
	 * Updates entries for Asset, that means all usages
	 * @param asset
	 * @param newSourceIds
	 * @throws Exception
	 */
	private void saveAssetPermissionRef (Asset asset, List<Integer> newSourceIds) throws Exception {
		log.debug ("saveAssetPermissionRef(): assetId: " + asset.getId() + ", newSourceIds = " + StringUtils.join(newSourceIds, ", "));

		// delete the relations (all asset uses)
		Query query = entityManager.createNativeQuery("delete from ASSET_PERM_REF where asset_id = ?");
		query.setParameter(1, asset.getId());
		query.executeUpdate();

		List<Integer> auIds = loadAssetUseIdsForAssetId(asset.getId());
		for (Integer auId : auIds) {
			// we don't actually need to lazyLoad everything here - saveAssetPermissionRefInner will lazyLoad more
			AssetUse au = lazyLoad (AssetUse.class, auId, new String[] {"commonWork"});
			saveAssetPermissionRefInner(asset, newSourceIds, au);
		}
	}

	private void saveAssetPermissionRefInner(Asset asset, List<Integer> newSourceIds, AssetUse use) {
		// add the new ones
		for (Integer sourceId : newSourceIds) {
			log.debug("saveAssetPermissionRefInner(): add sourceId " + sourceId);
			AssetPermissionRef apr = new AssetPermissionRef();
			apr.setAsset(asset);
			apr.setAssetUse(use);
			apr.setPermissionStatus(use.getStatus());
			apr.setStatusExplanation(use.getStatusExplanation());
			apr.setCommonWork(use.getCommonWork());

			if (sourceId != null) {
				Source source = new Source();
				source.setId(sourceId);
				apr.setSource(source);

				if (use.getSourceStatuses() != null) {
					for (AuSourcePermStatus status : use.getSourceStatuses()) {
						if (sourceId.equals(status.getSourceId())) {
							if (status.getLatestContractId() != null) {
								Contract contract = new Contract();
								contract.setId(status.getLatestContractId());
								apr.setContract(contract);
							}
							if (status.getLatestPoId() != null) {
								PurchaseOrder po = new PurchaseOrder();
								po.setId(status.getLatestPoId());
								apr.setPurchaseOrder(po);
							}
						}
					}
				}
			}
			entityManager.merge(apr);
		}
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void updateDateToProduction (Date dateToProd, int cwId, Integer[] componentIds,Integer userGroupId) {
		//https://www.pivotaltracker.com/story/show/56833716
		//if asset is Camera Copy to Come do not tag with date. Do not overwrite date if date already exists
		boolean orNull = false;
		if (Arrays.asList(componentIds).contains(0)) {
			orNull = true;
		}

		String sql = "update asset_use set date_to_prod=?, sent_to_production=1 where cw_id = ? ";
		if (!orNull) {
			sql += "and component_id in (" + StringUtil.arrayToString(componentIds, ",") + ") ";

		}
		sql += "and date_to_prod is null and camera_copy_to_come=0 ";

		if (!userGroupId.equals(new Integer(0))) {
			sql =  sql + " and user_group_id = " + userGroupId;
		}

		log.debug("updateDateToProduction(): dateToProd = " + dateToProd + ", cwId = " + cwId + ", sql = " + sql);

		Query query = entityManager.createNativeQuery(sql);

		query.setParameter(1, dateToProd);
		query.setParameter(2, cwId);
		int rows = query.executeUpdate();
		log.debug("updateDateToProduction(): #rows updated = " + rows);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void assignUserGroupToAu (Integer assetUseId, Integer userGroupId) {
		Query query = entityManager.createNativeQuery("update asset_use set user_group_id=? where id = ? ");
		query.setParameter(1, userGroupId);
		query.setParameter(2, assetUseId);
		query.executeUpdate();
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public List<AssetUseFile> loadAssetUseFiles(Integer auId) {
		TypedQuery<AssetUseFile> query = entityManager.createQuery(
				"from AssetUseFile auf where auf.assetUse.id = ?", AssetUseFile.class);

		query.setParameter(1, auId);
		List<AssetUseFile> auList = query.getResultList();
		return auList;
	}

	/**
	 * for now just save the new ones. We might have to add logic to delete the removed ones
	 * @param auId
	 * @param files
	 * @throws PersistenceException
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public void saveAssetUseFiles(int auId, List<AssetUseFile> files) throws PersistenceException {
		for (AssetUseFile auf : files) {
			// save only the new ones (it's possible some ids are negative (from ui) so we set them to be null)
			if (null == auf.getId() || 0 >= auf.getId()) {
				auf.setId(null);
				AssetUse au = new AssetUse();
				au.setId(auId);
				auf.setAssetUse(au);
				// if one fails, let them all fail
				merge (auf);
			}
		}
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public boolean doesAssetUseHaveFiles(int auId) {
		final String sql = "select count(*) as count from asset_use_file where au_id = ?";
		Query query = entityManager.createNativeQuery(sql, "scalarCount");
		query.setParameter(1, auId);
		// For count, DB2 and MS SQL Server return Integer but MySQL returns BigInteger
		// Integer and BigInteger inherit from Number
		Number result = (Number) query.getSingleResult();

		return result.intValue() > 0;
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public boolean doesAssetHaveFiles(int assetId) {
		final String sql = "select count(*) as count from asset_file where asset_id = ?";
		Query query = entityManager.createNativeQuery(sql, "scalarCount");
		query.setParameter(1, assetId);
		// For count, DB2 and MS SQL Server return Integer but MySQL returns BigInteger
		// Integer and BigInteger inherit from Number
		Number result = (Number) query.getSingleResult();

		return result.intValue() > 0;
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public boolean doesLatestContractHaveFile(int auId) {
		// In most cases an AssetUse will only have a single latest contract but
		// it can have more than one if the Asset has more than one source.
		// We want to return true as long as at least one of the latest contracts
		// has a file.
		final String sql = "select count(*) as count from contract_file cf, au_source_perm_status asps"
			+ " where asps.asset_use_id = ? and asps.latest_contract_id = cf.contract_id";

		Query query = entityManager.createNativeQuery(sql, "scalarCount");
		query.setParameter(1, auId);
		// For count, DB2 and MS SQL Server return Integer but MySQL returns BigInteger
		// Integer and BigInteger inherit from Number
		Number result = (Number) query.getSingleResult();

		return result.intValue() > 0;
	}

	/**
	 * This method may return multiple contract ids if the Asset has multiple sources.
	 * Otherwise only one id at most will be returned.
	 * Will not return any null ids.
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<Integer> getLatestContractIds(int auId) {
		String sql = "select latest_contract_id as id from au_source_perm_status where asset_use_id = ? and latest_contract_id is not null";
		Query query = entityManager.createNativeQuery(sql, "scalarId");
		query.setParameter(1, auId);
		@SuppressWarnings("unchecked")
		List<Integer> list = query.getResultList();
		return list;
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<Map<String, String>> loadSourceDetailReportView (String selections) throws SQLException
	{
		if (StringUtils.isBlank(selections)) {
			return null;
		}

		final String sql =
			"select distinct src.name,  prod.isbn13,  prod.title, c.number as invoice_no, comp.name component,u.description usage_type, " +
			" au.position, au.manuscript_page,  a.media_type, a.description asset_description, a.credit_line, a.vendor_id source_ref,au.final_page, " +
			// Query updated for DM-1606
			" case when au.permission_status like 'formSent' and po.is_permission_Request=false then 'Waiting on Invoice' else au.permission_status END as  permission_status, "+
			"DATE_FORMAT(c.date, '%c/%d/%Y') invoice_date, DATE_FORMAT(c.start_date, '%c/%d/%Y') permission_date, " +
			"CASE WHEN au.reused_from_prev_ed = 1 THEN 'Yes' ELSE 'No' END as isReused, " +
			" FORMAT(au.estimated_cost,'###,###.##')  estimated_cost, FORMAT(c.price, '###,###.##') final_cost, " +
			"CASE WHEN a.is_royalty_free = 1 THEN 'Yes' ELSE 'No' END as isRoyaltyFree, " +
			"CASE WHEN au.is_pickup = 1 THEN 'Yes' ELSE 'No' END as isPickup, " +
			"au.permission_comment, prod.business_unit, pl.code, " +
			"prod.pub_status,au.asset_id " +
			"from contract c, contract_2_asset c2a, asset a, product prod, product_line pl, source src, asset_use au " +
			// Start : Query updated for DM-1606
			"left join purchase_order_2_asset po2a on po2a.asset_base_id =au.asset_id "+
			"left join PURCHASE_ORDER po on po2a.purchase_order_id =po.id "+
			// End : Query updated for DM-1606
			"left join component comp on au.component_id = comp.id  " +
			"left join au_source_perm_status aps on aps.asset_use_id = au.id, " +
			"	media_type mt, usage_type u, source s, permission_status ps,  " +
			"	(select max(c1.date) as max_date,  c2a1.asset_base_id from contract c1, contract_2_asset c2a1 " +
			"	where c1.id = c2a1.contract_id group by c2a1.asset_base_id) d " +
			"where c.id = c2a.contract_id  and c2a.asset_base_id = d.asset_base_id " +
			"      and a.id = c2a.asset_base_id and au.asset_id = a.id  " +
			"      and au.usage_type = u.code and a.media_type = mt.code  " +
			"      and c.source_id = s.id and ps.code = aps.permission_status " +
			"      and  prod.cw_id =  c.cw_id  and prod.is_cw_primary = 1 " +
			"      and  pl.id =   prod.product_line_id   " +
			"      and src.id = c.source_id " + selections;

		log.debug("loadSourceDetailReportView(): sql:\r\n" + sql);
		Connection con = getDataSource().getConnection();
		PreparedStatement ps = null;

		try {
			ps = con.prepareStatement(sql);
			ResultSet rs = ps.executeQuery();

			List<Map<String, String>> rows = new ArrayList<Map<String, String>>();

			while (rs.next()) {
				Map<String, String> info = new HashMap<String,String>();
				info.put("SourceName", rs.getString(1));
				info.put("Isbn", rs.getString(2));
				info.put("Title", rs.getString(3));
				info.put("Invoice", rs.getString(4));
				info.put("Component", rs.getString(5));
				info.put("Usage", rs.getString(6));
				info.put("Position", rs.getString(7));
				info.put("ManPage", rs.getString(8));
				info.put("MediaType", rs.getString(9));
				info.put("AssetDescription", rs.getString(10));
				info.put("CreditLine", rs.getString(11));
				info.put("SourceRef", rs.getString(12));
				info.put("FinalPage", rs.getString(13));
				info.put("PermissionStatus", rs.getString(14));
				info.put("InvocieDate", rs.getString(15));
				info.put("PermissionDate", rs.getString(16));
				info.put("Reused", rs.getString(17));
				info.put("EstimatedCost", rs.getString(18));
				info.put("FinalCost", rs.getString(19));
				info.put("RoyaltyFree", rs.getString(20));
				info.put("Pickup", rs.getString(21));
				info.put("Comment", rs.getString(22));
				info.put("BusinessUnit", rs.getString(23));
				info.put("ProductLine", rs.getString(24));
				info.put("PubStatus", rs.getString(25));
				info.put("AssetId", rs.getString(26));

				rows.add(info);
			}
			return rows;

		}
		finally {
			if (ps != null) ps.close();
			con.close();
		}
	}

	/*
	 * Added for Creative Services Spreadsheet import Activity
	 * To Check for the Duplicate entries in the System by Filtering
	 * With ISBN,VendorID,Source Name,Contract Date.
	 */
	public int loadAssetBySourceVendorIdForCS(int sourceId, String vendorId,String isbn, String cNumber) {

/*		List<Source> sources = executeMultiResultNamedQuery("Source.findSourceByNameOrExternalId", new Object[] {sourceId, sourceId});
		// if no source found by name or externalId
		if (CollectionUtils.isEmpty(sources)) {
			return 0;
		}
		List<Integer> ids = new ArrayList<Integer> ();
		for (Source s : sources) {
			ids.add(s.getId());
		}*/

		String sql = "select count(asset.id) as count "+
					 "from asset join asset_base on asset.id = asset_base.id "+
					 "join asset_use on asset.id = asset_use.asset_id "+
					 "join asset_2_source a2s on a2s.asset_id = asset.id "+
				     "join source on source.id = a2s.source_id "+
				     "join product on product.cw_id = asset_use.cw_id "+
				     "join contract_2_asset c2a on c2a.asset_base_id = asset.id "+
				     "join contract c on c.id = c2a.contract_id "+
                     "where source.id = ? and asset.vendor_id=? and c.number = ? and ";

		if (isbn.trim().length() == 13) {
			sql = sql + "product.isbn13 = ? ";
		} else {
			sql = sql + "product.isbn10 = ? ";
		}
		log.debug (sql);

		Query query = entityManager.createNativeQuery(sql, "scalarCount");
	/** Changes for CS Spreadsheet Starts */
		query.setParameter(1, sourceId);
		query.setParameter(2, vendorId);
		query.setParameter(3, cNumber);
		query.setParameter(4, isbn);
		/** Changes for CS Spreadsheet Ends */
		// For count, DB2 and MS SQL Server return Integer but MySQL returns BigInteger
		// Integer and BigInteger inherit from Number
		Number result = (Number) query.getSingleResult();

		return result.intValue();
    }

	/*Added for Incident INC_60293*/
	@SuppressWarnings("unchecked")
	@Transactional(propagation = Propagation.REQUIRED, readOnly=true)
	public AssetUse loadPreviousAssetUse(String assetid) throws PersistenceException
	{
		log.debug("loadPreviousAssetUse(): entered ");
		try {
			String sql = "select * from asset_use where asset_id = ? order by created_date desc LIMIT 1 ";
			
			log.debug("loadPreviousAssetUse(): sql: " + sql + "with assetid: " + assetid );
			Query  query = entityManager.createNativeQuery(sql, AssetUse.class);

			query.setParameter(1, assetid);
			List<AssetUse> result = query.getResultList();

			if (result.size() > 0) {
				// return only the first one
				AssetUse test = result.get(0);
				return test;
			} else {
				log.debug("loadAssetUseByUsage(): failed to load ");
				return null;
			}
		}
		catch (NoResultException e) {
			log.debug("loadAssetUseByUsage(): failed to load ");
			return null;
		}
	}
	/*End Added for Incident INC_60293*/
	
	/*Added for Inc_96553  */
	
	@SuppressWarnings("unchecked")
	@Transactional(propagation = Propagation.REQUIRED, readOnly=true)
	public AssetUse loadAssetUseforAssetId(Integer cwid, Integer sourceId) throws PersistenceException
	{
		log.debug("loadAssetUseforAssetId(): entered ");
		try {
			String sql = "select * from asset_use au join asset_2_source a2s where au.asset_id=a2s.asset_id and au.cw_id=? and a2s.source_id=? ";
			
			log.debug("loadAssetUseforAssetId(): sql: " + sql + "with cw: " + cwid );
			Query  query = entityManager.createNativeQuery(sql, AssetUse.class);

			query.setParameter(1, cwid);
			query.setParameter(2, sourceId);
			List<AssetUse> result = query.getResultList();

			if (result.size() > 0) {
				// return only the first one
				AssetUse test = result.get(0);
				log.debug("Asset ID------>"+test.getAsset().getId());
				return test;
			} else {
				log.debug("loadAssetUseforAssetId(): failed to load ");
				return null;
			}
		}
		catch (NoResultException e) {
			log.debug("loadAssetUseforAssetId(): failed to load ");
			return null;
		}
	}
	
	/* End Added */
	
	/*Added for INC_126115 - Paperwork and Contract Corruptions within GPS  */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<Condition> loadQuizConditionValueForAssetId(int assetId,int cwId) {
		
        final String sql = "select cv.* from contract_2_asset c2a,contract c,contract_2_condition c2c,condition_value cv "
                           + "where c.id = c2a.contract_id and c2c.contract_id = c.id and c2c.condition_id = cv.id and c.cw_id = ? and c2a.asset_base_id = ? "
                          // + "and cv.condition_type = '" + ConditionCode.EDITION.getCode() + "'";
        				   +"and cv.condition_type in('medium','sales','language','print_run','edition','dwork','sublicense')";
		Query query = entityManager.createNativeQuery(sql, Condition.class);

		query.setParameter(1, cwId);
		query.setParameter(2, assetId);
		try{
			@SuppressWarnings("unchecked")
			List<Condition> valueList = query.getResultList();
			return valueList;
		}
		catch(NoResultException ex){
			return null;
		}
	}
	/* End Added for INC_126115 - Paperwork and Contract Corruptions within GPS*/

	// Start : Added for DM-284
	@Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
	public boolean isMultiUsageAsset(Integer assetId, Integer cw_id) {
		final String sql = "select count(au.cw_id) as count from asset_use au where au.asset_id = ? and au.cw_id=?";
		Query query = entityManager.createNativeQuery(sql, "scalarCount");
		query.setParameter(1, assetId);
		query.setParameter(2, cw_id);
		// For count, DB2 and MS SQL Server return Integer but MySQL returns BigInteger
		// Integer and BigInteger inherit from Number
		Number result = (Number) query.getSingleResult();
		log.debug("isMultiUsageAsset() result : "+result+" result.intValue() > 1 :"+(result.intValue() > 1));
		return result.intValue() > 1;
	}
	// End : Added for DM-284

	public CommonWorkRepository getCommonWorkRepository() {
		return cwRepository;
	}
	
	public void setCommonWorkRepository(CommonWorkRepository cwRepository) {
		this.cwRepository = cwRepository;
	}

	public CommonWorkService getCommonWorkService() {
		return cwService;
	}
	
	public void setCommonWorkService(CommonWorkService cwService) {
		this.cwService = cwService;
	}

	public AssetRepository getAssetRepository() {
		return assetRepository;
	}
	
	public void setAssetRepository(AssetRepository assetRepository) {
		this.assetRepository = assetRepository;
	}

	public DataSource getDataSource() {
		return dataSource;
	}
	
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}
}
