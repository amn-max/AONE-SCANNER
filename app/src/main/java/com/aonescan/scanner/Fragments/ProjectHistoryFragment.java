package com.aonescan.scanner.Fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aonescan.scanner.Adapter.ProjectHistoryAdapter;
import com.aonescan.scanner.CostumClass.ScanNameDialog;
import com.aonescan.scanner.MainActivity;
import com.aonescan.scanner.MainViewModel;
import com.aonescan.scanner.Model.ProjectHistoryListener;
import com.aonescan.scanner.R;
import com.aonescan.scanner.database.Project;
import com.aonescan.scanner.database.ProjectDBClient;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.skydoves.balloon.ArrowOrientation;
import com.skydoves.balloon.ArrowPositionRules;
import com.skydoves.balloon.Balloon;
import com.skydoves.balloon.BalloonAnimation;
import com.skydoves.balloon.BalloonSizeSpec;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ProjectHistoryFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ProjectHistoryFragment extends Fragment implements ProjectHistoryListener, LifecycleObserver, ProjectHistoryAdapter.OnItemClickListener, ScanNameDialog.ScanNameDialogListener {

    private RecyclerView recyclerViewProjectHistory;
    private ProjectHistoryAdapter projectHistoryAdapter;
    private ArrayList<Project> projectList;
    private FloatingActionButton fabCreateNewProject;
    private CreatePdfBottomSheetDialog bottomSheetDialog;
    private MainViewModel viewModel;
    private RelativeLayout fragment_project_history_parent;
    private Balloon balloon;
    private ScanNameDialog scanNameDialog;
    private final Executor executor = new ThreadPoolExecutor(5, 128, 1,
            TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    private final Handler handler = new Handler(Looper.getMainLooper());
    private String historyName;
    private int historyId;
    private int changedPositionAdapter = -1;
    public ProjectHistoryFragment() {
        // Required empty public constructor
    }

    public static ProjectHistoryFragment newInstance() {
        ProjectHistoryFragment fragment = new ProjectHistoryFragment();
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
            viewModel.updateActionBarTitle("Scan History");
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (projectHistoryAdapter.getSelected().size() > 0) {
                    projectHistoryAdapter.unSelectAll();
                    projectHistoryAdapter.setLongPressAndActionListener(false, false);
                } else {
//                    setEnabled(false);

                    requireActivity().onBackPressed();
                }
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_project_history, container, false);
        projectList = new ArrayList<>();
        recyclerViewProjectHistory = view.findViewById(R.id.recycler_view_project_history);
        fabCreateNewProject = view.findViewById(R.id.fab_create_new_project);
        fragment_project_history_parent = view.findViewById(R.id.fragment_project_history_parent);
        recyclerViewProjectHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        balloon = new Balloon.Builder(requireContext())
                .setArrowSize(10)
                .setArrowOrientation(ArrowOrientation.TOP)
                .setArrowPositionRules(ArrowPositionRules.ALIGN_BALLOON)
                .setArrowPosition(0.5f)
                .setWidth(BalloonSizeSpec.WRAP)
                .setHeight(65)
                .setTextSize(15f)
                .setPadding(5)
                .setCornerRadius(4f)
                .setAlpha(0.9f)
                .setText("Create your first scan")
                .setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                .setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.light_purple))
                .setBalloonAnimation(BalloonAnimation.FADE)
                .setAutoDismissDuration(4000L)
                .build();

        fabCreateNewProject.setOnClickListener(v -> {
            bottomSheetDialog = new CreatePdfBottomSheetDialog();
            bottomSheetDialog.show(((MainActivity) requireActivity()).getSupportFragmentManager(), "ModalBottomSheet");
        });
        fetchFromHistory();
        ProjectHistoryAdapter.OnItemClickListener listener = this;
        projectHistoryAdapter = new ProjectHistoryAdapter(requireContext(), projectList, this, (MainActivity) getActivity(), fragment_project_history_parent,executor, listener);
        recyclerViewProjectHistory.setAdapter(projectHistoryAdapter);
        return view;
    }


    public void closeBottomSheetDialog() {
        bottomSheetDialog.dismiss();
    }

    @Override
    public void OnProjectAction(Boolean isSelected) {
    }

    public void fetchFromHistory(){
        handler.post(() -> ProjectDBClient.getInstance(requireActivity().getApplicationContext())
                .getProjectDB()
                .projectDao()
                .getAllProjects("[]")
                .observe(getViewLifecycleOwner(), projects -> {
                    projectList.clear();
                    projectList.addAll(projects);
                    if(projectHistoryAdapter!=null){
                        if(changedPositionAdapter>0){
                            projectHistoryAdapter.notifyItemChanged(changedPositionAdapter);
                            changedPositionAdapter = -1;
                        }else{
                            projectHistoryAdapter.notifyDataSetChanged();
                            changedPositionAdapter = -1;
                        }
                        if (projectList.size() == 0) {
                            balloon.showAlignTop(fabCreateNewProject, 0, -10);
                        }
                    }
                }));
        executor.execute(() -> ProjectDBClient.getInstance(requireActivity().getApplicationContext())
                .getProjectDB()
                .projectDao()
                .deleteEmptyProjects("[]"));
    }


    @Override
    public void onItemClicked(View v, String historyName, int historyId,int position) {
        scanNameDialog = new ScanNameDialog();
        scanNameDialog.setDefaultName(historyName);
        scanNameDialog.show(getChildFragmentManager(),"Scan Name");
        scanNameDialog.setFileNameDialogListener((ScanNameDialog.ScanNameDialogListener) this);
        scanNameDialog.setTitle("Enter new scan name");
        scanNameDialog.setEditTextHint("Scan name");
        this.historyId = historyId;
        this.historyName = historyName;
        this.changedPositionAdapter = position;
    }

    @Override
    public void applyScanText(String fileName) {
        //change scan name here using db
        executor.execute(new Runnable() {
            @Override
            public void run() {
                if(historyId>0 && !historyName.isEmpty()){
                    ProjectDBClient.getInstance(requireActivity().getApplicationContext())
                            .getProjectDB()
                            .projectDao()
                            .updateProjectName(fileName, historyId);
                }
            }
        });
    }

    @Override
    public void onScanDialog(boolean showDialog) {
        if(showDialog){
            scanNameDialog.dismiss();
        }
    }
}