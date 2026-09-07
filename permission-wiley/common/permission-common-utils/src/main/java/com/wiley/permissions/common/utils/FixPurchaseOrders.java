package com.wiley.permissions.common.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.sf.common.sql.SimpleDBConnect;
import com.wiley.sf.common.text.TimeFormat;

/**
 * This program takes care of the situation where there are po's with no assets.
 * It deletes them as long as there is another po for the same commonwork
 * and source that does have assets.
 *
 * @since JDK 1.6
 * @version 4/24/2013
 * @author Steve Markoff
 */
public class FixPurchaseOrders {

	private static final Log log = LogFactory.getLog(FixPurchaseOrders.class);

	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			System.err.println("Usage: <properties file>");
			System.exit(1);
		}

		SimpleDBConnect dbConnect = SimpleDBConnect.create(args[0]);
		// throws various exceptions
		FixPurchaseOrders prog = new FixPurchaseOrders(
				dbConnect.getConnection(), true);
		prog.go();
	}

	private final Connection con;
	private final boolean logToSystemOut;
	//private final boolean makeChanges = false;


	public FixPurchaseOrders(Connection con, boolean logToSystemOut) throws SQLException {
		this.con = con;
		this.logToSystemOut = logToSystemOut;
		con.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
		con.setAutoCommit(true);
	}

	public void go() throws SQLException, IOException {
		long startTime = System.currentTimeMillis();

		List<PurchaseOrder> list = getPOsWithNoAssets();
		log("found " + list.size() + " po's with no assets.");

		for (int i = 0; i < list.size(); i++) {
			PurchaseOrder po = list.get(i);
			process(po, i);
		}

		long time = System.currentTimeMillis() - startTime;
		TimeFormat timeFormat = new TimeFormat();
		log("total time was " + timeFormat.formatMS(time));
	}

	private void process(PurchaseOrder po, int i) throws SQLException, IOException {
		log("---- processing PO: " + po);
		List<PurchaseOrder> similarList = getSimilarPOs(po);
		log("# other POs with same cwId and sourceId and having assets: " + similarList.size());

		boolean delete = false;
		if (similarList.size() > 0) {
			delete = true;
		}
		else {
			delete = promptYes("delete PO");
		}

		if (delete) {
			deletePurchaseOrder(po);
		}
	}

	private List<PurchaseOrder> getPOsWithNoAssets() throws SQLException {
		String sql = "select po.id, po.cw_id, po.source_id, s.name from purchase_order po, source s where s.id = po.source_id"
			+ " and po.id not in (select distinct(purchase_order_id) from purchase_order_2_asset) order by po.cw_id, po.source_id, po.last_updated_date desc";
		PreparedStatement ps = con.prepareStatement(sql);
		ResultSet rs = ps.executeQuery();
		List<PurchaseOrder> list = new ArrayList<PurchaseOrder>();
		while (rs.next()) {
			list.add(new PurchaseOrder(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getString(4)));
		}
		ps.close();
		return list;
	}

	private List<PurchaseOrder> getSimilarPOs(PurchaseOrder po) throws SQLException {
		String sql = "select id, cw_id, source_id from contract c where cw_id = ? and source_id = ?"
			+ " and (select count(*) from contract_2_asset where contract_id = c.id) > 0";
		PreparedStatement ps = con.prepareStatement(sql);
		ps.setInt(1, po.getCwId());
		ps.setInt(2, po.getSourceId());

		ResultSet rs = ps.executeQuery();
		List<PurchaseOrder> list = new ArrayList<PurchaseOrder>();
		while (rs.next()) {
			list.add(new PurchaseOrder(rs.getInt(1), rs.getInt(2), rs.getInt(3), null));
		}
		ps.close();
		return list;
	}

	private boolean promptYes(String msg) throws IOException {
		System.out.print("Delete PO? (y/n) > ");
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String line = br.readLine();
		return line.startsWith("y");
	}

	private void deletePurchaseOrder(PurchaseOrder po) throws SQLException {
		con.setAutoCommit(false);

		try {
			String sql = "delete from po_2_condition where po_id = ?";
			PreparedStatement ps = con.prepareStatement(sql);
			ps.setInt(1, po.getId());
			int rows = ps.executeUpdate();
			log("deleted " + rows + " conditions");
			ps.close();

			sql = "delete from purchase_order where id = ?";
			ps = con.prepareStatement(sql);
			ps.setInt(1, po.getId());
			rows = ps.executeUpdate();
			log("deleted " + rows + " po's");
			ps.close();
		}
		catch (SQLException ex) {
			log("Caught SQLException: " + ex);
			con.rollback();
		}

		con.commit();
		con.setAutoCommit(true);
	}

	/*
		String sql = "update asset set credit_line = ? where id IN "
			+ "(select asset_base_id from contract_2_asset where contract_id = ?) and credit_line is null";
		String psql = sql.replaceFirst("\\?", "<" + creditLineCondition.getValue() + ">");
		psql = psql.replace("?", "<" + contract.getId() + ">");
		log("pending: " + psql);
		if (makeChanges) {
			PreparedStatement ps = con.prepareStatement(sql);
			ps.setString(1, creditLineCondition.getValue());
			ps.setInt(2, contract.getId());
			int rows = ps.executeUpdate();
			if (rows == 1) log("fixed creditLine for contract id " + contract.getId());
			ps.close();
		}
	}*/

	/*
	private void fixCreditLine(List<Contract> contracts) throws SQLException, IOException {
		con.setAutoCommit(false);

		for (Contract c : contracts) {
			fixCreditLine(c);
		}

		System.out.print("Hit Enter to commit the above SQL...");
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		br.readLine();

		con.commit();
		con.setAutoCommit(true);
	}*/

	private void log(String msg) {
		if (logToSystemOut) {
			System.out.println(msg);
		}
		else {
			log.info(msg);
		}
	}

	class PurchaseOrder {
		private final int id;
		private final int cwId;
		private final int sourceId;
		private final String sourceName;

		public PurchaseOrder(int id, int cwId, int sourceId, String sourceName) {
			this.id = id;
			this.cwId = cwId;
			this.sourceId = sourceId;
			this.sourceName = sourceName;
		}

		@Override
		public String toString() {
			return "id [" + id + "] cwId [" + cwId + "] sourceId [" + sourceId + "] sourceName [" + sourceName + "]";
		}

		public int getId() { return id; }
		public int getCwId() { return cwId; }
		public int getSourceId() { return sourceId; }
		public String getSourceName() { return sourceName; }
	}
}
