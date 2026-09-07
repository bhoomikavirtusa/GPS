package com.wiley.permissions.validator.rules.service;

import java.util.List;
import java.util.Map;

import com.wiley.permissions.validator.rules.base.Operation;


/**
 * The Interface RuleServiceInputDto.
 *	@author Sreenath 
 *	@version 1.1 Created on Jun 10, 2008 at 1:49:45 PM
 */
public interface RuleServiceInputDto {
	
	/**
	 * Gets the operation.
	 * @return the operation
	 */
	public Operation getOperation();
	
	/**
	 * Sets the operation.
	 * @param operation the new operation
	 */
	public void setOperation(Operation operation );
	
	/**
	 * Gets the facts.
	 * @return the facts
	 */
	public List<Object> getFacts();
	
	/**
	 * Sets the facts.
	 * @param facts the new facts
	 */
	public void setFacts(List<Object> facts);
	
	/**
	 * Gets the globals.
	 * @return the globals
	 */
	public Map<String, Object> getGlobals();
	
	/**
	 * Sets the globals.
	 * @param globals the globals
	 */
	public void setGlobals(Map<String,Object> globals);

}
