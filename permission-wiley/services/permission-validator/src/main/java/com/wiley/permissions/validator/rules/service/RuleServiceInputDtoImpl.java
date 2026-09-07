package com.wiley.permissions.validator.rules.service;

import java.util.List;
import java.util.Map;

import com.wiley.permissions.validator.rules.base.Operation;

/**
 * The Class RuleServiceInputDtoImpl.
 * 
 * @author Sreenath
 * @version 1.1 Created on Jun 10, 2008 at 1:55:49 PM
 */
public class RuleServiceInputDtoImpl implements RuleServiceInputDto {

	/** The facts. */
	private List<Object> facts;
	
	/** The globals. */
	private Map<String, Object> globals;
	
	/** The operation. */
	private Operation operation;
	
	/**
	 * Instantiates a new rule service input dto impl.
	 */
	public RuleServiceInputDtoImpl() {
		super();
	}
	
	
	/**
	 * Instantiates a new rule service input dto impl.
	 * 
	 * @param facts the facts
	 * @param globals the globals
	 * @param operation the operation
	 */
	public RuleServiceInputDtoImpl(List<Object> facts,
			Map<String, Object> globals, Operation operation) {
		super();
		this.facts = facts;
		this.globals = globals;
		this.operation = operation;
	}


	/* (non-Javadoc)
	 * @see com.wiley.permissions.validator.rule.service.RuleServiceInputDto#getFacts()
	 */
	public List<Object> getFacts() {
		return facts;
	}

	/* (non-Javadoc)
	 * @see com.wiley.permissions.validator.rule.service.RuleServiceInputDto#setFacts(java.util.List)
	 */
	public void setFacts(List<Object> facts) {
		this.facts = facts;
	}

	/* (non-Javadoc)
	 * @see com.wiley.permissions.validator.rule.service.RuleServiceInputDto#getGlobals()
	 */
	public Map<String, Object> getGlobals() {
		return globals;
	}

	/* (non-Javadoc)
	 * @see com.wiley.permissions.validator.rule.service.RuleServiceInputDto#setGlobals(java.util.Map)
	 */
	public void setGlobals(Map<String, Object> globals) {
		this.globals = globals;
	}

	/* (non-Javadoc)
	 * @see com.wiley.permissions.validator.rule.service.RuleServiceInputDto#getOperation()
	 */
	public Operation getOperation() {
		return operation;
	}

	/* (non-Javadoc)
	 * @see com.wiley.permissions.validator.rule.service.RuleServiceInputDto#setOperation(com.wiley.permissions.validator.rules.base.Operation)
	 */
	public void setOperation(Operation operation) {
		this.operation = operation;
	}

}
