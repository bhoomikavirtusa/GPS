package com.wiley.permissions.web.internal.controllers.reports;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.AttributedString;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.queryparser.classic.ParseException;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.PieSectionLabelGenerator;
import org.jfree.chart.plot.MultiplePiePlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;
import org.jfree.util.TableOrder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.View;

import com.wiley.permissions.domain.message.pe.ProductSearchResult;
import com.wiley.permissions.domain.persistence.permissions.BusinessUnit;
import com.wiley.permissions.domain.persistence.permissions.Role;
import com.wiley.permissions.repositories.ProductRepository;
import com.wiley.permissions.services.ComplianceForm;
import com.wiley.permissions.services.ProductIndexService;
import com.wiley.permissions.services.ProductIndexService.ComplianceCount;
import com.wiley.permissions.web.shared.PermUserContext;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;
import com.wiley.permissions.web.shared.controllers.reports.ExcelView;

/**
 *
 * @author smarkoff
 */
@Controller
/*@RequestMapping("/report/compliance/")*/
@RequestMapping
public class ComplianceController extends BaseAnnotatedController {

	private static final Log log = LogFactory.getLog(ComplianceController.class);

	private static final int CHART_WIDTH = 800;
	private static final int CHART_HEIGHT = 300;
	private static final String PRE_PRODUCTION_LABEL = "Pre-production (P, E)";
	private static final String IN_PRODUCTION_LABEL = "In Production/Active (I, N)";
	protected static final String FORM_MODEL_NAME = "complianceForm";

	private ProductIndexService productIndexService;
	private ProductRepository productRepository;
	private View excelView;
	private String internalUrl;


	/**
	 * This is just to fix problem where if have not yet created a chart
	 * and try to stop Tomcat via Ctrl-C on my local Windows machine,
	 * Tomcat won't shut down properly because JChart has some background
	 * thread running or something. This is a weird problem.
	 * This constructor should not be necessary at all.
	 */
	public ComplianceController() {
		PieDataset dataset = createDemoDataset();
		createPieChart(dataset, "title");
	}

	@RequestMapping(value="/report/compliance/form", method = {RequestMethod.GET, RequestMethod.POST})
	public String form(Model model, HttpServletRequest request) {
		log.debug("form(): entered...");

		model.addAttribute(FORM_MODEL_NAME, new ComplianceForm());

		boolean isSuper = request.isUserInRole(Role.SUPER.getCode());
		model.addAttribute("isSuper", isSuper);

		String dateFormat = PermUserContext.getPickerDateFormat(request);
		model.addAttribute("dateFormat",dateFormat);

		return "pages.report.compliance.form";
	}

	@RequestMapping(value="/report/compliance/list", method = {RequestMethod.GET, RequestMethod.POST})
	public String list(Model model, @ModelAttribute("complianceForm") ComplianceForm form) throws ParseException, IOException {
		log.debug("list(): entered...");
		ComplianceCount count = productIndexService.complianceCount(form);

		model.addAttribute("count", count);
		model.addAttribute("queryString", form.getQueryString());

		return "pages.report.compliance.list";
	}

