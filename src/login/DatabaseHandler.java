package login;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseHandler {

    // Database settings
    private static final String DB_URL = "jdbc:mysql://localhost:3306/fras_db";
    private static final String USER = "root";
    private static final String PASS = "root";

    // This method will return the connection
    public static Connection getDBConnection() {
        Connection connection = null;
        try {
            // 1. Load Driver
            Class.forName("com.mysql.cj.jdbc.Driver");

            // 2. Create Connection
            connection = DriverManager.getConnection(DB_URL, USER, PASS);

        } catch (ClassNotFoundException e) {
            System.out.println(" Error: MySQL Driver not found! (Add the library)");
            e.printStackTrace();
        } catch (SQLException e) {
            System.out.println(" Error: Database did not connect! (Check XAMPP)");
            e.printStackTrace();
        }
        return connection;
    }
}