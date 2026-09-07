package com.wiley.permissions.common.bean;

import com.wiley.permissions.common.bean.BeanUtility.ObjectType;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * This class represents a discovered property from a class.  It gives us enough
 * information about the property to read it's annotations, etc.
 * 
 * @author ttidwell
 */
public class BeanProperty {
	
	private String name;
	private ObjectType propertyType = ObjectType.OBJECT;
	private Class<?> type;
	private Field field;
	private Method readMethod;
	private Method writeMethod;
	private Map<Class<?>, Annotation> annotations = new HashMap<Class<?>, Annotation>();

	public BeanProperty() {
	}

	public Type getGenericType() {
		if (field == null) {
			return readMethod.getGenericReturnType();
		}
		else {
			return field.getGenericType();
		}
	}

	public <T extends Annotation> T getAnnotation(Class<T> type) {
		return (T) annotations.get(type);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ObjectType getPropertyType() {
		return propertyType;
	}

	public void setPropertyType(ObjectType propertyType) {
		this.propertyType = propertyType;
	}

	public Class<?> getType() {
		return type;
	}

	public void setType(Class<?> type) {
		this.type = type;
	}

	public Field getField() {
		return field;
	}

	public void setField(Field field) {
		this.field = field;
	}

	public Method getReadMethod() {
		return readMethod;
	}

	public void setReadMethod(Method readMethod) {
		this.readMethod = readMethod;
	}

	public Method getWriteMethod() {
		return writeMethod;
	}

	public void setWriteMethod(Method writeMethod) {
		this.writeMethod = writeMethod;
	}

	public  Map<Class<?>, Annotation> getAnnotations() {
		return annotations;
	}

	public void setAnnotations(Map<Class<?>, Annotation> annotations) {
		this.annotations = annotations;
	}
}
