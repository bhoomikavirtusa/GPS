package com.wiley.permissions.services;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.validation.ConstraintViolationException;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.wiley.permissions.common.dispatcher.DispatcherException;
import com.wiley.permissions.common.dispatcher.OperationType;
import com.wiley.permissions.common.integration.Dataset;
import com.wiley.permissions.common.integration.Dataset.Data;
import com.wiley.permissions.common.integration.Dataset.Data.Row;
import com.wiley.permissions.common.integration.ObjectFactory;
import com.wiley.permissions.common.transformer.XmlToObject;
import com.wiley.permissions.domain.message.Message;
import com.wiley.permissions.domain.message.Message.MessageEntity;
import com.wiley.permissions.domain.message.Message.MessageType;
import com.wiley.permissions.domain.message.MessageException;
import com.wiley.permissions.domain.message.MessageOperation;
import com.wiley.permissions.domain.message.MessageOperationParameter;
import com.wiley.permissions.domain.message.pe.ProductSearchMessage;
import com.wiley.permissions.domain.message.pe.ProductSearchReply;
import com.wiley.permissions.domain.message.pe.ProductSearchResult;
import com.wiley.permissions.domain.message.pe.ProductSearchResults;
import com.wiley.permissions.domain.message.pe.ProductSearchTerm;
import com.wiley.permissions.domain.message.pe.ProductSearchTerm.ProductSearchConnective;
import com.wiley.permissions.domain.message.pe.ProductSearchTerm.ProductSearchField;
import com.wiley.permissions.domain.message.pe.ProductSearchTerm.ProductSearchOperator;
import com.wiley.permissions.domain.persistence.permissions.BusinessUnit;
import com.wiley.permissions.domain.persistence.permissions.CommonWork;
import com.wiley.permissions.domain.persistence.permissions.DataSource;
import com.wiley.permissions.domain.persistence.permissions.MessageErrorOp;
import com.wiley.permissions.domain.persistence.permissions.PermissionPayer;
import com.wiley.permissions.domain.persistence.permissions.Product;
import com.wiley.permissions.domain.persistence.permissions.ProductError;
import com.wiley.permissions.domain.persistence.permissions.ProductLine;
import com.wiley.permissions.domain.persistence.permissions.PublicationStatus;
import com.wiley.permissions.domain.persistence.permissions.User;
import com.wiley.permissions.domain.util.UniqueIdentifierGenerator;
import com.wiley.permissions.persistence.LabelValueBean;
import com.wiley.permissions.persistence.PersistenceException;
import com.wiley.permissions.repositories.AssetUseRepository;
import com.wiley.permissions.repositories.CommonWorkRepository;
import com.wiley.permissions.repositories.ProductRepository;
import com.wiley.permissions.security.web.PermissionsSecurityException;
import com.wiley.permissions.services.message.PEMessageService;
import com.wiley.permissions.services.message.PEProductSearchMessageService;
import com.wiley.permissions.services.util.ServiceException;
import com.wiley.permissions.services.view.ProductSummaryView;
import com.wiley.sf.common.config.PropertiesUtil;
import com.wiley.sf.common.lang.ArgUtil;
import com.wiley.sf.common.lang.ExceptionUtil;
import com.wiley.sf.common.monitor.PerformanceMonitor.PerfTimer;
import com.wiley.sf.common.sql.SimpleDBConnect;

/**
 * This class will also be exposed thru a REST service
 * lnagy - for now all methods are Transactional (SUPPORT) because of the Filter
 * (when the filter tries to persist the session we get detached entity tried to persist)
 *
 * @author lnagy
 */
@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = Exception.class)
public class ProductServiceImpl extends BaseService implements ProductService {

	private static final Log log = LogFactory.getLog(ProductServiceImpl.class);
	private static final Log logPEErrors = LogFactory.getLog(PEMessageService.class);

	private OperationType latestProductOperationType = OperationType.GET_LATEST_PRODUCT;
	private OperationType updateCommonWorkOperationType = OperationType.UPDATE_COMMON_WORK;

	private CommonWorkService commonWorkService;
	private PEMessageService peMessageService;
	private PEProductSearchMessageService peSearchMessageService;

	private ProductRepository productRepository;
	private ProductIndexService productIndexService;

	private CommonWorkRepository cwRepository;
	private AssetUseRepository assetUseRepository;

	@Override
	public void handleErrorMessageOp(List<MessageErrorOp> errorList) throws ServiceException,
			MessageException
	{
		log.info("handleErrorMessageOp() called");
		// TODO: fill in
	}

