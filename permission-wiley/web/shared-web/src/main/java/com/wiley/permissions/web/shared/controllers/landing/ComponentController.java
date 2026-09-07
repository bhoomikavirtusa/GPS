package com.wiley.permissions.web.shared.controllers.landing;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.ModelAndView;

import com.wiley.permissions.common.utils.UniqueConstraintViolationException;
import com.wiley.permissions.common.utils.ValidateException;
import com.wiley.permissions.domain.persistence.permissions.CommonWork;
import com.wiley.permissions.domain.persistence.permissions.Component;
import com.wiley.permissions.domain.persistence.permissions.ComponentCategory;
import com.wiley.permissions.domain.persistence.permissions.Privilege;
import com.wiley.permissions.domain.persistence.permissions.Role;
import com.wiley.permissions.persistence.PersistenceException;
import com.wiley.permissions.repositories.CommonWorkRepository;
import com.wiley.permissions.services.CommonWorkService;
import com.wiley.permissions.web.shared.PermUserContext;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;
import com.wiley.permissions.web.shared.forms.BigComponentForm;
import com.wiley.permissions.web.shared.forms.ComponentForm;

/**
 *
 * @author smarkoff
 */
@Controller
/*@RequestMapping("/landing/component/")*/
@RequestMapping
@SessionAttributes( { "componentCategoryList", "componentList" })
public class ComponentController extends BaseAnnotatedController {

	private static final Log log = LogFactory.getLog(ComponentController.class);

	private static final String EDIT_FORM_NAME = "editForm";
	private static final String FORM_MODEL_NAME = "componentForm";
	private static final String BIG_FORM_MODEL_NAME = "bigComponentForm";

	private CommonWorkService commonWorkService = null;
	private CommonWorkRepository cwRepository = null;

	@Override
	protected List<String> getReferenceDataNames(HttpServletRequest request) throws Exception
	{
		List<String> output = new ArrayList<String>();
		// output.add("componentCategoryList");
		return output;
	}

	@RequestMapping(value="/landing/component/create", method = {RequestMethod.GET, RequestMethod.POST})
	public String createView(Model model, HttpServletRequest request, @ModelAttribute(BIG_FORM_MODEL_NAME) BigComponentForm form) throws Exception
	{
		log.debug("createView(): entered...");

		createBigForm(form);
		model.addAttribute("componentCategoryList", loadComponentCategories (request));
        return "pages.landing.component.create";
	}

