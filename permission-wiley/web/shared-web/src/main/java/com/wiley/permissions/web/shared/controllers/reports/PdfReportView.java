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
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.export.JRPdfExporterParameter;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;

import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.view.AbstractView;

public class PdfReportView extends AbstractView {

    private String url;
    private DataSource jdbcDataSource;
    private Map<String, String> subReportUrls;
    private Properties headers;

    public PdfReportView() {
        setContentType("application/pdf");
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
        if (!CollectionUtils.isEmpty(subReportUrls)) {
            for (Map.Entry<String, String> entry : subReportUrls.entrySet()) {
                parameters.put(entry.getKey(), loadReport(request, entry.getValue()));
            }
        }
        try (Connection connection = jdbcDataSource.getConnection()) {
            JasperPrint print = JasperFillManager.fillReport(report, parameters, connection);
            JRPdfExporter exporter = new JRPdfExporter();
            exporter.setParameter(JRPdfExporterParameter.METADATA_AUTHOR, "Wiley & Sons");
            exporter.setParameter(JRPdfExporterParameter.METADATA_CREATOR, "Wiley & Sons Permissions System.");
            exporter.setParameter(JRPdfExporterParameter.IS_TAGGED, true);
            exporter.setParameter(JRPdfExporterParameter.TAG_LANGUAGE, "English");
            applyHeaders(response);
            exporter.setExporterInput(new SimpleExporterInput(print));
            exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(response.getOutputStream()));
            exporter.exportReport();
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

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public DataSource getJdbcDataSource() { return jdbcDataSource; }
    public void setJdbcDataSource(DataSource jdbcDataSource) { this.jdbcDataSource = jdbcDataSource; }
    public Map<String, String> getSubReportUrls() { return subReportUrls; }
    public void setSubReportUrls(Map<String, String> subReportUrls) { this.subReportUrls = subReportUrls; }
    public Properties getHeaders() { return headers; }
    public void setHeaders(Properties headers) { this.headers = headers; }

    private void applyHeaders(HttpServletResponse response) {
        if (headers != null) {
            for (String name : headers.stringPropertyNames()) {
                response.setHeader(name, headers.getProperty(name));
            }
        }
    }
}