	@RequestMapping(value="/report/compliance/chart", method = {RequestMethod.GET, RequestMethod.POST})
	public void chart(HttpServletResponse response,
			@ModelAttribute("complianceForm") ComplianceForm form) throws IOException, ParseException
	{
		log.debug("chart(): entered...");

		form.setInProduction(false);
		ComplianceCount preProductionCount = productIndexService.complianceCount(form);
		form.setInProduction(true);
		ComplianceCount inProductionCount = productIndexService.complianceCount(form);

		DefaultCategoryDataset dataset = new DefaultCategoryDataset();
		dataset.setValue(preProductionCount.getNotStartedCount(), PRE_PRODUCTION_LABEL, "Not started");
		dataset.setValue(preProductionCount.getInProcessCount(), PRE_PRODUCTION_LABEL, "In process");
		dataset.setValue(preProductionCount.getCompleteCount(), PRE_PRODUCTION_LABEL, "Complete");
		dataset.setValue(preProductionCount.getCompleteNo3rdPartyCount(), PRE_PRODUCTION_LABEL, "Complete: No Third-Party Assets");
		dataset.setValue(preProductionCount.getProblemCount(), PRE_PRODUCTION_LABEL, "Problem");

		dataset.setValue(inProductionCount.getNotStartedCount(), IN_PRODUCTION_LABEL, "Not started");
		dataset.setValue(inProductionCount.getInProcessCount(), IN_PRODUCTION_LABEL, "In process");
		dataset.setValue(inProductionCount.getCompleteCount(), IN_PRODUCTION_LABEL, "Complete");
		dataset.setValue(inProductionCount.getCompleteNo3rdPartyCount(), IN_PRODUCTION_LABEL, "Complete: No Third-Party Assets");
		dataset.setValue(inProductionCount.getProblemCount(), IN_PRODUCTION_LABEL, "Problem");

		String title = null;
		JFreeChart chart = createMultiplePieChart(dataset, title);

		createImage(response, chart);
	}

