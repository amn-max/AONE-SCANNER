package com.aonescan.scanner.Adapter;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aonescan.scanner.Fragments.ImageListFragment;
import com.aonescan.scanner.MainActivity;
import com.aonescan.scanner.Model.ProjectHistoryListener;
import com.aonescan.scanner.R;
import com.aonescan.scanner.database.Project;
import com.aonescan.scanner.database.ProjectDBClient;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textview.MaterialTextView;

import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Executor;

public class ProjectHistoryAdapter extends RecyclerView.Adapter<ProjectHistoryAdapter.ProjectHistoryViewHolder> implements androidx.appcompat.view.ActionMode.Callback {
    private final List<Project> selectedItems = new ArrayList<>();
    private Context mCtx;
    private List<Project> projectList;
    private ProjectHistoryListener listener;
    private boolean isOnLongPress = false;
    private boolean multiSelect = false;
    private AppCompatActivity appCompatActivity;
    private RelativeLayout fragment_project_history_parent;
    private Executor executor;
    private Handler handler = new Handler(Looper.getMainLooper());
    private ProjectHistoryAdapter.OnItemClickListener itemClickListener;
    public ProjectHistoryAdapter(Context mCtx,
                                 List<Project> projectList,
                                 ProjectHistoryListener listener,
                                 AppCompatActivity activity,
                                 RelativeLayout fragment_project_history_parent,
                                 Executor executor,
                                 ProjectHistoryAdapter.OnItemClickListener itemClickListener) {
        this.mCtx = mCtx;
        this.projectList = projectList;
        this.listener = listener;
        this.appCompatActivity = activity;
        this.fragment_project_history_parent = fragment_project_history_parent;
        this.executor = executor;
        this.itemClickListener = itemClickListener;
    }

    public ProjectHistoryAdapter() {
    }


