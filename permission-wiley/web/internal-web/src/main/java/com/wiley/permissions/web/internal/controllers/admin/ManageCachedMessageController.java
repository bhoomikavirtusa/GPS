package com.wiley.permissions.web.internal.controllers.admin;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.wiley.permissions.common.bean.BeanMergeException;
import com.wiley.permissions.common.bean.BeanUtility;
import com.wiley.permissions.domain.message.Message;
import com.wiley.permissions.domain.persistence.message.CachedMessage;
import com.wiley.permissions.persistence.PersistenceException;
import com.wiley.permissions.repositories.CachedMessageRepository;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;

/**
 *
 * @author smarkoff
 */
@Controller
public class ManageCachedMessageController extends BaseAnnotatedController {

	private static final Log log = LogFactory.getLog(ManageCachedMessageController.class);

	private static final String FORM_MODEL_NAME = "cachedMessage";

	private CachedMessageRepository cachedMessageRepository = null;

	@RequestMapping(method = {RequestMethod.GET, RequestMethod.POST})
	public String formBackingObject(Model model,
		@RequestParam("cachedMessageId") int cachedMessageId)
	throws PersistenceException
	{
		log.debug("formBackingObject(): entered...");

		CachedMessage msg = cachedMessageRepository.find(CachedMessage.class, cachedMessageId);
	    model.addAttribute(FORM_MODEL_NAME, msg);
	    model.addAttribute("messageStatusArray", Message.MESSAGE_STATUS_ARRAY);

        return getFormView();
	}

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.POST})
	public String onSubmit(@ModelAttribute(FORM_MODEL_NAME) CachedMessage msg,
            BindingResult bindingResult) throws PersistenceException, BeanMergeException
    {
    	log.debug("onSubmit(): entered...");
    	CachedMessage old = cachedMessageRepository.find(CachedMessage.class, msg.getId());
    	BeanUtility.merge(msg, old);
    	getCachedMessageRepository().saveRequiresNew(old);
    	return getSuccessView() + "?generalMessage=Update+Successful+for+id+" + msg.getId();
    }

	public CachedMessageRepository getCachedMessageRepository() {
		return cachedMessageRepository;
	}

	public void setCachedMessageRepository(CachedMessageRepository cachedMessageRepository) {
		this.cachedMessageRepository = cachedMessageRepository;
	}
}