	@RequestMapping(value="/report/compliance/excel", method = {RequestMethod.GET, RequestMethod.POST})
	public View excel(Model model,
			@ModelAttribute("complianceForm") ComplianceForm form,
			@RequestParam("complianceStatus") String complianceStatus) throws ParseException, IOException {
		log.debug("excel(): entered...");

		List<ProductSearchResult> resultList = productIndexService.complianceList(form, complianceStatus);
		log.debug("excel(): resultList.size() = " + resultList.size());

        // create a dual dimension List for rows/columns
		List<Object> rowList = new ArrayList<Object>();

		// create criteria section
		List<Object> row = new ArrayList<Object>();
		row.add("..Subhead");
		row.add("Compliance Report");
		rowList.add(row);

		// add a blank line
		rowList.add(new ArrayList<Object>());

		row = new ArrayList<Object>();
		row.add("..Subhead");
		row.add("Report Criteria");
		rowList.add(row);

		if (form.getToDateYear() >= form.getFromDateYear() && form.getToDateYear() > 0) {
			row = new ArrayList<Object>();
			if (form.getDateCriterion().equals("copyrightYear")) {
				row.add(form.getDateCriterion() + ": " + form.getFromDateYear() + " - " + form.getToDateYear());
			}
			else {
				row.add(form.getDateCriterion() + ": " + form.getFromDate() + " - " + form.getToDate());
			}
			rowList.add(row);
		}

		row = new ArrayList<Object>();;
		row.add("Division: " + BusinessUnit.forCode(form.getBusinessUnitCode()).getName());
		rowList.add(row);

		if (StringUtils.isNotBlank(form.getProductLineCode())) {
			row = new ArrayList<Object>();
			row.add("Product Line: " + form.getProductLineCode());
			rowList.add(row);
		}

		if (StringUtils.isNotBlank(form.getEditorCode())) {
			row = new ArrayList<Object>();
			row.add("Editor code: " + form.getEditorCode());
			rowList.add(row);
		}

		row = new ArrayList<Object>();
		row.add(form.isInProduction() ? IN_PRODUCTION_LABEL : PRE_PRODUCTION_LABEL);
		rowList.add(row);

		row = new ArrayList<Object>();
		if(null != complianceStatus && complianceStatus.equals("completeNo3rdParty")) {
			row.add("Compliance status: Complete: No Third-Party Assets");
		} else {
			row.add("Compliance status: " + complianceStatus);
		}

		rowList.add(row);

		// add a blank line
		rowList.add(new ArrayList<Object>());

		// set header
		row = new ArrayList<Object>();
		row.add("..Subhead");  // need to specify this because not on the normal header line
		row.add("Product Line");
		row.add("Editor");
		row.add("E. Name");
		row.add("ISBN10");
		row.add("ISBN13");
		row.add("Author(s)");
		row.add("Title");
		row.add("Copyright Year");
		row.add("Transmittal Date");
		row.add("CRD");
		row.add("Last Updated");  // (not Product.lastUpdatedDate but CW.lastWorkedOnDate)
		row.add("Proj'd assets");  // (photoIllusTotalCount)
		row.add("Total Assets");  // not canceled
		row.add("Cover Assets");  // ditto
		row.add("Other Assets");  // ditto
		row.add("Status Not OK");
		row.add("%complete");
		row.add("URL");

		rowList.add(row);

		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

		for (ProductSearchResult result : resultList) {
			row = new ArrayList<Object>();

			row.add(result.getProductLineCode());
			row.add(result.getEditor());
			row.add(result.getEditorName());
			row.add(result.getIsbn10());
			row.add(result.getIsbn13());
			row.add(result.getAuthorsAsString());
			row.add(result.getTitle());
			row.add(result.getCopyrightYear());
			Date transmittalDate = result.getTransmittalDate();
			row.add(transmittalDate == null ? "" : dateFormat.format(transmittalDate));
			Date crd = result.getConsolidatedReleaseDate();
			row.add(crd == null ? "" : dateFormat.format(crd));
			Date lud = result.getLastWorkedOnDate();  // not lastUpdatedDate but lastWorkedOnDate
			row.add(lud == null ? "" : dateFormat.format(lud));
			row.add(result.getPhotoIllusTotalCount());
			row.add(result.getAuCountNotCanceled());
			row.add(result.getCoverCountNotCanceled());
			// all count Integer's should be non-null for the products we are dealing with here
			// but check for null just in case
			Integer nonCoverCount = null;
			if (result.getAuCountNotCanceled() != null && result.getCoverCountNotCanceled() != null) {
				nonCoverCount = result.getAuCountNotCanceled() - result.getCoverCountNotCanceled();
			}
			row.add(nonCoverCount);
			row.add(result.getStatusNotOkCount());
			Integer percentComplete = null;
			// auCountNotCanceled and statusNotOkCount should be non-null but check anyway
			// photoIllusTotalCount may definitely be null
			if (result.getAuCountNotCanceled() != null && result.getStatusNotOkCount() != null
					&& result.getPhotoIllusTotalCount() != null && result.getAuCountNotCanceled() > 0) {
				int denom = result.getAuCountNotCanceled() - result.getStatusNotOkCount();
				if (result.getPhotoIllusTotalCount() > 0) {
					percentComplete = (100 * denom) / result.getPhotoIllusTotalCount();
				}
			}
			row.add(percentComplete);
			row.add(internalUrl + "/internal-web/sapp/cwlanding/scroll?cwId=" + result.getCwId());

			rowList.add(row);
		}

		model.addAttribute("data", rowList);
		model.addAttribute(ExcelView.FIRST_ROW_IS_HEADER, Boolean.TRUE);
		model.addAttribute(ExcelView.AUTO_SIZE_COLS, 17);
		model.addAttribute(ExcelView.AUTO_SIZE_MAX_WIDTH, 80);
		model.addAttribute(ExcelView.AUTO_SIZE_FIRST_ROW, 7);
		model.addAttribute(ExcelView.FILE_NAME, "compliance.xls");

		return excelView;
	}

	@RequestMapping(value="/report/compliance/getEditorCodeSelect", method = {RequestMethod.GET, RequestMethod.POST})
	public void getEditorCodeSelect(HttpServletResponse response,
		@ModelAttribute("complianceForm") ComplianceForm form) throws IOException {

		log.debug("getEditorCodeSelect(): entered...");

		String buCode = form.getBusinessUnitCode();
		String productLineCode = form.getProductLineCode();

		List<String> codes = productRepository.getEditorCodesForBUAndProductLine(buCode, productLineCode);

		StringBuilder sb = new StringBuilder();
		addOption(sb, "");
		for (String code : codes) {
			addOption(sb, code);
		}
		htmlToResponse(response, sb.toString());
	}

