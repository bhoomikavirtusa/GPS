package com.wiley.permissions.web.internal.controllers.myaccount;

import java.beans.PropertyEditorSupport;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.permissions.domain.persistence.permissions.ConditionType;

public class ConditionTypePropertyEditor extends PropertyEditorSupport {
    private static final Log log = LogFactory.getLog(ConditionTypePropertyEditor.class);

    @Override
    public String getAsText() {
    	ConditionType type = (ConditionType) getValue();
    	if (type == null)  return null;
    	else {
			log.debug("Returning the code " + type.getCode()
					+ " for condition type");
			return type.getCode();
		}
	}

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
		if (StringUtils.isNotBlank(text)) {
			log.debug("Creating ConditionType for the code: " + text);
			ConditionType conditionType = new ConditionType();
			conditionType.setCode(text);
			setValue(conditionType);
		}
	}
}
