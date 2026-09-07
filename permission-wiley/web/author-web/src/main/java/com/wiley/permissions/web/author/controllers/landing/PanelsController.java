package com.wiley.permissions.web.author.controllers.landing;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.wiley.permissions.domain.persistence.permissions.AuthorCommonWorkPK;
import com.wiley.permissions.domain.persistence.permissions.AuthorToCommonWork;
import com.wiley.permissions.repositories.UserRepository;
import com.wiley.permissions.services.ProductService;
import com.wiley.permissions.services.view.ProductSummaryView;
import com.wiley.permissions.web.shared.PermUserContext;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;

@Controller
@RequestMapping("/panels")
public class PanelsController extends BaseAnnotatedController{
	private static final Log log = LogFactory.getLog(PanelsController.class);

	private UserRepository userRepository;
	private ProductService productService;

	@RequestMapping("/summaryPanel")
	public ModelAndView summaryPanel(HttpServletRequest request,
			@RequestParam("cwId") int cwId)
	throws Exception
	{
		log.debug("summaryPanel(): entered...");

		ProductSummaryView view = productService.loadPrimaryProductView(cwId);  // throws various exceptions
		int authorId = PermUserContext.getCurrentUserId(request);  // throws Exception

		AuthorToCommonWork a2cw = userRepository.find(AuthorToCommonWork.class, new AuthorCommonWorkPK (authorId, cwId));		
		
		ModelAndView mv = new ModelAndView("pages.landing.summaryPanel");
		mv.addObject("view", view);
		mv.addObject("cwId", cwId);
		mv.addObject("dueDate", (null == a2cw) ? null : a2cw.getDueDate());
		mv.addObject("permissionComplete", (null == a2cw) ? false : a2cw.isPermissionComplete());
		return mv;
	}

	public UserRepository getUserRepository()
	{
		return userRepository;
	}

	public void setUserRepository(UserRepository userRepository)
	{
		this.userRepository = userRepository;
	}

	public ProductService getProductService()
	{
		return productService;
	}

	public void setProductService(ProductService productService)
	{
		this.productService = productService;
	}
}
