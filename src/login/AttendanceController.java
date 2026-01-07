package login;

import org.opencv.videoio.Videoio;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AttendanceController {

    @FXML private ImageView cameraView;
    @FXML private Label statusLabel;
    @FXML private Button startBtn;
    @FXML private Button stopBtn;


    @FXML private TableView<AttendanceRow> attendanceTable;
    @FXML private TableColumn<AttendanceRow, String> colRoll;
    @FXML private TableColumn<AttendanceRow, String> colName;
    @FXML private TableColumn<AttendanceRow, String> colTime;

    // List to hold table data
    private ObservableList<AttendanceRow> attendanceData = FXCollections.observableArrayList();

    // Cooldown Logic: Key = RollNo, Value = Last Time
    private Map<String, Long> lastScanTime = new HashMap<>();
    private static final long COOLDOWN_TIME = 60 * 1000; // 1 Minute Wait

    // OpenCV & Logic Variables
    private VideoCapture capture;
    private ScheduledExecutorService timer;
    private boolean cameraActive = false;
    private CascadeClassifier faceDetector;
    private FaceRecognizer recognizer;

    // --- INITIALIZE ---
    @FXML
    public void initialize() {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        // Helper Classes Initialize
        this.faceDetector = new CascadeClassifier("haarcascade_frontalface_alt.xml");
        this.recognizer = new FaceRecognizer();

        // Table Setup
        colRoll.setCellValueFactory(new PropertyValueFactory<>("rollNo"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("time"));
        attendanceTable.setItems(attendanceData);

        // Buttons
        startBtn.setOnAction(event -> startCamera());
        stopBtn.setOnAction(event -> stopCamera());

        // Auto Start
        startCamera();
    }

    // --- CAMERA & RECOGNITION LOGIC ---
    private void startCamera() {
        if (!cameraActive) {
            // 1. DSHOW Zaroor Lagayein
            capture = new VideoCapture(0, org.opencv.videoio.Videoio.CAP_DSHOW);

            if (capture.isOpened()) {
                cameraActive = true;
                statusLabel.setText("Scanning Active...");
                System.out.println("✅ Camera Started Successfully!");

                // Check: Kya Haar Cascade load hua?
                if (this.faceDetector.empty()) {
                    System.out.println("❌ CRITICAL ERROR: The haarcascade file did not load!");
                    System.out.println("Make sure 'haarcascade_frontalface_alt.xml' is in the project folder.");
                    return;
                }

                Runnable frameGrabber = () -> {
                    Mat frame = new Mat();

                    // 2. Frame Read Check
                    if (capture.read(frame)) {

                        // Debug: Har 50 frames ke baad bataye ke camera zinda hai
                        // (Taake console spam na ho, lekin humein pata chale ke chal raha hai)
                        if (System.currentTimeMillis() % 2000 < 50) {
                            System.out.println("🔄 Camera Running... Frame Processing...");
                        }

                        Mat grayFrame = new Mat();
                        MatOfRect faces = new MatOfRect();

                        try {
                            // 3. Image Conversion
                            Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);
                            Imgproc.equalizeHist(grayFrame, grayFrame);

                            // 4. Detection Logic
                            this.faceDetector.detectMultiScale(grayFrame, faces);

                            // Debug: Agar 1 bhi chehra mila to foran batao
                            if (faces.toArray().length > 0) {
                                System.out.println("👀 FACE DETECTED: " + faces.toArray().length);
                            }

                            for (Rect rect : faces.toArray()) {
                                Imgproc.rectangle(frame, new Point(rect.x, rect.y),
                                        new Point(rect.x + rect.width, rect.y + rect.height),
                                        new Scalar(0, 255, 0), 2);

                                Mat faceOnly = new Mat(grayFrame, rect);

                                // 5. Recognition Check
                                String rollNo = recognizer.recognizeFace(faceOnly);

                                if (rollNo != null) {
                                    System.out.println("🎉 MATCH: " + rollNo);
                                    Platform.runLater(() -> markAttendance(rollNo));
                                    Imgproc.putText(frame, rollNo, new Point(rect.x, rect.y-10),
                                            Imgproc.FONT_HERSHEY_SIMPLEX, 1.0, new Scalar(0,255,0), 2);
                                } else {
                                    // System.out.println("❌ Face detected but not recognized");
                                }
                            }

                            Image imageToShow = mat2Image(frame);
                            Platform.runLater(() -> cameraView.setImage(imageToShow));
                        } catch (Exception e) {
                            System.out.println("❌ ERROR during Processing: " + e.getMessage());
                        }

                    } else {
                        System.out.println("⚠️ Warning: Frame read failed (Blank Frame)");
                    }
                };

                timer = Executors.newSingleThreadScheduledExecutor();
                timer.scheduleAtFixedRate(frameGrabber, 0, 100, TimeUnit.MILLISECONDS);

                startBtn.setDisable(true);
                stopBtn.setDisable(false);
            } else {
                System.out.println("❌ Error: Could not open the camera!");
            }
        }
    }

    // --- ATTENDANCE MARKING (DATABASE ONLY) ---
    private void markAttendance(String rollNo) {
        long currentTime = System.currentTimeMillis();

        // 1. Cooldown Check (Taake duplicate entry na ho)
        if (lastScanTime.containsKey(rollNo)) {
            long lastTime = lastScanTime.get(rollNo);
            if ((currentTime - lastTime) < COOLDOWN_TIME) {
                // Abhi 1 minute nahi guzra, ignore karo
                return;
            }
        }

        // --- NEW ENTRY ---
        lastScanTime.put(rollNo, currentTime);

        // 2. Database se Naam lo
        String studentName = getStudentNameFromDB(rollNo);
        String timeStr = new SimpleDateFormat("HH:mm:ss").format(new Date());

        // 3. Screen Table Update
        attendanceData.add(new AttendanceRow(rollNo, studentName, timeStr));
        statusLabel.setText("Verified: " + studentName);
        statusLabel.setStyle("-fx-text-fill: #2ecc71;"); // Green

        // 4. ⭐ DATABASE SAVE (No File Export)
        saveAttendanceToDB(rollNo, studentName);
    }

    private void saveAttendanceToDB(String rollNo, String name) {
        // Query: Date aur Time automatically CURDATE() aur CURTIME() se aayega
        String query = "INSERT INTO attendance_logs (student_roll_number, student_name, attendance_date, attendance_time) VALUES (?, ?, CURDATE(), CURTIME())";

        try (Connection conn = DatabaseHandler.getDBConnection();
             PreparedStatement pst = conn.prepareStatement(query)) {

            pst.setString(1, rollNo);
            pst.setString(2, name);

            int result = pst.executeUpdate();
            if (result > 0) {
                System.out.println("✅ Database Saved Successfully: " + name);
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("❌ Database Error: Attendance was not saved!");
        }
    }

    private String getStudentNameFromDB(String rollNo) {
        String name = "Unknown";
        String query = "SELECT full_name FROM students WHERE roll_number = ?";
        try (Connection conn = DatabaseHandler.getDBConnection();
             PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setString(1, rollNo);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                name = rs.getString("full_name");
            }
        } catch (Exception e) { e.printStackTrace(); }
        return name;
    }

    private void stopCamera() {
        if (cameraActive) {
            cameraActive = false;
            try { if (timer != null) timer.shutdown(); } catch (Exception e) {}
            if (capture != null) capture.release();
            startBtn.setDisable(false);
            stopBtn.setDisable(true);
        }
    }

    private Image mat2Image(Mat frame) {
        try {
            MatOfByte buffer = new MatOfByte();
            Imgcodecs.imencode(".png", frame, buffer);
            return new Image(new ByteArrayInputStream(buffer.toArray()));
        } catch (Exception e) { return null; }
    }

    @FXML
    private void goBackToDashboard(ActionEvent event) {
        stopCamera();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/dashboard.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            String cssPath = "/css/style.css";
            if (getClass().getResource(cssPath) != null) scene.getStylesheets().add(getClass().getResource(cssPath).toExternalForm());
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) { e.printStackTrace(); }
    }

    // Table Data Class
    public static class AttendanceRow {
        private String rollNo, name, time;
        public AttendanceRow(String rollNo, String name, String time) {
            this.rollNo = rollNo; this.name = name; this.time = time;
        }
        public String getRollNo() { return rollNo; }
        public String getName() { return name; }
        public String getTime() { return time; }
    }
}