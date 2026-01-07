package login;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginController {

    // --- INITIALIZE METHOD (Runs when screen loads) ---
    @FXML
    public void initialize() {
        // 1. Enter Key Logic for Username Field
        // Jab user Username likh kar Enter dabaye, to focus Password par chala jaye
        usernameField.setOnAction(event -> {
            passwordField.requestFocus();
        });

        // Note: Password field par Enter dabane se automatically Login button click hoga
        // kyunke humne FXML mein defaultButton="true" set kiya tha.
    }



    // FXML Elements (Inka naam FXML file se match hona chahiye)
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;

    // --- LOGIN BUTTON ACTION ---
    @FXML
    public void handleLogin(ActionEvent event) {
        String userInput = usernameField.getText();
        String passInput = passwordField.getText();

        // 1. Validation: Agar fields khali hain to rook do
        if (userInput.isEmpty() || passInput.isEmpty()) {
            errorLabel.setText("Please enter Username and Password.");
            return;
        }

        // 2. Database Connection
        Connection conn = DatabaseHandler.getDBConnection();

        // Agar connection nahi bana (Shayad XAMPP off hai)
        if (conn == null) {
            errorLabel.setText("Database Connection Failed! Check XAMPP.");
            return;
        }

        try {
            // 3. SQL Query: Check karo ke user database mein hai ya nahi
            String query = "SELECT * FROM admin_users WHERE username = ? AND password = ?";

            PreparedStatement pst = conn.prepareStatement(query);
            pst.setString(1, userInput);
            pst.setString(2, passInput);

            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                // ✅ Login Successful
                System.out.println("✅ Credentials Verified! Opening Dashboard...");

                // Dashboard kholne wala function call karo
                openDashboard(event);

            } else {
                // ❌ Wrong Password
                errorLabel.setText("Invalid Username or Password!");
                System.out.println("❌ Login Failed: Wrong credentials.");
            }

            // Resources close karo taake memory leak na ho
            pst.close();
            conn.close();

        } catch (SQLException e) {
            e.printStackTrace();
            errorLabel.setText("Database Error.");
        }
    }

    // --- DASHBOARD OPEN KARNE KA METHOD ---
    private void openDashboard(ActionEvent event) {
        try {
            // 1. Dashboard FXML Load karo
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/dashboard.fxml"));
            Parent root = loader.load();

            // 2. Current Window (Stage) hasil karo
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            // 3. Scene Setup karo
            Scene scene = new Scene(root);

            // CSS Link karna
            String cssPath = "/css/style.css";
            if (getClass().getResource(cssPath) != null) {
                scene.getStylesheets().add(getClass().getResource(cssPath).toExternalForm());
            } else {
                System.out.println("⚠️ Warning: CSS file nahi mili.");
            }

            stage.setScene(scene);
            stage.setTitle("Admin Dashboard");
            stage.centerOnScreen(); // Window ko screen ke beech mein lao
            stage.show();

        } catch (IOException e) {
            System.out.println("❌ Error loading Dashboard FXML!");
            e.printStackTrace();
            errorLabel.setText("System Error: Dashboard file missing.");
        }
    }
}