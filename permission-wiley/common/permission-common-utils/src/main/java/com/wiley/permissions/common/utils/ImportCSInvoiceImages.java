package com.wiley.permissions.common.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;

import com.wiley.sf.common.io.FileUtil;
import com.wiley.sf.common.lang.StringUtil;
import com.wiley.sf.common.sql.SimpleDBConnect;
import com.wiley.sf.common.text.TimeFormat;

/**
 * Imports PDFs for Invoices with file names having the format
 * "<ISBN10 or 13>_<source name>_<invoice number>.pdf".
 * The underscore is used as a field separator so for example the source name
 * must not contain any underscores.
 *
 * @since JDK 1.6
 * @version 3/20/2014
 * @author Steve Markoff
 */
public class ImportCSInvoiceImages {

	private static final Log log = LogFactory.getLog(ImportCSInvoiceImages.class);

	public static void main(String[] args) throws Exception {
		if (args.length < 2) {
			System.err.println("Usage: <properties file> <image dir>");
			System.exit(1);
		}

		SimpleDBConnect dbConnect = SimpleDBConnect.create(args[0]);
		// throws various exceptions
		File imageDir = new File(args[1]);
		ImportCSInvoiceImages prog = new ImportCSInvoiceImages(
				dbConnect.getConnection(), imageDir, true);
		prog.go();
	}

	private final Connection con;
	private final File imageDir;
	private final boolean logToSystemOut;
	private final boolean makeChanges = true;
	private final boolean outputErrorsOnly = true;

	private int successCount = 0;
	private int alreadyImportedCount = 0;
	private int matchWithDifferentSourceNameCount = 0;
	private int errorCount = 0;

	public ImportCSInvoiceImages(Connection con, File imageDir, boolean logToSystemOut) throws SQLException {
		this.con = con;
		this.imageDir = imageDir;
		FileUtil.checkDirectory(imageDir, false);
		this.logToSystemOut = logToSystemOut;
		con.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
		con.setAutoCommit(true);
	}

	public void go() throws SQLException, IOException {
		long startTime = System.currentTimeMillis();

		handleFileOrDir(imageDir);

		long time = System.currentTimeMillis() - startTime;
		TimeFormat timeFormat = new TimeFormat();
		log("total time was " + timeFormat.formatMS(time));

		log("errorCount = " + errorCount);
		log("matchWithDifferentSourceNameCount = " + matchWithDifferentSourceNameCount);
		log("successCount = " + successCount);
		log("alreadyImportedCount = " + alreadyImportedCount);
	}
	
	private void handleFileOrDir(File file) throws SQLException, IOException {
		// we don't expect this but check anyway
		if (file.isDirectory()) {
			log("going into Directory [" + file.getName() + "].");
			File [] files = file.listFiles();
			for (File f2 : files) {
				handleFileOrDir(f2);  // recursive call
			}
			return;
		}
		
		// we don't expect this but check anyway
		if (!StringUtils.endsWithIgnoreCase(file.getName(), ".pdf")) {
			log("File [" + file.getName() + "] does not end with pdf - skipping.");
			errorCount++;
			return;
		}

		processFile(file);
	}

	private void processFile(File file) throws SQLException, IOException {
		// we already checked that the file ends with ".pdf" so we know the ending exists
		int dotIndex = file.getName().lastIndexOf('.');
		String name = file.getName().substring(0, dotIndex);
		String [] split = name.split("_");
		if (split.length != 3) {
			log("File [" + file.getName() + "] does not have 2 underscores or does not have an invoice number after the last underscore.");
			errorCount++;
			return;
		}
		String isbn = split[0];
		String sourceName = split[1];
		String invoiceNumber = split[2];
		
		if (invoiceNumber.startsWith(" ") || invoiceNumber.endsWith(" ")) {
			log("fixing automatically: invoiceNumber starts or ends with space - file [" + file.getName() + "]");
			invoiceNumber = invoiceNumber.trim();
		}

		Integer cwId = getCwIdForIsbn(isbn);
		if (cwId == null) {
			log("isbn " + isbn + " not found in db.");
			errorCount++;
			return;
		}

		List<ContractSummary> list = getContracts(cwId, invoiceNumber, sourceName);
		if (list.size() == 0) {
			log("No invoices with number [" + invoiceNumber + "] found for isbn ["
					+ isbn + "] cwId [" + cwId + "] sourceName [" + sourceName + "].");
			list = getContracts(cwId, invoiceNumber, null);
			if (list.size() == 0) {
				//log("-> Still no invoices found if exclude sourceName from query");
			}
			else {
				matchWithDifferentSourceNameCount++;
				log("-> " + list.size() + " invoices found if exclude sourceName from query - first sourceName ["
						+ list.get(0).getSourceName() + "]");
			}
			errorCount++;
			return;
		}

		if (list.size() > 1) {
			// while this is not necessarily an issue, it probably is
			// (definitely is if two contracts have the same cwId, sourceId, and number)
			log("warning: multiple contracts found for number [" + invoiceNumber
					+ "] and isbn [" + isbn + "] cwId [" + cwId + "] sourceName [" + sourceName + "]");
			log("- file [" + file.getName() + "]");
			log("- " + StringUtil.collectionToString(list, " | "));
		}
		
		for (ContractSummary summary : list) {
			if (!outputErrorsOnly) {
				log("invoice found for number [" + invoiceNumber + "] and isbn [" + isbn + "] cwId [" + cwId
						+ "] db source [" + summary.getSourceName() + "] file source [" + sourceName + "]");
			}
			boolean exists = exists(list.get(0).getContractId(), file);
			if (!outputErrorsOnly) {
				log("exists = " + exists);
			}
			if (exists)  alreadyImportedCount++;
			if (!exists && makeChanges) {
				insert(list.get(0).getContractId(), file);
			}
			successCount++;
		}
	}

