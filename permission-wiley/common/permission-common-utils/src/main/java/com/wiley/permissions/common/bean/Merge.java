package com.wiley.permissions.common.bean;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is to be used with the BeanMergeUtility to facilitate
 * the merging of two JavaBeans.
 * 
 * This annotation controls how each property of a bean is merged
 * using the BeanMergeUtility.  
 * 
 * There are three modes used for the mergeMode property:
 * <ul>
 *	 <li>NEVER_MERGE: This means this property should never be merged</li>
 *   <li>
 *      OVERWRITE_IF_NOT_DEFAULT: This means the target property will be
 *		overwritten if the source property is not a default value.
 *   </li>
 *   <li>
 *      OVERWRITE: Always overwrite the target value with the source, ignoring
 *      default values.
 *   </li>
 * </ul>
 * 
 * Default values are determined based on the various primitive defaults defined by the annotation.
 * These are set to -1 for all numeric types, 0 for byte and character types.  Boolean types will
 * always be merged.  For Object types, null is always considered the default value.
 * 
 * @author ttidwell
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Merge
{
	public enum PropertyProtection
	{
		DEFAULT,
		NEVER_MERGE,
		OVERWRITE,
		OVERWRITE_IF_NOT_DEFAULT
	}

	public enum CollectionHandling
	{
		DEFAULT,
		REPLACE,
		MERGE
	}

	PropertyProtection propertyProtection() default PropertyProtection.DEFAULT;

	CollectionHandling collectionHandling() default CollectionHandling.DEFAULT;

	short shortDefault() default BeanUtility.DEFAULT_SHORT_VALUE;
	
	int integerDefault() default BeanUtility.DEFAULT_INT_VALUE;
	
	long longDefault() default BeanUtility.DEFAULT_LONG_VALUE;
	
	float floatDefault() default BeanUtility.DEFAULT_FLOAT_VALUE;
	
	double doubleDefault() default BeanUtility.DEFAULT_DOUBLE_VALUE;
	
	char charDefault() default BeanUtility.DEFAULT_CHAR_VALUE;
	
	byte byteDefault() default BeanUtility.DEFAULT_BYTE_VALUE;
}
