package com.aonescan.scanner.Fragments;

import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.net.Uri;
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
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.aonescan.scanner.Adapter.CapturedImagesAdapter;
import com.aonescan.scanner.CameraActivity;
import com.aonescan.scanner.CostumClass.FileNameDialog;
import com.aonescan.scanner.CostumClass.FileUtils;
import com.aonescan.scanner.CostumClass.GridSpacingItemDecoration;
import com.aonescan.scanner.CostumClass.OutputDirectory;
import com.aonescan.scanner.MainActivity;
import com.aonescan.scanner.MainViewModel;
import com.aonescan.scanner.Model.Images;
import com.aonescan.scanner.Model.ImagesListener;
import com.aonescan.scanner.R;
import com.aonescan.scanner.database.Project;
import com.aonescan.scanner.database.ProjectDBClient;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject;
import com.tom_roush.pdfbox.util.PDFBoxResourceLoader;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static android.app.Activity.RESULT_OK;


public class ImageListFragment extends Fragment implements ImagesListener, FileNameDialog.FileNameDialogListener, LifecycleObserver {
//    private static final int CAMERA_REQUEST_CODE = 122;
//    private static final int GALLERY_REQUEST_CODE = 124;
//    private static final int CROP_INTENT_CODE = 1021;
    private final Executor executor = new ThreadPoolExecutor(5, 128, 1,
            TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    private final ArrayList<Images> imagesObject = new ArrayList<>();
    private final Handler handlerExe = new Handler(Looper.getMainLooper());
    private RecyclerView recyclerView;
    private CapturedImagesAdapter madapter;
    private RecyclerView.LayoutManager layoutManager;
    private ArrayList<String> photosResult = new ArrayList<>();
    private FloatingActionButton openCameraButton;
    private FloatingActionButton openGalleyButton;
    private FloatingActionButton startScanButton;
    private FloatingActionButton expandMoreButtons;
    private Project currProject;
    private Runnable runAnimation;
    private FileNameDialog fileNameDialog;
    private String deleteTimeStamp = "";
    private ActivityResultLauncher<Intent> cameraRequestLauncher;
    private ActivityResultLauncher<String[]> galleryRequestLauncher;
    private ActivityResultLauncher<Intent> cropRequestLauncher;
    ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.START | ItemTouchHelper.END, 0) {
        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            int fromPosition = viewHolder.getAdapterPosition();
            int toPosition = target.getAdapterPosition();

            Collections.swap(imagesObject, fromPosition, toPosition);
            if(madapter!=null){
                madapter.notifyItemMoved(fromPosition, toPosition);
            }
            ArrayList<String> paths = new ArrayList<>();
            for (Images image : imagesObject) {
                paths.add(image.getImage());
            }
            currProject.setImagePaths(paths);
            deleteTimeStamp = "";
            updateImagePath();
            return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

        }
    };
    private MainViewModel viewModel;
    private Animation fabOpen, fabClose, rotateForward, rotateBackward, arrowShake;
    private boolean isOpen = false;
    private LinearLayout noImagesLayoutText;
    private File outputDirectory;
    private Bundle arguments;
    private File filePathForSaving;
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

        requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (madapter.getSelectedImages().size() > 0) {
                    madapter.unSelectAll();
                } else {
//                    setEnabled(false);

                    requireActivity().onBackPressed();
                }
            }
        });


    }

    public void insertOnUpdate(ArrayList<String> paths) {

        if (!deleteTimeStamp.isEmpty()) {
            Project p = new Project(deleteTimeStamp, paths);
            long s = ProjectDBClient.getInstance(requireActivity().getApplicationContext())
                    .getProjectDB()
                    .projectDao()
                    .insert(p);

        } else {
            String ts = String.valueOf(System.currentTimeMillis());
            Project p = new Project(ts, paths);
            long s = ProjectDBClient.getInstance(requireActivity().getApplicationContext())
                    .getProjectDB()
                    .projectDao()
                    .insert(p);
        }


    }

    @Override
    public void onAttach(@NonNull @NotNull Context context) {
        super.onAttach(context);
        requireActivity().getLifecycle().addObserver(this);
        cameraRequestLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        photosResult = result.getData().getStringArrayListExtra("photosResult");
                        int sizeBefore = imagesObject.size();
                        for (int i = 0; i < photosResult.size(); i++) {
                            imagesObject.add(new Images(photosResult.get(i)));
                        }
                        int sizeAfter = imagesObject.size();
                        if(sizeAfter-sizeBefore>0){
                            try {
                                madapter.notifyItemRangeInserted(sizeBefore+1,sizeAfter);
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                        updateImagePath();
                        updateImageDatabase(photosResult);
                        noImagesLayoutText.setVisibility(View.GONE);
                    }
                }
        );

        galleryRequestLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenMultipleDocuments(),
                result -> {
                    Log.d("CameraCallback","Receviced");
                    if(!result.isEmpty()){
                            ArrayList<String> photoArray = new ArrayList<>();
                            int sizeBefore = imagesObject.size();
                            for (int i = 0; i < result.size(); i++) {
                                Uri uri = result.get(i);
                                String realPath = FileUtils.getPath(uri, getContext());
                                photoArray.add(realPath);
                                imagesObject.add(new Images(realPath));
                            }
                            int sizeAfter = imagesObject.size();
                            if(sizeAfter-sizeBefore>0){
                                try {
                                    madapter.notifyItemRangeInserted(sizeBefore+1,sizeAfter);
                                }catch (Exception e){
                                    e.printStackTrace();
                                }
                            }
                            updateImagePath();
                            updateImageDatabase(photoArray);
                    }
                    noImagesLayoutText.setVisibility(View.GONE);
                }
        );


        cropRequestLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Log.d("CropResult","Recevied");
                    if (result.getResultCode() == RESULT_OK && result.getData()!=null) {
                        String editedPhoto = result.getData().getStringExtra("EditedResult");
                        int pos = result.getData().getIntExtra("resultSingleImgPos", 0);
                        imagesObject.get(pos).setImage(editedPhoto);
                        madapter.notifyItemChanged(pos);
                        updateImagePath();
                        ArrayList<String> singlePhotoArray = new ArrayList<>();
                        singlePhotoArray.add(editedPhoto);
                        updateImageDatabase(singlePhotoArray);
                    }
                }
        );
    }

    private void updateImageDatabase(ArrayList<String> paths) {
        Images image = new Images();
        executor.execute(() -> {
            for (int i = 0; i < paths.size(); i++) {
                image.setId(currProject.getId());
                image.setImage(paths.get(i));
                image.setIsEnhanced(false);
                ProjectDBClient.getInstance(requireActivity().getApplicationContext())
                        .getProjectDB()
                        .imagesDao()
                        .insert(image);
            }
        });
    }

    private void setImagesView() {
        Log.d("ImageListFragment", String.valueOf(imagesObject.size()));
        madapter = new CapturedImagesAdapter(getActivity(),
                imagesObject,
                this,
                (MainActivity) getActivity(),
                image_list_parent_layout,
                Integer.parseInt(arguments.getString("historyId", "")),
                executor,
                cropRequestLauncher);
        recyclerView.setAdapter(madapter);

    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    public void onCreated(){
        requireActivity().getLifecycle().removeObserver(this);
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String historyName = arguments.getString("historyTitle");
        requireActivity().runOnUiThread(() -> {
            viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
            viewModel.updateActionBarTitle(historyName);
        });
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
        fabOpen = AnimationUtils.loadAnimation(requireActivity().getApplicationContext(), R.anim.fab_open);
        fabClose = AnimationUtils.loadAnimation(requireActivity().getApplicationContext(), R.anim.fab_close);
        rotateForward = AnimationUtils.loadAnimation(requireActivity().getApplicationContext(), R.anim.rotate_forward);
        rotateBackward = AnimationUtils.loadAnimation(requireActivity().getApplicationContext(), R.anim.rotate_backward);
        arrowShake = AnimationUtils.loadAnimation(requireActivity().getApplicationContext(), R.anim.arrow_shake);

        handlerExe.postDelayed(() -> {
            // Your Code
            expandMoreButtons.startAnimation(arrowShake);
        }, 500);

        arguments = getArguments();
        String historyId = requireArguments().getString("historyId", "");

        if (!historyId.isEmpty()) {
            fetchProjectById(Integer.parseInt(historyId));
        } else {
            fetchPhotosFromDb();
        }

        recyclerView = view.findViewById(R.id.RV_capturedImages);
        layoutManager = new GridLayoutManager(getActivity(), 2);
        noImagesLayoutText = view.findViewById(R.id.txt_No_PDF_CREATED);
//        deleteSelected = view.findViewById(R.id.btn_deleteSelected);
        recyclerView.setLayoutManager(layoutManager);
        int spacingInPixels = getResources().getDimensionPixelSize(R.dimen.grid_layout_margin);
        recyclerView.addItemDecoration(new GridSpacingItemDecoration(2,spacingInPixels,true,0));
        // Disable Add Photos Text Here

        openCameraButton.setOnClickListener(view1 -> {
            Intent cameraIntent = new Intent(getActivity(), CameraActivity.class);
            cameraRequestLauncher.launch(cameraIntent);
        });

        openGalleyButton.setOnClickListener(view13 -> {
//            Intent galleryIntent = new Intent();
//            galleryIntent.setType("image/*");
//            galleryIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
//            galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
//            galleryRequestLauncher.launch(Intent.createChooser(galleryIntent,"Select Pictures"));
            galleryRequestLauncher.launch(new String[]{"image/*"});
        });


        startScanButton.setOnClickListener(view12 -> {
            if (imagesObject.size() > 0) {
                filePathForSaving = new File(outputDirectory, "PDF_" + System.currentTimeMillis() + ".pdf");
                String aAbsPath = filePathForSaving.getAbsolutePath();
                String aFileName = aAbsPath.substring(aAbsPath.lastIndexOf("/") + 1);
                openDialog(aFileName);
            } else {
                Toast.makeText(getActivity(), "No Images Added!", Toast.LENGTH_SHORT).show();
            }
        });

        expandMoreButtons.setOnClickListener(v -> animateFab());

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
        setImagesView();
        return view;
    }


    private void shakeArrow() {
        runAnimation = () -> expandMoreButtons.startAnimation(arrowShake);

        handlerExe.postDelayed(runAnimation, 5000);
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
            handlerExe.removeCallbacks(runAnimation);
        }
    }

    private void openDialog(String fileName) {
        fileNameDialog = new FileNameDialog();
        fileNameDialog.setFileNameDialogListener((FileNameDialog.FileNameDialogListener) this);
        fileNameDialog.setDefaultName(fileName);
        fileNameDialog.show(getChildFragmentManager(), "File Name Dialog");
        fileNameDialog.setEditTextHint("Filename");
        fileNameDialog.setTitle("Enter new pdf filename");
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
            Toast.makeText(requireActivity().getApplicationContext(), "File Already Exits!", Toast.LENGTH_SHORT).show();
            openDialog(tempName);
        } else {
            createPDF(tempName);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        PDFBoxResourceLoader.init(requireActivity().getApplicationContext());
        if(madapter!=null){
            if (madapter.getItemCount() > 0) {
            noImagesLayoutText.setVisibility(View.GONE);
        } else {
            noImagesLayoutText.setVisibility(View.VISIBLE);
        }
        }
    }

    public void createPDF(String fileName) {
        if (getActivity() != null) {
            File file = new File(outputDirectory, fileName);
            Snackbar bar = Snackbar.make(image_list_parent_layout, "Please wait! Do not close window while processing images", Snackbar.LENGTH_INDEFINITE)
                    .setActionTextColor(requireContext().getResources().getColor(R.color.light_orange));
            ViewGroup contentLay = (ViewGroup) bar.getView()
                    .findViewById(com.google.android.material.R.id.snackbar_text).getParent();
            ProgressBar item = new ProgressBar(getContext());
            item.getIndeterminateDrawable().setColorFilter(requireContext().getResources().getColor(R.color.orange), PorterDuff.Mode.MULTIPLY);
            contentLay.addView(item);
            bar.show();
            PDDocument document = new PDDocument();
            executor.execute(() -> {
                for (Images image : imagesObject) {
                    try {
                        requireActivity().runOnUiThread(() -> {
                            if ((imagesObject.indexOf(image) + 1) < imagesObject.size()) {
                                bar.setText("Converting To PDF " + (imagesObject.indexOf(image) + 1) + "/" + imagesObject.size());
                            } else {
                                bar.setText("Saving PDF " + (imagesObject.indexOf(image) + 1) + "/" + imagesObject.size());
                            }
                        });
                        Log.d("FileExtenstion", image.getImage());
                        PDImageXObject pdImage = PDImageXObject.createFromFile(image.getImage(), document);
                        PDPage page = new PDPage(PDRectangle.A4);
                        document.addPage(page);
                        PDPageContentStream contentStream = new PDPageContentStream(document, page, true, true);
                        //adjust the padding in generated pdf
                        int padding = 40;
                        int nh = (int) (pdImage.getHeight() * ((PDRectangle.A4.getWidth() - padding) / pdImage.getWidth()));
                        int nw = (int) (pdImage.getWidth() * ((float) nh / pdImage.getHeight()));
                        if (nh <= PDRectangle.A4.getHeight()){

                        }else{
                            nh = (int) PDRectangle.A4.getHeight();
                            nw = (int)PDRectangle.A4.getHeight()*pdImage.getWidth()/pdImage.getHeight();
                        }
                        int heightThresh = (int) ((PDRectangle.A4.getHeight() / 2) - (nh / 2));
                        int widthThresh = (int) ((PDRectangle.A4.getWidth() / 2) - (nw / 2));
                        contentStream.drawImage(pdImage, widthThresh, heightThresh, nw, nh);
                        contentStream.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                        requireActivity().runOnUiThread(() -> {
                            bar.dismiss();
                            Toast.makeText(getContext(), "Pdf creation error - code 24", Toast.LENGTH_SHORT).show();
                        });
                    }
                }

                handlerExe.post(() -> {
                    try {
                        document.save(file);
                        document.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    bar.dismiss();
                    Snackbar bar1 = Snackbar.make(image_list_parent_layout, "Pdf generated! You can check on recent pdf in menu down below 😊", Snackbar.LENGTH_LONG)
                            .setActionTextColor(requireContext().getResources().getColor(R.color.light_orange));
                    bar1.show();
                });
            });
        }

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

    public void fetchProjectById(int id){
        executor.execute(() -> {
            Project project = ProjectDBClient.getInstance(requireActivity().getApplicationContext())
                    .getProjectDB()
                    .projectDao()
                    .getProjectByIdStatic(id);
            currProject = project;
            ArrayList<String> imagePathDb = project.getImagePaths();
            imagesObject.clear();
            for(String path:imagePathDb){
                Images image = new Images(path);
                imagesObject.add(image);
            }
            handlerExe.post(() -> {
                if(madapter!=null){
                    madapter.notifyDataSetChanged();
                }
                if (project.getImagePaths().size() > 0) {
                    noImagesLayoutText.setVisibility(View.GONE);
                } else {
                    noImagesLayoutText.setVisibility(View.VISIBLE);
                }
            });
        });
        handlerExe.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if(getViewLifecycleOwner()!=null);{
                        ProjectDBClient.getInstance(requireActivity().getApplicationContext())
                                .getProjectDB()
                                .projectDao()
                                .getProjectById(id)
                                .observe(getViewLifecycleOwner(), project -> viewModel.updateActionBarTitle(project.getProjectName()));
                    }
                }catch (IllegalStateException e){

                }
            }
        });
    }

    public void updateImagePath(){
        executor.execute(() -> {
            ArrayList<String> paths = new ArrayList<>();
            paths.clear();
            for (Images image : imagesObject) {
                paths.add(image.getImage());
            }
            if (paths.size() >= 0 && !deleteTimeStamp.isEmpty()) {
                insertOnUpdate(paths);
            } else {
                ProjectDBClient.getInstance(requireActivity().getApplicationContext())
                        .getProjectDB()
                        .projectDao()
                        .update(currProject.getId(), paths);

            }
        });
    }

    public void fetchPhotosFromDb(){
        executor.execute(() -> {
            Project project = ProjectDBClient.getInstance(requireActivity().getApplicationContext())
                    .getProjectDB()
                    .projectDao()
                    .getLatestProjectStatic();
            currProject = project;
            ArrayList<String> imagePathsDb = currProject.getImagePaths();
            imagesObject.clear();
            for (String path : imagePathsDb) {
                Images image = new Images(path);
                imagesObject.add(image);
            }
            if(madapter!=null){
                madapter.notifyDataSetChanged();
            }
            noImagesLayoutText.setVisibility(View.GONE);
            viewModel.updateActionBarTitle(currProject.getProjectName());
        });
    }

}