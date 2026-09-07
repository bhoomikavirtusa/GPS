package com.wiley.permissions.validator.rules.base;

import java.util.Collection;
import java.util.Map;

/**
 *
 * @author sputta
 * @since jdk 1.5
 *
 * This is the Entry point to the Jboss Drools (RULE ENGINE) Framework.
 */
public interface RuleBaseFacade {

    /**
     * This is method which will hand over your Globals and Facts to the
     * Rules Engine to execute matching Rules.
     * @param globals
     * @param facts
     */
    public void execute(final Map<String, Object> globals, final Collection<Object> facts);

}