	private boolean exists(int contractId, File file) throws SQLException {
		String sql = "select * from contract_file where contract_id = ? and file_name = ? and length(file_data) = ?";
		PreparedStatement ps = con.prepareStatement(sql);
		ps.setInt(1, contractId);
		ps.setString(2, file.getName());
		ps.setLong(3, file.length());
		ResultSet rs = ps.executeQuery();
		boolean exists = rs.next();
		ps.close();
		return exists;
	}

	private void insert(int contractId, File file) throws SQLException, IOException {
		String sql = "insert into contract_file (contract_id, created_date, last_updated_date, file_name, description, mime_type, file_data)"
		    + " values (?, ?, ?, ?, ?, ?, ?)";
		PreparedStatement ps = con.prepareStatement(sql);
		ps.setInt(1, contractId);
		Timestamp now = new Timestamp(System.currentTimeMillis());
		ps.setTimestamp(2, now);
		ps.setTimestamp(3, now);
		ps.setString(4, file.getName());
		ps.setString(5, "(from zip import)");
		ps.setString(6, "application/pdf");
		FileInputStream fis = new FileInputStream(file);
		ps.setBlob(7, fis);
		ps.executeUpdate();
		ps.close();
		fis.close();
	}

	private Integer getCwIdForIsbn(String isbn) throws SQLException {
		String sql = "select cw_id from product where isbn13 = ? or isbn10 = ?";
		PreparedStatement ps = con.prepareStatement(sql);
		ps.setString(1, isbn);
		ps.setString(2, isbn);
		ResultSet rs = ps.executeQuery();
		Integer cwId = null;
		if (rs.next()) {
			cwId = rs.getInt(1);
		}
		ps.close();
		return cwId;
	}

	private List<ContractSummary> getContracts(int cwId, String number, String sourceName) throws SQLException {
		// sourceName == null used to mean don't include sourceName in query
		// generally we should only get a single contract but if we get more than one make sure
		// the newest one comes first
		// Note we do not need to add any logic for a case INsensitive comparison on sourceName since we
		// are using MySQL which handles queries case-INsensitive by default.
		String sql = "select c.id, s.name from contract c, source s where c.cw_id = ? and c.number = ? and s.id = c.source_id"
				+ (sourceName == null ? "" : " and s.name = ?")
				+ " order by date desc, id desc";
		PreparedStatement ps = con.prepareStatement(sql);
		ps.setInt(1, cwId);
		ps.setString(2, number);
		if (sourceName != null) {
			ps.setString(3, sourceName);
		}
		ResultSet rs = ps.executeQuery();
		List<ContractSummary> list = new ArrayList<ContractSummary>();
		while (rs.next()) {
			list.add(new ContractSummary(rs.getInt(1), rs.getString(2)));
		}
		ps.close();
		return list;
	}

	private void log(String msg) {
		if (logToSystemOut) {
			System.out.println(msg);
		}
		else {
			log.info(msg);
		}
	}

	class ContractSummary {
		private final int contractId;
		private final String sourceName;

		public ContractSummary(int contractId, String sourceName) {
			this.contractId = contractId;
			this.sourceName = sourceName;
		}

		public int getContractId() {
			return contractId;
		}

		public String getSourceName() {
			return sourceName;
		}

		@Override
		public String toString() {
			return "(" + contractId + ", " + sourceName + ")";
		}
	}
}
