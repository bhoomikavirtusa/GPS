package com.wiley.sf.common.sql;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.EmailAttachment;
import org.apache.commons.mail.HtmlEmail;

import com.wiley.sf.common.config.PropertiesUtil;
import com.wiley.sf.common.io.FileUtil;
import com.wiley.sf.common.lang.ArgUtil;
import com.wiley.sf.common.lang.StringUtil;

/**
 *
 * @since   JDK 1.6
 * @version 4/8/2011
 * @author  Steve Markoff
 */
public class MonitorMySqlLockWait implements Runnable {

    private static final String newLine = "\r\n";
    private static final String CSS_STYLE =
        "body {" + newLine
        + "font-family: sans-serif;" + newLine
        + "}" + newLine
        + ".tableHeading {" + newLine
        + "  margin-top: 1em;" + newLine
        + "  font-weight: bold;" + newLine
        + "}" + newLine
        + "table  {" + newLine
        + "  color: black;" + newLine
        + "  border-collapse: collapse;" + newLine
        + "  border: 1px solid black;" + newLine
        + "}" + newLine
        + "table tr:nth-child(odd) { background-color: lightgray }" + newLine
        + "table tr:nth-child(even) { background-color: white }" + newLine
        + "table tr:hover td {" + newLine
        + "  background: #bbbbee;" + newLine
        + "}" + newLine
        + "table th {" + newLine
        + "  padding: 2px 4px;" + newLine
        + "  color: white;" + newLine
        + "  background-color: #555555;" + newLine
        + "  border: 1px solid black;" + newLine
        + "}" + newLine
        + "table td {" + newLine
        + "  padding: 2px 4px;" + newLine
        + "  border: 1px solid #555555;" + newLine
        + "}" + newLine;

    public static void main(String [] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: <connection properties file> <log/email properties file>");
            System.exit(1);
        }

        SimpleDBConnect dbConnect = SimpleDBConnect.create(args[0]);
            // throws various exceptions

        PropertiesUtil pu = new PropertiesUtil(args[1]);
        File logDir = new File(pu.getProperty("logDir"));
        int waitSecs = pu.getInt("waitSecs", 20);
        int reconnectWaitMin = pu.getInt("reconnectWaitMin", 3);
        String environmentName = pu.getString("environmentName");
        String smtpServer = pu.getString("smtpServer", null);
        String emailFrom = pu.getString("emailFrom", null);
        ArrayList<String> emailList = pu.getStringListTrim("email", false);

