package com.wiley.permissions.repositories;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.apache.commons.collections.Closure;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.wiley.permissions.domain.persistence.permissions.Asset;
import com.wiley.permissions.domain.persistence.permissions.AssetUse;
import com.wiley.permissions.domain.persistence.permissions.AssetUseIdComparator;
import com.wiley.permissions.domain.persistence.permissions.CommonWork;
import com.wiley.permissions.domain.persistence.permissions.Condition;
import com.wiley.permissions.domain.persistence.permissions.ConditionMatchResult;
import com.wiley.permissions.domain.persistence.permissions.ConditionNode;
import com.wiley.permissions.domain.persistence.permissions.ConditionType;
import com.wiley.permissions.domain.persistence.permissions.ConditionType.ConditionCode;
import com.wiley.permissions.domain.persistence.permissions.Contract;
import com.wiley.permissions.domain.persistence.permissions.EnumUsageCondition;
import com.wiley.permissions.domain.persistence.permissions.MasterAgreementDeal;
import com.wiley.permissions.domain.persistence.permissions.Size;
import com.wiley.permissions.domain.persistence.permissions.Usage;
import com.wiley.permissions.domain.persistence.permissions.UsageConditionSize;
import com.wiley.permissions.persistence.PersistenceException;
import com.wiley.permissions.persistence.repository.JPARepository;
import com.wiley.permissions.services.CommonWorkService;
import com.wiley.permissions.services.ContractService;
import com.wiley.permissions.services.PurchaseOrderService;
import com.wiley.sf.common.lang.ArgUtil;
import com.wiley.sf.common.monitor.PerformanceMonitor.PerfTimer;

//import edu.emory.mathcs.backport.java.util.Collections;

/**
 * Contains most (but not all) of the server side logic for dealing with conditions.
 *
 * @author smarkoff
 */
public class ConditionRepository extends JPARepository
{
	private static final Log log = LogFactory.getLog(ConditionRepository.class);

	public enum ObjectType { NA, CW, CONTRACT, MA_DEAL }

	@PersistenceContext(unitName = "permissions")
	protected EntityManager entityManager;

	@Override
	public EntityManager getEntityManager() {
		return entityManager;
	}

	private UserRepository userRepository;
	private CommonWorkRepository commonWorkRepository;
	private CommonWorkService commonWorkService;
	private PurchaseOrderService purchaseOrderService;
	private ContractService contractService;


	/**
	 * @param cwId  Should be valid
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<ConditionNode> loadCwConditions(int cwId) {
		Query query = entityManager.createNativeQuery(
			"select cv.* from condition_value cv, cw_2_condition cw2c"
			+ " where cw2c.cw_id = ? and cv.id = cw2c.condition_id", Condition.class);
		query.setParameter(1, cwId);
		@SuppressWarnings("unchecked")
		List<Condition> valueList = query.getResultList();
		List<ConditionNode> nodeList = loadConditionTreeAllNodes(valueList, ObjectType.CW);
		return nodeList;
	}

	/**
	 * @param maDealId  Should be valid or null if it is a new master deal
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	@SuppressWarnings("unchecked")
	public List<ConditionNode> loadMaDealConditions(Integer maDealId) {
		List<Condition> valueList;
		if (maDealId == null) {
			valueList = new ArrayList<Condition>(0);
		}
		else {
			final String sql = "select cv.* from condition_value cv, ma_deal_2_condition mad2c"
					+ " where mad2c.ma_deal_id = ? and cv.id = mad2c.condition_id";
			Query query = entityManager.createNativeQuery(sql, Condition.class);
			query.setParameter(1, maDealId);
			valueList = query.getResultList();
		}
		List<ConditionNode> nodeList = loadConditionTreeAllNodes(valueList, ObjectType.MA_DEAL);
		return nodeList;
	}

	/**
	 * @param contractId  Should be valid or null
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<ConditionNode> loadContractConditions(Integer contractId, boolean isPermissionForm) {
		log.debug("loadContractConditions: called, contractId = " + contractId + ", isPermissionForm = " + isPermissionForm);
		PerfTimer timer = monitor.startTimer("ConditionRepository::loadContractConditions");

		List<Condition> valueList;
		if (contractId == null) {
			valueList = new ArrayList<Condition>(0);
		}
		else {
			valueList = loadContractConditionsFromDB(contractId);
		}

		List<ConditionNode> nodeList = loadConditionTreeAllNodes(valueList, ObjectType.CONTRACT, isPermissionForm);

		long time = timer.stopTimer();
		log.debug("loadContractConditions(): took " + time + "ms, valueList.size() = " + valueList.size());

		return nodeList;
	}

	private List<Condition> loadContractConditionsFromDB(int contractId) {
		String sql = "select cv.* from condition_value cv, contract_2_condition c2c"
			+ " where c2c.contract_id = ? and cv.id = c2c.condition_id ";
		Query query = entityManager.createNativeQuery(sql, Condition.class);
		query.setParameter(1, contractId);
		@SuppressWarnings("unchecked")
		List<Condition> valueList = query.getResultList();
		return valueList;
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public String loadCreditLineForContractAsset(int contractId, int assetId) {
		final String sql = "select credit_line as string from contract_2_asset where contract_id = ? and asset_base_id = ?";
		Query query = entityManager.createNativeQuery(sql, "scalarString");
		query.setParameter(1, contractId);
		query.setParameter(2, assetId);
		String result = (String) query.getSingleResult();
		return result;
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<UsageConditionSize> loadUsageSizeForContractAsset(int contractId, int assetId) {
		final String sql = "select usage_condition_code, size_code from usage_2_size where contract_id = ? and asset_base_id = ?";
		Query query = entityManager.createNativeQuery(sql);
		query.setParameter(1, contractId);
		query.setParameter(2, assetId);
		@SuppressWarnings("unchecked")
		List<Object[]> results = query.getResultList();
		List<UsageConditionSize> list = new ArrayList<UsageConditionSize>();
		for (Object [] row : results) {
			String usageConditionCode = (String) row[0];
			EnumUsageCondition usageCondition = EnumUsageCondition.getByCode(usageConditionCode);  // may be null for N/A
			String sizeCode = (String) row[1];
			Size size = Size.getByCode(sizeCode);  // should not be null since "N/A" is a value
			list.add(new UsageConditionSize(usageCondition, size));
		}
		return list;
	}

	/**
	 *
	 * @param contractId  Must be valid contractId
	 * @param map  Keys must be valid assetIds
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public void saveUsageSizeMap(int contractId, Map<Integer, List<UsageConditionSize>> map) {
		// Generally this is not needed because the method that calls this first deletes rows from contract_2_asset
		// which causes a delete cascade to usage_2_size, but best to have this to ensure this method always works.
		deleteUsageSizeForContract(contractId);

		for (Integer assetId : map.keySet()) {
			// I don't think it matters whether we modify the existing list or create a new one
			// - for now I've chosen to create a new temporary list
			List<UsageConditionSize> usageSizeList = map.get(assetId);
			usageSizeList = eliminateRedundancies(usageSizeList);

			for (UsageConditionSize pair : usageSizeList) {
				// UsageConditionSize may be null but not Size
				String usageConditionCode = pair.getUsageCondition() == null ? null : pair.getUsageCondition().getCode();
				saveUsageSize(contractId, assetId, usageConditionCode, pair.getSize().getCode());
			}
		}
	}

	/**
	 * Note there is a unit test for this method in ConditionRepositoryTest.
	 * This method is public only so it can called by the unit test.
	 *
	 * @param usageSizeList  Must be non-null
	 */
	public static List<UsageConditionSize> eliminateRedundancies(List<UsageConditionSize> usageSizeList) {
		// This list should always be at least size 1 and I expect may often be size 1
		if (usageSizeList.size() <= 1) return usageSizeList;

		log.debug("eliminateRedundancies(): input:\r\n" + StringUtils.join(usageSizeList, "\r\n"));

		// keep in mind that Usage can be null (null means N/A) but for Size there is an N/A value (cannot be null)

		// create map of usage to size where size is the largest of the size values for each usage
		Map<EnumUsageCondition, Size> map = new HashMap<EnumUsageCondition, Size>();
		for (UsageConditionSize pair : usageSizeList) {
			Size size = map.get(pair.getUsageCondition());
			if (size == null || (size != null && pair.getSize().getCompValue() > size.getCompValue())) {
				map.put(pair.getUsageCondition(), pair.getSize());
			}
		}

		usageSizeList = new ArrayList<UsageConditionSize>();
		// Any Usage/Size pair where the Usage is null can make another pair redundant
		// if the Size of that pair is smaller
		Size nullUsageSize = map.containsKey(null) ? map.get(null): null;
		for (EnumUsageCondition usage : map.keySet()) {
			Size size = map.get(usage);
			if (usage == null || nullUsageSize == null || size.getCompValue() > nullUsageSize.getCompValue()) {
				usageSizeList.add(new UsageConditionSize(usage, size));
			}
		}

		log.debug("eliminateRedundancies(): output:\r\n" + StringUtils.join(usageSizeList, "\r\n"));
		return usageSizeList;
	}

