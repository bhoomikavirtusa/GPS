package com.wiley.permissions.web.internal.controllers.admin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.persistence.NoResultException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.ModelAndView;

import com.wiley.permissions.domain.persistence.permissions.RoyaltyFreeDeal;
import com.wiley.permissions.domain.persistence.permissions.Source;
import com.wiley.permissions.domain.persistence.permissions.SourceGroup;
import com.wiley.permissions.repositories.SourceRepository;
import com.wiley.permissions.repositories.UserRepository;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;
import com.wiley.permissions.web.shared.controllers.BaseFormBean.FormMode;

@Controller
/*@RequestMapping("/admin/manageSourceGroup")*/
@RequestMapping
@SessionAttributes(ManageSourceGroupController.FORM_NAME)
public class ManageSourceGroupController
extends BaseAnnotatedController
{
	private final static Log log = LogFactory.getLog(ManageSourceGroupController.class);

	protected final static String FORM_NAME = "manageSourceGroupForm";

	private UserRepository userRepository;
	private SourceRepository sourceRepository;

	private String finishViewName;
	private String [] pages;


	@Override
	protected List<String> getReferenceDataNames(HttpServletRequest request)
	throws Exception
	{
		List<String> output = new ArrayList<String>();

		output.add("accountTypes");
		return output;
	}

	@RequestMapping(value="/admin/manageSourceGroup/form" ,method = {RequestMethod.POST, RequestMethod.GET})
	@SuppressWarnings("incomplete-switch")
	public ModelAndView form(HttpServletRequest request)
	throws Exception
	{
		log.debug("form(): entered...");

		ManageSourceGroupForm form = new ManageSourceGroupForm();
		form.setNofly(false);

		ModelAndView mv = new ModelAndView(getPages()[0]);
		mv.addObject(FORM_NAME, form);

		Object test = request.getAttribute("groupId");

		SourceGroup group = null;
		Integer groupId = null;

		if (test != null) {
			groupId = (Integer) test;

			test = request.getAttribute("mode");

			if (test != null) {
				form.setMode((FormMode) test);
			}
			else {
				form.setMode(FormMode.ADD);
			}
		}
		else {
			form.setMode(FormMode.ADD);
		}

		switch (form.getMode()) {
			case ADD:
				group = new SourceGroup();
				break;

			case VIEW:
			case MODIFY:
			case REMOVE:
				group = sourceRepository.loadSourceGroupById(groupId);
		}

		form.setGroup(group);
		form.setOriginalMode(form.getMode());
		form.setSourceGroupComment(group.getComment());
		form.setNofly(group.isNofly());

		return mv;
	}

	@RequestMapping(value="/admin/manageSourceGroup/process" ,method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView processPage(HttpServletRequest request,
			@ModelAttribute(FORM_NAME) ManageSourceGroupForm form,
			BindingResult bindingResult, @RequestParam("page") int page,
			@RequestParam("targetPage") int targetPage)
	throws Exception
	{
		log.debug("processPage(): page = " + page + ", targetPage = " + targetPage);

		SourceGroup group = form.getGroup();
		switch (page) {
			case 0: {
				if (form.getSelectedChild() != null && form.getMode() == FormMode.REMOVE_CHILD) {
					int selectedChild = form.getSelectedChild();

					Source source = getSourceWithIdFromCollection(selectedChild, group.getSources());
					if (source != null) {
						source.setSourceGroup(null);
						group.getSources().remove(source);
						form.getRemovedSources().add(source.getId());
					}
					form.setSelectedChild(null);
					form.setSelectedSource(null);
				}

				if (form.getSelectedDeal() != null && form.getMode() == FormMode.REMOVE_ALT_CHILD) {
					int selectedDeal = form.getSelectedDeal();
					for (int x=0; x < form.getGroup().getRoyaltyFreeDeals().size(); x++) {
						if (form.getGroup().getRoyaltyFreeDeals().get(x).getId() == selectedDeal) {
							form.getGroup().getRoyaltyFreeDeals().remove(x);
						}
					}
					sourceRepository.deleteRoyaltyFreeDeal(form.getSelectedDeal());

					form.setSelectedDealChild(null);

				}

				if (form.getSelectedDeal() != null && form.getMode() == FormMode.DISABLE_ALT_CHILD) {
					int selectedDeal = form.getSelectedDeal();
					for (int x=0; x < form.getGroup().getRoyaltyFreeDeals().size(); x++) {
						if (form.getGroup().getRoyaltyFreeDeals().get(x).getId() == selectedDeal) {
							form.getGroup().getRoyaltyFreeDeals().get(x).setDisabledFlag(true);
							sourceRepository.saveDeal(form.getGroup().getRoyaltyFreeDeals().get(x));
							form.getGroup().getRoyaltyFreeDeals().remove(x);
						}
					}

					form.setSelectedDealChild(null);
				}

				if (form.getMode() == FormMode.ADD_CHILD) {
					doSearch(request);
				}

				// if new, clear the session
				if (form.getMode() == FormMode.ADD_ALT_CHILD) {
					form.clear();
				}

				if (form.getMode() == FormMode.EDIT_ALT_CHILD && null != group.getId()) {
					if (null == group.getId()) {
						// first get the id
						group = sourceRepository.saveSourceGroup(group, form.getRemovedSources());
					}
					RoyaltyFreeDeal deal = sourceRepository.loadRoyaltyFreeDealById(form.getSelectedDeal());
					form.setDealName(deal.getDescription());
					form.setSeats(deal.getSeats());
					form.setTotalPrintRun(deal.getTotalPrintRun());
					//sandhya
					form.setReadytosublicense(deal.isLicenseFlag());
					form.setSelectedDealChild(form.getSelectedDeal());
				}

				break;
			}

			// smarkoff 11/2012 - I don't see what situation in the UI ever causes this to be called
			// (page never set to 1 as far as I can tell)
			case 1: {
				if (form.getSelectedSource() == null && form.getSourceName() != null && form.getSourceName().length() > 1) {
					// name entered but not selected from the drop down
					try {
						Source ws = getSourceRepository().loadSourceByName(form.getSourceName());
						form.setSelectedSource(ws.getId());
					} catch (Exception f) {
						// do nothing;
					}
				}
				if (form.getSelectedSource() == null) {
					String backTest = request.getParameter("_target0");

					if (StringUtils.isBlank(backTest)) {
						doSearch(request);
					}
				}
				else if (form.getSelectedSource() != null) {
					Source source = null;
					try {
						source = getSourceRepository().loadSourceById(form.getSelectedSource());
						source.setNofly(form.getNofly());
					}
					catch (NoResultException ex) { }

					if (source != null) {
						group.getSources().add(source);
					}
				}
				else {
					reset(form);
				}

				break;
			}

			case 2: {
				if (form.getMode() == FormMode.REMOVE_ALT_CHILD || form.getMode() == FormMode.ADD_ALT_CHILD || form.getMode() == FormMode.EDIT_ALT_CHILD ) {
					if (form.getMode() == FormMode.ADD_ALT_CHILD && null != group.getId()) {
						if (null == group.getId()) {
							// first get the id
							group = sourceRepository.saveSourceGroup(group, form.getRemovedSources());
						}
						RoyaltyFreeDeal deal = new RoyaltyFreeDeal();
						deal.setDescription(form.getDealName());
						if (null == form.getSeats()) {
							form.setSeats(0);
						}
						if (null == form.getTotalPrintRun()) {
							form.setTotalPrintRun(0);
						}
						deal.setSeats(form.getSeats());
						deal.setTotalPrintRun(form.getTotalPrintRun());
						//sandhya
						deal.setLicenseFlag(form.isReadytosublicense());
						deal.setSourceGroupId(group.getId());
						deal = sourceRepository.saveDeal(deal);
						form.getGroup().getRoyaltyFreeDeals().add(deal);
					}
				}

				if (form.getMode() == FormMode.EDIT_ALT_CHILD && null != group.getId()) {
					for (RoyaltyFreeDeal deal : form.getGroup().getRoyaltyFreeDeals()) {
						if(deal.getId() == form.getSelectedDealChild()) {
							deal.setDescription(form.getDealName());
							deal.setSeats(form.getSeats());
							deal.setTotalPrintRun(form.getTotalPrintRun());
							//System.out.println("radio button :" + form.isReadytosublicense());
						deal.setLicenseFlag(form.isReadytosublicense());
						//System.out.println("radio button1 :" + deal.isLicenseFlag());
							sourceRepository.saveDeal(deal);
							form.setSelectedDealChild(null);
						}
					}
				}

				break;
			}
		}

		form.setSelectedSource(null);
		form.setSelectedDeal(null);

		ModelAndView mv = new ModelAndView(getPages()[targetPage]);
		mv.addObject(FORM_NAME, form);
		return mv;
	}

	private Source getSourceWithIdFromCollection(Integer sourceId, Collection<Source> sources) {
		for (Source s : sources) {
			if (s.getId().equals(sourceId)) {
				return s;
			}
		}

		return null;
	}

	@RequestMapping(value="/admin/manageSourceGroup/finish" ,method = {RequestMethod.GET, RequestMethod.POST})
	@SuppressWarnings("incomplete-switch")
	public ModelAndView processFinish(HttpServletRequest request,
			@ModelAttribute(FORM_NAME) ManageSourceGroupForm form,
			BindingResult bindingResult)
	throws Exception
	{
		log.debug("processFinish(): mode = " + form.getMode());
		ModelAndView mv = new ModelAndView(getFinishViewName());
		if (form.getMode() == FormMode.CANCEL) {
			return mv;
		}
		SourceGroup group = form.getGroup();

		if (form.getMode() == FormMode.ADD) {
			if (StringUtils.isBlank(group.getName())) {
			// invalid description.
				mv = new ModelAndView(getPages()[0]);
				mv.addObject(FORM_NAME, form);
				mv.addObject("ErrorMessage", "Name is Required");
				return mv;
			} else {
				List<SourceGroup> sourceGroups = getSourceRepository().loadAll(SourceGroup.class);
				for (SourceGroup sg: sourceGroups) {
					if (sg.getName().equals(group.getName())) {
						mv = new ModelAndView(getPages()[0]);
						mv.addObject(FORM_NAME, form);
						mv.addObject("ErrorMessage","ERROR: Source Group (" + group.getName() + ") already Exists");
						return mv;
					} else {
					}
				}
			}
		}

		if (form.getMode() == FormMode.APPLY_TO_MEMBERS) {
			group.setComment(form.getSourceGroupComment());
			if (group.getSources().size() > 0) {
				for (Source source: group.getSources()) {
					source.setComment(form.getSourceGroupComment());
					source = sourceRepository.save(source);
				}
			}
			form.setGroup(group);
			mv = new ModelAndView(getPages()[0]);
			mv.addObject(FORM_NAME, form);
			mv.addObject("ErrorMessage","Messge Applied to " + group.getSources().size() + " source(s)" );
			return mv;
		}

		group.setNofly(form.getNofly());
		group.setComment(form.getSourceGroupComment());
		log.debug("nofly:" + group.isNofly());
		if (group.getSources().size() > 0) {
			for (Source source: group.getSources()) {
				source.setNofly(group.isNofly());
			}
		}

		group =	sourceRepository.save(group);

		switch (form.getMode()) {
			case ADD:
			case MODIFY:
				group = sourceRepository.saveSourceGroup(group, form.getRemovedSources());
				form.setGroup(group);
				break;

			case REMOVE:
				sourceRepository.deleteSourceGroupIfNotUsed(group);
				break;
		}

	//	if (errors.hasErrors()) {
	//		mv.setViewName(getViewName(request, command, 0));
	//	}

		log.debug("processFinish(): Going to view: " + mv.getViewName());

		return mv;
	}

	private void reset(ManageSourceGroupForm form) {
		form.setMode(form.getOriginalMode());
		form.setSelectedChild(null);
	}

	private void doSearch(HttpServletRequest request)
	throws Exception
	{
		List<Source> sources = getSourceRepository().loadAll(Source.class);
		request.setAttribute("sources", sources);
	}

	public UserRepository getUserRepository() {
		return userRepository;
	}

	public void setUserRepository(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public SourceRepository getSourceRepository() {
		return sourceRepository;
	}

	public void setSourceRepository(SourceRepository sourceRepository) {
		this.sourceRepository = sourceRepository;
	}

	public String getFinishViewName() {
		return finishViewName;
	}

	public void setFinishViewName(String s) {
		finishViewName = s;
	}

	public String [] getPages() {
		return pages;
	}

	public void setPages(String [] pages) {
		this.pages = pages;
	}
}
