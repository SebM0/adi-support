package com.axway.adi.tools.disturb;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import com.axway.adi.tools.disturb.db.DbBind;
import com.axway.adi.tools.disturb.db.DbObject;

public class DiagnosticPersistence {
    public static DiagnosticPersistence DB = null;
    public static final int VERSION = 1;

    private Connection connection;

    public void connect() throws SQLException {
        //Load the driver class
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException ex) {
            System.out.println("Unable to load the class. Terminating the program");
            System.exit(-1);
        }
        //get the connection
        connection = DriverManager.getConnection("jdbc:postgresql://qa08.adi.axway.int:5432/support", "support", "Tornado01");
    }

    public <K extends DbObject> List<K> select(Class<K> clazz) {
        List<K> list = new ArrayList<>();
        DbBind dbBind = clazz.getDeclaredAnnotation(DbBind.class);
        if (dbBind == null) {
            System.out.println("Error getting binding from: " + clazz.getSimpleName());
            System.exit(-1);
        }
        String tableName = dbBind.value();
        String query = "SELECT * FROM \"" + tableName + "\"";
        try {
            // Has a PK ?
            Optional<String> pk = Arrays.stream(clazz.getDeclaredFields()).filter(f -> {
                DbBind fieldBind = f.getDeclaredAnnotation(DbBind.class);
                return fieldBind != null && fieldBind.primary();
            }).map(Field::getName).findFirst();
            if (pk.isPresent()) {
                query += " ORDER BY " + pk.get();
            }
            // Run query
            ResultSet resultSet = executeQuery(query);
            //Get Number of columns
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnsNumber = metaData.getColumnCount();
            TreeMap<Integer,Field> bindings = new TreeMap<>();
            for (int i = 1; i <= columnsNumber; i++) {
                String col = metaData.getColumnName(i);
                try {
                    Field field = clazz.getDeclaredField(col);
                    bindings.put(i, field);
                } catch (NoSuchFieldException e) {
                    //skip
                }
            }
            while (resultSet.next()) {
                K dto = clazz.getConstructor().newInstance();
                list.add(dto);
                for (Map.Entry<Integer, Field> entry : bindings.entrySet()) {
                    int col = entry.getKey();
                    Object object = resultSet.getObject(col);
                    if (object != null) {
                        Field field = entry.getValue();
                        field.set(dto, object);
                    }
                }
            }
        } catch (SQLException ex) {
            System.out.println("Exception while executing statement. Terminating program... " + ex.getMessage());
        } catch (Exception ex) {
            System.out.println("General exception while executing query. Terminating the program..." + ex.getMessage());
        }

        return list;
    }

    public <K extends DbObject> void insert(K item) {
        insert(item, true);
    }

    public <K extends DbObject> void delete(K item) {
        try {
            DbAnalysis dbAnalysis = DbAnalysis.analyze(item);
            String deleteQuery = "DELETE FROM \"" + dbAnalysis.tableName + "\" WHERE " + dbAnalysis.primaryField + " = " + dbAnalysis.valueList.getFirst();
            executeUpdate(deleteQuery);
        } catch (SQLException ex) {
            System.out.println("Exception while executing statement. Terminating program... " + ex.getMessage());
        } catch (Exception ex) {
            System.out.println("General exception while executing query. Terminating the program..." + ex.getMessage());
        }
    }

    public <K extends DbObject> void insert(Collection<K> items) {
        boolean first = true;
        for (K item : items) {
            insert(item, first);
            first = false;
        }
    }

    private static class DbAnalysis {
        String tableName;
        LinkedList<String> fieldList = new LinkedList<>();
        LinkedList<String> valueList = new LinkedList<>();
        String primaryField = null;
        String foreignClause = null;

        static <K extends DbObject> DbAnalysis analyze(K item) throws IllegalAccessException {
            DbAnalysis dbAnalysis = new DbAnalysis();
            Class<? extends DbObject> clazz = item.getClass();
            DbBind dbBind = clazz.getDeclaredAnnotation(DbBind.class);
            if (dbBind == null) {
                System.out.println("Error getting binding from: " + item.getClass().getSimpleName());
                System.exit(-1);
            }
            dbAnalysis.tableName = dbBind.value();
            for (Field field : clazz.getDeclaredFields()) {
                if (!field.canAccess(item))
                    continue;
                Object value = field.get(item);
                if (value == null)
                    continue;
                DbBind fieldBind = field.getDeclaredAnnotation(DbBind.class);
                boolean isPrimary = fieldBind != null && fieldBind.primary();
                if (isPrimary) {
                    dbAnalysis.primaryField = field.getName();
                    dbAnalysis.fieldList.addFirst(field.getName());
                } else {
                    dbAnalysis.fieldList.addLast(field.getName());
                }
                boolean isForeign = fieldBind != null && fieldBind.foreign();
                boolean isString = field.getType().isAssignableFrom(String.class);
                String valueStr = isString ? "'" + value.toString() + "'" : value.toString();
                if (isPrimary) {
                    dbAnalysis.valueList.addFirst(valueStr);
                } else {
                    dbAnalysis.valueList.addLast(valueStr);
                }
                if (isForeign) {
                    dbAnalysis.foreignClause = field.getName() + " = " + valueStr;
                }
            }
            return dbAnalysis;
        }
    }

    private <K extends DbObject> void insert(K item, boolean deleteIfNeeded) {
        try {
            DbAnalysis dbAnalysis = DbAnalysis.analyze(item);
            StringBuilder query = new StringBuilder("INSERT INTO \"" + dbAnalysis.tableName + "\"(");
            query.append(String.join(", ", dbAnalysis.fieldList));
            query.append(") VALUES (");
            query.append(String.join(", ", dbAnalysis.valueList));
            query.append(")");
            if (dbAnalysis.primaryField != null) {
                query.append(" ON CONFLICT (");
                query.append(dbAnalysis.primaryField);
                query.append(") DO UPDATE SET ");
                String[] updates = dbAnalysis.fieldList.stream().skip(1).map(field -> field + " = EXCLUDED." + field).toArray(String[]::new);
                query.append(String.join(", ", updates));
            } else if (deleteIfNeeded && dbAnalysis.foreignClause != null) {
                String deleteQuery = "DELETE FROM \"" + dbAnalysis.tableName + "\" WHERE " + dbAnalysis.foreignClause;
                executeUpdate(deleteQuery);
            }
            executeUpdate(query.toString());
        } catch (SQLException ex) {
            System.out.println("Exception while executing statement. Terminating program... " + ex.getMessage());
        } catch (Exception ex) {
            System.out.println("General exception while executing query. Terminating the program..." + ex.getMessage());
        }
    }

    public ResultSet executeQuery(String query) throws SQLException {
        //executing query
        Statement stmt = connection.createStatement();
        return stmt.executeQuery(query);
    }

    public int executeUpdate(String query) throws SQLException {
        //executing query
        Statement stmt = connection.createStatement();
        int updates = stmt.executeUpdate(query);
        if (updates > 0 && !connection.getAutoCommit()) {
            connection.commit();
        }
        return updates;
    }
}
