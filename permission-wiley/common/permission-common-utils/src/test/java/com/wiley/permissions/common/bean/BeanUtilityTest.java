/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.wiley.permissions.common.bean;

import static org.junit.Assert.assertTrue;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Map;

import javax.persistence.Transient;

import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.wiley.permissions.common.mock.TestBean;

/**
 *
 * @author ttidwell, smarkoff
 */
public class BeanUtilityTest {
	private TestBean source = null;
	private TestBean target = null;


	public BeanUtilityTest() {
	}

	@BeforeClass
	public static void setUpClass() throws Exception {
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
	}

	@Before
	public void setUp() {
		source = new TestBean();

		source.setNoMergeProperty("Test 2");

		source.setBooleanProperty1(true);
		source.setBooleanProperty2(true);

		source.setObjectProperty1(null);
		source.setObjectProperty2(null);
		source.setObjectProperty3("Source");

		byte [] byteArray = { 1, 2, 3 };
		source.setArrayProperty1(byteArray);

		source.setByteProperty1(new Integer(15).byteValue());
		source.setByteProperty2(new Integer(0).byteValue());
		source.setByteProperty3(new Integer(1).byteValue());
		source.setByteProperty4(new Integer(15).byteValue());

		source.setCharProperty1('b');
		source.setCharProperty2((char) 0);
		source.setCharProperty3('a');
		source.setCharProperty4('b');

		source.setShortProperty1((short) 15);
		source.setShortProperty2((short) -1);
		source.setShortProperty3((short) 0);
		source.setShortProperty4((short) 15);

		source.setIntProperty1(15);
		source.setIntProperty2(-1);
		source.setIntProperty3(0);
		source.setIntProperty4(15);

		source.setLongProperty1(15L);
		source.setLongProperty2(-1L);
		source.setLongProperty3(0L);
		source.setLongProperty4(15L);

		source.setFloatProperty1(15F);
		source.setFloatProperty2(-1F);
		source.setFloatProperty3(0F);
		source.setFloatProperty4(15F);

		source.setDoubleProperty1(15D);
		source.setDoubleProperty2(-1D);
		source.setDoubleProperty3(0D);
		source.setDoubleProperty4(15D);

		source.setMethodAnnotationProperty("Source");

		source.setNoAnnotationProperty1(null);
		source.setNoAnnotationProperty2("Source");

		target = new TestBean();

		target.setNoMergeProperty("Test");

		target.setObjectProperty1("Target");
		target.setObjectProperty2("Target");
		target.setObjectProperty3("Target");

		target.setArrayProperty1(null);

		target.setBooleanProperty1(false);
		target.setBooleanProperty2(false);

		target.setByteProperty2(new Integer(15).byteValue());
		target.setByteProperty3(new Integer(15).byteValue());

		target.setCharProperty2('b');
		target.setCharProperty3('b');

		target.setShortProperty2((short) 15);
		target.setShortProperty3((short) 15);

		target.setIntProperty2(15);
		target.setIntProperty3(15);

		target.setLongProperty2(15L);
		target.setLongProperty3(15L);

		target.setFloatProperty2(15F);
		target.setFloatProperty3(15F);

		target.setDoubleProperty2(15D);
		target.setDoubleProperty3(15D);

		target.setMethodAnnotationProperty("Target");

		target.setNoAnnotationProperty1("Target");
		target.setNoAnnotationProperty2("Target");
	}

	@After
	public void tearDown() {
	}

