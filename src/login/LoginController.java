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

public class LoginController {

    @FXML private TextField usernameField;  // Aapka fx:id check kar lena
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    // Login Button dabane par yeh chalega
    @FXML
    public void handleLogin(ActionEvent event) {
        String user = usernameField.getText();
        String pass = passwordField.getText();

        // Check (filhal hardcode rakha hai check karne k liye)
        if (user.equals("admin") && pass.equals("123")) {

            try {
                // 1. Dashboard file load karna
                // NOTE: Path check karein. Agar fxml folder mein hai to "/fxml/dashboard.fxml"
                // Agar direct src mein hai to "dashboard.fxml"
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));
                Parent root = loader.load();

                // 2. Current window (Stage) ko pakarna
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

                // 3. Naya scene set karna (Dashboard)
                Scene scene = new Scene(root);
                stage.setScene(scene);
                stage.show();

            } catch (IOException e) {
                e.printStackTrace();
                errorLabel.setText("Error loading Dashboard file!");
            }

        } else {
            errorLabel.setText("Wrong Username or Password!");
        }
    }
}