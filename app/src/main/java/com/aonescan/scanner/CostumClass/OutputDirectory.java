package com.aonescan.scanner.CostumClass;

import android.content.Context;
import android.os.Build;
import android.os.Environment;

import com.aonescan.scanner.R;

import java.io.File;

public class OutputDirectory {
    private Context context;
    private String folderName;

    public OutputDirectory(Context context, String folderName) {
        this.context = context;
        this.folderName = folderName;
    }

    public File getFileDir() {
        File directory;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            directory = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) + File.separator + folderName);
        } else {
            directory = new File(Environment.getExternalStorageDirectory() + File.separator + context.getResources().getString(R.string.app_name) + File.separator + folderName);
        }
        if (!directory.exists()) {
            directory.mkdirs();
        }
        return directory;
    }
}
