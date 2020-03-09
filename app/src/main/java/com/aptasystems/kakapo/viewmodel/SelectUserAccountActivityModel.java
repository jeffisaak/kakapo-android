package com.aptasystems.kakapo.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SelectUserAccountActivityModel extends ViewModel {

    private final MutableLiveData<Boolean> _floatingMenuOpenLiveData = new MutableLiveData<>(false);

    public MutableLiveData<Boolean> getFloatingMenuOpenLiveData() {
        return _floatingMenuOpenLiveData;
    }
}
