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
import java.util.HashMap;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.sf.common.lang.StringUtil;
import com.wiley.sf.common.sql.SimpleDBConnect;
import com.wiley.sf.common.text.TimeFormat;

/**
 * This program takes care of the situation where there are Asset Uses that are
 * canceled but not deleted because there is a Contract that covers them,
 * BUT they really should be deleted because there is a similar Asset Use which
 * is not canceled and covered by a similar Contract.
 * It deletes the contract that covers the canceled Asset Use and the Asset Use
 * as long as a similar Asset Use exists which is in some granted status.
 * Also checks if the contract to be deleted has a file and if so makes
 * sure that another contract for the same common work has a file of the
 * same size in bytes (so likely the same file).
 * Checks to see if the Asset should also be deleted but in most of these
 * situations the Asset is reused by the non-canceled Asset Use.
 * 
 * TODO: Review and see if need to update this program with regards to the
 * new replacement option (for cancelled asset uses).
 *
 * @since JDK 1.6
 * @version 2/10/2014
 * @author Steve Markoff
 */
public class CleanCanceledAssetUses {

	private static final Log log = LogFactory.getLog(CleanCanceledAssetUses.class);

	private static final NumberFormat intFormat = NumberFormat.getIntegerInstance();

	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			System.err.println("Usage: <properties file>");
			System.exit(1);
		}

		SimpleDBConnect dbConnect = SimpleDBConnect.create(args[0]);
		// throws various exceptions
		CleanCanceledAssetUses prog = new CleanCanceledAssetUses(
				dbConnect.getConnection(), true);
		prog.go();
	}

	private final Connection con;
	private final boolean logToSystemOut;
	private int totalCount = 0;
	private int anotherSameAssetCount = 0;
	private int anotherDifferentAssetCount = 0;
	private int contractDeleteCount = 0;
	private int poDeleteCount = 0;
	private int auDeleteCount = 0;
	private int assetDeleteCount = 0;
	//private final boolean makeChanges = true;

	private final HashMap<Integer, Integer> cwIdsAffected = new HashMap<Integer, Integer>();


	public CleanCanceledAssetUses(Connection con, boolean logToSystemOut) throws SQLException {
		this.con = con;
		this.logToSystemOut = logToSystemOut;
		con.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
		con.setAutoCommit(true);
	}

	public void go() throws SQLException, IOException {
		long startTime = System.currentTimeMillis();

		List<AssetUseWithAsset> list = getCanceledAssetUses();
		log("found " + intFormat.format(list.size()) + " canceled asset uses.");

		for (int i = 0; i < list.size(); i++) {
			AssetUseWithAsset auwa = list.get(i);
			process(auwa, i);
			if (i % 1000 == 999) {
				log("--- i = " + i);
				logCounts();
			}
		}

		long time = System.currentTimeMillis() - startTime;
		TimeFormat timeFormat = new TimeFormat();
		log("total time was " + timeFormat.formatMS(time));

		logCounts();
	}

	private void logCounts() {
		log("totalCount: " + intFormat.format(totalCount));
		log("anotherSameAssetCount: " + intFormat.format(anotherSameAssetCount));
		log("anotherDifferentAssetCount: " + intFormat.format(anotherDifferentAssetCount));
		log("contractDeleteCount: " + intFormat.format(contractDeleteCount));
		log("poDeleteCount: " + intFormat.format(poDeleteCount));
		log("auDeleteCount: " + intFormat.format(auDeleteCount));
		log("assetDeleteCount: " + intFormat.format(assetDeleteCount));
		log("cwIdsAffected.size() = " + cwIdsAffected.size());
		log("cwIdsAffected: " + StringUtil.collectionToString(cwIdsAffected.keySet(), " "));
		log("cwCounts: " + StringUtil.mapToString(cwIdsAffected, ", "));
	}

	private void process(AssetUseWithAsset auwa, int i) throws SQLException, IOException {
		totalCount++;
		AssetUseWithAsset another = anotherAssetUseNotCanceled(auwa.getCwId(), auwa.getDescription(), auwa.getPosition());

		if (another == null) return;

		if (another.getAssetId() == auwa.getAssetId()) {
			anotherSameAssetCount++;
			log("---- two Asset Uses with same asset:\r\n" + auwa + "\r\n" + another);

			List<Contract> contractList = getContracts(auwa.getAssetId(), auwa.getCwId());
			log("# contracts for this asset and common work = " + contractList.size());
			if (contractList.size() > 1) {
				processContracts(contractList);
			}
			deleteAssetUse(auwa);
		}
		else {
			anotherDifferentAssetCount++;
			log("---- two Asset Uses with different asset:\r\n" + auwa + "\r\n" + another);
			deleteAssetUse(auwa);
			if (!isAssetRefByAnyAssetUse(auwa.getAssetId())) {
				deleteAssetCheckContracts(auwa.getAssetId(), another.getAssetId());
			}
		}
	}

	private List<Contract> getContracts(int assetId, int cwId) throws SQLException {
		String sql = "select c.id, c.cw_id, c.source_id, c.date from contract c, contract_2_asset c2a"
			+ " where c.id = c2a.contract_id and c2a.asset_base_id = ? and c.cw_id = ? order by c.date";
		PreparedStatement ps = con.prepareStatement(sql);
		ps.setInt(1, assetId);
		ps.setInt(2, cwId);
		ResultSet rs = ps.executeQuery();
		List<Contract> list = new ArrayList<Contract>();
		while (rs.next()) {
			list.add(new Contract(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getTimestamp(4)));
		}
		ps.close();
		return list;
	}

	private void processContracts(List<Contract> contracts) throws SQLException, IOException {
		log("number of contracts: " + contracts.size());
		int sourceId = 0;
		for (Contract contract : contracts) {
			if (sourceId == 0)  sourceId = contract.getSourceId();
			else {
				if (sourceId != contract.getSourceId()) {
					log("one contract was different source");
					return;
				}
			}
		}

		int fileSize = fileSize(contracts.get(0).getId());
		if (fileSize > 0) {
			if (oneContractHasFileOfSize(contracts.subList(1, contracts.size()), fileSize)) {
				log("contract has file and another contract has file of same size");
				deleteContract(contracts.get(0).getId());
			}
			else {
				log("no other contract file of same size [" + fileSize + "] so won't delete");
			}
		}
		else deleteContract(contracts.get(0).getId());
	}

	private List<AssetUseWithAsset> getCanceledAssetUses() throws SQLException {
		String sql = "select au.id, a.id, au.cw_id, a.description, au.position, au.created_date from asset_use au, asset a"
			+ " where au.asset_id = a.id and au.permission_status = 'canceled' order by cw_id";
		PreparedStatement ps = con.prepareStatement(sql);
		ResultSet rs = ps.executeQuery();
		List<AssetUseWithAsset> list = new ArrayList<AssetUseWithAsset>();
		while (rs.next()) {
			list.add(new AssetUseWithAsset(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getString(4), rs.getString(5), "canceled", rs.getTimestamp(6)));
		}
		ps.close();
		return list;
	}

	private AssetUseWithAsset anotherAssetUseNotCanceled(int cwId, String description, String position) throws SQLException {
		String sql = "select au.id, a.id, au.permission_status, au.created_date from asset_use au, asset a"
			+ " where au.asset_id = a.id and au.cw_id = ? and au.permission_status != 'canceled'"
			+ " and a.description = ? and au.position = ?";
		PreparedStatement ps = con.prepareStatement(sql);
		ps.setInt(1, cwId);
		ps.setString(2, description);
		ps.setString(3, position);
		ResultSet rs = ps.executeQuery();
		AssetUseWithAsset auwa = null;
		if (rs.next()) {
			auwa = new AssetUseWithAsset(rs.getInt(1), rs.getInt(2), cwId, description, position, rs.getString(3), rs.getTimestamp(4));
		}
		ps.close();
		return auwa;
	}

	private boolean promptYes(String msg) throws IOException {
		System.out.print(msg + " (y/n) > ");
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String line = br.readLine();
		return line.startsWith("y");
	}

	private int countContractAssets(int contractId) throws SQLException {
		String sql = "select count(*) from contract_2_asset where contract_id = ?";
		PreparedStatement ps = con.prepareStatement(sql);
		ps.setInt(1, contractId);
		ResultSet rs = ps.executeQuery();
		rs.next();
		int result = rs.getInt(1);
		ps.close();
		return result;
	}

	private int countPOAssets(int poId) throws SQLException {
		String sql = "select count(*) from purchase_order_2_asset where purchase_order_id = ?";
		PreparedStatement ps = con.prepareStatement(sql);
		ps.setInt(1, poId);
		ResultSet rs = ps.executeQuery();
		rs.next();
		int result = rs.getInt(1);
		ps.close();
		return result;
	}

	private void deleteContract(int contractId) throws SQLException, IOException {
		int count = countContractAssets(contractId);
		if (count > 1) {
			log("won't delete contract because has more than one asset");
			return;
		}

		if (!promptYes("delete contractId = " + contractId))  return;
		con.setAutoCommit(false);

		try {
			String sql = "delete from contract_2_condition where contract_id = ?";
			PreparedStatement ps = con.prepareStatement(sql);
			ps.setInt(1, contractId);
			int rows = ps.executeUpdate();
			log("deleted " + rows + " contract conditions");
			ps.close();

			sql = "delete from contract_2_asset where contract_id = ?";
			ps = con.prepareStatement(sql);
			ps.setInt(1, contractId);
			rows = ps.executeUpdate();
			log("deleted " + rows + " rows from contract_2_asset");
			ps.close();

			sql = "delete from contract_file where contract_id = ?";
			ps = con.prepareStatement(sql);
			ps.setInt(1, contractId);
			rows = ps.executeUpdate();
			log("deleted " + rows + " contract files");
			ps.close();

			sql = "delete from amendment where contract_id = ?";
			ps = con.prepareStatement(sql);
			ps.setInt(1, contractId);
			rows = ps.executeUpdate();
			log("deleted " + rows + " amendments");
			ps.close();

			sql = "delete from contract where id = ?";
			ps = con.prepareStatement(sql);
			ps.setInt(1, contractId);
			rows = ps.executeUpdate();
			log("deleted " + rows + " contracts");
			ps.close();

			contractDeleteCount++;
		}
		catch (SQLException ex) {
			log("Caught SQLException: " + ex);
			con.rollback();
		}

		con.commit();
		con.setAutoCommit(true);
	}

	private void deletePO(int poId) throws SQLException, IOException {
		// this method assumes that the PO has no assets
		con.setAutoCommit(false);

		try {
			// POs no longer have conditions so don't worry about that

			String sql = "delete from purchase_order where id = ?";
			PreparedStatement ps = con.prepareStatement(sql);
			ps.setInt(1, poId);
			int rows = ps.executeUpdate();
			log("deleted " + rows + " pos");
			ps.close();

			poDeleteCount++;
		}
		catch (SQLException ex) {
			log("Caught SQLException: " + ex);
			con.rollback();
		}

		con.commit();
		con.setAutoCommit(true);
	}

	private void deleteAssetUse(AssetUseWithAsset auwa) throws SQLException, IOException {
		//if (!promptYes("delete auId = " + auId))  return;
		Integer count = cwIdsAffected.get(auwa.getCwId());
		if (count == null)  count = new Integer(1);
		else count++;
		cwIdsAffected.put(auwa.getCwId(), count);
		int auId = auwa.getAuId();

		con.setAutoCommit(false);

		try {
			String sql = "delete from asset_perm_ref where asset_use_id = ?";
			PreparedStatement ps = con.prepareStatement(sql);
			ps.setInt(1, auId);
			int rows = ps.executeUpdate();
			log("deleted " + rows + " asset_perm_ref rows");
			ps.close();

			sql = "delete from au_source_perm_status where asset_use_id = ?";
			ps = con.prepareStatement(sql);
			ps.setInt(1, auId);
			rows = ps.executeUpdate();
			log("deleted " + rows + " au_source_perm_status rows");
			ps.close();

			sql = "delete from asset_use where id = ?";
			ps = con.prepareStatement(sql);
			ps.setInt(1, auId);
			rows = ps.executeUpdate();
			log("deleted " + rows + " asset_use rows");
			ps.close();

			auDeleteCount++;
		}
		catch (SQLException ex) {
			log("Caught SQLException: " + ex);
			con.rollback();
		}

		con.commit();
		con.setAutoCommit(true);
	}

	private int fileSize(int contractId) throws SQLException {
		String sql = "select id, length(file_data) from contract_file where contract_id = ?";
		PreparedStatement ps = con.prepareStatement(sql);
		ps.setInt(1, contractId);
		ResultSet rs = ps.executeQuery();
		int size = 0;
		if (rs.next()) {
			size = rs.getInt(2);
		}
		ps.close();
		return size;
	}

	private int assetFileSize(int assetId) throws SQLException {
		String sql = "select id, length(data) from asset_file where asset_id = ?";
		PreparedStatement ps = con.prepareStatement(sql);
		ps.setInt(1, assetId);
		ResultSet rs = ps.executeQuery();
		int size = 0;
		if (rs.next()) {
			size = rs.getInt(2);
		}
		ps.close();
		return size;
	}

	private boolean oneContractHasFileOfSize(List<Contract> list, int fileSize) throws SQLException {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < list.size(); i++) {
			Contract c = list.get(i);
			sb.append(c.getId());
			if (i < list.size() - 1) sb.append(", ");
		}
		String sql = "select id from contract_file where contract_id in (" + sb + ") and length(file_data) = " + fileSize;
		PreparedStatement ps = con.prepareStatement(sql);
		ResultSet rs = ps.executeQuery();
		boolean hasRow = rs.next();
		ps.close();
		return hasRow;
	}

	private boolean isAssetRefByAnyAssetUse(int assetId) throws SQLException {
		String sql = "select id from asset_use where asset_id = ?";
		PreparedStatement ps = con.prepareStatement(sql);
		ps.setInt(1, assetId);
		ResultSet rs = ps.executeQuery();
		boolean hasRow = rs.next();
		ps.close();
		return hasRow;
	}

	private void deleteAssetCheckContracts(int assetId, int anotherAssetId) throws SQLException, IOException {
		ArrayList<Integer> contractIds = new ArrayList<Integer>();
		String sql = "select contract_id from contract_2_asset where asset_base_id = ?";
		PreparedStatement ps = con.prepareStatement(sql);
		ps.setInt(1, assetId);
		ResultSet rs = ps.executeQuery();
		while (rs.next()) {
			contractIds.add(rs.getInt(1));
		}
		ps.close();

		ArrayList<Integer> poIds = new ArrayList<Integer>();
		sql = "select purchase_order_id from purchase_order_2_asset where asset_base_id = ?";
		ps = con.prepareStatement(sql);
		ps.setInt(1, assetId);
		rs = ps.executeQuery();
		while (rs.next()) {
			poIds.add(rs.getInt(1));
		}
		ps.close();

		if (assetFileSize(assetId) > 0) {
			if (!promptYes("delete assetFile for assetId " + assetId + "(manually check if asset " + anotherAssetId + " has same files)"))  return;
		}

		con.setAutoCommit(false);

		try {
			sql = "delete from asset_file where asset_id = ?";
			ps = con.prepareStatement(sql);
			ps.setInt(1, assetId);
			int rows = ps.executeUpdate();
			log("deleted " + rows + " rows from asset_file");
			ps.close();

			sql = "delete from contract_2_asset where asset_base_id = ?";
			ps = con.prepareStatement(sql);
			ps.setInt(1, assetId);
			rows = ps.executeUpdate();
			log("deleted " + rows + " rows from contract_2_asset");
			ps.close();

			sql = "delete from purchase_order_2_asset where asset_base_id = ?";
			ps = con.prepareStatement(sql);
			ps.setInt(1, assetId);
			rows = ps.executeUpdate();
			log("deleted " + rows + " rows from purchase_order_2_asset");
			ps.close();

			sql = "delete from asset_2_source where asset_id = ?";
			ps = con.prepareStatement(sql);
			ps.setInt(1, assetId);
			rows = ps.executeUpdate();
			log("deleted " + rows + " rows from asset_2_source");
			ps.close();

			sql = "delete from asset where id = ?";
			ps = con.prepareStatement(sql);
			ps.setInt(1, assetId);
			rows = ps.executeUpdate();
			log("deleted " + rows + " rows from asset");
			ps.close();

			sql = "delete from asset_base where id = ?";
			ps = con.prepareStatement(sql);
			ps.setInt(1, assetId);
			rows = ps.executeUpdate();
			log("deleted " + rows + " rows from asset_base");
			ps.close();

			assetDeleteCount++;
		}
		catch (SQLException ex) {
			log("Caught SQLException: " + ex);
			con.rollback();
		}

		con.commit();
		con.setAutoCommit(true);

		// delete contracts before POs since contract may reference PO

		for (Integer cId : contractIds) {
			if (countContractAssets(cId) == 0) {
				if (fileSize(cId) > 0) {
					if (!promptYes("delete file for contract id "
						+ cId + " (manually compare to contract for anotherAssetId " + anotherAssetId + ")"))  continue;
				}

				deleteContract(cId);
			}
		}

		for (Integer poId : poIds) {
			if (countPOAssets(poId) == 0) {
				deletePO(poId);
			}
		}
	}

	private void log(String msg) {
		if (logToSystemOut) {
			System.out.println(msg);
		}
		else {
			log.info(msg);
		}
	}

	class AssetUseWithAsset {
		private final int auId;
		private final int assetId;
		private final int cwId;
		private final String description;
		private final String position;
		private final String status;
		private final Timestamp auCreatedDate;

		public AssetUseWithAsset(int auId, int assetId, int cwId, String description, String position, String status, Timestamp auCreatedDate) {
			this.auId = auId;
			this.assetId = assetId;
			this.cwId = cwId;
			this.description = description;
			this.position = position;
			this.status = status;
			this.auCreatedDate = auCreatedDate;
		}

		public int getAuId() { return auId; }
		public int getAssetId() { return assetId; }
		public int getCwId() { return cwId; }
		public String getDescription() { return description; }
		public String getPosition() { return position; }
		public String getStatus() { return status; }
		public Timestamp getAuCreatedDate() { return auCreatedDate; }

		@Override
		public String toString() {
			return "auId [" + auId + "] assetId [" + assetId + "] cwId [" + cwId
				+ "] description [" + description + "] position [" + position
				+ "] status [" + status + "] auCreatedDate [" + auCreatedDate + "]";
		}
	}

	class Contract {
		private final int id;
		private final int cwId;
		private final int sourceId;
		private final Timestamp timestamp;

		public Contract(int id, int cwId, int sourceId, Timestamp timestamp) {
			this.id = id;
			this.cwId = cwId;
			this.sourceId = sourceId;
			this.timestamp = timestamp;
		}

		@Override
		public String toString() {
			return "id [" + id + "] cwId [" + cwId + "] sourceId [" + sourceId + "]";
		}

		public int getId() { return id; }
		public int getCwId() { return cwId; }
		public int getSourceId() { return sourceId; }
		public Timestamp getTimestamp() { return timestamp; }
	}
}
