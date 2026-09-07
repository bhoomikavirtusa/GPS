package com.wiley.permissions.services.imports.filemaker;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TestConnection {

	private static final Log log = LogFactory.getLog(TestConnection.class);
	
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
			e.printStackTrace();
			System.out.println("Failed to connect to FileMaker");
			return;
		}

	}
}
