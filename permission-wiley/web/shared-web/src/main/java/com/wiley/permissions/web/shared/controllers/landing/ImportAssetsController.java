package com.wiley.permissions.web.shared.controllers.landing;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.jxls.reader.XLSDataReadException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import com.wiley.permissions.common.excel.ExcelTransformerUtility;
import com.wiley.permissions.domain.persistence.permissions.CommonWork;
import com.wiley.permissions.domain.persistence.permissions.CommonWorkStatus;
import com.wiley.permissions.domain.persistence.permissions.ExtendedAssetUse;
import com.wiley.permissions.domain.persistence.permissions.PermissionType;
import com.wiley.permissions.persistence.PersistenceException;
import com.wiley.permissions.services.imports.ExcelDataImportUtility;
import com.wiley.permissions.services.imports.ImportAssetsStatus;
import com.wiley.permissions.services.util.ServiceException;
import com.wiley.permissions.web.shared.PermUserContext;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;

/**
 * Importing an asset spreadsheet
 * @author lnagy
 */
@Controller
/*@RequestMapping("/commonwork/importAssets")*/
@RequestMapping
@SessionAttributes(ImportAssetsController.FORM_MODEL_NAME)
public class ImportAssetsController extends BaseAnnotatedController {

	private static final Log log = LogFactory.getLog(ImportAssetsController.class);

	protected final static String FORM_MODEL_NAME = "importAssetsForm";

	private ExcelDataImportUtility importAssetsUtility;
	private String importAssetsView;
	private String userLandingViewName;
	private String successRedirectUrl;

	@Override
	protected List<String> getReferenceDataNames(HttpServletRequest request) throws Exception
	{
		List<String> output = new ArrayList<String>();

		output.add("mediaTypes");
		output.add("ownerTypes");
		output.add("renditionTypes");
		output.add("usages");
		output.add("sizes");
		output.add("statuses");
		output.add("countries");

		return output;
	}

	@Override
	@ModelAttribute
	public void referenceData(Model model, HttpServletRequest request)
	throws Exception
	{
		super.referenceData(model, request);
		model.addAttribute("permissionTypes", PermissionType.VALID_VALUES);
	}

