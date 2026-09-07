package com.wiley.permissions.domain.message.pe;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import com.wiley.permissions.domain.message.Message.MessageEntity;
import com.wiley.permissions.domain.message.Message.MessageType;

/**
 *
 * @author ttidwell
 */
@XmlRootElement(name = "message")
public class ProductSearchMessage
implements Serializable
{
	private static final long serialVersionUID = 1L;

	@XmlAttribute
	private String id = null;

	@XmlAttribute
	private MessageType type = MessageType.REQUEST;

	@XmlAttribute
	private MessageEntity source = MessageEntity.PERMISSIONS;

	@XmlElement(name = "system")
	@XmlElementWrapper(name = "target")
	private List<MessageEntity> targets = new ArrayList<MessageEntity>();

	@XmlAttribute
	private Long sent = null;

	@XmlAttribute
	private String protocolVersion = "1.0";

	@XmlElement(name = "request")
	private ProductSearchRequest request = null;

	@XmlElement(name = "reply")
	private ProductSearchReply reply = null;

	public ProductSearchMessage()
	{

	}

	public String getId()
	{
		return id;
	}

	public void setId(String id)
	{
		this.id = id;
	}

	public MessageType getType()
	{
		return type;
	}

	public void setType(MessageType type)
	{
		this.type = type;
	}

	public MessageEntity getSource()
	{
		return source;
	}

	public void setSource(MessageEntity source)
	{
		this.source = source;
	}

	public List<MessageEntity> getTargets()
	{
		return targets;
	}

	public void setTargets(List<MessageEntity> targets)
	{
		this.targets = targets;
	}

	public Long getSent()
	{
		return sent;
	}

	public void setSent(Long sent)
	{
		this.sent = sent;
	}

	public String getProtocolVersion()
	{
		return protocolVersion;
	}

	public void setProtocolVersion(String protocolVersion)
	{
		this.protocolVersion = protocolVersion;
	}

	public ProductSearchRequest getRequest()
	{
		return request;
	}

	public void setRequest(ProductSearchRequest request)
	{
		this.request = request;
	}

	public ProductSearchReply getReply()
	{
		return reply;
	}

	public void setReply(ProductSearchReply reply)
	{
		this.reply = reply;
	}
}