	@Override
	public void saveMasterLists(Message message) throws Exception {
		List<Object> masterListObjects = message.getMetadata();
		log.info("saveMasterLists(): called with set size = " + masterListObjects.size());
		List<MessageOperation> opList = message.getOperations();
		if (opList.size() == 0) {
			// (in this case we will also have had a set size of 0 above)
			return;
		}
		if (opList.size() > 1) {
			log.error("received more than one message operation (not expected) in message: " + message);
		}
		MessageOperation op = opList.get(0);
		String name = op.getParameterValue("name");
		String dataSourceString = op.getParameterValue("dataSource");
		DataSource dataSource = DataSource.forCode(dataSourceString);
		String updateTimeString = op.getParameterValue("updateTime");
		long updateTimeLong = Long.parseLong(updateTimeString);
		Date updateTime = new Date(updateTimeLong);

		Date lastUpdate = commonWorkService.getLastUpdate(name, dataSource);
		if (lastUpdate.after(updateTime)) {
			// normally this is not expected to happen
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
			log.info("saveMasterLists(): saved last_update ["
				+ dateFormat.format(lastUpdate) + "] newer than updateTime ["
				+ dateFormat.format(updateTime) + "] in message id: " + message.getId());
			return;
		}

		int count = 0;
		int errorCount = 0;

		for (Object o : masterListObjects) {
			count++;
			try {
				log.debug("saveMasterLists(): saving " + count + " of " + masterListObjects.size());
				// automatically rolls back for any exception
				productRepository.savePartialRequiresNew(o);
			}
			catch (ConstraintViolationException cve) {
				errorCount++;
				String msg = "Caught ConstraintViolationException trying to save object type ["
					+ o.getClass().getName() + "]\r\ntoString: " + o.toString() + "---- [end toString()] ----\r\n"
					+ ProductRepository.buildValidationErrorMessage(cve);
				log.error(msg);
				commonWorkService.sendEmailToAdminReceiver("saveMasterLists(): ", msg);
			}
			catch (Exception ex) {
				errorCount++;
				String msg = "Caught exception from savePartialRequiresNew(): " + o + "\r\nStack: " + ExceptionUtil.getStackTrace(ex);
				log.error(msg, ex);
				commonWorkService.sendEmailToAdminReceiver("saveMasterLists(): ", msg);
			}
		}

		if (errorCount == 0) {
			commonWorkService.setLastUpdate(updateTime, name, dataSource);
		}
	}

	@Override
	public Product saveProduct(Product peProd)
	throws Exception
	{
		return updateProduct(peProd, false);
	}

	@Override
	public List<Product> saveProducts(List<Product> products) throws Exception
	{
		List<Product> output = new ArrayList<Product>();

		log.info("saveProducts(): Updating " + products.size() + " products.");

		for (Product product : products) {
			output.add(saveProduct(product));  // throws Exception
		}

		return products;
	}

	// convenience method call (dispatch messages by default)
	@Override
	public Product refreshProduct(Product product, boolean getAllProducts)
	throws Exception
	{
		return refreshProduct(product, getAllProducts, true);
	}

	@Override
	public Product refreshProduct(Product product, boolean getAllProducts, boolean dispatch)
			throws Exception
	{
		Product output = null;

		try {
			Message message = new Message();
			message.setId(UniqueIdentifierGenerator.getNextIdentifier());
			message.setType(MessageType.REQUEST);
			message.setSource(MessageEntity.PERMISSIONS);
			message.getTargets().add(MessageEntity.PRODUCT_ENGINEERING);

			MessageOperation op = message.createMessageOperation(OperationType.PRODUCT_UPDATE);

			if (product.getDataSource() != null) {
				MessageOperationParameter param = op.createMessageOperationParameter("dataSource",
						product.getDataSource());

				op.getParameters().add(param);
			}

			List<Object> products = new ArrayList<Object>();
			products.add(product);
			op.setItems(products);
			message.getOperations().add(op);

			Message response = (Message) getServiceDispatcher().send(latestProductOperationType, message, null);

			log.info("refreshProduct(): Got a response from PE!");

			boolean isErrorMessage = false;
			if (response.getOperations().size() > 0) {
				MessageOperation operation = response.getOperations().get(0);

				if (operation.getItems().size() > 0) {
					Object item = operation.getItems().get(0);
					if (item instanceof Product) {
						Product product2 = (Product) item;

						log.info("refreshProduct(): Found Product: " + product2);
						log.debug("refreshProduct(): dataSource = " + product2.getDataSource());

						// updateProductFromPE is REQUIRES_NEW (calls separate class)
						output = updateProduct(product2, getAllProducts, dispatch);  // throws Exception
					} else if (item instanceof ProductError) {
						isErrorMessage = true;
					}
				}
			} else {
				isErrorMessage = true;
			}

			if (isErrorMessage) {
				String msg = "Product Not Found - PE sent a failure reply for externalId ["
						+ product.getExternalId() + "] isbn10 [" + product.getIsbn10()
						+ "] isbn13 [" + product.getIsbn13() + "] pnumber [" + product.getPnumber() + "]";
				logPEErrors.info("refreshProduct(): " + msg);
				throw new ServiceException(msg);
			}
		}
		catch (DispatcherException e) {
			throw new ServiceException("Could Not Search For Products", e);
		}

		return output;
	}

	/**
	 * One of the input params (productId, isbn13, pnumber, externalId) must be non-blank.
	 * dataSource must be non-blank
	 * 
	 * If product not found, we try to load from PE
	 */
	public Product loadProductWithRefresh (Integer productId, String isbn13, String pnumber, String externalId, String dataSource, boolean getAllProducts) throws Exception {
		if (productId == null && StringUtils.isBlank(isbn13) && StringUtils.isBlank(pnumber) && StringUtils.isBlank(externalId)) {
			throw new IllegalArgumentException("One of (productId, isbn13, pnumber, externalId) was expected to be specified.");
		}
		ArgUtil.notBlank(dataSource, "dataSource");
		
		Product product = null;
		if (null != productId) {
			product = productRepository.loadExtendedProductById(productId);
			if (null == product) {
				throw new ServiceException("failed to load product by id [" + productId + "]");
			}
		} else if (StringUtils.isNotBlank(isbn13)) {
			product = productRepository.getProductByISBN(isbn13);
			if (null == product) {
				product = new Product();
				product.setIsbn13(isbn13);
				product.setDataSource(dataSource);
				product = refreshProduct(product, getAllProducts);
			}
		} else if (StringUtils.isNotBlank(pnumber)) {
			product = productRepository.getProductByPnumber(pnumber);
			if (null == product) {
				product = new Product();
				product.setPnumber(pnumber);
				product.setDataSource(dataSource);
				product= refreshProduct(product, getAllProducts);
			}
		} else if (StringUtils.isNotBlank(externalId)) {
			product = productRepository.loadByExternalId(externalId);
			if (null == product) {
				product = new Product();
				product.setExternalId(externalId);
				product.setDataSource(dataSource);
				product = refreshProduct(product, getAllProducts);
			}
		}
		
		// At this point we should always have a non-null product object
		// (otherwise we would have already thrown an exception above)
		
		// don't catch exception if lazyLoad below fails
		product = productRepository.lazyLoad (Product.class, product.getId(), new String[] {"commonWork"});

		return product;
	}

