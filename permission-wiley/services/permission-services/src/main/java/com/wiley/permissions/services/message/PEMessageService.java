package com.wiley.permissions.services.message;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.wiley.permissions.common.dispatcher.DispatcherException;
import com.wiley.permissions.common.dispatcher.OperationType;
import com.wiley.permissions.common.transformer.ObjectToXml;
import com.wiley.permissions.common.transformer.XmlToObject;
import com.wiley.permissions.domain.message.MasterList;
import com.wiley.permissions.domain.message.Message;
import com.wiley.permissions.domain.message.Message.MessageEntity;
import com.wiley.permissions.domain.message.Message.MessageType;
import com.wiley.permissions.domain.message.MessageException;
import com.wiley.permissions.domain.message.MessageOperation;
import com.wiley.permissions.domain.message.MessageOperationParameter;
import com.wiley.permissions.domain.message.RequiresDelayedProcessingException;
import com.wiley.permissions.domain.message.pe.UpdateProductNotificationMessage;
import com.wiley.permissions.domain.persistence.permissions.BusinessUnit;
import com.wiley.permissions.domain.persistence.permissions.CommonWork;
import com.wiley.permissions.domain.persistence.permissions.DataSource;
import com.wiley.permissions.domain.persistence.permissions.Product;
import com.wiley.permissions.domain.persistence.permissions.ProductError;
import com.wiley.permissions.domain.persistence.permissions.PublicationStatus;
import com.wiley.permissions.domain.util.UniqueIdentifierGenerator;
import com.wiley.permissions.persistence.PersistenceException;
import com.wiley.permissions.repositories.CommonWorkRepository;
import com.wiley.permissions.repositories.ProductRepository;
import com.wiley.permissions.repositories.UserRepository;
import com.wiley.permissions.services.CommonWorkService;
import com.wiley.permissions.services.ProductIndexService;
import com.wiley.sf.common.lang.ArgUtil;
import com.wiley.sf.common.lang.CollectionUtil;
import com.wiley.sf.common.xml.XSLTransform;

/**
 * @author ttidwell
 */
