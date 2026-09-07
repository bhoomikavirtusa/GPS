package com.wiley.permissions.web.shared.controllers.landing;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.queryparser.classic.ParseException;
import org.mule.api.lifecycle.InitialisationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.xml.sax.InputSource;

import com.wiley.permissions.common.transformer.ObjectToJson;
import com.wiley.permissions.common.transformer.TransformationException;
import com.wiley.permissions.common.transformer.XmlToObject;
import com.wiley.permissions.domain.persistence.permissions.ConditionNode;
import com.wiley.permissions.domain.persistence.permissions.ConditionType;
import com.wiley.permissions.domain.persistence.permissions.Contract;
import com.wiley.permissions.domain.rightslink.License;
import com.wiley.permissions.domain.rightslink.Licenses;
import com.wiley.permissions.persistence.PersistenceException;
import com.wiley.permissions.repositories.ConditionRepository;
import com.wiley.permissions.repositories.ContractRepository;
import com.wiley.permissions.services.ContractService;
import com.wiley.permissions.services.RightsLinkClient;
import com.wiley.permissions.services.StatusCodeAndContent;
import com.wiley.permissions.services.util.ServiceException;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;


@Controller
/*@RequestMapping("/landing/license")*/
@RequestMapping
public class LicenseController extends BaseAnnotatedController {

	// --------------------- static data ---------------------------------

	private final static Log log = LogFactory.getLog(LicenseController.class);

	private final static String LICENSE_KEY = "rightsLink.license";
	private static final String CONDITIONS_NAME = "licenseConditions";

	// --------------------- instance data -------------------------------
	private ContractService contractService;
	private ContractRepository contractRepository;
	private ConditionRepository conditionRepository;
	private RightsLinkClient rightsLinkClient;

	@Override
	protected List<String> getReferenceDataNames(HttpServletRequest request) throws Exception {
		List<String> output = new ArrayList<String>();

		output.add("currencies");
		output.add("contractTypes");
		return output;
	}

	@RequestMapping(value="/landing/license/load", method = {RequestMethod.GET, RequestMethod.POST})
	public String load(HttpServletRequest request, @RequestParam("cwId") int cwId, Model model) {
		// note using cwId right now (use current cwId in requestLicense below) but could use it
		log.debug("load(): entered, cwId = " + cwId);
		model.addAttribute("cwId", cwId);
		request.getSession().removeAttribute(LICENSE_KEY);
		return "pages.landing.license";
	}

	/*@GetMapping("/editContract")*/
	@RequestMapping(value="/landing/license/editContract", method = {RequestMethod.GET, RequestMethod.POST})
	public String editContract(HttpServletRequest request, Model model) {
		log.debug("editContract(): entered");
		License license = (License) request.getSession().getAttribute(LICENSE_KEY);
		model.addAttribute("license", license);
		return "pages.landing.license.edit.contract";
	}

	/*@GetMapping(value = "/saveContract")*/
	@RequestMapping(value="/landing/license/saveContract", method = {RequestMethod.GET, RequestMethod.POST})
	public String saveContract(HttpServletRequest request, Model model,
			@RequestParam("invoiceDate") Date invoiceDate,
			@RequestParam("creditLine") String creditLine,
			@RequestParam("grantStartDate") Date grantStartDate,
			@RequestParam("grantEndDate") Date grantEndDate,
			@RequestParam("totalAmount") double totalAmount,
			@RequestParam("contractType") String contractType,
			@RequestParam("currency") String currency
		) {
		log.debug("saveContract(): entered");

		License license = (License) request.getSession().getAttribute(LICENSE_KEY);
		license.setContractType(contractType);
		license.setCreditLine(creditLine);
		license.setCurrency(currency);
		license.setGrantEndDate(grantEndDate);
		license.setGrantStartDate(grantStartDate);
		license.setInvoiceDate(invoiceDate);
		license.setTotalAmount(totalAmount);
		model.addAttribute("license", license);
		return "pages.landing.license.view.contract";
	}

