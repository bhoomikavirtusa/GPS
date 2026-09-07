package com.wiley.permissions.web.shared.controllers.asset;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import com.wiley.permissions.domain.persistence.permissions.Asset;
import com.wiley.permissions.domain.persistence.permissions.AssetFile;
import com.wiley.permissions.domain.persistence.permissions.AssetUse;
import com.wiley.permissions.domain.persistence.permissions.RenditionType;
import com.wiley.permissions.repositories.AssetRepository;
import com.wiley.permissions.repositories.AssetUseRepository;
import com.wiley.permissions.services.AssetService;
import com.wiley.permissions.services.AssetUseService;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;
import com.wiley.permissions.web.shared.controllers.UploadForm;


/*@RequestMapping(value={"/asset/main", "/asset/main/custom", "/asset/main/view"})*/
@RequestMapping
@SessionAttributes(value={AssetNavigationController.FORM_MODEL_NAME})
public class AssetNavigationController extends BaseAnnotatedController {

	private static final Log log = LogFactory.getLog(AssetNavigationController.class);

	// when compile on command line requires this not to be private or won't compile
	// (since used above class definition)
	protected static final String FORM_MODEL_NAME = "assetMainForm";
	private final long maximumAssetFileSize = 10 * 1024 * 1024;

	private AssetUseRepository assetUseRepository;
	private AssetRepository assetRepository;
	private AssetService assetService;
	private AssetUseService assetUseService;

