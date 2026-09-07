package com.wiley.permissions.web.shared.controllers.landing;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.wiley.permissions.common.dispatcher.DispatcherException;
import com.wiley.permissions.domain.message.pe.ProductSearchResult;
import com.wiley.permissions.domain.persistence.permissions.AssetUse;
import com.wiley.permissions.domain.persistence.permissions.CommonWork;
import com.wiley.permissions.domain.persistence.permissions.Component;
import com.wiley.permissions.domain.persistence.permissions.DataSource;
import com.wiley.permissions.domain.persistence.permissions.ImportSource;
import com.wiley.permissions.domain.persistence.permissions.Privilege;
import com.wiley.permissions.domain.persistence.permissions.Product;
import com.wiley.permissions.domain.persistence.permissions.Role;
import com.wiley.permissions.persistence.PersistenceException;
import com.wiley.permissions.repositories.CommonWorkRepository;
import com.wiley.permissions.repositories.ProductRepository;
import com.wiley.permissions.services.AssetUseIndexService;
import com.wiley.permissions.services.AssetUseSearchResult;
import com.wiley.permissions.services.AssetUseSearchResults;
import com.wiley.permissions.services.AssetUseService;
import com.wiley.permissions.services.CommonWorkService;
import com.wiley.permissions.services.ProductIndexService;
import com.wiley.permissions.services.ProductService;
import com.wiley.permissions.services.imports.ImportAssetsStatus;
import com.wiley.permissions.services.util.ServiceException;
import com.wiley.permissions.web.shared.PermUserContext;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;


/*@RequestMapping("/landing/copyAssets")*/
@RequestMapping
public class CopyAssetsController extends BaseAnnotatedController {

	// --------------------- static data ---------------------------------
	private final static Log log = LogFactory.getLog(CopyAssetsController.class);

	private static final String STATUS_KEY = "copyAssetStatus";

	//protected final static String MODEL_FORM_NAME = "copyAssetsForm";

	// --------------------- instance data -------------------------------

	private String selectPreviousEditionView;
	private String searchProductView;
	private String redirectFormView;

	private ProductIndexService productIndexService;
	private AssetUseIndexService assetUseIndexService;
	private CommonWorkRepository cwRepository;
	private CommonWorkService commonWorkService;
	private ProductRepository productRepository;
	private ProductService productService;

	private AssetUseService assetUseService;


