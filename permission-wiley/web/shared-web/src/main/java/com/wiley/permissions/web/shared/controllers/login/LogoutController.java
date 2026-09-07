package com.wiley.permissions.web.shared.controllers.login;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.ParameterizableViewController;

/**
 *
 * @author ttidwell
 */
public class LogoutController
extends ParameterizableViewController
{

	public LogoutController()
	{
	}

	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
	throws Exception
	{
		request.getSession().invalidate();

		return super.handleRequestInternal(request, response);
	}
}
