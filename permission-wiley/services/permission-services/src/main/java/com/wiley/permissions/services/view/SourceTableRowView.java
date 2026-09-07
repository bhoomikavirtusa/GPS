package com.wiley.permissions.services.view;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.permissions.domain.persistence.permissions.PermissionStatus;
import com.wiley.permissions.services.AssetUseSearchResult;

public class SourceTableRowView {

	private static final Log log = LogFactory.getLog(SourceTableRowView.class);

	private String name;
	private int sourceId;
	private String contactName;
	private String contactEmail;
	private int totalCount;
	private int requestedCount;
	private int enteredCount;
	private int unrequestedCount;
	private boolean disabled;
	private int needPaymentCount;

	private final Map<Integer, Object[]> assetMap = new HashMap<Integer, Object[]>();

	public SourceTableRowView() {
		super();
	}

	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}

	public boolean isDisabled() {
		return disabled;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getSourceId() {
		return sourceId;
	}

	public void setSourceId(int sourceId) {
		this.sourceId = sourceId;
	}

	public String getContactName() {
		return contactName;
	}

	public void setContactName(String contactName) {
		this.contactName = contactName;
	}

	public String getContactEmail() {
		return contactEmail;
	}

	public void setContactEmail(String contactEmail) {
		this.contactEmail = contactEmail;
	}

	public int getTotalCount() {
		return totalCount;
	}

	public void setTotalCount(int assetCount) {
		this.totalCount = assetCount;
	}

	public int getRequestedCount() {
		return requestedCount;
	}

	public void setRequestedCount(int requestedCount) {
		this.requestedCount = requestedCount;
	}

	public int getNeedPaymentCount() {
		return needPaymentCount;
	}

	public void setNeedPaymentCount(int needPaymentCount) {
		this.needPaymentCount = needPaymentCount;
	}

	public int getEnteredCount() {
		return enteredCount;
	}

	public void setEnteredCount(int enteredCount) {
		this.enteredCount = enteredCount;
	}

	public int getUnrequestedCount() {
		return unrequestedCount;
	}

	public void setUnrequestedCount(int unrequestedCount) {
		this.unrequestedCount = unrequestedCount;
	}

	public void addAssetUse(AssetUseSearchResult au) {
		Integer aid = new Integer(au.getAssetId());
		String sStatus = au.getPermissionStatusCode();
		boolean needsPaymentRequest = au.isNeedPaymentRequest();

		if (StringUtils.isBlank(sStatus)) {
			log.error("addAssetUse(): blank status for assetId = " + aid);
			return;
		}

		PermissionStatus status = null;
		try {
			status = PermissionStatus.getPermissionStatusForCode(sStatus);
		}
		catch (IllegalArgumentException ie) {
			// this should not happen but log if it does
			log.error("addAssetUse(): assetId = " + aid, ie);
			return;
		}

		if (assetMap.containsKey(aid)) {
			Object[] map = assetMap.get(aid);
			PermissionStatus oldStatus = (PermissionStatus) map[0];
			if (null == oldStatus || status.getStatusRating() < oldStatus.getStatusRating()) {
				assetMap.put(aid, new Object[] {status, needsPaymentRequest});
			}
		}
		else {
			assetMap.put(aid, new Object[] {status, needsPaymentRequest});
		}
	}

	/**
	 * before we send the data to the client, we calculate the totals
	 */
	public void calculateTotals() {
		Set<Integer> aids = assetMap.keySet();
		for (Integer aid : aids) {
			this.totalCount++;
			Object[] array = assetMap.get(aid);
			PermissionStatus status = (PermissionStatus)array[0];
			if (status == null)  continue;  // should never happen

			boolean needsPaymentRequest = (Boolean)array[1];
			if (PermissionStatus.isUnrequested(status))
				unrequestedCount++;
			if (PermissionStatus.isFormSent(status)
					|| status.equals(PermissionStatus.AMENDMENT_NEEDED)
					|| status.equals(PermissionStatus.AMENDMENT_SENT))
				requestedCount++;
			if (PermissionStatus.isGranted(status) || PermissionStatus.isInsufficient (status)) {
				enteredCount++;
				if (needsPaymentRequest) {
					needPaymentCount++;
				}
			}
		}
	}

}
