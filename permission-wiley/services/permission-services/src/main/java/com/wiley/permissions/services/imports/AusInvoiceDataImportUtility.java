package com.wiley.permissions.services.imports;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.sql.DataSource;

import net.sf.jxls.reader.XLSDataReadException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlProvider;

import com.wiley.permissions.domain.persistence.permissions.Address;
import com.wiley.permissions.domain.persistence.permissions.AssetUse;
import com.wiley.permissions.domain.persistence.permissions.Contact;
import com.wiley.permissions.domain.persistence.permissions.Contract;
import com.wiley.permissions.domain.persistence.permissions.ContractType;
import com.wiley.permissions.domain.persistence.permissions.Currency;
import com.wiley.permissions.domain.persistence.permissions.ExtendedAssetUse;
import com.wiley.permissions.domain.persistence.permissions.ImportSource;
import com.wiley.permissions.domain.persistence.permissions.PurchaseOrder;
import com.wiley.permissions.domain.persistence.permissions.Source;
import com.wiley.permissions.persistence.PersistenceException;
import com.wiley.permissions.repositories.PurchaseOrderRepository;

public class AusInvoiceDataImportUtility {

	private static final Log log = LogFactory.getLog(AusInvoiceDataImportUtility.class);

	private DataSource dataSource;
	private ImportAssetsUtility importAssetsUtility;
	private PurchaseOrderRepository purchaseOrderRepository;
	// single threaded connection for looking up assets in ozlist
	private Connection assetLookupConnection;

