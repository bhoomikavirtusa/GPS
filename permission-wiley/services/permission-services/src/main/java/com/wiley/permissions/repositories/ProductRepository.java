package com.wiley.permissions.repositories;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.wiley.permissions.common.integration.Dataset.Data.Row;
import com.wiley.permissions.domain.persistence.permissions.CommonWork;
import com.wiley.permissions.domain.persistence.permissions.ISBNData;
import com.wiley.permissions.domain.persistence.permissions.ISBNDataTmp;
import com.wiley.permissions.domain.persistence.permissions.Medium;
import com.wiley.permissions.domain.persistence.permissions.Product;
import com.wiley.permissions.domain.persistence.permissions.ProductEdition;
import com.wiley.permissions.domain.persistence.permissions.ProductFamily;
import com.wiley.permissions.domain.persistence.permissions.ProductLine;
import com.wiley.permissions.domain.persistence.permissions.ProductPrinting;
import com.wiley.permissions.domain.persistence.permissions.ProductType;
import com.wiley.permissions.domain.persistence.permissions.PublicationStatus;
import com.wiley.permissions.domain.persistence.permissions.Relation;
import com.wiley.permissions.domain.persistence.permissions.Role;
import com.wiley.permissions.domain.persistence.permissions.SubMedium;
import com.wiley.permissions.domain.persistence.permissions.User;
import com.wiley.permissions.domain.persistence.permissions.UserToRole;
import com.wiley.permissions.domain.util.UniqueIdentifierGenerator;
import com.wiley.permissions.persistence.PersistenceException;
import com.wiley.permissions.persistence.repository.JPARepository;
import com.wiley.permissions.services.ProductIndexService;
import com.wiley.permissions.services.UserService;
import com.wiley.sf.common.lang.ArgUtil;
import com.wiley.sf.common.monitor.PerformanceMonitor.PerfTimer;

/**
 * All Product related database operations
 * The load/find methods can be used directly if wanted
 * The update/save should be used only thru the ProductService interface
 *
 * @author lnagy
 */
public class ProductRepository extends JPARepository {
	private static final Log log = LogFactory.getLog(ProductRepository.class);

	private AssetUseRepository assetUseRepository;
	private UserRepository userRepository;
	private UserService userService;
	private CommonWorkRepository cwRepository;
	private ProductIndexService productIndexService;

	// smarkoff: split into two methods because Rollback sometimes causes exception
	// to be thrown and we lose original exception (logged here)
	@Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
	public void savePartialProduct(Product peProd) throws Exception {
		try {
			PerfTimer timer = monitor.startTimer("ProductRepository::savePartialProduct");
			savePartialProductInner(peProd);
			timer.stopTimer();
		}
		catch (Exception ex) {
			log.error("caught exception from savePartialProductInner(): ", ex);
			throw ex;
		}
	}

