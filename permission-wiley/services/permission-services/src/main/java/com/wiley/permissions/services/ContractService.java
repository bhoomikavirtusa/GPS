package com.wiley.permissions.services;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.lucene.queryparser.classic.ParseException;
import org.mule.api.MuleException;

import com.wiley.permissions.common.dispatcher.DispatcherException;
import com.wiley.permissions.domain.persistence.permissions.Account;
import com.wiley.permissions.domain.persistence.permissions.Amendment;
import com.wiley.permissions.domain.persistence.permissions.CompCopy;
import com.wiley.permissions.domain.persistence.permissions.ConditionNode;
import com.wiley.permissions.domain.persistence.permissions.Contract;
import com.wiley.permissions.domain.persistence.permissions.Contract.PaymentType;
import com.wiley.permissions.domain.persistence.permissions.ContractList;
import com.wiley.permissions.domain.persistence.permissions.PaymentRequest;
import com.wiley.permissions.domain.persistence.permissions.Source;
import com.wiley.permissions.domain.persistence.permissions.UsageConditionSize;
import com.wiley.permissions.domain.rightslink.License;
import com.wiley.permissions.persistence.PersistenceException;
import com.wiley.permissions.security.web.PermissionsSecurityException;

public interface ContractService {

	public Contract save(Contract contract, List<ConditionNode> conditions, Map<Integer,
			List<UsageConditionSize>> usageSizeMap, List<Integer> returnRemovedAssetIds) throws Exception;

	public Contract save(Contract contract, List<ConditionNode> conditions, Map<Integer,
			List<UsageConditionSize>> usageSizeMap, List<Integer> returnRemovedAssetIds, boolean updateStatus) throws Exception;

	public Amendment save(Amendment amendment) throws Exception;

	public void updateStatusForContract(int contractId, List<Integer> removedAssetIds) throws Exception;

	public PaymentRequest payContract(int contractId, PaymentType pType, Date pDate, Account account, String costCenter, int checkNumber) throws Exception;

	public List<Integer> refreshAssetIndex(Integer contractId) throws Exception;
	/**
	 * resets the Asset status, removes the ContractAsset plus all conditions
	 * associated with it
	 * @param product
	 * @param contractId
	 * @param assetId
	 * @throws Exception
	 * @throws NumberFormatException
	 */
	public void removeAssetFromContract(Integer contractId, Integer assetId)
			throws Exception;

	public void addAssetsToContract(Integer contractId, Integer[] assetIds) throws Exception;

	/**
	 * Will send a request to the core web service to request an order ID.
	 *
	 * @return CompCopy
	 * @throws PersistenceException
	 */
	public CompCopy requestCompCopyOrder(CompCopy origCompCopy)
			throws PermissionsSecurityException, PersistenceException, MuleException, DispatcherException;

	public Account getAccountByNumberAndSubCode(String accountNumber, String subCode) throws PersistenceException;

	/** Returns a list of 0 or 1 Contracts. */
	public ContractList loadLatestForAssetSourceCW(int assetId, int sourceId, int cwId)
	throws PersistenceException, ParseException, IOException;

	/** If you all you need is the latest contract, call the above method instead for efficiency. */
	public ContractList loadListForAssetSourceCW(int assetId, int sourceId, int cwId)
	throws PersistenceException, ParseException, IOException;

	public void calculateOutOfCompliance() throws Exception;

	/****************************************************
	 * VIEWS LOAD METHODS							    *
	 * (// preload stuff needed by Controller and jsp) 	*
	 ****************************************************/
	public List<Contract> loadContractListView (int cwId);

	public List<PaymentRequest> loadPaymentRequestListView (int cwId);

	public PaymentRequest loadPaymentRequestView(Integer paymentRequestId) throws PersistenceException;

	public Map<Source, List<Contract>> loadAssetPermissionsView (int auId) throws Exception;

	/**
	 * @param contractId  0 means create new contract
	 * @param refreshAuIds  Should be passed in empty to be filled (like 2nd return variable)
	 *
	 * Returns list of newly created auIds.
	 */
	public List<Integer> saveLicense(License license, int cwId, List<ConditionNode> conditions,
		int numAssets, int contractId, List<Integer> refreshAuIds) throws Exception;
}