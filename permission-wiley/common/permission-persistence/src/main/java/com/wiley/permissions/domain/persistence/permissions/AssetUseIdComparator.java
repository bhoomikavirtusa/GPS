package com.wiley.permissions.domain.persistence.permissions;

import java.util.Comparator;

/**
 * Comparator to sort a collection of AssetUse by id.
 *
 * @author smarkoff
 */
public class AssetUseIdComparator implements Comparator<AssetUse> {
	public int compare(AssetUse o1, AssetUse o2) {
		// id is a mandatory field (non-nullable)
		return o1.getId().compareTo(o2.getId());
	}
}
