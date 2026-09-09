package com.wiley.permissions.web.shared.controllers.landing;

import java.util.List;
import java.util.Properties;

import javax.persistence.PersistenceException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import com.wiley.permissions.domain.persistence.permissions.GuidelineFile;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;
import com.wiley.permissions.web.shared.util.GenericFileView;

/**
 * This class should be in internal-web/admin but because we have to show a list in 
 * author-web also, I have it under shared/landing.
 * @author lnagy
 */
@Controller
/*@RequestMapping(value={"/admin/guidelines", "/guidelines"})*/
@RequestMapping
public class ManageGuidelinesController  extends BaseAnnotatedController {

	private final static Log log = LogFactory.getLog(ManageGuidelinesController.class);

	private static final String VIEW_ADMIN = "pages.admin.guidelines.main";
	private static final String VIEW_AUTHOR = "pages.guidelines";

	private String resolveSuccessView(javax.servlet.http.HttpServletRequest request) {
		String uri = request != null ? request.getRequestURI() : null;
		if (uri != null && uri.contains("/guidelines/smalllist")) {
			return VIEW_AUTHOR;
		}
		return VIEW_ADMIN;
	}

	@RequestMapping(value={"/admin/guidelines/download", "/guidelines/download"}, method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView download(@RequestParam("fileId") int fileId)
			throws Exception
	{
		log.debug("download(): fileId = " + fileId);

		ModelAndView mv = new ModelAndView();
		GenericFileView view = new GenericFileView();

		GuidelineFile guideline = getRepository().find(GuidelineFile.class, fileId);

		view.setFileName(guideline.getFileName());

		log.debug("File Data: " + guideline.getFileData().length);
		view.setData(guideline.getFileData());
		mv.setView(view);
		return mv;
	}

	/*@RequestMapping(value = "/delete", method = {RequestMethod.GET, RequestMethod.POST})*/
	@RequestMapping(value={"/admin/guidelines/delete", "/guidelines/delete"}, method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView delete(@RequestParam(value="fileId", required=true) int fileId)
			throws Exception
	{
		log.debug("delete(): fileId = " + fileId);
		getRepository().remove(GuidelineFile.class, fileId);
		ModelAndView mv = new ModelAndView("redirect:/sapp/admin/guidelines/list");
		return mv;
	}

	@RequestMapping(value = {"/admin/guidelines/list", "/guidelines/smalllist"}, method = {RequestMethod.GET, RequestMethod.POST})
	
	public ModelAndView list(javax.servlet.http.HttpServletRequest request)
			throws Exception
	{
		log.debug("list()");
		Properties prop = new Properties();
		prop.put("orderBy", "sortOrder");
		List<GuidelineFile> files = getRepository().loadAll(GuidelineFile.class, prop);
		ModelAndView mv = new ModelAndView(resolveSuccessView(request));
		mv.addObject("guidelines", files);
		return mv;
	}

	@RequestMapping(value = "/admin/guidelines/add", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView add()
			throws Exception
	{
		log.debug("add()");
		GuidelineFile guideline = new GuidelineFile ();
		ModelAndView mv = new ModelAndView(VIEW_ADMIN);
		mv.addObject("guidelineFile", guideline);
		return mv;
	}

	@RequestMapping(value = "/admin/guidelines/sort", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView sort(@RequestParam(value="fileId", required=true) int fileId, @RequestParam(value="order", required=true) int order)
			throws Exception
	{
		log.debug("sort()");
		Properties prop = new Properties();
		prop.put("orderBy", "sortOrder");
		List<GuidelineFile> files = getRepository().loadAll(GuidelineFile.class, prop);
		GuidelineFile guideline = getRepository().find(GuidelineFile.class, fileId);
		
		int idx = files.indexOf(guideline);
		if (-1 == idx) {
			log.error("Failed to find the file within loadAll");
			throw new Exception ("Failed to find the file within loadAll");
		}
		GuidelineFile sguide = files.get(idx + order);
		// switch sortOrder
		int sortOrder = sguide.getSortOrder();
		sguide.setSortOrder (guideline.getSortOrder());
		guideline.setSortOrder (sortOrder);
		getRepository().save(guideline);
		getRepository().save(sguide);
		ModelAndView mv = new ModelAndView("redirect:/sapp/admin/guidelines/list");
		return mv;
	}

	@RequestMapping(value = "/admin/guidelines/save", method = RequestMethod.POST)
	public ModelAndView save(@RequestParam("file") MultipartFile file, 
			@RequestParam("displayName") String displayName, 
			@ModelAttribute("guidelineFileForm") GuidelineFile gFile, BindingResult bindingResult)
			throws Exception
	{
		log.debug("save(): entered...");
		// TODO: There should be validation against max file size here
		gFile.setFileData(file.getBytes());
		gFile.setFileName(file.getOriginalFilename());
		gFile.setMimeType(file.getContentType());
		// gFile.setDisplayName(displayName);
		gFile.setSortOrder(getRepository().getCount(GuidelineFile.class) + 1);
		try {
			getRepository().save(gFile);
		} catch (PersistenceException e) {			
			Object [] errorArgs = { displayName };
			bindingResult.rejectValue("displayName", null, errorArgs, "{0} is a duplicate name");
			ModelAndView mv = new ModelAndView(VIEW_ADMIN);
			mv.addObject("guidelineFile", gFile);			
			return mv;
		}
		ModelAndView mv = new ModelAndView("redirect:/sapp/admin/guidelines/list");
		return mv;
	}
}
