package com.wiley.permissions.web.author.controllers.landing;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.wiley.permissions.domain.persistence.permissions.AuthorCommonWorkPK;
import com.wiley.permissions.domain.persistence.permissions.AuthorToCommonWork;
import com.wiley.permissions.domain.persistence.permissions.Product;
import com.wiley.permissions.domain.persistence.permissions.SystemNotification;
import com.wiley.permissions.repositories.SystemNotificationRepository;
import com.wiley.permissions.repositories.UserRepository;
import com.wiley.permissions.services.ProductService;
import com.wiley.permissions.web.shared.PermUserContext;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;

@Controller
@RequestMapping("/product/userLanding")
public class UserLandingController extends BaseAnnotatedController {
	private static final Log log = LogFactory.getLog(UserLandingController.class);

	private UserRepository userRepository;
	private SystemNotificationRepository systemNotificationRepository;
	private ProductService productService;

	@RequestMapping("/main")
	public ModelAndView main(HttpServletRequest request) throws Exception {
		log.debug("main(): entered...");

		int userId = PermUserContext.getCurrentUserId(request);  // throws Exception

		List<Product> productList = userRepository.getCurrentProductsForAuthor(userId);
		//List<CommonWork> cwList = userRepository.getCurrentCWsForAuthor(userId);

		ModelAndView mv = new ModelAndView("user.landing");
		mv.addObject("productList", productList);

		// this is same as internal-web UserLandingController
		List<SystemNotification> notifications = systemNotificationRepository.loadActiveNotifications();
		if (CollectionUtils.isNotEmpty(notifications)) {
			mv.addObject("notification", notifications.get(0).getMessageToPost());
		}

		return mv;
	}

	@RequestMapping("/archived")
	public ModelAndView archived(HttpServletRequest request) throws Exception {
		log.debug("archived(): entered...");

		int userId = PermUserContext.getCurrentUserId(request);  // throws Exception
		List<Product> productList = userRepository.getArchivedProductsForAuthor(userId);

		ModelAndView mv = new ModelAndView("user.landing.archived");  // archivedProjects.jspx
		mv.addObject("productList", productList);

		return mv;
	}

	@RequestMapping("/togglePermissionComplete")
	public ModelAndView togglePermissionComplete(HttpServletRequest request, @RequestParam("cwId") Integer cwId)
		throws Exception
	{
		log.debug("togglePermissionComplete(): entered...");
		int authorId = PermUserContext.getCurrentUserId(request);  // throws Exception

		AuthorToCommonWork a2cw = userRepository.find(AuthorToCommonWork.class, new AuthorCommonWorkPK (authorId, cwId));
		// a2cw cannot be null - we should already have an association
		a2cw.setPermissionComplete(!a2cw.isPermissionComplete());
		userRepository.save(a2cw);

		ModelAndView mv = new ModelAndView("redirect:/sapp/panels/summaryPanel?cwId=" + cwId);
		return mv;
	}

	public UserRepository getUserRepository() {
		return userRepository;
	}

	public void setUserRepository(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public SystemNotificationRepository getSystemNotificationRepository() {
		return systemNotificationRepository;
	}

	public void setSystemNotificationRepository(SystemNotificationRepository systemNotificationRepository) {
		this.systemNotificationRepository = systemNotificationRepository;
	}

	public ProductService getProductService() {
		return productService;
	}

	public void setProductService(ProductService productService) {
		this.productService = productService;
	}
}
