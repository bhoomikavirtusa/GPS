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
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.sf.common.lang.StringUtil;
import com.wiley.sf.common.sql.SimpleDBConnect;
import com.wiley.sf.common.text.TimeFormat;

/**
 * This program takes care of the situation where there are multiple AssetUses for
 * an Asset which are exactly the same (same cw_id, position and other fields).
 *
 * @since JDK 1.6
 * @version 6/24/2013
 * @author Steve Markoff
 */
public class CleanDuplicateAssetUses {

	private static final Log log = LogFactory.getLog(CleanDuplicateAssetUses.class);

	private static final NumberFormat intFormat = NumberFormat.getIntegerInstance();

	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			System.err.println("Usage: <properties file>");
			System.exit(1);
		}

		SimpleDBConnect dbConnect = SimpleDBConnect.create(args[0]);
		// throws various exceptions
		CleanDuplicateAssetUses prog = new CleanDuplicateAssetUses(
				dbConnect.getConnection(), true);
		prog.go();
	}

	private final Connection con;
	private final boolean logToSystemOut;
	private int notDeleteCount = 0;
	private int deleteCount = 0;
	private int deleteErrorCount = 0;
	//private final boolean makeChanges = true;

	private final HashMap<Integer, Integer> cwIdsAffected = new HashMap<Integer, Integer>();


	public CleanDuplicateAssetUses(Connection con, boolean logToSystemOut) throws SQLException {
		this.con = con;
		this.logToSystemOut = logToSystemOut;
		con.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
		con.setAutoCommit(true);
	}

	public void go() throws SQLException, IOException {
		long startTime = System.currentTimeMillis();

		List<AssetWithCount> list = getCounts();
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
		log("notDeleteCount: " + intFormat.format(notDeleteCount));
		log("deleteCount: " + intFormat.format(deleteCount));
		log("deleteErrorCount: " + intFormat.format(deleteErrorCount));
		log("cwIdsAffected.size() = " + cwIdsAffected.size());
		log("cwIdsAffected: " + StringUtil.collectionToString(cwIdsAffected.keySet(), " "));
		log("cwCounts: " + StringUtil.mapToString(cwIdsAffected, ", "));
	}

	private void process(AssetWithCount awc, int i) throws SQLException, IOException {
		log("---- looking at: " + awc);
		List<AssetUse> auList = getAssetUseList(awc.getAssetId(), awc.getCwId(), awc.getPosition(), awc.getComponentId());

		AssetUse first = auList.get(0);

		AssetUse last = first;
		for (AssetUse au : auList) {
			log(au.toString());

			if (au.getId() == first.getId()) {
				continue;
			}

			boolean delete = false;
			String diff = last.importantFieldsAreSame(au);
			//if (diff.isEmpty()) {
			if (diff.isEmpty() || (!diff.contains("usageType ") && !diff.contains("finalPage ") && !diff.contains("size ")
					&& promptYes("delete asset use with id [" + au.getId() + "] (diff fields: " + diff + ")"))) {

				int fileSize = getFileSize(au.getId());
				if (fileSize == -1) {
					log("AU has multiple files so won't delete -- handle manually");
				}
				else if (fileSize == 0) {
					delete = true;
				}
				else if (fileSize > 0) {
					int lastFileSize = getFileSize(last.getId());
					if (fileSize == lastFileSize) {
						deleteAssetUseFile(au.getId());
						delete = true;
					}
					else {
						if (lastFileSize == 0) {
							moveAssetUseFile(last.getId(), au.getId());
							delete = true;
						}
						else {
							log("won't delete because has file and last AU file size does not match");
						}
					}
				}
			}
			else {
				log("differences: " + diff);
			}

			if (delete) {
				deleteAssetUse(au.getId(), awc.getCwId());
			}
			else {
				notDeleteCount++;
			}

			last = au;
		}
	}

	private List<AssetUse> getAssetUseList(int assetId, int cwId, String position, Integer componentId) throws SQLException {
		String sql = "select id, last_updated_date, last_updated_user_id, cw_id, asset_id,"
			+ " position, usage_type, manuscript_page, final_page, found_on, size, permission_comment,"
			+ " production_comment, request_comment, caption, pickup_isbn, pickup_comment, is_canceled"
			+ " from asset_use where cw_id = ? and asset_id = ?"
			+ ((position == null) ? " and position is null" : " and position = ?")
			+ ((componentId == null) ? " and component_id is null " : " and component_id = ?")
			+ " order by is_canceled, id desc";
		// note order by is_canceled before id is important - so if there are some canceled au's then we
		// will ending up deleting these instead of something that is not canceled.
		PreparedStatement ps = con.prepareStatement(sql);
		int pos = 1;
		ps.setInt(pos++, cwId);
		ps.setInt(pos++, assetId);
		if (position != null) {
			ps.setString(pos++, position);
		}
		if (componentId != null) {
			ps.setInt(pos++, componentId);
		}
		ResultSet rs = ps.executeQuery();
		List<AssetUse> list = new ArrayList<AssetUse>();
		while (rs.next()) {
			list.add(new AssetUse(rs.getInt(1), rs.getTimestamp(2), rs.getInt(3), rs.getInt(4),
					rs.getInt(5), rs.getString(6), rs.getString(7), rs.getString(8), rs.getString(9),
					rs.getString(10), rs.getString(11), rs.getString(12), rs.getString(13), rs.getString(14),
					rs.getString(15), rs.getString(16), rs.getString(17), rs.getBoolean(18)));
		}
		ps.close();
		return list;
	}

	private List<AssetWithCount> getCounts() throws SQLException {
		// after the filemaker import is done, can remove the filter by filemaker false to see if filemaker import caused any duplicates
		String sql = "select count(*) as count, cw_id, asset_id, description, position, component_id from asset_use au, asset a where au.asset_id = a.id"
			+ " and (au.import_source is null or au.import_source <> 1)"
			+ " group by cw_id, asset_id, position, component_id having count(*) > 1 order by count desc, cw_id desc, asset_id, position";
		PreparedStatement ps = con.prepareStatement(sql);
		ResultSet rs = ps.executeQuery();
		List<AssetWithCount> list = new ArrayList<AssetWithCount>();
		while (rs.next()) {
			// AssetWithCount converts componentId of 0 to null
			list.add(new AssetWithCount(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getString(4), rs.getString(5), rs.getInt(6)));
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

	/**
	 * Returns 0 if there is no file for the asset use, returns -1 if there is more
	 * than one file, otherwise returns the size of the single file.
	 * @param auId
	 * @return
	 * @throws SQLException
	 */
	private int getFileSize(int auId) throws SQLException {
		String sql = "select length(file_data) from asset_use_file where au_id = ?";
		PreparedStatement ps = con.prepareStatement(sql);
		ps.setInt(1, auId);
		ResultSet rs = ps.executeQuery();
		int result = 0;
		if (rs.next()) {
			result = rs.getInt(1);
		}
		if (rs.next()) {
			result = -1;
		}
		ps.close();
		return result;
	}

	private void deleteAssetUseFile(int auId) throws SQLException {
		String sql = "delete from asset_use_file where au_id = ?";
		PreparedStatement ps = con.prepareStatement(sql);
		ps.setInt(1, auId);
		int rows = ps.executeUpdate();
		log("deleted " + rows + " from asset_use_file");
		ps.close();
	}

	private void moveAssetUseFile(int from, int to) throws SQLException {
		String sql = "update asset_use_file set au_id = ? where au_id = ?";
		PreparedStatement ps = con.prepareStatement(sql);
		ps.setInt(1, from);
		ps.setInt(2, to);
		int rows = ps.executeUpdate();
		log("updated " + rows + " row from asset_use_file");
		ps.close();
	}

	private void deleteAssetUse(int auId, int cwId) throws SQLException, IOException {
		con.setAutoCommit(false);

		try {
			String sql = "delete from asset_perm_ref where asset_use_id = ?";
			PreparedStatement ps = con.prepareStatement(sql);
			ps.setInt(1, auId);
			int rows = ps.executeUpdate();
			log("deleted " + rows + " from asset_perm_ref");
			ps.close();

			sql = "delete from au_source_perm_status where asset_use_id = ?";
			ps = con.prepareStatement(sql);
			ps.setInt(1, auId);
			rows = ps.executeUpdate();
			log("deleted " + rows + " from au_source_perm_status");
			ps.close();

			sql = "delete from asset_use where id = ?";
			ps = con.prepareStatement(sql);
			ps.setInt(1, auId);
			rows = ps.executeUpdate();
			log("deleted " + rows + " from asset_use");
			ps.close();

			deleteCount++;

			Integer count = cwIdsAffected.get(cwId);
			if (count == null) count = new Integer(1);
			else count = new Integer(count + 1);
			cwIdsAffected.put(cwId, count);
		}
		catch (SQLException ex) {
			log("Caught SQLException: " + ex);
			con.rollback();
			deleteErrorCount++;
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
		private final int cwId;
		private final int assetId;
		private final String description;
		private final String position;
		private final Integer componentId;

		public AssetWithCount(int count, int cwId, int assetId, String description, String position, Integer componentId) {
			this.count = count;
			this.cwId = cwId;
			this.assetId = assetId;
			this.description = description;
			this.position = position;
			if (componentId == 0)  componentId = null;
			this.componentId = componentId;
		}

		public int getCount() { return count; }
		public int getCwId() { return cwId; }
		public int getAssetId() { return assetId; }
		public String getDescription() { return description; }
		public String getPosition() { return position; }
		public Integer getComponentId() { return componentId; }

		@Override
		public String toString() {
			return "count [" + count + "] cwId [" + cwId + "] assetId [" + assetId
				+ "] description [" + description + "] position [" + position + "] componentId [" + componentId + "]";
		}
	}

	class AssetUse {
		private final int id;
		private final Timestamp lastUpdatedDate;
		private final int lastUpdatedUserId;
		private final int cwId;
		private final int assetId;
		private final String position;
		private final String usageType;
		private final String manuscriptPage;
		private final String finalPage;
		private final String foundOn;
		private final String size;
		private final String permissionComment;
		private final String productionComment;
		private final String requestComment;
		private final String caption;
		private final String pickupISBN;
		private final String pickupComment;
		private final boolean canceled;

		public AssetUse(int id, Timestamp lastUpdatedDate, int lastUpdatedUserId,
				int cwId, int assetId, String position, String usageType, String manuscriptPage, String finalPage,
				String foundOn, String size, String permissionComment, String productionComment, String requestComment,
				String caption, String pickupISBN, String pickupComment, boolean canceled) {
			this.id = id;
			this.lastUpdatedDate = lastUpdatedDate;
			this.lastUpdatedUserId = lastUpdatedUserId;
			this.cwId = cwId;
			this.assetId = assetId;
			this.position = position;
			this.usageType = usageType;
			this.manuscriptPage = manuscriptPage;
			this.finalPage = finalPage;
			this.foundOn = foundOn;
			this.size = size;
			this.permissionComment = permissionComment;
			this.productionComment = productionComment;
			this.requestComment = requestComment;
			this.caption = caption;
			this.pickupISBN = pickupISBN;
			this.pickupComment = pickupComment;
			this.canceled = canceled;
		}

		public String importantFieldsAreSame(AssetUse other) {
			StringBuilder sb = new StringBuilder();
			//if (!isSameDay(date, other.date)) sb.append("date ");
			if (lastUpdatedUserId != other.lastUpdatedUserId) sb.append("lastUpdatedUserId ");
			if (stringDiff(usageType, other.usageType))  sb.append("usageType ");
			if (stringDiff(manuscriptPage, other.manuscriptPage))  sb.append("manuscriptPage ");
			if (stringDiff(finalPage, other.finalPage))  sb.append("finalPage ");
			if (stringDiff(foundOn, other.foundOn))  sb.append("foundOn ");
			if (stringDiff(size, other.size))  sb.append("size ");
			if (stringDiff(permissionComment, other.permissionComment)) sb.append("permissionComment ");
			if (stringDiff(productionComment, other.productionComment)) sb.append("productionComment ");
			if (stringDiff(requestComment, other.requestComment))  sb.append("requestComment ");
			if (stringDiff(caption, other.caption))  sb.append("caption ");
			if (stringDiff(pickupISBN, other.pickupISBN))  sb.append("pickupISBN ");
			if (stringDiff(pickupComment, other.pickupComment))  sb.append("pickupComment ");
			return sb.toString();
		}

		private boolean stringDiff(String thisField, String other) {
			// other is the older object so if the older is blank and the newer is not, ok
			// to not save older
			return (!StringUtils.equals(thisField, other) && !StringUtils.isBlank(other));
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
			// not going to print out all fields here
			return "id [" + id + "] lastUpdatedDate [" + lastUpdatedDate + "] lastUpdatedUserId ["
				+ lastUpdatedUserId + "] cwId [" + cwId + "] assetId [" + assetId
				+ "] position [" + position + "] usageType [" + usageType
				+ "] manuscriptPage [" + manuscriptPage + "] finalPage [" + finalPage
				+ "] size [" + size + "] permissionComment [" + permissionComment
				+ "] productionComment [" + productionComment + "] pickupISBN [" + pickupISBN + "] canceled [" + canceled + "]";
		}

		public int getId() { return id; }
		public Timestamp getLastUpdatedDate() { return lastUpdatedDate; }
		public int getLastUpdatedUserId() { return lastUpdatedUserId; }
		public int getCwId() { return cwId; }
		public int getAssetId() { return assetId; }
		// don't need other getters
	}
}
