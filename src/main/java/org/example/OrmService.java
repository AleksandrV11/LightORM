package org.example;

import io.github.cdimascio.dotenv.Dotenv;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class OrmService {
    private static final Dotenv dotenv = Dotenv.load();

    private static final String URL = dotenv.get("DB_URL");
    private static final String USER = dotenv.get("DB_USER");
    private static final String PASSWORD = dotenv.get("DB_PASSWORD");


    public void create(Object object) {
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD)) {
            connection.createStatement().execute("SET search_path TO orm");
            OrmEngine<?> ormEngine = new OrmEngine<>(object, connection);
            ormEngine.createTable(object.getClass());
            Long id = ormEngine.insertObject(object);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void delete(Object object, Long idObject) {
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD)) {
            connection.createStatement().execute("SET search_path TO orm");
            OrmEngine<?> ormEngine = new OrmEngine<>(object, connection);
            ormEngine.deleteObj(object, idObject);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        System.out.println(" ВИДАЛИВ ");
    }

    public void update(Object object, Long idObject) {
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD)) {
            connection.createStatement().executeUpdate("SET search_path TO orm");
            OrmEngine<?> ormEngine = new OrmEngine<>(object, connection);
            ormEngine.updateObject(object,idObject);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        System.out.println(" ОБНОВИВ ");
    }

    public <T> T read(Class<?> clazz, Long id) {
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD)) {
            connection.createStatement().execute("SET search_path TO orm");
            OrmEngine<?> daoRead = new OrmEngine<>(clazz, connection);
            T obj = (T) daoRead.read(clazz, id);
            return obj;
        } catch (SQLException e) {
            System.err.println("База даних недоступна: " + e.getMessage());
            throw new RuntimeException(e);
        } catch (Exception e) {
            System.err.println("Інша помилка: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