	/*@GetMapping("/loadConditions")*/
	@RequestMapping(value="/landing/license/loadConditions", method = {RequestMethod.GET, RequestMethod.POST})
	public void loadConditions(HttpServletRequest request, HttpServletResponse response,
			@RequestParam("contractId") int contractId) throws Exception {
		log.debug("loadConditions(): entered..., contractId = " + contractId);

		List<ConditionNode> conditions = conditionRepository.loadContractConditions(contractId == 0 ? null : contractId, false);
		// save data in session
		request.getSession().setAttribute(CONDITIONS_NAME, conditions);

		// note loadContractConditions() actually builds a map internally and then converts to a list.
		// Here we reconvert to map - might consider code reorg later but would affect other things.
		Map<String, ConditionNode> nodeMap = conditionRepository.nodeListToMap(conditions);

		// Note the map contains top-level nodes with their children attached
		// (the map does not contain the children directly)

		if (contractId == 0) {
			// prefill certain conditions from the license
			License license = (License) request.getSession().getAttribute(LICENSE_KEY);

			ConditionNode otherNode = nodeMap.get(ConditionType.OTHER.getCode());
			// the otherNode should always have exactly one child which is the notesNode
			ConditionNode notesNode = otherNode.getChildren().get(0);
			notesNode.setValue(license.getOtherElements());

			String printRunString = license.getPrintRun();
			if (StringUtils.isNotBlank(printRunString)) {
				try {
					Integer.parseInt(printRunString);  // throws NumberFormatException
					// Can't access nodes we want directly - navigate from top level
					ConditionNode printRunNode = nodeMap.get(ConditionType.PRINT_RUN.getCode());
					ConditionNode printRunLimitNode = printRunNode.getChildWithCode(ConditionType.PRINT_RUN_LIMIT.getCode());
					ConditionNode printRunLimitBoxNode = printRunLimitNode.getChildWithCode(ConditionType.PRINT_RUN_LIMIT_BOX.getCode());
					printRunLimitNode.setValueToTrue();
					printRunLimitBoxNode.setValue(printRunString);
				}
				catch (NumberFormatException ex) {
					log.error("loadConditions(): could not parse printRun value [" + printRunString + "]");
				}
			}

			if (StringUtils.isNotBlank(license.getMedium())) {
				if (license.getMedium().equalsIgnoreCase("print and electronic")
						|| license.getMedium().equalsIgnoreCase("both print and electronic")) {
					// Can't access nodes we want directly - navigate from top level
					ConditionNode mediumNode = nodeMap.get(ConditionType.MEDIUM.getCode());
					ConditionNode mediumPhysicalElectronicNode = mediumNode.getChildWithCode(ConditionType.MEDIUM_PHYSICAL_ELECTRONIC.getCode());
					mediumPhysicalElectronicNode.setValueToTrue();
				}
				else {
					log.info("loadConditions(): did not map medium value [" + license.getMedium() + "]");
				}
			}

			// since we've changed some values, need to recalc rollup values
			conditionRepository.calculateRollupValues(conditions);
		}

		// manually hide a bunch of nodes (override normal contract settings)
		ConditionType [] toHide = { ConditionType.SUBLICENSE };
		for (ConditionType ct : toHide) {
			ConditionNode node = nodeMap.get(ct.getCode());
			node.setCanEdit(false);
			node.setCanSee(false);
		}

		String json = ObjectToJson.doTransform(conditions, null);
		log.debug("loadConditions(): json: " + json);

        response.setContentType("application/json");
        PrintWriter writer = response.getWriter();  // throws IOException
        writer.println(json);
	}

	/**
	 * This is almost exactly the same as ContractWizardController.postNode().
	 * Perhaps should refactor.
	 */
	/*@GetMapping("/postNode")*/
	@RequestMapping(value="/landing/license/postNode", method = {RequestMethod.GET, RequestMethod.POST})
	public void postNode(HttpServletRequest request,
			@RequestParam(value = "parent") String node, HttpServletResponse response) throws Exception {
		log.debug("postNode(): entered...parent: " + node);
		// load data from session
		@SuppressWarnings("unchecked")
		List<ConditionNode> conditions = (List<ConditionNode>) request.getSession().getAttribute(CONDITIONS_NAME);
		ConditionNode cnode = ConditionControllerUtils.refreshParentNode(request, conditions, node);

		response.setContentType("text/plain");
		PrintWriter writer = response.getWriter(); // throws IOException
		// cannot write null to the writer - throws Exception
		String rollup = (null == cnode.getRollupValue() ? "" : cnode.getRollupValue());
		writer.write(rollup);
	}

