package com.wiley.permissions.web.shared.controllers.landing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.wiley.permissions.common.transformer.ObjectToJson;
import com.wiley.permissions.common.transformer.XmlToObject;
import com.wiley.permissions.common.utils.Constants;
import com.wiley.permissions.domain.persistence.permissions.AssetUse;
import com.wiley.permissions.domain.persistence.permissions.CommonWork;
import com.wiley.permissions.domain.persistence.permissions.Component;
import com.wiley.permissions.domain.persistence.permissions.ComponentCategory;
import com.wiley.permissions.domain.persistence.permissions.DataSource;
import com.wiley.permissions.domain.persistence.permissions.ExportAsset;
import com.wiley.permissions.domain.persistence.permissions.ISBNData;
import com.wiley.permissions.domain.persistence.permissions.Medium;
import com.wiley.permissions.domain.persistence.permissions.PermissionStatus;
import com.wiley.permissions.domain.persistence.permissions.Privilege;
import com.wiley.permissions.domain.persistence.permissions.Product;
import com.wiley.permissions.domain.persistence.permissions.ProductCount;
import com.wiley.permissions.domain.persistence.permissions.Role;
import com.wiley.permissions.domain.persistence.permissions.User;
import com.wiley.permissions.domain.persistence.permissions.UserDefaults;
import com.wiley.permissions.domain.persistence.permissions.UserProfile;
import com.wiley.permissions.persistence.PersistenceException;
import com.wiley.permissions.repositories.AssetUseRepository;
import com.wiley.permissions.repositories.CommonWorkRepository;
import com.wiley.permissions.repositories.ConditionRepository;
import com.wiley.permissions.repositories.ProductRepository;
import com.wiley.permissions.repositories.UserRepository;
import com.wiley.permissions.security.web.UserPrincipal;
import com.wiley.permissions.services.AssetUseIndexService;
import com.wiley.permissions.services.AssetUseSearchResults;
import com.wiley.permissions.services.CommonWorkService;
import com.wiley.permissions.services.ContractService;
import com.wiley.permissions.services.LandingFilterForm;
import com.wiley.permissions.services.ProductIndexService;
import com.wiley.permissions.services.ProductService;
import com.wiley.permissions.services.util.ServiceException;
import com.wiley.permissions.services.view.InvoiceTabView;
import com.wiley.permissions.services.view.JSONDataTableView;
import com.wiley.permissions.web.shared.PermUserContext;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;
import com.wiley.permissions.web.shared.util.JSTLFunctions;
import com.wiley.sf.common.servlet.ServletUtil;

/**
 *
 * @author smarkoff
 */
@Controller
public class LandingController extends BaseAnnotatedController {

	// --------------------- static data ---------------------------------

	private final static Log log = LogFactory.getLog(LandingController.class);

	// session attribute name for latest cwId
	public final static String SESSION_CW_ID = "sessionCwId";
	public static final String ISBN13 = "isbn13";

	// --------------------- instance data -------------------------------

	private AssetUseRepository assetUseRepository;
	private ConditionRepository conditionRepository;
	private CommonWorkRepository commonWorkRepository;
	private UserRepository userRepository;
	private CommonWorkService commonWorkService;
	private ProductRepository productRepository;
	private ProductIndexService productIndexService;
	private ContractService contractService;
	private ProductService productService;
	private AssetUseIndexService assetUseIndexService; // Added for DM-534

