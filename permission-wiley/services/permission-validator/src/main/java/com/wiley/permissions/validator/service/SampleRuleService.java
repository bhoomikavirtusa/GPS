package com.wiley.permissions.validator.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.permissions.domain.persistence.permissions.Asset;
import com.wiley.permissions.validator.rules.base.Operation;
import com.wiley.permissions.validator.rules.base.RuleBaseFacade;

public class SampleRuleService {

    private static final Log log = LogFactory.getLog(SampleRuleService.class);

    public static final String ASSET_USE = "AssetUse";

    private RuleBaseFacade ruleBaseFacade = null;

	public RuleBaseFacade getRuleBaseFacade() {
		return ruleBaseFacade;
	}

	public void setRuleBaseFacade(RuleBaseFacade ruleBaseFacade) {
		this.ruleBaseFacade = ruleBaseFacade;
	}

	public Asset validateAssetWithAgendaGroup(Asset asset) {
		Map<String, Object> globals = new HashMap<String, Object>();
		globals.put("ruleLog", log);
		List<Object> facts = new ArrayList<Object>();
		facts.add(new Operation("permission-status"));
		facts.add(asset);
		getRuleBaseFacade().execute(globals, facts);
		//log.debug(asset);
		return asset;
	}

	public Asset validateAssetWithoutAgendaGroup(Asset asset) {
		Map<String, Object> globals = new HashMap<String, Object>();
		globals.put("ruleLog", log);
		List<Object> facts = new ArrayList<Object>();
		//facts.add(new Operation(ASSET_USE));
		facts.add(asset);
		getRuleBaseFacade().execute(globals, facts);
		//log.debug(asset);
		return asset;
	}
}
