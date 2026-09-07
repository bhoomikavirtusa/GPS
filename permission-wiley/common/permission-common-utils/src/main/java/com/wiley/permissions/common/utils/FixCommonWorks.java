package com.wiley.permissions.common.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.sf.common.monitor.PerformanceMonitor;
import com.wiley.sf.common.monitor.PerformanceMonitor.PerfTimer;
import com.wiley.sf.common.sql.SimpleDBConnect;
import com.wiley.sf.common.text.TimeFormat;

/**
 * Program to fix data problems with common work and products.
 * Fixes 3 problems:
 * 1) A common work has no primary products.
 * 2) A common work has more than one primary product.
 * 3) A common work has the wrong product as primary.
 *
 * Also reports:
 * 3) A common work has no products - there is no point in deleting
 * because we insert/update from the CommonWork MasterList.
 *
 * @since JDK 1.6
 * @version 10/8/2013
 * @author Steve Markoff
 */
public class FixCommonWorks {

	private static final Log log = LogFactory.getLog(FixCommonWorks.class);

	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			System.err.println("Usage: <properties file>");
			System.exit(1);
		}

		SimpleDBConnect dbConnect = SimpleDBConnect.create(args[0]);
		// throws various exceptions
		FixCommonWorks prog = new FixCommonWorks(
				dbConnect.getConnection(), true);
		prog.go();
	}

	private final Connection con;
	private final boolean logToSystemOut;
	private final boolean makeChanges = true;

	public FixCommonWorks(Connection con, boolean logToSystemOut) throws SQLException {
		this.con = con;
		this.logToSystemOut = logToSystemOut;
		con.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
		con.setAutoCommit(true);
	}

	public void go() throws SQLException {
		PerformanceMonitor monitor = PerformanceMonitor.getInstance();
		PerfTimer timer = monitor.startTimer("FixCommonWorks::part1");

		ArrayList<Integer> noProductList = new ArrayList<Integer>();
		ArrayList<Integer> cwIdList = getBadCommonWorks(noProductList);
		NumberFormat intFormat = NumberFormat.getIntegerInstance();
		log("# commonWorks with 0 or more than 1 primary product = " + intFormat.format(cwIdList.size()));
		log("# commonWorks that have no products = " + intFormat.format(noProductList.size()));

		for (Integer cwId : cwIdList) {
			fixCommonWork(cwId);
		}

		long time = timer.stopTimer();
		log("part 1 completed in " + time + " ms.");


		timer = monitor.startTimer("FixCommonWorks::part2");

		log("now reviewing all commonWorks to make sure the right product (medium) is primary...");
		cwIdList = getAllCommonWorks();
		int changeCount = 0;
		for (Integer cwId : cwIdList) {
			boolean changeMade = fixCommonWork(cwId);
			if (changeMade)  changeCount++;
		}
		log("# commonWorks where primary product was incorrect (wrong medium) = " + intFormat.format(changeCount));
		log("# commonWorks with more than 0 products = " + intFormat.format(cwIdList.size()));

		time = timer.stopTimer();
		TimeFormat timeFormat = new TimeFormat();
		log("part 2 completed in " + timeFormat.formatMS(time));
	}

	private ArrayList<Integer> getBadCommonWorks(List<Integer> noProductList) throws SQLException {
		String sql = "select id, "
			+ "(select count(*) from product where cw_id = cw.id and is_cw_primary = 1) as primary_count, "
			+ "(select count(*) from product where cw_id = cw.id) as count "
			+ "from common_work cw order by primary_count, count";

		PreparedStatement ps = con.prepareStatement(sql);

		ResultSet rs = ps.executeQuery();
		ArrayList<Integer> cwIdList = new ArrayList<Integer>();

		while (rs.next()) {
			int cwId = rs.getInt(1);
			int primaryCount = rs.getInt(2);
			int count = rs.getInt(3);
			if (count == 0) {
				noProductList.add(cwId);
			}
			else if ((primaryCount == 0 && count > 0) || primaryCount > 1) {
				cwIdList.add(cwId);
			}
		}
		ps.close();
		return cwIdList;
	}

	private ArrayList<Integer> getAllCommonWorks() throws SQLException {
		String sql = "select id from common_work cw where ((select count(*) from product where cw_id = cw.id) > 0)";

		PreparedStatement ps = con.prepareStatement(sql);
		ResultSet rs = ps.executeQuery();
		ArrayList<Integer> cwIdList = new ArrayList<Integer>();

		while (rs.next()) {
			int cwId = rs.getInt(1);
			cwIdList.add(cwId);
		}
		ps.close();
		return cwIdList;
	}

	private boolean fixCommonWork(int cwId) throws SQLException {
		ArrayList<Product> productList = getProducts(cwId);

		if (productList.size() == 0) {
			// we don't expect this situation
			return false;
		}

		boolean changeMade = false;

		int highRank = 0;
		int highRankIndex = 0;
		for (int i = 0; i < productList.size(); i++) {
			Product product = productList.get(i);
			int rank = product.getMediumRank();
			if (rank > highRank) {
				highRank = rank;
				highRankIndex = i;
			}
		}

		for (int i = 0; i < productList.size(); i++) {
			Product product = productList.get(i);
			boolean value = (highRankIndex == i ? true : false);
			if (product.isCwPrimary() != value) {
				changeMade = true;
				updateProduct(product.getId(), value);
			}
		}

		if (changeMade) {
			log("---- above changes for commonWork id " + cwId + ", has " + productList.size() + " products");
		}

		return changeMade;
	}

	private ArrayList<Product> getProducts(int cwId) throws SQLException {
		String sql = "select id, is_cw_primary, medium from product where cw_id = ?";
		PreparedStatement ps = con.prepareStatement(sql);
		ps.setInt(1, cwId);
		ResultSet rs = ps.executeQuery();
		ArrayList<Product> productList = new ArrayList<Product>();

		while (rs.next()) {
			int id = rs.getInt(1);
			boolean cwPrimary = rs.getBoolean(2);
			String medium = rs.getString(3);
			productList.add(new Product(id, cwPrimary, medium));
		}
		ps.close();
		return productList;
	}

	private void updateProduct(int id, boolean cwPrimary) throws SQLException {
		String beg = makeChanges ? "running: " : "would run: ";
		log(beg + "update product set is_cw_primary = " + cwPrimary + " where id = " + id);

		if (!makeChanges) return;

		String sql = "update product set is_cw_primary = ? where id = ?";
		PreparedStatement ps = con.prepareStatement(sql);
		ps.setBoolean(1, cwPrimary);
		ps.setInt(2, id);

		int numRows = ps.executeUpdate();
		if (numRows != 1) { // we don't expect this situation
			log("numRows updated was " + numRows + " but expected 1 for product id = " + id);
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
}

class Product {
	private final int id;
	private boolean isCwPrimary;
	private final String medium;

	public Product(int id, boolean isCwPrimary, String medium) {
		this.id = id;
		this.isCwPrimary = isCwPrimary;
		this.medium = medium;
	}

	public int getId() {
		return id;
	}

	public boolean isCwPrimary() {
		return isCwPrimary;
	}

	public String getMedium() {
		return medium;
	}

	public void setCwPrimary(boolean cwPrimary) {
		isCwPrimary = cwPrimary;
	}

	// similar to ProductRespository.getMediumRank()
	// would be better to use medium constants but can't access from this
	// package
	public int getMediumRank() {
		if (medium == null)  return 0;

		if (medium.equals("C")) {
			return 10;
		}
		else if (medium.equals("P")) {
			return 9;
		}
		else if (medium.equals("L")) {
			return 8;
		}
		else if (medium.equals("V")) {
			return 7;
		}
		else if (medium.equals("W")) {
			return 6;
		}
		else if (medium.equals("S")) {
			return 5;
		}
		else {
			return 0;
		}
	}

	@Override
	public String toString() {
		return "id = " + id + ", isCwPrimary = " + isCwPrimary + ", medium = "
				+ medium;
	}
}