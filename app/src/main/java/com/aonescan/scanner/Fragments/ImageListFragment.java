package com.aonescan.scanner.Fragments;

import android.content.ClipData;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.exifinterface.media.ExifInterface;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.aonescan.scanner.Adapter.CapturedImagesAdapter;
import com.aonescan.scanner.CameraActivity;
import com.aonescan.scanner.CostumClass.FileNameDialog;
import com.aonescan.scanner.CostumClass.FileUtils;
import com.aonescan.scanner.CostumClass.OutputDirectory;
import com.aonescan.scanner.CostumClass.ScanNameDialog;
import com.aonescan.scanner.MainActivity;
import com.aonescan.scanner.MainViewModel;
import com.aonescan.scanner.Model.Images;
import com.aonescan.scanner.Model.ImagesListener;
import com.aonescan.scanner.R;
import com.aonescan.scanner.database.Project;
import com.aonescan.scanner.database.ProjectDBClient;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject;
import com.tom_roush.pdfbox.util.PDFBoxResourceLoader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;


public class ImageListFragment extends Fragment implements ImagesListener, FileNameDialog.FileNameDialogListener, ScanNameDialog.ScanNameDialogListener {
    private static int CAMERA_REQUEST_CODE = 122;
    private static int GALLERY_REQUEST_CODE = 124;
    private static int CROP_INTENT_CODE = 1021;
    private Executor executor = new ThreadPoolExecutor(5, 128, 1,
            TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
    private RecyclerView recyclerView;
    private CapturedImagesAdapter madapter;
    private RecyclerView.LayoutManager layoutManager;
    private ArrayList<String> photosResult = new ArrayList<>();
    private ArrayList<Images> imagesObject = new ArrayList<>();
    private Handler handlerExe = new Handler(Looper.getMainLooper());
    private FloatingActionButton openCameraButton;
    private FloatingActionButton openGalleyButton;
    private FloatingActionButton startScanButton;
    private FloatingActionButton expandMoreButtons;
    private Project currProject;
    private Spinner enhanceSpinner;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable runAnimation;
    private FileNameDialog fileNameDialog;
    private MaterialButton btn_bulk_enhance;
    private FloatingActionButton btn_check_all_img;
    private String deleteTimeStamp = new String("");
    ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.START | ItemTouchHelper.END, 0) {
        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            int fromPosition = viewHolder.getAdapterPosition();
            int toPosition = target.getAdapterPosition();

            Collections.swap(imagesObject, fromPosition, toPosition);
            recyclerView.getAdapter().notifyItemMoved(fromPosition, toPosition);
            ArrayList<String> paths = new ArrayList<>();
            for (Images image : imagesObject) {
                paths.add(image.getImage());
            }
            currProject.setImagePaths(paths);
            deleteTimeStamp = "";
            UpdateImagePath updateImagePath = new UpdateImagePath();
            updateImagePath.executeOnExecutor(executor);
            return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

        }
    };
    private MainViewModel viewModel;
    private Animation fabOpen, fabClose, rotateForward, rotateBackward, arrowShake;
    private boolean isOpen = false;
    private ArrayList<String> sendToNextActivity = new ArrayList<>();
    private LinearLayout noImagesLayoutText;
    private FloatingActionButton deleteSelected;
    private File outputDirectory;
    private ViewPager2 mainViewPager;
    private Bundle arguments;
    private File filePathForSaving;
    private MaterialButton btn_change_title_name;
    private ScanNameDialog scanNameDialog;
    private RelativeLayout image_list_parent_layout;

    public ImageListFragment() {
        // Required empty public constructor
    }

    public static ImageListFragment newInstance() {
        ImageListFragment fragment = new ImageListFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        outputDirectory = new OutputDirectory(getContext(), "PDF").getFileDir();
//        ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
//                new ActivityResultContracts.StartActivityForResult(),
//                new ActivityResultCallback<ActivityResult>() {
//                    @Override
//                    public void onActivityResult(ActivityResult result) {
//
//                    }
//                }
//        );

        getActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (madapter.getSelectedImages().size() > 0) {
                    madapter.unSelectAll();
                } else {
//                    setEnabled(false);

                    getActivity().onBackPressed();
                }
            }
        });

    }

    public void insertOnUpdate(ArrayList<String> paths) {

        if (!deleteTimeStamp.isEmpty()) {
            Project p = new Project(deleteTimeStamp, paths);
            long s = ProjectDBClient.getInstance(getActivity().getApplicationContext())
                    .getProjectDB()
                    .projectDao()
                    .insert(p);

        } else {
            String ts = String.valueOf(System.currentTimeMillis());
            Project p = new Project(ts, paths);
            long s = ProjectDBClient.getInstance(getActivity().getApplicationContext())
                    .getProjectDB()
                    .projectDao()
                    .insert(p);
        }


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
//                updateImagesPath
                madapter.notifyDataSetChanged();
                UpdateImagePath updateImagePath = new UpdateImagePath();
                updateImagePath.executeOnExecutor(executor);
                updateImageDatabase(photosResult);
                noImagesLayoutText.setVisibility(View.GONE);
            }
            if (resultCode == RESULT_CANCELED) {

            }
        }
        if (requestCode == GALLERY_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                if (data.getData() != null) {
                    Uri mImageUri = data.getData();
                    String realPath = FileUtils.getPath(mImageUri, getContext());
                    imagesObject.add(new Images(realPath));
                    madapter.notifyDataSetChanged();
                    UpdateImagePath updateImagePath = new UpdateImagePath();
                    updateImagePath.executeOnExecutor(executor);
                    ArrayList singlePhotoArray = new ArrayList<String>();
                    singlePhotoArray.add(realPath);
                    updateImageDatabase(singlePhotoArray);
                }
                if (data.getClipData() != null) {
                    ClipData mClipData = data.getClipData();
                    ArrayList photoArray = new ArrayList<String>();
                    for (int i = 0; i < mClipData.getItemCount(); i++) {
                        ClipData.Item item = mClipData.getItemAt(i);
                        Uri uri = item.getUri();
                        String realPath = FileUtils.getPath(uri, getContext());
                        photoArray.add(realPath);
                        imagesObject.add(new Images(realPath));
                    }
                    madapter.notifyDataSetChanged();

                    UpdateImagePath updateImagePath = new UpdateImagePath();
                    updateImagePath.executeOnExecutor(executor);
                    updateImageDatabase(photoArray);
                }
                noImagesLayoutText.setVisibility(View.GONE);
            }
        }
        if (requestCode == CROP_INTENT_CODE) {
            if (resultCode == RESULT_OK) {
                String editedPhoto = data.getStringExtra("EditedResult");
                int pos = data.getIntExtra("resultSingleImgPos", 0);
                imagesObject.get(pos).setImage(editedPhoto);
                madapter.notifyDataSetChanged();

                UpdateImagePath updateImagePath = new UpdateImagePath();
                updateImagePath.executeOnExecutor(executor);
                ArrayList singlePhotoArray = new ArrayList<String>();
                singlePhotoArray.add(editedPhoto);
                updateImageDatabase(singlePhotoArray);
            }
        }

        if (requestCode == 1548) {
            if (requestCode == RESULT_OK) {
                Log.d("DilaogFIleName", "Filename");
            }
        }
    }

    private void updateImageDatabase(ArrayList<String> paths) {
        Images image = new Images();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < paths.size(); i++) {
                    image.setId(currProject.getId());
                    image.setImage(paths.get(i));
                    image.setIsEnhanced(false);
                    ProjectDBClient.getInstance(getActivity().getApplicationContext())
                            .getProjectDB()
                            .imagesDao()
                            .insert(image);
                }
            }
        });
    }

    private void setImagesView() {
        madapter = new CapturedImagesAdapter(getActivity(),
                imagesObject,
                this,
                (MainActivity) getActivity(),
                image_list_parent_layout,
                Integer.valueOf(arguments.getString("historyId", "")),
                executor);
        recyclerView.setAdapter(madapter);

    }

    @Override
    public void onActivityCreated(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        String historyName = arguments.getString("historyTitle");
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                viewModel = new ViewModelProvider(getActivity()).get(MainViewModel.class);
                viewModel.updateActionBarTitle(historyName);
            }
        });
    }

    void changeTitle(String fileName) {
        class ChangeTitle extends AsyncTask<Void, Void, Void> {

            @Override
            protected Void doInBackground(Void... voids) {
                String historyName = arguments.getString("historyTitle");
                String historyId = arguments.getString("historyId", "");

                ProjectDBClient.getInstance(getActivity().getApplicationContext())
                        .getProjectDB()
                        .projectDao()
                        .updateProjectName(fileName, currProject.getId());


                return null;
            }

            @Override
            protected void onPostExecute(Void unused) {
                super.onPostExecute(unused);
                ProjectDBClient.getInstance(getActivity().getApplicationContext())
                        .getProjectDB()
                        .projectDao()
                        .getProjectById(currProject.getId())
                        .observe(getViewLifecycleOwner(), new Observer<Project>() {
                            @Override
                            public void onChanged(Project project) {
                                viewModel.updateActionBarTitle(project.getProjectName());
                            }
                        });
            }
        }
        ChangeTitle changeTitle = new ChangeTitle();
        changeTitle.executeOnExecutor(executor);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_image_list, container, false);
        openCameraButton = view.findViewById(R.id.btn_openCamera);
        openGalleyButton = view.findViewById(R.id.btn_openGallery);
        startScanButton = view.findViewById(R.id.btn_start_scan);
        expandMoreButtons = view.findViewById(R.id.expandMoreButtons);
        image_list_parent_layout = view.findViewById(R.id.image_list_parent_layout);
        btn_change_title_name = view.findViewById(R.id.btn_change_title_name);
        fabOpen = AnimationUtils.loadAnimation(getActivity().getApplicationContext(), R.anim.fab_open);
        fabClose = AnimationUtils.loadAnimation(getActivity().getApplicationContext(), R.anim.fab_close);
        rotateForward = AnimationUtils.loadAnimation(getActivity().getApplicationContext(), R.anim.rotate_forward);
        rotateBackward = AnimationUtils.loadAnimation(getActivity().getApplicationContext(), R.anim.rotate_backward);
        arrowShake = AnimationUtils.loadAnimation(getActivity().getApplicationContext(), R.anim.arrow_shake);