	/**
	 * Imports the assets from Australian Permissions DTB
	 *
	 * @param data
	 * @throws XLSDataReadException
	 * @throws PersistenceException
	 */
	// @Transactional(propagation = Propagation.REQUIRED)
	public void importAusAssets(byte[] data) throws XLSDataReadException, PersistenceException
	{
		log.debug("importAusAssets() entered");

		try {
		assetLookupConnection = dataSource.getConnection();
		} catch (Exception ex) {
			log.debug("****unable to get connection******");
		}
		JdbcTemplate template = new JdbcTemplate(dataSource);
		String sql = new String(data);

		log.debug(sql);
		UpdatablePreparedStatementCreator psc = new UpdatablePreparedStatementCreator (sql);
		template.query(psc, new RowMapper<PurchaseOrder>() {
			@Override
			public PurchaseOrder mapRow(ResultSet rs, int rowNum) throws SQLException
			{
				try {
					// getMonitor().startTimer("ImportAssetsUtility::mapRow");
					log.debug("---------------------------------------------");
					log.debug("Import row " + rowNum);
					rs.updateString("status", "started");
					String ozProjectNo = rs.getString(2);
					log.debug("about to process OZPOID:" + ozProjectNo);
					String poNumber = "";
					String invoiceNumber = "";
					String sourceExternalId = "";
					Date poDate = new Date();
					Date invoiceDate = new Date();
					Date invoiceDatePaid = new Date();
					double poAmount = 0;
					double invoiceAmount = 0;

					PurchaseOrder purchaseOrder = new PurchaseOrder();
					Contract contract = new Contract();
					contract.setImportSource(ImportSource.FROM_AUSTRALIA);

					if (StringUtils.isNotBlank(rs.getString(3))) {
						// SourceExternalId,
						sourceExternalId = rs.getString(3);
						String importSourceName = rs.getString(13);
						log.debug("source name:" + importSourceName);
						log.debug("source external id:" + sourceExternalId);
						try {
							Source source = null;
							try {
								 source = importAssetsUtility.getSourceRepository().loadSourceByExternalId(sourceExternalId);
								 log.debug("source found by external id name = " + source.getName() + " id:" + sourceExternalId);
							} catch (Exception src) {
							   //
							}

							if (null == source) {
								log.debug("trying to find source by name");
								source = importAssetsUtility.getSourceRepository().loadSourceByName(importSourceName);
								log.debug("source found by name " + importSourceName);
							}
							purchaseOrder.setSource(source);
							contract.setSource(source);

							source = importAssetsUtility.getSourceRepository().lazyLoad(Source.class, source.getId(), new String[] { "addresses"});
							purchaseOrder.setSource(source);
							contract.setSource(source);

							try {
								Contact contact = importAssetsUtility.getSourceRepository().find(Contact.class, new Integer(purchaseOrder.getSourceContactInfo()));
								if (null != contact) {
									String sourceContactInfo = purchaseOrder.getSource().getDisplayName() + "\nAttn: " + contact.getName();
									purchaseOrder.setSourceContactInfo(sourceContactInfo);
									purchaseOrder.setSourceAddressInfo(contact.getAddress().getAddressDisplay());
								}
							} catch (Exception cont) {
							    purchaseOrder.setSourceContactInfo(source.getDisplayName());
							    if (source.getAddresses().size() > 0) {
							    	Address address = source.getAddresses().get(0).getAddress();
							    	purchaseOrder.setSourceAddressInfo(address.getAddressDisplay());
							    }
								log.debug("no contact found" + cont.getMessage());
							}
						} catch (Exception f) {
							log.debug("source " + sourceExternalId + "not found");
						}
					}
					if (StringUtils.isNotBlank(rs.getString(4))) {
						// process PO_number
						poNumber = rs.getString(4);
						purchaseOrder.setNote("PO Number: " + poNumber);
						log.debug("po:" + poNumber);
					}
					if (StringUtils.isNotBlank(rs.getString(5))) {
						// process po date
						log.debug("** PO Date: " + rs.getString(5));
						poDate = new SimpleDateFormat("MM/dd/yyyy", Locale.US).parse(rs.getString(5).trim());
						purchaseOrder.setCreatedDate(new Date());
						purchaseOrder.setDate(poDate);
						contract.setCreatedDate(new Date());
					}
					if (StringUtils.isNotBlank(rs.getString(6))) {
						// po amount
						poAmount = Double.parseDouble(rs.getString(6));
						purchaseOrder.setEstimatedPrice(poAmount);
						log.debug("po amount:" + poAmount);
					}
					if (StringUtils.isNotBlank(rs.getString(7))) {
						// process invoice number
						invoiceNumber = rs.getString(7);
						contract.setNumber(invoiceNumber);
						log.debug("invoice:" + invoiceNumber);
						log.debug("Contract number:" + contract.getNumber());
					}
					if (StringUtils.isNotBlank(rs.getString(8))) {
						// process invoice date
						log.debug("** invoiceDate: " + rs.getString(8));
						invoiceDate = new SimpleDateFormat("MM/dd/yyyy", Locale.US).parse(rs.getString(8).trim());
						contract.setCreatedDate(invoiceDate);
					}
					if (StringUtils.isNotBlank(rs.getString(9))) {
						// process invoice amount
						invoiceAmount = Double.parseDouble(rs.getString(9));
						contract.setPrice(invoiceAmount);
						log.debug("invoice amount:" + invoiceAmount);
					}
					if (StringUtils.isNotBlank(rs.getString(10))) {
						// process date invoice paid
						invoiceDatePaid = new SimpleDateFormat("MM/dd/yyyy", Locale.US).parse(rs.getString(10));
						contract.setStartDate(invoiceDatePaid);
						log.debug("Contract Start date:" + invoiceDatePaid);
					}
					if (StringUtils.isNotBlank(rs.getString(11))) {
						// process status
					}

					if (StringUtils.isNotBlank(rs.getString(12))) {
						// process error
					}

					boolean exists = false;
					int poId = 0, oldPoId = 0, contractId = 0;

					try {
						log.debug("about to persist po");
						purchaseOrder = persistPurchaseOrder(purchaseOrder, ozProjectNo, poNumber, sourceExternalId);
						poId = purchaseOrder.getId();
						log.debug("Persisted po id:" + poId);
					} catch (Exception e) {
					    log.debug( "failed to persist po number:" + poNumber  + "reason:" + e.getMessage() ) ;
					}

					if (null != invoiceNumber && invoiceNumber.trim().length() > 0 && poId > 0) {
						contract.setContractType(ContractType.FRONTLIST);
						try {
								log.debug("about to persist contract");
								contractId = persistInvoice(contract, purchaseOrder, ozProjectNo, poNumber, sourceExternalId);
								log.debug("created contract id:" + contractId + " for invoice:" + contract.getNumber() );
						} catch (Exception f) {
							    log.debug( "failed to persist contract for invoice:" + contract.getNumber() + "reason:" + f.getMessage() ) ;
						}
					}

					if (exists) {
						if (oldPoId != poId) {
							rs.updateString("status", "error update existing - different ids [" + oldPoId + "," + poId + "]");
						} else {
							rs.updateString("status", "success update existing [" + poId + "]");
						}
					} else {
						rs.updateString("status", "success [" + poId + "]");
					}
					log.debug("Imported Purchase Order id " + poId);
					// getMonitor().startTimer("ImportAssetsUtility::mapRow");
					return null;
				} catch (Exception e) {
					log.debug("mapRow(): failed to process row, but we continue", e);
					rs.updateString("status", "error");
					rs.updateString("error", e.getMessage());
					return null;
				} finally {
					log.debug("about to update processed row");
					rs.updateRow();
					log.debug("row updated");
				}
			}
		});
		// getMonitor().dumpTimerStats();
	}

