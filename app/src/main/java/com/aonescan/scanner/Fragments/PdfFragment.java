package com.aonescan.scanner.Fragments;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aonescan.scanner.Adapter.PDFAdapter;
import com.aonescan.scanner.CostumClass.OutputDirectory;
import com.aonescan.scanner.MainViewModel;
import com.aonescan.scanner.Model.Pdf;
import com.aonescan.scanner.R;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PdfFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PdfFragment extends Fragment implements LifecycleObserver {

    private final ArrayList<Pdf> pdfFiles = new ArrayList<>();
    String pdfPattern = ".pdf";
    private PDFAdapter pdfAdapter;
    private RecyclerView recyclerView;
    private RecyclerView.LayoutManager layoutManager;
    private File outputDirectory;
    private LinearLayout displayNoPdfCreated;
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

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    public void onCreated(){
        requireActivity().getLifecycle().removeObserver(this);
    }

    @Override
    public void onAttach(@NonNull @NotNull Context context) {
        super.onAttach(context);
        requireActivity().getLifecycle().addObserver(this);
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        requireActivity().runOnUiThread(() -> {
            viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
            viewModel.updateActionBarTitle("Created PDF's");
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        outputDirectory = new OutputDirectory(getContext(), "PDF").getFileDir();
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

        File[] FileList = outputDirectory.listFiles();
        if(FileList!=null){
            Arrays.sort(FileList,
                    (o1, o2) -> Long.compare(o2.lastModified(), o1.lastModified()));
        }


        pdfFiles.clear();
        if (FileList != null) {
            for (File file : FileList) {
                if (file.isDirectory()) {

                } else {
                    if (file.getName().endsWith(pdfPattern)) {
    //                        pdfFileName.add(FileList[i].getAbsolutePath());
    //                        Log.e("PDFADAPTER"," "+pdfFileName);
                        String name = file.getName();
                        String absPath = file.getAbsolutePath();
                        long dateModified = file.lastModified();
                        String size = getFileSize(file.length());
                        pdfFiles.add(new Pdf(name, absPath, dateModified, size));
                    }
                }
            }
        }
        requireActivity().runOnUiThread(new Runnable() {
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