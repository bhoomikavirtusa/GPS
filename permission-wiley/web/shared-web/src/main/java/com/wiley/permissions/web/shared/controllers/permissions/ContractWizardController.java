package com.wiley.permissions.web.shared.controllers.permissions;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang3.ArrayUtils;
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

import com.wiley.permissions.common.transformer.ObjectToJson;
import com.wiley.permissions.domain.persistence.permissions.Address;
import com.wiley.permissions.domain.persistence.permissions.AddressType;
import com.wiley.permissions.domain.persistence.permissions.Amendment;
import com.wiley.permissions.domain.persistence.permissions.Asset;
import com.wiley.permissions.domain.persistence.permissions.AssetUse;
import com.wiley.permissions.domain.persistence.permissions.AssetUseFile;
import com.wiley.permissions.domain.persistence.permissions.CompCopy;
import com.wiley.permissions.domain.persistence.permissions.ConditionNode;
import com.wiley.permissions.domain.persistence.permissions.Contact;
import com.wiley.permissions.domain.persistence.permissions.Contract;
import com.wiley.permissions.domain.persistence.permissions.ContractAsset;
import com.wiley.permissions.domain.persistence.permissions.ContractFile;
import com.wiley.permissions.domain.persistence.permissions.ContractFileName;
import com.wiley.permissions.domain.persistence.permissions.CopyrightType;
import com.wiley.permissions.domain.persistence.permissions.Currency;
import com.wiley.permissions.domain.persistence.permissions.EnumUsageCondition;
import com.wiley.permissions.domain.persistence.permissions.ImportSource;
import com.wiley.permissions.domain.persistence.permissions.MasterAgreementDeal;
import com.wiley.permissions.domain.persistence.permissions.MediaType;
import com.wiley.permissions.domain.persistence.permissions.OwnerType;
import com.wiley.permissions.domain.persistence.permissions.PermissionStatus;
import com.wiley.permissions.domain.persistence.permissions.Privilege;
import com.wiley.permissions.domain.persistence.permissions.Product;
import com.wiley.permissions.domain.persistence.permissions.PurchaseOrder;
import com.wiley.permissions.domain.persistence.permissions.Role;
import com.wiley.permissions.domain.persistence.permissions.RoyaltyFreeDeal;
import com.wiley.permissions.domain.persistence.permissions.Size;
import com.wiley.permissions.domain.persistence.permissions.Source;
import com.wiley.permissions.domain.persistence.permissions.SourceAddress;
import com.wiley.permissions.domain.persistence.permissions.UploadDocumentsHistory;
import com.wiley.permissions.domain.persistence.permissions.UploadedDocumentsDetails;
import com.wiley.permissions.domain.persistence.permissions.UsageConditionSize;
import com.wiley.permissions.persistence.PersistenceException;
import com.wiley.permissions.repositories.AssetRepository;
import com.wiley.permissions.repositories.AssetUseRepository;
import com.wiley.permissions.repositories.ConditionRepository;
import com.wiley.permissions.repositories.ContractRepository;
import com.wiley.permissions.repositories.ProductRepository;
import com.wiley.permissions.repositories.PurchaseOrderRepository;
import com.wiley.permissions.repositories.SourceRepository;
import com.wiley.permissions.security.web.UserPrincipal;
import com.wiley.permissions.services.AssetUseService;
import com.wiley.permissions.services.CommonWorkService;
import com.wiley.permissions.services.ContractService;
import com.wiley.permissions.services.view.POAssetView;
import com.wiley.permissions.web.shared.PermUserContext;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;
import com.wiley.permissions.web.shared.controllers.MultiUploadForm;
import com.wiley.permissions.web.shared.controllers.UploadForm;
import com.wiley.permissions.web.shared.controllers.landing.ConditionControllerUtils;
import com.wiley.permissions.web.shared.util.GenericFileView;
import com.wiley.sf.common.lang.ArgUtil;
import com.wiley.sf.common.lang.StringUtil;
import com.wiley.sf.common.monitor.PerformanceMonitor.PerfTimer;

@Controller
/*@RequestMapping("/permissions/contract")*/
@RequestMapping
@SessionAttributes(ContractWizardController.FORM_NAME)
public class ContractWizardController extends BaseAnnotatedController {

	// --------------------- static data ---------------------------------

	private final static Log log = LogFactory.getLog(ContractWizardController.class);

	public final static String FORM_NAME = "contractWizardForm";
	private static final String CONDITIONS_NAME = "contractConditions";

	// --------------------- instance data -------------------------------
	// default can be overridden in Spring config
	private final long maximumContractFileSize = 10 * 1024 * 1024;
	private final long maximumAmendmentFileSize = 10 * 1024 * 1024;

	private AssetUseRepository assetUseRepository;
	private AssetRepository assetRepository;
	private PurchaseOrderRepository purchaseOrderRepository;
	private ContractRepository contractRepository;
	private ContractService contractService;
	private AssetUseService assetUseService;
	private CommonWorkService commonWorkService;//Added for DM-532
	private ConditionRepository conditionRepository;
	private SourceRepository sourceRepository;
	private ProductRepository productRepository;//Added for DM-532


	private ImportSource importSource;

	@ModelAttribute("mailingAddressList")
	public List<Address> getMailingAddressList(HttpServletRequest request) throws Exception {
		ContractWizardForm form = (ContractWizardForm) request.getSession().getAttribute(FORM_NAME);
		if (null != form && null != form.getSource()) {
			return sourceRepository.getMailToAddresses(form.getSource());
		}
		return null;
	}

	@Override
	protected List<String> getReferenceDataNames(HttpServletRequest request) throws Exception {
		List<String> output = new ArrayList<String>();

		output.add("currencies");
		output.add("countries");
		output.add("contractTypes");
		output.add("sizes");
		output.add("usageConditions");
		output.add("addressTypes");
		return output;
	}

	@RequestMapping(value="/permissions/contract/download_start", method = {RequestMethod.GET, RequestMethod.POST})
	public Object downloadStart(@RequestParam("auId") Integer auId,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		// if multiple files we return a page with links to all the files, otherwise return empty
		log.debug("downloadStart(): entered...");

		//Added for Paperwork Change starts
		//Integer assetId = assetUseRepository.loadAssetIdForAssetUseId(auId);
		//contractRepository.loadContractDetailsForAssetId(assetId);
		//Added for Paperwork Change Ends

		List<AssetUseFile> aufiles = assetUseRepository.loadAssetUseFiles (auId);
		AssetUse au = assetUseRepository.lazyLoad(AssetUse.class, auId, new String[] {"asset", "commonWork"});
		//Modified for Paperwork Change starts
		List<Contract> contracts = new ArrayList<Contract>();
		log.debug("PICKUP ISBN  --> "+au.getReusedISBN());
		log.debug("PICKUP ISBN BEFORE CONDITION --> "+au.getCommonWork().getId());
		if(StringUtils.isBlank(au.getReusedISBN()) && StringUtils.isBlank(au.getPickupISBN())){
		  log.debug("PICKUP ISBN IF ");
		  contracts = contractRepository.loadListForAssetCW (au.getAsset().getId(), au.getCommonWork().getId());
		//Added for Sr_195127
		  if (CollectionUtils.isEmpty(contracts)) {
			  List<Integer> list = assetUseRepository.getLatestContractIds(auId);
			  for(Integer i : list){
			  Contract c=contractRepository.loadContractById(i);
			  contracts.add(c);
			  }
		  }
		}else{
		  Integer cw_Id = 0;
		  //Changed for Paperwork Download
		  String isbn13 = au.getPickupISBN() != null ?  au.getPickupISBN() : au.getReusedISBN();
		  contracts = contractRepository.loadListForAssetCW(au.getAsset().getId(), au.getCommonWork().getId());
		  if (CollectionUtils.isEmpty(contracts)) {
		  if(StringUtils.isNotBlank(isbn13) && au.getImportSource() != null){
			  cw_Id = contractRepository.loadProductCwId(isbn13);
			  contracts = contractRepository.loadListForAssetCW(au.getAsset().getId(), cw_Id);
		  }else{
			  contracts = contractRepository.loadListForAssetCW(au.getAsset().getId(), au.getCommonWork().getId());
		  }
		  }
		  if (CollectionUtils.isEmpty(contracts)) {
			  List<Integer> list = assetUseRepository.getLatestContractIds(auId);
			  for(Integer i : list){
			  Contract c=contractRepository.loadContractById(i);
			  contracts.add(c);
			  }
		  }
		  /*
		  if(au.getReusedISBN() != null){
			  cw_Id = contractRepository.loadProductCwId(au.getReusedISBN());
		  }else{
			  cw_Id = contractRepository.loadProductCwId(au.getPickupISBN());
		  }
		  log.debug("PICKUP ISBN ELSE ");
		  log.debug("PICKUP ISBN ELSE "+cw_Id);
		  contracts = contractRepository.loadListForAssetCW (au.getAsset().getId(), cw_Id);*/

		}
		//Modified for Paperwork Change Ends
		List<ContractFile> allFiles = new ArrayList<ContractFile>();
		for (Contract contract : contracts) {
			contract = contractRepository.lazyLoad(Contract.class, contract.getId(), new String[] { "files" });
			List<ContractFile> files = contract.getFiles();
			allFiles.addAll(files);
		}

		int size = aufiles.size() + allFiles.size();
        log.debug("SIZEEEE ----> "+size);

		// just one file, we will start the download
		if (size == 1) {
			log.debug("SIZEEEE ----> "+size);
			response.setContentType("text/plain");
			PrintWriter writer = response.getWriter(); // throws IOException
			if (1 == aufiles.size()) {
				log.debug("IN IF BLOCK");
				writer.write(request.getContextPath() + "/sapp/asset/custom/download_file?fileId=" + aufiles.get(0).getId());
			} else {
				log.debug("IN ELSE BLOCK"+allFiles.get(0).getId()+""+request.getContextPath());
				writer.write(request.getContextPath() + "/sapp/permissions/contract/download_file?fileId=" + allFiles.get(0).getId());
			}
			return null;
		}
		else {
			log.debug("IN ELSE 2 BLOCK");
			ModelAndView mv = new ModelAndView("pages.contract.filelist");
			mv.addObject("files", allFiles);
			mv.addObject("aufiles", aufiles);
			return mv;
		}
	}

	/*@GetMapping(value = "/upload_amendment_file")*/
	@RequestMapping(value="/permissions/contract/upload_amendment_file", method = {RequestMethod.GET, RequestMethod.POST})
	public void wizardUploadAmendment(HttpServletRequest request, HttpServletResponse response,
			@ModelAttribute(UploadForm.FORM_MODEL_NAME) UploadForm form)
			throws Exception {
		MultipartFile mpf = form.getFile();
		ContractWizardForm cForm = (ContractWizardForm) request.getSession().getAttribute(FORM_NAME);

		if (mpf != null && mpf.getSize() > 0) {
			PrintWriter writer = response.getWriter(); // throws IOException
			response.setContentType("text/plain");
			Amendment amendment = cForm.getContract().getAmendment();

			if (null == amendment) {
				amendment = new Amendment();
				amendment.setContract(cForm.getContract());
				cForm.getContract().setAmendment(amendment);
			}

			if (mpf.getSize() <= maximumAmendmentFileSize) {
				amendment.setData(mpf.getBytes());
				amendment.setFileName(mpf.getOriginalFilename());
				amendment.setMimeType(mpf.getContentType());

				log.debug("uploadAmendment(): File Has " + mpf.getSize() + " bytes");
				writer.write("File " + mpf.getOriginalFilename() + " (" + mpf.getSize() + " bytes) uploaded");
			}
			else {
				log.info("uploadAmendment(): File TOO LARGE - has " + mpf.getSize() + " bytes");
				writer.write("File too large. Maximum size is " + maximumAmendmentFileSize + " bytes.");
			}
		}
	}

