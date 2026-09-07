package com.wiley.permissions.validator.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.permissions.domain.persistence.permissions.Asset;

public class ProductValidationService extends ValidationServiceImpl {
	
    private static final Log log = LogFactory.getLog(ProductValidationService.class);


    public List<Asset> validate(List<Asset> assets) {
		Map<String, Object> globals = new HashMap<String, Object>();
		globals.put("ruleLog", log);
		List<Object> facts = new ArrayList<Object>();
		facts.add(assets);
		getRuleBaseFacade().execute(globals, facts);
		return assets;
	}

}
