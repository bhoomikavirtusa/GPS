package com.wiley.permissions.services;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.wiley.permissions.domain.persistence.permissions.Asset;
import com.wiley.permissions.domain.persistence.permissions.AssetUse;
import com.wiley.permissions.domain.persistence.permissions.CommonWork;
import com.wiley.permissions.domain.persistence.permissions.PermissionType;
import com.wiley.permissions.domain.persistence.permissions.PurchaseOrder;
import com.wiley.permissions.domain.persistence.permissions.PurchaseOrderList;
import com.wiley.permissions.domain.persistence.permissions.Source;
import com.wiley.permissions.domain.persistence.permissions.User;
import com.wiley.permissions.domain.persistence.permissions.UserDefaults;
import com.wiley.permissions.persistence.PersistenceException;
import com.wiley.permissions.repositories.AssetUseRepository;
import com.wiley.permissions.repositories.CommonWorkRepository;
import com.wiley.permissions.repositories.ConditionRepository;
import com.wiley.permissions.repositories.PurchaseOrderRepository;
import com.wiley.permissions.repositories.SourceRepository;
import com.wiley.permissions.repositories.UserRepository;

/**
 * @version $Id: PurchaseOrderServiceImpl.java,v 1.43 2014-06-06 00:21:11 smarkoff Exp $
 */
@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = Exception.class)
public class PurchaseOrderServiceImpl extends BaseService implements PurchaseOrderService
{
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(PurchaseOrderServiceImpl.class);

	private AssetUseService assetUseService;

	private PurchaseOrderRepository poRepository;
	private AssetUseRepository assetUseRepository;
	private SourceRepository sourceRepository;
	private UserRepository userRepository;
	private CommonWorkRepository cwRepository;
	private ConditionRepository conditionRepository;


	@Override
	public void removeAssetFromPO(Integer purchaseOrderId, Integer assetId)
	throws Exception
	{
		poRepository.removeAssetFromPO(purchaseOrderId, assetId);

		PurchaseOrder purchaseOrder = poRepository.loadPurchaseOrderById(purchaseOrderId);

		List<AssetUse> auList = assetUseRepository.loadAssetUseListByCWIdAssetId(
				purchaseOrder.getCommonWork().getId(), assetId);
		getAssetUseService().updateStatusForAssetUseCollection(auList);
	}

	@Override
	public void addAssetsToPO(Integer purchaseOrderId, Integer [] assetIds) throws Exception
	{
		ArrayList<Asset> addedList = poRepository.addAssetsToPO(purchaseOrderId, assetIds);

		PurchaseOrder purchaseOrder = poRepository.loadPurchaseOrderById(purchaseOrderId);

		getAssetUseService().updateStatusForAssetCollection (purchaseOrder.getCommonWork().getId(), addedList);
	}

	/**
	 * This method is different then persistPO because the asset list is part of the PurchaseOrder object.
	 * Will not initialize with the default unused assets.
	 * (This method also used to take condition list and save that too but POs no longer have conditions.)
	 *
	 * This method might need more work for update action: delete the assets and conditions, then add them again.
	 */
	@Override
	public PurchaseOrder save(PurchaseOrder purchaseOrder)
	throws Exception
	{
		purchaseOrder = poRepository.saveRequiresNew(purchaseOrder);  // REQUIRES_NEW

		updateStatusForPO(purchaseOrder.getId());

		return purchaseOrder;
	}

	@Override
	public void updateStatusForPO(Integer poId) throws Exception {
		PurchaseOrder po = poRepository.loadPurchaseOrderById(poId);

		getAssetUseService().updateStatusForAssetCollection(po.getCommonWork().getId(), po.getAssets());
	}

	@Override
	public PurchaseOrderList loadLatestForAssetSourceCW(int assetId, int sourceId, int cwId) throws PersistenceException
	{
		return poRepository.loadLatestForAssetSourceCW (assetId, sourceId, cwId);
	}


	/****************************************************
	 * VIEWS&FORMS LOAD METHODS							    *
	 * (// preload stuff needed by Controller and jsp) 	*
	 ****************************************************/

