package com.wiley.permissions.web.internal.controllers.admin;

import java.util.Date;
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

import com.wiley.permissions.domain.persistence.permissions.DataSource;
import com.wiley.permissions.repositories.AssetRepository;
import com.wiley.permissions.repositories.CommonWorkRepository;
import com.wiley.permissions.services.CommonWorkService;
import com.wiley.permissions.services.DevMiscService;
import com.wiley.permissions.services.ProductService;
import com.wiley.permissions.services.RightsLinkClient;
import com.wiley.permissions.services.SecurityService;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;

/**
 *
 * @author smarkoff
 */
@Controller
/*@RequestMapping("/admin/devMisc")*/
public class DevMiscController extends BaseAnnotatedController {
	private static final Log log = LogFactory.getLog(DevMiscController.class);

	private ProductService productService;
	private AssetRepository assetRepository;
	private SecurityService securityService;
	private CommonWorkService cwService;
	private CommonWorkRepository cwRepository;
	private DevMiscService devMiscService;

	@RequestMapping(value="/admin/devMisc/load", method = {RequestMethod.GET, RequestMethod.POST})
	public String load(Model model, HttpServletRequest request)
	throws Exception
	{
		log.debug("load(): entered...");

		Integer cwId = (Integer) request.getSession().getAttribute(AssetUseIndexController.SESSION_CW_ID);
		model.addAttribute("cwId", cwId);

		return getFormView();
	}

	@RequestMapping(value="/admin/devMisc/updateEbookSales", method = {RequestMethod.GET, RequestMethod.POST})
	public String updateEbookSales(@RequestParam(value="isbn13", required = false) String isbn13) throws Exception {
		isbn13 = StringUtils.stripToNull(isbn13);
		log.debug("updateEbookSales(): entered...isbn13 [" + isbn13 + "]");
		productService.updateEbookSales(isbn13);  // throws various exceptions
		return getSuccessView();
	}

	@RequestMapping(value="/admin/devMisc/updateAssetSeatsAndPrintRun", method = {RequestMethod.GET, RequestMethod.POST})
	public String updateAssetSeatsAndPrintRun(@RequestParam("assetId") int assetId) throws Exception {
		log.debug("updateAssetSeatsAndPrintRun(): entered...assetId [" + assetId + "]");
		if (assetId == 0) {
			List<Integer> assetIds = assetRepository.loadAllAssetIds();
			for (Integer id : assetIds) {
				try {
				assetRepository.updateTotalSeatsAndPrintRunByAssetId(id);  // REQUIRES_NEW, throws Exception
				}
				catch (Exception ex) {
					log.error("caught exception calling updateTotalSeatsAndPrintRunByAssetId() with assetId " + id + ": ", ex);
				}
			}
		}
		else {
			assetRepository.updateTotalSeatsAndPrintRunByAssetId(assetId);  // throws Exception
		}
		return getSuccessView();
	}

	@RequestMapping(value="/admin/devMisc/fixCommonWorks", method = {RequestMethod.GET, RequestMethod.POST})
	public String fixCommonWorks() throws Exception {
		log.debug("fixCommonWorks(): entered...");
		cwRepository.fixCommonWorks();
		return getSuccessView();
	}

	@RequestMapping(value="/admin/devMisc/assignUsersToGroups", method = {RequestMethod.GET, RequestMethod.POST})
	public String assignUsersToGroups() throws Exception {
		log.debug("assignGroups(): entered...");
		securityService.assignUsersToGroups();  // throws various exceptions
		return getSuccessView();
	}

	@RequestMapping(value="/admin/devMisc/rightsLinkTest", method = {RequestMethod.GET, RequestMethod.POST})
	public String rightsLinkTest() throws Exception {
		log.debug("rightsLinkTest(): entered...");
		// not the correct way to get client but this is just for testing
		String token = new RightsLinkClient().getAccessToken();
		log.debug("rightsLinkTest(): token = " + token);
		return getSuccessView();
	}
	
	//added by santhosh for PE enable/disable
	@RequestMapping(value="/admin/devMisc/updatePEStatus", method = {RequestMethod.GET, RequestMethod.POST})
	public String updatePEStatus(@RequestParam("peStatus") String peStatus) throws Exception {
		log.debug("updatePEStatus(): entered..."+peStatus);
		cwService.updatePEStatus(peStatus);
		return getSuccessView();
	}
	//end added by santhosh

	@RequestMapping(value="/admin/devMisc/transactionTest", method = {RequestMethod.GET, RequestMethod.POST})
	public String transactionTest(@RequestParam("testNumber") int testNumber) throws Exception {
		log.debug("transactionTest(): entered..., testNumber = " + testNumber);

		if (testNumber == 1) {
			// The cwService.setLastUpdate() method is normally used for a complete
			// different purpose (actually part of the real application)
			// but we are also using it here for this test.
			cwService.setLastUpdate(new Date(), "transactionTest1-part1", DataSource.US);
			log.debug("transactionTest(): completed update part1");

			cwService.setLastUpdate(new Date(), "transactionTest1-part2", DataSource.US);
			log.debug("transactionTest(): completed update part2");
		}
		else if (testNumber == 2) {
			devMiscService.testTransaction2();
		}
		else if (testNumber == 3) {
			devMiscService.testTransaction3();
		}
		else if (testNumber == 4) {
			devMiscService.testTransaction4();
		}
		else if (testNumber == 5) {
			devMiscService.testTransaction5();
		}

		return getSuccessView();
	}

	public ProductService getProductService() {
		return productService;
	}

	public void setProductService(ProductService productService) {
		this.productService = productService;
	}

	public AssetRepository getAssetRepository() {
		return assetRepository;
	}

	public void setAssetRepository(AssetRepository assetRepository) {
		this.assetRepository = assetRepository;
	}

	public SecurityService getSecurityService() {
		return securityService;
	}

	public void setSecurityService(SecurityService securityService) {
		this.securityService = securityService;
	}

	public CommonWorkService getCommonWorkService() {
		return cwService;
	}

	public void setCommonWorkService(CommonWorkService cwService) {
		this.cwService = cwService;
	}

	public CommonWorkRepository getCommonWorkRepository() {
		return cwRepository;
	}

	public void setCommonWorkRepository(CommonWorkRepository cwRepository) {
		this.cwRepository = cwRepository;
	}

	public DevMiscService getDevMiscService() {
		return devMiscService;
	}

	public void setDevMiscService(DevMiscService devMiscService) {
		this.devMiscService = devMiscService;
	}
}
