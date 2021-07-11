package com.aonescan.scanner;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.ScaleGestureDetector;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.FocusMeteringResult;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.MeteringPointFactory;
import androidx.camera.core.Preview;
import androidx.camera.core.SurfaceOrientedMeteringPointFactory;
import androidx.camera.core.ZoomState;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.exifinterface.media.ExifInterface;
import androidx.lifecycle.LiveData;

import com.aonescan.scanner.CostumClass.OutputDirectory;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.common.util.concurrent.ListenableFuture;
import com.scijoker.observablelist.ObservableArrayList;
import com.scijoker.observablelist.ObservableList;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class CameraActivity extends AppCompatActivity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ObservableArrayList<String> ListImagesAbsPath = new ObservableArrayList<>();
    public ScaleGestureDetector scaleGestureDetector;
    int soundShutter = 0;
    private ImageCapture imageCapture = null;
    private ExecutorService cameraExecutor;
    private File outputDirectory;
    private CameraControl cControl;
    private CameraInfo cInfo;
    private ProcessCameraProvider cameraProvider;
    private int flashMode = ImageCapture.FLASH_MODE_OFF;
    private Camera camera;
    private CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
    private SeekBar zoomBar;
    private View focusView;
    private final Runnable focusingTOInvisible = new Runnable() {
        @Override
        public void run() {
            focusView.setVisibility(View.INVISIBLE);
        }
    };
    private Button cameraFlip;
    private PreviewView viewFinder;
    private Preview preview;
    private View cameraFlash;
    private Button camera_Capture_Button;
    private TextView noOfImages;
    private Button sumbitPhotos;
    private OrientationEventListener mOrientationListener;
    private ImageAnalysis imageAnalysis = null;
    private ShapeableImageView viewStamp;
    private SoundPool soundPool;
    private LinearLayout leftShutterAnim;
    private LinearLayout rightShutterAnim;
    private AudioAttributes audioAttributes;
    private Runnable glideRunnable;
    private Boolean isOnSingleCaptureMode;
    private MaterialButton removeRetakeSingleImage;
    private void playShutter() {
        try {
            soundPool.play(soundShutter, 1, 1, 1, 0, 1);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        try {
            Objects.requireNonNull(getSupportActionBar()).hide();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        try {
            isOnSingleCaptureMode = getIntent().getBooleanExtra("CAPTURE_ONE_IMAGE",false);
        }catch (Exception e){
            e.printStackTrace();
        }

        zoomBar = findViewById(R.id.zoomBar);
        zoomBar.setMax(100);
        zoomBar.setProgress(0);
        focusView = findViewById(R.id.focus);
        cameraFlip = findViewById(R.id.camera_flip);
        cameraFlash = findViewById(R.id.camera_flash);
        viewFinder = findViewById(R.id.viewFinder1);
        camera_Capture_Button = findViewById(R.id.camera_capture_button);
        noOfImages = findViewById(R.id.txt_numberOfImages);
        sumbitPhotos = findViewById(R.id.btn_submit_photos);
        viewStamp = findViewById(R.id.imgViewStamp);
        removeRetakeSingleImage = findViewById(R.id.removeRetakeSingleImage);
        leftShutterAnim = findViewById(R.id.leftShutterAnim);
        rightShutterAnim = findViewById(R.id.rightShutterAnim);
        leftShutterAnim.setAlpha(0.0f);
        rightShutterAnim.setAlpha(0.0f);
        audioAttributes = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setAudioAttributes(audioAttributes)
                .build();

        soundShutter = soundPool.load(this, R.raw.camerashutter, 1);
        camera_Capture_Button.setOnClickListener(v -> {
            playShutter();
            takePhotos();
            leftShutterAnim.animate().setDuration(200).alpha(0.4f).translationX(leftShutterAnim.getWidth()).setListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {

                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    leftShutterAnim.animate().setDuration(200).alpha(0.4f).translationX(-leftShutterAnim.getWidth());
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
            rightShutterAnim.animate().setDuration(200).alpha(0.4f).translationX(-rightShutterAnim.getWidth()).setListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {

                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    rightShutterAnim.animate().setDuration(200).alpha(0.4f).translationX(rightShutterAnim.getWidth());
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
        });

        cameraFlip.setOnClickListener(v -> {
            if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
            } else {
                cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
            }
            try {
                cameraProvider.unbindAll();
                startCamera();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        ListImagesAbsPath.addOnChangeListener(new ObservableList.OnChangeListener<String>() {
            @Override
            public void onChanged(ObservableList.EventType eventType, List<ObservableList.Event<String>> list) {
                noOfImages.setText(String.valueOf(ListImagesAbsPath.size()));
                if(isOnSingleCaptureMode && ListImagesAbsPath.size()==1){
                    camera_Capture_Button.setEnabled(false);
                    camera_Capture_Button.setVisibility(View.INVISIBLE);
                    removeRetakeSingleImage.setVisibility(View.VISIBLE);
                    removeRetakeSingleImage.setEnabled(true);
                }else if (isOnSingleCaptureMode && ListImagesAbsPath.size()>1){
                    Toast.makeText(getApplicationContext(),"Cannot take more photos when retaking",Toast.LENGTH_SHORT);
                }
            }
        });

        removeRetakeSingleImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ListImagesAbsPath.clear();
                Bitmap b = null;
                Glide.with(getApplicationContext()).load(b).into(viewStamp);
                noOfImages.setText(String.valueOf(ListImagesAbsPath.size()));
                camera_Capture_Button.setEnabled(true);
                camera_Capture_Button.setVisibility(View.VISIBLE);
                removeRetakeSingleImage.setVisibility(View.INVISIBLE);
                removeRetakeSingleImage.setEnabled(false);
            }
        });
        cameraFlash.setOnClickListener(v -> {
            switch (flashMode) {
                case ImageCapture.FLASH_MODE_OFF:
                    flashMode = ImageCapture.FLASH_MODE_ON;
                    cameraFlash.setBackground(ContextCompat.getDrawable(getBaseContext(), R.drawable.ic_flash_on));
                    break;
                case ImageCapture.FLASH_MODE_ON:
                    flashMode = ImageCapture.FLASH_MODE_AUTO;
                    cameraFlash.setBackground(ContextCompat.getDrawable(getBaseContext(), R.drawable.ic_flash_auto));
                    break;
                default:
                    flashMode = ImageCapture.FLASH_MODE_OFF;
                    cameraFlash.setBackground(ContextCompat.getDrawable(getBaseContext(), R.drawable.ic_flash_off));
                    break;
            }
            try {
                imageCapture.setFlashMode(flashMode);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        sumbitPhotos.setOnClickListener(view -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("photosResult", ListImagesAbsPath);
            setResult(RESULT_OK, resultIntent);
            if (ListImagesAbsPath.size() <= 0) {
                Toast.makeText(CameraActivity.this, "No Images Captured", Toast.LENGTH_SHORT).show();
            }
            finish();
        });


        outputDirectory = new OutputDirectory(this, ".images").getFileDir();
        cameraExecutor = Executors.newSingleThreadExecutor();
        startCamera();

        mOrientationListener = new OrientationEventListener(getApplicationContext()) {
            @Override
            public void onOrientationChanged(int orientation) {
                int rotation;
                if (orientation >= 45 && orientation < 135) {
                    rotation = Surface.ROTATION_270;
                } else if (orientation >= 135 && orientation < 225) {
                    rotation = Surface.ROTATION_180;
                } else if (orientation >= 225 && orientation < 315) {
                    rotation = Surface.ROTATION_90;
                } else {
                    rotation = Surface.ROTATION_0;
                }
                try {
                    imageCapture.setTargetRotation(rotation);
                    imageAnalysis.setTargetRotation(rotation);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

    }


    @Override
    protected void onStart() {
        super.onStart();
        mOrientationListener.enable();
    }

    public int getScreenOrientation(Context context) {
        final int screenOrientation = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
        if (screenOrientation >= 45 && screenOrientation < 135) {
            return Surface.ROTATION_270;
        } else if (screenOrientation >= 135 && screenOrientation < 225) {
            return Surface.ROTATION_180;
        } else if (screenOrientation >= 225 && screenOrientation < 315) {
            return Surface.ROTATION_90;
        } else {
            return Surface.ROTATION_0;
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                //bind Camera Preview to Surface provider ie:viewFinder in my case
                preview = new Preview.Builder().build();
                imageAnalysis = new ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).setTargetRotation(getScreenOrientation(this)).build();
                ImageAnalysis.Analyzer analyzer = ImageProxy::close;
                imageAnalysis.setAnalyzer(cameraExecutor, analyzer);
                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .setTargetRotation(getScreenOrientation(this))
                        .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                        .setFlashMode(flashMode)
                        .build();
                Preview.SurfaceProvider surfaceProvider = viewFinder.getSurfaceProvider();
                preview.setSurfaceProvider(surfaceProvider);

                try {
                    cameraProvider.unbindAll();

                    camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, imageAnalysis);
                    cControl = camera.getCameraControl();
                    cInfo = camera.getCameraInfo();

                    //AutoFocus Every X Seconds
                    MeteringPointFactory AFfactory = new SurfaceOrientedMeteringPointFactory((float) viewFinder.getWidth(), (float) viewFinder.getHeight());
                    float centerWidth = (float) viewFinder.getWidth() / 2;
                    float centerHeight = (float) viewFinder.getHeight() / 2;
                    MeteringPoint AFautoFocusPoint = AFfactory.createPoint(centerWidth, centerHeight);
                    try {
                        FocusMeteringAction action = new FocusMeteringAction.Builder(AFautoFocusPoint, FocusMeteringAction.FLAG_AF).setAutoCancelDuration(1, TimeUnit.SECONDS).build();
                        cControl.startFocusAndMetering(action);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    //AutoFocus CameraX
                    viewFinder.setOnTouchListener((v, event) -> {
                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            handler.removeCallbacks(focusingTOInvisible);
                            focusView.setBackground(ContextCompat.getDrawable(getBaseContext(), R.drawable.ic_focus));
                            focusView.setVisibility(View.VISIBLE);
                            return true;
                        } else if (event.getAction() == MotionEvent.ACTION_UP) {
                            MeteringPointFactory factory = new SurfaceOrientedMeteringPointFactory((float) viewFinder.getWidth(), (float) viewFinder.getHeight());
                            MeteringPoint autoFocusPoint = factory.createPoint(event.getX(), event.getY());
                            FocusMeteringAction action = new FocusMeteringAction.Builder(autoFocusPoint, FocusMeteringAction.FLAG_AF).setAutoCancelDuration(5, TimeUnit.SECONDS).build();
                            ListenableFuture<FocusMeteringResult> future = cControl.startFocusAndMetering(action);

                            future.addListener(() -> {
                                handler.postDelayed(focusingTOInvisible, 3000);
                                try {
                                    FocusMeteringResult result = future.get();
                                    if (result.isFocusSuccessful()) {
                                        focusView.setBackground(ContextCompat.getDrawable(getBaseContext(), R.drawable.ic_focus_green));
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }, cameraExecutor);


                            return true;
                        } else {

                            return false;
                        }
                    });

                    pinchToZoom();
                    setUpZoomSlider();
                } catch (Exception e) {
                    Toast.makeText(this, "Camera Bind Failed", Toast.LENGTH_SHORT).show();
                }

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }


    private void setUpZoomSlider() {
        zoomBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float mat = (float) (progress) / (100);
                cControl.setLinearZoom(mat);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }



    private void pinchToZoom() {
        //Pinch Zoom Camera
        ScaleGestureDetector.SimpleOnScaleGestureListener listener = new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                LiveData<ZoomState> ZoomRatio = cInfo.getZoomState();
                float currentZoomRatio = 0;
                try {
                    currentZoomRatio = Objects.requireNonNull(ZoomRatio.getValue()).getZoomRatio();
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
                float linearValue = Objects.requireNonNull(ZoomRatio.getValue()).getLinearZoom();
                float delta = detector.getScaleFactor();
                cControl.setZoomRatio(currentZoomRatio * delta);
                float mat = (linearValue) * (100);
                zoomBar.setProgress((int) mat);
                return true;
            }
        };

        scaleGestureDetector = new ScaleGestureDetector(getBaseContext(), listener);
    }

    private Bitmap extractRotation(Bitmap scaledBitmap, String image) throws IOException {
        ExifInterface ei = new ExifInterface(image);
        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

        Bitmap rotatedBitmap;
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

    protected Bitmap rotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    private void takePhotos() {
        focusView.setVisibility(View.INVISIBLE);
        String fileName = "Image_" + System.currentTimeMillis() + ".jpg";
        File photoFile = new File(outputDirectory, fileName);

        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(outputFileOptions, cameraExecutor, new ImageCapture.OnImageSavedCallback() {

            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                String absPath = photoFile.getAbsolutePath();
                try {
                    Bitmap bitmap = BitmapFactory.decodeFile(absPath);
                    bitmap = extractRotation(bitmap, absPath);
                    Bitmap finalBitmap = bitmap;
                    cameraExecutor.execute(() -> {
                        OutputStream fileOutputStream;
                        try {
                            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.Q){
                                ContentValues values = new ContentValues();
                                values.put(MediaStore.Images.Media.DISPLAY_NAME,fileName);
                                values.put(MediaStore.Images.Media.MIME_TYPE,"image/jpeg");
                                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);
                                Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);
                                fileOutputStream = getContentResolver().openOutputStream(uri);
                            }else{
                                fileOutputStream = new FileOutputStream(photoFile);
                            }
                            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, fileOutputStream);
                            fileOutputStream.flush();
                            fileOutputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        glideRunnable = new Runnable() {
                            @Override
                            public void run() {
                                Glide.with(CameraActivity.this).load(finalBitmap).thumbnail(0.1f).circleCrop().listener(new RequestListener<Drawable>() {
                                    @Override
                                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                        return false;
                                    }

                                    @Override
                                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                        return false;
                                    }
                                }).error(R.drawable.ic_error).centerCrop().into(viewStamp);
                            }
                        };
                        handler.post(glideRunnable);
                    });

                } catch (IOException e) {
                    e.printStackTrace();
                }
                handler.post(() -> {
                    ListImagesAbsPath.add(absPath);
                    noOfImages.setVisibility(View.VISIBLE);
                });
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Toast.makeText(getBaseContext(), "Error Saving Image" + photoFile.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }

    @Override
    protected void onStop() {
        super.onStop();
        cameraExecutor.shutdown();
        handler.removeCallbacks(glideRunnable);
        mOrientationListener.disable();
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleGestureDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }
}
