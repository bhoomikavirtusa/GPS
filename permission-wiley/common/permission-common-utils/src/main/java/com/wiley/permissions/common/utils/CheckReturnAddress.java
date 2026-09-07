package com.wiley.permissions.common.utils;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.sf.common.sql.SimpleDBConnect;
import com.wiley.sf.common.text.TimeFormat;

/**
 * This program checks the return address column of 2 tables - purchase_order and user_defaults.
 *
 * @since JDK 1.6
 * @version 4/23/2013
 * @author Steve Markoff
 */
public class CheckReturnAddress {

	private static final Log log = LogFactory.getLog(FixContracts.class);

	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			System.err.println("Usage: <properties file>");
			System.exit(1);
		}

		SimpleDBConnect dbConnect = SimpleDBConnect.create(args[0]);
		// throws various exceptions
		CheckReturnAddress prog = new CheckReturnAddress(
				dbConnect.getConnection(), true);
		prog.go();
	}

	private final Connection con;
	private final boolean logToSystemOut;
	private int errorCount = 0;


	public CheckReturnAddress(Connection con, boolean logToSystemOut) throws SQLException {
		this.con = con;
		this.logToSystemOut = logToSystemOut;
		con.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
		con.setAutoCommit(true);
	}

	public void go() throws SQLException, IOException {
		long startTime = System.currentTimeMillis();

		List<Info> list = getPOReturnAddressList();

		for (int i = 0; i < list.size(); i++) {
			Info info = list.get(i);
			process(info);
		}

		list = getUserDefaultReturnAddressList();

		for (int i = 0; i < list.size(); i++) {
			Info info = list.get(i);
			process(info);
		}

		long time = System.currentTimeMillis() - startTime;
		TimeFormat timeFormat = new TimeFormat();
		log("total time was " + timeFormat.formatMS(time));
		log("errorCount was " + errorCount);
	}

	private void process(Info info) {
		String errorMsg = validateReturnAddress(info.getReturnAddress());
		if (errorMsg != null) {
			errorCount++;
			log("---- bad return address: " + errorMsg + " ----");
			log(info.toString());
		}
	}

	private List<Info> getPOReturnAddressList() throws SQLException {
		String sql = "select po.return_address, po.id, po.cw_id, po.last_updated_user_id, u.first_name, u.last_name"
			+ " from purchase_order po, user_table u where u.id = po.last_updated_user_id order by po.last_updated_user_id, po.id";
		PreparedStatement ps = con.prepareStatement(sql);
		ResultSet rs = ps.executeQuery();
		List<Info> list = new ArrayList<Info>();
		while (rs.next()) {
			String returnAddress = rs.getString(1);
			int id = rs.getInt(2);
			int cwId = rs.getInt(3);
			int lastUpdatedUserId = rs.getInt(4);
			String firstName = rs.getString(5);
			String lastName = rs.getString(6);
			String info = "poId = " + id + ", cwId = " + cwId + ", lastUpdatedUserId = " + lastUpdatedUserId
				+ ", first/last = " + firstName + " / " + lastName;
			list.add(new Info(returnAddress, info));
		}
		ps.close();
		return list;
	}

	private List<Info> getUserDefaultReturnAddressList() throws SQLException {
		String sql = "select ud.return_address, ud.user_id, u.first_name, u.last_name"
			+ " from user_defaults ud, user_table u where u.id = ud.user_id";
		PreparedStatement ps = con.prepareStatement(sql);
		ResultSet rs = ps.executeQuery();
		List<Info> list = new ArrayList<Info>();
		while (rs.next()) {
			String returnAddress = rs.getString(1);
			int userId = rs.getInt(2);
			String firstName = rs.getString(3);
			String lastName = rs.getString(4);
			String info = "userId = " + userId + ", first/last = " + firstName + " / " + lastName;
			list.add(new Info(returnAddress, info));
		}
		ps.close();
		return list;
	}

	private void log(String msg) {
		if (logToSystemOut) {
			System.out.println(msg);
		}
		else {
			log.info(msg);
		}
	}

	class Info {
		private final String returnAddress;
		private final String info;

		public Info(String returnAddress, String info) {
			this.returnAddress = returnAddress;
			this.info = info;
		}

		@Override
		public String toString() {
			return "returnAddress [" + returnAddress + "] info [" + info + "]";
		}

		public String getReturnAddress() { return returnAddress; }
		public String getInfo() { return info; }
	}

	// this copied/pasted from PurchaseOrder
	/**
	 * Returns error message if Return Address is not valid.
	 * Possible errors are being null/blank,
	 * more than 7 new lines, or a single line is more than 38 chars.
	 */
	public static String validateReturnAddress(String s) {
		if (StringUtils.isBlank(s)) {
			//return "Return Address cannot be blank";
			return null;  // ignore blank for now
		}
/*
		// check for newline instead of carriage returns since cover both "\r\n" and "\n"
		int newLineCount = StringUtils.countMatches(s, "\n");
		if (newLineCount > 7) {
			// (can either have 7 lines with a newline at the end of each one
			// or 8 lines with no newline at the end but this looks bad)
			return "Return Address cannot have more than 7 lines";
		}*/

		String [] lines = s.split("\\r?\\n");  // note "\\" may not be necessary
		for (String line : lines) {
			if (line.length() > 38) {
				return "Return Address cannot have a line more than 38 chars (found " + line.length() + ")";
			}
		}

		return null;
	}
}
