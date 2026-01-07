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

import javafx.stage.FileChooser; // Save dialog ke liye
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.File;

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

    // --- EXPORT TO EXCEL (CSV) ---
    @FXML
    private void handleExport(ActionEvent event) {
        // 1. Check karo table khali to nahi?
        if (attendanceData.isEmpty()) {
            System.out.println("⚠️ Table khali hai, export karne ke liye kuch nahi.");
            return;
        }

        // 2. Save Dialog Kholo
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Attendance Report");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));

        // Default file name: Attendance_Date.csv
        String defaultName = "Attendance_" + new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + ".csv";
        fileChooser.setInitialFileName(defaultName);

        // Window reference lo
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            saveToFile(file);
        }
    }

    private void saveToFile(File file) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            // 3. Pehle Headers likho
            writer.write("Roll Number,Name,Time");
            writer.newLine();

            // 4. Table ka sara data loop karke likho
            for (AttendanceRow row : attendanceData) {
                writer.write(row.getRollNo() + "," + row.getName() + "," + row.getTime());
                writer.newLine();
            }

            System.out.println("✅ Report Exported Successfully: " + file.getAbsolutePath());
            statusLabel.setText("Report Saved!");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("❌ Error saving file.");
        }
    }

    // Puraana Set hata kar yeh Map use karein
    // Key: RollNo, Value: Last Attendance Time (in milliseconds)
    private java.util.Map<String, Long> lastScanTime = new java.util.HashMap<>();

    // Cooldown Time: 30 Minutes (in milliseconds)
    // 30 * 60 * 1000 = 1800000 ms
    // Testing ke liye hum 1 Minute rakhte hain (60 * 1000)
    private static final long COOLDOWN_TIME = 60 * 1000;
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
    // --- ATTENDANCE MARKING (WITH COOLDOWN) ---
    private void markAttendance(String rollNo) {
        long currentTime = System.currentTimeMillis();

        // Check: Kya yeh banda pehle scan hua hai?
        if (lastScanTime.containsKey(rollNo)) {
            long lastTime = lastScanTime.get(rollNo);

            // Agar abhi Cooldown time nahi guzra
            if ((currentTime - lastTime) < COOLDOWN_TIME) {
                // Console mein bata do, lekin attendance mat lagao
                // System.out.println("⏳ Wait! " + rollNo + " ki attendance pehle hi lag chuki hai.");
                return;
            }
        }

        // --- AGAR KAFI WAQT GUZAR GAYA HAI TO ATTENDANCE LAGAO ---

        // 1. Time Update karo
        lastScanTime.put(rollNo, currentTime);

        // 2. Database se naam nikalo
        String studentName = getStudentNameFromDB(rollNo);
        String timeStr = new SimpleDateFormat("HH:mm:ss").format(new Date());

        // 3. Table Update
        attendanceData.add(new AttendanceRow(rollNo, studentName, timeStr));

        // 4. Status Update
        statusLabel.setText("Verified: " + studentName);
        statusLabel.setStyle("-fx-text-fill: #2ecc71;"); // Green

        // 5. Database Save
        saveAttendanceToDB(rollNo, studentName);

        // 6. Sound Effect (Optional - Future mein)
        System.out.println("✅ Attendance Marked for: " + studentName);
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