package com.wiley.permissions.resources;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.queryparser.classic.ParseException;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.InitialisationException;

import com.wiley.permissions.common.dispatcher.DispatcherException;
import com.wiley.permissions.common.transformer.ObjectToJson;
import com.wiley.permissions.common.transformer.TransformationException;
import com.wiley.permissions.domain.message.MessageException;
import com.wiley.permissions.domain.message.pe.ProductSearchResult;
import com.wiley.permissions.persistence.PersistenceException;
import com.wiley.permissions.security.web.PermissionsSecurityException;
import com.wiley.permissions.services.ProductIndexService;
import com.wiley.permissions.services.ProductService;
import com.wiley.permissions.services.util.ServiceException;
import com.wiley.permissions.services.view.ProductSummaryView;

/**
 * This class is the REST service implementation
 * Uses the ProductService to make the calls, but it also transforms the results
 * to XML or JSON
 * We need this because the version of MULE we have I tested it and it does not do the
 * JSON transformation
 * Maybe MULE 3.0 will handle the JSON transformation and then we can annotate the ProductServiceImpl
 * directly as a REST resource
 * Because of cross domain requests, we need JSONP (a callback method name is passed as QueryParam)
 * @author lnagy
 */
@Path ("/")
public class ProductResource {

	private static final Log log = LogFactory.getLog(ProductResource.class);

	private ProductService productService;
	private ProductIndexService productIndexService;

	@GET
	@Produces("application/json")
	@Path("/{id}")
	public String loadProductView (@PathParam("id") Integer id, @QueryParam("callback") String callback)
		throws PersistenceException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, InitialisationException, TransformationException
	{
		log.debug("loadProductView(): called with id = " + id);
		ProductSummaryView product = productService.loadPrimaryProductView(id);
		return ObjectToJson.doTransform(product, callback);
	}

	@GET
	@Produces("application/json")
	@Path("/search/{term}")
	public String productSearchView (@PathParam("term") String term, @QueryParam("callback") String callback)
		throws ServiceException, MessageException, TransformationException, PermissionsSecurityException, DispatcherException, MuleException, IOException, ParseException
	{
		log.debug("productSearchView(): called with term = " + term);
		List<ProductSearchResult> products = productIndexService.searchByTitleStart(term, 100);
		log.debug("productSearchView(): results size = " + products.size());
		return ObjectToJson.doTransform(products, callback);
	}

	@GET
	@Produces("application/json")
	@Path("/isbnsearch/{term}")
	public String productIsbnSearchView (@PathParam("term") String term, @QueryParam("callback") String callback)
		throws ServiceException, MessageException, TransformationException, PermissionsSecurityException, DispatcherException, MuleException, IOException, ParseException
	{
		log.debug("productIsbnSearchView(): called with term = " + term);

		List<ProductSearchResult> products = new ArrayList<ProductSearchResult>();
		if (term.length() == 10 || term.length() == 13) {
			ProductSearchResult product = productIndexService.searchByISBN(term);
			if (null != product) {
				products.add(product);
			}
		}

		log.debug("productIsbnSearchView(): results size = " + products.size());

		return ObjectToJson.doTransform(products, callback);
	}

	@GET
	@Produces("application/json")
	@Path("/loadProductData/{term}")
	public String loadProductData (@PathParam("term") String term, @QueryParam("callback") String callback)
			throws ServiceException, MessageException, TransformationException, PermissionsSecurityException, DispatcherException, MuleException, IOException, ParseException
	{
		log.debug("loadProductData(): called with term = " + term);

		try {
			List<ProductSearchResult> products = productService.searchProducts(term, "isbn");
		//	List<Product> products = productIndexService.getProductRepository().getProductByISBN(term);
	    	List<ProductSummaryView > results = new ArrayList<ProductSummaryView >();
	    //	Product product = getProductIndexService().getProductRepository().lazyLoad(Product.class, products.get(0).getId(), new String[] {"edition", "medium"});
	    	ProductSummaryView  productView = new ProductSummaryView ();
	    	productView.setIsbn13(products.get(0).getIsbn13());
	    	productView.setIsbn10(products.get(0).getIsbn10());
	    	productView.setPnumber(products.get(0).getPnumber());
	    	productView.setTitle(products.get(0).getTitle());
	    	productView.setMediumCode(products.get(0).getMediumCode());
	    	productView.setEditionNumber((null != products.get(0).getEditionNumber()) ? new Integer (products.get(0).getEditionNumber()) : null);
	    //	productView .setMediumCode(product.getMedium().getName());
	    //	productView.setIsbn13(product.getIsbn13());
	    //	productView.setTitle(product.getTitle());
	    //	productView.setEditionNumber(product.getEdition().getEditionNumber() + "");

	    	results.add(productView);

			return ObjectToJson.doTransform(results, callback);
		} catch (Exception e) {
			log.debug("exeption thrown during transform: " + e.getMessage());
			return null;
		}
	}

	public ProductService getProductService() {
		return productService;
	}

	public void setProductService(ProductService productService) {
		this.productService = productService;
	}

	public ProductIndexService getProductIndexService() {
		return productIndexService;
	}

	public void setProductIndexService(ProductIndexService productIndexService) {
		this.productIndexService = productIndexService;
	}
}
