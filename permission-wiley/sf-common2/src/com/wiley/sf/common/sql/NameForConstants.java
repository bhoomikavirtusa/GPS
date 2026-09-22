package com.wiley.sf.common.sql;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Types;

/**
 * Provides a way to print out a name corresponding to the constants in the
 * JDBC classes, since the JDK does not currently seem to
 * provide such a mechanism.
 *
 * @since   JDK 1.6
 * @version 3/21/2011
 * @author  Steve Markoff
 */
public class NameForConstants {

    public static String nameForTransactionIsolation(int ti) {
        switch (ti) {
            case Connection.TRANSACTION_NONE:
                return "None";

            case Connection.TRANSACTION_READ_UNCOMMITTED:
                return "Read Uncommitted";

            case Connection.TRANSACTION_READ_COMMITTED:
                return "Read Committed";

            case Connection.TRANSACTION_REPEATABLE_READ:
                return "Repeatable Read";

            case Connection.TRANSACTION_SERIALIZABLE:
                return "Serializable";

            default:
                return "(" + ti
                    + " doesn't map to valid Connection transaction isolation)";
        }
    }

    /**
     * An IllegalArgument exception will be thrown if the input name
     * does not match (case-insensitive) any known string that can be output
     * by nameForTransactionIsolation().
     *
     * @param s  Must be non-null
     */
    public static int lookupTransactionIsolationByName(String s) {
        if ("None".equalsIgnoreCase(s))  return Connection.TRANSACTION_NONE;
        else if ("Read Uncommitted".equalsIgnoreCase(s))  return Connection.TRANSACTION_READ_UNCOMMITTED;
        else if ("Read Committed".equalsIgnoreCase(s))  return Connection.TRANSACTION_READ_COMMITTED;
        else if ("Repeatable Read".equalsIgnoreCase(s))  return Connection.TRANSACTION_REPEATABLE_READ;
        else if ("Serializable".equalsIgnoreCase(s))  return Connection.TRANSACTION_SERIALIZABLE;
        else {
            throw new IllegalArgumentException("\"" + s + "\" doesn't match a known Transaction Isolation.");
        }
    }

    public static String nameForDataType(int type) {
        // Note I could probably implement this method by examining the Types
        // class with the Reflection API, but a switch statement is faster.

        switch (type) {
            case Types.ARRAY: return "array";
            case Types.BIGINT: return "bigint";
            case Types.BINARY: return "binary";
            case Types.BIT: return "bit";
            case Types.BLOB: return "blob";
            case Types.BOOLEAN: return "boolean";
            case Types.CHAR: return "char";
            case Types.CLOB: return "clob";
            case Types.DATALINK: return "datalink";
            case Types.DATE: return "date";
            case Types.DECIMAL: return "decimal";
            case Types.DISTINCT: return "distinct";
            case Types.DOUBLE: return "double";
            case Types.FLOAT: return "float";
            case Types.INTEGER: return "integer";
            case Types.JAVA_OBJECT: return "java_object";
            case Types.LONGVARBINARY: return "longvarbinary";
            case Types.LONGVARCHAR: return "longvarchar";
            case Types.NCHAR: return "nchar"; // new in JDK 1.6
            case Types.NCLOB: return "nclob"; // new in JDK 1.6
            case Types.NULL: return "null";
            case Types.NUMERIC: return "numeric";
            case Types.NVARCHAR: return "nvarchar"; // new in JDK 1.6
            case Types.OTHER: return "other";
            case Types.REAL: return "real";
            case Types.REF: return "ref";
            case Types.ROWID: return "rowid"; // new in JDK 1.6
            case Types.SMALLINT: return "smallint";
            case Types.SQLXML: return "sqlxml"; // new in JDK 1.6
            case Types.TIME: return "time";
            case Types.TIMESTAMP: return "timestamp";
            case Types.TINYINT: return "tinyint";
            case Types.VARBINARY: return "varbinary";
            case Types.VARCHAR: return "varchar";

            default:
                return "(doesn't map to JDBC type)";
        } // end switch
    }

