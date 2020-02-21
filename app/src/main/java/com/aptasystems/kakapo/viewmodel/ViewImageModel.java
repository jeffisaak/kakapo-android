package com.aptasystems.kakapo.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ViewImageModel extends ViewModel {

    private final MutableLiveData<String> _displayedFilenameLiveData = new MutableLiveData<>();
    private final MutableLiveData<Float> _downloadProgressLiveData = new MutableLiveData<>();

    public MutableLiveData<String> getDisplayedFilenameLiveData() {
        return _displayedFilenameLiveData;
    }

    public MutableLiveData<Float> getDownloadProgressLiveData() {
        return _downloadProgressLiveData;
    }
}
