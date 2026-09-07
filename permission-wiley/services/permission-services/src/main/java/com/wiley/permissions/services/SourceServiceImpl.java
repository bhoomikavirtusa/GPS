package com.wiley.permissions.services;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.wiley.permissions.common.utils.UniqueConstraintViolationException;
import com.wiley.permissions.domain.persistence.permissions.ConditionNode;
import com.wiley.permissions.domain.persistence.permissions.Contract;
import com.wiley.permissions.domain.persistence.permissions.ContractAsset;
import com.wiley.permissions.domain.persistence.permissions.MasterAgreementDeal;
import com.wiley.permissions.domain.persistence.permissions.Reference;
import com.wiley.permissions.domain.persistence.permissions.Source;
import com.wiley.permissions.domain.persistence.permissions.SourceFile;
import com.wiley.permissions.persistence.PersistenceException;
import com.wiley.permissions.repositories.ConditionRepository;
import com.wiley.permissions.repositories.SourceRepository;
import com.wiley.permissions.services.message.CMSMessageService;
import com.wiley.permissions.services.util.ServiceException;

/**
 *
 */
@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = Exception.class)
public class SourceServiceImpl extends BaseService implements SourceService {

	private static final Log log = LogFactory.getLog(SourceService.class);

	private CMSMessageService outgoingMessageService;

	private SourceRepository sourceRepository;
	private ConditionRepository conditionRepository;

	public SourceServiceImpl() {
	}

	@Override
	public void handleDeleteSourceMessage(List<Reference> refs) throws ServiceException, PersistenceException
	{
		for (Reference ref : refs) {
			sourceRepository.deleteSourceByExternalId(ref.getExternalId());
		}
	}

	@Override
	public void deleteSourceByExternalId(String externalId, boolean sendMessage) throws ServiceException,
			PersistenceException
	{
		sourceRepository.deleteSourceByExternalId (externalId);

		if (sendMessage) {
			try {
				getOutgoingMessageService().sendDeleteSourceMessage(externalId);
				// throws MessageException
			}
			catch (Exception e) {
				log.error("deleteSourceByExternalId(): Exception caught trying to send DeleteSource message", e);
			}
		}
	}

	/**
	 * Note while this method has no transaction annotation, it calls another method
	 * which is REQUIRES_NEW, so this method behaves similar to being REQUIRES_NEW.
	 */
	@Override
	public Source saveSource(Source source, boolean sendMessage)
			throws PersistenceException, UniqueConstraintViolationException, Exception
	{
		boolean isNew = StringUtils.isBlank(source.getExternalId());

		source = sourceRepository.saveSource(source);  // REQUIRES_NEW

		if (isNew) {
			// reload the source to get the externalId created from the trigger
			source = sourceRepository.loadSourceById (source.getId());
		}

		if (sendMessage) {
			try {
				getOutgoingMessageService().sendUpdateSourceMessage(source, null);
			}
			catch (Exception ex) {
				log.error("Exception caught trying to send UpdateSource message: ", ex);
			}
		}

		return source;
	}

	/**
	 * Seems without the transactional annotation, the persist does nothing
	 * if we don't already have a transaction.
	 * @throws PersistenceException
	 */
	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public void createSourceFile(SourceFile sourceFile) throws PersistenceException {
		sourceRepository.persist(sourceFile);
	}

	@Override
	public void resetDisabledSourceFlag(String sourceExtId) throws Exception {
		sourceRepository.resetDisabledSourceFlag(sourceExtId);
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public Source getSourceView(Integer sourceId) throws PersistenceException {
		Source source = getSourceRepository().loadSourceByIdForView(sourceId, false);
		source.getContracts();

		for (Contract contract: source.getContracts()) {
			// Assume currency is the same for all contracts (it should be).
			contract.getCurrency().getCode();
			for (ContractAsset contractAsset: contract.getAssets()) {
				contractAsset.getAssetBase();
			}
		}

		return source;
	}

	@Override
	public MasterAgreementDeal saveMasterAgreementDeal(MasterAgreementDeal maDeal, List<ConditionNode> conditions) throws Exception
	{
		maDeal = getSourceRepository().saveRequiresNew(maDeal);
		getConditionRepository().saveMaDealConditions(maDeal.getId(), conditions);

		return maDeal;
	}

	@Override
	public void deleteSourceFiles(Integer sourceId) throws PersistenceException {
		sourceRepository.deleteSourceFiles(sourceId);
	}

	@Override
	public void deleteSourceFile(int fileId) throws PersistenceException {
		sourceRepository.deleteSourceFile(fileId);
	}

	public CMSMessageService getOutgoingMessageService() {
		return outgoingMessageService;
	}

	public void setOutgoingMessageService(CMSMessageService outgoingMessageService) {
		this.outgoingMessageService = outgoingMessageService;
	}

	public SourceRepository getSourceRepository() {
		return sourceRepository;
	}

	public void setSourceRepository(SourceRepository sourceRepository) {
		this.sourceRepository = sourceRepository;
	}

	public ConditionRepository getConditionRepository() {
    	return conditionRepository;
    }

	public void setConditionRepository(ConditionRepository conditionRepository) {
    	this.conditionRepository = conditionRepository;
    }
}
