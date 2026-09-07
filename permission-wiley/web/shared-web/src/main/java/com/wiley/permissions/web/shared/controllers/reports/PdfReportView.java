package com.wiley.permissions.web.shared.controllers.reports;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import net.sf.jasperreports.engine.JRExporter;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.export.JRPdfExporterParameter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.ui.jasperreports.JasperReportsUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.view.jasperreports.JasperReportsPdfView;

public class PdfReportView extends JasperReportsPdfView {

	private static final Log log = LogFactory.getLog(PdfReportView.class);

	/**
	 * Perform rendering for a single Jasper Reports exporter, that is,
	 * for a pre-defined output format.
	 *
	 * We override this method because we want to be able to pass the outputstream
	 * and handle special parameters ("AvoidResponse" and "file").
	 */
	@Override
	protected void renderReport(JasperPrint populatedReport, Map<String, Object> model, HttpServletResponse response)
			throws Exception
	{
		// Prepare report for rendering.
		JRExporter exporter = createExporter();
		exporter.setParameter( JRPdfExporterParameter.METADATA_AUTHOR, "Wiley & Sons" );
		exporter.setParameter( JRPdfExporterParameter.METADATA_CREATOR, "Wiley & Sons Permissions System." );
		exporter.setParameter( JRPdfExporterParameter.IS_TAGGED, true );
		exporter.setParameter( JRPdfExporterParameter.TAG_LANGUAGE, "English" );

		// Set exporter parameters - overriding with values from the Model.
		Map<JRExporterParameter,Object> mergedExporterParameters = mergeExporterParameters(model);
		if (!CollectionUtils.isEmpty(mergedExporterParameters)) {
			exporter.setParameters(mergedExporterParameters);
		}

		JasperReportsUtils.render(exporter, populatedReport, response.getOutputStream());
		    // throws JRException
		response.getOutputStream().flush();
	}

	/**
	 * smarkoff: In Spring 2.x, AbstractJasperReportsSingleFormatView used to have this method
	 * but it's gone in Spring 3.0.
	 * Actually the 2.5 code was Map model aka Map<Object, Object> model which makes sense
	 * but renderReport (above) takes Map<String, Object model, so I change the signature on
	 * this method to be the same thing - which means that key will never be instance of
	 * JRExportedParmeter - which means only the first part of this method might do anything.
	 */
	/**
	 * Merges the configured JRExporterParameters with any specified in the supplied model data.
	 * JRExporterParameters in the model override those specified in the configuration.
	 * @see #setExporterParameters(java.util.Map)
	 */
	protected Map<JRExporterParameter, Object> mergeExporterParameters(Map<String, Object> model) {
		Map<JRExporterParameter, Object> mergedParameters = new HashMap<JRExporterParameter, Object>();
		Map<JRExporterParameter, Object> convertedExporterParameters = getConvertedExporterParameters();
		if (!CollectionUtils.isEmpty(convertedExporterParameters)) {
			mergedParameters.putAll(convertedExporterParameters);
		}
		for (Iterator<String> it = model.keySet().iterator(); it.hasNext();) {
			Object key = it.next();
			if (key instanceof JRExporterParameter) {
				Object value = model.get(key);
				Object convertedValue = convertParameterValue((JRExporterParameter) key, value);
				mergedParameters.put((JRExporterParameter)key, convertedValue);
			}
		}
		return mergedParameters;
	}
}