	/*@GetMapping("/manage")*/
	@RequestMapping(value="/landing/component/manage", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView manageView(Model model, HttpServletRequest request, @ModelAttribute(FORM_MODEL_NAME) ComponentForm form) throws Exception
	{
		log.debug("manageView(): entered...");

		CommonWork cw = loadCommonWork(request);
		List<Component> componentSortList = cwRepository.loadComponentList(cw.getId(), true);
		if (null != componentSortList && componentSortList.size() > 1) {
			// first resort the components to get rid of 0 sort values
			for(int x = 0; x < componentSortList.size(); x++) {
				componentSortList.get(x).setSortOrder(x + 1);
				getRepository().save(componentSortList.get(x));	
			}
		}
		
		List<Component> componentList = loadComponentList(request, cw.getId());

		if (null == componentList || componentList.size() < 1) {
			return new ModelAndView ("redirect:/sapp/landing/component/create");
		}
		
	

		form.getComponent().setCommonWork(cw);
        model.addAttribute("componentList", componentList);
		model.addAttribute("componentCategoryList", loadComponentCategories (request));
        return new ModelAndView ("pages.landing.component.manage");
	}

	/*@PostMapping("/delete")*/
	@RequestMapping(value="/landing/component/delete", method = RequestMethod.POST)
	public ModelAndView delete(@RequestParam("deleteId") Integer deleteId)
		throws Exception
	{
		log.debug("delete(): entered...");

		try {
			getCommonWorkService().deleteComponentById(deleteId);
		}
		catch (ValidateException ve) {
			// cannot delete because it is used
			return new ModelAndView("redirect:/sapp/landing/component/manage" + "?generalMessage=" + ve.getMessage());
		}
	    return new ModelAndView("redirect:/sapp/landing/component/manage" + "?generalMessage=Delete+Successful");
	}

	@RequestMapping(value = "/landing/component/sort", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView sort(HttpServletRequest request,
							@RequestParam(value="componentId", required=true) int componentId,
							@RequestParam(value="order", required=true) int order)
			throws Exception
	{
		log.debug("sort()");
		CommonWork cw = PermUserContext.getCurrentCommonWork(request);
		List<Component> components = loadComponentList(request, cw.getId());
		Component component = getRepository().find(Component.class, componentId);

		int idx = components.indexOf(component);
		if (-1 == idx) {
			log.error("Failed to find the component within loadAll");
			throw new Exception ("Failed to find the component within loadAll");
		}
		log.debug("sort(): idx " + idx + " order " + order);
		Component scomponent = components.get(idx + order);
		// switch sortOrder
		int sortOrder = scomponent.getSortOrder();
		log.debug("sort(): switch sort order " + component.getSortOrder() + " with " + sortOrder);
		scomponent.setSortOrder (component.getSortOrder());
		component.setSortOrder (sortOrder);
		getRepository().save(component);
		getRepository().save(scomponent);
		ModelAndView mv = new ModelAndView("redirect:/sapp/landing/component/manage");
		return mv;
	}

	@GetMapping("/editRow")
	public String editRow(Model model, HttpServletRequest request, @RequestParam("componentId") int componentId) throws Exception {
		log.debug("editRow(): entered...");

		Component component = cwRepository.loadComponentById(componentId);
		model.addAttribute("component", component);
		// and we have "componentCategoryList" from getReferenceDataNames() above
		return "pages.landing.component.editRow";
	}

	/*@GetMapping("/saveRow")*/
	@RequestMapping(value = "/landing/component/saveRow", method = {RequestMethod.GET, RequestMethod.POST})
	public String saveRow(Model model, @ModelAttribute(EDIT_FORM_NAME) ComponentForm form) throws PersistenceException {
		log.debug("saveRow(): entered...");

    	Component component = cwRepository.find(Component.class, form.getComponent().getId());

    	if (component == null) {  // Not expected but check
    		throw new RuntimeException("component for id [" + form.getComponent().getId() + "] not found");
    	}

    	component.setName(form.getComponent().getName());
    	component.setCategory(form.getComponent().getCategory());
    	component.setSortOrder(form.getComponent().getSortOrder());

    	log.debug("saveRow(): component = " + component);

    	component = getCommonWorkService().saveComponent(component);

		model.addAttribute("component", component);

		return "pages.landing.component.reloadRow";
	}

	/*@GetMapping("/simpleAdd")*/
	@RequestMapping(value = "/landing/component/simpleAdd", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView simpleAdd(Model model, HttpServletRequest request,
			@ModelAttribute(FORM_MODEL_NAME) ComponentForm form,
            BindingResult bindingResult)
	    throws Exception
	{
		log.debug("simpleAdd(): entered...");

		getValidator().validate(form, bindingResult);

		// If try to always call loadProductAndComponentList here
		// and not call below, should work, but get exception saying
		// can't lazy load the product componentList
		// - So, have hidden field in form to copy the product id from
		// the GET to the POST, so we don't have to load the product here.
		// - We also don't have to load the componentList unless we
		// are going back to the formView because of a form validation
		// or processing error.

		if (bindingResult.hasErrors()) {
			return new ModelAndView ("pages.landing.component.manage");
		}

		try {
		    Component component = getCommonWorkService().addComponentToCommonWork (form.getComponent(), true);
		    form.setComponent(component);
		}
		catch (UniqueConstraintViolationException ex) {
			Object [] errorArgs = { form.getComponent().getName() };
			bindingResult.rejectValue("component.name", null, errorArgs, "{0} is a duplicate name");
			return new ModelAndView ("pages.landing.component.manage");
		}

        return new ModelAndView ("redirect:/sapp/landing/component/manage");
	}

	/*@GetMapping("/bigAdd")*/
	@RequestMapping(value = "/landing/component/bigAdd", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView bigAdd(Model model, HttpServletRequest request,
			@ModelAttribute(BIG_FORM_MODEL_NAME) BigComponentForm form,
            BindingResult bindingResult)
	    throws Exception
	{
		log.debug("bigAdd(): entered...");

		getValidator().validate(form, bindingResult);

		if (bindingResult.hasErrors()) {
			return new ModelAndView	("pages.landing.component.create");
		}

		CommonWork cw = loadCommonWork (request);

		ComponentCategory chapterCategory = (ComponentCategory)
			getReferenceDataCache().getObjectByType(ComponentCategory.class, "CH");
		ComponentCategory appendixCategory = (ComponentCategory)
		    getReferenceDataCache().getObjectByType(ComponentCategory.class, "AP");

		Component c = form.getComponent1();
		String field = "component1.name";

		try {
		    checkComponent(form.getAdd1(), c, cw);
		    c = form.getComponent2();
		    field = "component2.name";
		    checkComponent(form.getAdd2(), c, cw);
		    c = form.getComponent3();
		    field = "component3.name";
		    checkComponent(form.getAdd3(), c, cw);
		    c = form.getComponent4();
		    field = "component4.name";
		    checkComponent(form.getAdd4(), c, cw);
		    c = form.getComponent5();
		    field = "component5.name";
		    checkComponent(form.getAdd5(), c, cw);
		    c = form.getComponent6();
		    field = "component6.name";
		    checkComponent(form.getAdd6(), c, cw);
		    c = form.getComponent7();
		    field = "component7.name";
		    checkComponent(form.getAdd7(), c, cw);
		    c = form.getComponent8();
		    field = "component8.name";
		    checkComponent(form.getAdd8(), c, cw);
		    c = form.getComponent9();
		    field = "component9.name";
		    checkComponent(form.getAdd9(), c, cw);
		    field = null;

		    if (form.getAddChapters()) {
		    	int sortOrder = form.getChapterStartSortOrder();
		    	for (int num = form.getChapterStart(); num <= form.getChapterEnd(); num++) {
		    		c = new Component();
		    		if (num < 10) {
		    		    c.setName("Chapter 0" + num);
		    		}
		    		else {
		    			c.setName("Chapter " + num);
		    		}
		    		c.setCategory(chapterCategory);
		    		c.setSortOrder(sortOrder);
		    		c.setCommonWork(cw);
		    		getCommonWorkService().addComponentToCommonWork(c, true);
		    		sortOrder++;
		    	}
		    }

		    if (form.getAddAppendices()) {
		    	char start = Character.toUpperCase (form.getAppendixStart());
		    	char end = Character.toUpperCase (form.getAppendixEnd());
		    	start = (start < 'A' ? 'A' : start); // min 'A'
		    	end = (end > 'Z' ? 'Z' : end); // max 'Z'
		    	int sortOrder = form.getAppendixStartSortOrder();
		    	for (char num = start; num <= end; num++) {
		    		c = new Component();
		    		c.setName("Appendix " + num);
		    		c.setCategory(appendixCategory);
		    		c.setSortOrder(sortOrder);
		    		c.setCommonWork(cw);
		    		getCommonWorkService().addComponentToCommonWork(c, true);
		    		sortOrder++;
		    	}
		    }
		}
		catch (UniqueConstraintViolationException ex) {
			Object [] errorArgs = { c.getName() };
			bindingResult.rejectValue(field, null, errorArgs, "{0} is a duplicate name");
			return new ModelAndView	("pages.landing.component.create");
		}

        return new ModelAndView (getSuccessView());
	}

	private void createBigForm(BigComponentForm bigForm) throws Exception {
		// (initial Component names are set in BigComponentForm ctor)

		// set initial categories to: Cover, Preface, Introduction,
		// [TOC] - use Front Matter (FRW)? for this, Foreword,
		// [Afterword, Conclusion] - use Epilogue (EP)for these
		// lnagy we disable only in UI the cover components
		bigForm.getComponent1().setCategory((ComponentCategory)
			getReferenceDataCache().getObjectByType(ComponentCategory.class, "CVW"));
		bigForm.getComponent2().setCategory((ComponentCategory)
				getReferenceDataCache().getObjectByType(ComponentCategory.class, "CVW"));
		bigForm.getComponent3().setCategory((ComponentCategory)
				getReferenceDataCache().getObjectByType(ComponentCategory.class, "CVW"));
		bigForm.getComponent4().setCategory((ComponentCategory)
				getReferenceDataCache().getObjectByType(ComponentCategory.class, "PRF"));
		bigForm.getComponent5().setCategory((ComponentCategory)
				getReferenceDataCache().getObjectByType(ComponentCategory.class, "IT"));
		bigForm.getComponent6().setCategory((ComponentCategory)
				getReferenceDataCache().getObjectByType(ComponentCategory.class, "FRW"));
		bigForm.getComponent7().setCategory((ComponentCategory)
				getReferenceDataCache().getObjectByType(ComponentCategory.class, "FW"));
		bigForm.getComponent8().setCategory((ComponentCategory)
				getReferenceDataCache().getObjectByType(ComponentCategory.class, "PR"));
		bigForm.getComponent9().setCategory((ComponentCategory)
				getReferenceDataCache().getObjectByType(ComponentCategory.class, "PR"));

		bigForm.getComponent1().setSortOrder(1);
		bigForm.getComponent2().setSortOrder(2);
		bigForm.getComponent3().setSortOrder(3);
		bigForm.getComponent4().setSortOrder(4);
		bigForm.getComponent5().setSortOrder(5);
		bigForm.getComponent6().setSortOrder(6);
		bigForm.getComponent7().setSortOrder(7);
		bigForm.getComponent8().setSortOrder(150);
		bigForm.getComponent9().setSortOrder(160);

		bigForm.setChapterStart(1);
		bigForm.setChapterEnd(10);
		bigForm.setChapterStartSortOrder(100);
		bigForm.setAppendixStart('A');
		bigForm.setAppendixEnd('K');  // b/c don't do "Appendix A" by default
		bigForm.setAppendixStartSortOrder(200);
	}

    private List<Component> loadComponentList(HttpServletRequest request, int cwId) {
		boolean includeCovers = request.isUserInRole(Role.EMPLOYEE_DEFAULT.getCode()) ||
								request.isUserInRole(Privilege.COVER_ASSETS.getCode());
		List<Component> componentList = cwRepository.loadComponentList(cwId, includeCovers);

        return componentList;
    }


	private CommonWork loadCommonWork(HttpServletRequest request) throws PersistenceException {

		CommonWork cw = PermUserContext.getCurrentCommonWork(request);

		cw = getCommonWorkRepository().loadCWById(cw.getId());
		return cw;
	}

    private List<Object> loadComponentCategories(HttpServletRequest request) throws Exception
    {
		boolean includeCovers = request.isUserInRole(Role.EMPLOYEE_DEFAULT.getCode()) ||
								request.isUserInRole(Privilege.COVER_ASSETS.getCode());
		List<Object> all = getReferenceDataCache().get("componentCategoryList");

		if (!includeCovers) {
			for (Iterator<Object> iter = all.iterator(); iter.hasNext(); ) {
				ComponentCategory category = (ComponentCategory) iter.next();
				if (category.equals(ComponentCategory.COVER)) {
				    iter.remove();
				}
			}
		}
        return all;
    }

    private void checkComponent(boolean add, Component component, CommonWork commonWork)
    throws UniqueConstraintViolationException, PersistenceException, Exception {
    	if (!add)  return;
    	// It would be nice to give the user an error msg if they check
    	// a component but put a blank name, but this will rarely happen
    	// since we pre-fill the fields - so just ignore.
    	// (If wanted an error msg, throw some kind of exception and catch
    	// in calling method.)
    	if (StringUtils.isBlank(component.getName()))  return;

    	component.setCommonWork(commonWork);
		component = getCommonWorkService().addComponentToCommonWork(component, true);
    }

	public CommonWorkService getCommonWorkService() {
		return commonWorkService;
	}

	public void setCommonWorkService(CommonWorkService commonWorkService) {
		this.commonWorkService = commonWorkService;
	}

	public CommonWorkRepository getCommonWorkRepository() {
		return cwRepository;
	}

	public void setCommonWorkRepository(CommonWorkRepository cwRepositry) {
		this.cwRepository = cwRepositry;
	}
}