    public static String nameForNullability(int n) {
        switch (n) {
            case DatabaseMetaData.columnNoNulls:
                return "Not Nullable";
            case DatabaseMetaData.columnNullable:
                return "Nullable";
            case DatabaseMetaData.columnNullableUnknown:
                return "Unknown Nullability";

            default:
                return "(" + n + " doesn't map to valid nullability)";
        }
    }

    public static String yesNoForNullable(int n) {
        switch (n) {
            case DatabaseMetaData.columnNoNulls:
                return "No";
            case DatabaseMetaData.columnNullable:
                return "Yes";
            case DatabaseMetaData.columnNullableUnknown:
                return "Unknown";

            default:
                return "(" + n + " doesn't map to valid nullability)";
        }
    }

    public static String nameForImportRule(short rule) {
        switch (rule) {
            case DatabaseMetaData.importedKeyNoAction: return "importedKeyNoAction";
            case DatabaseMetaData.importedKeyCascade: return "importedKeyCascade";
            case DatabaseMetaData.importedKeySetNull: return "importedKeySetNull";
            case DatabaseMetaData.importedKeySetDefault: return "importedKeySetDefault";
            case DatabaseMetaData.importedKeyRestrict: return "importedKeyRestrict";
            default: throw new RuntimeException("Invalid rule: " + rule);
        }
    }

    public static String nameForIndexType(short type) {
        switch (type) {
            case DatabaseMetaData.tableIndexStatistic:
                return "Statistic";
            case DatabaseMetaData.tableIndexClustered:
                return "Clustered";
            case DatabaseMetaData.tableIndexHashed:
                return "Hashed";
            case DatabaseMetaData.tableIndexOther:
                return "Other";
            default:
                throw new RuntimeException("Invalid index type: " + type);
        }
    }

    public static String nameForConcurrency(int c) {
        switch (c) {
            case ResultSet.CONCUR_READ_ONLY:
                return "concur read only";

            case ResultSet.CONCUR_UPDATABLE:
                return "concur updatable";

            default:
                return "(doesn't map to valid ResultSet concurrency)";
        }
    }

    public static String nameForRSType(int t) {
        switch (t) {
            case ResultSet.TYPE_FORWARD_ONLY:
                return "type forward only";

            case ResultSet.TYPE_SCROLL_INSENSITIVE:
                return "type scroll INsensitive";

            case ResultSet.TYPE_SCROLL_SENSITIVE:
                return "type scroll sensitive";

            default:
                return "(doesn't map to valid ResultSet type)";
        }
    }

    public static String nameForFetchDirection(int d) {
        switch (d) {
            case ResultSet.FETCH_FORWARD:
                return "fetch forward";
            case ResultSet.FETCH_REVERSE:
                return "fetch reverse";
            case ResultSet.FETCH_UNKNOWN:
                return "fetch unknown";

            default:
                return "(doesn't map to valid fetch direction)";
        }
    }

    public static String nameForHoldability(int h) {
        // These constants new in JDK 1.6
        switch (h) {
            case ResultSet.HOLD_CURSORS_OVER_COMMIT:
                return "hold cursors over commit";
            case ResultSet.CLOSE_CURSORS_AT_COMMIT:
                return "close cursors at commit";

            default:
                return "(doesn't map to valid holdability)";
        }
    }

    public static String nameForProcedureType(short type) {
        switch (type) {
            case DatabaseMetaData.procedureNoResult:
                return "noResult";
            case DatabaseMetaData.procedureReturnsResult:
                return "returnsResult";
            case DatabaseMetaData.procedureResultUnknown:
                return "resultUnknown";

            default:
                return "(doesn't map to valid type)";
        }
    }

    public static String nameForProcedureColumnType(short type) {
        switch (type) {
            case DatabaseMetaData.procedureColumnUnknown:
                return "unknown";
            case DatabaseMetaData.procedureColumnIn:
                return "in";
            case DatabaseMetaData.procedureColumnInOut:
                return "in/out";
            case DatabaseMetaData.procedureColumnOut:
                return "out";
            case DatabaseMetaData.procedureColumnReturn:
                return "return";
            case DatabaseMetaData.procedureColumnResult:
                return "result";

            default:
                return "(doesn't map to valid type)";
        }
    }
}
