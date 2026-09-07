package com.wiley.permissions.web.internal.controllers.reports;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;		//	Added for DM-284
import java.sql.SQLException;		//	Added for DM-284
import java.text.DateFormat;
import java.text.ParseException;	//	Added for DM-284
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;

import com.wiley.permissions.common.utils.AssetComplianceReportBean;	//	Added for DM-284
import com.wiley.permissions.common.utils.AssetDetailsReportBean;		//	Added for DM-284
import com.wiley.permissions.common.utils.PermissionSummaryReportBean;	//	Added for DM-284
import com.wiley.permissions.domain.message.pe.ProductSearchResult;
import com.wiley.permissions.domain.persistence.permissions.Address;
import com.wiley.permissions.domain.persistence.permissions.Asset;
import com.wiley.permissions.domain.persistence.permissions.AssetUse;
import com.wiley.permissions.domain.persistence.permissions.BusinessUnit;
import com.wiley.permissions.domain.persistence.permissions.CommonWork;
import com.wiley.permissions.domain.persistence.permissions.CompCopy;
import com.wiley.permissions.domain.persistence.permissions.Component;
import com.wiley.permissions.domain.persistence.permissions.Condition;
import com.wiley.permissions.domain.persistence.permissions.Contract;
import com.wiley.permissions.domain.persistence.permissions.CwFile;
import com.wiley.permissions.domain.persistence.permissions.CwHistory;
import com.wiley.permissions.domain.persistence.permissions.PermissionStatus;
import com.wiley.permissions.domain.persistence.permissions.Product;
import com.wiley.permissions.domain.persistence.permissions.ProductLine;
import com.wiley.permissions.domain.persistence.permissions.RoyaltyFreeDeal;
import com.wiley.permissions.domain.persistence.permissions.Source;
import com.wiley.permissions.domain.persistence.permissions.SourceGroup;
import com.wiley.permissions.domain.persistence.permissions.UploadedDocumentsDetails;
import com.wiley.permissions.domain.persistence.permissions.User;
import com.wiley.permissions.domain.persistence.permissions.UserGroup;
import com.wiley.permissions.domain.persistence.permissions.UserToRole;
import com.wiley.permissions.persistence.PersistenceException;
import com.wiley.permissions.repositories.AssetRepository;
import com.wiley.permissions.repositories.AssetUseRepository;
import com.wiley.permissions.repositories.CommonWorkRepository;
import com.wiley.permissions.repositories.ContractRepository;
import com.wiley.permissions.repositories.ProductRepository;
import com.wiley.permissions.repositories.SourceRepository;
import com.wiley.permissions.repositories.UserRepository;
import com.wiley.permissions.security.web.UserPrincipal;
import com.wiley.permissions.services.AssetSearchForm;
import com.wiley.permissions.services.AssetUseIndexService;
import com.wiley.permissions.services.AssetUseSearchResult;
import com.wiley.permissions.services.AssetUseSearchResults;
//Start : Added for DM-534
import com.wiley.permissions.services.CommonWorkService;
import com.wiley.permissions.services.ProductIndexService;
import com.wiley.permissions.services.ProductService;
import com.wiley.permissions.web.shared.PermUserContext;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;
import com.wiley.permissions.web.shared.controllers.asset.AssetUseIndexSearchController;
import com.wiley.permissions.web.shared.controllers.reports.ExcelView;
import com.wiley.permissions.web.shared.controllers.reports.ExcelViewNew;
//End : Added for DM-534
import com.wiley.permissions.web.shared.controllers.reports.OneOffCellValue;
import com.wiley.permissions.web.shared.controllers.reports.PdfReportView;
import com.wiley.permissions.web.shared.util.LabelValueBean;
import com.wiley.sf.common.lang.StringUtil;
import com.wiley.sf.common.monitor.PerformanceMonitor.PerfTimer;

@Controller
/*@RequestMapping("/commonwork/report/")*/
@RequestMapping
public class ReportsController extends BaseAnnotatedController {

	private static final Log log = LogFactory.getLog(ReportsController.class);

	private static final String CW_ID = "commonWorkId";

	private CommonWorkRepository cwRepository;
	private ProductRepository productRepository;
	private AssetUseRepository assetUseRepository;
	private AssetUseIndexService assetUseIndexService;
	private ProductService productService;
	private UserRepository userRepository;
	private SourceRepository sourceRepository;
	private ContractRepository contractRepository;

	private ProductIndexService productIndexService;
	private CommonWorkService commonWorkService;//Added for DM-534

	private View excelView;
	private View summaryExcelView;
	private View sourceSummaryExcelView;
	private View commonWorkDetailExcelView;
	private String permissionSummaryView;
	private View photocopyrightExcelView;
	private String viewPhotoEditorStatusExcelView ;
	private PdfReportView pdfView;
	private String userLandingViewName;
	private String excludedAssetsView;
	private String viewDivestmentReportExcelView;
	//Added for 'Report For Legal' - Start
	private View reportLegalExcelView;
	//Added for 'Report For Legal' - End
	//Added for DM-534 - Start
	private View assetExportExcelView;
	//Added for -534 - End
	private AssetRepository assetRepository;	// Added for DM-1606
	private View newPermissionSummaryExcelView;	// Added for DM-284

