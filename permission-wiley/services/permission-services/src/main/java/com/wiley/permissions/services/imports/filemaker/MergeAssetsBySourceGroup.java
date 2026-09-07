package com.wiley.permissions.services.imports.filemaker;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;

public class MergeAssetsBySourceGroup {

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
			Class.forName("com.mysql.jdbc.Driver");
	//		conSource =  DriverManager.getConnection(
	//				"jdbc:mysql://san-ds-dev.wiley.com/perm_nm",
	//				"nmedrano",
	//				"nmedrano");
			conSource =  DriverManager.getConnection(
					"jdbc:mysql://vmpermdbprod.wiley.com:3322/perm",
					"permuser",
					"permuser1");
	//		conSource = DriverManager.getConnection(
	//				"jdbc:mysql://vmpermdbqa.wiley.com:3322/perm",
	//				"permuser",
	//				"permuser1");

			System.out.println("\n\n source database jdbc driver is loaded\n\n");
		}
		catch (Exception e) {
			System.out.println("Failed to connect to source data");
			return;
		}

		Connection conUpdate = null;
		try {
			Class.forName("com.mysql.jdbc.Driver");
	//		conUpdate = DriverManager.getConnection(
	//				"jdbc:mysql://san-ds-dev.wiley.com/perm_nm",
	//				"nmedrano",
	//				"nmedrano");
			conUpdate =  DriverManager.getConnection(
					"jdbc:mysql://vmpermdbprod.wiley.com:3322/perm",
					"permuser",
					"permuser1");
		//	conUpdate = DriverManager.getConnection(
		//			"jdbc:mysql://vmpermdbdev.wiley.com:3323/perm",
		//			"permuser",
		//			"permuser1");
	//		conUpdate = DriverManager.getConnection(
	//				"jdbc:mysql://vmpermdbqa.wiley.com:3322/perm",
	//				"permuser",
	//				"permuser1");
			
			System.out.println("\n\nMySQL jdbc update driver is loaded\n\n");
		}
		catch (Exception e) {
			System.out.println("Failed to connect to MySQL Update driver");
			return;
		}

	try {
String sourceSql = "select x.id, x.vendor_id, x.description, x.is_royalty_free, z.source_group, z.name as source_name, sg.name as source_group_name, z.id " +
"from asset x, asset_2_source y, source z, source_group sg where vendor_id in ( " +
"select a.vendor_id " +
" from asset a,  asset_2_source c, source d " +
"where  a.id = c.asset_id and c.source_id =  d.id " +
"group by   d.source_group || '-' || a.vendor_id  having count(  d.source_group || '-' || a.vendor_id ) > 1 " +
"order by   d.source_group || '-' || a.vendor_id ) " +
"and x.id = y.asset_id and y.source_id = z.id and sg.id = z.source_group " +
"order by z.source_group, vendor_id,  z.name  ";		
		

PreparedStatement assetCache = conSource.prepareStatement(sourceSql);
assetData = assetCache.executeQuery();
String selectPrefix =
	"insert into nmtemp(vendor_id, description, is_royalty_free, source_group, source_name, source_group_name) ";

// first drop constraint
String sqlx1 = "alter table usage_2_size drop foreign key fk_u2size_2_c2asset ";	
// first drop a constraint 
try {     	
			PreparedStatement stmtUpdatex1 = conUpdate.prepareStatement(sqlx1 );
		 	stmtUpdatex1.executeUpdate();
			stmtUpdatex1.close();
	} catch (SQLException e) {
		// TODO Auto-generated catch block
			e.printStackTrace();
	}





	Integer rowCount = 0;
		Integer numberOfFields = 9;
		String prevVendorId = "";
		Integer prevSourceGroup = 0;
		Integer prevAssetId = 0;
		Integer assetId = 0;
		Integer sourceId = 0;
		String vendorId = "";
		Integer sourceGroup = 0;
		while (assetData.next()) {
			String insertValues =
				" values(";
			for(Integer x=1;x < numberOfFields; x++) {
				String delim = "'";
				if (x == 1) {
					delim = "";
				} else {
					delim = "'";
				}

				if(x == 2) {
					vendorId = assetData.getString(x);
					assetId = assetData.getInt(1);

				}
				
				if(x == 5) {
					sourceGroup = assetData.getInt(x);
				}
				
				
				if(x == 8) {
					sourceId = assetData.getInt(x);
				}
				
		
				
				String value= assetData.getString(x);
				if (null != value) {
					value =  value.trim();
					value = value.replace("'", "''");
					value = value.replace("\n", " ");
				} else {
					value = "";
				}
		
				insertValues=insertValues + delim + value + delim;
				if (x < numberOfFields -1) {
					insertValues=insertValues + ", ";
				}
			}
			insertValues = insertValues + ")";
	//		System.out.println(insertValues);
			
			if(vendorId.compareTo(prevVendorId) != 0 || sourceGroup.compareTo(prevSourceGroup) != 0) {
				// control break
				prevAssetId = assetId;
				prevVendorId = vendorId;
				prevSourceGroup = sourceGroup;
				System.out.println("***** control break new sourceGroup:" + sourceGroup + " vendorId:" + vendorId + " new asset_id:" + assetId);
			} else {
				// need to merge
				mergeAssets(assetId, prevAssetId, sourceId, conUpdate);
			//	System.out.println("======  will merge assetId:" + assetId + " with assetId:" + prevAssetId);
				
			}
			
			
		//	PreparedStatement stmtUpdate = conUpdate.prepareStatement(selectPrefix + insertValues );
	//		stmtUpdate.executeUpdate();
	//		stmtUpdate.close();
			rowCount ++;
		
		//	if(rowCount > 1000) break;
		}
		assetData.close();
		
		System.out.println("data updated");


	}
	catch (Exception e) {
		e.printStackTrace();
		System.out.println(e);
	}
	
	
	// finally add the constraint back
	String sqlx2 = "alter table usage_2_size add constraint fk_u2size_2_c2asset foreign key (contract_id, asset_base_id) references contract_2_asset (contract_id, asset_base_id)  on delete cascade; ";	
	// add back the constraint
	try {     	
				PreparedStatement stmtUpdatex2 = conUpdate.prepareStatement(sqlx2 );
			 	stmtUpdatex2.executeUpdate();
				stmtUpdatex2.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
				e.printStackTrace();
		}
	
	
	
	
}
	