	/**
	 * POList page
	 * @param cwId
	 * @return
	 * TODO better to create another View object instead of calculating isPaid every time
	 * is called or getPrimaryProduct, but that means too many changes
	 */
	@Override
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<PurchaseOrder> loadPOListView(int cwId) {
		List<PurchaseOrder> poList = poRepository.loadPurchaseOrdersByCWId (cwId);
		for (PurchaseOrder po : poList) {
			po.getIsPaid ();
			po.getCommonWork().getPrimaryProduct();
		}
		return poList;
	}

	/**
	 * loads the PurchaseOrder object for ManagePOForm
	 * @throws Exception
	 */
	@Override
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public PurchaseOrder loadPurchaseOrderForm(Integer purchaseOrderId, Integer cwId, Integer sourceId, Integer userId) throws Exception
	{
		PurchaseOrder purchaseOrder = new PurchaseOrder();

		if (purchaseOrderId == null) {
			if (null != sourceId) {
				Source source = getSourceRepository().loadSourceById(sourceId);
				purchaseOrder.setSource(source);

				purchaseOrder.setPermissionRequest(true);
				if (PermissionType.REUSE_PO.getCode().equals(source.getPermissionType())) {
					purchaseOrder.setPermissionRequest(false);
				}
				if (PermissionType.PREFERRED_VENDOR.getCode().equals(source.getPermissionType())) {
					purchaseOrder.setPermissionRequest(false);
				}
				purchaseOrder.getSource().getId();
			}
			// When record is inserted into table, trigger will replace
			// the value for number.
			//purchaseOrder.setNumber(Constants.TRIGGER_TEMP_VALUE); -- now done by PurchaseOrder.prePersist()
			purchaseOrder.setDate(new Date());

			User user = getUserRepository().loadUserById(userId);

			if (null == user)
				throw new Exception ("Invalid User [" + userId + "]");

			UserDefaults userDefaults = user.getUserDefaults();

			if (userDefaults != null) {
				purchaseOrder.setUserSignature(userDefaults.getUserSignature());
				purchaseOrder.setReturnAddress(userDefaults.getReturnAddress());
				purchaseOrder.setShowEstimatedCost(userDefaults.isShowEstimatedCost());
			}
			else {
				purchaseOrder.setUserSignature("");
				purchaseOrder.setReturnAddress("");
			}

			CommonWork cw = getCommonWorkRepository().loadCWById(cwId);

			purchaseOrder.setCommonWork(cw);
		}
		else { // purchaseOrderId != null
			// assume it's coming from generate purchaseOrder
			purchaseOrder = poRepository.loadPurchaseOrderById(purchaseOrderId);
			purchaseOrder.getSource().getId();
		}

		return purchaseOrder;
	}


	public AssetUseService getAssetUseService() {
		return assetUseService;
	}

	public void setAssetUseService(AssetUseService assetUseService) {
		this.assetUseService = assetUseService;
	}

	public PurchaseOrderRepository getPoRepository() {
		return poRepository;
	}

	public void setPoRepository(PurchaseOrderRepository poRepository) {
		this.poRepository = poRepository;
	}

	public AssetUseRepository getAssetUseRepository() {
		return assetUseRepository;
	}

	public void setAssetUseRepository(AssetUseRepository assetUseRepository) {
		this.assetUseRepository = assetUseRepository;
	}

	public SourceRepository getSourceRepository() {
    	return sourceRepository;
    }

	public void setSourceRepository(SourceRepository sourceRepository) {
    	this.sourceRepository = sourceRepository;
    }

	public UserRepository getUserRepository() {
		return userRepository;
	}

	public void setUserRepository(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public CommonWorkRepository getCommonWorkRepository() {
		return cwRepository;
	}

	public void setCommonWorkRepository(CommonWorkRepository cwRepository) {
		this.cwRepository = cwRepository;
	}

	public ConditionRepository getConditionRepository() {
		return conditionRepository;
	}

	public void setConditionRepository(ConditionRepository conditionRepository) {
		this.conditionRepository = conditionRepository;
	}
}
