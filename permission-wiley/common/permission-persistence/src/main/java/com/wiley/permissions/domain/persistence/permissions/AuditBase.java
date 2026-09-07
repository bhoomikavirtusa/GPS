package com.wiley.permissions.domain.persistence.permissions;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.permissions.domain.DomainObject;
import com.wiley.sf.common.xml.bind.DateXmlAdapter;

@MappedSuperclass
public abstract class AuditBase
extends DomainObject
{
	private static final long serialVersionUID = 1L;

	private static final Log log = LogFactory.getLog(AuditBase.class);

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "CREATED_DATE", nullable=false, updatable=false)
	private Date createdDate = null;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "LAST_UPDATED_DATE", nullable=false)
	private Date lastUpdatedDate = null;

	public AuditBase() {
		super();
	}

	@PrePersist
	public void prePersist() {
		log.debug("prePersist() called for class " + getClass().getSimpleName());
		if (getCreatedDate() == null) {
			setCreatedDate(new Date());
		}

		if (getLastUpdatedDate() == null) {
			setLastUpdatedDate(new Date());
		}
	}

	@PreUpdate
	public void preUpdate() {
		//log.debug("preUpdate() called for class " + getClass().getSimpleName());
		setLastUpdatedDate(new Date());
	}

	public Date getCreatedDate() {
		return createdDate;
	}

	public void setCreatedDate(Date createdDate) {
		this.createdDate = createdDate;
	}

	@XmlElement
	@XmlJavaTypeAdapter(DateXmlAdapter.class)
	public Date getLastUpdatedDate() {
		return lastUpdatedDate;
	}

	public void setLastUpdatedDate(Date lastUpdatedDate) {
		this.lastUpdatedDate = lastUpdatedDate;
	}

	@Override
	public String toString() {
		// use getters due to the way JPA works
		return "createdDate = " + getCreatedDate()
		    + ", lastUpdatedDate = " + getLastUpdatedDate();
	}
}
