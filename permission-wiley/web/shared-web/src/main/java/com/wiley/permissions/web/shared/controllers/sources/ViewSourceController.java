package com.wiley.permissions.web.shared.controllers.sources;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.ModelAndView;

import com.wiley.permissions.domain.persistence.permissions.CommonWork;
import com.wiley.permissions.domain.persistence.permissions.Contact;
import com.wiley.permissions.domain.persistence.permissions.Contract;
import com.wiley.permissions.domain.persistence.permissions.Currency;
import com.wiley.permissions.domain.persistence.permissions.Source;
import com.wiley.permissions.domain.persistence.permissions.SourceAddress;
import com.wiley.permissions.repositories.SourceRepository;
import com.wiley.permissions.web.shared.PermUserContext;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;
import com.wiley.permissions.web.shared.controllers.BaseFormBean.FormMode;

/**
 *
 * @author ttidwell
 */
@Controller
/*@RequestMapping("/sources/viewSource")*/
@RequestMapping
@SessionAttributes(ViewSourceController.MODEL_FORM_NAME)
public class ViewSourceController
extends BaseAnnotatedController
{
	private final static Log log = LogFactory.getLog(ViewSourceController.class);

	public final static String SOURCE_ID = "sourceId";
	public final static String SOURCE_EXT_ID = "sourceExtId";

	private final static String ADDRESS_ID = "addressId";
	private final static String CONTACT_ID = "contactId";

	// when compile on command line requires this not to be private or won't compile
	// (since used above class definition)
	protected final static String MODEL_FORM_NAME = "viewSourceForm";

	private SourceRepository sourceRepository;

	private String manageSourceURL;
	private String manageAddressURL;
	private String manageContactURL;
	private String manageNoFlyURL;


	@RequestMapping(value="/sources/viewSource/form", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView form(HttpServletRequest request,
			@RequestParam(value="cameFromSourceGroupId", required=false) Integer cameFromSourceGroupId)
	throws Exception
	{
		log.debug("form(): entered, cameFromSourceGroupId = " + cameFromSourceGroupId);

		// In order for this mechanism to work properly, any link to this controller
		// which is NOT from sourceGroup management must pass cameFromSourceGroupId=0.
		// (And the link from sourceGroup management should pass the flag with a valid group id).
		if (cameFromSourceGroupId != null) {
			if (cameFromSourceGroupId == 0) {
				request.getSession().removeAttribute("cameFromSourceGroupId");
			}
			else {
				request.getSession().setAttribute("cameFromSourceGroupId", cameFromSourceGroupId);
			}
		}

		ViewSourceForm form = new ViewSourceForm();
        updateForm(request, form);

		// parameter may be null
		form.setCameFrom(request.getParameter("cameFrom"));
		log.debug("form(): cameFrom = " + form.getCameFrom());

		ModelAndView mv = new ModelAndView(getFormView());
		mv.addObject(MODEL_FORM_NAME, form);
		return mv;
	}

	/*@GetMapping("/submit")*/
	@RequestMapping(value="/sources/viewSource/submit", method = RequestMethod.POST)
	public ModelAndView onSubmit(HttpServletRequest request,
			@ModelAttribute(MODEL_FORM_NAME) ViewSourceForm form, BindingResult bindingResult)
	throws Exception
	{
		log.debug("onSubmit(): entered..., sourceCommand = " + form.getSourceCommand());
		ModelAndView mv = new ModelAndView(getFormView());

		String redirectURL = null;

		FormMode mode = FormMode.VIEW;

		switch (form.getSourceCommand()) {
			case ADD_CONTACT: {
				redirectURL = manageContactURL;
				mode = FormMode.ADD;
				break;
			}

			case ADD_CONTACT_ADDRESS: {
				redirectURL = manageAddressURL;
				mv.addObject(CONTACT_ID, form.getCurrentContactId());
				mode = FormMode.ADD;
				break;
			}

			case ADD_SOURCE_ADDRESS: {
				redirectURL = manageAddressURL;
				mode = FormMode.ADD;
				break;
			}

			case MODIFY_CONTACT: {
				redirectURL = manageContactURL;
				mv.addObject(CONTACT_ID, form.getCurrentContactId());
				mode = FormMode.MODIFY;
				break;
			}

			case MODIFY_CONTACT_ADDRESS: {
				redirectURL = manageAddressURL;

				mv.addObject(CONTACT_ID, form.getCurrentContactId());
				mv.addObject(ADDRESS_ID, form.getCurrentContactAddressId());

				mode = FormMode.MODIFY;
				break;
			}

			case MODIFY_SOURCE: {
				redirectURL = manageSourceURL;
				mode = FormMode.MODIFY;
				mv.addObject("managementMode", form.getManagementMode());
				break;
			}

			case NOFLY: {
				redirectURL = manageNoFlyURL;
				mode = FormMode.MODIFY;
				mv.addObject("managementMode", form.getManagementMode());
				break;
			}

			case MODIFY_SOURCE_ADDRESS: {
				redirectURL = manageAddressURL;
				mv.addObject(ADDRESS_ID, form.getCurrentSourceAddressId());
				mode = FormMode.MODIFY;
				break;
			}

			case REMOVE_CONTACT: {
				redirectURL = manageContactURL;

				mv.addObject(CONTACT_ID, form.getCurrentContactId());
				mv.addObject("remove", true);

				mode = FormMode.REMOVE;
				break;
			}

			case REMOVE_CONTACT_ADDRESS: {
				redirectURL = manageAddressURL;

				mv.addObject(CONTACT_ID, form.getCurrentContactId());
				mv.addObject(ADDRESS_ID, form.getCurrentContactAddressId());
				mv.addObject("remove", true);

				mode = FormMode.REMOVE;
				break;
			}

			case REMOVE_SOURCE_ADDRESS: {
				redirectURL = manageAddressURL;

				mv.addObject(ADDRESS_ID, form.getCurrentSourceAddressId());
				mv.addObject("remove", true);

				mode = FormMode.REMOVE;
				break;
			}

			case VIEW: {
				updateForm(request, form);
				redirectURL = null;
				break;
			}
		}

		if (redirectURL != null) {
			mv.addObject(SOURCE_ID, form.getCurrentSourceId());
			mv.addObject("mode", mode);
			mv.setViewName(redirectURL);
		}
		else {
			mv.addObject(MODEL_FORM_NAME, form);
		}

		return mv;
	}

	private void updateForm(HttpServletRequest request, ViewSourceForm form) throws Exception {
		Integer sourceId = null;

		String sourceIdString = request.getParameter(SOURCE_ID);
		// externalId will be used when coming from CMS
		String sourceExtId = request.getParameter(SOURCE_EXT_ID);

		if (StringUtils.isNotBlank(sourceIdString)) {
			sourceId = new Integer(sourceIdString);
		} else if (StringUtils.isNotBlank(sourceExtId)) {
			Source source = getSourceRepository().loadSourceByExternalId(sourceExtId);


			sourceId = source.getId();
	    } else {
			sourceId = (Integer) request.getAttribute(SOURCE_ID);
		}

		if (sourceId != null) {
			log.debug("updateForm(): Got Source ID: " + sourceId);
			form.setCurrentSourceId(sourceId);
		}

		populateForm(request, form); // throws Exception
	}

	private void populateForm(HttpServletRequest request, ViewSourceForm form) throws Exception {
		Integer sourceId = form.getCurrentSourceId();

		log.debug("populateForm(): sourceId = " + sourceId);

		if (sourceId == null) {
			throw new RuntimeException("sourceId not expected to be null");
		}

	//	Source source = sourceRepository.loadSourceByIdForView(sourceId, true);
		Source source = getSourceRepository().getSourceView(sourceId);

		if (source == null) {
			throw new RuntimeException("Did not find source for id " + sourceId);
		}

		form.setSource(source);
		if (CollectionUtils.isNotEmpty(source.getContacts())) {
			form.setContact(source.getContacts().get(0));
			if (form.getCurrentContactId() != null) {
				for (Contact c : source.getContacts()) {
					if (form.getCurrentContactId().equals(c.getId())) {
						form.setContact(c);
					}
				}
			}
		}
		if (CollectionUtils.isNotEmpty(source.getAddresses())) {
			form.setSourceAddress(source.getAddresses().get(0));
			if (form.getCurrentSourceAddressId() != null) {
				for (SourceAddress sa : source.getAddresses()) {
					if (form.getCurrentSourceAddressId().equals(sa.getId())) {
						form.setSourceAddress(sa);
					}
				}
			}
		}

		// This needs to happen when form() is called
		// maybe move this code elsewhere and use model instead of raw
		// request object when convert to annotated controller
		CommonWork cw = PermUserContext.getCurrentCommonWork(request);

		if (cw != null) {
			// currency will be overwritten below unless there
			// are no contracts for the current product
			Currency currency = Currency.US;
			double totalPrice = 0;
			int assetCount = 0;

			for (Contract contract: source.getContracts()) {
				// Assume currency is the same for all contracts (it should be).
				currency = contract.getCurrency();

				CommonWork contractCommonWork = contract.getCommonWork();
				if (contractCommonWork != null
						&& contractCommonWork.getId().equals(cw.getId())) {
					totalPrice += contract.getPrice();
					assetCount += contract.getAssets().size();
				}
			}

			request.setAttribute("sourceCurrency", currency);
			request.setAttribute("sourceCwAssetCount", assetCount);
			request.setAttribute("sourceCwContractPrice", totalPrice);
		}
	}

	public SourceRepository getSourceRepository() {
		return sourceRepository;
	}

	public void setSourceRepository(SourceRepository sourceRepository) {
		this.sourceRepository = sourceRepository;
	}

	public String getManageAddressURL() {
		return manageAddressURL;
	}

	public void setManageAddressURL(String manageAddressURL) {
		this.manageAddressURL = manageAddressURL;
	}

	public String getManageContactURL() {
		return manageContactURL;
	}

	public void setManageContactURL(String manageContactURL) {
		this.manageContactURL = manageContactURL;
	}

	public String getManageSourceURL() {
		return manageSourceURL;
	}

	public void setManageSourceURL(String manageSourceURL) {
		this.manageSourceURL = manageSourceURL;
	}

	public String getManageNoFlyURL() {
		return manageNoFlyURL;
	}

	public void setManageNoFlyURL(String manageNoFlyURL) {
		this.manageNoFlyURL = manageNoFlyURL;
	}
}