	private void savePartialProductInner(Product peProd) throws Exception {
		// lnagy - removed the debugging because it slows down the import considerably
		// debugU2P(); // temp debug for Mantis 4833

		PerfTimer timerFirstBlock = monitor.startTimer("ProductRepository::savePartialProductInner::(first block)");

		Product permProd = loadByExternalId(peProd.getExternalId());
		log.debug("updateProductFromPE(): Does Product exist in Perm DB? - " + (permProd != null));

		CommonWork commonWork = peProd.getCommonWork();
		log.debug("updateProductFromPE(): Did Product come with a CW? - " + (commonWork != null));
		if (commonWork != null) log.debug("commonWork: " + commonWork);

		//boolean getAllProducts = false;

		if (permProd == null) {
			if (commonWork == null) {
				log.debug("updateProductFromPE(): Creating a dummy CW");
				commonWork = new CommonWork();
				commonWork.setCode(UniqueIdentifierGenerator.getNextIdentifier("perm.cw."));
				commonWork.setName(peProd.getTitle());
				peProd.setCommonWork(commonWork);
				peProd.setCwPrimary(true);
				List<Product> products = new ArrayList<Product>(1);
				products.add(peProd);
				commonWork.setProducts(products);
			}
			else {
				log.debug("updateProductFromPE(): received new Product with a CW");
				// do we have the commonWork as specified by PE?
				CommonWork permPE_CW = cwRepository.loadByCode(commonWork.getCode());
				log.debug("Do we already have that CW? - " + (permPE_CW != null));
				if (permPE_CW == null || permPE_CW.getProductCount() == 0) {
					log.debug("updateProductFromPE(): Setting to primary since first product for the CW.");
					peProd.setCwPrimary(true);
				}
				//getAllProducts = true;
			}
		}
		else {
			if (commonWork == null) {
				log.debug("updateProductFromPE(): product came withOUT a CommonWork but using the one we already have.");
				commonWork = permProd.getCommonWork();
				peProd.setCommonWork(commonWork);
			}
			else {
				if (commonWork.equals(permProd.getCommonWork())) {
					log.debug("updateProductFromPE(): product came with a CW that matched what we had for the product.");
					// do nothing
				}
				else {
					log.debug("updateProductFromPE(): product came with a CW ["
							+ commonWork.getCode() + "] that did not match what we had for the product ["
							+ permProd.getCommonWork().getCode() + "].");

					// do we have the commonWork as specified by PE?
					CommonWork permPE_CW = cwRepository.loadByCode(commonWork.getCode());

					if (permProd.getCommonWork().isDummy()) {
						log.debug("updateProductFromPE(): our existing CW for the product was dummy.");

						if (permPE_CW == null) {
							log.debug("updateProductFromPE(): just rename our dummy since we don't have the PE CW.");
							permProd.getCommonWork().setCode(commonWork.getCode());
							permProd.getCommonWork().setName(commonWork.getName());
							//getAllProducts = true;
						}
						else {
							log.debug("updateProductFromPE(): we have the PE CW so move the product and delete the dummy.");
							CommonWork oldCW = permProd.getCommonWork();
							permProd.setCommonWork(permPE_CW);

							int permCWUseCount = assetUseRepository.countAssetUseInCommonWork(oldCW.getId());
							int peCWUseCount = assetUseRepository.countAssetUseInCommonWork(permPE_CW.getId());

							if (permCWUseCount > 0 && peCWUseCount > 0) {
								String msg = "Importing Product with externalId " + permProd.getExternalId()
								+ ": The CW code from PE [" + permPE_CW.getCode()
								+ "] doesn't match our current CW [" + oldCW.getCode()
								+ "] for the product and we currently have some asset uses for both of these CWs.";
								throw new RuntimeException("updateProductFromPE(): " + msg);
							}
							else {
								changeCWRefs(oldCW.getId(), permPE_CW.getId());
							}

							entityManager.remove(oldCW);
						}
					}
					else {
						log.debug("updateProductFromPE(): our existing CW from the product was NOT a dummy.");

						// reminder: We have the Product from PE,
						// but the CW from PE doesn't match our CW for the Product.
						// We may or may not have the CW as specified by PE.
						// We do have our own non-dummy CW for the product.

						int permCWUseCount = assetUseRepository.countAssetUseInCommonWork(permProd.getCommonWork().getId());

						int permProdCWProdCount = permProd.getCommonWork().getProductCount();
						log.debug("updateProductFromPE(): The product count for our existing CW for the product was " + permProdCWProdCount);

						if (permPE_CW == null) {
							log.debug("updateProductFromPE(): We didn't have the CW from PE.");

							if (permProdCWProdCount == 1) {
								log.debug("updateProductFromPE(): We only have one product for our CW so rename our existing CW.");
								// do this regardless of whether the existing CW we have for the product has any Asset Uses
								// since we only have the one product for the existing CW
								permProd.getCommonWork().setCode(commonWork.getCode());
								permProd.getCommonWork().setName(commonWork.getName());
							}
							else {
								log.debug("updateProductFromPE(): We have multiple products for our CW so insert the new CW and change our product to it.");
								commonWork = persist(commonWork);
								permProd.setCommonWork(commonWork);
								/*
								String msg = "Importing Product with externalId " + permProd.getExternalId()
								+ ": The CW code from PE [" + commonWork.getCode()
								+ "] doesn't match our current CW [" + permProd.getCommonWork().getCode()
								+ "] for the product and we currently have more than one product for our CW,"
								+ " and we currently don't have the CW from PE.";
								throw new RuntimeException("updateProductFromPE(): " + msg);
								*/
							}

							//getAllProducts = true;
						}
						else {
							log.debug("updateProductFromPE(): We did have the CW from PE.");

							int peCWUseCount = assetUseRepository.countAssetUseInCommonWork(permPE_CW.getId());

							if (peCWUseCount == 0 && permCWUseCount == 0) {
								log.debug("The AssetUse count for both CWs == 0 so move the product from one CW to the other.");

								CommonWork oldCW = permProd.getCommonWork();
								permProd.setCommonWork(permPE_CW);

								if (oldCW.getProductCount() == 0) {
									log.debug("The old CW was orphaned so delete it.");
									entityManager.remove(oldCW);
								}
							}
							else {
								String msg = "Importing Product with externalId " + permProd.getExternalId()
									+ ": The CW code from PE [" + commonWork.getCode()
									+ "] doesn't match our current CW [" + permProd.getCommonWork().getCode()
									+ "] for the product and we currently have some asset uses for one of these CWs.";
								throw new RuntimeException("updateProductFromPE(): " + msg);
							}
						}
					}
				}
			}
		}
		timerFirstBlock.stopTimer();

		PerfTimer timerMiddle = monitor.startTimer("ProductRepository::savePartialProductInner::(middle bits)");
		List<ProductPrinting> pp = peProd.getPrintings();
		for (ProductPrinting printing : pp) {
			printing.setProduct(peProd);
		}
		eliminateDuplicates(pp);

		List<Relation> relations = peProd.getRelations();
		if (relations != null) {
			for (Relation r : relations) {
				r.setProduct(peProd);
			}
		}

		fixUserInUserToRoles(peProd);
		preSaveRoles(peProd);
		eliminateDuplicateRoles(peProd);
		// actually we don't need this - user ids are looked up as part of savePartialEntity
		//lookupUserIdsForRoles(peProd);
		//printRoles(peProd);
		fixBadProductLine(peProd);
		fixBadSubMedium(peProd);
		fixNullTitle(peProd);
		timerMiddle.stopTimer();

		Product newProd = savePartialEntity(peProd);

		PerfTimer timerIndex = monitor.startTimer("ProductRepository::savePartialProductInner::updateIndex");
		productIndexService.updateIndex(newProd);
		timerIndex.stopTimer();

		PerfTimer timerReconcileUsers = monitor.startTimer("ProductRepository::savePartialProductInner::reconcileUsers");
		reconcileUsers(peProd);
		timerReconcileUsers.stopTimer();

		//return getAllProducts;
	}

	private void changeCWRefs(int oldId, int newId) {
		log.debug("changeCWRefs(): going to update all references to cw_id " + oldId + " to " + newId);
		changeCWRef("asset_perm_ref", oldId, newId, false);
		changeCWRef("contract", oldId, newId, false);
		changeCWRef("purchase_order", oldId, newId, false);
		changeCWRef("asset_use", oldId, newId, false);
		changeCWRef("cw_file", oldId, newId, false);
		changeCWRef("watched_cw", oldId, newId, false);

		// for these, delete existing records referring to the new id first
		// (new will only be deleted if there are some old records to move over)
		changeCWRef("cw_history", oldId, newId, true);
		changeCWRef("cw_2_condition", oldId, newId, true);
		changeCWRef("cw_photo_estimate", oldId, newId, true);
		changeCWRef("cw_summary", oldId, newId, true);
		changeCWRef("component", oldId, newId, true);

		// not doing product here because done by calling method
	}

	private void changeCWRef(String tableName, int oldId, int newId, boolean deleteNewFirst) {
		if (deleteNewFirst) {
			// only delete new if there are some old records to move over
			int oldCount = countCWRefs(tableName, oldId);
			if (oldCount > 0) {
				deleteCWRefs(tableName, newId);
			}
		}
		String sql = "update " + tableName + " set cw_id = ? where cw_id = ?";
		Query q = createNativeQuery(sql);
		q.setParameter(1, newId);
		q.setParameter(2, oldId);
		int rowCount = q.executeUpdate();
		log.debug("changeCWRef(): update " + tableName + " set cw_id = <" + newId
			+ "> where cw_id = <" + oldId + ">: " + rowCount + " rows updated.");
	}

	private int countCWRefs(String tableName, int cwId) {
		String sql = "select count(*) as count from " + tableName + " where cw_id = ?";
		Query query = createNativeQuery(sql, "scalarCount");
		query.setParameter(1, cwId);
		// For count, DB2 and MS SQL Server return Integer but MySQL returns BigInteger
		// Integer and BigInteger inherit from Number
		Number resultNum = (Number) query.getSingleResult();
		return resultNum.intValue();
	}

	private void deleteCWRefs(String tableName, int cwId) {
		String sql = "delete from " + tableName + " where cw_id = ?";
		Query q = createNativeQuery(sql);
		q.setParameter(1, cwId);
		int rowCount = q.executeUpdate();
		log.debug("deleteCWRefs(): delete from " + tableName
			+ " where cw_id = <" + cwId + ">: " + rowCount + " rows deleted.");
	}

