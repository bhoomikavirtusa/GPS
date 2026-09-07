package com.wiley.permissions.domain.persistence.permissions;


import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PersistenceUnit;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.wiley.permissions.common.bean.Merge;
import com.wiley.permissions.common.bean.Merge.PropertyProtection;
import com.wiley.permissions.domain.DomainObject;
import com.wiley.permissions.domain.MaterializationKey;

@Entity
@PersistenceUnit(unitName="permissions")
@Table(name = "SYSTEM_NOTIFICATION")
public class SystemNotification
extends DomainObject
{
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID", nullable = false, unique = true)
	@MaterializationKey
	private Integer id = null;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "FROM_DATE", nullable = false)
	private Date fromDate;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "TO_DATE", nullable = false)
	private Date toDate;
	
	@Column(name = "MESSAGE_TO_POST", nullable = true, length = 1000)
	@Merge(propertyProtection = PropertyProtection.OVERWRITE_IF_NOT_DEFAULT)
	private String messageToPost = null;

	public SystemNotification()
	{
		super();
	}

	public Integer getId()
	{
		return id;
	}

	public void setId(Integer id)
	{
		this.id = id;
	}


	public Date getFromDate()
	{
		return fromDate;
	}

	public void setFromDate(Date fromDate)
	{
		this.fromDate = fromDate;
	}

	public Date getToDate()
	{
		return toDate;
	}

	public void setToDate(Date toDate)
	{
		this.toDate = toDate;
	}

	public String getMessageToPost()
	{
		return messageToPost;
	}

	public void setMessageToPost(String messageToPost)
	{
		this.messageToPost = messageToPost;
	}
	
	@Override
	public String toString()
	{
		// use getters due to the way JPA works
		return super.toString() + ",\r\n" + "id = " + getId()
			+ ", fromDate = " + getFromDate()
			+ ", toDate = " + getToDate()
			+ ", messageToPost = " + getMessageToPost();
			}

	
}