	/**
	 * Refreshes the product from PE
	 * @param externalId  Must be non-blank
	 * @param dataSource  Must be non-blank
	 */
	public Product refreshProduct(String externalId, String dataSource, boolean getAllProducts)
			throws Exception
	{
		ArgUtil.notBlank(externalId, "externalId");
		ArgUtil.notBlank(dataSource, "dataSource");

		log.debug("refreshProduct(): looking for external id:" + externalId + ", dataSource: " + dataSource);
		Product product = new Product();

		product.setExternalId(externalId);
		product.setDataSource(dataSource);

		return refreshProduct(product, getAllProducts);  // throws Exception
	}

	/**
	 * will send an async request to MULE to start the request process for
	 * common work and process the results
	 * @param commonWorkCode
	 */
	private void sendUpdateCommonWorkRequest(String commonWorkCode, boolean dispatch)
	{
		ArgUtil.notNull(commonWorkCode, "commonWorkCode");

		log.debug("sendUpdateCommonWorkRequest(): cw code = " + commonWorkCode);
		try {
			log.debug("sendUpdateCommonWorkRequest(): send async MULE message");
			if(dispatch) {
				getServiceDispatcher().dispatch(updateCommonWorkOperationType, commonWorkCode, null);
			} else {
				getServiceDispatcher().send(updateCommonWorkOperationType, commonWorkCode, null);
			}
		}
		catch (Exception e) {
			log.debug("sendUpdateCommonWorkRequest(): failed to send the request to MULE", e);
		}
	}

	@Override
	public void sendUpdateCommonWork(Product product)
	throws Exception
	{
		product = refreshProduct(product, false);  // throws Exception

		if (null != product.getCommonWork()) {
			sendUpdateCommonWorkRequest (product.getCommonWork().getCode(), false);
		}
	}

	//Start: Added for Build Ticket DM-292
	@Override
	public void updateGrossUnitsFromBMIS() throws Exception {
		log.debug("Inside updateGrossUnitsFromBMIS method of ProductServiceImpl");
		String path = "smb:"+getBMISPropertyValue(1);
		String user = getBMISPropertyValue(2);
		String password = getBMISPropertyValue(3);
		log.debug("BMIS path value : " + path);

		NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication(null, user, password);
		SmbFileInputStream smbfin = null;
		List<List<Row>> mainList = null;
		log.debug("NtlmPasswordAuthentication----------------");

		try {
			SmbFile smbFile = new SmbFile(path, auth);
			log.debug("After SmbFile creation------------------");

			smbfin = new SmbFileInputStream(smbFile);
			log.debug("After getting SmbFileInputStream---------------------");

			Object obj = XmlToObject.xmlInputStreamToObject(ObjectFactory.class, smbfin);
			log.debug("After converting xml stream to java Object------------------");

			Dataset dataset = (Dataset) obj;
			Data data = dataset.getData();
			List<Row> items = data.getRow();

			log.debug("List size --- " + items.size());

			if(null != items && !items.isEmpty()) {
				productRepository.deleteFromISBNDataTempTable();
				//Create threads to achieve the huge list update tasks
				int subListSize = 300, mainListSize = 0, originalListSize = items.size();
				mainListSize = (originalListSize % subListSize == 0) ? (originalListSize/subListSize) : (originalListSize/subListSize) + 1;
				log.debug("Main list size : "+mainListSize+"    Sub-list size : "+subListSize);
				mainList = new ArrayList<List<Row>>(mainListSize);
				List<Row> tempList = new ArrayList<Row>(subListSize);
				for(Row row : items) {
					tempList.add(row);
					if(tempList.size() == subListSize) {
						mainList.add(tempList);
						tempList = new ArrayList<Row>(subListSize);
					}
				}
				//add the last tempList if it contains elements
				if(tempList.size() > 0) {
					mainList.add(tempList);
				}
				//Create a thread pool of 10 to achieve the DB. Please make sure this many connections available to run DB task concurrently
				ExecutorService executor = Executors.newFixedThreadPool(10);
				for(int i=0; i<mainListSize; i++) {
					Runnable worker = new UpdateGrossCount(mainList.get(i), i);
					executor.execute(worker);
				}
				executor.shutdown();
				while (!executor.isTerminated()) {
					//waiting for termination of executer service
					//don't keep any log here... will be printed infinitely... :-)
				}
				log.debug("Finished all threads... ");
				productRepository.deleteFromISBNDataTable();
				productRepository.insertToISBNDataTableFromTempTbl();

			}
			log.debug("Completed ...nice !");
		} catch (Exception ex) {
			log.error("Exception occured :: " + ex.getMessage());
		} finally {//Kept cleaning work in finally to make sure it executes always
			if(null != smbfin) { //Release I/O resource
				smbfin.close();
			}
			if(null != mainList) {
				//Clear the list, so that it will be available for GC
				mainList.clear();
				mainList = null;
			}
		}
	}