	private void fixUserInUserToRoles(Product peProd) {
		// smarkoff: right here user_id on all UserToRole objects will be non-null but
		// if User object itself is null this means that in the XML we received
		// there was an author specified without a corresponding entry in the masterList (bottom part of message)
		// Generally if the this was missing in the message, there will be a later message that has it,
		// so just forget about the particular user2role for now.
		// - And so far I've only seen one author masterList record missing - ie if there is
		// 1 author then the masterList might not have it, if there are 2 the then authorList has 1 (or 2), etc.
		// NEW - with newer CustomIdResolver (which creates objects even if the MasterList object for a reference
		// is missing) we need to check that the User object has a non-blank firstName + lastName and remove
		// it if does not -- otherwise it will fail the javax.validation (because there are annotations on the class).
		// 6/2012 - add check for duplicate user/role combo's -- sometimes a product will for example
		// list the same user as being an author twice - ex. CORE.001.PROD.0000302224 / ISBN 9781118369074
		List<UserToRole> u2rList = peProd.getUsers();
		int removeCount = 0;

		for (int i = 0; i < u2rList.size(); i++) {
			UserToRole u2r = u2rList.get(i);
			User user = u2r.getUser();
			if (user == null || StringUtils.isBlank(user.getFirstName()) || StringUtils.isBlank(user.getLastName())) {
				if (user != null) {
					log.info("fixUserInUserToRoles(): removing bad User: " + user);
				}
				u2rList.remove(i);
				removeCount++;
				i--;
			}
		}

		if (removeCount > 0) {
			log.info("fixuserInUserToRoles(): # bad users was " + removeCount + " for " + peProd.getExternalId());
		}
	}

	private void preSaveRoles(Product peProd) throws PersistenceException {
		// smarkoff: fix so if two of same role (such as Author) for same product,
		// won't try to insert the role twice (doesn't work since primary key is not code + role_type)
		// - tries to save twice and get constraint violation 2nd time
		HashMap<Role, Role> roleMap = new HashMap<Role, Role>();
		List<UserToRole> userToRoleList = peProd.getUsers();
		for (UserToRole u2r : userToRoleList) {
			Role role = u2r.getRole();
			// role id will always be null here since from PE
			Role temp = roleMap.get(role);
			if (temp != null) {
				log.debug("preSaveRoles(): got Role from Map: " + temp);
			}
			else {
				temp = userRepository.loadRoleByTypeAndCode(role);  // throws PersistenceException
				if (temp != null) {
					log.debug("preSaveRoles(): got Role from DB: " + temp);
				}
				else {
					temp = persist(role);  // throws PersistenceException
					log.debug("preSaveRoles(): persisted Role: " + temp);
				}
			}

			role.setId(temp.getId());  // may already be set
			roleMap.put(role, temp);  // may already be there
		}
	}

	private void eliminateDuplicateRoles(Product peProd) {
		List<UserToRole> u2rList = peProd.getUsers();
		// for key, use Role id (guaranteed to be set by this point)
		// User code (because id maybe not set at this point)
		HashSet<String> set = new HashSet<String>();
		for (int i = 0; i < u2rList.size(); i++) {
			UserToRole u2r = u2rList.get(i);
			String key = u2r.getRole().getId() + "." + u2r.getUser().getCode();
			if (set.contains(key)) {
				log.debug("elminateDuplicateRoles(): removing duplicate: role id ["
					+ u2r.getRole().getId() + "] user code [" + u2r.getUser().getCode() + "]");
				u2rList.remove(i);
				i--;
			}
			else set.add(key);
		}
	}

	// actually don't need this method - savePartialEntity uses
	// MaterializationKey(s) to do a similar lookup
	/*
	private void lookupUserIdsForRoles(Product peProd) {
		List<UserToRole> u2rList = peProd.getUsers();
		for (UserToRole u2r : u2rList) {
			User user = u2r.getUser();
			if (user.getId() == null && StringUtils.isNotBlank(user.getCode())) {
				User dbUser = userRepository.loadByCode(user.getCode());
				if (dbUser != null) {
					log.debug("lookupUserIdsForRoles(): user id ["
							+ dbUser.getId() + "] found for code [" + user.getCode() + "]");
					user.setId(dbUser.getId());
				}
				else {
					log.debug("lookupUserIdsForRoles(): user id not found for code [" + user.getCode() + "]");
				}
			}
		}
	}*/

	// for debug
	@SuppressWarnings("unused")
	private void printRoles(Product peProd) {
		List<UserToRole> u2rList = peProd.getUsers();
		for (UserToRole u2r : u2rList) {
			log.debug("printRoles(): u2r: product = " + u2r.getProduct());
			log.debug("printRoles(): u2r: user = " + u2r.getUser());
			log.debug("printRoles(): u2r: role = " + u2r.getRole());
		}
	}

	// called by reconcileUsers below
	private boolean doesUserHaveRole(int userId, int roleId) {
		final String sql = "select count(*) as count from user_2_role where user_id = ? and role_id = ?";
		Query query = entityManager.createNativeQuery(sql, "scalarCount");
		query.setParameter(1, userId);
		query.setParameter(2, roleId);
		// For count, DB2 and MS SQL Server return Integer but MySQL returns BigInteger
		// Integer and BigInteger inherit from Number
		Number result = (Number) query.getSingleResult();
		return result.intValue() > 0;
	}

	// called by reconcileUsers below
	private void addGlobalRoleToUser(int userId, int roleId) {
		try {
			final String sql = "insert into user_2_role (user_id, role_id, product_id) values (?, ?, null)";
			Query query = entityManager.createNativeQuery(sql);
			query.setParameter(1, userId);
			query.setParameter(2, roleId);
			query.executeUpdate();
		}
		catch (Exception ex) {
			log.error("addGlobalRoleToUser(): caught exception: ", ex);
		}
	}

	private void reconcileUsers(Product peProd) throws PersistenceException {
		// this method just makes sure that Employees have the default employee role
		// and authors have the default author role
		// - it also use to set employees to enabled and authors to disabled (enabled flag on user_table)
		// but I don't think this is necessary (note column default for enabled is true)

		// load Roles we will need to get ids
		Role employeeDefaultRole = userRepository.loadRoleByTypeAndCode(Role.EMPLOYEE_DEFAULT);
		Role authorDefaultRole = userRepository.loadRoleByTypeAndCode(Role.AUTHOR_DEFAULT);

		for (UserToRole up : peProd.getUsers()) {
			User user = up.getUser();
			if (user.getId() == null) {  // this does happen once in a while, not sure why
				log.error("reconcileUsers(): id is null for user: " + user.getFullName() + ", type = " + user.getType());
				continue;
			}
			log.debug("reconcileUsers(): user = " + user.getFullName() + ", type = " + user.getType());
			Role checkRole = null;
			if (user.getType() == User.Type.EMPLOYEE)  checkRole = employeeDefaultRole;
			else if (user.getType() == User.Type.AUTHOR)  checkRole = authorDefaultRole;
			if (checkRole == null) {
				// I don't think this will ever happen - will only get Employees and Authors from PE
				// (not Freelancer and certainly not System which is our own made up type)
				continue;
			}
			if (doesUserHaveRole(user.getId(), checkRole.getId())) {
				log.debug("reconcileUsers(): user already had " + checkRole.getCode());
			}
			else {
				log.debug("reconcileUsers(): user did not have " + checkRole.getCode() + " - will add");
				addGlobalRoleToUser(user.getId(), checkRole.getId());
			}
		}
	}

