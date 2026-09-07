package com.wiley.permissions.common.utils;

import java.util.Map.Entry;

/**
 *
 * @author ttidwell
 * @version 1.0
 */
public class InstanceMapEntry<K, O>
implements Entry<K, O>
{
	private K key = null;
	
	private O value = null;
	
	public InstanceMapEntry()
	{
		
	}
	
	public O getValue()
	{
		return value;
	}

	public O setValue(O value)
	{
		O output = this.value;
		
		this.value=value;
		
		return output;
	}

	public K getKey()
	{
		return key;
	}

	public void setKey(K key)
	{
		this.key = key;
	}
}
