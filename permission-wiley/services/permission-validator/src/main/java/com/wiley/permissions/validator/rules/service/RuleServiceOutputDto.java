package com.wiley.permissions.validator.rules.service;

/**
 * The Interface RuleServiceOutputDto.
 *
 * @author Sreenath
 * @version 1.1 Created on Jun 10, 2008 at 1:56:31 PM
 */
public interface RuleServiceOutputDto {

	/**
	 * Gets the results.
	 *
	 * @return the results
	 */
	public ValidationResult getResult();


	/**
	 * Sets the results.
	 *
	 * @param results the new results
	 */
	public void setResult(ValidationResult result);
}
