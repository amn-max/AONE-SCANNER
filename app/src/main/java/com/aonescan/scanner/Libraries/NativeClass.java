package com.aonescan.scanner.Libraries;

import android.graphics.Bitmap;
import android.util.Log;

import com.aonescan.scanner.Helpers.ImageUtils;
import com.aonescan.scanner.Helpers.MathUtils;
import com.zomato.photofilters.imageprocessors.Filter;
import com.zomato.photofilters.imageprocessors.subfilters.ToneCurveSubFilter;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfFloat;
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
    private static Comparator<MatOfPoint2f> AreaDescendingComparator = new Comparator<MatOfPoint2f>() {
        public int compare(MatOfPoint2f m1, MatOfPoint2f m2) {
            double area1 = Imgproc.contourArea(m1);
            double area2 = Imgproc.contourArea(m2);
            return (int) Math.ceil(area2 - area1);
        }
    };

    static {
        System.loadLibrary("NativeImageProcessor");
    }

    private Mat enhancedBitmap = new Mat();

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


                    Imgproc.Canny(gray0, gray, 60, 100);


                    // Dilate Canny output to remove potential holes between edge segments.
                    Imgproc.dilate(gray, gray, Mat.ones(new Size(3, 3), 0));
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

    private Filter getPaperFilter(float alpha, float beta) {
        Filter myFilter = new Filter();
//        com.zomato.photofilters.geometry.Point[] rgbKnots;
//        rgbKnots = new com.zomato.photofilters.geometry.Point[5];
//        rgbKnots[0] = new com.zomato.photofilters.geometry.Point(0,0);
//        rgbKnots[1] = new com.zomato.photofilters.geometry.Point(83,117);
//        rgbKnots[2] = new com.zomato.photofilters.geometry.Point(130,135);
//        rgbKnots[3] = new com.zomato.photofilters.geometry.Point(156,212);
//        rgbKnots[4] = new  com.zomato.photofilters.geometry.Point(255,255);
//        rgbKnots = new com.zomato.photofilters.geometry.Point[2];
//        rgbKnots[0] = new com.zomato.photofilters.geometry.Point(0,255);
//        rgbKnots[1] = new com.zomato.photofilters.geometry.Point(255,0);

//        myFilter.addSubFilter(new ContrastSubFilter(Math.abs(beta)-10.0f));
//        myFilter.addSubFilter(new ContrastSubFilter(alpha*1f));
//        myFilter.addSubFilter(new SaturationSubFilter(Math.abs(alpha)*0.008f));
        return myFilter;
    }

    public Filter getDarkerFilter() {
        Filter myFilter = new Filter();
        com.zomato.photofilters.geometry.Point[] rgbKnots;
        rgbKnots = new com.zomato.photofilters.geometry.Point[3];
        rgbKnots[0] = new com.zomato.photofilters.geometry.Point(0, 0);
        rgbKnots[1] = new com.zomato.photofilters.geometry.Point(143, 86);
        rgbKnots[2] = new com.zomato.photofilters.geometry.Point(255, 255);

        myFilter.addSubFilter(new ToneCurveSubFilter(rgbKnots, null, null, null));
        return myFilter;
    }

    public Mat convertScale(Mat img, Scalar alpha, Scalar beta) {
        Mat dst = new Mat();
        Mat newImg = new Mat();
        Core.multiply(img, alpha, dst);
        Core.add(dst, beta, newImg);
        newImg.get(0, 0)[0] = 0;
        newImg.get(255, 0)[0] = 255;
        Mat f = new Mat();
        newImg.assignTo(f, 1);
        return f;
    }

    public Mat brightnessAndContrastAuto(Mat image, float clipHistPercent) {
        clipHistPercent = 1f;
        Mat gray = new Mat();
        int histSize = 256;
        Mat original = new Mat();
        image.copyTo(original);
        Imgproc.cvtColor(image, gray, Imgproc.COLOR_RGB2GRAY);
        gray.convertTo(gray, CvType.CV_32F);
        List<Mat> planes = new ArrayList<>();
        Core.split(gray, planes);
        Mat hist = new Mat();
        Mat mask = new Mat();
        MatOfInt channel = new MatOfInt(0);
        MatOfFloat histRange = new MatOfFloat(0f, 256f);
        ArrayList<Mat> imageList = new ArrayList();
        imageList.add(gray);
        MatOfInt mHistSize = new MatOfInt(histSize);
        Imgproc.calcHist(planes, channel, mask, hist, mHistSize, histRange);

        ArrayList<Float> accumulator = new ArrayList();
        accumulator.add(Float.valueOf(String.valueOf(hist.get(0, 0)[0])));

        for (int i = 1; i < histSize; i++) {
            accumulator.add(accumulator.get(i - 1) + Float.valueOf(String.valueOf(hist.get(i, 0)[0])));
        }

        Float maximum = accumulator.get(accumulator.size() - 1);
        clipHistPercent *= (maximum / 100.0);
        clipHistPercent /= 2.0;

        Log.d("NativeClass", String.valueOf(accumulator));
        //left cut
        int minimum_gray = 0;
        while (accumulator.get(minimum_gray) < clipHistPercent) {
            minimum_gray += 1;
        }

        //right cut
        int maximum_gray = histSize - 1;
        while (accumulator.get(maximum_gray) >= (maximum - clipHistPercent)) {
            maximum_gray -= 1;
        }

        float alpha = (float) 255 / (float) (maximum_gray - minimum_gray);
        float beta = ((float) -minimum_gray) * alpha;

        Mat newHist = new Mat();
        MatOfFloat newRange = new MatOfFloat(minimum_gray, maximum_gray);
        Imgproc.calcHist(planes, channel, mask, newHist, mHistSize, newRange);
        Mat finale = new Mat();
        Log.d("NativeClass", String.valueOf(alpha));
        Log.d("NativeClass", String.valueOf(beta));
//        Core.convertScaleAbs(original,finale,alpha,beta);
        original.convertTo(finale, -1, alpha, beta);
//        Filter filter = getPaperFilter(alpha,beta);
//        Bitmap s = filter.processFilter(ImageUtils.matToBitmap(original));
        return finale;
    }

    public Bitmap getMagicColoredBitmap(Mat src, int iterations) {
        src.convertTo(src, CvType.CV_32FC3);
        Scan scanner = new Scan(src, 51, 66, 160);
        Mat scannedImg = scanner.scanImage(Scan.ScanMode.GCMODE);
        Mat auto = brightnessAndContrastAuto(scannedImg, 1f);
//        Mat d = brightnessAndContrastAuto(src,1);
        Size size;
        switch (iterations) {
            case 2:
                size = new Size(2, 2);
                break;
            case 3:
                size = new Size(3, 3);
                break;
            case 4:
                size = new Size(4, 4);
                break;
            default:
                size = new Size(1, 1);
                break;
        }
        Imgproc.cvtColor(auto, auto, Imgproc.COLOR_RGB2GRAY);
//        Imgproc.medianBlur(d,d,5);
//        Imgproc.GaussianBlur(d,d,new Size(3,3),0.0);
//        Imgproc.adaptiveThreshold(d,d,255,Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,Imgproc.THRESH_BINARY,7,4);
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
                size = new Size(2, 2);
                break;
            case 3:
                size = new Size(3, 3);
                break;
            case 4:
                size = new Size(4, 4);
                break;
            default:
                size = new Size(1, 1);
                break;
        }
        try {
            Imgproc.cvtColor(enhancedBitmap, enhancedBitmap, Imgproc.COLOR_RGB2GRAY);
        } catch (Exception e) {
        }
        Mat s = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, size);
        Imgproc.erode(enhancedBitmap, erode, s);
        return ImageUtils.matToBitmap(erode);
    }


