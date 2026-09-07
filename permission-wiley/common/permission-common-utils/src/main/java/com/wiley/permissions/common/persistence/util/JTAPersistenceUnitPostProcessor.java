package com.wiley.permissions.common.persistence.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.orm.jpa.persistenceunit.MutablePersistenceUnitInfo;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitPostProcessor;

/**
 * This just makes sure that things get the right DataSources.  Because we don't use
 * JNDI and instead use Spring injection, the JPA stuff doesn't find a JTA datasource,
 * instead it finds just a regular datasource.  This seems to work fine, but this class
 * makes sure that we don't take the chance.
 *
 * @author ttidwell
 */
public class JTAPersistenceUnitPostProcessor
implements PersistenceUnitPostProcessor
{
	private final static Log log = LogFactory.getLog(JTAPersistenceUnitPostProcessor.class);

	public void postProcessPersistenceUnitInfo(MutablePersistenceUnitInfo pu)
	{
		if (pu.getNonJtaDataSource() != null)
		{
			pu.setJtaDataSource(pu.getNonJtaDataSource());

			log.debug("JTA Data Source For " + pu.getPersistenceUnitName() + " set successfully.");
		}
		else
		{
			log.warn("JTA Data Source For " + pu.getPersistenceUnitName() + " not set.");
		}
	}
}
