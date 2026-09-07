package com.wiley.permissions.web.shared.controllers.landing;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.wiley.permissions.domain.persistence.permissions.CommonWork;
import com.wiley.permissions.domain.persistence.permissions.CwHistory;
import com.wiley.permissions.domain.persistence.permissions.CwSummary;
import com.wiley.permissions.domain.persistence.permissions.PermissionStatus;
import com.wiley.permissions.domain.persistence.permissions.Privilege;
import com.wiley.permissions.domain.persistence.permissions.Role;
import com.wiley.permissions.domain.persistence.permissions.UserGroup;
import com.wiley.permissions.persistence.PersistenceException;
import com.wiley.permissions.repositories.CommonWorkRepository;
import com.wiley.permissions.security.web.UserPrincipal;
import com.wiley.permissions.services.CommonWorkService;
import com.wiley.permissions.services.view.AssetSummaryView;
import com.wiley.permissions.web.shared.PermUserContext;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;

/**
 * This class can contain methods that apply to common work management.
 *
 * @author lnagy
 */
@Controller
/*@RequestMapping("/landing/cw")*/
@RequestMapping
public class MainCWController extends BaseAnnotatedController {

	private static final Log log = LogFactory.getLog(MainCWController.class);

	private CommonWorkService commonWorkService;
	private CommonWorkRepository cwRepository;

	private String landingViewName;
	private String adminViewName;
	private String assetSummaryView;

	@ModelAttribute
	public void customReferenceData(Model model) {
		try {
			model.addAttribute("allMediaTypes", getReferenceDataCache().get("mediaTypes"));
			model.addAttribute("allUserGroups", getReferenceDataCache().get("userGroups"));
		} catch (Exception ex) {  // not expected
			log.error("customReferenceData(): caught exception: ", ex);
		}
	}

