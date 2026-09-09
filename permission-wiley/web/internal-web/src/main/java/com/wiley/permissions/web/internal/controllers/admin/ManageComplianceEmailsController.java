package com.wiley.permissions.web.internal.controllers.admin;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.ModelAndView;

import com.wiley.permissions.domain.persistence.permissions.BusinessUnit;
import com.wiley.permissions.domain.persistence.permissions.Role;
import com.wiley.permissions.domain.persistence.permissions.Role.RoleType;
import com.wiley.permissions.repositories.ProductRepository;
import com.wiley.permissions.services.ContractService;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;

/**
 *
 * @author lnagy
 */
@Controller
@RequestMapping("/admin/manageComplianceEmails")
@SessionAttributes(ManageComplianceEmailsController.FORM_MODEL_NAME)
public class ManageComplianceEmailsController
extends BaseAnnotatedController
{
	private final static Log log = LogFactory.getLog(ManageComplianceEmailsController.class);
	public static final String FORM_MODEL_NAME = "ManageComplianceEmailsForm";

	private ProductRepository productRepository;
	private ContractService contractService;


	@RequestMapping(value = "/load", method = RequestMethod.GET)
	public ModelAndView onLoad (HttpServletRequest request)
	throws Exception
	{
		log.debug("onLoad() entered...");

		ModelAndView mv = new ModelAndView(getFormView());
		ManageComplianceEmailsForm form = new ManageComplianceEmailsForm();

		List<BusinessUnit> units = productRepository.loadAll(BusinessUnit.class);
		request.getSession().setAttribute(FORM_MODEL_NAME, units);
		form.setBusinessUnits(units);
		form.setCurrentBusinessUnit(units.get(0));
		form.setSelectedCode(units.get(0).getCode());

		// include only employee roles
		List<Role> roles = productRepository.loadAll(Role.class);
		for (int x=0; x < roles.size(); x++) {
			if (roles.get(x).getRoleType() == RoleType.AUTHOR) {
				roles.remove(x);
				x--;
			}
		}
		Role allRoles = new Role();
		allRoles.setCode("none");
		allRoles.setDescription("****** Do NOT warn based on Role ******");
		roles.add(allRoles);

		form.setRoles(roles);

		mv.addObject(FORM_MODEL_NAME, form);
		// mv.addObject("businessUnits", units);
		return mv;
	}

	@RequestMapping(value = "/load", method = RequestMethod.POST)
	public ModelAndView onSubmit(HttpServletRequest request,
			@ModelAttribute(FORM_MODEL_NAME) ManageComplianceEmailsForm form,
			BindingResult bindingResult)
	throws Exception
	{
		log.debug("onSubmit() called: ");

		// save current unit
		productRepository.save(form.getCurrentBusinessUnit());
		// set next unit
		List<BusinessUnit> units = form.getBusinessUnits();
		for (BusinessUnit unit : units) {
			if (unit.getCode().equalsIgnoreCase(form.getSelectedCode())) {
				form.setCurrentBusinessUnit(unit);
				break;
			}
		}

		ModelAndView mv = new ModelAndView(getFormView());
		mv.addObject(FORM_MODEL_NAME, form);
		return mv;
	}

	/**
	 * Calls the same method that the cron job normally calls.
	 */
	@RequestMapping("/cron")
	public String cron() {
		log.debug("cron() entered...");
		try {
			contractService.calculateOutOfCompliance();
		}
		catch (Exception ex) {
			log.error("cron(): caught exception: ", ex);
		}

		return "redirect:/sapp/admin/manageComplianceEmails/load";
	}

	public ProductRepository getProductRepository() {
		return productRepository;
	}

	public void setProductRepository(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}

	public ContractService getContractService() {
		return contractService;
	}

	public void setContractService(ContractService contractService) {
		this.contractService = contractService;
	}
}
