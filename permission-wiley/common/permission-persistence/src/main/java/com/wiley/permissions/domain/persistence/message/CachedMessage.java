package com.wiley.permissions.domain.persistence.message;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.PersistenceUnit;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.permissions.common.dispatcher.OperationType;
import com.wiley.permissions.common.transformer.ObjectToXml;
import com.wiley.permissions.common.transformer.TransformationException;
import com.wiley.permissions.domain.message.MasterList;
import com.wiley.permissions.domain.message.Message;
import com.wiley.permissions.domain.message.Message.MessageEntity;
import com.wiley.permissions.domain.message.Message.MessageProcessingDirection;
import com.wiley.permissions.domain.message.Message.MessageStatus;
import com.wiley.permissions.domain.message.Message.MessageType;
import com.wiley.permissions.domain.message.MessageOperation;
import com.wiley.permissions.domain.persistence.permissions.AuditBase;

/**
 *
 * @author ttidwell
 */
@PersistenceUnit(unitName="permissions")
@Entity
@Table(name="CACHED_MSG")
@NamedQueries
(
	{
		@NamedQuery(name="CachedMessage.findByMessageId",
			query="from CachedMessage cm where cm.messageId = :messageId"),
		@NamedQuery(name="CachedMessage.findByStatusBeforeDate",
			query="from CachedMessage cm where cm.status = :status and cm.lastDeliveryAttempt <= :date order by id"),
		@NamedQuery(name="CachedMessage.findByStatusAfterDate",
			query="from CachedMessage cm where cm.status = :status and cm.lastDeliveryAttempt > :date order by id")
	}
)
public class CachedMessage
extends AuditBase
{
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(CachedMessage.class);

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID", nullable=false, unique=true)
	private Integer id = null;

	@Column(name = "MESSAGE_ID", length = 50, nullable = false)
	private String messageId = null;

	@Enumerated(EnumType.STRING)
	@Column(name = "STATUS", length = 50, nullable = false)
	private MessageStatus status = null;

	@Enumerated(EnumType.STRING)
	@Column(name = "DIRECTION", length = 20, nullable = false)
	private Message.MessageProcessingDirection direction = null;

	@Enumerated(EnumType.STRING)
	@Column(name = "TYPE", length = 20, nullable = false)
	private MessageType type = null;

	@Enumerated(EnumType.STRING)
	@Column(name = "OPERATION_TYPE", length = 50, nullable = false)
	private OperationType operationType = null;

	@Column(name = "OPERATION_SUB_TYPE", length = 50, nullable = true)
	private String operationSubType = null;

	@Enumerated(EnumType.STRING)
	@Column(name = "SOURCE", length = 20, nullable = false)
	private MessageEntity source = null;

	@Column(name = "ITEM_COUNT", nullable = false)
	private int itemCount;

	@Column(name = "METADATA_COUNT", nullable = false)
	private int metadataCount;

	@OneToMany(cascade=CascadeType.ALL)
	@JoinColumn(name = "CACHED_MSG_ID")
	private List<CachedMessageTarget> targets = new ArrayList<CachedMessageTarget>();

	@Column(name = "PROTOCOL_VERSION", length = 20, nullable = false)
	private String protocolVersion = null;

	@Column(name = "DELIVERY_ATTEMPTS", nullable = false)
	private int deliveryAttempts = 0;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "LAST_DELIVERY_ATTEMPT", nullable = false)
	private Date lastDeliveryAttempt = new Date();

	private static int FAILURE_MESSAGE_LENGTH = 1222000;  // used below in setLastFailureMessage()
	@Lob
	@Column(name = "FAILURE_MESSAGE", length = 1222000, nullable = true)
	private String lastFailureMessage = null;

	@Lob
	@Column(name = "MESSAGE_XML", length = 50111000, nullable = false)
	private String message = null;

	@Lob
	@Column(name = "EXTERNAL_XML", length = 50111000, nullable = true)
	private String externalXml = null;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "SENT", nullable = true)
	private Date sent = null;

	public CachedMessage() {
		// need a default ctor for JPA
	}

	public CachedMessage(Message message)
	throws TransformationException
	{
		setMessageId(message.getId());
		setType(message.getType());
		MessageOperation operation = message.getFirstOperation();
		OperationType opType = operation.getOperationType();
		setOperationType(opType);
		if (opType == OperationType.MASTER_LIST_UPDATE) {
			if (getType() == MessageType.NOTIFICATION) {
				//masterList ends up in the metadata instead of operations
				List<Object> metadataList = message.getMetadata();
				Object o = metadataList.get(0);
				MasterList masterList = (MasterList) o;
				setOperationSubType(masterList.getName());
			}
			else if (getType() == MessageType.REQUEST) {
				//masterList will be in operations
				MasterList masterList = (MasterList) operation.getItems().get(0);
				setOperationSubType(masterList.getName());
			}
			else if (getType() == MessageType.REPLY) {
				//masterListObject ends up in the metadata instead of operations
				List<Object> metadataList = message.getMetadata();
				if (CollectionUtils.isNotEmpty(metadataList)) {
					Object o = metadataList.get(0);
					MasterList masterList = MasterList.masterListForObject(o);
					setOperationSubType(masterList == null ? null : masterList.getName());
				}
			}
		}

		List<Object> items = operation.getItems();
		int itemCount = (items == null) ? 0 : items.size();
		setItemCount(itemCount);

	    List<Object> metadata = message.getMetadata();
		int metadataCount = (metadata == null) ? 0 : metadata.size();
		setMetadataCount(metadataCount);

		setSource(message.getSource());
		setProtocolVersion(message.getProtocolVersion());
		if (message.getStatus() == null) {
		    setStatus(MessageStatus.NEW);
	    }
		setDirection(message.getDirection());
		setLastDeliveryAttempt(new Date());
		setSent(new Date(message.getSent()));

		for (MessageEntity target : message.getTargets()) {
			CachedMessageTarget t = new CachedMessageTarget();
			t.setTarget(target);
			t.setMessage(this);
			targets.add(t);
		}
		ObjectToXml transformer = new ObjectToXml();
		String xmlMessage = (String) transformer.transform(message);

		setMessage(xmlMessage);
		setExternalXml(message.getExternalXml());
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}

	public MessageStatus getStatus() {
		return status;
	}

	public void setStatus(MessageStatus status) {
		this.status = status;
	}

	public MessageProcessingDirection getDirection() {
		return direction;
	}

	public void setDirection(MessageProcessingDirection direction) {
		this.direction = direction;
	}

	public MessageType getType() {
		return type;
	}

	public void setType(MessageType type) {
		this.type = type;
	}

	public OperationType getOperationType() {
		return operationType;
	}

	public void setOperationType(OperationType operationType) {
		this.operationType = operationType;
	}

	public String getOperationSubType() {
		return operationSubType;
	}

	public void setOperationSubType(String operationSubType) {
		this.operationSubType = operationSubType;
	}

	public MessageEntity getSource() {
		return source;
	}

	public void setSource(MessageEntity source) {
		this.source = source;
	}

	public int getItemCount() {
		return itemCount;
	}

	public void setItemCount(int i) {
		itemCount = i;
	}

	public int getMetadataCount() {
		return metadataCount;
	}

	public void setMetadataCount(int i) {
		metadataCount = i;
	}

	public List<CachedMessageTarget> getTargets() {
		return targets;
	}

	public void setTargets(List<CachedMessageTarget> targets) {
		this.targets = targets;
	}

	public String getProtocolVersion() {
		return protocolVersion;
	}

	public void setProtocolVersion(String protocolVersion) {
		this.protocolVersion = protocolVersion;
	}

	public int getDeliveryAttempts() {
		return deliveryAttempts;
	}

	public void setDeliveryAttempts(int deliveryAttempts) {
		this.deliveryAttempts = deliveryAttempts;
	}

	public Date getLastDeliveryAttempt() {
		return lastDeliveryAttempt;
	}

	public void setLastDeliveryAttempt(Date lastDeliveryAttempt) {
		this.lastDeliveryAttempt = lastDeliveryAttempt;
	}

	public String getLastFailureMessage() {
		return lastFailureMessage;
	}

	public void setLastFailureMessage(String lastFailureMessage) {
		final int limit = FAILURE_MESSAGE_LENGTH - 50;
		if (lastFailureMessage != null && lastFailureMessage.length() > limit) {
			lastFailureMessage = lastFailureMessage.substring(0, limit)
				+ " [... truncated from " + lastFailureMessage.length() + " chars]";
		}

		this.lastFailureMessage = lastFailureMessage;
	}

	public void setLastFailureMessage(Exception error) {
		if (error != null) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			error.printStackTrace(pw);
			setLastFailureMessage(sw.toString());
		}
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getExternalXml() {
		return externalXml;
	}

	public void setExternalXml(String xml) {
		this.externalXml = xml;
	}

	public Date getSent() {
		return sent;
	}

	public void setSent(Date sent) {
		this.sent = sent;
	}
}
