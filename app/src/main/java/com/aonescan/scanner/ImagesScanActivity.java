package com.aonescan.scanner;


import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;

import com.aonescan.scanner.CostumClass.CustomDialog;
import com.aonescan.scanner.CostumClass.LoadingDialogTransparent;
import com.aonescan.scanner.CostumClass.OutputDirectory;
import com.aonescan.scanner.Helpers.ImageUtils;
import com.aonescan.scanner.Libraries.NativeClass;
import com.aonescan.scanner.Libraries.PolygonView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.xw.repo.BubbleSeekBar;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.InstallCallbackInterface;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dev.shreyaspatil.MaterialDialog.AbstractDialog;
import dev.shreyaspatil.MaterialDialog.interfaces.DialogInterface;

public class ImagesScanActivity extends AppCompatActivity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handlerExe = new Handler(Looper.getMainLooper());
    private final ArrayList<Bitmap> bitmapsForUndo = new ArrayList<>();
    private final BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            if (status == LoaderCallbackInterface.SUCCESS) {
                Log.i("MainActivity", "loaded successfully");
            } else {
                super.onManagerConnected(status);
                CustomDialog customDialog = new CustomDialog(ImagesScanActivity.this);
                customDialog.showMyDialog("Oops!, this is awkward", "You Device is Not Supported", false, "Ok", R.drawable.ic_done, new AbstractDialog.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        dialogInterface.dismiss();
                    }
                }, null, 0, null);
            }
        }

        @Override
        public void onPackageInstall(int operation, InstallCallbackInterface callback) {

        }
    };
    private FrameLayout holderImageCrop;
    private ShapeableImageView imageView;
    private PolygonView polygonView;
    private Bitmap selectedImageBitmap;
    private FloatingActionButton btnCropImage;
    private MaterialButton btnDoneEditing;
    private FloatingActionButton btnRotateImage;
    private FloatingActionButton btnEnhanceImage;
    private FloatingActionButton btnBlackWhite;
    private NativeClass nativeClass;
    private Bitmap singleImage;
    private int singleImagePos;
    private File outputDirectory;
    private int changeCount = 0;
    private int currentShowingIndex = 0;
    private FloatingActionButton btn_undoBitmap;
    private FloatingActionButton btn_redoBitmap;
    private final View.OnClickListener btnImageCropClick = v -> cropImage();
    private BubbleSeekBar bubbleSeekBar;
    private Bitmap originalImage;
    private MaterialButton resetToOriginal;
    private LinearLayout bubbleSeekBarLL;
    private boolean isEnhanced = false;
    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d("ImagesScanActivity", "Internal library not found. Using Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        } else {
            Log.d("ImagesScanActivity", "library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void cropImage() {
        final LoadingDialogTransparent dialog = new LoadingDialogTransparent(ImagesScanActivity.this);
        dialog.startLoadingDialog();
        changeCount++;
        if (polygonView.isValidShape(polygonView.getPoints())) {
            selectedImageBitmap = getCroppedImage();
        } else {
            Toast.makeText(getApplicationContext(), "Oops! cant crop this.", Toast.LENGTH_SHORT).show();
            polygonView.resetPaintColor();
        }
        addToUndoList();
        Bitmap scaledBitmap = scaledBitmap(selectedImageBitmap, holderImageCrop.getWidth(), holderImageCrop.getHeight());
        imageView.setImageBitmap(scaledBitmap);
        singleImage = selectedImageBitmap;
        dialog.dismissDialog();
        final LoadingDialogTransparent dialogRun = new LoadingDialogTransparent(ImagesScanActivity.this);
        dialogRun.startLoadingDialog();
        holderImageCrop.post(() -> {
            initializeCropping();
            runOnUiThread(dialogRun::dismissDialog);
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_scan);
        try {
            setTitle("Edit Image");
        }catch (Exception e) {
            e.printStackTrace();
        }
//        singleImage = BitmapFactory.decodeFile(getIntent().getStringExtra("singleImage"));
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, getApplicationContext(), mLoaderCallback);
        try {
            singleImage =
                    extractRotation(MediaStore.Images.Media.getBitmap(getApplicationContext().getContentResolver(),
                    FileProvider.getUriForFile(ImagesScanActivity.this, "com.aonescan.scanner.provider",
                            new File(getIntent().getStringExtra("singleImage")))));
            originalImage = singleImage.copy(singleImage.getConfig(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        singleImagePos = getIntent().getIntExtra("singleImagePosition", 0);
        isEnhanced = getIntent().getBooleanExtra("isEnhanced", false);

        holderImageCrop = findViewById(R.id.holderImageCrop);
        initializeElement();
    }


    @Override
    protected void onPause() {
        super.onPause();
        if(Build.VERSION.SDK_INT<Build.VERSION_CODES.LOLLIPOP){
            finish();
        }else {
            supportFinishAfterTransition();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(Build.VERSION.SDK_INT<Build.VERSION_CODES.LOLLIPOP){
            finish();
        }else {
            supportFinishAfterTransition();
        }
    }

    @Override
    public void onBackPressed() {

        if(changeCount>0){
            //showDialogBox for confirmation
            CustomDialog customDialog = new CustomDialog(this);
            customDialog.showMyDialog("Save Changes", "If you close without saving, the changes you have made will be lost.", true, "Save", R.drawable.ic_done, new AbstractDialog.OnClickListener() {
                @Override
                public void onClick(dev.shreyaspatil.MaterialDialog.interfaces.DialogInterface dialogInterface, int which) {
                    btnDoneEditing.performClick();
                }
            }, "Close", R.drawable.ic_close, new AbstractDialog.OnClickListener() {
                @Override
                public void onClick(dev.shreyaspatil.MaterialDialog.interfaces.DialogInterface dialogInterface, int which) {
                    dialogInterface.dismiss();
                    if(Build.VERSION.SDK_INT<Build.VERSION_CODES.LOLLIPOP){
                        finish();
                    }else {
                        supportFinishAfterTransition();
                    }
                }
            });
        }else{
            super.onBackPressed();
            if(Build.VERSION.SDK_INT<Build.VERSION_CODES.LOLLIPOP){
                finish();
            }else {
                supportFinishAfterTransition();
            }
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        selectedImageBitmap.recycle();
        recycleBitmapList(0);
    }

    private void recycleBitmapList(int i) {
        while (i < bitmapsForUndo.size()) {
            bitmapsForUndo.get(i).recycle();
            bitmapsForUndo.remove(i);
        }
    }

    private void addToUndoList() {
        try {
            recycleBitmapList(++currentShowingIndex);
            bitmapsForUndo.add(selectedImageBitmap.copy(selectedImageBitmap.getConfig(), true));
            setVisibility();
        } catch (OutOfMemoryError error) {
            bitmapsForUndo.get(1).recycle();
            bitmapsForUndo.remove(1);
            bitmapsForUndo.add(selectedImageBitmap.copy(selectedImageBitmap.getConfig(), true));
            setVisibility();
        }
    }

    private void setVisibility() {
        btn_undoBitmap.setEnabled(currentShowingIndex > 0);
        btn_redoBitmap.setEnabled(currentShowingIndex + 1 < bitmapsForUndo.size());
    }

    private Bitmap getUndoBitmap() {
        try {
            if (currentShowingIndex - 1 >= 0)
                currentShowingIndex -= 1;
            else currentShowingIndex = 0;

            return bitmapsForUndo.get(currentShowingIndex).copy(bitmapsForUndo.get(currentShowingIndex).getConfig(), true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private Bitmap getRedoBitmap() {
        try {
            if (currentShowingIndex + 1 <= bitmapsForUndo.size())
                currentShowingIndex += 1;
            else currentShowingIndex = bitmapsForUndo.size() - 1;

            return bitmapsForUndo.get(currentShowingIndex).copy(bitmapsForUndo.get(currentShowingIndex).getConfig(), true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    private void initializeElement() {
        nativeClass = new NativeClass();
        btnCropImage = findViewById(R.id.btn_crop_Image);
        btnDoneEditing = findViewById(R.id.btnSubmitPhoto);
        btnRotateImage = findViewById(R.id.btnRotateImage);
        btnEnhanceImage = findViewById(R.id.btnChangeColor);
        btnBlackWhite = findViewById(R.id.btnBlack_White);
        btn_undoBitmap = findViewById(R.id.undo);
        btn_redoBitmap = findViewById(R.id.redo);
        imageView = findViewById(R.id.imageView);

        polygonView = findViewById(R.id.polygonView);
        bubbleSeekBar = findViewById(R.id.bubbleSeekBar);
        resetToOriginal = findViewById(R.id.resetToOriginal);
        bubbleSeekBarLL = findViewById(R.id.bubbleSeekBarLL);
        bubbleSeekBarLL.animate().alpha(0.0f);
        setVisibility();
        outputDirectory = new OutputDirectory(this, ".images").getFileDir();
        holderImageCrop.post(this::initializeCropping);


        resetToOriginal.setOnClickListener(v -> {
            final LoadingDialogTransparent dialog = new LoadingDialogTransparent(ImagesScanActivity.this);
            dialog.startLoadingDialog();
            selectedImageBitmap = originalImage;
            singleImage = selectedImageBitmap;
            currentShowingIndex = 0;
            setVisibility();
            holderImageCrop.post(() -> {
                initializeCropping();
                runOnUiThread(dialog::dismissDialog);
            });
        });

        btnEnhanceImage.setOnClickListener(v -> {
            bubbleSeekBarLL.animate().alpha(1.0f);
            changeCount++;
            openButtonSeekBar();
            addToUndoList();
            final LoadingDialogTransparent dialog = new LoadingDialogTransparent(ImagesScanActivity.this);
            dialog.startLoadingDialog();
            executor.execute(() -> {
                if (!isEnhanced) {
                    selectedImageBitmap = nativeClass.getMagicColoredBitmap(ImageUtils.bitmapToMat(singleImage), 1);
                    Bitmap scaledBitmap = scaledBitmap(selectedImageBitmap, holderImageCrop.getWidth(), holderImageCrop.getHeight());
                    handlerExe.post(() -> runOnUiThread(() -> {
                        dialog.dismissDialog();
                        imageView.setImageBitmap(scaledBitmap);
                    }));
                } else {
                    handlerExe.post(() -> runOnUiThread(dialog::dismissDialog));
                }
            });
        });

        btnBlackWhite.setOnClickListener(v -> {
            changeCount++;
            closeButtonSeekBar();
            addToUndoList();
            final LoadingDialogTransparent dialog = new LoadingDialogTransparent(ImagesScanActivity.this);
            dialog.startLoadingDialog();
            AsyncTask.execute(() -> {
                selectedImageBitmap = nativeClass.getBlackAndWhiteBitmap(ImageUtils.bitmapToMat(singleImage));
                Bitmap scaledBitmap = scaledBitmap(selectedImageBitmap, holderImageCrop.getWidth(), holderImageCrop.getHeight());
                runOnUiThread(() -> {
                    dialog.dismissDialog();
                    imageView.setImageBitmap(scaledBitmap);
                });
            });
        });

        bubbleSeekBar.setOnProgressChangedListener(new BubbleSeekBar.OnProgressChangedListener() {
            @Override
            public void onProgressChanged(int progress, float progressFloat) {

            }

            @Override
            public void getProgressOnActionUp(int progress, float progressFloat) {
                int iteration = 0;
                switch (progress) {
                    case 25:
                        iteration = 1;
                        break;
                    case 50:
                        iteration = 2;
                        break;
                    case 75:
                        iteration = 3;
                        break;
                    case 100:
                        iteration = 4;
                        break;
                    default:
                        break;
                }
                final LoadingDialogTransparent dialog = new LoadingDialogTransparent(ImagesScanActivity.this);
                dialog.startLoadingDialog();
                int finalIteration = iteration;
                AsyncTask.execute(() -> {
                    Log.e("progress", "" + progress + " " + progressFloat);

//                        selectedImageBitmap = singleImage.copy(singleImage.getConfig(),true);
//                        Bitmap temp = singleImage.copy(singleImage.getConfig(),true);

                    selectedImageBitmap = nativeClass.getErodedImage(finalIteration);

                    addToUndoList();
                    Bitmap scaledBitmap = scaledBitmap(selectedImageBitmap, holderImageCrop.getWidth(), holderImageCrop.getHeight());
                    runOnUiThread(() -> {
                        dialog.dismissDialog();
                        imageView.setImageBitmap(scaledBitmap);
                    });
                });
            }

            @Override
            public void getProgressOnFinally(int progress, float progressFloat) {

            }
        });


        btnCropImage.setOnClickListener(btnImageCropClick);


        btn_undoBitmap.setOnClickListener(v -> {
            Bitmap temp = getUndoBitmap();
            if (temp != null) {
                final LoadingDialogTransparent dialog = new LoadingDialogTransparent(ImagesScanActivity.this);
                dialog.startLoadingDialog();
                selectedImageBitmap = temp;
                Bitmap scaledBitmap = scaledBitmap(selectedImageBitmap, holderImageCrop.getWidth(), holderImageCrop.getHeight());
                singleImage = selectedImageBitmap;
                imageView.setImageBitmap(scaledBitmap);
                setVisibility();
                holderImageCrop.post(() -> {
                    initializeCropping();
                    runOnUiThread(dialog::dismissDialog);
                });
            }
        });

        btn_redoBitmap.setOnClickListener(v -> {
            Bitmap temp = getRedoBitmap();
            if (temp != null) {
                final LoadingDialogTransparent dialog = new LoadingDialogTransparent(ImagesScanActivity.this);
                dialog.startLoadingDialog();
                selectedImageBitmap = temp;
                Bitmap scaledBitmap = scaledBitmap(selectedImageBitmap, holderImageCrop.getWidth(), holderImageCrop.getHeight());
                singleImage = selectedImageBitmap;
                imageView.setImageBitmap(scaledBitmap);
                setVisibility();
                holderImageCrop.post(() -> {
                    initializeCropping();
                    runOnUiThread(dialog::dismissDialog);
                });
            }
        });


        btnDoneEditing.setOnClickListener(v -> {
            cropImage();
            File EditedFile = new File(outputDirectory, "Image_" + System.currentTimeMillis() + ".jpg");
            String filename = EditedFile.getAbsolutePath();
            if (changeCount > 0) {
                final LoadingDialogTransparent dialog = new LoadingDialogTransparent(ImagesScanActivity.this);
                dialog.startLoadingDialog();
                new Thread(() -> {
                    try {
                        FileOutputStream out = new FileOutputStream(EditedFile);
                        selectedImageBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
                        out.flush();
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("EditedResult", filename);
                    resultIntent.putExtra("resultSingleImgPos", singleImagePos);
                    setResult(RESULT_OK, resultIntent);
                    runOnUiThread(() -> {
                        dialog.dismissDialog();
                        if(Build.VERSION.SDK_INT<Build.VERSION_CODES.LOLLIPOP){
                            finish();
                        }else {
                            supportFinishAfterTransition();
                        }
                    });
                }).start();
            } else {
                Intent resultIntent = new Intent();
                setResult(RESULT_CANCELED, resultIntent);
                if(Build.VERSION.SDK_INT<Build.VERSION_CODES.LOLLIPOP){
                    finish();
                }else {
                    supportFinishAfterTransition();
                }
            }

        });

        btnRotateImage.setOnClickListener(v -> {
            changeCount++;
            final LoadingDialogTransparent dialog = new LoadingDialogTransparent(ImagesScanActivity.this);
            dialog.startLoadingDialog();
            selectedImageBitmap = ImageUtils.rotateBitmap(selectedImageBitmap, -90);
            addToUndoList();
            Bitmap scaledBitmap = scaledBitmap(selectedImageBitmap, holderImageCrop.getWidth(), holderImageCrop.getHeight());
            imageView.setImageBitmap(scaledBitmap);
            singleImage = selectedImageBitmap;
            holderImageCrop.post(() -> {
                initializeCropping();
                runOnUiThread(dialog::dismissDialog);
            });
        });


    }

    private void closeButtonSeekBar() {
        bubbleSeekBar.setVisibility(View.INVISIBLE);
    }

    private void openButtonSeekBar() {
        bubbleSeekBar.setVisibility(View.VISIBLE);
    }


    private void initializeCropping() {

        selectedImageBitmap = singleImage;

        Bitmap scaledBitmap = scaledBitmap(selectedImageBitmap, holderImageCrop.getWidth(), holderImageCrop.getHeight());
        imageView.setImageBitmap(scaledBitmap);

        Bitmap tempBitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
        Map<Integer, PointF> pointFs = getEdgePoints(tempBitmap);
        Log.e("pointF", "" + pointFs);
        polygonView.setPoints(pointFs);
        polygonView.setVisibility(View.VISIBLE);

        int padding = (int) getResources().getDimension(R.dimen.scanPadding);

        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(tempBitmap.getWidth() + 2 * padding, tempBitmap.getHeight() + 2 * padding);
        layoutParams.gravity = Gravity.CENTER;

        polygonView.setLayoutParams(layoutParams);

    }

    private Bitmap extractRotation(Bitmap scaledBitmap) throws IOException {
        ExifInterface ei = new ExifInterface(getIntent().getStringExtra("singleImage"));
        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

        Bitmap rotatedBitmap = null;
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                rotatedBitmap = ImageUtils.rotateBitmap(scaledBitmap, 90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                rotatedBitmap = ImageUtils.rotateBitmap(scaledBitmap, 180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                rotatedBitmap = ImageUtils.rotateBitmap(scaledBitmap, 270);
                break;
            default:
                rotatedBitmap = scaledBitmap;
        }
        return rotatedBitmap;
    }


    protected Bitmap getCroppedImage() {

        Map<Integer, PointF> points = polygonView.getPoints();

        float xRatio = (float) selectedImageBitmap.getWidth() / imageView.getWidth();
        float yRatio = (float) selectedImageBitmap.getHeight() / imageView.getHeight();

        float x1 = (Objects.requireNonNull(points.get(0)).x) * xRatio;
        float x2 = (Objects.requireNonNull(points.get(1)).x) * xRatio;
        float x3 = (Objects.requireNonNull(points.get(2)).x) * xRatio;
        float x4 = (Objects.requireNonNull(points.get(3)).x) * xRatio;
        float y1 = (Objects.requireNonNull(points.get(0)).y) * yRatio;
        float y2 = (Objects.requireNonNull(points.get(1)).y) * yRatio;
        float y3 = (Objects.requireNonNull(points.get(2)).y) * yRatio;
        float y4 = (Objects.requireNonNull(points.get(3)).y) * yRatio;

        return nativeClass.getScannedBitmap(selectedImageBitmap, x1, y1, x2, y2, x3, y3, x4, y4);

    }

    private Bitmap scaledBitmap(Bitmap bitmap, int width, int height) {
        Matrix m = new Matrix();
        m.setRectToRect(new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight()), new RectF(0, 0, width, height), Matrix.ScaleToFit.CENTER);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
    }

    private Map<Integer, PointF> getEdgePoints(Bitmap tempBitmap) {
        List<PointF> pointFs = getContourEdgePoints(tempBitmap);
        Map<Integer, PointF> orderedPoints = orderedValidEdgePoints(tempBitmap, pointFs);
        Log.e("OrderedPoints", "" + orderedPoints);
        return orderedPoints;
    }

    private List<PointF> getContourEdgePoints(Bitmap tempBitmap) {
        List<PointF> result = new ArrayList<>();
        try {
            MatOfPoint2f point2f = nativeClass.getPoint(tempBitmap);
            List<Point> points = Arrays.asList(point2f.toArray());

            for (int i = 0; i < points.size(); i++) {
                result.add(new PointF(((float) points.get(i).x), ((float) points.get(i).y)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private Map<Integer, PointF> getOutlinePoints(Bitmap tempBitmap) {
        Map<Integer, PointF> outlinePoints = new HashMap<>();
        outlinePoints.put(0, new PointF(0, 0));
        outlinePoints.put(1, new PointF(tempBitmap.getWidth(), 0));
        outlinePoints.put(2, new PointF(0, tempBitmap.getHeight()));
        outlinePoints.put(3, new PointF(tempBitmap.getWidth(), tempBitmap.getHeight()));
        Log.e("OutlinePoints", "" + outlinePoints);
        return outlinePoints;
    }

    private Map<Integer, PointF> orderedValidEdgePoints(Bitmap tempBitmap, List<PointF> pointFs) {
        Map<Integer, PointF> orderedPoints = polygonView.getOrderedPoints(pointFs);
        if (!polygonView.isValidShape(orderedPoints)) {
            orderedPoints = getOutlinePoints(tempBitmap);
        }
        return orderedPoints;
    }
}
