package com.aonescan.scanner;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.util.FitPolicy;

import java.io.File;

public class PdfViewer extends AppCompatActivity {
    private PDFView pdfView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_viewer);
        String absPath = getIntent().getStringExtra("selectedAbsPath");

        pdfView = findViewById(R.id.pdf_viewer);
        pdfView.useBestQuality(true);

        pdfView.fromFile(new File(absPath))
                .enableDoubletap(true)
                .enableAntialiasing(true)
                .pageFitPolicy(FitPolicy.WIDTH)
                .spacing(5).autoSpacing(false)
                .defaultPage(0)
                .onRender(nbPages -> pdfView.fitToWidth(pdfView.getCurrentPage()))
                .load();
    }

    @Override
    public void onBackPressed() {
        finish();
        super.onBackPressed();
    }
}