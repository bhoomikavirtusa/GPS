package com.wiley.permissions.services.imports.filemaker;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class AssetMergeUpdate {


	private static Connection conUpdate = null;

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{


		try
		{
			Class.forName("com.mysql.jdbc.Driver");
		//	conUpdate = DriverManager.getConnection(
		//			"jdbc:mysql://vmpermdbprod.wiley.com:3322/perm",
		//			"permuser",
		//			"permuser1");

			conUpdate = DriverManager.getConnection(
					"jdbc:mysql://vmpermdbdev.wiley.com:3306/perm",
					"permuser",
					"permuser1");

		//	conUpdate = DriverManager.getConnection(
		//			"jdbc:mysql://vmpermdbqa.wiley.com:3322/perm",
		//			"permuser",
		//			"permuser1");

	//		conUpdate = DriverManager.getConnection(
	//				"jdbc:mysql://san-ds-dev.wiley.com/perm_nm",
	//				"nmedrano",
	//				"nmedrano");
	//

			System.out.println("\n\nMySQL jdbc driver is loaded\n\n");
		}
		catch (Exception e)
		{
			System.out.println("Failed to connect to DEV");
			return;
		}

	String vendorId = "";
	try
	{


		// initialize
		PreparedStatement stmtUpdateInitial = conUpdate.prepareStatement("delete from global_assets_2merge ");
		stmtUpdateInitial.executeUpdate();
		PreparedStatement stmtUpdateInitial2 = conUpdate.prepareStatement("insert into global_assets_2merge " +
					" select min(a.id),a.vendor_id, b.source_id from asset a, asset_2_source b " +
				    " where a.id =  b.asset_id " +
					" group by a.vendor_id, b.source_id having count(vendor_id) > 1 " +
					" and a.vendor_id is not null and a.vendor_id <> '' " +
					" order by count(vendor_id) ");
					stmtUpdateInitial2.executeUpdate();


//		Statement stmtSelect = conUpdate.createStatement();
//		ResultSet results = stmtSelect.executeQuery("select destination_asset_id, vendor_id, source_id from global_assets_2merge");

	boolean processed = true;
	String previousSource = "";

	while(processed) {

		processed = false;


		Statement stmtSelect = conUpdate.createStatement();
		ResultSet results = stmtSelect.executeQuery("select destination_asset_id, vendor_id, source_id from global_assets_2merge" +
													"  where source_id = (select min(source_id) from global_assets_2merge " +
													" where source_id > '" +  previousSource + "' ) " +
													" and source_id > '" + previousSource + "'") ;
		while (results.next())
		{
			Integer destinationId = results.getInt(1);
			vendorId = results.getString(2);
			String sourceId = results.getString(3);

			if(!processed) {
				System.out.println("processing assets for source:" + sourceId);
				previousSource = sourceId;
				processed = true;
			}


			System.out.println("processing id:" + destinationId + " vendorId:" + vendorId + " source:" + sourceId );
			PreparedStatement selectedAssets = conUpdate.prepareStatement("select id from asset where vendor_id = ? ");
			selectedAssets.setString(1,vendorId);
			ResultSet assets = selectedAssets.executeQuery();
			while(assets.next())
			{
				Integer cAssetId = assets.getInt(1);
				if(cAssetId.compareTo(destinationId) != 0) {
					transferAsset(cAssetId, destinationId, new Integer(sourceId));
				}
			}
			assets.close();
			selectedAssets.close();

		}

		results.close();

	  } // end of while processed

	}
	catch (Exception e)
	{
		e.printStackTrace();
		System.out.println(e);
	}



}

public static void transferAsset(Integer fromId, Integer toId, Integer sourceId) {

	try {

	// if no assets for this source return
	PreparedStatement stmtUpdate = conUpdate.prepareStatement(
			"select count(*) from asset_perm_ref where asset_id = " + fromId + " and source_id = " + sourceId );
	ResultSet assets = stmtUpdate.executeQuery();
	assets.next();
	Integer count = assets.getInt(1);
	assets.close();
	stmtUpdate.close();
	if(count < 1) return;

	System.out.println("Transfering AssetId from:" + fromId + " to " + toId + " for source:" + sourceId);

	// transfer po's
	processTable("purchase_order_2_asset",
			"update purchase_order_2_asset set asset_base_id = " + toId +
			" where asset_base_id = " + fromId +
			" and purchase_order_id in (select id from purchase_order where source_id = " + sourceId + ")"
	);

	// transfer asset use
	processTable("asset_use",
			"update asset_use set asset_id = " + toId +
			" where asset_id = " + fromId +
			" and asset_id in (select asset_id from asset_perm_ref where asset_id = " + fromId + " and source_id = " + sourceId + ")");


	// transfer contract 2 asset
	processTable("contract_2_asset",
			"update contract_2_asset set asset_base_id = " + toId +
				" where asset_base_id = " + fromId +
				" and asset_base_id in (select asset_id from asset_perm_ref where asset_id = " + fromId + " and source_id = " + sourceId + ")") ;


	// transfer condition values
	processTable("condition_value",
	"update condition_value set asset_base_id = " + toId +
			" where asset_base_id = " + fromId  +
			" and asset_base_id in (select asset_id from asset_perm_ref where asset_id = " + fromId + " and source_id = " + sourceId + ")") ;



	// transfer asset files
	processTable("asset_file",
			"update asset_file set asset_id = " + toId +
			" where asset_id = " + fromId +
			" and asset_id in (select asset_id from asset_perm_ref where asset_id = " + fromId + " and source_id = " + sourceId + ")") ;



	// delete old asset to source
	processTable("asset_2_source",
			"delete from asset_2_source " +
			" where asset_id = " + fromId + " and source_id = " + sourceId);


	// transfer asset perm ref
	processTable("asset_perm_ref",
			"update asset_perm_ref set asset_id = " + toId +
			" where asset_id = " + fromId + " and source_id = " + sourceId);


	// delete old asset if no other sources are using it
	processTable("asset",
			"delete from asset " +
			" where id = " + fromId +
			" and id not in (select asset_id from asset_perm_ref where asset_id = " + fromId + ")") ;


	// delete old asset base if no other sources are using it
	processTable("asset_base",
			"delete from asset_base " +
			" where id = " + fromId +
			" and id not in (select asset_id from asset_perm_ref where asset_id = " + fromId + ")") ;



	} catch (Exception e) {
		System.out.println("ERROR " + e.getMessage());
	}

}

public static void processTable(String table, String sqlStmt) {

	try {
		PreparedStatement stmtUpdate = conUpdate.prepareStatement(sqlStmt) ;
		Integer updates = stmtUpdate.executeUpdate();
		if(updates > 0) {
			System.out.println( updates + " updates  for " + table + " processed");
		}
		 stmtUpdate.close();
	} catch (Exception e) {
		System.out.println("ERROR processing " + sqlStmt);
		System.out.println(e.getMessage());
	}

}



}