	private String getBMISPropertyValue(int val) {
		String fileName = System.getenv("PERMISSIONS_HOME") + "/permissions.properties";
		PropertiesUtil pu = null;
		String bmisValue = null;
		try {
			pu = new PropertiesUtil(fileName);
			log.info("getBMISPropertyValue(): loaded properties from " + fileName);
		}
		catch (IOException e) {
			log.error("getBMISPropertyValue(): Could not load Properties from: " + fileName);
		}

		if (pu != null) {
			try {
				if(val == 1) {
					bmisValue = pu.getStringNotBlank("bmis.filepath.url");
				} else if(val == 2) {
					bmisValue = pu.getStringNotBlank("bmis.filepath.user");
				} else if (val == 3) {
					bmisValue = pu.getStringNotBlank("bmis.filepath.password");
				}
			}
			catch (Exception ex) {
				log.error("getBMISPropertyValue(): Error reading properties: ", ex);
			}
		}
		return bmisValue;
	}
	//End: Added for Build Ticket DM-292

	/**
	 * @param externalId  Must be non-blank
	 * @param dataSource  Must be non-blank
	 */
	// Don't mark this method as readOnly because getLatestProductInformation may insert/update
	@Override
	public Product loadByExternalIdWithRefresh(String externalId, String dataSource)
			throws PersistenceException
	{
		ArgUtil.notBlank(externalId, "externalId");
		ArgUtil.notBlank(dataSource, "dataSource");
		
		// if product not found in DB try to load from PE
		try {
			Product product = loadProductWithRefresh (null, null, null, externalId, dataSource, true);
			return product;
		}
		catch (Exception e) {
			log.error("loadByExternalId(): caught exception: ", e);
			return null;
		}

		/** - assume trigger works fine, can remove this later
		try {
			Integer cwId = prod.getCommonWork().getId();
			commonWorkService.addToCwHistory_productImport(cwId);
		}
		catch (Exception ex) {
			log.error("failed to save product import to cw_history: ", ex);
		}
		*/
	}

	// convenience call (Dispatch Messages by default)
	private Product updateProduct(Product peProd, boolean getAllProducts) throws Exception {
			return  updateProduct(peProd, getAllProducts, true);
	}

	private Product updateProduct(Product peProd, boolean getAllProducts, boolean dispatch) throws Exception {
		try {
			productRepository.savePartialProduct(peProd);  // REQUIRES_NEW
		}
		// When we catch an exception here this means that the Message Status will
		// be saved as 'PROCESSED' instead of 'FAILED'.
		// - But since we send an email it's probably ok (and these exceptions are generally
		// repeated if we try to process the productUpdate again, so might as well not go to FAILED).
		catch (RuntimeException e) {
			StringBuilder sb = new StringBuilder("updateProduct(): Product: " + peProd + "\r\n");
			sb.append("Caught RuntimeException from savePartialProduct(): ");

			if (e instanceof javax.validation.ConstraintViolationException) {
				javax.validation.ConstraintViolationException cve = (javax.validation.ConstraintViolationException)e;
				sb.append("---- javax.validation.ConstraintViolationException:\r\n");
				sb.append(ProductRepository.buildValidationErrorMessage(cve));
			}
			else {
				org.hibernate.exception.ConstraintViolationException cve =
					(org.hibernate.exception.ConstraintViolationException)
					ExceptionUtil.findException(e, org.hibernate.exception.ConstraintViolationException.class);
				if (cve != null) {
					sb.append("---- org.hibernate.exception.ConstraintViolationException: ");
					sb.append("constraintName = " + cve.getConstraintName());
					sb.append("\r\nsql = " + cve.getSQL());
					sb.append("\r\nsqlException = " + cve.getSQLException() + "-----------------\r\n");
				}
			}

			sb.append("Exception starting with root: ");
			sb.append(StringUtils.join(ExceptionUtils.getRootCauseStackTrace(e), "\r\n"));

			String subject = "ProductServiceImpl.updateProduct()";
			String msg = sb.toString();
			commonWorkService.sendEmailToAdminReceiver(subject, msg);
			log.error(msg);
		}

		if (getAllProducts) {
		    // old sync call
			//updateCommonWorkFromPE(commonWork.getCode());

			// send message instead of update directly so call is async
			sendUpdateCommonWorkRequest(peProd.getCommonWork().getCode(), dispatch);
		}

		return peProd;
	}

