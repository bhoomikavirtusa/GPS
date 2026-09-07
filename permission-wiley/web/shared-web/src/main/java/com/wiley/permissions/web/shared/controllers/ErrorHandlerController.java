package com.wiley.permissions.web.shared.controllers;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.ParameterizableViewController;

/**
 *
 * @author ttidwell
 */
public class ErrorHandlerController
extends ParameterizableViewController
{
	@SuppressWarnings("unused")
	private final static Log log = LogFactory.getLog(ErrorHandlerController.class);

	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
	throws Exception
	{
		ModelAndView output = super.handleRequestInternal(request, response);

		String viewName = getViewName();

		Integer test = (Integer) request.getAttribute("javax.servlet.error.status_code");

		if (test != null)
		{
			viewName="errors." + test;
		}

		output.setViewName(viewName);


		return output;
	}
}