	public PurchaseOrder persistPurchaseOrder(PurchaseOrder purchaseOrder, String ozProjectNo, String poNumber, String sourceExternalId) throws Exception {

		log.debug("entering persistPurchaseOrder() for project " + ozProjectNo);
		// load assets
		List<Integer> poAssetIds = new ArrayList<Integer>();

		ResultSet assetData = null;
		String sql2 = "select asset_use_id, project_number from ozlist " +
		" where \"kill\" <> 'Y' and project_number = '" + ozProjectNo + "' and ( sourceId = '" +
		purchaseOrder.getSource().getExternalId() + "' or sourceId = '" + sourceExternalId + "') and po_number = '" + poNumber + "' order by project_number " ;

		PreparedStatement assetCache = assetLookupConnection.prepareStatement(sql2);
		assetData = assetCache.executeQuery();
//		double estimatedPrice = 0;
		Currency estimatedCurrency = Currency.AUD;
		while (assetData.next()) {
			Integer assetUseId = new Integer(assetData.getString(1));
			log.debug("adding assetId:" + assetUseId + " to ozPO:" + ozProjectNo);

			AssetUse au = importAssetsUtility.getAssetUseService().loadAssetView(assetUseId);
			if(purchaseOrder.getAssets().contains(au.getAsset())) continue;
			purchaseOrder.getAssets().add(au.getAsset());
			log.debug("usage:" + au.getUsage().getDescription());
			if (null == purchaseOrder.getCommonWork()) {
				purchaseOrder.setCommonWork(au.getCommonWork());
			}
//			if (au.getEstimatedCost() > 0) {
//				estimatedPrice = estimatedPrice + au.getEstimatedCost();
//			}
			if (null != au.getEstimatedCurrency()) {
				estimatedCurrency = au.getEstimatedCurrency();
			}
			if (null == purchaseOrder.getSource()) {
				purchaseOrder.setSource(au.getAsset().getSources().get(0));
			}

			log.debug("adding asset to po:" + au.getAsset().getId());
			poAssetIds .add(au.getAsset().getId());
		}
		assetData.close();
		assetCache.close();

		purchaseOrder.setEstimatedCurrency(estimatedCurrency);
	//	purchaseOrder.setNote("from Australia Project:" + ozProjectNo);
		log.debug("po:" + purchaseOrder);


		if (poAssetIds.size() < 1) {
			log.debug("***ERROR *** PO has no assets failed to persist");
			return null;
		}
		Integer[] idArray = new Integer[poAssetIds.size()];
		int i=0;
		for (Integer id : poAssetIds) {
			idArray[i++] = id;
		}

		try {
			log.debug("about to persist PO for " + ozProjectNo);
			purchaseOrder.setAssets(null);
			purchaseOrder = getPurchaseOrderRepository().save(purchaseOrder);

			getPurchaseOrderRepository().addAssetsToPO(purchaseOrder.getId(), idArray);

			log.debug("added po with id:" + purchaseOrder.getId());

		} catch(Exception s) {
			log.debug("po error:" + s.getMessage());
			log.debug(s.getStackTrace().toString());
		}

		log.debug("leaving persistPurchaseOrder() for project " + ozProjectNo);
		return purchaseOrder;
	}

