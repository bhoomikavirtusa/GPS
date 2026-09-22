package com.wiley.sf.common.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Try and create a deadlock by duplicating this scenario:
 * http://thushw.blogspot.com/2010/11/mysql-deadlocks-with-concurrent-inserts.html
 *
 * @since   JDK 1.6
 * @version 3/23/2011
 * @author  Steve Markoff
 */
public class CreateInsertDeadlock {

    private final static String TABLE = "DEAD_INSERT";


    public static void main(String [] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: <properties file>");
            System.exit(1);
        }

        SimpleDBConnect dbConnect = SimpleDBConnect.create(args[0]);
            // throws various exceptions

        CreateInsertDeadlock cid = new CreateInsertDeadlock(dbConnect.getConnection(),
                dbConnect.getConnection(), dbConnect.getConnection(), dbConnect.getConnection());
        cid.go();
    }


    private final Connection con1;
    private final Connection con2;
    private final Connection con3;
    private final Connection con4;

    public CreateInsertDeadlock(Connection con1, Connection con2, Connection con3, Connection con4) throws SQLException {
        this.con1 = con1;
        this.con2 = con2;
        this.con3 = con3;
        this.con4 = con4;
        con1.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        con2.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        con3.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        con4.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        con1.setAutoCommit(false);
        con2.setAutoCommit(false);
        con3.setAutoCommit(false);
        con4.setAutoCommit(false);
    }

    public void go() throws SQLException {
        deleteAllAndCommit(con1);
        new InsertThread(con1, true, 1).start();
        new InsertThread(con2, true, 2).start();
        new InsertThread(con4, true, 4).start();
        new SelectThread(con4).start();
    }


    private void insert(Connection con, String code) throws SQLException {
        String sql = "insert into " + TABLE + " (code, name) values (?, ?)";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setString(1, code);
        ps.setString(2, code);
        int rowCount = ps.executeUpdate();
        ps.close();

        // This SQL is MySQL-specific
        sql = "select LAST_INSERT_ID()";
        ps = con.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            rs.getInt(1);
        }
        ps.close();

        con.commit();
    }

    private void select(Connection con) throws SQLException {
        String sql = "select id, code, name from " + TABLE;
        PreparedStatement ps = con.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            int id = rs.getInt(1);
            String code = rs.getString(2);
            String name = rs.getString(3);
        }
        ps.close();
        con.commit();
    }

    private void deleteAllAndCommit(Connection con) throws SQLException {
        String sql = "delete from " + TABLE;
        PreparedStatement ps = con.prepareStatement(sql);
        int rowCount = ps.executeUpdate();
        ps.close();
        con.commit();
    }

    class InsertThread extends Thread {
        private final Connection con;
        private final boolean startLow;
        private int threadNum;
        private int startValue;

        public InsertThread(Connection con, boolean startLow, int threadNum) {
            this.con = con;
            this.startLow = startLow;
            this.threadNum = threadNum;

            if (startLow) startValue = 1;
            else  startValue = 1000000;
        }

        @Override
        public void run() {
            String code = String.valueOf(startValue);

            for (int i = 0; i < 10000; i++) {
                doInsert(code);

                if (startLow) startValue += 2;
                else startValue -= 2;
                code = String.valueOf(startValue);
            }
        }

        private void doInsert(String code) {
            try {
                insert(con, code);
                System.out.println("thread " + threadNum + " inserted code " + code);
            }
            catch (Exception ex) {
                System.out.println("thread " + threadNum + " Caught exception trying to insert code " + code + ": " + ex);
            }
        }
    }

    class SelectThread extends Thread {
        private final Connection con;
        private int count = 0;

        public SelectThread(Connection con) {
            this.con = con;
            setDaemon(true);  // so will die when no other threads are running
        }

        @Override
        public void run() {
            while (true) {
                doSelect();
            }
        }

        private void doSelect() {
            try {
                select(con);
                count++;
                System.out.println("select count = " + count);
            }
            catch (Exception ex) {
                System.out.println("Select thread caught exception: " + ex);
            }
        }
    }
}

/**
---- SQL to create tables in DB2:
-- change varchar to nvarchar when db2 supports

create table dead_insert (
  id int not null generated by default as identity
    constraint pk_dead_insert primary key,
  code varchar(20) not null,
  name varchar(100)
);

alter table dead_insert
  add constraint un_insert_code unique (code);

---- SQL to create tables in MySQL:

create table dead_insert (
  id int not null auto_increment,
  constraint pk_dead_insert primary key (id),
  code nvarchar(20),
  name nvarchar(100)
);

alter table dead_insert
  add constraint un_insert_code unique (code);
*/