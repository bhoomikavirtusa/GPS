package com.wiley.permissions.services;

import java.util.List;

import com.wiley.permissions.domain.persistence.permissions.PurchaseOrder;
import com.wiley.permissions.domain.persistence.permissions.PurchaseOrderList;
import com.wiley.permissions.persistence.PersistenceException;

public interface PurchaseOrderService {

	public void removeAssetFromPO(Integer purchaseOrderId, Integer assetId)
			throws Exception;

	public void addAssetsToPO(Integer purchaseOrderId, Integer[] assetIds) throws Exception;

	public PurchaseOrder save(PurchaseOrder purchaseOrder)
			throws Exception;

	public void updateStatusForPO(Integer poId) throws Exception;

	public PurchaseOrderList loadLatestForAssetSourceCW(int assetId, int sourceId, int cwId) throws PersistenceException;

	/****************************************************
	 * VIEWS LOAD METHODS							    *
	 * (// preload stuff needed by Controller and jsp) 	*
	 ****************************************************/

	public List<PurchaseOrder> loadPOListView (int cwId);

	public PurchaseOrder loadPurchaseOrderForm(Integer purchaseOrderId, Integer cwId, Integer sourceId, Integer userId) throws Exception;
}
