package com.wiley.permissions.web.internal.controllers.admin;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.wiley.permissions.domain.persistence.permissions.ConditionNode;
import com.wiley.permissions.repositories.ConditionRepository;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;
import com.wiley.sf.common.servlet.ServletUtil;

/**
 *
 * @author smarkoff
 */
/*@RequestMapping("/admin/testTree")*/
@RequestMapping
public class TestTreeController extends BaseAnnotatedController {
	private static final Log log = LogFactory.getLog(TestTreeController.class);

	private ConditionRepository conditionRepository;


	@RequestMapping(value="/admin/testTree/view", method = {RequestMethod.GET, RequestMethod.POST})
	public String formBackingObject(Model model)
	throws Exception
	{
		log.debug("formBackingObject(): entered...");

		return getFormView();
	}

	@RequestMapping(value="/admin/testTree/load", method = {RequestMethod.GET, RequestMethod.POST})
	public void load(HttpServletResponse response) throws IOException {
		log.debug("load(): entered...");

		List<ConditionNode> topNodes = conditionRepository.loadConditionTreeAllNodes();
		String json = ""; //ConditionNode.toJSON(topNodes);
		log.debug("json: " + json);

        response.setContentType("application/json");
        PrintWriter writer = response.getWriter();  // throws IOException
        writer.println(json);
	}

	@RequestMapping(value="/admin/testTree/submit", method = {RequestMethod.GET, RequestMethod.POST})
	public String onSubmit(HttpServletRequest request) {
		log.debug("onSubmit(): request params: " + ServletUtil.getAllParameters(request));

		return getSuccessView();
	}

	public ConditionRepository getConditionRepository() {
		return conditionRepository;
	}

	public void setConditionRepository(ConditionRepository conditionRepository) {
		this.conditionRepository = conditionRepository;
	}
}
