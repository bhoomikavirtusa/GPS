package com.wiley.permissions.validator.rules.base;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang3.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.drools.RuleBase;
import org.drools.RuleBaseFactory;
import org.drools.StatelessSession;
import org.drools.compiler.DroolsParserException;
import org.drools.compiler.PackageBuilder;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;

import com.wiley.permissions.validator.rules.listeners.ValidatorAgendaEventListener;

/**
 * The Class RuleBaseFacadeImpl.
 *
 * @author sputta
 * @since JDK 1.5
 * @version 1.1 Created on Jun 1, 2008 at 12:37:21 AM
 */
public class RuleBaseFacadeImpl implements RuleBaseFacade, InitializingBean {

	private static final Log log = LogFactory.getLog(RuleBaseFacadeImpl.class);

	Resource[] resources = null;

	private RuleBase ruleBase;

	/**
	 * Gets the rule base.
	 *
	 * @return the rule base
	 */
	public RuleBase getRuleBase() {
		return ruleBase;
	}

	/**
	 * Sets the rule base.
	 *
	 * @param ruleBase the new rule base
	 */
	public void setRuleBase(RuleBase ruleBase) {
		this.ruleBase = ruleBase;
	}

	/**
	 * Fire all rules.
	 */
	public void fireAllRules() {
		log.debug("Inside fireAllRules method");
		StatelessSession session = setupWorkingSession();
		session.execute(new Object());
	}


	/* (non-Javadoc)
	 * @see com.wiley.permissions.validator.rules.base.RuleBaseFacade#execute(java.util.Map, java.util.Collection)
	 */
	public void execute(Map<String, Object> globals, Collection<Object> facts) {
		log.debug("About to call RuleBase Execute method");
		StatelessSession workingSession = setupWorkingSession();
		setupGlobals(globals, workingSession);
		workingSession.execute(facts);

	}

	/* (non-Javadoc)
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws BeanCreationException {

		log.debug("Initializing the Rules Loader ");
		Validate.notNull(getResources(), "Resources are not null");

		log.debug("Creating a RuleBase:");
		final RuleBase ruleBase = RuleBaseFactory.newRuleBase(RuleBase.RETEOO);
		try {
			for (Resource resource : getResources()) {
				PackageBuilder builder = new PackageBuilder();
				builder.addPackageFromDrl(new InputStreamReader(resource
						.getInputStream()));
				log.debug("Adding The following rule Packages");
				ruleBase.addPackage(builder.getPackage());
			}

			setRuleBase(ruleBase);
		} catch (IOException ioe) {
			log.error(ioe);
			throw new BeanCreationException(
					"Unable to Read Drl files from the file System", ioe);
		} catch (DroolsParserException dpe) {
			log.error(dpe);
			throw new BeanCreationException(
					"Unable to Parse Drl files, Please look into the files ",
					dpe);
		} catch (Exception e) {
			log.error(e);
			throw new BeanCreationException(
					"Unable to create RuleBaseFacade..", e);
		}
	}

	/**
	 * Gets the resources.
	 *
	 * @return the resources
	 */
	public Resource[] getResources() {
		return resources;
	}

	/**
	 * Sets the resources.
	 *
	 * @param resources the resources
	 */
	public void setResources(Resource[] resources) {
		this.resources = resources;
	}

	/**
	 * Setup working session.
	 *
	 * @return the stateless session
	 */
	public StatelessSession setupWorkingSession() {
		log.debug("Creating the Working memory");
		StatelessSession workingSession = getRuleBase().newStatelessSession();
		workingSession.addEventListener(new ValidatorAgendaEventListener());

		return workingSession;
	}

	/**
	 * Setup globals.
	 *
	 * @param globals the globals
	 * @param workingSession the working session
	 */
	public void setupGlobals(Map<String, Object> globals,
			StatelessSession workingSession) {
		if (globals != null) {
			for (Map.Entry<String, Object> entry : globals.entrySet()) {
				workingSession.setGlobal(entry.getKey(), entry.getValue());
			}
		}
	}

}
