package com.wiley.permissions.web.shared.util;

import java.beans.PropertyEditorSupport;

import org.apache.commons.lang3.StringUtils;

import com.wiley.permissions.domain.persistence.permissions.Component;

/**
 *
 * @author smarkoff
 */
public class ComponentPropertyEditor extends PropertyEditorSupport {

    @Override
    public String getAsText() {
    	Component component = (Component) getValue();
        if (component == null)  return "";
        else return String.valueOf(component.getId());
	}

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
    	Component component = null;
    	if (StringUtils.isNotBlank(text)) {
    		// Is it ok just to set the id of the component?
    		component = new Component();
    		component.setId(Integer.parseInt(text));
    	}
    	setValue(component);
	}
}
