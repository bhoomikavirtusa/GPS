package com.wiley.permissions.web.shared.controllers.landing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.ModelAndView;

import com.wiley.permissions.domain.persistence.permissions.AssetUse;
import com.wiley.permissions.domain.persistence.permissions.CommonWork;
import com.wiley.permissions.domain.persistence.permissions.Component;
import com.wiley.permissions.domain.persistence.permissions.ComponentCategory;
import com.wiley.permissions.repositories.AssetUseRepository;
import com.wiley.permissions.repositories.CommonWorkRepository;
import com.wiley.permissions.services.AssetUseIndexService;
import com.wiley.permissions.services.AssetUseService;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;

@Controller
/*@RequestMapping("/landing/chapterMerge")*/
@RequestMapping
@SessionAttributes(ChapterMergeController.FORM_MODEL_NAME)
public class ChapterMergeController extends BaseAnnotatedController {

	private static final Log log = LogFactory.getLog(ChapterMergeController.class);

	private AssetUseRepository assetUseRepository;
	private AssetUseService assetUseService;
	private AssetUseIndexService assetUseIndexService;
	private CommonWorkRepository commonWorkRepository;

	protected final static String FORM_MODEL_NAME = "chapterMergeForm";

	@RequestMapping(value = "/landing/chapterMerge/selectchapter", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView start(@RequestParam(value = "cwId") Integer cwId)
	throws Exception
	{
		log.debug("--------select component: entered...cwId=" + cwId);

		// passing true/false for includeCovers here doesn't matter - non-chapters ignored
		List<Component> components = getCommonWorkRepository().loadComponentList(cwId, true);

		List<AssetUse> atsv = getAssetUseRepository().loadAssetUseListByCWId(cwId);

		List<AssetUse> chapters = new ArrayList<AssetUse>();
		for (int x = 0; x < atsv.size(); x++) {
			if (null != atsv.get(x).getComponent() && atsv.get(x).getComponent().getName().indexOf("Chapter") > -1) {
				chapters.add(atsv.get(x));
			}
		}

		// sort asset use list by component name
		Collections.sort(chapters, new Comparator<AssetUse>() {

			@Override
			public int compare(AssetUse p1, AssetUse p2) {
				String t1 = "";
				String t2 = "";
				if (null != p1.getComponent())
					t1 = p1.getComponent().getName();
				if (null != p2.getComponent())
					t2 = p2.getComponent().getName();
				return t1.compareToIgnoreCase(t2);
			}
		});

		// remove all but chapter components
		for (int x = 0; x < components.size(); x++) {
			if (components.get(x).getName().indexOf("Chapter") < 0) {
				components.remove(x);
				x--;
			}
		}
		// sort the available components
		Collections.sort(components, new Comparator<Component>() {

			@Override
			public int compare(Component c1, Component c2) {
				return c1.getName().compareToIgnoreCase(c2.getName());
			}
		});

		// now remove any asset uses that contain the last component;
		if (chapters.size() > 0) {
			Component tmp = components.get(components.size() - 1);
			String ts = tmp.getName();
			for (int x = 0; x < chapters.size(); x++) {
				if (chapters.get(x).getComponent().getName().compareTo(ts) == 0) {
					chapters.remove(x);
					x--;
				}
			}
		}

		ModelAndView mv = null;
		mv = new ModelAndView("pages.landing.asset.merge.chapter");

		if (chapters.size() > 0) {
			List<AssetUse> chapters2 = new ArrayList<AssetUse>();
			String prevChapter = "";
			for (int x = 0; x < chapters.size(); x++) {
				if (prevChapter.compareTo(chapters.get(x).getComponentName()) != 0) {
					chapters2.add(chapters.get(x));
					prevChapter = chapters.get(x).getComponentName();
				}
			}

			mv.addObject("chapters", chapters2);
			mv.addObject("usages", chapters);

		}
		mv.addObject("step", "gettingChapters");
		mv.addObject("nextUrl", "/sapp/landing/chapterMerge/selectchapter?cwId=" + cwId);
		mv.addObject("prevUrl", "/sapp/landing/chapterMerge/selectchapter?cwId=" + cwId);

		ChapterMergeForm form = new ChapterMergeForm();

		form.setCwId(cwId);
		form.setComponents(components);
		mv.addObject(FORM_MODEL_NAME, form);

		return mv;
	}

	@RequestMapping(value = "/landing/chapterMerge/selectchapter", method = RequestMethod.POST)
	public ModelAndView selectChapter(@ModelAttribute(FORM_MODEL_NAME) ChapterMergeForm form)
			throws Exception
	{
		log.debug("--------processing form for selectcomponent usage: entered...");
		ModelAndView mv = null;

		mv = new ModelAndView("redirect:/sapp/landing/chapterMerge/chapters");

		List<AssetUse> atsv = getAssetUseRepository().loadAssetUseListByCWId(form.getCwId());

		// sort asset use list by component name
		Collections.sort(atsv, new Comparator<AssetUse>() {

			@Override
			public int compare(AssetUse p1, AssetUse p2) {
				String t1 = "";
				String t2 = "";
				if (null != p1.getComponent())
					t1 = p1.getComponent().getName();
				if (null != p2.getComponent())
					t2 = p2.getComponent().getName();
				return t1.compareToIgnoreCase(t2);
			}
		});

		List<AssetRenumberView> filteredList = new ArrayList<AssetRenumberView>();

		for (int x = 0; x < atsv.size(); x++) {
			AssetUse aux = atsv.get(x);
			if (null != aux.getComponent() && aux.getComponent().getName().indexOf("Chapter") > -1) {
				AssetRenumberView arv = new AssetRenumberView(aux);
				arv.setNewPosition(aux.getComponent().getName()); // this handles no change of chapter name
				if (aux.getComponent().getId() == form.getSelectedChapter()) {
					arv.setNewPosition(form.getNextComponent(aux.getComponent()).getName());
					filteredList.add(arv);
				}
			}
			else {
				aux = null;
			}
		}

		form.setRenumList(filteredList);
		mv.addObject("chapters", filteredList);
		mv.addObject(FORM_MODEL_NAME, form);
		mv.addObject("nextUrl", "/sapp/landing/chapterMerge/chapters");
		mv.addObject("prevUrl", "/sapp/landing/chapterMerge/selectchapter?cwId=" + form.getCwId());
		mv.addObject("step", "chapters");

		return mv;
	}

	@RequestMapping(value = "/landing/chapterMerge/chapters", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView chaptersLoad(@ModelAttribute(FORM_MODEL_NAME) ChapterMergeForm form)
	throws Exception
	{
		log.debug("--------chaptersLoad: entered...cwId=" + form.getCwId());

		ModelAndView mv = null;
		mv = new ModelAndView("pages.landing.asset.merge.chapter");
		mv.addObject("nextUrl", "/sapp/landing/chapterMerge/chapters");
		mv.addObject("prevUrl", "/sapp/landing/chapterMerge/selectchapter?cwId=" + form.getCwId());
		mv.addObject("step", "chapters");

		mv.addObject(FORM_MODEL_NAME, form);

		return mv;
	}

	@RequestMapping(value = "/landing/chapterMerge/chapters", method = RequestMethod.POST)
	public ModelAndView mergeChaptersSubmit(@ModelAttribute(FORM_MODEL_NAME) ChapterMergeForm form)
			throws Exception
	{
		log.debug("--------mergeChaptersSubmit: entered...");

		List<AssetRenumberView> newAu = form.getRenumList();
		for (int x = 0; x < newAu.size(); x++) {
			AssetUse assetUse = getAssetUseRepository().loadAssetUseByIdForManageAsset(newAu.get(x).getAssetUseId());
			// find the new component
			for (int y = 0; y < form.getComponents().size(); y++) {
				Component tempCmp = form.getComponents().get(y);
				if (tempCmp.getName().compareTo(newAu.get(x).getNewPosition()) == 0) {
					assetUse.setComponent(tempCmp);
					// then update asset use with new component
					getAssetUseService().saveAssetUse(assetUse);
				}
			}
		}

		ModelAndView mv = new ModelAndView("redirect:/sapp/cwlanding/main?cwId=" + form.getCwId());
		return mv;
	}

	// ------------------------------------------------------------------------------------------
	// start of insert new chapter functionality
	// ------------------------------------------------------------------------------------------

	@RequestMapping(value = "/landing/chapterMerge/insertingChapter", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView insertingChapter(@RequestParam(value = "cwId") Integer cwId)
		throws Exception
	{
		log.debug("--------select component: entered...cwId=" + cwId);

		// passing true/false for includeCovers here won't matter - will be removed below
		List<Component> components = getCommonWorkRepository().loadComponentList(cwId, true);

		// remove non chapter components
		for (int x = 0; x < components.size(); x++) {
			if (components.get(x).getName().indexOf("Chapter") < 0) {
				components.remove(x);
				x--;
			}
		}

		// sort the available components
		Collections.sort(components, new Comparator<Component>() {

			@Override
			public int compare(Component c1, Component c2) {
				return c1.getName().compareToIgnoreCase(c2.getName());
			}
		});

		ModelAndView mv = null;
		mv = new ModelAndView("pages.landing.asset.merge.chapter");

		if (null != components && components.size() > 0) {
			mv.addObject("chapters", components);
		}
		mv.addObject("step", "insertingChapter");
		mv.addObject("nextUrl", "/sapp/landing/chapterMerge/insertingChapter?cwId=" + cwId);
		mv.addObject("prevUrl", "/sapp/landing/chapterMerge/insertingChapter?cwId=" + cwId);

		ChapterMergeForm form = new ChapterMergeForm();
		form.setCwId(cwId);
		form.setComponents(components);

		if (components.size() > 0) {
			String lastComponent = components.get(components.size() - 1).getName();
			String lcWk = lastComponent.substring(8);

			form.setChapterFrom(new Integer(lcWk) + 1 + "");
			form.setCurrentLargestChapter(lcWk);
			form.setChapterTo(new Integer(lcWk) + 5 + "");
		} else {
			form.setChapterFrom("01");
			form.setCurrentLargestChapter("00");
			form.setChapterTo("05");
		}

		// passing true/false here for includeCovers won't matter - they will be removed below
		List<Component> appendices = getCommonWorkRepository().loadComponentList(cwId, true);

		// remove non appendix components
		for (int x = 0; x < appendices.size(); x++) {
			if (appendices.get(x).getName().indexOf("Appendix") < 0) {
				appendices.remove(x);
				x--;
			}
		}

		// sort the available components
		Collections.sort(appendices, new Comparator<Component>() {

			@Override
			public int compare(Component c1, Component c2) {
				return c1.getName().compareToIgnoreCase(c2.getName());
			}
		});

		boolean alphaApendix = false;

		if (appendices.size() > 0) {
			String lastAppendix = appendices.get(appendices.size() - 1).getName();
			if (lastAppendix.length() == 8) {
				if (!StringUtils.isNumeric(lastAppendix.substring(8))) {
					alphaApendix = true;
					form.setAppendicesFrom("B");
					form.setCurrentLargestAppendix("A");
					form.setAppendicesTo("F");
				} else {
					form.setAppendicesFrom("02");
					form.setCurrentLargestAppendix("01");
					form.setAppendicesTo("06");
				}
			} else {
				String lcAp = lastAppendix.substring(9);
				if (!StringUtils.isNumeric(lcAp)) {
					alphaApendix = true;
					form.setAppendicesFrom(lcAp + "1" + "");
					form.setCurrentLargestAppendix(lcAp);
					form.setAppendicesTo(lcAp + "5" + "");
				} else {
					form.setAppendicesFrom(new Integer(lcAp) + 1 + "");
					form.setCurrentLargestAppendix(lcAp);
					form.setAppendicesTo(new Integer(lcAp) + 5 + "");
				}
			}
		} else {
			form.setAppendicesFrom("01");
			form.setCurrentLargestAppendix("00");
			form.setAppendicesTo("05");
		}

		mv.addObject(FORM_MODEL_NAME, form);

		return mv;
	}

	@RequestMapping(value = "/landing/chapterMerge/insertingChapter", method = RequestMethod.POST)
	public ModelAndView insertingChapterProcessForm(@ModelAttribute(FORM_MODEL_NAME) ChapterMergeForm form)
		throws Exception
	{
		log.debug("processing form for insertingChapter");

		CommonWork cw = getCommonWorkRepository().loadCWById(form.getCwId());

		ModelAndView mv = new ModelAndView("redirect:/sapp/cwlanding/main?cwId=" + form.getCwId());

		Boolean option2 = false;
		log.debug(" selected chapter:" + form.getSelectedChapter());

		if (form.getSelectedChapter() < 0 && !form.getAppendicesCheckBox() && !form.getChaptersCheckBox()) {
			return mv;
		}

		if (form.getChaptersCheckBox()) {
			if (StringUtils.isBlank(form.getChapterFrom())) {
				mv = new ModelAndView("pages.landing.asset.merge.chapter");
				mv.addObject("error", "both from and to chapters are required");
				mv.addObject(FORM_MODEL_NAME, form);
				mv.addObject("step", "insertingChapter");
				mv.addObject("nextUrl", "/sapp/landing/chapterMerge/insertingChapter?cwId=" + form.getCwId());
				mv.addObject("prevUrl", "/sapp/landing/chapterMerge/insertingChapter?cwId=" + form.getCwId());
				return mv;
			}
			if (StringUtils.isBlank(form.getChapterTo())) {
				mv = new ModelAndView("pages.landing.asset.merge.chapter");
				mv.addObject("error", "both from and to chapters are required");
				mv.addObject("step", "insertingChapter");
				mv.addObject("nextUrl", "/sapp/landing/chapterMerge/insertingChapter?cwId=" + form.getCwId());
				mv.addObject("prevUrl", "/sapp/landing/chapterMerge/insertingChapter?cwId=" + form.getCwId());
				mv.addObject(FORM_MODEL_NAME, form);
				return mv;
			}

			option2 = true;
			Integer idx1 = new Integer(form.getChapterFrom().trim());
			Integer idx2 = new Integer(form.getChapterTo().trim());

			for (int x = idx1; x <= idx2; x++) {
				try {
					// only add if not already in the database
					if (x > new Integer(form.getCurrentLargestChapter())) {
						Component newComponent = new Component();
						newComponent.setId(null);
						newComponent.setExternalId(null);
						newComponent.setCategory(ComponentCategory.CHAPTER);
						newComponent.setCommonWork(cw);
						String wk = x + "";
						if (wk.length() == 1) {
							wk = "0" + wk;
						}
						newComponent.setName("Chapter " + wk);
						newComponent.setSortOrder(new Integer(wk) + 100);
						getCommonWorkRepository().addComponentToCommonWork(newComponent);
					}
				} catch (Exception e) {
					// ignore if already exists
				}
			}
		} // end if

		if (form.getAppendicesCheckBox()) {
			if (StringUtils.isBlank(form.getAppendicesFrom())) {
				mv = new ModelAndView("pages.landing.asset.merge.chapter");
				mv.addObject("error", "both from and to Appendices are required");
				mv.addObject("step", "insertingChapter");
				mv.addObject("nextUrl", "/sapp/landing/chapterMerge/insertingChapter?cwId=" + form.getCwId());
				mv.addObject("prevUrl", "/sapp/landing/chapterMerge/insertingChapter?cwId=" + form.getCwId());
				return mv;
			}
			if (StringUtils.isBlank(form.getAppendicesTo())) {
				mv = new ModelAndView("pages.landing.asset.merge.chapter");
				mv.addObject("error", "both from and to Appendices are required");
				mv.addObject("step", "insertingChapter");
				mv.addObject("nextUrl", "/sapp/landing/chapterMerge/insertingChapter?cwId=" + form.getCwId());
				mv.addObject("prevUrl", "/sapp/landing/chapterMerge/insertingChapter?cwId=" + form.getCwId());
				return mv;
			}

			option2 = true;
			if (StringUtils.isNumeric(form.getAppendicesFrom().trim())) {
				Integer idx1 = new Integer(form.getAppendicesFrom().trim());
				Integer idx2 = new Integer(form.getAppendicesTo().trim());

				Integer preIdx = (int) form.getAppendicesFrom().trim().charAt(0);

				for (int x = idx1; x <= idx2; x++) {
					try {
						// only add if not already in the database
						if (x > new Integer(form.getCurrentLargestAppendix())) {
							Component newComponent = new Component();
							newComponent.setId(null);
							newComponent.setExternalId(null);
							newComponent.setCategory(ComponentCategory.APPENDIX);
							newComponent.setCommonWork(cw);
							String wk = x + "";
							if (wk.length() == 1) {
								wk = "0" + wk;
							}
							newComponent.setName("Appendix " + wk);
							newComponent.setSortOrder(new Integer(wk) + (preIdx * 100));
							getCommonWorkRepository().addComponentToCommonWork(newComponent);
						}
					} catch (Exception e) {
						// ignore if already exists
					}
				}
			} else {
				String first = form.getAppendicesFrom().trim();
				String last = form.getAppendicesTo().trim();
				String prefix = first.substring(0, first.length() - 1);

				Integer preIdx = (int) prefix.charAt(0);

				log.debug("firstL" + first + " last:" + last);
				log.debug("prefix:" + prefix);
				log.debug("idx:" + first.substring(first.length()));
				Integer idx1 = new Integer(first.substring(first.length() - 1));
				Integer idx2 = new Integer(last.substring(last.length() - 1));
				log.debug(idx1 + "  -  " + idx2);

				for (int x = idx1; x <= idx2; x++) {
					Component newComponent = new Component();
					newComponent.setId(null);
					newComponent.setExternalId(null);
					newComponent.setCategory(ComponentCategory.APPENDIX);
					newComponent.setCommonWork(cw);
					String wk = x + "";
					if (wk.length() == 1) {
						wk = "0" + wk;
					}
					newComponent.setName("Appendix " + prefix + wk);
					log.debug("creating:" + newComponent.getName());
					newComponent.setSortOrder(new Integer(wk) + (preIdx * 100));
					try {
						getCommonWorkRepository().addComponentToCommonWork(newComponent);
					} catch (Exception e) {
						log.debug("failed to create appendix error:" + e.getMessage());
						// ignore if already exists
					}
				}
			} // end else
		}

		if (option2) {
			return mv;
		}

		// user did not select option 2
		Component newComponent = new Component();
		newComponent.setId(null);
		newComponent.setExternalId(null);
		newComponent.setCategory(ComponentCategory.CHAPTER);
		newComponent.setCommonWork(cw);

		if (null == form.getComponents() || form.getComponents().size() < 1) {
			// this must be first chapter in the common work
			newComponent.setName("Chapter 01");
			newComponent.setSortOrder(100);
			getCommonWorkRepository().addComponentToCommonWork(newComponent);
			return mv;
		}

		// common work actually has chapters

		// start by renaming the chapters without changing component id's
		// this will sort the chapters and will make it possible to change the
		// names of the components without having to update the asset use record in them
		List<Component> newComponents = new ArrayList<Component>();
		Integer offset = 0;
		for (int x = 0; x < form.getComponents().size(); x++) {
			String suffix1 = x + 1 + offset + "";
			if (suffix1.length() < 2) {
				suffix1 = "0" + suffix1;
			}

			if (form.getComponents().get(x).getId() == form.getSelectedChapter()) {
				newComponent.setName("Chapter " + suffix1);
				newComponent.setSortOrder(new Integer(suffix1) + 100);
				offset = 1;
				suffix1 = x + 1 + offset + "";
				if (suffix1.length() < 2) {
					suffix1 = "0" + suffix1;
				}
				log.debug("adding:" + newComponent.getName());
				newComponents.add(newComponent);
			}
			form.getComponents().get(x).setName("Chapter " + suffix1);
			form.getComponents().get(x).setSortOrder(new Integer(suffix1) + 100);
			log.debug("processing:" + form.getComponents().get(x).getName());
			newComponents.add(form.getComponents().get(x));
		}

		// sort the available components
		Collections.sort(form.getComponents(), new Comparator<Component>() {

			@Override
			public int compare(Component c1, Component c2) {
				return c1.getName().compareToIgnoreCase(c2.getName());
			}
		});

		// first rename existing components (To prevent dup constraint error)
		for (int x = newComponents.size(); x > 0; x--) {
			Component tc = null;
			try {
				tc = getCommonWorkRepository().find(Component.class, newComponents.get(x).getId());
			} catch (Exception e) {
				tc = null;
			}
			if (null != tc) {
				log.debug("re-writting:" + newComponents.get(x).getName()
						+ " sort:" + newComponents.get(x).getSortOrder());
				tc.setName(newComponents.get(x).getName());
				tc.setSortOrder(newComponents.get(x).getSortOrder());
				log.debug("re-writting new values:"
						+ newComponents.get(x).getName() + " sort:"
						+ newComponents.get(x).getSortOrder());
				getCommonWorkRepository().saveRequiresNew(tc);
			}
		}

		// then insert new ones (To prevent dup constraint error)
		for (int x = 0; x < newComponents.size(); x++) {
			Component tc = null;
			try {
				tc = getCommonWorkRepository().find(Component.class, newComponents.get(x).getId());
			} catch (Exception e) {
				tc = null;
			}
			if (null == tc) {
				log.debug("writting:" + newComponents.get(x).getName() + " sort:" + newComponents.get(x).getSortOrder());
				getCommonWorkRepository().saveRequiresNew(newComponents.get(x));
			}
		}

		// update asset use indexes because the component name is no longer what it used to be
		List<AssetUse> assetUses = assetUseRepository.loadAssetUseListByCWId(form.getCwId());

		for (AssetUse au : assetUses) {
			assetUseIndexService.updateIndexNow(au.getId());
		}

		return mv;
	}

	public AssetUseRepository getAssetUseRepository() {
		return assetUseRepository;
	}

	public void setAssetUseRepository(AssetUseRepository assetUseRepository) {
		this.assetUseRepository = assetUseRepository;
	}

	public CommonWorkRepository getCommonWorkRepository() {
		return commonWorkRepository;
	}

	public void setCommonWorkRepository(CommonWorkRepository commonWorkRepository) {
		this.commonWorkRepository = commonWorkRepository;
	}

	public void setAssetUseIndexService(AssetUseIndexService assetUseIndexService) {
		this.assetUseIndexService = assetUseIndexService;
	}

	public AssetUseIndexService getAssetUseIndexService() {
		return assetUseIndexService;
	}

	public AssetUseService getAssetUseService() {
		return assetUseService;
	}

	public void setAssetUseService(AssetUseService assetUseService) {
		this.assetUseService = assetUseService;
	}
}
