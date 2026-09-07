package com.wiley.permissions.web.author.controllers.landing;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.wiley.permissions.domain.persistence.permissions.AuthorCommonWorkPK;
import com.wiley.permissions.domain.persistence.permissions.AuthorToCommonWork;
import com.wiley.permissions.domain.persistence.permissions.Privilege;
import com.wiley.permissions.domain.persistence.permissions.Product;
import com.wiley.permissions.domain.persistence.permissions.PublicationStatus;
import com.wiley.permissions.security.web.UserPrincipal;
import com.wiley.permissions.web.shared.PermUserContext;
import com.wiley.permissions.web.shared.controllers.landing.LandingController;

/**
 *
 * @author nmedrano
 */
@Controller
public class CwLandingController extends LandingController {

	// --------------------- static data ---------------------------------

	private final static Log log = LogFactory.getLog(CwLandingController.class);

	// --------------------- instance data -------------------------------

	@Override
	@RequestMapping("/cwlanding/scroll")
	public ModelAndView mainScroll(HttpServletRequest request,
		@RequestParam(value = "cwId", required = false) Integer cwId,
		@RequestParam(value = "productId", required = false) Integer productId,
		@RequestParam(value = ISBN13, required = false) String isbn13,
		@RequestParam(value = "pnumber", required = false) String pnumber
		) throws Exception
	{
		return super.mainScroll(request, cwId, productId, isbn13, pnumber);
	}

	@Override
	@RequestMapping("/cwlanding/main")
	public ModelAndView main(HttpServletRequest request,
		@RequestParam(value = "cwId", required = false) Integer cwId,
		@RequestParam(value = "productId", required = false) Integer productId,
		@RequestParam(value = ISBN13, required = false) String isbn13,
		@RequestParam(value = "pnumber", required = false) String pnumber
		) throws Exception
	{
		log.debug("main(): cwId = " + cwId + ", productId = " + productId + ", isbn13 = " + isbn13 +", pnumber = " + pnumber);
		UserPrincipal user = PermUserContext.getCurrentUser(request);

		ModelAndView mv = super.main(request, cwId, productId, isbn13, pnumber);
		Product primaryProduct = (Product) mv.getModel().get("primaryProduct");
		cwId = (Integer) mv.getModel().get("cwId");

		AuthorToCommonWork a2cw = getUserRepository().find(AuthorToCommonWork.class, new AuthorCommonWorkPK (user.getId(), cwId));

		try {
			Product testProduct = getProductRepository().lazyLoad (Product.class, primaryProduct.getId(), new String[] {"publicationStatus"});

			boolean includeInProduction = request.isUserInRole(Privilege.IN_PRODUCTION_ACTIVE.getCode());
			boolean isReadOnly = a2cw.isReadOnly() || !PublicationStatus.isPreProduction(testProduct.getPublicationStatus().getCode(), includeInProduction);
			mv.addObject("isReadOnly", isReadOnly);

		} catch (Exception e) {
			mv.addObject("isReadOnly", true);
		}

		// db way (could use index instead but this a little safer)
		boolean hasPreviousEdition = getProductRepository().doesPreviousEditionWIDExist(primaryProduct);
		mv.addObject("hasPreviousEdition", hasPreviousEdition);

		// if the user does not have the privilege to edit all chapters, we have to filter those
		boolean filterChapters = !request.isUserInRole(Privilege.EDIT_CHAPTERS.getCode());
		String sChapters = "";
		if (filterChapters) {
			boolean includeCovers = request.isUserInRole(Privilege.COVER_ASSETS.getCode());
			List<Integer> selectedChapters = getCommonWorkRepository().loadSelectedChaptersId (user.getId(), cwId, includeCovers);
			sChapters = StringUtils.join(selectedChapters, ",");
		}
		mv.addObject("chapters", sChapters);
		return mv;
	}
}
