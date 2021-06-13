package com.aonescan.scanner.Adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.aonescan.scanner.CostumClass.CustomDialog;
import com.aonescan.scanner.Fragments.PdfFragment;
import com.aonescan.scanner.Model.Pdf;
import com.aonescan.scanner.PdfViewer;
import com.aonescan.scanner.R;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.PdfiumCore;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

import dev.shreyaspatil.MaterialDialog.AbstractDialog;

public class PDFAdapter extends RecyclerView.Adapter<PDFAdapter.PdfViewHolder> {

    private Context context;
    private ArrayList<Pdf> pdfAbsPath = new ArrayList<>();
    private PdfFragment pdfFragment;
    private File renameFile;
    private PdfiumCore pdfiumCore;
    private int pageNum = 0;

    public PDFAdapter(Context c, ArrayList<Pdf> pdfAbsPath, PdfFragment FragmentManager) {
        this.context = c;
        this.pdfAbsPath = pdfAbsPath;
        this.pdfFragment = FragmentManager;
        this.pdfiumCore = new PdfiumCore(c);
    }

    @NonNull
    @Override
    public PDFAdapter.PdfViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.main_fragment_created_pdf_single, parent, false);

        return new PdfViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PDFAdapter.PdfViewHolder holder, int position) {
        try {
            holder.bindPdf(pdfAbsPath.get(position), position);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    @Override
    public int getItemCount() {
        return pdfAbsPath.size();
    }

    public class PdfViewHolder extends RecyclerView.ViewHolder implements PopupMenu.OnMenuItemClickListener {
        ImageView pdfImage;
        TextView pdfText;
        Button morePdf;
        TextView dateModified;
        TextView fileSize;
        LinearLayout main_fragment_file_info_layout;

        public PdfViewHolder(@NonNull View itemView) {
            super(itemView);
            pdfImage = itemView.findViewById(R.id.main_fragment_pdf_image);
            pdfText = itemView.findViewById(R.id.main_fragment_pdf_name);
            morePdf = itemView.findViewById(R.id.main_fragment_more_pdf);
            dateModified = itemView.findViewById(R.id.main_fragment_date_modified);
            fileSize = itemView.findViewById(R.id.main_fragment_pdf_size);
            main_fragment_file_info_layout = itemView.findViewById(R.id.main_fragment_file_info_layout);

        }

        void bindPdf(Pdf pdf, int position) throws FileNotFoundException {
            pdfText.setText(pdf.fileName);

            DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(context);
            dateModified.setText(dateFormat.format(new Date(pdf.dateModified)));
            fileSize.setText(pdf.fileSize);

            generateImageFromPdf(Uri.fromFile(new File(pdf.absPath)));
            main_fragment_file_info_layout.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return true;
                }
            });


            pdfImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openPdf(pdf.absPath);
                }
            });
            pdfText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openPdf(pdf.absPath);
                }
            });
            morePdf.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showPopupMenu(v);
                }
            });
            Log.e("PDFADAPTER", " " + pdf.fileName);
        }

        void openPdf(String absPath) {
//            File pdfFile = new File(absPath);
//            Intent openPdf = new Intent(Intent.ACTION_VIEW);
//            Activity origin = (Activity) context;
//            openPdf.setDataAndType(FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", pdfFile), "application/pdf");
//            openPdf.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_GRANT_READ_URI_PERMISSION);
//            origin.startActivity(openPdf);
            Intent openPdf = new Intent(context, PdfViewer.class);
            openPdf.putExtra("selectedAbsPath", absPath);
            Activity origin = (Activity) context;
            origin.startActivity(openPdf);
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.share_pdf:
                    Intent implicitIntent = new Intent();
                    implicitIntent.setAction(Intent.ACTION_SEND);
                    implicitIntent.setType("application/pdf");
                    implicitIntent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", new File(pdfAbsPath.get(getAdapterPosition()).absPath)));
                    implicitIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    Activity origin = (Activity) context;
                    origin.startActivity(Intent.createChooser(implicitIntent, "Share To:"));
                    return true;
                case R.id.delete_pdf:
                    String deletingName = pdfAbsPath.get(getAdapterPosition()).fileName;
                    File file = new File(pdfAbsPath.get(getAdapterPosition()).absPath);
                    if (file.exists()) {
                        CustomDialog customDialog = new CustomDialog(pdfFragment.getActivity());
                        customDialog.showMyDialog("Delete File", "Are you sure you want to delete this PDF?", false, "Yes", R.drawable.ic_done, new AbstractDialog.OnClickListener() {
                            @Override
                            public void onClick(dev.shreyaspatil.MaterialDialog.interfaces.DialogInterface dialogInterface, int which) {
                                file.delete();
                                dialogInterface.dismiss();
                                Toast.makeText(context, deletingName + " has been deleted.", Toast.LENGTH_SHORT).show();
                                pdfAbsPath.remove(getAdapterPosition());
                                notifyDataSetChanged();
                            }
                        }, "No", R.drawable.ic_close, new AbstractDialog.OnClickListener() {
                            @Override
                            public void onClick(dev.shreyaspatil.MaterialDialog.interfaces.DialogInterface dialogInterface, int which) {
                                dialogInterface.dismiss();
                            }
                        });

                    } else {
                        Toast.makeText(context, deletingName + " does not exist.", Toast.LENGTH_SHORT).show();
                    }
                    return true;

//                case R.id.rename_pdf:
//                    renameFile = new File(pdfAbsPath.get(getAdapterPosition()).absPath);
//                    if(renameFile.exists()){
//                        pdfFragment.renamePdf(pdfAbsPath.get(getAdapterPosition()).fileName);
//                    }
//                    return true;
            }
            return false;
        }

        private void showPopupMenu(View view) {
            PopupMenu popupMenu = new PopupMenu(view.getContext(), view);
            popupMenu.inflate(R.menu.popup_menu);
            popupMenu.setOnMenuItemClickListener(this);
            popupMenu.show();
        }


        //        @Override
