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
            System.out.println("Ось таке id : " + id);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        System.out.println(" СТВОРИВ ");
    }

//    public void delete(Object object) {
//        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD)) {
//            connection.createStatement().execute("SET search_path TO orm");
////            GenDaoDelete<?> daoDelete = new GenDaoDelete<>(object, connection);
////            daoDelete.delete();
//        } catch (SQLException e) {
//            throw new RuntimeException(e);
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//        System.out.println(" ВИДАЛИВ ");
//    }

//    public void update(Object object) {
//        delete(object);
//        create(object);
//        System.out.println(" ОБНОВИВ ");
//    }

//    public <T> T read(Class<T> clazz, Long id) {
//        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD)) {
//            connection.createStatement().execute("SET search_path TO orm");
////            GenDaoRead<T> daoRead = new GenDaoRead<>(clazz, id, connection,true);
////            T obj = daoRead.read();
////            System.out.println(" ПРИЧИТАВ " + obj);
////            return obj;
//        } catch (SQLException e) {
//            System.err.println("База даних недоступна: " + e.getMessage());
//            throw new RuntimeException(e);
//        } catch (Exception e) {
//            System.err.println("Інша помилка: " + e.getMessage());
//            throw new RuntimeException(e);
//        }
//        return null;   //тимчасово
//    }
}
