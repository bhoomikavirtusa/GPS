package com.wiley.permissions.web.shared.controllers.asset;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.NoResultException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.wiley.permissions.domain.persistence.permissions.AssetUse;
import com.wiley.permissions.domain.persistence.permissions.CommonWork;
import com.wiley.permissions.repositories.AssetUseRepository;
import com.wiley.permissions.services.ContractService;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;

@Controller
/*@RequestMapping("/asset/usageandpermissions")*/
@RequestMapping
public class AssetDetailsUsageAndPermissionsController extends BaseAnnotatedController {

	private static final Log log = LogFactory.getLog(AssetDetailsUsageAndPermissionsController.class);

	private AssetUseRepository assetUseRepository;
	private ContractService contractService;

	@RequestMapping(value = "/asset/usageandpermissions/start", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView start(@RequestParam(value = "auId") Integer auId,
			@RequestParam(value = "readOnly", required=false) boolean readOnly)
			throws Exception
	{
		log.debug("start(): entered, auId = " + auId + ", readOnly = " + readOnly);
		AssetUse au = null;
		ModelAndView mv = null;
		if (readOnly) {
			mv = new ModelAndView("pages.asset.view.usage.and.permissions");
		} else {
			mv = new ModelAndView("pages.asset.usage.and.permissions");
		}

		try {
			au = getAssetUseRepository().loadAssetUseById(auId);
		} catch (NoResultException e) {
			log.debug("start(): no asset found with id " + auId);
		}

		if (null == au) {
			mv.addObject("permissions", null);
		} else {
			List<AssetUse> aList = getAssetUseRepository().loadAssetUseListForAssetId(au.getAsset().getId());
			List<CommonWork> projects = new ArrayList<CommonWork>();
			if (!CollectionUtils.isEmpty(aList)) {
				for (AssetUse assetUse : aList) {
					// if not in list already and different then the current one
					if (!projects.contains(assetUse.getCommonWork()) && !assetUse.getCommonWork().equals(au.getCommonWork())) {
						projects.add(assetUse.getCommonWork());
					}
				}
			}

			if (CollectionUtils.isEmpty(projects)) {
				projects = null;
			}

			mv.addObject("permissions", getContractService().loadAssetPermissionsView(auId));
			mv.addObject("projects", projects);
		}

		log.debug("start(): returning view: " + mv.getViewName());
		return mv;
	}

	public AssetUseRepository getAssetUseRepository() {
		return assetUseRepository;
	}

	public void setAssetUseRepository(AssetUseRepository assetUseRepository) {
		this.assetUseRepository = assetUseRepository;
	}

	public ContractService getContractService() {
		return contractService;
	}

	public void setContractService(ContractService contractService) {
		this.contractService = contractService;
	}
}
