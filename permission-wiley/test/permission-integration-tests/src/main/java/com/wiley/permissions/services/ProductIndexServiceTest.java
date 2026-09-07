package com.wiley.permissions.services;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.queryparser.classic.ParseException;
import org.junit.Before;
import org.junit.Test;

import com.wiley.permissions.domain.message.pe.ProductSearchResult;

/**
 * Unit tests for ProductIndexService.
 *
 * @since JDK 1.6, JUnit 4.11
 * @author smarkoff
 * @version $Id: ProductIndexServiceTest.java,v 1.37 2017-05-30 10:56:16 amahammed Exp $
 */
public class ProductIndexServiceTest {

	private ProductIndexService service;

	@Before
	public void setup() throws IOException {
		service = new ProductIndexService(true);  // throws IOException

		service.updateIndex("1", "1", null, null, null, "123", "123", "123", "The quick brown fox",
			null, null, null, null, null, null, null, null, "US",
			null, null, null, null, null, null, null, null, null,
			null, null, null, null, null, null, null, null, null, null,
			null, null, null, null, null, null, null, null, null, null);
	}

	@Test
	public void searchIndex() throws ParseException, IOException {
		searchIndex("quick", 1, 1);
	}

	private void searchIndex(String searchString, int expectedHits, int testNumber)
	throws ParseException, IOException {
		List<ProductSearchResult> list = service.searchByTitleStart(searchString, 100);
		String msg = "#" + testNumber + ": expected hits = " + expectedHits + ", actual = " + list.size();
		System.out.println(msg);
		assertTrue(msg, expectedHits == list.size());
	}

	public static void main(String [] args) throws IOException, ParseException {
		ProductIndexServiceTest test = new ProductIndexServiceTest();
		test.setup();
		test.searchIndex();
	}
}
