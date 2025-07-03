package org.example;

import java.lang.reflect.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class OrmEngine<T> {
    private final T obj;
    private final Class<T> type;
    private final String tableName;
    private final Connection connection;


    public OrmEngine(T obj, Connection connection) {
        if (obj == null) {
            throw new IllegalArgumentException("Об'єкт не може бути null");
        }
        this.obj = obj;
        this.type = (Class<T>) obj.getClass();
        this.tableName = type.getSimpleName().replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
        this.connection = connection;

    }

    public OrmEngine(Class<T> type, Connection connection) {
        this.obj = null;
        this.type = type;
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
        sqlCreate.append("\"id\" SERIAL PRIMARY KEY, ");
        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            field.setAccessible(true);
            sqlCreate.append(buildSqlColumnFromField(field));
        }
        sqlCreate.setLength(sqlCreate.length() - 2);
        sqlCreate.append(");");
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sqlCreate.toString());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

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
            throw new UnsupportedOperationException("Непідтримуваний тип поля: " + field + " (" + type.getSimpleName() + ")");
        }
    }

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
        sqlInsert.setLength(sqlInsert.length() - 2);
        sqlValues.setLength(sqlValues.length() - 2);
        sqlInsert.append(") ").append(sqlValues).append(") RETURNING id;");
        try (PreparedStatement preparedStatement = connection.prepareStatement(sqlInsert.toString())) {
            for (int i = 0; i < values.size(); i++) {
                preparedStatement.setObject(i + 1, values.get(i));
            }
            try (ResultSet rs = preparedStatement.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("id");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public T read(Class<?> aClass, Long id) {
        String tableName = toSnakeCase(aClass.getSimpleName());
        String sql = "SELECT * FROM \"" + tableName + "\" WHERE id =?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setLong(1, id);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                T instance = (T) aClass.getDeclaredConstructor().newInstance();
                for (Field field : aClass.getDeclaredFields()) {
                    field.setAccessible(true);
                    Class<?> fieldType = field.getType();
                    String columnName = toSnakeCase(field.getName());
                    if (LocalDateTime.class.isAssignableFrom(fieldType)) {
                        Timestamp timestamp = resultSet.getTimestamp(columnName);
                        if (timestamp != null) {
                            field.set(instance, timestamp.toLocalDateTime());
                        } else {
                            field.set(instance, null);
                        }
                    } else {
                        Object value = resultSet.getObject(columnName);
                        if (value == null && isPrimitiveOrString(fieldType)) {
                            value = getDefaultValueForPrimitive(fieldType);
                            field.set(instance, value);
                        } else {
                            field.set(instance, value);
                        }
                    }
                }
                return instance;
            } else {
                throw new RuntimeException("Обʼєкт з id = " + id + " не знайдено в таблиці " + tableName);
            }
        } catch (SQLException | NoSuchMethodException |
                 InvocationTargetException | InstantiationException |
                 IllegalAccessException e) {
            throw new RuntimeException("Помилка при читанні в методі read з таблиці " + tableName, e);
        }
    }

    public Object getDefaultValueForPrimitive(Class<?> type) {
        if (type.equals(boolean.class)) {
            return false;
        } else if (type.equals(int.class)) {
            return 0;
        } else if (type.equals(long.class)) {
            return 0L;
        } else if (type.equals(float.class)) {
            return 0f;
        } else if (type.equals(double.class)) {
            return 0d;
        } else if (type.equals(char.class)) {
            return '\u0000';  // нульовий символ
        } else if (type.equals(String.class)) {
            return null;
        }
        return null;
    }
}
