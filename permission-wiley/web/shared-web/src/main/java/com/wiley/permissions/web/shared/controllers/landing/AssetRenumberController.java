 package com.wiley.permissions.web.shared.controllers.landing;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.math.NumberUtils;
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
import com.wiley.permissions.domain.persistence.permissions.Component;
import com.wiley.permissions.domain.persistence.permissions.Role;
import com.wiley.permissions.domain.persistence.permissions.Usage;
import com.wiley.permissions.domain.persistence.permissions.User;
import com.wiley.permissions.repositories.AssetUseRepository;
import com.wiley.permissions.repositories.CommonWorkRepository;
import com.wiley.permissions.repositories.UserRepository;
import com.wiley.permissions.services.AssetUseIndexService;
import com.wiley.permissions.services.AssetUseService;
import com.wiley.permissions.web.shared.PermUserContext;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;

@Controller
/*@RequestMapping("/landing/renumber")*/
@RequestMapping
@SessionAttributes(AssetRenumberController.FORM_MODEL_NAME)
public class AssetRenumberController extends BaseAnnotatedController {

	private static final Log log = LogFactory.getLog(AssetRenumberController.class);
	


	private AssetUseRepository assetUseRepository;
	private AssetUseService assetUseService;
	private CommonWorkRepository commonWorkRepository;
	private AssetUseIndexService assetUseIndexService;
	private UserRepository userRepository;

	protected final static String FORM_MODEL_NAME = "assetRenumberForm";

	@Override
	protected List<String> getReferenceDataNames(HttpServletRequest request) throws Exception
	{
		List<String> output = new ArrayList<String>();

		output.add("usages");

		return output;
	}