	// This is NOT the same as the method in CopyAssetsFromProductController (old code).
	@RequestMapping(value="/landing/copyAssets/selectPreviousEdition", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView selectPreviousEdition(HttpServletRequest request,
			@RequestParam(value = "cwId") int cwId) throws PersistenceException, ParseException, IOException
	{
		log.debug("selectPreviousEdition(): entered...");

		ModelAndView mv = new ModelAndView(selectPreviousEditionView);

		CommonWork cw = cwRepository.loadWithPrimaryProductById(cwId);
		Product primaryProduct = cw.getPrimaryProduct();

		Product previousEdition = null;
		String previousEditionWID = primaryProduct.getPreviousEditionWID();
		if (StringUtils.isNotBlank(previousEditionWID)) {
			previousEdition = productRepository.loadByExternalId(previousEditionWID);
			if (previousEdition == null) {
				log.error("selectPreviousEdition(): previousEditionWID [" + previousEditionWID
					+ "] for WID [" + primaryProduct.getExternalId() + "] not found in DB");
			}
		}
		List<Product> productList = new ArrayList<Product>();

		while (previousEdition != null) {
			productList.add(previousEdition);
			previousEditionWID = previousEdition.getPreviousEditionWID();
			if (StringUtils.isNotBlank(previousEditionWID)) {
				previousEdition = productRepository.loadByExternalId(previousEditionWID);
			}
			else previousEdition = null;
		}

		mv.addObject("productList", productList);

		return mv;
	}

	// This IS the same as the method in CopyAssetsFromProductController (old code).
	/*@GetMapping("/searchProduct")*/
	@RequestMapping(value="/landing/copyAssets/searchProduct", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView searchProduct(HttpServletRequest request,
			@RequestParam(value = "searchProduct", required = false) String searchString) throws Exception
	{
		log.debug("searchProduct(): entered...");

		searchString = StringUtils.trimToEmpty(searchString);
		CommonWork currentCw = PermUserContext.getCurrentCommonWork(request);

		ModelAndView mv = new ModelAndView(getSearchProductView());

		List<ProductSearchResult> wkResults = new ArrayList<ProductSearchResult>();
		List<ProductSearchResult> results = new ArrayList<ProductSearchResult>();
		String name = "Author";
		String dataSourceString = "US";
		DataSource dataSource = DataSource.forCode(dataSourceString);
		boolean status = commonWorkService.getPEUpdateStatus(name,dataSource);
		if (StringUtils.isNotBlank(searchString)) {
			if(status==true){
			Integer userId = PermUserContext.getCurrentUserId(request);
			try{
				results = getProductService().searchProducts(
					searchString, "isbn", userId, true);
			}catch(DispatcherException de){
				log.debug(de.getMessage());
				//results=getProductService().newsearchProducts(searchString);
			}
			}
			else if(status == false){
				results=getProductService().newsearchProducts(searchString);
			}
			for (ProductSearchResult result : results) {
				if (null != result.getWid() && result.getCommonWorkCode().equals(currentCw.getCode())) {
					mv.addObject("ERROR", "ERROR - You may not copy assets from the same product into itself");
				} else {
					wkResults.add(result);
				}
			}

			mv.addObject("results", wkResults);
		}
		return mv;
	}

	/*@GetMapping("/selectProductByWid")*/
	@RequestMapping(value="/landing/copyAssets/selectProductByWid", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView selectProductByWid(HttpServletRequest request,
			@RequestParam(value = "wid", required = true) String wid)
			throws PersistenceException, ServiceException
	{
		log.debug("selectProductByWid(): wid = [" + wid + "]");

		ModelAndView mv = new ModelAndView(getSearchProductView());

		Product product = productRepository.loadByExternalId(wid);
		if (product == null) {
			// in case the product is not loaded yet in Permissions, it has no assets
			String msg = getMessageSource().getMessage("error.product.copy.assets.search.product",
					null, null);
			mv.addObject("generalMessage", msg);
			return mv;
		}

		mv.setViewName(getRedirectFormView() + "?productId=" + product.getId());

		return mv;
	}

	// This IS the same as the method in CopyAssetsFromProductController (old code).
	/*@GetMapping("/view")*/
	@RequestMapping(value="/landing/copyAssets/view", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView view(HttpServletRequest request,
			//@RequestParam(value = "pickList", required = false) List<Integer> pickList,
			@RequestParam(value = "productId", required = true) Integer productId,
			@RequestParam(value = "edition", required = false) Boolean edition)
			throws PersistenceException, ServiceException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, ParseException, IOException
	{
		log.debug("view(): entered, productId = " + productId + ", edition = " + edition);

		Product selectedProduct = productRepository.loadById(productId);
		if (selectedProduct == null) { // this situation is not expected
			throw new IllegalStateException("No current Product");
		}

		ModelAndView mv = new ModelAndView(getFormView());

		boolean includeCovers = request.isUserInRole(Role.EMPLOYEE_DEFAULT.getCode())
			|| request.isUserInRole(Privilege.COVER_ASSETS.getCode());
		AssetUseSearchResults results = assetUseIndexService.getCWAssetUsesFromIndex(selectedProduct.getCommonWork().getId(),
				includeCovers, false);  // false means don't include canceled Asset Uses

		ArrayList<AssetUseSearchResult> temp = new ArrayList<AssetUseSearchResult>();

		// exclude Reviewed Unknown and Author Provided Unknown from the assets that can be copied into another product
		for (AssetUseSearchResult sr : results.getDocuments()) {
			if (sr.isReviewedOrAuthorUnknown()) continue;
			temp.add(sr);
		}

		mv.addObject("selectedProduct", selectedProduct);
		mv.addObject("commonWorkAssets", temp);
	//	mv.addObject("commonWorkAssets", results.getDocumentsExludingDisabledSources());
		mv.addObject("edition", BooleanUtils.toBoolean(edition));
		return mv;
	}

	// This IS the same as the method in CopyAssetsFromProductController (old code).
	// pickList contains assetUse ids
	/*@GetMapping("/submit")*/
	@RequestMapping(value="/landing/copyAssets/submit", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView onSubmit(HttpServletRequest request,
			@RequestParam(value = "pickList", required = false) List<Integer> pickList,
			@RequestParam(value = "productId", required = true) Integer productId,
			@RequestParam(value = "edition", required = false) boolean edition,
			@RequestParam(value = "includeUsage", required = false) boolean includeUsage,
			@RequestParam(value = "copyFlag", required = false) boolean copyFlag) throws Exception
	{
		log.debug("onSubmit(): entered...");

		log.debug("onSubmit(): pickList = " + StringUtils.join(pickList, ", "));
		log.debug("onSubmit(): productId = " + productId);
		log.debug("onSubmit(): edition = " + edition);
		log.debug("onSubmit(): includeUsage = " + includeUsage);
		log.debug("onSubmit(): copyFlag = " + copyFlag);

		CommonWork cw = PermUserContext.getCurrentCommonWork(request);
		Product product = getProductRepository().loadById(productId);

		ImportAssetsStatus status = new ImportAssetsStatus();
		boolean copyStatus = false;
		if (null == cw) {
			throw new IllegalStateException("No current CommonWork");
		}

		if (null != pickList) {
			boolean includeCovers = request.isUserInRole(Role.EMPLOYEE_DEFAULT.getCode())
					|| request.isUserInRole(Privilege.COVER_ASSETS.getCode());

			// Code change for SS task 11 starts
 			int userGroupId = PermUserContext.getCurrentUser(request).getGroupId();
			log.debug("in CopyAssetController...userGroupId "+userGroupId);
			// Code change for SS task 11 ends

			// copyAssets() creates a thread (async)
			/*commonWorkService.copyAssets (cw.getId(), product.getCommonWork().getId(), pickList,
					edition, includeUsage, request.getSession().getId(), includeCovers, userGroupId, copyFlag);*/

			//dummy method
			copyStatus = copyAssetsWithinClass(cw.getId(), product.getCommonWork().getId(), pickList,
				edition, includeUsage, request.getSession().getId(), includeCovers, userGroupId, copyFlag);

			/*status = commonWorkService.getCopyAssetsStatus(request.getSession().getId());*/
			log.debug("onSubmit(): status: " + copyStatus);
			if(copyStatus == false){
				commonWorkService.deleteCopyAssetsStatus(request.getSession().getId());
			}
			else{
				return new ModelAndView(getSuccessView());
			}

			/*if (status != null) {
				request.getSession().setAttribute(STATUS_KEY, status);

				while (status.isStatusOK() && (status.getProcessedCount() < status.getTotalCount()) ) {
					Thread.sleep(200);

					status = commonWorkService.getCopyAssetsStatus(request.getSession().getId());
					log.debug("onSubmit(): status: " + status);
					request.getSession().setAttribute(STATUS_KEY, status);
				}
				// maybe if status is not OK we want to display a message
				// and then delete the status from cache
				commonWorkService.deleteCopyAssetsStatus(request.getSession().getId());
			}*/
		}

		log.debug("onSubmit(): returning successView...");
		return new ModelAndView(getSuccessView());
	}

	public boolean copyAssetsWithinClass(int newCWId, int origCWId, List<Integer> auIds,
			boolean edition, boolean includeUsage, String sessionId, boolean includeCovers, int userGroupId, boolean copyFlag) {

		AssetUse copy = new AssetUse();
		try{
			log.debug("newCWId "+newCWId+"includeCovers "+includeCovers);
			List<Component> components =  cwRepository.loadComponentList(newCWId, includeCovers);
			if (CollectionUtils.isEmpty(components)) {
				log.debug("origCWId "+origCWId+"includeCovers "+includeCovers);
				cwRepository.copyComponents(newCWId, cwRepository.loadComponentList(origCWId, includeCovers));
			}
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

					copy = assetUseService.copyAsset(newCWId, origCWId, auId, edition, includeUsage, userGroupId, copyFlag);

					if (null != copy)
						auList.add(copy);
				}
				catch (Exception e) {
					log.warn("copyAssets(): caught exception calling copyAsset(): ", e);
				}
			}
			assetUseService.sendUpdateAssetUseMessages(auList, null);
		}
		catch(Exception e){
			log.debug("Exception while copying the assets"+e);
		}

		if(null != copy.getImportSource() && copy.getImportSource().equals(ImportSource.COPY_FROM_PREVIOUS_EDITION)){
			return true;
		}else{
		    return false;
		}
	}

	// This IS the same as the method in CopyAssetsFromProductController (old code).
	/*@GetMapping("/progressbar")*/
	@RequestMapping(value="/landing/copyAssets/progressbar", method = {RequestMethod.GET, RequestMethod.POST})
	public void progressbar(HttpServletRequest request, HttpServletResponse response)
			throws ServiceException, IOException
	{
		ImportAssetsStatus status = (ImportAssetsStatus) request.getSession().getAttribute(STATUS_KEY);

		int percentage = 5;
		int total = status.getTotalCount() - status.getFailureCount();

		if (null != status && (0 != total)) {
			percentage = (100 * status.getProcessedCount()) / total;
		}

		String output = String.valueOf(percentage);
		// content-length will be same as string length since no double-byte chars
		response.setContentLength(output.length());
		response.setContentType("text/plain");
		PrintWriter writer = response.getWriter();  // throws IOException
		writer.print(output);
	}


	public void setSelectPreviousEditionView(String selectPreviousEditionView) {
		this.selectPreviousEditionView = selectPreviousEditionView;
	}

	public String getSearchProductView() {
		return searchProductView;
	}

	public void setSearchProductView(String searchProductView) {
		this.searchProductView = searchProductView;
	}

	public void setRedirectFormView(String redirectFormView) {
		this.redirectFormView = redirectFormView;
	}

	public String getRedirectFormView() {
		return redirectFormView;
	}

	public String getSelectPreviousEditionView() {
		return selectPreviousEditionView;
	}

	public void setProductIndexService(ProductIndexService productIndexService) {
		this.productIndexService = productIndexService;
	}

	public ProductIndexService getProductIndexService() {
		return productIndexService;
	}

	public void setAssetUseIndexService(AssetUseIndexService assetUseIndexService) {
		this.assetUseIndexService = assetUseIndexService;
	}

	public AssetUseIndexService getAssetUseIndexService() {
		return assetUseIndexService;
	}

	public void setCommonWorkRepository(CommonWorkRepository cwRepository) {
		this.cwRepository = cwRepository;
	}

	public CommonWorkRepository getCommonWorkRepository() {
		return cwRepository;
	}

	public void setCommonWorkService(CommonWorkService commonWorkService) {
		this.commonWorkService = commonWorkService;
	}

	public CommonWorkService getCommonWorkService() {
		return commonWorkService;
	}

	public void setProductRepository(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}

	public ProductRepository getProductRepository() {
		return productRepository;
	}

	public void setProductService(ProductService productService) {
		this.productService = productService;
	}

	public ProductService getProductService() {
		return productService;
	}

	public AssetUseService getAssetUseService() {
		return assetUseService;
	}

	public void setAssetUseService(AssetUseService assetUseService) {
		this.assetUseService = assetUseService;
	}
}
