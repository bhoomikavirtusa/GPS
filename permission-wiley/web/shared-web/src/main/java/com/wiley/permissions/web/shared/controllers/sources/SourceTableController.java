package com.wiley.permissions.web.shared.controllers.sources;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.wiley.permissions.domain.persistence.permissions.Address;
import com.wiley.permissions.repositories.SourceRepository;
import com.wiley.permissions.services.view.SourceSummaryView;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;
import com.wiley.permissions.web.shared.util.DataTablesUtil;
import com.wiley.permissions.web.shared.util.TableCell;
import com.wiley.sf.common.lang.StringUtil;
import com.wiley.sf.common.monitor.PerformanceMonitor.PerfTimer;

/**
 *
 * @author smarkoff
 */
@Controller
public class SourceTableController extends BaseAnnotatedController
{
	private final static Log log = LogFactory.getLog(SourceTableController.class);


	private SourceRepository sourceRepository = null;


	@GetMapping("/sources/sources/sourceTable")
	public void handle(HttpServletRequest request, HttpServletResponse response,
			@RequestParam(value="filter", required=false) String filter,
			@RequestParam(value="sSearch_1", required=false) String search,
			@RequestParam(value="sSearch", required=false) String searchBox,
			@RequestParam("selectMode") int selectMode,
			@RequestParam("activateMode") int activateMode,
			@RequestParam("editMode") int editMode)
		throws Exception
	{
		log.debug("handle(): entered, filter [" + filter + "] search [" + search + "] searchBox [" + searchBox + "]");
		//log.debug("handle(): request params: " + ServletUtil.getAllParameters(request));
		if (StringUtils.isBlank(filter)) {
			if (StringUtils.isNotBlank(searchBox)) {
				filter = searchBox;
			} else if (null != search) {
				filter = search;
			}
		}

		List<SourceSummaryView> ssvList = sourceRepository.loadSourceSummaryListWithDisabledFlag(filter);
		NumberFormat intFormat = NumberFormat.getIntegerInstance();
		log.debug("handle(): loaded " + intFormat.format(ssvList.size()) + " source records");

		PerfTimer timerBuildTableCells = sourceRepository.getMonitor().startTimer("SourceTableController::handle::buildTableCells");
		List<TableCell []> list = new ArrayList<TableCell []>(ssvList.size());
		String contextPath = request.getContextPath();

		for (SourceSummaryView ssv : ssvList) {
			// disable delete
			// TableCell [] array = new TableCell[editMode == 1 ? 5 : 4];
			TableCell [] array = new TableCell[7];

			if (selectMode == 1) {
				array[0] = getSelect(ssv, contextPath);
			}

			if (editMode == 1) {
				array[0] = getEdit(ssv, contextPath);
				// disable delete
				// array[1] = getRemove(ssv, contextPath);
			}

			if (activateMode == 1) {
				array[0] = getActivate(ssv, contextPath);
			}

			// disable delete
			// int index = editMode == 1 ? 2 : 1;
			int index = 1;
			String name = StringUtil.removeSpecialControlChars(ssv.getName().trim());

			array[index++] = new TableCell(name);
			Address mainAddress = ssv.getMainAddress();
			array[index++] = disable(ssv, StringUtil.removeSpecialControlChars(mainAddress.getOneLineDisplay()));
			array[index++] = disable(ssv, String.valueOf(ssv.getNumberOfContacts()));
			array[index++] = disable(ssv, ssv.getNofly());
			array[index++] = disable(ssv, ssv.getMasterAgreement());
			array[index] = disable(ssv, ssv.getSourceGroup());

			list.add(array);
		}
		timerBuildTableCells.stopTimer();

		PerfTimer timerFilterSort = sourceRepository.getMonitor().startTimer("SourceTableController::handle::filterSort");
		@SuppressWarnings("unused")
		String jsonData = DataTablesUtil.sendJSONData(request, response, list, true);
		//log.debug("handle(): jsonData: " + jsonData);
		timerFilterSort.stopTimer();
	}

	private TableCell getSelect(SourceSummaryView ssv, String contextPath) {
		if (ssv.isDisabled()) {
			return new TableCell(null);
		}

		// lnagy: JavaScript method addSource is defined in manageAsset.jspx
		String html = "<a href=\"#\" onclick=\"return addSource('" + ssv.getPermissionsId() + "')\">"
			+ "<img alt=\"Select\" src=\"" + contextPath + "/images/buttons/add-icon.png\" />"
			+ "</a>";
		return new TableCell(null, html);
	}

	private TableCell getEdit(SourceSummaryView ssv, String contextPath) {
		String html = "<a href=\"" + contextPath + "/sapp/sources/sources/view?selectedSourceExtId=" + ssv.getPermissionsId()
			// cameFromSourceGroupId=0 tells ViewSourceController to remove any cameFromSourceGroupId from session
			+ "&amp;cameFromSourceGroupId=0" + "\">"
			+ "<img alt=\"Edit\" src=\"" + contextPath + "/images/buttons/edit-icon.png\" />"
			+ "</a>";

		return new TableCell(null, html);
	}

	private TableCell getRemove(SourceSummaryView ssv, String contextPath) {
		String html = "<a href=\"#\" onclick=\"return removeSource('" + ssv.getPermissionsId() + "', '" + ssv.getName() + "')\">"
			+ "<img alt=\"Remove\" src=\"" + contextPath + "/images/buttons/delete-icon.png\" />"
			+ "</a>";

		return new TableCell(null, html);
	}

	private TableCell getActivate(SourceSummaryView ssv, String contextPath) {
		String html = "<a href=\"" + contextPath + "/sapp/sources/sources/resetDisabled?selectedSourceExtId=" + ssv.getPermissionsId() + "\">"
			+ "<img alt=\"" + (ssv.isDisabled() ? "Activate" : "Deactivate") + "\" src=\"" + contextPath
			+ "/images/buttons/" + (ssv.isDisabled() ? "checkbox_off_background.png" : "checkbox_on_background.png") + "\" />"
			+ "</a>";

		return new TableCell(null, html);
	}

	private TableCell disable(SourceSummaryView ssv, String data) {
	//	if (!ssv.isDisabled() || StringUtils.isBlank(data)) {
	//		return new TableCell(data);
	//	}

		String html = data;

		if (ssv.isDisabled()) {
			html = "<span style=\"text-decoration: line-through\">" + html + "</span>";
		}

		if (ssv.getNofly().equals("Yes") ||
				(!ssv.getMasterAgreement().equals("Yes") && StringUtils.isNotBlank(ssv.getSourceGroup())
						)) {
			html = "<span style=\"background-color: yellow\">" + html + "</span>";
		}
		return new TableCell(data, html);
	}


	public SourceRepository getSourceRepository() {
    	return sourceRepository;
    }

	public void setSourceRepository(SourceRepository sourceRepository) {
    	this.sourceRepository = sourceRepository;
    }
}
