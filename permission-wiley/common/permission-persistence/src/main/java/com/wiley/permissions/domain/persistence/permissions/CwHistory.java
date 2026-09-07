package com.wiley.permissions.domain.persistence.permissions;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PersistenceUnit;
import javax.persistence.Table;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author smarkoff
 */
@Entity
@PersistenceUnit(unitName="permissions")
@Table(name = "CW_HISTORY")
public class CwHistory
extends ExtendedAuditBase
{
	private static final long serialVersionUID = 1L;

	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(CwHistory.class);

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name="ID", nullable=false, unique=true)
	private Integer id = null;

	// Don't bother with CommonWork object - we don't need it
	@Column(name = "CW_ID", nullable = false)
	private Integer cwId;

	@Column(name = "DESCRIPTION", nullable = false, length = 1000)
	private String description = null;


	public CwHistory()
	{
		super();
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer i) {
		id = i;
	}

	public Integer getCwId() {
		return cwId;
	}

	public void setCwId(Integer cwId) {
		this.cwId = cwId;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}


	@Override
	public String toString() {
		// use getters due to the way JPA works
		return "id = " + id
		    + ", description = " + getDescription() + "\r\n";
		    // don't print CW.id because can cause lazy-load issue with ManageComponentController when debug.jspx used
		    //+ ", commonWork.id = [" + getCommonWork().getId() + "]";
	}

	/**
	 * Base on id.
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		// call getId since using JPA - not sure if necessary
		result = prime * result + ((getId() == null) ? 0 : getId().hashCode());

		return result;
	}

	/**
	 * Base on id.
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		// Important to use "instance of" due to JPA proxies
		//if (!getClass().equals(obj.getClass())) return false;
		if (!(obj instanceof CwHistory)) return false;

		// call getId since using JPA - not sure if necessary
		CwHistory other = (CwHistory) obj;
		if (other.getId() == null || getId() == null) return false;
		return other.getId().equals(getId());
	}
}
