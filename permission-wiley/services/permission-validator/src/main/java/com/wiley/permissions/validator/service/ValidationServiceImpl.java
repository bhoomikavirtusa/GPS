package com.wiley.permissions.validator.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.permissions.validator.rules.base.Operation;
import com.wiley.permissions.validator.rules.base.RuleBaseFacade;
import com.wiley.permissions.validator.rules.exception.ValidationException;

/**
 * The Class ValidationServiceImpl.
 * @author Sreenath
 * @version 1.1 Created on Jun 3, 2008 at 9:42:37 AM
 */
public  class ValidationServiceImpl implements ValidationService {

	private static final Log log = LogFactory.getLog(ValidationServiceImpl.class);

	private RuleBaseFacade ruleBaseFacade = null;

	/**
	 * Implements ValidationService interface.
	 */
	public List<Object> validate(Operation operation, Collection<Object> facts)
			throws ValidationException {

		if (operation == null) {
			throw new ValidationException("No Operation is Specified");
		}
		if (facts == null) {
			throw new ValidationException(
					"Cannot Validate the rules without any facts");
		}
		List<Object> commands = new ArrayList<Object>();
		Map<String, Object> globals = new HashMap<String, Object>();
		globals.put("commands", commands);
		globals.put("ruleLog", log);
		//facts.add(operation);
		try {
			getRuleBaseFacade().execute(globals, facts);
		} catch (Exception e) {
			throw new ValidationException(e);
		}
		return commands;

	}

	public RuleBaseFacade getRuleBaseFacade() {
		return ruleBaseFacade;
	}

	public void setRuleBaseFacade(RuleBaseFacade ruleBaseFacade) {
		this.ruleBaseFacade = ruleBaseFacade;
	}
}
