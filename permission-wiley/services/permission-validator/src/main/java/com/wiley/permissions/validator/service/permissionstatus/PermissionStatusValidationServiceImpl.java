package com.wiley.permissions.validator.service.permissionstatus;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.permissions.domain.persistence.permissions.AssetUse;
import com.wiley.permissions.domain.persistence.permissions.PermissionStatus;
import com.wiley.permissions.repositories.AssetUseRepository;
import com.wiley.permissions.repositories.CommonWorkRepository;
import com.wiley.permissions.services.message.CMSMessageService;
import com.wiley.permissions.services.message.UpdatePermissionStatusInput;
import com.wiley.permissions.validator.rules.service.RuleService;
import com.wiley.sf.common.monitor.PerformanceMonitor;

/**
 * The Class PermissionStatusValidationServiceImpl.
 *
 * @author sputta
 */
public class PermissionStatusValidationServiceImpl implements PermissionStatusValidationService {

	private static final Log log = LogFactory.getLog(PermissionStatusValidationServiceImpl.class);

	private RuleService ruleService;
	private AssetUseRepository assetUseRepository;
	private CommonWorkRepository cwRepository;
	private CMSMessageService outgoingMessageService;

	private PermissionStatusRequiresNew newTransactionService;
	private PerformanceMonitor monitor;


	/**
	 * Instantiates a new permission status validation service impl.
	 */
	public PermissionStatusValidationServiceImpl() {
		super();
	}

	/**
	 * smarkoff: This method is currently not used (5/2011).
	 */
	/*
	@Transactional(propagation = Propagation.REQUIRED)
	public void updatePermissionStatusForCWId(Integer cwId)
	{
		log.debug("updatePermissionStatusForCW(): entered... ");
		getMonitor().startTimer("PermissionStatusValidationServiceImpl::updatePermissionStatusForCWId");

		try {
			// lnagy - I think it is safer to recalculate the statuses of assets instead of assetUse
			Collection<Integer> idCollection = getCommonWorkRepository().loadAssetIdsForCWId(cwId);

			updatePermissionStatusForAssetCollection(idCollection);
		}
		catch (Exception ex) {
			log.error("updatePermissionStatusForCWId(): Caught exception: ", ex);
		}

		getMonitor().stopTimer("PermissionStatusValidationServiceImpl::updatePermissionStatusForCWId");
		getMonitor().dumpTimerStats();
	}

	public void updatePermissionStatusForAssetCollection(Collection<Integer> idCollection) {
		log.debug("updatePermissionStatusForAssetCollection(): entered...collection size = " + idCollection.size());

		getMonitor().startTimer("PermissionStatusValidationServiceImpl::updatePermissionStatusForAssetCollection");
		for (Integer assetId : idCollection) {
			List<Integer> auIdList = assetUseRepository.loadAssetUseIdsForAssetId(assetId);
			updatePermissionStatusForAssetUseCollection(auIdList);
		}
		getMonitor().stopTimer("PermissionStatusValidationServiceImpl::updatePermissionStatusForAssetCollection");
		getMonitor().dumpTimerStats();
	}*/

	public Integer updatePermissionStatusForAssetUseCollection(UpdatePermissionStatusInput input) {
		Collection<Integer> idCollection = input.getAuIdCollection();
		boolean sendUpdateMessageForStatusChange = input.getSendUpdateMessageForStatusChange();

		log.debug("updatePermissionStatusForAssetUseCollection(): entered...collection size = " + idCollection.size());

		int changedCount = 0;
		Integer currentId = null;
		try {
			for (Integer auId : idCollection) {
				currentId = auId;
				boolean statusChanged = updatePermissionStatus(auId);
				// smarkoff: With 2,000 assetUses, this method is 3x faster doing a commit after each one
				// - commit automatically since updatePermissionStatus() calls a method that REQUIRES_NEW
				if (statusChanged) {
					changedCount++;
					if (sendUpdateMessageForStatusChange) {
						AssetUse au = assetUseRepository.loadAssetUseById(auId);
						getOutgoingMessageService().sendUpdateAssetUseMessage(au, null);
					}
				}
			}

			log.debug("updatePermissionStatusForAssetUseCollection(): "
				+ changedCount + " out of " + idCollection.size() + " AssetUses had a status change.");
		}
		catch (Exception ex) {
			log.error("updatePermissionStatusForAssetUseCollection(): got Exception updating status for auId "
				+ currentId, ex);
		}

		return changedCount;
	}

