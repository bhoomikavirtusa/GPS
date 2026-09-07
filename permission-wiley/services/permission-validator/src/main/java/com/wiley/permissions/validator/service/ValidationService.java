package com.wiley.permissions.validator.service;

import java.util.Collection;
import java.util.List;

import com.wiley.permissions.validator.rules.base.Operation;
import com.wiley.permissions.validator.rules.exception.ValidationException;

/**
 * Validation Service
 * @author sputta
 * @version 1.1 Created on Jun 3, 2008 at 9:43:26 AM
 */
public interface ValidationService extends RuleService {

    /**
     * This is the method which will call RuleBase Facade to execute the rules
     */
    public List<Object> validate(Operation operation, Collection<Object> facts) throws ValidationException ;
}