	public int persistInvoice(Contract contract, PurchaseOrder po, String ozProjectNo, String poNumber, String sourceExternalId) throws Exception {
		ImportAssetsStatus status = null;

		log.debug("entering persistInvoice() for project " + ozProjectNo + " and contract:" + contract.getNumber());
		// load assets
		List<Integer> contractAssetIds = new ArrayList<Integer>();
		ResultSet assetData2 = null;
		String sql2 = "select asset_use_id, project_number from ozlist " +
		" where \"kill\" <> 'Y' and  project_number = '" + ozProjectNo + "' and (sourceId = '" +
		contract.getSource().getExternalId() + "' or sourceId = '" + sourceExternalId + "') and po_number = '" + poNumber + "' order by project_number " ;

		log.debug("about to execute sql:" + sql2);
		PreparedStatement assetCache2 =  assetLookupConnection.prepareStatement(sql2);
		assetData2 = assetCache2.executeQuery();

	//	double estimatedPrice = 0;
		Currency estimatedCurrency = Currency.AUD;
		while (assetData2.next()) {
			Integer assetUseId = new Integer(assetData2.getString(1));
			log.debug("adding assetId:" + assetUseId + " to ozProject:" + ozProjectNo);

			AssetUse au = importAssetsUtility.getAssetUseService().loadAssetView(assetUseId);
			if (contract.getAssets().contains(au.getAsset())) continue;

			log.debug("usage:" + au.getUsage().getDescription());
			if (null == contract.getCommonWork()) {
				contract.setCommonWork(au.getCommonWork());
			}
	//		if (au.getEstimatedCost() > 0) {
	//			estimatedPrice = estimatedPrice + au.getEstimatedCost();
	//		}
			if (null != au.getEstimatedCurrency()) {
				estimatedCurrency = au.getEstimatedCurrency();
			}
			if (null == contract.getSource()) {
				contract.setSource(au.getAsset().getSources().get(0));
			}

			log.debug("adding asset to contract list:" + au.getId());
			contractAssetIds .add(au.getAsset().getId());
		}

		assetData2.close();
		assetCache2.close();
		// now do contract

		contract.setPurchaseOrder(po);
		contract.setSource(po.getSource());
		contract.setContractType(ContractType.FRONTLIST);

		contract.setCurrency(po.getEstimatedCurrency());
		contract.setCommonWork(po.getCommonWork());
//		List<ConditionNode> conditionNodes = new ArrayList<ConditionNode>();
//		conditionNodes = importAssetsUtility.getConditionRepository().loadContractConditions(null, null, null, null);
//		contract.setConditionNodeTree(conditionNodes);
	//	contract.setCreatedUser(createdUser)

		log.debug("contract: " + contract);

		if (contractAssetIds.size() < 1) {
			log.debug("***ERROR *** contract has no assets failed to persist");
			return 0;
		}

		Integer[] idArray = new Integer[contractAssetIds.size()];
		int i=0;
		for (Integer id : contractAssetIds) {
			idArray[i++] = id;
		}

		try {
			contract.setDate(new Date());
			log.debug("about to persist Contract for " + ozProjectNo);
			contract = importAssetsUtility.getContractRepository().save(contract);
			log.debug("added contract with id:" + contract.getId());

			log.debug("adding assets to contract");
			importAssetsUtility.getContractRepository().addAssetsToContract(contract, idArray);
			log.debug("assets added");

		} catch (Exception s) {
			log.debug("contract error:" + s.getMessage());
			log.debug(s.getStackTrace().toString());
		}

		log.debug("leaving persistInvoice() for project " + ozProjectNo + " and invoice:" + contract.getNumber());
		return contract.getId();
	}


	/**
	 * Imports one asset from the Aus database.
	 */
	public ImportAssetsStatus populateAusAssetUse(ExtendedAssetUse assetUse) {
		log.debug("populateAusAssetUse()...begin");
		// set the aus flag
		assetUse.setImportSource(ImportSource.FROM_AUSTRALIA);
	//	assetUse.setStatus(PermissionStatus.MIGRATED_FROM_AUSTRALIA);
		assetUse.getAsset().setFeeRequired(true);
		assetUse.getAsset().setRoyaltyFree(false);
		assetUse.getAsset().setImportSource(ImportSource.FROM_AUSTRALIA);
		return getImportAssetsUtility().populateAssetUse(assetUse);
	}


	public ImportAssetsUtility getImportAssetsUtility() {
		return importAssetsUtility;
	}

	public void setImportAssetsUtility(ImportAssetsUtility importAssetsUtility) {
		this.importAssetsUtility = importAssetsUtility;
	}

	public PurchaseOrderRepository getPurchaseOrderRepository() {
		return purchaseOrderRepository;
	}

	public void setPurchaseOrderRepository(PurchaseOrderRepository purchaseOrderRepository) {
		this.purchaseOrderRepository = purchaseOrderRepository;
	}

	public DataSource getDataSource() {
		return dataSource;
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	private class UpdatablePreparedStatementCreator implements PreparedStatementCreator, SqlProvider {
		private final String sql;

		public UpdatablePreparedStatementCreator(String sql) {
			this.sql = sql;
		}

		@Override
		public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
			return con.prepareStatement(this.sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
		}

		@Override
		public String getSql() {
			return this.sql;
		}
	}
}
