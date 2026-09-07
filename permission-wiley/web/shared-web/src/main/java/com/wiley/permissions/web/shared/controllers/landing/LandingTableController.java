package com.wiley.permissions.web.shared.controllers.landing;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.queryparser.classic.ParseException;
import org.mule.api.lifecycle.InitialisationException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.wiley.permissions.common.transformer.ObjectToJson;
import com.wiley.permissions.common.transformer.TransformationException;
import com.wiley.permissions.domain.persistence.permissions.Privilege;
import com.wiley.permissions.domain.persistence.permissions.Role;
import com.wiley.permissions.persistence.PersistenceException;
import com.wiley.permissions.services.AssetUseService;
import com.wiley.permissions.services.LandingFilterForm;
import com.wiley.permissions.services.view.AssetUseTableRowView;
import com.wiley.permissions.services.view.JSONDataTableView;

/**
 *
 * @author smarkoff
 */
@Controller
public class LandingTableController {

	// --------------------- static data ---------------------------------

	private final static Log log = LogFactory.getLog(LandingTableController.class);

	// --------------------- instance data -------------------------------

	private AssetUseService assetUseService;


	@RequestMapping
	public void allRequests(HttpServletRequest request, HttpServletResponse response,
		@RequestParam(value = "cwId") int cwId) throws InitialisationException, TransformationException, PersistenceException, ParseException, IOException
	{
		log.debug("allRequests(): entered...");
		boolean useFilter = false;
		LandingFilterForm form = (LandingFilterForm) request.getSession().getAttribute(LandingFilterController.MODEL_FORM_NAME + cwId);
		log.debug("allRequests(): form == null: " + (form == null));
		if (form != null) {
			useFilter = form.getUseFilter();
			if (!useFilter)  form = null;
		}

		boolean includeCovers = request.isUserInRole(Role.EMPLOYEE_DEFAULT.getCode())
			|| request.isUserInRole(Privilege.COVER_ASSETS.getCode());
		List<AssetUseTableRowView> assetUseList = assetUseService.loadAssetListTableFromIndex(cwId, form, includeCovers);
		JSONDataTableView data = new JSONDataTableView();
		data.setTotalRecords(assetUseList.size());
		data.setTotalDisplayRecords(assetUseList.size());
		data.setTableRows(assetUseList);
		String output = ObjectToJson.doTransform(data, null);  // throws InitialisationException, TransformationException

		//log.debug("allRequests(): sevletParams: " + ServletUtil.getAllParameters(request));
		//log.debug("allRequests(): sending:\r\n" + output);

		response.setContentType("application/json; charset=UTF-8");
		// Don't set contentLength because String.length() is not the same as #bytes if there are double-byte chars
		//response.setContentLength(output.length());
		PrintWriter writer = response.getWriter();  // throws IOException
		writer.write(output);
	}

	public AssetUseService getAssetUseService() {
		return assetUseService;
	}

	public void setAssetUseService(AssetUseService assetUseService) {
		this.assetUseService = assetUseService;
	}
}
