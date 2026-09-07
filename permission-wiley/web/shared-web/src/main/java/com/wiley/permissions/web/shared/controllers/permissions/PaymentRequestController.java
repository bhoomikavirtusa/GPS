package com.wiley.permissions.web.shared.controllers.permissions;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.ModelAndView;

import com.wiley.permissions.common.transformer.ObjectToJson;
import com.wiley.permissions.domain.persistence.permissions.Account;
import com.wiley.permissions.domain.persistence.permissions.AssetUse;
import com.wiley.permissions.domain.persistence.permissions.CommonWork;
import com.wiley.permissions.domain.persistence.permissions.Contract;
import com.wiley.permissions.domain.persistence.permissions.Contract.PaymentType;
import com.wiley.permissions.domain.persistence.permissions.ContractAsset;
import com.wiley.permissions.domain.persistence.permissions.PaymentRequest;
import com.wiley.permissions.domain.persistence.permissions.Source;
import com.wiley.permissions.domain.persistence.permissions.User;
import com.wiley.permissions.domain.persistence.permissions.UserDefaults;
import com.wiley.permissions.persistence.PersistenceException;
import com.wiley.permissions.repositories.AssetUseRepository;
import com.wiley.permissions.repositories.ContractRepository;
import com.wiley.permissions.repositories.SourceRepository;
import com.wiley.permissions.repositories.UserRepository;
import com.wiley.permissions.services.ContractService;
import com.wiley.permissions.services.util.ServiceException;
import com.wiley.permissions.web.shared.PermUserContext;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;

/**
 *
 * @author Nmedrano
 */
@Controller
/*@RequestMapping("/permissions/paymentrequest/")*/
@RequestMapping
@SessionAttributes(PaymentRequestController.FORM_MODEL_NAME)
public class PaymentRequestController extends BaseAnnotatedController {

	private static final Log log = LogFactory.getLog(PaymentRequestController.class);
	protected static final String FORM_MODEL_NAME = "generatePaymentRequestForm";

	private String paymentRequestView = null;
	private String listPaymentRequestView = null;
	private String redirectListPaymentRequestView = null;
	private String noGoViewName = null;
	private String showSourcesViewName = null;
	private String manageSuccessView = null;

	private ContractService contractService;

	private ContractRepository contractRepository = null;
	private UserRepository userRepository;
	private AssetUseRepository assetUseRepository;
	private SourceRepository sourceRepository;

	@Override
	protected List<String> getReferenceDataNames(HttpServletRequest request) throws Exception
	{
		List<String> output = new ArrayList<String>();

		output.add("accounts");

		return output;
	}

