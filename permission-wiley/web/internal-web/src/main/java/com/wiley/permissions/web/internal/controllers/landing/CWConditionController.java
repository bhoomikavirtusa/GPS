package com.wiley.permissions.web.internal.controllers.landing;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.wiley.permissions.common.transformer.ObjectToJson;
import com.wiley.permissions.domain.persistence.permissions.BusinessUnit;
import com.wiley.permissions.domain.persistence.permissions.CommonWork;
import com.wiley.permissions.domain.persistence.permissions.ConditionNode;
import com.wiley.permissions.domain.persistence.permissions.ConditionType;
import com.wiley.permissions.domain.persistence.permissions.DataSource;
import com.wiley.permissions.repositories.CommonWorkRepository;
import com.wiley.permissions.repositories.ConditionRepository;
import com.wiley.permissions.services.CommonWorkService;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;
import com.wiley.permissions.web.shared.controllers.landing.ConditionControllerUtils;

/*@RequestMapping("/landing/cwConditions")*/
@RequestMapping
public class CWConditionController extends BaseAnnotatedController {

	// --------------------- static data ---------------------------------
	private final static Log log = LogFactory.getLog(CWConditionController.class);

	private static final String CONDITIONS_NAME = "cwConditions";
	private static final String COMMON_WORK = "commonWork";

	private final static String helpMessage = "You are strongly encouraged to acquire the following rights for all 3rd party content:<br/><br/>" +
			"Worldwide distribution, all languages, unlimited print run, for use in print and/or electronic delivery platforms in any and all media now "
			+ "known or hereafter developed, in any and all derivative and ancillary works, for use in advertisements and promotional materials associated "
			+ "with the work; for use in all editions, for the life of those editions without limitation for Wiley and its licensees.<br/><br/>"
			+ "Current minimum settings for this product are:<br/>" +
			"<conditions>" + "<br/><br/>" +
			" You may add additional languages as a minimum requirement and you may change the minimum print run to <division>";

	public static final Map<BusinessUnit, String[]> USDivisionMapping = new HashMap<BusinessUnit, String[]>() {
		private static final long serialVersionUID = 1L;
	{
		put(BusinessUnit.GE, new String[] {"500K units", "500000"});
		put(BusinessUnit.PD, new String[] {"print run equal to two times the expected lifetime sales of print and digital or 100K, whichever is higher", "100000"});
		put(BusinessUnit.GR, new String[] {"15K units", "15000"});
	}};

	public static final Map<BusinessUnit, String[]> AUDivisionMapping = new HashMap<BusinessUnit, String[]>() {
		private static final long serialVersionUID = 1L;
	{
		put(BusinessUnit.GE, new String[] {"50K units", "50000"});
		put(BusinessUnit.PD, new String[] {"print run equal to two times the expected lifetime sales of print and digital or 50K, whichever is higher", "50000"});
		put(BusinessUnit.U9, new String[] {"100K units", "100000"});
	}};

	// --------------------- instance data -------------------------------

	private CommonWorkService commonWorkService;
	private CommonWorkRepository commonWorkRepository;
	private ConditionRepository conditionRepository;

