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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wiley.sf.common.sql.SimpleDBConnect;
import com.wiley.sf.common.text.TimeFormat;

/**
 * Created 3/2014. This is a temporary program (can eventually be deleted).
 *
 * @since JDK 1.6
 * @author Steve Markoff
 */
public class ConvertConditions4 {

	private static final Log log = LogFactory.getLog(ConvertConditions4.class);

	// have to duplicate constants from ConditionType
	// because we can't reference the persistence package from this one

	private static final String
		MEDIUM = "medium",
		MEDIUM_ALL = "medium_all",
		MEDIUM_NO_MENTION = "medium_no_mention",
		MEDIUM_ALL_PHYSICAL = "medium_all_physical",
		MEDIUM_PAPER = "medium_paper",  // old condition
		MEDIUM_CLOTH = "medium_cloth",  // old condition
		MEDIUM_CDROM = "medium_cdrom",  // old condition
		//MEDIUM_ALL_ELECTRONIC = "medium_all_elect",  // old condition
		MEDIUM_EBOOK = "medium_ebook",  // old condition
		MEDIUM_PRINT_ONLY = "medium_print_only",  // new condition
		MEDIUM_PHYSICAL_ELECTRONIC = "medium_physical_electronic",  // new condition
		MEDIUM_PHYSICAL_EBOOK_WEB = "medium_physical_ebook_web",  // new condition
		//MEDIUM_ALL_ONLINE_DELIVERY = "medium_all_online_d",  // old condition
		MEDIUM_WILEY_HOSTED = "medium_wiley_hosted";  // old condition
		//MEDIUM_CUSTOMER_HOSTED = "medium_cust_hosted";  // old condition

	private static final String
		SALES = "sales",
		SALES_NORTH_AMERICA = "sales_north_america",
		SALES_NORTH_AMERICA_ALIAS = "sales_north_america_alias",
		SALES_WORLD = "sales_world",
		SALES_WORLD_ALIAS = "sales_world_alias",
		SALES_NO_MENTION = "sales_no_mention",
		SALES_US = "sales_us",
		SALES_CA = "sales_ca",
		SALES_MX = "sales_mx";

	private static final String
		LANGUAGE = "language",
		LANGUAGE_ENGLISH = "language_eng",
		LANGUAGE_ENGLISH_ALIAS = "language_eng_alias",
		LANGUAGE_ALL = "language_all",
		LANGUAGE_ALL_ALIAS = "language_all_alias",
		LANGUAGE_NO_MENTION = "language_no_mention";

	private static final String
		PRINT_RUN = "print_run",
		PRINT_RUN_UNLIMITED = "print_run_unlimited",
		PRINT_RUN_NO_MENTION = "print_run_no_mention",
		PRINT_RUN_LIMIT = "print_run_limit",
		PRINT_RUN_LIMIT_BOX = "print_run_limit_box";

	private static final String
		EDITION = "edition",
		EDITION_NO_MENTION = "edition_no_mention",
		EDITION_PRODUCT_FAMILY = "edition_pf_for_this",  // old condition
		EDITION_CURRENT_FUTURE = "edition_all_c_and_f",
		EDITION_THIS = "edition_this";

	private static final String
		DWORK = "dwork",
		DWORK_NO_MENTION = "dwork_no_mention",
		DWORK_ALL = "dwork_all",
		DWORK_ANC_AND_DERIV = "dwork_anc_and_deriv",
		DWORK_CUSTOM = "dwork_custom",  // old condition
		DWORK_ISV = "dwork_isv";  // old condition

	private static final String
		SUBLICENSE = "sublicense",
		SUBLICENSE_NO_MENTION = "sublicense_no_mention";

