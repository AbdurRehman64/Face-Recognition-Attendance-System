package login;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

public class TestOpenCV {
    public static void main(String[] args) {
        try {
            // 1. Native Library Load karna (Yeh line har OpenCV code mein pehli honi chahiye)
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

            // 2. Version Check
            System.out.println("✅ OpenCV Successfully Loaded!");
            System.out.println("Version: " + Core.VERSION);

            // 3. Matrix Test (Math check)
            Mat m = Mat.eye(3, 3, CvType.CV_8UC1);
            System.out.println("m = \n" + m.dump());

        } catch (UnsatisfiedLinkError e) {
            System.out.println("❌ Error: DLL file link nahi hui!");
            System.out.println("Step 4 dobara check karein.");
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("❌ Koi aur error hai.");
            e.printStackTrace();
        }
    }
}