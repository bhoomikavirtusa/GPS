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
 * This program takes care of the situation where there are multiple similar
 * contracts that cover the same assets within a common work.
 * A bunch of duplicate contracts seem to have been created by Creative Service spreadsheet imports.
 * Another situation is that sometimes the user did not know what they were doing and created
 * a new contract to replace an existing one instead of just editing the existing one.
 * So delete the existing old contracts, as long as important fields are the same
 * and the contracts cover the same assets. We assume that if one contract has a file
 * that is that same size as another contract file that they are the same file.
 * If a new contract has a file but the older contract we are thinking of deleting
 * does not, that is fine.
 *
 * @since JDK 1.6
 * @version 2/10/2014
 * @author Steve Markoff
 */
public class CleanDuplicateContracts {

	private static final Log log = LogFactory.getLog(CleanDuplicateContracts.class);

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
		CleanDuplicateContracts prog = new CleanDuplicateContracts(
				dbConnect.getConnection(), true, cwId);
		prog.go();
	}

	private final Connection con;
	private final boolean logToSystemOut;
	private final int totalCount = 0;
	private final int cwId;
	private int contractDeleteCount = 0;
	private int contractDeleteErrorCount = 0;
	private int assetsNotSameCount = 0;
	private int differentFileBytesCount = 0;
	//private final boolean makeChanges = true;

	private final HashMap<Integer, Integer> cwIdsAffected = new HashMap<Integer, Integer>();

	/**
	 * @param cwId  0 means all common works
	 */
	public CleanDuplicateContracts(Connection con, boolean logToSystemOut, int cwId) throws SQLException {
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
		log("contractDeleteCount: " + intFormat.format(contractDeleteCount));
		log("contractDeleteErrorCount: " + intFormat.format(contractDeleteErrorCount));
		log("assetsNotSameCount: " + intFormat.format(assetsNotSameCount));
		log("cwIdsAffected.size() = " + cwIdsAffected.size());
		log("cwIdsAffected: " + StringUtil.collectionToString(cwIdsAffected.keySet(), " "));
		log("cwCounts: " + StringUtil.mapToString(cwIdsAffected, ", "));
		log("differentFileBytesCount: " + intFormat.format(differentFileBytesCount));
	}

	private void process(AssetWithCount awc, int i) throws SQLException, IOException {
		log("---- looking at: " + awc);
		List<Contract> contracts = getContracts(awc.getAssetId(), awc.getCwId(), awc.getSourceId());

		Contract firstContract = contracts.get(0);

		int firstFileBytes = fileSize(firstContract.getId());

		Set<Integer> firstAssetIds = getContractAssets(firstContract.getId());

		Contract lastContract = firstContract;
		Set<Integer> lastAssetIds = firstAssetIds;
		for (Contract contract : contracts) {
			log(contract.toString());

			if (contract.getId() == firstContract.getId()) {
				continue;
			}

			int fileBytes = fileSize(contract.getId());
			if (fileBytes != firstFileBytes && fileBytes > 0) {
				differentFileBytesCount++;
				log("contract with id [" + contract.getId() + "] has different fileBytes so skipping");
				continue;
			}

			Set<Integer> assetIds = getContractAssets(contract.getId());
			if (!assetIds.equals(lastAssetIds)) {
				log("contract with id [" + contract.getId() + "] does not have same assets so skipping\r\n"
					+ "    current (" + StringUtils.join(assetIds, " ") + ") last (" + StringUtils.join(lastAssetIds, " ") + ")");
				assetsNotSameCount++;
				continue;
			}

			String diff = lastContract.importantFieldsAreSame(contract);
			if (diff.isEmpty() || (!diff.contains("price ") && !diff.contains("number ")
					&& promptYes("delete contract with id [" + contract.getId() + "] (diff fields: " + diff + ")")))
			{
				log("deleting contract with id [" + contract.getId() + "]");
				deleteContract(contract.getId());
			}
			else {
				log("not deleting because of differences: " + diff);
			}

			lastContract = contract;
			lastAssetIds = assetIds;
		}
	}

	private List<Contract> getContracts(int assetId, int cwId, int sourceId) throws SQLException {
		String sql = "select id, number, last_updated_date, last_updated_user_id, start_date, end_date, date,"
			+ " price, currency, purchase_order_id from contract"
			+ " where id in (select contract_id from contract_2_asset where asset_base_id = ?)"
			+ " and cw_id = ? and source_id = ? order by date desc, id desc";
		PreparedStatement ps = con.prepareStatement(sql);
		ps.setInt(1, assetId);
		ps.setInt(2, cwId);
		ps.setInt(3, sourceId);
		ResultSet rs = ps.executeQuery();
		List<Contract> list = new ArrayList<Contract>();
		while (rs.next()) {
			list.add(new Contract(rs.getInt(1), rs.getString(2), rs.getTimestamp(3), rs.getInt(4),
				rs.getTimestamp(5), rs.getTimestamp(6), rs.getTimestamp(7), rs.getDouble(8), rs.getString(9), rs.getInt(10)));
		}
		ps.close();
		return list;
	}

	/**
	 * @param cwId  0 means all common works
	 */
	private List<AssetWithCount> getCounts(int cwId) throws SQLException {
		String sql = "select count(*) as count, asset_base_id, cw_id, source_id from contract_2_asset c2a, contract c"
			+ " where c.id = c2a.contract_id"
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

	private Set<Integer> getContractAssets(int contractId) throws SQLException {
		String sql = "select asset_base_id from contract_2_asset where contract_id = ?";
		PreparedStatement ps = con.prepareStatement(sql);
		ps.setInt(1, contractId);
		ResultSet rs = ps.executeQuery();
		HashSet<Integer> set = new HashSet<Integer>();
		while (rs.next()) {
			set.add(rs.getInt(1));
		}
		ps.close();
		return set;
	}

	private void deleteContract(int contractId) throws SQLException, IOException {
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
			
			// assume same comp_copy assigned to first contract
			// (if there is a comp_copy at all)
			sql = "delete from comp_copy where contract_id = ?";
			ps = con.prepareStatement(sql);
			ps.setInt(1, contractId);
			rows = ps.executeUpdate();
			log("deleted " + rows + " comp_copy records");
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
			contractDeleteErrorCount++;
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

	class Contract {
		private final int id;
		private final String number;
		private final Timestamp lastUpdatedDate;
		private final int lastUpdatedUserId;
		private final Timestamp startDate;
		private final Timestamp endDate;
		private final Timestamp date;
		private final double price;
		private final String currency;
		private final Integer poId;

		public Contract(int id, String number, Timestamp lastUpdatedDate, int lastUpdatedUserId,
				Timestamp startDate, Timestamp endDate, Timestamp date, double price, String currency, Integer poId) {
			this.id = id;
			this.number = number;
			this.lastUpdatedDate = lastUpdatedDate;
			this.lastUpdatedUserId = lastUpdatedUserId;
			this.startDate = startDate;
			this.endDate = endDate;
			this.date = date;
			this.price = price;
			this.currency = currency;
			if (poId == 0)  poId = null;
			this.poId = poId;
		}

		public String importantFieldsAreSame(Contract other) {
			StringBuilder sb = new StringBuilder();
			if (!StringUtils.equals(number, other.number)) sb.append("number ");
			if (!isSameDay(startDate, other.startDate)) sb.append("startDate ");
			//if (!isSameDay(date, other.date)) sb.append("date ");
			if (price != other.price) sb.append("price ");
			if (!StringUtils.equals(currency, other.currency))  sb.append("currency: ");
			return sb.toString();
		}

		private boolean isSameDay(Timestamp t1, Timestamp t2) {
			if (t1 == null && t2 == null) return true;
			if (t1 == null || t2 == null) return false;
			Date d1 = new Date(t1.getTime());
			Date d2 = new Date(t2.getTime());
			return DateUtils.isSameDay(d1, d2);
		}

		@Override
		public String toString() {
			return "id [" + id + "] number [" + number + "] lastUpdatedDate [" + lastUpdatedDate + "] lastUpdatedUserId ["
				+ lastUpdatedUserId + "] startDate [" + startDate + "] endDate [" + endDate + "] date [" + date
				+ "] price [" + price + "] currency [" + currency + "] poId [" + poId + "]";
		}

		public int getId() { return id; }
		public String getNumber() { return number; }
		public Timestamp getLastUpdatedDate() { return lastUpdatedDate; }
		public int getLastUpdatedUserId() { return lastUpdatedUserId; }
		public Timestamp getStartDate() { return startDate; }
		public Timestamp getEndDate() { return endDate; }
		public Timestamp getDate() { return date; }
		public double getPrice() { return price; }
		public String getCurrency() { return currency; }
		public Integer getPoId() { return poId; }
	}
}