	private void eliminateDuplicates(List<ProductPrinting> printings) {
		// fix bad data from PE (actually from GBPM)
		// - actually Bruce has put a filter in on his side now for this problem
		// but might as well leave this code here too
		// products where have seen duplicate records (everything is the same):
		// CORE.001.PROD.0000041943
		// (few others I think)
		HashSet<ProductPrinting> set = new HashSet<ProductPrinting>();
		for (int i = 0; i < printings.size(); i++) {
			ProductPrinting pp = printings.get(i);
			if (set.contains(pp)) {
				log.info("eliminateDuplicates(): found duplicate printing - removing: " + pp);
				printings.remove(i);
				i--;
			}
			else {
				set.add(pp);
			}
		}
	}

	private void fixBadProductLine(Product peProd) {
		// smarkoff:
		// Product Lines end up with a null dataSource when the PE message does not
		// have a masterList object for a Product Line - which happens because Bruce
		// says that GBPM has some bad data - products with productLines that are not
		// valid for the businessUnit (division) they are in.
		// From checking data when this happens - even if I were to set the dataSource
		// before the lookup is done (using Materialization Keys on the class) the
		// lookup would fail because the matching record does not exist for that dataSource.
		// So trying to set the dataSource is no good - just null it out.
		// Also note that when the JPARepository.materialize method is trying to find the
		// object using the Materialization Keys, it appears that if one key is null, it
		// just does not include that key in the query -- that might be incorrect.
		ProductLine productLine = peProd.getProductLine();
		if (productLine != null) {
			if (productLine.getDataSource() == null) {
				log.warn("fixBadProductLine(): productLine [" + productLine.getCode()
						+ "] for [" + peProd.getExternalId() + "] has null dataSource - will null out productLine");
				peProd.setProductLine(null);
			}
		}

		// example products with this problem (not a complete list - there are at least 1,000) (9/2012):
		// generally but not always true: 001 = US, 002 = CA, 003 = AU, 004 = UK, 005 = SG, 006 = DE
		// CORE.001.PROD.0000113666
		// CORE.004.PROD.0000079785
		// CORE.004.PROD.0000079855
		// CORE.004.PROD.0000079872
		// CORE.004.PROD.0000078491
		// CORE.004.PROD.0000078351
		// CORE.004.PROD.0000061836
		// CORE.004.PROD.0000061739
	}

	private void fixBadSubMedium(Product peProd) {
		// Similar situation to fixBadProductLine() above - see comments there.
		// Bad sample products for this (not a complete list): (1/2013)
		// CORE.006.PROD.0000017093
		// CORE.006.PROD.0000017095
		// CORE.006.PROD.0000003990
		// CORE.006.PROD.0000017166
		SubMedium subMedium = peProd.getSubMedium();
		if (subMedium != null) {
			if (subMedium.getDataSource() == null) {
				log.warn("fixBadSubMedium(): subMedium [" + subMedium.getCode()
						+ "] for [" + peProd.getExternalId() + "] has null dataSource - will null out subMedium");
				peProd.setSubMedium(null);
			}
		}
	}

	private void fixNullTitle(Product peProd) {
		// Some products have a null title (message from PE does not contain title element)
		// Generally when this the case the shortTile is still present and is "DELETE" or "ABANDONED".
		// So I will just copy the shortTitle over to the title.
		if (StringUtils.isNotBlank(peProd.getTitle())) return;
		log.warn("fixNullTitle(): title is null for [" + peProd.getExternalId()
				+ "] shortTile [" + peProd.getShortTitle() + "]");
		if (StringUtils.isNotBlank(peProd.getShortTitle())) {
			peProd.setTitle(peProd.getShortTitle());
		}
		else {
			peProd.setTitle("[Missing Title]");
		}

		// example products with this problem (not a complete list) (9/2012):
		// generally but not always true: 001 = US, 002 = CA, 003 = AU, 004 = UK, 005 = SG, 006 = DE
		// CORE.003.PROD.0000006334
		// CORE.003.PROD.0000006131
		// CORE.003.PROD.0000003431
		// CORE.003.PROD.0000003673
		// CORE.003.PROD.0000004608
		// CORE.003.PROD.0000006509
		// CORE.003.PROD.0000006504
		// CORE.003.PROD.0000001275
		// CORE.003.PROD.0000007642
		// CORE.003.PROD.0000005223
		// CORE.003.PROD.0000000826
		// CORE.003.PROD.0000001500
	}

	@Transactional (propagation = Propagation.REQUIRES_NEW)
	public void recalculatePrimaryProduct(String cwCode) throws PersistenceException {
		CommonWork commonWork = cwRepository.loadByCode(cwCode);

		List<Product> products = commonWork.getProducts();
		//List<Product> products = getProductService().loadProductsByCWId (commonWork.getId());

		// check for no products although probably we will not allow this to happen
		// (but could happen if database is manually manipulated)
		if (CollectionUtils.isEmpty(products)) {
			log.debug("recalculatePrimaryProduct(): no products for cw code " + cwCode);
			return;
		}

		log.debug("recalculatePrimaryProduct(): products.size() = "
			+ products.size() + " for cw code = " + cwCode);

		int maxRank = 0;
		Product maxProduct = products.get(0);

		if (products.size() == 1) {
			// may be already set
			maxProduct.setCwPrimary(true);
			return;
		}

		for (Product p : products) {
			int rank = getMediumRank(p.getMedium());
			if (rank > maxRank) {
				maxProduct = p;
			}
		}

		for (Product p : products) {
			boolean primary = p.equals(maxProduct);
			p.setCwPrimary(primary);
			if (primary) {
				log.debug("recalculatePrimaryProduct(): set product with id " + p.getId() + " to primary.");
			}
		}
	}

	/**
	 * This method is used to compare two mediums and decide which one should
	 * correspond to a primary product.
	 * A higher value means more likely to be primary.
	 *
	 * @param medium  May be null (returns 0)
	 */
	public int getMediumRank(Medium medium) {
		if (medium == null)  return 0;
		return getMediumRank(medium.getCode());
	}

