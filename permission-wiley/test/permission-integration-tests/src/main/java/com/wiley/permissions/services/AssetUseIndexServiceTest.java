package com.wiley.permissions.services;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.lucene.queryparser.classic.ParseException;
import org.junit.Before;
import org.junit.Test;

import com.wiley.permissions.domain.persistence.permissions.Asset;
import com.wiley.permissions.domain.persistence.permissions.AssetUse;
import com.wiley.permissions.domain.persistence.permissions.CommonWork;
import com.wiley.permissions.domain.persistence.permissions.MediaType;
import com.wiley.permissions.domain.persistence.permissions.OwnerType;
import com.wiley.permissions.domain.persistence.permissions.PermissionStatus;
import com.wiley.permissions.domain.persistence.permissions.Product;
import com.wiley.permissions.domain.persistence.permissions.Role;
import com.wiley.permissions.domain.persistence.permissions.Usage;
import com.wiley.permissions.domain.persistence.permissions.User;
import com.wiley.permissions.domain.persistence.permissions.UserToRole;

/**
 * Unit tests for AssetUseIndexService.
 *
 * @since JDK 1.6, JUnit 4.11
 * @author smarkoff
 * @version $Id: AssetUseIndexServiceTest.java,v 1.4 2014-05-13 20:12:18 smarkoff Exp $
 */
public class AssetUseIndexServiceTest {

	private AssetUseIndexService service;

	@Before
	public void setup() throws IOException {
		service = new AssetUseIndexService(true);

		ArrayList<AssetUse> auList = new ArrayList<AssetUse>();

		AssetUse au = new AssetUse();

		Asset asset = new Asset();
		asset.setDescription("Photo of an elephant in Calcutta, India.");
		asset.setMediaType(MediaType.PHOTO);
		asset.setOwnerType(OwnerType.THIRD_PARTY);
		asset.setVendorId("ABC123");
		asset.setManaged(false);
		asset.setFeeRequired(true);

		CommonWork cw = new CommonWork();
		cw.setCode("perm.cw.99");

		Product product = new Product();
		product.setId(99);
		product.setTitle("Bert's Book of Bicycle Repair foo'bar 'inside' \"double\" forward\\slash back\\slash foo_bar (Par) foo(s)");
		product.setCommonWork(cw);
		product.setCwPrimary(true);

		ArrayList<Product> products = new ArrayList<Product>();
		products.add(product);
		cw.setProducts(products);

		ArrayList<UserToRole> u2pList = new ArrayList<UserToRole>();
		User author = new User();
		author.setFirstName("John");
		author.setLastName("Smith");
		User productionEditor = new User();
		productionEditor.setFirstName("Jane");
		productionEditor.setLastName("Doe");

		UserToRole u2p1 = new UserToRole();
		u2p1.setUser(author);
		u2p1.setProduct(product);
		u2p1.setRole(Role.AUTHOR);

		UserToRole u2p2 = new UserToRole();
		u2p2.setUser(productionEditor);
		u2p2.setProduct(product);
		u2p2.setRole(Role.PRODUCTION_EDITOR);

		u2pList.add(u2p1);
		u2pList.add(u2p2);
		product.setUsers(u2pList);

		au.setAsset(asset);
		au.setCommonWork(cw);
		au.setStatus(PermissionStatus.UNREQUESTED);
		au.setUsage(Usage.FIGURE);
		au.setPosition("4-2");
		au.setFoundOn("pages 5-10");
		au.setManuscriptPage("157");

		auList.add(au);
		service.buildIndexByWhole(auList);
	}