	@RequestMapping(value = "/landing/renumber/selectcomponent", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView start(HttpServletRequest request, @RequestParam(value = "cwId") Integer cwId)
			throws Exception
	{
		log.debug("--------select component: entered...cwId=" + cwId);
		
		// pass true for includeCovers because user will not be able to renumber if
		// they don't have access to all components
		List<Component> components = getCommonWorkRepository().loadComponentList(cwId, true);

		ModelAndView mv = null;
		mv = new ModelAndView("pages.landing.asset.renumber");

		mv.addObject("components", components);
		mv.addObject("step", "gettingComponents");
		mv.addObject("nextUrl", "/sapp/landing/renumber/selectcomponent?cwId=" + cwId);
		mv.addObject("prevUrl", "/sapp/landing/renumber/selectcomponent?cwId=" + cwId);

		AssetRenumberForm form = new AssetRenumberForm();

		form.setCwId(cwId);
		mv.addObject(FORM_MODEL_NAME, form);

		return mv;
	}

	@RequestMapping(value = "/landing/renumber/selectcomponent", method = RequestMethod.POST)
	public ModelAndView selectComponent(@ModelAttribute(FORM_MODEL_NAME)  AssetRenumberForm form)
	throws Exception
	{
		log.debug("--------processing form for selectcomponent usage: entered..." );

		ModelAndView mv = null;

		mv = new ModelAndView("redirect:/sapp/landing/renumber/selectusage?component=" + form.getSelectedComponent());

		mv.addObject("nextUrl", "/sapp/landing/renumber/selectusage?component=" + form.getSelectedComponent() );
		mv.addObject("prevUrl", "/sapp/landing/renumber/selectcomponent?cwId=" + form.getCwId() );
		mv.addObject("step", "selectusage");

		mv.addObject(FORM_MODEL_NAME, form);

		return mv;
	}

	@RequestMapping(value = "/landing/renumber/selectusage", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView selectUsageStart(@RequestParam(value = "component") String componentId,
			@ModelAttribute(FORM_MODEL_NAME)  AssetRenumberForm form)
			throws Exception
	{
		log.debug("--------select component: entered...componentId=" + componentId);

		List<AssetUse> auList = getAssetUseRepository().loadAssetUseListByCWIdComponent(form.getCwId(),new Integer(componentId));

		ModelAndView mv = null;
		mv = new ModelAndView("pages.landing.asset.renumber");

		mv.addObject("step", "selectusage");
		mv.addObject("assets", auList);
		mv.addObject("nextUrl", "/sapp/landing/renumber/selectusage?component=" + componentId);
		mv.addObject("prevUrl", "/sapp/landing/renumber/selectcomponent?cwId=" + form.getCwId() );

		form.setSelectedComponent( new Integer(componentId));

		mv.addObject(FORM_MODEL_NAME, form);

		return mv;
	}

	@RequestMapping(value = "/landing/renumber/selectusage", method = RequestMethod.POST)
	public ModelAndView selectUsageForm(@ModelAttribute(FORM_MODEL_NAME)  AssetRenumberForm form,
			@RequestParam(value = "component") String componentId)
	throws Exception
	{
		log.debug("--------processing form for selectusage: entered..." );

		ModelAndView mv = null;

		mv = new ModelAndView("redirect:/sapp/landing/renumber/assets");
		mv.addObject("nextUrl", "/sapp/landing/renumber/assets");
		mv.addObject("prevUrl", "/sapp/landing/renumber/selectcomponent?cwId=" + form.getCwId() );

		mv.addObject(FORM_MODEL_NAME, form);

		return mv;
	}

	@RequestMapping(value = "/landing/renumber/assets", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView assetsStart(HttpServletRequest request, @ModelAttribute(FORM_MODEL_NAME)  AssetRenumberForm form)
			throws Exception
	{
		log.debug("--------assets: entered...cwId=" + form.getCwId() );
		
		Integer userId = PermUserContext.getCurrentUserId(request);
		User user = userRepository.lazyLoad(User.class, userId, new String[] { "group","roles" });
		Integer userGroupId = user.getGroup().getId();

		List<AssetUse> atsv = getAssetUseRepository().loadAssetUseListByCWId(form.getCwId());

		List<AssetRenumberView> filteredList = new ArrayList<AssetRenumberView>();

		for (int x=0; x< atsv.size(); x++) {
			AssetUse aux = atsv.get(x);
			// exclude asset uses that were not created by user's user group
			// unless user is administrator
			if(!user.hasRole(Role.ADMIN)) {
				if(null == aux.getUserGroup() || aux.getUserGroup().getId() != userGroupId) {
					continue;
				} 
			}
			
			Usage tmpUsage = aux.getUsage();
			if(null != tmpUsage) {
				for(int y = 0; y < form.getUsageCodes().length;y++) {
					if(form.getUsageCodes()[y].compareTo(tmpUsage.getCode()) == 0  &&
						null != aux.getComponent() &&
						form.getSelectedComponent() ==	aux.getComponent().getId()
						&& 	null != aux.getPosition() && aux.getPosition().trim().length() > 0
											) {
						filteredList.add(new AssetRenumberView(aux) );
					}
				}
			}
		}

		ModelAndView mv = null;
		mv = new ModelAndView("pages.landing.asset.renumber");
		mv.addObject("step", "assets");
		mv.addObject("prevUrl", "/sapp/landing/renumber/selectcomponent?cwId=" + form.getCwId() );

		filteredList = renumberPositions(filteredList);
		form.setRenumList(filteredList);
		mv.addObject(FORM_MODEL_NAME, form);

		return mv;
	}

	public List<AssetRenumberView> renumberPositions(List<AssetRenumberView> positions) {
		 // simple all numbers
		 //int newvalue = 0;
		 //int newPeriodValue = 0;
		 //int newDashValue = 0;

		 String[] ourAlphabet = {"a","b","c","d","e","f","g","h"
					,"i","j","k","l","m","n","o","p","q"
					,"r","s","t","u","v","w","x","y","z",
					"A","B","C","D","E","F","G","H"
					,"E","J","K","L","M","N","O","P","Q"
					,"R","S","T","U","V","W","X","Y","Z"};

		 // alpha suffix first - change on control breaks
		 int idx1 = -1;
		 String prev = "";
		 for (int x=0; x < positions.size(); x++) {
			 if (null != positions.get(x).getAlphaSuffix() && positions.get(x).getAlphaSuffix().trim().length() > 0) {
				 String wk = positions.get(x).getPrefix() + positions.get(x).getSeparator() + positions.get(x).getSuffix();
				 if (prev.compareTo(wk) != 0) {
					 // process control break
					 prev = wk;
					 idx1 = -1;
				 	}
				 	idx1++;
				 	if (idx1 > 52) idx1 = 1;
				 	positions.get(x).setAlpahSuffix(ourAlphabet[idx1]);
			 	} // end of non empty position test

			 }

		 	// numeric suffix with alpha suffix
		 	idx1 = 0;
		 	prev = "";
		 	for (int x=0;x < positions.size(); x++) {
		 		boolean hasAlphaSuffix = false;
	 			if (null != positions.get(x).getAlphaSuffix() && positions.get(x).getAlphaSuffix().trim().length() > 0 ) {
	 				hasAlphaSuffix = true;
	 			}

		 		if (null != positions.get(x).getSuffix() && positions.get(x).getSuffix().trim().length() > 0
		 				&& hasAlphaSuffix && NumberUtils.isDigits(positions.get(x).getSuffix().trim())  ) {
		 			String wk = positions.get(x).getPrefix() + positions.get(x).getSeparator() + positions.get(x).getSuffix();
		 			if (prev.compareTo(wk) != 0) {
		 				// process control break
		 				prev = wk;
		 				idx1++;
				 		}
		 					positions.get(x).setSuffix(idx1 + "");
			 		} // end of non empty position test
			 	}

		 	// numeric suffix with no alpha suffix
		 	idx1 = 0;
		 	prev = "";
		 	for (int x=0;x < positions.size(); x++) {
		 		boolean hasAlphaSuffix = false;
	 			if (null != positions.get(x).getAlphaSuffix() && positions.get(x).getAlphaSuffix().trim().length() > 0 ) {
	 				hasAlphaSuffix = true;
	 			}

		 		if (null != positions.get(x).getSuffix() && positions.get(x).getSuffix().trim().length() > 0
		 				&& !hasAlphaSuffix && NumberUtils.isDigits(positions.get(x).getSuffix().trim()) ) {
		 			String wk = positions.get(x).getPrefix() + positions.get(x).getSeparator();
		 			if (prev.compareTo(wk) != 0) {
		 				// process control break
		 				prev = wk;
		 				idx1 = 0;
				 		}
		 			  idx1 ++;
		 			  positions.get(x).setSuffix(idx1 + "");
			 		} // end of non empty position test
			 	}

		 	// numeric prefix with separators
		 	idx1 = 0;
		 	prev = "";
		 	for (int x=0; x < positions.size(); x++) {
		 		boolean hasSeparator = false;
	 			if (null != positions.get(x).getSeparator() && positions.get(x).getSeparator().trim().length() > 0 ) {
	 				hasSeparator = true;
	 			}

		 		if (null != positions.get(x).getPrefix() && positions.get(x).getPrefix().trim().length() > 0
		 				&& hasSeparator && NumberUtils.isDigits(positions.get(x).getPrefix().trim()) ) {
		 			String wk = positions.get(x).getPrefix() + positions.get(x).getSeparator();
		 			if(prev.compareTo(wk) != 0) {
		 				// process control break
		 				prev = wk;
		 				idx1++;
				 		}
		 					positions.get(x).setPrefix(idx1 + "");
			 		} // end of non empty position test

			 	}


		 	// numeric prefix with no separator
		 	idx1 = 0;
		 	prev = "";
		 	for (int x=0; x < positions.size(); x++) {
		 		boolean hasSeparator = false;
	 			if(null != positions.get(x).getSeparator() && positions.get(x).getSeparator().trim().length() > 0 ) {
	 				hasSeparator = true;
	 			}

		 		if(null != positions.get(x).getPrefix() && positions.get(x).getPrefix().trim().length() > 0
		 				&& !hasSeparator && NumberUtils.isDigits(positions.get(x).getPrefix().trim() ) ) {
		 			String wk = positions.get(x).getPrefix() + positions.get(x).getSeparator();
		 			if(prev.compareTo(wk) != 0) {
		 				// process control break
		 				prev = wk;
		 				idx1 = 0;
				 		}
		 			  idx1 ++;
		 			  positions.get(x).setPrefix(idx1 + "");
			 		} // end of non empty position test
			 	}

		 // rebuild positions from renumbered component parts
		 for (int x=0; x < positions.size(); x++) {
			 String newValue = positions.get(x).getPrefix() +
			 				   positions.get(x).getSeparator() +
			 				  positions.get(x).getSuffix() +
			 				 positions.get(x).getAlphaSuffix();
			 positions.get(x).setNewPosition(newValue);

		 }

		return positions;
	}

	@RequestMapping(value = "/landing/renumber/assets", method = RequestMethod.POST)
	public ModelAndView assetsReplace(@ModelAttribute(FORM_MODEL_NAME)  AssetRenumberForm form)
	throws Exception
	{
		log.debug("--------processing form for assets: entered..." );

		 List<AssetRenumberView> newAu = form.getRenumList();
		 for (int x = 0; x < newAu.size(); x++) {
			 AssetUse assetUse = getAssetUseRepository().loadAssetUseByIdForManageAsset(newAu.get(x).getAssetUseId());
			 assetUse.setPosition(newAu.get(x).getNewPosition());
			 getAssetUseService().saveAssetUse(assetUse);
		 }

		// ModelAndView mv = new ModelAndView("pages.landing.main");
		ModelAndView mv = new ModelAndView("redirect:/sapp/cwlanding/main?cwId=" + form.getCwId());
		return mv;
	}

	public AssetUseRepository getAssetUseRepository()
	{
		return assetUseRepository;
	}

	public void setAssetUseRepository(AssetUseRepository assetUseRepository)
	{
		this.assetUseRepository = assetUseRepository;
	}
	
	public UserRepository getUserRepository()
	{
		return userRepository;
	}

	public void setUserRepository(UserRepository userRepository)
	{
		this.userRepository = userRepository;
	}

	public CommonWorkRepository getCommonWorkRepository()
	{
		return commonWorkRepository;
	}

	public void setCommonWorkRepository(CommonWorkRepository commonWorkRepository)
	{
		this.commonWorkRepository = commonWorkRepository;
	}

	public void setAssetUseIndexService(AssetUseIndexService assetUseIndexService)
	{
		this.assetUseIndexService = assetUseIndexService;
	}

	public AssetUseIndexService getAssetUseIndexService()
	{
		return assetUseIndexService;
	}

	public AssetUseService getAssetUseService()
	{
		return assetUseService;
	}

	public void setAssetUseService(AssetUseService assetUseService)
	{
		this.assetUseService = assetUseService;
	}
}
