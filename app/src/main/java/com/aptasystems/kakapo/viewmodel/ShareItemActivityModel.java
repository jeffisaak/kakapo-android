package com.aptasystems.kakapo.viewmodel;

import android.net.Uri;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ShareItemActivityModel extends ViewModel {

    private final MutableLiveData<Uri> _selectedAttachmentUriLiveData =
            new MutableLiveData<>();

    private final MutableLiveData<String> _mimeTypeLiveData =
            new MutableLiveData<>();

    public MutableLiveData<Uri> getSelectedAttachmentUriLiveData() {
        return _selectedAttachmentUriLiveData;
    }

    public MutableLiveData<String> getMimeTypeLiveData() {
        return _mimeTypeLiveData;
    }
}
