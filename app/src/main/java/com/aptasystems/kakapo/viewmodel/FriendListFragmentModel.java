package com.aptasystems.kakapo.viewmodel;

import android.util.Pair;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class FriendListFragmentModel extends ViewModel {

    private final MutableLiveData<Boolean> _floatingMenuOpenLiveData = new MutableLiveData<>(false);

    public MutableLiveData<Boolean> getFloatingMenuOpenLiveData() {
        return _floatingMenuOpenLiveData;
    }
}
