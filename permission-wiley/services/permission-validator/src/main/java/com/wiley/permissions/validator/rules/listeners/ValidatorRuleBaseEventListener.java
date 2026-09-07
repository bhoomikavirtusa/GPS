package com.wiley.permissions.validator.rules.listeners;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.drools.event.AfterFunctionRemovedEvent;
import org.drools.event.AfterPackageAddedEvent;
import org.drools.event.AfterPackageRemovedEvent;
import org.drools.event.AfterRuleAddedEvent;
import org.drools.event.AfterRuleBaseLockedEvent;
import org.drools.event.AfterRuleBaseUnlockedEvent;
import org.drools.event.AfterRuleRemovedEvent;
import org.drools.event.BeforeFunctionRemovedEvent;
import org.drools.event.BeforePackageAddedEvent;
import org.drools.event.BeforePackageRemovedEvent;
import org.drools.event.BeforeRuleAddedEvent;
import org.drools.event.BeforeRuleBaseLockedEvent;
import org.drools.event.BeforeRuleBaseUnlockedEvent;
import org.drools.event.BeforeRuleRemovedEvent;
import org.drools.event.RuleBaseEventListener;

/**
 * 
 * @author sputta
 */
public class ValidatorRuleBaseEventListener implements RuleBaseEventListener {
	
    private static final Log log = LogFactory.getLog(ValidatorRuleBaseEventListener.class); 
    
    public void afterFunctionRemoved(AfterFunctionRemovedEvent event) {
	    log.debug("After Function Removed Event fired for: " + event.getFunction());
    }

    public void afterPackageAdded(AfterPackageAddedEvent event) {
	    log.debug("After Package Added Event fired for: " + event.getPackage().getName());
    }

    public void afterPackageRemoved(AfterPackageRemovedEvent event) {
	    log.debug("After Package Removed Event fired for: " + event.getPackage().getName());
    }

    public void afterRuleAdded(AfterRuleAddedEvent event) {
	    log.debug("After Rule Added Event fired for: " + event.getRule().getName());
    }

    public void afterRuleBaseLocked(AfterRuleBaseLockedEvent event) {
	    log.debug("After RuleBase Locked Event fired for: " + event.getRuleBase().toString());
    }

    public void afterRuleBaseUnlocked(AfterRuleBaseUnlockedEvent event) {
	    log.debug("After RuleBase Unlocked Event fired for: " + event.getRuleBase().toString());
    }

    public void afterRuleRemoved(AfterRuleRemovedEvent event) {
	    log.debug("After Rule Removed Event fired for: " + event.getRule().getName());
    }

    public void beforeFunctionRemoved(BeforeFunctionRemovedEvent event) {
	    log.debug("Before Function Removed Event fired for: " + event.getFunction());
    }

    public void beforePackageAdded(BeforePackageAddedEvent event) {
	    log.debug("Before Package Added Event fired for: " + event.getPackage().getName());
    }

    public void beforePackageRemoved(BeforePackageRemovedEvent event) {
	    log.debug("Before Package Removed Event fired for: " + event.getPackage().getName());
    }

    public void beforeRuleAdded(BeforeRuleAddedEvent event) {
	    log.debug("Before Rule Added Event fired for: " + event.getRule().getName());
    }

    public void beforeRuleBaseLocked(BeforeRuleBaseLockedEvent event) {
	    log.debug("Before RuleBase Locked Event fired for: " + event.getRuleBase());
    }

    public void beforeRuleBaseUnlocked(BeforeRuleBaseUnlockedEvent event) {
	    log.debug("Before RuleBase Unlocked Event fired for : " + event.getRuleBase().toString());
    }

    public void beforeRuleRemoved(BeforeRuleRemovedEvent event) {
	    log.debug("Before Rule Removed Event fired for : " + event.getRule().getName());
    }
}
