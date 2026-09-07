package com.wiley.permissions.repositories;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.wiley.permissions.domain.persistence.permissions.Account;
import com.wiley.permissions.domain.persistence.permissions.Asset;
import com.wiley.permissions.domain.persistence.permissions.BusinessUnit;
import com.wiley.permissions.domain.persistence.permissions.ComponentCategory;
import com.wiley.permissions.domain.persistence.permissions.Condition;
import com.wiley.permissions.domain.persistence.permissions.ConditionType;
import com.wiley.permissions.domain.persistence.permissions.ConditionType.ConditionCode;
import com.wiley.permissions.domain.persistence.permissions.Contract;
import com.wiley.permissions.domain.persistence.permissions.ContractAsset;
import com.wiley.permissions.domain.persistence.permissions.ContractAssetPK;
import com.wiley.permissions.domain.persistence.permissions.ContractFile;
import com.wiley.permissions.domain.persistence.permissions.ContractFileName;
import com.wiley.permissions.domain.persistence.permissions.ContractList;
import com.wiley.permissions.domain.persistence.permissions.MasterAgreementDeal;
import com.wiley.permissions.domain.persistence.permissions.MediaType;
import com.wiley.permissions.domain.persistence.permissions.PaymentRequest;
import com.wiley.permissions.domain.persistence.permissions.PurchaseOrder;
import com.wiley.permissions.domain.persistence.permissions.RoyaltyFreeDeal;
import com.wiley.permissions.domain.persistence.permissions.UploadedDocumentsDetails;
import com.wiley.permissions.domain.persistence.permissions.UserGroup;
import com.wiley.permissions.persistence.PersistenceException;
import com.wiley.permissions.persistence.repository.JPARepository;
import com.wiley.permissions.services.ProductIndexService;
import com.wiley.permissions.services.view.POAssetView;
import com.wiley.sf.common.monitor.PerformanceMonitor.PerfTimer;

/**
 *
 */
public class ContractRepository extends JPARepository {
	private static final Log log = LogFactory.getLog(ContractRepository.class);

	private AssetUseRepository assetUseRepository;
	private CommonWorkRepository cwRepository;
	private ProductIndexService productIndexService;

	private DataSource dataSource;

	/**
	 * Return the newest contract that applies to the given asset, source, and cw.
	 * Wraps the SINGLE contract in a list to make easier to use by the rule engine.
	 * An empty list is returned if there is no contract (so return list size is 0 or 1).
	 * The returned contract may be from a previous edition if it has a future edition grant
	 * and is the latest contract.
	 * @throws IOException
	 * @throws ParseException
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public ContractList loadLatestForAssetSourceCW(int assetId, int sourceId, int cwId) throws PersistenceException, ParseException, IOException
	{
		ContractList latest = loadForAssetSourceCW(assetId, sourceId, cwId, true);

		Integer previousCwId = productIndexService.getPreviousEditionByCwId(cwId);  // throws ParseException, IOException

		// This could be made little more efficient but I'm not going to worry about it
		// much because only 10% of products have a previous edition.
		while (previousCwId != null) {
			ContractList list2 = loadForAssetSourceCW(assetId, sourceId, previousCwId, true);
			if (list2.size() > 0) {
				if (latest.size() == 0 || list2.get(0).getDate().after(latest.get(0).getDate())) {
					if (doesContractHaveFutureEditionGrant(list2.get(0))) {
						latest = list2;
					}
				}
			}
			previousCwId = productIndexService.getPreviousEditionByCwId(previousCwId);
		}

		return latest;
	}

	/**
	 * If you all you need is the latest contract, call the above method instead for efficiency.
	 * Sorts the list by date descending.
	 * The list may contain a contract from a previous edition if it has a future edition grant
	 * and is the latest contract.
	 * @throws IOException
	 * @throws ParseException
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public ContractList loadListForAssetSourceCW(int assetId, int sourceId, int cwId) throws PersistenceException, ParseException, IOException
	{
		ContractList list = loadForAssetSourceCW(assetId, sourceId, cwId, false);
		Contract latest = list.size() > 0 ? list.get(0) : null;
		boolean latestIsPrevious = false;

		Integer previousCwId = productIndexService.getPreviousEditionByCwId(cwId);  // throws ParseException, IOException

		// This could be made little more efficient but I'm not going to worry about it
		// much because only 10% of products have a previous edition.
		while (previousCwId != null) {
			ContractList list2 = loadForAssetSourceCW(assetId, sourceId, previousCwId, true);
			if (list2.size() > 0) {
				if (latest == null || list2.get(0).getDate().after(latest.getDate())) {
					if (doesContractHaveFutureEditionGrant(list2.get(0))) {
						latest = list2.get(0);
						latestIsPrevious = true;
					}
				}
			}
			previousCwId = productIndexService.getPreviousEditionByCwId(previousCwId);
		}

		if (latestIsPrevious) {
			list.add(0, latest);
		}
		return list;
	}

	/**
	 * Sorts the list by date desc (so either get latest contract or list with latest first).
	 */
	private ContractList loadForAssetSourceCW(int assetId, int sourceId, int cwId, boolean latestOnly) throws PersistenceException
	{
		PerfTimer timer = monitor.startTimer("ContractRepository::loadForAssetSourceCW::latestOnly-" + latestOnly);
		try {
			TypedQuery<Contract> query = entityManager.createQuery(
					"select c from Contract c, ContractAsset ca where c.commonWork.id = ?" +
					" and c.source.id = ? and ca.contract.id = c.id and ca.assetBase.id = ?"
					+ " order by c.date desc, c.id desc",
					// a lot of the contract dates are truncated to just day (not hours, etc)
					// so sort by "id desc" also
					Contract.class
			);

			query.setParameter(1, cwId);
			query.setParameter(2, sourceId);
			query.setParameter(3, assetId);
			if (latestOnly) {
				query.setMaxResults(1);
			}
			List<Contract> list = query.getResultList();
			ContractList contractList = new ContractList();
			contractList.addAll(list);
			return contractList;
		}
		finally {
			timer.stopTimer();
		}
	}

