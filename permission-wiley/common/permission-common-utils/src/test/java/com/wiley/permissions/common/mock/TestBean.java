package com.wiley.permissions.common.mock;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import com.wiley.permissions.common.bean.BeanProperty;
import com.wiley.permissions.common.bean.BeanUtility;
import com.wiley.permissions.common.bean.Merge;
import com.wiley.permissions.common.bean.Merge.PropertyProtection;


/**
 *
 * @author ttidwell
 */
public class TestBean {
	@Merge(propertyProtection = PropertyProtection.NEVER_MERGE)
	private String noMergeProperty = null;

	@Merge(propertyProtection = PropertyProtection.OVERWRITE)
	private String objectProperty1 = null;

	@Merge(propertyProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private String objectProperty2 = null;

	@Merge(propertyProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private String objectProperty3 = null;
	
	@Merge(propertyProtection = PropertyProtection.OVERWRITE)
	private byte [] arrayProperty1 = null;

	@Merge(propertyProtection = PropertyProtection.OVERWRITE)
	private byte byteProperty1 = 0;

	@Merge(propertyProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private byte byteProperty2 = 0;

	@Merge(propertyProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT, byteDefault = 0x1)
	private byte byteProperty3 = 0;

	@Merge(propertyProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private byte byteProperty4 = 0;

	@Merge(propertyProtection = PropertyProtection.OVERWRITE)
	private char charProperty1 = 0;

	@Merge(propertyProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private char charProperty2 = 0;

	@Merge(propertyProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT, charDefault = 'a')
	private char charProperty3 = 0;

	@Merge(propertyProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private char charProperty4 = 0;

	@Merge(propertyProtection = PropertyProtection.OVERWRITE)
	private boolean booleanProperty1 = false;

	@Merge(propertyProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private boolean booleanProperty2 = false;

	@Merge(propertyProtection = PropertyProtection.OVERWRITE)
	private short shortProperty1 = -1;

	@Merge(propertyProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private short shortProperty2 = -1;

	@Merge(propertyProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT, shortDefault = 0)
	private short shortProperty3 = -1;

	@Merge(propertyProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private short shortProperty4 = -1;

	@Merge(propertyProtection = PropertyProtection.OVERWRITE)
	private int intProperty1 = -1;

	@Merge(propertyProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private int intProperty2 = -1;

	@Merge(propertyProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT, integerDefault = 0)
	private int intProperty3 = -1;

	@Merge(propertyProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private int intProperty4 = -1;

	@Merge(propertyProtection = PropertyProtection.OVERWRITE)
	private long longProperty1 = -1;

	@Merge(propertyProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private long longProperty2 = -1;

	@Merge(propertyProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT, longDefault = 0)
	private long longProperty3 = -1;

	@Merge(propertyProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private long longProperty4 = -1;

	@Merge(propertyProtection = PropertyProtection.OVERWRITE)
	private float floatProperty1 = -1;

	@Merge(propertyProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private float floatProperty2 = -1;

	@Merge(propertyProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT, floatDefault = 0)
	private float floatProperty3 = -1;

	@Merge(propertyProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private float floatProperty4 = -1;

	@Merge(propertyProtection = PropertyProtection.OVERWRITE)
	private double doubleProperty1 = -1;

	@Merge(propertyProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private double doubleProperty2 = -1;

	@Merge(propertyProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT, doubleDefault = 0)
	private double doubleProperty3 = -1;

	@Merge(propertyProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private double doubleProperty4 = -1;

	private String methodAnnotationProperty = null;

	private String noAnnotationProperty1 = null;

	private String noAnnotationProperty2 = null;

	public TestBean() {

	}

	public String toString() {
		StringWriter s = new StringWriter();

		PrintWriter p = new PrintWriter(s);

		List<BeanProperty> props = BeanUtility.getAllProperties(getClass());

		for (BeanProperty prop : props) {
			Field field = prop.getField();

			Method writeMethod = prop.getWriteMethod();

			Method readMethod = prop.getReadMethod();

			Object sourceValue = null;

			if (readMethod != null) {
				try {
					sourceValue = readMethod.invoke(this, new Object[] {});
				} catch (Exception e) {
				}
			} else if (field != null) {
				try {
					sourceValue = field.get(this);
				} catch (Exception e) {

				}
			}

			p.println(prop.getName() + ": " + sourceValue);
		}

		return s.toString();
	}

	public String getNoMergeProperty() {
		return noMergeProperty;
	}

	public void setNoMergeProperty(String noMergeProperty) {
		this.noMergeProperty = noMergeProperty;
	}

	public String getObjectProperty1() {
		return objectProperty1;
	}

	public void setObjectProperty1(String objectProperty1) {
		this.objectProperty1 = objectProperty1;
	}

	public String getObjectProperty2() {
		return objectProperty2;
	}

	public void setObjectProperty2(String objectProperty2) {
		this.objectProperty2 = objectProperty2;
	}

	public String getObjectProperty3() {
		return objectProperty3;
	}

	public void setObjectProperty3(String objectProperty3) {
		this.objectProperty3 = objectProperty3;
	}

	public byte [] getArrayProperty1() {
		return arrayProperty1;
	}

	public void setArrayProperty1(byte [] arrayProperty1) {
		this.arrayProperty1 = arrayProperty1;
	}
	
	public byte getByteProperty1() {
		return byteProperty1;
	}

	public void setByteProperty1(byte byteProperty1) {
		this.byteProperty1 = byteProperty1;
	}

	public byte getByteProperty2() {
		return byteProperty2;
	}

	public void setByteProperty2(byte byteProperty2) {
		this.byteProperty2 = byteProperty2;
	}

	public byte getByteProperty3() {
		return byteProperty3;
	}

	public void setByteProperty3(byte byteProperty3) {
		this.byteProperty3 = byteProperty3;
	}

	public byte getByteProperty4() {
		return byteProperty4;
	}

	public void setByteProperty4(byte byteProperty4) {
		this.byteProperty4 = byteProperty4;
	}

	public char getCharProperty1() {
		return charProperty1;
	}

	public void setCharProperty1(char charProperty1) {
		this.charProperty1 = charProperty1;
	}

	public char getCharProperty2() {
		return charProperty2;
	}

	public void setCharProperty2(char charProperty2) {
		this.charProperty2 = charProperty2;
	}

	public char getCharProperty3() {
		return charProperty3;
	}

	public void setCharProperty3(char charProperty3) {
		this.charProperty3 = charProperty3;
	}

	public char getCharProperty4() {
		return charProperty4;
	}

	public void setCharProperty4(char charProperty4) {
		this.charProperty4 = charProperty4;
	}

	public boolean isBooleanProperty1() {
		return booleanProperty1;
	}

	public void setBooleanProperty1(boolean booleanProperty1) {
		this.booleanProperty1 = booleanProperty1;
	}

	public boolean isBooleanProperty2() {
		return booleanProperty2;
	}

	public void setBooleanProperty2(boolean booleanProperty2) {
		this.booleanProperty2 = booleanProperty2;
	}

	public short getShortProperty1() {
		return shortProperty1;
	}

	public void setShortProperty1(short shortProperty1) {
		this.shortProperty1 = shortProperty1;
	}

	public short getShortProperty2() {
		return shortProperty2;
	}

	public void setShortProperty2(short shortProperty2) {
		this.shortProperty2 = shortProperty2;
	}

	public short getShortProperty3() {
		return shortProperty3;
	}

	public void setShortProperty3(short shortProperty3) {
		this.shortProperty3 = shortProperty3;
	}

	public short getShortProperty4() {
		return shortProperty4;
	}

	public void setShortProperty4(short shortProperty4) {
		this.shortProperty4 = shortProperty4;
	}

	public int getIntProperty1() {
		return intProperty1;
	}

	public void setIntProperty1(int intProperty1) {
		this.intProperty1 = intProperty1;
	}

	public int getIntProperty2() {
		return intProperty2;
	}

	public void setIntProperty2(int intProperty2) {
		this.intProperty2 = intProperty2;
	}

	public int getIntProperty3() {
		return intProperty3;
	}

	public void setIntProperty3(int intProperty3) {
		this.intProperty3 = intProperty3;
	}

	public int getIntProperty4() {
		return intProperty4;
	}

	public void setIntProperty4(int intProperty4) {
		this.intProperty4 = intProperty4;
	}

	public long getLongProperty1() {
		return longProperty1;
	}

	public void setLongProperty1(long longProperty1) {
		this.longProperty1 = longProperty1;
	}

	public long getLongProperty2() {
		return longProperty2;
	}

	public void setLongProperty2(long longProperty2) {
		this.longProperty2 = longProperty2;
	}

	public long getLongProperty3() {
		return longProperty3;
	}

	public void setLongProperty3(long longProperty3) {
		this.longProperty3 = longProperty3;
	}

	public long getLongProperty4() {
		return longProperty4;
	}

	public void setLongProperty4(long longProperty4) {
		this.longProperty4 = longProperty4;
	}

	public float getFloatProperty1() {
		return floatProperty1;
	}

	public void setFloatProperty1(float floatProperty1) {
		this.floatProperty1 = floatProperty1;
	}

	public float getFloatProperty2() {
		return floatProperty2;
	}

	public void setFloatProperty2(float floatProperty2) {
		this.floatProperty2 = floatProperty2;
	}

	public float getFloatProperty3() {
		return floatProperty3;
	}

	public void setFloatProperty3(float floatProperty3) {
		this.floatProperty3 = floatProperty3;
	}

	public float getFloatProperty4() {
		return floatProperty4;
	}

	public void setFloatProperty4(float floatProperty4) {
		this.floatProperty4 = floatProperty4;
	}

	public double getDoubleProperty1() {
		return doubleProperty1;
	}

	public void setDoubleProperty1(double doubleProperty1) {
		this.doubleProperty1 = doubleProperty1;
	}

	public double getDoubleProperty2() {
		return doubleProperty2;
	}

	public void setDoubleProperty2(double doubleProperty2) {
		this.doubleProperty2 = doubleProperty2;
	}

	public double getDoubleProperty3() {
		return doubleProperty3;
	}

	public void setDoubleProperty3(double doubleProperty3) {
		this.doubleProperty3 = doubleProperty3;
	}

	public double getDoubleProperty4() {
		return doubleProperty4;
	}

	public void setDoubleProperty4(double doubleProperty4) {
		this.doubleProperty4 = doubleProperty4;
	}

	@Merge(propertyProtection = PropertyProtection.NEVER_MERGE)
	public String getMethodAnnotationProperty() {
		return methodAnnotationProperty;
	}

	public void setMethodAnnotationProperty(String methodAnnotationProperty) {
		this.methodAnnotationProperty = methodAnnotationProperty;
	}

	public String getNoAnnotationProperty1() {
		return noAnnotationProperty1;
	}

	public void setNoAnnotationProperty1(String noAnnotationProperty1) {
		this.noAnnotationProperty1 = noAnnotationProperty1;
	}

	public String getNoAnnotationProperty2() {
		return noAnnotationProperty2;
	}

	public void setNoAnnotationProperty2(String noAnnotationProperty2) {
		this.noAnnotationProperty2 = noAnnotationProperty2;
	}
}