	/**
	 * Returns true if status changed.
	 */
	public boolean updatePermissionStatus(int auId) {
		return newTransactionService.updatePermissionStatus(auId);
	}

	/**
	 * To be called for unit testing only.
	 * Otherwise use the above method that takes an auId.
	 */
	public PermissionStatus updatePermissionStatus(AssetUse au) {
		return newTransactionService.updatePermissionStatus(au);
	}

	/** Don't use this (smarkoff)
	@Transactional(propagation = Propagation.REQUIRED)
	public void updatePermissionStatusForAllAssetUses() throws com.wiley.permissions.persistence.PersistenceException {
		List<Integer> auIds = assetUseService.loadAssetUseIds();
		log.debug("updatePermissionStatusForAllAssetUses(): number of AssetUses = " + auIds.size());
		long startTime = System.currentTimeMillis();
		int successCount = 0;
		int failedCount = 0;
		NumberFormat intFormat = NumberFormat.getIntegerInstance();

		for (Integer auId : auIds) {
			try {
				AssetUse au = getAssetUseService().getEntityManager().find(AssetUse.class, auId);
				updatePermissionStatus(au);
				TransactionManagerLookupUtility.commitCurrentTransactionAndStartNew();  // throws Exception
				successCount++;
				log.debug("updatePermissionStatusForAllAssetUses(): updated status successfully for AssetUse id = " + auId);
				log.debug("updatePermissionStatusForAllAssetUses(): failedCount = "
					+ intFormat.format(failedCount) + ", successCount = " + intFormat.format(successCount));
			}
			catch (Exception ex) {
				failedCount++;
				log.error("updatePermissionStatusForAllAssetUses(): failed to update status for AssetUse id = " + auId, ex);
			}
		}

		long time = System.currentTimeMillis() - startTime;
		log.debug("updatePermissionStatusForAllAssetUses(): failedCount = "
			+ intFormat.format(failedCount) + ", successCount = " + intFormat.format(successCount));
		TimeFormat timeFormat = new TimeFormat(true);
		log.debug("updatePermissionStatusForAllAssetUses(): took " + timeFormat.formatMS(time));
	}*/


	public RuleService getRuleService() {
		return ruleService;
	}

	public void setRuleService(RuleService ruleService) {
		this.ruleService = ruleService;
	}

	public CommonWorkRepository getCommonWorkRepository() {
		return cwRepository;
	}

	public void setCommonWorkRepository(CommonWorkRepository commonWorkService) {
		this.cwRepository = commonWorkService;
	}

	public PerformanceMonitor getMonitor() {
		return monitor;
	}

	public void setMonitor(PerformanceMonitor monitor) {
		this.monitor = monitor;
	}

	public AssetUseRepository getAssetUseRepository() {
		return assetUseRepository;
	}

	public void setAssetUseRepository(AssetUseRepository assetUseRepository) {
		this.assetUseRepository = assetUseRepository;
	}

	public PermissionStatusRequiresNew getNewTransactionService() {
		return newTransactionService;
	}

	public void setNewTransactionService(PermissionStatusRequiresNew newTransactionService) {
		this.newTransactionService = newTransactionService;
	}

	public void setOutgoingMessageService(CMSMessageService outgoingMessageService) {
		this.outgoingMessageService = outgoingMessageService;
	}

	public CMSMessageService getOutgoingMessageService() {
		return outgoingMessageService;
	}
}
