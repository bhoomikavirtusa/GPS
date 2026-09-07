package com.wiley.permissions.common.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.sf.common.sql.SimpleDBConnect;
import com.wiley.sf.common.text.TimeFormat;

/**
 * Created 4/2014. This is a temporary program (can eventually be deleted).
 *
 * Might want to run this SQL before running this program:
 * delete from contract_2_condition where contract_id in (select id from contract where is_permission_form is true);
 *
 * @since JDK 1.6
 * @author Steve Markoff
 */
public class ConvertConditions5 {

	private static final Log log = LogFactory.getLog(ConvertConditions5.class);

	// have to duplicate constants from ConditionType
	// because we can't reference the persistence package from this one

	private static final String
		MEDIUM = "medium",
		MEDIUM_ALL = "medium_all";

	private static final String
		SALES = "sales",
		SALES_WORLD_ALIAS = "sales_world_alias";

	private static final String
		LANGUAGE = "language",
		LANGUAGE_ALL_ALIAS = "language_all_alias";

	private static final String
		PRINT_RUN = "print_run",
		PRINT_RUN_UNLIMITED = "print_run_unlimited";

	private static final String
		EDITION = "edition",
		EDITION_CURRENT_FUTURE = "edition_all_c_and_f";

	private static final String
		DWORK = "dwork",
		DWORK_ALL = "dwork_all";

	private static final String
		SUBLICENSE = "sublicense",
		SUBLICENSE_RIGHT = "sublicense_right";

	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			System.err.println("Usage: <properties file>");
			System.exit(1);
		}

		SimpleDBConnect dbConnect = SimpleDBConnect.create(args[0]);
		// throws various exceptions
		ConvertConditions5 prog = new ConvertConditions5(
				dbConnect.getConnection(), true);
		prog.go();
	}

	private final Connection con;
	private final boolean logToSystemOut;
	private int addedConditionsCount = 0;

	private final File specialFile = new File("C:\\temp\\convertConditions5.txt");
	private final PrintWriter specialWriter;


	public ConvertConditions5(Connection con, boolean logToSystemOut) throws SQLException, FileNotFoundException {
		this.con = con;
		this.logToSystemOut = logToSystemOut;
		con.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
		con.setAutoCommit(true);

		specialWriter = new PrintWriter(specialFile);
	}

	public void go() throws SQLException, IOException {
		NumberFormat intFormat = NumberFormat.getIntegerInstance();
		TimeFormat timeFormat = new TimeFormat();
		long startTime = System.currentTimeMillis();
		long time;

		List<Integer> idList = getContractIds();

		for (int i = 0; i < idList.size(); i++) {
			Integer id = idList.get(i);
			if (i % 5000 == 0) {
				time = System.currentTimeMillis() - startTime;
				log("-- " + intFormat.format(i) + " of " + intFormat.format(idList.size())
						+ " completed so far, " + timeFormat.formatMS(time) + " elapsed");
			}
			process(id, i);
		}

		log("idList.size() = " + intFormat.format(idList.size()));
		log("addedConditionsCount = " + intFormat.format(addedConditionsCount));

		time = System.currentTimeMillis() - startTime;
		log("total time was " + timeFormat.formatMS(time));
	}

	private List<Integer> getContractIds() throws SQLException {
		String sql = "select id from contract where is_permission_form is true"
			+ " and id not in (select contract_id from contract_2_condition) order by id";

		PreparedStatement ps = con.prepareStatement(sql);
		ResultSet rs = ps.executeQuery();
		List<Integer> list = new ArrayList<Integer>();
		while (rs.next()) {
			list.add(rs.getInt(1));
		}
		ps.close();
		return list;
	}

	private void process(Integer id, int i) throws SQLException, IOException {
		log("Adding conditions for contract id = " + id);
		//promptYes("continue?");

		con.setAutoCommit(false);

		addCondition(id, MEDIUM_ALL, "true", null);
		addCondition(id, SALES_WORLD_ALIAS, "true", null);
		addCondition(id, LANGUAGE_ALL_ALIAS, "true", null);
		addCondition(id, PRINT_RUN_UNLIMITED, "true", null);
		addCondition(id, EDITION_CURRENT_FUTURE, "true", null);
		addCondition(id, DWORK_ALL, "true", null);
		addCondition(id, SUBLICENSE_RIGHT, "true", null);

		addCondition(id, MEDIUM, null, "All media types including future types");
		addCondition(id, SALES, null, "Worldwide");
		addCondition(id, LANGUAGE, null, "All Languages");
		addCondition(id, PRINT_RUN, null, "Unlimited print run is granted");
		addCondition(id, EDITION, null, "Granted for this edition and all future editions");
		addCondition(id, DWORK, null, "Wiley can include the asset(s) in any ancillaries, derivatives and custom works");
		addCondition(id, SUBLICENSE, null, "Wiley can include the asset(s) when sub-licensing product");

		con.commit();
		addedConditionsCount++;
		con.setAutoCommit(true);
		//promptYes("Adding conditions complete, continue?");
	}

	private int addCondition(int objectId, String conditionType, String value, String rollupValue) throws SQLException {
		String sql = "insert into condition_value (condition_type, value, rollup_value) values (?, ?, ?)";
		PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
		ps.setString(1, conditionType);
		ps.setString(2, value);
		ps.setString(3, rollupValue);
		ps.executeUpdate();

		ResultSet genKeys = ps.getGeneratedKeys();
		genKeys.next();
		int genId = genKeys.getInt(1);
		genKeys.close();
		ps.close();

		//log("generated condition id = " + genId);

		sql = "insert into contract_2_condition (contract_id, condition_id) values (?, ?)";
		ps = con.prepareStatement(sql);
		ps.setInt(1, objectId);
		ps.setInt(2, genId);
		ps.executeUpdate();
		ps.close();

		return genId;
	}

	@SuppressWarnings("unused")
	private boolean promptYes(String msg) throws IOException {
		System.out.print(msg + " (y/n) > ");
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String line = br.readLine();
		return line.startsWith("y");
	}

	private void log(String msg) {
		if (logToSystemOut) {
			System.out.println(msg);
		}
		else {
			log.info(msg);
		}
	}

	@SuppressWarnings("unused")
	private void logSpecial(String msg) {
		log(msg);
		specialWriter.println(msg);
	}

	class Condition {
		private final int id;
		private final String conditionType;
		//private final Integer assetBaseId;
		private final String value;
		//private final String rollupValue;
		//private final boolean rollupNoValue;

		public Condition(int id, String conditionType, String value) {
			this.id = id;
			this.conditionType = conditionType;
			//this.assetBaseId = assetBaseId;
			this.value = value;
			//this.rollupValue = rollupValue;
			//this.rollupNoValue = rollupNoValue;
		}

		@Override
		public String toString() {
			return "id [" + id + "] conditionType [" + conditionType + "] value [" + value + "]";
		}

		public int getId() { return id; }
		public String getConditionType() { return conditionType; }
		public String getValue() { return value; }

		public boolean isValueTrue() {
			return "true".equals(value);
		}
	}
}
