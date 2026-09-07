package com.wiley.permissions.domain.message;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.permissions.common.bean.BeanProperty;
import com.wiley.permissions.common.bean.BeanUtility;
import com.wiley.permissions.common.bean.BeanUtility.ObjectType;
import com.wiley.permissions.common.dispatcher.OperationType;
import com.wiley.permissions.domain.persistence.permissions.Asset;
import com.wiley.permissions.domain.persistence.permissions.AssetUse;
import com.wiley.permissions.domain.persistence.permissions.Bundle;
import com.wiley.permissions.domain.persistence.permissions.BusinessUnit;
import com.wiley.permissions.domain.persistence.permissions.CommonWork;
import com.wiley.permissions.domain.persistence.permissions.Component;
import com.wiley.permissions.domain.persistence.permissions.ComponentCategory;
import com.wiley.permissions.domain.persistence.permissions.Contract;
import com.wiley.permissions.domain.persistence.permissions.Currency;
import com.wiley.permissions.domain.persistence.permissions.DataType;
import com.wiley.permissions.domain.persistence.permissions.DeliveryMethod;
import com.wiley.permissions.domain.persistence.permissions.Editor;
import com.wiley.permissions.domain.persistence.permissions.GeographicalLocation;
import com.wiley.permissions.domain.persistence.permissions.MediaType;
import com.wiley.permissions.domain.persistence.permissions.Medium;
import com.wiley.permissions.domain.persistence.permissions.OwnerType;
import com.wiley.permissions.domain.persistence.permissions.PaymentRequest;
import com.wiley.permissions.domain.persistence.permissions.PermissionPayer;
import com.wiley.permissions.domain.persistence.permissions.PermissionStatus;
import com.wiley.permissions.domain.persistence.permissions.Product;
import com.wiley.permissions.domain.persistence.permissions.ProductEdition;
import com.wiley.permissions.domain.persistence.permissions.ProductFamily;
import com.wiley.permissions.domain.persistence.permissions.ProductLine;
import com.wiley.permissions.domain.persistence.permissions.ProductType;
import com.wiley.permissions.domain.persistence.permissions.PublicationStatus;
import com.wiley.permissions.domain.persistence.permissions.RelationCode;
import com.wiley.permissions.domain.persistence.permissions.Role;
import com.wiley.permissions.domain.persistence.permissions.Size;
import com.wiley.permissions.domain.persistence.permissions.SubMedium;
import com.wiley.permissions.domain.persistence.permissions.SubjectCode;
import com.wiley.permissions.domain.persistence.permissions.Usage;
import com.wiley.permissions.domain.persistence.permissions.User;
import com.wiley.permissions.domain.persistence.permissions.UserToRole;

