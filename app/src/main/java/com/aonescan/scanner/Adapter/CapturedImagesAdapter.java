package com.aonescan.scanner.Adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.recyclerview.widget.RecyclerView;

import com.aonescan.scanner.CostumClass.OutputDirectory;
import com.aonescan.scanner.Helpers.ImageUtils;
import com.aonescan.scanner.ImagesScanActivity;
import com.aonescan.scanner.Libraries.NativeClass;
import com.aonescan.scanner.Model.Images;
import com.aonescan.scanner.Model.ImagesListener;
import com.aonescan.scanner.R;
import com.aonescan.scanner.database.ProjectDBClient;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Executor;


public class CapturedImagesAdapter extends RecyclerView.Adapter<CapturedImagesAdapter.MyViewHolder> implements androidx.appcompat.view.ActionMode.Callback {
    private static final int CROP_INTENT_CODE = 1021;
    private Context context;
    private ArrayList<Images> absList = new ArrayList<>();
    private LayoutInflater mInflater;
    private ImagesListener imagesListener;
    private boolean multiSelect = false;
    private ArrayList<Images> selectedItems = new ArrayList<>();
    private AppCompatActivity appCompatActivity;
    private RelativeLayout image_list_parent_layout;
    private int projectId;
    private Executor executor;
    private Handler handler1 = new Handler(Looper.getMainLooper());
    private Handler handler2 = new Handler(Looper.getMainLooper());

    public CapturedImagesAdapter(Context c, ArrayList<Images> absList,
                                 ImagesListener imagesListener,
                                 AppCompatActivity activity,
                                 RelativeLayout image_list_parent_layout,
                                 int projectId,
                                 Executor executor) {
        this.context = c;
        this.absList = absList;
        this.mInflater = LayoutInflater.from(c);
        this.imagesListener = imagesListener;
        this.appCompatActivity = activity;
        this.image_list_parent_layout = image_list_parent_layout;
        this.projectId = projectId;
        this.executor = executor;
    }

    @NonNull
    @Override
    public CapturedImagesAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.recyler_images_single_row, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        holder.bindImages(absList.get(position), position);
    }

    @Override
    public int getItemCount() {
        return absList.size();
    }

    public ArrayList getSelectedImages() {
        return selectedItems;
    }

    public void unSelectAll() {
        selectedItems.clear();
    }

    public void setAllChecked() {
        selectedItems.addAll(absList);
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
                    for (int j = 0; j < selectedItems.size(); j++) {
                        if (absList.get(i).getImage() == selectedItems.get(j).getImage()) {
                            selectedItems.get(j).setSelected(false);
                            absList.get(i).setSelected(false);
                            absList.remove(selectedItems.get(j));
                        }
                    }
                }
            } catch (IndexOutOfBoundsException e) {

            }
            deleteSelectedFromDb(selectedItems);
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
            BulkEnhance bulkEnhance = new BulkEnhance(mode);
            bulkEnhance.executeOnExecutor(executor);

        } else if (item.getItemId() == R.id.action_check_all) {
            setAllChecked();
        }
        return true;
    }

    public void updatePaths(ArrayList<String> paths) {
        class UpdatePaths extends AsyncTask<Void, Void, Void> {

            @Override
            protected Void doInBackground(Void... voids) {
                ProjectDBClient.getInstance(context.getApplicationContext())
                        .getProjectDB()
                        .projectDao()
                        .update(projectId, paths);
                return null;
            }
        }
        UpdatePaths updatePaths = new UpdatePaths();
        updatePaths.executeOnExecutor(executor);
    }

    private void deleteSelectedFromDb(ArrayList<Images> selectedImages) {
        class DeleteSelectedFromDb extends AsyncTask<Void, Void, Void> {

            @Override
            protected Void doInBackground(Void... voids) {
                ArrayList<String> paths = new ArrayList<>();
                for (Images image : absList) {
                    paths.add(image.getImage());
                }
                ProjectDBClient.getInstance(context)
                        .getProjectDB()
                        .projectDao()
                        .update(projectId, paths);
                return null;
            }
        }
        DeleteSelectedFromDb deleteSelectedFromDb = new DeleteSelectedFromDb();
        deleteSelectedFromDb.executeOnExecutor(executor);
    }

    private void restoreDeletedFromDb(ArrayList<Images> deletedImages) {
        class RestoreDeletedFromDb extends AsyncTask<Void, Void, Void> {

            @Override
            protected Void doInBackground(Void... voids) {
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

                }
                return null;
            }
        }
        RestoreDeletedFromDb restoreDeletedFromDb = new RestoreDeletedFromDb();
        restoreDeletedFromDb.executeOnExecutor(executor);
    }

    @Override
    public void onDestroyActionMode(androidx.appcompat.view.ActionMode mode) {
        multiSelect = false;
        selectedItems.clear();
        notifyDataSetChanged();
    }

    class BulkEnhance extends AsyncTask<Void, Void, Void> {
        ArrayList<String> paths = new ArrayList<>();
        private ActionMode mode;

        public BulkEnhance(ActionMode mode) {
            this.mode = mode;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            ArrayList<Images> tempSelectedImages = new ArrayList<>(selectedItems);
            handler1.post(new Runnable() {
                @Override
                public void run() {
                    mode.finish();
                }
            });
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

                    handler1.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                int processingImageIndex = index + 1;
                                int size = tempSelectedImages.size();
                                int percent = (processingImageIndex * 100) / size;
                                snackbar.setText("Processing selected images " + percent + " %");
                                absList.get(index).setEnhancing(true);
                                notifyItemChanged(index);
                            } catch (ArrayIndexOutOfBoundsException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    File EditedFile = new File(outputDirectory, "b_Image_" + System.currentTimeMillis() + ".jpg");
                    Bitmap bitmap = BitmapFactory.decodeFile(absList.get(index).getImage());
                    Bitmap enhancedBitmap = new NativeClass().getMagicColoredBitmap(ImageUtils.bitmapToMat(bitmap), 1);
                    bitmap.recycle();
                    absList.get(index).setIsEnhanced(true);
                    absList.get(index).setImage(EditedFile.getAbsolutePath());
                    FileOutputStream fileOutputStream = new FileOutputStream(absList.get(index).getImage(), false);
                    enhancedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, fileOutputStream);
                    fileOutputStream.flush();
                    fileOutputStream.close();
                    handler2.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                absList.get(index).setEnhancing(false);
                                notifyItemChanged(index);
                            } catch (ArrayIndexOutOfBoundsException e) {
                                absList.get(index).setEnhancing(false);
                                notifyItemChanged(index);
                            }
                        }
                    }, 200);
                    if (isCancelled()) {
                        break;
                    }
                    Thread.sleep(5);

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ArrayIndexOutOfBoundsException e) {
                    e.printStackTrace();
                }
            }
            multiSelect = false;
            selectedItems.clear();
            tempSelectedImages.clear();
            handler2.post(new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            });
            snackbar.dismiss();
            return null;
        }

        @Override
        protected void onCancelled(Void unused) {
            super.onCancelled(unused);
            Snackbar snackbar = Snackbar.make(context, image_list_parent_layout, "Canceled enhancing!", Snackbar.LENGTH_INDEFINITE)
                    .setActionTextColor(context.getResources().getColor(R.color.light_orange));
            snackbar.show();
        }

        @Override
        protected void onPostExecute(Void unused) {
            super.onPostExecute(unused);
            for (Images image : absList) {
                paths.add(image.getImage());
            }
            updatePaths(paths);
        }
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        ImageView singleView;
        ProgressBar progressBar;
