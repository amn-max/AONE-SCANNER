package com.aonescan.scanner.CostumClass;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.aonescan.scanner.R;

public class LoadingDialog {
    private Activity activity;
    private AlertDialog dialog;
    private TextView txt_Loading;

    public LoadingDialog(Activity mActivity) {
        this.activity = mActivity;
    }

    public void startLoadingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        LayoutInflater inflater = activity.getLayoutInflater();
        View view = inflater.inflate(R.layout.costum_dialog, null);
        txt_Loading = view.findViewById(R.id.txt_setLoadingText);
        builder.setView(view);
        builder.setCancelable(false);

        dialog = builder.create();
        dialog.show();
    }

    public void setLoadingText(String loadingText) {
        if (loadingText.isEmpty()) {
            txt_Loading.setText("");
        } else {
            txt_Loading.setText(loadingText);
        }
    }

    public void dismissDialog() {
        dialog.dismiss();
    }
}