	/**
	 * Returns Contracts ordered by contract.date descending (newest first).
	 * Note if the asset has more than one source then just taking the first
	 * contract from this list only give you the "latest" contract from a
	 * single source.
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public List<Contract> loadListForAssetCW(int assetId, int cwId) throws PersistenceException
	{
		TypedQuery<Contract> query = entityManager.createQuery(
			"select c from Contract c, ContractAsset ca where c.commonWork.id = ?" +
			" and ca.contract.id = c.id and ca.assetBase.id = ? order by c.date desc",
			Contract.class
		);

		query.setParameter(1, cwId);
		query.setParameter(2, assetId);
		return query.getResultList();
	}

	public boolean contractContainsExternalAssets(int contractId) {
		// James says to use component_category instead of usage_type to determine 'Cover'
		final String sql = "select count(*) as count from asset_use au, component c where asset_id in (" +
			" select asset_id from contract where id = ? " +
			") and au.cw_id in (select cw_id from contract where id = ?) and " +
			"au.component_id = c.id and c.category = '" + ComponentCategory.COVER.getCode() + "'";
		Query query = entityManager.createNativeQuery(sql);
		query.setParameter(1, contractId);
		query.setParameter(2, contractId);
		// For count, DB2 and MS SQL Server return Integer but MySQL returns BigInteger
		// Integer and BigInteger inherit from Number
		Number result = (Number) query.getSingleResult();

		return result.intValue() > 0;
	}

	public boolean contractContainsInternalAssets(int contractId) {
		// James says to use component_category instead of usage_type to determine 'Cover'
		final String sql = "select count(*) as count from asset_use au left join component c on c.id = au.component_id where asset_id in (" +
			" select asset_id from contract where id = ? " +
			") and au.cw_id in (select cw_id from contract where id = ?) and " +
			"(c.category is null or c.category != '" + ComponentCategory.COVER.getCode() + "')";
		//log.debug("contractContainsInternalAssets(): sql: " + sql);
		Query query = entityManager.createNativeQuery(sql);
		query.setParameter(1, contractId);
		query.setParameter(2, contractId);
		// For count, DB2 and MS SQL Server return Integer but MySQL returns BigInteger
		// Integer and BigInteger inherit from Number
		Number result = (Number) query.getSingleResult();

		return result.intValue() > 0;
	}

	/**
	 * removes the ContractAsset
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public Contract removeAssetFromContract(Contract contract, Integer assetId)
			throws PersistenceException
	{
		// TODO: Perhaps this could be more efficient using a native delete query

		Asset asset = find(Asset.class, assetId);

		ContractAsset contractAsset = find(ContractAsset.class,
			new ContractAssetPK(asset, contract));

		if (contractAsset != null) {
			contract.removeAsset (contractAsset);

			getEntityManager().remove(contractAsset);
		}
		else {
			throw new PersistenceException("Asset Not Found In the Given Contract");
		}

		return contract;
	}

	//Added by santhosh for DM-532
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void removeFileFromContract(ContractFile contractfile)
			throws PersistenceException
	{
		// TODO: Perhaps this could be more efficient using a native delete query

		contractfile = find(ContractFile.class,contractfile.getId() );
		if (contractfile != null) {
			getEntityManager().remove(contractfile);
		}
		else {
			throw new PersistenceException("Contract File Not Found In the Given Contract");
		}


	}
	//END aDDED

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public boolean removeFileFromContractFileName(Integer contractId,String conFileName) throws PersistenceException {
		String sql = "delete from contract_file_name where contract_id = ? and file_name=?";
		Query q = entityManager.createNativeQuery(sql);
		q.setParameter(1, contractId);
		q.setParameter(2, conFileName);
		int rowCount = q.executeUpdate();
		log.debug("delete from contract_file_name where contract_id = <" + contractId + ">: " + rowCount + " rows deleted.");
		return rowCount>0;
		}
	
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public ContractFile loadContractFilesByFileId(Integer fileId) throws PersistenceException {
		ContractFile conFile = null;
		String sql = "select * from contract_file where id = ?";
		Query query = entityManager.createNativeQuery(sql, ContractFile.class);
		query.setParameter(1, fileId);

		conFile = (ContractFile) query.getSingleResult();
		return conFile;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public ArrayList<Asset> addAssetsToContract(Contract contract, Integer[] assetIds) throws PersistenceException
	{
		for (Integer assetId : assetIds) {
			log.debug("addAssetsToContract(): assetId: " + assetId);
		}

		ArrayList<Asset> addedList = new ArrayList<Asset>(assetIds.length);

		for (Integer assetId : assetIds) {
			Asset asset = find(Asset.class, assetId);
			addedList.add(asset);

			ContractAsset contractAsset = new ContractAsset(asset, contract, null);
			entityManager.persist(contractAsset);
			contract.addAsset(contractAsset);
		}

		return addedList;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public Account getAccountByNumberAndSubCode(String accountNumber, String subCode) throws PersistenceException
	{
		String queryString = "from Account account where account.accountNumber = ? and account.subCode = ?";

		try {
			TypedQuery<Account> q = entityManager.createQuery(queryString, Account.class);
			q.setParameter(1, accountNumber);
			q.setParameter(2, subCode);
			return q.getSingleResult();
		}
		catch (NoResultException e) {
			return null;
		}
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<POAssetView> loadPOAssetsView(int contractId)
	{
		final String sql = " select distinct a.id, a.description, a.media_type, " +
				"GROUP_CONCAT(au.usage_type), GROUP_CONCAT(au.position), ct.id, ct.date, GROUP_CONCAT('[' || c.name || ', ' || au.position || ']'), "
				+ "c.id, c_2_a.price, c_2_a.rfdeal_id, a.credit_line,  a.vendor_id,  c_2_a.credit_line, a.is_royalty_free, a.will_be_work_for_hire, " +
				" a.will_be_royalty_free, c_2_a.is_asset_level_rfdeal " +
		" from asset a join asset_use au on a.id = au.asset_id" +
		" left join component c on au.component_id = c.id" +
		" left join contract_2_asset c_2_a on c_2_a.asset_base_id = a.id " +
		" left join contract ct on c_2_a.contract_id = ct.id" +
		" where ct.id = ? group by a.id";

		Object[] args = { contractId };

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
				poView.setPrice (rs.getDouble(10));
				poView.setRfDealId(rs.getInt(11));
				poView.setCreditLine (rs.getString(12));
				poView.setVendorId (rs.getString(13));
				poView.setConditionCreditLine(rs.getString(14));
				poView.setRoyaltyFree (rs.getBoolean(15));
				poView.setWillBeWorkForHire(rs.getBoolean(16));
				poView.setWillBeRoyaltyFree(rs.getBoolean(17));
				poView.setHasAssetLevelRFDeal(rs.getBoolean(18));//Added for implementing DM-122
				return poView;
			}
		});

		return poViews;
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public POAssetView loadPOAssetView(int assetId)
	{
		final String sql = " select distinct a.id, a.description, a.media_type, " +
				"GROUP_CONCAT(au.usage_type), GROUP_CONCAT(au.position), GROUP_CONCAT('[' || c.name || ', ' || au.position || ']'), "
				+ "c.id, a.credit_line,  a.vendor_id,  a.is_royalty_free, rf.po_id, po.date " +
		" from asset a join asset_use au on a.id = au.asset_id" +
		" left join component c on au.component_id = c.id" +
		" left join asset_perm_ref rf on rf.asset_use_id = au.id " +
		" left join purchase_order po on po.id = rf.po_id " +
		" where a.id = ? group by a.id";

		Object[] args = { assetId };

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
				poView.setComponentName (rs.getString (6));
				poView.setComponentId (rs.getInt (7));
				poView.setCreditLine (rs.getString(8));
				poView.setVendorId (rs.getString(9));
				poView.setRoyaltyFree (rs.getBoolean(10));
				poView.setPoId(rs.getInt(11));
				poView.setPoDate(rs.getDate(12));
				return poView;
			}
		});

		if (poViews.isEmpty()) {
			return null;
		} else {
			return poViews.get(0);
		}
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public Contract loadContractById(Integer contractId) throws PersistenceException
	{
		return find(Contract.class, contractId);
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public Contract loadContractWithFiles(Integer contractId) throws PersistenceException
	{
		Contract contract =  loadContractById(contractId);
		for (ContractFile file : contract.getFiles()) {
			file.getId();
			file.getFileData();
			file.getFileName();
			file.getLastUpdatedDate();
		}

		return contract;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public Contract loadContractByNumber(String contractNumber, Integer sourceId, Integer cwId) throws PersistenceException
	{
		TypedQuery<Contract> query = entityManager.createQuery("from Contract up where up.number = ? and up.source.id = ? and up.commonWork.id = ?",
				Contract.class);
		query.setParameter(1, contractNumber);
		query.setParameter(2, sourceId);
		query.setParameter(3, cwId);
		try {
			Contract contract = query.getSingleResult();
			contract.getAssets();
			for (ContractAsset asset : contract.getAssets()) {
				asset.getAssetBaseId();
			}
			for (ContractFile file : contract.getFiles()) {
				file.getId();
			}
			contract.getSource().isCoveredByMasterAgreement();
			return contract;
		} catch (NoResultException e) {
			return null;
		}
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void updateContract2Asset(Integer contractId, Integer assetId, double price, Integer dealId) throws PersistenceException
	{
		String sql = "update contract_2_asset set price = ?, rfdeal_id = ? where contract_id = ? and asset_base_id = ?";
		Query q = entityManager.createNativeQuery(sql);
		q.setParameter(1, price);
		q.setParameter(2, dealId);
		q.setParameter(3, contractId);
		q.setParameter(4, assetId);
		q.executeUpdate();

		sql = "update contract set price = (select sum(price) from contract_2_asset where contract_id = ? and price is not null) where id = ?";
		q = entityManager.createNativeQuery(sql);
		q.setParameter(1, contractId);
		q.setParameter(2, contractId);
		q.executeUpdate();
	}

	//Start: Added for DM-532
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void updateContractFileName(Contract contract, List<ContractFileName> contractFileList) throws PersistenceException {
		List<ContractFileName> contractFileNames = contract.getFileNames();
		boolean fileExists;
		for (ContractFileName fileName : contractFileList) {
			fileExists = false;
			for (ContractFileName contractFileNameObj : contractFileNames) {
				if(contractFileNameObj.getFileName().equalsIgnoreCase(fileName.getFileName())) {
					fileExists = true;
					break;
				}
			}
			if(!fileExists) {
				fileName.setContract(contract);
				entityManager.persist(fileName);
			}
		}
	}

	public boolean isMultiUploadInProgress(int cwId) {
		final String sql = "select count(*) as count from upload_docs_history where cw_id = ? and upload_in_progress is true";
		Query query = entityManager.createNativeQuery(sql);
		query.setParameter(1, cwId);
		Number result = (Number) query.getSingleResult();
		return result.intValue() > 0;
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<UploadedDocumentsDetails> loadUploadDocsDetailsByHistoryId (int historyID) {
		TypedQuery<UploadedDocumentsDetails> query = entityManager.createQuery("from UploadedDocumentsDetails udd where udd.uploadHistoryId.id = ?",
				UploadedDocumentsDetails.class);
		query.setParameter(1, historyID);
		return query.getResultList();
	}
	//End: Added for DM-532

	/**
	 * Returns a list of contracts for a commonWork
	 * @param cwId
	 * @return List<CwFile>
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<Contract> loadContractsByCWId (int cwId) {
		TypedQuery<Contract> query = entityManager.createQuery("from Contract up join fetch up.source where up.commonWork.id = ?",
				Contract.class);
		query.setParameter(1, cwId);
		return query.getResultList();
	}

	/**
	 * Returns a list of contracts that need amendment for a commonWork, source id
	 * @param cwId - common work id
	 * @param sourceId
	 * @param assetId
	 * @return List<Contract>
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<Contract> loadContractsThatNeedAmendment (int cwId, int sourceId, int assetId) {
		log.debug("loadContractsThatNeedAmendment: cwId [" + cwId + "], sourceId [" + sourceId + "], assetId [" + assetId + "]");
		String sql = "select c.* from contract c " +
			(0 != assetId ? " right join contract_2_asset on c.id = contract_2_asset.contract_id and asset_base_id = :assetId" : "") +
			" where c.cw_id = :cwId " +
			(0 != sourceId ? " and c.source_id = :sourceId " : "") +
			" and c.id not in (select contract_id from amendment) " +
			" order by date desc ";
		log.debug("sql: " + sql);
		Query query = entityManager.createNativeQuery(sql, Contract.class);

		query.setParameter("cwId", cwId);
		if (0 != sourceId)
			query.setParameter("sourceId", sourceId);
		if (0 != assetId)
			query.setParameter("assetId", assetId);
		@SuppressWarnings("unchecked")
		List<Contract> list = query.getResultList();
		return list;
	}

	/**
	 * Returns a list of contracts with amendment for a commonWork, source id
	 * @param cwId - common work id
	 * @param sourceId
	 * @param assetId
	 * @return List<Contract>
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<Contract> loadContractsWithAmendment (int cwId, int sourceId, int assetId) {
		log.debug("loadContractsWithAmendment: cwId [" + cwId + "], sourceId [" + sourceId + "], assetId [" + assetId + "]");
		String sql = "select c.* from contract c " +
			(0 != assetId ? " right join contract_2_asset on c.id = contract_2_asset.contract_id and asset_base_id = :assetId" : "") +
			" where c.cw_id = :cwId " +
			(0 != sourceId ? " and c.source_id = :sourceId " : "") +
			" and c.id in (select contract_id from amendment) " +
			" order by date desc ";
		log.debug("sql: " + sql);
		Query query = entityManager.createNativeQuery(sql, Contract.class);

		query.setParameter("cwId", cwId);
		if (0 != sourceId)
			query.setParameter("sourceId", sourceId);
		if (0 != assetId)
			query.setParameter("assetId", assetId);
		@SuppressWarnings("unchecked")
		List<Contract> list = query.getResultList();
		return list;
	}

	/**
	 * Returns a list of contracts for a commonWork id and optional source id, optional assetId
	 * @param cwId     Must be valid
	 * @param sourceId 0 means don't filter on this
	 * @param assetId  0 means don't filter on this
	 * @return List<Contract>
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<Contract> loadMostRecentContractList (int cwId, int sourceId, int assetId) {
		log.debug("loadMostRecentContractList: cwId [" + cwId + "], sourceId [" + sourceId + "], assetId [" + assetId + "]");
		String sql = "select c.* from contract c " +
			(0 != assetId ? " right join contract_2_asset on c.id = contract_2_asset.contract_id and asset_base_id = :assetId" : "") +
			" where c.cw_id = :cwId " +
			(0 != sourceId ? " and c.source_id = :sourceId " : "") +
			" order by date desc "
			// bug fix - https://www.pivotaltracker.com/story/show/57609278 show all contracts to edit
			// + (0 != assetId ? " limit 1 " : "")
			;
		log.debug("sql: " + sql);
		Query query = entityManager.createNativeQuery(sql, Contract.class);

		query.setParameter("cwId", cwId);
		if (0 != sourceId)
			query.setParameter("sourceId", sourceId);
		if (0 != assetId)
			query.setParameter("assetId", assetId);
		@SuppressWarnings("unchecked")
		List<Contract> list = query.getResultList();
		for(Contract contract : list) {
			contract.getFiles();
		}
		return list;
	}

	/**
	 * Given a contract id, looks at po's and contracts that have the same cwId and sourceId.
	 * From this set determines more recent po's and contract's that covers the same assets.
	 * If no such po or contract exists in the list, returns false.
	 *
	 * @param contractId  Should be valid
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public boolean moreRecentPOOrContractWithAllAssets(Contract orig) {
		String sql = "select c.* from contract c where cw_id = ? and source_id = ? and date > ? order by date desc";
		Query query = entityManager.createNativeQuery(sql, Contract.class);
		query.setParameter(1, orig.getCommonWork().getId());
		query.setParameter(2, orig.getSource().getId());
		query.setParameter(3, orig.getDate());
		@SuppressWarnings("unchecked")
		List<Contract> contractList = query.getResultList();
		for (Contract c : contractList) {
			if (doesContractContainAllAssets(c.getId(), orig.getId())) {
				log.debug("moreRecentPOOrContractWithAllAssets(): found contract id "
						+ c.getId() + " that covers contract id " + orig.getId());
				return true;
			}
		}

		sql = "select po.* from purchase_order po where cw_id = ? and source_id = ? and date > ? order by date desc";
		query = entityManager.createNativeQuery(sql, PurchaseOrder.class);
		query.setParameter(1, orig.getCommonWork().getId());
		query.setParameter(2, orig.getSource().getId());
		query.setParameter(3, orig.getDate());
		@SuppressWarnings("unchecked")
		List<PurchaseOrder> poList = query.getResultList();
		for (PurchaseOrder po : poList) {
			if (doesPOContainAllAssets(po.getId(), orig.getId())) {
				log.debug("moreRecentPOOrContractWithAllAssets(): found po id "
						+ po.getId() + " that covers contract id " + orig.getId());
				return true;
			}
		}

		return false;
	}

	/**
	 * Called by moreRecentPOOrContractWithAllAssets().
	 * Returns true if contract 1 contains all assets of contract 2.
	 *
	 * @param contractId
	 * @param contractId2
	 */
	private boolean doesContractContainAllAssets(int contractId1, int contractId2) {
		String sql = "select count(*) as count from contract_2_asset "
			+ "where contract_id = ? and asset_base_id not in (select asset_base_id from contract_2_asset where contract_id = ?)";

		Query query = entityManager.createNativeQuery(sql, "scalarCount");
		query.setParameter(1, contractId1);
		query.setParameter(2, contractId2);

		// For count, DB2 and MS SQL Server return Integer but MySQL returns BigInteger
		// Integer and BigInteger inherit from Number
		Number resultNum = (Number) query.getSingleResult();
		return resultNum.intValue() == 0;
	}

