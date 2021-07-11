package com.aonescan.scanner.Libraries;

import android.graphics.Bitmap;
import android.util.Log;

import com.aonescan.scanner.Helpers.ImageUtils;
import com.aonescan.scanner.Helpers.MathUtils;
import com.zomato.photofilters.imageprocessors.Filter;
import com.zomato.photofilters.imageprocessors.subfilters.BrightnessSubFilter;
import com.zomato.photofilters.imageprocessors.subfilters.ContrastSubFilter;
import com.zomato.photofilters.imageprocessors.subfilters.SaturationSubFilter;

import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class NativeClass {

    private static final int THRESHOLD_LEVEL = 2;
    private static final double AREA_LOWER_THRESHOLD = 0.2;
    private static final double AREA_UPPER_THRESHOLD = 0.98;
    private static final double DOWNSCALE_IMAGE_SIZE = 600f;
    private static final Comparator<MatOfPoint2f> AreaDescendingComparator = new Comparator<MatOfPoint2f>() {
        public int compare(MatOfPoint2f m1, MatOfPoint2f m2) {
            double area1 = Imgproc.contourArea(m1);
            double area2 = Imgproc.contourArea(m2);
            return (int) Math.ceil(area2 - area1);
        }
    };

    static
    {
        System.loadLibrary("NativeImageProcessor");
    }
    private Mat enhancedBitmap = new Mat();

    public void setEnhancedBitmap(Mat enhancedBitmap){
        this.enhancedBitmap=enhancedBitmap;
    }

    public Bitmap getScannedBitmap(Bitmap bitmap, float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4) {
        PerspectiveTransformation perspective = new PerspectiveTransformation();
        MatOfPoint2f rectangle = new MatOfPoint2f();
        rectangle.fromArray(new Point(x1, y1), new Point(x2, y2), new Point(x3, y3), new Point(x4, y4));
        Mat dstMat = perspective.transform(ImageUtils.bitmapToMat(bitmap), rectangle);
        return ImageUtils.matToBitmap(dstMat);
    }

    public MatOfPoint2f getPoint(Bitmap bitmap) {

        Mat src = ImageUtils.bitmapToMat(bitmap);

        // Downscale image for better performance.
        double ratio = DOWNSCALE_IMAGE_SIZE / Math.max(src.width(), src.height());
        Size downscaledSize = new Size(src.width() * ratio, src.height() * ratio);
        Mat downscaled = new Mat(downscaledSize, src.type());
        Imgproc.resize(src, downscaled, downscaledSize);

        List<MatOfPoint2f> rectangles = getPoints(downscaled);
        if (rectangles.size() == 0) {
            return null;
        }
        Collections.sort(rectangles, AreaDescendingComparator);
        MatOfPoint2f largestRectangle = rectangles.get(0);
        MatOfPoint2f result = MathUtils.scaleRectangle(largestRectangle, 1f / ratio);
        return result;
    }

    private static Mat morph_kernel = new Mat(new Size(3,3),CvType.CV_8UC1,new Scalar(255));


    //public native float[] getPoints(Bitmap bitmap);
    public List<MatOfPoint2f> getPoints(Mat src) {

        // Blur the image to filter out the noise.
        Mat blurred = new Mat();
        Imgproc.medianBlur(src, blurred, 7);

        // Set up images to use.
        Mat gray0 = new Mat(blurred.size(), CvType.CV_8U);
        Mat gray = new Mat();

        // For Core.mixChannels.
        List<MatOfPoint> contours = new ArrayList<>();
        List<MatOfPoint2f> rectangles = new ArrayList<>();

        List<Mat> sources = new ArrayList<>();
        sources.add(blurred);
        List<Mat> destinations = new ArrayList<>();
        destinations.add(gray0);

        // To filter rectangles by their areas.
        int srcArea = src.rows() * src.cols();


        // Find squares in every color plane of the image.
        for (int c = 0; c < 3; c++) {
            int[] ch = {c, 0};
            MatOfInt fromTo = new MatOfInt(ch);

            Core.mixChannels(sources, destinations, fromTo);
            Mat element = new Mat();
            // Try several threshold levels.
            for (int l = 0; l < THRESHOLD_LEVEL; l++) {
                if (l == 0) {
                    // HACK: Use Canny instead of zero threshold level.
                    // Canny helps to catch squares with gradient shading.
                    // NOTE: No kernel size parameters on Java API.


                    Imgproc.Canny(gray0, gray, 185 , 85);


                    // Dilate Canny output to remove potential holes between edge segments.
//                    Imgproc.dilate(gray, gray, Mat.ones(new Size(3, 3), 0));
                    Imgproc.threshold(gray,gray,155,255,Imgproc.THRESH_TOZERO);
                    Imgproc.morphologyEx(gray,gray,Imgproc.MORPH_CLOSE,morph_kernel,new Point(-1,-1),1);
                } else {
                    int threshold = (l + 1) * 255 / THRESHOLD_LEVEL;
                    Imgproc.threshold(gray0, gray, threshold, 255, Imgproc.THRESH_BINARY);
                }

                // Find contours and store them all as a list.
                Imgproc.findContours(gray, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

                for (MatOfPoint contour : contours) {
                    MatOfPoint2f contourFloat = MathUtils.toMatOfPointFloat(contour);
                    double arcLen = Imgproc.arcLength(contourFloat, true) * 0.02;

                    // Approximate polygonal curves.
                    MatOfPoint2f approx = new MatOfPoint2f();
                    Imgproc.approxPolyDP(contourFloat, approx, arcLen, true);

                    if (isRectangle(approx, srcArea)) {
                        rectangles.add(approx);
                    }
                }
            }
        }
        return rectangles;
    }

    private byte saturate(double val) {
        int iVal = (int) Math.round(val);
        iVal = iVal > 255 ? 255 : (iVal < 0 ? 0 : iVal);
        return (byte) iVal;
    }

    public Bitmap getBlackAndWhiteBitmap(Mat src) {
        Mat gray = new Mat();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGB2GRAY);

        return ImageUtils.matToBitmap(gray);
    }

    public double lap(Mat gray) {
        Mat temp = new Mat();
        Imgproc.Laplacian(gray, temp, 3);
        MatOfDouble median = new MatOfDouble();
        MatOfDouble std = new MatOfDouble();
        Core.meanStdDev(temp, median, std);
        double d = Math.pow(std.get(0, 0)[0], 2);
        return d;
    }

//    private Filter getPaperFilter(float alpha, float beta) {
//        Filter myFilter = new Filter();
////        com.zomato.photofilters.geometry.Point[] rgbKnots;
////        rgbKnots = new com.zomato.photofilters.geometry.Point[5];
////        rgbKnots[0] = new com.zomato.photofilters.geometry.Point(0,0);
////        rgbKnots[1] = new com.zomato.photofilters.geometry.Point(83,117);
////        rgbKnots[2] = new com.zomato.photofilters.geometry.Point(130,135);
////        rgbKnots[3] = new com.zomato.photofilters.geometry.Point(156,212);
////        rgbKnots[4] = new  com.zomato.photofilters.geometry.Point(255,255);
////        rgbKnots = new com.zomato.photofilters.geometry.Point[2];
////        rgbKnots[0] = new com.zomato.photofilters.geometry.Point(0,255);
////        rgbKnots[1] = new com.zomato.photofilters.geometry.Point(255,0);
//
////        myFilter.addSubFilter(new ContrastSubFilter(Math.abs(beta)-10.0f));
////        myFilter.addSubFilter(new ContrastSubFilter(alpha*1f));
////        myFilter.addSubFilter(new SaturationSubFilter(Math.abs(alpha)*0.008f));
//        return myFilter;
//    }

//    public Filter getDarkerFilter() {
//        Filter myFilter = new Filter();
//        com.zomato.photofilters.geometry.Point[] rgbKnots;
//        rgbKnots = new com.zomato.photofilters.geometry.Point[3];
//        rgbKnots[0] = new com.zomato.photofilters.geometry.Point(0, 0);
//        rgbKnots[1] = new com.zomato.photofilters.geometry.Point(143, 86);
//        rgbKnots[2] = new com.zomato.photofilters.geometry.Point(255, 255);
//
//        myFilter.addSubFilter(new ToneCurveSubFilter(rgbKnots, null, null, null));
//        return myFilter;
//    }

    public Filter getAutoBrightnessFilter(float alpha,float beta){
        Filter filter = new Filter();
        Log.d("Alpha",String.valueOf(alpha));
        Log.d("Beta",String.valueOf(beta));
        filter.addSubFilter(new BrightnessSubFilter((int) (Math.abs(alpha)*4)));
        filter.addSubFilter(new SaturationSubFilter((float) ((Math.abs(beta)+0.1)*1.01)));
        filter.addSubFilter(new ContrastSubFilter(1.2f));
        return filter;
    }


    public Bitmap getMagicColoredBitmap(Mat src, int iterations) {
        src.convertTo(src, CvType.CV_32FC3);
        Scan scanner = new Scan(src, 51, 66, 160);
        Mat scannedImg = scanner.scanImage(Scan.ScanMode.GCMODE);
        List<Float> imgValues = ImageUtils.brightnessAndContrastAuto(scannedImg, 1f);
        Filter filter = getAutoBrightnessFilter(imgValues.get(0),imgValues.get(1));
        Bitmap output = filter.processFilter(ImageUtils.matToBitmap(scannedImg));
        Size size;
        switch (iterations) {
            case 2:
                size = new Size(3, 3);
                break;
            case 3:
                size = new Size(4, 4);
                break;
            case 4:
                size = new Size(5, 5);
                break;
            default:
                size = new Size(2, 2);
                break;
        }
        Mat auto = ImageUtils.bitmapToMat(output);
        Mat s = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, size);
        Imgproc.erode(auto, auto, s);
        auto.copyTo(enhancedBitmap);
        return ImageUtils.matToBitmap(auto);
    }

    public Bitmap getErodedImage(int iterations) {
        Mat erode = new Mat();
        Size size;
        switch (iterations) {
            case 2:
                size = new Size(3, 3);
                break;
            case 3:
                size = new Size(4, 4);
                break;
            case 4:
                size = new Size(5, 5);
                break;
            default:
                size = new Size(2, 2);
                break;
        }
        Mat s = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, size);
            try {
                Imgproc.erode(enhancedBitmap, erode, s);

            }catch (CvException e){
                e.printStackTrace();
                return null;
            }

        return ImageUtils.matToBitmap(enhancedBitmap);
    }

    private boolean isRectangle(MatOfPoint2f polygon, int srcArea) {
        MatOfPoint polygonInt = MathUtils.toMatOfPointInt(polygon);

        if (polygon.rows() != 4) {
            return false;
        }

        double area = Math.abs(Imgproc.contourArea(polygon));
        if (area < srcArea * AREA_LOWER_THRESHOLD || area > srcArea * AREA_UPPER_THRESHOLD) {
            return false;
        }

        if (!Imgproc.isContourConvex(polygonInt)) {
            return false;
        }

        // Check if the all angles are more than 72.54 degrees (cos 0.3).
        double maxCosine = 0;
        Point[] approxPoints = polygon.toArray();

        for (int i = 2; i < 5; i++) {
            double cosine = Math.abs(MathUtils.angle(approxPoints[i % 4], approxPoints[i - 2], approxPoints[i - 1]));
            maxCosine = Math.max(cosine, maxCosine);
        }

        return !(maxCosine >= 0.3);
    }

}