//        LinearLayout image_selected_layer;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            singleView = (ImageView) itemView.findViewById(R.id.singleImage);
            progressBar = itemView.findViewById(R.id.singleImageLoading);
//            image_selected_layer = itemView.findViewById(R.id.image_selected_layer);
        }


        void bindImages(Images image, int pos) {
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

            }
            if (selectedItems.contains(image)) {
                singleView.setAlpha(0.3f);
            } else {
                singleView.setAlpha(1.0f);
            }

            if (image.getIsEnhancing()) {
                progressBar.setVisibility(View.VISIBLE);
            } else {
                progressBar.setVisibility(View.INVISIBLE);
            }

            singleView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (!multiSelect) {
                        multiSelect = true;
                        appCompatActivity.startSupportActionMode(CapturedImagesAdapter.this);
                        selectItem(image, singleView);
                        return true;
                    } else {
                        return false;
                    }
                }
            });
            singleView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (multiSelect) {
                        selectItem(image, singleView);
                    } else {
                        Intent cropImageIntent = new Intent(context, ImagesScanActivity.class);
//                    cropImageIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        Activity origin = (Activity) context;
                        cropImageIntent.putExtra("singleImage", image.getImage());
                        cropImageIntent.putExtra("singleImagePosition", pos);
                        cropImageIntent.putExtra("isEnhanced", image.getIsEnhanced());
                        origin.startActivityForResult(cropImageIntent, CROP_INTENT_CODE);
                    }
                }
            });

//            if (image.getSelected()) {
//                checkBox.setChecked(true);
//                imagesListener.OnImagesAction(true);
//                try {
//                    notifyItemChanged(pos);
//                }catch (Exception e){
//
//                }
//            } else {
//                checkBox.setChecked(false);
//                try {
//                    notifyItemChanged(pos);
//                }catch (Exception e){
//
//                }
//            }
//            if(getSelectedImages().size()==0){
//                imagesListener.OnImagesAction(false);
//            }


//            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//                @Override
//                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                    image.setSelected(!image.getSelected());
//                    checkBox.setChecked(image.getSelected() ? true : false);
//                    if(getSelectedImages().size()!=0){
//                        imagesListener.OnImagesAction(true);
//
//                    }else {
//                        imagesListener.OnImagesAction(false);
//
//                    }
////                    if (image.getSelected()) {
////                        image.setSelected(false);
////                        imagesListener.OnImagesAction(true);
////                        if (getSelectedImages().size() == 0) {
////                            //Add Listener
////                            imagesListener.OnImagesAction(false);
////                        }
////                    } else {
////                        image.setSelected(true);
////                        imagesListener.OnImagesAction(true);
////                    }
//                }
//            });

//            singleView.setOnLongClickListener(new View.OnLongClickListener() {
//                @Override
//                public boolean onLongClick(View view) {
//                    if(image.isSelected){
//                        checkCircle.setVisibility(View.GONE);
//                        image.isSelected = false;
//                        if(getSelectedImages().size()==0){
//                            //Add Listener
//                            imagesListener.OnImagesAction(false);
//                        }
//                    }else{
//                        checkCircle.setVisibility(View.VISIBLE);
//                        image.isSelected = true;
//                        imagesListener.OnImagesAction(true);
//                    }
//                    return true;
//                }
//            });


        }

        private void selectItem(Images image, ImageView image_selected_layer) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
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