	/**
	 * Called by moreRecentPOOrContractWithAllAssets().
	 * Returns true if PO contains all assets of Contract.
	 *
	 * @param poId
	 * @param contractId
	 */
	private boolean doesPOContainAllAssets(int poId, int contractId) {
		String sql = "select count(*) as count from purchase_order_2_asset "
			+ "where purchase_order_id = ? and asset_base_id not in (select asset_base_id from contract_2_asset where contract_id = ?)";

		Query query = entityManager.createNativeQuery(sql, "scalarCount");
		query.setParameter(1, poId);
		query.setParameter(2, contractId);

		// For count, DB2 and MS SQL Server return Integer but MySQL returns BigInteger
		// Integer and BigInteger inherit from Number
		Number resultNum = (Number) query.getSingleResult();
		return resultNum.intValue() == 0;
	}

	/**
	 * Returns a list of unpaid contracts for a commonWork, source id
	 * @param cwId - common work id
	 * @param sourceId
	 * @param assetId
	 * @return List<Contract>
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<Contract> loadUnpaidContractList (int cwId, int sourceId, int assetId) {
		PerfTimer timer = monitor.startTimer("ContractRepository::loadUnpaidContractList");
		log.debug("loadUnpaidContractList: cwId [" + cwId + "], sourceId [" + sourceId + "], assetId [" + assetId + "]");
		String sql = "select c.* from contract c " +
			" join contract_2_asset c2a on c2a.contract_id = c.id " +
			(0 != assetId ? " and c2a.asset_base_id = :assetId" : "") +
			" join asset_use au on c2a.asset_base_id = au.asset_id and au.need_payment_request=1 " +
			" where c.cw_id = :cwId and c.payment_type is null " +
			(0 != sourceId ? " and c.source_id = :sourceId " : "") +
			" order by date desc " +
			(0 != assetId ? " limit 1 " : "");
		log.debug("loadUnpaidContractList(): sql: " + sql);
		Query query = entityManager.createNativeQuery(sql, Contract.class);

		query.setParameter("cwId", cwId);
		if (0 != sourceId)
			query.setParameter("sourceId", sourceId);
		if (0 != assetId)
			query.setParameter("assetId", assetId);
		@SuppressWarnings("unchecked")
		List<Contract> resultList = query.getResultList();
		timer.stopTimer();
		return resultList;
	}

	/**
	 * Returns a list of payment requests for a commonWork
	 * @param cwId
	 * @return
	 */
	public List<PaymentRequest> getPaymentRequestsByCWId(int cwId) {
		TypedQuery<PaymentRequest> query = entityManager.createQuery("from PaymentRequest up where up.contract.commonWork.id = ?",
				PaymentRequest.class);
		query.setParameter(1, cwId);
		return query.getResultList();
	}

