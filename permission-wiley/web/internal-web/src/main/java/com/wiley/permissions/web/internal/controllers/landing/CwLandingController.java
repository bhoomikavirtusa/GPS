package com.wiley.permissions.web.internal.controllers.landing;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.wiley.permissions.domain.persistence.permissions.UserGroup;
import com.wiley.permissions.web.shared.controllers.landing.LandingController;

/**
 *
 * @author nmedrano
 */
@Controller
public class CwLandingController extends LandingController {

	// --------------------- static data ---------------------------------

	private final static Log log = LogFactory.getLog(CwLandingController.class);

	@ModelAttribute
	public void customReferenceData(Model model) {
		UserGroup authorGroup = (UserGroup) getUserRepository().executeSingleResultNamedQuery("UserGroup.findByName",
				new Object[] {UserGroup.AUTHOR_GROUP.getName()});
		UserGroup freelancerGroup = (UserGroup) getUserRepository().executeSingleResultNamedQuery("UserGroup.findByName",
				new Object[] {UserGroup.FREELANCER_GROUP.getName()});
		Integer[] groupIds = new Integer[] {authorGroup.getId(), freelancerGroup.getId()};
		String sGroupIds = StringUtils.join(groupIds, ",");
		model.addAttribute("externalGroupIds", sGroupIds );
	}

	// --------------------- instance data -------------------------------

	/*@GetMapping("/cwlanding/scroll")*/
	@RequestMapping(value="/cwlanding/scroll", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView doMainScroll(HttpServletRequest request,
		@RequestParam(value = "cwId", required = false) Integer cwId,
		@RequestParam(value = "productId", required = false) Integer productId,
		@RequestParam(value = ISBN13, required = false) String isbn13,
		@RequestParam(value = "pnumber", required = false) String pnumber
		) throws Exception
	{
		return mainScroll(request, cwId, productId, isbn13, pnumber);
	}

	@Override
	/*@GetMapping("/cwlanding/main")*/
	@RequestMapping(value="/cwlanding/main", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView main(HttpServletRequest request,
		@RequestParam(value = "cwId", required = false) Integer cwId,
		@RequestParam(value = "productId", required = false) Integer productId,
		@RequestParam(value = ISBN13, required = false) String isbn13,
		@RequestParam(value = "pnumber", required = false) String pnumber
		) throws Exception
	{		
		log.debug("main(): cwId = " + cwId + ", productId = " + productId + ", isbn13 = " + isbn13 +", pnumber = " + pnumber);
		
		ModelAndView mv;
		try {
			mv = super.main(request, cwId, productId, isbn13, pnumber);
		} catch (Exception ex) {
			log.info("bad identifier or product not found error will redirect to user landing page");
			mv = new ModelAndView("redirect:/sapp/product/userLanding/main");
			mv.addObject("generalMessage", ex.getMessage());
			return mv;
		}
		
		cwId = (Integer) mv.getModel().get("cwId");
		
		mv.addObject("coverCompleteCapable", getCommonWorkRepository().isCompleteCapable(cwId, false));
		mv.addObject("interiorCompleteCapable", getCommonWorkRepository().isCompleteCapable(cwId, true));

		mv.addObject("coverNoPermRequiredCapable", getCommonWorkRepository().isNoPermRequiredCapable(cwId, false));
		mv.addObject("interiorNoPermRequiredCapable", getCommonWorkRepository().isNoPermRequiredCapable(cwId, true));
		return mv;
	}
}