@XmlRootElement(name="message")
public class Message
implements Serializable
{

	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(Message.class);

	@XmlEnum
	public enum MessageEntity
	{
		@XmlEnumValue("PERM001")
		PERMISSIONS("PERM001"),
		@XmlEnumValue("PERMLN")
		PERMLN("PERMLN"),
		@XmlEnumValue("PERMSM")
		PERMSM("PERMSM"),
		@XmlEnumValue("PERMNM")
		PERMNM("PERMNM"),
		@XmlEnumValue("CMS")
		CMS("CMS"),
		@XmlEnumValue("CMS001")
		CMS_US("CMS001"),
		@XmlEnumValue("CORE001")
		CORE_US("CORE001"),
		@XmlEnumValue("PE001")
		PRODUCT_ENGINEERING("PE001");

		private String entity;

		private MessageEntity(String entity)
		{
			this.entity = entity;

		}

		public String getEntity()
		{
			return entity;
		}
	}

	@XmlEnum
	public enum MessageStatus
	{
		@XmlEnumValue("NEW")
		NEW("NEW"),
		@XmlEnumValue("PROCESSING")
		PROCESSING("PROCESSING"),
		@XmlEnumValue("PENDING_MORE_INFORMATION")
		PENDING_MORE_INFORMATION("PENDING_MORE_INFORMATION"),
		@XmlEnumValue("PROCESSED")
		PROCESSED("PROCESSED"),
		@XmlEnumValue("FAILED")
		FAILED("FAILED"),
		@XmlEnumValue("TERMINALLY_FAILED")
		TERMINALLY_FAILED("TERMINALLY_FAILED");

		private String code;

		private MessageStatus(String code) {
			this.code = code;
		}

		public String getCode() {
			return code;
		}
	}

	public final static MessageStatus [] MESSAGE_STATUS_ARRAY = {
		MessageStatus.NEW,
		MessageStatus.PROCESSING,
		MessageStatus.PENDING_MORE_INFORMATION,
		MessageStatus.PROCESSED,
		MessageStatus.FAILED,
		MessageStatus.TERMINALLY_FAILED
	};

	@XmlEnum
	public enum MessageProcessingDirection
	{
		@XmlEnumValue("INCOMING")
		INCOMING("INCOMING"),
		@XmlEnumValue("OUTGOING")
		OUTGOING("OUTGOING");

		// smarkoff: Only reason we have code is for scripting with Groovy (Mule)

		private final String code;

		private MessageProcessingDirection(String code) {
			this.code = code;
		}

		public String getCode() {
			return code;
		}
	}

	@XmlEnum
	public enum MessageType
	{
		@XmlEnumValue("notification")
		NOTIFICATION("notification"),
		@XmlEnumValue("request")
		REQUEST("request"),
		@XmlEnumValue("reply")
		REPLY("reply"),
		@XmlEnumValue("error")  // error msg's are also always replies
		ERROR("error");

		private final String code;

		private MessageType(String code)
		{
			this.code = code;
		}

		public String getCode()
		{
			return code;
		}
	}

	@XmlEnum
	public enum MessageFailureCode
	{
		INTERNAL_SYSTEM,
		EXTERNAL_SYSTEM
	}

	private static final long serialVersionUID = 1L;

	private static final List<Class<?>> allowableMetadata = new ArrayList<Class<?>>();

	@XmlAttribute
	private String id;

	@XmlAttribute
	private MessageEntity source;

	@XmlAttribute
	private MessageType type;

	@XmlAttribute
	private MessageProcessingDirection direction = MessageProcessingDirection.OUTGOING;

	private MessageStatus status = null;

	@XmlAttribute
	private String replyId;

	@XmlAttribute
	private long sent = System.currentTimeMillis();

	@XmlAttribute
	private String protocolVersion = "1.0";

	@XmlElement(name="target")
	@XmlElementWrapper(name="targets")
	private List<MessageEntity> targets = new ArrayList<MessageEntity>();

	@XmlElement(name="operation")
	@XmlElementWrapper(name="operations")
	private List<MessageOperation> operations = new ArrayList<MessageOperation>();

	private List<Object> metadata = new ArrayList<Object>();

	@XmlElementWrapper(name = "messageError")
	@XmlElement(name = "error")
	private List<MessageError> errorList = null;

	// TODO: This is not being used for CMS messages. Check if used for PE and
	// if not then remove - errorList is used instead for CMS. (smarkoff)
	private MessageFailureCode failureCode = MessageFailureCode.INTERNAL_SYSTEM;

	// TODO: This is not being used for CMS messages. Check if used for PE and
	// if not then remove - errorList is used instead for CMS. (smarkoff)
	// - It might be used internally along with cached_msg - which has a similar field.
	// - Or not - maybe it was but here and then decided it was better in CachedMsg
	// and just not removed here.
	private String lastFailureMessage = null;

	@XmlTransient
	private String externalXml = null;


	public Message()
	{
		setSource(MessageEntity.PERMISSIONS);
	}

	protected void gatherMetadata()
	{
		List<MessageOperation> opList = getOperations();
		boolean useMetaData = false;

		for (MessageOperation op: opList) {
			OperationType opType = op.getOperationType();
			if (opType == OperationType.ALL_NEW_PRODUCTS
				    || opType == OperationType.MASTER_LIST_UPDATE
				    || opType == OperationType.PRODUCT_UPDATE) {
				useMetaData = true;
			}
		}

		if (!useMetaData) {
			metadata = null;
			return;
		}

		if (allowableMetadata.size() == 0)
		{
			BeanProperty bp = BeanUtility.getPropertyFromClass(Message.class, "metadata");

			if (bp != null)
			{
				XmlElements elements = bp.getAnnotation(XmlElements.class);

				if (elements != null)
				{
					for (XmlElement element : elements.value())
					{
						allowableMetadata.add(element.type());
					}
				}
			}
		}

		for (MessageOperation op : operations)
		{
			for (Object item : op.getItems())
			{
				gatherMetadata(item);
			}
		}
	}

	protected boolean canBeMetadata(Object obj)
	{
		boolean output = true;

		if (!metadata.contains(obj))
		{
			for (MessageOperation op : operations)
			{
				if (op.getItems().contains(obj))
				{
					output = false;

					break;
				}
			}
		}
		else
		{
			output = false;
		}

		return output;
	}

	protected void gatherMetadata(Object obj)
	{
		List<BeanProperty> props = BeanUtility.getAllProperties(obj.getClass());

		for (BeanProperty prop : props)
		{
			if (prop.getPropertyType() == ObjectType.OBJECT && allowableMetadata.contains(prop.getType()))
			{
				try
				{
					Method readMethod = prop.getReadMethod();

					Object value = readMethod.invoke(obj, new Object[] {});

					if (value != null)
					{
						if (value instanceof Collection<?>)
						{
							Collection<?> collection = (Collection<?>) value;

							for (Object newObj : collection)
							{
								if (canBeMetadata(newObj))
								{
									metadata.add(newObj);

									gatherMetadata(newObj);
								}
							}
						}
						else if (value instanceof Map<?, ?>)
						{
							Map<?, ?> map = (Map<?, ?>) value;

							for (Object key : map.keySet())
							{
								Object newObj = map.get(key);

								if (canBeMetadata(newObj))
								{
									metadata.add(newObj);

									gatherMetadata(newObj);
								}
							}
						}
						else if (prop.getType().isArray())
						{
							Object values[] = (Object[]) value;

							for (Object newObj : values)
							{
								if (canBeMetadata(newObj))
								{
									metadata.add(newObj);

									gatherMetadata(newObj);
								}
							}
						}
						else
						{
							if (canBeMetadata(value))
							{
								metadata.add(value);

								gatherMetadata(value);
							}
						}
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		}
	}

	public MessageOperation createMessageOperation()
	{
		return new MessageOperation();
	}

	public MessageOperation createMessageOperation(OperationType type)
	{
		return new MessageOperation(type);
	}

	public void setLastFailureMessage(Throwable error) {
		if (error != null) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			error.printStackTrace(pw);
			setLastFailureMessage(sw.toString());
		}
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public MessageEntity getSource() {
		return source;
	}

	public void setSource(MessageEntity source) {
		this.source = source;
	}

	public MessageType getType() {
		return type;
	}

	public void setType(MessageType type) {
		this.type = type;
	}

	public MessageProcessingDirection getDirection() {
		return direction;
	}

	public void setDirection(MessageProcessingDirection direction) {
		this.direction = direction;
	}

	public MessageStatus getStatus() {
		return status;
	}

	public void setStatus(MessageStatus status) {
		this.status = status;
	}

	public String getReplyId() {
		return replyId;
	}

	public void setReplyId(String replyId) {
		this.replyId = replyId;
	}

	public long getSent() {
		return sent;
	}

	public void setSent(long sent) {
		this.sent = sent;
	}

	public String getProtocolVersion() {
		return protocolVersion;
	}

	public void setProtocolVersion(String protocolVersion) {
		this.protocolVersion = protocolVersion;
	}

	public List<MessageEntity> getTargets() {
		return targets;
	}

	public void setTargets(List<MessageEntity> targets) {
		this.targets = targets;
	}

	public List<MessageOperation> getOperations() {
		return operations;
	}

	/**
	 * I think we always just have one operation - later make this change
	 * since we are saving OperationType in the message cache.
	 */
	public MessageOperation getFirstOperation() {
		if (operations == null || operations.size() == 0) return null;
		return operations.get(0);
	}

	public void setOperations(List<MessageOperation> operations) {
		this.operations = operations;
	}

	@XmlElementWrapper(name="metadata")
	@XmlElements(
		{
			@XmlElement(name="asset", type=Asset.class),
			@XmlElement(name="assetUse", type=AssetUse.class),
			@XmlElement(name="bundle", type=Bundle.class),
			@XmlElement(name="businessUnit", type=BusinessUnit.class),
			@XmlElement(name="commonWork", type=CommonWork.class),
			@XmlElement(name="component", type=Component.class),
			@XmlElement(name="componentCategory", type=ComponentCategory.class),
			@XmlElement(name="copiedFrom", type=Asset.class),
			@XmlElement(name="currency", type=Currency.class),
			@XmlElement(name="dataType", type=DataType.class),
			@XmlElement(name="deliveryMethod", type=DeliveryMethod.class),
			@XmlElement(name="editor", type=Editor.class),
			@XmlElement(name="geographicalLocation", type=GeographicalLocation.class),
			@XmlElement(name="invoice", type=Contract.class),
			@XmlElement(name="mediaType", type=MediaType.class),
			@XmlElement(name="medium", type=Medium.class),
			@XmlElement(name="ownerType", type=OwnerType.class),
			@XmlElement(name="paymentRequest", type=PaymentRequest.class),
			@XmlElement(name="permissionPayer", type=PermissionPayer.class),
			@XmlElement(name="permissionStatus", type=PermissionStatus.class),
			@XmlElement(name="product", type=Product.class),
			@XmlElement(name="productEdition", type=ProductEdition.class),
			@XmlElement(name="productFamily", type=ProductFamily.class),
			@XmlElement(name="productLine", type=ProductLine.class),
			@XmlElement(name="productType", type=ProductType.class),
			@XmlElement(name="publicationStatus", type=PublicationStatus.class),
			@XmlElement(name="relationCode", type=RelationCode.class),
			@XmlElement(name="role", type=Role.class),
			@XmlElement(name="size", type=Size.class),
			@XmlElement(name="subjectCode", type=SubjectCode.class),
			@XmlElement(name="usage", type=Usage.class),
			@XmlElement(name="user", type=User.class),
			@XmlElement(name="userToProducts", type=UserToRole.class),
			@XmlElement(name="subMedium", type=SubMedium.class),
			@XmlElement(name="masterList", type=MasterList.class)
		}
	)
	public List<Object> getMetadata() {
		gatherMetadata();
		return metadata;
	}

	public void setMetadata(List<Object> metadata) {
		this.metadata = metadata;
	}

	public List<MessageError> getErrorList() {
		return errorList;
	}

	public void setErrorList(List<MessageError> errorList) {
		this.errorList = errorList;
	}

	public MessageFailureCode getFailureCode() {
		return failureCode;
	}

	public void setFailureCode(MessageFailureCode failureCode) {
		this.failureCode = failureCode;
	}

	public String getLastFailureMessage() {
		return lastFailureMessage;
	}

	public void setLastFailureMessage(String lastFailureMessage) {
		this.lastFailureMessage = lastFailureMessage;
	}

	public String getExternalXml() {
		return externalXml;
	}

	public void setExternalXml(String s) {
		externalXml = s;
	}

	@Override
	public String toString() {
		return "(printing out a subset of attributes:)\n"
		    + "id = " + id
		    + "\nsource = " + source;
	}
}