	/**
	 * Returns a list of contracts that are close to expire and are not
	 * attached to common works in status 'O' (Out of Print) or 'W' (New edition pending)
	 * except if the process code is HS or SP ignore the O or W
	 * More logic 9/2012: Also include two expiration windows - 2 years, and 8 months
	 * with 2 week window around each (requirements about whether 2 weeks are "centered"
	 * on 2 year mark or start at 2 year mark are unspecified - left up to us).
	 *
	 * @param daysBeforeExpire
	 * @returnList<Contract>
	 */
	@SuppressWarnings("unchecked")
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<Contract> getContractsCloseToExpireOrWindow(int daysBeforeExpire) {
		String sql = "select c.* from contract c join product p on c.cw_id = p.cw_id" +
			" where c.end_date is not null and c.end_date > current_timestamp"
			+ " and ("
				+ " (c.end_date <= (current_timestamp + interval '" + daysBeforeExpire + "' day))"
				+ " or (c.end_date >= (current_timestamp + interval '2' year) and c.end_date < (current_timestamp + interval '2' year + interval '14' day))"
				+ " or (c.end_date >= (current_timestamp + interval '240' day) and c.end_date < (current_timestamp + interval '254' day))"
			+ ") and p.is_cw_primary = true and (p.pub_status not in ('O', 'W') or p.process_code in ('HS', 'SP'))";
		log.debug("getContractsCloseToExpire(): sql: " + sql);
		Query query = entityManager.createNativeQuery(sql, Contract.class);
		return query.getResultList();
	}

