package com.wiley.permissions.web.shared.util;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.view.AbstractView;

/**
 * This is an implementation of the Spring view object that allows us to
 * basically send any file type to the browser.  You simply send the right
 * content type, file name, and data and it will take care of it.
 *
 * @author ttidwell
 */
public class GenericFileView
extends AbstractView
{
	private String fileName = null;
	private byte[] data = null;

	public GenericFileView() {

	}

	@Override
	public void renderMergedOutputModel(Map<String, Object> model,
			HttpServletRequest request, HttpServletResponse response)
	throws Exception
	{
		response.setContentLength(data.length);

		String disposition="attachment; filename=\"" + fileName + "\"";
		response.setHeader("Content-Disposition", disposition);

		// If don't do this then won't load properly in IE 8 (known IE 8 bug)
		response.setHeader("Cache-Control", "private");

		response.getOutputStream().write(data);
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}
}
