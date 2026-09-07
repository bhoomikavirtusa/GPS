package com.wiley.permissions.web.shared.forms;

import com.wiley.permissions.domain.persistence.permissions.CommonWork;
import com.wiley.permissions.domain.persistence.permissions.Component;
import com.wiley.permissions.web.shared.controllers.BaseFormBean;

/**
 *
 * @author smarkoff
 */
public class ComponentForm
extends BaseFormBean {

	private static final long serialVersionUID = 1L;

	private Component component = new Component();


	public ComponentForm() {
		super();
		component.setCommonWork(new CommonWork());
	}

	public void setComponent(Component component) {
		this.component = component;
	}

	public Component getComponent() {
		return component;
	}
}
