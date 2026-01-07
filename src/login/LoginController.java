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

    @FXML
    public void initialize() {

        usernameField.setOnAction(event -> {
            passwordField.requestFocus();
        });

    }

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;

    // --- LOGIN BUTTON ACTION ---
    @FXML
    public void handleLogin(ActionEvent event) {
        String userInput = usernameField.getText();
        String passInput = passwordField.getText();


        if (userInput.isEmpty() || passInput.isEmpty()) {
            errorLabel.setText("Please enter Username and Password.");
            return;
        }

        // 2. Database Connection
        Connection conn = DatabaseHandler.getDBConnection();

        if (conn == null) {
            errorLabel.setText("Database Connection Failed! .");
            return;
        }

        try {
            // 3. SQL Query: Check user in db
            String query = "SELECT * FROM admin_users WHERE username = ? AND password = ?";

            PreparedStatement pst = conn.prepareStatement(query);
            pst.setString(1, userInput);
            pst.setString(2, passInput);

            ResultSet rs = pst.executeQuery();

            if (rs.next()) {

                System.out.println("Credentials Verified! Opening Dashboard...");

                openDashboard(event);

            } else {
                errorLabel.setText("Invalid Username or Password!");
                System.out.println("Login Failed: Wrong credentials.");
            }


            pst.close();
            conn.close();

        } catch (SQLException e) {
            e.printStackTrace();
            errorLabel.setText("Database Error.");
        }
    }

    private void openDashboard(ActionEvent event) {
        try {
            // 1. Dashboard FXML Load karo
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/dashboard.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            Scene scene = new Scene(root);

            String cssPath = "/css/style.css";
            if (getClass().getResource(cssPath) != null) {
                scene.getStylesheets().add(getClass().getResource(cssPath).toExternalForm());
            } else {
                System.out.println("Css file not present");
            }

            stage.setScene(scene);
            stage.setTitle("Admin Dashboard");
            stage.centerOnScreen(); // Window ko screen ke beech mein lao
            stage.show();

        } catch (IOException e) {
            System.out.println("Error loading Dashboard FXML!");
            e.printStackTrace();
            errorLabel.setText("System Error: Dashboard file missing.");
        }
    }
}