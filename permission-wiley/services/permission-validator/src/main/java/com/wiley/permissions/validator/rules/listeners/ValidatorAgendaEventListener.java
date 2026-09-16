package com.wiley.permissions.validator.rules.listeners;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.AgendaEventListener;
import org.kie.api.event.rule.AgendaGroupPoppedEvent;
import org.kie.api.event.rule.AgendaGroupPushedEvent;
import org.kie.api.event.rule.BeforeMatchFiredEvent;
import org.kie.api.event.rule.MatchCancelledEvent;
import org.kie.api.event.rule.MatchCreatedEvent;
import org.kie.api.event.rule.RuleFlowGroupActivatedEvent;
import org.kie.api.event.rule.RuleFlowGroupDeactivatedEvent;

/**
 * 
 * @author sputta
 * Ported from org.drools.event.AgendaEventListener (Drools 4) to org.kie.api.event.rule.AgendaEventListener (Phase 8).
 */
public class ValidatorAgendaEventListener implements AgendaEventListener{

    private static final Log log = LogFactory.getLog(ValidatorAgendaEventListener.class);

    /**
     * Default Constructor
     */
    public ValidatorAgendaEventListener() {
	    super();
    }

    public void matchCancelled(MatchCancelledEvent event) {
	    log.debug("Activation Cancelled for: " + event.getMatch().getRule().getName());
    }

    public void matchCreated(MatchCreatedEvent event) {
	    log.debug("Activation Created for: " + event.getMatch().getRule().getName());
    }

    public void afterMatchFired(AfterMatchFiredEvent event) {
	    //log.debug("Activation Fired for: " + event.getMatch().getRule().getName());
    }

    public void agendaGroupPopped(AgendaGroupPoppedEvent event) {
	    log.debug("Agenda Group Popped for: " + event.getAgendaGroup().getName());
    }

    public void agendaGroupPushed(AgendaGroupPushedEvent event) {
	    log.debug("Agenda Group Pushed for:" + event.getAgendaGroup().getName());
    }

    public void beforeMatchFired(BeforeMatchFiredEvent event) {
	    //log.debug("Before Activation Fired for: " + event.getMatch().getRule().getName() );
    }

    public void beforeRuleFlowGroupActivated(RuleFlowGroupActivatedEvent event) {
    }

    public void afterRuleFlowGroupActivated(RuleFlowGroupActivatedEvent event) {
    }

    public void beforeRuleFlowGroupDeactivated(RuleFlowGroupDeactivatedEvent event) {
    }

    public void afterRuleFlowGroupDeactivated(RuleFlowGroupDeactivatedEvent event) {
    }
}

