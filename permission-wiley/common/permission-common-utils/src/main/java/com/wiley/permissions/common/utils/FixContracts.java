package com.wiley.permissions.common.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.util.StringUtils;

import com.wiley.sf.common.sql.SimpleDBConnect;
import com.wiley.sf.common.text.TimeFormat;

/**
 * This program looks at multiple contracts for the same common work and source
 * having the same number. This situation should not exist so tries to fix the
 * problem by merging these contracts.
 *
 * Simplest version of problem: There are two contracts, each with a single
 * asset for a particular common work and source, and having the same contract
 * number. The two contracts are identical except for the their price and the
 * fact that they are for different assets. So in this case the program moves
 * one asset from one contract to the other, and deletes one contract. Also
 * alters the total price of the contract that now has 2 assets.
 *
 * This program takes care of the most common situations but some situations
 * need to be helped along with manual tweaking of data or done entirely
 * manually. This program tries to NOT do any work where the situation is
 * considered to be outside the scope of what the program can handle.
 *
 * @since JDK 1.6
 * @version 2/10/2014
 * @author Steve Markoff
 */
public class FixContracts {

	private static final Log log = LogFactory.getLog(FixContracts.class);

	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			System.err.println("Usage: <properties file>");
			System.exit(1);
		}

		SimpleDBConnect dbConnect = SimpleDBConnect.create(args[0]);
		// throws various exceptions
		FixContracts prog = new FixContracts(dbConnect.getConnection(), true);
		prog.go();
	}

	private final Connection con;
	private final boolean logToSystemOut;
	private final boolean makeChanges = true;

	private int equalCount = 0;
	private int notEqualCount = 0;

	public FixContracts(Connection con, boolean logToSystemOut)
			throws SQLException {
		this.con = con;
		this.logToSystemOut = logToSystemOut;
		con.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
		con.setAutoCommit(true);
	}

	public void go() throws SQLException, IOException {
		long startTime = System.currentTimeMillis();

		List<Summary> list = getCounts();

		for (int i = 0; i < list.size(); i++) {
			Summary summary = list.get(i);
			if (summary.getCount() > 1) {
				process(summary, i);
			}
		}

		long time = System.currentTimeMillis() - startTime;
		TimeFormat timeFormat = new TimeFormat();
		log("total time was " + timeFormat.formatMS(time));

		log("equalCount = " + equalCount);
		log("notEqualCount = " + notEqualCount);
	}

	private List<Summary> getCounts() throws SQLException {
		String sql = "select count(*) as count, number, source_id, cw_id from contract"
				+ " where number is not null group by number, source_id, cw_id order by count desc, cw_id";
		PreparedStatement ps = con.prepareStatement(sql);
		ResultSet rs = ps.executeQuery();
		List<Summary> list = new ArrayList<Summary>();
		while (rs.next()) {
			list.add(new Summary(rs.getInt(1), rs.getString(2), rs.getInt(3),
					rs.getInt(4)));
		}
		ps.close();
		return list;
	}

	private void process(Summary summary, int index) throws SQLException,
			IOException {
		log("---- " + index + ": cwId = " + summary.getCwId() + ", sourceId = "
				+ summary.getSourceId() + ", number = " + summary.getNumber()
				+ ", count = " + summary.getCount());
		List<Contract> contracts = getContracts(summary);

		// first make sure all the assets for these contracts are different
		// if they are the same, that's a different situation that this program
		// is not build for - but see CleanDuplicateContracts.java which deals with that
		//

		HashSet<Integer> assetIdSet = new HashSet<Integer>();
		int totalListCount = 0;
		for (Contract contract : contracts) {
			List<Integer> list = getAssetIdsForContract(contract.getId());
			totalListCount += list.size();
			assetIdSet.addAll(list);
		}
		if (totalListCount > assetIdSet.size()) {  // this means some of the assetsIs are repeated
			log("-- skipping because at least some of the assets are the same - see CleanDuplicateContracts.java for this.");
			return;
		}

		Contract lastContract = null;
		StringBuilder reasonSb = new StringBuilder();
		for (Contract contract : contracts) {
			List<Condition> conditions = getConditions(contract);
			contract.setConditions(conditions);
			log(contract.toString());
			for (Condition c : contract.getConditions()) {
				log("  " + c.toString());
			}
			if (lastContract != null) {
				reasonSb.append(lastContract.equalsExceptIdAndPrice(contract));
			}
			lastContract = contract;
		}
		String reason = reasonSb.toString();
		if (StringUtils.isNotBlank(reason)) {
			log("not equals because: " + reason);
			notEqualCount++;
			if (reason.contains("conditions[credit_line]")
					&& !reason.contains("conditions.size")) {
				fixCreditLine(contracts);
			} else if (reason.contains("conditions")
					&& !reason.contains("assetCount")
					&& !reason.contains("startDate")
					&& !reason.contains("endDate")
					&& !reason.contains("contractType")
					&& !reason.contains("isPermissionForm")
					&& !reason.contains("date") && !reason.contains("currency")
					&& !reason.contains("poId")
					&& !reason.contains("paidByAuthor")
					&& !reason.contains("paymentType")
					&& !reason.contains("paymentDate")
					&& !reason.contains("needsAmendment") && promptMerge()) {
				mergeContracts(contracts);
			}
		} else {
			log("--all equal except id and price");
			equalCount++;
			mergeContracts(contracts);
		}
	}

	private boolean promptMerge() throws IOException {
		System.out
				.print("Merge contracts even though conditions don't match? (y/n) > ");
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String line = br.readLine();
		return line.startsWith("y");
	}

	private void mergeContracts(List<Contract> contracts) throws SQLException,
			IOException {
		con.setAutoCommit(false);

		log("merging contracts...");

		Contract firstContract = contracts.get(0);
		final int firstContractId = firstContract.getId();

		// get size of first contract file (if exists) - will assume that
		// any file for other contracts is the same if it is the same size
		int fileBytes = 0;
		String sql = "select length(file_data) from contract_file where contract_id = ?";
		{ // scope
			PreparedStatement ps = con.prepareStatement(sql);
			ps.setInt(1, firstContractId);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				fileBytes = rs.getInt(1);
				NumberFormat intFormat = NumberFormat.getIntegerInstance();
				log("file bytes = " + intFormat.format(fileBytes));
			}
			ps.close();
		}

		// copy price on this first contract from the contract to
		// contract_2_asset
		// If there are multiple assets then the price on contract_2_asset may
		// already be set so only change the price if null.
		sql = "update contract_2_asset set price = (select price from contract where id = ?) where contract_id = ? and price is null";
		String psql = sql.replace("?", "<" + firstContractId + ">");
		log("pending: " + psql);
		if (makeChanges) {
			PreparedStatement ps = con.prepareStatement(sql);
			ps.setInt(1, firstContractId);
			ps.setInt(2, firstContractId);
			int rows = ps.executeUpdate();
			log(String.valueOf(rows) + " rows updated.");
			ps.close();
		}

		double totalPrice = 0;
		for (Contract contract : contracts) {
			totalPrice += contract.getPrice();
			if (contract.getId() == firstContractId)
				continue;

			List<Integer> contractAssetIds = getAssetIdsForContract(contract
					.getId());

			if (contractAssetIds.size() == 1) {
				// add asset id to first contract with price
				sql = "insert into contract_2_asset (contract_id, asset_base_id, price) values (?, ?, ?)";
				psql = sql.replaceFirst("\\?", "<" + firstContractId + ">");
				psql = psql.replaceFirst("\\?",
						"<" + contract.getFirstAssetId() + ">");
				psql = psql.replace("?", "<" + contract.getPrice() + ">");
				log("pending: " + psql);
				if (makeChanges) {
					PreparedStatement ps = con.prepareStatement(sql);
					ps.setInt(1, firstContractId);
					ps.setInt(2, contract.getFirstAssetId());
					ps.setDouble(3, contract.getPrice());
					ps.executeUpdate();
					ps.close();
				}
			} else {
				// with multiple assets, copy over price from existing record
				// (which may be null)
				// instead of copying price from contract
				sql = "insert into contract_2_asset (contract_id, asset_base_id, price)"
						+ " select ?, asset_base_id, price from contract_2_asset where contract_id = ?";
				psql = sql.replaceFirst("\\?", "<" + firstContractId + ">");
				psql = psql.replace("?", "<" + contract.getId() + ">");
				log("pending: " + psql);
				if (makeChanges) {
					PreparedStatement ps = con.prepareStatement(sql);
					ps.setInt(1, firstContractId);
					ps.setInt(2, contract.getId());
					int rows = ps.executeUpdate();
					log(String.valueOf(rows) + " rows inserted.");
					ps.close();
				}
			}

			moveUsageAndSize(contract.getId(), firstContractId);

			// delete asset id(s) from old contract and then delete contract
			sql = "delete from contract_2_asset where contract_id = ?";
			psql = sql.replace("?", "<" + contract.getId() + ">");
			log("pending: " + psql);
			if (makeChanges) {
				PreparedStatement ps = con.prepareStatement(sql);
				ps.setInt(1, contract.getId());
				int rows = ps.executeUpdate();
				log(String.valueOf(rows) + " rows deleted.");
				ps.close();
			}

			deleteConditions(contract.getId());

			sql = "delete from contract_file where contract_id = ? and length(file_data) = ?";
			psql = sql.replaceFirst("\\?", "<" + contract.getId() + ">");
			psql = psql.replace("?", "<" + fileBytes + ">");
			log("pending: " + psql);
			if (makeChanges) {
				PreparedStatement ps = con.prepareStatement(sql);
				ps.setInt(1, contract.getId());
				ps.setInt(2, fileBytes);
				ps.executeUpdate();
				ps.close();
			}

			sql = "update asset_perm_ref set contract_id = ? where contract_id = ?";
			psql = sql.replaceFirst("\\?", "<" + firstContractId + ">");
			psql = psql.replace("?", "<" + contract.getId() + ">");
			log("pending: " + psql);
			if (makeChanges) {
				PreparedStatement ps = con.prepareStatement(sql);
				ps.setInt(1, firstContractId);
				ps.setInt(2, contract.getId());
				ps.executeUpdate();
				ps.close();
			}

			sql = "update au_source_perm_status set latest_contract_id = ? where latest_contract_id = ?";
			psql = sql.replaceFirst("\\?", "<" + firstContractId + ">");
			psql = psql.replace("?", "<" + contract.getId() + ">");
			log("pending: " + psql);
			if (makeChanges) {
				PreparedStatement ps = con.prepareStatement(sql);
				ps.setInt(1, firstContractId);
				ps.setInt(2, contract.getId());
				ps.executeUpdate();
				ps.close();
			}

			sql = "update au_source_perm_status set active_contract_id = ? where active_contract_id = ?";
			psql = sql.replaceFirst("\\?", "<" + firstContractId + ">");
			psql = psql.replace("?", "<" + contract.getId() + ">");
			log("pending: " + psql);
			if (makeChanges) {
				PreparedStatement ps = con.prepareStatement(sql);
				ps.setInt(1, firstContractId);
				ps.setInt(2, contract.getId());
				ps.executeUpdate();
				ps.close();
			}

			// assume same comp_copy assigned to first contract (if there is a
			// comp_copy at all)
			sql = "delete from comp_copy where contract_id = ?";
			psql = sql.replace("?", "<" + contract.getId() + ">");
			log("pending: " + psql);
			if (makeChanges) {
				PreparedStatement ps = con.prepareStatement(sql);
				ps.setInt(1, contract.getId());
				ps.executeUpdate();
				ps.close();
			}

			sql = "delete from contract where id = ?";
			psql = sql.replace("?", "<" + contract.getId() + ">");
			log("pending: " + psql);
			if (makeChanges) {
				PreparedStatement ps = con.prepareStatement(sql);
				ps.setInt(1, contract.getId());
				ps.executeUpdate();
				ps.close();
			}
		} // end for Contract

		// update price
		sql = "update contract set price = ? where id = ?";
		psql = sql.replaceFirst("\\?", "<" + totalPrice + ">");
		psql = psql.replace("?", "<" + firstContractId + ">");
		log("pending: " + psql);
		if (makeChanges) {
			PreparedStatement ps = con.prepareStatement(sql);
			ps.setDouble(1, totalPrice);
			ps.setInt(2, firstContractId);
			ps.executeUpdate();
			ps.close();
		}

		System.out.print("Hit Enter to commit the above contract merge...");
		if (!makeChanges) {
			System.out
					.print("(No change will actually be made since makeChanges is false)");
		}
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		br.readLine();

		con.commit();
		con.setAutoCommit(true);
	}

	private void deleteConditions(int contractId) throws SQLException {
		// delete relationship, then condition_value
		// first must select condition ids to delete later
		String sql = "select map.condition_id from contract_2_condition map, condition_value cv"
				+ " where map.contract_id = ? and cv.id = map.condition_id";
		PreparedStatement ps = con.prepareStatement(sql);
		ps.setInt(1, contractId);
		ResultSet rs = ps.executeQuery();
		List<Integer> conditionIdList = new ArrayList<Integer>();
		while (rs.next()) {
			conditionIdList.add(rs.getInt(1));
		}
		ps.close();

		sql = "delete from contract_2_condition where condition_id = ?";
		ps = con.prepareStatement(sql);
		for (Integer conditionId : conditionIdList) {
			String psql = sql.replace("?", "<" + conditionId + ">");
			log("pending: " + psql);
			if (makeChanges) {
				ps.setInt(1, conditionId);
				ps.executeUpdate();
			}
		}
		ps.close();

		sql = "delete from condition_value where id = ?";
		ps = con.prepareStatement(sql);
		for (Integer conditionId : conditionIdList) {
			String psql = sql.replace("?", "<" + conditionId + ">");
			log("pending: " + psql);
			if (makeChanges) {
				ps.setInt(1, conditionId);
				ps.executeUpdate();
			}
		}
		ps.close();
	}

	private void moveUsageAndSize(int contractFromId, int contractToId)
			throws SQLException {
		String sql = "update usage_2_size set contract_id = ? where contract_id = ?";
		PreparedStatement ps = con.prepareStatement(sql);
		String psql = sql.replaceFirst("\\?", "<" + contractToId + ">");
		psql = psql.replace("?", "<" + contractFromId + ">");
		log("pending: " + psql);
		if (makeChanges) {
			ps.setInt(1, contractToId);
			ps.setInt(2, contractFromId);
			int rows = ps.executeUpdate();
			log(String.valueOf(rows) + " rows updated.");
		}
		ps.close();
	}

	private void fixCreditLine(List<Contract> contracts) throws SQLException,
			IOException {
		con.setAutoCommit(false);

		for (Contract c : contracts) {
			fixCreditLine(c);
		}

		System.out.print("Hit Enter to commit the above SQL...");
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		br.readLine();

		con.commit();
		con.setAutoCommit(true);
	}

	private void fixCreditLine(Contract contract) throws SQLException,
			IOException {
		// set credit line for asset(s) if current asset(s) credit line is null
		// - handles 1 or more assets for a contract that only has global
		// conditions

		Condition creditLineCondition = getCreditLineCondition(contract
				.getConditions());
		String sql = "update asset set credit_line = ? where id IN "
				+ "(select asset_base_id from contract_2_asset where contract_id = ?) and credit_line is null";
		String psql = sql.replaceFirst("\\?",
				"<" + creditLineCondition.getValue() + ">");
		psql = psql.replace("?", "<" + contract.getId() + ">");
		log("pending: " + psql);
		if (makeChanges) {
			PreparedStatement ps = con.prepareStatement(sql);
			ps.setString(1, creditLineCondition.getValue());
			ps.setInt(2, contract.getId());
			int rows = ps.executeUpdate();
			if (rows == 1)
				log("fixed creditLine for contract id " + contract.getId());
			ps.close();
		}

		// delete credit line condition from contract_2_condition and
		// condition_value tables
		sql = "delete from contract_2_condition where condition_id = ?";
		psql = sql.replace("?", "<" + creditLineCondition.getId() + ">");
		log("pending: " + psql);
		if (makeChanges) {
			PreparedStatement ps = con.prepareStatement(sql);
			ps.setInt(1, creditLineCondition.getId());
			int rows = ps.executeUpdate();
			if (rows == 1)
				log("deleted creditLine condition relationship");
			ps.close();
		}

		sql = "delete from condition_value where id = ?";
		psql = sql.replace("?", "<" + creditLineCondition.getId() + ">");
		log("pending: " + psql);
		if (makeChanges) {
			PreparedStatement ps = con.prepareStatement(sql);
			ps.setInt(1, creditLineCondition.getId());
			int rows = ps.executeUpdate();
			if (rows == 1)
				log("deleted creditLine condition");
			ps.close();
		}
	}

	private Condition getCreditLineCondition(List<Condition> conditions) {
		for (Condition c : conditions) {
			if (c.getConditionType().equals("credit_line")) {
				return c;
			}
		}
		return null;
	}

	private List<Contract> getContracts(Summary summary) throws SQLException {
		String sql = "select id, (select count(*) from contract_2_asset where contract_id = id) as asset_count,"
				+ " start_date, end_date, contract_type, is_permission_form, date, price, currency, purchase_order_id,"
				+ " payment_type, payment_date,"
				+ " (select asset_base_id from contract_2_asset where contract_id = id order by asset_base_id limit 1) as first_asset_id"
				+ " from contract where cw_id = ? and source_id = ? and number = ? order by id";
		PreparedStatement ps = con.prepareStatement(sql);
		ps.setInt(1, summary.getCwId());
		ps.setInt(2, summary.getSourceId());
		ps.setString(3, summary.getNumber());
		ResultSet rs = ps.executeQuery();
		List<Contract> list = new ArrayList<Contract>();
		while (rs.next()) {
			list.add(new Contract(rs.getInt(1), rs.getInt(2), rs
					.getTimestamp(3), rs.getTimestamp(4), rs.getString(5), rs
					.getBoolean(6), rs.getTimestamp(7), rs.getDouble(8), rs
					.getString(9), rs.getInt(10), rs.getString(11), rs
					.getTimestamp(12), rs.getInt(13)));
		}
		ps.close();
		return list;
	}

	private List<Integer> getAssetIdsForContract(int contractId)
			throws SQLException {
		String sql = "select asset_base_id from contract_2_asset where contract_id = ? order by asset_base_id";
		PreparedStatement ps = con.prepareStatement(sql);
		ps.setInt(1, contractId);
		ResultSet rs = ps.executeQuery();
		List<Integer> list = new ArrayList<Integer>();
		while (rs.next()) {
			list.add(new Integer(rs.getInt(1)));
		}
		ps.close();
		return list;
	}

	private List<Condition> getConditions(Contract contract)
			throws SQLException {
		String sql = "select condition_type, value, id from view_contract_condition where contract_id = ? order by condition_type";
		PreparedStatement ps = con.prepareStatement(sql);
		ps.setInt(1, contract.getId());
		ResultSet rs = ps.executeQuery();
		List<Condition> list = new ArrayList<Condition>();
		while (rs.next()) {
			list.add(new Condition(rs.getString(1), rs.getString(2), rs
					.getInt(3)));
		}
		ps.close();
		return list;
	}

	private void log(String msg) {
		if (logToSystemOut) {
			System.out.println(msg);
		} else {
			log.info(msg);
		}
	}

	class Summary {
		private final int count;
		private final String number;
		private final int sourceId;
		private final int cwId;

		public Summary(int count, String number, int sourceId, int cwId) {
			this.count = count;
			this.number = number;
			this.sourceId = sourceId;
			this.cwId = cwId;
		}

		public int getCount() {
			return count;
		}

		public String getNumber() {
			return number;
		}

		public int getCwId() {
			return cwId;
		}

		public int getSourceId() {
			return sourceId;
		}

		@Override
		public String toString() {
			return "(" + count + ", " + number + ", " + sourceId + ", " + cwId
					+ ")";
		}
	}

	class Contract {
		private final int id;
		private final int assetCount;
		private final Timestamp startDate;
		private final Timestamp endDate;
		private final String contractType;
		private final boolean isPermissionForm;
		private final Timestamp date;
		private final double price;
		private final String currency;
		private final int poId;
		private final String paymentType;
		private final Timestamp paymentDate;
		private final int firstAssetId;

		private List<Condition> conditions;

		public Contract(int id, int assetCount, Timestamp startDate,
				Timestamp endDate, String contractType,
				boolean isPermissionForm, Timestamp date, double price,
				String currency, int poId, String paymentType,
				Timestamp paymentDate, int firstAssetId) {
			this.id = id;
			this.assetCount = assetCount;
			this.startDate = startDate;
			this.endDate = endDate;
			this.contractType = contractType;
			this.isPermissionForm = isPermissionForm;
			this.date = date;
			this.price = price;
			this.currency = currency;
			this.poId = poId;
			this.paymentType = paymentType;
			this.paymentDate = paymentDate;
			this.firstAssetId = firstAssetId;
		}

		@Override
		public String toString() {
			return "id [" + id + "] assetCount [" + assetCount
					+ "] startDate [" + startDate + "] endDate [" + endDate
					+ "] contractType [" + contractType
					+ "] isPermissionForm [" + isPermissionForm + "] date ["
					+ date + "] price [" + price + "] currency [" + currency
					+ "] poId [" + poId + "] paymentType [" + paymentType
					+ "] paymentDate [" + paymentDate + "]";
		}

		public int getId() {
			return id;
		};

		public int getAssetCount() {
			return assetCount;
		}

		public Timestamp getStartDate() {
			return startDate;
		}

		public Timestamp getEndDate() {
			return endDate;
		}

		public String getContractType() {
			return contractType;
		}

		public boolean isPermissionForm() {
			return isPermissionForm;
		}

		public Timestamp getDate() {
			return date;
		}

		public double getPrice() {
			return price;
		}

		public String getCurrency() {
			return currency;
		}

		public int getPoId() {
			return poId;
		}

		public String getPaymentType() {
			return paymentType;
		}

		public Timestamp getPaymentDate() {
			return paymentDate;
		}

		public int getFirstAssetId() {
			return firstAssetId;
		}

		public List<Condition> getConditions() {
			return conditions;
		}

		public void setConditions(List<Condition> list) {
			this.conditions = list;
		}

		public String equalsExceptIdAndPrice(Contract c) {
			StringBuilder sb = new StringBuilder();
			// if (c.getAssetCount() != assetCount) sb.append("assetCount ");
			if (!c.getStartDate().equals(startDate))
				sb.append("startDate ");
			// When fully on Java 7 use !Objects.equals(...) from JDK instead of ObjectUtils from Apache Commons
			// - will fix these deprecation warnings
			if (!ObjectUtils.equals(c.getEndDate(), endDate))
				sb.append("endDate ");
			if (!ObjectUtils.equals(c.getContractType(), contractType))
				sb.append("contractType ");
			if (c.isPermissionForm() != isPermissionForm)
				sb.append("isPermissionForm ");
			if (!c.getDate().equals(date))
				sb.append("date ");
			if (!c.getCurrency().equals(currency))
				sb.append("currency ");
			if (getPoId() != poId)
				sb.append("poId ");
			if (!ObjectUtils.equals(c.getPaymentType(), paymentType))
				sb.append("paymentType ");
			if (!ObjectUtils.equals(c.getPaymentDate(), paymentDate))
				sb.append("paymentDate ");

			if (c.getConditions().size() != conditions.size()) {
				sb.append("conditions.size[");
				// figure out what condition type are not in both sets
				HashSet<String> set1 = new HashSet<String>();
				HashSet<String> set3 = new HashSet<String>();
				HashSet<String> set2 = new HashSet<String>();
				for (Condition cond : conditions) {
					set1.add(cond.getConditionType());
					set3.add(cond.getConditionType());
				}
				for (Condition cond : c.getConditions()) {
					set2.add(cond.getConditionType());
				}
				set1.removeAll(set2);
				set2.removeAll(set3);
				set3.clear();
				set3.addAll(set1);
				set3.addAll(set2);
				for (String s : set3) {
					sb.append(s);
					sb.append(" ");
				}
				sb.append("] ");
			} else {
				for (int i = 0; i < conditions.size(); i++) {
					Condition c1 = conditions.get(i);
					Condition c2 = c.getConditions().get(i);
					if (!c1.equals(c2)) {
						sb.append("conditions[" + c1.getConditionType() + "] ");
					}
				}
			}

			return sb.toString();
		}
	}

	class Condition {
		private final String conditionType;
		private final String value;
		private final int id;

		public Condition(String conditionType, String value, int id) {
			this.conditionType = conditionType;
			this.value = value;
			this.id = id;
		}

		@Override
		public String toString() {
			return "conditionType [" + conditionType + "] value [" + value
					+ "]";
		}

		public String getConditionType() {
			return conditionType;
		}

		public String getValue() {
			return value;
		}

		public int getId() {
			return id;
		}

		// don't look at id here
		@Override
		public boolean equals(Object o) {
			if (o == null || !(o instanceof Condition))
				return false;
			Condition c = (Condition) o;
			if (!c.getConditionType().equals(conditionType))
				return false;
			if (!StringUtils.equalsIgnoreCase(c.getValue(), value))
				return false;

			return true;
		}
	}
}
