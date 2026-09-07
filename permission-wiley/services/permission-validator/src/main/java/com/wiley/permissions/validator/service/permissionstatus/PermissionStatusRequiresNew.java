package com.wiley.permissions.validator.service.permissionstatus;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.wiley.permissions.domain.persistence.permissions.AssetUse;
import com.wiley.permissions.domain.persistence.permissions.AuSourcePermStatus;
import com.wiley.permissions.domain.persistence.permissions.ContractList;
import com.wiley.permissions.domain.persistence.permissions.ImportSource;
import com.wiley.permissions.domain.persistence.permissions.PermissionStatus;
import com.wiley.permissions.domain.persistence.permissions.PurchaseOrderList;
import com.wiley.permissions.domain.persistence.permissions.Condition;
import com.wiley.permissions.domain.persistence.permissions.Source;
import com.wiley.permissions.repositories.AssetUseRepository;
import com.wiley.permissions.repositories.ConditionRepository;
import com.wiley.permissions.services.ContractService;
import com.wiley.permissions.services.PurchaseOrderService;
import com.wiley.permissions.validator.rules.base.Operation;
import com.wiley.permissions.validator.rules.service.RuleService;
import com.wiley.permissions.validator.rules.service.RuleServiceInputDto;
import com.wiley.permissions.validator.rules.service.RuleServiceInputDtoImpl;
import com.wiley.permissions.validator.rules.service.RuleServiceOutputDto;
import com.wiley.permissions.validator.rules.service.ValidationResult;
import com.wiley.sf.common.monitor.PerformanceMonitor;
import com.wiley.sf.common.monitor.PerformanceMonitor.PerfTimer;

/**
 * Ideally this would be part of PermissionStatusValidationImpl but needs to
 * be a separate class so we can have REQUIRES_NEW paid attention to.
 */
public class PermissionStatusRequiresNew {

	private static final Log log = LogFactory.getLog(PermissionStatusRequiresNew.class);

	private RuleService ruleService;
	private AssetUseRepository assetUseRepository;
	private PurchaseOrderService poService;
	private ContractService contractService;
	private ConditionRepository conditionRepository;
	private PerformanceMonitor monitor;