//    public Bitmap getMagicColoredBitmap(Mat src,int iterations){
//        Bitmap bit = ImageUtils.matToBitmap(src);
//        Filter myFilter = getPaperFilter();
//        Bitmap outputImage = myFilter.processFilter(bit);
//        Mat gray = new Mat();
//        Mat thresh = new Mat();
//        Mat blur = new Mat();
////        Imgproc.cvtColor(src,src,Imgproc.COLOR_RGB2BGR);
//////        Imgproc.equalizeHist(src,gray);
////
////        ArrayList<Mat> dst = new ArrayList<>(3);
////        ArrayList<Mat> eqChannels = new ArrayList<>();
////        Core.split(src, dst);
////        for (int i=0;i<dst.size();i++){
////            Mat e = new Mat();
////            Imgproc.equalizeHist(dst.get(i),e);
////            eqChannels.add(e);
////        }
////        Core.merge(eqChannels,blur);
////        Imgproc.cvtColor(blur,blur,Imgproc.COLOR_BGR2RGB);
//        Imgproc.cvtColor(src,src,Imgproc.COLOR_RGB2HSV);
//
//
//        Size size;
//        switch (iterations){
//            case 2: size = new Size(2,2);
//                break;
//            case 3: size = new Size(3,3);
//                break;
//            case 4: size = new Size(4,4);
//                break;
//            default: size = new Size(1,1);
//                break;
//        }
//
////        gray = ImageUtils.bitmapToMat(outputImage);
////        Imgproc.cvtColor(gray,thresh,Imgproc.COLOR_RGB2GRAY);
////        Imgproc.adaptiveThreshold(gray,thresh,255,Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,Imgproc.THRESH_BINARY_INV,7,4);
//        Mat s = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, size);
//        Imgproc.erode(thresh,thresh,s);
//        return ImageUtils.matToBitmap(blur);
//    }