	private void deleteUsageSizeForContract(int contractId) {
		final String sql = "delete from usage_2_size where contract_id = ?";
		Query query = entityManager.createNativeQuery(sql);
		query.setParameter(1, contractId);
		query.executeUpdate();
	}

	private void saveUsageSize(int contractId, int assetBaseId, String usageConditionCode, String sizeCode) {
		log.debug("saveUsageSize(): contractId = " + contractId + ", assetBaseId = "
				+ assetBaseId + ", usageConditionCode = " + usageConditionCode + ", sizeCode = " + sizeCode);
		// usageConditionCode may be null but not sizeCode
		ArgUtil.notBlank(sizeCode, "sizeCode");
		final String sql = "insert into usage_2_size (contract_id, asset_base_id, usage_condition_code, size_code)"
				+ " values (?, ?, ?, ?)";
		Query query = entityManager.createNativeQuery(sql);
		query.setParameter(1, contractId);
		query.setParameter(2, assetBaseId);
		query.setParameter(3, usageConditionCode);
		query.setParameter(4, sizeCode);
		query.executeUpdate();
	}

	/**
	 * @param cwId  Must be valid
	 * @param nodeList  Must be non-null
	 *
	 * @return number of changes made
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public int saveCwConditions(int cwId, List<ConditionNode> nodeList) throws Exception {
		ArgUtil.notNull(nodeList, "nodeList");
		return saveCwConditionsSingleMap(cwId, buildSingleMap(nodeList));
	}

	/**
	 * @param cwId  Must be valid
	 * @param paramMap  Must be non-null
	 *
	 * @return number of changes made
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public int saveCwConditions(int cwId, Map<String, String []> paramMap) throws Exception {
		ArgUtil.notNull(paramMap, "paramMap");
		return saveCwConditionsSingleMap(cwId, buildSingleMap(paramMap));
	}

	private int saveCwConditionsSingleMap(int cwId, Map<String, String> singleMap) throws Exception {
		CommonWork cw = find(CommonWork.class, cwId);  // throws PersistenceException
		List<Condition> currentList = cw.getConditionsNotNull();
		return saveConditions(currentList, singleMap);
	}

	/**
	 * @param maDealId  Must be valid
	 * @param nodeList  Must be non-null
	 *
	 * @return number of changes made
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public int saveMaDealConditions(int maDealId, List<ConditionNode> nodeList) throws Exception {
		ArgUtil.notNull(nodeList, "nodeList");
		return saveMaDealConditionsSingleMap(maDealId, buildSingleMap(nodeList));
	}

	/**
	 * @param maDealId  Must be valid
	 * @param paramMap  Must be non-null
	 *
	 * @return number of changes made
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public int saveMaDealConditions(int maDealId, Map<String, String []> paramMap) throws Exception {
		ArgUtil.notNull(paramMap, "paramMap");
		return saveMaDealConditionsSingleMap(maDealId, buildSingleMap(paramMap));
	}

	private int saveMaDealConditionsSingleMap(int maDealId, Map<String, String> singleMap) throws Exception {
		MasterAgreementDeal maDeal = find(MasterAgreementDeal.class, maDealId);  // throws PersistenceException
		List<Condition> currentList = maDeal.getConditionsNotNull();

		return saveConditions(currentList, singleMap);
	}

	/**
	 * @param contractId  Must be valid
	 * @param nodeList  Must be non-null
	 *
	 * @return number of changes made
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public int saveContractConditions(int contractId, List<ConditionNode> nodeList) throws Exception {
		ArgUtil.notNull(nodeList, "nodeList");

		return saveContractConditionsSingleMap(contractId, buildSingleMap(nodeList));
	}

	/**
	 * @param contractId  Must be valid
	 * @param paramMap  Must be non-null
	 *
	 * @return number of changes made
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public int saveContractConditions(int contractId, Map<String, String []> paramMap) throws Exception {
		ArgUtil.notNull(paramMap, "paramMap");
		return saveContractConditionsSingleMap(contractId, buildSingleMap(paramMap));
	}

	private int saveContractConditionsSingleMap(int contractId, Map<String, String> singleMap) throws Exception {
		Contract contract = find(Contract.class, contractId);  // throws PersistenceException
		List<Condition> currentList = contract.getConditionsNotNull();
		// Important to load by Contract so that changes to the list affect contract_2_condition
		//List<Condition> currentList = loadContractConditions(contractId);
		log.debug("saveContractConditionsSingleMap(): currentList.size() = " + currentList.size()
			+ ", contractId = " + contractId);
		return saveConditions(currentList, singleMap);
	}


	/**
	 * @param nodeList  Must be non-null
	 */
	private HashMap<String, String> buildSingleMap(List<ConditionNode> nodeList) {
		HashMap<String, String> singleMap = new HashMap<String, String>();
		for (ConditionNode node : nodeList) {
			buildSingleMap(node, singleMap);
		}
		return singleMap;
	}

	private void buildSingleMap(ConditionNode node, HashMap<String, String> singleMap) {
		if (StringUtils.isNotBlank(node.getValue())) {
			if (singleMap.containsKey(node.getCode())) {
				// This should never happen
				throw new RuntimeException("More than one value received for [" + node.getCode() + "].");
			}
			singleMap.put(node.getCode(), node.getValue());
		}
		for (ConditionNode child : node.getChildrenNotNull()) {
			buildSingleMap(child, singleMap);  // recursive call
		}
	}

