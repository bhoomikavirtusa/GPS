package com.wiley.permissions.repositories;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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

import com.wiley.permissions.common.utils.DuplicateAddressTypeException;
import com.wiley.permissions.common.utils.UniqueConstraintViolationException;
import com.wiley.permissions.domain.persistence.permissions.Address;
import com.wiley.permissions.domain.persistence.permissions.AddressType;
import com.wiley.permissions.domain.persistence.permissions.Asset;
import com.wiley.permissions.domain.persistence.permissions.AssetUse;
import com.wiley.permissions.domain.persistence.permissions.CompCopy;
import com.wiley.permissions.domain.persistence.permissions.Contact;
import com.wiley.permissions.domain.persistence.permissions.Contract;
import com.wiley.permissions.domain.persistence.permissions.ContractAsset;
import com.wiley.permissions.domain.persistence.permissions.Country;
import com.wiley.permissions.domain.persistence.permissions.DeliveryMethod;
import com.wiley.permissions.domain.persistence.permissions.FavoriteGroup;
import com.wiley.permissions.domain.persistence.permissions.MasterAgreementDeal;
import com.wiley.permissions.domain.persistence.permissions.PurchaseOrder;
import com.wiley.permissions.domain.persistence.permissions.RoyaltyFreeDeal;
import com.wiley.permissions.domain.persistence.permissions.Source;
import com.wiley.permissions.domain.persistence.permissions.SourceAddress;
import com.wiley.permissions.domain.persistence.permissions.SourceFile;
import com.wiley.permissions.domain.persistence.permissions.SourceGroup;
import com.wiley.permissions.persistence.PersistenceException;
import com.wiley.permissions.persistence.repository.JPARepository;
import com.wiley.permissions.services.util.ServiceException;
import com.wiley.permissions.services.view.SourceSummaryView;
import com.wiley.sf.common.lang.ArgUtil;
import com.wiley.sf.common.monitor.PerformanceMonitor.PerfTimer;

public class SourceRepository extends JPARepository {

	private static final Log log = LogFactory.getLog(SourceRepository.class);

	@PersistenceContext(unitName = "permissions")
	protected EntityManager entityManager;

	private DataSource dataSource;

