package com.wiley.permissions.services;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.mule.api.MuleException;

import com.wiley.permissions.common.dispatcher.DispatcherException;
import com.wiley.permissions.domain.message.Message;
import com.wiley.permissions.domain.message.MessageException;
import com.wiley.permissions.domain.message.pe.ProductSearchResult;
import com.wiley.permissions.domain.persistence.permissions.MessageErrorOp;
import com.wiley.permissions.domain.persistence.permissions.Product;
import com.wiley.permissions.persistence.LabelValueBean;
import com.wiley.permissions.persistence.PersistenceException;
import com.wiley.permissions.security.web.PermissionsSecurityException;
import com.wiley.permissions.services.util.ServiceException;
import com.wiley.permissions.services.view.ProductSummaryView;

//@GenerateRemote
public interface ProductService {

	/**
	 * This will be used for errors relating to CMS messages.
	 */
	public void handleErrorMessageOp(List<MessageErrorOp> errorList)
		throws ServiceException, MessageException;

	public void saveMasterLists(Message message) throws Exception;

	public Product saveProduct(Product peProd)
		throws Exception;

	public List<Product> saveProducts(List<Product> products) throws Exception;

	public Product refreshProduct(Product product, boolean getAllProducts)
		throws Exception;

	public Product refreshProduct(Product product, boolean getAllProducts, boolean dispatch)
		throws Exception;

	public Product refreshProduct(String externalId, String dataSource, boolean getAllProducts)
		throws Exception;

	public Product loadProductWithRefresh(Integer productId, String isbn13, String pnumber, String externalId, String dataSource, boolean getAllProducts)
		throws Exception;

	public void sendUpdateCommonWork(Product product)
		throws Exception;

	public void updateCommonWork(String commonWorkCode) throws ServiceException;

	// Don't mark this method as readOnly because getLatestProductInformation may insert/update
	public Product loadByExternalIdWithRefresh(String externalId, String productDataSource)
		throws PersistenceException;

	public List<ProductSearchResult> searchProducts(String searchString, String searchField)
		throws ServiceException, PermissionsSecurityException, MessageException, MuleException, DispatcherException;

	public List<ProductSearchResult> searchProducts(String searchString, String searchField, Integer userId, boolean format)
		throws ServiceException, MessageException, PermissionsSecurityException, MuleException, DispatcherException;
	
	public List<ProductSearchResult> newsearchProducts(String searchString)
			throws ServiceException, MessageException, PermissionsSecurityException, MuleException, DispatcherException, Exception;

	public List<ProductSearchResult> advancedSearchProducts(List <LabelValueBean> terms, Integer userId, boolean format)
		throws ServiceException, MessageException, PermissionsSecurityException, MuleException, DispatcherException;

	/****************************************************
	 * VIEWS LOAD METHODS							    *
	 * those methods that we want exposed thru a REST 	*
	 * service have to be defined in Service class 		*
	 ****************************************************/
	public ProductSummaryView loadPrimaryProductView(Integer cwId)
		throws PersistenceException, IllegalAccessException, InvocationTargetException, NoSuchMethodException;

	public List<Product> loadCWDetailsReportView (int cwId);

	public Product loadCWLandingView (int productId) throws PersistenceException;

	public void updateEbookSales();
	public void updateEbookSales(String isbn13);
	public void updateGrossUnitsFromBMIS() throws Exception;
}