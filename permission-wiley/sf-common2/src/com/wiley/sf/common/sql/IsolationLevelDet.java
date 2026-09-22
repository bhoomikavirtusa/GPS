package com.wiley.sf.common.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import com.wiley.sf.common.lang.ArgUtil;

/**
 * This class determines what the Transaction Isolation Level of a DataSource
 * appears to be by using test cases. Useful if you think your configuration
 * of a particular level may not be working.
 * Requires the existence of a test table in your database (see below for
 * table create script).
 *
 * @since   JDK 1.6
 * @version 3/18/2011
 * @author  Steve Markoff
 */
public class IsolationLevelDet {

    public static String TABLE = "ISOLATION_TEST";


    public static void main(String [] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: <properties file>");
            System.exit(1);
        }

        SimpleDBConnect dbConnect = SimpleDBConnect.create(args[0]);
            // throws various exceptions

        Connection con1 = dbConnect.getConnection();
        Connection con2 = dbConnect.getConnection();
        final int transIso = Connection.TRANSACTION_READ_COMMITTED;
        con1.setTransactionIsolation(transIso);
        con2.setTransactionIsolation(transIso);

        IsolationLevelDet det = new IsolationLevelDet(con1, con2, 1);
        System.out.println("determineIsoLevel = " + NameForConstants.nameForTransactionIsolation(det.determineIsoLevel()));
        System.out.println("doesSelectBlockUpdate = " + det.doesSelectBlockUpdate());
        det.closeConnections();
    }


    private final Connection con1;
    private final Connection con2;
    private final int waitSecs;


    public IsolationLevelDet(DataSource ds, int waitSecs) throws SQLException {
        this(ds.getConnection(), ds.getConnection(), waitSecs);
    }

    public IsolationLevelDet(Connection con1, Connection con2, int waitSecs) throws SQLException {
        ArgUtil.inRange(waitSecs, "waitSecs", 1, 10);
        this.con1 = con1;
        this.con2 = con2;
        this.waitSecs = waitSecs;
        con1.setAutoCommit(false);
        con2.setAutoCommit(false);
    }

    public int determineIsoLevel() throws SQLException {
        // if Connection is TRANSACTION_SERIALIZABLE then canReadCommitted() method
        // can block so call doesSelectBlockInsert() first.
        if (doesSelectBlockInsert())  return Connection.TRANSACTION_SERIALIZABLE;
        else if (canReadUncommitted())  return Connection.TRANSACTION_READ_UNCOMMITTED;
        else if (canReadCommitted())  return Connection.TRANSACTION_READ_COMMITTED;
        else return Connection.TRANSACTION_REPEATABLE_READ;
    }

    public void closeConnections() throws SQLException {
        con1.close();
        con2.close();
    }

    private boolean canReadUncommitted() throws SQLException {
        deleteAllAndCommit(con1);

        insertRow(con1);
        try { Thread.sleep(1000 * waitSecs); } catch (InterruptedException ex) { }
        int rowCount = countRows(con2);
        con1.commit();
        con2.commit();
        return rowCount > 0;
    }

    private boolean canReadCommitted() throws SQLException {
        deleteAllAndCommit(con1);

        int rowCount = countRows(con2);
        ArgUtil.inRange(rowCount, "rowCount", 0, 0);
        insertRow(con1);  // blocks here if TRANSACTION_SERIALIAZABLE
        con1.commit();
        try { Thread.sleep(1000 * waitSecs); } catch (InterruptedException ex) { }
        rowCount = countRows(con2);
        con2.commit();
        return rowCount > 0;
    }

    private boolean doesSelectBlockInsert() throws SQLException {
        deleteAllAndCommit(con1);

        boolean insertSuccess = false;
        int rowCount = countRows(con1);
        SecondInsertThread thread2 = new SecondInsertThread(con2);
        thread2.start();
        try { Thread.sleep(1000 * waitSecs); } catch (InterruptedException ex) { }

        try {
            insertRow(con1);
            con1.commit();
            insertSuccess = true;
        }
        catch (SQLException ex) {
            System.out.println("thread 1 failed to insert (ok)");
            try { con1.commit(); } catch (Exception ex2) { }
        }

        return insertSuccess;
    }

    private boolean doesSelectBlockUpdate() throws SQLException {
        deleteAllAndCommit(con1);
        insertRow(con1);
        con1.commit();

        readRows(con1);
        UpdateThread thread2 = new UpdateThread(con2);
        thread2.start();
        try { Thread.sleep(1000 * waitSecs); } catch (InterruptedException ex) { }
        con1.commit();
        try { thread2.join(); } catch (InterruptedException ex) { }
        //System.out.println("update time was " + thread2.getTimeMS());
        return thread2.getTimeMS() > 500;
    }

    private void deleteAllAndCommit(Connection con) throws SQLException {
        String sql = "delete from " + TABLE;
        PreparedStatement ps = con.prepareStatement(sql);
        int rowCount = ps.executeUpdate();
        ps.close();
        con.commit();
    }

    private void insertRow(Connection con) throws SQLException {
        String sql = "insert into " + TABLE + " (id, name) values (1, 'name1')";
        PreparedStatement ps = con.prepareStatement(sql);
        int rowCount = ps.executeUpdate();
        ps.close();
    }

    private void updateRow(Connection con) throws SQLException {
        String sql = "update " + TABLE + " set name = ? where id = ?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setString(1, "abc");
        ps.setInt(2, 1);
        int rowCount = ps.executeUpdate();
        ps.close();
    }

    private int countRows(Connection con) throws SQLException {
        String sql = "select count(*) from " + TABLE;
        PreparedStatement ps = con.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();
        rs.next();
        int count = rs.getInt(1);
        ps.close();
        return count;
    }

    private void readRows(Connection con) throws SQLException {
        String sql = "select id, name from " + TABLE;
        PreparedStatement ps = con.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            rs.getInt(1);
            rs.getString(2);
        }
        ps.close();
    }

    class SecondInsertThread extends Thread {
        private final Connection con;

        public SecondInsertThread(Connection con) {
            this.con = con;
        }

        @Override
        public void run() {
            try {
                insertRow(con);
                con.commit();
            }
            catch (SQLException ex) {
                System.out.println("thread 2 failed to insert (ok)");
                try { con.commit(); } catch (Exception ex2) { }
            }
        }
    }

    class UpdateThread extends Thread {
        private final Connection con;
        private volatile long timeMS = -1;

        public UpdateThread(Connection con) {
            this.con = con;
        }

        @Override
        public void run() {
            try {
                long startTime = System.currentTimeMillis();
                updateRow(con);
                con.commit();
                this.timeMS = System.currentTimeMillis() - startTime;
            }
            catch (Exception ex) {
                System.err.println("Unexpected exception in UpdateThread: " + ex);
            }
        }

        public long getTimeMS() {
            return timeMS;
        }
    }
}

/**
---- SQL to create tables in DB2:
-- change varchar to nvarchar when db2 supports

create table isolation_test (
  id int not null generated by default as identity
    constraint pk_isolation primary key,
  name varchar(100)
);


---- SQL to create tables in MySQL:

create table isolation_test (
  id int not null auto_increment,
  constraint pk_isolation primary key (id),
  name nvarchar(100)
);

*/