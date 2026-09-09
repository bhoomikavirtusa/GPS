package com.wiley.permissions.web.shared.controllers.landing;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.wiley.permissions.domain.persistence.permissions.CwSummary;
import com.wiley.permissions.repositories.CommonWorkRepository;
import com.wiley.permissions.services.CommonWorkService;
import com.wiley.permissions.web.shared.PermUserContext;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;

/**
 *
 * @author smarkoff
 */
@Controller
public class ManageSummaryController extends BaseAnnotatedController {
	private static final Log log = LogFactory.getLog(ManageSummaryController.class);

	private static final String CW_ID = "cwId";
	private static final String MODEL_FORM_NAME = "cwSummary";

	private CommonWorkRepository cwRepository = null;
	private CommonWorkService commonWorkService;

	@RequestMapping(value = "/landing/manageSummary", method = RequestMethod.GET)
	public String formBackingObject(HttpServletRequest request, Model model,
			@RequestParam(CW_ID) Integer cwId)
	throws Exception
	{
		log.debug("formBackingObject(): entered...");

		CwSummary cwSummary = cwRepository.find(CwSummary.class, cwId);

		if (cwSummary == null) {
			cwSummary = new CwSummary();
			cwSummary.setCwId(cwId);
		}

		String dateFormat = PermUserContext.getPickerDateFormat(request);
		model.addAttribute("dateFormat",dateFormat);

		model.addAttribute(MODEL_FORM_NAME, cwSummary);

		return getFormView();
	}

	@RequestMapping(value = "/landing/manageSummary", method = RequestMethod.POST)
	public String onSubmit(HttpServletRequest request,
			@ModelAttribute(MODEL_FORM_NAME) CwSummary cwSummary,
			BindingResult bindingResult)
	throws Exception
	{
		log.debug("onSubmit(): entered...");

		getValidator().validate(cwSummary, bindingResult);

        if (bindingResult.hasErrors()) {
        	return getFormView();
        }
        else {
        	commonWorkService.saveCwSummary(cwSummary);

            return getSuccessView();
        }
	}

	public CommonWorkRepository getCommonWorkRepository() {
		return cwRepository;
	}

	public void setCommonWorkRepository(CommonWorkRepository cwRepository) {
		this.cwRepository = cwRepository;
	}

	public CommonWorkService getCommonWorkService() {
		return commonWorkService;
	}

	public void setCommonWorkService(CommonWorkService commonWorkService) {
		this.commonWorkService = commonWorkService;
	}
}
