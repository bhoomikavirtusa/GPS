package com.wiley.permissions.validator.service.permissionstatus;

import com.wiley.permissions.domain.persistence.permissions.AssetUse;
import com.wiley.permissions.domain.persistence.permissions.PermissionStatus;
import com.wiley.permissions.services.message.UpdatePermissionStatusInput;


/**
 *
 * @author Sreenath
 * @version 1.1 Created on Jun 1, 2008 at 4:22:25 PM
 */
public interface PermissionStatusValidationService {

	//public void updatePermissionStatusForCWId(Integer cwId);

	//public void updatePermissionStatusForAssetCollection(Collection<Integer> idCollection);

	public Integer updatePermissionStatusForAssetUseCollection(UpdatePermissionStatusInput input);

	public boolean updatePermissionStatus(int auId);  // returns true if status changed

	public PermissionStatus updatePermissionStatus(AssetUse au);  // for unit testing only
}