	private void addOption(StringBuilder sb, String value) {
		sb.append("<option value=\"");
		sb.append(value);
		sb.append("\">");
		sb.append(value);
		sb.append("</option>\r\n");
	}

	private void htmlToResponse(HttpServletResponse response, String s) throws IOException {
		response.setContentType("text/html");
		//response.setCharacterEncoding("utf-8");
		// Only ok to set contentLength because we know that there are no double-byte chars in editor codes
        response.setContentLength(s.length());

        PrintWriter writer = response.getWriter();  // throws IOException
        writer.print(s);
	}

    private JFreeChart createPieChart(PieDataset dataset, String title) {
        JFreeChart chart = ChartFactory.createPieChart(
        	title,  // chart title
            dataset,             // data
            true,                // include legend
            true,
            false);
        //TextTitle textTitle = chart.getTitle();
        //textTitle.setToolTipText("A title tooltip!");

        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setLabelFont(new Font("SansSerif", Font.PLAIN, 12));
        plot.setNoDataMessage("0 records match your criteria");
        plot.setLabelGap(0.02);
        // problem with setIgnoreZeroValues() is that when it ignores a value it
        // doesn't "use up" a color - so if you have 2 pie charts with the same categories
        // the colors can be mismatched if there are some zero values.
        // - fixed this by using MuliplePieChart with custom LabelGenerator in createChart2() below
        plot.setIgnoreZeroValues(true);

        return chart;
    }

    private JFreeChart createMultiplePieChart(CategoryDataset dataset, String title) {
        JFreeChart chart = ChartFactory.createMultiplePieChart(
            title,
            dataset,             // data
            TableOrder.BY_ROW,   // dataset.addValue(value, row, column)
            true,                // include legend
            true,
            false);
        //TextTitle textTitle = chart.getTitle();
        //textTitle.setToolTipText("A title tooltip!");

        MultiplePiePlot plot = (MultiplePiePlot) chart.getPlot();
        PiePlot p = (PiePlot) plot.getPieChart().getPlot();
        p.setLabelFont(new Font("SansSerif", Font.PLAIN, 12));
        p.setNoDataMessage("No data available");
        p.setLabelGap(0.02);

        // This doesn't prevent the label from showing up, but the LabelGenerator below does
        p.setIgnoreZeroValues(true);

        p.setLabelGenerator(new PieSectionLabelGenerator() {
			@Override
			public AttributedString generateAttributedSectionLabel(
					PieDataset dataset, Comparable key) {
				Double value = (Double) dataset.getValue(key);
				if (value == null || value == 0) {
					return null;
				}
				else return null;  // supposedly JFreeChart doesn't actually use this method
			}

			@Override
			public String generateSectionLabel(PieDataset dataset, Comparable key) {
				Double value = (Double) dataset.getValue(key);
				if (value == null || value == 0) {
					return null;
				}
				else return String.valueOf(key);
			}
        });

        // set custom colors instead of taking default
        p.setSectionPaint("Not started", new Color(255, 85, 85));  // dull red, same as default
        p.setSectionPaint("In process", new Color(85, 85, 255));  // blue, same as default
        //p.setSectionPaint("In process", new Color(255, 220, 85));  // sort of dull yellow
        p.setSectionPaint("Complete", new Color(85, 255, 85));  // green, same as default
        p.setSectionPaint("Complete: No Third-Party Assets", new Color(255, 0, 0));
        p.setSectionPaint("Problem", new Color(238, 148, 36));  // light orange (default is yellow)

        return chart;
    }

    @RequestMapping(value="/report/compliance/pie", method = {RequestMethod.GET, RequestMethod.POST})
    public void pie(HttpServletResponse response) throws IOException {
		PieDataset dataset = createDemoDataset();
		JFreeChart chart = createPieChart(dataset, "Pie Chart Demo");

		createImage(response, chart);
	}

