package com.wiley.permissions.domain.persistence.permissions;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.wiley.permissions.common.bean.BeanMergeException;
import com.wiley.permissions.common.bean.BeanUtility;

/**
 *
 *
 * @since JDK 1.6, JUnit 4.11
 * @author smarkoff
 */
public class AssetBeanMergeTest {

	@Test
	public void mergeAsset() throws BeanMergeException {
		Asset source = new Asset();
		Asset target = new Asset();

		source.setCreditLine("credit");
		target.setCreditLine("(blank)");

		source.setDescription("different");
		target.setDescription("(blank)");

		BeanUtility.merge(source, target);
		    // throws BeanMergeException

		// For some reason the bean merge does not work properly with Asset.description
		// (but does work fine with creditLine).
		// Maybe something to do with fact that Asset has a base class with getDescription().

		String msg = "expected value was [credit] but got [" + target.getCreditLine() + "]";
		assertTrue(msg, target.getCreditLine().equals("credit"));

		msg = "expected value was [different] but got [" + target.getDescription() + "]";
		assertTrue(msg, target.getDescription().equals("different"));
	}

	@Test
	public void mergeAssetUse() throws BeanMergeException {
		ExtendedAssetUse e = new ExtendedAssetUse();
		AssetUse au = new AssetUse();
		BeanUtility.merge(e, au);
	}
}