//        enhanceSpinner = view.findViewById(R.id.enhance_spinner);
//        ArrayAdapter<CharSequence> enhanceAdapter = ArrayAdapter
//                .createFromResource(getContext(),
//                        R.array.text_enhance_values,
//                        R.layout.enhance_spinner_item);
//        enhanceAdapter.setDropDownViewResource(R.layout.enhace_spinner_dropdown_item);
//        enhanceSpinner.setAdapter(enhanceAdapter);
//
//        enhanceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
////                switch (position){
////                    case 0:
////                }
//                btn_bulk_enhance.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        Log.d("Spinner", String.valueOf(position));
//
////                        BulkEnhance bulkEnhance = new BulkEnhance();
////                        bulkEnhance.execute();
//                    }
//                });
//            }
//
//            @Override
//            public void onNothingSelected(AdapterView<?> parent) {
//
//            }
//        });

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                // Your Code
                expandMoreButtons.startAnimation(arrowShake);
            }
        }, 500);

        arguments = getArguments();
        String historyId = arguments.getString("historyId", "");

        if (!historyId.isEmpty()) {
            FetchProjectById fetchProjectById = new FetchProjectById();
            fetchProjectById.setId(Integer.valueOf(historyId));
            fetchProjectById.executeOnExecutor(executor);
        } else {
            FetchPhotosFromDb fetchPhotosFromDb = new FetchPhotosFromDb();
            fetchPhotosFromDb.executeOnExecutor(executor);
        }

        recyclerView = view.findViewById(R.id.RV_capturedImages);
        recyclerView.setHasFixedSize(true);
        layoutManager = new GridLayoutManager(getActivity(), 2);
        noImagesLayoutText = view.findViewById(R.id.txt_No_PDF_CREATED);
