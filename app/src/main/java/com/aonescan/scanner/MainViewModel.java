package com.aonescan.scanner;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class MainViewModel extends ViewModel {
    public MutableLiveData<String> _title = new MutableLiveData<String>();
    LiveData<String> title;

    LiveData<String> get() {
        return _title;
    }

    public void updateActionBarTitle(String title) {
        _title.postValue(title);
    }
}
