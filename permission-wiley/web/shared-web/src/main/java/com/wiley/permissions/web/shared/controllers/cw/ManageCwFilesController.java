package com.wiley.permissions.web.shared.controllers.cw;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import com.wiley.permissions.domain.persistence.permissions.CommonWork;
import com.wiley.permissions.domain.persistence.permissions.CwFile;
import com.wiley.permissions.persistence.PersistenceException;
import com.wiley.permissions.repositories.CommonWorkRepository;
import com.wiley.permissions.services.CommonWorkService;
import com.wiley.permissions.services.util.ServiceException;
import com.wiley.permissions.web.shared.PermUserContext;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;
import com.wiley.permissions.web.shared.util.GenericFileView;

/**
 * Main operations for ProductFile entities
 * @author lnagy
 */
@Controller
/*@RequestMapping("/cw/files")*/
@RequestMapping
public class ManageCwFilesController extends BaseAnnotatedController {

	private static final Log log = LogFactory.getLog(ManageCwFilesController.class);

	// private static final String PRODUCT_ID = "productId";
	private static final String FILE_ID = "fileId";
	private static final String CW_ID = "commonWorkId";

	private String finishView = null;
	private String reloadView = null;

	private CommonWorkService commonWorkService = null;
	private CommonWorkRepository cwRepository;

	@RequestMapping(value="/cw/files/view", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView view(HttpServletRequest request) throws NumberFormatException, PersistenceException,
			ServiceException
	{
		log.debug("view(): entered...");
		CommonWork commonWork = PermUserContext.getCurrentCommonWork(request);
		ModelAndView modelAndView = new ModelAndView(getFormView());
		modelAndView.addObject("files", cwRepository.loadFiles(commonWork.getId()));
		modelAndView.addObject("commonWorkId", commonWork.getId());
		modelAndView.addObject("ISBN", commonWork.getPrimaryProduct().getIsbn());// Added for implementing Build RN 160121-001880

		return modelAndView;
	}

	/*@PostMapping("/delete")*/
	@RequestMapping(value="/cw/files/delete", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView delete(HttpServletRequest request,
			@RequestParam(value = FILE_ID, required = true) Integer fileId)
	throws ServiceException, PersistenceException
	{
		log.debug("delete(): entered...");

		getRepository().remove(CwFile.class, fileId);

		ModelAndView modelAndView = new ModelAndView(getReloadView());

		return modelAndView;
	}

	/*@GetMapping("/download")*/
	@RequestMapping(value="/cw/files/download", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView download(HttpServletRequest request,
			@RequestParam(value = FILE_ID, required = true) Integer fileId)
	throws NumberFormatException, PersistenceException, ServiceException
	{
		log.debug("download(): entered...");

		CwFile file = getCommonWorkRepository().find(CwFile.class, fileId);
		ModelAndView mv = new ModelAndView();
		GenericFileView view = new GenericFileView();

		view.setFileName(file.getFileName());
		view.setContentType(file.getMimeType());

		log.debug("download(): File Data size: " + file.getData().length);

		view.setData(file.getData());

		mv.setView(view);

		return mv;
	}

	/*@GetMapping("/upload")*/
	@RequestMapping(value="/cw/files/upload", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView upload(HttpServletRequest request,
			@RequestParam(value = CW_ID, required = true) Integer cwId, final @RequestParam MultipartFile[] ajaxFiles)
	throws FileUploadException, NumberFormatException, ServiceException, IOException,
			PersistenceException
	{
		log.debug("upload(): entered...");
		// Start: Added for implementing Build RN 160121-001880
		String DATE_FORMAT = "yyyyMMdd";
		SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
		Calendar cal = Calendar.getInstance(); // today
		String dateString = sdf.format(cal.getTime());
		String fileName = null;
		String [] splitStrings = null;
		StringBuilder sb = null;
		int length = 0;
		// End: Added for implementing Build RN 160121-001880

		//if (request instanceof MultipartHttpServletRequest) {
			// the request is a MultipartHttpServletRequest => one upload occurred
			// execution order => DispatcherServlet => CommonsMultipartResolver => ManageCwFilesController
			// CommonsMultipartResolver wrapped HttpServletRequest into MultipartHttpServletRequest
		//	MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;

		//	Map<String, MultipartFile> filesMap = multipartRequest.getFileMap();
		//	if (null != filesMap) {
		//		Collection<MultipartFile> files = filesMap.values();
		if ((ajaxFiles != null) && (ajaxFiles.length > 0) && (!ajaxFiles.equals(""))) {	
				for (MultipartFile file : ajaxFiles) {
					log.debug("upload(): fileName = " + file.getName());
					log.debug("upload(): originalFileName = " + file.getOriginalFilename());
					log.debug("upload(): file.getSize() = " + file.getSize());
					// Start: Added for implementing Build RN 160121-001880
					splitStrings = file.getOriginalFilename().split("\\.");
					sb = new StringBuilder();
					length = splitStrings.length;
					if (length == 2) {
						sb.append(splitStrings[0]).append("_").append(dateString).append(".").append(splitStrings[1]);
					} else {
						sb.append(splitStrings[0]).append("_").append(dateString);
					}
					
					fileName = sb.toString();
					// End: Added for implementing Build RN 160121-001880
					log.debug("upload(): modified format of the filename = " + fileName);
					CwFile pFile = new CwFile();
					pFile.setData(file.getBytes());
					//pFile.setDescription(file.getOriginalFilename());
					//pFile.setFileName(file.getOriginalFilename());
					pFile.setDescription(fileName);// Added for implementing Build RN 160121-001880
					pFile.setFileName(fileName);// Added for implementing Build RN 160121-001880
					pFile.setMimeType(file.getContentType());
					try {
						getCommonWorkService().saveCwFile(pFile, cwId);
					} catch (Exception e) {
						throw new PersistenceException(e.getMessage());
					}
				}
			//}
		}

		// well, let's do nothing with the bean for now and return
		return new ModelAndView(getReloadView());
	}

	public CommonWorkService getCommonWorkService() {
		return commonWorkService;
	}

	public void setCommonWorkService(CommonWorkService commonWorkService) {
		this.commonWorkService = commonWorkService;
	}

	public String getFinishView() {
		return finishView;
	}

	public void setFinishView(String finishView) {
		this.finishView = finishView;
	}

	public String getReloadView() {
		return reloadView;
	}

	public void setReloadView(String reloadView) {
		this.reloadView = reloadView;
	}

	public CommonWorkRepository getCommonWorkRepository() {
		return cwRepository;
	}

	public void setCommonWorkRepository(CommonWorkRepository cwRepository) {
		this.cwRepository = cwRepository;
	}
}
