package com.wiley.permissions.web.shared.controllers;

import java.beans.PropertyEditorSupport;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.MessageSource;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springmodules.validation.commons.DefaultBeanValidator;

import com.wiley.permissions.persistence.repository.JPARepository;
import com.wiley.permissions.repositories.ReferenceDataCache;
import com.wiley.permissions.repositories.ReferenceDataPropertyEditorSupport;

/**
 *
 * @author ttidwell, smarkoff
 */
public abstract class BaseAnnotatedController {

	// --------------------- static data ---------------------------------
	private final static Log log = LogFactory.getLog(BaseAnnotatedController.class);

	protected final static String TARGET_PARAM = "target";

	// --------------------- instance data -------------------------------

	private String formView;
	private String successView;

	private JPARepository repository;

	private DefaultBeanValidator validator;

	private MessageSource messageSource;

	private ReferenceDataCache referenceDataCache;

	private List<String> referenceDataNames;

	private Map<Class<?>, PropertyEditorSupport> customBinders = new HashMap<Class<?>, PropertyEditorSupport>();


	@InitBinder
	public void initBinder(WebDataBinder binder)
	throws Exception
	{
		//log.debug("initBinder(): called for objectName = " + binder.getObjectName());

		if (referenceDataCache == null) {
			log.debug("No ReferenceDataCache Set");
		}
		else {
			Map<Class<?>, ReferenceDataPropertyEditorSupport> cacheBinders = referenceDataCache.getBinders();

			if (cacheBinders != null) {
				for (Entry<Class<?>, ReferenceDataPropertyEditorSupport> entry : cacheBinders.entrySet()) {
					Class<?> type = entry.getKey();
					ReferenceDataPropertyEditorSupport tmpBinder = entry.getValue();
					binder.registerCustomEditor(type, tmpBinder);
				}
			}
		}

		if (customBinders != null) {
			for (Entry<Class<?>, PropertyEditorSupport> entry : customBinders.entrySet()) {
				Class<?> type = entry.getKey();
				PropertyEditorSupport tmpBinder = entry.getValue();
				binder.registerCustomEditor(type, tmpBinder);
			}
		}
	}

	protected List<String> getReferenceDataNames(HttpServletRequest request)
	throws Exception
	{
		return null;
	}

	@ModelAttribute
	public void referenceData(Model model, HttpServletRequest request)
	throws Exception
	{
		//log.debug("referenceData(): entered...");

		if (referenceDataNames == null) {
			referenceDataNames = getReferenceDataNames(request);
		}

		if (referenceDataNames != null) {
			for (String name : referenceDataNames) {
				Object test = referenceDataCache.get(name);

				if (test != null) {
					model.addAttribute(name, test);
				}
			}
		}
	}

	/**
	 * decides the redirect path - it can be the landingPage
	 * @return String - the view name
	 */
	protected String getTarget(String targetView, String defaultView)
	{
		if (StringUtils.isBlank(targetView)) {
			targetView = defaultView;
		}
		else {
			try {
				targetView = BeanUtils.getProperty(this, targetView);
			}
			catch (Exception e) {
				log.warn("getTarget(): not able to read the property for [" + targetView + "]");

				targetView = defaultView;
			}
		}

		log.debug("getTarget(): target selected [" + targetView + "]");

		return targetView;
	}


	public String getFormView() {
		return formView;
	}

	public void setFormView(String formViewName) {
		this.formView = formViewName;
	}

	public String getSuccessView() {
		return successView;
	}

	public void setSuccessView(String successViewName) {
		this.successView = successViewName;
	}

	public DefaultBeanValidator getValidator() {
		return validator;
	}

    public void setValidator(DefaultBeanValidator validator) {
        this.validator = validator;
    }

    public MessageSource getMessageSource() {
    	return messageSource;
    }

    public void setMessageSource(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

	public ReferenceDataCache getReferenceDataCache() {
		return referenceDataCache;
	}

	public void setReferenceDataCache(ReferenceDataCache referenceDataCache) {
		this.referenceDataCache = referenceDataCache;
	}

	public List<String> getReferenceDataNames() {
		return referenceDataNames;
	}

	public void setReferenceDataNames(List<String> referenceDataNames) {
		this.referenceDataNames = referenceDataNames;
	}

	public Map<Class<?>, PropertyEditorSupport> getCustomBinders() {
		return customBinders;
	}

	public void setCustomBinders(Map<Class<?>, PropertyEditorSupport> customBinders) {
		this.customBinders = customBinders;
	}

	public JPARepository getRepository() {
		return repository;
	}

	public void setRepository(JPARepository repository) {
		this.repository = repository;
	}
}
