package com.wiley.permissions.services;

import java.io.File;	// Added for DM-534
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;	// Added for DM-533
import java.util.List;
import java.util.Map;	// Added for DM-533
import java.util.Map.Entry;	// Added for DM-533

import javax.mail.MessagingException;	// Added for DM-534
import javax.mail.internet.MimeMessage;	// Added for DM-534
import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.mail.HtmlEmail;
import org.springframework.core.io.FileSystemResource;	// Added for DM-534
import org.springframework.mail.MailParseException;	// Added for DM-534
import org.springframework.mail.javamail.JavaMailSenderImpl;	// Added for DM-534
import org.springframework.mail.javamail.MimeMessageHelper;	// Added for DM-534
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.wiley.permissions.common.utils.SrcSrcRefInvCombination;	// Added for DM-533
import com.wiley.permissions.common.utils.UniqueConstraintViolationException;
import com.wiley.permissions.common.utils.ValidateException;
import com.wiley.permissions.domain.message.MessageException;
import com.wiley.permissions.domain.persistence.permissions.AssetUse;
import com.wiley.permissions.domain.persistence.permissions.BusinessUnit;
import com.wiley.permissions.domain.persistence.permissions.CommonWork;
import com.wiley.permissions.domain.persistence.permissions.CommonWorkStatus;
import com.wiley.permissions.domain.persistence.permissions.Component;
import com.wiley.permissions.domain.persistence.permissions.ComponentList;
import com.wiley.permissions.domain.persistence.permissions.Condition;
import com.wiley.permissions.domain.persistence.permissions.Contract;
import com.wiley.permissions.domain.persistence.permissions.CwFile;
import com.wiley.permissions.domain.persistence.permissions.CwHistory;
import com.wiley.permissions.domain.persistence.permissions.CwPhotoEstimate;
import com.wiley.permissions.domain.persistence.permissions.CwSummary;
import com.wiley.permissions.domain.persistence.permissions.DataSource;
import com.wiley.permissions.domain.persistence.permissions.ExtendedAssetUse;	// Added for DM-533
import com.wiley.permissions.domain.persistence.permissions.MessageErrorOp;
import com.wiley.permissions.domain.persistence.permissions.Product;
import com.wiley.permissions.domain.persistence.permissions.Reference;
import com.wiley.permissions.domain.persistence.permissions.Role;
import com.wiley.permissions.domain.persistence.permissions.User;
import com.wiley.permissions.persistence.PersistenceException;
import com.wiley.permissions.repositories.AssetUseRepository;
import com.wiley.permissions.repositories.CommonWorkRepository;
import com.wiley.permissions.repositories.ConditionRepository;
import com.wiley.permissions.repositories.ContractRepository;
import com.wiley.permissions.repositories.ProductRepository;
import com.wiley.permissions.repositories.UserRepository;
import com.wiley.permissions.security.web.PermissionsSecurityException;
import com.wiley.permissions.security.web.ThreadLocalUser;
import com.wiley.permissions.security.web.UserPrincipal;
import com.wiley.permissions.services.imports.ImportAssetsStatus;
import com.wiley.permissions.services.message.CMSMessageService;
import com.wiley.permissions.services.util.ServiceException;
import com.wiley.sf.common.lang.ArgUtil;
import com.wiley.sf.common.text.TimeFormat;

/**
 * This class will also be exposed thru a REST service
 * lnagy - for now all methods are Transactional (SUPPORT) because of the Filter
 * (when the filter tries to persist the session we get detached entity tried to persist)
 *
 * @author lnagy
 */
// @Path ("/cw")
@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = Exception.class)
public class CommonWorkServiceImpl extends BaseService implements CommonWorkService {

	private static final Log log = LogFactory.getLog(CommonWorkServiceImpl.class);

	private String smtpServer;

	private CMSMessageService outgoingMessageService;
	private ProductService productService;
	private AssetUseService assetUseService;
	private AssetUseIndexService assetUseIndexService;

	private AssetUseRepository assetUseRepository;
	private CommonWorkRepository cwRepository;
	private UserRepository userRepository;
	private ProductRepository productRepository;
	private ContractRepository contractRepository;
	private ConditionRepository conditionRepository;
	private ProductIndexService productIndexService;

	private String internalUrl;
	private String authorUrl;
	private String saveExcelFilesUrl; // Added for DM-534

