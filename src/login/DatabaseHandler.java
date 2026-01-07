package login;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseHandler {

    // Database ki settings
    private static final String DB_URL = "jdbc:mysql://localhost:3306/fras_db";
    private static final String USER = "root";
    private static final String PASS = "";

    // Yeh method connection wapis karega
    public static Connection getDBConnection() {
        Connection connection = null;
        try {
            // 1. Driver Load karna
            Class.forName("com.mysql.cj.jdbc.Driver");

            // 2. Connection Banana
            connection = DriverManager.getConnection(DB_URL, USER, PASS);

        } catch (ClassNotFoundException e) {
            System.out.println("❌ Error: MySQL Driver nahi mila! (Library add karein)");
            e.printStackTrace();
        } catch (SQLException e) {
            System.out.println("❌ Error: Database connect nahi hua! (XAMPP check karein)");
            e.printStackTrace();
        }
        return connection;
    }
}