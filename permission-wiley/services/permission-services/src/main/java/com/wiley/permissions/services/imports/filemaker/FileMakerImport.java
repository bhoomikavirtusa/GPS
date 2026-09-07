package com.wiley.permissions.services.imports.filemaker;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class FileMakerImport {

	private static final Log log = LogFactory.getLog(FileMakerImport.class);

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		log.debug("Start");
		// getFilemaker connection
		Connection conFM = null;
		try
		{
			@SuppressWarnings("unused")
			Driver d = (Driver) Class.forName("com.ddtek.jdbc.sequelink.SequeLinkDriver").newInstance();
			conFM = DriverManager.getConnection("jdbc:sequelink://localhost:2399", "Admin", "apple");
			System.out.println("\n\nFilemaker jdbc driver is loaded\n\n");
		}
		catch (Exception e)
		{
			System.out.println("Failed to connect to FileMaker");
			return;
		}

		// getMySQL on Linux connection
		Connection conMySQL = null;
		try
		{
			Class.forName("com.mysql.jdbc.Driver");
			conMySQL = DriverManager.getConnection(
					"jdbc:mysql://san-ds-dev.wiley.com/perm_ln",
					"lnagy",
					"lnagy");
			System.out.println("\n\nMySQL jdbc driver is loaded\n\n");
		}
		catch (Exception e)
		{
			System.out.println("Failed to connect to MySQL");
			return;
		}

		// load Vendor data into a HashMap
		Map<String, String[]> vendors = new HashMap<String, String[]>();
		try
		{
			Statement stmtFM = conFM.createStatement();
			ResultSet results = stmtFM.executeQuery("Select keyVendID, Company, recID  from Vendor");
			while (results.next())
			{
				String keyId = results.getString(1);
				String company = results.getString(2);
				String recID = "perm.source.FileMaker." + results.getInt(3);

				System.out.println(keyId + " " + company + " " + recID);
				// vendors.put(keyId, company);
				vendors.put(keyId, new String[] {company, recID});
			}
			results.close();
			stmtFM.close();
		}
		catch (Exception e)
		{
			System.out.println(e);
		}

		// read data from FileMaker
		ResultSet results = null;
		try
		{
			Statement stmtFM = conFM.createStatement();
			Statement stmtMySQL = conMySQL.createStatement();
/*
			results = stmtFM
					.executeQuery("select count(*) from PWList INNER JOIN Project P on pwlist.keyProjID= P.keyProjID where description is not null");
			while (results.next()) {
				System.out.println("TOTAL COUNT " + results.getInt(1));
			}
*/

			results = stmtFM
					.executeQuery(" select ChapterNo,TypeSection,TypeFigure,Figure_Num,Final_Pg_No,page_pos,Pickup_PrevTextRef,P_Num,Description,"
							+ " \"Kill\",Color,BW,Pict_No,DesignSize,PhotoSize,SourcePhotog,Credit,Camera_Copy,Free,RoyaltyFree,ObtainedbyAuthor,"
							+ " ChapOpener,WorkforHire,ModelRelease,\"New\",Retain,Reuse,Archive,P.ISBN,ProdNotes,DeptNotes,keyVendID from PWList "
							+ " INNER JOIN Project P on pwlist.keyProjID= P.keyProjID where description is not null and " +
									"P.isbn is not null and (P.ProjectType like 'Main%' or P.ProjectType like 'Cover%')");
									// "P.photo_activeProj is not null and P.isbn ='0471039144'");

			ResultSetMetaData rsmd = results.getMetaData();
			int numCols = rsmd.getColumnCount();
			int count = 0;
			while (results.next())
			{
				String insert = "";
				try
				{
					insert = "INSERT INTO pwlist(ChapterNo,TypeSection,TypeFigure,Figure_Num,Final_Pg_No,page_pos,Pickup_PrevTextRef,P_Num,Description, "
							+ "\"Kill\",Color,BW,Pict_No,DesignSize,PhotoSize,SourcePhotog,Credit,Camera_Copy,Free,RoyaltyFree,ObtainedbyAuthor, "
							+ "ChapOpener,WorkforHire,ModelRelease,\"New\",Retain,Reuse,Archive,ISBN,prodNotes,deptNotes,Company,SourceId) VALUES (";
					for (int i = 1; i <= numCols - 1; i++)
					{
						if (i > 1)
							insert += ",";
						if (results.getString(i) == null)
							insert += "null";
						else
							insert += "'" + escape((results.getString(i))) + "'";
					}
					String keyVendID = results.getString(numCols);
					if (StringUtils.isEmpty(keyVendID))
					{
						insert += ",null,null";
					}
					else
					{
						String[] company = vendors.get(keyVendID);
						if (null == company) {
							insert += ",null,null";
						}
						else
						{
							String name = company[0];
							String sourceId = company[1];
							insert += ",'" + escape(name) + "', '" + sourceId + "'";
						}
					}
					insert += ")";
/*
					if (count++ < 100)
						System.out.print(".");
					else {
						count = 0;
						System.out.println(".");
					}
*/
					System.out.println((count++));
					stmtMySQL.execute(insert);
				}
				catch (Exception e)
				{
					e.printStackTrace();
					System.out.println("---" + insert + "\nError" + e);
				}
			}
			System.out.println("COUNT " + count);
			results.close();
			stmtFM.close();
			stmtMySQL.close();
		}
		catch (Exception e)
		{
			System.out.println(e);
		}
	}

	private static String escape(String string)
	{
		if (string != null)
			return string.replaceAll("'", "''");
		else
			return null;
	}
}