	@Override
	public void updateCommonWork(String cwCode) throws ServiceException {
		ArgUtil.notNull(cwCode, "cwCode");
		log.debug("updateCommonWorkFromPE(): cwCode = " + cwCode);
		PerfTimer timer = getMonitor().startTimer("ProductService::updateCommonWorkFromPE");

		try {
			List<ProductSearchResult> results = null;
			try {
				PerfTimer timerSearchProducts = getMonitor().startTimer("ProductService::updateCommonWorkFromPE::searchProducts");
				// first search for all products with same commonWork
				results = searchProducts(cwCode, "commonWork");  // throws various exceptions
				log.debug("updateCommonWorkFromPE(): found " + results.size() + " products for CW code = " + cwCode);
				timerSearchProducts.stopTimer();
			}
			catch (Exception e) {
				throw new ServiceException("Caught exception trying to searchProducts: ", e);
			}

			List<Product> productList = new ArrayList<Product>();

			// for each product, load the product from PE
			for (ProductSearchResult result : results) {
				PerfTimer timerInner = getMonitor().startTimer("ProductService::updateCommonWorkFromPE::getLatestProductInformation");
				try {
					// log.debug("--------------------- PROCESS WID " + result.getWid());
					// if product already in DB
					// lnagy - maybe we should always force the reload of each product,
					// for now we check if we already have it
					Product prod = productRepository.loadByExternalId(result.getWid());
					// log.debug("--------------------- AFTER LOAD");
					// log.debug("--------------------- PRODUCT " + prod);
					if (null == prod) {
						// log.debug("--------------------- SEND REQUEST " + result.getWid());
						// Important: refreshProduct() ends up eventually calling a method that is
						// REQUIRES_NEW, so when this call is done, the DB is already updated
						prod = refreshProduct(result.getWid(), result.getDataSource(), false);
						log.debug("updateCommonWorkFromPE(): loaded product: dataSource = "
							+ prod.getDataSource() + ", wid = " + prod.getExternalId());
					}
					productList.add(prod);

				}
				catch (Exception e) {
					log.warn("updateCommonWorkFromPE(): failed to load product with extId [" + result.getWid() + "]", e);
				}
				timerInner.stopTimer();
			}

			try {
				// smarkoff: Passing our productList into recalculatePrimaryProduct() is no good
				// because these objects are not attached to the persistent context and would
				// therefore not be updated except in memory
				productRepository.recalculatePrimaryProduct(cwCode);  // REQUIRES_NEW

				CommonWork commonWork = cwRepository.loadByCode(cwCode);

				commonWorkService.updateStatusForCommonWork(commonWork);
			}
			catch (Exception e) {
				log.error("updateCommonWorkFromPE(): Caught exception: " + e);
			}
		}
		finally {
			timer.stopTimer();
		}
	}

	@Override
	public List<ProductSearchResult> searchProducts(String searchString, String searchField)
		throws ServiceException, PermissionsSecurityException, MessageException, DispatcherException, MuleException
	{
		if (!searchField.equals("author") && !searchField.equals("title") &&
			!searchField.equals("isbn") && !searchField.equals("commonWork") &&
			!searchField.equals("all"))
		{
			throw new ServiceException("searchField [" + searchField
					+ "] is invalid. Possible values = {author, title, isbn, all}.");
		}

		String searchTerm = "*" + searchString + "*";

		List<ProductSearchTerm> terms = new ArrayList<ProductSearchTerm>();

		if (searchField.equals("author") || searchField.equals("all")) {
			terms.add(new ProductSearchTerm(
				ProductSearchField.AUTHOR,
				ProductSearchOperator.EQUALS,
				ProductSearchConnective.OR,
				searchTerm));
		}

		if (searchField.equals("isbn") || searchField.equals("all")) {
			terms.add(new ProductSearchTerm(
					ProductSearchField.ISBN_10,
					ProductSearchOperator.EQUALS,
					ProductSearchConnective.OR,
					searchTerm));
			terms.add(new ProductSearchTerm(
					ProductSearchField.ISBN_13,
					ProductSearchOperator.EQUALS,
					ProductSearchConnective.OR,
					searchTerm));
			terms.add(new ProductSearchTerm(
					ProductSearchField.PRODUCT_NUMBER,
					ProductSearchOperator.EQUALS,
					ProductSearchConnective.OR,
					searchTerm));
		}

		if (searchField.equals("title") || searchField.equals("all")) {
			terms.add(new ProductSearchTerm(
					ProductSearchField.TITLE,
					ProductSearchOperator.EQUALS,
					ProductSearchConnective.OR,
					searchTerm));
		}

		if (searchField.equals("commonWork")) {
			terms.add(new ProductSearchTerm(
					ProductSearchField.COMMON_WORK,
					ProductSearchOperator.EQUALS,
					ProductSearchConnective.OR,
					searchTerm));
		}

		ProductSearchMessage psm = peSearchMessageService.createMessage(terms);

		ProductSearchMessage response = peSearchMessageService.sendMessage(psm);

		if (response == null) {
			throw new ServiceException("No response returned from search (response == null)");
		}

		ProductSearchReply reply = response.getReply();

		if (reply == null) {
			throw new ServiceException("No reply node returned from search (reply == null)");
		}

		ProductSearchResults results = reply.getProductSearchResults();
		//log.debug("searchProducts(): results: " + results);

		if (results == null) {
			throw new ServiceException("No Product Search Results Returned (results == null)");
		}

		if (results.getError() != null) {
			throw new ServiceException(results.getError().getMessage());
		}
		else return results.getResults();
	}

	@Override
	public List<ProductSearchResult> searchProducts(String searchString, String searchField, Integer userId, boolean format)
	throws ServiceException, MessageException, PermissionsSecurityException, DispatcherException, MuleException
	{
		List<ProductSearchResult> results = searchProducts (searchString, searchField);
		if (format) {
			return formatSearchResults (results, userId);
		}
		else return results;
	}
	
