package com.aonescan.scanner.CostumClass;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;

import androidx.appcompat.app.AlertDialog;

import com.aonescan.scanner.R;

public class LoadingDialogTransparent {
    private final Activity activity;
    private AlertDialog dialog;

    public LoadingDialogTransparent(Activity mActivity) {
        this.activity = mActivity;

    }

    public void startLoadingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        LayoutInflater inflater = activity.getLayoutInflater();
        View view = inflater.inflate(R.layout.transparent_load, null);
        builder.setView(view);
        builder.setCancelable(false);

        dialog = builder.create();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();
    }

    public void dismissDialog() {
        dialog.dismiss();
    }
}
