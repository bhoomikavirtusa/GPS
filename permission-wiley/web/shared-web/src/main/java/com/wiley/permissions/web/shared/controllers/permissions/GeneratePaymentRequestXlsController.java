package com.wiley.permissions.web.shared.controllers.permissions;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.View;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;
import com.wiley.permissions.web.shared.controllers.reports.JasperReportsXlsView;

/**
 * Generates the Payment Request Excel report
 *
 * @param Receives
 *            1 parameter : paymentRequestId
 * @return an Excel file
 *
 * @author lnagy
 */
@Controller
public class GeneratePaymentRequestXlsController
extends BaseAnnotatedController {

	private final static Log log = LogFactory.getLog(GeneratePaymentRequestXlsController.class);

	private static final String PR_ID = "prId";

	private JasperReportsXlsView xlsView = null;

	private DataSource sourceDataSource = null;

	@RequestMapping
	public View handle(HttpServletRequest request, HttpServletResponse response,
			@RequestParam(PR_ID) Integer prId)
	throws Exception {
		log.debug("handle(): entered...");

		Connection conn = null;

		try {
			conn = getSourceDataSource().getConnection();

			HashMap<String, Object> model = new HashMap<String, Object>();
			model.put("PR_ID", prId);
			model.put("SOURCE_CONNECTION", conn);

			response.setContentType("application/vnd.ms-excel");
			Properties headers = new Properties();
			// The default Content-Disposition of JasperReportsXlsView is "inline"
			// but "attachment" is better - prompts user to open or download
			//headers.put("Content-Disposition", "inline");
	        headers.put("Content-Disposition", "attachment;filename=\"paymentrequest.xls\"");
	        getXlsView().setHeaders(headers);

			// Must call render right here in the controller because
			// if return the view and wait for Spring to render it,
			// then we loose the db Connection
            //return new ModelAndView(getXlsView(), model);
			getXlsView().render(model, request, response);

		} finally {
			if (null != conn) conn.close();
		}

		return getXlsView();
	}

	public JasperReportsXlsView getXlsView() {
		return xlsView;
	}

	public void setXlsView(JasperReportsXlsView xlsView) {
		this.xlsView = xlsView;
	}

	public DataSource getSourceDataSource() {
		return sourceDataSource;
	}

	public void setSourceDataSource(DataSource sourceDataSource) {
		this.sourceDataSource = sourceDataSource;
	}
}
