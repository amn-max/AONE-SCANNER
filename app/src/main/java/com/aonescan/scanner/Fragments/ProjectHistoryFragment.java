package com.aonescan.scanner.Fragments;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aonescan.scanner.Adapter.ProjectHistoryAdapter;
import com.aonescan.scanner.MainActivity;
import com.aonescan.scanner.MainViewModel;
import com.aonescan.scanner.Model.ProjectHistoryListener;
import com.aonescan.scanner.R;
import com.aonescan.scanner.database.Project;
import com.aonescan.scanner.database.ProjectDBClient;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ProjectHistoryFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ProjectHistoryFragment extends Fragment implements ProjectHistoryListener {

    private RecyclerView recyclerViewProjectHistory;
    private ProjectHistoryAdapter projectHistoryAdapter;
    private ArrayList<Project> projectList;
    private FloatingActionButton fabCreateNewProject;
    private CreatePdfBottomSheetDialog bottomSheetDialog;
    private MainViewModel viewModel;
    private RelativeLayout fragment_project_history_parent;

    public ProjectHistoryFragment() {
        // Required empty public constructor
    }

    public static ProjectHistoryFragment newInstance() {
        ProjectHistoryFragment fragment = new ProjectHistoryFragment();
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
                viewModel.updateActionBarTitle("Scan History");
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (projectHistoryAdapter.getSelected().size() > 0) {
                    projectHistoryAdapter.unSelectAll();
                    projectHistoryAdapter.setLongPressAndActionListener(false, false);
                } else {
//                    setEnabled(false);

                    getActivity().onBackPressed();
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
        fabCreateNewProject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheetDialog = new CreatePdfBottomSheetDialog();
                bottomSheetDialog.show(((MainActivity) getActivity()).getSupportFragmentManager(), "ModalBottomSheet");
            }
        });


        FetchFromHistory fetchFromHistory = new FetchFromHistory();
        fetchFromHistory.execute();
        return view;
    }


    public void closeBottomSheetDialog() {
        bottomSheetDialog.dismiss();
    }

    @Override
    public void OnProjectAction(Boolean isSelected) {
    }

    void runAdapter(List<Project> projects) {
        projectList.clear();
        projectList.addAll(projects);
        projectHistoryAdapter = new ProjectHistoryAdapter(getContext(), projectList, this, (MainActivity) getActivity(), fragment_project_history_parent);
        recyclerViewProjectHistory.setAdapter(projectHistoryAdapter);
    }

    class FetchFromHistory extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            ProjectDBClient.getInstance(getActivity().getApplicationContext())
                    .getProjectDB()
                    .projectDao()
                    .getAllProjects("[]")
                    .observe(getViewLifecycleOwner(), new Observer<List<Project>>() {
                        @Override
                        public void onChanged(List<Project> projects) {
                            runAdapter(projects);
                        }
                    });
        }

        @Override
        protected Void doInBackground(Void... voids) {
            ProjectDBClient.getInstance(getActivity().getApplicationContext())
                    .getProjectDB()
                    .projectDao()
                    .deleteEmptyProjects("[]");
            return null;
        }
    }
}