package com.wiley.sf.common.sql.bench;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.NumberFormat;

import org.apache.commons.lang3.BooleanUtils;

import com.wiley.sf.common.sql.SimpleDBConnect;
import com.wiley.sf.common.text.TimeFormat;

/**
 *
 * @since   JDK 1.6
 * @version 9/28/2010
 * @author  Steve Markoff
 */
public class Main {

    public static String PRODUCT_TABLE = "BENCH_PRODUCT";
    public static String ASSET_TABLE = "BENCH_ASSET";
    public static String MAP_TABLE = "B_ASSET_2_PRODUCT";
    public static int PRODUCT_INSERT_NUM = 10000;
    public static int ASSETS_PER_PRODUCT = 100;
    public static int EXTRA_INSERT_NUM = 1000;
    public static int SEARCH_NUM = 100;  // must be <= PRODUCT_INSERT_NUM
    public static int JOIN_NUM = 100;


    public static void main(String [] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: <properties file> [force reinsert (yes/no)]");
            System.exit(1);
        }

        SimpleDBConnect dbConnect = SimpleDBConnect.create(args[0]);
            // throws various exceptions
        boolean forceReinsert = false;
        if (args.length > 1) {
            forceReinsert = BooleanUtils.toBoolean(args[1]);
        }
        Main main = new Main(dbConnect.getConnection(), forceReinsert);
        main.go();
    }


    private final Connection con;
    private final boolean forceReinsert;
    private final NumberFormat intFormat = NumberFormat.getIntegerInstance();
    private final TimeFormat timeFormat = new TimeFormat(true);


    public Main(Connection con, boolean forceReinsert) throws SQLException {
        this.con = con;
        this.forceReinsert = forceReinsert;
        con.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        con.setAutoCommit(true);
    }

    public void go() throws SQLException {
        int productRowCount = determineRowCount(PRODUCT_TABLE);
        if (productRowCount != PRODUCT_INSERT_NUM || forceReinsert) {
            delete();
            // We don't rely on the product id starting at 1 but might as well do this
            resetIdentity(PRODUCT_TABLE);
            insertProduct();
        }

        insertAsset();

        System.out.println("---- Product table (" + intFormat.format(PRODUCT_INSERT_NUM) + " rows) ----");
        searchProduct();
        extraInsertProduct(false);
        extraInsertProduct(true);

        System.out.println("---- Asset table (" + intFormat.format(PRODUCT_INSERT_NUM * ASSETS_PER_PRODUCT) + " rows) ----");
        searchAsset();
        extraInsertAsset(false);
        extraInsertAsset(true);

        join();
    }

    private int determineRowCount(String tableName) throws SQLException {
        String sql = "select count(*) from " + tableName;
        PreparedStatement ps = con.prepareStatement(sql);

        ResultSet rs = ps.executeQuery();
        rs.next();
        int rowCount = rs.getInt(1);

        ps.close();
        return rowCount;
    }

    private void resetIdentity(String tableName) throws SQLException {
        int resetValue = 1;
        int rowCount = determineRowCount(tableName);
        if (rowCount > 0) {
            resetValue = determineMaxId(tableName) + 1;
        }

        // Using a parameter marker for the restart value doesn't seem to work, at least with db2.

        DatabaseMetaData md = con.getMetaData();
        String dbName = md.getDatabaseProductName();
        // For DB2 and ANSI (assuming identity used and not sequence)
        // (PostgreSQL is ANSI for sequence)
        String sql = "alter table " + tableName + " alter column id restart with " + resetValue;
        if (dbName.equalsIgnoreCase("Oracle")) {
            // Oracle is special - see below
            sql = null;
        }
        else if (dbName.equalsIgnoreCase("Microsoft SQL Server")) {
            sql = "dbcc checkident (" + tableName + ", RESEED, " + (resetValue - 1) + ")";
        }
        else if (dbName.equalsIgnoreCase("MySQL")) {
            sql = "alter table " + tableName + " auto_increment = " + resetValue;
        }
        else if (dbName.equalsIgnoreCase("PostgreSQL")) {
            sql = "alter sequence " + tableName + "_id_seq restart with " + resetValue;
        }

        if (sql == null) {
            resetIdentityOracle(tableName, resetValue);
        }
        else {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.executeUpdate();
            ps.close();
        }
    }

    private void resetIdentityOracle(String tableName, int resetValue) throws SQLException {
        String seqName = "seq_" + tableName;

        // Use nextval instead of currval because otherwise can get
        // "CURRVAL is not yet defined in this session".
        String sql = "select " + seqName + ".nextval from dual";
        Statement stmt = con.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        rs.next();
        int currentValue = rs.getInt(1);
        stmt.close();

        // We expect currentValue to be >= resetValue.
        int minusValue = currentValue - resetValue + 1;
        if (minusValue == 0) return;

        sql = "alter sequence " + seqName + " increment by -" + minusValue + " minvalue 0";
        stmt = con.createStatement();
        stmt.executeUpdate(sql);
        stmt.close();

        sql = "select " + seqName + ".nextval from dual";
        stmt = con.createStatement();
        stmt.executeUpdate(sql);
        stmt.close();

        sql = "alter sequence " + seqName + " increment by 1";
        stmt = con.createStatement();
        stmt.executeUpdate(sql);
        stmt.close();
    }

    private void delete(String tableName) throws SQLException {
        long startTime = System.currentTimeMillis();
        String sql = "delete from " + tableName;
        PreparedStatement ps = con.prepareStatement(sql);
        int numRows = ps.executeUpdate();
        ps.close();
        long time = System.currentTimeMillis() - startTime;
        System.out.println("deleted " + intFormat.format(numRows) + " rows from "
            + tableName + " in " + timeFormat.formatMS(time, true));
    }

    private void delete() throws SQLException {
        delete(MAP_TABLE);
        delete(PRODUCT_TABLE);
        delete(ASSET_TABLE);
    }

    private void insertProduct() throws SQLException {
        System.out.println("inserting into " + PRODUCT_TABLE + "...");
        long startTime = System.currentTimeMillis();

        String insertProductSql = "insert into " + PRODUCT_TABLE + " (isbn, isbn_indexed, title) values (?, ?, ?)";
        PreparedStatement ps = con.prepareStatement(insertProductSql);

        for (int i = 0; i < PRODUCT_INSERT_NUM; i++) {
            String isbn = String.valueOf(i);
            String title = "title " + i;
            ps.setString(1, isbn);
            ps.setString(2, isbn);
            ps.setString(3, title);
            ps.executeUpdate();
        }

        ps.close();

        long time = System.currentTimeMillis() - startTime;
        System.out.println("inserted " + intFormat.format(PRODUCT_INSERT_NUM)
                + " rows into " + PRODUCT_TABLE + " in " + timeFormat.formatMS(time, true));
    }

    private void insertAsset() throws SQLException {
        int rowCount = determineRowCount(ASSET_TABLE);
        final int targetRowCount = PRODUCT_INSERT_NUM * ASSETS_PER_PRODUCT;
        if (rowCount == targetRowCount) {
            return;
        }

        // If this program is stopped in the middle then often the next identity
        // value is 1 greater than the max id, so take care of that problem here.
        // Also might as well reset to start 1 with if the table is empty but
        // at the moment the rest of the code does not rely on the id starting with 1.
        resetIdentity(ASSET_TABLE);

        int todoRowCount = targetRowCount - rowCount;
        System.out.println("inserting " + intFormat.format(todoRowCount) + " rows into " + ASSET_TABLE + "...");

        int minProductId = determineMinId(PRODUCT_TABLE);

        int productId = minProductId + (rowCount / ASSETS_PER_PRODUCT);
        int assetNum = rowCount % ASSETS_PER_PRODUCT;
        int externalIdInt = rowCount + 1;

        // We want to make sure the number of rows in ASSET_TABLE
        // and MAP_TABLE is the same in case the program is stopped
        // unexpectedly, so commit after each pair of inserts.
        // - Actually we will commit after every ASSETS_PER_PRODUCT pairs of
        // inserts because this is typically faster.
        con.setAutoCommit(false);

        long startTime = System.currentTimeMillis();

        String insertAssetSql = "insert into " + ASSET_TABLE + " (external_id, ext_id_indexed, asset_num) values (?, ?, ?)";
        String insertA2pSql = "insert into " + MAP_TABLE + " (asset_id, product_id) values (?, ?)";
        PreparedStatement ps = con.prepareStatement(insertAssetSql, Statement.RETURN_GENERATED_KEYS);
        PreparedStatement ps2 = con.prepareStatement(insertA2pSql);

        long setStartTime = System.currentTimeMillis();

        for (int i = 0; i < todoRowCount; i++) {
            String externalId = "a" + externalIdInt;
            ps.setString(1, externalId);
            ps.setString(2, externalId);
            ps.setInt(3, assetNum);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            rs.next();  // we expect exactly one generated key
            int assetId = rs.getInt(1);
            rs.close();

            ps2.setInt(1, assetId);
            ps2.setInt(2, productId);
            ps2.executeUpdate();

            //con.commit();

            externalIdInt++;
            assetNum++;
            if (assetNum == ASSETS_PER_PRODUCT) {
                con.commit();

                long tempTime = System.currentTimeMillis();
                long setTime = tempTime - setStartTime;
                setStartTime = tempTime;
                System.out.println("time to insert the last " + ASSETS_PER_PRODUCT + " (x2) rows = " + intFormat.format(setTime) + " ms");

                assetNum = 0;
                productId++;
            }
        }

        ps.close();
        ps2.close();

        long time = System.currentTimeMillis() - startTime;
        System.out.println("inserted " + intFormat.format(todoRowCount)
                + " rows into " + ASSET_TABLE + " and " + MAP_TABLE
                + " in " + timeFormat.formatMS(time, true));

        con.setAutoCommit(true);
    }

    private void extraInsertProduct(boolean oneTransaction) throws SQLException {
        resetIdentity(PRODUCT_TABLE);  // so we are always testing exactly the same situation

        if (oneTransaction) {
            con.setAutoCommit(false);
        }

        long startTime = System.currentTimeMillis();

        int maxProductId = determineMaxId(PRODUCT_TABLE);

        String insertProductSql = "insert into " + PRODUCT_TABLE + " (isbn, isbn_indexed, title) values (?, ?, ?)";
        PreparedStatement ps = con.prepareStatement(insertProductSql);

        for (int i = 0; i < EXTRA_INSERT_NUM; i++) {
            String isbn = String.valueOf(maxProductId + 1 + i);
            String title = "title " + (maxProductId + 1 + i);
            ps.setString(1, isbn);
            ps.setString(2, isbn);
            ps.setString(3, title);
            ps.executeUpdate();
        }

        ps.close();

        if (oneTransaction) {
            con.commit();
        }

        long time = System.currentTimeMillis() - startTime;
        String transactionMsg = oneTransaction ? "[all in one transaction]" : "[each in separate transaction]";
        System.out.println("inserted " + intFormat.format(EXTRA_INSERT_NUM)
            + " extra rows in " + intFormat.format(time) + " ms. " + transactionMsg);

        startTime = System.currentTimeMillis();

        // Don't do "delete from " + PRODUCT_TABLE + " where id > ?" because
        // this is not how a real application would do it.
        String sql = "delete from " + PRODUCT_TABLE + " where id = ?";
        ps = con.prepareStatement(sql);

        for (int i = 0; i < EXTRA_INSERT_NUM; i++) {
            ps.setInt(1, maxProductId + 1 + i);
            ps.executeUpdate();
        }

        ps.close();

        if (oneTransaction) {
            con.commit();
            con.setAutoCommit(true);
        }

        time = System.currentTimeMillis() - startTime;
        System.out.println("deleted " + intFormat.format(EXTRA_INSERT_NUM)
            + " extra rows in " + intFormat.format(time) + " ms. " + transactionMsg);
    }

    private void extraInsertAsset(boolean oneTransaction) throws SQLException {
        resetIdentity(ASSET_TABLE);  // so we are always testing exactly the same situation

        if (oneTransaction) {
            con.setAutoCommit(false);
        }

        long startTime = System.currentTimeMillis();

        int maxProductId = determineMaxId(ASSET_TABLE);

        String insertProductSql = "insert into " + ASSET_TABLE + " (external_id, ext_id_indexed, asset_num) values (?, ?, ?)";
        PreparedStatement ps = con.prepareStatement(insertProductSql);

        for (int i = 0; i < EXTRA_INSERT_NUM; i++) {
            String externalId = "a " + (maxProductId + 1 + i);
            ps.setString(1, externalId);
            ps.setString(2, externalId);
            ps.setInt(3, 0);
            ps.executeUpdate();
        }

        ps.close();

        if (oneTransaction) {
            con.commit();
        }

        long time = System.currentTimeMillis() - startTime;
        String transactionMsg = oneTransaction ? "[all in one transaction]" : "[each in separate transaction]";
        System.out.println("inserted " + intFormat.format(EXTRA_INSERT_NUM)
            + " extra rows in " + intFormat.format(time) + " ms. " + transactionMsg);

        startTime = System.currentTimeMillis();

        // Don't do "delete from " + ASSET_TABLE + " where id > ?" because
        // this is not how a real application would do it.
        String sql = "delete from " + ASSET_TABLE + " where id = ?";
        ps = con.prepareStatement(sql);

        for (int i = 0; i < EXTRA_INSERT_NUM; i++) {
            ps.setInt(1, maxProductId + 1 + i);
            ps.executeUpdate();
        }

        ps.close();

        if (oneTransaction) {
            con.commit();
            con.setAutoCommit(true);
        }

        time = System.currentTimeMillis() - startTime;
        System.out.println("deleted " + intFormat.format(EXTRA_INSERT_NUM)
            + " extra rows in " + intFormat.format(time) + " ms. " + transactionMsg);
    }

    private int determineMinId(String tableName) throws SQLException {
        String sql = "select min(id) from " + tableName;
        PreparedStatement ps = con.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();
        rs.next();
        int minId = rs.getInt(1);
        ps.close();
        return minId;
    }

    private int determineMaxId(String tableName) throws SQLException {
        String sql = "select max(id) from " + tableName;
        PreparedStatement ps = con.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();
        rs.next();
        int maxId = rs.getInt(1);
        ps.close();
        return maxId;
    }

    /**
     * Might be nice to randomize the searches but really it won't make a different because
     * we are searching using a parameter marker for the id - which means the db won't cache
     * the query for any particular id.
     */

    private void searchProduct() throws SQLException {
        int minProductId = determineMinId(PRODUCT_TABLE);
        int jump = PRODUCT_INSERT_NUM / SEARCH_NUM;

        search(minProductId, jump, null, "select * from " + PRODUCT_TABLE + " where id = ?", "by id");
        search(1, jump, "", "select * from " + PRODUCT_TABLE + " where isbn = ?", "by non-indexed short string");
        search(1, jump, "", "select * from " + PRODUCT_TABLE + " where isbn_indexed = ?", "by indexed short string");
    }

    private void searchAsset() throws SQLException {
        int minAssetId = determineMinId(ASSET_TABLE);
        int jump = PRODUCT_INSERT_NUM * ASSETS_PER_PRODUCT / SEARCH_NUM;

        search(minAssetId, jump, null, "select * from " + ASSET_TABLE + " where id = ?", "by id");
        search(1, jump, "a", "select * from " + ASSET_TABLE + " where external_id = ?", "by non-indexed short string");
        search(1, jump, "a", "select * from " + ASSET_TABLE + " where ext_id_indexed = ?", "by indexed short string");
    }

    private void search(int start, int jump, String prefix, String sql, String msg) throws SQLException {
        PreparedStatement ps = con.prepareStatement(sql);

        long startTime = System.currentTimeMillis();

        int whereValue = start;

        for (int s = 0; s < SEARCH_NUM; s++) {
            if (prefix == null) {
                ps.setInt(1, whereValue);
            }
            else {
                ps.setString(1, prefix + whereValue);
            }
            ResultSet rs = ps.executeQuery();
            // we expect exactly 1 row
            rs.next();
            rs.getString(2);

            whereValue += jump;
        }

        long time = System.currentTimeMillis() - startTime;
        System.out.println("" + SEARCH_NUM + " searches " + msg + " = " + intFormat.format(time) + " ms.");

        ps.close();
    }

    private void join() throws SQLException {
        System.out.println("---- Join on Many-to-Many ----");
        System.out.println("-- Without index on product_id:");
        join2();

        String sql = "create index idx_b_a2p_product_id on " + MAP_TABLE + " (product_id)";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.executeUpdate();
        ps.close();

        System.out.println("-- With index on product_id:");
        join2();

        DatabaseMetaData md = con.getMetaData();
        String dbName = md.getDatabaseProductName();
        // For DB2, Oracle, and PostgreSQL
        sql = "drop index idx_b_a2p_product_id";
        if (dbName.equalsIgnoreCase("MySQL")) {
            sql = "alter table " + MAP_TABLE + " drop index idx_b_a2p_product_id";
        }
        else if (dbName.equalsIgnoreCase("Microsoft SQL Server")) {
            sql = "drop index idx_b_a2p_product_id on " + MAP_TABLE;
        }
        ps = con.prepareStatement(sql);
        ps.executeUpdate();
        ps.close();
    }

    private void join2() throws SQLException {
        String sql = "select id, external_id, ext_id_indexed, asset_num from "
            + ASSET_TABLE + " a, " + MAP_TABLE + " map"
            + " where map.product_id = ? and a.id = map.asset_id";
        PreparedStatement ps = con.prepareStatement(sql);

        long startTime = System.currentTimeMillis();

        int productId = determineMinId(PRODUCT_TABLE);
        int jump = PRODUCT_INSERT_NUM / JOIN_NUM;

        for (int j = 0; j < JOIN_NUM; j++) {
            ps.setInt(1, productId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                rs.getInt(1);
                rs.getString(2);
                rs.getString(3);
                rs.getInt(4);
            }

            productId += jump;
        }

        long time = System.currentTimeMillis() - startTime;
        System.out.println("Time to do " + JOIN_NUM + " queries for all Assets for a Product = " + intFormat.format(time) + " ms.");

        ps.close();
    }
}

