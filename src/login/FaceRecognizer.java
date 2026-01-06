package login;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FaceRecognizer {

    private static final String IMAGE_FOLDER = "saved_faces";

    // 👇 Threshold kam kar diya hai (Pehle 0.50 tha)
    private static final double THRESHOLD = 0.40;

    public String recognizeFace(Mat liveFace) {
        File folder = new File(IMAGE_FOLDER);
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".jpg") || name.endsWith(".png"));

        if (files == null || files.length == 0) {
            System.out.println("⚠️ Warning: Saved faces folder khali hai!");
            return null;
        }

        double bestScore = -1.0;
        String identifiedRollNo = null;

        // Live face ko resize karo (Standard Size: 150x150)
        Mat resizedLive = new Mat();
        Imgproc.resize(liveFace, resizedLive, new Size(150, 150));

        for (File file : files) {
            Mat savedImage = Imgcodecs.imread(file.getAbsolutePath());
            if (savedImage.empty()) continue;

            // Saved image ko bhi resize karo (Standard Size: 150x150)
            Mat resizedSaved = new Mat();
            Imgproc.resize(savedImage, resizedSaved, new Size(150, 150));

            // Gray scale conversion
            Imgproc.cvtColor(resizedSaved, resizedSaved, Imgproc.COLOR_BGR2GRAY);

            // Compare
            double score = compareHistograms(resizedLive, resizedSaved);

            // 👇 Console mein score print hoga ab
            // System.out.println("Checking: " + file.getName() + " | Score: " + String.format("%.2f", score));

            if (score > THRESHOLD && score > bestScore) {
                bestScore = score;
                identifiedRollNo = file.getName().split("_")[0];
            }
        }

        // Agar score acha mila to return karo
        if (identifiedRollNo != null) {
            System.out.println("✅ MATCH FOUND: " + identifiedRollNo + " (Score: " + bestScore + ")");
            return identifiedRollNo;
        }

        return null; // Koi match nahi mila
    }

    private double compareHistograms(Mat img1, Mat img2) {
        try {
            Mat hist1 = new Mat();
            Mat hist2 = new Mat();

            MatOfFloat ranges = new MatOfFloat(0, 256);
            MatOfInt histSize = new MatOfInt(256);

            List<Mat> images1 = new ArrayList<>(); images1.add(img1);
            Imgproc.calcHist(images1, new MatOfInt(0), new Mat(), hist1, histSize, ranges);

            List<Mat> images2 = new ArrayList<>(); images2.add(img2);
            Imgproc.calcHist(images2, new MatOfInt(0), new Mat(), hist2, histSize, ranges);

            Core.normalize(hist1, hist1, 0, 1, Core.NORM_MINMAX);
            Core.normalize(hist2, hist2, 0, 1, Core.NORM_MINMAX);

            return Imgproc.compareHist(hist1, hist2, Imgproc.CV_COMP_CORREL);
        } catch (Exception e) {
            return 0.0;
        }
    }
}