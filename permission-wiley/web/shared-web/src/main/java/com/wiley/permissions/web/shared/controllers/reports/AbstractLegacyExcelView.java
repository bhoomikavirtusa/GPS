package com.wiley.permissions.web.shared.controllers.reports;

import java.io.OutputStream;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.web.servlet.view.AbstractView;

abstract class AbstractLegacyExcelView extends AbstractView {

	private String url;

	protected AbstractLegacyExcelView() {
		setContentType("application/vnd.ms-excel");
	}

	protected String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	@Override
	protected void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		HSSFWorkbook workbook = getTemplateSource(getUrl(), request);
		buildExcelDocument(model, workbook, request, response);
		response.setContentType(getContentType());
		try (OutputStream output = response.getOutputStream()) {
			workbook.write(output);
			output.flush();
		}
	}

	protected HSSFWorkbook getTemplateSource(String url, HttpServletRequest request) throws Exception {
		return new HSSFWorkbook();
	}

	protected abstract void buildExcelDocument(Map<String, Object> model, HSSFWorkbook workbook,
			HttpServletRequest request, HttpServletResponse response) throws Exception;

	protected HSSFCell getCell(HSSFSheet sheet, int row, int column) {
		return sheet.getRow(row) == null ? sheet.createRow(row).createCell(column)
				: sheet.getRow(row).createCell(column);
	}

	protected void setText(HSSFCell cell, String text) {
		cell.setCellValue(text);
	}
}