//        deleteSelected = view.findViewById(R.id.btn_deleteSelected);
        recyclerView.setLayoutManager(layoutManager);
        // Disable Add Photos Text Here
        setImagesView();

        btn_change_title_name.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanNameDialog = new ScanNameDialog();
                scanNameDialog.setTargetFragment(ImageListFragment.this, 1648);
                String historyName = arguments.getString("historyTitle");
                scanNameDialog.setDefaultName(historyName);

                scanNameDialog.show(getActivity().getSupportFragmentManager(), "Scan Name");
                scanNameDialog.setTitle("Enter new scan name");
                scanNameDialog.setEditTextHint("Scan name");
            }
        });


        openCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent cameraIntent = new Intent(getActivity(), CameraActivity.class);
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


    private void shakeArrow() {
        runAnimation = new Runnable() {
            @Override
            public void run() {
                expandMoreButtons.startAnimation(arrowShake);
            }
        };

        handler.postDelayed(runAnimation, 5000);
    }

    private void animateFab() {
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
        } else {
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
        fileNameDialog = new FileNameDialog();
        fileNameDialog.setTargetFragment(ImageListFragment.this, 1548);
        fileNameDialog.setDefaultName(fileName);
        fileNameDialog.show(getActivity().getSupportFragmentManager(), "File Name Dialog");
        fileNameDialog.setEditTextHint("Filename");
        fileNameDialog.setTitle("Enter new pdf filename");
    }

    @Override
    public void applyScanText(String fileName) {
        changeTitle(fileName);
    }

    @Override
    public void onScanDialog(boolean showDialog) {
        if (showDialog) {
            scanNameDialog.dismiss();
        }
    }

    @Override
    public void onDialog(boolean showDialog) {
        if (showDialog) {
            fileNameDialog.dismiss();
        }
    }

    @Override
    public void applyText(String fileName) {
        String tempName = fileName;
        String sliceText = null;
        try {
            sliceText = fileName.substring(fileName.lastIndexOf("."));
        } catch (Exception e) {
            sliceText = "cssadsadsadsakm";
        }
        Log.e("slicer", "" + sliceText);
        if (!sliceText.equals(".pdf")) {
            tempName = tempName + ".pdf";
            Log.e("slicer", "" + sliceText);
        }
        File file = new File(outputDirectory, tempName);
        if (file.exists()) {
            Toast.makeText(getActivity().getApplicationContext(), "File Already Exits!", Toast.LENGTH_SHORT).show();
            openDialog(tempName);
        } else {
            createPDF(tempName);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        PDFBoxResourceLoader.init(getActivity().getApplicationContext());
        if (madapter.getItemCount() > 0) {
            noImagesLayoutText.setVisibility(View.GONE);
        } else {
            noImagesLayoutText.setVisibility(View.VISIBLE);
        }
    }

    public void createPDF(String fileName) {
        if (getActivity() != null) {
            File file = new File(outputDirectory, fileName);
            Snackbar bar = Snackbar.make(image_list_parent_layout, "Please wait! Do not close window while processing images", Snackbar.LENGTH_INDEFINITE)
                    .setActionTextColor(getContext().getResources().getColor(R.color.light_orange));
            ViewGroup contentLay = (ViewGroup) bar.getView()
                    .findViewById(com.google.android.material.R.id.snackbar_text).getParent();
            ProgressBar item = new ProgressBar(getContext());
            item.getIndeterminateDrawable().setColorFilter(getContext().getResources().getColor(R.color.orange), PorterDuff.Mode.MULTIPLY);
            contentLay.addView(item);
            bar.show();
            PDDocument document = new PDDocument();
            class SavePdf extends AsyncTask<Void, Void, Void> {
                @Override
                protected Void doInBackground(Void... voids) {
                    for (Images image : imagesObject) {
                        try {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if ((imagesObject.indexOf(image) + 1) < imagesObject.size()) {
                                        bar.setText("Converting To PDF " + (imagesObject.indexOf(image) + 1) + "/" + imagesObject.size());
                                    } else {
                                        bar.setText("Saving PDF " + (imagesObject.indexOf(image) + 1) + "/" + imagesObject.size());
                                    }
                                }
                            });
                            Log.d("FileExtenstion", image.getImage());
                            PDImageXObject pdImage = PDImageXObject.createFromFile(image.getImage(), document);
                            PDPage page = new PDPage(PDRectangle.A4);
                            document.addPage(page);
                            PDPageContentStream contentStream = new PDPageContentStream(document, page, true, true);
                            int nh = (int) (pdImage.getHeight() * (Float.valueOf(PDRectangle.A4.getWidth() - 40) / pdImage.getWidth()));
                            int nw = (int) (pdImage.getWidth() * (Float.valueOf(nh) / pdImage.getHeight()));
                            int heightThresh = (int) ((PDRectangle.A4.getHeight() / 2) - (nh / 2));
                            int widthThresh = (int) ((PDRectangle.A4.getWidth() / 2) - (nw / 2));
                            contentStream.drawImage(pdImage, widthThresh, heightThresh, nw, nh);
                            contentStream.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    bar.dismiss();
                                    Toast.makeText(getContext(), "Pdf creation error - code 24", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Void unused) {
                    super.onPostExecute(unused);
                    try {
                        document.save(file);
                        document.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            bar.dismiss();
                            Snackbar bar1 = Snackbar.make(image_list_parent_layout, "Pdf generated! You can check on recent pdf in menu down below 😊", Snackbar.LENGTH_SHORT)
                                    .setActionTextColor(getContext().getResources().getColor(R.color.light_orange));
                            bar1.show();
                        }
                    }, 10);
                }
            }
            SavePdf savePdf = new SavePdf();
            savePdf.executeOnExecutor(executor);
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
//        if (isSelected) {
//            //Show Delete Button
//            deleteSelected.setVisibility(View.VISIBLE);
//            btn_check_all_img.setVisibility(View.VISIBLE);
//        } else {
//            //Dont Show Delete Button
//            deleteSelected.setVisibility(View.INVISIBLE);
//            btn_check_all_img.setVisibility(View.INVISIBLE);
//        }
    }

    public interface newPdfCreated {
        void onNewPdfCreated(boolean isCreated);
    }

    class FetchProjectById extends AsyncTask<Void, Void, Project> {
        private int id;
        private Project list;

        public void setId(int id) {
            this.id = id;
        }

        @Override
        protected Project doInBackground(Void... voids) {
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            ProjectDBClient.getInstance(getActivity().getApplicationContext())
                    .getProjectDB()
                    .projectDao()
                    .getProjectById(id)
                    .observe(getViewLifecycleOwner(), new Observer<Project>() {
                        @Override
                        public void onChanged(Project project) {
                            currProject = project;
                            ArrayList<String> imagePathsDb = project.getImagePaths();
                            imagesObject.clear();
                            for (String path : imagePathsDb) {
                                Images image = new Images(path);
                                imagesObject.add(image);
                            }
                            if (project.getImagePaths().size() > 0) {
                                noImagesLayoutText.setVisibility(View.GONE);
                            } else {
                                noImagesLayoutText.setVisibility(View.VISIBLE);
                            }
                            viewModel.updateActionBarTitle(project.getProjectName());
                        }
                    });
        }
    }

    class UpdateImagePath extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            ArrayList<String> paths = new ArrayList<String>();
            paths.clear();
            for (Images image : imagesObject) {
                paths.add(image.getImage());
            }
            if (paths.size() >= 0 && !deleteTimeStamp.isEmpty()) {
                insertOnUpdate(paths);
            } else {
                ProjectDBClient.getInstance(getActivity().getApplicationContext())
                        .getProjectDB()
                        .projectDao()
                        .update(currProject.getId(), paths);

            }
            return null;
        }
    }

    class FetchPhotosFromDb extends AsyncTask<Void, Void, Project> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            ProjectDBClient.getInstance(getActivity().getApplicationContext())
                    .getProjectDB()
                    .projectDao()
                    .getLatestProject()
                    .observe(getViewLifecycleOwner(), new Observer<Project>() {
                        @Override
                        public void onChanged(Project project) {
                            currProject = project;
                        }
                    });
        }

        @Override
        protected Project doInBackground(Void... voids) {
//            Project list = ProjectDBClient.getInstance(getActivity().getApplicationContext())
//                    .getProjectDB()
//                    .projectDao()
//                    .getLatestProject();
            return currProject;
        }

        @Override
        protected void onPostExecute(Project project) {
            super.onPostExecute(project);
            currProject = project;
            ArrayList<String> imagePathsDb = project.getImagePaths();
            imagesObject.clear();
            for (String path : imagePathsDb) {
                Images image = new Images(path);
                imagesObject.add(image);
            }
            madapter.notifyDataSetChanged();
            noImagesLayoutText.setVisibility(View.GONE);
            viewModel.updateActionBarTitle(project.getProjectName());
        }
    }

}