/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.wiley.permissions.repositories;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.permissions.common.bean.BeanProperty;
import com.wiley.permissions.common.bean.BeanUtility;

/**
 *
 * @author ttidwell
 */
public class ReferenceDataCache
{
	private final static Log log = LogFactory.getLog(ReferenceDataCache.class);

	private Map<String, ReferenceDataEntry> cacheByName = new HashMap<String, ReferenceDataEntry>();

	private Map<Class<?>, ReferenceDataPropertyEditorSupport> binders
	    = new HashMap<Class<?>, ReferenceDataPropertyEditorSupport>();

	private EnumDataRepository enumDataRepository = null;

	private final static Object[] BLANK_ARGS = new Object[] { };

	public ReferenceDataCache() {

	}

	public List<Object> get(String name)
	throws Exception
	{
		List<Object> output = new ArrayList<Object>();

		ReferenceDataEntry entry = cacheByName.get(name);

		if (entry != null) {
			if (!entry.isValid()) {
				load(entry);
			}

			output = entry.getObjects();
		}

		return output;
	}

	public Map<String, List<Object>> get(List<String> names)
	throws Exception
	{
		Map<String, List<Object>> map = new HashMap<String, List<Object>>();

		for (String name : names) {
			map.put(name, get(name));
		}

		return map;
	}

	public void setEnumDataTypes(Map<String, String> metaDataMap)
	throws Exception
	{
		for (Entry<String, String> entry : metaDataMap.entrySet()) {
			String name=entry.getKey();
			String typeStr = entry.getValue();
			Properties discriminators = new Properties();
			String firstSplit[] = typeStr.split("\\?");
			String classStr = firstSplit[0];
			Class<?> type = Thread.currentThread().getContextClassLoader().loadClass(classStr);

			if (firstSplit.length > 1) {
				String propString = firstSplit[1];
				StringTokenizer tokens = new StringTokenizer(propString, "&");

				while(tokens.hasMoreTokens()) {
					String pair = tokens.nextToken();
					String secondSplit[] = pair.split("=");
					String propName = secondSplit[0];

					if (secondSplit.length > 1) {
						String propValue = secondSplit[1];

						if (StringUtils.isNotBlank(propValue)) {
							propValue = URLDecoder.decode(propValue, "UTF-8");
							discriminators.setProperty(propName, propValue);
						}
					}
				}
			}

			try {
				ReferenceDataEntry newEntry = new ReferenceDataEntry();
				newEntry.setName(name);
				newEntry.setType(type);
				newEntry.setDiscriminators(discriminators);
				newEntry.setValid(false);
				cacheByName.put(name, newEntry);
			}
			catch (Exception e) {
				log.error("Error Loading MetaData For: " + name + " -- " + typeStr, e);
			}
		}
	}

	public void setBinderPropertyNames(Map<Class<?>, String> names)
	{
		for (Entry<Class<?>, String> entry : names.entrySet()) {
			Class<?> type = entry.getKey();

			String propertyName = entry.getValue();

			ReferenceDataPropertyEditorSupport binder = new ReferenceDataPropertyEditorSupport();

			binder.setPropertyName(propertyName);
			binder.setReferenceDataCache(this);
			binder.setType(type);

			binders.put(type, binder);
		}
	}

	public List<ReferenceDataEntry> getEntriesByType(Class<?> type)
	throws Exception
	{
		List<ReferenceDataEntry> output = new ArrayList<ReferenceDataEntry>();

		for (ReferenceDataEntry entry : cacheByName.values()) {
			if (entry.getType().equals(type)) {
				if (!entry.isValid()) {
					load(entry);
				}

				output.add(entry);
			}
		}

		return output;
	}