	/*@GetMapping("/download_file")*/
	@RequestMapping(value="/permissions/contract/download_file", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView downloadFile(HttpServletRequest request, @RequestParam("fileId") Integer fileId)
			throws Exception {
		log.debug("downloadFile(): entered...");

		ModelAndView mv = new ModelAndView();
		GenericFileView view = new GenericFileView();
		ContractFile file = null;
		if (null != fileId) {
			file = contractRepository.find(ContractFile.class, fileId);
		// if it is contract wizard and the file has just been uploaded
		} else {
			ContractWizardForm form = (ContractWizardForm) request.getSession().getAttribute(FORM_NAME);
			if (form != null) {
				Contract contract = form.getContract();
				List<ContractFile> list = contract.getFiles();
				for (int i = 0; i < list.size(); i++) {
					file = list.get(i);
					// if values are equal (can be null)
					log.debug("downloadFile(): " + file.getId() + " param " + fileId);
					if (ObjectUtils.equals(fileId, file.getId())) {
						break;
					}
				}
			}
		}

		if (null != file) {
			view.setFileName(file.getFileName());
			view.setContentType(file.getMimeType());

			//log.debug("downloadFile(): fileData.length: " + file.getFileData().length);
			view.setData(file.getFileData());
			mv.setView(view);
			return mv;
		} else {
			throw new Exception ("downloadFile(): file was not found");
		}
	}

	@RequestMapping(value = "/permissions/contract/delete_file", method = RequestMethod.POST)
	public void deleteFile(HttpServletRequest request, HttpServletResponse response,
			@RequestParam("fileId") Integer fileId,
			@RequestParam(value = "contractId", required = false) Integer contractId)
			throws Exception {
		log.debug("deleteFile(): fileId = " + fileId +"contractId "+contractId);
		ContractFile conFile = contractRepository.loadContractFilesByFileId(fileId);
		if(conFile!=null)
		{
			boolean removedFileStatus = contractRepository.removeFileFromContractFileName(contractId,conFile.getFileName());
		}
		contractRepository.remove(ContractFile.class, fileId);

		// also need to remove from contract in session so doesn't get re-saved
		ContractWizardForm form = (ContractWizardForm) request.getSession().getAttribute(FORM_NAME);
		if (form != null) {
			Contract contract = form.getContract();
			List<ContractFile> list = contract.getFiles();
			for (int i = 0; i < list.size(); i++) {
				ContractFile file = list.get(i);
				// if values are equal (can be null)
				log.debug("deleteFile(): " + file.getId() + " param " + fileId);
				if (ObjectUtils.equals(fileId, file.getId())) {
					list.remove(i);
					break;
				}
			}
		}

		try {
			if(null != contractId) {
				contractService.refreshAssetIndex (contractId);
			} else {
				contractService.refreshAssetIndex (form.getContract().getId());
			}

		} catch (Exception ex) {  // this is not expected
			// log exception and continue
			log.error("deleteFile(): caught exception calling refreshAssetIndex():", ex);
		}

		String output = "File removed";
		// If don't set contentType causes JavaScript error in Firefox
		// (either "text/plain" or "text/html" works)
		response.setContentType("text/plain");
		// Only ok to set contentLength because we know that there are no double-byte chars in output
        response.setContentLength(output.length());
		PrintWriter writer = response.getWriter(); // throws IOException
		writer.write(output);
	}

	//Start: Added for DM-532
	@RequestMapping(value = "/permissions/contract/save_file_names", method = RequestMethod.POST)//validate_file_names
	public void saveFileNames(HttpServletRequest request, HttpServletResponse response,
			@RequestParam("fileText") String fileText,
			@RequestParam(value = "contractId", required = false) Integer contractId)
			throws Exception {
		log.debug("saveFileNames(): fileText = " + fileText);
		if(StringUtils.isNotEmpty(fileText)) {
			fileText = fileText.trim();
		} else {
			fileText = "";
		}
		//loadContractById
		Contract contract = null;

		// also need to remove from contract in session so doesn't get re-saved
		ContractWizardForm form = (ContractWizardForm) request.getSession().getAttribute(FORM_NAME);
		if(null != contractId) {
			//contract = contractRepository.loadContractById(contractId);
			contract = contractRepository.lazyLoad(Contract.class, contractId, new String[] {"fileNames"});
		}
		else {
			if(form != null) {
				contract = form.getContract();
			}
		}
		if(null != contract) {
			List<ContractFileName> list = contract.getFileNames();
			List<Integer> indexList = null;
			int size = list.size();
			if(size > 0) {
				indexList = new ArrayList<Integer>(size);
			}
			String fileNamesStr = contract.getFileNameString();
			log.debug("fileNamesStr = " + fileNamesStr);
			String fileName = null;
			for (int i = 0; i < size; i++) {
				ContractFileName fileNameObj = list.get(i);
				fileName = fileNameObj.getFileName();
				if(fileName.length() > 0) {
					if(StringUtils.isNotBlank(fileText) && StringUtils.contains(fileText, fileName)) {
						continue;
					} else {
						//list.remove(i);
						indexList.add(i);
						contractRepository.remove(ContractFileName.class, fileNameObj.getId());
						log.debug("saveFileNames(): removed file name object for file name: " + fileName);
					}
				}
			}

			for(Integer index : indexList) {
				list.remove(index);
			}

			for (String filename : fileText.split(";")) {
				filename = filename.trim();
				if(filename.length() > 0) {
					if(StringUtils.contains(fileNamesStr, filename)) {
						continue;
					} else {
						ContractFileName fileObj = new ContractFileName();
						fileObj.setFileName(filename);
						fileObj.setContract(contract);
						ContractFileName saveFile = contractRepository.saveRequiresNew(fileObj);
						list.add(saveFile);
						log.debug("saveFileNames(): added file name object for file name: " + filename);
						log.debug("500 saveFile :  " + contract.getFileNameString());
					}
				}
			}

			// this will set what the user enters
			//	contract.setFileNameString(fileText);
		}

		try {
			if(null != contractId) {
				contractService.refreshAssetIndex (contractId);
			} else {
				contractService.refreshAssetIndex (form.getContract().getId());
			}

		} catch (Exception ex) {  // this is not expected
			// log exception and continue
			log.error("saveFileNames(): caught exception calling refreshAssetIndex():", ex);
		}

		// Get FileNames from the DB and send the same response
		Contract respToUiContract = contractRepository.lazyLoad(Contract.class, contractId, new String[] {"fileNames"});
		String output =  respToUiContract.getFileNameString();
		if(output==null)
			output = "";
		contract.setFileNameString(output);
		//String output = contract.getFileNameString();
		// If don't set contentType causes JavaScript error in Firefox
		// (either "text/plain" or "text/html" works)
		response.setContentType("text/plain");
		// Only ok to set contentLength because we know that there are no double-byte chars in output
		response.setContentLength(output.length());
		PrintWriter writer = response.getWriter(); // throws IOException
		log.debug("response to UI : "+output);
		writer.write(output);
	}

	@RequestMapping(value = "/permissions/contract/validate_file_names", method = RequestMethod.POST)
	public void validateContractFileNamesStr(HttpServletRequest request, HttpServletResponse response,
			@RequestParam("fileText") String fileTextWithNoSpace)
			throws Exception {
		String output = "false";
		if(StringUtils.isNotBlank(fileTextWithNoSpace)) {
			if(!StringUtils.containsNone(fileTextWithNoSpace, ",:")) {
				output="true";
			}else {
				log.debug("validateContractFileNamesStr() Entered str : "+fileTextWithNoSpace);
				String fileText = fileTextWithNoSpace.trim().replaceAll("\n", "").replaceAll(" ", "");	//replace both \n and space with empty character

				// New logic Added by Chethan for DM-532
			String[] fileNames = fileText.trim().split(";");
			for (String fileName : fileNames) {
				fileName = fileName.trim();
				String pattern = "^([a-zA-Z0-9_]+(\\.[a-zA-Z]{3,4}))$";

				//String pattern = "^([a-zA-Z0-9_]+(\\.[pdf,doc,docx,png,gif,ppt,xls,xlsx,txt,rtf,tif,msg,jpg,jpeg]{3,4}))$";

				Pattern p = Pattern.compile(pattern);
				Matcher m = p.matcher(fileName.trim()); // get a matcher object
				if (!m.find()) {
					log.debug("Not allowable fileName "+fileName);
					output="true";
				}
				if ( !fileName.endsWith(".txt") && !fileName.endsWith(".pdf") && !fileName.endsWith(".doc")  && !fileName.endsWith(".docx")  && !fileName.endsWith(".png")  && !fileName.endsWith(".gif")
						 && !fileName.endsWith(".ppt")  && !fileName.endsWith(".xls")  && !fileName.endsWith(".xlsx")  && !fileName.endsWith(".rtf") && !fileName.endsWith(".tif")
						 && !fileName.endsWith(".msg") && !fileName.endsWith(".jpg") && !fileName.endsWith(".jpeg")) {
					log.debug("Not allowable fileName "+fileName);

					output="true";
				}

				//removed code As user can enter other than .exe or .msi
				/*String[] fileNameParts = fileName.split("\\.");
				if (fileNameParts.length > 1) {

					if (fileNameParts[fileNameParts.length - 1].equalsIgnoreCase("exe")
							|| !StringUtils.isAlpha(fileNameParts[fileNameParts.length - 1])
							|| fileNameParts[fileNameParts.length - 1].equalsIgnoreCase("msi")) {

						output="true";
					}
				} else {
					output="true";
				}*/

			}
			}
		}
		response.setContentType("text/plain");
        response.setContentLength(output.length());
		PrintWriter writer = response.getWriter(); // throws IOException
		writer.write(output);
	}

	@RequestMapping(value = "/permissions/contract/multiUpload", method = {RequestMethod.GET, RequestMethod.POST})
	public void multiFileUpload(HttpServletRequest request, HttpServletResponse response,
			@ModelAttribute(MultiUploadForm.FORM_MODEL_NAME) MultiUploadForm form,
			@RequestParam(value = "cwId", required = false) Integer cwId,
			@RequestParam(value = "assetUseIds", required = false) String auIDs)
			throws Exception {

		log.debug("Inside multiFileUpload(): cwId =" + cwId + ", assetUseIds =" + auIDs);

		List<MultipartFile> fileslist = form.getFiles();
		Map<String, MultipartFile> tempMap = null;
		Contract contractWithFiles = null;
		MultipartFile mpf = null;
		List<Integer> auIds = null;
		Set <Integer> auIdsSet = null;
		PrintWriter writer = response.getWriter();
		//Document upload status string constants
		final String successfullyUploadedStatus = "Successfully Uploaded";
		final String successfullyReplacedStatus = "Successfully Replaced";
		final String failedUploadStatus = "Failed Upload";
		final String documentNotFoundStatus = "Document not found";
		final String fileNamesMissingStatus = "Contract Filename(s) missing";


		if(CollectionUtils.isNotEmpty(fileslist) && StringUtils.isNotBlank(auIDs) && cwId != 0) {

			//First make an entry in upload_docs_history with upload_in_progress=true.
			UserPrincipal user = PermUserContext.getCurrentUser(request);
			UploadDocumentsHistory uploadDocsHistory = new UploadDocumentsHistory();
			uploadDocsHistory.setCwId(cwId);
			uploadDocsHistory.setUploadedDate(new Date());
			uploadDocsHistory.setUploadedUserId(user.getId());
			uploadDocsHistory.setUploadInprogress(true);
			uploadDocsHistory = contractRepository.saveRequiresNew(uploadDocsHistory);

			//Remove the below code... sleep method has put to make the process delay for testing purpose (other task)
			for (int ti=0; ti<15; ti++) {//to stop the execution of this method for 15 seconds
				Thread.sleep(1000);//Remove this code before moving code to Production.
			}

			try {
				auIdsSet = new HashSet<Integer>();

				tempMap = new HashMap<String, MultipartFile>();
				for (MultipartFile file : fileslist) {
					log.debug("OriginalFileName = " + file.getOriginalFilename());
					tempMap.put(file.getOriginalFilename().toLowerCase(), file);
				}

				String [] assetUseIds = auIDs.split(",");
				if(assetUseIds.length > 0) {
					for(int i=0; i<assetUseIds.length; i++) {
						Integer auId = Integer.valueOf(assetUseIds[i]);
						AssetUse au = assetUseRepository.lazyLoad(AssetUse.class, auId, new String[] {"asset","component"});
						if(null == au) {
							continue;
						}
						Asset asset = assetRepository.lazyLoad(Asset.class, au.getAsset().getId(), new String[] {"sources"});

						List<Contract> contracts = new ArrayList<Contract>();
						contracts = contractRepository.loadListForAssetCW (au.getAsset().getId(), cwId);
						if(CollectionUtils.isEmpty(contracts)) {
							List<Integer> contractIdList = assetUseRepository.getLatestContractIds(auId);
							for(Integer contractId : contractIdList) {
								Contract c = contractRepository.loadContractById(contractId);
								contracts.add(c);
							}
						}

						if(CollectionUtils.isNotEmpty(contracts)) {
							for(Contract contract : contracts) {
								contractWithFiles = contractRepository.lazyLoad(Contract.class, contract.getId(), new String[] { "files", "fileNames"});
								List<ContractFile> contractFiles = contractWithFiles.getFiles();
								//To update/override the existing contract file
								if(CollectionUtils.isNotEmpty(contractFiles)) {
									for(ContractFile cFile : contractFiles) {
										mpf = tempMap.get(cFile.getFileName().toLowerCase());
										if(null != mpf) {
											contractRepository.removeFileFromContract(cFile);
											if(mpf.getSize() <= maximumContractFileSize) {
												ContractFile cFile1 = new ContractFile();
												cFile1.setFileData(mpf.getBytes());
												cFile1.setFileName(mpf.getOriginalFilename());
												cFile1.setMimeType(mpf.getContentType());
												cFile1.setContract(contractWithFiles);
												cFile1.setDescription("contract file");

												contractRepository.save(cFile1);

												auIds = contractService.refreshAssetIndex (contractWithFiles.getId());
												auIdsSet.addAll(auIds);

												//Now insert a record into uploaded_docs_details as document upload replaced the existing file
												saveUploadDocDetails( au, asset, uploadDocsHistory, successfullyReplacedStatus, mpf.getOriginalFilename());

												log.debug("multiFileUpload: File: '"+ mpf.getOriginalFilename() +"' Has " + mpf.getSize() + " bytes");
											} else {
												log.debug("multiFileUpload(): File TOO LARGE - has " + mpf.getSize() + " bytes");
												//Now insert a record into uploaded_docs_details as document upload failed
												saveUploadDocDetails( au, asset, uploadDocsHistory, failedUploadStatus, mpf.getOriginalFilename());
											}
										}
									}
								}
								//new code Added By santhosh
								List<ContractFileName> contractFileNames = contractWithFiles.getFileNames();
								if(CollectionUtils.isNotEmpty(contractFileNames)) {
									for(ContractFileName cFileName : contractFileNames) {
								log.debug("Contract File name----->"+cFileName.getFileName().toLowerCase());
								String[] filessplit = cFileName.getFileName().toLowerCase().split(",");

								ArrayList<String> newlist = new ArrayList<String>();
								boolean contains=false;
								for(String sp: filessplit) {
									if(CollectionUtils.isNotEmpty(contractFiles)) {
										for(ContractFile file1: contractFiles) {
											log.debug("Contract File name inside for "+file1.getFileName().toLowerCase());
										if(file1.getFileName().toLowerCase().equals(sp)) {
											log.debug("Contract File exists ----->"+sp);
											contains=true;
											break;
										}
										}
										if(!contains) {
										newlist.add(sp);
										}
										else {
											contains=false;
										}
									}
								}

								for(String spl: newlist) {
									log.debug("inside for--->"+spl);
									mpf = tempMap.get(spl.toLowerCase());
									if(null != mpf) {
										if(mpf.getSize() <= maximumContractFileSize) {
											ContractFile newFile = new ContractFile();
											newFile.setFileData(mpf.getBytes());
											newFile.setFileName(mpf.getOriginalFilename());
											newFile.setMimeType(mpf.getContentType());
											newFile.setContract(contractWithFiles);
											newFile.setDescription("contract file");

											contractRepository.save(newFile);

											//After saving the contract file, set filename upload status to true for not showing on UI
											cFileName.setFileUploaded(true);
											cFileName.setContract(contractWithFiles);

											contractRepository.save(cFileName);

											auIds = contractService.refreshAssetIndex (contractWithFiles.getId());
											auIdsSet.addAll(auIds);

											//Now insert a record into uploaded_docs_details as document upload success
											saveUploadDocDetails( au, asset, uploadDocsHistory, successfullyUploadedStatus, mpf.getOriginalFilename());

											log.debug("multiFileUpload: File: '"+ mpf.getOriginalFilename() +"' Has " + mpf.getSize() + " bytes");

										} else {
											log.debug("multiFileUpload(): File TOO LARGE - has " + mpf.getSize() + " bytes");
											//Now insert a record into uploaded_docs_details as document upload failed
											saveUploadDocDetails( au, asset, uploadDocsHistory, failedUploadStatus, mpf.getOriginalFilename());
										}
									} else {
										//Now insert a record into uploaded_docs_details as document not found in folder for the filename
										saveUploadDocDetails( au, asset, uploadDocsHistory, documentNotFoundStatus, spl.toLowerCase());
									}

									}
								if(CollectionUtils.isEmpty(contractFiles)){
									for(String sp1: filessplit) {
										log.debug("inside else if contract files empty--->"+sp1);
										mpf = tempMap.get(sp1.toLowerCase());
										if(null != mpf) {
											if(mpf.getSize() <= maximumContractFileSize) {
												ContractFile newFile = new ContractFile();
												newFile.setFileData(mpf.getBytes());
												newFile.setFileName(mpf.getOriginalFilename());
												newFile.setMimeType(mpf.getContentType());
												newFile.setContract(contractWithFiles);
												newFile.setDescription("contract file");

												contractRepository.save(newFile);

												//After saving the contract file, set filename upload status to true for not showing on UI
												cFileName.setFileUploaded(true);
												cFileName.setContract(contractWithFiles);

												contractRepository.save(cFileName);

												auIds = contractService.refreshAssetIndex (contractWithFiles.getId());
												auIdsSet.addAll(auIds);

												//Now insert a record into uploaded_docs_details as document upload success
												saveUploadDocDetails( au, asset, uploadDocsHistory, successfullyUploadedStatus, mpf.getOriginalFilename());

												log.debug("multiFileUpload: File: '"+ mpf.getOriginalFilename() +"' Has " + mpf.getSize() + " bytes");
											} else {
												log.debug("multiFileUpload(): File TOO LARGE - has " + mpf.getSize() + " bytes");
												//Now insert a record into uploaded_docs_details as document upload failed
												saveUploadDocDetails( au, asset, uploadDocsHistory, failedUploadStatus, mpf.getOriginalFilename());
											}
										} else {
											//Now insert a record into uploaded_docs_details as document not found in folder for the filename
											saveUploadDocDetails( au, asset, uploadDocsHistory, documentNotFoundStatus, sp1.toLowerCase());
										}
								}

									}

								}
									}

								else {
									//Now insert a record into uploaded_docs_details as file names missing
									saveUploadDocDetails( au, asset, uploadDocsHistory, fileNamesMissingStatus, "");
								}
								//End new Code By santhosh

								//To add new contract file for the contract by comparing file names (for filenames saved from spreadsheet upload)
								/*List<ContractFileName> contractFileNames = contractWithFiles.getFileNames();
								if(CollectionUtils.isNotEmpty(contractFileNames)) {
									for(ContractFileName cFileName : contractFileNames) {
										mpf = tempMap.get(cFileName.getFileName().toLowerCase());
										if(null != mpf) {
											if(mpf.getSize() <= maximumContractFileSize) {
												ContractFile newFile = new ContractFile();
												newFile.setFileData(mpf.getBytes());
												newFile.setFileName(mpf.getOriginalFilename());
												newFile.setMimeType(mpf.getContentType());
												newFile.setContract(contractWithFiles);
												newFile.setDescription("contract file");

												contractRepository.save(newFile);

												//After saving the contract file, set filename upload status to true for not showing on UI
												cFileName.setFileUploaded(true);
												cFileName.setContract(contractWithFiles);

												contractRepository.save(cFileName);

												auIds = contractService.refreshAssetIndex (contractWithFiles.getId());
												auIdsSet.addAll(auIds);

												//Now insert a record into uploaded_docs_details as document upload success
												saveUploadDocDetails( au, asset, uploadDocsHistory, successfullyUploadedStatus, mpf.getOriginalFilename());

												log.debug("multiFileUpload: File: '"+ mpf.getOriginalFilename() +"' Has " + mpf.getSize() + " bytes");
											} else {
												log.debug("multiFileUpload(): File TOO LARGE - has " + mpf.getSize() + " bytes");
												//Now insert a record into uploaded_docs_details as document upload failed
												saveUploadDocDetails( au, asset, uploadDocsHistory, failedUploadStatus, mpf.getOriginalFilename());
											}
										} else {
											//Now insert a record into uploaded_docs_details as document not found in folder for the filename
											saveUploadDocDetails( au, asset, uploadDocsHistory, documentNotFoundStatus, cFileName.getFileName());
										}
									}
						} else {
									//Now insert a record into uploaded_docs_details as file names missing
									saveUploadDocDetails( au, asset, uploadDocsHistory, fileNamesMissingStatus, "");
								}*/
							}
						}
					}
				}
				//upload_docs_history with upload_in_progress=false before exit.
				uploadDocsHistory.setUploadInprogress(false);
				contractRepository.save(uploadDocsHistory);
				log.debug("after setting upload in progress to false : ");
			} catch (Exception ex) {
				//upload_docs_history with upload_in_progress=false before exit if at all exception occurred.
				uploadDocsHistory.setUploadInprogress(false);
				contractRepository.save(uploadDocsHistory);
				log.error("Error occurred : ", ex.fillInStackTrace());
			} finally {
				//send email to the triggered user if exception occurs or not -- so this code placed in finally
				try {
					Integer productId = productRepository.getPrimaryProductId(cwId);
					Product primaryProduct = null;
					primaryProduct = productRepository.lazyLoad(Product.class, productId, new String[] {"users"});
					commonWorkService.sendEmailToUploadDocsInitiator(user.getEmail(), primaryProduct.getAuthorsAsString(),
															primaryProduct.getTitle(), primaryProduct.getIsbn(), cwId.toString(), String.valueOf(uploadDocsHistory.getId()));
				} catch (Exception ex) {
					log.error("Exception occurred while sending email notification : ", ex.fillInStackTrace());
				}

				//Update the same document upload status to Product History
				try {
					String message = "Documents uploaded: Click <a href=\"javascript:download('?cwId="+cwId+"&uploadHistoryId="+uploadDocsHistory.getId()+"')\">here</a>";
					commonWorkService.addToCwHistory_UploadDocsHistory(cwId, message);
				} catch (Exception ex) {
					log.error("Exception occurred while updating upload status to Product History : ", ex.fillInStackTrace());
				}
			}
		}
		String output = ObjectToJson.doTransform(auIdsSet, null);

		log.debug("Asset use ids to be refreshed on product landing page :: sending: " + output);

		response.setContentType("application/json");
		writer.write(output);
	}

	private void saveUploadDocDetails(AssetUse au, Asset asset, UploadDocumentsHistory uploadDocsHistory, String status, String fileName) throws PersistenceException{
		log.debug("Inside saveUploadDocDetails(): ");
		try {
			UploadedDocumentsDetails uploadDocDetails = new UploadedDocumentsDetails();
			uploadDocDetails.setUploadHistoryId(uploadDocsHistory);
			if(null != au.getComponent()) {
				uploadDocDetails.setComponent(au.getComponent().getName());
			}
			if (null != asset.getMediaType()) {
				uploadDocDetails.setMediaType(asset.getMediaType().getDescription());
			}
			uploadDocDetails.setDescription(asset.getDescription());
			if (null != asset.getSource(0) && null != asset.getSource(0).getName()) {
				uploadDocDetails.setSource(asset.getSource(0).getName());
			}
			if (null != asset.getVendorId()) {
				uploadDocDetails.setSourceRefNumber(asset.getVendorId());
			}
			uploadDocDetails.setDocumentName(fileName);
			uploadDocDetails.setUploadStatus(status);
			contractRepository.save(uploadDocDetails);
		} catch (Exception ex) {
			log.error("Error occurred in saveUploadDocDetails (): ", ex.fillInStackTrace());
		}
	}

	@RequestMapping(value = "/permissions/contract/isMultiUploadInProgress", method = RequestMethod.POST)
	public void isMultiUploadInProgress(HttpServletRequest request, HttpServletResponse response,
			@RequestParam(value = "commonWorkId", required = false) String cwId) throws Exception {
		log.debug("Inside isMultiUploadInProgress(): cwId =" + cwId);
		int cwid = Integer.parseInt(cwId);
		String output = "false";

		if(cwid != 0 && contractRepository.isMultiUploadInProgress(cwid)) {
			output = "true";
		}
		response.setContentType("text/plain");
        response.setContentLength(output.length());
		PrintWriter writer = response.getWriter();
		writer.write(output);
	}
	//End: Added for DM-532

	/*@GetMapping(value = "/permissions/contract/upload_file")*/
	@RequestMapping(value = "/permissions/contract/upload_file", method = {RequestMethod.GET, RequestMethod.POST})
	public void uploadFile(HttpServletRequest request, HttpServletResponse response,
			@ModelAttribute(UploadForm.FORM_MODEL_NAME) UploadForm form,
			@RequestParam(value = "contractId", required = false) Integer contractId)
			throws Exception {
		MultipartFile mpf = form.getFile();
		Contract contract = contractRepository.lazyLoad(Contract.class, contractId,
				new String[] { "files"});

		PrintWriter writer = response.getWriter(); // throws IOException
		// create the JSON object to be sent to the dialog
		Map<String, Object> map = new HashMap<String, Object>();

		if (mpf != null && mpf.getSize() > 0) {
			if (mpf.getSize() <= maximumContractFileSize) {
				ContractFile newFile = new ContractFile();

				newFile.setFileData(mpf.getBytes());
				newFile.setFileName(mpf.getOriginalFilename());
				newFile.setMimeType(mpf.getContentType());
				newFile.setContract(contract);
				newFile.setDescription("contract file");

				// contract.getFiles().add(newFile);

				contractRepository.save(newFile);
				// refreshes the index so the invoice icon shows in the UI
				List<Integer> auIds = contractService.refreshAssetIndex (contractId);

				// this was creating a detached entity error and there really
				// is no point on saving the contract here
				// so now only the file is persisted.
				// contractRepository.save(contract);
				map.put("auids", auIds);

				log.debug("uploadFile(): File Has " + mpf.getSize() + " bytes");
				map.put("message", "File " + mpf.getOriginalFilename() + " (" + mpf.getSize() + " bytes) uploaded");
			}
			else {
				log.info("uploadFile(): File TOO LARGE - has " + mpf.getSize() + " bytes");
				map.put("message", "File too large. Maximum size is " + maximumContractFileSize + " bytes.");
			}
		}
		String output = ObjectToJson.doTransform(map, null); // throws InitialisationException,

		log.debug("uploadFile(): sending:\r\n" + output);

		response.setContentType("application/json");
		// Don't set contentLength because String.length() is not the same
		// as #bytes if there are double-byte chars
		// response.setContentLength(output.length());
		writer.write(output);
	}

	/*@GetMapping(value = "/wizard_upload_file")*/
	@RequestMapping(value = "/permissions/contract/wizard_upload_file", method = {RequestMethod.GET, RequestMethod.POST})
	public void wizardUploadFile(HttpServletRequest request, HttpServletResponse response,
			@ModelAttribute(UploadForm.FORM_MODEL_NAME) UploadForm form)
			throws Exception {
		log.debug("wizardUploadFile(): entered...");
		MultipartFile mpf = form.getFile();
		ContractWizardForm cForm = (ContractWizardForm) request.getSession().getAttribute(FORM_NAME);

		if (mpf != null && mpf.getSize() > 0) {
			PrintWriter writer = response.getWriter(); // throws IOException
			response.setContentType("text/plain");

			if (mpf.getSize() <= maximumContractFileSize) {
				ContractFile newFile = new ContractFile();

				newFile.setFileData(mpf.getBytes());
				newFile.setFileName(mpf.getOriginalFilename());
				newFile.setMimeType(mpf.getContentType());
				newFile.setContract(cForm.getContract());
				newFile.setDescription("contract file");
				cForm.getContract().getFiles().add(newFile);

				log.debug("wizardUploadFile(): File Has " + mpf.getSize() + " bytes");
				writer.write("File " + mpf.getOriginalFilename() + " (" + mpf.getSize() + " bytes) uploaded");
			}
			else {
				log.info("wizardUploadFile(): File TOO LARGE - has " + mpf.getSize() + " bytes");
				writer.write("File too large. Maximum size is " + maximumContractFileSize + " bytes.");
			}
		}
	}

	@RequestMapping(value = "/permissions/contract/reinvoice_start", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView reinvoiceStart(HttpServletRequest request, @RequestParam(value = "auId") Integer auId)
			throws Exception {
		log.debug("reinvoiceStart(): entered...");
		// load minimum information here. If you need extra information,
		// load it on the specific branch (see authorProvided example)
		ContractWizardForm form = new ContractWizardForm();
		form.setAddress(new Address());
		form.setAssetRepository(assetRepository);
		form.setRerequest(true);

		ModelAndView mv = null;
		AssetUse au = getAssetUseRepository().loadAssetUseById(auId);
		List<Source> sources = getAssetUseRepository().loadSourceForStatus(auId, PermissionStatus.REREQUEST_GROUP);
		if (CollectionUtils.isEmpty(sources)) {
			mv = new ModelAndView("dialog.success");
			mv.addObject("message", "No source for status granted");
		}
		else if (sources.size() == 1) {
			mv = new ModelAndView("redirect:/sapp/permissions/contract/select_source?sourceId=" + sources.get(0).getId());
			form.setPrevUrl(null);
		}
		else {
			mv = new ModelAndView("pages.contract.sources");
			mv.addObject("sources", sources);
			form.setPrevUrl("/sapp/permissions/contract/asset_start?auId=" + auId);
		}

		form.setCwId(au.getCommonWork().getId());
		form.setAssetId(au.getAsset().getId());

		String dateFormat = PermUserContext.getPickerDateFormat(request);
		mv.addObject("dateFormat",dateFormat);

		mv.addObject(FORM_NAME, form);

		return mv;
	}

	@RequestMapping(value = "/permissions/contract/asset_start", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView assetStart(@RequestParam(value = "auId") Integer auId,
			@RequestParam(value = "custom", required = false) Boolean isCustom)
			throws Exception {
		log.debug("assetStart(): entered...");
		// load minimum information here. If you need extra information,
		// load it on the specific branch (see authorProvided example)
		ContractWizardForm form = new ContractWizardForm();
		form.setAddress(new Address());
		form.setAssetRepository(assetRepository);
		ModelAndView mv = null;
		AssetUse au = getAssetUseRepository().loadAssetUseById(auId);
		List<Source> sources = new ArrayList<Source>();
		if (au.getImportSource() == null) {
			sources = assetUseRepository.loadSourceForStatus(auId, PermissionStatus.FORM_SENT_GROUP);
		} else {

			//Ram added below code for invoice extension issue

			au.setImportSource(null);
			form.setImportSource(importSource);
			log.debug("au.getImportSource(): entered,Contract Wizard file  importSource = " +au.getImportSource());
			sources = assetUseRepository.loadSourceForStatus(auId, PermissionStatus.MIGRATED_GROUP_FORM_SENT);

		//sources = assetUseRepository.loadSourceForStatus(auId, PermissionStatus.MIGRATED_GROUP);

		//Ends here
		}

		if (CollectionUtils.isEmpty(sources)) {
			mv = new ModelAndView("dialog.success");
			mv.addObject("message", "No source for status unrequested");
		}
		else if (sources.size() == 1) {
			mv = new ModelAndView("redirect:/sapp/permissions/contract/select_source?sourceId=" + sources.get(0).getId()+"&commonwid="+au.getCommonWork().getId());
			form.setPrevUrl(null);
		}
		else {
			mv = new ModelAndView("pages.contract.sources");
			mv.addObject("sources", sources);
			form.setPrevUrl("/sapp/permissions/contract/asset_start?auId=" + auId);
		}

		form.setCwId(au.getCommonWork().getId());
		form.setAssetId(au.getAsset().getId());
		mv.addObject(FORM_NAME, form);

		return mv;
	}

	@RequestMapping(value = "/permissions/contract/custom_start", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView customStart(@RequestParam(value = "sourceId") Integer sourceId,
			@RequestParam(value = "cwId") Integer cwId,
			@RequestParam(value = "currency") String currency,
			@RequestParam(value = "data") String data)
			throws Exception {
		log.debug("customStart(): entered...");
		// load minimum information here. If you need extra information,
		// load it on the specific branch (see authorProvided example)
		ContractWizardForm form = new ContractWizardForm();
		form.setAddress(new Address());
		form.getContract().setCurrency(new Currency (currency, null));
		form.setAssetRepository(assetRepository);
		form.setCwId(cwId);
		form.setData(data);

		ModelAndView mv = new ModelAndView("redirect:/sapp/permissions/contract/select_source?sourceId=" + sourceId);
		form.setPrevUrl(null);
		mv.addObject(FORM_NAME, form);
		return mv;
	}

	@RequestMapping(value = "/permissions/contract/source_start", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView sourceStart(@RequestParam("sourceId") int sourceId,
			@RequestParam("cwId") int cwId,
			@RequestParam(value = "rerequest", required=false) boolean rerequest,
			@RequestParam(value = "permissionRequest", required=false) boolean permissionRequest,
			@RequestParam(value = "auId", required=false) Integer auId,
			@RequestParam(value = "wizardOwnerType", required=false) String wizardOwnerType,
			@RequestParam(value = "appliedOnline", required=false) boolean appliedOnline
			)
			throws Exception {
		log.debug("sourceStart(): entered, rerequest = " + rerequest);
		// load minimum information here. If you need extra information,
		// load it on the specific branch (see authorProvided example)
		ContractWizardForm form = new ContractWizardForm();
		form.setAddress(new Address());
		form.setAssetRepository(assetRepository);
		form.setCwId(cwId);
		form.setRerequest(rerequest);
		form.setPermissionRequest(permissionRequest);
		form.setAppliedOnline(appliedOnline);
		//Added for Paperwork Task C Starts
		form.setWizardOwnerType(wizardOwnerType);
		//Added for Paperwork Task C ends
       	log.debug("soureStart(): selected auId: " + auId);

		if (null != auId && auId != 0) {
			log.debug("IN IF BLOCK OF AUID");
			AssetUse au = getAssetUseRepository().lazyLoad(AssetUse.class, auId, new String[] {"asset"});
			form.setAssetId(au.getAsset().getId());
			log.debug("Asset OwnerType --> "+au.getAsset().getOwnerType());
			if(au.getAsset().getOwnerType().equals(OwnerType.PUBLIC_DOMAIN)){
				log.debug("GUESS IS CORRECT");
				form.setWizardOwnerType("Public Domain");
			}
		}

		ModelAndView mv = new ModelAndView("redirect:/sapp/permissions/contract/select_source?sourceId=" + sourceId);
		mv.addObject(FORM_NAME, form);

		return mv;
	}

	@RequestMapping(value = "/permissions/contract/view_asset_start", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView viewAssetStart(@RequestParam(value = "auId") Integer auId,
			@RequestParam(value = "custom", required = false) Boolean isCustom) throws PersistenceException {
		log.debug("viewAssetStart(): entered...");
		ModelAndView mv = null;
		// load minimum information here. If you need extra information,
		// load it on the specific branch (see authorProvided example)
		ContractWizardForm form = new ContractWizardForm();
		form.setAddress(new Address());
		form.setAssetRepository(assetRepository);
		form.setReadOnly(true);

		AssetUse au = getAssetUseRepository().loadAssetUseById(auId);
		List<Contract> contracts = contractRepository.loadMostRecentContractList(au.getCommonWork().getId(), 0, au.getAsset().getId());
		//Added for Sr_195127
		  if (CollectionUtils.isEmpty(contracts)) {
			  List<Integer> list = assetUseRepository.getLatestContractIds(auId);
			  for(Integer i : list){
			  Contract c=contractRepository.loadContractById(i);
			  contracts.add(c);
			  }
		  }
		log.debug("viewAssetStart(): " + contracts);
		if (CollectionUtils.isEmpty(contracts)) {
			mv = new ModelAndView("dialog.success");
			mv.addObject("message", "No contracts found");
			mv.addObject("redirectUrl", "/sapp/cwlanding/scroll?cwid=" + au.getCommonWork().getId());
		}
		else if (contracts.size() == 1) {
			mv = new ModelAndView("redirect:/sapp/permissions/contract/select_contract?cid=" + contracts.get(0).getId()+"&commonwid="+au.getCommonWork().getId());
			form.setPrevUrl("");
		}
		else {
			mv = new ModelAndView("pages.contract.contractlist");
			form.setPrevUrl("/sapp/permissions/contract/view_asset_start?auId=" + auId);
			mv.addObject("contracts", contracts);
			mv.addObject("commonwid", au.getCommonWork().getId());
		}
		mv.addObject(FORM_NAME, form);
		return mv;
	}

	@RequestMapping(value = "/permissions/contract/edit_asset_start", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView editAssetStart(@RequestParam(value = "auId") Integer auId,
			@RequestParam(value = "custom", required = false) Boolean isCustom)
			throws Exception {
		log.debug("editAssetStart(): entered..., auId = " + auId + ", isCustom = " + isCustom);
		ModelAndView mv = null;
		// load minimum information here. If you need extra information,
		// load it on the specific branch (see authorProvided example)
		ContractWizardForm form = new ContractWizardForm();
		form.setAddress(new Address());
		form.setAssetRepository(assetRepository);
		form.setInEditAsset(true);//Added for DM-374

		AssetUse au = getAssetUseRepository().loadAssetUseById(auId);
		form.setAssetId(au.getAsset().getId());//Added for DM-122

		//Modified for Paperwork change Starts
		List<Contract> contracts = new ArrayList<Contract>();
		if(StringUtils.isBlank(au.getReusedISBN()) && StringUtils.isBlank(au.getPickupISBN())){
			  log.debug("PICKUP ISBN IF ");
			  contracts = contractRepository.loadMostRecentContractList(au.getCommonWork().getId(), 0, au.getAsset().getId());
			//Added for Sr_195127
			  if (CollectionUtils.isEmpty(contracts)) {
				  List<Integer> list = assetUseRepository.getLatestContractIds(auId);
				  for(Integer i : list){
				  Contract c=contractRepository.loadContractById(i);
				  contracts.add(c);
				  }
			  }
		}else{
			  Integer cw_Id = 0;
			//Starts -- Modified for pulling the correct Contract Details View/Edit link
			  String isbn13 = au.getPickupISBN() != null ?  au.getPickupISBN() : au.getReusedISBN();
			  contracts = contractRepository.loadMostRecentContractList(au.getCommonWork().getId(), 0, au.getAsset().getId());
			  if (CollectionUtils.isEmpty(contracts)) {
				  if(StringUtils.isNotBlank(isbn13) && au.getImportSource() != null){
					  cw_Id = contractRepository.loadProductCwId(isbn13);
					  contracts = contractRepository.loadMostRecentContractList(cw_Id, 0, au.getAsset().getId());
				  }else{
					  contracts = contractRepository.loadMostRecentContractList(au.getCommonWork().getId(), 0, au.getAsset().getId());
				  }
			  }
			  if (CollectionUtils.isEmpty(contracts)) {
				  List<Integer> list = assetUseRepository.getLatestContractIds(auId);
				  for(Integer i : list){
				  Contract c=contractRepository.loadContractById(i);
				  contracts.add(c);
				  }
			  }
			 /* if(au.getReusedISBN() != null){
				  cw_Id = contractRepository.loadProductCwId(au.getReusedISBN());
			  }else{
				  scw_Id = contractRepository.loadProductCwId(au.getPickupISBN());
			  }*/
			//Ends -- Modified for pulling the correct Contract Details View/Edit link
			  log.debug("PICKUP ISBN ELSE ");
			  log.debug("PICKUP ISBN ELSE "+cw_Id);
			  //contracts = contractRepository.loadMostRecentContractList(cw_Id, 0, au.getAsset().getId());
		}
		//Modified for Paperwork change Ends

		log.debug("editAssetStart(): contracts: " + contracts);
		if (CollectionUtils.isEmpty(contracts)) {
			mv = new ModelAndView("dialog.success");
			mv.addObject("message", "No contracts found");
			mv.addObject("redirectUrl", "/sapp/cwlanding/scroll?cwid=" + au.getCommonWork().getId());
		}
		else if (contracts.size() == 1) {
			mv = new ModelAndView("redirect:/sapp/permissions/contract/select_contract?cid=" + contracts.get(0).getId()+"&commonwid="+au.getCommonWork().getId());//added current commonwid for 96553
			form.setPrevUrl("");
		}
		else {
			mv = new ModelAndView("pages.contract.contractlist");
			form.setPrevUrl("/sapp/permissions/contract/edit_asset_start?auId=" + auId);
			mv.addObject("contracts", contracts);
			mv.addObject("commonwid", au.getCommonWork().getId());
		}
		mv.addObject(FORM_NAME, form);
		return mv;
	}

	@RequestMapping(value = "/permissions/contract/view_start", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView viewStart(@RequestParam(value = "cId") Integer contractId, HttpServletRequest request)
			throws Exception {
		log.debug("viewStart(): entered..., contractId = " + contractId);

		ModelAndView mv = null;
		ContractWizardForm form = new ContractWizardForm();

		Contract contract = contractRepository.lazyLoad(Contract.class, contractId,
			new String[] { "compCopies", "currency", "assets", "source", "contractType.description"});

		form.setUseLimitationOfLiabilityLogic(false);
		for (ContractAsset cAsset: contract.getAssets()) {
			Asset asset = assetRepository.loadAssetById(cAsset.getAssetBaseId());
			if (MediaType.PHOTO.equals(asset.getMediaType())) {
				form.setUseLimitationOfLiabilityLogic(true);
				break;
			}
		}
		form.setContract(contract);

		form.setSource(contract.getSource());
		List<POAssetView> poAssetViews = loadPoAssetView (form, request);
		// add file details to contract
		Contract wContract = contractRepository.loadContractWithFiles(contractId);
		contract.setFiles(wContract.getFiles());

		String conditions = contractRepository.loadContractPermissionInfo(contractId);
		log.debug("viewStart(): " + contract);
		mv = new ModelAndView("pages.contract.view");
		mv.addObject("contract", contract);
		mv.addObject("conditions", StringUtils.isEmpty(conditions) ? "N/A" : conditions);
		mv.addObject("assets", poAssetViews);
		return mv;
	}

	@RequestMapping(value = "/permissions/contract/edit_start", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView editStart(HttpServletRequest request, @RequestParam(value = "sourceId") Integer sourceId,
			@RequestParam(value = "cwId") Integer cwId)
			throws Exception {
		log.debug("editStart(): entered..."+cwId);

		ModelAndView mv = null;
		List<Contract> contracts=null;
		Integer cwIdForPickUpIsbn= null;
		// load minimum information here. If you need extra information,
		// load it on the specific branch (see authorProvided example)
		ContractWizardForm form = new ContractWizardForm();
		form.setAddress(new Address());
		form.setAssetRepository(assetRepository);

		contracts = contractRepository.loadMostRecentContractList(cwId, sourceId, 0);
		log.debug("editStart(): " + contracts);
		//96553 changes for View/Edit link when contract is not in current commonwid, then fetch from original contract
		if (CollectionUtils.isEmpty(contracts) || contracts == null) {

			AssetUse asstuse = getAssetUseRepository().loadAssetUseforAssetId(cwId,sourceId);
			log.debug("pickup isbn from the asset repository for current "+asstuse.getPickupISBN()+"asset id : "+asstuse.getAsset().getId());
			cwIdForPickUpIsbn= getAssetUseRepository().getCwIdForIsbn(asstuse.getPickupISBN());
			if(cwIdForPickUpIsbn!=null){
			log.debug("cwIdForPickUpIsbn --->"+cwIdForPickUpIsbn);
			contracts = contractRepository.loadMostRecentContractList(cwIdForPickUpIsbn, sourceId, asstuse.getAsset().getId());
			}
		}


		if (CollectionUtils.isEmpty(contracts)) {
			mv = new ModelAndView("dialog.success");
			mv.addObject("message", "No contracts found");
			mv.addObject("redirectUrl", "/sapp/cwlanding/scroll?cwid=" + cwId);
		}
		else if (contracts!=null && contracts.size() == 1) {
			mv = new ModelAndView("redirect:/sapp/permissions/contract/select_contract?cid=" + contracts.get(0).getId()+"&commonwid="+cwId);
			form.setPrevUrl("");
		}

		else {
			log.debug(" Inside else ");
			mv = new ModelAndView("pages.contract.contractlist");
			form.setPrevUrl("/sapp/permissions/contract/edit_start?sourceId=" + sourceId + "&cwId=" + cwId);
				mv.addObject("contracts", contracts);
				mv.addObject("commonwid",cwId);
			}

		mv.addObject(FORM_NAME, form);


		return mv;
	}

	@RequestMapping(value = "/permissions/contract/select_contract", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView selectContract(@RequestParam(value = "cid") int cid,@RequestParam(value = "commonwid") int commonwid,
			@ModelAttribute(FORM_NAME) ContractWizardForm form)
			throws Exception {
		log.debug("selectContract(): entered..., cid = " + cid);

		ModelAndView mv = null;
		Contract contract = contractRepository.lazyLoad(Contract.class, cid,
				new String[] { "source", "assets", "commonWork", "purchaseOrder", "currency", "compCopies", "files", "contractType" });

		if (ImportSource.FROM_RIGHTS_LINK.equals(contract.getImportSource())) {  // order matters since 2nd may be null
			// redirect to Rights Link UI
			return new ModelAndView("redirect:/sapp/landing/license/edit?contractId=" + cid);
		}

		Source source = sourceRepository.lazyLoad(Source.class, contract.getSource().getId(), new String[] {"contacts"});
		if (null == source) {
			mv = new ModelAndView("dialog.success");
			mv.addObject("message", "No source found");
			return mv;
		}
		// Zero out the cost for 96553
		log.debug("Current Commonwork id-->"+commonwid+" Original contract cw_id" +contract.getCommonWork().getId());
		if(contract.getCommonWork().getId() !=commonwid){
			contract.setPrice(0.0);
		}
		form.setSource(source);
		form.setContract(contract);
		form.setUseLimitationOfLiabilityLogic(false);
		for (ContractAsset cAsset: contract.getAssets()) {
			Asset asset = assetRepository.loadAssetById(cAsset.getAssetBaseId());
			if (asset.getMediaType().equals(MediaType.PHOTO)) {
				form.setUseLimitationOfLiabilityLogic(true);
				break;
			}
		}
		form.populateEditForm();

		log.debug("selectContract(): form.isReadOnly() = " + form.isReadOnly() +"iiiii  " +contract.getAssetIds().toString());
		// if read only, just go directly to view contract
		if (form.isReadOnly()) {
			mv = new ModelAndView("redirect:/sapp/permissions/contract/view_start?cId=" + contract.getId());
		} else {
			// if permission request, go to restrictions page, else assets page
			if (form.isPermissionRequest()) {
				log.debug("selectContract(): will do contract/request");
				mv = new ModelAndView("redirect:/sapp/permissions/contract/request");
				// we will use this flag to show a message in the dialog
				mv.addObject("isEdit", 1);
			} else {
				log.debug("selectContract(): will do contract/details");
				mv = new ModelAndView("redirect:/sapp/permissions/contract/details?commonwid="+commonwid); // current cwid added for 96553
			}
		}

		mv.addObject(FORM_NAME, form);
		return mv;
	}

	@RequestMapping(value = "/permissions/contract/select_source", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView selectSource(@RequestParam(value = "sourceId") Integer sourceId,
			@ModelAttribute(FORM_NAME) ContractWizardForm form)
			throws Exception {
		log.debug("selectSource(): entered...form.isRerequest() = " + form.isRerequest());

		ModelAndView mv = new ModelAndView("pages.contract.polist");
		Source source = sourceRepository.lazyLoad(Source.class, sourceId, new String[] {"contacts"});
		if (null == source) {
			mv = new ModelAndView("dialog.success");
			mv.addObject("message", "No source found");
		}
		form.setSource(source);
		mv.addObject(FORM_NAME, form);

		List<PurchaseOrder> pos = purchaseOrderRepository.loadMostRecentPurchaseOrderList(form.getCwId(), sourceId, form.getAssetId());
		// order of if/else is very important here - do not change it
		if (CollectionUtils.isEmpty(pos)) {
			// if no PO, we still continue, we can create a contract without PO
			// if permission request, go to restrictions page, else assets page
			if (form.isPermissionRequest()) {
				mv = new ModelAndView("redirect:/sapp/permissions/contract/request");
			} else {
				mv = new ModelAndView("redirect:/sapp/permissions/contract/details?commonwid="+form.getCwId());
			}
		}
		else if (pos.size() == 1) {
			mv = new ModelAndView("redirect:/sapp/permissions/contract/select_po?poId=" + pos.get(0).getId());
		}
		else {
			mv.addObject("pos", pos);
			form.setPrevUrl("/sapp/permissions/contract/select_source?sourceId=" + sourceId);
		}

		return mv;
	}

	@RequestMapping(value = "/permissions/contract/select_po", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView selectPo(@RequestParam(value = "poId") Integer poId,
			@ModelAttribute(FORM_NAME) ContractWizardForm form)
			throws Exception {
		log.debug("selectPo(): entered...");
		form.setPoId(poId);
		PurchaseOrder po = purchaseOrderRepository.lazyLoad(PurchaseOrder.class, poId, new String[] { "assets" });
		for (Asset asset : po.getAssets()) {
			form.getContract().addAssetId(asset.getId());
		}
		form.setPermissionRequest(po.isPermissionRequest());
		ModelAndView mv = null;
		// if permission request, go to restrictions page, else assets page
		if (form.isPermissionRequest()) {
			mv = new ModelAndView("redirect:/sapp/permissions/contract/request");
		} else {
			mv = new ModelAndView("redirect:/sapp/permissions/contract/details?commonwid="+form.getCwId());
		}
		return mv;
	}

	@RequestMapping(value = "/permissions/contract/request", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView request(@ModelAttribute(FORM_NAME) ContractWizardForm form, HttpServletRequest request)
			throws Exception {
		log.debug("request(): entered...");

		ModelAndView mv = new ModelAndView("pages.contract.request");
		mv.addObject("commonwid", form.getCwId());
		return mv;
	}

	@RequestMapping(value = "/permissions/contract/submit_request", method = RequestMethod.POST)
	public ModelAndView requestSubmit(@ModelAttribute(FORM_NAME) ContractWizardForm form,
			HttpServletRequest request)
			throws Exception {
		log.debug("requestSubmit(): entered...");
		log.debug("restrictions selected: " + form.isNoRestrictions());

		ModelAndView mv = null;
		// if no restrictions
		if (form.isPermissionRequest() && form.isNoRestrictions()) {
			 mv = new ModelAndView("redirect:/sapp/permissions/contract/assets?commonwid="+form.getCwId());
		} else {
			 mv = new ModelAndView("redirect:/sapp/permissions/contract/details?commonwid="+form.getCwId());
		}

		return mv;
	}

	// Added for SS Task 12 - Start
	@RequestMapping(value = "/permissions/contract/approve_asset_detail", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView approveAssetDetails(@RequestParam(value = "auId") Integer auId,
			@RequestParam(value = "custom", required = false) Boolean isCustom)
			throws Exception {
		log.debug("approveAssetDetails(): entered...");
		ModelAndView mv = null;
		ContractWizardForm form = new ContractWizardForm();

		AssetUse au = getAssetUseRepository().loadAssetUseById(auId);
		form.setAssetId(au.getAsset().getId().intValue());
		form.setCwId(au.getCommonWork().getId());
		List<Contract> contracts = contractRepository.loadMostRecentContractList(au.getCommonWork().getId(), 0, au.getAsset().getId());
		log.debug("approveAssetDetails(): contracts: " + contracts);
		if (CollectionUtils.isEmpty(contracts)) {
			mv = new ModelAndView("dialog.success");
			mv.addObject("message", "No contracts found");
			mv.addObject("redirectUrl", "/sapp/cwlanding/scroll?cwid=" + au.getCommonWork().getId());
		}
		else if (contracts.size() == 1) {
			mv = new ModelAndView("redirect:/sapp/permissions/contract/approve_select_contract?cid=" + contracts.get(0).getId() + "&auId=" + auId);
			form.setPrevUrl("");
		}
		else {
			mv = new ModelAndView("pages.contract.contractlist");
			form.setPrevUrl("/sapp/permissions/contract/approve_asset_detail?auId=" + auId);
			mv.addObject("contracts", contracts);
			mv.addObject("commonwid", au.getCommonWork().getId());
		}

		mv.addObject(FORM_NAME, form);
		return mv;
	}

	@RequestMapping(value = "/permissions/contract/approve_select_contract", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView approveSelectContract(@RequestParam(value = "cid") int cid, @RequestParam(value = "auId") int auId,
			@ModelAttribute(FORM_NAME) ContractWizardForm form)
			throws Exception {
		log.debug("approveSelectContract(): entered..., cid = " + cid);
		ModelAndView mv = new ModelAndView("pages.contract.approveassetdetails");
		Contract contract = contractRepository.lazyLoad(Contract.class, cid, new String[] {"assets"});
		form.setContract(contract);
		mv.addObject("auId", auId);
		mv.addObject(FORM_NAME, form);
		return mv;
	}

	@RequestMapping(value = "/permissions/contract/submit_ApproveAssetDtl", method = RequestMethod.POST)
	public void submitApproveAssetDetails(@ModelAttribute(FORM_NAME) ContractWizardForm form, @RequestParam(value = "auId", required=false) int auId,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		log.debug("submitApproveAssetDetails(): entered... auId = "+auId);
		Contract contract = form.getContract();

		UserPrincipal user = PermUserContext.getCurrentUser(request);
		Map<String, Object> input = new HashMap<String, Object>();
		input.put("userName", user.getName());
		input.put("approverName", contract.getApproverName());
		input.put("approverRemarks", contract.getApproverRemarks());
		input.put("assetId", form.getAssetId());
		input.put("contractId", contract.getId());

		getContractRepository().saveApproverDetails(input);

		Integer status = getAssetUseService().updateStatusForAssetUse(auId, true);

		log.debug("Status: " + status);

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
		map.put("url", request.getContextPath() + "/sapp/permissions/contract/medium?cwId="
						+ form.getCwId() + "&contractId=" + contract.getId());

		String output = ObjectToJson.doTransform(map, null);  // throws InitialisationException, TransformationException

		log.debug("save(): sending:\r\n" + output);

		response.setContentType("application/json");
		PrintWriter writer = response.getWriter();  // throws IOException
		writer.write(output);
	}
	// Added for SS Task 12 - End

	@RequestMapping(value = "/permissions/contract/details", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView detailsLoad(HttpServletRequest request,@RequestParam(value = "commonwid") int commonwid, @ModelAttribute(FORM_NAME) ContractWizardForm form)
			throws Exception {
		log.debug("detailsLoad(): entered...");
		//log.debug("common work in detailsLoad(): entered..."+commonwid);
		// if new contract, set default currency
		Contract contract = form.getContract();
		// if any of the assets is will be for for hire, then set contract as work for hire
		log.debug("form.getContract().getCopyrightType() --> "+form.getContract().getCopyrightType());
		if (null == form.getContract().getCopyrightType()) {
			for(Integer assetId: contract.getAssetIds()) {
				log.debug("HEREE : ");
				Asset asset = assetRepository.loadAssetById(assetId);
				if(StringUtils.isNotBlank(asset.getOwnerType().toString())){
					if(asset.getOwnerType().getDescription().equals("Public Domain")){
						form.setWizardOwnerType("Public Domain");
						asset.setManaged(false);
					}
					else if(asset.getOwnerType().getDescription().equals("Author Owned")){
						form.setWizardOwnerType("Author Owned");
						asset.setManaged(false);
					}
				}
				//Paperwork Task C Public Domain Ends
				if (asset.isWillBeWorkForHire()) {
					form.getContract().setCopyrightType(CopyrightType.WILEY_OWNED_WORK_FOR_HIRE);
					break;
				}
			}
		}

		if (null == contract.getId() && null == contract.getCurrency()) {
			// default to user profile
			String currencyCode = PermUserContext.getUserSession(request).getCurrentUser().getCurrencyCode();
			contract.setCurrency((Currency) getReferenceDataCache().getObjectByType(
					Currency.class, currencyCode));
		}
		if (CollectionUtils.isNotEmpty(form.getContract().getCompCopies())) {
			// load address data
			for (int x = 0; x < form.getContract().getCompCopies().size(); x++) {
				Address address = getSourceRepository().loadAddressById(form.getContract().getCompCopies().get(x).getAddressId());
				form.getContract().getCompCopies().get(x).setAddress(address);
			}
		}
		// set form.maDealId if appropriate here - it's not set yet
		if (contract.getId() != null && contract.getMasterAgreementDeal() != null) {
			// contract.getMasterAgreementDeal().getId() causes
			// LazyInitializationException: could not initialize proxy - no Session
			// so load from DB
			//log.debug("detailsLoad(): maDealId = " + form.getMaDealId());
			Integer maDealId = contractRepository.loadMaDealIdForContract(contract.getId());
			form.setMaDealId(maDealId);
			//log.debug("detailsLoad(): maDealId = " + form.getMaDealId());
		}

		ModelAndView mv = new ModelAndView("pages.contract.details");
		//Added for Granted Public Domain starts
				//To make the Invoice date to default
				if(form.getAssetId() != 0){
					Asset assetDetails = assetRepository.loadAssetById(form.getAssetId());
					log.debug("one : "+assetDetails.getOwnerType().toString());
					log.debug("one : "+assetDetails.getOwnerType().getDescription());
					log.debug("one : "+assetDetails.getOwnerType().getCode());
					//Added to implement Build DM-374 -- Start
					if(null != assetDetails) {
						if(assetDetails.isManagerApproved()) {
							contract.setDate(new Date());
							if(!form.isInEditAsset()) {
								form.setManagerApproved(true);
							}
						}
					}
					//Added to implement Build DM-374 -- End
					if(StringUtils.isNotBlank(assetDetails.getOwnerType().toString())){
						if(assetDetails.getOwnerType().getDescription().equals("Public Domain")){
							log.debug("hahaaaaa  "+form.getWizardOwnerType());
							form.setWizardOwnerType("Public Domain");
							log.debug("hahaaaaa111  "+form.getWizardOwnerType());
							mv.addObject("isPublicDomain", true);
						}
						//Added for implementing Build RN 151026-001937 - Start
						if(assetDetails.getOwnerType().getDescription().equals("Author Owned")){
							log.debug("Wizard owner type: "+form.getWizardOwnerType());
							form.setWizardOwnerType("Author Owned");
							log.debug("Wizard owner type: "+form.getWizardOwnerType());
							mv.addObject("isAuthorOwned", true);
						}
						//Added for implementing Build RN 151026-001937 - End
					}
				}

				if(contract.getAssetIds().size() > 0){
					for(Integer assetId: contract.getAssetIds()) {
						log.debug("HEREE : ");
					    Asset assetDtls =  assetRepository.loadAssetById(assetId);
					  //Added to implement Build DM-374 -- Start
					    if(null != assetDtls) {
							if(assetDtls.isManagerApproved()) {
								contract.setDate(new Date());
								if(!form.isInEditAsset()) {
									form.setManagerApproved(true);
								}
							}
						}
					  //Added to implement Build DM-374 -- End
					    if(StringUtils.isNotBlank(assetDtls.getOwnerType().toString())) {
							if(assetDtls.getOwnerType().getDescription().equals("Public Domain")) {
								mv.addObject("isPublicDomain", true);
							}
							//Added for implementing Build RN 151026-001937 - Start
							if(assetDtls.getOwnerType().getDescription().equals("Author Owned")) {
								mv.addObject("isAuthorOwned", true);
							}
							//Added for implementing Build RN 151026-001937 - End
						}
					}
				}
		//Granted Public Domain changes Ends


		String formatString = "MM/dd/yyyy";
		if (null != PermUserContext.getDateFormat(request)) {
			formatString = PermUserContext.getDateFormat(request).toLowerCase();
			formatString = formatString.replace("yyyy", "yy");
		}
		mv.addObject("dateFormat", formatString);

		boolean sourceGroupHasMasterAgreements = false;
		boolean hasRFDealsForSouceGroup = false;
		if (form.getSource().getSourceGroup() != null) {
			Integer sourceGroupId = form.getSource().getSourceGroup().getId();
			sourceGroupHasMasterAgreements = sourceRepository.doesSourceGroupHaveMasterAgreements(sourceGroupId);
			hasRFDealsForSouceGroup = (form.getSource().getSourceGroup().getRoyaltyFreeDeals().size() > 0);
		}
		mv.addObject("sourceGroupHasMasterAgreements", sourceGroupHasMasterAgreements);
		form.setSourceGroupHasMasterAgreements(sourceGroupHasMasterAgreements);//Added to implement DM-122
		form.setHasRFDealsForSouceGroup(hasRFDealsForSouceGroup);//Added to implement DM-122 and DM-123
		mv.addObject("commonwid",commonwid);   // Added current commonwid for 96553
		return mv;
	}

	@RequestMapping(value = "/permissions/contract/rights_summary", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView rightsSummaryLoad(HttpServletRequest request, @ModelAttribute(FORM_NAME) ContractWizardForm form)
			throws Exception {
		log.debug("rightsSummaryLoad(): entered...");

		ModelAndView mv = new ModelAndView("pages.contract.rightssummary");
		mv.addObject("contract", form.getContract());
		List<POAssetView> poAssetViews = new ArrayList<POAssetView> ();
		for (ContractAsset asset : form.getContract().getAssets()) {
			poAssetViews.add(contractRepository.loadPOAssetView(asset.getAssetBaseId()));
		}
		mv.addObject("assets", poAssetViews);
		//Start: Added to implement DM-122
		List<ConditionNode> conditions = form.getConditions();
		if (!CollectionUtils.isEmpty(conditions)) {
			if(form.isAssetLevelRF()) {
				String rollupValue = null;
	        	for(ConditionNode node : conditions.get(7).getChildren()) {
	        		if(!StringUtils.isEmpty(node.getValue())) {
	        			if("true".equalsIgnoreCase(node.getValue())) {
	        				rollupValue = node.getDescription();
	        				if(null != node.getChildren()) {
		        				for(ConditionNode subnode : node.getChildren()) {
		        					rollupValue = "="+subnode.getValue();
		        				}
	        				}
	        			}
	        		}
	          	}
	        	conditions.get(7).setRollupValue(rollupValue);
			}
		}
		//End: Added to implement DM-122
		mv.addObject("conditions", CollectionUtils.isEmpty(conditions) ? "N/A" : conditionRepository.calculateRollupString (conditions));

		return mv;
	}

	@RequestMapping(value = "/permissions/contract/assets", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView assetsLoad(@ModelAttribute(FORM_NAME) ContractWizardForm form,@RequestParam(value = "commonwid") int commonwid,
			HttpServletRequest request)
			throws Exception {
		log.debug("assetsLoad(): entered... form.isRerequest() = " + form.isRerequest());

		ModelAndView mv = new ModelAndView("pages.contract.assets");
		List<POAssetView> poAssetViews = loadPoAssetViewForRF (form, request,commonwid); // Added current commonwid to fetch assets
		int selectCount = 0;  // just for debugging
		HashMap<Integer, List<UsageConditionSize>> usageSizeMap = new HashMap<Integer, List<UsageConditionSize>>();

		//Start: Added to implement DM-1185
		if(null != form.getSource().getSourceGroup()) {
			Date invoiceDate = null;
			if(null != form.getInvoiceDateStr()) {
				SimpleDateFormat dateFormat = PermUserContext.getInternalDateFormat();
				invoiceDate = dateFormat.parse(form.getInvoiceDateStr());
			}

			log.debug("assetsLoad(): invoiceDate after parsing: " + invoiceDate);
			boolean maExistForInvoiceDate = sourceRepository.doesSourceGroupHaveMasterAgreements(form.getSource().getSourceGroup().getId(), invoiceDate);
			log.debug("assetsLoad(): setMaExistForInvoiceDate to: " + maExistForInvoiceDate);
			form.setMaExistForInvoiceDate(maExistForInvoiceDate);
		} else {
			form.setMaExistForInvoiceDate(false);//set to false as a safe operation
		}
		//Start: Added to implement DM-1185

		// load the rfree deals to be displayed.
		List<RoyaltyFreeDeal> royaltyFreeDeals = (null != form.getSource().getSourceGroup()) ?
										(form.getSource().getSourceGroup().getRoyaltyFreeDeals()) : new ArrayList<RoyaltyFreeDeal> ();

				for (POAssetView view : poAssetViews) {
					if (view.getAssetId() == form.getAssetId()) {
						view.setSelected(true);
						if (view.getWillBeRoyaltyFree() && null != royaltyFreeDeals && royaltyFreeDeals.size() > 0 ) {
							if (null == view.getRfDealId() || view.getRfDealId() == 0) {
								view.setRfDealId(royaltyFreeDeals.get(0).getId());
						//		view.setPrice(0.0);
							}
						}
				selectCount++;
			}

			List<UsageConditionSize> usageSizeList;
			if (form.getContract() == null || form.getContract().getId() == null) {
				usageSizeList = new ArrayList<UsageConditionSize>(1);
			}
			else {
				usageSizeList = conditionRepository.loadUsageSizeForContractAsset(form.getContract().getId(), view.getAssetId());
			}

			if (usageSizeList.size() == 0) {
				usageSizeList.add(new UsageConditionSize(null, Size.NA));
			}

			usageSizeMap.put(view.getAssetId(), usageSizeList);
		}
		log.debug("assetsLoad(): selected " + selectCount + " out of " + poAssetViews.size() + " assets");

		mv.addObject("assets", poAssetViews);
		if (form.getUsageSizeMap() == null) {
			// If we are not coming back from previous button or server-side form validation that did not pass
			form.setUsageSizeMap(usageSizeMap);
		}
		List<Contact> contacts = form.getSource().getContacts();

		// select the first contact to display
		log.debug("assetsLoad(): about to test for contacts");
		if (CollectionUtils.isNotEmpty(contacts)) {
			log.debug("assetsLoad(): there are contacts");
			mv.addObject("contact", contacts.get(0));
		}

		form.setPrevUrl("/sapp/permissions/contract/details?commonwid=" + commonwid);

		// load the rfree deals to be displayed.
//		List<RoyaltyFreeDeal> royaltyFreeDeals = (null != form.getSource().getSourceGroup()) ?
//										(form.getSource().getSourceGroup().getRoyaltyFreeDeals()) : new ArrayList<RoyaltyFreeDeal> ();
		mv.addObject("royaltyFreeDeals", royaltyFreeDeals);
		// if only one asset, and we do not have royalty free deals, we skip the assets page (redirect to submit_assets)
		// - NO, James says (5/31/2013) we should now always show the assets page
		//if (poAssetViews.size() == 1 && CollectionUtils.isEmpty(royaltyFreeDeals)) {
		//	mv = new ModelAndView("redirect:/sapp/permissions/contract/submit_assets?data=" + poAssetViews.get(0).getAssetId() + ",0,0");
		//}
		log.debug("form.getWizardOwnerType()"+form.getWizardOwnerType());

		return mv;
	}

	@RequestMapping(value = "/permissions/contract/submit_assets", method = { RequestMethod.POST, RequestMethod.GET })
	public ModelAndView assetsSubmit(@ModelAttribute(FORM_NAME) ContractWizardForm form,
			HttpServletRequest request)
			throws Exception {
		log.debug("assetsSubmit(): entered...");
		//log.debug("assetsSubmit(): params: " + ServletUtil.getAllParameters(request));
		populateUsageSizeMap(request, form);
		// This is now checked by client-side JavaScript also, but good to always check on the server-side
		if (CollectionUtils.isEmpty(form.getContract().getAssets())) {
				return new ModelAndView("redirect:/sapp/permissions/contract/assets?generalMessage=You must select at least one Asset" + "&commonwid="+ form.getCwId());
		}

		//test to see if asset costs = contract cost
		double assetsCost = 0;
		for (ContractAsset casset : form.getContract().getAssets()) {
			assetsCost = assetsCost +casset.getPrice();
			// Fix for asset cost not adding up issue
			assetsCost= Math.round(assetsCost*100.0)/100.0;
		}

		double contractPrice = form.getContract().getPrice();

		// reset flags for unselected assets in existing contract
		if (null != form.getContract().getId()) {
			resetUnselectedAssets(form.getContract());
		}
		ModelAndView mv = null;
		// if no restrictions or a master agreement is selected
		//The check '!form.isAssetLevelRF()' is added for implementing DM-122
		if ((form.isPermissionRequest() && form.isNoRestrictions()) || (form.getMaDealId() != null && !form.isAssetLevelRF())) {
			mv = new ModelAndView("redirect:/sapp/permissions/contract/save");
			// log.debug("assetsSubmit(): total assetsCost: " + assetsCost + " total Contract Cost: " + contractPrice);
			// if (assetsCost != contractPrice) {
			//	return new ModelAndView("redirect:/sapp/permissions/contract/assets?generalMessage=Cost of all assets does not match the contract cost " +
			//			"current Contract cost: " + contractPrice + " total assets cost: " + assetsCost);
			// 	mv = new ModelAndView("redirect:/sapp/permissions/contract/details?generalMessage=Cost of all assets does not match the contract cost " +
			// 			"current Contract cost: " + contractPrice + " total assets cost: " + assetsCost);
			// 	mv.addObject(FORM_NAME, form);
			return mv;
		}
		else {
			// lnagy - if the user selected restrictions, then we set to be a contract, no permission request
			// if you decide to redirect to assets page if cost is different, then put the setPermissionrequest only when redirecting to details
			form.setPermissionRequest(false);
			mv = new ModelAndView("redirect:/sapp/permissions/contract/permissions");
			log.debug("assetsSubmit(): total assetsCost: " + assetsCost + " total Contract Cost: " + contractPrice);
			if (assetsCost != contractPrice) {
		//		return new ModelAndView("redirect:/sapp/permissions/contract/assets?generalMessage=Cost of all assets does not match the contract cost " +
		//				"current Contract cost: " + contractPrice + " total assets cost: " + assetsCost);
		//	}
				mv = new ModelAndView("redirect:/sapp/permissions/contract/assets?generalMessage=Cost of all assets does not match the contract cost " +
						"current Contract cost: " + contractPrice + " total assets cost: " + assetsCost + "&commonwid="+ form.getCwId());

				mv.addObject(FORM_NAME, form);
				return mv;
			}
		}

		return mv;
	}

	// look for previously selected assets and reset Royalty Free, seats, printrun etc.
	// if asset has not other contracts then also make rights managed.
	public void resetUnselectedAssets(Contract newContract) {
		List<Integer> unselectedIds = new ArrayList<Integer>();
		try {
			Contract oldContract = contractRepository.lazyLoad(Contract.class, newContract.getId(), new String[] { "assets" });
			for (Integer assetId: oldContract.getAssetIds()) {
				if (!newContract.getAssetIds().contains(assetId)) {
					unselectedIds.add(assetId);
				}
			}

			for (Integer assetId: unselectedIds) {
				Asset asset = assetRepository.loadAssetById(assetId);
				// if only one asset use for this asset, reset flags
				if (asset.isRoyaltyFree() && getAssetUseRepository().countContractsForAssetId(assetId) == 1) {
					asset.setRoyaltyFree(false);
					asset.setManaged(true);
					getAssetRepository().merge(asset);
				}
			}
		} catch (Exception ex) {
			log.error("Exception processing old contract: ", ex);
		}

		return;
	}

	@RequestMapping(value = "/permissions/contract/permissions", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView permissionsLoad(HttpServletRequest request, @ModelAttribute(FORM_NAME) ContractWizardForm form)
			throws Exception {
		log.debug("permissionsLoad(): entered...");

		ModelAndView mv = new ModelAndView("pages.contract.permissions");
		//Added for implementing Build RN 151026-001937 - Start
		if(StringUtils.isNotBlank(form.getWizardOwnerType())) {
			if(form.getWizardOwnerType().equals("Author Owned")){
				log.debug("Wizard owner type: "+form.getWizardOwnerType());
				mv.addObject("isAuthorOwned", true);
			}
		}
		//Added for implementing Build RN 151026-001937 - End
		mv.addObject("commonwid", form.getCwId());
		return mv;
	}

	@RequestMapping(value="/permissions/contract/doesContractNumberExist",method = {RequestMethod.GET, RequestMethod.POST})
	public void doesContractNumberExist(
			@RequestParam("cwId") int cwId,
			@RequestParam("sourceId") int sourceId,
			@RequestParam("number") String number,
			@RequestParam(value = "contractId", required = false) Integer contractId,
			HttpServletResponse response) throws Exception {
		log.debug("doesContractNumberExist(): cwId = " + cwId + ", sourceId = " + sourceId
			+ ", number = " + number + ", contractId = " + contractId);
		boolean result = contractRepository.contractExists(cwId, sourceId, number, contractId);

		response.setContentType("text/plain");
		PrintWriter writer = response.getWriter(); // throws IOException
		log.debug("doesContractNumberExist(): writing [" + result + "]");
		writer.write(String.valueOf(result));
	}

	@RequestMapping(value = "/permissions/contract/add_compcopy", method = RequestMethod.POST)
	public ModelAndView addCompCopy(@ModelAttribute(FORM_NAME) ContractWizardForm form)
			throws Exception {
		log.debug("addCompCopy(): entered..." + form.getNumberOfCopies() + " " + form.getMailingAddressId());

		if (0 != form.getNumberOfCopies() && 0 != form.getMailingAddressId()) {
			CompCopy compCopy = new CompCopy();
			compCopy.setNumberOfCopies(form.getNumberOfCopies());

			Address address = getSourceRepository().loadAddressById(form.getMailingAddressId());
			if (null != address) {
				compCopy.setAddressId(form.getMailingAddressId());
				compCopy.setAddress(address);
				compCopy.setContract(form.getContract());
				// compCopy = contractRepository.saveRequiresNew(compCopy);

				form.getContract().getCompCopies().add(compCopy);
			}
		}
		ModelAndView mv = new ModelAndView("redirect:/sapp/permissions/contract/details?commonwid="+form.getCwId());
		return mv;
	}

	@RequestMapping(value = "/permissions/contract/new_address", method = RequestMethod.POST)
	public ModelAndView addNewAddress(@ModelAttribute(FORM_NAME) ContractWizardForm form)
			throws Exception {
		log.debug("addNewAddress(): entered..." + form.getNumberOfCopies() + " " + form.getMailingAddressId());

		if (null != form.getCompRecipient() && form.getCompRecipient().trim().length() > 0) {
			Source dI = form.getSource();
			Contact cContact = new Contact();

			cContact.setFirstName(form.getCompRecipient());
			cContact.setFirstName(form.getCompRecipient());
			log.debug("addNewAddress(): about to persist contact:" + cContact.getFirstName());

			// log.debug("about to persist address:" + form.getAddress().getCity());
			Address dAddress = form.getAddress();
			// insert recipient in address line one
			dAddress.setLineThree(dAddress.getLineTwo());
			dAddress.setLineTwo(dAddress.getLineOne());
			dAddress.setLineOne(form.getCompRecipient());
			dAddress.setCountry(form.getAddress().getCountry());
			dAddress.setType(AddressType.OTHER);  // this used to be MAILING but was changed to OTHER
			dAddress = getSourceRepository().saveRequiresNew(dAddress);

			cContact.setAddress(dAddress);
			cContact.setSource(dI);
			cContact = getSourceRepository().saveRequiresNew(cContact);


			SourceAddress iAddress = new SourceAddress();
			iAddress.setAddress(dAddress);
			iAddress.setSource(dI);
			iAddress = getSourceRepository().saveRequiresNew(iAddress);

			form.setMailingAddressId(dI.getId().intValue());
		}

		ModelAndView mv = new ModelAndView("redirect:/sapp/permissions/contract/generalDetails");
		// mv.addObject(FORM_MODEL_NAME, form);
		// mv.addObject("mailingAddressList",
		return mv;
	}

	@RequestMapping(value = "/permissions/contract/conditions", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView conditions(HttpServletRequest request, @ModelAttribute(FORM_NAME) ContractWizardForm form)
			throws Exception {

		log.debug("conditions(): entered...");
		ModelAndView mv = new ModelAndView();
		mv = new ModelAndView("pages.contract.conditions");
		mv.addObject(FORM_NAME, form);

		List<Asset> dAssets = new ArrayList<Asset>();
		log.debug("conditions(): asset ids: " +  StringUtil.collectionToString(form.getContract().getAssetIds(), ","));

		for (Integer id : form.getContract().getAssetIds()) {
			dAssets.add(assetRepository.loadAssetById(id));
		}

		mv.addObject("assets", dAssets);
		mv.addObject("firstAsset", dAssets.get(0));

		return mv;
	}

	@RequestMapping(value = "/permissions/contract/load_conditions_frame", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView loadConditionsFrame(HttpServletRequest request,
			@ModelAttribute(FORM_NAME) ContractWizardForm form,
			@RequestParam(value = "caids", required = false) String aids)
			throws Exception {

		ModelAndView mv = new ModelAndView();

		log.debug("loadConditionsFrame(): entered...");
		mv = new ModelAndView("pages.conditions.include");

		mv.addObject(FORM_NAME, form);
		mv.addObject("loadUrl", request.getContextPath() + "/sapp/permissions/contract/load_conditions?" +
				(!StringUtils.isBlank(aids) ? "caids=" + aids : ""));
		mv.addObject("postUrl", request.getContextPath() + "/sapp/permissions/contract/submit_conditions");
		mv.addObject("postNodeUrl", request.getContextPath() + "/sapp/permissions/contract/postNode");
		return mv;
	}

	@RequestMapping(value="/permissions/contract/load_conditions", method = {RequestMethod.GET, RequestMethod.POST})
	public void loadConditions(HttpServletRequest request,
			@ModelAttribute(FORM_NAME) ContractWizardForm form,
			HttpServletResponse response,
			@RequestParam(value = "caids", required = false) String aids) throws Exception {
		log.debug("loadConditions(): entered...");
		log.debug("form.isAssetLevelRF() value entered..."+form.isAssetLevelRF());

		List<ConditionNode> conditions = form.getConditions();
		if (CollectionUtils.isEmpty(conditions)) {
			conditions = loadConditions(form);
		}
		//Start: Added to implement DM-122
		int conditionsSize = conditions.size();
        if(!form.isAssetLevelRF()) {
  			conditions.get(7).setRollupValue(null);
        	conditions.get(7).setCanSee(false);//Disable the seat conditions. i.e. 8th condition tree
        } else {
        	conditions.get(7).setCanSee(true);//Enable the seat conditions. i.e. 8th condition tree
        }
        //End: Added to implement DM-122
		//Added for Paperwork Task C Starts
		if((form.getWizardOwnerType() != null && (form.getWizardOwnerType().equals("Wiley_Created")
				|| form.getWizardOwnerType().equals("Public Domain"))) || form.isManagerApproved()){//Added condition for DM-374
	        log.debug("CONDITIONS "+conditions.get(0).getCode());

	        if(conditions.get(1).getChildren().get(2).getDescription().equals("Other")){
	        	conditions.get(1).getChildren().get(2).setCanSee(false);
	        }
	        if(conditions.get(2).getChildren().get(2).getDescription().equals("Other (select as many as apply)")){
	        	conditions.get(2).getChildren().get(2).setCanSee(false);
	        }
	        if(conditions.get(3).getChildren().get(1).getDescription().equals("Maximum copies Wiley can print")){
	        	conditions.get(3).getChildren().get(1).setCanSee(false);
	        }
	      //Start: Added to implement DM-122
	        if(form.isAssetLevelRF()) {
	        	if(conditions.get(7).getChildren().get(1).getDescription().equals("Maximum Seats allowed")){
		        	conditions.get(7).getChildren().get(1).setCanSee(false);
		        }
	        }
	      //End: Added to implement DM-122

        	log.debug("enetered"+conditions.get(0).getRollupValue());
        	for(int i = 0 ;i<conditionsSize-1;i++){ // i<7
	        	for(int j = 0;j<conditions.get(i).getChildren().size();j++){
	        		if(j == 0){
	        			conditions.get(i).getChildren().get(j).setValueToTrue();
	        			log.debug("CONDITIONS "+conditions.get(0).getCode());
	        		}else{
	        			conditions.get(i).getChildren().get(j).setCanEdit(false);
	        		}
	        	}
        	}

        	if(conditions.get(0).getCode().equals("medium")){
        		log.debug("hahahaa");
        		conditions.get(0).setRollupValue("All media types including future types");
        	}

        	if(conditions.get(1).getCode().equals("sales")){
        		conditions.get(1).setRollupValue("Worldwide");
        	}

        	if(conditions.get(2).getCode().equals("language")){
        		conditions.get(2).setRollupValue("All Languages");
        	}

        	if(conditions.get(3).getCode().equals("print_run")){
        		conditions.get(3).setRollupValue("Unlimited print run is granted");
        	}

            if(conditions.get(4).getCode().equals("edition")){
            	conditions.get(4).setRollupValue("Granted for this, future editions and/or entire author series");
        	}

            if(conditions.get(5).getCode().equals("dwork")){
            	conditions.get(5).setRollupValue("Wiley can include the asset(s) in any ancillaries, derivatives and custom works");
        	}

            if(conditions.get(6).getCode().equals("sublicense")){
            	conditions.get(6).setRollupValue("Wiley can include the asset(s) when sub-licensing product");
        	}
            //Start: Added to implement DM-122
            if(form.isAssetLevelRF()) {
            	if(conditions.get(7).getCode().equals("seats")){
    	        	conditions.get(7).setRollupValue("Unlimited Seats are granted");
    	        }
            } else {
            	//conditions.get(7).setDescription(null);
            	conditions.get(7).setRollupValue(null);
            }
            //End: Added to implement DM-122

		}//Added for implementing Build RN 151026-001937 - Start
		else if (form.getWizardOwnerType() != null && form.getWizardOwnerType().equals("Author Owned")) {
			/*
			 * For the combination of Layer 1 owner type value author created/owned and Layer 2 owner type value Will work for hire? or
			 * no Layer 2 owner type value, then on Get permissions screen
			 * (i)	System must pre-populate the Cosmo Quiz with the top most options
			 * (ii)	System must allow user to amend the grant conditions on the pre-populated Cosmo Quiz
			 */
			log.debug("Inside Author Owned conditions setup: "+conditions.get(0).getRollupValue());
			//First set all values to false. This is required since browser is caching values when we go to PrevUrl and come back to this screen
			for(int i=0; i<conditionsSize-1; i++) {
	        	for(int j=0; j<conditions.get(i).getChildren().size(); j++) {
	        		conditions.get(i).getChildren().get(j).setValue("false");
	        	}
        	}
			//Now set value to true for the top children in the tree.
        	for(int i=0; i<conditionsSize-1; i++) {
	        	for(int j=0; j<conditions.get(i).getChildren().size(); j++) {
	        		if(j == 0) {
	        			conditions.get(i).getChildren().get(j).setValueToTrue();
	        		}
	        	}
        	}
        	if(conditions.get(0).getCode().equals("medium")){
        		conditions.get(0).setRollupValue("All media types including future types");
        	}

        	if(conditions.get(1).getCode().equals("sales")){
        		conditions.get(1).setRollupValue("Worldwide");
        	}

        	if(conditions.get(2).getCode().equals("language")){
        		conditions.get(2).setRollupValue("All Languages");
        	}

        	if(conditions.get(3).getCode().equals("print_run")){
        		conditions.get(3).setRollupValue("Unlimited print run is granted");
        	}

            if(conditions.get(4).getCode().equals("edition")){
            	conditions.get(4).setRollupValue("Granted for this, future editions and/or entire author series");
        	}

            if(conditions.get(5).getCode().equals("dwork")){
            	conditions.get(5).setRollupValue("Wiley can include the asset(s) in any ancillaries, derivatives and custom works");
        	}

            if(conditions.get(6).getCode().equals("sublicense")){
            	conditions.get(6).setRollupValue("Wiley can include the asset(s) when sub-licensing product");
        	}
            //Start: Added to implement DM-122
            if(form.isAssetLevelRF()) {
            	if(conditions.get(7).getCode().equals("seats")){
    	        	conditions.get(7).setRollupValue("Unlimited Seats are granted");
    	        }
            } else {
            	//conditions.get(7).setDescription(null);
            	conditions.get(7).setRollupValue(null);
            }
            //End: Added to implement DM-122
		}

		//Added for implementing Build RN 151026-001937 - End
        //log.debug("CONDITIONS "+node.getChildren().get(0).getValue());
        //form.getWizardOwnerType() == 'Wiley_Created' &&
		//Added for Paerwork Task C ends

		// cache data
		form.setConditions(conditions);
		// save the loaded conditions in session
		request.getSession().setAttribute(CONDITIONS_NAME, conditions);

		String json = ObjectToJson.doTransform(conditions, null);
		//log.debug("loadConditions(): json: " + json);

		response.setContentType("application/json");
		PrintWriter writer = response.getWriter(); // throws IOException
		writer.println(json);
	}

	/**
	 * loadConditions - will load the conditions from DB (or init in memory if new contract)
	 *
	 * @param form
	 * @return List<ConditionNode>
	 */
	private List<ConditionNode> loadConditions(ContractWizardForm form) {
		Integer contractId = form.getContract().getId();
		List<ConditionNode> conditions = conditionRepository.loadContractConditions(contractId, form.getContract().isPermissionForm());

		return conditions;
	}

	/*@GetMapping("/postNode")*/
	@RequestMapping(value="/permissions/contract/postNode", method = {RequestMethod.GET, RequestMethod.POST})
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

	@RequestMapping(value = "/permissions/contract/submit_conditions", method = RequestMethod.POST)
	public void conditionsSubmit(HttpServletRequest request, HttpServletResponse response,
			@ModelAttribute(FORM_NAME) ContractWizardForm form)
			throws Exception {
		log.debug("conditionsSubmit(): entered...");

		@SuppressWarnings("unchecked")
		List<ConditionNode> conditions = (List<ConditionNode>) request.getSession().getAttribute(CONDITIONS_NAME);
		form.setConditions(conditions);

		response.setContentType("text/plain");
		PrintWriter writer = response.getWriter(); // throws IOException
		writer.write("");
		// log.debug("conditions " + conditions);
	}

	// you need this to be both get and post please do not change
	@RequestMapping(value = "/permissions/contract/save", method = { RequestMethod.POST, RequestMethod.GET })
	public void save(HttpServletRequest request, HttpServletResponse response,
			@ModelAttribute(FORM_NAME) ContractWizardForm form)
			throws Exception {
		log.debug("save(): entered...");
		PerfTimer timer = assetRepository.getMonitor().startTimer("ContractWizardController::save");

		form.populateBean();
		Contract contract = form.getContract();

		ArgUtil.notEmpty(contract.getAssets(), "contract.getAssets()");

		// pass conditions to be saved
		List<ConditionNode> conditions = form.getConditions();
		// at this point if the contract is a permission form and a new contract,
		// we expect the conditions will be empty - so initialize them in this case
		if (contract.isPermissionForm() && CollectionUtils.isEmpty(conditions)) {
			conditions = conditionRepository.loadContractConditions(contract.getId(), contract.isPermissionForm());
		}

		List<Integer> removedAssetIds = new ArrayList<Integer>();
		if (form.getMaDealId() == null || form.isAssetLevelRF()) {//check 'form.isAssetLevelRF()' added for DM-122
			if (null == contract.getDate()) {
				contract.setDate(new Date());
			}
			contract = contractService.save(contract, conditions, form.getUsageSizeMapSelectedOnly(), removedAssetIds);
		}
		else {
			int maDealId = form.getMaDealId();
			MasterAgreementDeal maDeal = new MasterAgreementDeal();
			maDeal.setId(maDealId);
			// (smarkoff: tested this does not blank out MADeal name when save)
			contract.setMasterAgreementDeal(maDeal);

			List<ConditionNode> nodeList = conditionRepository.loadMaDealConditions(maDealId);
			contract = contractService.save(contract, nodeList, form.getUsageSizeMapSelectedOnly(), removedAssetIds);
		}

		// create the JSON object to be sent to the dialog
		Map<String, Object> map = new HashMap<String, Object>();

		// load all asset use ids for the assets attached to contract so we can refresh them in data
		// table
		List<Integer> aids = contract.getAssetIds();
		List<Integer> auids = new ArrayList<Integer>();
		for (Integer aid : aids) {
			List<Integer> ids = assetUseRepository.loadAssetUseIdsForAssetId(aid);
			log.debug("save(): processing asset id: " + aid);
			auids.addAll(ids);
		}

		// repeat only part of logic above for removedAssetIds
		for (Integer aid : removedAssetIds) {
			List<Integer> ids = assetUseRepository.loadAssetUseIdsForAssetId(aid);
			auids.addAll(ids);
		}

		//Updating the C2A credit line to Asset Table as per James Russiello Comment - Start
		contract = contractRepository.lazyLoad(Contract.class, contract.getId(), new String[] {"assets", "compCopies", "source"});
		List<ContractAsset> newAssetList = contract.getAssets();

		log.debug("save():newAssetList: "+newAssetList);

		for (ContractAsset ca : newAssetList) {
				Asset asset = assetRepository.find(Asset.class, ca.getAssetBaseId());
				if (null != ca.getCreditLine()) {
						asset.setCreditLine(ca.getCreditLine());
						assetRepository.saveRequiresNew(asset);
					}
		}
		//Updating the C2A credit line to Asset Table as per James Russiello Comment - End

		// if no response is needed (called as a plain method) then return
		if (null == response) return;

		response.setContentType("application/json");

		map.put("auids", auids);
		map.put("url", request.getContextPath() + "/sapp/permissions/contract/medium?cwId="
				+ form.getCwId() + "&contractId=" + contract.getId());

		String output = ObjectToJson.doTransform(map, null); // throws InitialisationException,
																	// TransformationException
		log.debug("save(): sending:\r\n" + output);

		// Don't set contentLength because String.length() is not the
		// same as #bytes if there are double-byte chars
		// response.setContentLength(output.length());
		PrintWriter writer = response.getWriter(); // throws IOException
		writer.write(output);

		timer.stopTimer();
	}

	/**
	 * smarkoff: This method should be deleted (we no longer have insufficent mediums)
	 * but first figure out what to do in method above other than set url to ../medium
	 * (ie need to not call this method).
	 */
	/*@GetMapping("/medium")*/
	@RequestMapping(value = "/permissions/contract/medium", method = {RequestMethod.GET, RequestMethod.POST})
	public Object loadInsufficientMediums(@RequestParam("cwId") int cwId,
			@RequestParam(value = "contractId", required = false) Integer contractId,
			HttpServletResponse response)
			throws Exception {
		log.debug("loadWithInsufficientMediums() entered...cwId, contractId = " + cwId + ", " + contractId);

		// empty response means no excluded mediums means close the dialog
		PrintWriter writer = response.getWriter(); // throws IOException
		writer.write("");
		return null;
	}

	private List<POAssetView> loadPoAssetView (ContractWizardForm form, HttpServletRequest request) throws Exception {
		List<POAssetView> poAssetViews = null;
		Integer contractId = form.getContract().getId();
		log.debug("loadPoAssetView(): contractId = " + contractId + ", poId = " + form.getPoId());
		// the order of if/else is important
		if (contractId != null) {
			poAssetViews = contractRepository.loadPOAssetsView(contractId);

			// besides assets already in contract, show ones that could be added to contract
			PermissionStatus [] statuses = ArrayUtils.addAll(PermissionStatus.UNREQUESTED_GROUP, PermissionStatus.MIGRATED_GROUP_FORM_SENT);


			boolean includeCovers = request.isUserInRole(Role.EMPLOYEE_DEFAULT.getCode())
					|| request.isUserInRole(Privilege.COVER_ASSETS.getCode());
			boolean allChapters = request.isUserInRole(Role.EMPLOYEE_DEFAULT.getCode())
					|| request.isUserInRole(Privilege.EDIT_CHAPTERS.getCode());
			Integer userId = PermUserContext.getCurrentUserId(request);

			List<POAssetView> list2 = purchaseOrderRepository.loadPOAssetsViewForSource(
					form.getSource().getId(), form.getCwId(), statuses, includeCovers, allChapters, userId);
			// in combining these two lists, we should not get any repeats because anything already associated
			// to the contract will have a GRANTED or Contract Insufficient status.
			poAssetViews.addAll(list2);

		} else {
			// lnagy (https://www.pivotaltracker.com/story/show/55193556) -
			// On enter details, asset selection screen, increase asset pick list to include all available assets from that source
			// if (form.getPoId() == 0) {
			// if no PO available, we load the assets that have status UNREQUESTED or FORM_SENT,
			// but for FORM_SENT means it has a PO
			PermissionStatus [] statuses = null;
			if (form.isRerequest()) {
				statuses = PermissionStatus.REREQUEST_GROUP;
			} else {
				statuses = ArrayUtils.addAll(PermissionStatus.UNREQUESTED_GROUP, PermissionStatus.MIGRATED_GROUP_FORM_SENT);
			}
			boolean includeCovers = request.isUserInRole(Role.EMPLOYEE_DEFAULT.getCode())
					|| request.isUserInRole(Privilege.COVER_ASSETS.getCode());
			boolean allChapters = request.isUserInRole(Role.EMPLOYEE_DEFAULT.getCode())
					|| request.isUserInRole(Privilege.EDIT_CHAPTERS.getCode());
			Integer userId = PermUserContext.getCurrentUserId(request);
			poAssetViews = purchaseOrderRepository.loadPOAssetsViewForSource(
					form.getSource().getId(), form.getCwId(), statuses, includeCovers, allChapters, userId);
		}
		// else {
		//	poAssetViews = purchaseOrderRepository.loadPOAssetsView(form.getPoId());
		//}

		// start with all selected
		for (POAssetView poAssetView : poAssetViews) {
			ContractAsset ca = form.getContract().getContractAssetById(poAssetView.getAssetId());
			if (ca != null) {
				poAssetView.setSelected(true);
				poAssetView.setPrice(ca.getPrice());
				poAssetView.setRfDealId(ca.getRoyaltyFreeDealId());
				poAssetView.setHasAssetLevelRFDeal(ca.isAssetLevelRFDeal());//Added for DM-122
				form.setLoadedAssetLvlRF(ca.isAssetLevelRFDeal());//Added for DM-122
				poAssetView.setConditionCreditLine(ca.getCreditLine());
				poAssetView.setNoCrop(ca.isNoCrop());
				poAssetView.setNoBleed(ca.isNoBleed());
			}
		}
		return poAssetViews;
	}
// To Eliminate Extra assets from original contract and making cost zero out.
	private List<POAssetView> loadPoAssetViewForRF (ContractWizardForm form, HttpServletRequest request,int commonwid) throws Exception {
		List<POAssetView> poAssetViews = null;
		// Changes for 96553
		List<POAssetView> poAssetViews1 = new ArrayList<POAssetView>();
		POAssetView pav= new POAssetView();

		Integer contractId = form.getContract().getId();
		log.debug("loadPoAssetViewForRf(): contractId = " + contractId + ", poId = " + form.getPoId());
		// the order of if/else is important
		if (contractId != null) {
			poAssetViews = contractRepository.loadPOAssetsView(contractId);


			// besides assets already in contract, show ones that could be added to contract
			PermissionStatus [] statuses = ArrayUtils.addAll(PermissionStatus.UNREQUESTED_GROUP, PermissionStatus.MIGRATED_GROUP_FORM_SENT);


			boolean includeCovers = request.isUserInRole(Role.EMPLOYEE_DEFAULT.getCode())
					|| request.isUserInRole(Privilege.COVER_ASSETS.getCode());
			boolean allChapters = request.isUserInRole(Role.EMPLOYEE_DEFAULT.getCode())
					|| request.isUserInRole(Privilege.EDIT_CHAPTERS.getCode());
			Integer userId = PermUserContext.getCurrentUserId(request);
			// commented out for INC_96553 as no need to display extra assets
			List<POAssetView> list2 = purchaseOrderRepository.loadPOAssetsViewForSource(
					form.getSource().getId(), form.getCwId(), statuses, includeCovers, allChapters, userId);
			// in combining these two lists, we should not get any repeats because anything already associated
			// to the contract will have a GRANTED or Contract Insufficient status.
			poAssetViews.addAll(list2);


			//Eliminate extra assets for 96553

			List<AssetUse> asstuse = getAssetUseRepository().loadAssetUseListByCWId(commonwid);
			for(AssetUse au : asstuse){
				for (POAssetView view : poAssetViews) {
					if (view.getAssetId() == au.getAsset().getId()) {
						int index= poAssetViews.indexOf(view);
						log.debug("index of matched assets --> "+index);
						if(index >=0){
							 pav = poAssetViews.get(index);
							 //First time it adds, if same obj exists, then it doesn't add to the list.
							 if(!poAssetViews1.contains(pav)){
							 poAssetViews1.add(pav);
							 }
						}

					}

				}

			}

		} else {
			// lnagy (https://www.pivotaltracker.com/story/show/55193556) -
			// On enter details, asset selection screen, increase asset pick list to include all available assets from that source
			// if (form.getPoId() == 0) {
			// if no PO available, we load the assets that have status UNREQUESTED or FORM_SENT,
			// but for FORM_SENT means it has a PO
			PermissionStatus [] statuses = null;
			if (form.isRerequest()) {
				statuses = PermissionStatus.REREQUEST_GROUP;
			} else {
				statuses = ArrayUtils.addAll(PermissionStatus.UNREQUESTED_GROUP, PermissionStatus.MIGRATED_GROUP_FORM_SENT);
			}
			boolean includeCovers = request.isUserInRole(Role.EMPLOYEE_DEFAULT.getCode())
					|| request.isUserInRole(Privilege.COVER_ASSETS.getCode());
			boolean allChapters = request.isUserInRole(Role.EMPLOYEE_DEFAULT.getCode())
					|| request.isUserInRole(Privilege.EDIT_CHAPTERS.getCode());
			Integer userId = PermUserContext.getCurrentUserId(request);
			poAssetViews = purchaseOrderRepository.loadPOAssetsViewForSource(
					form.getSource().getId(), form.getCwId(), statuses, includeCovers, allChapters, userId);
		}
		// else {
		//	poAssetViews = purchaseOrderRepository.loadPOAssetsView(form.getPoId());
		//}
		// Eliminating extra assets 96553
		if (poAssetViews1.size()!=0){
			log.debug("Size of another PO Asset Views .......>>>>>>"+poAssetViews1.size());
			poAssetViews=poAssetViews1;
		}
		// start with all selected
		for (POAssetView poAssetView : poAssetViews) {
			Asset asset = assetRepository.loadAssetById(poAssetView.getAssetId());
			int count = assetRepository.getAssetFilesCount(poAssetView.getAssetId()); //Added for DM-117
			if(count > 0) {// Added for implementing DM-117
				poAssetView.setHasAssetFiles(true);
			}
			// if only one asset use for this asset, reset flags

			ContractAsset ca = form.getContract().getContractAssetById(poAssetView.getAssetId());
			if (ca != null) {
				poAssetView.setSelected(true);
				// changes for INC_96553
				if (asset.isManaged())
				{
					poAssetView.setPrice(ca.getPrice());
				}
				else
				{
					poAssetView.setPrice(0.0);
				}
				poAssetView.setRfDealId(ca.getRoyaltyFreeDealId());
				poAssetView.setHasAssetLevelRFDeal(ca.isAssetLevelRFDeal());//Added for DM-122
				form.setLoadedAssetLvlRF(ca.isAssetLevelRFDeal());//Added for DM-122
				poAssetView.setConditionCreditLine(ca.getCreditLine());
				poAssetView.setPrice(ca.getPrice());
				poAssetView.setNoCrop(ca.isNoCrop());
				poAssetView.setNoBleed(ca.isNoBleed());
			}
		}
	return poAssetViews;
	}

	private void populateUsageSizeMap(HttpServletRequest request, ContractWizardForm form) {
		HashMap<Integer, List<UsageConditionSize>> map = new HashMap<Integer, List<UsageConditionSize>>();
		// size-286822
		@SuppressWarnings("unchecked")
		Enumeration<String> en = request.getParameterNames();
		while (en.hasMoreElements()) {
			String paramName = en.nextElement();
			if (paramName.startsWith("usage-")) {
				String assetId = paramName.substring("usage-".length());
				String [] usageValues = request.getParameterValues(paramName);
				String [] sizeValues = request.getParameterValues("size-" + assetId);

				if (usageValues.length != sizeValues.length) {
					// this should not happen
					throw new RuntimeException("number of usage params did not match number of size params");
				}

				List<UsageConditionSize> list = new ArrayList<UsageConditionSize>(usageValues.length);
				for (int i = 0; i < usageValues.length; i++) {
					// for Usage the code of "N/A" will be converted to null
					EnumUsageCondition usage = EnumUsageCondition.getByCode(usageValues[i]);
					Size size = Size.getByCode(sizeValues[i]);
					list.add(new UsageConditionSize(usage, size));
				}
				map.put(new Integer(assetId), list);
				log.debug("populateUsageSizeMap(): put list of size " + list.size() + " for assetId " + assetId);
			}
		}

		form.setUsageSizeMap(map);
	}



	// --------------------- getters and setters --------------------------

	public AssetUseRepository getAssetUseRepository() {
		return assetUseRepository;
	}

	public void setAssetUseRepository(AssetUseRepository assetUseRepository) {
		this.assetUseRepository = assetUseRepository;
	}

	public PurchaseOrderRepository getPurchaseOrderRepository() {
		return purchaseOrderRepository;
	}

	public void setPurchaseOrderRepository(PurchaseOrderRepository purchaseOrderRepository) {
		this.purchaseOrderRepository = purchaseOrderRepository;
	}

	public ContractService getContractService() {
		return contractService;
	}

	public void setContractService(ContractService contractService) {
		this.contractService = contractService;
	}

	public AssetRepository getAssetRepository() {
		return assetRepository;
	}

	public void setAssetRepository(AssetRepository assetRepository) {
		this.assetRepository = assetRepository;
	}

	public ContractRepository getContractRepository() {
		return contractRepository;
	}

	public void setContractRepository(ContractRepository contractRepository) {
		this.contractRepository = contractRepository;
	}

	public AssetUseService getAssetUseService() {
		return assetUseService;
	}

	public void setAssetUseService(AssetUseService assetUseService) {
		this.assetUseService = assetUseService;
	}

	public CommonWorkService getCommonWorkService() {
		return commonWorkService;
	}

	public void setCommonWorkService(CommonWorkService commonWorkService) {
		this.commonWorkService = commonWorkService;
	}

	public ConditionRepository getConditionRepository() {
		return conditionRepository;
	}

	public void setConditionRepository(ConditionRepository conditionRepository) {
		this.conditionRepository = conditionRepository;
	}

	public SourceRepository getSourceRepository() {
		return sourceRepository;
	}

	public void setSourceRepository(SourceRepository sourceRepository) {
		this.sourceRepository = sourceRepository;
	}

	public ProductRepository getProductRepository() {
		return productRepository;
	}

	public void setProductRepository(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}

	public ImportSource getImportSource() {
		return importSource;
	}

	public void setImportSource(ImportSource importSource) {
		this.importSource = importSource;
	}

}
