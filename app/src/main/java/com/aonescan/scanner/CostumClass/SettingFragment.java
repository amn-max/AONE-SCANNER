package com.aonescan.scanner.CostumClass;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import com.aonescan.scanner.R;

public class SettingFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }
}
