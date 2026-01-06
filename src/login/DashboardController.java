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

// OpenCV Imports
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;

// Java IO & SQL Imports
import java.io.ByteArrayInputStream;
import java.io.File;
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
    private VideoCapture capture;
    private ScheduledExecutorService timer;
    private boolean cameraActive = false;

    // Face Detection Variables
    private CascadeClassifier faceDetector;
    private MatOfRect faceDetections;

    // ⭐ NEW: Photo ka Path save karne ke liye variable
    private String currentImagePath = null;

    // --- INITIALIZE ---
    @FXML
    public void initialize() {
        // 1. Load Native Library
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        // 2. Load Haar Cascade
        this.faceDetector = new CascadeClassifier();
        this.faceDetector.load("haarcascade_frontalface_alt.xml");
        this.faceDetections = new MatOfRect();

        // 3. Initialize Camera
        this.capture = new VideoCapture();

        // 4. Start Camera
        startCamera();

        // Actions
        saveBtn.setOnAction(this::handleSaveStudent);
        captureBtn.setOnAction(this::handleCaptureFace);
    }

    // --- CAMERA & DETECTION LOGIC ---
    private void startCamera() {
        if (!cameraActive) {
            capture.open(0);
            if (capture.isOpened()) {
                cameraActive = true;

                Runnable frameGrabber = () -> {
                    Mat frame = new Mat();
                    if (capture.read(frame)) {

                        // Face Detection Logic
                        Mat grayFrame = new Mat();
                        Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);
                        Imgproc.equalizeHist(grayFrame, grayFrame);

                        this.faceDetector.detectMultiScale(grayFrame, this.faceDetections);

                        for (Rect rect : this.faceDetections.toArray()) {
                            Imgproc.rectangle(frame,
                                    new Point(rect.x, rect.y),
                                    new Point(rect.x + rect.width, rect.y + rect.height),
                                    new Scalar(0, 255, 0), 2);
                        }

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

    // --- CAPTURE BUTTON ACTION (Photo Save Logic) ---
    private void handleCaptureFace(ActionEvent event) {
        String rollNo = rollNoField.getText().trim();

        // Validation: Roll number zaroori hai filename ke liye
        if (rollNo.isEmpty()) {
            System.out.println("⚠️ Error: Pehle Roll Number likhein!");
            return;
        }

        // Check karo face detect hua ya nahi
        if (this.faceDetections.toArray().length > 0) {
            Mat frame = new Mat();
            if (capture.read(frame)) {

                // Pehla chehra crop karo
                Rect rect = this.faceDetections.toArray()[0];
                Mat faceOnly = new Mat(frame, rect);
                Imgproc.cvtColor(faceOnly, faceOnly, Imgproc.COLOR_BGR2GRAY);

                // Folder aur File setup
                File directory = new File("saved_faces");
                if (!directory.exists()) {
                    directory.mkdirs();
                }

                String finalFileName = rollNo + "_" + System.currentTimeMillis() + ".jpg";
                File fileToSave = new File(directory, finalFileName);

                // Image Save karo
                boolean saved = Imgcodecs.imwrite(fileToSave.getAbsolutePath(), faceOnly);

                if (saved) {
                    // ⭐ Path ko variable mein store kar liya
                    this.currentImagePath = fileToSave.getAbsolutePath();

                    System.out.println("✅ Photo Saved! Path: " + this.currentImagePath);
                    captureBtn.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white;"); // Green Button
                    captureBtn.setText("Captured!");
                } else {
                    System.out.println("❌ Error: File save nahi hui.");
                }
            }
        } else {
            System.out.println("⚠️ No Face Detected!");
        }
    }

    // --- SAVE BUTTON ACTION (Database Logic) ---
    private void handleSaveStudent(ActionEvent event) {
        String name = nameField.getText();
        String roll = rollNoField.getText();
        String dept = depField.getText();

        // Validation
        if (name.isEmpty() || roll.isEmpty() || dept.isEmpty()) {
            System.out.println("⚠️ Please fill all fields!");
            return;
        }

        if (this.currentImagePath == null) {
            System.out.println("⚠️ Warning: Photo capture nahi ki gayi!");
            // Agar aap chahein to yahan 'return' laga dein taake bina photo save na ho
        }

        saveToDatabase(name, roll, dept);
    }

    private void saveToDatabase(String name, String roll, String dept) {
        // Query mein 4th value (face_image_path) add ki hai
        String query = "INSERT INTO students (full_name, roll_number, department, face_image_path) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseHandler.getDBConnection();
             PreparedStatement pst = conn.prepareStatement(query)) {

            pst.setString(1, name);
            pst.setString(2, roll);
            pst.setString(3, dept);
            pst.setString(4, this.currentImagePath); // Photo ka path yahan jayega

            int result = pst.executeUpdate();
            if (result > 0) {
                System.out.println("✅ Student & Photo Linked Successfully!");

                // Form Reset Logic
                nameField.clear();
                rollNoField.clear();
                depField.clear();
                captureBtn.setStyle(null);
                captureBtn.setText("Capture Face");
                this.currentImagePath = null; // Path reset
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("❌ Error Saving Data to Database");
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