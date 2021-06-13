package com.aonescan.scanner.Fragments;

import android.content.ClipData;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.exifinterface.media.ExifInterface;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.aonescan.scanner.Adapter.CapturedImagesAdapter;
import com.aonescan.scanner.CameraActivty;
import com.aonescan.scanner.CostumClass.ImageFilePath;
import com.aonescan.scanner.CostumClass.LoadingDialog;
import com.aonescan.scanner.FileNameDialog;
import com.aonescan.scanner.Model.Images;
import com.aonescan.scanner.Model.ImagesListener;
import com.aonescan.scanner.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.play.core.tasks.Task;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;


public class FilesFragment extends Fragment implements ImagesListener, FileNameDialog.FileNameDialogListener {
    private static int CAMERA_REQUEST_CODE = 122;
    private static int GALLERY_REQUEST_CODE = 124;
    private static int CROP_INTENT_CODE = 1021;

    private RecyclerView recyclerView;
    private CapturedImagesAdapter madapter;
    private RecyclerView.LayoutManager layoutManager;
    private ArrayList<String> photosResult = new ArrayList<>();
    private ArrayList<Images> imagesObject = new ArrayList<>();

    private FloatingActionButton openCameraButton;
    private FloatingActionButton openGalleyButton;
    private FloatingActionButton startScanButton;
    private FloatingActionButton expandMoreButtons;

    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable runAnimation;

    private Animation fabOpen, fabClose, rotateForward, rotateBackward, arrowShake;
    private boolean isOpen = false;
    ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.START | ItemTouchHelper.END, 0) {
        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            int fromPosition = viewHolder.getAdapterPosition();
            int toPosition = target.getAdapterPosition();

            Collections.swap(imagesObject, fromPosition, toPosition);
            recyclerView.getAdapter().notifyItemMoved(fromPosition, toPosition);
            return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

        }
    };
    private ArrayList<String> sendToNextActivity = new ArrayList<>();
    private LinearLayout noImagesLayoutText;
    private FloatingActionButton deleteSelected;
    private File outputDirectory;
    private ViewPager2 mainViewPager;
    private File filePathForSaving;

    public FilesFragment() {
        // Required empty public constructor
    }

    public static FilesFragment newInstance(String param1, String param2) {
        FilesFragment fragment = new FilesFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        outputDirectory = new Output;
        mainViewPager = getActivity().findViewById(R.id.mainViewPager);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                photosResult = data.getStringArrayListExtra("photosResult");
                for (int i = 0; i < photosResult.size(); i++) {
                    imagesObject.add(new Images(photosResult.get(i)));
                }
                madapter.notifyDataSetChanged();
                noImagesLayoutText.setVisibility(View.GONE);
            }
            if (resultCode == RESULT_CANCELED) {

            }
        }
        if (requestCode == GALLERY_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                if (data.getData() != null) {
                    Uri mImageUri = data.getData();
                    String realPath = ImageFilePath.getPath(getActivity(), mImageUri);
                    imagesObject.add(new Images(realPath));
                    madapter.notifyDataSetChanged();
                }
                if (data.getClipData() != null) {
                    ClipData mClipData = data.getClipData();
                    for (int i = 0; i < mClipData.getItemCount(); i++) {
                        ClipData.Item item = mClipData.getItemAt(i);
                        Uri uri = item.getUri();
                        String realPath = ImageFilePath.getPath(getActivity(), uri);
                        imagesObject.add(new Images(realPath));
                    }
                    madapter.notifyDataSetChanged();
                }
                noImagesLayoutText.setVisibility(View.GONE);
            }
        }
        if (requestCode == CROP_INTENT_CODE) {
            if (resultCode == RESULT_OK) {
                String editedPhoto = data.getStringExtra("EditedResult");
                int pos = data.getIntExtra("resultSingleImgPos", 0);
                imagesObject.get(pos).image = editedPhoto;
                madapter.notifyDataSetChanged();
                Log.e("1021REQUEST", "" + editedPhoto);
            }
        }
    }

    private void setImagesView() {
        madapter = new CapturedImagesAdapter(getActivity(), imagesObject, this);
        recyclerView.setAdapter(madapter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_files, container, false);
        openCameraButton = view.findViewById(R.id.btn_openCamera);
        openGalleyButton = view.findViewById(R.id.btn_openGallery);
        startScanButton = view.findViewById(R.id.btn_start_scan);
        expandMoreButtons = view.findViewById(R.id.expandMoreButtons);

        fabOpen = AnimationUtils.loadAnimation(getActivity().getApplicationContext(),R.anim.fab_open);
        fabClose = AnimationUtils.loadAnimation(getActivity().getApplicationContext(),R.anim.fab_close);
        rotateForward = AnimationUtils.loadAnimation(getActivity().getApplicationContext(),R.anim.rotate_forward);
        rotateBackward = AnimationUtils.loadAnimation(getActivity().getApplicationContext(),R.anim.rotate_backward);
        arrowShake = AnimationUtils.loadAnimation(getActivity().getApplicationContext(),R.anim.arrow_shake);

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                // Your Code
                expandMoreButtons.startAnimation(arrowShake);
            }
        }, 500);

        recyclerView = view.findViewById(R.id.RV_capturedImages);
        layoutManager = new GridLayoutManager(getActivity(), 2);
        noImagesLayoutText = view.findViewById(R.id.txt_No_PDF_CREATED);
        deleteSelected = view.findViewById(R.id.btn_deleteSelected);
        recyclerView.setLayoutManager(layoutManager);
        // Disable Add Photos Text Here
        setImagesView();

        openCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent cameraIntent = new Intent(getActivity(), CameraActivty.class);
                startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
            }
        });

        openGalleyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent galleryIntent = new Intent();
                galleryIntent.setType("image/*");
                galleryIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(galleryIntent, "Select Pictures"), GALLERY_REQUEST_CODE);
            }
        });

        deleteSelected.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //get selected array list here using getSelectedImages method in adapter
                ArrayList<Images> selectedImages = new ArrayList<>();
                selectedImages = madapter.getSelectedImages();