	// I created this in case we might want to expose the copyAssets method thru a service
	// in that case I need to provide the status back to the client, so I need to persist the status until it is done.
	// I associate the status with the sessionId
	private static HashMap<String, ImportAssetsStatus> copyAssetStatusMap = new HashMap<String, ImportAssetsStatus>();


	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public Component addComponentToCommonWork (Component component, boolean sendMessage)
		throws UniqueConstraintViolationException, PersistenceException, MessageException
	{
		component = cwRepository.addComponentToCommonWork(component);

		if (sendMessage) {
			// reload to get values given by triggers for externalId, dates
			component = cwRepository.loadComponentById (component.getId());
			try {
				// following line throws MessageException
				outgoingMessageService.sendUpdateComponentMessage(component, null);
			}
			catch (Exception e) {
				log.error("addComponentToProduct(): Exception caught trying to send UpdateComponent message", e);
			}
		}
		return component;
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	// lnagy - still need transaction here because the send message will try to lazy load the object
	// (I overwrite it here and not allow the SUPPORT to take care of it because we will need it after we remove the filter )
	public Component saveComponent(Component component) throws PersistenceException
	{
		component = cwRepository.saveRequiresNew(component);

		// reload to get values given by triggers for externalId, dates
		component = cwRepository.loadComponentById (component.getId());

		try {
			// following line throws MessageException
			outgoingMessageService.sendUpdateComponentMessage(component, null);
		}
		catch (Exception e) {
			log.error("saveComponent(): Exception caught trying to send UpdateComponent message", e);
		}
		return component;
	}

	@Override
	public void deleteComponentById(Integer componentId) throws PersistenceException, ValidateException
	{
		Component component = cwRepository.loadComponentById(componentId);
		deleteComponent (component, false);
	}

	@Override
	public void deleteComponentByExternalId(String externalId, boolean handlingMessage)
	throws PersistenceException, ValidateException
	{
		Component component = cwRepository.loadComponentByExternalId(externalId);
		deleteComponent (component, handlingMessage);
	}


	private void deleteComponent(Component component, boolean handlingMessage)
	throws PersistenceException, ValidateException
	{
		ArgUtil.notNull(component, "component");

		String externalId = component.getExternalId();

		boolean didDelete = false;
		try {
			didDelete = cwRepository.deleteComponentById(component.getId());
		}
		catch (ValidateException e) {
			if (handlingMessage) {
				String replyId = null; // TODO: Somehow pass replyId to this method
				outgoingMessageService.sendErrorMessageLogException(
						replyId,
						MessageErrorOp.CANNOT_DELETE,
						e.getMessage(),
						externalId);
			}
			else {
				throw new ValidateException(e.getMessage());
			}
		}

		if (didDelete) {
			try {
				// following line throws MessageException
				outgoingMessageService.sendDeleteComponentMessage(externalId);
			}
			catch (Exception e) {
				log.error("deleteComponentById(): Exception caught trying to send DeleteComponent message", e);
			}
		}
	}

	@Override
	public void handleDeleteComponentMessage(List<Reference> refs) throws ServiceException, MessageException,
			PersistenceException
	{
		log.info("handleDeleteComponentMessage() called");

		for (Reference ref : refs) {
			try {
				deleteComponentByExternalId(ref.getExternalId(), true);
			}
			catch (ValidateException e) {
				log.error("Failed to delete component ", e);
			}
		}
	}

	@Override
	public void updateStatusForCommonWork(CommonWork commonWork) throws Exception {
		// preload cwId because if do after commit can get exception
		Integer cwId = commonWork.getId();

		// if cwId is null, it is a new commonWork and we have no assets,
		// so no reason to call updateStatus
		if (cwId == null)
			return;

		updateStatusForCommonWork(cwId);
	}

	@Override
	public void updateStatusForCommonWork(int cwId) throws Exception {
		// TODO lnagy - maybe we should refresh all assets instead of all AssetUses
		Collection<AssetUse> auCollection = assetUseRepository.loadAssetUseListByCWId(cwId);

		// if no assets, no use to go farther
		if (CollectionUtils.isEmpty(auCollection))
			return;

		assetUseService.updateStatusForAssetUseCollection(auCollection);
	}

	@Override
	public void updateAllStatuses() {
		long startTime = System.currentTimeMillis();
		int failedCount = 0;

		try {
			List<Integer> cwIdList = cwRepository.loadCWIdsForStatusRecalculate();

			if (cwIdList.size() == 0) {
				log.debug("updateAllStatuses(): no CommonWorks found.");
				return;
			}

			int count = 1;
			final int total = cwIdList.size();
			NumberFormat intFormat = NumberFormat.getIntegerInstance();

			for (Integer cwId : cwIdList) {
				log.debug("updateAllStatuses(): cw id = " + cwId
					+ " (" + intFormat.format(count) + " out of " + intFormat.format(total)
					+ ") (" + intFormat.format(failedCount) + " failed)");

				try {
					updateStatusForCommonWork(cwId);
				}
				catch (Exception e) {
					failedCount++;
					log.error("updateAllStatuses(): failed to update cw id [" + cwId + "] ", e);
				}

				count++;
			}
		}
		catch (PersistenceException e1) {
			log.error("updateAllStatuses(): failed to read CommonWorks ", e1);
		}

		long time = System.currentTimeMillis() - startTime;
		TimeFormat timeFormat = new TimeFormat(true);
		log.debug("updateAllStatuses(): *** All Statuses have been updated in " + timeFormat.formatMS(time)
			+ " (failed count = " + failedCount + ") ***");
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	// lnagy - still need transaction here because the send message will try to lazy load the object
	// (I overwrite it here and not allow the SUPPORT to take care of it because we will need it after we remove the filter )
	public void handleGetComponentMessage(List<Reference> refs) throws PersistenceException, MessageException
	{
		log.info("handleGetComponentMessage() called");

		// TODO: need to pass original message ID to this method somehow so can
		// use for replyId
		String replyId = null;

		for (Reference ref : refs) {
			Component c = cwRepository.loadComponentByExternalId(ref.getExternalId());
			// throws PersistenceException
			if (c == null) {
				outgoingMessageService.sendErrorMessage(
						replyId,
						MessageErrorOp.NOT_FOUND,
						"Could not find the Component with the given wid",
						ref.getExternalId());
			}
			else {
				outgoingMessageService.sendUpdateComponentMessage(c, replyId);
				// throws MessageException
			}
		}
	}

	@Override
	public void handleUpdateComponentMessage(List<Component> list) throws Exception
	{
		for (Component c : list) {
			log.info("handleUpdateComponent(): Component = " + c);
			createUpdateComponentFromMessage(c);  // throws Exception
		}
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	// lnagy - still need transaction here because the send message will try to lazy load the object
	// (I overwrite it here and not allow the SUPPORT to take care of it because we will need it after we remove the filter )
	public void handleGetComponentListMessage(List<Reference> refs) throws PersistenceException,
			MessageException
	{
		log.info("handleGetComponentListMessage() called");

		// TODO: need to pass original message ID to this method somehow so can
		// use for replyId
		String replyId = null;

		for (Reference ref : refs) {
			Product p = productRepository.loadByExternalId(ref.getExternalId());
			// throws PersistenceException
			if (p == null) {
				outgoingMessageService.sendErrorMessage(
						replyId,
						MessageErrorOp.NOT_FOUND,
						"Could not find the Product with the given wid",
						ref.getExternalId());
			}
			else {
				List<Component> list = cwRepository.loadComponentList(p.getCommonWork().getId(), true);
				ComponentList list2 = new ComponentList(list);
				outgoingMessageService.sendReportComponentListMessage(list2, replyId);
				// throws MessageException
			}
		}
	}

	@Override
	public void sendEmailToAdminReceiver(String subject, String htmlMsg) {
		try {
			HtmlEmail htmlEmail = new HtmlEmail();
			htmlEmail.setHostName(getSmtpServer());
			// Windows doesn't have the HOSTNAME var so do a different way
			//String localHostName = System.getenv("HOSTNAME");
			String localHostName = InetAddress.getLocalHost().getHostName();

			htmlEmail.setFrom(getMessageSource().getMessage("email.from.admin", null, null));
			subject = localHostName + ": " + subject;
			htmlEmail.setSubject(subject);
			htmlEmail.setHtmlMsg(htmlMsg);

			List<User> superUsers = userRepository.getAllUsersByGlobalRole(Role.ADMIN_EMAIL_RECEIVER);
			for (User user : superUsers) {
				htmlEmail.addTo(user.getEmail());
			}

			htmlEmail.send(); // throws EmailException
		}
		catch (Exception e) {
			log.error("sendEmailToSuperUsers(): failed", e);
		}
	}

	//Start: Added to implement DM-122
	@Override
	public void sendEmailToAdminRFDealReceiver(String assetDescription, String isbn13, String sourceName, int assetCount) {
		try {
			HtmlEmail htmlEmail = new HtmlEmail();
			htmlEmail.setHostName(getSmtpServer());
			// Windows doesn't have the HOSTNAME var so do a different way
			//String localHostName = System.getenv("HOSTNAME");
			//String localHostName = InetAddress.getLocalHost().getHostName();

			htmlEmail.setFrom(getMessageSource().getMessage("email.from.admin", null, null));

			List<User> superUsers = userRepository.getAllUsersByGlobalRole(Role.RFDEAL_MAIL_RECEIVER);
			for (User user : superUsers) {
				htmlEmail.addTo(user.getEmail());
			}

			String subject = getMessageSource().getMessage(
					"email.subject.assetlevel.rfdeal.added",
					new Object[] { sourceName },
					null);
			htmlEmail.setSubject(subject);
			String body = getMessageSource().getMessage(
					"email.body.assetlevel.rfdeal.added",
					new Object[] { assetDescription, isbn13, sourceName, assetCount },
					null);
			htmlEmail.setHtmlMsg(body);

			htmlEmail.send(); // throws EmailException
		}
		catch (Exception e) {
			log.error("sendEmailToSuperUsers(): failed", e);
		}
	}
	//End: Added to implement DM-122

	//Start: Added to implement DM-532
	@Override
	public void sendEmailToUploadDocsInitiator(String userEmail, String authorFull, String productTitle, String isbn13,
															String cwId, String uploadHistoryId) {
		try {
			HtmlEmail htmlEmail = new HtmlEmail();
			htmlEmail.setHostName(getSmtpServer());

			htmlEmail.setFrom(getMessageSource().getMessage("email.from.admin", null, null));

			htmlEmail.addTo(userEmail);

			String subject = getMessageSource().getMessage(
					"email.subject.contractfiles.bulkupload.status",
					new Object[] { authorFull, productTitle, isbn13 },
					null);
			htmlEmail.setSubject(subject);
			String body = getMessageSource().getMessage(
					"email.body.contractfiles.bulkupload.status",
					new Object[] { authorFull, productTitle, isbn13, internalUrl, cwId, uploadHistoryId },
					null);
			htmlEmail.setHtmlMsg(body);

			htmlEmail.send(); // throws EmailException
		}
		catch (Exception e) {
			log.error("sendEmailToUploadDocsInitiator(): failed", e);
		}
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public void addToCwHistory_UploadDocsHistory(Integer cwId, String message) throws PersistenceException {
		addToCwHistory(cwId, message);
	}
	//End: Added to implement DM-532

	//Start: Added to implement DM-534
			/*
			 * @Override
			public void sendAssetsExportEmail(String userEmail, String authorFull, String productTitle, String isbn13,String cwId) {
				try {
					HtmlEmail htmlEmail = new HtmlEmail();
					htmlEmail.setHostName(getSmtpServer());

					htmlEmail.setFrom(getMessageSource().getMessage("email.from.admin", null, null));

					htmlEmail.addTo(userEmail);

					String subject = getMessageSource().getMessage(
							"email.subject.assets.export",
							new Object[] { authorFull, productTitle, isbn13 },
							null);
					htmlEmail.setSubject(subject);
					String body = getMessageSource().getMessage(
							"email.body.assets.export",
							new Object[] { authorFull, productTitle, isbn13, cwId },
							null);
					htmlEmail.setHtmlMsg(body);
					//htmlEmail.

					htmlEmail.send(); // throws EmailException
				}
				catch (Exception e) {
					log.error("sendAssetsExportEmail(): failed", e);
				}
			}
			*/

			@Override
			public void sendAssetsExportEmail(String userEmail, String authorFull, String productTitle, String isbn13, String toSaveFileName) {
				try {
					log.debug("in sendAssetExportEmail() userEmail "+userEmail+"  authorFull "+authorFull+" productTitle "+productTitle+" isbn13 "+" toSaveFileName "+toSaveFileName);
					JavaMailSenderImpl sender = new JavaMailSenderImpl();
					MimeMessage mimeMessage = sender.createMimeMessage();
					sender.setHost(getSmtpServer());
					//sender.setPort(465);
					//sender.setUsername("chethanv.43@gmail.com");
						try {
							MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
							helper.setFrom(getMessageSource().getMessage("email.from.admin", null, null));
							helper.setTo(userEmail);
							String subject = getMessageSource().getMessage("email.subject.assets.export",
											new Object[] { authorFull, productTitle, isbn13 },null);
							String body = getMessageSource().getMessage("email.body.assets.export",
											new Object[] { authorFull, productTitle, isbn13 },null);
							helper.setSubject(subject);
							helper.setText(body);
							FileSystemResource file = new FileSystemResource(toSaveFileName);
							log.debug("Subject "+subject+" body "+body+" fileName "+file.getFilename()+" substring "+file.getFilename().substring(7));
							helper.addAttachment(file.getFilename().substring(7), file);
							sender.send(mimeMessage);
							} catch (MessagingException e) {
									throw new MailParseException(e);
							}
					}catch (Exception e) {
							log.error("sendAssetsExportEmail(): failed", e);
					}
				}

			@Override
			public void sendAssetsExportErrorEmail(String userEmail, String authorFull, String productTitle, String isbn13,String cwId) {
				try {
					HtmlEmail htmlEmail = new HtmlEmail();
					htmlEmail.setHostName(getSmtpServer());

					htmlEmail.setFrom(getMessageSource().getMessage("email.from.admin", null, null));

					htmlEmail.addTo(userEmail);

					String subject = getMessageSource().getMessage(
							"email.subject.assets.export.error",
							new Object[] { authorFull, productTitle, isbn13 },
							null);
					htmlEmail.setSubject(subject);
					String body = getMessageSource().getMessage(
							"email.body.assets.export.error",
							new Object[] { authorFull, productTitle, isbn13, cwId },
							null);
					htmlEmail.setHtmlMsg(body);
					//htmlEmail.

					htmlEmail.send(); // throws EmailException
				}
				catch (Exception e) {
					log.error("sendAssetsExportEmail(): failed", e);
				}
			}

			@Override
			public void deleteAssetExcelFiles() {
				log.debug("deleteAssetExcelFiles() entered");
				File files = new File(getSaveExcelFilesUrl());

			    if(files.isDirectory())
			    {
			    	for(File f : files.listFiles() )
			    	{
			    		 long diff = new Date().getTime() - f.lastModified();
		                 long cutoff = (30 * (24L * 60L * 60L * 1000L));

			    		if(diff>cutoff)
			    		{
			    			log.debug("Deleting old file "+f.getName());
			    			f.delete();
			    		}
			    	}
			    }
			}
			//End: Added to implement DM-534


	@Override
	public void sendEmailAuthorTempPassword(String email, String password) {
		try {
			HtmlEmail htmlEmail = new HtmlEmail();
			htmlEmail.setHostName(getSmtpServer());
			// Windows doesn't have the HOSTNAME var so do a different way
			//String localHostName = System.getenv("HOSTNAME");
			String localHostName = InetAddress.getLocalHost().getHostName();

			htmlEmail.setFrom(getMessageSource().getMessage("email.from.admin", null, null));
			htmlEmail.addTo(email);

			String subject = getMessageSource().getMessage(
					"email.subject.author.temp.password",
					new Object[] { },
					null);
			htmlEmail.setSubject(subject);
			String body = getMessageSource().getMessage(
					"email.body.author.temp.password",
					new Object[] { password, authorUrl, localHostName },
					null);
			htmlEmail.setHtmlMsg(body);

			htmlEmail.send();  // throws EmailException
			log.debug("sendEmailAuthorTempPassword(): sent email ok");
		}
		catch (Exception e) {
			log.error("sendEmailAuthorTempPassword(): failed", e);
		}
	}

	@Override
	public void sendEmailAuthorAddedToCW(String email, String title) {
		try {
			HtmlEmail htmlEmail = new HtmlEmail();
			htmlEmail.setHostName(getSmtpServer());
			// Windows doesn't have the HOSTNAME var so do a different way
			//String localHostName = System.getenv("HOSTNAME");
			String localHostName = InetAddress.getLocalHost().getHostName();

			htmlEmail.setFrom(getMessageSource().getMessage("email.from.admin", null, null));
			htmlEmail.addTo(email);

			String subject = getMessageSource().getMessage(
					"email.subject.author.added.to.cw",
					new Object[] { },
					null);
			htmlEmail.setSubject(subject);
			String body = getMessageSource().getMessage(
					"email.body.author.added.to.cw",
					new Object[] { title, authorUrl, localHostName },
					null);
			htmlEmail.setHtmlMsg(body);

			htmlEmail.send();  // throws EmailException
			log.debug("sendEmailAuthorAddedToCW(): sent email ok");
		}
		catch (Exception e) {
			log.error("sendEmailAuthorAddedToCW(): failed", e);
		}
	}

	@Override
	public void sendEmailContractCloseToExpire(Contract contract) {
		sendEmailContract(contract, "email.subject.contract.close.to.expire",
			"email.body.contract.close.to.expire");
	}

	@Override
	public void sendEmailContractExpired(Contract contract) {
		sendEmailContract(contract, "email.subject.contract.expired",
			"email.body.contract.expired");
	}

	private void sendEmailContract(Contract contract, String subjectKey, String messageKey) {
		try {
			Product product = contract.getCommonWork().getPrimaryProduct();
			HtmlEmail htmlEmail = getEmailAddressesFromBusinessUnit(product, contract);
			if (htmlEmail.getToAddresses().size() == 0) {
				log.debug("sendEmailContract(): no email addresses configured, won't send email.");
				return;
			}
			String sourceName = contract.getSource().getName();

			// Windows doesn't have the HOSTNAME var so do a different way
			//String localHostName = System.getenv("HOSTNAME");
			String localHostName = InetAddress.getLocalHost().getHostName();

			String subject = getMessageSource().getMessage(
					subjectKey,
					new Object[] { product.getAuthorFullName(), product.getTitle(),
							product.getIsbn13(), sourceName },
					null);
			htmlEmail.setSubject(subject);
			String body = getMessageSource().getMessage(
					messageKey,
					new Object[] { product.getTitle(), product.getIsbn13(),
							sourceName, contract.getEndDate(),
							contract.getNumber(), contract.getDate(),
							internalUrl, product.getId(), localHostName },
					null);
			htmlEmail.setHtmlMsg(body);
			htmlEmail.send(); // throws EmailException
		}
		catch (Exception e) {
			log.error("sendEmailContract(): failed", e);
		}
	}

	@Override
	public void sendEmailWarningPrinting(CommonWork commonWork, Integer value, Contract contract) {
		try {
			Product product = commonWork.getPrimaryProduct();
			HtmlEmail htmlEmail = getEmailAddressesFromBusinessUnit(product, contract);
			String sourceName = contract.getSource().getName();

			// Windows doesn't have the HOSTNAME var so do a different way
			//String localHostName = System.getenv("HOSTNAME");
			String localHostName = InetAddress.getLocalHost().getHostName();

			String subject = getMessageSource().getMessage(
					"email.subject.print.run.warning",
					new Object[] { product.getAuthorFullName(), product.getTitle(),
							product.getIsbn13(), sourceName },
					null);
			htmlEmail.setSubject(subject);
			int cwId = contract.getCommonWork().getId();
			String body = getMessageSource().getMessage(
					"email.body.print.run.warning",
					new Object[] { product.getTitle(), product.getIsbn13(),
							cwRepository.totalPrintings(cwId), sourceName, value,
							contract.getNumber(), contract.getDate(), internalUrl, product.getId(), localHostName },
					null);
			htmlEmail.setHtmlMsg(body);
			htmlEmail.send(); // throws EmailException
		}
		catch (Exception e) {
			log.error("sendEmailWarningPrinting(): failed", e);
		}
	}

	@Override
	public void sendEmailExceedsPrinting(CommonWork commonWork, Integer value, Contract contract) {
		try {
			Product product = commonWork.getPrimaryProduct();
			HtmlEmail htmlEmail = getEmailAddressesFromBusinessUnit(product, contract);
			String sourceName = contract.getSource().getName();

			// Windows doesn't have the HOSTNAME var so do a different way
			//String localHostName = System.getenv("HOSTNAME");
			String localHostName = InetAddress.getLocalHost().getHostName();

			String subject = getMessageSource().getMessage(
					"email.subject.print.run.exceed",
					new Object[] { product.getAuthorFullName(), product.getTitle(),
							product.getIsbn13(), sourceName },
					null);
			htmlEmail.setSubject(subject);
			int cwId = contract.getCommonWork().getId();
			String body = getMessageSource().getMessage(
					"email.body.print.run.exceed",
					new Object[] { product.getTitle(), product.getIsbn13(),
							cwRepository.totalPrintings(cwId), sourceName, value,
							contract.getNumber(), contract.getDate(), internalUrl, product.getId(), localHostName },
					null);
			htmlEmail.setHtmlMsg(body);
			htmlEmail.send(); // throws EmailException
		}
		catch (Exception e) {
			log.error("sendEmailExceedsPrinting(): failed", e);
		}
	}

	/**
	 *
	 * @param product  Must be non-null
	 * @param contract  Must be non-null
	 */
	private HtmlEmail getEmailAddressesFromBusinessUnit(Product product, Contract contract) {
		HashSet<String> emap = new HashSet<String>();

		HtmlEmail htmlEmail = null;
		try {
			htmlEmail = new HtmlEmail();
			htmlEmail.setHostName(getSmtpServer());
			htmlEmail.setFrom(getMessageSource().getMessage("email.from.admin", null, null));
		}  catch (Exception e) {
			log.error("getEmailAddressesFromBusinessUnit(): failed", e);
		}

		// handle non covers - aka internal assets
		if (contractRepository.contractContainsInternalAssets(contract.getId())) {
			getEmailAddressesFromBusinessUnitInner(product, emap, htmlEmail, false);
		}

		// handle covers - aka external assets
		if (contractRepository.contractContainsExternalAssets(contract.getId())) {
			getEmailAddressesFromBusinessUnitInner(product, emap, htmlEmail, true);
		}

		log.debug("getEmailAddressesFromBusinessUnit(): toAddresses = "
			+ StringUtils.join(htmlEmail.getToAddresses(), "\r\n"));
		return htmlEmail;
	}

	private void getEmailAddressesFromBusinessUnitInner(Product product, HashSet<String> emap, HtmlEmail htmlEmail, boolean cover) {
		// product.business_unit is non-nullable so we should always get one
		BusinessUnit bu = product.getBusinessUnit();

		String [] emailArray;

		// get hard coded e-mails first
		String emails = bu.getEmail();
		if (StringUtils.isNotBlank(emails) && !emails.equals("none")) {
			emailArray = emails.split(",");
			for (String email : emailArray) {
				try {
					if (!emap.contains(email)) {
						emap.add(email);
						htmlEmail.addTo(email);
					}
				} catch (Exception e) {
					log.warn("getEmailAddressesFromBusinessUnitInner(): caught exception: ", e);
				}
		  	}
		}

		// process roles
		if (cover) {
			emails = bu.getWarnRolesCovers();
		}
		else {
			emails = bu.getWarnRoles();
		}
		if (StringUtils.isNotBlank(emails) && !emails.equals("none")) {
			emailArray = emails.split(",");
			for (String email : emailArray) {
				for (int y = 0; y < product.getUsers().size(); y++) {
					if (product.getUsers().get(y).getRole().getCode().equals(email)) {
						String demail = product.getUsers().get(y).getUser().getEmail();
						if (null != demail) {
							try {
								if (!emap.contains(demail)) {
									emap.add(demail);
									htmlEmail.addTo(demail);
								}
							} catch (Exception e) {
								log.warn("getEmailAddressesFromBusinessUnitInner(): caught exception: ", e);
							}
						}
					}
				}
			}
		}

		// process last updated user
		boolean warnLastUpdate = cover ? bu.getWarnLastUpdateUserCovers() : bu.getWarnLastUpdateUser();
		if (warnLastUpdate) {
			try {
				List<CwHistory> list = cwRepository.getHistory(product.getCommonWork().getId(), 1);
				if (list.size() > 0) {
					String email = list.get(0).getLastUpdatedUser().getEmail();
					if (!emap.contains(email)) {
						emap.add(email);
						htmlEmail.addTo(email);
					}
				}
			} catch (Exception e) {
				log.warn("getEmailAddressesFromBusinessUnitInner(): caught exception: ", e);
			}
		}
	}

	@Override
	public Component createUpdateComponentFromMessage(Component component) throws Exception
	{
		// first check if the product referenced by the Component exists
		// - if it does not, then send a message to request the product
		// and send another message to get this component again
		// (would be nicer to re-queue the current msg for processing but we
		// no longer have a handle to the message at this point - possible
		// redesign?)
		String productExternalId = component.getCommonWork().getPrimaryProduct().getExternalId();
		Product product = productRepository.loadByExternalId(productExternalId);
		if (product == null) {
			// get product from PE
			// TODO: Don't hard code US DataSource
			product = productService.refreshProduct(productExternalId, DataSource.US.getCode(), true);
			// throws Exception
		}
		component = cwRepository.savePartialEntity(component);
		return component;
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public void deleteCWFromUserWatch(int userId, String productExternalId)
			throws PermissionsSecurityException, ServiceException, PersistenceException
	{
		// smarkoff: This works but I expected the single SQL stmt below is more efficient
		//Product product = productRepository.loadByExternalId(productExternalId);
		//if (product != null) {
		//    User user = cwRepository.find(User.class, userId);
		//    user.getWatchedCommonWorks().remove(product.getCommonWork());
		//    cwRepository.merge(user);
		//}

		String sql = "delete from watched_cw where user_id = ? and cw_id = "
			+ "(select cw_id from product p where p.external_id = ?)";
		Query q = productRepository.createNativeQuery(sql);
		q.setParameter(1, userId);
		q.setParameter(2, productExternalId);
		q.executeUpdate();
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void addCWToUserWatch(int userId, int cwId)
			throws PermissionsSecurityException, ServiceException, PersistenceException
	{
		User user = cwRepository.find(User.class, userId);
		CommonWork commonWork = cwRepository.find(CommonWork.class, cwId);

		// smarkoff (9/27/2010): Check if already in list because if try to add again then the
		// resulting constraint violation error for some reason causes future
		// db operations to fail.
		List<CommonWork> watchedCommonWorks = user.getWatchedCommonWorks();
		if (!watchedCommonWorks.contains(commonWork)) {
			watchedCommonWorks.add(commonWork);
			cwRepository.persist(user);
		}
	}

	/**
	 * @param userId
	 * @param externalId  Must be non-blank
	 * @param dataSource  Must be non-blank
	 */
	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public void addProductToUserWatch(int userId, String externalId, String dataSource)
			throws PersistenceException, PermissionsSecurityException, ServiceException
	{
		ArgUtil.notBlank(externalId, "externalId");
		ArgUtil.notBlank(dataSource, "dataSource");

		Product product = productService.loadByExternalIdWithRefresh(externalId, dataSource);
		if (null != product) {

			addCWToUserWatch (userId, product.getCommonWork().getId());
		}
		else {
			String msg = "Could Not Find Product From PE";
			throw new ServiceException (msg);
		}
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public void saveCwFile(CwFile pFile, int cwId) throws Exception
	{
		CommonWork commonWork = cwRepository.loadCWById(cwId);
		pFile.setCommonWork(commonWork);

		cwRepository.merge(pFile);
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public void toggleWatched(int userId, int cwId) throws ServiceException, PermissionsSecurityException, PersistenceException
	{
		CommonWork cw = cwRepository.loadWithPrimaryProductById(cwId);
		if (cwRepository.isCwWatched(userId, cwId))
			deleteCWFromUserWatch(userId, cw.getPrimaryProduct().getExternalId());
		else
			addCWToUserWatch(userId, cwId);
	}

	/**
	 * Returns a non-null message if the required operation is not allowed at this time or is allowed only because the user has the Admin role.
	 */
	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public String toggleCWStatus(int cwId, String cwStatusCode, boolean interior, boolean isAdmin) throws Exception
	{
		//CommonWork cw = cwRepository.loadCWById(cwId);
		// note we index below
		CommonWork cw = cwRepository.loadByIdForProductIndex(cwId);  // throws Exception

		if (cw == null) { // We don't expect this to happen but check
			throw new PersistenceException("commonWorkId " + cwId + " not found!");
		}

		boolean isNoPermRequiredCapable = cwRepository.isNoPermRequiredCapable(cwId, interior);
		boolean isCompleteCapable = cwRepository.isCompleteCapable(cwId, interior);

		CommonWorkStatus cwStatus = CommonWorkStatus.getCommonWorkStatusForCode(cwStatusCode);
		String returnMsg = null;
		if (cwStatus == CommonWorkStatus.COMPLETE && !isCompleteCapable) {
			returnMsg = "Complete cannot be selected unless\r\n"
				+ "all asset usages are Granted or Cancelled";
			if (isAdmin) {
				returnMsg = "Admin override: Normally " + returnMsg
					+ "\r\nbut since you are an Administrator this is allowed.";
			}
			else  return "Illegal Action: " + returnMsg;
		}
		if (cwStatus == CommonWorkStatus.NO_PERMISSIONS_REQUIRED && !isNoPermRequiredCapable) {
			return "Illegal Action: No Permissions Required cannot be\r\n"
				+ "selected unless all assets are not mananged.";
		}

		if (interior) {
			cw.setInteriorCWStatus(cwStatus);
		}
		else {
			cw.setCoverCWStatus(cwStatus);
		}
		cwRepository.merge(cw);
		//persist cover status to history
		log.debug("before updating cover status.. ");
		if(interior)
		{
			addToCwHistory( cwId, "Interior status updated to "+cwStatus.getDescription());
		}
		else
		{
			addToCwHistory( cwId, "Cover status updated to "+cwStatus.getDescription());
		}
		
		

		for (Product p : cw.getProducts()) {
			productIndexService.updateIndex(p);
		}

		return returnMsg;
	}

	@Override
	public void saveCwNotes(int cwId, String notes) throws PersistenceException {
		cwRepository.saveCwNotes(cwId, notes);
	}

	public List<Component> loadComponentList(int cwId, boolean includeCovers) {
		return cwRepository.loadComponentList(cwId, includeCovers);
	}

	@Override
	public List<CwPhotoEstimate> loadPhotoEstimates (int cwId) {
		return cwRepository.loadPhotoEstimates(cwId);
	}

	@Override
	public void copyAssets(int newCWId, int origCWId, List<Integer> auIds,
			boolean edition, boolean includeUsage, String sessionId, boolean includeCovers, int userGroupId, boolean copyFlag)
		throws PersistenceException, MessageException, IOException
	{
		ImportAssetsStatus status = new ImportAssetsStatus();
		status.setTotalCount(auIds.size());

		copyAssetStatusMap.put(sessionId, status);

		new CopyAssetsThread (newCWId, origCWId, auIds, edition, includeUsage, sessionId, includeCovers, ThreadLocalUser.get().getId(), userGroupId, copyFlag)
			.start();
	}

	@Override
	public ImportAssetsStatus getCopyAssetsStatus(String sessionId) {
		return copyAssetStatusMap.get(sessionId);
	}

	@Override
	public void deleteCopyAssetsStatus(String sessionId) {
		copyAssetStatusMap.remove(sessionId);
	}

	@Override
	public void setInteriorCWStatus(CommonWork cw, CommonWorkStatus cwStatus) {
		cwRepository.setInteriorCWStatus(cw, cwStatus);
	}


	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public void saveCwSummary(CwSummary cwSummary) throws PersistenceException {
		cwRepository.merge(cwSummary);
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public void saveCwPhotoEstimate(CwPhotoEstimate cwPhotoEstimate) throws PersistenceException {
		cwRepository.merge(cwPhotoEstimate);
	}


	public class CopyAssetsThread extends Thread {

		private final Integer newCWId;
		private final Integer origCWId;
		private final List<Integer> auIds;
		private final boolean edition;
		private final boolean includeUsage;
		private final String sessionId;
		private final boolean includeCovers;
		private final int userId;
		private final int userGroupId;
		private final boolean copyFlag;

		public CopyAssetsThread(int newCWId, int origCWId, List<Integer> auIds,
			boolean edition, boolean includeUsage, String sessionId, boolean includeCovers, int userId, int userGroupId, boolean copyFlag)
		{
			this.newCWId = newCWId;
			this.origCWId = origCWId;
			this.auIds = auIds;
			this.edition = edition;
			this.includeUsage = includeUsage;
			this.sessionId = sessionId;
			this.includeCovers = includeCovers;
			this.userId = userId;
			this.userGroupId = userGroupId;
			this.copyFlag = copyFlag;
		}

		@Override
		@Transactional
		public void run() {
			// set a ThreadLocalUser
			UserPrincipal userPrincipal = new UserPrincipal();
			userPrincipal.setId(userId);
			ThreadLocalUser.set(userPrincipal);  // so correct userId saved for lastUpdatedUserId

			ImportAssetsStatus status = copyAssetStatusMap.get(sessionId);
			try {
				// copy all components from previous edition if there are no components
				// else copy only components associated with copied assets in attachAssetToCW
				log.debug("newCWId "+newCWId+"includeCovers "+includeCovers);
				List<Component> components = loadComponentList(newCWId, includeCovers);
				if (CollectionUtils.isEmpty(components)) {
					log.debug("origCWId "+origCWId+"includeCovers "+includeCovers);
					cwRepository.copyComponents(newCWId, loadComponentList(origCWId, includeCovers));
				}

				// we get all the new assets in this list and we send the messages on a
				// separate Thread
				List<AssetUse> auList = new ArrayList<AssetUse>();
				log.debug("userGroupId "+userGroupId);
				for (Integer auId : auIds) {
					try {
						log.debug("newCWId "+newCWId);
						log.debug("origCWId "+origCWId);
						log.debug("auId "+auId);
						log.debug("edition "+edition);
						log.debug("includeUsage "+includeUsage);
						log.debug("userGroupId "+userGroupId);
						log.debug("$$$$$$copyFlag "+copyFlag);

						AssetUse copy = assetUseService.copyAsset(newCWId, origCWId, auId, edition, includeUsage, userGroupId, copyFlag);
						// lnagy - ignore the disabled ones
						//Changes made for SS Task 9
						status.setProcessedCount(status.getProcessedCount() + 1);
						if (null != copy)
							auList.add(copy);
					}
					catch (Exception e) {
						log.warn("copyAssets(): caught exception calling copyAsset(): ", e);
					}
				}
				assetUseService.sendUpdateAssetUseMessages(auList, null);

				// no longer needed - delete later
				//if (CollectionUtils.isNotEmpty(auList)) {
				//	AssetUse lastAu = auList.get(auList.size() - 1);
				//	assetUseIndexService.waitForAssetUseCreateUpdate(lastAu.getId(), lastAu.getLastUpdatedDate());
				//}
			}
			catch (Exception e) {
				log.debug("Exception while copying the assets", e);
				status.setStatusOK (false);
				status.setErrorMessage(e.getMessage());
			}

			log.info("run(): Thread done.");
		}
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public boolean hasProfile(Integer cwId) throws PersistenceException
	{
		CommonWork cw = cwRepository.loadCWById(cwId);
		return CollectionUtils.isNotEmpty(cw.getConditions());
	}

	/****************************************************
	 * VIEWS LOAD METHODS							    *
	 * (// preload stuff needed by Controller and jsp) 	*
	 ****************************************************/

	/**
	 * @param cwId
	 * @return
	 * @throws PersistenceException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 */
	@Override
	// TODO: This could probably be optimized a bit using fetch statement(s)
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public CommonWork loadCWLandingView(Integer cwId) throws PersistenceException
	{
		CommonWork cw = cwRepository.loadWithPrimaryProductById (cwId);
		if (cw == null)  return null;

		cw.getPoCount();

		for (Condition cond : cw.getConditions()) {
			cond.getValue();
		}

		Product p = cw.getPrimaryProduct();
		productService.loadCWLandingView(p.getId());

		p.getAuthorsAsString();

		return cw;
	}

	/**
	 * load the common work counts view
	 * @param int cwId
	 * @param includeCovers
	 * @return AssetSummaryView
	 * @throws Exception
	 */

	private void addToCwHistory(Integer cwId, String description) throws PersistenceException {
		CwHistory history = new CwHistory();
		history.setCwId(cwId);
		history.setDescription(description);
		cwRepository.persist(history);
	}

	/**
	 * (This method is currently not used - using trigger instead.)
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public void addToCwHistory_productImport(Integer cwId) throws PersistenceException {
		addToCwHistory(cwId, "Project created");
	}

	/**
	 * This method is currently not used - using trigger instead.
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public void addToCwHistory_assetCreateUpdate(Integer cwId) throws PersistenceException {
		if (!existingCwHistoryForAssets(cwId)) {
			addToCwHistory(cwId, "Asset(s) added/modified");
		}
	}

	/**
	 * This method is currently not used - only called by unused method above.
	 */
	private boolean existingCwHistoryForAssets(Integer cwId) {
		final String sql = "select count(*) as count from cw_history where cw_id = ? "
				+ " and description = 'Asset(s) added/modified' "
				//+ " and user_id = ? "
			       + " and datediff(last_updated_date, current_timestamp) = 0";
		Query query = cwRepository.createNativeQuery(sql, "scalarCount");
		query.setParameter(1, cwId);

		// For count, DB2 and MS SQL Server return Integer but MySQL returns BigInteger
		// Integer and BigInteger inherit from Number
		Number resultNum = (Number) query.getSingleResult();
		return resultNum.intValue() > 0;
	}

	/**
	 * (This method IS used.)
	 */
	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public void addToCwHistory_assetUseDelete(Integer cwId) throws PersistenceException {
		addToCwHistory(cwId, "Asset deleted");
	}

	/**
	 * This method should really be in PEMessageService but moved here just to create a transaction.
	 */
	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public Date getLastUpdate(String objectType, DataSource dataSource) {
		String sql = "select last_update as date from pe_update where object_type = ? and data_source = ?";
		Query query = productRepository.createNativeQuery(sql, "scalarDate");
		query.setParameter(1, objectType);
		query.setParameter(2, dataSource.getCode());
		try {
			return (Date) query.getSingleResult();
		}
		catch (NoResultException ex) {
			sql = "insert into pe_update (object_type, data_source, last_update) values (?, ?, ?)";
			query = productRepository.createNativeQuery(sql);
			query.setParameter(1, objectType);
			query.setParameter(2, dataSource.getCode());
			// 0 (=1970.01.01) would really be correct but it will cause large requests from PE and annoy Bruce so don't do it
			// instead make the date 30 days in the past
			//long time = 0;
			long time = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
			Date date = new Date(time);
			query.setParameter(3, date);
			int rows = query.executeUpdate();
			if (rows != 1) {
				log.warn("getLastUpdate(): insert did not return expected row count of 1 for objectType ["
					+ objectType + "] dataSource [" + dataSource.getCode() + "]");
			}
			return date;
		}
	}

	/**
	 * This method should really be in PEMessageService but moved here just to create a transaction.
	 */
	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public void setLastUpdate(Date date, String objectType, DataSource dataSource) {
		String sql = "update pe_update set last_update = ? where object_type = ? and data_source = ?";
		Query query = productRepository.createNativeQuery(sql);
		query.setParameter(1, date);
		query.setParameter(2, objectType);
		query.setParameter(3, dataSource.getCode());
		int rows = query.executeUpdate();
		if (rows != 1) {
			log.warn("setLastUpdate(): update did not return expected row count of 1 for objectType ["
				+ objectType + "] dataSource [" + dataSource.getCode() + "]");
		}
	}
	
	/**
	 * Added by santhosh for ISBN search from PE or not.
	 */
	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public void updatePEStatus(String status) {
		String sql = "update pe_update set pe_status = ? where object_type = ? and data_source = ?";
		Query query = productRepository.createNativeQuery(sql);
		Integer sts = Integer.parseInt(status);
		query.setParameter(1, sts);
		query.setParameter(2, "Author");
		query.setParameter(3, "US");
		int rows = query.executeUpdate();
		if (rows != 1) {
			log.warn("updatePEStatus(): update did not return expected row count of 1 for objectType [Author"
				 + "] dataSource [US]");
		}
	}
	
	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public boolean getPEUpdateStatus(String objectType, DataSource dataSource) {
		String sql = "select pe_status as status from pe_update where object_type = ? and data_source = ?";
		Query query = productRepository.createNativeQuery(sql);
		query.setParameter(1, objectType);
		query.setParameter(2, dataSource.getCode());
		return (Boolean) query.getSingleResult();
		
	}
	//end added by santhosh

	/**
	 * Add an author. Affects the AUTHOR_2_CW table. Does NOT effect PE author data.
	 * @throws PersistenceException
	 */
	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public User addAuthorIfNotIncluded(int cwId, User author) throws PersistenceException {
		CommonWork cw = cwRepository.find(CommonWork.class, cwId);  // throws PersistenceException
		boolean alreadyIncluded = false;
		for (User u : cw.getAuthors()) {
			if (u.getId().equals(author.getId())) {
				alreadyIncluded = true;
				break;
			}
		}
		if (!alreadyIncluded) {
			cw.getAuthors().add(author);
			author = cwRepository.merge(author);  // throws PersistenceException
			sendEmailAuthorAddedToCW(author.getEmail(), cw.getPrimaryProduct().getTitle());
		}
		return author;
	}

	/**
	 * Will create the common work default conditions
	 * @param cwId
	 * @return boolean true if the conditions have been initialized
	 * @throws Exception
	 */
	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public boolean initDefaultConditions(int cwId) throws Exception {
		CommonWork commonWork = cwRepository.loadCWById(cwId);
		if (commonWork.isConditionsInit()) {
			return true;
		} else {
			try {
				conditionRepository.createDefaultCWConditions(cwId);
				// set the flag to be true
				commonWork.setConditionsInit(true);
				// 10 years
				commonWork.setMinGrantYears(10);
				cwRepository.persist(commonWork);
				return true;
			} catch (Exception e) {
				log.debug("initDefaultConditions(): failed to init conditions", e);
				return false;
			}
		}
	}
//	Start	: Added for DM-533
	/* Validate and report following warning if the spreadsheet being uploaded has multiple (>1) rows for an asset
	   with the same Source name, same Source Ref. Number (i.e., asset ID),and same Invoice Number
	   and all the rows have zero value in the “Cost of Asset” column */
	@Override
	public void validateCostDuplicationAssetWarning (List<ExtendedAssetUse> validList, StringBuilder errorMessage, ImportAssetsStatus status) {
		log.debug("Inside validateCostDuplicationAssetWarning ");
		int warnings = 0;
		LinkedHashMap<SrcSrcRefInvCombination, Integer> srcSrcRefInvCombinationMap = new LinkedHashMap<SrcSrcRefInvCombination, Integer>();
		LinkedHashMap<SrcSrcRefInvCombination, Integer> origCostWarningMap = new LinkedHashMap<SrcSrcRefInvCombination, Integer>();
		int count = 0;
		int origCostCount = 0;
		String excelSrcName = "";
		String excelSrcRefNo = "";
		String excelInvNo = "";

		for (ExtendedAssetUse forinvoiceClassTest : validList) {
			excelSrcName = forinvoiceClassTest.getGetSourceName() == null ?  "" : forinvoiceClassTest.getGetSourceName();
			excelSrcRefNo = forinvoiceClassTest.getAssetSourceVendorId() == null ? "" : forinvoiceClassTest.getAssetSourceVendorId();
			excelInvNo = forinvoiceClassTest.getContractNoStr() == null ? "" : forinvoiceClassTest.getContractNoStr() ;


			if (!excelSrcName.isEmpty() && !excelSrcRefNo.isEmpty()	&& !excelInvNo.isEmpty()) {
				if (srcSrcRefInvCombinationMap.containsKey(new SrcSrcRefInvCombination(excelSrcName,excelSrcRefNo, excelInvNo))) {
					if ((forinvoiceClassTest.getContractPrice() == null) || (forinvoiceClassTest.getContractPrice() != null
							&& Double.parseDouble(forinvoiceClassTest.getContractPrice()) == 0)) {
						// increase the count value by 1 if the cost of an asset
						// has null value or 0
						count = srcSrcRefInvCombinationMap.get(new SrcSrcRefInvCombination(excelSrcName,excelSrcRefNo, excelInvNo)) + 1;
						srcSrcRefInvCombinationMap.put(new SrcSrcRefInvCombination(excelSrcName,excelSrcRefNo, excelInvNo), count);
					}
					origCostCount = origCostWarningMap.get(new SrcSrcRefInvCombination(excelSrcName,excelSrcRefNo, excelInvNo)) + 1;
					origCostWarningMap.put(new SrcSrcRefInvCombination(excelSrcName, excelSrcRefNo, excelInvNo),origCostCount);
				} else {
					if ((forinvoiceClassTest.getContractPrice() == null) || (forinvoiceClassTest.getContractPrice() != null &&
							Double.parseDouble(forinvoiceClassTest.getContractPrice()) == 0)) {
						// set the count value to 1 if the cost of an asset has
						// null value or 0
						srcSrcRefInvCombinationMap.put(new SrcSrcRefInvCombination(excelSrcName,excelSrcRefNo, excelInvNo), 1);
					}
					if (origCostWarningMap != null && !origCostWarningMap.isEmpty() &&
							origCostWarningMap.containsKey(new SrcSrcRefInvCombination(excelSrcName, excelSrcRefNo,excelInvNo))) {
						origCostCount = origCostWarningMap.get(new SrcSrcRefInvCombination(excelSrcName,excelSrcRefNo, excelInvNo)) + 1;
					} else {
						origCostCount = 1;
					}
					origCostWarningMap.put(new SrcSrcRefInvCombination(excelSrcName, excelSrcRefNo, excelInvNo),origCostCount);
				}
			}
		}



		for (Map.Entry<SrcSrcRefInvCombination, Integer> warnCombimap : origCostWarningMap.entrySet()) {
			if (warnCombimap.getValue() > 1) {
				for (Map.Entry<SrcSrcRefInvCombination, Integer> innerCombimap : srcSrcRefInvCombinationMap.entrySet()) {
					if (innerCombimap.getKey().equals(warnCombimap.getKey()) && innerCombimap.getValue().equals(warnCombimap.getValue())) {
						// warnCombimap.getValue() > 1
						warnings += 1;
						if (warnings == 1) {
							status.setStatusOK(false);
							errorMessage.append("<br/><b>Warning:</b> All records of following multi-use asset(s) have zero value in the “Cost of Asset” column. On import these assets will be created with zero invoice amount: <br/>");
						}
						String assetWarningMsg = getMessageSource().getMessage(
								"product.import.assets.warnmsg",
								new Object[] { warnings,
										warnCombimap.getKey().getSrcRefNo(),
										warnCombimap.getKey().getSrcName(),
										warnCombimap.getKey().getInvNo() },
								null);
						errorMessage.append(assetWarningMsg);
						log.debug("Inside validateCostDuplicationAssetWarning <br/>"+ assetWarningMsg);
					}
				}

			}
		}
		status.setErrorMessage(errorMessage.toString());
	}


	/* Validate and report an error if the spreadsheet being uploaded has multiple (>1) rows for an asset
	 with the same Source name, same Source Ref. Number (i.e., asset ID), and same Invoice Number
	 and more than one of these rows has a non-zero value in the “Cost of Asset” column.*/
	@Override
	public void validateCostDuplicationAssetError(List<ExtendedAssetUse> validList, StringBuilder errorMessage,ImportAssetsStatus status) {
		log.debug("Inside validateCostDuplicationAssetError ");
		int failures = status.getFailureCount();
		LinkedHashMap<SrcSrcRefInvCombination, Integer> srcSrcRefInvCombinationMap = new LinkedHashMap<SrcSrcRefInvCombination, Integer>();
		LinkedHashMap<SrcSrcRefInvCombination, String> positionsrcSrcRefInvCombinationMap = new LinkedHashMap<SrcSrcRefInvCombination, String>();
		int count = 0;
		String excelSrcName = "";
		String excelSrcRefNo = "";
		String excelInvNo = "";
		String excelPosNo = "";
		for (ExtendedAssetUse forinvoiceClassTest : validList) {
			excelSrcName = forinvoiceClassTest.getGetSourceName() == null ? "" : forinvoiceClassTest.getGetSourceName();
			excelSrcRefNo = forinvoiceClassTest.getAssetSourceVendorId() == null ? "" : forinvoiceClassTest.getAssetSourceVendorId();
			excelInvNo = forinvoiceClassTest.getContractNoStr() == null ? "" : forinvoiceClassTest.getContractNoStr();
			excelPosNo = forinvoiceClassTest.getStrPosition() == null ? "" : forinvoiceClassTest.getStrPosition();

			if (srcSrcRefInvCombinationMap.containsKey(new SrcSrcRefInvCombination(excelSrcName,excelSrcRefNo, excelInvNo))) {
				if ((forinvoiceClassTest.getContractPrice() != null && Double.parseDouble(forinvoiceClassTest.getContractPrice()) > 0)) {
					// increase the count value by 1 if the cost of an asset is
					// more than 1
					count = srcSrcRefInvCombinationMap.get(new SrcSrcRefInvCombination(excelSrcName,excelSrcRefNo, excelInvNo)) + 1;
					srcSrcRefInvCombinationMap.put(new SrcSrcRefInvCombination(excelSrcName, excelSrcRefNo, excelInvNo), count);
				}
			} else {
				if ((forinvoiceClassTest.getContractPrice() != null && Double.parseDouble(forinvoiceClassTest.getContractPrice()) > 0)) {
					// set the count value to 1 if the cost of an asset is more
					// than 0
					srcSrcRefInvCombinationMap.put(new SrcSrcRefInvCombination(excelSrcName, excelSrcRefNo, excelInvNo), 1);
					// logic to add asset with same Source name, same Source
					// Ref. Number (i.e., asset ID), and same Invoice Number but
					// different position numbers.

					positionsrcSrcRefInvCombinationMap.put(new SrcSrcRefInvCombination(excelSrcName,excelSrcRefNo, excelInvNo), excelPosNo);
				}
			}
		}

		// Position number validation
		// check for an asset in a srcSrcRefInvCombinationMap, remove an error
		// asset from srcSrcRefInvCombinationMap
		// where position numbers are same for all the asset in a
		// combination(src, srcRefNo, invNo)
		// exit the loop if any of the combination map contains different
		// positions
		for (Entry<SrcSrcRefInvCombination, String> positionMap : positionsrcSrcRefInvCombinationMap.entrySet()) {
			int countOfMultipeCostAsset = srcSrcRefInvCombinationMap.get(positionMap.getKey());
			if (countOfMultipeCostAsset > 1) {
				boolean samePos = false;
				String positionValue = positionMap.getValue();
				for (ExtendedAssetUse forPositionCheck : validList) {
					excelSrcName = forPositionCheck.getGetSourceName() == null ? "" : forPositionCheck.getGetSourceName();
					excelSrcRefNo = forPositionCheck.getAssetSourceVendorId() == null ? "" : forPositionCheck.getAssetSourceVendorId();
					excelInvNo = forPositionCheck.getContractNoStr() == null ? "" : forPositionCheck.getContractNoStr();
					excelPosNo = forPositionCheck.getStrPosition() == null ? "" : forPositionCheck.getStrPosition();

					if (excelSrcName.equalsIgnoreCase(positionMap.getKey().getSrcName())
							&& excelSrcRefNo.equalsIgnoreCase(positionMap.getKey().getSrcRefNo())
							&& excelInvNo.equalsIgnoreCase(positionMap.getKey().getInvNo())) {
						if (positionValue.equalsIgnoreCase(excelPosNo)) {
							samePos = true;
						} else {
							samePos = false;
							break;
						}
					}
				}
				if (samePos) {
					srcSrcRefInvCombinationMap.remove(positionMap.getKey());
				}
			}
		}

		int errorMsgCount = failures;
		for (Map.Entry<SrcSrcRefInvCombination, Integer> errorCombimap : srcSrcRefInvCombinationMap
				.entrySet()) {
			if (errorCombimap.getValue() > 1) {
				// failures =failures + errorCombimap.getValue();
				// status.setFailureCount(failures);
				errorMsgCount = errorMsgCount + 1;
				status.getSrcSrcRefInvStatusMap().put(new SrcSrcRefInvCombination(errorCombimap.getKey().getSrcName(),
						errorCombimap.getKey().getSrcRefNo(), errorCombimap.getKey().getInvNo()), errorCombimap.getValue());
				String assetErrorMsg = getMessageSource().getMessage(
						"product.import.assets.errormsg",
						new Object[] { errorMsgCount,
								errorCombimap.getKey().getSrcRefNo(),
								errorCombimap.getKey().getSrcName(),
								errorCombimap.getKey().getInvNo() }, null);
				errorMessage.append(assetErrorMsg);
				/*
				 * errorMessage.append(failures + ". For multi-use asset \"" +
				 * errorCombimap.getKey().getSrcRefNo() + "\" from \"" +
				 * errorCombimap.getKey().getSrcName() +
				 * "\" and with invoice number \"" +
				 * errorCombimap.getKey().getInvNo() +
				 * "\" ,the invoice amount must be reported only once in the \"Cost of Asset\" column. <br/>"
				 * );
				 */
				log.debug("Inside validateCostDuplicationAssetError <br/> "+ assetErrorMsg);
			}
		}
		status.setFailureCount(errorMsgCount);
		status.setErrorMessage(errorMessage.toString());
	}

	// End	: Added for DM-533



	public String getSmtpServer() {
		return smtpServer;
	}

	public void setSmtpServer(String smtpServer) {
		this.smtpServer = smtpServer;
	}

	public CommonWorkRepository getCommonWorkRepository() {
		return cwRepository;
	}

	public void setCommonWorkRepository(CommonWorkRepository cwRepository) {
		this.cwRepository = cwRepository;
	}

	public CMSMessageService getOutgoingMessageService() {
		return outgoingMessageService;
	}

	public void setOutgoingMessageService(CMSMessageService outgoingMessageService) {
		this.outgoingMessageService = outgoingMessageService;
	}

	public UserRepository getUserRepository() {
		return userRepository;
	}

	public void setUserRepository(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public ProductService getProductService() {
		return productService;
	}

	public void setProductService(ProductService productService) {
		this.productService = productService;
	}

	public ProductRepository getProductRepository() {
		return productRepository;
	}

	public void setProductRepository(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}

	public AssetUseService getAssetUseService() {
		return assetUseService;
	}

	public void setAssetUseService(AssetUseService assetUseService) {
		this.assetUseService = assetUseService;
	}

	public AssetUseRepository getAssetUseRepository() {
		return assetUseRepository;
	}

	public void setAssetUseRepository(AssetUseRepository assetUseRepository) {
		this.assetUseRepository = assetUseRepository;
	}

	public AssetUseIndexService getAssetUseIndexService() {
		return assetUseIndexService;
	}

	public void setAssetUseIndexService(AssetUseIndexService assetUseIndexService) {
		this.assetUseIndexService = assetUseIndexService;
	}

	public ContractRepository getContractRepository() {
		return contractRepository;
	}

	public void setContractRepository(ContractRepository contractRepository) {
		this.contractRepository = contractRepository;
	}

	public void setProductIndexService(ProductIndexService service) {
		productIndexService = service;
	}

	public void setInternalUrl(String url) {
		this.internalUrl = url;
	}

	public void setAuthorUrl(String url) {
		this.authorUrl = url;
	}

	public ConditionRepository getConditionRepository() {
		return conditionRepository;
	}

	public void setConditionRepository(ConditionRepository conditionRepository) {
		this.conditionRepository = conditionRepository;
	}

	//	Start : Added for DM-534

	public String getSaveExcelFilesUrl() {
		return saveExcelFilesUrl;
	}

	public void setSaveExcelFilesUrl(String saveExcelFilesUrl) {
		this.saveExcelFilesUrl = saveExcelFilesUrl;
	}

	//	End : Added for DM-534

}
