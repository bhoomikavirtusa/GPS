package com.wiley.permissions.common.utils;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.sf.common.sql.SimpleDBConnect;

/**
 * This program should be used after adding a new condition_type that is underneath a parent
 * such as "all sizes". It adds the new child where ever the parent is found and true.
 *
 * @since JDK 1.6
 * @version 11/27/2012
 * @author Steve Markoff
 */
public class AddConditionType {

	private static final Log log = LogFactory.getLog(AddConditionType.class);

	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			System.err.println("Usage: <properties file>");
			System.exit(1);
		}

		SimpleDBConnect dbConnect = SimpleDBConnect.create(args[0]);
		// throws various exceptions
		AddConditionType prog = new AddConditionType(
				dbConnect.getConnection(), true);
		prog.go("usage_repo_size_18", "usage_repo_all_size");

		prog.go("usage_repo_case_art", "usage_repo_all_use");
		prog.go("usage_repo_cdrom", "usage_repo_all_use");
		prog.go("usage_repo_endpapers", "usage_repo_all_use");
		prog.go("usage_repo_figure", "usage_repo_all_use");
		prog.go("usage_repo_flaps", "usage_repo_all_use");
		prog.go("usage_repo_front_mat", "usage_repo_all_use");
	}

	private final Connection con;
	private final boolean logToSystemOut;
	private final boolean makeChanges = true;

	public AddConditionType(Connection con, boolean logToSystemOut) throws SQLException {
		this.con = con;
		this.logToSystemOut = logToSystemOut;
		con.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
		con.setAutoCommit(true);
	}

	public void go(String childCode, String parentCode) throws SQLException, IOException {
		List<Summary> summaryList = getSummaryList(parentCode);

		for (int i = 0; i < summaryList.size(); i++) {
			Summary summary = summaryList.get(i);
			boolean exists = exists(childCode, summary);
			log("childCode [" + childCode + "] summary " + summary + " exists = " + exists);
			if (!exists && makeChanges) {
				add(childCode, summary);
			}
		}
	}

	private List<Summary> getSummaryList(String parentCode) throws SQLException {
		String sql = "select contract_id, asset_base_id from view_contract_condition where condition_type = ? and value = 'true'";
		PreparedStatement ps = con.prepareStatement(sql);
		ps.setString(1, parentCode);
		ResultSet rs = ps.executeQuery();
		List<Summary> list = new ArrayList<Summary>();
		while (rs.next()) {
			int contractId = rs.getInt(1);
			Integer assetId = rs.getInt(2);
			if (assetId == 0) assetId = null;
			list.add(new Summary(contractId, assetId));
		}
		ps.close();
		return list;
	}

	private boolean exists(String childCode, Summary summary) throws SQLException {
		String sql = "select count(*) from view_contract_condition where condition_type = ? and contract_id = ?"
			+ (summary.getAssetId() == null ? " and asset_base_id is null" : " and asset_base_id = ?");
		PreparedStatement ps = con.prepareStatement(sql);
		ps.setString(1, childCode);
		ps.setInt(2, summary.getContractId());
		if (summary.getAssetId() != null) {
			ps.setInt(3, summary.getAssetId());
		}
		ResultSet rs = ps.executeQuery();
		rs.next();
		int count = rs.getInt(1);
		ps.close();
		return count > 0;
	}

	private void add(String childCode, Summary summary) throws SQLException {
		String sql = "insert into condition_value (condition_type, asset_base_id, value) values (?, ?, 'true')";
		PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
		ps.setString(1, childCode);
		if (summary.getAssetId() == null) {
			ps.setNull(2, Types.INTEGER);
		}
		else {
			ps.setInt(2, summary.getAssetId());
		}
		int rows = ps.executeUpdate();
		if (rows != 1) {
			throw new RuntimeException("add() part 1: number of inserted rows expected to be 1, childCode = "
				+ childCode + ", summary = " + summary);
		}
		ResultSet genKeys = ps.getGeneratedKeys();
		genKeys.next();
		int genId = genKeys.getInt(1);
		genKeys.close();
		ps.close();
		log("generated condition id = " + genId);

		sql = "insert into contract_2_condition (contract_id, condition_id) values (?, ?)";
		ps = con.prepareStatement(sql);
		ps.setInt(1, summary.getContractId());
		ps.setInt(2, genId);
		rows = ps.executeUpdate();
		if (rows != 1) {
			throw new RuntimeException("add() part 2: number of inserted rows expected to be 1, childCode = "
				+ childCode + ", summary = " + summary);
		}
		ps.close();
	}

	private void log(String msg) {
		if (logToSystemOut) {
			System.out.println(msg);
		}
		else {
			log.info(msg);
		}
	}

	class Summary {
		private final int contractId;
		private final Integer assetId;

		public Summary(int contractId, Integer assetId) {
			this.contractId = contractId;
			this.assetId = assetId;
		}

		public int getContractId() { return contractId; }
		public Integer getAssetId() { return assetId; }

		@Override
		public String toString() {
			return "(" + contractId + ", " + assetId + ")";
		}
	}
}