	@Test
	public void searchIndex() throws ParseException, IOException {
		AssetSearchForm form = new AssetSearchForm();

		// product stuff
		form.setIncludeTitle(true);
		form.setTitle("repair book");
		searchIndex(form, 1, 1);
		form.setTitle("bert's");
		searchIndex(form, 1, 2);
		form.setTitle("foo'bar");
		searchIndex(form, 1, 3);
		form.setTitle("'inside'");
		searchIndex(form, 1, 4);
		form.setTitle("inside");
		searchIndex(form, 1, 5);
		form.setTitle("double");
		searchIndex(form, 1, 6);
		form.setTitle("\"double\"");
		searchIndex(form, 1, 7);
		form.setTitle("forward\\slash");
		searchIndex(form, 1, 8);
		form.setTitle("back\\slash");
		searchIndex(form, 1, 9);
		form.setTitle("foo_bar");
		searchIndex(form, 1, 10);
		form.setTitle("(Par)");
		searchIndex(form, 1, 11);
		form.setTitle("Par");
		searchIndex(form, 1, 12);
		form.setTitle("foo(s)");
		searchIndex(form, 1, 13);
		form.setTitle("foos");
		searchIndex(form, 1, 14);
		form.setIncludeTitle(false);

		form.setIncludeAuthor(true);
		form.setAuthor("smith");
		searchIndex(form, 1, 15);
		form.setIncludeAuthor(false);

		form.setIncludeUserName(true);
		form.setUserName("Smith");
		searchIndex(form, 0, 16);
		form.setUserName("Jane");
		searchIndex(form, 1, 17);
		form.setIncludeUserName(false);

		// asset stuff
		form.setIncludeDescription(true);
		form.setDescription("photo ele");
		searchIndex(form, 1, 20);
		form.setDescription("photo of an ele");
		searchIndex(form, 1, 21);
		form.setDescription("calcutta india");
		searchIndex(form, 1, 22);
		form.setDescription("calcutta india");
		searchIndex(form, 1, 23);
		form.setIncludeDescription(false);

		form.setIncludeMediaType(true);
		form.setMediaType(MediaType.PHOTO);
		searchIndex(form, 1, 24);
		form.setIncludeMediaType(false);

		form.setIncludeOwnerType(true);
		form.setOwnerType(OwnerType.WILEY);
		searchIndex(form, 0, 25);
		form.setOwnerType(OwnerType.THIRD_PARTY);
		searchIndex(form, 1, 26);
		form.setIncludeOwnerType(false);

		form.setIncludeSourceRef(true);
		form.setSourceRef("ABC");
		searchIndex(form, 0, 27);
		form.setSourceRef("ABC123");
		searchIndex(form, 1, 28);
		form.setIncludeSourceRef(false);

		form.setIncludeManaged(true);
		form.setManaged(false);
		searchIndex(form, 1, 29);
		form.setIncludeManaged(false);

		form.setIncludeFeeRequired(true);
		form.setFeeRequired(true);
		searchIndex(form, 1, 30);
		form.setIncludeFeeRequired(false);

		// asset_use stuff
		form.setIncludePermissionStatus(true);
		form.setPermissionStatus(PermissionStatus.UNREQUESTED);
		searchIndex(form, 1, 40);
		form.setIncludePermissionStatus(false);

		form.setIncludeUsage(true);
		form.setUsage(Usage.FIGURE);
		searchIndex(form, 1, 41);
		form.setIncludeUsage(false);

		form.setIncludePosition(true);
		form.setPosition("4-2");
		searchIndex(form, 1, 42);
		form.setPosition("42");
		searchIndex(form, 0, 43);
		form.setIncludePosition(false);

		form.setIncludeFoundOn(true);
		form.setFoundOn("pages 5-10");
		searchIndex(form, 1, 44);
		form.setIncludeFoundOn(false);

		form.setIncludeManuscriptPage(true);
		form.setManuscriptPage("157");
		searchIndex(form, 1, 45);
		form.setIncludeManuscriptPage(false);
	}

	private void searchIndex(AssetSearchForm form, int expectedHits, int testNumber)
	throws ParseException, IOException {
		AssetUseSearchResults results = service.searchIndex(form);
		String msg = "#" + testNumber + ": expected hits = " + expectedHits
			+ ", actual = " + results.getTotalHits();
		assertTrue(msg, expectedHits == results.getTotalHits());
	}
}