/**
---- SQL to create tables in DB2:
-- change varchar to nvarchar when db2 supports

create table bench_product (
  id int not null generated by default as identity
    constraint pk_bench_product primary key,
  isbn varchar(13),
  isbn_indexed varchar(13),
  title varchar(200) not null
);

create index idx_isbn on bench_product(isbn_indexed);

create table bench_asset (
  id int not null generated by default as identity
    constraint pk_bench_asset primary key,
  external_id varchar(50) not null,
  ext_id_indexed varchar(50) not null,
  asset_num int not null
);

-- declaring a unique constraint gets an automatic index

alter table bench_asset
  add constraint un_b_a_ext_id_idx unique (ext_id_indexed);

create table b_asset_2_product (
  asset_id int not null,
  product_id int not null
);

alter table b_asset_2_product
  add constraint pk_b_a2p primary key (asset_id, product_id);

alter table b_asset_2_product
  add constraint fk_ba2p_2_a foreign key (asset_id) references bench_asset (id);

alter table b_asset_2_product
  add constraint fk_ba2p_2_p foreign key (product_id) references bench_product (id);

-- index on b_asset_2_product(product_id) will be added/dropped by the program


---- SQL to create tables in SQL Server:
-- Always use nvarchar because JDBC driver sends all strings as Unicode

create table bench_product (
  id int not null identity (1, 1) constraint pk_bench_product primary key,
  isbn nvarchar(13),
  isbn_indexed nvarchar(13),
  title nvarchar(200) not null
);

create index idx_isbn on bench_product(isbn_indexed);

create table bench_asset (
  id int not null identity (1, 1) constraint pk_bench_asset primary key,
  external_id nvarchar(50) not null,
  ext_id_indexed nvarchar(50) not null,
  asset_num int not null
);

-- declaring a unique constraint gets an automatic index

alter table bench_asset
  add constraint un_b_a_ext_id_idx unique (ext_id_indexed);

create table b_asset_2_product (
  asset_id int not null,
  product_id int not null
);

alter table b_asset_2_product
  add constraint pk_b_a2p primary key (asset_id, product_id);

alter table b_asset_2_product
  add constraint fk_ba2p_2_a foreign key (asset_id) references bench_asset (id);

alter table b_asset_2_product
  add constraint fk_ba2p_2_p foreign key (product_id) references bench_product (id);

-- index on b_asset_2_product(product_id) will be added/dropped by the program


---- SQL to create tables in Oracle:
-- would use nvarchar instead of varchar for some reason 10g is complaining

create table bench_product (
  id number(10) not null constraint pk_bench_product primary key,
  isbn varchar(13),
  isbn_indexed varchar(13),
  title varchar(200) not null
);

create sequence seq_bench_product;

create or replace trigger trig_bench_product
before insert on bench_product
for each row
when (new.id is null)
begin
  SELECT seq_bench_product.nextval INTO :new.id FROM dual;
end;

create index idx_isbn on bench_product(isbn_indexed);

create table bench_asset (
  id number(10) not null constraint pk_bench_asset primary key,
  external_id varchar(50) not null,
  ext_id_indexed varchar(50) not null,
  asset_num int not null
);

create sequence seq_bench_asset;

create or replace trigger trig_bench_asset
before insert on bench_asset
for each row
when (new.id is null)
begin
  SELECT seq_bench_asset.nextval INTO :new.id FROM dual;
end;

-- declaring a unique constraint gets an automatic index

alter table bench_asset
  add constraint un_b_a_ext_id_idx unique (ext_id_indexed);

create table b_asset_2_product (
  asset_id int not null,
  product_id int not null
);

alter table b_asset_2_product
  add constraint pk_b_a2p primary key (asset_id, product_id);

alter table b_asset_2_product
  add constraint fk_ba2p_2_a foreign key (asset_id) references bench_asset (id);

alter table b_asset_2_product
  add constraint fk_ba2p_2_p foreign key (product_id) references bench_product (id);

-- index on b_asset_2_product(product_id) will be added/dropped by the program


---- SQL to create tables in MySQL:

create table bench_product (
  id int not null auto_increment,
  constraint pk_bench_product primary key (id),
  isbn nvarchar(13),
  isbn_indexed nvarchar(13),
  title nvarchar(200) not null
);

create index idx_isbn on bench_product(isbn_indexed);

create table bench_asset (
  id int not null auto_increment,
  constraint pk_bench_asset primary key (id),
  external_id nvarchar(50) not null,
  ext_id_indexed nvarchar(50) not null,
  asset_num int not null
);

-- declaring a unique constraint gets an automatic index

alter table bench_asset
  add constraint un_b_a_ext_id_idx unique (ext_id_indexed);

create table b_asset_2_product (
  asset_id int not null,
  product_id int not null
);

alter table b_asset_2_product
  add constraint pk_b_a2p primary key (asset_id, product_id);

alter table b_asset_2_product
  add constraint fk_ba2p_2_a foreign key (asset_id) references bench_asset (id);

alter table b_asset_2_product
  add constraint fk_ba2p_2_p foreign key (product_id) references bench_product (id);

-- index on b_asset_2_product(product_id) will be added/dropped by the program


---- SQL to create tables in PostgreSQL:

create table bench_product (
  id serial not null constraint pk_bench_product primary key,
  isbn varchar(13),
  isbn_indexed varchar(13),
  title varchar(200) not null
);

create index idx_isbn on bench_product(isbn_indexed);

create table bench_asset (
  id serial not null constraint pk_bench_asset primary key,
  external_id varchar(50) not null,
  ext_id_indexed varchar(50) not null,
  asset_num int not null
);

-- declaring a unique constraint gets an automatic index

alter table bench_asset
  add constraint un_b_a_ext_id_idx unique (ext_id_indexed);

create table b_asset_2_product (
  asset_id int not null,
  product_id int not null
);

alter table b_asset_2_product
  add constraint pk_b_a2p primary key (asset_id, product_id);

alter table b_asset_2_product
  add constraint fk_ba2p_2_a foreign key (asset_id) references bench_asset (id);

alter table b_asset_2_product
  add constraint fk_ba2p_2_p foreign key (product_id) references bench_product (id);

-- index on b_asset_2_product(product_id) will be added/dropped by the program
*/