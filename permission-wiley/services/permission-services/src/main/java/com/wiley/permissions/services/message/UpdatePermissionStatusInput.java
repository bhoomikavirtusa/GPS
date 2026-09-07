package com.wiley.permissions.services.message;

import java.io.Serializable;
import java.util.Collection;

import com.wiley.sf.common.lang.ArgUtil;

/**
 * The input for PermissionStatusValidationService.updatePermissionStatusForAssetUseCollection()
 * must be a single Object. This object encapsulates multiple sub-objects.
 *
 * @author smarkoff
 */
public class UpdatePermissionStatusInput implements Serializable {

	private static final long serialVersionUID = 1L;

	private Collection<Integer> auIdCollection;
	private boolean sendUpdateMessageForStatusChanges;

	/**
	 * Only reason we have the empty constructor (and setters) is for serialization.
	 */
	public UpdatePermissionStatusInput() {

	}

	/**
	 * @param auIdCollection  Must be non-null (may be empty)
	 */
	public UpdatePermissionStatusInput(Collection<Integer> auIdCollection, boolean sendUpdateMessageForStatusChanges) {
		ArgUtil.notNull(auIdCollection, "auIdCollection");

		this.auIdCollection = auIdCollection;
		this.sendUpdateMessageForStatusChanges = sendUpdateMessageForStatusChanges;
	}

	public Collection<Integer> getAuIdCollection() {
		return auIdCollection;
	}

	public boolean getSendUpdateMessageForStatusChange() {
		return sendUpdateMessageForStatusChanges;
	}

	/**
	 * @param auIdCollection  Msut be non-null
	 */
	public void setAuIdCollection(Collection<Integer> auIdCollection) {
		ArgUtil.notNull(auIdCollection, "auIdCollection");

		this.auIdCollection = auIdCollection;
	}

	public void setSendUpdateMessageForStatusChange(boolean b) {
		sendUpdateMessageForStatusChanges = b;
	}
}
