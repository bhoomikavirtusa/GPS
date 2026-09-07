package com.wiley.permissions.services;


import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.wiley.permissions.domain.persistence.permissions.DataSource;
import com.wiley.permissions.repositories.DevMiscRepository;

/**
 *
 * @author smarkoff
 */
@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = Exception.class)
public class DevMiscServiceImpl extends BaseService implements DevMiscService {

	private static final Log log = LogFactory.getLog(DevMiscServiceImpl.class);

	private CommonWorkService cwService;
	private DevMiscRepository devMiscRepository;

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public void testTransaction2() {
		// The cwService.setLastUpdate() method is normally used for a complete
		// different purpose (actually part of the real application)
		// but we are also using it here for this test.
		cwService.setLastUpdate(new Date(), "transactionTest2-part1", DataSource.US);
		log.debug("transactionTest2(): completed update part1");

		cwService.setLastUpdate(new Date(), "transactionTest2-part2", DataSource.US);
		log.debug("transactionTest2(): completed update part2");
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public void testTransaction3() {
		devMiscRepository.testTransaction34("transactionTest3-part1");
		devMiscRepository.testTransaction34("transactionTest3-part2");
	}

	@Override
	public void testTransaction4() {
		devMiscRepository.testTransaction34("transactionTest4-part1");
		devMiscRepository.testTransaction34("transactionTest4-part2");
	}

	@Override
	public void testTransaction5() {
		devMiscRepository.testTransaction5("transactionTest5-part1");
		devMiscRepository.testTransaction5("transactionTest5-part2");
	}

	public CommonWorkService getCommonWorkService() {
		return cwService;
	}

	public void setCommonWorkService(CommonWorkService cwService) {
		this.cwService = cwService;
	}

	public DevMiscRepository getDevMiscRepository() {
		return devMiscRepository;
	}

	public void setDevMiscRepository(DevMiscRepository devMiscRepository) {
		this.devMiscRepository = devMiscRepository;
	}
}
