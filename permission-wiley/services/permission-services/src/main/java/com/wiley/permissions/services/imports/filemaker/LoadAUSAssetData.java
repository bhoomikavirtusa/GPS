package com.wiley.permissions.services.imports.filemaker;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;

public class LoadAUSAssetData {

//	private static final Log log = LogFactory.getLog(LoadAUSAssetData.class);

	private static ResultSet assetData = null;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
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
		//	conUpdate = DriverManager.getConnection(
		//			"jdbc:mysql://vmpermdbqa.wiley.com:3322/perm",
		//			"permuser",
		//			"permuser1");
			
			System.out.println("\n\nMySQL jdbc driver is loaded\n\n");
		}
		catch (Exception e) {
			System.out.println("Failed to connect to MySQL");
			return;
		}

	try {
String sourceSql = "select distinct au.CCG4CE as	id, " +
" au.CCDPNR as	ChapterNo, " +
//-- typeSection feeds component so this is fed from chapterfor chapters use au.CCDPNR (adding "chapter " literal in front)
//-- and this field ends up with Chapter
//-- for Cover use CCATSS F=Front Cover,  B=BackCover, I=?Introduction,W=?
" CASE WHEN (au.ccdpnr > 0) THEN 'Chapter' " +
" ELSE " +
"    CASE WHEN (au.ccatss = 'F') THEN 'Front Cover' " +
" ELSE " +
"    CASE WHEN (au.ccatss = 'B') THEN 'Back Cover' " +
" ELSE " +
"    CASE WHEN (au.ccatss = 'I') THEN 'Contents' " +    // I is internal and I think it means contents but not sure
" ELSE " +
"    CASE WHEN (au.ccatss = 'W') THEN 'WrapAround Cover' " +
" ELSE " +
"    CASE WHEN (c1.D2DNTE = 'COVER') THEN 'Front Cover' " +
"  ELSE " +
"      'Contents' END END END END END END as typeSection, " +
// " a.CDHECE as	TypeSection, " +
" c1.D2DNTE  as typeFigure, " +
" au.CCDSTE as	Figure_Num, " +
" au.CCFMTE as	Final_Pg_No, " +
" au.CCDPTE as	page_pos, " +
" '' as	Pickup_PrevTextRef, " +
" '' as	P_Num, " +
" a.CDDYTE as	Description, " +
// " CASE WHEN (au.CCAKSS > 'D') THEN 'Y' ELSE '' END  as	\"Kill\",  " +
 " '' as	\"Kill\", " +
" CASE WHEN (au.CCHFCE = 'C') then 'Y' ELSE '' END as Color, " +
" CASE WHEN (au.CCHFCE = 'B') then 'Y' ELSE '' END as BW, " +
" a.CDDRTE as	Pict_No, " +
" '' as	DesignSize, " +
" au.CCDLNR as	PhotoSize, " +
" ' ' as	SourcePhotog, " +
" a.CDDWTE as	Credit, " +
" '' as	Camera_Copy, " +
" '' as	Free, " +
" a.CDALSS as	RoyaltyFree, " +
" au.CCAYSS as	ObtainedbyAuthor, " +
" '' as	ChapOpener, " +
" '' as	WorkforHire, " +
" '' as	ModelRelease, " +
" '' as	New, " +
" '' as	Retain, " +
" '' as	Reuse, " +
" '' as	Archive, " +
" ' ' as	prodNotes, " +
" com.comment || com.comment01 || com.comment02 || com.comment03 || com.comment04 || com.comment05 || com.comment06 " +
" || com.comment07 || com.comment08 || com.comment09 || com.comment10 as deptNotes, " +
" c.MFA8TX as company, " +
" 'perm.source.aus.' || a.cddccd || '.' || a.cdb6cd as	SourceId, " +
" p.A4LANG || p.A4ISBN as	ISBN, " +
" '' as	status, " +
" '' as	error, " +
" au.CCEDCE as project_number, " +
" au.CCH2CE as po_number, " +
" au.CCAKSS as perm_req_status, " +
" au.CCHGCE as request_Type, " +
" '' as asset_use_id, " +
" au.CCEKPR as estimated_Price, " +
" au.CCG7PC as price, " +
" CASE WHEN (au.CCAKSS = 'D') THEN '1' ELSE '0' END  as	cancelled_flag,  " +
" c.MFA8TX as Source_Nme " +
" from ozcccpp au " +
" left join PPECOMM com on au.CCHKCE = com.COMMID,  OZCDREP a " +
" left join OZD2REL1 c1 on c1.D2HECE = a.CDHECE, OZA4REP p, mfmcusp c " +
"  where au.ccg5ce > '' " +
"  and au.ccg5ce = a.CDG5CE " +
"   and p.A4EDCE = au.CCEDCE and p.A4ISBN > ''" +
"   and c.mfdccd = a.cddccd " +
"   and c.mfb6cd = a.cdb6cd ";
// "  and p.A4LANG || p.A4ISBN in ('1118599039','0701636254','0701637951','1118624173','1118606248')";
PreparedStatement assetCache = conSource.prepareStatement(sourceSql);
assetData = assetCache.executeQuery();
String selectPrefix =
	"insert into ozlist(id, ChapterNo, TypeSection, TypeFigure, Figure_Num, Final_Pg_No, page_pos, Pickup_PrevTextRef, P_Num, Description, " +
	"	\"Kill\", Color, BW, Pict_No, DesignSize, PhotoSize, SourcePhotog, Credit, Camera_Copy, Free, RoyaltyFree, " +
	"	ObtainedbyAuthor, ChapOpener, WorkforHire, ModelRelease, New, Retain, Reuse, Archive, " +
	"	prodNotes, deptNotes, Company, SourceId, ISBN, status, error, project_number, po_number,  perm_req_status,request_type, asset_use_id, estimated_price, price, canceled_flag, source_name)";
	Integer rowCount = 0;
		Integer numberOfFields = 46;
		while (assetData.next()) {
			// skip rows
		//	if(rowCount <= 139364) {
		//		System.out.println(rowCount);
		//		rowCount ++;
		//		continue;
		//	}
			String sourceExternalId = assetData.getString(33);
			String sourceName = assetData.getString(45);
		//	System.out.println("@@@@ sourceId:" + sourceExternalId + "  name:" + sourceName);
			sourceExternalId = getRealExternalId(sourceExternalId, sourceName,conUpdate );
			String insertValues = 
				" values(";
			for(Integer x=1;x < numberOfFields; x++) {
				String delim = "'";
				if (x == 1) {
					delim = "";
				} else {
					delim = "'";
				}

				String value= assetData.getString(x);
				if (null != value) {
					value =  value.trim();
					value = value.replace("'", "''");
					value = value.replace("\n", " ");
				} else {
					value = "";
				}
				if (x==1) {
					value = rowCount +"";
				}
				if(x==33) {
					value = sourceExternalId;
				}
				insertValues=insertValues + delim + value + delim;
				if (x < numberOfFields -1) {
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
		
		System.out.println("data imported");


	}
	catch (Exception e) {
		e.printStackTrace();
		System.out.println(e);
	}
}

	static String getRealExternalId(String externalId, String sourceName, Connection connection) {
		ResultSet tmpData = null;
		ResultSet tmpData2 = null;
		String sql = "Select External_id from source where external_id = '" + externalId + "'" ;
		String sql2 = "Select External_id from source where name = '" + sourceName + "' ";
		System.out.println(sql);
		System.out.println(sql2);
		
		
		try {
			PreparedStatement eid;
			eid = connection.prepareStatement(sql);
			tmpData = eid.executeQuery();
			tmpData.next();
			String newExternalId = tmpData.getString(1);
	//		System.out.println("@@@@@@ found by id id:" + newExternalId);
			tmpData.close();
			eid.close();
			return newExternalId;
		} catch (SQLException e) {
		//    System.out.println("looking for source by name");
		   
		    try {
		    	PreparedStatement eid2;
		    	eid2 = connection.prepareStatement(sql2);
		    	tmpData2 = eid2.executeQuery();
		    	tmpData2.next();
		    	String newExternalId = tmpData2.getString(1);
		//    	System.out.println("@@@@@@ found by name id:" + newExternalId);
		    	tmpData2.close();
		    	eid2.close();
		    	return newExternalId;
		    } catch (SQLException f) {
		    	// I give up
		    }
		    
		}
	
		
	return externalId;
	}
	
}