	@Override
	protected EntityManager getEntityManager() {
		return entityManager;
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public Source loadSourceByTemplate(Source source) throws PersistenceException
	{
		Source oldSource = null;

		if (null == source)
			return null;

		if (StringUtils.isNotBlank(source.getName()))
			oldSource = loadSourceByName(source.getName());
		if (null == oldSource && StringUtils.isNotBlank(source.getExternalId()))
			oldSource = loadSourceByExternalId(source.getExternalId());
		return oldSource;
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<Source> loadSourceList() throws PersistenceException
	{
		List<Source> list = loadAll(Source.class);
		return list;
	}

	/**
	 * Source has a unique constraint on name. Return the externalId of the
	 * source already having the name if an insert of the given source will
	 * violate the constraint. Returns null if the name is not already in use.
	 * The given source is expected to have a non-null name.
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public String checkForDuplicateName(Source source) throws PersistenceException
	{
		ArgUtil.notNull(source.getName(), "name");

		TypedQuery<Source> query = entityManager.createQuery("from Source s where s.name = ?", Source.class);
		query.setParameter(1, source.getName());

		try {
			Source s = query.getSingleResult();
			return s.getExternalId();
		}
		catch (NoResultException ex) {
			return null;
		}
	}

	/**
	 * This method is used for both creating a Source and modifying one. If
	 * the source externalId are null, then a
	 * create action is assumed.
	 *
	 * @throws Exception
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public Source saveSource(Source source)
			throws PersistenceException, UniqueConstraintViolationException, Exception
	{
		// we want to make sure the source name is not blank. Will help us debug
		// also
		ArgUtil.notBlank(source.getName(), "source.name");

		boolean isNew = (source.getId() == null);

		log.debug("saveSource(): Source name: " + source.getName());

		if (isNew) {
			String extId = checkForDuplicateName(source); // throws PersistenceException

			if (extId != null) {
				String msg = "A source (extId = " + extId + ") already exists with the name "
						+ source.getName() + ".";
				throw new UniqueConstraintViolationException(msg, extId, true);
			}
		}
		if (null == source.getDisplayName()) {
			source.setDisplayName(source.getName());
		}

		source = entityManager.merge(source);

		log.debug("saveSource(): merged source (committed): " + source);
		log.debug("saveSource(): isNew = " + isNew);

		return source;
	}

	public Integer assetCountForSource(Integer sourceId) {
		Query query = entityManager.createNativeQuery(
				"select count(*) as count from asset_2_source  where source_id = ? " ,
				"scalarCount");
		query.setParameter(1, sourceId);
		// For count, DB2 and MS SQL Server return Integer but MySQL returns BigInteger
		// Integer and BigInteger inherit from Number
		Number result = (Number) query.getSingleResult();

		return result.intValue();
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public Source loadSourceById(Integer id) throws PersistenceException
	{
		return find(Source.class, id);
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public Source loadSourceByIdForMerge(Integer id) throws PersistenceException
	{
		Source dSource = find(Source.class, id);
		Iterator <Asset> iter =  dSource.getAssets().iterator();
		while (iter.hasNext()) {
			Asset dAsset = iter.next();
			Iterator <AssetUse> auIter = dAsset.getAssetUses().iterator();
			while (auIter.hasNext()) {
				auIter.next();
			}
		}

		return dSource;
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public Source loadSourceByIdForView(Integer id, boolean preloadContracts) throws PersistenceException {
		Source source = find(Source.class, id);

		// preload stuff
		if (preloadContracts) {
			for (Contract contract : source.getContracts()) {
				contract.getCurrency();
			}
		}

		for (SourceFile file : source.getFiles()) {
			file.getFileName();
		}

		return source;
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public Source loadSourceByExternalId(Integer id) throws PersistenceException
	{
		return loadSourceByExternalId(String.valueOf(id));
	}

	public List<String> loadDisabledSourcePermissionIds() {
		Query q = entityManager.createNativeQuery("select external_id from source where nofly = 1",
			"scalarExternalId");
		@SuppressWarnings("unchecked")
		List<String> permissionIds = q.getResultList();

		return permissionIds;
	}
	
	//Added by Santhosh for REQ0376879
	public String loadLastUpdatedDate() {
		Query q = entityManager.createNativeQuery("select max(last_updated_date) as lastdate from source");
		@SuppressWarnings("unchecked")
		Date lastdate = (Date)q.getSingleResult();
		DateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy");
		String ldate=dateFormat.format(lastdate);
		return ldate;
	}
	//end Added by Santhosh REQ0376879

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public Source loadSourceByExternalId(String externalId) throws PersistenceException
	{
		try {
			TypedQuery<Source> query = entityManager.createQuery(
					"from Source s where s.externalId = ?",
					Source.class);

			query.setParameter(1, externalId);

			return query.getSingleResult();
		}
		catch (NoResultException e) {
			return null;
		}
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public Source loadSourceByName(String name) throws PersistenceException
	{
		try {
			TypedQuery<Source> query = entityManager.createQuery("from Source s where s.name = ?",
					Source.class);
			query.setParameter(1, name);
			List<Source> sources = query.getResultList();
			if (CollectionUtils.isEmpty(sources))
				return null;
			else {
				// return first one
				return sources.get(0);
			}
		}
		catch (NoResultException e) {
			return null;
		}
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<Source> searchSourceByName(String name) throws PersistenceException
	{
		PerfTimer timer = monitor.startTimer("SourceRepository::searchSourceByName");
		try {
		//	TypedQuery<Source> query = entityManager.createQuery("from Source s where UPPER(s.name) like UPPER(:name) and s.disabled=0 ",
		//			Source.class);

			TypedQuery<Source> query = entityManager.createQuery("from Source s where UPPER(s.name) like UPPER(:name) order by s.name ",
					Source.class);

			if (null != name && name.indexOf(",") > -1) {
				String lname = name.substring(0,name.indexOf(",")).trim();
				String fname = "";

				if (name.length() > name.indexOf(",") + 1) {
					fname = name.substring(name.indexOf(",") + 1).trim();
					query = entityManager.createQuery("from Source s where UPPER(s.name) like UPPER(:lname) and UPPER(s.name) like UPPER(:fname) order by s.name ",
						Source.class);
					query.setParameter("lname", "%" + lname + "%");
					query.setParameter("fname", "%" + fname + "%");
				} else {
					query.setParameter("name", "%" + name + "%");
				}

			} else {
				query.setParameter("name", "%" + name + "%");
			}

			return query.getResultList();
		}
		catch (NoResultException e) {
			return null;
		}
		finally {
			timer.stopTimer();
		}
	}

	/**
	 * @param Source  Must be non-null
	 */
	public void deleteSourceIfNotUsed(Source source) throws PersistenceException
	{
		Set<Asset> assets = source.getAssets();
		List<Contract> contracts = source.getContracts();
		List<PurchaseOrder> pos = source.getPurchaseOrders();

		// if not used
		if (CollectionUtils.isEmpty(assets) && CollectionUtils.isEmpty(contracts)
				&& CollectionUtils.isEmpty(pos))
		{
			log.debug("deleteSourceIfNotUsed(): deleting unreferenced source with id [" + source.getName()
					+ "] ...");
			entityManager.remove(source);
		}
		else {
			StringBuilder error = new StringBuilder();
			if (CollectionUtils.isNotEmpty(assets)) {
				// Make sure error message is not over 8k or else redirect fails
				// and message not shown properly in the browser.
				if (assets.size() > 10) {
					error.append(String.valueOf(assets.size()) + " assets");
				}
				else {
					for (Asset asset : assets) {
						error.append(" Asset [description = " + asset.getDescription() + "]");
					}
				}
			}
			if (CollectionUtils.isNotEmpty(contracts)) {
				if (contracts.size() > 10) {
					error.append(String.valueOf(contracts.size()) + " contracts");
				}
				else {
					for (Contract contract : contracts) {
						error.append(" Contract [number = " + contract.getNumber() + "]");
					}
				}
			}
			if (CollectionUtils.isNotEmpty(pos)) {
				if (pos.size() > 10) {
					error.append(String.valueOf(pos.size()) + " purchase orders");
				}
				else {
					for (PurchaseOrder po : pos) {
						error.append(" Purchase Order [id/number = " + po.getId() + "]");
					}
				}
			}

			String errorString = error.toString();
			String msg = "Source [" + source.getName() + "] cannot be deleted since still referenced by: " + errorString;
			throw new PersistenceException(msg);
		}
	}

	protected List<CompCopy> getCompCopiesByAddressId(Long addressId) throws PersistenceException
	{
		// if we do not cast, it will fail with ClassCastException
		Integer nAddressId = addressId.intValue();

		TypedQuery<CompCopy> query = entityManager.createQuery(
				"select ea from CompCopy ea where addressId = :addressId",
				CompCopy.class);
		query.setParameter("addressId", nAddressId);

		List<CompCopy> eaList = query.getResultList();

		return eaList;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void deleteSourceFiles(Integer sourceId) throws PersistenceException
	{
		Query q = entityManager.createQuery("delete from SourceFile where id = :sourceId");
		q.setParameter("sourceId", sourceId);
		q.executeUpdate();
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void deleteSourceFile(int fileId) throws PersistenceException
	{
		Query q = entityManager.createQuery(
				"delete from SourceFile where id = ?");
		q.setParameter(1, fileId);
		q.executeUpdate();
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<Source> getAllEnabledSourcesForCW(Integer cwId) throws PersistenceException
	{
		String sql = "select * from source join "
			+ "	(select distinct source_id from asset_2_source join asset_use on asset_2_source.asset_id = asset_use.asset_id "
			+ "		where cw_id = ?) a on source.id = a.source_id where source.nofly = 0 order by name";
		Query query = entityManager.createNativeQuery(sql, Source.class);
		query.setParameter(1, cwId);
		// no need to get noDups as we do select distinct,
		// and also creating a HashSet will not preserve the sorting
		@SuppressWarnings("unchecked")
		List<Source> list = query.getResultList();
		return list;
	}

	/**
	 * Sets the disabled flag (nofly) as !current value.
	 * REQUIRES_NEW ensures commit is done right away.
	 *
	 * @param sourceExtId
	 * @throws Exception
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void resetDisabledSourceFlag(String sourceExtId) throws Exception
	{
		// system source cannot be reset
		if (sourceExtId.equals(Source.WILEY_EXT_ID)) return;
		Source source = loadSourceByExternalId (sourceExtId);  // throws PersistenceException
		source.setNofly(!source.isNofly());
		entityManager.merge(source);
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<DeliveryMethod> loadDeliveryMethodList() throws PersistenceException
	{
		List<DeliveryMethod> list = loadAll(DeliveryMethod.class, null);
		return list;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public boolean mergeSources(Integer fromSourceId, Integer toSourceId, Integer userId)
	throws PersistenceException, ServiceException
	{
		Source fromSource = loadSourceById(fromSourceId);
		Source toSource = loadSourceById(toSourceId);
		// first check if sources are in different groups. If that is the case,
		// just throw an exception and do not allow the merge
		Query source_group = entityManager.createNativeQuery(
				"select if(s1.source_group = s2.source_group, 1, 0) as count from source s1, source s2 where s1.id=? and s2.id=? and " +
				" s1.source_group is not null and s2.source_group is not null", "scalarCount");
		source_group.setParameter(1, fromSourceId);
		source_group.setParameter(2, toSourceId);
		// For count, DB2 and MS SQL Server return Integer but MySQL returns BigInteger
		// Integer and BigInteger inherit from Number
		Number result = null;
		try {
			result = (Number) source_group.getSingleResult();
		} catch (NoResultException e) {
			result = null;
		}
		log.debug ("mergeSources(): compare the source groups " + result);
		// if they are equal or not both have groups (result == null), we continue
		if ((null != result && result.intValue() == 0))
			throw new ServiceException ("Cannot merge the source because they are part of different groups");

	// currently we cannot merge sources unless they have the same source group
	//	source_group = entityManager.createNativeQuery(
	//			" update source set source_group = (select * from (select max(source_group) from source where id=? or id=?) as t) where id=?");
	//	source_group.setParameter(1, fromSourceId);
	//	source_group.setParameter(2, toSourceId);
	//	source_group.setParameter(3, toSourceId);
	//	source_group.executeUpdate();

		Query asset_2_source = entityManager.createNativeQuery(
				"update asset_2_source as1 set as1.source_id = ? where as1.source_id = ? and " +
				" as1.asset_id not in (select * from (select as2.asset_id from asset_2_source as2 where as2.source_id=?) as t)");
		asset_2_source.setParameter(1, toSourceId);
		asset_2_source.setParameter(2, fromSourceId);
		asset_2_source.setParameter(3, toSourceId);
		asset_2_source.executeUpdate();

		asset_2_source = entityManager.createNativeQuery("delete from asset_2_source where source_id = ?");
		asset_2_source.setParameter(1, fromSourceId);
		asset_2_source.executeUpdate();

		Query asset_group = entityManager.createNativeQuery(
				"update asset_group set source_id = ? where source_id = ? ");
		asset_group.setParameter(1, toSourceId);
		asset_group.setParameter(2, fromSourceId);
		asset_group.executeUpdate();

		Query au_source_perm_status = entityManager.createNativeQuery(
				"update au_source_perm_status set source_id = ? where source_id = ? and " +
				" asset_use_id not in (select * from (select as2.asset_use_id from au_source_perm_status as2 where as2.source_id=?) as t)");
		au_source_perm_status.setParameter(1, toSourceId);
		au_source_perm_status.setParameter(2, fromSourceId);
		au_source_perm_status.setParameter(3, toSourceId);
		au_source_perm_status.executeUpdate();

		au_source_perm_status = entityManager.createNativeQuery("delete from au_source_perm_status where source_id = ?");
		au_source_perm_status.setParameter(1, fromSourceId);
		au_source_perm_status.executeUpdate();

		Query user_2_source = entityManager.createNativeQuery(
				"update user_2_source set source_id = ? where source_id = ? and " +
				" user_id not in (select * from (select as2.user_id from user_2_source as2 where as2.source_id=?) as t)");
		user_2_source.setParameter(1, toSourceId);
		user_2_source.setParameter(2, fromSourceId);
		user_2_source.setParameter(3, toSourceId);
		user_2_source.executeUpdate();

		user_2_source = entityManager.createNativeQuery("delete from user_2_source where source_id = ?");
		user_2_source.setParameter(1, fromSourceId);
		user_2_source.executeUpdate();

		Query asset_perm_ref = entityManager.createNativeQuery(
				"update asset_perm_ref set source_id = ? where source_id = ? and " +
				" asset_use_id not in (select * from (select as2.asset_use_id from asset_perm_ref as2 where as2.source_id=?) as t)");
		asset_perm_ref.setParameter(1, toSourceId);
		asset_perm_ref.setParameter(2, fromSourceId);
		asset_perm_ref.setParameter(3, toSourceId);
		asset_perm_ref.executeUpdate();

		asset_perm_ref = entityManager.createNativeQuery("delete from asset_perm_ref where source_id = ?");
		asset_perm_ref.setParameter(1, fromSourceId);
		asset_perm_ref.executeUpdate();

		Query contact = entityManager.createNativeQuery(
		"update contact set source_id = ? where source_id = ? ");
		contact.setParameter(1, toSourceId);
		contact.setParameter(2, fromSourceId);
		contact.executeUpdate();

		Query contract = entityManager.createNativeQuery(
				"update contract set source_id = ? where source_id = ? ");
		contract.setParameter(1, toSourceId);
		contract.setParameter(2, fromSourceId);
		contract.executeUpdate();

	//	Query merge = entityManager.createNativeQuery(
	//	"update global_assets_2merge set source_id = ? where source_id = ? ");
	//	merge.setParameter(1, toSourceId);
	//	merge.setParameter(2, fromSourceId);
	//	merge.executeUpdate();

		Query purchase_order = entityManager.createNativeQuery(
				"update purchase_order set source_id = ? where source_id = ? ");
		purchase_order.setParameter(1, toSourceId);
		purchase_order.setParameter(2, fromSourceId);
		purchase_order.executeUpdate();

		Query source_file = entityManager.createNativeQuery(
				"update source_file set source_id = ? where source_id = ? ");
		source_file.setParameter(1, toSourceId);
		source_file.setParameter(2, fromSourceId);
		source_file.executeUpdate();

		Query contacts = entityManager.createNativeQuery(
		"update contact set source_id = ? where source_id = ? ");
		contacts.setParameter(1, toSourceId);
		contacts.setParameter(2, fromSourceId);
		contacts.executeUpdate();


		Query source2address = entityManager.createNativeQuery(
				"update source_2_address, address  set source_2_address.source_id = ? where source_id = ? and " +
				" source_2_address.address_id = address.id and " +
				" address_type_code not in (select * from (select address_type_code " +
				" from source_2_address, address where source_id = ? and source_2_address.address_id =  address.id) as t)" );
		source2address.setParameter(1, toSourceId);
		source2address.setParameter(2, fromSourceId);
		source2address.setParameter(3, toSourceId);
		source2address.executeUpdate();

		// addresses still associated with old source need to be tracked so that they can later be deleted
		// if the destination source already has an address of the same type as the from source,
		// then the from source address it is not moved to the destination source.
		Query addressIdSql = entityManager.createNativeQuery("select address_id from source_2_address "
			 +	" where source_id in (select source_id from source_2_address where source_id = " + fromSourceId + ")");
		@SuppressWarnings("unchecked")
		List<Integer> addressIds = addressIdSql.getResultList();

		Query s2a = entityManager.createNativeQuery(
				"delete from source_2_address where source_id = ?");
		s2a.setParameter(1, fromSourceId);
		s2a.executeUpdate();

		// now delete addresses that were not transfered;
		if (null != addressIds && addressIds.size() > 0) {
		/*	for (int x = 0; x < addressIds.size(); x++) {
				try {
					//Issue No Foreign Key Violation on table CONTACT
					Query c2a = entityManager.createNativeQuery(
							"delete from contact where address_id = ?");
					c2a.setParameter(1, addressIds.get(x));
					c2a.executeUpdate();
					//Ends
					Query address = entityManager.createNativeQuery(
							"delete from address where id = ?");
					address.setParameter(1, addressIds.get(x));
					address.executeUpdate();
				} catch (Exception e) {
					// previously transfered so not found to delete
					log.debug(e.getMessage());
				}
			}*/
		}

		try {
			Query source_2_favorite_group = entityManager.createNativeQuery(
					"update source_2_favorite_group set source_id = ? where source_id = ? ");
			source_2_favorite_group.setParameter(1, toSourceId);
			source_2_favorite_group.setParameter(2, fromSourceId);
			source_2_favorite_group.executeUpdate();
		} catch (Exception e) {
			log.debug(e.getMessage());
		}

		// now transfer the meta data that is in the source and not in the destination

		if (!toSource.isNofly() && fromSource.isNofly()) {
			toSource.setNofly(true);
		}
		if (toSource.getCountry() == null && null != fromSource.getCountry()) {
			toSource.setCountry(fromSource.getCountry());
		}
		if (toSource.getCreditLine() == null || toSource.getCreditLine().trim().length() < 1) {
			if(null != fromSource.getCreditLine()) {
				toSource.setCreditLine(fromSource.getCreditLine());
			}
		}
		if (toSource.getFaxNumber() == null && null != fromSource.getFaxNumber()) {
			toSource.setFaxNumber(fromSource.getFaxNumber());
		}
		if (toSource.getJdeVendorNumber() == null && null != fromSource.getJdeVendorNumber()) {
			toSource.setJdeVendorNumber(fromSource.getJdeVendorNumber());
		}
		if (toSource.getNoFlyPhotographerLastName() == null && null != fromSource.getNoFlyPhotographerLastName()) {
			toSource.setNoFlyPhotographerLastName(fromSource.getNoFlyPhotographerLastName());
		}
		if (toSource.getPermissionRequestUrl() == null && null != fromSource.getPermissionRequestUrl()) {
			toSource.setPermissionRequestUrl(fromSource.getPermissionRequestUrl());
		}
		if (toSource.getPhoneNumber() == null && null != fromSource.getPhoneNumber()) {
			toSource.setPhoneNumber(fromSource.getPhoneNumber());
		}
		if (toSource.getSourceGroup() == null && null != fromSource.getSourceGroup()) {
			toSource.setSourceGroup(fromSource.getSourceGroup());
		}
		if (toSource.getWebsite() == null && null != fromSource.getWebsite()) {
			toSource.setWebsite(fromSource.getWebsite());
		}
		if (toSource.getComment() == null || toSource.getComment().trim().length() < 1) {
			if (null != toSource.getComment()) {
				toSource.setComment(fromSource.getComment());
			}
		}

		merge(toSource);


		Query source = entityManager.createNativeQuery(
				"delete from source where id = ? ");
		source.setParameter(1, fromSourceId);
		source.executeUpdate();

		// if there where no errors update the transfer history table

		updateSourceTransferHistory(fromSource, toSource, userId);

		return true;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public SourceGroup loadSourceGroupById(Integer id) throws Exception
	{
		SourceGroup sg = lazyLoad(SourceGroup.class, id, new String[] { "masterAgreementDeals", "sources" });
		for (RoyaltyFreeDeal deal : sg.getRoyaltyFreeDeals()) {
			deal.setIsBeingUsed(isRoyaltyFreeDealUsedByContract(deal.getId()));
		}
		for (MasterAgreementDeal maDeal : sg.getMasterAgreementDeals()) {
			maDeal.getBusinessUnitsDisplay();
			maDeal.getUserLocationsDisplay();
		}
		return sg;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public FavoriteGroup loadFavoriteSourceGroupById(Integer id) throws Exception
	{
		FavoriteGroup fg = lazyLoad(FavoriteGroup.class, id, new String[] { "sources" });
		return fg;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public List<SourceGroup> loadSourceGroupList() throws PersistenceException
	{
		TypedQuery<SourceGroup> query = entityManager.createQuery(
				"select distinct sg from SourceGroup sg " +
				"left join fetch sg.masterAgreementDeals", SourceGroup.class);
		return query.getResultList();
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public List<Source> getSourcesInSourceGroup(Integer sourceGroupId) throws PersistenceException
	{
		String sql = "select * from source where source_group = ? "
			+ " order by name";
		Query query = entityManager.createNativeQuery(sql, Source.class);
		query.setParameter(1, sourceGroupId);
		@SuppressWarnings("unchecked")
		List<Source> list = query.getResultList();
		return list;
	}


	/**
	 * @param SourceGroup  Must be non-null
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public void deleteSourceGroupIfNotUsed(SourceGroup sourceGroup) throws PersistenceException
	{
		Query q = entityManager.createQuery("update Source s set s.sourceGroup = null where s.sourceGroup.id = :groupId");
		q.setParameter("groupId", sourceGroup.getId());
		q.executeUpdate();

		try {
			Query q2 = entityManager.createQuery("delete from RoyaltyFreeDeal d where d.sourceGroupId = :groupId");
			q2.setParameter("groupId", sourceGroup.getId());
			q2.executeUpdate();
		} catch (Exception e) {
			// ignore
			log.error("deleteSourceGroupIfNotUsed(): SQL error: ", e);
		}

		try {
			Query q2 = entityManager.createQuery("delete from MasterAgreement ma where ma.sourceGroupId = :groupId");
			q2.setParameter("groupId", sourceGroup.getId());
			q2.executeUpdate();
		} catch (Exception e) {
			// ignore
			log.error("deleteSourceGroupIfNotUsed(): SQL error: ", e);
		}

		Query q3 = entityManager.createQuery("delete from SourceGroup where id = :groupId");
		q3.setParameter("groupId", sourceGroup.getId());
		q3.executeUpdate();
	}

	/**
	 * @param Source  Must be non-null
	 */
	public SourceGroup deleteSourceGroupChild(Integer child, SourceGroup sourceGroup) throws PersistenceException
	{
		// currently not used
		throw new RuntimeException("deleteSourceGroupChild() not implemented");
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void deleteRoyaltyFreeDeal(Integer dealId) throws PersistenceException
	{
		Query q = entityManager.createQuery("delete from RoyaltyFreeDeal where id = ?");
		q.setParameter(1, dealId);
		q.executeUpdate();
	}

	/**
	 * Returns true if the given RoyaltyFree Deal id is referenced by a contract.
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly=true)
	public boolean isRoyaltyFreeDealUsedByContract(Integer dealId) {
		final String sql = "select count(*) as count from contract_2_asset where rfdeal_id = ?";
		Query query = entityManager.createNativeQuery(sql, "scalarCount");
		query.setParameter(1, dealId);
		// For count, DB2 and MS SQL Server return Integer but MySQL returns BigInteger
		// Integer and BigInteger inherit from Number
		Number result = (Number) query.getSingleResult();
		return result.intValue() > 0;
	}

	/**
	 * Returns true if the given Master Agreement id is referenced by a contract.
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly=true)
	public boolean isMasterAgreementUsedByContract(Integer masterAgreementId) {
		final String sql = "select count(*) as count from contract where ma_deal_id = ?";
		Query query = entityManager.createNativeQuery(sql, "scalarCount");
		query.setParameter(1, masterAgreementId);
		// For count, DB2 and MS SQL Server return Integer but MySQL returns BigInteger
		// Integer and BigInteger inherit from Number
		Number result = (Number) query.getSingleResult();

		return result.intValue() > 0;
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly=true)
	public boolean hasInUseRoyaltyFreeDeals(SourceGroup group) {
		for (int x = 0; x < group.getRoyaltyFreeDeals().size(); x++) {
			if (isRoyaltyFreeDealUsedByContract(group.getRoyaltyFreeDeals().get(x).getId() ) ) return true;
		}
		return false;
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly=true)
	public boolean hasInUseMasterAgreements(SourceGroup group) {
		// smarkoff: alternative: could take just sourceGroupId as input and do something like
		// select count(*) as count from contract c, master_agreement_deal ma where ma.source_group_id = ? and ma.id = c.ma_deal_id
		// but the following is fine - calling method in SourceGroupMainController needs to load list anyway for other reasons

		List<MasterAgreementDeal> mas = group.getMasterAgreementDeals();
		for (MasterAgreementDeal ma : mas) {
			if (isMasterAgreementUsedByContract(ma.getId())) return true;
		}
		return false;
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly=true)
	public boolean doesSourceGroupHaveMasterAgreements(int sourceGroupId) {
		final String sql = "select count(*) as count from master_agreement_deal where source_group_id = ?";
		Query query = entityManager.createNativeQuery(sql, "scalarCount");
		query.setParameter(1, sourceGroupId);
		// For count, DB2 and MS SQL Server return Integer but MySQL returns BigInteger
		// Integer and BigInteger inherit from Number
		Number result = (Number) query.getSingleResult();
		return result.intValue() > 0;
	}
	
	//Start: Added to implement DM-1185
		@Transactional(propagation = Propagation.REQUIRED, readOnly=true)
		public boolean doesSourceGroupHaveMasterAgreements(int sourceGroupId, Date date) {
			log.debug("doesSourceGroupHaveMasterAgreements(sourceGroupId,date): sourceGroupId = " + sourceGroupId + ", date = "+date);
			if(null != date && sourceGroupId > 0) {
				final String sql = "select count(*) as count from master_agreement_deal mad where mad.source_group_id = ? and mad.start_date <= ? and (mad.end_date is null or mad.end_date >= ?)";
				Query query = entityManager.createNativeQuery(sql.toString(), "scalarCount");

				query.setParameter(1, sourceGroupId);
				query.setParameter(2, date);
				query.setParameter(3, date);

				Number result = (Number) query.getSingleResult();
				return result.intValue() > 0;
			} else {
				return false;
			}
		}
		//End: Added to implement DM-1185

	@Transactional(propagation = Propagation.REQUIRED)
	public RoyaltyFreeDeal loadRoyaltyFreeDealById(Integer id) throws PersistenceException
	{
		return find(RoyaltyFreeDeal.class, id);
	}

	/**
	 * Returns false if the deal could not be deleted because it's referenced by a contract.
	 * Otherwise returns true (may also throw an exception for an unexpected situation).
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public boolean removeMasterAgreementDeal(int id) throws PersistenceException
	{
		if (isMasterAgreementUsedByContract(id)) {
			return false;
		}

		MasterAgreementDeal deal = find(MasterAgreementDeal.class, id);
		if (null != deal) {
			getEntityManager().remove(deal);
		}
		return true;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public SourceGroup saveSourceGroup(SourceGroup group, List<Integer> removedSourceIds) throws PersistenceException
	{
	//	List<RoyaltyFreeDeal> dealsList2 = group.getRoyaltyFreeDeals();
		if (null == group.getId() || group.getId() == 0) {
			group = getEntityManager().merge(group);
		}
		List<Source> sourceList2 = new ArrayList<Source>();
		sourceList2.addAll(group.getSources());

		for (Source source : sourceList2) {
			source.setSourceGroup(group);
			source.setNofly(group.isNofly());
			source = getEntityManager().merge(source);
		}

		for (Integer sourceId : removedSourceIds) {
			voidSourceGroup(sourceId);  // throws PersistenceException
		}

	//	for(RoyaltyFreeDeal deal : dealsList2) {
	//		deal.setSourceGroupId(group.getId());
	//		deal = saveDeal(deal);
	//		log.debug("saveSourceGroup - just saved deal:" + deal);
	//
	//	}

		return group;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public FavoriteGroup saveFavoriteGroup(FavoriteGroup group) throws PersistenceException
	{
		return getEntityManager().merge(group);
	}


	@Transactional(propagation = Propagation.REQUIRED)
	public void voidSourceGroup(Integer sourceId) throws PersistenceException
	{
		Query q = getEntityManager().createQuery("update Source s set s.sourceGroup = null where s.id = ?");
		q.setParameter(1, sourceId);
		q.executeUpdate();
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void updateSourceTransferHistory(Source fromSource, Source toSource, Integer userId) throws PersistenceException
	{
		// first insert the current transfer row
		Query q = getEntityManager().createNativeQuery("insert into source_transfer_history(original_Source_id, original_external_id," +
				"original_external_name, new_external_name, current_source_id, update_user_Id, created_date) values(?,?,?,?,?,?,? )");
		q.setParameter(1, fromSource.getId());
		q.setParameter(2, fromSource.getExternalId());
		q.setParameter(3, fromSource.getExternalName());
		q.setParameter(4, toSource.getExternalName());
		q.setParameter(5, toSource.getId());
		q.setParameter(6, userId);
		q.setParameter(7, new Date());
		q.executeUpdate();

		// then update any previously inserted rows with the latest source_id
		Query q2 = getEntityManager().createNativeQuery("update source_transfer_history set current_source_id = ? " +
				" where current_source_id = ?");
		q2.setParameter(1, toSource.getId());
		q2.setParameter(2, fromSource.getId());
		q2.executeUpdate();
	}

	public Integer getCurrentSourceIdFromTransfersByExternalID(String externalId) throws PersistenceException {
		Query query = entityManager.createNativeQuery(
				"select current_source_id as id from source_transfer_history where original_external_id = ? ");

		query.setParameter(1, externalId);

		Integer sourceId = 0;
		try {
			sourceId = (Integer) query.getSingleResult();
		} catch (NoResultException e) {
			return -1;
		}

		return sourceId;
	}

	public Integer getCurrentSourceIdFromTransfersByName(String externalName) throws PersistenceException {
		Query query = entityManager.createNativeQuery(
				"select current_source_id as id from source_transfer_history where original_external_name = ? ");
		query.setParameter(1, externalName);
		Integer sourceId = 0;
		try {
			sourceId = (Integer) query.getSingleResult();
		} catch (NoResultException e) {
			return -1;
		}

		return sourceId;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public SourceGroup mergeSourceGroup(SourceGroup group) throws PersistenceException
	{
		return getEntityManager().merge(group);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public RoyaltyFreeDeal saveDeal(RoyaltyFreeDeal deal) throws PersistenceException
	{
		log.debug("about to save deal:" + deal);
		if (null == deal.getId()) {
			getEntityManager().persist(deal);
			return deal;
		} else {
//			RoyaltyFreeDeal updateDeal = userRepository.find(RoyaltyFreeDeal.class, deal.getId());
//			updateDeal.setDisabledFlag(deal.getDisabledFlag());
//			updateDeal.setSeats(deal.getSeats());
//			updateDeal.setTotalPrintRun(deal.getTotalPrintRun());

			// 0 means unlimited now
			if (null == deal.getTotalPrintRun() || deal.getTotalPrintRun() < 0 ) {
				deal.setTotalPrintRun(0);
			}
			if (null == deal.getSeats() || deal.getSeats() < 0 ) {
				deal.setSeats(0);
			}
			return getEntityManager().merge(deal);
		//	return userRepository.merge(updateDeal);
		}
	}

	@SuppressWarnings("unchecked")
	@Transactional(propagation = Propagation.REQUIRED, readOnly=true)
	public List<Integer> loadiStockphotoSourceIds() {
		final String sql = "select source.id from source join source_group on " +
				"source.source_group = source_group.id and UPPER(TRIM(source_group.name))='ISTOCKPHOTO'";
		Query query = entityManager.createNativeQuery(sql, "scalarId");
		return query.getResultList();
	}

	@SuppressWarnings("unchecked")
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<Source> loadMasterAgreementSources() throws PersistenceException
	{
		final String sql = "select s.* from source s where s.source_group in( " +
				"		select sg.id from source_group sg where sg.id in( " +
				"		select ma.source_group_id from master_agreement_deal ma)) ";
		Query query = entityManager.createNativeQuery(sql, Source.class);

		return query.getResultList();
	}

	@SuppressWarnings("unchecked")
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<Source> loadPreferedVendors() throws PersistenceException
	{
		final String sql = "select s.* from source s where s.permission_type = 'Preferred Vendor' ";
		Query query = entityManager.createNativeQuery(sql, Source.class);
		return query.getResultList();
	}

	@SuppressWarnings("unchecked")
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<Source> loadDisabledSources() throws PersistenceException
	{
		final String sql = "select s.* from source s where nofly = 1 order by s.name ";
		Query query = entityManager.createNativeQuery(sql, Source.class);
		return query.getResultList();
	}

	@SuppressWarnings("unchecked")
	public List<Source> searchNoflyPhotographer(String term) {

		StringBuilder sqlB = new StringBuilder("select s.* from source s where (nofly = 1 and MATCH(nofly_photographer_last_name) against ('"
				+ term + "' IN NATURAL LANGUAGE MODE))");
			if(StringUtils.isNotBlank(term)) {
				sqlB = sqlB.append(" or (instr(name,'"	+ term.trim() + "') and nofly=1)");
			}
			final String sql = sqlB.toString();
			Query query = entityManager.createNativeQuery(sql, Source.class);
			return query.getResultList();
    }

	public Source loadSourceUsingTransferHistory(Source source) throws PersistenceException {
		// first check to see if in transfers table

		Integer sourceId = -1;
		if (null != source.getExternalId()) {
			sourceId = getCurrentSourceIdFromTransfersByExternalID(source.getExternalId());
		}
		if (sourceId == -1 && null != source.getExternalName()) {
			sourceId = getCurrentSourceIdFromTransfersByName(source.getExternalName());
		}

		log.debug("loadSourceUsingTransferHistory(): source id found in transfers: " + sourceId);
		if (sourceId != -1) {
			try {
				return(loadSourceById(sourceId));
			} catch (Exception e) {
				return loadSourceByTemplate(source);
			}
		}

		return loadSourceByTemplate(source);
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public Address loadAddressById(Integer addressId) throws PersistenceException
	{
		return find(Address.class, addressId);
	}

	/**
	 * Throws an exception if the source can't be deleted
	 * due to existing an existing foreign key constraint or other reasons.
	 *
	 * @param externalId
	 * @throws PersistenceException
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void deleteSourceByExternalId(String externalId) throws PersistenceException
	{
		Source source = loadSourceByExternalId(externalId);

		// throws PersistenceException if the source can't be deleted
		if (source != null)  deleteSourceIfNotUsed(source);
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public Address loadAddressOfType(Integer sourceId, AddressType type) throws Exception {
		Source source = lazyLoad (Source.class, sourceId, new String[] {"addresses"});
		List<SourceAddress> sourceAddress = source.getAddresses();

		for (SourceAddress address : sourceAddress) {
			if (address.getAddress().getType().equals(type))
				return address.getAddress();
		}
	    return null;
    }

	/**
	 * Returns all addresses associated with an Source
	 *
	 * @param source
	 * @return List<Address>
	 * @throws Exception
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public List<Address> getMailToAddresses(Source source) throws Exception
	{
		List<Address> addresses = new ArrayList<Address>();
		source = lazyLoad (Source.class, source.getId(), new String[] {"addresses"});
		List<SourceAddress> sourceAddress = source.getAddresses();

		for (SourceAddress address : sourceAddress) {
			addresses.add(address.getAddress());
		}

		log.debug("getMailToAddresses(): addresses.size(): " + addresses.size());

		return addresses;
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public Source getSourceView(Integer sourceId) throws PersistenceException {
		Source source = loadSourceByIdForView(sourceId, false);
		source.getContracts();
		source.getContacts();
		source.getAddresses();

		for (Contract contract: source.getContracts()) {
			// Assume currency is the same for all contracts (it should be).
			contract.getCurrency().getCode();
				for (ContractAsset contractAsset: contract.getAssets()) {
					contractAsset.getAssetBase();
				}
		}

		for (Contact contact: source.getContacts()) {
			contact.getAddress();
		}

		for (SourceAddress address: source.getAddresses()) {
			address.getAddress();
		}

		return source;
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public Source getSourceMergeView(Integer sourceId) throws PersistenceException {
		Source source = loadSourceById(sourceId);
		source.getContracts();
		source.getContacts();
		source.getAddresses();

		for(Contract contract: source.getContracts()) {
			contract.getId();
		}

		for(Contact contact: source.getContacts()) {
			contact.getAddress();
		}

		for(SourceAddress address: source.getAddresses()) {
			address.getAddress();
		}

		for(Asset asset: source.getAssets()) {
			asset.getDescription();
		}

		if (null != source.getSourceGroup()) {
			source.getSourceGroup().getId();
		}

		return source;
	}


	/**
	 * This method may be called for a creating or updating the Address for an
	 * Source.
	 *
	 * @param sourceId
	 *            Must be non-null
	 * @param address
	 *            Must be non-null
	 * @return
	 * @throws PersistenceException
	 * @throws DuplicateAddressTypeException
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public SourceAddress saveSourceAddress(int sourceId, Address address)
			throws PersistenceException, DuplicateAddressTypeException
	{
		// Note address.getId() will be null for a new address

		// If matches type of an existing address, then report a duplicate
		// address type error.
		SourceAddress workAddress = loadSourceAddressOfType(sourceId, address.getType());
		if (workAddress != null) {
			Integer workId = workAddress.getAddress().getId();
			if (address.getId() == null || !workId.equals(address.getId())) {
				throw new DuplicateAddressTypeException(workAddress.getAddress().getId(), true);
			}
		}

		log.debug("Merging Address: " + address);
		address = entityManager.merge(address);
		log.debug("After Merge: " + address);

		SourceAddress output = loadSourceAddressByIds(sourceId, address.getId());

		if (output == null) {
			output = new SourceAddress();
			output.setAddress(address);
			Source source = find(Source.class, sourceId);
			output.setSource(source);
		}

		entityManager.persist(output);

		return output;
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public SourceAddress loadSourceAddressOfType(int sourceId, AddressType type)
			throws PersistenceException
	{
		TypedQuery<SourceAddress> q = entityManager.createQuery(
				"from SourceAddress where source.id = :sourceId and address.type.code = :typeCode",
				SourceAddress.class);

		q.setParameter("sourceId", sourceId);
		q.setParameter("typeCode", type.getCode());

		try {
			return q.getSingleResult();
		}
		catch (NoResultException e) {
			return null;
		}
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public SourceAddress loadSourceAddressByIds(int sourceId, Integer addressId)
			throws PersistenceException
	{
		TypedQuery<SourceAddress> q = entityManager.createQuery(
				"from SourceAddress where source.id = :sourceId and address.id = :addressId",
				SourceAddress.class);

		q.setParameter("sourceId", sourceId);
		q.setParameter("addressId", addressId);

		try {
			return q.getSingleResult();
		}
		catch (NoResultException e) {
			return null;
		}
		catch (Exception e) {
			throw new PersistenceException("Cannot Find Source Address: " + sourceId + ", "
					+ addressId, e);
		}
	}

	/**
	 * This method is much faster than loadSourceListWithDisabledFlag(filter)
	 * so use it if all you need is the SourceSummary.
	 * Returns a list of SourceSummary with disabled flag set.
	 * This method does not order by source name - it's assumed that the calling
	 * method will sort as desired. Filter is done case-insensitively.
	 *
	 * @param filter  If blank then not used
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<SourceSummaryView> loadSourceSummaryListWithDisabledFlag(String filter) throws PersistenceException, SQLException
	{
		PerfTimer timer = monitor.startTimer("SourceRepository::loadSourceSummaryListWithDisabledFlag");
		List<SourceSummaryView> list = loadSourceSummaryListByName(filter);

		List<String> permissionIds = loadDisabledSourcePermissionIds();
		for (SourceSummaryView es : list) {
			if (permissionIds.contains(es.getPermissionsId())) {
				//log.debug("loadSourceSummaryListWithDisabledFlag() disabled: " + es.getName());
				es.setDisabled(true);
			}
		}
		timer.stopTimer();
		return list;
	}

	/**
	 * This method is much faster than loadSourceListByName(filter) so use it
	 * if all you need is the SourceSummary.
	 * This method does not order by source name - it's assumed that the calling
	 * method will sort as desired. Filter is done case-insensitively.
	 *
	 * @param filter  If blank then not be used
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<SourceSummaryView> loadSourceSummaryListByName(String filter) throws SQLException
	{
		if (null != filter) {
			if (filter.indexOf("'") > -1) {
				filter = filter.replace("'", "''");
			}

			if (filter.indexOf("%") > -1) {
				filter = filter.replace("%", "%%");
			}
		}
		Map<String, String> countriesMap = new HashMap<String, String> ();
		try {
			List<Country> countries = loadCountryList();
			for (Country country : countries) {
				countriesMap.put(country.getCode(), country.getDescription());
			}
		} catch (PersistenceException e) {
			throw new SQLException ("Failed to load countries", e);
		}

		String sql = "select s.name, s.external_id, s.country_code, s.jde_vendor_number,"
			+ "a.line_one, a.line_two, a.line_three, a.city, a.state_province, a.postal_code, "
			+ "s.source_group, s.nofly, sg.name, "
			+ " (select count(*) from contact where source_id = s.id), "
			+ " (select count(*) from master_agreement_deal where source_group_id = sg.id) "
			+ " from source s " +
					" left join source_2_address s2a on s2a.source_id = s.id " +
					" left join address a on s2a.address_id = a.id " +
					" left join source_group sg on s.source_group = sg.id where a.address_type_code='A' ";
		if (StringUtils.isNotBlank(filter)) {
			if (filter.indexOf("^") < 0) {
				sql += " and lower(s.name) like '" + "%" + filter.toLowerCase() + "%'";
			} else {
				sql += " and lower(s.name) REGEXP '" + filter.toLowerCase() + "'";
			}

		}
		sql += " order by s.name";
		// log.debug(sql);

		Connection con = getDataSource().getConnection();
		PreparedStatement ps = null;

		List<SourceSummaryView> rows = new ArrayList<SourceSummaryView>();
		try {
			ps = con.prepareStatement(sql);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				SourceSummaryView row = new SourceSummaryView();
				row.setName(rs.getString(1));
				row.setPermissionsId(rs.getString(2));
				row.setCountry(rs.getString(3));
				row.setVendor(rs.getString(4));
				Address a = new Address();
				a.setLineOne(rs.getString(5));
				a.setLineTwo(rs.getString(6));
				a.setLineThree(rs.getString(7));
				a.setCity(rs.getString(8));
				a.setProvince(rs.getString(9));
				a.setPostalCode(rs.getString(10));
				if(null != countriesMap && countriesMap.size()> 0) {
					a.setCountry(new Country(rs.getString(3),countriesMap.get(rs.getString(3))) );
				}
				row.setMainAddress(a);
				if (null != rs.getString(13) && !rs.getString(13).equals("null")) {
					row.setSourceGroup(rs.getString(13));
				} else {
					row.setSourceGroup(" ");
				}
				row.setNofly(rs.getBoolean(12) ? "Yes" : " ");
				Integer maCount = new Integer(rs.getLong(15) + "");
				row.setMasterAgreement(maCount > 0 ? "Yes" : "No");

				row.setNumberOfContacts(new Integer(rs.getLong(14) + ""));
				rows.add(row);
			}
			return rows;
		} catch (Exception e) {
			log.error("loadSourceSummaryListByName(): caught exception: ", e);
			return rows;
		}
		finally {
			if (ps != null) ps.close();
			con.close();
		}
	}
	//Added for DM-532
	@SuppressWarnings("unchecked")
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public boolean isNoFly(String name) throws PersistenceException
	{
		log.debug("in is NoFly--------->"+name);
		final String sql ="select nofly from source s where name = ?";
		Query query = entityManager.createNativeQuery(sql);
		query.setParameter(1, name);
		List results=query.getResultList();
		if(results==null || results.isEmpty()) {
			return false;
		}
		/*Boolean nofly = (Boolean)query.getSingleResult();
		log.debug("Value--------->"+nofly);
		return nofly;*/
		log.debug("Value--------->"+(boolean)results.get(0));
		return (boolean) results.get(0);
	}
	
	@SuppressWarnings("unchecked")
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public boolean isNoFlyArtist(String name) throws PersistenceException
	{
		final String sql ="select nofly from source s where nofly_photographer_last_name = ?";
		Query query = entityManager.createNativeQuery(sql);
		query.setParameter(1, name);
		//Boolean nofly = (Boolean)query.getSingleResult();
		//return nofly;
		List results=query.getResultList();
		if(results==null || results.isEmpty()) {
			return false;
		}
		log.debug("Value--------->"+(boolean)results.get(0));
		return (boolean) results.get(0);
	}
	
	//end Added for DM-532
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<Country> loadCountryList() throws PersistenceException
	{
		List<Country> list = loadAll(Country.class, null);
		return list;
	}

	public DataSource getDataSource() {
		return dataSource;
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}
}
