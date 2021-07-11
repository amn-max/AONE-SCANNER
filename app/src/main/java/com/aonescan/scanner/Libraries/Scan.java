package com.aonescan.scanner.Libraries;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class Scan {

    private final Mat inputImg;
    private final Mat filtered = new Mat();
    private final int blackPoint;
    private Mat processedImg = new Mat();
    private int kSize;
    private int whitePoint;

    public Scan(Mat image, int kernelSize, int blackPoint, int whitePoint) {
        this.inputImg = image.clone();
        this.kSize = kernelSize;
        this.blackPoint = blackPoint;
        this.whitePoint = whitePoint;
    }

    public void highPassFilter() {
        if (kSize % 2 == 0)
            kSize++;

        Mat kernel = Mat.ones(kSize, kSize, CvType.CV_32FC1);
        kernel = kernel.mul(kernel, 1 / ((float) kSize * (float) kSize));

        Imgproc.filter2D(inputImg, filtered, -1, kernel);

        filtered.convertTo(filtered, CvType.CV_32FC3);
        inputImg.convertTo(inputImg, CvType.CV_32FC3);

        Core.subtract(inputImg, filtered, filtered);

        kernel = Mat.zeros(inputImg.size(), CvType.CV_32FC3);
        kernel.setTo(new Scalar(1, 1, 1));

        Core.multiply(kernel, new Scalar(127.0, 127.0, 127.0), kernel);

        if(filtered.channels()-kernel.channels()==1){
            Imgproc.cvtColor(filtered, filtered,Imgproc.COLOR_BGRA2BGR);
        }

        Core.add(filtered, kernel, filtered);

        filtered.convertTo(filtered, CvType.CV_8UC3);

    }

    private void whitePointSelect() {


        Imgproc.threshold(processedImg, processedImg, whitePoint, 255, Imgproc.THRESH_TRUNC);

        Core.subtract(processedImg, new Scalar(0, 0, 0), processedImg);

        float tmp = (255.0f) / ((float) whitePoint - 0);
        Core.multiply(processedImg, new Scalar(tmp, tmp, tmp), processedImg);

    }

    private void blackPointSelect() {


        Core.subtract(processedImg, new Scalar(blackPoint, blackPoint, blackPoint), processedImg);

        float tmp = (255.0f) / (255.0f - blackPoint);
        Core.multiply(processedImg, new Scalar(tmp, tmp, tmp), processedImg);
    }

    private void blackAndWhite() {

        List<Mat> lab = new ArrayList<>();
        Mat subA = new Mat();
        Mat subB = new Mat();

        Imgproc.cvtColor(processedImg, processedImg, Imgproc.COLOR_BGR2Lab);
        Core.split(processedImg, lab);

        Core.subtract(lab.get(0), lab.get(1), subA);
        Core.subtract(lab.get(0), lab.get(2), subB);

        Core.add(subA, subB, processedImg);
    }

    public Mat scanImage(ScanMode mode) {

        switch (mode) {
            case GCMODE:
                this.highPassFilter();
                processedImg = filtered.clone();
                // Fix white point value at 127 for GCMODE
                this.whitePoint = 127;
                this.whitePointSelect();
                this.blackPointSelect();
                break;

            case RMODE:
                processedImg = inputImg.clone();
                this.blackPointSelect();
                this.whitePointSelect();
                break;

            case SMODE:
                processedImg = inputImg.clone();
                this.blackPointSelect();
                this.whitePointSelect();
                this.blackAndWhite();
                break;

            default:
                System.out.println("Error: Incorrect ScanMode supplied. Expected input: GCSCAN/RSCAN/SCAN");
                break;
        }


        return processedImg;
    }

    public enum ScanMode {
        GCMODE,
        RMODE,
        SMODE,
    }
}