    @NonNull
    @NotNull
    @Override
    public ProjectHistoryAdapter.ProjectHistoryViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mCtx).inflate(R.layout.recyler_view_project_layout, parent, false);
        return new ProjectHistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull ProjectHistoryAdapter.ProjectHistoryViewHolder holder, int position) {
        holder.bindProject(position);
    }

    public interface OnItemClickListener {
        void onItemClicked(View v,String historyName,int historyId,int position);
    }

    @Override
    public int getItemCount() {
        return projectList.size();
    }

    //get all selected items
    public List<Project> getAll() {
        return projectList;
    }

    //get selected when btn clicked
    public List<Project> getSelected() {
        synchronized (selectedItems){
            return selectedItems;
        }
    }

    public void unSelectAll() {
        synchronized (selectedItems){
            selectedItems.clear();
        }
    }

    public void setLongPressAndActionListener(boolean isOnLongPress, boolean isSelected) {
        this.isOnLongPress = isOnLongPress;
        this.listener.OnProjectAction(isSelected);
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        MenuInflater menuInflater = mode.getMenuInflater();
        menuInflater.inflate(R.menu.action_menu_projects, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        if (item.getItemId() == R.id.action_delete) {
            ArrayList<Project> tempProjectObject = new ArrayList<>(projectList);

            try {
                for (int i = 0; i < projectList.size(); i++) {
                    tempProjectObject.get(i).setChecked(false);
                    synchronized (selectedItems){
                        for (int j = 0; j < selectedItems.size(); j++) {
                        if (projectList.get(i).getId() == selectedItems.get(j).getId()) {
                            selectedItems.get(j).setChecked(false);
                            projectList.get(i).setChecked(false);
                            projectList.remove(selectedItems.get(j));
                        }
                    }
                    }
                }
            } catch (IndexOutOfBoundsException e) {
                e.printStackTrace();
            }
            deleteFromDb();
            Snackbar.make(mCtx, fragment_project_history_parent, "Undo Deletion of Scans", Snackbar.LENGTH_LONG).setActionTextColor(mCtx.getResources().getColor(R.color.light_orange)).setAction("UNDO", new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    projectList.clear();
                    projectList.addAll(tempProjectObject);
                    restoreInDb(tempProjectObject);
                    notifyDataSetChanged();
                }
            }).show();
            mode.finish();
        } else if (item.getItemId() == R.id.action_check_all) {
            setAllChecked();
        }
        return true;
    }

    void restoreInDb(ArrayList<Project> items) {
        executor.execute(() -> {
            for (Project item : items) {
                ProjectDBClient.getInstance(mCtx.getApplicationContext())
                        .getProjectDB()
                        .projectDao()
                        .insert(item);
            }
        });
    }

    void deleteFromDb() {
        executor.execute(() -> {
            synchronized (selectedItems){
                for (Iterator<Project> iterator = selectedItems.iterator(); iterator.hasNext();){
                    try {
                        Project project = iterator.next();
                        project.setChecked(false);
                        ProjectDBClient.getInstance(mCtx.getApplicationContext())
                                .getProjectDB()
                                .projectDao()
                                .delete(project);
                    }catch (ConcurrentModificationException e){

                    }
                }
            }
        });
    }

    private void setAllChecked() {
        synchronized (selectedItems){
            selectedItems.addAll(projectList);
        }
        notifyDataSetChanged();
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {

        synchronized (selectedItems){
            multiSelect = false;
            selectedItems.clear();
        }
        notifyDataSetChanged();
    }

    public class ProjectHistoryViewHolder extends RecyclerView.ViewHolder{
        private final MaterialTextView modifiedProjectDate;
        private final MaterialTextView numberOfImageInProject;
        private final ShapeableImageView imageView0;
        private final LinearLayout open_list_pdf;
        private final RelativeLayout RL_selection_layout;
        private final LinearLayout selection_background;
        private final TextView txt_project_name;
        private final MaterialButton btn_change_title_name;
        public ProjectHistoryViewHolder(@NonNull @NotNull View itemView) {
            super(itemView);
            modifiedProjectDate = itemView.findViewById(R.id.txt_modified_project_date);
            numberOfImageInProject = itemView.findViewById(R.id.txt_numberOfImages_project);
            imageView0 = itemView.findViewById(R.id.image_preview0);
            RL_selection_layout = itemView.findViewById(R.id.RL_selection_layout);
            open_list_pdf = itemView.findViewById(R.id.open_list_pdf);
            selection_background = itemView.findViewById(R.id.selection_background);
            txt_project_name = itemView.findViewById(R.id.txt_project_name);
            btn_change_title_name = itemView.findViewById(R.id.edit_scan_name);
        }

        public void bindProject(int pos) {
            //new bind
            Project curr = projectList.get(pos);

            synchronized (selectedItems){
                if (selectedItems.contains(curr)) {
                    selection_background.setVisibility(View.VISIBLE);
                } else {
                    selection_background.setVisibility(View.INVISIBLE);
                }
            }


            //old bind
            ArrayList<String> imagePaths = curr.getImagePaths();
            int size = imagePaths.size();

            String date = null;
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm aa", Locale.getDefault());
            date = format.format(new Date(Long.parseLong(curr.getCreatedOn())));
            modifiedProjectDate.setText(date);
            numberOfImageInProject.setText(String.valueOf(size));
            txt_project_name.setText(curr.getProjectName());
            try {
                if (getAdapterPosition() == pos) {
                    Glide.with(mCtx).load(imagePaths.get(0))
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .skipMemoryCache(true)
                            .into(imageView0);
                } else {
                    Glide.with(mCtx).clear(imageView0);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            open_list_pdf.setOnClickListener(v -> {
                if (multiSelect) {
                    selectItem(selection_background, curr);
                } else {
                    openRecentHistory(pos);
                }
            });

            imageView0.setOnClickListener(v -> {
                if (multiSelect) {
                    selectItem(selection_background, curr);
                } else {
                    openRecentHistory(pos);
                }
            });
            open_list_pdf.setOnLongClickListener(v -> {
                longPressAction(curr);
                return true;
            });

            imageView0.setOnLongClickListener(v -> {
                longPressAction(curr);
                return true;
            });
            RL_selection_layout.setOnLongClickListener(v -> {
                longPressAction(curr);
                return true;
            });

            btn_change_title_name.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    itemClickListener.onItemClicked(v,curr.getProjectName(),curr.getId(),pos);
                }
            });

        }


        void longPressAction(Project curr) {
            if (!multiSelect) {
                multiSelect = true;
                appCompatActivity.startSupportActionMode(ProjectHistoryAdapter.this);
                selectItem(selection_background, curr);
            }
        }

        private void selectItem(LinearLayout selection_background, Project curr) {
            executor.execute(() -> {
                synchronized (selectedItems){
                    if (selectedItems.contains(curr)) {
                        selectedItems.remove(curr);
                        handler.post(() -> selection_background.setVisibility(View.INVISIBLE));
                    } else {
                        selectedItems.add(curr);
                        handler.post(() -> selection_background.setVisibility(View.VISIBLE));
                    }
                }
            });
        }


        void openRecentHistory(int pos) {
            replaceFragment(ImageListFragment.newInstance(), "IMAGE_LIST_FRAGMENT", projectList.get(pos).getId(), projectList.get(pos).getProjectName());
        }

        public void replaceFragment(Fragment fragment, String tag, int historyId, String historyTitle) {
            //Get current fragment placed in container
            Bundle arguments = new Bundle();
            arguments.putString("historyId", String.valueOf(historyId));
            arguments.putString("historyTitle", historyTitle);
            fragment.setArguments(arguments);
            Fragment currentFragment = ((MainActivity) mCtx).getSupportFragmentManager().findFragmentById(R.id.main_frame_layout);

            //Prevent adding same fragment on top
            if (Objects.requireNonNull(currentFragment).getClass() == fragment.getClass()) {
                return;
            }

            //If fragment is already on stack, we can pop back stack to prevent stack infinite growth
            if (((MainActivity) mCtx).getSupportFragmentManager().findFragmentByTag(tag) != null) {
                ((MainActivity) mCtx).getSupportFragmentManager().popBackStack(tag, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            }

            //Otherwise, just replace fragment
            ((MainActivity) mCtx).getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.anim.slide_in,
                            R.anim.fade_out,
                            R.anim.fade_in,
                            R.anim.slide_out)
                    .addToBackStack(tag)
                    .replace(R.id.main_frame_layout, fragment, tag)
                    .commit();
        }
    }
}