        MonitorMySqlLockWait monitor = new MonitorMySqlLockWait(dbConnect,
            logDir, waitSecs, reconnectWaitMin, environmentName, smtpServer, emailFrom, emailList);
        monitor.go();
    }


    private final SimpleDBConnect dbConnect;
    private Connection con;
    private final File logDir;
    private final int waitSecs;
    private final int reconnectWaitMin;
    private final String environmentName;
    private final String smtpServer;
    private final String emailFrom;
    private final ArrayList<String> emailList;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final SimpleDateFormat fileDateFormat = new SimpleDateFormat("yyyy-MM-dd'_'HH-mm-ss");

    /**
     *
     * @param dbConnect     Must be non-null
     * @param logDir        Must be a writable directory
     * @param waitSecs      Must be in the range 1-500
     * @param reconnectWaitMin  Must be in the range 1-500
     * @param environmentName  Must be non-blank
     * @param smtpServer    May be null or blank (in which case no email will be sent)
     * @param emailFrom     Should be non-blank
     * @param emailList     May be null or empty (in which case no email will be sent)
     *
     * @throws SQLException
     */
    public MonitorMySqlLockWait(SimpleDBConnect dbConnect, File logDir, int waitSecs,
            int reconnectWaitMin, String environmentName, String smtpServer,
            String emailFrom, ArrayList<String> emailList)
    throws SQLException
    {
        this.dbConnect = dbConnect;
        this.logDir = logDir;
        this.waitSecs = waitSecs;
        this.reconnectWaitMin = reconnectWaitMin;
        this.environmentName = environmentName;
        this.smtpServer = smtpServer;
        this.emailFrom = emailFrom;
        this.emailList = emailList;

        System.out.println("logDir: " + logDir.getAbsolutePath());
        System.out.println("waitSecs: " + waitSecs);
        System.out.println("reconnectWaitMin: " + reconnectWaitMin);
        System.out.println("environmentName: " + environmentName);
        System.out.println("smtpServer: " + smtpServer);
        System.out.println("emailFrom: " + emailFrom);
        System.out.println("emailList: " + StringUtil.collectionToString(emailList, ", "));

        ArgUtil.notNull(dbConnect, "dbConnect");
        FileUtil.checkDirectory(logDir, true);
        ArgUtil.inRange(waitSecs, "waitSecs", 1, 500);
        ArgUtil.inRange(reconnectWaitMin, "reconnectWaitMin", 1, 500);

        con = dbConnect.getConnection();
        con.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        con.setAutoCommit(true);
    }

    public void go() {
        new Thread(this).start();
    }

    public void run() {
        System.out.println(dateFormat.format(new Date()) + " thread started...");
        boolean keepRunning = true;

        while (keepRunning) {
            try {
                check();
            }
            catch (Exception ex) {
                System.out.println("Caught exception: " + ex);

                // Hopefully the problem is just that we've lost the database
                // connection due to a db restart.
                // Test the connection to see

                boolean connectionIsBad = true;
                try {
                    currentLockWaits(con);
                    connectionIsBad = false;
                }
                catch (Exception ex2) { }

                if (connectionIsBad) {
                    System.out.println("bad connection - waiting " + reconnectWaitMin + " min to reconnect...");
                    sendEmail("Lock Monitor: bad connection - waiting " + reconnectWaitMin + " min to reconnect...", null);
                    try { Thread.sleep(1000 * 60 * reconnectWaitMin); }
                    catch (InterruptedException ex2) { }

                    try { con.close(); } catch (Exception ex3) { }
                    try {
                        con = dbConnect.getConnection();
                        System.out.println("connection resumed.");
                        sendEmail("Lock Monitor: connection resumed.", null);
                    }
                    catch (Exception ex4) {
                        keepRunning = false;
                        System.out.println("shutting down due to connection failure: " + ex);
                        sendEmail("Lock Monitor: shutting down due to connection failure: ", ex);
                    }
                }
                else {
                    keepRunning = false;
                    System.out.println("shutting down due to error: " + ex);
                    sendEmail("Lock Monitor: shutting down due to error: ", ex);
                }
            }
        }
    }

    private void check() throws SQLException, IOException {
        int numLockWaits = currentLockWaits(con);  // throws SQLException

        if (numLockWaits > 0) {
            Date date = new Date();
            String dateString = dateFormat.format(date);
            String fileDateString = fileDateFormat.format(date);
            System.out.println(dateString + ": numLocksWaits = " + numLockWaits);

            File statusFile = new File(logDir, fileDateString + "_innodb_status.txt");
            saveInnodbStatus(statusFile);  // throws SQLException, IOException

            File otherFile = new File(logDir, fileDateString + "_other.html");
            String [] queryArray = {
                "select * from information_schema.innodb_locks",
                "select * from information_schema.innodb_lock_waits",
                "select * from information_schema.innodb_trx",
                "show processlist"
            };
            createHtmlFile(queryArray, otherFile, fileDateString);  // throws SQLException, IOException

            sendEmail(statusFile, otherFile);
        }

        try { Thread.sleep(1000 * waitSecs); }
        catch (InterruptedException ex) { }
    }

    private void sendEmail(String subject, Exception ex) {
        if (StringUtils.isBlank(smtpServer) || CollectionUtils.isEmpty(emailList)) {
            return;
        }

        try {
            HtmlEmail email = new HtmlEmail();
            email.setHostName(smtpServer);

            email.setFrom(emailFrom);
            for (String to: emailList) {
                email.addTo(to);
            }
            subject = subject + " [" + environmentName + "]";
            email.setSubject(subject);
            if (ex == null)  email.setTextMsg(subject);
            else  email.setTextMsg(ex.toString());

            email.send();
        }
        catch (Exception e) {
            System.err.println("sending email failed: " + e);
        }
    }

    private void sendEmail(File statusFile, File otherFile) {
        if (StringUtils.isBlank(smtpServer) || CollectionUtils.isEmpty(emailList)) {
            return;
        }

        try {
            HtmlEmail email = new HtmlEmail();
            email.setHostName(smtpServer);

            email.setFrom(emailFrom);
            for (String to: emailList) {
                email.addTo(to);
            }
            email.setSubject("lock waits(s) detected [" + environmentName + "]");
            email.setHtmlMsg(FileUtil.fileToString(otherFile));
            email.setTextMsg(FileUtil.fileToString(statusFile));

            EmailAttachment a1 = new EmailAttachment();
            a1.setURL(otherFile.toURI().toURL());
            //a1.setPath("mypictures/john.jpg");
            a1.setDisposition(EmailAttachment.ATTACHMENT);
            a1.setDescription("Table dumps");
            a1.setName(otherFile.getName());
            email.attach(a1);

            EmailAttachment a2 = new EmailAttachment();
            a2.setURL(statusFile.toURI().toURL());
            //a2.setPath("mypictures/john.jpg");
            a2.setDisposition(EmailAttachment.ATTACHMENT);
            a2.setDescription("InnoDB Status");
            a2.setName(statusFile.getName());
            email.attach(a2);

            email.send();
        }
        catch (Exception e) {
            System.err.println("sending email failed: " + e);
        }
    }

    private int currentLockWaits(Connection con) throws SQLException {
        String sql = "show status where variable_name = 'Innodb_row_lock_current_waits'";
        PreparedStatement ps = con.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();
        rs.next();
        String value = rs.getString(2);
        ps.close();

        return Integer.parseInt(value);
    }

    private void saveInnodbStatus(File file) throws SQLException, IOException {
        String sql = "show engine innodb status";
        PreparedStatement ps = con.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();
        rs.next();
        // Columns are Type ("InnoDB"), Name (blank), and Status
        String value = rs.getString(3);
        ps.close();

        FileUtil.stringToFile(value, file);  // throws IOException
    }

    private void createHtmlFile(String [] sqlQueryArray, File file, String title) throws SQLException, IOException {
        StringBuilder sb = new StringBuilder();
        for (String sqlQuery : sqlQueryArray) {
            sb.append("<div class=\"tableHeading\">" + sqlQuery + "</div>");
            sb.append(createHtmlTable(sqlQuery));  // throws SQLException
        }
        ResultSetViewer.createHtmlShell(file, sb.toString(), title, CSS_STYLE);  // throws IOException
    }

    @SuppressWarnings("unused")
    private void createHtmlFile(String sqlQuery, File file, String title) throws SQLException, IOException {
        String table = createHtmlTable(sqlQuery);  // throws SQLException
        ResultSetViewer.createHtmlShell(file, table, title, CSS_STYLE);  // throws IOException
    }

    private String createHtmlTable(String sqlQuery) throws SQLException {
        PreparedStatement ps = con.prepareStatement(sqlQuery);
        ResultSet rs = ps.executeQuery();
        ResultSetViewer rsView = new ResultSetViewer(rs);
        ps.close();

        return rsView.createHtmlTableView(false);
    }

    @SuppressWarnings("unused")
    private void createTabDelimFile(String sqlQuery, File file) throws SQLException, IOException {
        PreparedStatement ps = con.prepareStatement(sqlQuery);
        ResultSet rs = ps.executeQuery();
        ResultSetViewer rsView = new ResultSetViewer(rs);
        ps.close();

        rsView.createTabDelimitedView(file, false);  // throws IOException
    }
}
