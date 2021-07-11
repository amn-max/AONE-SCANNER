package com.aonescan.scanner.Adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.core.app.ActivityOptionsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.aonescan.scanner.CostumClass.CustomDialog;
import com.aonescan.scanner.CostumClass.OutputDirectory;
import com.aonescan.scanner.Fragments.ImageListFragment;
import com.aonescan.scanner.Helpers.ImageUtils;
import com.aonescan.scanner.ImagesScanActivity;
import com.aonescan.scanner.Libraries.NativeClass;
import com.aonescan.scanner.MainActivity;
import com.aonescan.scanner.Model.Images;
import com.aonescan.scanner.Model.ImagesListener;
import com.aonescan.scanner.R;
import com.aonescan.scanner.database.ProjectDBClient;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.concurrent.Executor;

import dev.shreyaspatil.MaterialDialog.AbstractDialog;
import dev.shreyaspatil.MaterialDialog.interfaces.DialogInterface;


public class CapturedImagesAdapter extends RecyclerView.Adapter<CapturedImagesAdapter.MyViewHolder> implements androidx.appcompat.view.ActionMode.Callback {
    private static final int CROP_INTENT_CODE = 1021;
    private final Context context;
    private final LayoutInflater mInflater;
    private final ImagesListener imagesListener;
    private final ArrayList<Images> selectedItems = new ArrayList<>();
    private final AppCompatActivity appCompatActivity;
    private final RelativeLayout image_list_parent_layout;
    private final int projectId;
    private final Executor executor;
    private final Handler handler1 = new Handler(Looper.getMainLooper());
    private final ArrayList<Images> absList;
    private boolean multiSelect = false;
    private ActivityResultLauncher<Intent> cropRequestLauncher;
    private ImageListFragment imageListFragment;
    public CapturedImagesAdapter(Context c, ArrayList<Images> absList,
                                 ImagesListener imagesListener,
                                 AppCompatActivity activity,
                                 RelativeLayout image_list_parent_layout,
                                 int projectId,
                                 Executor executor,
                                 ActivityResultLauncher<Intent> cropRequestLauncher,
                                 ImageListFragment imageListFragment) {
        this.context = c;
        this.absList = absList;
        this.mInflater = LayoutInflater.from(c);
        this.imagesListener = imagesListener;
        this.appCompatActivity = activity;
        this.image_list_parent_layout = image_list_parent_layout;
        this.projectId = projectId;
        this.executor = executor;
        this.cropRequestLauncher = cropRequestLauncher;
        this.imageListFragment = imageListFragment;
    }

