package io.github.mike10004.seleniumcapture.firefox;

import com.google.common.base.MoreObjects;

import javax.annotation.Nullable;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Interface of a value class that contains information
 * used for import into a sqlite database.
 */
public interface Sqlite3ImportInfo {

    /**
     * Creates an instance.
     * @param tableName table name
     * @param columns columns list
     * @param createTableSql sql statements to create table
     * @return instance
     */
    static Sqlite3ImportInfo create(String tableName,
                                    List<String> columns,
                                    List<String> createTableSql,
                                    @Nullable String idColumnName) {
        requireNonNull(tableName, "tableName");
        requireNonNull(columns, "columns");
        requireNonNull(createTableSql, "createTableSql");
        if (columns.isEmpty()) {
            throw new IllegalArgumentException("columns list must be nonempty");
        }
        if (createTableSql.isEmpty()) {
            throw new IllegalArgumentException("create table statements list must be nonempty");
        }
        return new Sqlite3ImportInfo() {
            @Override
            public String tableName() {
                return tableName;
            }

            @Override
            public List<String> columnNames() {
                return columns;
            }

            @Override
            public List<String> createTableSqlStatements() {
                return createTableSql;
            }

            @Override
            public String idColumnName() {
                return idColumnName;
            }

            @Override
            public String toString() {
                return MoreObjects.toStringHelper(Sqlite3ImportInfo.class)
                        .add("tableName", tableName)
                        .add("columns.size", columns.size())
                        .add("idColumnName", idColumnName)
                        .add("createTableStatements.size", createTableSql.size())
                        .toString();
            }
        };
    }

    String tableName();

    List<String> columnNames();

    List<String> createTableSqlStatements();

    String idColumnName();

    default String defaultCellValue() {
        return "";
    }

}
