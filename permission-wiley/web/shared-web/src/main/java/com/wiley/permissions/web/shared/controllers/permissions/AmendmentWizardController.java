package com.wiley.permissions.web.shared.controllers.permissions;

import java.io.PrintWriter;
import java.sql.Connection;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import net.sf.jasperreports.engine.JRParameter;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import com.wiley.permissions.common.transformer.ObjectToJson;
import com.wiley.permissions.domain.persistence.permissions.Amendment;
import com.wiley.permissions.domain.persistence.permissions.AssetUse;
import com.wiley.permissions.domain.persistence.permissions.CommonWork;
import com.wiley.permissions.domain.persistence.permissions.Contract;
import com.wiley.permissions.domain.persistence.permissions.ContractAsset;
import com.wiley.permissions.domain.persistence.permissions.EnumLanguage;
import com.wiley.permissions.repositories.AssetUseRepository;
import com.wiley.permissions.repositories.CommonWorkRepository;
import com.wiley.permissions.repositories.ContractRepository;
import com.wiley.permissions.services.ContractService;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;
import com.wiley.permissions.web.shared.controllers.UploadForm;
import com.wiley.permissions.web.shared.controllers.reports.PdfReportView;

@Controller
/*@RequestMapping("/permissions/amendment")*/
@RequestMapping
@SessionAttributes(AmendmentWizardController.FORM_MODEL_NAME)
public class AmendmentWizardController extends BaseAnnotatedController {

	// --------------------- static data ---------------------------------

	private final static Log log = LogFactory.getLog(AmendmentWizardController.class);
	protected final static String FORM_MODEL_NAME = "amendmentWizardForm";

	// --------------------- instance data -------------------------------

	private AssetUseRepository assetUseRepository;
	private ContractRepository contractRepository;
	private ContractService contractService;
	private CommonWorkRepository commonWorkRepository;

	private PdfReportView pdfView;
	private DataSource sourceDataSource;

	// default can be overridden in Spring config
	private final long maximumFileSize = 10 * 1024 * 1024;

	@Override
	protected List<String> getReferenceDataNames(HttpServletRequest request) throws Exception
	{
		List<String> output = new ArrayList<String>();

		output.add("languages");
		return output;
	}

