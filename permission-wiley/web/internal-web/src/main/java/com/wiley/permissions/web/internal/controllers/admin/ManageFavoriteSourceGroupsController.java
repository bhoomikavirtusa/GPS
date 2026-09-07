package com.wiley.permissions.web.internal.controllers.admin;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.ModelAndView;

import com.wiley.permissions.domain.persistence.permissions.FavoriteGroup;
import com.wiley.permissions.repositories.SourceRepository;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;

@Controller
/*@RequestMapping("/admin/manageFavoriteGroup/")*/
@RequestMapping
@SessionAttributes(ManageFavoriteSourceGroupsController.FORM_MODEL_NAME)
public class ManageFavoriteSourceGroupsController extends BaseAnnotatedController {

	// --------------------- static data ---------------------------------

	private final static Log log = LogFactory.getLog(ManageFavoriteSourceGroupsController.class);
	protected final static String FORM_MODEL_NAME = "ManageFavoriteSourceGroupsForm";

	// --------------------- instance data -------------------------------

	private SourceRepository sourceRepository;

	@RequestMapping(value = "/admin/manageFavoriteGroup/manage_Group", method = {RequestMethod.GET, RequestMethod.POST})
		public ModelAndView manageGroup(HttpServletRequest request,
				@RequestParam(value = "id", required=false) Integer id)
	throws Exception
	{
		log.debug("manageGroup(): entered..., id = " + id);
		ModelAndView mv = new ModelAndView("pages.admin.manage.favorite.source.groups");
		ManageFavoriteSourceGroupsForm form = new ManageFavoriteSourceGroupsForm();

		form.setFavoriteGroupDescription("test test");
		form.setFavoriteGroupId(-1);

	 	List<FavoriteGroup> favoriteGroups = getSourceRepository().loadAll(FavoriteGroup.class);
	 	if (favoriteGroups.size() > 0) {
	 		if (null != id) {
	 				for (int x = 0; x < favoriteGroups.size(); x++) {
	 					if (favoriteGroups.get(x).getId().equals(id)) {
	 						form.setFavoriteGroupDescription(favoriteGroups.get(x).getDescription());
	 			 			form.setFavoriteGroupId(favoriteGroups.get(x).getId());
	 			 			form.setFavoriteGroup(favoriteGroups.get(x));
	 					}
	 				}
	 		} else {
	 			form.setFavoriteGroupDescription(favoriteGroups.get(0).getDescription());
	 			form.setFavoriteGroupId(favoriteGroups.get(0).getId());
	 			form.setFavoriteGroup(favoriteGroups.get(0));
	 		}
	 	}
		form.setFavoriteGroups(favoriteGroups);

		mv.addObject(FORM_MODEL_NAME, form);

		return mv;
	}

	@RequestMapping(value = "/admin/manageFavoriteGroup/manage_group", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView processForm(
			@ModelAttribute(FORM_MODEL_NAME) ManageFavoriteSourceGroupsForm form,
			@RequestParam(value = "id", required=false) Integer id)
	throws Exception
	{
		log.debug("processForm(): entered..., id = " + id);
		log.debug("form.getFavoriteGroupId() = " + form.getFavoriteGroupId()
			+ ", form.getFavoriteGroupDescription() = " + form.getFavoriteGroupDescription());

		if (form.getFavoriteGroupId() == -1 && (null != form.getFavoriteGroupDescription()
				|| StringUtils.isNotBlank(form.getFavoriteGroupDescription()) )) {
			FavoriteGroup fg = new FavoriteGroup();
			fg.setDescription(form.getFavoriteGroupDescription());
			fg = getSourceRepository().save(fg);
			form.getFavoriteGroups().add(fg);
			form.setFavoriteGroupId(fg.getId());
			form.setFavoriteGroup(fg);

		} else {
			Integer did = form.getFavoriteGroupId();
			for (int x=0; x < form.getFavoriteGroups().size(); x++) {
				if (form.getFavoriteGroups().get(x).getId().equals(did)) {
					form.getFavoriteGroups().get(x).setDescription(form.getFavoriteGroupDescription());
					getSourceRepository().save(form.getFavoriteGroups().get(x));
				}
			}
		}

		getReferenceDataCache().invalidate(FavoriteGroup.class);

		ModelAndView mv = new ModelAndView("pages.admin.manage.favorite.source.groups");

		mv.addObject(FORM_MODEL_NAME, form);
		return mv;
	}

	@RequestMapping(value = "/admin/manageFavoriteGroup/select_group", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView selectGroup(HttpServletRequest request,
			@ModelAttribute(FORM_MODEL_NAME) ManageFavoriteSourceGroupsForm form,
			@RequestParam(value = "id", required=true) Integer id)
	throws Exception
	{
		log.debug("selectGroup(): entered..., id = " + id);
		for (int x = 0; x < form.getFavoriteGroups().size(); x++) {
			if (form.getFavoriteGroups().get(x).getId().equals(id)) {
				form.setFavoriteGroupDescription(form.getFavoriteGroups().get(x).getDescription());
				form.setFavoriteGroupId(form.getFavoriteGroups().get(x).getId());
				form.setFavoriteGroup(form.getFavoriteGroups().get(x));
			}
		}

		ModelAndView mv = new ModelAndView("pages.admin.manage.favorite.source.groups");
		// String dmsg = "";

		mv.addObject(FORM_MODEL_NAME, form);
		return mv;
	}

	@RequestMapping(value = "/admin/manageFavoriteGroup/remove_source", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView removeSource(HttpServletRequest request,
			@ModelAttribute(FORM_MODEL_NAME) ManageFavoriteSourceGroupsForm form,
			@RequestParam(value = "id", required=true) Integer id)
	throws Exception
	{
		log.debug("removeSource(): entered..., id = " + id);
		
		for (int x=0; x < form.getFavoriteGroup().getSources().size(); x++) {
			if (form.getFavoriteGroup().getSources().get(x).getId().equals(id)) {
				form.getFavoriteGroup().getSources().remove(x);
			}
		}
		
		getSourceRepository().saveFavoriteGroup(form.getFavoriteGroup());

		ModelAndView mv = new ModelAndView("pages.admin.manage.favorite.source.groups");

		mv.addObject(FORM_MODEL_NAME, form);
		return mv;
	}


	// --------------------- getters and setters --------------------------

	public SourceRepository getSourceRepository() {
		return sourceRepository;
	}

	public void setSourceRepository(SourceRepository sourceRepository) {
		this.sourceRepository = sourceRepository;
	}
}
