package com.wiley.permissions.domain;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A MaterializationKey is used on a property to denote that it
 * can be used to find the object in the database.  Keys come in
 * two varieties: UNIQUE and ADDITIVE. 
 * 
 * Unique keys can be used uniquely to identify the object.  Additive
 * keys, on the otherhand, have to work in conjunction with other keys.
 * 
 * Additive keys has a nullable property.  If an additive key is marked
 * as nullable, then it is not required to be present to do the additive
 * search.  However, if it is not nullable, then it must be present before
 * an additive search will be performed.
 * 
 * @author ttidwell
 */
@Target(
	{
		ElementType.FIELD,
		ElementType.METHOD
	}
)
@Retention(RetentionPolicy.RUNTIME)
public @interface MaterializationKey
{
	public enum Mode
	{
		ADDITIVE,
		UNIQUE
	}
	
	Mode mode() default Mode.UNIQUE;

	boolean nullable() default false;

	boolean ignoreCase() default false;

	boolean alwaysTrim() default false;
}
