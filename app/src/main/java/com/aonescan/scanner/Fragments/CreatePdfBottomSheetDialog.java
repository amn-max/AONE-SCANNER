package com.aonescan.scanner.Fragments;

import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;


public class CreatePdfBottomSheetDialog extends BottomSheetDialogFragment {

    private static int CAMERA_REQUEST_CODE = 122;
    private static int GALLERY_REQUEST_CODE = 124;
    private static int CROP_INTENT_CODE = 1021;
    private static int PDF_REQUEST_CODE = 4789;

    private ArrayList<String> photosResult = new ArrayList<>();
    private ArrayList<Images> imagesObject = new ArrayList<>();
    private MaterialButton openCamera;
    private MaterialButton openGallery;
    private Project temp;

    //    private MaterialButton convertPdfToImage;
    @Nullable
    @org.jetbrains.annotations.Nullable
    @Override
    public View onCreateView(@NonNull @NotNull LayoutInflater inflater, @Nullable @org.jetbrains.annotations.Nullable ViewGroup container, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_layout, container, false);

        openCamera = view.findViewById(R.id.btn_openCamera);
        openGallery = view.findViewById(R.id.btn_openGallery);
//        convertPdfToImage = view.findViewById(R.id.btn_convert_to_png);

        openCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent cameraIntent = new Intent(getActivity(), CameraActivity.class);
                startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
            }
        });

        openGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent galleryIntent = new Intent();
                galleryIntent.setType("image/*");
                galleryIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(galleryIntent, "Select Pictures"), GALLERY_REQUEST_CODE);
            }
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
    public void onActivityResult(int requestCode, int resultCode, @Nullable @org.jetbrains.annotations.Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                photosResult.clear();
                photosResult = data.getStringArrayListExtra("photosResult");
                for (int i = 0; i < photosResult.size(); i++) {
                    imagesObject.add(new Images(photosResult.get(i)));
                }
//                madapter.notifyDataSetChanged();
//                noImagesLayoutText.setVisibility(View.GONE);
                AddToHistory addToHistory = new AddToHistory();
                addToHistory.execute();

            }
            if (resultCode == RESULT_CANCELED) {

            }
        }
        if (requestCode == GALLERY_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                photosResult.clear();
                if (data.getData() != null) {
                    Uri mImageUri = data.getData();
                    String realPath = FileUtils.getPath(mImageUri, getContext());
                    imagesObject.add(new Images(realPath));
                    photosResult.add(realPath);
//                    madapter.notifyDataSetChanged();
                    AddToHistory addToHistory = new AddToHistory();
                    addToHistory.execute();

                }
                if (data.getClipData() != null) {
                    ClipData mClipData = data.getClipData();
                    for (int i = 0; i < mClipData.getItemCount(); i++) {
                        ClipData.Item item = mClipData.getItemAt(i);
                        Uri uri = item.getUri();
                        String realPath = FileUtils.getPath(uri, getContext());
                        imagesObject.add(new Images(realPath));
                        photosResult.add(realPath);
                    }
//                    madapter.notifyDataSetChanged();
                    AddToHistory addToHistory = new AddToHistory();
                    addToHistory.execute();

                }
//                noImagesLayoutText.setVisibility(View.GONE);
            }
        }
//        if (requestCode == CROP_INTENT_CODE) {
//            if (resultCode == RESULT_OK) {
//                String editedPhoto = data.getStringExtra("EditedResult");
//                int pos = data.getIntExtra("resultSingleImgPos", 0);
//                imagesObject.get(pos).image = editedPhoto;
////                madapter.notifyDataSetChanged();
//                Log.e("1021REQUEST", "" + editedPhoto);
//
//            }
//        }
//        if(requestCode == PDF_REQUEST_CODE){
//            if(resultCode==RESULT_OK && data!=null){
//                if(data.getData()!=null){
//                    Log.d("pdf","pdf");
//                    Uri pdfUri = data.getData();
//                    String path = FileUtils.getPath(pdfUri,getContext());
//                    convertPdfToJpeg(pdfUri);
////                    Log.d("msgPath", FileUtils.getPath(pdfUri,getContext()));
//                }
//            }
//        }
    }

