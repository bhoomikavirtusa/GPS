package com.wiley.permissions.validator.rules.service;

/**
 * The Class RuleServiceOutputDtoImpl.
 *
 * @author Sreenath
 * @version 1.1 Created on Jun 10, 2008 at 1:55:38 PM
 */
public class RuleServiceOutputDtoImpl implements RuleServiceOutputDto {

	private ValidationResult result;

	/**
	 * Instantiates a new rule service output dto impl.
	 */
	public RuleServiceOutputDtoImpl() {

	}

	/**
	 * Instantiates a new rule service output dto impl.
	 *
	 * @param result the result
	 */
	public RuleServiceOutputDtoImpl(ValidationResult result) {
		this.result = result;
	}


	/**
	 * Implements RuleServiceOutputDto.
	 */
	public ValidationResult getResult() {
		return result;
	}

	/**
	 * Implements RuleServiceOutputDto.
	 */
	public void setResult(ValidationResult result) {
		this.result = result;
	}

}
