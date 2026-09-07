package com.wiley.permissions.web.internal.controllers.admin;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.wiley.permissions.domain.persistence.permissions.FavoriteGroup;
import com.wiley.permissions.domain.persistence.permissions.Source;
import com.wiley.permissions.domain.persistence.permissions.SourceGroup;
import com.wiley.permissions.persistence.LabelValueBean;
import com.wiley.permissions.repositories.SourceRepository;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;
import com.wiley.permissions.web.shared.controllers.BaseFormBean.FormMode;

@Controller
/*@RequestMapping("/admin/multiSourceSelection/")*/
@RequestMapping
public class MultiSourceSelectionController extends BaseAnnotatedController {

	private final static Log log = LogFactory.getLog(MultiSourceSelectionController.class);

	protected final static String FORM_MODEL_NAME = "multisourceForm";


	private SourceRepository sourceRepository;

	@RequestMapping(value = "/admin/multiSourceSelection//search", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView searchMultiSource(@RequestParam(value="sgId", required=false) Integer sgId,
			@RequestParam(value="fsgId", required=false) Integer fsgId)
			throws Exception
	{
		log.debug("searchMultiSource(): entered...");

		ModelAndView mv = new ModelAndView("pages.admin.sourcegroup.multisource.search");
		MultiSourceForm form = new MultiSourceForm();

		if (null != sgId){
			form.setSourceGroupId(sgId);
		}
		if (null != fsgId) {
			form.setFavoriteSourceGroupId(fsgId);
		}

		mv.addObject(FORM_MODEL_NAME, form);

		return mv;
	}

	@RequestMapping(value = "/admin/multiSourceSelection/search", method = RequestMethod.POST)
	public ModelAndView getResults(
			@ModelAttribute(FORM_MODEL_NAME) MultiSourceForm form)
			throws Exception
	{
		log.debug("getResults(): entered...");

		ModelAndView mv = new ModelAndView("pages.admin.sourcegroup.multisource.search");

		if (null != form.getSourceSearch() && form.getSourceSearch().length() > 0) {
			form.setSources(getSourceRepository().searchSourceByName(form.getSourceSearch()));

			List<LabelValueBean> selSources = new ArrayList<LabelValueBean>();

			for (Source source : form.getSources()) {
				selSources.add(new LabelValueBean(source.getName(), source.getId() + ""));
			}

			form.setSelectedSources(selSources);
			form.setSourceSearch(null);
			mv.addObject(FORM_MODEL_NAME, form);
			return mv;
		}

		if (form.getSelectedSources().size() > 0) {
			boolean sourcesSelected = false;
			// save selected sources
			SourceGroup sg = null;
			FavoriteGroup fg = null;
			if (null != form.getSourceGroupId()) {
			    sg = getSourceRepository().loadSourceGroupById(form.getSourceGroupId());
			}
			if (null != form.getFavoriteSourceGroupId()) {
				fg = getSourceRepository().loadFavoriteSourceGroupById(form.getFavoriteSourceGroupId());
			}

			for (LabelValueBean source : form.getSelectedSources()) {
					if (null != source.getValue()) {
						Integer sourceId = new Integer(source.getValue().substring(source.getValue().indexOf("_") + 1));
						Source dsrc = getSourceRepository().loadSourceById(sourceId);

						if (null != sg) {
							dsrc.setSourceGroup(sg);
							dsrc.setSourceGroupName(sg.getName());
							dsrc.setNofly(sg.isNofly());
							dsrc = getSourceRepository().saveSource(dsrc);
							if(!sg.getSources().contains(dsrc)) {
								sg.getSources().add(dsrc);
							}
						}

						if (null != fg) {
							if (!fg.getSources().contains(dsrc)) {
								fg.getSources().add(dsrc);
							}
						}

						sourcesSelected = true;

					}
			}
			form.setSelectedSources(new ArrayList<LabelValueBean>());
			if (sourcesSelected) {
				if (null != fg) {
					fg = getSourceRepository().saveFavoriteGroup(fg);
				    mv = new ModelAndView("redirect:/sapp/admin/manageFavoriteGroup/manage_Group?id=" + fg.getId());
				    return mv;
				}
				if (null != sg) {
					sg = getSourceRepository().saveSourceGroup(sg, new ArrayList<Integer>());
				}
				mv = new ModelAndView("pages.admin.source.group.manage");
				ManageSourceGroupForm sgForm = new ManageSourceGroupForm();
				if(null!=sg){               
				sgForm.setGroup(sg);
				if(sg.isNofly()){		
				sgForm.setNofly(sg.isNofly());
				}
				else{							
					sgForm.setNofly(false); 	
				}
					
				// setOriginalMode to MODIFY or else MODIFY button will not show up
				sgForm.setOriginalMode(FormMode.MODIFY);
				} 								
				mv.addObject("manageSourceGroupForm", sgForm);
				if (null != sg){
					mv.addObject("groupId", sg.getId());
				}
				return mv;

			} else {
				mv = new ModelAndView("pages.admin.sourcegroup.multisource.search");
			}

			mv.addObject(FORM_MODEL_NAME, form);
		}

		return mv;
	}

	public SourceRepository getSourceRepository() {
		return sourceRepository;
	}

	public void setSourceRepository(SourceRepository sourceRepository) {
		this.sourceRepository = sourceRepository;
	}
}
