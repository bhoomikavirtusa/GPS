package com.wiley.permissions.web.internal.controllers.admin;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.wiley.permissions.domain.message.pe.ProductSearchResult;
import com.wiley.permissions.services.ProductIndexService;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;
import com.wiley.sf.common.lucene.FieldInfo;
import com.wiley.sf.common.lucene.IndexInfo;

/**
 *
 * @author smarkoff
 */
@Controller
/*@RequestMapping("/admin/productIndex")*/
@RequestMapping
public class ProductIndexController extends BaseAnnotatedController {
	private static final Log log = LogFactory.getLog(ProductIndexController.class);

	private ProductIndexService productIndexService;


	@RequestMapping(value="/admin/productIndex/load" ,method = {RequestMethod.GET, RequestMethod.POST})
	public String load(@RequestParam(value="widOrIsbn", required=false) String widOrIsbn,
			@RequestParam(value="extendedInfo", required=false) Boolean extendedInfo,
			Model model)
	throws Exception
	{
		log.debug("load(): entered...");

		IndexInfo info = productIndexService.readIndexInfo();
		model.addAttribute("info", info);

		if (extendedInfo != null && extendedInfo) {
			List<FieldInfo> indexedInfo = productIndexService.readIndexedFieldInfo();
			List<FieldInfo> storedInfo = productIndexService.readStoredFieldInfo();
			model.addAttribute("indexedInfo", indexedInfo);
			model.addAttribute("storedInfo", storedInfo);
		}

		if (StringUtils.isNotBlank(widOrIsbn)) {
			ProductSearchResult result = productIndexService.searchByWIDOrISBN(widOrIsbn);
			model.addAttribute("searchWidOrIsbn", widOrIsbn);
			model.addAttribute("searchResult", result);
		}

		return getFormView();
	}

	@RequestMapping(value="/admin/productIndex/compare" ,method = {RequestMethod.GET, RequestMethod.POST})
	public String compare() throws Exception {
		log.debug("compare(): entered...");
		productIndexService.compareDBToIndex();  // throws various exceptions
		return getSuccessView();
	}

	@RequestMapping(value="/admin/productIndex/updateComputedFields" ,method = {RequestMethod.GET, RequestMethod.POST})
	public String updateComputedFields(@RequestParam(value="wid", required=false) String wid) throws Exception {
		wid = StringUtils.trimToNull(wid);
		log.debug("updateComputedFields(): wid = [" + wid + "]");
		productIndexService.updateComputedFields(wid);  // throws various exceptions
		return getSuccessView();
	}

	@RequestMapping(value="/admin/productIndex/updateProductIndexForCWIds" ,method = {RequestMethod.GET, RequestMethod.POST})
	public String updateProductIndexes(@RequestParam("cwIDs") String cwIds) throws Exception {
		log.debug("updateProductIndexes(): entered... cwIds = " + cwIds);
		if(!StringUtils.isBlank(cwIds)) {
			cwIds = cwIds.trim();
			String [] cwIdsArray = cwIds.split(",");
			for (String cwId : cwIdsArray) {
				try {
					productIndexService.updateProductsIndexForCWID(Integer.parseInt(cwId.trim()));
				}
				catch (NumberFormatException ex) {
					log.info("updateProductIndexes(): [" + cwId + "] is not an integer - skipping.");
				}
			}
		}
		return getSuccessView();
	}

	@RequestMapping(value="/admin/productIndex/checkPreAndInProduction" ,method = {RequestMethod.GET, RequestMethod.POST})
	public String checkPreAndInProduction() throws Exception {
		log.debug("checkPreAndInProduction(): entered...");
		productIndexService.checkPreAndInProduction();  // throws various exceptions
		return getSuccessView();
	}

	@RequestMapping(value="/admin/productIndex/fix" ,method = {RequestMethod.GET, RequestMethod.POST})
	public String fix() throws Exception {
		log.debug("fix(): entered...");
		productIndexService.fix();  // throws various exceptions
		return getSuccessView();
	}

	/**
	 * This method really has nothing to do with the productIndex.
	 * Just didn't want to create another controller just to serve up this jsp.
	 */
	@RequestMapping(value="/admin/productIndex/sessionInfo" ,method = {RequestMethod.GET, RequestMethod.POST})
	public String sessionInfo() {
		return "pages.admin.sessionInfo";
	}


	public ProductIndexService getProductIndexService() {
		return productIndexService;
	}

	public void setProductIndexService(ProductIndexService productIndexService) {
		this.productIndexService = productIndexService;
	}
}