	@RequestMapping(value="/landing/cw/toggleWatched", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView toggleWatched(HttpServletRequest request,
			@RequestParam("cwId") Integer cwId)
		throws Exception
	{
		log.debug("toggleWatched(): entered...");
		UserPrincipal userPrincipal = PermUserContext.getCurrentUser(request);
		commonWorkService.toggleWatched(userPrincipal.getId(), cwId);
		ModelAndView mv = new ModelAndView("redirect:/sapp/panels/summaryPanel?cwId=" + cwId);
		return mv;
	}

	/*@GetMapping("/toggleCWStatus")*/
	@RequestMapping(value="/landing/cw/toggleCWStatus", method = {RequestMethod.GET, RequestMethod.POST})
	public void toggleCWStatus(HttpServletRequest request, HttpServletResponse response,
			@RequestParam("cwId") Integer cwId,
			@RequestParam("cwStatusCode") String cwStatusCode,
			@RequestParam("interior") boolean interior)
		throws Exception
	{
		log.debug("toggleCWStatus(): entered...");
		boolean isAdmin = request.isUserInRole(Role.ADMIN.getCode());
		String msg = commonWorkService.toggleCWStatus(cwId, cwStatusCode, interior, isAdmin);

        response.setContentType("text/plain");
        PrintWriter writer = response.getWriter();  // throws IOException
        if (msg == null) writer.print("ok");
        else writer.print(msg);
	}

	/*@GetMapping("/updateAllStatuses")*/
	@RequestMapping(value="/landing/cw/updateAllStatuses", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView updateAllStatuses(HttpServletRequest request)
		throws Exception
	{
		log.debug("updateAllStatuses(): entered...");
		commonWorkService.updateAllStatuses();
		ModelAndView mv = new ModelAndView(getAdminViewName());
		return mv;
	}

	/*@GetMapping("/assetSummary")*/
	@RequestMapping(value="/landing/cw/assetSummary", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView assetSummary(HttpServletRequest request,
			@RequestParam(value="mediaType", required=false) String[] mediaType,
			@RequestParam(value="userGroup", required=false) Integer[] userGroup)
		throws Exception
	{
		log.debug("assetSummary(): entered...");

		CommonWork cw = PermUserContext.getCurrentCommonWork(request);

		if (null == cw) {
			String msg = getMessageSource().getMessage("no.product.for.product.landing", null, null);
			ModelAndView mv = new ModelAndView(getLandingViewName() + "?generalMessage=" + msg);
			return mv;
		}

		CwSummary cwSummary = cwRepository.find(CwSummary.class, cw.getId());

		if (cwSummary == null) {
			cwSummary = new CwSummary();
		}

		int cwId = cw.getId();

		ModelAndView mv = new ModelAndView(getAssetSummaryView());

		boolean includeCovers = request.isUserInRole(Role.EMPLOYEE_DEFAULT.getCode())
			|| request.isUserInRole(Privilege.COVER_ASSETS.getCode());

		String mediaTypes = (null == mediaType) ? null : "'" + StringUtils.join(mediaType, "','") + "'";
		mv.addObject("summary", cwRepository.loadAssetSummary(cwId, includeCovers, mediaTypes, StringUtils.join(userGroup)));
		mv.addObject("statuses", cwRepository.loadAll(PermissionStatus.class));
		// added this back because it is needed to save progress and due dates
		mv.addObject("commonWork", cwRepository.loadWithPrimaryProductById(cwId));
		mv.addObject("cwSummary", cwSummary);

		mv.addObject("photoEstimates", cwRepository.loadPhotoEstimates(cwId));
		mv.addObject("totalEstimatedCost", cwRepository.getTotalEstimatedCost(cwId));
		mv.addObject("currentActualCost", cwRepository.getCurrentActualCost(cwId));
		mv.addObject("mediaTypes", (null != mediaType) ? Arrays.asList(mediaType) : null);
		mv.addObject("userGroups", (null != userGroup) ? Arrays.asList(userGroup) : null);

		return mv;
	}

	/**
	 *
	 * @param request
	 * @param cwId
	 * @param limit  0 means no limit
	 */
	/*@GetMapping("/history")*/
	@RequestMapping(value="/landing/cw/history", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView history(HttpServletRequest request,
			@RequestParam("cwId") int cwId,
			@RequestParam("limit") int limit) throws PersistenceException
	{
		log.debug("history(): cwId = " + cwId);
		List<CwHistory> historyList = cwRepository.getHistory(cwId, limit);

		ModelAndView mv = new ModelAndView(limit == 0 ? "pages.landing.cwHistoryDialog" : "pages.landing.cwHistoryPanel");
		mv.addObject("historyList", historyList);
		mv.addObject("cwId", cwId);

		return mv;
	}

	/*@GetMapping("/countsPanel")*/
	@RequestMapping(value="/landing/cw/countsPanel", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView countsPanel(HttpServletRequest request,
			@RequestParam("cwId") int cwId)
		throws Exception
	{
		log.debug("countsPanel(): cwId = " + cwId);

		boolean includeCovers = request.isUserInRole(Role.EMPLOYEE_DEFAULT.getCode())
			|| request.isUserInRole(Privilege.COVER_ASSETS.getCode());
		AssetSummaryView countsView = cwRepository.loadAssetsCountByUserGroup(cwId, includeCovers);

		ModelAndView mv = new ModelAndView("pages.landing.summaryCountsPanel");

		mv.addObject("groups", cwRepository.loadAll(UserGroup.class));
		mv.addObject("T", countsView);
		mv.addObject("cwId", cwId);

		return mv;
	}

	public CommonWorkService getCommonWorkService() {
		return commonWorkService;
	}

	public void setCommonWorkService(CommonWorkService commonWorkService) {
		this.commonWorkService = commonWorkService;
	}

	public String getLandingViewName() {
		return landingViewName;
	}

	public void setLandingViewName(String landingViewName) {
		this.landingViewName = landingViewName;
	}

	public String getAdminViewName() {
		return adminViewName;
	}

	public void setAdminViewName(String value) {
		this.adminViewName = value;
	}

	public String getAssetSummaryView() {
		return assetSummaryView;
	}

	public void setAssetSummaryView(String assetSummaryView) {
		this.assetSummaryView = assetSummaryView;
	}

	public CommonWorkRepository getCommonWorkRepository() {
		return cwRepository;
	}

	public void setCommonWorkRepository(CommonWorkRepository cwRepositry) {
		this.cwRepository = cwRepositry;
	}
}
