package com.wiley.permissions.web.internal.controllers.landing;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.wiley.permissions.services.AssetUseIndexService;
import com.wiley.permissions.services.AssetUseSearchResult;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;
import com.wiley.sf.common.lucene.FieldInfo;
import com.wiley.sf.common.lucene.IndexInfo;

/**
 *
 * @author smarkoff
 */
@Controller
/*@RequestMapping("/assetUseIndex")*/
@RequestMapping
public class AssetUseIndexController extends BaseAnnotatedController {
	private static final Log log = LogFactory.getLog(AssetUseIndexController.class);

	public final static String SESSION_CW_ID = "sessionCwId";

	private AssetUseIndexService assetUseIndexService;

	@RequestMapping(value="/assetUseIndex/load", method = {RequestMethod.GET, RequestMethod.POST})
	public String load(Model model, HttpServletRequest request,
			@RequestParam(value="searchAssetUseId", required=false) String searchAssetUseId,
			@RequestParam(value="extendedInfo", required=false) Boolean extendedInfo)
	throws Exception
	{
		log.debug("load(): entered...");

		IndexInfo info = assetUseIndexService.readIndexInfo();
		model.addAttribute("info", info);

		Integer cwId = (Integer) request.getSession().getAttribute(SESSION_CW_ID);
		model.addAttribute("cwId", cwId);

		/*if (extendedInfo != null && extendedInfo) {
			List<FieldInfo> indexedInfo = assetUseIndexService.readIndexedFieldInfo();
			List<FieldInfo> storedInfo = assetUseIndexService.readStoredFieldInfo();
			model.addAttribute("indexedInfo", indexedInfo);
			model.addAttribute("storedInfo", storedInfo);
		}

		if (StringUtils.isNotBlank(searchAssetUseId)) {
			AssetUseSearchResult result = assetUseIndexService.searchIndexByAssetUseId(Integer.valueOf(searchAssetUseId));
			model.addAttribute("searchAssetUseId", searchAssetUseId);
			model.addAttribute("searchResult", result);
		}*/

		return getFormView();
	}

	@RequestMapping(value="/assetUseIndex/build", method = RequestMethod.POST)
	public String build(@RequestParam("cwIds") String cwIds,
						@RequestParam(value="overridePrimeTime", required=false) Boolean overridePrimeTime)
	throws Exception {
		cwIds = cwIds.trim();
		log.debug("build(): entered... cwIds = " + cwIds);
		boolean overridePT = overridePrimeTime != null && overridePrimeTime;
		String [] array = cwIds.split("\\s");
		// split returns a single element array if the input is the empty string
		if (array.length == 1 && StringUtils.isBlank(array[0])) {
			assetUseIndexService.buildIndex(null, overridePT);  // null means rebuild entire index
		}
		else {
			for (String id : array) {
				try {
					assetUseIndexService.buildIndex(Integer.parseInt(id), overridePT);
				}
				catch (NumberFormatException ex) {
					log.info("build(): [" + id + "] is not an integer - skipping.");
				}
			}
		}
		return getSuccessView();
	}


	public AssetUseIndexService getAssetUseIndexService() {
		return assetUseIndexService;
	}

	public void setAssetUseIndexService(AssetUseIndexService assetUseIndexService) {
		this.assetUseIndexService = assetUseIndexService;
	}
}
