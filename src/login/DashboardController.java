package login;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DashboardController {

    // --- FXML ELEMENTS ---
    @FXML private ImageView cameraView;
    @FXML private Button captureBtn;
    @FXML private Button saveBtn;

    // Input Fields
    @FXML private TextField nameField;
    @FXML private TextField rollNoField;
    @FXML private TextField depField;

    // OpenCV Variables
    // ⚠️ FIXED: Yahan 'new VideoCapture()' nahi likhna
    private VideoCapture capture;
    private ScheduledExecutorService timer;
    private boolean cameraActive = false;

    // --- INITIALIZE ---
    @FXML
    public void initialize() {
        // 1. Pehle Library Load karo
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        // 2. Ab Camera Object banao (Library load hone ke baad)
        this.capture = new VideoCapture();

        // 3. Camera Start karo
        startCamera();

        // Actions
        saveBtn.setOnAction(this::handleSaveStudent);
        captureBtn.setOnAction(this::handleCaptureFace);
    }

    // --- CAMERA LOGIC ---
    private void startCamera() {
        if (!cameraActive) {
            capture.open(0); // 0 = Default Camera
            if (capture.isOpened()) {
                cameraActive = true;
                Runnable frameGrabber = () -> {
                    Mat frame = new Mat();
                    if (capture.read(frame)) {
                        Image imageToShow = mat2Image(frame);
                        Platform.runLater(() -> cameraView.setImage(imageToShow));
                    }
                };
                timer = Executors.newSingleThreadScheduledExecutor();
                timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);
            } else {
                System.out.println("❌ Error: Camera open nahi ho raha!");
            }
        }
    }

    private Image mat2Image(Mat frame) {
        try {
            MatOfByte buffer = new MatOfByte();
            Imgcodecs.imencode(".png", frame, buffer);
            return new Image(new ByteArrayInputStream(buffer.toArray()));
        } catch (Exception e) {
            System.err.println("Cannot convert the Mat object: " + e);
            return null;
        }
    }

    // --- BUTTON ACTIONS ---

    private void handleCaptureFace(ActionEvent event) {
        System.out.println("📸 Capture Button Clicked!");
        // Future code: Save image logic here
    }

    private void handleSaveStudent(ActionEvent event) {
        String name = nameField.getText();
        String roll = rollNoField.getText();
        String dept = depField.getText();

        if (name.isEmpty() || roll.isEmpty() || dept.isEmpty()) {
            System.out.println("⚠️ Please fill all fields!");
            return;
        }

        saveToDatabase(name, roll, dept);
    }

    private void saveToDatabase(String name, String roll, String dept) {
        String query = "INSERT INTO students (full_name, roll_number, department) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseHandler.getDBConnection();
             PreparedStatement pst = conn.prepareStatement(query)) {

            pst.setString(1, name);
            pst.setString(2, roll);
            pst.setString(3, dept);

            int result = pst.executeUpdate();
            if (result > 0) {
                System.out.println("✅ Student Saved: " + name);
                nameField.clear();
                rollNoField.clear();
                depField.clear();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("❌ Error Saving Data");
        }
    }

    // --- NAVIGATION METHODS ---

    @FXML
    private void goToAttendance(ActionEvent event) {
        stopCamera();
        loadScreen(event, "/AttendanceView.fxml", "Live Attendance Monitor");
    }

    @FXML
    private void goToReports(ActionEvent event) {
        System.out.println("Navigating to Reports...");
    }

    @FXML
    private void handleTrainBtn(ActionEvent event) {
        System.out.println("Training Data...");
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        stopCamera();
        loadScreen(event, "/login.fxml", "Admin Login");
    }

    private void stopCamera() {
        if (cameraActive) {
            cameraActive = false;
            try {
                if (timer != null && !timer.isShutdown()) {
                    timer.shutdown();
                    timer.awaitTermination(33, TimeUnit.MILLISECONDS);
                }
            } catch (InterruptedException e) { e.printStackTrace(); }

            if (capture != null && capture.isOpened()) {
                capture.release();
            }
        }
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
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}