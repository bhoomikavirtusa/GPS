package com.wiley.permissions.common.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author ttidwell
 */
public class InstanceMap<K, O>
implements Map<K, O>
{
	private final List<InstanceMapEntry<K,O>> entries = new ArrayList<InstanceMapEntry<K, O>>();

	public void clear()
	{
		entries.clear();
	}

	public boolean containsKey(Object key)
	{
		boolean output = false;

		for (InstanceMapEntry<K, O> entry : entries)
		{
			if (entry.getValue() == key)
			{
				output = true;

				break;
			}
		}

		return output;
	}

	public boolean containsValue(Object value)
	{
		boolean output = false;

		for (InstanceMapEntry<K, O> entry : entries)
		{
			if (value != null && value.equals(entry.getValue()))
			{
				output = true;

				break;
			}
		}

		return output;
	}

	public Set<Entry<K, O>> entrySet()
	{
		Set<Entry<K, O>> output = new InstanceOnlySet<Entry<K, O>>();

		output.addAll(entries);

		return output;
	}

	public O get(Object key)
	{
		O output = null;

		for (InstanceMapEntry<K, O> entry : entries)
		{
			if (entry.getKey() == key)
			{
				output=entry.getValue();

				break;
			}
		}

		return output;
	}

	public boolean isEmpty()
	{
		return entries.isEmpty();
	}

	public Set<K> keySet()
	{
		Set<K> output = new InstanceOnlySet<K>();

		for (InstanceMapEntry<K, O> entry : entries)
		{
			output.add(entry.getKey());
		}

		return output;
	}

	public O put(K key, O value)
	{
		O output = null;

		if (key != null && value != null)
		{
			for (InstanceMapEntry<K, O> entry : entries)
			{
				if (entry.getKey() == key)
				{
					output = entry.setValue(value);

					break;
				}
			}

			if (output == null)
			{
				InstanceMapEntry<K, O> entry = new InstanceMapEntry<K, O>();

				entry.setKey(key);
				entry.setValue(value);

				entries.add(entry);
			}
		}

		return output;
	}

	public void putAll(Map<? extends K, ? extends O> m)
	{
		for (K key : m.keySet())
		{
			put(key, m.get(key));
		}
	}

	public O remove(Object key)
	{
		O output = null;

		for (int x=0; x < entries.size() && output == null; x++)
		{
			InstanceMapEntry<K, O> entry = entries.get(x);

			if (key == entry.getKey())
			{
				output = entry.getValue();
			}
		}

		return output;
	}

	public int size()
	{
		return entries.size();
	}

	public Collection<O> values()
	{
		List<O> output = new ArrayList<O>();

		for (InstanceMapEntry<K,O> entry : entries)
		{
			output.add(entry.getValue());
		}

		return output;
	}
}
