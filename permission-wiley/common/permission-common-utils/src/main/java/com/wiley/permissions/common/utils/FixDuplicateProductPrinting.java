package com.wiley.permissions.common.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.ArrayList;

import com.wiley.sf.common.sql.SimpleDBConnect;

/**
 * Program to fix temporary data problem.
 *
 * @since   JDK 1.6
 * @version 3/31/2011
 * @author  Steve Markoff
 */
public class FixDuplicateProductPrinting {

	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			System.err.println("Usage: <properties file>");
			System.exit(1);
		}

		SimpleDBConnect dbConnect = SimpleDBConnect.create(args[0]);
		// throws various exceptions
		FixDuplicateProductPrinting prog = new FixDuplicateProductPrinting(dbConnect.getConnection());
		prog.go();
	}


	private final Connection con;
	private final boolean makeChanges = true;

	public FixDuplicateProductPrinting(Connection con) throws SQLException {
		this.con = con;
		con.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
		con.setAutoCommit(true);
	}

	public void go() throws SQLException {
		System.out.println("querying for duplicate records...");
		ArrayList<DuplicateSummary> list = getDuplicateRecords();
		NumberFormat intFormat = NumberFormat.getIntegerInstance();
		System.out.println("number of duplicate records = " + intFormat.format(list.size()));

		int total = list.size();
		int count = 1;
		for (DuplicateSummary s : list) {
			System.out.println("---- " + intFormat.format(count) + " out of "
					+ intFormat.format(total) + ": working on record [" + s + "] ----");
			fixDuplicates(s, count);
			count++;
		}
	}

	private ArrayList<DuplicateSummary> getDuplicateRecords() throws SQLException {
		String sql = "select count(*) as count, product_id, printing_number, distribution_center, po_number"
				+ " from product_printing group by product_id, printing_number, distribution_center, po_number";
		PreparedStatement ps = con.prepareStatement(sql);

		ResultSet rs = ps.executeQuery();
		ArrayList<DuplicateSummary> list = new ArrayList<DuplicateSummary>();

		while (rs.next()) {
			int count = rs.getInt(1);
			int productId = rs.getInt(2);
			int printingNumber = rs.getInt(3);
			int distributionCenter = rs.getInt(4);
			String poNumber = rs.getString(5);

			if (count > 1) {
				list.add(new DuplicateSummary(productId, printingNumber, distributionCenter, poNumber));
			}
		}
		ps.close();
		return list;
	}

	private void fixDuplicates(DuplicateSummary s, int count) throws SQLException {
		ArrayList<Integer> idList = getIds(s);
		System.out.println("#ids = " + idList.size());
		int firstId = idList.get(0);

		deleteOthers(s, firstId);
	}

	private ArrayList<Integer> getIds(DuplicateSummary s) throws SQLException {
		String sql = "select id from product_printing where product_id = ? and printing_number = ?"
				+ " and distribution_center = ? and po_number = ? order by id desc";
		PreparedStatement ps = con.prepareStatement(sql);
		ps.setInt(1, s.getProductId());
		ps.setInt(2, s.getPrintingNumber());
		ps.setInt(3, s.getDistributionCenter());
		ps.setString(4, s.getPONumber());
		ResultSet rs = ps.executeQuery();
		ArrayList<Integer> idList = new ArrayList<Integer>();

		while (rs.next()) {
			idList.add(rs.getInt(1));
		}
		ps.close();
		return idList;
	}

	private void deleteOthers(DuplicateSummary s, int firstId) throws SQLException {
		String sql = "delete from product_printing where product_id = ? and printing_number = ?"
				+ " and distribution_center = ? and po_number = ? and id != ?";
		System.out.println("psuedo-SQL: delete from product_printing where product_id = "
				+ s.getProductId() + " and printing_number = "
				+ s.getPrintingNumber() + " and distribution_center = "
				+ s.getDistributionCenter() + " and po_number = '"
				+ s.getPONumber() + "' and id != " + firstId);
		PreparedStatement ps = con.prepareStatement(sql);
		ps.setInt(1, s.getProductId());
		ps.setInt(2, s.getPrintingNumber());
		ps.setInt(3, s.getDistributionCenter());
		ps.setString(4, s.getPONumber());
		ps.setInt(5, firstId);
		if (makeChanges) {
			int rowCount = ps.executeUpdate();
			System.out.println("" + rowCount + " rows deleted");
		}
		else {
			System.out.println("nothing deleted - test mode");
		}
		ps.close();
	}
	
	class DuplicateSummary {
		private final int productId;
		private final int printingNumber;
		private final int distributionCenter;
		private final String poNumber;

		public DuplicateSummary(int productId, int printingNumber, int distributionCenter, String poNumber) {
			this.productId = productId;
			this.printingNumber = printingNumber;
			this.distributionCenter = distributionCenter;
			this.poNumber = poNumber;
		}

		public int getProductId() {
			return productId;
		}

		public int getPrintingNumber() {
			return printingNumber;
		}

		public int getDistributionCenter() {
			return distributionCenter;
		}

		public String getPONumber() {
			return poNumber;
		}

		@Override
		public String toString() {
			return "productId = " + productId
					+ ", printingNumber = " + printingNumber
					+ ", distributionCenter = " + distributionCenter
					+ ", poNumber = " + poNumber;
		}
	}
}