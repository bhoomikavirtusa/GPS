package com.wiley.permissions.common.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;

import com.wiley.sf.common.sql.SimpleDBConnect;

/**
* Program to fix temporary data problem.
*
* @since   JDK 1.6
* @version 9/26/2012
* @author  Steve Markoff
*/
public class FixDuplicateConditions {

	// Another handy SQL to run to see duplicates easily:
	// select count(*) as count, condition_type, contract_id, asset_base_id from view_contract_condition
	//   group by condition_type, contract_id, asset_base_id order by count desc;
	// - same for view_po_condition, etc.

	// Also here is a check for unreferenced conditions:
	//select * from condition_value where id not in (select condition_id from contract_2_condition)
	//  and id not in (select condition_id from po_2_condition)
	//  and id not in (select condition_id from cw_2_condition)
	//  and id not in (select condition_id from ma_deal_2_condition)
	//  and id not in (select condition_id from user_2_condition)

	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			System.err.println("Usage: <properties file>");
			System.exit(1);
		}

		SimpleDBConnect dbConnect = SimpleDBConnect.create(args[0]);
			// throws various exceptions
		FixDuplicateConditions prog = new FixDuplicateConditions( dbConnect.getConnection());
		prog.go();
	}

	private final Connection con;
	private final boolean makeChanges = true;
	private int deleteCount = 0;

	public FixDuplicateConditions(Connection con) throws SQLException {
		this.con = con;
		con.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
		con.setAutoCommit(true);
	}

	public void go() throws SQLException {
		doObject("contract_id", "view_contract_condition", "contract_2_condition");
		doObject("po_id", "view_po_condition", "po_2_condition");
		// I think we only have global conditions for cw and user but it doesn't matter
		// -- will still find duplicates properly
		doObject("cw_id", "view_cw_condition", "cw_2_condition");
		doObject("user_id", "view_user_condition", "user_2_condition");
	}

	public void doObject(String objectIdName, String viewName, String mapTable) throws SQLException {
		deleteCount = 0;
		ArrayList<ObjectCondition> conditionList = getObjectConditions(objectIdName, viewName);
		NumberFormat intFormat = NumberFormat.getIntegerInstance();
		System.out.println("---- number of " + viewName + " conditions = " + intFormat.format(conditionList.size()));
		HashSet<String> set = new HashSet<String>();

		for (ObjectCondition c : conditionList) {
			String code = c.getObjectId() + "@" + c.getConditionType() + "@" + c.getAssetBaseId();
			if (set.contains(code)) {
				delete(c.getConditionId(), mapTable);
			}
			else {
				set.add(code);
			}
		}

		System.out.println("deleteCount = " + intFormat.format(deleteCount));
	}

	private void delete(int conditionId, String mapTable) throws SQLException {
		String sql = "delete from " + mapTable + " where condition_id = ?";
		String printSql = sql.replace("?", "[? = " + conditionId + "]");
		System.out.println((makeChanges ? "running: " : "would run: ") + printSql);
		if (makeChanges) {
			PreparedStatement ps = con.prepareStatement(sql);
			ps.setInt(1, conditionId);
			int numRows = ps.executeUpdate();
			if (numRows != 1) {
				System.out.println("numRows expected to be 1 but was " + numRows);
			}
			ps.close();
		}

		sql = "delete from condition_value where id = ?";
		printSql = sql.replace("?", "[? = " + conditionId + "]");
		System.out.println((makeChanges ? "running: " : "would run: ") + printSql);
		if (makeChanges) {
			PreparedStatement ps = con.prepareStatement(sql);
			ps.setInt(1, conditionId);
			int numRows = ps.executeUpdate();
			if (numRows != 1) {
				System.out.println("numRows expected to be 1 but was " + numRows);
			}
			ps.close();
		}

		deleteCount++;
	}

    private ArrayList<ObjectCondition> getObjectConditions(String objectIdName, String viewName) throws SQLException {
    	String sql = "select " + objectIdName + ", id, condition_type, asset_base_id from "
    		+ viewName + " order by " + objectIdName + ", condition_type, id";
    	PreparedStatement ps = con.prepareStatement(sql);

    	ResultSet rs = ps.executeQuery();
    	ArrayList<ObjectCondition> conditionList = new ArrayList<ObjectCondition>();

    	while (rs.next()) {
    		int objectId = rs.getInt(1);
    		int conditionId = rs.getInt(2);
    		String conditionType = rs.getString(3);
    		Integer assetBaseId = rs.getInt(4);
    		if (assetBaseId == 0)  assetBaseId = null;
    		conditionList.add(new ObjectCondition(objectId, conditionId, conditionType, assetBaseId));
    	}
    	ps.close();
    	return conditionList;
    }
}

class ObjectCondition {
	private final int objectId;
	private final int conditionId;
	private final String conditionType;
	private final Integer assetBaseId;

	public ObjectCondition(int objectId, int conditionId, String conditionType, Integer assetBaseId) {
		this.objectId = objectId;
		this.conditionId = conditionId;
		this.conditionType = conditionType;
		this.assetBaseId = assetBaseId;
	}

	public int getObjectId() { return objectId; }
	public int getConditionId() { return conditionId; }
	public String getConditionType() { return conditionType; }
	public Integer getAssetBaseId() { return assetBaseId; }

	@Override
	public String toString() {
		return "objectId = " + objectId + ", conditionId = " + conditionId
			+ ", conditionType = " + conditionType + ", assetBaseId = " + assetBaseId;
	}
}