	private HashMap<String, String> buildSingleMap(Map<String, String []> paramMap) throws PersistenceException {
		HashMap<String, String> singleMap = new HashMap<String, String>();
		for (String key : paramMap.keySet()) {
			if (key.equals("selNodes")) {
				String [] selNodes = paramMap.get("selNodes");
				for (String s : selNodes) {
					singleMap.put(s, "true");
				}
			}
			else {
				String [] value = paramMap.get(key);
				if (value.length > 1) {
					// This should never happen
					throw new RuntimeException("More than one value received for [" + key + "].");
				}
				singleMap.put(key, value[0]);
			}
		}

		// remove activeNode since it doesn't go with logic below
		// maybe later we will use it for something
		@SuppressWarnings("unused")
		String activeNode = singleMap.remove("activeNode");

		return singleMap;
	}

	/**
	 * @return number of changes made
	 */
	private int saveConditions(Collection<Condition> currentCol, Map<String, String> singleMap) throws PersistenceException {
		// currentCol here is not just the top level ones

		List<ConditionType> typeList = loadConditionTypes();
		HashMap<String, ConditionType> typeMap = new HashMap<String, ConditionType>();
		for (ConditionType type : typeList) {
			typeMap.put(type.getCode(), type);
		}

		log.debug("saveConditions(): currentList.size() = " + currentCol.size());
		HashMap<String, Condition> currentMap = new HashMap<String, Condition>();
		for (Condition c : currentCol) {
			currentMap.put(c.getType().getCode(), c);
		}

		int numChanges = 0;
		List<Condition> addList = new ArrayList<Condition>();

		for (String key : singleMap.keySet()) {
			Condition current = currentMap.get(key);
			String newValue = StringUtils.trimToNull(singleMap.get(key));
			if (current == null) {
				if (newValue == null) continue;
				log.debug("saveConditions(): adding value of [" + key + "] as [" + newValue + "].");
				Condition c = new Condition();
				ConditionType type = typeMap.get(key);
				if (type == null) {
					// This should never happen
					throw new RuntimeException("ConditionType [" + key + "] not found in map.");
				}
				c.setType(type);
				c.setValue(newValue);
				persist(c);  // throws PersistenceException
				addList.add(c);
			}
			else {
				if (StringUtils.equals(current.getValue(), newValue)) {
					//log.debug("saveConditions(): value of [" + key + "] remains [" + newValue + "]");
				}
				else {
					log.debug("saveConditions(): changing value of [" + key + "] from [" + current.getValue() + "] to [" + newValue + "].");
					current.setValue(newValue);
					numChanges++;
				}
			}
		}

		// now null out values that were NOT in the POST data (stuff that was unchecked or text boxes that were blanked out)
		// and might as well do this before add new values (more efficient)
		for (Condition c : currentCol) {
			String newValue = singleMap.get(c.getType().getCode());
			if (c.getValue() != null && newValue == null) {
				c.setValue(null);
				numChanges++;
				log.debug("saveConditions(): nulling out (removing) value for [" + c.getType().getCode() + "]");
			}
		}

		currentCol.addAll(addList);

		if (numChanges + addList.size() > 0) {
			calculateRollupValues(currentCol, typeMap);
		}

		log.debug("saveConditions(): total number of changes, adds = " + numChanges + ", " + addList.size());
		return numChanges + addList.size();
	}

	public void calculateRollupValues(List<ConditionNode> nodeList) {
		for (ConditionNode topNode : nodeList) {
			calculateRollupValueTop(topNode);
		}
	}

	/**
	 * Expected to be a top-level node that is passed in.
	 *
	 * @param node  Must be non-null
	 */
	public void calculateRollupValueTop(ConditionNode node) {
		RollupData rollupData = calculateRollupValueInner(node);
		String rollupValue = rollupData.getValue();
		// It is now expected that rollupValue will always be non-blank since we require the user to choose something
		node.setRollupValue(rollupValue);
		log.debug("calculateRollupValueTop(): rollup for [" + node.getCode() + "] = [" + rollupValue + "]");
	}

	private void calculateRollupValues(Collection<Condition> currentCol, Map<String, ConditionType> typeMap) throws PersistenceException {
		// currentCol here is not just the top level ones

		HashMap<String, Condition> map = new HashMap<String, Condition>();
		List<Condition> subCurrentList = new ArrayList<Condition>(currentCol.size());
		for (Condition c: currentCol) {
			map.put(c.getType().getCode(), c);
			subCurrentList.add(c);
		}

		List<Condition> addList = new ArrayList<Condition>();

		List<ConditionNode> nodeList = loadConditionTreeAllNodes(subCurrentList, ObjectType.NA);
		for (ConditionNode node: nodeList) {
			String oldRollupValue = node.getRollupValue();
			calculateRollupValueTop(node);

			// If the new rollup value is blank we don't need to update unless it was set before
			if (StringUtils.isNotBlank(node.getRollupValue()) || StringUtils.isNotBlank(oldRollupValue)) {
				Condition c = map.get(node.getCode());
				if (c == null) {
					c = new Condition();
					ConditionType type = typeMap.get(node.getCode());
					if (type == null) {
						// This should never happen
						throw new RuntimeException("ConditionType [" + node.getCode() + "] not found in map.");
					}
					c.setType(type);
					c.setRollupValue(node.getRollupValue());
					persist(c);  // throws PersistenceException
					addList.add(c);
				}
				else {
					c.setRollupValue(node.getRollupValue());
				}
			}
		}

		currentCol.addAll(addList);
	}

	private class RollupData {
		private String value = null;
		private boolean selected = false;

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}

		public boolean isSelected() {
			return selected;
		}

		public void setSelected(boolean selected) {
			this.selected = selected;
		}

