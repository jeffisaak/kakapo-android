package com.aptasystems.kakapo.viewmodel;

import com.aptasystems.kakapo.HelpActivity;

import java.util.Stack;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class HelpActivityModel extends ViewModel {

    private final MutableLiveData<Stack<HelpActivity.HelpHistoryEntry>> _helpHistoryOpenLiveData = new MutableLiveData<>(new Stack<>());

    public MutableLiveData<Stack<HelpActivity.HelpHistoryEntry>> getHelpHistoryOpenLiveData() {
        return _helpHistoryOpenLiveData;
    }
}
