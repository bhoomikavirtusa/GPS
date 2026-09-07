package com.wiley.permissions.repositories;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 *
 * @author ttidwell
 */
public class ReferenceDataEntry {

	private String name = null;
	private Class<?> type = null;
	private List<Object> objects = new ArrayList<Object>();
	private Properties discriminators = null;
	private boolean valid = false;


	public ReferenceDataEntry() {
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Class getType() {
		return type;
	}

	public void setType(Class<?> type) {
		this.type = type;
	}

	public List<Object> getObjects() {
		return objects;
	}

	public void setObjects(List<Object> objects) {
		this.objects = objects;
	}

	public Properties getDiscriminators() {
		return discriminators;
	}

	public void setDiscriminators(Properties discriminators) {
		this.discriminators = discriminators;
	}

	public boolean isValid() {
		return valid;
	}

	public void setValid(boolean valid) {
		this.valid = valid;
	}
}
