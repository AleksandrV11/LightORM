package org.example;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class OrmEngine<T> {
    private final T obj;
    private final Class<T> type;
    private final String tableName;
    private final Connection connection;
    private Map<Long, Object> objectId;

    public OrmEngine(T obj, Connection connection) {
        if (obj == null) {
            throw new IllegalArgumentException("Об'єкт не може бути null");
        }
        this.obj = obj;
        this.type = (Class<T>) obj.getClass();
        this.tableName = type.getSimpleName().replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
        this.connection = connection;

    }

    // Створення таблиці для заданого об'єкта
    public void createTable(Class<?> obj) throws IllegalAccessException {
        if (obj == null) {
            throw new IllegalArgumentException("Переданий об'єкт при створені табліці не може бути null");
        }
        String name = toSnakeCase(obj.getSimpleName());
        StringBuilder sqlCreate = new StringBuilder("CREATE TABLE IF NOT EXISTS \"")
                .append(toSnakeCase(name)).append("\" (");
        Field[] fields = obj.getDeclaredFields();
        // Проходимо через всі поля і генеруємо відповідні SQL запити
        sqlCreate.append("\"id\" SERIAL PRIMARY KEY, ");
        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers())) continue; // Пропускаємо статичні поля
            field.setAccessible(true);
            sqlCreate.append(buildSqlColumnFromField(field));
        }
        // Завершуємо створення запиту для таблиці
        sqlCreate.setLength(sqlCreate.length() - 2);
        sqlCreate.append(");");
        System.out.println(sqlCreate);
        // Виконання запитів
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sqlCreate.toString()); // Створення таблиці
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    //пошук примитивов та строк
    public boolean isPrimitiveOrString(Class<?> fieldType) {
        if (fieldType.isPrimitive() || fieldType.equals(String.class)
                || fieldType.equals(Long.class) || fieldType.equals(Integer.class)
                || fieldType.equals(Double.class) || fieldType.equals(Float.class)
                || fieldType.equals(Boolean.class) || fieldType.equals(Byte.class)
                || fieldType.equals(Short.class) || fieldType.equals(Character.class)) {
            return true;
        }
        return false;
    }

    //public String getSQLTypeForClass(Class<?> cls) {
//    if (cls == String.class) return "TEXT";
//    if (cls == Integer.class || cls == int.class) return "INT";
//    if (cls == Long.class || cls == long.class) return "BIGINT";
//    if (cls == Double.class || cls == double.class) return "DOUBLE PRECISION";
//    if (cls == Float.class || cls == float.class) return "REAL";
//    if (cls == Boolean.class || cls == boolean.class) return "BOOLEAN";
//    if (cls == Date.class || cls == java.sql.Date.class) return "TIMESTAMP";
//    // ... додавай типи за потреби
//    return "TEXT"; // тип за замовчуванням
//}
    public StringBuilder buildSqlColumnFromField(Field field) {
        try {
            StringBuilder sqlString = new StringBuilder();
            String fieldName = toSnakeCase(field.getName());
            if (field.getType().equals(Long.class) || field.getType().equals(long.class)) {
                sqlString.append("\"").append(fieldName).append("\" BIGINT, ");
            } else if (field.getType().equals(String.class)) {
                sqlString.append("\"").append(fieldName).append("\" VARCHAR(255), ");
            } else if (field.getType().equals(Integer.class) || field.getType().equals(int.class)) {
                sqlString.append("\"").append(fieldName).append("\" INT, ");
            } else if (field.getType().equals(Boolean.class) || field.getType().equals(boolean.class)) {
                sqlString.append("\"").append(fieldName).append("\" BOOLEAN, ");
            } else if (field.getType().equals(Double.class) || field.getType().equals(double.class)) {
                sqlString.append("\"").append(fieldName).append("\" DOUBLE PRECISION, ");
            } else if (field.getType().equals(Float.class) || field.getType().equals(float.class)) {
                sqlString.append("\"").append(fieldName).append("\" REAL, ");
            } else if (LocalDateTime.class.isAssignableFrom(field.getType())) {
                sqlString.append("\"").append(fieldName).append("\" TIMESTAMP, ");
            }
            return sqlString;
        } catch (Exception e) {
            // Логування або виняток, замість псування SQL
            throw new UnsupportedOperationException("Непідтримуваний тип поля: " + field + " (" + type.getSimpleName() + ")");
        }
    }

    // Перетворення в snake_case для назв полів та таблиць
    public static String toSnakeCase(String camelCase) {
        StringBuilder snakeCase = new StringBuilder();
        for (char c : camelCase.toCharArray()) {
            if (Character.isUpperCase(c)) {
                if (snakeCase.length() > 0) snakeCase.append('_');
                snakeCase.append(Character.toLowerCase(c));
            } else {
                snakeCase.append(c);
            }
        }
        return snakeCase.toString();
    }

    public Long insertObject(Object obj) throws Exception {
        if (obj == null) {
            throw new IllegalArgumentException("Переданий об'єкт при заповнені не може бути null");
        }
        Class<?> aClass = obj.getClass();
        String tableName = toSnakeCase(aClass.getSimpleName());
        StringBuilder sqlInsert = new StringBuilder("INSERT INTO \"")
                .append(toSnakeCase(tableName)).append("\" (");
        StringBuilder sqlValues = new StringBuilder("VALUES (");
        List<Object> values = new ArrayList<>();
        Field[] fields = aClass.getDeclaredFields();
        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers())) continue; // Пропускаємо статичні поля
            field.setAccessible(true);
            String camelCaseName = toSnakeCase(field.getName());
            sqlInsert.append("\"" + camelCaseName + "\", ");
            sqlValues.append("?, ");
            values.add(field.get(obj));
        }
        // Завершуємо SQL запит для вставки
        sqlInsert.setLength(sqlInsert.length() - 2);
        sqlValues.setLength(sqlValues.length() - 2);
        sqlInsert.append(") ").append(sqlValues).append(") RETURNING id;");
        System.out.println(sqlInsert);
        System.out.println(values);
        try (PreparedStatement preparedStatement = connection.prepareStatement(sqlInsert.toString())) {
            for (int i = 0; i < values.size(); i++) {
                preparedStatement.setObject(i + 1, values.get(i)); //запис
            }
            try (ResultSet rs = preparedStatement.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("id"); // Повертаємо згенерований id
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