    @RequestMapping(value="/report/compliance/pie2", method = RequestMethod.POST)
	public void pie2(HttpServletResponse response) throws IOException {
		CategoryDataset dataset = createDemo2Dataset();
		JFreeChart chart = createMultiplePieChart(dataset, "Multiple Pie Chart Demo");

		createImage(response, chart);
	}

	private void createImage(HttpServletResponse response, JFreeChart chart) throws IOException {
        // Write to intermediate stream first so we can determine size.
        // (Although for a small image we don't have to set the size
        // and Tomcat will because it will fit inside the 8K buffer.)
		ByteArrayOutputStream tempOut = new ByteArrayOutputStream();
		ChartUtilities.writeChartAsPNG(tempOut, chart, CHART_WIDTH, CHART_HEIGHT);  // throws IOException
		response.setContentType("image/png");
        response.setContentLength(tempOut.size());

        OutputStream out = response.getOutputStream();  // throws IOException
        tempOut.writeTo(out);
	}

    private PieDataset createDemoDataset() {
        DefaultPieDataset dataset = new DefaultPieDataset();
        dataset.setValue("One", new Double(0));
        dataset.setValue("Two", new Double(10.0));
        dataset.setValue("Three", new Double(27.5));
        dataset.setValue("Four", new Double(17.5));
        dataset.setValue("Five", new Double(11.0));
        dataset.setValue("Six", new Double(19.4));
        return dataset;
    }

    private CategoryDataset createDemo2Dataset() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        dataset.addValue(43.2, "Pie 0", "Cat 0");
        dataset.addValue(10.0, "Pie 0", "Cat 1");
        dataset.addValue(27.5, "Pie 0", "Cat 2");

        dataset.addValue(0, "Pie 1", "Cat 0");
        dataset.addValue(11.0, "Pie 1", "Cat 1");
        dataset.addValue(19.4, "Pie 1", "Cat 2");

        return dataset;
    }

	/**
	 * Generates a place holder png image.
	 */
    @RequestMapping(value="/report/compliance/placeHolderImage", method = {RequestMethod.GET, RequestMethod.POST})
    public void placeHolderImage(HttpServletResponse response) throws IOException {
		log.debug("placeHolderImage(): entered...");
        // Note must set all response headers before write to response
        // OutputStream.  But we will do both at very end.

        BufferedImage image = new BufferedImage(CHART_WIDTH, CHART_HEIGHT,
            BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        g.setColor(new Color(192, 192, 192));
        g.fillRect(0, 0, CHART_WIDTH, CHART_HEIGHT);
        g.setColor(Color.BLACK);
        g.drawRect(0, 0, CHART_WIDTH - 1, CHART_HEIGHT - 1);

        g.dispose();

        // Convert image to png and send.
        // Write to intermediate stream first so we can determine size.
        // (Although for a small image we don't have to set the size
        // and Tomcat will because it will fit inside the 8K buffer.)

        ByteArrayOutputStream tempOut = new ByteArrayOutputStream();
        @SuppressWarnings("unused")
        boolean ok = ImageIO.write(image, "png", tempOut);  // throws IOException

        response.setContentType("image/png");
        response.setContentLength(tempOut.size());

        OutputStream out = response.getOutputStream();  // throws IOException
        tempOut.writeTo(out);
    }


	public ProductIndexService getProductIndexService() {
		return productIndexService;
	}

	public void setProductIndexService(ProductIndexService service) {
		productIndexService = service;
	}

	public void setProductRepository(ProductRepository repo) {
		productRepository = repo;
	}

	public void setExcelView(View excelView) {
		this.excelView = excelView;
	}

	public View getComplianceExcelView() {
		return excelView;
	}

	public void setInternalUrl(String url) {
		this.internalUrl = url;
	}
}
