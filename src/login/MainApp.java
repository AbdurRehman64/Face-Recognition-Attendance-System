package login; // Aapke screenshot ke mutabiq package name 'login' hai

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.net.URL;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // ✅ Step 1: File Load karna
            // Note: "/" ka matlab hai ke computer 'resources' folder mein dhoondega
            URL fxmlLocation = getClass().getResource("/login.fxml");

            // ✅ Step 2: Check karna ke file mili ya nahi
            if (fxmlLocation == null) {
                System.out.println("------------------------------------------------");
                System.out.println("❌ ERROR: 'login.fxml' file nahi mili!");
                System.out.println("👉 Make sure karein ke file 'resources' folder ke andar hai.");
                System.out.println("------------------------------------------------");
                return; // Code yahi rok do
            }

            // ✅ Step 3: Scene Load karna
            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent root = loader.load();

            primaryStage.setTitle("Face Recognition Attendance System");
            primaryStage.setScene(new Scene(root));
            primaryStage.show();

        } catch (Exception e) {
            System.out.println("❌ Code mein koi aur error aaya hai:");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}