		@Override
		public String toString() {
			return "value = [" + value + "], selected = " + selected;
		}
	}

	/**
	 * This is a recursive method, so it is first called with a top level node
	 * and then calls itself with child nodes.
	 *
	 * @param node  Must be non-null
	 */
	private RollupData calculateRollupValueInner(ConditionNode node) {
		// The dynatree will submit top level 'medium' for example as being checked
		// if everything under it is checked even though this top node is not checkable.
		// So the value will be saved as true - so check not only that the value is not blank
		// but that the dataType is not null.
		boolean stopHere = node.getDataType() != null
			&& (
				(node.getDataType().isTextOrTextArea() && StringUtils.isNotBlank(node.getValue()))
				// Radio can be checked with not all children checked so we treat differently than Checkbox
				|| (node.getDataType().isCheckbox() && node.isValueTrue()
				|| (node.getDataType().isRadio() && node.isValueTrue() && node.getChildrenNotNull().size() == 0))
			);

		RollupData rollupData = new RollupData();
		if (stopHere) {
			if (node.getDataType().isTextOrTextArea()) {
				rollupData.setSelected(true);  // already checked above that text non-blank
				if (node.isTop()) {
					rollupData.setValue(node.getValue());
				}
				else {
					String value = node.getDescription() + "=" + node.getValue();
					rollupData.setValue(value);
				}
			}
			else {  // dataType is checkbox or radio
				rollupData.setValue(node.getDescription());
				rollupData.setSelected(true);  // already checked above that node.isValueTrue()
			}

			// don't descend tree -- children should be checked but listing them
			// would be redundant
		}
		else {
			int numSelected = 0;
			boolean hadPartial = false;
			StringBuilder regular = new StringBuilder();
			StringBuilder missing = new StringBuilder();
			for (ConditionNode child: node.getChildrenNotNull()) {
				RollupData rd = calculateRollupValueInner(child); // recursive call
				if (rd.isSelected()) numSelected++;
				else if (StringUtils.isNotBlank(rd.getValue())) {
					hadPartial = true;
				}

				if (StringUtils.isBlank(rd.getValue())) {
					if (missing.length() > 0)  missing.append(", ");
					missing.append(child.getDescription());
				}
				else {
					if (regular.length() > 0) {
						char lastChar = regular.charAt(regular.length() - 1);
						if (lastChar == ';') regular.append(' ');
						else regular.append(", ");
					}
					regular.append(rd.getValue());
				}
			}

			int numChildren = node.getChildrenNotNull().size();
			if (!hadPartial && (numSelected < numChildren) && ((2 * numSelected) > numChildren)) {
				rollupData.setValue(node.getDescription() + " except " + missing.toString() + ";");
			}
			else {
				rollupData.setValue(regular.toString());
			}
		}

		//log.debug("code [" + node.getCode() + "] - rollup: " + rollupData);
		return rollupData;
	}

	/**
	 * If objectType is CONTRACT, you should (important!) use the version below with the 3rd isPermissionForm parameter.
	 */
	public List<ConditionNode> loadConditionTreeAllNodes(List<Condition> valueList, ObjectType objectType) {
		return loadConditionTreeAllNodes(valueList, objectType, false);
	}

	// pass in cw.getConditions(), etc
	/**
	 * isPermissionForm is ignored unless ObjectType is CONTRACT
	 */
	public List<ConditionNode> loadConditionTreeAllNodes(List<Condition> valueList, ObjectType objectType, boolean isPermissionForm) {
		log.debug("loadConditionTreeAllNodes(): entered, objectType = " + objectType);
		HashMap<String, ConditionNode> nodeMap = new HashMap<String, ConditionNode>();
		List<ConditionNode> nodeList = loadConditionTreeAllNodes(nodeMap, objectType);

		for (Condition c : valueList) {
			ConditionNode node = nodeMap.get(c.getType().getCode());
			if (node == null) {
				// This should never happen
				throw new RuntimeException("node not found for [" + c.getType().getCode() + "]");
			}
			node.setValue(c.getValue());
			node.setRollupValue(c.getRollupValue());
			node.setValidationClass (c.getType().getValidationClass());
		}

		// If there are no values then
		// 1) init conditions with defaults (if appropriate for the objectType)
		// 2) that means we don't have any rollup values for the top nodes, so calculate.
		if (valueList.size() == 0) {
			if (objectType == ObjectType.CONTRACT) {
				initContractConditions(nodeMap, isPermissionForm);
			}
			else if (objectType == ObjectType.MA_DEAL) {
				initMasterAgreementConditions(nodeMap);
			}

			for (ConditionNode node : nodeList) {
				calculateRollupValueTop(node);
			}
		}

		//StringBuilder sb = new StringBuilder();
		//for (ConditionNode node : nodeList) {
		//	sb.append(node.toString());
		//	sb.append("\r\n");
		//}
		//log.debug("loadConditionTreeAllNodes(): objectType = " + objectType + ", nodeList:\r\n" + sb);

		return nodeList;
	}

	// make private later, currently called by TestTreeController
	public List<ConditionNode> loadConditionTreeAllNodes() {
		Map<String, ConditionNode> nodeMap = new HashMap<String, ConditionNode>();
		return loadConditionTreeAllNodes(nodeMap, ObjectType.CONTRACT);
	}

	/**
	 *
	 * @param nodeMap  Must be non-null (but can call empty param version of this method instead
	 */
	private List<ConditionNode> loadConditionTreeAllNodes(Map<String, ConditionNode> nodeMap, ObjectType objectType) {
		List<ConditionType> typeList = loadConditionTypes();

		ArrayList<ConditionNode> topList = new ArrayList<ConditionNode>();

		for (ConditionType type : typeList) {
			ConditionNode node = toNode(type, objectType);
			nodeMap.put(type.getCode(), node);
			if (type.getParentCode() == null) {
				topList.add(node);
				node.setTop(true);
			}
		}

		for (ConditionType type : typeList) {
			if (type.getParentCode() != null) {
				ConditionNode parent = nodeMap.get(type.getParentCode());
				ConditionNode child = nodeMap.get(type.getCode());
				parent.getChildrenNotNull().add(child);
			}
		}

		return topList;
	}

	/**
	 * Returns a map where the key is node.getCode() and the value is the ConditionNode.
	 * The children of each node are NOT put in the map.
	 *
	 * @param nodeList  Must be non-null
	 */
	public Map<String, ConditionNode> nodeListToMap(List<ConditionNode> nodeList) {
		HashMap<String, ConditionNode> map = new HashMap<String, ConditionNode>();

		for (ConditionNode node : nodeList) {
			map.put(node.getCode(), node);
		}

		return map;
	}

	/**
	 * Returns the requested node (matching the type) or null otherwise.
	 *
	 * If you are doing multiple lookups, consider converting to a map (call nodeListToMap)
	 * for efficiency.
	 */
	@SuppressWarnings("unused")
	private ConditionNode find(List<ConditionNode> nodeList, ConditionType type) {
		for (ConditionNode node : nodeList) {
			if (type.getCode().equals(node.getCode()))  return node;
		}
		return null;
	}

	public ConditionNode find (List<ConditionNode> nodeList, final ConditionType type, final boolean recursive) {
		final ConditionNode[] found = new ConditionNode[] {null};
		CollectionUtils.forAllDo(nodeList, new Closure() {

		    @Override
			public void execute(Object obj) {
				ConditionNode node = (ConditionNode)obj;
				boolean isInList = (null != obj && (node).getCode().equals(type.getCode()));
				// log.debug("evaluate(): " + node.getCode() + ": is in List :" + isInList);
				if (isInList)
					found[0] = node;
				if (recursive && !isInList && CollectionUtils.isNotEmpty(node.getChildren ())) {
					CollectionUtils.forAllDo(node.getChildren (), this);
				}
		    }

		});
		log.debug("find(): " + found[0]);
		return found[0];
	}

	public boolean exists (List<ConditionNode> nodeList, final ConditionType type, final boolean recursive) {
		ConditionNode node = find(nodeList, type, recursive);
		return node != null;
	}

	public boolean exists (List<ConditionNode> conditions, ConditionType type) {
		ConditionNode condition = find(conditions, type, true);
		log.debug("isConditionSet(): " + type + ": " + condition);
		return condition != null && StringUtils.isNotBlank(condition.getValue());
	}

	// TODO: Load this from referenceData cache instead
	private List<ConditionType> loadConditionTypes() {
		TypedQuery<ConditionType> query = entityManager.createQuery(
			    "from ConditionType ct order by sortOrder", ConditionType.class);
		List<ConditionType> typeList = query.getResultList();
		return typeList;
	}

	private Map<String, ConditionType> loadConditionTypeMap() {
		List<ConditionType> list = loadConditionTypes();
		HashMap<String, ConditionType> map = new HashMap<String, ConditionType>();

		for (ConditionType type : list) {
			map.put(type.getCode(), type);
		}

		return map;
	}

	private ConditionType getRoot(ConditionType type, Map<String, ConditionType> typeMap) {
		if (type.getParentCode() == null)  return type;

		ConditionType parent = typeMap.get(type.getParentCode());
		if (parent == null) {
			throw new RuntimeException("This should never happen.");
		}
		else {
			return getRoot(parent, typeMap);  // recursive call
		}
	}

	private ConditionNode toNode(ConditionType type, ObjectType objectType) {
		ConditionNode node = type.toConditionNode();

		if (objectType == ObjectType.CONTRACT) {
			node.setCanSee(type.getCanSeeInContract());
			node.setCanEdit(type.getCanEditInContract());
		}
		else if (objectType == ObjectType.CW) {
			node.setCanSee(type.getCanSeeInCW());
			node.setCanEdit(type.getCanEditInCW());
		}
		else if (objectType == ObjectType.MA_DEAL) {
			node.setCanSee(type.getCanSeeInMadeal());
			node.setCanEdit(type.getCanEditInMadeal());
		}

		return node;
	}

	/**
	 *
	 * @param cwConditions  Must be non-null
	 * @param ccMap  Must be non-null
	 */
	private ConditionMatchResult matches(List<Condition> cwConditions, Map<ConditionType, Condition> ccMap)
	{
		// cwConditions here is a list of all conditions at all levels (not a tree, ie not a list of just the root conditions)

		Map<String, ConditionType> typeMap = loadConditionTypeMap();
		StringBuilder explanation = new StringBuilder();
		// rootSetWithFailure used for logic to report only one match failure per major tree branch
		HashSet<ConditionType> rootSetWithFailure = new HashSet<ConditionType>();

		for (Condition cwCondition : cwConditions) {
			log.debug("matches(): cwCondition: " + cwCondition);

			if (StringUtils.isBlank(cwCondition.getValue())) {
				continue;
			}

			Condition contractCondition = ccMap.get(cwCondition.getType());

			ConditionType rootType = getRoot(cwCondition.getType(), typeMap);
			//Condition rootContractCondition = ccMap.get(rootType);
			// root will never be null anymore (used to be possible)

			String detailEx = matches(cwCondition, contractCondition, ccMap);

			// Don't report no match if root since this is not useful information
			if (detailEx != null && !rootType.equals(cwCondition.getType())
					&& !rootSetWithFailure.contains(rootType)) {
				if (explanation.length() > 0)  explanation.append(",\r\n");
				explanation.append(cwCondition.getType().getDescription());
				if (StringUtils.isBlank(cwCondition.getType().getDescription())) {
					// print_run is unique in having an empty description
					// Use the parent description
					explanation.append(ConditionType.PRINT_RUN_LIMIT.getDescription());
				}
				explanation.append(" condition not satisfied - ").append(detailEx);
				rootSetWithFailure.add(rootType);
			}
		}

		if (explanation.length() > 0) {
			return new ConditionMatchResult(explanation.toString());
		}
		else return new ConditionMatchResult();  // true match
	}

	public Map<ConditionType, Condition> conditionListToMap(List<Condition> list) {
		HashMap<ConditionType, Condition> map = new HashMap<ConditionType, Condition>();
		for (Condition c : list) {
			// error check for 2 conditions of same type - should never happen
			Condition first = map.get(c.getType());
			if (first != null) {
				throw new RuntimeException("conditionListToMap: There were 2 conditions of the same type: "
					+ c.getType().getCode());
			}
			map.put(c.getType(), c);
		}
		return map;
	}

	/**
	 * Returns null if the conditions match, otherwise returns a detail explanation.
	 */
	private String matches(Condition cwc, Condition c, Map<ConditionType, Condition> ccMap) {
		// Don't need to worry about any conditions here other than that ones
		// that CW can have (medium, sales, language, print_run, edition, derivative works).

		// cwc.getValue() is not blank (or else this method would not have been called)

		// We should NOT get ANY top level conditions in this method since all those values
		// are always null. Lets check just in case:
		if (cwc.getType().getParentCode() == null) {
			log.error("matches(): Not expecteding top-level condition [" + cwc
					+ "] in this method - probably has true value when should have null.");
			return null;
		}

		// Although we used to have much more generalized logic, so many of the conditions
		// now are no longer in a pure tree format, so the easiest things is to hard
		// code specific logic for each top level condition type.

		String cwCode = cwc.getType().getCode();
		if (cwCode.startsWith("medium_")) {
			// We expect the CW medium condition to always be "All"
			if (cwCode.equals(ConditionCode.MEDIUM_ALL.getCode())) {
				Condition ccMediumAll = ccMap.get(ConditionType.MEDIUM_ALL);
				Condition ccMediumNoMention = ccMap.get(ConditionType.MEDIUM_NO_MENTION);
				if (ccMediumAll != null && ccMediumAll.isValueTrue())  return null;
				else if (ccMediumNoMention != null && ccMediumNoMention.isValueTrue())  return null;
				// else "missing" will be returned below
			}
			else {
				throw new RuntimeException("Unexpected CW condition code [" + cwCode + "]");
			}
		}
		else if (cwCode.startsWith("sales_")) {
			// It is always expected that the CW minimum will be SALES_WORLD_ALIAS
			// We assume this and do not consider other cases
			if (!cwCode.equals(ConditionCode.SALES_WORLD_ALIAS.getCode())) {
				throw new RuntimeException("Unexpected CW condition code [" + cwCode + "]");
			}

			Condition ccSalesWorldAlias = ccMap.get(ConditionType.SALES_WORLD_ALIAS);
			Condition ccSalesNoMention = ccMap.get(ConditionType.SALES_NO_MENTION);
			if (ccSalesWorldAlias != null && ccSalesWorldAlias.isValueTrue())  return null;
			else if (ccSalesNoMention != null && ccSalesNoMention.isValueTrue())  return null;
			// else "missing" will be returned below
		}
		else if (cwCode.startsWith("language_")) {
			// Do a simple match for everything except All Languages, No Mention, and English
			// where we need to worry about matching Alias or non-alias values
			Condition ccLanguageAllAlias = ccMap.get(ConditionType.LANGUAGE_ALL_ALIAS);
			Condition ccLanguageNoMention = ccMap.get(ConditionType.LANGUAGE_NO_MENTION);
			if (ccLanguageAllAlias != null && ccLanguageAllAlias.isValueTrue())  return null;
			else if (ccLanguageNoMention != null && ccLanguageNoMention.isValueTrue())  return null;

			if (cwCode.equals(ConditionCode.LANGUAGE_ENGLISH.getCode())) {
				Condition ccEnglishAlias = ccMap.get(ConditionType.LANGUAGE_ENGLISH_ALIAS);
				if (ccEnglishAlias != null && ccEnglishAlias.isValueTrue()) {
					return null;
				}
			}
			else if (cwCode.equals(ConditionCode.LANGUAGE_ENGLISH_ALIAS.getCode())) {
				Condition ccEnglish = ccMap.get(ConditionType.LANGUAGE_ENGLISH);
				if (ccEnglish != null && ccEnglish.isValueTrue()) {
					return null;
				}
			}

			// simple match
			Condition ccCondition = ccMap.get(cwc.getType());
			if (ccCondition != null && ccCondition.isValueTrue()) {
				return null;
			}
			// else "missing" will be returned below
		}
		else if (cwCode.startsWith("print_run_")) {
			Condition ccPrintRunUnlimited = ccMap.get(ConditionType.PRINT_RUN_UNLIMITED);
			Condition ccPrintRunNoMention = ccMap.get(ConditionType.PRINT_RUN_NO_MENTION);
			if (ccPrintRunUnlimited != null && ccPrintRunUnlimited.isValueTrue())  return null;
			else if (ccPrintRunNoMention != null && ccPrintRunNoMention.isValueTrue())  return null;
			// else "missing" will be returned below unless limited condition is satisfied

			if (cwCode.equals(ConditionCode.PRINT_RUN_UNLIMITED.getCode())) {
				// most of the logic already done above for this
				return "needs to be unlimited";  // instead of "missing" as would get below
			}
			else if (cwCode.equals(ConditionCode.PRINT_RUN_LIMIT.getCode())) {
				// give automatic match - what we really care about is PRINT_RUN_LIMIT_BOX below which has the limit value
				return null;
			}
			else if (cwCode.equals(ConditionCode.PRINT_RUN_LIMIT_BOX.getCode())) {
				// logic above already checked for unlimited in contract, also check for limited
				Condition ccPrintRunLimitBox = ccMap.get(ConditionType.PRINT_RUN_LIMIT_BOX);
				int cwPrintRunValue = Integer.parseInt(cwc.getValue());
				if (ccPrintRunLimitBox == null || StringUtils.isBlank(ccPrintRunLimitBox.getValue())) {
					return "needs to be at least " + cwPrintRunValue;
				}
				else {
					int ccPrintRunValue = Integer.parseInt(ccPrintRunLimitBox.getValue());
					if (ccPrintRunValue < cwPrintRunValue) {
						return "needs to be at least " + cwPrintRunValue;
					}
					else return null;
				}
			}
			else {
				throw new RuntimeException("Unexpected CW condition code [" + cwCode + "]");
			}
		}
		else if (cwCode.startsWith("edition_")) {
			// CW condition for edition will always be "this edition"
			// and all possible choices for contract will match.
			return null;
		}
		else if (cwCode.startsWith("dwork_")) {
			// We expect the CW dwork condition to always be "All"
			if (cwCode.equals(ConditionCode.DERIVATIVE_WORKS_ALL.getCode())) {
				Condition ccDWorkAll = ccMap.get(ConditionType.DERIVATIVE_WORKS_ALL);
				Condition ccDWorkNoMention = ccMap.get(ConditionType.DERIVATIVE_WORKS_NO_MENTION);
				if (ccDWorkAll != null && ccDWorkAll.isValueTrue())  return null;
				else if (ccDWorkNoMention != null && ccDWorkNoMention.isValueTrue())  return null;
				// else "missing" will be returned below
			}
			else {
				throw new RuntimeException("Unexpected CW condition code [" + cwCode + "]");
			}
		}
		else if (cwCode.startsWith("sublicense_")) {
			// Not a valid branch for CW conditions
			throw new RuntimeException("Unexpected CW condition code [" + cwCode + "]");
		}
		else if (cwCode.startsWith("other_")) {
			// Not a valid branch for CW conditions
			throw new RuntimeException("Unexpected CW condition code [" + cwCode + "]");
		}
		else {
			throw new RuntimeException("Unexpected CW condition code [" + cwCode + "]");
		}

		return "missing";
	}

	/**
	 * Returns true if the Contract conditions matches the CommonWork conditions
	 * of the AssetUse.
	 *
	 * Will NOT return false if the contract is expired (that is checked in rules
	 * because we want a different status in that case).
	 *
	 * Assumes that the given au.asset is associated to the given Contract
	 * and does not bother to check again.
	 *
	 * Called by the rules engine (and ContractTest).
	 *
	 * @param au  Must be non-null
	 */
	public ConditionMatchResult matches(Contract contract, AssetUse au) {
		ArgUtil.notNull(au, "AssetUse");
		CommonWork commonWork = au.getCommonWork();
		ArgUtil.notNull(commonWork, "au.commonWork");

		// check for this in rules instead b/c we need OUT_OF_COMPLIANCE status for this instead of CONTRACT_INSUFFICIENT
		//if (contract.isDateExpired())  return new ConditionMatchResult("Contract was expired");

		// This is not exactly a condition but we do want a CONTRACT_INSUFFICIENT status for it
		if (!contract.startEndDateMeetMinGrantYears(commonWork)) {
			log.debug("matches(): returning false match since Contract start/endDate insufficient for mininum grant years");
			return new ConditionMatchResult("Contract start/endDate insufficient for minimum grant years [" + commonWork.getMinGrantYears() + "]");
		}

		// Note we now check conditions for a permission form, just like a full contract.
		//if (contract.isPermissionForm()) {
			// used to return a true match for a permission form
		//}

		Integer cwId = contract.getCommonWork().getId();
		if (cwId == null) {  // Not expected to happen
			throw new IllegalStateException("Contract has a common work with a null id - illegal!");
		}

		// Now ok (under certain conditions such as another edition) for Contract to be for different CW from AssetUse
		//if (!cwId.equals(commonWork.getId())) {
		//	throw new IllegalStateException("This method is not expected to be called for a Contract for a different CommonWork");
		//}

		List<Condition> cwConditions = commonWork.getConditionsNotNull();
		List<Condition> contractConditions = contract.getConditionsNotNull();
		log.debug("matches(): contract conditions count = " + contractConditions.size());
		log.debug("matches(): CwConditions count = " + cwConditions.size());

		Map<ConditionType, Condition> ccMap = conditionListToMap(contractConditions);

		//PerfTimer timer = monitor.startTimer("ConditionRepository::matches(cwConditions, ccMap)");
        ConditionMatchResult match = matches(cwConditions, ccMap);
        //timer.stopTimer();

        // extra logic to match conditions that are on AssetUse instead of CW conditions
        // (don't bother to check if we already had a failed match on some other condition)
        if (match.isMatch()) {
        	Usage usage = au.getUsage();  // non-nullable
        	Size size = au.getSize();  // non-nullable but have "N/A" value
        	List<UsageConditionSize> usageSizeList = loadUsageSizeForContractAsset(contract.getId(), au.getAsset().getId());
        	match = matches(match, usage, size, usageSizeList);
        }

        log.debug("matches(): returning match = " + match);
        return match;
	}

	private final static HashMap<Usage, EnumUsageCondition> usageMap = new HashMap<Usage, EnumUsageCondition>();
	static {
		// Note multiple Usage values map to FRONT_COVER and INTERIOR_SINGLE_USE
		usageMap.put(Usage.BACK_COVER, EnumUsageCondition.BACK_COVER);
		usageMap.put(Usage.CASE_ART, EnumUsageCondition.FRONT_COVER);
		usageMap.put(Usage.CDROM, EnumUsageCondition.CDROM);
		usageMap.put(Usage.ENDPAPERS, EnumUsageCondition.INTERIOR_SINGLE_USE);
		usageMap.put(Usage.EXHIBIT, EnumUsageCondition.INTERIOR_SINGLE_USE);
		usageMap.put(Usage.FEATURE, EnumUsageCondition.INTERIOR_SINGLE_USE);
		usageMap.put(Usage.FIGURE, EnumUsageCondition.INTERIOR_SINGLE_USE);
		usageMap.put(Usage.FLAPS, EnumUsageCondition.FLAPS);
		usageMap.put(Usage.FRONT_COVER, EnumUsageCondition.FRONT_COVER);
		usageMap.put(Usage.FRONT_MATTER, EnumUsageCondition.INTERIOR_SINGLE_USE);
		usageMap.put(Usage.ICON, EnumUsageCondition.INTERIOR_SINGLE_USE);
		usageMap.put(Usage.INLINE_TEXT, EnumUsageCondition.INTERIOR_SINGLE_USE);
		usageMap.put(Usage.OPENER, EnumUsageCondition.OPENER_EPIGRAPH);
		usageMap.put(Usage.SPINE, EnumUsageCondition.SPINE);
		usageMap.put(Usage.TABLE, EnumUsageCondition.INTERIOR_SINGLE_USE);
		usageMap.put(Usage.UNNUMBERED_FIGURE, EnumUsageCondition.INTERIOR_SINGLE_USE);
		usageMap.put(Usage.WRAP_COVER, EnumUsageCondition.WRAP_COVER);
	}

	private ConditionMatchResult matches(ConditionMatchResult trueMatch, Usage usage, Size size, List<UsageConditionSize> usageSizeList) {
		log.debug("matches() (Usage/Size): AU Usage = " + usage + ", AU Size = " + size);

		// normally we expect that there will be at least one element in the
		// list (which may have Usage null and Size N/A) but the data
		// conversion from the old conditions will not create anything in some
		// cases so accept this also (nothing means everything)
		if (usageSizeList.size() == 0) {
			return trueMatch;
		}

		for (UsageConditionSize pair : usageSizeList) {
			// If both Usage and Size match then we return trueMatch, otherwise continue the loop

			EnumUsageCondition usageCondition = pair.getUsageCondition();

			boolean usageMatch = false;
			if (usageCondition == null)  {
				log.debug("matches() (Usage/Size): usageCondition is null so matches");
				usageMatch = true;
			}
			else {
				EnumUsageCondition usageConditionRequired = usageMap.get(usage);
				usageMatch = usageCondition.equals(usageConditionRequired);
			}

			if (!usageMatch) continue;

			Size sizeCondition = pair.getSize();
			boolean sizeMatch = false;
			// No real need to check for NA since NA has larger compValue than everything
			//if (sizeCondition.equals(Size.NA)) {
			//	log.debug("matches() (Usage/Size): sizeCondition is N/A so matches");
			//	sizeMatch = true;
			//}
			//else {
				sizeMatch = sizeCondition.getCompValue() >= size.getCompValue();
			//}

			if (sizeMatch)  return trueMatch;
		}

		return new ConditionMatchResult("au.usage of [" + usage.getCode()
				+ "] plus au.size of [" + size.getCode() + "] not met (one or other not met)");
	}

	/**
	 * Returns true if there were no existing conditions (and default conditions
	 * were created).
	 * Returns false if there were existing conditions (does nothing in this case).
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public boolean createDefaultCWConditions(int cwId) throws Exception {
		// Logic:
		// If the CW already has any conditions, do nothing.
		// Otherwise:
		// Set the following (these are read-only for CW):
		// - init Medium to all
		// - init Sales Territory to world (all)
		// - init Derivative works to all
		// - init edition to this edition only
		// (following are read-write for CW):
		// - init Language to English
		// - init print run to unlimited

		CommonWork commonWork = commonWorkRepository.loadCWById(cwId);

		log.debug("createDefaultConditions(): # existing conditions = " + commonWork.getConditionsNotNull().size());
		if (commonWork.getConditionsNotNull().size() > 0) {
			return false;
		}

		// Create a Collection of conditions to add and then add all at once
		// because service method will also update PermissionStatuses for whole CW.
		ArrayList<Condition> conditionsToAdd = new ArrayList<Condition>();

		conditionsToAdd.add(Condition.MEDIUM.copyNoId());
		conditionsToAdd.add(Condition.MEDIUM_ALL.copyNoId());

		conditionsToAdd.add(Condition.SALES_TERRITORY.copyNoId());
		conditionsToAdd.add(Condition.SALES_WORLD_ALIAS.copyNoId());

		conditionsToAdd.add(Condition.DERIVATIVE_WORKS.copyNoId());
		conditionsToAdd.add(Condition.DERIVATIVE_WORKS_ALL.copyNoId());

		conditionsToAdd.add(Condition.EDITION.copyNoId());
		conditionsToAdd.add(Condition.EDITION_THIS.copyNoId());

		conditionsToAdd.add(Condition.LANGUAGE.copyNoId());
		conditionsToAdd.add(Condition.LANGUAGE_ENGLISH_ALIAS.copyNoId());

		conditionsToAdd.add(Condition.PRINT_RUN.copyNoId());
		conditionsToAdd.add(Condition.PRINT_RUN_UNLIMITED.copyNoId());

		for (Condition c : conditionsToAdd) {
			persist(c);
			commonWork.getConditionsNotNull().add(c);
		}
		// at one point needed to do this to get add conditions attached to cw but don't need to anymore (tested)
		//entityManager.merge(commonWork);

		return true;
	}

	/**
	 * This method does NOT update the database at all, just changes conditions
	 * in memory. Currently this method does nothing (leaves conditions blank)
	 * for full contracts but does set conditions for permission forms.
	 *
	 * @param nodeMap  Must be non-null
	 */
	private void initContractConditions(HashMap<String, ConditionNode> nodeMap, boolean isPermissionForm) {
		log.debug("initContractConditions(): entered...isPermissionForm = " + isPermissionForm);

		if (!isPermissionForm) return;  // leave all conditions blank if not permissionForm

		ConditionNode mediumAll = nodeMap.get(ConditionType.MEDIUM_ALL.getCode());
		mediumAll.setValueToTrue();

		ConditionNode salesWorldAlias = nodeMap.get(ConditionType.SALES_WORLD_ALIAS.getCode());
		salesWorldAlias.setValueToTrue();

		ConditionNode langAllAlias = nodeMap.get(ConditionType.LANGUAGE_ALL_ALIAS.getCode());
		langAllAlias.setValueToTrue();

		ConditionNode printRunUnlimited = nodeMap.get(ConditionType.PRINT_RUN_UNLIMITED.getCode());
		printRunUnlimited.setValueToTrue();

		// EDITION_FUTURE is the second highest setting for edition
		ConditionNode editionFuture = nodeMap.get(ConditionType.EDITION_FUTURE.getCode());
		editionFuture.setValueToTrue();

		ConditionNode dworkAll = nodeMap.get(ConditionType.DERIVATIVE_WORKS_ALL.getCode());
		dworkAll.setValueToTrue();

		ConditionNode sublicenseRight = nodeMap.get(ConditionType.SUBLICENSE_RIGHT.getCode());
		sublicenseRight.setValueToTrue();
	}

	/**
	 * This method does NOT update the database at all, just changes conditions
	 * in memory.
	 *
	 * @param nodeMap  Must be non-null
	 */
	private void initMasterAgreementConditions(HashMap<String, ConditionNode> nodeMap) {
		log.debug("initMasterAgreementConditions(): entered...");

		// These are conditions that don't show in the UI for MA conditions but
		// we do want to set them so when they are copied over to a contract they will show.
		// Set Medium/Media to All, Sales to World, Derivative Works to All

		ConditionNode mediumAll = nodeMap.get(ConditionType.MEDIUM_ALL.getCode());
		mediumAll.setValueToTrue();

		ConditionNode salesWorldAlias = nodeMap.get(ConditionType.SALES_WORLD_ALIAS.getCode());
		salesWorldAlias.setValueToTrue();

		ConditionNode dworkAll = nodeMap.get(ConditionType.DERIVATIVE_WORKS_ALL.getCode());
		dworkAll.setValueToTrue();
	}

	// this method is currently not used but might be useful someday (keep for now)
	@SuppressWarnings("unused")
	private void overwriteBranchValues(ConditionNode source, ConditionNode target) {
		if (!source.getCode().equals(target.getCode())) {
			// this should never happen if this method is used properly
			throw new RuntimeException("Overwriting branch of different type not allowed: source ["
				+ source.getCode() + "] target [" + target.getCode() + "]");
		}

		target.setValue(source.getValue());

		int sourceNumChildren = source.getChildrenNotNull().size();
		int targetNumChildren = target.getChildrenNotNull().size();
		if (sourceNumChildren != targetNumChildren) {
			// this should never happen if this method is used properly
			throw new RuntimeException("Source and Target #children don't match: source # = "
				+ sourceNumChildren + ", target # = " + targetNumChildren);
		}

		if (sourceNumChildren > 0) {
			for (int i = 0; i < sourceNumChildren; i++) {
				ConditionNode sourceChild = source.getChildren().get(i);
				ConditionNode targetChild = target.getChildren().get(i);
				overwriteBranchValues(sourceChild, targetChild);  // recursive call
			}
		}
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public boolean doesCWHaveConditions(int cwId) {
		String sql = "select count(*) as count from cw_2_condition where cw_id = ?";
		Query query = entityManager.createNativeQuery(sql, "scalarCount");
		query.setParameter(1, cwId);

		// For count, DB2 and MS SQL Server return Integer but MySQL returns BigInteger
		// Integer and BigInteger inherit from Number
		Number resultNum = (Number) query.getSingleResult();
		return resultNum.intValue() > 0;
	}


	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public int getPrintRunForProductsUsingAsset(int assetId) {
		int count = 0;
		// lnagy - can we combine the 2 queries ? it seems like it is adding the values
		try {
			/* bad performance
			 * String sql = "select sum(received_quantity) as count from product_printing " +
				"where product_id in ( " +
				"select id from product where cw_id in ( " +
				"select cw_id from asset_use where asset_id = ? )) group by po_status having po_status = 'closed'";
			*/
			String sql = "select sum(received_quantity) as count from product_printing pp " +
					"	  join product p on pp.product_id = p.id " +
					"	  	and pp.po_status in ('open', 'closed') " +
					"	  join asset_use au on au.cw_id = p.cw_id and asset_id = ?";
			Query query = entityManager.createNativeQuery(sql, "scalarCount");
			query.setParameter(1, assetId);
			// For count, DB2 and MS SQL Server return Integer but MySQL returns BigInteger
			// Integer and BigInteger inherit from Number
			Number result = (Number) query.getSingleResult();
			count = result.intValue();
		} catch (Exception e) {
			// in case there are no records to sum
			// (count remains 0)
		}

/*
 * lnagy - combine the 2 queries into one and change it for performance reasons
		try {
			 String sql = "select sum(received_quantity) as count from product_printing " +
				"where product_id in ( " +
				"select id from product where cw_id in ( " +
				"select cw_id from asset_use where asset_id = ? )) group by po_status having po_status = 'open'";
			Query query2 = entityManager.createNativeQuery(sql, "scalarCount");
			query2.setParameter(1, assetId);
			// For count, DB2 and MS SQL Server return Integer but MySQL returns BigInteger
			// Integer and BigInteger inherit from Number
			Number result = (Number) query2.getSingleResult();
			count = count + result.intValue();
		} catch (Exception e) {
			// no need to handle
		}
*/
		return count;
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public boolean hasSeat(AssetUse assetUse) {
		Asset asset = assetUse.getAsset();
		int totalSeats = asset.getTotalSeats();

		// unlimited seats
		if (totalSeats <= 0)  {
			log.debug ("hasSeat(): unlimited seats, so seats are available");
			return true;
		}

		// if seats available
		if (asset.getTotalUsedSeats() <= totalSeats) {
			log.debug ("hasSeat(): seats are available, no need to calculate the creators: used [" + asset.getTotalUsedSeats() + "] vs. total [" + totalSeats + "]");
			return true;
		}

		HashSet<Integer> userIdSet = new HashSet<Integer>();
		List<AssetUse> assetUses = asset.getAssetUses();
		java.util.Collections.sort(assetUses, new AssetUseIdComparator());
		for (AssetUse au : assetUses) {
			if (userIdSet.size() >= totalSeats) break;
			userIdSet.add(au.getCreatedUser().getId());
		}

		boolean userHasSeat = userIdSet.contains(assetUse.getCreatedUser().getId());
		log.debug("hasSeat(): returning " + userHasSeat);
		return userHasSeat;
	}

	public List<ConditionNode> updateConditionNodeData(String type, String value, String rollupValue, List<ConditionNode> conditionNodes) {

		for (ConditionNode node : conditionNodes) {
			if (null != node.getChildren() && node.getChildren().size() > 0) {
				node.setChildren(updateConditionNodeData(type, value, rollupValue, node.getChildren()));
			}
			if (node.getCode().equals(type)) {
				node.setValue(value);
				node.setRollupValue(rollupValue);
				if (null != value && value.trim().toLowerCase().equals("true")) {
					// set children also if check box or radio
					node = checkUncheckSubTree(node, true);
				}
				return conditionNodes;
			}
		}
		return conditionNodes;
	}

	public List<ConditionNode> checkUncheckNode(String code, List<ConditionNode> conditionNodes, boolean set) {
		for (ConditionNode node : conditionNodes) {
			if (null != node.getChildren() && node.getChildren().size() > 0) {
				node.setChildren(checkUncheckNode(code, node.getChildren(), set));
			}
			if (node.getCode().equals(code)) {
				if (set) {
					node.setValueToTrue();
				} else {
					node.setValue(null);
				}
				// go do children too
				checkUncheckSubTree(node, set);
				return conditionNodes;
			}
		}
		return conditionNodes;
	}

	private ConditionNode checkUncheckSubTree(ConditionNode node, boolean set) {
		if (null != node.getChildren()) {
			for (ConditionNode cnode : node.getChildren()) {
				cnode = checkUncheckSubTree(cnode, set);
				if (set) {
					cnode.setValueToTrue();
				} else {
					cnode.setValue(null);
				}
			}
		}
		return node;
	}


	/**
	 * returns a string of HTML formated conditions to be used by the view page
	 * @param conditions
	 * @return String of conditions
	 */
	public String calculateRollupString(List<ConditionNode> conditions) {
		String rollup = "<table>";
		for (ConditionNode condition : conditions) {
			if (!StringUtils.isEmpty(condition.getRollupValue())) {
				rollup += "<tr><td>" + condition.getDescription() + " </td><td> " + condition.getRollupValue() + "</td></tr> ";
			}
		}
		rollup += "</table>";
		return rollup;
	}
	//Added for DM-289
	public List<ConditionType> loadAllConditionTypeFromDB() {
		String sql = "select * from condition_type";
		Query query = entityManager.createNativeQuery(sql, ConditionType.class);
		@SuppressWarnings("unchecked")
		List<ConditionType> conditionTypeList = query.getResultList();
		return conditionTypeList;
	}

	public CommonWorkRepository getCommonWorkRepository() {
		return commonWorkRepository;
	}
	
	public void setCommonWorkRepository(CommonWorkRepository commonWorkRepository) {
		this.commonWorkRepository = commonWorkRepository;
	}

	public CommonWorkService getCommonWorkService() {
		return commonWorkService;
	}
	
	public void setCommonWorkService(CommonWorkService commonWorkService) {
		this.commonWorkService = commonWorkService;
	}

	public PurchaseOrderService getPurchaseOrderService() {
		return purchaseOrderService;
	}
	
	public void setPurchaseOrderService(PurchaseOrderService purchaseOrderService) {
		this.purchaseOrderService = purchaseOrderService;
	}

	public ContractService getContractService() {
		return contractService;
	}
	
	public void setContractService(ContractService contractService) {
		this.contractService = contractService;
	}
	
	public void setUserRepository(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public UserRepository getUserRepository() {
		return userRepository;
	}
}