	//new method added by santhosh
		@Override
		@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
		public List<ProductSearchResult> newsearchProducts(String searchString)
		throws ServiceException, MessageException, PermissionsSecurityException, DispatcherException, MuleException, Exception
		{
			List<ProductSearchResult> results = new ArrayList<ProductSearchResult>();
			if (StringUtils.isNotBlank(searchString)) {
				Product product = productRepository.getProductByISBN(searchString);
				ProductSearchResult result = new ProductSearchResult();
				CommonWork cw = product.getCommonWork();
				ProductLine pl = product.getProductLine();
				PublicationStatus ps = product.getPublicationStatus();
				BusinessUnit bu = product.getBusinessUnit();
				List<String> authors = new ArrayList<String>();
				for(User u : product.getAuthors()){
					authors.add(u.getLastName()+", "+u.getFirstName());
				}
				log.debug("External id--->"+product.getExternalId());
				log.debug("Title--->"+product.getTitle());
				log.debug("getCommonWork()"+cw.getCode());
				result.setWid(product.getExternalId());
				result.setBusinessUnitCode(bu.getCode());
				result.setIsbn10(product.getIsbn10());
				result.setIsbn13(product.getIsbn13());
				result.setPnumber(product.getPnumber());
				result.setStatus(ps.getCode());
				result.setDataSource(product.getDataSource());
				result.setTitle(product.getTitle());
				result.setAuthors(authors);
				result.setProductLine(pl.getCode());
				result.setEditor(product.getEditor());
				result.setCopyrightYear(""+product.getCopyrightYear());
				result.setMediumCode(product.getMedium().getCode());
				result.setEditionNumber(""+product.getEditionNumber());
				result.setShortAuthorName(product.getShortAuthorName());
				result.setCommonWorkCode(cw.getCode());
				results.add(result);
			}
			return results;
		}
		//end new method

	@Override
	public List<ProductSearchResult> advancedSearchProducts(List <LabelValueBean> userSelections, Integer userId, boolean format)
	throws ServiceException, MessageException, PermissionsSecurityException, DispatcherException, MuleException
	{
		List<ProductSearchTerm> terms = new ArrayList<ProductSearchTerm>();

		if (null == userSelections || userSelections.size() == 0) {
			return new ArrayList<ProductSearchResult> ();
		}

		for (int x = 0; x < userSelections.size(); x++) {

			LabelValueBean sel = userSelections.get(x);

			if (sel.getLabel().equals("Author")) {
				terms.add(new ProductSearchTerm(
					ProductSearchField.AUTHOR,
					ProductSearchOperator.EQUALS,
					ProductSearchConnective.AND,
					"*" + sel.getValue() + "*"));
			}

			if (sel.getLabel().equals("Title")) {
				terms.add(new ProductSearchTerm(
					ProductSearchField.TITLE,
					ProductSearchOperator.EQUALS,
					ProductSearchConnective.AND,
					"*" + sel.getValue() + "*"));
			}

			if (sel.getLabel().equals("ProductLine")) {
				terms.add(new ProductSearchTerm(
					ProductSearchField.PRODUCT_LINE,
					ProductSearchOperator.EQUALS,
					ProductSearchConnective.AND,
					sel.getValue()));
			}

			if (sel.getLabel().equals("Edition")) {
				terms.add(new ProductSearchTerm(
						ProductSearchField.EDITION_NUMBER,
						ProductSearchOperator.EQUALS,
						ProductSearchConnective.AND,
						sel.getValue() ));

				/* lnagy - just use the value from UI
				Pattern p = Pattern.compile("(\\d+\\.\\d{1,3}){10}");

				Matcher m = p.matcher(sel.getValue());

				String numEd = "";
				if (m.find()) {
				    numEd = m.group();
				}

				if (numEd.length() > 0) {
					terms.add(new ProductSearchTerm(
							ProductSearchField.EDITION_NUMBER,
							ProductSearchOperator.EQUALS,
							ProductSearchConnective.AND,
							numEd ));
				}
				*/
			}

			if (sel.getLabel().equals("EditorCode")) {
				terms.add(new ProductSearchTerm(
					ProductSearchField.EDITOR,
					ProductSearchOperator.EQUALS,
					ProductSearchConnective.AND,
					sel.getValue() ));
			}

			if (sel.getLabel().equals("CopyrightYear")) {
				terms.add(new ProductSearchTerm(
					ProductSearchField.COPYRIGHT_YEAR,
					ProductSearchOperator.EQUALS,
					ProductSearchConnective.AND,
					sel.getValue()));
			}

			if (sel.getLabel().equals("PubStatus")) {
				terms.add(new ProductSearchTerm(
					ProductSearchField.STATUS,
					ProductSearchOperator.EQUALS,
					ProductSearchConnective.AND,
					sel.getValue()));
			}

		}

		ProductSearchMessage psm = peSearchMessageService.createMessage(terms);
		ProductSearchMessage response = peSearchMessageService.sendMessage(psm);

		if (response == null) {
			throw new ServiceException("No response returned from search (response == null)");
		}

		ProductSearchReply reply = response.getReply();

		if (reply == null) {
			throw new ServiceException("No reply node returned from search (reply == null)");
		}

		ProductSearchResults results = reply.getProductSearchResults();

		if (results == null) {
			throw new ServiceException("No Product Search Results Returned (results == null)");
		}

		if (results.getError() != null) {
			throw new ServiceException(results.getError().getMessage());
		}
		else return results.getResults();
	}


