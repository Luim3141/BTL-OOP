package library.database;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A lightweight SQL-like database engine used for the library management demo application.
 * <p>
 * The implementation purposely supports only a very small subset of SQL that is necessary for the
 * application logic. It understands CREATE TABLE, INSERT, SELECT, UPDATE and DELETE statements with
 * simple equality based WHERE clauses. The data is stored on disk using Java serialisation to
 * provide persistence between application runs.
 * </p>
 */
public class SimpleSqlDatabase {
    private static final Pattern CREATE_TABLE = Pattern.compile(
            "CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?(\\w+)\\s*\\((.+)\\)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern INSERT = Pattern.compile(
            "INSERT\\s+INTO\\s+(\\w+)\\s*\\(([^)]+)\\)\\s*VALUES\\s*\\(([^)]+)\\)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern SELECT = Pattern.compile(
            "SELECT\\s+(.+)\\s+FROM\\s+(\\w+)(?:\\s+WHERE\\s+(.+))?",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern UPDATE = Pattern.compile(
            "UPDATE\\s+(\\w+)\\s+SET\\s+(.+?)(?:\\s+WHERE\\s+(.+))?",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern DELETE = Pattern.compile(
            "DELETE\\s+FROM\\s+(\\w+)(?:\\s+WHERE\\s+(.+))?",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private final Path storageFile;
    private Map<String, Table> tables;

    public SimpleSqlDatabase(String storagePath) {
        this.storageFile = Paths.get(storagePath);
        this.tables = new HashMap<>();
        load();
    }

    public synchronized void execute(String sql) {
        sql = sanitise(sql);
        Matcher matcher = CREATE_TABLE.matcher(sql);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Unsupported statement: " + sql);
        }
        String tableName = matcher.group(1).toLowerCase(Locale.ROOT);
        String columnsPart = matcher.group(2);
        List<String> columnDefinitions = splitCommaSeparated(columnsPart);
        Table table = tables.computeIfAbsent(tableName, key -> new Table(tableName));
        boolean updated = false;
        if (!table.columns.isEmpty()) {
            for (String columnDef : columnDefinitions) {
                Column column = parseColumnDefinition(columnDef.trim());
                if (table.getColumn(column.name) == null) {
                    table.columns.add(column);
                    for (Map<String, Object> row : table.rows) {
                        row.putIfAbsent(column.name, null);
                    }
                    updated = true;
                }
                if (column.autoIncrement) {
                    table.autoIncrementColumn = column.name;
                }
            }
            if (updated) {
                persist();
            }
            return;
        }
        for (String columnDef : columnDefinitions) {
            Column column = parseColumnDefinition(columnDef.trim());
            table.columns.add(column);
            if (column.autoIncrement) {
                table.autoIncrementColumn = column.name;
            }
        }
        persist();
    }

    public synchronized int update(String sql, Object... params) {
        sql = sanitise(sql);
        Matcher insertMatcher = INSERT.matcher(sql);
        if (insertMatcher.matches()) {
            return handleInsert(insertMatcher, params);
        }
        Matcher updateMatcher = UPDATE.matcher(sql);
        if (updateMatcher.matches()) {
            return handleUpdate(updateMatcher, params);
        }
        Matcher deleteMatcher = DELETE.matcher(sql);
        if (deleteMatcher.matches()) {
            return handleDelete(deleteMatcher, params);
        }
        throw new IllegalArgumentException("Unsupported statement: " + sql);
    }

    public synchronized List<Map<String, Object>> query(String sql, Object... params) {
        sql = sanitise(sql);
        Matcher matcher = SELECT.matcher(sql);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Unsupported statement: " + sql);
        }
        String columnPart = matcher.group(1).trim();
        String tableName = matcher.group(2).toLowerCase(Locale.ROOT);
        String wherePart = matcher.group(3);
        Table table = getRequiredTable(tableName);
        List<Condition> conditions = parseConditions(wherePart, params, new AtomicInteger(0));
        List<String> requestedColumns = parseRequestedColumns(columnPart, table);
        List<Map<String, Object>> results = new ArrayList<>();
        for (Map<String, Object> row : table.rows) {
            if (matches(row, conditions)) {
                Map<String, Object> resultRow = new LinkedHashMap<>();
                for (String column : requestedColumns) {
                    resultRow.put(column, row.get(column));
                }
                results.add(resultRow);
            }
        }
        return results;
    }

    private int handleInsert(Matcher matcher, Object[] params) {
        String tableName = matcher.group(1).toLowerCase(Locale.ROOT);
        String columnPart = matcher.group(2);
        String valuesPart = matcher.group(3);
        Table table = getRequiredTable(tableName);
        List<String> columns = splitCommaSeparated(columnPart);
        List<String> values = splitCommaSeparated(valuesPart);
        if (columns.size() != values.size()) {
            throw new IllegalArgumentException("Columns and values mismatch for INSERT");
        }
        Map<String, Object> row = new LinkedHashMap<>();
        AtomicInteger paramIndex = new AtomicInteger(0);
        for (int i = 0; i < columns.size(); i++) {
            String column = columns.get(i).trim().toLowerCase(Locale.ROOT);
            String valueExpression = values.get(i).trim();
            Object value = parseValue(valueExpression, params, paramIndex);
            row.put(column, value);
        }
        if (table.autoIncrementColumn != null) {
            Object idValue = row.get(table.autoIncrementColumn);
            if (idValue == null) {
                long nextValue = ++table.autoIncrementSeed;
                row.put(table.autoIncrementColumn, nextValue);
            } else if (idValue instanceof Number) {
                long provided = ((Number) idValue).longValue();
                table.autoIncrementSeed = Math.max(table.autoIncrementSeed, provided);
            }
        }
        // ensure all columns exist
        for (Column column : table.columns) {
            row.putIfAbsent(column.name, null);
        }
        table.rows.add(row);
        persist();
        return 1;
    }

    private int handleUpdate(Matcher matcher, Object[] params) {
        String tableName = matcher.group(1).toLowerCase(Locale.ROOT);
        Table table = getRequiredTable(tableName);
        List<String> assignments = splitCommaSeparated(matcher.group(2));
        AtomicInteger paramIndex = new AtomicInteger(0);
        List<ValueAssignment> updates = new ArrayList<>();
        for (String assignment : assignments) {
            String[] parts = assignment.split("=", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid assignment: " + assignment);
            }
            String column = parts[0].trim().toLowerCase(Locale.ROOT);
            Object value = parseValue(parts[1].trim(), params, paramIndex);
            updates.add(new ValueAssignment(column, value));
        }
        List<Condition> conditions = parseConditions(matcher.group(3), params, paramIndex);
        int affected = 0;
        for (Map<String, Object> row : table.rows) {
            if (matches(row, conditions)) {
                for (ValueAssignment assignment : updates) {
                    row.put(assignment.column, assignment.value);
                }
                affected++;
            }
        }
        if (affected > 0) {
            persist();
        }
        return affected;
    }

    private int handleDelete(Matcher matcher, Object[] params) {
        String tableName = matcher.group(1).toLowerCase(Locale.ROOT);
        Table table = getRequiredTable(tableName);
        List<Condition> conditions = parseConditions(matcher.group(2), params, new AtomicInteger(0));
        int originalSize = table.rows.size();
        table.rows.removeIf(row -> matches(row, conditions));
        int affected = originalSize - table.rows.size();
        if (affected > 0) {
            persist();
        }
        return affected;
    }

    private Table getRequiredTable(String tableName) {
        Table table = tables.get(tableName);
        if (table == null) {
            throw new IllegalArgumentException("Table not found: " + tableName);
        }
        return table;
    }

    private boolean matches(Map<String, Object> row, List<Condition> conditions) {
        for (Condition condition : conditions) {
            Object value = row.get(condition.column);
            if (!Objects.equals(value, condition.value)) {
                return false;
            }
        }
        return true;
    }

    private List<String> parseRequestedColumns(String columnPart, Table table) {
        if ("*".equals(columnPart.trim())) {
            List<String> columns = new ArrayList<>();
            for (Column column : table.columns) {
                columns.add(column.name);
            }
            return columns;
        }
        List<String> columns = new ArrayList<>();
        for (String column : splitCommaSeparated(columnPart)) {
            columns.add(column.trim().toLowerCase(Locale.ROOT));
        }
        return columns;
    }

    private List<Condition> parseConditions(String wherePart, Object[] params, AtomicInteger paramIndex) {
        if (wherePart == null || wherePart.isBlank()) {
            return Collections.emptyList();
        }
        List<String> rawConditions = Arrays.asList(wherePart.split("(?i)AND"));
        List<Condition> conditions = new ArrayList<>();
        for (String raw : rawConditions) {
            String[] pieces = raw.split("=", 2);
            if (pieces.length != 2) {
                throw new IllegalArgumentException("Unsupported WHERE clause: " + raw);
            }
            String column = pieces[0].trim().toLowerCase(Locale.ROOT);
            Object value = parseValue(pieces[1].trim(), params, paramIndex);
            conditions.add(new Condition(column, value));
        }
        return conditions;
    }

    private Object parseValue(String token, Object[] params, AtomicInteger paramIndex) {
        token = token.trim();
        if (token.equals("?")) {
            if (paramIndex.get() >= params.length) {
                throw new IllegalArgumentException("Insufficient parameters provided");
            }
            return params[paramIndex.getAndIncrement()];
        }
        if (token.startsWith("'") && token.endsWith("'")) {
            return token.substring(1, token.length() - 1);
        }
        if (token.equalsIgnoreCase("NULL")) {
            return null;
        }
        if (token.equalsIgnoreCase("TRUE") || token.equalsIgnoreCase("FALSE")) {
            return Boolean.parseBoolean(token.toLowerCase(Locale.ROOT));
        }
        try {
            if (token.contains(".")) {
                return Double.parseDouble(token);
            }
            return Long.parseLong(token);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Unsupported literal: " + token, ex);
        }
    }

    private Column parseColumnDefinition(String definition) {
        String[] parts = definition.split("\\s+");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid column definition: " + definition);
        }
        String columnName = parts[0].trim().toLowerCase(Locale.ROOT);
        String type = parts[1].trim().toUpperCase(Locale.ROOT);
        Column column = new Column(columnName, type);
        String upperDefinition = definition.toUpperCase(Locale.ROOT);
        if (upperDefinition.contains("PRIMARY KEY")) {
            column.primaryKey = true;
        }
        if (upperDefinition.contains("AUTOINCREMENT")) {
            column.autoIncrement = true;
        }
        if (upperDefinition.contains("NOT NULL")) {
            column.notNull = true;
        }
        if (upperDefinition.contains("UNIQUE")) {
            column.unique = true;
        }
        return column;
    }