	@RequestMapping(value="/commonwork/importAssets/view", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView view(HttpServletRequest request, HttpServletResponse response) throws ServiceException
	{
		CommonWork cw = PermUserContext.getCurrentCommonWork(request);
		// lnagy - maybe create a Super controller for every page that
		// requires the PermUserContext.getCurrentCommonWork to be present - we had to do this test in the reports
		// and in the commonworkLandingPage
		ModelAndView mv = new ModelAndView(getImportAssetsView());
		if (null == cw)
		{
			String msg = getMessageSource().getMessage("no.product.for.product.landing", null, null);
			mv = new ModelAndView(getUserLandingViewName() + "?generalMessage=" + msg);
		}
		mv.addObject(FORM_MODEL_NAME, new ImportAssetsForm());
		return mv;
	}

	/*@GetMapping("/progressbar")*/
	@RequestMapping(value="/commonwork/importAssets/progressbar", method = {RequestMethod.GET, RequestMethod.POST})
	public void progressbar(HttpServletRequest request, HttpServletResponse response) throws ServiceException, IOException
	{
		ImportAssetsStatus status = (ImportAssetsStatus)request.getSession().getAttribute("progressbar");
		int percentage = 5;

		if (null != status)
		{
			int total = status.getTotalCount() - status.getFailureCount();
			if (0 != total)
				percentage = (100 * status.getProcessedCount()) / total;
		}
		log.debug("progressbar(): percentage = " + percentage);

		String percentageString = String.valueOf(percentage);
        response.setContentType("text/plain");
        // We can safely sent content-length to the string length
        // since there won't be any double-byte chars.
        response.setContentLength(percentageString.length());
        PrintWriter out = response.getWriter();  // throws IOException
        out.print(percentageString);
	}

	/**
	 * Processing the uploaded file. First step is to populate and validate the
	 * data. If that fails because: 1. unable to read the file - user has to
	 * upload another file 2. all records fail the validation (like missing a
	 * column) - user has to upload another file 3. some records fail - the user
	 * will be asked to commit or not the records that were successful - that
	 * means the function will run again with force = TRUE
	 *
	 * @param form
	 * @param bindingResult
	 * @param request
	 * @return
	 * @throws ServiceException
	 */
	/*@RequestMapping("/submit")
	public ModelAndView submit(@ModelAttribute(FORM_MODEL_NAME)	ImportAssetsForm form,
			BindingResult bindingResult, HttpServletRequest request) throws ServiceException
	{
		String[] columns = null;
		MultipartFile f = form.getFile();
		byte[] bytes = getFileAsByteArray(form);

		log.debug("submit(): file = " + f.getOriginalFilename());
		log.debug("submit(): forceImport " + form.isForcePersist());

		// validate the form, and the spreadsheet (the name of the worksheet, if there are any rows)
		validate(form, bindingResult);
		try {
			//Start: Changes made for the new spreadsheet upload requirement DM-289
			columns = ExcelTransformerUtility.validate(bytes);
			columns = importAssetsUtility.getMappingColumns (columns);
			ExcelTransformerUtility.validate(bytes);
			columns = importAssetsUtility.getColumns (columns);
			//for(String column : columns)
			//	log.debug("submit(): columns = " + column);
			//importAssetsUtility.validateColumns (columns);
			//End: Changes made for the new spreadsheet upload requirement DM-289
		} catch (ValidateException e1) {
			bindingResult.reject(null, e1.getMessage());
		}

		if (bindingResult.hasErrors()){
			return error(bindingResult);
		}


		ModelAndView modelAndView = new ModelAndView(getImportAssetsView());
		// reset some of the fields
		bindingResult.getModel().put("filename", null);
		bindingResult.getModel().put("force", Boolean.FALSE);
		modelAndView.addAllObjects(bindingResult.getModel());

		CommonWork cw = PermUserContext.getCurrentCommonWork(request);

		List<ExtendedAssetUse> auList = null;

		try {
			Integer userId = PermUserContext.getCurrentUserId(request);
			// James says: "No Permissions Required" is generally used when
			// there are no assets so because assets are now being added,
			// it's a good idea to change this.
			if (cw.getInteriorCWStatus().equals(CommonWorkStatus.NO_PERMISSIONS_REQUIRED)) {
				cw.setInteriorCWStatus(CommonWorkStatus.IN_PROGRESS);
			}
			ImportAssetsStatus status = new ImportAssetsStatus();

			request.getSession().setAttribute("progressbar", status);

			log.debug("submit(): Before populateAndValidate");
			getAssetsImportUtility().populateAssetUseListFromExcel(
					userId, cw.getId(), columns, bytes, status, true);

			auList = status.getGoodRecords();

			// if validation failed
			if (!status.isStatusOK()) {
				log.debug("submit(): populateAndValidate - failed");
				if (!form.isForcePersist()) {
					if (status.getFailureCount() == status.getTotalCount()) {
						log.debug("submit(): Failed to import assets " + status.getErrorMessage());
						bindingResult.reject(
								"product.import.assets.errors.all.validation",
								new Object[] { status.getErrorMessage() },
								StringUtils.EMPTY);
						log.debug("submit(): ImportAssetsException not force all failed ");
					}
					// if just few have failed, we show the message for commit or rollback
					else {
						modelAndView.addObject("filename", form.getFilename());
						modelAndView.addObject("forceImport", Boolean.TRUE);

						log.debug("submit(): Failed to import assets " + status.getErrorMessage());
						bindingResult.reject("product.import.assets.errors.validation", new Object[] {
								status.getTotalCount(),
								status.getFailureCount(), status.getErrorMessage() }, StringUtils.EMPTY);
						log.debug("submit(): ImportAssetsException not force partial failed ");
					}
					return error(bindingResult);
				}
				else {
					log.debug("submit(): populateAndValidate - failed but ForcePersist is "
							+ "true so we try to persist the good records");
				}
			}
			else {
				log.debug("submit(): populateAndValidate - success");
			}
			log.debug("sumbit(): Before persist");
			// if no failures or failures and force flag is true
			// we persist
			getAssetsImportUtility().persistAssetUseListFromExcel(auList, status, true);
			log.debug("submit(): After persist");
			log.debug("submit(): THE END");
		}
		catch (XLSDataReadException e) {
			log.debug("submit(): Failed to read the spreadsheet", e);
			bindingResult.reject("product.import.assets.errors.spreadsheet", new Object[] { e
					.getMessage() }, StringUtils.EMPTY);
			return error(bindingResult);
		}
		catch (PersistenceException e) {
			log.debug("submit(): Failed to persist assets ", e);
			bindingResult.reject("product.import.assets.errors.general",
					new Object[] { e.getMessage() }, StringUtils.EMPTY);
			return error(bindingResult);
		}
		catch (Exception e) {
			log.debug("submit(): Failed to import ", e);
			bindingResult.reject(
					"product.import.assets.errors.general",
					new Object[] { e.getMessage() },
					StringUtils.EMPTY);
			return error(bindingResult);
		}

		modelAndView = new ModelAndView("dialog.success");
		String msg = getMessageSource().getMessage("product.import.assets.success",
				new Object[] {auList.size()}, null);
		String msg = getMessageSource().getMessage("product.import.assets.success",
				new Object[] {5}, null);//5 is hard coded value need to be removed later when the above code uncommented.
		modelAndView.addObject("message", msg);
		modelAndView.addObject("redirectUrl", successRedirectUrl);

		return modelAndView;
	}*/

	//Start: Changes made for the new spreadsheet upload requirement DM-289
	/*@PostMapping("/submit")*/
	@RequestMapping(value="/commonwork/importAssets/submit", method = RequestMethod.POST)
	public ModelAndView submit(@ModelAttribute(FORM_MODEL_NAME)	ImportAssetsForm form,
			BindingResult bindingResult, HttpServletRequest request) throws ServiceException
	{
		String[] columns = null;
		byte[] bytes = null;
		try {
			MultipartFile f = form.getFile();
			bytes = getFileAsByteArray(form);

			log.debug("submit(): file = " + f.getOriginalFilename());
			log.debug("submit(): forceImport " + form.isForcePersist());

			// validate the form, and the spreadsheet (the name of the worksheet, if there are any rows)
			validate(form, bindingResult);

			ExcelTransformerUtility.validate(bytes);
			columns = importAssetsUtility.getColumns (columns);

		} catch (Exception e1) {
			bindingResult.reject(null, e1.getMessage());
		}

		if (bindingResult.hasErrors()){
			return error(bindingResult);
		}


		ModelAndView modelAndView = new ModelAndView(getImportAssetsView());
		// reset some of the fields
		bindingResult.getModel().put("filename", null);
		bindingResult.getModel().put("force", Boolean.FALSE);
		modelAndView.addAllObjects(bindingResult.getModel());
		//modelAndView.addObject("hasErrors", Boolean.FALSE);

		CommonWork cw = PermUserContext.getCurrentCommonWork(request);

		List<ExtendedAssetUse> auList = null;
		ImportAssetsStatus status = null;

		try {
			Integer userId = PermUserContext.getCurrentUserId(request);
			// James says: "No Permissions Required" is generally used when
			// there are no assets so because assets are now being added,
			// it's a good idea to change this.
			if (cw.getInteriorCWStatus().equals(CommonWorkStatus.NO_PERMISSIONS_REQUIRED)) {
				cw.setInteriorCWStatus(CommonWorkStatus.IN_PROGRESS);
			}
			status = new ImportAssetsStatus();

			request.getSession().setAttribute("progressbar", status);

			log.debug("submit(): Before populateAndValidate");
			getAssetsImportUtility().populateAssetUseListFromExcel(
					userId, cw.getId(), columns, bytes, status, true);

			auList = status.getGoodRecords();

			// if validation failed
			if (!status.isStatusOK()) {
				log.debug("submit(): populateAndValidate - failed");
				if (!form.isForcePersist()) {
					if (status.getFailureCount() == status.getTotalCount()) {
						log.debug("submit(): Failed to import assets " + status.getErrorMessage());
						bindingResult.reject(
								"product.import.assets.errors.all.validation",
								new Object[] { status.getErrorMessage() },
								StringUtils.EMPTY);
						log.debug("submit(): ImportAssetsException not force all failed ");
					}
					// if just few have failed, we show the message for commit or rollback
					else {
						modelAndView.addObject("filename", form.getFilename());
						modelAndView.addObject("forceImport", Boolean.TRUE);

						log.debug("submit(): Failed to import assets " + status.getErrorMessage());
						bindingResult.reject("product.import.assets.errors.validation", new Object[] {
								status.getTotalCount(),
								status.getFailureCount(), status.getErrorMessage() }, StringUtils.EMPTY);
						log.debug("submit(): ImportAssetsException not force partial failed ");
					}
					return error(bindingResult);
				}
				else {
					log.debug("submit(): populateAndValidate - failed but ForcePersist is "
							+ "true so we try to persist the good records");
				}
			}
			else {
				log.debug("submit(): populateAndValidate - success");
			}
			log.debug("sumbit(): Before persist");
			// if no failures or failures and force flag is true
			// we persist
			getAssetsImportUtility().persistAssetUseListFromExcel(auList, status, true);
			log.debug("submit(): After persist");
			log.debug("submit(): THE END");
		}
		catch (XLSDataReadException e) {
			log.debug("submit(): Failed to read the spreadsheet", e);
			bindingResult.reject("product.import.assets.errors.spreadsheet", new Object[] { e
					.getMessage() }, StringUtils.EMPTY);
			modelAndView.getModel().putAll(bindingResult.getModel());
			//return error(bindingResult);
		}
		catch (PersistenceException e) {
			log.debug("submit(): Failed to persist assets ", e);
			bindingResult.reject("product.import.assets.errors.general",
					new Object[] { e.getMessage() }, StringUtils.EMPTY);
			modelAndView.getModel().putAll(bindingResult.getModel());
			//return error(bindingResult);
		}
		catch (Exception e) {
			log.debug("submit(): Failed to import ", e);
			bindingResult.reject(
					"product.import.assets.errors.general",
					new Object[] { e.getMessage() },
					StringUtils.EMPTY);
			modelAndView.getModel().putAll(bindingResult.getModel());
			//return error(bindingResult);
		}

		modelAndView = new ModelAndView("dialog.success");
		String msg = getMessageSource().getMessage("product.import.assets.success",
				new Object[] {auList.size()}, null);
		modelAndView.addObject("message", msg);
		modelAndView.addObject("redirectUrl", successRedirectUrl);

		return modelAndView;
	}
	//End: Changes made for the new spreadsheet upload requirement DM-289

	/**
	 * returns the bytes of the uploaded file or the file from the temp
	 * directory
	 *
	 * @param form
	 * @return byte[]
	 * @throws IOException
	 */
	private byte[] getFileAsByteArray(ImportAssetsForm form) {
		byte[] bytes;
		MultipartFile f = form.getFile();

		try {
			// if a file is uploaded, always has precedence
			if (null != f && 0 < f.getSize()) {
				bytes = f.getBytes();
				// save the file to temp folder
				File tempFile = new File(FileUtils.getTempDirectory(), f.getOriginalFilename());
				form.setFilename(tempFile.getName());
				FileUtils.writeByteArrayToFile(tempFile, bytes);
			}
			else {
				// if the flag to force the import is set, we just read the data
				// from the temp file we initially saved
				if (form.isForcePersist()) {
					File tempFile = new File(FileUtils.getTempDirectory(), form.getFilename());
					bytes = FileUtils.readFileToByteArray(tempFile);
				}
				else {
					throw new IOException("No file specified to import");
				}
			}
		}
		catch (IOException e) {
			throw new XLSDataReadException(e);
		}
		return bytes;
	}

	/**
	 * validates the form
	 * @param form
	 * @param bindingResult
	 */
	private void validate(ImportAssetsForm form, BindingResult bindingResult) {
		MultipartFile f = form.getFile();

		getValidator().validate(form, bindingResult);

		// if the force import is set, we do not check for the file again
		// because we have the file on the temporary folder
		if (!form.isForcePersist() && (null == f || f.getSize() == 0)) {
			bindingResult.rejectValue("file", "product.import.assets.file");
		}
	}

	private ModelAndView error(BindingResult bindingResult) {
		log.debug(bindingResult.getAllErrors());
		ModelAndView error = new ModelAndView(getImportAssetsView());
		//error.addObject("hasErrors", Boolean.TRUE);
		error.getModel().putAll(bindingResult.getModel());
		return error;
	}

	public String getImportAssetsView() {
		return importAssetsView;
	}

	public void setImportAssetsView(String importAssetsView) {
		this.importAssetsView = importAssetsView;
	}

	public ExcelDataImportUtility getAssetsImportUtility() {
		return importAssetsUtility;
	}

	public void setAssetsImportUtility(ExcelDataImportUtility importAssetsUtility) {
		this.importAssetsUtility = importAssetsUtility;
	}

	public String getUserLandingViewName() {
		return userLandingViewName;
	}

	public void setUserLandingViewName(String userLandingViewName) {
		this.userLandingViewName = userLandingViewName;
	}

	public void setSuccessRedirectUrl(String successRedirectUrl) {
		this.successRedirectUrl = successRedirectUrl;
	}
}