	/**
	 * Returns true if the status changed.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public boolean updatePermissionStatus(int auId) {
		try {
			AssetUse au = assetUseRepository.loadAssetUseById(auId);
			log.debug("au.getStatusExplanation() --> "+au.getStatusExplanation());
			log.debug("au.getStatus() --> "+au.getStatus());
			PermissionStatus oldStatus = au.getStatus();
			PermissionStatus newStatus = updatePermissionStatus(au);
			// When fully on Java 7 use !Objects.equals(...) from JDK instead of ObjectUtils from Apache Commons
			// - will fix these deprecation warning
			boolean statusChanged = !ObjectUtils.equals(oldStatus, newStatus);
			// SR_280926 begins
						if(newStatus==null){
							au.setStatus(oldStatus);
							if((au.getStatus()==PermissionStatus.AMENDMENT_NEEDED) || (au.getStatus().equals(PermissionStatus.AMENDMENT_NEEDED))){
								au.setStatus(PermissionStatus.CONTRACT_INSUFFICIENT);
							}
						}
						else
						{
						au.setStatus(newStatus);
						}
						//SR_280928 ends
			
			
			assetUseRepository.saveRequiresNew(au);
			return statusChanged;
		}
		catch (Exception ex) {
			log.error("updatePermissionStatus(int auId): caught exception: ", ex);
			return false;
		}
	}

	/**
	 * This method should NOT be called directly from outside this class
	 * except for unit testing. Call the above method instead that takes an auId.
	 *
	 * @param assetUse  Must be non-null
	 */
	public PermissionStatus updatePermissionStatus(AssetUse assetUse) {
		log.debug("updatePermissionStatus(): entered...");
		PerfTimer timer = monitor.startTimer("PermissionStatusValidationServiceImpl::updatePermissionStatus");
		ValidationResult finalResult = null;

		try {
			Operation operation = new Operation("Permission-Status");

			// Copy source list so that if we add null we won't affect the original list
			List<Source> origSourceList = assetUse.getAsset().getSources();
			List<Source> sourceList2 = new ArrayList<Source>();
			if (origSourceList != null) {
				sourceList2.addAll(origSourceList);
			}

			log.debug("updatePermissionStatus(): source count: " + sourceList2.size());

			// important to call this method before adding null to sourceList2
			// lnagy - I call the repository directly for delete, and merge down below
			// not sure if we need to do it thru the service here because the PERMISSION status
			// calculation is already happening in mule
			assetUseRepository.deleteNonCurrentSourceStatuses(assetUse, sourceList2);

			if (sourceList2.size() == 0) {
				sourceList2.add(null);
			}

			StringBuilder explanation = new StringBuilder();

			for (Source currentSource: sourceList2) {
				// Note currentSource may be null (if there are no sources)
				log.debug("updatePermissionStatus(): calculating status for source " + currentSource);
				if (currentSource != null) {
					explanation.append("status for source [" + currentSource.getName() + "]:\n");
				}

				List<Object> facts = new ArrayList<Object>();
				// add the facts
				log.debug("ASSET ID--> "+assetUse.getAsset().isManaged()+"OWNER TYPE--> "+assetUse.getAsset().getOwnerType());
				facts.add(assetUse);
				log.debug("COMMON WORK ID "+assetUse.getCopyAssetsCwId());
				facts.add(currentSource);
				// make sure there is a non-null PO List and ContractList in the facts,
				// even if they are empty
				if (currentSource == null) {
					facts.add(new PurchaseOrderList());
					facts.add(new ContractList());
				}
				else {
					int assetId = assetUse.getAsset().getId();
					int sourceId = currentSource.getId();
					int cwId = assetUse.getCommonWork().getId();
					int commonWorkId = 0;
					int cwIdFromContract  = 0;
					List<Integer> cwIdList = new ArrayList<Integer>();
					//Added for paperwork Task Granted Starts
					log.debug(" Asset --> "+assetUse.getAsset().getOwnerType()+" Ownertype Description --> "+assetUse.getAsset().getOwnerTypeDescription());
					if(!assetUse.getAsset().isManaged()){
						if(null != assetUse.getImportSource() && assetUse.getImportSource().equals(ImportSource.COPY_FROM_PREVIOUS_EDITION)){
							commonWorkId = assetUseRepository.loadCwIdForAssetId(assetId, sourceId); //From Purcahse Order
							cwIdFromContract = assetUseRepository.loadCwIdForAssetIdForContract(assetId, sourceId, true); //From Latest Contract
							log.debug("Imported Asset");
							facts.add(poService.loadLatestForAssetSourceCW(assetId, sourceId, commonWorkId));
		                    facts.add(contractService.loadLatestForAssetSourceCW(assetId, sourceId, cwIdFromContract));
						}else{
						//Paperwork Task Granted Ends
							commonWorkId = assetUseRepository.loadCwIdForAssetId(assetId, sourceId); //From Purcahse Order
							cwIdFromContract = assetUseRepository.loadCwIdForAssetIdForContract(assetId, sourceId, true); 
							log.debug("Not Imported Asset");
							facts.add(poService.loadLatestForAssetSourceCW(assetId, sourceId, commonWorkId));
		                    facts.add(contractService.loadLatestForAssetSourceCW(assetId, sourceId, cwIdFromContract));
	                        //facts.add(poService.loadLatestForAssetSourceCW(assetId, sourceId, cwId));
	                    	//facts.add(contractService.loadLatestForAssetSourceCW(assetId, sourceId, cwId));
						}
						}else{
							commonWorkId = assetUseRepository.loadCwIdForAssetId(assetId, sourceId); //From Purcahse Order
							cwIdFromContract = assetUseRepository.loadCwIdForAssetIdForContract(assetId, sourceId, true); //From Latest Contract
							List<Condition> valueList=assetUseRepository.loadQuizConditionValueForAssetId(assetId,cwIdFromContract);
							if(valueList.size() > 0 && valueList.get(0).getRollupValue().equals("All media types including future types") && valueList.get(1).getRollupValue().equals("Worldwide")
									&& valueList.get(2).getRollupValue().equals("All Languages") && valueList.get(3).getRollupValue().equals("Unlimited print run is granted") 
									&& (valueList.get(4).getRollupValue().equals("Granted for this edition and all future editions") || valueList.get(4).getRollupValue().equals("Granted for this, future editions and/or entire author series"))
									&& valueList.get(5).getRollupValue().equals("Wiley can include the asset(s) in any ancillaries, derivatives and custom works") 
									&& valueList.get(6).getRollupValue().equals("Wiley can include the asset(s) when sub-licensing product")){
										facts.add(poService.loadLatestForAssetSourceCW(assetId, sourceId, commonWorkId));
					                    facts.add(contractService.loadLatestForAssetSourceCW(assetId, sourceId, cwIdFromContract));	
									}
									else{
										facts.add(poService.loadLatestForAssetSourceCW(assetId, sourceId, cwId));
										facts.add(contractService.loadLatestForAssetSourceCW(assetId, sourceId, cwId));
									}
							
						}
				}

				// Create the Globals
				Map<String, Object> globals = new HashMap<String, Object>();
				RuleServiceInputDto input = new RuleServiceInputDtoImpl(facts, globals, operation);
				RuleServiceOutputDto output = ruleService.execute(input);
				ValidationResult result = null;
				if (output != null) {  // normally output should not be null
					result = output.getResult();
					log.debug("result value.....99" +result );

				}

				if (result == null) {
					log.error("updatePermissionStatus(): We've run into a situation that " +
							"is not covered by a rule [" + assetUse + "]");
				}
				else {
					explanation.append(result.getExplanation());
					explanation.append("\n");
					//Ram added logs for asset issue
					log.debug("result.getExplanation()1" +result.getExplanation());
					//ends here asset issue
					if (currentSource == null) {
						log.debug("updatePermissionStatus(): Saving status for auId " + assetUse.getId() + " and sourceId null");

						// lnagy - update the reference table
						//Ram added logs for asset issue
						log.debug("result.getExplanation()3" +result.getStatus() +"when currentSource=null");
						log.debug("result.getExplanation()3" +result.getExplanation() +"when currentSource=null");
						//ends here asset issue
						assetUseRepository.saveAssetPermissionRefStatus(assetUse, null,
								null, null, result.getStatus(), result.getExplanation());
					} else {
						log.debug("updatePermissionStatus(): Saving status for auId " + assetUse.getId() + " and sourceId " + currentSource.getId());

						//Ram added logs for asset issue
						log.debug("result.getExplanation()2" +result.getStatus());
						log.debug("result.getExplanation()2" +result.getExplanation());

						//ends here asset issue

						AuSourcePermStatus asps = new AuSourcePermStatus(assetUse, currentSource, result.getStatus(), result.getExplanation());
						asps.setLatestContractId(result.getContractId());
						asps.setLatestPoId(result.getPoId());
						asps.setNeedPaymentRequest(result.isNeedPaymentRequest());
						asps.setPaid(result.isPaid());

				//Added code for paper work C
					/*	if(assetUse.getWizardOwnerType.equals("Wiley_Created")){
						int assetId = assetUse.getAsset().getId();
                        int sourceId = currentSource.getId();
                        int cwId = assetUse.getCommonWork().getId();
                   //     contractService.loadLatestForAssetSourceCW(assetId, sourceId, cwId);
                   //     asps.setLatestContractId(contractList.getContractId().getId());

                     //   log.debug("paper work contractList..... " +contractList.getContractId().getId());
					}
					*/

				//ends code for paper work C



						if (result.getContractId() != null) {
							String creditLine = conditionRepository.loadCreditLineForContractAsset(result.getContractId(), assetUse.getAsset().getId());
							asps.setContractCreditLine(creditLine);
						}

						if (PermissionStatus.isGranted(result.getStatus())) {
							asps.setActiveContractId(result.getContractId());
						}

						if (PermissionStatus.isFormSent(result.getStatus())) {
							asps.setActivePoId(result.getPoId());
						}
						asps = assetUseRepository.saveRequiresNew(asps);

						// lnagy - update the reference table
						assetUseRepository.saveAssetPermissionRefStatus(assetUse, currentSource.getId(),
								asps.getActivePoId(), asps.getActiveContractId(), result.getStatus(), result.getExplanation());
					}
				}

				if (null == finalResult) {
					finalResult = result;
				}
				else if (null != finalResult && null != result) {
					PermissionStatus finalStatus = finalResult.getStatus();
					PermissionStatus status = result.getStatus();
					log.debug("updatePermissionStatus(): status ratings for old status "
							+ finalStatus.getCode() + " = " + finalStatus.getStatusRating());
					log.debug("updatePermissionStatus(): status ratings for new status "
							+ status.getCode() + " = " + status.getStatusRating());

					// if needPaymentRequest / paid are true for any source then "rollup" to true value
					finalResult.setNeedPaymentRequest(finalResult.isNeedPaymentRequest() || result.isNeedPaymentRequest());
					finalResult.setPaid(finalResult.isPaid() || result.isPaid());

					if (status.getStatusRating() < finalStatus.getStatusRating()) {
						finalResult = result;
						log.debug("updatePermissionStatus(): status is now " + status.getCode());
					}
				}

			} // end for (source loop)

			assetUse.setLastUpdatedStatus(new Date());
			// finalResult should not be null, but it might be if we have a bug in the rules
			// (this will get logged above)
			if (finalResult == null) {
				assetUse.setStatus(null);
			}
			else {
				assetUse.setStatus(finalResult.getStatus());
				// finalResult.isNeedPaymentRequest() / isPaid() will be true if ANY result (for any source) was true
				assetUse.setNeedPaymentRequest(finalResult.isNeedPaymentRequest());
				assetUse.setPaid(finalResult.isPaid());
			}
			assetUse.setStatusExplanation(explanation.toString());
		}
		catch (Exception ex) {
			log.error("updatePermissionStatus(): caught exception: ", ex);
			finalResult = null;  // null means unknown
		}

		log.debug("updatePermissionStatus(): finalResult for auId " + assetUse.getId() + ": " + finalResult);
		timer.stopTimer();
		return finalResult == null ? null : finalResult.getStatus();
	}


	public RuleService getRuleService() {
		return ruleService;
	}

	public void setRuleService(RuleService ruleService) {
		this.ruleService = ruleService;
	}

	public AssetUseRepository getAssetUseRepository() {
		return assetUseRepository;
	}

	public void setAssetUseRepository(AssetUseRepository assetUseRepository) {
		this.assetUseRepository = assetUseRepository;
	}

	public PurchaseOrderService getPurchaseOrderService() {
		return poService;
	}

	public void setPurchaseOrderService(PurchaseOrderService poService) {
		this.poService = poService;
	}

	public ContractService getContractService() {
		return contractService;
	}

	public void setContractService(ContractService contractService) {
		this.contractService = contractService;
	}

	public ConditionRepository getConditionRepository() {
		return conditionRepository;
	}

	public void setConditionRepository(ConditionRepository conditionRepository) {
		this.conditionRepository = conditionRepository;
	}

	public PerformanceMonitor getMonitor() {
		return monitor;
	}

	public void setMonitor(PerformanceMonitor monitor) {
		this.monitor = monitor;
	}

}
