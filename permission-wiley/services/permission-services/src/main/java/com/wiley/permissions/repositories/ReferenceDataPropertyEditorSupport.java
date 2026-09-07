package com.wiley.permissions.repositories;

import java.beans.PropertyEditorSupport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 *
 * @author ttidwell
 */
public class ReferenceDataPropertyEditorSupport
extends PropertyEditorSupport
{
	@SuppressWarnings("unused")
	private final static Log log = LogFactory.getLog(ReferenceDataPropertyEditorSupport.class);

	private ReferenceDataCache referenceDataCache = null;
	private Class<?> type = null;
	private String propertyName = null;


	@Override
	public void setAsText(String text) throws IllegalArgumentException
	{
		// PropertyEditorSupport.setAsText() doesn't throw Exception so re-type as IllegalArgumentException
		try {
			Object finalValue = referenceDataCache.getObjectByType(type, text); // throws Exception
			setValue(finalValue);
		}
		catch (Exception ex) {
			throw new IllegalArgumentException(ex);
		}
	}

	public ReferenceDataCache getReferenceDataCache()
	{
		return referenceDataCache;
	}

	public void setReferenceDataCache(ReferenceDataCache referenceDataCache)
	{
		this.referenceDataCache = referenceDataCache;
	}

	public Class<?> getType()
	{
		return type;
	}

	public void setType(Class<?> type)
	{
		this.type = type;
	}

	public String getPropertyName()
	{
		return propertyName;
	}

	public void setPropertyName(String propertyName)
	{
		this.propertyName = propertyName;
	}
}