	/*@GetMapping("/request")*/
	@RequestMapping(value="/landing/license/request", method = {RequestMethod.GET, RequestMethod.POST})
	public String requestLicense (HttpServletRequest request,
		@RequestParam("licenseNumber") String licenseNumber,
		@RequestParam("cwId") int cwId,
		Model model)
		throws ServiceException, PersistenceException, ParseException, IOException, InitialisationException, TransformationException
	{
		licenseNumber = licenseNumber.trim();
		log.debug("requestLicense(): entered, licenseNumber [" + licenseNumber + "] cwId [" + cwId + "]");

		model.addAttribute("cwId", cwId);
		model.addAttribute("contractId", 0);  // means no existing contract

		// if error, will display the license page with the error
		String errorViewName = "pages.landing.license";
		String successViewName = "pages.landing.license.review";

		// client side checks for blank but not for digits so check that here (always good to double check on server side anyway)
		if (!NumberUtils.isDigits(licenseNumber)) {
			model.addAttribute("licenseRequestError", "license number [" + licenseNumber + "] is not valid - must contain only digits.");
			return errorViewName;
		}

		// from experimentation, RightsLink sends back 500 (Internal Server Error) for
		// license number longer than 19 digits so also check that here to give better error message
		// - client side now limits input to 19 digits but keep this check
		if (licenseNumber.length() > 19) {
			model.addAttribute("licenseRequestError", "license number [" + licenseNumber + "] is not valid - limit of 19 digits.");
			return errorViewName;
		}

		// Before get the license make sure that no other RightsLink contract in the system (globally)
		// already was created from this license number.
		Number count = (Number) contractRepository.executeSingleResultNamedQuery(
			"Contract.countContractsWithRightsLinkLicense", new Object[] { licenseNumber});
		log.debug("requestLicense(): existingCount = " + count);
		if (count.intValue() > 0) {
			model.addAttribute("licenseRequestError", "license number [" + licenseNumber + "] has already been used.");
			return errorViewName;
		}

		try {
			StatusCodeAndContent scac = rightsLinkClient.getLicenseXml(licenseNumber);
			if (scac.getStatusCode() == 404) {
				log.debug("requestLicense(): got 404 (not found) for license number [" + licenseNumber + "]");
				model.addAttribute("licenseRequestError", "License number [" + licenseNumber + "] not found.");
				return errorViewName;
			}
			else if (scac.getStatusCode() != 200) {
				log.debug("requestLicense(): got non-200 for license number [" + licenseNumber + "]");
				model.addAttribute("licenseRequestError", "Unexpected error for License number [" + licenseNumber + "]");
				return errorViewName;
			}
			else {
				String licenseXml = scac.getContent();
				Licenses licenses = (Licenses) XmlToObject.xmlToObject(Licenses.class, licenseXml);
				License license = licenses.getLicenses().get(0);
				license.setXml(formatXml(licenseXml));
				model.addAttribute("license", license);
				model.addAttribute("assetCount", license.getAssetsCount());
				request.getSession().setAttribute(LICENSE_KEY, license);
			}
		} catch (Exception ex) {
			log.debug ("requestLicense(): failed to request a license object for number [" + licenseNumber + "]", ex);
			model.addAttribute("licenseRequestError", ex.toString());
			return errorViewName;
		}
		// success page
		return successViewName;
	}

