package com.wiley.permissions.web.internal.controllers.landing;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import com.wiley.permissions.domain.persistence.permissions.CommonWork;
import com.wiley.permissions.domain.persistence.permissions.Product;
import com.wiley.permissions.domain.persistence.permissions.SystemNotification;
import com.wiley.permissions.domain.persistence.permissions.User;
import com.wiley.permissions.domain.persistence.permissions.UserDefaults;
import com.wiley.permissions.domain.persistence.permissions.UserToRole;
import com.wiley.permissions.repositories.CommonWorkRepository;
import com.wiley.permissions.repositories.ProductRepository;
import com.wiley.permissions.repositories.SystemNotificationRepository;
import com.wiley.permissions.repositories.UserRepository;
import com.wiley.permissions.services.CommonWorkService;
import com.wiley.permissions.services.ProductService;
import com.wiley.permissions.web.shared.PermUserContext;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;

/**
 *
 * @author nmedrano
 */
@Controller
/*@RequestMapping("/product/userLanding")*/
public class UserLandingController extends BaseAnnotatedController {
	private static final Log log = LogFactory.getLog(UserLandingController.class);

	private UserRepository userRepository;
	private CommonWorkRepository commonWorkRepository;
	private ProductRepository productRepository;
	private SystemNotificationRepository systemNotificationRepository;
	private CommonWorkService commonWorkService;
	private ProductService productService;
	private static final String PRODUCT_ISBN13 = "isbn13";


	/*@RequestMapping("/main")*/
	@RequestMapping(value="/product/userLanding/main", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView main(HttpServletRequest request,
			@RequestParam(value = "userId", required = false) Integer userId,
			@RequestParam(value = "generalMessage", required = false) String generalMessage,			
			@RequestParam(value = "cwId", required = false) Integer cwId,
			@RequestParam(value = PRODUCT_ISBN13, required = false) String productISBN13,
			@RequestParam(value = "productId", required = false) Integer productId
			) throws Exception
	{
		log.debug("main(): entered...");

		CommonWork commonWork = null;
		Product product = null;

		if (cwId != null) {
			commonWork = commonWorkService.loadCWLandingView(cwId);

			if (commonWork == null) {
				throw new RuntimeException("commonWorkId " + cwId + " not found.");
			}

			product = commonWork.getPrimaryProduct();
			PermUserContext.setCurrentCommonWork(request, commonWork);
		}
		else if (productId != null) {
			product = productService.loadCWLandingView(productId);
			if (product == null) {
				throw new RuntimeException("productId " + productId + " not found.");
			}
			commonWork = commonWorkService.loadCWLandingView(product.getCommonWork().getId());
			PermUserContext.setCurrentCommonWork(request, commonWork);
		}
		else if (StringUtils.isNotBlank(productISBN13)) {
			product = productRepository.getProductByISBN(productISBN13);
			if (product == null) {
				throw new RuntimeException("productISBN13 " + productISBN13 + " not found.");
			}
			else {
				commonWork = product.getCommonWork();
				PermUserContext.setCurrentCommonWork(request, commonWork);
			}
		}
		else {
			commonWork = PermUserContext.getCurrentCommonWork(request);
			if (commonWork != null) {
				commonWork = commonWorkService.loadCWLandingView(commonWork.getId());
			    product = commonWork.getPrimaryProduct();
			}
		}

		if (userId == null) {
			userId = PermUserContext.getCurrentUserId(request);
		}

		UserDefaults ud = userRepository.loadUserDefaults(userId);
		if (null != ud && null != ud.getDateFormat()) {
			PermUserContext.setDateFormat(request, ud.getDateFormat());
		}

//		if (cwId == null) {
//			cwId = commonWork.getId();
//		}

		ModelAndView mv = new ModelAndView("userLanding");		

		List<UserToRole> landingList = new ArrayList<UserToRole>();
		User user = userRepository.loadDataForUserLanding(userId, landingList);


		List<SystemNotification> notifications = getSystemNotificationRepository().loadActiveNotifications();

		if (CollectionUtils.isNotEmpty(notifications)) {
			mv.addObject("notification", notifications.get(0).getMessageToPost());
		}

		mv.addObject("user", user);
		mv.addObject("userLandingList", landingList);

		if (StringUtils.isNotBlank(generalMessage)) {
			mv.addObject("generalMessage", generalMessage);
		}
		return mv;
	}

	@RequestMapping(value="/product/userLanding/remove", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView removeCW(HttpServletRequest request,
			@RequestParam(value = "userId", required = false) Integer userId,						
			@RequestParam(value = "cwId", required = false) Integer cwId
			) throws Exception
	{
		log.debug("removeCWFromUser(): entered...");

		CommonWork cw = commonWorkRepository.loadWithExtendedPrimaryProductById(cwId);

		String externalId = cw.getPrimaryProduct().getExternalId();
		getCommonWorkService().deleteCWFromUserWatch(userId, externalId);
		return new ModelAndView("redirect:/sapp/product/userLanding/main");
	}


	public UserRepository getUserRepository() {
		return userRepository;
	}

	public void setUserRepository(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public CommonWorkRepository getCommonWorkRepository() {
		return commonWorkRepository;
	}

	public void setCommonWorkRepository(CommonWorkRepository commonWorkRepository) {
		this.commonWorkRepository = commonWorkRepository;
	}

	public ProductRepository getProductRepository() {
		return productRepository;
	}

	public void setProductRepository(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}

	public SystemNotificationRepository getSystemNotificationRepository() {
		return systemNotificationRepository;
	}

	public void setSystemNotificationRepository(SystemNotificationRepository systemNotificationRepository) {
		this.systemNotificationRepository = systemNotificationRepository;
	}

	public CommonWorkService getCommonWorkService() {
		return commonWorkService;
	}

	public void setCommonWorkService(CommonWorkService commonWorkService) {
		this.commonWorkService = commonWorkService;
	}

	public ProductService getProductService() {
		return productService;
	}

	public void setProductService(ProductService productService) {
		this.productService = productService;
	}
}