//                for(Images image: imagesObject){
//                    if(selectedImages.contains(image)){
//                        //if equals
//                        image.isSelected = false;
//                    }
//                }
                ArrayList<Images> tempImageObject = new ArrayList<>(imagesObject);
                imagesObject.removeAll(selectedImages);
                madapter.notifyDataSetChanged();
                deleteSelected.setVisibility(View.GONE);
                Snackbar.make(view, "Undo Deletion of Recent Images", Snackbar.LENGTH_LONG).setAction("UNDO", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        imagesObject.clear();
                        imagesObject.addAll(tempImageObject);
                        madapter.notifyDataSetChanged();
                    }
                }).setActionTextColor(getResources().getColor(R.color.holo_blue_dark)).show();

            }
        });

        startScanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (imagesObject.size() > 0) {
                    filePathForSaving = new File(outputDirectory, "PDF_" + System.currentTimeMillis() + ".pdf");
                    String aAbsPath = filePathForSaving.getAbsolutePath();
                    String aFileName = aAbsPath.substring(aAbsPath.lastIndexOf("/") + 1);
                    openDialog(aFileName);
                } else {
                    Toast.makeText(getActivity(), "No Images Added!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        expandMoreButtons.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateFab();
            }
        });

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
        setImagesView();
        return view;
    }

    private void shakeArrow(){
        runAnimation = new Runnable() {
            @Override
            public void run() {
                expandMoreButtons.startAnimation(arrowShake);
            }
        };

        handler.postDelayed(runAnimation,5000);
    }

    private void animateFab(){
        if (isOpen) {
            expandMoreButtons.startAnimation(rotateBackward);
            openCameraButton.startAnimation(fabClose);
            openGalleyButton.startAnimation(fabClose);
            startScanButton.startAnimation(fabClose);
            openCameraButton.setClickable(false);
            openGalleyButton.setClickable(false);
            startScanButton.setClickable(false);
            isOpen = false;
            shakeArrow();
        }else{
            expandMoreButtons.startAnimation(rotateForward);
            openCameraButton.startAnimation(fabOpen);
            openGalleyButton.startAnimation(fabOpen);
            startScanButton.startAnimation(fabOpen);
            openCameraButton.setClickable(true);
            openGalleyButton.setClickable(true);
            startScanButton.setClickable(true);
            isOpen = true;
            handler.removeCallbacks(runAnimation);
        }
    }

    private void openDialog(String fileName) {
        FileNameDialog fileNameDialog = new FileNameDialog();
        fileNameDialog.setTargetFragment(FilesFragment.this, 1548);
        fileNameDialog.setDefaultName(fileName);
        fileNameDialog.show(getActivity().getSupportFragmentManager(), "File Name Dialog");
    }

    @Override
    public void applyText(String fileName) {
        String tempName = fileName;
        String sliceText = null;
        try {
            sliceText = fileName.substring(fileName.lastIndexOf("."));
        } catch (Exception e) {
            sliceText="cssadsadsadsakm";
        }
        Log.e("slicer",""+sliceText);
        if (!sliceText.equals(".pdf")) {
            tempName = tempName + ".pdf";
            Log.e("slicer",""+sliceText);
        }
        File file = new File(outputDirectory, tempName);
        if(file.exists()){
            Toast.makeText(getActivity().getApplicationContext(),"File Already Exits!",Toast.LENGTH_SHORT).show();
            openDialog(tempName);
        }else {
            createPDF(tempName);
        }
    }

    public void createPDF(String fileName) {
        if (getActivity() != null) {
            File file = new File(outputDirectory, fileName);
            final LoadingDialog dialog = new LoadingDialog(getActivity());
            dialog.startLoadingDialog();
            dialog.setLoadingText("Loading...");
            new Thread(() -> {
                PdfDocument document = new PdfDocument();
                Bitmap bitmapFile = null;
                int height = 1191;
                int width = 842;
                int reqH, reqW;
                reqW = width;

                for (Images image : imagesObject) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if((imagesObject.indexOf(image)+1)<imagesObject.size()){
                                dialog.setLoadingText("Converting To PDF "+(imagesObject.indexOf(image)+1)+"/"+imagesObject.size());
                            }else{
                                dialog.setLoadingText("Saving PDF "+(imagesObject.indexOf(image)+1)+"/"+imagesObject.size());
                            }
                        }
                    });
                    bitmapFile = BitmapFactory.decodeFile(image.image);
                    try {
                        bitmapFile = extractRotation(bitmapFile, image.image);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    reqH = width * bitmapFile.getHeight() / bitmapFile.getWidth();
                    Log.e("reqH", "=" + reqH);
                    if (reqH < height) {
                        bitmapFile = Bitmap.createScaledBitmap(bitmapFile, reqW, reqH-70, true);
                    } else {
                        reqH = height;
                        reqW = height * bitmapFile.getWidth() / bitmapFile.getHeight();
                        Log.e("reqW", "=" + reqW);
                        bitmapFile = Bitmap.createScaledBitmap(bitmapFile, reqW, reqH-70, true);
                    }
                    PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(842, 1191, 1).create();
                    PdfDocument.Page page = document.startPage(pageInfo);
                    Canvas canvas = page.getCanvas();

                    Log.e("PDF", "pdf = " + bitmapFile.getWidth() + "x" + bitmapFile.getHeight());

                    canvas.drawBitmap(bitmapFile, ((width / 2) - ((reqW) / 2)), ((height / 2) - ((reqH-70) / 2)), null);
                    document.finishPage(page);
                }
                FileOutputStream fos;
                try {
                    fos = new FileOutputStream(file);
                    document.writeTo(fos);
                    document.close();
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.dismissDialog();
                        try {
                            mainViewPager.setCurrentItem(1);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }).start();
        }


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

    @Override
    public void OnImagesAction(Boolean isSelected) {
        if (isSelected) {
            //Show Delete Button
            deleteSelected.setVisibility(View.VISIBLE);
        } else {
            //Dont Show Delete Button
            deleteSelected.setVisibility(View.GONE);
        }
    }

    public interface newPdfCreated {
        void onNewPdfCreated(boolean isCreated);
    }

}