//        public void applyText(String fileName) {
//            File renameTo = new File(getOutputDirectory().getAbsolutePath(),fileName);
//            renameFile.renameTo(renameTo);
//        }
        void generateImageFromPdf(Uri pdfUri) {
            int pageNumber = 0;
            class GeneratePreview extends AsyncTask<Void, Void, Bitmap> {
                Bitmap bmp = null;

                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    Glide.with(context).clear(pdfImage);
                }

                @Override
                protected Bitmap doInBackground(Void... voids) {
                    try {
                        //http://www.programcreek.com/java-api-examples/index.php?api=android.os.ParcelFileDescriptor
                        ParcelFileDescriptor fd = context.getContentResolver().openFileDescriptor(pdfUri, "r");
                        PdfDocument pdfDocument = pdfiumCore.newDocument(fd);
                        pdfiumCore.openPage(pdfDocument, pageNumber);
                        int width = pdfiumCore.getPageWidthPoint(pdfDocument, pageNumber);
                        int height = pdfiumCore.getPageHeightPoint(pdfDocument, pageNumber);
                        bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_4444);
                        pdfiumCore.renderPageBitmap(pdfDocument, bmp, pageNumber, 0, 0, width, height);
                        pdfiumCore.closeDocument(pdfDocument); // important!
                    } catch (Exception e) {
                        //todo with exception
                    }
                    return bmp;
                }

                @Override
                protected void onPostExecute(Bitmap bitmap) {
                    super.onPostExecute(bitmap);
                    Glide.with(context)
                            .load(bitmap)
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .thumbnail(0.1f)
                            .into(pdfImage);
                }
            }
            GeneratePreview generatePreview = new GeneratePreview();
            generatePreview.execute();
//        try {
//            //http://www.programcreek.com/java-api-examples/index.php?api=android.os.ParcelFileDescriptor
//            ParcelFileDescriptor fd = context.getContentResolver().openFileDescriptor(pdfUri, "r");
//            PdfDocument pdfDocument = pdfiumCore.newDocument(fd);
//            pdfiumCore.openPage(pdfDocument, pageNumber);
//            int width = pdfiumCore.getPageWidthPoint(pdfDocument, pageNumber);
//            int height = pdfiumCore.getPageHeightPoint(pdfDocument, pageNumber);
//            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
//            pdfiumCore.renderPageBitmap(pdfDocument, bmp, pageNumber, 0, 0, width, height);
//            pdfiumCore.closeDocument(pdfDocument); // important!
//            return bmp;
//        } catch(Exception e) {
//            //todo with exception
//        }
//        return null;
        }
    }


}