	/*@GetMapping("/edit")*/
	@RequestMapping(value="/landing/license/edit", method = {RequestMethod.GET, RequestMethod.POST})
	public String edit (HttpServletRequest request,
		@RequestParam("contractId") int contractId,
		Model model)
		throws Exception
	{
		log.debug("edit(): entered..., contractId [" + contractId + "]");
		Contract contract = contractRepository.lazyLoad(Contract.class, contractId,
				new String[] { "assets", "commonWork", "contractType", "currency" });

		model.addAttribute("cwId", contract.getCommonWork().getId());
		model.addAttribute("contractId", contract.getId());
		model.addAttribute("assetCount", contract.getAssets().size());

		String licenseXml = contract.getLicenseXml();
		Licenses licenses = (Licenses) XmlToObject.xmlToObject(Licenses.class, licenseXml);
		License license = licenses.getLicenses().get(0);
		license.load(contract);
		log.debug("edit(): " + license.getXml());
		model.addAttribute("license", license);
		request.getSession().setAttribute(LICENSE_KEY, license);

		return "pages.landing.license.review";
	}

	/*@GetMapping("/save")*/
	@RequestMapping(value="/landing/license/save", method = {RequestMethod.GET, RequestMethod.POST})
	public String save(HttpServletRequest request,
			@RequestParam("cwId") int cwId,
			@RequestParam("numAssets") int numAssets,
			@RequestParam("contractId") int contractId,  // 0 means new contract
			Model model) throws Exception {
		log.debug("save(): entered...");
		@SuppressWarnings("unchecked")
		List<ConditionNode> conditions = (List<ConditionNode>) request.getSession().getAttribute(CONDITIONS_NAME);

		License license = (License) request.getSession().getAttribute(LICENSE_KEY);
		request.getSession().removeAttribute(LICENSE_KEY);
		String result = "success";
		log.debug("save(): license " + license);
		try {
			List<Integer> refreshAuIds = new ArrayList<Integer>();
			List<Integer> newAuIds = contractService.saveLicense(license, cwId, conditions, numAssets, contractId, refreshAuIds);
			// set null if empty (no new assets)
			String newAuIdsString = CollectionUtils.isEmpty(newAuIds) ? null : StringUtils.join(newAuIds, ",");
			String refreshAuIdsString = StringUtils.join(refreshAuIds, ",");
			// client side will refresh table for these newAuIds and refreshAuIds
			log.debug("newIds : " + newAuIdsString);
			log.debug("refreshAuIds : " + refreshAuIdsString);
			model.addAttribute("newAuIds", newAuIdsString);
			model.addAttribute("refreshAuIds", refreshAuIdsString);
		}
		catch (Exception ex) {
			log.debug("save(): failed to save license for number [" + license.getInvoiceNumber() + "]", ex);
			result = "Error: " + ex.toString();
		}
		model.addAttribute("licenseResult", result);

		return "pages.landing.license.result";
	}

	public static String formatXml(String sourceXml) {
		try {
			Transformer serializer = SAXTransformerFactory.newInstance().newTransformer();
			serializer.setOutputProperty(OutputKeys.INDENT, "yes");
			// serializer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION,// "yes");
			serializer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

			// serializer.setOutputProperty("{http://xml.customer.org/xslt}indent-amount", // "2");
			InputStream inputStream = new ByteArrayInputStream(sourceXml.getBytes("UTF-8"));

			InputSource is = new InputSource(inputStream);
			is.setEncoding("UTF-8");
			Source xmlSource = new SAXSource(is);
			StreamResult res = new StreamResult(new ByteArrayOutputStream());

			serializer.transform(xmlSource, res);

			return new String(((ByteArrayOutputStream) res.getOutputStream()).toByteArray());
		} catch (Exception e) {
			log.debug(e);
			return sourceXml;
		}
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

	public ConditionRepository getConditionRepository() {
		return conditionRepository;
	}

	public void setConditionRepository(ConditionRepository conditionRepository) {
		this.conditionRepository = conditionRepository;
	}

	public RightsLinkClient getRightsLinkClient() {
		return rightsLinkClient;
	}

	public void setRightsLinkClient(RightsLinkClient rightsLinkClient) {
		this.rightsLinkClient = rightsLinkClient;
	}
}
