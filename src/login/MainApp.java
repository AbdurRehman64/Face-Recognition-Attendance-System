package login; // Based on your screenshot, the package name is 'login'

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
            // ✅ Step 1: Load the file
            // Note: "/" means the system will look inside the 'resources' folder
            URL fxmlLocation = getClass().getResource("/login.fxml");

            // ✅ Step 2: Check if the file was found
            if (fxmlLocation == null) {
                System.out.println("------------------------------------------------");
                System.out.println("❌ ERROR: 'login.fxml' file not found!");
                System.out.println("👉 Make sure the file is inside the 'resources' folder.");
                System.out.println("------------------------------------------------");
                return; // Stop execution here
            }

            // ✅ Step 3: Load the Scene
            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent root = loader.load();

            primaryStage.setTitle("Face Recognition Attendance System");
            primaryStage.setScene(new Scene(root));
            primaryStage.show();

        } catch (Exception e) {
            System.out.println("❌ An error occurred in the code:");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}