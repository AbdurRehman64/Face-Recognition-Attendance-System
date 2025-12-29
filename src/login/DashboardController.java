package login;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class DashboardController {

    @FXML
    public void handleReportsBtn(ActionEvent event) {
        loadScreen(event, "/dashboard.fxml", "Admin Dashboard");
    }

    @FXML
    public void handleTrainBtn(ActionEvent event) {
        loadScreen(event, "/TrainDataView.fxml", "Train Data");
    }



    @FXML
    private void goToReports(ActionEvent event) {
        loadScreen(event, "/ReportsView.fxml", "Attendance Reports");
    }

   /* @FXML
    private void goToTrain(ActionEvent event) {
        loadScreen(event, "/TrainDataView.fxml", "Train Data");
    }*/

    // 1. Attendance Screen par jane ke liye
    @FXML
    private void goToAttendance(ActionEvent event) {
        // Path corrected: "/AttendanceView.fxml" (Seedha resources folder mein)
        loadScreen(event, "/AttendanceView.fxml", "Live Attendance Monitor");
    }

    // 2. Wapas Login screen par jane ke liye (Logout)
    @FXML
    private void handleLogout(ActionEvent event) {
        // Path corrected: "/login.fxml"
        loadScreen(event, "/login.fxml", "Admin Login");
    }

    // --- Helper Method ---
    private void loadScreen(ActionEvent event, String fxmlPath, String title) {
        try {
            // 1. Loader setup
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            // 2. Current Stage (Window) get karna
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            // 3. Scene set karna
            Scene scene = new Scene(root);

            // 4. CSS Link karna (Path check karein: /css/style.css)
            String css = this.getClass().getResource("/css/style.css").toExternalForm();
            scene.getStylesheets().add(css);

            stage.setScene(scene);
            stage.setTitle(title);
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("❌ Error loading screen: " + fxmlPath);
            System.out.println("👉 Make sure file '" + fxmlPath + "' exists in 'resources' folder.");
        } catch (NullPointerException e) {
            e.printStackTrace();
            System.out.println("❌ Path Error: File shayad galat jagah par hai ya naam galat hai.");
        }
    }
}