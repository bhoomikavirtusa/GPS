package com.wiley.permissions.web.internal.controllers.admin;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import com.wiley.permissions.repositories.UserRepository;
import com.wiley.permissions.services.view.UserSubsetView;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;
import com.wiley.permissions.web.shared.controllers.BaseFormBean.FormMode;
import com.wiley.permissions.web.shared.util.DataTablesUtil;
import com.wiley.permissions.web.shared.util.TableCell;
import com.wiley.sf.common.monitor.PerformanceMonitor.PerfTimer;

/**
 *
 * @author ttidwell
 */
@Controller
@RequestMapping
public class UserMainController
extends BaseAnnotatedController
{
	private final static Log log = LogFactory.getLog(UserMainController.class);

	private final static String FORM_MODEL_NAME = "userMainForm";

	private UserRepository userRepository;
	private String manageUserURL;


	@RequestMapping(value="/admin/users/handle", method = {RequestMethod.GET,RequestMethod.POST})
	public ModelAndView handle(HttpServletRequest request,
			@ModelAttribute(FORM_MODEL_NAME) UserMainForm form,
			BindingResult bindingResult)
	throws Exception
	{
		log.debug("handle(): entered...");

		ModelAndView mv = new ModelAndView(getSuccessView());
		// When does this make a difference (test case)? - think can take next line out (smarkoff)
		mv.addAllObjects(bindingResult.getModel());

		String redirectURL = null;

		FormMode mode = form.getMode();

		if (mode == null) {
			mode = FormMode.SEARCH;
		}

		log.debug("handle(): mode: " + mode);
		log.debug("manageUserURL: " +manageUserURL);
		FormMode newMode = FormMode.ADD;

		switch (mode) {
			case VIEW:
			case SEARCH: {
				// DataTable now loads data from separate URL so don't need data in model
				//List<UserSubsetView> users = userRepository.loadAllUserSubsetList();
				//mv.addObject("users", users);

				break;
			}

			case MODIFY_CHILD: {
				redirectURL = manageUserURL;
				newMode = FormMode.MODIFY;
				break;
			}

			case REMOVE_CHILD: {  // used for enable/disable
				redirectURL = manageUserURL;
				newMode = FormMode.REMOVE;
				break;
			}

			default: throw new RuntimeException("Unexpected case.");
		}

		if (redirectURL != null) {
			if (form.getUserId() != null) {
				mv.addObject("userId", form.getUserId());
			}

			mv.addObject("mode", newMode);
			mv.setViewName("forward:" + redirectURL);

			form.setMode(FormMode.SEARCH);
		}

		return mv;
	}

/* Format that tableData() returns:
	{ "aaData": [
		    [
		      "col1",
		      "col2",
		    ],
		    […]
		]}
*/
	@RequestMapping(value="/admin/users/tableData",method = {RequestMethod.GET, RequestMethod.POST})
	public void tableData(HttpServletRequest request, HttpServletResponse response) throws IOException {
		log.debug("tableData(): entered...");
		List<UserSubsetView> views = userRepository.loadAllUserSubsetList();
		NumberFormat intFormat = NumberFormat.getIntegerInstance();
		log.debug("tableData(): loaded " + intFormat.format(views.size()) + " user records");

		PerfTimer timerBuildTableCells = userRepository.getMonitor().startTimer("UserMainController::tableData::buildTableCells");
		List<TableCell []> list = new ArrayList<TableCell []>(views.size());
		String contextPath = request.getContextPath();

		for (UserSubsetView view : views) {
			TableCell [] array = new TableCell[6];
			array[0] = getEdit(view, contextPath);
			array[1] = getEnableDisable(view, contextPath);
			int index = 2;
			array[index++] = new TableCell(view.getFirstName());
			array[index++] = new TableCell(view.getLastName());
			array[index++] = new TableCell(view.getEmail());
			array[index++] = new TableCell(view.getType().toString());
			list.add(array);
		}
		timerBuildTableCells.stopTimer();

		PerfTimer timerFilterSort = userRepository.getMonitor().startTimer("UserMainController::tableData::filterSort");
		@SuppressWarnings("unused")
		String jsonData = DataTablesUtil.sendJSONData(request, response, list, false);
		//log.debug("tableData(): jsonData: " + jsonData);
		timerFilterSort.stopTimer();
	}

	private TableCell getEdit(UserSubsetView view, String contextPath) {
		String html = "<input type=\"image\" alt=\"Edit User\" src=\"" + contextPath + "/images/buttons/edit-icon.png\""
			+ " onclick=\"this.form.elements['mode'].value='MODIFY_CHILD'; this.form.elements['userId'].value='"
			+ view.getId() + "';\" />";

		return new TableCell(null, html);
	}

	private TableCell getEnableDisable(UserSubsetView view, String contextPath) {
		String alt = view.isEnabled() ? "Disable User" : "Enable User";
		String src = contextPath + (view.isEnabled() ? "/images/buttons/add-icon.png" : "/images/buttons/delete-icon.png");
		String html = "<input type=\"image\" alt=\"" + alt + "\" src=\"" + src + "\""
			+ " onclick=\"this.form.elements['mode'].value='REMOVE_CHILD'; this.form.elements['userId'].value='"
			+ view.getId() + "';\" />";

		return new TableCell(view.isEnabled(), html);
	}


	public UserRepository getUserRepository() {
		return userRepository;
	}

	public void setUserRepository (UserRepository securityService) {
		this.userRepository = securityService;
	}

	public String getManageUserURL() {
		return manageUserURL;
	}

	public void setManageUserURL(String manageUserURL) {
		this.manageUserURL = manageUserURL;
	}
}
