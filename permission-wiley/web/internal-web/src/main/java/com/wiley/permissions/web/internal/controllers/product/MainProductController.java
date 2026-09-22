package com.wiley.permissions.web.internal.controllers.product;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.wiley.permissions.common.dispatcher.DispatcherException;
import com.wiley.permissions.domain.message.pe.ProductSearchResult;
import com.wiley.permissions.domain.persistence.permissions.DataSource;
import com.wiley.permissions.domain.persistence.permissions.Product;
import com.wiley.permissions.persistence.LabelValueBean;
import com.wiley.permissions.persistence.PersistenceException;
import com.wiley.permissions.repositories.ProductRepository;
import com.wiley.permissions.security.web.UserPrincipal;
import com.wiley.permissions.services.CommonWorkService;
import com.wiley.permissions.services.ProductService;
import com.wiley.permissions.services.util.ServiceException;
import com.wiley.permissions.web.shared.PermUserContext;
import com.wiley.permissions.web.shared.UserSession;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;
import com.wiley.sf.common.lang.StringUtil;

/**
 * This class can contain methods that apply to product management.
 *
 * @author lnagy
 */
@Controller
/*@RequestMapping("/product/product")*/
@RequestMapping
public class MainProductController extends BaseAnnotatedController {

	private static final Log log = LogFactory.getLog(MainProductController.class);

	private final static String GENERAL_MESSAGE = "generalMessage";
	private final static String FORM_MODEL_NAME = "searchProductForm";
	private final static String ADVANCED_SEARCH_FORM_MODEL_NAME = "advancedSearchForm";

	private ProductRepository productRepository;

	private ProductService productService;
	private ProductService localProductService;

	private CommonWorkService commonWorkService;

	private String landingViewName = null;
	private String adminUserViewName = null;
	private String searchViewName = null;
	private String userLandingViewName = null;
	private String adminViewName = null;
	private String advancedSearchViewName = null;

	@Override
	protected List<String> getReferenceDataNames(HttpServletRequest request) throws Exception
	{
		List<String> list = new ArrayList<String>();

		list.add("businessUnits");
		list.add("publicationStatusList");

		return list;
	}

	@RequestMapping(value = "/product/product/reloadProductFromPE", method ={RequestMethod.GET, RequestMethod.POST})
	public ModelAndView reloadProductFromPE(
			@RequestParam(value = "productIdentifier", required = true) String productIdentifier,
			@RequestParam(value = "includeCommonWork", required = false) boolean includeCommonWork,
			@RequestParam(value = "localRequest", required = false) boolean localRequest)
			throws PersistenceException, ServiceException
	{
		log.debug("reloadProductFromPE(): entered...for productIdentifier " + productIdentifier);

		ModelAndView mv = new ModelAndView(adminViewName);

		try {
			Product product = new Product();
			if (10 == productIdentifier.length()) {
				product.setIsbn10(productIdentifier);
			}
			else if (13 == productIdentifier.length()) {
				product.setIsbn13(productIdentifier);
			}
			else {
				product.setExternalId(productIdentifier);
			}

			ProductService ps = getProductService();

			if (localRequest)
				ps = getLocalProductService();

			// TODO: don't hardcode US
			product.setDataSource(DataSource.US.getCode());

			if (includeCommonWork) {
				ps.sendUpdateCommonWork(product);
			}
			else {
				ps.refreshProduct(product, false);
			}
		}
		catch (Exception e) {
			mv.addObject("generalMessage", e.getMessage());
		}

		return mv;
	}

