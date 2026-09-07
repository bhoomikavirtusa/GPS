package com.wiley.permissions.web.internal.controllers.admin;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.wiley.permissions.services.AssetUseService;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;
import com.wiley.sf.common.monitor.PerfStatSnapshot;
import com.wiley.sf.common.monitor.PerformanceMonitor;

/**
 *
 * @author smarkoff
 */
@Controller
/*@RequestMapping("/admin/performance")*/
@RequestMapping
public class PerformanceController extends BaseAnnotatedController {
	private static final Log log = LogFactory.getLog(PerformanceController.class);

	private AssetUseService assetUseService;

	@RequestMapping(value="/admin/performance/load" ,method = {RequestMethod.GET, RequestMethod.POST})
	public String load(Model model)
	throws Exception
	{
		log.debug("load(): entered...");

		List<PerfStatSnapshot> tomcatList = PerformanceMonitor.getInstance().getStatSnapshot();
		model.addAttribute("tomcatList", tomcatList);

		List<PerfStatSnapshot> muleList = assetUseService.getPerformanceStatsFromMuleProc();
		model.addAttribute("muleList", muleList);

		return getFormView();
	}

	public void setAssetUseService(AssetUseService service) {
		assetUseService = service;
	}
}