	/**
	 * takes the SearchResultList and formats it to be displayed on the SearchResult page
	 * (marks the watched ones, merges the commonWorks, and loads the BusinessUnit description)
	 * @param resultList
	 * @param userId
	 * @return
	 * @throws ServiceException
	 */
	private List<ProductSearchResult> formatSearchResults (List<ProductSearchResult> resultList, Integer userId)
		throws ServiceException
	{
		List<ProductSearchResult> finalList = new ArrayList<ProductSearchResult>();

		try {
			HashSet<String> watchedWidSet = new HashSet<String>();
			HashMap<String, ProductSearchResult> commonWorks = new HashMap <String, ProductSearchResult>();

			if (userId != null) {
				List<Product> watchedProducts = productRepository.loadProductsInWatchedCommonWorks (userId);
				// copy watched wids to HashSet for fast lookup

				for (Product p : watchedProducts) {
					watchedWidSet.add(p.getExternalId());
				}
			}

			for (ProductSearchResult psr : resultList) {
				if (watchedWidSet.contains(psr.getWid())) {
					psr.setWatched(true);
				}
				// Go through the results and populate businessUnitName
				// for each businessUnit code, using the ReferenceDataCache.
				String code = psr.getBusinessUnitCode();
				if (StringUtils.isBlank(code)) {
					psr.setBusinessUnitCode("(not specified)");
				}
				else {
				    BusinessUnit bu = productRepository.find (BusinessUnit.class, psr.getBusinessUnitCode());
				    if (bu == null) {
				    	psr.setBusinessUnitName("(unknown code: " + code + ")");
				    }
				    else {
				    	psr.setBusinessUnitName(bu.getName());
				    }
				}
				// add non commonWork products directly to final list
				if (StringUtils.isBlank(psr.getCommonWorkCode())) {
					log.debug("formatSearchResults(): No common work code ... add it to final list " + psr.getIsbn13());
					finalList.add(psr);
				}
				else {
					ProductSearchResult primary = commonWorks.get(psr.getCommonWorkCode());
					// if no entry for the current commonWork
					if (null == primary) {
						log.debug("formatSearchResults(): No primary ... add it to primary list " + psr.getIsbn13());
						commonWorks.put(psr.getCommonWorkCode(), psr);
					}
					else {
						int primaryRank = productRepository.getMediumRank(primary.getMediumCode());
						int currentRank = productRepository.getMediumRank(psr.getMediumCode());

						log.debug("formatSearchResults(): Compare ranks : primary " + primary.getIsbn13() + "[" + primaryRank +
								"] current " + psr.getIsbn13() + "[" + currentRank + "]");
						// if current rank is greater then the one saved
						if (currentRank > primaryRank) {
							log.debug("formatSearchResults(): Replace primary with " + psr.getIsbn13());

							commonWorks.put(psr.getCommonWorkCode(), psr);
						}
						else {
							log.debug("formatSearchResults(): Keep primary " + primary.getIsbn13());
						}
					}
				}
			}

			// add commonWork entries
			finalList.addAll(commonWorks.values());
			// need to add sort
		}
		catch (PersistenceException pe) {
			log.debug("formatSearchResults(): Failed to load user information.", pe);
		}

		return finalList;
	}

	/****************************************************
	 * VIEWS LOAD METHODS							    *
	 * (// preload stuff needed by Controller and jsp) 	*
	 ****************************************************/

	/**
	 * The Author UI product gadget
	 * @param productId
	 * @return
	 * @throws PersistenceException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 */
	@Override
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public ProductSummaryView loadPrimaryProductView(Integer cwId)
			throws PersistenceException, IllegalAccessException, InvocationTargetException, NoSuchMethodException
	{
		Product product = cwRepository.loadCWById(cwId).getPrimaryProduct();
		ProductSummaryView pInfo = new ProductSummaryView (product);
		pInfo.setBoundBookDate(product.getPrintDate());
		pInfo.setManuscriptProductionDate(product.getProductionDate());
		//SR_256833-Add field to pull 'Tier' from GBPM starts
		if(product.getProductPriority()!= null){
			pInfo.setProductPriority(product.getProductPriority());
		}
		//SR_256833-Add field to pull 'Tier' from GBPM ends
		pInfo.setMediumCode(product.getMedium().getCode());
		// not used
		// pInfo.setAssetsCount(assetUseRepository.countAssetUseInCommonWork(pInfo.getCommonWorkId()));
		return pInfo;
	}

	/**
	 * CWDetails Report page
	 * @param int cwId
	 * @return List<Product>
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 * @throws PersistenceException
	 */
	@Override
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<Product> loadCWDetailsReportView (int cwId) {
		List<Product> pList = productRepository.loadProductsByCWId (cwId);
		for (Product p: pList) {
			productRepository.loadExtendedProduct (p);
			PermissionPayer permissionPayer = p.getPermissionPayer();
			if (permissionPayer != null)
				p.getPermissionPayer().getDescription();
		}

		return pList;
	}

	@Override
	@Transactional(propagation = Propagation.NEVER)
	public void updateEbookSales() {
		updateEbookSales(null);
	}