	@Test
	public void merge()
	throws BeanMergeException {
		BeanUtility.merge(source, target);

		System.out.println(target.toString());

		assert(target.getNoMergeProperty().equals("Test"));

		assert(target.getObjectProperty1() == null);
		assert(target.getObjectProperty2().equals("Target"));
		assert(target.getObjectProperty3().equals("Source"));

		byte [] array1 = target.getArrayProperty1();
		assert(array1[0] == 1);
		assert(array1[1] == 2);
		assert(array1[1] == 3);

		assert(target.isBooleanProperty1() == true);
		assert(target.isBooleanProperty2() == true);

		assert(target.getByteProperty1() == 15);
		assert(target.getByteProperty2() == 15);
		assert(target.getByteProperty3() == 15);
		assert(target.getByteProperty4() == 15);

		assert(target.getCharProperty1() == 'b');
		assert(target.getCharProperty2() == 'b');
		assert(target.getCharProperty3() == 'b');
		assert(target.getCharProperty4() == 'b');

		assert(target.getShortProperty1() == 15);
		assert(target.getShortProperty2() == 15);
		assert(target.getShortProperty3() == 15);
		assert(target.getShortProperty4() == 15);

		assert(target.getIntProperty1() == 15);
		assert(target.getIntProperty2() == 15);
		assert(target.getIntProperty3() == 15);
		assert(target.getIntProperty4() == 15);

		assert(target.getLongProperty1() == 15);
		assert(target.getLongProperty2() == 15);
		assert(target.getLongProperty3() == 15);
		assert(target.getLongProperty4() == 15);

		assert(target.getFloatProperty1() == 15);
		assert(target.getFloatProperty2() == 15);
		assert(target.getFloatProperty3() == 15);
		assert(target.getFloatProperty4() == 15);

		assert(target.getDoubleProperty1() == 15);
		assert(target.getDoubleProperty2() == 15);
		assert(target.getDoubleProperty3() == 15);
		assert(target.getDoubleProperty4() == 15);

		assert(target.getMethodAnnotationProperty().equals("Target"));

		assert(target.getNoAnnotationProperty1().equals("Target"));
		assert(target.getNoAnnotationProperty2().equals("Source"));
	}

	@Test
	public void merge2() throws BeanMergeException {
		// test merging a class with a subclass where the subclass has more methods/properties
		Bean1a b1a = new Bean1a();
		b1a.setField4(20);
		Bean1b b1b = new Bean1b();
		b1b.setField4(50);
		BeanUtility.merge(b1b, b1a);
		String msg = "expected 100 but got " + b1a.getField4();
		assertTrue(msg, b1a.getField4() == 100);

		// test merging two classes that do not have a common base class but have some of the same named properties
		b1a.setField1(10);
		Bean2a b2a = new Bean2a();
		BeanUtility.merge(b1a, b2a);
		msg = "expected 10 but got " + b2a.getField1();
		assertTrue(msg, b2a.getField1() == 10);
	}

	@Test
	public void getAllProperties() {
		List<BeanProperty> list = BeanUtility.getAllProperties(Bean1b.class);
		for (BeanProperty p : list) {
			Map<Class<?>, Annotation> annotationMap = p.getAnnotations();
			StringBuilder sb = new StringBuilder();
			for (Annotation a : annotationMap.values()) {
				sb.append(a.toString());
			}
			System.out.println("name = " + p.getName()
				+ ", field = " + p.getField()
				+ ", readMethod = " + p.getReadMethod().getName()
				+ ", readMethod className = " + p.getReadMethod().getDeclaringClass().getSimpleName()
				+ ", annotations = " + sb
				+ ", writeMethod = " + (p.getWriteMethod() == null ? "null" : p.getWriteMethod().getName()));
		}
	}
}

@Retention(RetentionPolicy.RUNTIME)
@interface TestAnno1 {

}

class Bean1a {
	private int field1;
	private int field2;
	private int field3;
	private int field4;

	public int getField1() {
		return field1;
	}

	public void setField1(int field1) {
		this.field1 = field1;
	}

	public int getField2() {
		return field2;
	}

	public void setField2(int field2) {
		this.field2 = field2;
	}

	@Transient
	public int getField3() {
		return field3;
	}

	public void setField3(int field3) {
		this.field3 = field3;
	}

	@TestAnno1
	public int getField4() {
		return field4;
	}

	public void setField4(int field4) {
		this.field4 = field4;
	}
}

class Bean2a {
	private int field1;
	private int field2;

	public int getField1() {
		return field1;
	}

	public void setField1(int field1) {
		this.field1 = field1;
	}

	public int getField2() {
		return field2;
	}

	public void setField2(int field2) {
		this.field2 = field2;
	}
}

class Bean1b extends Bean1a implements HibernateProxy {
	private static final long serialVersionUID = 1L;

	private int field5;
	private int field6;

	@Override
	public int getField2() {
		return super.getField2();
	}

	@Override
	public int getField3() {
		return super.getField3();
	}

	@Override
	public int getField4() {
		//return super.getField4();
		// behaves different than super.getField4()
		return 100;
	}

	@Transient
	public int getField5() {
		return field5;
	}

	@TestAnno1
	public int getField6() {
		return field6;
	}

	public void setField6(int field6) {
		this.field6 = field6;
	}

	@Override
	public LazyInitializer getHibernateLazyInitializer() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object writeReplace() {
		// TODO Auto-generated method stub
		return null;
	}
}