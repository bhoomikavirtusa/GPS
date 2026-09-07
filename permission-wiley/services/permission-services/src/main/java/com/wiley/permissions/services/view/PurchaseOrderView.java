package com.wiley.permissions.services.view;

import java.util.ArrayList;
import java.util.List;

import com.wiley.permissions.domain.persistence.permissions.Asset;
import com.wiley.permissions.domain.persistence.permissions.ConditionMatchResult;
import com.wiley.permissions.domain.persistence.permissions.PurchaseOrder;

public class PurchaseOrderView {

	private PurchaseOrder purchaseOrder;
	private List<ConditionView> conditionsView = new ArrayList<ConditionView>();
	private List<Asset> usedAssetsWithPosition;
	private List<Asset> unusedAssets;
	private ConditionMatchResult matchResult;

	
	public PurchaseOrderView(PurchaseOrder po)
	{
		setPurchaseOrder (po);
	}

	public PurchaseOrder getPurchaseOrder()
	{
		return purchaseOrder;
	}

	public void setPurchaseOrder(PurchaseOrder po)
	{
		this.purchaseOrder = po;
	}

	public List<ConditionView> getConditions()
	{
		return conditionsView;
	}

	public void setConditions(List<ConditionView> conditionsView)
	{
		this.conditionsView = conditionsView;
	}

	public List<Asset> getUsedAssetsWithPosition()
	{
		return usedAssetsWithPosition;
	}

	public void setUsedAssetsWithPosition(List<Asset> usedAssetsWithPosition)
	{
		this.usedAssetsWithPosition = usedAssetsWithPosition;
	}

	public List<Asset> getUnusedAssets()
	{
		return unusedAssets;
	}

	public void setUnusedAssets(List<Asset> unusedAssets)
	{
		this.unusedAssets = unusedAssets;
	}

	public ConditionMatchResult getMatchResult()
	{
		return matchResult;
	}

	public void setMatchResult(ConditionMatchResult matchResult)
	{
		this.matchResult = matchResult;
	}

}
