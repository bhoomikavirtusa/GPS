package com.wiley.permissions.validator.rules.base;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang3.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieRepository;
import org.kie.api.builder.Message;
import org.kie.api.builder.ReleaseId;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.StatelessKieSession;
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
 * Ported from the Drools 4 RuleBase/PackageBuilder API to the KIE API (Phase 8, Drools ${drools.version}).
 */
public class RuleBaseFacadeImpl implements RuleBaseFacade, InitializingBean {

	private static final Log log = LogFactory.getLog(RuleBaseFacadeImpl.class);

	Resource[] resources = null;

	private KieBase kieBase;

	/**
	 * Gets the kie base.
	 *
	 * @return the kie base
	 */
	public KieBase getKieBase() {
		return kieBase;
	}

	/**
	 * Sets the kie base.
	 *
	 * @param kieBase the new kie base
	 */
	public void setKieBase(KieBase kieBase) {
		this.kieBase = kieBase;
	}

	/* (non-Javadoc)
	 * @see com.wiley.permissions.validator.rules.base.RuleBaseFacade#execute(java.util.Map, java.util.Collection)
	 */
	public void execute(Map<String, Object> globals, Collection<Object> facts) {
		log.debug("About to call RuleBase Execute method");
		StatelessKieSession workingSession = setupWorkingSession();
		setupGlobals(globals, workingSession);
		workingSession.execute(facts);

	}

	/* (non-Javadoc)
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws BeanCreationException {

		log.debug("Initializing the Rules Loader ");
		Validate.notNull(getResources(), "Resources are not null");

		log.debug("Creating a KieBase:");
		KieServices kieServices = KieServices.Factory.get();
		KieFileSystem kieFileSystem = kieServices.newKieFileSystem();
		try {
			int index = 0;
			for (Resource resource : getResources()) {
				// KieFileSystem needs a unique virtual path per .drl, the actual classpath location doesn't matter
				String path = "src/main/resources/rules/generated" + (index++) + ".drl";
				kieFileSystem.write(path, kieServices.getResources()
						.newInputStreamResource(resource.getInputStream())
						.setResourceType(ResourceType.DRL));
				log.debug("Adding rule package from: " + resource.getFilename());
			}

			KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
			kieBuilder.buildAll();
			if (kieBuilder.getResults().hasMessages(Message.Level.ERROR)) {
				throw new BeanCreationException(
						"Unable to Parse Drl files, Please look into the files: "
								+ kieBuilder.getResults().getMessages(Message.Level.ERROR));
			}

			KieRepository kieRepository = kieServices.getRepository();
			ReleaseId releaseId = kieRepository.getDefaultReleaseId();
			KieContainer kieContainer = kieServices.newKieContainer(releaseId);

			setKieBase(kieContainer.getKieBase());
		} catch (IOException ioe) {
			log.error(ioe);
			throw new BeanCreationException(
					"Unable to Read Drl files from the file System", ioe);
		} catch (BeanCreationException bce) {
			throw bce;
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
	public StatelessKieSession setupWorkingSession() {
		log.debug("Creating the Working memory");
		StatelessKieSession workingSession = getKieBase().newStatelessKieSession();
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
			StatelessKieSession workingSession) {
		if (globals != null) {
			for (Map.Entry<String, Object> entry : globals.entrySet()) {
				workingSession.setGlobal(entry.getKey(), entry.getValue());
			}
		}
	}

}