	@RequestMapping(value="/landing/scroll", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView mainScroll(HttpServletRequest request,
		@RequestParam(value = "cwId", required = false) Integer cwId,
		@RequestParam(value = "productId", required = false) Integer productId,
		@RequestParam(value = ISBN13, required = false) String isbn13,
		@RequestParam(value = "pnumber", required = false) String pnumber
		) throws Exception
	{
		ModelAndView mv = main(request, cwId, productId, isbn13, pnumber);
		mv.addObject("scroll", "true");
		return mv;
	}

	public ModelAndView main(HttpServletRequest request,
		@RequestParam(value = "cwId", required = false) Integer cwId,
		@RequestParam(value = "productId", required = false) Integer productId,
		@RequestParam(value = ISBN13, required = false) String isbn13,
		@RequestParam(value = "pnumber", required = false) String pnumber
		) throws Exception
	{
		log.debug("main(): cwId = " + cwId + ", productId = " + productId + ", isbn13 = " + isbn13 +", pnumber = " + pnumber);

		if (request.getParameterMap().size() == 0) {
			log.debug("main(): no request parameters, will try to get cw_id from session");
			cwId = (Integer) request.getSession().getAttribute(SESSION_CW_ID);
		}

		if (cwId == null && productId == null && StringUtils.isBlank(isbn13) && StringUtils.isBlank(pnumber)) {
			log.info("main(): Going to throw exception because no valid identifier specified.\r\nAll request params: "
					+ ServletUtil.getAllParameters(request));
			throw new ServiceException("One of the following identifiers must be specified: cwId, productId, isbn13, pnumber.");
		}

		if (productId != null || StringUtils.isNotBlank(isbn13) || StringUtils.isNotBlank(pnumber)) {
			// loadProductWithRefresh will throw exception if product not found in DB or PE
			// TODO: we do not have the dataSource to pass to loadProductWithRefresh
			// - I use US for now. Probably should not do this. I might need to load from the index or something.
			Product product = getProductService().loadProductWithRefresh(productId, isbn13, pnumber, null, DataSource.US.getCode(), true);
			cwId = product.getCommonWork().getId();
		}

		// store the latest cwId, so we can return to same landing page
		request.getSession().setAttribute(SESSION_CW_ID, cwId);

		// -----------------------------------------
		boolean useFilter = applyFilter (request, cwId);

		CommonWork cw = commonWorkRepository.loadWithPrimaryProductById(cwId);  // throws PersistenceException
		PermUserContext.setCurrentCommonWork(request, cw);

		// cache the common work privileges
		UserPrincipal user = PermUserContext.getCurrentUser(request);
		user.setCwPrivileges(userRepository.getCwPrivileges(user.getId(), cw.getId()));

		UserDefaults ud = userRepository.loadUserDefaults(user.getId());
		if (null != ud && null != ud.getDateFormat()) {
			PermUserContext.setDateFormat(request, ud.getDateFormat());
		}

		Product primaryProduct = cw.getPrimaryProduct();

		// db way (could use index instead but this a little safer)
		boolean hasPreviousEdition = productRepository.doesPreviousEditionWIDExist(primaryProduct);
		boolean hasExportInProgress = userRepository.doesExportInProgress(cw.getId());

		boolean hasConditions = false;
		try {
			hasConditions = commonWorkService.initDefaultConditions(cwId);
		} catch (Exception e) {
			log.error("main(): failed to init default conditions", e);
		}
		boolean hasComponents = commonWorkRepository.hasComponents(cwId);

		ModelAndView mv = new ModelAndView(getFormView());
		mv.addObject("useFilter", useFilter);
		mv.addObject("cwId", cw.getId());
		mv.addObject("hasExportInProgress", hasExportInProgress);
		mv.addObject("hasPreviousEdition", hasPreviousEdition);
		mv.addObject("hasConditions", hasConditions);
		mv.addObject("hasComponents", hasComponents);

		mv.addObject("primaryProduct", primaryProduct);
		mv.addObject("interiorCWStatus", cw.getInteriorCWStatus());
		mv.addObject("coverCWStatus", cw.getCoverCWStatus());
		mv.addObject("permissionComplete", cw.isPermissionComplete());  // still need for now (/landing/main.jspx uses)

		return mv;
	}

	@RequestMapping(value="/landing/descriptionValidation", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView descriptionValidation(HttpServletRequest request,
			@RequestParam(value = "assetUseId", required = false) Integer assetUseId,
			@RequestParam(value = "pDescription", required = false) String pDescription,
			@RequestParam(value = "pType", required = false) String pType,
			@RequestParam(value = "pUsage", required = false) String pUsage,
			@RequestParam(value = "pComponent", required = false) String pComponent,
			@RequestParam(value = "pPosition", required = false) String pPosition
	) throws Exception
	{
		log.debug("descriptionValidation(): assetUseId = " + assetUseId);

		ModelAndView mv = new ModelAndView(getFormView());
		mv.addObject("assetUseId", assetUseId);
		log.debug("descriptionValidation(): after adding assetUseId = " + assetUseId);
		if (null != assetUseId && assetUseId != 0) {
			AssetUse au = getAssetUseRepository().loadAssetUseById(assetUseId);
			mv.addObject("pDescription", au.getAsset().getDescription());
			if (null != au.getComponent()) {
				mv.addObject("pComponent", au.getComponent().getName());
			} else {
				mv.addObject("pComponent", "");
			}
			mv.addObject("pType", pType);
			mv.addObject("pUsage", pUsage);
			mv.addObject("pComponent", pComponent);
			mv.addObject("pPosition", pPosition);
		} else {
			mv.addObject("pDescription", "");
			mv.addObject("pComponent", "previously inserted row");
			mv.addObject("pPosition", "");
			mv.addObject("pType", pType);
			mv.addObject("pUsage", pUsage);
			mv.addObject("pComponent", pComponent);
			mv.addObject("pPosition", pPosition);
		}

		return mv;
	}

	@RequestMapping(value="/landing/invoiceList", method = {RequestMethod.GET, RequestMethod.POST})
	public void listInvoices(HttpServletRequest request, HttpServletResponse response,
			@RequestParam("cwId") int cwId)
	throws Exception
	{
		log.debug("listInvoices(): cwId = " + cwId);
		String filter = PermUserContext.getCurrentUser(request).getCustomFilter();

		boolean includeCovers = request.isUserInRole(Role.EMPLOYEE_DEFAULT.getCode()) ||
								request.isUserInRole(Privilege.COVER_ASSETS.getCode());
		if (!includeCovers)
			filter = UserDefaults.INTERNAL_FILTER;
		List<InvoiceTabView> invoicesList = assetUseRepository.loadInvoiceTabView(cwId, filter);
		JSONDataTableView aaData = new JSONDataTableView();
		aaData.setTableRows(invoicesList);
		String json = ObjectToJson.doTransform(aaData, null);
		//log.debug("listInvoices(): json: " + json);

        response.setContentType("application/json");
        PrintWriter writer = response.getWriter();  // throws IOException
        writer.println(json);
	}

	@RequestMapping(value="/landing/saveNotes", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView saveNotes(HttpServletRequest request,
			@RequestParam("cwId") int cwId,
			@RequestParam("notes") String notes) throws PersistenceException
	{
		log.debug("saveNotes(): cwId = " + cwId);

		if (StringUtils.isBlank(notes))  notes = null;

		commonWorkService.saveCwNotes(cwId, notes);

		ModelAndView mv = new ModelAndView("redirect:/sapp/panels/summaryPanel?cwId=" + cwId);

		return mv;
	}

	//Start: Added for DM-534
		@RequestMapping(value="/landing/exportAssetPre", method = {RequestMethod.GET, RequestMethod.POST})
		public void exportAssetPre(HttpServletRequest request, HttpServletResponse response,
			@RequestParam(value = "uid", required = false) Integer uId,@RequestParam(value = "cwId", required = false) Integer cwId
			) throws Exception
		{
			if (null == uId) uId = Constants.SYSTEM_USER_ID;
			int totalAssetsToExport = 0;
			log.debug("exportAssetPre(): uid = " + uId);
			String output = null;
			ExportAsset exportstatus = commonWorkRepository.loadUserexportStatus(cwId,"in_progress");
			if(exportstatus !=null) {
			if(null !=exportstatus.getExportStatus() && exportstatus.getExportStatus().equals("in_progress")) {
				User user=userRepository.loadUserById(exportstatus.getUserId());
				log.debug("exportAssetPre(): username = " + user.getFullName());
				output="This Title cannot be edited until the Asset Export process completes. Please contact "+user.getFullName()+" to check the status of Asset Export Process";//Export in Progress
			}
			else {
				output="msg1";
			}
			}else {
				output="msg1";
			}
			AssetUseSearchResults results = assetUseIndexService.getCWAssetUsesFromIndexForExcel(cwId,false, false, false);
			totalAssetsToExport=results.getDocuments().size();
			if(totalAssetsToExport==0)
			{
				output=getMessageSource().getMessage("asset.export.alert", null, null);;
			}
			response.setContentType("text/plain");
	        response.setContentLength(output.length());
			PrintWriter writer = response.getWriter();
			writer.write(output);
			writer.close();

		}

		@RequestMapping(value="/landing/displayCount", method = {RequestMethod.GET, RequestMethod.POST})
		public void exportCompletedCount(HttpServletRequest request, HttpServletResponse response,
				@RequestParam(value = "cwId", required = false) Integer cwId
			) throws Exception
		{

			String output = "";
			try{
			ExportAsset exportstatus = commonWorkRepository.loadexportCount(cwId);
			if(exportstatus !=null) {
				log.debug("ID------------>"+exportstatus.getId());
				output=exportstatus.getCompleted()+" out of "+exportstatus.getTotalCount()+ " Asset Export process completed.";
			}
			}
			catch(Exception ex)
			{
			ex.printStackTrace();
			output="Asset in Progress Please contact Technical team";
			}
			response.setContentType("text/plain");
	        response.setContentLength(output.length());
			PrintWriter writer = response.getWriter();
			writer.write(output);
			writer.close();

		}

		//End: Added for DM-534


	@RequestMapping(value="/landing/longSummary", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView longSummary(HttpServletRequest request,
			@RequestParam("cwId") int cwId,
			@RequestParam(value = "productId", required = false) Integer productId)
	throws Exception
	{
		log.debug("longSummary(): cwId = " + cwId + ", productId = " + productId);
		Product product = null;
		ModelAndView mv = new ModelAndView("pages.landing.longSummary");
		String mediumCodeType = null;
		ProductCount pc = null;
		int totalPrintings = 0;
		int grossUnits = 0;

		// load the cw members
		List<Product> cwMembers = productRepository.executeMultiResultNamedQuery("Product.findProductsByCW", new Object[] {cwId});
		//Start: Added for DM-292
		DefaultHttpClient httpClient = null;
		if(null != cwMembers && cwMembers.size() > 0) {
			httpClient = new DefaultHttpClient();
		}
		//End: Added for DM-292
		for (Product p : cwMembers) {
			if (null == productId && p.isCwPrimary()) {
				productId = p.getId();
			}
			//DM-292
			if(null != p.getMedium()) {
				mediumCodeType = Medium.getMediumType(p.getMedium().getCode());
				if (null != mediumCodeType && mediumCodeType.equals("P")) {
					//Now we will be getting the count by directly going to CORE as per DM-292
					pc = getProductCount(p.getIsbn13(), mediumCodeType, httpClient);
					if (null != pc) {
						log.debug("In longSummary(): Commonwork Members - Product Count String = " + pc.toString());
						totalPrintings += pc.getCount();
						p.setEbookSales(pc.getCount());// we use the ebookSales just as a place holder to be displayed in JSP
						p.setEbookFromBIorCore(pc.getCount().toString());
					} else {
						p.setEbookFromBIorCore("Title not available");
					}
				} else if (null != mediumCodeType && mediumCodeType.equals("E")) {
					ISBNData ebookData = productRepository.getEBookCount(p.getIsbn13());
					if(null != ebookData) {
						totalPrintings += ebookData.getGrossUnits();
						p.setEbookFromBIorCore(ebookData.getGrossUnits().toString());
					} else {
						p.setEbookFromBIorCore("Title not available");
					}
					//totalPrintings += ebookCount;

				}
			} else {
				grossUnits = ((Number)productRepository.executeSingleResultNamedQuery("Product.getGrossUnits", new Object[] {p.getId()})).intValue();
				totalPrintings += grossUnits;
				p.setEbookSales(grossUnits);
				p.setEbookFromBIorCore(grossUnits+"");

			}
			//DM-292
		}
		//Start: Added for DM-292
		if(null != cwMembers && cwMembers.size() > 0) {
			httpClient.getConnectionManager().shutdown();
		}
		//End: Added for DM-292

		// load the related products
		List<Product> customPubs = productRepository.executeMultiResultNamedQuery("Product.findRelatedCustomPublications", new Object[] {cwId});
		//Start: Added for DM-292
		if(null != customPubs && customPubs.size() > 0) {
			httpClient = new DefaultHttpClient();
		}
		//End: Added for DM-292
		for (Product p : customPubs) {
			//DM-292
			if(null != p.getMedium()) {
				mediumCodeType = Medium.getMediumType(p.getMedium().getCode());
				if (null != mediumCodeType && mediumCodeType.equals("P")) {
					pc = getProductCount(p.getIsbn13(), mediumCodeType, httpClient);
					if (null != pc) {
						log.debug("In longSummary(): Custom Publications - Product Count String = " + pc.toString());
						totalPrintings += pc.getCount();
						p.setEbookSales(pc.getCount());
						p.setEbookFromBIorCore(pc.getCount().toString());
					} else {
						p.setEbookFromBIorCore("Title not available");
					}
				} else if (null != mediumCodeType && mediumCodeType.equals("E")) {
					ISBNData ebookData = productRepository.getEBookCount(p.getIsbn13());
					if(null != ebookData) {
						totalPrintings += ebookData.getGrossUnits();
						p.setEbookFromBIorCore(ebookData.getGrossUnits().toString());
					} else {
						p.setEbookFromBIorCore("Title not available");
					}
				}
			} else {
				grossUnits = ((Number)productRepository.executeSingleResultNamedQuery("Product.getGrossUnits", new Object[] {p.getId()})).intValue();
				totalPrintings += grossUnits;
				p.setEbookSales(grossUnits);
				p.setEbookFromBIorCore(grossUnits+"");

			}
			//DM-292
		}
		//Start: Added for DM-292
		if(null != customPubs && customPubs.size() > 0) {
			httpClient.getConnectionManager().shutdown();
		}
		//End: Added for DM-292

		// load the primary product
		product = productRepository.lazyLoad(Product.class, productId, new String[] {"users", "lastUpdatedUser", "developmentEditor", "publicationStatus",
			"edition", "businessUnit", "productLine", "productFamily", "productType", "subMedium"});

		List<Product> familyMembers;
		if (product.getProductFamily() == null) {
			familyMembers = new ArrayList<Product>(0);
		} else if (product.getEdition() == null){
			// load the family members
			familyMembers = productRepository.executeMultiResultNamedQuery("Product.findFamilyMembers",
				new Object[] {product.getProductFamily().getCode(), product.getId()});
		} else {
			// load the family edition members
			familyMembers = productRepository.executeMultiResultNamedQuery("Product.findEditionMembers",
				new Object[] {product.getProductFamily().getCode(), product.getEdition().getId(), product.getId()});
		}

		for (Product p : familyMembers) {
			// we use the ebookSales just as a place holder to be displayed in JSP
			// calling setEbookSales here does NOT update the database (which is important) because we are not binded to the JPA context here
			p.setEbookSales (((Number)productRepository.executeSingleResultNamedQuery("CommonWork.getTotalAssetCount",
				new Object[] {p.getCommonWork().getId()})).intValue());
		}

		// Create link for BPM if the user is an employee (has an SSO login)
		String bpmUrl = "";
		Integer userId = PermUserContext.getCurrentUserId(request);
		try {
			User user = productRepository.getUserRepository().loadUserById(userId);

			// Don't include SYSTEM because those users won't have an SSO login
			if (user.getType() == User.Type.EMPLOYEE) {
				bpmUrl = product.getBPMUrl();
			}
		}
		catch (Exception ex) {
			log.debug("longSummary(): caught exception dealing with bpmUrl: ", ex);
		}

		mv.addObject("bpmUrl", bpmUrl);

		mv.addObject("cwMembers", cwMembers);
		mv.addObject("customPubs", customPubs);
		mv.addObject("familyMembers", familyMembers);
		//We are going to sum the values got from CORE and assigning to totalPrintings.
		mv.addObject("totalPrintings", totalPrintings);
		mv.addObject("product", product);

		return mv;
	}

	//Start: Added to implement DM-292
	private ProductCount getProductCount(String isbn, String productType, DefaultHttpClient httpClient) throws Exception {
		try {
			HttpGet getRequest = new HttpGet(JSTLFunctions.getProdCountURL()+"+isbn13(a0130):"+isbn+"+prodtyp(a0010):"+productType);
			getRequest.addHeader("accept", "text/html");

			HttpResponse response = httpClient.execute(getRequest);

			if (response.getStatusLine().getStatusCode() != 200) {
				throw new RuntimeException("Failed : HTTP error code : " + response.getStatusLine().getStatusCode());
			}

			BufferedReader br = new BufferedReader(new InputStreamReader((response.getEntity().getContent())));

			String output;
			StringBuilder outputXML = new StringBuilder();
			while ((output = br.readLine()) != null) {
				outputXML.append(output);
			}
			return (ProductCount) XmlToObject.xmlToObject(ProductCount.class, outputXML.toString());

		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	//End: Added to implement DM-292

	@RequestMapping(value="/landing/viewRowTemplate", method = {RequestMethod.GET, RequestMethod.POST})
	public void viewRowTemplate(HttpServletRequest request, HttpServletResponse response,
		@RequestParam(value = "uid", required = false) Integer uId,@RequestParam(value="cwId" , required=false)Integer cwId) throws Exception
	{
		if (null == uId) uId = Constants.SYSTEM_USER_ID;

		log.debug("viewRowTemplate(): uid = " + uId);

		List<UserProfile> profile = userRepository.loadProfile(uId);
		String dTemplate = buildViewRowTemplate(profile,cwId);
		//log.debug("viewRowTemplate(): output:\r\n" + dTemplate);

		response.setContentType("text/plain");
		// Safe to set content-length here since no double-byte chars in the template
		response.setContentLength(dTemplate.length());
		response.getWriter().write(dTemplate); // throws IOException
	}

	@RequestMapping(value="/landing/editRowTemplate", method = {RequestMethod.GET, RequestMethod.POST})
	public void editRowTemplate(HttpServletRequest request, HttpServletResponse response,
		@RequestParam(value = "uid", required = false) Integer uId
		) throws Exception
	{
		if (null == uId) uId = Constants.SYSTEM_USER_ID;

		log.debug("editRowTemplate(): uid = " + uId);

		List<UserProfile> profile = userRepository.loadProfile(uId);


		String dTemplate = buildEditRowTemplate(profile);

		response.setContentType("text/plain");
		// Safe to set content-length here since no double-byte chars in the template
		response.setContentLength(dTemplate.length());
		response.getWriter().write(dTemplate); // throws IOException
	}

	private String buildViewRowTemplate(List<UserProfile> profile,Integer cwId) {
		int sourceIndex = 0;
		int statusIndex = 0;
		int descriptionIndex = 0;
		int creditLineIndex = 0;
		int sortOrderIndex = 0;
		int gbpmIndex = 0;

		//Added for DM-534 to disable the checkboxes when asset export in progress
		String assetInProgressClass = "enable";
		boolean hasExportInProgress = userRepository.doesExportInProgress(cwId);
		if(hasExportInProgress)
		{
			assetInProgressClass="disablednoline";
		}

		String isCanceledIndex = "aData.length - 1";
		String ownerTypeIndex = "aData.length - 2";
		String isDisabledIndex = "aData.length - 3";
		String paymentRequiredIndex = "aData.length - 4";
		String isCustomIndex = "aData.length - 5";
		String isDownloadContractIndex = "aData.length - 7";
		String isPaidIndex = "aData.length - 8";
		String hasCommentsIndex = "aData.length - 11";
		String hasThumbnailIndex = "aData.length - 12";
		String hasContractsIndex = "aData.length - 13";

		/*
		 * Ensure that we know the sourceIndex before we get to the logic
		 * below for the Status column (which makes use of sourceIndex).
		 * Without this loop here, if the Status column is before the Sources
		 * column then sourceIndex will not be set in time.
		 */
		for (int y=0; y < profile.size(); y++) {
			UserProfile pfTest = profile.get(y);
			if (pfTest.getTitle().equals("Sources")) {
				sourceIndex = pfTest.getSortOrder();
			}
		}

		String dTemplate =
			"	<td class='{ $P.profile[0][\"sClass\"] }'> " +
			"		<input type=\"checkbox\" name=\"auList\" id=\"auList\" class=\"enable\" value=\"{ $P.aData[0] }\" {oSelector.fnIsChecked ($P.aData[0]) } />	" +
					// Code change for upload button in datatable
			"		<img id=\"{ $P.aData[0] }\" class=\"uploadAssetUse "+assetInProgressClass+"\" src=\"../../images/buttons/upload_icon.png\" height=\"17\"/>	" +
			"		<img id=\"{ $P.aData[0] }\" class=\"editAssetUse "+assetInProgressClass+"\" src=\"../../images/buttons/edit-icon.png\" height=\"17\"/>	" +
			"		{#if $P.aData[$P." + isDownloadContractIndex + "] == true}	" +
			"			<img id=\"{ $P.aData[0] }\" class=\"downloadContract enable\" src=\"../../images/invoice_icon.png\" height=\"17\"/>	" +
			"		{#/if}	" +
			"       {#if $P.aData[$P." + hasCommentsIndex + "] == true}	" +
			" 			<img id=\"{ $P.aData[0] }\" class=\"showComments enable\" src=\"../../images/comments_icon.png\" eight=\"17\"/>	" +
			"		{#/if}	" +
			"       {#if $P.aData[$P." + hasThumbnailIndex + "] == true}	" +
			" 			<img id=\"{ $P.aData[0] }\" class=\"showThumbnail enable\" src=\"../../images/thumbnail_thumbnail.png\" eight=\"17\"/>	" +
			"		{#/if}	" +
			"	</td>	";

		for (int x=0; x < profile.size(); x++) {
			boolean processed = false;
			UserProfile pf = profile.get(x);

			if (pf.getTitle().equals("Sources")) {
				processed = true;
				sourceIndex = pf.getSortOrder();
				dTemplate = dTemplate +
				"	{#if $P.profile[" + sourceIndex + "][\"bVisible\"] == true }	" +
				"	<td class='{ $P.profile[" + sourceIndex + "][\"sClass\"] }' title=\"{ $P.aData[" + sourceIndex + "].formatSourceText() }\">	" +
				"		{#if $P.aData[" + sourceIndex + "] != '' && $P.aData[" + sourceIndex + "] != '---'}	" +
				"			{ $P.aData[" + sourceIndex + "].formatSourceHTML() } | <a href=\"#\" id=\"{ $P.aData[0] }\" class=\"wizard\">Change</a>	" +
				"		{#/if}	" +
				"		{#if $P.aData[" + sourceIndex + "] == ''} " +
				"			{#if $P.aData[$P." + ownerTypeIndex + "] == 'Author Owned'}	" +
				"				<i>Author</i> | <a href=\"#\" id=\"{ $P.aData[0] }\" class=\"wizard\">Change</a>	" +
				"			{#else}	" +
				"				{#if $P.aData[$P." + ownerTypeIndex + "] == 'Illustration Request'}	" +
				"					<i>Illustration Request</i> | <a href=\"#\" id=\"{ $P.aData[0] }\" class=\"wizard\">Change</a>	" +
				"				{#else}	" +
				"					{#if $P.aData[$P." + ownerTypeIndex + "] == 'Photo Request'}	" +
				"						<i>Photo Request</i> | <a href=\"#\" id=\"{ $P.aData[0] }\" class=\"wizard\">Change</a>	" +
				"					{#else}	" +
				"						{#if $P.aData[$P." + ownerTypeIndex + "] == 'Wiley Owned' || $P.aData[$P." + ownerTypeIndex + "] == 'Work For Hire'}	" +
				"							<i>Wiley Owned</i> | <a href=\"#\" id=\"{ $P.aData[0] }\" class=\"wizard\">Change</a>	" +
				"						{#else}	" +
				"							{#if $P.aData[$P." + ownerTypeIndex + "] == 'Public Domain'}	" +
				"								<i>Public Domain</i> | <a href=\"#\" id=\"{ $P.aData[0] }\" class=\"wizard\">Change</a>	" +
				"							{#else}	" +
				"								{#if $P.aData[$P." + ownerTypeIndex + "] == 'Wiley Created'}	" +
				"									<i>Wiley Created</i> | <a href=\"#\" id=\"{ $P.aData[0] }\" class=\"wizard\">Change</a>	" +
				"								{#else}	" +
				"									<a href=\"#\" id=\"{ $P.aData[0] }\" class=\"wizard\">Enter details</a>	" +
				"								{#/if}	" +
				"							{#/if}	" +
				"						{#/if}	" +
				"					{#/if}	" +
				"				{#/if}	" +
				"			{#/if}	" +
				"		{#/if}	" +
				"	</td>	" +
				"	{#/if}	";
			}

			if (pf.getTitle().equals("Status")) {
				processed = true;
				statusIndex = pf.getSortOrder();
				dTemplate = dTemplate +
				"	{#if $P.profile[" + statusIndex + "][\"bVisible\"] == true }	" +
				"	<td class='{ $P.profile[" + statusIndex + "][\"sClass\"] }'>	" +
				"		{#if " + generateStatusIfStatementForTemplate (statusIndex, PermissionStatus.PROBLEM_GROUP) + "} " +
				"			<span id=\"{ $P.aData[0] }\" class=\"tooltip\">{ $P.aData[" + statusIndex + "] }</span>	" +
				// Start : Added for DM-1606
				"		{#else}	" +
				"			{#if " + generateStatusIfStatementForTemplate (statusIndex, new PermissionStatus[] {PermissionStatus.FORM_WAITING_ON_INVOICE}) + "}	" +
				"				<a href=\"#\" id=\"{ $P.aData[0] }\" class=\"downloadPdf enable\" title=\"{ $P.aData[" + statusIndex + "].tooltip() }\">Waiting on Invoice</a>	" +
				// End : Added for DM-1606
				"			{#else}	" +
				"				{#if " + generateStatusIfStatementForTemplate (statusIndex, PermissionStatus.FORM_SENT_GROUP)+ "}	" +
				"					<a href=\"#\" id=\"{ $P.aData[0] }\" class=\"downloadPdf enable\" title=\"{ $P.aData[" + statusIndex + "].tooltip() }\">Request Sent</a>	" +
				"			{#else}	" +
				"				{#if " + generateStatusIfStatementForTemplate (statusIndex, new PermissionStatus[] {PermissionStatus.AMENDMENT_SENT}) + "}	" +
				"					<a href=\"#\" id=\"{ $P.aData[0] }\" class=\"downloadAmendment\" title=\"{ $P.aData[" + statusIndex + "].tooltip() }\">Amendment Sent</a>	" +
				"				{#else}	" +
				"					<span title=\"{ $P.aData[" + statusIndex + "].tooltip() }\">{ $P.aData[" + statusIndex + "] }</span>	" +
				"					{#/if}	" +
				"				{#/if}	" +
				"			{#/if}	" +
				"		{#/if}	" +
				"		{#if (!$P.aData[$P." + isDisabledIndex + "] && !$P.aData[$P." + isCanceledIndex + "]) }	" +
				"			{#if " + generateStatusIfStatementForTemplate (statusIndex, PermissionStatus.UNREQUESTED_GROUP) + "}	" +
				"				| <a href=\"#\" id=\"{ $P.aData[0] }\" class=\"permissions\">Get Permissions</a>	" +
				"			{#/if}	" +
				"			{#if " + generateStatusIfStatementForTemplate (statusIndex, new PermissionStatus[] {PermissionStatus.GRANTED_WORK_FOR_HIRE}) + "}	" +
				"				{#if ($P.aData[$P." + hasContractsIndex + "] == true)} " +
				"					| <a href=\"#\" id=\"{ $P.aData[0] }\" class=\"editContractDetails enable\">View/Edit</a> " +
				"				{#/if}	" +
				"			{#/if}	" +
				"			{#if " + generateStatusIfStatementForTemplate (statusIndex, PermissionStatus.FORM_SENT_GROUP) + "}	" +
				"				{#if $P.aData[$P." + isCustomIndex + "]}	" +
				"					| <a href=\"#\" id=\"{ $P.aData[0] }\" class=\"customContractDetails\">Enter grant details</a>	" +
				"				{#else}	" +
				"					| <a href=\"#\" id=\"{ $P.aData[0] }\" class=\"contractDetails\">Enter grant details</a>	" +
				"				{#/if}	" +
				"			{#/if}	" +
				" 			{#if " + generateStatusIfStatementForTemplate (statusIndex, PermissionStatus.MIGRATED_GROUP) + "}	" +
				"              {#if ($P.aData[$P." + hasContractsIndex + "] != true && $P.aData[" + sourceIndex + "] != '' && " + generateStatusIfStatementForTemplateNotEqual(statusIndex, new PermissionStatus[] {PermissionStatus.MIGRATED_FROM_AUSTRALIA}) + ")} " +
				"					| <a href=\"#\" id=\"{ $P.aData[0] }\" class=\"contractDetails\">Enter grant details</a>	" +
				"			   {#/if}	" +
				"			{#/if}	" +
				"			{#if ($P.aData[$P." + hasContractsIndex + "] == true && " + generateStatusIfStatementForTemplateNotEqual(statusIndex, new PermissionStatus[] {PermissionStatus.MIGRATED_FROM_AUSTRALIA}) + ")} " +
				"				| <a href=\"#\" id=\"{ $P.aData[0] }\" class=\"editContractDetails "+assetInProgressClass+"\">View/Edit</a> " +
				"			{#/if}	" +
				"			{#if " + generateStatusIfStatementForTemplate (statusIndex, new PermissionStatus[] {PermissionStatus.AMENDMENT_SENT}) + "}" +
				"				| <a href=\"#\" id=\"{ $P.aData[0] }\" class=\"uploadAmendment\">Upload Form</a>	" +
				"			{#/if}	" +
				"			{#if " + generateStatusIfStatementForTemplate (statusIndex, PermissionStatus.REREQUEST_GROUP) + "}	" +
				"				{#if $P.aData[$P." + isCustomIndex + "]} " +
				"					| <a href=\"#\" id=\"{ $P.aData[0] }\" class=\"reinvoice\">New Contract</a>	" +
				"				{#else}	" +
				"                   {#if " + generateStatusIfStatementForTemplate (statusIndex, new PermissionStatus[] {PermissionStatus.GRANTED_RF_UNLIMITED}) + "}	" +
				"					{#else}	" +
				"					     | <a href=\"#\" id=\"{ $P.aData[0] }\" class=\"rerequest\">Re-request</a>	" +
				"				   {#/if}	" +
				"				{#/if}	" +
				"			{#/if}	" +
				// Added for SS Task 12 - Start
				"			{#if " + generateStatusIfStatementForTemplate (statusIndex, new PermissionStatus[] {PermissionStatus.CONTRACT_INSUFFICIENT}) + "}" +
				"				| <a href=\"#\" id=\"{ $P.aData[0] }\" class=\"approveDetails\">Approve</a> " +
				"			{#/if}	" +
				// Added for SS Task 12	- End
				//Added for Paperwork Task B Public Domain Starts
				"			{#if " + generateStatusIfStatementForTemplate (statusIndex, new PermissionStatus[] {PermissionStatus.GRANTED_PUBLIC_DOMAIN}) + "}" +
				"			{#/if}	" +
				//Added for Paperwork task B Public Domain Ends
				"			{#if ($P.aData[$P." + paymentRequiredIndex + "]) } " +
				"				| <a href=\"#\" id=\"{ $P.aData[0] }\" class=\"pay\">Pay</a> " +
				"			{#/if}	" +
				"			{#if ($P.aData[$P." + isPaidIndex + "]) } " +
				"				| <a href=\"#\" id=\"{ $P.aData[0] }\" class=\"paid\">Paid</a> " +
				"			{#/if}	" +
				"		{#/if}		" +
				"	</td>	" +
				"	{#/if}	";
			}

			if (pf.getTitle().equals("Description")) {
				processed = true;
				descriptionIndex = pf.getSortOrder();
				dTemplate = dTemplate +
				"	{#if $P.profile[" + descriptionIndex + "][\"bVisible\"] == true }													" +
				"	<td class='{ $P.profile[" + descriptionIndex + "][\"sClass\"] }' title=\"{ $P.aData[" + descriptionIndex + "] }\">	" +
				"		{#if $P.aData[" + descriptionIndex + "] != null }																" +
				"			{#if ($P.aData[$P." + isDisabledIndex + "]) }																" +
				"				<span class=\"disabled\">{ $P.aData[" + descriptionIndex + "].truncate(30) }</span>						" +
				"			{#else}																										" +
				"				{ $P.aData[" + descriptionIndex + "].truncate(30) }														" +
				"			{#/if}	" +
				"		{#/if}	" +
				"	</td>		" +
				"	{#/if}		";
			}





			if (pf.getTitle().equals("Credit Line")) {
				processed = true;
				creditLineIndex = pf.getSortOrder();
				dTemplate = dTemplate +
				"	{#if $P.profile[" + creditLineIndex + "][\"bVisible\"] == true }	" +
				"	<td class='{ $P.profile[" + creditLineIndex + "][\"sClass\"] }'>	" +
				"		{#if $P.aData[" + creditLineIndex + "] != null }				" +
				"			{ $P.aData[" + creditLineIndex + "].truncate(30) }			" +
				"		{#/if}		" +
				"	</td>			" +
				"	{#/if}			" ;
			}

			if (pf.getTitle().equals("Asset Order")) {
				processed = true;
				sortOrderIndex = pf.getSortOrder();
				dTemplate = dTemplate +
				"	{#if $P.profile[" + sortOrderIndex + "][\"bVisible\"] == true }		" +
				"	<td class='{ $P.profile[" + sortOrderIndex + "][\"sClass\"] }'>		" +
				"		{#if $P.aData[" + sortOrderIndex + "] != 'null' }				" +
				"			{ $P.aData[" + sortOrderIndex + "]}							" +
				"		{#/if}														" +
				"	</td>	" +
				"	{#/if}	" ;
			}

			if (pf.getTitle().equals("GBPM Category")) {
				processed = true;
				gbpmIndex = pf.getSortOrder();
				dTemplate = dTemplate +
				"	{#if $P.profile[" + gbpmIndex + "][\"bVisible\"] == true }		" +
				"	<td class='{ $P.profile[" + gbpmIndex + "][\"sClass\"] }'>		" +
				"		{#if $P.aData[" + gbpmIndex + "] != '0' }				    " +
				"			{ $P.aData[" + gbpmIndex + "]}							" +
				"		{#/if}														" +
				"	</td>	" +
				"	{#/if}	" ;
			}


			if (!processed) {
				dTemplate = dTemplate +
				"	{#if $P.profile[" + pf.getSortOrder() + "][\"bVisible\"] == true }												" +
				"	<td class='{ $P.profile[" + pf.getSortOrder() + "][\"sClass\"] }'>{ $P.aData[" + pf.getSortOrder() + "] }</td>	" +
				"	{#/if}	";
			}
		}
        //log.debug("buildViewRowTemplate(): dTemplate: " + dTemplate);
		return dTemplate;
	}

	private String generateStatusIfStatementForTemplate(int statusIndex, PermissionStatus[] statuses) {
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < statuses.length; i++) {
			PermissionStatus status = statuses[i];
			sb.append(" $P.aData[" + statusIndex + "] == '" + status.getDescription() + "'");
			// if not last, add ||
			if (i + 1 < statuses.length) {
				sb.append (" || ");
			}
		}
		return sb.toString();
	}

	private String generateStatusIfStatementForTemplateNotEqual (int statusIndex, PermissionStatus[] statuses) {
		StringBuilder buffer = new StringBuilder ("");

		for (int i = 0; i < statuses.length; i++) {
			PermissionStatus status  = statuses[i];
			buffer.append(" $P.aData[" + statusIndex + "] != '" + status.getDescription() + "'");
			log.debug("Description Output-->"+status.getDescription());
			// if not last, add ||
			if ((i+1) < statuses.length) {
				buffer.append (" || ");
			}
		}
		log.debug("some string output--->"+buffer.toString());
		return buffer.toString();
	}

	private String buildEditRowTemplate(List<UserProfile> profile) {
		// (6/2014) Note the UserProfile class/table has a field/column called "editable"
		// but we currently are not looking at this flag in this method - we could but
		// at least currently it's not necessary since generally a field is either editable
		// or not for all users and all situations (at least for the landing table).

		String dTemplate =
			"<td class='{ $P.profile[0][\"sClass\"] }'> " +
			"<input type=\"checkbox\" name=\"auList\" id=\"auList\" value=\"{ $P.aData.assetUseId }\" {oSelector.fnIsChecked ($P.aData.assetUseId) } /> " +
			"</td> ";

		for (int x=0; x < profile.size(); x++) {
			UserProfile pf = profile.get(x);
			if (pf.getTitle().equals("Component")) {
				dTemplate = dTemplate +
				"{#if $P.profile[" + pf.getSortOrder() + "][\"bVisible\"] == true }  " +
				"<td> " +
				"	<select name=\"componentId\"> " +
				"		<option value='0'>--</option> " +
				"		{#foreach $P.aData.components as component} " +
				"			{#if $T.component.id == $P.aData.componentId } " +
				"				<option selected='selected' value='{$T.component.id}'>{$T.component.name}</option> " +
				"			{#else} " +
				"				<option value='{$T.component.id}'>{$T.component.name}</option> " +
				"			{#/if}  " +
				"		{#/for} " +
				"	</select> " +
				"</td> " +
				"{#/if} " ;
			}

			if (pf.getTitle().equals("MS Page")) {
				dTemplate = dTemplate +
				"{#if $P.profile[" + pf.getSortOrder() + "][\"bVisible\"] == true }  " +
				"<td> " +
				"	<input type=\"text\" name=\"manuscriptPage\" value=\"{ $P.aData.manuscriptPage }\" size=\"5\"/> " +
				"</td> " +
				"{#/if} " ;
			}

			if (pf.getTitle().equals("Media Type")) {
				dTemplate = dTemplate +
				"{#if $P.profile[" + pf.getSortOrder() + "][\"bVisible\"] == true }  " +
				"<td> " +
				"	<select name=\"mediaTypeCode\" style=\"width:100px\"> " +
				"		<option value=''>--</option> " +
				"		{#foreach $P.aData.mediaTypes as mediaType}  " +
				"			{#if $T.mediaType.code == $P.aData.mediaTypeCode } " +
				"				<option selected='selected' value='{$T.mediaType.code}'>{$T.mediaType.description}</option>  " +
				"			{#else} " +
				"				<option value='{$T.mediaType.code}'>{$T.mediaType.description}</option>  " +
				"			{#/if}  " +
				"		{#/for} " +
				"	</select> " +
				"</td> " +
				"{#/if} ";
			}

			if (pf.getTitle().equals("Description")) {
				dTemplate = dTemplate +
				"{#if $P.profile[" + pf.getSortOrder() + "][\"bVisible\"] == true }  " +
				"<td> " +
				"	<input type=\"text\" name=\"description\" id=\"description\" value=\"{ $P.aData.description }\" size=\"20\"/> " +
				"</td> " +
				"{#/if} " ;
			}

			if (pf.getTitle().equals("Usage")) {
				dTemplate = dTemplate +
				"{#if $P.profile[" + pf.getSortOrder() + "][\"bVisible\"] == true }  " +
				"<td> " +
				"	<select name=\"usageCode\" style=\"width:100px\"> " +
				"		<option value=''>--</option> " +
				"		{#foreach $P.aData.usages as usage}  " +
				"			{#if $T.usage.code == $P.aData.usageCode } " +
				"				<option selected='selected' value='{$T.usage.code}'>{$T.usage.description}</option>  " +
				"			{#else} " +
				"				<option value='{$T.usage.code}'>{$T.usage.description}</option>  " +
				"			{#/if}  " +
				"		{#/for} " +
				"	</select> " +
				"</td> " +
				"{#/if} " ;
			}

			if (pf.getTitle().equals("Position")) {
				dTemplate = dTemplate +
				"{#if $P.profile[" + pf.getSortOrder() + "][\"bVisible\"] == true }  " +
				"<td> " +
				"	<input type=\"text\" name=\"position\" value=\"{ $P.aData.position }\" size=\"5\"/> " +
				"</td> " +
				"{#/if} " ;
			}

			if (pf.getTitle().equals("Sources")) {
				dTemplate = dTemplate +
				"{#if $P.profile[" + pf.getSortOrder() + "][\"bVisible\"] == true }  " +
				"<td title=\"{ $P.aData.sourcesAsHTMLLinks.formatSourceText() }\"> " +
				"	{#if $P.aData.sourcesAsHTMLLinks != ''}  " +
				"		{ $P.aData.sourcesAsHTMLLinks.formatSourceHTML() } | <a href=\"#\" class=\"wizard\" id=\"{ $P.aData.assetUseId }\">Change</a> " +
				"	{#/if} " +
				"	{#if $P.aData.sourcesAsHTMLLinks == ''}  " +
				"		<a href=\"#\" class=\"wizard\" id=\"{ $P.aData.assetUseId }\">Enter details</a> " +
				"	{#/if} " +
				"</td> " +
				"{#/if} " ;
			}

			if (pf.getTitle().equals("Status")) {
				dTemplate = dTemplate +
				"{#if $P.profile[" + pf.getSortOrder() + "][\"bVisible\"] == true }  " +
				"<td>{ $P.aData.permissionStatus }</td> " +
				"{#/if} " ;
			}

			if (pf.getTitle().equals("Final Page")) {
				dTemplate = dTemplate +
				"{#if $P.profile[" + pf.getSortOrder() + "][\"bVisible\"] == true }  " +
				"<td> " +
				"	<input type=\"text\" name=\"finalPage\" value=\"{ $P.aData.finalPage }\" size=\"5\"/> " +
				"</td> " +
				"{#/if} " ;
			}

			if (pf.getTitle().equals("Source Ref. Number")) {
				dTemplate = dTemplate +
				"{#if $P.profile[" + pf.getSortOrder() + "][\"bVisible\"] == true }  " +
				"<td> " +
				"	<input type=\"text\" name=\"sourceRefNumber\" value=\"{ $P.aData.sourceRefNumber }\" size=\"5\"/> " +
				"</td> " +
				"{#/if} " ;
			}

			if (pf.getTitle().equals("Credit Line")) {
				dTemplate = dTemplate +
				"{#if $P.profile[" + pf.getSortOrder() + "][\"bVisible\"] == true }  " +
				"<td> " +
				"	<input type=\"text\" name=\"creditLine\" value=\"{ $P.aData.creditLine }\" size=\"20\" maxlength=\"500\"/> " +
				"</td> " +
				"{#/if} " ;
			}

			if (pf.getTitle().equals("Pickup")) {
				dTemplate = dTemplate +
				"{#if $P.profile[" + pf.getSortOrder() + "][\"bVisible\"] == true }  " +
				"<td> " +
				"	<input type=\"checkbox\" id=\"pickup\" name=\"pickup\" value=\"1\" {isBChecked ($P.aData.pickup)}/> " +
				"</td> " +
				"{#/if} " ;
			}

			if (pf.getTitle().equals("Royalty Free")) {
				dTemplate = dTemplate +
				"{#if $P.profile[" + pf.getSortOrder() + "][\"bVisible\"] == true }  " +

				// user should not be able to edit RoyaltyFree in landing table
				//"<td> " +
				//"	<input type=\"checkbox\" id=\"royaltyFree\" name=\"royaltyFree\" value=\"1\" {isBChecked ($P.aData.royaltyFree)}/> " +
				//"</td> " +

				// read-only
				"<td>{ $P.aData.royaltyFree }</td> " +
				"{#/if} ";
			}

			if (pf.getTitle().equals("Camera Copy To Come")) {
				dTemplate = dTemplate +
				"{#if $P.profile[" + pf.getSortOrder() + "][\"bVisible\"] == true }  " +
				"<td> " +
				"	<input type=\"checkbox\" id=\"cameraCopyToCome\" name=\"cameraCopyToCome\" value=\"1\" {isBChecked ($P.aData.cameraCopyToCome)}/> " +
				"</td> " +
				"{#/if} " ;
			}

			if (pf.getTitle().equals("Sent To Production")) {
				dTemplate = dTemplate +
				"{#if $P.profile[" + pf.getSortOrder() + "][\"bVisible\"] == true }  " +
				"<td> " +
				"	<input type=\"checkbox\" id=\"sentToProduction\" name=\"sentToProduction\" value=\"1\" {isBChecked ($P.aData.sentToProduction)}/> " +
				"</td> " +
				"{#/if} " ;
			}

			if (pf.getTitle().equals("Comments")) {
				dTemplate = dTemplate +
				"{#if $P.profile[" + pf.getSortOrder() + "][\"bVisible\"] == true }  " +
				"<td> " +
				"	<input type=\"text\" name=\"permissionComment\" value=\"{ $P.aData.permissionComment }\" size=\"20\" maxlength=\"500\"/> " +
				"</td> " +
				"{#/if} " ;
			}

			if (pf.getTitle().equals("Asset Order")) {
				dTemplate = dTemplate +
				"{#if $P.profile[" + pf.getSortOrder() + "][\"bVisible\"] == true }  " +
				"<td> " +
				"	<input type=\"text\" name=\"sortOrder\" value=\"{ $P.aData.sortOrder }\" size=\"10\" maxlength=\"10\"/> " +
				"</td> " +
				"{#/if} " ;
			}

			if (pf.getTitle().equals("GBPM Category")) {
				dTemplate = dTemplate +
				"{#if $P.profile[" + pf.getSortOrder() + "][\"bVisible\"] == true }  " +
				"<td> " +
			"	<select name=\"gbpmCategory\" style=\"width:100px\"> " +
			"		<option value='0'>--</option> " +
			"		{#foreach $P.aData.gbpmCategories as gbpm}  " +
			"			{#if $T.gbpm.code == $P.aData.gbpmCategory } " +
			"				<option selected='selected' value='{$T.gbpm.code}'>{$T.gbpm.description}</option>  " +
			"			{#else} " +
			"				<option value='{$T.gbpm.code}'>{$T.gbpm.description}</option>  " +
			"			{#/if} " +
			"	{#/for} " +
			"	</select> " +
				"</td> " +
				"{#/if} " ;
			}

			// Making Photographer Column editable
			if (pf.getTitle().equals("Photographer")) {

				dTemplate = dTemplate +
				"{#if $P.profile[" + pf.getSortOrder() + "][\"bVisible\"] == true }  " +
				"<td> " +
				"	<input type=\"text\" name=\"artist\" id=\"artist\" value=\"{ $P.aData.artist }\" size=\"20\"/> " +
				"</td> " +

				"{#/if} " ;
			}
			//SR_301213 starts
			if (pf.getTitle().equals("Media Manager")) {
				dTemplate = dTemplate +
				"{#if $P.profile[" + pf.getSortOrder() + "][\"bVisible\"] == true }  " +
				"<td> " +
				"	<input type=\"checkbox\" id=\"mediaManager\" name=\"mediaManager\" value=\"1\" {isBChecked ($P.aData.mediaManager)}/> " +
				"</td> " +
				"{#/if} " ;
			}
			//SR_301213 ends

        		} // end of for loop

		return dTemplate;
	}

	/**
	 * Will read the custom filter if available and apply it, otherwise just use the form filter
	 * @param request
	 * @param cwId
	 * @return boolean
	 */
	public boolean applyFilter (HttpServletRequest request, int cwId) {
		boolean useFilter = false;
		LandingFilterForm form = (LandingFilterForm) request.getSession().getAttribute(LandingFilterController.MODEL_FORM_NAME + cwId);
		String filter = PermUserContext.getCurrentUser(request).getCustomFilter();

		if (null == form && filter != null) {  // was getting NullPointerException below [filter.equals(..)] for certain users with certain cw's in QA
			form = new LandingFilterForm();
			request.getSession().setAttribute(LandingFilterController.MODEL_FORM_NAME + cwId, form);
			form.setUseFilter(true);

			boolean includeCovers = request.isUserInRole(Role.EMPLOYEE_DEFAULT.getCode())
				|| request.isUserInRole(Privilege.COVER_ASSETS.getCode());
			List<Component> componentList = commonWorkRepository.loadComponentList(cwId, includeCovers);
			if (filter.equals(UserDefaults.CUSTOM_FILTER)) {
				form.setComponentIds (new ArrayList<Integer>());
				for (Component component : componentList) {
					if (null != component.getCategory() && component.getCategory().equals(ComponentCategory.COVER)) {
						form.addComponentId(component.getId());
					}
				}
			} else if (filter.equals(UserDefaults.INTERNAL_FILTER)) {
				form.setComponentIds (new ArrayList<Integer>());
				for (Component component : componentList) {
					if (null != component.getCategory() && !component.getCategory().equals(ComponentCategory.COVER)) {
						form.addComponentId(component.getId());
					}
				}
			}
		}

		// if we have a custom filter, and no filter is selected using UI, we prefilter
		if (form != null) {
			form.setCustomFilter(filter);
			useFilter = form.getUseFilter();
		}
		return useFilter;
	}

	public void setAssetUseRepository(AssetUseRepository assetUseRepository) {
		this.assetUseRepository = assetUseRepository;
	}

	public AssetUseRepository getAssetUseRepository() {
		return assetUseRepository;
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

	public void setConditionRepository(ConditionRepository conditionRepository) {
		this.conditionRepository = conditionRepository;
	}

	public ConditionRepository getConditionRepository() {
		return conditionRepository;
	}

	public void setCommonWorkRepository(CommonWorkRepository commonWorkRepository) {
		this.commonWorkRepository = commonWorkRepository;
	}

	public CommonWorkRepository getCommonWorkRepository() {
		return commonWorkRepository;
	}

	public void setCommonWorkService(CommonWorkService commonWorkService) {
		this.commonWorkService = commonWorkService;
	}

	public CommonWorkService getCommonWorkService() {
		return commonWorkService;
	}

	public void setProductIndexService(ProductIndexService productIndexService) {
		this.productIndexService = productIndexService;
	}

	public ProductIndexService getProductIndexService() {
		return productIndexService;
	}

	public ContractService getContractService() {
		return contractService;
	}

	public void setContractService(ContractService contractService) {
		this.contractService = contractService;
	}

	public UserRepository getUserRepository() {
		return userRepository;
	}

	public void setUserRepository(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	// Start : Added for DM-534
	public AssetUseIndexService getAssetUseIndexService() {
		return assetUseIndexService;
	}

	public void setAssetUseIndexService(AssetUseIndexService assetUseIndexService) {
		this.assetUseIndexService = assetUseIndexService;
	}
	// End : Added for DM-534
}
