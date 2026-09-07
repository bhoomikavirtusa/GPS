package com.wiley.permissions.domain.persistence.permissions;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Used to display condition trees.
 *
 * @author smarkoff
 */
@XmlAccessorType(XmlAccessType.PROPERTY)
public class ConditionNode {

	@SuppressWarnings("unused")
	private final static Log log = LogFactory.getLog(ConditionNode.class);

	private String code;  // from ConditionType
	private String description;  // from ConditionType
	private DataType dataType;  // from ConditionType
	private String value;  // from Condition (Value)
	private String rollupValue;  // from Condition (Value)
	private boolean canSee = true;  // ConditionType + [User / CW / PO / Contract]
	private boolean canEdit = true;  // ConditionType + [User / CW / PO / Contract]
	private String validationClass;

	private boolean top;
	private List<ConditionNode> children;


	public ConditionNode() {
	}

	@XmlElement (name="key")
	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	@XmlElement (name="title")
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDataTypeCode() {
		return (null != dataType) ? dataType.getCode() : null;
	}

	public boolean getHideCheckbox() {
		return true; //(null == dataType) ? true : !dataType.equals(DataType.CHECKBOX);
	}

	@XmlTransient
	public DataType getDataType() {
		return dataType;
	}

	public void setDataType(DataType dataType) {
		this.dataType = dataType;
	}

	public String getValue() {
		return value;
	}

	public boolean isValueTrue() {
		return "true".equals(value);
	}

	public void setValue(String value) {
		this.value = value;
	}

	public void setValueToTrue() {
		this.value = "true";
	}

	public void setBranchToTrue() {
		setValueToTrue();

		if (children != null) {
			for (ConditionNode node : children) {
				node.setBranchToTrue();  // recursive call
			}
		}
	}

	// smarkoff: false and null are equivalent - actually anything that is not "true" is equivalent
	// when treated as a boolean field
	// - just to be consistent let's always use null instead of "false" to mean false

	/**
	 * This method will throw a RuntimeException if not called on a top-level node.
	 * Since we don't have a reference to a possible parent node, we would not be able
	 * to do the right thing if this method was called on a child node.
	 * (The right thing to do would be to follow the logic in this method but then in
	 * addition set all parent nodes to null as well - the UI does this in JavaScript.)
	 */
	public void setBranchToNull() {
		if (!top) {
			throw new RuntimeException("setBranchToNull() should only be called on top level nodes.");
		}

		this.value = null;

		if (children != null) {
			for (ConditionNode node : children) {
				node.setBranchToNull();  // recursive call
			}
		}
	}

	public String getRollupValue() {
		return rollupValue;
	}

	public void setRollupValue(String rollupValue) {
		this.rollupValue = rollupValue;
	}

	public boolean getIsFolder() {
		return !(CollectionUtils.isEmpty(children));
	}

	public void setCanSee(boolean canSee) {
		this.canSee = canSee;
	}

	public boolean isCanSee() {
		return canSee;
	}

	public void setCanEdit(boolean canEdit) {
		this.canEdit = canEdit;
	}

	public boolean isCanEdit() {
		return canEdit;
	}

	public boolean isTop() {
		return top;
	}

	public void setTop(boolean top) {
		this.top = top;
	}

	public String getValidationClass() {
		return validationClass;
	}

	public void setValidationClass(String validationClass) {
		this.validationClass = validationClass;
	}

	public List<ConditionNode> getChildren() {
		return children;
	}

	@XmlTransient
	public List<ConditionNode> getChildrenNotNull() {
		if (children == null) {
			children = new ArrayList<ConditionNode>();
		}
		return children;
	}

	@XmlTransient
	public ConditionNode getChildWithCode(String code) {
		if (children == null) return null;
		for (ConditionNode child : children) {
			if (child.getCode().equals(code))  return child;
		}
		return null;
	}

	public void setChildren(List<ConditionNode> children) {
		this.children = children;
	}

	/**
	 * To be called with a list of top-level nodes.
	 * (Also called recursively by toJSON() below.
	 *
	 * @param nodes  Must be non-empty
	 */
/*
	public static String toJSON(List<ConditionNode> nodes) {
		ArgUtil.notEmpty(nodes, "nodes");
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		boolean haveOne = false;
		for (ConditionNode node : nodes) {
			if (haveOne) sb.append(",\r\n");
			sb.append(node.toJSON());
			haveOne = true;
		}
		sb.append("]");
		return sb.toString();
	}

	public String toJSON() {
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		String title = description;
		if (DataType.TEXT.equals(dataType)) {
			String v = (value == null) ? "" : value;
			title += " <input type='text' name='" + code + "' value='" + v + "' />";
		}
		else {
			sb.append(JsonUtil.pair("key", code)).append(", ");
		}

		if (DataType.CHECKBOX.equals(dataType) && isValueTrue()) {
			sb.append(JsonUtil.pair("select", true)).append(", ");
		}

		if (top) {
			String rv = (rollupValue == null) ? "" : rollupValue;
			title += "<span style='margin-left:30em' id='rollup_" + code + "'>" + rv + "</span>";
		}

		sb.append(JsonUtil.pair("title", title));
		if (CollectionUtils.isEmpty(children)) {
			if (DataType.TEXT.equals(dataType)) {
				sb.append(", ");
				sb.append(JsonUtil.pair("noLink", true)).append(", ");
				sb.append(JsonUtil.pair("hideCheckbox", true));
			}
		}
		else { // has children
			sb.append(", ");
			sb.append(JsonUtil.pair("isFolder", true)).append(", ");
			if (dataType == null) {
				sb.append(JsonUtil.pair("hideCheckbox", true)).append(", ");
			}
			sb.append("\"children\": ");
			sb.append(toJSON(children));
		}
		sb.append("}");
		return sb.toString();
	}
*/

	@Override
	public String toString() {
		String msg = "code = " + code + "\n"
			+ ", description = " + description + "\n"
			+ ", dataType = [" + dataType + "]\n"
			+ ", value = " + value + "\n"
			+ ", top = " + top + ", " + ", canSee = " + canSee + ", canEdit = " + canEdit + "\n";
		for (ConditionNode child : getChildrenNotNull()) {
			msg += "\n\t Child " + child.toString();
		}
		return msg;
	}

	/** Base on code only. */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((code == null) ? 0 : code.hashCode());
		return result;
	}

	/** Base on code only. */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ConditionNode other = (ConditionNode) obj;
		if (code == null) {
			if (other.code != null)
				return false;
		} else if (!code.equals(other.code))
			return false;
		return true;
	}
}
