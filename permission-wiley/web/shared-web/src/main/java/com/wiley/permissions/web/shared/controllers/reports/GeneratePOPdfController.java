package com.wiley.permissions.web.shared.controllers.reports;

import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import net.sf.jasperreports.engine.JRParameter;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.wiley.permissions.domain.persistence.permissions.EnumLanguage;
import com.wiley.permissions.domain.persistence.permissions.PurchaseOrder;
import com.wiley.permissions.repositories.PurchaseOrderRepository;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;

/**
 * Generates the Purchase Order PDF report
 *
 * @param Receives
 *            2 parameter : productId and poId
 * @return Returns a PDF
 *
 * @author lnagy
 */
@Controller
public class GeneratePOPdfController extends BaseAnnotatedController {

	private final static Log log = LogFactory.getLog(GeneratePOPdfController.class);

	private static final String PRODUCT_ID = "productId";
	private static final String CW_ID = "cwId";
	private static final String PO_ID = "poId";
	private static final String DATE_FORMAT = "MM/dd/yyyy";

	private PdfReportView pdfView = null;
	private PurchaseOrderRepository poRepository;

	private DataSource sourceDataSource = null;

	@GetMapping
	public void handle(HttpServletRequest request,
			HttpServletResponse response,
			@RequestParam(PRODUCT_ID) Integer productId,
			@RequestParam(CW_ID) Integer cwId,
			@RequestParam(PO_ID) String poId,
			@RequestParam(value="download", required=false) boolean download)
	throws Exception
	{
		log.debug("handle(): productId = " + productId + ", cwId = " + cwId
				+ ", poId = " + poId + ", download = " + download);
		Connection conn = null;

		try {
			conn = getSourceDataSource().getConnection();

			HashMap<String, Object> model = new HashMap<String, Object>();
			model.put("PRODUCT_ID", productId);
			model.put("CW_ID", cwId);
			// maybe add some NullPointerException tests here
		    String[] poIds = StringUtils.split(poId, ",");
			model.put("POIDS", Arrays.asList(poIds));
			model.put("SOURCE_CONNECTION", conn);

			response.setContentType ("application/pdf");
			Properties headers = new Properties();

			String name = "po.pdf";
		    SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT);
			PurchaseOrder po = poRepository.lazyLoad (PurchaseOrder.class, new Integer (poIds[0]), new String[] {"source"});
			if (po.isPermissionRequest()) {
				name = "Permission request " + po.getSource().getName() + " " + format.format(po.getDate()) + ".pdf";
			} else {
				name = "Permission PO " + po.getSource().getName() + " " + format.format(po.getDate()) + ".pdf";
			}

			if (download) {
	            headers.put("Content-Disposition", "attachment;filename=\"" + name + "\"");
			} else {
	            headers.put("Content-Disposition", "inline;filename=\"" + name + "\"");
			}
			getPdfView().setHeaders(headers);

			Locale locale = EnumLanguage.getLocale(po.getLanguageCode());
			model.put (JRParameter.REPORT_LOCALE, locale);

			// Must call render right here in the controller because
			// if return the view and wait for Spring to render it,
			// then we loose the db Connection
            // return new ModelAndView(getPdfView(), model);
			getPdfView().render(model, request, response);
		}
		finally {
			if (null != conn) conn.close();
		}
	}

	public PdfReportView getPdfView() {
		return pdfView;
	}

	public void setPdfView(PdfReportView pdfView) {
		this.pdfView = pdfView;
	}

	public DataSource getSourceDataSource() {
		return sourceDataSource;
	}

	public void setSourceDataSource(DataSource sourceDataSource) {
		this.sourceDataSource = sourceDataSource;
	}

	public PurchaseOrderRepository getPoRepository() {
		return poRepository;
	}

	public void setPoRepository(PurchaseOrderRepository poRepository) {
		this.poRepository = poRepository;
	}
}
