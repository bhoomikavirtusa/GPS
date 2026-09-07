package com.wiley.permissions.repositories;

import java.util.Date;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.wiley.permissions.domain.persistence.permissions.DataSource;
import com.wiley.permissions.persistence.repository.JPARepository;
import com.wiley.permissions.services.CommonWorkService;

public class DevMiscRepository extends JPARepository {
	private static final Log log = LogFactory.getLog(DevMiscRepository.class);

	@PersistenceContext(unitName = "permissions")
	protected EntityManager entityManager;

	@Override
	public EntityManager getEntityManager() {
		return entityManager;
	}

	private CommonWorkService cwService;


	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void testTransaction34(String objectType) {
		cwService.setLastUpdate(new Date(), objectType, DataSource.US);
		log.debug("transactionTest34(): updated " + objectType);
	}

	public void testTransaction5(String objectType) {
		cwService.setLastUpdate(new Date(), objectType, DataSource.US);
		log.debug("transactionTest5(): updated " + objectType);
	}

	public CommonWorkService getCommonWorkService() {
		return cwService;
	}

	public void setCommonWorkService(CommonWorkService cwService) {
		this.cwService = cwService;
	}
}
