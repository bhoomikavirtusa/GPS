package com.wiley.sf.common.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.wiley.sf.common.lang.ArgUtil;

/**
 * Determine what commit frequency is best for insert/update/selects.
 * (Could be after every operation, after every 10, etc.)
 *
 * @since   JDK 1.6
 * @version 4/25/2011
 * @author  Steve Markoff
 */
public class BenchCommitFrequency {

    private final static String TABLE = "BENCH_USER";
    private final static int NUM_ROWS = 10000;


    public static void main(String [] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: <properties file>");
            System.exit(1);
        }

        SimpleDBConnect dbConnect = SimpleDBConnect.create(args[0]);
            // throws various exceptions

        BenchCommitFrequency bench = new BenchCommitFrequency(dbConnect.getConnection());
        bench.setFrequency(1);
        bench.go();

        System.out.println("----");
        bench.setFrequency(10);
        bench.go();

        System.out.println("----");
        bench.setFrequency(100);
        bench.go();

        System.out.println("----");
        bench.setFrequency(1000);
        bench.go();

        System.out.println("----");
        bench.setFrequency(10000);
        bench.go();
    }


    private final Connection con;
    private int frequency = 1;

    public BenchCommitFrequency(Connection con) throws SQLException {
        this.con = con;
        con.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        con.setAutoCommit(false);
    }

    /**
     * @param frequency  Must be 1 or greater
     */
    public void setFrequency(int frequency) {
        ArgUtil.notLess1(frequency, "frequency");
        this.frequency = frequency;
    }

    public void go() throws SQLException {
        deleteAllAndCommit();

        long startTime = System.currentTimeMillis();
        insertAll();
        long time = System.currentTimeMillis() - startTime;
        System.out.println("insert time for frequency " + frequency + " was " + time + " ms.");

        startTime = System.currentTimeMillis();
        updateAll();
        time = System.currentTimeMillis() - startTime;
        System.out.println("update time for frequency " + frequency + " was " + time + " ms.");

        startTime = System.currentTimeMillis();
        selectAll();
        time = System.currentTimeMillis() - startTime;
        System.out.println("select time for frequency " + frequency + " was " + time + " ms.");
    }


    private void deleteAllAndCommit() throws SQLException {
        String sql = "delete from " + TABLE;
        PreparedStatement ps = con.prepareStatement(sql);
        int rowCount = ps.executeUpdate();
        //System.out.println("deleted " + rowCount + " rows.");
        ps.close();
        con.commit();
    }

    private void insertAll() throws SQLException {
        String sql = "insert into " + TABLE + " (username, first_name, last_name, email, notes) values (?, ?, ?, ?, ?)";
        PreparedStatement ps = con.prepareStatement(sql);

        for (int i = 1; i <= NUM_ROWS; i++) {
            String username = "user." + i;
            String firstName = "first." + i;
            String lastName = "last." + i;
            String email = "email." + i;
            String notes = "notes." + i;
            ps.setString(1, username);
            ps.setString(2, firstName);
            ps.setString(3, lastName);
            ps.setString(4, email);
            ps.setString(5, notes);
            int rowCount = ps.executeUpdate();
            if (i % frequency == 0) {
                con.commit();
            }
        }

        ps.close();
        con.commit();
    }

    private void updateAll() throws SQLException {
        String sql = "update " + TABLE + " set notes = ? where username = ?";
        PreparedStatement ps = con.prepareStatement(sql);

        for (int i = 1; i <= NUM_ROWS; i++) {
            String username = "user." + i;
            ps.setString(1, "The quick brown fox jumped over the lazy dogs.");
            ps.setString(2, username);
            int rowCount = ps.executeUpdate();
            if (i % frequency == 0) {
                con.commit();
            }
        }

        ps.close();
        con.commit();
    }

    private void selectAll() throws SQLException {
        String sql = "select username, first_name, last_name, email, notes from " + TABLE + " where username = ?";
        PreparedStatement ps = con.prepareStatement(sql);

        for (int i = 1; i <= NUM_ROWS; i++) {
            String username = "user." + i;
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                rs.getString(2);
                rs.getString(3);
                rs.getString(4);
                rs.getString(5);
            }
            rs.close();
            if (i % frequency == 0) {
                con.commit();
            }
        }

        ps.close();
        con.commit();
    }
}

/**
---- SQL to create tables in DB2:
-- change varchar to nvarchar when db2 supports

create table bench_user (
  id int not null generated by default as identity
    constraint pk_bench_person primary key,
  username varchar(50) not null,
  first_name varchar(50) not null,
  last_name varchar(50) not null,
  email varchar(50) not null,
  notes varchar(1000)
);

alter table bench_user
  add constraint un_bench_user unique (username);

---- SQL to create tables in MySQL:

create table bench_user (
  id int not null auto_increment,
  constraint pk_bench_user primary key (id),
  username nvarchar(50) not null,
  first_name nvarchar(50) not null,
  last_name nvarchar(50) not null,
  email nvarchar(50) not null,
  notes nvarchar(1000)
);

alter table bench_user
  add constraint un_bench_user unique (username);
*/