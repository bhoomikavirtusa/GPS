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
 * This program takes care of the situation where there are contracts with no assets.
 * It deletes them as long as there is another contract for the same common work
 * and  source that does have assets. (Also checks if has file that the
 * other contract has the same file so don't lose it.)
 *
 * @since JDK 1.6
 * @version 2/10/2014
 * @author Steve Markoff
 */
public class FixContracts2 {

	private static final Log log = LogFactory.getLog(FixContracts2.class);

	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			System.err.println("Usage: <properties file>");
			System.exit(1);
		}

		SimpleDBConnect dbConnect = SimpleDBConnect.create(args[0]);
		// throws various exceptions
		FixContracts2 prog = new FixContracts2(
				dbConnect.getConnection(), true);
		prog.go();
	}

	private final Connection con;
	private final boolean logToSystemOut;
	//private final boolean makeChanges = true;


	public FixContracts2(Connection con, boolean logToSystemOut) throws SQLException {
		this.con = con;
		this.logToSystemOut = logToSystemOut;
		con.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
		con.setAutoCommit(true);
	}

	public void go() throws SQLException, IOException {
		long startTime = System.currentTimeMillis();

		List<Contract> list = getContractsWithNoAssets();
		log("found " + list.size() + " contracts with no assets.");

		for (int i = 0; i < list.size(); i++) {
			Contract contract = list.get(i);
			process(contract, i);
		}

		long time = System.currentTimeMillis() - startTime;
		TimeFormat timeFormat = new TimeFormat();
		log("total time was " + timeFormat.formatMS(time));
	}

	private void process(Contract contract, int i) throws SQLException {
		log("---- processing Contract: " + contract);
		List<Contract> similarList = getSimilarContracts(contract);
		log("# other contracts with same cwId and sourceId and having assets: " + similarList.size());
		boolean delete = false;
		if (similarList.size() == 0) {
			if (fileSize(contract) > 0) {
				log("Contract has file and No other contract for same cwId and sourceId and having assets found so doing nothing.");
			}
			else {
				delete = true;
			}
		}
		else {
			int fileSize = fileSize(contract);
			if (fileSize > 0) {
				if (oneContractHasFileOfSize(similarList, fileSize)) {
					delete = true;
				}
				else {
					log("Has file not matched in size by other contracts so skipping.");
				}
			}
			else {
				delete = true;
			}
		}

		if (delete) {
			deleteContract(contract);
		}
	}

	private List<Contract> getContractsWithNoAssets() throws SQLException {
		String sql = "select id, cw_id, source_id from contract where id not in "
			+ "(select distinct(contract_id) from contract_2_asset) order by cw_id, source_id, last_updated_date desc";
		PreparedStatement ps = con.prepareStatement(sql);
		ResultSet rs = ps.executeQuery();
		List<Contract> list = new ArrayList<Contract>();
		while (rs.next()) {
			list.add(new Contract(rs.getInt(1), rs.getInt(2), rs.getInt(3)));
		}
		ps.close();
		return list;
	}

	private List<Contract> getSimilarContracts(Contract contract) throws SQLException {
		String sql = "select id, cw_id, source_id from contract c where cw_id = ? and source_id = ?"
			+ " and (select count(*) from contract_2_asset where contract_id = c.id) > 0";
		PreparedStatement ps = con.prepareStatement(sql);
		ps.setInt(1, contract.getCwId());
		ps.setInt(2, contract.getSourceId());

		ResultSet rs = ps.executeQuery();
		List<Contract> list = new ArrayList<Contract>();
		while (rs.next()) {
			list.add(new Contract(rs.getInt(1), rs.getInt(2), rs.getInt(3)));
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

	private void deleteContract(Contract contract) throws SQLException {
		con.setAutoCommit(false);

		try {
			String sql = "delete from contract_2_condition where contract_id = ?";
			PreparedStatement ps = con.prepareStatement(sql);
			ps.setInt(1, contract.getId());
			int rows = ps.executeUpdate();
			log("deleted " + rows + " conditions");
			ps.close();

			sql = "delete from contract_file where contract_id = ?";
			ps = con.prepareStatement(sql);
			ps.setInt(1, contract.getId());
			rows = ps.executeUpdate();
			log("deleted " + rows + " contract files");
			ps.close();

			sql = "delete from amendment where contract_id = ?";
			ps = con.prepareStatement(sql);
			ps.setInt(1, contract.getId());
			rows = ps.executeUpdate();
			log("deleted " + rows + " amendments");
			ps.close();

			sql = "delete from contract where id = ?";
			ps = con.prepareStatement(sql);
			ps.setInt(1, contract.getId());
			rows = ps.executeUpdate();
			log("deleted " + rows + " contracts");
			ps.close();
		}
		catch (SQLException ex) {
			log("Caught SQLException: " + ex);
			con.rollback();
		}

		con.commit();
		con.setAutoCommit(true);
	}

	private int fileSize(Contract contract) throws SQLException {
		String sql = "select id, length(file_data) from contract_file where contract_id = ?";
		PreparedStatement ps = con.prepareStatement(sql);
		ps.setInt(1, contract.getId());
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
		String sql = "select id from contract_file where contract_id in (" + sb + ") and length(file_data) = ?";
		PreparedStatement ps = con.prepareStatement(sql);
		ps.setInt(1, fileSize);
		ResultSet rs = ps.executeQuery();
		boolean hasRow = rs.next();
		ps.close();
		return hasRow;
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

	class Contract {
		private final int id;
		private final int cwId;
		private final int sourceId;

		public Contract(int id, int cwId, int sourceId) {
			this.id = id;
			this.cwId = cwId;
			this.sourceId = sourceId;
		}

		@Override
		public String toString() {
			return "id [" + id + "] cwId [" + cwId + "] sourceId [" + sourceId + "]";
		}

		public int getId() { return id; }
		public int getCwId() { return cwId; }
		public int getSourceId() { return sourceId; }
	}
}