	public int getMediumRank(String mediumCode) {
		if (mediumCode.equals(Medium.CLOTH.getCode())) {
			return 10;
		}
		else if (mediumCode.equals(Medium.PAPER.getCode())) {
			return 9;
		}
		else if (mediumCode.equals(Medium.LOOSE_LEAF.getCode())) {
			return 8;
		}
		else if (mediumCode.equals(Medium.VIDEO.getCode())) {
			return 7;
		}
		else if (mediumCode.equals(Medium.WEBSITE.getCode())) {
			return 6;
		}
		else if (mediumCode.equals(Medium.SOFTWARE.getCode())) {
			return 5;
		}
		else {
			return 0;
		}
	}

	// smarkoff: This method doesn't currently work - nested sort properties
	// such as "component.name" cause a SQL error: SQLCODE=-1585, SQLSTATE=54048.

	// @Transactional(propagation = Propagation.REQUIRED)
	// public List<AssetUse> searchAssetUses(Product product, TableFacade tf, boolean firstHit)
	// throws PersistenceException {
	//     TODO: Need to filter by product id
	//     tf.addFilterMatcher(arg0, arg1);
	//
	//     SortSet sortSet = tf.getLimit().getSortSet();
	//     log.debug("current SortSet: " +
	//         StringUtil.collectionToString(sortSet.getSorts(), ", "));
	//
	//     if (firstHit) {
	//         tf.getLimit().getSortSet().addSort("component.name", Order.ASC);
	//         tf.getLimit().getSortSet().addSort("position", Order.ASC);
	//         tf.getLimit().getSortSet().addSort("asset.description", Order.ASC);
	//      }
	//
	//     List<AssetUse> list = search(AssetUse.class, tf);
	//     return list;
	// }

	// This method is not currently used. Not sure if it works ok or not
	// (smarkoff).
	/**
	 * Tries to find the product by externalId, merges the values and saves the
	 * new values
	 *
	 * @param product
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	/*
	 * @Transactional(propagation = Propagation.REQUIRED) public void
	 * updateProductByExternalId(Product product) throws PersistenceException,
	 * IllegalAccessException, InvocationTargetException { Product oldProduct =
	 * null;
	 *
	 * oldProduct = loadByExternalId(product.getExternalId());
	 *
	 * if (oldProduct != null) { BeanUtils.copyProperties(oldProduct, product); }
	 * else { oldProduct = product; }
	 *
	 * persist(oldProduct); }
	 */