	/*@RequestMapping("/first")*/
	@RequestMapping(value={"/asset/main/first", "/asset/main/custom/first", "/asset/main/view/first"}, method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView first(HttpServletRequest request,
			@RequestParam(value = "auIds") String ids) throws Exception
	{
		log.debug("first(): entered...");



		AssetNavigationForm form = new AssetNavigationForm();

		String[] aIds = StringUtils.split(ids, ",");
		form.setAUIds (aIds);
		// reset the session on first
		request.getSession().setAttribute(CustomAssetController.FORM_MODEL_LIST, null);

		ModelAndView mv = new ModelAndView (getSuccessView() + "?auId=" + form.getId());
		mv.addObject(FORM_MODEL_NAME, form);
		return mv;
	}


	/*@GetMapping("/upload")*/
	@RequestMapping(value={"/asset/main/upload", "/asset/main/custom/upload", "/asset/main/view/upload"}, method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView upload(HttpServletRequest request,
			@RequestParam(value = "auIds") String ids) throws Exception
	{
		log.debug("upload(): entered..."+ids);

		AssetNavigationForm form = new AssetNavigationForm();

		String[] aIds = StringUtils.split(ids, ",");
		form.setAUIds (aIds);
		// reset the session on first
		//request.getSession().setAttribute(CustomAssetController.FORM_MODEL_LIST, null);
		AssetUse assetUse = null;
		ModelAndView mv = new ModelAndView (getFormView());
		// Added for Thumbnail Display - Start
		mv.addObject("auIds", ids);
		// Added for Thumbnail Display - End
		mv.addObject(FORM_MODEL_NAME, form);
		return mv;
	}

	/*@GetMapping(value = "/wizard_upload_file")*/
	@RequestMapping(value={"/asset/main/wizard_upload_file", "/asset/main/custom/wizard_upload_file", "/asset/main/view/wizard_upload_file"}, method = {RequestMethod.GET, RequestMethod.POST})
	public void wizardUploadFile(HttpServletRequest request, HttpServletResponse response,
			@ModelAttribute(UploadForm.FORM_MODEL_NAME) UploadForm form)
			throws Exception {
		log.debug("wizardUploadFile(): entered...");


		MultipartFile mpf = form.getFile();
		AssetNavigationForm aForm = (AssetNavigationForm) request.getSession().getAttribute(FORM_MODEL_NAME);
		log.debug("AssetNavigationForm..."+aForm.getId());

		AssetUse assetUse = aForm.getAssetUse();

		assetUse = assetUseRepository.loadAssetUseById(Integer.parseInt(aForm.getId()));

		log.debug("AssetNavigationForm...assetuse"+assetUse);
		Asset asset = assetUse.getAsset();
		log.debug("AssetNavigationForm...asset"+asset.getId());
		//assetUse = getAssetUseService().saveAssetUse(assetUse);
		asset = assetUse.getAsset();
		//asset = getAssetRepository().loadAssetById(asset.getId());
		log.debug("AssetNavigationForm...asset after "+asset.getId());
		if (mpf != null && mpf.getSize() > 0) {
			PrintWriter writer = response.getWriter(); // throws IOException
			response.setContentType("text/plain");
			log.debug("AssetNavigationForm...inside big if");
			if (mpf.getSize() <= maximumAssetFileSize) {
				AssetFile assetfile = new AssetFile();
				log.debug("AssetNavigationForm...inside small if");
				//newFile.setFileData(mpf.getBytes());
				//newFile.setFileName(mpf.getOriginalFilename());
				//newFile.setMimeType(mpf.getContentType());
				//newFile.setContract(cForm.getContract());
				//newFile.setDescription("Asset file");
				//cForm.getContract().getFiles().add(newFile);

				assetfile.setAsset(asset);
				assetfile.setObjectName(mpf.getOriginalFilename());
				assetfile.setFileFormat(mpf.getContentType());
				assetfile.setData(mpf.getBytes());
				assetfile.setRenditionType(RenditionType.ORIGINAL);
				log.debug("AssetNavigationForm...before asset service "+assetfile);
				getAssetService().saveAssetFile(assetfile);

				// Added for Thumbnail Display - Start
				// need to make sure to update index with asset file
				try {
					// reload or else get lazy init exception
					asset = getAssetRepository().loadAssetById(asset.getId());
					log.debug("AssetNavigationForm...asset "+asset);
					assetUseService.updateStatusForAsset(asset);
				}
				catch (Exception ex) {
					log.error("Caught exception trying to update Permission status for Asset: ", ex);
				}
				// Added for Thumbnail Display - Start

				log.debug("wizardUploadFile(): File Has " + mpf.getSize() + " bytes");
				writer.write("File " + mpf.getOriginalFilename() + " (" + mpf.getSize() + " bytes) uploaded");
			}
			else {
				log.info("wizardUploadFile(): File TOO LARGE - has " + mpf.getSize() + " bytes");
				writer.write("File too large. Maximum size is " + maximumAssetFileSize + " bytes.");
			}
		}
	}

	/**
	 * Most of the time assetUseId will be the parameter passed,
	 * but when the user clicks on "Save and add another use" then
	 * the assetId parameter will be passed instead.
	 * (And for createAsset nothing is passed.)
	 */
	/*@RequestMapping(value="/load")*/
	@RequestMapping(value={"/asset/main/load", "/asset/main/custom/load", "/asset/main/view/load"}, method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView load (@ModelAttribute(FORM_MODEL_NAME) AssetNavigationForm form,
			@RequestParam(value = "auId") Integer id) throws Exception
	{
		log.debug("formBackingObject(): entered...");
		AssetUse assetUse = null;
		if (0 >= id) {
			assetUse = new AssetUse();
		} else {
			 assetUse = assetUseRepository.loadAssetUseById(id);
		}

		ModelAndView mv = new ModelAndView(getFormView());
		mv.addObject("assetUse", assetUse);
		mv.addObject("isLast", form.isLast ("" + id));
		mv.addObject("isFirst", form.isFirst ("" + id));
		return mv;
	}


	@RequestMapping(value={"/asset/main/next", "/asset/main/custom/next", "/asset/main/view/next"}, method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView next(@ModelAttribute(FORM_MODEL_NAME) AssetNavigationForm form)
		throws Exception
	{
		return new ModelAndView (getSuccessView() + "?auId=" + form.getNextId());
	}

	@RequestMapping(value={"/asset/main/add", "/asset/main/custom/add", "/asset/main/view/add"}, method = {RequestMethod.GET, RequestMethod.POST})
	public void add(HttpServletResponse response,
			@ModelAttribute(FORM_MODEL_NAME) AssetNavigationForm form,
			@RequestParam(value = "auId") String id)
		throws Exception
	{
		form.addAuid(id);
		response.setContentType("application/json");
		PrintWriter writer = response.getWriter(); // throws IOException
		writer.println(id);
	}

	@RequestMapping(value={"/asset/main/prev", "/asset/main/custom/prev", "/asset/main/view/prev"}, method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView prev(@ModelAttribute(FORM_MODEL_NAME) AssetNavigationForm form)
		throws Exception
	{
		return new ModelAndView (getSuccessView() + "?auId=" + form.getPrevId());
	}

	public AssetUseRepository getAssetUseRepository()
	{
		return assetUseRepository;
	}

	public void setAssetUseRepository(AssetUseRepository assetUseRepository)
	{
		this.assetUseRepository = assetUseRepository;
	}
	public AssetRepository getAssetRepository() {
		return assetRepository;
	}

	public void setAssetRepository(AssetRepository assetRepository) {
		this.assetRepository = assetRepository;
	}

	public AssetService getAssetService() {
		return assetService;
	}

	public void setAssetService(AssetService assetService) {
		this.assetService = assetService;
	}
	public AssetUseService getAssetUseService() {
		return assetUseService;
	}

	public void setAssetUseService(AssetUseService assetUseService) {
		this.assetUseService = assetUseService;
	}

}
