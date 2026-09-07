package com.wiley.permissions.domain.persistence.permissions;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.wiley.permissions.common.bean.BeanMergeException;
import com.wiley.permissions.common.bean.BeanUtility;

/**
 *
 * @since JDK 1.6, JUnit 4.11
 * @author smarkoff
 */
public class AssetUseBeanMergeTest {

	@Test
	public void test() throws BeanMergeException {
		AssetUse source = new AssetUse();
		AssetUse target = new AssetUse();

		source.setCaption("caption");
		target.setCaption("(blank)");

		Asset sourceAsset = new Asset();
		source.setAsset(sourceAsset);
		sourceAsset.setCreditLine("credit");
		Asset targetAsset = new Asset();
	    target.setAsset(targetAsset);
	    targetAsset.setCreditLine("(blank)");

		BeanUtility.merge(source, target);
		    // throws BeanMergeException

		String msg = "expected value was [caption] but got [" + target.getCaption() + "]";
		assertTrue(msg, target.getCaption().equals("caption"));

		msg = "expected value was [different] but got [" + target.getAsset().getCreditLine() + "]";
		assertTrue(msg, target.getAsset().getCreditLine().equals("credit"));
	}
}