	@RequestMapping(value = "/permissions/amendment/asset_start", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView assetStart(@RequestParam(value = "auId") Integer auId)
			throws Exception
	{
		log.debug("assetStart(): entered..., auId = " + auId);
		AssetUse au = getAssetUseRepository().loadAssetUseById(auId);
		ModelAndView mv = null;
		// load minimum information here. If you need extra information,
		// load it on the specific branch (see authorProvided example)
		AmendmentWizardForm form = new AmendmentWizardForm();
		form.setAuId(auId);
		List<Contract> contracts = contractRepository.loadContractsThatNeedAmendment(au.getCommonWork().getId(), 0, au.getAsset().getId());
		log.debug("assetStart(): contracts: " + contracts);
		if (CollectionUtils.isEmpty(contracts)) {
			mv = new ModelAndView("dialog.success");
			mv.addObject("message", "No contracts found");
		} else if (contracts.size() == 1) {
			mv = new ModelAndView("redirect:/sapp/permissions/amendment/select_contract?cid=" + contracts.get(0).getId());
			form.setNextUrl("/sapp/permissions/amendment/details");
			form.setPrevUrl("");
		} else {
			mv = new ModelAndView("pages.amendment.contractlist");
			form.setPrevUrl("/sapp/permissions/amendment/asset_start?auId=" + auId);
			form.setNextUrl("/sapp/permissions/amendment/details");
			mv.addObject("contracts", contracts);
		}
		mv.addObject(FORM_MODEL_NAME, form);
		return mv;
	}

	@RequestMapping(value = "/permissions/amendment/select_contract", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView selectContract(@RequestParam(value = "cid") Integer contractId,
			@ModelAttribute(FORM_MODEL_NAME) AmendmentWizardForm form)
			throws Exception
	{
		log.debug("selectContract(): entered..., contractId = " + contractId);

		ModelAndView mv = new ModelAndView("redirect:" + form.getNextUrl());
		Contract contract = contractRepository.lazyLoad(Contract.class, contractId, new String[] {"assets"});
		if (null != contract.getAmendment())
			form.setAmendment(contract.getAmendment());
		form.getAmendment().setContract(contract);
		form.setCwId(contract.getCommonWork().getId());
		List<ContractAsset> cas = contract.getAssets();
		List<Integer> aids = new ArrayList<Integer> ();
		for (ContractAsset ca : cas)
			aids.add(ca.getAssetBaseId());
		form.setAssetIds(aids);

		mv.addObject(FORM_MODEL_NAME, form);
		return mv;
	}

	@RequestMapping(value = "/permissions/amendment/details", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView detailsLoad(@ModelAttribute(FORM_MODEL_NAME) AmendmentWizardForm form)
			throws Exception
	{
		log.debug("detailsLoad(): entered...");
		ModelAndView mv = new ModelAndView("pages.amendment.details");
		return mv;
	}

	@RequestMapping(value = "/permissions/amendment/save", method = RequestMethod.POST)
	public void save(HttpServletRequest request, HttpServletResponse response,
			@ModelAttribute(FORM_MODEL_NAME) AmendmentWizardForm form)
	throws Exception
	{
		log.debug("save(): entered...");

		Amendment amendment = form.getAmendment();
		amendment = contractService.save(amendment);

		// create the JSON object to be sent to the dialog
		Map<String, Object> map = new HashMap <String, Object> ();

		// load all asset use ids for the assets attached to purchase order so we can refresh them in data table
		List<Integer> aids = form.getAssetIds();
		log.debug("save(): asset ids = " + aids);
		List<Integer> auids = new ArrayList<Integer> ();
		for (Integer aid : aids) {
			List<Integer> ids = assetUseRepository.loadAssetUseIdsForAssetIdAndCwId(aid, form.getCwId());
			auids.addAll(ids);
		}
		map.put("auids", auids);
		map.put("url", request.getContextPath() + "/sapp/permissions/amendment/preview?auId=" + form.getAuId());
		String output = ObjectToJson.doTransform(map, null);  // throws InitialisationException, TransformationException

		log.debug("save(): sending:\r\n" + output);

		response.setContentType("application/json");
		// Don't set contentLength because String.length() is not the same as #bytes if there are double-byte chars
		//response.setContentLength(output.length());
		PrintWriter writer = response.getWriter();  // throws IOException
		writer.write(output);
	}

	@RequestMapping(value = "/permissions/amendment/upload_start", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView uploadStart(@RequestParam(value = "auId") Integer auId)
			throws Exception
	{
		log.debug("uploadStart(): entered, auId = " + auId);
		AssetUse au = getAssetUseRepository().loadAssetUseById(auId);
		ModelAndView mv = null;
		// load minimum information here. If you need extra information,
		// load it on the specific branch (see authorProvided example)
		AmendmentWizardForm form = new AmendmentWizardForm();
		form.setAuId(auId);
		List<Contract> contracts = contractRepository.loadContractsWithAmendment(au.getCommonWork().getId(), 0, au.getAsset().getId());
		log.debug("uploadStart(): contracts: " + contracts);
		if (CollectionUtils.isEmpty(contracts)) {
			mv = new ModelAndView("dialog.success");
			mv.addObject("message", "No contracts found");
		} else if (contracts.size() == 1) {
			mv = new ModelAndView("redirect:/sapp/permissions/amendment/select_contract?cid=" + contracts.get(0).getId());
			form.setNextUrl("/sapp/permissions/amendment/upload");
			form.setPrevUrl("");
		} else {
			mv = new ModelAndView("pages.amendment.contractlist");
			form.setPrevUrl("/sapp/permissions/amendment/asset_start?auId=" + auId);
			form.setNextUrl("/sapp/permissions/amendment/upload");
			mv.addObject("contracts", contracts);
		}
		mv.addObject(FORM_MODEL_NAME, form);
		return mv;
	}

	@RequestMapping(value = "/permissions/amendment/upload", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView uploadLoad(@ModelAttribute(FORM_MODEL_NAME) AmendmentWizardForm form)
			throws Exception
	{
		log.debug("uploadLoad(): entered...");
		ModelAndView mv = new ModelAndView("pages.amendment.upload");
		return mv;
	}

	@GetMapping(value = "/permissions/amendment/upload_file")
	public void uploadFile(HttpServletRequest request, HttpServletResponse response,
			@ModelAttribute(UploadForm.FORM_MODEL_NAME) UploadForm form)
			throws Exception
	{
		log.debug("uploadFile(): entered...");
		MultipartFile mpf = form.getFile();
		AmendmentWizardForm aForm = (AmendmentWizardForm) request.getSession().getAttribute(FORM_MODEL_NAME);

		if (mpf != null && mpf.getSize() > 0) {
			PrintWriter writer = response.getWriter(); // throws IOException
			response.setContentType("text/plain");
			
			if (mpf.getSize() <= maximumFileSize) {
				aForm.getAmendment().setData(mpf.getBytes());
				aForm.getAmendment().setFileName(mpf.getOriginalFilename());
				aForm.getAmendment().setMimeType(mpf.getContentType());

				log.debug("uploadFile(): File has " + mpf.getSize() + " bytes");

				writer.write("File " + mpf.getOriginalFilename() + " (" + mpf.getSize() + " bytes) uploaded");
			}
			else if (mpf.getSize() > maximumFileSize) {
				NumberFormat intFormat = NumberFormat.getIntegerInstance();
				log.info("uploadFile(): File TOO LARGE - has " + mpf.getSize() + " bytes");
				writer.write("File too large. Maximum size is " + intFormat.format(maximumFileSize) + " bytes.");
			}
		}
	}

	@RequestMapping(value = "/permissions/amendment/submit_upload", method = RequestMethod.POST)
	public void submitUpload(HttpServletRequest request, HttpServletResponse response,
			@ModelAttribute(FORM_MODEL_NAME) AmendmentWizardForm form)
	throws Exception
	{
		log.debug("submitUpload(): entered...");

		Amendment amendment = form.getAmendment();
		amendment = contractService.save(amendment);

		// create the JSON object to be sent to the dialog
		Map<String, Object> map = new HashMap <String, Object> ();

		// load all asset use ids for the assets attached to purchase order so we can refresh them in data table
		List<Integer> aids = form.getAssetIds();
		log.debug("submitUpload(): asset ids = " + aids);
		List<Integer> auids = new ArrayList<Integer> ();
		for (Integer aid : aids) {
			List<Integer> ids = assetUseRepository.loadAssetUseIdsForAssetIdAndCwId(aid, form.getCwId());
			auids.addAll(ids);
		}
		map.put("auids", auids);
		map.put("url", "");
		String output = ObjectToJson.doTransform(map, null);  // throws InitialisationException, TransformationException

		log.debug("save(): sending:\r\n" + output);

		response.setContentType("application/json");
		// Don't set contentLength because String.length() is not the same as #bytes if there are double-byte chars
		//response.setContentLength(output.length());
		PrintWriter writer = response.getWriter();  // throws IOException
		writer.write(output);
	}

	@RequestMapping(value = "/permissions/amendment/pdf", method = {RequestMethod.GET, RequestMethod.POST})
	public void generatePdf(HttpServletRequest request, HttpServletResponse response,
			@RequestParam(value = "auId") int auId,
			@RequestParam(value="download", required=false) boolean download)
	throws Exception
	{
		log.debug("generatePdf(): entered...");
		Connection conn = null;
		AssetUse au = getAssetUseRepository().loadAssetUseById(auId);
		List<Contract> contracts = contractRepository.loadContractsWithAmendment (au.getCommonWork().getId(), 0, au.getAsset().getId());
		// we should have just one contract per asset per source. Anyway, we select the first one
		if (CollectionUtils.isEmpty(contracts)) {
			log.debug("generatePdf(): No contracts found");
			return;
		}
		Contract contract = contracts.get(0);
		// lazy load source here because is not initially loaded
		contract = contractRepository.lazyLoad(Contract.class, contract.getId(), new String[] {"source"});
		CommonWork cw = commonWorkRepository.lazyLoad(CommonWork.class, au.getCommonWork().getId(), new String [] {"products"});
		try	{
			conn = getSourceDataSource().getConnection();

			HashMap<String, Object> model = new HashMap<String, Object>();
			model.put("PRODUCT_ID", cw.getPrimaryProduct().getId());
			model.put("CONTRACT_ID", contract.getId());
			model.put("CW_ID", cw.getId());
			model.put("SOURCE_CONNECTION", conn);

			response.setContentType ("application/pdf");
			Properties headers = new Properties();
			if (download) {
				String name = "Amendment " + contract.getSource().getName() + ".pdf";
	            headers.put("Content-Disposition", "attachment;filename=\"" + name + "\"");
			} else {
	            headers.put("Content-Disposition", "inline");
			}
			getPdfView().setHeaders(headers);

			Locale locale = EnumLanguage.getLocale(contract.getAmendment().getLanguageCode());
			model.put (JRParameter.REPORT_LOCALE, locale);

			// Must call render right here in the controller because
			// if return the view and wait for Spring to render it,
			// then we loose the db Connection
            // return new ModelAndView(getPdfView(), model);
			getPdfView().render(model, request, response);
		}
		finally {
			if (null != conn) conn.close();
		}
	}

	public PdfReportView getPdfView() {
		return pdfView;
	}

	public void setPdfView(PdfReportView pdfView) {
		this.pdfView = pdfView;
	}

	public DataSource getSourceDataSource() {
		return sourceDataSource;
	}

	public void setSourceDataSource(DataSource sourceDataSource) {
		this.sourceDataSource = sourceDataSource;
	}

	public AssetUseRepository getAssetUseRepository() {
		return assetUseRepository;
	}

	public void setAssetUseRepository(AssetUseRepository assetUseRepository) {
		this.assetUseRepository = assetUseRepository;
	}

	public ContractRepository getContractRepository() {
		return contractRepository;
	}

	public void setContractRepository(ContractRepository contractRepository) {
		this.contractRepository = contractRepository;
	}

	public ContractService getContractService() {
		return contractService;
	}

	public void setContractService(ContractService contractService) {
		this.contractService = contractService;
	}

	public CommonWorkRepository getCommonWorkRepository() {
		return commonWorkRepository;
	}

	public void setCommonWorkRepository(CommonWorkRepository commonWorkRepository) {
		this.commonWorkRepository = commonWorkRepository;
	}
}
