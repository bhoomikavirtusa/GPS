package com.wiley.sf.common.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.wiley.sf.common.lang.ArgUtil;

/**
 *
 * @since   JDK 1.6
 * @version 4/15/2011
 * @author  Steve Markoff
 */
public class MySqlCompareVariables {

    public static void main(String [] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: <connection properties file1> <file2>");
            System.exit(1);
        }

        SimpleDBConnect dbConnect1 = SimpleDBConnect.create(args[0]);
            // throws various exceptions
        SimpleDBConnect dbConnect2 = SimpleDBConnect.create(args[1]);

        MySqlCompareVariables compare = new MySqlCompareVariables(dbConnect1.getConnection(),
                dbConnect2.getConnection());
        compare.go();
    }


    private final Connection con1;
    private final Connection con2;


    /**
     *
     * @param con1  Must be non-null
     * @param con2  Must be non-null
     *
     * @throws SQLException
     */
    public MySqlCompareVariables(Connection con1, Connection con2)
    throws SQLException
    {
        ArgUtil.notNull(con1, "con1");
        ArgUtil.notNull(con2, "con2");
        this.con1 = con1;
        this.con2 = con2;

        con1.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        con2.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        con1.setAutoCommit(true);
        con2.setAutoCommit(true);
    }

    public void go() throws SQLException {
        HashMap<String, String> map1 = readVariables(con1);
        HashMap<String, String> map2 = readVariables(con2);
        Set<String> set1 = map1.keySet();
        Set<String> set2 = map2.keySet();
        HashSet<String> shared = new HashSet<String>();
        shared.addAll(set1);
        shared.retainAll(set2);

        int diffCount = 0;
        for (String key: shared) {
            String value1 = map1.get(key);
            String value2 = map2.get(key);
            if (!value1.equals(value2)) {
                System.out.println(key + ": db1 = " + value1);
                System.out.println(key + ": db2 = " + value2);
                diffCount++;
            }
        }

        if (diffCount == 0) {
            System.out.println("There were no differences between the values for db1 vs. db2.");
        }

        set1.removeAll(shared);
        set2.removeAll(shared);

        printSetExtra(set1, map1, "db1", "db2");
        printSetExtra(set2, map2, "db2", "db1");
    }

    private void printSetExtra(Set<String> set, Map<String, String> map, String db1Name, String db2Name) {
        if (set.size() == 0) {
            System.out.println(db1Name + " doesn't contain any variables that " + db2Name + " doesn't have");
        }
        else {
            System.out.println(db1Name + " contains the following variables that " + db2Name + " doesn't have:");
            for (String key: set) {
                System.out.println(key + " = " + map.get(key));
            }
        }
    }

    private HashMap<String, String> readVariables(Connection con) throws SQLException {
        String sql = "show variables";
        PreparedStatement ps = con.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();
        HashMap<String, String> map = new HashMap<String, String>();
        while (rs.next()) {
            String name = rs.getString(1);
            String value = rs.getString(2);
            map.put(name, value);
        }
        ps.close();

        return map;
    }
}