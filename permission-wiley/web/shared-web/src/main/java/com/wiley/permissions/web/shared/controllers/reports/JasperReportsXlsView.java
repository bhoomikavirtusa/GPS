package com.wiley.permissions.web.shared.controllers.reports;

import java.io.InputStream;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.engine.export.JRXlsExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;

import org.springframework.web.servlet.view.AbstractView;

public class JasperReportsXlsView extends AbstractView {

	private String url;
	private DataSource jdbcDataSource;
	private Properties headers;
	private Map<String, String> subReportUrls;

	public JasperReportsXlsView() {
		setContentType("application/vnd.ms-excel");
	}

	@Override
	protected void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		JasperReport report;
		try (InputStream input = request.getSession().getServletContext().getResourceAsStream(url)) {
			if (input == null) {
				throw new IllegalStateException("Jasper report resource not found: " + url);
			}
			report = (JasperReport) JRLoader.loadObject(input);
		}
		Map<String, Object> parameters = new HashMap<String, Object>(model);
		if (subReportUrls != null) {
			for (Map.Entry<String, String> entry : subReportUrls.entrySet()) {
				parameters.put(entry.getKey(), loadReport(request, entry.getValue()));
			}
		}
		Connection connection = (Connection) parameters.get("SOURCE_CONNECTION");
		boolean closeConnection = false;
		if (connection == null) {
			if (jdbcDataSource == null) {
				throw new IllegalStateException("No JDBC connection or data source configured for Jasper XLS report");
			}
			connection = jdbcDataSource.getConnection();
			closeConnection = true;
		}
		try {
			JasperPrint print = JasperFillManager.fillReport(report, parameters, connection);
			applyHeaders(response);
			JRXlsExporter exporter = new JRXlsExporter();
			exporter.setExporterInput(new SimpleExporterInput(print));
			exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(response.getOutputStream()));
			exporter.exportReport();
		} finally {
			if (closeConnection) {
				connection.close();
			}
		}
	}

	private JasperReport loadReport(HttpServletRequest request, String resourcePath) throws Exception {
		InputStream input = request.getSession().getServletContext().getResourceAsStream(resourcePath);
		if (input == null) {
			throw new IllegalStateException("Jasper report resource not found: " + resourcePath);
		}
		try (InputStream reportInput = input) {
			return (JasperReport) JRLoader.loadObject(reportInput);
		}
	}

	private void applyHeaders(HttpServletResponse response) {
		if (headers != null) {
			for (String name : headers.stringPropertyNames()) {
				response.setHeader(name, headers.getProperty(name));
			}
		}
	}

	public String getUrl() { return url; }
	public void setUrl(String url) { this.url = url; }
	public DataSource getJdbcDataSource() { return jdbcDataSource; }
	public void setJdbcDataSource(DataSource jdbcDataSource) { this.jdbcDataSource = jdbcDataSource; }
	public Properties getHeaders() { return headers; }
	public void setHeaders(Properties headers) { this.headers = headers; }
	public Map<String, String> getSubReportUrls() { return subReportUrls; }
	public void setSubReportUrls(Map<String, String> subReportUrls) { this.subReportUrls = subReportUrls; }
}