    private List<String> splitCommaSeparated(String value) {
        List<String> result = new ArrayList<>();
        int depth = 0;
        StringBuilder current = new StringBuilder();
        for (char ch : value.toCharArray()) {
            if (ch == '(') {
                depth++;
            } else if (ch == ')') {
                depth--;
            }
            if (ch == ',' && depth == 0) {
                result.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        if (current.length() > 0) {
            result.add(current.toString());
        }
        return result;
    }

    private String sanitise(String sql) {
        return sql.trim().replaceAll(";+$", "");
    }

    private void persist() {
        try {
            Files.createDirectories(storageFile.getParent());
            try (ObjectOutputStream outputStream = new ObjectOutputStream(Files.newOutputStream(storageFile))) {
                outputStream.writeObject(tables);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to persist database", e);
        }
    }

    @SuppressWarnings("unchecked")
    private void load() {
        if (!Files.exists(storageFile)) {
            return;
        }
        try (ObjectInputStream inputStream = new ObjectInputStream(Files.newInputStream(storageFile))) {
            Object data = inputStream.readObject();
            if (data instanceof Map<?, ?> loaded) {
                tables = (Map<String, Table>) loaded;
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Failed to load database", e);
        }
    }

    private static final class Condition {
        private final String column;
        private final Object value;

        private Condition(String column, Object value) {
            this.column = column;
            this.value = value;
        }
    }

    private static final class ValueAssignment {
        private final String column;
        private final Object value;

        private ValueAssignment(String column, Object value) {
            this.column = column;
            this.value = value;
        }
    }

    private static final class Column implements Serializable {
        private static final long serialVersionUID = 1L;

        private final String name;
        private final String type;
        private boolean primaryKey;
        private boolean autoIncrement;
        private boolean notNull;
        private boolean unique;

        private Column(String name, String type) {
            this.name = name;
            this.type = type;
        }
    }

    private static final class Table implements Serializable {
        private static final long serialVersionUID = 1L;

        private final String name;
        private final List<Column> columns;
        private final List<Map<String, Object>> rows;
        private String autoIncrementColumn;
        private long autoIncrementSeed;

        private Table(String name) {
            this.name = name;
            this.columns = new ArrayList<>();
            this.rows = new ArrayList<>();
            this.autoIncrementColumn = null;
            this.autoIncrementSeed = 0L;
        }

        private Column getColumn(String name) {
            for (Column column : columns) {
                if (column.name.equalsIgnoreCase(name)) {
                    return column;
                }
            }
            return null;
        }
    }
}