	// GET+POST: form submit is POST; results refresh/pagination/bookmarks are GET.
	// Handler already reloads blank criteria from session (see below).
	@RequestMapping(value = "/product/product/search", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView search(HttpServletRequest request,
			@ModelAttribute(FORM_MODEL_NAME) SearchProductForm form) throws Exception
	{
		log.debug("search(): entered...");
		ModelAndView mv = new ModelAndView(searchViewName);

		// If there are no search parameters in searchProductForm in the request,
		// then check the session, because for page 0 in ManageUserController,
		// the search parameters are put in a form in the session (the only place
		// safe from a redirect - and because it's a wizard controller it can't stick
		// the parameters onto the redirect)

		HttpSession session = request.getSession();

		if (StringUtils.isBlank(form.getSearchString()) || StringUtils.isBlank(form.getSearchField())) {
			SearchProductForm sessionForm = (SearchProductForm) session.getAttribute(FORM_MODEL_NAME);
			// fix NullPointerException on starting the app
			if (null != sessionForm) {
				form.setSearchString(sessionForm.getSearchString());
				form.setSearchField(sessionForm.getSearchField());
			}
		}

		try {
			// This just makes sure that old results don't show up.
			form.setResults(null);

			if (StringUtils.isNotBlank(form.getSearchString()) && StringUtils.isNotBlank(form.getSearchField())) {
				Integer userId = PermUserContext.getCurrentUserId(request);
				String searchString = form.getSearchString();
				// remove any dashes from isbn or common work ID entered by user before we search
				if (form.getSearchField().equals("isbn") || form.getSearchField().equals("commonWork")) {
					searchString = StringUtil.removeAny(searchString, "-");
				}
				List<ProductSearchResult> results = new ArrayList<ProductSearchResult>();
				String name = "Author";
				String dataSourceString = "US";
				DataSource dataSource = DataSource.forCode(dataSourceString);
				boolean status = commonWorkService.getPEUpdateStatus(name,dataSource);
				log.debug("PE_status---->"+status);
				if(status==true){
				try{
				 results = getProductService().searchProducts(
						searchString,
						form.getSearchField(),
						userId,
						true);
				}catch(DispatcherException de){
					log.debug(de.getMessage());
					//results=getProductService().newsearchProducts(searchString);
				}
				}
				else if(status == false){
					results=getProductService().newsearchProducts(searchString);
				}

				form.setResults(results);

				UserSession userSession = PermUserContext.getUserSession(request);
				userSession.setProductSearchString(form.getSearchString());
				userSession.setProductSearchField(form.getSearchField());
			}

			/*
			boolean firstHit = (form.getAfterFirstHit() == null || !form.getAfterFirstHit());
			// lnagy - restore the JMESA settings
			if (firstHit) {
				log.debug("handle(): DO RESTORE");
				mv.addObject("restore", "true");
			} else {
				log.debug("handle(): DO NOT RESTORE");
			}
			*/
		}
		catch (ServiceException e) {
			log.error("search(): Error Searching Products", e);

			String msgKey = "product.searchProduct.errors.general";
			String msg = getMessageSource().getMessage(msgKey, null, null);
			request.setAttribute(GENERAL_MESSAGE, msg);
		}

		mv.addObject(FORM_MODEL_NAME, form);

		session.setAttribute(FORM_MODEL_NAME, form);
		return mv;
	}

	// GET only: show the advanced search form.
	// POST is handled by advancedSearch() below. Mapping both methods here
	// collides with that handler under Spring 5.3+ (Ambiguous handler methods).
	@RequestMapping(value = "/product/product/advancedsearch", method = RequestMethod.GET)
	public ModelAndView advancedSearchPage(HttpServletRequest request,
			@ModelAttribute(ADVANCED_SEARCH_FORM_MODEL_NAME) AdvancedSearchProductForm form
		) throws Exception
	{
		log.debug("advancedSearchPage(): entered...");
		ModelAndView mv = new ModelAndView(advancedSearchViewName);

		if (null == form) {
			form = new AdvancedSearchProductForm();
		}

		List<LabelValueBean> editionsList = new ArrayList<LabelValueBean>();
		editionsList.add(new LabelValueBean("first Edition", "1"));
		editionsList.add(new LabelValueBean("second Edition", "2"));
		editionsList.add(new LabelValueBean("third Edition", "3"));
		editionsList.add(new LabelValueBean("fourth Edition", "4"));
		editionsList.add(new LabelValueBean("fith Edition", "5"));
		editionsList.add(new LabelValueBean("sixth Edition", "6"));

		mv.addObject(ADVANCED_SEARCH_FORM_MODEL_NAME, form);
		mv.addObject("editionsList", editionsList);

		return mv;
	}

	@RequestMapping(value = "/product/product/advancedsearch", method = RequestMethod.POST)
	public ModelAndView advancedSearch(HttpServletRequest request,
			@ModelAttribute(ADVANCED_SEARCH_FORM_MODEL_NAME) AdvancedSearchProductForm form) throws Exception
	{
		List <LabelValueBean> terms = new ArrayList<LabelValueBean>();

		log.debug("advancedSearch(): entered...");
		ModelAndView mv = new ModelAndView(searchViewName);

		if (form.getTitle().length() > 0 && form.getTitleCheck()) {
			terms.add(new LabelValueBean("Title", form.getTitle()));
		}

		if (form.getAuthor().length() > 0 && form.getAuthorCheck()) {
			terms.add(new LabelValueBean("Author", form.getAuthor()));
		}

		if (form.getEdition().length() > 0 && form.getEditionCheck()) {
			terms.add(new LabelValueBean("Edition", form.getEdition()));
		}

		if (form.getProductLine().length() > 0 && form.getProductLineCheck()) {
			terms.add(new LabelValueBean("ProductLine", form.getProductLine()));
		}

		if (form.getEditorCode().length() > 0 && form.getEditorCodeCheck()) {
			terms.add(new LabelValueBean("EditorCode", form.getEditorCode()));
		}
		if (form.getCopyrightYear().length() > 0 && form.getCopyrightYearCheck()) {
			terms.add(new LabelValueBean("CopyrightYear", form.getCopyrightYear()));
		}
		if (form.getPubStatus().length() > 0 && form.getPubStatusCheck()) {
			terms.add(new LabelValueBean("PubStatus", form.getPubStatus()));
		}

		// set up to feed search results controller.  This form will be fed to the standard search results processor
		SearchProductForm resultForm = new SearchProductForm();

		// If there are no search parameters in searchProductForm in the request,
		// then check the session, because for page 0 in ManageUserController,
		// the search parameters are put in a form in the session (the only place
		// safe from a redirect - and because it's a wizard controller it can't stick
		// the parameters onto the redirect)

		HttpSession session = request.getSession();

//		if (StringUtils.isBlank(form.getSearchString()) || StringUtils.isBlank(form.getSearchField())) {
//			AdvancedSearchProductForm sessionForm = (AdvancedSearchProductForm) session.getAttribute(ADVANCED_SEARCH_FORM_MODEL_NAME);
//			// fix NullPointerException on starting the app
//			if (null != sessionForm) {
//				form.setSearchString(sessionForm.getSearchString());
//				form.setSearchField(sessionForm.getSearchField());
//			}
//		}

		try {
			// This just makes sure that old results don't show up.
			form.setResults(null);

			if (terms.size() > 0) {
				Integer userId = PermUserContext.getCurrentUserId(request);
			//	String searchString = form.getSearchString();
				// remove any dashes from isbn or common work ID entered by user before we search
		//		if(form.getSearchField().equals("isbn") || form.getSearchField().equals("commonWork")) {
		//			searchString = StringUtil.removeAny(searchString, "-");
		//		}
				List<ProductSearchResult> results = getProductService().advancedSearchProducts(
						terms,
						userId,
						true);

				resultForm.setResults(results);
				UserSession userSession = PermUserContext.getUserSession(request);
				userSession.setProductSearchString("");
				userSession.setProductSearchField("ISBN");
			}

		}
		catch (ServiceException e) {
			log.error("search(): Error Searching Products", e);

			String msgKey = "product.searchProduct.errors.general";
			String msg = getMessageSource().getMessage(msgKey, null, null);
			request.setAttribute(GENERAL_MESSAGE, msg);
		}

		// reset search form because previous search data does not apply to advanced search
		resultForm.setSearchField("ISBN");
		resultForm.setSearchString("");
		mv.addObject(FORM_MODEL_NAME, resultForm);

		session.setAttribute(FORM_MODEL_NAME, resultForm);
		return mv;
	}


	/**
	 * Will load the product first if not in DB and attach it to current user
	 * @param request
	 * @param form
	 * @return
	 * @throws Exception
	 * TODO: CW - re-test after we implement load of a new product - I tested only with already loaded products
	 */
	@RequestMapping(value = "/product/product/addCWToUser", method ={RequestMethod.GET, RequestMethod.POST})
	public ModelAndView addCWToUser(HttpServletRequest request,
			@ModelAttribute(FORM_MODEL_NAME) SearchProductForm form)
			throws Exception
	{
		log.debug("addCWToUser(): entered...");

		String externalId = form.getSelectedProduct();
		String productDataSource = form.getSelectedProductDataSource();

		UserPrincipal userPrincipal = PermUserContext.getCurrentUser(request);

		try {
			commonWorkService.addProductToUserWatch(userPrincipal.getId(), externalId, productDataSource);
		}
		catch (ServiceException se) {
			request.setAttribute(GENERAL_MESSAGE, se.getMessage());
		}

		// repopulate the results
		HttpSession session = request.getSession();
		SearchProductForm form2 = (SearchProductForm) session.getAttribute(FORM_MODEL_NAME);
		if (null != form2) {
			form.setResults(form2.getResults());
		}

		// adjust result for product just added
		for (ProductSearchResult psr : form.getResults()) {
			if (externalId.equals(psr.getWid())) {
				psr.setWatched(true);
			}
		}

		String viewName = getTarget(form.getTarget(), getLandingViewName());
		log.debug("addCWToUser(): Going to viewName: " + viewName);
		ModelAndView mv = new ModelAndView(viewName);
		mv.addObject(FORM_MODEL_NAME, form);

		return mv;
	}

	/**
	 * For now we deal with CommonWork indirectly thru the primary product
	 * (This operation is available from userLanding and searchResults - in one case
	 * we show CommonWorks, in the other we show Products)
	 * If a product is unchecked in search results page, we remove the common work from
	 * WATCHED_CW
	 * @param request
	 * @param form
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/product/product/removeCWFromUser", method ={RequestMethod.GET, RequestMethod.POST})
	public ModelAndView removeCWFromUser(HttpServletRequest request,
			@ModelAttribute(FORM_MODEL_NAME) SearchProductForm form)
		throws Exception
	{
		log.debug("removeCWFromUser(): entered...");

		String externalId = form.getSelectedProduct();

		UserPrincipal userPrincipal = PermUserContext.getCurrentUser(request);

		commonWorkService.deleteCWFromUserWatch(userPrincipal.getId(), externalId);

		// repopulate the results
		HttpSession session = request.getSession();
		SearchProductForm form2 = (SearchProductForm) session.getAttribute(FORM_MODEL_NAME);
		if (null != form2) {
			form.setResults(form2.getResults());

			// adjust result for product just added
			for (ProductSearchResult psr : form.getResults()) {
				if (externalId.equals(psr.getWid())) {
					psr.setWatched(false);
				}
			}
		}

		ModelAndView mv = new ModelAndView(getTarget(form.getTarget(), landingViewName));
		mv.addObject(FORM_MODEL_NAME, form);

		return mv;
	}

	@RequestMapping(value = "/product/product/loadProduct", method =RequestMethod.POST)
	public ModelAndView loadProduct(HttpServletRequest request,
			@ModelAttribute(FORM_MODEL_NAME) SearchProductForm form)
			throws Exception
	{
		log.debug("loadProduct(): entered...");

		String externalId = form.getSelectedProduct();
		String productDataSource = form.getSelectedProductDataSource();

		Product product = productService.loadByExternalIdWithRefresh(externalId, productDataSource);

		if (null == product) {
			request.setAttribute(GENERAL_MESSAGE, "Could Not Find Product From PE");
			log.warn("loadProduct(): Could not find the product from PE: " + externalId);
		}

		//repopulate the results
		HttpSession session = request.getSession();
		SearchProductForm form2 = (SearchProductForm) session.getAttribute(FORM_MODEL_NAME);
		if (null != form2) {
			form.setResults(form2.getResults());
		}

        ModelAndView mv;

		if (product == null) {
			mv = new ModelAndView(searchViewName);
		}
		else {
			String viewName = getTarget(form.getTarget(), landingViewName);
			mv = new ModelAndView(viewName);
			mv.addObject("productId", product.getId());
		}

		mv.addObject(FORM_MODEL_NAME, form);

		return mv;
	}

	public String getAdminViewName() {
		return adminViewName;
	}

	public void setAdminViewName(String adminView) {
		this.adminViewName = adminView;
	}

	public String getSearchViewName() {
		return searchViewName;
	}

	public void setSearchViewName(String searchViewName) {
		this.searchViewName = searchViewName;
	}

	public ProductService getProductService() {
		return productService;
	}

	public void setProductService(ProductService productService) {
		this.productService = productService;
	}

	public CommonWorkService getCommonWorkService() {
		return commonWorkService;
	}

	public void setCommonWorkService(CommonWorkService commonWorkService) {
		this.commonWorkService = commonWorkService;
	}

	public String getLandingViewName() {
		return landingViewName;
	}

	public void setLandingViewName(String landingViewName) {
		this.landingViewName = landingViewName;
	}

	public String getAdminUserViewName() {
		return adminUserViewName;
	}

	public void setAdminUserViewName(String adminUserViewName) {
		this.adminUserViewName = adminUserViewName;
	}

	public String getUserLandingViewName() {
		return userLandingViewName;
	}

	public void setUserLandingViewName(String userLandingViewName) {
		this.userLandingViewName = userLandingViewName;
	}

	public void setAdvancedSearchViewName(String advancedSearchViewName) {
		this.advancedSearchViewName = advancedSearchViewName;
	}

	public String getAdvancedSearchViewName() {
		return advancedSearchViewName;
	}



	public ProductService getLocalProductService() {
		return localProductService;
	}

	public void setLocalProductService(ProductService localProductService) {
		this.localProductService = localProductService;
	}

	public ProductRepository getProductRepository() {
		return productRepository;
	}

	public void setProductRepository(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}
}
