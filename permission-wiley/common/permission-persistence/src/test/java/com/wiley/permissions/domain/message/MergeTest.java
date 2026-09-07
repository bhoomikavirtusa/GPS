package com.wiley.permissions.domain.message;

import javax.persistence.Entity;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.wiley.permissions.common.bean.BeanUtility;
import com.wiley.permissions.domain.persistence.permissions.Product;
import com.wiley.permissions.domain.persistence.permissions.Source;

/**
 *
 * @author ttidwell
 */
public class MergeTest {

	public MergeTest() {
	}

	@BeforeClass
	public static void setUpClass()
	throws Exception {
	}

	@AfterClass
	public static void tearDownClass()
	throws Exception {
	}

	@Before
	public void setUp() {
	}

	@After
	public void tearDown() {
	}

	@Test
	public void annotationTest() {
		Entity entity = BeanUtility.getAnnotation(Entity.class, Product.class);

		if (entity != null) {
			System.out.println(entity);
		}
		else {
			System.out.println("No Annotation Found For Product");
		}

		Entity entity2 = BeanUtility.getAnnotation(Entity.class, Source.class);

		if (entity2 != null) {
			System.out.println(entity2);
		}
		else {
			System.out.println("No Annotation Found For PermSource");
		}
	}
}