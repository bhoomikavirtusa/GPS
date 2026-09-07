package com.wiley.permissions.validator.rules.listeners;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.drools.WorkingMemory;
import org.drools.event.ActivationCancelledEvent;
import org.drools.event.ActivationCreatedEvent;
import org.drools.event.AfterActivationFiredEvent;
import org.drools.event.AgendaEventListener;
import org.drools.event.AgendaGroupPoppedEvent;
import org.drools.event.AgendaGroupPushedEvent;
import org.drools.event.BeforeActivationFiredEvent;

/**
 * 
 * @author sputta
 */
public class ValidatorAgendaEventListener implements AgendaEventListener{

    private static final Log log = LogFactory.getLog(ValidatorAgendaEventListener.class);

    /**
     * Default Constructor
     */
    public ValidatorAgendaEventListener() {
	    super();
    }

    public void activationCancelled(ActivationCancelledEvent event, WorkingMemory workingMemory) {
	    log.debug("Activation Cancelled for: " + event.getActivation().getRule().getName());
    }

    public void activationCreated(ActivationCreatedEvent event, WorkingMemory workingMemory) {
	    log.debug("Activation Created for: " + event.getActivation().getRule().getName());
    }

    public void afterActivationFired(AfterActivationFiredEvent event, WorkingMemory workingMemory) {
	    //log.debug("Activation Fired for: " + event.getActivation().getRule().getName());
    }

    public void agendaGroupPopped(AgendaGroupPoppedEvent event, WorkingMemory workingMemory) {
	    log.debug("Agenda Group Popped for: " + event.getAgendaGroup().getName());
    }

    public void agendaGroupPushed(AgendaGroupPushedEvent event, WorkingMemory workingMemory) {
	    log.debug("Agenda Group Pushed for:" + event.getAgendaGroup().getName());
    }

    public void beforeActivationFired(BeforeActivationFiredEvent event, WorkingMemory workingMemory) {
	    //log.debug("Before Activation Fired for: " + event.getActivation().getRule().getName() );
    }
}