//    public Bitmap getMagicColoredBitmap(Mat src,int iterations){
//        double d = lap(src);
//        Log.d("getMagicColoredBitmap", String.valueOf(d));
//        Photo.fastNlMeansDenoising(src,src,30, 31, 9);
//        Mat gray = new Mat();
//        Mat thresh = new Mat();
//        Mat flat = new Mat(src.size(),CvType.CV_8U);
//        flat.setTo(new Scalar(168,148,118));
//        Size size;
//        switch (iterations){
//            case 2: size = new Size(2,2);
//                break;
//            case 3: size = new Size(3,3);
//                break;
//            case 4: size = new Size(4,4);
//                break;
//            default: size = new Size(1,1);
//                break;
//        }
//        Mat dest = new Mat();
////        src.convertTo(dest,-1,1.9,-80);
//        Imgproc.cvtColor(src,src,CvType.CV_8U);
////        Imgproc.bilateralFilter(src,dest,15,80,80,Core.BORDER_DEFAULT);\
//        if(d<100.0){
//            Imgproc.GaussianBlur(src,dest,new Size(9,9),0.0);
//
//        }else if(d<150.0){
//            Imgproc.GaussianBlur(src,dest,new Size(3,3),0.0);
//        }else{
//            src.copyTo(dest);
//        }
//        Imgproc.cvtColor(dest,gray,Imgproc.COLOR_RGB2GRAY);
//        Imgproc.adaptiveThreshold(gray,thresh,230,Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,Imgproc.THRESH_BINARY,7,2);
//        Core.multiply(thresh,flat,thresh);
////        Core.divide(thresh,flat,thresh);
//
//        thresh.convertTo(dest,-1, 1.9, -80);
//        Mat s = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, size);
//        Imgproc.erode(dest,dest,s);
////        if(d>13000.0){
////            Photo.fastNlMeansDenoising(dest,dest,30, 31, 9);
////            Photo.fastNlMeansDenoising(dest,dest,30, 31, 9);
////        }
//        return ImageUtils.matToBitmap(dest);
//    }

//    public Bitmap getMagicColoredBitmap(Mat src,int iterations){
//        Mat gray = new Mat();
//        Mat thresh = new Mat();
//        Mat temp =new Mat(src.rows(),src.cols(),src.type());
//        Scalar img = new Scalar(167, 148, 117);
//        temp.setTo(img);
//        Size size;
//        switch (iterations){
//            case 2: size = new Size(2,2);
//            break;
//            case 3: size = new Size(3,3);
//            break;
//            case 4: size = new Size(4,4);
//            break;
//            default: size = new Size(1,1);
//            break;
//        }
//        Mat dest = new Mat(src.rows(), src.cols(), src.type());
//        src.convertTo(dest,-1,1.9,-80);
//        Imgproc.medianBlur(src,src,7);
//        Imgproc.cvtColor(src,gray,Imgproc.COLOR_RGB2GRAY);
//        Core.multiply(src,temp,thresh);
////        Core.divide(thresh,temp,thresh);
////        Imgproc.GaussianBlur(gray,gray,new Size(3,3),0.0);
////        Imgproc.adaptiveThreshold(gray,thresh,255,Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,Imgproc.THRESH_BINARY,11,2);
//        Mat s = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, size);
//        Imgproc.erode(thresh,thresh,s);
//
////        Core.addWeighted(thresh,1.5,gray,-0.5,0,thresh);
////        Core.inRange(thresh, new Scalar(120,120,120),new Scalar(255,255,255),thresh);
////        Photo.fastNlMeansDenoising(thresh,thresh,50,07,21);
//
////        Mat contrast = new Mat();
////        contrast = contrast(thresh,2.2,20);
//
//        return ImageUtils.matToBitmap(temp);
//    }

    public Bitmap compress(Bitmap bitmap) {
        Mat src = ImageUtils.bitmapToMat(bitmap);
        Mat dest = new Mat();
        int nh = (int) (bitmap.getHeight() * (1200.0 / bitmap.getWidth()));
        Imgproc.resize(src, dest, new Size(1200, nh), 0, 0, Imgproc.INTER_CUBIC);
        return ImageUtils.matToBitmap(dest);
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

        if (maxCosine >= 0.3) {
            return false;
        }

        return true;
    }

}