	/**
	 * Returns a list of products that are associated with watched common works
	 *
	 * @param userId
	 * @return list of products
	 * @throws PersistenceException
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<Product> loadProductsInWatchedCommonWorks(int userId) throws PersistenceException
	{
		PerfTimer timer = monitor.startTimer("ProductService::loadProductsInWatchedCommonWork");
		try {
			// TODO: Can this be converted to a JQL so that we get back a TypedQuery?
			Query query = entityManager.createNativeQuery(
					"select p.* from product p where p.cw_id in (select cw_id from watched_cw where user_id = ?)",
					Product.class);

			query.setParameter(1, userId);
			@SuppressWarnings("unchecked")
			List<Product> list = query.getResultList();
			return list;
		}
		finally {
			timer.stopTimer();
		}
	}

	/**
	 * Returns a list of products that are associated with a common work
	 *
	 * @param cwId
	 * @return list of products
	 * @throws PersistenceException
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<Product> loadProductsByCWId(int cwId) {
		PerfTimer timer = monitor.startTimer("ProductService::loadProductsByCWId");
		try {
			TypedQuery<Product> query = entityManager.createQuery("from Product p where p.commonWork.id  = ?", Product.class);
			query.setParameter(1, cwId);
			return query.getResultList();
		}
		finally {
			timer.stopTimer();
		}
	}

	/**
	 * Returns a product loaded by externalId - returns null if not found.
	 *
	 * @param externalId
	 * @return
	 * @throws PersistenceException
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public Product loadByExternalId(String externalId) throws PersistenceException {
		try {
			TypedQuery<Product> query = entityManager.createQuery("from Product up where up.externalId = ?", Product.class);
			query.setParameter(1, externalId);
			Product p = query.getSingleResult();
			// For some reason if return query.getSingleResult() directly this code hangs (in DEV) - weird !!
			// - This may have been a bug in the JDBC driver.
			return p;
		}
		catch (NoResultException e) {
			log.debug("loadByExternalId(): product not found with externalId " + externalId);
			return null;
		}
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public boolean doesExternalIdExist(String externalId) {
		final String sql = "select count(*) as count from product where external_id = ?";
		Query query = entityManager.createNativeQuery(sql, "scalarCount");
		query.setParameter(1, externalId);
		// For count, DB2 and MS SQL Server return Integer but MySQL returns BigInteger
		// Integer and BigInteger inherit from Number
		Number result = (Number) query.getSingleResult();
		return result.intValue() > 0;  // (for externalId we expect 0 or 1 rows)
	}

	/**
	 * @param product  Must be non-null
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public boolean doesPreviousEditionWIDExist(Product product) {
		ArgUtil.notNull(product, "product");
		boolean hasPreviousEdition = false;
		String previousEditionWID = product.getPreviousEditionWID();
		if (StringUtils.isNotBlank(previousEditionWID)) {
			// could skip making sure that previousEditionWID exists in DB but think good to check
			hasPreviousEdition = doesExternalIdExist(previousEditionWID);
			if (!hasPreviousEdition) {
				// James says this situation is ok
				// We do have a lot of it - to see run this query:
				//select p.external_id, p.isbn13, p2.external_id from product p left join (product p2)
				//on p.previous_edition_wid = p2.external_id where p.previous_edition_wid is not null and p2.external_id is null;
				log.debug("doesPreviousEditionWIDExist(): previousEditionWID [" + previousEditionWID
					+ "] for WID [" + product.getExternalId() + "] not found in DB");
			}
		}
		return hasPreviousEdition;
	}

	/**
	 * Pre-loads stuff required for index.
	 * @throws Exception
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public Product loadByExternalIdForIndex(String externalId) throws Exception {
		PerfTimer timer = monitor.startTimer("ProductRepository::loadByExternalIdForIndex");
		Product p = loadByExternalId(externalId);
		if (p == null)  return null;

		lazyLoadForIndex(p);
		timer.stopTimer();

		return p;
	}

	@Transactional(propagation = Propagation.SUPPORTS)
	public void lazyLoadForIndex(Product p) throws Exception {
		log.debug("lazyLoadForIndex(Product) called");
		p.getExternalId();
		p.isCwPrimary();
		p.getDataSource();
		p.getShortAuthorName();
		p.getAuthors();
		p.getPublicationStatus().getCode();
		p.getBusinessUnit().getCode();
		// use lazyLoad method for stuff that might be null, also because lazyLoad will call all primitive getters
		// (we want codes for stuff)
		lazyLoad(p, "productLine");
		lazyLoad(p, "medium");
		lazyLoad(p, "subMedium");
		// here use lazyLoad because will iterate through array and get code (primitive) for all elements
		lazyLoad(p, "bundles");
		lazyLoad(p, "relations");

		CommonWork cw = p.getCommonWork();
		cw.getCode();
		cw.getAUCountNotCanceled();
		cw.getCoverCountNotCanceled();
		cw.getNonCoverCountNotCanceled();
		cw.getStatusNotOkCount();
	}

	/**
	 * @param whereExpression  May be null to mean load all external ids
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public Set<String> loadExternalIds(String whereExpression) throws PersistenceException
	{
		final String sql = "select external_id as string from product p" + (StringUtils.isBlank(whereExpression) ? "" : " where " + whereExpression);
		Query query = entityManager.createNativeQuery(sql, "scalarString");
		@SuppressWarnings("unchecked")
		List<String> list = query.getResultList();
		HashSet<String> set = new HashSet<String>(list.size());
		set.addAll(list);
		return set;
	}

	/**
	 * @param whereExpression  May be null to mean load all isbn13's
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public Set<String> loadIsbn13s(String whereExpression) throws PersistenceException
	{
		final String sql = "select isbn13 as string from product p" + (StringUtils.isBlank(whereExpression) ? "" : " where " + whereExpression);
		Query query = entityManager.createNativeQuery(sql, "scalarString");
		@SuppressWarnings("unchecked")
		List<String> list = query.getResultList();
		HashSet<String> set = new HashSet<String>(list.size());
		set.addAll(list);
		return set;
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public Set<String> loadExternalIdsWherePrimaryAndPubStatusPEIN() throws PersistenceException {
		final String where = "is_cw_primary is true and (pub_status in ('" + PublicationStatus.PRE_CONTRACT.getCode()
			+ "', '" + PublicationStatus.EDITORIAL.getCode()
			+ "', '" + PublicationStatus.IN_PRODUCTION.getCode()
			+ "') or (pub_status = '" + PublicationStatus.PUBLISHED.getCode()
			+ "' and copyright_year >= " + PublicationStatus.PUBLISHED_START_YEAR + ") )";
		return loadExternalIds(where);
	}

	/**
	 * @param whereExpression  May be null to mean count all products
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public int countProducts(String whereExpression) throws PersistenceException {
		final String sql = "select count(*) as count from product p" + (StringUtils.isBlank(whereExpression) ? "" : " where " + whereExpression);
		Query query = entityManager.createNativeQuery(sql, "scalarCount");
		// For count, DB2 and MS SQL Server return Integer but MySQL returns BigInteger
		// Integer and BigInteger inherit from Number
		Number result = (Number) query.getSingleResult();

		return result.intValue();
	}

	/**
	 * Returns a product loaded by Id - returns null if not found.
	 * Will load the commonWork object too, so we have access to commonWork.id
	 * @param Id
	 * @return
	 * @throws PersistenceException
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public Product loadById(Integer id) throws PersistenceException
	{
		String jpql = "from Product p join fetch p.commonWork where p.id = ?";
		TypedQuery<Product> query = entityManager.createQuery(jpql, Product.class);
		query.setParameter(1, id);
		return query.getSingleResult();
	}

	/**
	 * Returns null if not found.
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public Product loadExtendedProductById(Integer productId) throws PersistenceException
	{
		Product p = loadById(productId);
		loadExtendedProduct(p);
		return p;
	}

	/**
	 * @param p  May be null, in which nothing is loaded
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public void loadExtendedProduct(Product p)
	{
		if (p == null)  return;
		p.getCommonWork();
		p.getAuthorsAsString();
		p.getPublicationStatus().getDescription();
		ProductLine productLine = p.getProductLine();
		if (productLine != null)  productLine.getName();
		ProductType productType = p.getProductType();
		if (productType != null)  productType.getName();
		ProductFamily productFamily = p.getProductFamily();
		if (productFamily != null)  productFamily.getCode();
		ProductEdition productEdition = p.getEdition();
		if (productEdition != null) productEdition.getEditionNumber();
		SubMedium subMedium = p.getSubMedium();
		if (subMedium != null) subMedium.getName();
	}

	/**
	 * Returns the externalId for the id, or null if no product is in the Permissions db with that id.
	 *
	 * @throws PersistenceException
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public Integer lookupIdForExternalId(String externalId) throws PersistenceException
	{
		log.debug("lookupIdForExternalId() called...");
		try {
			Query query = entityManager.createNativeQuery("select id from product p where p.external_id = ?",
					"scalarId");
			query.setParameter(1, externalId);
			Integer result = (Integer) query.getSingleResult();
			return result;
		}
		catch (NoResultException ex) {
			return null;
		}
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<Product> getProductByTitle(String title) throws PersistenceException
	{
		TypedQuery<Product> query = entityManager.createNamedQuery("Product.findByTitle", Product.class);
		query.setParameter("title", "%" + title + "%");
		return query.getResultList();
	}

	/**
	 * Returns null if not found.
	 * @param isbn  isbn13 or isbn10 of the product
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public Product getProductByISBN(String isbn) throws PersistenceException {
		ArgUtil.notNull(isbn, "isbn");
		TypedQuery<Product> query;

		if (isbn.length() == 10) {
			query = entityManager.createNamedQuery("Product.findByIsbn10", Product.class);
			query.setParameter("isbn", isbn);
		}
		else if (isbn.length() == 9) {
			query = entityManager.createNamedQuery("Product.findByPNumber", Product.class);
			query.setParameter("pnumber", isbn);
		}
		else if (isbn.length() == 13)  {
			query = entityManager.createNamedQuery("Product.findByIsbn13", Product.class);
			query.setParameter("isbn", isbn);
		}
		else {
			throw new IllegalArgumentException("isbn expected to be of length 9, 10, or 13 [" + isbn + "]");
		}

		List<Product> products = query.getResultList();

		if (products.size() == 0) return null;
		else if (products.size() == 1)  return products.get(0);
		else {
			log.warn("More than one product found for isbn [" + isbn + "]");
			return products.get(0);
		}
	}

	/**
	 * Returns null if not found.
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public Product getProductByPnumber(String pnumber) throws PersistenceException {
		ArgUtil.notNull(pnumber, "pnumber");

		TypedQuery<Product> query = entityManager.createNamedQuery("Product.findByPNumber", Product.class);
		query.setParameter("pnumber", pnumber);
		List<Product> products = query.getResultList();

		if (products.size() == 0) return null;
		else if (products.size() == 1)  return products.get(0);
		else {
			log.warn("More than one product found for pnumber [" + pnumber + "]");
			return products.get(0);
		}
	}

	/**
	 * returns all products related to currentProduct by CommonWork
	 *
	 * @param familyCode
	 * @param currentId
	 * @return
	 * @throws PersistenceException
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<Product> loadProductRelatedWorks(int currentId, CommonWork workCode)
			throws PersistenceException
	{
		if (null == workCode)
			return null;

		TypedQuery<Product> query = entityManager.createNamedQuery(
				"Product.findRelatedWorks", Product.class);

		query.setParameter("workCode", workCode.getCode());
		query.setParameter("currentId", currentId);

		return query.getResultList();
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public Product loadPreviousEdition_doNotUse(int productId) throws PersistenceException {
		return loadEdition_doNotUse(productId, true);
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public Product loadNextEdition_doNotUse(int productId) throws PersistenceException {
		return loadEdition_doNotUse(productId, false);
	}

	/**
	 * Returns the latest previous edition or null if not found.
	 * TODO - maybe the queries can be optimized a little bit.
	 *
	 * @param product Must be non-null
	 * @param previous  false means next edition instead of previous
	 */
	private Product loadEdition_doNotUse(int productId, boolean previous) throws PersistenceException
	{
		if (System.currentTimeMillis() > 0) {  // needed to not have complier complain that code below is unreachable
			throw new RuntimeException(
			"Do Not Use this method anymore - use Product.getPrevious/NextEditionWID methods or the ProductIndexService");
		}

		// select * from product where medium = 'C' and product_family = '00001'
		// and edition = (
		// select id from product_edition e where e.product_family = '00001'
		// and e.edition_number < 8 order by e.edition_number desc
		// FETCH FIRST 1 ROWS ONLY
		// )
		Product product = find(Product.class, productId);

		if (product == null) {
			log.warn("loadEdition(): productId " + productId + " not found.");
			return null;
		}

		if (product.getEdition() == null) {
			log.warn("loadEdition(): product.getEdition() was null for productId = " + productId);
			return null;
		}

		String familyCode = product.getProductFamily() == null ? null : product.getProductFamily().getCode();

		// FETCH FIRST does not work with Hibernate, so we do a 2 step query
		String sqlEdition = previous ? "from ProductEdition e where e.productFamily.code = ?"
				+ "	and e.editionNumber < ? order by e.editionNumber desc"
				:
				"from ProductEdition e where e.productFamily.code = ?"
				+ "	and e.editionNumber > ? order by e.editionNumber";
		TypedQuery<ProductEdition> query = entityManager.createQuery(sqlEdition, ProductEdition.class);
		query.setParameter(1, familyCode);
		query.setParameter(2, product.getEdition().getEditionNumber());
		query.setMaxResults(1);
		List<ProductEdition> editions = query.getResultList();

		if (editions.size() == 0) {
			log.debug("loadEdition(): No " + (previous ? "Previous" : "Next") + " editions found for productId " + productId);
			return null;
		}

		Integer editionId = editions.get(0).getId();
		log.debug((previous ? "Latest Previous" : "Earliest Next") + " edition = " + editionId + " for productId " + productId);

		// lnagy - not sure what the logic should be here if no medium
		// for now I will just fix it so it does not throw an exception
		TypedQuery<Product> q2;
		if (null != product.getMedium()) {
			String sql = "from Product p where p.productFamily.code = ? "
						+ " and p.edition.id = ? and p.medium.code = ?";
			q2 = entityManager.createQuery(sql, Product.class);

			q2.setParameter(3, product.getMedium().getCode());
		}
		else {
			String sql = "from Product p where p.productFamily.code = ? "
					+ " and p.edition.id = ?";
			q2 = entityManager.createQuery(sql, Product.class);
		}
		q2.setParameter(1, familyCode);
		q2.setParameter(2, editionId);
		List<Product> products = q2.getResultList();

		if (products.size() > 1) {
			log.warn("loadEdition(): found " + products.size() + " results (more than 1) for productId " + productId);
		}

		if (products.size() == 0)
			return null;
		else {
			Product p = products.get(0);
			if (products.size() > 1) {
				log.debug("loadEdition(): returning product: " + p);
			}
			return p;
		}
	}

