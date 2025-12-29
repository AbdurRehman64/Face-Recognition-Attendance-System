package login;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import java.io.IOException;

public class TrainDataController {

    @FXML private Button trainBtn;
    @FXML private ProgressBar progressBar;
    @FXML private TextArea logArea;

    @FXML
    public void initialize() {
        trainBtn.setOnAction(e -> startTraining());
    }

    private void startTraining() {
        logArea.appendText("\nStarting training process...");
        progressBar.setVisible(true);
        progressBar.setProgress(-1); // Indeterminate loading
        // Logic to call your Python script or Java training code will go here
    }

    // 1. Attendance Screen par jane ke liye
    @FXML
    private void goToAttendance(ActionEvent event) {
        // Path corrected: "/AttendanceView.fxml" (Seedha resources folder mein)
        loadScreen(event, "/AttendanceView.fxml", "Live Attendance Monitor");
    }
    @FXML
    private void handleDashboardBtn(ActionEvent event) {
        loadScreen(event, "/dashboard.fxml", "Admin Dashboard");
    }

    @FXML
    private void handleReportsBtn(ActionEvent event) {
        loadScreen(event, "/ReportsView.fxml", "Attendance Reports");
    }

    private void loadScreen(ActionEvent event, String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            String css = this.getClass().getResource("/css/style.css").toExternalForm();
            scene.getStylesheets().add(css);
            stage.setScene(scene);
            stage.setTitle(title);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}