public class PEMessageService
		extends GenericMessageService {
	private final static Log log = LogFactory.getLog(PEMessageService.class);

	private ProductRepository productRepository;
	private ProductIndexService productIndexService;
	private CommonWorkRepository cwRepository;
	private CommonWorkService cwService;
	private UserRepository userRepository;
	private int defaultBunchLimit = 20;  // this is overridden by Spring config

	private String source = MessageEntity.PERMISSIONS.getEntity();

	/**
	 * Implements abstract GenericMessageService method.
	 *
	 * Make synchronized because we believe that situations were occurring
	 * where two threads were trying to update the same product/CW at the same
	 * time and this was causing problems.
     */
	@Override
	public synchronized void receiveMessage(Message message)
			throws RequiresDelayedProcessingException, MessageException, MuleException, DispatcherException
	{
		for (MessageOperation operation : message.getOperations()) {
			OperationType operationType = operation.getOperationType();

			log.info("receiveMessage(): Got A Message Of Type: " + operationType);

			switch (operationType) {
				case ALL_NEW_PRODUCTS: {
					// smarkoff: I talked to Bruce 9/2011 and he said that there is no such thing as
					// an allNewProduct Notification or Reply -- there is only a request - which he
					// doesn't see any point in us using (just use updateSince type of request instead).

					break;
				}

				case MASTER_LIST_UPDATE: {
					receiveMasterListUpdate(message);
					break;
				}

				case PRODUCT_UPDATE: {
					if (message.getType() == MessageType.NOTIFICATION) {
						receiveProductUpdateNotification(message, operation);
					}
					else {
						receiveProductUpdateReply(message, operation);
					}
					break;
				}

				default: {
					throw new MessageException("Operation Type " + operationType
						+ " not recognized by this handler.");
				}
			}
		}
	}

	private void receiveMasterListUpdate(Message message) throws MessageException, DispatcherException, MuleException {
		// A Master List Update can either be a notification or a reply.

		if (message.getType() == MessageType.NOTIFICATION) {
			List<Object> metadata = message.getMetadata();

			// We will take each masterList we are notified about
			// (may be one or multiple) and split into separate request messages
			// so that we can track the sent time of the reply separately for each
			// masterList name and so that the reply messages won't be too large.

			if (metadata.size() > 0) {
				// all objects in metadata are expected to be of class MasterList
				for (Object o : metadata) {
					MasterList masterList = (MasterList) o;
					if (masterList.isTypeWeNeed()) {
						DataSource dataSource = DataSource.forCode(masterList.getDataSource());
						if (!masterList.computeGlobal() || dataSource == DataSource.US) {
							Date lastUpdate = cwService.getLastUpdate(masterList.getName(), dataSource);
							Date updateTimeDate = new Date(masterList.getUpdateTime());

							// Could have same date compare issue here as in receiveProductUpdateReply()
							// but I don't think it makes much difference here - so for now not worrying
							// about it - but save commented out code below.

							//SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
							//boolean msgDateLater = updateTimeDate.after(lastUpdate);
							//log.debug("receiveMasterListUpdate(): compare dates (msg, our db): " + dateFormat.format(updateTimeDate)
							//		+ ", " + dateFormat.format(lastUpdate) + " msg date is later: " + msgDateLater);

							// To be a little conservative, if the two dates are the same time, then still make the request
							if (lastUpdate.after(updateTimeDate)) {
								log.info("receiveMasterListUpdate(): received a notification updateTime ["
									+ updateTimeDate + "] that was before our lastUpdate [" + lastUpdate + "] - msg id = " + message.getId());
							}
							else {
								masterList.setUpdatedSince(lastUpdate.getTime());
								// if don't remove updateTime (since we are reusing this object) then PE will complain
								masterList.setUpdateTime(null);
								requestMasterListUpdate(masterList, message.getId());  // throws MessageException
							}
						}
						else {
							log.info("receiveMasterListUpdate(): ignoring notification for MasterList of type ["
								+ masterList.getName() + "] dataSource [" + masterList.getDataSource()
								+ "] because it is a global type and non-US (redundant)");
						}
					}
					else {
						log.info("receiveMasterListUpdate(): ignoring notification for MasterList of type ["
							+ masterList.getName() + "] because we don't track this type");
					}
				}
			}
		}

		if (message.getType() == MessageType.REPLY) {
			// Direct call does not work.
			// - get java.lang.IllegalStateException: no transaction started on this thread
			// getProductService().saveMasterLists(message);

			getServiceDispatcher().send(OperationType.UPDATE_MASTER_LISTS, message, null);
		}
	}

	private void receiveProductUpdateNotification(Message message, MessageOperation operation) throws MessageException {
		List<Product> finalList = new ArrayList<Product>();

		for (Object tmp : operation.getItems()) {
			UpdateProductNotificationMessage productMessage = (UpdateProductNotificationMessage) tmp;
			Product product = productMessage.buildProductWithExternalIdAndDataSource();
			// we don't want any other fields set on the product because other fields in the message
			// can mess up acceptance/handling by PE

			try {
				boolean weCareAboutProduct = doWeCareAboutProduct(productMessage.getEditor(),
						productMessage.getCommonWork(), productMessage.getPublicationStatus(), productMessage.getCopyrightYear());
				log.debug("receiveProductUpdateNotification(): weCareAboutProduct = " + weCareAboutProduct);

				if (weCareAboutProduct) {
					 finalList.add(product);
				}
				else {
					// do NOT update the index now if we DO care about the product because
					// the index will get updated more completely from the full update message
					updateProductIndex(productMessage);
				}
			}
			catch (Exception e) {
				throw new MessageException("Caught exception from doWeCareAboutProduct(): ", e);
			}
		}

		if (finalList.size() > 0) {
			requestProductUpdate(finalList, message.getId());  // throws MessageException
		}
	}

	private void receiveProductUpdateReply(Message message, MessageOperation operation) throws MuleException, MessageException {
		List<Product> products = new ArrayList<Product>();
		int errorCount = 0;

		// Even though each product in the message has the completeRecordSet field,
		// the value of this field for all products in the message should be the same
		// and we assume it is.
		boolean completeRecordSet = false;
		for (Object item : operation.getItems()) {
			// ProductError is possible instead of Product
			if (item instanceof Product) {
				Product product = (Product) item;
				products.add(product);
				if (product.isCompleteRecordSet()) completeRecordSet = true;
			}

			if (item instanceof ProductError) {
				ProductError error = (ProductError) item;
				errorCount++;
				log.debug("receiveProductUpdateReply(): got back error: " + error);
				// if product not found in PE then delete from product index
				if ("1004".equals(error.getCode())) {
					String wid = error.getQueryValue();
					if (StringUtils.isNotBlank(wid)) {
						log.info("receiveProductUpdateReply(): going to delete WID [" + wid + "] from product index");
						productIndexService.deleteFromIndexByWID(wid);
					}
				}
			}
		}

		log.debug("receiveProductUpdateReply(): products.size() = " + products.size()
				+ ", # productErrors = " + errorCount);
		log.debug("receiveProductUpdateReply(): completeRecordSet = " + completeRecordSet);

		if (products.size() == 0) return;

		if (completeRecordSet) {
			try {
				getServiceDispatcher().send(OperationType.UPDATE_PRODUCTS, products, null);
			}
			catch (DispatcherException e) {
				throw new MessageException("Could Not Call Service", e);
			}
		}
		else {
			List<Product> requestList = new ArrayList<Product>();
			for (Product product : products) {
				try {
					String cwCode = product.getCommonWork() == null ? null : product.getCommonWork().getCode();
					boolean weCareAboutProduct = doWeCareAboutProduct(product.getExternalId(), cwCode,
							product.getPublicationStatus().getCode(), product.getCopyrightYear());
					log.debug("receiveProductUpdateReply(): REPLY weCareAboutProduct = " + weCareAboutProduct);
					Product dbProduct = getProductRepository().loadByExternalId(product.getExternalId());  // throws PersistenceException

					if (weCareAboutProduct) {
						boolean saveProduct = false;

						if (dbProduct == null)  saveProduct = true;
						else {
							saveProduct = product.getLastUpdatedDate().after(dbProduct.getLastUpdatedDate());
							SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
							log.debug("receiveProductUpdateReply(): compare dates (msg, our db): " + dateFormat.format(product.getLastUpdatedDate())
									+ ", " + dateFormat.format(dbProduct.getLastUpdatedDate()) + " msg date is later: " + saveProduct);
						}

						if (saveProduct) {
							log.debug("receiveProductUpdateReply(): will request full update for [" + product.getExternalId() + "].");
							// use new empty product to avoid other fields becoming part of message
							Product holder = new Product();
							holder.setExternalId(product.getExternalId());
							holder.setDataSource(product.getDataSource());
							requestList.add(product);
						}
						else {
							log.debug("receiveProductUpdateReply(): will NOT request full update for [" + product.getExternalId() + "].");
						}
					}
				}
				catch (Exception ex) {
					log.warn("receiveProductUpdateReply(): caught exception processing reply where NOT completeRecordSet, externalId ["
						+ product.getExternalId() + "]", ex);
				}
			} // end for
			requestProductUpdate(requestList, message.getId());  // will spread across messages in bunches
		} // end else
	}

	/**
	 * Returns true if the product has a pre-production or in-production/published status,
	 * or if we already have the product in our database, or we already
	 * have another product for the same common work in our database.
	 *
	 * @param externalId  Should be non-blank
	 * @param cwCode  May be blank
	 * @param pubStatus  Should be non-blank
	 * @param copyrightYear  May be null
	 * @throws PersistenceException
	 */
	private boolean doWeCareAboutProduct(String externalId, String cwCode, String pubStatus, Integer copyrightYear) throws PersistenceException {
		if (PublicationStatus.doWeCareAboutProduct(pubStatus, copyrightYear))  return true;
		//if (System.currentTimeMillis() > 0) return true;

		Product test = getProductRepository().loadByExternalId(externalId);  // throws PersistenceException
		if (test != null) return true;

		// if the product does not exist but is part of an existing CommonWork
		if (StringUtils.isBlank(cwCode))  return false;

		CommonWork cw = cwRepository.loadByCodeWithProductCount(cwCode);  // throws PersistenceException
		// We will most likely already have the CW do to MasterList updates
		// so check if we have at least one product for the CW.
		return (cw != null && cw.getProductCount() > 0);
	}

	private void updateProductIndex(UpdateProductNotificationMessage msg) {
		log.debug("updateProductIndex(): message = " + msg);

		List<String> authorNames = authorCodesToNames(msg.getAuthorCodes());

		// smarkoff: Actually I believe businessUnit is always non-null
		String businessUnitName = null;
		if (msg.getBusinessUnit() != null) {
			businessUnitName = BusinessUnit.forCode(msg.getBusinessUnit()).getName();
		}

		productIndexService.updateIndex(msg.getExternalId(),
			msg.getCommonWork(), null, null, msg.getLastUpdatedDate(), msg.getIsbn13(), msg.getIsbn10(),
			msg.getPnumber(), msg.getTitle(), msg.getShortAuthorName(), authorNames,
			msg.getEditionNumber(), msg.getPreviousEditionWID(), msg.getNextEditionWID(),
			msg.getPublicationStatus(), msg.getBusinessUnit(), businessUnitName, msg.getDataSource(),
			msg.getCopyrightYear(), null, null, null, null, null, null, null, msg.getMediumCode(),
			null, null, null, null, null, null, null, null, null, null, null, null, null,null,
			null, null, null, null, null, null);
	}

	/**
	 * Public so can be called from ProductIndexService.indexProductData(InputStream)
	 * as well as the updateProductIndex() method above.
	 */
	public List<String> authorCodesToNames(List<String> authorCodes) {
		if (CollectionUtils.isEmpty(authorCodes))  return null;

		List<String> authorNames = new ArrayList<String>(authorCodes.size());
		for (String code : authorCodes) {
			// Don't load whole user object anymore - was much slower than loading just 2 columns we need
			// (whole object was 300 ms on smarkoff machine, 9 ms for just 2 columns)
			//User user = userRepository.loadByCode(code);
			String combinedName = userRepository.loadCombinedNameByCode(code);
			if (combinedName == null) {
				log.warn("updateProductIndex(): couldn't lookup user for code [" + code + "]");
			}
			else {
				authorNames.add(combinedName);
			}
		}

		return authorNames;
	}

	/**
	 * Returns a new message with basic fields pre-filled. (id, source, direction, sent, protocolVersion, and
	 * target)
	 */
	@Override
	public Message createMessage() {
		Message msg = super.createMessage();

		// overwrite the source with value from spring config file
		msg.setSource(Message.MessageEntity.valueOf(getSource()));
		msg.setType(MessageType.REQUEST);

		List<MessageEntity> targetList = new ArrayList<MessageEntity>();
		targetList.add(MessageEntity.PRODUCT_ENGINEERING);
		msg.setTargets(targetList);

		return msg;
	}

	/**
	 * Will create a reply to a productUpdate REQUEST message (simulate PE) Used in ProductResource (REST
	 * Service) It needs Transactional because it loads the product and then does XML serialization
	 *
	 * @param msg
	 * @return Message
	 * @throws JAXBException
	 * @throws XMLStreamException
	 * @throws PersistenceException
	 */
	@Transactional
	public String createProductUpdateReplyMessage(Message msg) throws JAXBException, XMLStreamException, PersistenceException
	{
		String externalId;
		String isbn10;
		String isbn13;

		try {
			log.debug("createProductUpdateMessage() :" + msg.getFirstOperation());
			List<Object> items = msg.getFirstOperation().getItems();
			Product product = (Product) items.get(0);
			externalId = product.getExternalId();
			isbn10 = product.getIsbn10();
			isbn13 = product.getIsbn13();
			log.debug("createProductUpdateMessage(): externalId " + externalId + " isbn10 " + isbn10 + " isbn13 " + isbn13);
		}
		catch (Exception e) {
			log.debug("createProductUpdateMessage(): failed to read product object from message", e);
			return null;
		}

		Product product = null;

		if (StringUtils.isNotBlank(externalId)) {
			product = getProductRepository().loadByExternalId(externalId);
		}

		if (StringUtils.isNotBlank(isbn10)) {
			product = getProductRepository().getProductByISBN(isbn10);
		}

		if (StringUtils.isNotBlank(isbn13)) {
			product = getProductRepository().getProductByISBN(isbn13);
		}

		if (product == null)
			return null;

		msg = createProductUpdateReplyMessage(product);

		return convertMessageToPEXml (msg, "/xsl/permissions-reply-to-pe(restlet).xsl");
	}

	/**
	 * creates a message that simulates the REPLY message from PE
	 * @param product
	 * @return
	 * @throws JAXBException
	 * @throws XMLStreamException
	 */
	private Message createProductUpdateReplyMessage(Product product) throws JAXBException, XMLStreamException
	{
		ArgUtil.notNull(product, "product");

		Message message = new Message();
		message.setId(UniqueIdentifierGenerator.getNextIdentifier());
		message.setType(MessageType.REPLY);
		message.setSource(MessageEntity.PRODUCT_ENGINEERING);
		message.getTargets().add(MessageEntity.PERMISSIONS);

		MessageOperation op = message.createMessageOperation(OperationType.PRODUCT_UPDATE);
		Product tmp = product;
		List<Object> products = new ArrayList<Object>();
		products.add(tmp);
		op.setItems(products);
		message.getOperations().add(op);

		return message;
	}

	/**
	 * If you have an list of products, use the method below that takes a list instead (more efficient).
	 *
	 * @param product  Must be non-null and have a non-blank dataSource
	 * @param replyId  May be null
	 *
	 * @throws MessageException
	 */
	public void requestProductUpdate(Product product, String replyId) throws MessageException {
		ArgUtil.notNull(product, "product");
		ArgUtil.notBlank(product.getDataSource(), "product.dataSource");

		List<Product> list = new ArrayList<Product>(1);
		list.add(product);
		requestProductUpdateInternal(list, replyId);  // throws MessageException
	}

	/**
	 * Request detailed information for the given products. If there are many products,
	 * will split into multiple messages with [defaultBunchLimit] products per message.
	 *
	 * @param products  Must be non-empty
	 * @param replyId  May be null
	 *
	 * @throws MessageException
	 */
	public void requestProductUpdate(List<Product> products, String replyId) throws MessageException {
		requestProductUpdate(products, replyId, defaultBunchLimit);
	}

	/**
	 *
	 * @param products  Must be non-empty
	 * @param replyId  May be null
	 * @param bunchLimit  0 means no limit
	 *
	 * @throws MessageException
	 */
	public void requestProductUpdate(List<Product> products, String replyId, int bunchLimit) throws MessageException {
		if (bunchLimit > 0) {
			List<List<Product>> listList = CollectionUtil.split(products, bunchLimit);
			for (List<Product> list : listList) {
				requestProductUpdateInternal(list, replyId);
			}
		}
		else {
			requestProductUpdateInternal(products, replyId);
		}
	}

	/**
	 * Requests detailed product information for the products listed.
	 * Will modify any product that has more than one identifier to only
	 * have one (setting the others to null) to satisfy PE request protocol.
	 * So it's important that calling methods know this.
	 * (smarkoff) As of 9/7/2012 I checked that this is not
	 * a problem for any calling method.
	 *
	 * @param products  Must be non-empty
	 * @param dataSource  Must be non-blank
	 * @param replyId  May be null
	 *
	 * @throws com.wiley.permissions.domain.message.MessageException
	 */
	private void requestProductUpdateInternal(List<Product> products, String replyId)
			throws MessageException
	{
		log.debug("requestProductUpdateInternal(): entered...");

		// get dataSource from first product which we will use for the overall data source
		// (but then the dataSource at the product level will override)
		ArgUtil.notEmpty(products, "products");
		Product first = products.get(0);
		String dataSource = first.getDataSource();
		ArgUtil.notBlank(dataSource, "dataSource");

		// check that only one identifier on product set because otherwise
		// Bruce's program will send back an error
		checkSingleId(products);

		Message message = createMessage();

		// overwrite the source with value from spring config file
		message.setSource(Message.MessageEntity.valueOf(getSource()));

		if (replyId != null) {
			message.setReplyId(replyId);
		}

		MessageOperation op = message.createMessageOperation(OperationType.PRODUCT_UPDATE);

		List<MessageOperationParameter> msgOpParamList = new ArrayList<MessageOperationParameter>(1);
		MessageOperationParameter msgOpParam = new MessageOperationParameter();
		msgOpParam.setName("dataSource");
		msgOpParam.setValue(dataSource);
		msgOpParamList.add(msgOpParam);
		op.setParameters(msgOpParamList);

		List<Object> objectList = new ArrayList<Object>(products.size());
		objectList.addAll(products);

		op.setItems(objectList);

		message.getOperations().add(op);

		// This fails when called in Tomcat because the war file does not have
		// the pe-client jar with the permissions-request-to-pe.xsl
		//debugMessageToPEXml(message);

		sendMessage(message);
	}

	/**
	 * Makes sure exactly one id on the product is set, will change
	 * the product (setting extra ids to null) as needed.
	 * Priority is wid, isbn13, isbn10, pnumber.
	 *
	 * @param p  Must be non-null
	 */
	public void checkSingleId(Product p) {
		String wid = p.getExternalId();
		String isbn13 = p.getIsbn13();
		String isbn10 = p.getIsbn10();

		if (StringUtils.isNotBlank(wid)) {
			// these may or may not already be null
			p.setIsbn13(null);
			p.setIsbn10(null);
			p.setPnumber(null);
		}
		else if (StringUtils.isNotBlank(isbn13)) {
			// these may or may not already be null
			p.setIsbn10(null);
			p.setPnumber(null);
		}
		else if (StringUtils.isNotBlank(isbn10)) {
			// may or may not already be null
			p.setPnumber(null);
		}
	}

	/**
	 * Makes sure exactly one id on each product is set.
	 * Changes products as needed (setting extra ids to null).
	 *
	 * @param products  Must be non-null
	 */
	public void checkSingleId(List<Product> products) {
		for (Product p : products) {
			checkSingleId(p);
		}
	}

	public void requestMasterListUpdate(MasterList masterList) throws MessageException {
		requestMasterListUpdate(masterList, null);
	}

	/**
	 * @param masterList  Must be non-null
	 * @param replyId  May be null
	 */
	public void requestMasterListUpdate(MasterList masterList, String replyId)
			throws MessageException
	{
		List<Object> masterTableList = new ArrayList<Object>(1);
		masterTableList.add(masterList);
		requestMasterListUpdate(masterTableList, replyId);
	}

	/**
	 * This method requests detailed master list information for the MasterList's listed in reply to the message id
	 * given.
	 *
	 * @param masterTableList  Must be non-null
	 * @param replyId  May be null
	 *
	 * @throws com.wiley.permissions.domain.message.MessageException
	 */
	public void requestMasterListUpdate(List<Object> masterTableList, String replyId)
			throws MessageException
	{
		ArgUtil.notNull(masterTableList, "masterTableList");

		Message message = createMessage();

		// overwrite the source with value from spring config file
		message.setSource(Message.MessageEntity.valueOf(getSource()));

		if (replyId != null) {
			message.setReplyId(replyId);
		}

		MessageOperation op = message.createMessageOperation(OperationType.MASTER_LIST_UPDATE);

		// Note updatedSince should be set on the MasterList object and if so, will automatically
		// end up in the message (no need to add to param list here).
		// Same for dataSource.

		//List<MessageOperationParameter> msgOpParamList = new ArrayList<MessageOperationParameter>();
		//MessageOperationParameter msgOpParam = op.createMessageOperationParameter(
		//		"dataSource", dataSource.getCode());

		//msgOpParamList.add(msgOpParam);
		//op.setParameters(msgOpParamList);

		op.setItems(masterTableList);
		message.getOperations().add(op);
		debugMessageToPEXml(message);
		sendMessage(message);
	}

	/**
	 * Called by cron (and Test Message page).
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public void requestProductUpdateSinceLastCheck() {
		for (DataSource dataSource : DataSource.ALL) {
			try {
				Date currentDate = new Date();
				Date date = cwService.getLastUpdate("Product", dataSource);
				requestProductUpdateSince(date, dataSource);  // throws MessageException
				cwService.setLastUpdate(currentDate, "Product", dataSource);
			}
			catch (Exception ex) {
				log.error("requestProductUpdateSinceLastCheck(): caught exception: ", ex);
			}
		}
	}

	/**
	 * Called by cron (and Test Message page).
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public void requestMasterListUpdateSinceLastCheck() {
		for (MasterList ml : MasterList.ALL_WE_NEED) {
			if (ml.isGlobal()) {
				requestMasterListUpdateSinceLastCheck(ml, DataSource.US);
			}
			else {
				for (DataSource dataSource : DataSource.ALL) {
					requestMasterListUpdateSinceLastCheck(ml, dataSource);
				}
			}
		}
	}

	public void requestMasterListUpdateSinceLastCheck(MasterList ml, DataSource dataSource) {
		try {
			// Don't update the last_update field here - will do when receive the reply message
			//Date currentDate = new Date();
			Date date = cwService.getLastUpdate(ml.getName(), dataSource);
			ml.setUpdatedSince(date.getTime());
			ml.setDataSource(dataSource.getCode());
			requestMasterListUpdate(ml);
			//setLastUpdate(currentDate, ml.getName(), dataSource);
		}
		catch (Exception ex) {
			log.error("requestMasterListUpdateSinceLastCheck(): caught exception: ", ex);
		}
	}

	/**
	 * This method requests an update to product information for the given parameters.
	 *
	 * @param updatedSince  Must be non-null
	 * @param dataSource  May be null
	 * @throws com.wiley.permissions.domain.message.MessageException
	 */
	public void requestProductUpdateSince(Date updatedSince, DataSource dataSource)
			throws MessageException
	{
		ArgUtil.notNull(updatedSince, "updatedSince");

		Message message = createMessage();

		// overwrite the source with value from spring config file
		message.setSource(Message.MessageEntity.valueOf(getSource()));

		MessageOperation op = message.createMessageOperation(OperationType.PRODUCT_UPDATE);

		MessageOperationParameter param = op.createMessageOperationParameter(
				"updatedSince", String.valueOf(updatedSince.getTime()));
		op.getParameters().add(param);

		if (dataSource != null) {
			param = op.createMessageOperationParameter("dataSource", dataSource.getCode());
			op.getParameters().add(param);
		}

		// I think includeDetails and includeRelatedDeatils are "no" by
		// default, but will include these params anyway to make sure.
		param = op.createMessageOperationParameter("includeDetails", "no");
		op.getParameters().add(param);
		param = op.createMessageOperationParameter("includeRelatedData", "no");
		op.getParameters().add(param);

		message.getOperations().add(op);

		// This fails when called in Tomcat because the war file does not have
		// the pe-client jar with the permissions-request-to-pe.xsl
		//debugMessageToPEXml(message);

		sendMessage(message);
	}

	/**
	 * smarkoff: I don't think we use or need this method
	 * (We don't care about new products, unless the user does a search for
	 * one and then wants to import it. Although we do care about a new product
	 * for pre-imported common work - is this taken care of elsewhere?)
	 *
	 * @param updatedSince
	 * @param dataSource
	 * @throws com.wiley.permissions.domain.message.MessageException
	 */
	public void requestAllNewProducts(Date updatedSince, DataSource dataSource)
			throws MessageException
	{
		// TODO: smarkoff: 9/2011 Bruce said we might as well use the updateSince type request
		// instead of createdSince -- should probably change this.

		Message message = createMessage();

		// overwrite the source with value from spring config file
		message.setSource(Message.MessageEntity.valueOf(getSource()));

		MessageOperation op = message.createMessageOperation(OperationType.ALL_NEW_PRODUCTS);

		if (updatedSince != null) {
			MessageOperationParameter param = op.createMessageOperationParameter(
					"createdSince", String.valueOf(updatedSince.getTime()));

			op.getParameters().add(param);
		}

		if (dataSource != null) {
			MessageOperationParameter param = op.createMessageOperationParameter(
					"dataSource", dataSource.getCode());

			op.getParameters().add(param);
		}

		message.getOperations().add(op);

		sendMessage(message);
	}

	/**
	 * Normally we should NOT use this method - creates too much load on PE (says Bruce).
	 *
	 * @throws PersistenceException
	 */
	public void requestAllProductUpdate() throws PersistenceException
	{
		log.debug("requestAllProductUpdate()... starting");
		List<Product> products = productRepository.loadAll(Product.class);

		try {
			requestProductUpdate(products, null);
		}
		catch (MessageException e) {
			log.debug("Failed to send a productUpdate request message: ", e);
		}
	}

	public String convertMessageToPEXml(Message msg, String xslFile) {
		try {
			String internalXml = ObjectToXml.objectToXml(msg);
			log.debug("------ INTERNAL MESSAGE " + msg.getType() + " ----------");
			log.debug(internalXml);

			XSLTransform tf = new XSLTransform();
			tf.setXml(internalXml);
			tf.setXsl(PEMessageService.class, xslFile);

			String peXml = tf.transformToString();

			log.debug("------ PE MESSAGE " + msg.getType() + " ----------");
			log.debug(peXml);

			return peXml;
		}
		catch (Exception e) {
			log.debug("failed to debug the message transformation", e);
		}

		return null;
	}

	public Message convertPEXmlToMessage(String peXml, String xslFile)
	{
		log.debug("------ PE MESSAGE " + " ----------");
		log.debug(peXml);

		try {
			XSLTransform tf = new XSLTransform();
			tf.setXml(peXml);
			tf.setXsl(PEMessageService.class, xslFile);
			// throws IOException, FileNotFoundException
			String internalXml = tf.transformToString();
			// throws TransformerConfigurationException, TransformerException

			log.debug("------ INTERNAL MESSAGE  ----------");
			log.debug(internalXml);

			Message msg = (Message) XmlToObject.xmlToObject(Message.class, internalXml);
			// throws JAXBException

			return msg;
		}
		catch (Exception e) {
			log.debug("failed to debug the message transformation", e);
		}

		return null;
	}

	public void debugMessageToPEXml(Message msg) {
		String peXml = convertMessageToPEXml(msg, "/xsl/permissions-request-to-pe.xsl");

		log.debug(peXml);
	}

	public void debugPEXmlToMessage(String peXml) {
		log.debug("------ PE MESSAGE " + " ----------");
		log.debug(peXml);

		Message msg = convertPEXmlToMessage(peXml, "/xsl/pe-reply-to-permissions.xsl");

		log.debug("------ INTERNAL MESSAGE " + msg.getType() + " ----------");
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public ProductRepository getProductRepository() {
		return productRepository;
	}

	public void setProductRepository(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}

	public ProductIndexService getProductIndexService() {
		return productIndexService;
	}

	public void setProductIndexService(ProductIndexService productIndexService) {
		this.productIndexService = productIndexService;
	}

	public CommonWorkRepository getCommonWorkRepository() {
		return cwRepository;
	}

	public void setCommonWorkRepository(CommonWorkRepository cwRepository) {
		this.cwRepository = cwRepository;
	}

	public CommonWorkService getCommonWorkService() {
		return cwService;
	}

	public void setCommonWorkService(CommonWorkService cwService) {
		this.cwService = cwService;
	}

	public UserRepository getUserRepository() {
		return userRepository;
	}

	public void setUserRepository(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public int getDefaultBunchLimit() {
		return defaultBunchLimit;
	}

	public void setDefaultBunchLimit(int defaultBunchLimit) {
		this.defaultBunchLimit = defaultBunchLimit;
	}
}
