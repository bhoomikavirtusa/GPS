package com.wiley.permissions.web.internal.controllers.admin;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.wiley.permissions.domain.message.MasterList;
import com.wiley.permissions.domain.persistence.permissions.DataSource;
import com.wiley.permissions.domain.persistence.permissions.Product;
import com.wiley.permissions.services.ProductService;
import com.wiley.permissions.services.message.CMSMessageService;
import com.wiley.permissions.services.message.PEMessageService;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;

/**
 *
 * @author smarkoff
 */
@Controller
public class ManageMessageController
extends BaseAnnotatedController
{
	private final static Log log = LogFactory.getLog(ManageMessageController.class);

	private CMSMessageService outMsgService;
	private ProductService productService;
	private PEMessageService peMessageService;


	@RequestMapping(method = {RequestMethod.GET, RequestMethod.POST})
	public String formBackingObject(Model model)
	throws Exception
	{
        log.debug("formBackingObject(): entered...");

        model.addAttribute("manageMessageForm", new ManageMessageForm());
        model.addAttribute("masterListsToPickFrom", MasterList.ALL_WE_NEED);

        return getFormView();
	}

	@RequestMapping(method = RequestMethod.POST)
	public String onSubmit(Model model,
			@ModelAttribute("manageMessageForm") ManageMessageForm form,
			BindingResult bindingResult)
	    throws Exception
	{
		log.debug("onSubmit(): entering...");

		model.addAttribute("masterListsToPickFrom", MasterList.ALL_WE_NEED);

		String messageName = form.getMessageName();
		String externalId = form.getExternalId();

		if (StringUtils.isNotBlank(messageName)) {
			if (StringUtils.isBlank(externalId)) {
				externalId = "perm.dummy";
			}

			if (messageName.equals("GetAsset")) {
				getOutgoingMessageService().sendGetAssetMessage(externalId);
			} else if (messageName.equals("GetAssetUse")) {
				getOutgoingMessageService().sendGetAssetUseMessage(externalId);
			} else if (messageName.equals("GetSource")) {
				getOutgoingMessageService().sendGetSourceMessage(externalId);
			} else if (messageName.equals("GetComponent")) {
				getOutgoingMessageService().sendGetComponentMessage(externalId);

			} else if (messageName.equals("DeleteAsset")) {
				getOutgoingMessageService().sendDeleteAssetMessage(externalId);
			} else if (messageName.equals("DeleteAssetUse")) {
				getOutgoingMessageService().sendDeleteAssetUseMessage(externalId);
			} else if (messageName.equals("DeleteSource")) {
				getOutgoingMessageService().sendDeleteSourceMessage(externalId);
			} else if (messageName.equals("DeleteComponent")) {
				getOutgoingMessageService().sendDeleteComponentMessage(externalId);
			}

			else if (messageName.equals("Error")) {
				getOutgoingMessageService().sendErrorMessage("replyId_dummy", "0", "(dummy error message)", null);
			}

			else if (messageName.equals("RefreshProduct")) {
				String dataSourceCode = form.getDataSourceCode();
				if (dataSourceCode.equals("Auto")) {
					dataSourceCode = determineDataSourceFromExtId(externalId);
				}
				getProductService().refreshProduct(externalId, dataSourceCode, true);
			}
			else if (messageName.equals("RequestProductUpdate")) {
				final String [] externalIds = StringUtils.split(externalId);
				List<Product> products = new ArrayList<Product>(externalIds.length);
				for (String extId : externalIds) {
					Product product = new Product();
					product.setExternalId(extId);
					String dataSourceCode = form.getDataSourceCode();
					if (dataSourceCode.equals("Auto")) {
						dataSourceCode = determineDataSourceFromExtId(extId);
					}
					product.setDataSource(dataSourceCode);
					products.add(product);
				}
				peMessageService.requestProductUpdate(products, null, form.getBunchLimit());
			}
			else if (messageName.equals("RequestProductUpdateSince")) {
				Long updatedSinceLong = updatedSinceFieldsToLong(form);
				Date updatedSince = new Date(updatedSinceLong);
				peMessageService.requestProductUpdateSince(updatedSince, null);
			}
			else if (messageName.equals("RequestProductUpdateSinceLastCheck")) {
				peMessageService.requestProductUpdateSinceLastCheck();
			}
			else if (messageName.equals("RequestMasterListUpdate")) {
				if (StringUtils.isBlank(form.getMasterListName())) {
					// PE gives an error if you don't specify a particular type of masterList
					bindingResult.rejectValue("masterListName", null, "Name must be selected");
				}
				else {
					MasterList masterList = new MasterList();
					masterList.setDataSource(form.getDataSourceCode());  // may be null
					masterList.setName(form.getMasterListName());
					Long updatedSince = updatedSinceFieldsToLong(form);
					masterList.setUpdatedSince(updatedSince);
					getPEMessageService().requestMasterListUpdate(masterList, null);
				}
			}
			else if (messageName.equals("RequestMasterListUpdateSinceLastCheck")) {
				peMessageService.requestMasterListUpdateSinceLastCheck();
			}
		}

		if (bindingResult.hasErrors())  return getFormView();
        else  return getSuccessView();
	}

	private Long updatedSinceFieldsToLong(ManageMessageForm form) {
		int mins = form.getUpdatedSinceMins();
		int hours = form.getUpdatedSinceHours();
		int days = form.getUpdatedSinceDays();
		final long ONE_MIN = 60 * 1000;
		final long ONE_HOUR = 60 * ONE_MIN;
	    final long ONE_DAY = 24 * ONE_HOUR;
	    final long currentTime = System.currentTimeMillis();

	    if (hours == -1) {
	    	return 0L;  // 1970
	    }

	    return currentTime - (mins * ONE_MIN) - (hours * ONE_HOUR) - (days * ONE_DAY);
	}

	// Bruce says not to rely on this, but it's generally true
	// 001 = US, 002 = CA, 003 = AU, 004 = UK, 005 = SG, 006 = DE
	private String determineDataSourceFromExtId(String externalId) {
		String p2 = extractPart2(externalId);

		if ("001".equals(p2)) return DataSource.US.getCode();
		else if ("002".equals(p2)) return DataSource.CA.getCode();
		else if ("003".equals(p2)) return DataSource.AU.getCode();
		else if ("004".equals(p2)) return DataSource.UK.getCode();
		else if ("005".equals(p2)) return DataSource.SG.getCode();
		else if ("006".equals(p2)) return DataSource.DE.getCode();
		else {
			log.error("Could not successfully determine dataSource from externalId: " + externalId);
			return DataSource.US.getCode();
		}
	}

	private String extractPart2(String externalId) {
		// example external id format for products: CORE.003.PROD.0000006334
		int index1 = externalId.indexOf('.');
		if (index1 == -1) return "";  // not expected
		int index2 = externalId.indexOf('.', index1 + 1);
		if (index2 == -1) return "";  // not expected
		return externalId.substring(index1 + 1, index2);
	}

	public CMSMessageService getOutgoingMessageService() {
		return outMsgService;
	}

	public void setOutgoingMessageService(CMSMessageService outMsgService) {
		this.outMsgService = outMsgService;
	}

	public ProductService getProductService() {
		return productService;
	}

	public void setProductService(ProductService productService) {
		this.productService = productService;
	}

	public PEMessageService getPEMessageService() {
		return peMessageService;
	}

	public void setPEMessageService(PEMessageService peMessageService) {
		this.peMessageService = peMessageService;
	}
}
