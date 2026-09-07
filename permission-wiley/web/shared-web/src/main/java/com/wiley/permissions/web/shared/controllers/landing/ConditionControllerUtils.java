package com.wiley.permissions.web.shared.controllers.landing;

import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.permissions.domain.persistence.permissions.ConditionNode;
import com.wiley.permissions.repositories.ConditionRepository;
import com.wiley.permissions.services.datatypes.IDataType;

public class ConditionControllerUtils {

	// --------------------- static data ---------------------------------
	private final static Log log = LogFactory.getLog(ConditionControllerUtils.class);

	/**
	 * Public and static to be re-used
	 */
	public static ConditionNode refreshParentNode (HttpServletRequest request, List<ConditionNode> conditions, String parent)
	throws Exception {
		populateConditions (request, conditions);
		ConditionNode cnode = getConditionNode (conditions, parent);
		if (null == cnode) {  // not expected
			throw new Exception ("Node not found [" + parent + "]");
		}
		ConditionRepository conditionRepository = new ConditionRepository();
		log.debug("refreshParentNode(): the old rollup [" + cnode.getRollupValue() + "]");
		conditionRepository.calculateRollupValueTop(cnode);
		// calculateRollupValueTop() logs new value so don't need to here
		//log.debug("refreshParentNode(): the new rollup [" + cnode.getRollupValue() + "]");
		return cnode;
	}

	/**
	 * Iterates thru the condition nodes and returns the one that matches the key or null if
	 * node not found
	 *
	 * @param nodes
	 * @param key
	 * @return ConditionNode
	 */
	public static ConditionNode getConditionNode(List<ConditionNode> nodes, String key) {
		if (null == nodes) return null;
		for (ConditionNode node : nodes) {
			// for the most part we treat node key/code as case sensitive but
			// there is little harm in using case INsensitive equals here
			if (node.getCode().equalsIgnoreCase(key)) {
				return node;
			}
			List<ConditionNode> children = node.getChildren();
			if (null != children) {
				ConditionNode child = getConditionNode(children, key);
				if (child != null) {
					return child;
				}
			}
		}
		return null;
	}

	/**
	 * @throws Exception
	 */
	private static List<ConditionNode> populateConditions (HttpServletRequest request, List<ConditionNode> conditions)
	throws Exception
	{
		// warning will go away when upgrade to Servlet 3.0 API
		@SuppressWarnings("unchecked")
		Enumeration<String> names = request.getParameterNames();

		while (names.hasMoreElements()) {
			String node = names.nextElement();
			//log.debug("populateConditions(): node = " + node);
			// ignore the parent param
			if (node.equals("parent"))
				continue;

			String[] values = request.getParameterValues(node);
			String value = values[0];
			//log.debug("populateConditions(): value = " + value);

			ConditionNode cnode = getConditionNode(conditions, node);
			if (null == cnode) {
				log.debug ("populateConditions(): Node not found [" + node + "]");
			}

			if (StringUtils.isNotBlank(cnode.getValidationClass())) {
				Class<?> clazz = Class.forName(cnode.getValidationClass());
				log.debug("populateConditions(): " + clazz.getName());
				Method method = clazz.getDeclaredMethod("getInstance");
				IDataType dataType = (IDataType) method.invoke(null, null);
				if (dataType.valid(value)) {
					value = dataType.format(value);
					log.debug("populateConditions(): valid value [" + cnode.getCode() + "]: " + value);
				}
				else {
					log.debug("populateConditions(): NOT valid value [" + cnode.getCode() + "]: " + value);
				}
			}
			cnode.setValue(value);
		}
		return conditions;
	}
}
