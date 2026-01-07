package login;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;

public class ReportsController {

    // --- FXML UI Elements ---
    @FXML private TableView<AttendanceLog> reportsTable;
    @FXML private TableColumn<AttendanceLog, String> colDate;
    @FXML private TableColumn<AttendanceLog, String> colRoll;
    @FXML private TableColumn<AttendanceLog, String> colName;
    @FXML private TableColumn<AttendanceLog, String> colTime;
    @FXML private TableColumn<AttendanceLog, String> colStatus;

    @FXML private DatePicker datePicker;
    @FXML private TextField searchField;

    // Data List
    private ObservableList<AttendanceLog> attendanceList = FXCollections.observableArrayList();

    // --- INITIALIZE ---
    @FXML
    public void initialize() {
        // Columns Link karo
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colRoll.setCellValueFactory(new PropertyValueFactory<>("rollNo"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("time"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Load Initial Data (Sab records dikhao)
        loadData("SELECT * FROM attendance_logs ORDER BY attendance_date DESC, attendance_time DESC");
    }

    // --- DATA LOADING LOGIC ---
    private void loadData(String query) {
        attendanceList.clear();

        System.out.println("🔎 Executing Query: " + query); // Debugging line

        try (Connection conn = DatabaseHandler.getDBConnection();
             PreparedStatement pst = conn.prepareStatement(query);
             ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {
                attendanceList.add(new AttendanceLog(
                        rs.getString("attendance_date"),
                        rs.getString("student_roll_number"),
                        rs.getString("student_name"),
                        rs.getString("attendance_time"),
                        "Present"
                ));
            }
            reportsTable.setItems(attendanceList);

            if(attendanceList.isEmpty()) {
                System.out.println("⚠️ No records found for this search.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("❌ Error Loading Reports");
        }
    }

    // --- SEARCH LOGIC (FIXED) ---
    @FXML
    private void handleSearch(ActionEvent event) {
        String searchText = searchField.getText().trim();
        LocalDate date = datePicker.getValue();

        // Basic Query
        StringBuilder query = new StringBuilder("SELECT * FROM attendance_logs WHERE 1=1");

        // 1. Agar Date select ki hai
        if (date != null) {
            query.append(" AND attendance_date = '").append(date.toString()).append("'");
        }

        // 2. Agar Text likha hai (Naam ya Roll No)
        if (!searchText.isEmpty()) {
            // "LOWER" function use kiya taake chote/bade lafzon ka masla na ho
            query.append(" AND (student_name LIKE '%").append(searchText).append("%'")
                    .append(" OR student_roll_number LIKE '%").append(searchText).append("%')");
        }

        // 3. Order By (Latest pehle)
        query.append(" ORDER BY attendance_time DESC");

        // Load Data
        loadData(query.toString());
    }

    @FXML
    private void handleRefresh(ActionEvent event) {
        searchField.clear();
        datePicker.setValue(null);
        loadData("SELECT * FROM attendance_logs ORDER BY attendance_date DESC, attendance_time DESC");
    }

    // --- NAVIGATION ---
    @FXML
    private void goToAttendance(ActionEvent event) {
        loadScreen(event, "/AttendanceView.fxml", "Live Attendance Monitor");
    }

    @FXML
    private void handleDashboardBtn(ActionEvent event) {
        loadScreen(event, "/dashboard.fxml", "Admin Dashboard");
    }

    @FXML
    private void handleTrainBtn(ActionEvent event) {
        System.out.println("Training Data Clicked");
    }

    private void loadScreen(ActionEvent event, String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);

            String cssPath = "/css/style.css";
            if (getClass().getResource(cssPath) != null) {
                scene.getStylesheets().add(getClass().getResource(cssPath).toExternalForm());
            }

            stage.setScene(scene);
            stage.setTitle(title);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("❌ Failed to load screen: " + fxmlPath);
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        loadScreen(event, "/login.fxml", "Admin Login");
    }
    // --- INNER CLASS (Data Model) ---
    public static class AttendanceLog {
        private String date, rollNo, name, time, status;

        public AttendanceLog(String date, String rollNo, String name, String time, String status) {
            this.date = date;
            this.rollNo = rollNo;
            this.name = name;
            this.time = time;
            this.status = status;
        }

        public String getDate() { return date; }
        public String getRollNo() { return rollNo; }
        public String getName() { return name; }
        public String getTime() { return time; }
        public String getStatus() { return status; }
    }
}