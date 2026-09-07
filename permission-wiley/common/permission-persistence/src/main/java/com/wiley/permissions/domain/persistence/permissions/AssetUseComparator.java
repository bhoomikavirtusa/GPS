package com.wiley.permissions.domain.persistence.permissions;

import java.util.Comparator;

/**
 * Comparator to sort a collection of AssetUse by Component, Position, and Description.
 *
 * @author smarkoff
 */
public class AssetUseComparator implements Comparator<AssetUse> {
	public int compare(AssetUse o1, AssetUse o2) {
		// Will sort nulls to be at the bottom
		if (o1.getComponentName() != null || o2.getComponentName() != null) {
			if (o1.getComponentName() == null)  return 1;
			if (o2.getComponentName() == null)  return -1;
			int ret = o1.getComponentName().compareTo(o2.getComponentName());
			if (ret != 0) return ret;
		}

		if (o1.getPosition() != null || o2.getPosition() != null) {
			if (o1.getPosition() == null)  return 1;
			if (o2.getPosition() == null)  return -1;
			int ret = o1.getPosition().compareTo(o2.getPosition());
			if (ret != 0) return ret;
		}

		// Description is a mandatory field (non-nullable)
		return o1.getAsset().getDescription().compareTo(o2.getAsset().getDescription());
	}
}
