package com.aonescan.scanner.Fragments;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.aonescan.scanner.CameraActivity;
import com.aonescan.scanner.CostumClass.FileUtils;
import com.aonescan.scanner.Model.Images;
import com.aonescan.scanner.R;
import com.aonescan.scanner.database.Project;
import com.aonescan.scanner.database.ProjectDBClient;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static android.app.Activity.RESULT_OK;


public class CreatePdfBottomSheetDialog extends BottomSheetDialogFragment {

    private final Executor executor = new ThreadPoolExecutor(5, 128, 1,
            TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ArrayList<Images> imagesObject = new ArrayList<>();
    private ArrayList<String> photosResult = new ArrayList<>();
    private MaterialButton openCamera;
    private MaterialButton openGallery;
    private ActivityResultLauncher<Intent> cameraRequestLauncher;
    private ActivityResultLauncher<String[]> galleryRequestLauncher;

    //    private MaterialButton convertPdfToImage;
    @Nullable
    @org.jetbrains.annotations.Nullable
    @Override
    public View onCreateView(@NonNull @NotNull LayoutInflater inflater, @Nullable @org.jetbrains.annotations.Nullable ViewGroup container, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_layout, container, false);

        openCamera = view.findViewById(R.id.btn_openCamera);
        openGallery = view.findViewById(R.id.btn_openGallery);
//        convertPdfToImage = view.findViewById(R.id.btn_convert_to_png);

        openCamera.setOnClickListener(v -> {
            Intent cameraIntent = new Intent(getActivity(), CameraActivity.class);
            cameraRequestLauncher.launch(cameraIntent);
        });

        openGallery.setOnClickListener(v -> {
            galleryRequestLauncher.launch(new String[]{"image/*"});
        });
//        convertPdfToImage.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent pdfIntent = new Intent();
//                pdfIntent.setType("application/pdf");
//                pdfIntent.setAction(Intent.ACTION_GET_CONTENT);
//                startActivityForResult(Intent.createChooser(pdfIntent, "Select Document"), PDF_REQUEST_CODE);
//            }
//        });

        return view;
    }

    @Override
    public void onAttach(@NonNull @NotNull Context context) {
        super.onAttach(context);
        cameraRequestLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        photosResult.clear();
                        photosResult = result.getData().getStringArrayListExtra("photosResult");
                        for (int i = 0; i < photosResult.size(); i++) {
                            imagesObject.add(new Images(photosResult.get(i)));
                        }
                        addToHistory();
                    }
                }
        );

        galleryRequestLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenMultipleDocuments(),
                result -> {
                    if (!result.isEmpty()) {
                        for (int i = 0; i < result.size(); i++) {
                            Uri uri = result.get(i);
                            String realPath = FileUtils.getPath(uri, getContext());
                            imagesObject.add(new Images(realPath));
                            photosResult.add(realPath);
                        }
                        addToHistory();
                    }
                }
        );
    }


    private void getLatestProject() {
        executor.execute(() -> {
            Project p = ProjectDBClient.getInstance(requireActivity().getApplicationContext()).getProjectDB()
                    .projectDao()
                    .getLatestProjectStatic();
            handler.post(() -> {
                ProjectHistoryFragment s = (ProjectHistoryFragment) requireActivity()
                        .getSupportFragmentManager()
                        .findFragmentById(R.id.main_frame_layout);
                if (s != null) {
                    s.closeBottomSheetDialog();
                }
                replaceFragment(ImageListFragment.newInstance(), "IMAGE_LIST_FRAGMENT", p);
            });
        });
    }

    public void replaceFragment(Fragment fragment, String tag, Project p) {
        //Get current fragment placed in container
        Bundle arguments = new Bundle();
        arguments.putString("historyId", String.valueOf(p.getId()));
        arguments.putString("historyTitle", p.getProjectName());
        fragment.setArguments(arguments);
        Fragment currentFragment = requireActivity().getSupportFragmentManager().findFragmentById(R.id.main_frame_layout);

        //Prevent adding same fragment on top
        if (Objects.requireNonNull(currentFragment).getClass() == fragment.getClass()) {
            return;
        }

        //If fragment is already on stack, we can pop back stack to prevent stack infinite growth
        if (requireActivity().getSupportFragmentManager().findFragmentByTag(tag) != null) {
            requireActivity().getSupportFragmentManager().popBackStack(tag, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }

        //Otherwise, just replace fragment
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.slide_in,
                        R.anim.fade_out,
                        R.anim.fade_in,
                        R.anim.slide_out)
                .addToBackStack(tag)
                .replace(R.id.main_frame_layout, fragment, tag)
                .commit();
    }

    public void addToHistory() {
        executor.execute(() -> {
            String ts = String.valueOf(System.currentTimeMillis());
            String date = null;
            SimpleDateFormat format = new SimpleDateFormat("yyy-MM-dd hh:mm", Locale.getDefault());
            date = format.format(new Date(Long.parseLong(ts)));
            String project_title = "AONE " + date;
            Project project = new Project(ts, photosResult);
            project.setProjectName(project_title);
            long s = ProjectDBClient.getInstance(requireActivity().getApplicationContext()).getProjectDB()
                    .projectDao()
                    .insert(project);
            Log.d("ProjectId", String.valueOf(s));
            Images image = new Images();
            for (int i = 0; i < imagesObject.size(); i++) {
                image.setImage(imagesObject.get(i).getImage());
                image.setIsEnhanced(false);
                image.setId((int) s);
                ProjectDBClient.getInstance(requireActivity().getApplicationContext()).getProjectDB()
                        .imagesDao()
                        .insert(image);
            }
            handler.post(this::getLatestProject);
        });
    }
}
