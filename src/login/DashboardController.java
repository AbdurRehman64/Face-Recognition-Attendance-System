package login;

import javafx.application.Platform;
import org.opencv.videoio.Videoio;
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

    // Photo ka Path save karne ke liye variable
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
            capture = new VideoCapture(0, Videoio.CAP_DSHOW);

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
                System.out.println(" Error: The webcam failed to initialize.");
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

    // CAPTURE BUTTON ACTION (Multiple Photos) ---
    private void handleCaptureFace(ActionEvent event) {
        String rollNo = rollNoField.getText().trim();

        if (rollNo.isEmpty()) {
            System.out.println("Please provide a Roll Number before proceeding");
            return;
        }

        // Folder check
        File directory = new File("saved_faces");
        if (!directory.exists()) directory.mkdirs();

        int count = 0;
        int maxPhotos = 10;

        for (int i = 0; i < maxPhotos; i++) {
            Mat frame = new Mat();

            //new frame
            if (capture.read(frame)) {

                // Detection
                MatOfRect faces = new MatOfRect();
                Mat grayFrame = new Mat();
                Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);
                this.faceDetector.detectMultiScale(grayFrame, faces);

                if (faces.toArray().length > 0) {
                    Rect rect = faces.toArray()[0];
                    Mat faceOnly = new Mat(frame, rect);
                    // Grayscale save
                    Imgproc.cvtColor(faceOnly, faceOnly, Imgproc.COLOR_BGR2GRAY);

                    // File Name: RollNo_Time_Counter.jpg
                    String finalFileName = rollNo + "_" + System.currentTimeMillis() + "_" + i + ".jpg";
                    File fileToSave = new File(directory, finalFileName);

                    boolean saved = Imgcodecs.imwrite(fileToSave.getAbsolutePath(), faceOnly);

                    if (saved) {
                        count++;
                        System.out.println(" Photo " + count + " Saved");

                        // Database ke liye sirf pehli photo ka path kaafi hai
                        if (this.currentImagePath == null) {
                            this.currentImagePath = fileToSave.getAbsolutePath();
                        }
                    }
                }
            }

            try { Thread.sleep(50); } catch (Exception e) {}
        }

        if (count > 0) {
            System.out.println("Dataset Created! Total Photos: " + count);
            captureBtn.setText("Captured (" + count + ")");
            captureBtn.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white;");
        } else {
            System.out.println("Error: Face not detected. Please try again.");
        }
    }

    // --- SAVE BUTTON ACTION (Database Logic) ---
    private void handleSaveStudent(ActionEvent event) {
        String name = nameField.getText();
        String roll = rollNoField.getText();
        String dept = depField.getText();

        if (name.isEmpty() || roll.isEmpty() || dept.isEmpty()) {
            System.out.println("Please fill all fields!");
            return;
        }

        if (this.currentImagePath == null) {
            System.out.println(" Warning: Capture failed.");
        }

        saveToDatabase(name, roll, dept);
    }

    private void saveToDatabase(String name, String roll, String dept) {
        String query = "INSERT INTO students (full_name, roll_number, department, face_image_path) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseHandler.getDBConnection();
             PreparedStatement pst = conn.prepareStatement(query)) {

            pst.setString(1, name);
            pst.setString(2, roll);
            pst.setString(3, dept);
            pst.setString(4, this.currentImagePath);

            int result = pst.executeUpdate();
            if (result > 0) {
                System.out.println(" Student & Photo Linked Successfully!");

                // Form Reset Logic
                nameField.clear();
                rollNoField.clear();
                depField.clear();
                captureBtn.setStyle(null);
                captureBtn.setText("Capture Face");
                this.currentImagePath = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error Saving Data to Database");
        }
    }


    @FXML
    private void goToAttendance(ActionEvent event) {
        stopCamera();
        loadScreen(event, "/AttendanceView.fxml", "Live Attendance Monitor");
    }

    @FXML
    private void goToReports(ActionEvent event) {
        System.out.println(" Trying to open Reports Screen..."); // Debugging Line

        try {

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ReportsView.fxml"));
            Parent root = loader.load();


            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);


            String cssPath = "/css/style.css";
            if (getClass().getResource(cssPath) != null) {
                scene.getStylesheets().add(getClass().getResource(cssPath).toExternalForm());
            }


            stage.setScene(scene);
            stage.setTitle("Attendance Reports");
            stage.show();

            System.out.println(" Reports Screen Opened Successfully!");

        } catch (IOException e) {
            System.out.println(" ERROR: The Reports Screen is not opening!");
            e.printStackTrace(); // Console mein error dekhein
        }
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
    @FXML
    private void handleDashboardBtn(ActionEvent event) {
        System.out.println("Already on Dashboard");
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