	@RequestMapping(value = "/permissions/paymentrequest/asset_start", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView assetStart(@RequestParam(value="auId") Integer auId)
	throws Exception
	{
		log.debug("assetStart(): entered, auId = " + auId);
		// load minimum information here. If you need extra information,
		// load it on the specific branch (see authorProvided example)
		PaymentRequestForm form = new PaymentRequestForm();

		ModelAndView mv = new ModelAndView("pages.paymentrequest.sources");
		AssetUse au = getAssetUseRepository().loadAssetUseById(auId);
		List<Source> sources = assetUseRepository.loadSourcesNeedingPayment(auId);

		if (CollectionUtils.isEmpty(sources)) {
			mv = new ModelAndView("dialog.success");
			mv.addObject("message", "No source for status granted");
		}
		// just one source
		if (sources.size() == 1) {
			mv = new ModelAndView("redirect:/sapp/permissions/paymentrequest/select_source?sourceId=" + sources.get(0).getId());
		//	form.setPrevUrl (null);
			form.setPrevUrl ("redirect:/sapp/permissions/paymentrequest/asset_start?auId=" + auId);
		} else {
			mv.addObject("sources", sources);
			form.setPrevUrl ("redirect:/sapp/permissions/paymentrequest/asset_start?auId=" + auId);
		}
		form.setCwId (au.getCommonWork().getId());
		form.setAssetId (au.getAsset().getId());
		mv.addObject(FORM_MODEL_NAME, form);

		return mv;
	}

	@RequestMapping(value = "/permissions/paymentrequest/source_start", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView sourceStart(@RequestParam(value="sourceId") Integer sourceId,
			@RequestParam(value="cwId") Integer cwId)
	throws Exception
	{
		log.debug("sourceStart(): entered, cwId = " + cwId);
		// load minimum information here. If you need extra information,
		// load it on the specific branch (see authorProvided example)
		PaymentRequestForm form = new PaymentRequestForm();
		form.setCwId(cwId);

		ModelAndView mv = new ModelAndView("redirect:/sapp/permissions/paymentrequest/select_source?sourceId=" + sourceId);
		form.setPrevUrl ("redirect:/sapp/permissions/paymentrequest/select_source?sourceId=" + sourceId);
		mv.addObject(FORM_MODEL_NAME, form);

		return mv;
	}

	@RequestMapping(value = "/permissions/paymentrequest/select_source", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView selectSource(@RequestParam(value="sourceId") Integer sourceId,
			@ModelAttribute(FORM_MODEL_NAME) PaymentRequestForm form)
	throws Exception
	{
		log.debug("selectSource(): entered, sourceId = " + sourceId + ", cwId = " + form.getCwId());

		ModelAndView mv = new ModelAndView("pages.paymentrequest.contractlist");
		Source source = sourceRepository.loadSourceById(sourceId);
		if (null == source) {
			mv = new ModelAndView("dialog.success");
			mv.addObject("message", "No source found");
		}
		form.setSource(source);
		mv.addObject(FORM_MODEL_NAME, form);

		// TODO load only those that need payment and are not paid
		List<Contract> contracts = contractRepository.loadUnpaidContractList(form.getCwId(), sourceId, form.getAssetId());
		if (CollectionUtils.isEmpty(contracts)) {
			mv = new ModelAndView("dialog.success");
			mv.addObject("message", "No contract found");
		}

		if (contracts.size() == 1) {
			mv = new ModelAndView("redirect:/sapp/permissions/paymentrequest/select_contract?contractId=" + contracts.get(0).getId());
		} else {
			mv.addObject("contracts", contracts);
			form.setPrevUrl ("/sapp/permissions/paymentrequest/select_source?sourceId=" + sourceId);
		}
		return mv;
	}

	@RequestMapping(value = "/permissions/paymentrequest/select_contract", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView selectContract(HttpServletRequest request, @RequestParam("contractId") Integer contractId,
			@ModelAttribute(FORM_MODEL_NAME) PaymentRequestForm form)
	throws Exception
	{
		log.debug("selectContract(): entered, contractId = " + contractId);
		form.setContractId(contractId);
		ModelAndView mv = new ModelAndView("redirect:/sapp/permissions/paymentrequest/managePaymentRequest");
		mv.addObject(FORM_MODEL_NAME, form);
		
		String DATE_FORMAT = "mm/dd/yy]";
		if(null != PermUserContext.getDateFormat(request)) {
			DATE_FORMAT = PermUserContext.getDateFormat(request).toLowerCase();
			DATE_FORMAT = DATE_FORMAT.replace("yyyy", "yy");
		}
		mv.addObject("dateFormat",DATE_FORMAT);
		
		
		return mv;
	}

	/**
	 * This is used by the Pay link on the Invoice tab (on the landing page).
	 */
	@GetMapping("/permissions/paymentrequest/select_contract_create_form")
	public ModelAndView selectContractCreateForm(@RequestParam("contractId") int contractId) throws Exception {
		ModelAndView mv = new ModelAndView("redirect:/sapp/permissions/paymentrequest/select_contract?contractId=" + contractId);
		PaymentRequestForm form = new PaymentRequestForm();
		Contract contract = contractRepository.lazyLoad(Contract.class, contractId, new String[] {"source"});
		form.setCwId(contract.getCommonWork().getId());
		form.setSource(contract.getSource());
		form.setSourceId(contract.getSource().getId());
		mv.addObject(FORM_MODEL_NAME, form);
		return mv;
	}

	@RequestMapping(value = "/permissions/paymentrequest/managePaymentRequest", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView managePaymentRequest(@ModelAttribute(FORM_MODEL_NAME) PaymentRequestForm form,
			HttpServletRequest request) throws Exception
	{
		log.debug("managePaymentRequest(): entered...");

		Integer userId = PermUserContext.getCurrentUserId(request);
		User user = getUserRepository().loadUserById(userId);
		UserDefaults defaults = user.getUserDefaults();

		if (null != defaults) {
			form.setCostCenter(defaults.getCostCenter());

			if (null != defaults.getAccount()) {
				form.setAccountNumberAndSubCode(defaults.getAccount().getAccountNumberSubCode());
			}
		}

		ModelAndView mv = new ModelAndView("pages.paymentrequest.manage");
		mv.addObject(FORM_MODEL_NAME, form);
		return mv;
	}

	@RequestMapping(value = "/permissions/paymentrequest/managePaymentRequest", method = RequestMethod.POST)
	public void submitPaymentRequest(HttpServletRequest request, HttpServletResponse response,
			@ModelAttribute(FORM_MODEL_NAME) PaymentRequestForm form) throws Exception
	{
		log.debug("submitPaymentRequest(): entered...");

		String costCenter = form.getCostCenter();
		String accountNumber = form.getAccountNumber();
		String subCode = form.getSubCode();
		//Added for SS Task
		int checkNumber = form.getCheckNumber();
		//Ends
		Account account = getContractService().getAccountByNumberAndSubCode(accountNumber, subCode);

		// log current user id in case user tries to create a duplicate payment request
		Integer currentUserId = PermUserContext.getCurrentUserId(request);
		log.debug("submitPaymentRequest(): currentUserId: " + currentUserId);
		log.debug("submitPaymentRequest(): Payment type / date: " + form.getPaymentType() + " / " + form.getPaymentDate() );

		Contract contract = contractRepository.lazyLoad(Contract.class, form.getContractId(), new String[] {"assets"});

		PaymentRequest paymentRequest = getContractService().payContract (form.getContractId(),
				form.getPaymentType(), form.getPaymentDate(),
				account, costCenter, checkNumber);

		// create the JSON object to be sent to the dialog
		Map<String, Object> map = new HashMap <String, Object> ();

		// load all asset use ids for the assets attached to contract so we can refresh them in data table
		List<ContractAsset> assets = contract.getAssets();
		List<Integer> aids = new ArrayList<Integer> ();
		for (ContractAsset asset : assets) {
			aids.add(asset.getAssetBaseId());
		}
		List<Integer> auids = new ArrayList<Integer> ();
		for (Integer aid : aids) {
			List<Integer> ids = assetUseRepository.loadAssetUseIdsForAssetId(aid);
			auids.addAll(ids);
		}
		map.put("auids", auids);
		if (form.getPaymentType().equals(PaymentType.PAYMENT)) {
			map.put("url", request.getContextPath() + "/sapp/permissions/paymentrequest.xls" + "?prId="
					+ paymentRequest.getId());
		} else {
			map.put("url", StringUtils.EMPTY);
		}

		String output = ObjectToJson.doTransform(map, null);  // throws InitialisationException, TransformationException

		log.debug("save(): sending:\r\n" + output);

		response.setContentType("application/json");
		// Don't set contentLength because String.length() is not the same as #bytes if there are double-byte chars
		//response.setContentLength(output.length());
		PrintWriter writer = response.getWriter();  // throws IOException
		writer.write(output);
	}

	@RequestMapping(value = "/permissions/paymentrequest/paid_view", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView paidView(@RequestParam("auId") int auId) {
		log.debug("paidView(): entered, auId = " + auId);
		ModelAndView mv = new ModelAndView("pages.paymentrequest.paidView");
		List<Contract> contractList = contractRepository.getPaidContractsForAssetUse(auId);
		mv.addObject("contracts", contractList);
		return mv;
	}

	@RequestMapping(value = "/permissions/paymentrequest/paid_view_contract", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView paidViewContract(@RequestParam("contractId") int contractId) throws Exception {
		log.debug("paidViewContract(): entered, contractId = " + contractId);
		ModelAndView mv = new ModelAndView("pages.paymentrequest.paidView");
		Contract contract = contractRepository.lazyLoad(Contract.class, contractId, new String[] {"source"});
		List<Contract> contractList = new ArrayList<Contract>(1);
		contractList.add(contract);
		mv.addObject("contracts", contractList);
		return mv;
	}

	/*@GetMapping("/viewPaymentRequest")*/
	@RequestMapping(value = "/permissions/paymentrequest/viewPaymentRequest", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView viewPaymentRequest(
			@RequestParam("paymentRequestId") Integer paymentRequestId)
	throws PersistenceException
	{
		log.debug("viewPaymentRequest(): paymentRequestId = " + paymentRequestId);
		PaymentRequest paymentRequest = contractService.loadPaymentRequestView(paymentRequestId);

		ModelAndView modelAndView = new ModelAndView(getPaymentRequestView());

		modelAndView.addObject("paymentRequest", paymentRequest);

		return modelAndView;
	}

	/*@GetMapping("/list")*/
	@RequestMapping(value = "/permissions/paymentrequest/list", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView list(HttpServletRequest request,
			@RequestParam(value="sourceId", required=false) Integer sourceId,
			@RequestParam(value="assetId", required=false) Integer assetId
	) throws PersistenceException
	{
		log.debug("list(): sourceId = " + sourceId + ", assetId = " + assetId);
		CommonWork cw = PermUserContext.getCurrentCommonWork(request);

		ModelAndView mv = new ModelAndView(getListPaymentRequestView());

		List<PaymentRequest> paymentRequests = contractService.loadPaymentRequestListView(cw.getId());

		mv.addObject("commonWork", cw);
		mv.addObject("paymentRequests", paymentRequests);

		return mv;
	}

	/*@GetMapping("/modifyPaymentRequest")*/
	@RequestMapping(value = "/permissions/paymentrequest/modifyPaymentRequest", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView modifyPaymentRequest(
			@RequestParam("paymentRequestId") Integer paymentRequestId,
			@RequestParam(value="paid", required=false) boolean paid)
	throws PersistenceException, ServiceException
	{
		log.debug("modifyPaymentRequest(): paymentRequestId = " + paymentRequestId + ", paid = " + paid);

		PaymentRequest paymentRequest = contractService.loadPaymentRequestView(paymentRequestId);

		paymentRequest.setPaid(paid);

		paymentRequest = contractRepository.saveRequiresNew(paymentRequest);

		ModelAndView modelAndView = new ModelAndView(getRedirectListPaymentRequestView());

		return modelAndView;
	}

	/*@GetMapping("/listSourcesToPay")*/
	@RequestMapping(value = "/permissions/paymentrequest/listSourcesToPay", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView listSourcesToPay(HttpServletRequest request)
		throws Exception
	{
		log.debug("listSourcesToPay(): entered...");
		CommonWork cw = PermUserContext.getCurrentCommonWork(request);

		List<Contract> contractWOPaymentRequest = new ArrayList<Contract>();

	//	List<Contract> contracts = getContractRepository().loadContractsByCWId(cw.getId());
		List<Contract> contracts = getContractService().loadContractListView (cw.getId());

		for (Contract contract : contracts) {
			if (contract.getPaymentRequest() == null) {
				if (!contractWOPaymentRequest.contains(contract)) {
					contractWOPaymentRequest.add(contract);
				}
			}
		}

		ModelAndView modelAndView = new ModelAndView(getShowSourcesViewName());

		if (contractWOPaymentRequest.size() > 0) {
			modelAndView.addObject("sourceToAssetData", contractWOPaymentRequest);
		}
		else {
			String msg = getMessageSource().getMessage("paymentRequest.no.assets.for", null, null);
			modelAndView.setViewName(getNoGoViewName() + "?generalMessage=" + msg);
		}

		modelAndView.addObject("commonWork", cw);

		return modelAndView;
	}


	public String getListPaymentRequestView() {
		return listPaymentRequestView;
	}

	public void setListPaymentRequestView(String listPaymentRequestView) {
		this.listPaymentRequestView = listPaymentRequestView;
	}

	public String getPaymentRequestView() {
		return paymentRequestView;
	}

	public void setPaymentRequestView(String paymentRequestView) {
		this.paymentRequestView = paymentRequestView;
	}

	public String getRedirectListPaymentRequestView() {
		return redirectListPaymentRequestView;
	}

	public void setRedirectListPaymentRequestView(String redirectListPaymentRequestView) {
		this.redirectListPaymentRequestView = redirectListPaymentRequestView;
	}

	public ContractService getContractService() {
		return contractService;
	}

	public void setContractService(ContractService contractService) {
		this.contractService = contractService;
	}

	public ContractRepository getContractRepository() {
		return contractRepository;
	}

	public void setContractRepository(ContractRepository contractRepository) {
		this.contractRepository = contractRepository;
	}

	public String getNoGoViewName() {
		return noGoViewName;
	}

	public void setNoGoViewName(String noGoViewName) {
		this.noGoViewName = noGoViewName;
	}

	public String getShowSourcesViewName() {
		return showSourcesViewName;
	}

	public void setShowSourcesViewName(String showSourcesViewName) {
		this.showSourcesViewName = showSourcesViewName;
	}

	public UserRepository getUserRepository() {
		return userRepository;
	}

	public void setUserRepository(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public String getManageSuccessView() {
		return manageSuccessView;
	}

	public void setManageSuccessView(String manageSuccessViewName) {
		this.manageSuccessView = manageSuccessViewName;
	}

	public AssetUseRepository getAssetUseRepository() {
		return assetUseRepository;
	}

	public void setAssetUseRepository(AssetUseRepository assetUseRepository) {
		this.assetUseRepository = assetUseRepository;
	}

	public SourceRepository getSourceRepository() {
    	return sourceRepository;
    }

	public void setSourceRepository(SourceRepository sourceRepository) {
    	this.sourceRepository = sourceRepository;
    }
}
