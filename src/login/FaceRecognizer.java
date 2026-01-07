package login;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FaceRecognizer {

    private static final String IMAGE_FOLDER = "saved_faces";

    // 👇 Threshold ko bohot kam kar diya hai testing ke liye
    private static final double THRESHOLD = 0.3;

    public String recognizeFace(Mat liveFace) {
        File folder = new File(IMAGE_FOLDER);
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".jpg") || name.endsWith(".png"));

        if (files == null || files.length == 0) return null;

        double bestScore = -1.0;
        String identifiedRollNo = null;

        // 1. Live Face Process (Resize + Equalize)
        Mat processedLive = preprocessImage(liveFace);

        // System.out.println("--- Scaning New Frame ---"); // Debugging line

        for (File file : files) {
            // Saved image ko Grayscale mein hi load karein
            Mat savedImage = Imgcodecs.imread(file.getAbsolutePath(), Imgcodecs.IMREAD_GRAYSCALE);

            if (savedImage.empty()) continue;

            // 2. Saved Face Process
            Mat processedSaved = preprocessImage(savedImage);

            // 3. Compare Histograms
            double score = compareHistograms(processedLive, processedSaved);

            // 👇 IMPORTANT: Console mein score print karwaya hai
            // Agar score 0.1 bhi aaye to humein pata chal jayega
            if (score > 0.1) {
                System.out.println("Checking: " + file.getName() + " | Score: " + String.format("%.2f", score));
            }

            if (score > THRESHOLD && score > bestScore) {
                bestScore = score;
                identifiedRollNo = file.getName().split("_")[0];
            }
        }

        if (identifiedRollNo != null) {
            return identifiedRollNo;
        }

        return null;
    }

    private Mat preprocessImage(Mat img) {
        Mat processed = new Mat();

        // Resize to fixed standard size
        Imgproc.resize(img, processed, new Size(150, 150));

        // Equalize Histogram (Light balance karna)
        Imgproc.equalizeHist(processed, processed);

        return processed;
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