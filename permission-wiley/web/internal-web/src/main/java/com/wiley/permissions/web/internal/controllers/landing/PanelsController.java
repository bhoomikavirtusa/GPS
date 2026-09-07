package com.wiley.permissions.web.internal.controllers.landing;

import java.io.PrintWriter;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.wiley.permissions.domain.persistence.permissions.CommonWork;
import com.wiley.permissions.domain.persistence.permissions.CommonWorkStatus;
import com.wiley.permissions.domain.persistence.permissions.CwFile;
import com.wiley.permissions.repositories.CommonWorkRepository;
import com.wiley.permissions.repositories.UserRepository;
import com.wiley.permissions.security.web.UserPrincipal;
import com.wiley.permissions.services.ProductService;
import com.wiley.permissions.services.view.ProductSummaryView;
import com.wiley.permissions.web.shared.PermUserContext;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;
import com.wiley.permissions.web.shared.util.GenericFileView;
import com.wiley.sf.common.monitor.PerformanceMonitor.PerfTimer;

@Controller
/*@RequestMapping("/panels")*/
@RequestMapping
public class PanelsController  extends BaseAnnotatedController {
	private static final Log log = LogFactory.getLog(PanelsController.class);

	private ProductService productService;
	private CommonWorkRepository cwRepository;
	private UserRepository userRepository;

	@RequestMapping(value="/panels/summaryPanel", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView summaryPanel(HttpServletRequest request,
			@RequestParam("cwId") int cwId)
	throws Exception
	{
		log.debug("summaryPanel(): entered...");
		PerfTimer timer = cwRepository.getMonitor().startTimer("PanelsController::summaryPanel");

		UserPrincipal userPrincipal = PermUserContext.getCurrentUser(request);

		ProductSummaryView view = productService.loadPrimaryProductView(cwId);  // throws various exceptions

		ModelAndView mv = new ModelAndView("pages.landing.summaryPanel");
		mv.addObject("view", view);
		mv.addObject("cwId", cwId);

		mv.addObject("coverNoPermRequiredCapable", cwRepository.isNoPermRequiredCapable(cwId, false));
		mv.addObject("interiorNoPermRequiredCapable", cwRepository.isNoPermRequiredCapable(cwId, true));
		mv.addObject("coverCompleteCapable", cwRepository.isCompleteCapable(cwId, false));
		mv.addObject("interiorCompleteCapable", cwRepository.isCompleteCapable(cwId, true));

		CommonWork cw = cwRepository.loadCWById(cwId);
		mv.addObject("interiorCWStatus", cw.getInteriorCWStatus());
		mv.addObject("permissionInfo", cwRepository.loadCommonWorkPermissionInfoNoHeaders(cw));
		mv.addObject("coverCWStatus", cw.getCoverCWStatus());
		mv.addObject("cwStatusArray", CommonWorkStatus.ALL_CW_STATUS_ARRAY);
		mv.addObject("watched", cwRepository.isCwWatched(userPrincipal.getId(), cwId));
		mv.addObject("photoIllusTotalCount", view.getPhotoIllusTotalCount());
		mv.addObject("productPriority", view.getProductPriority());
		mv.addObject("files", cwRepository.loadFiles(cwId));// Added for implementing Build RN 160121-001880

		//DM-534 Added for Spreadsheet Export in Progress Start
		boolean hasExportInProgress = userRepository.doesExportInProgress(cwId);
		mv.addObject("hasExportInProgress", hasExportInProgress);
		//DM-534 Added for Spreadsheet Export in Progress End

		timer.stopTimer();

		return mv;
	}
	
	// Start: Added for implementing Build RN 160121-001880
	@RequestMapping(value="/panels/download_start", method = {RequestMethod.GET, RequestMethod.POST})
	public Object titleDownloadStart(@RequestParam("cwId") Integer cwId,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		// if multiple files we return a page with links to all the files, otherwise return empty
		log.debug("titleDownloadStart(): entered...");

		List<CwFile> titleFiles = cwRepository.loadFiles(cwId);

		// just one file, we will start the download
		if (null != titleFiles && titleFiles.size() == 1) {
			log.debug("title files list size ----> "+titleFiles.size());
			response.setContentType("text/plain");
			PrintWriter writer = response.getWriter(); // throws IOException
			writer.write(request.getContextPath() + "/sapp/panels/download_file?cwId="+cwId+"&fileId=" + titleFiles.get(0).getId());
			return null;
		}
		else {
			log.debug("In else block of titleDownloadStart(): ");
			ModelAndView mv = new ModelAndView("pages.cw.filesList");
			mv.addObject("files", titleFiles);
			mv.addObject("cwId", cwId);
			return mv;
		}
	}
	
	@RequestMapping(value="/panels/download_file", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView downloadTitleFile(HttpServletRequest request, 
			@RequestParam("cwId") Integer cwId, @RequestParam("fileId") Integer fileId)
			throws Exception {
		log.debug("downloadTitleFile(): entered...");
		List<CwFile> titleFiles = cwRepository.loadFiles(cwId);
		ModelAndView mv = new ModelAndView();
		GenericFileView view = new GenericFileView();
		CwFile file = null;
		if (null != titleFiles) {
			for (int i = 0; i < titleFiles.size(); i++) {
				file = titleFiles.get(i);
				// if values are equal or can be null
				log.debug("downloadTitleFile(): " + file.getId() + " param " + fileId);
				if (ObjectUtils.equals(fileId, file.getId())) {
					break;
				}
			}
		}
		if (null != file) {
			view.setFileName(file.getFileName());
			view.setContentType(file.getMimeType());
			view.setData(file.getData());
			mv.setView(view);
			return mv;
		} else {
			throw new Exception ("downloadTitleFile(): file was not found");
		}
	}
	// End: Added for implementing Build RN 160121-001880
	
	public ProductService getProductService() {
		return productService;
	}

	public void setProductService(ProductService productService) {
		this.productService = productService;
	}

	public CommonWorkRepository getCommonWorkRepository() {
		return cwRepository;
	}

	public void setCommonWorkRepository(CommonWorkRepository cwRepository) {
		this.cwRepository = cwRepository;
	}

	public UserRepository getUserRepository() {
		return userRepository;
	}

	public void setUserRepository(UserRepository userRepository) {
		this.userRepository = userRepository;
	}
}
