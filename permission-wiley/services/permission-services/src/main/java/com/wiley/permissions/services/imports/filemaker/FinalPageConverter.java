package com.wiley.permissions.services.imports.filemaker;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class FinalPageConverter {

	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(FinalPageConverter.class);

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		System.out.println("Start");
		// getDB2 on AS400 connection
		Connection conDB2 = null;
		try
		{
			Class.forName("com.ibm.db2.jcc.DB2Driver");
			conDB2 = DriverManager.getConnection(
					"jdbc:db2://san-ds-dev.wiley.com:50000/PERM",
					"lnagy",
					"lnagy");

		}
		catch (Exception e)
		{
			System.out.println("Failed to connect to DB2");
			return;
		}

		System.out.println("Read data from DB");
		// read data from DB
		ResultSet results = null;
		try
		{
			Statement stmtDB2 = conDB2.createStatement();
			PreparedStatement stmtUpdateDB2 = conDB2.prepareStatement(
					"update pwlist set final_pg_no=? where id=?");

			results = stmtDB2.executeQuery(
					"select id, final_pg_no " +
					"from pwlist where SUBSTR(HEX(final_pg_no), LENGTH(HEX(final_pg_no))-1, 2) = '00' order by id"
					);
					// update pwlist set final_pg_no = SUBSTR(final_pg_no, 1, LENGTH(final_pg_no)-2) where SUBSTR(final_pg_no, LENGTH(final_pg_no)-1, 2) = '.0'
			System.out.println("Execute query");
			while (results.next())
			{
				String id = results.getString(1);
				String finalPage = results.getString(2);
				String escapedFinalPage = escape(finalPage);
				System.out.println(id + " " + finalPage + " " + escapedFinalPage);
				try
				{
					stmtUpdateDB2.setString(1, escapedFinalPage);
					stmtUpdateDB2.setString(2, id);
					stmtUpdateDB2.execute();
				}
				catch (Exception e)
				{
					System.out.println("Error" + e);
				}
			}
			results.close();
			stmtUpdateDB2.close();
			stmtDB2.close();
		}
		catch (Exception e)
		{
			System.out.println(e);
		}
	}

	private static String escape(String string)
	{
		return string.substring(0, string.length() - 1);
	}
}
