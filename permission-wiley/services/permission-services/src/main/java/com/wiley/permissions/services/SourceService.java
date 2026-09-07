package com.wiley.permissions.services;

import java.util.List;

import com.wiley.permissions.common.utils.UniqueConstraintViolationException;
import com.wiley.permissions.domain.persistence.permissions.ConditionNode;
import com.wiley.permissions.domain.persistence.permissions.MasterAgreementDeal;
import com.wiley.permissions.domain.persistence.permissions.Reference;
import com.wiley.permissions.domain.persistence.permissions.Source;
import com.wiley.permissions.domain.persistence.permissions.SourceFile;
import com.wiley.permissions.persistence.PersistenceException;
import com.wiley.permissions.services.util.ServiceException;

public interface SourceService {

	public void handleDeleteSourceMessage(List<Reference> refs) throws ServiceException, PersistenceException;

	public void deleteSourceByExternalId(String externalId, boolean sendMessage) throws ServiceException,
			PersistenceException;

	/**
	 * This method is used for both creating an source and modifying one. If
	 * the source externalId are null, then a
	 * create action is assumed.
	 * 
	 * While this method has no transaction annotation, it calls another method
	 * which is REQUIRES_NEW, so this method behaves similar to being REQUIRES_NEW.
	 *
	 * @throws Exception
	 */
	public Source saveSource(Source source, boolean sendMessage)
			throws PersistenceException, UniqueConstraintViolationException, Exception;

	public void deleteSourceFiles(Integer sourceId) throws PersistenceException;
	
	public void createSourceFile(SourceFile sourceFile) throws PersistenceException;

	public void deleteSourceFile(int fileId) throws PersistenceException;

	/**
	 * Sets the disabled flag as !current value.
	 * REQUIRES_NEW ensures commit is done right away.
	 *
	 * @param sourceExtId
	 * @throws Exception
	 */
	public void resetDisabledSourceFlag(String sourceExtId) throws Exception;
	
	public Source getSourceView(Integer sourceId) throws PersistenceException;

	public MasterAgreementDeal saveMasterAgreementDeal(MasterAgreementDeal maDeal, List<ConditionNode> conditions) throws Exception;

}