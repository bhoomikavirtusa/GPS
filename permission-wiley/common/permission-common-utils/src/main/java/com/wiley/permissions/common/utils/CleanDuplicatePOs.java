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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.sf.common.lang.StringUtil;
import com.wiley.sf.common.sql.SimpleDBConnect;
import com.wiley.sf.common.text.TimeFormat;

/**
 * This program takes care of the situation where there are multiple similar purchase orders
 * that cover the same assets within a common work.
 * Generally the user did not know what they were doing and created a new purchase order
 * to replace an existing one instead of just editing the existing one.
 * So delete the existing old purchase order, as long as important fields
 * are not different (and covers the same assets).
 *
 * @since JDK 1.6
 * @version 10/23/2013
 * @author Steve Markoff
 */
public class CleanDuplicatePOs {

	private static final Log log = LogFactory.getLog(CleanDuplicatePOs.class);

	private static final NumberFormat intFormat = NumberFormat.getIntegerInstance();

	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			System.err.println("Usage: <properties file> [cwId]");
			System.exit(1);
		}

		SimpleDBConnect dbConnect = SimpleDBConnect.create(args[0]);
		// throws various exceptions

		int cwId = 0;
		if (args.length > 1) {
			cwId = Integer.parseInt(args[1]);
		}
		CleanDuplicatePOs prog = new CleanDuplicatePOs(
				dbConnect.getConnection(), true, cwId);
		prog.go();
	}

	private final Connection con;
	private final boolean logToSystemOut;
	private final int cwId;
	private final int totalCount = 0;
	private int poDeleteCount = 0;
	private int poDeleteErrorCount = 0;
	private int poRefByContractCount = 0;
	private int assetsNotSameCount = 0;
	//private final boolean makeChanges = true;

	private final HashMap<Integer, Integer> cwIdsAffected = new HashMap<Integer, Integer>();

	/**
	 * @param cwId  0 means all common works
	 */
	public CleanDuplicatePOs(Connection con, boolean logToSystemOut, int cwId) throws SQLException {
		this.con = con;
		this.logToSystemOut = logToSystemOut;
		this.cwId = cwId;
		con.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
		con.setAutoCommit(true);
	}

	public void go() throws SQLException, IOException {
		long startTime = System.currentTimeMillis();

		List<AssetWithCount> list = getCounts(cwId);
		log("count list size = " + intFormat.format(list.size()));

		for (int i = 0; i < list.size(); i++) {
			AssetWithCount assetWithCount = list.get(i);
			process(assetWithCount, i);
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
		log("poDeleteCount: " + intFormat.format(poDeleteCount));
		log("poDeleteErrorCount: " + intFormat.format(poDeleteErrorCount));
		log("poRefByContractCount: " + intFormat.format(poRefByContractCount));
		log("assetsNotSameCount: " + intFormat.format(assetsNotSameCount));
		log("cwIdsAffected.size() = " + cwIdsAffected.size());
		log("cwIdsAffected: " + StringUtil.collectionToString(cwIdsAffected.keySet(), " "));
		log("cwCounts: " + StringUtil.mapToString(cwIdsAffected, ", "));
	}

	private void process(AssetWithCount awc, int i) throws SQLException, IOException {
		log("---- looking at: " + awc);
		List<PurchaseOrder> pos = getPOs(awc.getAssetId(), awc.getCwId(), awc.getSourceId());

		PurchaseOrder firstPO = pos.get(0);
		Set<Integer> firstAssetIds = getPOAssets(firstPO.getId());

		PurchaseOrder lastPO = firstPO;
		Set<Integer> lastAssetIds = firstAssetIds;
		for (PurchaseOrder po : pos) {
			log(po.toString());

			if (po.getId() == firstPO.getId()) {
				continue;
			}

			Set<Integer> assetIds = getPOAssets(po.getId());
			if (!assetIds.equals(lastAssetIds)) {
				log("po with id [" + po.getId() + "] does not have same assets so skipping\r\n"
					+ "    current (" + StringUtils.join(assetIds, " ") + ") last (" + StringUtils.join(lastAssetIds, " ") + ")");
				assetsNotSameCount++;
				continue;
			}

			String diff = lastPO.importantFieldsAreSame(po);
			if (diff.isEmpty() || (!diff.contains("estimatedPrice ")
					&& promptYes("delete purchase order with id [" + po.getId() + "] (diff fields: " + diff + ")"))) {
				deletePO(po.getId());
			}

			lastPO = po;
			lastAssetIds = firstAssetIds;
		}
	}

	private List<PurchaseOrder> getPOs(int assetId, int cwId, int sourceId) throws SQLException {
		String sql = "select id, last_updated_date, last_updated_user_id, date,"
			+ " is_permission_request, note, return_address, user_signature, is_outside_record,"
			+ " estimated_price, estimated_currency, show_estimated_cost from purchase_order"
			+ " where id in (select purchase_order_id from purchase_order_2_asset where asset_base_id = ?)"
			+ " and cw_id = ? and source_id = ? order by date desc, id desc";
		PreparedStatement ps = con.prepareStatement(sql);
		ps.setInt(1, assetId);
		ps.setInt(2, cwId);
		ps.setInt(3, sourceId);
		ResultSet rs = ps.executeQuery();
		List<PurchaseOrder> list = new ArrayList<PurchaseOrder>();
		while (rs.next()) {
			list.add(new PurchaseOrder(rs.getInt(1), rs.getTimestamp(2), rs.getInt(3), rs.getTimestamp(4),
					rs.getBoolean(5), rs.getString(6), rs.getString(7), rs.getString(8), rs.getBoolean(9),
					rs.getDouble(10), rs.getString(11), rs.getBoolean(12)));
		}
		ps.close();
		return list;
	}

	/**
	 * @param cwId  0 means all common works
	 */
	private List<AssetWithCount> getCounts(int cwId) throws SQLException {
		String sql = "select count(*) as count, asset_base_id, cw_id, source_id from purchase_order_2_asset po2a, purchase_order po"
			+ " where po.id = po2a.purchase_order_id"
			+ (cwId > 0 ? " and cw_id = ?" : "")
			+ " group by asset_base_id, cw_id, source_id having count(*) > 1 order by count desc, cw_id desc";
		PreparedStatement ps = con.prepareStatement(sql);
		if (cwId > 0) {
			ps.setInt(1, cwId);
		}
		ResultSet rs = ps.executeQuery();
		List<AssetWithCount> list = new ArrayList<AssetWithCount>();
		while (rs.next()) {
			list.add(new AssetWithCount(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getInt(4)));
		}
		ps.close();
		return list;
	}

	private boolean promptYes(String msg) throws IOException {
		System.out.print(msg + " (y/n) > ");
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String line = br.readLine();
		return line.startsWith("y");
	}

	private Set<Integer> getPOAssets(int poId) throws SQLException {
		String sql = "select asset_base_id from purchase_order_2_asset where purchase_order_id = ?";
		PreparedStatement ps = con.prepareStatement(sql);
		ps.setInt(1, poId);
		ResultSet rs = ps.executeQuery();
		HashSet<Integer> set = new HashSet<Integer>();
		while (rs.next()) {
			set.add(rs.getInt(1));
		}
		ps.close();
		return set;
	}

	private int referencedByContract(int poId) throws SQLException {
		String sql = "select id from contract where purchase_order_id = ?";
		PreparedStatement ps = con.prepareStatement(sql);
		ps.setInt(1, poId);
		ResultSet rs = ps.executeQuery();
		int result = 0;
		if (rs.next()) {
			result = rs.getInt(1);
		}
		ps.close();
		return result;
	}

	private void deletePO(int poId) throws SQLException, IOException {
		int contractId = referencedByContract(poId);
		if (contractId != 0) {
			log("won't delete PO id [" + poId + "] because referenced by contract id [" + contractId + "]");
			poRefByContractCount++;
			return;
		}

		con.setAutoCommit(false);

		try {
			// POs no longer have conditions so skip that

			String sql = "delete from purchase_order_2_asset where purchase_order_id = ?";
			PreparedStatement ps = con.prepareStatement(sql);
			ps.setInt(1, poId);
			int rows = ps.executeUpdate();
			log("deleted " + rows + " rows from purchase_order_2_asset");
			ps.close();

			sql = "delete from purchase_order where id = ?";
			ps = con.prepareStatement(sql);
			ps.setInt(1, poId);
			rows = ps.executeUpdate();
			log("deleted " + rows + " pos");
			ps.close();

			poDeleteCount++;
		}
		catch (SQLException ex) {
			log("Caught SQLException: " + ex);
			con.rollback();
			poDeleteErrorCount++;
		}

		con.commit();
		con.setAutoCommit(true);
	}

	private void log(String msg) {
		if (logToSystemOut) {
			System.out.println(msg);
		}
		else {
			log.info(msg);
		}
	}

	class AssetWithCount {
		private final int count;
		private final int assetId;
		private final int cwId;
		private final int sourceId;

		public AssetWithCount(int count, int assetId, int cwId, int sourceId) {
			this.count = count;
			this.assetId = assetId;
			this.cwId = cwId;
			this.sourceId = sourceId;
		}

		public int getCount() { return count; }
		public int getAssetId() { return assetId; }
		public int getCwId() { return cwId; }
		public int getSourceId() { return sourceId; }

		@Override
		public String toString() {
			return "count [" + count + "] assetId [" + assetId + "] cwId [" + cwId + "] sourceId [" + sourceId + "]";
		}
	}

	class PurchaseOrder {
		private final int id;
		private final Timestamp lastUpdatedDate;
		private final int lastUpdatedUserId;
		private final Timestamp date;
		private final boolean isPermissionRequest;
		private final String note;
		private final String returnAddress;
		private final String userSignature;
		private final boolean isOutsideRecord;
		private final double estimatedPrice;
		private final String estimatedCurrency;
		private final boolean showEstimatedCost;

		public PurchaseOrder(int id, Timestamp lastUpdatedDate, int lastUpdatedUserId,
				Timestamp date, boolean isPermissionRequest, String note, String returnAddress,
				String userSignature, boolean isOutsideRecord, double estimatedPrice, String estimatedCurrency,
				boolean showEstimatedCost) {
			this.id = id;
			this.lastUpdatedDate = lastUpdatedDate;
			this.lastUpdatedUserId = lastUpdatedUserId;
			this.date = date;
			this.isPermissionRequest = isPermissionRequest;
			this.note = note;
			this.returnAddress = returnAddress;
			this.userSignature = userSignature;
			this.isOutsideRecord = isOutsideRecord;
			this.estimatedPrice = estimatedPrice;
			this.estimatedCurrency = estimatedCurrency;
			this.showEstimatedCost = showEstimatedCost;
		}

		public String importantFieldsAreSame(PurchaseOrder other) {
			StringBuilder sb = new StringBuilder();
			//if (!isSameDay(date, other.date)) sb.append("date ");
			if (isPermissionRequest != other.isPermissionRequest) sb.append("isPermissionRequest ");
			if (!StringUtils.equals(note, other.note))  sb.append("note ");
			if (!StringUtils.equals(returnAddress, other.returnAddress)) sb.append("returnAddress ");
			if (!StringUtils.equals(userSignature, other.userSignature)) sb.append("userSignature ");
			if (isOutsideRecord != other.isOutsideRecord) sb.append("isOutsideRecord ");
			if (estimatedPrice != other.estimatedPrice) sb.append("estimatedPrice ");
			if (!StringUtils.equals(estimatedCurrency, other.estimatedCurrency)) sb.append("estimatedCurrency ");
			if (showEstimatedCost != other.showEstimatedCost) sb.append("showEsimatedCost ");
			return sb.toString();
		}

		@SuppressWarnings("unused")
		private boolean isSameDay(Timestamp t1, Timestamp t2) {
			if (t1 == null && t2 == null) return true;
			if (t1 == null || t2 == null) return false;
			Date d1 = new Date(t1.getTime());
			Date d2 = new Date(t2.getTime());
			return DateUtils.isSameDay(d1, d2);
		}

		@Override
		public String toString() {
			return "id [" + id + "] lastUpdatedDate [" + lastUpdatedDate + "] lastUpdatedUserId ["
				+ lastUpdatedUserId + "] date [" + date + "] isPermissionRequest [" + isPermissionRequest
				+ "] note [" + note + "] returnAddress [" + returnAddress + "] userSignature [" + userSignature
				+ "] isOutsideRecord [" + isOutsideRecord + "] estimatedPrice ["
				+ estimatedPrice + "] estimatedCurrency [" + estimatedCurrency + "] showEstimatedCost [" + showEstimatedCost + "]";
		}

		public int getId() { return id; }
		public Timestamp getLastUpdatedDate() { return lastUpdatedDate; }
		public int getLastUpdatedUserId() { return lastUpdatedUserId; }
		public Timestamp getDate() { return date; }
		public boolean isPermissionRequest() { return isPermissionRequest; }
		public String getNote() { return note; }
		public String getReturnAddress() { return returnAddress; }
		public String getUserSignature() { return userSignature; }
		public boolean isOutsideRecord() { return isOutsideRecord; }
		public double getEstimatedPrice() { return estimatedPrice; }
		public String getEstimatedCurrenty() { return estimatedCurrency; }
		public boolean getShowEstimatedCost() { return showEstimatedCost; }
	}
}
