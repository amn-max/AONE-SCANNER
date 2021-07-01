package com.aonescan.scanner.Helpers;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.util.Log;

import androidx.exifinterface.media.ExifInterface;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ImageUtils {
    public static Bitmap getScaledDownBitmap(Bitmap bitmap, int threshold, boolean isNecessaryToKeepOrig) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int newWidth = width;
        int newHeight = height;

        if (width > height && width > threshold) {
            newWidth = threshold;
            newHeight = (int) (height * (float) newWidth / width);
        }

        if (width > height && width <= threshold) {
            //the bitmap is already smaller than our required dimension, no need to resize it
            return bitmap;
        }

        if (width < height && height > threshold) {
            newHeight = threshold;
            newWidth = (int) (width * (float) newHeight / height);
        }

        if (width < height && height <= threshold) {
            //the bitmap is already smaller than our required dimension, no need to resize it
            return bitmap;
        }

        if (width == height && width > threshold) {
            newWidth = threshold;
            newHeight = newWidth;
        }

        if (width == height && width <= threshold) {
            //the bitmap is already smaller than our required dimension, no need to resize it
            return bitmap;
        }

        return getResizedBitmap(bitmap, newWidth, newHeight, isNecessaryToKeepOrig);
    }

    public static Bitmap compress(Bitmap bitmap) {
        Mat src = ImageUtils.bitmapToMat(bitmap);
        Mat dest = new Mat();
        int nh = (int) (bitmap.getHeight() * (1200.0 / bitmap.getWidth()));
        Imgproc.resize(src, dest, new Size(1200, nh), 0, 0, Imgproc.INTER_CUBIC);
        return ImageUtils.matToBitmap(dest);
    }

    public static List<Float> brightnessAndContrastAuto(Mat image, float clipHistPercent) {
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

        ArrayList values = new ArrayList();
        values.add(alpha);
        values.add(beta);

        return values;
    }

    private static Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight, boolean isNecessaryToKeepOrig) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
        if (!isNecessaryToKeepOrig) {
            bm.recycle();
        }
        return resizedBitmap;
    }

    private Bitmap extractRotation(Bitmap scaledBitmap, String image) throws IOException {
        ExifInterface ei = new ExifInterface(image);
        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

        Bitmap rotatedBitmap = null;
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                rotatedBitmap = rotateBitmap(scaledBitmap, 90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                rotatedBitmap = rotateBitmap(scaledBitmap, 180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                rotatedBitmap = rotateBitmap(scaledBitmap, 270);
                break;
            default:
                rotatedBitmap = scaledBitmap;
        }
        return rotatedBitmap;
    }

    public static Bitmap rotateBitmap(Bitmap original, int angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(original, 0, 0, original.getWidth(), original.getHeight(), matrix, true);
    }

    public static Mat bitmapToMat(Bitmap bitmap) {
        Mat mat = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8U, new Scalar(4));
        Bitmap bitmap32 = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bitmap32, mat);
        return mat;
    }

    public static Bitmap matToBitmap(Mat mat) {
        Bitmap bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat, bitmap);
        return bitmap;
    }

//    public void animateImageHeight(ImageView img, Bitmap bitmap){
//        float bitmapWidth = bitmap.getWidth();
//        float bitmapHeight = bitmap.getHeight();
//        float imageViewWidth = img.getWidth();
//        float imageViewHeight = img.getHeight();
//        int rotation = imageRotation % 360;
//        int newRotation = (rotation+90);
//
//        int newViewHeight;
//        float imageScale;
//        float newImageScale;
//
//        switch (rotation){
//            case 0:
//            case 180:
//                imageScale = imageViewWidth/bitmapWidth;
//                newImageScale = imageViewWidth/bitmapHeight;
//                newViewHeight = (int) (bitmapWidth*newImageScale);
//                break;
//            case 90:
//            case 270:
//                imageScale = imageViewWidth/bitmapHeight;
//                newImageScale = imageViewWidth/bitmapWidth;
//                newViewHeight = (int) (bitmapHeight*newImageScale);
//                break;
//            default:throw new UnsupportedOperationException("rotation can 0, 90, 180 or 270. "+ rotation + " is unsupported")
//        }
//        AccelerateDecelerateInterpolator interpolator = new AccelerateDecelerateInterpolator();
//        long duration = (long) getResources().getInteger(android.R.integer.config_mediumAnimTime);
//        ValueAnimator valueAnimator = ValueAnimator.ofFloat(0f, 1f);
//        valueAnimator.addListener(new AnimatorListenerAdapter() {
//            @Override
//            public void onAnimationStart(Animator animation) {
//                super.onAnimationStart(animation);
//                btnRotateImage.setClickable(false);
//            }
//
//            @Override
//            public void onAnimationEnd(Animator animation) {
//                super.onAnimationEnd(animation);
//                imageRotation = newRotation % 360;
//                btnRotateImage.setClickable(true);
//            }
//        });
//        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
//            @Override
//            public void onAnimationUpdate(ValueAnimator animation) {
//                float animVal = (float) animation.getAnimatedValue();
//                float complementaryAnimVal = 1 - animVal;
//                int animatedHeight = (int) (complementaryAnimVal * imageViewHeight + animVal * newViewHeight);
//                int animatedScale = (int) (complementaryAnimVal * imageScale + animVal * newImageScale);
//                int animatedRotation = (int) (complementaryAnimVal * rotation + animVal * newRotation);
//            }
//        });
//    }

}
