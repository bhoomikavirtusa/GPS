package com.wiley.permissions.web.shared.controllers.landing;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.wiley.permissions.domain.persistence.permissions.CommonWork;
import com.wiley.permissions.domain.persistence.permissions.CwPhotoEstimate;
import com.wiley.permissions.repositories.CommonWorkRepository;
import com.wiley.permissions.services.CommonWorkService;
import com.wiley.permissions.web.shared.PermUserContext;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;

/**
 * This is the Controller to manage the ProductPhotoEstimates page. Compared with
 * other controllers, this one does not save the state in memory and when DONE
 * is pressed is persisting the changes, but persists the changes on DB while
 * they are performed (Delete a condition will delete it from DB), adding a
 * condition will add it to DB.
 *
 * @author lnagy
 */
@Controller
/*@RequestMapping("/landing/managePhotoEstimate")*/
public class ManagePhotoEstimateController extends BaseAnnotatedController {
	private static final Log log = LogFactory.getLog(ManagePhotoEstimateController.class);

	private static final String ESTIMATE_ID = "estimateId";

	private String pageView = null;

	private String redirectView = null;

	private CommonWorkService commonWorkService = null;
	private CommonWorkRepository cwRepository;

	@Override
	protected List<String> getReferenceDataNames(HttpServletRequest request) throws Exception
	{
		List<String> output = new ArrayList<String>();
		output.add("photoEstimateTypes");
		return output;
	}

	@Override
	@InitBinder
	public void initBinder(WebDataBinder binder) throws Exception
	{
		log.debug("initBinder(): called for objectName = " + binder.getObjectName());
		super.initBinder(binder);
	}

	@RequestMapping(value="/landing/managePhotoEstimate/view", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView view(HttpServletRequest request) throws Exception
	{
		log.debug("view(): entered...");

		CommonWork cw = PermUserContext.getCurrentCommonWork(request);

		cw = cwRepository.loadCWById(cw.getId());

		ModelAndView mv = new ModelAndView(getPageView());

		mv.addObject("estimates", commonWorkService.loadPhotoEstimates(cw.getId()));
		mv.addObject("cwPhotoEstimate", new CwPhotoEstimate());

		String dateFormat = PermUserContext.getPickerDateFormat(request);

		mv.addObject("dateFormat",dateFormat);

		return mv;
	}

	/*@GetMapping("/delete")*/
	@RequestMapping(value="/landing/managePhotoEstimate/delete", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView delete(@RequestParam(ESTIMATE_ID) Integer estimateId)
	throws Exception
	{
		log.debug("delete(): entered...");

		// to do here the LOAD and REMOVE throws exception DetachedEntityException
		// so we have to create a method in the service that works
		getRepository().remove(CwPhotoEstimate.class, estimateId);

		ModelAndView modelAndView = new ModelAndView(getRedirectView());

		return modelAndView;
	}

	/*@GetMapping("/add")*/
	@RequestMapping(value="/landing/managePhotoEstimate/add", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView add(@ModelAttribute("cwPhotoEstimate") CwPhotoEstimate ppe,
			BindingResult bindingResult, HttpServletRequest request) throws Exception
	{
		log.debug("add(): entered...");

		Double dValue = new Double(0);

		// first calculate total fees.
		if (null != ppe.getFreelanceResearchFees()) {
			dValue = dValue + ppe.getFreelanceResearchFees();
		} else {
			ppe.setFreelanceResearchFees(new Double(0));
		}

		if (null != ppe.getReproductionFees()) {
			dValue = dValue + ppe.getReproductionFees();
		} else {
			ppe.setReproductionFees(new Double(0));
		}

		if (null != ppe.getResearchFees()) {
			dValue = dValue + ppe.getResearchFees();
		} else {
			ppe.setResearchFees(new Double(0));
		}

		if (null == ppe.getPhotos()) {
			ppe.setPhotos(0);
		}

		ppe.setEstimatedValue(dValue);

		getValidator().validate(ppe, bindingResult);

		// if the dataType is int, check that no decimal places were entered
		// Since the variable is a double, the standard validation will not catch this
		// TODO: slight problem with this - DataType is not loaded or passed from form
		// and so is null - fix later
		// ___THIS PART WAS COMMENTED OUT BEFORE
		/*
		if (ppe.getPhotoEstimateType().getDataType().equals(DataType.INT)) {

			double value = ppe.getEstimatedValue();
			if (Math.floor(value) != value) {
		        Object [] errorArgs = { value };
		        bindingResult.rejectValue("estimate.estimatedValue", null, errorArgs, "{0} is not an integer");
			}
		}
		*/

		if (StringUtils.isBlank(ppe.getPhotoEstimateType().getCode())) {
			 Object[] args = new Object[] { ppe.getPhotoEstimateType().getDescription() };
				String defaultMsg = getMessageSource().getMessage(
						"condition.error.int.required", args, null);
				bindingResult.reject("photo.estimate.type.required", args, defaultMsg);

		}

		if (bindingResult.hasErrors()) {
			return handleException(request, bindingResult);
		}

		CommonWork cw = PermUserContext.getCurrentCommonWork(request);

		ppe.setCommonWork(cw);

		commonWorkService.saveCwPhotoEstimate(ppe);

		ModelAndView modelAndView = new ModelAndView(getRedirectView());

		return modelAndView;
	}

	private ModelAndView handleException(HttpServletRequest request, BindingResult bindingResult)
			throws Exception
	{
		ModelAndView mv = view(request);

		log.debug(bindingResult.getAllErrors());

		mv.addObject("globalErrors", bindingResult.getAllErrors());
		return mv;
	}

	public CommonWorkService getCommonWorkService() {
		return commonWorkService;
	}

	public void setCommonWorkService(CommonWorkService commonWorkService) {
		this.commonWorkService = commonWorkService;
	}

	public String getPageView() {
		return pageView;
	}

	public void setPageView(String pageView) {
		this.pageView = pageView;
	}

	public String getRedirectView() {
		return redirectView;
	}

	public void setRedirectView(String redirectView) {
		this.redirectView = redirectView;
	}

	public CommonWorkRepository getCommonWorkRepository() {
		return cwRepository;
	}

	public void setCommonWorkRepository(CommonWorkRepository cwRepository) {
		this.cwRepository = cwRepository;
	}
}
