package com.wiley.permissions.services;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.wiley.permissions.common.utils.UniqueConstraintViolationException;
import com.wiley.permissions.common.utils.ValidateException;
import com.wiley.permissions.domain.message.MessageException;
import com.wiley.permissions.domain.persistence.permissions.CommonWork;
import com.wiley.permissions.domain.persistence.permissions.CommonWorkStatus;
import com.wiley.permissions.domain.persistence.permissions.Component;
import com.wiley.permissions.domain.persistence.permissions.Contract;
import com.wiley.permissions.domain.persistence.permissions.CwFile;
import com.wiley.permissions.domain.persistence.permissions.CwPhotoEstimate;
import com.wiley.permissions.domain.persistence.permissions.CwSummary;
import com.wiley.permissions.domain.persistence.permissions.DataSource;
import com.wiley.permissions.domain.persistence.permissions.ExtendedAssetUse;
import com.wiley.permissions.domain.persistence.permissions.Reference;
import com.wiley.permissions.domain.persistence.permissions.User;
import com.wiley.permissions.persistence.PersistenceException;
import com.wiley.permissions.security.web.PermissionsSecurityException;
import com.wiley.permissions.services.imports.ImportAssetsStatus;
import com.wiley.permissions.services.util.ServiceException;

public interface CommonWorkService {

	@Transactional(propagation = Propagation.REQUIRED)
	// lnagy - still need transaction here because the send message will try to lazy load the object
	public Component addComponentToCommonWork(Component component, boolean sendMessage)
			throws UniqueConstraintViolationException, PersistenceException, MessageException;

	@Transactional(propagation = Propagation.REQUIRED)
	// lnagy - still need transaction here because the send message will try to lazy load the object
	public Component saveComponent(Component component) throws PersistenceException;

	public void deleteComponentById(Integer componentId) throws PersistenceException, ValidateException;

	public void deleteComponentByExternalId(String externalId, boolean handlingMessage)
			throws PersistenceException, ValidateException;

	public void handleDeleteComponentMessage(List<Reference> refs) throws ServiceException, MessageException,
			PersistenceException;

	public void updateStatusForCommonWork(CommonWork cw) throws Exception;

	public void updateStatusForCommonWork(int cwId) throws Exception;

	public void updateAllStatuses();

	public void handleGetComponentMessage(List<Reference> refs) throws PersistenceException, MessageException;

	public void handleUpdateComponentMessage(List<Component> list) throws Exception;

	public void handleGetComponentListMessage(List<Reference> refs) throws PersistenceException,
			MessageException;

	public void sendEmailToAdminReceiver(String subject, String htmlMsg);

	public void sendEmailToAdminRFDealReceiver(String assetDescription, String isbn13, String sourceName, int assetCount); //Added to implement DM-122

	public void sendEmailToUploadDocsInitiator(String userEmail, String authorFull, String productTitle, String isbn13,
			String productId, String uploadHistoryId); //Added to implement DM-532
	
	public void addToCwHistory_UploadDocsHistory(Integer cwId, String message) throws PersistenceException;//Added to implement DM-532

	// Start : Added for DM-534
	public void sendAssetsExportEmail(String userEmail, String authorFull, String productTitle, String isbn13,String cwId);

	public void sendAssetsExportErrorEmail(String userEmail, String authorFull, String productTitle, String isbn13,String cwId);

	public void deleteAssetExcelFiles();
	// End : Added for DM-534

	public void sendEmailAuthorTempPassword(String email, String password);

	public void sendEmailAuthorAddedToCW(String email, String title);

	public void sendEmailContractCloseToExpire(Contract contract);

	public void sendEmailContractExpired(Contract contract);

	public void sendEmailWarningPrinting(CommonWork commonWork, Integer value, Contract contract);

	public void sendEmailExceedsPrinting(CommonWork commonWork, Integer value, Contract contract);

	public Component createUpdateComponentFromMessage(Component component) throws Exception;

	public void deleteCWFromUserWatch(int userId, String productExternalId) throws PermissionsSecurityException,
			ServiceException, PersistenceException;

	public void addProductToUserWatch(int userId, String productExternalId, String productDataSource) throws PersistenceException,
			PermissionsSecurityException, ServiceException;

	public void toggleWatched(int userId, int cwId) throws ServiceException, PermissionsSecurityException, PersistenceException;

	/** Returns a non-null message if the required operation is not allowed at this time or is allowed only because the user has the Admin role. */
	public String toggleCWStatus(int cwId, String cwStatusCode, boolean interior, boolean isAdmin) throws Exception;

	public void saveCwNotes(int cwId, String notes) throws PersistenceException;

	public void saveCwFile(CwFile file, int cwId) throws Exception;

	public List<CwPhotoEstimate> loadPhotoEstimates (int cwId);

	public void setInteriorCWStatus(CommonWork cw, CommonWorkStatus cwStatus);

	public void saveCwSummary(CwSummary cwSummary) throws PersistenceException;

	public void saveCwPhotoEstimate(CwPhotoEstimate cwPhotoEstimate) throws PersistenceException;

	public void copyAssets(int newCWId, int origCWId, List<Integer> values,
		boolean edition, boolean includeUsage, String sessionId, boolean includeCovers, int userGroupId, boolean copyFlag)
		throws PersistenceException, MessageException, IOException;

	public ImportAssetsStatus getCopyAssetsStatus(String sessionId);

	public void deleteCopyAssetsStatus(String sessionId);

	public boolean hasProfile(Integer cwId) throws PersistenceException;

	/****************************************************
	 * VIEWS LOAD METHODS							    *
	 * those methods that we want exposed thru a REST 	*
	 * service have to be defined in Service class 		*
	 ****************************************************/

	public CommonWork loadCWLandingView(Integer cwId) throws PersistenceException;

	public void addToCwHistory_assetUseDelete(Integer cwId) throws PersistenceException;

	public Date getLastUpdate(String objectType, DataSource dataSource);

	public void setLastUpdate(Date date, String objectType, DataSource dataSource);
	
	public void updatePEStatus(String status);
	
	public boolean getPEUpdateStatus(String objectType, DataSource dataSource);

	public User addAuthorIfNotIncluded(int cwId, User user) throws PersistenceException;

	public boolean initDefaultConditions(int cwId)  throws Exception;

	// Start : Added for DM-533
	public void validateCostDuplicationAssetWarning (List<ExtendedAssetUse> validList, StringBuilder errorMessage, ImportAssetsStatus status);
	public void validateCostDuplicationAssetError (List<ExtendedAssetUse> validList, StringBuilder errorMessage, ImportAssetsStatus status);
	// End : Added for DM-533

}