	/**
	 * Returns a list of contracts that are expired and are not
	 * attached to common works in status 'O' (Out of Print) or 'W' (New edition pending)
	 * except if the process code is HS or SP ignore the O or W
	 *
	 * @returnList<Contract>
	 */
	@SuppressWarnings("unchecked")
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<Contract> getContractsExpired() {
		String sql = "select c.* from contract c join product p on c.cw_id = p.cw_id  " +
			"where c.end_date is not null and c.end_date <= current_timestamp" +
			" and p.is_cw_primary = true and (p.pub_status not in ('O', 'W') or p.process_code in ('HS', 'SP'))";
		log.debug("getContractsExpired(): sql: " + sql);
		Query query = entityManager.createNativeQuery(sql, Contract.class);
		return query.getResultList();
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public String loadContractPermissionInfo (int contractId) {
		// "scalarGroup" is defined in an annotation at the top of the AssetUse class.
		Query query = entityManager.createNativeQuery(
				"select group_concat(condt.description || ':' || cond.rollup_value) as cgroup " +
				"from condition_value cond join condition_type condt on condt.code = cond.condition_type " +
				"where rollup_value is not null and cond.id in " +
				"(select condition_id from contract_2_condition where contract_id = ?)",
			"scalarGroup");
		query.setParameter(1, contractId);
		String groupValue = (String) query.getSingleResult();
		return groupValue;
	}

	/**
	 * All inputs expected to be non-null except contractId.
	 * contractId is excluded from the search if non-null.
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public boolean contractExists(int cwId, int sourceId, String number, Integer contractId) {
		String sql = "select count(*) as count from contract where cw_id = ? and source_id = ? and number = ?";
		if (contractId != null) sql += " and id != ?";
		Query query = entityManager.createNativeQuery(sql, "scalarCount");
		query.setParameter(1, cwId);
		query.setParameter(2, sourceId);
		query.setParameter(3, number);
		if (contractId != null) {
			query.setParameter(4, contractId);
		}

		// For count, DB2 and MS SQL Server return Integer but MySQL returns BigInteger
		// Integer and BigInteger inherit from Number
		Number resultNum = (Number) query.getSingleResult();
		return resultNum.intValue() > 0;
	}

	/**
	 * Source and PO wizards call this method with today's date.
	 * Contract wizard calls this method with the invoice date.
	 *
	 * @param sourceGroupId  Should be non-null and valid
	 * @param userId  Should be non-null and valid
	 * @param date  Must be non-null
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<MasterAgreementDeal> loadMasterAgreements(Integer sourceGroupId, Integer userId, Date date) {
		final String sql = "select distinct mad.* from master_agreement_deal mad, user_table u, user_defaults ud, "
			+ "madeal_2_ulocation lmap, madeal_2_businessunit bmap, user_group ug\r\n"
			+ "where mad.source_group_id = ? and mad.start_date <= ? and (mad.end_date is null or mad.end_date >= ?)\r\n"
			+ "and u.id = ? and ud.user_id = u.id and u.user_group_id = ug.id\r\n"
			+ "and ud.user_location_code = lmap.ulocation_code and lmap.madeal_id = mad.id\r\n"
			+ "and (\r\n"
			+ "  ud.business_unit_code is null or ud.business_unit_code in ('" + BusinessUnit.U0.getCode()
			+ "', '" + BusinessUnit.U5.getCode() + "', '" + BusinessUnit.U6.getCode() + "', '"
			+ BusinessUnit.U7.getCode() + "', '" + BusinessUnit.U8.getCode() + "', '"
			+ BusinessUnit.U9.getCode() + "')\r\n"
			+ "  or (ud.business_unit_code = bmap.businessunit_code and bmap.madeal_id = mad.id)\r\n"
			+ "  or ug.name in ('" + UserGroup.CORPORATE_GROUP.getName() + "', '"
			+ UserGroup.AUTHOR_GROUP.getName() + "', '" + UserGroup.FREELANCER_GROUP.getName() + "')\r\n"
			+ ")";

		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS z");
		log.debug("loadMasterAgreements(): sql: " + sql + "\r\nsourceGroupId = " + sourceGroupId
				+ ", userId = " + userId + ", date = " + dateFormat.format(date));
		Query query = entityManager.createNativeQuery(sql, MasterAgreementDeal.class);
		query.setParameter(1, sourceGroupId);
		query.setParameter(2, date);
		query.setParameter(3, date);
		query.setParameter(4, userId);
		@SuppressWarnings("unchecked")
		List<MasterAgreementDeal> list = query.getResultList();
		log.debug("loadMasterAgreements(): resultList.size() = " + list.size());
		return list;
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public Integer loadMaDealIdForContract(int contractId) {
		// Use "as id" to match scalarId
		Query query = createNativeQuery("select ma_deal_id as id from contract where id = ?", "scalarId");
		query.setParameter(1, contractId);
		try {
			return (Integer) query.getSingleResult();
		}
		catch (NoResultException ex) {
			return null;
		}
	}
	
	//Start: Added to implement DM-1185
		@Transactional(propagation = Propagation.REQUIRED)
		public RoyaltyFreeDeal loadRFDealById(Integer rfDealId) throws PersistenceException
		{
			return find(RoyaltyFreeDeal.class, rfDealId);
		}
		//End: Added to implement DM-1185
	
	
	/**
	 * checks to see if print run condition has been exceeded for a given contract
	 * @return boolean
	 */
	public boolean exceededPrinting(Contract contract) {
		int contractValue = getPrintRunFromContract(contract);
		int cwId = contract.getCommonWork().getId();
		int cwValue = cwRepository.totalPrintings(cwId);

		if (contractValue == 0) {
			return false;
		}

		if (contractValue < cwValue) {
			return true;
		}

		// no print run condition found so not exceeded
		return false;
	}

	/**
	 * checks to see if print run condition has been exceeded for a given contract
	 * @return boolean
	 */
	public boolean closeToExceedingPrinting(Contract contract) {
		int contractValue = getPrintRunFromContract(contract);
		int cwId = contract.getCommonWork().getId();
		int cwValue = cwRepository.totalPrintings(cwId);

		if (contractValue == 0) {
			return false;
		}

		if ((contractValue * 8 / 10) < cwValue) {
			return true;
		}

		// no appropriate print run condition found so not exceeded
		return false;
	}

	/**
	 * Returns 0 if the contract specifies an unlimited print run or specifies "no mention".
	 * Otherwise the contract should specify and exact number and returns that.
	 */
	public int getPrintRunFromContract(Contract contract) {
		Condition c = contract.getConditionByType(ConditionType.PRINT_RUN_UNLIMITED);
		if (c != null && c.isValueTrue()) return 0;

		c = contract.getConditionByType(ConditionType.PRINT_RUN_NO_MENTION);
		if (c != null && c.isValueTrue()) return 0;

		c = contract.getConditionByType(ConditionType.PRINT_RUN_LIMIT_BOX);
		if (c == null) {
			log.error("getPrintRunFromContract(): None of PrintRunUnlimited, No Mention, or Limit Box conditions are set!");
			return 0;
		}

		int value = 0;

		try {
			value = Integer.parseInt(c.getValue());
		} catch (Exception e) {
			log.error("getPrintRunFromContract(): could not parse print run value ["
				+ c.getValue() + "] from contractId " + contract.getId());
		}

		return value;
	}

	public boolean doesContractHaveFutureEditionGrant(Contract contract) {
		Condition c = contract.getConditionByType(ConditionType.EDITION_THIS_FUTURE_AUTHOR);
		if (c != null && c.isValueTrue())  return true;

		c = contract.getConditionByType(ConditionType.EDITION_FUTURE);
		if (c != null && c.isValueTrue())  return true;

		return false;
	}

	@SuppressWarnings("unchecked")
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<Contract> getPaidContractsForAssetUse(int auId) {
		// perhaps should use "active_contract_id" instead of "latest_contract_id" but probably doesn't matter
		String sql = "select c.* from contract c join au_source_perm_status asps on c.id = asps.latest_contract_id " +
			"where asps.asset_use_id = ? and asps.paid is true";
		Query query = entityManager.createNativeQuery(sql, Contract.class);
		query.setParameter(1, auId);
		List<Contract> list = query.getResultList();
		// preload Sources
		for (Contract c : list) {
			c.getSource().getName();
		}
		return list;
	}

	/**
	 * This method is called by the rules engine.
	 *
	 * @param contract  Must be non-null
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public boolean doesContractNeedAmendment(Contract contract) {
		log.debug("doesContractNeedAmendment(): contract id = " + contract.getId());
		PerfTimer timer = monitor.startTimer("ContractRepository::doesContractNeedAmendment");

		try {
			// We no longer have contracts with no conditions, except stuff imported
			// from Australia which has what you might call placeholder contracts.
			if (contract.getConditions() == null)  return false;

			if (contract.hasAmendmentFile())  return false;
			if (contract.getSource().isCoveredByMasterAgreement())  return false;

			if (!contract.isUnlimitedPrintRun() || contract.getEndDate() != null) {
				if (atLeastOnePhotoForContract(contract)) {
					return true;
				}
			}

			return false;
		}
		finally {
			timer.stopTimer();
		}
	}

	private boolean atLeastOnePhotoForContract(Contract contract) {
		PerfTimer timer = monitor.startTimer("ContractRepository::atLeastOnePhotoForContract");

		// Note we are ONLY looking at MediaType.PHOTO here - not Photo-equivalent such as Illustration
		String sql = "select count(*) as count from contract_2_asset ca, asset a, contract c, asset_use au"
			+ " where ca.contract_id = ? and ca.asset_base_id = a.id and a.media_type = '" + MediaType.PHOTO.getCode()
			+ "' and c.id = ca.contract_id and au.cw_id = c.cw_id and au.asset_id = a.id and au.is_canceled is false";
		Query query = entityManager.createNativeQuery(sql);
		query.setParameter(1, contract.getId());
		// For count, DB2 and MS SQL Server return Integer but MySQL returns BigInteger
		// Integer and BigInteger inherit from Number
		Number result = (Number) query.getSingleResult();

		timer.stopTimer();
		return result.intValue() > 0;
	}

	/**
	 * Given a valid contractId this method should always return a non-null sourceId.
	 */
	public Integer getSourceIdForContractId(int contractId) {
		final String sql = "select source_id as int_col from contract where id = ?";
		Query query = entityManager.createNativeQuery(sql, "scalarInt");
		query.setParameter(1, contractId);
		// don't bother catching NoResultException - we should always get a result assume a valid contractId
		return (Integer) query.getSingleResult();
	}

	public boolean doesContractGiveSublicenseRight(int contractId) {
		final String sql = "select count(*) as count from view_contract_condition"
			+ " where contract_id = ? and condition_type = '"
			+ ConditionCode.SUBLICENSE_RIGHT.getCode() + "' and value = 'true'";
		Query query = entityManager.createNativeQuery(sql);
		query.setParameter(1, contractId);
		// For count, DB2 and MS SQL Server return Integer but MySQL returns BigInteger
		// Integer and BigInteger inherit from Number
		Number result = (Number) query.getSingleResult();
		return result.intValue() > 0;
	}

	//Start: Added to implement DM-122
	public boolean isAssetLevelRoyaltyFree(int contractId, int assetId) {
		final String sql = "select count(*) as count from contract_2_asset"
			+ " where contract_id = ? and asset_base_id = ? and is_asset_level_rfdeal is true";
		Query query = entityManager.createNativeQuery(sql);
		query.setParameter(1, contractId);
		query.setParameter(2, assetId);
		// For count, DB2 and MS SQL Server return Integer but MySQL returns BigInteger
		// Integer and BigInteger inherit from Number
		Number result = (Number) query.getSingleResult();
		return result.intValue() > 0;
	}
	//End: Added to implement DM-122

	/*
	 * Updating the Current , Approver Username with Approver Remarks
	 * for Contract and Asset Id in Contract_2_asset Table
	 * for SS Task 12
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public void saveApproverDetails(Map<String, Object> input) throws PersistenceException
	{
		String sql = "update contract_2_asset set user_name = ?, approver_name = ?, approver_remarks = ? where asset_base_id = ? and contract_id = ?";
		log.debug("sql: "+sql);
		Query q = entityManager.createNativeQuery(sql);
		q.setParameter(1, input.get("userName"));
		q.setParameter(2, input.get("approverName"));
		q.setParameter(3, input.get("approverRemarks"));
		q.setParameter(4, input.get("assetId"));
		q.setParameter(5, input.get("contractId"));
		q.executeUpdate();
	}
    /*
     * Paperwork change to get CommonWork ID
     * by providing Pickup ISBN
     */
	public Integer loadProductCwId(String pickupISBN) {
		final String sql = "select cw_id from product"
				+ " where isbn13 = ? ";
			Query query = entityManager.createNativeQuery(sql);
			query.setParameter(1, pickupISBN);
			// For count, DB2 and MS SQL Server return Integer but MySQL returns BigInteger
			// Integer and BigInteger inherit from Number
			Integer result = (Integer) query.getSingleResult();
			return result;
	}

	//start Added for DM-534
		@SuppressWarnings("unchecked")
		@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
		public String loadContractFiles(int contractid) throws PersistenceException
		{
			log.debug("in load contract files");
			final String sql ="select file_name from contract_file where contract_id = ?";
			StringBuilder sb = new StringBuilder();
			Query query = entityManager.createNativeQuery(sql);
			query.setParameter(1, contractid);
			List<String> results=query.getResultList();
			log.debug("in load contract files"+results.size());
			if(results==null || results.isEmpty()) {
				return "";
			}
			for(String filename: results) {
				if(sb.length() > 0){
			        sb.append(',');
			    }
				sb.append(filename);
			}
			return sb.toString();
		}

		@SuppressWarnings("unchecked")
		@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
		public String loadConditionDesc(String code) throws PersistenceException
		{
			log.debug("in load loadConditionDesc");
			final String sql ="select description from condition_type where code = ?";
			Query query = entityManager.createNativeQuery(sql);
			query.setParameter(1, code);
			String desc = (String)query.getSingleResult();
			return desc;
		}
		//end Added for DM-534

	public AssetUseRepository getAssetUseRepository() {
		return assetUseRepository;
	}

	public void setAssetUseRepository(AssetUseRepository assetUseRepository) {
		this.assetUseRepository = assetUseRepository;
	}

	public CommonWorkRepository getCommonWorkRepository() {
		return cwRepository;
	}

	public void setCommonWorkRepository(CommonWorkRepository cwRepository) {
		this.cwRepository = cwRepository;
	}

	public ProductIndexService getProductIndexService() {
		return productIndexService;
	}

	public void setProductIndexService(ProductIndexService service) {
		this.productIndexService = service;
	}

	public DataSource getDataSource() {
		return dataSource;
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}
}
