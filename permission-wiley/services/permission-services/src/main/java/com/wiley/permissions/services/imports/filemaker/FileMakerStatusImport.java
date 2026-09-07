package com.wiley.permissions.services.imports.filemaker;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class FileMakerStatusImport {

	private static final Log log = LogFactory.getLog(FileMakerStatusImport.class);

	/**
	 * Processed = 1 means the asset was imported
	 * Processed = 2 means the product was not found
	 * Processed = 0 means the asset was not imported for another reason
	 * @param args
	 */
	public static void main(String[] args)
	{
		log.debug("Start");
		// production connection
		Connection conProd = null;
		try {
			Class.forName("com.mysql.jdbc.Driver");
			conProd = DriverManager.getConnection(
					"jdbc:mysql://vmpermdbqa.wiley.com:3322/perm",
					"permuser",
					"permuser1");
			System.out.println("\n\nMySQL jdbc driver is loaded\n\n");
		}
		catch (Exception e) {
			System.out.println("Failed to connect to FileMaker");
			return;
		}

		// getMySQL on Linux connection
		Connection conMySQL = null;
		try {
			Class.forName("com.mysql.jdbc.Driver");
			conMySQL = DriverManager.getConnection(
					"jdbc:mysql://san-ds-dev.wiley.com/perm_ln",
					"lnagy",
					"lnagy");
			System.out.println("\n\nMySQL jdbc driver is loaded\n\n");
		}
		catch (Exception e) {
			System.out.println("Failed to connect to MySQL");
			return;
		}

		try {
			PreparedStatement stmtMySQL = conMySQL.prepareStatement("select id, description, figure_num, isbn from PWList2 where figure_num is null and processed is null");
			PreparedStatement stmtUpdate = conMySQL.prepareStatement("update pwlist2 set processed = 1 where id=?");
			PreparedStatement stmtFM = conProd.prepareStatement("SELECT asset_use.id from asset_use join asset on asset_use.asset_id = asset.id " +
					"join common_work on common_work.id = asset_use.cw_id " +
					"join product on product.cw_id = common_work.id where asset.description=? " +
					"and asset_use.position is null and (product.isbn10=? or product.isbn13=?)");
			ResultSet resultsLocal = null;
			ResultSet results = stmtMySQL.executeQuery();
			while (results.next()) {
				String id = results.getString(1);
				String description = results.getString(2);
				String position = results.getString(3);
				String isbn = results.getString(4);

				stmtFM.setString(1, description);
				//stmtFM.setString(2, position);
				stmtFM.setString(2, isbn);
				stmtFM.setString(3, isbn);
				resultsLocal = stmtFM.executeQuery();
				int count = 0;
				while (resultsLocal.next()) {
					System.out.println("\t\t FOUND " + id + " " + description + " " + isbn + " " + position);
					count++;
					try {
						stmtUpdate.setString(1, id);
						stmtUpdate.execute();
					} catch (Exception e) {
						System.out.println(" Exception on update " + id + " " + e.getMessage());
					}
				}
				if (count == 0)
					System.out.println("NOT FOUND " + id + " " + description + " " + isbn + " " + position);
			}

			results.close();
			stmtFM.close();
			resultsLocal.close();
			stmtMySQL.close();
		}
		catch (Exception e)	{
			System.out.println(e);
		}


/*
		try {
			PreparedStatement stmtProd = conProd.prepareStatement("select * from product where isbn10=? or isbn13=?");
			PreparedStatement stmtUpdate = conMySQL.prepareStatement("update pwlist set processed=2 where isbn=? and processed=0");

			Statement stmtMySQL = conMySQL.createStatement();
			ResultSet results = stmtMySQL.executeQuery("select distinct isbn from pwlist where processed=0");
			while (results.next()) {
				String isbn = results.getString(1);
				System.out.println("\t " + isbn);

				stmtProd.setString(1, isbn);
				stmtProd.setString(2, isbn);
				ResultSet resultsProd = stmtProd.executeQuery();
				int count = 0;
				while (resultsProd.next()) {
					count ++;
				}
				if (count == 0) {
					System.out.println(" NOT FOUND " + isbn);
					try {
						stmtUpdate.setString(1, isbn);
						stmtUpdate.execute();
					} catch (Exception e) {
						System.out.println(" Exception on update " + isbn + " " + e.getMessage());
					}
				}
				resultsProd.close();
			}
			results.close();
			stmtProd.close();
			stmtMySQL.close();
			stmtUpdate.close();
		}
		catch (Exception e) {
			System.out.println(e);
		}
	}
*/
/*
	try {
		Statement stmtFM = conProd.createStatement();
		PreparedStatement stmtMySQL = conMySQL.prepareStatement("select id from PWList where description=? and (isbn=? or isbn=?) and figure_num is null and processed=0");
		PreparedStatement stmtUpdate = conMySQL.prepareStatement("update pwlist set processed = 1 where id=?");
		ResultSet resultsLocal = null;
		ResultSet results = stmtFM.executeQuery("SELECT position ,asset.description ,product.isbn10 ,product.isbn13 from asset_use " +
				"join asset on asset_use.asset_id = asset.id join common_work on common_work.id = asset_use.cw_id " +
				"join product on product.cw_id = common_work.id and product.is_cw_primary=1 and position is null");
		while (results.next()) {
			String description = results.getString(2);
			String position = results.getString(1);
			String isbn10 = results.getString(3);
			String isbn13 = results.getString(4);
			System.out.println("\t " + description + " " + isbn10 + " " + position);

			stmtMySQL.setString(1, description);
			stmtMySQL.setString(2, isbn10);
			stmtMySQL.setString(3, isbn13);
			// stmtMySQL.setString(4, position);
			resultsLocal = stmtMySQL.executeQuery();
			int count = 0;
			while (resultsLocal.next()) {
				String id = resultsLocal.getString(1);
				// String pos = resultsLocal.getString(2);
				System.out.println("\t\t FOUND " + id + " " + description + " " + isbn10);
				count ++;
				try {
					stmtUpdate.setString(1, id);
					// stmtUpdate.execute();
				} catch (Exception e) {
					System.out.println(" Exception on update " + id + " " + e.getMessage());
				}
			}
			if (count == 0)
				System.out.println(" NOT FOUND " + description);
		}
		results.close();
		stmtFM.close();
		resultsLocal.close();
		stmtMySQL.close();
	}
	catch (Exception e) {
		e.printStackTrace();
		System.out.println(e);
	}
	*/

}
}
