package com.wiley.permissions.common.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;

import org.apache.commons.lang3.BooleanUtils;

import com.wiley.sf.common.lang.StringUtil;
import com.wiley.sf.common.sql.SimpleDBConnect;

/**
 * This utility is obsolete now that we have merged the wintouch schema with the perm schema.
 * Program to check and fix (some) Wintouch data problems.
 * (Data can easily get messed up in Wintouch since there are no foreign key constraints.)
 *
 * @since JDK 1.6
 * @version 5/18/2011
 * @author Steve Markoff
 */
public class FixEnterpriseRefs {

	public static void main(String[] args) throws Exception {
		if (args.length < 2) {
			System.err.println("Usage: <perm con properties file> <wintouch con properties file> [makeChanges (y/n)]");
			System.exit(1);
		}

		SimpleDBConnect permDbConnect = SimpleDBConnect.create(args[0]);
		SimpleDBConnect wintouchDbConnect = SimpleDBConnect.create(args[1]);
		boolean makeChanges = false;
		if (args.length > 2) {
			makeChanges = BooleanUtils.toBoolean(args[2]);
		}
		else {
			System.out.println("Running in read-only mode - run again with 'y' for 3rd param to make changes.");
		}
		// throws various exceptions
		FixEnterpriseRefs prog = new FixEnterpriseRefs(permDbConnect.getConnection(), wintouchDbConnect.getConnection(), makeChanges);
		prog.go();
	}


	private final Connection permCon;
	private final Connection wintouchCon;
	private final boolean makeChanges;

	public FixEnterpriseRefs(Connection permCon, Connection wintouchCon, boolean makeChanges) throws SQLException {
		this.permCon = permCon;
		this.wintouchCon = wintouchCon;
		this.makeChanges = makeChanges;
		permCon.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
		permCon.setAutoCommit(true);
		wintouchCon.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
		wintouchCon.setAutoCommit(true);
	}

	public void go() throws SQLException {
		checkOneToOne();
		checkForOrphanedWebsites();
		checkForOrphanedC2E();  // do before check orphaned contacts
		checkForOrphanedContacts();
		checkForOrphanedA2E_A2C();  // do before check orphaned addresses
		checkForOrphanedAddresses();
		checkForOrphanedEmail();  // do after delete orphaned contacts
	}

	public void checkForOrphanedWebsites() throws SQLException {
		String sql = "select webid from wtwebs where webid not in (select eid from wtentr)";
		ArrayList<Integer> websiteRefs = getWintouchIntegerList(sql);
		System.out.println("found " + websiteRefs.size() + " website records that reference a non-existent Enterprise.");
		if (websiteRefs.size() > 0 && makeChanges) {
			sql = "delete from wtwebs where webid not in (select eid from wtentr)";
			deleteFromWintouch(sql);
		}
	}

	public void checkForOrphanedEmail() throws SQLException {
		String sql = "select emid from wtmail where emid not in (select cid from wtcont)";
		ArrayList<Integer> emailRefs = getWintouchIntegerList(sql);
		System.out.println("found " + emailRefs.size() + " email records that reference a non-existent Contact.");
		if (emailRefs.size() > 0 && makeChanges) {
			sql = "delete from wtmail where emid not in (select cid from wtcont)";
			deleteFromWintouch(sql);
		}
	}

	public void checkForOrphanedC2E() throws SQLException {
		String sql = "select recid from wtenct where reid not in (select eid from wtentr) or rcid not in (select cid from wtcont)";
		ArrayList<Integer> c2eRefs = getWintouchIntegerList(sql);
		System.out.println("found " + c2eRefs.size() + " contact2Enterprise records that are not connected to an Enterprise or Contact.");
		if (c2eRefs.size() > 0 && makeChanges) {
			sql = "delete from wtenct where where reid not in (select eid from wtentr) or rcid not in (select cid from wtcont)";
			deleteFromWintouch(sql);
		}
	}

