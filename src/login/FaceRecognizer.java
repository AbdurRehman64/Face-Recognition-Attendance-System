package login;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FaceRecognizer {

    private static final String IMAGE_FOLDER = "saved_faces";

    // Threshold abhi 0.30 hi rakhein
    private static final double THRESHOLD = 0.30;

    public String recognizeFace(Mat liveFace) {
        File folder = new File(IMAGE_FOLDER);
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".jpg") || name.endsWith(".png"));

        if (files == null || files.length == 0) return null;

        double bestScore = -1.0;
        String identifiedRollNo = null;

        // Live Face Process
        Mat processedLive = preprocessImage(liveFace);

        for (File file : files) {

            // ⭐ CHANGE IS HERE: IMREAD_GRAYSCALE flag lagaya hai
            // Isse pakka ho jayega ke saved photo 1 channel (Gray) hi rahe
            Mat savedImage = Imgcodecs.imread(file.getAbsolutePath(), Imgcodecs.IMREAD_GRAYSCALE);

            if (savedImage.empty()) continue;

            // Saved Face Process
            Mat processedSaved = preprocessImage(savedImage);

            // Compare
            double score = compareHistograms(processedLive, processedSaved);

            // Debug Print (Ab check karein score kya aa raha hai)
            // System.out.println("Checking: " + file.getName() + " | Score: " + score);

            if (score > THRESHOLD && score > bestScore) {
                bestScore = score;
                identifiedRollNo = file.getName().split("_")[0];
            }
        }

        if (identifiedRollNo != null) {
            System.out.println("✅ MATCH FOUND: " + identifiedRollNo + " (Score: " + bestScore + ")");
            return identifiedRollNo;
        }

        return null;
    }

    private Mat preprocessImage(Mat img) {
        Mat processed = new Mat();

        // Resize
        Imgproc.resize(img, processed, new Size(150, 150));

        // Equalize (Lighting Fix)
        // Note: Kyunke humne upar hi Grayscale load kiya hai, yahan conversion ki zaroorat nahi
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