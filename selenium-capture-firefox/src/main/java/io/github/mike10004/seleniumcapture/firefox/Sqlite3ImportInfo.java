package io.github.mike10004.seleniumcapture.firefox;

import java.util.Collections;
import java.util.List;

public interface Sqlite3ImportInfo {

    static Sqlite3ImportInfo create(String tableName, List<String> columns, String createTableSql) {
        return create(tableName, columns, Collections.singletonList(createTableSql));
    }

    static Sqlite3ImportInfo create(String tableName, List<String> columns, List<String> createTableSql) {
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
        };
    }

    String tableName();

    List<String> columnNames();

    List<String> createTableSqlStatements();

    default String defaultCellValue() {
        return "";
    }
}
