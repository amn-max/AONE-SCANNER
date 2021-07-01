package com.aonescan.scanner.CostumClass;

import android.app.Activity;

import dev.shreyaspatil.MaterialDialog.AbstractDialog;
import dev.shreyaspatil.MaterialDialog.BottomSheetMaterialDialog;

public class CustomDialog {
    private final Activity mActivity;

    public CustomDialog(Activity activity) {
        this.mActivity = activity;
    }

    public void showMyDialog(
            String title,
            String message,
            Boolean cancelable,
            String positiveString,
            int positiveDrawable,
            AbstractDialog.OnClickListener positiveListener,
            String negativeString,
            int negativeDrawable,
            AbstractDialog.OnClickListener negativeListener
    ) {
        BottomSheetMaterialDialog bottomSheetMaterialDialog;
        if (negativeString == null || negativeListener == null) {
            bottomSheetMaterialDialog = new BottomSheetMaterialDialog.Builder(mActivity)
                    .setTitle(title)
                    .setMessage(message)
                    .setCancelable(cancelable)
                    .setPositiveButton(positiveString, positiveDrawable, positiveListener)
                    .build();
        } else {
            bottomSheetMaterialDialog = new BottomSheetMaterialDialog.Builder(mActivity)
                    .setTitle(title)
                    .setMessage(message)
                    .setCancelable(cancelable)
                    .setPositiveButton(positiveString, positiveDrawable, positiveListener)
                    .setNegativeButton(negativeString, negativeDrawable, negativeListener)
                    .build();
        }
        bottomSheetMaterialDialog.show();
    }
}