	//Code Change for SS Task 3 - Added new parameter userGroupId
	@RequestMapping(value="/commonwork/report/assetCredits.xls", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView downloadAssetCredits(HttpServletRequest request, HttpServletResponse response,
			@RequestParam(value = "userGroupId", required = false) Integer userGroupId,
			@RequestParam(CW_ID) Integer cwId)
			throws Exception {
		log.debug("downloadAssetCredits(): entered..."+userGroupId);
		PerfTimer timer = cwRepository.getMonitor().startTimer("ReportsController::assetCredits");

		CommonWork cw = PermUserContext.getCurrentCommonWork(request);

		if (null == cw) {
			String msg = getMessageSource().getMessage("no.product.for.product.landing", null, null);
			ModelAndView mv = new ModelAndView(getUserLandingViewName() + "?generalMessage=" + msg);
			return mv;
		}


		// create a dual dimension List for rows/columns
		List<Object> rowList = new ArrayList<Object>();

		// set header
		List<Object> row = new ArrayList<Object>();

		// Added New Headers -Start
				row = new ArrayList<Object>();
				row.add("Credit Line Report");
				rowList.add(row);

				Product product = getProductRepository().lazyLoad(Product.class, cw.getPrimaryProduct().getId(), new String[] { "authors" });


				row = new ArrayList<Object>();
				row.add("..Subhead");
				row.add("Author(s): " + product.getAuthorsAsString());
				rowList.add(row);

				row = new ArrayList<Object>();
				row.add("..Subhead");
				row.add("Title: " + product.getTitle());
				rowList.add(row);

				row = new ArrayList<Object>();
				row.add("..Subhead");
				row.add("Edition: " + product.getEditionNumber());
				rowList.add(row);

				row = new ArrayList<Object>();
				row.add("..Subhead");
				row.add("ISBN: " + product.getIsbn13());
				rowList.add(row);

				row = new ArrayList<Object>();
				row.add("");
				rowList.add(row);

				// headings
				row = new ArrayList<Object>();
				row.add("..Subhead");
		// Added New Headers -End

		row.add("Component");
		row.add("Usage");
		row.add("Figure Number");
		row.add("Description");
		// Credit Line Report change for Australian Users - Start
		row.add("Source");
		// Credit Line Report change for Australian Users - End
		row.add("Credit Line");
		// Credit Line Report change for Australian Users - Start
		row.add("Manuscript Page Number");
		// Credit Line Report change for Australian Users - End
		row.add("Page Number");
		row.add("Photo Position");
		row.add("Caption");

		rowList.add(row);

		// authors can't see this report so just pass true for includeCovers
		// pass false for includeCanceled
		//Code Change for SS Task 3 - Start
		//AssetUseSearchResults results = assetUseIndexService.getCWAssetUsesFromIndex(cw.getId(), true, false);
		AssetUseSearchResults results = assetUseIndexService.getCWAssetUsesFromIndexWithUserGroupId(cw.getId(),userGroupId, true, true);
		//Code Change for SS Task 3 - End
		ArrayList<AssetUseSearchResult> docs = results.getDocuments();

		for (int x = 0; x < docs.size(); x++) {
			AssetUseSearchResult result = docs.get(x);
			log.debug("IAM IN "+result.getComponentName());
			log.debug(result.getDescription());
			row = new ArrayList<Object>();
			row.add(result.getComponentName());
			row.add(result.getUsage());
			row.add(result.getPosition());
			row.add(result.getDescription());
			// Credit Line Report change for Australian Users - Start
			row.add(result.getSourceNames());
			// Credit Line Report change for Australian Users - End
			row.add(result.getCreditLine());
			row.add(result.getManuscriptPage());
			// Credit Line Report change for Australian Users - Start
			row.add(result.getFinalPage());
			// Credit Line Report change for Australian Users - End
			row.add(result.getPagePositionDescription());
			row.add(result.getCaption());

			rowList.add(row);
		}

		HashMap<String, Object> model = new HashMap<String, Object>();
		model.put("data", rowList);
		model.put(ExcelView.FIRST_ROW_IS_HEADER, Boolean.TRUE);
		model.put(ExcelView.AUTO_SIZE_COLS, 8);
		model.put(ExcelView.AUTO_SIZE_MAX_WIDTH, 100);

		String authorLname = "";
		if(null != product.getAuthor()){
			authorLname = product.getAuthor().getLastName();
		}

		String fileName = "CreditLineReport."+authorLname+"."+product.getIsbn13()+".xls";
		model.put(ExcelView.FILE_NAME, fileName);

		ModelAndView mv = new ModelAndView(getExcelView(), model);
		timer.stopTimer();

		return mv;
	}

	//Code Change for SS Task 3 - Added new parameter userGroupId
	@RequestMapping(value="/commonwork/report/sourceSummary.xls", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView sourceSummary(@RequestParam(value = "userGroupId", required = false) Integer userGroupId,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		log.debug("sourceSummary(): entered...");
		PerfTimer timer = cwRepository.getMonitor().startTimer("ReportsController::sourceSummary");
		CommonWork cw = PermUserContext.getCurrentCommonWork(request);

		if (null == cw) {
			String msg = getMessageSource().getMessage("no.product.for.product.landing", null, null);
			ModelAndView mv = new ModelAndView(getUserLandingViewName() + "?generalMessage=" + msg);
			return mv;
		}

		//Product product = cwRepository.loadWithPrimaryProductById(cw.getId()).getPrimaryProduct();
		Product product = cwRepository.loadWithExtendedPrimaryProductById(cw.getId()).getPrimaryProduct();

		// create a dual dimension List for rows/columns
		List<Object> rowList = new ArrayList<Object>();

		// set header
		List<Object> row = new ArrayList<Object>();
		//Code Change for SS Task 3 - Start
		//List<Object[]> data = cwRepository.loadSourceSummary(cw.getId());
		List<Object[]> data = cwRepository.loadSourceSummary(cw.getId(), userGroupId);
		//Code Change for SS Task 3 - End

		for (Object[] columns : data) {
			row = new ArrayList<Object>();
			row.add(columns[0]);
			String info = columns[1] + " at a cost of $" + cwRepository.loadCostByCwSourceName(cw.getId(), Integer.valueOf(columns[2].toString()));
			row.add(info);
			rowList.add(row);
		}

		HashMap<String, Object> model = new HashMap<String, Object>();

		//ArrayList<OneOffCellValue> oneOffList = new ArrayList<OneOffCellValue>();
		//oneOffList.add(new OneOffCellValue(1, 2, product.getTitle()));
		//oneOffList.add(new OneOffCellValue(1, 3, product.getIsbn13()));

		ArrayList<OneOffCellValue> oneOffList = new ArrayList<OneOffCellValue>();
		// Although we should always have an author for a product, it's possible not to.
		// (avoid NullPointer exception)
		if (StringUtils.isNotBlank(product.getAuthorsAsString())) {
			oneOffList.add(new OneOffCellValue(2, 1, product.getAuthorsAsString())); // C2
		}

		oneOffList.add(new OneOffCellValue(2, 2, product.getTitle())); // C3
		oneOffList.add(new OneOffCellValue(4, 2, product.getIsbn13())); // E3
		oneOffList.add(new OneOffCellValue(2, 3, (null != product.getAcquisitionsEditor() ? product.getAcquisitionsEditor().getFullName() : ""))); // C4

		model.put("data", rowList);
		model.put(ExcelView.START_ROW, new Integer(6));
		model.put(ExcelView.ONE_OFF_CELL_VALUES, oneOffList);

		model.put(ExcelView.AUTO_SIZE_COLS, 4);
		model.put(ExcelView.AUTO_SIZE_MAX_WIDTH, 100);

		String authorLname = "";
		if(null != product.getAuthor()){
			authorLname = product.getAuthor().getLastName();
		}

		String fileName = "SourceSummaryReport."+authorLname+"."+product.getIsbn13()+".xls";
		model.put(ExcelView.FILE_NAME, fileName);

		ModelAndView mv = new ModelAndView(getSourceSummaryExcelView(), model);
		timer.stopTimer();

		return mv;
	}

	//Code Change for SS Task 3 - Added new parameter userGroupId
	@RequestMapping(value="/commonwork/report/commonWorkDetail.xls",method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView cwDetail(@RequestParam(value = "userGroupId", required = false) Integer userGroupId,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		log.debug("cwDetail(): entered...");
		PerfTimer timer = cwRepository.getMonitor().startTimer("ReportsController::commonWorkDetail");
		CommonWork cw = PermUserContext.getCurrentCommonWork(request);

		List<Object> dottedLine = new ArrayList<Object>();
		dottedLine.add("-----------------------------------------------------------------------------------");
		dottedLine.add("-------------------------------------------------------------------------------------------------------------");

		List<Object> blankLine = new ArrayList<Object>();
		blankLine.add(" ");

		if (null == cw) {
			String msg = getMessageSource().getMessage("no.product.for.product.landing", null, null);
			ModelAndView mv = new ModelAndView(getUserLandingViewName() + "?generalMessage=" + msg);
			return mv;
		}

		List<Object> row = new ArrayList<Object>();
		// create a dual dimension List for rows/columns
		List<Object> rowList = new ArrayList<Object>();

		row = new ArrayList<Object>();
		row.add("Common Work Detail Report");
		rowList.add(row);

		// Added New Headers -Start
		Product product = getProductRepository().lazyLoad(Product.class, cw.getPrimaryProduct().getId(), new String[] { "authors" });

		row = new ArrayList<Object>();
		row.add("..Subhead");
		row.add("Author(s): " + product.getAuthorsAsString());
		rowList.add(row);

		row = new ArrayList<Object>();
		row.add("..Subhead");
		row.add("Title: " + product.getTitle());
		rowList.add(row);

		row = new ArrayList<Object>();
		row.add("..Subhead");
		row.add("Edition: " + product.getEditionNumber());
		rowList.add(row);

		row = new ArrayList<Object>();
		row.add("..Subhead");
		row.add("ISBN: " + product.getIsbn13());
		rowList.add(row);

		row = new ArrayList<Object>();
		row.add("");
		rowList.add(row);
		// Added New Headers -End

		rowList.add(dottedLine);

		row = new ArrayList<Object>();
		row.add("Common Work:");
		row.add(cw.getName());
		rowList.add(row);
		row = new ArrayList<Object>();
		row.add("Common Work ID:");
		row.add(cw.getCode());
		rowList.add(row);
		rowList.add(dottedLine);

		Product primary = null;
		List<Product> products = productService.loadCWDetailsReportView(cw.getId());
		if (products.size() > 1) {
			row = new ArrayList<Object>();
			row.add("..Subhead");
			row.add("PRODUCTS IN COMMON WORK");
			rowList.add(row);

			for (int x = 0; x < products.size(); x++) {
				Product prod = products.get(x);
				String prodData = "";
				if (null != prod.getMedium()) {
					prodData = prod.getMedium().getName() + " - " + prod.getIsbn13() + " - " + prod.getTitle();
				}
				else {
					prodData = "UNKNOWN - " + prod.getIsbn13() + " - " + prod.getTitle();
				}
				if (prod.isCwPrimary()) {
					primary = prod;
					prodData = prodData + "(Primary)";
				}
				row = new ArrayList<Object>();
				row.add(" ");
				row.add(prodData);
				rowList.add(row);
			}

			rowList.add(dottedLine);

		}
		else {
			primary = products.get(0);
		}

		if (null != primary) {
			row = new ArrayList<Object>();
			row.add("..Subhead");
			row.add("PRIMARY PRODUCT INFORMATION");
			rowList.add(row);

			row = new ArrayList<Object>();
			row.add("Medium");
			if (null != primary.getMedium()) {
				row.add(primary.getMedium().getName());
			}
			rowList.add(row);

			row = new ArrayList<Object>();
			row.add("Title");
			row.add(primary.getTitle());
			rowList.add(row);

			row = new ArrayList<Object>();
			row.add("ISBN");
			row.add(primary.getIsbn13());
			rowList.add(row);

			row = new ArrayList<Object>();
			row.add("Author(s)");
			row.add(primary.getAuthorsAsString());
			rowList.add(row);

			row = new ArrayList<Object>();
			row.add("Publication Status");
			if (null != primary.getPublicationStatus()) {
				row.add(primary.getPublicationStatus().getDescription());
			}
			rowList.add(row);

			row = new ArrayList<Object>();
			row.add("BBD");
			row.add("..Date");
			row.add(primary.getPrintDate());
			rowList.add(row);

			row = new ArrayList<Object>();
			row.add("Edition");
			if (null != primary.getEdition() && null != primary.getEdition().getEditionNumber()) {
				row.add(primary.getEdition().getEditionNumber());
			}
			rowList.add(row);

			row = new ArrayList<Object>();
			row.add("Sub Medium");
			row.add(primary.getSubMedium());
			rowList.add(row);

			row = new ArrayList<Object>();
			row.add("Product Line");
			if (null != primary.getProductLine()) {
				row.add(primary.getProductLine().getName());
			}
			rowList.add(row);

			row = new ArrayList<Object>();
			row.add("Product Family");
			if (null != primary.getProductFamily()) {
				row.add(primary.getProductFamily().getCode() + "-" + primary.getProductFamily().getName());
			}
			rowList.add(row);

			row = new ArrayList<Object>();
			row.add("Product Type");
			if (null != primary.getProductType()) {
				row.add(primary.getProductType().getName());
			}
			rowList.add(row);

			row = new ArrayList<Object>();
			row.add("Coppyright Year");
			row.add(primary.getCopyrightYear());
			rowList.add(row);

			row = new ArrayList<Object>();
			row.add("Permission Payer");
			row.add(primary.getPermissionPayer());
			rowList.add(row);
		}

		// processing asset use
		rowList.add(blankLine);
		rowList.add(dottedLine);
		row = new ArrayList<Object>();
		row.add("..Subhead");
		row.add("ASSET USAGE INFORMATION");
		rowList.add(row);

		row = new ArrayList<Object>();
		row.add("..Subhead");
		row.add("Description");
		row.add("Source(s)");
		row.add("Position");
		row.add("Media Type");
		row.add("Component");
		row.add("Usage");
		row.add("Msp Page");
		row.add("Owner Type");
		row.add("Status");
		row.add("Pickup");
		row.add("Needs payment Request");
		row.add("Excluded");
		row.add("Canceled");
		row.add("Reuse");
		rowList.add(row);

		// List<AssetUse> aus = cw.getAssetUses();

		// authors cannot see this report so just pass true for includeCovers
		// pass true for includeCanceled (we have a Canceled column)
		//Code Change for SS Task 3 - Start
		// AssetUseSearchResults results = assetUseIndexService.getCWAssetUsesFromIndex(cw.getId(), true, true);
		AssetUseSearchResults results = assetUseIndexService.getCWAssetUsesFromIndexWithUserGroupId(cw.getId(),userGroupId, true, true);
		//Code Change for SS Task 3 - End

		ArrayList<AssetUseSearchResult> docs = results.getDocuments();

		for (int x = 0; x < docs.size(); x++) {
			AssetUseSearchResult result = docs.get(x);
			row = new ArrayList<Object>();
			row.add(result.getDescription());
			row.add("..WrappedText");
			row.add(result.getSourceNames());
			row.add(result.getPosition());
			row.add(result.getMediaType());
			row.add(result.getComponentName());
			row.add(result.getUsage());
			row.add(result.getManuscriptPage());
			row.add(result.getOwnerType());
			//	row.add(result.getPermissionStatusDescription());

			// Start : Added for DM-1606
			String permStatusCode = result.getPermissionStatusCode();
			if(permStatusCode.equalsIgnoreCase(PermissionStatus.FORM_SENT.getCode()))
			{
				boolean createdPO = assetRepository.checkForPoCreation(result.getAssetId());
				log.debug("AssetUseTableRowView() PO created "+createdPO+" for asset ID "+result.getAssetId());
				// if any PO created for an asset in Purchase_order table then display waiting on invoice else display form sent
				if(createdPO)
						row.add(PermissionStatus.FORM_WAITING_ON_INVOICE.getDescription());
					else
						row.add(result.getPermissionStatusDescription());
			}
			else
			{
				row.add(result.getPermissionStatusDescription());
			}
			// End : Added for DM-1606

			row.add(result.isPickup() ? "Yes" : "No");
			row.add(result.isNeedPaymentRequest() ? "Yes" : "No");
			row.add(result.exclude() ? "No" : "Yes");
			row.add(result.isCanceled() ? "Yes" : "No");
			row.add(result.isReuse() ? "Yes" : "No");
			rowList.add(row);
		}

		// set header for image counts

		rowList.add(blankLine);
		rowList.add(dottedLine);
		row = new ArrayList<Object>();
		row.add("..Subhead");
		row.add("IMAGE COUNT BY SOURCE");
		rowList.add(row);

		row = new ArrayList<Object>();
		row.add("..Subhead");
		row.add(0, "source");
		row.add(1, "Photos");
		rowList.add(row);

		List<Object[]> data = cwRepository.loadCwDetail(cw.getId(), userGroupId);
		for (Object[] columns : data) {
			row = new ArrayList<Object>();
			row.add(columns[0]);
			String info = columns[1] + " at a cost of $" + cwRepository.loadCostByCwSourceName(cw.getId(), Integer.valueOf(columns[2].toString()));
			row.add(info);
			rowList.add(row);
		}

		HashMap<String, Object> model = new HashMap<String, Object>();
		model.put("data", rowList);
		model.put(ExcelView.FIRST_ROW_IS_HEADER, Boolean.TRUE);
		model.put(ExcelView.AUTO_SIZE_COLS, 12);
		model.put(ExcelView.AUTO_SIZE_MAX_WIDTH, 100);

		String authorLname = "";
		if(null != product.getAuthor()){
			authorLname = product.getAuthor().getLastName();
		}

		String fileName = "ProductDetailReport."+authorLname+"."+product.getIsbn13()+".xls";
		model.put(ExcelView.FILE_NAME, fileName);

		ModelAndView mv = new ModelAndView(getExcelView(), model);
		timer.stopTimer();

		return mv;
	}

	//Code Change for SS Task 3 - Added new parameter userGroupId
	@RequestMapping(value="/commonwork/report/assetDetail.xls",method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView assetDetail(@RequestParam(value = "userGroupId", required = false) Integer userGroupId,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		log.debug("assetDetail(): entered...");
		PerfTimer timer = cwRepository.getMonitor().startTimer("ReportsController::assetDetail");

		CommonWork cw = PermUserContext.getCurrentCommonWork(request);

		List<Object> blankLine = new ArrayList<Object>();
		blankLine.add(" ");

		if (null == cw) {
			String msg = getMessageSource().getMessage("no.product.for.product.landing", null, null);
			ModelAndView mv = new ModelAndView(getUserLandingViewName() + "?generalMessage=" + msg);
			return mv;
		}

		List<Object> row = new ArrayList<Object>();
		// create a dual dimension List for rows/columns
		List<Object> rowList = new ArrayList<Object>();

		row = new ArrayList<Object>();
		row.add("..Subhead");
		row.add("Asset Detail Report");
		rowList.add(row);

		//row = new ArrayList<Object>();
		//row.add("..Subhead");
		//row.add("Title: " + cw.getPrimaryProduct().getTitle());
		//rowList.add(row);

		//row = new ArrayList<Object>();
		//row.add("..Subhead");
		//row.add("ISBN: " + cw.getPrimaryProduct().getIsbn13());
		//rowList.add(row);

		// Added New Headers -Start

		Product product = getProductRepository().lazyLoad(Product.class, cw.getPrimaryProduct().getId(), new String[] { "authors" });

		List<CwFile> loadFiles = cwRepository.loadFiles(cw.getId());// Added for implementing Build RN 160121-001880

		row = new ArrayList<Object>();
		row.add("..Subhead");
		row.add("Author(s): " + product.getAuthorsAsString());
		insertTitleFileNamesList(loadFiles, 1, row, "ADR");// Added for implementing Build RN 160121-001880
		rowList.add(row);

		row = new ArrayList<Object>();
		row.add("..Subhead");
		row.add("Title: " + product.getTitle());
		insertTitleFileNamesList(loadFiles, 2, row, "ADR");// Added for implementing Build RN 160121-001880
		rowList.add(row);

		row = new ArrayList<Object>();
		row.add("..Subhead");
		row.add("Edition: " + product.getEditionNumber());
		insertTitleFileNamesList(loadFiles, 3, row, "ADR");// Added for implementing Build RN 160121-001880
		rowList.add(row);

		row = new ArrayList<Object>();
		row.add("..Subhead");
		row.add("ISBN: " + product.getIsbn13());
		rowList.add(row);
		// Added New Headers -End


		// processing asset use
		rowList.add(blankLine);
		row = new ArrayList<Object>();
		row.add("..Subhead");
		row.add("ASSET INFORMATION");
		rowList.add(row);

		row = new ArrayList<Object>();
		row.add("..Subhead");
		row.add("Asset Order");
		row.add("Component");
		row.add("Usage");
		row.add("Position");
		row.add("MS Page");
		row.add("Media Type");
		row.add("Description");
		row.add("Credit Line");
		row.add("Sources");
		row.add("Source Ref. Number");
		row.add("Final Page");
		row.add("Status");
		row.add("Invoice Number");
		row.add("Invoice or Permission Date");
		row.add("Reuse");
		row.add("Estimated ");
		row.add("Final Cost");
		row.add("Multi-Use");	// Added for DM-284
		row.add("Royalty Free ( * indicates Asset-Level permission)");
		row.add("Pickup");
		row.add("Camera Copy To Come");
		row.add("Sent To Production");
		row.add("Comments                                              ");
		rowList.add(row);

		// use index to get AssetUses instead
		//List<AssetUse> aus = cw.getAssetUses();

		// authors cannot see this report so just pass true for includeCovers
		// pass false for includeCanceled
		//Code Change for SS Task 3 - Start
		// AssetUseSearchResults results = assetUseIndexService.getCWAssetUsesFromIndex(cw.getId(), true, false);
		AssetUseSearchResults results = assetUseIndexService.getCWAssetUsesFromIndexWithUserGroupId(cw.getId(),userGroupId, true, false);
		//Code Change for SS Task 3 - End
		ArrayList<AssetUseSearchResult> docs = results.getDocuments();
		HashSet<Integer> assetIdSet = new HashSet<Integer>();
		String royaltyFree = null;//Added to implement DM-122
		// Start : Added for DM-284
		ArrayList<AssetDetailsReportBean> reportBeanList = new ArrayList<AssetDetailsReportBean>();

		for (int x = 0; x < docs.size(); x++) {
			AssetUseSearchResult result = docs.get(x);

			// Start : Added for DM-1606
			String permStatusCode = result.getPermissionStatusCode();
			if(permStatusCode.equalsIgnoreCase(PermissionStatus.FORM_SENT.getCode()))
			{
				boolean createdPO = assetRepository.checkForPoCreation(result.getAssetId());
				log.debug("AssetUseTableRowView() PO created "+createdPO+" for asset ID "+result.getAssetId());
				if(createdPO)
					permStatusCode = PermissionStatus.FORM_WAITING_ON_INVOICE.getCode();
			}
			// End : Added for DM-1606

			List<Contract> clist = getContractRepository().loadListForAssetCW(result.getAssetId(), cw.getId());
			String contractNumber = "";
			String contractDate = "";
			String cRoyalyFree = "";
			//BigDecimal finalCost = new BigDecimal(0);
			double finalCost = 0.00;
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
				if(clist.size()>0 && clist != null)
				{
					for(Contract contractInfo : clist)
					{
						if(contractNumber!=null && contractNumber.length()>0)
							contractNumber = contractNumber + " , "+contractInfo.getNumber();
						else
							contractNumber = contractInfo.getNumber();

						if(contractDate.length()>0)
							contractDate = contractDate +" , "+ dateFormat.format(contractInfo.getDate());
						else
							contractDate = dateFormat.format(contractInfo.getDate());

						if (!assetIdSet.contains(result.getAssetId())) {
							assetIdSet.add(result.getAssetId());
							BigDecimal finalCostDecimal = getRepository().executeSingleResultNamedQuery("Asset.finalCost", new Object[] { result.getAssetId(), cw.getId() });
							finalCost = finalCostDecimal.doubleValue();
						}

						royaltyFree = (result.isRoyaltyFree() ? "Yes" : "No");
						boolean isAssetLevelRF = contractRepository.isAssetLevelRoyaltyFree(contractInfo.getId(), result.getAssetId());
						if(isAssetLevelRF) {
							royaltyFree += '*';
						}
						if(cRoyalyFree.length()>0)
							cRoyalyFree = cRoyalyFree + " , "+royaltyFree;
						else
							cRoyalyFree = royaltyFree;
					}
				}
				String reuse = result.isReuse() ? "Yes" : "No";
			String mutiUsageAsset = "No";
			boolean isMultiUsage = assetUseRepository.isMultiUsageAsset(result.getAssetId(),cw.getId());
			if(isMultiUsage)
			{
				log.debug("asset is multi usage : "+result.getAssetId());
				mutiUsageAsset = "Yes";
			}
			String pickup = result.isPickup() ? "Yes" : "No";
			String cameraCopyToCome = result.isCameraCopyToCome() ? "Yes" : "No";
			String sentToProduction = result.isSentToProduction()? "Yes" : "No";
			String permissionComment = result.getPermissionComment();


			AssetDetailsReportBean bean = new AssetDetailsReportBean();
			bean.setSortOrder(result.getSortOrder());
			bean.setComponentName(result.getComponentName());
			bean.setAssetUsage(result.getUsage());
			bean.setAssetPosition(result.getPosition());
			bean.setManuScriptPage(result.getManuscriptPage());
			bean.setMediaType(result.getMediaType());
			bean.setDecscription(result.getDescription());
			bean.setCreditLine(result.getCreditLine());
			bean.setSourceNames(result.getSourceNames());
			bean.setSourceRef(result.getSourceRef());
			bean.setFinalPage(result.getFinalPage());
			bean.setPermStatusCode(permStatusCode);
			bean.setContractNumber(contractNumber);
			bean.setContractDate(contractDate);
			bean.setReuse(reuse);
			bean.setEstimatedCost(result.getEstimatedCost());
			bean.setFinalCost(finalCost);
			bean.setMutiUsageAsset(mutiUsageAsset);
			bean.setcRoyalyFree(cRoyalyFree);
			bean.setPickup(pickup);
			bean.setCameraCopyToCome(cameraCopyToCome);
			bean.setSentToProduction(sentToProduction);
			bean.setPermissionComment(permissionComment);
			reportBeanList.add(bean);
			//rowList.add(row);
		}

		Collections.sort(reportBeanList,AssetDetailsReportBean.finalCostComparator);
		log.debug("Bean List "+reportBeanList+"\n\n");
		Collections.reverse(reportBeanList);
		log.debug("Bean List in reverse "+reportBeanList+"\n\n");
		log.debug("row List before sorting "+rowList+"\n\n");
		HashMap<String, Object> model = new HashMap<String, Object>();

		for(AssetDetailsReportBean beanRow : reportBeanList)
		{
			row = new ArrayList<Object>();
			row.add(beanRow.getSortOrder());
			row.add(beanRow.getComponentName());
			row.add(beanRow.getAssetUsage());
			row.add(beanRow.getAssetPosition());
			row.add(beanRow.getManuScriptPage());
			row.add(beanRow.getMediaType());
			row.add(beanRow.getDecscription());
			row.add(beanRow.getCreditLine());
			row.add(beanRow.getSourceNames());
			row.add(beanRow.getSourceRef());
			row.add(beanRow.getFinalPage());
			row.add(beanRow.getPermStatusCode());
			row.add(beanRow.getContractNumber());
			row.add(beanRow.getContractDate());
			row.add(beanRow.getReuse());
			row.add(beanRow.getEstimatedCost());
			row.add(beanRow.getFinalCost());
			row.add(beanRow.getMutiUsageAsset());
			row.add(beanRow.getcRoyalyFree());
			row.add(beanRow.getPickup());
			row.add(beanRow.getCameraCopyToCome());
			row.add(beanRow.getSentToProduction());
			row.add(beanRow.getPermissionComment());
			rowList.add(row);
		}
		log.debug("row List after sorting  "+rowList+"\n\n");
		model.put("data", rowList);
		model.put(ExcelView.FIRST_ROW_IS_HEADER, Boolean.TRUE);
		model.put(ExcelView.AUTO_SIZE_COLS, 22);
		model.put(ExcelView.AUTO_SIZE_MAX_WIDTH, 100);
		model.put(ExcelView.AUTO_SIZE_FIRST_ROW, new Integer(5));  // 0-based index
		// End : Added for DM-284
		String authorLname = "";
		if(null != product.getAuthor()){
			authorLname = product.getAuthor().getLastName();
		}

		String fileName = "AssetDetailReport."+authorLname+"."+product.getIsbn13()+".xls";
		model.put(ExcelView.FILE_NAME, fileName);

		ModelAndView mv = new ModelAndView(getExcelView(), model);
		timer.stopTimer();

		return mv;
	}

	//Code Change for SS Task 3 - Added new parameter userGroupId
	@RequestMapping(value="/commonwork/report/sublicense.xls",method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView sublicense(@RequestParam(value = "userGroupId", required = false) Integer userGroupId,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		log.debug("sublicense(): entered...");
		PerfTimer timer = cwRepository.getMonitor().startTimer("ReportsController::sublicense");
		CommonWork cw = PermUserContext.getCurrentCommonWork(request);

		if (null == cw) {
			String msg = getMessageSource().getMessage("no.product.for.product.landing", null, null);
			ModelAndView mv = new ModelAndView(getUserLandingViewName() + "?generalMessage=" + msg);
			return mv;
		}

		Product product = getProductRepository().lazyLoad(Product.class, cw.getPrimaryProduct().getId(), new String[] { "authors" });

		List<CwFile> loadFiles = cwRepository.loadFiles(cw.getId());// Added for implementing Build RN 160121-001880

		// create a dual dimension List for rows/columns
		List<Object> rowList = new ArrayList<Object>();
		List<Object> row;

		row = new ArrayList<Object>();
		row.add("..Subhead");
		row.add("Sublicense Report");
		rowList.add(row);

		row = new ArrayList<Object>();
		row.add("..Subhead");
		row.add("Author(s): " + product.getAuthorsAsString());
		insertTitleFileNamesList(loadFiles, 1, row, "SLR");// Added for implementing Build RN 160121-001880
		rowList.add(row);

		row = new ArrayList<Object>();
		row.add("..Subhead");
		row.add("Title: " + product.getTitle());
		insertTitleFileNamesList(loadFiles, 2, row, "SLR");// Added for implementing Build RN 160121-001880
		rowList.add(row);

		row = new ArrayList<Object>();
		row.add("..Subhead");
		row.add("Edition: " + product.getEditionNumber());
		insertTitleFileNamesList(loadFiles, 3, row, "SLR");// Added for implementing Build RN 160121-001880
		rowList.add(row);

		row = new ArrayList<Object>();
		row.add("..Subhead");
		row.add("ISBN: " + product.getIsbn13());
		rowList.add(row);

		// add blank row
		row = new ArrayList<Object>();
		rowList.add(row);

		row = new ArrayList<Object>();
		row.add("..Subhead");
		row.add("Component");
		row.add("Media Type");
		row.add("Description");
		row.add("Position");
		row.add("Source");
		row.add("Source Reference #");
		row.add("Source Contact Info");
		row.add("Right to Sublicense ( * indicates Asset-Level permission)");
		rowList.add(row);

		// authors cannot see this report so just pass true for includeCovers
		// pass false for includeCanceled
		//Code Change for SS Task 3 - Start
		//AssetUseSearchResults results = assetUseIndexService.getCWAssetUsesFromIndex(cw.getId(), true, false);
		AssetUseSearchResults results = assetUseIndexService.getCWAssetUsesFromIndexWithUserGroupId(cw.getId(),userGroupId, true, false);
		//Code Change for SS Task 3 - End
		ArrayList<AssetUseSearchResult> docs = results.getDocuments();
		String sublicence = null;//Added to implement DM-122

		for (int x = 0; x < docs.size(); x++) {
			AssetUseSearchResult result = docs.get(x);

			List<Integer> sourceIds = result.getSourceIds();
			if (sourceIds.size() == 0)  sourceIds.add(null);
			List<Integer> latestContractIds = result.getLatestContractIds();

			for (Integer sourceId : sourceIds) {
				row = new ArrayList<Object>();
				row.add(result.getComponentName());
				row.add(result.getMediaType());
				row.add(result.getDescription());
				row.add(result.getPosition());

				if (sourceId != null) {
					Source source = getProductRepository().lazyLoad(Source.class, sourceId, new String[] { "addresses" });
					row.add(source.getName());
					// currently sourceRef is on Asset which may be inappropriate (Asset_2_source seems appropriate)
					// so at least for now this will be repeated for all sources the asset may have
					row.add(result.getSourceRef());
					StringBuilder sb = new StringBuilder();
					if (StringUtils.isNotBlank(source.getPhoneNumber())) {
						sb.append(source.getPhoneNumber());
					}
					if (StringUtils.isNotBlank(source.getWebsite())) {
						if (sb.length() > 0) sb.append(" ");
						sb.append(source.getWebsite());
					}
					Address mainAddress = source.getMainAddress();
					if (mainAddress != null) {
						if (sb.length() > 0) sb.append(" ");
						sb.append(mainAddress.toStringOneLine());
					}
					row.add(sb.toString());

					if (latestContractIds.size() > 0) {
						// most of the time there is only going to be a single source and therefore
						// a single latestContractId - so let's optimize for this situation
						// and not worry about the fact that the following code checks more than
						// than one contract in rare cases - but note the following code is careful
						// to check that it's using the correct contract if there is more than one source
						for (int contractId : latestContractIds) {
							try {
								Integer contractSourceId = contractRepository.getSourceIdForContractId(contractId);
								// make sure to call equals on contractSourceId, not sourceId because contractSourceId should
								// always be non-null and sourceId may be null (see above)
								if (contractSourceId.equals(sourceId)) {
									boolean yes = contractRepository.doesContractGiveSublicenseRight(contractId);
									//Start: Added to implement DM-122
									sublicence = (yes ? "Yes" : "No");
									boolean isAssetLevelRF = contractRepository.isAssetLevelRoyaltyFree(contractId, result.getAssetId());
									if(isAssetLevelRF) {
										sublicence += '*';
									}
									row.add(sublicence);
									//End: Added to implement DM-122
								}
							} catch (Exception ex) {
								log.error("Error occured due to : ",ex);
							}
						}
					}
					else {
						row.add("No Contract");  // for Right to Sublicense column
					}
				}
				else {  // sourceId is null
					row.add("");  // source
					row.add("");  // source reference #
					row.add("");  // source contact info
					row.add("No Source");  // right to sublicense
				}
				rowList.add(row);
			}
		}

		HashMap<String, Object> model = new HashMap<String, Object>();
		model.put("data", rowList);
		model.put(ExcelView.FIRST_ROW_IS_HEADER, Boolean.TRUE);
		model.put(ExcelView.AUTO_SIZE_COLS, 8);
		model.put(ExcelView.AUTO_SIZE_MAX_WIDTH, 100);
		model.put(ExcelView.AUTO_SIZE_FIRST_ROW, new Integer(5));  // 0-based index

		model.put(ExcelView.HEADER_LEFT, "John Wiley and Sons, Inc. Confidential");
		SimpleDateFormat dateFormat = PermUserContext.getSimpleDateFormat(request);
		model.put(ExcelView.HEADER_CENTER, dateFormat.format(new Date()));

		String authorLname = "";
		if(null != product.getAuthor()){
			authorLname = product.getAuthor().getLastName();
		}

		String fileName = "SublicenseReport."+authorLname+"."+product.getIsbn13()+".xls";
		model.put(ExcelView.FILE_NAME, fileName);

		ModelAndView mv = new ModelAndView(getExcelView(), model);
		timer.stopTimer();

		return mv;
	}

	//Code Change for SS Task 3 - Added new parameter userGroupId
	@RequestMapping(value="/commonwork/report/photoDeptToComeSummary.xls",method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView viewPhotoDeptToComeSummary(@RequestParam(value = "userGroupId", required = false) Integer userGroupId,
			HttpServletRequest request, HttpServletResponse response)
			throws PersistenceException {

		ModelAndView mv = new ModelAndView("redirect:/sapp/commonwork/report/summary.xls");
		mv.addObject("userGroupId", userGroupId);
		mv.addObject("PhotoFlag", "Y");

		return mv;
	}

	//Code Change for SS Task 3 - Added new parameter userGroupId
	@RequestMapping(value="/commonwork/report/summary.xls",method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView summary(HttpServletRequest request,
			@RequestParam(value = "PhotoFlag", required = false) String photoFlag,
			@RequestParam(value = "userGroupId", required = false) Integer userGroupId) throws Exception {
		log.debug("summary(): entered..."+userGroupId);
		PerfTimer timer = cwRepository.getMonitor().startTimer("ReportsController::summary");
		CommonWork cw = PermUserContext.getCurrentCommonWork(request);

		SimpleDateFormat sdf = PermUserContext.getSimpleDateFormat(request);

		if (null == cw) {
			String msg = getMessageSource().getMessage("no.product.for.product.landing", null, null);
			ModelAndView mv = new ModelAndView(getUserLandingViewName() + "?generalMessage=" + msg);
			return mv;
		}

		Product product = cwRepository.loadWithExtendedPrimaryProductById(cw.getId()).getPrimaryProduct();

		// create a dual dimension List for rows/columns
		List<Object> rowList = new ArrayList<Object>();

		// No header row since in template spreadsheet
		int rowNum = 1;

		// authors can't see this report so just pass true for includeCovers
		// pass false for includeCanceled
		//Code Change for SS Task 3 -  Start
		//AssetUseSearchResults results = assetUseIndexService.getCWAssetUsesFromIndex(cw.getId(), true, true);
		AssetUseSearchResults results = assetUseIndexService.getCWAssetUsesFromIndexWithUserGroupId(cw.getId(),userGroupId, true, true);
		//Code Change for SS Task 3 - End
		//	Start : Updated for DM-284
		ArrayList<AssetUseSearchResult> docs = results.getDocuments();

		if("Y".equals(photoFlag)){
			rowList = newPhotoDeptComeReport(docs,cw.getId(),userGroupId,photoFlag,sdf);
		}else{
			rowList = newPermissionSummaryReport(docs,cw.getId(),userGroupId,photoFlag,sdf);
		}


		ArrayList<OneOffCellValue> oneOffList = new ArrayList<OneOffCellValue>();

		// Although we should always have an author for a product, it's possible not to.
		// (avoid NullPointer exception)
		if (StringUtils.isNotBlank(product.getAuthorsAsString())) {
			oneOffList.add(new OneOffCellValue(2, 1, product.getAuthorsAsString())); // C2
		}

		oneOffList.add(new OneOffCellValue(2, 2, product.getTitle())); // C3
		oneOffList.add(new OneOffCellValue(4, 2, product.getIsbn13())); // E3
		oneOffList.add(new OneOffCellValue(2, 3, (null != product.getAcquisitionsEditor() ? product.getAcquisitionsEditor().getFullName() : ""))); // C4

		oneOffList.add(new OneOffCellValue(4, 3, (null != product.getProductionDate() ? sdf.format(product.getProductionDate()) : ""))); // E4
		oneOffList.add(new OneOffCellValue(5, 3, "(" + sdf.toPattern().toLowerCase() + ")")); // E5
		oneOffList.add(new OneOffCellValue(3, 5, cwRepository.loadCommonWorkPermissionInfo(cw))); // D6

		//Start: Added for implementing Build RN 160121-001880
		List<CwFile> loadFiles = cwRepository.loadFiles(cw.getId());

		if(CollectionUtils.isNotEmpty(loadFiles)) {
			oneOffList.add(new OneOffCellValue(7, 1,"Title-Level Attachments:" )); // H2
			int temp = 0, calc = 0, colNum = 8;
			for(int i=0; i<loadFiles.size(); i++) {
				calc = i%3;
				if(temp == 3) {
					colNum = colNum + 1;
					temp = 0;
				}
				if (calc == 0) {
					oneOffList.add(new OneOffCellValue(colNum, 1, (i+1)+". "+loadFiles.get(i).getFileName()));
					temp++;
				}
				else if (calc == 1) {
					oneOffList.add(new OneOffCellValue(colNum, 2, (i+1)+". "+loadFiles.get(i).getFileName()));
					temp++;
				}
				else if (calc == 2) {
					oneOffList.add(new OneOffCellValue(colNum, 3, (i+1)+". "+loadFiles.get(i).getFileName()));
					temp++;
				}
			}
		}
		else {
			oneOffList.add(new OneOffCellValue(7, 1,"Title-Level Attachments:" )); // H2
			oneOffList.add(new OneOffCellValue(8, 1,"None" )); // I2
		}
		//End: Added for implementing Build RN 160121-001880

		HashMap<String, Object> model = new HashMap<String, Object>();
		ModelAndView mv = null;
		if ("Y".equals(photoFlag)) {
			oneOffList.add(new OneOffCellValue(25, 8, "Photo Request Comment"));//Changed the order as per DM-122
			oneOffList.add(new OneOffCellValue(0, 0, "Photo Dept. To Come report"));
		}else{
			oneOffList.add(new OneOffCellValue(25, 8, "Multi-Use"));// Added for DM-284
			oneOffList.add(new OneOffCellValue(26, 8, "GBPM Category"));//Changed the order as per DM-122
			String authorLname = "";
			if(null != product.getAuthor()){
				authorLname = product.getAuthor().getLastName();
			}
			String fileName = "PermissionSummaryReport."+authorLname+"."+product.getIsbn13()+".xls";
			model.put(ExcelView.FILE_NAME, fileName);
		}

		model.put("data", rowList);
		model.put(ExcelView.START_ROW, new Integer(9));
		model.put(ExcelView.ONE_OFF_CELL_VALUES, oneOffList);

		if ("Y".equals(photoFlag)) {
			mv = new ModelAndView(getSummaryExcelView(), model);
		}else{
			mv = new ModelAndView(getNewPermissionSummaryExcelView(), model);
		}
		timer.stopTimer();

		return mv;
		//	End : Updated for DM-284
	}


	//Start: Added to implement DM-122
	private String getFormatedSeatValue(String seat) {
		if(null != seat) {
			seat = seat.trim();
			if("Unlimited Seats are granted".equals(seat)) {
				return "Unlimited";
			} else if("Seats limitation is not mentioned".equals(seat)) {
				return "Unlimited - Not mentioned";
			} else {
				if(seat.startsWith("=")) {
					//seats - value comes as =10 for 10 seat limitation
					return seat.substring(1)+" Seat Limitation";
				}
			}
		}
		return seat;
	}
	//End: Added to implement DM-122

	//Added for 'Report For Legal' - Start
	@RequestMapping(value="/commonwork/report/insufficientApprovalReport.xls",method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView reportForLegal(HttpServletRequest request) throws Exception {
		PerfTimer timer = cwRepository.getMonitor().startTimer("ReportsController::reportForLegal");
		CommonWork cw = PermUserContext.getCurrentCommonWork(request);

		SimpleDateFormat sdf = PermUserContext.getSimpleDateFormat(request);

		if (null == cw) {
			String msg = getMessageSource().getMessage("no.product.for.product.landing", null, null);
			ModelAndView mv = new ModelAndView(getUserLandingViewName() + "?generalMessage=" + msg);
			return mv;
		}

		Product product = cwRepository.loadWithExtendedPrimaryProductById(cw.getId()).getPrimaryProduct();

		// create a dual dimension List for rows/columns
		List<Object> rowList = new ArrayList<Object>();

		// No header row since in template spreadsheet
		int rowNum = 1;

		List<Object[]> data = assetUseRepository.reportLegalAssetUseDetail(cw.getId());

		log.debug("reportForLegal data = " + data);

		for (Object[] columns : data) {
			int assetUseId = Integer.parseInt(columns[0].toString());
			int cwId = Integer.parseInt(columns[1].toString());
			AssetUseSearchResult result = assetUseIndexService.searchIndexByAssetUseId(assetUseId);
			if (null != result){
				ArrayList<Object> row = new ArrayList<Object>();
				row.add(new Integer(rowNum++));


				Map<String, String> info = assetUseRepository.loadReportForLegalInfo(cwId, assetUseId);
				log.debug("reportForLegal info = " + info);

				row.add(info.get("approverName"));
				row.add(info.get("approverRemarks"));

				row.add(result.getDescription());
				row.add(result.getCreditLine());
				row.add(result.getSourceNames());
				row.add(result.getSourceRef());
				row.add(result.getComponentName());
				row.add(result.getPosition());
				row.add(result.getMediaType());
				row.add(result.getUsage());
				row.add(result.getFinalPage());
				//row.add(result.getPermissionStatus());
				// Start : Added for DM-1606
				String permStatusCode = result.getPermissionStatusCode();
				if(permStatusCode.equalsIgnoreCase(PermissionStatus.FORM_SENT.getCode()))
				{
					boolean createdPO = assetRepository.checkForPoCreation(result.getAssetId());
					log.debug("AssetUseTableRowView() PO created "+createdPO+" for asset ID "+result.getAssetId());
					// if any PO created for an asset in Purchase_order table then display waiting on invoice else display form sent
					if(createdPO)
							row.add(PermissionStatus.FORM_WAITING_ON_INVOICE);
						else
							row.add(result.getPermissionStatus());
				}
				else
				{
					row.add(result.getPermissionStatus());
				}
				// End : Added for DM-1606

				row.add(info.get("editions"));
				row.add(info.get("medium"));
				row.add(info.get("territory"));
				row.add(info.get("language"));
				row.add(info.get("printRun"));
				row.add(info.get("derivatives"));
				row.add(info.get("sublicense"));
				row.add(info.get("invoiceNumber"));
				row.add("..DateBorder");
				row.add((null != info.get("startDate") ? sdf.format(sdf.parse(info.get("startDate"))) : ""));
				row.add("..DateBorder");
				row.add((null != info.get("expirationDate") ? sdf.format(sdf.parse(info.get("expirationDate"))) : ""));
				row.add("..DateBorder");
				row.add((null != info.get("invoiceDate") ? sdf.format(sdf.parse(info.get("invoiceDate"))) : ""));
				rowList.add(row);
			}
		}


		ArrayList<OneOffCellValue> oneOffList = new ArrayList<OneOffCellValue>();

		// Although we should always have an author for a product, it's possible not to.
		// (avoid NullPointer exception)
		if (StringUtils.isNotBlank(product.getAuthorsAsString())) {
			oneOffList.add(new OneOffCellValue(2, 1, product.getAuthorsAsString())); // C2
		}

		oneOffList.add(new OneOffCellValue(2, 2, product.getTitle())); // C3
		oneOffList.add(new OneOffCellValue(4, 2, product.getIsbn13())); // E3
		oneOffList.add(new OneOffCellValue(2, 3, (null != product.getAcquisitionsEditor() ? product.getAcquisitionsEditor().getFullName() : ""))); // C4

		oneOffList.add(new OneOffCellValue(4, 3, (null != product.getProductionDate() ? sdf.format(product.getProductionDate()) : ""))); // E4
		oneOffList.add(new OneOffCellValue(5, 3, "(" + sdf.toPattern().toLowerCase() + ")")); // E5
		oneOffList.add(new OneOffCellValue(3, 5, cwRepository.loadCommonWorkPermissionInfo(cw))); // D6

		HashMap<String, Object> model = new HashMap<String, Object>();

		String authorLname = "";
		if(null != product.getAuthor()){
			authorLname = product.getAuthor().getLastName();
		}
		String fileName = "InsufficientApprovalReport."+authorLname+"."+product.getIsbn13()+".xls";
		model.put(ExcelView.FILE_NAME, fileName);

		model.put("data", rowList);
		model.put(ExcelView.START_ROW, new Integer(9));
		model.put(ExcelView.ONE_OFF_CELL_VALUES, oneOffList);

		ModelAndView mv = new ModelAndView(getReportLegalExcelView(), model);
		timer.stopTimer();

		return mv;
	}
	//Added for 'Report For Legal' - End


	@RequestMapping(value="/commonwork/report/photocopyrightReport.xls",method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView photocopyrightReport(HttpServletRequest request,
			@RequestParam(value = "year", required = false) String year) throws Exception {
		log.debug("photocopyrightReport(): entered, year = " + year);
		PerfTimer timer = cwRepository.getMonitor().startTimer("ReportsController::photocopyright");

		if (null == year || year.trim().length() < 2) {
			year = String.valueOf(Calendar.getInstance().get(Calendar.YEAR));
		}

		List<Object> rowList = new ArrayList<Object>();
		List<Object> row = new ArrayList<Object>();

		row = new ArrayList<Object>();
		row.add("..Subhead");
		row.add("Photo Department report for Copyright year " + year);
		rowList.add(row);

		row = new ArrayList<Object>();
		row.add("");
		rowList.add(row);

		// headings
		row = new ArrayList<Object>();
		row.add("..Subhead");
		row.add("Photo Editor");
		row.add("Author");
		row.add("Title");
		row.add("Ms-Comp");
		row.add("Final Cost");
		row.add("Total Photos");
		row.add("Pickups");
		row.add("Free");
		row.add("Royalty Free");
		rowList.add(row);

		List<ProductSearchResult> resultList = productIndexService.photoCopyrightReport(Integer.parseInt(year));

		log.debug("photocopyrightReport(): # results = " + resultList.size());

		for (ProductSearchResult result : resultList) {
			row = new ArrayList<Object>();

			String peName = "";
			if (result.getPhotoEditorLastName() != null) peName = result.getPhotoEditorLastName();
			if (result.getPhotoEditorFirstName() != null) peName += " " + result.getPhotoEditorFirstName();
			row.add(peName);
			row.add(result.getAuthorsAsString());
			row.add(result.getTitle());
			row.add(result.getMsToComp());
			row.add(result.getFinalCost());
			row.add(result.getTotalPhotos());
			row.add(result.getPhotoCoverPickupCount());
			row.add(result.getPhotoInternalFreeCount());
			row.add(result.getPhotoInternalRoyalityFreeCount());

			rowList.add(row);
		}

		HashMap<String, Object> model = new HashMap<String, Object>();
		model.put("data", rowList);
		model.put(ExcelView.FIRST_ROW_IS_HEADER, Boolean.TRUE);
		model.put(ExcelView.AUTO_SIZE_COLS, 12);
		model.put(ExcelView.AUTO_SIZE_MAX_WIDTH, 80);
		model.put(ExcelView.AUTO_SIZE_FIRST_ROW, new Integer(2));  // 0-based index

		ModelAndView mv = new ModelAndView(getExcelView(), model);
		timer.stopTimer();

		return mv;
	}

	@RequestMapping(value="/commonwork/report/photoEditorStatusReport.xls",method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView photoEditorstatusReport(HttpServletRequest request,
			@RequestParam(value = "editorId", required = false) String editorId) throws Exception {
		log.debug("photoEditorstatusReport(): entered...");
		PerfTimer timer = cwRepository.getMonitor().startTimer("ReportsController::photoEditorStatus");

		if (null == editorId || editorId.trim().length() < 1) {
			String msg = "A Photo Editor must first be selected ";
			ModelAndView mv = new ModelAndView(getUserLandingViewName() + "?generalMessage=" + msg);
			return mv;
		}

		User photoEditor = getUserRepository().loadUserById(new Integer(editorId.trim()));

		List<Object> rowList = new ArrayList<Object>();

		List<Object> row = new ArrayList<Object>();
		row = new ArrayList<Object>();
		row.add("..Subhead");
		row.add("Photo Editor Status Report ");
		rowList.add(row);
		row = new ArrayList<Object>();
		row.add("");
		rowList.add(row);
		row = new ArrayList<Object>();
		row.add("..Subhead");
		row.add(photoEditor.getFirstName() + " " + photoEditor.getLastName());
		rowList.add(row);
		row = new ArrayList<Object>();
		row.add("");
		rowList.add(row);

		// headings
		row = new ArrayList<Object>();
		row.add("..Subhead");
		row.add("Title");
		row.add("Author");
		row.add("isbn13");
		row.add("Product Line");
		row.add("Copyright Year");
		row.add("Total Photos");
		row.add("        Editorial staff         ");
		row.add("Research Progress Notes");
		row.add("Permissions Due");
		row.add("Page Proof Schedule date");
		rowList.add(row);

		List<Object[]> data = cwRepository.loadPhotoEditorDetail(photoEditor.getId());
		for (Object[] columns : data) {
			String cw_id = columns[1].toString();
			String product_id = columns[0].toString();

			row = new ArrayList<Object>();
			row.add(columns[2]); // title
			row.add(productRepository.getAuthorsByProductId(new Integer(product_id)));
			row.add(columns[3]); // isbn13
			row.add(columns[5]); // Product Line
			row.add(columns[4]); // copyright year
			row.add(cwRepository.getTotalPhotoCount(new Integer(cw_id)));
			row.add("..WrappedText");
			row.add(productRepository.getPhotoEditorByProductId(new Integer(product_id))); // editorial staff

			row.add(columns[6]); // research progress notes
			if (null != columns[7]) {
				String dDate = columns[7].toString();
				dDate = StringUtils.replace(dDate, " 00:00:00.0", " ");
				row.add("..Date");
				row.add(dDate); // permissions due
			}
			if (null != columns[8]) {
				String dDate = columns[8].toString();
				dDate = StringUtils.replace(dDate, " 00:00:00.0", " ");
				row.add("..Date");
				row.add(dDate); // page proof schedule date
			}
			rowList.add(row);
		}

		HashMap<String, Object> model = new HashMap<String, Object>();
		model.put("data", rowList);
		model.put(ExcelView.FIRST_ROW_IS_HEADER, Boolean.TRUE);
		int autoSizeNumCols = 12;
		model.put(ExcelView.AUTO_SIZE_COLS, autoSizeNumCols);
		model.put(ExcelView.AUTO_SIZE_MAX_WIDTH, 200);

		ModelAndView mv = new ModelAndView(getExcelView(), model);
		timer.stopTimer();

		return mv;
	}

	@RequestMapping(value="/commonwork/report/assetSearch.xls",method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView assetSearch(HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		log.debug("assetSearch(): entered...");
		PerfTimer timer = cwRepository.getMonitor().startTimer("ReportsController::assetSearch");
		// The user has just done a search and has now clicked on the Export to Excel button.
		// So just get the form with the search criteria out of the session - it has already
		// been validated.
		HttpSession session = request.getSession();
		AssetSearchForm form = (AssetSearchForm) session.getAttribute(AssetUseIndexSearchController.MODEL_FORM_NAME);

		AssetUseSearchResults results = assetUseIndexService.searchIndex(form);

		// create a dual dimension List for rows/columns
		List<Object> rowList = new ArrayList<Object>();

		// set header
		List<Object> row = new ArrayList<Object>();
		if (!form.getIncludeCommonWorkId()) {
			row.add("Author(s)");
			row.add("Title");
			row.add("ISBN");
			row.add("\u00a9Year");
		}

		row.add("Description");
		row.add("Media Type");
		row.add("Source Name(s)");
		row.add("Component");
		row.add("Position");
		row.add("Usage");
		row.add("Status");
		row.add("Cancelled");

		rowList.add(row);

		for (AssetUseSearchResult r : results.getDocuments()) {
			row = new ArrayList<Object>();

			if (!form.getIncludeCommonWorkId()) {
				row.add(r.getAuthorNames());
				row.add(r.getTitle());
				row.add(r.getIsbn());
				row.add(r.getCopyrightYear());
			}

			row.add(r.getDescription());
			row.add(r.getMediaType());
			row.add(r.getSourceNames());
			row.add(r.getComponentName());
			row.add(r.getPosition());
			row.add(r.getUsage());
			row.add(r.getPermissionStatusDescription());
			row.add(r.isCanceled());

			rowList.add(row);
		}

		HashMap<String, Object> model = new HashMap<String, Object>();
		model.put("data", rowList);
		model.put(ExcelView.FIRST_ROW_IS_HEADER, Boolean.TRUE);
		int autoSizeNumCols = form.getIncludeCommonWorkId() ? 12 : 8;
		model.put(ExcelView.AUTO_SIZE_COLS, autoSizeNumCols);
		model.put(ExcelView.AUTO_SIZE_MAX_WIDTH, 80);

		ModelAndView mv = new ModelAndView(getExcelView(), model);
		timer.stopTimer();

		return mv;
	}

	@RequestMapping(value="/commonwork/report/artLog.pdf",method = {RequestMethod.GET, RequestMethod.POST})
	public void artLog(HttpServletRequest request,
			HttpServletResponse response,
			@RequestParam(value = "userGroupId", required = false) Integer userGroupId,
			@RequestParam(CW_ID) Integer cwId, @RequestParam("componentIds") String componentIds)
			throws Exception {
		log.debug("artLog(): entered, userGroupId = " + userGroupId);
	//	PerfTimer timer = cwRepository.getMonitor().startTimer("ReportsController::artLog");

		Product product = cwRepository.loadWithPrimaryProductById(cwId).getPrimaryProduct();

		HashMap<String, Object> model = new HashMap<String, Object>();
		model.put("CW_ID", cwId);
		model.put("PRODUCT_ID", product.getId());
		if (!userGroupId.equals(new Integer(0))) {
			UserGroup ug = cwRepository.find(UserGroup.class, userGroupId);
			model.put("USER_GROUP_FILTER", " and AU.user_group_id = " + userGroupId + " " );
			model.put("USER_GROUP_FILTER_NAME", ug.getDescription());
		} else {
			model.put("USER_GROUP_FILTER", " ");
			model.put("USER_GROUP_FILTER_NAME", "All ");

		}

		String[] compIds = StringUtils.split(componentIds, ",");
		// if all components, we have the 0 value in the array
		if (Arrays.asList(compIds).contains("0")) {
			model.put("COMPONENT_IDS", " ");
			model.put("COMPONENT_NAMES", " All ");
		} else {
			model.put("COMPONENT_IDS", " and AU.COMPONENT_ID IN (" + componentIds + ") ");
			model.put("COMPONENT_NAMES", cwRepository.loadComponentListNames(cwId, componentIds, userGroupId));
		}

		// Must call render right here in the controller because
		// if return the view and wait for Spring to render it,
		// then we loose the db Connection
		// return new ModelAndView(getPdfView(), model);
		getPdfView().render(model, request, response);
	//	timer.stopTimer();
	}


	/*
	 * Report generated here for Asset Compliance
	 */
	@RequestMapping(value="/commonwork/report/assetComplianceReport.xls",method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView assetComplianceReport(HttpServletRequest request,
			HttpServletResponse response,
			@RequestParam(value = "userGroupId", required = false) Integer userGroupId,
			@RequestParam(CW_ID) Integer cwId)
			throws Exception {
		log.debug("assetComplianceReport(): entered, userGroupId = " + userGroupId);

		HashMap<String, Object> model = new HashMap<String, Object>();
		List<Object> rowList = new ArrayList<Object>();
		List<Object> row = new ArrayList<Object>();
		row = new ArrayList<Object>();
		row.add("..WrappedText");
		row.add("..Head");
		row.add("GE Compliance Report ");
		rowList.add(row);

		// Added New Headers -Start
		CommonWork cw = PermUserContext.getCurrentCommonWork(request);

		if (null == cw) {
			String msg = getMessageSource().getMessage("no.product.for.product.landing", null, null);
			ModelAndView mv = new ModelAndView(getUserLandingViewName() + "?generalMessage=" + msg);
			return mv;
		}

		List<CwFile> loadFiles = cwRepository.loadFiles(cwId);// Added for implementing Build RN 160121-001880

		Product product = getProductRepository().lazyLoad(Product.class, cw.getPrimaryProduct().getId(), new String[] { "authors" });

		row = new ArrayList<Object>();
		row.add("..WrappedText");
		row.add("..Head");
		row.add("Author(s): " + product.getAuthorsAsString());
		row.add("..WrappedText");
		insertTitleFileNamesList(loadFiles, 1, row, "ACR");// Added for implementing Build RN 160121-001880
		rowList.add(row);

		row = new ArrayList<Object>();
		row.add("..WrappedText");
		row.add("..Head");
		row.add("Title: " + product.getTitle());
		row.add("..WrappedText");
		insertTitleFileNamesList(loadFiles, 2, row, "ACR");// Added for implementing Build RN 160121-001880
		rowList.add(row);

		row = new ArrayList<Object>();
		row.add("..WrappedText");
		row.add("..Head");
		row.add("Edition: " + product.getEditionNumber());
		row.add("..WrappedText");
		insertTitleFileNamesList(loadFiles, 3, row, "ACR");// Added for implementing Build RN 160121-001880
		rowList.add(row);

		row = new ArrayList<Object>();
		row.add("..WrappedText");
		row.add("..Head");
		row.add("ISBN: " + product.getIsbn13());
		row.add("..WrappedText");
		rowList.add(row);

		row = new ArrayList<Object>();
		row.add("..WrappedText");
		row.add("");
		row.add("..WrappedText");
		rowList.add(row);
		// Added New Headers -End

		// headings
		row = new ArrayList<Object>();
		row.add("..WrappedText");
		row.add("..Subhead");
		row.add("Component");
		row.add("Usage");
		row.add("Position");
		row.add("Description");
	    //ADDITION OF NEW COLUMN Status
		row.add("Status");
        //END
		row.add("Credit");
		row.add("Source");
		row.add("Source Ref#");
		row.add("Invoice # or Permission Letter");
		row.add("Invoice or Perm Date");
		row.add("Final Cost");//Added by Chandra as per RN Ticket No. 151007-000929
		row.add("Multi-Use");	// Added to DM-284
	    row.add("Check Number");
		row.add("Check Date");
		row.add("Is Royalty free");
		row.add("Seats");//Added to implement DM-122
		row.add("Exceptions to World All language rights for print/e-book/web/all manner of media/derivative works including /custom and international student version/life of the edition)");
		row.add("Comments");
		row.add("..WrappedText");
		rowList.add(row);
		log.debug("assetComplianceReport(): row info = " + row);

		List<Object[]> data = cwRepository.loadComplianceReportData(userGroupId,cwId);
		int rowCount=0;
		HashSet<Integer> assetIdSet = new HashSet<Integer>(); // Added for DM-284
		//	Start : Updated for DM-284
		ArrayList<AssetComplianceReportBean> reportBeanList = new ArrayList<AssetComplianceReportBean>();
		for (Object[] columns : data) {
			//row = new ArrayList<Object>();

			String component  = columns[0] == null? "" : ("" + columns[0]);
			String usage = columns[1] == null? "" : ("" + columns[1]);
			String position = columns[2] == null? "" : ("" + columns[2]);
			String description = columns[3] == null? "" : ("" + columns[3]);
			String status = columns[4] == null? "" : ("" + columns[4]);
			String creditLine = columns[5] == null? "" : ("" + columns[5]);
			String sourceName = columns[6] == null? "" : ("" + columns[6]);
			String sourceRef = columns[7] == null? "" : ("" + columns[7]);
			String invoiceOrPermLetter = columns[8] == null? "" : ("" + columns[8]);
			String invoiceOrPermDate = columns[9] == null? "" : ("" + columns[9]);
			double finalCost = 0.00;


			/*row.add("..WrappedText");

			row.add(columns[0]); // Component
			row.add(columns[1]); // Usage
			row.add(columns[2]); // Position
			row.add(columns[3]); // Description
			row.add(columns[4]); // Status
			row.add(columns[5]); // Credit Line
			row.add(columns[6]); // SourceName
			row.add(columns[7]); // Source Ref#
			row.add(columns[8]); // Invoice# or Permission Letter
			row.add(columns[9]); // Invoice or Perm Date
*/			//row.add(columns[10]);//Final Cost added by Chandra as per Right Now Ticket No. 151007-000929

			// Start : Added for DM-284
			  Integer assetInteger = Integer.valueOf(columns[20].toString());
			if (null == columns[10] || columns[10].equals("0.00")) {
			//	row.add("");
				finalCost = 0.00;
			} else if(assetIdSet.contains(assetInteger)) {
				finalCost = 0.00;
			}else{
				//assetIdSet.add(Integer.parseInt(info.get("assetId")));
				assetIdSet.add(assetInteger);
				//finalCost = Double.valueOf(columns[10].toString());
				log.debug("finalCost = "+finalCost);
				BigDecimal bigDeciFinalCost = getRepository().executeSingleResultNamedQuery("Asset.finalCost", new Object[] { assetInteger, cw.getId() });
				finalCost = bigDeciFinalCost.doubleValue();
			}
			//row.add(finalCost);


			String mutiUsageAsset = "No";
			String checkNumber = columns[11] == null? "" : ("" + columns[11]);
			String checkDate = columns[12] == null? "" : ("" + columns[12]);
			String royaltyFree = columns[13] == null? "" : ("" + columns[13]);
			String seatsvalue = "";
			String data1 = "";
			String comments = columns[15] == null? "" : ("" + columns[15]);


			// Condition for adding multi_usage column
			boolean isMultiUsage = assetUseRepository.isMultiUsageAsset(assetInteger,cwId);
			if(isMultiUsage)
			{
				log.debug("asset is multi usage : "+assetInteger);
				mutiUsageAsset = "Yes";
			}
			/*row.add(mutiUsageAsset);
			// End : Added for DM-284

			row.add(columns[11]); // Check Number
            row.add(columns[12]); // Check Date
            row.add(columns[13]);//Royalty Free
*/            //Start: Added to implement DM-122
            if(null != columns[19]) {
            	//row.add(columns[19]);//Seats value
            	seatsvalue = ""+columns[19];//Seats value
            } else if(null != columns[18]){
            	if(Integer.parseInt(columns[18].toString()) > 0) {
            		//row.add(columns[18]+" Seat Limitation");//Seats value
            		seatsvalue = ""+columns[18]+" Seat Limitation";//Seats value

            	}
            }
            //End: Added to implement DM-122
            //	End : Updated for DM-284
            log.debug("CONTRACT ID= "+columns[14]);
            log.debug("SOURCE ID=" +columns[16]);
            //Uncommented the if part of the code to avoid exception error in the UI
            //if(columns[13] ==null){
            	//columns[13] is the Contract ID
            //	data1="";
            //}
            //else{
            //Ends
            SimpleDateFormat inputDf = new SimpleDateFormat("yyyy-MM-dd");
            if(columns[14] != null || (columns[17] != null && columns[16] != null)){
            String invoiceDate = inputDf.format(columns[17])+ " 00:00:00";
            data1 = cwRepository.loadComplianceReportConditionData(columns[14],columns[16],invoiceDate);
            }else{
            	//do nothing
            }
            log.debug("data1 --> "+data1);
             if(data1 ==  null || data1.equals(null) || columns[13].equals(true) )
		     {
		      data1 = "";
		     } else
		       {
			         data1 = data1.toString().replace("[" , "");
			         data1 = data1.toString().replace("]" , "");
			         //Added by Rajesh as per the discussion with Amanda
			         data1 = data1.toString().replace("No mention of media types,", "");
			         data1 = data1.toString().replace("No mention of distribution/sales territories,", "");
			         data1 = data1.toString().replace("No mention of language,", "");
			         data1 = data1.toString().replace("Print run is not mentioned", "");
			         data1 = data1.toString().replace("No mention of derivative works", "");
			         //Ends
			        data1 = data1.toString().replace("All media types including future types,", "");
			        data1 = data1.toString().replace("Worldwide,", "");
			        data1 = data1.toString().replace("All Languages,", "");
			        data1 = data1.toString().replace("Unlimited print run is granted,", "");
			       // data1 = data1.toString().replace("Granted for this, future editions and/or entire author series,", "");
			        data1 = data1.toString().replace("Wiley can include the asset(s) in any ancillaries, derivatives and custom works", "");
			       // data1 = data1.toString().replace("Granted for this edition and all future editions,", "");
			        data1 = data1.toString().replace("All,", "");
			        data1 = data1.toString().replace("World,", "");
			        data1 = data1.toString().replace(",   ,", ",");
			        data1 = data1.toString().replace(", ,", ",");
			        data1=data1.trim();
			        if(data1 != null && !data1.isEmpty())
			        {
			         if(data1.charAt(data1.length()-1) == ',')
			         {
			          data1= data1.substring(0,data1.length()-1);
			         }
			        }
		       }



		//}
			/*row.add(data1);
			row.add(columns[15]); //Comments
			row.add("..WrappedText");
		    rowList.add(row);
			rowCount++;*/
			//	Start : Updated for DM-284
			AssetComplianceReportBean reportBean = new AssetComplianceReportBean();
			reportBean.setComponent(component );
			reportBean.setUsage(usage);
			reportBean.setPosition(position);
			reportBean.setDescription(description);
			reportBean.setStatus(status);
			reportBean.setCreditLine(creditLine);
			reportBean.setSourceName(sourceName);
			reportBean.setSourceRef(sourceRef);
			reportBean.setInvoiceOrPermLetter(invoiceOrPermLetter);
			reportBean.setInvoiceOrPermDate(invoiceOrPermDate);
			reportBean.setFinalCost(finalCost);
			reportBean.setMutiUsageAsset(mutiUsageAsset);
			reportBean.setCheckNumber(checkNumber);
			reportBean.setCheckDate(checkDate);
			reportBean.setRoyaltyFree(royaltyFree);
			reportBean.setSeatsvalue(seatsvalue);
			reportBean.setData1(data1);
			reportBean.setComments(comments);
			reportBeanList.add(reportBean);
		}
		Collections.sort(reportBeanList,AssetComplianceReportBean.finalCostComparator);
		Collections.reverse(reportBeanList);

		for(AssetComplianceReportBean bean : reportBeanList){
			row = new ArrayList<Object>();
			row.add("..WrappedText");
			row.add(bean.getComponent());
			row.add(bean.getUsage());
			row.add(bean.getPosition());
			row.add(bean.getDescription());
			row.add(bean.getStatus());
			row.add(bean.getCreditLine());
			row.add(bean.getSourceName());
			row.add(bean.getSourceRef());
			row.add(bean.getInvoiceOrPermLetter());
			row.add(bean.getInvoiceOrPermDate());
			row.add(bean.getFinalCost());
			row.add(bean.getMutiUsageAsset());
			row.add(bean.getCheckNumber());
			row.add(bean.getCheckDate());
			row.add(bean.getRoyaltyFree());
			row.add(bean.getSeatsvalue());
			row.add(bean.getData1());
			row.add(bean.getComments());
			row.add("..WrappedText");
			rowList.add(row);
			rowCount++;
		}
		//	End : Updated for DM-284
		int rowCount1=rowCount+9;
		int rowCount2=rowCount+10;
		int rowCount3=rowCount+11;

		SimpleDateFormat dateFormat = PermUserContext.getSimpleDateFormat(request);
		row = new ArrayList<Object>();
	    row.add("..WrappedText");
		row.add("..Subhead");
		row.add("");
		row.add("..WrappedText");
		rowList.add(row);

		row = new ArrayList<Object>();
		row.add("..WrappedText");
		row.add("..Subhead");
		row.add("");
		row.add("..WrappedText");
		rowList.add(row);

		row = new ArrayList<Object>();
		row.add("..WrappedText");
		row.add("..Subhead");
	    request.setAttribute("RowCount", rowCount1);
	    row.add("..Merge");
		row.add("PHOTO EDITOR SIGNATURE:________________________________________" );
		row.add("");
		row.add("");
		row.add("");
		row.add("");
		row.add("..WrappedText");
		row.add("..Date");
		row.add("..WrappedText");
		row.add("..Subhead");
		row.add("DATE: " + dateFormat.format(new Date()));
		row.add("..WrappedText");
		rowList.add(row);

		row = new ArrayList<Object>();
		row.add("..WrappedText");
		row.add("..Subhead");
		request.setAttribute("RowCount2", rowCount2);
		row.add("..Merge2");
		row.add("COMPLIANCE SIGNATURE:____________________________________________" );
		row.add("");
		row.add("");
		row.add("");
		row.add("");
		row.add("..WrappedText");
		row.add("..Date");
		row.add("..WrappedText");
		row.add("..Subhead");
		row.add("DATE: " + dateFormat.format(new Date()));
		row.add("..WrappedText");
		rowList.add(row);

		row = new ArrayList<Object>();
		row.add("..WrappedText");
		row.add("..Subhead");
		request.setAttribute("RowCount3", rowCount3);
		row.add("..Merge3");
		row.add("MELINDA PATELLI SIGNATURE:_______________________________________" );
		row.add("");
		row.add("");
		row.add("");
		row.add("");
		row.add("..WrappedText");
		row.add("..Date");
		row.add("..WrappedText");
		row.add("..Subhead");
		row.add("DATE: ");
		row.add("..WrappedText");
		rowList.add(row);


		model.put("data", rowList);
		model.put(ExcelView.FIRST_ROW_IS_HEADER, Boolean.TRUE);
		model.put(ExcelView.AUTO_SIZE_FIRST_ROW, 10);



		//START: Modified the column width
		int autoSizeNumCols =10;

		model.put(ExcelView.AUTO_SIZE_COLS, autoSizeNumCols);
		model.put(ExcelView.AUTO_SIZE_MAX_WIDTH, 15);
		String authorLname = "";
		if(null != product.getAuthor()){
			authorLname = product.getAuthor().getLastName();
		}

		String fileName = "AssetComplianceReport."+authorLname+"."+product.getIsbn13()+".xls";
		model.put(ExcelView.FILE_NAME, fileName);
		ModelAndView mv = new ModelAndView(getExcelView(), model);
		return mv;
	}

	// Start: Added for implementing Build RN 160121-001880
	private void insertTitleFileNamesList(List<CwFile> list, int rowPosition, List<Object> row, String fromReport) {
		int size = 0;
		int temp = 0;
		boolean firstTimeOnly = true;
		if(CollectionUtils.isNotEmpty(list)) {
			size = list.size();
			temp = rowPosition - 1;
			for (int i=0; i<size; i++) {
				if (rowPosition == 1) {
					if (firstTimeOnly) {
						for (int j=0; j<5; j++) {
							row.add(null);
						}
						row.add("..WrappedText");
						row.add("..Subhead");
						row.add("Title-Level Attachments:");
						if(null != fromReport && fromReport.equals("ACR")) {
							row.add(null);//This if code is to just to fix some alignment issue in the report
							row.add(null);
						}
						firstTimeOnly = false;
					}
					if(temp == i) {
						row.add("..WrappedText");
						row.add("..Subhead");
						row.add((i+1)+". "+list.get(i).getFileName());
						temp += 3;
					}
				}
				else {
					if (firstTimeOnly) {
						if(null != fromReport && fromReport.equals("ACR")) {
							for (int j=0; j<8; j++) {
								row.add(null);
							}
						}
						else {
							for (int j=0; j<6; j++) {
								row.add(null);
							}
						}

						firstTimeOnly = false;
					}
					if(temp == i) {
						row.add("..WrappedText");
						row.add("..Subhead");
						row.add((i+1)+". "+list.get(i).getFileName());
						temp += 3;
					}
				}
			}
		}
		else {
			if (rowPosition == 1) {
				for (int j=0; j<5; j++) {
					row.add(null);
				}
				row.add("..WrappedText");
				row.add("..Subhead");
				row.add("Title-Level Attachments:");
				if(null != fromReport && fromReport.equals("ACR")) {
					row.add(null);
					row.add(null);
				}
				row.add("..WrappedText");
				row.add("..Subhead");
				row.add("None");
			}
		}
		row.add("..WrappedText");
	}
	//End: Added for implementing Build RN 160121-001880

	/*
	 * GE Compliance Report
	 */
	@RequestMapping(value="/commonwork/report/assetCompliance.xls",method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView assetCompliance(HttpServletRequest request, HttpServletResponse response,
			@RequestParam("reportType") String reportType)
			throws PersistenceException {
		log.debug("assetCompliance(): entered..."+reportType);

		ModelAndView mv = new ModelAndView("pages.report.assetCompliance");

		List<UserGroup> userGroups = userRepository.loadUserGroupList();
		mv.addObject("userGroupList", userGroups);
		mv.addObject("reportType", reportType);
		return mv;
	}

	public Object[] findDetailLine(List<Object[]> data, String searchValue) {
		for (Object[] columns : data) {
			if (columns[0].toString().equals(searchValue)) {
				return columns;
			}
		}

		return null;
	}

	@RequestMapping(value="/commonwork/report/assignedProjects.xls",method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView assignedProjects(HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		log.debug("assignedProjects(): entered...");
		PerfTimer timer = cwRepository.getMonitor().startTimer("ReportsController::assignedProjects");
		UserPrincipal user = PermUserContext.getCurrentUser(request);

		// User duser = getUserRepository().loadUserById(user.getId());

		List<Object> dottedLine = new ArrayList<Object>();
		dottedLine.add("-----------------------------------------------------------------------------------");

		List<Object> blankLine = new ArrayList<Object>();
		blankLine.add(" ");

		if (null == user) {
			String msg = "unable to find user";
			ModelAndView mv = new ModelAndView(getUserLandingViewName() + "?generalMessage=" + msg);
			return mv;
		}

		List<Object> row = new ArrayList<Object>();
		// create a dual dimension List for rows/columns
		List<Object> rowList = new ArrayList<Object>();

		row = new ArrayList<Object>();
		row.add("Assigned Project(s) Change History");
		rowList.add(row);

		rowList.add(blankLine);

		row = new ArrayList<Object>();
		row.add("Projects Assigned to: " + user.getFirstName() + " " + user.getLastName() + " (" + user.getName() + ")");
		rowList.add(row);
		rowList.add(dottedLine);

		row = new ArrayList<Object>();
		row.add("..Subhead");
		row.add("ASSIGNED PRODUCTS");
		rowList.add(row);

		row = new ArrayList<Object>();
		row.add("..Subhead");
		row.add("Isbn10");
		row.add("Isbn13");
		row.add("Title");
		row.add("Authors");
		rowList.add(row);

		Properties properties = new Properties();
		properties.setProperty("user_id", String.valueOf(user.getId()));
		List<UserToRole> utr = getUserRepository().loadAll(UserToRole.class, properties);
		Iterator<UserToRole> iter = utr.iterator();
		while (iter.hasNext()) {
			UserToRole role = iter.next();
			if (null != role.getProduct()) {
				Product testProduct = getProductRepository().lazyLoad(Product.class, role.getProduct().getId(), new String[] { "authors", "commonWork", "publicationStatus" });
				if (testProduct.getPublicationStatus().getCode().equals("E") ||
						testProduct.getPublicationStatus().getCode().equals("I")) {

					row = new ArrayList<Object>();
					row.add(testProduct.getIsbn10());
					row.add(testProduct.getIsbn13());
					row.add(testProduct.getTitle());
					row.add(testProduct.getAuthorsAsString());
					rowList.add(row);
					row = new ArrayList<Object>();
					row.add("..Subhead");
					row.add("");
					row.add("Change History");
					rowList.add(row);

					List<CwHistory> historyList = cwRepository.getHistory(testProduct.getCommonWork().getId(), 6);
					for (int h = 0; h < historyList.size(); h++) {
						row = new ArrayList<Object>();
						row.add("");
						row.add(historyList.get(h).getDescription());
						row.add("On: " + historyList.get(h).getLastUpdatedDate() + " by:" + historyList.get(h).getLastUpdatedUser().getFullName());
						rowList.add(row);
					}
					rowList.add(blankLine);
				}
			}
		}

		HashMap<String, Object> model = new HashMap<String, Object>();
		model.put("data", rowList);
		model.put(ExcelView.FIRST_ROW_IS_HEADER, Boolean.TRUE);
		model.put(ExcelView.AUTO_SIZE_COLS, 12);
		model.put(ExcelView.AUTO_SIZE_MAX_WIDTH, 100);

		ModelAndView mv = new ModelAndView(getExcelView(), model);
		timer.stopTimer();

		return mv;
	}

	// Napoleon test
	@RequestMapping(value = "/commonwork/report/sourceUsageReport", method = RequestMethod.GET)
	public ModelAndView sourceSearch(HttpServletRequest request)
			throws Exception {
		log.debug("sourceSearch(): entered...");
		// The user has just done a search and has now clicked on the Export to Excel button.
		// So just get the form with the search criteria out of the session - it has already
		// been validated.
		SourceUsageReportForm form = new SourceUsageReportForm();
	//	form.setSearchSourceName("NapoleonTest");

		ArrayList<Source> sources = new ArrayList<Source>();

	//	Source source =  new Source();
	//	source.setId(99);
	//	source.setName("Napoleon Medrano");
	//	sources.add(source);

		form.setDivision("all");
		form.setSelectedSourceId(0);
		ModelAndView mv = new ModelAndView("pages.landing.reports.sourceUsageReport");
		form.setSources(sources);
		mv.addObject("SourceUsageReportForm", form);
		mv.addObject("Sources", sources);
		List<BusinessUnit> bu = getProductRepository().loadAll(BusinessUnit.class);
		mv.addObject("bu", bu);
		List<SourceGroup> sourceGroups = getSourceRepository().loadSourceGroupList();
		mv.addObject("sourceGroups", sourceGroups);

		String dateFormat = PermUserContext.getPickerDateFormat(request);
		mv.addObject("dateFormat", dateFormat);

		return mv;
	}

	@RequestMapping(value = "/commonwork/report/sourceUsageReport", method = RequestMethod.POST)
	public ModelAndView sourceUsage(HttpServletRequest request, HttpServletResponse response,
			@ModelAttribute("SourceUsageReportForm") SourceUsageReportForm form,
			@ModelAttribute("Sources") ArrayList<Source> sources)
			throws Exception {
		log.debug("sourceUsage(): entered...");
		PerfTimer timer = cwRepository.getMonitor().startTimer("ReportsController::sourceUsage");

		if (null != form.getSources()) {
			log.debug("sources from form size" + form.getSources().size());
			sources = form.getSources();
		} else {
			form.setSources(new ArrayList<Source>());
		}

		if (null != sources) {
			log.debug("form sources size:" + sources.size());
		} else {
			sources = new ArrayList<Source>();
		}

		if (null != form.getProcessingType() ) {
			log.debug("sourceUsage() processing type:" + form.getProcessingType());
		}

		if (null != form.getProcessingType() &&
				form.getProcessingType().equals("AddSourceGroup") &&
				null != form.getSelectedSourceGroup() && form.getSelectedSourceGroup() != 0) {
			log.debug("selected Source group =" + form.getSelectedSourceId());
			sources = form.getSources();
			List<Source> sgSources = sourceRepository.getSourcesInSourceGroup(form.getSelectedSourceGroup());
			// only add new sources
			for (int x = 0; x < sgSources.size(); x++) {
				Source ns = new Source();
				ns.setName(sgSources.get(x).getName());
				ns.setId(sgSources.get(x).getId());
				if (!sources.contains(ns)) {
					sources.add(ns);
				}
			}
		}

		if (null != form.getProcessingType() && form.getProcessingType().equals("AddNoFly")) {
			sources = form.getSources();
			List<Source> maSources = sourceRepository.loadDisabledSources();
			// only add new sources
			for (int x = 0; x < maSources.size();x ++) {
				Source ns = new Source();
				ns.setName(maSources.get(x).getName());
				ns.setId(maSources.get(x).getId());
				if (!sources.contains(ns)) {
					sources.add(ns);
				}
			}

		}

		if (null != form.getProcessingType() && form.getProcessingType().equals("addPreferred")) {
			sources = form.getSources();
			List<Source> maSources = sourceRepository.loadPreferedVendors();
			// only add new sources
			for (int x = 0; x < maSources.size();x ++) {
				Source ns = new Source();
				ns.setName(maSources.get(x).getName());
				ns.setId(maSources.get(x).getId());
				if (!sources.contains(ns)) {
					sources.add(ns);
				}
			}
		}

		if (null != form.getProcessingType() && form.getProcessingType().equals("AddMASources")) {
			sources = form.getSources();
			List<Source> maSources = sourceRepository.loadMasterAgreementSources();
			// only add new sources
			for(int x = 0; x < maSources.size();x ++) {
				Source ns = new Source();
				ns.setName(maSources.get(x).getName());
				ns.setId(maSources.get(x).getId());
				if(!sources.contains(ns)) {
					sources.add(ns);
				}
			}
		}

		if (null != form.getProcessingType() && form.getProcessingType().equals("remove")) {
			sources = form.getSources();
			for(int x=0;x<sources.size();x++) {
				log.debug("Removing source:---->" +sources.get(x).getId()+"and"+ form.getSelectedSourceId());
				if(sources.get(x).getId().equals(form.getSelectedSourceId())) {
					sources.remove(x);
					break;
				}
			}
		}

		if (null != form.getProcessingType() && form.getProcessingType().equals("add")) {
			sources = form.getSources();
			log.debug("processing source:" + form.getSelectedSourceId());
			Source source =  new Source();
			source.setId(form.getSelectedSourceId());
			source.setName(form.getSearchSourceName());
			sources.add(source);
		}

		if (null != form.getProcessingType() && form.getProcessingType().compareTo("report") != 0) {
			// first sort sources
			Collections.sort(sources, new Comparator<Source>() {

				@Override
				public int compare(Source p1, Source p2)
				{
					String t1 = "";
					String t2 = "";
					if (null != p1.getName())
						t1 = p1.getName();
					if (null != p2.getName())
						t2 = p2.getName();
					return t1.compareToIgnoreCase(t2);
				}
			});

			form.setSources(sources);
			log.debug("total source count:" + sources.size());
			ModelAndView mv = new ModelAndView("pages.landing.reports.sourceUsageReport");
			List<SourceGroup> sourceGroups = getSourceRepository().loadSourceGroupList();
			mv.addObject("sourceGroups",sourceGroups);
			form.setSelectedSourceId(0);
			form.setSearchSourceName("");
			form.setProcessingType("");
			mv.addObject("SourceUsageReportForm", form);
			List<BusinessUnit> bu = getProductRepository().loadAll(BusinessUnit.class);
			mv.addObject("bu",bu );
			mv.addObject("Sources", sources);

			return mv;
		}

		String selections = "";


		log.debug("total source count:" + sources.size());

		List<Object> rowList = new ArrayList<Object>();

		List<Object> row = new ArrayList<Object>();
		row.add("Permission System Source Usage Report");
		rowList.add(row);

		if (sources.size() < 1) {
			row.add("There were no sources selected for the report");
			rowList.add(row);
		} else {
			row = new ArrayList<Object>();
			row.add("..Subhead");
			row.add("");
			if (null == form.getReportType()) {
				form.setReportType(2);
			}
			if (form.getReportType() == 2) {
				row.add("Invoice");
			} else {
				row.add("AssetDescription");
			}
			row.add("Component");
			row.add("Usage");
			row.add("Position");
			row.add("ManPage");
			row.add("MediaType");
			if (form.getReportType() == 2) {
				row.add("AssetDescription");
			} else {
				row.add("Invoice");
			}
			row.add("CreditLine");
			row.add("SourceRef");
			row.add("FinalPage");
			row.add("PermissionStatus");
			row.add("InvocieDate");
			row.add("PermissionDate");
			row.add("Reused");
			row.add("EstimatedCost");
			row.add("FinalCost");
			row.add("RoyaltyFree");
			row.add("Pickup");
			row.add("Comment");
			rowList.add(row);

			String sourceIdList = "";
			for (int x=0; x < form.getSources().size(); x++) {
				Source wSource = form.getSources().get(x);
				if (x > 0) sourceIdList = sourceIdList + ", ";
				sourceIdList = sourceIdList + wSource.getId();
			}


			if (null != sourceIdList) {
				selections =  selections + " and c.source_id in (" + sourceIdList + ")";
			}

			if (! form.getAssetsToShow().equals(1)) {
				if(form.getAssetsToShow().equals(2)) {
					selections = selections + " and comp.name like '%Cover%' ";
				} else {
					selections = selections + " and (comp.name not like '%Cover%' or comp.name is null)";
				}
			}

			if (null != form.getProductLines() && form.getProductLines().length() > 0) {
				String productLines = "";
		     	StringTokenizer st = new StringTokenizer(form.getProductLines(),",");
		     	while (st.hasMoreTokens()) {
		     		productLines = productLines + "'" + st.nextToken().trim()+ "'";
		     		if(st.hasMoreTokens()) productLines = productLines + ", ";
		     	}
		     	selections = selections + " and pl.code in (" + productLines + ") ";
			}
			if (null != form.getDivision() && form.getDivision().compareTo("all") != 0) {
				selections = selections + " and prod.business_unit = '" + form.getDivision() + "' ";
			}

			if (null != form.getDateType()) {
				SimpleDateFormat inputDf = new SimpleDateFormat("yyyy-MM-dd");
				String fromDate = null;
				String toDate = null;
				if (null != form.getFromDate()) {
					fromDate = inputDf.format(form.getFromDate()) + " 00:00:00";
				}
				if (null != form.getToDate()) {
					toDate = inputDf.format(form.getToDate()) + " 60:60:99";
				}

				if (form.getDateType() == 1) {
					if (null != fromDate) {
						selections = selections + " and prod.consolidated_release_date >= '" + fromDate + "' ";
					}
					if (null != toDate) {
						selections = selections + " and prod.consolidated_release_date <= '" + toDate + "' ";
					}
				} else {
					if (null != fromDate) {
						selections = selections + " and c.date >= '" + fromDate + "' ";
					}
					if (null != toDate) {
						selections = selections + " and c.date <= '" + toDate + "' ";
					}
				}
			}

			if (null != form.getProductStatus()) {
				if (form.getProductStatus().equals("inactive")) {
					selections = selections + " and prod.pub_status in('O','B','R','X') ";
				} else {
					selections = selections + " and prod.pub_status not in('O','B','R','X') ";
				}
			}


			if (form.getReportType() == 2) {
				selections = selections  + " order by src.name, prod.title,  a.description, c.number, a.description ";
			} else {
				selections = selections  + " order by src.name, prod.title,  a.description, a.description, c.number ";
			}


			String prevSource = "";
			String prevIsbn = "";

			List<Map<String, String>> rs = assetUseRepository.loadSourceDetailReportView (selections);
			Iterator <Map<String, String>> results = rs.iterator();
			//Added for the Asset USe Calulation Task by rbongoni
			HashSet<Integer> assetIdSet = new HashSet<Integer>();
			//ends
			while (results.hasNext()) {
				Map<String, String> cells = results.next();
				if (prevSource.compareTo(cells.get("SourceName")) != 0) {
					row = new ArrayList<Object>();
					row.add("..Subhead");
					row.add("Source: " + cells.get("SourceName"));
					rowList.add(row);
					prevSource = cells.get("SourceName");
				}

				if (null != cells.get("Isbn") && prevIsbn.compareTo(cells.get("Isbn")) != 0) {
					row = new ArrayList<Object>();
					row.add("..Subhead");
					row.add("          Isbn: " + cells.get("Isbn") );
					rowList.add(row);
					row = new ArrayList<Object>();
					row.add("..Subhead");
					row.add("         Title: " + cells.get("Title"));
					rowList.add(row);

					row = new ArrayList<Object>();
					row.add("..Subhead");
					row.add("          Product Line: " + cells.get("ProductLine") + " Pub Status: " + cells.get("PubStatus"));
					rowList.add(row);
					prevIsbn = cells.get("Isbn");
				}

				row = new ArrayList<Object>();

				row.add("");
				if (form.getReportType() == 2) {
					row.add(cells.get("Invoice"));
				} else {
					row.add(cells.get("AssetDescription"));
				}
				row.add(cells.get("Component"));
				row.add(cells.get("Usage"));
				row.add(cells.get("Position"));
				row.add(cells.get("ManPage"));
				row.add(cells.get("MediaType"));
				if (form.getReportType() == 2) {
					row.add(cells.get("AssetDescription"));
				} else {
					row.add(cells.get("Invoice"));
				}
				row.add(cells.get("CreditLine"));
				row.add(cells.get("SourceRef"));
				row.add(cells.get("FinalPage"));
				row.add(cells.get("PermissionStatus"));
				row.add("..Date");
				row.add(cells.get("InvocieDate"));
				row.add("..Date");
				row.add(cells.get("PermissionDate"));
				row.add(cells.get("Reused"));
				row.add(cells.get("EstimatedCost"));

				//Added for the Asset USe Calulation Task by rbongoni
				if (assetIdSet.contains(cells.get("AssetId"))) {
					row.add("0.00");
				} else {
					assetIdSet.add(Integer.parseInt(cells.get("AssetId")));
					row.add(cells.get("FinalCost"));
				}
				//Ends
				row.add(cells.get("RoyaltyFree"));
				row.add(cells.get("Pickup"));
				row.add(cells.get("Comment"));
				rowList.add(row);
			}
		}

		HashMap<String, Object> model = new HashMap<String, Object>();
		model.put("data", rowList);
		model.put(ExcelView.FIRST_ROW_IS_HEADER, Boolean.TRUE);
		model.put(ExcelView.AUTO_SIZE_COLS, 23);
		model.put(ExcelView.AUTO_SIZE_MAX_WIDTH, 80);

		ModelAndView mv = new ModelAndView(getExcelView(), model);
		timer.stopTimer();

		return mv;
	}


	// SELECTION VIEWS ------------------------------------------------------------------------

	@RequestMapping(value="/commonwork/report/viewPhotoResearch.pdf" ,method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView viewPhotoResearch(HttpServletRequest request, HttpServletResponse response)
			throws PersistenceException {
		log.debug("viewPhotoResearch(): entered...");
		CommonWork cw = PermUserContext.getCurrentCommonWork(request);

		if (null == cw) {
			String msg = getMessageSource().getMessage("no.product.for.product.landing", null, null);
			ModelAndView mv = new ModelAndView(getUserLandingViewName() + "?generalMessage=" + msg);
			return mv;
		}

		ModelAndView mv = new ModelAndView("pages.report.photoResearch");

		List<Component> componentList = cwRepository.loadComponentList(cw.getId(), true);
		mv.addObject("componentList", componentList);

		List<UserGroup> userGroups = userRepository.loadUserGroupList();
		mv.addObject("userGroupList", userGroups);

		mv.addObject("userGroupId", new Integer(0));

		String dateFormat = PermUserContext.getPickerDateFormat(request);
		mv.addObject("dateFormat",dateFormat);

		return mv;
	}

	@RequestMapping(value="/commonwork/report/submitPhotoResearch.pdf",method = {RequestMethod.GET, RequestMethod.POST})
	public Object submitPhotoResearch(@RequestParam(value = "dateToProd", required = true) Date dateToProd,
			@RequestParam(value = "componentId", required = true) Integer[] componentIds,
			@RequestParam(value = "userGroupId", required = false) Integer userGroupId,
			HttpServletRequest request, HttpServletResponse response)
			throws PersistenceException, IOException {
		log.debug("submitPhotoResearch(): entered...");
		log.debug("user group id:" + userGroupId);
		CommonWork cw = PermUserContext.getCurrentCommonWork(request);

		if (null == cw) {
			String msg = getMessageSource().getMessage("no.product.for.product.landing", null, null);
			ModelAndView mv = new ModelAndView(getUserLandingViewName() + "?generalMessage=" + msg);
			return mv;
		}

		response.setContentType("text/plain");
		PrintWriter writer = response.getWriter(); // throws IOException

		assetUseRepository.updateDateToProduction (dateToProd, cw.getId(), componentIds, userGroupId);
		writer.write(request.getContextPath() + "/sapp/commonwork/report/artLog.pdf?" +
				"commonWorkId=" + cw.getId() + "&userGroupId=" + userGroupId + "&componentIds=" + StringUtil.arrayToString(componentIds, ","));
		return null;
	}

	/*
	 * Asset Compliance Report to generate the report
	 */
	@RequestMapping(value="/commonwork/report/submitAssetCompliance.xls",method = {RequestMethod.GET, RequestMethod.POST})
	public Object submitAssetCompliance(@RequestParam(value = "userGroupId", required = false) Integer userGroupId,
			HttpServletRequest request, HttpServletResponse response)
			throws PersistenceException, IOException {
		log.debug("submitAssetCompliance(): entered...");
		log.debug("user group id:" + userGroupId);

		CommonWork cw = PermUserContext.getCurrentCommonWork(request);

		if (null == cw) {
			String msg = getMessageSource().getMessage("no.product.for.product.landing", null, null);
			ModelAndView mv = new ModelAndView(getUserLandingViewName() + "?generalMessage=" + msg);
			return mv;
		}

		response.setContentType("text/plain");
		PrintWriter writer = response.getWriter(); // throws IOException

		writer.write(request.getContextPath() + "/sapp/commonwork/report/assetComplianceReport.xls?" +
				"commonWorkId=" + cw.getId() + "&userGroupId=" + userGroupId);
		return null;
	}

	//Code Change for SS Task 3 - Start
	/*
	 * Permission Summary Report to generate the report
	 */
	@RequestMapping(value="/commonwork/report/submitPermSummary.xls",method = {RequestMethod.GET, RequestMethod.POST})
	public Object submitPermSummary(@RequestParam(value = "userGroupId", required = false) Integer userGroupId,
			HttpServletRequest request, HttpServletResponse response)
			throws PersistenceException, IOException {
		log.debug("submitPermSummary(): entered...");
		log.debug("user group id:" + userGroupId);

		CommonWork cw = PermUserContext.getCurrentCommonWork(request);

		if (null == cw) {
			String msg = getMessageSource().getMessage("no.product.for.product.landing", null, null);
			ModelAndView mv = new ModelAndView(getUserLandingViewName() + "?generalMessage=" + msg);
			return mv;
		}

		response.setContentType("text/plain");
		PrintWriter writer = response.getWriter(); // throws IOException

		writer.write(request.getContextPath() + "/sapp/commonwork/report/summary.xls?" +
				"commonWorkId=" + cw.getId() + "&userGroupId=" + userGroupId);
		return null;
	}

	/*
	 * Source Summary Report to generate the report
	 */
	@RequestMapping(value="/commonwork/report/submitSourceSummary.xls",method = {RequestMethod.GET, RequestMethod.POST})
	public Object submitSourceSummary(@RequestParam(value = "userGroupId", required = false) Integer userGroupId,
			HttpServletRequest request, HttpServletResponse response)
			throws PersistenceException, IOException {
		log.debug("submitSourceSummary(): entered...");
		log.debug("user group id:" + userGroupId);

		CommonWork cw = PermUserContext.getCurrentCommonWork(request);

		if (null == cw) {
			String msg = getMessageSource().getMessage("no.product.for.product.landing", null, null);
			ModelAndView mv = new ModelAndView(getUserLandingViewName() + "?generalMessage=" + msg);
			return mv;
		}

		response.setContentType("text/plain");
		PrintWriter writer = response.getWriter(); // throws IOException

		writer.write(request.getContextPath() + "/sapp/commonwork/report/sourceSummary.xls?" +
				"commonWorkId=" + cw.getId() + "&userGroupId=" + userGroupId);
		return null;
	}

	/*
	 * Product Detail Report to generate the report
	 */
	@RequestMapping(value="/commonwork/report/submitCommonWorkDetail.xls",method = {RequestMethod.GET, RequestMethod.POST})
	public Object submitCommonWorkDetail(@RequestParam(value = "userGroupId", required = false) Integer userGroupId,
			HttpServletRequest request, HttpServletResponse response)
			throws PersistenceException, IOException {
		log.debug("submitCommonWorkDetail(): entered...");
		log.debug("user group id:" + userGroupId);

		CommonWork cw = PermUserContext.getCurrentCommonWork(request);

		if (null == cw) {
			String msg = getMessageSource().getMessage("no.product.for.product.landing", null, null);
			ModelAndView mv = new ModelAndView(getUserLandingViewName() + "?generalMessage=" + msg);
			return mv;
		}

		response.setContentType("text/plain");
		PrintWriter writer = response.getWriter(); // throws IOException

		writer.write(request.getContextPath() + "/sapp/commonwork/report/commonWorkDetail.xls?" +
				"commonWorkId=" + cw.getId() + "&userGroupId=" + userGroupId);
		return null;
	}

	/*
	 * Sublicense Report to generate the report
	 */
	@RequestMapping(value="/commonwork/report/submitSublicense.xls",method = {RequestMethod.GET, RequestMethod.POST})
	public Object submitSublicense(@RequestParam(value = "userGroupId", required = false) Integer userGroupId,
			HttpServletRequest request, HttpServletResponse response)
			throws PersistenceException, IOException {
		log.debug("submitSublicense(): entered...");
		log.debug("user group id:" + userGroupId);

		CommonWork cw = PermUserContext.getCurrentCommonWork(request);

		if (null == cw) {
			String msg = getMessageSource().getMessage("no.product.for.product.landing", null, null);
			ModelAndView mv = new ModelAndView(getUserLandingViewName() + "?generalMessage=" + msg);
			return mv;
		}

		response.setContentType("text/plain");
		PrintWriter writer = response.getWriter(); // throws IOException

		writer.write(request.getContextPath() + "/sapp/commonwork/report/sublicense.xls?" +
				"commonWorkId=" + cw.getId() + "&userGroupId=" + userGroupId);
		return null;
	}

	/*
	 * Source Summary Report to generate the report
	 */
	@RequestMapping(value="/commonwork/report/submitPhotoDeptToComeSummary.xls",method = {RequestMethod.GET, RequestMethod.POST})
	public Object submitPhotoDeptToComeSummary(@RequestParam(value = "userGroupId", required = false) Integer userGroupId,
			HttpServletRequest request, HttpServletResponse response)
			throws PersistenceException, IOException {
		log.debug("submitPhotoDeptToComeSummary(): entered...");
		log.debug("user group id:" + userGroupId);

		CommonWork cw = PermUserContext.getCurrentCommonWork(request);

		if (null == cw) {
			String msg = getMessageSource().getMessage("no.product.for.product.landing", null, null);
			ModelAndView mv = new ModelAndView(getUserLandingViewName() + "?generalMessage=" + msg);
			return mv;
		}

		response.setContentType("text/plain");
		PrintWriter writer = response.getWriter(); // throws IOException

		writer.write(request.getContextPath() + "/sapp/commonwork/report/photoDeptToComeSummary.xls?" +
				"commonWorkId=" + cw.getId() + "&userGroupId=" + userGroupId);
		return null;
	}

	/*
	 * Asset Details Report to generate the report
	 */
	@RequestMapping(value="/commonwork/report/submitAssetDetail.xls",method = {RequestMethod.GET, RequestMethod.POST})
	public Object submitAssetDetail(@RequestParam(value = "userGroupId", required = false) Integer userGroupId,
			HttpServletRequest request, HttpServletResponse response)
			throws PersistenceException, IOException {
		log.debug("submitAssetDetail(): entered...");
		log.debug("user group id:" + userGroupId);

		CommonWork cw = PermUserContext.getCurrentCommonWork(request);

		if (null == cw) {
			String msg = getMessageSource().getMessage("no.product.for.product.landing", null, null);
			ModelAndView mv = new ModelAndView(getUserLandingViewName() + "?generalMessage=" + msg);
			return mv;
		}

		response.setContentType("text/plain");
		PrintWriter writer = response.getWriter(); // throws IOException

		writer.write(request.getContextPath() + "/sapp/commonwork/report/assetDetail.xls?" +
				"commonWorkId=" + cw.getId() + "&userGroupId=" + userGroupId);
		return null;
	}

	/*
	 * Asset Compliance Report to generate the report
	 */
	@RequestMapping(value="/commonwork/report/submitAssetCredits.xls",method = {RequestMethod.GET, RequestMethod.POST})
	public Object submitAssetCredits(@RequestParam(value = "userGroupId", required = false) Integer userGroupId,
			HttpServletRequest request, HttpServletResponse response)
			throws PersistenceException, IOException {
		log.debug("submitAssetCompliance(): entered...");
		log.debug("user group id:" + userGroupId);

		CommonWork cw = PermUserContext.getCurrentCommonWork(request);

		if (null == cw) {
			String msg = getMessageSource().getMessage("no.product.for.product.landing", null, null);
			ModelAndView mv = new ModelAndView(getUserLandingViewName() + "?generalMessage=" + msg);
			return mv;
		}

		response.setContentType("text/plain");
		PrintWriter writer = response.getWriter(); // throws IOException

		writer.write(request.getContextPath() + "/sapp/commonwork/report/assetCredits.xls?" +
				"commonWorkId=" + cw.getId() + "&userGroupId=" + userGroupId);
		return null;
	}
	//Code Change for SS Task 3 - End

	@RequestMapping(value="/commonwork/report/viewSummary.xls",method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView viewSummary(HttpServletRequest request, HttpServletResponse response)
			throws PersistenceException {
		log.debug("viewSummary(): entered...");
		CommonWork cw = PermUserContext.getCurrentCommonWork(request);

		if (null == cw) {
			String msg = getMessageSource().getMessage("no.product.for.product.landing", null, null);
			ModelAndView mv = new ModelAndView(getUserLandingViewName() + "?generalMessage=" + msg);
			return mv;
		}

		ModelAndView mv = new ModelAndView(getPermissionSummaryView());

		List<Component> componentList = cwRepository.loadComponentList(cw.getId(), true);
		mv.addObject("componentList", componentList);

		return mv;
	}

	@RequestMapping(value="/commonwork/report/viewPhotoEditorStatusReport.xls",method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView viewPhotoEditorStatusReport(HttpServletRequest request, HttpServletResponse response)
			throws PersistenceException {
		log.debug("viewPhotoEditorStatusReport(): entered...");
		ModelAndView mv = new ModelAndView(getViewPhotoEditorStatusExcelView());

		try {
			List<LabelValueBean> editors = new ArrayList<LabelValueBean>();

			List<Object[]> data = cwRepository.loadPhotoEditors();
			for (Object[] columns : data) {
				LabelValueBean editor = new LabelValueBean(columns[1] + "", columns[0] + "");
				editors.add(editor);
			}

			mv.addObject("editorsList", editors);
			mv.addObject("cwId", PermUserContext.getCurrentCommonWork(request).getId());
		}
		catch (Exception e) {
			log.warn("viewPhotoEditorStatusReport(): ", e);
		}

		return mv;
	}

	@RequestMapping(value="/commonwork/report/viewDivestmentReport.xls",method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView viewDivestmentReport(HttpServletRequest request, HttpServletResponse response)
			throws PersistenceException
	{
		log.debug("viewDivestmentReport(): entered...");
		List<BusinessUnit> bu = getProductRepository().loadAll(BusinessUnit.class);
		List<ProductLine> pl = getProductRepository().loadAll(ProductLine.class);

		ModelAndView mv = new ModelAndView(getViewDivestmentReportExcelView());
		mv.addObject("bu",bu );
		mv.addObject("pl",pl );
//		mv.addObject("cwId", PermUserContext.getCurrentCommonWork(request).getId());
		String dateFormat = PermUserContext.getPickerDateFormat(request);
		mv.addObject("dateFormat",dateFormat);

		return mv;
	}


	// this is work in progress we need details of what to include in the spreadsheet
	@RequestMapping(value="/commonwork/report/divestmentReport.xls",method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView divestmentReport(HttpServletRequest request,
			@RequestParam(value = "yearFrom", required = false) String yearFrom,
			@RequestParam(value = "yearTo", required = false) String yearTo,
			@RequestParam(value = "editorCode", required = false) String editorCode,
			@RequestParam(value = "businessUnit", required = false) String businessUnit,
			@RequestParam(value = "productLine", required = false) String productLine,
			@RequestParam(value = "crdFrom", required = false) String crdFrom,
			@RequestParam(value = "crdTo", required = false) String crdTo,
			@RequestParam(value = "workedFrom", required = false) String workedFrom,
			@RequestParam(value = "workedTo", required = false) String workedTo
			) throws Exception
	{
		log.debug("divestmentReport(): entered...");
		PerfTimer timer = cwRepository.getMonitor().startTimer("ReportsController::divestment");

		List<Object> rowList = new ArrayList<Object>();
		List<Object> row = new ArrayList<Object>();
		List<Object> blankLine = new ArrayList<Object>();

		HashMap<String, Object> model = new HashMap<String, Object>();
		model.put("data", rowList);
		model.put(ExcelView.FIRST_ROW_IS_HEADER, Boolean.TRUE);
		int autoSizeNumCols =  20 ;
		model.put(ExcelView.AUTO_SIZE_COLS, autoSizeNumCols);
		model.put(ExcelView.AUTO_SIZE_MAX_WIDTH, 90);

		ModelAndView mv = new ModelAndView(getExcelView(), model);

		row = new ArrayList<Object>();
		row.add("..Subhead");
		row.add("Divestment Report");
		rowList.add(row);
		rowList.add(blankLine);

		row = new ArrayList<Object>();
		row.add("..Subhead");
		row.add("Report Criteria");
		rowList.add(row);

		row = new ArrayList<Object>();
		String stringYearTo = yearTo + "";
		// if (null == yearTo || yearTo.trim().length() < 1) stringYearTo = Calendar.getInstance().get(Calendar.YEAR) + "      ";
		String stringYearFrom = yearFrom + "     ";
		if (null == yearFrom || yearFrom.length() < 1) stringYearFrom = "any year";
		if (null == yearTo || yearTo.length() < 1) stringYearTo = "any year";
		row.add("Copyright Year From: " + stringYearFrom + "    To: " + stringYearTo );
		rowList.add(row);

		row = new ArrayList<Object>();
		String stringCrdFrom = crdFrom.toString();
		if (null == stringCrdFrom || stringCrdFrom.length() < 1) stringCrdFrom = "any date   ";
		String stringCrdTo = crdTo.toString();
		if (null == stringCrdTo || stringCrdTo.length() < 1) stringCrdTo = "any date";
		row.add("               CRD From: " + stringCrdFrom + " To: " + stringCrdTo);
		rowList.add(row);

		row = new ArrayList<Object>();

		String stringWorkedFrom = workedFrom.toString();
		if (null == stringWorkedFrom || stringWorkedFrom.length() < 1) stringWorkedFrom = "any date   ";
		String stringWorkedTo = workedTo.toString();
		if (null == stringWorkedTo || stringWorkedTo.length() < 1) stringWorkedTo = "any date";
		row.add("    Wordked On From: " + stringWorkedFrom + " To: " + stringWorkedTo);
		rowList.add(row);
		row = new ArrayList<Object>();

		if (null == editorCode || editorCode.length() < 1) {
			row.add("   Editor Code: All Selected" );
		} else {
			row.add("   Editor Code: " + editorCode);
		}
		rowList.add(row);

		row = new ArrayList<Object>();
		if (null == businessUnit || businessUnit.length() < 1) {
			row.add("Division: All Selected");
		} else {
			List<BusinessUnit> bu = getProductRepository().loadAll(BusinessUnit.class);
			String buString = "";
			for (int x=0; x < bu.size(); x++) {
				if (bu.get(x).getCode().equals(businessUnit)) buString = bu.get(x).getName();
			}
			row.add("Division: " + buString );
		}
		rowList.add(row);


		row = new ArrayList<Object>();
		if (null == productLine || productLine.length() < 1) {
			row.add("  Product Line: All selected");
		} else {
			List<ProductLine> pl = getProductRepository().loadAll(ProductLine.class);
			String plString = "";
			for (int x=0; x < pl.size(); x++) {
				if (pl.get(x).getId().toString().equals(productLine)) plString = pl.get(x).getCode();
			}
			row.add("  Product Line: " + plString);
		}
		rowList.add(row);

		rowList.add(blankLine);

		row = new ArrayList<Object>();
		row.add("..Subhead");
		row.add("Busiess unit");
		row.add("Product Line");
		row.add("Editor");
		row.add("ISBN10");
		row.add("ISBN13 ");
		row.add("Title");
		row.add("Copyright Year");
		row.add("CRD");
		row.add("Last Updated");
		row.add("# of assets in GBPM/Specs");
		row.add("Total Assets");
		row.add("Cover Assets");
		row.add("Other Assets");  // Non-Cover
		row.add("Status OK");
		row.add("Status Not OK");
		row.add("In Progress");
		row.add("Not Requested");

		rowList.add(row);

		// smarkoff: note this data is also in the product index - could get from there if wanted to
		List<Object[]> data = cwRepository.loadEditorUsageDetail(yearFrom, yearTo, editorCode,
										businessUnit, productLine, crdFrom, crdTo, workedFrom, workedTo);
		for (Object[] columns : data) {
			row = new ArrayList<Object>();
			row.add(columns[1]);  // business Unit
			row.add(columns[2]);  // product Line
			row.add(columns[4]);  // editor
			row.add(columns[7]);  // isbn10
			row.add(columns[8]);  // isbn13
			row.add(columns[9]);  // title
			row.add(columns[10]);  // copyright year
			String crd = "";
			if (null != columns[11] ) crd = columns[11].toString();
			if (crd.equals("(null)")) {
				crd = "";
			}
			if (crd.length() > 10) crd = crd.substring(0, 10);
			row.add(crd);  // CRD

			String lastUpdated = "";
			if (null != columns[20] ) lastUpdated = columns[20].toString();
			if (lastUpdated.length() > 10) lastUpdated = lastUpdated.substring(0, 10);
			row.add("..Date");
			row.add(lastUpdated);  // Loast Updated date

			int tbpmTotal = 0;
			if (null != columns[12] && columns[12].toString().compareTo("(null)") != 0) {
				tbpmTotal = new Integer(columns[12].toString());
			}

			row.add(tbpmTotal + "");  // Total from GBPM
			row.add(columns[13]);  // total Assets
			row.add(columns[14]);  // cover assets
			row.add(columns[15]);  // other (non-cover) assets
			row.add(columns[16]);  // status OK
			row.add(columns[17]);  // status not OK
			row.add(columns[19]);  // status in progress
			row.add(columns[18]);  // not requested

			rowList.add(row);
		}

		model.put("data", rowList);
		timer.stopTimer();

		return mv;
	}


	@RequestMapping(value="/commonwork/report/noflyList.xls",method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView noflyListy(HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		log.debug("noflyList(): entered...");
		PerfTimer timer = cwRepository.getMonitor().startTimer("ReportsController::noflyList");

		List<Object> blankLine = new ArrayList<Object>();
		List <Source> sources = getSourceRepository().loadDisabledSources();


		// create a dual dimension List for rows/columns
		List<Object> rowList = new ArrayList<Object>();
		// set header
		List<Object> row = new ArrayList<Object>();
		row = new ArrayList<Object>();
		row.add("..Subhead");
		row.add("No Fly Source List");
		rowList.add(row);
		rowList.add(blankLine);

		row = new ArrayList<Object>();
		row.add("..Subhead");
		row.add("Source");
		row.add("Photographer Last Name");
		row.add("Sued Wiley");
		row.add("No Fly Comments");
		rowList.add(row);

		rowList.add(blankLine);

		for (Source source: sources) {
			row = new ArrayList<Object>();
			row.add(source.getExternalName());
			row.add(source.getNoFlyPhotographerLastName());

			row.add(source.isSuedWiley() ? "Yes" : "No");
			row.add(source.getComment());

			rowList.add(row);
		}

		HashMap<String, Object> model = new HashMap<String, Object>();

		model.put("data", rowList);
		model.put(ExcelView.START_ROW, new Integer(1));

		model.put(ExcelView.AUTO_SIZE_COLS, 5);
		model.put(ExcelView.AUTO_SIZE_MAX_WIDTH, 500);

		//Added By Santhosh for REQ0376879
		String ldate=sourceRepository.loadLastUpdatedDate();
		String fileName = "noFlyList_"+ldate+".xls";
		model.put(ExcelView.FILE_NAME, fileName);
		//end

		ModelAndView mv = new ModelAndView(getExcelView(), model);
		timer.stopTimer();

		return mv;
	}

	//Start: Added for DM-532
	@RequestMapping(value="/commonwork/report/bulkDocumentsUpload.xls",method = {RequestMethod.GET, RequestMethod.POST})
	public Object documentsUploadReport(@RequestParam(value = "cwId", required = true) Integer cwid,
			@RequestParam(value = "uploadHistoryId", required = true) Integer uploadHistoryId,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		log.debug("documentsUploadReport(): entered... with cwId :"+cwid+": uploadHistoryId :"+uploadHistoryId+":");

		response.setContentType("text/plain");
		PrintWriter writer = response.getWriter(); // throws IOException

		writer.write(request.getContextPath() + "/sapp/commonwork/report/uploadStatuses.xls?" +
				"cwId=" + cwid + "&uploadHistoryId=" + uploadHistoryId);
		return null;
	}

	@RequestMapping(value="/commonwork/report/uploadStatuses.xls",method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView uploadStatusReport(@RequestParam(value = "cwId", required = true) Integer cwid,
			@RequestParam(value = "uploadHistoryId", required = true) Integer uploadHistoryId,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		log.debug("uploadStatusReport(): entered... with cwId :"+cwid+": uploadHistoryId :"+uploadHistoryId+":");
		PerfTimer timer = cwRepository.getMonitor().startTimer("ReportsController::documentsUploadReport");

		List<Object> rowList = new ArrayList<Object>();
		List<Object> row;

		row = new ArrayList<Object>();
		row.add("..Subhead");
		row.add("Document Upload Status:");
		rowList.add(row);

		// add blank row
		row = new ArrayList<Object>();
		rowList.add(row);

		row = new ArrayList<Object>();
		row.add("..Subhead");
		row.add("Component");
		row.add("Media Type");
		row.add("Description");
		row.add("Source");
		row.add("Source Ref. Number");
		row.add("Document Name");
		row.add("Upload Status");
		rowList.add(row);

		List<UploadedDocumentsDetails> uploadDocsList = contractRepository.loadUploadDocsDetailsByHistoryId(uploadHistoryId);

		if(CollectionUtils.isNotEmpty(uploadDocsList)) {
			for(UploadedDocumentsDetails doc : uploadDocsList) {
				row = new ArrayList<Object>();
				row.add(doc.getComponent());
				row.add(doc.getMediaType());
				row.add(doc.getDescription());
				row.add(doc.getSource());
				row.add(doc.getSourceRefNumber());
				row.add(doc.getDocumentName());
				row.add(doc.getUploadStatus());
				rowList.add(row);
			}
		}

		HashMap<String, Object> model = new HashMap<String, Object>();
		model.put("data", rowList);
		model.put(ExcelView.FIRST_ROW_IS_HEADER, Boolean.TRUE);
		model.put(ExcelView.AUTO_SIZE_COLS, 7);
		model.put(ExcelView.AUTO_SIZE_MAX_WIDTH, 100);
		model.put(ExcelView.AUTO_SIZE_FIRST_ROW, new Integer(5));  // 0-based index

		model.put(ExcelView.HEADER_LEFT, "John Wiley and Sons, Inc. Confidential");
		SimpleDateFormat dateFormat = PermUserContext.getSimpleDateFormat(request);
		model.put(ExcelView.HEADER_CENTER, dateFormat.format(new Date()));

		String fileName = "DocumentUploadStatus.xls";
		model.put(ExcelView.FILE_NAME, fileName);

		ModelAndView mv = new ModelAndView(getExcelView(), model);
		timer.stopTimer();

		return mv;
	}
	//End: Added for DM-532

	// Start: Added for DM-534
			@RequestMapping(value="/commonwork/report/exportExcelReport.xls",method = {RequestMethod.GET, RequestMethod.POST})
			public Object exportExcelReport(@RequestParam(value = "cwId", required = true) Integer cwid,
					HttpServletRequest request, HttpServletResponse response)
					throws Exception {
				log.debug("exportExcelReport(): entered... with cwId :"+cwid);

				response.setContentType("text/plain");
				PrintWriter writer = response.getWriter(); // throws IOException

				writer.write(request.getContextPath() + "/sapp/commonwork/report/exportAssetToExcel.xls?" +
						"cwId=" + cwid );
				log.debug("checking---------------------------->");
				return null;
			}

			@RequestMapping(value="/commonwork/report/exportAssetToExcel.xls",method = {RequestMethod.GET})
			public ModelAndView exportAssetToExcel(@RequestParam(value = "cwId", required = true) Integer cwid,
					HttpServletRequest request, HttpServletResponse response)
					throws Exception {
				log.debug("exportAssetToExcel(): entered... with cwId :"+cwid);
				ModelAndView mv=null;
				HashMap<String, Object> model = new HashMap<String, Object>();
				PerfTimer timer = cwRepository.getMonitor().startTimer("ReportsController::exportExcelReport");

				// create a dual dimension List for rows/columns
				List<Object> rowList = new ArrayList<Object>();

				// No header row since in template spreadsheet
				int rowNum = 1;
				int count=0;
				int totalcount=0;
				String sendSuccessEmail="yes";
				Integer productId = productRepository.getPrimaryProductId(cwid);
				Product primaryProduct = null;
				primaryProduct = productRepository.lazyLoad(Product.class, productId, new String[] {"users"});
				UserPrincipal user = PermUserContext.getCurrentUser(request);
				Integer userId = PermUserContext.getCurrentUserId(request);
				log.debug("user id---->"+userId);
				try {

				AssetUseSearchResults results = assetUseIndexService.getCWAssetUsesFromIndexForExcel(cwid,false, false, false);

				ArrayList<AssetUseSearchResult> docs = results.getDocuments();
				totalcount=docs.size();

				/*Not usable code anyhow we are restricting in UI
				 * ExportAsset exportstatus = cwRepository.loadexportStatus(cwid,userId);
				if(exportstatus ==null) {
					cwRepository.saveExportAsset("in_progress",cwid,userId,docs.size());
				}
				else {
					cwRepository.updateExportAsset("in_progress", cwid, userId,docs.size());
				}*/

				cwRepository.saveExportAsset("in_progress",cwid,userId,docs.size());

				for (int x = 0; x < docs.size(); x++) {
					String contractFiles = "";
					String invoiceNumbers = "";
					String invoiceDates = "";
					String currencies = "";
					int costs = 0;
					String startDates = "";
					String endDates = "";
					String cccRightLinkLicNo = "";
					String printRunUnlimited = "";
					String printRunLimitBox = "";
					String PrintRunIncludeEbooks = "";
					String ebookPrintRun = "";
					String sizeLimitation = "";
					String sizeGranted = "";
					String allLanguageGranted = "";
					String languageLimitation = "";
					String worldSalesTerGranted = "";
					String salesTerLimitation = "";
					String ancDerCusRightsGranted = "";
					String ancDerCusRightsLimitation = "";
					String ClrAllFutureMediaTyp = "";
					String mediaLimitation = "";
					String clrAllFutureEditions = "";
					String editionLimitation = "";
					String subLicGranted = "";
					String perComField = "";
					String prodComField = "";
					String compCopies = "";
					String compCopyName = "";
					String compCopyAddress = "";
					String compCopyCity = "";
					String compCopyProvince = "";
					String compCopyPostalCode = "";
					String compCopyCountry = "";
					String continuedUse = "";
					String reUsePickUpISBN = "";
					String continuedUsePos = "";
					String continuedUsePageNo = "";
					String continuedUseComments = "";
					AssetUseSearchResult result = docs.get(x);
					ArrayList<Object> row = new ArrayList<Object>();

					Asset asset=assetUseRepository.getAssetRepository().loadAssetById(result.getAssetId());

					row.add("");
					row.add(result.getSortOrder());
					row.add(result.getComponentName());
					row.add(result.getUsage());
					row.add(result.getMediaType());
					row.add(result.getPosition());
					row.add(result.getDescription());
					row.add(result.getArtist());
					row.add(result.getSourceNames());
					row.add(result.getSourceRef());
					if(asset.getOriginalPublicationIsbn() != null) {
						row.add(asset.getOriginalPublicationIsbn());}
					else {
					row.add("");}
					if(asset.getOriginalPublicationTitle() != null) {
						row.add(asset.getOriginalPublicationTitle());}
					else {
					row.add("");}
					if(asset.getOriginalArticleTitle() != null) {
						row.add(asset.getOriginalArticleTitle());}
					else {
					row.add("");}
					if(asset.getOriginalPublicationAuthor() != null) {
						row.add(asset.getOriginalPublicationAuthor());}
					else {
					row.add("");}
					if(asset.getOriginalPageNumber() != null) {
						row.add(asset.getOriginalPageNumber());}
					else {
					row.add("");}
					if(asset.getOriginalPublicationDate() != null) {
						DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
						String ldate=dateFormat.format(asset.getOriginalPublicationDate());
						row.add(ldate);}
					else {
					row.add("");}
					row.add(result.getCreditLine());
					if(asset.getOwnerType() != null) {
						row.add(asset.getOwnerType().getDescription());}
					else {
					row.add("");}
					if(asset.getModelRelease() != null) {
						row.add(asset.getModelRelease().getName());}
					else {
					row.add("");}
					row.add(result.isRoyaltyFree() ? "Yes" : "No");

					row.add("");//public domain
					row.add(asset.isWorkForHire()? "Yes" : "No");
					row.add("");//fair use
					log.debug("Going to get Contract details");

					List<Contract> clist = getContractRepository().loadListForAssetCW(result.getAssetId(), result.getCommonWorkId());
					// New Enhancement(Suggested by Amanda for an asset with multiple contracts) Start
					if(clist.size()>0)
					{
						for(Contract contractInfo : clist)
						{
							log.debug("Inside for contract check--------->");
							//Contract Files
							if(contractInfo.getId()!=null) {
							log.debug("Contract id------------------>"+contractInfo.getId()+"asset id------------------>"+result.getAssetId());
							String filenames= getContractRepository().loadContractFiles(contractInfo.getId());
							if(filenames !=null) {
								if(contractFiles.length()>0)
									contractFiles = contractFiles+","+filenames;
								else
									contractFiles=filenames;
								}

							}
							DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
							contractInfo = getContractRepository().lazyLoad(Contract.class, contractInfo.getId(), new String[] {"currency","compCopies"});
							if(contractInfo!=null)
							{
								// Invoice Number
								if(contractInfo.getNumber()!=null && !contractInfo.getNumber().isEmpty())
								{
									invoiceNumbers = normalizeString(invoiceNumbers,contractInfo.getNumber());
								}

								// Invoice Date
								if(contractInfo.getDate()!=null)
								{
									invoiceDates = normalizeString(invoiceDates,String.valueOf(dateFormat.format(contractInfo.getDate())));
								}

								// currencies
								if(contractInfo.getCurrency().getDescription()!=null)
								{
									currencies = normalizeString(currencies,String.valueOf(contractInfo.getCurrency().getDescription()));
								}

								// Cost of an Asset
								if(contractInfo.getPrice()>0)
								{
									costs = costs + (int)contractInfo.getPrice();
								}
								else{
									costs = (int)contractInfo.getPrice();
								}

								// Contract StartDate
								if(contractInfo.getStartDate()!=null)
								{
									startDates = normalizeString(startDates,String.valueOf(dateFormat.format(contractInfo.getStartDate())));
								}

								// Contract EndDate
								if(contractInfo.getEndDate()!=null)
								{
									endDates = normalizeString(endDates,String.valueOf(dateFormat.format(contractInfo.getEndDate())));
								}

								List<Condition> conditions=contractInfo.getConditions();
								HashMap<String, String> tempMap = new HashMap<String,String>();
								for(Condition c :conditions) {
									log.debug("Condition Type---------->"+c.getType());
									log.debug("Condition value---------->"+c.getValue());
									tempMap.put(c.getType().getCode(), c.getValue());
										/*if(c.getValue()!=null)
										{
											tempMap.put(c.getType().getCode(), c.getValue());
										}
										else
										{
											tempMap.put(c.getType().getCode(), c.getRollupValue());
										}*/
									}

								// Print Run Unlimited
								if(tempMap.get("print_run_unlimited") != null) {
								if(tempMap.get("print_run_unlimited").equals("true")){
									printRunUnlimited = normalizeString(printRunUnlimited, "YES");
									// row.add("YES");
								}
								else {
									printRunUnlimited = normalizeString(printRunUnlimited, "NO");
									// row.add("NO");
								}
								}

								// Print Run Limit Box
								if(tempMap.containsKey("print_run_limit_box")) {
									printRunLimitBox = normalizeString(printRunLimitBox, tempMap.get("print_run_limit_box"));
									// row.add(tempMap.get("print_run_limit_box"));
								}

								// Print Run Ebook
								if(tempMap.containsKey("print_run_ebook")) {
									printRunLimitBox = normalizeString(printRunLimitBox, tempMap.get("print_run_ebook"));
									// row.add(tempMap.get("print_run_ebook"));
								 }

								// All Language Selected
								if(tempMap.containsKey("language_all_alias")) {
									if(tempMap.get("language_all_alias") != null) {
									if(tempMap.get("language_all_alias").equals("true")){
										allLanguageGranted = normalizeString(allLanguageGranted, "YES");
										// row.add("YES");
									}
									else {
										allLanguageGranted = normalizeString(allLanguageGranted, "NO");
										// row.add("NO");
									}
									}
								}

								tempMap.remove("language_all_alias");

								if(tempMap.containsKey("language"))
									tempMap.remove("language");

								if(tempMap.containsKey("language_all"))
									tempMap.remove("language_all");

								if(tempMap.containsKey("language_a_c"))
									tempMap.remove("language_a_c");

								if(tempMap.containsKey("language_d_h"))
									tempMap.remove("language_d_h");

								if(tempMap.containsKey("language_eng_alias"))
									tempMap.remove("language_eng_alias");

								if(tempMap.containsKey("language_i_l"))
									tempMap.remove("language_i_l");

								if(tempMap.containsKey("language_m_r"))
									tempMap.remove("language_m_r");

								if(tempMap.containsKey("language_s"))
									tempMap.remove("language_s");

								if(tempMap.containsKey("language_t_z"))
									tempMap.remove("language_t_z");
								for(Map.Entry<String,String> entry : tempMap.entrySet()){
									String entryKey = entry.getKey();
									// For Muliple Languages
									if(entry.getKey().startsWith("language_")){
										languageLimitation = normalizeString(languageLimitation, getContractRepository().loadConditionDesc(entry.getKey()));
										// break;
									}
								}

								//sales
								if(tempMap.containsKey("sales_world_alias")) {
									if(tempMap.get("sales_world_alias") != null) {
									if(tempMap.get("sales_world_alias").equals("true")){
										worldSalesTerGranted = normalizeString(worldSalesTerGranted, "YES");
										// row.add("YES");
									}
									else {
										worldSalesTerGranted = normalizeString(worldSalesTerGranted, "NO");
										// row.add("NO");
									}
									}
								}
								tempMap.remove("sales_world_alias");

								if(tempMap.containsKey("sales_world"))
									tempMap.remove("sales_world");

								if(tempMap.containsKey("sales"))
									tempMap.remove("sales");

								if(tempMap.containsKey("sales_no_mention"))
									tempMap.remove("sales_no_mention");

								for(Map.Entry<String,String> entry : tempMap.entrySet()){
									if(entry.getKey().startsWith("sales_")){
										salesTerLimitation = normalizeString(salesTerLimitation, getContractRepository().loadConditionDesc(entry.getKey()));
										//break;
									}
								}

								//derivatives
								if(tempMap.containsKey("dwork_all")) {
									if(tempMap.get("dwork_all") != null) {
									if(tempMap.get("dwork_all").equals("true")){
										ancDerCusRightsGranted = normalizeString(ancDerCusRightsGranted, "YES");
										// row.add("YES");
									}
									else {
										ancDerCusRightsGranted = normalizeString(ancDerCusRightsGranted, "NO");
									}
									}
								}

								if(tempMap.containsKey("dwork_anc_and_deriv")) {
									if(tempMap.get("dwork_anc_and_deriv") != null) {
									if(tempMap.get("dwork_anc_and_deriv").equals("true")){
										ancDerCusRightsLimitation = normalizeString(ancDerCusRightsGranted, "No Custom");
										// row.add("No Custom");
									}
									}
								}
								else
								if(tempMap.containsKey("dwork_main_only")) {
									if(tempMap.get("dwork_main_only") != null) {
									if(tempMap.get("dwork_main_only").equals("true")){
										ancDerCusRightsLimitation = normalizeString(ancDerCusRightsGranted, "No Derivative or Custom");
										// row.add("No Derivative or Custom");
									}
									}
								}

								//medium
								if(tempMap.containsKey("medium_all")) {
									if(tempMap.get("medium_all") != null) {
									if(tempMap.get("medium_all").equals("true")){
										ClrAllFutureMediaTyp = normalizeString(ClrAllFutureMediaTyp, "YES");
										// row.add("YES");
									}
									else {
										ClrAllFutureMediaTyp = normalizeString(ClrAllFutureMediaTyp, "NO");
										// row.add("NO");
									}

									}
								}
								if(tempMap.containsKey("medium_all_physical")) {
									if(tempMap.get("medium_all_physical") != null) {
									if(tempMap.get("medium_all_physical").equals("true")){
										mediaLimitation = normalizeString(mediaLimitation, "All Physical Media including Print and CD");
										// row.add("All Physical Media including Print and CD");
									}
									}
								}
								else
								if(tempMap.containsKey("medium_physical_electronic")) {
									if(tempMap.get("medium_physical_electronic") != null) {
									if(tempMap.get("medium_physical_electronic").equals("true")){
										mediaLimitation = normalizeString(mediaLimitation,"All Physical and Electronic Media (but not Future Types)" );
										// row.add("All Physical and Electronic Media (but not Future Types)");
									}

									}
								}
								else
								if(tempMap.containsKey("medium_print_only")) {
									if(tempMap.get("medium_print_only") != null) {
									if(tempMap.get("medium_print_only").equals("true")){
										mediaLimitation = normalizeString(mediaLimitation, "Print Only");
										// row.add("Print Only)");
									}

									}
								}

								//edition
								if(tempMap.containsKey("edition_all_c_and_f")) {
									if(tempMap.get("edition_all_c_and_f") != null) {
									if(tempMap.get("edition_all_c_and_f").equals("true")){
										clrAllFutureEditions = normalizeString(clrAllFutureEditions, "YES");
										// row.add("YES");
									}
									else {
										clrAllFutureEditions = normalizeString(clrAllFutureEditions, "NO");
										// row.add("NO");
									}
									}
								}
								if(tempMap.containsKey("edition_this")) {
									if(tempMap.get("edition_this") != null) {
									if(tempMap.get("edition_this").equals("true")){
										editionLimitation = normalizeString(editionLimitation, "This Edition Only");
										// row.add("This Edition Only)");
									}
									}
								}

								//sublicence
								if(tempMap.containsKey("sublicense_right")) {
									if(tempMap.get("sublicense_right") != null) {
									if(tempMap.get("sublicense_right").equals("true")){
										subLicGranted = normalizeString(subLicGranted, "YES");
										// row.add("YES");
									}
									else {
										subLicGranted = normalizeString(subLicGranted, "NO");
										// row.add("NO");
									}
									}
								}
							}
							// compcopies
							log.debug("Comp copies size "+contractInfo.getCompCopies().size());
							if(! contractInfo.getCompCopies().isEmpty()) {
								CompCopy comp = contractInfo.getCompCopies().get(0);
								compCopies = comp.getNumberOfCopies().toString() ;
								compCopyName = result.getSourceNames();
								// row.add(comp.getNumberOfCopies().toString());
								// row.add(result.getSourceNames());
								log.debug(comp.getAddressId());
								Address add = sourceRepository.lazyLoad(Address.class, comp.getAddressId(), new String[] { "type" });
								StringBuilder address= new StringBuilder();
								if(null !=add.getLineOne())
									address.append(add.getLineOne());
								if(null !=add.getLineTwo() )
									address.append(" ,"+add.getLineTwo());
								if(null != add.getLineThree() )
									address.append(" ,"+add.getLineThree());

								compCopyAddress = String.valueOf(address);
								compCopyCity = add.getCity();
								compCopyProvince = add.getProvince();
								compCopyPostalCode = add.getPostalCode();
								compCopyCountry = add.getCountry().getDescription();
								/*row.add(address);
								row.add(add.getCity());
								row.add(add.getProvince());
								row.add(add.getPostalCode());
								row.add(add.getCountry().getDescription());*/
							}

						}
					}

					// Permission Comment
					if(result.getPermissionComment()!=null) {
						perComField = result.getPermissionComment();
						// row.add(result.getPermissionComment());
						}

					// Production Comment
					if(result.getProductionComment()!=null) {
						prodComField = result.getProductionComment();
						// row.add(result.getProductionComment());
						}

					AssetUse assetuse =assetUseRepository.lazyLoad(AssetUse.class, result.getAssetUseId(),new String[] { "status" });
					if(assetuse.isReusedFromPreviousEdition()) {

						continuedUse = "Reused" ;
						reUsePickUpISBN = assetuse.getReusedISBN();
						continuedUsePos = assetuse.getReusedPosition();
						continuedUsePageNo = assetuse.getReusedPage();
						continuedUseComments = assetuse.getReusedComment();
					/*	row.add("Reused");
						row.add(assetuse.getReusedISBN());
						row.add(assetuse.getReusedPosition());
						row.add(assetuse.getReusedPage());
						row.add(assetuse.getReusedComment());*/
					}
					else {
						if(assetuse.isPickup()) {
						continuedUse = "Pickup" ;
						reUsePickUpISBN = assetuse.getPickupISBN();
						continuedUsePos = assetuse.getPickupPosition();
						continuedUsePageNo = assetuse.getPickupPage();
						continuedUseComments = assetuse.getPickupComment();

						/*row.add("Pickup");
						row.add(assetuse.getPickupISBN());
						row.add(assetuse.getPickupPosition());
						row.add(assetuse.getPickupPage());
						row.add(assetuse.getPickupComment());*/
						}
					}

					// Adding contract details(ie.from column h to all) to row
					row.add(contractFiles);
					row.add(invoiceNumbers);
					row.add(invoiceDates);
					row.add(currencies);
					row.add(costs);
					row.add(startDates);
					row.add(endDates);
					row.add(cccRightLinkLicNo);
					row.add(printRunUnlimited);
					row.add(printRunLimitBox);
					row.add(PrintRunIncludeEbooks);
					row.add(ebookPrintRun);
					row.add(sizeLimitation);
					row.add(sizeGranted);
					row.add(allLanguageGranted);
					row.add(languageLimitation);
					row.add(worldSalesTerGranted);
					row.add(salesTerLimitation);
					row.add(ancDerCusRightsGranted);
					row.add(ancDerCusRightsLimitation);
					row.add(ClrAllFutureMediaTyp);
					row.add(mediaLimitation);
					row.add(clrAllFutureEditions);
					row.add(editionLimitation);
					row.add(subLicGranted);
					row.add(perComField);
					row.add(prodComField);
					row.add(compCopies);
					row.add(compCopyName);
					row.add(compCopyAddress);
					row.add(compCopyCity);
					row.add(compCopyProvince);
					row.add(compCopyPostalCode);
					row.add(compCopyCountry);
					row.add(continuedUse);
					row.add(reUsePickUpISBN);
					row.add(continuedUsePos);
					row.add(continuedUsePageNo);
					row.add(continuedUseComments);
					rowList.add(row);
					count++;
					cwRepository.updateExportAssetCount(count, cwid,userId);
					// New Enhancement(Suggested by Amanda for an asset with multiple contracts) End

				}

				model.put("data", rowList);
				model.put(ExcelViewNew.START_ROW, new Integer(3));

				LocalDateTime localDate = LocalDateTime.now();
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hhmmss_MMddyyyy");

		        String formatDateTime = localDate.format(formatter);
				String fileName = formatDateTime+"_export_"+primaryProduct.getIsbn()+".xlsm";
				model.put("primaryProduct", primaryProduct);
				model.put(ExcelViewNew.FILE_NAME, fileName);

				}catch(Exception e) {
					totalcount = 0;
					sendSuccessEmail="No";
					commonWorkService.sendAssetsExportErrorEmail(user.getEmail(), primaryProduct.getAuthorsAsString(),
							primaryProduct.getTitle(), primaryProduct.getIsbn(), cwid.toString());
					PrintWriter writer = response.getWriter(); // throws IOException
					writer.write("Error: " + e.getMessage());
					e.printStackTrace(writer);
					return null;
				}
				finally {
					log.debug("in finally------------------->");
					cwRepository.updateExportAsset("complete", cwid.intValue(), userId.intValue(), totalcount);
			      }
				model.put("sendSuccessEmail", sendSuccessEmail);
			    mv = new ModelAndView(getAssetExportExcelView(), model);
			    timer.stopTimer();
				return mv;
			}

			private String normalizeString(String returnString, String stringToAdd) {
				if(returnString.length()>0)
					returnString = returnString+","+stringToAdd;
				else
					returnString = stringToAdd;
				// TODO Auto-generated method stub
				return returnString;
			}

			// End -Added DM-534


	// ---------- GETTERS AND SETTERS ---------------------------------------
	public CommonWorkRepository getCommonWorkRepository() {
		return cwRepository;
	}

	public void setCommonWorkRepository(CommonWorkRepository commonWorkService) {
		this.cwRepository = commonWorkService;
	}

	public AssetUseIndexService getAssetUseIndexService() {
		return assetUseIndexService;
	}

	public void setAssetUseIndexService(AssetUseIndexService assetUseIndexService) {
		this.assetUseIndexService = assetUseIndexService;
	}

	public View getExcelView() {
		return excelView;
	}

	public void setExcelView(View excelView) {
		this.excelView = excelView;
	}

	public View getSummaryExcelView() {
		return summaryExcelView;
	}

	public void setSummaryExcelView(View summaryExcelView) {
		this.summaryExcelView = summaryExcelView;
	}

	public String getPermissionSummaryView() {
		return permissionSummaryView;
	}

	public void setPermissionSummaryView(String permissionSummaryView) {
		this.permissionSummaryView = permissionSummaryView;
	}

	public PdfReportView getPdfView() {
		return pdfView;
	}

	public void setPdfView(PdfReportView pdfView) {
		this.pdfView = pdfView;
	}

	public String getUserLandingViewName() {
		return userLandingViewName;
	}

	public void setUserLandingViewName(String userLandingViewName) {
		this.userLandingViewName = userLandingViewName;
	}

	public String getExcludedAssetsView() {
		return excludedAssetsView;
	}

	public void setExcludedAssetsView(String excludedAssetsView) {
		this.excludedAssetsView = excludedAssetsView;
	}

	public View getSourceSummaryExcelView() {
		return sourceSummaryExcelView;
	}

	public void setSourceSummaryExcelView(View sourceSummaryExcelView) {
		this.sourceSummaryExcelView = sourceSummaryExcelView;
	}

	public View getCommonWorkDetailExcelView() {
		return commonWorkDetailExcelView;
	}

	public void setCommonWorkDetailExcelView(View commonWorkDetailExcelView) {
		this.commonWorkDetailExcelView = commonWorkDetailExcelView;
	}

	public View getPhotocopyrightExcelView() {
		return photocopyrightExcelView;
	}

	public void setPhotocopyrightExcelView(View photocopyrightExcelView) {
		this.photocopyrightExcelView = photocopyrightExcelView;
	}

	public String getViewPhotoEditorStatusExcelView() {
		return viewPhotoEditorStatusExcelView;
	}

	public void setViewPhotoEditorStatusExcelView(String value) {
		this.viewPhotoEditorStatusExcelView = value;
	}

	public ProductRepository getProductRepository() {
		return productRepository;
	}

	public void setProductRepository(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}

	public ContractRepository getContractRepository() {
		return contractRepository;
	}

	public void setContractRepository(ContractRepository contractRepository) {
		this.contractRepository = contractRepository;
	}

	public AssetUseRepository getAssetUseRepository() {
		return assetUseRepository;
	}

	public void setAssetUseRepository(AssetUseRepository assetUseRepository) {
		this.assetUseRepository = assetUseRepository;
	}

	public UserRepository getUserRepository() {
		return userRepository;
	}

	public void setUserRepository(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public SourceRepository getSourceRepository() {
		return sourceRepository;
	}

	public void setSourceRepository(SourceRepository sourceRepository) {
		this.sourceRepository = sourceRepository;
	}

	public void setProductIndexService(ProductIndexService service) {
		this.productIndexService = service;
	}

	public ProductService getProductService() {
		return productService;
	}

	public void setProductService(ProductService productService) {
		this.productService = productService;
	}

	public String getViewDivestmentReportExcelView() {
		return viewDivestmentReportExcelView;
	}

	public void setViewDivestmentReportExcelView(String value) {
		this.viewDivestmentReportExcelView = value;
	}

	//Added for 'Report For Legal' - Start
	public View getReportLegalExcelView() {
		return reportLegalExcelView;
	}

	public void setReportLegalExcelView(View reportLegalExcelView) {
		this.reportLegalExcelView = reportLegalExcelView;
	}
	//Added for 'Report For Legal' - End

	//Added for DM-534 - Start
		public View getAssetExportExcelView() {
			return assetExportExcelView;
		}

		public void setAssetExportExcelView(View assetExportExcelView) {
			this.assetExportExcelView = assetExportExcelView;
		}


	public CommonWorkService getCommonWorkService() {
			return commonWorkService;
		}

		public void setCommonWorkService(CommonWorkService commonWorkService) {
			this.commonWorkService = commonWorkService;
		}
		//Added for DM-534 - End

		//	Start : Added for DM-1606
		public AssetRepository getAssetRepository() {
			return assetRepository;
		}

		public void setAssetRepository(AssetRepository assetRepository) {
			this.assetRepository = assetRepository;
		}
		//End : Added for DM-1606

		//	Start : Added for DM-284
		public View getNewPermissionSummaryExcelView() {
			return newPermissionSummaryExcelView;
		}

		public void setNewPermissionSummaryExcelView(View newPermissionSummaryExcelView) {
			this.newPermissionSummaryExcelView = newPermissionSummaryExcelView;
		}


		//	Start : Updated for DM-284
		private List<Object> newPermissionSummaryReport(ArrayList<AssetUseSearchResult> docs,int cwId, int userGroupId, String photoFlag, SimpleDateFormat sdf) throws PersistenceException, SQLException {
			List<Object> row ;
			// create a dual dimension List for rows/columns
			List<Object> rowList = new ArrayList<Object>();
			HashSet<Integer> assetIdSet = new HashSet<Integer>(); // Added for DM-284
			HashSet<Integer> assetUseIdSet = new HashSet<Integer>();	// Added for DM-284
			ArrayList<PermissionSummaryReportBean> reportBeanList = new ArrayList<PermissionSummaryReportBean>();
		for (int x = 0; x < docs.size(); x++) {
			AssetUseSearchResult result = docs.get(x);
			if (assetUseIdSet.contains(result.getAssetUseId())) {
				continue;
			}
			assetUseIdSet.add(result.getAssetUseId());
			String description = "";
			String creditLine = "";
			String sourceNames = "";
			String sourceRef = "";
			String componentName = "";
			String position = "";
			String mediaType = "";
			String usage = "";
			String finalPage = "";
			String permissionStatus = "";
			String editions = "";
			String medium = "";
			String territory = "";
			String languages = "";
			String totalPrintRun = "";
			String seats = "";
			String derivatives = "";
			String licenseFlag = "";
			String invNumber = "";
			String invStartDate = "";
			String invExpireDate = "";
			String invoicedDate = "";
			String estimatedCost = "";
			double finalCost = 0.00;
			String multiUsageAsset = "No";
			String gbpmCategory = "";

			description = result.getDescription();
			creditLine = result.getCreditLine();
			sourceNames = result.getSourceNames();
			sourceRef = result.getSourceRef();
			componentName = result.getComponentName();
			position = result.getPosition();
			mediaType = result.getMediaType();
			usage = result.getUsage();
			finalPage = result.getFinalPage();
			// row.add(result.getPermissionStatus());
			// Start : Added for DM-1606
			String permStatusCode = result.getPermissionStatusCode();
			if (permStatusCode.equalsIgnoreCase(PermissionStatus.FORM_SENT.getCode())) {
				boolean createdPO = assetRepository.checkForPoCreation(result.getAssetId());
				log.debug("AssetUseTableRowView() PO created " + createdPO
						+ " for asset ID " + result.getAssetId());
				// if any PO created for an asset in Purchase_order table then
				// display waiting on invoice else display form sent
				if (createdPO)
					permissionStatus = ""+ PermissionStatus.FORM_WAITING_ON_INVOICE;
				else
					permissionStatus = "" + result.getPermissionStatus();
			} else {
				permissionStatus = "" + result.getPermissionStatus();
			}
			// End : Added for DM-1606

			Map<String, String> info = assetUseRepository.loadSummaryReportInfo(cwId, userGroupId,result.getAssetUseId(), photoFlag);

			// Start: Added to implement DM-1185
			RoyaltyFreeDeal rfDeal = null;
			try {
				int rfdID = Integer.parseInt(info.get("rfdealId"));
				if (rfdID != 0) {
					rfDeal = contractRepository.loadRFDealById(rfdID);
				}
			} catch (NumberFormatException nfe) {
				// do nothing -- assumed RF deal as null
			}

			if (null != rfDeal) {
				if (null != info.get("editions") && !info.get("editions").trim().isEmpty()) {
					editions = info.get("editions");
				} else {
					editions = "Granted for this, future editions and/or entire author series";
				}
				if (null != info.get("medium") && !info.get("medium").trim().isEmpty()) {
					medium = info.get("medium");
				} else {
					medium = "All media types including future types";
				}
				if (null != info.get("territory") && !info.get("territory").trim().isEmpty()) {
					territory = info.get("territory");
				} else {
					territory = "Worldwide";
				}
				if (null != info.get("language") && !info.get("language").trim().isEmpty()) {
					languages = info.get("language");
				} else {
					languages = "All Languages";
				}
				if (rfDeal.getTotalPrintRun() == 0) {
					totalPrintRun = "Unlimited print run is granted";
				} else {
					totalPrintRun = "=" + rfDeal.getTotalPrintRun();
				}
				if (rfDeal.getSeats() == 0) {
					seats = "Unlimited";
				} else {
					seats = rfDeal.getSeats() + " Seat Limitation";
				}
				if (null != info.get("derivatives") && !info.get("derivatives").trim().isEmpty()) {
					derivatives = info.get("derivatives");
				} else {
					derivatives = "Wiley can include the asset(s) in any ancillaries, derivatives and custom works";
				}
				if (rfDeal.isLicenseFlag()) {
					licenseFlag = "Wiley can include the asset(s) when sub-licensing product";
				} else {
					licenseFlag = "Wiley cannot include the asset(s) when sub-licensing product";
				}
			} else {
				editions = info.get("editions");
				medium = info.get("medium");
				territory = info.get("territory");
				languages = info.get("language");
				totalPrintRun = info.get("printRun");
				seats = getFormatedSeatValue(info.get("seats"));// Added to implement DM-122
				derivatives = info.get("derivatives");
				licenseFlag = info.get("sublicense");
			}
			// End: Added to implement DM-1185

			Integer infoAssetId = Integer.parseInt(info.get("assetId"));

			List<Contract> clist = getContractRepository().loadListForAssetCW(infoAssetId, cwId);

			if (clist.size() > 0 && clist != null) {
				for (Contract contractInfo : clist) {
					if (invNumber!=null && invNumber.length() > 0)
						invNumber = invNumber + " , " + contractInfo.getNumber();
					else
						invNumber = contractInfo.getNumber();

					if (contractInfo.getStartDate() != null) {
						if (invStartDate.length() > 0)
							invStartDate = invStartDate + " , " + sdf.format(contractInfo.getStartDate());
						else
							invStartDate = sdf.format(contractInfo.getStartDate());
					}

					if (contractInfo.getEndDate() != null) {
						if (invStartDate.length() > 0)
							invExpireDate = invExpireDate + " , " + sdf.format(contractInfo.getEndDate());
						else
							invExpireDate = sdf.format(contractInfo.getEndDate());
					}

					if (contractInfo.getDate() != null) {
						if (invoicedDate.length() > 0)
							invoicedDate = invoicedDate + " , " + sdf.format(contractInfo.getDate());
						else
							invoicedDate = sdf.format(contractInfo.getDate());
					}
					if (!assetIdSet.contains(infoAssetId)) {
						assetIdSet.add(infoAssetId);
						BigDecimal bigDeciFinalCost = getRepository().executeSingleResultNamedQuery("Asset.finalCost",new Object[] { infoAssetId, cwId });
						finalCost = bigDeciFinalCost.doubleValue();
					}

				}
			}
			if (null == info.get("estimatedCost") || info.get("estimatedCost").equals("0.00")) {
				estimatedCost = "";
			} else {
				estimatedCost = info.get("estimatedCost");
			}
			// Condition for adding multi_usage column
			boolean isMultiUsage = assetUseRepository.isMultiUsageAsset(result.getAssetId(), cwId);
			if (isMultiUsage) {
				log.debug("asset is multi usage : " + result.getAssetId());
				multiUsageAsset = "Yes";
			}
			if (info.get("gbpmcategory") != null && info.get("gbpmcategory").length() > 0) {
				gbpmCategory = info.get("gbpmcategory");
			}

			PermissionSummaryReportBean bean = new PermissionSummaryReportBean();
			bean.setDescription(description);
			bean.setCreditLine(creditLine);
			bean.setSourceNames(sourceNames);
			bean.setSourceRef(sourceRef);
			bean.setComponentName(componentName);
			bean.setPosition(position);
			bean.setMediaType(mediaType);
			bean.setUsage(usage);
			bean.setFinalPage(finalPage);
			bean.setPermissionStatus(permissionStatus);
			bean.setEditions(editions);
			bean.setMedium(medium);
			bean.setTerritory(territory);
			bean.setLanguages(languages);
			bean.setTotalPrintRun(totalPrintRun);
			bean.setSeats(seats);
			bean.setDerivatives(derivatives);
			bean.setLicenseFlag(licenseFlag);
			bean.setInvNumber(invNumber);
			bean.setInvStartDate(invStartDate);
			bean.setInvExpireDate(invExpireDate);
			bean.setInvoicedDate(invoicedDate);
			bean.setEstimatedCost(estimatedCost);
			bean.setFinalCost(finalCost);
			bean.setMultiUsageAsset(multiUsageAsset);
			bean.setGbpmCategory(gbpmCategory);
			reportBeanList.add(bean);
		}
		Collections.sort(reportBeanList,PermissionSummaryReportBean.finalCostComparator);
		Collections.reverse(reportBeanList);
		int rowNum = 1;
		for (PermissionSummaryReportBean bean : reportBeanList) {
			row = new ArrayList<Object>();
			row.add(new Integer(rowNum++));
			row.add(bean.getDescription());
			row.add(bean.getCreditLine());
			row.add(bean.getSourceNames());
			row.add(bean.getSourceRef());
			row.add(bean.getComponentName());
			row.add(bean.getPosition());
			row.add(bean.getMediaType());
			row.add(bean.getUsage());
			row.add(bean.getFinalPage());
			row.add(bean.getPermissionStatus());
			row.add(bean.getEditions());
			row.add(bean.getMedium());
			row.add(bean.getTerritory());
			row.add(bean.getLanguages());
			row.add(bean.getTotalPrintRun());
			row.add(bean.getSeats());
			row.add(bean.getDerivatives());
			row.add(bean.getLicenseFlag());
			row.add(bean.getInvNumber());
			row.add(bean.getInvStartDate());
			row.add(bean.getInvExpireDate());
			row.add(bean.getInvoicedDate());
			row.add(bean.getEstimatedCost());
			row.add(bean.getFinalCost());
			row.add(bean.getMultiUsageAsset());
			row.add(bean.getGbpmCategory());
			rowList.add(row);
		}

		return rowList;
	}

		private List<Object> newPhotoDeptComeReport(ArrayList<AssetUseSearchResult> docs, Integer cwId,
				Integer userGroupId, String photoFlag, SimpleDateFormat sdf) throws ParseException,PersistenceException, SQLException {
			int rowNum = 1;
			List<Object> rowList = new ArrayList<Object>();
			for (int x = 0; x < docs.size(); x++) {
				AssetUseSearchResult result = docs.get(x);
				ArrayList<Object> row = new ArrayList<Object>();

				row.add(new Integer(rowNum++));
				row.add(result.getDescription());
				row.add(result.getCreditLine());
				row.add(result.getSourceNames());
				row.add(result.getSourceRef());
				row.add(result.getComponentName());
				row.add(result.getPosition());
				row.add(result.getMediaType());
				row.add(result.getUsage());
				row.add(result.getFinalPage());
				// row.add(result.getPermissionStatus());
				// Start : Added for DM-1606
				String permStatusCode = result.getPermissionStatusCode();
				if(permStatusCode.equalsIgnoreCase(PermissionStatus.FORM_SENT.getCode()))
				{
					boolean createdPO = assetRepository.checkForPoCreation(result.getAssetId());
					log.debug("AssetUseTableRowView() PO created "+createdPO+" for asset ID "+result.getAssetId());
					// if any PO created for an asset in Purchase_order table then display waiting on invoice else display form sent
					if(createdPO)
							row.add(PermissionStatus.FORM_WAITING_ON_INVOICE);
						else
							row.add(result.getPermissionStatus());
				}
				else
				{
					row.add(result.getPermissionStatus());
				}
				// End : Added for DM-1606


				//Code Change for SS Task 3
				Map<String, String> info = assetUseRepository.loadSummaryReportInfo(cwId,userGroupId, result.getAssetUseId(), photoFlag);

				//Start: Added to implement DM-1185
				RoyaltyFreeDeal rfDeal = null;
				try {
					int rfdID = Integer.parseInt(info.get("rfdealId"));
					if(rfdID != 0) {
						rfDeal = contractRepository.loadRFDealById(rfdID);
					}
				} catch (NumberFormatException nfe) {
					//do nothing -- assumed RF deal as null
				}

				if(null != rfDeal) {
					if(null != info.get("editions") && !info.get("editions").trim().isEmpty()) {
						row.add(info.get("editions"));
					} else {
						row.add("Granted for this, future editions and/or entire author series");
					}
					if(null != info.get("medium") && !info.get("medium").trim().isEmpty()) {
						row.add(info.get("medium"));
					} else {
						row.add("All media types including future types");
					}
					if(null != info.get("territory") && !info.get("territory").trim().isEmpty()) {
						row.add(info.get("territory"));
					} else {
						row.add("Worldwide");
					}
					if(null != info.get("language") && !info.get("language").trim().isEmpty()) {
						row.add(info.get("language"));
					} else {
						row.add("All Languages");
					}
					if(rfDeal.getTotalPrintRun() == 0) {
						row.add("Unlimited print run is granted");
					} else {
						row.add("="+rfDeal.getTotalPrintRun());
					}
					if(rfDeal.getSeats() == 0) {
						row.add("Unlimited");
					} else {
						row.add(rfDeal.getSeats()+" Seat Limitation");
					}
					if(null != info.get("derivatives") && !info.get("derivatives").trim().isEmpty()) {
						row.add(info.get("derivatives"));
					} else {
						row.add("Wiley can include the asset(s) in any ancillaries, derivatives and custom works");
					}
					if(rfDeal.isLicenseFlag()) {
						row.add("Wiley can include the asset(s) when sub-licensing product");
					} else {
						row.add("Wiley cannot include the asset(s) when sub-licensing product");
					}
				} else {
					row.add(info.get("editions"));
					row.add(info.get("medium"));
					row.add(info.get("territory"));
					row.add(info.get("language"));
					row.add(info.get("printRun"));
					row.add(getFormatedSeatValue(info.get("seats")));//Added to implement DM-122
					row.add(info.get("derivatives"));
					row.add(info.get("sublicense"));
				}
				//End: Added to implement DM-1185

					row.add(info.get("invoiceNumber"));
					row.add("..DateBorder");
					row.add((null != info.get("startDate") ? sdf.format(sdf.parse(info.get("startDate"))) : ""));
					row.add("..DateBorder");
					row.add((null != info.get("expirationDate") ? sdf.format(sdf.parse(info.get("expirationDate"))) : ""));
					row.add("..DateBorder");
					row.add((null != info.get("invoiceDate") ? sdf.format(sdf.parse(info.get("invoiceDate"))) : ""));
					if (null == info.get("estimatedCost") ||info.get("estimatedCost").equals("0.00")) {
						row.add("");
					} else {
						row.add(info.get("estimatedCost"));
					}

					if (null == info.get("finalCost") || info.get("finalCost").equals("0.00")) {
					//Changed here for Permission Summary Report final Cost Null values.
					//	row.add("");
						row.add("0.00");
					} else {
						row.add(info.get("finalCost"));
					}
					row.add(info.get("requestComment"));
				rowList.add(row);
			}
			return rowList;
		}
		//	End : Added for DM-284

}