//
//    private void convertPdfToJpeg(Uri pdfUri) {
//        class ConvertPDFToJPEG extends AsyncTask<Void,Void,Void>{
//            File outputDirectory = new OutputDirectory(getContext(),".images").getFileDir();
//            @Override
//            protected Void doInBackground(Void... voids) {
//                try {
//                    String path = getFileFromContentUri(pdfUri,getContext()).getAbsolutePath();
//                    Log.d("msgPath",path);
//                    PDDocument document = PDDocument.load(new File(path));
//                    PDFRenderer pdfRenderer = new PDFRenderer(document);
//                    Log.d("msgPath", "fd");
//                    for (int page=0;page<document.getNumberOfPages();++page){
//                        Bitmap s = pdfRenderer.renderImageWithDPI(page, 300, Bitmap.Config.RGB_565);
//                        File EditedFile = new File(outputDirectory, "p_Image_" + System.currentTimeMillis() + ".jpg");
//                        FileOutputStream fileOutputStream = new FileOutputStream(EditedFile,false);
//                        s.compress(Bitmap.CompressFormat.JPEG,100,fileOutputStream);
//                        fileOutputStream.flush();
//                        fileOutputStream.close();
//                        imagesObject.add(new Images(EditedFile.getAbsolutePath()));
//                        photosResult.add(EditedFile.getAbsolutePath());
////                        Thread.sleep(10);
//                    }
//
//
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//                return null;
//            }
//
//            @Override
//            protected void onPostExecute(Void unused) {
//                super.onPostExecute(unused);
//                AddToHistory addToHistory = new AddToHistory();
//                addToHistory.execute();
//                ProjectHistoryFragment s = (ProjectHistoryFragment) getActivity()
//                        .getSupportFragmentManager()
//                        .findFragmentById(R.id.main_frame_layout);
//                s.closeBottomSheetDialog();
//                replaceFragment(ImageListFragment.newInstance(),"IMAGE_LIST_FRAGMENT");
//            }
//        }
//        ConvertPDFToJPEG convertPDFToJPEG = new ConvertPDFToJPEG();
//        convertPDFToJPEG.execute();
//    }

    private void getLatestProject() {
        class GetLatestProject extends AsyncTask<Void, Void, Project> {
            @Override
            protected Project doInBackground(Void... voids) {
                Project p = ProjectDBClient.getInstance(getActivity().getApplicationContext()).getProjectDB()
                        .projectDao()
                        .getLatestProjectStatic();
                return p;
            }

            @Override
            protected void onPostExecute(Project project) {
                super.onPostExecute(project);
                temp = project;
                ProjectHistoryFragment s = (ProjectHistoryFragment) getActivity()
                        .getSupportFragmentManager()
                        .findFragmentById(R.id.main_frame_layout);
                s.closeBottomSheetDialog();
                replaceFragment(ImageListFragment.newInstance(), "IMAGE_LIST_FRAGMENT", temp);
            }
        }
        GetLatestProject getLatestProject = new GetLatestProject();
        getLatestProject.execute();
    }

    public void replaceFragment(Fragment fragment, String tag, Project p) {
        //Get current fragment placed in container
        Bundle arguments = new Bundle();
        arguments.putString("historyId", String.valueOf(p.getId()));
        arguments.putString("historyTitle", p.getProjectName());
        fragment.setArguments(arguments);
        Fragment currentFragment = getActivity().getSupportFragmentManager().findFragmentById(R.id.main_frame_layout);

        //Prevent adding same fragment on top
        if (currentFragment.getClass() == fragment.getClass()) {
            return;
        }

        //If fragment is already on stack, we can pop back stack to prevent stack infinite growth
        if (getActivity().getSupportFragmentManager().findFragmentByTag(tag) != null) {
            getActivity().getSupportFragmentManager().popBackStack(tag, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }

        //Otherwise, just replace fragment
        getActivity().getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.slide_in,
                        R.anim.fade_out,
                        R.anim.fade_in,
                        R.anim.slide_out)
                .addToBackStack(tag)
                .replace(R.id.main_frame_layout, fragment, tag)
                .commit();
    }

    class AddToHistory extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            String ts = String.valueOf(System.currentTimeMillis());
            String date = null;
            SimpleDateFormat format = new SimpleDateFormat("yyy-MM-dd hh:mm");
            date = format.format(new Date(Long.valueOf(ts)));
            String project_title = "AONE " + date;
            Project project = new Project(ts, photosResult);
            project.setProjectName(project_title);
            long s = ProjectDBClient.getInstance(getActivity().getApplicationContext()).getProjectDB()
                    .projectDao()
                    .insert(project);
            Log.d("ProjectId", String.valueOf(s));
            Images image = new Images();
            for (int i = 0; i < imagesObject.size(); i++) {
                image.setImage(imagesObject.get(i).getImage());
                image.setIsEnhanced(false);
                image.setId((int) s);
                ProjectDBClient.getInstance(getActivity().getApplicationContext()).getProjectDB()
                        .imagesDao()
                        .insert(image);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            super.onPostExecute(unused);
            getLatestProject();

        }
    }
}