	public Object getObjectByType(Class<?> type, String text)
	throws Exception
	{
		List<ReferenceDataEntry> entries = getEntriesByType(type);  // throws Exception

		Object finalValue = null;

		if (entries.size() == 0) {
			throw new IllegalArgumentException("No Reference Data Found Of Type: " + type);
		}

		ReferenceDataPropertyEditorSupport binder = getBinderByType (type);

		if (binder == null) {
			throw new IllegalArgumentException("No Binder Found Of Type: " + type);
		}

		for (int x = 0; x < entries.size() && finalValue == null; x++) {
			List<Object> cachedList = entries.get(x).getObjects();

			for (Object entry : cachedList) {
				if (matches(binder, entry, text)) {
					finalValue = entry;
					break;
				}
			}
		}

		return (finalValue);
	}

	private boolean matches(ReferenceDataPropertyEditorSupport binder, Object object, String text)
	{
		boolean output = false;

		if (object != null) {
			BeanProperty prop = BeanUtility.getPropertyFromClass(binder.getType(), binder.getPropertyName());

			if (prop != null) {
				Method readMethod = prop.getReadMethod();

				try {
					Object value = readMethod.invoke(object, BLANK_ARGS);

					if (value != null) {
						if (String.valueOf(value).equals(text)) {
							output = true;
						}
					}
				}
				catch (IllegalAccessException e) {
					throw new IllegalArgumentException("Could Not Retrieve Property "
				        + binder.getPropertyName() + " from " + binder.getType().getName(), e);
				}
				catch (InvocationTargetException e) {
					throw new IllegalArgumentException("Could Not Retrieve Property "
						+ binder.getPropertyName() + " from " + binder.getType().getName(), e);
				}
			}
			else {
				throw new IllegalArgumentException(binder.getType().getName()
					+ " has no property named " + binder.getPropertyName());
			}
		}

		return output;
	}

	public void reload(Class<?> type)
	throws Exception
	{
		for (ReferenceDataEntry entry : cacheByName.values()) {
			if (entry.getType().equals(type)) {
				load(entry);
			}
		}
	}

	public void reload(String name)
	throws Exception
	{
		ReferenceDataEntry entry = cacheByName.get(name);

		if (entry != null) {
			load(entry);
		}
	}

	public void invalidate(Class<?> type)
	throws Exception
	{
		for (ReferenceDataEntry entry : cacheByName.values()) {
			if (entry.getType().equals(type)) {
				entry.setValid(false);
			}
		}
	}

	public void invalidate(String name)
	throws Exception
	{
		ReferenceDataEntry entry = cacheByName.get(name);

		if (entry != null) {
			entry.setValid(false);
		}
	}

	private void load(ReferenceDataEntry entry)
	throws Exception
	{
		List<Object> objects = enumDataRepository.getEnumDataByType(entry.getType(), entry.getDiscriminators());
		entry.setObjects(objects);
		entry.setValid(true);
	}

	public Map<String, ReferenceDataEntry> getCacheByName()
	{
		return cacheByName;
	}

	public void setCacheByName(Map<String, ReferenceDataEntry> cacheByName)
	{
		this.cacheByName = cacheByName;
	}

	public Map<Class<?>, ReferenceDataPropertyEditorSupport> getBinders()
	{
		return binders;
	}

	public ReferenceDataPropertyEditorSupport getBinderByType (Class<?> type) {
		Map<Class<?>, ReferenceDataPropertyEditorSupport> cacheBinders = getBinders();

		if (cacheBinders != null) {
			for (Entry<Class<?>, ReferenceDataPropertyEditorSupport> entry : cacheBinders.entrySet()) {
				Class<?> binderType = entry.getKey();

				if (binderType.equals(type)) {
					ReferenceDataPropertyEditorSupport tmpBinder = entry.getValue();
					return  tmpBinder;
				}
			}
		}
		return null;
	}

	public void setBinders(Map<Class<?>, ReferenceDataPropertyEditorSupport> binders) {
		this.binders = binders;
	}

	public EnumDataRepository getEnumDataRepository() {
		return enumDataRepository;
	}

	public void setEnumDataRepository(EnumDataRepository enumDataRepository) {
		this.enumDataRepository = enumDataRepository;
	}
}
