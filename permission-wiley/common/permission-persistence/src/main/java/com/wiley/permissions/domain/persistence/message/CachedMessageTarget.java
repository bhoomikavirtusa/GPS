package com.wiley.permissions.domain.persistence.message;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceUnit;
import javax.persistence.Table;

import com.wiley.permissions.domain.DomainObject;
import com.wiley.permissions.domain.message.Message.MessageEntity;

/**
 *
 * @author ttidwell
 */
@PersistenceUnit(unitName="permissions")
@Entity
@Table(name = "CACHED_MSG_TARGET")
public class CachedMessageTarget
extends DomainObject
{
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID", nullable=false, unique=true)
	private Integer id = null;

	@ManyToOne
	@JoinColumn(name = "CACHED_MSG_ID", nullable = false)
	private CachedMessage message = null;

	@Enumerated(EnumType.STRING)
	@Column(name = "TARGET", length = 32, nullable = false)
	private MessageEntity target = null;

	public CachedMessageTarget()
	{

	}

	public Integer getId()
	{
		return id;
	}

	public void setId(Integer id)
	{
		this.id = id;
	}

	public CachedMessage getMessage()
	{
		return message;
	}

	public void setMessage(CachedMessage message)
	{
		this.message = message;
	}

	public MessageEntity getTarget()
	{
		return target;
	}

	public void setTarget(MessageEntity target)
	{
		this.target = target;
	}
}
