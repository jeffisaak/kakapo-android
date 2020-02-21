package com.aptasystems.kakapo.viewmodel;

import android.util.Pair;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class MeFragmentModel extends ViewModel {

    private final MutableLiveData<Pair<Integer, Integer>> _quotaLiveData = new MutableLiveData<>();
    private final MutableLiveData<Integer> _swatchColourLiveData = new MutableLiveData<>();

    public MutableLiveData<Pair<Integer, Integer>> getQuotaLiveData() {
        return _quotaLiveData;
    }

    public MutableLiveData<Integer> getSwatchColourLiveData() {
        return _swatchColourLiveData;
    }
}