	@RequestMapping(value="/landing/cwConditions/viewEdit", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView viewEdit(HttpServletRequest request,
			@RequestParam("cwId") Integer cwId)
			throws Exception
	{
		log.debug("viewEdit(): entered..., cwId = " + cwId);

		ModelAndView mv = new ModelAndView(getFormView());

		List<ConditionNode> conditions = conditionRepository.loadCwConditions(cwId);

		CommonWork cw = commonWorkRepository.lazyLoad(CommonWork.class, cwId, new String[] {"primaryProduct", "primaryProduct.businessUnit"});
		request.getSession().setAttribute(COMMON_WORK, cw);

		mv.addObject("cwId", cwId);
		mv.addObject("conditions", conditions);

		mv.addObject("grantYears", cw.getMinGrantYears());
		String msg = helpMessage.replace("<conditions>", commonWorkRepository.loadCommonWorkPermissionInfo(cw));
		BusinessUnit bu = cw.getPrimaryProduct().getBusinessUnit();
		log.debug("viewEdit(): BusinessUnit: " + bu);
		Map<BusinessUnit, String []> map = USDivisionMapping;
		if (cw.getPrimaryProduct().getDataSource().equals(DataSource.AU.getCode())) {
			map = AUDivisionMapping;
		}
		String [] mapValue = map.get(bu);
		if (mapValue == null) {  // normally we don't expect this
			throw new RuntimeException("No mapping for BusinessUnit: " + bu);
		}
		msg = msg.replace("<division>", mapValue[0]);
		mv.addObject("helpMessage", msg);
		mv.addObject("readOnly", false);
		return mv;
	}

	/**
	 * This postNode method has extra logic for print run validation
	 * that ContractWizardController.postNode()
	 * and MasterAgreementDealController.postConditionNode()
	 * and LicenseController.postNode() don't have.
	 * But otherwise this method is similar.
	 */
	@RequestMapping(value="/landing/cwConditions/postNode", method = {RequestMethod.GET, RequestMethod.POST})
	public void postNode(HttpServletRequest request,
			@RequestParam(value = "parent") String node, HttpServletResponse response) throws Exception {
		log.debug("postNode(): entered...parent: " + node);
		// load data from session
		@SuppressWarnings("unchecked")
		List<ConditionNode> conditions = (List<ConditionNode>) request.getSession().getAttribute(CONDITIONS_NAME);

		ConditionNode cnode = ConditionControllerUtils.refreshParentNode(request, conditions, node);
		log.debug("postNode(): cnode: " + cnode.getCode());

		if (node.equalsIgnoreCase(ConditionType.PRINT_RUN.getCode())) {
			ConditionNode printRunBoxNode = ConditionControllerUtils.getConditionNode(conditions, ConditionType.PRINT_RUN_LIMIT_BOX.getCode());
			log.debug("postNode(): printRunBoxNode.getValue(): [" + printRunBoxNode.getValue() + "]");

			CommonWork cw = (CommonWork) request.getSession().getAttribute(COMMON_WORK);
			// if value is less then minimum
			BusinessUnit bu = cw.getPrimaryProduct().getBusinessUnit();
			log.debug("postNode(): BusinessUnit: " + bu);
			Map<BusinessUnit, String []> map = USDivisionMapping;
			if (cw.getPrimaryProduct().getDataSource().equals(DataSource.AU.getCode())) {
				map = AUDivisionMapping;
			}
			String [] mapValue = map.get(bu);
			if (mapValue == null) {  // normally we don't expect this
				throw new RuntimeException("No mapping for BusinessUnit: " + bu);
			}
			String minimum = mapValue[1];

			try {
				log.debug("postNode(): mininum: " + minimum);
				if (StringUtils.isBlank(printRunBoxNode.getValue()) || new Integer(printRunBoxNode.getValue()).intValue() < new Integer(minimum).intValue()) {
					printRunBoxNode.setValue(minimum);
					// the client sees "Error" and displays the special dialog saying it's increasing the value
					// (This special rollupValue is not saved to the db)
					cnode.setRollupValue("Error: " + minimum);
				}
			// the value entered might not be a number
			} catch (NumberFormatException e) {
				log.debug("postNode(): failed to convert print run value to number", e);
				cnode.setValue(minimum);
				cnode.setRollupValue("Error: " + minimum);
			}
		}

		response.setContentType("text/plain");
		PrintWriter writer = response.getWriter(); // throws IOException
		// cannot write null to the writer - throws Exception
		String rollup = (null == cnode.getRollupValue() ? "" : cnode.getRollupValue());
		writer.write(rollup);
	}

	@RequestMapping(value="/landing/cwConditions/load", method = {RequestMethod.GET, RequestMethod.POST})
	public void load(HttpServletRequest request, HttpServletResponse response,
			@RequestParam("cwId") int cwId) throws Exception {
		log.debug("load(): cwId = " + cwId);

		List<ConditionNode> conditions = conditionRepository.loadCwConditions(cwId);
		// save data in session
		request.getSession().setAttribute(CONDITIONS_NAME, conditions);

		String json = ObjectToJson.doTransform(conditions, null);
		log.debug("json: " + json);

        response.setContentType("application/json");
        PrintWriter writer = response.getWriter();  // throws IOException
        writer.println(json);
	}

	@RequestMapping(value="/landing/cwConditions/save", method = RequestMethod.POST)
	public ModelAndView save(HttpServletRequest request,
			@RequestParam("cwId") int cwId) throws Exception
	{
		log.debug("save(): cwId = " + cwId);
		// load data from session
		@SuppressWarnings("unchecked")
		List<ConditionNode> conditions = (List<ConditionNode>) request.getSession().getAttribute(CONDITIONS_NAME);
		int numChanges = conditionRepository.saveCwConditions(cwId, conditions);

		if (numChanges > 0) {
			commonWorkService.updateStatusForCommonWork(cwId);  // throws Exception
		}

		ModelAndView mv = new ModelAndView(getSuccessView() + "?cwId=" + cwId);
		return mv;
	}

	@RequestMapping(value="/landing/cwConditions/saveGrantYears", method = RequestMethod.POST)
	public void saveGrantYears(HttpServletRequest request, HttpServletResponse response,
			@RequestParam("cwId") Integer cwId,
			@RequestParam("grantYears") String grantYears) throws Exception
	{
		log.debug("saveGrantYears(): cwId = " + cwId + ", grantYears = " + grantYears);
		String responseString = "Must be an integer";
		try {
			int value = Integer.parseInt(grantYears);
			commonWorkRepository.saveGrantYears(cwId, value);
			responseString = String.valueOf(value);
		}
		catch (NumberFormatException ex) {
			// do nothing
		}

		String json = ObjectToJson.doTransform(responseString, null);
		log.debug("saveGrantYears(): json: " + json);

        response.setContentType("application/json");
        PrintWriter writer = response.getWriter();  // throws IOException
        writer.println(json);
	}


	public void setCommonWorkService(CommonWorkService commonWorkService) {
		this.commonWorkService = commonWorkService;
	}

	public CommonWorkService getCommonWorkService() {
		return commonWorkService;
	}

	public CommonWorkRepository getCommonWorkRepository() {
		return commonWorkRepository;
	}

	public void setCommonWorkRepository(CommonWorkRepository commonWorkRepository) {
		this.commonWorkRepository = commonWorkRepository;
	}

	public ConditionRepository getConditionRepository() {
		return conditionRepository;
	}

	public void setConditionRepository(ConditionRepository conditionRepository) {
		this.conditionRepository = conditionRepository;
	}
}