	@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
	public String getAuthorsByProductId(Integer id) throws Exception {
		return getUsersByProductIdRole(id, Role.AUTHOR);
	}

	@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
	public String getPhotoEditorByProductId(Integer id) throws Exception {
		return getUsersByProductIdRoleCombined(id, "'PHE'");
	}

	@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
	public String getEditorByProductId(Integer id) throws Exception {
		return getUsersByProductIdRoleCombined(
				id, "'AE', 'TE', 'E', 'ED', 'PHE', 'SE','MED', 'PJE', 'NME', 'EDX', 'CE', 'EM', 'VPE'");
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
	public String getUsersByProductIdRole(Integer productId, Role role) throws Exception
	{
		String sql = "select last_name, first_name from user_2_role map, user_table u, role r "
			+ "where map.user_id = u.id and map.role_id = r.id and map.product_id = ? "
			+ "and r.code = ? and r.role_type = ?";
		Query q = entityManager.createNativeQuery(sql);
		q.setParameter(1, productId);
		q.setParameter(2, role.getCode());
		q.setParameter(3, role.getRoleType());
		@SuppressWarnings("unchecked")
		List<Object[]> results = q.getResultList();

		if (results.size() > 0) {
			String authors = "";

			for (Object[] columns : results) {
				if (authors.length() > 0) authors = authors + ", ";
				if (null != columns[1]) authors = authors + columns[1];
				if (null != columns[0]) authors = authors + " " + columns[0];
			}

			return authors;
		}

		return " ";
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
	public String getUsersByProductIdRoleCombined(Integer id, String roleCodes) throws Exception
	{
		// first load the Cover assets
		final String sql = "select r.description || ': ' || u.first_name || u.last_name"
			+ " from user_2_role map, user_table u, role r "
			+ "where map.user_id = u.id and map.role_id = r.id and map.product_id = ? "
			+ "and r.code in(" + roleCodes + ") ";

		String authors = "";

		Query q = entityManager.createNativeQuery(sql);
		q.setParameter(1, id);
		@SuppressWarnings("unchecked")
		List<String> results = q.getResultList();

		if (results.size() > 0) {
			String newLine = "";
			for (String columns : results) {
				if (null != columns) {
					if (authors.length() > 0) authors = authors + ", ";
					authors = authors + columns + newLine;
					newLine = "\n";
				}
			}

			// return authors;
		}
		 return authors;
	}

	@SuppressWarnings("unchecked")
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<Product> getProductsUpdatedLately(int daysFromLastUpdate) throws Exception
	{
		Query q = entityManager.createNativeQuery("select * from product where last_updated_date >= ADDDATE(CURDATE(), -"
				+ daysFromLastUpdate + ")", Product.class );

		return q.getResultList();
	}

	/**
	 * Temporary method called by ProductIndexService.indexProductData(InputStream).
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public void updatePreviousNextEditionWIDs(String wid, String previousEditionWID, String nextEditionWID) {
		String sql = "update product set previous_edition_wid = ?, next_edition_wid = ? where external_id = ?";
		Query q = createNativeQuery(sql);
		q.setParameter(1, previousEditionWID);
		q.setParameter(2, nextEditionWID);
		q.setParameter(3, wid);
		q.executeUpdate();
	}

	/**
	 * Temporary method called by ProductIndexService.indexProductData(InputStream).
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public void updateComponentFlag(String wid, String componentFlag) {
		String sql = "update product set component_flag = ? where external_id = ?";
		Query q = createNativeQuery(sql);
		q.setParameter(1, componentFlag);
		q.setParameter(2, wid);
		q.executeUpdate();
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void updateEbookSalesForIsbn13(String isbn13, int total) {
		String sql = "update product set ebook_sales = ? where isbn13 = ?";
		Query q = createNativeQuery(sql);
		q.setParameter(1, total);
		q.setParameter(2, isbn13);
		q.executeUpdate();
	}

	//Start: Added for Build Ticket DM-292
	@Transactional(propagation = Propagation.REQUIRED)
	public void updateBatchGrossUnitsForIsbn13(List<Row> items, int n) throws PersistenceException {
		String temp = null;
		int count = 0, i = 0;
        int batchSize = 30;
        ISBNDataTmp data = null;
		for (Row item : items) {
			List<String> val = item.getValue();
			if (null != val && !val.isEmpty()) {
				//Here val.get(0) = ISBN13 and val.get(1) = gross count.
				if(!StringUtils.isEmpty(val.get(0)) && !StringUtils.isEmpty(val.get(1))) {
					temp = val.get(1).trim();
					if(temp.startsWith("-")) {//If it is negative count, i.e., starts with "-", then make the count as 0
						count = 0;
					} else {//it is not starting with minus sign "-"
						try {
							count = Integer.parseInt(temp);
						} catch (NumberFormatException nfe) {
							count = 0; //simply set the count to zero.
						}
					}
					data = new ISBNDataTmp(val.get(0), count);
					entityManager.persist(data);
					i++;

					if (i % batchSize == 0) {
						entityManager.flush();
						entityManager.clear();
		            }
				}
			}
		}
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public ISBNData getEBookCount(String isbn13) throws PersistenceException {
		try {
    		String qlString = "select * from isbn_data where isbn13 = ?";
    		Query query = entityManager.createNativeQuery(qlString, ISBNData.class);
    		query.setParameter(1, isbn13);

    		ISBNData data = (ISBNData) query.getSingleResult();

    		return data;
    		//return ((null == data.getGrossUnits()) ? 0 : data.getGrossUnits());
    	} catch (NoResultException e) {
    		return null;
    		//return 0;
    	}
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void insertToISBNDataTableFromTempTbl() throws PersistenceException {
		String sql = "insert into isbn_data select * from isbn_data_tmp";
		Query q = entityManager.createNativeQuery(sql);
		q.executeUpdate();
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void deleteFromISBNDataTable() throws PersistenceException {
		String sql = "delete from isbn_data";
		Query q = entityManager.createNativeQuery(sql);
		q.executeUpdate();
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void deleteFromISBNDataTempTable() throws PersistenceException {
		String sql = "delete from isbn_data_tmp";
		Query q = entityManager.createNativeQuery(sql);
		q.executeUpdate();
	}
	//End: Added for Build Ticket DM-292

	//Start: Added for Build Ticket DM-532
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public Integer getPrimaryProductId(Integer cwId) throws PersistenceException
	{
		log.debug("getPrimaryProductId() called...");
		try {
			Query query = entityManager.createNativeQuery("select id from product p where p.cw_id = ? and p.is_cw_primary is true",
					"scalarId");
			query.setParameter(1, cwId);
			Integer result = (Integer) query.getSingleResult();
			return result;
		}
		catch (NoResultException ex) {
			return null;
		}
	}
	//End: Added for Build Ticket DM-532

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<String> getEditorCodesForBUAndProductLine(String buCode, String productLineCode) {
		final String sql = "select distinct(editor) as string from product p, product_line pl where pl.code = ?"
			+ " and p.product_line_id = pl.id and p.business_unit = ? and editor is not null order by editor";
		Query query = entityManager.createNativeQuery(sql, "scalarString");
		query.setParameter(1, productLineCode);
		query.setParameter(2, buCode);
		@SuppressWarnings("unchecked")
		List<String> list = query.getResultList();
		return list;
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public int readProductIndexFilePosition() throws PersistenceException
	{
		// "as id" matches scalarId
		final String sql = "select position as id from product_index_file_position";
		Query query = entityManager.createNativeQuery(sql, "scalarId");
		Integer position = (Integer) query.getSingleResult();
		return position;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void writeProductIndexFilePosition(int position) throws PersistenceException
	{
		// "as id" matches scalarId
		final String sql = "update product_index_file_position set position = ?";
		Query query = entityManager.createNativeQuery(sql);
		query.setParameter(1, position);
		int rows = query.executeUpdate();
		if (rows != 1) {
			log.warn("writeProductIndexFilePostion(): updated " + rows + " rows (expected 1)");
		}
	}

	/**
	 * @param code  Must be non-null (should be non-blank)
	 * @param dataSource  Must be non-null (should be non-blank)
	 * @return  the name if found, null otherwise
	 * @throws PersistenceException
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public String lookupEditorNameForCode(String code, String dataSource) throws PersistenceException
	{
		ArgUtil.notNull(code, "code");
		ArgUtil.notNull(dataSource, "dataSource");
		final String sql = "select name as string from editor where code = ? and data_source = ?";
		Query query = entityManager.createNativeQuery(sql, "scalarString");
		query.setParameter(1, code);
		query.setParameter(2, dataSource);
		try {
			return (String) query.getSingleResult();
		}
		catch (NoResultException ex) {
			return null;
		}
	}


	public CommonWorkRepository getCommonWorkRepository() {
		return cwRepository;
	}

	public void setCommonWorkRepository(CommonWorkRepository cwRepository) {
		this.cwRepository = cwRepository;
	}

	public AssetUseRepository getAssetUseRepository() {
		return assetUseRepository;
	}

	public void setAssetUseRepository(AssetUseRepository assetUseRepository) {
		this.assetUseRepository = assetUseRepository;
	}

	public UserRepository getUserRepository() {
		return userRepository;
	}

	public void setUserRepository(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public UserService getUserService() {
		return userService;
	}

	public void setUserService(UserService userService) {
		this.userService = userService;
	}

	public void setProductIndexService(ProductIndexService productIndexService) {
		this.productIndexService = productIndexService;
	}
}
