package com.wiley.permissions.services.imports.filemaker;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;

//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;

public class FileMakerPicNoUpdate {

//	private static final Log log = LogFactory.getLog(FileMakerPicNoUpdate.class);

	private static ResultSet productData = null;

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
//		log.debug("Start");
		// production connection
		Connection conUpdate = null;

		try {
			Class.forName("com.mysql.jdbc.Driver");
		//	conUpdate = DriverManager.getConnection(
		//			"jdbc:mysql://vmpermdbprod.wiley.com:3322/perm",
		//			"permuser",
		//			"permuser1");

		//	conUpdate = DriverManager.getConnection(
		//			"jdbc:mysql://vmpermdbdev.wiley.com/perm",
		//			"permuser",
		//			"permuser1");

			conUpdate = DriverManager.getConnection(
					"jdbc:mysql://vmpermdbqa.wiley.com:3322/perm",
					"permuser",
					"permuser1");

			System.out.println("\n\nMySQL jdbc driver is loaded\n\n");
		}
		catch (Exception e) {
			System.out.println("Failed to connect to DEV");
			return;
		}

		// getMySQL on Linux connection
		Connection conSource = null;
		try {
			Class.forName("com.mysql.jdbc.Driver");
			conSource = DriverManager.getConnection(
					"jdbc:mysql://san-ds-dev.wiley.com/perm_ln",
					"lnagy",
					"lnagy");
			System.out.println("\n\nMySQL jdbc driver is loaded\n\n");
		}
		catch (Exception e) {
			System.out.println("Failed to connect to MySQL");
			return;
		}


	try
	{
		Statement stmtSource = conSource.createStatement();

		PreparedStatement productCache = conUpdate.prepareStatement("select isbn10, isbn13, cw_id from product");
		productData = productCache.executeQuery();
		HashMap<String,String> cacheProducts = new HashMap<String,String>();
		while (productData.next()) {
			String i1 = productData.getString(1);
			String i2 = productData.getString(2);
			String cw_id = productData.getString(3);
			// add both isbns
			cacheProducts.put(i1, cw_id);
			cacheProducts.put(i2, cw_id);
		}
		productData.close();

		PreparedStatement stmtUpdate = conUpdate.prepareStatement("update asset set vendor_id = ? " +
			" where id = ?");

		PreparedStatement stmtSelect = conUpdate.prepareStatement("select distinct asset_use.asset_id, asset.description from asset_use join asset on asset_use.asset_id = asset.id where asset_use.cw_id = ? ");


		ResultSet results = stmtSource.executeQuery("SELECT ISBN, Pict_No, Description from pwlist where Description is not null " +
				" and Pict_No is not null and ISBN is not null order by ISBN");
		String previousIsbn = "";
		HashMap<String,String> cacheAssets = new HashMap<String,String>();
		while (results.next()) {
			long startTime = System.currentTimeMillis();

			String description = results.getString(3);

			String isbn = results.getString(1);
			isbn = isbn.replaceAll("-", "");
			String cw_id = null;
			cw_id = cacheProducts.get(isbn);


			String pictNo = results.getString(2);

			if (null != cw_id ) {
				try {
					if (previousIsbn.compareTo(isbn) != 0) {
						stmtSelect.setString (1, cw_id);
						ResultSet assets = stmtSelect.executeQuery();
						cacheAssets = new HashMap<String,String>();
						while (assets.next()) {
							String t1 = assets.getString(1);
							String t2 = assets.getString(2);
							cacheAssets.put(t2, t1);
						}
						previousIsbn = isbn;
					}


					int cassets = 0;

					String assetId = cacheAssets.get(description);
					if (null != assetId) {
						stmtUpdate.setString(1,pictNo);
						stmtUpdate.setString(2,assetId);
						cassets = stmtUpdate.executeUpdate();

						long time = System.currentTimeMillis() - startTime;
						if (cassets > 1) {
							System.out.println("\t *** updated multiples ISBN" + "(" + time + "):" + isbn + " pict_no:" + pictNo + " Description: " + description);
						}

						if (cassets < 1) {
							System.out.println("\t *** could not update ISBN" + "(" + time + "):" + isbn + " pict_no:" + pictNo + " Description: " + description);
						}

						if (cassets == 1) {
							System.out.println("\t *** succesfully updated ISBN" + "(" + time + "):" + isbn + " pict_no:" + pictNo + " Description: " + description);
						}

					}

				} catch (Exception e) {
					System.out.println(" Exception on update " + description + " " + isbn + " " + pictNo + " " + e.getMessage());
				}
			}

		}
		results.close();
		stmtUpdate.close();
		stmtSource.close();
	}
	catch (Exception e) {
		e.printStackTrace();
		System.out.println(e);
	}
}
}
