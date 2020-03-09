package com.aptasystems.kakapo.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class NewsFragmentModel extends ViewModel {

    private final MutableLiveData<Long> _remainingItemCountLiveData = new MutableLiveData<>();

    public MutableLiveData<Long> getRemainingItemCountLiveData() {
        return _remainingItemCountLiveData;
    }
}
