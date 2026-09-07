package com.wiley.permissions.domain.persistence.permissions;

import java.io.Serializable;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;

/**
 * This is just a wrapper for a List of Component for the purpose
 * of XML marshalling / unmarshalling.
 * 
 * @author smarkoff
 */
public class ComponentList
implements Serializable
{
	private static final long serialVersionUID = 1L;

    private List<Component> list;
    
    

    public ComponentList() {
    	
    }
    
	public ComponentList(List<Component> list) {
		this.list = list;
	}

	// MessageOperation "componentList"
	//@XmlElementWrapper(name = "componentList")
	@XmlElement(name = "component")
	public List<Component> getList() {
		return list;
	}

	public void setList(List<Component> list) {
		this.list = list;
	}
}