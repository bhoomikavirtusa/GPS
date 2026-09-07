package com.wiley.permissions.services.imports;

import java.util.ArrayList;
import java.util.LinkedHashMap;	// Added for DM-533
import java.util.List;

import com.wiley.permissions.common.utils.SrcSrcRefInvCombination;	// Added for DM-533
import com.wiley.permissions.domain.persistence.permissions.ExtendedAssetUse;

public class ImportAssetsStatus {

	private int totalCount;
	private int failureCount;
	private int processedCount = 0;
	private boolean statusOK = true;
	private List<ExtendedAssetUse> goodRecords = new ArrayList<ExtendedAssetUse>();
	private String errorMessage;
	LinkedHashMap<SrcSrcRefInvCombination, Integer> srcSrcRefInvStatusMap = new LinkedHashMap<SrcSrcRefInvCombination, Integer>();	//	Added for DM-533

	public int getTotalCount()
	{
		return totalCount;
	}
	public void setTotalCount(int totalCount)
	{
		this.totalCount = totalCount;
	}

	public int getFailureCount()
	{
		return failureCount;
	}
	public void setFailureCount(int failureCount)
	{
		this.failureCount = failureCount;
	}

	public boolean isStatusOK()
	{
		return statusOK;
	}
	public void setStatusOK(boolean statusOK)
	{
		this.statusOK = statusOK;
	}

	public String getErrorMessage()
	{
		return errorMessage;
	}
	public void setErrorMessage(String errorMessage)
	{
		this.errorMessage = errorMessage;
	}
	public List<ExtendedAssetUse> getGoodRecords()
	{
		return goodRecords;
	}
	public void setGoodRecords(List<ExtendedAssetUse> goodRecords)
	{
		this.goodRecords = goodRecords;
	}
	public int getProcessedCount()
	{
		return processedCount;
	}
	public void setProcessedCount(int processedCount)
	{
		this.processedCount = processedCount;
	}
	// Start : Added for DM-533

	public LinkedHashMap<SrcSrcRefInvCombination, Integer> getSrcSrcRefInvStatusMap() {
		return srcSrcRefInvStatusMap;
	}
	public void setSrcSrcRefInvStatusMap(
			LinkedHashMap<SrcSrcRefInvCombination, Integer> srcSrcRefInvStatusMap) {
		this.srcSrcRefInvStatusMap = srcSrcRefInvStatusMap;
	}
	// End : Added for DM-533
	@Override
	public String toString()
	{
		return "ImportAssetsStatus [totalCount=" + totalCount + ", failureCount=" + failureCount + ", processedCount=" + processedCount + ", statusOK=" + statusOK + ", errorMessage=" + errorMessage
				+ "]";
	}
}