	public enum ObjectType { CONTRACT, MA_DEAL, CW }

	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			System.err.println("Usage: <properties file>");
			System.exit(1);
		}

		SimpleDBConnect dbConnect = SimpleDBConnect.create(args[0]);
		// throws various exceptions
		ConvertConditions4 prog = new ConvertConditions4(
				dbConnect.getConnection(), true);
		System.out.println("---- ma deals first ----");
		prog.go(ObjectType.MA_DEAL);
		System.out.println("---- now contracts ----");
		prog.go(ObjectType.CONTRACT);
		System.out.println("---- now cw conditions ----");
		prog.go(ObjectType.CW);
	}

	private final Connection con;
	private final boolean logToSystemOut;
	private int switchedMediumNothingCount = 0;
	private int switchedMediumOtherCount = 0;
	private int mediumNotHandledCount = 0;
	private int switchedNorthAmericaCount = 0;
	private int switchedSalesWorldCount = 0;
	private int switchedSalesNothingCount = 0;
	private int switchedEnglishCount = 0;
	private int switchedLanguageAllCount = 0;
	private int switchedLanguageNothingCount = 0;
	private int switchedPrintRunNothingCount = 0;
	private int modifyPrintRunCount = 0;
	private int switchedEditionProductFamilyCount = 0;
	private int switchedEditionNothingCount = 0;
	private int switchedDWorkCustomCount = 0;
	private int switchedDWorkISVCount = 0;
	private int switchedDWorkNothingCount = 0;
	private int switchedSublicenseNothingCount = 0;

	private final File specialFile = new File("C:\\temp\\convertConditions4.txt");
	private final PrintWriter specialWriter;


	public ConvertConditions4(Connection con, boolean logToSystemOut) throws SQLException, FileNotFoundException {
		this.con = con;
		this.logToSystemOut = logToSystemOut;
		con.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
		con.setAutoCommit(true);

		specialWriter = new PrintWriter(specialFile);
	}

	public void go(ObjectType objectType) throws SQLException, IOException {
		// reset counters because call this method twice
		switchedMediumNothingCount = 0;
		switchedMediumOtherCount = 0;
		mediumNotHandledCount = 0;
		switchedNorthAmericaCount = 0;
		switchedSalesWorldCount = 0;
		switchedSalesNothingCount = 0;
		switchedEnglishCount = 0;
		switchedLanguageAllCount = 0;
		switchedLanguageNothingCount = 0;
		switchedPrintRunNothingCount = 0;
		modifyPrintRunCount = 0;
		switchedEditionProductFamilyCount = 0;
		switchedEditionNothingCount = 0;
		switchedDWorkCustomCount = 0;
		switchedDWorkISVCount = 0;
		switchedDWorkNothingCount = 0;
		switchedSublicenseNothingCount = 0;

		NumberFormat intFormat = NumberFormat.getIntegerInstance();
		TimeFormat timeFormat = new TimeFormat();
		long startTime = System.currentTimeMillis();
		long time;

		List<Integer> idList = getObjectIds(objectType);

		for (int i = 0; i < idList.size(); i++) {
			Integer id = idList.get(i);
			if (i % 5000 == 0) {
				time = System.currentTimeMillis() - startTime;
				log("-- " + intFormat.format(i) + " of " + intFormat.format(idList.size())
						+ " completed so far, " + timeFormat.formatMS(time) + " elapsed");
			}
			process(id, i, objectType);
		}

		log("idList.size() = " + intFormat.format(idList.size()));
		log("switchedMediumNothingCount = " + intFormat.format(switchedMediumNothingCount));
		log("switchedMediumOtherCount = " + intFormat.format(switchedMediumOtherCount));
		log("mediumNotHandledCount = " + intFormat.format(mediumNotHandledCount));
		log("switchedNorthAmericaCount = " + intFormat.format(switchedNorthAmericaCount));
		log("switchedSalesWorldCount = " + intFormat.format(switchedSalesWorldCount));
		log("switchedSalesNothingCount = " + intFormat.format(switchedSalesNothingCount));
		log("switchedEnglishCount = " + intFormat.format(switchedEnglishCount));
		log("switchedLanguageAllCount = " + intFormat.format(switchedLanguageAllCount));
		log("switchedLanguageNothingCount = " + intFormat.format(switchedLanguageNothingCount));
		log("switchedPrintRunNothingCount = " + intFormat.format(switchedPrintRunNothingCount));
		log("modifyPrintRunCount = " + intFormat.format(modifyPrintRunCount));
		log("switchedEditionProductFamilyCount = " + intFormat.format(switchedEditionProductFamilyCount));
		log("switchedEditionNothingCount = " + intFormat.format(switchedEditionNothingCount));
		log("switchedDWorkCustomCount = " + intFormat.format(switchedDWorkCustomCount));
		log("switchedDWorkISVCount = " + intFormat.format(switchedDWorkISVCount));
		log("switchedDWorkNothingCount = " + intFormat.format(switchedDWorkNothingCount));
		log("switchedSublicenseNothingCount = " + intFormat.format(switchedSublicenseNothingCount));

		time = System.currentTimeMillis() - startTime;
		log("total time was " + timeFormat.formatMS(time));
	}

	private List<Integer> getObjectIds(ObjectType objectType) throws SQLException {
		String sql;
		if (objectType == ObjectType.CONTRACT) {
			sql = "select id from contract where is_permission_form = 0 order by id";
		}
		else if (objectType == ObjectType.MA_DEAL) {
			sql = "select id from master_agreement_deal order by id";
		}
		else {
			sql = "select id from common_work order by id";
		}
		PreparedStatement ps = con.prepareStatement(sql);
		ResultSet rs = ps.executeQuery();
		List<Integer> list = new ArrayList<Integer>();
		while (rs.next()) {
			list.add(rs.getInt(1));
		}
		ps.close();
		return list;
	}

	private void process(Integer id, int i, ObjectType objectType) throws SQLException, IOException {
		//log("-- processing " + objectType + " id " + id);

		Map<String, Condition> map = getConditions(id, objectType);

		Map<String, Condition> trueMap = filterTrueConditions(map, "medium_");
		checkNoMedium(id, objectType, map, trueMap);
		checkOtherMediumSituations(id, objectType, map, trueMap);

		trueMap = filterTrueConditions(map, "sales_");
		checkNorthAmericaOnly(id, objectType, map, trueMap);
		checkSalesWorld(id, objectType, map, trueMap);
		checkNoSales(id, objectType, map, trueMap);

		trueMap = filterTrueConditions(map, "language_");
		checkEnglishOnly(id, objectType, map, trueMap);
		checkAllLanguages(id, objectType, map, trueMap);
		checkNoLanguage(id, objectType, map, trueMap);

		Map<String, Condition> notNullMap = filterNotNullConditions(map, "print_run_");
		checkNoPrintRun(id, objectType, map, notNullMap);
		checkPrintRun(id, objectType, map, notNullMap);

		trueMap = filterTrueConditions(map, "edition_");
		Map<String, Condition> dworkTrueMap = filterTrueConditions(map, "dwork_");
		// When one routine changes something in the DB, remember it doesn't affect the map
		// so for example here we need a boolean flag to keep track of the fact that we made a change
		// which will affect another routine
		boolean dWorkSet = checkEditionProductFamily(id, objectType, map, trueMap, dworkTrueMap);
		checkNoEdition(id, objectType, map, trueMap);

		if (!dWorkSet) {
			dWorkSet = checkDWorkCustom(id, objectType, map, dworkTrueMap);
		}
		if (!dWorkSet) {
			dWorkSet = checkDWorkISV(id, objectType, map, dworkTrueMap);
		}
		if (!dWorkSet) {
			checkNoDWork(id, objectType, map, dworkTrueMap);
		}

		trueMap = filterTrueConditions(map, "sublicense_");
		checkNoSublicense(id, objectType, map, trueMap);
	}

	private void checkNoMedium(int objectId, ObjectType objectType, Map<String, Condition> map, Map<String, Condition> trueMap)
	throws SQLException, IOException {
		if (trueMap.size() > 0) return;

		String condName = (objectType == ObjectType.CW) ? "All" : "No Mention";
		log("Setting Medium " + condName + " for " + objectType + " id = " + objectId);
		//promptYes("continue?");

		con.setAutoCommit(false);
		deleteAllConditionsExcept(objectType, map, "medium_", "medium");
		String codeToAdd = (objectType == ObjectType.CW) ? MEDIUM_ALL : MEDIUM_NO_MENTION;
		addCondition(objectId, codeToAdd, "true", objectType);
		Condition medium = map.get(MEDIUM);
		String rollupValue = (objectType == ObjectType.CW) ? "All media types including future types" : "No mention of media types";
		if (medium == null) {
			int genId = addCondition(objectId, MEDIUM, null, objectType);
			updateConditionRollupValue(genId, rollupValue);
		}
		else {
			updateConditionRollupValue(medium.getId(), rollupValue);
		}
		con.commit();
		switchedMediumNothingCount++;
		con.setAutoCommit(true);
		//promptYes("Medium Nothing update complete, continue?");
	}

	private void checkOtherMediumSituations(int objectId, ObjectType objectType, Map<String, Condition> map, Map<String, Condition> trueMap)
	throws SQLException, IOException {
		if (trueMap.size() == 0) return;  // in this case would have already been handled by checkNoMedium()

		Condition mediumNoMention = trueMap.get(MEDIUM_NO_MENTION);
		if (mediumNoMention != null) return;  // in this case already handled by checkNoMedium() in previous run of program

		Condition mediumPrintOnly = trueMap.get(MEDIUM_PRINT_ONLY);
		if (mediumPrintOnly != null) return;  // in this case already handled by this method in previous run of program

		Condition mediumPhysicalElectronic = trueMap.get(MEDIUM_PHYSICAL_ELECTRONIC);
		if (mediumPhysicalElectronic != null) return;  // in this case already handled by this method in previous run of program

		Condition mediumPhysicalEbookWeb = trueMap.get(MEDIUM_PHYSICAL_EBOOK_WEB);
		if (mediumPhysicalEbookWeb != null) return;  // in this case already handled by this method in previous run of program

		Condition mediumAll = trueMap.get(MEDIUM_ALL);
		if (mediumAll != null) {
			if (trueMap.size() > 1) {
				log("Deleting extra medium(s) besides All for " + objectType + " id = " + objectId);
				//promptYes("continue?");
				// other ones that used to be under All but are now at same level will be set so delete them
				// don't bother turning auto commit off because usually just one extra condition set
				deleteAllConditionsExcept(objectType, map, "medium_", MEDIUM_ALL);
				switchedMediumOtherCount++;
			}
			return;
		}

		Condition medium = map.get(MEDIUM);
		Condition mediumAllPhysical = trueMap.get(MEDIUM_ALL_PHYSICAL);
		Condition mediumPaper = trueMap.get(MEDIUM_PAPER);
		Condition mediumCloth = trueMap.get(MEDIUM_CLOTH);
		Condition mediumCdrom = trueMap.get(MEDIUM_CDROM);

		if (mediumAllPhysical != null && trueMap.size() == 1) return;  // in this case already handled by this method in previous run of program

		// trueMap.size() == 4 below ensures that nothing besides AllPhysical (and paper, cloth, cdrom below) is checked
		// checking for paper, etc is redundant (AllPhysical includes that) but maybe little safer
		if (mediumAllPhysical != null && mediumPaper != null && mediumCloth != null && mediumCdrom != null && trueMap.size() == 4) {
			// leave as is except update rollup and delete sub-nodes so won't update rollup again if run again
			log("Converting Medium All Physical to single node and update rollup for " + objectType + " id = " + objectId);
			//promptYes("continue?");
			con.setAutoCommit(false);
			updateConditionRollupValue(medium.getId(), "All physical media including print and CDROM");
			deleteAllConditionsExcept(objectType, map, "medium_", MEDIUM_ALL_PHYSICAL);
			con.commit();
			switchedMediumOtherCount++;
			con.setAutoCommit(true);
			return;
		}

		Condition mediumEbook = trueMap.get(MEDIUM_EBOOK);
		Condition mediumWileyHosted = trueMap.get(MEDIUM_WILEY_HOSTED);
		//Condition mediumCustomerHosted = trueMap.get(MEDIUM_CUSTOMER_HOSTED);
		//Condition mediumAllElectronic = trueMap.get(MEDIUM_ALL_ELECTRONIC);
		//Condition mediumAllOnlineDelivery = trueMap.get(MEDIUM_ALL_ONLINE_DELIVERY);

		if (((mediumPaper != null || mediumCloth != null) && trueMap.size() == 1)
			|| (mediumPaper != null && mediumCloth != null && trueMap.size() == 2)) {
			log("Converting Medium Paper and/or Cloth to Print only for " + objectType + " id = " + objectId);
			//promptYes("continue?");
			con.setAutoCommit(false);
			deleteAllConditionsExcept(objectType, map, "medium_", "medium");
			addCondition(objectId, MEDIUM_PRINT_ONLY, "true", objectType);

			String rollupValue = "Print only";
			if (medium == null) {
				int genId = addCondition(objectId, MEDIUM, null, objectType);
				updateConditionRollupValue(genId, rollupValue);
			}
			else {
				updateConditionRollupValue(medium.getId(), rollupValue);
			}

			con.commit();
			switchedMediumOtherCount++;
			con.setAutoCommit(true);
			return;
		}

		if ((mediumEbook != null && trueMap.size() == 1)
			|| (mediumEbook != null && mediumPaper != null && trueMap.size() == 2)
			|| (mediumEbook != null && mediumCdrom != null && trueMap.size() == 2)
			|| (mediumPaper != null && mediumCloth != null && mediumEbook != null && trueMap.size() == 3)
			|| (mediumPaper != null && mediumCloth != null && mediumCdrom != null && mediumAllPhysical != null && mediumEbook != null && trueMap.size() == 5)) {
			log("Converting Medium to All physical and electronic media for " + objectType + " id = " + objectId);
			//promptYes("continue?");
			con.setAutoCommit(false);
			deleteAllConditionsExcept(objectType, map, "medium_", "medium");
			addCondition(objectId, MEDIUM_PHYSICAL_ELECTRONIC, "true", objectType);

			String rollupValue = "All physical and electronic media";
			if (medium == null) {
				int genId = addCondition(objectId, MEDIUM, null, objectType);
				updateConditionRollupValue(genId, rollupValue);
			}
			else {
				updateConditionRollupValue(medium.getId(), rollupValue);
			}

			con.commit();
			switchedMediumOtherCount++;
			con.setAutoCommit(true);
			return;
		}

		// There are many combinations of old settings that should be converted to All physical media and ebook/web
		// but they all have mediumWileyHosted in common
		if (mediumWileyHosted != null) {
			log("Converting Medium to All physical media and ebook/web for " + objectType + " id = " + objectId);
			//promptYes("continue?");
			con.setAutoCommit(false);
			deleteAllConditionsExcept(objectType, map, "medium_", "medium");
			addCondition(objectId, MEDIUM_PHYSICAL_EBOOK_WEB, "true", objectType);

			String rollupValue = "All physical media and ebook/web";
			if (medium == null) {
				int genId = addCondition(objectId, MEDIUM, null, objectType);
				updateConditionRollupValue(genId, rollupValue);
			}
			else {
				updateConditionRollupValue(medium.getId(), rollupValue);
			}

			con.commit();
			switchedMediumOtherCount++;
			con.setAutoCommit(true);
			return;
		}

		mediumNotHandledCount++;

		log("Medium situation not handled for " + objectType + " id = " + objectId + " - true conditions:");

		Iterator<Entry<String, Condition>> it = trueMap.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, Condition> pairs = it.next();
			log("value = " + pairs.getValue());
		}

		//promptYes("continue?");
	}

	private void checkEnglishOnly(int objectId, ObjectType objectType, Map<String, Condition> map, Map<String, Condition> trueMap)
	throws SQLException, IOException {
		Condition english = trueMap.get(LANGUAGE_ENGLISH);
		if (english == null) return;

		if (trueMap.size() > 1) {
			//log("English was checked but also other condition(s) so not doing anything (" + objectType + " id " + objectId + ")");
			return;
		}

		log("Converting English to English Alias for " + objectType + " id = " + objectId);
		con.setAutoCommit(false);
		deleteCondition(english.getId(), objectType);
		addCondition(objectId, LANGUAGE_ENGLISH_ALIAS, "true", objectType);
		con.commit();
		switchedEnglishCount++;
		con.setAutoCommit(true);
		//promptYes("English update complete, continue?");
	}

	private void checkNorthAmericaOnly(int objectId, ObjectType objectType, Map<String, Condition> map, Map<String, Condition> trueMap)
	throws SQLException, IOException {
		Condition northAmerica = trueMap.get(SALES_NORTH_AMERICA);
		if (northAmerica == null) return;

		Condition salesUs = trueMap.get(SALES_US);
		Condition salesCa = trueMap.get(SALES_CA);
		Condition salesMx = trueMap.get(SALES_MX);
		if (trueMap.size() == 4 && salesUs != null && salesCa != null && salesMx != null) {
			// continue to section below
		}
		else if (trueMap.size() > 1) {
			//log("North America was checked but also other condition(s) so not doing anything (" + objectType + " id " + objectId + ")");
			return;
		}

		log("Converting North America to North America Alias for " + objectType + " id = " + objectId);
		con.setAutoCommit(false);
		deleteAllConditionsExcept(objectType, map, "sales_", "sales");
		addCondition(objectId, SALES_NORTH_AMERICA_ALIAS, "true", objectType);
		con.commit();
		switchedNorthAmericaCount++;
		con.setAutoCommit(true);
		//promptYes("North America update complete, continue?");
	}

	private void checkAllLanguages(int objectId, ObjectType objectType, Map<String, Condition> map, Map<String, Condition> trueMap)
	throws SQLException, IOException {
		Condition languageAll = trueMap.get(LANGUAGE_ALL);
		if (languageAll == null)  return;

		log("Converting Language All to Alias for " + objectType + " id = " + objectId);
		con.setAutoCommit(false);
		deleteAllConditionsExcept(objectType, map, "language_", "language");
		addCondition(objectId, LANGUAGE_ALL_ALIAS, "true", objectType);
		con.commit();
		switchedLanguageAllCount++;
		con.setAutoCommit(true);
		//promptYes("Language All update complete, continue?");
	}

	private void checkSalesWorld(int objectId, ObjectType objectType, Map<String, Condition> map, Map<String, Condition> trueMap)
	throws SQLException, IOException {
		Condition salesWorld = trueMap.get(SALES_WORLD);
		if (salesWorld == null)  return;

		log("Converting Sales World to Alias for " + objectType + " id = " + objectId);
		con.setAutoCommit(false);
		deleteAllConditionsExcept(objectType, map, "sales_", "sales");
		addCondition(objectId, SALES_WORLD_ALIAS, "true", objectType);
		con.commit();
		switchedSalesWorldCount++;
		con.setAutoCommit(true);
		//promptYes("Sales World update complete, continue?");
	}

	private void checkNoLanguage(int objectId, ObjectType objectType, Map<String, Condition> map, Map<String, Condition> trueMap)
	throws SQLException, IOException {
		if (trueMap.size() > 0) return;

		String condName = (objectType == ObjectType.CW) ? "All" : "No Mention";
		log("Setting Language " + condName + " for " + objectType + " id = " + objectId);

		con.setAutoCommit(false);
		deleteAllConditionsExcept(objectType, map, "language_", "language");
		String codeToAdd = (objectType == ObjectType.CW) ? LANGUAGE_ALL_ALIAS : LANGUAGE_NO_MENTION;
		addCondition(objectId, codeToAdd, "true", objectType);
		Condition language = map.get(LANGUAGE);
		String rollupValue = (objectType == ObjectType.CW) ? "All Languages" : "No mention of language";
		if (language == null) {
			int genId = addCondition(objectId, LANGUAGE, null, objectType);
			updateConditionRollupValue(genId, rollupValue);
		}
		else {
			updateConditionRollupValue(language.getId(), rollupValue);
		}
		con.commit();
		switchedLanguageNothingCount++;
		con.setAutoCommit(true);
		//promptYes("Language Nothing update complete, continue?");
	}

	private void checkNoSales(int objectId, ObjectType objectType, Map<String, Condition> map, Map<String, Condition> trueMap)
	throws SQLException, IOException {
		if (trueMap.size() > 0) return;

		String condName = (objectType == ObjectType.CW) ? "World" : "No Mention";
		log("Setting Sales " + condName + " for " + objectType + " id = " + objectId);

		con.setAutoCommit(false);
		deleteAllConditionsExcept(objectType, map, "sales_", "sales");
		String codeToAdd = (objectType == ObjectType.CW) ? SALES_WORLD_ALIAS : SALES_NO_MENTION;
		addCondition(objectId, codeToAdd, "true", objectType);
		Condition sales = map.get(SALES);
		String rollupValue = (objectType == ObjectType.CW) ? "Worldwide" : "No mention of distribution/sales territories";
		if (sales == null) {
			int genId = addCondition(objectId, SALES, null, objectType);
			updateConditionRollupValue(genId, rollupValue);
		}
		else {
			updateConditionRollupValue(sales.getId(), rollupValue);
		}
		con.commit();
		switchedSalesNothingCount++;
		con.setAutoCommit(true);
		//promptYes("Sales Nothing update complete, continue?");
	}

	private void checkNoPrintRun(int objectId, ObjectType objectType, Map<String, Condition> map, Map<String, Condition> notNullMap)
	throws SQLException, IOException {
		if (notNullMap.size() > 0) return;

		String condName = (objectType == ObjectType.CW) ? "Unlimited" : "No Mention";
		log("Setting Print Run " + condName + " for " + objectType + " id = " + objectId);

		con.setAutoCommit(false);
		deleteAllConditionsExcept(objectType, map, "print_run_", "print_run_ebook");
		String codeToAdd = (objectType == ObjectType.CW) ? PRINT_RUN_UNLIMITED : PRINT_RUN_NO_MENTION;
		addCondition(objectId, codeToAdd, "true", objectType);
		Condition printRun = map.get(PRINT_RUN);
		String rollupValue = (objectType == ObjectType.CW) ? "Unlimited print run is granted" : "Print run is not mentioned";
		if (printRun == null) {
			int genId = addCondition(objectId, PRINT_RUN, null, objectType);
			updateConditionRollupValue(genId, rollupValue);
		}
		else {
			updateConditionRollupValue(printRun.getId(), rollupValue);
		}
		con.commit();
		switchedPrintRunNothingCount++;
		con.setAutoCommit(true);
		//promptYes("Print Run Nothing update complete, continue?");
	}

	private void checkPrintRun(int objectId, ObjectType objectType, Map<String, Condition> map, Map<String, Condition> notNullMap)
	throws SQLException, IOException {
		Condition printRunLimitBox = notNullMap.get(PRINT_RUN_LIMIT_BOX);
		if (printRunLimitBox == null)  return;

		// check if already have printRunLimit, if so, we've already run this logic
		Condition printRunLimit = notNullMap.get(PRINT_RUN_LIMIT);
		if (printRunLimit != null)  return;

		log("Checking Print Run Limit Box [" + printRunLimitBox.getValue() + "] for " + objectType + " id = " + objectId);
		int value = Integer.parseInt(printRunLimitBox.getValue());
		con.setAutoCommit(false);
		// printRunLimitBox should be the only print_run condition that exists (except maybe print_run_ebook)
		int genId = addCondition(objectId, PRINT_RUN, null, objectType);
		if (value == 0) {
			updateConditionRollupValue(genId, "Unlimited print run is granted");
			addCondition(objectId, PRINT_RUN_UNLIMITED, "true", objectType);
			deleteCondition(printRunLimitBox.getId(), objectType);
		}
		else {
			updateConditionRollupValue(genId, printRunLimitBox.getValue());
			addCondition(objectId, PRINT_RUN_LIMIT, "true", objectType);
			// PRINT_RUN_LIMIT_BOX already exists
		}
		con.commit();
		modifyPrintRunCount++;
		con.setAutoCommit(true);
		//promptYes("Modify Print Run update complete, continue?");
	}

	private boolean checkEditionProductFamily(int objectId, ObjectType objectType, Map<String, Condition> map, Map<String, Condition> trueMap,
			Map<String, Condition> dWorkTrueMap)
	throws SQLException, IOException {
		if (objectType == ObjectType.CW) return false;  // Should not be set for CW (if is will be deleted later by script)

		Condition productFamily = trueMap.get(EDITION_PRODUCT_FAMILY);
		if (productFamily == null)  return false;

		log("Edition Product Family condition found for " + objectType + " id = " + objectId);
		//promptYes("continue?");

		Condition dworkAll = dWorkTrueMap.get(DWORK_ALL);
		if (dworkAll != null) {
			con.setAutoCommit(false);
			// next method call (addEditionNoMention) will take care of delete or else 253
			//deleteCondition(productFamily.getId(), objectType);
			maybeAddEditionNoMention(objectId, objectType, map, trueMap);
			con.commit();
			switchedEditionProductFamilyCount++;
			con.setAutoCommit(true);
			return true;  // actually here return value could be true or false, doesn't matter much
		}

		log("Converting Edition Product Family to All D.Works for " + objectType + " id = " + objectId);
		con.setAutoCommit(false);
		// next method call (addEditionNoMention) will take care of delete or else 253
		//deleteCondition(productFamily.getId(), objectType);
		maybeAddEditionNoMention(objectId, objectType, map, trueMap);

		deleteAllConditionsExcept(objectType, map, "dwork_", "dwork");
		addCondition(objectId, DWORK_ALL, "true", objectType);
		Condition dwork = map.get(DWORK);
		String rollupValue = "Wiley can include the asset(s) in any ancillaries, derivatives and custom works";
		if (dwork == null) {
			int genId = addCondition(objectId, DWORK, null, objectType);
			updateConditionRollupValue(genId, rollupValue);
		}
		else {
			updateConditionRollupValue(dwork.getId(), rollupValue);
		}
		con.commit();
		switchedEditionProductFamilyCount++;
		con.setAutoCommit(true);
		//promptYes("EditionProductFamily update complete, continue?");
		return true;
	}

	// called from both checkEditionProductFamily() and checkNoEdition()
	private void maybeAddEditionNoMention(int objectId, ObjectType objectType, Map<String, Condition> map, Map<String, Condition> trueMap)
	throws SQLException {
		Condition editionCurrentFuture = trueMap.get(EDITION_CURRENT_FUTURE);
		Condition editionThis = trueMap.get(EDITION_THIS);
		Condition edition = map.get(EDITION);
		// since these used to be checkboxes instead of radio both could be checked which we no longer want
		if (editionCurrentFuture != null && editionThis != null) {
			// delete the "weaker" choice
			deleteCondition(editionThis.getId(), objectType);
		}

		if (editionCurrentFuture != null) {
			updateConditionRollupValue(edition.getId(), "Granted for this edition and all future editions");
			return;
		}
		else if (editionThis != null) {
			updateConditionRollupValue(edition.getId(), "Granted for this edition only");
			return;
		}

		deleteAllConditionsExcept(objectType, map, "edition_", "edition");
		addCondition(objectId, EDITION_NO_MENTION, "true", objectType);
		String rollupValue = "No mention of editions";
		if (edition == null) {
			int genId = addCondition(objectId, EDITION, null, objectType);
			updateConditionRollupValue(genId, rollupValue);
		}
		else {
			updateConditionRollupValue(edition.getId(), rollupValue);
		}
	}

	private void checkNoEdition(int objectId, ObjectType objectType, Map<String, Condition> map, Map<String, Condition> trueMap)
	throws SQLException, IOException {
		if (trueMap.size() > 0) return;
		if (objectType == ObjectType.CW) return;  // No reason to create for CW

		log("Setting Edition No Mention for " + objectType + " id = " + objectId);

		con.setAutoCommit(false);
		// in this case the following method always WILL add the condition
		maybeAddEditionNoMention(objectId, objectType, map, trueMap);
		con.commit();
		switchedEditionNothingCount++;
		con.setAutoCommit(true);
		//promptYes("Edition Nothing update complete, continue?");
	}

	private boolean checkDWorkCustom(int objectId, ObjectType objectType, Map<String, Condition> map, Map<String, Condition> trueMap)
	throws SQLException, IOException {
		if (objectType == ObjectType.CW) return false;  // Should not be set for CW (if is will be deleted later by script)

		Condition dworkCustom = trueMap.get(DWORK_CUSTOM);
		if (dworkCustom == null)  return false;

		Condition dworkAll = trueMap.get(DWORK_ALL);
		if (dworkAll != null) {
			// this delete not really necessary (would be done by 253) but might as well do here
			deleteCondition(dworkCustom.getId(), objectType);  // autoCommit is on here
			return true;  // actually here return value could be true or false, doesn't matter much
		}

		log("Converting DWork Custom to All D.Works for " + objectType + " id = " + objectId);
		con.setAutoCommit(false);
		// deleteAll below takes care of dworkCustom
		//deleteCondition(dworkCustom.getId(), objectType);
		deleteAllConditionsExcept(objectType, map, "dwork_", "dwork");
		addCondition(objectId, DWORK_ALL, "true", objectType);
		Condition dwork = map.get(DWORK);
		String rollupValue = "Wiley can include the asset(s) in any ancillaries, derivatives and custom works";
		if (dwork == null) {
			int genId = addCondition(objectId, DWORK, null, objectType);
			updateConditionRollupValue(genId, rollupValue);
		}
		else {
			updateConditionRollupValue(dwork.getId(), rollupValue);
		}
		con.commit();
		switchedDWorkCustomCount++;
		con.setAutoCommit(true);
		//promptYes("DWork Custom update complete, continue?");
		return true;
	}

	private boolean checkDWorkISV(int objectId, ObjectType objectType, Map<String, Condition> map, Map<String, Condition> trueMap)
	throws SQLException, IOException {
		if (objectType == ObjectType.CW) return false;  // Should not be set for CW (if is will be deleted later by script)

		Condition dworkISV = trueMap.get(DWORK_ISV);
		if (dworkISV == null)  return false;

		Condition dworkAll = trueMap.get(DWORK_ALL);
		if (dworkAll != null) {
			// this delete not really necessary (would be done by 253) but might as well do here
			deleteCondition(dworkISV.getId(), objectType);  // autoCommit is on here
			return true;  // actually here return value could be true or false, doesn't matter much
		}

		log("Converting DWork ISV to DWork Anc + Deriv for " + objectType + " id = " + objectId);
		con.setAutoCommit(false);
		// deleteAll below takes care of dworkISV
		//deleteCondition(dworkISV.getId(), objectType);
		deleteAllConditionsExcept(objectType, map, "dwork_", "dwork");
		addCondition(objectId, DWORK_ANC_AND_DERIV, "true", objectType);
		Condition dwork = map.get(DWORK);
		String rollupValue = "Wiley can include the asset(s) in ancillaries and derivatives with the exception of custom";
		if (dwork == null) {
			int genId = addCondition(objectId, DWORK, null, objectType);
			updateConditionRollupValue(genId, rollupValue);
		}
		else {
			updateConditionRollupValue(dwork.getId(), rollupValue);
		}
		con.commit();
		switchedDWorkISVCount++;
		con.setAutoCommit(true);
		//promptYes("DWork ISV update complete, continue?");
		return true;
	}

	private void checkNoDWork(int objectId, ObjectType objectType, Map<String, Condition> map, Map<String, Condition> trueMap)
	throws SQLException, IOException {
		if (trueMap.size() > 0) return;
		if (objectType == ObjectType.CW) return;  // No reason to create for CW

		log("Setting DWork No Mention for " + objectType + " id = " + objectId);

		con.setAutoCommit(false);
		deleteAllConditionsExcept(objectType, map, "dwork_", "dwork");
		addCondition(objectId, DWORK_NO_MENTION, "true", objectType);
		Condition dwork = map.get(DWORK);
		String rollupValue = "No mention of derivative works";
		if (dwork == null) {
			int genId = addCondition(objectId, DWORK, null, objectType);
			updateConditionRollupValue(genId, rollupValue);
		}
		else {
			updateConditionRollupValue(dwork.getId(), rollupValue);
		}
		con.commit();
		switchedDWorkNothingCount++;
		con.setAutoCommit(true);
		//promptYes("DWork Nothing update complete, continue?");
	}

	private void checkNoSublicense(int objectId, ObjectType objectType, Map<String, Condition> map, Map<String, Condition> trueMap)
	throws SQLException, IOException {
		if (trueMap.size() > 0) return;
		if (objectType == ObjectType.CW) return;  // No reason to create for CW

		log("Setting Sublicense No Mention for " + objectType + " id = " + objectId);

		con.setAutoCommit(false);
		deleteAllConditionsExcept(objectType, map, "sublicense_", "sublicense");
		addCondition(objectId, SUBLICENSE_NO_MENTION, "true", objectType);
		Condition sublicense = map.get(SUBLICENSE);
		String rollupValue = "This grant does not mention sublicensing";
		if (sublicense == null) {
			int genId = addCondition(objectId, SUBLICENSE, null, objectType);
			updateConditionRollupValue(genId, rollupValue);
		}
		else {
			updateConditionRollupValue(sublicense.getId(), rollupValue);
		}
		con.commit();
		switchedSublicenseNothingCount++;
		con.setAutoCommit(true);
		//promptYes("Sublicense Nothing update complete, continue?");
	}

	private Map<String, Condition> filterTrueConditions(Map<String, Condition> map, String startsWith) {
		Map<String, Condition> trueMap = new HashMap<String, Condition>();

		Iterator<Entry<String, Condition>> it = map.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, Condition> pairs = it.next();
			//log("value = " + pairs.getValue());
			if (pairs.getValue().isValueTrue() && pairs.getKey().startsWith(startsWith)) {
				trueMap.put(pairs.getKey(), pairs.getValue());
			}
		}

		return trueMap;
	}

	/** Used for print run. */
	private Map<String, Condition> filterNotNullConditions(Map<String, Condition> map, String startsWith) {
		Map<String, Condition> notNullMap = new HashMap<String, Condition>();

		Iterator<Entry<String, Condition>> it = map.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, Condition> pairs = it.next();
			//log("value = " + pairs.getValue());
			if (pairs.getValue().getValue() != null && pairs.getKey().startsWith(startsWith)) {
				notNullMap.put(pairs.getKey(), pairs.getValue());
			}
		}

		return notNullMap;
	}

	private int addCondition(int objectId, String conditionType, String value, ObjectType objectType) throws SQLException {
		String sql = "insert into condition_value (condition_type, value) values (?, ?)";
		PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
		ps.setString(1, conditionType);
		ps.setString(2,  value);
		ps.executeUpdate();

		ResultSet genKeys = ps.getGeneratedKeys();
		genKeys.next();
		int genId = genKeys.getInt(1);
		genKeys.close();
		ps.close();

		//log("generated condition id = " + genId);

		sql = "insert into contract_2_condition (contract_id, condition_id) values (?, ?)";
		if (objectType == ObjectType.MA_DEAL) {
			sql = "insert into ma_deal_2_condition (ma_deal_id, condition_id) values (?, ?)";
		}
		else if (objectType == ObjectType.CW) {
			sql = "insert into cw_2_condition (cw_id, condition_id) values (?, ?)";
		}
		ps = con.prepareStatement(sql);
		ps.setInt(1, objectId);
		ps.setInt(2, genId);
		ps.executeUpdate();
		ps.close();

		return genId;
	}

	private void updateConditionValue(int conditionId, String value) throws SQLException {
		String sql = "update condition_value set value = ? where id = ?";
		PreparedStatement ps = con.prepareStatement(sql);
		ps.setString(1, value);
		ps.setInt(2, conditionId);
		int rows = ps.executeUpdate();
		ps.close();

		if (rows != 1) {
			// this should never happen
			throw new RuntimeException("updateConditionValue(): number of rows was " + rows);
		}
	}

	private void updateConditionRollupValue(int conditionId, String rollupValue) throws SQLException {
		String sql = "update condition_value set rollup_value = ? where id = ?";
		PreparedStatement ps = con.prepareStatement(sql);
		ps.setString(1, rollupValue);
		ps.setInt(2, conditionId);
		int rows = ps.executeUpdate();
		ps.close();

		if (rows != 1) {
			// this should never happen
			throw new RuntimeException("updateConditionRollupValue(): number of rows was " + rows);
		}
	}

	private void deleteCondition(int conditionId, ObjectType objectType) throws SQLException {
		String sql = "delete from contract_2_condition where condition_id = ?";
		if (objectType == ObjectType.MA_DEAL) {
			sql = "delete from ma_deal_2_condition where condition_id = ?";
		}
		else if (objectType == ObjectType.CW) {
			sql = "delete from cw_2_condition where condition_id = ?";
		}
		PreparedStatement ps = con.prepareStatement(sql);
		ps.setInt(1, conditionId);
		int rows = ps.executeUpdate();
		ps.close();
		if (rows != 1) {
			// this should never happen
			throw new RuntimeException("deleteCondition(): number of rows was " + rows);
		}

		sql = "delete from condition_value where id = ?";
		ps = con.prepareStatement(sql);
		ps.setInt(1, conditionId);
		rows = ps.executeUpdate();
		ps.close();

		if (rows != 1) {
			// this should never happen
			throw new RuntimeException("deleteCondition(): number of rows was " + rows);
		}
	}

	private void deleteAllConditionsExcept(ObjectType objectType, Map<String, Condition> map, String startsWith, String except) throws SQLException {
		Iterator<Entry<String, Condition>> it = map.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, Condition> pairs = it.next();
			//log("value = " + pairs.getValue());
			String key = pairs.getKey();
			if (key.startsWith(startsWith) && !key.equals(except)) {
				deleteCondition(pairs.getValue().getId(), objectType);
			}
		}
	}

	private Map<String, Condition> getConditions(Integer id, ObjectType objectType) throws SQLException {
		String sql;
		if (objectType == ObjectType.CONTRACT) {
			sql = "select id, condition_type, value from view_contract_condition where contract_id = ?";
		}
		else if (objectType == ObjectType.MA_DEAL) {
			sql = "select id, condition_type, value from view_ma_deal_condition where ma_deal_id = ?";
		}
		else {
			sql = "select id, condition_type, value from view_cw_condition where cw_id = ?";
		}
		PreparedStatement ps = con.prepareStatement(sql);
		ps.setInt(1, id);
		ResultSet rs = ps.executeQuery();
		Map<String, Condition> map = new HashMap<String, Condition>();
		while (rs.next()) {
			String conditionType = rs.getString(2);
			map.put(conditionType, new Condition(rs.getInt(1), conditionType, rs.getString(3)));
		}
		ps.close();
		return map;
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