	public void checkForOrphanedContacts() throws SQLException {
		String sql = "select cid from wtcont where cid not in (select rcid from wtenct)";
		ArrayList<Integer> contactRefs = getWintouchIntegerList(sql);
		System.out.println("found " + contactRefs.size() + " contact records that are not associated to an Enterprise.");
		if (contactRefs.size() > 0 && makeChanges) {
			sql = "delete from wtcont where cid not in (select rcid from wtenct)";
			deleteFromWintouch(sql);
		}
	}

	public void checkForOrphanedA2E_A2C() throws SQLException {
		String sql = "select * from wtradr where radid not in (select adid from wtaddr) "
			+ "or (raetid not in (select eid from wtentr) and raetid not in (select cid from wtcont))";
		ArrayList<Integer> a2xxRefs = getWintouchIntegerList(sql);
		System.out.println("found " + a2xxRefs.size() + " a2xx records that are not connected to an Address or (Enterprise or Contact).");
		if (a2xxRefs.size() > 0 && makeChanges) {
			sql = "delete from wtradr where radid not in (select adid from wtaddr) "
				+ "or (raetid not in (select eid from wtentr) and raetid not in (select cid from wtcont))";
			deleteFromWintouch(sql);
		}
	}

	public void checkForOrphanedAddresses() throws SQLException {
		String sql = "select adid from wtaddr where adid not in (select radid from wtradr)";
		ArrayList<Integer> addressRefs = getWintouchIntegerList(sql);
		System.out.println("found " + addressRefs.size() + " address records that are not associated to an Enterprise or Contact.");
		if (addressRefs.size() > 0 && makeChanges) {
			sql = "delete from wtaddr where adid not in (select radid from wtradr)";
			deleteFromWintouch(sql);
		}
	}

	private ArrayList<Integer> getWintouchIntegerList(String selectSql) throws SQLException {
		PreparedStatement ps = wintouchCon.prepareStatement(selectSql);

		ResultSet rs = ps.executeQuery();
		ArrayList<Integer> list = new ArrayList<Integer>();

		while (rs.next()) {
			list.add(new Integer(rs.getInt(1)));
		}
		ps.close();
		return list;
	}

	private void deleteFromWintouch(String deleteSql) throws SQLException {
		PreparedStatement ps = wintouchCon.prepareStatement(deleteSql);
		int numRows = ps.executeUpdate();
		System.out.println("" + numRows + " deleted.");
		ps.close();
	}

	public void checkOneToOne() throws SQLException {
		HashSet<String> permSet = getSourceExternalIds();
		HashSet<String> wintouchSet = getEnterpriseExternalIds();
		HashSet<String> notInPerm = new HashSet<String>();
		HashSet<String> notInWintouch = new HashSet<String>();
		notInPerm.addAll(wintouchSet);
		notInPerm.removeAll(permSet);
		notInWintouch.addAll(permSet);
		notInWintouch.removeAll(wintouchSet);

		System.out.println("# External names that are in wintouch but not in perm: " + notInPerm.size());
		if (notInPerm.size() > 0) {
			StringUtil.collectionToString(notInPerm, "\r\n");
		}

		System.out.println("# External names that are in perm but not in wintouch: " + notInWintouch.size());
		if (notInWintouch.size() > 0){
			StringUtil.collectionToString(notInWintouch, "\r\n");
		}
	}

	private HashSet<String> getSourceExternalIds() throws SQLException {
		String sql = "select external_id from source";
		PreparedStatement ps = permCon.prepareStatement(sql);
		ResultSet rs = ps.executeQuery();
		HashSet<String> set = new HashSet<String>();
		while (rs.next()) {
			rs.getString(1);
		}
		ps.close();
		return set;
	}

	private HashSet<String> getEnterpriseExternalIds() throws SQLException {
		String sql = "select U01547 from wtentr";
		PreparedStatement ps = wintouchCon.prepareStatement(sql);
		ResultSet rs = ps.executeQuery();
		HashSet<String> set = new HashSet<String>();
		while (rs.next()) {
			rs.getString(1);
		}
		ps.close();
		return set;
	}
}
