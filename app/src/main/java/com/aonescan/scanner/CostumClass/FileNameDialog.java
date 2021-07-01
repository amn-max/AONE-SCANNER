package com.aonescan.scanner.CostumClass;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.DialogFragment;

import com.aonescan.scanner.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textview.MaterialTextView;

public class FileNameDialog extends DialogFragment {
    public FileNameDialogListener fileNameDialogListener;
    private EditText editTextFilename;
    private String defaultFileName;
    private MaterialTextView titleTextview;
    private TextInputEditText fileEdittext;
    private String FILE_NAME_STRING = "File Name";
    private MaterialButton btnSave;
    private MaterialButton btnCancel;
    private String title;
    private String textHint;


    public void setTitle(String title) {
        this.title = title;
    }

    public void setEditTextHint(String textHint) {
        this.textHint = textHint;
    }

    public void setFileNameDialogListener(FileNameDialogListener fileNameDialogListener){
        this.fileNameDialogListener = fileNameDialogListener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        MaterialAlertDialogBuilder materialBuilder = new MaterialAlertDialogBuilder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        final View view = inflater.inflate(R.layout.ask_file_name, null);
        btnSave = view.findViewById(R.id.btn_save);
        btnCancel = view.findViewById(R.id.btn_cancel);
        titleTextview = view.findViewById(R.id.txt_enter_file_name);
        fileEdittext = view.findViewById(R.id.et_filename_pdf);
        titleTextview.setText(title);
        fileEdittext.setHint(textHint);
        materialBuilder.setView(view);
        materialBuilder.setBackground(ResourcesCompat.getDrawable(requireContext().getResources(),R.drawable.rounded_corner_white,null));
        materialBuilder.setCancelable(false);

        btnSave.setOnClickListener(v -> {

            String fileName = editTextFilename.getText().toString();
            fileNameDialogListener.applyText(fileName);
            fileNameDialogListener.onDialog(true);
            closeKeyboard();
        });

        btnCancel.setOnClickListener(v -> {
            fileNameDialogListener.onDialog(true);
            closeKeyboard();
        });


        editTextFilename = view.findViewById(R.id.et_filename_pdf);
        editTextFilename.setText(defaultFileName);
        editTextFilename.setSelectAllOnFocus(true);
        editTextFilename.requestFocus();
        showKeyboard();
        return materialBuilder.create();
    }

    public void setFILE_NAME_STRING(String titleName) {
        this.FILE_NAME_STRING = titleName;
    }

    public void showKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager) requireActivity().getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    public void closeKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager) requireActivity().getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
    }

//    @Override
//    public void onAttach(@NonNull Context context) {
//        super.onAttach(context);
//        try {
//            fileNameDialogListener = (FileNameDialogListener) getTargetFragment();
//        } catch (ClassCastException e) {
//            Log.e("dialog", "onAttach : ClassCastException : " + e.getMessage());
//        }
//    }

    public void setDefaultName(String fileName) {
        this.defaultFileName = fileName;
    }


    public interface FileNameDialogListener {
        void applyText(String fileName);

        void onDialog(boolean showDialog);
    }
}
