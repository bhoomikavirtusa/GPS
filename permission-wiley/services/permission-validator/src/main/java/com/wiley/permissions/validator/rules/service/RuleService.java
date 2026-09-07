package com.wiley.permissions.validator.rules.service;

import com.wiley.permissions.validator.rules.exception.ValidationException;

/**
 * The Interface RuleService.
 *
 * @author Sreenath
 * @version 1.1 Created on Jun 10, 2008 at 1:56:16 PM
 */
public interface RuleService {

	/**
	 * Execute.
	 *
	 * @param input the input
	 *
	 * @return the rule service output
	 *
	 * @throws ValidationException the validation exception
	 */
	public RuleServiceOutputDto execute (RuleServiceInputDto input) throws ValidationException;

}