public static void mergeAssets(Integer fromId, Integer toId, Integer sourceId, Connection conUpdate) {
	
	System.out.println("======= about to merge assetId:" + fromId + " into:" + toId);
	
	
	System.out.println(new Date() +  ": Transfering AssetId from:" + fromId + " to " + toId + " for source:" + sourceId);
	
	


	// transfer po's	
	String sql1 = "update purchase_order_2_asset set asset_base_id = " + toId +
		" where asset_base_id = " + fromId +
		" and purchase_order_id in (select id from purchase_order where source_id = " + sourceId + ")";
	System.out.println(sql1);
	
	
	try {     	
				PreparedStatement stmtUpdate = conUpdate.prepareStatement(sql1 );
			 	stmtUpdate.executeUpdate();
				stmtUpdate.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
				e.printStackTrace();
		}
			
		try {    
	// transfer asset use
	String sql2 = "update asset_use set asset_id = " + toId +
	" where asset_id = " + fromId +
	" and asset_id in (select asset_id from asset_perm_ref where asset_id = " + fromId + " and source_id = " + sourceId + ")";
	System.out.println(sql2);
		PreparedStatement stmtUpdate2 = conUpdate.prepareStatement(sql2 );
			stmtUpdate2.executeUpdate();
			stmtUpdate2.close();
	} catch (SQLException e) {
			// TODO Auto-generated catch block
				e.printStackTrace();
	}
	
	
	try {    
		// transfer usage_2_size
		String sql2b = "update usage_2_size set asset_base_id = " + toId +
		" where asset_base_id = " + fromId +
		" and asset_base_id in (select asset_id from asset_perm_ref where asset_id = " + fromId + " and source_id = " + sourceId + ")";
		System.out.println(sql2b);
			PreparedStatement stmtUpdate2 = conUpdate.prepareStatement(sql2b );
				stmtUpdate2.executeUpdate();
				stmtUpdate2.close();
		} catch (SQLException e) {
				// TODO Auto-generated catch block
					e.printStackTrace();
		}
	

	try {    
	// transfer contract 2 asset
	String sql3 = "update contract_2_asset set asset_base_id = " + toId +
	" where asset_base_id = " + fromId +
	" and asset_base_id in (select asset_id from asset_perm_ref where asset_id = " + fromId + " and source_id = " + sourceId + ")";
	System.out.println(sql3);
		PreparedStatement stmtUpdate3 = conUpdate.prepareStatement(sql3 );
			stmtUpdate3.executeUpdate();
			stmtUpdate3.close();
	} catch (SQLException e) {
		// TODO Auto-generated catch block
			e.printStackTrace();
	}
	

	
//	try {
	// transfer condition values
//	String sql4 = "update condition_value set asset_base_id = " + toId +
//	" where asset_base_id = " + fromId  +
//		" and asset_base_id in (select asset_id from asset_perm_ref where asset_id = " + fromId + " and source_id = " + sourceId + ")";
//	System.out.println(sql4);
	//	PreparedStatement stmtUpdate4 = conUpdate.prepareStatement(sql4 );
	//		stmtUpdate4.executeUpdate();
	//		stmtUpdate4.close();
//	} catch (SQLException e) {
//		// TODO Auto-generated catch block
//			e.printStackTrace();
//	}
	
	try {    
	// transfer asset files
	String sql5 = "update asset_file set asset_id = " + toId +
		" where asset_id = " + fromId +
		" and asset_id in (select asset_id from asset_perm_ref where asset_id = " + fromId + " and source_id = " + sourceId + ")";
	System.out.println(sql5);
		PreparedStatement stmtUpdate5 = conUpdate.prepareStatement(sql5 );
			stmtUpdate5.executeUpdate();
			stmtUpdate5.close();
	} catch (SQLException e) {
		// TODO Auto-generated catch block
			e.printStackTrace();
	}	


	try {    
	// delete old asset to source
	String sql6 = "delete from asset_2_source where asset_id = " + fromId + " and source_id = " + sourceId;
	System.out.println(sql6);
		PreparedStatement stmtUpdate6 = conUpdate.prepareStatement(sql6 );
			stmtUpdate6.executeUpdate();
			stmtUpdate6.close();
	} catch (SQLException e) {
		// TODO Auto-generated catch block
			e.printStackTrace();
	}

	try {    
	// transfer asset perm ref
	String sql7 = "update asset_perm_ref set asset_id = " + toId + " where asset_id = " + fromId + " and source_id = " + sourceId;
	System.out.println(sql7);
		PreparedStatement stmtUpdate7 = conUpdate.prepareStatement(sql7 );
			stmtUpdate7.executeUpdate();
			stmtUpdate7.close();
	} catch (SQLException e) {
		// TODO Auto-generated catch block
			e.printStackTrace();
	}

	try {    
	// delete old asset if no other sources are using it
	String sql8 = "delete from asset " + " where id = " + fromId + " and id not in (select asset_id from asset_perm_ref where asset_id = " + fromId + ")";
	System.out.println(sql8);
		PreparedStatement stmtUpdate8 = conUpdate.prepareStatement(sql8 );
			stmtUpdate8.executeUpdate();
			stmtUpdate8.close();
	} catch (SQLException e) {
		// TODO Auto-generated catch block
			e.printStackTrace();
	}


	try {    
	// delete old asset base if no other sources are using it
	String sql9 = "delete from asset_base " + " where id = " + fromId + " and id not in (select asset_id from asset_perm_ref where asset_id = " + fromId + ")";
	System.out.println(sql9);
		PreparedStatement stmtUpdate9 = conUpdate.prepareStatement(sql9 );
			stmtUpdate9.executeUpdate();
			stmtUpdate9.close();
	} catch (SQLException e) {
		// TODO Auto-generated catch block
			e.printStackTrace();
	}
	
	
	
	
	
}
	
	
}

