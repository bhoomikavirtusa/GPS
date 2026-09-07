package com.wiley.permissions.web.internal.controllers.admin;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.wiley.permissions.domain.persistence.message.CachedMessage;
import com.wiley.permissions.repositories.CachedMessageRepository;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;

/**
 *
 * @author smarkoff
 */
@Controller
public class ManageCachedMessageListController extends BaseAnnotatedController {
	private static final Log log = LogFactory.getLog(ManageCachedMessageListController.class);

	private static final String LIST_MODEL_NAME = "cachedMessageList";

	private CachedMessageRepository cachedMessageRepository = null;


	// handle both GET and POST (POST will be from delete link)
	@RequestMapping
	public String handle(Model model, HttpServletRequest request,
		@RequestParam(value = "deleteId", required = false) Integer deleteId)
	throws Exception
	{
		log.debug("handle(): entered...");

		if (deleteId != null) {
			log.debug("handle(): deleteId = " + deleteId);
			cachedMessageRepository.delete(deleteId);
			model.addAttribute("generalMessage", "Message with id = " + deleteId + " was deleted.");
		}

		List<CachedMessage> cachedMessageList = cachedMessageRepository.loadAllPartial();
        model.addAttribute(LIST_MODEL_NAME, cachedMessageList);

        return getFormView();
	}

	public CachedMessageRepository getCachedMessageRepository() {
		return cachedMessageRepository;
	}

	public void setCachedMessageRepository(CachedMessageRepository cachedMessageRepository) {
		this.cachedMessageRepository = cachedMessageRepository;
	}
}
