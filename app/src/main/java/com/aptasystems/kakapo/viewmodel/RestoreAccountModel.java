package com.aptasystems.kakapo.viewmodel;

import com.aptasystems.kakapo.service.AccountData;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class RestoreAccountModel extends ViewModel {

    private final MutableLiveData<AccountData> _accountDataLiveData =
            new MutableLiveData<>();

    public MutableLiveData<AccountData> getAccountDataLiveData() {
        return _accountDataLiveData;
    }
}
