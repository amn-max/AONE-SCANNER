package com.aonescan.scanner.Libraries;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class NoiseRemoval {

    /**
     * Mean filtering
     */
    public Mat blur(Mat src) {
        // grayscale when loading
        Mat dst = src.clone();
        Imgproc.blur(src, dst, new Size(9, 9), new Point(-1, -1), Core.BORDER_DEFAULT);
        return dst;

    }

    /**
     * Gaussian filtering
     */

    public Mat GaussianBlur(Mat src) {
        // grayscale when loading
        Mat dst = src.clone();
        Imgproc.GaussianBlur(src, dst, new Size(9, 9), 0, 0, Core.BORDER_DEFAULT);
        return dst;
    }

    /**
     * Median filtering
     */

    public Mat medianBlur(Mat src) {
        Mat dst = src.clone();
        Imgproc.medianBlur(src, dst, 7);
        return dst;
    }
}
