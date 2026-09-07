package com.wiley.permissions.services.imports.filemaker;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;

public class LoadAUSInvoicesData {

//	private static final Log log = LogFactory.getLog(FileMakerPicNoUpdate.class);

	private static ResultSet assetData = null;

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
//		log.debug("Start");
		// production connection
		Connection conSource = null;

		try {
			Class.forName("com.ibm.as400.access.AS400JDBCDriver");
			conSource = DriverManager.getConnection(
					"jdbc:as400://brisbane.wiley.com/AUSDTA",
					"TABLESIQ",
					"ping2pong");

			System.out.println("\n\n AS400 jdbc driver is loaded\n\n");
		}
		catch (Exception e) {
			System.out.println("Failed to connect to AUSDTA");
			return;
		}


		Connection conUpdate = null;
		try {
			Class.forName("com.mysql.jdbc.Driver");
			conUpdate = DriverManager.getConnection(
					"jdbc:mysql://san-ds-dev.wiley.com/perm_nm",
					"nmedrano",
					"nmedrano");
		//	conUpdate = DriverManager.getConnection(
		//			"jdbc:mysql://vmpermdbdev.wiley.com:3323/perm",
		//			"permuser",
		//			"permuser1");
	//		conUpdate = DriverManager.getConnection(
	//				"jdbc:mysql://vmpermdbqa.wiley.com:3322/perm",
	//				"permuser",
	//				"permuser1");
			
			System.out.println("\n\nMySQL jdbc driver is loaded\n\n");
		}
		catch (Exception e) {
			System.out.println("Failed to connect to MySQL");
			return;
		}


	try
	{
String invoiceSql =
	" 	select distinct 0 as id, " +
	" 	po.CFEDCE as project_No, " +
	" 	'perm.source.aus.' || po.cfdccd || '.' || po.cfb6cd as	SourceExternalId, " +
	" 	po.CFH2CE as PO_number, " +
	" 	substr(po.CFT5DT + 19000000,5,2) || '/' || substr(po.CFT5DT + 19000000,7,2) || '/' || substr(po.CFT5DT + 19000000,1,4) as po_date, " +
	" 	po.CFFZVL as Po_Amount, " +
	" 	inv.CVD2TE as Invoice_Number, " +
	" 	substr(inv.CVT6DT + 19000000,5,2) || '/' || substr(inv.CVT6DT + 19000000,7,2) || '/' || substr(inv.CVT6DT + 19000000,1,4) as Invoice_Date, " +
	" 	inv.CVF1VL as Invoice_Amount, " +
	" 	substr(inv.CVT8DT + 19000000,5,2) || '/' || substr(inv.CVT8DT + 19000000,7,2) || '/' || substr(inv.CVT8DT + 19000000,1,4) as date_invoice_payed, " +
	" 	'' as  status, " +
	" 	'' as  error,  " +
	"     c.MFA8TX as Source_Name " +
	" 	from OZCFREP po left join OZCVREP inv on inv.CVEDCE = po.CFEDCE and inv.CVH2CE = po.CFH2CE " +
	"        left join mfmcusp c on   c.MFDCCD = inv.CVDCCD and  c.MFB6CD = inv.CVB6CD  " +
	" 	where po.CFEECE = 'R' " +
//	"   and po.CFEDCE in ('000407','001131','006003','006078','006218')" +
		" 	order by 2,3,4,7 ";




	 		PreparedStatement assetCache = conSource.prepareStatement(invoiceSql);
		assetData = assetCache.executeQuery();
		String selectPrefix =
			"insert into ozInvoicelist(id,  project_No, SourceExternalId, PO_number, po_date, Po_Amount, " +
			"	Invoice_Number, Invoice_Date, Invoice_Amount, date_invoice_payed, status, error, source_name) ";

		Integer rowCount = 0;
		Integer numberOfFields = 14;
		while (assetData.next()) {
			String insertValues =
				" values(";
			for(Integer x=1;x < numberOfFields; x++) {
				String delim = "'";
				if(x == 1) {
					delim = "";
				} else {
					delim = "'";
				}

				String value= assetData.getString(x);
				if(null != value) {
					value =  value.trim();
					value = value.replace("'", "''");
					value = value.replace("\n", " ");
				} else {
					value = "";
				}
				if(x==1) {
					value = rowCount +"";
				}
				insertValues=insertValues + delim + value + delim;
				if( x < numberOfFields -1) {
					insertValues=insertValues + ", ";
				}
			}
			insertValues = insertValues + ")";
			System.out.println(insertValues);
			PreparedStatement stmtUpdate = conUpdate.prepareStatement(selectPrefix + insertValues );
			stmtUpdate.executeUpdate();
			stmtUpdate.close();
			rowCount ++;
		//	if(rowCount > 1000) break;
		}
		assetData.close();


		System.out.println("finsished");



	}
	catch (Exception e) {
		e.printStackTrace();
		System.out.println(e);
	}
}
}
