package com.wiley.permissions.services.imports;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.jxls.reader.XLSDataReadException;
import net.sf.jxls.reader.XLSReadMessage;
import net.sf.jxls.reader.XLSReadStatus;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.SAXException;

import com.wiley.permissions.common.excel.ExcelTransformerUtility;
import com.wiley.permissions.common.utils.ValidateException;
import com.wiley.permissions.domain.persistence.permissions.DataSource;
import com.wiley.permissions.domain.persistence.permissions.Product;
import com.wiley.permissions.persistence.PersistenceException;
import com.wiley.permissions.repositories.ProductRepository;
import com.wiley.permissions.services.ProductService;
import com.wiley.permissions.services.message.PEMessageService;
import com.wiley.permissions.services.util.ServiceException;

/**
 * Reads a spreadsheet from FileMakerPro and calls PE to update the
 * product based on ISBN10 or ISBN13
 * If EDITION is required to be imported from spreadsheet, make sure there are
 * Integer values in the column
 *
 * @author lnagy
 */
public class ImportProductsUtility {

	// it will be nice to pass this thru the interface
	boolean bHeader = true;

	private ExcelTransformerUtility excelUtility;

	private ProductService productService;
	private ProductRepository productRepository;

	private static final String FILEMAKER_PRODUCT_MAPPING_FILE = "/conf/mapping/filemakerProductMapping.xml";

	private static final Log log = LogFactory.getLog(ImportProductsUtility.class);

	private static final Log logPEErrors = LogFactory.getLog(PEMessageService.class);

	/**
	 * The FileMakerPro import for products
	 * @param data
	 * @throws XLSDataReadException
	 * @throws PersistenceException
	 */
	public void importFileMakerProducts(byte[] data) throws XLSDataReadException, PersistenceException
	{
		log.debug("importFileMakerProducts()...begin");
		List<Product> products = convertXLSToProductList(data, FILEMAKER_PRODUCT_MAPPING_FILE);

		importProducts(products);
	}

	/**
	 * Processes a list of products
	 * @param products
	 */
	public void importProducts(List<Product> products)
	{
		log.debug("importProducts()...product list size " + products.size());
		StringBuilder errorMessage = new StringBuilder();

		int failures = 0;
		int currentRow = 0;

		for (Product product : products) {
			// skip the header
			if (currentRow++ == 0 && bHeader) {
				continue;
			}
			try {
				log.debug("*** Processing row " + currentRow);
				importProduct(product);
			}
			catch (Exception e) {
				log.debug("importProducts(): failed processing product  [" + product.getIsbn10()
						+ "], error [" + e.getMessage() + "]");
				errorMessage.append("Row (" + currentRow + ") : " + e.getMessage() + "<br/>");
				failures++;
			}
		}
		log.debug("importProducts() failures [" + failures + "]");
	}

	/**
	 * Reads the spreadsheet and converts it into ProductList
	 *
	 * @param data
	 * @return List<Product>
	 * @throws IOException
	 * @throws SAXException
	 * @throws XLSDataReadException
	 */
	private List<Product> convertXLSToProductList(byte[] data, String mappingFile)
			throws XLSDataReadException
	{
		log.debug("convertXLSToProductList()...begin");

		Map<String, Object> input = new HashMap<String, Object>();
		List<Product> products = new ArrayList<Product>();
		StringBuilder errorMessage = new StringBuilder();

		InputStream in = getClass().getResourceAsStream(mappingFile);
		try {
			String mapping = IOUtils.toString(in);

			input.put("inputStream", data);
			input.put("inputMapping", mapping.getBytes());
			// empty list that will be populated
			input.put("products", products);

			XLSReadStatus status = getExcelUtility().transformExcelToBeans(input);
			for (XLSReadMessage message : (List<XLSReadMessage>) status.getReadMessages()) {
				errorMessage.append(message.getMessage());
				errorMessage.append("<br/>");
			}
		}
		catch (Exception e) {
			throw new XLSDataReadException("Failed to read the spreadsheet file [" + e.getMessage() + "]");
		}

		// decided to throw the errors from reading the spreadsheet here
		// not wait until the assets are processed
		String errorMsg = errorMessage.toString();
		if (StringUtils.isNotBlank(errorMsg)) {
			log.debug("convertXLSToProductList(): failed to read the spreadsheet file"
					+ errorMsg);
			throw new XLSDataReadException(errorMsg);
		}
		return products;
	}

