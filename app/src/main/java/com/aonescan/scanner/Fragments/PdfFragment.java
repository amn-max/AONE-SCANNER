package com.aonescan.scanner.Fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.aonescan.scanner.Adapter.PDFAdapter;
import com.aonescan.scanner.CostumClass.OutputDirectory;
import com.aonescan.scanner.MainViewModel;
import com.aonescan.scanner.Model.Pdf;
import com.aonescan.scanner.R;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.play.core.tasks.Task;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PdfFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PdfFragment extends Fragment {

    String pdfPattern = ".pdf";
    private PDFAdapter pdfAdapter;
    private RecyclerView recyclerView;
    private RecyclerView.LayoutManager layoutManager;
    private File outputDirectory;
    private ArrayList<Pdf> pdfFiles = new ArrayList<>();
    private ViewPager2 mainViewPager;
    private LinearLayout displayNoPdfCreated;
    private ReviewInfo reviewInfo;
    private ReviewManager manager;
    private int appCount = 0;
    private int intialCount = 0;
    private MainViewModel viewModel;

    public PdfFragment() {
        // Required empty public constructor
    }

    public static PdfFragment newInstance() {
        PdfFragment fragment = new PdfFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onActivityCreated(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                viewModel = new ViewModelProvider(getActivity()).get(MainViewModel.class);
                viewModel.updateActionBarTitle("Created PDF's");
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        outputDirectory = new OutputDirectory(getContext(), "PDF").getFileDir();

        initReviews();
        SharedPreferences pref = getActivity().getApplicationContext().getSharedPreferences(getResources().getString(R.string.app_name), Context.MODE_PRIVATE);
        appCount = pref.getInt("timesOpened", 1);
        intialCount = pref.getInt("initialCount", 1);

    }

    private void initReviews() {
        manager = ReviewManagerFactory.create(getActivity());
        Task<ReviewInfo> request = manager.requestReviewFlow();
        request.addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                reviewInfo = task.getResult();
            } else {

            }
        });
    }

    private void askForReview() {
        if (reviewInfo != null) {
            Task<Void> flow = manager.launchReviewFlow(getActivity(), reviewInfo);
            flow.addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    SharedPreferences pref = getActivity().getApplicationContext().getSharedPreferences(getResources().getString(R.string.app_name), Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putInt("timesOpened", 1);
                } else {

                }
            });
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        searchDir();
        Log.d("TAGMAIN", pdfFiles.toString());
        pdfAdapter = new PDFAdapter(getActivity(), pdfFiles, PdfFragment.this);
        recyclerView = view.findViewById(R.id.recycler_view_created_pdfs);
        layoutManager = new LinearLayoutManager(getActivity());
        displayNoPdfCreated = view.findViewById(R.id.showNOPdfFile);
        recyclerView.setLayoutManager(layoutManager);

        recyclerView.setAdapter(pdfAdapter);
        if (pdfAdapter.getItemCount() <= 0) {
            displayNoPdfCreated.setVisibility(View.VISIBLE);
        }
        return view;
    }

    private void searchDir() {

        File FileList[] = outputDirectory.listFiles();
        Arrays.sort(FileList, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return Long.valueOf(o2.lastModified()).compareTo(o1.lastModified());
            }
        });


        if (FileList != null) {
            pdfFiles.clear();
            for (int i = 0; i < FileList.length; i++) {
                if (FileList[i].isDirectory()) {

                } else {
                    if (FileList[i].getName().endsWith(pdfPattern)) {
//                        pdfFileName.add(FileList[i].getAbsolutePath());
//                        Log.e("PDFADAPTER"," "+pdfFileName);
                        String name = FileList[i].getName();
                        String absPath = FileList[i].getAbsolutePath();
                        long dateModified = FileList[i].lastModified();
                        String size = getFileSize(FileList[i].length());
                        pdfFiles.add(new Pdf(name, absPath, dateModified, size));
                    }
                }
            }
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        pdfAdapter.notifyDataSetChanged();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    private String getFileSize(long length) {
        final DecimalFormat format = new DecimalFormat("#.##");
        final long MiB = 1024 * 1024;
        final long KiB = 1024;
        if (length > MiB) {
            return format.format(length / MiB) + " MiB";
        }
        if (length > KiB) {
            return format.format(length / KiB) + " KiB";
        }
        return format.format(length) + " B";
    }
}