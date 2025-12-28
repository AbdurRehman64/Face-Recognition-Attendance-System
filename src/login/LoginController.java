package login;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private javafx.scene.control.Button loginButton; // Button ka reference

    @FXML
    public void handleLogin(ActionEvent event) {
        String user = usernameField.getText();
        String pass = passwordField.getText();

        System.out.println("👉 Login Button Clicked!"); // Console check karein

        if (user.equals("admin") && pass.equals("123")) {
            System.out.println("✅ Credentials Correct. Loading Dashboard...");
            try {
                // 1. Path Check Karein
                URL url = getClass().getResource("/dashboard.fxml");
                System.out.println("🔍 Checking path for dashboard.fxml: " + url);

                if (url == null) {
                    System.out.println("❌ ERROR: File 'dashboard.fxml' nahi mili!");
                    System.out.println("👉 Solution: Project Rebuild karein ya check karein ke file 'resources' folder mein hai.");
                    errorLabel.setText("System Error: Dashboard file missing.");
                    return;
                }

                // 2. Loader Setup
                FXMLLoader loader = new FXMLLoader(url);
                Parent root = loader.load(); // Yahan error aa sakta hai agar Controller ghalat ho

                // 3. Stage & Scene
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                Scene scene = new Scene(root);

                // CSS Link
                URL cssUrl = getClass().getResource("/css/style.css");
                if (cssUrl != null) {
                    scene.getStylesheets().add(cssUrl.toExternalForm());
                } else {
                    System.out.println("⚠️ Warning: CSS file nahi mili.");
                }

                stage.setScene(scene);
                stage.setTitle("Admin Dashboard");
                stage.show();
                System.out.println("🚀 Dashboard Opened Successfully!");

            } catch (IOException e) {
                System.out.println("❌ ERROR: Dashboard load karte waqt crash ho gaya!");
                e.printStackTrace(); // Yeh poora error console mein dikhayega
                errorLabel.setText("Error loading Dashboard.");
            }
        } else {
            System.out.println("❌ Wrong Password entered.");
            errorLabel.setText("Invalid Username or Password!");
        }
    }
}