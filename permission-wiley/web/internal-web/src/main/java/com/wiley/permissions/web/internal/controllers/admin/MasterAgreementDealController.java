package com.wiley.permissions.web.internal.controllers.admin;

import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import com.wiley.permissions.common.transformer.ObjectToJson;
import com.wiley.permissions.domain.persistence.permissions.BusinessUnit;
import com.wiley.permissions.domain.persistence.permissions.ConditionNode;
import com.wiley.permissions.domain.persistence.permissions.MasterAgreementDeal;
import com.wiley.permissions.domain.persistence.permissions.Source;
import com.wiley.permissions.domain.persistence.permissions.UserLocation;
import com.wiley.permissions.repositories.ConditionRepository;
import com.wiley.permissions.repositories.ContractRepository;
import com.wiley.permissions.repositories.SourceRepository;
import com.wiley.permissions.security.web.UserPrincipal;
import com.wiley.permissions.services.SourceService;
import com.wiley.permissions.web.shared.PermUserContext;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;
import com.wiley.permissions.web.shared.controllers.landing.ConditionControllerUtils;
import com.wiley.permissions.web.shared.util.GenericFileView;

@Controller
/*@RequestMapping("/admin/sourcegroup/madeal")*/
@RequestMapping
public class MasterAgreementDealController extends BaseAnnotatedController {

	private final static Log log = LogFactory.getLog(MasterAgreementDealController.class);
	private final static String CONDITIONS_NAME = "maDealConditions";

	// default can be overridden in Spring config (if add get/set)
	private final long maximumFileSize = 10 * 1024 * 1024;

	private SourceService sourceService;
	private SourceRepository sourceRepository;
	private ConditionRepository conditionRepository;
	private ContractRepository contractRepository;


	@ModelAttribute
	public void customReferenceData(Model model) {
		model.addAttribute("allBusinessUnits", BusinessUnit.ALL_FOR_MA_DEAL);
		model.addAttribute("allUserLocations", UserLocation.ALL);
	}

