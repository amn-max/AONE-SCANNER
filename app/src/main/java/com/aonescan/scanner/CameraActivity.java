package com.aonescan.scanner;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
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
import com.google.android.material.imageview.ShapeableImageView;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class CameraActivity extends AppCompatActivity {
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
    private Button cameraFlip;
    private PreviewView viewFinder;
    private Preview preview;
    private View cameraFlash;
    private Button camera_Capture_Button;
    private Handler handler = new Handler();
    private ArrayList<String> ListImagesAbsPath = new ArrayList<String>();
    private TextView noOfImages;
    private Button sumbitPhotos;
    private OrientationEventListener mOrientationListener;
    private ImageAnalysis imageAnalysis = null;
    private ShapeableImageView viewStamp;
    private SoundPool soundPool;
    private LinearLayout leftShutterAnim;
    private LinearLayout rightShutterAnim;
    private AudioAttributes audioAttributes;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Handler handlerExe = new Handler(Looper.getMainLooper());
    private Runnable focusingTOInvisible = new Runnable() {
        @Override
        public void run() {
            focusView.setVisibility(View.INVISIBLE);
        }
    };


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
            getSupportActionBar().hide();
        } catch (NullPointerException e) {

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
            camera_Capture_Button.setEnabled(false);
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

            noOfImages.setText(Integer.toString(ListImagesAbsPath.size() + 1));
        });

        cameraFlip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                    cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
                } else {
                    cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                }
                try {
                    cameraProvider.unbindAll();
                    startCamera();
                } catch (Exception e) {

                }
            }
        });

        cameraFlash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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

                }
            }


        });

        sumbitPhotos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (camera_Capture_Button.isEnabled()) {
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("photosResult", ListImagesAbsPath);
                    setResult(RESULT_OK, resultIntent);
                    if (ListImagesAbsPath.size() <= 0) {
                        Toast.makeText(CameraActivity.this, "No Images Captured", Toast.LENGTH_SHORT).show();
                    }
                    finish();
                } else {
                    Toast.makeText(CameraActivity.this, "Wait for camera to take action", Toast.LENGTH_SHORT).show();
                }
            }
        });


        outputDirectory = new OutputDirectory(this, ".images").getFileDir();
        cameraExecutor = Executors.newSingleThreadExecutor();
        startCamera();

        mOrientationListener = new OrientationEventListener(getApplicationContext()) {
            @Override
            public void onOrientationChanged(int orientation) {
                int rotation = 0;
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
        final int screenOrientation = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getOrientation();
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
                ImageAnalysis.Analyzer analyzer = new ImageAnalysis.Analyzer() {
                    @Override
                    public void analyze(@NonNull ImageProxy image) {
                        Log.e("analyzer", "working");
                        image.close();
                    }
                };
                imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor(), analyzer);
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
                            ListenableFuture future = cControl.startFocusAndMetering(action);

                            future.addListener(() -> {
                                handler.postDelayed(focusingTOInvisible, 3000);
                                try {
                                    FocusMeteringResult result = (FocusMeteringResult) future.get();
                                    if (result.isFocusSuccessful()) {
                                        focusView.setBackground(ContextCompat.getDrawable(getBaseContext(), R.drawable.ic_focus_green));
                                    }
                                } catch (Exception e) {

                                }
                            }, cameraExecutor);


                            return true;
                        } else {

                            return false;
                        }
                    });


                } catch (Exception e) {
                    Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show();
                }
                pinchToZoom();
                setUpZoomSlider();
            } catch (ExecutionException | InterruptedException e) {

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
                    currentZoomRatio = ZoomRatio.getValue().getZoomRatio();
                } catch (NullPointerException e) {

                }
                float linearValue = ZoomRatio.getValue().getLinearZoom();
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

    protected Bitmap rotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    private void takePhotos() {
        focusView.setVisibility(View.INVISIBLE);
        File photoFile = new File(outputDirectory, "Image_" + System.currentTimeMillis() + ".jpg");

        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(getBaseContext()), new ImageCapture.OnImageSavedCallback() {

            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                String absPath = photoFile.getAbsolutePath();
                try {
                    Bitmap bitmap = BitmapFactory.decodeFile(absPath);
                    bitmap = extractRotation(bitmap, absPath);
                    Bitmap finalBitmap = bitmap;

                    class SaveAndPreview extends AsyncTask<Void, Void, Void> {

                        @Override
                        protected Void doInBackground(Void... voids) {
                            FileOutputStream fileOutputStream = null;
                            try {
                                fileOutputStream = new FileOutputStream(photoFile);
                                finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, fileOutputStream);
                                fileOutputStream.flush();
                                fileOutputStream.close();
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            return null;
                        }

                        @Override
                        protected void onPostExecute(Void unused) {
                            super.onPostExecute(unused);
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
                    }

                    SaveAndPreview saveAndPreview = new SaveAndPreview();
                    saveAndPreview.executeOnExecutor(executor);


                } catch (IOException e) {
                    e.printStackTrace();
                }

                ListImagesAbsPath.add(absPath);
                noOfImages.setVisibility(View.VISIBLE);
                camera_Capture_Button.setEnabled(true);


            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Toast.makeText(getBaseContext(), "Error Saving Image" + photoFile.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    @Override
    public void onBackPressed() {
        if (camera_Capture_Button.isEnabled()) {
            super.onBackPressed();
            setResult(RESULT_CANCELED);
        }
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
        mOrientationListener.disable();
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleGestureDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }
}
