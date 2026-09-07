package com.wiley.permissions.domain.persistence.permissions;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.PersistenceUnit;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.permissions.domain.MaterializationKey;
import com.wiley.permissions.domain.util.UniqueIdentifierGenerator;

@Entity
@PersistenceUnit(unitName="permissions")
@Table(name = "COMPONENT")
@NamedNativeQueries({
	@NamedNativeQuery(	name = "Component.checkForDuplicatesInCW",
		 		  		query = "select count(*) as count from component where cw_id=? and (name = ? or sort_order = ?)", 
		 		  		resultSetMapping="scalarCount"),

    @NamedNativeQuery(	name = "Component.getNextSortOrderInCW",
		 		  		query = "select COALESCE(max(sort_order), 0) + 1 as count from component where cw_id=?", 
		 		  		resultSetMapping="scalarCount")
})
public class Component
extends AuditBase
{
	private static final long serialVersionUID = 1L;

	private static final Log log = LogFactory.getLog(Component.class);

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name="ID", nullable=false, unique=true)
	private Integer id = null;

	@Column(name = "EXTERNAL_ID", nullable=false, length = 64)
	@MaterializationKey
	private String externalId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "CW_ID", nullable = false)
	private CommonWork commonWork = null;

	@Column(name = "NAME", nullable = false, length = 50)
	private String name = null;

	@ManyToOne
	@JoinColumn(name = "CATEGORY", nullable = false)
	private ComponentCategory category = null;

	@Column(name = "SORT_ORDER", nullable = false)
	private int sortOrder = 0;


	public Component() {

	}

	public Component(ComponentCategory category, String name) {
		this.category = category;
		this.name = name;
	}

	@Override
	@PrePersist
	public void prePersist() {
		log.debug("prePersist() called");
		super.prePersist();

		if (StringUtils.isBlank(getExternalId())) {
			setExternalId(UniqueIdentifierGenerator.getNextIdentifier("perm.component."));
		}
	}

	@XmlElement
	public Integer getId() {
		return id;
	}

	public void setId(Integer i) {
		id = i;
	}

	@XmlElement
	@XmlID
	public String getExternalId() {
		return externalId;
	}

	public void setExternalId(String s) {
		externalId = s;
	}

	@XmlElement
	public CommonWork getCommonWork() {
		return commonWork;
	}

	public void setCommonWork(CommonWork cw) {
		this.commonWork = cw;
	}

	@XmlElement
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@XmlElement(name = "category")
	public ComponentCategory getCategory() {
		return category;
	}

	public void setCategory(ComponentCategory category) {
		this.category = category;
	}

	public int getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(int sortOrder) {
		this.sortOrder = sortOrder;
	}

	@Override
	public String toString() {
		// use getters due to the way JPA works
		return "id = " + id
		    + ", externalId = " + getExternalId()
		    + ", name = " + getName() + "\r\n"
		    + ", category = [" + getCategory() + "]\r\n";
		    // don't print CW.id because can cause lazy-load issue with ManageComponentController when debug.jspx used
		    //+ ", commonWork.id = [" + getCommonWork().getId() + "]";
	}

	/**
	 * Currently base hashCode() and equals() on id but maybe should switch to externalID (XmlID).
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		// call getId() since using JPA - not sure if necessary
		result = prime * result + ((getId() == null) ? 0 : getId().hashCode());

		return result;
	}

	/**
	 * Currently base hashCode() and equals() on id but maybe should switch to externalID (XmlID).
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		// Important to use "instance of" due to JPA proxies
		//if (!getClass().equals(obj.getClass())) return false;
		if (!(obj instanceof Component)) return false;

		// call getId since using JPA - not sure if necessary
		Component other = (Component) obj;
		if (other.getId() == null || getId() == null) return false;
		return other.getId().equals(getId());
	}
}