	// keep "agreementId" instead of "dealId" because later may rename deal to just agreement (no longer have both)
	@RequestMapping(value="/admin/sourcegroup/madeal/download" ,method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView download(@RequestParam("agreementId") int agreementId)
			throws Exception
	{
		log.debug("download(): agreementId = " + agreementId);

		ModelAndView mv = new ModelAndView();
		GenericFileView view = new GenericFileView();

		MasterAgreementDeal agreement = getSourceRepository().find(MasterAgreementDeal.class, agreementId);

		view.setFileName(agreement.getFileName());
		view.setContentType(agreement.getMimeType());

		log.debug("download(): File Data length: " + agreement.getFileData().length);
		view.setData(agreement.getFileData());
		mv.setView(view);
		return mv;
	}

	/**
	 * @param dealId   Must be 0 for a new Deal
	 * @param groupId  Must be a valid groupId
	 */
	@RequestMapping(value = "/admin/sourcegroup/madeal/edit", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView edit(@RequestParam(value = "dealId", required = false) Integer dealId,
			@RequestParam("groupId") int groupId)
			throws Exception
	{
		log.debug("edit(): dealId = " + dealId + ", groupId = " + groupId);
		ModelAndView mv = new ModelAndView(getFormView());
		MasterAgreementDeal maDeal = null;
		// maDeal.setId(dealId);

		if (null == dealId) {
			maDeal = new MasterAgreementDeal();
			maDeal.setSourceGroupId(groupId);
			maDeal.setSelectedBusinessUnits(BusinessUnit.ALL_FOR_MA_DEAL);
			maDeal.setSelectedUserLocations(UserLocation.ALL);
		}
		else {
			maDeal = getSourceRepository().lazyLoad(MasterAgreementDeal.class, dealId,
					new String [] { "businessUnits", "userLocations", "conditions" });
			if (maDeal == null) {  // should not happen
				throw new RuntimeException("MA Deal with id " + dealId + " not found.");
			}
			maDeal.setSelectedBusinessUnitsFromData();
			maDeal.setSelectedUserLocationsFromData();
		}

		mv.addObject("maDeal", maDeal);
		return mv;
	}

	@RequestMapping(value = "/admin/sourcegroup/madeal/submit", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView submit(HttpServletRequest request,
			@RequestParam(value = "file", required = false) MultipartFile mpf,
			@ModelAttribute("maDeal") MasterAgreementDeal maDeal,
			BindingResult bindingResult)
			throws Exception
	{
		log.debug("submit(): entered...");
		ModelAndView mv = new ModelAndView(getSuccessView() + "&groupId=" + maDeal.getSourceGroupId());

		if (mpf != null && mpf.getSize() > 0) {
			if (mpf.getSize() <= maximumFileSize) {
				maDeal.setFileData(mpf.getBytes());
				maDeal.setFileName(mpf.getOriginalFilename());
				maDeal.setMimeType(mpf.getContentType());

				log.debug("submit(): File has " + mpf.getSize() + " bytes");
			}
			else {
				log.info("submit(): File TOO LARGE - has " + mpf.getSize() + " bytes");
				bindingResult.reject(null, "File too large. Maximum size is " + maximumFileSize + " bytes.");
			}
		}
		else if (maDeal.getId() == null || maDeal.getId() == 0) {
			bindingResult.reject(null, "File cannot be empty");
		}
		else {  // file is not set and maDeal is not new (so already has a file)
			// set file to existing
			MasterAgreementDeal dbRecord = getSourceRepository().find(MasterAgreementDeal.class, maDeal.getId());
			maDeal.setFileName(dbRecord.getFileName());
			maDeal.setMimeType(dbRecord.getMimeType());
			maDeal.setFileData(dbRecord.getFileData());
		}

		maDeal.setBusinessUnitsFromSelected();
		maDeal.setUserLocationsFromSelected();

		if (bindingResult.hasErrors()) {
			return new ModelAndView(getFormView());
		}

		@SuppressWarnings("unchecked")
        List<ConditionNode> conditions = (List<ConditionNode>) request.getSession().getAttribute(CONDITIONS_NAME);
		sourceService.saveMasterAgreementDeal(maDeal, conditions);

		return mv;
	}

	@RequestMapping(value = "/admin/sourcegroup/madeal/delete", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView delete(@RequestParam("dealId") int dealId,
			@RequestParam("groupId") int groupId)
			throws Exception
	{
		log.debug("delete(): dealId = " + dealId + ", groupId = " + groupId);
		boolean noContractUse = sourceRepository.removeMasterAgreementDeal(dealId);
		String view = getSuccessView() + "&groupId=" + groupId;
		if (!noContractUse) {
			view += "&maDealUsedByContract=true";
		}
		ModelAndView mv = new ModelAndView(view);
		return mv;
	}

	@RequestMapping(value="/admin/sourcegroup/madeal/loadConditions", method = {RequestMethod.GET, RequestMethod.POST})
	public void loadConditions(HttpServletRequest request, HttpServletResponse response,
			@RequestParam(value = "maDealId", required = false) Integer maDealId) throws Exception {
		log.debug("loadConditions(): maDealId = " + maDealId);

		List<ConditionNode> conditions = conditionRepository.loadMaDealConditions(maDealId);
		// save data in session
		request.getSession().setAttribute(CONDITIONS_NAME, conditions);

		String json = ObjectToJson.doTransform(conditions, null);
		log.debug("json: " + json);

        response.setContentType("application/json");
        PrintWriter writer = response.getWriter();  // throws IOException
        writer.println(json);
	}

	/**
	 * This is almost exactly the same as ContractWizardController.postNode().
	 * Perhaps should refactor.
	 */
	@RequestMapping(value="/admin/sourcegroup/madeal/postConditionNode", method = {RequestMethod.GET, RequestMethod.POST})
	public void postConditionNode (HttpServletRequest request,
			@RequestParam(value="parent") String node, HttpServletResponse response) throws Exception {
		log.debug("postConditionNode(): parent: " + node);
		// load data from session
		@SuppressWarnings("unchecked")
		List<ConditionNode> conditions = (List<ConditionNode>) request.getSession().getAttribute(CONDITIONS_NAME);
		ConditionNode cnode = ConditionControllerUtils.refreshParentNode(request, conditions, node);

		response.setContentType("text/plain");
		PrintWriter writer = response.getWriter();  // throws IOException
		// cannot write null to the writer - throws Exception
		String rollup = (null == cnode.getRollupValue() ? "" : cnode.getRollupValue());
		writer.write(rollup);
	}

	@RequestMapping(value="/admin/sourcegroup/madeal/loadAgreements", method = {RequestMethod.GET, RequestMethod.POST})
	public void loadAgreements(HttpServletRequest request, HttpServletResponse response,
			@RequestParam("sourceId") int sourceId,
			@RequestParam("invoiceDate") String invoiceDateString) throws Exception {
		log.debug("loadAgreements(): sourceId = " + sourceId + ", invoiceDate = " + invoiceDateString);

		DateFormat dateFormat = PermUserContext.getInternalDateFormat();
		Date invoiceDate = dateFormat.parse(invoiceDateString);
		log.debug("loadAgreements(): invoiceDate after parsing: " + dateFormat.format(invoiceDate));
		Source source = sourceRepository.find(Source.class, sourceId);
		if (source == null) { // not expected
			throw new RuntimeException("sourceId [" + sourceId + "] not found");
		}

		List<MasterAgreementDeal> list;
		if (source.getSourceGroup() == null) {
			log.debug("loadAgreements(): No sourceGroup for sourceId [" + sourceId + "] so no master agreements.");
			list = new ArrayList<MasterAgreementDeal>(0);
		}
		else {
			UserPrincipal userPrin = PermUserContext.getCurrentUser(request);
			log.debug("loadAgreements(): userId = " + userPrin.getId());
			list = contractRepository.loadMasterAgreements(source.getSourceGroup().getId(), userPrin.getId(), invoiceDate);
			log.debug("loadAgreements(): list.size() = " + list.size());
		}

		String json = ObjectToJson.doTransform(list, null);
		log.debug("loadAgreements(): sending: " + json);

        response.setContentType("application/json");
        PrintWriter writer = response.getWriter();  // throws IOException
        writer.println(json);
	}

	public SourceService getSourceService() {
    	return sourceService;
    }

	public void setSourceService(SourceService sourceService) {
    	this.sourceService = sourceService;
    }

	public SourceRepository getSourceRepository() {
    	return sourceRepository;
    }

	public void setSourceRepository(SourceRepository sourceRepository) {
    	this.sourceRepository = sourceRepository;
    }

	public ConditionRepository getConditionRepository() {
		return conditionRepository;
	}

	public void setConditionRepository(ConditionRepository conditionRepository) {
		this.conditionRepository = conditionRepository;
	}

	public ContractRepository getContractRepository() {
		return contractRepository;
	}

	public void setContractRepository(ContractRepository contractRepository) {
		this.contractRepository = contractRepository;
	}
}
