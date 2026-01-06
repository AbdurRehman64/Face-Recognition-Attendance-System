package login;

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
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AttendanceController {

    @FXML private ImageView cameraView;
    @FXML private Label statusLabel;
    @FXML private Button startBtn;
    @FXML private Button stopBtn;

    // Table Elements
    @FXML private TableView<AttendanceRow> attendanceTable;
    @FXML private TableColumn<AttendanceRow, String> colRoll;
    @FXML private TableColumn<AttendanceRow, String> colName;
    @FXML private TableColumn<AttendanceRow, String> colTime;

    // List to hold table data
    private ObservableList<AttendanceRow> attendanceData = FXCollections.observableArrayList();
    // Set to avoid duplicate entries (ek banda baar baar add na ho)
    private Set<String> scannedRollNumbers = new HashSet<>();

    // OpenCV & Logic Variables
    private VideoCapture capture;
    private ScheduledExecutorService timer;
    private boolean cameraActive = false;
    private CascadeClassifier faceDetector;
    private FaceRecognizer recognizer; // Hamari banayi hui class

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
    // --- CAMERA & RECOGNITION LOGIC (DEBUG VERSION) ---
    private void startCamera() {
        if (!cameraActive) {
            capture = new VideoCapture(0);
            if (capture.isOpened()) {
                cameraActive = true;
                statusLabel.setText("Scanning Active...");

                // Debug Message
                System.out.println("✅ Camera Start ho gaya. Scanning shuru...");

                Runnable frameGrabber = () -> {
                    Mat frame = new Mat();
                    if (capture.read(frame)) {

                        // 1. Detect Face
                        MatOfRect faces = new MatOfRect();
                        Mat grayFrame = new Mat();
                        Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);
                        Imgproc.equalizeHist(grayFrame, grayFrame);

                        this.faceDetector.detectMultiScale(grayFrame, faces);

                        // Debug: Agar chehra mila to batao
                        if (faces.toArray().length > 0) {
                            System.out.println("👀 Chehra Nazar Aya! (Faces: " + faces.toArray().length + ")");
                        }

                        // 2. Loop through faces
                        for (Rect rect : faces.toArray()) {
                            // Green Box Draw karo
                            Imgproc.rectangle(frame, new Point(rect.x, rect.y),
                                    new Point(rect.x + rect.width, rect.y + rect.height),
                                    new Scalar(0, 255, 0), 2);

                            // 3. Recognize Face
                            Mat faceOnly = new Mat(grayFrame, rect);

                            // Recognizer ko call karo
                            String rollNo = recognizer.recognizeFace(faceOnly);

                            if (rollNo != null) {
                                System.out.println("🎉 PEHCHAN LIYA! Roll No: " + rollNo);

                                Platform.runLater(() -> markAttendance(rollNo));

                                Imgproc.putText(frame, "ID: " + rollNo,
                                        new Point(rect.x, rect.y - 10),
                                        Imgproc.FONT_HERSHEY_SIMPLEX, 1.0,
                                        new Scalar(0, 255, 0), 2);
                            } else {
                                // Agar match nahi hua
                                System.out.println("❌ Chehra saaf nahi hai ya match nahi hua.");
                            }
                        }

                        Image imageToShow = mat2Image(frame);
                        Platform.runLater(() -> cameraView.setImage(imageToShow));
                    } else {
                        System.out.println("⚠️ Camera frame read nahi kar pa raha.");
                    }
                };

                timer = Executors.newSingleThreadScheduledExecutor();
                timer.scheduleAtFixedRate(frameGrabber, 0, 100, TimeUnit.MILLISECONDS);

                startBtn.setDisable(true);
                stopBtn.setDisable(false);
            } else {
                System.out.println("❌ Error: Camera open nahi hua!");
            }
        }
    }

    // --- ATTENDANCE MARKING ---
    private void markAttendance(String rollNo) {
        // Agar yeh banda pehle se scan nahi hua
        if (!scannedRollNumbers.contains(rollNo)) {

            // Database se naam nikalo
            String studentName = getStudentNameFromDB(rollNo);
            String currentTime = new SimpleDateFormat("HH:mm:ss").format(new Date());

            // 1. Table mein add karo
            attendanceData.add(new AttendanceRow(rollNo, studentName, currentTime));

            // 2. Duplicate list mein daal do
            scannedRollNumbers.add(rollNo);

            // 3. Status update
            statusLabel.setText("Verified: " + studentName);

            // (Optional) Yahan hum Database insert ki query bhi laga sakte hain 'attendance_logs' table mein
            saveAttendanceToDB(rollNo, studentName);
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

    private void saveAttendanceToDB(String rollNo, String name) {
        String query = "INSERT INTO attendance_logs (student_roll_number, student_name, attendance_date, attendance_time) VALUES (?, ?, CURDATE(), CURTIME())";
        try (Connection conn = DatabaseHandler.getDBConnection();
             PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setString(1, rollNo);
            pst.setString(2, name);
            pst.executeUpdate();
            System.out.println("✅ Database Updated for: " + name);
        } catch (Exception e) { e.printStackTrace(); }
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