	/**
	 * sends a message to PE to get the product, and persists the product
	 * If no product in PE, ignores the product
	 *
	 * @param product
	 * @return
	 * @throws PersistenceException
	 * @throws ServiceException
	 * @throws ValidateException
	 */
	private void importProduct(Product product)
	throws Exception
	{
		String identifier = product.getIsbn10().trim();
		String notes = "";
		
		if(null != product.getCommonWork().getNotes()) {
			notes = product.getCommonWork().getNotes();
		    product.setCommonWork(null);
		    log.debug("notes found:" + notes);
		}

		// if no ISBN, no way to update
		if (StringUtils.isBlank(identifier)) {
			logPEErrors.debug("empty ISBN value, title [" + product.getTitle() + "]");
			return;
		}

		identifier = setProductIdentifiers (product);

		// we try first to see if the product is already in the DB, so there is no reason to send
		// a request to PE
		try {
			Product existingProduct = productRepository.getProductByISBN (identifier.toUpperCase());
			if (existingProduct != null) {
				log.debug("importProduct(): found product by ISBN[" + identifier + "]. ");
		//		return;
			}
		}
		catch (Exception e) {
			log.debug("importProduct(): failed to load product by ISBN[" + identifier + "]. " +
					"We send the message to PE");
		}

		if (StringUtils.isBlank(product.getDataSource())) {
			product.setDataSource(DataSource.US.getCode());
		}
		productService.refreshProduct(product, true, false);
			// throws Exception
		
		// add logic to update the common work Notes here
		Product existingProduct = productRepository.getProductByISBN (identifier.toUpperCase());
		existingProduct = getProductRepository().lazyLoad(Product.class, existingProduct.getId(), new String[] {"commonWork"});	
		existingProduct.getCommonWork().setNotes(notes);
	
		//getProductRepository().persist(existingProduct);
		getProductRepository().save(existingProduct);
		//  load product by isbn again
		// do lazy load on product existingPrdouct common work 
		// set common work notes
		// persist common work
		
	}

	/**
	 * tries to process the ISBN from the spreadsheet and also tries to validate the format
	 * @param product
	 * @return
	 * @throws ServiceException
	 */
	public String setProductIdentifiers (Product product) throws ServiceException
	{
		String identifier = product.getIsbn10().trim();

		log.debug("setProductIdentifiers: " + identifier);

		// if it is 11 characters, I try without the last character : usually "C" or "S"
		if (identifier.length() == 11) {
			identifier = identifier.substring(0, identifier.length() - 1);
			product.setIsbn10(identifier);
		}
		if (identifier.length() == 9) {
			product.setPnumber(identifier);
			product.setIsbn10(null);
		}
		else if (identifier.length() == 10) {
			// we send ISBN10

		}
		else if (identifier.length() == 13) {
			// we send ISBN13
			product.setIsbn13(identifier);
			product.setIsbn10(null);
		}
		else {
			// no correct length
			logPEErrors.debug("no correct ISBN value [" + identifier + "]");
			throw new ServiceException("no correct ISBN value [" + identifier + "]");
		}
		return identifier;
	}

	public ExcelTransformerUtility getExcelUtility() {
		return excelUtility;
	}

	public void setExcelUtility(ExcelTransformerUtility excelUtility) {
		this.excelUtility = excelUtility;
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
}