	/**
	 *
	 * @param requestedIsbn13  If blank will do for all
	 */
	// we don't want a transaction for this method itself - want one for all sub methods
	@Transactional(propagation = Propagation.NEVER)
	@Override
	public void updateEbookSales(String requestedIsbn13) {
		// Notes about the data in CORE DB2 database:
		// Library: GOIDTALIB, Table: PVBYCPP
		// There are a bunch of columns but the important ones are
		// 1 - BYI013 - ISBN 13
		// 3 - BYWBB2 - GOI Year
		// 4 - BYWCB2 - GOI Month
		// 8 - BYWDB2 - GOI Sales units (up to 9 digit integer)

		// Because we are accessing the CORE database this method runs much faster when
		// in the USDC environment (slow in local environment in San Francisco).

		// could move this config stuff to properties file if desired
		// - SimpleDBConnect can take a properties file
		String driverClass = "com.ibm.as400.access.AS400JDBCDriver";
		String connectString = "jdbc:as400://edcsysb.wiley.com//GOIDTALIB";
		String user = "webuser";
		String password = "ODBCWEB1";
		Connection con = null;
		NumberFormat intFormat = NumberFormat.getIntegerInstance();
		int nonNullCount = 0;
		int count = 0;

		try {
			SimpleDBConnect simpleConnect = new SimpleDBConnect(driverClass, connectString,
	            user, password);
			con = simpleConnect.getConnection();
			log.debug("updateEbookSales(): got db connection");
		}
		catch (Exception ex) {
			log.error("updateEbookSales(): connect get database connection: ", ex);
			return;
		}

		Set<String> isbn13Set = null;
		try {
			String whereClause;
			if (StringUtils.isBlank(requestedIsbn13)) {
				whereClause = "isbn13 is not null and medium = 'E'";
			}
			else {
				whereClause = "isbn13 = '" + requestedIsbn13 + "' and medium = 'E'";
			}
			isbn13Set = productRepository.loadIsbn13s(whereClause);
			log.debug("updateEbookSales(): loaded " + intFormat.format(isbn13Set.size()) + " isbn13's");
		}
		catch (Exception ex) {
			log.error("updateEbookSales(): could not load isbn13s: ", ex);
			return;
		}

		for (String isbn13 : isbn13Set) {
			count++;
			try {
				Integer total = loadEbookSalesForIsbn13(con, isbn13);
				//log.debug("updateEbookSales(): count for isbn13 [" + isbn13 + "] is " + count);
				if (total != null) {
					nonNullCount++;
					productRepository.updateEbookSalesForIsbn13(isbn13, total);  // REQUIRES_NEW
				}
				if (count % 1000 == 0) {
					log.debug("updateEbookSales(): count = " + intFormat.format(count)
							+ ", nonNullCount = " + intFormat.format(nonNullCount)
							+ ", nonNullPercent = " + (100 * nonNullCount / count));
				}
			}
			catch (Exception ex) {
				log.error("updateEbookSales(): caught exception trying to load/update data for isbn13: " + isbn13, ex);
			}
		}

		log.debug("updateEbookSales(): nonNullCount = " + intFormat.format(nonNullCount)
				+ "(out of " + intFormat.format(count) + " isbn's)");
		// count can be zero if user requested an isbn that is not medium E so avoid divide by zero error
		log.debug("updateEbookSales(): nonNullPercent = " + (count == 0 ? "n/a" : (100 * nonNullCount / count)));
	}

	private Integer loadEbookSalesForIsbn13(Connection con, String isbn13) throws SQLException {
		// this method is a bit slow from San Francisco but hopefully much faster running from USDC
		PerfTimer timer = getMonitor().startTimer("ProductService::loadEbookSalesForIsbn13");

		final String sql = "select sum(BYWDB2) from GOIDTALIB.PVBYCPP where BYI013 = ?";
		PreparedStatement ps = con.prepareStatement(sql);
		ps.setString(1, isbn13);
		ResultSet rs = ps.executeQuery();
		rs.next();
		Integer result = rs.getInt(1);
		if (rs.wasNull()) result = null;
		ps.close();

		timer.stopTimer();
		return result;
	}

	/**
	 * CW Landing page
	 * @param productId
	 * @return
	 * @throws PersistenceException
	 */
	@Override
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public Product loadCWLandingView(int productId) throws PersistenceException {
		return productRepository.loadExtendedProductById (productId);
	}


	public OperationType getLatestProductOperationType() {
		return latestProductOperationType;
	}
	
	public void setLatestProductOperationType(OperationType getLatestProductOperationType) {
		this.latestProductOperationType = getLatestProductOperationType;
	}

	public OperationType getUpdateCommonWorkOperationType() {
		return updateCommonWorkOperationType;
	}
	
	public void setUpdateCommonWorkOperationType(OperationType updateCommonWorkOperationType) {
		this.updateCommonWorkOperationType = updateCommonWorkOperationType;
	}

	public CommonWorkService getCommonWorkService() {
		return commonWorkService;
	}
	
	public void setCommonWorkService(CommonWorkService commonWorkService) {
		this.commonWorkService = commonWorkService;
	}

	public PEMessageService getPeMessageService() {
		return peMessageService;
	}
	
	public void setPeMessageService(PEMessageService peMessageService) {
		this.peMessageService = peMessageService;
	}

	public PEProductSearchMessageService getPeSearchMessageService() {
		return peSearchMessageService;
	}
	
	public void setPeSearchMessageService(PEProductSearchMessageService peSearchMessageService) {
		this.peSearchMessageService = peSearchMessageService;
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

	public AssetUseRepository getAssetUseRepository() {
		return assetUseRepository;
	}
	
	public void setAssetUseRepository(AssetUseRepository assetUseRepository) {
		this.assetUseRepository = assetUseRepository;
	}

	//Start: Added for Build Ticket DM-292
	class UpdateGrossCount implements Runnable {
		private final List<Row> items;
		private final int i;
		public UpdateGrossCount(List<Row> items, int i) {
			this.items = items;
			this.i = i;
		}
		@Override
		public void run() {
			try {
				productRepository.updateBatchGrossUnitsForIsbn13(items, i);
			} catch (PersistenceException e) {
				e.printStackTrace();
			}
		}
	}
	//End: Added for Build Ticket DM-292
}