    @NonNull
    @Override
    public CapturedImagesAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.recyler_images_single_row, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        holder.bindImages(absList.get(position), position, holder);
    }

    @Override
    public int getItemCount() {
        return absList.size();
    }

    public ArrayList<Images> getSelectedImages() {
        synchronized (selectedItems){
            return selectedItems;
        }
    }

    public void unSelectAll() {
        synchronized (selectedItems){
            selectedItems.clear();
        }
    }

    public void setAllChecked() {
        synchronized (selectedItems){
            selectedItems.addAll(absList);
        }
        notifyDataSetChanged();
    }


    @Override
    public boolean onCreateActionMode(androidx.appcompat.view.ActionMode mode, Menu menu) {
        MenuInflater menuInflater = mode.getMenuInflater();
        menuInflater.inflate(R.menu.action_menu_images, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(androidx.appcompat.view.ActionMode mode, Menu menu) {
        return true;
    }

    @Override
    public boolean onActionItemClicked(androidx.appcompat.view.ActionMode mode, MenuItem item) {
        if (item.getItemId() == R.id.action_delete) {
            //handle delete
            ArrayList<Images> tempImageObject = new ArrayList<>(absList);
            try {
                for (int i = 0; i < absList.size(); i++) {
                    tempImageObject.get(i).setSelected(false);
                    synchronized (selectedItems){
                        for (int j = 0; j < selectedItems.size(); j++) {
                        if (absList.get(i).getImage().equals(selectedItems.get(j).getImage())) {
                            selectedItems.get(j).setSelected(false);
                            absList.get(i).setSelected(false);
                            absList.remove(selectedItems.get(j));
                        }
                    }
                    }
                }
            } catch (IndexOutOfBoundsException e) {
                e.printStackTrace();
            }
            deleteSelectedFromDb();
            Snackbar.make(context, image_list_parent_layout, "Undo Deletion of Recent Images", Snackbar.LENGTH_LONG).setActionTextColor(context.getResources().getColor(R.color.light_orange)).setAction("UNDO", new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try {
                        absList.clear();
                        absList.addAll(tempImageObject);
                        restoreDeletedFromDb(tempImageObject);
                        notifyDataSetChanged();
                    } catch (Exception e) {
                        Toast.makeText(context, "Oops! your are out of time", Toast.LENGTH_SHORT).show();
                    }
                }
            }).show();
            mode.finish();
        } else if (item.getItemId() == R.id.action_enhance_selected) {
            bulkEnhance(mode);
        } else if (item.getItemId() == R.id.action_check_all) {
            setAllChecked();
        }
        return true;
    }

    public void updatePaths(ArrayList<String> paths) {
        executor.execute(() -> ProjectDBClient.getInstance(context.getApplicationContext())
                .getProjectDB()
                .projectDao()
                .update(projectId, paths));
    }

    private void deleteSelectedFromDb() {
        executor.execute(() -> {
            ArrayList<String> paths = new ArrayList<>();
            for (Images image : absList) {
                paths.add(image.getImage());
            }
            ProjectDBClient.getInstance(context)
                    .getProjectDB()
                    .projectDao()
                    .update(projectId, paths);
        });
    }

    private void restoreDeletedFromDb(ArrayList<Images> deletedImages) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    ArrayList<String> paths = new ArrayList<>();
                    for (Images image : deletedImages) {
                        paths.add(image.getImage());
                    }
                    ProjectDBClient.getInstance(context)
                            .getProjectDB()
                            .projectDao()
                            .update(projectId, paths);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onDestroyActionMode(androidx.appcompat.view.ActionMode mode) {
        synchronized (selectedItems){
            multiSelect = false;
            selectedItems.clear();
        }
        notifyDataSetChanged();
    }

    public void bulkEnhance(ActionMode mode){
        ArrayList<String> paths = new ArrayList<>();
        executor.execute(() -> {
                try{
                    ArrayList<Images> tempSelectedImages = new ArrayList<>(selectedItems);
                    handler1.post(mode::finish);
                    File outputDirectory = new OutputDirectory(context, ".images").getFileDir();
                    Snackbar snackbar = Snackbar.make(context, image_list_parent_layout, "Please wait! Do not close window while processing images", Snackbar.LENGTH_INDEFINITE)
                            .setActionTextColor(context.getResources().getColor(R.color.light_orange));
                    ViewGroup contentLay = (ViewGroup) snackbar.getView()
                            .findViewById(com.google.android.material.R.id.snackbar_text).getParent();
                    ProgressBar item = new ProgressBar(context);
                    item.getIndeterminateDrawable().setColorFilter(context.getResources().getColor(R.color.orange), PorterDuff.Mode.MULTIPLY);
                    contentLay.addView(item);
                    snackbar.show();
                    for (int i = 0; i < tempSelectedImages.size(); i++) {
                        try {
                            int index = absList.indexOf(tempSelectedImages.get(i));
                            if(!absList.get(index).getIsEnhanced() && new File(absList.get(index).getImage()).exists()){
                                handler1.post(() -> {
                                    try {
                                        int processingImageIndex = index + 1;
                                        int size = tempSelectedImages.size();
                                        int percent = 0;
                                        try {
                                            if(size!=0){
                                                percent = (processingImageIndex * 100) / size;
                                            }
                                        }catch (ArithmeticException e){

                                        }
                                        snackbar.setText("Processing selected images " + percent + " %");
                                        absList.get(index).setEnhancing(true);
                                        notifyItemChanged(index);
                                    } catch (ArrayIndexOutOfBoundsException e) {
                                        e.printStackTrace();
                                    }
                                });
                                File EditedFile = new File(outputDirectory, "b_Image_" + System.currentTimeMillis() + ".jpg");
                                Bitmap bitmap = BitmapFactory.decodeFile(absList.get(index).getImage());
                                bitmap = ImageUtils.extractRotation(bitmap,absList.get(index).getImage());
                                Bitmap enhancedBitmap = new NativeClass().getMagicColoredBitmap(ImageUtils.bitmapToMat(bitmap), 1);
                                bitmap.recycle();
                                absList.get(index).setIsEnhanced(true);
                                absList.get(index).setImage(EditedFile.getAbsolutePath());
                                FileOutputStream fileOutputStream = new FileOutputStream(absList.get(index).getImage(), false);
                                enhancedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, fileOutputStream);
                                fileOutputStream.flush();
                                fileOutputStream.close();
                                handler1.post(() -> {
                                    try {
                                        absList.get(index).setEnhancing(false);
                                        notifyItemChanged(index);
                                    } catch (ArrayIndexOutOfBoundsException e) {
                                        absList.get(index).setEnhancing(false);
                                        notifyItemChanged(index);
                                    }
                                });
                            }
                        } catch (IOException | ArithmeticException | ArrayIndexOutOfBoundsException e) {
                            e.printStackTrace();
                        }
                    }
                    multiSelect = false;
                    selectedItems.clear();
                    tempSelectedImages.clear();
                    handler1.post(CapturedImagesAdapter.this::notifyDataSetChanged);
                    snackbar.dismiss();
                    for (Images image : absList) {
                        paths.add(image.getImage());
                    }
                    updatePaths(paths);
                }catch (ConcurrentModificationException e){
                    e.printStackTrace();
                }
        });
    }


    public class MyViewHolder extends RecyclerView.ViewHolder {
        ImageView singleView;
        ProgressBar progressBar;
        LinearLayout dragIndicator;
//        LinearLayout image_selected_layer;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            singleView = itemView.findViewById(R.id.singleImage);
            progressBar = itemView.findViewById(R.id.singleImageLoading);
            dragIndicator = itemView.findViewById(R.id.dragLinearLayout);
//            image_selected_layer = itemView.findViewById(R.id.image_selected_layer);
        }


        void bindImages(Images image, int pos, MyViewHolder holder) {
            try {
                Log.d("ImageLocation", image.getImage());

                Glide.with(context).load(image.getImage())
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .error(R.drawable.ic_error)
                        .into(singleView);
//                singleView.setImageURI(Uri.fromFile(new File(image.image)));
//                picasso.get().load(new File(image.image)).error(R.drawable.ic_error).into(singleView);


            } catch (ArrayIndexOutOfBoundsException e) {
                e.printStackTrace();
            }
            synchronized (selectedItems){
                if (selectedItems.contains(image)) {
                    singleView.setAlpha(0.3f);
                } else {
                    singleView.setAlpha(1.0f);
                }
            }

            if (image.getIsEnhancing()) {
                progressBar.setVisibility(View.VISIBLE);
            } else {
                progressBar.setVisibility(View.INVISIBLE);
            }

            dragIndicator.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if(event.getActionMasked() == MotionEvent.ACTION_DOWN){
                        imageListFragment.startDragging(holder);
                    }
                    return true;
                }
            });

            singleView.setOnLongClickListener(v -> {
                if (!multiSelect) {
                    multiSelect = true;
                    appCompatActivity.startSupportActionMode(CapturedImagesAdapter.this);
                    selectItem(image, singleView);
                    return true;
                } else {
                    return false;
                }
            });
            singleView.setOnClickListener(view -> {
                if (multiSelect) {
                    selectItem(image, singleView);
                } else {
                    if(Build.VERSION.SDK_INT<Build.VERSION_CODES.LOLLIPOP){
                        if(!image.getImage().isEmpty()){
                            if(new File(image.getImage()).exists()){
                                Intent cropImageIntent = new Intent(context, ImagesScanActivity.class);
//                    cropImageIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                                Activity origin = (Activity) context;
                                cropImageIntent.putExtra("singleImage", image.getImage());
                                cropImageIntent.putExtra("singleImagePosition", pos);
                                cropImageIntent.putExtra("isEnhanced", image.getIsEnhanced());
//                                origin.startActivityForResult(cropImageIntent, CROP_INTENT_CODE);
                                cropRequestLauncher.launch(cropImageIntent);
                            }else{
                                CustomDialog customDialog = new CustomDialog((MainActivity)context);
                                customDialog.showMyDialog("Image does not exist", "", true, "Ok", R.drawable.ic_done, new AbstractDialog.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int which) {
                                        dialogInterface.dismiss();
                                    }
                                }, null, 0, null);
                            }
                        }
                    }else{
                        if(!image.getImage().isEmpty()){
                            if(new File(image.getImage()).exists()){
                                Intent cropImageIntent = new Intent(context, ImagesScanActivity.class);
//                    cropImageIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                Activity origin = (Activity) context;
                                cropImageIntent.putExtra("singleImage", image.getImage());
                                cropImageIntent.putExtra("singleImagePosition", pos);
                                cropImageIntent.putExtra("isEnhanced", image.getIsEnhanced());
                                ActivityOptionsCompat optionsCompat = ActivityOptionsCompat.makeSceneTransitionAnimation(origin,singleView,"singleImage");
                                cropRequestLauncher.launch(cropImageIntent,optionsCompat);
//                                origin.startActivityForResult(cropImageIntent, CROP_INTENT_CODE,optionsCompat.toBundle());
                            }else{
                                CustomDialog customDialog = new CustomDialog((MainActivity)context);
                                customDialog.showMyDialog("Image does not exist", "", true, "Ok", R.drawable.ic_done, new AbstractDialog.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int which) {
                                        dialogInterface.dismiss();
                                    }
                                }, null, 0, null);
                            }
                        }

                    }

                }
            });
        }

        private void selectItem(Images image, ImageView image_selected_layer) {
            executor.execute(() -> {
               synchronized (selectedItems){
                   if (selectedItems.contains(image)) {
                       selectedItems.remove(image);
                       singleView.setAlpha(1.0f);
                   } else {
                       selectedItems.add(image);
                       singleView.setAlpha(0.3f);
                   }
               }
            });
        }
    }


}
