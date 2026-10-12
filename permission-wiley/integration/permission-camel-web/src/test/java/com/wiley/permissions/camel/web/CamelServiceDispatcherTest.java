package com.wiley.permissions.camel.web;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CamelServiceDispatcherTest {

	@Test
	public void toDirect_stripsVmScheme() {
		assertEquals("direct:services.PEProductSearch",
				CamelServiceDispatcher.toDirect("vm://services.PEProductSearch"));
	}

	@Test
	public void toDirect_leavesDirectUnchanged() {
		assertEquals("direct:gps.camel.web.ping",
				CamelServiceDispatcher.toDirect("direct:gps.camel.web.ping"));
	}

	@Test
	public void toDirect_prefixesBarePath() {
		assertEquals("direct:assetService.handleGetAssetMessage",
				CamelServiceDispatcher.toDirect("assetService.handleGetAssetMessage"));
	}
}
