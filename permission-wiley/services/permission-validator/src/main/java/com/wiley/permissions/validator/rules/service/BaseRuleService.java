package com.wiley.permissions.validator.rules.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.permissions.validator.rules.base.Operation;
import com.wiley.permissions.validator.rules.base.RuleBaseFacade;
import com.wiley.permissions.validator.rules.exception.ValidationException;


/**
 * The Class AbstractRuleService.
 *
 * @author Sreenath
 * @version 1.1 Created on Jun 10, 2008 at 1:56:03 PM
 */
public  class BaseRuleService implements RuleService {

	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(BaseRuleService.class);

	private static final String RESULT = "validationResult";

	private RuleBaseFacade ruleBaseFacade = null;

	/**
	 * Implements RuleService interface.
	 */
	public RuleServiceOutputDto execute(RuleServiceInputDto input)
			throws ValidationException {
		Operation operation = input.getOperation();

		if (operation == null) {
			throw new ValidationException("No Operation Specified");
		}

		List<Object> facts = input.getFacts();
		if (facts != null) {
			facts.add(operation);
		} else {
			throw new ValidationException("No facts Inserted");
		}

		Map<String, Object> globals = input.getGlobals();
		if (globals == null) {
			globals = new HashMap<String, Object>();
		}

		ValidationResult validationResult = new ValidationResult();
		globals.put(RESULT, validationResult);

		ruleBaseFacade.execute(input.getGlobals(), input.getFacts());


		if (validationResult.getStatus() == null) {
			return null;
		}
		else {
			return new RuleServiceOutputDtoImpl(validationResult);
		}
	}

	public void setRuleBaseFacade(RuleBaseFacade ruleBaseFacade) {
		this.ruleBaseFacade = ruleBaseFacade;
	}
}
